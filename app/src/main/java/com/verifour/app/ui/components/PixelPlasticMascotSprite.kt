package com.verifour.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verifour.app.model.PLASTIC_MASCOTS
import com.verifour.app.model.PlasticType
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoPrimary
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PixelPlasticMascotSprite(
    plasticType: PlasticType,
    modifier: Modifier = Modifier,
    spriteSize: Dp = 76.dp,
    externalJumpY: Float = 0f,
    externalScaleX: Float = 1f,
    externalScaleY: Float = 1f,
    externalTiltDeg: Float = 0f,
    happyOverride: Boolean = false,
    fastBob: Boolean = false,
    flipHorizontal: Boolean = false,
    onTap: (() -> Unit)? = null
) {
    val meta = PLASTIC_MASCOTS[plasticType] ?: PLASTIC_MASCOTS.getValue(PlasticType.PET)
    val infinite = rememberInfiniteTransition(label = "plastic_mascot_bob")
    val bobDuration = if (fastBob) 260 else 680

    val bobPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = bobDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plastic_bob"
    )

    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(plasticType) {
        while (true) {
            delay(2800.milliseconds)
            isBlinking = true
            delay(130.milliseconds)
            isBlinking = false
        }
    }

    val idleOffsetY = -bobPhase * if (fastBob) 8f else 5f
    val idleScaleX = 1.04f - bobPhase * 0.08f
    val idleScaleY = 0.95f + bobPhase * 0.09f
    val frame = if (bobPhase >= 0.5f) 1 else 0

    val totalHeightFactor = ((-idleOffsetY - externalJumpY) / 36f).coerceIn(0f, 1f)
    val shadowScale = 1f - totalHeightFactor * 0.4f

    Box(
        modifier = modifier.then(
            if (onTap != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                )
            } else {
                Modifier
            }
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier
                    .size(spriteSize)
                    .graphicsLayer {
                        translationY = (idleOffsetY + externalJumpY) * density * 0.85f
                        scaleX = idleScaleX * externalScaleX * (if (flipHorizontal) -1f else 1f)
                        scaleY = idleScaleY * externalScaleY
                        rotationZ = externalTiltDeg
                    }
            ) {
                val grid = buildPlasticMascotGrid(
                    plasticType = plasticType,
                    frame = frame,
                    blinking = isBlinking && !happyOverride,
                    happy = happyOverride
                )
                val rows = grid.size
                val cols = grid[0].length
                val cell = floor(size.minDimension / rows.toFloat()).coerceAtLeast(1f)
                val totalW = cell * cols
                val totalH = cell * rows
                val offsetX = (size.width - totalW) / 2f
                val offsetY = (size.height - totalH) / 2f

                val primaryColor = Color(meta.primaryHex)
                val secondaryColor = Color(meta.secondaryHex)
                val shadowColor = Color(
                    red = (primaryColor.red * 0.72f).coerceIn(0f, 1f),
                    green = (primaryColor.green * 0.72f).coerceIn(0f, 1f),
                    blue = (primaryColor.blue * 0.72f).coerceIn(0f, 1f),
                    alpha = 1f
                )
                val capColor = when (plasticType) {
                    PlasticType.PET -> Color(0xFF1A6FA8)
                    PlasticType.HDPE -> Color(0xFF157D4C)
                    PlasticType.PP -> Color(0xFF20A064)
                    PlasticType.LDPE -> Color(0xFF20A064)
                    PlasticType.PS -> Color(0xFF6B4EA3)
                }

                for (r in 0 until rows) {
                    val rowStr = grid[r]
                    for (c in 0 until cols) {
                        val ch = rowStr[c]
                        if (ch == '.') continue
                        val color = when (ch) {
                            'K', 'E' -> Color(0xFF102A1F)
                            '1' -> primaryColor
                            '2' -> secondaryColor
                            '3' -> shadowColor
                            'W' -> Color(0xFFFFFFFF)
                            'C' -> Color(0xFFFF8FA3)
                            'B' -> capColor
                            'L' -> EcoPrimary
                            'M' -> Color(0xFF73EB9E)
                            'Y' -> Color(0xFFF5B82E)
                            else -> primaryColor
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(offsetX + c * cell, offsetY + r * cell),
                            size = Size(cell + 0.4f, cell + 0.4f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Canvas(
                modifier = Modifier
                    .size(width = spriteSize * 0.58f, height = 6.dp)
                    .graphicsLayer { scaleX = shadowScale }
            ) {
                drawRect(
                    color = EcoForeground.copy(alpha = 0.15f),
                    topLeft = Offset(size.width * 0.12f, 1f),
                    size = Size(size.width * 0.76f, size.height * 0.65f)
                )
            }
        }
    }
}

private fun buildPlasticMascotGrid(
    plasticType: PlasticType,
    frame: Int,
    blinking: Boolean,
    happy: Boolean
): List<String> {
    val base = when (plasticType) {
        PlasticType.PET -> if (frame == 0) PET_F0 else PET_F1
        PlasticType.HDPE -> if (frame == 0) HDPE_F0 else HDPE_F1
        PlasticType.PP -> if (frame == 0) PP_F0 else PP_F1
        PlasticType.LDPE -> if (frame == 0) LDPE_F0 else LDPE_F1
        PlasticType.PS -> if (frame == 0) PS_F0 else PS_F1
    }.map { it.toCharArray() }

    if (blinking || happy) {
        for (r in 5..12) {
            for (c in 3..14) {
                if (base[r][c] == 'W' && base[r + 1][c] == 'E') {
                    base[r][c] = '1'
                }
            }
        }
        if (happy) {
            for (r in 6..12) {
                for (c in 3..14) {
                    if (base[r][c] == 'E' && base[r - 1][c] == 'E') {
                        base[r][c] = '1'
                    }
                }
            }
        }
    }

    return base.map { String(it) }
}

// 18x18 pixel art sprites for the 5 Plastic Types

// PET: Sparkling water bottle mascot with cap & eco label
private val PET_F0 = listOf(
    "......KKKKKK......",
    "......KBBBBK......",
    "......KKKKKK......",
    ".......K22K.......",
    ".....KK2112KK.....",
    "....K22111111K....",
    "....K21111111K....",
    "....K1WE11EW1K....",
    ".KK.K1EE11EE1K.KK.",
    "K1K.K1C1KK1C1K.K1K",
    ".KKKLLLLLLLLLLKKK.",
    "....KLLMMMMLLK....",
    "....K11111111K....",
    "....K31111113K....",
    ".....K333333K.....",
    ".....KKKKKKKK.....",
    ".....KK....KK.....",
    ".................."
)

private val PET_F1 = listOf(
    "......KKKKKK......",
    "......KBBBBK......",
    "......KKKKKK......",
    ".......K22K.......",
    ".....KK2112KK.....",
    "....K22111111K....",
    ".KK.K21111111K.KK.",
    "K1K.K1WE11EW1K.K1K",
    ".KK.K1EE11EE1K.KK.",
    "....K1C1KK1C1K....",
    "....KLLLLLLLLK....",
    "....KLLMMMMLLK....",
    "....K11111111K....",
    "....K31111113K....",
    ".....KKKKKKKK.....",
    "....KK......KK....",
    "..................",
    ".................."
)

// HDPE: Sturdy milk/detergent jug with side handle
private val HDPE_F0 = listOf(
    ".................. ",
    ".....KKKKKK.......",
    ".....KBBBBK.......",
    ".....KKKKKK.......",
    "....K221111KK.....",
    "...K221111111KK...",
    "...K21111111K11K..",
    "...K1WE11EW1K.K1K.",
    "..KK1EE11EE1K.K1K.",
    ".K1K1C1KK1C1KKK1K.",
    "..KK1WWWWWW111KK..",
    "...K1WLLLLW11K....",
    "...K1WWWWWW11K....",
    "...K311111113K....",
    "....KKKKKKKKK.....",
    ".....KK...KK......",
    "..................",
    ".................."
).map { it.trimEnd().padEnd(18, '.').take(18) }

private val HDPE_F1 = listOf(
    ".....KKKKKK.......",
    ".....KBBBBK.......",
    ".....KKKKKK.......",
    "....K221111KK.....",
    "...K221111111KK...",
    "...K21111111K11K..",
    "..KK1WE11EW1K.K1K.",
    ".K1K1EE11EE1K.K1K.",
    "..KK1C1KK1C1KKK1K.",
    "...K1WWWWWW111KK..",
    "...K1WLLLLW11K....",
    "...K1WWWWWW11K....",
    "...K311111113K....",
    "....KKKKKKKKK.....",
    "....KK.....KK.....",
    "..................",
    "..................",
    ".................."
)

// PP: Cute boba cup mascot with green straw & pearls
private val PP_F0 = listOf(
    "..........KK......",
    ".........KLLK.....",
    "........KLLK......",
    "....KKKKKKKKKK....",
    "...KWWWWWWWWWWK...",
    "...KKKKKKKKKKKK...",
    "....K21111111K....",
    "....K1WE11EW1K....",
    ".KK.K1EE11EE1K.KK.",
    "K1K.K1C1KK1C1K.K1K",
    ".KK.K11111111K.KK.",
    "....K11111111K....",
    "....K1KK1KK1KK....",
    "....K3KK3KK3KK....",
    ".....KKKKKKKK.....",
    ".....KK....KK.....",
    "..................",
    ".................."
)

private val PP_F1 = listOf(
    "..........KK......",
    ".........KMMK.....",
    "........KLLK......",
    "....KKKKKKKKKK....",
    "...KWWWWWWWWWWK...",
    "...KKKKKKKKKKKK...",
    ".KK.K21111111K.KK.",
    "K1K.K1WE11EW1K.K1K",
    ".KK.K1EE11EE1K.KK.",
    "....K1C1KK1C1K....",
    "....K11111111K....",
    "....K11111111K....",
    "....K1KK1KK1KK....",
    "....K3KK3KK3KK....",
    ".....KKKKKKKK.....",
    "....KK......KK....",
    "..................",
    ".................."
)

// LDPE: Fluffy eco shopping bag mascot with twin handles
private val LDPE_F0 = listOf(
    "....KK......KK....",
    "...K11K....K11K...",
    "...K11KKKKKK11K...",
    "...K11K....K11K...",
    "..KK2222222222KK..",
    "..K221111111111K..",
    "..K11WE1111EW11K..",
    ".KK11EE1111EE11KK.",
    "K1K11C11KK11C11K1K",
    ".KK1111LLLL1111KK.",
    "..K111LMMMML111K..",
    "..K1111LLLL1111K..",
    "..K311111111113K..",
    "...KKKKKKKKKKKK...",
    ".....KK....KK.....",
    "..................",
    "..................",
    ".................."
)

private val LDPE_F1 = listOf(
    "....KK......KK....",
    "...K11K....K11K...",
    "...K11KKKKKK11K...",
    "...K11K....K11K...",
    "..KK2222222222KK..",
    ".KK221111111111KK.",
    "K1K11WE1111EW11K1K",
    ".KK11EE1111EE11KK.",
    "..K11C11KK11C11K..",
    "..K1111LLLL1111K..",
    "..K111LMMMML111K..",
    "..K1111LLLL1111K..",
    "..K311111111113K..",
    "...KKKKKKKKKKKK...",
    "....KK......KK....",
    "..................",
    "..................",
    ".................."
)

// PS: Foam takeout box mascot
private val PS_F0 = listOf(
    "..................",
    "..................",
    "....KKKKKKKKKK....",
    "...KWWWWWWWWWWK...",
    "..K222222222222K..",
    "..KKKKKKKKKKKKKK..",
    "..K211111111111K..",
    "..K11WE1111EW11K..",
    ".KK11EE1111EE11KK.",
    "K1K11C11KK11C11K1K",
    ".KK111WWWWWW111KK.",
    "..K11WWWWWWWW11K..",
    "..K331111111133K..",
    "...KKKKKKKKKKKK...",
    ".....KK....KK.....",
    "..................",
    "..................",
    ".................."
)

private val PS_F1 = listOf(
    "..................",
    "....KKKKKKKKKK....",
    "...KWWWWWWWWWWK...",
    "..K222222222222K..",
    "..KKKKKKKKKKKKKK..",
    "..K211111111111K..",
    ".KK11WE1111EW11KK.",
    "K1K11EE1111EE11K1K",
    ".KK11C11KK11C11KK.",
    "..K111WWWWWW111K..",
    "..K11WWWWWWWW11K..",
    "..K331111111133K..",
    "...KKKKKKKKKKKK...",
    "....KK......KK....",
    "..................",
    "..................",
    "..................",
    ".................."
)
