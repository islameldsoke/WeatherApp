package com.example.weatherapp.data.remote.api
import com.example.weatherapp.data.remote.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" // or "imperial" for Fahrenheit
    ): WeatherResponse
}