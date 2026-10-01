package com.example.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.download.DownloadController
import com.example.model.MediaFormat
import com.example.model.MediaType
import com.example.model.ParsedMedia
import com.example.model.PlatformType
import com.example.parser.MediaParserEngine
import com.example.ui.theme.BrandFacebook
import com.example.ui.theme.BrandPinterest
import com.example.ui.theme.BrandYouTube
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayDownloadSheet(
    sharedUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isLoading by remember { mutableStateOf(true) }
    var parsedMedia by remember { mutableStateOf<ParsedMedia?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedMediaType by remember { mutableStateOf(MediaType.VIDEO) }
    var selectedFormat by remember { mutableStateOf<MediaFormat?>(null) }
    var isDownloadStarted by remember { mutableStateOf(false) }

    val downloadController = remember { DownloadController(context) }

    LaunchedEffect(sharedUrl) {
        isLoading = true
        errorMessage = null
        val result = MediaParserEngine.parse(sharedUrl)
        result.onSuccess { media ->
            parsedMedia = media
            val hasVideo = media.availableFormats.any { it.mediaType == MediaType.VIDEO }
            if (hasVideo) {
                selectedMediaType = MediaType.VIDEO
                selectedFormat = media.availableFormats.firstOrNull { it.mediaType == MediaType.VIDEO }
            } else {
                selectedMediaType = MediaType.AUDIO
                selectedFormat = media.availableFormats.firstOrNull { it.mediaType == MediaType.AUDIO }
            }
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Unable to parse this media stream"
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFF334155), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with App Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(CyanAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "My Download Overlay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Instant Share Downloader",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                // Loading State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(DarkSurfaceVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = CyanAccent,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Analyzing media link...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Extracting video & audio streams",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else if (errorMessage != null) {
                // Error State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Could not parse stream directly",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("Dismiss", color = Color.White)
                        }
                    }
                }
            } else if (parsedMedia != null) {
                val media = parsedMedia!!

                // Media Preview Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (media.thumbnailUrl != null) {
                            AsyncImage(
                                model = media.thumbnailUrl,
                                contentDescription = "Thumbnail",
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Platform Tag
                        val (badgeColor, badgeText) = when (media.platform) {
                            PlatformType.PINTEREST -> Pair(BrandPinterest, "Pinterest")
                            PlatformType.FACEBOOK -> Pair(BrandFacebook, "Facebook")
                            PlatformType.YOUTUBE -> Pair(BrandYouTube, "YouTube")
                            PlatformType.WEB -> Pair(CyanAccent, "Web Video")
                        }

                        Surface(
                            color = badgeColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = media.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Format Type Selector (MP4 Video / MP3 Audio)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    // MP4 Video Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedMediaType == MediaType.VIDEO) CyanAccent else Color.Transparent)
                            .clickable {
                                selectedMediaType = MediaType.VIDEO
                                val defaultVideo = media.availableFormats.firstOrNull { it.mediaType == MediaType.VIDEO }
                                if (defaultVideo != null) selectedFormat = defaultVideo
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (selectedMediaType == MediaType.VIDEO) Color.Black else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MP4 Video",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedMediaType == MediaType.VIDEO) Color.Black else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // MP3 Audio Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedMediaType == MediaType.AUDIO) PurpleAccent else Color.Transparent)
                            .clickable {
                                selectedMediaType = MediaType.AUDIO
                                val defaultAudio = media.availableFormats.firstOrNull { it.mediaType == MediaType.AUDIO }
                                if (defaultAudio != null) selectedFormat = defaultAudio
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = if (selectedMediaType == MediaType.AUDIO) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MP3 Audio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedMediaType == MediaType.AUDIO) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resolution / Quality Options List
                val filteredFormats = media.availableFormats.filter { it.mediaType == selectedMediaType }

                Text(
                    text = if (selectedMediaType == MediaType.VIDEO) "Select Video Resolution:" else "Select Audio Quality:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredFormats.forEach { format ->
                        val isSelected = selectedFormat?.id == format.id
                        val borderColor = if (isSelected) {
                            if (selectedMediaType == MediaType.VIDEO) CyanAccent else PurpleAccent
                        } else Color(0xFF293548)

                        val bgColor = if (isSelected) {
                            if (selectedMediaType == MediaType.VIDEO) CyanAccent.copy(alpha = 0.12f)
                            else PurpleAccent.copy(alpha = 0.12f)
                        } else DarkSurfaceVariant

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .clickable { selectedFormat = format }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (selectedMediaType == MediaType.VIDEO) Icons.Default.HighQuality else Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = if (isSelected) CyanAccent else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = format.qualityLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                    )
                                    val sizeStr = com.example.parser.UrlUtils.formatFileSize(format.estimatedSizeBytes)
                                    val subtitle = if (sizeStr.isNotEmpty()) "${format.formatName} • $sizeStr" else "${format.formatName} format"
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(
                                        2.dp,
                                        if (isSelected) (if (selectedMediaType == MediaType.VIDEO) CyanAccent else PurpleAccent)
                                        else Color(0xFF475569),
                                        CircleShape
                                    )
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                if (selectedMediaType == MediaType.VIDEO) CyanAccent else PurpleAccent,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Download Button or Success State
                AnimatedVisibility(
                    visible = !isDownloadStarted,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = {
                            val format = selectedFormat ?: return@Button
                            isDownloadStarted = true
                            coroutineScope.launch {
                                downloadController.startDownload(media, format)
                                delay(600) // Brief feedback before dismiss
                                onDismiss()
                            }
                        },
                        enabled = selectedFormat != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMediaType == MediaType.VIDEO) CyanAccent else PurpleAccent,
                            contentColor = if (selectedMediaType == MediaType.VIDEO) Color.Black else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download ${selectedFormat?.formatName ?: ""} (${selectedFormat?.qualityLabel ?: ""})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isDownloadStarted,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(EmeraldSuccess.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .border(1.dp, EmeraldSuccess, RoundedCornerShape(14.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download Started! Check Notifications",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
