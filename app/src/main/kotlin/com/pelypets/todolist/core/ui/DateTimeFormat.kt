package com.pelypets.todolist.core.ui

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "Sun, 1 Nov 2026" / "вс, 1 нояб. 2026": the formatter follows the default locale at call time. */
fun LocalDate.formatted(): String = format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))

fun LocalTime.formatted(): String = format(timeFormatter)
