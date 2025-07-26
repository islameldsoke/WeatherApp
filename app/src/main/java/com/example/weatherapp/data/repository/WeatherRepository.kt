package com.example.weatherapp.data.repository


import com.example.weatherapp.data.remote.api.WeatherApiService
import com.example.weatherapp.domain.model.Weather
import com.example.weatherapp.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class WeatherRepository(private val weatherApiService: WeatherApiService) {

    /**
     * Fetches current weather data for a given latitude and longitude.
     * This method will always request temperature in metric units (Celsius) from the API.
     *
     * @param lat Latitude.
     * @param lon Longitude.
     * @return A Result object containing Weather data on success, or an Exception on failure.
     */
    suspend fun getCurrentWeather(
        lat: Double,
        lon: Double
    ): Result<Weather> = withContext(Dispatchers.IO) {
        try {
            // Always request metric units (Celsius) from the API
            val response = weatherApiService.getCurrentWeather(
                lat = lat,
                lon = lon,
                apiKey = Constants.OPEN_WEATHER_MAP_API_KEY,
                units = "metric" // Always fetch in Celsius
            )
            // Map the remote WeatherResponse to our domain Weather model
            Result.success(
                Weather(
                    temperature = response.main.temp, // This will be in Celsius
                    humidity = response.main.humidity,
                    windSpeed = response.wind.speed,
                    description = response.weather.firstOrNull()?.description ?: "N/A",
                    iconCode = response.weather.firstOrNull()?.icon ?: "",
                    cityName = response.name
                )
            )
        } catch (e: Exception) {
            // Handle network errors or API errors
            when (e) {
                is retrofit2.HttpException -> {
                    val errorBody = e.response()?.errorBody()?.string()
                    Result.failure(IOException("HTTP Error: ${e.code()} - $errorBody", e))
                }
                is IOException -> {
                    Result.failure(IOException("Network Error: ${e.message}", e))
                }
                else -> {
                    Result.failure(Exception("An unexpected error occurred: ${e.message}", e))
                }
            }
        }
    }
}
