package com.example.ui.overlay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.parser.UrlUtils
import com.example.ui.theme.MyDownloadTheme

class ShareOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawText = extractSharedText(intent)
        val extractedUrl = rawText?.let { UrlUtils.extractUrlFromText(it) }

        if (extractedUrl.isNullOrEmpty()) {
            Toast.makeText(this, "No valid link found in shared content", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MyDownloadTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    OverlayDownloadSheet(
                        sharedUrl = extractedUrl,
                        onDismiss = {
                            finish()
                            overridePendingTransition(0, 0)
                        }
                    )
                }
            }
        }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent == null) return null
        if (Intent.ACTION_SEND == intent.action) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) return text

            val clipData = intent.clipData
            if (clipData != null && clipData.itemCount > 0) {
                val itemText = clipData.getItemAt(0).text?.toString()
                if (!itemText.isNullOrBlank()) return itemText
                val itemUri = clipData.getItemAt(0).uri?.toString()
                if (!itemUri.isNullOrBlank()) return itemUri
            }

            val data = intent.dataString
            if (!data.isNullOrBlank()) return data
        }
        return null
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
