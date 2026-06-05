package com.vega.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vega.data.database.Task
import com.vega.data.database.TaskDao
import com.vega.data.database.TaskState
import com.vega.data.database.VegaDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TaskRepositoryTest {

    private lateinit var db: VegaDatabase
    private lateinit var dao: TaskDao
    private lateinit var repository: TaskRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VegaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.taskDao()
        repository = TaskRepository(dao)
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
        val task1 = Task(id = "1", title = "Upcoming 1", state = TaskState.UPCOMING.name, dueDate = 2000L)
        val task2 = Task(id = "2", title = "Upcoming 2", state = TaskState.UPCOMING.name, dueDate = 1000L)
        
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
}
