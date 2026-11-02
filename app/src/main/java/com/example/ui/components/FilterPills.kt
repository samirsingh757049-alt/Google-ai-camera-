package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiRetouchSettings
import com.example.data.model.AspectRatioOption
import com.example.data.model.CropState
import com.example.data.model.IPhoneFilter
import com.example.data.model.UpscaleTarget
import com.example.ui.theme.IPhoneBlack
import com.example.ui.theme.IPhoneCardDark
import com.example.ui.theme.IPhoneCardElevated
import com.example.ui.theme.IPhoneControlFill
import com.example.ui.theme.IPhoneCyan
import com.example.ui.theme.IPhoneTextSecondary
import com.example.ui.theme.IPhoneYellow
import kotlin.math.roundToInt

@Composable
fun FilterSelector(
    selectedFilter: IPhoneFilter,
    onSelectFilter: (IPhoneFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IPhoneBlack)
            .padding(vertical = 12.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(IPhoneFilter.values()) { filter ->
                val isSelected = filter == selectedFilter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectFilter(filter) }
                        .testTag("filter_${filter.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                when (filter) {
                                    IPhoneFilter.ORIGINAL -> Color(0xFF4A4A4C)
                                    IPhoneFilter.VIVID -> Color(0xFFE63946)
                                    IPhoneFilter.VIVID_WARM -> Color(0xFFF77F00)
                                    IPhoneFilter.VIVID_COOL -> Color(0xFF0077B6)
                                    IPhoneFilter.DRAMATIC -> Color(0xFF2B2D42)
                                    IPhoneFilter.DRAMATIC_WARM -> Color(0xFF6B4226)
                                    IPhoneFilter.DRAMATIC_COOL -> Color(0xFF1D2D44)
                                    IPhoneFilter.MONO -> Color(0xFF8D99AE)
                                    IPhoneFilter.SILVERTONE -> Color(0xFFCED4DA)
                                    IPhoneFilter.NOIR -> Color(0xFF1B1B1E)
                                    IPhoneFilter.IPHONE_GOLD -> Color(0xFFD4AF37)
                                    IPhoneFilter.DEEP_INDIGO -> Color(0xFF3F37C9)
                                }
                            )
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) IPhoneYellow else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.displayName.take(2).uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = filter.displayName,
                        color = if (isSelected) IPhoneYellow else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun AiFaceRetouchControls(
    settings: AiRetouchSettings,
    onSettingsChange: (AiRetouchSettings) -> Unit,
    isMlEnhancing: Boolean = false,
    mlResult: com.example.engine.MlEnhancementResult? = null,
    onTriggerMlEnhance: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var mlSmoothStrength by remember { mutableFloatStateOf(65f) }
    var mlSharpStrength by remember { mutableFloatStateOf(50f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IPhoneBlack)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ML Kit Auto Face Enhancement Hero Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = IPhoneCardElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, IPhoneCyan.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = IPhoneYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ML KIT ON-DEVICE MODEL",
                            color = IPhoneYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Button(
                        onClick = { onTriggerMlEnhance?.invoke(mlSmoothStrength, mlSharpStrength) },
                        enabled = !isMlEnhancing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IPhoneYellow,
                            contentColor = IPhoneBlack
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("ml_enhance_btn")
                    ) {
                        if (isMlEnhancing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = IPhoneBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("ENHANCE FACE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Detects facial contours to smooth skin blemishes while sharpening eyes & lips.",
                    color = IPhoneTextSecondary,
                    fontSize = 10.sp
                )

                if (mlResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IPhoneControlFill,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = mlResult.description,
                                color = IPhoneCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (mlResult.metrics.isNotEmpty()) {
                                val first = mlResult.metrics[0]
                                val smile = first.smileProbability?.let { " • Smile: ${(it * 100).roundToInt()}%" } ?: ""
                                val eye = first.leftEyeOpenProbability?.let { " • Eyes: ${(it * 100).roundToInt()}%" } ?: ""
                                Text(
                                    text = "Face ${first.faceIndex}$smile$eye",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manual Tuning Sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANUAL FACIAL ADJUSTMENTS",
                color = IPhoneTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (!settings.isDefault) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IPhoneControlFill,
                    modifier = Modifier.clickable { onSettingsChange(AiRetouchSettings()) }
                ) {
                    Text(
                        text = "RESET",
                        color = IPhoneYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Skin Smoothing
        FaceSliderRow(
            title = "Skin Smoothing",
            value = settings.faceSmoothing,
            onValueChange = { onSettingsChange(settings.copy(faceSmoothing = it)) }
        )

        // Face Glow
        FaceSliderRow(
            title = "Radiant Face Glow",
            value = settings.faceGlow,
            onValueChange = { onSettingsChange(settings.copy(faceGlow = it)) }
        )

        // Eye Clarity
        FaceSliderRow(
            title = "Eye & Hair Clarity",
            value = settings.eyeClarity,
            onValueChange = { onSettingsChange(settings.copy(eyeClarity = it)) }
        )

        // Smart HDR Boost
        FaceSliderRow(
            title = "Smart HDR Range",
            value = settings.hdrBoost,
            onValueChange = { onSettingsChange(settings.copy(hdrBoost = it)) }
        )
    }
}

@Composable
private fun FaceSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (value > 0) IPhoneCyan else IPhoneTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(120.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = IPhoneCyan,
                activeTrackColor = IPhoneCyan,
                inactiveTrackColor = IPhoneCardElevated
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${value.roundToInt()}%",
            color = if (value > 0) IPhoneCyan else Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
fun UpscaleSelector(
    selectedTarget: UpscaleTarget,
    isProcessing: Boolean,
    superResSpecs: com.example.engine.SuperResolutionSpecs? = null,
    onSelectTarget: (UpscaleTarget) -> Unit,
    onTriggerUpscale: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var noiseReduction by remember { mutableFloatStateOf(45f) }
    var detailEnhance by remember { mutableFloatStateOf(55f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IPhoneBlack)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Target Resolution Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UpscaleTarget.values().forEach { target ->
                val isSelected = target == selectedTarget
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTarget(target) }
                        .testTag("upscale_${target.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) IPhoneControlFill else IPhoneCardDark,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) IPhoneYellow else Color.DarkGray
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = target.label,
                            color = if (isSelected) IPhoneYellow else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = target.resolutionDesc,
                            color = IPhoneTextSecondary,
                            fontSize = 8.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sliders for Noise Reduction & Detail Reconstruction
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Noise Suppression",
                color = IPhoneTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(115.dp)
            )
            Slider(
                value = noiseReduction,
                onValueChange = { noiseReduction = it },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = IPhoneYellow,
                    activeTrackColor = IPhoneYellow,
                    inactiveTrackColor = IPhoneCardElevated
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${noiseReduction.roundToInt()}%",
                color = IPhoneYellow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edge Reconstruction",
                color = IPhoneTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(115.dp)
            )
            Slider(
                value = detailEnhance,
                onValueChange = { detailEnhance = it },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = IPhoneCyan,
                    activeTrackColor = IPhoneCyan,
                    inactiveTrackColor = IPhoneCardElevated
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${detailEnhance.roundToInt()}%",
                color = IPhoneCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )
        }

        if (superResSpecs != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = IPhoneCardElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${superResSpecs.targetWidth} × ${superResSpecs.targetHeight} • 4K UHD",
                            color = IPhoneYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format("%.1f", superResSpecs.megapixels)} MP • ${superResSpecs.noiseReductionLevel}% noise reduced",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        text = "${superResSpecs.processingTimeMs}ms",
                        color = IPhoneTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onTriggerUpscale(noiseReduction, detailEnhance) },
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = IPhoneYellow,
                contentColor = IPhoneBlack
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("apply_upscale_btn")
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = IPhoneBlack,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "INTERPOLATING & DENOISING...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "APPLY ${selectedTarget.label} SUPER-RESOLUTION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CropControls(
    cropState: CropState,
    onRotate: () -> Unit,
    onFlip: () -> Unit,
    onSelectRatio: (AspectRatioOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IPhoneBlack)
            .padding(16.dp)
    ) {
        // Rotate & Flip buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clickable(onClick = onRotate)
                    .testTag("rotate_btn"),
                shape = RoundedCornerShape(12.dp),
                color = IPhoneCardElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Rotate90DegreesCw,
                        contentDescription = "Rotate 90 degrees",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rotate 90°", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Surface(
                modifier = Modifier
                    .clickable(onClick = onFlip)
                    .testTag("flip_btn"),
                shape = RoundedCornerShape(12.dp),
                color = IPhoneCardElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Flip Horizontal",
                        tint = if (cropState.flipHorizontal) IPhoneYellow else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flip H", color = if (cropState.flipHorizontal) IPhoneYellow else Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Aspect Ratio Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AspectRatioOption.values()) { ratio ->
                val isSelected = ratio == cropState.aspectRatio
                Surface(
                    modifier = Modifier.clickable { onSelectRatio(ratio) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) IPhoneYellow else IPhoneCardDark,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) IPhoneYellow else Color.DarkGray
                    )
                ) {
                    Text(
                        text = ratio.label,
                        color = if (isSelected) IPhoneBlack else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
