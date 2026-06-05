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
    
    @Query("SELECT * FROM tasks WHERE state = 'UPCOMING' ORDER BY dueDate ASC")
    fun getUpcomingTasksSortedByDueDate(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE state = 'DONE' ORDER BY updatedAt DESC")
    fun getDoneTasksSortedByRecency(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE title LIKE :query ORDER BY createdAt DESC")
    fun searchByTitle(query: String): Flow<List<Task>>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE state = :state")
    suspend fun countTasksByState(state: String): Int
    
    @Query("SELECT * FROM tasks WHERE state = 'TODAY'")
    fun getTodayTasksForSnooze(): Flow<List<Task>>
}
