package com.example.sleeptrackerapp

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class InactivityWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        checkSleepActivity()
        return Result.success()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun checkSleepActivity() {
        // Vérifier les SharedPreferences pour la dernière date de sommeil
        val sharedPref = context.getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        val lastSleepDateStr = sharedPref.getString("last_sleep_date", null)

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Si aucune donnée aujourd'hui, envoyer une notif
        if (lastSleepDateStr != todayStr) {
            val channelId = "sleep_reminders"
            val notificationId = 1003

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stats)
                .setContentTitle("Pas de sommeil enregistré ?")
                .setContentText("N'oubliez pas d'ajouter votre nuit pour suivre vos statistiques.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            try {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }
}

fun scheduleInactivityCheck(context: Context) {
    // Programmer une vérification tous les jours à midi par exemple
    val workManager = WorkManager.getInstance(context)
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12) // Vérification à midi
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
    }

    val initialDelay = target.timeInMillis - now.timeInMillis
    val request = PeriodicWorkRequestBuilder<InactivityWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    workManager.enqueueUniquePeriodicWork("InactivityCheck", ExistingPeriodicWorkPolicy.KEEP, request)
}