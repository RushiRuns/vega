package com.vega.ui.viewmodels

import com.vega.data.database.Task
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
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @Mock
    private lateinit var repository: TaskRepository

    private lateinit var viewModel: SearchViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEmptyQueryReturnsEmptyListImmediately() = runTest(testDispatcher) {
        viewModel.setSearchQuery("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.searchResults.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
        verifyNoInteractions(repository)
    }

    @Test
    fun testSearchQueryDebouncesAndQueriesRepository() = runTest(testDispatcher) {
        val tasks = listOf(Task(id = "1", title = "Go to Dentist"))
        whenever(repository.searchTasks("Dentist")).thenReturn(flowOf(tasks))

        viewModel.setSearchQuery("Dentist")
        // Loading state is set to true immediately when setting a query
        assertTrue(viewModel.isLoading.value)

        // Advance time past the 300ms debounce
        testDispatcher.scheduler.advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).searchTasks("Dentist")
        assertEquals(tasks, viewModel.searchResults.value)
        assertFalse(viewModel.isLoading.value) // Done loading
    }
}
