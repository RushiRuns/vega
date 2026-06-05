package com.vega.utils

import com.vega.data.database.RecurrenceRule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class RecurrenceUtilsTest {

    @Test
    fun testDailyRecurrenceCustomInterval() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0) // Friday
            set(Calendar.MILLISECOND, 0)
        }
        val rule = RecurrenceRule(frequency = "DAILY", interval = 2)
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, rule)
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 7, 12, 0, 0) // Sunday
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testWeeklyMultiDayRecurrenceSameWeek() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 8, 12, 0, 0) // Monday
            set(Calendar.MILLISECOND, 0)
        }
        val rule = RecurrenceRule(
            frequency = "WEEKLY",
            interval = 1,
            weekdays = listOf(Calendar.MONDAY, Calendar.FRIDAY)
        )
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, rule)
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 12, 12, 0, 0) // Friday of same week
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testWeeklyMultiDayRecurrenceNextWeekHop() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 12, 12, 0, 0) // Friday
            set(Calendar.MILLISECOND, 0)
        }
        val rule = RecurrenceRule(
            frequency = "WEEKLY",
            interval = 2,
            weekdays = listOf(Calendar.MONDAY, Calendar.FRIDAY)
        )
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, rule)
        
        // Expected: Should skip 2 weeks, landing on Monday of week 3 (June 22, 2026)
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 22, 12, 0, 0) // Monday of week 3
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testMonthlyDayOfWeekFirstFriday() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0) // Friday June 5
            set(Calendar.MILLISECOND, 0)
        }
        val rule = RecurrenceRule(
            frequency = "MONTHLY",
            interval = 1,
            monthlyType = "DAY_OF_WEEK",
            dayOfWeek = Calendar.FRIDAY,
            dayOfWeekOccurrence = 1 // First Friday
        )
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, rule)
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 3, 12, 0, 0) // First Friday of July is July 3rd
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testMonthlyDayOfWeekLastFriday() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val rule = RecurrenceRule(
            frequency = "MONTHLY",
            interval = 1,
            monthlyType = "DAY_OF_WEEK",
            dayOfWeek = Calendar.FRIDAY,
            dayOfWeekOccurrence = -1 // Last Friday
        )
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, rule)
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 31, 12, 0, 0) // Last Friday of July is July 31st
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testMonthlyDayOfMonth() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val rule = RecurrenceRule(
            frequency = "MONTHLY",
            interval = 1,
            monthlyType = "DAY_OF_MONTH",
            dayOfMonth = 31 // Rent is due on 31st
        )
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, rule)
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 31, 12, 0, 0) // July has 31 days
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testYearlyRecurrence() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val rule = RecurrenceRule(frequency = "YEARLY", interval = 3)
        val nextDate = RecurrenceUtils.calculateNextDueDate(calendar.timeInMillis, rule)
        
        val expected = Calendar.getInstance().apply {
            set(2029, Calendar.JUNE, 5, 12, 0, 0) // 3 years later
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(expected.timeInMillis, nextDate)
    }

    @Test
    fun testFormatSummaryDaily() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        
        val summary1 = RecurrenceUtils.formatSummary(context, "DAILY")
        assertEquals("Daily", summary1)
        
        val ruleJson = RecurrenceRule(frequency = "DAILY", interval = 3).toJson()
        val summary2 = RecurrenceUtils.formatSummary(context, ruleJson)
        assertEquals("Every 3 days", summary2)
    }

    @Test
    fun testFormatSummaryWeekly() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        
        val ruleJson = RecurrenceRule(
            frequency = "WEEKLY",
            interval = 1,
            weekdays = listOf(Calendar.MONDAY, Calendar.WEDNESDAY)
        ).toJson()
        val summary = RecurrenceUtils.formatSummary(context, ruleJson)
        assertEquals("Weekly on Mon, Wed", summary)
    }

    @Test
    fun testFormatSummaryMonthly() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        
        val ruleJson = RecurrenceRule(
            frequency = "MONTHLY",
            interval = 1,
            monthlyType = "DAY_OF_MONTH",
            dayOfMonth = 15
        ).toJson()
        val summary = RecurrenceUtils.formatSummary(context, ruleJson)
        assertEquals("Monthly on Day 15", summary)
        
        val ruleJsonRelative = RecurrenceRule(
            frequency = "MONTHLY",
            interval = 2,
            monthlyType = "DAY_OF_WEEK",
            dayOfWeek = Calendar.FRIDAY,
            dayOfWeekOccurrence = 1 // First Friday
        ).toJson()
        val summaryRelative = RecurrenceUtils.formatSummary(context, ruleJsonRelative)
        assertEquals("Every 2 months on First Friday", summaryRelative)
    }
}
