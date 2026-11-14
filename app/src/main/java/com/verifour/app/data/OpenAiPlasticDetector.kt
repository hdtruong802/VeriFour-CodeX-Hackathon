package com.verifour.app.data

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.verifour.app.BuildConfig
import com.verifour.app.model.AppLocale
import com.verifour.app.model.PlasticType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class DetectionVerdict {
    DETECTED,
    UNCERTAIN,
    UNSUPPORTED_PLASTIC
}

data class PlasticDetectionResult(
    val verdict: DetectionVerdict,
    val plasticType: PlasticType?,
    val resinCodeLabel: String,
    val confidence: Double,
    val mascotMessage: String,
    val prepTip: String,
    val angleSuggestions: List<String>,
    val packagingOrOriginQuestions: List<String>
)

class OpenAiPlasticDetector {
    companion object {
        private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-4o-mini"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun parseDetectionJson(rawContent: String): PlasticDetectionResult {
            val cleaned = rawContent
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val obj = JsonParser.parseString(cleaned).asJsonObject
            val rawStatus = obj.get("status")?.takeIf { !it.isJsonNull }?.asString?.uppercase().orEmpty()
            val rawCode = obj.get("plastic_code")?.takeIf { !it.isJsonNull }?.asString?.uppercase().orEmpty()
            val confidence = obj.get("confidence")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
            val mascotMessage = obj.get("mascot_message")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            val prepTip = obj.get("prep_tip")?.takeIf { !it.isJsonNull }?.asString.orEmpty()

            val angles = obj.getAsJsonArray("angle_suggestions")
                ?.mapNotNull { el -> el.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotEmpty() } }
                ?: emptyList()

            val questions = obj.getAsJsonArray("packaging_origin_questions")
                ?.mapNotNull { el -> el.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotEmpty() } }
                ?: emptyList()

            val detectedPlastic = when (rawCode) {
                "PET" -> PlasticType.PET
                "HDPE" -> PlasticType.HDPE
                "PP" -> PlasticType.PP
                "LDPE" -> PlasticType.LDPE
                "PS" -> PlasticType.PS
                else -> null
            }

            return when {
                rawCode == "PVC_OR_OTHER" || rawStatus == "UNSUPPORTED" -> PlasticDetectionResult(
                    verdict = DetectionVerdict.UNSUPPORTED_PLASTIC,
                    plasticType = null,
                    resinCodeLabel = "PVC (#3) / OTHER (#7)",
                    confidence = confidence.coerceIn(0.0, 1.0),
                    mascotMessage = mascotMessage,
                    prepTip = prepTip,
                    angleSuggestions = angles,
                    packagingOrOriginQuestions = questions
                )
                rawStatus == "DETECTED" && detectedPlastic != null && confidence >= 0.68 -> PlasticDetectionResult(
                    verdict = DetectionVerdict.DETECTED,
                    plasticType = detectedPlastic,
                    resinCodeLabel = "${detectedPlastic.code} (${resinNumber(detectedPlastic)})",
                    confidence = confidence.coerceIn(0.0, 1.0),
                    mascotMessage = mascotMessage,
                    prepTip = prepTip,
                    angleSuggestions = emptyList(),
                    packagingOrOriginQuestions = emptyList()
                )
                else -> PlasticDetectionResult(
                    verdict = DetectionVerdict.UNCERTAIN,
                    plasticType = null,
                    resinCodeLabel = "UNKNOWN",
                    confidence = confidence.coerceIn(0.0, 1.0),
                    mascotMessage = mascotMessage,
                    prepTip = prepTip,
                    angleSuggestions = angles,
                    packagingOrOriginQuestions = questions
                )
            }
        }

        private fun resinNumber(type: PlasticType): String = when (type) {
            PlasticType.PET -> "#1"
            PlasticType.HDPE -> "#2"
            PlasticType.PP -> "#5"
            PlasticType.LDPE -> "#4"
            PlasticType.PS -> "#6"
        }
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = BuildConfig.OPENAI_API_KEY.isNotBlank()

    suspend fun detectPlastic(
        bitmap: Bitmap?,
        userHint: String,
        locale: AppLocale,
        serverUrl: String? = null
    ): PlasticDetectionResult = withContext(Dispatchers.IO) {
        if (bitmap == null && userHint.isBlank()) {
            throw FlysticApiException("Cần ít nhất ảnh hoặc mô tả bao bì/nguồn gốc", 400)
        }

        val base64Jpeg = bitmap?.let { encodeBitmapTransient(it) }

        // 1. Try local Mac server first (prompt is in backend/ai_prompt.txt, no APK rebuild needed!)
        val targetServer = serverUrl?.takeIf { it.isNotBlank() } ?: FlysticApiClient.DEFAULT_BASE_URL
        if (targetServer.isNotBlank()) {
            val normalized = FlysticApiClient.normalizeBaseUrl(targetServer)
            val backendPayload = mapOf(
                "imageBase64" to (base64Jpeg ?: ""),
                "userHint" to userHint.trim(),
                "locale" to locale.code
            )
            val backendReq = Request.Builder()
                .url("$normalized/api/v1/ai/detect")
                .post(gson.toJson(backendPayload).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val backendResult = runCatching {
                client.newCall(backendReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        if (body.isNotBlank()) parseDetectionJson(body) else null
                    } else null
                }
            }.getOrNull()

            if (backendResult != null) {
                return@withContext backendResult
            }
        }

        // 2. Direct OpenAI fallback
        val apiKey = BuildConfig.OPENAI_API_KEY.trim()
        if (apiKey.isBlank()) {
            throw FlysticApiException("Không thể kết nối máy chủ và chưa cấu hình OPENAI_API_KEY", 401)
        }

        val languageName = when (locale) {
            AppLocale.VI -> "Vietnamese"
            AppLocale.EN -> "English"
            AppLocale.KO -> "Korean"
        }

        val systemPrompt = """
            You are VeriFour's friendly and helpful campus plastic recycling assistant & Plastic Mascot voice.
            Users take photos or describe plastic items BEFORE bringing them to a campus recycling station so they can self-sort easily.

            Supported VeriFour plastic codes:
            - PET (#1): clear transparent drink/water bottles, thin clear fruit/salad boxes, clear cold-cup bubble tea cups.
            - HDPE (#2): opaque or translucent jugs/bottles (milk, shampoo, detergent, cosmetic lotion), thick crinkly shopping bags.
            - PP (#5): rigid milk-tea cups, microwaveable food takeaway containers, plastic bowls, bottle caps, sturdy reusable straws, medicine bottles.
            - LDPE (#4): soft, stretchy thin plastic bags, grocery shopping bags, cling film/food wrap, bubble wrap, flexible squeeze bottles.
            - PS (#6): white styrofoam lunchboxes/cups, rigid brittle disposable cutlery (spoons, forks, knives), yoghurt cups, CD cases.
            - PVC_OR_OTHER (#3 PVC or #7 PC/Other): PVC pipes/cards/clear binders, multi-layer pouches, hard polycarbonate bottles (NOT accepted at VeriFour stations).

            DETECTION GUIDELINES (FRIENDLY & PRACTICAL):
            - DO NOT be overly strict! You DO NOT need to see text, logos, or recycling triangle symbols (#1-#7) to classify the item.
            - Actively infer the plastic code from the visual appearance of the object: its shape, transparency/opacity, wall thickness, gloss, rigidity, and common real-world purpose:
              * Standard clear drinking water / soda bottle -> PET (#1).
              * Opaque/translucent shampoo, lotion, or milk bottle -> HDPE (#2).
              * Takeaway food box, bubble tea cup, soup container, or bottle cap -> PP (#5).
              * Soft, stretchy plastic bag or cling film -> LDPE (#4).
              * White styrofoam container or brittle disposable cutlery -> PS (#6).
            - If text or a recycling code is visible, use it to confirm or refine your prediction.
            - Status should be "DETECTED" (confidence 0.75 - 0.92) whenever the visual appearance strongly suggests one of these common plastics, even without any readable text.
            - In "prep_tip" or "mascot_message", always give practical advice on rinsing/drying, PLUS a friendly reminder to double-check (e.g. "Khuyến nghị: Bạn có thể kiểm tra thêm dưới đáy xem có dập nổi mã hoặc ký hiệu tam giác để chắc chắn 100% nhé!").
            - Only return status="UNCERTAIN" (confidence < 0.60) if the object is genuinely not discernible as plastic, totally obscured, pitch black, or made of non-plastic materials (paper, metal, glass, organic food).
            - When status="DETECTED", write "mascot_message" in the cheerful, encouraging first-person voice of that plastic's mascot (PET-Chan, HDPE-Xanh, PP-Cam, LDPE-Vàng, PS-Tím) welcoming the user and confirming the plastic type and points!

            Output strictly valid JSON with exact keys:
            {
              "status": "DETECTED" | "UNCERTAIN" | "UNSUPPORTED",
              "plastic_code": "PET" | "HDPE" | "PP" | "LDPE" | "PS" | "PVC_OR_OTHER" | "UNKNOWN",
              "confidence": 0.0 to 1.0,
              "mascot_message": "string in $languageName",
              "prep_tip": "string in $languageName",
              "angle_suggestions": ["string in $languageName"],
              "packaging_origin_questions": ["string in $languageName"]
            }
        """.trimIndent()

        val userText = buildString {
            append("Help me identify this plastic item for self-sorting before bringing it to the recycling station.")
            if (userHint.isNotBlank()) {
                append("\nUser clues about packaging, origin, or usage: ")
                append(userHint.trim())
            }
        }

        val contentList = mutableListOf<Map<String, Any>>(
            mapOf("type" to "text", "text" to userText)
        )
        if (bitmap != null) {
            val base64Jpeg = encodeBitmapTransient(bitmap)
            contentList.add(
                mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf(
                        "url" to "data:image/jpeg;base64,$base64Jpeg",
                        "detail" to "high"
                    )
                )
            )
        }

        val payload = mapOf(
            "model" to MODEL,
            "store" to false,
            "temperature" to 0.2,
            "max_tokens" to 550,
            "response_format" to mapOf("type" to "json_object"),
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to contentList)
            )
        )

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = runCatching {
                        JsonParser.parseString(raw).asJsonObject
                            .getAsJsonObject("error")
                            ?.get("message")?.asString
                    }.getOrNull()
                    throw FlysticApiException(
                        errMsg ?: "OpenAI API error (${response.code})",
                        response.code
                    )
                }
                val root = JsonParser.parseString(raw).asJsonObject
                val content = root.getAsJsonArray("choices")
                    ?.firstOrNull()
                    ?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")
                    ?.asString
                    ?: throw FlysticApiException("Empty AI response", response.code)

                parseDetectionJson(content)
            }
        } catch (e: FlysticApiException) {
            throw e
        } catch (e: IOException) {
            throw FlysticApiException(e.message ?: "Network error", 0)
        }
    }

    private fun encodeBitmapTransient(bitmap: Bitmap): String {
        val maxDim = 1024
        val scale = max(bitmap.width, bitmap.height).let { largest ->
            if (largest > maxDim) maxDim.toFloat() / largest.toFloat() else 1f
        }
        val target = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        target.compress(Bitmap.CompressFormat.JPEG, 88, out)
        val bytes = out.toByteArray()
        if (target !== bitmap) {
            target.recycle()
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
