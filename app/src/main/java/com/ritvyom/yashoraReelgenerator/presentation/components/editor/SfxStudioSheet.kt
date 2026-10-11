package com.ritvyom.yashoraReelgenerator.presentation.components.editor

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import com.ritvyom.yashoraReelgenerator.presentation.utils.SfxMetadata
import com.ritvyom.yashoraReelgenerator.presentation.utils.SoundSynth
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SfxStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onSelectSfx: (name: String, category: String, volume: Float) -> Unit,
    onRemoveSfx: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Trending") }
    var selectedSfxName by remember { mutableStateOf(currentScene.sfxName.ifEmpty { "None" }) }
    var currentlyPlayingName by remember { mutableStateOf<String?>(null) }
    var sfxVolume by remember { mutableFloatStateOf(currentScene.layeredSfxVolume.coerceIn(0.2f, 2.0f)) }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val categories = remember { SoundSynth.SFX_CATEGORIES }
    val displayedSfxList = remember(selectedCategory, searchQuery) {
        val base = SoundSynth.getSfxByCategory(selectedCategory)
        if (searchQuery.isBlank()) {
            base
        } else {
            SoundSynth.ALL_SFX.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val sheetScroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141E),
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Trending SFX Library".localize(appLanguage),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Cinematic Whooshes, Risers, Impacts & Social Pops".localize(appLanguage),
                            color = Color(0xFFA0A0B0),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color(0xFFFF5722), modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search sound effects (whoosh, boom, pop...)", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF5722),
                    unfocusedBorderColor = Color(0xFF2E2E3E),
                    focusedContainerColor = Color(0xFF1E1E2C),
                    unfocusedContainerColor = Color(0xFF1E1E2C)
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Categories Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat && searchQuery.isEmpty()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFFFF5722) else Color(0xFF222230),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFFF5722) else Color(0xFF323242)
                        ),
                        modifier = Modifier.clickable {
                            selectedCategory = cat
                            searchQuery = ""
                        }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else Color(0xFFC0C0D0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active SFX status bar
            if (selectedSfxName != "None" && selectedSfxName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00FF87).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF00FF87)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active: $selectedSfxName", color = Color(0xFF00FF87), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Remove",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                selectedSfxName = "None"
                                onRemoveSfx()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // SFX Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                items(displayedSfxList) { sfx ->
                    val isSelected = selectedSfxName == sfx.name
                    val isPlaying = currentlyPlayingName == sfx.name

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSfxName = sfx.name
                                currentlyPlayingName = sfx.name
                                onSelectSfx(sfx.name, sfx.category, sfxVolume)
                                coroutineScope.launch {
                                    SoundSynth.playSfx(sfx.name, sfxVolume)
                                    currentlyPlayingName = null
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFFF5722).copy(alpha = 0.22f) else Color(0xFF1E1E2C)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFFF5722) else Color(0xFF2C2C3E)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPlaying) Color(0xFF00FF87) else Color(0xFFFF5722).copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isPlaying) Color.Black else Color(0xFFFF5722),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(sfx.durationText, color = Color.Gray, fontSize = 9.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(sfx.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(sfx.description, color = Color(0xFFA0A0B0), fontSize = 9.5.sp, maxLines = 2, lineHeight = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Volume Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SFX Volume Gain".localize(appLanguage), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${(sfxVolume * 100).toInt()}%", color = Color(0xFFFF5722), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = sfxVolume,
                onValueChange = {
                    sfxVolume = it
                    if (selectedSfxName != "None") {
                        onSelectSfx(selectedSfxName, selectedCategory, it)
                    }
                },
                valueRange = 0.2f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF5722),
                    activeTrackColor = Color(0xFFFF5722),
                    inactiveTrackColor = Color(0xFF383848)
                )
            )
        }
    }
}
