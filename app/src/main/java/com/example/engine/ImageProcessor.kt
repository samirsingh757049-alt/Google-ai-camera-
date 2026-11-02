package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.AiRetouchSettings
import com.example.data.model.AspectRatioOption
import com.example.data.model.CropState
import com.example.data.model.IPhoneFilter
import com.example.data.model.PhotoAdjustments
import com.example.data.model.UpscaleTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageProcessor {

    /**
     * Creates a downsampled preview bitmap for instant real-time editing.
     */
    fun createPreviewBitmap(source: Bitmap, maxDimension: Int = 1200): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) {
            return source.copy(Bitmap.Config.ARGB_8888, true)
        }
        val ratio = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    /**
     * Applies full adjustment pipeline to the bitmap.
     */
    suspend fun processImage(
        baseBitmap: Bitmap,
        adjustments: PhotoAdjustments,
        filter: IPhoneFilter,
        aiRetouch: AiRetouchSettings,
        cropState: CropState
    ): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: Geometry (Rotate / Flip / Crop)
        val transformed = applyCropAndTransform(baseBitmap, cropState)

        // Step 2: ColorMatrix pipeline (Adjustments + Preset Filter)
        val colorMatrix = computeCombinedColorMatrix(adjustments, filter)
        val filteredBitmap = Bitmap.createBitmap(
            transformed.width,
            transformed.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(filteredBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(transformed, 0f, 0f, paint)

        // Step 3: HDR Brilliance & Highlights/Shadows Tone Mapping if active
        val brillianceApplied = if (adjustments.brilliance != 0f || adjustments.highlights != 0f || adjustments.shadows != 0f || aiRetouch.hdrBoost > 0f) {
            applyHdrBrilliance(filteredBitmap, adjustments, aiRetouch.hdrBoost)
        } else {
            filteredBitmap
        }

        // Step 4: AI Face Retouch (Skin smoothing, glow, eye clarity) if active
        val faceRetouched = if (!aiRetouch.isDefault) {
            applyAiFaceRetouch(brillianceApplied, aiRetouch)
        } else {
            brillianceApplied
        }

        // Step 5: Sharpness & Definition convolution if active
        val sharpened = if (adjustments.sharpness > 0f || adjustments.definition > 0f) {
            applySharpnessAndDefinition(faceRetouched, adjustments.sharpness, adjustments.definition)
        } else {
            faceRetouched
        }

        // Step 6: Vignette overlay
        val finalized = if (adjustments.vignette > 0f) {
            applyVignette(sharpened, adjustments.vignette)
        } else {
            sharpened
        }

        finalized
    }

    /**
     * Computes the combined ColorMatrix for Exposure, Contrast, Brightness, Saturation, Warmth, Tint, and Filters.
     */
    private fun computeCombinedColorMatrix(
        adj: PhotoAdjustments,
        filter: IPhoneFilter
    ): ColorMatrix {
        val matrix = ColorMatrix()

        // 1. Exposure & Brightness
        // Exposure shifts gain, brightness shifts offset
        val exposureGain = 1.0f + (adj.exposure / 100f) * 0.7f
        val brightnessOffset = (adj.brightness / 100f) * 60f
        val expMat = ColorMatrix(
            floatArrayOf(
                exposureGain, 0f, 0f, 0f, brightnessOffset,
                0f, exposureGain, 0f, 0f, brightnessOffset,
                0f, 0f, exposureGain, 0f, brightnessOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(expMat)

        // 2. Contrast & Black Point
        val contrastFactor = (1.0f + (adj.contrast / 100f) * 0.6f).coerceAtLeast(0.1f)
        val blackPointShift = (adj.blackPoint / 100f) * -30f
        val contrastOffset = 128f * (1f - contrastFactor) + blackPointShift
        val contrastMat = ColorMatrix(
            floatArrayOf(
                contrastFactor, 0f, 0f, 0f, contrastOffset,
                0f, contrastFactor, 0f, 0f, contrastOffset,
                0f, 0f, contrastFactor, 0f, contrastOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(contrastMat)

        // 3. Saturation & Vibrance
        val effectiveSat = (1.0f + (adj.saturation / 100f) * 0.8f + (adj.vibrance / 100f) * 0.4f).coerceAtLeast(0f)
        val satMat = ColorMatrix().apply { setSaturation(effectiveSat) }
        matrix.postConcat(satMat)

        // 4. Warmth (Color Temperature: Amber vs Blue)
        val warmth = adj.warmth / 100f
        val rWarmth = 1f + (warmth * 0.22f)
        val bWarmth = 1f - (warmth * 0.22f)
        val warmthMat = ColorMatrix(
            floatArrayOf(
                rWarmth, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, bWarmth, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(warmthMat)

        // 5. Tint (Magenta vs Green)
        val tint = adj.tint / 100f
        val gTint = 1f - (tint * 0.15f)
        val rTint = 1f + (tint * 0.08f)
        val bTint = 1f + (tint * 0.08f)
        val tintMat = ColorMatrix(
            floatArrayOf(
                rTint, 0f, 0f, 0f, 0f,
                0f, gTint, 0f, 0f, 0f,
                0f, 0f, bTint, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(tintMat)

        // 6. iPhone Filter Profile
        val filterMat = getIPhoneFilterMatrix(filter)
        matrix.postConcat(filterMat)

        return matrix
    }

    /**
     * iPhone Camera Preset Color grading matrices.
     */
    private fun getIPhoneFilterMatrix(filter: IPhoneFilter): ColorMatrix {
        val mat = ColorMatrix()
        when (filter) {
            IPhoneFilter.ORIGINAL -> {
                // Identity
            }
            IPhoneFilter.VIVID -> {
                // Punchy saturation and contrast
                mat.setSaturation(1.35f)
                val c = 1.12f
                val o = 128f * (1f - c)
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, o + 4f,
                    0f, c, 0f, 0f, o + 2f,
                    0f, 0f, c, 0f, o - 2f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.VIVID_WARM -> {
                mat.setSaturation(1.3f)
                val c = 1.1f
                val o = 128f * (1f - c)
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c * 1.15f, 0f, 0f, 0f, o + 10f,
                    0f, c * 1.05f, 0f, 0f, o + 4f,
                    0f, 0f, c * 0.88f, 0f, o - 8f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.VIVID_COOL -> {
                mat.setSaturation(1.25f)
                val c = 1.1f
                val o = 128f * (1f - c)
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c * 0.92f, 0f, 0f, 0f, o - 4f,
                    0f, c * 1.02f, 0f, 0f, o + 2f,
                    0f, 0f, c * 1.18f, 0f, o + 12f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.DRAMATIC -> {
                val c = 1.32f
                val o = 128f * (1f - c)
                mat.setSaturation(0.85f)
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, o - 6f,
                    0f, c, 0f, 0f, o - 6f,
                    0f, 0f, c, 0f, o - 6f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.DRAMATIC_WARM -> {
                val c = 1.25f
                val o = 128f * (1f - c)
                mat.setSaturation(0.88f)
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c * 1.16f, 0f, 0f, 0f, o + 8f,
                    0f, c * 1.04f, 0f, 0f, o + 2f,
                    0f, 0f, c * 0.85f, 0f, o - 14f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.DRAMATIC_COOL -> {
                val c = 1.28f
                val o = 128f * (1f - c)
                mat.setSaturation(0.82f)
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c * 0.88f, 0f, 0f, 0f, o - 8f,
                    0f, c * 1.0f, 0f, 0f, o - 2f,
                    0f, 0f, c * 1.22f, 0f, o + 14f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.MONO -> {
                mat.setSaturation(0f)
                val c = 1.15f
                val o = 128f * (1f - c)
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, o,
                    0f, c, 0f, 0f, o,
                    0f, 0f, c, 0f, o,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.SILVERTONE -> {
                mat.setSaturation(0f)
                val c = 1.08f
                val o = 128f * (1f - c) + 16f
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, o,
                    0f, c, 0f, 0f, o,
                    0f, 0f, c, 0f, o,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.NOIR -> {
                mat.setSaturation(0f)
                val c = 1.45f
                val o = 128f * (1f - c) - 18f
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, o,
                    0f, c, 0f, 0f, o,
                    0f, 0f, c, 0f, o,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.IPHONE_GOLD -> {
                // iPhone 16 Pro Cinematic Gold style
                val c = 1.14f
                val o = 128f * (1f - c)
                mat.setSaturation(1.15f)
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c * 1.22f, 0f, 0f, 0f, o + 12f,
                    0f, c * 1.10f, 0f, 0f, o + 6f,
                    0f, 0f, c * 0.82f, 0f, o - 10f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            IPhoneFilter.DEEP_INDIGO -> {
                // Editorial deep contrast with indigo teal undertones
                val c = 1.20f
                val o = 128f * (1f - c)
                mat.setSaturation(0.95f)
                mat.postConcat(ColorMatrix(floatArrayOf(
                    c * 0.94f, 0f, 0f, 0f, o - 5f,
                    0f, c * 1.06f, 0f, 0f, o + 2f,
                    0f, 0f, c * 1.24f, 0f, o + 16f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
        }
        return mat
    }

    /**
     * iPhone Smart HDR Brilliance, Shadow lift & Highlight recovery.
     */
    private fun applyHdrBrilliance(
        source: Bitmap,
        adj: PhotoAdjustments,
        hdrBoost: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val brilliance = adj.brilliance / 100f
        val highlights = adj.highlights / 100f
        val shadows = adj.shadows / 100f
        val hdr = hdrBoost / 100f

        // Precompute lookup curves for pixel luminance
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xff
            var r = (pixel shr 16) and 0xff
            var g = (pixel shr 8) and 0xff
            var b = pixel and 0xff

            // Relative luminance
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            // Shadow lift: affects darker regions (lum < 0.5)
            val shadowWeight = (1f - lum).coerceIn(0f, 1f) * (1f - lum)
            val shadowDelta = (shadows * 0.4f + brilliance * 0.35f + hdr * 0.25f) * shadowWeight * 255f

            // Highlight recovery / compression: affects bright regions (lum > 0.5)
            val highlightWeight = lum * lum
            val highlightDelta = (highlights * 0.35f - brilliance * 0.15f + hdr * 0.15f) * highlightWeight * 255f

            r = (r + shadowDelta + highlightDelta).roundToInt().coerceIn(0, 255)
            g = (g + shadowDelta + highlightDelta).roundToInt().coerceIn(0, 255)
            b = (b + shadowDelta + highlightDelta).roundToInt().coerceIn(0, 255)

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Natural AI Face Beautify & Retouch:
     * Softens skin texture and reduces blemishes without plastic blurring.
     * Preserves eye contours, lips, and hair edges while boosting radiant skin glow.
     */
    private fun applyAiFaceRetouch(
        source: Bitmap,
        retouch: AiRetouchSettings
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val smoothFactor = retouch.faceSmoothing / 100f
        val glowFactor = retouch.faceGlow / 100f
        val eyeClarity = retouch.eyeClarity / 100f

        // Sample horizontal & vertical neighbours for adaptive skin smoothing
        val smoothed = IntArray(width * height)
        System.arraycopy(pixels, 0, smoothed, 0, pixels.size)

        if (smoothFactor > 0.05f) {
            val radius = if (width > 1200) 2 else 1
            for (y in radius until height - radius) {
                for (x in radius until width - radius) {
                    val idx = y * width + x
                    val p = pixels[idx]
                    val r = (p shr 16) and 0xff
                    val g = (p shr 8) and 0xff
                    val b = p and 0xff

                    // Human skin tone heuristic: R > G > B, with healthy warmth
                    val isSkinTone = (r > 60 && g > 30 && b > 15 &&
                            r > g && (r - b) > 10 &&
                            abs(r - g) > 10 && (r - g) < 120)

                    if (isSkinTone) {
                        var sumR = 0
                        var sumG = 0
                        var sumB = 0
                        var count = 0

                        for (dy in -radius..radius) {
                            for (dx in -radius..radius) {
                                val nIdx = (y + dy) * width + (x + dx)
                                val np = pixels[nIdx]
                                val nr = (np shr 16) and 0xff
                                val ng = (np shr 8) and 0xff
                                val nb = np and 0xff
                                // Bilateral weight: only blend if color difference is moderate (preserves sharp edges like eyelashes/lips)
                                val diff = abs(nr - r) + abs(ng - g) + abs(nb - b)
                                if (diff < 90) {
                                    sumR += nr
                                    sumG += ng
                                    sumB += nb
                                    count++
                                }
                            }
                        }

                        if (count > 0) {
                            val avgR = sumR / count
                            val avgG = sumG / count
                            val avgB = sumB / count

                            val blendR = (r * (1f - smoothFactor) + avgR * smoothFactor).roundToInt()
                            val blendG = (g * (1f - smoothFactor) + avgG * smoothFactor).roundToInt()
                            val blendB = (b * (1f - smoothFactor) + avgB * smoothFactor).roundToInt()

                            val a = (p shr 24) and 0xff
                            smoothed[idx] = (a shl 24) or (blendR shl 16) or (blendG shl 8) or blendB
                        }
                    }
                }
            }
        }

        // Apply Radiant Face Glow & Eye Clarity pass
        for (i in pixels.indices) {
            val p = smoothed[i]
            val a = (p shr 24) and 0xff
            var r = (p shr 16) and 0xff
            var g = (p shr 8) and 0xff
            var b = p and 0xff

            // Face Glow adds soft warmth to midtones & highlights
            if (glowFactor > 0f) {
                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val glowWeight = (lum * (1f - lum) * 4f).coerceIn(0f, 1f)
                val glowR = (glowFactor * 24f * glowWeight)
                val glowG = (glowFactor * 16f * glowWeight)
                val glowB = (glowFactor * 8f * glowWeight)

                r = (r + glowR).roundToInt().coerceIn(0, 255)
                g = (g + glowG).roundToInt().coerceIn(0, 255)
                b = (b + glowB).roundToInt().coerceIn(0, 255)
            }

            // Eye clarity micro-contrast
            if (eyeClarity > 0f) {
                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                if (lum > 0.15f && lum < 0.85f) {
                    val boost = (lum - 0.5f) * (eyeClarity * 0.35f) * 255f
                    r = (r + boost).roundToInt().coerceIn(0, 255)
                    g = (g + boost).roundToInt().coerceIn(0, 255)
                    b = (b + boost).roundToInt().coerceIn(0, 255)
                }
            }

            output.setPixel(i % width, i / width, (a shl 24) or (r shl 16) or (g shl 8) or b)
        }

        return output
    }

    /**
     * Unsharp Mask & High-pass convolution for Sharpness and Definition.
     */
    private fun applySharpnessAndDefinition(
        source: Bitmap,
        sharpness: Float,
        definition: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val sharpStrength = (sharpness / 100f) * 1.5f
        val defStrength = (definition / 100f) * 1.2f
        val totalStrength = sharpStrength + defStrength

        val outPixels = IntArray(width * height)
        System.arraycopy(pixels, 0, outPixels, 0, pixels.size)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val centerIdx = y * width + x
                val center = pixels[centerIdx]
                val a = (center shr 24) and 0xff
                val r = (center shr 16) and 0xff
                val g = (center shr 8) and 0xff
                val b = center and 0xff

                // 4-neighbor laplacian kernel for edge extraction
                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val left = pixels[y * width + (x - 1)]
                val right = pixels[y * width + (x + 1)]

                val avgNeighborR = (((top shr 16) and 0xff) + ((bottom shr 16) and 0xff) + ((left shr 16) and 0xff) + ((right shr 16) and 0xff)) / 4f
                val avgNeighborG = (((top shr 8) and 0xff) + ((bottom shr 8) and 0xff) + ((left shr 8) and 0xff) + ((right shr 8) and 0xff)) / 4f
                val avgNeighborB = ((top and 0xff) + (bottom and 0xff) + (left and 0xff) + (right and 0xff)) / 4f

                val edgeR = r - avgNeighborR
                val edgeG = g - avgNeighborG
                val edgeB = b - avgNeighborB

                val newR = (r + edgeR * totalStrength).roundToInt().coerceIn(0, 255)
                val newG = (g + edgeG * totalStrength).roundToInt().coerceIn(0, 255)
                val newB = (b + edgeB * totalStrength).roundToInt().coerceIn(0, 255)

                outPixels[centerIdx] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Radial Vignette filter.
     */
    private fun applyVignette(source: Bitmap, vignetteStrength: Float): Bitmap {
        val width = source.width
        val height = source.height
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val radius = max(width, height) * 0.72f
        val alpha = ((vignetteStrength / 100f) * 220f).roundToInt().coerceIn(0, 255)

        val gradient = RadialGradient(
            width / 2f,
            height / 2f,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return output
    }

    /**
     * Crop, Rotate (90/180/270), and Flip Horizontal.
     */
    private fun applyCropAndTransform(source: Bitmap, crop: CropState): Bitmap {
        var current = source

        // Rotation & Flip
        if (crop.rotationDegrees != 0 || crop.flipHorizontal) {
            val matrix = Matrix()
            if (crop.rotationDegrees != 0) {
                matrix.postRotate(crop.rotationDegrees.toFloat())
            }
            if (crop.flipHorizontal) {
                matrix.postScale(-1f, 1f)
            }
            current = Bitmap.createBitmap(
                current,
                0,
                0,
                current.width,
                current.height,
                matrix,
                true
            )
        }

        // Aspect Ratio Crop
        val targetRatio = crop.aspectRatio.ratio
        if (targetRatio != null) {
            val currentRatio = current.width.toFloat() / current.height.toFloat()
            if (abs(currentRatio - targetRatio) > 0.01f) {
                var cropWidth = current.width
                var cropHeight = current.height

                if (currentRatio > targetRatio) {
                    cropWidth = (current.height * targetRatio).roundToInt()
                } else {
                    cropHeight = (current.width / targetRatio).roundToInt()
                }

                cropWidth = cropWidth.coerceIn(1, current.width)
                cropHeight = cropHeight.coerceIn(1, current.height)

                val xOffset = (current.width - cropWidth) / 2
                val yOffset = (current.height - cropHeight) / 2

                current = Bitmap.createBitmap(current, xOffset, yOffset, cropWidth, cropHeight)
            }
        }

        return current
    }

    /**
     * 4K UHD AI Super-Resolution Upscaling with Edge Sharpening & Denoise.
     */
    suspend fun upscaleBitmap(
        source: Bitmap,
        target: UpscaleTarget,
        noiseReductionStrength: Float = 45f,
        detailEnhanceStrength: Float = 55f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (target == UpscaleTarget.ORIGINAL || target.scaleMultiplier <= 1.0f) {
            return@withContext source
        }

        val targetDimension = if (target == UpscaleTarget.UHD_4K) 3840 else 1920
        val result = SuperResolutionEngine.scaleTo4k(
            source = source,
            targetDimension = targetDimension,
            noiseReductionStrength = noiseReductionStrength,
            detailEnhanceStrength = detailEnhanceStrength
        )
        result.upscaledBitmap
    }

    /**
     * Saves image directly to the user's Gallery (Pictures/iPhoneAIPhotoEditor) using MediaStore.
     */
    suspend fun saveImageToGallery(
        context: Context,
        bitmap: Bitmap,
        qualityPercent: Int = 95
    ): Uri? = withContext(Dispatchers.IO) {
        val fileName = "iPhoneAI_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/iPhoneAIPhotoEditor")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { stream: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent.coerceIn(50, 100), stream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                return@withContext uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                return@withContext null
            }
        }
        null
    }
}
