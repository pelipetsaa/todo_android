package com.pelypets.todolist.data

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.data.remote.DataJson
import com.pelypets.todolist.data.remote.api.AuthApi
import com.pelypets.todolist.data.remote.api.TodoApi
import com.pelypets.todolist.data.remote.interceptor.AuthInterceptor
import com.pelypets.todolist.data.remote.interceptor.TokenAuthenticator
import com.pelypets.todolist.data.repository.TodoRepositoryImpl
import com.pelypets.todolist.data.session.SessionManager
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.model.TimeRange
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenRefreshTest {

    private class InMemoryTokenStorage(var tokens: StoredTokens?) : TokenStorage {
        override fun read() = tokens
        override fun save(tokens: StoredTokens) {
            this.tokens = tokens
        }
        override fun clear() {
            tokens = null
        }
    }

    private val server = MockWebServer()
    private val storage = InMemoryTokenStorage(StoredTokens("old-access", "old-refresh"))
    private val session = SessionManager(storage)
    private lateinit var repository: TodoRepositoryImpl

    private val range = TimeRange(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-02T00:00:00Z"))

    @Before
    fun setUp() {
        server.start()
        lateinit var authApi: AuthApi
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .authenticator(TokenAuthenticator(session) { authApi })
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(DataJson.asConverterFactory("application/json".toMediaType()))
            .build()
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
        server.enqueue(json("""{"accessToken":"new-access","refreshToken":"new-refresh","expiresInSeconds":900}"""))
        server.enqueue(json("""[{"id":"1","title":"Milk","description":"","dueAt":"2026-11-01T10:00:00Z","status":"DONE","createdAt":"2026-10-30T08:00:00.123456Z","updatedAt":"2026-10-30T08:00:00Z"}]"""))

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

    private fun json(body: String) = MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()
}
