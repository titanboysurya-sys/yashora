package com.ritvyom.yashoraReelgenerator.presentation.components.editor

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.ritvyom.yashoraReelgenerator.presentation.utils.VideoSpeechTranscriptionEngine
import kotlinx.coroutines.launch
import android.widget.Toast
import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import com.ritvyom.yashoraReelgenerator.presentation.utils.EditorConstants
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize
import kotlinx.coroutines.delay

data class CaptionStylePreset(
    val id: String,
    val name: String,
    val highlightColor: String,
    val textColor: String,
    val bgColor: String,
    val font: String,
    val animation: String,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicCaptionsStudioSheet(
    currentScene: Scene,
    appLanguage: String,
    onSaveCaptions: (
        subtitle: String,
        stylePreset: String,
        highlightColor: String,
        textColor: String,
        font: String,
        animation: String,
        applyToAll: Boolean,
        srtCaptions: String
    ) -> Unit,
    onTranscribeSceneAudio: (suspend (preferredLanguage: String, onProgress: (String) -> Unit) -> VideoSpeechTranscriptionEngine.TranscriptionResult)? = null,
    onTranscribeAllVideos: (suspend (preferredLanguage: String, onProgress: (String) -> Unit) -> Pair<Int, Int>)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaPath = currentScene.mediaPath ?: currentScene.remoteUrl ?: ""
    val hasVoiceAudio = currentScene.customVoiceAudioPath?.isNotBlank() == true
    val isVideo = currentScene.mediaType == "VIDEO" || mediaPath.endsWith(".mp4", ignoreCase = true) || mediaPath.endsWith(".mov", ignoreCase = true)

    var isTranscribing by remember { mutableStateOf(false) }
    var transcriptionPhase by remember { mutableStateOf("") }
    var transcriptionError by remember { mutableStateOf<String?>(null) }
    var transcriptionSuccessMsg by remember { mutableStateOf<String?>(null) }
    var selectedTranscribeLang by remember { mutableStateOf("Auto") }
    var activeTranscriptionJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var subtitleText by remember { mutableStateOf(currentScene.subtitle.ifEmpty { currentScene.narrationText.take(90) }) }
    var srtText by remember { mutableStateOf(currentScene.srtCaptions) }
    var activeCaptionsTab by remember { mutableIntStateOf(0) }

    fun cancelTranscription() {
        activeTranscriptionJob?.cancel()
        activeTranscriptionJob = null
        isTranscribing = false
        transcriptionPhase = ""
        transcriptionError = "Transcription cancelled."
        Toast.makeText(context, "Transcription cancelled".localize(appLanguage), Toast.LENGTH_SHORT).show()
    }

    fun startTranscription() {
        val audioSource = when {
            !currentScene.customVoiceAudioPath.isNullOrBlank() -> currentScene.customVoiceAudioPath!!
            mediaPath.isNotBlank() -> mediaPath
            else -> ""
        }
        if (audioSource.isBlank()) {
            transcriptionError = "No audio or video found for this clip to transcribe."
            return
        }
        activeTranscriptionJob?.cancel()
        isTranscribing = true
        transcriptionError = null
        transcriptionSuccessMsg = null
        transcriptionPhase = "🎧 Extracting audio track from media..."

        activeTranscriptionJob = coroutineScope.launch {
            try {
                val result = onTranscribeSceneAudio?.invoke(selectedTranscribeLang) { phase ->
                    transcriptionPhase = phase
                } ?: VideoSpeechTranscriptionEngine.transcribeVideoAudio(
                    context = context,
                    mediaPath = audioSource,
                    preferredLanguage = selectedTranscribeLang,
                    maxDurationMs = (currentScene.durationSeconds * 1000L).coerceAtLeast(30000L),
                    onProgress = { phase -> transcriptionPhase = phase }
                )

                if (result.isSuccess && (result.fullTranscript.isNotBlank() || result.srtContent.isNotBlank())) {
                    subtitleText = result.fullTranscript
                    if (result.srtContent.isNotBlank()) {
                        srtText = result.srtContent
                    }
                    transcriptionSuccessMsg = "✓ Transcribed (${result.detectedLanguage}): \"${result.fullTranscript.take(50)}...\""
                    Toast.makeText(context, "✓ Speech transcribed to captions & SRT!".localize(appLanguage), Toast.LENGTH_SHORT).show()
                    activeCaptionsTab = 0
                } else {
                    transcriptionError = result.errorMessage ?: "Failed transcribing speech"
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                transcriptionError = "Transcription cancelled."
            } catch (e: Exception) {
                transcriptionError = e.message ?: "Failed transcribing speech"
            } finally {
                isTranscribing = false
                activeTranscriptionJob = null
            }
        }
    }

    fun startTranscribingAll() {
        if (isTranscribing) return
        activeTranscriptionJob?.cancel()
        isTranscribing = true
        transcriptionError = null
        transcriptionSuccessMsg = null
        transcriptionPhase = "🌐 Transcribing all video clips in timeline..."

        activeTranscriptionJob = coroutineScope.launch {
            try {
                if (onTranscribeAllVideos != null) {
                    val (success, total) = onTranscribeAllVideos.invoke(selectedTranscribeLang) { phase ->
                        transcriptionPhase = phase
                    }
                    if (success > 0) {
                        transcriptionSuccessMsg = "✓ Transcribed $success of $total video clips!"
                        Toast.makeText(context, "✓ $success video clips transcribed!".localize(appLanguage), Toast.LENGTH_SHORT).show()
                        activeCaptionsTab = 0
                    } else {
                        transcriptionError = "No speech detected across video clips"
                    }
                } else {
                    startTranscription()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                transcriptionError = "Transcription cancelled."
            } catch (e: Exception) {
                transcriptionError = e.message ?: "Failed transcribing clips"
            } finally {
                isTranscribing = false
                activeTranscriptionJob = null
            }
        }
    }

    var selectedPreset by remember { mutableStateOf(currentScene.dynamicCaptionStyle.ifEmpty { "Hormozi" }) }
    var highlightColorHex by remember { mutableStateOf(currentScene.dynamicCaptionHighlightColor.ifEmpty { "#FFEB3B" }) }
    var textColorHex by remember { mutableStateOf(currentScene.subtitleColor.ifEmpty { "#FFFFFF" }) }
    var selectedFont by remember { mutableStateOf(currentScene.captionFont.ifEmpty { "TikTok Style" }) }
    var selectedAnim by remember { mutableStateOf(currentScene.dynamicCaptionAnimation.ifEmpty { "WordByWord" }) }
    var applyToAllScenes by remember { mutableStateOf(false) }

    // Live preview simulated word index for Karaoke effect
    var activePreviewWordIndex by remember { mutableIntStateOf(0) }
    val allWords = remember(subtitleText) {
        subtitleText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .ifEmpty { listOf("Viral", "Instagram", "Reels", "Auto", "Captions") }
    }
    val previewWords = remember(allWords) {
        allWords.take(5)
    }

    LaunchedEffect(previewWords) {
        while (true) {
            delay(400)
            activePreviewWordIndex = (activePreviewWordIndex + 1) % previewWords.size.coerceAtLeast(1)
        }
    }

    val presets = remember {
        listOf(
            CaptionStylePreset("Hormozi", "⚡ Hormozi", "#FFEB3B", "#FFFFFF", "#CC000000", "TikTok Style", "WordByWord", "🔥 Viral"),
            CaptionStylePreset("Neon Glow", "✨ Neon Glow", "#00F0FF", "#FFFFFF", "#B312002A", "Display Bold", "GlowPulse", "Popular"),
            CaptionStylePreset("Bounce Pop", "🏀 Bounce Pop", "#00FF87", "#FFFFFF", "#CC000000", "TikTok Style", "BouncePop", "🔥 Viral"),
            CaptionStylePreset("Red Hot", "🔥 Red Hot", "#FF1744", "#FFF8E1", "#CC1B0000", "Display Bold", "BoxHighlight", "Trending"),
            CaptionStylePreset("Cyber Wave", "🟣 Cyber Wave", "#E040FB", "#80D8FF", "#B31C0B38", "Monospace", "GlowPulse", "Cyberpunk"),
            CaptionStylePreset("Emerald Pop", "🟩 Emerald", "#00E676", "#FFFFFF", "#CC001A0A", "TikTok Style", "WordByWord", "Clean"),
            CaptionStylePreset("Classic Clean", "📄 Clean Box", "#FFFFFF", "#FFFFFF", "#99000000", "Sans-Serif", "WordByWord", "Minimal")
        )
    }

    val highlightColors = remember {
        listOf("#FFEB3B", "#00F0FF", "#00FF87", "#FF1744", "#FF9100", "#E040FB", "#FFFFFF")
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
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Dynamic Auto-Captions".localize(appLanguage),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Gemini SRT Subtitles & Karaoke Word Highlights".localize(appLanguage),
                            color = Color(0xFFA0A0B0),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val finalSrt = if (srtText.isNotBlank()) {
                            val parsed = VideoSpeechTranscriptionEngine.parseSrt(srtText)
                            val subdivided = VideoSpeechTranscriptionEngine.subdivideLongSegments(parsed)
                            VideoSpeechTranscriptionEngine.segmentsToSrt(subdivided)
                        } else if (subtitleText.isNotBlank()) {
                            val totalSec = if (currentScene.durationMs > 0) currentScene.durationMs / 1000f else currentScene.durationSeconds.toFloat().coerceAtLeast(1f)
                            val segs = VideoSpeechTranscriptionEngine.splitTextIntoTimedSegments(subtitleText, totalSec)
                            val subdivided = VideoSpeechTranscriptionEngine.subdivideLongSegments(segs)
                            VideoSpeechTranscriptionEngine.segmentsToSrt(subdivided)
                        } else ""

                        onSaveCaptions(
                            subtitleText,
                            selectedPreset,
                            highlightColorHex,
                            textColorHex,
                            selectedFont,
                            selectedAnim,
                            applyToAllScenes,
                            finalSrt
                        )
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Apply", tint = Color(0xFFFF5722), modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Studio Tabs: Add / Edit Caption vs Generate AI Captions vs SRT Timeline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E2C), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Add / Edit Caption
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeCaptionsTab = 0 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeCaptionsTab == 0) Color(0xFFFF5722) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (activeCaptionsTab == 0) Color.White else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add / Edit".localize(appLanguage),
                            color = if (activeCaptionsTab == 0) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tab 1: Generate AI Captions
                Surface(
                    modifier = Modifier
                        .weight(1.15f)
                        .clickable { activeCaptionsTab = 1 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeCaptionsTab == 1) Color(0xFF00FF87) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = if (activeCaptionsTab == 1) Color.Black else Color(0xFF00FF87),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Generate".localize(appLanguage),
                            color = if (activeCaptionsTab == 1) Color.Black else Color(0xFF00FF87),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Tab 2: SRT Subtitles
                Surface(
                    modifier = Modifier
                        .weight(0.9f)
                        .clickable { activeCaptionsTab = 2 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeCaptionsTab == 2) Color(0xFF00F0FF) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SRT Timeline",
                            color = if (activeCaptionsTab == 2) Color.Black else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (srtText.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (activeCaptionsTab == 2) Color.Black else Color(0xFF00F0FF))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeCaptionsTab) {
                0 -> {
                    // TAB 0: ADD / EDIT CAPTIONS & STYLING
                    // Live Interactive Karaoke Preview Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0A0A10),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C3E))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LIVE PREVIEW (रीयल-टाइम प्रिव्यू)", color = Color(0xFF00FF87), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(selectedPreset, color = Color(0xFFFF5722), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Animated Caption Line
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                previewWords.forEachIndexed { idx, word ->
                                    val isActive = (idx == activePreviewWordIndex)
                                    val wordScale = if (isActive && selectedAnim == "BouncePop") 1.25f else 1.0f
                                    val curFontFamily = when (selectedFont) {
                                        "Sans-Serif" -> FontFamily.SansSerif
                                        "Serif" -> FontFamily.Serif
                                        "Monospace" -> FontFamily.Monospace
                                        else -> FontFamily.Default
                                    }

                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp, vertical = 2.dp)
                                            .scale(wordScale)
                                            .then(
                                                if (isActive && selectedAnim == "BoxHighlight") {
                                                    Modifier
                                                        .background(Color(android.graphics.Color.parseColor(highlightColorHex)), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                } else Modifier
                                            )
                                    ) {
                                        Text(
                                            text = word.uppercase(),
                                            color = if (isActive) {
                                                if (selectedAnim == "BoxHighlight") Color.Black
                                                else Color(android.graphics.Color.parseColor(highlightColorHex))
                                            } else Color(android.graphics.Color.parseColor(textColorHex)).copy(alpha = 0.85f),
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = curFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Suggestion Card to switch to AI Generate if subtitles empty
                    if (subtitleText.isBlank() && srtText.isBlank() && (isVideo || hasVoiceAudio)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF161228),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF87).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeCaptionsTab = 1 }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Hearing, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Auto-Transcribe Speech into Captions 🎙️".localize(appLanguage), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Listen to video speech and generate synced subtitles".localize(appLanguage), color = Color(0xFFA5B4FC), fontSize = 10.sp)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Subtitle Text Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subtitle / Caption Text".localize(appLanguage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (subtitleText.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    val totalSec = if (currentScene.durationMs > 0) currentScene.durationMs / 1000f else currentScene.durationSeconds.toFloat().coerceAtLeast(1f)
                                    val segs = VideoSpeechTranscriptionEngine.splitTextIntoTimedSegments(subtitleText, totalSec)
                                    val subdivided = VideoSpeechTranscriptionEngine.subdivideLongSegments(segs)
                                    srtText = VideoSpeechTranscriptionEngine.segmentsToSrt(subdivided)
                                    Toast.makeText(context, "⚡ Converted to Timed Karaoke SRT!".localize(appLanguage), Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync to SRT", color = Color(0xFF00FF87), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = subtitleText,
                        onValueChange = { subtitleText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter subtitle text...", color = Color.Gray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF5722),
                            unfocusedBorderColor = Color(0xFF383848),
                            focusedContainerColor = Color(0xFF1E1E2C),
                            unfocusedContainerColor = Color(0xFF1E1E2C)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset Styles Row
                    Text("Trending Viral Presets".localize(appLanguage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { preset ->
                            val isSelected = selectedPreset == preset.id
                            Card(
                                modifier = Modifier
                                    .clickable {
                                        selectedPreset = preset.id
                                        highlightColorHex = preset.highlightColor
                                        textColorHex = preset.textColor
                                        selectedFont = preset.font
                                        selectedAnim = preset.animation
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFF5722).copy(alpha = 0.25f) else Color(0xFF222230)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isSelected) Color(0xFFFF5722) else Color(0xFF343444)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(preset.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(preset.tag, color = if (isSelected) Color(0xFFFF5722) else Color(0xFF00FF87), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Highlight Color Selection
                    Text("Active Word Highlight Color".localize(appLanguage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        highlightColors.forEach { hex ->
                            val isSelected = highlightColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { highlightColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animation Style Chips
                    Text("Highlight Animation".localize(appLanguage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("WordByWord", "Karaoke"),
                            Pair("BouncePop", "Bounce Pop"),
                            Pair("BoxHighlight", "Box Wrap"),
                            Pair("GlowPulse", "Neon Glow")
                        ).forEach { (animId, label) ->
                            val isSelected = selectedAnim == animId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFF5722) else Color(0xFF222230),
                                modifier = Modifier.clickable { selectedAnim = animId }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: GENERATE AI CAPTIONS FROM VIDEO / AUDIO SPEECH
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_caption_transcribe_card"),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF161226),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            Brush.linearGradient(listOf(Color(0xFF00FF87), Color(0xFF00F0FF)))
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00FF87).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Hearing,
                                            contentDescription = null,
                                            tint = Color(0xFF00FF87),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Auto Caption Speech & Narration".localize(appLanguage),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Transcribes video speech into synchronized SRT captions".localize(appLanguage),
                                            color = Color(0xFFA5B4FC),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Badges indicating media detected
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (hasVoiceAudio) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFF5722).copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF5722))
                                    ) {
                                        Text(
                                            text = "🎙️ Narration Audio Detected",
                                            color = Color(0xFFFF8A65),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (isVideo) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF00F0FF).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00F0FF))
                                    ) {
                                        Text(
                                            text = "🎬 ${currentScene.durationSeconds}s Video Clip",
                                            color = Color(0xFF00F0FF),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Language Selector
                            Text(
                                text = "Spoken Speech Language in Video:".localize(appLanguage),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Auto", "Hindi", "Hinglish", "English").forEach { lang ->
                                    val isSel = selectedTranscribeLang == lang
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFF00FF87) else Color(0xFF221A38),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSel) Color(0xFF00FF87) else Color(0xFF382A5C)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedTranscribeLang = lang }
                                    ) {
                                        Text(
                                            text = lang,
                                            color = if (isSel) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isTranscribing) {
                                // Live Progress State with Prominent Cancel Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF100A20),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF87).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = Color(0xFF00FF87),
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = transcriptionPhase,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Prominent Red Cancel Button
                                        Button(
                                            onClick = { cancelTranscription() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("cancel_caption_generation_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancel",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Cancel Generation".localize(appLanguage),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Action Buttons: Transcribe This Clip & Transcribe All Clips
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { startTranscription() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("transcribe_video_speech_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Hearing,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (hasVoiceAudio && !isVideo) "🎙️ Transcribe Voiceover Narration".localize(appLanguage) else "🎙️ Generate Captions for This Clip".localize(appLanguage),
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    if (onTranscribeAllVideos != null) {
                                        OutlinedButton(
                                            onClick = { startTranscribingAll() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("transcribe_all_videos_button"),
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF))
                                        ) {
                                            Text(
                                                text = "🌐 Transcribe All Clips in Timeline".localize(appLanguage),
                                                color = Color(0xFF00F0FF),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Success Feedback message
                            transcriptionSuccessMsg?.let { success ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF00FF87).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF87))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = success,
                                            color = Color(0xFF00FF87),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Error Feedback message
                            transcriptionError?.let { err ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFF1744).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF1744))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF1744), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = err,
                                            color = Color(0xFFFF8A80),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                // SRT Subtitle Studio Tab
                val parsedSegments = remember(srtText) {
                    VideoSpeechTranscriptionEngine.parseSrt(srtText)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF100A20),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF87).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Subtitles, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SRT SUBTITLE TIMELINE",
                                    color = Color(0xFF00FF87),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF00FF87).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${parsedSegments.size} Segments",
                                    color = Color(0xFF00FF87),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Millisecond-accurate timecodes overlaying scenes during playback & export.",
                            color = Color(0xFFA5B4FC),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SRT Actions: Copy, Sync to Subtitle, Generate from Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("SRT Subtitles", srtText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "📋 SRT copied to clipboard!".localize(appLanguage), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy SRT", color = Color(0xFF00F0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (parsedSegments.isNotEmpty()) {
                                subtitleText = parsedSegments.joinToString(" ") { it.text }
                                Toast.makeText(context, "⚡ Synced SRT to Subtitle text!".localize(appLanguage), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No SRT segments to sync", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Text", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (subtitleText.isNotBlank()) {
                                val totalSec = if (currentScene.durationMs > 0) currentScene.durationMs / 1000f else currentScene.durationSeconds.toFloat().coerceAtLeast(1f)
                                val segs = VideoSpeechTranscriptionEngine.splitTextIntoTimedSegments(subtitleText, totalSec)
                                val subdivided = VideoSpeechTranscriptionEngine.subdivideLongSegments(segs)
                                srtText = VideoSpeechTranscriptionEngine.segmentsToSrt(subdivided)
                                Toast.makeText(context, "✓ Formatted SRT into timed 3-4 word captions!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF87))
                    ) {
                        Icon(Icons.Default.FormatAlignLeft, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-SRT", color = Color(0xFF00FF87), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SRT Raw Multi-line Editor
                Text("SRT Content & Timecodes (SRT टाइमकोड्स)".localize(appLanguage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = srtText,
                    onValueChange = { srtText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 260.dp),
                    placeholder = {
                        Text(
                            "1\n00:00:00,000 --> 00:00:02,500\nCaption line text...\n\n2\n00:00:02,500 --> 00:00:05,000\nNext caption line...",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF00FF87)
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF00FF87),
                        unfocusedTextColor = Color(0xFF00FF87),
                        focusedBorderColor = Color(0xFF00FF87),
                        unfocusedBorderColor = Color(0xFF383848),
                        focusedContainerColor = Color(0xFF0D0D18),
                        unfocusedContainerColor = Color(0xFF0D0D18)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

            // Apply to all scenes checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E2A))
                    .clickable { applyToAllScenes = !applyToAllScenes }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = applyToAllScenes,
                    onCheckedChange = { applyToAllScenes = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF5722))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Apply to All Scenes (सभी सीन्स पर लागू करें)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Keep consistent dynamic captions across the entire reel", color = Color.Gray, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Apply Button
            Button(
                onClick = {
                    val finalSrt = if (srtText.isNotBlank()) {
                        srtText
                    } else if (subtitleText.isNotBlank()) {
                        val totalSec = if (currentScene.durationMs > 0) currentScene.durationMs / 1000f else currentScene.durationSeconds.toFloat().coerceAtLeast(1f)
                        val segs = VideoSpeechTranscriptionEngine.splitTextIntoTimedSegments(subtitleText, totalSec)
                        VideoSpeechTranscriptionEngine.segmentsToSrt(segs)
                    } else ""

                    onSaveCaptions(
                        subtitleText,
                        selectedPreset,
                        highlightColorHex,
                        textColorHex,
                        selectedFont,
                        selectedAnim,
                        applyToAllScenes,
                        finalSrt
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Dynamic Captions", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Live Animated Dynamic Caption Overlay for preview and full-screen player canvases.
 * Synchronized with the video's actual playback position (or internal timer), displaying
 * only the 3-5 words currently being spoken so captions match speech and never clutter the screen.
 */
@Composable
fun DynamicCaptionOverlay(
    scene: Scene,
    isPlaying: Boolean,
    currentPositionMs: Long? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val totalDurationMs = remember(scene.durationMs, scene.durationSeconds) {
        if (scene.durationMs > 0) scene.durationMs else (scene.durationSeconds * 1000L).coerceAtLeast(1000L)
    }

    var internalPlaybackElapsedMs by remember(scene.sceneNumber, scene.srtCaptions) { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying, totalDurationMs, currentPositionMs) {
        if (currentPositionMs == null) {
            if (isPlaying) {
                internalPlaybackElapsedMs = 0L
                val stepMs = 50L
                while (true) {
                    delay(stepMs)
                    internalPlaybackElapsedMs = (internalPlaybackElapsedMs + stepMs) % totalDurationMs
                }
            } else {
                internalPlaybackElapsedMs = 0L
            }
        }
    }

    val effectiveElapsedMs = currentPositionMs ?: internalPlaybackElapsedMs
    val curSec = (effectiveElapsedMs / 1000f).coerceAtLeast(0f)

    val segments = remember(scene.srtCaptions, scene.subtitle, scene.narrationText, totalDurationMs) {
        VideoSpeechTranscriptionEngine.getOrGenerateSegments(scene)
    }
    if (segments.isEmpty()) return

    val activeSegment = remember(segments, curSec, isPlaying) {
        val match = segments.firstOrNull { curSec >= it.startSec && curSec <= it.endSec }
        if (match != null) match
        else if (!isPlaying && curSec < 0.4f) segments.firstOrNull() // Preview first segment when stopped at start
        else null
    }
    if (activeSegment == null) return

    val words = remember(activeSegment.text) {
        activeSegment.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }
    if (words.isEmpty()) return

    val segDur = (activeSegment.endSec - activeSegment.startSec).coerceAtLeast(0.2f)
    val progress = ((curSec - activeSegment.startSec) / segDur).coerceIn(0f, 1f)
    val activeWordIdx = (progress * words.size).toInt().coerceIn(0, words.size - 1)

    val highlightColor = remember(scene.dynamicCaptionHighlightColor) {
        try {
            Color(android.graphics.Color.parseColor(scene.dynamicCaptionHighlightColor.ifEmpty { "#FFEB3B" }))
        } catch (e: Exception) {
            Color(0xFFFFEB3B)
        }
    }
    val textColor = remember(scene.subtitleColor) {
        try {
            Color(android.graphics.Color.parseColor(scene.subtitleColor.ifEmpty { "#FFFFFF" }))
        } catch (e: Exception) {
            Color.White
        }
    }
    val fontFamily = when (scene.captionFont) {
        "Sans-Serif" -> FontFamily.SansSerif
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val displayWords = remember(words) { words }
        val wordChunks = remember(displayWords) {
            if (displayWords.size > 3) displayWords.chunked((displayWords.size + 1) / 2) else listOf(displayWords)
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xD90A0A12),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, highlightColor.copy(alpha = 0.8f)),
            shadowElevation = 6.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .wrapContentWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                var runningIdx = 0
                wordChunks.forEach { rowWords ->
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowWords.forEach { word ->
                            val index = runningIdx++
                            val isActive = (index == activeWordIdx)
                            val wordScale = if (isActive && scene.dynamicCaptionAnimation == "BouncePop") 1.15f else 1.0f

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                                    .scale(wordScale)
                                    .then(
                                        if (isActive && scene.dynamicCaptionAnimation == "BoxHighlight") {
                                            Modifier
                                                .background(highlightColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        } else Modifier
                                    )
                            ) {
                                Text(
                                    text = word,
                                    color = when {
                                        isActive && scene.dynamicCaptionAnimation == "BoxHighlight" -> Color.Black
                                        isActive -> highlightColor
                                        else -> textColor
                                    },
                                    fontFamily = fontFamily,
                                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                                    fontSize = if (isActive) 14.sp else 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

