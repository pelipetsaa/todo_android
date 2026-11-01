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

/** The refresh endpoint answered with [code] without rejecting the token, e.g. it throttled the client. */
class TokenRefreshUnavailableException(val code: Int) : IOException("Token refresh failed with HTTP $code")

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

            when (val refreshed = refresh(current)) {
                null -> session.endIf(expected = current)
                else -> if (session.replace(expected = current, new = refreshed)) return request.withBearer(refreshed.accessToken)
            }
            // The session changed while the refresh was in flight: a logout fails the request, a new sign-in repeats it.
            return session.currentTokens()?.let { request.withBearer(it.accessToken) }
        }
    }

    /**
     * Returns null when the refresh token is rejected. Throws [IOException] when the server cannot be reached
     * or does not answer properly: the session is kept and the request fails, so it can be retried.
     */
    private fun refresh(current: StoredTokens): StoredTokens? {
        val result = try {
            authApi.get().refresh(RefreshTokenDto(current.refreshToken)).execute()
        } catch (e: RuntimeException) {
            // E.g. a body that is not JSON. Anything but an IOException escapes on OkHttp's thread and crashes the app.
            throw IOException("Malformed token refresh response", e)
        }
        val body = result.body()
        return when {
            result.isSuccessful && body != null -> StoredTokens(body.accessToken, body.refreshToken)
            result.code() in REJECTED_STATUSES -> null
            else -> throw TokenRefreshUnavailableException(result.code())
        }
    }

    private companion object {
        val REJECTED_STATUSES = setOf(400, 401)
    }
}
