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

        // 1. Decouple script narration from scriptwriting cue tags [Visual: ...], [Hook: ...], [Narration: ...]
        val cleanScript = trimmed
            .replace(Regex("(?i)\\[(Hook|Intro|Scene\\s*\\d+|Outro|CTA|Visual|Narration|Voiceover|Audio|Music|SFX|Sound|Lighting|Camera)[^\\]]*\\]"), " ")
            .replace(Regex("(?i)^(Hook|Intro|Scene\\s*\\d+|Outro|CTA|Visual|Narration|Voiceover):", RegexOption.MULTILINE), " ")
            .replace(Regex("\\*\\*|__|[*_#]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

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

        val combinedContext = if (topicContext.isNotBlank()) topicContext else cleanScript

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

            val visualPrompt = "A sharp, cinematic $style sequence capturing: '$sentence'. " +
                    "$angle focusing precisely on $primarySearchKeyword. " +
                    "Illuminated with $lighting. " +
                    "Ultra-clean 4K visual composition grounded in '$combinedContext'."

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
        visualDirectivesBuilder.appendLine("- RULE 1 (Decouple Spoken Script): Under 'narrationText', write ONLY the spoken voiceover in the target language. Zero scene brackets, zero camera cues.")
        visualDirectivesBuilder.appendLine("- RULE 2 (Explicit Visual Metadata): For every scene, generate explicit visual metadata fields: 'visualPrompt', 'imageSearchQuery', 'keywords'.")
        visualDirectivesBuilder.appendLine("- RULE 3 (Concrete Keywords & Lighting): Every 'imageSearchQuery' MUST contain 2 to 4 concrete English nouns and action verbs (NO abstract fluff like 'cinematic', 'atmospheric', 'moody', 'scene').")
        visualDirectivesBuilder.appendLine("- RULE 4 (Ground in Topic): All visual keywords must be directly grounded in the topic context: '$combinedContext'.")

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

        return scenes.mapIndexed { index, scene ->
            val narration = scene.narrationText.ifEmpty { scene.subtitle }
            val existingPrompt = scene.visualPrompt

            // Sanitize narrative text from any accidental cue tags or ellipses
            val cleanNarr = narration
                .replace(Regex("(?i)\\[(Hook|Intro|Scene\\s*\\d+|Outro|CTA|Visual|Narration|Voiceover)[^\\]]*\\]"), "")
                .replace(Regex("(?i)^(Hook|Intro|Scene\\s*\\d+|Outro|CTA|Visual|Narration|Voiceover):", RegexOption.MULTILINE), "")
                .replace(Regex("\\.{3,}"), ".")
                .trim()

            // 1. Refine concrete stock search query from narrative tokens
            val initialProposed = scene.keywords.firstOrNull() ?: ""
            val refinedPrimaryQuery = ScriptSemanticAnalysisEngine.refineHighPrecisionSearchQuery(
                segmentText = cleanNarr.ifEmpty { existingPrompt },
                proposedQuery = initialProposed,
                style = style
            )

            // 2. Refine all secondary keywords to eliminate banned fluff
            val cleanKeywords = mutableListOf<String>()
            if (refinedPrimaryQuery.isNotEmpty()) {
                cleanKeywords.add(refinedPrimaryQuery)
            }

            scene.keywords.forEach { rawKw ->
                val refined = ScriptSemanticAnalysisEngine.refineHighPrecisionSearchQuery(
                    segmentText = cleanNarr.ifEmpty { existingPrompt },
                    proposedQuery = rawKw,
                    style = style
                )
                if (refined.isNotEmpty() && !cleanKeywords.contains(refined)) {
                    cleanKeywords.add(refined)
                }
            }

            if (cleanKeywords.isEmpty()) {
                cleanKeywords.add(refinedPrimaryQuery.ifEmpty { "vibrant cinematic visual" })
            }

            // 3. Extract or synthesize lighting & camera composition
            val promptLower = existingPrompt.lowercase()
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

            // 4. Build unified SceneVisualMetadata
            val visualMeta = scene.visualMetadata ?: SceneVisualMetadata(
                visualKeywords = cleanKeywords,
                lighting = detectedLighting,
                composition = detectedComposition,
                action = cleanNarr.take(60),
                subject = cleanKeywords.firstOrNull() ?: topicContext.ifBlank { "Subject" },
                visualPrompt = existingPrompt
            )

            // Update scene properties with strictly decoupled narrative and refined metadata
            scene.copy(
                narrationText = cleanNarr.ifEmpty { scene.narrationText },
                subtitle = scene.subtitle.replace(Regex("\\.{3,}"), ".").trim()
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
