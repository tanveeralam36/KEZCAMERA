package com.example.camera

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreviewView(
    cameraManager: CameraManager,
    onTapFocus: (Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        cameraManager.bindCamera(lifecycleOwner, previewView.surfaceProvider)
        onDispose {
            // Unbind handled by cameraManager lifecycle or unbindAll
        }
    }

    val gestureDetector = remember {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val x = e.x
                val y = e.y
                cameraManager.focusOnPoint(x, y, previewView.width.toFloat(), previewView.height.toFloat())
                onTapFocus(x, y)
                return true
            }
        })
    }

    val scaleGestureDetector = remember {
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                // Map scale factor to linear zoom delta
                val delta = (scaleFactor - 1.0f) * 0.5f
                val currentZoom = cameraManager.linearZoom.value
                val newZoom = (currentZoom + delta).coerceIn(0f, 1f)
                cameraManager.setLinearZoom(newZoom)
                onZoomChange(newZoom)
                return true
            }
        })
    }

    AndroidView(
        factory = {
            previewView.setOnTouchListener { _, event ->
                var handled = scaleGestureDetector.onTouchEvent(event)
                handled = gestureDetector.onTouchEvent(event) || handled
                true
            }
            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}
