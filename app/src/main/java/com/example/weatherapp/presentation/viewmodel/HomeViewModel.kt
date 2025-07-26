package com.example.weatherapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.local.model.CapturedImageEntity
import com.example.weatherapp.data.repository.ImageRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(private val imageRepository: ImageRepository) : ViewModel() {

    private val _capturedImages = MutableStateFlow<List<CapturedImageEntity>>(emptyList())
    val capturedImages: StateFlow<List<CapturedImageEntity>> = _capturedImages.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCapturedImages()
    }

    private fun loadCapturedImages() {
        viewModelScope.launch {
            imageRepository.getAllCapturedImages()
                .collectLatest { images ->
                    _capturedImages.value = images
                }
        }
    }

    fun deleteImage(imageId: Long) {
        viewModelScope.launch {
            imageRepository.deleteCapturedImage(imageId)
                .onFailure { e ->
                    _errorMessage.value = "Failed to delete image: ${e.message}"
                }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}