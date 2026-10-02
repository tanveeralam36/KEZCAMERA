package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_records")
data class VideoRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val aspectRatio: String, // "9:16", "16:9", "DUAL_VERTICAL", "DUAL_HORIZONTAL"
    val resolution: String, // "1080p", "720p", "4K"
    val durationMs: Long,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val linkedPairId: Long? = null, // Links the paired video if recorded in dual mode
    val isDualMode: Boolean = false,
    val notes: String = ""
)
