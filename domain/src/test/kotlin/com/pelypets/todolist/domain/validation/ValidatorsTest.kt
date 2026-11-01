package com.pelypets.todolist.domain.validation

import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidatorsTest {

    @Test
    fun `email is trimmed and checked for format`() {
        assertEquals(ValidationError.EMAIL_BLANK, CredentialsValidator.validateEmail("   "))
        assertEquals(ValidationError.EMAIL_INVALID, CredentialsValidator.validateEmail("no-at-sign"))
        assertNull(CredentialsValidator.validateEmail("  user@example.com "))
    }

    @Test
    fun `registration reports every invalid field`() {
        val errors = CredentialsValidator.validateRegistration("bad", "short", "other")
        assertEquals(
            listOf(ValidationError.EMAIL_INVALID, ValidationError.PASSWORD_TOO_SHORT, ValidationError.PASSWORD_MISMATCH),
            errors,
        )
    }

    @Test
    fun `login only requires a password to be present`() {
        assertTrue(CredentialsValidator.validateLogin("user@example.com", "x").isEmpty())
    }

    @Test
    fun `todo title must not be blank and description has a limit`() {
        val draft = TodoDraft(" ", "a".repeat(TodoValidator.DESCRIPTION_MAX_LENGTH + 1), Instant.EPOCH, TodoStatus.TODO)
        assertEquals(listOf(ValidationError.TITLE_BLANK, ValidationError.DESCRIPTION_TOO_LONG), TodoValidator.validate(draft))
    }
}
