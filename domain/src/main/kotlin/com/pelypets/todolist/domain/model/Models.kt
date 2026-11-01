package com.pelypets.todolist.domain.model

import java.time.Instant

data class User(val id: String, val email: String)

enum class TodoStatus { TODO, IN_PROGRESS, DONE }

data class Todo(val id: String, val title: String, val description: String, val dueAt: Instant, val status: TodoStatus)

data class TodoDraft(val title: String, val description: String, val dueAt: Instant, val status: TodoStatus)

enum class Period { TODAY, WEEK, MONTH }

/** Half-open interval `[from, to)`. */
data class TimeRange(val from: Instant, val to: Instant) {
    init {
        require(from < to) { "from must be before to" }
    }

    operator fun contains(instant: Instant): Boolean = instant >= from && instant < to
}

enum class SessionState {
    /** The stored tokens have not been read yet. */
    UNKNOWN,
    LOGGED_IN,
    LOGGED_OUT,
}
