package com.vega.alarms

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vega.data.database.Task
import com.vega.data.database.TaskDao
import com.vega.data.database.TaskState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskDao: TaskDao

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("EXTRA_TASK_ID") ?: return
        Log.d("TaskAlarmReceiver", "Received task alarm broadcast for ID: $taskId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskDao.getTaskById(taskId)
                if (task != null && task.state != TaskState.DONE.name) {
                    val prefs = context.getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
                    val mode = prefs.getString("pref_notification_mode", "fixed_ritual")
                    if (mode == "task_specific") {
                        showNotification(context, task)
                    } else {
                        Log.d("TaskAlarmReceiver", "Alarm skipped because notification mode is: $mode")
                    }
                } else {
                    Log.d("TaskAlarmReceiver", "Alarm skipped because task is null or DONE.")
                }
            } catch (e: Exception) {
                Log.e("TaskAlarmReceiver", "Failed to process task alarm for ID: $taskId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, task: Task) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to launch MainActivity when clicking the notification
        val clickIntent = Intent(context, com.vega.ui.activities.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "vega_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task Reminder")
            .setContentText(task.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(task.id.hashCode(), notification)
        Log.d("TaskAlarmReceiver", "Posted notification for task: ${task.title}")
    }
}
