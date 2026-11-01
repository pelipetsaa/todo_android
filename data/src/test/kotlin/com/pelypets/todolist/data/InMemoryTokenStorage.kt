package com.pelypets.todolist.data

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class InMemoryTokenStorage(@Volatile var tokens: StoredTokens?) : TokenStorage {
    override fun read() = tokens
    override fun save(tokens: StoredTokens) {
        this.tokens = tokens
    }
    override fun clear() {
        tokens = null
    }
}

/** A session that has not read [storage] yet, as during the splash screen. */
fun newSession(storage: TokenStorage) = SessionManager(storage, Dispatchers.Unconfined)

/** A session that has already read [storage], as after the splash screen. */
fun restoredSession(storage: TokenStorage) = newSession(storage).apply { runBlocking { restore() } }
