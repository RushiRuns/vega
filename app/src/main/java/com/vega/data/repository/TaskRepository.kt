package com.vega.data.repository

import com.vega.alarms.TaskAlarmScheduler
import com.vega.data.database.RecurrenceRule
import com.vega.data.database.Task
import com.vega.data.database.TaskDao
import com.vega.data.database.TaskState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: TaskAlarmScheduler
) {
    fun getInboxTasks(): Flow<List<Task>> = 
        taskDao.getTasksByState(TaskState.INBOX.name)
    
    private fun getEndOfToday(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun getTodayTasks(): Flow<List<Task>> = getTodayTasks(getEndOfToday())
    
    fun getTodayTasks(endOfToday: Long): Flow<List<Task>> {
        val nonRecurring = taskDao.getTodayTasks(endOfToday)
        val recurring = taskDao.getAllRecurringActiveTasks()
        return combine(nonRecurring, recurring) { nonRec, rec ->
            val filteredRecurring = rec.filter { task ->
                val rule = task.recurrence?.let {
                    if (it.startsWith("{")) RecurrenceRule.fromJson(it)
                    else RecurrenceRule(frequency = it)
                }
                // Only show recurring task if its startDate is today or in the past
                rule != null && rule.startDate <= endOfToday
            }
            (nonRec + filteredRecurring).sortedByDescending { it.createdAt }
        }
    }
    
    fun getUpcomingTasks(): Flow<List<Task>> = getUpcomingTasks(getEndOfToday())
    
    fun getUpcomingTasks(endOfToday: Long): Flow<List<Task>> = 
        taskDao.getUpcomingTasksSortedByDueDate(endOfToday)
    
    fun getDoneTasks(): Flow<List<Task>> = 
        taskDao.getDoneTasksSortedByRecency()
        
    suspend fun createTask(task: Task) {
        taskDao.insertTask(task)
        alarmScheduler.scheduleAlarm(task)
    }
    
    suspend fun updateTask(task: Task) {
        val oldTask = taskDao.getTaskById(task.id)
        taskDao.updateTask(task)
        alarmScheduler.scheduleAlarm(task)
        
        if (task.state == TaskState.DONE.name && (oldTask == null || oldTask.state != TaskState.DONE.name)) {
            val recurrencePattern = task.recurrence
            if (!recurrencePattern.isNullOrBlank()) {
                val rule = if (recurrencePattern.startsWith("{")) {
                    com.vega.data.database.RecurrenceRule.fromJson(recurrencePattern)
                } else {
                    com.vega.data.database.RecurrenceRule(frequency = recurrencePattern)
                }
                
                if (rule != null) {
                    val baseTime = task.dueDate ?: rule.startDate
                    val nextDueDate = com.vega.utils.RecurrenceUtils.calculateNextDueDate(baseTime, rule)
                    
                    var shouldSpawn = true
                    if (rule.endType == "ON_DATE" && rule.endDate != null && nextDueDate > rule.endDate) {
                        shouldSpawn = false
                    } else if (rule.endType == "AFTER_OCCURRENCES" && rule.endOccurrences != null) {
                        if (rule.currentOccurrenceCount >= rule.endOccurrences) {
                            shouldSpawn = false
                        }
                    }
                    
                    if (shouldSpawn) {
                        val todayCalendar = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        
                        val nextDayOnlyCalendar = java.util.Calendar.getInstance().apply {
                            timeInMillis = nextDueDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        
                        val nextState = if (nextDayOnlyCalendar.timeInMillis <= todayCalendar.timeInMillis) {
                            TaskState.TODAY.name
                        } else {
                            TaskState.UPCOMING.name
                        }
                        
                        val nextRule = rule.copy(
                            currentOccurrenceCount = rule.currentOccurrenceCount + 1,
                            startDate = nextDayOnlyCalendar.timeInMillis
                        )
                        
                        val nextTask = Task(
                            title = task.title,
                            dueDate = nextDueDate,
                            priority = task.priority,
                            state = nextState,
                            notes = task.notes,
                            recurrence = nextRule.toJson()
                        )
                        createTask(nextTask)
                    }
                }
            }
        }
    }
    
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        alarmScheduler.cancelAlarm(task.id)
    }
    
    fun searchTasks(query: String): Flow<List<Task>> = 
        taskDao.searchByTitle("%$query%")
    
    suspend fun getTaskById(id: String): Task? = 
        taskDao.getTaskById(id)
    
    fun getTodayTasksForSnooze(): Flow<List<Task>> = getTodayTasksForSnooze(getEndOfToday())

    fun getTodayTasksForSnooze(endOfToday: Long): Flow<List<Task>> {
        val nonRecurring = taskDao.getTodayTasksForSnooze(endOfToday)
        val recurring = taskDao.getAllRecurringActiveTasks()
        return combine(nonRecurring, recurring) { nonRec, rec ->
            val filteredRecurring = rec.filter { task ->
                val rule = task.recurrence?.let {
                    if (it.startsWith("{")) RecurrenceRule.fromJson(it)
                    else RecurrenceRule(frequency = it)
                }
                rule != null && rule.startDate <= endOfToday
            }
            nonRec + filteredRecurring
        }
    }
}
