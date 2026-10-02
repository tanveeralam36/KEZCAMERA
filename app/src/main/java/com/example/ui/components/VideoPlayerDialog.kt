package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.model.VideoRecord
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun VideoPlayerDialog(
    video: VideoRecord,
    onDismiss: () -> Unit,
    onRename: (VideoRecord, String) -> Unit,
    onDelete: (VideoRecord) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(video.durationMs.toInt()) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newTitleText by remember { mutableStateOf(video.title) }

    // Periodic position updater
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPosMs = vv.currentPosition
                    if (vv.duration > 0) totalDurationMs = vv.duration
                }
            }
            delay(250)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Video View Area
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoPath(video.filePath)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                totalDurationMs = duration
                                start()
                                isPlaying = true
                            }
                            setOnCompletionListener {
                                isPlaying = false
                            }
                            videoViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = video.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = video.aspectRatio,
                                color = if (video.aspectRatio.contains("9:16")) Color(0xFF38BDF8) else Color(0xFFFACC15),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• ${video.resolution}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• ${formatFileSize(video.fileSizeBytes)}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.White)
                        }
                        IconButton(onClick = { shareVideo(context, video) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF38BDF8))
                        }
                        IconButton(onClick = {
                            videoViewRef?.stopPlayback()
                            onDelete(video)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                        }
                    }
                }

                // Bottom Controls Toolbar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Timeline Slider & Time labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPosMs.toLong()),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Slider(
                            value = currentPosMs.toFloat(),
                            onValueChange = { newPos ->
                                currentPosMs = newPos.toInt()
                                videoViewRef?.seekTo(newPos.toInt())
                            },
                            valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                        Text(
                            text = formatDuration(totalDurationMs.toLong()),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    // Playback actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val target = (currentPosMs - 5000).coerceAtLeast(0)
                                videoViewRef?.seekTo(target)
                                currentPosMs = target
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Replay5, contentDescription = "-5s", tint = Color.White)
                        }

                        Spacer(Modifier.width(16.dp))

                        FilledIconButton(
                            onClick = {
                                videoViewRef?.let { vv ->
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF38BDF8)),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                val target = (currentPosMs + 5000).coerceAtMost(totalDurationMs)
                                videoViewRef?.seekTo(target)
                                currentPosMs = target
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Forward5, contentDescription = "+5s", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Recording") },
            text = {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    label = { Text("Video Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newTitleText.isNotBlank()) {
                        onRename(video, newTitleText.trim())
                    }
                    showRenameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
