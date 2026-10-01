package com.example.parser

import com.example.model.MediaFormat
import com.example.model.MediaType
import com.example.model.ParsedMedia
import com.example.model.PlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object GenericWebParser {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun parse(url: String): ParsedMedia = withContext(Dispatchers.IO) {
        val lower = url.lowercase()

        // Check if direct media file
        if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov")) {
            val fileName = url.substringAfterLast("/").substringBefore("?")
            return@withContext ParsedMedia(
                originalUrl = url,
                title = UrlUtils.sanitizeFilename(fileName),
                platform = PlatformType.WEB,
                thumbnailUrl = null,
                availableFormats = listOf(
                    MediaFormat(
                        id = "direct_mp4",
                        mediaType = MediaType.VIDEO,
                        formatName = "MP4",
                        qualityLabel = "Original Video File",
                        downloadUrl = url,
                        isDirectStream = true
                    ),
                    MediaFormat(
                        id = "direct_audio",
                        mediaType = MediaType.AUDIO,
                        formatName = "MP3",
                        qualityLabel = "Audio Only",
                        downloadUrl = url,
                        isDirectStream = true
                    )
                )
            )
        }

        if (lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav")) {
            val fileName = url.substringAfterLast("/").substringBefore("?")
            return@withContext ParsedMedia(
                originalUrl = url,
                title = UrlUtils.sanitizeFilename(fileName),
                platform = PlatformType.WEB,
                thumbnailUrl = null,
                availableFormats = listOf(
                    MediaFormat(
                        id = "direct_audio",
                        mediaType = MediaType.AUDIO,
                        formatName = "MP3",
                        qualityLabel = "Audio File",
                        downloadUrl = url,
                        isDirectStream = true
                    )
                )
            )
        }

        // Otherwise fetch webpage HTML
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .build()

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: ""

        // Title
        var title = "Web Media"
        val titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(html)
        if (titleMatcher.find()) {
            val t = titleMatcher.group(1)?.trim()
            if (!t.isNullOrEmpty()) {
                title = UrlUtils.unescapeHtmlAndJson(t)
            }
        }

        // Thumbnail
        var thumbnail: String? = null
        val ogImageMatcher = Pattern.compile("<meta property=\"og:image\" content=\"(.*?)\"", Pattern.CASE_INSENSITIVE).matcher(html)
        if (ogImageMatcher.find()) {
            thumbnail = ogImageMatcher.group(1)
        }

        // Direct video link extraction
        val formats = mutableListOf<MediaFormat>()
        var foundVideoUrl: String? = null

        val ogVideoMatcher = Pattern.compile("<meta property=\"og:video(?::secure_url)?\" content=\"(.*?)\"", Pattern.CASE_INSENSITIVE).matcher(html)
        if (ogVideoMatcher.find()) {
            foundVideoUrl = UrlUtils.unescapeHtmlAndJson(ogVideoMatcher.group(1) ?: "")
        }

        if (foundVideoUrl == null) {
            val videoTagMatcher = Pattern.compile("<video[^>]*src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
            if (videoTagMatcher.find()) {
                foundVideoUrl = UrlUtils.unescapeHtmlAndJson(videoTagMatcher.group(1) ?: "")
            }
        }

        val mediaUrl = foundVideoUrl ?: url
        formats.add(
            MediaFormat(
                id = "web_video_hd",
                mediaType = MediaType.VIDEO,
                formatName = "MP4",
                qualityLabel = "High Quality Video",
                downloadUrl = mediaUrl,
                isDirectStream = true
            )
        )
        formats.add(
            MediaFormat(
                id = "web_video_sd",
                mediaType = MediaType.VIDEO,
                formatName = "MP4",
                qualityLabel = "Standard Quality",
                downloadUrl = mediaUrl,
                isDirectStream = true
            )
        )
        formats.add(
            MediaFormat(
                id = "web_audio",
                mediaType = MediaType.AUDIO,
                formatName = "MP3",
                qualityLabel = "Audio Only (MP3)",
                downloadUrl = mediaUrl,
                isDirectStream = true
            )
        )

        ParsedMedia(
            originalUrl = url,
            title = title,
            platform = PlatformType.WEB,
            thumbnailUrl = thumbnail,
            availableFormats = formats
        )
    }
}
