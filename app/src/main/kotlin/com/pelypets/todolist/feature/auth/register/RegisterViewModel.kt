package com.pelypets.todolist.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.usecase.RegisterUseCase
import com.pelypets.todolist.feature.auth.confirmPasswordErrorFrom
import com.pelypets.todolist.feature.auth.emailErrorFrom
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

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val confirmPasswordError: ValidationError? = null,
    val isSubmitting: Boolean = false,
    val error: RegisterError? = null,
)

sealed interface RegisterError {
    data object EmailTaken : RegisterError
    data class Other(val error: DomainError) : RegisterError
}

sealed interface RegisterEvent {
    data object NavigateBack : RegisterEvent
}

@HiltViewModel
class RegisterViewModel @Inject constructor(private val register: RegisterUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events: Flow<RegisterEvent> = _events.receiveAsFlow()

    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value, passwordError = null, error = null) }

    fun onConfirmPasswordChanged(value: String) =
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, error = null) }

    fun onSubmitClicked() {
        val state = _uiState.value
        if (state.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = register(state.email, state.password, state.confirmPassword)
            _uiState.update { current ->
                val idle = current.copy(isSubmitting = false)
                when (result) {
                    is DomainResult.Success -> idle
                    is DomainResult.Failure -> idle.withError(result.error)
                }
            }
        }
    }

    fun onBackClicked() {
        if (!_uiState.value.isSubmitting) _events.trySend(RegisterEvent.NavigateBack)
    }

    private fun RegisterUiState.withError(error: DomainError): RegisterUiState = when {
        error is DomainError.Validation && error.errors.isNotEmpty() -> copy(
            emailError = emailErrorFrom(error.errors),
            passwordError = passwordErrorFrom(error.errors),
            confirmPasswordError = confirmPasswordErrorFrom(error.errors),
        )

        error == DomainError.Conflict -> copy(error = RegisterError.EmailTaken)

        else -> copy(error = RegisterError.Other(error))
    }
}
