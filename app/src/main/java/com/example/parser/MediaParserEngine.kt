package com.example.parser

import com.example.model.ParsedMedia
import com.example.model.PlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaParserEngine {

    suspend fun parse(rawInput: String): Result<ParsedMedia> = withContext(Dispatchers.IO) {
        try {
            val extractedUrl = UrlUtils.extractUrlFromText(rawInput)
                ?: return@withContext Result.failure(IllegalArgumentException("No valid URL found in shared content"))

            val platform = PlatformType.fromUrl(extractedUrl)

            val parsedMedia = when (platform) {
                PlatformType.PINTEREST -> PinterestParser.parse(extractedUrl)
                PlatformType.FACEBOOK -> FacebookParser.parse(extractedUrl)
                PlatformType.YOUTUBE -> YouTubeParser.parse(extractedUrl)
                PlatformType.WEB -> GenericWebParser.parse(extractedUrl)
            }

            Result.success(parsedMedia)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
