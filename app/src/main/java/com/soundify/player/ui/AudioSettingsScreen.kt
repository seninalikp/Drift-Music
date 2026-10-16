package com.soundify.player.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import coil.compose.AsyncImage
import com.soundify.player.utils.ReleaseInfo
import com.soundify.player.utils.UpdaterManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AudioQuality(
    val label: String,
    val bitrateKbps: Int,
    val description: String
) {
    NORMAL(
        label = "Normal",
        bitrateKbps = 96,
        description = "Uses less data. Ideal for everyday listening."
    ),
    HIGH(
        label = "High",
        bitrateKbps = 160,
        description = "A balance between sound quality and data usage."
    ),
    UHD(
        label = "Very High",
        bitrateKbps = 320,
        description = "Higher-bitrate streaming for more detailed sound."
    );

    companion object {
        fun fromStoredValue(value: String?): AudioQuality {
            return entries.firstOrNull { it.name == value } ?: UHD
        }
    }
}

class AudioSettingsManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _dataSaverEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DATA_SAVER, false)
    )
    val dataSaverEnabled: StateFlow<Boolean> =
        _dataSaverEnabled.asStateFlow()

    private val _wifiQuality = MutableStateFlow(
        AudioQuality.fromStoredValue(
            prefs.getString(KEY_WIFI_QUALITY, AudioQuality.UHD.name)
        )
    )
    val wifiQuality: StateFlow<AudioQuality> =
        _wifiQuality.asStateFlow()

    private val _cellularQuality = MutableStateFlow(
        AudioQuality.fromStoredValue(
            prefs.getString(KEY_CELLULAR_QUALITY, AudioQuality.NORMAL.name)
        )
    )
    val cellularQuality: StateFlow<AudioQuality> =
        _cellularQuality.asStateFlow()

    fun setDataSaver(enabled: Boolean) {
        _dataSaverEnabled.value = enabled
        prefs.edit {
            putBoolean(KEY_DATA_SAVER, enabled)
        }
    }

    fun setWifiQuality(quality: AudioQuality) {
        _wifiQuality.value = quality
        prefs.edit {
            putString(KEY_WIFI_QUALITY, quality.name)
        }
    }

    fun setCellularQuality(quality: AudioQuality) {
        _cellularQuality.value = quality
        prefs.edit {
            putString(KEY_CELLULAR_QUALITY, quality.name)
        }
    }

    /**
     * Returns the configured quality for the active network.
     * Data Saver overrides the effective quality only; saved preferences
     * remain unchanged.
     *
     * Call this when the network type changes or before selecting a stream.
     */
    fun resolveActiveQuality(isWifiConnected: Boolean): AudioQuality {
        if (_dataSaverEnabled.value) {
            return AudioQuality.NORMAL
        }

        return if (isWifiConnected) {
            _wifiQuality.value
        } else {
            _cellularQuality.value
        }
    }

    private companion object {
        const val PREFS_NAME = "audio_quality_prefs"
        const val KEY_DATA_SAVER = "data_saver"
        const val KEY_WIFI_QUALITY = "wifi_quality"
        const val KEY_CELLULAR_QUALITY = "cellular_quality"
    }
}

@Composable
fun AudioSettingsScreen(
    settingsManager: AudioSettingsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel? = null
) {
    val colors = DriftTheme.colors
    val skyAccent = colors.accent
    val bgDark = colors.background
    val cardDark = colors.card
    val surfaceDark = colors.surface
    val primaryText = colors.textPrimary
    val secondaryText = colors.textSecondary
    val dividerLine = colors.border

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dataSaver by settingsManager.dataSaverEnabled.collectAsState()
    val wifiQuality by settingsManager.wifiQuality.collectAsState()
    val cellularQuality by settingsManager.cellularQuality.collectAsState()

    val isDarkMode = viewModel?.isDarkMode?.collectAsState()?.value ?: true
    val userName = viewModel?.userName?.collectAsState()?.value ?: ""
    val userAvatarUrl = viewModel?.userAvatarUrl?.collectAsState()?.value ?: ""

    var showWifiDialog by rememberSaveable { mutableStateOf(false) }
    var showCellDialog by rememberSaveable { mutableStateOf(false) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }

    // Update checking state
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

    BackHandler {
        when {
            showWifiDialog -> showWifiDialog = false
            showCellDialog -> showCellDialog = false
            else -> onBack()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 8.dp,
            bottom = 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClick = onBack
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Go back",
                        tint = skyAccent,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Settings",
                        color = skyAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Audio Quality",
                    color = primaryText,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Make every track sound right for you.",
                    color = secondaryText,
                    fontSize = 14.sp
                )
            }
        }

        item {
            SectionLabel("DATA USAGE", skyAccent)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardDark
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = skyAccent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DataUsage,
                            contentDescription = null,
                            tint = skyAccent,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Data Saver",
                            color = primaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Use Normal quality while streaming to reduce data usage.",
                            color = secondaryText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Switch(
                        checked = dataSaver,
                        onCheckedChange = settingsManager::setDataSaver,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF062333),
                            checkedTrackColor = skyAccent,
                            checkedBorderColor = skyAccent,
                            uncheckedThumbColor = Color(0xFFD4D4D8),
                            uncheckedTrackColor = Color(0xFF3F3F46),
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Enable Data Saver"
                        }
                    )
                }

                if (dataSaver) {
                    HorizontalDivider(
                        color = dividerLine,
                        thickness = 1.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(skyAccent.copy(alpha = 0.06f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = skyAccent,
                            modifier = Modifier.size(17.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "Data Saver is active. Normal quality will be used.",
                            color = skyAccent,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        item {
            SectionLabel("STREAMING QUALITY", skyAccent)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardDark
                )
            ) {
                Column {
                    QualitySelectionRow(
                        icon = Icons.Default.Wifi,
                        title = "Wi-Fi",
                        subtitle = if (dataSaver) {
                            "Normal · Data Saver active"
                        } else {
                            wifiQuality.label
                        },
                        detail = if (dataSaver) {
                            "Your saved preference is ${wifiQuality.label}"
                        } else {
                            wifiQuality.description
                        },
                        enabled = !dataSaver,
                        accentColor = skyAccent,
                        surfaceColor = surfaceDark,
                        textColor = primaryText,
                        subTextColor = secondaryText,
                        onClick = { showWifiDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 70.dp),
                        color = dividerLine,
                        thickness = 1.dp
                    )

                    QualitySelectionRow(
                        icon = Icons.Default.CellTower,
                        title = "Cellular",
                        subtitle = if (dataSaver) {
                            "Normal · Data Saver active"
                        } else {
                            cellularQuality.label
                        },
                        detail = if (dataSaver) {
                            "Your saved preference is ${cellularQuality.label}"
                        } else {
                            cellularQuality.description
                        },
                        enabled = !dataSaver,
                        accentColor = skyAccent,
                        surfaceColor = surfaceDark,
                        textColor = primaryText,
                        subTextColor = secondaryText,
                        onClick = { showCellDialog = true }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Higher quality can use more data. Actual audio quality also depends on the source and playback format.",
                color = secondaryText,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Additional Settings Sections when viewModel is present
        if (viewModel != null) {
            item {
                SectionLabel("PROFILE & ACCOUNT", skyAccent)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cardDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClick = { showProfileDialog = true })
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(surfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = userAvatarUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = skyAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName.ifBlank { "Drifter" },
                                color = primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Profile picture, display name & preferences",
                                color = secondaryText,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "›",
                            color = secondaryText,
                            fontSize = 27.sp
                        )
                    }
                }
            }

            item {
                SectionLabel("APPEARANCE", skyAccent)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cardDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = skyAccent.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = skyAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Theme Mode",
                                color = primaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = if (isDarkMode) "Dark (Pure Black OLED)" else "Light Mode",
                                color = secondaryText,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF062333),
                                checkedTrackColor = skyAccent,
                                checkedBorderColor = skyAccent
                            )
                        )
                    }
                }
            }

            item {
                SectionLabel("APP & STORAGE", skyAccent)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cardDark)
                ) {
                    Column {
                        // Check for updates row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isCheckingUpdate) {
                                        isCheckingUpdate = true
                                        updateMessage = null
                                        scope.launch {
                                            UpdaterManager.checkForUpdate(currentVersion)
                                                .onSuccess { info ->
                                                    isCheckingUpdate = false
                                                    if (info != null) {
                                                        updateDialogInfo = info
                                                    } else {
                                                        updateMessage = "You're using the latest version ($currentVersion)!"
                                                    }
                                                }
                                                .onFailure {
                                                    isCheckingUpdate = false
                                                    updateMessage = "Couldn't check updates: ${it.localizedMessage ?: "Network error"}"
                                                }
                                        }
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = surfaceDark,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = skyAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(13.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Drift Updates",
                                    color = primaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = if (isCheckingUpdate) "Checking GitHub..." else updateMessage ?: "Version $currentVersion · Tap to check",
                                    color = if (updateMessage != null) skyAccent else secondaryText,
                                    fontSize = 12.sp
                                )
                            }

                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = skyAccent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "›",
                                    color = secondaryText,
                                    fontSize = 27.sp
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 70.dp),
                            color = dividerLine,
                            thickness = 1.dp
                        )

                        // Clear search history row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.clearRecentSearches() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = surfaceDark,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = secondaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(13.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear Search History",
                                    color = primaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "Remove all cached search queries",
                                    color = secondaryText,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog && viewModel != null) {
        ProfileSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showWifiDialog) {
        QualityPickerDialog(
            title = "Wi-Fi quality",
            current = wifiQuality,
            accentColor = skyAccent,
            cardColor = cardDark,
            primaryText = primaryText,
            secondaryText = secondaryText,
            onSelect = {
                settingsManager.setWifiQuality(it)
                showWifiDialog = false
            },
            onDismiss = { showWifiDialog = false }
        )
    }

    if (showCellDialog) {
        QualityPickerDialog(
            title = "Cellular quality",
            current = cellularQuality,
            accentColor = skyAccent,
            cardColor = cardDark,
            primaryText = primaryText,
            secondaryText = secondaryText,
            onSelect = {
                settingsManager.setCellularQuality(it)
                showCellDialog = false
            },
            onDismiss = { showCellDialog = false }
        )
    }

    if (updateDialogInfo != null) {
        AlertDialog(
            onDismissRequest = { if (downloadProgress == null) updateDialogInfo = null },
            title = {
                Text(
                    text = "Update Available (${updateDialogInfo!!.versionName})",
                    color = primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "What's New:",
                        color = skyAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = updateDialogInfo!!.releaseNotes,
                        color = secondaryText,
                        fontSize = 12.sp,
                        maxLines = 8
                    )
                    if (downloadProgress != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress!! },
                            color = skyAccent,
                            trackColor = cardDark,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Downloading... ${(downloadProgress!! * 100).toInt()}%",
                            color = primaryText,
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
                        colors = ButtonDefaults.buttonColors(containerColor = skyAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Download & Install", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (downloadProgress == null) {
                    TextButton(onClick = { updateDialogInfo = null }) {
                        Text("Later", color = secondaryText)
                    }
                }
            },
            containerColor = cardDark,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String, accentColor: Color) {
    Text(
        text = text,
        color = accentColor,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = 3.dp,
            bottom = 10.dp
        )
    )
}

@Composable
private fun QualitySelectionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    detail: String,
    enabled: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = surfaceColor,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accentColor else subTextColor,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(Modifier.width(13.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = if (enabled) textColor else subTextColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = subtitle,
                color = if (enabled) accentColor else subTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = detail,
                color = subTextColor,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = "›",
            color = if (enabled) subTextColor else subTextColor.copy(alpha = 0.35f),
            fontSize = 27.sp
        )
    }
}

@Composable
private fun QualityPickerDialog(
    title: String,
    current: AudioQuality,
    accentColor: Color,
    cardColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onSelect: (AudioQuality) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = cardColor,
        titleContentColor = primaryText,
        textContentColor = secondaryText,
        title = {
            Column {
                Text(
                    text = title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "Choose your preferred stream quality.",
                    color = secondaryText,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioQuality.entries.forEach { quality ->
                    val selected = quality == current

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (selected) {
                                    accentColor.copy(alpha = 0.10f)
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSelect(quality)
                            }
                            .padding(
                                horizontal = 10.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelect(quality) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = secondaryText
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = quality.label,
                                color = primaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(3.dp))

                            Text(
                                text = "${quality.bitrateKbps} kbps",
                                color = if (selected) accentColor else secondaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(Modifier.height(3.dp))

                            Text(
                                text = quality.description,
                                color = secondaryText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
