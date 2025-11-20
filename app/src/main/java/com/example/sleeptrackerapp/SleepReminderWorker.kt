package com.example.sleeptrackerapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SleepReminderWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        sendSleepReminderNotification()
        return Result.success()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendSleepReminderNotification() {
        val channelId = "sleep_reminders"
        val notificationId = 1001

        // Création du canal de notification (Android 8+)
        val name = "Rappels de sommeil"
        val descriptionText = "Rappels pour l'heure du coucher"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // Intent pour ouvrir l'application
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Construction de la notification "Pré-sommeil"
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle("Il est temps de dormir ! 🌙")
            .setContentText("Préparez-vous pour une bonne nuit de sommeil. Éloignez les écrans.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Gérer les permissions ici
        }
    }
}

// Fonction pour programmer le rappel quotidien
fun scheduleSleepReminder(context: Context, hour: Int, minute: Int) {
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

    val reminderRequest = PeriodicWorkRequestBuilder<SleepReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    workManager.enqueueUniquePeriodicWork(
        "DailySleepReminder",
        ExistingPeriodicWorkPolicy.UPDATE,
        reminderRequest
    )
}

fun cancelSleepReminder(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("DailySleepReminder")
}