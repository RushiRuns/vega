package com.vega.utils

import com.vega.data.database.RecurrenceRule
import java.util.Calendar

object RecurrenceUtils {

    /**
     * Calculates the next occurrence's due date based on the current due date and the sophisticated recurrence rule.
     *
     * @param baseTime The current task's due date (or completion time).
     * @param rule The RecurrenceRule object defining the schedule.
     * @return The next occurrence's due date in milliseconds.
     */
    fun calculateNextDueDate(baseTime: Long, rule: RecurrenceRule): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTime }
        
        when (rule.frequency.uppercase()) {
            "DAILY" -> {
                calendar.add(Calendar.DAY_OF_YEAR, rule.interval)
            }
            "WEEKLY" -> {
                val weekdays = rule.weekdays
                if (weekdays.isNullOrEmpty()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, rule.interval)
                } else {
                    val sortedWeekdays = weekdays.sorted()
                    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    
                    val nextWeekday = sortedWeekdays.firstOrNull { it > currentDayOfWeek }
                    if (nextWeekday != null) {
                        val daysToAdd = nextWeekday - currentDayOfWeek
                        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                    } else {
                        val firstWeekday = sortedWeekdays.first()
                        calendar.add(Calendar.WEEK_OF_YEAR, rule.interval)
                        calendar.set(Calendar.DAY_OF_WEEK, firstWeekday)
                    }
                }
            }
            "MONTHLY" -> {
                if (rule.monthlyType == "DAY_OF_WEEK") {
                    val targetDayOfWeek = rule.dayOfWeek ?: Calendar.FRIDAY
                    val occurrence = rule.dayOfWeekOccurrence ?: 1
                    
                    calendar.add(Calendar.MONTH, rule.interval)
                    findNthWeekdayOfMonth(calendar, targetDayOfWeek, occurrence)
                } else {
                    val targetDay = rule.dayOfMonth ?: calendar.get(Calendar.DAY_OF_MONTH)
                    calendar.add(Calendar.MONTH, rule.interval)
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, Math.min(targetDay, maxDay))
                }
            }
            "YEARLY" -> {
                calendar.add(Calendar.YEAR, rule.interval)
            }
            else -> {
                // Fallback: default DAILY shift if frequency is daily/weekdays/etc.
                if (rule.frequency.uppercase() == "WEEKDAYS") {
                    do {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
                             calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                } else {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        
        return calendar.timeInMillis
    }

    private fun findNthWeekdayOfMonth(calendar: Calendar, dayOfWeek: Int, occurrence: Int) {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        
        if (occurrence > 0) {
            while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            calendar.add(Calendar.WEEK_OF_MONTH, occurrence - 1)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            }
        }
    }

    /**
     * Formats a recurrence pattern/JSON string into a human-friendly readable summary.
     */
    fun formatSummary(context: android.content.Context, recurrencePattern: String?): String {
        if (recurrencePattern.isNullOrBlank()) {
            return context.getString(com.vega.R.string.recurrence_none)
        }
        val rule = if (recurrencePattern.startsWith("{")) {
            RecurrenceRule.fromJson(recurrencePattern)
        } else {
            val freq = recurrencePattern.uppercase()
            when (freq) {
                "DAILY" -> RecurrenceRule(frequency = "DAILY")
                "WEEKDAYS" -> RecurrenceRule(frequency = "WEEKDAYS")
                "WEEKLY" -> RecurrenceRule(frequency = "WEEKLY")
                "MONTHLY" -> RecurrenceRule(frequency = "MONTHLY")
                else -> RecurrenceRule(frequency = freq)
            }
        }
        
        if (rule == null) {
            return context.getString(com.vega.R.string.recurrence_none)
        }

        val symbols = java.text.DateFormatSymbols.getInstance(java.util.Locale.getDefault())
        val shortWeekdays = symbols.shortWeekdays
        val weekdays = symbols.weekdays

        return when (rule.frequency.uppercase()) {
            "DAILY" -> {
                if (rule.interval == 1) {
                    context.getString(com.vega.R.string.recurrence_summary_daily_1)
                } else {
                    context.getString(com.vega.R.string.recurrence_summary_daily, rule.interval)
                }
            }
            "WEEKLY" -> {
                val weekdaysList = rule.weekdays
                if (weekdaysList.isNullOrEmpty()) {
                    if (rule.interval == 1) {
                        context.getString(com.vega.R.string.recurrence_summary_weekly_1, "")
                    } else {
                        context.getString(com.vega.R.string.recurrence_summary_weekly, rule.interval, "")
                    }
                } else {
                    val daysStr = weekdaysList.sorted().map { shortWeekdays[it] }.joinToString(", ")
                    if (rule.interval == 1) {
                        context.getString(com.vega.R.string.recurrence_summary_weekly_1, daysStr)
                    } else {
                        context.getString(com.vega.R.string.recurrence_summary_weekly, rule.interval, daysStr)
                    }
                }
            }
            "WEEKDAYS" -> {
                context.getString(com.vega.R.string.recurrence_weekdays)
            }
            "MONTHLY" -> {
                val details = if (rule.monthlyType == "DAY_OF_WEEK") {
                    val targetDayOfWeek = rule.dayOfWeek ?: Calendar.FRIDAY
                    val occurrence = rule.dayOfWeekOccurrence ?: 1
                    val occurrenceStr = when (occurrence) {
                        1 -> "first"
                        2 -> "second"
                        3 -> "third"
                        4 -> "fourth"
                        -1 -> "last"
                        else -> ""
                    }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    val dayOfWeekName = weekdays[targetDayOfWeek]
                    context.getString(com.vega.R.string.monthly_relative_day, occurrenceStr, dayOfWeekName)
                } else {
                    val day = rule.dayOfMonth ?: 1
                    context.getString(com.vega.R.string.monthly_day_of_month, day)
                }
                
                if (rule.interval == 1) {
                    context.getString(com.vega.R.string.recurrence_summary_monthly_1) + " on " + details
                } else {
                    context.getString(com.vega.R.string.recurrence_summary_monthly, rule.interval) + " on " + details
                }
            }
            "YEARLY" -> {
                if (rule.interval == 1) {
                    context.getString(com.vega.R.string.recurrence_summary_yearly_1)
                } else {
                    context.getString(com.vega.R.string.recurrence_summary_yearly, rule.interval)
                }
            }
            else -> rule.frequency
        }
    }
}

