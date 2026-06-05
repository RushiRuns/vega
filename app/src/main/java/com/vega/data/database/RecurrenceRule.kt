package com.vega.data.database

import org.json.JSONArray
import org.json.JSONObject

data class RecurrenceRule(
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val interval: Int = 1,
    val weekdays: List<Int>? = null, // Calendar.SUNDAY=1, MONDAY=2, ...
    val monthlyType: String? = null, // DAY_OF_MONTH, DAY_OF_WEEK
    val dayOfMonth: Int? = null,
    val dayOfWeekOccurrence: Int? = null, // 1, 2, 3, 4, -1 (for first, second, third, fourth, last)
    val dayOfWeek: Int? = null, // Calendar.SUNDAY=1, etc.
    val startDate: Long = System.currentTimeMillis(),
    val endType: String = "NEVER", // NEVER, ON_DATE, AFTER_OCCURRENCES
    val endDate: Long? = null,
    val endOccurrences: Int? = null,
    val currentOccurrenceCount: Int = 1
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("frequency", frequency)
        obj.put("interval", interval)
        if (weekdays != null) {
            val arr = JSONArray()
            weekdays.forEach { arr.put(it) }
            obj.put("weekdays", arr)
        }
        obj.put("monthlyType", monthlyType ?: JSONObject.NULL)
        obj.put("dayOfMonth", dayOfMonth ?: JSONObject.NULL)
        obj.put("dayOfWeekOccurrence", dayOfWeekOccurrence ?: JSONObject.NULL)
        obj.put("dayOfWeek", dayOfWeek ?: JSONObject.NULL)
        obj.put("startDate", startDate)
        obj.put("endType", endType)
        obj.put("endDate", endDate ?: JSONObject.NULL)
        obj.put("endOccurrences", endOccurrences ?: JSONObject.NULL)
        obj.put("currentOccurrenceCount", currentOccurrenceCount)
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): RecurrenceRule? {
            if (jsonStr.isBlank()) return null
            return try {
                val obj = JSONObject(jsonStr)
                val frequency = obj.getString("frequency")
                val interval = obj.optInt("interval", 1)
                
                val weekdaysList = if (obj.has("weekdays") && !obj.isNull("weekdays")) {
                    val arr = obj.getJSONArray("weekdays")
                    val list = mutableListOf<Int>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getInt(i))
                    }
                    list
                } else null

                val monthlyType = if (obj.has("monthlyType") && !obj.isNull("monthlyType")) obj.getString("monthlyType") else null
                val dayOfMonth = if (obj.has("dayOfMonth") && !obj.isNull("dayOfMonth")) obj.getInt("dayOfMonth") else null
                val dayOfWeekOccurrence = if (obj.has("dayOfWeekOccurrence") && !obj.isNull("dayOfWeekOccurrence")) obj.getInt("dayOfWeekOccurrence") else null
                val dayOfWeek = if (obj.has("dayOfWeek") && !obj.isNull("dayOfWeek")) obj.getInt("dayOfWeek") else null
                val startDate = obj.optLong("startDate", System.currentTimeMillis())
                val endType = obj.optString("endType", "NEVER")
                val endDate = if (obj.has("endDate") && !obj.isNull("endDate")) obj.getLong("endDate") else null
                val endOccurrences = if (obj.has("endOccurrences") && !obj.isNull("endOccurrences")) obj.getInt("endOccurrences") else null
                val currentOccurrenceCount = obj.optInt("currentOccurrenceCount", 1)

                RecurrenceRule(
                    frequency = frequency,
                    interval = interval,
                    weekdays = weekdaysList,
                    monthlyType = monthlyType,
                    dayOfMonth = dayOfMonth,
                    dayOfWeekOccurrence = dayOfWeekOccurrence,
                    dayOfWeek = dayOfWeek,
                    startDate = startDate,
                    endType = endType,
                    endDate = endDate,
                    endOccurrences = endOccurrences,
                    currentOccurrenceCount = currentOccurrenceCount
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
