package com.vega.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vega.R
import com.vega.data.repository.TaskRepository
import com.vega.ui.activities.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class DailyReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun taskRepository(): TaskRepository
    }

    companion object {
        var repositoryProvider: ((Context) -> TaskRepository)? = null
        const val CHANNEL_ID = "vega_reminders"
    }

    private fun getRepository(): TaskRepository {
        return repositoryProvider?.invoke(applicationContext) ?: EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java
        ).taskRepository()
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getString("pref_notification_mode", "fixed_ritual")
        if (mode == "task_specific") {
            return Result.success()
        }

        val enabled = prefs.getBoolean("pref_daily_reminders", true)
        if (!enabled) {
            return Result.success()
        }

        val repository = getRepository()
        val todayTasks = repository.getTodayTasks().first()

        if (todayTasks.isEmpty()) {
            return Result.success()
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val notification = buildNotification(todayTasks.size)
        notificationManager.notify(1800, notification)

        return Result.success()
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vega Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Vega reminders and snooze prompts"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(taskCount: Int): Notification {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1800,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "You still have $taskCount tasks in Today."

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Tasks remaining")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
}
