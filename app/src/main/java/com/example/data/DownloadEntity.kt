package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val downloadManagerId: Long = -1L,
    val title: String,
    val sourceUrl: String,
    val downloadUrl: String,
    val platform: String, // PINTEREST, YOUTUBE, FACEBOOK, WEB
    val mediaType: String, // VIDEO, AUDIO
    val formatName: String, // MP4, MP3
    val qualityLabel: String, // 1080p, 720p, etc.
    val thumbnailUrl: String? = null,
    val localFilePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val status: String = "DOWNLOADING", // DOWNLOADING, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis()
)
