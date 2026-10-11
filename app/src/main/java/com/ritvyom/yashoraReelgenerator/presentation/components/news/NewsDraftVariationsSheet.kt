package com.ritvyom.yashoraReelgenerator.presentation.components.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ritvyom.yashoraReelgenerator.data.model.NewsItem
import com.ritvyom.yashoraReelgenerator.domain.models.NewsDraftVariationEngine
import com.ritvyom.yashoraReelgenerator.domain.models.NewsReelDraftVariation
import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize

/**
 * Interactive Studio Sheet that generates 3 distinct variations of a draft reel
 * from fetched news content and extracted authentic media.
 * Allows users to review, compare styles, and select their preferred draft
 * before entering the timeline video editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDraftVariationsSheet(
    news: NewsItem,
    articleImages: List<String>,
    articleVideos: List<String>,
    appLanguage: String,
    onSelectAndOpenInEditor: (variation: NewsReelDraftVariation) -> Unit,
    onSelectAndSendToPipeline: (variation: NewsReelDraftVariation) -> Unit,
    onOpenCustomScriptDialog: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    // Generate 3 variations from authentic news content
    val variations = remember(news, articleImages, articleVideos) {
        NewsDraftVariationEngine.generateVariations(news, articleImages, articleVideos)
    }

    var selectedVariationIndex by remember { mutableIntStateOf(0) }
    val currentVariation = variations[selectedVariationIndex.coerceIn(0, variations.lastIndex)]

    var isScriptExpanded by remember { mutableStateOf(false) }
    var editableScript by remember(currentVariation) { mutableStateOf(currentVariation.fullScript) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F081E),
        tonalElevation = 10.dp,
        modifier = Modifier.testTag("news_draft_variations_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // =========================================================================
            // 1. TOP HEADER: News Context & Close Button
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF007F), Color(0xFF8A2BE2))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Choose Draft Reel Style".localize(appLanguage),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00FF87).copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, Color(0xFF00FF87))
                            ) {
                                Text(
                                    text = "3 Styles".localize(appLanguage),
                                    color = Color(0xFF00FF87),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Generated from authentic news content & media".localize(appLanguage),
                            color = Color(0xFFA592C4),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenCustomScriptDialog != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2B134D),
                            border = BorderStroke(1.dp, Color(0xFF8A2BE2)),
                            modifier = Modifier
                                .clickable { onOpenCustomScriptDialog() }
                                .padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF00FF87),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Custom Prompt".localize(appLanguage),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // =========================================================================
            // 2. ARTICLE SUMMARY CARD WITH AUTHENTIC MEDIA COUNT
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1B0F33),
                border = BorderStroke(1.dp, Color(0xFF3B2363)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = news.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${news.source} • ${news.category}",
                            color = Color(0xFFB09CCC),
                            fontSize = 10.sp
                        )
                        val totalMediaCount = articleVideos.size + articleImages.size
                        Text(
                            text = "✓ $totalMediaCount authentic news media items attached".localize(appLanguage),
                            color = Color(0xFF00FF87),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 3. STYLE VARIATION SELECTOR TABS (3 DISTINCT CARDS)
            // =========================================================================
            Text(
                text = "Select Viral Reel Format:".localize(appLanguage),
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                variations.forEachIndexed { index, variation ->
                    val isSelected = selectedVariationIndex == index
                    val accentColor = Color(variation.accentColorLong)

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedVariationIndex = index }
                            .testTag("draft_variation_card_$index"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF281347) else Color(0xFF150B28)
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accentColor else Color(0xFF331D56)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(variation.emoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = variation.title,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = variation.hindiTitle,
                                color = if (isSelected) accentColor else Color.Gray,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Badge Duration
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = accentColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "~${variation.estimatedDurationSec}s • ${variation.targetSceneCount} Scn",
                                    color = accentColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 4. SELECTED VARIATION DETAILS & HERO CARD
            // =========================================================================
            val curAccent = Color(currentVariation.accentColorLong)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF190D32)),
                border = BorderStroke(1.dp, curAccent.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentVariation.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${currentVariation.title} (${currentVariation.hindiTitle})",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentVariation.subtitle,
                                    color = Color(0xFFC0AEE0),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Key Features List
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        currentVariation.keyFeatures.forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = curAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = feature,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Attributes Pills: Captions, Speed, Transition
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Caption Style Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2B164E),
                            border = BorderStroke(0.5.dp, Color(0xFF563098))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Subtitles, contentDescription = null, tint = curAccent, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Captions: ${currentVariation.captionStyle}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Speed Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2B164E),
                            border = BorderStroke(0.5.dp, Color(0xFF563098))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Speed: ${currentVariation.speedCurvePreset} (${currentVariation.speedMultiplier}x)",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Transition Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2B164E),
                            border = BorderStroke(0.5.dp, Color(0xFF563098))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MovieFilter, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Transition: ${currentVariation.transitionType}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 5. TIMELINE SCENES PREVIEW CAROUSEL
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scenes Breakdown (${currentVariation.scenes.size} Clips)".localize(appLanguage),
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ready to Edit ✓",
                    color = Color(0xFF00FF87),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(currentVariation.scenes) { scene ->
                    Card(
                        modifier = Modifier
                            .width(170.dp)
                            .height(140.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF140A26)),
                        border = BorderStroke(1.dp, Color(0xFF331D56))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Scene Media Background
                            val mediaAsset = scene.mediaPath ?: scene.remoteUrl
                            if (!mediaAsset.isNullOrBlank()) {
                                AsyncImage(
                                    model = mediaAsset,
                                    contentDescription = "Scene Media",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Dim overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.55f))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF261042), Color(0xFF0F061C))
                                            )
                                        )
                                )
                            }

                            // Top Info: Scene Number & Duration
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = curAccent,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "#${scene.sceneNumber}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }

                                Surface(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(9.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${scene.durationSeconds}s",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Bottom Subtitle Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color(0xD9000000))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = scene.narrationText.ifBlank { scene.subtitle },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 6. EXPANDABLE SCRIPT INSPECTOR
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF160B2C),
                border = BorderStroke(1.dp, Color(0xFF2C194D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isScriptExpanded = !isScriptExpanded }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = curAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View & Edit Narration Script".localize(appLanguage),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (isScriptExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }

                    if (isScriptExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editableScript,
                            onValueChange = { editableScript = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 240.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = curAccent,
                                unfocusedBorderColor = Color(0xFF3E236E),
                                focusedContainerColor = Color(0xFF110724),
                                unfocusedContainerColor = Color(0xFF110724)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 7. PRIMARY ACTION BUTTONS (OPEN IN EDITOR OR RENDER AI VIDEO)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary: AI Voiceover & Render Pipeline
                OutlinedButton(
                    onClick = {
                        val finalScenes = if (editableScript.trim() != currentVariation.fullScript.trim()) {
                            parseScriptToScenes(editableScript, currentVariation.scenes, currentVariation)
                        } else {
                            currentVariation.scenes
                        }
                        val finalVar = currentVariation.copy(
                            fullScript = editableScript,
                            scenes = finalScenes,
                            targetSceneCount = finalScenes.size,
                            estimatedDurationSec = finalScenes.sumOf { it.durationSeconds }
                        )
                        onSelectAndSendToPipeline(finalVar)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("draft_send_pipeline_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF8A2BE2)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AI Voice Pipeline".localize(appLanguage),
                            color = Color(0xFFD0BCFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TTS & Auto Render",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                // Primary: Direct 1-Click Open in Video Editor
                Button(
                    onClick = {
                        val finalScenes = if (editableScript.trim() != currentVariation.fullScript.trim()) {
                            parseScriptToScenes(editableScript, currentVariation.scenes, currentVariation)
                        } else {
                            currentVariation.scenes
                        }
                        val finalVar = currentVariation.copy(
                            fullScript = editableScript,
                            scenes = finalScenes,
                            targetSceneCount = finalScenes.size,
                            estimatedDurationSec = finalScenes.sumOf { it.durationSeconds }
                        )
                        onSelectAndOpenInEditor(finalVar)
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("draft_open_editor_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = curAccent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Open in Editor".localize(appLanguage),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Start Editing Timeline ➔",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Synchronizes user edits in the narration script text box back into individual Scene objects,
 * ensuring the timeline editor accurately receives all custom changes and complete news story scenes.
 */
private fun parseScriptToScenes(
    script: String,
    originalScenes: List<Scene>,
    variation: NewsReelDraftVariation
): List<Scene> {
    if (script.isBlank()) return originalScenes

    // Match [Scene X - ...] headers
    val sceneHeaderRegex = Regex("\\[Scene\\s*\\d+[^]]*\\]", RegexOption.IGNORE_CASE)
    val hasHeaders = script.contains(sceneHeaderRegex)

    val rawTexts = if (hasHeaders) {
        script.split(sceneHeaderRegex).map { it.trim() }.filter { it.isNotBlank() }
    } else {
        script.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
    }

    if (rawTexts.isEmpty()) return originalScenes

    return rawTexts.mapIndexed { idx, text ->
        val origScene = originalScenes.getOrNull(idx)
        val wordCount = text.split(Regex("\\s+")).count { it.isNotBlank() }
        val duration = ((wordCount / (2.4f * variation.speedMultiplier)).toInt()).coerceIn(3, 8)
        Scene(
            sceneNumber = idx + 1,
            narrationText = text,
            visualPrompt = text,
            durationSeconds = duration,
            subtitle = text,
            mediaType = origScene?.mediaType ?: "IMAGE",
            mediaPath = origScene?.mediaPath,
            remoteUrl = origScene?.remoteUrl,
            durationMs = duration * 1000L,
            captionFont = origScene?.captionFont ?: if (text.any { it in '\u0900'..'\u097F' }) "Devanagari Bold" else "TikTok Style",
            subtitleColor = origScene?.subtitleColor ?: "#FFFFFF",
            subtitleBgColor = origScene?.subtitleBgColor ?: "#CC000000",
            subtitleDesign = origScene?.subtitleDesign ?: variation.captionStyle,
            speedMultiplier = origScene?.speedMultiplier ?: variation.speedMultiplier,
            speedCurvePreset = origScene?.speedCurvePreset ?: variation.speedCurvePreset,
            transitionType = origScene?.transitionType ?: variation.transitionType
        )
    }
}
