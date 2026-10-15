package com.ritvyom.yashoraReelgenerator.presentation.viewmodels

import android.util.Log
import com.ritvyom.yashoraReelgenerator.data.remote.ScriptSemanticAnalysisEngine
import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import com.ritvyom.yashoraReelgenerator.domain.models.SceneVisualMetadata

/**
 * Unified Prompt Refinement Layer.
 *
 * Enforces strict scene-to-visual mapping by explicitly separating the narrative spoken
 * script from scene-based visual metadata (concrete stock keywords, lighting, composition,
 * action, subject) before calling the Gemini API and downstream stock media resolvers.
 *
 * This layer guarantees:
 * 1. Narrative Decoupling: Speech text is stripped of visual cues and bracketed director notes
 *    so Text-to-Speech stays smooth, fluent, and natural.
 * 2. Visual Metadata Isolation: Concrete stock query tokens (nouns & action verbs), lighting,
 *    camera framing/angle, and physical subject details are strictly extracted and structured.
 * 3. Enforced Visual Prompt Framing: Pre-conditions visual prompts for Gemini analysis and
 *    post-refines Gemini returned scenes to eliminate abstract fluff ("cinematic", "atmospheric",
 *    "candid", "scene") and replace with exact real-world stock footage queries.
 */
object UnifiedPromptRefinementLayer {

    private const val TAG = "PromptRefinementLayer"

    data class RefinedSceneDraft(
        val sceneNumber: Int,
        val cleanNarration: String,
        val visualMetadata: SceneVisualMetadata
    )

    data class RefinementPlan(
        val cleanedScript: String,
        val refinedSceneDrafts: List<RefinedSceneDraft>,
        val visualDirectivesPromptChunk: String
    )

    /**
     * Cleans raw news headlines and article topics into concise, high-signal core topics.
     * Strips clickbait prefixes ("Viral Video:", "Breaking News:"), news channel tags
     * ("- NDTV", "| Aaj Tak", "- BBC News"), punctuation, and media clutter.
     */
    fun cleanNewsTopic(rawTopic: String): String {
        if (rawTopic.isBlank()) return ""
        var cleaned = rawTopic.trim()

        // Strip common news organization and source branding suffixes
        val sourcePatterns = listOf(
            Regex("(?i)\\s*[-|–—]\\s*(NDTV|Aaj\\s*Tak|ABP\\s*News|Dainik\\s*Bhaskar|India\\s*Today|BBC(\\s*Hindi)?|Zee\\s*News|News18|Republic(\\s*Bharat)?|Times\\s*of\\s*India|Hindustan\\s*Times|The\\s*Hindu|Amar\\s*Ujala|Navbharat\\s*Times|Live\\s*Hindustan|News24|PTI|ANI|Reuters|AP|AFP|OneIndia|CNBC|Bloomberg)[^\\n]*$"),
            Regex("(?i)\\s*\\|\\s*[^|]+$"),
            Regex("(?i)\\s*[-–—]\\s*[^–—-]+$")
        )
        for (pattern in sourcePatterns) {
            val stripped = cleaned.replace(pattern, "").trim()
            if (stripped.length >= 4) {
                cleaned = stripped
                break
            }
        }

        // Strip news alert prefixes
        cleaned = cleaned.replace(
            Regex("(?i)^(\\s*(Breaking(\\s*News|\\s*Alert)?|Viral(\\s*News|\\s*Video)?|Big\\s*Breaking|Trending|Exclusive|Special\\s*Report|Live\\s*Updates?|Watch|Top\\s*News|Headline|ताजा\\s*खबर|बड़ी\\s*खबर|ब्रेकिंग(\\s*न्यूज)?|वायरल(\\s*वीडियो)?|खास\\s*रिपोर्ट|लाइव|एक्सक्लूसिव)\\s*[:\\-–—|!]+)+"),
            ""
        ).trim()

        // Strip leading/trailing symbols, brackets, and emojis
        cleaned = cleaned.replace(Regex("^[\\p{Punct}\\s\\p{So}\\p{Sk}]+"), "")
            .replace(Regex("[\\p{Punct}\\s\\p{So}\\p{Sk}]+$"), "")
            .replace(Regex("[🚨⚡🎬💥🔴📰✨📢🔥]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return cleaned
    }

    /**
     * Comprehensively strips scriptwriting cue tags, markdown artifacts, emojis,
     * and news structure labels (e.g. "1. Hook:", "2. Core Story:", "[Scene 1 - Headline Alert]")
     * to produce 100% pure, natural narrative voiceover prose for Text-to-Speech and semantic visual analysis.
     */
    fun cleanScriptText(rawScript: String): String {
        val trimmed = rawScript.trim()
        if (trimmed.isEmpty()) return ""

        var s = trimmed
        // 1. Remove bracketed scene/visual tags [Scene 1: ...], [Visual: ...], [Hook: ...]
        s = s.replace(
            Regex("(?i)\\[(Scene\\s*\\d+|Hook|Headline|Intro|Core\\s*Story|Story\\s*Details|Fact\\s*\\d*|Outro|CTA|Visual|Narration|Voiceover|Audio|Music|SFX|Sound|Lighting|Camera)[^\\]]*\\]"),
            " "
        )

        // 2. Remove parenthetical visual/director cue notes (Visual: ...), (Cut to ...)
        s = s.replace(
            Regex("(?i)\\((Visual|Camera|Cut\\s*to|Audio|Music|SFX|Sound|Hook|Scene\\s*\\d+)[^\\)]*\\)"),
            " "
        )

        // 3. Remove numbered/bulleted news section headers like "**1. Hook:**", "2. Core Story Facts:", "1. हुक:"
        s = s.replace(
            Regex("(?mi)^(?:\\[\\s*)?(?:\\*\\*)?(?:\\d+[.:)]\\s*)?(Hook|Headline|Intro|Core\\s*Story|Story\\s*Facts|Key\\s*Facts|Facts|Impact|Closing\\s*Question|Question|Outro|CTA|Call\\s*to\\s*action|Visual|Voiceover|Narration|हुक|मुख्य\\s*खबर|खबर|प्रभाव|सवाल|निष्कर्ष|अलर्ट|ब्रेकिंग)(?:\\s*\\([^)]*\\))?[:\\-*_\\s]+(?:\\*\\*)?"),
            " "
        )

        // 4. Strip leftover markdown artifacts
        s = s.replace(Regex("\\*\\*|__|[*_#]"), "")

        // 5. Strip news emojis (🚨, 🎬, etc.)
        s = s.replace(Regex("[🚨⚡🎬💥🔴📰✨📢🔥]"), "")

        // 6. Clean consecutive dots / ellipses
        s = s.replace(Regex("\\.{2,}"), ".")

        // 7. Normalize whitespace
        s = s.replace(Regex("\\s+"), " ").trim()

        return s
    }

    /**
     * Pre-analysis phase: Inspects raw user narrative script, extracts scene boundaries,
     * strips narration fluff, and builds decoupled visual metadata for each scene segment
     * before passing to the Gemini API.
     */
    fun prepareScriptForGeminiAnalysis(
        rawScript: String,
        topicContext: String,
        style: String,
        publishingStyle: String
    ): RefinementPlan {
        val trimmed = rawScript.trim()
        if (trimmed.isEmpty()) {
            return RefinementPlan(
                cleanedScript = "",
                refinedSceneDrafts = emptyList(),
                visualDirectivesPromptChunk = ""
            )
        }

        // 1. Decouple script narration from scriptwriting cue tags, emojis, and news labels
        val cleanScript = cleanScriptText(trimmed)

        // 2. Break script into sentence segments for strict scene mapping
        val sentenceRegex = Regex("(?<=[?!।|\n])\\s+|(?<=\\.)\\s+(?=[A-Za-z\\u0900-\\u097F]|\\$)")
        val rawSentences = cleanScript.split(sentenceRegex)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("//") }

        val drafts = mutableListOf<RefinedSceneDraft>()
        val cameraAngles = listOf(
            "Close-up focal shot",
            "Dynamic low-angle tracking shot",
            "Wide cinematic establishing shot",
            "Macro detail focus",
            "High-angle overview perspective",
            "Smooth panning camera tracking",
            "Eye-level medium portrait framing"
        )
        val lightingStyles = listOf(
            "Volumetric natural daylight with crisp contrast",
            "Golden hour warm side-lighting",
            "High-key vibrant studio illumination",
            "Dramatic rim lighting with deep cinematic depth of field",
            "Soft diffuse ambient illumination",
            "Moody chiaroscuro lighting with sharp specular highlights"
        )

        val cleanTopic = cleanNewsTopic(topicContext)
        val domainContext = if (cleanTopic.isNotBlank()) cleanTopic else cleanScript.take(60)

        rawSentences.forEachIndexed { index, sentence ->
            val sceneNum = index + 1
            val angle = cameraAngles[index % cameraAngles.size]
            val lighting = lightingStyles[index % lightingStyles.size]

            // Derive concrete stock keywords strictly from narrative tokens using semantic engine
            val semanticAnalysis = ScriptSemanticAnalysisEngine.analyze(sentence, style)
            val primarySearchKeyword = ScriptSemanticAnalysisEngine.refineHighPrecisionSearchQuery(
                segmentText = sentence,
                proposedQuery = "",
                style = style
            )

            val keywordList = mutableListOf<String>()
            if (primarySearchKeyword.isNotBlank()) {
                keywordList.add(primarySearchKeyword)
            }
            semanticAnalysis.refinedKeywords.forEach { kw ->
                if (kw.isNotBlank() && !keywordList.contains(kw) && kw != primarySearchKeyword) {
                    keywordList.add(kw)
                }
            }

            val subject = semanticAnalysis.criticalEntity ?: primarySearchKeyword.ifBlank { "Subject action" }
            val action = if (sentence.length > 50) sentence.take(50).trim() else sentence

            val topicAnchor = if (cleanTopic.isNotBlank()) "Contextually anchored in '$cleanTopic'. " else ""
            val visualPrompt = ScriptSemanticAnalysisEngine.generateRichContextualVisualPrompt(
                narrationText = sentence,
                style = style,
                topicContext = cleanTopic,
                sceneNumber = sceneNum,
                sceneTotal = rawSentences.size
            )

            val meta = SceneVisualMetadata(
                visualKeywords = keywordList,
                lighting = lighting,
                composition = angle,
                action = action,
                subject = subject,
                visualPrompt = visualPrompt
            )

            drafts.add(
                RefinedSceneDraft(
                    sceneNumber = sceneNum,
                    cleanNarration = sentence,
                    visualMetadata = meta
                )
            )
        }

        // 3. Build explicit Visual Directives Prompt Chunk to inject into Gemini prompt
        val visualDirectivesBuilder = StringBuilder()
        visualDirectivesBuilder.appendLine("UNIFIED PROMPT REFINEMENT SPECIFICATION (STRICT SCENE-TO-VISUAL DECOUPLING):")
        visualDirectivesBuilder.appendLine("- RULE 1 (Decouple Spoken Script): Under 'narrationText', write ONLY the spoken voiceover in the target language. Zero scene brackets, zero camera cues, zero section labels (e.g. no 'Hook:', '1.', 'Scene 1:').")
        visualDirectivesBuilder.appendLine("- RULE 2 (Explicit Visual Metadata): For every scene, generate explicit visual metadata fields: 'visualPrompt', 'imageSearchQuery', 'keywords'.")
        visualDirectivesBuilder.appendLine("- RULE 3 (Concrete Keywords & Lighting): Every 'imageSearchQuery' MUST contain 2 to 4 concrete English nouns and action verbs (NO abstract fluff like 'cinematic', 'atmospheric', 'moody', 'scene').")
        visualDirectivesBuilder.appendLine("- RULE 4 (Scene-Specific Narrative Action): Every scene's visualPrompt and imageSearchQuery MUST depict THAT specific scene's immediate visual action, subjects, and setting described in its narrationText, progressing step by step with the narrative. Do NOT repeat generic headline or media channel words across scenes!")
        if (cleanTopic.isNotBlank()) {
            visualDirectivesBuilder.appendLine("- RULE 5 (Core Topic Anchor): Contextually align the visuals with the subject domain '$cleanTopic' without using generic news studio or anchor tags unless explicitly narrated.")
        }

        return RefinementPlan(
            cleanedScript = cleanScript,
            refinedSceneDrafts = drafts,
            visualDirectivesPromptChunk = visualDirectivesBuilder.toString().trim()
        )
    }

    /**
     * Post-analysis phase: Enforces strict scene-to-visual mapping on the raw scenes returned
     * by GeminiService or local fallbacks. Ensures all keywords are sanitized, visual metadata
     * is populated, and every scene has concrete stock search terms.
     */
    fun enforceSceneVisualMapping(
        scenes: List<Scene>,
        style: String,
        topicContext: String = ""
    ): List<Scene> {
        if (scenes.isEmpty()) return emptyList()

        val cleanTopic = cleanNewsTopic(topicContext)

        val bannedNewsFluff = setOf(
            "ndtv", "aaj tak", "abp news", "bbc", "news18", "zee news", "republic",
            "breaking news broadcast", "news studio television broadcast", "news anchor",
            "courtroom gavel", "police officer", "news headline", "viral news"
        )

        return scenes.mapIndexed { index, scene ->
            val narration = scene.narrationText.ifEmpty { scene.subtitle }
            val existingPrompt = scene.visualPrompt

            // Sanitize narrative text from any accidental cue tags or ellipses
            val cleanNarr = cleanScriptText(narration)

            // 1. Refine concrete stock search query from narrative tokens
            val initialProposed = scene.keywords.firstOrNull() ?: ""
            var refinedPrimaryQuery = ScriptSemanticAnalysisEngine.refineHighPrecisionSearchQuery(
                segmentText = cleanNarr.ifEmpty { existingPrompt },
                proposedQuery = initialProposed,
                style = style
            )

            // Discard contaminated or generic news channel tags if narrative doesn't actually describe them
            val narrLower = cleanNarr.lowercase()
            if (refinedPrimaryQuery.lowercase() in bannedNewsFluff ||
                bannedNewsFluff.any { refinedPrimaryQuery.lowercase().contains(it) }
            ) {
                val hasActualAnchor = narrLower.contains("studio") || narrLower.contains("anchor") || narrLower.contains("रिपोर्टर")
                val hasActualCourt = narrLower.contains("court") || narrLower.contains("judge") || narrLower.contains("अदालत") || narrLower.contains("कोर्ट")
                val hasActualPolice = narrLower.contains("police") || narrLower.contains("पुलिस")
                if (!hasActualAnchor && !hasActualCourt && !hasActualPolice) {
                    refinedPrimaryQuery = ScriptSemanticAnalysisEngine.refineHighPrecisionSearchQuery(
                        segmentText = cleanNarr.ifEmpty { existingPrompt },
                        proposedQuery = "",
                        style = style
                    )
                }
            }

            // 2. Refine all secondary keywords to eliminate banned fluff
            val cleanKeywords = mutableListOf<String>()
            if (refinedPrimaryQuery.isNotEmpty()) {
                cleanKeywords.add(refinedPrimaryQuery)
            }

            scene.keywords.forEach { rawKw ->
                var refined = ScriptSemanticAnalysisEngine.refineHighPrecisionSearchQuery(
                    segmentText = cleanNarr.ifEmpty { existingPrompt },
                    proposedQuery = rawKw,
                    style = style
                )
                if (bannedNewsFluff.any { refined.lowercase().contains(it) }) {
                    refined = ""
                }
                if (refined.isNotEmpty() && !cleanKeywords.contains(refined)) {
                    cleanKeywords.add(refined)
                }
            }

            if (cleanKeywords.isEmpty()) {
                val derivedTokens = ScriptSemanticAnalysisEngine.refineKeywords(cleanNarr)
                if (derivedTokens.isNotEmpty()) {
                    cleanKeywords.addAll(derivedTokens.take(3))
                } else if (refinedPrimaryQuery.isNotBlank() && refinedPrimaryQuery !in bannedNewsFluff) {
                    cleanKeywords.add(refinedPrimaryQuery)
                } else {
                    cleanKeywords.add("narrative visual scene")
                }
            }

            // 3. Guarantee high-quality voiceover-matched visual prompt in fluent English
            val isBrokenOrGenericPrompt = existingPrompt.isBlank() ||
                existingPrompt.length < 25 ||
                existingPrompt.any { it in '\u0900'..'\u097F' } ||
                existingPrompt.equals(cleanNarr, ignoreCase = true) ||
                existingPrompt.lowercase().contains("vibrant, crime") ||
                existingPrompt.lowercase().contains("crime, vibrant") ||
                existingPrompt.lowercase().trim() in setOf("crime", "vibrant", "police officer", "courtroom gavel", "vibrant scenic visual", "cinematic narrative scene", "news studio television broadcast") ||
                existingPrompt.split("\\s+".toRegex()).size <= 3

            val targetVisualPrompt = if (isBrokenOrGenericPrompt) {
                ScriptSemanticAnalysisEngine.generateRichContextualVisualPrompt(
                    narrationText = cleanNarr,
                    style = style,
                    topicContext = cleanTopic,
                    sceneNumber = index + 1,
                    sceneTotal = scenes.size
                )
            } else {
                existingPrompt
            }

            // 4. Extract or synthesize lighting & camera composition
            val promptLower = targetVisualPrompt.lowercase()
            val detectedLighting = when {
                promptLower.contains("golden hour") -> "Golden hour warm illumination"
                promptLower.contains("studio lighting") || promptLower.contains("studio") -> "High-key studio lighting with soft shadows"
                promptLower.contains("volumetric") || promptLower.contains("sunlight") -> "Volumetric natural sunlight"
                promptLower.contains("rim light") || promptLower.contains("dramatic") -> "Dramatic rim lighting with high contrast"
                promptLower.contains("neon") || promptLower.contains("cyberpunk") -> "Vibrant neon specular reflections"
                else -> "Natural cinematic lighting with crisp depth of field"
            }

            val detectedComposition = when {
                promptLower.contains("close-up") || promptLower.contains("macro") -> "Close-up macro detail"
                promptLower.contains("wide") || promptLower.contains("establishing") || promptLower.contains("aerial") -> "Wide cinematic establishing shot"
                promptLower.contains("low angle") || promptLower.contains("low-angle") -> "Dynamic low-angle perspective"
                promptLower.contains("tracking") || promptLower.contains("panning") -> "Smooth tracking motion framing"
                else -> "Centered subject medium framing"
            }

            // 5. Build unified SceneVisualMetadata
            val visualMeta = SceneVisualMetadata(
                visualKeywords = cleanKeywords,
                lighting = detectedLighting,
                composition = detectedComposition,
                action = cleanNarr.take(60),
                subject = cleanKeywords.firstOrNull() ?: cleanTopic.ifBlank { "Subject" },
                visualPrompt = targetVisualPrompt
            )

            // Update scene properties with strictly decoupled narrative and refined metadata
            scene.copy(
                narrationText = cleanNarr.ifEmpty { scene.narrationText },
                visualPrompt = targetVisualPrompt,
                subtitle = scene.subtitle.replace(Regex("\\.{2,}"), ".").trim()
            ).apply {
                keywords = cleanKeywords
                visualMetadata = visualMeta
                mediaType = scene.mediaType
                mediaPath = scene.mediaPath
                remoteUrl = scene.remoteUrl
                durationMs = scene.durationMs
            }
        }
    }
}
