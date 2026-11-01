package com.pelypets.todolist.domain.validation

import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.TodoDraft

object CredentialsValidator {
    const val EMAIL_MAX_LENGTH = 254
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_MAX_LENGTH = 128

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validateEmail(email: String): ValidationError? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationError.EMAIL_BLANK
            trimmed.length > EMAIL_MAX_LENGTH || !emailRegex.matches(trimmed) -> ValidationError.EMAIL_INVALID
            else -> null
        }
    }

    fun validateNewPassword(password: String): ValidationError? = when {
        password.isEmpty() -> ValidationError.PASSWORD_BLANK
        password.length < PASSWORD_MIN_LENGTH -> ValidationError.PASSWORD_TOO_SHORT
        password.length > PASSWORD_MAX_LENGTH -> ValidationError.PASSWORD_TOO_LONG
        else -> null
    }

    fun validateLogin(email: String, password: String): List<ValidationError> = listOfNotNull(
        validateEmail(email),
        if (password.isEmpty()) ValidationError.PASSWORD_BLANK else null,
    )

    fun validateRegistration(email: String, password: String, confirmPassword: String): List<ValidationError> = listOfNotNull(
        validateEmail(email),
        validateNewPassword(password),
        if (password != confirmPassword) ValidationError.PASSWORD_MISMATCH else null,
    )
}

object TodoValidator {
    const val TITLE_MAX_LENGTH = 200
    const val DESCRIPTION_MAX_LENGTH = 2000

    fun validate(draft: TodoDraft): List<ValidationError> {
        val title = draft.title.trim()
        return listOfNotNull(
            when {
                title.isEmpty() -> ValidationError.TITLE_BLANK
                title.length > TITLE_MAX_LENGTH -> ValidationError.TITLE_TOO_LONG
                else -> null
            },
            if (draft.description.length > DESCRIPTION_MAX_LENGTH) ValidationError.DESCRIPTION_TOO_LONG else null,
        )
    }
}
