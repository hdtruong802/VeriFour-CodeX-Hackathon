package com.verifour.app.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.CityBuildingType
import com.verifour.app.model.EcoPersona
import com.verifour.app.model.GreenCityState
import com.verifour.app.model.HabitatTheme
import com.verifour.app.model.InvitedCityMascot
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetPersonality
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PetStage
import com.verifour.app.model.PlasticType
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCard
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoLeaf
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelGreenCityCard(
    locale: AppLocale,
    userName: String,
    petState: PetCompanionState,
    personalization: UserPersonalizationState,
    onWaterTree: () -> Unit,
    onUpgradeBuilding: (CityBuildingType) -> Unit,
    onGrantDemoResources: () -> Unit,
    onSavePersonalization: (
        petNickname: String,
        personality: PetPersonality,
        ecoPersona: EcoPersona,
        favoritePlastic: PlasticType,
        habitatTheme: HabitatTheme
    ) -> Unit,
    modifier: Modifier = Modifier,
    onOpenCampaigns: (() -> Unit)? = null
) {
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var showCityBuilder by remember { mutableStateOf(false) }
    var showPersonalizer by remember { mutableStateOf(false) }

    val city = personalization.safeCity()
    val habitat = personalization.resolvedHabitat()
    val invitedMascots = personalization.safeInvitedMascots()
    val greenIndex = city.greenIndexPercent()

    EcoCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp
    ) {
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
                    imageVector = Icons.Rounded.Apartment,
                    contentDescription = null,
                    tint = EcoPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = t("cityTitle"),
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
                color = EcoPrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, EcoPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showPersonalizer = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = EcoPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = t("personalizeButton"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Animated Pixel Green City Skyline with Roaming & Jumping Invited Mascots
        PixelAnimatedCityStage(
            city = city,
            habitat = habitat,
            userPetSpecies = petState.species,
            userPetStage = PetStage.fromLevel(petState.level),
            userPetSkin = personalization.resolvedSkin(),
            userPetName = personalization.petNickname.ifBlank { t(petState.species.nameKey) },
            invitedMascots = invitedMascots,
            stageHeight = 128.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, EcoBorder, RoundedCornerShape(16.dp))
                .clickable { showCityBuilder = true }
        )

        Spacer(Modifier.height(10.dp))

        // Green Index + Resource Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${t("cityGreenIndex")}: $greenIndex% · ${t("cityTreesPlanted", mapOf("count" to city.treesPlanted))}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Text(
                text = t("cityBonusPower", mapOf("power" to city.cityBattlePowerBonus())),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EcoPrimary
            )
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(EcoSecondary)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((greenIndex / 100f).coerceIn(0.05f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(EcoPrimary)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResourcePill(
                    text = "🌱 ${t("citySeedLabel", mapOf("seeds" to city.ecoSeeds))}",
                    bg = EcoLeaf.copy(alpha = 0.16f),
                    fg = EcoForeground
                )
                ResourcePill(
                    text = "💧 ${t("cityWaterLabel", mapOf("water" to city.waterDrops))}",
                    bg = EcoOcean.copy(alpha = 0.14f),
                    fg = EcoOcean
                )
            }
            if (onOpenCampaigns != null) {
                Surface(
                    shape = CircleShape,
                    color = EcoSunshine.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, EcoSunshine),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenCampaigns() }
                ) {
                    Text(
                        text = t("campaignCityBadge", mapOf("count" to invitedMascots.size)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onWaterTree,
                enabled = city.waterDrops > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (city.waterDrops > 0) EcoOcean.copy(alpha = 0.5f) else EcoBorder
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = if (city.waterDrops > 0) EcoOcean else EcoMutedForeground,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = t("cityWaterButton"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (city.waterDrops > 0) EcoOcean else EcoMutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = { showCityBuilder = true },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = "🏙️ ${t("cityBuildButton")}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showCityBuilder) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCityBuilder = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🏙️ ${t("cityModalTitle")}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Text(
                    text = t("cityModalSubtitle"),
                    fontSize = 13.sp,
                    color = EcoMutedForeground
                )

                PixelGreenCityCanvas(
                    city = city,
                    habitat = habitat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.5.dp, EcoBorder, RoundedCornerShape(18.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResourcePill(
                            text = "🌱 ${city.ecoSeeds}",
                            bg = EcoLeaf.copy(alpha = 0.16f),
                            fg = EcoForeground
                        )
                        ResourcePill(
                            text = "💧 ${city.waterDrops}",
                            bg = EcoOcean.copy(alpha = 0.14f),
                            fg = EcoOcean
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = EcoSunshine.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, EcoSunshine),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onGrantDemoResources() }
                    ) {
                        Text(
                            text = t("cityDemoBoost"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                CityBuildingType.entries.forEach { building ->
                    val curLvl = city.levelOf(building)
                    val isMax = curLvl >= building.maxLevel
                    val cost = building.upgradeCost(curLvl)
                    val affordable = !isMax && city.ecoSeeds >= cost

                    EcoCard(
                        containerColor = if (curLvl > 0) Color.White else EcoSecondary.copy(alpha = 0.6f),
                        borderColor = if (curLvl > 0) EcoPrimary.copy(alpha = 0.35f) else EcoBorder,
                        contentPadding = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(building.emoji, fontSize = 26.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = t(building.nameKey),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoForeground
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EcoPrimary.copy(alpha = 0.14f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "Lv.$curLvl/${building.maxLevel}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EcoPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = t(building.descKey),
                                    fontSize = 11.sp,
                                    color = EcoMutedForeground
                                )
                            }
                            Button(
                                onClick = { onUpgradeBuilding(building) },
                                enabled = affordable,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMax) EcoSecondary else EcoPrimary,
                                    contentColor = if (isMax) EcoMutedForeground else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isMax) t("cityMaxLevel") else "$cost 🌱",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showPersonalizer) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPersonalizer = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            PersonalizationSheetContent(
                locale = locale,
                userName = userName,
                petState = petState,
                personalization = personalization,
                onSave = { nick, personality, persona, plastic, theme ->
                    onSavePersonalization(nick, personality, persona, plastic, theme)
                    showPersonalizer = false
                }
            )
        }
    }
}

@Composable
fun PixelAnimatedCityStage(
    city: GreenCityState,
    habitat: HabitatTheme,
    userPetSpecies: PetSpecies,
    userPetStage: PetStage,
    userPetSkin: PetSkin,
    userPetName: String,
    invitedMascots: List<InvitedCityMascot>,
    modifier: Modifier = Modifier,
    stageHeight: Dp = 140.dp
) {
    val infinite = rememberInfiniteTransition(label = "city_mascots_patrol")
    val patrolClock by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "patrol_clock"
    )
    val hopClock by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (4f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hop_clock"
    )

    BoxWithConstraints(
        modifier = modifier.height(stageHeight)
    ) {
        PixelGreenCityCanvas(
            city = city,
            habitat = habitat,
            modifier = Modifier.fillMaxSize()
        )

        val stageW = maxWidth
        val spriteDp = 42.dp
        val maxTravel = (stageW - spriteDp - 8.dp).coerceAtLeast(24.dp)

        // User's own pet roaming inside their custom city
        val userSin = sin(patrolClock)
        val userNormX = (userSin + 1f) * 0.5f
        val userMovingLeft = cos(patrolClock) < 0f
        val userJumpDp = (-abs(sin(hopClock)) * 10f).dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 4.dp + maxTravel * userNormX, y = (-6).dp + userJumpDp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EcoPrimary.copy(alpha = 0.88f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = userPetName.take(10),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
            PixelPetSprite(
                species = userPetSpecies,
                stage = userPetStage,
                skin = userPetSkin,
                spriteSize = spriteDp,
                modifier = Modifier.graphicsLayer {
                    scaleX = if (userMovingLeft) -1f else 1f
                }
            )
        }

        // Invited friend mascots running & jumping around inside the custom city
        invitedMascots.take(8).forEachIndexed { idx, mascot ->
            val phase = (idx + 1) * 1.15f
            val speedMult = if (idx % 2 == 0) 1f else 2f
            val angle = patrolClock * speedMult + phase
            val normX = ((sin(angle) + 1f) * 0.5f).coerceIn(0f, 1f)
            val movingLeft = cos(angle) < 0f
            val jumpDp = (-abs(sin(hopClock * 1.3f + idx * 0.9f)) * 13f).dp
            val rowOffsetY = (-4 - (idx % 2) * 8).dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 4.dp + maxTravel * normX, y = rowOffsetY + jumpDp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EcoForeground.copy(alpha = 0.82f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = mascot.mascotName.take(10),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                if (mascot.plasticMascot != null) {
                    PixelPlasticMascotSprite(
                        plasticType = mascot.plasticMascot,
                        spriteSize = 38.dp,
                        happyOverride = true,
                        fastBob = true,
                        flipHorizontal = movingLeft
                    )
                } else {
                    PixelPetSprite(
                        species = mascot.species,
                        stage = PetStage.SPROUT,
                        skin = mascot.skin,
                        spriteSize = 40.dp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = if (movingLeft) -1f else 1f
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourcePill(
    text: String,
    bg: Color,
    fg: Color
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun PixelGreenCityCanvas(
    city: GreenCityState,
    habitat: HabitatTheme,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "city_motion")
    val windmillAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "windmill_spin"
    )
    val cloudShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_drift"
    )

    Canvas(modifier = modifier.background(Color(habitat.skyHex))) {
        val px = (size.height / 24f).coerceAtLeast(3f)
        val groundY = size.height - px * 4f

        // Pixel Sun / Moon
        val sunColor = if (habitat == HabitatTheme.CYBER_NIGHT) Color(0xFFFDE68A) else EcoSunshine
        drawRect(
            color = sunColor,
            topLeft = Offset(size.width * 0.84f, px * 2f),
            size = Size(px * 3f, px * 3f)
        )

        // Drifting Pixel Clouds
        val cOff = cloudShift * px * 5f
        drawRect(
            color = Color(habitat.cloudHex).copy(alpha = 0.78f),
            topLeft = Offset(px * 4f + cOff, px * 2.5f),
            size = Size(px * 5f, px * 1.4f)
        )
        drawRect(
            color = Color(habitat.cloudHex).copy(alpha = 0.65f),
            topLeft = Offset(size.width * 0.45f - cOff * 0.6f, px * 3.5f),
            size = Size(px * 4f, px * 1.2f)
        )

        // Ground meadow strip
        drawRect(
            color = Color(habitat.groundHex).copy(alpha = 0.35f),
            topLeft = Offset(0f, groundY),
            size = Size(size.width, size.height - groundY)
        )
        drawRect(
            color = Color(habitat.groundHex).copy(alpha = 0.65f),
            topLeft = Offset(0f, groundY),
            size = Size(size.width, px * 0.8f)
        )

        // Solar Power Hub
        val solarLvl = city.levelOf(CityBuildingType.SOLAR_HUB)
        if (solarLvl > 0) {
            val sx = size.width * 0.23f
            val h = px * (5f + solarLvl * 1.6f)
            drawRect(color = Color(0xFF334155), topLeft = Offset(sx, groundY - h), size = Size(px * 5.5f, h))
            // Solar panels on roof
            drawRect(color = Color(0xFF38BDF8), topLeft = Offset(sx - px * 0.4f, groundY - h - px * 1.2f), size = Size(px * 6.3f, px * 1.2f))
            // Lit pixel windows
            for (wy in 1..solarLvl + 1) {
                drawRect(
                    color = EcoSunshine,
                    topLeft = Offset(sx + px, groundY - h + wy * px * 1.6f),
                    size = Size(px, px)
                )
                drawRect(
                    color = EcoSunshine,
                    topLeft = Offset(sx + px * 3.5f, groundY - h + wy * px * 1.6f),
                    size = Size(px, px)
                )
            }
        }

        // Smart Recycle Lab
        val labLvl = city.levelOf(CityBuildingType.RECYCLE_LAB)
        if (labLvl > 0) {
            val rx = size.width * 0.42f
            val h = px * (4.5f + labLvl * 1.4f)
            drawRect(color = Color(0xFF0F766E), topLeft = Offset(rx, groundY - h), size = Size(px * 6f, h))
            drawRect(color = EcoPrimaryGlow, topLeft = Offset(rx + px, groundY - h + px), size = Size(px * 4f, px * 1.5f))
            // Leaf chimney
            drawRect(color = EcoPrimary, topLeft = Offset(rx + px * 4f, groundY - h - px * 2f), size = Size(px * 1.5f, px * 2f))
        }

        // Sky Garden Tower
        val skyLvl = city.levelOf(CityBuildingType.SKY_GARDEN)
        if (skyLvl > 0) {
            val tx = size.width * 0.62f
            val h = px * (7f + skyLvl * 2.2f)
            drawRect(color = Color(0xFF1E293B), topLeft = Offset(tx, groundY - h), size = Size(px * 5f, h))
            // Rooftop sky garden foliage
            drawRect(color = EcoPrimary, topLeft = Offset(tx - px * 0.6f, groundY - h - px * 1.8f), size = Size(px * 6.2f, px * 1.8f))
            drawRect(color = EcoLeaf, topLeft = Offset(tx + px * 0.5f, groundY - h - px * 2.8f), size = Size(px * 4f, px * 1.2f))
        }

        // Windmill Park with spinning pixel blades
        val windLvl = city.levelOf(CityBuildingType.WIND_PARK)
        if (windLvl > 0) {
            val wx = size.width * 0.80f
            val towerH = px * (6f + windLvl * 1.4f)
            drawRect(color = Color.White, topLeft = Offset(wx - px * 0.4f, groundY - towerH), size = Size(px * 0.9f, towerH))
            val hub = Offset(wx, groundY - towerH)
            val bladeLen = px * (2.6f + windLvl * 0.4f)
            for (b in 0 until 4) {
                val a = windmillAngle + b * (PI.toFloat() / 2f)
                val bx = hub.x + cos(a) * bladeLen
                val by = hub.y + sin(a) * bladeLen
                drawLine(
                    color = Color.White,
                    start = hub,
                    end = Offset(bx, by),
                    strokeWidth = px * 0.7f
                )
            }
        }

        // Eco Trees (drawn across foreground based on treesPlanted & ECO_FOREST level)
        val forestLvl = city.levelOf(CityBuildingType.ECO_FOREST)
        val treeCount = (city.treesPlanted + forestLvl).coerceIn(1, 7)
        val treePositions = listOf(0.08f, 0.15f, 0.34f, 0.55f, 0.73f, 0.90f, 0.28f)
        for (i in 0 until treeCount) {
            val tx = size.width * treePositions[i % treePositions.size]
            val treeSize = px * (1.8f + ((i + city.treeLevel) % 3) * 0.6f)
            // Trunk
            drawRect(
                color = Color(0xFF784F32),
                topLeft = Offset(tx - px * 0.4f, groundY - treeSize * 0.9f),
                size = Size(px * 0.9f, treeSize * 0.9f)
            )
            // Foliage
            val leafColor = if (habitat == HabitatTheme.SAKURA_GARDEN && i % 2 == 0) {
                Color(0xFFFF8CB8)
            } else if (i % 2 == 0) {
                EcoPrimary
            } else {
                EcoLeaf
            }
            drawRect(
                color = leafColor,
                topLeft = Offset(tx - treeSize * 0.85f, groundY - treeSize * 2.1f),
                size = Size(treeSize * 1.8f, treeSize * 1.4f)
            )
            drawRect(
                color = EcoPrimaryGlow.copy(alpha = 0.75f),
                topLeft = Offset(tx - treeSize * 0.45f, groundY - treeSize * 2.5f),
                size = Size(treeSize * 1.0f, treeSize * 0.7f)
            )
        }
    }
}

@Composable
fun PersonalizationSheetContent(
    locale: AppLocale,
    userName: String,
    petState: PetCompanionState,
    personalization: UserPersonalizationState,
    onSave: (
        petNickname: String,
        personality: PetPersonality,
        ecoPersona: EcoPersona,
        favoritePlastic: PlasticType,
        habitatTheme: HabitatTheme
    ) -> Unit
) {
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var nickname by remember(personalization.petNickname) { mutableStateOf(personalization.petNickname) }
    var personality by remember(personalization, petState.species) {
        mutableStateOf(personalization.resolvedPersonality(petState.species))
    }
    var ecoPersona by remember(personalization.ecoPersonaId) {
        mutableStateOf(personalization.resolvedPersona())
    }
    var favoritePlastic by remember(personalization.favoritePlasticCode) {
        mutableStateOf(personalization.resolvedFavoritePlastic())
    }
    var habitatTheme by remember(personalization.habitatThemeId) {
        mutableStateOf(personalization.resolvedHabitat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "✨ ${t("personalizeTitle")}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EcoForeground
        )
        Text(
            text = t("personalizeSubtitle", mapOf("name" to userName)),
            fontSize = 13.sp,
            color = EcoMutedForeground
        )

        // Pet Nickname
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = t("personalizeNicknameLabel"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = { if (it.length <= 20) nickname = it },
                placeholder = { Text(t("personalizeNicknameHint"), fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = EcoCard,
                    unfocusedContainerColor = EcoCard,
                    focusedBorderColor = EcoPrimary,
                    unfocusedBorderColor = EcoBorder
                )
            )
        }

        // Pet Personality
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = t("personalizePersonalityLabel"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PetPersonality.entries.forEach { p ->
                    val selected = personality == p
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) EcoPrimary else EcoSecondary,
                        border = BorderStroke(1.dp, if (selected) EcoPrimary else EcoBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { personality = p }
                    ) {
                        Text(
                            text = "${p.emoji} ${t(p.nameKey)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else EcoForeground,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Text(
                text = t(personality.descKey),
                fontSize = 11.sp,
                color = EcoMutedForeground
            )
        }

        // Eco Persona
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = t("personalizePersonaLabel"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            EcoPersona.entries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { persona ->
                        val selected = ecoPersona == persona
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { ecoPersona = persona },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) EcoPrimary.copy(alpha = 0.14f) else EcoSecondary,
                            border = BorderStroke(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) EcoPrimary else EcoBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${persona.emoji} ${t(persona.nameKey)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) EcoPrimary else EcoForeground
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stage Theme
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = t("personalizeHabitatLabel"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitatTheme.entries.forEach { theme ->
                    val selected = habitatTheme == theme
                    Surface(
                        shape = CircleShape,
                        color = if (selected) Color(theme.groundHex) else Color(theme.skyHex),
                        border = BorderStroke(if (selected) 2.dp else 1.dp, EcoBorder),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { habitatTheme = theme }
                    ) {
                        Text(
                            text = "${theme.emoji} ${t(theme.nameKey)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                selected -> Color.White
                                theme == HabitatTheme.CYBER_NIGHT -> Color.White
                                else -> EcoForeground
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Favorite Plastic Focus
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = t("personalizePlasticLabel"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlasticType.entries.forEach { pl ->
                    val selected = favoritePlastic == pl
                    Surface(
                        shape = CircleShape,
                        color = if (selected) EcoPrimary else EcoSecondary,
                        border = BorderStroke(1.dp, if (selected) EcoPrimary else EcoBorder),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { favoritePlastic = pl }
                    ) {
                        Text(
                            text = "${pl.emoji} ${pl.code}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else EcoForeground,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onSave(nickname, personality, ecoPersona, favoritePlastic, habitatTheme) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
        ) {
            Text(
                text = t("personalizeSaveButton"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
