package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CountdownOverlay(
    seconds: Int,
    onCountdownFinished: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember { mutableIntStateOf(seconds) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        onCountdownFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (remainingSeconds > 0) "$remainingSeconds" else "REC",
                        color = Color.White,
                        fontSize = if (remainingSeconds > 0) 56.sp else 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Look at the camera lens...",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
                Spacer(Modifier.width(6.dp))
                Text("Cancel Countdown")
            }
        }
    }
}
