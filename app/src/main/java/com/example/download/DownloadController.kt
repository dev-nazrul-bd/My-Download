package com.example.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.DownloadEntity
import com.example.model.MediaFormat
import com.example.model.MediaType
import com.example.model.ParsedMedia
import com.example.parser.UrlUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DownloadController(private val context: Context) {

    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val repository = AppDatabase.getInstance(appContext).downloadDao()

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val CHANNEL_ID = "my_download_channel"
        private val notificationIdCounter = AtomicInteger(1001)
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress and completion of video and audio downloads"
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun startDownload(
        parsedMedia: ParsedMedia,
        selectedFormat: MediaFormat
    ): Long = withContext(Dispatchers.IO) {
        val downloadUrl = selectedFormat.downloadUrl

        // Reject if URL is a plain webpage
        if (downloadUrl.contains("youtube.com/watch") ||
            downloadUrl.contains("youtu.be/") ||
            downloadUrl.contains("facebook.com/") ||
            downloadUrl.contains("pinterest.com/pin/") ||
            downloadUrl.contains("pin.it/")
        ) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    appContext,
                    "সরাসরি ভিডিও স্ট্রিম পাওয়া যায়নি, ওয়েবপেজ ডাউনলোড করা সম্ভব নয়।",
                    Toast.LENGTH_LONG
                ).show()
            }
            return@withContext -1L
        }

        val extension = selectedFormat.fileExtension
        val cleanTitle = UrlUtils.sanitizeFilename(parsedMedia.title)
        val fileName = "${cleanTitle}_${selectedFormat.formatName}_${selectedFormat.qualityLabel.take(10).replace(" ", "_")}_${System.currentTimeMillis() % 10000}.$extension"

        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "MyDownload"
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val targetFile = File(dir, fileName)

        // Insert placeholder into Room Database
        val entity = DownloadEntity(
            downloadManagerId = System.currentTimeMillis(),
            title = parsedMedia.title,
            sourceUrl = parsedMedia.originalUrl,
            downloadUrl = selectedFormat.downloadUrl,
            platform = parsedMedia.platform.name,
            mediaType = selectedFormat.mediaType.name,
            formatName = selectedFormat.formatName,
            qualityLabel = selectedFormat.qualityLabel,
            thumbnailUrl = parsedMedia.thumbnailUrl,
            localFilePath = targetFile.absolutePath,
            fileSizeBytes = selectedFormat.estimatedSizeBytes,
            status = "DOWNLOADING"
        )
        val recordId = repository.insert(entity)

        val notificationId = notificationIdCounter.incrementAndGet()

        withContext(Dispatchers.Main) {
            Toast.makeText(
                appContext,
                "ডাউনলোড শুরু হচ্ছে: ${parsedMedia.title.take(25)}...",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Launch resilient in-app background download
        CoroutineScope(Dispatchers.IO).launch {
            executeDirectStreamDownload(
                recordId = recordId,
                notificationId = notificationId,
                targetFile = targetFile,
                parsedMedia = parsedMedia,
                selectedFormat = selectedFormat
            )
        }

        recordId
    }

    private suspend fun executeDirectStreamDownload(
        recordId: Long,
        notificationId: Int,
        targetFile: File,
        parsedMedia: ParsedMedia,
        selectedFormat: MediaFormat
    ) {
        val notifBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Downloading: ${parsedMedia.title}")
            .setContentText("0% • ${selectedFormat.qualityLabel}")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        notificationManager.notify(notificationId, notifBuilder.build())

        try {
            val request = Request.Builder()
                .url(selectedFormat.downloadUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; Quest 2) AppleWebKit/537.36 (KHTML, like Gecko) OculusBrowser/15.0.0.0.22.280371431 SamsungBrowser/4.0 Chrome/89.0.4389.90 Mobile VR Safari/537.36"
                )
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Server returned HTTP ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("Empty response body")
            val totalBytes = if (body.contentLength() > 0) body.contentLength() else selectedFormat.estimatedSizeBytes

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastUpdate = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdate > 500) {
                    lastUpdate = now
                    val progress = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                    val sizeProgress = UrlUtils.formatFileSize(totalRead)

                    notifBuilder
                        .setContentText("$progress% ($sizeProgress) • ${selectedFormat.qualityLabel}")
                        .setProgress(100, progress, totalBytes <= 0)

                    notificationManager.notify(notificationId, notifBuilder.build())
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Update Room database
            repository.updateStatusById(
                id = recordId,
                status = "COMPLETED",
                filePath = targetFile.absolutePath,
                fileSize = totalRead
            )

            // Scan file into Android media gallery/music players
            val mimeType = if (selectedFormat.mediaType == MediaType.VIDEO) "video/mp4" else "audio/mp4"
            MediaScannerConnection.scanFile(
                appContext,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType),
                null
            )

            // Complete Notification with tap to open
            val openIntent = createOpenIntent(targetFile.absolutePath, mimeType)
            val pendingOpenIntent = PendingIntent.getActivity(
                appContext,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val completedNotif = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setContentTitle("Download Complete!")
                .setContentText("${parsedMedia.title} • Tap to play")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(pendingOpenIntent)
                .setAutoCancel(true)
                .setOngoing(false)
                .build()

            notificationManager.notify(notificationId, completedNotif)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    appContext,
                    "ডাউনলোড সম্পূর্ণ হয়েছে! (Downloads/MyDownload)",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            repository.updateStatusById(
                id = recordId,
                status = "FAILED",
                filePath = null,
                fileSize = 0L
            )

            val failedNotif = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setContentTitle("Download Failed")
                .setContentText(e.localizedMessage ?: "Network interrupted")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .setOngoing(false)
                .build()

            notificationManager.notify(notificationId, failedNotif)

            if (targetFile.exists() && targetFile.length() == 0L) {
                targetFile.delete()
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    appContext,
                    "ডাউনলোড ব্যর্থ হয়েছে: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createOpenIntent(filePath: String, mimeType: String): Intent {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openDownloadedFile(filePath: String?, mimeType: String = "video/*") {
        if (filePath == null) return
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(appContext, "ফাইলটি পাওয়া যায়নি বা মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )

            val resolvedMime = if (filePath.endsWith(".m4a") || filePath.endsWith(".mp3")) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(appContext, "ভিডিওটি চালানোর জন্য উপযুক্ত প্লেয়ার পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDownloadedFile(filePath: String?, mimeType: String = "video/*") {
        if (filePath == null) return
        val file = File(filePath)
        if (!file.exists()) return

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )

            val resolvedMime = if (filePath.endsWith(".m4a") || filePath.endsWith(".mp3")) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = resolvedMime
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(Intent.createChooser(intent, "শেয়ার করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
        }
    }

    suspend fun deleteFile(entity: DownloadEntity) = withContext(Dispatchers.IO) {
        if (entity.localFilePath != null) {
            val f = File(entity.localFilePath)
            if (f.exists()) {
                f.delete()
            }
        }
        repository.delete(entity)
    }
}
