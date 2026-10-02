package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val fontSizeSp: Float = 22f,
    val scrollSpeedPxPerSec: Float = 45f,
    val textColorHex: String = "#FFFFFF",
    val bgOpacityPercent: Int = 75,
    val textAlignment: String = "CENTER", // LEFT, CENTER, RIGHT
    val isMirrored: Boolean = false,
    val isFavorite: Boolean = false
)
