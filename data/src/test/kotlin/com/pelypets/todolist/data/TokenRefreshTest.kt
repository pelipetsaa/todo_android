package com.pelypets.todolist.data

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.remote.DataJson
import com.pelypets.todolist.data.remote.api.AuthApi
import com.pelypets.todolist.data.remote.api.TodoApi
import com.pelypets.todolist.data.remote.interceptor.AuthInterceptor
import com.pelypets.todolist.data.remote.interceptor.TokenAuthenticator
import com.pelypets.todolist.data.repository.TodoRepositoryImpl
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.model.TimeRange
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TokenRefreshTest {

    private val oldTokens = StoredTokens("old-access", "old-refresh")
    private val server = MockWebServer()
    private val storage = InMemoryTokenStorage(oldTokens)
    private val session = restoredSession(storage)
    private lateinit var client: OkHttpClient
    private lateinit var authApi: AuthApi
    private lateinit var repository: TodoRepositoryImpl

    private val range = TimeRange(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-02T00:00:00Z"))

    @Before
    fun setUp() {
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .authenticator(TokenAuthenticator(session) { authApi })
            .build()
        val retrofit = retrofit(server.url("/").toString())
        authApi = retrofit.create(AuthApi::class.java)
        repository = TodoRepositoryImpl(retrofit.create(TodoApi::class.java))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `expired access token is refreshed and the request is repeated`() = runTest {
        server.enqueue(MockResponse.Builder().code(401).build())
        server.enqueue(tokensResponse())
        server.enqueue(todosResponse())

        val result = repository.getTodos(range)

        assertEquals("Milk", (result as DomainResult.Success).value.single().title)
        assertEquals("Bearer old-access", server.takeRequest().headers["Authorization"])
        assertTrue(server.takeRequest().requestLine.contains("/api/v1/auth/refresh"))
        assertEquals("Bearer new-access", server.takeRequest().headers["Authorization"])
        assertEquals(StoredTokens("new-access", "new-refresh"), storage.tokens)
    }

    @Test
    fun `rejected refresh token ends the session`() = runTest {
        server.enqueue(MockResponse.Builder().code(401).build())
        server.enqueue(MockResponse.Builder().code(401).build())

        val result = repository.getTodos(range)

        assertEquals(DomainResult.Failure(DomainError.Unauthorized), result)
        assertEquals(SessionState.LOGGED_OUT, session.session.value)
        assertEquals(null, storage.tokens)
    }

    @Test
    fun `logging out while the token is being refreshed keeps the user logged out`() = runTest {
        server.dispatcher = dispatcher(onRefresh = {
            session.end()
            tokensResponse()
        })

        val result = repository.getTodos(range)

        assertEquals(DomainResult.Failure(DomainError.Unauthorized), result)
        assertEquals(SessionState.LOGGED_OUT, session.session.value)
        assertEquals(null, storage.tokens)
    }

    @Test
    fun `rejected refresh repeats the request with a session started in the meantime`() = runTest {
        val otherTokens = StoredTokens("other-access", "other-refresh")
        server.dispatcher = dispatcher(onRefresh = {
            session.start(otherTokens)
            MockResponse.Builder().code(401).build()
        })

        val result = repository.getTodos(range)

        assertTrue(result is DomainResult.Success)
        assertEquals(SessionState.LOGGED_IN, session.session.value)
        assertEquals(otherTokens, storage.tokens)
    }

    @Test
    fun `refreshed tokens do not replace a session started in the meantime`() = runTest {
        val otherTokens = StoredTokens("other-access", "other-refresh")
        server.dispatcher = dispatcher(onRefresh = {
            session.start(otherTokens)
            tokensResponse()
        })

        val result = repository.getTodos(range)

        assertTrue(result is DomainResult.Success)
        assertEquals(otherTokens, storage.tokens)
    }

    @Test
    fun `refresh during a redeploy keeps the session and reports the server as unavailable`() = runTest {
        server.enqueue(MockResponse.Builder().code(401).build())
        server.enqueue(MockResponse.Builder().code(503).build())

        val result = repository.getTodos(range)

        assertEquals(DomainResult.Failure(DomainError.ServerUnavailable), result)
        assertEquals(oldTokens, storage.tokens)
    }

    @Test
    fun `malformed refresh response fails the request with an IOException`() {
        server.enqueue(json("<html>Sign in to the Wi-Fi</html>"))
        val expired = Response.Builder()
            .request(Request.Builder().url(server.url("/api/v1/todos")).header("Authorization", "Bearer old-access").build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()

        // Any other exception would be rethrown on OkHttp's dispatcher thread and crash the app.
        assertFailsWith<IOException> { TokenAuthenticator(session) { authApi }.authenticate(null, expired) }
        assertEquals(oldTokens, storage.tokens)
    }

    @Test
    fun `throttled refresh keeps the session and reports too many requests`() = runTest {
        server.enqueue(MockResponse.Builder().code(401).build())
        server.enqueue(MockResponse.Builder().code(429).build())

        val result = repository.getTodos(range)

        assertEquals(DomainResult.Failure(DomainError.TooManyRequests), result)
        assertEquals(SessionState.LOGGED_IN, session.session.value)
        assertEquals(oldTokens, storage.tokens)
    }

    @Test
    fun `unreachable refresh endpoint keeps the session and reports a network error`() = runTest {
        server.enqueue(MockResponse.Builder().code(401).build())
        authApi = retrofit(CLOSED_PORT_URL).create(AuthApi::class.java)

        val result = repository.getTodos(range)

        assertEquals(DomainResult.Failure(DomainError.Network), result)
        assertEquals(SessionState.LOGGED_IN, session.session.value)
        assertEquals(oldTokens, storage.tokens)
    }

    @Test
    fun `parallel requests with an expired token share one refresh`() = runTest {
        val refreshes = AtomicInteger()
        val expiredRequests = CountDownLatch(2)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.isRefresh() -> {
                    refreshes.incrementAndGet()
                    tokensResponse()
                }

                request.headers["Authorization"] == "Bearer old-access" -> {
                    // Both requests must fail with the old token before either of them refreshes it.
                    expiredRequests.countDown()
                    expiredRequests.await(5, TimeUnit.SECONDS)
                    MockResponse.Builder().code(401).build()
                }

                else -> todosResponse()
            }
        }

        val results = listOf(async { repository.getTodos(range) }, async { repository.getTodos(range) }).awaitAll()

        assertTrue(results.all { it is DomainResult.Success })
        assertEquals(1, refreshes.get())
    }

    private fun retrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(DataJson.asConverterFactory("application/json".toMediaType()))
        .build()

    /** Answers the expired token with 401, the refresh call with [onRefresh] and any other token with the todos. */
    private fun dispatcher(onRefresh: () -> MockResponse) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            when {
                request.isRefresh() -> onRefresh()
                request.headers["Authorization"] == "Bearer old-access" -> MockResponse.Builder().code(401).build()
                else -> todosResponse()
            }
    }

    private fun RecordedRequest.isRefresh() = requestLine.contains("/api/v1/auth/refresh")

    private fun tokensResponse() = json("""{"accessToken":"new-access","refreshToken":"new-refresh","expiresInSeconds":900}""")

    private fun todosResponse() = json(
        """[{"id":"1","title":"Milk","description":"","dueAt":"2026-11-01T10:00:00Z","status":"DONE","createdAt":"2026-10-30T08:00:00.123456Z","updatedAt":"2026-10-30T08:00:00Z"}]""",
    )

    private fun json(body: String) = MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private companion object {
        const val CLOSED_PORT_URL = "http://127.0.0.1:1/"
    }
}
