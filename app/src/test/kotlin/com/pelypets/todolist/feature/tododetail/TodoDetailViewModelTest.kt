package com.pelypets.todolist.feature.tododetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoStatus
import com.pelypets.todolist.domain.usecase.DefaultDueDateUseCase
import com.pelypets.todolist.domain.usecase.DeleteTodoUseCase
import com.pelypets.todolist.domain.usecase.GetTodoUseCase
import com.pelypets.todolist.domain.usecase.SaveTodoUseCase
import com.pelypets.todolist.testing.FakeTodoRepository
import com.pelypets.todolist.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodoDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTodoRepository()
    private val clock = Clock.fixed(Instant.parse("2026-11-01T10:42:00Z"), ZoneOffset.UTC)

    private fun viewModel(todoId: String?) = TodoDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("todoId" to todoId)),
        getTodo = GetTodoUseCase(repository),
        saveTodo = SaveTodoUseCase(repository),
        deleteTodo = DeleteTodoUseCase(repository),
        defaultDueDate = DefaultDueDateUseCase(clock),
        clock = clock,
    )

    @Test
    fun `new todo defaults to the next full hour`() {
        val state = viewModel(todoId = null).uiState.value

        assertTrue(state.isNew)
        assertEquals(LocalDate.of(2026, 11, 1), state.date)
        assertEquals(LocalTime.of(11, 0), state.time)
    }

    @Test
    fun `existing todo is loaded into the form`() {
        repository.todos += Todo("7", "Call mom", "Sunday", Instant.parse("2026-11-03T18:30:00Z"), TodoStatus.IN_PROGRESS)

        val state = viewModel(todoId = "7").uiState.value

        assertEquals("Call mom", state.title)
        assertEquals(LocalTime.of(18, 30), state.time)
        assertEquals(TodoStatus.IN_PROGRESS, state.status)
        assertFalse(state.isLoading)
    }

    @Test
    fun `blank title is reported inline and nothing is saved`() {
        val viewModel = viewModel(todoId = null)

        viewModel.onSaveClicked()

        assertEquals(ValidationError.TITLE_BLANK, viewModel.uiState.value.titleError)
        assertEquals(null, repository.lastSaved)
    }

    @Test
    fun `saving combines date and time and closes the screen`() = runTest {
        val viewModel = viewModel(todoId = null)
        viewModel.onTitleChanged("Dentist")
        viewModel.onDateSelected(LocalDate.of(2026, 11, 5))
        viewModel.onTimeSelected(LocalTime.of(9, 15))

        viewModel.events.test {
            viewModel.onSaveClicked()
            assertEquals(TodoDetailEvent.Close, awaitItem())
        }
        assertEquals(Instant.parse("2026-11-05T09:15:00Z"), repository.lastSaved?.second?.dueAt)
    }

    @Test
    fun `failed delete re-enables the form and shows the error`() = runTest {
        repository.todos += Todo("7", "Call mom", "", Instant.parse("2026-11-03T18:30:00Z"), TodoStatus.TODO)
        val viewModel = viewModel(todoId = "7")
        repository.failWith = DomainError.Network

        viewModel.events.test {
            viewModel.onDeleteConfirmed()
            assertEquals(TodoDetailEvent.ShowError(DomainError.Network), awaitItem())
        }
        assertTrue(viewModel.uiState.value.canEdit)
    }
}
