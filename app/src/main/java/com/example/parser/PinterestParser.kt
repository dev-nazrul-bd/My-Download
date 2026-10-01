package com.example.parser

import com.example.model.MediaFormat
import com.example.model.MediaType
import com.example.model.ParsedMedia
import com.example.model.PlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object PinterestParser {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun parse(url: String): ParsedMedia = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = client.newCall(request).execute()
        val finalUrl = response.request.url.toString()
        val html = response.body?.string() ?: throw IllegalStateException("Could not load Pinterest page")

        // Title
        var title = "Pinterest Video"
        val titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(html)
        if (titleMatcher.find()) {
            val t = titleMatcher.group(1)?.replace("| Pinterest", "")?.trim()
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

        val foundUrls = linkedSetOf<String>()

        // 1. Check og:video
        val ogVideoMatcher = Pattern.compile("<meta property=\"og:video(?::secure_url)?\" content=\"(.*?)\"", Pattern.CASE_INSENSITIVE).matcher(html)
        while (ogVideoMatcher.find()) {
            val vUrl = UrlUtils.unescapeHtmlAndJson(ogVideoMatcher.group(1) ?: "")
            if (vUrl.startsWith("http") && (vUrl.contains(".mp4") || vUrl.contains("pinimg.com"))) {
                foundUrls.add(vUrl)
            }
        }

        // 2. Check JSON data in script __PWS_DATA__
        val scriptMatcher = Pattern.compile("<script id=\"__PWS_DATA__\"[^>]*>(.*?)</script>", Pattern.DOTALL).matcher(html)
        if (scriptMatcher.find()) {
            try {
                val jsonStr = scriptMatcher.group(1) ?: ""
                val root = JSONObject(jsonStr)
                val pins = root.optJSONObject("props")
                    ?.optJSONObject("initialReduxState")
                    ?.optJSONObject("pins")

                if (pins != null) {
                    val keys = pins.keys()
                    while (keys.hasNext()) {
                        val pinKey = keys.next()
                        val pinObj = pins.optJSONObject(pinKey)
                        val videosObj = pinObj?.optJSONObject("videos")
                        val videoList = videosObj?.optJSONObject("video_list")
                        if (videoList != null) {
                            val vKeys = videoList.keys()
                            while (vKeys.hasNext()) {
                                val vk = vKeys.next()
                                val vItem = videoList.optJSONObject(vk)
                                val vUrl = vItem?.optString("url")
                                if (!vUrl.isNullOrEmpty() && vUrl.startsWith("http") && vUrl.contains(".mp4")) {
                                    foundUrls.add(vUrl)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Regex for v.pinimg.com video files (mc/720p, mc/expMp4, etc.)
        val pinVideoPattern = Pattern.compile("https://v\\.pinimg\\.com/videos/[^\"]+\\.mp4")
        val pinMatcher = pinVideoPattern.matcher(html)
        while (pinMatcher.find()) {
            val group = pinMatcher.group(0)
            if (!group.isNullOrEmpty()) {
                val vUrl = UrlUtils.unescapeHtmlAndJson(group)
                foundUrls.add(vUrl)
            }
        }

        // 4. Regex for other raw mp4 URLs in response
        val genericMp4Pattern = Pattern.compile("\"url\":\"(https?:\\\\/\\\\/[^\"]+\\.mp4)\"")
        val genericMatcher = genericMp4Pattern.matcher(html)
        while (genericMatcher.find()) {
            val group = genericMatcher.group(1)
            if (!group.isNullOrEmpty()) {
                val vUrl = UrlUtils.unescapeHtmlAndJson(group)
                if (!vUrl.contains("youtube") && !vUrl.contains("facebook")) {
                    foundUrls.add(vUrl)
                }
            }
        }

        if (foundUrls.isEmpty()) {
            throw IllegalStateException("পিন্টারেস্ট থেকে সরাসরি ভিডিও লিংক পাওয়া যায়নি (লিংকটি সঠিক ভিডিও পিন নাও হতে পারে)।")
        }

        val formats = mutableListOf<MediaFormat>()
        val sortedList = foundUrls.toList()

        val hdUrl = sortedList.firstOrNull { it.contains("720p") || it.contains("1080p") } ?: sortedList.first()
        formats.add(
            MediaFormat(
                id = "pin_hd",
                mediaType = MediaType.VIDEO,
                formatName = "MP4",
                qualityLabel = "High Quality (HD)",
                downloadUrl = hdUrl,
                isDirectStream = true
            )
        )

        val sdUrl = sortedList.firstOrNull { it != hdUrl }
        if (sdUrl != null) {
            formats.add(
                MediaFormat(
                    id = "pin_sd",
                    mediaType = MediaType.VIDEO,
                    formatName = "MP4",
                    qualityLabel = "Standard Quality (SD)",
                    downloadUrl = sdUrl,
                    isDirectStream = true
                )
            )
        }

        formats.add(
            MediaFormat(
                id = "pin_audio",
                mediaType = MediaType.AUDIO,
                formatName = "MP3",
                qualityLabel = "Audio Only (MP3)",
                downloadUrl = hdUrl,
                isDirectStream = true,
                fileExtension = "mp3"
            )
        )

        ParsedMedia(
            originalUrl = url,
            title = title,
            platform = PlatformType.PINTEREST,
            thumbnailUrl = thumbnail,
            availableFormats = formats
        )
    }
}
