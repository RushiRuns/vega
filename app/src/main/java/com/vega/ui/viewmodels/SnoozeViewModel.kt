package com.vega.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vega.data.database.Task
import com.vega.data.database.TaskState
import com.vega.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class SnoozeViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _snoozeQueue = MutableStateFlow<List<Task>>(emptyList())
    val snoozeQueue: StateFlow<List<Task>> = _snoozeQueue

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val currentTask: StateFlow<Task?> = _snoozeQueue
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun clearError() {
        _error.value = null
    }

    fun startSnooze(tasks: List<Task>) {
        _snoozeQueue.value = tasks
    }

    fun loadTodayTasksForSnooze() {
        viewModelScope.launch {
            try {
                val tasks = repository.getTodayTasksForSnooze().first()
                startSnooze(tasks)
            } catch (e: Exception) {
                Log.e("SnoozeViewModel", "Failed to load tasks for snooze", e)
                _error.value = "Failed to load tasks for snooze"
            }
        }
    }

    fun moveToTomorrow(task: Task) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val origDueDate = task.dueDate
                if (origDueDate != null) {
                    calendar.timeInMillis = origDueDate
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                } else {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                }
                val newDueDate = calendar.timeInMillis

                repository.updateTask(
                    task.copy(
                        state = TaskState.UPCOMING.name,
                        dueDate = newDueDate,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                popQueue()
            } catch (e: Exception) {
                Log.e("SnoozeViewModel", "Failed to snooze task", e)
                _error.value = "Failed to snooze task"
            }
        }
    }

    fun keepInToday(task: Task) {
        viewModelScope.launch {
            // Keep in today, simply progress the queue
            popQueue()
        }
    }

    fun markAsDone(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(
                    task.copy(
                        state = TaskState.DONE.name,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                popQueue()
            } catch (e: Exception) {
                Log.e("SnoozeViewModel", "Failed to mark task done", e)
                _error.value = "Failed to mark task done"
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
                popQueue()
            } catch (e: Exception) {
                Log.e("SnoozeViewModel", "Failed to delete task", e)
                _error.value = "Failed to delete task"
            }
        }
    }

    private fun popQueue() {
        val currentList = _snoozeQueue.value
        if (currentList.isNotEmpty()) {
            _snoozeQueue.value = currentList.drop(1)
        }
    }
}
