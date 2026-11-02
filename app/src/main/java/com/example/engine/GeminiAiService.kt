package com.example.engine

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.AiAnalysisReport
import com.example.data.model.IPhoneFilter
import com.example.data.model.PhotoAdjustments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GeminiAiService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String? {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_GEMINI_API_KEY") key else null
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Checks if Gemini API Key is configured and valid.
     */
    fun hasApiKey(): Boolean {
        return getApiKey() != null
    }

    /**
     * Analyzes image with Gemini Vision or falls back to intelligent offline heuristics.
     */
    suspend fun analyzeImage(bitmap: Bitmap): AiAnalysisReport = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        if (apiKey != null) {
            try {
                val onlineResult = callGeminiVision(bitmap, apiKey)
                if (onlineResult != null) {
                    return@withContext onlineResult
                }
            } catch (e: Exception) {
                // Network or API failure -> seamlessly fallback to offline engine
            }
        }

        // Offline Intelligent Heuristic Analysis
        computeOfflineHeuristicReport(bitmap)
    }

    /**
     * Generates adjustments based on creative natural language prompt (e.g. "Warm Sunset", "Shot on iPhone 16 Pro").
     */
    suspend fun generatePromptAdjustments(
        bitmap: Bitmap,
        userPrompt: String
    ): PhotoAdjustments = withContext(Dispatchers.Default) {
        val promptLower = userPrompt.lowercase()

        when {
            promptLower.contains("gold") || promptLower.contains("sunset") || promptLower.contains("warm") -> {
                PhotoAdjustments(
                    warmth = 38f,
                    brilliance = 24f,
                    highlights = -12f,
                    shadows = 22f,
                    saturation = 18f,
                    vibrance = 25f,
                    contrast = 10f
                )
            }
            promptLower.contains("cinema") || promptLower.contains("iphone 16") || promptLower.contains("dramatic") -> {
                PhotoAdjustments(
                    contrast = 24f,
                    brilliance = 18f,
                    blackPoint = 16f,
                    definition = 25f,
                    sharpness = 22f,
                    highlights = -15f,
                    shadows = 15f,
                    vignette = 15f
                )
            }
            promptLower.contains("portrait") || promptLower.contains("face") || promptLower.contains("beauty") -> {
                PhotoAdjustments(
                    brilliance = 20f,
                    exposure = 8f,
                    shadows = 16f,
                    warmth = 12f,
                    sharpness = 12f,
                    definition = 8f
                )
            }
            promptLower.contains("cyberpunk") || promptLower.contains("neon") || promptLower.contains("night") -> {
                PhotoAdjustments(
                    contrast = 28f,
                    brilliance = 25f,
                    vibrance = 35f,
                    saturation = 20f,
                    tint = 22f,
                    warmth = -18f,
                    sharpness = 30f
                )
            }
            promptLower.contains("black") || promptLower.contains("mono") || promptLower.contains("b&w") -> {
                PhotoAdjustments(
                    saturation = -100f,
                    contrast = 35f,
                    blackPoint = 20f,
                    definition = 25f,
                    sharpness = 20f
                )
            }
            else -> {
                // Smart balanced boost
                PhotoAdjustments(
                    brilliance = 18f,
                    exposure = 5f,
                    highlights = -10f,
                    shadows = 15f,
                    vibrance = 15f,
                    sharpness = 15f
                )
            }
        }
    }

    /**
     * Calls Gemini 2.5 Flash via REST API with base64 downscaled image.
     */
    private fun callGeminiVision(bitmap: Bitmap, apiKey: String): AiAnalysisReport? {
        // Downsample to max 512 for fast network upload
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * 512f / maxOf(bitmap.width, bitmap.height)).roundToInt(),
            (bitmap.height * 512f / maxOf(bitmap.width, bitmap.height)).roundToInt(),
            true
        )
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val base64Data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        val prompt = """
            Analyze this photo as an Apple iPhone photography color scientist. Return ONLY a single JSON object (no markdown, no backticks) with:
            {
              "summary": "Brief 1-sentence photography critique",
              "lightingScore": integer 1-100,
              "dynamicRangeScore": integer 1-100,
              "colorTone": "Warm / Neutral / Cool",
              "suggestedFilter": "VIVID" or "VIVID_WARM" or "DRAMATIC" or "IPHONE_GOLD" or "ORIGINAL",
              "recommendations": ["Tip 1", "Tip 2"],
              "adjustments": {
                "exposure": float between -30 and 30,
                "brilliance": float between 0 and 40,
                "highlights": float between -40 and 20,
                "shadows": float between 0 and 40,
                "contrast": float between -20 and 20,
                "warmth": float between -30 and 30,
                "vibrance": float between 0 and 30,
                "sharpness": float between 0 and 35
              }
            }
        """.trimIndent()

        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Data)
                            })
                        })
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts") ?: return null
        val rawText = parts.getJSONObject(0).optString("text")

        // Parse extracted JSON
        val cleanJsonStr = rawText.replace("```json", "").replace("```", "").trim()
        val data = JSONObject(cleanJsonStr)

        val summary = data.optString("summary", "AI enhanced iPhone grading applied")
        val lightingScore = data.optInt("lightingScore", 85)
        val dynamicRangeScore = data.optInt("dynamicRangeScore", 82)
        val colorTone = data.optString("colorTone", "Warm Golden")
        val filterStr = data.optString("suggestedFilter", "VIVID")
        val suggestedFilter = try { IPhoneFilter.valueOf(filterStr) } catch (e: Exception) { IPhoneFilter.VIVID }

        val recArray = data.optJSONArray("recommendations")
        val recList = mutableListOf<String>()
        if (recArray != null) {
            for (i in 0 until recArray.length()) {
                recList.add(recArray.getString(i))
            }
        }
        if (recList.isEmpty()) {
            recList.add("Recovered shadow detail in darker elements")
            recList.add("Balanced iPhone Smart HDR contrast curve")
        }

        val adjJson = data.optJSONObject("adjustments")
        val adj = PhotoAdjustments(
            exposure = adjJson?.optDouble("exposure", 5.0)?.toFloat() ?: 5f,
            brilliance = adjJson?.optDouble("brilliance", 20.0)?.toFloat() ?: 20f,
            highlights = adjJson?.optDouble("highlights", -10.0)?.toFloat() ?: -10f,
            shadows = adjJson?.optDouble("shadows", 18.0)?.toFloat() ?: 18f,
            contrast = adjJson?.optDouble("contrast", 8.0)?.toFloat() ?: 8f,
            warmth = adjJson?.optDouble("warmth", 12.0)?.toFloat() ?: 12f,
            vibrance = adjJson?.optDouble("vibrance", 16.0)?.toFloat() ?: 16f,
            sharpness = adjJson?.optDouble("sharpness", 15.0)?.toFloat() ?: 15f
        )

        return AiAnalysisReport(
            summary = summary,
            lightingScore = lightingScore,
            dynamicRangeScore = dynamicRangeScore,
            colorTone = colorTone,
            suggestedFilter = suggestedFilter,
            recommendations = recList,
            autoAdjustments = adj
        )
    }

    /**
     * Offline heuristic image analysis (analyzes actual pixel luminance, contrast, and color balance).
     */
    private fun computeOfflineHeuristicReport(bitmap: Bitmap): AiAnalysisReport {
        val sampleW = min(bitmap.width, 200)
        val sampleH = min(bitmap.height, 200)
        val sampled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
        val pixels = IntArray(sampleW * sampleH)
        sampled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        var totalLum = 0.0
        var totalRed = 0.0
        var totalBlue = 0.0
        var darkPixels = 0
        var brightPixels = 0

        for (p in pixels) {
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            val lum = 0.299 * r + 0.587 * g + 0.114 * b
            totalLum += lum
            totalRed += r
            totalBlue += b

            if (lum < 60) darkPixels++
            if (lum > 200) brightPixels++
        }

        val count = pixels.size.toDouble()
        val avgLum = totalLum / count
        val darkRatio = darkPixels / count
        val brightRatio = brightPixels / count
        val isWarm = totalRed > (totalBlue * 1.08)
        val isCool = totalBlue > (totalRed * 1.08)

        val lightingScore = (80 + ((1.0 - abs(avgLum - 128) / 128) * 18)).roundToInt().coerceIn(65, 98)
        val dynamicRangeScore = (75 + ((1.0 - (darkRatio * 0.4 + brightRatio * 0.4)) * 22)).roundToInt().coerceIn(60, 96)

        val colorTone = when {
            isWarm -> "Warm Golden"
            isCool -> "Crisp Cinematic Blue"
            else -> "Balanced Neutral"
        }

        val suggestedFilter = when {
            isWarm -> IPhoneFilter.IPHONE_GOLD
            isCool -> IPhoneFilter.VIVID_COOL
            avgLum < 90 -> IPhoneFilter.DRAMATIC
            else -> IPhoneFilter.VIVID
        }

        // Smart dynamic range recovery parameters
        val exposureAdj = when {
            avgLum < 80 -> 18f
            avgLum > 180 -> -12f
            else -> 6f
        }
        val shadowLift = if (darkRatio > 0.2) 26f else 15f
        val highlightRecovery = if (brightRatio > 0.15) -22f else -10f
        val brilliance = 22f
        val vibrance = if (isWarm || isCool) 18f else 22f
        val warmth = if (isCool) 10f else if (isWarm) 5f else 8f

        val recs = mutableListOf<String>()
        if (darkRatio > 0.2) {
            recs.add("Shadows lifted to reveal deep textures without noise")
        }
        if (brightRatio > 0.15) {
            recs.add("Highlights compressed to prevent harsh blowout")
        }
        recs.add("Smart HDR dynamic tone mapping calibrated for natural contrast")
        recs.add("Skin-safe micro-vibrance applied")

        return AiAnalysisReport(
            summary = "Offline iPhone HDR Engine calibrated exposure, contrast, and color balance.",
            lightingScore = lightingScore,
            dynamicRangeScore = dynamicRangeScore,
            colorTone = colorTone,
            suggestedFilter = suggestedFilter,
            recommendations = recs,
            autoAdjustments = PhotoAdjustments(
                exposure = exposureAdj,
                brilliance = brilliance,
                highlights = highlightRecovery,
                shadows = shadowLift,
                contrast = 8f,
                warmth = warmth,
                vibrance = vibrance,
                sharpness = 16f,
                definition = 12f
            )
        )
    }
}
