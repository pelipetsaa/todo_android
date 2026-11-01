package com.pelypets.todolist.data

import com.pelypets.todolist.data.remote.interceptor.RetryInterceptor
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RetryInterceptorTest {

    private val server = MockWebServer()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        scheduler.shutdownNow()
        server.close()
    }

    @Test
    fun `GET is repeated after a transient 503`() {
        server.enqueue(MockResponse.Builder().code(503).build())
        server.enqueue(MockResponse.Builder().code(200).build())

        val response = client(baseDelayMillis = 1).newCall(get()).execute()

        assertEquals(200, response.code)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `canceled call stops waiting for the next attempt`() {
        server.enqueue(MockResponse.Builder().code(503).build())
        server.enqueue(MockResponse.Builder().code(200).build())
        val call = client(baseDelayMillis = 10_000).newCall(get())
        scheduler.schedule({ call.cancel() }, 200, TimeUnit.MILLISECONDS)

        val startedAt = System.nanoTime()
        assertFailsWith<IOException> { call.execute() }

        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        assertTrue(elapsedMillis < 2_000, "waited $elapsedMillis ms after the call was canceled")
    }

    private fun client(baseDelayMillis: Long) = OkHttpClient.Builder().addInterceptor(RetryInterceptor(baseDelayMillis)).build()

    private fun get() = Request.Builder().url(server.url("/")).build()
}
