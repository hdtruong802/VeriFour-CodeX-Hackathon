package com.verifour.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Recycling
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.verifour.app.R
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.LanguageSwitcherRow
import com.verifour.app.ui.components.PlasticPatternBackdrop
import com.verifour.app.ui.components.UniversityStickers
import com.verifour.app.ui.theme.EcoBackground
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary

@Composable
fun LandingScreen(
    locale: AppLocale,
    onLocaleChange: (AppLocale) -> Unit,
    onNavigateAuth: () -> Unit,
    onNavigateMap: () -> Unit
) {
    val context = LocalContext.current
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    Scaffold(
        containerColor = EcoBackground
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            PlasticPatternBackdrop()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, EcoBorder, RoundedCornerShape(12.dp))
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.verifour_avatar),
                                    contentDescription = "VeriFour",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "VeriFour",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoForeground
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LanguageSwitcherRow(
                                currentLocale = locale,
                                onSelect = onLocaleChange
                            )
                            Surface(
                                shape = CircleShape,
                                color = EcoForeground,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onNavigateAuth() }
                            ) {
                                Text(
                                    text = t("landingLogin"),
                                    color = EcoBackground,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                // Hero Section
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EcoPrimary.copy(alpha = 0.1f))
                                .border(1.dp, EcoPrimary.copy(alpha = 0.25f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EcoPrimary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = t("landingHeroLabel").uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = t("landingTitleLine1"),
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoForeground
                        )
                        Text(
                            text = t("landingTitleLine2"),
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoPrimary
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = t("landingHeroDescription"),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = EcoMutedForeground
                        )

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onNavigateAuth,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EcoPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = t("landingStartNow"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateMap,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, EcoBorder)
                            ) {
                                Text(
                                    text = t("landingViewMap"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoForeground,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Pilot Hubs Card
                item {
                    EcoCard(borderColor = EcoPrimary.copy(alpha = 0.25f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(EcoPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = EcoPrimary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = t("landingPilotLabel").uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoPrimary
                                )
                                Text(
                                    text = t("landingPilotTitle"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoForeground
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = t("landingPilotDescription"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground,
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            COLLECTION_POINTS.forEachIndexed { index, point ->
                                val (name, address) = FlysticTranslations.localizedCollectionPoint(
                                    point.canonicalName,
                                    locale
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(EcoSecondary.copy(alpha = 0.65f))
                                        .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(EcoPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoForeground
                                        )
                                        Text(
                                            text = address,
                                            fontSize = 11.sp,
                                            color = EcoMutedForeground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // HUST x JBNU Collaboration Card
                item {
                    EcoCard {
                        Text(
                            text = t("landingCollaborationLabel").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = t("landingCollaborationTitle"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = t("landingCollaborationDescription"),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = EcoMutedForeground
                        )
                        Spacer(Modifier.height(14.dp))
                        UniversityStickers()
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(EcoPrimary.copy(alpha = 0.06f))
                                    .border(1.dp, EcoPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("🇻🇳", fontSize = 22.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = t("landingHustName"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoForeground
                                    )
                                    Text(
                                        text = t("landingHustCountry"),
                                        fontSize = 11.sp,
                                        color = EcoMutedForeground
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(EcoOcean.copy(alpha = 0.06f))
                                    .border(1.dp, EcoOcean.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("🇰🇷", fontSize = 22.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = t("landingJbnuName"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoForeground
                                    )
                                    Text(
                                        text = t("landingJbnuCountry"),
                                        fontSize = 11.sp,
                                        color = EcoMutedForeground
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(EcoSecondary)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = t("landingProjectFocusLabel").uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoMutedForeground
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = t("landingProjectFocus"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoForeground
                            )
                        }
                    }
                }

                // How it works (4 steps)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = t("landingHowItWorksLabel").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                        Text(
                            text = t("landingHowItWorksTitle"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )

                        val steps = listOf(
                            Triple("01", t("landingStep1Title"), t("landingStep1Description")),
                            Triple("02", t("landingStep2Title"), t("landingStep2Description")),
                            Triple("03", t("landingStep3Title"), t("landingStep3Description")),
                            Triple("04", t("landingStep4Title"), t("landingStep4Description"))
                        )
                        steps.forEach { (num, title, desc) ->
                            EcoCard {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(
                                        text = num,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EcoPrimary.copy(alpha = 0.35f)
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoForeground
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = desc,
                                            fontSize = 13.sp,
                                            color = EcoMutedForeground,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Capabilities
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = t("landingProjectLabel").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoOcean
                        )
                        Text(
                            text = t("landingAdminTitle"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                        Text(
                            text = t("landingAdminDescription"),
                            fontSize = 14.sp,
                            color = EcoMutedForeground
                        )

                        val caps: List<Triple<ImageVector, String, String>> = listOf(
                            Triple(Icons.Rounded.LocationOn, t("landingCapabilityMapTitle"), t("landingCapabilityMapDescription")),
                            Triple(Icons.Rounded.Recycling, t("landingCapabilitySortingTitle"), t("landingCapabilitySortingDescription")),
                            Triple(Icons.Rounded.QrCodeScanner, t("landingCapabilityQrTitle"), t("landingCapabilityQrDescription")),
                            Triple(Icons.Rounded.VolunteerActivism, t("landingCapabilityRewardTitle"), t("landingCapabilityRewardDescription"))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            caps.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { (icon, title, desc) ->
                                        EcoCard(
                                            modifier = Modifier.weight(1f),
                                            contentPadding = 14.dp
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(EcoPrimary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = EcoPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                text = title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EcoForeground
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = desc,
                                                fontSize = 12.sp,
                                                color = EcoMutedForeground,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = EcoForeground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "VeriFour",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoBackground
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = t("landingCollaborationSectionTitle"),
                                fontSize = 13.sp,
                                color = EcoBackground.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EcoCoral.copy(alpha = 0.18f))
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = "mailto:myanh260305@gmail.com".toUri()
                                        }
                                        runCatching { context.startActivity(intent) }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Email,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "myanh260305@gmail.com",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = t("landingFooterCopyright"),
                                fontSize = 11.sp,
                                color = EcoBackground.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}
