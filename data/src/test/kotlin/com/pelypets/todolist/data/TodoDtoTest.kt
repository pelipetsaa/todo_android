package com.pelypets.todolist.data

import com.pelypets.todolist.data.remote.DataJson
import com.pelypets.todolist.data.remote.dto.TodoDto
import org.junit.Test
import kotlin.test.assertEquals

class TodoDtoTest {

    @Test
    fun `todo is decoded without the server bookkeeping fields`() {
        val dto = DataJson.decodeFromString<TodoDto>(
            """{"id":"1","title":"Milk","description":"","dueAt":"2026-11-01T10:00:00Z","status":"TODO"}""",
        )

        assertEquals("Milk", dto.title)
    }
}
