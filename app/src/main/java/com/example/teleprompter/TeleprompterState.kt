package com.example.teleprompter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PrompterPosition(val label: String) {
    TOP_LENS("Near Lens (Top)"),
    UPPER_THIRD("Upper Third"),
    CENTER("Center Screen"),
    COMPACT_BANNER("Compact Banner")
}

data class PrompterConfig(
    val fontSizeSp: Float = 22f,
    val scrollSpeedPxPerSec: Float = 42f,
    val textColor: Color = Color.White,
    val bgOpacityPercent: Int = 80,
    val textAlignment: TextAlign = TextAlign.Center,
    val isMirrored: Boolean = false,
    val positionPreset: PrompterPosition = PrompterPosition.TOP_LENS,
    val windowHeightDp: Dp = 220.dp,
    val isGuideLineVisible: Boolean = true
)
