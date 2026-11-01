package com.pelypets.todolist.domain.usecase

import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.repository.AuthRepository
import com.pelypets.todolist.domain.repository.SessionRepository
import com.pelypets.todolist.domain.validation.CredentialsValidator
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class LoginWithEmailUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): DomainResult<Unit> {
        val errors = CredentialsValidator.validateLogin(email, password)
        if (errors.isNotEmpty()) return DomainResult.Failure(DomainError.Validation(errors))
        return authRepository.login(email.trim(), password)
    }
}

class RegisterUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, confirmPassword: String): DomainResult<Unit> {
        val errors = CredentialsValidator.validateRegistration(email, password, confirmPassword)
        if (errors.isNotEmpty()) return DomainResult.Failure(DomainError.Validation(errors))
        return authRepository.register(email.trim(), password)
    }
}

class LoginWithGoogleUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(idToken: String): DomainResult<Unit> = if (idToken.isBlank()) {
        DomainResult.Failure(DomainError.Unauthorized)
    } else {
        authRepository.loginWithGoogle(idToken)
    }
}

class LogoutUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): DomainResult<Unit> = authRepository.logout()
}

class ObserveSessionUseCase @Inject constructor(private val sessionRepository: SessionRepository) {
    operator fun invoke(): StateFlow<SessionState> = sessionRepository.session
}

class RestoreSessionUseCase @Inject constructor(private val sessionRepository: SessionRepository) {
    suspend operator fun invoke() = sessionRepository.restore()
}
