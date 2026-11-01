package com.pelypets.todolist.data

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.data.session.SessionManager
import com.pelypets.todolist.domain.model.SessionState
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionManagerTest {

    private val old = StoredTokens("old-access", "old-refresh")
    private val new = StoredTokens("new-access", "new-refresh")
    private val storage = InMemoryTokenStorage(old)
    private val session = restoredSession(storage)

    @Test
    fun `session is unknown until the stored tokens are read`() = runTest {
        val session = newSession(storage)
        assertEquals(SessionState.UNKNOWN, session.session.value)

        session.restore()

        assertEquals(SessionState.LOGGED_IN, session.session.value)
        assertEquals(old, session.currentTokens())
    }

    @Test
    fun `stored tokens are read on the io dispatcher`() = runTest {
        var readOn: String? = null
        val recording = object : TokenStorage by storage {
            override fun read() = old.also { readOn = Thread.currentThread().name }
        }
        val io = Executors.newSingleThreadExecutor { Thread(it, "io") }.asCoroutineDispatcher()

        io.use { SessionManager(recording, it).restore() }

        assertEquals("io", readOn)
    }

    @Test
    fun `restore without stored tokens means logged out`() = runTest {
        val session = newSession(InMemoryTokenStorage(null))

        session.restore()

        assertEquals(SessionState.LOGGED_OUT, session.session.value)
    }

    @Test
    fun `late restore does not override a session started before it`() = runTest {
        val session = newSession(InMemoryTokenStorage(null))
        session.start(new)

        session.restore()

        assertEquals(new, session.currentTokens())
        assertEquals(SessionState.LOGGED_IN, session.session.value)
    }

    @Test
    fun `replace swaps the tokens of the current session`() {
        assertTrue(session.replace(expected = old, new = new))

        assertEquals(new, session.currentTokens())
        assertEquals(new, storage.tokens)
        assertEquals(SessionState.LOGGED_IN, session.session.value)
    }

    @Test
    fun `replace does not bring back a session that has ended`() {
        session.end()

        assertFalse(session.replace(expected = old, new = new))

        assertNull(session.currentTokens())
        assertNull(storage.tokens)
        assertEquals(SessionState.LOGGED_OUT, session.session.value)
    }

    @Test
    fun `endIf keeps a session that was started after the expected one`() {
        session.start(new)

        assertFalse(session.endIf(expected = old))

        assertEquals(new, session.currentTokens())
        assertEquals(SessionState.LOGGED_IN, session.session.value)
    }

    @Test
    fun `endIf ends the expected session`() {
        assertTrue(session.endIf(expected = old))

        assertNull(storage.tokens)
        assertEquals(SessionState.LOGGED_OUT, session.session.value)
    }
}
