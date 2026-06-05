package com.vega.data.repository

import com.vega.alarms.TaskAlarmScheduler
import com.vega.data.database.Task
import com.vega.data.database.TaskDao
import com.vega.data.database.TaskState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: TaskAlarmScheduler
) {
    fun getInboxTasks(): Flow<List<Task>> = 
        taskDao.getTasksByState(TaskState.INBOX.name)
    
    fun getTodayTasks(): Flow<List<Task>> = 
        taskDao.getTasksByState(TaskState.TODAY.name)
    
    fun getUpcomingTasks(): Flow<List<Task>> = 
        taskDao.getUpcomingTasksSortedByDueDate()
    
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
                    val baseTime = task.dueDate ?: System.currentTimeMillis()
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
                        
                        val nextRule = rule.copy(currentOccurrenceCount = rule.currentOccurrenceCount + 1)
                        
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
    
    fun getTodayTasksForSnooze(): Flow<List<Task>> = 
        taskDao.getTodayTasksForSnooze()
}
