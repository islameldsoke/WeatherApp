package com.example.weatherapp.presentation.ui


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.weatherapp.presentation.viewmodel.CameraViewModel
import com.example.weatherapp.utils.Constants
import com.example.weatherforecastapp.R
import java.util.Locale
import java.util.concurrent.Executor

@Composable
fun CameraScreen(
    cameraViewModel: CameraViewModel,
    onImageSaved: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }
    val weatherState by cameraViewModel.weatherState.collectAsStateWithLifecycle()
    val isCelsius by cameraViewModel.isCelsius.collectAsStateWithLifecycle()
    val imageSaveState by cameraViewModel.imageSaveState.collectAsStateWithLifecycle()

    var capturedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSaveConfirmation by remember { mutableStateOf(false) }

    // Fetch weather data when the screen is first composed
    LaunchedEffect(Unit) {
        cameraViewModel.fetchWeather(context)
    }

    // Observe image save state
    LaunchedEffect(imageSaveState) {
        when (imageSaveState) {
            CameraViewModel.ImageSaveState.Saved -> {
                onImageSaved() // Navigate back to home
                cameraViewModel.resetImageSaveState()
            }
            is CameraViewModel.ImageSaveState.Error -> {
                // Error dialog will be shown via the state
            }
            else -> { /* Do nothing for Idle or Saving */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
            update = { previewView ->
                // This block can be used for dynamic updates to the PreviewView
                // For now, no dynamic updates are needed here
            }
        )

        // Weather Overlay
        WeatherOverlay(weatherState = weatherState, isCelsius = isCelsius)

        // Camera Controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Refresh Weather Button
                IconButton(
                    onClick = { cameraViewModel.fetchWeather(context) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Filled.Refresh, "Refresh Weather", tint = Color.White)
                }

                // Capture Button
                IconButton(
                    onClick = {
                        takePhoto(
                            context = context,
                            cameraController = cameraController,
                            executor = ContextCompat.getMainExecutor(context),
                            onPhotoCaptured = { bitmap ->
                                capturedImageBitmap = bitmap
                                showSaveConfirmation = true
                            }
                        )
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(painterResource(R.drawable.camera), "Take Photo", tint = Color.White, modifier = Modifier.size(40.dp))
                }

                // Toggle Unit Button
                IconButton(
                    onClick = { cameraViewModel.toggleTemperatureUnit()
                        Toast.makeText(context, if (isCelsius) "°C" else "°F", Toast.LENGTH_SHORT).show()

                              },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Filled.Info, "Toggle Unit", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Loading indicator for image saving
        var isShowingDialog by rememberSaveable { mutableStateOf(false) }

        if (imageSaveState is CameraViewModel.ImageSaveState.Saving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Saving image...", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Error dialog for weather or image saving
        when (val state = weatherState) {
            is CameraViewModel.WeatherState.Error -> {
//                isShowingDialog = true
//                ErrorDialog(message = state.message, onDismiss = {
//                    cameraViewModel.resetImageSaveState()
//                    isShowingDialog = false
//                })
            }

            is CameraViewModel.WeatherState.Idle ->{
                isShowingDialog = false

            }
            else -> {}
        }
        when (val state = imageSaveState) {
            is CameraViewModel.ImageSaveState.Error -> {
                isShowingDialog = true
                ErrorDialog(message = state.message, onDismiss = { cameraViewModel.resetImageSaveState() })
            }
            else -> {}
        }
    }

    // Save Confirmation Dialog (simple example, could be a custom dialog)
    if (showSaveConfirmation && capturedImageBitmap != null) {
        SaveConfirmationDialog(
            onSave = {
                capturedImageBitmap?.let { bitmap ->
                    cameraViewModel.saveImageWithWeather(bitmap)
                    capturedImageBitmap = null // Clear bitmap after initiating save
                    showSaveConfirmation = false
                }
            },
            onDiscard = {
                capturedImageBitmap = null
                showSaveConfirmation = false
            }
        )
    }
}

@Composable
fun WeatherOverlay(weatherState: CameraViewModel.WeatherState, isCelsius: Boolean) {


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.TopStart
    ) {
        when (weatherState) {
            CameraViewModel.WeatherState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Loading weather...", color = Color.White)
                }
            }
            is CameraViewModel.WeatherState.Success -> {
                val weather = weatherState.weather
                val temperatureUnit = if (isCelsius) "°C" else "°F"
                Column {
                    Text(
                        text = weather.cityName,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = rememberAsyncImagePainter(Constants.getWeatherIconResId(weather.iconCode)),
                            contentDescription = weather.description,
                            tint = Color.Unspecified, // For drawable resources
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", weather.temperature)}$temperatureUnit",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        text = weather.description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()) else it.toString() },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Humidity: ${weather.humidity}%, Wind: ${String.format("%.1f", weather.windSpeed)} m/s",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is CameraViewModel.WeatherState.Error -> {
                Text("Error: ${weatherState.message}", color = MaterialTheme.colorScheme.error)
            }

            CameraViewModel.WeatherState.Idle -> Text("Tap location icon to fetch weather.", color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun SaveConfirmationDialog(onSave: () -> Unit, onDiscard: () -> Unit) {
    // This is a very basic "dialog" using a Box. For a proper dialog,
    // you'd use AlertDialog or a custom Composable that handles dismissal.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Save this image with weather data?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = onDiscard) {
                    Text("Discard")
                }
                Button(onClick = onSave) {
                    Text("Save")
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    cameraController: LifecycleCameraController,
    executor: Executor,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    cameraController.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                val rotationDegrees = image.imageInfo.rotationDegrees
                val bitmap = image.toBitmap()

                // Rotate the bitmap if necessary
                val rotatedBitmap = if (rotationDegrees != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }

                onPhotoCaptured(rotatedBitmap)
                image.close() // Important: close the image proxy
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Error capturing image: ${exception.message}", exception)
                // Handle error, e.g., show a Toast
                Toast.makeText(context, "Error capturing photo: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
