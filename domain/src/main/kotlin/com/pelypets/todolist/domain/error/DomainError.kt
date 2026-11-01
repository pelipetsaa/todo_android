package com.pelypets.todolist.domain.error

sealed interface DomainError {
    data object Network : DomainError
    data object Unauthorized : DomainError
    data object NotFound : DomainError

    /** The resource conflicts with an existing one, e.g. the email is already registered. */
    data object Conflict : DomainError

    /** The server throttled the client, e.g. after too many sign-in attempts. */
    data object TooManyRequests : DomainError

    data class Validation(val errors: List<ValidationError> = emptyList(), val serverMessage: String? = null) : DomainError

    data class Unknown(val message: String? = null) : DomainError
}

enum class ValidationError {
    EMAIL_BLANK,
    EMAIL_INVALID,
    PASSWORD_BLANK,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    PASSWORD_MISMATCH,
    TITLE_BLANK,
    TITLE_TOO_LONG,
    DESCRIPTION_TOO_LONG,
}
