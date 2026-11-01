package com.pelypets.todolist.data.remote.interceptor

import com.pelypets.todolist.data.remote.api.ApiRoutes
import com.pelypets.todolist.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

internal const val HEADER_AUTHORIZATION = "Authorization"

internal fun Request.isAuthEndpoint(): Boolean = url.encodedPath.startsWith(ApiRoutes.AUTH_PREFIX)

internal fun Request.withBearer(accessToken: String): Request =
    newBuilder().header(HEADER_AUTHORIZATION, "Bearer $accessToken").build()

/** Attaches the access token to every request except the auth endpoints. */
class AuthInterceptor @Inject constructor(private val session: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = session.currentTokens()?.accessToken
        if (request.isAuthEndpoint() || token == null) return chain.proceed(request)
        return chain.proceed(request.withBearer(token))
    }
}
