package com.soundify.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MainScreen(viewModel: PlayerViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = remember(configuration.screenHeightDp, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }.coerceAtLeast(1f)
    }

    val coroutineScope = rememberCoroutineScope()
    // 0f = Fully Expanded (Top of screen), screenHeightPx = Fully Collapsed (Hidden at bottom)
    val sheetOffsetY = remember { Animatable(screenHeightPx) }

    // Keep bounds updated if screen size changes
    LaunchedEffect(screenHeightPx) {
        sheetOffsetY.updateBounds(0f, screenHeightPx)
        if (currentTrack == null) {
            sheetOffsetY.snapTo(screenHeightPx)
        }
    }

    fun expandPlayer(velocity: Float = 0f) {
        coroutineScope.launch {
            sheetOffsetY.animateTo(
                targetValue = 0f,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    fun collapsePlayer(velocity: Float = 0f) {
        coroutineScope.launch {
            sheetOffsetY.animateTo(
                targetValue = screenHeightPx,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Intercept system back button when full player is open
    val isPlayerExpanded by remember {
        derivedStateOf { sheetOffsetY.value < screenHeightPx * 0.5f }
    }
    BackHandler(enabled = isPlayerExpanded) {
        collapsePlayer()
    }

    // Shared 1:1 finger-tracking draggable state for both MiniPlayer and Full Player
    val playerDragState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val newOffset = (sheetOffsetY.value + delta).coerceIn(0f, screenHeightPx)
            sheetOffsetY.snapTo(newOffset)
        }
    }

    val colors = DriftTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
                    if (currentTrack != null) {
                        MiniPlayer(
                            viewModel = viewModel,
                            onClick = { expandPlayer() },
                            modifier = Modifier.draggable(
                                state = playerDragState,
                                orientation = Orientation.Vertical,
                                onDragStopped = { velocity ->
                                    if (velocity < -600f || sheetOffsetY.value < screenHeightPx * 0.75f) {
                                        expandPlayer(velocity)
                                    } else {
                                        collapsePlayer(velocity)
                                    }
                                }
                            )
                        )
                    }

                    NavigationBar(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary
                    ) {
                        NavigationBarItem(
                            selected = activeTab == "home",
                            onClick = { viewModel.setActiveTab("home") },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                indicatorColor = if (colors.isDark) Color(0xFF242429) else Color(0xFFE2E8F0),
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == "search",
                            onClick = { viewModel.setActiveTab("search") },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                indicatorColor = if (colors.isDark) Color(0xFF242429) else Color(0xFFE2E8F0),
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == "library",
                            onClick = { viewModel.setActiveTab("library") },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                            label = { Text("Library") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                indicatorColor = if (colors.isDark) Color(0xFF242429) else Color(0xFFE2E8F0),
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary
                            )
                        )
                    }
                }
            },
            containerColor = colors.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (activeTab) {
                    "home" -> HomeScreen(viewModel)
                    "search" -> SearchScreen(viewModel)
                    "library" -> LibraryScreen(viewModel)
                }
            }
        }

        // Interactive 1:1 Finger-Tracking Full Screen Player Sheet
        val isSheetVisible by remember {
            derivedStateOf { currentTrack != null && sheetOffsetY.value < screenHeightPx - 1f }
        }

        if (isSheetVisible) {
            // Subtle backdrop scrim that darkens as the sheet rises
            val scrimAlpha = (1f - (sheetOffsetY.value / screenHeightPx)).coerceIn(0f, 1f) * 0.7f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha }
                    .background(Color.Black)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(x = 0, y = sheetOffsetY.value.roundToInt()) }
                    .draggable(
                        state = playerDragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            // Fast flick down (velocity > 750) or dragged below 32% of screen -> collapse
                            if (velocity > 750f || (velocity >= -400f && sheetOffsetY.value > screenHeightPx * 0.32f)) {
                                collapsePlayer(velocity)
                            } else {
                                expandPlayer(velocity)
                            }
                        }
                    )
            ) {
                NowPlayingScreen(
                    viewModel = viewModel,
                    onDismiss = { collapsePlayer() }
                )
            }
        }

        statusMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp, start = 16.dp, end = 16.dp),
                containerColor = Color(0xFF242429),
                contentColor = Color.White,
                shape = RoundedCornerShape(10.dp),
                action = {
                    TextButton(onClick = { viewModel.dismissStatusMessage() }) {
                        Text("OK", color = Color(0xFF38BDF8))
                    }
                }
            ) {
                Text(msg)
            }
        }
    }
}
