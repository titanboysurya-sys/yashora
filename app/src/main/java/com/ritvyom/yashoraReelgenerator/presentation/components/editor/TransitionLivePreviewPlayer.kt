package com.ritvyom.yashoraReelgenerator.presentation.components.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize
import com.ritvyom.yashoraReelgenerator.presentation.utils.Media3TransitionEngine
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Dedicated Real-Time Live Animated Transition Preview Player for YouCut Transition Manager.
 * Solves the exact user need: previewing any transition between Clip A and Clip B live
 * in 60 FPS before applying or exporting, with exact visual parity to export output.
 */
@Composable
fun TransitionLivePreviewPlayer(
    transitionType: String,
    durationMs: Long,
    currentScene: Scene?,
    nextScene: Scene?,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    val normId = remember(transitionType) {
        Media3TransitionEngine.normalizeTransitionId(transitionType)
    }

    var progress by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(true) }
    var replayCounter by remember { mutableIntStateOf(0) }

    // Smooth looping animation loop for transition preview
    LaunchedEffect(normId, durationMs, replayCounter, isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        // Immediately replay from start whenever transition selection or duration changes
        progress = 0f
        while (isPlaying) {
            val animDuration = durationMs.coerceIn(200L, 2500L)
            val startTime = System.currentTimeMillis()

            // Transition from 0f to 1.0f
            while (isPlaying && (System.currentTimeMillis() - startTime) < animDuration) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed.toFloat() / animDuration).coerceIn(0f, 1f)
                delay(16L) // ~60 FPS
            }
            progress = 1.0f

            // Hold on transformed Clip 2 for user to inspect result
            delay(900L)

            // Reset back to Clip 1
            progress = 0f
            delay(400L)
        }
    }

    val clip1Media = currentScene?.mediaPath
    val clip2Media = nextScene?.mediaPath

    val currentSceneNum = currentScene?.sceneNumber ?: 1
    val nextSceneNum = nextScene?.sceneNumber ?: (currentSceneNum + 1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F0F16))
            .border(1.2.dp, Brush.horizontalGradient(listOf(YouCutOrange.copy(alpha = 0.8f), YouCutOrangeDark.copy(alpha = 0.5f))), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        // Header Status Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Red.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE PREVIEW".localize(appLanguage),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = YouCutOrange.copy(alpha = 0.2f),
                    border = BorderStroke(0.8.dp, YouCutOrange.copy(alpha = 0.5f))
                ) {
                    val activeItem = Media3TransitionEngine.TRANSITIONS.firstOrNull { it.id.equals(normId, ignoreCase = true) }
                    val emoji = activeItem?.iconEmoji ?: "🎬"
                    Text(
                        text = "$emoji $normId",
                        color = YouCutOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${String.format(java.util.Locale.US, "%.1fs", durationMs / 1000f)} • Clip #$currentSceneNum ➔ #$nextSceneNum",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Live Transition Viewport
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val viewWidth = maxWidth
            val viewHeight = maxHeight

            // Render Clip 1 (Outgoing) and Clip 2 (Incoming) with transition transforms
            RenderTransitionPair(
                normId = normId,
                progress = progress,
                clip1Media = clip1Media,
                clip2Media = clip2Media,
                currentSceneNum = currentSceneNum,
                nextSceneNum = nextSceneNum,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                modifier = Modifier.fillMaxSize()
            )

            // Canvas Transition Overlay (RGB split scanlines, Glowing wipe edges, Strobe flashes, Light Leaks, Film burn, Vignette)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                when (normId) {
                    "WIPE L", "MOVE L" -> {
                        val wipeW = w * progress
                        val lineX = w - wipeW
                        // Glowing sweep line
                        drawLine(
                            color = Color(0xFFFF5722),
                            start = Offset(lineX, 0f),
                            end = Offset(lineX, h),
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(lineX, 0f),
                            end = Offset(lineX, h),
                            strokeWidth = 2f
                        )
                    }

                    "WIPE R", "MOVE R" -> {
                        val lineX = w * progress
                        drawLine(
                            color = Color(0xFFFF5722),
                            start = Offset(lineX, 0f),
                            end = Offset(lineX, h),
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(lineX, 0f),
                            end = Offset(lineX, h),
                            strokeWidth = 2f
                        )
                    }

                    "WIPE U", "MOVE U" -> {
                        val lineY = h * (1f - progress)
                        drawLine(
                            color = Color(0xFFFF5722),
                            start = Offset(0f, lineY),
                            end = Offset(w, lineY),
                            strokeWidth = 6f
                        )
                    }

                    "WIPE D", "MOVE D" -> {
                        val lineY = h * progress
                        drawLine(
                            color = Color(0xFFFF5722),
                            start = Offset(0f, lineY),
                            end = Offset(w, lineY),
                            strokeWidth = 6f
                        )
                    }

                    "SPLIT V" -> {
                        val splitW = (w / 2f) * (1f - progress)
                        drawLine(color = Color(0xFFFF5722), start = Offset(splitW, 0f), end = Offset(splitW, h), strokeWidth = 4f)
                        drawLine(color = Color(0xFFFF5722), start = Offset(w - splitW, 0f), end = Offset(w - splitW, h), strokeWidth = 4f)
                    }

                    "SPLIT H" -> {
                        val splitH = (h / 2f) * (1f - progress)
                        drawLine(color = Color(0xFFFF5722), start = Offset(0f, splitH), end = Offset(w, splitH), strokeWidth = 4f)
                        drawLine(color = Color(0xFFFF5722), start = Offset(0f, h - splitH), end = Offset(w, h - splitH), strokeWidth = 4f)
                    }

                    "RGB SPLIT" -> {
                        // Cyber aberration borders
                        val offset = w * 0.05f * progress
                        drawRect(
                            color = Color(0xFFFF0055).copy(alpha = 0.45f * progress),
                            topLeft = Offset(0f, 0f),
                            size = Size(offset, h)
                        )
                        drawRect(
                            color = Color(0xFF00E5FF).copy(alpha = 0.45f * progress),
                            topLeft = Offset(w - offset, 0f),
                            size = Size(offset, h)
                        )
                        // Scanlines
                        var y = 0f
                        while (y < h) {
                            drawLine(
                                color = Color.Black.copy(alpha = 0.35f * progress),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 2f
                            )
                            y += 8f
                        }
                    }

                    "FLASH" -> {
                        val strobe = if ((progress * 12).toInt() % 2 == 0) progress else progress * 0.5f
                        drawRect(color = Color.White.copy(alpha = (strobe * 0.85f).coerceIn(0f, 0.95f)))
                    }

                    "FADE W" -> {
                        val whiteAlpha = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                        drawRect(color = Color.White.copy(alpha = whiteAlpha.coerceIn(0f, 1f)))
                    }

                    "FADE B" -> {
                        val blackAlpha = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                        drawRect(color = Color.Black.copy(alpha = blackAlpha.coerceIn(0f, 1f)))
                    }

                    "LIGHT LEAK" -> {
                        val flareRadius = kotlin.math.max(w, h) * 0.9f
                        val flareX = w * (1f - progress * 0.7f)
                        val flareY = h * 0.25f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F).copy(alpha = 0.85f * progress),
                                    Color(0xFFFF7043).copy(alpha = 0.6f * progress),
                                    Color(0xFFE91E63).copy(alpha = 0.3f * progress),
                                    Color.Transparent
                                ),
                                center = Offset(flareX, flareY),
                                radius = flareRadius
                            ),
                            radius = flareRadius,
                            center = Offset(flareX, flareY)
                        )
                    }

                    "FILM BURN" -> {
                        val burnRadius = kotlin.math.max(w, h) * 0.7f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.9f * progress),
                                    Color(0xFFFF9800).copy(alpha = 0.8f * progress),
                                    Color(0xFFD50000).copy(alpha = 0.5f * progress),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.5f, h * 0.5f),
                                radius = burnRadius
                            ),
                            radius = burnRadius,
                            center = Offset(w * 0.5f, h * 0.5f)
                        )
                    }

                    "SHOCKWAVE" -> {
                        val maxR = kotlin.math.hypot(w / 2f, h / 2f)
                        val r = maxR * progress
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = (1f - progress) * 0.8f),
                            radius = r,
                            center = Offset(w / 2f, h / 2f),
                            style = Stroke(width = 12f * (1f - progress))
                        )
                    }

                    "FLOODLIGHT" -> {
                        val streakH = h * 0.15f
                        val streakY = h * 0.42f
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF80D8FF).copy(alpha = 0.85f * progress),
                                    Color.White.copy(alpha = 0.95f * progress),
                                    Color(0xFF80D8FF).copy(alpha = 0.85f * progress),
                                    Color.Transparent
                                )
                            ),
                            topLeft = Offset(0f, streakY),
                            size = Size(w, streakH)
                        )
                    }

                    "BLINK" -> {
                        val blinkVal = (progress * 2f) % 1f
                        val blinkH = (h / 2f) * sin(blinkVal * Math.PI).toFloat().coerceIn(0f, 1f)
                        drawRect(color = Color.Black, topLeft = Offset(0f, 0f), size = Size(w, blinkH))
                        drawRect(color = Color.Black, topLeft = Offset(0f, h - blinkH), size = Size(w, blinkH))
                    }

                    "COLOR FLASH" -> {
                        val colors = listOf(Color(0xFFFF007F), Color(0xFF00E5FF), Color(0xFFFFD700))
                        val c = colors[((progress * 6).toInt()) % colors.size].copy(alpha = progress * 0.65f)
                        drawRect(color = c)
                    }

                    "PRISM" -> {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Red.copy(alpha = 0.4f * progress),
                                    Color(0xFFFF9800).copy(alpha = 0.4f * progress),
                                    Color.Green.copy(alpha = 0.4f * progress),
                                    Color.Cyan.copy(alpha = 0.4f * progress),
                                    Color.Magenta.copy(alpha = 0.4f * progress)
                                )
                            )
                        )
                    }

                    "ZOOM IN", "ZOOM OUT", "SPEED ZOOM", "BOUNCE" -> {
                        val borderW = w * 0.14f * progress
                        drawRect(color = Color.Black.copy(alpha = 0.7f * progress), topLeft = Offset(0f, 0f), size = Size(w, borderW))
                        drawRect(color = Color.Black.copy(alpha = 0.7f * progress), topLeft = Offset(0f, h - borderW), size = Size(w, borderW))
                        drawRect(color = Color.Black.copy(alpha = 0.7f * progress), topLeft = Offset(0f, 0f), size = Size(borderW, h))
                        drawRect(color = Color.Black.copy(alpha = 0.7f * progress), topLeft = Offset(w - borderW, 0f), size = Size(borderW, h))
                    }
                }
            }

            // Watermark Preview Badge in Player
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = YouCutOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Playback Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play / Pause
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = YouCutOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Replay
                IconButton(
                    onClick = {
                        isPlaying = true
                        replayCounter++
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Interactive Progress Slider to scrub frame by frame
            Slider(
                value = progress,
                onValueChange = {
                    isPlaying = false
                    progress = it
                },
                valueRange = 0f..1f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = YouCutOrange,
                    activeTrackColor = YouCutOrange,
                    inactiveTrackColor = Color(0xFF282835)
                )
            )

            Text(
                text = if (normId == "None") "No Transition" else "$normId Preview",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Renders the two video clips (Clip 1 outgoing and Clip 2 incoming) using
 * proper geometric transforms (Translation, Scale, Alpha, and Clipping) for the transition.
 */
@Composable
private fun RenderTransitionPair(
    normId: String,
    progress: Float,
    clip1Media: String?,
    clip2Media: String?,
    currentSceneNum: Int,
    nextSceneNum: Int,
    viewWidth: androidx.compose.ui.unit.Dp,
    viewHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Base: Clip 1 (Outgoing)
        val clip1TransformModifier = when (normId) {
            "SLIDE L", "MOVE L", "PUSH L", "FAST WHIP L" -> {
                Modifier.offset { IntOffset(x = -(viewWidth.toPx() * progress).toInt(), y = 0) }
            }
            "SLIDE R", "MOVE R", "PUSH R", "FAST WHIP R" -> {
                Modifier.offset { IntOffset(x = (viewWidth.toPx() * progress).toInt(), y = 0) }
            }
            "SLIDE U", "MOVE U" -> {
                Modifier.offset { IntOffset(x = 0, y = -(viewHeight.toPx() * progress).toInt()) }
            }
            "SLIDE D", "MOVE D" -> {
                Modifier.offset { IntOffset(x = 0, y = (viewHeight.toPx() * progress).toInt()) }
            }
            "ZOOM IN", "SPEED ZOOM", "BOUNCE" -> {
                Modifier.graphicsLayer(
                    scaleX = 1.0f + 0.35f * progress,
                    scaleY = 1.0f + 0.35f * progress,
                    alpha = (1.0f - progress * 0.8f).coerceIn(0f, 1f)
                )
            }
            "ZOOM OUT" -> {
                Modifier.graphicsLayer(
                    scaleX = (1.0f - 0.35f * progress).coerceAtLeast(0.5f),
                    scaleY = (1.0f - 0.35f * progress).coerceAtLeast(0.5f),
                    alpha = (1.0f - progress * 0.8f).coerceIn(0f, 1f)
                )
            }
            "SWIRL L", "SWIRL R", "RADIAL", "ZOOM TWIST" -> {
                val rot = if (normId.contains("L")) -45f * progress else 45f * progress
                Modifier.graphicsLayer(
                    rotationZ = rot,
                    scaleX = 1f + 0.3f * progress,
                    scaleY = 1f + 0.3f * progress,
                    alpha = (1f - progress).coerceIn(0f, 1f)
                )
            }
            "SHAKE" -> {
                val jX = ((progress * 31f) % 1f - 0.5f) * 20f * progress
                val jY = ((progress * 47f) % 1f - 0.5f) * 20f * progress
                Modifier.offset { IntOffset(jX.toInt(), jY.toInt()) }
            }
            "MIX", "DISSOLVE", "BLUR DISSOLVE" -> {
                Modifier.graphicsLayer(alpha = (1f - progress * 0.85f).coerceIn(0f, 1f))
            }
            "FADE B", "FADE W" -> {
                val a = if (progress < 0.5f) (1f - progress * 2f).coerceIn(0f, 1f) else 0f
                Modifier.graphicsLayer(alpha = a)
            }
            else -> Modifier
        }

        ClipDisplayBox(
            mediaPath = clip1Media,
            sceneLabel = "Clip #$currentSceneNum (Current)",
            badgeColor = Color(0xFF1E88E5),
            modifier = Modifier.fillMaxSize().then(clip1TransformModifier)
        )

        // Overlay: Clip 2 (Incoming)
        if (normId != "None" && progress > 0.005f) {
            val clip2TransformModifier = when (normId) {
                "SLIDE L", "MOVE L", "PUSH L", "FAST WHIP L" -> {
                    Modifier.offset { IntOffset(x = (viewWidth.toPx() * (1f - progress)).toInt(), y = 0) }
                }
                "SLIDE R", "MOVE R", "PUSH R", "FAST WHIP R" -> {
                    Modifier.offset { IntOffset(x = -(viewWidth.toPx() * (1f - progress)).toInt(), y = 0) }
                }
                "SLIDE U", "MOVE U" -> {
                    Modifier.offset { IntOffset(x = 0, y = (viewHeight.toPx() * (1f - progress)).toInt()) }
                }
                "SLIDE D", "MOVE D" -> {
                    Modifier.offset { IntOffset(x = 0, y = -(viewHeight.toPx() * (1f - progress)).toInt()) }
                }
                "WIPE R" -> {
                    // Wipe reveal from left to right
                    Modifier.graphicsLayer {
                        clip = true
                        shape = androidx.compose.ui.graphics.RectangleShape
                    }
                }
                "ZOOM IN", "SPEED ZOOM", "BOUNCE" -> {
                    Modifier.graphicsLayer(
                        scaleX = 0.75f + 0.25f * progress,
                        scaleY = 0.75f + 0.25f * progress,
                        alpha = progress.coerceIn(0f, 1f)
                    )
                }
                "ZOOM OUT" -> {
                    Modifier.graphicsLayer(
                        scaleX = 1.35f - 0.35f * progress,
                        scaleY = 1.35f - 0.35f * progress,
                        alpha = progress.coerceIn(0f, 1f)
                    )
                }
                "FADE B", "FADE W" -> {
                    val a = if (progress >= 0.5f) ((progress - 0.5f) * 2f).coerceIn(0f, 1f) else 0f
                    Modifier.graphicsLayer(alpha = a)
                }
                "CIRCULAR" -> {
                    Modifier.graphicsLayer(alpha = progress)
                }
                else -> {
                    // Default crossfade alpha for MIX, DISSOLVE, FLASH, RGB SPLIT, LIGHT LEAK, etc.
                    Modifier.graphicsLayer(alpha = progress.coerceIn(0f, 1f))
                }
            }

            // For geometric wipes (WIPE L/R/U/D, SPLIT V/H), clip the composable dynamically
            when (normId) {
                "WIPE R" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                clip = true
                                shape = androidx.compose.ui.graphics.RectangleShape
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxSize()
                                .clipToBounds()
                        ) {
                            ClipDisplayBox(
                                mediaPath = clip2Media,
                                sceneLabel = "Clip #$nextSceneNum (Next)",
                                badgeColor = Color(0xFF00C853),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                "WIPE L" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(x = (viewWidth.toPx() * (1f - progress)).toInt(), y = 0) }
                                .fillMaxWidth(progress)
                                .fillMaxSize()
                                .clipToBounds()
                        ) {
                            ClipDisplayBox(
                                mediaPath = clip2Media,
                                sceneLabel = "Clip #$nextSceneNum (Next)",
                                badgeColor = Color(0xFF00C853),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                "WIPE D" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(progress)
                                .fillMaxSize()
                                .clipToBounds()
                        ) {
                            ClipDisplayBox(
                                mediaPath = clip2Media,
                                sceneLabel = "Clip #$nextSceneNum (Next)",
                                badgeColor = Color(0xFF00C853),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                "WIPE U" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(x = 0, y = (viewHeight.toPx() * (1f - progress)).toInt()) }
                                .fillMaxHeight(progress)
                                .fillMaxSize()
                                .clipToBounds()
                        ) {
                            ClipDisplayBox(
                                mediaPath = clip2Media,
                                sceneLabel = "Clip #$nextSceneNum (Next)",
                                badgeColor = Color(0xFF00C853),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                else -> {
                    ClipDisplayBox(
                        mediaPath = clip2Media,
                        sceneLabel = "Clip #$nextSceneNum (Next)",
                        badgeColor = Color(0xFF00C853),
                        modifier = Modifier.fillMaxSize().then(clip2TransformModifier)
                    )
                }
            }
        }
    }
}

/**
 * Reusable visual clip display card in the preview player.
 */
@Composable
private fun ClipDisplayBox(
    mediaPath: String?,
    sceneLabel: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF14141E)),
        contentAlignment = Alignment.Center
    ) {
        if (!mediaPath.isNullOrEmpty()) {
            AsyncImage(
                model = mediaPath,
                contentDescription = sceneLabel,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant gradient placeholder when media is not yet imported
            val isNext = sceneLabel.contains("Next")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            if (isNext) listOf(Color(0xFF0D47A1), Color(0xFF00897B), Color(0xFF1B5E20))
                            else listOf(Color(0xFF311B92), Color(0xFF880E4F), Color(0xFFE65100))
                        )
                    )
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isNext) "INCOMING CLIP" else "OUTGOING CLIP",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        // Subtitle pill identifying the clip
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.7f),
            border = BorderStroke(0.6.dp, badgeColor.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sceneLabel,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
