package com.example.weatherapp.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.weatherapp.data.local.model.CapturedImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturedImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapturedImage(image: CapturedImageEntity): Long

    @Query("SELECT * FROM captured_images ORDER BY timestamp DESC")
    fun getAllCapturedImages(): Flow<List<CapturedImageEntity>>

    @Query("DELETE FROM captured_images WHERE id = :imageId")
    suspend fun deleteCapturedImage(imageId: Long)
}