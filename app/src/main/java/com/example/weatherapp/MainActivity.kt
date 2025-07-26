package com.example.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.weatherapp.data.local.database.AppDatabase
import com.example.weatherapp.data.remote.api.WeatherApiService
import com.example.weatherapp.data.repository.ImageRepository
import com.example.weatherapp.data.repository.WeatherRepository
import com.example.weatherapp.presentation.ui.CameraScreen
import com.example.weatherapp.presentation.ui.HomeScreen
import com.example.weatherapp.presentation.ui.theme.WeatherForecastAppTheme
import com.example.weatherapp.presentation.viewmodel.CameraViewModel
import com.example.weatherapp.presentation.viewmodel.HomeViewModel
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.LocationUtils

import com.google.android.gms.location.LocationServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permissions granted, proceed with app
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
            // Recompose the UI to reflect permission status
            setContent {
                WeatherForecastApp()
            }
        } else {
            Toast.makeText(this, "Permissions not granted. Some features may not work.", Toast.LENGTH_LONG).show()
            finish() // Or disable features
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions when the activity is created
        requestPermissions()

        setContent {
            WeatherForecastApp()
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // For Android 9 and below
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // For Android 12 and below
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        if (!hasPermissions(permissionsToRequest)) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun WeatherForecastApp() {
    WeatherForecastAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val context = LocalContext.current
            val navController = rememberNavController()

            // --- Manual Dependency Injection ---
            // Room Database
            val database = remember { AppDatabase.getDatabase(context) }
            val capturedImageDao = remember { database.capturedImageDao() }

            // Retrofit for Weather API
            val retrofit = remember {
                Retrofit.Builder()
                    .baseUrl(Constants.WEATHER_API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            val weatherApiService = remember { retrofit.create(WeatherApiService::class.java) }

            // Location Client
            val fusedLocationClient = remember {
                LocationServices.getFusedLocationProviderClient(context)
            }

            // Repositories
            val weatherRepository = remember { WeatherRepository(weatherApiService) }
            val imageRepository = remember { ImageRepository(context, capturedImageDao) }
            val locationUtils = remember { LocationUtils(fusedLocationClient) }

            // ViewModels (initialized with dependencies)
            val homeViewModel = remember { HomeViewModel(imageRepository) }
            val cameraViewModel = remember {
                CameraViewModel(
                    weatherRepository,
                    imageRepository,
                    locationUtils
                )
            }

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        homeViewModel = homeViewModel,
                        onNavigateToCamera = { navController.navigate("camera") }
                    )
                }
                composable("camera") {
                    CameraScreen(
                        cameraViewModel = cameraViewModel,
                        onImageSaved = { navController.popBackStack() } // Go back to home after saving
                    )
                }
            }
        }
    }
}