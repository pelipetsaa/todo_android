package com.pelypets.todolist.data.repository

import com.pelypets.todolist.data.di.ApplicationScope
import com.pelypets.todolist.data.di.IoDispatcher
import com.pelypets.todolist.data.mapper.toStoredTokens
import com.pelypets.todolist.data.remote.api.AuthApi
import com.pelypets.todolist.data.remote.dto.EmailCredentialsDto
import com.pelypets.todolist.data.remote.dto.GoogleLoginDto
import com.pelypets.todolist.data.remote.dto.RefreshTokenDto
import com.pelypets.todolist.data.remote.dto.TokenPairDto
import com.pelypets.todolist.data.remote.safeApiCall
import com.pelypets.todolist.data.session.SessionManager
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.map
import com.pelypets.todolist.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val session: SessionManager,
    @ApplicationScope private val externalScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun login(email: String, password: String): DomainResult<Unit> =
        authenticate { api.login(EmailCredentialsDto(email, password)) }

    override suspend fun register(email: String, password: String): DomainResult<Unit> =
        authenticate { api.register(EmailCredentialsDto(email, password)) }

    override suspend fun loginWithGoogle(idToken: String): DomainResult<Unit> =
        authenticate { api.loginWithGoogle(GoogleLoginDto(idToken)) }

    override suspend fun logout(): DomainResult<Unit> {
        val refreshToken = session.currentTokens()?.refreshToken
        session.end()
        // Best effort, and a sleeping server can take a minute to answer: the user does not wait for it.
        if (refreshToken != null) {
            externalScope.launch { safeApiCall { api.logout(RefreshTokenDto(refreshToken)) } }
        }
        return DomainResult.Success(Unit)
    }

    private suspend inline fun authenticate(crossinline call: suspend () -> TokenPairDto): DomainResult<Unit> {
        val result = safeApiCall { call() }
        // Saving encrypts with a Keystore key, which the first sign-in also has to generate: too slow for the main thread.
        return withContext(ioDispatcher) { result.map { tokens -> session.start(tokens.toStoredTokens()) } }
    }
}
