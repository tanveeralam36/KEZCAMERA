package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.VideoRecord
import com.example.ui.components.VideoPlayerDialog
import com.example.viewmodel.TeleCamViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    viewModel: TeleCamViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videos by viewModel.allVideos.collectAsState()
    val selectedVideoForPreview by viewModel.selectedVideoForPreview.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var videoToDelete by remember { mutableStateOf<VideoRecord?>(null) }
    var videoToRename by remember { mutableStateOf<VideoRecord?>(null) }
    var renameText by remember { mutableStateOf("") }

    val filteredVideos = remember(videos, selectedFilter) {
        when (selectedFilter) {
            "DUAL" -> videos.filter { it.isDualMode }
            "9:16" -> videos.filter { it.aspectRatio.contains("9:16") }
            "16:9" -> videos.filter { it.aspectRatio.contains("16:9") }
            else -> videos
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Studio Recordings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${videos.size} videos in studio library",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0B1120),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${videos.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "DUAL",
                        onClick = { selectedFilter = "DUAL" },
                        label = { Text("⚡ Dual Takes (${videos.count { it.isDualMode }})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "9:16",
                        onClick = { selectedFilter = "9:16" },
                        label = { Text("9:16 Vertical (${videos.count { it.aspectRatio.contains("9:16") }})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "16:9",
                        onClick = { selectedFilter = "16:9" },
                        label = { Text("16:9 Horizontal (${videos.count { it.aspectRatio.contains("16:9") }})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            if (filteredVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (videos.isEmpty()) "No recordings yet" else "No matching videos",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Record with teleprompter in 9:16, 16:9, or Dual Mode. Your synchronized files will be saved here automatically.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.setTab(0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            modifier = Modifier.testTag("go_to_record_button")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Camera Studio", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredVideos, key = { it.id }) { video ->
                        VideoCardItem(
                            video = video,
                            onPlay = { viewModel.selectVideoForPreview(video) },
                            onShare = { shareVideo(context, video) },
                            onRename = {
                                videoToRename = video
                                renameText = video.title
                            },
                            onDelete = { videoToDelete = video }
                        )
                    }
                }
            }
        }
    }

    // Video Player Dialog
    selectedVideoForPreview?.let { video ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { viewModel.dismissVideoPreview() },
            onRename = { v, newTitle -> viewModel.renameVideo(v, newTitle) },
            onDelete = { v -> viewModel.deleteVideo(v) }
        )
    }

    // Delete confirmation dialog
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video?") },
            text = { Text("Are you sure you want to permanently delete '${video.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVideo(video)
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    videoToRename?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            title = { Text("Rename Video") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Video Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameVideo(video, renameText.trim())
                        }
                        videoToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VideoCardItem(
    video: VideoRecord,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(video.createdAt) {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(video.createdAt))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .clickable { onPlay() }
            .testTag("video_card_${video.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail / Preview Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFF0F172A))
            ) {
                // If thumbnail image exists, display it
                if (video.notes.isNotBlank() && File(video.notes).exists()) {
                    AsyncImage(
                        model = File(video.notes),
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Default fallback visual pattern
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (video.aspectRatio.contains("9:16")) Icons.Default.StayCurrentPortrait else Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                // Aspect Badge on top-left of preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (video.aspectRatio.contains("9:16")) Color(0xFF0284C7) else Color(0xFFCA8A04),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = video.aspectRatio,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (video.isDualMode) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF10B981), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⚡ DUAL SYNC",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Duration badge on bottom-right of preview
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationMs),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Play icon in center
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Info & Metadata Details
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$dateStr • ${video.resolution} • ${formatFileSize(video.fileSizeBytes)}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onPlay,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onRename, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun shareVideo(context: Context, video: VideoRecord) {
    try {
        val file = File(video.filePath)
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, video.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}
