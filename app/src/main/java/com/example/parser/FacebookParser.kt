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

object FacebookParser {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun parse(url: String): ParsedMedia = withContext(Dispatchers.IO) {
        var cleanUrl = url
        if (cleanUrl.contains("fb.watch") || cleanUrl.contains("facebook.com/share")) {
            cleanUrl = UrlUtils.resolveFinalUrl(cleanUrl)
        }

        val request = Request.Builder()
            .url(cleanUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw IllegalStateException("Could not load Facebook page")

        // Title
        var title = "Facebook Video"
        val titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(html)
        if (titleMatcher.find()) {
            val rawTitle = titleMatcher.group(1)?.replace("| Facebook", "")?.trim()
            if (!rawTitle.isNullOrEmpty() && rawTitle != "Facebook") {
                title = UrlUtils.unescapeHtmlAndJson(rawTitle)
            }
        }
        val ogTitleMatcher = Pattern.compile("<meta property=\"og:title\" content=\"(.*?)\"", Pattern.CASE_INSENSITIVE).matcher(html)
        if (ogTitleMatcher.find()) {
            val raw = ogTitleMatcher.group(1)?.trim()
            if (!raw.isNullOrEmpty()) {
                title = UrlUtils.unescapeHtmlAndJson(raw)
            }
        }

        // Thumbnail
        var thumbnail: String? = null
        val ogImageMatcher = Pattern.compile("<meta property=\"og:image\" content=\"(.*?)\"", Pattern.CASE_INSENSITIVE).matcher(html)
        if (ogImageMatcher.find()) {
            thumbnail = UrlUtils.unescapeHtmlAndJson(ogImageMatcher.group(1) ?: "")
        }

        // Extract HD and SD video URLs
        var hdUrl: String? = null
        var sdUrl: String? = null

        // 1. playable_url_quality_hd
        val hdPattern = Pattern.compile("playable_url_quality_hd[\":\\s]+([^\",;]+)")
        val hdMatcher = hdPattern.matcher(html)
        if (hdMatcher.find()) {
            val candidate = UrlUtils.unescapeHtmlAndJson(hdMatcher.group(1)?.replace("\"", "") ?: "")
            if (candidate.startsWith("http")) hdUrl = candidate
        }

        // 2. playable_url
        val sdPattern = Pattern.compile("playable_url[\":\\s]+([^\",;]+)")
        val sdMatcher = sdPattern.matcher(html)
        if (sdMatcher.find()) {
            val candidate = UrlUtils.unescapeHtmlAndJson(sdMatcher.group(1)?.replace("\"", "") ?: "")
            if (candidate.startsWith("http")) sdUrl = candidate
        }

        // 3. hd_src / sd_src
        if (hdUrl == null) {
            val hdSrcPattern = Pattern.compile("hd_src[\":\\s]+([^\",;]+)")
            val m = hdSrcPattern.matcher(html)
            if (m.find()) {
                val candidate = UrlUtils.unescapeHtmlAndJson(m.group(1)?.replace("\"", "") ?: "")
                if (candidate.startsWith("http")) hdUrl = candidate
            }
        }
        if (sdUrl == null) {
            val sdSrcPattern = Pattern.compile("sd_src[\":\\s]+([^\",;]+)")
            val m = sdSrcPattern.matcher(html)
            if (m.find()) {
                val candidate = UrlUtils.unescapeHtmlAndJson(m.group(1)?.replace("\"", "") ?: "")
                if (candidate.startsWith("http")) sdUrl = candidate
            }
        }

        // 4. og:video
        if (sdUrl == null && hdUrl == null) {
            val ogVideoMatcher = Pattern.compile("<meta property=\"og:video(?::secure_url)?\" content=\"(.*?)\"", Pattern.CASE_INSENSITIVE).matcher(html)
            if (ogVideoMatcher.find()) {
                val candidate = UrlUtils.unescapeHtmlAndJson(ogVideoMatcher.group(1) ?: "")
                if (candidate.startsWith("http") && (candidate.contains(".mp4") || candidate.contains("fbcdn.net"))) {
                    sdUrl = candidate
                }
            }
        }

        val formats = mutableListOf<MediaFormat>()
        if (hdUrl != null) {
            formats.add(
                MediaFormat(
                    id = "fb_hd",
                    mediaType = MediaType.VIDEO,
                    formatName = "MP4",
                    qualityLabel = "HD 720p / 1080p",
                    downloadUrl = hdUrl,
                    isDirectStream = true
                )
            )
        }
        if (sdUrl != null && sdUrl != hdUrl) {
            formats.add(
                MediaFormat(
                    id = "fb_sd",
                    mediaType = MediaType.VIDEO,
                    formatName = "MP4",
                    qualityLabel = "SD 480p",
                    downloadUrl = sdUrl,
                    isDirectStream = true
                )
            )
        }

        val audioSource = hdUrl ?: sdUrl
        if (audioSource != null) {
            formats.add(
                MediaFormat(
                    id = "fb_audio",
                    mediaType = MediaType.AUDIO,
                    formatName = "MP3",
                    qualityLabel = "Audio Only (MP3)",
                    downloadUrl = audioSource,
                    isDirectStream = true,
                    fileExtension = "mp3"
                )
            )
        }

        // Strict verification: Do NOT fallback to cleanUrl (HTML webpage)
        if (formats.isEmpty()) {
            throw IllegalStateException("ফেসবুক থেকে সরাসরি ভিডিও স্ট্রিম পাওয়া যায়নি। ভিডিওটি প্রাইভেট হতে পারে অথবা ফেসবুক লগইন প্রয়োজন।")
        }

        ParsedMedia(
            originalUrl = url,
            title = title,
            platform = PlatformType.FACEBOOK,
            thumbnailUrl = thumbnail,
            availableFormats = formats
        )
    }
}
