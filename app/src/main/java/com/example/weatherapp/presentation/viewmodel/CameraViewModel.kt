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
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Idle) // Changed to Idle
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    // UI State for temperature unit
    private val _isCelsius = MutableStateFlow(true)
    val isCelsius: StateFlow<Boolean> = _isCelsius.asStateFlow()

    // UI State for image saving process
    private val _imageSaveState = MutableStateFlow<ImageSaveState>(ImageSaveState.Idle)
    val imageSaveState: StateFlow<ImageSaveState> = _imageSaveState.asStateFlow()

    private var lastFetchedWeather: Weather? = null // Store the last successfully fetched weather

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
            val result = weatherRepository.getCurrentWeather(lat, lon, _isCelsius.value)
            result.onSuccess { weather ->
                lastFetchedWeather = weather // Store for image overlay
                _weatherState.value = WeatherState.Success(weather)
            }.onFailure { e ->
                _weatherState.value = WeatherState.Error(e.message ?: "Failed to fetch weather data.")
            }
        }
    }

    fun toggleTemperatureUnit() {
        _isCelsius.value = !_isCelsius.value
        // If we have last fetched weather, re-calculate temperature for display
        lastFetchedWeather?.let { weather ->
            val convertedTemp = if (_isCelsius.value) {
                (weather.temperature - 32) / 1.8 // F to C
            } else {
                (weather.temperature * 1.8) + 32 // C to F
            }
            _weatherState.value = WeatherState.Success(weather.copy(temperature = convertedTemp))
        }
        // Re-fetch weather if no lastFetchedWeather or if we want updated data
        // For simplicity, we just convert the last fetched. A real app might re-fetch.
    }

    fun saveImageWithWeather(originalBitmap: Bitmap) {
        val currentWeather = (weatherState.value as? WeatherState.Success)?.weather
        if (currentWeather == null) {
            _imageSaveState.value = ImageSaveState.Error("No weather data available to save with image.")
            return
        }

        _imageSaveState.value = ImageSaveState.Saving
        viewModelScope.launch {
            imageRepository.saveCapturedImageWithWeather(originalBitmap, currentWeather, _isCelsius.value)
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
        object Idle : WeatherState() // Added Idle state
        object Loading : WeatherState()
        data class Success(val weather: Weather) : WeatherState()
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

