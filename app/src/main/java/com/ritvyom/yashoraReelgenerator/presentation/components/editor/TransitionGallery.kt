package com.ritvyom.yashoraReelgenerator.presentation.components.editor

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import com.ritvyom.yashoraReelgenerator.presentation.utils.Media3TransitionEngine
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize

val GalleryOrange = Color(0xFFFF5722)
val GalleryOrangeDark = Color(0xFFE64A19)

/**
 * Resolves a dedicated Material Icon representing the visual motion of the transition effect.
 */
fun getTransitionMaterialIcon(transitionId: String): ImageVector {
    val norm = Media3TransitionEngine.normalizeTransitionId(transitionId)
    return when {
        norm == "None" -> Icons.Default.Block
        norm in listOf("MIX", "DISSOLVE", "BLUR DISSOLVE") -> Icons.Default.AutoFixHigh
        norm in listOf("FADE B", "FADE W", "LUMA FADE", "SEPIA FADE") -> Icons.Default.BrightnessMedium
        norm in listOf("FLASH", "COLOR FLASH", "FLOODLIGHT") -> Icons.Default.FlashOn
        norm in listOf("ZOOM IN", "SPEED ZOOM", "BOUNCE") -> Icons.Default.ZoomIn
        norm in listOf("ZOOM OUT", "ZOOM TWIST") -> Icons.Default.ZoomOut
        norm in listOf("SLIDE L", "MOVE L", "PUSH L", "FAST WHIP L") -> Icons.AutoMirrored.Filled.ArrowBack
        norm in listOf("SLIDE R", "MOVE R", "PUSH R", "FAST WHIP R") -> Icons.AutoMirrored.Filled.ArrowForward
        norm in listOf("SLIDE U", "MOVE U", "WIPE U") -> Icons.Default.ArrowUpward
        norm in listOf("SLIDE D", "MOVE D", "WIPE D") -> Icons.Default.ArrowDownward
        norm in listOf("WIPE L", "WIPE R") -> Icons.Default.CompareArrows
        norm in listOf("SPLIT V", "SPLIT H") -> Icons.Default.CallSplit
        norm in listOf("RGB SPLIT", "NOISE GLITCH") -> Icons.Default.Grain
        norm in listOf("SWIRL L", "SWIRL R", "RADIAL") -> Icons.Default.Refresh
        norm in listOf("SHAKE", "SHOCKWAVE") -> Icons.Default.Waves
        norm in listOf("LIGHT LEAK", "FILM BURN", "PRISM") -> Icons.Default.Flare
        norm in listOf("MOSAIC") -> Icons.Default.GridOn
        norm in listOf("TURN PAGE") -> Icons.Default.MenuBook
        norm in listOf("CIRCULAR", "BLINK") -> Icons.Default.RemoveRedEye
        else -> Icons.Default.Animation
    }
}

/**
 * Animated/graphical preview thumbnail for individual video transition cards.
 * Renders the visual movement (Fade, Slide, Zoom, Wipe, Glitch, etc.) with Material Icon overlay.
 */
@Composable
fun TransitionThumbnailPreview(
    transitionId: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val norm = remember(transitionId) { Media3TransitionEngine.normalizeTransitionId(transitionId) }
    val icon = remember(norm) { getTransitionMaterialIcon(norm) }

    val infiniteTransition = rememberInfiniteTransition(label = "thumb_anim_$norm")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thumb_progress_$norm"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E2C)),
        contentAlignment = Alignment.Center
    ) {
        // Thumbnail Graphical Canvas Preview
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val p = if (isSelected) animProgress else 0.5f

            when {
                norm == "None" -> {
                    drawRect(color = Color(0xFF282838))
                    drawLine(
                        color = Color.Red.copy(alpha = 0.6f),
                        start = Offset(w * 0.25f, h * 0.25f),
                        end = Offset(w * 0.75f, h * 0.75f),
                        strokeWidth = 3f
                    )
                }
                norm in listOf("MIX", "DISSOLVE", "BLUR DISSOLVE") -> {
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF3F51B5), Color(0xFFE91E63))),
                        size = size
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.28f * p),
                        radius = w * 0.4f * p,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                }
                norm in listOf("FADE B", "FADE W", "LUMA FADE", "SEPIA FADE") -> {
                    val isWhite = norm.contains("W")
                    val fadeColor = if (isWhite) Color.White else Color.Black
                    drawRect(brush = Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFF8E24AA))))
                    drawRect(
                        color = fadeColor.copy(alpha = (1f - p).coerceIn(0.1f, 0.9f)),
                        size = size
                    )
                }
                norm in listOf("FLASH", "COLOR FLASH", "FLOODLIGHT") -> {
                    drawRect(color = Color(0xFF1A1A2E))
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = (0.35f + 0.65f * p).coerceIn(0f, 1f)),
                        radius = w * 0.45f * (0.8f + 0.2f * p),
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                }
                norm in listOf("SLIDE L", "MOVE L", "PUSH L", "FAST WHIP L") -> {
                    val splitX = w * (1f - p * 0.6f)
                    drawRect(color = Color(0xFF1E88E5), size = Size(splitX, h))
                    drawRect(color = Color(0xFFFF5722), topLeft = Offset(splitX, 0f), size = Size(w - splitX, h))
                    drawLine(color = Color.White, start = Offset(splitX, 0f), end = Offset(splitX, h), strokeWidth = 2f)
                }
                norm in listOf("SLIDE R", "MOVE R", "PUSH R", "FAST WHIP R") -> {
                    val splitX = w * (p * 0.6f)
                    drawRect(color = Color(0xFFFF5722), size = Size(splitX, h))
                    drawRect(color = Color(0xFF1E88E5), topLeft = Offset(splitX, 0f), size = Size(w - splitX, h))
                    drawLine(color = Color.White, start = Offset(splitX, 0f), end = Offset(splitX, h), strokeWidth = 2f)
                }
                norm in listOf("SLIDE U", "MOVE U", "WIPE U") -> {
                    val splitY = h * (1f - p * 0.6f)
                    drawRect(color = Color(0xFF00897B), size = Size(w, splitY))
                    drawRect(color = Color(0xFFE91E63), topLeft = Offset(0f, splitY), size = Size(w, h - splitY))
                    drawLine(color = Color.White, start = Offset(0f, splitY), end = Offset(w, splitY), strokeWidth = 2f)
                }
                norm in listOf("SLIDE D", "MOVE D", "WIPE D") -> {
                    val splitY = h * (p * 0.6f)
                    drawRect(color = Color(0xFFE91E63), size = Size(w, splitY))
                    drawRect(color = Color(0xFF00897B), topLeft = Offset(0f, splitY), size = Size(w, h - splitY))
                    drawLine(color = Color.White, start = Offset(0f, splitY), end = Offset(w, splitY), strokeWidth = 2f)
                }
                norm in listOf("ZOOM IN", "SPEED ZOOM", "BOUNCE") -> {
                    drawRect(brush = Brush.radialGradient(listOf(Color(0xFFFF9800), Color(0xFF3F51B5))))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = (w * 0.2f * (0.6f + 0.4f * p)),
                        style = Stroke(width = 2.5f)
                    )
                }
                norm in listOf("ZOOM OUT", "ZOOM TWIST") -> {
                    drawRect(brush = Brush.radialGradient(listOf(Color(0xFF8E24AA), Color(0xFF00ACC1))))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = (w * 0.38f * (1.15f - 0.3f * p)),
                        style = Stroke(width = 2.5f)
                    )
                }
                norm in listOf("RGB SPLIT", "NOISE GLITCH") -> {
                    drawRect(color = Color(0xFF14141E))
                    drawRect(
                        color = Color.Cyan.copy(alpha = 0.45f),
                        topLeft = Offset(4f * (p - 0.5f), 0f),
                        size = size
                    )
                    drawRect(
                        color = Color.Magenta.copy(alpha = 0.45f),
                        topLeft = Offset(-4f * (p - 0.5f), 0f),
                        size = size
                    )
                }
                else -> {
                    drawRect(brush = Brush.linearGradient(listOf(Color(0xFF263238), Color(0xFF37474F))))
                }
            }
        }

        // Tint overlay for icon clarity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        // Center Material Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) GalleryOrange else Color.White,
            modifier = Modifier
                .size(24.dp)
                .scale(if (isSelected) 1.15f else 1.0f)
        )

        // Selected Checkmark Pill
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(GalleryOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.Black,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

/**
 * TransitionGallery UI Component:
 * Displays a categorized responsive grid of all available video transition effects
 * (Fade, Slide, Zoom, Glitch, Wipe, Mix, etc.) with animated preview thumbnails,
 * material-icons, search filtering, duration sliders, and real-time preview playback.
 */
@Composable
fun TransitionGallery(
    currentTransition: String,
    currentDurationMs: Long = 700L,
    currentScene: Scene? = null,
    nextScene: Scene? = null,
    allScenesCount: Int = 1,
    appLanguage: String = "en",
    onTransitionSelected: (transitionType: String, durationMs: Long) -> Unit,
    onApplyToAll: ((transitionType: String, durationMs: Long) -> Unit)? = null,
    onPreviewTransition: ((transitionType: String, durationMs: Long) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTransition by remember(currentTransition) {
        mutableStateOf(currentTransition.ifBlank { "None" })
    }
    var transitionDurationMs by remember(currentDurationMs) {
        mutableLongStateOf(if (currentDurationMs > 0L) currentDurationMs.coerceIn(200L, 2000L) else 700L)
    }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val allTransitions = remember { Media3TransitionEngine.TRANSITIONS }
    val categories = remember { listOf("ALL", "BASIC", "SLIDE", "GLITCH", "STYLE", "SPORT") }

    // Filter transitions based on Category and Search Query
    val filteredTransitions = remember(selectedCategory, searchQuery) {
        allTransitions.filter { item ->
            val matchesCategory = selectedCategory == "ALL" ||
                    item.category.equals(selectedCategory, ignoreCase = true) ||
                    item.id.equals("None", ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    item.displayName.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transition_gallery_container"),
        color = Color(0xFF14141E),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Header Bar with Title, Counter, and Apply Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Animation,
                        contentDescription = null,
                        tint = GalleryOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Transition Gallery".localize(appLanguage),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allTransitions.size} Effects (Fade, Slide, Zoom & More)".localize(appLanguage),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            val duration = if (selectedTransition.equals("None", ignoreCase = true)) 0L else transitionDurationMs
                            onTransitionSelected(selectedTransition, duration)
                            onDismiss?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GalleryOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("transition_gallery_apply_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply".localize(appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Interactive Live Preview Player
            TransitionLivePreviewPlayer(
                transitionType = selectedTransition,
                durationMs = transitionDurationMs,
                currentScene = currentScene,
                nextScene = nextScene,
                appLanguage = appLanguage,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Search Bar for Fast Transition Filtering
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E1E2C))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                    cursorBrush = SolidColor(GalleryOrange),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transition_search_input"),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search transitions (Fade, Slide, Zoom, Flash...)".localize(appLanguage), color = Color.Gray, fontSize = 12.sp)
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Duration Selector Slider & Quick Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duration: ${String.format(java.util.Locale.US, "%.1fs", transitionDurationMs / 1000f)}".localize(appLanguage),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(300L to "0.3s", 500L to "0.5s", 700L to "0.7s", 1000L to "1.0s", 1500L to "1.5s", 2000L to "2.0s").forEach { (ms, label) ->
                        val isDurSelected = transitionDurationMs == ms
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDurSelected) GalleryOrange else Color(0xFF262634),
                            border = if (isDurSelected) null else BorderStroke(0.6.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable {
                                transitionDurationMs = ms
                                onPreviewTransition?.invoke(selectedTransition, ms)
                            }
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Slider(
                value = transitionDurationMs.toFloat(),
                onValueChange = {
                    val ms = it.toLong()
                    transitionDurationMs = ms
                    onPreviewTransition?.invoke(selectedTransition, ms)
                },
                valueRange = 200f..2000f,
                colors = SliderDefaults.colors(
                    thumbColor = GalleryOrange,
                    activeTrackColor = GalleryOrange,
                    inactiveTrackColor = Color(0xFF383848)
                ),
                modifier = Modifier.testTag("transition_duration_slider")
            )

            // 5. Horizontal Category Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory.equals(cat, ignoreCase = true)
                    val count = if (cat == "ALL") allTransitions.size else allTransitions.count { it.category.equals(cat, ignoreCase = true) }
                    val catIcon = when (cat) {
                        "ALL" -> Icons.Default.Dashboard
                        "BASIC" -> Icons.Default.AutoFixNormal
                        "SLIDE" -> Icons.Default.CompareArrows
                        "GLITCH" -> Icons.Default.Grain
                        "STYLE" -> Icons.Default.Palette
                        "SPORT" -> Icons.Default.Speed
                        else -> Icons.Default.Animation
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCatSelected) GalleryOrange else Color(0xFF20202C),
                        border = if (isCatSelected) null else BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .clickable { selectedCategory = cat }
                            .testTag("transition_cat_$cat")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = catIcon, contentDescription = null, tint = if (isCatSelected) Color.White else Color.Gray, modifier = Modifier.size(13.dp))
                            Text(
                                text = cat.localize(appLanguage),
                                color = if (isCatSelected) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (isCatSelected) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "$count",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Apply to All Clips Shortcut Button
            if (allScenesCount > 1 && onApplyToAll != null) {
                OutlinedButton(
                    onClick = {
                        val duration = if (selectedTransition.equals("None", ignoreCase = true)) 0L else transitionDurationMs
                        onApplyToAll(selectedTransition, duration)
                        onDismiss?.invoke()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_transition_all_clips"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GalleryOrange),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(GalleryOrange, GalleryOrangeDark)))
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apply '${selectedTransition}' to All $allScenesCount Clips".localize(appLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 7. Grid of Transition Cards with Preview Thumbnails & Material Icons
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .testTag("transition_gallery_grid")
            ) {
                items(filteredTransitions, key = { it.id }) { item ->
                    val isSelected = selectedTransition.equals(item.id, ignoreCase = true)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) GalleryOrange.copy(alpha = 0.28f) else Color(0xFF20202C)
                        ),
                        border = if (isSelected) BorderStroke(2.dp, Brush.horizontalGradient(listOf(GalleryOrange, GalleryOrangeDark))) else BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTransition = item.id
                                onTransitionSelected(item.id, transitionDurationMs)
                                onPreviewTransition?.invoke(item.id, transitionDurationMs)
                            }
                            .testTag("transition_card_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Thumbnail Preview with Material Icon
                            TransitionThumbnailPreview(
                                transitionId = item.id,
                                isSelected = isSelected,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Transition Name
                            Text(
                                text = item.displayName.localize(appLanguage),
                                color = if (isSelected) GalleryOrange else Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Category Tag & PRO Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    color = Color.Gray,
                                    fontSize = 8.5.sp
                                )
                                if (item.isPro) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = Color(0xFFFFD700)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = Color.Black,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
