package com.example.sleeptrackerapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)

        // Mise à jour de la configuration pour l'application
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // Sauvegarde de la préférence
        val sharedPref = context.getSharedPreferences("app_pref", Context.MODE_PRIVATE)
        sharedPref.edit().putString("app_lang", languageCode).apply()
    }

    fun loadLocale(context: Context) {
        val sharedPref = context.getSharedPreferences("app_pref", Context.MODE_PRIVATE)
        val language = sharedPref.getString("app_lang", "") ?: ""
        if (language.isNotEmpty()) {
            setLocale(context, language)
        }
    }

    fun restartApp(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }
}