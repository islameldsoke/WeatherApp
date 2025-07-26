package com.example.weatherapp.data.local.model



import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_images")
data class CapturedImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String, // URI or file path of the saved image
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val weatherDescription: String,
    val weatherIconCode: String,
    val cityName: String,
    val timestamp: Long // Unix timestamp when the image was captured
)

