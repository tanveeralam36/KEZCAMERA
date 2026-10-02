package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.RecordingAspectMode
import com.example.camera.ResolutionOption

@Composable
fun DualLivePreviewPip(
    aspectMode: RecordingAspectMode,
    resolution: ResolutionOption,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    if (aspectMode != RecordingAspectMode.DUAL_SIMULTANEOUS) return

    var isExpanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
        modifier = modifier
            .padding(horizontal = 16.dp)
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Header bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Splitscreen,
                        contentDescription = "Dual Frame",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "DUAL LIVE STREAM FRAMES",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (isRecording) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Text(
                                text = "REC DUAL",
                                color = Color(0xFFEF4444),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 9:16 Vertical Preview Frame Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(10.dp))
                            .padding(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "9:16 VERTICAL",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = resolution.resolutionTag,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 9.sp
                                )
                            }

                            // Silhouette center crop guide
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight(0.65f)
                                    .align(Alignment.CenterHorizontally)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            ) {
                                Text(
                                    text = "Shorts / Reels",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 8.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            Text(
                                text = "Auto-cropped output #1",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 8.sp
                            )
                        }
                    }

                    // 16:9 Horizontal Preview Frame Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.5.dp, Color(0xFFFACC15), RoundedCornerShape(10.dp))
                            .padding(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "16:9 HORIZONTAL",
                                    color = Color(0xFFFACC15),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = resolution.resolutionTag,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 9.sp
                                )
                            }

                            // Horizontal widescreen guide
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .fillMaxHeight(0.55f)
                                    .align(Alignment.CenterHorizontally)
                                    .background(Color(0xFFFACC15).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFFFACC15).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            ) {
                                Text(
                                    text = "YouTube / TV",
                                    color = Color(0xFFFACC15),
                                    fontSize = 8.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            Text(
                                text = "Synchronized output #2",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
