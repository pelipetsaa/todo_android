package com.pelypets.todolist.feature.todolist

import app.cash.turbine.test
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoStatus
import com.pelypets.todolist.domain.period.PeriodRangeCalculator
import com.pelypets.todolist.domain.usecase.GetTodosForPeriodUseCase
import com.pelypets.todolist.domain.usecase.LogoutUseCase
import com.pelypets.todolist.testing.FakeAuthRepository
import com.pelypets.todolist.testing.FakeTodoRepository
import com.pelypets.todolist.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodoListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val todoRepository = FakeTodoRepository()
    private val authRepository = FakeAuthRepository()
    private val clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC)
    private val viewModel = TodoListViewModel(
        GetTodosForPeriodUseCase(todoRepository, PeriodRangeCalculator(clock)),
        LogoutUseCase(authRepository),
    )

    private fun todo(id: String, dueAt: String) = Todo(id, "Todo $id", "", Instant.parse(dueAt), TodoStatus.TODO)

    @Test
    fun `loaded todos are sorted by due time`() {
        todoRepository.todos += listOf(todo("late", "2026-11-01T18:00:00Z"), todo("early", "2026-11-01T08:00:00Z"))

        viewModel.onScreenShown()

        val state = viewModel.uiState.value
        assertEquals(listOf("early", "late"), state.todos.map { it.id })
        assertTrue(state.hasLoaded)
    }

    @Test
    fun `first load failure is shown full screen`() {
        todoRepository.failWith = DomainError.Network

        viewModel.onScreenShown()

        assertEquals(DomainError.Network, viewModel.uiState.value.error)
    }

    @Test
    fun `refresh failure keeps the list and shows a snackbar`() = runTest {
        todoRepository.todos += todo("1", "2026-11-01T12:00:00Z")
        viewModel.onScreenShown()
        todoRepository.failWith = DomainError.Network

        viewModel.events.test {
            viewModel.onRefresh()
            assertEquals(TodoListEvent.ShowError(DomainError.Network), awaitItem())
        }
        assertNull(viewModel.uiState.value.error)
        assertEquals(1, viewModel.uiState.value.todos.size)
    }

    @Test
    fun `switching the period resets the list`() {
        todoRepository.todos += todo("1", "2026-11-01T12:00:00Z")
        viewModel.onScreenShown()

        viewModel.onPeriodSelected(Period.WEEK)

        assertEquals(Period.WEEK, viewModel.uiState.value.period)
        assertTrue(viewModel.uiState.value.hasLoaded)
    }

    @Test
    fun `logout goes through the repository`() {
        viewModel.onLogoutClicked()
        assertTrue(authRepository.loggedOut)
    }
}
