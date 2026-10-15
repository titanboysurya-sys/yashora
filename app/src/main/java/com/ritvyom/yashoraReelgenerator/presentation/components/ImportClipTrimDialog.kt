package com.ritvyom.yashoraReelgenerator.presentation.components

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.ritvyom.yashoraReelgenerator.presentation.utils.Media3VideoTrimmer
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Dedicated Trim Interface for imported video clips.
 * Allows users to inspect imported footage, preview playback, and define start and end timestamps
 * before adding the clip to the final reel timeline.
 */
@Composable
fun ImportClipTrimDialog(
    videoUriOrPath: String,
    appLanguage: String,
    clipIndex: Int = 1,
    totalClips: Int = 1,
    onTrimAndAdd: (trimmedPath: String, startMs: Long, endMs: Long, durationSec: Int, autoTranscribe: Boolean, transcribeLang: String) -> Unit,
    onSkipTrimAndAdd: (originalPath: String, durationSec: Int, autoTranscribe: Boolean, transcribeLang: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var cachedFilePath by remember { mutableStateOf<String?>(null) }
    var isResolvingFile by remember { mutableStateOf(true) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }

    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(0L) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackMs by remember { mutableLongStateOf(0L) }

    var isTrimming by remember { mutableStateOf(false) }
    var trimProgress by remember { mutableFloatStateOf(0f) }
    var trimErrorMessage by remember { mutableStateOf<String?>(null) }

    var autoTranscribeVoice by remember { mutableStateOf(false) }
    var selectedTranscribeLang by remember { mutableStateOf("Auto") }

    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }

    // Step 1: Ensure video is locally accessible and obtain duration
    LaunchedEffect(videoUriOrPath) {
        isResolvingFile = true
        withContext(Dispatchers.IO) {
            var resolved = videoUriOrPath
            if (videoUriOrPath.startsWith("content://")) {
                try {
                    val uri = Uri.parse(videoUriOrPath)
                    val mediaDir = File(context.filesDir, "user_media").apply { mkdirs() }
                    val targetFile = File(mediaDir, "import_raw_${System.currentTimeMillis()}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (targetFile.exists() && targetFile.length() > 0L) {
                        resolved = targetFile.absolutePath
                    }
                } catch (e: Exception) {
                    Log.e("ImportClipTrimDialog", "Failed to cache content URI", e)
                }
            } else if (videoUriOrPath.startsWith("file://")) {
                resolved = videoUriOrPath.removePrefix("file://")
            }

            val dur = Media3VideoTrimmer.getVideoDurationMs(context, resolved)
            val finalDuration = if (dur > 0L) dur else 8000L

            withContext(Dispatchers.Main) {
                cachedFilePath = resolved
                totalDurationMs = finalDuration
                startMs = 0L
                endMs = finalDuration
                isResolvingFile = false
            }
        }
    }

    // Loop playback strictly between startMs and endMs
    LaunchedEffect(startMs, endMs, isPlaying) {
        while (true) {
            delay(100)
            val vv = videoViewInstance
            if (vv != null && vv.isPlaying) {
                val pos = vv.currentPosition.toLong()
                currentPlaybackMs = pos
                isPlaying = true

                if (pos >= endMs || pos < startMs) {
                    vv.seekTo(startMs.toInt())
                }
            } else if (vv != null && !vv.isPlaying) {
                isPlaying = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewInstance?.stopPlayback()
            } catch (e: Exception) {
                Log.e("ImportClipTrimDialog", "Error releasing VideoView", e)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(10.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF100C1E),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Trim Clip",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Trim Video Clip ✂️".localize(appLanguage),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (totalClips > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "$clipIndex of $totalClips",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Set start and end timestamps before adding to timeline".localize(appLanguage),
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            if (isResolvingFile) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Preparing imported video...".localize(appLanguage),
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                val filePath = cachedFilePath ?: videoUriOrPath
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Video Preview Player Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    videoViewInstance = this
                                    val f = File(filePath)
                                    if (f.exists()) {
                                        setVideoPath(filePath)
                                    } else {
                                        setVideoURI(Uri.parse(filePath))
                                    }
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        seekTo(startMs.toInt())
                                        start()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Play/Pause Overlay
                        IconButton(
                            onClick = {
                                val vv = videoViewInstance ?: return@IconButton
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    if (vv.currentPosition >= endMs || vv.currentPosition < startMs) {
                                        vv.seekTo(startMs.toInt())
                                    }
                                    vv.start()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Time indicator overlay badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "${formatTimestamp(currentPlaybackMs)} / ${formatTimestamp(totalDurationMs)}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trim Duration Summary Banner
                    val trimmedDurationMs = (endMs - startMs).coerceAtLeast(0L)
                    val savedDurationMs = (totalDurationMs - trimmedDurationMs).coerceAtLeast(0L)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1730)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Original: ${formatTimestamp(totalDurationMs)}".localize(appLanguage),
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Timeline Length: ${formatTimestamp(trimmedDurationMs)}".localize(appLanguage),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (savedDurationMs > 0L) {
                                Surface(
                                    color = Color(0xFF2E7D32).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "-${formatTimestamp(savedDurationMs)} cut".localize(appLanguage),
                                        color = Color(0xFF81C784),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dual Range Slider Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF171326), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Define Trim Timestamps ✂️".localize(appLanguage),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatTimestamp(startMs)}  ➜  ${formatTimestamp(endMs)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val maxRange = totalDurationMs.toFloat().coerceAtLeast(1000f)
                        var rangeState by remember(startMs, endMs, totalDurationMs) {
                            mutableStateOf(startMs.toFloat()..endMs.toFloat())
                        }

                        RangeSlider(
                            value = rangeState,
                            onValueChange = { newRange ->
                                val newStart = newRange.start.toLong().coerceIn(0L, totalDurationMs - 500L)
                                val newEnd = newRange.endInclusive.toLong().coerceIn(newStart + 500L, totalDurationMs)
                                rangeState = newStart.toFloat()..newEnd.toFloat()
                                startMs = newStart
                                endMs = newEnd
                            },
                            valueRange = 0f..maxRange,
                            onValueChangeFinished = {
                                videoViewInstance?.seekTo(startMs.toInt())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_trimmer_range_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fine-tune Start Timestamp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Start: ${formatTimestamp(startMs)}".localize(appLanguage),
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PrecisionButton("-1s") {
                                    startMs = (startMs - 1000L).coerceAtLeast(0L)
                                    videoViewInstance?.seekTo(startMs.toInt())
                                }
                                PrecisionButton("-0.5s") {
                                    startMs = (startMs - 500L).coerceAtLeast(0L)
                                    videoViewInstance?.seekTo(startMs.toInt())
                                }
                                PrecisionButton("+0.5s") {
                                    startMs = (startMs + 500L).coerceAtMost(endMs - 500L)
                                    videoViewInstance?.seekTo(startMs.toInt())
                                }
                                PrecisionButton("+1s") {
                                    startMs = (startMs + 1000L).coerceAtMost(endMs - 500L)
                                    videoViewInstance?.seekTo(startMs.toInt())
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Fine-tune End Timestamp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "End: ${formatTimestamp(endMs)}".localize(appLanguage),
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PrecisionButton("-1s") {
                                    endMs = (endMs - 1000L).coerceAtLeast(startMs + 500L)
                                    videoViewInstance?.seekTo(((endMs - 1000L).coerceAtLeast(startMs)).toInt())
                                }
                                PrecisionButton("-0.5s") {
                                    endMs = (endMs - 500L).coerceAtLeast(startMs + 500L)
                                    videoViewInstance?.seekTo(((endMs - 1000L).coerceAtLeast(startMs)).toInt())
                                }
                                PrecisionButton("+0.5s") {
                                    endMs = (endMs + 500L).coerceAtMost(totalDurationMs)
                                    videoViewInstance?.seekTo(((endMs - 1000L).coerceAtLeast(startMs)).toInt())
                                }
                                PrecisionButton("+1s") {
                                    endMs = (endMs + 1000L).coerceAtMost(totalDurationMs)
                                    videoViewInstance?.seekTo(((endMs - 1000L).coerceAtLeast(startMs)).toInt())
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Trimming Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChipButton(
                            label = "Cut Start 1s".localize(appLanguage),
                            onClick = {
                                startMs = (startMs + 1000L).coerceAtMost(endMs - 500L)
                                videoViewInstance?.seekTo(startMs.toInt())
                            }
                        )
                        PresetChipButton(
                            label = "Cut End 1s".localize(appLanguage),
                            onClick = {
                                endMs = (endMs - 1000L).coerceAtLeast(startMs + 500L)
                                videoViewInstance?.seekTo(((endMs - 1000L).coerceAtLeast(startMs)).toInt())
                            }
                        )
                        PresetChipButton(
                            label = "Keep 5s".localize(appLanguage),
                            onClick = {
                                endMs = (startMs + 5000L).coerceAtMost(totalDurationMs)
                                videoViewInstance?.seekTo(startMs.toInt())
                            }
                        )
                        PresetChipButton(
                            label = "Reset".localize(appLanguage),
                            onClick = {
                                startMs = 0L
                                endMs = totalDurationMs
                                videoViewInstance?.seekTo(0)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Error indicator
                    trimErrorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // Progress indicator during Media3 trim
                    if (isTrimming) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { trimProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Trimming clip with Media3... ${(trimProgress * 100).toInt()}%".localize(appLanguage),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        // Action buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Proceed to add clip to timeline
                            Button(
                                onClick = {
                                    isTrimming = true
                                    trimErrorMessage = null
                                    val trimmedDuration = (endMs - startMs).coerceAtLeast(500L)
                                    val durationSec = ((trimmedDuration / 1000L).toInt()).coerceAtLeast(1)

                                    val outputDir = File(context.cacheDir, "trimmed_imports").apply { mkdirs() }
                                    val outputFile = File(outputDir, "trim_import_${System.currentTimeMillis()}_clip$clipIndex.mp4")

                                    Media3VideoTrimmer.trimVideo(
                                        context = context,
                                        inputPath = filePath,
                                        outputPath = outputFile.absolutePath,
                                        startMs = startMs,
                                        endMs = endMs,
                                        onProgress = { pr -> trimProgress = pr },
                                        onComplete = { success, error ->
                                            isTrimming = false
                                            if (success && outputFile.exists() && outputFile.length() > 0L) {
                                                onTrimAndAdd(outputFile.absolutePath, startMs, endMs, durationSec, autoTranscribeVoice, selectedTranscribeLang)
                                            } else {
                                                // Fallback: use original file with timestamps
                                                onTrimAndAdd(filePath, startMs, endMs, durationSec, autoTranscribeVoice, selectedTranscribeLang)
                                            }
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("add_trimmed_clip_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                val buttonLabel = if (clipIndex < totalClips) {
                                    "Add & Next Clip (${clipIndex + 1}/$totalClips) ✂️"
                                } else {
                                    "Add Trimmed Clip to Timeline ✂️"
                                }
                                Text(buttonLabel.localize(appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("cancel_trim_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancel".localize(appLanguage), fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val fullDurSec = ((totalDurationMs / 1000L).toInt()).coerceAtLeast(1)
                                        onSkipTrimAndAdd(filePath, fullDurSec, autoTranscribeVoice, selectedTranscribeLang)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .height(40.dp)
                                        .testTag("skip_trim_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Use Full Video".localize(appLanguage), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun PrecisionButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PresetChipButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF221C35),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.LightGray,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = ((ms % 1000) / 100).toInt()
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, millis)
}
