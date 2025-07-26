package com.example.weatherapp.presentation.ui


import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.weatherapp.data.local.model.CapturedImageEntity
import com.example.weatherapp.presentation.viewmodel.HomeViewModel
import com.example.weatherapp.utils.Constants
import com.example.weatherforecastapp.R

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToCamera: () -> Unit
) {
    val capturedImages by homeViewModel.capturedImages.collectAsState()
    val errorMessage by homeViewModel.errorMessage.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCamera) {
                Icon(painterResource(R.drawable.camera), "Capture New Weather Photo",modifier = Modifier.size(40.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Weather Photo History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (capturedImages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No captured images yet. Tap the camera button to add one!")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedImages, key = { it.id }) { image ->
                        ImageHistoryCard(
                            image = image,
                            onDeleteClick = { homeViewModel.deleteImage(image.id) }
                        )
                    }
                }
            }

            errorMessage?.let { message ->
                ErrorDialog(message = message, onDismiss = { homeViewModel.clearErrorMessage() })
            }
        }
    }
}

@Composable
fun ImageHistoryCard(image: CapturedImageEntity, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Implement view full image functionality */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Thumbnail
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current).data(Uri.parse(image.imagePath))
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Captured Weather Image",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.size(12.dp))

            // Weather Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = image.cityName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = rememberAsyncImagePainter(Constants.getWeatherIconResId(image.weatherIconCode)),
                        contentDescription = image.weatherDescription,
                        tint = Color.Unspecified, // For drawable resources
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "${String.format("%.1f", image.temperature)}°C", // Assuming Celsius for display here
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Text(
                    text = image.weatherDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        image.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete Button
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, "Delete Image", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
