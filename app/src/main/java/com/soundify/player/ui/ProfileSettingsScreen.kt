package com.soundify.player.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soundify.player.utils.ReleaseInfo
import com.soundify.player.utils.UpdaterManager
import kotlinx.coroutines.launch

@Composable
fun ProfileSettingsDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = DriftTheme.colors
    val currentName by viewModel.userName.collectAsState()
    val currentAvatar by viewModel.userAvatarUrl.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var nameInput by remember { mutableStateOf(currentName) }
    var avatarInput by remember { mutableStateOf(currentAvatar) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateDialogInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }

    val currentVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    if (updateDialogInfo != null) {
        AlertDialog(
            onDismissRequest = { if (downloadProgress == null) updateDialogInfo = null },
            title = {
                Text(
                    text = "Update Available (${updateDialogInfo!!.versionName})",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "What's New:",
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = updateDialogInfo!!.releaseNotes,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 8
                    )
                    if (downloadProgress != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress!! },
                            color = colors.accent,
                            trackColor = colors.card,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Downloading... ${(downloadProgress!! * 100).toInt()}%",
                            color = colors.textPrimary,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (downloadProgress == null) {
                    Button(
                        onClick = {
                            downloadProgress = 0f
                            scope.launch {
                                UpdaterManager.downloadAndInstall(context, updateDialogInfo!!.downloadUrl) { prog ->
                                    downloadProgress = prog
                                }.onFailure {
                                    updateMessage = "Download failed: ${it.message}"
                                    downloadProgress = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Download & Install", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (downloadProgress == null) {
                    TextButton(onClick = { updateDialogInfo = null }) {
                        Text("Later", color = colors.textSecondary)
                    }
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile & Settings",
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Picture Row
                ProfileImagePicker(
                    currentImageUri = avatarInput.takeIf { it.isNotBlank() }?.let {
                        if (it.startsWith("/")) Uri.fromFile(File(it)) else Uri.parse(it)
                    },
                    onImageSelected = { newUri ->
                        avatarInput = newUri.toString()
                        viewModel.updateProfile(nameInput, newUri.toString())
                    },
                    onImageRemoved = {
                        avatarInput = ""
                        viewModel.updateProfile(nameInput, "")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Display Name Field
                Text(
                    text = "Display Name",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            viewModel.updateProfile(it, avatarInput)
                        },
                        placeholder = { Text("Your Name", color = colors.textSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border
                        )
                    )
                    Button(
                        onClick = { viewModel.updateProfile(nameInput, avatarInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Appearance Settings
                Text(
                    text = "Appearance",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .clickable { viewModel.toggleDarkMode() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Theme Mode", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (isDarkMode) "Dark (Pure Black OLED)" else "Light (Crisp Slate)", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.accent,
                            checkedTrackColor = colors.accent.copy(alpha = 0.35f),
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.border
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Statistics Row
                Text(
                    text = "Library Overview",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text("Liked Songs", color = colors.textSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${likedTracks.size}", color = colors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text("Playlists", color = colors.textSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${playlists.size}", color = colors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Storage & Cache
                Text(
                    text = "Storage & Data",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .clickable {
                            viewModel.clearRecentSearches()
                            viewModel.showStatusMessage("Search history cleared")
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Clear Search History", color = colors.textPrimary, fontSize = 14.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Updates
                Text(
                    text = "App Updates",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isCheckingUpdate = true
                        updateMessage = null
                        scope.launch {
                            UpdaterManager.checkForUpdate(currentVersion).onSuccess { release ->
                                isCheckingUpdate = false
                                if (release != null) {
                                    updateDialogInfo = release
                                } else {
                                    updateMessage = "You are up to date! (v$currentVersion)"
                                }
                            }.onFailure {
                                isCheckingUpdate = false
                                updateMessage = "Check failed: ${it.localizedMessage}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Checking for updates...", color = colors.textPrimary, fontSize = 14.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for Updates", color = colors.textPrimary, fontSize = 14.sp)
                        }
                    }
                }

                if (updateMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = updateMessage!!,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Drift Music v$currentVersion",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Liquid Glass Edition • Seamless Gapless Playback",
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
