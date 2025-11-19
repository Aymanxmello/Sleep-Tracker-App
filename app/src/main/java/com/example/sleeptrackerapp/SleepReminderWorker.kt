package com.example.sleeptrackerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

    override suspend fun doWork(): Result {
        sendSleepReminderNotification()
        return Result.success()
    }

    private fun sendSleepReminderNotification() {
        val channelId = "sleep_reminders"
        val notificationId = 1001

        // Créer le canal de notification (nécessaire pour Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Rappels de sommeil"
            val descriptionText = "Rappels pour l'heure du coucher"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Intent pour ouvrir l'app au clic
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Construire la notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_moon) // Assurez-vous que cette icône existe
            .setContentTitle("Il est temps de dormir ! 🌙")
            .setContentText("Préparez-vous pour une bonne nuit de sommeil. Éloignez les écrans.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Afficher
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Gérer la permission POST_NOTIFICATIONS sur Android 13+ ici si nécessaire
        }
    }
}

// Fonction utilitaire pour programmer le rappel
fun scheduleSleepReminder(context: Context, hour: Int, minute: Int) {
    val workManager = WorkManager.getInstance(context)

    // Calculer le délai initial jusqu'à l'heure cible
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (before(now)) {
            add(Calendar.DAY_OF_YEAR, 1) // Si l'heure est passée, programmer pour demain
        }
    }

    val initialDelay = target.timeInMillis - now.timeInMillis

    // Requête périodique (toutes les 24h)
    val reminderRequest = PeriodicWorkRequestBuilder<SleepReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    workManager.enqueueUniquePeriodicWork(
        "DailySleepReminder",
        ExistingPeriodicWorkPolicy.UPDATE, // Remplace l'ancien rappel si on change l'heure
        reminderRequest
    )
}

// Fonction pour annuler les rappels
fun cancelSleepReminder(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("DailySleepReminder")
}