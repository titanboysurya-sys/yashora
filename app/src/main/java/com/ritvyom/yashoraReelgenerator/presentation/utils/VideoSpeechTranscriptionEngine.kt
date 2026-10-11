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
                appendLine("Respond strictly with valid JSON with this exact schema:")
                appendLine("{")
                appendLine("  \"fullTranscript\": \"Full transcription of all spoken words in the video.\",")
                appendLine("  \"detectedLanguage\": \"Hindi / English / Hinglish\",")
                appendLine("  \"srtContent\": \"1\\n00:00:00,000 --> 00:00:02,800\\nFirst caption line\\n\\n2\\n00:00:02,800 --> 00:00:05,500\\nSecond caption line\",")
                appendLine("  \"segments\": [")
                appendLine("    {")
                appendLine("      \"startSec\": 0.0,")
                appendLine("      \"endSec\": 2.8,")
                appendLine("      \"text\": \"Subtitle sentence or phrase\"")
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

            // Modern models priority as recommended in gemini-api skill for multimodal audio
            val modelsToTry = listOf(
                "gemini-2.5-flash-native-audio-preview-12-2025",
                "gemini-flash-latest",
                "gemini-3.1-pro-preview",
                "gemini-3.5-flash"
            )

            var lastError: Throwable? = null
            for ((tierLabel, apiKey) in candidateKeys) {
                for (model in modelsToTry) {
                    try {
                        Log.d(TAG, "Attempting speech transcription with $tierLabel ($model)...")
                        val response = GeminiApiClient.service.generateContent(model, apiKey, request)
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

            // Fallback segments if JSON had full transcript but no array
            if (segmentsList.isEmpty() && fullTranscript.isNotBlank()) {
                val sentences = fullTranscript.split(Regex("(?<=[.!?।])\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                var curTime = 0.0f
                for (sentence in sentences) {
                    val duration = (sentence.length * 0.07f).coerceIn(2.0f, 6.0f)
                    segmentsList.add(
                        SubtitleSegment(
                            startSec = curTime,
                            endSec = curTime + duration,
                            text = sentence
                        )
                    )
                    curTime += duration
                }
            }

            if (srtContent.isBlank() && segmentsList.isNotEmpty()) {
                srtContent = segmentsToSrt(segmentsList)
            } else if (srtContent.isNotBlank() && segmentsList.isEmpty()) {
                segmentsList.addAll(parseSrt(srtContent))
            }

            TranscriptionResult(
                isSuccess = fullTranscript.isNotBlank() || segmentsList.isNotEmpty() || srtContent.isNotBlank(),
                fullTranscript = if (fullTranscript.isNotBlank()) fullTranscript else segmentsList.joinToString(" ") { it.text },
                detectedLanguage = detectedLang,
                segments = segmentsList,
                srtContent = if (srtContent.isNotBlank()) srtContent else segmentsToSrt(segmentsList)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing transcription JSON: $rawJson", e)
            // Check if raw output was formatted directly as an SRT file
            val directSrtSegments = parseSrt(rawJson)
            if (directSrtSegments.isNotEmpty()) {
                val full = directSrtSegments.joinToString(" ") { it.text }
                return TranscriptionResult(
                    isSuccess = true,
                    fullTranscript = full,
                    detectedLanguage = "Auto",
                    segments = directSrtSegments,
                    srtContent = rawJson.trim()
                )
            }

            // If raw text itself was returned instead of json
            val cleaned = rawJson.replace(Regex("[{}\"]"), "").trim()
            if (cleaned.isNotBlank() && !cleaned.startsWith("error", ignoreCase = true)) {
                val segs = listOf(SubtitleSegment(0.0f, 5.0f, cleaned))
                TranscriptionResult(
                    isSuccess = true,
                    fullTranscript = cleaned,
                    detectedLanguage = "Auto",
                    segments = segs,
                    srtContent = segmentsToSrt(segs)
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
     */
    fun parseSrt(srtContent: String): List<SubtitleSegment> {
        val result = mutableListOf<SubtitleSegment>()
        if (srtContent.isBlank()) return result
        try {
            val blocks = srtContent.trim().replace("\r\n", "\n").split(Regex("\n\n+"))
            for (block in blocks) {
                val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.size >= 2) {
                    val timeLine = if (lines[0].contains("-->")) lines[0] else if (lines.size >= 2 && lines[1].contains("-->")) lines[1] else null
                    if (timeLine != null) {
                        val parts = timeLine.split("-->")
                        if (parts.size == 2) {
                            val startSec = parseSrtTimestamp(parts[0])
                            val endSec = parseSrtTimestamp(parts[1])
                            val textLines = lines.filter { !it.contains("-->") && it.toIntOrNull() == null }
                            val text = textLines.joinToString(" ").trim()
                            if (text.isNotBlank()) {
                                result.add(
                                    SubtitleSegment(
                                        startSec = startSec,
                                        endSec = endSec.coerceAtLeast(startSec + 0.5f),
                                        text = text
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SRT text", e)
        }
        return result
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
