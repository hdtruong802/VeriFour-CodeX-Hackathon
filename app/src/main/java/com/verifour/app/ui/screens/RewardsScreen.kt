package com.verifour.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.verifour.app.R
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.FlysticState
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.theme.EcoBackground
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine

private enum class CommunityView {
    CHOICES,
    REWARDS,
    FUNDRAISING
}

@Composable
fun RewardsScreen(
    state: FlysticState,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var view by remember { mutableStateOf(CommunityView.CHOICES) }

    val formattedPoints = FlysticTranslations.formatNumber(state.points, locale)
    val formattedFundPoints = FlysticTranslations.formatNumber(state.fundPoints, locale)
    val formattedRewardPoints = FlysticTranslations.formatNumber(state.rewardPoints, locale)

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
        if (view != CommunityView.CHOICES) {
            item {
                Row(
                    modifier = Modifier
                        .clickable { view = CommunityView.CHOICES }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = EcoMutedForeground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = t("communityBackToChoices"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoMutedForeground
                    )
                }
            }
        }

        when (view) {
            CommunityView.CHOICES -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EcoForeground, EcoPrimary)
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(EcoPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎁", fontSize = 26.sp)
                            }
                            Spacer(Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = t("communityLabel").uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = t("communityTitle"),
                                fontSize = 26.sp,
                                lineHeight = 31.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = t("communityDescription"),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                item {
                    EcoCard(
                        containerColor = EcoPrimary.copy(alpha = 0.09f),
                        borderColor = EcoPrimary.copy(alpha = 0.2f),
                        contentPadding = 18.dp
                    ) {
                        Text(
                            text = t("communityPointsLabel").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formattedPoints,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoForeground
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = t("studentPoints"),
                                fontSize = 15.sp,
                                color = EcoMutedForeground,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = t("communityPointsDescription"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BalanceSubCard(
                                label = t("communityFundPointsLabel"),
                                value = formattedFundPoints,
                                modifier = Modifier.weight(1f)
                            )
                            BalanceSubCard(
                                label = t("communityRewardPointsLabel"),
                                value = formattedRewardPoints,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = t("communityChooseTitle"),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = t("communityChooseDescription"),
                        fontSize = 13.sp,
                        color = EcoMutedForeground
                    )
                    Spacer(Modifier.height(12.dp))
                    ChoiceCard(
                        icon = Icons.Rounded.CardGiftcard,
                        iconTint = EcoPrimary,
                        iconBg = EcoPrimary.copy(alpha = 0.12f),
                        title = t("communityRewardsOption"),
                        description = t("communityRewardsOptionDescription"),
                        onClick = { view = CommunityView.REWARDS }
                    )
                    Spacer(Modifier.height(10.dp))
                    ChoiceCard(
                        icon = Icons.Rounded.VolunteerActivism,
                        iconTint = EcoCoral,
                        iconBg = EcoCoral.copy(alpha = 0.12f),
                        title = t("communityFundraisingOption"),
                        description = t("communityFundraisingOptionDescription"),
                        onClick = { view = CommunityView.FUNDRAISING }
                    )
                }
            }

            CommunityView.REWARDS -> {
                val cashCount = state.rewardPoints / 1000
                val treeCount = state.rewardPoints / 500
                item {
                    EcoCard(
                        containerColor = EcoPrimary.copy(alpha = 0.09f),
                        borderColor = EcoPrimary.copy(alpha = 0.2f),
                        contentPadding = 18.dp
                    ) {
                        Text(
                            text = t("communityRewardsTitle").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = t("communityRewardsDescription"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground,
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = t("communityRewardBalance", mapOf("points" to formattedRewardPoints)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                    }
                }

                item {
                    RewardDetailCard(
                        icon = Icons.Rounded.Payments,
                        iconBg = EcoSunshine.copy(alpha = 0.25f),
                        iconTint = EcoForeground,
                        title = t("communityCashReward"),
                        detail = t("communityCashRewardDetail"),
                        availability = t("communityRewardCashAvailable", mapOf("count" to cashCount))
                    )
                }

                item {
                    RewardDetailCard(
                        icon = Icons.Rounded.Park,
                        iconBg = EcoPrimary.copy(alpha = 0.12f),
                        iconTint = EcoPrimary,
                        title = t("communityTreeReward"),
                        detail = t("communityTreeRewardDetail"),
                        availability = t("communityRewardTreesAvailable", mapOf("count" to treeCount))
                    )
                }

                item {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:myanh260305@gmail.com?subject=FLYSTIC%20-%20Redeem".toUri()
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoForeground)
                    ) {
                        Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t("communityRewardContact"), fontWeight = FontWeight.Bold, color = EcoBackground)
                    }
                }
            }

            CommunityView.FUNDRAISING -> {
                item {
                    Text(
                        text = t("communityFundraisingTeams").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    EcoCard(
                        containerColor = EcoPrimary.copy(alpha = 0.06f),
                        borderColor = EcoPrimary.copy(alpha = 0.22f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EcoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤝", fontSize = 22.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = t("communityVitTeam"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = t("communityVitTeamDescription"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground,
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${t("communityFundPointsLabel")}: $formattedFundPoints ${t("studentPoints")}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                    }
                }

                item {
                    Text(
                        text = t("communityAboutLabel").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = t("communityAboutTitle"),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = t("communityAboutDescription"),
                        fontSize = 13.sp,
                        color = EcoMutedForeground,
                        lineHeight = 19.sp
                    )
                }

                item {
                    Text(
                        text = t("communityMissionTitle"),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(10.dp))
                    val missions = listOf(
                        Triple(Icons.Rounded.VolunteerActivism, t("communityMissionServiceTitle"), t("communityMissionServiceDescription")),
                        Triple(Icons.Rounded.Groups, t("communityMissionConnectionTitle"), t("communityMissionConnectionDescription")),
                        Triple(Icons.Rounded.AutoAwesome, t("communityMissionSkillsTitle"), t("communityMissionSkillsDescription")),
                        Triple(Icons.Rounded.Code, t("communityMissionTechnologyTitle"), t("communityMissionTechnologyDescription"))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        missions.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { (icon, title, desc) ->
                                    EcoCard(modifier = Modifier.weight(1f), contentPadding = 14.dp) {
                                        Icon(icon, contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
                                        Spacer(Modifier.height(3.dp))
                                        Text(desc, fontSize = 11.sp, color = EcoMutedForeground, lineHeight = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = t("communityActivitiesTitle"),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = t("communityActivitiesDescription"),
                        fontSize = 13.sp,
                        color = EcoMutedForeground
                    )
                    Spacer(Modifier.height(10.dp))
                    EcoCard(contentPadding = 0.dp) {
                        Image(
                            painter = painterResource(id = R.drawable.soict_volunteer_festival),
                            contentDescription = t("communityActivitySummerTitle"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = t("communityActivitySummerTitle"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = t("communityActivitySummerDescription"),
                                fontSize = 13.sp,
                                color = EcoMutedForeground
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://www.facebook.com/doitinhnguyen.soict?locale=vi_VN".toUri()
                            )
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoOcean)
                    ) {
                        Text(t("communityFollowCta"), fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceSubCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.9f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = EcoMutedForeground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    EcoCard(onClick = onClick, contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
                Spacer(Modifier.height(3.dp))
                Text(description, fontSize = 12.sp, color = EcoMutedForeground, lineHeight = 17.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = EcoMutedForeground,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RewardDetailCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    detail: String,
    availability: String
) {
    EcoCard(contentPadding = 18.dp) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EcoForeground)
        Spacer(Modifier.height(4.dp))
        Text(detail, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EcoPrimary)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EcoSecondary)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(availability, fontSize = 12.sp, color = EcoMutedForeground)
        }
    }
}
