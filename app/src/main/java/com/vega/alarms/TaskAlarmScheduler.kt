package com.vega.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.vega.data.database.Task
import com.vega.data.database.TaskDao
import com.vega.data.database.TaskState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val taskDao: TaskDao
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val prefs by lazy {
        context.getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Schedules a precise alarm for the given task.
     * If the task is completed or has no due date, any existing alarm is cancelled.
     */
    fun scheduleAlarm(task: Task) {
        val mode = prefs.getString("pref_notification_mode", "fixed_ritual")
        if (mode != "task_specific") {
            cancelAlarm(task.id)
            return
        }

        val dueDate = task.dueDate
        if (dueDate == null || task.state == TaskState.DONE.name) {
            cancelAlarm(task.id)
            return
        }

        // Retrieve alarm offset in minutes (default is 5)
        val offsetMinutes = prefs.getInt("pref_alarm_offset_minutes", 5)
        val triggerTime = dueDate - (offsetMinutes * 60 * 1000)

        // Fallback: If trigger time is in the past, but the due date is still in the future,
        // schedule it to fire immediately (in 1 second) so the reminder isn't missed.
        val targetTime = if (triggerTime < System.currentTimeMillis()) {
            if (dueDate > System.currentTimeMillis()) {
                System.currentTimeMillis() + 1000
            } else {
                // Due date is already in the past, cancel any active alarm
                cancelAlarm(task.id)
                return
            }
        } else {
            triggerTime
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetTime,
                    pendingIntent
                )
            }
            Log.d("TaskAlarmScheduler", "Scheduled alarm for task: '${task.title}' at: $targetTime")
        } catch (e: SecurityException) {
            // Graceful fallback for security permission restrictions on Android 12+
            Log.w("TaskAlarmScheduler", "Exact alarm permission not granted, falling back to inexact alarm.", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetTime,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e("TaskAlarmScheduler", "Failed to schedule alarm for task: ${task.id}", e)
        }
    }

    /**
     * Cancels any active alarm for the given task ID.
     */
    fun cancelAlarm(taskId: String) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("TaskAlarmScheduler", "Cancelled alarm for task ID: $taskId")
        }
    }

    /**
     * Cancels all scheduled task alarms if the mode is "fixed_ritual".
     * Reschedules alarms for all incomplete tasks with due dates if the mode is "task_specific".
     */
    fun rescheduleAllAlarms() {
        val mode = prefs.getString("pref_notification_mode", "fixed_ritual")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeTasks = taskDao.getAllActiveTasks()
                for (task in activeTasks) {
                    if (mode == "task_specific") {
                        scheduleAlarm(task)
                    } else {
                        cancelAlarm(task.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskAlarmScheduler", "Failed to reschedule all alarms", e)
            }
        }
    }
}
