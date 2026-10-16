package com.soundify.player.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

private val SkyBlue = Color(0xFF38BDF8)
private val AvatarBg = Color(0xFF1E1E1E)

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
 * Tapping this component opens the native local gallery picker immediately.
 * No random avatars, no web links—just local photos from the user's device.
 * Automatically downscales and persists chosen image in private app storage.
 */
@Composable
fun ProfileImagePicker(
    currentImageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    onImageRemoved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    // Native modern Android photo picker contract
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isSaving = true
                val localUri = persistAvatarLocally(context, uri)
                isSaving = false
                if (localUri != null) {
                    onImageSelected(localUri)
                } else {
                    onImageSelected(uri)
                }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isSaving) {
                // Launch gallery picker filtered for images only
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Circle (60dp)
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(AvatarBg),
            contentAlignment = Alignment.Center
        ) {
            if (currentImageUri != null) {
                AsyncImage(
                    model = currentImageUri,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default User",
                    tint = SkyBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SkyBlue,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Profile Picture",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isSaving) "Saving to local storage..." else "Tap to change from gallery",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp
            )
        }

        if (currentImageUri != null && onImageRemoved != null && !isSaving) {
            IconButton(
                onClick = {
                    scope.launch {
                        deleteLocalAvatars(context)
                        onImageRemoved()
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Photo",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Convenience overload for callers passing avatarUrl string.
 */
@Composable
fun ProfileImagePicker(
    avatarUrl: String,
    displayName: String,
    onAvatarChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uri = remember(avatarUrl) {
        if (avatarUrl.isBlank()) null
        else if (avatarUrl.startsWith("/")) Uri.fromFile(File(avatarUrl))
        else Uri.parse(avatarUrl)
    }
    ProfileImagePicker(
        currentImageUri = uri,
        onImageSelected = { onAvatarChanged(it.toString()) },
        modifier = modifier,
        onImageRemoved = { onAvatarChanged("") }
    )
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
