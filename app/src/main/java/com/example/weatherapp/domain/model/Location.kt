package com.example.weatherapp.domain.model

// This is a clean data class representing location information,
// independent of the Android Location API or any specific data source.
data class Location(
    val latitude: Double,
    val longitude: Double
)
