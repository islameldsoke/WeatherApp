package com.example.weatherapp.domain.usecase


import android.content.Context
import android.location.Location
import com.example.weatherapp.data.repository.WeatherRepository
import com.example.weatherapp.domain.model.Weather
import com.example.weatherapp.utils.LocationUtils


/**
 * Use case for fetching weather data.
 * This class encapsulates the business logic of getting the user's location
 * and then fetching the weather for that location.
 *
 * @param weatherRepository The repository responsible for fetching weather data from an API.
 * @param locationUtils The utility class for handling location services.
 */
class GetWeatherUseCase(
    private val weatherRepository: WeatherRepository,
    private val locationUtils: LocationUtils
) {

    /**
     * Executes the use case to get current weather data for the user's location.
     * It first attempts to get a fresh location update, and if that fails,
     * it falls back to the last known location.
     *
     * @param context The application context, required for location permission checks.
     * @param isCelsius A boolean indicating whether temperature should be in Celsius or Fahrenheit.
     * @return A Result object containing Weather data on success, or an Exception on failure.
     */
    suspend fun execute(context: Context, isCelsius: Boolean): Result<Weather> {
        // 1. Check for location permissions
        if (!locationUtils.hasLocationPermissions(context)) {
            return Result.failure(SecurityException("Location permissions not granted."))
        }

        // 2. Attempt to get a fresh location update
        val locationResult = locationUtils.requestSingleLocationUpdate(context)
        val location: Location? = locationResult.getOrNull()

        val lat: Double
        val lon: Double

        if (location == null) {
            // 3. If fresh update fails, try last known location (might be stale)
            val lastLocationResult = locationUtils.getLastLocation(context)
            val lastLocation: Location? = lastLocationResult.getOrNull()

            if (lastLocation == null) {
                return Result.failure(
                    Exception("Could not get current location. Please enable location services or grant permissions.")
                )
            } else {
                // Use last known location
                lat = lastLocation.latitude
                lon = lastLocation.longitude
                // In a real app, you might want to inform the user that the location is stale.
            }
        } else {
            // Use the fresh location
            lat = location.latitude
            lon = location.longitude
        }

        // 4. Fetch weather data using the obtained coordinates
        return weatherRepository.getCurrentWeather(lat, lon, isCelsius)
    }
}
