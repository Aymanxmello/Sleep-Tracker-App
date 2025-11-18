package com.example.sleeptrackerapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SleepReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Envoyer une notification de rappel pour l'heure du coucher
        sendSleepReminderNotification()
        return Result.success()
    }

    private fun sendSleepReminderNotification() {
        // Implémentation de l'envoi de notification
    }
}

// Utilisation de WorkManager pour programmer les rappels
fun scheduleSleepReminders(context: Context) {
    val reminderRequest = OneTimeWorkRequestBuilder<SleepReminderWorker>()
        .setInitialDelay(1, TimeUnit.HOURS) // Exemple: rappel dans 1 heure
        .build()

    WorkManager.getInstance(context).enqueue(reminderRequest)
}