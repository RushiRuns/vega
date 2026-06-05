package com.vega.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.data.repository.TaskRepository
import com.vega.parser.NaturalLanguageParser
import com.vega.parser.ParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QuickAddUiState {
    object Idle : QuickAddUiState
    object Loading : QuickAddUiState
    object Success : QuickAddUiState
    data class Error(val message: String) : QuickAddUiState
}

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val parser: NaturalLanguageParser
) : ViewModel() {

    private val _parseResult = MutableStateFlow(ParseResult())
    val parseResult: StateFlow<ParseResult> = _parseResult.asStateFlow()

    private val _uiState = MutableStateFlow<QuickAddUiState>(QuickAddUiState.Idle)
    val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

    fun parseInput(text: String) {
        _parseResult.value = parser.parse(text)
    }

    fun updateDueDate(dueDate: Long?) {
        _parseResult.value = _parseResult.value.copy(dueDate = dueDate)
    }

    fun updatePriority(priority: TaskPriority) {
        _parseResult.value = _parseResult.value.copy(priority = priority)
    }

    fun createTask() {
        val currentResult = _parseResult.value
        val title = currentResult.title

        if (title.isNullOrBlank()) {
            _uiState.value = QuickAddUiState.Error("Task title cannot be empty")
            return
        }

        _uiState.value = QuickAddUiState.Loading

        viewModelScope.launch {
            try {
                val task = Task(
                    title = title,
                    dueDate = currentResult.dueDate,
                    priority = currentResult.priority.name,
                    state = TaskState.INBOX.name
                )
                repository.createTask(task)
                _uiState.value = QuickAddUiState.Success
            } catch (e: Exception) {
                _uiState.value = QuickAddUiState.Error(e.message ?: "Failed to create task")
            }
        }
    }

    fun clearState() {
        _uiState.value = QuickAddUiState.Idle
        _parseResult.value = ParseResult()
    }
}
