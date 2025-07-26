package com.example.weatherapp.utils

import com.example.weatherforecastapp.R

object Constants {
    // IMPORTANT: Replace with your actual OpenWeatherMap API Key
    const val OPEN_WEATHER_MAP_API_KEY = "ed002b42e975866a9b9b5d44f3982bd7"
    const val WEATHER_API_BASE_URL = "https://api.openweathermap.org/data/2.5/"

    // Default values
    const val DEFAULT_LATITUDE = 30.7997 // Example: Tanta, Egypt
    const val DEFAULT_LONGITUDE = 31.0003 // Example: Tanta, Egypt

    // Image saving
    const val IMAGE_FILE_NAME_PREFIX = "WeatherImage_"
    const val IMAGE_MIME_TYPE = "image/jpeg"
    const val IMAGE_COMPRESSION_QUALITY = 90 // 0-100

    // Weather icon mapping (simplified, you'd expand this)
    fun getWeatherIconResId(iconCode: String?): Int {
        return when (iconCode) {
            "01d", "01n" -> R.drawable.clear_sky // Sunny/Clear
            "02d", "02n" -> R.drawable.few_clouds // Few clouds
            "03d", "03n" -> R.drawable.scattered_clouds // Scattered clouds
            "04d", "04n" -> R.drawable.broken_clouds // Broken clouds
            "09d", "09n" -> R.drawable.shower_rain // Shower rain
            "10d", "10n" -> R.drawable.rain // Rain
            "11d", "11n" -> R.drawable.thunderstorm // Thunderstorm
            "13d", "13n" -> R.drawable.snow // Snow
            "50d", "50n" -> R.drawable.mist // Mist
            else -> android.R.drawable.ic_menu_help // Default/Unknown
        }
    }
}