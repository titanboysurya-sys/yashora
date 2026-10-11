package com.ritvyom.yashoraReelgenerator.data.voxeleven

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TtsRequest(
    @Json(name = "text") val text: String,
    @Json(name = "model_id") val modelId: String = "eleven_multilingual_v2",
    @Json(name = "voice_settings") val voiceSettings: VoiceSettings? = null
)

@JsonClass(generateAdapter = true)
data class VoiceSettings(
    @Json(name = "stability") val stability: Double = 0.5,
    @Json(name = "similarity_boost") val similarityBoost: Double = 0.75,
    @Json(name = "style") val style: Double = 0.0,
    @Json(name = "use_speaker_boost") val useSpeakerBoost: Boolean = true
)

@JsonClass(generateAdapter = true)
data class VoicesResponse(
    @Json(name = "voices") val voices: List<Voice>
)

@JsonClass(generateAdapter = true)
data class Voice(
    @Json(name = "voice_id") val voiceId: String,
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String? = null,
    @Json(name = "labels") val labels: Map<String, String>? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "preview_url") val previewUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "subscription") val subscription: Subscription? = null
)

@JsonClass(generateAdapter = true)
data class Subscription(
    @Json(name = "character_count") val characterCount: Int? = 0,
    @Json(name = "character_limit") val characterLimit: Int? = 10000,
    @Json(name = "tier") val tier: String? = "free"
)

@JsonClass(generateAdapter = true)
data class TtsWithTimestampsResponse(
    @Json(name = "audio_base64") val audioBase64: String,
    @Json(name = "alignment") val alignment: AlignmentInfo? = null,
    @Json(name = "normalized_alignment") val normalizedAlignment: AlignmentInfo? = null
)

@JsonClass(generateAdapter = true)
data class AlignmentInfo(
    @Json(name = "characters") val characters: List<String>? = null,
    @Json(name = "character_start_times_seconds") val characterStartTimesSeconds: List<Double>? = null,
    @Json(name = "character_end_times_seconds") val characterEndTimesSeconds: List<Double>? = null
)

data class SavedVoiceClip(
    val id: String,
    val title: String,
    val scriptText: String,
    val engineName: String,
    val voiceName: String,
    val filePath: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L
)

