package com.example.sleeptrackerapp

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WakeUpWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        sendWakeUpNotification()
        return Result.success()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendWakeUpNotification() {
        val channelId = "sleep_reminders" // On réutilise le même canal
        val notificationId = 1002

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_moon) // Remplacez par une icône de soleil si disponible
            .setContentTitle("Bonjour ! ☀️")
            .setContentText("C'est l'heure de se lever et de commencer la journée.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) { e.printStackTrace() }
    }
}

fun scheduleWakeUpReminder(context: Context, hour: Int, minute: Int) {
    val workManager = WorkManager.getInstance(context)
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (before(now)) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val initialDelay = target.timeInMillis - now.timeInMillis
    val request = PeriodicWorkRequestBuilder<WakeUpWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    workManager.enqueueUniquePeriodicWork("DailyWakeUpReminder", ExistingPeriodicWorkPolicy.UPDATE, request)
}