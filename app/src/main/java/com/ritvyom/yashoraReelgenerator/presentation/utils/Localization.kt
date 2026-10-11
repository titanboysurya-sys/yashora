package com.ritvyom.yashoraReelgenerator.presentation.utils

/**
 * Universal Studio Interface Engine.
 * Configured for crisp, standardized English UI across all editor workspaces.
 */
object Localization {
    /**
     * Normalizes language keys safely.
     */
    fun normalizeLanguageKey(language: String): String {
        return language.trim().uppercase()
    }

    /**
     * Translates the given English text to the requested target language.
     * With app interface locked to English standard, returns the English text directly.
     */
    fun translate(englishText: String, targetLanguage: String = "ENGLISH"): String {
        return englishText
    }
}

/**
 * Extension function on String to translate/localize UI texts inline.
 * Directly returns the standard English text with zero latency or dictionary lookup overhead.
 */
fun String.localize(languageState: String = "ENGLISH"): String {
    return this
}
