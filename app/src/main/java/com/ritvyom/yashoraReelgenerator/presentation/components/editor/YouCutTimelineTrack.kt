package com.ritvyom.yashoraReelgenerator.presentation.components.editor

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.input.pointer.pointerInput
import com.ritvyom.yashoraReelgenerator.engine.timeline.IntelligentSnapEngine
import com.ritvyom.yashoraReelgenerator.engine.timeline.TimelineSnapPoint
import com.ritvyom.yashoraReelgenerator.engine.timeline.SnapPointType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize
import java.util.Locale

/**
 * YouCut-style multi-track Filmstrip Timeline Component:
 * - Big Round Orange (+) button to pick & import user's own videos/photos
 * - Unmute / Mute clip audio quick toggle
 * - Time readouts: current playhead time "0:00.0" and "Total 0:22.9"
 * - Chronological filmstrip track with thumbnails, duration badges, selected clip orange handles,
 *   and orange vertical playhead bar with top/bottom indicator tabs.
 */
@Composable
fun YouCutTimelineTrack(
    scenes: List<Scene>,
    selectedSceneIndex: Int,
    currentPlaybackTimeMs: Long,
    totalDurationSeconds: Int,
    isAudioMuted: Boolean,
    appLanguage: String,
    onSelectScene: (Int) -> Unit,
    onAddMediaClick: () -> Unit,
    onToggleMuteAudio: () -> Unit,
    onScrubTime: (Long) -> Unit,
    onSplitClip: (Int) -> Unit,
    onDeleteClip: (Int) -> Unit,
    onDurationChanged: ((clipIndex: Int, durationSeconds: Int) -> Unit)? = null,
    onDurationMsChanged: ((clipIndex: Int, durationMs: Long) -> Unit)? = null,
    isClipSelected: Boolean = false,
    onDeselectClip: (() -> Unit)? = null,
    audioTrackName: String? = null,
    audioTrackDurationMs: Long = 0L,
    audioTrackStartMs: Long = 0L,
    bgmVolume: Float = 1.0f,
    videoAudioVolume: Float = 1.0f,
    onAudioTrackClick: (() -> Unit)? = null,
    onAddAudioTrackClick: (() -> Unit)? = null,
    onOpenAudioMixer: (() -> Unit)? = null,
    onRemoveAudioTrack: (() -> Unit)? = null,
    onBgmVolumeChange: ((Float) -> Unit)? = null,
    onOpenTransitionManager: ((clipIndex: Int) -> Unit)? = null,
    onReplaceClip: ((clipIndex: Int) -> Unit)? = null,
    onSpeedAdjustClick: ((clipIndex: Int) -> Unit)? = null,
    onBeginTimelineGesture: ((actionName: String) -> Unit)? = null,
    onCommitTimelineGesture: (() -> Unit)? = null,
    onCancelTimelineGesture: (() -> Unit)? = null,
    voiceoverTrackName: String? = null,
    voiceoverDurationMs: Long = 0L,
    voiceoverVolume: Float = 1.0f,
    onVoiceoverTrackClick: (() -> Unit)? = null,
    onPlayVoiceoverPreview: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 2-Finger Pinch Zoom & 0.1-second Micro-Precision Scale (0.5x to 8.0x)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showZoomBadge by remember { mutableStateOf(false) }
    var lastZoomTime by remember { mutableLongStateOf(0L) }
    var activeDraggingClipIndex by remember { mutableStateOf<Int?>(null) }
    var activeDraggingDurationMs by remember { mutableLongStateOf(0L) }
    var isDraggingHandleRight by remember { mutableStateOf(false) }

    LaunchedEffect(lastZoomTime) {
        if (lastZoomTime > 0L) {
            kotlinx.coroutines.delay(1600)
            showZoomBadge = false
        }
    }

    // Calculate candidate snap points from clip boundaries for magnetic timeline snapping
    val snapPoints = remember(scenes) {
        val points = mutableListOf<TimelineSnapPoint>()
        var accMs = 0L
        for ((idx, sc) in scenes.withIndex()) {
            val durMs = sc.durationSeconds * 1000L
            points.add(TimelineSnapPoint(accMs * 1000L, SnapPointType.CLIP_START, "Clip #" + (idx + 1) + " Start"))
            accMs += durMs
            points.add(TimelineSnapPoint(accMs * 1000L, SnapPointType.CLIP_END, "Clip #" + (idx + 1) + " End"))
        }
        points
    }
    var isSnappingActive by remember { mutableStateOf(false) }
    var activeSnapLabel by remember { mutableStateOf<String?>(null) }

    // Format time readouts like YouCut: "0:00.0" and "Total 0:22.9"
    val currentSec = currentPlaybackTimeMs / 1000f
    val currentFormatted = remember(currentPlaybackTimeMs) {
        val min = (currentSec / 60).toInt()
        val sec = currentSec % 60
        String.format(Locale.US, "%d:%04.1f", min, sec)
    }

    val totalFormatted = remember(totalDurationSeconds) {
        val min = totalDurationSeconds / 60
        val sec = (totalDurationSeconds % 60).toFloat()
        String.format(Locale.US, "Total %d:%04.1f", min, sec)
    }

    val configuration = LocalConfiguration.current
    val isSmallWidth = configuration.screenWidthDp <= 340
    val isCompactHeight = configuration.screenHeightDp < 600
    val isWideScreen = configuration.screenWidthDp >= 600

    val timelineBoxHeight = when {
        isCompactHeight -> 72.dp
        isSmallWidth -> 76.dp
        isWideScreen -> 96.dp
        else -> 84.dp
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF101014))
            .padding(vertical = 4.dp)
            .testTag("youcut_timeline_track")
    ) {
        // Automated Visual Feedback: Consistency check between script segments and fetched media assets
        val missingVideoScenes = scenes.mapIndexedNotNull { idx, sc ->
            val hasVideo = sc.mediaType == "VIDEO" && !sc.mediaPath.isNullOrBlank() && sc.mediaFetchStatus != "FAILED"
            if (!hasVideo) idx to sc else null
        }
        if (missingVideoScenes.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF332005),
                border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag("timeline_consistency_warning_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timeline Audit: ${missingVideoScenes.size} scene(s) lack matched video",
                            color = Color(0xFFFFE082),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Match #${missingVideoScenes.first().first + 1} ↗",
                        color = Color(0xFFFFB300),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                onSelectScene(missingVideoScenes.first().first)
                                onReplaceClip?.invoke(missingVideoScenes.first().first)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Main Timeline Row: Left Actions (Big Plus & Audio Mute) + Right Filmstrip Track
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isSmallWidth) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Big Orange Circle (+) Button (YouCut Signature)
            val leftColumnWidth = if (isSmallWidth) 46.dp else 58.dp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(leftColumnWidth)
                    .padding(end = if (isSmallWidth) 4.dp else 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSmallWidth) 40.dp else 46.dp)
                        .clip(CircleShape)
                        .background(YouCutOrange)
                        .clickable(onClick = onAddMediaClick)
                        .testTag("timeline_add_media_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Media to Timeline".localize(appLanguage),
                        tint = Color.White,
                        modifier = Modifier.size(if (isSmallWidth) 24.dp else 28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // 2. Mute / Unmute Clip Audio Toggle Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onToggleMuteAudio)
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .testTag("timeline_mute_audio_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Toggle Mute".localize(appLanguage),
                        tint = YouCutOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isSmallWidth) (if (isAudioMuted) "Unmute" else "Mute")
                               else (if (isAudioMuted) "Unmute clip\naudio".localize(appLanguage) else "Mute clip\naudio".localize(appLanguage)),
                        color = YouCutOrange,
                        fontSize = if (isSmallWidth) 7.5.sp else 8.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 9.sp
                    )
                }
            }

            // 3. Filmstrip Timeline Area with orange playhead & 2-finger Pinch-to-Zoom
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(timelineBoxHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A22))
                    .border(1.dp, Color(0xFF2A2A38), RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1.0f) {
                                zoomScale = (zoomScale * zoom).coerceIn(0.5f, 8.0f)
                                showZoomBadge = true
                                lastZoomTime = System.currentTimeMillis()
                            }
                        }
                    }
                    .testTag("timeline_filmstrip_box")
            ) {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(scenes) { index, scene ->
                        val isSelected = index == selectedSceneIndex
                        val isClipFocused = isSelected && isClipSelected
                        val durationSecFloat = if (scene.durationMs > 0) scene.durationMs / 1000f else scene.durationSeconds.toFloat()
                        val baseWidthPerSec = 36.dp * zoomScale
                        val clipWidth = (baseWidthPerSec * durationSecFloat)
                            .coerceAtLeast(46.dp * zoomScale.coerceAtLeast(0.6f))

                        val hasNoVideo = scene.mediaType != "VIDEO" || scene.mediaPath.isNullOrBlank() || scene.mediaFetchStatus == "FAILED"

                        Box(
                            modifier = Modifier
                                .width(clipWidth)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF242430))
                                .border(
                                    width = if (isClipFocused) 2.5.dp else if (isSelected || hasNoVideo) 1.5.dp else 1.dp,
                                    color = if (isClipFocused) Color.White else if (isSelected) YouCutOrange else if (hasNoVideo) Color(0xFFFFB300) else Color(0xFF383848),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectScene(index) }
                                .testTag("timeline_scene_$index")
                        ) {
                            // Thumbnail Preview
                            val path = scene.mediaPath
                            if (!path.isNullOrEmpty()) {
                                AsyncImage(
                                    model = path,
                                    contentDescription = scene.subtitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF2A2A3E), Color(0xFF1E1E2C))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (scene.mediaType == "VIDEO") Icons.Default.Movie else Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Subtle gradient overlay for text readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.35f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.65f)
                                            )
                                        )
                                    )
                            )

                            // Top Info: Clip # and Type
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (scene.mediaType == "VIDEO") {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = YouCutOrange,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Surface(
                                            shape = RoundedCornerShape(2.dp),
                                            color = Color(0xFF3E2723)
                                        ) {
                                            Text(
                                                text = "NO VIDEO",
                                                color = Color(0xFFFFB300),
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                                            )
                                        }
                                    }

                                    if (scene.narrationText.isNotBlank() || !scene.customVoiceAudioPath.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Surface(
                                            shape = RoundedCornerShape(2.dp),
                                            color = Color(0xFF4A148C)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFD54F),
                                                    modifier = Modifier.size(8.dp)
                                                )
                                                Spacer(modifier = Modifier.width(1.dp))
                                                Text(
                                                    text = "VOICE",
                                                    color = Color.White,
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Replace Icon button directly on clip header
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .clickable {
                                            onSelectScene(index)
                                            onReplaceClip?.invoke(index)
                                        }
                                        .testTag("clip_header_replace_btn_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Replace Media",
                                        tint = YouCutOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Prominent Replace Action Chip on Focused Clip
                            if (isClipFocused) {
                                Surface(
                                    onClick = {
                                        onSelectScene(index)
                                        onReplaceClip?.invoke(index)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1B1428).copy(alpha = 0.92f),
                                    border = BorderStroke(1.dp, YouCutOrange),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .testTag("timeline_focused_replace_chip_$index")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Replace", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Bottom Left Info: Speed / Curve Badge (e.g. "⚡ Montage", "⚡ Bullet", "0.5x", "2x")
                            val isSpeedCurveActive = !scene.speedCurvePreset.equals("Linear", ignoreCase = true) && !scene.speedCurvePoints.isNullOrBlank()
                            val isSpeedModified = isSpeedCurveActive || (scene.speedMultiplier != 1.0f)
                            val speedBadgeText = if (isSpeedCurveActive) {
                                "⚡ ${scene.speedCurvePreset}"
                            } else if (scene.speedMultiplier != 1.0f) {
                                "${"%.2f".format(java.util.Locale.US, scene.speedMultiplier)}x"
                            } else {
                                "1x"
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isSpeedCurveActive -> Color(0xFF00C853)
                                            isSpeedModified -> YouCutOrange
                                            else -> Color.Black.copy(alpha = 0.75f)
                                        }
                                    )
                                    .clickable {
                                        onSelectScene(index)
                                        onSpeedAdjustClick?.invoke(index)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = speedBadgeText,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Bottom Info: Duration Badge (e.g. "3.4s" with 0.1s precision)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1fs", durationSecFloat),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Selected Clip Trim Handles (YouCut-style signature white handles with 0.1-second precision)
                            if (isClipFocused) {
                                // Left Trim Handle (Draggable with 1 finger)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(28.dp)
                                        .fillMaxHeight()
                                        .pointerInput(index, scene.durationMs, scene.durationSeconds, zoomScale) {
                                            detectHorizontalDragGestures(
                                                onDragStart = {
                                                    activeDraggingClipIndex = index
                                                    activeDraggingDurationMs = if (scene.durationMs > 0) scene.durationMs else scene.durationSeconds * 1000L
                                                    isDraggingHandleRight = false
                                                    onBeginTimelineGesture?.invoke("Trim Clip Start")
                                                },
                                                onDragEnd = {
                                                    activeDraggingClipIndex = null
                                                    onCommitTimelineGesture?.invoke()
                                                },
                                                onDragCancel = {
                                                    activeDraggingClipIndex = null
                                                    onCancelTimelineGesture?.invoke()
                                                },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val currentMs = if (scene.durationMs > 0) scene.durationMs else scene.durationSeconds * 1000L
                                                    val deltaMs = (-dragAmount * (1000f / (36f * zoomScale * 2.5f))).toLong()
                                                    val newMs = ((currentMs + deltaMs + 50L) / 100L * 100L).coerceIn(100L, 120_000L)
                                                    if (newMs != currentMs) {
                                                        activeDraggingDurationMs = newMs
                                                        onDurationMsChanged?.invoke(index, newMs)
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(7.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(modifier = Modifier.size(1.5.dp, 6.dp).background(Color(0xFF333333), RoundedCornerShape(1.dp)))
                                            Box(modifier = Modifier.size(1.5.dp, 6.dp).background(Color(0xFF333333), RoundedCornerShape(1.dp)))
                                        }
                                    }
                                }

                                // Right Trim Handle (Draggable with 1 finger to stretch/shorten with 0.1s precision)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(28.dp)
                                        .fillMaxHeight()
                                        .pointerInput(index, scene.durationMs, scene.durationSeconds, zoomScale) {
                                            detectHorizontalDragGestures(
                                                onDragStart = {
                                                    activeDraggingClipIndex = index
                                                    activeDraggingDurationMs = if (scene.durationMs > 0) scene.durationMs else scene.durationSeconds * 1000L
                                                    isDraggingHandleRight = true
                                                    onBeginTimelineGesture?.invoke("Trim Clip End")
                                                },
                                                onDragEnd = {
                                                    activeDraggingClipIndex = null
                                                    onCommitTimelineGesture?.invoke()
                                                },
                                                onDragCancel = {
                                                    activeDraggingClipIndex = null
                                                    onCancelTimelineGesture?.invoke()
                                                },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val currentMs = if (scene.durationMs > 0) scene.durationMs else scene.durationSeconds * 1000L
                                                    val deltaMs = (dragAmount * (1000f / (36f * zoomScale * 2.5f))).toLong()
                                                    val newMs = ((currentMs + deltaMs + 50L) / 100L * 100L).coerceIn(100L, 120_000L)
                                                    if (newMs != currentMs) {
                                                        activeDraggingDurationMs = newMs
                                                        onDurationMsChanged?.invoke(index, newMs)
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(7.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(modifier = Modifier.size(1.5.dp, 6.dp).background(Color(0xFF333333), RoundedCornerShape(1.dp)))
                                            Box(modifier = Modifier.size(1.5.dp, 6.dp).background(Color(0xFF333333), RoundedCornerShape(1.dp)))
                                        }
                                    }
                                }

                                // Live floating duration readout during finger dragging
                                if (activeDraggingClipIndex == index) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF1E1E28),
                                        border = BorderStroke(1.dp, YouCutOrange),
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "⏱️ %.1fs", activeDraggingDurationMs / 1000f),
                                            color = YouCutOrange,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Transition button between clips with accessible >=36dp touch target
                        if (index < scenes.size - 1) {
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 36.dp, minHeight = 44.dp)
                                    .clickable { onOpenTransitionManager?.invoke(index) }
                                    .testTag("timeline_transition_button_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF262634))
                                        .border(1.dp, YouCutOrange.copy(alpha = 0.7f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Animation,
                                        contentDescription = "Transition",
                                        tint = YouCutOrange,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Orange Playhead Bar running vertically through the timeline with drag & magnetic snapping
                val playheadColor = if (isSnappingActive) Color(0xFFFFD54F) else YouCutOrange
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(if (isSnappingActive) 3.dp else 2.dp)
                        .fillMaxHeight()
                        .background(playheadColor)
                        .pointerInput(totalDurationSeconds, snapPoints) {
                            detectDragGestures(
                                onDragStart = { isSnappingActive = false },
                                onDragEnd = { isSnappingActive = false; activeSnapLabel = null },
                                onDragCancel = { isSnappingActive = false; activeSnapLabel = null },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val totalMs = (totalDurationSeconds * 1000L).coerceAtLeast(1000L)
                                    val deltaMs = (dragAmount.x * (20L / zoomScale.coerceAtLeast(0.5f))).toLong()
                                    val rawMs = currentPlaybackTimeMs + deltaMs
                                    val candidateMs = ((rawMs + 50L) / 100L * 100L).coerceIn(0L, totalMs)
                                    val snapResult = IntelligentSnapEngine.findSnap(
                                        requestedTimeUs = candidateMs * 1000L,
                                        snapPoints = snapPoints,
                                        toleranceUs = 120_000L,
                                        enabled = true
                                    )
                                    if (snapResult.didSnap) {
                                        isSnappingActive = true
                                        activeSnapLabel = snapResult.snapPoint?.label
                                        onScrubTime(snapResult.snappedTimeUs / 1000L)
                                    } else {
                                        isSnappingActive = false
                                        activeSnapLabel = null
                                        onScrubTime(candidateMs)
                                    }
                                }
                            )
                        }
                ) {
                    // Top Handle Marker
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(if (isSnappingActive) 8.dp else 6.dp, if (isSnappingActive) 10.dp else 8.dp)
                            .background(playheadColor, RoundedCornerShape(2.dp))
                    )
                    // Bottom Handle Marker
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(if (isSnappingActive) 8.dp else 6.dp, if (isSnappingActive) 10.dp else 8.dp)
                            .background(playheadColor, RoundedCornerShape(2.dp))
                    )
                }

                // Snap indicator toast badge above playhead
                if (isSnappingActive && !activeSnapLabel.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF2E2000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🧲", fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = activeSnapLabel ?: "Snapped",
                                color = Color(0xFFFFD54F),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Floating Zoom Badge (when user pinches or changes zoom)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showZoomBadge && (!isSnappingActive || activeSnapLabel.isNullOrEmpty()),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF221534),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YouCutOrange)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔍", fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(Locale.US, "%.1fx Zoom • 0.1s Precision", zoomScale),
                                color = YouCutOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Dedicated Voiceover Narration Track Lane
        val hasVoiceover = !voiceoverTrackName.isNullOrEmpty() || scenes.any { it.narrationText.isNotBlank() || !it.customVoiceAudioPath.isNullOrBlank() }
        if (hasVoiceover) {
            val displayVoiceName = voiceoverTrackName ?: "🎙️ Voiceover Track (${scenes.count { it.narrationText.isNotBlank() || !it.customVoiceAudioPath.isNullOrBlank() }} clips active)"
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF231433))
                    .border(1.dp, Color(0xFFBA68C8).copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .clickable { onVoiceoverTrackClick?.invoke() }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Voiceover Track",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayVoiceName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF140822),
                        border = BorderStroke(0.6.dp, Color(0xFFBA68C8))
                    ) {
                        Text(
                            text = "VOICE: ${(voiceoverVolume * 100).toInt()}%",
                            color = Color(0xFFFFD54F),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (voiceoverDurationMs > 0L) {
                        Text(
                            text = String.format(Locale.US, "%.1fs", voiceoverDurationMs / 1000f),
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (onPlayVoiceoverPreview != null) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Preview Voiceover",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onPlayVoiceoverPreview.invoke() }
                        )
                    }
                }
            }
        }

        // 5. Multi-Track Background Music Management Lane
        if (!audioTrackName.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1B2838))
                    .border(1.dp, Color(0xFF29B6F6).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .clickable { onAudioTrackClick?.invoke() }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = audioTrackName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Independent BGM Volume Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F1E2E),
                        border = BorderStroke(0.6.dp, Color(0xFF29B6F6).copy(alpha = 0.7f)),
                        modifier = Modifier.clickable { onOpenAudioMixer?.invoke() ?: onAudioTrackClick?.invoke() }
                    ) {
                        Text(
                            text = "BGM: ${(bgmVolume * 100).toInt()}%",
                            color = Color(0xFF29B6F6),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = String.format(Locale.US, "%.1fs", audioTrackDurationMs / 1000f),
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Icon(
                        Icons.Default.Equalizer,
                        contentDescription = "Audio Mixer",
                        tint = Color(0xFF29B6F6),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onOpenAudioMixer?.invoke() ?: onAudioTrackClick?.invoke() }
                    )

                    if (onRemoveAudioTrack != null) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove Audio Track",
                            tint = Color(0xFFFF5252).copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRemoveAudioTrack.invoke() }
                        )
                    }
                }
            }
        } else if (onAddAudioTrackClick != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF161622))
                    .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAddAudioTrackClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Background Music (.mp3, .wav)".localize(appLanguage), color = Color(0xFF29B6F6), fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.clickable { onOpenAudioMixer?.invoke() ?: onAudioTrackClick?.invoke() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Equalizer, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("MIXER", color = YouCutOrange, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Time Readout Row: Left = "0:00.0", Center = Mixer & Zoom Pill, Right = "Total 0:22.9"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentFormatted,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quick Audio Mixer Shortcut Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1B2332),
                    border = BorderStroke(0.6.dp, Color(0xFF29B6F6).copy(alpha = 0.6f)),
                    modifier = Modifier.clickable { onOpenAudioMixer?.invoke() ?: onAudioTrackClick?.invoke() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Equalizer, contentDescription = "Audio Mixer", tint = Color(0xFF29B6F6), modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("MIXER", color = Color(0xFF29B6F6), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Timeline Zoom Pill & Precision Control (Zoom in for 0.1s micro-precision)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1B26))
                        .border(0.5.dp, Color(0xFF2E2E40), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                // Zoom Out Button (-)
                Text(
                    text = "−",
                    color = if (zoomScale > 0.6f) YouCutOrange else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            zoomScale = (zoomScale - 0.5f).coerceAtLeast(0.5f)
                            showZoomBadge = true
                            lastZoomTime = System.currentTimeMillis()
                        }
                        .padding(horizontal = 4.dp)
                )

                Text(
                    text = String.format(Locale.US, "%.1fx", zoomScale),
                    color = if (zoomScale > 1.2f) YouCutOrange else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable {
                            zoomScale = if (zoomScale > 1.2f) 1.0f else 3.0f
                            showZoomBadge = true
                            lastZoomTime = System.currentTimeMillis()
                        }
                        .padding(horizontal = 4.dp)
                )

                // Zoom In Button (+)
                Text(
                    text = "+",
                    color = if (zoomScale < 7.9f) YouCutOrange else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            zoomScale = (zoomScale + 0.5f).coerceAtMost(8.0f)
                            showZoomBadge = true
                            lastZoomTime = System.currentTimeMillis()
                        }
                        .padding(horizontal = 4.dp)
                )
            }
        }

        Text(
            text = totalFormatted,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
}
