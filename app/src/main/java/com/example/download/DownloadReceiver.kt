package com.example.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.widget.Toast
import com.example.MainActivity
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == -1L) return

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val sizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1
                        val localUriString = if (uriIndex != -1) cursor.getString(uriIndex) else null
                        val sizeBytes = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                        cursor.close()

                        val db = AppDatabase.getInstance(context)
                        val cleanPath = localUriString?.replace("file://", "")

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            db.downloadDao().updateDownloadStatus(
                                dmId = downloadId,
                                status = "COMPLETED",
                                filePath = cleanPath,
                                fileSize = sizeBytes
                            )

                            // Trigger Android Media Scanner so it appears in Gallery/Music players
                            cleanPath?.let { path ->
                                MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(path),
                                    null,
                                    null
                                )
                            }

                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(
                                    context,
                                    "Download completed! Saved to Downloads/MyDownload",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            db.downloadDao().updateDownloadStatus(
                                dmId = downloadId,
                                status = "FAILED",
                                filePath = null,
                                fileSize = 0L
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } else if (action == DownloadManager.ACTION_NOTIFICATION_CLICKED) {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(openAppIntent)
        }
    }
}
