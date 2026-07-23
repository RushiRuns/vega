package com.vega.ui.viewmodels

import com.vega.data.database.Task
import com.vega.data.database.TaskState
import com.vega.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    @Mock
    private lateinit var repository: TaskRepository

    @Mock
    private lateinit var tagRepository: com.vega.data.repository.TagRepository

    private lateinit var viewModel: TodayViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        `when`(tagRepository.allTaskTagsMap).thenReturn(flowOf(emptyMap()))
        `when`(repository.countCompletedToday(org.mockito.kotlin.any())).thenReturn(flowOf(0))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun generateMockTasks(count: Int): List<Task> {
        return (1..count).map { id ->
            Task(
                id = "task_$id",
                title = "Task $id",
                state = TaskState.TODAY.name
            )
        }
    }

    @Test
    fun testTodayTasksInitiallyCappedAtSeven() = runTest(testDispatcher) {
        val mockTasks = generateMockTasks(10)
        `when`(repository.getTodayTasks()).thenReturn(flowOf(mockTasks))

        viewModel = TodayViewModel(repository, tagRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isExpanded.value)
        assertEquals(7, viewModel.todayTasks.value.size)
        assertEquals(3, viewModel.overflowCount.value)
        
        // Assert that the first 7 tasks are the ones shown
        for (i in 0 until 7) {
            assertEquals(mockTasks[i].id, viewModel.todayTasks.value[i].id)
        }
    }

    @Test
    fun testToggleExpandRevealsAllTasks() = runTest(testDispatcher) {
        val mockTasks = generateMockTasks(10)
        `when`(repository.getTodayTasks()).thenReturn(flowOf(mockTasks))

        viewModel = TodayViewModel(repository, tagRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle expand
        viewModel.toggleExpand()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isExpanded.value)
        assertEquals(10, viewModel.todayTasks.value.size)
        assertEquals(0, viewModel.overflowCount.value)
    }

    @Test
    fun testFewerThanSevenTasksNotCapped() = runTest(testDispatcher) {
        val mockTasks = generateMockTasks(4)
        `when`(repository.getTodayTasks()).thenReturn(flowOf(mockTasks))

        viewModel = TodayViewModel(repository, tagRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isExpanded.value)
        assertEquals(4, viewModel.todayTasks.value.size)
        assertEquals(0, viewModel.overflowCount.value)
    }
}
