package com.pelypets.todolist.domain.repository

import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    suspend fun login(email: String, password: String): DomainResult<Unit>
    suspend fun register(email: String, password: String): DomainResult<Unit>
    suspend fun loginWithGoogle(idToken: String): DomainResult<Unit>
    suspend fun logout(): DomainResult<Unit>
}

interface SessionRepository {
    val session: StateFlow<SessionState>
}

interface TodoRepository {
    suspend fun getTodos(range: TimeRange): DomainResult<List<Todo>>
    suspend fun getTodo(id: String): DomainResult<Todo>
    suspend fun create(draft: TodoDraft): DomainResult<Todo>
    suspend fun update(id: String, draft: TodoDraft): DomainResult<Todo>
    suspend fun delete(id: String): DomainResult<Unit>
}
