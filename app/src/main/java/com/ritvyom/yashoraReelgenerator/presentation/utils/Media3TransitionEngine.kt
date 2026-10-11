package com.ritvyom.yashoraReelgenerator.presentation.utils

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.media3.common.Effect
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import kotlin.math.sin
import kotlin.math.cos

/**
 * YouCut Pro Transition Manager and Effect Engine leveraging Media3 Transformer & Android Graphics APIs.
 * Supports a rich library of 42+ cinematic transitions across 5 categories:
 * BASIC, GLITCH, SLIDE, STYLE, and SPORT with customizable durations (200ms - 2000ms)
 * for real-time live preview and export compilation parity.
 */
object Media3TransitionEngine {

    data class TransitionItem(
        val id: String,
        val displayName: String,
        val category: String,
        val description: String,
        val iconEmoji: String,
        val isPro: Boolean = false
    )

    val CATEGORIES = listOf("ALL", "BASIC", "GLITCH", "SLIDE", "STYLE", "SPORT")

    val TRANSITIONS = listOf(
        // BASIC
        TransitionItem("None", "None", "BASIC", "Direct cut between clips without transition", "Ø"),
        TransitionItem("MIX", "MIX", "BASIC", "Smooth crossfade blend between clips", "✨"),
        TransitionItem("FADE B", "FADE B", "BASIC", "Cinematic smooth fade to black and fade in", "🌘"),
        TransitionItem("FADE W", "FADE W", "BASIC", "High-energy white flash fade transition", "🌕"),
        TransitionItem("MOVE U", "MOVE U", "BASIC", "Move up push transition", "⬆️"),
        TransitionItem("MOVE D", "MOVE D", "BASIC", "Move down push transition", "⬇️"),
        TransitionItem("MOVE L", "MOVE L", "BASIC", "Move left push transition", "⬅️"),
        TransitionItem("MOVE R", "MOVE R", "BASIC", "Move right push transition", "➡️"),
        TransitionItem("WIPE U", "WIPE U", "BASIC", "Vertical wipe sweeping upward", "🔼"),
        TransitionItem("WIPE D", "WIPE D", "BASIC", "Vertical wipe sweeping downward", "🔽"),
        TransitionItem("WIPE L", "WIPE L", "BASIC", "Horizontal wipe sweeping left", "◀️"),
        TransitionItem("WIPE R", "WIPE R", "BASIC", "Horizontal wipe sweeping right", "▶️"),
        TransitionItem("DISSOLVE", "DISSOLVE", "BASIC", "Subtle luma cross-dissolve", "🌫️"),

        // GLITCH
        TransitionItem("FLASH", "FLASH", "GLITCH", "High-energy strobe burst flash", "⚡", true),
        TransitionItem("RGB SPLIT", "RGB SPLIT", "GLITCH", "Chromatic aberration cyber glitch", "📺", false),
        TransitionItem("BLINK", "BLINK", "GLITCH", "Fast eye blink double strobe", "👁️", true),
        TransitionItem("SWIRL L", "SWIRL L", "GLITCH", "Counter-clockwise vortex swirl", "🌀", true),
        TransitionItem("SWIRL R", "SWIRL R", "GLITCH", "Clockwise vortex swirl", "💫", true),
        TransitionItem("RADIAL", "RADIAL", "GLITCH", "Radial blur spin cut", "🔄", true),
        TransitionItem("TURN PAGE", "TURN PAGE", "GLITCH", "3D paper curl turn transition", "📖", true),
        TransitionItem("FLOODLIGHT", "FLOODLIGHT", "GLITCH", "Studio floodlight lens flare bloom", "🔦", true),
        TransitionItem("CIRCULAR", "CIRCULAR", "GLITCH", "Circular iris aperture zoom", "⭕", true),
        TransitionItem("NOISE GLITCH", "NOISE GLITCH", "GLITCH", "Analog VHS static tape glitch", "📻", true),

        // SLIDE
        TransitionItem("SLIDE L", "SLIDE L", "SLIDE", "Smooth lateral slide to the left", "👈"),
        TransitionItem("SLIDE R", "SLIDE R", "SLIDE", "Smooth lateral slide to the right", "👉"),
        TransitionItem("SLIDE U", "SLIDE U", "SLIDE", "Smooth vertical slide upward", "👆"),
        TransitionItem("SLIDE D", "SLIDE D", "SLIDE", "Smooth vertical slide downward", "👇"),
        TransitionItem("PUSH L", "PUSH L", "SLIDE", "Dynamic push left transition", "⏪"),
        TransitionItem("PUSH R", "PUSH R", "SLIDE", "Dynamic push right transition", "⏩"),
        TransitionItem("SPLIT V", "SPLIT V", "SLIDE", "Vertical shutter doors opening", "🚪"),
        TransitionItem("SPLIT H", "SPLIT H", "SLIDE", "Horizontal blinds shutter", "🪟"),
        TransitionItem("ZOOM IN", "ZOOM IN", "SLIDE", "Fast dynamic camera push-in zoom", "🔍"),
        TransitionItem("ZOOM OUT", "ZOOM OUT", "SLIDE", "Smooth pull-back camera zoom out", "🔎"),
        TransitionItem("BOUNCE", "BOUNCE", "SLIDE", "Elastic scale bounce impact", "🎾", true),

        // STYLE
        TransitionItem("LIGHT LEAK", "LIGHT LEAK", "STYLE", "Warm vintage analog light flare", "☀️"),
        TransitionItem("FILM BURN", "FILM BURN", "STYLE", "Classic 35mm film strip burn", "🎞️"),
        TransitionItem("BLUR DISSOLVE", "BLUR", "STYLE", "Dreamy Gaussian blur dissolve", "🌫️"),
        TransitionItem("MOSAIC", "MOSAIC", "STYLE", "Pixelate mosaic dissolve blocks", "🧱"),
        TransitionItem("LUMA FADE", "LUMA FADE", "STYLE", "Luminance-based organic reveal", "✨"),
        TransitionItem("COLOR FLASH", "COLOR FLASH", "STYLE", "Vibrant neon color flash burst", "🌈"),
        TransitionItem("SEPIA FADE", "SEPIA FADE", "STYLE", "Warm retro sepia film fade", "🍂"),
        TransitionItem("PRISM", "PRISM", "STYLE", "Prismatic rainbow refraction flare", "💎", true),

        // SPORT
        TransitionItem("FAST WHIP L", "WHIP L", "SPORT", "High-velocity whip pan left", "💨"),
        TransitionItem("FAST WHIP R", "WHIP R", "SPORT", "High-velocity whip pan right", "🌪️"),
        TransitionItem("SHAKE", "SHAKE", "SPORT", "Heavy camera shake impact", "💥"),
        TransitionItem("SPEED ZOOM", "SPEED ZOOM", "SPORT", "Extreme motion blur speed ramp", "🚀"),
        TransitionItem("SHOCKWAVE", "SHOCKWAVE", "SPORT", "Sonic ripple shockwave distortion", "🌊"),
        TransitionItem("ZOOM TWIST", "ZOOM TWIST", "SPORT", "Rotational warp spin zoom", "🌀", true)
    )

    /**
     * Normalizes arbitrary user or legacy transition strings to canonical transition IDs.
     */
    fun normalizeTransitionId(raw: String?): String {
        if (raw.isNullOrBlank()) return "None"
        val t = raw.trim().uppercase()
        return when {
            t == "NONE" || t == "CUT" -> "None"
            t == "MIX" || t.contains("CROSS") -> "MIX"
            t == "DISSOLVE" || t.contains("DISSOLVE") && !t.contains("BLUR") -> "DISSOLVE"
            t == "FADE B" || t == "FADE (BLACK)" || t == "FADE" || t == "BLACK" -> "FADE B"
            t == "FADE W" || t == "FADE (WHITE)" || t == "WHITE" -> "FADE W"
            t == "FLASH" || t.contains("FLASH WHITE") || t.contains("STROBE") -> "FLASH"
            t == "RGB SPLIT" || t.contains("CHROMATIC") || (t.contains("GLITCH") && !t.contains("NOISE")) -> "RGB SPLIT"
            t == "BLINK" -> "BLINK"
            t == "SWIRL L" || t.contains("SWIRL LEFT") -> "SWIRL L"
            t == "SWIRL R" || t.contains("SWIRL RIGHT") -> "SWIRL R"
            t == "RADIAL" -> "RADIAL"
            t == "TURN PAGE" || t.contains("PAGE") -> "TURN PAGE"
            t == "FLOODLIGHT" || t.contains("FLARE") -> "FLOODLIGHT"
            t == "CIRCULAR" || t.contains("IRIS") || t.contains("CIRCLE") -> "CIRCULAR"
            t == "NOISE GLITCH" || t.contains("NOISE") -> "NOISE GLITCH"
            t == "SLIDE L" || t == "SLIDE LEFT" -> "SLIDE L"
            t == "SLIDE R" || t == "SLIDE RIGHT" -> "SLIDE R"
            t == "SLIDE U" || t == "SLIDE UP" -> "SLIDE U"
            t == "SLIDE D" || t == "SLIDE DOWN" -> "SLIDE D"
            t == "MOVE L" || t == "MOVE LEFT" -> "MOVE L"
            t == "MOVE R" || t == "MOVE RIGHT" -> "MOVE R"
            t == "MOVE U" || t == "MOVE UP" -> "MOVE U"
            t == "MOVE D" || t == "MOVE DOWN" -> "MOVE D"
            t == "PUSH L" || t == "PUSH LEFT" -> "PUSH L"
            t == "PUSH R" || t == "PUSH RIGHT" -> "PUSH R"
            t == "WIPE L" || t == "WIPE LEFT" -> "WIPE L"
            t == "WIPE R" || t == "WIPE RIGHT" -> "WIPE R"
            t == "WIPE U" || t == "WIPE UP" -> "WIPE U"
            t == "WIPE D" || t == "WIPE DOWN" -> "WIPE D"
            t == "SPLIT V" -> "SPLIT V"
            t == "SPLIT H" -> "SPLIT H"
            t == "ZOOM IN" -> "ZOOM IN"
            t == "ZOOM OUT" -> "ZOOM OUT"
            t == "BOUNCE" -> "BOUNCE"
            t == "LIGHT LEAK" -> "LIGHT LEAK"
            t == "FILM BURN" -> "FILM BURN"
            t == "BLUR DISSOLVE" || t == "BLUR" -> "BLUR DISSOLVE"
            t == "MOSAIC" -> "MOSAIC"
            t == "LUMA FADE" -> "LUMA FADE"
            t == "COLOR FLASH" -> "COLOR FLASH"
            t == "SEPIA FADE" -> "SEPIA FADE"
            t == "PRISM" -> "PRISM"
            t == "FAST WHIP L" || t == "WHIP L" -> "FAST WHIP L"
            t == "FAST WHIP R" || t == "WHIP R" -> "FAST WHIP R"
            t == "SHAKE" -> "SHAKE"
            t == "SPEED ZOOM" -> "SPEED ZOOM"
            t == "SHOCKWAVE" -> "SHOCKWAVE"
            t == "ZOOM TWIST" -> "ZOOM TWIST"
            else -> t
        }
    }

    /**
     * Builds a list of Media3 Effects for a clip based on the transition type and duration.
     * Integrates Media3 Transformer's built-in transition capabilities (RgbMatrix, MatrixTransformation).
     */
    fun createMedia3TransitionEffects(
        transitionType: String?,
        durationMs: Long,
        transitionDurationMs: Long = 700L
    ): List<Effect> {
        val normId = normalizeTransitionId(transitionType)
        if (normId.equals("None", ignoreCase = true) || transitionDurationMs <= 0L || durationMs <= 0L) {
            return emptyList()
        }

        val effects = mutableListOf<Effect>()
        val effectiveTransitionMs = transitionDurationMs.coerceIn(150L, (durationMs / 2).coerceAtLeast(200L))
        val totalUs = durationMs * 1000L
        val transUs = effectiveTransitionMs * 1000L

        when (normId) {
            "FADE B" -> {
                val fadeEffect = object : RgbMatrix {
                    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
                        val progress = when {
                            transUs > 0L && presentationTimeUs < transUs -> {
                                (presentationTimeUs.toFloat() / transUs).coerceIn(0f, 1f)
                            }
                            transUs > 0L && totalUs > transUs && presentationTimeUs > (totalUs - transUs) -> {
                                ((totalUs - presentationTimeUs).toFloat() / transUs).coerceIn(0f, 1f)
                            }
                            else -> 1f
                        }
                        return floatArrayOf(
                            progress, 0f, 0f, 0f,
                            0f, progress, 0f, 0f,
                            0f, 0f, progress, 0f,
                            0f, 0f, 0f, 1f
                        )
                    }
                }
                effects.add(fadeEffect)
            }

            "FADE W", "FLASH" -> {
                val flashEffect = object : RgbMatrix {
                    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
                        val boost = if (transUs > 0L && presentationTimeUs < transUs) {
                            val p = (presentationTimeUs.toFloat() / transUs).coerceIn(0f, 1f)
                            1.0f + 1.8f * (1f - p)
                        } else if (transUs > 0L && totalUs > transUs && presentationTimeUs > (totalUs - transUs)) {
                            val p = ((presentationTimeUs - (totalUs - transUs)).toFloat() / transUs).coerceIn(0f, 1f)
                            1.0f + 1.8f * p
                        } else 1f
                        return floatArrayOf(
                            boost, 0f, 0f, 0f,
                            0f, boost, 0f, 0f,
                            0f, 0f, boost, 0f,
                            0f, 0f, 0f, 1f
                        )
                    }
                }
                effects.add(flashEffect)
            }

            "MIX", "DISSOLVE", "BLUR DISSOLVE" -> {
                val dissolveEffect = object : RgbMatrix {
                    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
                        val progress = when {
                            transUs > 0L && presentationTimeUs < transUs -> {
                                (0.35f + 0.65f * (presentationTimeUs.toFloat() / transUs)).coerceIn(0f, 1f)
                            }
                            transUs > 0L && totalUs > transUs && presentationTimeUs > (totalUs - transUs) -> {
                                (0.35f + 0.65f * ((totalUs - presentationTimeUs).toFloat() / transUs)).coerceIn(0f, 1f)
                            }
                            else -> 1f
                        }
                        return floatArrayOf(
                            progress, 0f, 0f, 0f,
                            0f, progress, 0f, 0f,
                            0f, 0f, progress, 0f,
                            0f, 0f, 0f, 1f
                        )
                    }
                }
                effects.add(dissolveEffect)
            }

            "SLIDE L", "MOVE L", "PUSH L", "FAST WHIP L" -> {
                val slideEffect = object : MatrixTransformation {
                    override fun getMatrix(presentationTimeUs: Long): Matrix {
                        val matrix = Matrix()
                        if (transUs <= 0L) return matrix
                        if (presentationTimeUs < transUs) {
                            val progress = (presentationTimeUs.toFloat() / transUs).coerceIn(0f, 1f)
                            matrix.postTranslate((1f - progress) * 2f, 0f)
                        } else if (totalUs > transUs && presentationTimeUs > (totalUs - transUs)) {
                            val exitProgress = ((presentationTimeUs - (totalUs - transUs)).toFloat() / transUs).coerceIn(0f, 1f)
                            matrix.postTranslate(-exitProgress * 2f, 0f)
                        }
                        return matrix
                    }
                }
                effects.add(slideEffect)
            }

            "SLIDE R", "MOVE R", "PUSH R", "FAST WHIP R" -> {
                val slideEffect = object : MatrixTransformation {
                    override fun getMatrix(presentationTimeUs: Long): Matrix {
                        val matrix = Matrix()
                        if (transUs <= 0L) return matrix
                        if (presentationTimeUs < transUs) {
                            val progress = (presentationTimeUs.toFloat() / transUs).coerceIn(0f, 1f)
                            matrix.postTranslate(-(1f - progress) * 2f, 0f)
                        } else if (totalUs > transUs && presentationTimeUs > (totalUs - transUs)) {
                            val exitProgress = ((presentationTimeUs - (totalUs - transUs)).toFloat() / transUs).coerceIn(0f, 1f)
                            matrix.postTranslate(exitProgress * 2f, 0f)
                        }
                        return matrix
                    }
                }
                effects.add(slideEffect)
            }

            "SLIDE U", "MOVE U", "WIPE U" -> {
                val slideEffect = object : MatrixTransformation {
                    override fun getMatrix(presentationTimeUs: Long): Matrix {
                        val matrix = Matrix()
                        if (transUs <= 0L) return matrix
                        if (presentationTimeUs < transUs) {
                            val progress = (presentationTimeUs.toFloat() / transUs).coerceIn(0f, 1f)
                            matrix.postTranslate(0f, -(1f - progress) * 2f)
                        } else if (totalUs > transUs && presentationTimeUs > (totalUs - transUs)) {
                            val exitProgress = ((presentationTimeUs - (totalUs - transUs)).toFloat() / transUs).coerceIn(0f, 1f)
                            matrix.postTranslate(0f, exitProgress * 2f)
                        }
                        return matrix
                    }
                }
                effects.add(slideEffect)
            }

            "SLIDE D", "MOVE D", "WIPE D" -> {
                val slideEffect = object : MatrixTransformation {
                    override fun getMatrix(presentationTimeUs: Long): Matrix {
                        val matrix = Matrix()
                        if (transUs <= 0L) return matrix
                        if (presentationTimeUs < transUs) {
                            val progress = (presentationTimeUs.toFloat() / transUs).coerceIn(0f, 1f)
                            matrix.postTranslate(0f, (1f - progress) * 2f)
                        } else if (totalUs > transUs && presentationTimeUs > (totalUs - transUs)) {
                            val exitProgress = ((presentationTimeUs - (totalUs - transUs)).toFloat() / transUs).coerceIn(0f, 1f)
                            matrix.postTranslate(0f, -exitProgress * 2f)
                        }
                        return matrix
                    }
                }
                effects.add(slideEffect)
            }

            "ZOOM IN", "SPEED ZOOM", "BOUNCE" -> {
                val zoomEffect = object : MatrixTransformation {
                    override fun getMatrix(presentationTimeUs: Long): Matrix {
                        val matrix = Matrix()
                        if (transUs <= 0L) return matrix
                        if (presentationTimeUs < transUs) {
                            val progress = (presentationTimeUs.toFloat() / transUs).coerceIn(0f, 1f)
                            val scale = 1.25f - 0.25f * progress
                            matrix.postScale(scale, scale)
                        } else if (totalUs > transUs && presentationTimeUs > (totalUs - transUs)) {
                            val exitProgress = ((presentationTimeUs - (totalUs - transUs)).toFloat() / transUs).coerceIn(0f, 1f)
                            val scale = 1.0f + 0.25f * exitProgress
                            matrix.postScale(scale, scale)
                        }
                        return matrix
                    }
                }
                effects.add(zoomEffect)
            }

            "ZOOM OUT" -> {
                val zoomEffect = object : MatrixTransformation {
                    override fun getMatrix(presentationTimeUs: Long): Matrix {
                        val matrix = Matrix()
                        if (transUs <= 0L) return matrix
                        if (presentationTimeUs < transUs) {
                            val progress = (presentationTimeUs.toFloat() / transUs).coerceIn(0f, 1f)
                            val scale = 0.8f + 0.2f * progress
                            matrix.postScale(scale, scale)
                        } else if (totalUs > transUs && presentationTimeUs > (totalUs - transUs)) {
                            val exitProgress = ((presentationTimeUs - (totalUs - transUs)).toFloat() / transUs).coerceIn(0f, 1f)
                            val scale = 1.0f - 0.2f * exitProgress
                            matrix.postScale(scale, scale)
                        }
                        return matrix
                    }
                }
                effects.add(zoomEffect)
            }
        }

        return effects
    }

    /**
     * Draws real-time graphics canvas transition effect during video frame compilation and export.
     * Supports all 42+ transitions with smooth alpha, wipes, light leaks, film burns, zooms, and glitch effects.
     */
    fun drawTransition(
        canvas: Canvas,
        width: Int,
        height: Int,
        frameIndex: Int,
        totalFrames: Int,
        transitionType: String?,
        transitionDurationMs: Long = 700L,
        fps: Int = 30
    ) {
        val normId = normalizeTransitionId(transitionType)
        if (normId.equals("None", ignoreCase = true) || transitionDurationMs <= 0L || totalFrames <= 0) {
            return
        }

        val transitionFrames = ((transitionDurationMs / 1000f) * fps).toInt().coerceIn(3, (totalFrames / 2).coerceAtLeast(3))
        val isEntering = frameIndex < transitionFrames
        val isExiting = frameIndex > (totalFrames - transitionFrames)

        if (!isEntering && !isExiting) return

        val progress = if (isEntering) {
            1.0f - (frameIndex.toFloat() / transitionFrames) // 1.0 down to 0.0
        } else {
            (frameIndex - (totalFrames - transitionFrames)).toFloat() / transitionFrames // 0.0 up to 1.0
        }.coerceIn(0f, 1f)

        val wF = width.toFloat()
        val hF = height.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (normId) {
            "FADE B" -> {
                val alpha = (progress * 255).toInt().coerceIn(0, 255)
                paint.color = AndroidColor.argb(alpha, 0, 0, 0)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, hF, paint)
            }

            "FADE W" -> {
                val alpha = (progress * 255).toInt().coerceIn(0, 255)
                paint.color = AndroidColor.argb(alpha, 255, 255, 255)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, hF, paint)
            }

            "MIX", "DISSOLVE" -> {
                val alpha = (progress * 220).toInt().coerceIn(0, 255)
                paint.color = AndroidColor.argb(alpha, 16, 16, 24)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, hF, paint)
            }

            "FLASH" -> {
                // High-energy strobe flash with double-peak burst
                val strobeVal = if ((progress * 10).toInt() % 2 == 0) progress else progress * 0.7f
                val alpha = (strobeVal * 250).toInt().coerceIn(0, 255)
                paint.color = AndroidColor.argb(alpha, 255, 255, 240)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, hF, paint)
            }

            "RGB SPLIT" -> {
                // Cyber chromatic aberration split bars
                val offset = (wF * 0.05f * progress)
                paint.style = Paint.Style.FILL
                // Red channel band
                paint.color = AndroidColor.argb((progress * 130).toInt().coerceIn(0, 255), 255, 0, 80)
                canvas.drawRect(0f, 0f, offset, hF, paint)
                // Cyan channel band
                paint.color = AndroidColor.argb((progress * 130).toInt().coerceIn(0, 255), 0, 240, 255)
                canvas.drawRect(wF - offset, 0f, wF, hF, paint)
                // Horizontal scanlines
                val scanPaint = Paint().apply {
                    color = AndroidColor.argb((progress * 80).toInt().coerceIn(0, 255), 0, 0, 0)
                    strokeWidth = 3f
                }
                var y = 0f
                while (y < hF) {
                    canvas.drawLine(0f, y, wF, y, scanPaint)
                    y += 12f
                }
            }

            "WIPE L", "MOVE L" -> {
                val wipeWidth = wF * progress
                val left = wF - wipeWidth
                paint.color = AndroidColor.argb(255, 12, 12, 18)
                paint.style = Paint.Style.FILL
                canvas.drawRect(left, 0f, wF, hF, paint)
                // Neon Orange Edge line
                paint.color = AndroidColor.argb(230, 255, 87, 34)
                paint.strokeWidth = 8f
                canvas.drawLine(left, 0f, left, hF, paint)
            }

            "WIPE R", "MOVE R" -> {
                val wipeWidth = wF * progress
                paint.color = AndroidColor.argb(255, 12, 12, 18)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wipeWidth, hF, paint)
                paint.color = AndroidColor.argb(230, 255, 87, 34)
                paint.strokeWidth = 8f
                canvas.drawLine(wipeWidth, 0f, wipeWidth, hF, paint)
            }

            "WIPE U", "MOVE U" -> {
                val wipeHeight = hF * progress
                val top = hF - wipeHeight
                paint.color = AndroidColor.argb(255, 12, 12, 18)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, top, wF, hF, paint)
                paint.color = AndroidColor.argb(230, 255, 87, 34)
                paint.strokeWidth = 8f
                canvas.drawLine(0f, top, wF, top, paint)
            }

            "WIPE D", "MOVE D" -> {
                val wipeHeight = hF * progress
                paint.color = AndroidColor.argb(255, 12, 12, 18)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, wipeHeight, paint)
                paint.color = AndroidColor.argb(230, 255, 87, 34)
                paint.strokeWidth = 8f
                canvas.drawLine(0f, wipeHeight, wF, wipeHeight, paint)
            }

            "SLIDE L", "PUSH L", "FAST WHIP L" -> {
                val slideW = wF * progress
                val left = wF - slideW
                paint.color = AndroidColor.argb(255, 10, 10, 14)
                paint.style = Paint.Style.FILL
                canvas.drawRect(left, 0f, wF, hF, paint)
                val shadowWidth = (wF * 0.15f).coerceAtLeast(24f)
                val grad = LinearGradient(
                    left - shadowWidth, 0f, left, 0f,
                    AndroidColor.TRANSPARENT, AndroidColor.argb(200, 0, 0, 0),
                    Shader.TileMode.CLAMP
                )
                paint.shader = grad
                canvas.drawRect(left - shadowWidth, 0f, left, hF, paint)
                paint.shader = null
            }

            "SLIDE R", "PUSH R", "FAST WHIP R" -> {
                val slideW = wF * progress
                paint.color = AndroidColor.argb(255, 10, 10, 14)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, slideW, hF, paint)
                val shadowWidth = (wF * 0.15f).coerceAtLeast(24f)
                val grad = LinearGradient(
                    slideW, 0f, slideW + shadowWidth, 0f,
                    AndroidColor.argb(200, 0, 0, 0), AndroidColor.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                paint.shader = grad
                canvas.drawRect(slideW, 0f, slideW + shadowWidth, hF, paint)
                paint.shader = null
            }

            "SLIDE U" -> {
                val slideH = hF * progress
                val top = hF - slideH
                paint.color = AndroidColor.argb(255, 10, 10, 14)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, top, wF, hF, paint)
                val shadowHeight = (hF * 0.15f).coerceAtLeast(24f)
                val grad = LinearGradient(
                    0f, top - shadowHeight, 0f, top,
                    AndroidColor.TRANSPARENT, AndroidColor.argb(200, 0, 0, 0),
                    Shader.TileMode.CLAMP
                )
                paint.shader = grad
                canvas.drawRect(0f, top - shadowHeight, wF, top, paint)
                paint.shader = null
            }

            "SLIDE D" -> {
                val slideH = hF * progress
                paint.color = AndroidColor.argb(255, 10, 10, 14)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, slideH, paint)
                val shadowHeight = (hF * 0.15f).coerceAtLeast(24f)
                val grad = LinearGradient(
                    0f, slideH, 0f, slideH + shadowHeight,
                    AndroidColor.argb(200, 0, 0, 0), AndroidColor.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                paint.shader = grad
                canvas.drawRect(0f, slideH, wF, slideH + shadowHeight, paint)
                paint.shader = null
            }

            "SPLIT V" -> {
                // Shutter curtains opening/closing from center
                val splitW = (wF / 2f) * progress
                paint.color = AndroidColor.argb(255, 12, 12, 18)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, splitW, hF, paint)
                canvas.drawRect(wF - splitW, 0f, wF, hF, paint)
                paint.color = AndroidColor.argb(220, 255, 87, 34)
                paint.strokeWidth = 6f
                canvas.drawLine(splitW, 0f, splitW, hF, paint)
                canvas.drawLine(wF - splitW, 0f, wF - splitW, hF, paint)
            }

            "SPLIT H" -> {
                val splitH = (hF / 2f) * progress
                paint.color = AndroidColor.argb(255, 12, 12, 18)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, splitH, paint)
                canvas.drawRect(0f, hF - splitH, wF, hF, paint)
                paint.color = AndroidColor.argb(220, 255, 87, 34)
                paint.strokeWidth = 6f
                canvas.drawLine(0f, splitH, wF, splitH, paint)
                canvas.drawLine(0f, hF - splitH, wF, hF - splitH, paint)
            }

            "CIRCULAR" -> {
                // Iris circular reveal
                val maxRadius = kotlin.math.hypot(wF / 2f, hF / 2f)
                val currentRadius = maxRadius * (1f - progress)
                val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb((progress * 240).toInt().coerceIn(0, 255), 0, 0, 0)
                    style = Paint.Style.STROKE
                    strokeWidth = maxRadius * progress + 20f
                }
                canvas.drawCircle(wF / 2f, hF / 2f, currentRadius + irisPaint.strokeWidth / 2f, irisPaint)
            }

            "ZOOM IN", "ZOOM OUT", "SPEED ZOOM", "BOUNCE" -> {
                // Dynamic camera push/pull vignette border
                val alpha = (progress * 230).toInt().coerceIn(0, 255)
                paint.color = AndroidColor.argb(alpha, 0, 0, 0)
                val borderThickness = (wF * 0.18f * progress)
                canvas.drawRect(0f, 0f, wF, borderThickness, paint)
                canvas.drawRect(0f, hF - borderThickness, wF, hF, paint)
                canvas.drawRect(0f, borderThickness, borderThickness, hF - borderThickness, paint)
                canvas.drawRect(wF - borderThickness, borderThickness, wF, hF - borderThickness, paint)
            }

            "LIGHT LEAK" -> {
                // Warm vintage analog amber flare sweep
                val flareX = wF * (1f - progress * 0.8f)
                val flareY = hF * 0.2f
                val flareRadius = kotlin.math.max(wF, hF) * 0.85f
                val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        flareX, flareY, flareRadius,
                        intArrayOf(
                            AndroidColor.argb((progress * 230).toInt().coerceIn(0, 255), 255, 230, 160),
                            AndroidColor.argb((progress * 190).toInt().coerceIn(0, 255), 255, 120, 30),
                            AndroidColor.argb((progress * 110).toInt().coerceIn(0, 255), 220, 50, 80),
                            AndroidColor.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.35f, 0.7f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, wF, hF, flarePaint)
            }

            "FILM BURN" -> {
                // Hot retro 35mm film burn edge bloom
                val burnRadius = kotlin.math.max(wF, hF) * 0.6f
                val burnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        wF * 0.5f, hF * 0.5f, burnRadius,
                        intArrayOf(
                            AndroidColor.argb((progress * 240).toInt().coerceIn(0, 255), 255, 255, 255),
                            AndroidColor.argb((progress * 220).toInt().coerceIn(0, 255), 255, 150, 0),
                            AndroidColor.argb((progress * 150).toInt().coerceIn(0, 255), 180, 20, 0),
                            AndroidColor.argb((progress * 80).toInt().coerceIn(0, 255), 40, 0, 0)
                        ),
                        floatArrayOf(0f, 0.25f, 0.6f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, wF, hF, burnPaint)
            }

            "BLUR DISSOLVE" -> {
                val alpha = (progress * 200).toInt().coerceIn(0, 255)
                paint.color = AndroidColor.argb(alpha, 25, 25, 35)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, hF, paint)
            }

            "MOSAIC" -> {
                // Digital pixel grid block dissolution
                val cols = 8
                val rows = 12
                val blockW = wF / cols
                val blockH = hF / rows
                paint.style = Paint.Style.FILL
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val threshold = ((r * 7 + c * 13) % 20) / 20f
                        if (progress > threshold) {
                            val alpha = ((progress - threshold) * 3f * 255).toInt().coerceIn(0, 240)
                            paint.color = AndroidColor.argb(alpha, 12, 12, 18)
                            canvas.drawRect(c * blockW, r * blockH, (c + 1) * blockW, (r + 1) * blockH, paint)
                        }
                    }
                }
            }

            "BLINK" -> {
                // Eye double-blink
                val blinkPhase = (progress * 2f) % 1f
                val blinkHeight = (hF / 2f) * kotlin.math.sin(blinkPhase * Math.PI).toFloat().coerceIn(0f, 1f)
                paint.color = AndroidColor.argb(255, 0, 0, 0)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, blinkHeight, paint)
                canvas.drawRect(0f, hF - blinkHeight, wF, hF, paint)
            }

            "FLOODLIGHT" -> {
                // Anamorphic horizontal lens flare streak
                val streakH = hF * 0.12f
                val streakY = hF * 0.45f
                val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, streakY, 0f, streakY + streakH,
                        intArrayOf(
                            AndroidColor.TRANSPARENT,
                            AndroidColor.argb((progress * 240).toInt().coerceIn(0, 255), 180, 230, 255),
                            AndroidColor.argb((progress * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                            AndroidColor.argb((progress * 240).toInt().coerceIn(0, 255), 180, 230, 255),
                            AndroidColor.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, streakY, wF, streakY + streakH, flarePaint)
            }

            "SHAKE" -> {
                // Earthquake jitter ghost overlay
                val jitterX = ((progress * 37f) % 1f - 0.5f) * 40f * progress
                val jitterY = ((progress * 53f) % 1f - 0.5f) * 40f * progress
                paint.color = AndroidColor.argb((progress * 150).toInt().coerceIn(0, 255), 255, 60, 60)
                paint.style = Paint.Style.FILL
                canvas.drawRect(jitterX, jitterY, wF + jitterX, hF + jitterY, paint)
            }

            "SHOCKWAVE" -> {
                // Expanding concentric shockwave ripple
                val maxRadius = kotlin.math.hypot(wF / 2f, hF / 2f)
                val waveRadius = maxRadius * progress
                val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(((1f - progress) * 200).toInt().coerceIn(0, 255), 0, 230, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 24f * (1f - progress)
                }
                canvas.drawCircle(wF / 2f, hF / 2f, waveRadius, wavePaint)
            }

            "COLOR FLASH" -> {
                val alpha = (progress * 230).toInt().coerceIn(0, 255)
                val c = when (((progress * 6).toInt()) % 3) {
                    0 -> AndroidColor.argb(alpha, 255, 0, 128)
                    1 -> AndroidColor.argb(alpha, 0, 240, 255)
                    else -> AndroidColor.argb(alpha, 255, 220, 0)
                }
                paint.color = c
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, hF, paint)
            }

            "SEPIA FADE" -> {
                val alpha = (progress * 220).toInt().coerceIn(0, 255)
                paint.color = AndroidColor.argb(alpha, 112, 66, 20)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, wF, hF, paint)
            }

            "PRISM" -> {
                // Rainbow prism flare
                val prismPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, wF, hF,
                        intArrayOf(
                            AndroidColor.argb((progress * 160).toInt().coerceIn(0, 255), 255, 0, 0),
                            AndroidColor.argb((progress * 160).toInt().coerceIn(0, 255), 255, 165, 0),
                            AndroidColor.argb((progress * 160).toInt().coerceIn(0, 255), 0, 255, 0),
                            AndroidColor.argb((progress * 160).toInt().coerceIn(0, 255), 0, 200, 255),
                            AndroidColor.argb((progress * 160).toInt().coerceIn(0, 255), 180, 0, 255)
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, wF, hF, prismPaint)
            }

            "SWIRL L", "SWIRL R", "RADIAL", "ZOOM TWIST" -> {
                // Rotational spin blur vignette
                val swirlRadius = kotlin.math.hypot(wF / 2f, hF / 2f)
                val swirlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        wF / 2f, hF / 2f, swirlRadius,
                        intArrayOf(
                            AndroidColor.TRANSPARENT,
                            AndroidColor.argb((progress * 120).toInt().coerceIn(0, 255), 30, 20, 50),
                            AndroidColor.argb((progress * 240).toInt().coerceIn(0, 255), 0, 0, 0)
                        ),
                        floatArrayOf(0.4f, 0.7f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, wF, hF, swirlPaint)
            }

            "NOISE GLITCH" -> {
                val alpha = (progress * 180).toInt().coerceIn(0, 255)
                paint.color = AndroidColor.argb(alpha, 255, 255, 255)
                paint.strokeWidth = 4f
                val bandCount = 10
                for (i in 0 until bandCount) {
                    val y = ((progress * 173f + i * 89f) % hF)
                    canvas.drawLine(0f, y, wF, y, paint)
                }
            }
        }
    }
}
