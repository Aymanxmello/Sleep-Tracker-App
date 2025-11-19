package com.example.sleeptrackerapp

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface WeatherService {

    // Endpoint pour l'API One Call d'OpenWeatherMap
    @GET("data/2.5/onecall?exclude=minutely,hourly,alerts&units=metric")
    suspend fun fetchWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String
    ): OneCallResponse
}

// Client Retrofit
object WeatherApiClient {
    // URL de base de l'API OpenWeatherMap
    private const val BASE_URL = "https://api.openweathermap.org/"

    val service: WeatherService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }
}