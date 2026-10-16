package com.soundify.player.ui.glass

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ══════════════════════════════════════════════════════════════════════════
 *  DRIFT GLASS UI
 *  Apple Music-style mini player + now playing, Spotify-style library,
 *  liquid-glass surfaces. UI only: it never touches Track / PlaylistModel.
 *  Map your own models to GlassTrack / GlassPlayerState / GlassLibraryItem.
 * ══════════════════════════════════════════════════════════════════════════ */

/* ───────────────────────────── 1 · DESIGN TOKENS ───────────────────────── */

@Immutable
data class DriftGlassPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val card: Color,
    val accent: Color,
    val accentText: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val glassBase: Color,
    val glassOpacity: Float,
    val glassSheen: Float,
    val strokeHi: Color,
    val strokeLo: Color,
)

val DriftGlassDark = DriftGlassPalette(
    isDark = true,
    background = Color(0xFF121212),
    surface = Color(0xFF18181B),
    card = Color(0xFF222226),
    accent = Color(0xFF38BDF8),
    accentText = Color(0xFF38BDF8),
    onAccent = Color(0xFF04202E),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFA1A1AA),
    glassBase = Color(0xFF26262C),
    glassOpacity = 0.86f,
    glassSheen = 0.12f,
    strokeHi = Color.White.copy(alpha = 0.30f),
    strokeLo = Color.White.copy(alpha = 0.05f),
)

val DriftGlassLight = DriftGlassPalette(
    isDark = false,
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFEEF0F4),
    accent = Color(0xFF38BDF8),
    accentText = Color(0xFF0284C7),
    onAccent = Color(0xFF04202E),
    textPrimary = Color(0xFF0B0B0F),
    textSecondary = Color(0xFF6B7280),
    glassBase = Color.White,
    glassOpacity = 0.84f,
    glassSheen = 0.60f,
    strokeHi = Color.White.copy(alpha = 0.95f),
    strokeLo = Color.Black.copy(alpha = 0.10f),
)

val LocalDriftGlass = staticCompositionLocalOf { DriftGlassDark }

/** Edit this one function to bridge DriftTheme.colors into the glass palette. */
@Composable
fun rememberDriftGlassPalette(dark: Boolean = isSystemInDarkTheme()): DriftGlassPalette =
    if (dark) DriftGlassDark else DriftGlassLight

/* ───────────────────────────── 2 · UI MODELS ───────────────────────────── */

@Immutable
data class GlassTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSec: Int = 0,
    val tint: Color? = null,
)

enum class GlassRepeatMode { Off, All, One }

@Immutable
data class GlassPlayerState(
    val track: GlassTrack? = null,
    val isPlaying: Boolean = false,
    val positionSec: Float = 0f,
    val isLiked: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: GlassRepeatMode = GlassRepeatMode.Off,
    val volume: Float = 0.7f,
)

@Stable
class GlassPlayerActions(
    val onPlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onSeek: (Float) -> Unit = {},
    val onVolume: (Float) -> Unit = {},
    val onToggleLike: () -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onCycleRepeat: () -> Unit = {},
    val onOpenQueue: () -> Unit = {},
    val onMore: () -> Unit = {},
)

enum class GlassLibraryKind { Playlist, Album, Artist, Liked }

enum class GlassLibrarySort(val label: String) { Recents("Recents"), Alphabetical("Alphabetical") }

@Immutable
data class GlassLibraryItem(
    val id: String,
    val title: String,
    val kind: GlassLibraryKind,
    val detail: String? = null,
    val pinned: Boolean = false,
    val tint: Color? = null,
)

/** Plug Coil / Glide / your own loader in here. Receives the track and the size modifier. */
typealias GlassArtwork = @Composable (GlassTrack, Modifier) -> Unit

/* ───────────────────────────── 3 · VECTOR GLYPHS ───────────────────────── */

enum class Glyph {
    Play, Pause, Next, Previous, Shuffle, Repeat, RepeatOne, Heart, HeartFilled,
    Queue, More, Search, Plus, Close, Grid, Rows, Sort, Pin, Note, VolumeLow, VolumeHigh,
}

@Composable
fun GlassIcon(
    glyph: Glyph,
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp),
    strokeWidth: Float = 2f,
) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        translate((size.width - 24f * s) / 2f, (size.height - 24f * s) / 2f) {
            scale(scaleX = s, scaleY = s, pivot = Offset.Zero) {
                drawGlyph(glyph, tint, strokeWidth)
            }
        }
    }
}

private fun heartPath(): Path = Path().apply {
    moveTo(12f, 20.2f)
    cubicTo(5.5f, 15.6f, 3f, 12f, 3f, 8.6f)
    cubicTo(3f, 6f, 5f, 4.2f, 7.4f, 4.2f)
    cubicTo(9.2f, 4.2f, 11f, 5.2f, 12f, 7f)
    cubicTo(13f, 5.2f, 14.8f, 4.2f, 16.6f, 4.2f)
    cubicTo(19f, 4.2f, 21f, 6f, 21f, 8.6f)
    cubicTo(21f, 12f, 18.5f, 15.6f, 12f, 20.2f)
    close()
}

private fun DrawScope.drawGlyph(glyph: Glyph, c: Color, sw: Float) {
    val line = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val soften = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun fillSoft(path: Path) {
        drawPath(path, c, style = Fill)
        drawPath(path, c, style = soften)
    }
    fun stroke(block: Path.() -> Unit) = drawPath(Path().apply(block), c, style = line)

    when (glyph) {
        Glyph.Play -> fillSoft(Path().apply { moveTo(7.5f, 5f); lineTo(19f, 12f); lineTo(7.5f, 19f); close() })
        Glyph.Pause -> {
            drawRoundRect(c, Offset(6f, 4.5f), Size(4f, 15f), CornerRadius(1.4f))
            drawRoundRect(c, Offset(14f, 4.5f), Size(4f, 15f), CornerRadius(1.4f))
        }
        Glyph.Next -> {
            fillSoft(Path().apply { moveTo(5f, 5f); lineTo(15.5f, 12f); lineTo(5f, 19f); close() })
            drawRoundRect(c, Offset(17f, 5f), Size(2.8f, 14f), CornerRadius(1.2f))
        }
        Glyph.Previous -> {
            fillSoft(Path().apply { moveTo(19f, 5f); lineTo(8.5f, 12f); lineTo(19f, 19f); close() })
            drawRoundRect(c, Offset(4.2f, 5f), Size(2.8f, 14f), CornerRadius(1.2f))
        }
        Glyph.Shuffle -> {
            stroke { moveTo(3f, 7f); lineTo(7f, 7f); cubicTo(11f, 7f, 12f, 17f, 16f, 17f); lineTo(20f, 17f) }
            stroke { moveTo(3f, 17f); lineTo(7f, 17f); cubicTo(11f, 17f, 12f, 7f, 16f, 7f); lineTo(20f, 7f) }
            stroke { moveTo(17.5f, 14.5f); lineTo(20.5f, 17f); lineTo(17.5f, 19.5f) }
            stroke { moveTo(17.5f, 4.5f); lineTo(20.5f, 7f); lineTo(17.5f, 9.5f) }
        }
        Glyph.Repeat, Glyph.RepeatOne -> {
            stroke { moveTo(4f, 11f); lineTo(4f, 10f); quadraticBezierTo(4f, 7f, 7f, 7f); lineTo(20f, 7f) }
            stroke { moveTo(17f, 4f); lineTo(20f, 7f); lineTo(17f, 10f) }
            stroke { moveTo(20f, 13f); lineTo(20f, 14f); quadraticBezierTo(20f, 17f, 17f, 17f); lineTo(4f, 17f) }
            stroke { moveTo(7f, 14f); lineTo(4f, 17f); lineTo(7f, 20f) }
            if (glyph == Glyph.RepeatOne) {
                drawPath(
                    Path().apply { moveTo(10.8f, 10.9f); lineTo(12.3f, 10f); lineTo(12.3f, 14f) },
                    c,
                    style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
        Glyph.Heart -> drawPath(heartPath(), c, style = line)
        Glyph.HeartFilled -> fillSoft(heartPath())
        Glyph.Queue -> {
            drawLine(c, Offset(4f, 7f), Offset(15f, 7f), sw, StrokeCap.Round)
            drawLine(c, Offset(4f, 12f), Offset(15f, 12f), sw, StrokeCap.Round)
            drawLine(c, Offset(4f, 17f), Offset(10f, 17f), sw, StrokeCap.Round)
            fillSoft(Path().apply { moveTo(16.5f, 13.5f); lineTo(21f, 16.5f); lineTo(16.5f, 19.5f); close() })
        }
        Glyph.More -> {
            drawCircle(c, 1.7f, Offset(5f, 12f))
            drawCircle(c, 1.7f, Offset(12f, 12f))
            drawCircle(c, 1.7f, Offset(19f, 12f))
        }
        Glyph.Search -> {
            drawCircle(c, 6f, Offset(10.5f, 10.5f), style = line)
            drawLine(c, Offset(15f, 15f), Offset(20f, 20f), sw, StrokeCap.Round)
        }
        Glyph.Plus -> {
            drawLine(c, Offset(12f, 5f), Offset(12f, 19f), sw, StrokeCap.Round)
            drawLine(c, Offset(5f, 12f), Offset(19f, 12f), sw, StrokeCap.Round)
        }
        Glyph.Close -> {
            drawLine(c, Offset(6f, 6f), Offset(18f, 18f), sw, StrokeCap.Round)
            drawLine(c, Offset(18f, 6f), Offset(6f, 18f), sw, StrokeCap.Round)
        }
        Glyph.Grid -> {
            drawRoundRect(c, Offset(4f, 4f), Size(6.5f, 6.5f), CornerRadius(1.6f), style = line)
            drawRoundRect(c, Offset(13.5f, 4f), Size(6.5f, 6.5f), CornerRadius(1.6f), style = line)
            drawRoundRect(c, Offset(4f, 13.5f), Size(6.5f, 6.5f), CornerRadius(1.6f), style = line)
            drawRoundRect(c, Offset(13.5f, 13.5f), Size(6.5f, 6.5f), CornerRadius(1.6f), style = line)
        }
        Glyph.Rows -> {
            drawLine(c, Offset(4f, 6.5f), Offset(20f, 6.5f), sw, StrokeCap.Round)
            drawLine(c, Offset(4f, 12f), Offset(20f, 12f), sw, StrokeCap.Round)
            drawLine(c, Offset(4f, 17.5f), Offset(20f, 17.5f), sw, StrokeCap.Round)
        }
        Glyph.Sort -> {
            stroke { moveTo(8f, 19f); lineTo(8f, 5f) }
            stroke { moveTo(5f, 8f); lineTo(8f, 5f); lineTo(11f, 8f) }
            stroke { moveTo(16f, 5f); lineTo(16f, 19f) }
            stroke { moveTo(13f, 16f); lineTo(16f, 19f); lineTo(19f, 16f) }
        }
        Glyph.Pin -> {
            fillSoft(
                Path().apply {
                    moveTo(8.5f, 3.5f); lineTo(15.5f, 3.5f); lineTo(14.6f, 10f); lineTo(17.5f, 13f)
                    lineTo(6.5f, 13f); lineTo(9.4f, 10f); close()
                },
            )
            drawLine(c, Offset(12f, 13f), Offset(12f, 20.5f), sw, StrokeCap.Round)
        }
        Glyph.Note -> {
            drawCircle(c, 3.2f, Offset(8f, 17.5f))
            drawLine(c, Offset(11f, 17.5f), Offset(11f, 5f), sw, StrokeCap.Round)
            stroke { moveTo(11f, 5f); cubicTo(12.5f, 8f, 17f, 8f, 17f, 12.5f) }
        }
        Glyph.VolumeLow, Glyph.VolumeHigh -> {
            fillSoft(
                Path().apply {
                    moveTo(4f, 9.5f); lineTo(8f, 9.5f); lineTo(12.5f, 5.5f)
                    lineTo(12.5f, 18.5f); lineTo(8f, 14.5f); lineTo(4f, 14.5f); close()
                },
            )
            stroke { moveTo(15.8f, 9.5f); quadraticBezierTo(17.6f, 12f, 15.8f, 14.5f) }
            if (glyph == Glyph.VolumeHigh) stroke { moveTo(18f, 6.8f); quadraticBezierTo(21.5f, 12f, 18f, 17.2f) }
        }
    }
}

/* ─────────────────────── 4 · LIQUID GLASS PRIMITIVES ───────────────────── */

private fun Modifier.glassCore(
    shape: Shape,
    base: Color,
    opacity: Float,
    sheen: Float,
    strokeHi: Color,
    strokeLo: Color,
    elevation: Dp,
): Modifier {
    val top = base.copy(alpha = (opacity + 0.06f).coerceAtMost(1f))
    val bottom = base.copy(alpha = (opacity - 0.06f).coerceAtLeast(0f))
    var m: Modifier = this
    if (elevation > 0.dp) {
        m = m.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.30f),
            spotColor = Color.Black.copy(alpha = 0.40f),
        )
    }
    return m
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .drawBehind {
            // specular highlight, top-left
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = sheen), Color.Transparent),
                    center = Offset(size.width * 0.15f, 0f),
                    radius = size.maxDimension * 0.75f,
                ),
            )
            // soft inner glow, bottom edge
            drawRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.White.copy(alpha = sheen * 0.35f)))
        }
        .border(
            1.dp,
            Brush.linearGradient(listOf(strokeHi, strokeLo, strokeHi.copy(alpha = strokeHi.alpha * 0.5f))),
            shape,
        )
}

/** Theme-aware glass (follows DriftGlassPalette, dark + light). */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.Unspecified,
    elevation: Dp = 16.dp,
): Modifier {
    val p = LocalDriftGlass.current
    val base = if (tint == Color.Unspecified) p.glassBase else tint
    return glassCore(shape, base, p.glassOpacity, p.glassSheen, p.strokeHi, p.strokeLo, elevation)
}

/** White frosted glass for use on top of the immersive now-playing backdrop. */
fun Modifier.immersiveGlass(
    shape: Shape,
    tint: Color = Color.White,
    opacity: Float = 0.16f,
    elevation: Dp = 0.dp,
): Modifier = glassCore(
    shape = shape,
    base = tint,
    opacity = opacity,
    sheen = 0.20f,
    strokeHi = Color.White.copy(alpha = 0.50f),
    strokeLo = Color.White.copy(alpha = 0.06f),
    elevation = elevation,
)

/** Spring press-scale + click without ripple. */
@Composable
fun Modifier.bounceClick(
    scaleDown: Float = 0.92f,
    enabled: Boolean = true,
    description: String? = null,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "bounce",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
}

@Composable
private fun VSpace(h: Dp) = Spacer(Modifier.height(h))

@Composable
private fun HSpace(w: Dp) = Spacer(Modifier.width(w))

private fun formatTime(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val sec = s % 60
    return "${s / 60}:${if (sec < 10) "0" else ""}$sec"
}

/* ───────────────────────────── 5 · ARTWORK ─────────────────────────────── */

private fun artGradient(seed: String, tint: Color?): List<Color> {
    val h = (seed.hashCode() and 0x7FFFFFFF) % 360
    val a = tint ?: Color(android.graphics.Color.HSVToColor(floatArrayOf(h.toFloat(), 0.55f, 0.88f)))
    val b = Color(android.graphics.Color.HSVToColor(floatArrayOf(((h + 48) % 360).toFloat(), 0.72f, 0.42f)))
    return listOf(a, b)
}

/** Deterministic gradient placeholder so the UI looks finished even without cover art. */
@Composable
fun GeneratedArt(
    seed: String,
    modifier: Modifier = Modifier,
    glyph: Glyph? = null,
    label: String? = null,
    tint: Color? = null,
) {
    val colors = remember(seed, tint) { artGradient(seed, tint) }
    BoxWithConstraints(
        modifier.background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            label != null -> Text(
                label,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = (maxWidth.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
            )
            glyph != null -> GlassIcon(glyph, Color.White.copy(alpha = 0.92f), Modifier.size(maxWidth * 0.42f))
        }
    }
}

val DefaultGlassArtwork: GlassArtwork = { track, modifier ->
    GeneratedArt(seed = track.id + track.title, modifier = modifier, glyph = Glyph.Note, tint = track.tint)
}

@Composable
fun DefaultLibraryArtwork(item: GlassLibraryItem, modifier: Modifier = Modifier) {
    when (item.kind) {
        GlassLibraryKind.Liked -> Box(
            modifier.background(Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF38BDF8)))),
            contentAlignment = Alignment.Center,
        ) { GlassIcon(Glyph.HeartFilled, Color.White, Modifier.fillMaxSize(0.46f)) }
        GlassLibraryKind.Artist -> GeneratedArt(
            seed = item.id + item.title,
            modifier = modifier,
            label = item.title.take(1).uppercase(),
            tint = item.tint,
        )
        else -> GeneratedArt(seed = item.id + item.title, modifier = modifier, glyph = Glyph.Note, tint = item.tint)
    }
}

/* ───────────────────────────── 6 · SEEK BAR ────────────────────────────── */

/** Apple-style scrubber: thin bar that swells while dragging. Fractions are 0..1. */
@Composable
fun GlassSeekBar(
    fraction: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    trackColor: Color = Color.White.copy(alpha = 0.26f),
    idleHeight: Dp = 6.dp,
    activeHeight: Dp = 12.dp,
) {
    val change by rememberUpdatedState(onValueChange)
    val finish by rememberUpdatedState(onValueChangeFinished)
    var widthPx by remember { mutableStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val shown = if (dragging) dragValue else fraction.coerceIn(0f, 1f)
    val barHeight by animateDpAsState(
        targetValue = if (dragging) activeHeight else idleHeight,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "seekHeight",
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val f = (pos.x / widthPx).coerceIn(0f, 1f)
                    change(f)
                    finish(f)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { pos ->
                        dragValue = (pos.x / widthPx).coerceIn(0f, 1f)
                        dragging = true
                        change(dragValue)
                    },
                    onDragEnd = { dragging = false; finish(dragValue) },
                    onDragCancel = { dragging = false; finish(dragValue) },
                    onHorizontalDrag = { c, delta ->
                        c.consume()
                        dragValue = (dragValue + delta / widthPx).coerceIn(0f, 1f)
                        change(dragValue)
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(CircleShape)
                .background(trackColor),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .background(activeColor),
            )
        }
    }
}

/* ───────────────────────────── 7 · MINI PLAYER ─────────────────────────── */

/**
 * Floating liquid-glass mini player.
 * Tap or swipe up = expand · swipe left = next · swipe right = previous.
 */
@Composable
fun DriftMiniPlayer(
    state: GlassPlayerState,
    actions: GlassPlayerActions,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: GlassArtwork = DefaultGlassArtwork,
    palette: DriftGlassPalette = rememberDriftGlassPalette(),
) {
    val track = state.track ?: return
    CompositionLocalProvider(LocalDriftGlass provides palette) {
        MiniPlayerContent(track, state, actions, onExpand, modifier, artwork)
    }
}

@Composable
private fun MiniPlayerContent(
    track: GlassTrack,
    state: GlassPlayerState,
    actions: GlassPlayerActions,
    onExpand: () -> Unit,
    modifier: Modifier,
    artwork: GlassArtwork,
) {
    val p = LocalDriftGlass.current
    val scope = rememberCoroutineScope()
    val latestActions by rememberUpdatedState(actions)
    val latestExpand by rememberUpdatedState(onExpand)
    val swipe = remember { Animatable(0f) }
    val shape = RoundedCornerShape(28.dp)
    val progress = if (track.durationSec > 0) (state.positionSec / track.durationSec).coerceIn(0f, 1f) else 0f
    val tint = track.tint?.let { lerp(p.glassBase, it, 0.35f) } ?: Color.Unspecified

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(62.dp)
            .graphicsLayer {
                translationX = swipe.value * 0.4f
                alpha = 1f - (abs(swipe.value) / 900f).coerceAtMost(0.4f)
            }
            .bounceClick(0.97f, description = "Open now playing") { latestExpand() }
            .liquidGlass(shape, tint = tint, elevation = 18.dp)
            .pointerInput(Unit) {
                val threshold = 72.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val o = swipe.value
                        if (o <= -threshold) latestActions.onNext() else if (o >= threshold) latestActions.onPrevious()
                        scope.launch { swipe.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
                    },
                    onDragCancel = { scope.launch { swipe.animateTo(0f) } },
                    onHorizontalDrag = { c, dx ->
                        c.consume()
                        scope.launch { swipe.snapTo(swipe.value + dx) }
                    },
                )
            }
            .pointerInput(Unit) {
                var total = 0f
                val threshold = 36.dp.toPx()
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -threshold) latestExpand() },
                    onVerticalDrag = { c, dy -> c.consume(); total += dy },
                )
            },
    ) {
        // Colour bleed from the artwork (real blur on Android 12+, tint-only below).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            artwork(
                track,
                Modifier
                    .matchParentSize()
                    .graphicsLayer { scaleX = 2.2f; scaleY = 2.2f }
                    .blur(36.dp)
                    .alpha(0.42f),
            )
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 9.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artShape = RoundedCornerShape(11.dp)
            Box(
                Modifier
                    .size(44.dp)
                    .shadow(
                        6.dp, artShape,
                        ambientColor = Color.Black.copy(alpha = 0.3f),
                        spotColor = Color.Black.copy(alpha = 0.4f),
                    )
                    .clip(artShape),
            ) {
                Crossfade(targetState = track, label = "miniArt") { t -> artwork(t, Modifier.fillMaxSize()) }
            }
            HSpace(12.dp)
            AnimatedContent(
                targetState = track,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 3 }) togetherWith
                        (fadeOut(tween(140)) + slideOutVertically(tween(200)) { -it / 3 })
                },
                label = "miniMeta",
            ) { t ->
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        t.title,
                        color = p.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        t.artist,
                        color = p.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                Modifier
                    .size(46.dp)
                    .bounceClick(0.86f, description = if (state.isPlaying) "Pause" else "Play") {
                        latestActions.onPlayPause()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = state.isPlaying, animationSpec = tween(160), label = "miniPP") { playing ->
                    GlassIcon(if (playing) Glyph.Pause else Glyph.Play, p.textPrimary, Modifier.size(26.dp))
                }
            }
            Box(
                Modifier
                    .size(46.dp)
                    .bounceClick(0.86f, description = "Next track") { latestActions.onNext() },
                contentAlignment = Alignment.Center,
            ) { GlassIcon(Glyph.Next, p.textPrimary, Modifier.size(26.dp)) }
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 5.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(CircleShape)
                .background(p.textPrimary.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(p.accent),
            )
        }
    }
}

/* ───────────────────────────── 8 · NOW PLAYING ─────────────────────────── */

@Composable
private fun ImmersiveBackdrop(track: GlassTrack, artwork: GlassArtwork, modifier: Modifier = Modifier) {
    val p = LocalDriftGlass.current
    val base by animateColorAsState(track.tint ?: p.accent, tween(900), label = "backdropTint")
    val c2 = lerp(base, Color(0xFF7C3AED), 0.45f)
    val c3 = lerp(base, Color(0xFF0F172A), 0.35f)
    val transition = rememberInfiniteTransition(label = "aurora")
    val t1 by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse), label = "t1",
    )
    val t2 by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "t2",
    )
    val t3 by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "t3",
    )

    Box(modifier.background(Color(0xFF0A0A0C))) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            artwork(
                track,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.6f; scaleY = 1.6f }
                    .blur(72.dp)
                    .alpha(0.5f),
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun blob(color: Color, cx: Float, cy: Float, r: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.78f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
            }
            blob(base, w * (0.15f + 0.55f * t1), h * (0.16f + 0.10f * t2), w * 0.95f)
            blob(c2, w * (0.95f - 0.60f * t2), h * (0.50f + 0.12f * t1), w * 1.00f)
            blob(c3, w * (0.25f + 0.45f * t3), h * (0.92f - 0.15f * t2), w * 1.10f)
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.66f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun GlassCircleButton(glyph: Glyph, tint: Color, description: String, diameter: Dp = 36.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(diameter)
            .bounceClick(0.88f, description = description, onClick = onClick)
            .immersiveGlass(CircleShape, opacity = 0.18f),
        contentAlignment = Alignment.Center,
    ) { GlassIcon(glyph, tint, Modifier.size(diameter * 0.5f)) }
}

@Composable
private fun TransportButton(glyph: Glyph, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .bounceClick(0.86f, description = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { GlassIcon(glyph, Color.White, Modifier.size(40.dp)) }
}

@Composable
private fun PillToggle(glyph: Glyph, active: Boolean, description: String, onClick: () -> Unit) {
    val p = LocalDriftGlass.current
    val bg by animateColorAsState(if (active) p.accent else Color.Transparent, tween(220), label = "pillBg")
    val fg by animateColorAsState(if (active) p.onAccent else Color.White, tween(220), label = "pillFg")
    Box(
        Modifier
            .size(width = 64.dp, height = 44.dp)
            .bounceClick(0.9f, description = description, onClick = onClick)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) { GlassIcon(glyph, fg, Modifier.size(22.dp)) }
}

/**
 * Full-screen now playing. Pull down anywhere to dismiss.
 * Artwork shrinks while paused and springs back up on play, like Apple Music.
 * Always immersive (light-on-dark) regardless of app theme.
 */
@Composable
fun DriftNowPlayingScreen(
    state: GlassPlayerState,
    actions: GlassPlayerActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: GlassArtwork = DefaultGlassArtwork,
    palette: DriftGlassPalette = rememberDriftGlassPalette(),
) {
    val track = state.track ?: return
    CompositionLocalProvider(LocalDriftGlass provides palette) {
        NowPlayingContent(track, state, actions, onDismiss, modifier, artwork)
    }
}

@Composable
private fun NowPlayingContent(
    track: GlassTrack,
    state: GlassPlayerState,
    actions: GlassPlayerActions,
    onDismiss: () -> Unit,
    modifier: Modifier,
    artwork: GlassArtwork,
) {
    val p = LocalDriftGlass.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val latestDismiss by rememberUpdatedState(onDismiss)
    val dismissY = remember { Animatable(0f) }
    BackHandler { latestDismiss() }

    val duration = track.durationSec.coerceAtLeast(1)
    var scrub by remember { mutableStateOf<Float?>(null) }
    var clearJob by remember { mutableStateOf<Job?>(null) }
    val progress = (state.positionSec / duration).coerceIn(0f, 1f)
    val shown = scrub ?: progress
    val elapsed = (shown * duration).toInt()

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                val frac = (dismissY.value / 1600f).coerceIn(0f, 1f)
                translationY = dismissY.value
                scaleX = 1f - 0.05f * frac
                scaleY = 1f - 0.05f * frac
                shape = RoundedCornerShape((36f * frac).dp)
                clip = true
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { /* swallow touches so nothing behind the sheet reacts */ }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (dismissY.value > 160.dp.toPx()) {
                                dismissY.animateTo(size.height.toFloat(), tween(220))
                                latestDismiss()
                            } else {
                                dismissY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                            }
                        }
                    },
                    onDragCancel = { scope.launch { dismissY.animateTo(0f, spring()) } },
                    onVerticalDrag = { c, dy ->
                        c.consume()
                        scope.launch { dismissY.snapTo((dismissY.value + dy).coerceAtLeast(0f)) }
                    },
                )
            },
    ) {
        ImmersiveBackdrop(track, artwork, Modifier.matchParentSize())

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VSpace(10.dp)
            Box(
                Modifier
                    .size(width = 42.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.38f)),
            )

            // Artwork
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                val side = minOf(maxWidth, maxHeight)
                val artScale by animateFloatAsState(
                    targetValue = if (state.isPlaying) 1f else 0.84f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 220f),
                    label = "artScale",
                )
                val lift by animateDpAsState(
                    targetValue = if (state.isPlaying) 28.dp else 10.dp,
                    animationSpec = tween(400),
                    label = "artLift",
                )
                val artShape = RoundedCornerShape(18.dp)
                Box(
                    Modifier
                        .size(side)
                        .graphicsLayer { scaleX = artScale; scaleY = artScale }
                        .shadow(
                            lift, artShape,
                            ambientColor = Color.Black.copy(alpha = 0.5f),
                            spotColor = Color.Black.copy(alpha = 0.6f),
                        )
                        .clip(artShape),
                ) {
                    Crossfade(targetState = track, animationSpec = tween(350), label = "artFade") { t ->
                        artwork(t, Modifier.fillMaxSize())
                    }
                }
            }

            // Title + actions
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    VSpace(2.dp)
                    Text(
                        track.artist,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                HSpace(12.dp)
                GlassCircleButton(
                    glyph = if (state.isLiked) Glyph.HeartFilled else Glyph.Heart,
                    tint = if (state.isLiked) p.accent else Color.White,
                    description = if (state.isLiked) "Remove from liked" else "Add to liked",
                ) { actions.onToggleLike() }
                HSpace(10.dp)
                GlassCircleButton(Glyph.More, Color.White, "More options") { actions.onMore() }
            }

            VSpace(10.dp)

            // Scrubber
            GlassSeekBar(
                fraction = shown,
                onValueChange = { clearJob?.cancel(); scrub = it },
                onValueChangeFinished = { f ->
                    actions.onSeek(f * duration)
                    clearJob?.cancel()
                    clearJob = scope.launch { delay(450); scrub = null }
                },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(elapsed), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("-" + formatTime(duration - elapsed), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Transport
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransportButton(Glyph.Previous, "Previous track") { actions.onPrevious() }
                Box(
                    Modifier
                        .size(80.dp)
                        .bounceClick(0.88f, description = if (state.isPlaying) "Pause" else "Play") {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            actions.onPlayPause()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(targetState = state.isPlaying, animationSpec = tween(180), label = "npPP") { playing ->
                        GlassIcon(if (playing) Glyph.Pause else Glyph.Play, Color.White, Modifier.size(56.dp))
                    }
                }
                TransportButton(Glyph.Next, "Next track") { actions.onNext() }
            }

            // Volume
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassIcon(Glyph.VolumeLow, Color.White.copy(alpha = 0.6f), Modifier.size(20.dp))
                GlassSeekBar(
                    fraction = state.volume,
                    onValueChange = { actions.onVolume(it) },
                    onValueChangeFinished = { actions.onVolume(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    idleHeight = 5.dp,
                    activeHeight = 10.dp,
                )
                GlassIcon(Glyph.VolumeHigh, Color.White.copy(alpha = 0.6f), Modifier.size(20.dp))
            }

            VSpace(10.dp)

            // Liquid-glass toggle pill
            Row(
                Modifier
                    .immersiveGlass(CircleShape, opacity = 0.14f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PillToggle(Glyph.Shuffle, state.shuffle, "Shuffle") { actions.onToggleShuffle() }
                PillToggle(
                    glyph = if (state.repeat == GlassRepeatMode.One) Glyph.RepeatOne else Glyph.Repeat,
                    active = state.repeat != GlassRepeatMode.Off,
                    description = "Repeat",
                ) { actions.onCycleRepeat() }
                PillToggle(Glyph.Queue, false, "Up next") { actions.onOpenQueue() }
            }
            VSpace(14.dp)
        }
    }
}

/* ───────────────────────────── 9 · PLAYER HOST ─────────────────────────── */

/**
 * Drop this on top of your app content (same Box as your NavHost).
 * Shows the mini player while collapsed and the full sheet while expanded.
 * miniBottomPadding = bottom nav height + bottom inset so the mini player floats above it.
 */
@Composable
fun DriftPlayerHost(
    state: GlassPlayerState,
    actions: GlassPlayerActions,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    miniBottomPadding: Dp = 0.dp,
    artwork: GlassArtwork = DefaultGlassArtwork,
    palette: DriftGlassPalette = rememberDriftGlassPalette(),
) {
    val hasTrack = state.track != null
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = hasTrack && !expanded,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = miniBottomPadding),
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 380f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            DriftMiniPlayer(
                state = state,
                actions = actions,
                onExpand = { onExpandedChange(true) },
                artwork = artwork,
                palette = palette,
            )
        }
        AnimatedVisibility(
            visible = hasTrack && expanded,
            enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 320f)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200)),
        ) {
            DriftNowPlayingScreen(
                state = state,
                actions = actions,
                onDismiss = { onExpandedChange(false) },
                artwork = artwork,
                palette = palette,
            )
        }
    }
}

/* ───────────────────────────── 10 · LIBRARY ────────────────────────────── */

private val libraryChips = listOf(
    "Playlists" to GlassLibraryKind.Playlist,
    "Albums" to GlassLibraryKind.Album,
    "Artists" to GlassLibraryKind.Artist,
)

private fun GlassLibraryItem.subtitleText(): String {
    val label = when (kind) {
        GlassLibraryKind.Playlist, GlassLibraryKind.Liked -> "Playlist"
        GlassLibraryKind.Album -> "Album"
        GlassLibraryKind.Artist -> "Artist"
    }
    return if (detail.isNullOrBlank() || kind == GlassLibraryKind.Artist) label else "$label • $detail"
}

@Composable
fun EqualizerBars(active: Boolean, color: Color, modifier: Modifier = Modifier.size(14.dp)) {
    val t = rememberInfiniteTransition(label = "eq")
    val a by t.animateFloat(
        0.30f, 1f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a",
    )
    val b by t.animateFloat(
        0.20f, 0.9f, infiniteRepeatable(tween(410, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b",
    )
    val c by t.animateFloat(
        0.40f, 1f, infiniteRepeatable(tween(630, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c",
    )
    Canvas(modifier) {
        val w = size.width / 5f
        val hs = if (active) listOf(a, b, c) else listOf(0.35f, 0.6f, 0.45f)
        hs.forEachIndexed { i, h ->
            val bh = size.height * h
            drawRoundRect(color, Offset(i * 2f * w, size.height - bh), Size(w, bh), CornerRadius(w / 2f))
        }
    }
}

@Composable
private fun HeaderButton(glyph: Glyph, description: String, onClick: () -> Unit) {
    val p = LocalDriftGlass.current
    Box(
        Modifier
            .size(44.dp)
            .bounceClick(0.88f, description = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { GlassIcon(glyph, p.textPrimary, Modifier.size(24.dp)) }
}

@Composable
private fun LibraryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalDriftGlass.current
    val bg by animateColorAsState(if (selected) p.accent else p.card, tween(200), label = "chipBg")
    val fg by animateColorAsState(if (selected) p.onAccent else p.textPrimary, tween(200), label = "chipFg")
    Box(
        Modifier
            .height(34.dp)
            .bounceClick(0.94f, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

@Composable
private fun LibraryRow(
    item: GlassLibraryItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    art: @Composable (GlassLibraryItem, Modifier) -> Unit,
    onClick: () -> Unit,
) {
    val p = LocalDriftGlass.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val artShape: Shape = if (item.kind == GlassLibraryKind.Artist) CircleShape else RoundedCornerShape(6.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (pressed) p.textPrimary.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(60.dp)
                .clip(artShape),
        ) { art(item, Modifier.fillMaxSize()) }
        HSpace(14.dp)
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = if (isCurrent) p.accentText else p.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            VSpace(2.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.pinned) {
                    GlassIcon(Glyph.Pin, p.accentText, Modifier.size(14.dp))
                    HSpace(6.dp)
                }
                if (isCurrent) {
                    EqualizerBars(active = isPlaying, color = p.accentText, modifier = Modifier.size(14.dp))
                    HSpace(6.dp)
                }
                Text(
                    item.subtitleText(),
                    color = p.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LibraryTile(
    item: GlassLibraryItem,
    isCurrent: Boolean,
    art: @Composable (GlassLibraryItem, Modifier) -> Unit,
    onClick: () -> Unit,
) {
    val p = LocalDriftGlass.current
    val isArtist = item.kind == GlassLibraryKind.Artist
    val artShape: Shape = if (isArtist) CircleShape else RoundedCornerShape(8.dp)
    Column(
        Modifier.bounceClick(0.96f, onClick = onClick),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(artShape),
        ) { art(item, Modifier.fillMaxSize()) }
        VSpace(8.dp)
        Text(
            item.title,
            color = if (isCurrent) p.accentText else p.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            item.subtitleText(),
            color = p.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Spotify-style library: header, filter chips, sort + list/grid toggle,
 * pinned items first, live equalizer on the current item.
 * Pass items in "recents" order; Alphabetical is applied here.
 */
@Composable
fun DriftLibraryScreen(
    items: List<GlassLibraryItem>,
    onItemClick: (GlassLibraryItem) -> Unit,
    modifier: Modifier = Modifier,
    playingItemId: String? = null,
    isPlaying: Boolean = false,
    avatarLabel: String = "D",
    onAvatarClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onCreateClick: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    itemArtwork: @Composable (GlassLibraryItem, Modifier) -> Unit = { item, m -> DefaultLibraryArtwork(item, m) },
    palette: DriftGlassPalette = rememberDriftGlassPalette(),
) {
    CompositionLocalProvider(LocalDriftGlass provides palette) {
        LibraryContent(
            items, onItemClick, modifier, playingItemId, isPlaying, avatarLabel,
            onAvatarClick, onSearchClick, onCreateClick, bottomContentPadding, windowInsets, itemArtwork,
        )
    }
}

@Composable
private fun LibraryContent(
    items: List<GlassLibraryItem>,
    onItemClick: (GlassLibraryItem) -> Unit,
    modifier: Modifier,
    playingItemId: String?,
    isPlaying: Boolean,
    avatarLabel: String,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCreateClick: () -> Unit,
    bottomContentPadding: Dp,
    windowInsets: WindowInsets,
    itemArtwork: @Composable (GlassLibraryItem, Modifier) -> Unit,
) {
    val p = LocalDriftGlass.current
    var filter by rememberSaveable { mutableStateOf<GlassLibraryKind?>(null) }
    var sort by rememberSaveable { mutableStateOf(GlassLibrarySort.Recents) }
    var grid by rememberSaveable { mutableStateOf(false) }

    val visible = remember(items, filter, sort) {
        val f = filter
        val filtered = if (f == null) items else items.filter {
            it.kind == f || (f == GlassLibraryKind.Playlist && it.kind == GlassLibraryKind.Liked)
        }
        val ordered = if (sort == GlassLibrarySort.Alphabetical) filtered.sortedBy { it.title.lowercase() } else filtered
        ordered.sortedByDescending { it.pinned }
    }
    val listBottom = bottomContentPadding + 24.dp

    Column(
        modifier
            .fillMaxSize()
            .background(p.background)
            .background(
                Brush.verticalGradient(
                    colors = listOf(p.accent.copy(alpha = if (p.isDark) 0.20f else 0.16f), Color.Transparent),
                    startY = 0f,
                    endY = 1100f,
                ),
            )
            .windowInsetsPadding(windowInsets),
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .bounceClick(0.9f, description = "Profile", onClick = onAvatarClick)
                    .clip(CircleShape),
            ) {
                GeneratedArt(
                    seed = "avatar$avatarLabel",
                    modifier = Modifier.fillMaxSize(),
                    label = avatarLabel.take(1).uppercase(),
                    tint = p.accent,
                )
            }
            HSpace(12.dp)
            Text(
                "Your Library",
                color = p.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.weight(1f),
            )
            HeaderButton(Glyph.Search, "Search library", onSearchClick)
            HeaderButton(Glyph.Plus, "Create playlist", onCreateClick)
        }

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(libraryChips) { (label, kind) ->
                LibraryChip(label, selected = filter == kind) { filter = if (filter == kind) null else kind }
            }
        }

        // Sort + layout toggle
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .bounceClick(0.96f, description = "Change sort order") {
                        sort = if (sort == GlassLibrarySort.Recents) GlassLibrarySort.Alphabetical else GlassLibrarySort.Recents
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIcon(Glyph.Sort, p.textPrimary, Modifier.size(16.dp))
                HSpace(8.dp)
                Text(sort.label, color = p.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(40.dp)
                    .bounceClick(0.88f, description = if (grid) "Show as list" else "Show as grid") { grid = !grid },
                contentAlignment = Alignment.Center,
            ) { GlassIcon(if (grid) Glyph.Rows else Glyph.Grid, p.textPrimary, Modifier.size(20.dp)) }
        }

        // Content
        Crossfade(
            targetState = grid,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "libraryLayout",
        ) { isGrid ->
            if (isGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = listBottom),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    gridItems(visible, key = { it.id }) { item ->
                        LibraryTile(item, isCurrent = item.id == playingItemId, art = itemArtwork) { onItemClick(item) }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = listBottom)) {
                    items(visible, key = { it.id }) { item ->
                        LibraryRow(
                            item = item,
                            isCurrent = item.id == playingItemId,
                            isPlaying = isPlaying,
                            art = itemArtwork,
                        ) { onItemClick(item) }
                    }
                }
            }
        }
    }
}
