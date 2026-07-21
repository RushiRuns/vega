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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val tagRepository: com.vega.data.repository.TagRepository
) : ViewModel() {

    val taskTagsMap = tagRepository.allTaskTagsMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private val _dismissedTaskId = MutableStateFlow<String?>(null)
    val dismissedTaskId: StateFlow<String?> = _dismissedTaskId.asStateFlow()

    private val allTodayTasks = repository.getTodayTasks()

    private var previousTaskIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            allTodayTasks.collect { tasks ->
                val currentIds = tasks.map { it.id }.toSet()
                if (currentIds != previousTaskIds) {
                    _dismissedTaskId.value = null
                    previousTaskIds = currentIds
                }
            }
        }
    }

    val nextBestAction: StateFlow<Task?> = combine(allTodayTasks, _dismissedTaskId) { tasks, dismissedId ->
        com.vega.ui.logic.selectNextBestAction(tasks, System.currentTimeMillis(), dismissedId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun dismissNextBestAction(taskId: String) {
        _dismissedTaskId.value = taskId
    }

    val todayTasks: StateFlow<List<Task>> = combine(allTodayTasks, _isExpanded) { tasks, expanded ->
        if (expanded || tasks.size <= 7) {
            tasks
        } else {
            tasks.take(7)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val overflowCount: StateFlow<Int> = combine(allTodayTasks, _isExpanded) { tasks, expanded ->
        if (expanded || tasks.size <= 7) {
            0
        } else {
            tasks.size - 7
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val startOfToday: Long
        get() = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    val completedTodayCount: StateFlow<Int> = repository.countCompletedToday(startOfToday)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val totalTodayCount: StateFlow<Int> = combine(allTodayTasks, completedTodayCount) { tasks, completed ->
        tasks.size + completed
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun toggleExpand() {
        _isExpanded.value = !_isExpanded.value
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(task.copy(state = TaskState.DONE.name, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("TodayViewModel", "Failed to complete task", e)
                _error.value = "Failed to complete task"
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
            } catch (e: Exception) {
                Log.e("TodayViewModel", "Failed to delete task", e)
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
                Log.e("TodayViewModel", "Failed to postpone task", e)
                _error.value = "Failed to postpone task"
            }
        }
    }

    fun moveToInbox(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(task.copy(state = TaskState.INBOX.name, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("TodayViewModel", "Failed to move task to Inbox", e)
                _error.value = "Failed to move task to Inbox"
            }
        }
    }
}
