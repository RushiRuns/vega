package com.vega.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            val loadedTask = repository.getTaskById(taskId)
            _task.value = loadedTask
        }
    }

    fun saveTask(
        title: String,
        dueDate: Long?,
        priority: TaskPriority,
        state: TaskState,
        notes: String?
    ) {
        val current = _task.value ?: return
        if (title.isBlank()) {
            _error.value = "Task title cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                val updatedTask = current.copy(
                    title = title.trim(),
                    dueDate = dueDate,
                    priority = priority.name,
                    state = state.name,
                    notes = notes?.trim(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTask(updatedTask)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to save task"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
