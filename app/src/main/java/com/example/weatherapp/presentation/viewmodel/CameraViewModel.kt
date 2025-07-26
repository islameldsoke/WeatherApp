package com.example.weatherapp.presentation.viewmodel


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.repository.ImageRepository
import com.example.weatherapp.data.repository.WeatherRepository
import com.example.weatherapp.domain.model.Weather
import com.example.weatherapp.utils.LocationUtils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch





class CameraViewModel(
    private val weatherRepository: WeatherRepository,
    private val imageRepository: ImageRepository,
    private val locationUtils: LocationUtils
) : ViewModel() {

    // UI State for weather data
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Idle)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    // UI State for temperature unit
    private val _isCelsius = MutableStateFlow(true)
    val isCelsius: StateFlow<Boolean> = _isCelsius.asStateFlow()

    // UI State for image saving process
    private val _imageSaveState = MutableStateFlow<ImageSaveState>(ImageSaveState.Idle)
    val imageSaveState: StateFlow<ImageSaveState> = _imageSaveState.asStateFlow()

    // Store the last successfully fetched weather in its original unit (Celsius)
    private var lastFetchedWeatherCelsius: Weather? = null

    fun fetchWeather(context: Context) {
        _weatherState.value = WeatherState.Loading
        viewModelScope.launch {
            if (!locationUtils.hasLocationPermissions(context)) {
                _weatherState.value = WeatherState.Error("Location permissions not granted.")
                return@launch
            }

            // Try to get a fresh location update first
            val locationResult = locationUtils.requestSingleLocationUpdate(context)
            val location: Location? = locationResult.getOrNull()

            if (location == null) {
                // If fresh update fails, try last known location (might be stale)
                val lastLocationResult = locationUtils.getLastLocation(context)
                val lastLocation: Location? = lastLocationResult.getOrNull()

                if (lastLocation == null) {
                    _weatherState.value = WeatherState.Error("Could not get current location. Please enable location services or grant permissions.")
                    return@launch
                } else {
                    // Use last known location but inform user it might be stale
                    _weatherState.value = WeatherState.Error("Using last known location, might be outdated.")
                    fetchWeatherFromCoordinates(lastLocation.latitude, lastLocation.longitude)
                }
            } else {
                fetchWeatherFromCoordinates(location.latitude, location.longitude)
            }
        }
    }

    private fun fetchWeatherFromCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            // Always fetch in Celsius from the repository
            val result = weatherRepository.getCurrentWeather(lat, lon)
            result.onSuccess { weather ->
                lastFetchedWeatherCelsius = weather // Store the original Celsius weather
                // Update UI state with potentially converted temperature for display
                updateWeatherStateForDisplay(weather)
            }.onFailure { e ->
                _weatherState.value = WeatherState.Error(e.message ?: "Failed to fetch weather data.")
            }
        }
    }

    fun toggleTemperatureUnit() {
        _isCelsius.value = !_isCelsius.value // Toggle the display unit preference
        lastFetchedWeatherCelsius?.let { weather ->
            updateWeatherStateForDisplay(weather) // Re-update display based on new unit
        }
    }

    /**
     * Helper function to update the weatherState with the temperature
     * converted to the currently selected display unit (_isCelsius).
     */
    private fun updateWeatherStateForDisplay(weatherCelsius: Weather) {
        val temperatureToDisplay = if (_isCelsius.value) {
            weatherCelsius.temperature // Already in Celsius
        } else {
            // Convert Celsius to Fahrenheit
            (weatherCelsius.temperature * 9 / 5) + 32
        }
        // Create a new Weather object for display with the converted temperature
        _weatherState.value = WeatherState.Success(weatherCelsius.copy(temperature = temperatureToDisplay))
    }


    fun saveImageWithWeather(originalBitmap: Bitmap) {
        val currentWeatherForSave = lastFetchedWeatherCelsius // Always save the original Celsius value
        if (currentWeatherForSave == null) {
            _imageSaveState.value = ImageSaveState.Error("No weather data available to save with image.")
            return
        }

        _imageSaveState.value = ImageSaveState.Saving
        viewModelScope.launch {
            // Pass the original Celsius weather and the *current display preference* for overlay text
            imageRepository.saveCapturedImageWithWeather(originalBitmap, currentWeatherForSave, _isCelsius.value)
                .onSuccess {
                    _imageSaveState.value = ImageSaveState.Saved
                }
                .onFailure { e ->
                    _imageSaveState.value = ImageSaveState.Error(e.message ?: "Failed to save image.")
                }
        }
    }

    fun resetImageSaveState() {
        _imageSaveState.value = ImageSaveState.Idle
    }

    /**
     * Clears the current weather error message, effectively dismissing the error dialog.
     */
    fun clearWeatherError() {
        _weatherState.value = WeatherState.Idle // Reset to Idle state
    }

    // Sealed class to represent different states of weather data
    sealed class WeatherState {
        object Idle : WeatherState()
        object Loading : WeatherState()
        data class Success(val weather: Weather) : WeatherState() // Weather object here contains display temp
        data class Error(val message: String) : WeatherState()
    }

    // Sealed class to represent different states of image saving
    sealed class ImageSaveState {
        object Idle : ImageSaveState()
        object Saving : ImageSaveState()
        object Saved : ImageSaveState()
        data class Error(val message: String) : ImageSaveState()
    }
}

