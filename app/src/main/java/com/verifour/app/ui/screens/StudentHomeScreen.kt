package com.verifour.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.CityBuildingType
import com.verifour.app.model.EcoPersona
import com.verifour.app.model.FlysticState
import com.verifour.app.model.HabitatTheme
import com.verifour.app.model.PetAnimationEvent
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetPersonality
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PlasticType
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.PixelGreenCityCard
import com.verifour.app.ui.components.PixelPetHomeCard
import com.verifour.app.ui.theme.EcoBackground
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoLeaf
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine
import java.util.Locale

@Composable
fun StudentHomeScreen(
    state: FlysticState,
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
    onSelectPetSpecies: (PetSpecies) -> Unit,
    onSelectSkin: (PetSkin) -> Unit,
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
    onOpenScan: () -> Unit,
    onOpenAiSort: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenBattle: () -> Unit,
    onOpenCampaigns: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val user = state.user ?: return
    val locale = state.locale
    fun t(key: String) = FlysticTranslations.t(key, locale)

    val myTransactions = state.transactions.filter { it.userId == user.id }
    val recent = myTransactions.take(3)
    val confirmedCount = myTransactions.count {
        effectiveTransactionStatus(it) == TransactionStatus.CONFIRMED
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val persona = personalization.resolvedPersona()
            val favPlastic = personalization.resolvedFavoritePlastic()
            val tier = personalization.resolvedTier()
            val equippedTitle = personalization.resolvedEquippedTitle()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = t("studentGreeting"),
                    fontSize = 14.sp,
                    color = EcoMutedForeground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${user.name} 👋",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    if (equippedTitle != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EcoPrimary.copy(alpha = 0.14f))
                                .border(1.dp, EcoPrimary.copy(alpha = 0.35f), CircleShape)
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${equippedTitle.emoji} ${t(equippedTitle.nameKey)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoPrimary
                            )
                        }
                    }
                }
                EcoCard(
                    containerColor = EcoPrimary.copy(alpha = 0.08f),
                    borderColor = EcoPrimary.copy(alpha = 0.25f),
                    contentPadding = 10.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${persona.emoji} ${t(persona.nameKey)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoPrimary
                            )
                            Text(
                                text = "· ${favPlastic.emoji} ${favPlastic.code} · ${tier.emoji} ${personalization.arenaRp} RP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoMutedForeground
                            )
                        }
                        Text(
                            text = t(persona.missionKey),
                            fontSize = 11.sp,
                            color = EcoForeground
                        )
                    }
                }
            }
        }

        item {
            Column {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EcoPrimary.copy(alpha = 0.1f))
                        .border(1.dp, EcoPrimary.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EcoPrimary)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = t("landingHeroLabel").uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${t("landingTitleLine1")} ${t("landingTitleLine2")}",
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
            }
        }

        // Total Points Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = EcoForeground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "♻️",
                        fontSize = 46.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                    )
                    Column {
                        Text(
                            text = t("studentCurrentPoints").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimaryGlow.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = FlysticTranslations.formatNumber(state.points, locale),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoBackground
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = t("studentPoints"),
                                fontSize = 15.sp,
                                color = EcoBackground.copy(alpha = 0.65f),
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2 Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EcoCard(
                    modifier = Modifier.weight(1f),
                    containerColor = EcoLeaf.copy(alpha = 0.16f),
                    contentPadding = 16.dp
                ) {
                    Text(
                        text = t("studentCollected").uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoMutedForeground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.2f", state.totalKg)} kg",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                }
                EcoCard(
                    modifier = Modifier.weight(1f),
                    containerColor = EcoOcean.copy(alpha = 0.13f),
                    contentPadding = 16.dp
                ) {
                    Text(
                        text = t("studentTransactions").uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoMutedForeground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$confirmedCount",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                }
            }
        }

        // Pixel Eco Pet Companion
        item {
            PixelPetHomeCard(
                locale = locale,
                studentPoints = state.points,
                petState = petState,
                personalization = personalization,
                petReactionKey = petReactionKey,
                petEvent = petEvent,
                petEventTrigger = petEventTrigger,
                petEventPointsDelta = petEventPointsDelta,
                petEventNewLevel = petEventNewLevel,
                onPatPet = onPatPet,
                onFeedPet = onFeedPet,
                onUpgradePet = onUpgradePet,
                onDemoAddPoints = onDemoAddPoints,
                onDemoUpgradePet = onDemoUpgradePet,
                onSelectSpecies = onSelectPetSpecies,
                onSelectSkin = onSelectSkin
            )
        }

        // Green City, Trees & Personalization
        item {
            PixelGreenCityCard(
                locale = locale,
                userName = user.name,
                petState = petState,
                personalization = personalization,
                onWaterTree = onWaterTree,
                onUpgradeBuilding = onUpgradeBuilding,
                onGrantDemoResources = onGrantDemoResources,
                onSavePersonalization = onSavePersonalization,
                onOpenCampaigns = onOpenCampaigns
            )
        }

        // Quick Actions (Scan + AI Sort + Weekly Battle + Campaigns + Guide + Leaderboard)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        emoji = "🔳",
                        label = t("studentScanQr"),
                        bg = EcoPrimary,
                        fg = Color.White,
                        border = EcoPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenScan
                    )
                    QuickActionTile(
                        emoji = "🤖",
                        label = t("aiDetectNavBtn"),
                        bg = EcoOcean.copy(alpha = 0.14f),
                        fg = EcoForeground,
                        border = EcoOcean.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAiSort
                    )
                    QuickActionTile(
                        emoji = "⚔️",
                        label = t("battleButton"),
                        bg = EcoForeground,
                        fg = EcoPrimaryGlow,
                        border = EcoForeground,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenBattle
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        emoji = "📣",
                        label = t("campaignNavBtn"),
                        bg = EcoLeaf.copy(alpha = 0.22f),
                        fg = EcoForeground,
                        border = EcoPrimary.copy(alpha = 0.35f),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCampaigns
                    )
                    QuickActionTile(
                        emoji = "📚",
                        label = t("studentGuideSorting"),
                        bg = Color.White,
                        fg = EcoForeground,
                        border = EcoBorder,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenGuide
                    )
                    QuickActionTile(
                        emoji = "🏆",
                        label = t("studentLeaderboard"),
                        bg = EcoSunshine.copy(alpha = 0.35f),
                        fg = EcoForeground,
                        border = EcoSunshine,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLeaderboard
                    )
                }
            }
        }

        // Recent activity header
        item {
            Text(
                text = t("studentRecentActivity"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
        }

        if (recent.isEmpty()) {
            item {
                EcoCard(contentPadding = 20.dp) {
                    Text(
                        text = t("studentNoTransactions"),
                        fontSize = 14.sp,
                        color = EcoMutedForeground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            items(recent, key = { it.id }) { tx ->
                val stationTitle = FlysticTranslations.localizedCollectionPoint(
                    tx.collectionPoint,
                    locale
                ).first
                EcoCard(contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stationTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "+${tx.points}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoPrimary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tx.items.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EcoSecondary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${item.plasticType.emoji} ${item.plasticType.code} × ${item.quantity} kg",
                                    fontSize = 11.sp,
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

@Composable
private fun QuickActionTile(
    emoji: String,
    label: String,
    bg: Color,
    fg: Color,
    border: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                textAlign = TextAlign.Center
            )
        }
    }
}
