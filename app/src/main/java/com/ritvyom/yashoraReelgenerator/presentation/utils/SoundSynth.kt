package com.ritvyom.yashoraReelgenerator.presentation.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class SfxMetadata(
    val name: String,
    val category: String,
    val description: String,
    val durationText: String,
    val tag: String
)

object SoundSynth {

    val SFX_CATEGORIES = listOf("Trending", "Transitions", "Impacts", "Social & UI", "Cinematic", "Sci-Fi")

    val ALL_SFX: List<SfxMetadata> = listOf(
        // Trending & Transitions
        SfxMetadata("Whoosh", "Transitions", "Fast cinematic air movement for scene cuts", "0.6s", "🔥 Viral"),
        SfxMetadata("Swish", "Transitions", "Sharp whip swipe transition", "0.4s", "Popular"),
        SfxMetadata("Quick Zoom", "Transitions", "Rising pitch whoosh for rapid zoom-in", "0.5s", "🔥 Viral"),
        SfxMetadata("Camera Shutter", "Transitions", "Mechanical double-click photo snap", "0.3s", "Classic"),
        SfxMetadata("Glitch Swish", "Transitions", "Digital distortion transition noise", "0.5s", "Trendy"),
        SfxMetadata("Page Flip", "Transitions", "Crisp paper magazine flutter", "0.4s", "Clean"),

        // Impacts & Drops
        SfxMetadata("Cinematic Sub Drop", "Impacts", "Deep earth-shaking 120Hz to 25Hz sub-bass", "1.5s", "🔥 Viral"),
        SfxMetadata("Boom", "Impacts", "Explosive deep impact hit", "1.2s", "Heavy"),
        SfxMetadata("Metal Clang", "Impacts", "Metallic FM synthesized strike", "0.6s", "Punchy"),
        SfxMetadata("Punch Impact", "Impacts", "Fast acoustic transient hit", "0.3s", "Action"),
        SfxMetadata("Heavy Thud", "Impacts", "Low-frequency muffled impact", "0.5s", "Bass"),

        // Social & UI
        SfxMetadata("Bubble Pop", "Social & UI", "Clean resonant plastic bubble pop", "0.2s", "🔥 Viral"),
        SfxMetadata("Pop", "Social & UI", "Quick satisfying UI pop", "0.2s", "Popular"),
        SfxMetadata("Bell Ding", "Social & UI", "Crystal clear brass bell alert", "0.8s", "Notification"),
        SfxMetadata("Coin Collect", "Social & UI", "Double high-pitch sparkling coin ting", "0.4s", "Reward"),
        SfxMetadata("Like Chime", "Social & UI", "Harmonic ascending major chord chime", "0.5s", "🔥 Viral"),
        SfxMetadata("Success Fanfare", "Social & UI", "Three-tone energetic triumphant melody", "0.7s", "Milestone"),
        SfxMetadata("Notification Swipe", "Social & UI", "Warm ascending double tone", "0.4s", "Smooth"),

        // Cinematic & Risers
        SfxMetadata("Cinematic Riser", "Cinematic", "Rising tension swell leading to climax", "1.8s", "🔥 Viral"),
        SfxMetadata("Tension Drone", "Cinematic", "Low eerie suspense oscillator", "1.5s", "Mystery"),
        SfxMetadata("Tape Stop", "Cinematic", "Vinyl brake pitch-dropping slowdown", "0.7s", "Trendy"),
        SfxMetadata("Vinyl Scratch", "Cinematic", "Turntable backspin needle scratch", "0.5s", "Retro"),
        SfxMetadata("Heartbeat", "Cinematic", "Low dual pulse lub-dub rhythm", "1.0s", "Dramatic"),

        // Sci-Fi & Cyber
        SfxMetadata("Sci-Fi Laser", "Sci-Fi", "Cosmic downward pitch laser sweep", "0.4s", "Arcade"),
        SfxMetadata("Cyber Pulse", "Sci-Fi", "Modulated low-pass square wave bass drop", "0.5s", "Cyberpunk"),
        SfxMetadata("Digital Hit", "Sci-Fi", "Metallic FM synthesized pluck", "0.3s", "Electronic"),
        SfxMetadata("8-Bit Beep", "Sci-Fi", "Vintage arcade retro powerup chirp", "0.3s", "Retro")
    )

    fun getSfxByCategory(category: String): List<SfxMetadata> {
        return if (category == "Trending") {
            ALL_SFX.filter { it.tag.contains("Viral") || it.tag.contains("Popular") }
        } else {
            ALL_SFX.filter { it.category.equals(category, ignoreCase = true) }
        }
    }

    suspend fun playSfx(name: String, volumeGain: Float = 1.0f) = withContext(Dispatchers.Default) {
        val sampleRate = 44100
        val durationSec = when (name) {
            "Cinematic Sub Drop" -> 1.5f
            "Boom" -> 1.2f
            "Cinematic Riser" -> 1.8f
            "Tension Drone" -> 1.5f
            "Heartbeat" -> 1.0f
            "Bell Ding" -> 0.8f
            "Success Fanfare" -> 0.7f
            "Tape Stop" -> 0.7f
            "Whoosh" -> 0.6f
            "Metal Clang" -> 0.6f
            "Glitch Swish" -> 0.5f
            "Quick Zoom" -> 0.5f
            "Vinyl Scratch" -> 0.5f
            "Like Chime" -> 0.5f
            "Cyber Pulse" -> 0.5f
            "Heavy Thud" -> 0.5f
            "Swish" -> 0.4f
            "Sci-Fi Laser" -> 0.4f
            "Notification Swipe" -> 0.4f
            "Page Flip" -> 0.4f
            "Coin Collect" -> 0.4f
            "Digital Hit" -> 0.3f
            "Camera Shutter" -> 0.3f
            "Punch Impact" -> 0.3f
            "8-Bit Beep" -> 0.3f
            "Bubble Pop", "Pop" -> 0.2f
            else -> 0.5f
        }

        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val gain = volumeGain.coerceIn(0.1f, 2.0f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val time = i.toFloat() / sampleRate

            val wave = when (name) {
                "Whoosh" -> {
                    val noise = (Math.random() * 2.0 - 1.0).toFloat()
                    val envelope = sin(progress * PI.toFloat())
                    noise * envelope * 0.45f
                }
                "Swish" -> {
                    val noise = (Math.random() * 2.0 - 1.0).toFloat()
                    val envelope = (1f - progress) * sin(progress * PI.toFloat() * 1.5f).coerceAtLeast(0f)
                    noise * envelope * 0.5f
                }
                "Quick Zoom" -> {
                    val currentFreq = 200f + 1400f * progress * progress
                    val phase = 2f * PI.toFloat() * currentFreq * time
                    val noise = (Math.random() * 2.0 - 1.0).toFloat() * 0.2f
                    val envelope = sin(progress * PI.toFloat())
                    (sin(phase).toFloat() * 0.6f + noise) * envelope * 0.5f
                }
                "Camera Shutter" -> {
                    // Double mechanical click (first at 0.05s, second at 0.18s)
                    val tClick1 = (time - 0.03f).coerceAtLeast(0f)
                    val tClick2 = (time - 0.16f).coerceAtLeast(0f)
                    val click1 = if (time < 0.12f) exp(-40f * tClick1) * sin(2f * PI.toFloat() * 2200f * time).toFloat() else 0f
                    val click2 = if (time >= 0.12f) exp(-45f * tClick2) * sin(2f * PI.toFloat() * 1800f * time).toFloat() else 0f
                    (click1 + click2) * 0.6f
                }
                "Glitch Swish" -> {
                    val noise = if ((i / 400) % 3 == 0) (Math.random() * 2.0 - 1.0).toFloat() else 0f
                    val square = if (sin(2f * PI.toFloat() * 180f * time) > 0) 0.3f else -0.3f
                    val env = sin(progress * PI.toFloat())
                    (noise * 0.6f + square) * env * 0.5f
                }
                "Page Flip" -> {
                    val noise = (Math.random() * 2.0 - 1.0).toFloat()
                    val flutter = sin(2f * PI.toFloat() * 35f * time).toFloat()
                    val env = exp(-6f * progress)
                    noise * flutter * env * 0.4f
                }
                "Cinematic Sub Drop", "Boom" -> {
                    val fStart = 130f
                    val fEnd = 24f
                    val currentFreq = fStart + (fEnd - fStart) * progress
                    val phase = 2f * PI.toFloat() * currentFreq * time
                    val baseWave = sin(phase).toFloat()
                    val env = exp(-2.5f * progress)
                    baseWave * env * 0.85f
                }
                "Metal Clang" -> {
                    val carrierFreq = 340f
                    val modFreq = 960f
                    val modIndex = 5.0f * exp(-7f * progress)
                    val modPhase = 2f * PI.toFloat() * modFreq * time
                    val phase = 2f * PI.toFloat() * carrierFreq * time + modIndex * sin(modPhase)
                    sin(phase).toFloat() * exp(-4f * progress) * 0.65f
                }
                "Punch Impact" -> {
                    val currentFreq = 250f * exp(-12f * progress) + 40f
                    val phase = 2f * PI.toFloat() * currentFreq * time
                    val noise = (Math.random() * 2.0 - 1.0).toFloat() * exp(-18f * progress)
                    (sin(phase).toFloat() * 0.7f + noise * 0.5f) * exp(-8f * progress)
                }
                "Heavy Thud" -> {
                    val currentFreq = 80f * exp(-6f * progress) + 30f
                    val phase = 2f * PI.toFloat() * currentFreq * time
                    sin(phase).toFloat() * exp(-5f * progress) * 0.8f
                }
                "Bubble Pop", "Pop" -> {
                    val currentFreq = 420f + 900f * sin(progress * PI.toFloat())
                    val phase = 2f * PI.toFloat() * currentFreq * time
                    sin(phase).toFloat() * sin(progress * PI.toFloat()) * 0.65f
                }
                "Bell Ding" -> {
                    val f1 = sin(2f * PI.toFloat() * 1568f * time).toFloat()
                    val f2 = sin(2f * PI.toFloat() * 3136f * time).toFloat() * 0.4f
                    (f1 + f2) * exp(-3.2f * progress) * 0.6f
                }
                "Coin Collect" -> {
                    val freq = if (progress < 0.45f) 987f else 1318f // B5 -> E6
                    val phase = 2f * PI.toFloat() * freq * time
                    sin(phase).toFloat() * exp(-4f * progress) * 0.6f
                }
                "Like Chime" -> {
                    val freq = when {
                        progress < 0.33f -> 523.25f // C5
                        progress < 0.66f -> 659.25f // E5
                        else -> 783.99f // G5
                    }
                    val phase = 2f * PI.toFloat() * freq * time
                    sin(phase).toFloat() * exp(-3.5f * (progress % 0.33f) * 3f) * 0.55f
                }
                "Success Fanfare" -> {
                    val freq = when {
                        progress < 0.25f -> 587.33f // D5
                        progress < 0.5f -> 739.99f // F#5
                        progress < 0.75f -> 880.0f  // A5
                        else -> 1174.66f // D6
                    }
                    val phase = 2f * PI.toFloat() * freq * time
                    sin(phase).toFloat() * exp(-3f * (progress % 0.25f) * 4f) * 0.55f
                }
                "Notification Swipe" -> {
                    val f1 = 440f
                    val f2 = 880f
                    val freq = if (progress < 0.4f) f1 else f2
                    val phase = 2f * PI.toFloat() * freq * time
                    sin(phase).toFloat() * sin(progress * PI.toFloat()) * 0.45f
                }
                "Cinematic Riser" -> {
                    // Dramatic ascending pitch from 70Hz to 1400Hz with pulsating flutter
                    val currentFreq = 70f + 1330f * (progress * progress)
                    val phase = 2f * PI.toFloat() * currentFreq * time
                    val flutter = 1.0f + 0.3f * sin(2f * PI.toFloat() * (5f + 15f * progress) * time)
                    val env = progress * progress * 0.75f
                    sin(phase).toFloat() * flutter * env
                }
                "Tension Drone" -> {
                    val f1 = 65f
                    val f2 = 68f // binaural beat oscillation
                    val p1 = 2f * PI.toFloat() * f1 * time
                    val p2 = 2f * PI.toFloat() * f2 * time
                    val waveMix = sin(p1).toFloat() * 0.5f + sin(p2).toFloat() * 0.5f
                    waveMix * sin(progress * PI.toFloat()) * 0.6f
                }
                "Tape Stop" -> {
                    val speedFactor = (1.0f - progress).coerceAtLeast(0.05f)
                    val freq = 440f * speedFactor
                    val phase = 2f * PI.toFloat() * freq * time
                    sin(phase).toFloat() * (1f - progress * 0.8f) * 0.55f
                }
                "Vinyl Scratch" -> {
                    val noise = (Math.random() * 2.0 - 1.0).toFloat()
                    val mod = sin(2f * PI.toFloat() * 20f * time).toFloat()
                    val env = sin(progress * PI.toFloat())
                    noise * mod * env * 0.5f
                }
                "Heartbeat" -> {
                    val isFirstBeat = (progress in 0.05f..0.35f)
                    val isSecondBeat = (progress in 0.45f..0.75f)
                    val env = when {
                        isFirstBeat -> sin(((progress - 0.05f) / 0.3f) * PI.toFloat())
                        isSecondBeat -> sin(((progress - 0.45f) / 0.3f) * PI.toFloat()) * 0.8f
                        else -> 0f
                    }
                    val phase = 2f * PI.toFloat() * 55f * time
                    sin(phase).toFloat() * env * 0.75f
                }
                "Sci-Fi Laser" -> {
                    val fStart = 2400f
                    val fEnd = 160f
                    val currentFreq = fStart + (fEnd - fStart) * progress * progress
                    val phase = 2f * PI.toFloat() * currentFreq * time
                    val baseWave = (phase % (2f * PI.toFloat()) / PI.toFloat() - 1f).toFloat()
                    baseWave * (1.0f - progress) * 0.45f
                }
                "Cyber Pulse" -> {
                    val freq = 85f
                    val phase = 2f * PI.toFloat() * freq * time
                    val baseWave = if (sin(phase) > 0) 0.5f else -0.5f
                    baseWave * exp(-3.5f * progress) * 0.5f
                }
                "Digital Hit" -> {
                    val carrierFreq = 220f
                    val modFreq = 660f
                    val modIndex = 4.0f * exp(-6f * progress)
                    val modPhase = 2f * PI.toFloat() * modFreq * time
                    val phase = 2f * PI.toFloat() * carrierFreq * time + modIndex * sin(modPhase)
                    sin(phase).toFloat() * exp(-5f * progress) * 0.6f
                }
                "8-Bit Beep" -> {
                    val freq = if (progress < 0.5f) 440f else 880f
                    val phase = 2f * PI.toFloat() * freq * time
                    val square = if (sin(phase) > 0) 0.4f else -0.4f
                    square * (1f - progress * 0.5f) * 0.5f
                }
                else -> 0f
            }

            val finalSample = (wave * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            buffer[i] = finalSample
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(numSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            delay((durationSec * 1000).toLong() + 100)
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
