package com.verifour.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.data.DetectionVerdict
import com.verifour.app.data.OpenAiPlasticDetector
import com.verifour.app.data.PlasticDetectionResult
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.PLASTIC_MASCOTS
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.PixelPlasticMascotSprite
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCard
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun PlasticAiSortScreen(
    locale: AppLocale,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val detector = remember { OpenAiPlasticDetector() }

    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    // Strictly volatile RAM state — nothing is ever written to disk, SharedPreferences, or backend.
    var transientBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userHint by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var detectionResult by remember { mutableStateOf<PlasticDetectionResult?>(null) }

    fun clearVolatileState() {
        transientBitmap = null
        userHint = ""
        errorMsg = null
        detectionResult = null
    }

    DisposableEffect(Unit) {
        onDispose {
            clearVolatileState()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            transientBitmap = bitmap
            detectionResult = null
            errorMsg = null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }.getOrNull()
                }
                if (decoded != null) {
                    transientBitmap = decoded
                    detectionResult = null
                    errorMsg = null
                }
            }
        }
    }

    fun runDetection() {
        if (isAnalyzing) return
        if (transientBitmap == null && userHint.isBlank()) return
        isAnalyzing = true
        errorMsg = null
        scope.launch {
            runCatching {
                detector.detectPlastic(
                    bitmap = transientBitmap,
                    userHint = userHint,
                    locale = locale
                )
            }.onSuccess { res ->
                detectionResult = res
            }.onFailure { err ->
                errorMsg = err.message ?: "Error"
            }
            isAnalyzing = false
        }
    }

    val quickClues = remember(locale) {
        when (locale) {
            AppLocale.VI -> listOf(
                "Chai nước suối trong suốt",
                "Chai dầu gội / sữa tắm đục",
                "Ly trà sữa / hộp đựng cơm",
                "Túi nilon / màng bọc thực phẩm",
                "Hộp xốp / thìa nĩa dùng 1 lần"
            )
            AppLocale.EN -> listOf(
                "Clear mineral water bottle",
                "Opaque shampoo / milk bottle",
                "Milk-tea cup / takeaway box",
                "Soft plastic bag / food wrap",
                "Foam food box / disposable fork"
            )
            AppLocale.KO -> listOf(
                "투명 생수병",
                "불투명 샴푸·우유 용기",
                "밀크티 컵·음식 용기",
                "비닐봉지·식품 랩",
                "스티로폼 용기·일회용 식기"
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = t("backHome"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoMutedForeground,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "🤖 ${t("aiDetectTitle")}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = t("aiDetectSubtitle"),
                fontSize = 13.sp,
                color = EcoMutedForeground
            )
        }

        // Strict Zero-Retention Privacy Notice
        item {
            EcoCard(
                containerColor = EcoPrimary.copy(alpha = 0.08f),
                borderColor = EcoPrimary.copy(alpha = 0.28f),
                contentPadding = 12.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = EcoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = t("aiDetectPrivacyBadge"),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoForeground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Photo Input + Optional Packaging/Origin Context
        item {
            EcoCard(contentPadding = 16.dp) {
                transientBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = t("aiDetectCameraBtn"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EcoOcean.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = EcoOcean, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = t("aiDetectGalleryBtn"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoOcean,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = t("aiDetectHintLabel"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = userHint,
                    onValueChange = { userHint = it },
                    placeholder = { Text(t("aiDetectHintPlaceholder"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EcoCard,
                        unfocusedContainerColor = EcoCard,
                        focusedBorderColor = EcoPrimary,
                        unfocusedBorderColor = EcoBorder
                    )
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = t("aiDetectQuickCluesTitle"),
                    fontSize = 11.sp,
                    color = EcoMutedForeground
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickClues.forEach { clue ->
                        val active = userHint.contains(clue)
                        Surface(
                            shape = CircleShape,
                            color = if (active) EcoPrimary else EcoSecondary,
                            border = BorderStroke(1.dp, if (active) EcoPrimary else EcoBorder),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    userHint = if (userHint.isBlank()) clue else "$userHint; $clue"
                                }
                        ) {
                            Text(
                                text = clue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else EcoForeground,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { runDetection() },
                        enabled = !isAnalyzing && (transientBitmap != null || userHint.isNotBlank()),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoForeground)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(t("aiDetectAnalyzing"), fontSize = 12.sp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = EcoSunshine, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = t("aiDetectAnalyzeBtn"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (transientBitmap != null || detectionResult != null || userHint.isNotBlank()) {
                        OutlinedButton(
                            onClick = { clearVolatileState() },
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EcoCoral.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = t("aiDetectClearBtn"), tint = EcoCoral, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (!errorMsg.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        fontSize = 12.sp,
                        color = EcoCoral
                    )
                }
            }
        }

        // Result Card with Plastic Mascot Reply OR Honest "Uncertain" Guidance
        detectionResult?.let { res ->
            item {
                when (res.verdict) {
                    DetectionVerdict.DETECTED -> {
                        val plastic = res.plasticType ?: return@item
                        val meta = PLASTIC_MASCOTS.getValue(plastic)
                        val accent = Color(meta.primaryHex)
                        EcoCard(
                            containerColor = Color(meta.secondaryHex).copy(alpha = 0.25f),
                            borderColor = accent.copy(alpha = 0.45f),
                            contentPadding = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White)
                                        .border(1.5.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelPlasticMascotSprite(
                                        plasticType = plastic,
                                        spriteSize = 72.dp,
                                        happyOverride = true,
                                        fastBob = true
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${plastic.emoji} ${res.resinCodeLabel}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accent
                                    )
                                    Text(
                                        text = t(meta.nameKey),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoForeground
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = t("aiDetectPointsPerKg", mapOf("pts" to plastic.basePoints)),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EcoPrimary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = t("aiDetectConfidence", mapOf("pct" to (res.confidence * 100).roundToInt())),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EcoMutedForeground
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Mascot Speech Bubble Reply
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "💬 ${t(meta.nameKey)}: \"${res.mascotMessage}\"",
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EcoForeground
                                )
                            }

                            if (res.prepTip.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "✨ ${t("aiDetectPrepTipTitle")}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoForeground
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = res.prepTip,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = EcoForeground.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    DetectionVerdict.UNCERTAIN -> {
                        EcoCard(
                            containerColor = EcoSunshine.copy(alpha = 0.18f),
                            borderColor = EcoSunshine,
                            contentPadding = 16.dp
                        ) {
                            Text(
                                text = "🤔 ${t("aiDetectUncertainTitle")}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoForeground
                            )
                            Spacer(Modifier.height(6.dp))
                            if (res.mascotMessage.isNotBlank()) {
                                Text(
                                    text = res.mascotMessage,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = EcoForeground
                                )
                                Spacer(Modifier.height(10.dp))
                            }

                            if (res.angleSuggestions.isNotEmpty()) {
                                Text(
                                    text = t("aiDetectAnglesTitle"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoForeground
                                )
                                Spacer(Modifier.height(4.dp))
                                res.angleSuggestions.forEach { tip ->
                                    Text(
                                        text = "• $tip",
                                        fontSize = 12.sp,
                                        color = EcoForeground.copy(alpha = 0.9f),
                                        lineHeight = 17.sp
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            if (res.packagingOrOriginQuestions.isNotEmpty()) {
                                Text(
                                    text = t("aiDetectPackagingTitle"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoForeground
                                )
                                Spacer(Modifier.height(4.dp))
                                res.packagingOrOriginQuestions.forEach { q ->
                                    Text(
                                        text = "• $q",
                                        fontSize = 12.sp,
                                        color = EcoForeground.copy(alpha = 0.9f),
                                        lineHeight = 17.sp
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                            ) {
                                Text(
                                    text = t("aiDetectRetakeAngleBtn"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    DetectionVerdict.UNSUPPORTED_PLASTIC -> {
                        EcoCard(
                            containerColor = EcoCoral.copy(alpha = 0.1f),
                            borderColor = EcoCoral.copy(alpha = 0.45f),
                            contentPadding = 16.dp
                        ) {
                            Text(
                                text = "🚫 ${t("aiDetectUnsupportedTitle")}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoCoral
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = res.mascotMessage,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = EcoForeground
                            )
                            if (res.prepTip.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = res.prepTip,
                                    fontSize = 12.sp,
                                    color = EcoMutedForeground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
