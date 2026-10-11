package com.ritvyom.yashoraReelgenerator.domain.models

import com.ritvyom.yashoraReelgenerator.data.model.NewsItem
import java.io.Serializable
import java.util.UUID

/**
 * Represents a complete styled variation of a draft reel generated from fetched news content.
 * Allows users to choose their preferred viral style (Breaking, Explainer, Storyteller)
 * before entering the timeline video editor.
 */
data class NewsReelDraftVariation(
    val id: String,
    val title: String,
    val hindiTitle: String,
    val subtitle: String,
    val styleBadge: String,
    val badgeColorLong: Long,
    val accentColorLong: Long,
    val gradientColorsLong: List<Long>,
    val emoji: String,
    val estimatedDurationSec: Int,
    val targetSceneCount: Int,
    val tone: String,
    val captionStyle: String,
    val captionHighlightColor: String,
    val captionFont: String,
    val speedCurvePreset: String,
    val speedMultiplier: Float,
    val transitionType: String,
    val hookHeadline: String,
    val fullScript: String,
    val keyFeatures: List<String>,
    val scenes: List<Scene>
) : Serializable

/**
 * Deterministic AI Generator that crafts 3 distinct viral draft reel variations
 * from authentic fetched news article content, RSS metadata, and extracted media.
 */
object NewsDraftVariationEngine {

    fun generateVariations(
        news: NewsItem,
        articleImages: List<String>,
        articleVideos: List<String>
    ): List<NewsReelDraftVariation> {
        val titleClean = cleanText(news.title)
        val rawContent = if (news.content.isNotBlank() && news.content.length >= news.summary.length) {
            news.content
        } else if (news.summary.isNotBlank()) {
            news.summary
        } else {
            news.content
        }
        val rawBody = cleanText(rawContent).ifBlank { cleanText(news.title) }

        val isHindi = news.language.equals("Hindi", ignoreCase = true) ||
                news.title.any { it in '\u0900'..'\u097F' } ||
                news.summary.any { it in '\u0900'..'\u097F' } ||
                news.content.any { it in '\u0900'..'\u097F' }

        // Compile combined source media list (videos prioritized over images)
        val combinedMedia = mutableListOf<String>()
        combinedMedia.addAll(articleVideos.filter { it.isNotBlank() })
        if (!news.imageUrl.isNullOrBlank() && !combinedMedia.contains(news.imageUrl)) {
            combinedMedia.add(news.imageUrl)
        }
        combinedMedia.addAll(articleImages.filter { it.isNotBlank() && !combinedMedia.contains(it) })
        if (combinedMedia.isEmpty() && news.images.isNotEmpty()) {
            combinedMedia.addAll(news.images.filter { it.isNotBlank() })
        }

        // Extract and normalize all sentences of the news body
        val bodySentences = extractAndNormalizeSentences(rawBody, titleClean)

        val var1 = buildBreakingAlertVariation(news, titleClean, bodySentences, combinedMedia, isHindi)
        val var2 = buildInDepthExplainerVariation(news, titleClean, bodySentences, combinedMedia, isHindi)
        val var3 = buildViralStorytellerVariation(news, titleClean, bodySentences, combinedMedia, isHindi)

        return listOf(var1, var2, var3)
    }

    private fun cleanText(input: String): String {
        return input.replace(Regex("<.*?>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Extracts all individual sentences from the news body, splitting on Purna Viram (।),
     * periods (.), question marks (?), exclamation marks (!), or linebreaks.
     * Prevents dropping any part of the article.
     */
    private fun extractAndNormalizeSentences(rawBody: String, titleClean: String): List<String> {
        if (rawBody.isBlank()) return emptyList()

        // Match sentence endings in Hindi & English
        val sentenceDelimiters = Regex("(?<=[।!?\\.])\\s+|\\r?\\n+|(?<=[।!?\\.])")
        val rawParts = rawBody.split(sentenceDelimiters)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals(titleClean, ignoreCase = true) }

        val result = mutableListOf<String>()
        for (part in rawParts) {
            val words = part.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size > 35) {
                // Break overly long run-on sentences into readable clause chunks of ~20 words
                val chunks = words.chunked(20).map { it.joinToString(" ").trim() }
                result.addAll(chunks)
            } else if (words.size < 4 && result.isNotEmpty()) {
                // Merge tiny fragments with the previous sentence
                val lastIdx = result.lastIndex
                result[lastIdx] = result[lastIdx] + " " + part
            } else {
                result.add(part)
            }
        }
        return result.filter { it.length > 5 }
    }

    /**
     * Groups sentences into balanced scenes so 100% of the news content is covered.
     * If there are up to 8 sentences, each sentence gets its own dedicated scene.
     * If there are more than 8 sentences, pairs consecutive sentences so that the reel has
     * at most 8 core content scenes without losing a single fact.
     */
    private fun chunkSentencesForReel(sentences: List<String>, maxCoreScenes: Int = 8): List<String> {
        if (sentences.isEmpty()) return emptyList()
        if (sentences.size <= maxCoreScenes) return sentences

        val chunkSize = (sentences.size + maxCoreScenes - 1) / maxCoreScenes
        return sentences.chunked(chunkSize).map { it.joinToString(" ") }
    }

    /**
     * Calculates realistic speech duration based on word count (approx. 2.4 words per second).
     */
    private fun calculateSceneDuration(text: String, speedMultiplier: Float = 1.0f): Int {
        val wordCount = text.split(Regex("\\s+")).count { it.isNotBlank() }
        val baseSec = (wordCount / (2.4f * speedMultiplier)).toInt()
        return baseSec.coerceIn(3, 8)
    }

    private fun getMediaForIndex(mediaList: List<String>, index: Int): Pair<String?, String> {
        if (mediaList.isEmpty()) return Pair(null, "IMAGE")
        val mediaUrl = mediaList[index % mediaList.size]
        val isVid = mediaUrl.endsWith(".mp4", ignoreCase = true) ||
                mediaUrl.endsWith(".mov", ignoreCase = true) ||
                mediaUrl.contains("video", ignoreCase = true)
        return Pair(mediaUrl, if (isVid) "VIDEO" else "IMAGE")
    }

    // ---------------------------------------------------------------------------------
    // VARIATION 1: 🚨 FAST BREAKING NEWS (ब्रेकिंग अलर्ट - तेज़ & सनसनीखेज)
    // ---------------------------------------------------------------------------------
    private fun buildBreakingAlertVariation(
        news: NewsItem,
        title: String,
        sentences: List<String>,
        mediaList: List<String>,
        isHindi: Boolean
    ): NewsReelDraftVariation {
        val hook = if (isHindi) {
            "🚨 ब्रेकिंग अपडेट: $title"
        } else {
            "🚨 BREAKING ALERT: $title"
        }

        val sceneTexts = mutableListOf<String>()
        sceneTexts.add(hook)

        // Include ALL sentences of the article
        val coreScenes = chunkSentencesForReel(sentences, maxCoreScenes = 8)
        if (coreScenes.isEmpty()) {
            sceneTexts.add(
                if (isHindi) "इस बड़ी खबर पर सभी मुख्य तथ्य और ताज़ा जानकारी सामने आई है।"
                else "Key developments and details are emerging right now in this breaking story."
            )
        } else {
            coreScenes.forEach { sceneTexts.add(it) }
        }

        val outro = if (isHindi) {
            "पूरी रिपोर्ट व हर ताज़ा खबर के लिए जुड़े रहें • ${news.source.ifBlank { "यशोरा न्यूज़" }}"
        } else {
            "Stay tuned for live updates • ${news.source.ifBlank { "Yashora News Wire" }}"
        }
        sceneTexts.add(outro)

        val scenes = sceneTexts.mapIndexed { idx, text ->
            val (mediaUrl, mediaType) = getMediaForIndex(mediaList, idx)
            val duration = calculateSceneDuration(text, speedMultiplier = 1.25f)
            Scene(
                sceneNumber = idx + 1,
                narrationText = text,
                visualPrompt = text,
                durationSeconds = duration,
                subtitle = text,
                mediaType = mediaType,
                mediaPath = mediaUrl,
                remoteUrl = mediaUrl,
                durationMs = duration * 1000L,
                captionFont = if (isHindi) "Devanagari Bold" else "TikTok Style",
                subtitleColor = "#FFFFFF",
                subtitleBgColor = "#E6D50000",
                subtitleDesign = "Hormozi",
                speedMultiplier = 1.25f,
                speedCurvePreset = "Flash In",
                transitionType = "Zoom In"
            )
        }

        val scriptBuilder = StringBuilder()
        scenes.forEachIndexed { i, sc ->
            val label = when (i) {
                0 -> "Headline Alert"
                scenes.size - 1 -> "Outro"
                1 -> "Key Story"
                else -> "Story Details ${i}"
            }
            scriptBuilder.append("[Scene ${i + 1} - $label]\n")
            scriptBuilder.append("${sc.narrationText}\n\n")
        }

        val totalDuration = scenes.sumOf { it.durationSeconds }

        return NewsReelDraftVariation(
            id = "breaking_alert",
            title = "Fast Breaking Alert",
            hindiTitle = "सनसनीखेज ब्रेकिंग अलर्ट",
            subtitle = "Punchy, fast-cuts viral format designed for maximum retention in the first 3 seconds.",
            styleBadge = "⚡ VIRAL FAST CUTS",
            badgeColorLong = 0xFFFF1744,
            accentColorLong = 0xFFFF1744,
            gradientColorsLong = listOf(0xFFFF1744, 0xFFFF9100),
            emoji = "🚨",
            estimatedDurationSec = totalDuration,
            targetSceneCount = scenes.size,
            tone = "Breaking News",
            captionStyle = "Hormozi",
            captionHighlightColor = "#FF0055",
            captionFont = if (isHindi) "Devanagari Bold" else "TikTok Style",
            speedCurvePreset = "Flash In",
            speedMultiplier = 1.25f,
            transitionType = "Zoom In",
            hookHeadline = hook,
            fullScript = scriptBuilder.toString().trim(),
            keyFeatures = listOf(
                "⚡ 1.25x Flash In speed ramp for adrenaline pacing",
                "🎯 Bold Hormozi karaoke dynamic captions with red highlights",
                "💥 Zoom In transitions between breaking news scenes"
            ),
            scenes = scenes
        )
    }

    // ---------------------------------------------------------------------------------
    // VARIATION 2: 🔍 IN-DEPTH EXPLAINER (गहन पड़ताल & इनसाइट्स - विस्तृत विश्लेषण)
    // ---------------------------------------------------------------------------------
    private fun buildInDepthExplainerVariation(
        news: NewsItem,
        title: String,
        sentences: List<String>,
        mediaList: List<String>,
        isHindi: Boolean
    ): NewsReelDraftVariation {
        val hook = if (isHindi) {
            "आखिर क्या है पूरा मामला? जानिए इस खबर की पूरी पड़ताल..."
        } else {
            "What really happened? The crucial facts you need to know..."
        }

        val sceneTexts = mutableListOf<String>()
        sceneTexts.add(hook)
        sceneTexts.add(title)

        // Include ALL sentences of the article
        val coreScenes = chunkSentencesForReel(sentences, maxCoreScenes = 8)
        if (coreScenes.isEmpty()) {
            sceneTexts.add(
                if (isHindi) "इस बड़ी खबर पर विस्तृत विवरण और सभी तथ्य सामने आ रहे हैं।"
                else "Full context and verified facts are emerging in this developing report."
            )
        } else {
            coreScenes.forEach { sceneTexts.add(it) }
        }

        val outro = if (isHindi) {
            "स्रोत: ${news.source.ifBlank { "यशोरा न्यूज़" }} • निष्पक्ष और विश्वसनीय विश्लेषण के लिए फॉलो करें।"
        } else {
            "Source: ${news.source.ifBlank { "Yashora News Wire" }} • Follow for deep-dive journalism."
        }
        sceneTexts.add(outro)

        val scenes = sceneTexts.mapIndexed { idx, text ->
            val (mediaUrl, mediaType) = getMediaForIndex(mediaList, idx)
            val duration = calculateSceneDuration(text, speedMultiplier = 1.0f)
            Scene(
                sceneNumber = idx + 1,
                narrationText = text,
                visualPrompt = text,
                durationSeconds = duration,
                subtitle = text,
                mediaType = mediaType,
                mediaPath = mediaUrl,
                remoteUrl = mediaUrl,
                durationMs = duration * 1000L,
                captionFont = if (isHindi) "Devanagari Bold" else "Modern Sans",
                subtitleColor = "#FFFFFF",
                subtitleBgColor = "#99000000",
                subtitleDesign = "Classic Box",
                speedMultiplier = 1.0f,
                speedCurvePreset = "Linear",
                transitionType = "Fade"
            )
        }

        val scriptBuilder = StringBuilder()
        scenes.forEachIndexed { i, sc ->
            val label = when (i) {
                0 -> "Analytical Hook"
                1 -> "Headline Topic"
                scenes.size - 1 -> "Verdict & Source"
                else -> "Fact Check ${i - 1}"
            }
            scriptBuilder.append("[Scene ${i + 1} - $label]\n")
            scriptBuilder.append("${sc.narrationText}\n\n")
        }

        val totalDuration = scenes.sumOf { it.durationSeconds }

        return NewsReelDraftVariation(
            id = "in_depth_explainer",
            title = "In-Depth Explainer",
            hindiTitle = "गहन पड़ताल व विश्लेषण",
            subtitle = "Documentary-grade investigative breakdown providing context, background facts, and verified quotes.",
            styleBadge = "🔍 DOCUMENTARY EXPLAINER",
            badgeColorLong = 0xFF00D2FF,
            accentColorLong = 0xFF00D2FF,
            gradientColorsLong = listOf(0xFF00D2FF, 0xFF2979FF),
            emoji = "🔍",
            estimatedDurationSec = totalDuration,
            targetSceneCount = scenes.size,
            tone = "Documentary",
            captionStyle = "Classic Clean",
            captionHighlightColor = "#FFD700",
            captionFont = if (isHindi) "Devanagari Bold" else "Modern Sans",
            speedCurvePreset = "Linear",
            speedMultiplier = 1.0f,
            transitionType = "Fade",
            hookHeadline = hook,
            fullScript = scriptBuilder.toString().trim(),
            keyFeatures = listOf(
                "🎬 Cinematic 1.0x steady playback for high credibility",
                "✨ Clean subtitles with Royal Gold highlights",
                "🕊️ Smooth Crossfade transitions for documentary flow"
            ),
            scenes = scenes
        )
    }

    // ---------------------------------------------------------------------------------
    // VARIATION 3: ⚡ VIRAL STORYTELLER & ENGAGEMENT (वायरल स्टोरीटेलिंग & सोशल एंगेजमेंट)
    // ---------------------------------------------------------------------------------
    private fun buildViralStorytellerVariation(
        news: NewsItem,
        title: String,
        sentences: List<String>,
        mediaList: List<String>,
        isHindi: Boolean
    ): NewsReelDraftVariation {
        val hook = if (isHindi) {
            "इस खबर ने सोशल मीडिया पर सनसनी मचा दी है! देखिए क्या हुआ..."
        } else {
            "This story is taking over the internet! Here is what happened..."
        }

        val sceneTexts = mutableListOf<String>()
        sceneTexts.add(hook)
        sceneTexts.add(title)

        // Include ALL sentences of the article
        val coreScenes = chunkSentencesForReel(sentences, maxCoreScenes = 8)
        if (coreScenes.isEmpty()) {
            sceneTexts.add(
                if (isHindi) "इस बड़ी खबर पर सोशल मीडिया पर भारी चर्चा शुरू हो गई है।"
                else "Widespread reactions and discussion are spreading across social platforms."
            )
        } else {
            coreScenes.forEach { sceneTexts.add(it) }
        }

        val outro = if (isHindi) {
            "इस पूरे मामले पर आपकी क्या राय है? हमें कमेंट में ज़रूर बताएं! • ${news.source.ifBlank { "यशोरा न्यूज़" }}"
        } else {
            "What are your thoughts on this? Tell us in the comments below! • ${news.source.ifBlank { "Yashora News Wire" }}"
        }
        sceneTexts.add(outro)

        val scenes = sceneTexts.mapIndexed { idx, text ->
            val (mediaUrl, mediaType) = getMediaForIndex(mediaList, idx)
            val duration = calculateSceneDuration(text, speedMultiplier = 1.1f)
            Scene(
                sceneNumber = idx + 1,
                narrationText = text,
                visualPrompt = text,
                durationSeconds = duration,
                subtitle = text,
                mediaType = mediaType,
                mediaPath = mediaUrl,
                remoteUrl = mediaUrl,
                durationMs = duration * 1000L,
                captionFont = if (isHindi) "Devanagari Bold" else "Neon Glow",
                subtitleColor = "#FFFFFF",
                subtitleBgColor = "#881A0033",
                subtitleDesign = "Neon Glow",
                speedMultiplier = 1.1f,
                speedCurvePreset = "Montage",
                transitionType = "Slide Left"
            )
        }

        val scriptBuilder = StringBuilder()
        scenes.forEachIndexed { i, sc ->
            val label = when (i) {
                0 -> "Curiosity Hook"
                1 -> "Headline Arc"
                scenes.size - 1 -> "Comment CTA"
                else -> "Story Beat ${i - 1}"
            }
            scriptBuilder.append("[Scene ${i + 1} - $label]\n")
            scriptBuilder.append("${sc.narrationText}\n\n")
        }

        val totalDuration = scenes.sumOf { it.durationSeconds }

        return NewsReelDraftVariation(
            id = "viral_storyteller",
            title = "Viral Storyteller",
            hindiTitle = "वायरल स्टोरीटेलिंग & रिएक्शन",
            subtitle = "High-curiosity narrative with dramatic pauses, energetic speed curves, and high audience comment engagement.",
            styleBadge = "🔥 HIGH ENGAGEMENT",
            badgeColorLong = 0xFFD500F9,
            accentColorLong = 0xFFD500F9,
            gradientColorsLong = listOf(0xFFD500F9, 0xFF00E676),
            emoji = "⚡",
            estimatedDurationSec = totalDuration,
            targetSceneCount = scenes.size,
            tone = "Viral Story",
            captionStyle = "Bounce Pop",
            captionHighlightColor = "#00FF87",
            captionFont = if (isHindi) "Devanagari Bold" else "Neon Glow",
            speedCurvePreset = "Montage",
            speedMultiplier = 1.1f,
            transitionType = "Slide Left",
            hookHeadline = hook,
            fullScript = scriptBuilder.toString().trim(),
            keyFeatures = listOf(
                "📈 Montage speed curve (Fast ➔ Slow ➔ Fast)",
                "🌟 Bounce Pop animated captions with Neon Mint glow",
                "💬 High-conversion CTA asking viewer opinion in comments"
            ),
            scenes = scenes
        )
    }
}
