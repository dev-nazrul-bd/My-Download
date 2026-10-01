package com.example.model

enum class PlatformType(val displayName: String, val badgeColorHex: Long) {
    PINTEREST("Pinterest", 0xFFE60023),
    YOUTUBE("YouTube", 0xFFFF0000),
    FACEBOOK("Facebook", 0xFF1877F2),
    WEB("Web Video", 0xFF6366F1);

    companion object {
        fun fromUrl(url: String): PlatformType {
            val lower = url.lowercase()
            return when {
                lower.contains("pinterest") || lower.contains("pin.it") -> PINTEREST
                lower.contains("youtube") || lower.contains("youtu.be") -> YOUTUBE
                lower.contains("facebook") || lower.contains("fb.watch") || lower.contains("fb.com") -> FACEBOOK
                else -> WEB
            }
        }
    }
}
