package com.example.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object UrlUtils {
    private val URL_PATTERN = Pattern.compile(
        "https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_+.~#?&/=]*)"
    )

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun extractUrlFromText(text: String): String? {
        val matcher = URL_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group(0)
        } else {
            null
        }
    }

    suspend fun resolveFinalUrl(rawUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(rawUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .head()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.request.url.toString()
            }
        } catch (_: Exception) {
            rawUrl
        }
    }

    fun unescapeHtmlAndJson(input: String): String {
        return input
            .replace("\\/", "/")
            .replace("\\u0025", "%")
            .replace("\\u0026", "&")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(60).trim().ifEmpty { "download" }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            else -> String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }
}
