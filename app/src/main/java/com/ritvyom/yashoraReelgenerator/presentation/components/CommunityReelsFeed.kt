package com.ritvyom.yashoraReelgenerator.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.ritvyom.yashoraReelgenerator.data.remote.SupabaseCloudManager

private val GlowingPurple = Color(0xFF8A2BE2)
private val NeonPink = Color(0xFFFF007F)
private val EmeraldGreen = Color(0xFF00E676)
private val DeepCardBg = Color(0xFF140B28)

/**
 * Vibrant horizontal scrolling section for Home Screen showing pre-made community reels from Supabase.
 */
@Composable
fun TrendingCommunityReelsSection(
    communityReels: List<SupabaseCloudManager.CachedStoryboard>,
    isLoading: Boolean,
    appLanguage: String,
    onRemixClick: (SupabaseCloudManager.CachedStoryboard) -> Unit,
    onCardClick: (SupabaseCloudManager.CachedStoryboard) -> Unit,
    onSeeAllClick: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Space & Tech", "Sports & Fitness", "Motivation", "Spiritual", "Travel & History", "Food & Culinary", "Nature & Travel")

    val filteredList = remember(communityReels, selectedCategory) {
        if (selectedCategory == "All") communityReels
        else communityReels.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("trending_community_reels_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🔥 Trending Community Reels",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Viral news storyboards & ready-to-use clips from Supabase Cloud",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = "See All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GlowingPurple,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("see_all_community_reels_button")
            )
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) GlowingPurple else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) NeonPink else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .clickable { selectedCategory = category }
                        .testTag("community_category_chip_$category")
                ) {
                    Text(
                        text = category,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Reels Cards Carousel
        if (isLoading && communityReels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = GlowingPurple,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredList, key = { it.newsId }) { reel ->
                    CommunityReelCard(
                        storyboard = reel,
                        onCardClick = { onCardClick(reel) },
                        onRemixClick = { onRemixClick(reel) }
                    )
                }
            }
        }
    }
}

/**
 * Modern 9:14 Aspect Ratio Poster Card for a Community Reel.
 */
@Composable
fun CommunityReelCard(
    storyboard: SupabaseCloudManager.CachedStoryboard,
    onCardClick: () -> Unit,
    onRemixClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCardBg),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(GlowingPurple.copy(alpha = 0.6f), NeonPink.copy(alpha = 0.4f)))),
        modifier = modifier
            .width(170.dp)
            .height(260.dp)
            .clickable { onCardClick() }
            .testTag("community_reel_card_${storyboard.newsId}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Media Poster Image
            AsyncImage(
                model = storyboard.posterUrl,
                contentDescription = storyboard.newsTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top and Bottom Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.98f)
                            )
                        )
                    )
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${storyboard.sceneCount} clips",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GlowingPurple.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = storyboard.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Center Play Icon Circle (Glassmorphism)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onCardClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Preview",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Bottom Content Information
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = storyboard.newsTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${storyboard.usageCount}+ uses",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldGreen,
                        modifier = Modifier.clickable { onRemixClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Remix",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rich Preview Dialog allowing users to inspect all scenes and 1-tap remix the project into the editor.
 */
@Composable
fun CommunityReelPreviewDialog(
    storyboard: SupabaseCloudManager.CachedStoryboard,
    onDismiss: () -> Unit,
    onRemixClick: (SupabaseCloudManager.CachedStoryboard) -> Unit
) {
    var activeSceneIndex by remember { mutableIntStateOf(0) }
    val scenes = storyboard.scenes
    val currentScene = scenes.getOrNull(activeSceneIndex) ?: scenes.firstOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DeepCardBg),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(GlowingPurple, NeonPink))),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .testTag("community_reel_preview_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GlowingPurple.copy(alpha = 0.25f),
                                border = BorderStroke(0.6.dp, GlowingPurple)
                            ) {
                                Text(
                                    text = storyboard.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlowingPurple,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldGreen.copy(alpha = 0.2f),
                                border = BorderStroke(0.6.dp, EmeraldGreen)
                            ) {
                                Text(
                                    text = "Ready to Remix",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = storyboard.newsTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Media Viewer (Aspect Ratio 9:16 Preview Box)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentScene != null) {
                        AsyncImage(
                            model = currentScene.remoteUrl ?: currentScene.mediaPath ?: storyboard.posterUrl,
                            contentDescription = currentScene.narrationText,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Subtitle overlay
                    if (currentScene != null && currentScene.subtitle.isNotBlank()) {
                        Text(
                            text = currentScene.subtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Yellow,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }

                    // Scene Indicator pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Scene ${activeSceneIndex + 1}/${scenes.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Scene Selector Carousel dots / arrows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (activeSceneIndex > 0) activeSceneIndex-- },
                        enabled = activeSceneIndex > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Scene",
                            tint = if (activeSceneIndex > 0) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        scenes.forEachIndexed { idx, _ ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == activeSceneIndex) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (idx == activeSceneIndex) GlowingPurple else Color.White.copy(alpha = 0.3f))
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (activeSceneIndex < scenes.size - 1) activeSceneIndex++ },
                        enabled = activeSceneIndex < scenes.size - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Scene",
                            tint = if (activeSceneIndex < scenes.size - 1) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                // Script Narration Details Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Narration Voice-over:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = currentScene?.narrationText ?: storyboard.refinedScript,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    if (currentScene?.visualPrompt?.isNotBlank() == true) {
                        Text(
                            text = "Visual Prompt:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlowingPurple
                        )
                        Text(
                            text = currentScene.visualPrompt,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1-Tap Remix Button (Calls MainViewModel to load into studio editor)
                Button(
                    onClick = {
                        onDismiss()
                        onRemixClick(storyboard)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("remix_community_reel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚡ Remix & Open in Studio Editor",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 2-Column Vertical Grid of Community Reels for the Templates Tab.
 */
@Composable
fun CommunityReelsGrid(
    communityReels: List<SupabaseCloudManager.CachedStoryboard>,
    onCardClick: (SupabaseCloudManager.CachedStoryboard) -> Unit,
    onRemixClick: (SupabaseCloudManager.CachedStoryboard) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(communityReels, key = { it.newsId }) { reel ->
            CommunityReelCard(
                storyboard = reel,
                onCardClick = { onCardClick(reel) },
                onRemixClick = { onRemixClick(reel) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
