package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.camera.CountdownOption
import com.example.camera.RecordingAspectMode
import com.example.camera.ResolutionOption
import com.example.data.model.Script
import com.example.data.model.VideoRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DualPrompter", appName)
    }

    @Test
    fun `verify script creation and properties`() {
        val script = Script(
            title = "Test Script",
            content = "This is a teleprompter test script.",
            fontSizeSp = 24f,
            scrollSpeedPxPerSec = 45f
        )
        assertEquals("Test Script", script.title)
        assertTrue(script.content.isNotEmpty())
        assertEquals(24f, script.fontSizeSp)
    }

    @Test
    fun `verify video record dual mode pair linking`() {
        val horizontal = VideoRecord(
            id = 1L,
            title = "Dual Take (16:9)",
            filePath = "/path/to/horiz.mp4",
            aspectRatio = "16:9",
            resolution = "1080p",
            durationMs = 12000L,
            fileSizeBytes = 5000000L,
            isDualMode = true,
            linkedPairId = 2L
        )

        val vertical = VideoRecord(
            id = 2L,
            title = "Dual Take (9:16)",
            filePath = "/path/to/vert.mp4",
            aspectRatio = "9:16",
            resolution = "1080p",
            durationMs = 12000L,
            fileSizeBytes = 5000000L,
            isDualMode = true,
            linkedPairId = 1L
        )

        assertEquals(2L, horizontal.linkedPairId)
        assertEquals(1L, vertical.linkedPairId)
        assertEquals(horizontal.durationMs, vertical.durationMs)
    }

    @Test
    fun `verify recording aspect modes and countdown options`() {
        assertEquals(3, RecordingAspectMode.values().size)
        assertNotNull(RecordingAspectMode.DUAL_SIMULTANEOUS)
        assertNotNull(RecordingAspectMode.VERTICAL_9_16)
        assertNotNull(RecordingAspectMode.HORIZONTAL_16_9)

        assertEquals(3, CountdownOption.SEC_3.seconds)
        assertEquals(5, CountdownOption.SEC_5.seconds)
        assertEquals(10, CountdownOption.SEC_10.seconds)
        assertEquals(0, CountdownOption.OFF.seconds)
    }
}
