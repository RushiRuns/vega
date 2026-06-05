package com.vega.parser

import com.vega.data.database.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NaturalLanguageParserTest {

    private lateinit var parser: NaturalLanguageParser
    
    // Use a fixed "now" timestamp: Friday, June 5, 2026, 10:00:00 AM
    private val baseTime: Long by lazy {
        Calendar.getInstance(TimeZone.getDefault()).apply {
            set(2026, Calendar.JUNE, 5, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Before
    fun setUp() {
        parser = NaturalLanguageParser()
    }

    @Test
    fun testSimpleTitle() {
        val result = parser.parse("Buy milk", baseTime)
        assertEquals("Buy milk", result.title)
        assertNull(result.dueDate)
        assertEquals(TaskPriority.NONE, result.priority)
    }

    @Test
    fun testTitleWithDate() {
        val result = parser.parse("Buy milk tomorrow", baseTime)
        assertEquals("Buy milk", result.title)
        
        // Expected: Saturday, June 6, 2026, 00:00:00
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = baseTime
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expectedCal.timeInMillis, result.dueDate)
        assertEquals(TaskPriority.NONE, result.priority)
    }

    @Test
    fun testTitleWithPriority() {
        val result = parser.parse("Fix bug high", baseTime)
        assertEquals("Fix bug", result.title)
        assertNull(result.dueDate)
        assertEquals(TaskPriority.HIGH, result.priority)
    }

    @Test
    fun testTitleWithDateAndTime() {
        val result = parser.parse("Call boss tomorrow 3pm", baseTime)
        assertEquals("Call boss", result.title)
        
        // Expected: Saturday, June 6, 2026, 15:00:00
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = baseTime
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expectedCal.timeInMillis, result.dueDate)
    }

    @Test
    fun testPriorityASAP() {
        val result = parser.parse("Submit report asap", baseTime)
        assertEquals("Submit report", result.title)
        assertEquals(TaskPriority.HIGH, result.priority)
    }

    @Test
    fun testPriorityCritical() {
        val result = parser.parse("Fix server critical", baseTime)
        assertEquals("Fix server", result.title)
        assertEquals(TaskPriority.HIGH, result.priority)
    }

    @Test
    fun testPriorityMedium() {
        val result = parser.parse("Read book medium", baseTime)
        assertEquals("Read book", result.title)
        assertEquals(TaskPriority.MEDIUM, result.priority)
    }

    @Test
    fun testPriorityNormal() {
        val result = parser.parse("Buy groceries normal", baseTime)
        assertEquals("Buy groceries", result.title)
        assertEquals(TaskPriority.MEDIUM, result.priority)
    }

    @Test
    fun testPriorityLow() {
        val result = parser.parse("Walk dog low", baseTime)
        assertEquals("Walk dog", result.title)
        assertEquals(TaskPriority.LOW, result.priority)
    }

    @Test
    fun testPriorityWhenever() {
        val result = parser.parse("Clean garage whenever", baseTime)
        assertEquals("Clean garage", result.title)
        assertEquals(TaskPriority.LOW, result.priority)
    }

    @Test
    fun testDateNextMonday() {
        val result = parser.parse("Submit weekly status next Monday", baseTime)
        assertEquals("Submit weekly status", result.title)
        
        // baseTime is Friday, June 5, 2026. Next Monday is June 8, 2026.
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = baseTime
            set(Calendar.DAY_OF_MONTH, 8)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expectedCal.timeInMillis, result.dueDate)
    }

    @Test
    fun testDateNextFriday() {
        val result = parser.parse("Weekly sync next friday 10:30", baseTime)
        assertEquals("Weekly sync", result.title)
        
        // baseTime is Friday, June 5, 2026. Next Friday is June 12, 2026.
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = baseTime
            set(Calendar.DAY_OF_MONTH, 12)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expectedCal.timeInMillis, result.dueDate)
    }

    @Test
    fun testDateInNDays() {
        val result = parser.parse("File taxes in 3 days", baseTime)
        assertEquals("File taxes", result.title)
        
        // baseTime is Friday, June 5, 2026. +3 days is Monday, June 8, 2026.
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = baseTime
            add(Calendar.DAY_OF_YEAR, 3)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expectedCal.timeInMillis, result.dueDate)
    }

    @Test
    fun testDateMMDDYYYY() {
        val result = parser.parse("Doctor appointment 12/25/2026 3:30pm", baseTime)
        assertEquals("Doctor appointment", result.title)
        
        val expectedCal = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 25, 15, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expectedCal.timeInMillis, result.dueDate)
    }

    @Test
    fun testAmbiguousDateOnlyTomorrow() {
        // "Tomorrow" alone is ambiguous because title would be empty.
        // It fallback to keeping "Tomorrow" as title and dueDate as null.
        val result = parser.parse("Tomorrow", baseTime)
        assertEquals("Tomorrow", result.title)
        assertNull(result.dueDate)
    }

    @Test
    fun testAmbiguousDateOnlyNextMonday() {
        val result = parser.parse("Next Monday", baseTime)
        assertEquals("Next Monday", result.title)
        assertNull(result.dueDate)
    }

    @Test
    fun testTimeFormats1030() {
        val result = parser.parse("Call parent 10:30", baseTime)
        assertEquals("Call parent", result.title)
        
        // Expected: Friday, June 5, 2026, 10:30:00
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = baseTime
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expectedCal.timeInMillis, result.dueDate)
    }

    @Test
    fun testEdgeCaseEmpty() {
        val result = parser.parse("", baseTime)
        assertNull(result.title)
        assertNull(result.dueDate)
        assertEquals(TaskPriority.NONE, result.priority)
    }

    @Test
    fun testEdgeCaseSpecialChars() {
        val result = parser.parse("!!! Buy bread !!!", baseTime)
        assertEquals("!!! Buy bread !!!", result.title)
        assertNull(result.dueDate)
    }

    @Test
    fun testMultipleKeywords() {
        val result = parser.parse("urgent fix bug asap", baseTime)
        assertEquals("fix bug", result.title)
        assertEquals(TaskPriority.HIGH, result.priority)
    }

    @Test
    fun testConflictingInputs() {
        val result = parser.parse("urgent normal priority task", baseTime)
        // urgent = HIGH, normal = MEDIUM. HIGH should take precedence or first.
        assertEquals(TaskPriority.HIGH, result.priority)
        assertEquals("priority task", result.title)
    }

    @Test
    fun testPrepositionStripping() {
        // at
        val r1 = parser.parse("Buy milk tomorrow at 10am", baseTime)
        assertEquals("Buy milk", r1.title)

        // by
        val r2 = parser.parse("Submit report by next Monday", baseTime)
        assertEquals("Submit report", r2.title)

        // on
        val r3 = parser.parse("Doctor appointment on 12/25/2026", baseTime)
        assertEquals("Doctor appointment", r3.title)

        // for
        val r4 = parser.parse("Task for tomorrow", baseTime)
        assertEquals("Task", r4.title)

        // in
        val r5 = parser.parse("Call parent in 3 days", baseTime)
        assertEquals("Call parent", r5.title)

        // to
        val r6 = parser.parse("Go to tomorrow", baseTime)
        assertEquals("Go", r6.title)
        
        // preposition not at end should not be stripped
        val r7 = parser.parse("Buy milk at supermarket tomorrow", baseTime)
        assertEquals("Buy milk at supermarket", r7.title)
    }
}
