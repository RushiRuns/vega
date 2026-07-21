package com.vega.data.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "task_tag_cross_ref",
    primaryKeys = ["taskId", "tagId"],
    indices = [
        Index("taskId"),
        Index("tagId")
    ]
)
data class TaskTagCrossRef(
    val taskId: String,
    val tagId: String
)
