package com.example.model

data class ParsedMedia(
    val originalUrl: String,
    val title: String,
    val platform: PlatformType,
    val thumbnailUrl: String?,
    val author: String? = null,
    val durationSeconds: Long = 0L,
    val availableFormats: List<MediaFormat>
)
