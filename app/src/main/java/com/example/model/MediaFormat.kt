package com.example.model

enum class MediaType {
    VIDEO,
    AUDIO
}

data class MediaFormat(
    val id: String,
    val mediaType: MediaType,
    val formatName: String, // e.g. "MP4", "MP3"
    val qualityLabel: String, // e.g. "1080p Full HD", "720p HD", "480p", "320 kbps High", "128 kbps"
    val downloadUrl: String,
    val estimatedSizeBytes: Long = 0L,
    val isDirectStream: Boolean = true,
    val fileExtension: String = if (mediaType == MediaType.VIDEO) "mp4" else "mp3"
)
