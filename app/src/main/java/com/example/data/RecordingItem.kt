package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val effectType: String,
    val durationMs: Int,
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isImported: Boolean = false
)
