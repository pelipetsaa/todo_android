package com.pelypets.todolist.feature.todolist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.usecase.GetTodosForPeriodUseCase
import com.pelypets.todolist.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodoListUiState(
    val period: Period = Period.TODAY,
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    /** Shown full screen; only set when there is no list to keep showing. */
    val error: DomainError? = null,
) {
    val isEmpty: Boolean get() = hasLoaded && todos.isEmpty() && error == null
}

sealed interface TodoListEvent {
    data class OpenTodo(val id: String?) : TodoListEvent
    data class ShowError(val error: DomainError) : TodoListEvent
}

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val getTodosForPeriod: GetTodosForPeriodUseCase,
    private val logout: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoListUiState())
    val uiState: StateFlow<TodoListUiState> = _uiState.asStateFlow()

    private val _events = Channel<TodoListEvent>(Channel.BUFFERED)
    val events: Flow<TodoListEvent> = _events.receiveAsFlow()

    private var loadJob: Job? = null

    /** Called every time the screen becomes visible, e.g. after returning from the detail screen. */
    fun onScreenShown() = load(pullToRefresh = false)

    fun onRefresh() = load(pullToRefresh = true)

    fun onRetry() = load(pullToRefresh = false)

    fun onPeriodSelected(period: Period) {
        if (period == _uiState.value.period) return
        _uiState.value = TodoListUiState(period = period)
        load(pullToRefresh = false)
    }

    fun onTodoClicked(id: String) {
        _events.trySend(TodoListEvent.OpenTodo(id))
    }

    fun onAddClicked() {
        _events.trySend(TodoListEvent.OpenTodo(null))
    }

    fun onLogoutClicked() {
        viewModelScope.launch { logout() }
    }

    private fun load(pullToRefresh: Boolean) {
        val period = _uiState.value.period
        loadJob?.cancel()
        // Reloads of an already shown list stay silent instead of flashing a full-screen spinner.
        _uiState.update {
            it.copy(isRefreshing = pullToRefresh, isLoading = !pullToRefresh && !it.hasLoaded, error = null)
        }
        loadJob = viewModelScope.launch {
            val result = getTodosForPeriod(period)
            if (period != _uiState.value.period) return@launch
            when (result) {
                is DomainResult.Success -> _uiState.update {
                    it.copy(
                        todos = result.value.sortedBy { todo -> todo.dueAt },
                        isLoading = false,
                        isRefreshing = false,
                        hasLoaded = true,
                        error = null,
                    )
                }

                is DomainResult.Failure -> {
                    val hadContent = _uiState.value.hasLoaded
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = result.error.takeIf { !hadContent })
                    }
                    // An expired session is handled by the auth gate, which navigates to sign-in.
                    if (hadContent && result.error != DomainError.Unauthorized) {
                        _events.trySend(TodoListEvent.ShowError(result.error))
                    }
                }
            }
        }
    }
}
