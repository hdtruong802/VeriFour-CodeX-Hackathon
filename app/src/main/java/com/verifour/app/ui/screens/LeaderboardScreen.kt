package com.verifour.app.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.ArenaTier
import com.verifour.app.model.FlysticState
import com.verifour.app.model.LeaderboardRow
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetProgression
import com.verifour.app.model.PetStage
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.model.round2
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.PixelPetSprite
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoLeaf
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine
import java.util.Calendar
import java.util.Locale

private enum class LeaderboardTab {
    WEEK,
    ALL,
    FACULTY,
    ARENA
}

@Composable
fun LeaderboardScreen(
    state: FlysticState,
    isRemote: Boolean,
    petState: PetCompanionState,
    personalization: UserPersonalizationState,
    onFetchLeaderboard: suspend (String) -> List<LeaderboardRow>,
    onOpenBattle: (String?) -> Unit,
    onOpenCampaigns: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val user = state.user ?: return
    val locale = state.locale
    fun t(key: String) = FlysticTranslations.t(key, locale)
    val weeklySummary = remember(state, user.id, personalization) {
        PetProgression.computeWeeklySummary(state, user.id, personalization)
    }

    var tab by remember { mutableStateOf(LeaderboardTab.WEEK) }
    var remoteRows by remember { mutableStateOf(state.leaderboard) }

    LaunchedEffect(tab, isRemote) {
        if (isRemote) {
            val period = if (tab == LeaderboardTab.WEEK) "week" else "all"
            remoteRows = runCatching { onFetchLeaderboard(period) }.getOrElse { emptyList() }
        }
    }

    val startOfWeekMs = remember {
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val rows = remember(state, remoteRows, tab, isRemote, user) {
        if (isRemote) {
            val map = remoteRows.associateBy { it.userId }.toMutableMap()
            if (!map.containsKey(user.id)) {
                map[user.id] = LeaderboardRow(
                    userId = user.id,
                    name = user.name,
                    faculty = user.faculty ?: "Khác",
                    points = if (tab == LeaderboardTab.WEEK) 0 else state.points,
                    kg = if (tab == LeaderboardTab.WEEK) 0.0 else state.totalKg
                )
            }
            map.values.sortedWith(compareByDescending<LeaderboardRow> { it.points }.thenByDescending { it.kg })
        } else {
            val from = if (tab == LeaderboardTab.WEEK) startOfWeekMs else 0L
            val map = linkedMapOf<String, LeaderboardRow>()
            state.transactions
                .filter {
                    effectiveTransactionStatus(it) == TransactionStatus.CONFIRMED &&
                        it.userId != null &&
                        (it.confirmedAt ?: it.createdAt) >= from
                }
                .forEach { tx ->
                    val uid = tx.userId!!
                    val existing = map[uid]
                    map[uid] = LeaderboardRow(
                        userId = uid,
                        name = tx.userName ?: "Sinh viên FLYSTIC",
                        faculty = tx.userFaculty ?: "Khác",
                        points = (existing?.points ?: 0) + tx.points,
                        kg = round2((existing?.kg ?: 0.0) + tx.weightKg)
                    )
                }
            if (!map.containsKey(user.id)) {
                map[user.id] = LeaderboardRow(
                    userId = user.id,
                    name = user.name,
                    faculty = user.faculty ?: "Khác",
                    points = if (tab == LeaderboardTab.WEEK) weeklySummary.totalWeeklyScore else state.points,
                    kg = if (tab == LeaderboardTab.WEEK) weeklySummary.weeklyKg else state.totalKg
                )
            } else if (tab == LeaderboardTab.WEEK) {
                val cur = map.getValue(user.id)
                map[user.id] = cur.copy(
                    points = cur.points + personalization.weeklyBattleBonusPts,
                    kg = round2(cur.kg + personalization.weeklyBattleKgBonus)
                )
            }
            map.values.sortedWith(compareByDescending<LeaderboardRow> { it.points }.thenByDescending { it.kg })
        }
    }

    val facultyRows = remember(rows) {
        val grouped = linkedMapOf<String, Triple<String, Int, MutableSet<String>>>()
        rows.forEach { row ->
            val current = grouped[row.faculty] ?: Triple(row.faculty, 0, mutableSetOf())
            current.third.add(row.userId)
            grouped[row.faculty] = Triple(row.faculty, current.second + row.points, current.third)
        }
        grouped.values.sortedByDescending { it.second }
    }

    val myRank = rows.indexOfFirst { it.userId == user.id } + 1
    val top3 = rows.take(3)
    val rest = if (rows.size > 3) rows.drop(3) else emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = t("backHome"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoMutedForeground,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "🏆 ${t("studentLeaderboard")}",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = t("leaderboardDescription"),
                fontSize = 14.sp,
                color = EcoMutedForeground
            )
        }

        // Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .clip(CircleShape)
                    .background(EcoSecondary)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    LeaderboardTab.WEEK to t("leaderboardWeek"),
                    LeaderboardTab.ALL to t("leaderboardAll"),
                    LeaderboardTab.FACULTY to t("leaderboardFaculty"),
                    LeaderboardTab.ARENA to t("battleRankTab")
                ).forEach { (itemTab, label) ->
                    val selected = tab == itemTab
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) EcoForeground else Color.Transparent)
                            .clickable { tab = itemTab }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else EcoMutedForeground
                        )
                    }
                }
            }
        }

        // Quick Weekly Battle & Campaigns Banner
        if (tab != LeaderboardTab.ARENA) {
            item {
                val myTier = personalization.resolvedTier()
                EcoCard(
                    containerColor = EcoForeground,
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${myTier.emoji} ${t(myTier.nameKey)} · ${personalization.arenaRp} RP",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoPrimaryGlow
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", weeklySummary.weeklyKg)} kg tuần · W:${personalization.arenaWins}/L:${personalization.arenaLosses}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onOpenCampaigns,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EcoSunshine,
                                    contentColor = EcoForeground
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "📣 ${t("campaignNavBtn")}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { onOpenBattle(null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EcoPrimary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "⚔️ ${t("battleButton")}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (tab == LeaderboardTab.ARENA) {
            val myTier = personalization.resolvedTier()
            val myPetName = personalization.petNickname.ifBlank { t(petState.species.nameKey) }
            val arenaRivals = PetProgression.buildArenaOpponents(rows, user.id)
            val arenaBoard = (
                arenaRivals.map { it to false } +
                    listOf(
                        com.verifour.app.model.ArenaOpponent(
                            userId = user.id,
                            name = user.name,
                            faculty = user.faculty ?: "HUST",
                            petName = myPetName,
                            species = petState.species,
                            stage = PetStage.fromLevel(petState.level),
                            skin = personalization.resolvedSkin(),
                            personality = personalization.resolvedPersonality(petState.species),
                            level = petState.level,
                            rp = personalization.arenaRp,
                            maxHp = 100,
                            attack = 20,
                            weeklyKg = weeklySummary.weeklyKg,
                            weeklyPoints = weeklySummary.totalWeeklyScore,
                            equippedTitleId = personalization.equippedTitleId
                        ) to true
                    )
                ).sortedByDescending { it.first.rp }
            val myArenaRank = arenaBoard.indexOfFirst { it.second } + 1

            item {
                EcoCard(
                    containerColor = Color(myTier.colorHex).copy(alpha = 0.14f),
                    borderColor = Color(myTier.colorHex).copy(alpha = 0.45f),
                    contentPadding = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${t("battleYourRankCard")}: #$myArenaRank",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoMutedForeground
                            )
                            Text(
                                text = "${myTier.emoji} ${t(myTier.nameKey)} · ${personalization.arenaRp} RP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoForeground
                            )
                            Text(
                                text = "W:${personalization.arenaWins} / L:${personalization.arenaLosses} · 🔥 ${personalization.arenaStreak}",
                                fontSize = 11.sp,
                                color = EcoMutedForeground
                            )
                        }
                        Button(
                            onClick = { onOpenBattle(null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                        ) {
                            Text(
                                text = "⚔️ ${t("battleButton")}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            itemsIndexed(arenaBoard, key = { _, pair -> pair.first.userId }) { idx, (opp, isMe) ->
                val tier = ArenaTier.fromRp(opp.rp)
                val oppTitle = PetProgression.titleById(opp.equippedTitleId)
                EcoCard(
                    containerColor = if (isMe) EcoPrimary.copy(alpha = 0.08f) else Color.White,
                    borderColor = if (isMe) EcoPrimary else EcoBorder,
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (idx < 3) EcoPrimary else EcoMutedForeground,
                            modifier = Modifier.width(26.dp)
                        )
                        PixelPetSprite(
                            species = opp.species,
                            stage = opp.stage,
                            skin = opp.skin,
                            spriteSize = 46.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMe) "${opp.name} (Bạn)" else opp.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${tier.emoji} ${opp.rp} RP · ${String.format(Locale.US, "%.1f", opp.weeklyKg)}kg/tuần · Lv.${opp.level}",
                                fontSize = 11.sp,
                                color = EcoMutedForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (oppTitle != null) {
                                Text(
                                    text = "${oppTitle.emoji} ${t(oppTitle.nameKey)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoPrimary
                                )
                            }
                        }
                        if (!isMe) {
                            Button(
                                onClick = { onOpenBattle(opp.userId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EcoForeground),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "⚔️ ${t("battleChallengeBtn")}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoPrimaryGlow
                                )
                            }
                        }
                    }
                }
            }
        } else if (tab != LeaderboardTab.FACULTY) {
            if (top3.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        listOf(1, 0, 2).forEach { podiumIdx ->
                            val row = top3.getOrNull(podiumIdx)
                            if (row == null) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val height: Dp = when (podiumIdx) {
                                    0 -> 112.dp
                                    1 -> 84.dp
                                    else -> 68.dp
                                }
                                val tone = when (podiumIdx) {
                                    0 -> EcoSunshine
                                    1 -> EcoLeaf.copy(alpha = 0.45f)
                                    else -> EcoCoral.copy(alpha = 0.42f)
                                }
                                val medal = when (podiumIdx) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    else -> "🥉"
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(medal, fontSize = 22.sp)
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(2.dp, EcoPrimary.copy(alpha = 0.35f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (row.userId == user.id) "🧑" else "♻️",
                                            fontSize = 22.sp
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = row.name.split(" ").takeLast(2).joinToString(" "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoForeground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${FlysticTranslations.formatNumber(row.points, locale)}đ",
                                        fontSize = 10.sp,
                                        color = EcoMutedForeground
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(height)
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                            .background(tone),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${podiumIdx + 1}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = EcoForeground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Your Rank Card
            item {
                val myRow = rows.firstOrNull { it.userId == user.id }
                EcoCard(
                    containerColor = EcoPrimary.copy(alpha = 0.08f),
                    borderColor = EcoPrimary.copy(alpha = 0.35f),
                    contentPadding = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EcoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$myRank",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("leaderboardYourRank"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground
                            )
                            Text(
                                text = "${FlysticTranslations.formatNumber(myRow?.points ?: 0, locale)} ${t("studentPoints")} · ${String.format(Locale.US, "%.1f", myRow?.kg ?: 0.0)} kg",
                                fontSize = 12.sp,
                                color = EcoMutedForeground
                            )
                        }
                        Text("🌟", fontSize = 22.sp)
                    }
                }
            }

            itemsIndexed(rest, key = { _, item -> item.userId }) { idx, row ->
                val rank = idx + 4
                val isMe = row.userId == user.id
                EcoCard(
                    containerColor = if (isMe) EcoPrimary.copy(alpha = 0.07f) else Color.White,
                    borderColor = if (isMe) EcoPrimary.copy(alpha = 0.4f) else EcoBorder,
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$rank",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoMutedForeground,
                            modifier = Modifier.width(26.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EcoSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isMe) "🧑" else "♻️", fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoForeground
                            )
                            Text(
                                text = row.faculty,
                                fontSize = 11.sp,
                                color = EcoMutedForeground
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${FlysticTranslations.formatNumber(row.points, locale)}đ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", row.kg)} kg",
                                fontSize = 11.sp,
                                color = EcoMutedForeground
                            )
                        }
                    }
                }
            }
        } else {
            itemsIndexed(facultyRows) { index, (facultyName, pts, members) ->
                val badgeBg = when (index) {
                    0 -> EcoSunshine
                    1 -> EcoLeaf.copy(alpha = 0.45f)
                    2 -> EcoCoral.copy(alpha = 0.4f)
                    else -> EcoSecondary
                }
                EcoCard(contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = facultyName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground
                            )
                            Text(
                                text = "${members.size} ${t("leaderboardMembers")}",
                                fontSize = 12.sp,
                                color = EcoMutedForeground
                            )
                        }
                        Text(
                            text = "${FlysticTranslations.formatNumber(pts, locale)}đ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                    }
                }
            }
        }

        // Tips Card
        item {
            EcoCard(
                containerColor = EcoPrimary.copy(alpha = 0.1f),
                borderColor = EcoPrimary.copy(alpha = 0.25f),
                contentPadding = 16.dp
            ) {
                Text(
                    text = "💡 ${t("leaderboardTipsTitle")}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(8.dp))
                listOf(
                    t("leaderboardTipCleanDry"),
                    t("leaderboardTipPrioritize"),
                    t("leaderboardTipInviteFriends")
                ).forEach { tip ->
                    Text(
                        text = "• $tip",
                        fontSize = 12.sp,
                        color = EcoMutedForeground,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
