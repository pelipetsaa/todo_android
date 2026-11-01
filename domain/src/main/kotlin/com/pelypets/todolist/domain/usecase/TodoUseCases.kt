package com.pelypets.todolist.domain.usecase

import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.period.PeriodRangeCalculator
import com.pelypets.todolist.domain.repository.TodoRepository
import com.pelypets.todolist.domain.validation.TodoValidator
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetTodosForPeriodUseCase @Inject constructor(
    private val todoRepository: TodoRepository,
    private val rangeCalculator: PeriodRangeCalculator,
) {
    suspend operator fun invoke(period: Period): DomainResult<List<Todo>> =
        todoRepository.getTodos(rangeCalculator.rangeFor(period))
}

class GetTodoUseCase @Inject constructor(private val todoRepository: TodoRepository) {
    suspend operator fun invoke(id: String): DomainResult<Todo> = todoRepository.getTodo(id)
}

/** Creates a todo when [id] is null, otherwise updates the existing one. */
class SaveTodoUseCase @Inject constructor(private val todoRepository: TodoRepository) {
    suspend operator fun invoke(id: String?, draft: TodoDraft): DomainResult<Todo> {
        val errors = TodoValidator.validate(draft)
        if (errors.isNotEmpty()) return DomainResult.Failure(DomainError.Validation(errors))
        val normalized = draft.copy(title = draft.title.trim())
        return if (id == null) todoRepository.create(normalized) else todoRepository.update(id, normalized)
    }
}

class DeleteTodoUseCase @Inject constructor(private val todoRepository: TodoRepository) {
    suspend operator fun invoke(id: String): DomainResult<Unit> = todoRepository.delete(id)
}

/** Default due date for a new todo: the next full hour in the user's time zone. */
class DefaultDueDateUseCase @Inject constructor(private val clock: Clock) {
    operator fun invoke(): Instant = LocalDateTime.now(clock)
        .truncatedTo(ChronoUnit.HOURS)
        .plusHours(1)
        .atZone(clock.zone)
        .toInstant()
}
