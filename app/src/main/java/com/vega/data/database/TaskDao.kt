package com.vega.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insertTask(task: Task)
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?
    
    @Query("SELECT * FROM tasks WHERE state = :state ORDER BY createdAt DESC")
    fun getTasksByState(state: String): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE state = 'UPCOMING' AND (dueDate > :endOfToday OR dueDate IS NULL) AND (recurrence IS NULL OR recurrence = '') ORDER BY dueDate ASC")
    fun getUpcomingTasksSortedByDueDate(endOfToday: Long): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE state = 'DONE' ORDER BY updatedAt DESC")
    fun getDoneTasksSortedByRecency(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE title LIKE :query ORDER BY createdAt DESC")
    fun searchByTitle(query: String): Flow<List<Task>>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE state = :state")
    suspend fun countTasksByState(state: String): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE state = 'DONE' AND updatedAt >= :startOfToday")
    fun countCompletedToday(startOfToday: Long): Flow<Int>
    
    @Query("SELECT * FROM tasks WHERE state != 'DONE' AND state != 'INBOX' AND (recurrence IS NULL OR recurrence = '') AND (state = 'TODAY' OR (state = 'UPCOMING' AND dueDate <= :endOfToday))")
    fun getTodayTasksForSnooze(endOfToday: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE state != 'DONE' AND state != 'INBOX' AND (recurrence IS NULL OR recurrence = '') AND (state = 'TODAY' OR (state = 'UPCOMING' AND dueDate <= :endOfToday)) ORDER BY createdAt DESC")
    fun getTodayTasks(endOfToday: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE state != 'DONE' AND state != 'INBOX' AND recurrence IS NOT NULL AND recurrence != ''")
    fun getAllRecurringActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE state != 'DONE'")
    suspend fun getAllActiveTasks(): List<Task>
}
