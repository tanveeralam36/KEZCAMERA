package com.example.teleprompter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun TeleprompterOverlay(
    scriptTitle: String,
    scriptText: String,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    config: PrompterConfig,
    onConfigChange: (PrompterConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isControlsExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Smooth continuous auto-scroll based on time delta
    LaunchedEffect(isPlaying, config.scrollSpeedPxPerSec) {
        if (isPlaying) {
            var lastTimeNanos = System.nanoTime()
            while (isPlaying && scrollState.value < scrollState.maxValue) {
                delay(16) // ~60 FPS
                val currentTimeNanos = System.nanoTime()
                val deltaSeconds = (currentTimeNanos - lastTimeNanos) / 1_000_000_000f
                lastTimeNanos = currentTimeNanos

                val deltaPx = config.scrollSpeedPxPerSec * deltaSeconds
                scrollState.dispatchRawDelta(deltaPx)
            }
        }
    }

    val backgroundColor = Color.Black.copy(alpha = config.bgOpacityPercent / 100f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset { IntOffset(0, offsetY.roundToInt()) }
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)
                .testTag("teleprompter_card")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with Lens Proximity Indicator and Drag Handle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetY = (offsetY + dragAmount.y).coerceIn(-100f, 600f)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    // Lens proximity indicator badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(
                                color = Color(0xFFEF4444).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Text(
                            text = "LOOK HERE 👀 LENS",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Drag handle pill
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.4f))
                    )

                    // Compact controls toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { isControlsExpanded = !isControlsExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isControlsExpanded) Icons.Default.Tune else Icons.Default.Tune,
                                contentDescription = "Settings",
                                tint = if (isControlsExpanded) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                onRestart()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Restart",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onPlayPauseToggle,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = if (isPlaying) "Pause Prompter" else "Play Prompter",
                                tint = if (isPlaying) Color(0xFFFACC15) else Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Eye contact alignment guide line (horizontal subtle marker)
                if (config.isGuideLineVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.35f))
                    )
                }

                // Scrolling Script Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(config.windowHeightDp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = scriptText.ifBlank { "No script selected. Tap Scripts below to add or select a script." },
                        color = config.textColor,
                        fontSize = config.fontSizeSp.sp,
                        lineHeight = (config.fontSizeSp * 1.35f).sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = config.textAlignment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .graphicsLayer {
                                if (config.isMirrored) {
                                    scaleX = -1f
                                }
                            }
                            .padding(vertical = 12.dp)
                    )
                }

                // Expandable Teleprompter Tuning Drawer
                AnimatedVisibility(
                    visible = isControlsExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.92f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Speed Control Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Speed: ${config.scrollSpeedPxPerSec.roundToInt()} px/s",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = config.scrollSpeedPxPerSec,
                                onValueChange = { onConfigChange(config.copy(scrollSpeedPxPerSec = it)) },
                                valueRange = 10f..120f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Font Size Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.FormatSize,
                                contentDescription = "Font size",
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Size: ${config.fontSizeSp.roundToInt()} sp",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = config.fontSizeSp,
                                onValueChange = { onConfigChange(config.copy(fontSizeSp = it)) },
                                valueRange = 16f..38f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Background Opacity Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Opacity,
                                contentDescription = "Opacity",
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Opacity: ${config.bgOpacityPercent}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = config.bgOpacityPercent.toFloat(),
                                onValueChange = { onConfigChange(config.copy(bgOpacityPercent = it.roundToInt())) },
                                valueRange = 20f..100f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Action Toggles: Text Align, Mirror Text, Height
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Text Align Toggles
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = config.textAlignment == TextAlign.Left || config.textAlignment == TextAlign.Start,
                                    onClick = { onConfigChange(config.copy(textAlignment = TextAlign.Start)) },
                                    label = { Text("Left", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                FilterChip(
                                    selected = config.textAlignment == TextAlign.Center,
                                    onClick = { onConfigChange(config.copy(textAlignment = TextAlign.Center)) },
                                    label = { Text("Center", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }

                            // Mirror toggle
                            FilterChip(
                                selected = config.isMirrored,
                                onClick = { onConfigChange(config.copy(isMirrored = !config.isMirrored)) },
                                label = { Text("🪞 Mirror", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF43F5E),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        // Text Color Palette
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Color:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            val palette = listOf(
                                Color.White to "White",
                                Color(0xFFFACC15) to "Yellow",
                                Color(0xFF38BDF8) to "Cyan",
                                Color(0xFF34D399) to "Emerald",
                                Color(0xFFF472B6) to "Pink"
                            )
                            palette.forEach { (color, _) ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .pointerInput(Unit) {
                                            detectDragGestures { _, _ -> }
                                        }
                                        .padding(if (config.textColor == color) 2.dp else 0.dp)
                                ) {
                                    IconButton(
                                        onClick = { onConfigChange(config.copy(textColor = color)) },
                                        modifier = Modifier.fillMaxSize()
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
