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
import com.vega.data.database.Task
import com.vega.data.repository.TaskRepository
import com.vega.ui.activities.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class EndOfDaySnoozeWorker(
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
        const val EXTRA_START_SNOOZE = "EXTRA_START_SNOOZE"
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

        val enabled = prefs.getBoolean("pref_end_of_day_reminders", true)
        if (!enabled) {
            return Result.success()
        }

        val repository = getRepository()
        val tasks = repository.getTodayTasksForSnooze().first()

        if (tasks.isEmpty()) {
            return Result.success()
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        for (task in tasks) {
            val notification = buildNotification(task)
            notificationManager.notify(task.id.hashCode(), notification)
        }

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

    private fun buildNotification(task: Task): Notification {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_START_SNOOZE, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = try {
            applicationContext.getString(R.string.snooze_prompt_title)
        } catch (e: Exception) {
            "Unfinished task from Today"
        }

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(task.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
}
