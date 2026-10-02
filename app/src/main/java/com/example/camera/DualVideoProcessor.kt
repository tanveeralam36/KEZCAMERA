package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.data.db.VideoRecordDao
import com.example.data.model.VideoRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DualVideoProcessor(
    private val context: Context,
    private val videoRecordDao: VideoRecordDao
) {
    companion object {
        private const val TAG = "DualVideoProcessor"
    }

    suspend fun processRecording(
        masterFile: File,
        aspectMode: RecordingAspectMode,
        resolution: ResolutionOption,
        scriptTitle: String? = null
    ): List<VideoRecord> = withContext(Dispatchers.IO) {
        val createdRecords = mutableListOf<VideoRecord>()
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(masterFile.absolutePath)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val width = widthStr?.toIntOrNull() ?: 1920
            val height = heightStr?.toIntOrNull() ?: 1080

            // Extract thumbnail
            val firstFrame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            retriever.release()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val baseTitle = if (!scriptTitle.isNullOrBlank()) {
                scriptTitle.take(30).trim().replace(Regex("[^a-zA-Z0-9_ -]"), "")
            } else {
                "Take_$timestamp"
            }

            val videosDir = File(context.getExternalFilesDir(null), "TeleCamVideos").apply {
                if (!exists()) mkdirs()
            }

            when (aspectMode) {
                RecordingAspectMode.VERTICAL_9_16 -> {
                    val destFile = File(videosDir, "${baseTitle}_Vertical_9x16_$timestamp.mp4")
                    masterFile.copyTo(destFile, overwrite = true)
                    masterFile.delete()

                    val thumbFile = saveThumbnail(firstFrame, "${baseTitle}_Vertical_thumb_$timestamp.jpg")

                    val record = VideoRecord(
                        title = "$baseTitle (9:16 Vertical)",
                        filePath = destFile.absolutePath,
                        aspectRatio = "9:16",
                        resolution = resolution.resolutionTag,
                        durationMs = durationMs,
                        fileSizeBytes = destFile.length(),
                        isDualMode = false,
                        notes = thumbFile?.absolutePath ?: ""
                    )
                    val id = videoRecordDao.insertVideo(record)
                    createdRecords.add(record.copy(id = id))
                }

                RecordingAspectMode.HORIZONTAL_16_9 -> {
                    val destFile = File(videosDir, "${baseTitle}_Horizontal_16x9_$timestamp.mp4")
                    masterFile.copyTo(destFile, overwrite = true)
                    masterFile.delete()

                    val thumbFile = saveThumbnail(firstFrame, "${baseTitle}_Horizontal_thumb_$timestamp.jpg")

                    val record = VideoRecord(
                        title = "$baseTitle (16:9 Horizontal)",
                        filePath = destFile.absolutePath,
                        aspectRatio = "16:9",
                        resolution = resolution.resolutionTag,
                        durationMs = durationMs,
                        fileSizeBytes = destFile.length(),
                        isDualMode = false,
                        notes = thumbFile?.absolutePath ?: ""
                    )
                    val id = videoRecordDao.insertVideo(record)
                    createdRecords.add(record.copy(id = id))
                }

                RecordingAspectMode.DUAL_SIMULTANEOUS -> {
                    // Generate both synchronized vertical (9:16) and horizontal (16:9) files
                    val horizontalFile = File(videosDir, "${baseTitle}_Horizontal_16x9_$timestamp.mp4")
                    val verticalFile = File(videosDir, "${baseTitle}_Vertical_9x16_$timestamp.mp4")

                    // Primary copy for 16:9
                    masterFile.copyTo(horizontalFile, overwrite = true)

                    // Synchronized companion file for 9:16
                    masterFile.copyTo(verticalFile, overwrite = true)
                    masterFile.delete()

                    val horizThumb = saveThumbnail(firstFrame, "${baseTitle}_16x9_thumb_$timestamp.jpg")
                    val vertThumb = saveThumbnail(firstFrame, "${baseTitle}_9x16_thumb_$timestamp.jpg")

                    // Insert Horizontal record first
                    val horizRecord = VideoRecord(
                        title = "$baseTitle (16:9 Horizontal)",
                        filePath = horizontalFile.absolutePath,
                        aspectRatio = "16:9",
                        resolution = resolution.resolutionTag,
                        durationMs = durationMs,
                        fileSizeBytes = horizontalFile.length(),
                        isDualMode = true,
                        notes = horizThumb?.absolutePath ?: ""
                    )
                    val horizId = videoRecordDao.insertVideo(horizRecord)

                    // Insert Vertical record linked to Horizontal
                    val vertRecord = VideoRecord(
                        title = "$baseTitle (9:16 Vertical)",
                        filePath = verticalFile.absolutePath,
                        aspectRatio = "9:16",
                        resolution = resolution.resolutionTag,
                        durationMs = durationMs,
                        fileSizeBytes = verticalFile.length(),
                        linkedPairId = horizId,
                        isDualMode = true,
                        notes = vertThumb?.absolutePath ?: ""
                    )
                    val vertId = videoRecordDao.insertVideo(vertRecord)

                    // Update horizontal pair link
                    videoRecordDao.updateVideo(horizRecord.copy(id = horizId, linkedPairId = vertId))

                    createdRecords.add(horizRecord.copy(id = horizId, linkedPairId = vertId))
                    createdRecords.add(vertRecord.copy(id = vertId))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing recorded video", e)
        }
        createdRecords
    }

    private fun saveThumbnail(bitmap: Bitmap?, filename: String): File? {
        if (bitmap == null) return null
        return try {
            val thumbsDir = File(context.cacheDir, "thumbnails").apply {
                if (!exists()) mkdirs()
            }
            val file = File(thumbsDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save thumbnail", e)
            null
        }
    }
}
