package com.vega.alarms

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.vega.data.database.Task
import com.vega.data.database.TaskDao
import com.vega.data.database.TaskState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var scheduler: TaskAlarmScheduler
    private lateinit var prefs: SharedPreferences

    @Mock
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        prefs = context.getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        scheduler = TaskAlarmScheduler(context, taskDao)
    }

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
    }

    @Test
    fun testScheduleAlarm_doesNothingIfModeIsFixedRitual() {
        prefs.edit().putString("pref_notification_mode", "fixed_ritual").commit()

        val task = Task(
            id = "task1",
            title = "Task 1",
            state = TaskState.TODAY.name,
            dueDate = System.currentTimeMillis() + 600000 // 10 mins in future
        )

        scheduler.scheduleAlarm(task)

        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testScheduleAlarm_cancelsIfDone() {
        prefs.edit().putString("pref_notification_mode", "task_specific").commit()

        val task = Task(
            id = "task1",
            title = "Task 1",
            state = TaskState.DONE.name,
            dueDate = System.currentTimeMillis() + 600000
        )

        scheduler.scheduleAlarm(task)

        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testScheduleAlarm_cancelsIfNoDueDate() {
        prefs.edit().putString("pref_notification_mode", "task_specific").commit()

        val task = Task(
            id = "task1",
            title = "Task 1",
            state = TaskState.TODAY.name,
            dueDate = null
        )

        scheduler.scheduleAlarm(task)

        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testScheduleAlarm_schedulesCorrectly() {
        prefs.edit()
            .putString("pref_notification_mode", "task_specific")
            .putInt("pref_alarm_offset_minutes", 5)
            .commit()

        val now = System.currentTimeMillis()
        val dueDate = now + 600000 // 10 minutes in future
        val task = Task(
            id = "task1",
            title = "Task 1",
            state = TaskState.TODAY.name,
            dueDate = dueDate
        )

        scheduler.scheduleAlarm(task)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(1, scheduledAlarms.size)

        val alarm = scheduledAlarms[0]
        assertEquals(dueDate - (5 * 60 * 1000), alarm.triggerAtTime)
    }

    @Test
    fun testScheduleAlarm_fallbackToImmediateIfTriggerInPastButDueInFuture() {
        prefs.edit()
            .putString("pref_notification_mode", "task_specific")
            .putInt("pref_alarm_offset_minutes", 10)
            .commit()

        val now = System.currentTimeMillis()
        val dueDate = now + 300000 // 5 minutes in future, trigger time is 5 minutes in past
        val task = Task(
            id = "task1",
            title = "Task 1",
            state = TaskState.TODAY.name,
            dueDate = dueDate
        )

        scheduler.scheduleAlarm(task)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(1, scheduledAlarms.size)

        val alarm = scheduledAlarms[0]
        // Should trigger immediately (approx now + 1000)
        assertTrue(alarm.triggerAtTime >= now)
        assertTrue(alarm.triggerAtTime <= now + 2000)
    }

    @Test
    fun testCancelAlarm() {
        prefs.edit()
            .putString("pref_notification_mode", "task_specific")
            .putInt("pref_alarm_offset_minutes", 0)
            .commit()

        val task = Task(
            id = "task1",
            title = "Task 1",
            state = TaskState.TODAY.name,
            dueDate = System.currentTimeMillis() + 600000
        )

        scheduler.scheduleAlarm(task)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        scheduler.cancelAlarm(task.id)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testRescheduleAllAlarms_schedulesActiveTasks() = runTest {
        prefs.edit()
            .putString("pref_notification_mode", "task_specific")
            .putInt("pref_alarm_offset_minutes", 5)
            .commit()

        val task1 = Task(id = "1", title = "Task 1", state = TaskState.TODAY.name, dueDate = System.currentTimeMillis() + 600000)
        val task2 = Task(id = "2", title = "Task 2", state = TaskState.UPCOMING.name, dueDate = System.currentTimeMillis() + 900000)

        `when`(taskDao.getAllActiveTasks()).thenReturn(listOf(task1, task2))

        scheduler.rescheduleAllAlarms()

        var attempts = 0
        while (shadowAlarmManager.scheduledAlarms.size < 2 && attempts < 20) {
            Thread.sleep(50)
            attempts++
        }

        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testRescheduleAllAlarms_cancelsActiveTasksIfModeFixedRitual() = runTest {
        // First schedule them
        prefs.edit()
            .putString("pref_notification_mode", "task_specific")
            .putInt("pref_alarm_offset_minutes", 5)
            .commit()

        val task1 = Task(id = "1", title = "Task 1", state = TaskState.TODAY.name, dueDate = System.currentTimeMillis() + 600000)
        scheduler.scheduleAlarm(task1)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // Switch mode to fixed_ritual
        prefs.edit().putString("pref_notification_mode", "fixed_ritual").commit()
        `when`(taskDao.getAllActiveTasks()).thenReturn(listOf(task1))

        scheduler.rescheduleAllAlarms()

        var attempts = 0
        while (shadowAlarmManager.scheduledAlarms.size > 0 && attempts < 20) {
            Thread.sleep(50)
            attempts++
        }

        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }
}
