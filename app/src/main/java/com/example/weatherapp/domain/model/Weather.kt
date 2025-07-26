package com.example.weatherapp.domain.model

// This is a clean data class representing weather information,
// independent of the API response structure or local storage entity.
data class Weather(
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val description: String,
    val iconCode: String, // OpenWeatherMap icon code (e.g., "04d")
    val cityName: String
)
