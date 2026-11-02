package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdjustmentItem
import com.example.data.model.AiAnalysisReport
import com.example.data.model.AiRetouchSettings
import com.example.data.model.AspectRatioOption
import com.example.data.model.CropState
import com.example.data.model.EditorTab
import com.example.data.model.IPhoneFilter
import com.example.data.model.PhotoAdjustments
import com.example.data.model.UpscaleTarget
import com.example.engine.GeminiAiService
import com.example.engine.ImageProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorSnapshot(
    val adjustments: PhotoAdjustments,
    val filter: IPhoneFilter,
    val aiRetouch: AiRetouchSettings,
    val cropState: CropState
)

class PhotoEditorViewModel : ViewModel() {

    private val _originalSourceBitmap = MutableStateFlow<Bitmap?>(null)

    private val _previewOriginalBitmap = MutableStateFlow<Bitmap?>(null)
    val previewOriginalBitmap: StateFlow<Bitmap?> = _previewOriginalBitmap.asStateFlow()

    private val _previewEditedBitmap = MutableStateFlow<Bitmap?>(null)
    val previewEditedBitmap: StateFlow<Bitmap?> = _previewEditedBitmap.asStateFlow()

    private val _adjustments = MutableStateFlow(PhotoAdjustments())
    val adjustments: StateFlow<PhotoAdjustments> = _adjustments.asStateFlow()

    private val _selectedFilter = MutableStateFlow(IPhoneFilter.ORIGINAL)
    val selectedFilter: StateFlow<IPhoneFilter> = _selectedFilter.asStateFlow()

    private val _aiRetouch = MutableStateFlow(AiRetouchSettings())
    val aiRetouch: StateFlow<AiRetouchSettings> = _aiRetouch.asStateFlow()

    private val _cropState = MutableStateFlow(CropState())
    val cropState: StateFlow<CropState> = _cropState.asStateFlow()

    private val _selectedAdjustment = MutableStateFlow(AdjustmentItem.EXPOSURE)
    val selectedAdjustment: StateFlow<AdjustmentItem> = _selectedAdjustment.asStateFlow()

    private val _currentTab = MutableStateFlow(EditorTab.ADJUST)
    val currentTab: StateFlow<EditorTab> = _currentTab.asStateFlow()

    private val _isSplitActive = MutableStateFlow(false)
    val isSplitActive: StateFlow<Boolean> = _isSplitActive.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isProcessing4k = MutableStateFlow(false)
    val isProcessing4k: StateFlow<Boolean> = _isProcessing4k.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _savedExportUri = MutableStateFlow<Uri?>(null)
    val savedExportUri: StateFlow<Uri?> = _savedExportUri.asStateFlow()

    private val _aiReport = MutableStateFlow<AiAnalysisReport?>(null)
    val aiReport: StateFlow<AiAnalysisReport?> = _aiReport.asStateFlow()

    private val _isMlEnhancing = MutableStateFlow(false)
    val isMlEnhancing: StateFlow<Boolean> = _isMlEnhancing.asStateFlow()

    private val _mlEnhancementResult = MutableStateFlow<com.example.engine.MlEnhancementResult?>(null)
    val mlEnhancementResult: StateFlow<com.example.engine.MlEnhancementResult?> = _mlEnhancementResult.asStateFlow()

    private val _superResSpecs = MutableStateFlow<com.example.engine.SuperResolutionSpecs?>(null)
    val superResSpecs: StateFlow<com.example.engine.SuperResolutionSpecs?> = _superResSpecs.asStateFlow()

    // Undo / Redo history management system
    private val undoRedoManager = com.example.engine.UndoRedoManager(maxCapacity = 50)

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _undoCount = MutableStateFlow(0)
    val undoCount: StateFlow<Int> = _undoCount.asStateFlow()

    private val _redoCount = MutableStateFlow(0)
    val redoCount: StateFlow<Int> = _redoCount.asStateFlow()

    private val _historyMessage = MutableStateFlow<String?>(null)
    val historyMessage: StateFlow<String?> = _historyMessage.asStateFlow()

    private var renderJob: Job? = null

    fun selectTab(tab: EditorTab) {
        _currentTab.value = tab
    }

    fun selectAdjustmentItem(item: AdjustmentItem) {
        _selectedAdjustment.value = item
    }

    fun toggleSplitComparison() {
        _isSplitActive.value = !_isSplitActive.value
    }

    fun loadBitmap(bitmap: Bitmap) {
        _originalSourceBitmap.value = bitmap
        val preview = ImageProcessor.createPreviewBitmap(bitmap, maxDimension = 1200)
        _previewOriginalBitmap.value = preview
        resetAll(keepPhoto = true)
        triggerRender()
    }

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    val decoded = BitmapFactory.decodeStream(stream, null, options)
                    if (decoded != null) {
                        loadBitmap(decoded)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSampleResource(context: Context, resId: Int) {
        viewModelScope.launch {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                if (bitmap != null) {
                    loadBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateAdjustment(item: AdjustmentItem, value: Float) {
        val formattedVal = if (value > 0) "+${value.toInt()}" else "${value.toInt()}"
        pushUndoSnapshot("${item.title} $formattedVal", category = item.name)
        val current = _adjustments.value
        val updated = when (item) {
            AdjustmentItem.EXPOSURE -> current.copy(exposure = value)
            AdjustmentItem.BRILLIANCE -> current.copy(brilliance = value)
            AdjustmentItem.HIGHLIGHTS -> current.copy(highlights = value)
            AdjustmentItem.SHADOWS -> current.copy(shadows = value)
            AdjustmentItem.CONTRAST -> current.copy(contrast = value)
            AdjustmentItem.BRIGHTNESS -> current.copy(brightness = value)
            AdjustmentItem.BLACK_POINT -> current.copy(blackPoint = value)
            AdjustmentItem.SATURATION -> current.copy(saturation = value)
            AdjustmentItem.VIBRANCE -> current.copy(vibrance = value)
            AdjustmentItem.WARMTH -> current.copy(warmth = value)
            AdjustmentItem.TINT -> current.copy(tint = value)
            AdjustmentItem.SHARPNESS -> current.copy(sharpness = value)
            AdjustmentItem.DEFINITION -> current.copy(definition = value)
            AdjustmentItem.NOISE_REDUCTION -> current.copy(noiseReduction = value)
            AdjustmentItem.VIGNETTE -> current.copy(vignette = value)
        }
        _adjustments.value = updated
        triggerRender()
    }

    fun updateFilter(filter: IPhoneFilter) {
        pushUndoSnapshot("Filter: ${filter.displayName}")
        _selectedFilter.value = filter
        triggerRender()
    }

    fun updateAiRetouch(settings: AiRetouchSettings) {
        pushUndoSnapshot("AI Retouch", category = "AI_RETOUCH")
        _aiRetouch.value = settings
        triggerRender()
    }

    fun rotatePhoto() {
        pushUndoSnapshot("Rotate 90°")
        val nextDeg = (_cropState.value.rotationDegrees + 90) % 360
        _cropState.value = _cropState.value.copy(rotationDegrees = nextDeg)
        triggerRender()
    }

    fun flipPhoto() {
        pushUndoSnapshot("Flip Horizontal")
        _cropState.value = _cropState.value.copy(flipHorizontal = !_cropState.value.flipHorizontal)
        triggerRender()
    }

    fun updateAspectRatio(ratio: AspectRatioOption) {
        pushUndoSnapshot("Crop ${ratio.label}")
        _cropState.value = _cropState.value.copy(aspectRatio = ratio)
        triggerRender()
    }

    /**
     * One-tap iPhone Smart Auto-Enhance magic button.
     */
    fun autoMagicEnhance() {
        val base = _previewOriginalBitmap.value ?: return
        pushUndoSnapshot("Auto Enhance")
        viewModelScope.launch {
            val report = GeminiAiService.analyzeImage(base)
            _adjustments.value = report.autoAdjustments
            _selectedFilter.value = report.suggestedFilter
            _aiRetouch.value = _aiRetouch.value.copy(hdrBoost = 25f, faceGlow = 15f)
            triggerRender()
        }
    }

    fun analyzeWithAi() {
        val base = _previewOriginalBitmap.value ?: return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val report = GeminiAiService.analyzeImage(base)
                _aiReport.value = report
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun applyAiReport(report: AiAnalysisReport) {
        pushUndoSnapshot("AI Grading")
        _adjustments.value = report.autoAdjustments
        _selectedFilter.value = report.suggestedFilter
        _aiRetouch.value = _aiRetouch.value.copy(hdrBoost = 30f, faceSmoothing = 20f, faceGlow = 15f)
        triggerRender()
    }

    fun applyAiPrompt(prompt: String) {
        val base = _previewOriginalBitmap.value ?: return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val adjustments = GeminiAiService.generatePromptAdjustments(base, prompt)
                pushUndoSnapshot("AI Prompt: $prompt")
                _adjustments.value = adjustments
                if (prompt.contains("portrait", ignoreCase = true) || prompt.contains("beauty", ignoreCase = true)) {
                    _aiRetouch.value = _aiRetouch.value.copy(faceSmoothing = 35f, faceGlow = 25f, eyeClarity = 20f)
                }
                triggerRender()
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun triggerMlFaceEnhancement(smoothingStrength: Float = 60f, sharpnessStrength: Float = 50f) {
        val current = _previewEditedBitmap.value ?: _previewOriginalBitmap.value ?: return
        viewModelScope.launch {
            _isMlEnhancing.value = true
            try {
                val result = com.example.engine.MlFaceEnhancer.enhanceFaces(
                    source = current,
                    smoothingStrength = smoothingStrength,
                    detailSharpnessStrength = sharpnessStrength
                )
                pushUndoSnapshot("ML Face Beautify")
                _mlEnhancementResult.value = result
                _previewEditedBitmap.value = result.enhancedBitmap
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isMlEnhancing.value = false
            }
        }
    }

    fun triggerUpscale(
        target: UpscaleTarget,
        noiseReduction: Float = 45f,
        detailEnhance: Float = 55f
    ) {
        val current = _previewEditedBitmap.value ?: _previewOriginalBitmap.value ?: return
        viewModelScope.launch {
            _isProcessing4k.value = true
            try {
                val targetDim = if (target == UpscaleTarget.UHD_4K) 3840 else 1920
                val result = com.example.engine.SuperResolutionEngine.scaleTo4k(
                    source = current,
                    targetDimension = targetDim,
                    noiseReductionStrength = noiseReduction,
                    detailEnhanceStrength = detailEnhance
                )
                pushUndoSnapshot("4K Super-Res (${target.label})")
                _superResSpecs.value = result.specs
                _previewEditedBitmap.value = result.upscaledBitmap
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessing4k.value = false
            }
        }
    }

    fun exportPhoto(
        context: Context,
        target: UpscaleTarget,
        quality: Int,
        onSuccess: (Uri) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val source = _originalSourceBitmap.value ?: _previewOriginalBitmap.value ?: run {
            onError("No photo loaded")
            return
        }
        viewModelScope.launch {
            _isExporting.value = true
            _savedExportUri.value = null
            try {
                // Render full resolution
                val processedFull = ImageProcessor.processImage(
                    baseBitmap = source,
                    adjustments = _adjustments.value,
                    filter = _selectedFilter.value,
                    aiRetouch = _aiRetouch.value,
                    cropState = _cropState.value
                )

                val upscaledFull = ImageProcessor.upscaleBitmap(processedFull, target)
                val uri = ImageProcessor.saveImageToGallery(context, upscaledFull, quality)
                _savedExportUri.value = uri
                if (uri != null) {
                    onSuccess(uri)
                } else {
                    onError("Failed to save to MediaStore")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Export failed")
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun dismissExport() {
        _savedExportUri.value = null
    }

    private fun triggerRender() {
        val base = _previewOriginalBitmap.value ?: return
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val result = ImageProcessor.processImage(
                baseBitmap = base,
                adjustments = _adjustments.value,
                filter = _selectedFilter.value,
                aiRetouch = _aiRetouch.value,
                cropState = _cropState.value
            )
            _previewEditedBitmap.value = result
        }
    }

    private fun pushUndoSnapshot(actionName: String = "Edit", category: String? = null) {
        val currentSnapshot = EditorSnapshot(
            adjustments = _adjustments.value,
            filter = _selectedFilter.value,
            aiRetouch = _aiRetouch.value,
            cropState = _cropState.value
        )
        undoRedoManager.recordStep(actionName, currentSnapshot, category)
        updateHistoryFlags()
    }

    fun finishContinuousEdit() {
        undoRedoManager.finishContinuousEdit()
    }

    fun undo(): String? {
        val currentSnapshot = EditorSnapshot(
            adjustments = _adjustments.value,
            filter = _selectedFilter.value,
            aiRetouch = _aiRetouch.value,
            cropState = _cropState.value
        )
        val result = undoRedoManager.undo(currentSnapshot) ?: return null
        val (actionName, previous) = result
        _adjustments.value = previous.adjustments
        _selectedFilter.value = previous.filter
        _aiRetouch.value = previous.aiRetouch
        _cropState.value = previous.cropState
        _historyMessage.value = "Undid: $actionName"
        updateHistoryFlags()
        triggerRender()
        return actionName
    }

    fun redo(): String? {
        val currentSnapshot = EditorSnapshot(
            adjustments = _adjustments.value,
            filter = _selectedFilter.value,
            aiRetouch = _aiRetouch.value,
            cropState = _cropState.value
        )
        val result = undoRedoManager.redo(currentSnapshot) ?: return null
        val (actionName, next) = result
        _adjustments.value = next.adjustments
        _selectedFilter.value = next.filter
        _aiRetouch.value = next.aiRetouch
        _cropState.value = next.cropState
        _historyMessage.value = "Redid: $actionName"
        updateHistoryFlags()
        triggerRender()
        return actionName
    }

    fun jumpToHistoryStep(stepId: String) {
        val currentSnapshot = EditorSnapshot(
            adjustments = _adjustments.value,
            filter = _selectedFilter.value,
            aiRetouch = _aiRetouch.value,
            cropState = _cropState.value
        )
        val target = undoRedoManager.jumpToUndoStep(stepId, currentSnapshot) ?: return
        _adjustments.value = target.adjustments
        _selectedFilter.value = target.filter
        _aiRetouch.value = target.aiRetouch
        _cropState.value = target.cropState
        updateHistoryFlags()
        triggerRender()
    }

    private fun updateHistoryFlags() {
        _canUndo.value = undoRedoManager.canUndo
        _canRedo.value = undoRedoManager.canRedo
        _undoCount.value = undoRedoManager.undoCount
        _redoCount.value = undoRedoManager.redoCount
    }

    fun resetAll(keepPhoto: Boolean = true) {
        _adjustments.value = PhotoAdjustments()
        _selectedFilter.value = IPhoneFilter.ORIGINAL
        _aiRetouch.value = AiRetouchSettings()
        _cropState.value = CropState()
        undoRedoManager.clear()
        updateHistoryFlags()
        _historyMessage.value = "Reset all edits"
        if (keepPhoto) {
            triggerRender()
        }
    }
}
