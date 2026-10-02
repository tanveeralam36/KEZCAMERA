package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.RecordingStatus
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.ScriptsScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TeleCamViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TeleCamViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Prevent screen timeout during studio teleprompter & video sessions
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MyApplicationTheme {
                TeleCamStudioApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TeleCamStudioApp(viewModel: TeleCamViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val recordingStatus by viewModel.cameraManager.recordingStatus.collectAsState()
    val isCleanMode by viewModel.isCleanMode.collectAsState()

    // Handle back button on sub-screens
    if (currentTab != 0) {
        BackHandler {
            viewModel.setTab(0)
        }
    }

    Scaffold(
        bottomBar = {
            // Hide bottom bar during active recording clean mode or when user wants zero clutter
            AnimatedVisibility(
                visible = !(recordingStatus == RecordingStatus.RECORDING && isCleanMode),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.setTab(0) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 0) Icons.Filled.Videocam else Icons.Outlined.Videocam,
                                contentDescription = "Record"
                            )
                        },
                        label = { Text("Record", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFF38BDF8),
                            indicatorColor = Color(0xFF38BDF8),
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_record")
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.setTab(1) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 1) Icons.Filled.Description else Icons.Outlined.Description,
                                contentDescription = "Scripts"
                            )
                        },
                        label = { Text("Scripts", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFF38BDF8),
                            indicatorColor = Color(0xFF38BDF8),
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_scripts")
                    )

                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { viewModel.setTab(2) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 2) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                                contentDescription = "My Videos"
                            )
                        },
                        label = { Text("My Videos", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFF38BDF8),
                            indicatorColor = Color(0xFF38BDF8),
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_videos")
                    )
                }
            }
        },
        containerColor = Color(0xFF0B1120),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (recordingStatus == RecordingStatus.RECORDING && isCleanMode) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            when (currentTab) {
                0 -> RecordScreen(viewModel = viewModel)
                1 -> ScriptsScreen(viewModel = viewModel)
                2 -> VideosScreen(viewModel = viewModel)
            }
        }
    }
}
