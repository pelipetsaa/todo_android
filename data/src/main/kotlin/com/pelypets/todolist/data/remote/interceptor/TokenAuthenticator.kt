package com.pelypets.todolist.data.remote.interceptor

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.remote.api.AuthApi
import com.pelypets.todolist.data.remote.dto.RefreshTokenDto
import com.pelypets.todolist.data.session.SessionManager
import dagger.Lazy
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject

/**
 * Called by OkHttp on a 401: rotates the refresh token once and repeats the request.
 * A rejected refresh token ends the session, which sends the user back to sign-in.
 * [authApi] is lazy because it is built on the same OkHttpClient that owns this authenticator.
 */
class TokenAuthenticator @Inject constructor(
    private val session: SessionManager,
    private val authApi: Lazy<AuthApi>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (request.isAuthEndpoint() || response.priorResponse != null) return null
        val failedToken = request.header(HEADER_AUTHORIZATION)?.removePrefix("Bearer ")

        synchronized(this) {
            val current = session.currentTokens() ?: return null
            // A parallel request has already refreshed the tokens while this one was waiting.
            if (current.accessToken != failedToken) return request.withBearer(current.accessToken)

            return when (val refreshed = refresh(current.refreshToken)) {
                null -> null
                else -> {
                    session.start(refreshed)
                    request.withBearer(refreshed.accessToken)
                }
            }
        }
    }

    private fun refresh(refreshToken: String): StoredTokens? = try {
        val result = authApi.get().refresh(RefreshTokenDto(refreshToken)).execute()
        val body = result.body()
        when {
            result.isSuccessful && body != null -> StoredTokens(body.accessToken, body.refreshToken)

            result.code() in 400..499 -> {
                session.end()
                null
            }

            else -> null
        }
    } catch (e: IOException) {
        // Offline: keep the session, the request fails with a network error and can be retried.
        null
    }
}
