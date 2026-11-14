package com.verifour.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.TransactionStatus
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCard
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMuted
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine

@Composable
fun PlasticPatternBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(EcoPrimaryGlow.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.12f),
                radius = size.minDimension * 0.55f
            ),
            center = Offset(size.width * 0.2f, size.height * 0.12f),
            radius = size.minDimension * 0.55f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(EcoOcean.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.35f),
                radius = size.minDimension * 0.50f
            ),
            center = Offset(size.width * 0.85f, size.height * 0.35f),
            radius = size.minDimension * 0.50f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(EcoSunshine.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.9f),
                radius = size.minDimension * 0.50f
            ),
            center = Offset(size.width * 0.5f, size.height * 0.9f),
            radius = size.minDimension * 0.50f
        )

        // Subtle tiled eco rings
        val step = 220f
        var y = 60f
        while (y < size.height) {
            var x = 50f
            while (x < size.width) {
                drawCircle(
                    color = EcoPrimary.copy(alpha = 0.045f),
                    radius = 18f,
                    center = Offset(x, y),
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = EcoOcean.copy(alpha = 0.04f),
                    radius = 9f,
                    center = Offset(x + 95f, y + 85f)
                )
                x += step
            }
            y += step
        }
    }
}

@Composable
fun EcoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = EcoCard,
    borderColor: Color = EcoBorder,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun LanguageSwitcherRow(
    currentLocale: AppLocale,
    onSelect: (AppLocale) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val options = listOf(
            AppLocale.VI to "VI",
            AppLocale.EN to "EN",
            AppLocale.KO to "KO"
        )
        options.forEach { (loc, label) ->
            val selected = currentLocale == loc
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) EcoPrimary else EcoSecondary)
                    .clickable { onSelect(loc) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White else EcoForeground,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    status: TransactionStatus,
    locale: AppLocale,
    modifier: Modifier = Modifier
) {
    val (bg, fg, key) = when (status) {
        TransactionStatus.PENDING -> Triple(EcoSunshine.copy(alpha = 0.25f), EcoForeground, "adminTransactionPending")
        TransactionStatus.CONFIRMED -> Triple(EcoPrimary.copy(alpha = 0.14f), EcoPrimary, "adminTransactionConfirmed")
        TransactionStatus.EXPIRED -> Triple(EcoMuted, EcoMutedForeground, "adminTransactionExpired")
        TransactionStatus.CANCELLED -> Triple(EcoCoral.copy(alpha = 0.14f), EcoCoral, "adminTransactionCancelled")
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = FlysticTranslations.t(key, locale).uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun UniversityStickers(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .rotate(-2f)
                .clip(RoundedCornerShape(14.dp))
                .background(EcoPrimary.copy(alpha = 0.1f))
                .border(1.5.dp, EcoPrimary.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = EcoPrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "HUST 🇻🇳",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = EcoPrimary
            )
        }
        Text("×", fontWeight = FontWeight.Bold, color = EcoMutedForeground)
        Row(
            modifier = Modifier
                .rotate(2f)
                .clip(RoundedCornerShape(14.dp))
                .background(EcoOcean.copy(alpha = 0.1f))
                .border(1.5.dp, EcoOcean.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = EcoOcean,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "JBNU 🇰🇷",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = EcoOcean
            )
        }
    }
}

@Composable
fun QrCodeBitmapImage(
    content: String,
    sizeDp: Dp = 220.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(content) { generateQrBitmap(content, 512) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR $content",
            modifier = modifier.size(sizeDp)
        )
    }
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
    if (content.isBlank()) return null
    return runCatching {
        val hints = mapOf(EncodeHintType.MARGIN to 2)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp[x, y] = if (matrix[x, y]) 0xFF0F1D14.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        bmp
    }.getOrNull()
}
