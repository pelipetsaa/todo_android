package com.pelypets.todolist.domain.period

import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.TimeRange
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/** Weeks start on Monday. Boundaries are local midnights, so DST changes are handled by the time zone. */
class PeriodRangeCalculator @Inject constructor(private val clock: Clock) {

    fun rangeFor(period: Period): TimeRange {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val (start, endExclusive) = when (period) {
            Period.TODAY -> today to today.plusDays(1)

            Period.WEEK -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                monday to monday.plusWeeks(1)
            }

            Period.MONTH -> {
                val first = today.withDayOfMonth(1)
                first to first.plusMonths(1)
            }
        }
        return TimeRange(
            from = start.atStartOfDay(zone).toInstant(),
            to = endExclusive.atStartOfDay(zone).toInstant(),
        )
    }
}
