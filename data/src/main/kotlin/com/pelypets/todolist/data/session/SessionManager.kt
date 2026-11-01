package com.pelypets.todolist.data.session

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Single source of truth for the tokens; read on every request, so they are cached in memory. */
@Singleton
class SessionManager @Inject constructor(private val storage: TokenStorage) : SessionRepository {

    @Volatile
    private var tokens: StoredTokens? = storage.read()

    private val _session = MutableStateFlow(if (tokens != null) SessionState.LOGGED_IN else SessionState.LOGGED_OUT)
    override val session: StateFlow<SessionState> = _session.asStateFlow()

    fun currentTokens(): StoredTokens? = tokens

    @Synchronized
    fun start(newTokens: StoredTokens) {
        storage.save(newTokens)
        tokens = newTokens
        _session.value = SessionState.LOGGED_IN
    }

    @Synchronized
    fun end() {
        storage.clear()
        tokens = null
        _session.value = SessionState.LOGGED_OUT
    }
}
