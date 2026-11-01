package com.pelypets.todolist.feature.auth

import app.cash.turbine.test
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.usecase.LoginWithEmailUseCase
import com.pelypets.todolist.domain.usecase.LoginWithGoogleUseCase
import com.pelypets.todolist.feature.auth.google.GoogleSignInResult
import com.pelypets.todolist.feature.auth.login.LoginError
import com.pelypets.todolist.feature.auth.login.LoginEvent
import com.pelypets.todolist.feature.auth.login.LoginViewModel
import com.pelypets.todolist.testing.FakeAuthRepository
import com.pelypets.todolist.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel = LoginViewModel(LoginWithEmailUseCase(repository), LoginWithGoogleUseCase(repository))

    @Test
    fun `empty form shows inline errors`() {
        viewModel.onSubmitClicked()

        val state = viewModel.uiState.value
        assertEquals(ValidationError.EMAIL_BLANK, state.emailError)
        assertEquals(ValidationError.PASSWORD_BLANK, state.passwordError)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `typing clears the field error`() {
        viewModel.onSubmitClicked()
        viewModel.onEmailChanged("user@example.com")

        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `unauthorized response means invalid credentials`() {
        repository.loginResult = DomainResult.Failure(DomainError.Unauthorized)
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("wrong-password")

        viewModel.onSubmitClicked()

        assertEquals(LoginError.InvalidCredentials, viewModel.uiState.value.error)
    }

    @Test
    fun `google button launches the sign-in sheet and a cancel unblocks the form`() = runTest {
        viewModel.events.test {
            viewModel.onGoogleClicked()
            assertEquals(LoginEvent.LaunchGoogleSignIn, awaitItem())
        }
        assertEquals(true, viewModel.uiState.value.isGoogleInProgress)

        viewModel.onGoogleResult(GoogleSignInResult.Cancelled)

        assertFalse(viewModel.uiState.value.isBusy)
        assertNull(viewModel.uiState.value.error)
    }
}
