package com.vega.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["state"]),
        Index(value = ["dueDate"]),
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val title: String,
    val dueDate: Long? = null,
    val priority: String = TaskPriority.NONE.name,
    val state: String = TaskState.INBOX.name,
    val notes: String? = null,
    val recurrence: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
