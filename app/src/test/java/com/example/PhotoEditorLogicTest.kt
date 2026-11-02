package com.example

import com.example.data.model.AdjustmentItem
import com.example.data.model.AiRetouchSettings
import com.example.data.model.AspectRatioOption
import com.example.data.model.IPhoneFilter
import com.example.data.model.PhotoAdjustments
import com.example.data.model.UpscaleTarget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoEditorLogicTest {

    @Test
    fun defaultAdjustments_areZero() {
        val adj = PhotoAdjustments()
        assertTrue(adj.isDefault)
        assertEquals(0f, adj.exposure, 0.001f)
        assertEquals(0f, adj.brilliance, 0.001f)
        assertEquals(0f, adj.highlights, 0.001f)
        assertEquals(0f, adj.shadows, 0.001f)
        assertEquals(0f, adj.contrast, 0.001f)
        assertEquals(0f, adj.warmth, 0.001f)
    }

    @Test
    fun modifiedAdjustments_isDefaultFalse() {
        val adj = PhotoAdjustments(exposure = 15f)
        assertFalse(adj.isDefault)
    }

    @Test
    fun defaultAiRetouch_isDefaultTrue() {
        val retouch = AiRetouchSettings()
        assertTrue(retouch.isDefault)
    }

    @Test
    fun modifiedAiRetouch_isDefaultFalse() {
        val retouch = AiRetouchSettings(faceSmoothing = 30f)
        assertFalse(retouch.isDefault)
    }

    @Test
    fun upscaleTargets_haveValidMultipliers() {
        assertEquals(1.0f, UpscaleTarget.ORIGINAL.scaleMultiplier, 0.001f)
        assertTrue(UpscaleTarget.FHD_1080P.scaleMultiplier > 1.0f)
        assertTrue(UpscaleTarget.UHD_4K.scaleMultiplier > 2.0f)
    }

    @Test
    fun aspectRatios_verifyValues() {
        assertEquals(1.0f, AspectRatioOption.SQUARE.ratio ?: 0f, 0.001f)
        assertEquals(0.8f, AspectRatioOption.PORTRAIT_4_5.ratio ?: 0f, 0.001f)
        assertEquals(16f / 9f, AspectRatioOption.CINEMA_16_9.ratio ?: 0f, 0.001f)
    }

    @Test
    fun iphoneFilters_allPresent() {
        assertTrue(IPhoneFilter.values().contains(IPhoneFilter.VIVID))
        assertTrue(IPhoneFilter.values().contains(IPhoneFilter.VIVID_WARM))
        assertTrue(IPhoneFilter.values().contains(IPhoneFilter.DRAMATIC))
        assertTrue(IPhoneFilter.values().contains(IPhoneFilter.NOIR))
        assertTrue(IPhoneFilter.values().contains(IPhoneFilter.IPHONE_GOLD))
    }

    @Test
    fun superResolutionSpecs_calculationCorrect() {
        val specs = com.example.engine.SuperResolutionSpecs(
            originalWidth = 1000,
            originalHeight = 800,
            targetWidth = 3840,
            targetHeight = 3072,
            scaleMultiplier = 3.84f,
            megapixels = 11.79f,
            noiseReductionLevel = 45,
            processingTimeMs = 42L
        )
        assertEquals(3840, specs.targetWidth)
        assertEquals(3072, specs.targetHeight)
        assertEquals(45, specs.noiseReductionLevel)
        assertTrue(specs.scaleMultiplier > 3.0f)
    }

    @Test
    fun undoRedoManager_recordsAndUndoesSteps() {
        val manager = com.example.engine.UndoRedoManager(maxCapacity = 10)
        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)

        val snap0 = com.example.ui.viewmodel.EditorSnapshot(
            adjustments = PhotoAdjustments(),
            filter = IPhoneFilter.ORIGINAL,
            aiRetouch = AiRetouchSettings(),
            cropState = com.example.data.model.CropState()
        )
        val snap1 = snap0.copy(adjustments = PhotoAdjustments(exposure = 20f))
        val snap2 = snap1.copy(filter = IPhoneFilter.VIVID)

        manager.recordStep("Exposure +20", snap0)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)
        assertEquals(1, manager.undoCount)

        manager.recordStep("Filter Vivid", snap1)
        assertEquals(2, manager.undoCount)

        val undone = manager.undo(snap2)
        assertEquals("Filter Vivid", undone?.first)
        assertEquals(snap1, undone?.second)
        assertTrue(manager.canRedo)
        assertEquals(1, manager.undoCount)
        assertEquals(1, manager.redoCount)

        val redone = manager.redo(snap1)
        assertEquals("Filter Vivid", redone?.first)
        assertEquals(snap2, redone?.second)
        assertEquals(2, manager.undoCount)
        assertEquals(0, manager.redoCount)
    }

    @Test
    fun undoRedoManager_coalescesContinuousSliderEdits() {
        val manager = com.example.engine.UndoRedoManager()
        val snap0 = com.example.ui.viewmodel.EditorSnapshot(
            adjustments = PhotoAdjustments(),
            filter = IPhoneFilter.ORIGINAL,
            aiRetouch = AiRetouchSettings(),
            cropState = com.example.data.model.CropState()
        )

        // Simulate continuous dragging of Exposure slider
        manager.recordStep("Exposure +5", snap0.copy(adjustments = PhotoAdjustments(exposure = 5f)), category = "EXPOSURE")
        manager.recordStep("Exposure +10", snap0.copy(adjustments = PhotoAdjustments(exposure = 10f)), category = "EXPOSURE")
        manager.recordStep("Exposure +15", snap0.copy(adjustments = PhotoAdjustments(exposure = 15f)), category = "EXPOSURE")

        // Should have coalesced to 1 history entry
        assertEquals(1, manager.undoCount)
    }
}
