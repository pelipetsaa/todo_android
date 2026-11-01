package com.pelypets.todolist.data.session

import com.pelypets.todolist.data.di.IoDispatcher
import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Single source of truth for the tokens; read on every request, so they are cached in memory. */
@Singleton
class SessionManager @Inject constructor(
    private val storage: TokenStorage,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SessionRepository {

    @Volatile
    private var tokens: StoredTokens? = null

    private val _session = MutableStateFlow(SessionState.UNKNOWN)
    override val session: StateFlow<SessionState> = _session.asStateFlow()

    fun currentTokens(): StoredTokens? = tokens

    override suspend fun restore() = withContext(ioDispatcher) { readStoredSession() }

    @Synchronized
    private fun readStoredSession() {
        if (_session.value != SessionState.UNKNOWN) return
        tokens = storage.read()
        _session.value = if (tokens != null) SessionState.LOGGED_IN else SessionState.LOGGED_OUT
    }

    @Synchronized
    fun start(newTokens: StoredTokens) {
        storage.save(newTokens)
        tokens = newTokens
        _session.value = SessionState.LOGGED_IN
    }

    /** Rotates the tokens only if the session is still the one that [expected] belongs to. */
    @Synchronized
    fun replace(expected: StoredTokens, new: StoredTokens): Boolean {
        if (tokens != expected) return false
        storage.save(new)
        tokens = new
        return true
    }

    @Synchronized
    fun end() {
        storage.clear()
        tokens = null
        _session.value = SessionState.LOGGED_OUT
    }

    /** Ends the session only if it is still the one that [expected] belongs to. */
    @Synchronized
    fun endIf(expected: StoredTokens): Boolean {
        if (tokens != expected) return false
        end()
        return true
    }
}
