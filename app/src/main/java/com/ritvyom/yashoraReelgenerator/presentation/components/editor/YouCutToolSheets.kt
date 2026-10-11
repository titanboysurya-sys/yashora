package com.ritvyom.yashoraReelgenerator.presentation.components.editor

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import com.ritvyom.yashoraReelgenerator.engine.timeline.CanonicalSpeedEngine
import com.ritvyom.yashoraReelgenerator.engine.timeline.SpeedCurvePreset
import com.ritvyom.yashoraReelgenerator.engine.timeline.SpeedRampPoint
import com.ritvyom.yashoraReelgenerator.presentation.utils.AudioManager
import com.ritvyom.yashoraReelgenerator.presentation.utils.EffectManager
import com.ritvyom.yashoraReelgenerator.presentation.utils.FilterManager
import com.ritvyom.yashoraReelgenerator.presentation.utils.SoundSynth
import com.ritvyom.yashoraReelgenerator.presentation.utils.VoiceRecorderManager
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Filter Studio Sheet:
 * - Categories: Trending, Cinematic, Retro, Vintage, B&W, Pastel, Film, HDR, Warm, Cool
 * - Visual filter cards with color preview
 * - Filter Intensity Slider (0% - 100%)
 * - Reset to Original
 * - Apply to all clips option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onFilterSelected: (name: String, category: String, index: Int) -> Unit,
    onIntensityChanged: (Int) -> Unit,
    onApplyToAll: (name: String, category: String, index: Int, intensity: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(currentScene.filterCategory.ifEmpty { "Trending" }) }
    var currentIntensity by remember { mutableFloatStateOf(currentScene.filterIntensity.toFloat()) }
    var applyToAllClips by remember { mutableStateOf(false) }

    val categories = remember { FilterManager.CATEGORIES }
    val availableFilters = remember(selectedCategory) {
        FilterManager.getFiltersByCategory(selectedCategory)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = YouCutOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Filter Studio".localize(appLanguage),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            onFilterSelected("Normal", "Normal", 0)
                            onIntensityChanged(80)
                        }
                    ) {
                        Text("Reset / Original".localize(appLanguage), color = YouCutOrange, fontSize = 12.sp)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = YouCutOrange)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Category Tabs
            val catScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(catScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isCatSelected = selectedCategory.equals(category, ignoreCase = true)
                    FilterChip(
                        selected = isCatSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category.localize(appLanguage),
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YouCutOrange,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF242430),
                            labelColor = Color.LightGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Thumbnails Horizontal List
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Original / Normal Option
                item {
                    val isNoneSelected = currentScene.filterCategory == "Normal" || currentScene.selectedFilterName == "Normal"
                    Column(
                        modifier = Modifier
                            .width(68.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onFilterSelected("Normal", "Normal", 0)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E2E3E))
                                .border(
                                    width = if (isNoneSelected) 2.dp else 1.dp,
                                    color = if (isNoneSelected) YouCutOrange else Color(0xFF444458),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Original".localize(appLanguage),
                            color = if (isNoneSelected) YouCutOrange else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Filter Items
                items(availableFilters) { filterData ->
                    val isSelected = currentScene.filterCategory == filterData.name || currentScene.selectedFilterName == filterData.name
                    Column(
                        modifier = Modifier
                            .width(68.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val idx = FilterManager.ALL_FILTERS.indexOf(filterData).coerceAtLeast(0)
                                onFilterSelected(filterData.name, filterData.name, idx)
                                if (applyToAllClips) {
                                    onApplyToAll(filterData.name, filterData.name, idx, currentIntensity.toInt())
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            filterData.tintOverlayColor.copy(alpha = 0.8f),
                                            Color(0xFF1E1E2C)
                                        )
                                    )
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) YouCutOrange else Color(0xFF383848),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filterData.name.take(3).uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = filterData.name,
                            color = if (isSelected) YouCutOrange else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Intensity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Filter Intensity".localize(appLanguage),
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${currentIntensity.toInt()}%",
                    color = YouCutOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = currentIntensity,
                onValueChange = {
                    currentIntensity = it
                    onIntensityChanged(it.toInt())
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = YouCutOrange,
                    activeTrackColor = YouCutOrange,
                    inactiveTrackColor = Color(0xFF383848)
                )
            )

            // Apply to all clips checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { applyToAllClips = !applyToAllClips }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = applyToAllClips,
                    onCheckedChange = { applyToAllClips = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = YouCutOrange,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Apply filter to all scenes".localize(appLanguage),
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Visual Effects Studio Sheet:
 * - Categories: Basic, Party, Glitch, Nature, Cinematic, Spark, Fire
 * - Cards for effects (Glitch, VHS, Shake, Snow, Rain, Sparkle, Disco, Flash, Zoom, Blur, etc.)
 * - Intensity slider
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onEffectSelected: (name: String, category: String) -> Unit,
    onIntensityChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(currentScene.effectCategory.ifEmpty { "Glitch" }) }
    var currentIntensity by remember { mutableFloatStateOf(currentScene.effectIntensity.toFloat()) }

    val categories = remember { EffectManager.CATEGORIES }
    val availableEffects = remember(selectedCategory) {
        EffectManager.getEffectsByCategory(selectedCategory)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = YouCutOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Video Effects".localize(appLanguage),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = {
                        onEffectSelected("None", "None")
                    }
                ) {
                    Text("None / Remove".localize(appLanguage), color = YouCutOrange, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Tabs
            val catScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(catScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isCatSelected = selectedCategory.equals(category, ignoreCase = true)
                    FilterChip(
                        selected = isCatSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category.localize(appLanguage),
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YouCutOrange,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF242430),
                            labelColor = Color.LightGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Effect Items
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(availableEffects) { effectData ->
                    val isSelected = currentScene.effectName == effectData.name
                    Column(
                        modifier = Modifier
                            .width(74.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onEffectSelected(effectData.name, effectData.category)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) YouCutOrange.copy(alpha = 0.25f) else Color(0xFF222230))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) YouCutOrange else Color(0xFF383848),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isSelected) YouCutOrange else Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = effectData.name,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Effect Intensity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Effect Intensity".localize(appLanguage),
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${currentIntensity.toInt()}%",
                    color = YouCutOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = currentIntensity,
                onValueChange = {
                    currentIntensity = it
                    onIntensityChanged(it.toInt())
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = YouCutOrange,
                    activeTrackColor = YouCutOrange,
                    inactiveTrackColor = Color(0xFF383848)
                )
            )
        }
    }
}

/**
 * Trim & Split Studio Sheet:
 * - Start & End time adjustment
 * - Split at playhead
 * - Delete clip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimStudioSheet(
    currentScene: Scene,
    selectedSceneIndex: Int,
    totalScenes: Int,
    currentPlaybackTimeMs: Long,
    appLanguage: String,
    onDurationChanged: (Int) -> Unit,
    onSplitClip: () -> Unit,
    onDeleteClip: () -> Unit,
    onOpenPrecisionTrimmer: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var clipDuration by remember { mutableFloatStateOf(currentScene.durationSeconds.toFloat()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = YouCutOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trim & Cut Clip #${selectedSceneIndex + 1}".localize(appLanguage),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = YouCutOrange)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Duration Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clip Duration".localize(appLanguage),
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Text(
                    text = "${clipDuration.toInt()}s",
                    color = YouCutOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = clipDuration,
                onValueChange = {
                    clipDuration = it
                    onDurationChanged(it.toInt())
                },
                valueRange = 1f..60f,
                steps = 59,
                colors = SliderDefaults.colors(
                    thumbColor = YouCutOrange,
                    activeTrackColor = YouCutOrange,
                    inactiveTrackColor = Color(0xFF383848)
                )
            )
            if (onOpenPrecisionTrimmer != null && (currentScene.mediaType.equals("VIDEO", ignoreCase = true) || !currentScene.mediaPath.isNullOrEmpty())) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        onOpenPrecisionTrimmer()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Precision Start/End Trimmer ✂️".localize(appLanguage), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Split at Playhead & Delete Clip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onSplitClip()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = YouCutOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Split at Playhead".localize(appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onDeleteClip()
                        onDismiss()
                    },
                    enabled = totalScenes > 1,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Clip".localize(appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Music & Sound Effects Studio Sheet:
 * - Library of Beats & Genres
 * - Import Local Audio from device (.mp3, .wav, .m4a)
 * - Sound FX (Whoosh, Pop, Camera Click, Ding, etc.)
 * - Music Volume control
 * - Mute original clip audio
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onSelectBgCategory: (String) -> Unit,
    onPickCustomAudio: () -> Unit,
    onSelectSfx: (String) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onToggleMuteClipAudio: () -> Unit,
    isClipAudioMuted: Boolean,
    customAudioTrackName: String? = null,
    customAudioDurationMs: Long = 0L,
    audioTrimStartMs: Long = 0L,
    audioTrimDurationMs: Long = 0L,
    bgmVolume: Float = 1.0f,
    onBgmVolumeChanged: ((Float) -> Unit)? = null,
    onApplyVideoVolumeToAll: ((Float) -> Unit)? = null,
    isAutoDucking: Boolean = false,
    onToggleAutoDucking: (() -> Unit)? = null,
    onAudioTrimChanged: ((startMs: Long, durationMs: Long) -> Unit)? = null,
    onRemoveAudioTrack: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var activeMusicTab by remember { mutableIntStateOf(if (!customAudioTrackName.isNullOrEmpty()) 2 else 0) } // 0 = Music, 1 = SFX, 2 = Multi-Track Mixer
    val genres = remember { listOf("Upbeat", "Cinematic", "Lofi Chill", "Motivational", "Acoustic", "Hip Hop", "Ambient") }
    val sfxList = remember { listOf("Whoosh", "Pop", "Camera Click", "Ding", "Glitch Swish", "Sub Drop", "Magic Chime", "Drum Roll") }
    
    var videoVolumeState by remember(currentScene.volume) { mutableFloatStateOf(currentScene.volume * 100f) }
    var bgmVolumeState by remember(bgmVolume) { mutableFloatStateOf(bgmVolume * 100f) }
    
    var trimStartSec by remember(audioTrimStartMs) { mutableFloatStateOf(audioTrimStartMs / 1000f) }
    val audioDurationSec = remember(customAudioDurationMs) { (customAudioDurationMs / 1000f).coerceAtLeast(10f) }
    var trimDurationSec by remember(audioTrimDurationMs, audioDurationSec) { 
        mutableFloatStateOf(if (audioTrimDurationMs > 0L) (audioTrimDurationMs / 1000f) else audioDurationSec.coerceAtMost(30f)) 
    }
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = YouCutOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Music & Sound Effects".localize(appLanguage),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = YouCutOrange)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tabs: Background Music / Sound FX / Multi-Track Mixer
            TabRow(
                selectedTabIndex = activeMusicTab,
                containerColor = Color(0xFF222230),
                contentColor = YouCutOrange
            ) {
                Tab(
                    selected = activeMusicTab == 0,
                    onClick = { activeMusicTab = 0 },
                    text = { Text("Music Tracks".localize(appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeMusicTab == 1,
                    onClick = { activeMusicTab = 1 },
                    text = { Text("Sound Effects".localize(appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeMusicTab == 2,
                    onClick = { activeMusicTab = 2 },
                    text = { Text("🎚️ Multi-Track Mixer".localize(appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeMusicTab) {
                0 -> {
                    // Custom Audio Import Button
                    Button(
                        onClick = onPickCustomAudio,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = YouCutOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Music From Phone".localize(appLanguage), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Curated Background Tracks:".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(genres) { genre ->
                            Card(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(70.dp)
                                    .clickable {
                                        onSelectBgCategory(genre)
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF282838)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3E3E50)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(genre, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Sound FX list
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(160.dp)
                    ) {
                        items(sfxList) { sfxName ->
                            val isSelected = currentScene.sfxName == sfxName
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSfx(sfxName)
                                        coroutineScope.launch {
                                            SoundSynth.playSfx(sfxName)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) YouCutOrange.copy(alpha = 0.25f) else Color(0xFF252535)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) YouCutOrange else Color(0xFF383848)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(sfxName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Multi-Track Audio Mixer: Independent controls for Video Audio and Background Music
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Track 1: Original Video Audio
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF20202E)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF36364A))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Videocam, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Original Video Sound".localize(appLanguage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isClipAudioMuted || videoVolumeState == 0f) Color(0xFF4A1A1A) else YouCutOrange.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isClipAudioMuted || videoVolumeState == 0f) "Muted" else "${videoVolumeState.toInt()}%",
                                            color = if (isClipAudioMuted || videoVolumeState == 0f) Color(0xFFFF5252) else YouCutOrange,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Slider(
                                    value = if (isClipAudioMuted) 0f else videoVolumeState,
                                    onValueChange = {
                                        videoVolumeState = it
                                        onVolumeChanged(it / 100f)
                                    },
                                    valueRange = 0f..200f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = YouCutOrange,
                                        activeTrackColor = YouCutOrange,
                                        inactiveTrackColor = Color(0xFF383848)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.clickable { onToggleMuteClipAudio() },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isClipAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = if (isClipAudioMuted) Color(0xFFFF5252) else Color.LightGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isClipAudioMuted) "Unmute Video".localize(appLanguage) else "Mute Video".localize(appLanguage),
                                            color = if (isClipAudioMuted) Color(0xFFFF5252) else Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    if (onApplyVideoVolumeToAll != null) {
                                        TextButton(
                                            onClick = { onApplyVideoVolumeToAll(videoVolumeState / 100f) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Apply to All Clips".localize(appLanguage), fontSize = 10.5.sp, color = YouCutOrange)
                                        }
                                    }
                                }
                            }
                        }

                        // Track 2: Background Music (BGM) Track
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2838)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF29B6F6).copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Background Music (BGM)".localize(appLanguage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF29B6F6).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${bgmVolumeState.toInt()}%",
                                            color = Color(0xFF29B6F6),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (!customAudioTrackName.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = customAudioTrackName,
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (onRemoveAudioTrack != null) {
                                            IconButton(onClick = onRemoveAudioTrack, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove Audio", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = onPickCustomAudio,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF29B6F6)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF29B6F6).copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import Local Background Music File".localize(appLanguage), fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Slider(
                                    value = bgmVolumeState,
                                    onValueChange = {
                                        bgmVolumeState = it
                                        onBgmVolumeChanged?.invoke(it / 100f)
                                    },
                                    valueRange = 0f..200f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF29B6F6),
                                        activeTrackColor = Color(0xFF29B6F6),
                                        inactiveTrackColor = Color(0xFF383848)
                                    )
                                )

                                if (!customAudioTrackName.isNullOrEmpty()) {
                                    // Audio Trim Start Offset
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Music Start Offset".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp)
                                        Text(String.format(Locale.US, "%.1fs", trimStartSec), color = Color(0xFF29B6F6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Slider(
                                        value = trimStartSec,
                                        onValueChange = {
                                            trimStartSec = it
                                            onAudioTrimChanged?.invoke((it * 1000).toLong(), (trimDurationSec * 1000).toLong())
                                        },
                                        valueRange = 0f..(audioDurationSec - 1f).coerceAtLeast(0f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF29B6F6),
                                            activeTrackColor = Color(0xFF29B6F6),
                                            inactiveTrackColor = Color(0xFF383848)
                                        )
                                    )
                                }
                            }
                        }

                        // Smart Audio Ducking
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleAutoDucking?.invoke() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VolumeDown, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Smart Audio Ducking".localize(appLanguage), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Lowers music automatically when voiceover speaks".localize(appLanguage), color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                                Switch(
                                    checked = isAutoDucking,
                                    onCheckedChange = { onToggleAutoDucking?.invoke() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = YouCutOrange, checkedTrackColor = YouCutOrange.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Text & Typography Studio Sheet:
 * - Text overlay editor
 * - Color picker
 * - Font selection
 * - Text animation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onLiveUpdate: ((text: String, color: String, font: String, animation: String, fontSize: Float, durationSec: Float, startSec: Float) -> Unit)? = null,
    onTextSaveFull: ((text: String, color: String, font: String, animation: String, fontSize: Float, durationSec: Float, startSec: Float) -> Unit)? = null,
    onTextSave: ((text: String, color: String, font: String, animation: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf(currentScene.textOverlay ?: "") }
    var selectedColor by remember { mutableStateOf(currentScene.overlayColor.ifEmpty { "#FFFFFF" }) }
    var selectedFont by remember { mutableStateOf(currentScene.captionFont.ifEmpty { "TikTok Style" }) }
    var selectedAnimation by remember { mutableStateOf(currentScene.textAnimation.ifEmpty { "Pop" }) }
    var fontSize by remember { mutableFloatStateOf(currentScene.textOverlayFontSize.coerceIn(12f, 54f)) }
    val maxDuration = currentScene.durationSeconds.toFloat().coerceAtLeast(1f)
    var durationSec by remember { mutableFloatStateOf(currentScene.textOverlayDurationSeconds.coerceIn(0.5f, maxDuration)) }
    var startSec by remember { mutableFloatStateOf(currentScene.textOverlayStartTimeSeconds.coerceIn(0f, (maxDuration - 0.5f).coerceAtLeast(0f))) }

    fun notifyLive() {
        onLiveUpdate?.invoke(textInput, selectedColor, selectedFont, selectedAnimation, fontSize, durationSec, startSec)
    }

    val colors = remember {
        listOf(
            "#FFFFFF", "#FFD700", "#FF5722", "#00E5FF", "#FF007F",
            "#00E676", "#FFEB3B", "#E040FB", "#000000"
        )
    }

    val fonts = remember {
        listOf("TikTok Style", "Modern Bold", "Cyberpunk", "Serif Elegant", "Handwritten", "Display Heavy")
    }

    val animations = remember {
        listOf("None", "Pop", "Fade", "Slide Up", "Slide Down", "Slide Left", "Slide Right", "Typewriter", "Bounce")
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Text & Font, 1 = Color & Stroke, 2 = Animation & Timing
    var selectedAlignment by remember { mutableStateOf("Center") }
    var hasBackground by remember { mutableStateOf(false) }
    var selectedBgColor by remember { mutableStateOf("#99000000") }
    var strokeWidth by remember { mutableFloatStateOf(0f) }
    var selectedStrokeColor by remember { mutableStateOf("#000000") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TextFields, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Text & Titles Overlay".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (onTextSaveFull != null) {
                            onTextSaveFull(textInput, selectedColor, selectedFont, selectedAnimation, fontSize, durationSec, startSec)
                        } else {
                            onTextSave?.invoke(textInput, selectedColor, selectedFont, selectedAnimation)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YouCutOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Apply".localize(appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-tabs for clean mobile ergonomics
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF22222E),
                contentColor = YouCutOrange,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Text & Font".localize(appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Color & Style".localize(appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Motion & Time".localize(appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // TAB 0: Text Input, Font Size, Alignment, Font Family
                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        notifyLive()
                    },
                    placeholder = { Text("Enter title or caption overlay...".localize(appLanguage), color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YouCutOrange,
                        unfocusedBorderColor = Color(0xFF383848)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Style Presets Shortcut
                Text("Style Presets".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedColor == "#FFFFFF" && !hasBackground,
                            onClick = {
                                selectedColor = "#FFFFFF"
                                hasBackground = false
                                strokeWidth = 0f
                                notifyLive()
                            },
                            label = { Text("Classic White", fontSize = 10.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedColor == "#FFEB3B" && strokeWidth > 0f,
                            onClick = {
                                selectedColor = "#FFEB3B"
                                strokeWidth = 2.5f
                                selectedStrokeColor = "#000000"
                                notifyLive()
                            },
                            label = { Text("Bold Pop", fontSize = 10.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = hasBackground,
                            onClick = {
                                hasBackground = true
                                selectedBgColor = "#99000000"
                                selectedColor = "#FFFFFF"
                                notifyLive()
                            },
                            label = { Text("Box Banner", fontSize = 10.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedColor == "#00E5FF",
                            onClick = {
                                selectedColor = "#00E5FF"
                                selectedFont = "Cyberpunk"
                                strokeWidth = 1.5f
                                selectedStrokeColor = "#FF007F"
                                notifyLive()
                            },
                            label = { Text("Cyberpunk", fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Font Size Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font Size: ${fontSize.toInt()}sp".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(16f to "S", 24f to "M", 32f to "L", 42f to "XL").forEach { (sz, label) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (fontSize == sz) YouCutOrange else Color(0xFF282834),
                                modifier = Modifier.clickable {
                                    fontSize = sz
                                    notifyLive()
                                }
                            ) {
                                Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
                Slider(
                    value = fontSize,
                    onValueChange = {
                        fontSize = it
                        notifyLive()
                    },
                    valueRange = 12f..54f,
                    colors = SliderDefaults.colors(thumbColor = YouCutOrange, activeTrackColor = YouCutOrange, inactiveTrackColor = Color(0xFF383848))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Fonts
                Text("Font Family".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(fonts) { fontName ->
                        val isSelected = selectedFont == fontName
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFont = fontName
                                notifyLive()
                            },
                            label = { Text(fontName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YouCutOrange,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF242430),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }
            } else if (selectedTab == 1) {
                // TAB 1: Text Color, Background Pill, Stroke
                Text("Text Color".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colors) { hex ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) YouCutOrange else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = hex
                                    notifyLive()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Fine-Tune Spectrum (Live Drag)".localize(appLanguage), color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
                ) {
                    val barWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val rainbowColors = remember {
                        listOf(
                            Color(0xFFFF0000), Color(0xFFFF7F00), Color(0xFFFFFF00),
                            Color(0xFF00FF00), Color(0xFF00FFFF), Color(0xFF0000FF),
                            Color(0xFF8B00FF), Color(0xFFFF00FF), Color(0xFFFF0000)
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(barWidthPx) {
                                detectTapGestures { offset ->
                                    val frac = (offset.x / barWidthPx).coerceIn(0f, 1f)
                                    val hsv = floatArrayOf(frac * 360f, 1f, 1f)
                                    val c = android.graphics.Color.HSVToColor(hsv)
                                    selectedColor = String.format("#%06X", 0xFFFFFF and c)
                                    notifyLive()
                                }
                            }
                            .pointerInput(barWidthPx) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val frac = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                                    val hsv = floatArrayOf(frac * 360f, 1f, 1f)
                                    val c = android.graphics.Color.HSVToColor(hsv)
                                    selectedColor = String.format("#%06X", 0xFFFFFF and c)
                                    notifyLive()
                                }
                            }
                    ) {
                        drawRect(brush = Brush.horizontalGradient(rainbowColors))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Background Container Pill Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Background Container Box".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = hasBackground,
                        onCheckedChange = {
                            hasBackground = it
                            notifyLive()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = YouCutOrange, checkedTrackColor = YouCutOrange.copy(alpha = 0.5f))
                    )
                }

                if (hasBackground) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Background Box Color".localize(appLanguage), color = Color.Gray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("#99000000", "#E6000000", "#99FFFFFF", "#E6FF5722", "#E600E5FF")) { bgHex ->
                            val isSel = selectedBgColor.equals(bgHex, ignoreCase = true)
                            val c = try { Color(android.graphics.Color.parseColor(bgHex)) } catch (_: Exception) { Color.Black }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(c)
                                    .border(if (isSel) 2.dp else 1.dp, if (isSel) YouCutOrange else Color.Gray, RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedBgColor = bgHex
                                        notifyLive()
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stroke / Outline
                Text("Stroke / Outline Width: ${String.format(java.util.Locale.US, "%.1f", strokeWidth)}dp".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = strokeWidth,
                    onValueChange = {
                        strokeWidth = it
                        notifyLive()
                    },
                    valueRange = 0f..6f,
                    colors = SliderDefaults.colors(thumbColor = YouCutOrange, activeTrackColor = YouCutOrange, inactiveTrackColor = Color(0xFF383848))
                )
            } else {
                // TAB 2: Animation & Timing
                Text("Animation".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(animations) { anim ->
                        val isSelected = selectedAnimation == anim
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedAnimation = anim
                                notifyLive()
                            },
                            label = { Text(anim.localize(appLanguage), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YouCutOrange,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF242430),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timeline Duration & Start Time Controls
                Text("Duration & Timeline Placement".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222230),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Start: ${String.format(java.util.Locale.US, "%.1fs", startSec)}", color = Color.White, fontSize = 11.sp)
                        Text("Duration: ${String.format(java.util.Locale.US, "%.1fs", durationSec)}", color = YouCutOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("End: ${String.format(java.util.Locale.US, "%.1fs", (startSec + durationSec).coerceAtMost(maxDuration))}", color = Color.White, fontSize = 11.sp)
                    }
                }

                Text("Start Offset (s)".localize(appLanguage), color = Color.Gray, fontSize = 10.sp)
                Slider(
                    value = startSec,
                    onValueChange = {
                        startSec = it
                        if (startSec + durationSec > maxDuration) {
                            durationSec = (maxDuration - startSec).coerceAtLeast(0.5f)
                        }
                        notifyLive()
                    },
                    valueRange = 0f..(maxDuration - 0.5f).coerceAtLeast(0f),
                    colors = SliderDefaults.colors(thumbColor = YouCutOrange, activeTrackColor = YouCutOrange, inactiveTrackColor = Color(0xFF383848))
                )

                Text("Display Duration (s)".localize(appLanguage), color = Color.Gray, fontSize = 10.sp)
                Slider(
                    value = durationSec,
                    onValueChange = {
                        durationSec = it
                        notifyLive()
                    },
                    valueRange = 0.5f..(maxDuration - startSec).coerceAtLeast(0.5f),
                    colors = SliderDefaults.colors(thumbColor = YouCutOrange, activeTrackColor = YouCutOrange, inactiveTrackColor = Color(0xFF383848))
                )
            }
        }
    }
}

/**
 * Transition Manager Studio Sheet leveraging Media3 Transition Engine:
 * - Library of transitions: None, Fade, Dissolve, Wipe Left/Right/Up/Down, Zoom In/Out, Slide, Flash White, Blur
 * - Customizable transition duration slider (0.3s - 2.0s)
 * - "Apply to All Clips" shortcut
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionStudioSheet(
    currentScene: Scene,
    allScenesCount: Int,
    appLanguage: String,
    nextScene: Scene? = null,
    onTransitionSelected: (transitionType: String, durationMs: Long) -> Unit,
    onApplyToAll: (transitionType: String, durationMs: Long) -> Unit,
    onLiveTransitionChange: ((transitionType: String, durationMs: Long) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14141E),
        tonalElevation = 10.dp
    ) {
        TransitionGallery(
            currentTransition = currentScene.transitionType,
            currentDurationMs = currentScene.transitionDurationMs,
            currentScene = currentScene,
            nextScene = nextScene,
            allScenesCount = allScenesCount,
            appLanguage = appLanguage,
            onTransitionSelected = { transType, durMs ->
                onTransitionSelected(transType, durMs)
            },
            onApplyToAll = { transType, durMs ->
                onApplyToAll(transType, durMs)
            },
            onPreviewTransition = { transType, durMs ->
                onLiveTransitionChange?.invoke(transType, durMs)
            },
            onDismiss = onDismiss
        )
    }
}

/**
 * Sticker & Emoji Studio Sheet:
 * - Rich stickers: 🔥, ❤️, ⭐, 🎬, 🚀, 💯, 💥, ✨, 👑, 🎯, 🎉, 👏, 🤩, 💖, ⚡, 🔔, 🏆, 🌟, etc.
 * - Sticker Scale slider
 * - Remove sticker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onSelectSticker: (String) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val stickers = remember {
        listOf(
            "🔥", "❤️", "⭐", "🎬", "🚀", "💯", "💥", "✨",
            "👑", "🎯", "🎉", "👏", "🤩", "💖", "⚡", "🔔",
            "🏆", "🌟", "💡", "🎵", "🕺", "💃", "🍕", "☕"
        )
    }
    var currentScale by remember { mutableFloatStateOf(currentScene.stickerScale) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stickers & Emojis".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = { onSelectSticker("None") }) {
                    Text("Remove".localize(appLanguage), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(140.dp)
            ) {
                items(stickers) { sticker ->
                    val isSelected = currentScene.stickerName == sticker
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) YouCutOrange.copy(alpha = 0.25f) else Color(0xFF242430))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) YouCutOrange else Color(0xFF383848),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectSticker(sticker) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sticker, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scale slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sticker Size".localize(appLanguage), color = Color.LightGray, fontSize = 12.sp)
                Text("${(currentScale * 100).toInt()}%", color = YouCutOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Slider(
                value = currentScale,
                onValueChange = {
                    currentScale = it
                    onScaleChanged(it)
                },
                valueRange = 0.5f..2.5f,
                colors = SliderDefaults.colors(
                    thumbColor = YouCutOrange,
                    activeTrackColor = YouCutOrange,
                    inactiveTrackColor = Color(0xFF383848)
                )
            )
        }
    }
}

/**
 * Speed Control Studio Sheet:
 * - Presets: 0.2x, 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x, 3.0x
 * - Precision slider
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onSpeedChanged: (speed: Float, applyToAll: Boolean) -> Unit,
    onSpeedCurveChanged: ((preset: String, curvePoints: String, calculatedDurationMs: Long, applyToAll: Boolean) -> Unit)? = null,
    onPreviewSpeedRamp: ((preset: String, curvePoints: String, speedMultiplier: Float) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (currentScene.speedCurvePreset != "Linear") 1 else 0) }
    var speedMultiplier by remember { mutableFloatStateOf(currentScene.speedMultiplier) }
    var applyToAll by remember { mutableStateOf(false) }
    val standardPresets = remember { listOf(0.2f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 5.0f) }

    // Speed Curve State
    var selectedCurvePreset by remember { mutableStateOf(SpeedCurvePreset.fromId(currentScene.speedCurvePreset)) }
    var curvePoints by remember {
        mutableStateOf(
            CanonicalSpeedEngine.parsePoints(currentScene.speedCurvePoints.ifEmpty { null })
        )
    }
    var selectedPointIndex by remember { mutableIntStateOf(-1) }

    // Live calculated duration for the current scene
    val baseDurationMs = currentScene.durationMs.coerceAtLeast(1000L)
    val curvedDurationMs = remember(curvePoints, selectedTab, speedMultiplier) {
        if (selectedTab == 0) {
            (baseDurationMs / speedMultiplier.coerceAtLeast(0.1f)).toLong()
        } else {
            CanonicalSpeedEngine.calculateCurvedDurationMs(baseDurationMs, curvePoints)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141E),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Speed & Time Ramping".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (selectedTab == 0) "Constant Speed (${"%.2f".format(Locale.US, speedMultiplier)}x)" else "Curve Ramping (${selectedCurvePreset.displayName})",
                            color = Color(0xFFA0A0B0),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Preview Button
                    OutlinedButton(
                        onClick = {
                            if (selectedTab == 0) {
                                onPreviewSpeedRamp?.invoke("Linear", "0.0:1.0,1.0:1.0", speedMultiplier)
                            } else {
                                val ptsStr = CanonicalSpeedEngine.serializePoints(curvePoints)
                                val avgSpeed = CanonicalSpeedEngine.getEffectiveAverageSpeed(curvePoints)
                                onPreviewSpeedRamp?.invoke(selectedCurvePreset.id, ptsStr, avgSpeed)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF87)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview".localize(appLanguage), color = Color(0xFF00FF87), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                onSpeedChanged(speedMultiplier, applyToAll)
                                onSpeedCurveChanged?.invoke("Linear", "0.0:1.0,1.0:1.0", curvedDurationMs, applyToAll)
                            } else {
                                val ptsStr = CanonicalSpeedEngine.serializePoints(curvePoints)
                                val avgSpeed = CanonicalSpeedEngine.getEffectiveAverageSpeed(curvePoints)
                                onSpeedChanged(avgSpeed, applyToAll)
                                onSpeedCurveChanged?.invoke(selectedCurvePreset.id, ptsStr, curvedDurationMs, applyToAll)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YouCutOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Apply".localize(appLanguage), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Apply To All Clips Checkbox Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (applyToAll) YouCutOrange.copy(alpha = 0.15f) else Color(0xFF1E1E2C))
                    .border(1.dp, if (applyToAll) YouCutOrange else Color(0xFF2C2C3E), RoundedCornerShape(8.dp))
                    .clickable { applyToAll = !applyToAll }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (applyToAll) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (applyToAll) YouCutOrange else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply to All Clips (सभी क्लिप्स पर लागू करें)".localize(appLanguage),
                        color = if (applyToAll) Color.White else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (applyToAll) {
                    Text("ALL".localize(appLanguage), color = YouCutOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector: Standard vs Speed Curve
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF20202E))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 0) YouCutOrange else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Standard (समान गति)".localize(appLanguage),
                        color = if (selectedTab == 0) Color.White else Color(0xFFA0A0B0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 1) YouCutOrange else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Curve ⚡ (स्पीड कर्व)".localize(appLanguage),
                            color = if (selectedTab == 1) Color.White else Color(0xFFA0A0B0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // ================= STANDARD SPEED MODE =================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Playback Rate".localize(appLanguage), color = Color.LightGray, fontSize = 13.sp)
                    Text("${"%.2f".format(Locale.US, speedMultiplier)}x", color = YouCutOrange, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Discrete Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standardPresets.forEach { preset ->
                        val isSelected = kotlin.math.abs(speedMultiplier - preset) < 0.05f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) YouCutOrange else Color(0xFF262636))
                                .clickable {
                                    speedMultiplier = preset
                                    onSpeedChanged(preset, applyToAll)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${preset}x",
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = speedMultiplier,
                    onValueChange = {
                        speedMultiplier = it
                        onSpeedChanged(it, applyToAll)
                    },
                    valueRange = 0.2f..5.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = YouCutOrange,
                        activeTrackColor = YouCutOrange,
                        inactiveTrackColor = Color(0xFF383848)
                    )
                )
            } else {
                // ================= SPEED RAMPING CURVE MODE =================
                // 1. Preset Selector Carousel
                Text("Viral Speed Curve Presets".localize(appLanguage), color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SpeedCurvePreset.entries.toTypedArray()) { preset ->
                        val isSelected = (selectedCurvePreset == preset)
                        Card(
                            modifier = Modifier
                                .width(110.dp)
                                .clickable {
                                    selectedCurvePreset = preset
                                    curvePoints = preset.defaultPoints.map { it.copy() }
                                    selectedPointIndex = -1
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF2E1A14) else Color(0xFF1E1E2A)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) YouCutOrange else Color(0xFF2E2E3E)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(preset.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.displayName.substringBefore(" ("),
                                    color = if (isSelected) YouCutOrange else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = preset.description.take(22) + "...",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Interactive Speed Curve Canvas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D14)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28283C))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Interactive Curve Editor".localize(appLanguage), color = Color(0xFF00FF87), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Drag nodes up/down (0.2x - 5.0x)".localize(appLanguage), color = Color.Gray, fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Curve Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF08080C))
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(curvePoints) {
                                        detectTapGestures { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            // Check if tapped near an existing point
                                            val touchRadiusPx = 36.dp.toPx()
                                            var foundIdx = -1
                                            curvePoints.forEachIndexed { idx, pt ->
                                                val px = pt.normalizedTimelinePos * w
                                                val py = h - ((pt.speedMultiplier - 0.2f) / (5.0f - 0.2f)).coerceIn(0f, 1f) * h
                                                val dist = kotlin.math.hypot(offset.x - px, offset.y - py)
                                                if (dist <= touchRadiusPx) {
                                                    foundIdx = idx
                                                }
                                            }
                                            if (foundIdx != -1) {
                                                selectedPointIndex = foundIdx
                                            } else {
                                                // Tap to insert new keypoint on curve
                                                val normX = (offset.x / w).coerceIn(0.05f, 0.95f)
                                                val normSpeed = (0.2f + (1f - (offset.y / h).coerceIn(0f, 1f)) * (5.0f - 0.2f)).coerceIn(0.2f, 5.0f)
                                                val newPts = curvePoints.toMutableList()
                                                newPts.add(SpeedRampPoint(normX, normSpeed))
                                                curvePoints = newPts.sortedBy { it.normalizedTimelinePos }
                                                selectedCurvePreset = SpeedCurvePreset.CUSTOM
                                            }
                                        }
                                    }
                                    .pointerInput(curvePoints) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val w = size.width.toFloat()
                                                val h = size.height.toFloat()
                                                val touchRadiusPx = 40.dp.toPx()
                                                selectedPointIndex = curvePoints.indexOfFirst { pt ->
                                                    val px = pt.normalizedTimelinePos * w
                                                    val py = h - ((pt.speedMultiplier - 0.2f) / (5.0f - 0.2f)).coerceIn(0f, 1f) * h
                                                    kotlin.math.hypot(offset.x - px, offset.y - py) <= touchRadiusPx
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                if (selectedPointIndex in curvePoints.indices) {
                                                    val w = size.width.toFloat()
                                                    val h = size.height.toFloat()
                                                    val currentPt = curvePoints[selectedPointIndex]
                                                    // First and last point stay locked at pos 0.0 and 1.0
                                                    val newX = if (selectedPointIndex == 0) 0.0f
                                                    else if (selectedPointIndex == curvePoints.size - 1) 1.0f
                                                    else (currentPt.normalizedTimelinePos + dragAmount.x / w).coerceIn(0.05f, 0.95f)

                                                    // Drag Y modulates speed between 0.2x and 5.0x
                                                    val deltaSpeed = -(dragAmount.y / h) * (5.0f - 0.2f)
                                                    val newSpeed = (currentPt.speedMultiplier + deltaSpeed).coerceIn(0.2f, 5.0f)

                                                    val updated = curvePoints.toMutableList()
                                                    updated[selectedPointIndex] = SpeedRampPoint(newX, newSpeed)
                                                    curvePoints = updated.sortedBy { it.normalizedTimelinePos }
                                                    selectedCurvePreset = SpeedCurvePreset.CUSTOM
                                                }
                                            }
                                        )
                                    }
                            ) {
                                val w = size.width
                                val h = size.height

                                // Draw speed horizontal guide lines (0.5x, 1.0x, 2.0x, 4.0x)
                                val speedGuides = listOf(0.5f, 1.0f, 2.0f, 3.5f)
                                speedGuides.forEach { spd ->
                                    val gy = h - ((spd - 0.2f) / (5.0f - 0.2f)) * h
                                    drawLine(
                                        color = if (spd == 1.0f) Color(0x66FFFFFF) else Color(0x22FFFFFF),
                                        start = androidx.compose.ui.geometry.Offset(0f, gy),
                                        end = androidx.compose.ui.geometry.Offset(w, gy),
                                        strokeWidth = if (spd == 1.0f) 1.5f else 1.0f
                                    )
                                }

                                // Draw curve path
                                val path = androidx.compose.ui.graphics.Path()
                                val fillPath = androidx.compose.ui.graphics.Path()
                                val steps = 60
                                for (i in 0..steps) {
                                    val t = i.toFloat() / steps
                                    val spd = CanonicalSpeedEngine.getSpeedAt(curvePoints, t)
                                    val px = t * w
                                    val py = h - ((spd - 0.2f) / (5.0f - 0.2f)).coerceIn(0f, 1f) * h
                                    if (i == 0) {
                                        path.moveTo(px, py)
                                        fillPath.moveTo(px, h)
                                        fillPath.lineTo(px, py)
                                    } else {
                                        path.lineTo(px, py)
                                        fillPath.lineTo(px, py)
                                    }
                                }
                                fillPath.lineTo(w, h)
                                fillPath.close()

                                // Under-curve gradient fill
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(YouCutOrange.copy(alpha = 0.35f), Color.Transparent),
                                        startY = 0f,
                                        endY = h
                                    )
                                )

                                // Main glowing curve line
                                drawPath(
                                    path = path,
                                    color = YouCutOrange,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                                )

                                // Draw control points
                                curvePoints.forEachIndexed { idx, pt ->
                                    val px = pt.normalizedTimelinePos * w
                                    val py = h - ((pt.speedMultiplier - 0.2f) / (5.0f - 0.2f)).coerceIn(0f, 1f) * h
                                    val isSelected = (idx == selectedPointIndex)

                                    if (isSelected) {
                                        drawCircle(
                                            color = YouCutOrange.copy(alpha = 0.4f),
                                            radius = 14.dp.toPx(),
                                            center = androidx.compose.ui.geometry.Offset(px, py)
                                        )
                                    }

                                    drawCircle(
                                        color = if (isSelected) Color(0xFF00FF87) else Color.White,
                                        radius = 6.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(px, py)
                                    )
                                    drawCircle(
                                        color = YouCutOrange,
                                        radius = 4.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(px, py)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls: Selected Node Info, Add/Delete Node & Reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedPointIndex in curvePoints.indices) {
                                val pt = curvePoints[selectedPointIndex]
                                Text(
                                    text = "Node #${selectedPointIndex + 1}: ${"%.2f".format(Locale.US, pt.speedMultiplier)}x @ ${(pt.normalizedTimelinePos * 100).toInt()}%",
                                    color = Color(0xFF00FF87),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedPointIndex != 0 && selectedPointIndex != curvePoints.size - 1) {
                                    TextButton(
                                        onClick = {
                                            val updated = curvePoints.toMutableList()
                                            updated.removeAt(selectedPointIndex)
                                            curvePoints = updated
                                            selectedPointIndex = -1
                                            selectedCurvePreset = SpeedCurvePreset.CUSTOM
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Delete Node".localize(appLanguage), color = Color(0xFFFF5252), fontSize = 11.sp)
                                    }
                                }
                            } else {
                                Text("Tap curve to add anchor nodes".localize(appLanguage), color = Color.Gray, fontSize = 11.sp)
                            }

                            TextButton(
                                onClick = {
                                    curvePoints = selectedCurvePreset.defaultPoints.map { it.copy() }
                                    selectedPointIndex = -1
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Curve".localize(appLanguage), color = YouCutOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (selectedPointIndex in curvePoints.indices) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Speed:".localize(appLanguage), color = Color.Gray, fontSize = 10.sp)
                                listOf(0.2f to "0.2x", 0.5f to "0.5x", 1.0f to "1.0x", 2.0f to "2.0x", 3.5f to "3.5x").forEach { (speedVal, label) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF242436))
                                            .clickable {
                                                val updated = curvePoints.toMutableList()
                                                val cur = updated[selectedPointIndex]
                                                updated[selectedPointIndex] = SpeedRampPoint(cur.normalizedTimelinePos, speedVal)
                                                curvePoints = updated
                                                selectedCurvePreset = SpeedCurvePreset.CUSTOM
                                            }
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(label, color = if (curvePoints[selectedPointIndex].speedMultiplier == speedVal) YouCutOrange else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Duration Comparison Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1E2C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C3E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Duration Impact".localize(appLanguage), color = Color.Gray, fontSize = 10.sp)
                        Text(
                            "${"%.1f".format(Locale.US, baseDurationMs / 1000f)}s ➔ ${"%.1f".format(Locale.US, curvedDurationMs / 1000f)}s",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    val diff = (baseDurationMs - curvedDurationMs) / 1000f
                    if (diff > 0.05f) {
                        Text("⚡ Faster by ${"%.1f".format(Locale.US, diff)}s", color = Color(0xFF00FF87), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else if (diff < -0.05f) {
                        Text("🐢 Slowed by ${"%.1f".format(Locale.US, -diff)}s", color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Equi-timed", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Enhance & Color Grading Sheet:
 * - Brightness, Contrast, Saturation, Warmth
 * - Auto-enhance & Reset
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhanceStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onAdjustmentsChanged: (brightness: Float, contrast: Float, saturation: Float, warmth: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var brightness by remember { mutableFloatStateOf(currentScene.brightnessValue) }
    var contrast by remember { mutableFloatStateOf(currentScene.contrastValue) }
    var saturation by remember { mutableFloatStateOf(currentScene.saturationValue) }
    var warmth by remember { mutableFloatStateOf(currentScene.warmthValue) }

    val sheetScroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(sheetScroll)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enhance & Color Grading".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = {
                        brightness = 0f
                        contrast = 1.0f
                        saturation = 1.0f
                        warmth = 0f
                        onAdjustmentsChanged(0f, 1f, 1f, 0f)
                    }
                ) {
                    Text("Reset".localize(appLanguage), color = YouCutOrange, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brightness
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Brightness".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp)
                Text("${brightness.toInt()}", color = YouCutOrange, fontSize = 11.sp)
            }
            Slider(
                value = brightness,
                onValueChange = {
                    brightness = it
                    onAdjustmentsChanged(brightness, contrast, saturation, warmth)
                },
                valueRange = -50f..50f,
                colors = SliderDefaults.colors(thumbColor = YouCutOrange, activeTrackColor = YouCutOrange, inactiveTrackColor = Color(0xFF383848))
            )

            // Contrast
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Contrast".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp)
                Text(String.format("%.1f", contrast), color = YouCutOrange, fontSize = 11.sp)
            }
            Slider(
                value = contrast,
                onValueChange = {
                    contrast = it
                    onAdjustmentsChanged(brightness, contrast, saturation, warmth)
                },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(thumbColor = YouCutOrange, activeTrackColor = YouCutOrange, inactiveTrackColor = Color(0xFF383848))
            )

            // Saturation
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Saturation".localize(appLanguage), color = Color.LightGray, fontSize = 11.sp)
                Text(String.format("%.1f", saturation), color = YouCutOrange, fontSize = 11.sp)
            }
            Slider(
                value = saturation,
                onValueChange = {
                    saturation = it
                    onAdjustmentsChanged(brightness, contrast, saturation, warmth)
                },
                valueRange = 0.0f..2.0f,
                colors = SliderDefaults.colors(thumbColor = YouCutOrange, activeTrackColor = YouCutOrange, inactiveTrackColor = Color(0xFF383848))
            )
        }
    }
}

/**
 * Background & Canvas Aspect Ratio Studio Sheet:
 * - 9:16 (Reels/TikTok), 16:9 (YouTube), 1:1 (Instagram), 4:5, 3:4
 * - Blur video background, Solid colors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BgStudioSheet(
    activeAspectRatio: String,
    appLanguage: String,
    onRatioSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ratios = remember {
        listOf(
            Triple("9:16", "Reels / TikTok", Icons.Default.StayCurrentPortrait),
            Triple("16:9", "YouTube Landscape", Icons.Default.StayCurrentLandscape),
            Triple("1:1", "Square Feed", Icons.Default.CropSquare),
            Triple("4:5", "Portrait Post", Icons.Default.AspectRatio),
            Triple("3:4", "Standard Frame", Icons.Default.ViewCompact)
        )
    }

    val sheetScroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(sheetScroll)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Canvas Background & Ratio".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = YouCutOrange)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Select Aspect Ratio:".localize(appLanguage), color = Color.LightGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ratios) { (ratio, label, icon) ->
                    val isSelected = activeAspectRatio == ratio
                    Card(
                        modifier = Modifier
                            .width(100.dp)
                            .height(85.dp)
                            .clickable { onRatioSelected(ratio) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) YouCutOrange.copy(alpha = 0.25f) else Color(0xFF262636)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) YouCutOrange else Color(0xFF383848)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = if (isSelected) YouCutOrange else Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ratio, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(label, color = Color.Gray, fontSize = 8.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Voiceover Recording Studio Sheet:
 * - Real-time Voiceover recorder with live mic waveform
 * - Save directly to scene narration!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorderSheet(
    currentScene: Scene,
    selectedSceneIndex: Int,
    appLanguage: String,
    onVoiceRecorded: (audioFilePath: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isRecording by VoiceRecorderManager.isRecording.collectAsState()
    val recordingDuration by VoiceRecorderManager.recordingDurationSeconds.collectAsState()
    val recordedFile by VoiceRecorderManager.recordedAudioFile.collectAsState()
    val isPlaying by VoiceRecorderManager.isPlaying.collectAsState()

    val sheetScroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = {
            if (isRecording) {
                VoiceRecorderManager.stopRecording(discard = true)
            }
            onDismiss()
        },
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(sheetScroll),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record Voiceover".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recording Timer
            Text(
                text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                color = if (isRecording) YouCutOrange else Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Record Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) MaterialTheme.colorScheme.error else YouCutOrange)
                    .clickable {
                        if (isRecording) {
                            VoiceRecorderManager.stopRecording(discard = false)
                        } else {
                            VoiceRecorderManager.startRecording(context)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRecording) "Recording... Tap to stop".localize(appLanguage) else "Tap mic to start recording voiceover".localize(appLanguage),
                color = Color.LightGray,
                fontSize = 11.sp
            )

            // Playback & Save
            if (recordedFile != null && !isRecording) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isPlaying) {
                                VoiceRecorderManager.stopPlayback()
                            } else {
                                recordedFile?.let { VoiceRecorderManager.playRecordedAudio(context, it) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlaying) "Pause" else "Preview", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            recordedFile?.let { file ->
                                onVoiceRecorded(file.absolutePath)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = YouCutOrange)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply to Scene".localize(appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Volume Control Studio Sheet:
 * - 0% to 200% volume slider
 * - Mute switch
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onVolumeChanged: (Float) -> Unit,
    bgmVolume: Float = 1.0f,
    onBgmVolumeChanged: ((Float) -> Unit)? = null,
    customAudioTrackName: String? = null,
    onPickCustomAudio: (() -> Unit)? = null,
    onApplyVideoVolumeToAll: ((Float) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var volumePercent by remember(currentScene.volume) { mutableFloatStateOf((currentScene.volume * 100f).coerceIn(0f, 200f)) }
    var bgmPercent by remember(bgmVolume) { mutableFloatStateOf((bgmVolume * 100f).coerceIn(0f, 200f)) }
    val sheetScroll = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141C),
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("volume_studio_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(sheetScroll)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Equalizer, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Multi-Track Audio Mixer".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Adjust video sound and background music independently".localize(appLanguage), color = Color.Gray, fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = YouCutOrange)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track 1: Original Video Audio
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2A)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, YouCutOrange.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Track 1: Original Video Audio".localize(appLanguage), color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (volumePercent == 0f) Color(0xFF4A1A1A) else YouCutOrange.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (volumePercent == 0f) "Muted (0%)" else "${volumePercent.toInt()}%",
                                color = if (volumePercent == 0f) Color(0xFFFF5252) else YouCutOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = volumePercent,
                        onValueChange = {
                            volumePercent = it
                            onVolumeChanged(it / 100f)
                        },
                        valueRange = 0f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = YouCutOrange,
                            activeTrackColor = YouCutOrange,
                            inactiveTrackColor = Color(0xFF383848)
                        ),
                        modifier = Modifier.testTag("video_volume_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                volumePercent = 0f
                                onVolumeChanged(0f)
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (volumePercent == 0f) Color(0xFFFF5252) else Color.LightGray),
                            border = BorderStroke(0.8.dp, if (volumePercent == 0f) Color(0xFFFF5252) else Color(0xFF3E3E50))
                        ) {
                            Text("Mute (0%)", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                volumePercent = 50f
                                onVolumeChanged(0.5f)
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            border = BorderStroke(0.8.dp, Color(0xFF3E3E50))
                        ) {
                            Text("50%", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                volumePercent = 100f
                                onVolumeChanged(1f)
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = YouCutOrange),
                            border = BorderStroke(0.8.dp, YouCutOrange.copy(alpha = 0.8f))
                        ) {
                            Text("100%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                volumePercent = 200f
                                onVolumeChanged(2f)
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = YouCutOrange),
                            border = BorderStroke(0.8.dp, Color(0xFF3E3E50))
                        ) {
                            Text("200%", fontSize = 10.sp)
                        }
                    }

                    if (onApplyVideoVolumeToAll != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { onApplyVideoVolumeToAll(volumePercent / 100f) },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apply to All Video Clips".localize(appLanguage), fontSize = 11.sp, color = YouCutOrange)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track 2: Background Music (BGM)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF182232)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF29B6F6).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Track 2: Background Music (BGM)".localize(appLanguage), color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (bgmPercent == 0f) Color(0xFF4A1A1A) else Color(0xFF29B6F6).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (bgmPercent == 0f) "Muted (0%)" else "${bgmPercent.toInt()}%",
                                color = if (bgmPercent == 0f) Color(0xFFFF5252) else Color(0xFF29B6F6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (!customAudioTrackName.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F1722))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = customAudioTrackName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (onPickCustomAudio != null) {
                                TextButton(
                                    onClick = onPickCustomAudio,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Replace", fontSize = 10.5.sp, color = Color(0xFF29B6F6))
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { onPickCustomAudio?.invoke() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Local Music File (.mp3, .wav)".localize(appLanguage), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = bgmPercent,
                        onValueChange = {
                            bgmPercent = it
                            onBgmVolumeChanged?.invoke(it / 100f)
                        },
                        valueRange = 0f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF29B6F6),
                            activeTrackColor = Color(0xFF29B6F6),
                            inactiveTrackColor = Color(0xFF383848)
                        ),
                        modifier = Modifier.testTag("bgm_volume_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // BGM Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                bgmPercent = 0f
                                onBgmVolumeChanged?.invoke(0f)
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (bgmPercent == 0f) Color(0xFFFF5252) else Color.LightGray),
                            border = BorderStroke(0.8.dp, if (bgmPercent == 0f) Color(0xFFFF5252) else Color(0xFF3E3E50))
                        ) {
                            Text("Mute (0%)", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                bgmPercent = 30f
                                onBgmVolumeChanged?.invoke(0.3f)
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            border = BorderStroke(0.8.dp, Color(0xFF3E3E50))
                        ) {
                            Text("30%", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                bgmPercent = 100f
                                onBgmVolumeChanged?.invoke(1f)
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF29B6F6)),
                            border = BorderStroke(0.8.dp, Color(0xFF29B6F6).copy(alpha = 0.8f))
                        ) {
                            Text("100%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                bgmPercent = 200f
                                onBgmVolumeChanged?.invoke(2f)
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF29B6F6)),
                            border = BorderStroke(0.8.dp, Color(0xFF3E3E50))
                        ) {
                            Text("200%", fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-Track Balance Indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.8.dp, Color(0xFF2A2A3A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Independent Mixing Active".localize(appLanguage),
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "Video: ${volumePercent.toInt()}% • BGM: ${bgmPercent.toInt()}%",
                        color = YouCutOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Crop / Zoom Framing Studio Sheet:
 * - Scale factor from 1.0x to 2.5x
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropStudioSheet(
    appLanguage: String,
    onDismiss: () -> Unit
) {
    var zoomFactor by remember { mutableFloatStateOf(1.0f) }

    val sheetScroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(sheetScroll)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Crop, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crop & Zoom Framing".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Text(String.format("%.1fx", zoomFactor), color = YouCutOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = zoomFactor,
                onValueChange = { zoomFactor = it },
                valueRange = 1.0f..2.5f,
                colors = SliderDefaults.colors(
                    thumbColor = YouCutOrange,
                    activeTrackColor = YouCutOrange,
                    inactiveTrackColor = Color(0xFF383848)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = YouCutOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm Framing".localize(appLanguage), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Chroma Key Studio Sheet:
 * Real-time color keying cutout (Green screen, Blue screen, Custom color, Sensitivity & Spill suppression).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaKeyStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onChromaChanged: (isEnabled: Boolean, color: String, sensitivity: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(currentScene.isChromaKeyEnabled) }
    var selectedColor by remember { mutableStateOf(if (currentScene.chromaKeyColor.isBlank()) "#00FF00" else currentScene.chromaKeyColor) }
    var sensitivity by remember { mutableFloatStateOf(if (currentScene.chromaKeySensitivity <= 0f) 0.4f else currentScene.chromaKeySensitivity) }

    val keyPresets = remember {
        listOf(
            Triple("Green Screen", "#00FF00", Color(0xFF00FF00)),
            Triple("Blue Screen", "#0000FF", Color(0xFF2979FF)),
            Triple("Cyan Screen", "#00FFFF", Color(0xFF00E5FF)),
            Triple("Magenta", "#FF00FF", Color(0xFFFF00FF)),
            Triple("Black", "#000000", Color(0xFF111111)),
            Triple("White", "#FFFFFF", Color(0xFFEEEEEE))
        )
    }

    val sheetScroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(sheetScroll)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chroma Key Cutout".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        onChromaChanged(isEnabled, selectedColor, sensitivity)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = YouCutOrange
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Key Color Preset".localize(appLanguage), color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                keyPresets.forEach { (label, hex, col) ->
                    val isCur = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(
                                width = if (isCur) 3.dp else 1.dp,
                                color = if (isCur) YouCutOrange else Color(0xFF444455),
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColor = hex
                                isEnabled = true
                                onChromaChanged(true, hex, sensitivity)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCur) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = if (hex == "#FFFFFF") Color.Black else Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sensitivity Threshold".localize(appLanguage), color = Color.White, fontSize = 13.sp)
                Text(String.format("%.0f%%", sensitivity * 100f), color = YouCutOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Slider(
                value = sensitivity,
                onValueChange = {
                    sensitivity = it
                    if (isEnabled) {
                        onChromaChanged(true, selectedColor, it)
                    }
                },
                valueRange = 0.05f..0.95f,
                colors = SliderDefaults.colors(
                    thumbColor = YouCutOrange,
                    activeTrackColor = YouCutOrange,
                    inactiveTrackColor = Color(0xFF383848)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = YouCutOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply Chroma Key".localize(appLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Mask Studio Sheet:
 * Shape masking (None, Linear, Mirror, Radial Circle, Rectangle, Heart) with feather and invert.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onMaskChanged: (maskShape: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedShape by remember { mutableStateOf(if (currentScene.maskShape.isBlank()) "None" else currentScene.maskShape) }

    val maskOptions = remember {
        listOf(
            Pair("None", Icons.Default.Block),
            Pair("Linear", Icons.Default.Splitscreen),
            Pair("Mirror", Icons.Default.ViewAgenda),
            Pair("Circle", Icons.Default.Circle),
            Pair("Rectangle", Icons.Default.CropLandscape),
            Pair("Heart", Icons.Default.Favorite)
        )
    }

    val sheetScroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(sheetScroll)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterFrames, contentDescription = null, tint = YouCutOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mask Shapes".localize(appLanguage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Text(selectedShape, color = YouCutOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                maskOptions.forEach { (shape, icon) ->
                    val isCur = selectedShape.equals(shape, ignoreCase = true)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCur) YouCutOrange else Color(0xFF242434))
                            .clickable {
                                selectedShape = shape
                                onMaskChanged(shape)
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = shape,
                            tint = if (isCur) Color.White else Color.LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shape,
                            color = if (isCur) Color.White else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = YouCutOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm Mask".localize(appLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
