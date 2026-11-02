package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EditorTab
import com.example.data.model.UpscaleTarget
import com.example.engine.GeminiAiService
import com.example.ui.components.AdjustmentToolBar
import com.example.ui.components.AiFaceRetouchControls
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.components.CameraCaptureView
import com.example.ui.components.CropControls
import com.example.ui.components.ExportDialog
import com.example.ui.components.FilterSelector
import com.example.ui.components.OnlineAiStudio
import com.example.ui.components.UpscaleSelector
import com.example.ui.theme.IPhoneBlack
import com.example.ui.theme.IPhoneCardDark
import com.example.ui.theme.IPhoneCardElevated
import com.example.ui.theme.IPhoneControlFill
import com.example.ui.theme.IPhoneCyan
import com.example.ui.theme.IPhoneTextSecondary
import com.example.ui.theme.IPhoneYellow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PhotoEditorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PhotoEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainEditorScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEditorScreen(viewModel: PhotoEditorViewModel) {
    val context = LocalContext.current

    // Observe state
    val previewOriginal by viewModel.previewOriginalBitmap.collectAsState()
    val previewEdited by viewModel.previewEditedBitmap.collectAsState()
    val adjustments by viewModel.adjustments.collectAsState()
    val filter by viewModel.selectedFilter.collectAsState()
    val aiRetouch by viewModel.aiRetouch.collectAsState()
    val cropState by viewModel.cropState.collectAsState()
    val selectedAdjustment by viewModel.selectedAdjustment.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isSplitActive by viewModel.isSplitActive.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isProcessing4k by viewModel.isProcessing4k.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val savedExportUri by viewModel.savedExportUri.collectAsState()
    val aiReport by viewModel.aiReport.collectAsState()
    val isMlEnhancing by viewModel.isMlEnhancing.collectAsState()
    val mlResult by viewModel.mlEnhancementResult.collectAsState()
    val superResSpecs by viewModel.superResSpecs.collectAsState()

    var showExportSheet by remember { mutableStateOf(false) }
    var showSampleSheet by remember { mutableStateOf(false) }
    var showCameraView by remember { mutableStateOf(false) }

    // Android Zero-Permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadFromUri(context, uri)
            Toast.makeText(context, "Loaded photo from library", Toast.LENGTH_SHORT).show()
        }
    }

    // Load initial sample photo on first launch
    LaunchedEffect(Unit) {
        if (previewOriginal == null) {
            viewModel.loadSampleResource(context, R.drawable.sample_sunset)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(IPhoneBlack),
        containerColor = IPhoneBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // TOP BAR: Navigation, Undo, Auto-Enhance, Compare, Export
            TopNavigationBar(
                canUndo = canUndo,
                canRedo = canRedo,
                isSplitActive = isSplitActive,
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onOpenCamera = { showCameraView = true },
                onOpenSamples = { showSampleSheet = true },
                onAutoEnhance = {
                    viewModel.autoMagicEnhance()
                    Toast.makeText(context, "✨ iPhone Smart Auto-Enhance applied!", Toast.LENGTH_SHORT).show()
                },
                onToggleSplit = { viewModel.toggleSplitComparison() },
                onUndo = {
                    val action = viewModel.undo()
                    if (action != null) {
                        Toast.makeText(context, "↶ Reverted: $action", Toast.LENGTH_SHORT).show()
                    }
                },
                onRedo = {
                    val action = viewModel.redo()
                    if (action != null) {
                        Toast.makeText(context, "↷ Reapplied: $action", Toast.LENGTH_SHORT).show()
                    }
                },
                onReset = {
                    viewModel.resetAll()
                    Toast.makeText(context, "Reset all edits", Toast.LENGTH_SHORT).show()
                },
                onExport = { showExportSheet = true }
            )

            // MAIN VIEWPORT: Photo with Before/After split comparison
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                BeforeAfterSlider(
                    originalBitmap = previewOriginal,
                    editedBitmap = previewEdited,
                    isSplitActive = isSplitActive,
                    onToggleSplit = { viewModel.toggleSplitComparison() }
                )
            }

            // TOOL CONTROLS (Based on selected tab)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(IPhoneBlack)
            ) {
                when (currentTab) {
                    EditorTab.ADJUST -> {
                        AdjustmentToolBar(
                            adjustments = adjustments,
                            selectedItem = selectedAdjustment,
                            onSelectItem = { viewModel.selectAdjustmentItem(it) },
                            onValueChange = { item, value -> viewModel.updateAdjustment(item, value) }
                        )
                    }
                    EditorTab.FILTERS -> {
                        FilterSelector(
                            selectedFilter = filter,
                            onSelectFilter = { viewModel.updateFilter(it) }
                        )
                    }
                    EditorTab.AI_FACE -> {
                        AiFaceRetouchControls(
                            settings = aiRetouch,
                            onSettingsChange = { viewModel.updateAiRetouch(it) },
                            isMlEnhancing = isMlEnhancing,
                            mlResult = mlResult,
                            onTriggerMlEnhance = { smooth, sharp ->
                                viewModel.triggerMlFaceEnhancement(smooth, sharp)
                                Toast.makeText(context, "ML Kit Face Enhancement applied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    EditorTab.UPSCALE_4K -> {
                        var selectedUpscaleTarget by remember { mutableStateOf(UpscaleTarget.UHD_4K) }
                        UpscaleSelector(
                            selectedTarget = selectedUpscaleTarget,
                            isProcessing = isProcessing4k,
                            superResSpecs = superResSpecs,
                            onSelectTarget = { selectedUpscaleTarget = it },
                            onTriggerUpscale = { noise, detail ->
                                viewModel.triggerUpscale(selectedUpscaleTarget, noise, detail)
                                Toast.makeText(context, "4K Super-Resolution applied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    EditorTab.ONLINE_AI -> {
                        OnlineAiStudio(
                            isAnalyzing = isAnalyzing,
                            report = aiReport,
                            hasOnlineKey = GeminiAiService.hasApiKey(),
                            onAnalyze = { viewModel.analyzeWithAi() },
                            onApplyReport = {
                                viewModel.applyAiReport(it)
                                Toast.makeText(context, "Applied AI grading report!", Toast.LENGTH_SHORT).show()
                            },
                            onApplyPrompt = { prompt ->
                                viewModel.applyAiPrompt(prompt)
                                Toast.makeText(context, "Applied: $prompt", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    EditorTab.CROP -> {
                        CropControls(
                            cropState = cropState,
                            onRotate = { viewModel.rotatePhoto() },
                            onFlip = { viewModel.flipPhoto() },
                            onSelectRatio = { viewModel.updateAspectRatio(it) }
                        )
                    }
                }
            }

            // BOTTOM NAVIGATION TABS (iPhone style)
            IPhoneBottomTabs(
                selectedTab = currentTab,
                onSelectTab = { viewModel.selectTab(it) },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }

    // Export Dialog Sheet
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showExportSheet = false
                viewModel.dismissExport()
            },
            containerColor = IPhoneCardDark,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ExportDialog(
                isExporting = isExporting,
                savedUri = savedExportUri,
                onDismiss = {
                    showExportSheet = false
                    viewModel.dismissExport()
                },
                onSaveToGallery = { target, quality ->
                    viewModel.exportPhoto(
                        context = context,
                        target = target,
                        quality = quality,
                        onSuccess = { uri ->
                            Toast.makeText(
                                context,
                                "✓ Photo successfully saved to Gallery in ${target.label} UHD!\nSaved via MediaStore to Pictures/iPhoneAIPhotoEditor",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, "Failed to save: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onShare = {
                    savedExportUri?.let { uri ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share iPhone Edited Photo"))
                    }
                }
            )
        }
    }

    // Sample Photo Picker Sheet
    if (showSampleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSampleSheet = false },
            containerColor = IPhoneCardDark,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SamplePhotosDrawer(
                onSelectSample = { resId ->
                    viewModel.loadSampleResource(context, resId)
                    showSampleSheet = false
                    Toast.makeText(context, "Loaded sample photo", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showSampleSheet = false }
            )
        }
    }

    // Camera Capture Viewfinder
    if (showCameraView) {
        CameraCaptureView(
            onPhotoCaptured = { bitmap ->
                viewModel.loadBitmap(bitmap)
                showCameraView = false
                Toast.makeText(context, "Photo captured & ready to edit!", Toast.LENGTH_SHORT).show()
            },
            onClose = {
                showCameraView = false
            }
        )
    }
}

@Composable
private fun TopNavigationBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isSplitActive: Boolean,
    onPickPhoto: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenSamples: () -> Unit,
    onAutoEnhance: () -> Unit,
    onToggleSplit: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Photo Library, Camera & Samples
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onPickPhoto,
                modifier = Modifier
                    .size(38.dp)
                    .background(IPhoneCardElevated, CircleShape)
                    .testTag("pick_photo_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Pick photo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onOpenCamera,
                modifier = Modifier
                    .size(38.dp)
                    .background(IPhoneCardElevated, CircleShape)
                    .testTag("open_camera_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Capture photo",
                    tint = IPhoneYellow,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onOpenSamples,
                modifier = Modifier
                    .size(38.dp)
                    .background(IPhoneCardElevated, CircleShape)
                    .testTag("sample_photos_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = "Sample photos",
                    tint = IPhoneCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Center: Magic Auto-Enhance, Compare, Undo/Redo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // One-tap Auto Enhance Magic button
            Surface(
                modifier = Modifier
                    .clickable(onClick = onAutoEnhance)
                    .testTag("auto_enhance_btn"),
                shape = RoundedCornerShape(16.dp),
                color = IPhoneControlFill,
                border = androidx.compose.foundation.BorderStroke(1.dp, IPhoneYellow)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Auto Enhance",
                        tint = IPhoneYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AUTO",
                        color = IPhoneYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Before/After Split comparison toggle
            IconButton(
                onClick = onToggleSplit,
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isSplitActive) IPhoneYellow else IPhoneCardElevated, CircleShape)
                    .testTag("split_compare_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = "Toggle split comparison",
                    tint = if (isSplitActive) IPhoneBlack else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Undo
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Redo
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Right: Revert & Export
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Export / Save to Gallery button
            Surface(
                modifier = Modifier
                    .clickable(onClick = onExport)
                    .testTag("save_to_gallery_top_btn"),
                shape = RoundedCornerShape(18.dp),
                color = IPhoneYellow
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save to Gallery",
                        tint = IPhoneBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SAVE TO GALLERY",
                        color = IPhoneBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun IPhoneBottomTabs(
    selectedTab: EditorTab,
    onSelectTab: (EditorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(IPhoneCardDark)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            Triple(EditorTab.ADJUST, "Adjust", Icons.Default.Tune),
            Triple(EditorTab.FILTERS, "Filters", Icons.Default.Filter),
            Triple(EditorTab.AI_FACE, "AI Face", Icons.Default.Face),
            Triple(EditorTab.UPSCALE_4K, "4K UHD", Icons.Default.HighQuality),
            Triple(EditorTab.ONLINE_AI, "Online AI", Icons.Default.Psychology),
            Triple(EditorTab.CROP, "Crop", Icons.Default.Crop)
        )

        tabs.forEach { (tab, label, icon) ->
            val isSelected = tab == selectedTab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("tab_${tab.name.lowercase()}")
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) IPhoneYellow else IPhoneTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    color = if (isSelected) IPhoneYellow else IPhoneTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SamplePhotosDrawer(
    onSelectSample: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "PRELOADED SAMPLE PHOTOS",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Select high-res test photo for instant iPhone AI editing",
            color = IPhoneTextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SampleCard(
                title = "Golden Sunset",
                desc = "iPhone HDR",
                resId = R.drawable.sample_sunset,
                onSelect = { onSelectSample(R.drawable.sample_sunset) },
                modifier = Modifier.weight(1f)
            )
            SampleCard(
                title = "Studio Portrait",
                desc = "Face Beautify",
                resId = R.drawable.sample_portrait,
                onSelect = { onSelectSample(R.drawable.sample_portrait) },
                modifier = Modifier.weight(1f)
            )
            SampleCard(
                title = "Neon Night",
                desc = "4K Super-Res",
                resId = R.drawable.sample_city,
                onSelect = { onSelectSample(R.drawable.sample_city) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SampleCard(
    title: String,
    desc: String,
    resId: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onSelect)
            .testTag("sample_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        color = IPhoneCardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, IPhoneControlFill)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = IPhoneYellow, fontSize = 10.sp)
        }
    }
}
