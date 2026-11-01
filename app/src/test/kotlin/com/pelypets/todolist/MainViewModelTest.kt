package com.pelypets.todolist

import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.usecase.ObserveSessionUseCase
import com.pelypets.todolist.domain.usecase.RestoreSessionUseCase
import com.pelypets.todolist.testing.FakeSessionRepository
import com.pelypets.todolist.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class MainViewModelTest {

    /** Coroutines launched on the main thread wait for the test, as they would wait for the next frame. */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakeSessionRepository(stored = SessionState.LOGGED_IN)

    private fun viewModel() = MainViewModel(ObserveSessionUseCase(repository), RestoreSessionUseCase(repository))

    @Test
    fun `stored session is read without blocking the screen`() = runTest {
        val viewModel = viewModel()
        assertEquals(SessionState.UNKNOWN, viewModel.session.value)

        advanceUntilIdle()

        assertEquals(SessionState.LOGGED_IN, viewModel.session.value)
    }

    @Test
    fun `a restored back stack can read the session right away`() = runTest {
        val viewModel = viewModel()

        viewModel.restoreSessionNow()

        assertEquals(SessionState.LOGGED_IN, viewModel.session.value)
    }
}
