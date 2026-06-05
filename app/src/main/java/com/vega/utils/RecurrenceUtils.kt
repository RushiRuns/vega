package com.vega.utils

import java.util.Calendar

object RecurrenceUtils {

    /**
     * Calculates the next occurrence's due date based on the current due date and the recurrence pattern.
     *
     * @param baseTime The current task's due date (or current completion time).
     * @param recurrence The recurrence pattern (DAILY, WEEKDAYS, WEEKLY, MONTHLY).
     * @return The next occurrence's due date in milliseconds.
     */
    fun calculateNextDueDate(baseTime: Long, recurrence: String): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTime }
        
        when (recurrence.uppercase()) {
            "DAILY" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            "WEEKDAYS" -> {
                do {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
                         calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            "WEEKLY" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            "MONTHLY" -> {
                calendar.add(Calendar.MONTH, 1)
            }
        }
        
        return calendar.timeInMillis
    }
}
