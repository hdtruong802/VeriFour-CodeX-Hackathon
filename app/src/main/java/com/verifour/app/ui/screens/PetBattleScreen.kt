package com.verifour.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.verifour.app.model.ArenaOpponent
import com.verifour.app.model.BattleMove
import com.verifour.app.model.FlysticState
import com.verifour.app.model.PLASTIC_MASCOTS
import com.verifour.app.model.PetAnimationEvent
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetProgression
import com.verifour.app.model.PetStage
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.PixelPetSprite
import com.verifour.app.ui.components.PixelPlasticMascotSprite
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
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import java.util.Locale

private enum class BattlePhase {
    LOBBY,
    FIGHTING,
    FINISHED
}

@Composable
fun PetBattleScreen(
    state: FlysticState,
    petState: PetCompanionState,
    personalization: UserPersonalizationState,
    initialOpponentId: String? = null,
    onBattleFinished: (won: Boolean, rpDelta: Int, seedsEarned: Int, xpEarned: Int, weeklyPtsEarned: Int, weeklyKgEarned: Double) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val user = state.user ?: return
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    val opponents = remember(state.leaderboard, user.id) {
        PetProgression.buildArenaOpponents(state.leaderboard, user.id)
    }

    val weeklySummary = remember(state, user.id, personalization) {
        PetProgression.computeWeeklySummary(state, user.id, personalization)
    }
    val weeklyAtkBoost = remember(weeklySummary) {
        (weeklySummary.weeklyKg * 1.5).toInt().coerceAtMost(10) + (weeklySummary.cleanRatio * 4).toInt()
    }
    val baseStats = remember(petState, personalization) {
        PetProgression.battleStats(petState, personalization)
    }
    val playerStats = remember(baseStats, weeklyAtkBoost) {
        baseStats.copy(attack = baseStats.attack + weeklyAtkBoost)
    }
    val playerPetName = personalization.petNickname.ifBlank { t(petState.species.nameKey) }
    val playerStage = PetStage.fromLevel(petState.level)
    val playerSkin = personalization.resolvedSkin()
    val playerTier = personalization.resolvedTier()
    val equippedTitle = personalization.resolvedEquippedTitle()
    val habitat = personalization.resolvedHabitat()
    val favoritePlastic = personalization.resolvedFavoritePlastic()
    val assistMascotMeta = PLASTIC_MASCOTS.getValue(favoritePlastic)

    var phase by remember { mutableStateOf(BattlePhase.LOBBY) }
    var activeOpponent by remember { mutableStateOf<ArenaOpponent?>(null) }

    var playerHp by remember { mutableIntStateOf(playerStats.maxHp) }
    var enemyHp by remember { mutableIntStateOf(100) }
    var playerCharge by remember { mutableIntStateOf(0) }
    var assistUsed by remember { mutableStateOf(false) }
    var assistVisible by remember { mutableStateOf(false) }
    var isAnimatingTurn by remember { mutableStateOf(false) }
    var battleWon by remember { mutableStateOf(false) }
    var resultRpDelta by remember { mutableIntStateOf(0) }
    var resultSeeds by remember { mutableIntStateOf(0) }
    var resultXp by remember { mutableIntStateOf(0) }
    var resultWeeklyPts by remember { mutableIntStateOf(0) }
    var resultWeeklyKg by remember { mutableStateOf(0.0) }

    var playerEvent by remember { mutableStateOf(PetAnimationEvent.IDLE) }
    var playerEventTrigger by remember { mutableIntStateOf(0) }
    var enemyEvent by remember { mutableStateOf(PetAnimationEvent.IDLE) }
    var enemyEventTrigger by remember { mutableIntStateOf(0) }
    var enemyPopup by remember { mutableStateOf<String?>(null) }
    var playerPopup by remember { mutableStateOf<String?>(null) }

    val battleLog = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    val playerDashX = remember { Animatable(0f) }
    val enemyDashX = remember { Animatable(0f) }

    fun startBattle(opponent: ArenaOpponent) {
        activeOpponent = opponent
        playerHp = playerStats.maxHp
        enemyHp = opponent.maxHp
        playerCharge = 0
        assistUsed = false
        assistVisible = false
        isAnimatingTurn = false
        battleWon = false
        battleLog.clear()
        battleLog.add(t("battleLogStart", mapOf("pet" to playerPetName, "enemy" to opponent.petName)))
        phase = BattlePhase.FIGHTING
    }

    // Auto-start if launched directly with an opponent from Leaderboard
    val autoStartedRef = remember { mutableStateOf(false) }
    if (initialOpponentId != null && !autoStartedRef.value) {
        autoStartedRef.value = true
        val target = opponents.firstOrNull { it.userId == initialOpponentId } ?: opponents.firstOrNull()
        if (target != null) {
            startBattle(target)
        }
    }

    fun finishMatch(won: Boolean, opponent: ArenaOpponent) {
        battleWon = won
        val rpDelta = if (won) {
            val underdogBonus = if (opponent.rp > personalization.arenaRp) 10 else 0
            28 + underdogBonus
        } else {
            -12
        }
        val seeds = if (won) 25 else 8
        val xp = if (won) 20 else 6
        val weeklyPts = if (won) 28 else 10
        val weeklyKg = if (won) 0.5 else 0.2
        resultRpDelta = rpDelta
        resultSeeds = seeds
        resultXp = xp
        resultWeeklyPts = weeklyPts
        resultWeeklyKg = weeklyKg
        phase = BattlePhase.FINISHED
        onBattleFinished(won, rpDelta, seeds, xp, weeklyPts, weeklyKg)
    }

    fun executeTurn(move: BattleMove?, useAssist: Boolean = false) {
        val opponent = activeOpponent ?: return
        if (isAnimatingTurn || phase != BattlePhase.FIGHTING) return
        isAnimatingTurn = true

        scope.launch {
            var shieldActive = false

            if (useAssist && !assistUsed) {
                assistUsed = true
                assistVisible = true
                val heal = 18
                val dmg = (playerStats.attack * 0.9f).toInt() + Random.nextInt(4, 9)
                playerHp = (playerHp + heal).coerceAtMost(playerStats.maxHp)
                enemyHp = max(0, enemyHp - dmg)
                playerPopup = "+$heal HP"
                enemyPopup = "-$dmg"
                playerEvent = PetAnimationEvent.HAPPY_TAP
                playerEventTrigger++
                battleLog.add(
                    0,
                    t(
                        "battleLogAssist",
                        mapOf(
                            "mascot" to t(assistMascotMeta.nameKey),
                            "heal" to heal,
                            "dmg" to dmg
                        )
                    )
                )
                delay(550.milliseconds)
                assistVisible = false
            } else if (move != null) {
                when (move) {
                    BattleMove.ECO_STRIKE -> {
                        playerDashX.animateTo(26f, tween(130, easing = FastOutSlowInEasing))
                        val dmg = max(8, playerStats.attack + Random.nextInt(-2, 6))
                        enemyHp = max(0, enemyHp - dmg)
                        playerCharge = (playerCharge + 1).coerceAtMost(2)
                        enemyPopup = "-$dmg"
                        enemyEvent = PetAnimationEvent.HAPPY_TAP
                        enemyEventTrigger++
                        battleLog.add(
                            0,
                            t(
                                "battleLogPlayerHit",
                                mapOf("pet" to playerPetName, "move" to t(move.nameKey), "dmg" to dmg)
                            )
                        )
                        playerDashX.animateTo(0f, tween(160))
                    }

                    BattleMove.RECYCLE_SHIELD -> {
                        shieldActive = true
                        val heal = 10 + playerStats.defense / 2
                        playerHp = (playerHp + heal).coerceAtMost(playerStats.maxHp)
                        playerCharge = (playerCharge + 1).coerceAtMost(2)
                        playerPopup = "+$heal 🛡️"
                        playerEvent = PetAnimationEvent.HAPPY_TAP
                        playerEventTrigger++
                        battleLog.add(
                            0,
                            t("battleLogShield", mapOf("pet" to playerPetName, "heal" to heal))
                        )
                        delay(300.milliseconds)
                    }

                    BattleMove.CLEANSE_BURST -> {
                        playerCharge = 0
                        playerDashX.animateTo(34f, tween(140, easing = FastOutSlowInEasing))
                        val dmg = (playerStats.attack * 1.85f).toInt() + Random.nextInt(3, 10)
                        enemyHp = max(0, enemyHp - dmg)
                        enemyPopup = "⚡-$dmg"
                        playerEvent = PetAnimationEvent.POINTS_ADDED
                        playerEventTrigger++
                        enemyEvent = PetAnimationEvent.HAPPY_TAP
                        enemyEventTrigger++
                        battleLog.add(
                            0,
                            t("battleLogBurst", mapOf("pet" to playerPetName, "dmg" to dmg))
                        )
                        playerDashX.animateTo(0f, tween(180))
                    }
                }
            }

            if (enemyHp <= 0) {
                delay(380.milliseconds)
                isAnimatingTurn = false
                finishMatch(true, opponent)
                return@launch
            }

            // Enemy Turn
            delay(360.milliseconds)
            enemyDashX.animateTo(-26f, tween(140, easing = FastOutSlowInEasing))
            val rawEnemyDmg = max(6, opponent.attack - playerStats.defense / 3 + Random.nextInt(-3, 5))
            val finalDmg = if (shieldActive) max(3, (rawEnemyDmg * 0.35f).toInt()) else rawEnemyDmg
            playerHp = max(0, playerHp - finalDmg)
            playerPopup = "-$finalDmg"
            playerEvent = PetAnimationEvent.HAPPY_TAP
            playerEventTrigger++
            battleLog.add(0, t("battleLogEnemyHit", mapOf("enemy" to opponent.petName, "dmg" to finalDmg)))
            enemyDashX.animateTo(0f, tween(160))

            if (playerHp <= 0) {
                delay(380.milliseconds)
                isAnimatingTurn = false
                finishMatch(false, opponent)
                return@launch
            }

            isAnimatingTurn = false
        }
    }

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
                text = "⚔️ ${t("battleTitle")}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = t("battleSubtitle"),
                fontSize = 13.sp,
                color = EcoMutedForeground
            )
        }

        // Player Weekly Recycling Rank Card
        item {
            EcoCard(
                containerColor = Color(playerTier.colorHex).copy(alpha = 0.12f),
                borderColor = Color(playerTier.colorHex).copy(alpha = 0.4f),
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${playerTier.emoji} ${t(playerTier.nameKey)} · ${personalization.arenaRp} RP",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoForeground
                        )
                        if (equippedTitle != null) {
                            Text(
                                text = "${equippedTitle.emoji} ${t(equippedTitle.nameKey)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary
                            )
                        }
                        Text(
                            text = "$playerPetName (Lv.${petState.level}) · W:${personalization.arenaWins}/L:${personalization.arenaLosses} · 🔥 ${personalization.arenaStreak}",
                            fontSize = 11.sp,
                            color = EcoMutedForeground
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "HP ${playerStats.maxHp}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                        Text(
                            text = "ATK ${playerStats.attack} (+$weeklyAtkBoost)",
                            fontSize = 10.sp,
                            color = EcoMutedForeground
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.72f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(t("battleWeeklyKgLabel"), fontSize = 9.sp, color = EcoMutedForeground)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", weeklySummary.weeklyKg)} kg",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoForeground
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.72f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(t("battleWeeklyPtsLabel"), fontSize = 9.sp, color = EcoMutedForeground)
                            Text(
                                text = "${weeklySummary.totalWeeklyScore}đ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoPrimary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.72f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(t("battleWeeklyCleanLabel"), fontSize = 9.sp, color = EcoMutedForeground)
                            Text(
                                text = "${(weeklySummary.cleanRatio * 100).roundToInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoOcean
                            )
                        }
                    }
                }
            }
        }

        if (phase == BattlePhase.LOBBY) {
            item {
                Button(
                    onClick = {
                        val bestMatch = opponents.minByOrNull {
                            kotlin.math.abs(it.rp - personalization.arenaRp)
                        } ?: opponents.first()
                        startBattle(bestMatch)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                ) {
                    Text(
                        text = "⚡ ${t("battleQuickMatch")}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            items(opponents, key = { it.userId }) { opponent ->
                val tier = com.verifour.app.model.ArenaTier.fromRp(opponent.rp)
                val oppTitle = PetProgression.titleById(opponent.equippedTitleId)
                EcoCard(contentPadding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PixelPetSprite(
                            species = opponent.species,
                            stage = opponent.stage,
                            skin = opponent.skin,
                            spriteSize = 58.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${opponent.name} (${tier.emoji} ${opponent.rp} RP)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${opponent.petName} · ${String.format(Locale.US, "%.1f", opponent.weeklyKg)}kg/tuần · ${opponent.faculty}",
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
                        Button(
                            onClick = { startBattle(opponent) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoForeground),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t("battleChallengeBtn"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimaryGlow
                            )
                        }
                    }
                }
            }
        } else {
            val opponent = activeOpponent
            if (opponent != null) {
                // Live Pixel Battle Arena Stage
                item {
                    EcoCard(contentPadding = 14.dp) {
                        // HP Bars Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BattleHealthBar(
                                name = playerPetName,
                                hp = playerHp,
                                maxHp = playerStats.maxHp,
                                accentColor = EcoPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            BattleHealthBar(
                                name = opponent.petName,
                                hp = enemyHp,
                                maxHp = opponent.maxHp,
                                accentColor = EcoCoral,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Pixel Arena Stage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(166.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(habitat.skyHex))
                                .border(1.5.dp, EcoBorder, RoundedCornerShape(18.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val px = (size.height / 20f).coerceAtLeast(3f)
                                drawRect(
                                    color = Color(habitat.groundHex).copy(alpha = 0.28f),
                                    topLeft = Offset(0f, size.height - px * 4f),
                                    size = Size(size.width, px * 4f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PixelPetSprite(
                                    species = petState.species,
                                    stage = playerStage,
                                    skin = playerSkin,
                                    event = playerEvent,
                                    eventTriggerId = playerEventTrigger,
                                    eventBannerText = playerPopup,
                                    spriteSize = 98.dp,
                                    modifier = Modifier.graphicsLayer {
                                        translationX = playerDashX.value * density
                                    }
                                )

                                if (assistVisible) {
                                    PixelPlasticMascotSprite(
                                        plasticType = favoritePlastic,
                                        spriteSize = 62.dp,
                                        happyOverride = true,
                                        fastBob = true
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(EcoForeground.copy(alpha = 0.85f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "VS",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = EcoSunshine
                                        )
                                    }
                                }

                                PixelPetSprite(
                                    species = opponent.species,
                                    stage = opponent.stage,
                                    skin = opponent.skin,
                                    event = enemyEvent,
                                    eventTriggerId = enemyEventTrigger,
                                    eventBannerText = enemyPopup,
                                    spriteSize = 98.dp,
                                    modifier = Modifier.graphicsLayer {
                                        translationX = enemyDashX.value * density
                                    }
                                )
                            }
                        }

                        if (phase == BattlePhase.FIGHTING) {
                            Spacer(Modifier.height(10.dp))
                            // Charge Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ Energy: ${"●".repeat(playerCharge)}${"○".repeat(2 - playerCharge)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoOcean
                                )
                                Text(
                                    text = "${opponent.name} (${opponent.rp} RP)",
                                    fontSize = 11.sp,
                                    color = EcoMutedForeground
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Tactical Moves
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BattleMoveButton(
                                    emoji = BattleMove.ECO_STRIKE.emoji,
                                    label = t(BattleMove.ECO_STRIKE.nameKey),
                                    enabled = !isAnimatingTurn,
                                    containerColor = EcoPrimary,
                                    contentColor = Color.White,
                                    modifier = Modifier.weight(1f),
                                    onClick = { executeTurn(BattleMove.ECO_STRIKE) }
                                )
                                BattleMoveButton(
                                    emoji = BattleMove.RECYCLE_SHIELD.emoji,
                                    label = t(BattleMove.RECYCLE_SHIELD.nameKey),
                                    enabled = !isAnimatingTurn,
                                    containerColor = EcoOcean,
                                    contentColor = Color.White,
                                    modifier = Modifier.weight(1f),
                                    onClick = { executeTurn(BattleMove.RECYCLE_SHIELD) }
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BattleMoveButton(
                                    emoji = BattleMove.CLEANSE_BURST.emoji,
                                    label = t(BattleMove.CLEANSE_BURST.nameKey),
                                    enabled = !isAnimatingTurn && playerCharge >= 2,
                                    containerColor = EcoSunshine,
                                    contentColor = EcoForeground,
                                    modifier = Modifier.weight(1f),
                                    onClick = { executeTurn(BattleMove.CLEANSE_BURST) }
                                )
                                BattleMoveButton(
                                    emoji = favoritePlastic.emoji,
                                    label = t("battleAssistBtn", mapOf("name" to favoritePlastic.code)),
                                    enabled = !isAnimatingTurn && !assistUsed,
                                    containerColor = Color(assistMascotMeta.primaryHex),
                                    contentColor = Color.White,
                                    modifier = Modifier.weight(1f),
                                    onClick = { executeTurn(null, useAssist = true) }
                                )
                            }
                        } else if (phase == BattlePhase.FINISHED) {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (battleWon) EcoPrimary.copy(alpha = 0.14f) else EcoCoral.copy(alpha = 0.14f))
                                    .padding(14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (battleWon) "🏆 ${t("battleVictoryTitle")}" else "💫 ${t("battleDefeatTitle")}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (battleWon) EcoPrimary else EcoCoral,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    val rpSign = if (resultRpDelta >= 0) "+$resultRpDelta" else "$resultRpDelta"
                                    Text(
                                        text = "$rpSign RP · +$resultWeeklyPts đ Tuần (+${resultWeeklyKg}kg) · +$resultSeeds 🌱 · +$resultXp XP",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoForeground,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { phase = BattlePhase.LOBBY },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(t("battlePlayAgain"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onOpenLeaderboard,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                                ) {
                                    Text(t("battleBackLeaderboard"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Combat Log
                item {
                    EcoCard(containerColor = EcoSecondary.copy(alpha = 0.6f), contentPadding = 12.dp) {
                        battleLog.take(4).forEachIndexed { idx, line ->
                            Text(
                                text = "• $line",
                                fontSize = 12.sp,
                                fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (idx == 0) EcoForeground else EcoMutedForeground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleHealthBar(
    name: String,
    hp: Int,
    maxHp: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$hp/$maxHp",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(EcoSecondary)
        ) {
            val frac = (hp.toFloat() / maxHp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
private fun BattleMoveButton(
    emoji: String,
    label: String,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) containerColor else EcoSecondary,
        border = BorderStroke(1.dp, if (enabled) containerColor else EcoBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 14.sp)
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) contentColor else EcoMutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
