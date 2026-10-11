package com.ritvyom.yashoraReelgenerator.presentation.screens

import androidx.compose.runtime.Composable
import com.ritvyom.yashoraReelgenerator.presentation.viewmodels.MainViewModel

data class LocalVoicePresetItem(
    val name: String,
    val language: String,
    val gender: String, // "Male", "Female", "Child"
    val description: String
)

val localVoiceCatalog = listOf(
    // Hindi
    LocalVoicePresetItem("Swara", "Hindi", "Female", "Hindi Natural Standard Female"),
    LocalVoicePresetItem("Madhur", "Hindi", "Male", "Hindi Standard Male Voice"),
    LocalVoicePresetItem("Kavya", "Hindi", "Female", "Hindi Soft Storyteller Voice"),
    LocalVoicePresetItem("Manish", "Hindi", "Male", "Hindi Deep Narrator Voice"),
    LocalVoicePresetItem("Aarav", "Hindi", "Male", "Hindi Bold Dynamic Voice"),
    LocalVoicePresetItem("Ananya", "Hindi", "Female", "Hindi Energetic Female Voice"),

    // English
    LocalVoicePresetItem("Jenny", "English", "Female", "US Natural Clear Female"),
    LocalVoicePresetItem("Guy", "English", "Male", "US Standard Clear Male"),
    LocalVoicePresetItem("Brian", "English", "Male", "US Deep Narrator Voice"),
    LocalVoicePresetItem("Steffan", "English", "Male", "US Dynamic Bold Male"),
    LocalVoicePresetItem("Emma", "English", "Female", "US Soft Ambient Female"),
    LocalVoicePresetItem("Michelle", "English", "Female", "US Energetic Female"),
    LocalVoicePresetItem("Aria", "English", "Female", "US Classic Storyteller"),
    LocalVoicePresetItem("Eric", "English", "Child", "US Playful Kid Voice"),
    LocalVoicePresetItem("Ana", "English", "Child", "US Sweet Child Voice"),

    // Bengali
    LocalVoicePresetItem("Tanisha", "Bengali", "Female", "Bengali Natural Female Voice"),
    LocalVoicePresetItem("Bashkar", "Bengali", "Male", "Bengali Standard Male Voice"),

    // Tamil
    LocalVoicePresetItem("Pallavi", "Tamil", "Female", "Tamil Expressive Female Voice"),
    LocalVoicePresetItem("Valluvar", "Tamil", "Male", "Tamil Classical Male Voice"),

    // Telugu
    LocalVoicePresetItem("Shruti", "Telugu", "Female", "Telugu Natural Female Voice"),
    LocalVoicePresetItem("Mohan", "Telugu", "Male", "Telugu Standard Male Voice"),

    // Gujarati
    LocalVoicePresetItem("Dhwani", "Gujarati", "Female", "Gujarati Clear Female Voice"),
    LocalVoicePresetItem("Niranjan", "Gujarati", "Male", "Gujarati Standard Male Voice"),

    // Marathi
    LocalVoicePresetItem("Aarohi", "Marathi", "Female", "Marathi Expressive Female Voice"),
    LocalVoicePresetItem("Manohar", "Marathi", "Male", "Marathi Standard Male Voice"),

    // Punjabi
    LocalVoicePresetItem("Gurpreet", "Punjabi", "Female", "Punjabi Natural Female Voice"),
    LocalVoicePresetItem("Harpreet", "Punjabi", "Male", "Punjabi Standard Male Voice"),

    // Spanish
    LocalVoicePresetItem("Elvira", "Spanish", "Female", "Spanish Natural Female Voice"),
    LocalVoicePresetItem("Alvaro", "Spanish", "Male", "Spanish Standard Male Voice"),

    // French
    LocalVoicePresetItem("Denise", "French", "Female", "French Natural Female Voice"),
    LocalVoicePresetItem("Henri", "French", "Male", "French Clear Male Voice"),

    // German
    LocalVoicePresetItem("Amala", "German", "Female", "German Expressive Female Voice"),
    LocalVoicePresetItem("Conrad", "German", "Male", "German Deep Male Voice"),

    // Japanese
    LocalVoicePresetItem("Nanami", "Japanese", "Female", "Japanese Soft Female Voice"),
    LocalVoicePresetItem("Keita", "Japanese", "Male", "Japanese Clear Male Voice"),

    // Chinese
    LocalVoicePresetItem("Xiaoxiao", "Chinese", "Female", "Mandarin Natural Female Voice"),
    LocalVoicePresetItem("Yunxi", "Chinese", "Male", "Mandarin Standard Male Voice"),

    // Korean
    LocalVoicePresetItem("SunHi", "Korean", "Female", "Korean Soft Female Voice"),
    LocalVoicePresetItem("InJoon", "Korean", "Male", "Korean Clear Male Voice"),

    // Arabic
    LocalVoicePresetItem("Zariyah", "Arabic", "Female", "Arabic Natural Female Voice"),
    LocalVoicePresetItem("Hamed", "Arabic", "Male", "Arabic Standard Male Voice"),

    // Urdu
    LocalVoicePresetItem("Uzma", "Urdu", "Female", "Urdu Expressive Female Voice"),
    LocalVoicePresetItem("Asad", "Urdu", "Male", "Urdu Standard Male Voice"),

    // Russian
    LocalVoicePresetItem("Svetlana", "Russian", "Female", "Russian Clear Female Voice"),
    LocalVoicePresetItem("Dmitry", "Russian", "Male", "Russian Deep Male Voice"),

    // Portuguese
    LocalVoicePresetItem("Francisca", "Portuguese", "Female", "Portuguese Natural Female Voice"),
    LocalVoicePresetItem("Antonio", "Portuguese", "Male", "Portuguese Clear Male Voice"),

    // Turkish
    LocalVoicePresetItem("Emel", "Turkish", "Female", "Turkish Natural Female Voice"),
    LocalVoicePresetItem("Ahmet", "Turkish", "Male", "Turkish Standard Male Voice"),

    // Indonesian
    LocalVoicePresetItem("Gadis", "Indonesian", "Female", "Indonesian Female Voice"),
    LocalVoicePresetItem("Ardi", "Indonesian", "Male", "Indonesian Male Voice"),

    // Thai
    LocalVoicePresetItem("Achara", "Thai", "Female", "Thai Female Voice"),
    LocalVoicePresetItem("Niwat", "Thai", "Male", "Thai Male Voice"),

    // Vietnamese
    LocalVoicePresetItem("HoaiMy", "Vietnamese", "Female", "Vietnamese Female Voice"),
    LocalVoicePresetItem("NamMinh", "Vietnamese", "Male", "Vietnamese Male Voice")
)

/**
 * Backward compatibility wrapper. Forwards to [VoiceStudioScreen].
 */
@Composable
fun VoxElevenVoiceProviderScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    VoiceStudioScreen(
        viewModel = viewModel,
        onBack = onBack
    )
}
