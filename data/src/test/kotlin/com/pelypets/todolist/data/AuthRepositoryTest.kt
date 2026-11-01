package com.pelypets.todolist.data

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.data.remote.api.AuthApi
import com.pelypets.todolist.data.remote.dto.EmailCredentialsDto
import com.pelypets.todolist.data.remote.dto.GoogleLoginDto
import com.pelypets.todolist.data.remote.dto.RefreshTokenDto
import com.pelypets.todolist.data.remote.dto.TokenPairDto
import com.pelypets.todolist.data.repository.AuthRepositoryImpl
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.model.SessionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.Call
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthRepositoryTest {

    /** Signs in right away; the server never answers the logout call, like a sleeping instance that is still starting. */
    private class FakeAuthApi : AuthApi {
        var revokedToken: String? = null
        val logoutResponse = CompletableDeferred<Unit>()

        override suspend fun login(body: EmailCredentialsDto) = TokenPairDto("new-access", "new-refresh", expiresInSeconds = 900)

        override suspend fun logout(body: RefreshTokenDto) {
            revokedToken = body.refreshToken
            logoutResponse.await()
        }

        override suspend fun register(body: EmailCredentialsDto): TokenPairDto = error("unused")
        override suspend fun loginWithGoogle(body: GoogleLoginDto): TokenPairDto = error("unused")
        override fun refresh(body: RefreshTokenDto): Call<TokenPairDto> = error("unused")
    }

    private val storage = InMemoryTokenStorage(StoredTokens("access", "refresh"))
    private val session = restoredSession(storage)
    private val api = FakeAuthApi()

    private fun repository(externalScope: CoroutineScope, ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined) =
        AuthRepositoryImpl(api, session, externalScope, ioDispatcher)

    @Test
    fun `logout ends the session without waiting for the server`() = runTest {
        repository(externalScope = backgroundScope).logout()

        assertEquals(SessionState.LOGGED_OUT, session.session.value)
        assertNull(storage.tokens)
    }

    @Test
    fun `logout still revokes the refresh token on the server`() = runTest {
        repository(externalScope = backgroundScope).logout()
        runCurrent()

        assertEquals("refresh", api.revokedToken)
    }

    @Test
    fun `tokens of a new sign-in are saved on the io dispatcher`() = runTest {
        var savedOn: String? = null
        val recording = object : TokenStorage by storage {
            override fun save(tokens: StoredTokens) {
                savedOn = Thread.currentThread().name
                storage.save(tokens)
            }
        }
        val io = Executors.newSingleThreadExecutor { Thread(it, "io") }.asCoroutineDispatcher()

        val result = io.use {
            AuthRepositoryImpl(api, restoredSession(recording), backgroundScope, ioDispatcher = it).login("a@b.c", "password")
        }

        assertEquals(DomainResult.Success(Unit), result)
        assertEquals("io", savedOn)
        assertEquals(StoredTokens("new-access", "new-refresh"), storage.tokens)
    }
}
