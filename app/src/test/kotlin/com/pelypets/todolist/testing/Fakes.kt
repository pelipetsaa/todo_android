package com.pelypets.todolist.testing

import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.repository.AuthRepository
import com.pelypets.todolist.domain.repository.SessionRepository
import com.pelypets.todolist.domain.repository.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeAuthRepository : AuthRepository {
    var loginResult: DomainResult<Unit> = DomainResult.Success(Unit)
    var loggedOut = false

    override suspend fun login(email: String, password: String) = loginResult
    override suspend fun register(email: String, password: String) = loginResult
    override suspend fun loginWithGoogle(idToken: String) = loginResult
    override suspend fun logout(): DomainResult<Unit> {
        loggedOut = true
        return DomainResult.Success(Unit)
    }
}

class FakeSessionRepository(private val stored: SessionState) : SessionRepository {
    override val session = MutableStateFlow(SessionState.UNKNOWN)

    override suspend fun restore() {
        if (session.value == SessionState.UNKNOWN) session.value = stored
    }
}

class FakeTodoRepository : TodoRepository {
    val todos = mutableListOf<Todo>()
    var failWith: DomainError? = null
    var lastSaved: Pair<String?, TodoDraft>? = null
    var deletedId: String? = null

    private fun <T> result(value: () -> T): DomainResult<T> =
        failWith?.let { DomainResult.Failure(it) } ?: DomainResult.Success(value())

    override suspend fun getTodos(range: TimeRange) = result { todos.toList() }

    override suspend fun getTodo(id: String): DomainResult<Todo> =
        failWith?.let { DomainResult.Failure(it) }
            ?: todos.firstOrNull { it.id == id }?.let { DomainResult.Success(it) }
            ?: DomainResult.Failure(DomainError.NotFound)

    override suspend fun create(draft: TodoDraft) = result {
        lastSaved = null to draft
        Todo("new", draft.title, draft.description, draft.dueAt, draft.status)
    }

    override suspend fun update(id: String, draft: TodoDraft) = result {
        lastSaved = id to draft
        Todo(id, draft.title, draft.description, draft.dueAt, draft.status)
    }

    override suspend fun delete(id: String) = result { deletedId = id }
}
