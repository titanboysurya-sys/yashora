package com.ritvyom.yashoraReelgenerator.presentation.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Base64
import android.util.Log
import com.ritvyom.yashoraReelgenerator.data.network.Content
import com.ritvyom.yashoraReelgenerator.data.network.GeminiApiClient
import com.ritvyom.yashoraReelgenerator.data.network.GeminiRequest
import com.ritvyom.yashoraReelgenerator.data.network.GenerationConfig
import com.ritvyom.yashoraReelgenerator.data.network.InlineData
import com.ritvyom.yashoraReelgenerator.data.network.Part
import com.ritvyom.yashoraReelgenerator.data.remote.UnifiedApiClient
import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.content as aiContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Intelligent Speech-to-Text Transcription Engine for Imported Videos.
 * Listens to audio in imported video clips, decodes speech using AI models
 * (supporting Hindi, English, Hinglish, and regional languages), and converts
 * spoken voice into synchronized timeline captions and subtitles.
 */
object VideoSpeechTranscriptionEngine {
    private const val TAG = "VideoSpeechTranscribe"

    data class SubtitleSegment(
        val startSec: Float,
        val endSec: Float,
        val text: String
    )

    data class TranscriptionResult(
        val isSuccess: Boolean,
        val fullTranscript: String,
        val detectedLanguage: String,
        val segments: List<SubtitleSegment>,
        val srtContent: String = "",
        val errorMessage: String? = null
    )

    /**
     * Extracts audio from a video file and transcribes speech into text using AI.
     */
    suspend fun transcribeVideoAudio(
        context: Context,
        mediaPath: String,
        preferredLanguage: String = "Auto",
        maxDurationMs: Long = 90000L,
        onProgress: (phaseText: String) -> Unit = {}
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        if (mediaPath.isBlank()) {
            return@withContext TranscriptionResult(
                isSuccess = false,
                fullTranscript = "",
                detectedLanguage = "",
                segments = emptyList(),
                errorMessage = "No video media path provided"
            )
        }

        try {
            onProgress("🎧 Extracting audio track from video...")
            val pcmBytes = ReelVideoCompiler.decodeAudioTrackToPcm(
                context = context,
                mediaPath = mediaPath,
                targetSampleRate = 16000,
                targetChannels = 1,
                maxDurationMs = maxDurationMs
            )

            if (pcmBytes == null || pcmBytes.isEmpty()) {
                Log.w(TAG, "No audio data decoded from video: $mediaPath")
                return@withContext TranscriptionResult(
                    isSuccess = false,
                    fullTranscript = "",
                    detectedLanguage = "",
                    segments = emptyList(),
                    errorMessage = "This video clip has no audible audio track or is muted."
                )
            }

            // Check if audio has perceptible energy/volume (16-bit little-endian PCM)
            var maxAmplitude = 0
            val shortsCount = pcmBytes.size / 2
            for (i in 0 until shortsCount) {
                val b0 = pcmBytes[i * 2].toInt() and 0xFF
                val b1 = pcmBytes[i * 2 + 1].toInt()
                val sample = Math.abs(((b1 shl 8) or b0).toShort().toInt())
                if (sample > maxAmplitude) {
                    maxAmplitude = sample
                }
            }

            if (maxAmplitude < 50) {
                Log.w(TAG, "Audio amplitude is nearly zero ($maxAmplitude). Video is silent.")
                return@withContext TranscriptionResult(
                    isSuccess = false,
                    fullTranscript = "",
                    detectedLanguage = "",
                    segments = emptyList(),
                    errorMessage = "Audio track is silent. No speech detected in this clip."
                )
            }

            onProgress("📦 Encoding audio for AI Speech-to-Text...")
            val wavBytes = pcmToWav(pcmBytes, sampleRate = 16000, channels = 1)
            val base64Audio = Base64.encodeToString(wavBytes, Base64.NO_WRAP)

            onProgress("🧠 AI listening & transcribing speech...")
            val langInstruction = when {
                preferredLanguage.contains("Hindi", ignoreCase = true) -> "The speaker is speaking in Hindi (Devanagari script) or Hinglish. Transcribe every spoken word accurately."
                preferredLanguage.contains("English", ignoreCase = true) -> "The speaker is speaking in English. Transcribe every spoken word accurately."
                preferredLanguage.contains("Hinglish", ignoreCase = true) -> "The speaker is speaking in Hinglish (Hindi words in Latin script). Transcribe natural conversational speech."
                else -> "Auto-detect the language spoken (Hindi, English, Hinglish, etc.). Transcribe accurately in that language."
            }

            val prompt = buildString {
                appendLine("You are an expert audio speech-to-text transcriber for video captions.")
                appendLine("Listen carefully to this audio track extracted from the user's video clip or voice narration.")
                appendLine("Task: Transcribe all spoken words accurately in the exact language spoken. $langInstruction")
                appendLine("Ensure natural punctuation, correct spelling of words, and provide precise timecodes formatted into SRT subtitle captions.")
                appendLine("CRITICAL: Break captions into short, punchy 3 to 4 word micro-segments with millisecond-accurate timestamps matching when words are actually spoken. NEVER put long sentences in one segment.")
                appendLine("Respond strictly with valid JSON with this exact schema:")
                appendLine("{")
                appendLine("  \"fullTranscript\": \"Full transcription of all spoken words in the video.\",")
                appendLine("  \"detectedLanguage\": \"Hindi / English / Hinglish\",")
                appendLine("  \"srtContent\": \"1\\n00:00:00,000 --> 00:00:01,800\\nFirst 3-4 words\\n\\n2\\n00:00:01,800 --> 00:00:03,500\\nNext 3-4 words\",")
                appendLine("  \"segments\": [")
                appendLine("    {")
                appendLine("      \"startSec\": 0.0,")
                appendLine("      \"endSec\": 1.8,")
                appendLine("      \"text\": \"3 to 4 spoken words\"")
                appendLine("    }")
                appendLine("  ]")
                appendLine("}")
                appendLine("Do NOT wrap in markdown fences or add commentary outside JSON.")
            }

            val candidateKeys = UnifiedApiClient.resolveCandidateGeminiKeys()
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "audio/wav", data = base64Audio))
                        ),
                        role = "user"
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.2f,
                    maxOutputTokens = 2048,
                    responseMimeType = "application/json"
                ),
                systemInstruction = Content(
                    parts = listOf(
                        Part(text = "You are a professional audio speech recognition engine. Transcribe audio speech to clean subtitle JSON.")
                    )
                )
            )

            // Modern Gemini models supporting multimodal audio input
            val modelsToTry = listOf(
                "gemini-2.5-flash",
                "gemini-flash-latest",
                "gemini-1.5-flash"
            )

            var lastError: Throwable? = null
            for ((tierLabel, apiKey) in candidateKeys) {
                for (model in modelsToTry) {
                    try {
                        Log.d(TAG, "Attempting speech transcription with $tierLabel ($model)...")
                        val response = kotlinx.coroutines.withTimeoutOrNull(25_000L) {
                            GeminiApiClient.service.generateContent(model, apiKey, request)
                        }
                        if (response == null) {
                            Log.w(TAG, "Model $model timed out after 25s, trying next candidate...")
                            continue
                        }
                        val text = response.candidates?.firstOrNull()?.content?.parts
                            ?.mapNotNull { it.text }
                            ?.joinToString("\n")
                            ?.trim()

                        if (!text.isNullOrBlank()) {
                            onProgress("✨ Parsing synchronized captions...")
                            val parsed = parseTranscriptionJson(text)
                            if (parsed.isSuccess) {
                                return@withContext parsed
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Model $model with $tierLabel failed: ${e.message}")
                        lastError = e
                    }
                }
            }

            // Fallback to secondary Firebase.ai Vertex AI endpoint if candidateKeys empty or REST failed
            try {
                onProgress("✨ Trying high-priority cloud speech engine...")
                val backends = listOf(
                    GenerativeBackend.googleAI(),
                    GenerativeBackend.vertexAI()
                )
                for (backend in backends) {
                    for (modelName in listOf("gemini-2.5-flash", "gemini-flash-latest")) {
                        try {
                            val fModel = Firebase.ai(backend = backend).generativeModel(
                                modelName = modelName,
                                generationConfig = generationConfig {
                                    temperature = 0.2f
                                    maxOutputTokens = 2048
                                }
                            )
                            val response = fModel.generateContent(
                                aiContent {
                                    text(prompt)
                                    inlineData(wavBytes, "audio/wav")
                                }
                            )
                            val respText = response.text?.trim()
                            if (!respText.isNullOrBlank()) {
                                val parsed = parseTranscriptionJson(respText)
                                if (parsed.isSuccess) {
                                    return@withContext parsed
                                }
                            }
                        } catch (e: Throwable) {
                            Log.w(TAG, "Firebase.ai fallback with $modelName failed: ${e.message}")
                            if (lastError == null) lastError = e
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firebase.ai fallback failed: ${e.message}")
            }

            return@withContext TranscriptionResult(
                isSuccess = false,
                fullTranscript = "",
                detectedLanguage = "",
                segments = emptyList(),
                errorMessage = lastError?.message ?: "AI speech transcription could not be completed. Please check network or API key in Settings."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription threw exception", e)
            return@withContext TranscriptionResult(
                isSuccess = false,
                fullTranscript = "",
                detectedLanguage = "",
                segments = emptyList(),
                errorMessage = e.message ?: "Failed transcribing video speech"
            )
        }
    }

    /**
     * Parses the JSON output from AI speech transcription into structured data.
     */
    private fun parseTranscriptionJson(rawJson: String): TranscriptionResult {
        return try {
            val cleanJson = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val root = JSONObject(cleanJson)
            val fullTranscript = root.optString("fullTranscript", "").trim()
            val detectedLang = root.optString("detectedLanguage", "Auto").trim()
            val segmentsList = mutableListOf<SubtitleSegment>()

            val segmentsArray = root.optJSONArray("segments")
            if (segmentsArray != null) {
                for (i in 0 until segmentsArray.length()) {
                    val segObj = segmentsArray.optJSONObject(i) ?: continue
                    val start = segObj.optDouble("startSec", 0.0).toFloat()
                    val end = segObj.optDouble("endSec", start + 3.0).toFloat()
                    val text = segObj.optString("text", "").trim()
                    if (text.isNotBlank()) {
                        segmentsList.add(SubtitleSegment(startSec = start, endSec = end, text = text))
                    }
                }
            }

            var srtContent = root.optString("srtContent", "").trim()

            // Prioritize srtContent if present because it contains exact millisecond-level timecodes
            if (srtContent.isNotBlank()) {
                val parsedSrt = parseSrt(srtContent)
                if (parsedSrt.isNotEmpty()) {
                    segmentsList.clear()
                    segmentsList.addAll(parsedSrt)
                }
            }

            // Fallback segments if both array and SRT were missing but full transcript exists
            if (segmentsList.isEmpty() && fullTranscript.isNotBlank()) {
                segmentsList.addAll(splitTextIntoTimedSegments(fullTranscript, 30f))
            }

            val finalSegments = subdivideLongSegments(segmentsList)
            val finalSrt = segmentsToSrt(finalSegments)

            TranscriptionResult(
                isSuccess = fullTranscript.isNotBlank() || finalSegments.isNotEmpty(),
                fullTranscript = if (fullTranscript.isNotBlank()) fullTranscript else finalSegments.joinToString(" ") { it.text },
                detectedLanguage = detectedLang,
                segments = finalSegments,
                srtContent = finalSrt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing transcription JSON: $rawJson", e)
            // Check if raw output was formatted directly as an SRT file
            val directSrtSegments = parseSrt(rawJson)
            if (directSrtSegments.isNotEmpty()) {
                val subdivided = subdivideLongSegments(directSrtSegments)
                val full = subdivided.joinToString(" ") { it.text }
                return TranscriptionResult(
                    isSuccess = true,
                    fullTranscript = full,
                    detectedLanguage = "Auto",
                    segments = subdivided,
                    srtContent = segmentsToSrt(subdivided)
                )
            }

            // If raw text itself was returned instead of json
            val cleaned = rawJson.replace(Regex("[{}\"]"), "").trim()
            if (cleaned.isNotBlank() && !cleaned.startsWith("error", ignoreCase = true)) {
                val segs = splitTextIntoTimedSegments(cleaned, 10f)
                val subdivided = subdivideLongSegments(segs)
                TranscriptionResult(
                    isSuccess = true,
                    fullTranscript = cleaned,
                    detectedLanguage = "Auto",
                    segments = subdivided,
                    srtContent = segmentsToSrt(subdivided)
                )
            } else {
                TranscriptionResult(
                    isSuccess = false,
                    fullTranscript = "",
                    detectedLanguage = "",
                    segments = emptyList(),
                    srtContent = "",
                    errorMessage = "Could not parse transcription output"
                )
            }
        }
    }

    /**
     * Formats seconds into standard SRT timestamp format: HH:mm:ss,SSS
     */
    fun formatSrtTimestamp(seconds: Float): String {
        val totalMs = (seconds * 1000f).toLong().coerceAtLeast(0L)
        val hours = totalMs / 3600000
        val minutes = (totalMs % 3600000) / 60000
        val secs = (totalMs % 60000) / 1000
        val millis = totalMs % 1000
        return String.format(java.util.Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, secs, millis)
    }

    /**
     * Parses an SRT time string (e.g. 00:01:23,456 or 01:23.45) into seconds.
     */
    fun parseSrtTimestamp(timeStr: String): Float {
        return try {
            val clean = timeStr.trim().replace(',', '.')
            val parts = clean.split(":")
            if (parts.size == 3) {
                val h = parts[0].toFloat()
                val m = parts[1].toFloat()
                val s = parts[2].toFloat()
                h * 3600f + m * 60f + s
            } else if (parts.size == 2) {
                val m = parts[0].toFloat()
                val s = parts[1].toFloat()
                m * 60f + s
            } else {
                clean.toFloatOrNull() ?: 0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Converts a list of SubtitleSegment entries into standard .srt file format.
     */
    fun segmentsToSrt(segments: List<SubtitleSegment>): String {
        if (segments.isEmpty()) return ""
        val sb = StringBuilder()
        segments.forEachIndexed { index, seg ->
            sb.append(index + 1).append("\n")
            val startTs = formatSrtTimestamp(seg.startSec)
            val endTs = formatSrtTimestamp(seg.endSec)
            sb.append(startTs).append(" --> ").append(endTs).append("\n")
            sb.append(seg.text.trim()).append("\n\n")
        }
        return sb.toString().trim()
    }

    /**
     * Parses .srt formatted content into a list of SubtitleSegment items.
     * Uses robust regex matching on timestamp arrows ("-->") to support single or multi-line spacing.
     */
    fun parseSrt(srtContent: String): List<SubtitleSegment> {
        val result = mutableListOf<SubtitleSegment>()
        if (srtContent.isBlank()) return result
        try {
            val normalized = srtContent.trim().replace("\r\n", "\n")
            val timestampRegex = Regex("""(\d{1,2}:\d{2}:\d{2}[,\.]\d{1,3})\s*-->\s*(\d{1,2}:\d{2}:\d{2}[,\.]\d{1,3})""")
            val matches = timestampRegex.findAll(normalized).toList()

            if (matches.isNotEmpty()) {
                for (i in matches.indices) {
                    val match = matches[i]
                    val startSec = parseSrtTimestamp(match.groupValues[1])
                    val endSec = parseSrtTimestamp(match.groupValues[2])

                    val textStart = match.range.last + 1
                    val textEnd = if (i + 1 < matches.size) matches[i + 1].range.first else normalized.length
                    val rawBlock = normalized.substring(textStart, textEnd)

                    val cleanLines = rawBlock.lines()
                        .map { it.trim() }
                        .filter { line ->
                            line.isNotEmpty() && line.toIntOrNull() == null && !line.contains("-->")
                        }
                    val text = cleanLines.joinToString(" ").trim()
                    if (text.isNotBlank()) {
                        result.add(
                            SubtitleSegment(
                                startSec = startSec,
                                endSec = endSec.coerceAtLeast(startSec + 0.3f),
                                text = text
                            )
                        )
                    }
                }
            } else {
                // Fallback for line-by-line text without standard SRT timestamps
                val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (lines.isNotEmpty()) {
                    var curT = 0f
                    val durPerLine = 2.0f
                    for (line in lines) {
                        result.add(SubtitleSegment(startSec = curT, endSec = curT + durPerLine, text = line))
                        curT += durPerLine
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SRT text", e)
        }
        return result
    }

    /**
     * Splits arbitrary subtitle or transcript text into natural, punchy 3-5 word timed segments
     * distributed evenly across totalDurationSec, preventing caption overflow/screen clutter.
     */
    fun splitTextIntoTimedSegments(text: String, totalDurationSec: Float): List<SubtitleSegment> {
        val clean = text.trim()
        if (clean.isBlank()) return emptyList()

        val allWords = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (allWords.isEmpty()) return emptyList()

        // Chunk words into small, readable groups of 3 to 5 words (ideal for Reels/Shorts dynamic captions)
        val wordChunks = allWords.chunked(4)
        val totalChunks = wordChunks.size
        val effectiveDuration = totalDurationSec.coerceAtLeast(totalChunks * 1.0f)
        val chunkDuration = (effectiveDuration / totalChunks).coerceIn(0.8f, 3.5f)

        val result = mutableListOf<SubtitleSegment>()
        var curStart = 0.0f
        for (chunk in wordChunks) {
            val chunkText = chunk.joinToString(" ")
            val curEnd = (curStart + chunkDuration).coerceAtMost(effectiveDuration)
            result.add(
                SubtitleSegment(
                    startSec = curStart,
                    endSec = curEnd.coerceAtLeast(curStart + 0.4f),
                    text = chunkText
                )
            )
            curStart = curEnd
        }
        return result
    }

    /**
     * Subdivides any segment that has more than 5 words into smaller, punchy 3-4 word micro-segments.
     * This guarantees captions never fill up the whole screen at any given moment.
     */
    fun subdivideLongSegments(segments: List<SubtitleSegment>): List<SubtitleSegment> {
        if (segments.isEmpty()) return emptyList()
        val result = mutableListOf<SubtitleSegment>()

        for (seg in segments) {
            val words = seg.text.replace("\n", " ").trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size <= 4) {
                result.add(seg.copy(text = words.joinToString(" ")))
            } else {
                // Break into punchy 3-4 word sub-chunks so captions never cover the screen
                val chunkSize = if (words.size in 5..6) 3 else 4
                val chunks = words.chunked(chunkSize)
                val totalSegDur = (seg.endSec - seg.startSec).coerceAtLeast(0.5f)
                val subDur = totalSegDur / chunks.size
                var subStart = seg.startSec
                for (chunk in chunks) {
                    val subEnd = subStart + subDur
                    result.add(
                        SubtitleSegment(
                            startSec = subStart,
                            endSec = subEnd.coerceAtLeast(subStart + 0.3f),
                            text = chunk.joinToString(" ")
                        )
                    )
                    subStart = subEnd
                }
            }
        }
        return result
    }

    /**
     * Gets parsed SRT segments for a scene, or auto-generates cleanly timed 3-5 word segments
     * from subtitle or narration text so captions match video timing and never clutter the screen.
     */
    fun getOrGenerateSegments(scene: com.ritvyom.yashoraReelgenerator.domain.models.Scene): List<SubtitleSegment> {
        val totalSec = if (scene.durationMs > 0) {
            scene.durationMs / 1000f
        } else {
            scene.durationSeconds.toFloat().coerceAtLeast(1f)
        }

        var list = if (scene.srtCaptions.isNotBlank()) {
            parseSrt(scene.srtCaptions)
        } else emptyList()

        if (list.isEmpty()) {
            val raw = if (scene.subtitle.isNotBlank()) scene.subtitle else scene.narrationText
            if (raw.isNotBlank()) {
                list = splitTextIntoTimedSegments(raw, totalSec)
            }
        }

        return subdivideLongSegments(list)
    }

    /**
     * Transcribes audio narration or recorded voice into SRT captions.
     */
    suspend fun transcribeAudioNarrationToSrt(
        context: Context,
        audioOrVideoPath: String,
        preferredLanguage: String = "Auto",
        maxDurationMs: Long = 90000L,
        onProgress: (phaseText: String) -> Unit = {}
    ): TranscriptionResult {
        return transcribeVideoAudio(
            context = context,
            mediaPath = audioOrVideoPath,
            preferredLanguage = preferredLanguage,
            maxDurationMs = maxDurationMs,
            onProgress = onProgress
        )
    }

    /**
     * Converts raw 16-bit PCM bytes into standard WAV format with a 44-byte RIFF header.
     */
    fun pcmToWav(pcmData: ByteArray, sampleRate: Int = 16000, channels: Int = 1): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * 2
        val header = ByteArray(44)
        val out = ByteArrayOutputStream(44 + pcmData.size)

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Audio format 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // 16-bit
        header[35] = 0

        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmData.size and 0xff).toByte()
        header[41] = ((pcmData.size shr 8) and 0xff).toByte()
        header[42] = ((pcmData.size shr 16) and 0xff).toByte()
        header[43] = ((pcmData.size shr 24) and 0xff).toByte()

        out.write(header)
        out.write(pcmData)
        return out.toByteArray()
    }
}
