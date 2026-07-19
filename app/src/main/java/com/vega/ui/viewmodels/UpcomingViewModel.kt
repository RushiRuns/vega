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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.LinkedHashMap

@HiltViewModel
class UpcomingViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val upcomingTasks: StateFlow<List<UpcomingListItem>> = repository.getUpcomingTasks()
        .map { tasks -> groupTasks(tasks) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun groupTasks(tasks: List<Task>): List<UpcomingListItem> {
        val today = Calendar.getInstance()
        
        // 1. Tomorrow boundaries
        val tomorrowStart = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrowEnd = Calendar.getInstance().apply {
            timeInMillis = tomorrowStart.timeInMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // 2. Next 7 Days boundaries (days 2 to 7 from today)
        val nextDaysStart = tomorrowEnd.timeInMillis + 1
        val nextDaysEnd = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // 3. Next Week boundary (days 8 to 14 from today)
        val nextWeekStart = nextDaysEnd.timeInMillis + 1
        val nextWeekEnd = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, 14)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Categorize lists
        val tomorrowList = mutableListOf<Task>()
        val weekdayLists = LinkedHashMap<String, MutableList<Task>>()
        val nextWeekList = mutableListOf<Task>()
        val laterList = mutableListOf<Task>()
        val unscheduledList = mutableListOf<Task>()

        for (task in tasks) {
            val dueDate = task.dueDate
            if (dueDate == null) {
                unscheduledList.add(task)
            } else {
                val dueCal = Calendar.getInstance().apply { timeInMillis = dueDate }
                when {
                    dueDate <= tomorrowEnd.timeInMillis -> {
                        tomorrowList.add(task)
                    }
                    dueDate in nextDaysStart..nextDaysEnd.timeInMillis -> {
                        val key = SimpleDateFormat("EEEE (MMM d)", Locale.getDefault()).format(dueCal.time)
                        weekdayLists.getOrPut(key) { mutableListOf() }.add(task)
                    }
                    dueDate in nextWeekStart..nextWeekEnd.timeInMillis -> {
                        nextWeekList.add(task)
                    }
                    else -> {
                        laterList.add(task)
                    }
                }
            }
        }

        val result = mutableListOf<UpcomingListItem>()

        if (tomorrowList.isNotEmpty()) {
            result.add(UpcomingListItem.Header("tomorrow", "Tomorrow"))
            result.addAll(tomorrowList.map { UpcomingListItem.TaskItem(it) })
        }

        for ((dayHeader, dayTasks) in weekdayLists) {
            if (dayTasks.isNotEmpty()) {
                result.add(UpcomingListItem.Header("day_$dayHeader", dayHeader))
                result.addAll(dayTasks.map { UpcomingListItem.TaskItem(it) })
            }
        }

        if (nextWeekList.isNotEmpty()) {
            result.add(UpcomingListItem.Header("next_week", "Next Week"))
            result.addAll(nextWeekList.map { UpcomingListItem.TaskItem(it) })
        }

        if (laterList.isNotEmpty()) {
            result.add(UpcomingListItem.Header("later", "Later"))
            result.addAll(laterList.map { UpcomingListItem.TaskItem(it) })
        }

        if (unscheduledList.isNotEmpty()) {
            result.add(UpcomingListItem.Header("unscheduled", "Unscheduled"))
            result.addAll(unscheduledList.map { UpcomingListItem.TaskItem(it) })
        }

        return result
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
