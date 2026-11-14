package com.verifour.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.model.PetAnimationEvent
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PetStage
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoLeaf
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSunshine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private data class PixelParticle(
    val angleRad: Float,
    val speed: Float,
    val color: Color,
    val isHeart: Boolean
)

@Composable
fun PixelPetSprite(
    species: PetSpecies,
    stage: PetStage,
    modifier: Modifier = Modifier,
    skin: PetSkin = PetSkin.CLASSIC,
    event: PetAnimationEvent = PetAnimationEvent.IDLE,
    eventTriggerId: Int = 0,
    eventBannerText: String? = null,
    spriteSize: Dp = 108.dp,
    onTap: (() -> Unit)? = null
) {
    val infinite = rememberInfiniteTransition(label = "pet_idle_bob")

    // Smooth rhythmic "nhún nhún" phase 0f..1f
    val bobPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 760, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob_phase"
    )

    // Orbiting sparkle pulse for higher stages
    val sparklePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_phase"
    )

    // Blinking every ~3.4s
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3100.milliseconds)
            isBlinking = true
            delay(140.milliseconds)
            isBlinking = false
        }
    }

    // Action animation drivers
    val actionJumpY = remember { Animatable(0f) }
    val actionScaleX = remember { Animatable(1f) }
    val actionScaleY = remember { Animatable(1f) }
    val actionRotation = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }

    var activeEvent by remember { mutableStateOf(PetAnimationEvent.IDLE) }
    var showBanner by remember { mutableStateOf(false) }
    var activeBanner by remember { mutableStateOf<String?>(null) }
    var happyEyes by remember { mutableStateOf(false) }

    val particles = remember(eventTriggerId, activeEvent) {
        val count = when (activeEvent) {
            PetAnimationEvent.LEVEL_UP -> 20
            PetAnimationEvent.POINTS_ADDED -> 12
            PetAnimationEvent.HAPPY_TAP -> 6
            PetAnimationEvent.IDLE -> 0
        }
        val palette = when (activeEvent) {
            PetAnimationEvent.LEVEL_UP -> listOf(EcoSunshine, EcoPrimaryGlow, EcoCoral, EcoOcean, Color(0xFFFFF099))
            PetAnimationEvent.POINTS_ADDED -> listOf(EcoPrimary, EcoPrimaryGlow, EcoSunshine, EcoLeaf)
            else -> listOf(EcoCoral, Color(0xFFFF8FA3), EcoPrimaryGlow)
        }
        List(count) { idx ->
            val angle = ((2f * PI.toFloat()) / count.coerceAtLeast(1)) * idx + (idx % 3) * 0.14f
            PixelParticle(
                angleRad = angle,
                speed = 0.72f + (idx % 4) * 0.18f,
                color = palette[idx % palette.size],
                isHeart = activeEvent == PetAnimationEvent.HAPPY_TAP || (idx % 3 == 0)
            )
        }
    }

    LaunchedEffect(eventTriggerId) {
        if (eventTriggerId <= 0 || event == PetAnimationEvent.IDLE) return@LaunchedEffect
        activeEvent = event
        happyEyes = true
        activeBanner = eventBannerText
        showBanner = !eventBannerText.isNullOrBlank()

        when (event) {
            PetAnimationEvent.HAPPY_TAP -> {
                launch {
                    particleProgress.snapTo(0f)
                    particleProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                }
                actionScaleX.animateTo(1.18f, tween(90))
                actionScaleY.animateTo(0.84f, tween(90))
                launch { actionScaleX.animateTo(0.94f, tween(130)) }
                launch { actionScaleY.animateTo(1.12f, tween(130)) }
                actionJumpY.animateTo(-18f, tween(160, easing = FastOutSlowInEasing))
                actionJumpY.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                actionScaleX.animateTo(1f, tween(120))
                actionScaleY.animateTo(1f, tween(120))
                delay(450.milliseconds)
            }

            PetAnimationEvent.POINTS_ADDED -> {
                launch {
                    particleProgress.snapTo(0f)
                    particleProgress.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
                }
                // Joyful double-hop + wiggle
                repeat(2) { hopIndex ->
                    actionScaleX.animateTo(1.16f, tween(70))
                    actionScaleY.animateTo(0.85f, tween(70))
                    launch { actionScaleX.animateTo(0.93f, tween(120)) }
                    launch { actionScaleY.animateTo(1.14f, tween(120)) }
                    launch { actionRotation.animateTo(if (hopIndex == 0) -8f else 8f, tween(120)) }
                    actionJumpY.animateTo(-24f, tween(160, easing = FastOutSlowInEasing))
                    actionJumpY.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
                    launch { actionRotation.animateTo(0f, tween(110)) }
                    actionScaleX.animateTo(1.12f, tween(70))
                    actionScaleY.animateTo(0.88f, tween(70))
                    actionScaleX.animateTo(1f, tween(90))
                    actionScaleY.animateTo(1f, tween(90))
                }
                delay(650.milliseconds)
            }

            PetAnimationEvent.LEVEL_UP -> {
                // Crouch charge-up
                actionScaleX.animateTo(1.24f, tween(160))
                actionScaleY.animateTo(0.76f, tween(160))
                // Mega jump + shockwave ring + confetti burst
                launch {
                    ringProgress.snapTo(0f)
                    ringProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
                }
                launch {
                    particleProgress.snapTo(0f)
                    particleProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
                }
                launch { actionScaleX.animateTo(0.90f, tween(150)) }
                launch { actionScaleY.animateTo(1.20f, tween(150)) }
                launch {
                    actionRotation.animateTo(-10f, tween(110))
                    actionRotation.animateTo(10f, tween(140))
                    actionRotation.animateTo(0f, tween(120))
                }
                actionJumpY.animateTo(-36f, tween(240, easing = FastOutSlowInEasing))
                actionJumpY.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                // Landing squash & settle
                actionScaleX.animateTo(1.16f, tween(90))
                actionScaleY.animateTo(0.84f, tween(90))
                launch { actionScaleX.animateTo(1f, tween(140)) }
                actionScaleY.animateTo(1f, tween(140))
                delay(1200.milliseconds)
            }
        }

        happyEyes = false
        showBanner = false
        activeEvent = PetAnimationEvent.IDLE
        ringProgress.snapTo(0f)
        particleProgress.snapTo(0f)
    }

    // Idle "nhún nhún" physics
    val idleOffsetY = -bobPhase * 6.5f
    val idleScaleX = 1.045f - bobPhase * 0.085f
    val idleScaleY = 0.945f + bobPhase * 0.10f
    val spriteFrame = if (bobPhase >= 0.5f) 1 else 0

    // Shadow width shrinks when pet is higher in the air
    val totalHeightFactor = ((-idleOffsetY - actionJumpY.value) / 42f).coerceIn(0f, 1f)
    val shadowScale = 1f - totalHeightFactor * 0.42f

    var tapInteractionDummy by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .then(
                if (onTap != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        tapInteractionDummy++
                        onTap()
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(spriteSize),
                contentAlignment = Alignment.Center
            ) {
                // Level-up expanding pixel shockwave ring + particles
                if (activeEvent != PetAnimationEvent.IDLE || ringProgress.value > 0f || stage == PetStage.LEGEND) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pxSize = (size.minDimension / 24f).coerceAtLeast(3f)

                        if (ringProgress.value in 0.01f..0.99f) {
                            val r = size.minDimension * (0.25f + ringProgress.value * 0.48f)
                            drawCircle(
                                color = EcoSunshine.copy(alpha = (1f - ringProgress.value) * 0.85f),
                                radius = r,
                                center = center,
                                style = Stroke(width = pxSize * 1.4f)
                            )
                        }

                        if (particleProgress.value in 0.01f..0.99f) {
                            val p = particleProgress.value
                            val alpha = (1f - p).coerceIn(0f, 1f)
                            val radiusBase = size.minDimension * 0.45f
                            particles.forEach { pt ->
                                val dist = radiusBase * pt.speed * p
                                val px = center.x + cos(pt.angleRad) * dist
                                val py = center.y + sin(pt.angleRad) * dist - p * 18f
                                drawRect(
                                    color = pt.color.copy(alpha = alpha),
                                    topLeft = Offset(px - pxSize / 2f, py - pxSize / 2f),
                                    size = Size(pxSize, pxSize)
                                )
                                if (pt.isHeart) {
                                    drawRect(
                                        color = pt.color.copy(alpha = alpha * 0.7f),
                                        topLeft = Offset(px + pxSize * 0.7f, py - pxSize),
                                        size = Size(pxSize * 0.75f, pxSize * 0.75f)
                                    )
                                }
                            }
                        }

                        // Subtle orbiting pixel stars for Legend stage
                        if (stage == PetStage.LEGEND && activeEvent == PetAnimationEvent.IDLE) {
                            for (i in 0 until 3) {
                                val a = sparklePhase + i * (2f * PI.toFloat() / 3f)
                                val sx = center.x + cos(a) * size.minDimension * 0.44f
                                val sy = center.y + sin(a) * size.minDimension * 0.34f - size.height * 0.08f
                                drawRect(
                                    color = EcoSunshine.copy(alpha = 0.85f),
                                    topLeft = Offset(sx, sy),
                                    size = Size(pxSize * 0.9f, pxSize * 0.9f)
                                )
                            }
                        }
                    }
                }

                // Pixel Pet Body Canvas with nhún-nhún squash & stretch
                Canvas(
                    modifier = Modifier
                        .size(spriteSize * 0.86f)
                        .graphicsLayer {
                            translationY = (idleOffsetY + actionJumpY.value) * density * 0.85f
                            scaleX = idleScaleX * actionScaleX.value
                            scaleY = idleScaleY * actionScaleY.value
                            rotationZ = actionRotation.value
                        }
                ) {
                    val grid = buildPetGrid(
                        species = species,
                        stage = stage,
                        frame = spriteFrame,
                        blinking = isBlinking && !happyEyes,
                        happy = happyEyes
                    )
                    val rows = grid.size
                    val cols = grid[0].length
                    val cell = floor(size.minDimension / rows.toFloat()).coerceAtLeast(1f)
                    val totalW = cell * cols
                    val totalH = cell * rows
                    val offsetX = (size.width - totalW) / 2f
                    val offsetY = (size.height - totalH) / 2f

                    val primaryColor = Color(skin.primaryOverrideHex ?: species.primaryHex)
                    val secondaryColor = Color(skin.secondaryOverrideHex ?: species.secondaryHex)
                    val shadowColor = Color(
                        red = (primaryColor.red * 0.72f).coerceIn(0f, 1f),
                        green = (primaryColor.green * 0.72f).coerceIn(0f, 1f),
                        blue = (primaryColor.blue * 0.72f).coerceIn(0f, 1f),
                        alpha = 1f
                    )
                    val badgeColor = if (skin != PetSkin.CLASSIC) {
                        Color(skin.accentHex)
                    } else {
                        when (species) {
                            PetSpecies.SPROUTLING -> EcoSunshine
                            PetSpecies.TURTLE -> EcoCoral
                            PetSpecies.FOX -> EcoPrimary
                            PetSpecies.WHALE -> EcoSunshine
                        }
                    }

                    for (r in 0 until rows) {
                        val rowStr = grid[r]
                        for (c in 0 until cols) {
                            val ch = rowStr[c]
                            if (ch == '.') continue
                            val color = when (ch) {
                                'K' -> Color(0xFF102A1F)
                                'E' -> Color(0xFF102A1F)
                                '1' -> primaryColor
                                '2' -> secondaryColor
                                '3' -> shadowColor
                                'W' -> Color(0xFFFFFFFF)
                                'C' -> Color(0xFFFF8FA3)
                                'Y' -> Color(0xFFF5B82E)
                                'G' -> Color(0xFFFFF099)
                                'L' -> EcoPrimary
                                'M' -> Color(0xFF73EB9E)
                                'B' -> badgeColor
                                else -> primaryColor
                            }
                            drawRect(
                                color = color,
                                topLeft = Offset(offsetX + c * cell, offsetY + r * cell),
                                size = Size(cell + 0.4f, cell + 0.4f)
                            )
                        }
                    }

                    // Pixel Skin Accessories
                    fun drawSkinPixel(r: Int, c: Int, color: Color) {
                        drawRect(
                            color = color,
                            topLeft = Offset(offsetX + c * cell, offsetY + r * cell),
                            size = Size(cell + 0.4f, cell + 0.4f)
                        )
                    }

                    when (skin) {
                        PetSkin.CLASSIC -> Unit
                        PetSkin.SAKURA -> {
                            val petal = Color(0xFFFF8CB8)
                            val center = Color(0xFFFFF099)
                            drawSkinPixel(3, 12, petal)
                            drawSkinPixel(4, 11, petal)
                            drawSkinPixel(4, 13, petal)
                            drawSkinPixel(5, 12, petal)
                            drawSkinPixel(4, 12, center)
                        }
                        PetSkin.SOLAR_TECH -> {
                            val visor = Color(0xFF38BDF8)
                            val glow = Color(0xFFE0F2FE)
                            for (c in 5..12) {
                                drawSkinPixel(9, c, visor)
                            }
                            drawSkinPixel(9, 6, glow)
                            drawSkinPixel(9, 10, glow)
                        }
                        PetSkin.GOLDEN_LEAF -> {
                            val gold = Color(0xFFFFD700)
                            val gem = Color(0xFF20A064)
                            drawSkinPixel(1, 6, gold)
                            drawSkinPixel(1, 9, gold)
                            drawSkinPixel(1, 12, gold)
                            for (c in 6..12) {
                                drawSkinPixel(2, c, gold)
                            }
                            drawSkinPixel(2, 9, gem)
                        }
                        PetSkin.GALAXY_ECO -> {
                            val star = Color(0xFFFDE047)
                            val mint = Color(0xFF34D399)
                            drawSkinPixel(2, 3, star)
                            drawSkinPixel(3, 14, mint)
                            drawSkinPixel(14, 2, mint)
                            drawSkinPixel(13, 15, star)
                        }
                    }
                }

                // Floating action badge (+XP / LEVEL UP / heart)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBanner && !activeBanner.isNullOrBlank(),
                    enter = fadeIn(tween(150)) + slideInVertically { it / 2 },
                    exit = fadeOut(tween(250)) + slideOutVertically { -it / 2 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp)
                ) {
                    val pillColor = when (activeEvent) {
                        PetAnimationEvent.LEVEL_UP -> EcoSunshine
                        PetAnimationEvent.POINTS_ADDED -> EcoPrimary
                        else -> EcoCoral
                    }
                    val textColor = if (activeEvent == PetAnimationEvent.LEVEL_UP) {
                        EcoForeground
                    } else {
                        Color.White
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(pillColor)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = activeBanner.orEmpty(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            // Pixel ground shadow that shrinks & expands with the bounce
            Canvas(
                modifier = Modifier
                    .size(width = spriteSize * 0.56f, height = 8.dp)
                    .graphicsLayer { scaleX = shadowScale }
            ) {
                drawRect(
                    color = EcoForeground.copy(alpha = 0.16f),
                    topLeft = Offset(size.width * 0.1f, 2f),
                    size = Size(size.width * 0.8f, size.height * 0.6f)
                )
            }
        }
    }
}

private fun buildPetGrid(
    species: PetSpecies,
    stage: PetStage,
    frame: Int,
    blinking: Boolean,
    happy: Boolean
): List<String> {
    val base = when (species) {
        PetSpecies.SPROUTLING -> if (frame == 0) SPROUTLING_F0 else SPROUTLING_F1
        PetSpecies.TURTLE -> if (frame == 0) TURTLE_F0 else TURTLE_F1
        PetSpecies.FOX -> if (frame == 0) FOX_F0 else FOX_F1
        PetSpecies.WHALE -> if (frame == 0) WHALE_F0 else WHALE_F1
    }.map { it.toCharArray() }

    // Blinking or Happy Eyes adjustment
    if (blinking || happy) {
        for (r in 5..11) {
            for (c in 2..15) {
                if (base[r][c] == 'W' && c in 4..13 && r in 6..10) {
                    // Replace top white eye-glint pixel only when surrounding row has E
                    val belowHasE = base[r + 1][c] == 'E'
                    if (belowHasE) {
                        base[r][c] = base[r - 1][c].takeIf { it in listOf('1', '2') } ?: '1'
                    }
                }
            }
        }
        if (happy) {
            // Happy "^ ^" pixel eyes look: brighten center eye pixels slightly
            for (r in 6..10) {
                for (c in 3..14) {
                    if (base[r][c] == 'E' && r > 6 && base[r - 1][c] == 'E') {
                        base[r][c] = '1'
                    }
                }
            }
        }
    }

    // Stage 2+ (Sprout): Cute collar / eco gem accent
    if (stage.minLevel >= PetStage.SPROUT.minLevel) {
        base[11][8] = 'B'
        base[11][9] = 'B'
        if (species == PetSpecies.SPROUTLING) {
            base[2][8] = 'Y'
            base[2][9] = 'G'
        }
    }

    // Stage 3+ (Guardian): Cute pixel eco-wings on sides
    if (stage.minLevel >= PetStage.GUARDIAN.minLevel) {
        val wingY = if (frame == 0) 9 else 8
        base[wingY][0] = 'M'
        base[wingY][1] = 'L'
        base[wingY + 1][1] = 'M'
        base[wingY][17] = 'M'
        base[wingY][16] = 'L'
        base[wingY + 1][16] = 'M'
    }

    // Stage 4 (Legend): Golden Pixel Crown + side sparkles!
    if (stage == PetStage.LEGEND) {
        base[0][6] = 'Y'
        base[0][8] = 'G'
        base[0][9] = 'G'
        base[0][11] = 'Y'
        base[1][6] = 'Y'
        base[1][7] = 'G'
        base[1][8] = 'Y'
        base[1][9] = 'Y'
        base[1][10] = 'G'
        base[1][11] = 'Y'
        if (frame == 1) {
            base[3][1] = 'G'
            base[3][16] = 'G'
        } else {
            base[4][1] = 'Y'
            base[4][16] = 'Y'
        }
    }

    return base.map { String(it) }
}

// 18x18 pixel art matrices (exact 18 chars per row)
private val SPROUTLING_F0 = listOf(
    "..................",
    ".....LL....LL.....",
    "....LMML..LMML....",
    ".....KKLLLLKK.....",
    "........KK........",
    ".....KKKKKKKK.....",
    "....K22211111K....",
    "...K2211111111K...",
    "..K211111111111K..",
    "..K11WE1111EW11K..",
    "..K11EE1111EE11K..",
    "..K1C111KK111C1K..",
    "..K111WWWWWW111K..",
    "..K31WWWWWWWW13K..",
    "...K3311111133K...",
    "....KKKKKKKKKK....",
    ".....KK....KK.....",
    ".................."
)

private val SPROUTLING_F1 = listOf(
    "....LL......LL....",
    "...LMML....LMML...",
    "....KKLLLLLLKK....",
    "........KK........",
    ".....KKKKKKKK.....",
    "....K22211111K....",
    "...K2211111111K...",
    "..K211111111111K..",
    "..K11WE1111EW11K..",
    "..K11EE1111EE11K..",
    "..K1C111KK111C1K..",
    "..K111WWWWWW111K..",
    "..K31WWWWWWWW13K..",
    "...K3311111133K...",
    "....KKKKKKKKKK....",
    "....KK......KK....",
    "..................",
    ".................."
)

private val TURTLE_F0 = listOf(
    "..................",
    "..................",
    "..................",
    "......KKKKKK......",
    "....KK222111KK....",
    "...K2211111111K...",
    "...K1WE1111EW1K...",
    "...K1EE1111EE1K...",
    "...K1C11KK11C1K...",
    "..KLLLKKKKKKLLLK..",
    ".K11KLLMMMMLLK11K.",
    ".K1KLMMLYYLMMLK1K.",
    "..KKLLLLLLLLLLKK..",
    "...K11WWWWWW11K...",
    "....KK333333KK....",
    ".....KK....KK.....",
    "..................",
    ".................."
)

private val TURTLE_F1 = listOf(
    "..................",
    "..................",
    "......KKKKKK......",
    "....KK222111KK....",
    "...K2211111111K...",
    "...K1WE1111EW1K...",
    "...K1EE1111EE1K...",
    "...K1C11KK11C1K...",
    "..KLLLKKKKKKLLLK..",
    ".K11KLLMMMMLLK11K.",
    ".K1KLMMLYYLMMLK1K.",
    "..KKLLLLLLLLLLKK..",
    "...K11WWWWWW11K...",
    "....KK333333KK....",
    "....KK......KK....",
    "..................",
    "..................",
    ".................."
)

private val FOX_F0 = listOf(
    "..................",
    "..KK..........KK..",
    "..K1K........K1K..",
    "..K1WK......KW1K..",
    "..K1WWKKKKKKWW1K..",
    "..K211111111111K..",
    "..K11WE1111EW11K..",
    "..K11EE1111EE11K..",
    "..K1C1WWKKWW1C1K..",
    "...KK1WWWWWW1KK...",
    "....K11WWWW11K..KK",
    "...K111WWWW11KK1WK",
    "...K111WWWW11K11WK",
    "...K311111111KK1K.",
    "....KKKKKKKKKK.KK.",
    ".....KK....KK.....",
    "..................",
    ".................."
)

private val FOX_F1 = listOf(
    "..KK..........KK..",
    "..K1K........K1K..",
    "..K1WK......KW1K..",
    "..K1WWKKKKKKWW1K..",
    "..K211111111111K..",
    "..K11WE1111EW11K..",
    "..K11EE1111EE11K..",
    "..K1C1WWKKWW1C1K..",
    "...KK1WWWWWW1KK...",
    "....K11WWWW11K.KK.",
    "...K111WWWW11KK1WK",
    "...K111WWWW11K11WK",
    "...K311111111KK1K.",
    "....KKKKKKKKKK.KK.",
    "....KK......KK....",
    "..................",
    "..................",
    ".................."
)

private val WHALE_F0 = listOf(
    ".......W..W.......",
    "......W.WW.W......",
    "........WW........",
    ".....KKKKKKKK.....",
    "...KK22222111KK...",
    "..K221111111111K..",
    "..K111111111111K..",
    "..K1WE111111EW1K..",
    "..K1EE111111EE1K..",
    "..K1C111KK111C1K..",
    ".KK11WWWWWWWW11KK.",
    "K211WWWWWWWWWW112K",
    ".KK1WWWWWWWWWW1KK.",
    "..K33WWWWWWWW33K..",
    "...KK33333333KK...",
    ".....KKKKKKKK.....",
    "..................",
    ".................."
)

private val WHALE_F1 = listOf(
    "......W....W......",
    ".....W..WW..W.....",
    "........WW........",
    ".....KKKKKKKK.....",
    "...KK22222111KK...",
    "..K221111111111K..",
    "..K111111111111K..",
    "..K1WE111111EW1K..",
    "..K1EE111111EE1K..",
    "..K1C111KK111C1K..",
    "KK111WWWWWWWW111KK",
    "K211WWWWWWWWWW112K",
    ".KK1WWWWWWWWWW1KK.",
    "..K33WWWWWWWW33K..",
    "...KK33333333KK...",
    ".....KKKKKKKK.....",
    "..................",
    ".................."
)
