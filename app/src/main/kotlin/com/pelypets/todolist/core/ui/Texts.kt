package com.pelypets.todolist.core.ui

import androidx.annotation.StringRes
import com.pelypets.todolist.R
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.TodoStatus

@get:StringRes
val DomainError.messageRes: Int
    get() = when (this) {
        DomainError.Network -> R.string.error_network
        DomainError.Unauthorized -> R.string.error_unauthorized
        DomainError.NotFound -> R.string.error_not_found
        DomainError.Conflict -> R.string.error_conflict
        DomainError.TooManyRequests -> R.string.error_too_many_requests
        DomainError.ServerUnavailable -> R.string.error_server_unavailable
        is DomainError.Validation -> R.string.error_validation
        is DomainError.Unknown -> R.string.error_unknown
    }

@get:StringRes
val ValidationError.messageRes: Int
    get() = when (this) {
        ValidationError.EMAIL_BLANK -> R.string.validation_email_blank
        ValidationError.EMAIL_INVALID -> R.string.validation_email_invalid
        ValidationError.PASSWORD_BLANK -> R.string.validation_password_blank
        ValidationError.PASSWORD_TOO_SHORT -> R.string.validation_password_too_short
        ValidationError.PASSWORD_TOO_LONG -> R.string.validation_password_too_long
        ValidationError.PASSWORD_MISMATCH -> R.string.validation_password_mismatch
        ValidationError.TITLE_BLANK -> R.string.validation_title_blank
        ValidationError.TITLE_TOO_LONG -> R.string.validation_title_too_long
        ValidationError.DESCRIPTION_TOO_LONG -> R.string.validation_description_too_long
    }

@get:StringRes
val TodoStatus.labelRes: Int
    get() = when (this) {
        TodoStatus.TODO -> R.string.status_todo
        TodoStatus.IN_PROGRESS -> R.string.status_in_progress
        TodoStatus.DONE -> R.string.status_done
    }

@get:StringRes
val Period.labelRes: Int
    get() = when (this) {
        Period.TODAY -> R.string.period_today
        Period.WEEK -> R.string.period_week
        Period.MONTH -> R.string.period_month
    }

@get:StringRes
val Period.emptyRes: Int
    get() = when (this) {
        Period.TODAY -> R.string.empty_today
        Period.WEEK -> R.string.empty_week
        Period.MONTH -> R.string.empty_month
    }
