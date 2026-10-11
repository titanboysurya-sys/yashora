package com.ritvyom.yashoraReelgenerator

import android.graphics.Bitmap
import android.graphics.Canvas
import com.ritvyom.yashoraReelgenerator.presentation.utils.Media3TransitionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YouCutTransitionSuiteTest {

    @Test
    fun testTransitionCatalogCompleteness() {
        val transitions = Media3TransitionEngine.TRANSITIONS
        // Verify rich library of transitions
        assertTrue("Should have at least 40 transitions", transitions.size >= 40)

        // Verify categories
        val categories = Media3TransitionEngine.CATEGORIES
        assertTrue(categories.contains("ALL"))
        assertTrue(categories.contains("BASIC"))
        assertTrue(categories.contains("GLITCH"))
        assertTrue(categories.contains("SLIDE"))
        assertTrue(categories.contains("STYLE"))
        assertTrue(categories.contains("SPORT"))

        // Check key YouCut transitions exist
        val ids = transitions.map { it.id }
        assertTrue(ids.contains("None"))
        assertTrue(ids.contains("MIX"))
        assertTrue(ids.contains("FADE B"))
        assertTrue(ids.contains("FADE W"))
        assertTrue(ids.contains("FLASH"))
        assertTrue(ids.contains("RGB SPLIT"))
        assertTrue(ids.contains("SLIDE L"))
        assertTrue(ids.contains("SLIDE R"))
        assertTrue(ids.contains("WIPE L"))
        assertTrue(ids.contains("WIPE R"))
        assertTrue(ids.contains("ZOOM IN"))
        assertTrue(ids.contains("LIGHT LEAK"))
        assertTrue(ids.contains("FILM BURN"))
        assertTrue(ids.contains("SHAKE"))
    }

    @Test
    fun testNormalizeTransitionId() {
        assertEquals("None", Media3TransitionEngine.normalizeTransitionId(null))
        assertEquals("None", Media3TransitionEngine.normalizeTransitionId(""))
        assertEquals("None", Media3TransitionEngine.normalizeTransitionId("None"))
        assertEquals("None", Media3TransitionEngine.normalizeTransitionId("Cut"))

        assertEquals("MIX", Media3TransitionEngine.normalizeTransitionId("MIX"))
        assertEquals("DISSOLVE", Media3TransitionEngine.normalizeTransitionId("Dissolve"))
        assertEquals("MIX", Media3TransitionEngine.normalizeTransitionId("Crossfade"))

        assertEquals("FADE B", Media3TransitionEngine.normalizeTransitionId("FADE B"))
        assertEquals("FADE B", Media3TransitionEngine.normalizeTransitionId("Fade (Black)"))
        assertEquals("FADE B", Media3TransitionEngine.normalizeTransitionId("Fade"))

        assertEquals("FLASH", Media3TransitionEngine.normalizeTransitionId("FLASH"))
        assertEquals("FLASH", Media3TransitionEngine.normalizeTransitionId("Flash White"))

        assertEquals("SLIDE L", Media3TransitionEngine.normalizeTransitionId("SLIDE L"))
        assertEquals("SLIDE L", Media3TransitionEngine.normalizeTransitionId("Slide Left"))

        assertEquals("WIPE R", Media3TransitionEngine.normalizeTransitionId("WIPE R"))
        assertEquals("WIPE R", Media3TransitionEngine.normalizeTransitionId("WIPE RIGHT"))

        assertEquals("RGB SPLIT", Media3TransitionEngine.normalizeTransitionId("RGB SPLIT"))
        assertEquals("RGB SPLIT", Media3TransitionEngine.normalizeTransitionId("Chromatic Glitch"))
    }

    @Test
    fun testDrawTransitionExecutesSafely() {
        val bitmap = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val sampleTransitions = listOf(
            "None", "MIX", "FADE B", "FADE W", "FLASH", "RGB SPLIT",
            "WIPE L", "WIPE R", "WIPE U", "WIPE D", "SLIDE L", "SLIDE R",
            "SPLIT V", "SPLIT H", "ZOOM IN", "ZOOM OUT", "LIGHT LEAK",
            "FILM BURN", "BLUR DISSOLVE", "MOSAIC", "BLINK", "FLOODLIGHT",
            "SHAKE", "SHOCKWAVE", "COLOR FLASH", "CIRCULAR"
        )

        for (trans in sampleTransitions) {
            // Frame 2 of 30 entering
            Media3TransitionEngine.drawTransition(
                canvas = canvas,
                width = 360,
                height = 640,
                frameIndex = 2,
                totalFrames = 60,
                transitionType = trans,
                transitionDurationMs = 700L,
                fps = 30
            )

            // Frame 58 of 60 exiting
            Media3TransitionEngine.drawTransition(
                canvas = canvas,
                width = 360,
                height = 640,
                frameIndex = 58,
                totalFrames = 60,
                transitionType = trans,
                transitionDurationMs = 700L,
                fps = 30
            )
        }

        assertNotNull(bitmap)
    }

    @Test
    fun testCreateMedia3TransitionEffects() {
        val emptyEffects = Media3TransitionEngine.createMedia3TransitionEffects("None", 3000L, 700L)
        assertTrue("None should have 0 effects", emptyEffects.isEmpty())

        val fadeEffects = Media3TransitionEngine.createMedia3TransitionEffects("FADE B", 3000L, 700L)
        assertFalse("FADE B should produce effects", fadeEffects.isEmpty())

        val slideEffects = Media3TransitionEngine.createMedia3TransitionEffects("SLIDE L", 3000L, 700L)
        assertFalse("SLIDE L should produce effects", slideEffects.isEmpty())

        val zoomEffects = Media3TransitionEngine.createMedia3TransitionEffects("ZOOM IN", 3000L, 700L)
        assertFalse("ZOOM IN should produce effects", zoomEffects.isEmpty())
    }
}
