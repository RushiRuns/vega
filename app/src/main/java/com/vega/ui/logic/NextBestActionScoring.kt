package com.vega.ui.logic

import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import java.util.Calendar

/**
 * Calculates a priority score for a given task.
 *
 * The score is calculated using the following weights:
 * - Overdue task (dueDate < now): +100 points
 * - Due within 2 hours: +40 points
 * - Due today: +20 points
 * - Priority:
 *   - HIGH: +30 points
 *   - MEDIUM: +15 points
 *   - LOW: +5 points
 *   - NONE: +0 points
 *
 * @param task The task to score.
 * @param now The current time in milliseconds.
 * @return The calculated score as an integer.
 */
fun scoreTask(task: Task, now: Long): Int {
    var score = 0

    val dueDate = task.dueDate
    if (dueDate != null) {
        if (dueDate < now) {
            score += 100
        } else {
            val diffMs = dueDate - now
            val diffHours = diffMs.toDouble() / (1000 * 60 * 60)
            if (diffHours <= 2.0) {
                score += 40
            } else {
                val calNow = Calendar.getInstance().apply { timeInMillis = now }
                val calDue = Calendar.getInstance().apply { timeInMillis = dueDate }
                val isSameDay = calNow.get(Calendar.YEAR) == calDue.get(Calendar.YEAR) &&
                                calNow.get(Calendar.DAY_OF_YEAR) == calDue.get(Calendar.DAY_OF_YEAR)
                if (isSameDay) {
                    score += 20
                }
            }
        }
    }

    val priority = try {
        TaskPriority.valueOf(task.priority)
    } catch (e: Exception) {
        TaskPriority.NONE
    }

    score += when (priority) {
        TaskPriority.HIGH -> 30
        TaskPriority.MEDIUM -> 15
        TaskPriority.LOW -> 5
        TaskPriority.NONE -> 0
    }

    return score
}

/**
 * Selects the single best task for the user to work on next.
 *
 * It filters out done tasks and the task that was dismissed by the user for the current session.
 * The remaining tasks are sorted descending by score, and ties are broken sequentially by:
 * 1. Due date ascending (earliest first).
 * 2. Task priority descending (highest first).
 * 3. Task title alphabetically.
 * 4. Task ID (deterministic tie-breaker).
 *
 * @param tasks The list of all today tasks.
 * @param now The current time in milliseconds.
 * @param dismissedTaskId The ID of the task dismissed by the user, if any.
 * @return The recommended task, or null if no tasks are eligible.
 */
fun selectNextBestAction(tasks: List<Task>, now: Long, dismissedTaskId: String?): Task? {
    val activeTasks = tasks.filter { 
        it.state != TaskState.DONE.name && it.id != dismissedTaskId
    }

    if (activeTasks.isEmpty()) return null

    return activeTasks.sortedWith(
        compareByDescending<Task> { scoreTask(it, now) }
            .thenBy { it.dueDate ?: Long.MAX_VALUE }
            .thenByDescending { 
                try {
                    TaskPriority.valueOf(it.priority).ordinal
                } catch (e: Exception) {
                    TaskPriority.NONE.ordinal
                }
            }
            .thenBy { it.title }
            .thenBy { it.id }
        ).firstOrNull()
}
