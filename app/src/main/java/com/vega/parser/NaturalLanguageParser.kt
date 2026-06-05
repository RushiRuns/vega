package com.vega.parser

import com.vega.data.database.TaskPriority
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * A parser that extracts task properties (such as title, due date, time, and priority)
 * from a freeform natural language string.
 */
class NaturalLanguageParser @Inject constructor() {

    /**
     * Parses the given freeform input string and extracts task details.
     *
     * @param input The natural language string to parse.
     * @param now The baseline timestamp in milliseconds (used for relative date parsing).
     * @return A [ParseResult] containing the extracted title, due date, priority, and ambiguity flag.
     */
    fun parse(input: String, now: Long = System.currentTimeMillis()): ParseResult {
        if (input.isBlank()) {
            return ParseResult()
        }

        val workingText = input.trim()

        // 1. Extract priority
        val priority = extractPriority(workingText)
        
        // 2. Extract recurrence
        val recurrence = extractRecurrence(workingText)

        // 3. Extract date/time
        var dueDate = extractDateTime(workingText, now)
        if (dueDate == null && recurrence != null) {
            dueDate = extractRecurrenceDueDate(workingText, now)
        }

        // 4. Remove priority tokens
        var title = removePriorityTokens(workingText)

        // 5. Remove recurrence tokens
        if (recurrence != null) {
            title = removeRecurrenceTokens(title)
        }

        // 6. Remove date/time tokens
        if (dueDate != null) {
            title = removeDateTimeTokens(title)
        }

        // Clean up title (remove double spaces)
        title = title.replace(Regex("\\s+"), " ").trim()

        // Remove trailing prepositions left over after date/time/recurrence removal
        val prepositionRegex = Regex("(?:^|\\s+)\\b(at|by|on|for|to|in)\\b\\s*$", RegexOption.IGNORE_CASE)
        title = title.replace(prepositionRegex, "").trim()

        // 7. Ambiguity check: if title is empty, fallback
        if (title.isBlank()) {
            return ParseResult(
                title = input.trim(),
                dueDate = null,
                priority = TaskPriority.NONE,
                recurrence = null,
                isAmbiguous = true
            )
        }

        return ParseResult(
            title = title,
            dueDate = dueDate,
            priority = priority,
            recurrence = recurrence,
            isAmbiguous = false
        )
    }

    private fun extractPriority(text: String): TaskPriority {
        val lowerText = text.lowercase(Locale.getDefault())
        val words = lowerText.split(Regex("\\s+"))

        return when {
            words.any { it == "high" || it == "urgent" || it == "asap" || it == "critical" || it == "urgent:" } ||
            lowerText.contains("urgent:") -> TaskPriority.HIGH
            
            words.any { it == "medium" || it == "normal" } -> TaskPriority.MEDIUM
            
            words.any { it == "low" || it == "whenever" } -> TaskPriority.LOW
            
            else -> TaskPriority.NONE
        }
    }

    private fun removePriorityTokens(text: String): String {
        val patterns = listOf(
            "\\b(high|urgent|asap|critical)\\b",
            "\\b(medium|normal)\\b",
            "\\b(low|whenever)\\b",
            "\\b(urgent:)\\b"
        )
        
        var result = text
        for (pattern in patterns) {
            result = result.replace(Regex(pattern, RegexOption.IGNORE_CASE), "")
        }
        return result.trim()
    }

    private fun extractDateTime(text: String, now: Long): Long? {
        val lowerText = text.lowercase(Locale.getDefault())
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        var dateFound = false
        
        // 1. Try MM/DD/YYYY format
        val datePattern = Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})")
        val dateMatch = datePattern.find(text)
        if (dateMatch != null) {
            val month = dateMatch.groupValues[1].toInt() - 1
            val day = dateMatch.groupValues[2].toInt()
            val year = dateMatch.groupValues[3].toInt()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            dateFound = true
        } else if (lowerText.contains("today")) {
            dateFound = true
        } else if (lowerText.contains("tomorrow")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            dateFound = true
        } else if (lowerText.contains("next ")) {
            val weekdays = mapOf(
                "sunday" to Calendar.SUNDAY,
                "monday" to Calendar.MONDAY,
                "tuesday" to Calendar.TUESDAY,
                "wednesday" to Calendar.WEDNESDAY,
                "thursday" to Calendar.THURSDAY,
                "friday" to Calendar.FRIDAY,
                "saturday" to Calendar.SATURDAY
            )
            
            for ((name, value) in weekdays) {
                if (lowerText.contains("next $name")) {
                    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    var daysToAdd = value - currentDayOfWeek
                    if (daysToAdd <= 0) {
                        daysToAdd += 7
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                    dateFound = true
                    break
                }
            }
        } else {
            // in N days
            val inDaysPattern = Regex("in\\s+(\\d+)\\s+days?", RegexOption.IGNORE_CASE)
            val inDaysMatch = inDaysPattern.find(text)
            if (inDaysMatch != null) {
                val days = inDaysMatch.groupValues[1].toIntOrNull() ?: 0
                calendar.add(Calendar.DAY_OF_YEAR, days)
                dateFound = true
            }
        }

        if (!dateFound) {
            val textForTime = text.replace(datePattern, "")
            val explicitTimePattern = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b|\\b(\\d{1,2}):(\\d{2})\\b", RegexOption.IGNORE_CASE)
            if (explicitTimePattern.containsMatchIn(textForTime)) {
                dateFound = true
            }
        }

        if (!dateFound) return null

        // Parse time if present
        val textForTime = text.replace(datePattern, "")
        val explicitTimePattern = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b|\\b(\\d{1,2}):(\\d{2})\\b", RegexOption.IGNORE_CASE)
        val match = explicitTimePattern.find(textForTime)
        
        if (match != null) {
            var hour = 0
            var minute = 0
            var meridiem = ""
            
            if (match.groupValues[1].isNotEmpty()) {
                hour = match.groupValues[1].toInt()
                minute = match.groupValues[2].toIntOrNull() ?: 0
                meridiem = match.groupValues[3].lowercase(Locale.getDefault())
            } else {
                hour = match.groupValues[4].toInt()
                minute = match.groupValues[5].toInt()
            }
            
            val adjustedHour = when {
                meridiem == "pm" && hour < 12 -> hour + 12
                meridiem == "am" && hour == 12 -> 0
                else -> hour
            }
            
            calendar.set(Calendar.HOUR_OF_DAY, adjustedHour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis
    }

    private fun removeDateTimeTokens(text: String): String {
        var result = text
        
        // Remove MM/DD/YYYY
        result = result.replace(Regex("\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b"), "")
        
        // Remove date keywords
        result = result.replace(Regex("\\b(today|tomorrow)\\b", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("\\bnext\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("\\bin\\s+\\d+\\s+days?\\b", RegexOption.IGNORE_CASE), "")
        
        // Remove time patterns
        result = result.replace(Regex("\\b\\d{1,2}(?::\\d{2})?\\s*(am|pm)\\b", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("\\b\\d{1,2}:\\d{2}\\b"), "")

        return result.trim()
    }

    private fun extractRecurrence(text: String): String? {
        val lowerText = text.lowercase(Locale.getDefault()).trim()
        return when {
            lowerText.contains(Regex("\\b(every day)\\b")) || lowerText.contains(Regex("\\bdaily$")) || lowerText.contains(Regex("\\bdaily\\b\\s+(at|by|on|for|to|in)\\b")) -> "DAILY"
            lowerText.contains(Regex("\\b(every weekday)\\b")) -> "WEEKDAYS"
            lowerText.contains(Regex("\\b(every week)\\b")) || lowerText.contains(Regex("\\bweekly$")) || lowerText.contains(Regex("\\bweekly\\b\\s+(at|by|on|for|to|in)\\b")) ||
            lowerText.contains(Regex("\\bevery (sunday|monday|tuesday|wednesday|thursday|friday|saturday)\\b")) -> "WEEKLY"
            lowerText.contains(Regex("\\b(every month)\\b")) || lowerText.contains(Regex("\\bmonthly$")) || lowerText.contains(Regex("\\bmonthly\\b\\s+(at|by|on|for|to|in)\\b")) -> "MONTHLY"
            else -> null
        }
    }

    private fun extractRecurrenceDueDate(text: String, now: Long): Long? {
        val lowerText = text.lowercase(Locale.getDefault())
        val weekdays = mapOf(
            "sunday" to Calendar.SUNDAY,
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY
        )
        for ((name, value) in weekdays) {
            if (lowerText.contains("every $name")) {
                val calendar = Calendar.getInstance().apply { timeInMillis = now }
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                var daysToAdd = value - currentDayOfWeek
                if (daysToAdd < 0) {
                    daysToAdd += 7
                }
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)

                val explicitTimePattern = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b|\\b(\\d{1,2}):(\\d{2})\\b", RegexOption.IGNORE_CASE)
                val match = explicitTimePattern.find(text)
                if (match != null) {
                    var hour = 0
                    var minute = 0
                    var meridiem = ""
                    if (match.groupValues[1].isNotEmpty()) {
                        hour = match.groupValues[1].toInt()
                        minute = match.groupValues[2].toIntOrNull() ?: 0
                        meridiem = match.groupValues[3].lowercase(Locale.getDefault())
                    } else {
                        hour = match.groupValues[4].toInt()
                        minute = match.groupValues[5].toInt()
                    }
                    val adjustedHour = when {
                        meridiem == "pm" && hour < 12 -> hour + 12
                        meridiem == "am" && hour == 12 -> 0
                        else -> hour
                    }
                    calendar.set(Calendar.HOUR_OF_DAY, adjustedHour)
                    calendar.set(Calendar.MINUTE, minute)
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                }
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return calendar.timeInMillis
            }
        }

        if (lowerText.contains("every day") || lowerText.contains("daily") || lowerText.contains("every weekday")) {
            val calendar = Calendar.getInstance().apply { timeInMillis = now }
            val explicitTimePattern = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b|\\b(\\d{1,2}):(\\d{2})\\b", RegexOption.IGNORE_CASE)
            val match = explicitTimePattern.find(text)
            if (match != null) {
                var hour = 0
                var minute = 0
                var meridiem = ""
                if (match.groupValues[1].isNotEmpty()) {
                    hour = match.groupValues[1].toInt()
                    minute = match.groupValues[2].toIntOrNull() ?: 0
                    meridiem = match.groupValues[3].lowercase(Locale.getDefault())
                } else {
                    hour = match.groupValues[4].toInt()
                    minute = match.groupValues[5].toInt()
                }
                val adjustedHour = when {
                    meridiem == "pm" && hour < 12 -> hour + 12
                    meridiem == "am" && hour == 12 -> 0
                    else -> hour
                }
                calendar.set(Calendar.HOUR_OF_DAY, adjustedHour)
                calendar.set(Calendar.MINUTE, minute)
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
            }
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (lowerText.contains("every weekday")) {
                val day = calendar.get(Calendar.DAY_OF_WEEK)
                if (day == Calendar.SATURDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 2)
                } else if (day == Calendar.SUNDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return calendar.timeInMillis
        }

        return null
    }

    private fun removeRecurrenceTokens(text: String): String {
        val patterns = listOf(
            "\\b(every day)\\b",
            "\\bdaily$",
            "\\bdaily\\b\\s+(at|by|on|for|to|in)\\b",
            "\\b(every weekday)\\b",
            "\\b(every week)\\b",
            "\\bweekly$",
            "\\bweekly\\b\\s+(at|by|on|for|to|in)\\b",
            "\\b(every month)\\b",
            "\\bmonthly$",
            "\\bmonthly\\b\\s+(at|by|on|for|to|in)\\b",
            "\\bevery (sunday|monday|tuesday|wednesday|thursday|friday|saturday)\\b"
        )
        var result = text
        for (pattern in patterns) {
            result = result.replace(Regex(pattern, RegexOption.IGNORE_CASE), "")
        }
        return result.trim()
    }
}
