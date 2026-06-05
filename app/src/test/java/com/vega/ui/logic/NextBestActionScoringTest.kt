package com.vega.ui.logic

import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class NextBestActionScoringTest {

    // Fixed now reference: Friday, June 5, 2026, 12:00:00 PM
    private val baseTime: Long by lazy {
        Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun testScoreTaskOverdue() {
        // Overdue task (due yesterday) with HIGH priority
        // Score: overdue (100) + HIGH (30) = 130
        val task = Task(
            title = "Overdue task",
            dueDate = baseTime - 24 * 60 * 60 * 1000,
            priority = TaskPriority.HIGH.name
        )
        val score = scoreTask(task, baseTime)
        assertEquals(130, score)
    }

    @Test
    fun testScoreTaskDueWithinTwoHours() {
        // Due in 1 hour with MEDIUM priority
        // Score: due within 2 hours (40) + MEDIUM (15) = 55
        val task = Task(
            title = "Due soon task",
            dueDate = baseTime + 1 * 60 * 60 * 1000,
            priority = TaskPriority.MEDIUM.name
        )
        val score = scoreTask(task, baseTime)
        assertEquals(55, score)
    }

    @Test
    fun testScoreTaskDueToday() {
        // Due in 5 hours (on same calendar day) with LOW priority
        // Score: due today (20) + LOW (5) = 25
        val task = Task(
            title = "Due today task",
            dueDate = baseTime + 5 * 60 * 60 * 1000,
            priority = TaskPriority.LOW.name
        )
        val score = scoreTask(task, baseTime)
        assertEquals(25, score)
    }

    @Test
    fun testPriorityWeights() {
        // Check priority weights alone (no due date)
        assertEquals(30, scoreTask(Task(title = "Task", priority = TaskPriority.HIGH.name), baseTime))
        assertEquals(15, scoreTask(Task(title = "Task", priority = TaskPriority.MEDIUM.name), baseTime))
        assertEquals(5, scoreTask(Task(title = "Task", priority = TaskPriority.LOW.name), baseTime))
        assertEquals(0, scoreTask(Task(title = "Task", priority = TaskPriority.NONE.name), baseTime))
    }

    @Test
    fun testDeterministicTieBreaking() {
        // Ties in score
        // Task A: Priority HIGH, Title B
        // Task B: Priority HIGH, Title A
        // Both score 30 (no due date). Title A should win.
        val taskA = Task(id = "1", title = "Task B", priority = TaskPriority.HIGH.name)
        val taskB = Task(id = "2", title = "Task A", priority = TaskPriority.HIGH.name)
        
        val winner = selectNextBestAction(listOf(taskA, taskB), baseTime, null)
        assertEquals(taskB.id, winner?.id)
    }

    @Test
    fun testSelectNextBestActionFiltersDismissed() {
        val taskA = Task(id = "1", title = "Task A", priority = TaskPriority.HIGH.name)
        val taskB = Task(id = "2", title = "Task B", priority = TaskPriority.MEDIUM.name)

        // Without dismissal, taskA (HIGH) wins
        var winner = selectNextBestAction(listOf(taskA, taskB), baseTime, null)
        assertEquals(taskA.id, winner?.id)

        // Dismiss taskA: taskB should win
        winner = selectNextBestAction(listOf(taskA, taskB), baseTime, taskA.id)
        assertEquals(taskB.id, winner?.id)
    }
}
