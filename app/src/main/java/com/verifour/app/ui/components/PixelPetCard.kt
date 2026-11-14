package com.verifour.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.DuoInteractionType
import com.verifour.app.model.PetAnimationEvent
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetProgression
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PetStage
import com.verifour.app.model.PlasticType
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoLeaf
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private enum class SanctuaryTab {
    SPECIES,
    EVOLUTIONS,
    STORY,
    SKINS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelPetHomeCard(
    locale: AppLocale,
    studentPoints: Int,
    petState: PetCompanionState,
    personalization: UserPersonalizationState,
    petReactionKey: String?,
    petEvent: PetAnimationEvent,
    petEventTrigger: Int,
    petEventPointsDelta: Int,
    petEventNewLevel: Int,
    onPatPet: () -> Unit,
    onFeedPet: () -> Unit,
    onUpgradePet: () -> Unit,
    onDemoAddPoints: () -> Unit,
    onDemoUpgradePet: () -> Unit,
    onSelectSpecies: (PetSpecies) -> Unit,
    onSelectSkin: (PetSkin) -> Unit,
    modifier: Modifier = Modifier
) {
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var showSanctuary by remember { mutableStateOf(false) }
    var sanctuaryInitialTab by remember { mutableStateOf(SanctuaryTab.SPECIES) }

    var activePlastic by remember { mutableStateOf<PlasticType?>(null) }
    var duoAction by remember { mutableStateOf(DuoInteractionType.INTRO) }
    var duoTriggerId by remember { mutableIntStateOf(0) }
    var showDemoPanel by remember { mutableStateOf(false) }
    var isAutoPlaying by remember { mutableStateOf(false) }
    var demoStepLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isAutoPlaying) {
        if (!isAutoPlaying) {
            demoStepLabel = null
            return@LaunchedEffect
        }
        demoStepLabel = "1/4 · PET"
        activePlastic = PlasticType.PET
        duoAction = DuoInteractionType.INTRO
        duoTriggerId++
        delay(1700.milliseconds)

        if (!isAutoPlaying) return@LaunchedEffect
        demoStepLabel = "2/4 · High-Five +25 XP"
        duoAction = DuoInteractionType.HIGH_FIVE
        duoTriggerId++
        onDemoAddPoints()
        delay(1900.milliseconds)

        if (!isAutoPlaying) return@LaunchedEffect
        demoStepLabel = "3/4 · PP Dance"
        activePlastic = PlasticType.PP
        duoAction = DuoInteractionType.DANCE
        duoTriggerId++
        delay(2200.milliseconds)

        if (!isAutoPlaying) return@LaunchedEffect
        demoStepLabel = "4/4 · Level Up!"
        onDemoUpgradePet()
        duoAction = DuoInteractionType.HIGH_FIVE
        duoTriggerId++
        delay(1800.milliseconds)

        isAutoPlaying = false
        demoStepLabel = null
    }

    val stage = PetStage.fromLevel(petState.level)
    val canUpgrade = PetProgression.canUpgrade(studentPoints, petState)
    val (earnedInLevel, span, progress) = PetProgression.progressInCurrentLevel(studentPoints, petState)
    val remainingXp = (span - earnedInLevel).coerceAtLeast(0)
    val personality = personalization.resolvedPersonality(petState.species)
    val activeSkin = personalization.resolvedSkin()
    val habitatTheme = personalization.resolvedHabitat()
    val displayPetName = personalization.petNickname.ifBlank { t(petState.species.nameKey) }

    val bannerText = when (petEvent) {
        PetAnimationEvent.POINTS_ADDED -> "+$petEventPointsDelta XP"
        PetAnimationEvent.LEVEL_UP -> "LV.$petEventNewLevel!"
        PetAnimationEvent.HAPPY_TAP -> "♥"
        PetAnimationEvent.IDLE -> null
    }

    val statusSpeech = when {
        !petReactionKey.isNullOrBlank() -> t(petReactionKey)
        petEventTrigger > 0 && petEvent == PetAnimationEvent.LEVEL_UP ->
            t("petMsgLevelUp", mapOf("level" to petEventNewLevel))
        petEventTrigger > 0 && petEvent == PetAnimationEvent.POINTS_ADDED ->
            t("petMsgPoints", mapOf("points" to petEventPointsDelta))
        petEventTrigger > 0 && petEvent == PetAnimationEvent.HAPPY_TAP ->
            t("petMsgTap")
        else -> t("petTapHint")
    }

    val pulseTransition = rememberInfiniteTransition(label = "upgrade_pulse")
    val upgradePulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (canUpgrade) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "upgrade_scale"
    )

    EcoCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp
    ) {
        // Header row
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pets,
                        contentDescription = null,
                        tint = EcoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = t("petCompanionTitle"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (showDemoPanel) EcoForeground else EcoSunshine.copy(alpha = 0.28f),
                    border = BorderStroke(
                        1.dp,
                        if (showDemoPanel) EcoForeground else EcoSunshine
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            showDemoPanel = !showDemoPanel
                            if (!showDemoPanel) isAutoPlaying = false
                        }
                ) {
                    Text(
                        text = "🎬 ${t("judgeDemoButton")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (showDemoPanel) EcoPrimaryGlow else EcoForeground,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EcoPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EcoPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            sanctuaryInitialTab = SanctuaryTab.EVOLUTIONS
                            showSanctuary = true
                        }
                ) {
                    Text(
                        text = "✨ ${t("petEvolutionButton")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = EcoCoral.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EcoCoral.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            sanctuaryInitialTab = SanctuaryTab.STORY
                            showSanctuary = true
                        }
                ) {
                    Text(
                        text = "📖 ${t("petStoryButton")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoCoral,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color(activeSkin.accentHex).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(activeSkin.accentHex).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            sanctuaryInitialTab = SanctuaryTab.SKINS
                            showSanctuary = true
                        }
                ) {
                    Text(
                        text = "${activeSkin.emoji} Skin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = EcoSecondary,
                    border = BorderStroke(1.dp, EcoBorder),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            sanctuaryInitialTab = SanctuaryTab.SPECIES
                            showSanctuary = true
                        }
                ) {
                    Text(
                        text = "🐾 ${t("petSwitchButton")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Interactive Duo Pixel Stage (Pet + Plastic Mascot)
        PixelDuoStage(
            locale = locale,
            petSpecies = petState.species,
            petStage = stage,
            petEvent = petEvent,
            petEventTrigger = petEventTrigger,
            bannerText = bannerText,
            activePlastic = activePlastic,
            duoAction = duoAction,
            duoTriggerId = duoTriggerId,
            onTapPet = onPatPet,
            onTapPlastic = {
                duoAction = if (duoAction == DuoInteractionType.HIGH_FIVE) {
                    DuoInteractionType.DANCE
                } else {
                    DuoInteractionType.HIGH_FIVE
                }
                duoTriggerId++
            },
            petSkin = activeSkin,
            habitatTheme = habitatTheme
        )

        Spacer(Modifier.height(10.dp))

        // Pet Info & Personality Reaction & XP Progress
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayPetName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EcoForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EcoForeground)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Lv.${petState.level}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoPrimaryGlow
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EcoPrimary.copy(alpha = 0.12f))
                        .clickable {
                            sanctuaryInitialTab = SanctuaryTab.EVOLUTIONS
                            showSanctuary = true
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "✨ ${t(stage.titleKey)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EcoSecondary)
                        .clickable {
                            sanctuaryInitialTab = SanctuaryTab.STORY
                            showSanctuary = true
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${personality.emoji} ${t(personality.nameKey)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                }
                Text(
                    text = "♥ ${t("petBondLabel")}: ${personalization.petBond}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoCoral
                )
            }

            Spacer(Modifier.height(5.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(EcoSecondary.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "💬 $statusSpeech",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (petEvent == PetAnimationEvent.LEVEL_UP) EcoPrimary else EcoForeground,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(EcoSecondary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceAtLeast(0.05f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (canUpgrade) EcoSunshine else EcoPrimary)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (canUpgrade) {
                    t("petUpgradeReady")
                } else {
                    "$earnedInLevel / $span XP · ${t("petNeedMoreXp", mapOf("xp" to remainingXp))}"
                },
                fontSize = 10.sp,
                fontWeight = if (canUpgrade) FontWeight.Bold else FontWeight.Normal,
                color = if (canUpgrade) EcoPrimary else EcoMutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(10.dp))

        // Plastic Mascot Selector & Duo Actions
        PlasticMascotSelectorBar(
            locale = locale,
            activePlastic = activePlastic,
            onSummonPlastic = { plastic ->
                isAutoPlaying = false
                activePlastic = plastic
                duoAction = DuoInteractionType.INTRO
                duoTriggerId++
            },
            onHighFive = {
                isAutoPlaying = false
                duoAction = DuoInteractionType.HIGH_FIVE
                duoTriggerId++
            },
            onDance = {
                isAutoPlaying = false
                duoAction = DuoInteractionType.DANCE
                duoTriggerId++
            },
            onDismiss = {
                isAutoPlaying = false
                activePlastic = null
            }
        )

        Spacer(Modifier.height(12.dp))

        // Action Buttons: Feed (+15 XP) & Upgrade
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onFeedPet,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EcoPrimary.copy(alpha = 0.45f)),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    text = "🍃 ${t("petFeedButton", mapOf("xp" to PetProgression.FEED_BONUS_XP))}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onUpgradePet,
                enabled = canUpgrade,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .graphicsLayer {
                        scaleX = upgradePulse
                        scaleY = upgradePulse
                    },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canUpgrade) EcoSunshine else EcoSecondary,
                    contentColor = if (canUpgrade) EcoForeground else EcoMutedForeground,
                    disabledContainerColor = EcoSecondary,
                    disabledContentColor = EcoMutedForeground.copy(alpha = 0.65f)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = t("petUpgradeButton", mapOf("level" to petState.level + 1)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showDemoPanel) {
            Spacer(Modifier.height(12.dp))
            JudgeDemoPanel(
                locale = locale,
                isAutoPlaying = isAutoPlaying,
                demoStepLabel = demoStepLabel,
                canUpgrade = canUpgrade,
                onToggleAutoPlay = { isAutoPlaying = !isAutoPlaying },
                onDemoHighFive = {
                    isAutoPlaying = false
                    if (activePlastic == null) activePlastic = PlasticType.PET
                    duoAction = DuoInteractionType.HIGH_FIVE
                    duoTriggerId++
                },
                onDemoDance = {
                    isAutoPlaying = false
                    if (activePlastic == null) activePlastic = PlasticType.PP
                    duoAction = DuoInteractionType.DANCE
                    duoTriggerId++
                },
                onDemoAddPoints = {
                    isAutoPlaying = false
                    onDemoAddPoints()
                },
                onDemoUpgrade = {
                    isAutoPlaying = false
                    onDemoUpgradePet()
                }
            )
        }
    }

    if (showSanctuary) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSanctuary = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            PetSanctuaryContent(
                locale = locale,
                studentPoints = studentPoints,
                petState = petState,
                personalization = personalization,
                petEvent = petEvent,
                petEventTrigger = petEventTrigger,
                bannerText = bannerText,
                initialTab = sanctuaryInitialTab,
                onPatPet = onPatPet,
                onFeedPet = onFeedPet,
                onUpgradePet = onUpgradePet,
                onSelectSpecies = onSelectSpecies,
                onSelectSkin = onSelectSkin
            )
        }
    }
}

@Composable
private fun PetSanctuaryContent(
    locale: AppLocale,
    studentPoints: Int,
    petState: PetCompanionState,
    personalization: UserPersonalizationState,
    petEvent: PetAnimationEvent,
    petEventTrigger: Int,
    bannerText: String?,
    initialTab: SanctuaryTab,
    onPatPet: () -> Unit,
    onFeedPet: () -> Unit,
    onUpgradePet: () -> Unit,
    onSelectSpecies: (PetSpecies) -> Unit,
    onSelectSkin: (PetSkin) -> Unit
) {
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    val actualStage = PetStage.fromLevel(petState.level)
    val canUpgrade = PetProgression.canUpgrade(studentPoints, petState)
    val activeSkin = personalization.resolvedSkin()
    val unlockedSkins = personalization.safeUnlockedSkins()
    val city = personalization.safeCity()
    val personality = personalization.resolvedPersonality(petState.species)
    val displayPetName = personalization.petNickname.ifBlank { t(petState.species.nameKey) }

    var activeTab by remember(initialTab) {
        mutableStateOf(initialTab)
    }
    var previewStage by remember { mutableStateOf<PetStage?>(null) }
    var previewBurstId by remember { mutableIntStateOf(0) }

    val displayedStage = previewStage ?: actualStage
    val isPreviewingDifferent = previewStage != null && previewStage != actualStage
    val effectiveEvent = if (previewBurstId > 0 && isPreviewingDifferent) {
        PetAnimationEvent.LEVEL_UP
    } else {
        petEvent
    }
    val effectiveTrigger = if (previewBurstId > 0 && isPreviewingDifferent) {
        petEventTrigger + previewBurstId * 1000
    } else {
        petEventTrigger
    }
    val effectiveBanner = if (previewBurstId > 0 && isPreviewingDifferent) {
        "Lv.${displayedStage.minLevel}+"
    } else {
        bannerText
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = t("petSanctuaryTitle"),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EcoForeground
        )
        Text(
            text = t("petSanctuarySubtitle"),
            fontSize = 13.sp,
            color = EcoMutedForeground
        )

        // Large active / preview stage box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(personalization.resolvedHabitat().skyHex))
                .border(1.5.dp, EcoBorder, RoundedCornerShape(22.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isPreviewingDifferent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EcoSunshine.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = t(
                                    "petPreviewingStage",
                                    mapOf("stage" to "${t(displayedStage.titleKey)} (Lv.${displayedStage.minLevel}+)")
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground
                            )
                        }
                        Text(
                            text = t("petResetPreview"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { previewStage = null }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                PixelPetSprite(
                    species = petState.species,
                    stage = displayedStage,
                    skin = activeSkin,
                    event = effectiveEvent,
                    eventTriggerId = effectiveTrigger,
                    eventBannerText = effectiveBanner,
                    spriteSize = 128.dp,
                    onTap = onPatPet
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isPreviewingDifferent) {
                        "$displayPetName · ${t(displayedStage.titleKey)} (Lv.${displayedStage.minLevel}+)"
                    } else {
                        "$displayPetName · Lv.${petState.level} (${t(actualStage.titleKey)})"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${personality.emoji} ${t(personality.nameKey)} · ${t(petState.species.descKey)}",
                    fontSize = 12.sp,
                    color = EcoMutedForeground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onFeedPet,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🍃 ${t("petFeedButton", mapOf("xp" to PetProgression.FEED_BONUS_XP))}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                    }
                    Button(
                        onClick = {
                            previewStage = null
                            onUpgradePet()
                        },
                        enabled = canUpgrade,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EcoSunshine,
                            contentColor = EcoForeground
                        )
                    ) {
                        Text(
                            text = "✨ ${t("petUpgradeButton", mapOf("level" to petState.level + 1))}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Switcher tabs: Choose Pet / Evolutions / Story / Skins
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(EcoSecondary)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                SanctuaryTab.SPECIES to t("petTabSpecies"),
                SanctuaryTab.EVOLUTIONS to t("petTabEvolutions"),
                SanctuaryTab.STORY to t("petTabStory"),
                SanctuaryTab.SKINS to t("petTabSkins")
            ).forEach { (tab, label) ->
                val selected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { activeTab = tab }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) EcoPrimary else EcoMutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        when (activeTab) {
            SanctuaryTab.SPECIES -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PetSpecies.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { species ->
                                val selected = petState.species == species
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { onSelectSpecies(species) },
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (selected) Color(species.secondaryHex).copy(alpha = 0.25f) else EcoSecondary.copy(alpha = 0.55f),
                                    border = BorderStroke(
                                        if (selected) 2.dp else 1.dp,
                                        if (selected) Color(species.primaryHex) else EcoBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        PixelPetSprite(
                                            species = species,
                                            stage = displayedStage,
                                            skin = activeSkin,
                                            spriteSize = 70.dp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = t(species.nameKey),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoForeground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SanctuaryTab.EVOLUTIONS -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = t("petTapStageToPreview"),
                        fontSize = 12.sp,
                        color = EcoMutedForeground
                    )
                    val roadmap = listOf(
                        Triple(PetStage.BABY, "Lv.1", t("petStageRuleBaby")),
                        Triple(PetStage.SPROUT, "Lv.3", t("petStageRuleSprout")),
                        Triple(PetStage.GUARDIAN, "Lv.6", t("petStageRuleGuardian")),
                        Triple(PetStage.LEGEND, "Lv.10", t("petStageRuleLegend"))
                    )
                    roadmap.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { (st, lv, rule) ->
                                val unlocked = petState.level >= st.minLevel
                                val isSelectedPreview = displayedStage == st
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            previewStage = st
                                            previewBurstId++
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    color = when {
                                        isSelectedPreview -> Color(petState.species.secondaryHex).copy(alpha = 0.28f)
                                        unlocked -> Color.White
                                        else -> EcoSecondary.copy(alpha = 0.5f)
                                    },
                                    border = BorderStroke(
                                        if (isSelectedPreview) 2.dp else 1.dp,
                                        when {
                                            isSelectedPreview -> EcoPrimary
                                            unlocked -> EcoPrimary.copy(alpha = 0.4f)
                                            else -> EcoBorder
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = lv,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (unlocked) EcoPrimary else EcoMutedForeground
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (unlocked) EcoPrimary.copy(alpha = 0.14f)
                                                        else EcoSecondary
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = t(if (unlocked) "petUnlockedBadge" else "petLockedBadge"),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (unlocked) EcoPrimary else EcoMutedForeground
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        PixelPetSprite(
                                            species = petState.species,
                                            stage = st,
                                            skin = activeSkin,
                                            spriteSize = 74.dp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = t(st.titleKey),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoForeground
                                        )
                                        Text(
                                            text = rule,
                                            fontSize = 11.sp,
                                            color = EcoMutedForeground,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SanctuaryTab.STORY -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EcoCard(
                        containerColor = EcoPrimary.copy(alpha = 0.08f),
                        borderColor = EcoPrimary.copy(alpha = 0.3f),
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "${personality.emoji} ${t(personality.nameKey)} · ♥ ${t("petBondLabel")} ${personalization.petBond}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = t(personality.descKey),
                            fontSize = 12.sp,
                            color = EcoMutedForeground
                        )
                    }

                    petState.species.storyChapters().forEach { ch ->
                        val unlocked = petState.level >= ch.unlockLevel
                        EcoCard(
                            containerColor = if (unlocked) Color.White else EcoSecondary.copy(alpha = 0.6f),
                            borderColor = if (unlocked) EcoPrimary.copy(alpha = 0.35f) else EcoBorder,
                            contentPadding = 12.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = t(ch.titleKey),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoForeground,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (unlocked) EcoPrimary.copy(alpha = 0.14f) else EcoSecondary)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (unlocked) "Lv.${ch.unlockLevel}+" else t("petStoryLocked", mapOf("level" to ch.unlockLevel)),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unlocked) EcoPrimary else EcoMutedForeground
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (unlocked) t(ch.bodyKey) else "🔒 ${t("petStoryLocked", mapOf("level" to ch.unlockLevel))}",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = if (unlocked) EcoForeground.copy(alpha = 0.85f) else EcoMutedForeground
                            )
                        }
                    }
                }
            }

            SanctuaryTab.SKINS -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🌱 ${t("citySeedLabel", mapOf("seeds" to city.ecoSeeds))}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary
                    )
                    PetSkin.entries.forEach { skin ->
                        val isUnlocked = skin.id in unlockedSkins
                        val isEquipped = activeSkin == skin
                        val canBuy = !isUnlocked && petState.level >= skin.minLevel && city.ecoSeeds >= skin.seedCost

                        EcoCard(
                            containerColor = if (isEquipped) Color(skin.accentHex).copy(alpha = 0.14f) else Color.White,
                            borderColor = if (isEquipped) EcoPrimary else EcoBorder,
                            contentPadding = 12.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PixelPetSprite(
                                    species = petState.species,
                                    stage = actualStage,
                                    skin = skin,
                                    spriteSize = 58.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${skin.emoji} ${t(skin.nameKey)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoForeground
                                    )
                                    Text(
                                        text = t(skin.descKey),
                                        fontSize = 11.sp,
                                        color = EcoMutedForeground
                                    )
                                }
                                Button(
                                    onClick = { onSelectSkin(skin) },
                                    enabled = isUnlocked || canBuy,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            isEquipped -> EcoSecondary
                                            isUnlocked -> EcoPrimary
                                            canBuy -> EcoSunshine
                                            else -> EcoSecondary
                                        },
                                        contentColor = when {
                                            isEquipped -> EcoMutedForeground
                                            isUnlocked -> Color.White
                                            else -> EcoForeground
                                        }
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    val label = when {
                                        isEquipped -> t("skinEquipped")
                                        isUnlocked -> t("skinEquip")
                                        petState.level < skin.minLevel -> t("skinNeedLevel", mapOf("level" to skin.minLevel))
                                        else -> t("skinUnlock", mapOf("seeds" to skin.seedCost))
                                    }
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun PixelPetScanCelebration(
    locale: AppLocale,
    pointsEarned: Int,
    studentPoints: Int,
    petState: PetCompanionState,
    petEvent: PetAnimationEvent,
    petEventTrigger: Int,
    petEventNewLevel: Int,
    onUpgradePet: () -> Unit,
    modifier: Modifier = Modifier,
    celebratedPlastic: PlasticType? = null,
    personalization: UserPersonalizationState = UserPersonalizationState()
) {
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    val stage = PetStage.fromLevel(petState.level)
    val canUpgrade = PetProgression.canUpgrade(studentPoints, petState)
    val activeSkin = personalization.resolvedSkin()
    val habitatTheme = personalization.resolvedHabitat()

    val eventToPlay = if (petEvent == PetAnimationEvent.LEVEL_UP) {
        PetAnimationEvent.LEVEL_UP
    } else {
        PetAnimationEvent.POINTS_ADDED
    }
    val banner = if (petEvent == PetAnimationEvent.LEVEL_UP) {
        "LV.$petEventNewLevel!"
    } else {
        "+$pointsEarned"
    }

    var duoTrigger by remember { mutableIntStateOf(1) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (celebratedPlastic != null) {
            PixelDuoStage(
                locale = locale,
                petSpecies = petState.species,
                petStage = stage,
                petEvent = eventToPlay,
                petEventTrigger = petEventTrigger.coerceAtLeast(1),
                bannerText = banner,
                activePlastic = celebratedPlastic,
                duoAction = DuoInteractionType.HIGH_FIVE,
                duoTriggerId = duoTrigger + petEventTrigger,
                onTapPet = {},
                onTapPlastic = { duoTrigger++ },
                petSkin = activeSkin,
                habitatTheme = habitatTheme
            )
        } else {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(habitatTheme.skyHex))
                    .border(2.dp, EcoPrimary.copy(alpha = 0.35f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                PixelHabitatBackdrop()
                PixelPetSprite(
                    species = petState.species,
                    stage = stage,
                    skin = activeSkin,
                    event = eventToPlay,
                    eventTriggerId = petEventTrigger.coerceAtLeast(1),
                    eventBannerText = banner,
                    spriteSize = 122.dp
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = t("petScanBuddyCheer", mapOf("name" to t(petState.species.nameKey))),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoPrimary,
            textAlign = TextAlign.Center
        )
        if (canUpgrade) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onUpgradePet,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoSunshine,
                    contentColor = EcoForeground
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "✨ ${t("petUpgradeButton", mapOf("level" to petState.level + 1))}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun PixelHabitatBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val px = (size.minDimension / 16f).coerceAtLeast(4f)
        // Pixel meadow ground strip
        drawRect(
            color = EcoLeaf.copy(alpha = 0.18f),
            topLeft = Offset(0f, size.height - px * 3f),
            size = Size(size.width, px * 3f)
        )
        // Little pixel clouds / stars
        drawRect(
            color = Color.White.copy(alpha = 0.65f),
            topLeft = Offset(px * 1.5f, px * 1.5f),
            size = Size(px * 2.5f, px)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(size.width - px * 4f, px * 2.5f),
            size = Size(px * 2f, px)
        )
    }
}
