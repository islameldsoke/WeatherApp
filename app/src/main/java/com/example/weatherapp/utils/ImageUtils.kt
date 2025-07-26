package com.example.weatherapp.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object ImageUtils {


    suspend fun overlayWeatherInfo(
        originalBitmap: Bitmap,
        weatherText: String,
        @DrawableRes iconResId: Int,
        context: Context
    ): Bitmap = withContext(Dispatchers.Default) {
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // Paint for text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f // Adjust text size as needed
            isAntiAlias = true
            setShadowLayer(5f, 2f, 2f, Color.BLACK) // Add shadow for readability
        }

        // Calculate text bounds to position it
        val textBounds = Rect()
        textPaint.getTextBounds(weatherText, 0, weatherText.length, textBounds)

        // Load and scale icon
        val imageLoader = ImageLoader(context)
        val iconBitmap: Bitmap? = try {
            val request = ImageRequest.Builder(context)
                .data(iconResId)
                .size(Size(100.dp.value.toInt(), 100.dp.value.toInt())) // Adjust icon size
                .allowHardware(false) // Required for bitmap manipulation
                .build()
            (imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null // Handle error loading icon
        }

        // Position calculations (example: top-left corner with padding)
        val padding = 30f
        val iconLeft = padding
        val iconTop = padding
        val textLeft = iconLeft + (iconBitmap?.width ?: 0) + padding
        val textTop = iconTop + (iconBitmap?.height ?: 0) / 2 + textBounds.height() / 2 // Vertically center text with icon

        // Draw icon
        iconBitmap?.let {
            canvas.drawBitmap(it, iconLeft, iconTop, null)
        }

        // Draw text
        canvas.drawText(weatherText, textLeft, textTop, textPaint)

        mutableBitmap
    }

    /**
     * Saves a bitmap to the device's MediaStore (Pictures directory).
     * @param context The application context.
     * @param bitmap The bitmap to save.
     * @param fileName The desired file name (e.g., "MyImage.jpg").
     * @param mimeType The MIME type of the image (e.g., "image/jpeg").
     * @param quality The compression quality (0-100).
     * @return The URI of the saved image, or null if saving failed.
     */
    suspend fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        mimeType: String,
        quality: Int
    ): String? = withContext(Dispatchers.IO) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/WeatherForecastApp")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        var uri: String? = null
        var outputStream: OutputStream? = null

        try {
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val imageUri = resolver.insert(collection, contentValues) ?: return@withContext null
            uri = imageUri.toString()

            outputStream = resolver.openOutputStream(imageUri)
            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) // Use JPEG for common image saving
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            // Clean up if something goes wrong
            if (uri != null) {
                resolver.delete(android.net.Uri.parse(uri), null, null)
            }
            null
        } finally {
            outputStream?.close()
        }
    }
}