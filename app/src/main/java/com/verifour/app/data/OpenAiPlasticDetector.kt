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
        locale: AppLocale
    ): PlasticDetectionResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.OPENAI_API_KEY.trim()
        if (apiKey.isBlank()) {
            throw FlysticApiException("OPENAI_API_KEY chưa được cấu hình trong local.properties", 401)
        }
        if (bitmap == null && userHint.isBlank()) {
            throw FlysticApiException("Cần ít nhất ảnh hoặc mô tả bao bì/nguồn gốc", 400)
        }

        val languageName = when (locale) {
            AppLocale.VI -> "Vietnamese"
            AppLocale.EN -> "English"
            AppLocale.KO -> "Korean"
        }

        val systemPrompt = """
            You are FLYSTIC's zero-retention plastic recycling assistant & Plastic Mascot voice.
            Users take photos or describe plastic items BEFORE bringing them to a campus recycling station so they can self-sort accurately.

            Supported FLYSTIC plastic codes:
            - PET (#1): clear water/soda bottles, thin clear fruit boxes, cold drink cups with #1 mark.
            - HDPE (#2): opaque milk jugs, shampoo/detergent/cosmetic bottles, thick opaque grocery bags.
            - PP (#5): rigid milk-tea cups, microwaveable takeaway food containers, bottle caps, sturdy straws.
            - LDPE (#4): soft thin shopping bags, cling film/food wrap, multipack outer film.
            - PS (#6): styrofoam food boxes, brittle disposable cutlery/thin white cups.
            - PVC_OR_OTHER (#3 PVC or #7 PC/Other): student ID cards, PVC pipes/folders, multi-layer pouches, reusable hard PC water bottles (NOT accepted at FLYSTIC stations).

            CRITICAL HONESTY RULE:
            - NEVER guess blindly! If the image is blurry, dark, cropped, lacks a visible recycling triangle (#1-#7), or could reasonably be multiple plastic types without packaging/origin clues, you MUST respond with status="UNCERTAIN", plastic_code="UNKNOWN", confidence < 0.65.
            - In "mascot_message", honestly say you don't know / are not sure yet (in $languageName), and ask for better clues.
            - When UNCERTAIN, provide 2-3 specific "angle_suggestions" (e.g., flip to inspect the bottom/base for triangle code 1-7, zoom into embossed text/cap, better lighting) AND 2-3 "packaging_origin_questions" (e.g., what product/food/liquid was inside, brand/label text, is it rigid vs stretchy vs foam).
            - When confident (status="DETECTED" and confidence >= 0.72), write "mascot_message" in the playful first-person voice of that plastic's mascot (e.g., PET-Chan, HDPE-Xanh, PP-Cam, LDPE-Vàng, PS-Tím) welcoming the item and confirming its points/category, plus "prep_tip" telling the user how to rinse/dry/prepare it before bringing to the station.

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
        val maxDim = 768
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
        target.compress(Bitmap.CompressFormat.JPEG, 82, out)
        val bytes = out.toByteArray()
        if (target !== bitmap) {
            target.recycle()
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
