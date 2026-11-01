package com.pelypets.todolist.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.usecase.LoginWithEmailUseCase
import com.pelypets.todolist.domain.usecase.LoginWithGoogleUseCase
import com.pelypets.todolist.feature.auth.emailErrorFrom
import com.pelypets.todolist.feature.auth.google.GoogleSignInResult
import com.pelypets.todolist.feature.auth.passwordErrorFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val isSubmitting: Boolean = false,
    val isGoogleInProgress: Boolean = false,
    val error: LoginError? = null,
) {
    val isBusy: Boolean get() = isSubmitting || isGoogleInProgress
}

sealed interface LoginError {
    data object InvalidCredentials : LoginError
    data object GoogleFailed : LoginError
    data class Other(val error: DomainError) : LoginError
}

sealed interface LoginEvent {
    data object LaunchGoogleSignIn : LoginEvent
    data object NavigateToRegister : LoginEvent
}

/** A successful sign-in starts the session; navigation to the todo list is driven by the session, not by this screen. */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithEmail: LoginWithEmailUseCase,
    private val loginWithGoogle: LoginWithGoogleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value, passwordError = null, error = null) }

    fun onSubmitClicked() {
        val state = _uiState.value
        if (state.isBusy) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            handleResult(loginWithEmail(state.email, state.password), viaGoogle = false)
        }
    }

    fun onGoogleClicked() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isGoogleInProgress = true, error = null) }
        _events.trySend(LoginEvent.LaunchGoogleSignIn)
    }

    fun onGoogleResult(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> viewModelScope.launch {
                handleResult(loginWithGoogle(result.idToken), viaGoogle = true)
            }

            GoogleSignInResult.Cancelled -> _uiState.update { it.copy(isGoogleInProgress = false) }

            is GoogleSignInResult.Failure -> _uiState.update { it.copy(isGoogleInProgress = false, error = LoginError.GoogleFailed) }
        }
    }

    fun onRegisterClicked() {
        if (!_uiState.value.isBusy) _events.trySend(LoginEvent.NavigateToRegister)
    }

    private fun handleResult(result: DomainResult<Unit>, viaGoogle: Boolean) {
        _uiState.update { state ->
            val idle = state.copy(isSubmitting = false, isGoogleInProgress = false)
            when (result) {
                is DomainResult.Success -> idle
                is DomainResult.Failure -> idle.withError(result.error, viaGoogle)
            }
        }
    }

    private fun LoginUiState.withError(error: DomainError, viaGoogle: Boolean): LoginUiState = when {
        error is DomainError.Validation && error.errors.isNotEmpty() -> copy(
            emailError = emailErrorFrom(error.errors),
            passwordError = passwordErrorFrom(error.errors),
        )

        error == DomainError.Unauthorized -> copy(error = if (viaGoogle) LoginError.GoogleFailed else LoginError.InvalidCredentials)

        else -> copy(error = LoginError.Other(error))
    }
}
