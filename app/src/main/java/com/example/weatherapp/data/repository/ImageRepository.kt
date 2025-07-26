package com.example.weatherapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.weatherapp.data.local.database.CapturedImageDao
import com.example.weatherapp.data.local.model.CapturedImageEntity
import com.example.weatherapp.domain.model.Weather
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.ImageUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageRepository(
    private val context: Context,
    private val capturedImageDao: CapturedImageDao
) {

    /**
     * Saves a captured image with overlaid weather data to MediaStore and its metadata to Room.
     * @param originalBitmap The original bitmap captured from the camera.
     * @param weather The Weather data to overlay and save.
     * @param isCelsius True if temperature is in Celsius, false for Fahrenheit.
     * @return A Result indicating success or failure.
     */
    suspend fun saveCapturedImageWithWeather(
        originalBitmap: Bitmap,
        weather: Weather,
        isCelsius: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Overlay weather info onto the bitmap
            val temperatureUnit = if (isCelsius) "°C" else "°F"
            val weatherText = "${String.format("%.1f", weather.temperature)}$temperatureUnit, ${weather.description}"
            val iconResId = Constants.getWeatherIconResId(weather.iconCode)

            val imageWithOverlay = ImageUtils.overlayWeatherInfo(
                originalBitmap,
                weatherText,
                iconResId,
                context
            )

            // 2. Save the new bitmap to MediaStore
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "${Constants.IMAGE_FILE_NAME_PREFIX}${dateFormat.format(Date(timestamp))}.jpg"

            val imageUri = ImageUtils.saveBitmapToMediaStore(
                context,
                imageWithOverlay,
                fileName,
                Constants.IMAGE_MIME_TYPE,
                Constants.IMAGE_COMPRESSION_QUALITY
            )

            if (imageUri == null) {
                return@withContext Result.failure(Exception("Failed to save image to MediaStore."))
            }

            // 3. Save metadata to Room database
            val entity = CapturedImageEntity(
                imagePath = imageUri,
                temperature = weather.temperature,
                humidity = weather.humidity,
                windSpeed = weather.windSpeed,
                weatherDescription = weather.description,
                weatherIconCode = weather.iconCode,
                cityName = weather.cityName,
                timestamp = timestamp
            )
            capturedImageDao.insertCapturedImage(entity)

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Retrieves all captured images from the local database.
     * @return A Flow emitting a list of CapturedImageEntity objects.
     */
    fun getAllCapturedImages(): Flow<List<CapturedImageEntity>> {
        return capturedImageDao.getAllCapturedImages()
    }

    /**
     * Deletes a captured image entry from the database.
     * Note: This does NOT delete the actual image file from MediaStore.
     * For a complete deletion, you would need to implement deletion from MediaStore separately
     * using the imagePath (URI).
     */
    suspend fun deleteCapturedImage(imageId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            capturedImageDao.deleteCapturedImage(imageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}