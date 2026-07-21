package com.vega.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag)

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * FROM task_tag_cross_ref")
    fun getAllTaskTagCrossRefs(): Flow<List<TaskTagCrossRef>>

    @Query("DELETE FROM task_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteTaskTagCrossRefsByTagId(tagId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTagCrossRef(crossRef: TaskTagCrossRef)

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId")
    suspend fun deleteTaskTagCrossRefsByTaskId(taskId: String)

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskWithTags(taskId: String): Flow<TaskWithTags?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithTagsSync(taskId: String): TaskWithTags?

    @Transaction
    suspend fun setTaskTags(taskId: String, tagIds: List<String>) {
        deleteTaskTagCrossRefsByTaskId(taskId)
        tagIds.forEach { tagId ->
            insertTaskTagCrossRef(TaskTagCrossRef(taskId, tagId))
        }
    }
}
