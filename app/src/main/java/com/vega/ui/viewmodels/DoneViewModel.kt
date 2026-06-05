package com.vega.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vega.data.database.Task
import com.vega.data.database.TaskState
import com.vega.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class DoneViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val doneTasks: StateFlow<List<Task>> = repository.getDoneTasks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun clearError() {
        _error.value = null
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
            } catch (e: Exception) {
                Log.e("DoneViewModel", "Failed to delete task", e)
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
                Log.e("DoneViewModel", "Failed to postpone task", e)
                _error.value = "Failed to postpone task"
            }
        }
    }
}
