package com.soundify.player.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

private const val PREFS_NAME = "soundify_profile_prefs"
private const val KEY_AVATAR_PATH = "local_avatar_file_path"

/**
 * Remembers the user's saved local avatar across app restarts and device reboots.
 */
@Composable
fun rememberPersistentAvatarUri(): MutableState<Uri?> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val state = remember {
        val savedPath = prefs.getString(KEY_AVATAR_PATH, null)
        val initialUri = savedPath?.let { path ->
            val file = File(path)
            if (file.exists()) Uri.fromFile(file) else null
        }
        mutableStateOf(initialUri)
    }

    return remember {
        object : MutableState<Uri?> {
            override var value: Uri?
                get() = state.value
                set(newUri) {
                    state.value = newUri
                    prefs.edit().putString(KEY_AVATAR_PATH, newUri?.path).apply()
                }

            override fun component1(): Uri? = value
            override fun component2(): (Uri?) -> Unit = { value = it }
        }
    }
}

/**
 * Compact, neat profile avatar picker: displays the circular avatar centerpiece with
 * a sleek camera overlay badge, plus compact "Change Photo" / "Upload Photo" and "Remove" pills.
 */
@Composable
fun ProfileImagePicker(
    avatarUrl: String,
    displayName: String,
    onAvatarChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DriftTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri ->
        if (pickedUri != null) {
            isSaving = true
            scope.launch {
                val localUri = persistAvatarLocally(context, pickedUri)
                isSaving = false
                if (localUri != null) {
                    onAvatarChanged(localUri.toString())
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centerpiece Avatar Box
        Box(
            modifier = Modifier
                .size(92.dp)
                .clickable(enabled = !isSaving) {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Main circular image / monogram
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, colors.accent, CircleShape)
                    .background(colors.card),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.accent, Color(0xFF6366F1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(1).uppercase().ifBlank { "D" },
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isSaving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Bottom-Right Camera Pill Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .border(2.dp, colors.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change Photo",
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compact Actions Row: "Upload Photo" / "Change Photo" + "Remove"
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !isSaving) {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (avatarUrl.isNotBlank()) "Change Photo" else "Upload Photo",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (avatarUrl.isNotBlank() && !isSaving) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            scope.launch {
                                deleteLocalAvatars(context)
                                onAvatarChanged("")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Remove",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Copies the temporary gallery URI into private internal storage, center-crops and downscales
 * to 512x512 JPEG so it loads instantaneously and never loses read permissions after reboot.
 */
private suspend fun persistAvatarLocally(
    context: Context,
    sourceUri: Uri,
    maxSizePx: Int = 512
): Uri? = withContext(Dispatchers.IO) {
    try {
        // 1. Decode bounds first to avoid OOM on huge photos
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }

        var sampleSize = 1
        val minDim = min(boundsOptions.outWidth, boundsOptions.outHeight)
        while (minDim / (sampleSize * 2) >= maxSizePx) {
            sampleSize *= 2
        }

        // 2. Decode downsampled bitmap
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return@withContext null

        // 3. Center-crop square thumbnail
        val targetDim = min(maxSizePx, min(rawBitmap.width, rawBitmap.height))
        val squareBitmap = ThumbnailUtils.extractThumbnail(
            rawBitmap,
            targetDim,
            targetDim,
            ThumbnailUtils.OPTIONS_RECYCLE_INPUT
        )

        // 4. Remove old avatar files and write a fresh timestamped file (busts Coil cache)
        deleteLocalAvatars(context)
        val outFile = File(context.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { out ->
            squareBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        squareBitmap.recycle()

        Uri.fromFile(outFile)
    } catch (e: Exception) {
        null
    }
}

private suspend fun deleteLocalAvatars(context: Context) = withContext(Dispatchers.IO) {
    context.filesDir.listFiles()?.forEach { file ->
        if (file.name.startsWith("profile_avatar_")) {
            file.delete()
        }
    }
}
