package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.data.db.VideoRecordDao
import com.example.data.model.VideoRecord
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class AudioInputDevice(
    val id: Int,
    val name: String,
    val type: String
)

class CameraManager(
    private val context: Context,
    private val videoRecordDao: VideoRecordDao
) {
    companion object {
        private const val TAG = "CameraManager"
    }

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val dualVideoProcessor = DualVideoProcessor(context, videoRecordDao)

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var masterOutputFile: File? = null

    // State flows
    private val _recordingStatus = MutableStateFlow(RecordingStatus.IDLE)
    val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _linearZoom = MutableStateFlow(0f)
    val linearZoom: StateFlow<Float> = _linearZoom.asStateFlow()

    private val _exposureIndex = MutableStateFlow(0)
    val exposureIndex: StateFlow<Int> = _exposureIndex.asStateFlow()

    private val _exposureRange = MutableStateFlow(Pair(-4, 4))
    val exposureRange: StateFlow<Pair<Int, Int>> = _exposureRange.asStateFlow()

    private val _aspectMode = MutableStateFlow(RecordingAspectMode.DUAL_SIMULTANEOUS)
    val aspectMode: StateFlow<RecordingAspectMode> = _aspectMode.asStateFlow()

    private val _resolution = MutableStateFlow(ResolutionOption.FHD_1080P)
    val resolution: StateFlow<ResolutionOption> = _resolution.asStateFlow()

    private val _supportedResolutions = MutableStateFlow<List<ResolutionOption>>(
        listOf(ResolutionOption.HD_720P, ResolutionOption.FHD_1080P)
    )
    val supportedResolutions: StateFlow<List<ResolutionOption>> = _supportedResolutions.asStateFlow()

    private val _availableAudioInputs = MutableStateFlow<List<AudioInputDevice>>(emptyList())
    val availableAudioInputs: StateFlow<List<AudioInputDevice>> = _availableAudioInputs.asStateFlow()

    private val _selectedAudioInput = MutableStateFlow<AudioInputDevice?>(null)
    val selectedAudioInput: StateFlow<AudioInputDevice?> = _selectedAudioInput.asStateFlow()

    private val _lastSavedVideos = MutableStateFlow<List<VideoRecord>>(emptyList())
    val lastSavedVideos: StateFlow<List<VideoRecord>> = _lastSavedVideos.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Focus state indicator for UI
    private val _focusPoint = MutableStateFlow<Pair<Float, Float>?>(null)
    val focusPoint: StateFlow<Pair<Float, Float>?> = _focusPoint.asStateFlow()

    private var currentSurfaceProvider: Preview.SurfaceProvider? = null
    private var currentLifecycleOwner: LifecycleOwner? = null

    init {
        detectAudioInputs()
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        currentLifecycleOwner = lifecycleOwner
        currentSurfaceProvider = surfaceProvider

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                startCamera(lifecycleOwner, surfaceProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera provider", e)
                _errorMessage.value = "Failed to access camera: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()

            val lensFacing = if (_isFrontCamera.value) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // Preview
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            // Quality selector
            val requestedQuality = when (_resolution.value) {
                ResolutionOption.HD_720P -> Quality.HD
                ResolutionOption.FHD_1080P -> Quality.FHD
                ResolutionOption.UHD_4K -> Quality.UHD
            }

            val qualitySelector = QualitySelector.from(
                requestedQuality,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
            )

            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .setExecutor(cameraExecutor)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )

            // Setup camera observation
            camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { zoomState ->
                _zoomRatio.value = zoomState.zoomRatio
                _linearZoom.value = zoomState.linearZoom
            }

            camera?.cameraInfo?.exposureState?.let { exp ->
                if (exp.isExposureCompensationSupported) {
                    val range = exp.exposureCompensationRange
                    _exposureRange.value = Pair(range.lower, range.upper)
                    _exposureIndex.value = exp.exposureCompensationIndex
                }
            }

            // Query supported video qualities
            checkSupportedQualities(camera?.cameraInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Binding camera use cases failed", e)
            _errorMessage.value = "Could not initialize camera: ${e.message}"
        }
    }

    private fun checkSupportedQualities(cameraInfo: CameraInfo?) {
        if (cameraInfo == null) return
        try {
            val capabilities = Recorder.getVideoCapabilities(cameraInfo)
            val supported = capabilities.getSupportedQualities(DynamicRange.SDR)
            val list = mutableListOf<ResolutionOption>()
            if (supported.contains(Quality.HD)) list.add(ResolutionOption.HD_720P)
            if (supported.contains(Quality.FHD)) list.add(ResolutionOption.FHD_1080P)
            if (supported.contains(Quality.UHD)) list.add(ResolutionOption.UHD_4K)
            if (list.isNotEmpty()) {
                _supportedResolutions.value = list
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking supported qualities", e)
        }
    }

    fun switchCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
        _isTorchOn.value = false
        val owner = currentLifecycleOwner ?: return
        val surface = currentSurfaceProvider ?: return
        startCamera(owner, surface)
    }

    fun setAspectMode(mode: RecordingAspectMode) {
        if (_recordingStatus.value == RecordingStatus.IDLE) {
            _aspectMode.value = mode
        }
    }

    fun setResolution(res: ResolutionOption) {
        if (_recordingStatus.value == RecordingStatus.IDLE) {
            _resolution.value = res
            val owner = currentLifecycleOwner ?: return
            val surface = currentSurfaceProvider ?: return
            startCamera(owner, surface)
        }
    }

    fun toggleTorch(): Boolean {
        val cam = camera ?: return false
        if (_isFrontCamera.value) return false // Front flash typically not supported via torch
        return if (cam.cameraInfo.hasFlashUnit()) {
            val newState = !_isTorchOn.value
            cam.cameraControl.enableTorch(newState)
            _isTorchOn.value = newState
            true
        } else {
            false
        }
    }

    fun setLinearZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0f, 1f)
        camera?.cameraControl?.setLinearZoom(clamped)
        _linearZoom.value = clamped
    }

    fun setExposureIndex(index: Int) {
        val (min, max) = _exposureRange.value
        val clamped = index.coerceIn(min, max)
        camera?.cameraControl?.setExposureCompensationIndex(clamped)
        _exposureIndex.value = clamped
    }

    fun focusOnPoint(x: Float, y: Float, previewWidth: Float, previewHeight: Float) {
        val cam = camera ?: return
        try {
            val factory = SurfaceOrientedMeteringPointFactory(previewWidth, previewHeight)
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            _focusPoint.value = Pair(x, y)
            cam.cameraControl.startFocusAndMetering(action)

            mainScope.launch {
                delay(2000)
                _focusPoint.value = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tap to focus error", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording(hasAudioPermission: Boolean, activeScriptTitle: String?, onRecordingStarted: () -> Unit) {
        val vc = videoCapture ?: run {
            _errorMessage.value = "Camera is not ready"
            return
        }

        if (activeRecording != null) {
            Log.w(TAG, "Recording already in progress")
            return
        }

        try {
            val tempDir = context.cacheDir
            masterOutputFile = File.createTempFile("telecam_master_", ".mp4", tempDir)

            val fileOutputOptions = FileOutputOptions.Builder(masterOutputFile!!).build()

            val pending = vc.output.prepareRecording(context, fileOutputOptions)
            if (hasAudioPermission) {
                pending.withAudioEnabled()
            }

            activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        _recordingStatus.value = RecordingStatus.RECORDING
                        _recordingDurationMs.value = 0L
                        onRecordingStarted()
                    }
                    is VideoRecordEvent.Status -> {
                        _recordingDurationMs.value = event.recordingStats.recordedDurationNanos / 1_000_000L
                    }
                    is VideoRecordEvent.Pause -> {
                        _recordingStatus.value = RecordingStatus.PAUSED
                    }
                    is VideoRecordEvent.Resume -> {
                        _recordingStatus.value = RecordingStatus.RECORDING
                    }
                    is VideoRecordEvent.Finalize -> {
                        handleRecordingFinalize(event, activeScriptTitle)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            _errorMessage.value = "Failed to start recording: ${e.localizedMessage}"
            _recordingStatus.value = RecordingStatus.IDLE
        }
    }

    fun pauseRecording() {
        activeRecording?.pause()
    }

    fun resumeRecording() {
        activeRecording?.resume()
    }

    fun stopRecording() {
        _recordingStatus.value = RecordingStatus.PROCESSING
        activeRecording?.stop()
        activeRecording = null
    }

    private fun handleRecordingFinalize(event: VideoRecordEvent.Finalize, scriptTitle: String?) {
        if (!event.hasError()) {
            val masterFile = masterOutputFile
            if (masterFile != null && masterFile.exists()) {
                mainScope.launch {
                    _recordingStatus.value = RecordingStatus.PROCESSING
                    val created = dualVideoProcessor.processRecording(
                        masterFile = masterFile,
                        aspectMode = _aspectMode.value,
                        resolution = _resolution.value,
                        scriptTitle = scriptTitle
                    )
                    _lastSavedVideos.value = created
                    _recordingStatus.value = RecordingStatus.IDLE
                    _recordingDurationMs.value = 0L
                }
            } else {
                _recordingStatus.value = RecordingStatus.IDLE
            }
        } else {
            Log.e(TAG, "Video recording error: ${event.error}")
            _errorMessage.value = "Recording stopped with error code: ${event.error}"
            _recordingStatus.value = RecordingStatus.IDLE
            masterOutputFile?.delete()
        }
        masterOutputFile = null
    }

    private fun detectAudioInputs() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                val list = devices.map { device ->
                    val typeName = when (device.type) {
                        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Mic"
                        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Mic"
                        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Mic"
                        else -> "External Mic"
                    }
                    val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && device.productName.isNotBlank()) {
                        device.productName.toString()
                    } else {
                        typeName
                    }
                    AudioInputDevice(device.id, name, typeName)
                }
                _availableAudioInputs.value = list
                _selectedAudioInput.value = list.firstOrNull()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio input detection failed", e)
        }
    }

    fun selectAudioInput(device: AudioInputDevice) {
        _selectedAudioInput.value = device
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun release() {
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
