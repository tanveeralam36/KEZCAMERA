package com.example.data.db

import androidx.room.*
import com.example.data.model.VideoRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoRecordDao {
    @Query("SELECT * FROM video_records ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<VideoRecord>>

    @Query("SELECT * FROM video_records WHERE id = :id")
    suspend fun getVideoById(id: Long): VideoRecord?

    @Query("SELECT * FROM video_records WHERE linkedPairId = :pairId")
    suspend fun getPairedVideos(pairId: Long): List<VideoRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoRecord): Long

    @Update
    suspend fun updateVideo(video: VideoRecord)

    @Delete
    suspend fun deleteVideo(video: VideoRecord)

    @Query("DELETE FROM video_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
