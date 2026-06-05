package com.vega.ui.viewmodels

import com.vega.data.database.Task
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
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.check
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class SnoozeViewModelTest {

    @Mock
    private lateinit var repository: TaskRepository

    private lateinit var viewModel: SnoozeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = SnoozeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStartSnoozePopulatesQueueAndCurrentTask() = runTest(testDispatcher) {
        val tasks = listOf(
            Task(id = "1", title = "Task 1", state = TaskState.TODAY.name),
            Task(id = "2", title = "Task 2", state = TaskState.TODAY.name)
        )
        viewModel.startSnooze(tasks)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(tasks, viewModel.snoozeQueue.value)
        assertEquals(tasks[0], viewModel.currentTask.value)
    }

    @Test
    fun testKeepInTodayAdvancesQueueWithoutUpdatingDatabase() = runTest(testDispatcher) {
        val tasks = listOf(
            Task(id = "1", title = "Task 1", state = TaskState.TODAY.name),
            Task(id = "2", title = "Task 2", state = TaskState.TODAY.name)
        )
        viewModel.startSnooze(tasks)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.keepInToday(tasks[0])
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(tasks[1], viewModel.currentTask.value)
        org.mockito.Mockito.verifyNoInteractions(repository)
    }

    @Test
    fun testMarkAsDoneUpdatesDatabaseAndAdvancesQueue() = runTest(testDispatcher) {
        val tasks = listOf(
            Task(id = "1", title = "Task 1", state = TaskState.TODAY.name),
            Task(id = "2", title = "Task 2", state = TaskState.TODAY.name)
        )
        viewModel.startSnooze(tasks)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.markAsDone(tasks[0])
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).updateTask(check { task ->
            assertEquals("1", task.id)
            assertEquals(TaskState.DONE.name, task.state)
        })

        assertEquals(tasks[1], viewModel.currentTask.value)
    }

    @Test
    fun testDeleteTaskUpdatesDatabaseAndAdvancesQueue() = runTest(testDispatcher) {
        val tasks = listOf(
            Task(id = "1", title = "Task 1", state = TaskState.TODAY.name)
        )
        viewModel.startSnooze(tasks)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTask(tasks[0])
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).deleteTask(check { task ->
            assertEquals("1", task.id)
        })

        assertNull(viewModel.currentTask.value)
    }

    @Test
    fun testMoveToTomorrowCalculatesCorrectDueDateAndAdvancesQueue() = runTest(testDispatcher) {
        val origDueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
        }.timeInMillis

        val tasks = listOf(
            Task(id = "1", title = "Task 1", state = TaskState.TODAY.name, dueDate = origDueDate)
        )
        viewModel.startSnooze(tasks)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.moveToTomorrow(tasks[0])
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).updateTask(check { task ->
            assertEquals("1", task.id)
            assertEquals(TaskState.UPCOMING.name, task.state)
            
            val expectedDueDate = Calendar.getInstance().apply {
                timeInMillis = origDueDate
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            assertEquals(expectedDueDate, task.dueDate)
        })

        assertNull(viewModel.currentTask.value)
    }
}
