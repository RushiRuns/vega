package com.vega.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class RecurrenceUtilsTest {

    @Test
    fun testDailyRecurrence() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0) // Friday
            set(Calendar.MILLISECOND, 0)
        }
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, "DAILY")
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 6, 12, 0, 0) // Saturday
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testWeekdaysRecurrenceFromFriday() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0) // Friday
            set(Calendar.MILLISECOND, 0)
        }
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, "WEEKDAYS")
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 8, 12, 0, 0) // Monday
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testWeekdaysRecurrenceFromThursday() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 4, 12, 0, 0) // Thursday
            set(Calendar.MILLISECOND, 0)
        }
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, "WEEKDAYS")
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0) // Friday
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testWeeklyRecurrence() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0) // Friday
            set(Calendar.MILLISECOND, 0)
        }
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, "WEEKLY")
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 12, 12, 0, 0) // Next Friday
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testMonthlyRecurrence() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0) // June 5th
            set(Calendar.MILLISECOND, 0)
        }
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, "MONTHLY")
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 5, 12, 0, 0) // July 5th
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }
}
