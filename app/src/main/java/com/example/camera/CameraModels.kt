package com.example.camera

enum class RecordingAspectMode(val label: String, val badge: String, val description: String) {
    VERTICAL_9_16("9:16 Vertical", "9:16", "Shorts, TikTok, Reels"),
    HORIZONTAL_16_9("16:9 Horizontal", "16:9", "YouTube, Desktop, TV"),
    DUAL_SIMULTANEOUS("Dual 9:16 + 16:9", "DUAL", "Simultaneous Dual-Frame Capture")
}

enum class ResolutionOption(val label: String, val resolutionTag: String, val shortName: String) {
    HD_720P("720p HD", "720p", "HD"),
    FHD_1080P("1080p Full HD", "1080p", "FHD"),
    UHD_4K("4K Ultra HD", "4K", "4K")
}

enum class RecordingStatus {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    PROCESSING
}

enum class CountdownOption(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_5(5, "5s"),
    SEC_10(10, "10s")
}

data class CameraSettings(
    val aspectMode: RecordingAspectMode = RecordingAspectMode.DUAL_SIMULTANEOUS,
    val resolution: ResolutionOption = ResolutionOption.FHD_1080P,
    val isFrontCamera: Boolean = true,
    val isTorchOn: Boolean = false,
    val zoomLinear: Float = 0f,
    val zoomRatio: Float = 1f,
    val exposureCompensationIndex: Int = 0,
    val exposureMin: Int = -4,
    val exposureMax: Int = 4,
    val countdown: CountdownOption = CountdownOption.SEC_3,
    val isAudioEnabled: Boolean = true,
    val isGuideGridVisible: Boolean = true,
    val isDualPreviewVisible: Boolean = true
)
