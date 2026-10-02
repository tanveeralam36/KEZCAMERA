package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.RecordingAspectMode

@Composable
fun DualFrameOverlay(
    aspectMode: RecordingAspectMode,
    isRuleOfThirdsVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Rule of thirds subtle grid
            if (isRuleOfThirdsVisible) {
                val gridColor = Color.White.copy(alpha = 0.15f)
                val strokeWidth = 1.dp.toPx()
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                // Vertical lines
                drawLine(
                    color = gridColor,
                    start = Offset(canvasWidth / 3f, 0f),
                    end = Offset(canvasWidth / 3f, canvasHeight),
                    strokeWidth = strokeWidth,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = gridColor,
                    start = Offset(canvasWidth * 2f / 3f, 0f),
                    end = Offset(canvasWidth * 2f / 3f, canvasHeight),
                    strokeWidth = strokeWidth,
                    pathEffect = dashEffect
                )

                // Horizontal lines
                drawLine(
                    color = gridColor,
                    start = Offset(0f, canvasHeight / 3f),
                    end = Offset(canvasWidth, canvasHeight / 3f),
                    strokeWidth = strokeWidth,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, canvasHeight * 2f / 3f),
                    end = Offset(canvasWidth, canvasHeight * 2f / 3f),
                    strokeWidth = strokeWidth,
                    pathEffect = dashEffect
                )
            }

            // Framing guidelines based on aspect mode
            when (aspectMode) {
                RecordingAspectMode.VERTICAL_9_16 -> {
                    // Portrait 9:16 target inside full viewfinder
                    val targetAspect = 9f / 16f
                    val currentAspect = canvasWidth / canvasHeight
                    if (currentAspect > targetAspect) {
                        // Wider than 9:16, pillarbox side masks
                        val targetWidth = canvasHeight * targetAspect
                        val horizontalMargin = (canvasWidth - targetWidth) / 2f

                        // Shaded side pillars
                        drawRect(
                            color = Color.Black.copy(alpha = 0.45f),
                            topLeft = Offset(0f, 0f),
                            size = Size(horizontalMargin, canvasHeight)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.45f),
                            topLeft = Offset(canvasWidth - horizontalMargin, 0f),
                            size = Size(horizontalMargin, canvasHeight)
                        )

                        // 9:16 frame border
                        drawRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(horizontalMargin, 0f),
                            size = Size(targetWidth, canvasHeight),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                RecordingAspectMode.HORIZONTAL_16_9 -> {
                    // Landscape 16:9 target inside portrait viewfinder
                    val targetAspect = 16f / 9f
                    // If device is in portrait, 16:9 is width / (width * 9/16)
                    val targetHeight = canvasWidth / targetAspect
                    val verticalMargin = (canvasHeight - targetHeight) / 2f

                    // Top and bottom letterbox shades
                    drawRect(
                        color = Color.Black.copy(alpha = 0.45f),
                        topLeft = Offset(0f, 0f),
                        size = Size(canvasWidth, verticalMargin)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.45f),
                        topLeft = Offset(0f, canvasHeight - verticalMargin),
                        size = Size(canvasWidth, verticalMargin)
                    )

                    // 16:9 frame border
                    drawRect(
                        color = Color(0xFFFACC15),
                        topLeft = Offset(0f, verticalMargin),
                        size = Size(canvasWidth, targetHeight),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                RecordingAspectMode.DUAL_SIMULTANEOUS -> {
                    // Dual mode: Show both frames simultaneously!
                    // 16:9 horizontal guide (Yellow)
                    val targetHeight169 = canvasWidth / (16f / 9f)
                    val verticalMargin169 = (canvasHeight - targetHeight169) / 2f

                    drawRect(
                        color = Color(0xFFFACC15).copy(alpha = 0.85f),
                        topLeft = Offset(0f, verticalMargin169),
                        size = Size(canvasWidth, targetHeight169),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                        )
                    )

                    // 9:16 vertical pillar guide (Cyan)
                    val targetWidth916 = canvasHeight * (9f / 16f)
                    val horizontalMargin916 = (canvasWidth - targetWidth916).coerceAtLeast(0f) / 2f

                    if (horizontalMargin916 > 0) {
                        drawRect(
                            color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                            topLeft = Offset(horizontalMargin916, 0f),
                            size = Size(targetWidth916, canvasHeight),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                            )
                        )
                    }

                    // Intersection center sweet spot
                    val sweetSpot = Rect(
                        left = horizontalMargin916.coerceAtLeast(20f),
                        top = verticalMargin169,
                        right = canvasWidth - horizontalMargin916.coerceAtLeast(20f),
                        bottom = canvasHeight - verticalMargin169
                    )
                    drawRect(
                        color = Color(0xFF10B981).copy(alpha = 0.4f),
                        topLeft = sweetSpot.topLeft,
                        size = sweetSpot.size,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        // Floating Badges indicating active framing mode
        if (aspectMode == RecordingAspectMode.DUAL_SIMULTANEOUS) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 16:9 Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFACC15), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "16:9 Main",
                        color = Color(0xFFFACC15),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 9:16 Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF38BDF8), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "9:16 Shorts",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
