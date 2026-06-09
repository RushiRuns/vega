package com.vega.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vega.alarms.TaskAlarmScheduler
import com.vega.data.database.RecurrenceRule
import com.vega.data.database.Task
import com.vega.data.database.TaskDao
import com.vega.data.database.TaskState
import com.vega.data.database.VegaDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TaskRepositoryTest {

    private lateinit var db: VegaDatabase
    private lateinit var dao: TaskDao
    private lateinit var repository: TaskRepository

    @Mock
    private lateinit var alarmScheduler: TaskAlarmScheduler

    @Before
    fun createDb() {
        MockitoAnnotations.openMocks(this)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VegaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.taskDao()
        repository = TaskRepository(dao, alarmScheduler)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testGetInboxTasks() = runTest {
        val task1 = Task(id = "1", title = "Inbox Task", state = TaskState.INBOX.name)
        val task2 = Task(id = "2", title = "Today Task", state = TaskState.TODAY.name)
        
        repository.createTask(task1)
        repository.createTask(task2)

        val inboxTasks = repository.getInboxTasks().first()
        assertEquals(1, inboxTasks.size)
        assertEquals("Inbox Task", inboxTasks[0].title)
    }

    @Test
    fun testGetTodayTasks() = runTest {
        val task1 = Task(id = "1", title = "Inbox Task", state = TaskState.INBOX.name)
        val task2 = Task(id = "2", title = "Today Task", state = TaskState.TODAY.name)
        
        repository.createTask(task1)
        repository.createTask(task2)

        val todayTasks = repository.getTodayTasks().first()
        assertEquals(1, todayTasks.size)
        assertEquals("Today Task", todayTasks[0].title)
    }

    @Test
    fun testGetUpcomingTasksSortedByDueDate() = runTest {
        val tomorrow = System.currentTimeMillis() + 86400000L
        val dayAfterTomorrow = System.currentTimeMillis() + 2 * 86400000L
        val task1 = Task(id = "1", title = "Upcoming 1", state = TaskState.UPCOMING.name, dueDate = dayAfterTomorrow)
        val task2 = Task(id = "2", title = "Upcoming 2", state = TaskState.UPCOMING.name, dueDate = tomorrow)
        
        repository.createTask(task1)
        repository.createTask(task2)

        val upcomingTasks = repository.getUpcomingTasks().first()
        assertEquals(2, upcomingTasks.size)
        assertEquals("Upcoming 2", upcomingTasks[0].title)
        assertEquals("Upcoming 1", upcomingTasks[1].title)
    }

    @Test
    fun testGetDoneTasksSortedByRecency() = runTest {
        val task1 = Task(id = "1", title = "Done 1", state = TaskState.DONE.name, updatedAt = 1000L)
        val task2 = Task(id = "2", title = "Done 2", state = TaskState.DONE.name, updatedAt = 2000L)
        
        repository.createTask(task1)
        repository.createTask(task2)

        val doneTasks = repository.getDoneTasks().first()
        assertEquals(2, doneTasks.size)
        assertEquals("Done 2", doneTasks[0].title)
        assertEquals("Done 1", doneTasks[1].title)
    }

    @Test
    fun testGetUpcomingTasksFiltersOutRecurringTasksAndPastDue() = runTest {
        // Task 1: Non-recurring, due in future
        val task1 = Task(id = "1", title = "Non-recurring Future", state = TaskState.UPCOMING.name, dueDate = 2000L)
        // Task 2: Non-recurring, due in past/today
        val task2 = Task(id = "2", title = "Non-recurring Past", state = TaskState.UPCOMING.name, dueDate = 500L)
        // Task 3: Recurring, due in future
        val task3 = Task(id = "3", title = "Recurring Future", state = TaskState.UPCOMING.name, dueDate = 2000L, recurrence = "DAILY")
        
        repository.createTask(task1)
        repository.createTask(task2)
        repository.createTask(task3)

        // Using 1000L as endOfToday
        val upcomingTasks = repository.getUpcomingTasks(1000L).first()
        // Should only return task1 because:
        // - task2 has dueDate <= 1000L
        // - task3 has recurrence not null/empty
        assertEquals(1, upcomingTasks.size)
        assertEquals("Non-recurring Future", upcomingTasks[0].title)
    }

    @Test
    fun testGetTodayTasksIncludesRecurringTasksWhoseStartDateIsTodayOrEarlier() = runTest {
        // Task 1: Non-recurring Today task
        val task1 = Task(id = "1", title = "State Today", state = TaskState.TODAY.name)
        // Task 2: Recurring task with startDate in the past (should appear in Today)
        val recurringPast = RecurrenceRule(frequency = "DAILY", startDate = 500L)
        val task2 = Task(id = "2", title = "Recurring Start Past", state = TaskState.TODAY.name, recurrence = recurringPast.toJson())
        // Task 3: Recurring task with startDate in the future (should NOT appear in Today)
        val recurringFuture = RecurrenceRule(frequency = "DAILY", startDate = 2000L)
        val task3 = Task(id = "3", title = "Recurring Start Future", state = TaskState.TODAY.name, recurrence = recurringFuture.toJson())
        // Task 4: Recurring task with startDate = endOfToday exactly (should appear)
        val recurringExact = RecurrenceRule(frequency = "DAILY", startDate = 1000L)
        val task4 = Task(id = "4", title = "Recurring Start Today", state = TaskState.TODAY.name, recurrence = recurringExact.toJson())

        repository.createTask(task1)
        repository.createTask(task2)
        repository.createTask(task3)
        repository.createTask(task4)

        // Using 1000L as endOfToday
        val todayTasks = repository.getTodayTasks(1000L).first()
        // Should return task1, task2, task4 — NOT task3 (future startDate)
        assertEquals(3, todayTasks.size)
        val titles = todayTasks.map { it.title }.toSet()
        assertTrue(titles.contains("State Today"))
        assertTrue(titles.contains("Recurring Start Past"))
        assertTrue(titles.contains("Recurring Start Today"))
        assertTrue(!titles.contains("Recurring Start Future"))
    }

    @Test
    fun testGetTodayTasksExcludesRecurringTasksWithFutureStartDate() = runTest {
        // Task 1: Recurring task with future startDate — should NOT appear in Today
        val ruleF = RecurrenceRule(frequency = "DAILY", startDate = 2000L)
        val task1 = Task(id = "1", title = "Recurring Future Start", state = TaskState.TODAY.name, recurrence = ruleF.toJson())
        // Task 2: Recurring task with past/today startDate — should appear in Today
        val ruleP = RecurrenceRule(frequency = "DAILY", startDate = 1000L)
        val task2 = Task(id = "2", title = "Recurring Past Start", state = TaskState.TODAY.name, recurrence = ruleP.toJson())

        repository.createTask(task1)
        repository.createTask(task2)

        // Using 1000L as endOfToday
        val todayTasks = repository.getTodayTasks(1000L).first()
        // Should only return task2 because task1 has startDate > 1000L
        assertEquals(1, todayTasks.size)
        assertEquals("Recurring Past Start", todayTasks[0].title)
    }
}
