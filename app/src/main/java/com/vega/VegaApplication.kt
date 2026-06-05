package com.vega

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vega.workers.EndOfDaySnoozeWorker
import com.vega.ui.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class VegaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyTheme(this)
        scheduleEndOfDaySnooze()
        scheduleDailyReminder()
        scheduleWeeklyReview()
    }

    private fun scheduleEndOfDaySnooze() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val snoozeRequest = PeriodicWorkRequestBuilder<com.vega.workers.EndOfDaySnoozeWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "EndOfDaySnoozeWork",
            ExistingPeriodicWorkPolicy.KEEP,
            snoozeRequest
        )
    }

    private fun scheduleDailyReminder() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val dailyRequest = PeriodicWorkRequestBuilder<com.vega.workers.DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )
    }

    private fun scheduleWeeklyReview() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val weeklyRequest = PeriodicWorkRequestBuilder<com.vega.workers.WeeklyReviewWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeeklyReviewWork",
            ExistingPeriodicWorkPolicy.KEEP,
            weeklyRequest
        )
    }
}
