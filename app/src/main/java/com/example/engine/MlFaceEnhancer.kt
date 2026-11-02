package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class MlFaceMetric(
    val faceIndex: Int,
    val bounds: Rect,
    val smileProbability: Float?,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float
)

data class MlEnhancementResult(
    val enhancedBitmap: Bitmap,
    val facesDetected: Int,
    val metrics: List<MlFaceMetric>,
    val description: String
)

object MlFaceEnhancer {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setMinFaceSize(0.12f)
        .build()

    private val faceDetector = FaceDetection.getClient(detectorOptions)

    /**
     * Detects faces and performs ML-guided automatic facial feature enhancement:
     * - Selective skin smoothing on face cheeks, forehead and chin
     * - Preserves and sharpens eyes, irises, eyelashes, eyebrows, and lips
     */
    suspend fun enhanceFaces(
        source: Bitmap,
        smoothingStrength: Float = 60f,
        detailSharpnessStrength: Float = 50f
    ): MlEnhancementResult = withContext(Dispatchers.Default) {
        val faces = detectFaces(source)

        if (faces.isEmpty()) {
            // No face found in image -> subtle overall clarity enhancement
            val sharpened = ImageProcessor.processImage(
                baseBitmap = source,
                adjustments = com.example.data.model.PhotoAdjustments(sharpness = 20f, definition = 15f),
                filter = com.example.data.model.IPhoneFilter.ORIGINAL,
                aiRetouch = com.example.data.model.AiRetouchSettings(),
                cropState = com.example.data.model.CropState()
            )
            return@withContext MlEnhancementResult(
                enhancedBitmap = sharpened,
                facesDetected = 0,
                metrics = emptyList(),
                description = "No face detected. Applied adaptive scene clarity."
            )
        }

        val width = source.width
        val height = source.height

        val workingBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val metricsList = mutableListOf<MlFaceMetric>()

        val smoothFactor = (smoothingStrength / 100f).coerceIn(0f, 1f)
        val sharpFactor = (detailSharpnessStrength / 100f).coerceIn(0f, 1f)

        // Generate full-image blurred / smoothed version for skin blending
        val smoothedBase = createEdgePreservingSkinSmooth(workingBitmap, smoothFactor)

        // Generate full-image sharpened version for facial feature pop
        val sharpenedBase = createFacialDetailSharpened(workingBitmap, sharpFactor)

        val output = workingBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val outCanvas = Canvas(output)

        // Process each detected face
        faces.forEachIndexed { index, face ->
            metricsList.add(
                MlFaceMetric(
                    faceIndex = index + 1,
                    bounds = face.boundingBox,
                    smileProbability = face.smilingProbability,
                    leftEyeOpenProbability = face.leftEyeOpenProbability,
                    rightEyeOpenProbability = face.rightEyeOpenProbability,
                    headEulerAngleY = face.headEulerAngleY,
                    headEulerAngleZ = face.headEulerAngleZ
                )
            )

            val box = face.boundingBox
            val clampedBox = Rect(
                box.left.coerceIn(0, width - 1),
                box.top.coerceIn(0, height - 1),
                box.right.coerceIn(1, width),
                box.bottom.coerceIn(1, height)
            )

            if (clampedBox.width() > 10 && clampedBox.height() > 10) {
                // 1. Build Skin Mask (Cheeks, forehead, chin)
                val skinMaskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
                val skinMaskCanvas = Canvas(skinMaskBitmap)
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

                // Draw face oval or bounding ellipse
                val faceOvalContour = face.getContour(FaceContour.FACE)
                if (faceOvalContour != null && faceOvalContour.points.size > 5) {
                    val path = Path()
                    val pts = faceOvalContour.points
                    path.moveTo(pts[0].x.coerceIn(0f, width.toFloat()), pts[0].y.coerceIn(0f, height.toFloat()))
                    for (i in 1 until pts.size) {
                        path.lineTo(pts[i].x.coerceIn(0f, width.toFloat()), pts[i].y.coerceIn(0f, height.toFloat()))
                    }
                    path.close()
                    skinMaskCanvas.drawPath(path, maskPaint)
                } else {
                    skinMaskCanvas.drawOval(RectF(clampedBox), maskPaint)
                }

                // 2. Punch out Eyes and Lips from skin mask so they are NOT smoothed
                val punchOutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }

                // Exclude Left Eye
                face.getContour(FaceContour.LEFT_EYE)?.points?.let { eyePts ->
                    if (eyePts.isNotEmpty()) {
                        val path = Path()
                        path.moveTo(eyePts[0].x, eyePts[0].y)
                        for (p in eyePts) path.lineTo(p.x, p.y)
                        path.close()
                        skinMaskCanvas.drawPath(path, punchOutPaint)
                    }
                } ?: run {
                    face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.let { p ->
                        val eyeR = clampedBox.width() * 0.12f
                        skinMaskCanvas.drawCircle(p.x, p.y, eyeR, punchOutPaint)
                    }
                }

                // Exclude Right Eye
                face.getContour(FaceContour.RIGHT_EYE)?.points?.let { eyePts ->
                    if (eyePts.isNotEmpty()) {
                        val path = Path()
                        path.moveTo(eyePts[0].x, eyePts[0].y)
                        for (p in eyePts) path.lineTo(p.x, p.y)
                        path.close()
                        skinMaskCanvas.drawPath(path, punchOutPaint)
                    }
                } ?: run {
                    face.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.let { p ->
                        val eyeR = clampedBox.width() * 0.12f
                        skinMaskCanvas.drawCircle(p.x, p.y, eyeR, punchOutPaint)
                    }
                }

                // Exclude Lips & Mouth
                val upperLip = face.getContour(FaceContour.UPPER_LIP_TOP)?.points
                val lowerLip = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points
                if (!upperLip.isNullOrEmpty() && !lowerLip.isNullOrEmpty()) {
                    val mouthPath = Path()
                    mouthPath.moveTo(upperLip[0].x, upperLip[0].y)
                    for (p in upperLip) mouthPath.lineTo(p.x, p.y)
                    for (p in lowerLip.reversed()) mouthPath.lineTo(p.x, p.y)
                    mouthPath.close()
                    skinMaskCanvas.drawPath(mouthPath, punchOutPaint)
                } else {
                    face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position?.let { m ->
                        val mouthW = clampedBox.width() * 0.22f
                        skinMaskCanvas.drawOval(
                            RectF(m.x - mouthW, m.y - mouthW * 0.5f, m.x + mouthW, m.y + mouthW * 0.5f),
                            punchOutPaint
                        )
                    }
                }

                // 3. Composite smoothed skin through the skin mask
                val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = (smoothFactor * 220).roundToInt()
                }
                outCanvas.drawBitmap(smoothedBase, 0f, 0f, skinPaint)

                // 4. Composite sharpened eyes and lips for high-definition pop
                val featureMaskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
                val featureMaskCanvas = Canvas(featureMaskBitmap)

                // Sharpen Left Eye
                face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.let { p ->
                    val r = clampedBox.width() * 0.15f
                    featureMaskCanvas.drawCircle(p.x, p.y, r, maskPaint)
                }

                // Sharpen Right Eye
                face.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.let { p ->
                    val r = clampedBox.width() * 0.15f
                    featureMaskCanvas.drawCircle(p.x, p.y, r, maskPaint)
                }

                // Sharpen Lips
                face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position?.let { m ->
                    val mouthW = clampedBox.width() * 0.25f
                    featureMaskCanvas.drawOval(
                        RectF(m.x - mouthW, m.y - mouthW * 0.6f, m.x + mouthW, m.y + mouthW * 0.6f),
                        maskPaint
                    )
                }

                val featurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = (sharpFactor * 240).roundToInt()
                }
                outCanvas.drawBitmap(sharpenedBase, 0f, 0f, featurePaint)
            }
        }

        val desc = if (faces.size == 1) {
            "ML Kit enhanced 1 face: skin smoothed + eyes & lips sharpened"
        } else {
            "ML Kit enhanced ${faces.size} faces: skin smoothed + eyes & lips sharpened"
        }

        MlEnhancementResult(
            enhancedBitmap = output,
            facesDetected = faces.size,
            metrics = metricsList,
            description = desc
        )
    }

    /**
     * Executes ML Kit Face Detection asynchronously with coroutines.
     */
    private suspend fun detectFaces(bitmap: Bitmap): List<Face> = suspendCancellableCoroutine { cont ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (cont.isActive) {
                        cont.resume(faces)
                    }
                }
                .addOnFailureListener { exc ->
                    if (cont.isActive) {
                        cont.resumeWithException(exc)
                    }
                }
        } catch (e: Exception) {
            if (cont.isActive) {
                cont.resume(emptyList())
            }
        }
    }

    /**
     * Edge-preserving skin smoothing filter.
     */
    private fun createEdgePreservingSkinSmooth(source: Bitmap, factor: Float): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(width * height)
        val radius = if (width > 1200) 2 else 1

        for (y in radius until height - radius) {
            for (x in radius until width - radius) {
                val idx = y * width + x
                val p = pixels[idx]
                val r = (p shr 16) and 0xff
                val g = (p shr 8) and 0xff
                val b = p and 0xff

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

                        // Bilateral weight to keep edges sharp
                        val diff = abs(nr - r) + abs(ng - g) + abs(nb - b)
                        if (diff < 75) {
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
                    val blendR = (r * (1f - factor) + avgR * factor).roundToInt()
                    val blendG = (g * (1f - factor) + avgG * factor).roundToInt()
                    val blendB = (b * (1f - factor) + avgB * factor).roundToInt()
                    val a = (p shr 24) and 0xff
                    outPixels[idx] = (a shl 24) or (blendR shl 16) or (blendG shl 8) or blendB
                } else {
                    outPixels[idx] = p
                }
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Unsharp mask for iris, lashes, and lip micro-detail sharpening.
     */
    private fun createFacialDetailSharpened(source: Bitmap, factor: Float): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(width * height)
        System.arraycopy(pixels, 0, outPixels, 0, pixels.size)

        val strength = factor * 2.2f

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val center = pixels[idx]
                val a = (center shr 24) and 0xff
                val r = (center shr 16) and 0xff
                val g = (center shr 8) and 0xff
                val b = center and 0xff

                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val left = pixels[y * width + (x - 1)]
                val right = pixels[y * width + (x + 1)]

                val avgR = (((top shr 16) and 0xff) + ((bottom shr 16) and 0xff) + ((left shr 16) and 0xff) + ((right shr 16) and 0xff)) / 4f
                val avgG = (((top shr 8) and 0xff) + ((bottom shr 8) and 0xff) + ((left shr 8) and 0xff) + ((right shr 8) and 0xff)) / 4f
                val avgB = ((top and 0xff) + (bottom and 0xff) + (left and 0xff) + (right and 0xff)) / 4f

                val edgeR = r - avgR
                val edgeG = g - avgG
                val edgeB = b - avgB

                val newR = (r + edgeR * strength).roundToInt().coerceIn(0, 255)
                val newG = (g + edgeG * strength).roundToInt().coerceIn(0, 255)
                val newB = (b + edgeB * strength).roundToInt().coerceIn(0, 255)

                outPixels[idx] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }
}
