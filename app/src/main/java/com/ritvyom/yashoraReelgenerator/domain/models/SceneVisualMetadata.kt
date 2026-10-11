package com.ritvyom.yashoraReelgenerator.domain.models

import java.io.Serializable

/**
 * Explicit Scene Visual Metadata decoupled from the narrative script text.
 * Enforces strict scene-to-visual mapping with concrete stock keywords, lighting,
 * camera composition, core visual action, and mood atmosphere.
 */
data class SceneVisualMetadata(
    val visualKeywords: List<String> = emptyList(),
    val lighting: String = "",
    val composition: String = "",
    val action: String = "",
    val subject: String = "",
    val visualPrompt: String = ""
) : Serializable
