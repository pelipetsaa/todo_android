package com.pelypets.todolist.data.repository

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
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(private val api: AuthApi, private val session: SessionManager) : AuthRepository {

    override suspend fun login(email: String, password: String): DomainResult<Unit> =
        authenticate { api.login(EmailCredentialsDto(email, password)) }

    override suspend fun register(email: String, password: String): DomainResult<Unit> =
        authenticate { api.register(EmailCredentialsDto(email, password)) }

    override suspend fun loginWithGoogle(idToken: String): DomainResult<Unit> =
        authenticate { api.loginWithGoogle(GoogleLoginDto(idToken)) }

    override suspend fun logout(): DomainResult<Unit> {
        session.currentTokens()?.refreshToken?.let { refreshToken ->
            // Best effort: the local session is cleared even if the server cannot be reached.
            safeApiCall { api.logout(RefreshTokenDto(refreshToken)) }
        }
        session.end()
        return DomainResult.Success(Unit)
    }

    private suspend inline fun authenticate(crossinline call: suspend () -> TokenPairDto): DomainResult<Unit> =
        safeApiCall { call() }.map { tokens -> session.start(tokens.toStoredTokens()) }
}
