package com.vega.ui.viewmodels

import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.data.repository.TaskRepository
import com.vega.parser.NaturalLanguageParser
import com.vega.parser.ParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {

    @Mock
    private lateinit var repository: TaskRepository

    @Mock
    private lateinit var parser: NaturalLanguageParser

    private lateinit var viewModel: QuickAddViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = QuickAddViewModel(repository, parser)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testParseInputUpdatesPreview() = runTest(testDispatcher) {
        val parseResult = ParseResult("Buy milk", 123456L, TaskPriority.HIGH)
        `when`(parser.parse(org.mockito.kotlin.eq("Buy milk tomorrow high"), org.mockito.kotlin.any())).thenReturn(parseResult)

        viewModel.parseInput("Buy milk tomorrow high")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(parseResult, viewModel.parseResult.value)
    }

    @Test
    fun testCreateTaskSuccess() = runTest(testDispatcher) {
        val parseResult = ParseResult("Buy milk", 123456L, TaskPriority.HIGH)
        `when`(parser.parse(org.mockito.kotlin.eq("Buy milk tomorrow high"), org.mockito.kotlin.any())).thenReturn(parseResult)
        
        viewModel.parseInput("Buy milk tomorrow high")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createTask()
        testDispatcher.scheduler.advanceUntilIdle()

        org.mockito.kotlin.verify(repository).createTask(org.mockito.kotlin.check { task ->
            assertEquals("Buy milk", task.title)
            assertEquals(123456L, task.dueDate)
            assertEquals(TaskPriority.HIGH.name, task.priority)
            assertEquals(TaskState.INBOX.name, task.state)
        })

        assertTrue(viewModel.uiState.value is QuickAddUiState.Success)
    }

    @Test
    fun testCreateTaskEmptyTitleRejected() = runTest(testDispatcher) {
        val parseResult = ParseResult("", null, TaskPriority.NONE)
        `when`(parser.parse(org.mockito.kotlin.eq(""), org.mockito.kotlin.any())).thenReturn(parseResult)

        viewModel.parseInput("")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createTask()
        testDispatcher.scheduler.advanceUntilIdle()

        org.mockito.kotlin.verify(repository, never()).createTask(any())

        assertTrue(viewModel.uiState.value is QuickAddUiState.Error)
        assertEquals("Task title cannot be empty", (viewModel.uiState.value as QuickAddUiState.Error).message)
    }
}
