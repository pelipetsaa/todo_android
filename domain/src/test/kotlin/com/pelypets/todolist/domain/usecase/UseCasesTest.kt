package com.pelypets.todolist.domain.usecase

import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus
import com.pelypets.todolist.domain.repository.AuthRepository
import com.pelypets.todolist.domain.repository.TodoRepository
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UseCasesTest {

    private class RecordingAuthRepository : AuthRepository {
        var loggedInWith: Pair<String, String>? = null
        override suspend fun login(email: String, password: String): DomainResult<Unit> {
            loggedInWith = email to password
            return DomainResult.Success(Unit)
        }
        override suspend fun register(email: String, password: String) = DomainResult.Success(Unit)
        override suspend fun loginWithGoogle(idToken: String) = DomainResult.Success(Unit)
        override suspend fun logout() = DomainResult.Success(Unit)
    }

    private class RecordingTodoRepository : TodoRepository {
        var created: TodoDraft? = null
        var updated: Pair<String, TodoDraft>? = null
        override suspend fun getTodos(range: TimeRange) = DomainResult.Success(emptyList<Todo>())
        override suspend fun getTodo(id: String): DomainResult<Todo> = DomainResult.Failure(DomainError.NotFound)
        override suspend fun create(draft: TodoDraft): DomainResult<Todo> {
            created = draft
            return DomainResult.Success(Todo("1", draft.title, draft.description, draft.dueAt, draft.status))
        }
        override suspend fun update(id: String, draft: TodoDraft): DomainResult<Todo> {
            updated = id to draft
            return DomainResult.Success(Todo(id, draft.title, draft.description, draft.dueAt, draft.status))
        }
        override suspend fun delete(id: String) = DomainResult.Success(Unit)
    }

    @Test
    fun `login trims the email and skips the repository on invalid input`() = runTest {
        val repository = RecordingAuthRepository()
        val login = LoginWithEmailUseCase(repository)

        val invalid = login("", "")
        assertIs<DomainResult.Failure>(invalid)
        assertNull(repository.loggedInWith)

        login(" user@example.com ", "secret")
        assertEquals("user@example.com" to "secret", repository.loggedInWith)
    }

    @Test
    fun `save creates without id and updates with id`() = runTest {
        val repository = RecordingTodoRepository()
        val save = SaveTodoUseCase(repository)
        val draft = TodoDraft("  Buy milk ", "", Instant.EPOCH, TodoStatus.TODO)

        save(null, draft)
        assertEquals("Buy milk", repository.created?.title)

        save("42", draft)
        assertEquals("42", repository.updated?.first)
    }

    @Test
    fun `save rejects an invalid draft`() = runTest {
        val result = SaveTodoUseCase(RecordingTodoRepository())(null, TodoDraft("", "", Instant.EPOCH, TodoStatus.TODO))
        val error = (result as DomainResult.Failure).error
        assertEquals(DomainError.Validation(listOf(ValidationError.TITLE_BLANK)), error)
    }

    @Test
    fun `default due date is the next full hour`() {
        val clock = Clock.fixed(Instant.parse("2026-11-01T10:42:13Z"), ZoneOffset.UTC)
        assertEquals(Instant.parse("2026-11-01T11:00:00Z"), DefaultDueDateUseCase(clock)())
    }
}
