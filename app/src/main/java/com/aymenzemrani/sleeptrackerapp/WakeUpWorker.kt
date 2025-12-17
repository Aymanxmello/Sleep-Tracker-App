package com.aymenzemrani.sleeptrackerapp

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WakeUpWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        sendWakeUpNotification()
        return Result.success()
    }

    private fun sendWakeUpNotification() {
        val channelId = "sleep_reminders"
        val notificationId = 1002

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_moon) // Assurez-vous d'avoir cette icône
            .setContentTitle("Bonjour ! ☀️")
            .setContentText("C'est l'heure de se lever !")
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
        .addTag("wake_up_tag")
        .build()

    workManager.enqueueUniquePeriodicWork(
        "DailyWakeUpReminder",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}