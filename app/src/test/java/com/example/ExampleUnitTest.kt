package com.example

import com.example.data.model.NotebookState
import com.example.data.model.TaskItem
import com.example.util.JalaliCalendar
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testJalaliDateConversion() {
        val today = JalaliCalendar.today()
        assertNotNull(today)
        assertTrue(today.matches(Regex("\\d{4}/\\d{2}/\\d{2}")))

        val normalized = JalaliCalendar.normalizeDate("1405/01/15")
        assertEquals("1405/01/15", normalized)

        val faNum = JalaliCalendar.toFa("12345")
        assertEquals("۱۲۳۴۵", faNum)
    }

    @Test
    fun testNotebookStateSerialization() {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(NotebookState::class.java)

        val state = NotebookState(
            task = listOf(TaskItem(id = "1", t = "تست تسک", st = "todo"))
        )

        val json = adapter.toJson(state)
        assertTrue(json.contains("تست تسک"))

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(1, deserialized?.task?.size)
        assertEquals("تست تسک", deserialized?.task?.first()?.t)
    }
}
