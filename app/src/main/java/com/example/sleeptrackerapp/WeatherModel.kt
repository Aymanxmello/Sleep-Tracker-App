package com.example.sleeptrackerapp

import com.google.gson.annotations.SerializedName

// Top-level response for the One Call API
data class OneCallResponse(
    // Array contenant les prévisions quotidiennes
    val daily: List<DailyWeather>,
)

// Représente les données météo pour une seule journée
data class DailyWeather(
    // Timestamp Unix pour la date (secondes)
    val dt: Long,

    // Timestamp Unix pour le lever du soleil (secondes)
    val sunrise: Long,

    // Timestamp Unix pour le coucher du soleil (secondes)
    val sunset: Long,

    // Objet température
    val temp: Temp,
)

// Représente les données détaillées de température (nous utilisons 'day' pour l'affichage)
data class Temp(
    // Température de la journée
    val day: Double,

    // Température de la nuit
    val night: Double,

    // Température du soir
    val eve: Double
)