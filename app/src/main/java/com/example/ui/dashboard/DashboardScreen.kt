package com.example.ui.dashboard

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.DownloadEntity
import com.example.model.MediaType
import com.example.model.PlatformType
import com.example.ui.theme.BrandFacebook
import com.example.ui.theme.BrandPinterest
import com.example.ui.theme.BrandYouTube
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanAccentGlow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowToDialog by remember { mutableStateOf(false) }

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    brush = Brush.linearGradient(listOf(CyanAccent, PurpleAccent)),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Logo",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "My Download",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Pinterest • YouTube • Facebook",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanAccent,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHowToDialog = true },
                        modifier = Modifier.testTag("help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help Guide",
                            tint = CyanAccentGlow
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Share Overlay Feature Card
            item {
                ShareOverlayPromoCard(onLearnMore = { showHowToDialog = true })
            }

            // Quick Download Input Box
            item {
                ManualDownloadCard(
                    url = uiState.inputUrl,
                    isParsing = uiState.isParsing,
                    errorMessage = uiState.parseError,
                    onUrlChange = { viewModel.onUrlInputChanged(it) },
                    onPaste = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text?.toString() ?: ""
                            viewModel.onUrlInputChanged(text)
                            Toast.makeText(context, "Link pasted!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClear = { viewModel.onUrlInputChanged("") },
                    onParse = { viewModel.parseCurrentUrl() }
                )
            }

            // Parsed Result Format Dialog
            if (uiState.parsedMedia != null) {
                item {
                    ParsedResultCard(
                        media = uiState.parsedMedia!!,
                        selectedFormat = uiState.selectedFormat,
                        isDownloading = uiState.isDownloadInitiated,
                        onSelectFormat = { viewModel.selectFormat(it) },
                        onDownload = { viewModel.startDownload() },
                        onCancel = { viewModel.dismissParseDialog() }
                    )
                }
            }

            // Supported Platforms Chips
            item {
                SupportedPlatformsRow()
            }

            // Downloads List Header & Filters
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloads History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${uiState.downloads.size} files",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.activeFilter == "ALL",
                            onClick = { viewModel.setFilter("ALL") },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceVariant,
                                labelColor = Color(0xFFCBD5E1)
                            )
                        )
                        FilterChip(
                            selected = uiState.activeFilter == "VIDEO",
                            onClick = { viewModel.setFilter("VIDEO") },
                            label = { Text("Videos (MP4)") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceVariant,
                                labelColor = Color(0xFFCBD5E1)
                            )
                        )
                        FilterChip(
                            selected = uiState.activeFilter == "AUDIO",
                            onClick = { viewModel.setFilter("AUDIO") },
                            label = { Text("Audio (MP3)") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleAccent,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceVariant,
                                labelColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }

            // Download items
            if (uiState.downloads.isEmpty()) {
                item {
                    EmptyDownloadsView()
                }
            } else {
                items(uiState.downloads, key = { it.id }) { item ->
                    DownloadItemCard(
                        entity = item,
                        onPlay = { viewModel.openFile(item) },
                        onShare = { viewModel.shareFile(item) },
                        onDelete = { viewModel.deleteDownload(item) }
                    )
                }
            }
        }
    }

    if (showHowToDialog) {
        HowToOverlayDialog(onDismiss = { showHowToDialog = false })
    }
}

@Composable
fun ShareOverlayPromoCard(onLearnMore: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(CyanAccent.copy(alpha = 0.6f), PurpleAccent.copy(alpha = 0.6f))),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Instant Share Overlay",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = EmeraldSuccess,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Share link from Pinterest, Facebook or YouTube -> Tap 'My Download' -> Quick overlay selector pops up without leaving app!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            IconButton(onClick = onLearnMore) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Details",
                    tint = CyanAccent
                )
            }
        }
    }
}

@Composable
fun ManualDownloadCard(
    url: String,
    isParsing: Boolean,
    errorMessage: String?,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onParse: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Manual Video & Audio Downloader",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Paste any Pinterest, YouTube, Facebook, or web video link:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = { Text("https://pin.it/... or facebook.com/reel/...", color = Color(0xFF64748B)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (url.isNotEmpty()) {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                            }
                        }
                        IconButton(onClick = onPaste, modifier = Modifier.testTag("paste_button")) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = CyanAccent)
                        }
                    }
                }
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onParse,
                enabled = url.isNotBlank() && !isParsing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("parse_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = Color.Black
                )
            ) {
                if (isParsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parsing Media Stream...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze & Download", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ParsedResultCard(
    media: com.example.model.ParsedMedia,
    selectedFormat: com.example.model.MediaFormat?,
    isDownloading: Boolean,
    onSelectFormat: (com.example.model.MediaFormat) -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, CyanAccent, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (media.thumbnailUrl != null) {
                        AsyncImage(
                            model = media.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (media.author != null) {
                        Text(
                            text = media.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            var selectedType by remember(media) {
                mutableStateOf(if (media.availableFormats.any { it.mediaType == MediaType.VIDEO }) MediaType.VIDEO else MediaType.AUDIO)
            }

            // MP4 Video vs MP3 Audio Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedType == MediaType.VIDEO) CyanAccent else Color.Transparent)
                        .clickable {
                            selectedType = MediaType.VIDEO
                            val v = media.availableFormats.firstOrNull { it.mediaType == MediaType.VIDEO }
                            if (v != null) onSelectFormat(v)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (selectedType == MediaType.VIDEO) Color.Black else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "MP4 Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedType == MediaType.VIDEO) Color.Black else Color(0xFF94A3B8)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedType == MediaType.AUDIO) PurpleAccent else Color.Transparent)
                        .clickable {
                            selectedType = MediaType.AUDIO
                            val a = media.availableFormats.firstOrNull { it.mediaType == MediaType.AUDIO }
                            if (a != null) onSelectFormat(a)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = if (selectedType == MediaType.AUDIO) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "MP3 Audio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedType == MediaType.AUDIO) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (selectedType == MediaType.VIDEO) "Select Video Resolution:" else "Select Audio Quality:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            val displayFormats = media.availableFormats.filter { it.mediaType == selectedType }.ifEmpty { media.availableFormats }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayFormats.forEach { format ->
                    val isSelected = selectedFormat?.id == format.id
                    val isAudio = format.mediaType == MediaType.AUDIO

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) {
                                    if (isAudio) PurpleAccent.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f)
                                } else DarkSurfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) {
                                    if (isAudio) PurpleAccent else CyanAccent
                                } else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectFormat(format) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAudio) Icons.Default.Audiotrack else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (isAudio) PurpleAccent else CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val sizeStr = com.example.parser.UrlUtils.formatFileSize(format.estimatedSizeBytes)
                            val label = if (sizeStr.isNotEmpty()) "${format.formatName} • ${format.qualityLabel} ($sizeStr)" else "${format.formatName} • ${format.qualityLabel}"
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .border(
                                    2.dp,
                                    if (isSelected) (if (isAudio) PurpleAccent else CyanAccent) else Color(0xFF475569),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (isAudio) PurpleAccent else CyanAccent, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDownload,
                enabled = selectedFormat != null && !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_download_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedFormat?.mediaType == MediaType.AUDIO) PurpleAccent else CyanAccent,
                    contentColor = if (selectedFormat?.mediaType == MediaType.AUDIO) Color.White else Color.Black
                )
            ) {
                if (isDownloading) {
                    Text("Starting download...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download ${selectedFormat?.formatName ?: ""} (${selectedFormat?.qualityLabel ?: ""})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SupportedPlatformsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlatformTagCard("Pinterest", BrandPinterest, Modifier.weight(1f))
        PlatformTagCard("YouTube", BrandYouTube, Modifier.weight(1f))
        PlatformTagCard("Facebook", BrandFacebook, Modifier.weight(1f))
        PlatformTagCard("Web/MP4", CyanAccent, Modifier.weight(1f))
    }
}

@Composable
fun PlatformTagCard(name: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 12.sp
        )
    }
}

@Composable
fun DownloadItemCard(
    entity: DownloadEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_card_${entity.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Media Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                if (entity.thumbnailUrl != null) {
                    AsyncImage(
                        model = entity.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (entity.mediaType == "AUDIO") Icons.Default.Audiotrack else Icons.Default.Movie,
                        contentDescription = null,
                        tint = if (entity.mediaType == "AUDIO") PurpleAccent else CyanAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Format Chip
                    Surface(
                        color = (if (entity.mediaType == "AUDIO") PurpleAccent else CyanAccent).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${entity.formatName} • ${entity.qualityLabel}",
                            color = if (entity.mediaType == "AUDIO") PurpleAccent else CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(entity.createdAt))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Status indication
                if (entity.status == "DOWNLOADING") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = CyanAccent,
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Downloading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanAccent,
                            fontSize = 11.sp
                        )
                    }
                } else if (entity.status == "COMPLETED") {
                    val sizeLabel = com.example.parser.UrlUtils.formatFileSize(entity.fileSizeBytes)
                    val statusText = if (sizeLabel.isNotEmpty()) "Saved • $sizeLabel" else "Saved in Downloads/MyDownload"
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldSuccess,
                        fontSize = 11.sp
                    )
                }
            }

            Row {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDownloadsView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(DarkSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No Downloads Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Share any video link from Pinterest, Facebook or YouTube\nto start instant downloading via overlay!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun HowToOverlayDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(10.dp))
                Text("How Instant Share Works", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "You don't need to open this app manually to download videos! Follow these 3 easy steps:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                StepItem(
                    stepNumber = "1",
                    title = "Open Any App",
                    description = "In Pinterest, Facebook, YouTube, or your browser, find a video or reel and tap 'Share'."
                )

                StepItem(
                    stepNumber = "2",
                    title = "Tap 'My Download'",
                    description = "Select 'My Download' from the Android share sheet. A sleek floating overlay will appear over your screen."
                )

                StepItem(
                    stepNumber = "3",
                    title = "Choose Format & Download",
                    description = "Pick your preferred resolution (1080p, 720p, 480p) or MP3 audio. Download starts immediately with system notifications, without switching away from your current app!"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
            ) {
                Text("Got It!", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun StepItem(stepNumber: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(CyanAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
