package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FilterHdr
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

data class PhotoAdjustments(
    val exposure: Float = 0f,       // -100..100
    val brilliance: Float = 0f,     // -100..100 (iPhone HDR Brilliance)
    val highlights: Float = 0f,     // -100..100
    val shadows: Float = 0f,        // -100..100
    val contrast: Float = 0f,       // -100..100
    val brightness: Float = 0f,     // -100..100
    val blackPoint: Float = 0f,     // -100..100
    val saturation: Float = 0f,     // -100..100
    val vibrance: Float = 0f,       // -100..100
    val warmth: Float = 0f,         // -100..100
    val tint: Float = 0f,           // -100..100
    val sharpness: Float = 0f,      // 0..100
    val definition: Float = 0f,     // 0..100
    val noiseReduction: Float = 0f, // 0..100
    val vignette: Float = 0f        // 0..100
) {
    val isDefault: Boolean
        get() = exposure == 0f && brilliance == 0f && highlights == 0f &&
                shadows == 0f && contrast == 0f && brightness == 0f &&
                blackPoint == 0f && saturation == 0f && vibrance == 0f &&
                warmth == 0f && tint == 0f && sharpness == 0f &&
                definition == 0f && noiseReduction == 0f && vignette == 0f
}

enum class AdjustmentItem(
    val title: String,
    val icon: ImageVector,
    val min: Float,
    val max: Float,
    val defaultVal: Float
) {
    EXPOSURE("Exposure", Icons.Default.BrightnessHigh, -100f, 100f, 0f),
    BRILLIANCE("Brilliance", Icons.Default.FilterHdr, -100f, 100f, 0f),
    HIGHLIGHTS("Highlights", Icons.Default.Lightbulb, -100f, 100f, 0f),
    SHADOWS("Shadows", Icons.Default.Brightness4, -100f, 100f, 0f),
    CONTRAST("Contrast", Icons.Default.Contrast, -100f, 100f, 0f),
    BRIGHTNESS("Brightness", Icons.Default.Brightness6, -100f, 100f, 0f),
    BLACK_POINT("Black Point", Icons.Default.Tonality, -100f, 100f, 0f),
    SATURATION("Saturation", Icons.Default.Palette, -100f, 100f, 0f),
    VIBRANCE("Vibrance", Icons.Default.ColorLens, -100f, 100f, 0f),
    WARMTH("Warmth", Icons.Default.WbSunny, -100f, 100f, 0f),
    TINT("Tint", Icons.Default.InvertColors, -100f, 100f, 0f),
    SHARPNESS("Sharpness", Icons.Default.Lens, 0f, 100f, 0f),
    DEFINITION("Definition", Icons.Default.Opacity, 0f, 100f, 0f),
    NOISE_REDUCTION("Noise Red.", Icons.Default.Grain, 0f, 100f, 0f),
    VIGNETTE("Vignette", Icons.Default.ShutterSpeed, 0f, 100f, 0f)
}

enum class IPhoneFilter(val displayName: String, val subtitle: String) {
    ORIGINAL("Original", "Default"),
    VIVID("Vivid", "Rich punchy colors"),
    VIVID_WARM("Vivid Warm", "Golden vibrancy"),
    VIVID_COOL("Vivid Cool", "Crisp ocean blue"),
    DRAMATIC("Dramatic", "High contrast"),
    DRAMATIC_WARM("Dramatic Warm", "Sunset shadows"),
    DRAMATIC_COOL("Dramatic Cool", "Moody cinema"),
    MONO("Mono", "Pure B&W"),
    SILVERTONE("Silvertone", "Classic silver"),
    NOIR("Noir", "Deep black punch"),
    IPHONE_GOLD("Cinematic Gold", "iPhone 16 Pro"),
    DEEP_INDIGO("Deep Indigo", "Editorial tone")
}

data class AiRetouchSettings(
    val faceSmoothing: Float = 0f, // 0..100
    val faceGlow: Float = 0f,      // 0..100
    val eyeClarity: Float = 0f,    // 0..100
    val hdrBoost: Float = 0f       // 0..100
) {
    val isDefault: Boolean
        get() = faceSmoothing == 0f && faceGlow == 0f && eyeClarity == 0f && hdrBoost == 0f
}

enum class UpscaleTarget(val label: String, val resolutionDesc: String, val scaleMultiplier: Float) {
    ORIGINAL("Original", "Native Source", 1.0f),
    FHD_1080P("Full HD", "1080p Ultra Clear", 1.5f),
    UHD_4K("4K UHD", "3840×2160 Super-Res", 2.2f)
}

enum class AspectRatioOption(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    STORY_9_16("9:16", 9f / 16f),
    CINEMA_16_9("16:9", 16f / 9f),
    PHOTO_3_2("3:2", 3f / 2f),
    PHOTO_2_3("2:3", 2f / 3f)
}

data class CropState(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val aspectRatio: AspectRatioOption = AspectRatioOption.FREE
)

enum class EditorTab(val title: String) {
    ADJUST("Adjust"),
    FILTERS("Filters"),
    AI_FACE("AI Face"),
    UPSCALE_4K("4K UHD"),
    ONLINE_AI("Online AI"),
    CROP("Crop & Angle")
}

data class AiAnalysisReport(
    val summary: String,
    val lightingScore: Int,
    val dynamicRangeScore: Int,
    val colorTone: String,
    val suggestedFilter: IPhoneFilter,
    val recommendations: List<String>,
    val autoAdjustments: PhotoAdjustments
)
