package com.verifour.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.ECO_TITLES
import com.verifour.app.model.EcoCampaign
import com.verifour.app.model.FlysticState
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetProgression
import com.verifour.app.model.TitleCategory
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.theme.EcoBackground
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoLeaf
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary
import java.util.Locale
import kotlin.math.roundToInt

private enum class ProfileSheet {
    SETTINGS,
    HELP,
    TERMS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: FlysticState,
    isRemote: Boolean,
    petState: PetCompanionState,
    personalization: UserPersonalizationState,
    campaigns: List<EcoCampaign>,
    onEquipTitle: (String) -> Unit,
    onLocaleChange: (AppLocale) -> Unit,
    onSignOut: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val user = state.user ?: return
    val locale = state.locale
    fun t(key: String) = FlysticTranslations.t(key, locale)

    var activeSheet by remember { mutableStateOf<ProfileSheet?>(null) }
    var selectedCategory by remember { mutableStateOf<TitleCategory?>(null) }

    val myConfirmed = state.transactions.filter {
        it.userId == user.id && effectiveTransactionStatus(it) == TransactionStatus.CONFIRMED
    }
    val bottles = (state.totalKg * 40).roundToInt()
    val itemsKg = myConfirmed.sumOf { tx -> tx.items.sumOf { it.quantity } }
    val unlockedTitleIds = remember(state, petState, personalization, campaigns) {
        PetProgression.evaluateUnlockedTitles(state, petState, personalization, campaigns)
    }
    val equippedTitle = personalization.resolvedEquippedTitle()

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
        // User Profile Header Card
        item {
            EcoCard(contentPadding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(EcoPrimary, EcoLeaf))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                        if (equippedTitle != null) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EcoPrimary.copy(alpha = 0.14f))
                                    .border(1.dp, EcoPrimary.copy(alpha = 0.35f), CircleShape)
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${equippedTitle.emoji} ${t(equippedTitle.nameKey)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EcoPrimary
                                )
                            }
                        } else {
                            Text(
                                text = t("titleNoneEquipped"),
                                fontSize = 11.sp,
                                color = EcoMutedForeground
                            )
                        }
                        val meta = listOfNotNull(user.studentId, user.className).joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(meta, fontSize = 13.sp, color = EcoMutedForeground)
                        }
                        if (!user.faculty.isNullOrBlank()) {
                            Text(user.faculty, fontSize = 12.sp, color = EcoMutedForeground)
                        }
                    }
                }
            }
        }

        // Environmental Impact
        item {
            EcoCard(contentPadding = 18.dp) {
                Text(
                    text = t("profileImpact").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoMutedForeground
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImpactMetric(
                        value = String.format(Locale.US, "%.1f", state.totalKg),
                        unit = t("profileKg"),
                        modifier = Modifier.weight(1f)
                    )
                    ImpactMetric(
                        value = "$bottles",
                        unit = t("profileBottles"),
                        modifier = Modifier.weight(1f)
                    )
                    ImpactMetric(
                        value = String.format(Locale.US, "%.1f", itemsKg),
                        unit = t("profileItems"),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Multi-Category Eco Titles
        item {
            EcoCard(contentPadding = 18.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏅 ${t("titleSectionTitle")}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Text(
                        text = "${unlockedTitleIds.size}/${ECO_TITLES.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EcoPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t("titleSectionSubtitle"),
                    fontSize = 12.sp,
                    color = EcoMutedForeground
                )
                Spacer(Modifier.height(10.dp))

                // Category Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allSelected = selectedCategory == null
                    Surface(
                        shape = CircleShape,
                        color = if (allSelected) EcoForeground else EcoSecondary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { selectedCategory = null }
                    ) {
                        Text(
                            text = "🌟 ${ECO_TITLES.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allSelected) Color.White else EcoForeground,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    TitleCategory.entries.forEach { cat ->
                        val selected = selectedCategory == cat
                        Surface(
                            shape = CircleShape,
                            color = if (selected) EcoPrimary else EcoSecondary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = "${cat.emoji} ${t(cat.nameKey)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else EcoForeground,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                val categoriesToShow = selectedCategory?.let { listOf(it) } ?: TitleCategory.entries
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categoriesToShow.forEach { category ->
                        val catTitles = ECO_TITLES.filter { it.category == category }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${category.emoji} ${t(category.nameKey)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoForeground
                            )
                            catTitles.forEach { title ->
                                val unlocked = title.id in unlockedTitleIds
                                val isEquipped = personalization.equippedTitleId == title.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when {
                                                isEquipped -> EcoPrimary.copy(alpha = 0.12f)
                                                unlocked -> Color.White
                                                else -> EcoSecondary.copy(alpha = 0.7f)
                                            }
                                        )
                                        .border(
                                            width = if (isEquipped) 1.5.dp else 1.dp,
                                            color = if (isEquipped) EcoPrimary else EcoBorder,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .alpha(if (unlocked) 1f else 0.56f)
                                        .clickable(enabled = unlocked) { onEquipTitle(title.id) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(title.emoji, fontSize = 22.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = t(title.nameKey),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoForeground
                                        )
                                        Text(
                                            text = t(title.descKey),
                                            fontSize = 11.sp,
                                            color = EcoMutedForeground,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isEquipped -> EcoPrimary
                                                    unlocked -> EcoPrimary.copy(alpha = 0.12f)
                                                    else -> EcoBorder
                                                }
                                            )
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isEquipped -> t("titleEquippedBadge")
                                                unlocked -> t("titleEquipBtn")
                                                else -> t("titleLockedBadge")
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isEquipped -> Color.White
                                                unlocked -> EcoPrimary
                                                else -> EcoMutedForeground
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Menu items
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileMenuRow(
                    icon = Icons.Rounded.Settings,
                    label = t("profileSettings"),
                    onClick = { activeSheet = ProfileSheet.SETTINGS }
                )
                ProfileMenuRow(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    label = t("profileHelp"),
                    onClick = { activeSheet = ProfileSheet.HELP }
                )
                ProfileMenuRow(
                    icon = Icons.Rounded.Shield,
                    label = t("profileTerms"),
                    onClick = { activeSheet = ProfileSheet.TERMS }
                )
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, EcoCoral.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = EcoCoral.copy(alpha = 0.09f),
                    contentColor = EcoCoral
                )
            ) {
                Text(t("profileSignOut"), fontWeight = FontWeight.Bold)
            }
        }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (sheet) {
                    ProfileSheet.SETTINGS -> {
                        Text(t("profileSettingsTitle"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
                        Text(t("profileSettingsDescription"), fontSize = 13.sp, color = EcoMutedForeground)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(EcoSecondary)
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(t("profileLanguageTitle"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    AppLocale.VI to t("vietnamese"),
                                    AppLocale.EN to t("english"),
                                    AppLocale.KO to t("korean")
                                ).forEach { (loc, label) ->
                                    val selected = locale == loc
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) EcoPrimary else Color.White,
                                        border = BorderStroke(1.dp, if (selected) EcoPrimary else EcoBorder),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { onLocaleChange(loc) }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else EcoForeground,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(t("profileLanguageDescription"), fontSize = 11.sp, color = EcoMutedForeground)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Text(t("profileDataTitle"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = (if (isRemote) t("authRemoteNote") else t("authDemoNote")) + "\n" + t("profileDataDescription"),
                                fontSize = 12.sp,
                                color = EcoMutedForeground,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    ProfileSheet.HELP -> {
                        Text(t("profileHelpTitle"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
                        Text(t("profileHelpDescription"), fontSize = 13.sp, color = EcoMutedForeground)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(EcoSecondary)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(t("profileHelpStep1"), t("profileHelpStep2"), t("profileHelpStep3")).forEachIndexed { idx, step ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(EcoPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${idx + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(step, fontSize = 13.sp, color = EcoForeground)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(EcoPrimary.copy(alpha = 0.08f))
                                .border(1.dp, EcoPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Text(t("profileHelpContactTitle"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
                            Spacer(Modifier.height(4.dp))
                            Text(t("profileHelpContactDescription"), fontSize = 12.sp, color = EcoMutedForeground)
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO, "mailto:myanh260305@gmail.com".toUri())
                                    runCatching { context.startActivity(intent) }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EcoForeground)
                            ) {
                                Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("myanh260305@gmail.com", fontSize = 12.sp, color = EcoBackground)
                            }
                        }
                    }

                    ProfileSheet.TERMS -> {
                        Text(t("profileTermsTitle"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
                        Text(t("profileTermsDescription"), fontSize = 13.sp, color = EcoMutedForeground)

                        listOf(
                            t("profileTermsUseTitle") to t("profileTermsUseDescription"),
                            t("profileTermsDataTitle") to t("profileTermsDataDescription"),
                            t("profileTermsPrivacyTitle") to t("profileTermsPrivacyDescription")
                        ).forEach { (title, desc) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
                                Spacer(Modifier.height(4.dp))
                                Text(desc, fontSize = 12.sp, color = EcoMutedForeground, lineHeight = 17.sp)
                            }
                        }
                        Text(t("profileTermsContact"), fontSize = 11.sp, color = EcoMutedForeground)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImpactMetric(value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(EcoSecondary)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EcoPrimary)
        Spacer(Modifier.height(2.dp))
        Text(
            text = unit.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = EcoMutedForeground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    EcoCard(onClick = onClick, contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EcoSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EcoForeground, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = EcoMutedForeground)
        }
    }
}
