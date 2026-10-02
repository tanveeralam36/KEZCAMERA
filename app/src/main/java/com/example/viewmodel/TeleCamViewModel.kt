package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.*
import com.example.data.db.AppDatabase
import com.example.data.model.Script
import com.example.data.model.VideoRecord
import com.example.teleprompter.PrompterConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class TeleCamViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val scriptDao = database.scriptDao()
    private val videoRecordDao = database.videoRecordDao()

    val cameraManager = CameraManager(application, videoRecordDao)

    // Data streams from Room
    val allScripts: StateFlow<List<Script>> = scriptDao.getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVideos: StateFlow<List<VideoRecord>> = videoRecordDao.getAllVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active script loaded into prompter
    private val _activeScript = MutableStateFlow<Script?>(null)
    val activeScript: StateFlow<Script?> = _activeScript.asStateFlow()

    // Teleprompter config state
    private val _prompterConfig = MutableStateFlow(PrompterConfig())
    val prompterConfig: StateFlow<PrompterConfig> = _prompterConfig.asStateFlow()

    // Teleprompter playback state
    private val _isPrompterPlaying = MutableStateFlow(false)
    val isPrompterPlaying: StateFlow<Boolean> = _isPrompterPlaying.asStateFlow()

    // Countdown active state
    private val _isCountdownActive = MutableStateFlow(false)
    val isCountdownActive: StateFlow<Boolean> = _isCountdownActive.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(3)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    // Selected countdown setting
    private val _countdownOption = MutableStateFlow(CountdownOption.SEC_3)
    val countdownOption: StateFlow<CountdownOption> = _countdownOption.asStateFlow()

    // Active bottom navigation screen (0: Record, 1: Scripts, 2: My Videos)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Active video being previewed in player dialog
    private val _selectedVideoForPreview = MutableStateFlow<VideoRecord?>(null)
    val selectedVideoForPreview: StateFlow<VideoRecord?> = _selectedVideoForPreview.asStateFlow()

    // Script currently being edited in dialog
    private val _editingScript = MutableStateFlow<Script?>(null)
    val editingScript: StateFlow<Script?> = _editingScript.asStateFlow()

    private val _isScriptEditorOpen = MutableStateFlow(false)
    val isScriptEditorOpen: StateFlow<Boolean> = _isScriptEditorOpen.asStateFlow()

    // Controls visibility toggle (clean prompter view while recording)
    private val _isCleanMode = MutableStateFlow(false)
    val isCleanMode: StateFlow<Boolean> = _isCleanMode.asStateFlow()

    init {
        // Automatically select the first script once loaded
        viewModelScope.launch {
            allScripts.collect { scripts ->
                if (_activeScript.value == null && scripts.isNotEmpty()) {
                    selectScript(scripts.first())
                }
            }
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun selectScript(script: Script) {
        _activeScript.value = script
        _prompterConfig.value = _prompterConfig.value.copy(
            fontSizeSp = script.fontSizeSp,
            scrollSpeedPxPerSec = script.scrollSpeedPxPerSec,
            bgOpacityPercent = script.bgOpacityPercent,
            textColor = parseColorHex(script.textColorHex)
        )
    }

    fun togglePrompterPlayPause() {
        _isPrompterPlaying.value = !_isPrompterPlaying.value
    }

    fun restartPrompter() {
        _isPrompterPlaying.value = false
        // Briefly pause then resume if recording is active
        viewModelScope.launch {
            if (cameraManager.recordingStatus.value == RecordingStatus.RECORDING) {
                _isPrompterPlaying.value = true
            }
        }
    }

    fun updatePrompterConfig(newConfig: PrompterConfig) {
        _prompterConfig.value = newConfig
    }

    fun setCountdownOption(option: CountdownOption) {
        _countdownOption.value = option
    }

    fun toggleCleanMode() {
        _isCleanMode.value = !_isCleanMode.value
    }

    fun onRecordButtonClicked(hasAudioPermission: Boolean) {
        when (cameraManager.recordingStatus.value) {
            RecordingStatus.IDLE -> {
                val countSec = _countdownOption.value.seconds
                if (countSec > 0) {
                    _countdownSeconds.value = countSec
                    _isCountdownActive.value = true
                } else {
                    startRecordingInternal(hasAudioPermission)
                }
            }
            RecordingStatus.RECORDING -> {
                // Stop recording
                cameraManager.stopRecording()
                _isPrompterPlaying.value = false
            }
            RecordingStatus.PAUSED -> {
                cameraManager.stopRecording()
                _isPrompterPlaying.value = false
            }
            else -> {}
        }
    }

    fun onCountdownFinished(hasAudioPermission: Boolean) {
        _isCountdownActive.value = false
        startRecordingInternal(hasAudioPermission)
    }

    fun cancelCountdown() {
        _isCountdownActive.value = false
    }

    private fun startRecordingInternal(hasAudioPermission: Boolean) {
        val scriptTitle = _activeScript.value?.title
        cameraManager.startRecording(
            hasAudioPermission = hasAudioPermission,
            activeScriptTitle = scriptTitle,
            onRecordingStarted = {
                _isPrompterPlaying.value = true
            }
        )
    }

    fun pauseRecording() {
        cameraManager.pauseRecording()
        _isPrompterPlaying.value = false
    }

    fun resumeRecording() {
        cameraManager.resumeRecording()
        _isPrompterPlaying.value = true
    }

    fun stopRecording() {
        cameraManager.stopRecording()
        _isPrompterPlaying.value = false
    }

    // Scripts CRUD
    fun openNewScriptEditor() {
        _editingScript.value = null
        _isScriptEditorOpen.value = true
    }

    fun openEditScript(script: Script) {
        _editingScript.value = script
        _isScriptEditorOpen.value = true
    }

    fun closeScriptEditor() {
        _editingScript.value = null
        _isScriptEditorOpen.value = false
    }

    fun saveScript(title: String, content: String) {
        viewModelScope.launch {
            val current = _editingScript.value
            if (current != null) {
                val updated = current.copy(
                    title = title,
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
                scriptDao.updateScript(updated)
                if (_activeScript.value?.id == current.id) {
                    _activeScript.value = updated
                }
            } else {
                val newScript = Script(
                    title = title,
                    content = content,
                    fontSizeSp = _prompterConfig.value.fontSizeSp,
                    scrollSpeedPxPerSec = _prompterConfig.value.scrollSpeedPxPerSec
                )
                val id = scriptDao.insertScript(newScript)
                _activeScript.value = newScript.copy(id = id)
            }
            closeScriptEditor()
        }
    }

    fun deleteScript(script: Script) {
        viewModelScope.launch {
            scriptDao.deleteScript(script)
            if (_activeScript.value?.id == script.id) {
                _activeScript.value = allScripts.value.firstOrNull { it.id != script.id }
            }
        }
    }

    // Videos Management
    fun selectVideoForPreview(video: VideoRecord) {
        _selectedVideoForPreview.value = video
    }

    fun dismissVideoPreview() {
        _selectedVideoForPreview.value = null
    }

    fun renameVideo(video: VideoRecord, newTitle: String) {
        viewModelScope.launch {
            videoRecordDao.updateVideo(video.copy(title = newTitle))
            if (_selectedVideoForPreview.value?.id == video.id) {
                _selectedVideoForPreview.value = video.copy(title = newTitle)
            }
        }
    }

    fun deleteVideo(video: VideoRecord) {
        viewModelScope.launch {
            try {
                val file = File(video.filePath)
                if (file.exists()) file.delete()
                if (video.notes.isNotBlank()) {
                    val thumb = File(video.notes)
                    if (thumb.exists()) thumb.delete()
                }
                videoRecordDao.deleteVideo(video)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseColorHex(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color.White
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.release()
    }
}
