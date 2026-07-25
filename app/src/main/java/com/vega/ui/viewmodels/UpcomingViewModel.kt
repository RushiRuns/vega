package com.vega.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vega.data.database.Task
import com.vega.data.database.TaskState
import com.vega.data.repository.TaskRepository
import com.vega.ui.models.UpcomingListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import java.util.Calendar

@HiltViewModel
class UpcomingViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val tagRepository: com.vega.data.repository.TagRepository
) : ViewModel() {

    val taskTagsMap = tagRepository.allTaskTagsMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val upcomingTasks: StateFlow<List<UpcomingListItem>> = repository.getUpcomingTasks()
        .map { tasks -> groupTasks(tasks) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun groupTasks(tasks: List<Task>): List<UpcomingListItem> {
        return tasks.map { UpcomingListItem.TaskItem(it) }
    }


    fun clearError() {
        _error.value = null
    }

    fun moveToToday(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(task.copy(state = TaskState.TODAY.name, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("UpcomingViewModel", "Failed to move task to Today", e)
                _error.value = "Failed to move task to Today"
            }
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(task.copy(state = TaskState.DONE.name, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("UpcomingViewModel", "Failed to complete task", e)
                _error.value = "Failed to complete task"
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
            } catch (e: Exception) {
                Log.e("UpcomingViewModel", "Failed to delete task", e)
                _error.value = "Failed to delete task"
            }
        }
    }

    fun postponeTask(task: Task) {
        viewModelScope.launch {
            try {
                val calendar = java.util.Calendar.getInstance()
                val origDueDate = task.dueDate
                if (origDueDate != null) {
                    calendar.timeInMillis = origDueDate
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                } else {
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                }
                val newDueDate = calendar.timeInMillis
                repository.updateTask(
                    task.copy(
                        state = TaskState.UPCOMING.name,
                        dueDate = newDueDate,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e("UpcomingViewModel", "Failed to postpone task", e)
                _error.value = "Failed to postpone task"
            }
        }
    }

    fun updateMultipleTasks(
        taskIds: List<String>,
        dueDate: Long? = null,
        dueDateUpdated: Boolean = false,
        priority: com.vega.data.database.TaskPriority? = null,
        state: com.vega.data.database.TaskState? = null,
        recurrence: String? = null,
        recurrenceUpdated: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                taskIds.forEach { id ->
                    val task = repository.getTaskById(id)
                    if (task != null) {
                        val updatedTask = task.copy(
                            dueDate = if (dueDateUpdated) dueDate else task.dueDate,
                            priority = priority?.name ?: task.priority,
                            state = state?.name ?: task.state,
                            recurrence = if (recurrenceUpdated) recurrence else task.recurrence,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.updateTask(updatedTask)
                    }
                }
            } catch (e: Exception) {
                Log.e("UpcomingViewModel", "Failed to update multiple tasks", e)
                _error.value = "Failed to update selected tasks"
            }
        }
    }
}
