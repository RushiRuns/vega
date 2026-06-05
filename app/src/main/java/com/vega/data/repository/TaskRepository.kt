package com.vega.data.repository

import com.vega.data.database.Task
import com.vega.data.database.TaskDao
import com.vega.data.database.TaskState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getInboxTasks(): Flow<List<Task>> = 
        taskDao.getTasksByState(TaskState.INBOX.name)
    
    fun getTodayTasks(): Flow<List<Task>> = 
        taskDao.getTasksByState(TaskState.TODAY.name)
    
    fun getUpcomingTasks(): Flow<List<Task>> = 
        taskDao.getUpcomingTasksSortedByDueDate()
    
    fun getDoneTasks(): Flow<List<Task>> = 
        taskDao.getDoneTasksSortedByRecency()
    
    suspend fun createTask(task: Task) = 
        taskDao.insertTask(task)
    
    suspend fun updateTask(task: Task) = 
        taskDao.updateTask(task)
    
    suspend fun deleteTask(task: Task) = 
        taskDao.deleteTask(task)
    
    fun searchTasks(query: String): Flow<List<Task>> = 
        taskDao.searchByTitle("%$query%")
    
    suspend fun getTaskById(id: String): Task? = 
        taskDao.getTaskById(id)
    
    fun getTodayTasksForSnooze(): Flow<List<Task>> = 
        taskDao.getTodayTasksForSnooze()
}
