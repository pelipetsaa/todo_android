package com.pelypets.todolist.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Repeats GET requests when a proxy answers 502/503/504 while the server restarts or redeploys.
 * Only GETs: repeating a POST could create the same todo twice.
 */
class RetryInterceptor(private val baseDelayMillis: Long) : Interceptor {

    @Inject constructor() : this(baseDelayMillis = 500)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        if (request.method != "GET") return response

        var attempt = 0
        while (response.code in TRANSIENT_STATUSES && attempt < MAX_RETRIES) {
            response.close()
            Thread.sleep(baseDelayMillis shl attempt)
            attempt++
            response = chain.proceed(request)
        }
        return response
    }

    private companion object {
        const val MAX_RETRIES = 2
        val TRANSIENT_STATUSES = setOf(502, 503, 504)
    }
}
