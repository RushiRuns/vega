package com.vega.ui.viewmodels

import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.check
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    @Mock
    private lateinit var repository: TaskRepository

    private lateinit var viewModel: TaskDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskDetailViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadTaskLoadsTaskIntoStateFlow() = runTest(testDispatcher) {
        val task = Task(id = "test-id", title = "Task Title")
        whenever(repository.getTaskById("test-id")).thenReturn(task)

        viewModel.loadTask("test-id")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(task, viewModel.task.value)
    }

    @Test
    fun testSaveTaskUpdatesAllFieldsAndSaves() = runTest(testDispatcher) {
        val originalTask = Task(id = "test-id", title = "Old Title", state = TaskState.INBOX.name)
        whenever(repository.getTaskById("test-id")).thenReturn(originalTask)

        viewModel.loadTask("test-id")
        testDispatcher.scheduler.advanceUntilIdle()

        val newDueDate = 123456789L
        viewModel.saveTask(
            title = "New Title",
            dueDate = newDueDate,
            priority = TaskPriority.HIGH,
            state = TaskState.TODAY,
            notes = "New Notes"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).updateTask(check { task ->
            assertEquals("test-id", task.id)
            assertEquals("New Title", task.title)
            assertEquals(newDueDate, task.dueDate)
            assertEquals(TaskPriority.HIGH.name, task.priority)
            assertEquals(TaskState.TODAY.name, task.state)
            assertEquals("New Notes", task.notes)
        })
        assertTrue(viewModel.saveSuccess.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun testSaveTaskFailsOnBlankTitle() = runTest(testDispatcher) {
        val originalTask = Task(id = "test-id", title = "Old Title")
        whenever(repository.getTaskById("test-id")).thenReturn(originalTask)

        viewModel.loadTask("test-id")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveTask(
            title = "   ",
            dueDate = null,
            priority = TaskPriority.LOW,
            state = TaskState.UPCOMING,
            notes = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not save to repository
        verify(repository, org.mockito.Mockito.never()).updateTask(org.mockito.kotlin.any())
        assertEquals("Task title cannot be empty", viewModel.error.value)
        assertTrue(!viewModel.saveSuccess.value)
    }
}
