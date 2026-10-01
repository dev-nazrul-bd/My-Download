package com.example.parser

import com.example.model.MediaFormat
import com.example.model.MediaType
import com.example.model.ParsedMedia
import com.example.model.PlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object YouTubeParser {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val VIDEO_ID_PATTERNS = listOf(
        Pattern.compile("youtu\\.be/([a-zA-Z0-9_-]{11})"),
        Pattern.compile("[?&]v=([a-zA-Z0-9_-]{11})"),
        Pattern.compile("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
        Pattern.compile("youtube\\.com/embed/([a-zA-Z0-9_-]{11})")
    )

    fun extractVideoId(url: String): String? {
        for (pattern in VIDEO_ID_PATTERNS) {
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    suspend fun parse(url: String): ParsedMedia = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(url)
            ?: throw IllegalArgumentException("সঠিক YouTube লিংক পাওয়া যায়নি")

        var title = "YouTube Video ($videoId)"
        var author: String? = null
        val thumbnail = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

        val formatsList = mutableListOf<MediaFormat>()
        val seenVideoResolutions = mutableSetOf<String>()

        // 1. Fetch metadata & streams using YouTube Innertube API (ANDROID_VR client)
        try {
            val payload = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID_VR")
                        put("clientVersion", "1.61.48")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; Quest 2) AppleWebKit/537.36 (KHTML, like Gecko) OculusBrowser/15.0.0.0.22.280371431 SamsungBrowser/4.0 Chrome/89.0.4389.90 Mobile VR Safari/537.36"
                )
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        val json = JSONObject(bodyString)
                        val videoDetails = json.optJSONObject("videoDetails")
                        if (videoDetails != null) {
                            title = videoDetails.optString("title", title)
                            if (videoDetails.has("author")) {
                                author = videoDetails.getString("author")
                            }
                        }

                        val streamingData = json.optJSONObject("streamingData")
                        if (streamingData != null) {
                            // Muxed formats (video + audio in single stream)
                            val muxedFormats = streamingData.optJSONArray("formats") ?: JSONArray()
                            for (i in 0 until muxedFormats.length()) {
                                val f = muxedFormats.getJSONObject(i)
                                val streamUrl = f.optString("url")
                                var quality = f.optString("qualityLabel")
                                if (quality.isEmpty()) {
                                    val height = f.optInt("height", 0)
                                    quality = if (height > 0) "${height}p" else "360p"
                                }
                                val contentLength = f.optLong("contentLength", 0L)

                                if (streamUrl.startsWith("http") && quality !in seenVideoResolutions) {
                                    seenVideoResolutions.add(quality)
                                    formatsList.add(
                                        MediaFormat(
                                            id = "yt_muxed_${quality}_$i",
                                            mediaType = MediaType.VIDEO,
                                            formatName = "MP4",
                                            qualityLabel = "$quality (ভিডিও ও অডিও)",
                                            downloadUrl = streamUrl,
                                            estimatedSizeBytes = contentLength,
                                            isDirectStream = true,
                                            fileExtension = "mp4"
                                        )
                                    )
                                }
                            }

                            // Adaptive formats (HD Video & Audio)
                            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()
                            for (i in 0 until adaptiveFormats.length()) {
                                val f = adaptiveFormats.getJSONObject(i)
                                val streamUrl = f.optString("url")
                                val mime = f.optString("mimeType", "")
                                val contentLength = f.optLong("contentLength", 0L)
                                val itag = f.optInt("itag", 0)
                                var quality = f.optString("qualityLabel")
                                if (quality.isEmpty()) {
                                    val height = f.optInt("height", 0)
                                    if (height > 0) quality = "${height}p"
                                }

                                if (streamUrl.startsWith("http")) {
                                    // Video stream (prefer mp4)
                                    if ((mime.contains("video/mp4") || mime.contains("video/")) && quality.isNotEmpty() && quality !in seenVideoResolutions) {
                                        seenVideoResolutions.add(quality)
                                        formatsList.add(
                                            MediaFormat(
                                                id = "yt_adapt_${quality}_$i",
                                                mediaType = MediaType.VIDEO,
                                                formatName = "MP4",
                                                qualityLabel = "$quality (HD Video)",
                                                downloadUrl = streamUrl,
                                                estimatedSizeBytes = contentLength,
                                                isDirectStream = true,
                                                fileExtension = "mp4"
                                            )
                                        )
                                    }
                                    // Audio stream (itag 140 is AAC/M4A audio)
                                    if ((mime.contains("audio/mp4") || itag == 140 || mime.contains("audio/")) && formatsList.none { it.mediaType == MediaType.AUDIO }) {
                                        formatsList.add(
                                            MediaFormat(
                                                id = "yt_audio_hq",
                                                mediaType = MediaType.AUDIO,
                                                formatName = "MP3",
                                                qualityLabel = "High Quality Audio (M4A/MP3)",
                                                downloadUrl = streamUrl,
                                                estimatedSizeBytes = contentLength,
                                                isDirectStream = true,
                                                fileExtension = "m4a"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Fallback: If no direct video format was found (e.g. music videos with LOGIN_REQUIRED), query Invidious
        if (formatsList.none { it.mediaType == MediaType.VIDEO }) {
            try {
                val mirrorUrl = "https://invidious.f5.si/api/v1/videos/$videoId"
                val req = Request.Builder().url(mirrorUrl).header("User-Agent", "Mozilla/5.0").build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val data = JSONObject(resp.body?.string() ?: "")
                        title = data.optString("title", title)
                        if (data.has("author")) {
                            author = data.getString("author")
                        }

                        // Muxed format streams
                        val formatStreams = data.optJSONArray("formatStreams") ?: JSONArray()
                        for (i in 0 until formatStreams.length()) {
                            val s = formatStreams.getJSONObject(i)
                            val sUrl = s.optString("url")
                            val q = s.optString("qualityLabel", "360p")
                            val size = s.optLong("size", 0L)
                            if (sUrl.startsWith("http") && q !in seenVideoResolutions) {
                                seenVideoResolutions.add(q)
                                formatsList.add(
                                    MediaFormat(
                                        id = "inv_vid_$q",
                                        mediaType = MediaType.VIDEO,
                                        formatName = "MP4",
                                        qualityLabel = "$q (ভিডিও ও অডিও)",
                                        downloadUrl = sUrl,
                                        estimatedSizeBytes = size,
                                        isDirectStream = true,
                                        fileExtension = "mp4"
                                    )
                                )
                            }
                        }

                        // Adaptive format streams (1080p, 720p, 480p, 360p, 240p, audio)
                        val adaptive = data.optJSONArray("adaptiveFormats") ?: JSONArray()
                        for (i in 0 until adaptive.length()) {
                            val a = adaptive.getJSONObject(i)
                            val aType = a.optString("type", "")
                            val aUrl = a.optString("url", "")
                            var res = a.optString("resolution")
                            if (res.isEmpty()) {
                                res = a.optString("qualityLabel")
                            }
                            val clen = a.optLong("clen", a.optLong("contentLength", 0L))

                            if (aUrl.startsWith("http")) {
                                // Extract MP4 video formats
                                if (aType.contains("video/mp4") && res.isNotEmpty() && res !in seenVideoResolutions) {
                                    seenVideoResolutions.add(res)
                                    formatsList.add(
                                        MediaFormat(
                                            id = "inv_adapt_$res",
                                            mediaType = MediaType.VIDEO,
                                            formatName = "MP4",
                                            qualityLabel = "$res (HD Video)",
                                            downloadUrl = aUrl,
                                            estimatedSizeBytes = clen,
                                            isDirectStream = true,
                                            fileExtension = "mp4"
                                        )
                                    )
                                }

                                // Extract audio format
                                if ((aType.contains("audio/mp4") || aType.contains("audio")) && formatsList.none { it.mediaType == MediaType.AUDIO }) {
                                    formatsList.add(
                                        MediaFormat(
                                            id = "inv_audio_hq",
                                            mediaType = MediaType.AUDIO,
                                            formatName = "MP3",
                                            qualityLabel = "High Quality Audio (M4A/MP3)",
                                            downloadUrl = aUrl,
                                            estimatedSizeBytes = clen,
                                            isDirectStream = true,
                                            fileExtension = "m4a"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Fallback: If still no video formats found, add secondary mirror resolutions
        if (formatsList.none { it.mediaType == MediaType.VIDEO }) {
            // As a last-resort direct fallback, extract webm videos from Invidious if available
            try {
                val mirrorUrl = "https://invidious.f5.si/api/v1/videos/$videoId"
                val req = Request.Builder().url(mirrorUrl).header("User-Agent", "Mozilla/5.0").build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val data = JSONObject(resp.body?.string() ?: "")
                        val adaptive = data.optJSONArray("adaptiveFormats") ?: JSONArray()
                        for (i in 0 until adaptive.length()) {
                            val a = adaptive.getJSONObject(i)
                            val aType = a.optString("type", "")
                            val aUrl = a.optString("url", "")
                            val res = a.optString("resolution")
                            if (aUrl.startsWith("http") && aType.contains("video") && res.isNotEmpty() && res !in seenVideoResolutions) {
                                seenVideoResolutions.add(res)
                                formatsList.add(
                                    MediaFormat(
                                        id = "inv_generic_$res",
                                        mediaType = MediaType.VIDEO,
                                        formatName = "MP4",
                                        qualityLabel = "$res (HD Video)",
                                        downloadUrl = aUrl,
                                        isDirectStream = true,
                                        fileExtension = "mp4"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (formatsList.isEmpty()) {
            throw IllegalStateException("ইউটিউব থেকে মিডিয়া স্ট্রিম পাওয়া যায়নি। লিংকটি পুনরায় চেক করুন।")
        }

        // Sort: Video formats first (highest resolution down to lowest), then Audio
        val resolutionRank = mapOf(
            "2160p" to 1, "1440p" to 2, "1080p" to 3, "720p" to 4,
            "480p" to 5, "360p" to 6, "240p" to 7, "144p" to 8
        )

        val sortedList = formatsList.sortedWith(
            compareBy<MediaFormat> { it.mediaType != MediaType.VIDEO }
                .thenBy { format ->
                    val match = resolutionRank.keys.firstOrNull { format.qualityLabel.contains(it) }
                    match?.let { resolutionRank[it] } ?: 10
                }
        )

        ParsedMedia(
            originalUrl = "https://www.youtube.com/watch?v=$videoId",
            title = title,
            platform = PlatformType.YOUTUBE,
            thumbnailUrl = thumbnail,
            author = author,
            availableFormats = sortedList
        )
    }
}
