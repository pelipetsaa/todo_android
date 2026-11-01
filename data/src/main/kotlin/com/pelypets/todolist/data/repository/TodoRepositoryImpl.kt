package com.pelypets.todolist.data.repository

import com.pelypets.todolist.data.mapper.toDomain
import com.pelypets.todolist.data.mapper.toDto
import com.pelypets.todolist.data.remote.api.TodoApi
import com.pelypets.todolist.data.remote.safeApiCall
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.repository.TodoRepository
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(private val api: TodoApi) : TodoRepository {

    override suspend fun getTodos(range: TimeRange): DomainResult<List<Todo>> =
        safeApiCall { api.getTodos(range.from.toString(), range.to.toString()).map { it.toDomain() } }

    override suspend fun getTodo(id: String): DomainResult<Todo> = safeApiCall { api.getTodo(id).toDomain() }

    override suspend fun create(draft: TodoDraft): DomainResult<Todo> = safeApiCall { api.create(draft.toDto()).toDomain() }

    override suspend fun update(id: String, draft: TodoDraft): DomainResult<Todo> =
        safeApiCall { api.update(id, draft.toDto()).toDomain() }

    override suspend fun delete(id: String): DomainResult<Unit> = safeApiCall { api.delete(id) }
}
