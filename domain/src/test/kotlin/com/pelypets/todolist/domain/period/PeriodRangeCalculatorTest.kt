package com.pelypets.todolist.domain.period

import com.pelypets.todolist.domain.model.Period
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class PeriodRangeCalculatorTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private fun calculatorAt(local: String) =
        PeriodRangeCalculator(Clock.fixed(ZonedDateTime.parse("$local[Europe/Berlin]").toInstant(), zone))

    private fun midnight(date: String): Instant = ZonedDateTime.parse("${date}T00:00+01:00[Europe/Berlin]").toInstant()

    @Test
    fun `today spans from local midnight to the next one`() {
        val range = calculatorAt("2026-11-05T15:30+01:00").rangeFor(Period.TODAY)
        assertEquals(midnight("2026-11-05"), range.from)
        assertEquals(midnight("2026-11-06"), range.to)
    }

    @Test
    fun `week starts on monday`() {
        // 2026-11-01 is a Sunday.
        val range = calculatorAt("2026-11-01T10:00+01:00").rangeFor(Period.WEEK)
        assertEquals(midnight("2026-10-26"), range.from)
        assertEquals(midnight("2026-11-02"), range.to)
    }

    @Test
    fun `month crosses the year boundary`() {
        val range = calculatorAt("2026-12-15T09:00+01:00").rangeFor(Period.MONTH)
        assertEquals(midnight("2026-12-01"), range.from)
        assertEquals(midnight("2027-01-01"), range.to)
    }
}
