package com.example.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.*
import com.example.teleprompter.TeleprompterOverlay
import com.example.ui.components.*
import com.example.viewmodel.TeleCamViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordScreen(
    viewModel: TeleCamViewModel,
    modifier: Modifier = Modifier
) {
    val cameraManager = viewModel.cameraManager
    val recordingStatus by cameraManager.recordingStatus.collectAsState()
    val recordingDurationMs by cameraManager.recordingDurationMs.collectAsState()
    val isFrontCamera by cameraManager.isFrontCamera.collectAsState()
    val isTorchOn by cameraManager.isTorchOn.collectAsState()
    val zoomRatio by cameraManager.zoomRatio.collectAsState()
    val linearZoom by cameraManager.linearZoom.collectAsState()
    val exposureIndex by cameraManager.exposureIndex.collectAsState()
    val exposureRange by cameraManager.exposureRange.collectAsState()
    val aspectMode by cameraManager.aspectMode.collectAsState()
    val resolution by cameraManager.resolution.collectAsState()
    val supportedResolutions by cameraManager.supportedResolutions.collectAsState()
    val availableAudioInputs by cameraManager.availableAudioInputs.collectAsState()
    val selectedAudioInput by cameraManager.selectedAudioInput.collectAsState()
    val errorMessage by cameraManager.errorMessage.collectAsState()
    val focusPoint by cameraManager.focusPoint.collectAsState()

    val activeScript by viewModel.activeScript.collectAsState()
    val prompterConfig by viewModel.prompterConfig.collectAsState()
    val isPrompterPlaying by viewModel.isPrompterPlaying.collectAsState()
    val isCountdownActive by viewModel.isCountdownActive.collectAsState()
    val countdownSeconds by viewModel.countdownSeconds.collectAsState()
    val countdownOption by viewModel.countdownOption.collectAsState()
    val isCleanMode by viewModel.isCleanMode.collectAsState()

    // Menu dropdown states
    var showResolutionMenu by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }
    var showCountdownMenu by remember { mutableStateOf(false) }
    var showMicMenu by remember { mutableStateOf(false) }
    var showExposureSlider by remember { mutableStateOf(false) }
    var showZoomSlider by remember { mutableStateOf(false) }

    // Permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    if (!permissionsState.allPermissionsGranted) {
        PermissionRequiredScreen(
            permissionsState = permissionsState,
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Live Camera Preview
        CameraPreviewView(
            cameraManager = cameraManager,
            onTapFocus = { _, _ -> },
            onZoomChange = { _ -> },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Dual Aspect Ratio Framing Overlay Guides
        DualFrameOverlay(
            aspectMode = aspectMode,
            isRuleOfThirdsVisible = true
        )

        // Focus tap indicator
        focusPoint?.let { (fx, fy) ->
            Box(
                modifier = Modifier
                    .offset(x = (fx - 24).dp, y = (fy - 24).dp)
                    .size(48.dp)
                    .border(2.dp, Color(0xFFFACC15), CircleShape)
            )
        }

        // 3. Floating Teleprompter Overlay positioned near camera lens
        TeleprompterOverlay(
            scriptTitle = activeScript?.title ?: "Select a Script",
            scriptText = activeScript?.content ?: "",
            isPlaying = isPrompterPlaying,
            onPlayPauseToggle = { viewModel.togglePrompterPlayPause() },
            onRestart = { viewModel.restartPrompter() },
            config = prompterConfig,
            onConfigChange = { viewModel.updatePrompterConfig(it) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (isCleanMode) 12.dp else 56.dp)
        )

        // 4. Live Dual Preview PIP in Dual Mode
        if (aspectMode == RecordingAspectMode.DUAL_SIMULTANEOUS && !isCleanMode) {
            DualLivePreviewPip(
                aspectMode = aspectMode,
                resolution = resolution,
                isRecording = recordingStatus == RecordingStatus.RECORDING,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 110.dp)
            )
        }

        // 5. Top Controls Bar
        AnimatedVisibility(
            visible = !isCleanMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopCameraBar(
                isFrontCamera = isFrontCamera,
                isTorchOn = isTorchOn,
                aspectMode = aspectMode,
                resolution = resolution,
                countdown = countdownOption,
                selectedMic = selectedAudioInput?.name ?: "Mic",
                isRecording = recordingStatus != RecordingStatus.IDLE,
                onSwitchCamera = { cameraManager.switchCamera() },
                onToggleTorch = { cameraManager.toggleTorch() },
                onAspectClick = { showAspectMenu = true },
                onResolutionClick = { showResolutionMenu = true },
                onCountdownClick = { showCountdownMenu = true },
                onMicClick = { showMicMenu = true },
                onCleanModeClick = { viewModel.toggleCleanMode() }
            )
        }

        // Clean mode restore button when clean mode is active
        if (isCleanMode) {
            IconButton(
                onClick = { viewModel.toggleCleanMode() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = "Show Controls", tint = Color.White)
            }
        }

        // 6. Center Zoom & Exposure Quick Controls (Slider drawers)
        if (showZoomSlider) {
            ZoomSliderCard(
                zoomRatio = zoomRatio,
                linearZoom = linearZoom,
                onZoomChange = { cameraManager.setLinearZoom(it) },
                onClose = { showZoomSlider = false },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )
        }

        if (showExposureSlider) {
            ExposureSliderCard(
                exposureIndex = exposureIndex,
                exposureRange = exposureRange,
                onExposureChange = { cameraManager.setExposureIndex(it) },
                onClose = { showExposureSlider = false },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            )
        }

        // 7. Bottom Recording Controls
        BottomRecordingBar(
            recordingStatus = recordingStatus,
            recordingDurationMs = recordingDurationMs,
            zoomRatio = zoomRatio,
            exposureIndex = exposureIndex,
            onRecordClicked = { viewModel.onRecordButtonClicked(hasAudioPermission = true) },
            onPauseClicked = { viewModel.pauseRecording() },
            onResumeClicked = { viewModel.resumeRecording() },
            onStopClicked = { viewModel.stopRecording() },
            onZoomClicked = { showZoomSlider = !showZoomSlider },
            onExposureClicked = { showExposureSlider = !showExposureSlider },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 74.dp)
        )

        // 8. Countdown Overlay Animation
        if (isCountdownActive) {
            CountdownOverlay(
                seconds = countdownSeconds,
                onCountdownFinished = { viewModel.onCountdownFinished(hasAudioPermission = true) },
                onCancel = { viewModel.cancelCountdown() }
            )
        }

        // 9. Processing Indicator
        if (recordingStatus == RecordingStatus.PROCESSING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Processing Video Files...",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (aspectMode == RecordingAspectMode.DUAL_SIMULTANEOUS) {
                                "Generating synchronized 9:16 and 16:9 videos"
                            } else {
                                "Saving high-resolution MP4"
                            },
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Error snackbar / alert
        errorMessage?.let { error ->
            Snackbar(
                action = {
                    TextButton(onClick = { cameraManager.clearError() }) {
                        Text("Dismiss", color = Color(0xFF38BDF8))
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Text(error)
            }
        }

        // Dropdown Menus
        AspectDropdown(
            expanded = showAspectMenu,
            selected = aspectMode,
            onSelect = {
                cameraManager.setAspectMode(it)
                showAspectMenu = false
            },
            onDismiss = { showAspectMenu = false }
        )

        ResolutionDropdown(
            expanded = showResolutionMenu,
            selected = resolution,
            supported = supportedResolutions,
            onSelect = {
                cameraManager.setResolution(it)
                showResolutionMenu = false
            },
            onDismiss = { showResolutionMenu = false }
        )

        CountdownDropdown(
            expanded = showCountdownMenu,
            selected = countdownOption,
            onSelect = {
                viewModel.setCountdownOption(it)
                showCountdownMenu = false
            },
            onDismiss = { showCountdownMenu = false }
        )

        MicDropdown(
            expanded = showMicMenu,
            devices = availableAudioInputs,
            selected = selectedAudioInput,
            onSelect = {
                cameraManager.selectAudioInput(it)
                showMicMenu = false
            },
            onDismiss = { showMicMenu = false }
        )
    }
}

@Composable
private fun TopCameraBar(
    isFrontCamera: Boolean,
    isTorchOn: Boolean,
    aspectMode: RecordingAspectMode,
    resolution: ResolutionOption,
    countdown: CountdownOption,
    selectedMic: String,
    isRecording: Boolean,
    onSwitchCamera: () -> Unit,
    onToggleTorch: () -> Unit,
    onAspectClick: () -> Unit,
    onResolutionClick: () -> Unit,
    onCountdownClick: () -> Unit,
    onMicClick: () -> Unit,
    onCleanModeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Camera Switch
        IconButton(
            onClick = onSwitchCamera,
            enabled = !isRecording,
            modifier = Modifier.size(36.dp).testTag("switch_camera_button")
        ) {
            Icon(
                Icons.Default.FlipCameraAndroid,
                contentDescription = "Switch Camera",
                tint = if (!isRecording) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Torch
        IconButton(
            onClick = onToggleTorch,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Torch",
                tint = if (isTorchOn) Color(0xFFFACC15) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Aspect Ratio Selector Chip
        SuggestionChip(
            onClick = onAspectClick,
            enabled = !isRecording,
            label = {
                Text(
                    text = aspectMode.badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (aspectMode == RecordingAspectMode.DUAL_SIMULTANEOUS) Color(0xFF38BDF8) else Color.White
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Color.White.copy(alpha = 0.12f)
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
                enabled = true,
                borderColor = if (aspectMode == RecordingAspectMode.DUAL_SIMULTANEOUS) Color(0xFF38BDF8) else Color.Transparent
            )
        )

        // Resolution Selector Chip
        SuggestionChip(
            onClick = onResolutionClick,
            enabled = !isRecording,
            label = {
                Text(
                    text = resolution.shortName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Color.White.copy(alpha = 0.12f)
            )
        )

        // Countdown Selector Chip
        SuggestionChip(
            onClick = onCountdownClick,
            enabled = !isRecording,
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = "Timer", modifier = Modifier.size(12.dp), tint = Color.White)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = countdown.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Color.White.copy(alpha = 0.12f)
            )
        )

        // Audio Input Mic Chip
        IconButton(
            onClick = onMicClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Mic: $selectedMic", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
        }

        // Clean Mode Toggle
        IconButton(
            onClick = onCleanModeClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.VisibilityOff, contentDescription = "Clean Mode", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BottomRecordingBar(
    recordingStatus: RecordingStatus,
    recordingDurationMs: Long,
    zoomRatio: Float,
    exposureIndex: Int,
    onRecordClicked: () -> Unit,
    onPauseClicked: () -> Unit,
    onResumeClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onZoomClicked: () -> Unit,
    onExposureClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recording Timer Badge
        if (recordingStatus == RecordingStatus.RECORDING || recordingStatus == RecordingStatus.PAUSED) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (recordingStatus == RecordingStatus.RECORDING) Color(0xFFEF4444) else Color(0xFFFACC15))
                )
                Text(
                    text = if (recordingStatus == RecordingStatus.PAUSED) "PAUSED" else "REC",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimer(recordingDurationMs),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        // Quick adjust chips: Zoom & Exposure
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = false,
                onClick = onZoomClicked,
                label = {
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", zoomRatio)}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = FilterChipDefaults.filterChipColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )

            FilterChip(
                selected = false,
                onClick = onExposureClicked,
                label = {
                    Text(
                        text = "EV: ${if (exposureIndex > 0) "+$exposureIndex" else "$exposureIndex"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = FilterChipDefaults.filterChipColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        }

        Spacer(Modifier.height(14.dp))

        // Main Record & Control Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Secondary control button (Pause/Resume when recording)
            if (recordingStatus == RecordingStatus.RECORDING) {
                IconButton(
                    onClick = onPauseClicked,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            } else if (recordingStatus == RecordingStatus.PAUSED) {
                IconButton(
                    onClick = onResumeClicked,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.3f), CircleShape)
                        .testTag("resume_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                }
            } else {
                Spacer(Modifier.size(52.dp))
            }

            // Central Record Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(6.dp)
                    .clickable { onRecordClicked() }
                    .testTag("record_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (recordingStatus == RecordingStatus.RECORDING || recordingStatus == RecordingStatus.PAUSED) 32.dp else 64.dp)
                        .clip(if (recordingStatus == RecordingStatus.RECORDING || recordingStatus == RecordingStatus.PAUSED) RoundedCornerShape(8.dp) else CircleShape)
                        .background(Color(0xFFEF4444))
                )
            }

            // Stop button when recording
            if (recordingStatus == RecordingStatus.RECORDING || recordingStatus == RecordingStatus.PAUSED) {
                IconButton(
                    onClick = onStopClicked,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.3f), CircleShape)
                        .testTag("stop_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                }
            } else {
                Spacer(Modifier.size(52.dp))
            }
        }
    }
}

@Composable
private fun ZoomSliderCard(
    zoomRatio: Float,
    linearZoom: Float,
    onZoomChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
        modifier = modifier.width(160.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Zoom", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = "${String.format(Locale.getDefault(), "%.1f", zoomRatio)}x",
                color = Color(0xFF38BDF8),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Slider(
                value = linearZoom,
                onValueChange = onZoomChange,
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onZoomChange(0f) }, contentPadding = PaddingValues(0.dp)) {
                    Text("1x", fontSize = 11.sp, color = Color.White)
                }
                TextButton(onClick = { onZoomChange(0.33f) }, contentPadding = PaddingValues(0.dp)) {
                    Text("2x", fontSize = 11.sp, color = Color.White)
                }
                TextButton(onClick = { onZoomChange(0.66f) }, contentPadding = PaddingValues(0.dp)) {
                    Text("3x", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ExposureSliderCard(
    exposureIndex: Int,
    exposureRange: Pair<Int, Int>,
    onExposureChange: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (min, max) = exposureRange
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
        modifier = modifier.width(160.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Exposure", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = if (exposureIndex > 0) "+$exposureIndex" else "$exposureIndex",
                color = Color(0xFFFACC15),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Slider(
                value = exposureIndex.toFloat(),
                onValueChange = { onExposureChange(it.toInt()) },
                valueRange = min.toFloat()..max.toFloat(),
                steps = (max - min).coerceAtLeast(1),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AspectDropdown(
    expanded: Boolean,
    selected: RecordingAspectMode,
    onSelect: (RecordingAspectMode) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF1E293B))
    ) {
        RecordingAspectMode.values().forEach { mode ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = mode.label,
                            fontWeight = if (mode == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == selected) Color(0xFF38BDF8) else Color.White
                        )
                        Text(
                            text = mode.description,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                onClick = { onSelect(mode) }
            )
        }
    }
}

@Composable
private fun ResolutionDropdown(
    expanded: Boolean,
    selected: ResolutionOption,
    supported: List<ResolutionOption>,
    onSelect: (ResolutionOption) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF1E293B))
    ) {
        ResolutionOption.values().forEach { res ->
            val isSupported = supported.contains(res)
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = res.label,
                            fontWeight = if (res == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (res == selected) Color(0xFF38BDF8) else if (isSupported) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                        if (!isSupported) {
                            Text(
                                text = "Unsupported",
                                fontSize = 10.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                },
                onClick = {
                    if (isSupported) onSelect(res)
                },
                enabled = isSupported
            )
        }
    }
}

@Composable
private fun CountdownDropdown(
    expanded: Boolean,
    selected: CountdownOption,
    onSelect: (CountdownOption) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF1E293B))
    ) {
        CountdownOption.values().forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = option.label + if (option.seconds > 0) " countdown" else "",
                        fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (option == selected) Color(0xFF38BDF8) else Color.White
                    )
                },
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun MicDropdown(
    expanded: Boolean,
    devices: List<AudioInputDevice>,
    selected: AudioInputDevice?,
    onSelect: (AudioInputDevice) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF1E293B))
    ) {
        if (devices.isEmpty()) {
            DropdownMenuItem(
                text = { Text("Standard Built-in Mic", color = Color.White) },
                onClick = onDismiss
            )
        } else {
            devices.forEach { device ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${device.name} (${device.type})",
                            fontWeight = if (device == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (device == selected) Color(0xFF38BDF8) else Color.White
                        )
                    },
                    onClick = { onSelect(device) }
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequiredScreen(
    permissionsState: com.google.accompanist.permissions.MultiplePermissionsState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Camera & Audio Access Needed",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "DualPrompter requires camera and microphone permissions to record professional high-resolution videos with simultaneous audio.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                modifier = Modifier.fillMaxWidth().testTag("grant_permissions_button")
            ) {
                Text("Grant Permissions", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatTimer(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
