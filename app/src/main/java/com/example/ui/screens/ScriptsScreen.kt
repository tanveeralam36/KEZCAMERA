package com.example.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Script
import com.example.viewmodel.TeleCamViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsScreen(
    viewModel: TeleCamViewModel,
    modifier: Modifier = Modifier
) {
    val scripts by viewModel.allScripts.collectAsState()
    val activeScript by viewModel.activeScript.collectAsState()
    val isEditorOpen by viewModel.isScriptEditorOpen.collectAsState()
    val editingScript by viewModel.editingScript.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var scriptToDelete by remember { mutableStateOf<Script?>(null) }

    val filteredScripts = remember(scripts, searchQuery) {
        if (searchQuery.isBlank()) scripts
        else scripts.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Scripts Studio",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${scripts.size} scripts available",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.openNewScriptEditor() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.padding(end = 8.dp).testTag("new_script_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Script", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Script", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewScriptEditor() },
                containerColor = Color(0xFF38BDF8),
                contentColor = Color.Black,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 72.dp)
                    .testTag("fab_add_script")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Script")
            }
        },
        containerColor = Color(0xFF0B1120),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search scripts or topics...", color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Quick Template Inspirations
            Text(
                text = "CREATOR TEMPLATES",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                item {
                    TemplateChip(
                        title = "⚡ Tech Review Hook",
                        onClick = {
                            viewModel.saveScript(
                                title = "Tech Review Hook",
                                content = "Today we're testing the game changer everyone is talking about.\n\nIs it worth your hard-earned money, or just marketing hype?\n\nLet's unpack the pros, cons, and performance numbers!"
                            )
                        }
                    )
                }
                item {
                    TemplateChip(
                        title = "🎯 60s Elevator Pitch",
                        onClick = {
                            viewModel.saveScript(
                                title = "60s Startup Pitch",
                                content = "Most creators waste half their day editing two versions of every video.\n\nOur solution records vertical and horizontal takes simultaneously while you read your teleprompter seamlessly.\n\nSave 10 hours this week!"
                            )
                        }
                    )
                }
                item {
                    TemplateChip(
                        title = "📢 News & Updates",
                        onClick = {
                            viewModel.saveScript(
                                title = "Breaking Community Update",
                                content = "Good morning everyone. We have three major announcements to share with you today.\n\nFirst, our brand new schedule kicks off Monday.\n\nSecond, join our live Q&A this Friday.\n\nLet's get straight into the details."
                            )
                        }
                    )
                }
            }

            // Script Cards List
            if (filteredScripts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "No scripts saved yet" else "No matching scripts",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap 'New Script' above to write or paste your video script for teleprompter recording.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredScripts, key = { it.id }) { script ->
                        val isActive = script.id == activeScript?.id
                        ScriptCard(
                            script = script,
                            isActive = isActive,
                            onSelectActive = {
                                viewModel.selectScript(script)
                                viewModel.setTab(0) // Jump directly to camera to record!
                            },
                            onEdit = { viewModel.openEditScript(script) },
                            onDelete = { scriptToDelete = script }
                        )
                    }
                }
            }
        }
    }

    // Script Editor Dialog
    if (isEditorOpen) {
        ScriptEditorDialog(
            script = editingScript,
            onDismiss = { viewModel.closeScriptEditor() },
            onSave = { title, content ->
                viewModel.saveScript(title, content)
            }
        )
    }

    // Delete Confirmation Dialog
    scriptToDelete?.let { script ->
        AlertDialog(
            onDismissRequest = { scriptToDelete = null },
            title = { Text("Delete Script?") },
            text = { Text("Are you sure you want to delete '${script.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteScript(script)
                        scriptToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScriptCard(
    script: Script,
    isActive: Boolean,
    onSelectActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val wordCount = remember(script.content) {
        script.content.split(Regex("\\s+")).count { it.isNotBlank() }
    }
    // Estimated reading time assuming ~130 words per minute
    val estSeconds = (wordCount / 130.0 * 60).toInt().coerceAtLeast(5)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 0.5.dp,
                color = if (isActive) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelectActive() }
            .testTag("script_card_${script.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = script.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Excerpt preview
            Text(
                text = script.content.trim(),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            // Stats and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount words",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "• ~${estSeconds}s read",
                        color = Color(0xFFFACC15),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = onSelectActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) Color(0xFF10B981) else Color(0xFF38BDF8)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isActive) "Ready to Record" else "Record with this",
                            fontSize = 11.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateChip(
    title: String,
    onClick: () -> Unit
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(title, fontSize = 12.sp, color = Color.White) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0xFF1E293B)
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = Color(0xFF38BDF8).copy(alpha = 0.4f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptEditorDialog(
    script: Script?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(script?.title ?: "") }
    var content by remember { mutableStateOf(script?.content ?: "") }

    val wordCount = remember(content) {
        content.split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val estSeconds = (wordCount / 130.0 * 60).toInt().coerceAtLeast(0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (script != null) "Edit Script" else "New Script", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (title.isBlank()) title = "Script - ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())}"
                                onSave(title.trim(), content.trim())
                            },
                            enabled = content.isNotBlank(),
                            modifier = Modifier.testTag("save_script_button")
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, color = if (content.isNotBlank()) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.4f))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )
            },
            containerColor = Color(0xFF0B1120)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Script Title (e.g. YouTube Episode 14)") },
                    label = { Text("Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("script_title_input")
                )

                Spacer(Modifier.height(12.dp))

                // Stats & Paste Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount words • ~$estSeconds sec read time",
                        color = Color(0xFFFACC15),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                                val item = clipboard.primaryClip?.getItemAt(0)
                                val pasted = item?.text?.toString() ?: ""
                                if (pasted.isNotBlank()) {
                                    content = if (content.isBlank()) pasted else "$content\n$pasted"
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Paste Text", fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Script Content Multiline TextField
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            "Type or paste your complete video script here...\n\nKeep sentences natural and concise for easy teleprompter reading.",
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("script_content_input")
                )
            }
        }
    }
}
