package com.vega.parser

import com.vega.data.database.TaskPriority

data class ParseResult(
    val title: String? = null,
    val dueDate: Long? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val recurrence: String? = null,
    val isAmbiguous: Boolean = false
) {
    fun isValid(): Boolean = !title.isNullOrBlank()

    override fun toString(): String {
        return "ParseResult(title=$title, dueDate=$dueDate, priority=$priority, recurrence=$recurrence, isAmbiguous=$isAmbiguous)"
    }
}
