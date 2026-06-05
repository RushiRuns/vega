package com.vega.workers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.vega.data.database.Task
import com.vega.data.database.TaskState
import com.vega.data.repository.TaskRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
class ReminderWorkersTest {

    @Mock
    private lateinit var repository: TaskRepository

    private lateinit var context: Context
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)

        val executor = java.util.concurrent.Executor { command -> command.run() }
        val serialExecutor = mock(androidx.work.impl.utils.taskexecutor.SerialExecutor::class.java)
        val taskExecutor = mock(androidx.work.impl.utils.taskexecutor.TaskExecutor::class.java)
        whenever(taskExecutor.serialTaskExecutor).thenReturn(serialExecutor)

        workerParams = WorkerParameters(
            java.util.UUID.randomUUID(),
            androidx.work.Data.EMPTY,
            emptyList(),
            WorkerParameters.RuntimeExtras(),
            1,
            0,
            executor,
            taskExecutor,
            androidx.work.WorkerFactory.getDefaultWorkerFactory(),
            mock(androidx.work.ProgressUpdater::class.java),
            mock(androidx.work.ForegroundUpdater::class.java)
        )

        DailyReminderWorker.repositoryProvider = { repository }

        // Reset shared preferences
        val prefs = context.getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @After
    fun tearDown() {
        DailyReminderWorker.repositoryProvider = null
    }

    @Test
    fun testDailyReminderWorkerDisabled() = runTest {
        val prefs = context.getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pref_daily_reminders", false).commit()

        val worker = DailyReminderWorker(context, workerParams)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, shadowNotificationManager.size())
    }

    @Test
    fun testDailyReminderWorkerWithTasks() = runTest {
        val prefs = context.getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pref_daily_reminders", true).commit()

        val tasks = listOf(Task(id = "1", title = "Task 1", state = TaskState.TODAY.name))
        whenever(repository.getTodayTasks()).thenReturn(flowOf(tasks))

        val worker = DailyReminderWorker(context, workerParams)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, shadowNotificationManager.size())

        val notifications = shadowNotificationManager.allNotifications
        val notification = notifications[0]
        assertEquals("You still have 1 tasks in Today.", notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
        assertEquals("Tasks remaining", notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
    }

    @Test
    fun testWeeklyReviewWorkerDisabled() = runTest {
        val prefs = context.getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pref_weekly_review", false).commit()

        val worker = WeeklyReviewWorker(context, workerParams)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, shadowNotificationManager.size())
    }

    @Test
    fun testWeeklyReviewWorkerEnabled() = runTest {
        val prefs = context.getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pref_weekly_review", true).commit()

        val worker = WeeklyReviewWorker(context, workerParams)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, shadowNotificationManager.size())

        val notifications = shadowNotificationManager.allNotifications
        val notification = notifications[0]
        assertEquals("Review your Inbox and plan your week", notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
        assertEquals("Weekly Review", notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
    }
}
