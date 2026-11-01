package com.pelypets.todolist.feature.tododetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus
import com.pelypets.todolist.domain.usecase.DefaultDueDateUseCase
import com.pelypets.todolist.domain.usecase.DeleteTodoUseCase
import com.pelypets.todolist.domain.usecase.GetTodoUseCase
import com.pelypets.todolist.domain.usecase.SaveTodoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class TodoDetailUiState(
    val todoId: String?,
    val title: String = "",
    val description: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val status: TodoStatus = TodoStatus.TODO,
    val titleError: ValidationError? = null,
    val descriptionError: ValidationError? = null,
    val isLoading: Boolean = false,
    val loadError: DomainError? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
) {
    val isNew: Boolean get() = todoId == null
    val isBusy: Boolean get() = isLoading || isSaving || isDeleting
    val canEdit: Boolean get() = !isBusy && loadError == null
}

sealed interface TodoDetailEvent {
    data object Close : TodoDetailEvent
    data class ShowError(val error: DomainError) : TodoDetailEvent
}

@HiltViewModel
class TodoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTodo: GetTodoUseCase,
    private val saveTodo: SaveTodoUseCase,
    private val deleteTodo: DeleteTodoUseCase,
    defaultDueDate: DefaultDueDateUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val todoId: String? = TodoDetailFragmentArgs.fromSavedStateHandle(savedStateHandle).todoId

    private val _uiState = MutableStateFlow(
        LocalDateTime.ofInstant(defaultDueDate(), clock.zone).let { dueAt ->
            TodoDetailUiState(todoId = todoId, date = dueAt.toLocalDate(), time = dueAt.toLocalTime(), isLoading = todoId != null)
        },
    )
    val uiState: StateFlow<TodoDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<TodoDetailEvent>(Channel.BUFFERED)
    val events: Flow<TodoDetailEvent> = _events.receiveAsFlow()

    init {
        todoId?.let(::load)
    }

    fun onRetry() {
        todoId?.let(::load)
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value, titleError = null) }

    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value, descriptionError = null) }

    fun onStatusSelected(status: TodoStatus) = _uiState.update { it.copy(status = status) }

    fun onDateSelected(date: LocalDate) = _uiState.update { it.copy(date = date) }

    fun onTimeSelected(time: LocalTime) = _uiState.update { it.copy(time = time) }

    fun onSaveClicked() {
        val state = _uiState.value
        if (!state.canEdit) return
        _uiState.update { it.copy(isSaving = true) }
        val draft = TodoDraft(
            title = state.title,
            description = state.description,
            dueAt = LocalDateTime.of(state.date, state.time).atZone(clock.zone).toInstant(),
            status = state.status,
        )
        viewModelScope.launch {
            when (val result = saveTodo(state.todoId, draft)) {
                is DomainResult.Success -> _events.send(TodoDetailEvent.Close)

                is DomainResult.Failure -> {
                    val errors = (result.error as? DomainError.Validation)?.errors.orEmpty()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            titleError = errors.firstOrNull { error -> error in titleErrors },
                            descriptionError = errors.firstOrNull { error -> error == ValidationError.DESCRIPTION_TOO_LONG },
                        )
                    }
                    if (result.error.isReportable()) _events.send(TodoDetailEvent.ShowError(result.error))
                }
            }
        }
    }

    fun onDeleteConfirmed() {
        val id = todoId ?: return
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            when (val result = deleteTodo(id)) {
                is DomainResult.Success -> _events.send(TodoDetailEvent.Close)

                is DomainResult.Failure -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    if (result.error.isReportable()) _events.send(TodoDetailEvent.ShowError(result.error))
                }
            }
        }
    }

    fun onBackClicked() {
        if (!_uiState.value.isBusy) _events.trySend(TodoDetailEvent.Close)
    }

    private fun load(id: String) {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = getTodo(id)) {
                is DomainResult.Success -> {
                    val todo = result.value
                    val dueAt = LocalDateTime.ofInstant(todo.dueAt, clock.zone)
                    _uiState.update {
                        it.copy(
                            title = todo.title,
                            description = todo.description,
                            date = dueAt.toLocalDate(),
                            time = dueAt.toLocalTime(),
                            status = todo.status,
                            isLoading = false,
                            loadError = null,
                        )
                    }
                }

                is DomainResult.Failure -> _uiState.update { it.copy(isLoading = false, loadError = result.error) }
            }
        }
    }

    private companion object {
        val titleErrors = setOf(ValidationError.TITLE_BLANK, ValidationError.TITLE_TOO_LONG)

        /** Field errors are shown inline and an expired session is handled by the auth gate. */
        fun DomainError.isReportable(): Boolean =
            !(this is DomainError.Validation && errors.isNotEmpty()) && this != DomainError.Unauthorized
    }
}
