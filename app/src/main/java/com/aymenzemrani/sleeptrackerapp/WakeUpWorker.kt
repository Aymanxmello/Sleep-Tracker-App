package com.aymenzemrani.sleeptrackerapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.util.Calendar

// We use AlarmManager now for precise "Alarm" behavior instead of WorkManager
fun scheduleWakeUpReminder(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)

    val operationPendingIntent = PendingIntent.getBroadcast(
        context,
        1002, // ID for WakeUp
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Intent to open the app (required for setAlarmClock)
    val showIntent = Intent(context, MainActivity::class.java)
    val showPendingIntent = PendingIntent.getActivity(
        context,
        123,
        showIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(now)) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(target.timeInMillis, showPendingIntent),
                    operationPendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    operationPendingIntent
                )
            }
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(target.timeInMillis, showPendingIntent),
                operationPendingIntent
            )
        }

        // Feedback to user
        val dateFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        Toast.makeText(
            context,
            "Alarme réglée pour ${dateFormat.format(target.time)}",
            Toast.LENGTH_SHORT
        ).show()

    } catch (e: SecurityException) {
        e.printStackTrace()
        Toast.makeText(context, "Erreur permission alarme", Toast.LENGTH_SHORT).show()
    }
}

fun cancelWakeUpReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1002,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}
