package com.pelypets.todolist.feature.auth

import com.pelypets.todolist.domain.error.ValidationError

private val emailErrors = setOf(ValidationError.EMAIL_BLANK, ValidationError.EMAIL_INVALID)
private val passwordErrors = setOf(
    ValidationError.PASSWORD_BLANK,
    ValidationError.PASSWORD_TOO_SHORT,
    ValidationError.PASSWORD_TOO_LONG,
)

internal fun emailErrorFrom(errors: List<ValidationError>): ValidationError? = errors.firstOrNull { it in emailErrors }

internal fun passwordErrorFrom(errors: List<ValidationError>): ValidationError? = errors.firstOrNull { it in passwordErrors }

internal fun confirmPasswordErrorFrom(errors: List<ValidationError>): ValidationError? =
    errors.firstOrNull { it == ValidationError.PASSWORD_MISMATCH }
