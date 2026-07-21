package com.vega.data.repository

import com.vega.data.database.Tag
import com.vega.data.database.TagDao
import com.vega.data.database.TaskWithTags
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.combine

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao
) {
    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    val allTaskTagsMap: Flow<Map<String, List<Tag>>> = combine(
        tagDao.getAllTags(),
        tagDao.getAllTaskTagCrossRefs()
    ) { tags, refs ->
        val tagMap = tags.associateBy { it.id }
        refs.groupBy { it.taskId }
            .mapValues { entry -> entry.value.mapNotNull { tagMap[it.tagId] } }
    }

    suspend fun createTag(name: String, colorHex: String = "#4ECDC4"): Tag {
        val tag = Tag(name = name, colorHex = colorHex)
        tagDao.insertTag(tag)
        return tag
    }

    suspend fun updateTag(tag: Tag) {
        tagDao.updateTag(tag)
    }

    suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTaskTagCrossRefsByTagId(tag.id)
        tagDao.deleteTag(tag)
    }

    suspend fun setTaskTags(taskId: String, tagIds: List<String>) {
        tagDao.setTaskTags(taskId, tagIds)
    }

    fun getTaskWithTags(taskId: String): Flow<TaskWithTags?> {
        return tagDao.getTaskWithTags(taskId)
    }
}
