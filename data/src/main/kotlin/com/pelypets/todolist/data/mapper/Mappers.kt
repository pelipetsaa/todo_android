package com.pelypets.todolist.data.mapper

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.remote.dto.TodoDto
import com.pelypets.todolist.data.remote.dto.TodoInputDto
import com.pelypets.todolist.data.remote.dto.TodoStatusDto
import com.pelypets.todolist.data.remote.dto.TokenPairDto
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus

internal fun TodoDto.toDomain() = Todo(id = id, title = title, description = description, dueAt = dueAt, status = status.toDomain())

internal fun TodoDraft.toDto() = TodoInputDto(title = title, description = description, dueAt = dueAt, status = status.toDto())

internal fun TodoStatusDto.toDomain(): TodoStatus = when (this) {
    TodoStatusDto.TODO -> TodoStatus.TODO
    TodoStatusDto.IN_PROGRESS -> TodoStatus.IN_PROGRESS
    TodoStatusDto.DONE -> TodoStatus.DONE
}

internal fun TodoStatus.toDto(): TodoStatusDto = when (this) {
    TodoStatus.TODO -> TodoStatusDto.TODO
    TodoStatus.IN_PROGRESS -> TodoStatusDto.IN_PROGRESS
    TodoStatus.DONE -> TodoStatusDto.DONE
}

internal fun TokenPairDto.toStoredTokens() = StoredTokens(accessToken = accessToken, refreshToken = refreshToken)
