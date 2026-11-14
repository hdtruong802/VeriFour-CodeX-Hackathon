package com.verifour.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.DuoInteractionType
import com.verifour.app.model.HabitatTheme
import com.verifour.app.model.PLASTIC_MASCOTS
import com.verifour.app.model.PetAnimationEvent
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PetStage
import com.verifour.app.model.PlasticType
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PixelDuoStage(
    locale: AppLocale,
    petSpecies: PetSpecies,
    petStage: PetStage,
    petEvent: PetAnimationEvent,
    petEventTrigger: Int,
    bannerText: String?,
    activePlastic: PlasticType?,
    duoAction: DuoInteractionType,
    duoTriggerId: Int,
    onTapPet: () -> Unit,
    onTapPlastic: () -> Unit,
    modifier: Modifier = Modifier,
    petSkin: PetSkin = PetSkin.CLASSIC,
    habitatTheme: HabitatTheme = HabitatTheme.MEADOW
) {
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    val petOffsetX = remember { Animatable(0f) }
    val petJumpY = remember { Animatable(0f) }
    val petTilt = remember { Animatable(0f) }

    val plasticOffsetX = remember { Animatable(140f) }
    val plasticJumpY = remember { Animatable(0f) }
    val plasticScaleX = remember { Animatable(1f) }
    val plasticScaleY = remember { Animatable(1f) }
    val plasticTilt = remember { Animatable(0f) }

    val impactBurst = remember { Animatable(0f) }
    val musicNotesProgress = remember { Animatable(0f) }

    var isRunningIn by remember { mutableStateOf(false) }
    var duoHappy by remember { mutableStateOf(false) }
    var showImpactFlash by remember { mutableStateOf(false) }
    var showMusicNotes by remember { mutableStateOf(false) }
    var centerImpactScale by remember { mutableFloatStateOf(0f) }

    // Entrance & choreography animations
    LaunchedEffect(activePlastic, duoAction, duoTriggerId) {
        if (activePlastic == null) {
            duoHappy = false
            showImpactFlash = false
            showMusicNotes = false
            launch { petOffsetX.animateTo(0f, tween(280, easing = FastOutSlowInEasing)) }
            launch { petJumpY.animateTo(0f, tween(200)) }
            launch { petTilt.animateTo(0f, tween(200)) }
            return@LaunchedEffect
        }

        // Slide / run plastic mascot in from right
        if (plasticOffsetX.value > 35f || duoAction == DuoInteractionType.INTRO) {
            isRunningIn = true
            launch { petOffsetX.animateTo(-42f, tween(360, easing = FastOutSlowInEasing)) }
            plasticOffsetX.snapTo(150f)
            plasticOffsetX.animateTo(44f, tween(480, easing = FastOutSlowInEasing))
            isRunningIn = false
        } else {
            launch { petOffsetX.animateTo(-42f, tween(220)) }
            launch { plasticOffsetX.animateTo(44f, tween(220)) }
        }

        when (duoAction) {
            DuoInteractionType.INTRO -> {
                duoHappy = true
                plasticScaleX.animateTo(1.16f, tween(80))
                plasticScaleY.animateTo(0.86f, tween(80))
                launch { plasticScaleX.animateTo(1f, tween(140)) }
                launch { plasticScaleY.animateTo(1f, tween(140)) }
                plasticJumpY.animateTo(-18f, tween(160, easing = FastOutSlowInEasing))
                plasticJumpY.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
                delay(450.milliseconds)
                duoHappy = false
            }

            DuoInteractionType.HIGH_FIVE -> {
                duoHappy = true
                // Both dash toward center
                launch { petOffsetX.animateTo(-18f, tween(200, easing = FastOutSlowInEasing)) }
                plasticOffsetX.animateTo(18f, tween(200, easing = FastOutSlowInEasing))

                // Crouch anticipation
                launch { plasticScaleX.animateTo(1.16f, tween(90)) }
                launch { plasticScaleY.animateTo(0.84f, tween(90)) }
                delay(90.milliseconds)

                // Leap up together & tilt inward for HIGH FIVE!
                launch { plasticScaleX.animateTo(0.95f, tween(140)) }
                launch { plasticScaleY.animateTo(1.12f, tween(140)) }
                launch { petTilt.animateTo(12f, tween(140)) }
                launch { plasticTilt.animateTo(-12f, tween(140)) }
                launch { petJumpY.animateTo(-28f, tween(170, easing = FastOutSlowInEasing)) }
                plasticJumpY.animateTo(-28f, tween(170, easing = FastOutSlowInEasing))

                // Pixel High-Five Impact Flash!
                showImpactFlash = true
                centerImpactScale = 1f
                launch {
                    impactBurst.snapTo(0f)
                    impactBurst.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
                    showImpactFlash = false
                }

                delay(90.milliseconds)
                // Land & recoil back to duo spots
                launch { petTilt.animateTo(0f, tween(180)) }
                launch { plasticTilt.animateTo(0f, tween(180)) }
                launch { petJumpY.animateTo(0f, tween(200)) }
                launch { plasticJumpY.animateTo(0f, tween(200)) }
                launch { plasticScaleX.animateTo(1f, tween(160)) }
                launch { plasticScaleY.animateTo(1f, tween(160)) }
                launch { petOffsetX.animateTo(-42f, tween(260, easing = FastOutSlowInEasing)) }
                plasticOffsetX.animateTo(44f, tween(260, easing = FastOutSlowInEasing))
                delay(500.milliseconds)
                duoHappy = false
            }

            DuoInteractionType.DANCE -> {
                duoHappy = true
                showMusicNotes = true
                launch {
                    musicNotesProgress.snapTo(0f)
                    musicNotesProgress.animateTo(1f, tween(1800))
                    showMusicNotes = false
                }
                repeat(4) { beat ->
                    val dir = if (beat % 2 == 0) 1f else -1f
                    launch { petTilt.animateTo(13f * dir, tween(160)) }
                    launch { plasticTilt.animateTo(-13f * dir, tween(160)) }
                    launch { petJumpY.animateTo(-16f, tween(160, easing = FastOutSlowInEasing)) }
                    plasticJumpY.animateTo(-16f, tween(160, easing = FastOutSlowInEasing))
                    launch { petJumpY.animateTo(0f, tween(160, easing = FastOutSlowInEasing)) }
                    plasticJumpY.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
                }
                launch { petTilt.animateTo(0f, tween(120)) }
                plasticTilt.animateTo(0f, tween(120))
                duoHappy = false
            }
        }
    }

    val mascotMeta = activePlastic?.let { PLASTIC_MASCOTS[it] }
    val speechBubbleText = when {
        mascotMeta == null -> null
        duoAction == DuoInteractionType.HIGH_FIVE ->
            t("mascotHighFiveBubble", mapOf("name" to t(mascotMeta.nameKey)))
        duoAction == DuoInteractionType.DANCE ->
            t("mascotDanceBubble", mapOf("name" to t(mascotMeta.nameKey)))
        else -> t(mascotMeta.introKey)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(habitatTheme.skyHex))
                .border(1.5.dp, EcoBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Pixel habitat backdrop + impact burst / music notes
            Canvas(modifier = Modifier.fillMaxSize()) {
                val px = (size.minDimension / 16f).coerceAtLeast(4f)
                drawRect(
                    color = Color(habitatTheme.groundHex).copy(alpha = 0.25f),
                    topLeft = Offset(0f, size.height - px * 3.2f),
                    size = Size(size.width, px * 3.2f)
                )
                drawRect(
                    color = Color(habitatTheme.cloudHex).copy(alpha = 0.72f),
                    topLeft = Offset(px * 2f, px * 1.6f),
                    size = Size(px * 3f, px)
                )
                drawRect(
                    color = Color(habitatTheme.cloudHex).copy(alpha = 0.62f),
                    topLeft = Offset(size.width - px * 6f, px * 2.4f),
                    size = Size(px * 2.5f, px)
                )

                val center = Offset(size.width / 2f, size.height * 0.44f)

                if (showImpactFlash) {
                    val p = impactBurst.value
                    val alpha = (1f - p).coerceIn(0f, 1f)
                    val starColors = listOf(EcoSunshine, EcoPrimaryGlow, EcoCoral, Color.White)
                    for (i in 0 until 10) {
                        val angle = (2f * PI.toFloat() / 10f) * i
                        val dist = 16f + p * 72f
                        val sx = center.x + cos(angle) * dist
                        val sy = center.y + sin(angle) * dist
                        drawRect(
                            color = starColors[i % starColors.size].copy(alpha = alpha),
                            topLeft = Offset(sx - px * 0.5f, sy - px * 0.5f),
                            size = Size(px, px)
                        )
                    }
                }

                if (showMusicNotes) {
                    val mp = musicNotesProgress.value
                    val noteAlpha = (1f - mp).coerceIn(0f, 1f)
                    val noteColors = listOf(EcoPrimary, EcoOcean, EcoSunshine, EcoCoral)
                    for (i in 0 until 6) {
                        val nx = size.width * (0.22f + i * 0.11f) + sin(mp * 8f + i) * 10f
                        val ny = size.height * 0.46f - mp * (55f + (i % 3) * 18f)
                        drawRect(
                            color = noteColors[i % noteColors.size].copy(alpha = noteAlpha),
                            topLeft = Offset(nx, ny),
                            size = Size(px * 0.8f, px * 1.1f)
                        )
                        drawRect(
                            color = noteColors[i % noteColors.size].copy(alpha = noteAlpha),
                            topLeft = Offset(nx + px * 0.6f, ny - px * 0.4f),
                            size = Size(px * 0.9f, px * 0.5f)
                        )
                    }
                }
            }

            // Your Pet Sprite
            PixelPetSprite(
                species = petSpecies,
                stage = petStage,
                skin = petSkin,
                event = petEvent,
                eventTriggerId = petEventTrigger,
                eventBannerText = bannerText,
                spriteSize = 98.dp,
                onTap = onTapPet,
                modifier = Modifier.graphicsLayer {
                    translationX = petOffsetX.value * density
                    translationY = petJumpY.value * density
                    rotationZ = petTilt.value
                }
            )

            // Plastic Mascot Sprite (runs in from right!)
            if (activePlastic != null) {
                PixelPlasticMascotSprite(
                    plasticType = activePlastic,
                    spriteSize = 78.dp,
                    externalJumpY = plasticJumpY.value,
                    externalScaleX = plasticScaleX.value,
                    externalScaleY = plasticScaleY.value,
                    externalTiltDeg = plasticTilt.value,
                    happyOverride = duoHappy,
                    fastBob = isRunningIn,
                    onTap = onTapPlastic,
                    modifier = Modifier.graphicsLayer {
                        translationX = plasticOffsetX.value * density
                    }
                )
            }

            // Center High-Five Badge
            androidx.compose.animation.AnimatedVisibility(
                visible = showImpactFlash && centerImpactScale > 0f,
                enter = fadeIn(tween(80)),
                exit = fadeOut(tween(220)),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EcoSunshine)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "🙌 HIGH FIVE!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EcoForeground
                    )
                }
            }
        }

        // Speech bubble from Plastic Mascot
        if (speechBubbleText != null && mascotMeta != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(mascotMeta.secondaryHex).copy(alpha = 0.25f))
                    .border(
                        1.dp,
                        Color(mascotMeta.primaryHex).copy(alpha = 0.35f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Column {
                    Text(
                        text = "${activePlastic.emoji} ${t(mascotMeta.nameKey)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(mascotMeta.primaryHex)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = speechBubbleText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = EcoForeground
                    )
                }
            }
        }
    }
}

@Composable
fun PlasticMascotSelectorBar(
    locale: AppLocale,
    activePlastic: PlasticType?,
    onSummonPlastic: (PlasticType) -> Unit,
    onHighFive: () -> Unit,
    onDance: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = t("mascotDuoTitle"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EcoMutedForeground
            )
            if (activePlastic != null) {
                Text(
                    text = t("mascotDismiss"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoCoral,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlasticType.entries.forEach { plastic ->
                val meta = PLASTIC_MASCOTS.getValue(plastic)
                val selected = activePlastic == plastic
                Surface(
                    shape = CircleShape,
                    color = if (selected) Color(meta.primaryHex) else EcoSecondary,
                    border = BorderStroke(
                        1.dp,
                        if (selected) Color(meta.primaryHex) else EcoBorder
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSummonPlastic(plastic) }
                ) {
                    Text(
                        text = "${plastic.emoji} ${plastic.code}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else EcoForeground,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (activePlastic != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onHighFive,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EcoPrimary.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "🙌 ${t("mascotHighFive")}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onDance,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EcoOcean.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "💃 ${t("mascotDance")}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoOcean,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun JudgeDemoPanel(
    locale: AppLocale,
    isAutoPlaying: Boolean,
    demoStepLabel: String?,
    canUpgrade: Boolean,
    onToggleAutoPlay: () -> Unit,
    onDemoHighFive: () -> Unit,
    onDemoDance: () -> Unit,
    onDemoAddPoints: () -> Unit,
    onDemoUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoForeground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎬 ${t("judgeDemoTitle")}",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EcoPrimaryGlow
            )
            if (!demoStepLabel.isNullOrBlank()) {
                Text(
                    text = demoStepLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoSunshine
                )
            }
        }

        Button(
            onClick = onToggleAutoPlay,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAutoPlaying) EcoCoral else EcoPrimary,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = if (isAutoPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isAutoPlaying) "${t("judgeDemoRunning")} (${t("judgeDemoStop")})" else t("judgeDemoAutoPlay"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DemoQuickChip(
                label = "🙌 ${t("mascotHighFive")}",
                bg = Color.White.copy(alpha = 0.14f),
                fg = Color.White,
                onClick = onDemoHighFive,
                modifier = Modifier.weight(1f)
            )
            DemoQuickChip(
                label = "💃 ${t("mascotDance")}",
                bg = Color.White.copy(alpha = 0.14f),
                fg = Color.White,
                onClick = onDemoDance,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DemoQuickChip(
                label = "✨ ${t("judgeDemoAddPoints")}",
                bg = EcoPrimary.copy(alpha = 0.35f),
                fg = EcoPrimaryGlow,
                onClick = onDemoAddPoints,
                modifier = Modifier.weight(1f)
            )
            DemoQuickChip(
                label = "👑 ${t("judgeDemoLevelUp")}",
                bg = if (canUpgrade) EcoSunshine else Color.White.copy(alpha = 0.14f),
                fg = if (canUpgrade) EcoForeground else EcoSunshine,
                onClick = onDemoUpgrade,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DemoQuickChip(
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (label.startsWith("👑")) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label.removePrefix("👑 "),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
