package com.ritvyom.yashoraReelgenerator.engine.timeline

import java.io.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Standard discrete playback speeds supported by professional video editors.
 */
enum class StandardSpeedPreset(val multiplier: Float, val label: String) {
    SPEED_0_25X(0.25f, "0.25x"),
    SPEED_0_5X(0.5f, "0.5x"),
    SPEED_0_75X(0.75f, "0.75x"),
    SPEED_1_0X(1.0f, "1x"),
    SPEED_1_25X(1.25f, "1.25x"),
    SPEED_1_5X(1.5f, "1.5x"),
    SPEED_2_0X(2.0f, "2x"),
    SPEED_3_0X(3.0f, "3x"),
    SPEED_4_0X(4.0f, "4x");

    companion object {
        fun fromMultiplier(speed: Float): StandardSpeedPreset {
            return entries.minByOrNull { abs(it.multiplier - speed) } ?: SPEED_1_0X
        }
    }
}

/**
 * Speed curve anchor point for non-linear time-ramping.
 * [normalizedTimelinePos]: 0.0f (clip start) to 1.0f (clip end)
 * [speedMultiplier]: speed multiplier at this point (e.g. 0.2f to 5.0f)
 */
data class SpeedRampPoint(
    val normalizedTimelinePos: Float, // 0.0 to 1.0 within clip
    val speedMultiplier: Float       // e.g. 0.2f, 2.0f
) : Serializable

/**
 * Professional Speed Curve Ramping Presets matching CapCut, Premiere & VN.
 */
enum class SpeedCurvePreset(
    val id: String,
    val displayName: String,
    val description: String,
    val iconEmoji: String,
    val defaultPoints: List<SpeedRampPoint>
) {
    LINEAR(
        "Linear",
        "Normal (Constant)",
        "Standard constant playback speed",
        "➡️",
        listOf(
            SpeedRampPoint(0.0f, 1.0f),
            SpeedRampPoint(1.0f, 1.0f)
        )
    ),
    MONTAGE(
        "Montage",
        "Montage (मोंटाज)",
        "Fast intro, slow motion middle, fast punchy outro",
        "⚡",
        listOf(
            SpeedRampPoint(0.0f, 2.5f),
            SpeedRampPoint(0.25f, 1.0f),
            SpeedRampPoint(0.5f, 0.4f),
            SpeedRampPoint(0.75f, 1.0f),
            SpeedRampPoint(1.0f, 2.5f)
        )
    ),
    HERO(
        "Hero",
        "Hero (हीरो क्लाइमेक्स)",
        "Slow atmospheric buildup into hyper-speed climax",
        "🦸",
        listOf(
            SpeedRampPoint(0.0f, 0.6f),
            SpeedRampPoint(0.4f, 0.6f),
            SpeedRampPoint(0.6f, 3.5f),
            SpeedRampPoint(1.0f, 0.5f)
        )
    ),
    BULLET(
        "Bullet",
        "Bullet Time (मैट्रिक्स स्लो-मो)",
        "Real-time pacing dropping to extreme 0.2x Matrix slow-mo",
        "🎯",
        listOf(
            SpeedRampPoint(0.0f, 1.0f),
            SpeedRampPoint(0.25f, 1.0f),
            SpeedRampPoint(0.5f, 0.2f),
            SpeedRampPoint(0.75f, 1.0f),
            SpeedRampPoint(1.0f, 1.0f)
        )
    ),
    JUMP_CUT(
        "Jump Cut",
        "Jump Cut (रिदमिक बीट)",
        "Rapid rhythmic speed pulse bursts synced to upbeat reels",
        "🦘",
        listOf(
            SpeedRampPoint(0.0f, 3.0f),
            SpeedRampPoint(0.25f, 0.8f),
            SpeedRampPoint(0.5f, 3.0f),
            SpeedRampPoint(0.75f, 0.8f),
            SpeedRampPoint(1.0f, 3.0f)
        )
    ),
    FLASH_IN(
        "Flash In",
        "Flash In (फ्लैश इन)",
        "High 4.0x initial burst smoothly settling into real-time",
        "⚡",
        listOf(
            SpeedRampPoint(0.0f, 4.0f),
            SpeedRampPoint(0.35f, 1.8f),
            SpeedRampPoint(0.7f, 1.0f),
            SpeedRampPoint(1.0f, 1.0f)
        )
    ),
    FLASH_OUT(
        "Flash Out",
        "Flash Out (फ्लैश आउट)",
        "Normal playback catapulting into extreme speed zoom outro",
        "🚀",
        listOf(
            SpeedRampPoint(0.0f, 1.0f),
            SpeedRampPoint(0.3f, 1.0f),
            SpeedRampPoint(0.65f, 1.8f),
            SpeedRampPoint(1.0f, 4.0f)
        )
    ),
    CUSTOM(
        "Custom",
        "Custom Bézier (कस्टम कर्व)",
        "Fully interactive keyframe speed ramping curve",
        "📈",
        listOf(
            SpeedRampPoint(0.0f, 1.0f),
            SpeedRampPoint(0.33f, 2.0f),
            SpeedRampPoint(0.66f, 0.5f),
            SpeedRampPoint(1.0f, 1.5f)
        )
    );

    companion object {
        fun fromId(id: String): SpeedCurvePreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LINEAR
        }
    }
}

/**
 * Deterministic Speed & Time-Remapping Engine.
 * Converts bidirectionally between Timeline Composition Time, Clip Local Time, and Source Media Presentation Time.
 * Ensures preview and export use 100% identical timestamp mappings.
 */
object CanonicalSpeedEngine {

    /**
     * Parse serialized string formatted as "0.0:1.0,0.5:2.0,1.0:1.0"
     */
    fun parsePoints(pointsStr: String?): List<SpeedRampPoint> {
        if (pointsStr.isNullOrBlank()) return SpeedCurvePreset.LINEAR.defaultPoints
        return try {
            pointsStr.split(",")
                .mapNotNull { token ->
                    val parts = token.trim().split(":")
                    if (parts.size == 2) {
                        val pos = parts[0].toFloatOrNull()?.coerceIn(0.0f, 1.0f) ?: return@mapNotNull null
                        val speed = parts[1].toFloatOrNull()?.coerceIn(0.1f, 10.0f) ?: 1.0f
                        SpeedRampPoint(pos, speed)
                    } else null
                }
                .sortedBy { it.normalizedTimelinePos }
                .ifEmpty { SpeedCurvePreset.LINEAR.defaultPoints }
        } catch (e: Exception) {
            SpeedCurvePreset.LINEAR.defaultPoints
        }
    }

    /**
     * Serialize a list of SpeedRampPoints into a compact string format: "pos:speed,pos:speed"
     */
    fun serializePoints(points: List<SpeedRampPoint>): String {
        return points.sortedBy { it.normalizedTimelinePos }
            .joinToString(",") { "%.2f:%.2f".format(java.util.Locale.US, it.normalizedTimelinePos, it.speedMultiplier) }
    }

    /**
     * Linear / Smooth interpolation of speed multiplier at normalized position t in [0.0, 1.0].
     */
    fun getSpeedAt(points: List<SpeedRampPoint>, t: Float): Float {
        if (points.isEmpty()) return 1.0f
        val clampedT = t.coerceIn(0.0f, 1.0f)
        if (clampedT <= points.first().normalizedTimelinePos) return points.first().speedMultiplier
        if (clampedT >= points.last().normalizedTimelinePos) return points.last().speedMultiplier

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            if (clampedT in p0.normalizedTimelinePos..p1.normalizedTimelinePos) {
                val span = p1.normalizedTimelinePos - p0.normalizedTimelinePos
                if (span <= 0.0001f) return p0.speedMultiplier
                val frac = (clampedT - p0.normalizedTimelinePos) / span
                // Smooth cosine / hermite blend for natural video deceleration/acceleration
                val smoothFrac = (1f - kotlin.math.cos(frac * Math.PI.toFloat())) / 2f
                return p0.speedMultiplier + smoothFrac * (p1.speedMultiplier - p0.speedMultiplier)
            }
        }
        return points.last().speedMultiplier
    }

    /**
     * Calculates the adjusted duration of a clip when a speed curve is applied.
     * Integrates dt / speed(t) over normalized steps.
     */
    fun calculateCurvedDurationMs(baseDurationMs: Long, points: List<SpeedRampPoint>): Long {
        if (points.isEmpty() || (points.size == 2 && abs(points[0].speedMultiplier - 1.0f) < 0.01f && abs(points[1].speedMultiplier - 1.0f) < 0.01f)) {
            return baseDurationMs
        }
        val steps = 40
        var totalCurvedRatio = 0.0
        val dt = 1.0 / steps
        for (i in 0 until steps) {
            val midT = ((i + 0.5) * dt).toFloat()
            val spd = getSpeedAt(points, midT).coerceAtLeast(0.1f)
            totalCurvedRatio += dt / spd
        }
        return (baseDurationMs * totalCurvedRatio).toLong().coerceAtLeast(500L)
    }

    /**
     * Returns the average effective speed across the entire curve.
     */
    fun getEffectiveAverageSpeed(points: List<SpeedRampPoint>): Float {
        if (points.isEmpty()) return 1.0f
        val steps = 30
        var sum = 0.0f
        for (i in 0 until steps) {
            val t = (i.toFloat() / (steps - 1)).coerceIn(0.0f, 1.0f)
            sum += getSpeedAt(points, t)
        }
        return (sum / steps).coerceIn(0.1f, 10.0f)
    }

    /**
     * Maps timeline composition time [timelineTimeUs] to the media source PTS [sourceTimeUs] for a clip.
     * Handles:
     * - Constant speed scaling (0.25x .. 10x)
     * - Reverse playback (plays backwards from clip end PTS)
     * - Freeze frame (holds source timestamp at freeze point)
     * - Speed curve ramping
     */
    fun mapTimelineToSourceTimeUs(
        timelineTimeUs: Long,
        clipStartTimeUs: Long,
        clipDurationUs: Long,
        sourceStartUs: Long,
        sourceDurationUs: Long,
        speedMultiplier: Float = 1.0f,
        isReversed: Boolean = false,
        isFrozen: Boolean = false,
        speedCurvePreset: String = "Linear",
        speedCurvePointsStr: String? = null
    ): Long {
        if (isFrozen) {
            return sourceStartUs
        }

        val localTimeUs = (timelineTimeUs - clipStartTimeUs).coerceIn(0L, clipDurationUs)

        // Check if non-linear curve is active
        if (!speedCurvePreset.equals("Linear", ignoreCase = true) && !speedCurvePointsStr.isNullOrBlank()) {
            val points = parsePoints(speedCurvePointsStr)
            val normalizedT = if (clipDurationUs > 0) {
                (localTimeUs.toFloat() / clipDurationUs.toFloat()).coerceIn(0.0f, 1.0f)
            } else 0.0f

            // Integrate source progress up to normalizedT
            val steps = 20
            var sourceProgressFrac = 0.0f
            val subDt = normalizedT / steps.coerceAtLeast(1)
            for (i in 0 until steps) {
                val midT = (i + 0.5f) * subDt
                val instantaneousSpeed = getSpeedAt(points, midT)
                sourceProgressFrac += (subDt * instantaneousSpeed)
            }

            val sourceOffsetUs = (sourceProgressFrac.coerceIn(0.0f, 1.0f) * sourceDurationUs).toLong()

            return if (isReversed) {
                val endSourceUs = sourceStartUs + sourceDurationUs
                (endSourceUs - sourceOffsetUs).coerceIn(sourceStartUs, endSourceUs)
            } else {
                (sourceStartUs + sourceOffsetUs).coerceIn(sourceStartUs, sourceStartUs + sourceDurationUs)
            }
        }

        // Standard constant speed mapping
        val speed = speedMultiplier.coerceAtLeast(0.01f)

        return if (isReversed) {
            val elapsedSourceUs = (localTimeUs * speed).toLong()
            val endSourceUs = sourceStartUs + sourceDurationUs
            (endSourceUs - elapsedSourceUs).coerceIn(sourceStartUs, endSourceUs)
        } else {
            val sourceOffsetUs = (localTimeUs * speed).toLong()
            (sourceStartUs + sourceOffsetUs).coerceIn(sourceStartUs, sourceStartUs + sourceDurationUs)
        }
    }

    /**
     * Maps source media PTS [sourceTimeUs] back to timeline composition time [timelineTimeUs].
     */
    fun mapSourceToTimelineTimeUs(
        sourceTimeUs: Long,
        clipStartTimeUs: Long,
        sourceStartUs: Long,
        sourceDurationUs: Long,
        speedMultiplier: Float = 1.0f,
        isReversed: Boolean = false
    ): Long {
        val speed = speedMultiplier.coerceAtLeast(0.01f)
        return if (isReversed) {
            val endSourceUs = sourceStartUs + sourceDurationUs
            val diffFromEnd = (endSourceUs - sourceTimeUs).coerceAtLeast(0L)
            clipStartTimeUs + (diffFromEnd / speed).toLong()
        } else {
            val offsetUs = (sourceTimeUs - sourceStartUs).coerceAtLeast(0L)
            clipStartTimeUs + (offsetUs / speed).toLong()
        }
    }

    /**
     * Calculates the resulting timeline duration in microseconds when applying [newSpeed] to [sourceDurationUs].
     */
    fun calculateTimelineDurationUs(sourceDurationUs: Long, newSpeed: Float): Long {
        val speed = newSpeed.coerceIn(0.1f, 10.0f)
        return (sourceDurationUs / speed).toLong()
    }
}
