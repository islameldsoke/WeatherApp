package com.example.weatherapp.data.remote.model

import com.google.gson.annotations.SerializedName

// This file defines the data structure received from the OpenWeatherMap API

data class WeatherResponse(
    @SerializedName("coord") val coord: Coord,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("main") val main: Main,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("name") val name: String, // City name
    @SerializedName("dt") val dt: Long // Timestamp of data calculation
)

data class Coord(
    @SerializedName("lon") val lon: Double,
    @SerializedName("lat") val lat: Double
)

data class Weather(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String, // e.g., "Clouds", "Rain"
    @SerializedName("description") val description: String, // e.g., "overcast clouds"
    @SerializedName("icon") val icon: String // e.g., "04d"
)

data class Main(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("pressure") val pressure: Int,
    @SerializedName("humidity") val humidity: Int
)

data class Wind(
    @SerializedName("speed") val speed: Double, // Wind speed in m/s (metric) or mph (imperial)
    @SerializedName("deg") val deg: Int // Wind direction, degrees (meteorological)
)
