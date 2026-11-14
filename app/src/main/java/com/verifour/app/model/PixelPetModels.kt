package com.verifour.app.model

import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class PetPersonality(
    val id: String,
    val emoji: String,
    val nameKey: String,
    val descKey: String
) {
    CHEERFUL("cheerful", "☀️", "personalityCheerfulName", "personalityCheerfulDesc"),
    CHILL("chill", "🍵", "personalityChillName", "personalityChillDesc"),
    ENERGETIC("energetic", "⚡", "personalityEnergeticName", "personalityEnergeticDesc"),
    DREAMY("dreamy", "☁️", "personalityDreamyName", "personalityDreamyDesc");

    companion object {
        fun fromId(id: String?): PetPersonality =
            entries.firstOrNull { it.id == id } ?: CHEERFUL
    }
}

enum class PetReactionContext {
    PAT,
    FEED,
    SCAN,
    LEVEL_UP,
    WATER_TREE,
    CITY_BUILD,
    SKIN_EQUIP,
    BATTLE_WIN,
    BATTLE_LOSE
}

data class PetStoryChapter(
    val chapterNumber: Int,
    val unlockLevel: Int,
    val titleKey: String,
    val bodyKey: String
)

enum class PetSpecies(
    val id: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val nameKey: String,
    val descKey: String,
    val defaultPersonality: PetPersonality
) {
    SPROUTLING(
        id = "sproutling",
        primaryHex = 0xFF20A064,
        secondaryHex = 0xFF8BE8A8,
        nameKey = "petSproutlingName",
        descKey = "petSproutlingDesc",
        defaultPersonality = PetPersonality.CHEERFUL
    ),
    TURTLE(
        id = "turtle",
        primaryHex = 0xFF2B7AB8,
        secondaryHex = 0xFF72D6B8,
        nameKey = "petTurtleName",
        descKey = "petTurtleDesc",
        defaultPersonality = PetPersonality.CHILL
    ),
    FOX(
        id = "fox",
        primaryHex = 0xFFE87A42,
        secondaryHex = 0xFFFAD07A,
        nameKey = "petFoxName",
        descKey = "petFoxDesc",
        defaultPersonality = PetPersonality.ENERGETIC
    ),
    WHALE(
        id = "whale",
        primaryHex = 0xFF3898EC,
        secondaryHex = 0xFFA3E4FF,
        nameKey = "petWhaleName",
        descKey = "petWhaleDesc",
        defaultPersonality = PetPersonality.DREAMY
    );

    fun storyChapters(): List<PetStoryChapter> = listOf(
        PetStoryChapter(1, 1, "story_${id}_1_title", "story_${id}_1_body"),
        PetStoryChapter(2, 3, "story_${id}_2_title", "story_${id}_2_body"),
        PetStoryChapter(3, 6, "story_${id}_3_title", "story_${id}_3_body")
    )

    companion object {
        fun fromId(id: String?): PetSpecies =
            entries.firstOrNull { it.id == id } ?: SPROUTLING
    }
}

enum class PetStage(
    val minLevel: Int,
    val titleKey: String
) {
    BABY(1, "petStageBaby"),
    SPROUT(3, "petStageSprout"),
    GUARDIAN(6, "petStageGuardian"),
    LEGEND(10, "petStageLegend");

    companion object {
        fun fromLevel(level: Int): PetStage = when {
            level >= LEGEND.minLevel -> LEGEND
            level >= GUARDIAN.minLevel -> GUARDIAN
            level >= SPROUT.minLevel -> SPROUT
            else -> BABY
        }
    }
}

enum class PetSkin(
    val id: String,
    val emoji: String,
    val nameKey: String,
    val descKey: String,
    val seedCost: Int,
    val minLevel: Int,
    val primaryOverrideHex: Long?,
    val secondaryOverrideHex: Long?,
    val accentHex: Long
) {
    CLASSIC(
        id = "classic",
        emoji = "🌱",
        nameKey = "skinClassicName",
        descKey = "skinClassicDesc",
        seedCost = 0,
        minLevel = 1,
        primaryOverrideHex = null,
        secondaryOverrideHex = null,
        accentHex = 0xFFF5B82E
    ),
    SAKURA(
        id = "sakura",
        emoji = "🌸",
        nameKey = "skinSakuraName",
        descKey = "skinSakuraDesc",
        seedCost = 35,
        minLevel = 1,
        primaryOverrideHex = 0xFFE86F9A,
        secondaryOverrideHex = 0xFFFFD1E1,
        accentHex = 0xFFFF9EBB
    ),
    SOLAR_TECH(
        id = "solar_tech",
        emoji = "🕶️",
        nameKey = "skinSolarName",
        descKey = "skinSolarDesc",
        seedCost = 65,
        minLevel = 2,
        primaryOverrideHex = 0xFF0EA5A4,
        secondaryOverrideHex = 0xFF7DF9FF,
        accentHex = 0xFF38BDF8
    ),
    GOLDEN_LEAF(
        id = "golden_leaf",
        emoji = "👑",
        nameKey = "skinGoldenName",
        descKey = "skinGoldenDesc",
        seedCost = 100,
        minLevel = 3,
        primaryOverrideHex = 0xFFD97706,
        secondaryOverrideHex = 0xFFFDE68A,
        accentHex = 0xFFFFD700
    ),
    GALAXY_ECO(
        id = "galaxy_eco",
        emoji = "🌌",
        nameKey = "skinGalaxyName",
        descKey = "skinGalaxyDesc",
        seedCost = 150,
        minLevel = 4,
        primaryOverrideHex = 0xFF6D28D9,
        secondaryOverrideHex = 0xFFC4B5FD,
        accentHex = 0xFF34D399
    );

    companion object {
        fun fromId(id: String?): PetSkin =
            entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

enum class HabitatTheme(
    val id: String,
    val emoji: String,
    val nameKey: String,
    val skyHex: Long,
    val groundHex: Long,
    val cloudHex: Long
) {
    MEADOW("meadow", "🌿", "habitatMeadow", 0xFFDFF7EA, 0xFF20A064, 0xFFFFFFFF),
    SAKURA_GARDEN("sakura_garden", "🌸", "habitatSakura", 0xFFFFEAF2, 0xFFE86F9A, 0xFFFFF5F8),
    SUNSET_CAMPUS("sunset_campus", "🌇", "habitatSunset", 0xFFFFE7CE, 0xFFE87A42, 0xFFFFF4E6),
    CYBER_NIGHT("cyber_night", "🌃", "habitatCyber", 0xFF172536, 0xFF10B981, 0xFF67E8F9);

    companion object {
        fun fromId(id: String?): HabitatTheme =
            entries.firstOrNull { it.id == id } ?: MEADOW
    }
}

enum class EcoPersona(
    val id: String,
    val emoji: String,
    val nameKey: String,
    val missionKey: String
) {
    FOREST_KEEPER("forest_keeper", "🌳", "personaForestName", "personaForestMission"),
    CITY_ARCHITECT("city_architect", "🏙️", "personaCityName", "personaCityMission"),
    ARENA_CHAMPION("arena_champion", "⚔️", "personaArenaName", "personaArenaMission"),
    MASCOT_COLLECTOR("mascot_collector", "🎨", "personaCollectorName", "personaCollectorMission");

    companion object {
        fun fromId(id: String?): EcoPersona =
            entries.firstOrNull { it.id == id } ?: FOREST_KEEPER
    }
}

enum class CityBuildingType(
    val id: String,
    val emoji: String,
    val nameKey: String,
    val descKey: String,
    val baseSeedCost: Int,
    val maxLevel: Int = 3
) {
    ECO_FOREST("eco_forest", "🌳", "cityForestName", "cityForestDesc", 20),
    SOLAR_HUB("solar_hub", "☀️", "citySolarName", "citySolarDesc", 30),
    RECYCLE_LAB("recycle_lab", "♻️", "cityRecycleName", "cityRecycleDesc", 35),
    WIND_PARK("wind_park", "🌬️", "cityWindName", "cityWindDesc", 40),
    SKY_GARDEN("sky_garden", "🌿", "citySkyGardenName", "citySkyGardenDesc", 50);

    fun upgradeCost(currentLevel: Int): Int = baseSeedCost + currentLevel * 15
}

data class GreenCityState(
    val treeLevel: Int = 1,
    val treesPlanted: Int = 1,
    val ecoSeeds: Int = 55,
    val waterDrops: Int = 4,
    val buildingLevels: Map<String, Int>? = mapOf(CityBuildingType.ECO_FOREST.id to 1)
) {
    fun levelOf(type: CityBuildingType): Int =
        (buildingLevels.orEmpty()[type.id] ?: 0).coerceIn(0, type.maxLevel)

    fun totalBuildingLevels(): Int =
        CityBuildingType.entries.sumOf { levelOf(it) }

    fun greenIndexPercent(): Int {
        val maxTotal = CityBuildingType.entries.sumOf { it.maxLevel } + 10
        val current = totalBuildingLevels() + min(10, treesPlanted)
        return ((current.toFloat() / maxTotal.toFloat()) * 100f).toInt().coerceIn(5, 100)
    }

    fun cityBattlePowerBonus(): Int =
        totalBuildingLevels() * 3 + min(15, treesPlanted)
}

enum class ArenaTier(
    val id: String,
    val minRp: Int,
    val emoji: String,
    val nameKey: String,
    val colorHex: Long
) {
    BRONZE("bronze", 0, "🌱", "tierBronze", 0xFF8D6E63),
    SILVER("silver", 1100, "🌿", "tierSilver", 0xFF607D8B),
    GOLD("gold", 1300, "💎", "tierGold", 0xFFE5A100),
    DIAMOND("diamond", 1500, "🔥", "tierDiamond", 0xFF0EA5A4),
    LEGEND("legend", 1750, "👑", "tierLegend", 0xFF8B5CF6);

    companion object {
        fun fromRp(rp: Int): ArenaTier = when {
            rp >= LEGEND.minRp -> LEGEND
            rp >= DIAMOND.minRp -> DIAMOND
            rp >= GOLD.minRp -> GOLD
            rp >= SILVER.minRp -> SILVER
            else -> BRONZE
        }
    }
}

enum class BattleMove(
    val id: String,
    val emoji: String,
    val nameKey: String,
    val descKey: String
) {
    ECO_STRIKE("strike", "🍃", "battleMoveStrike", "battleMoveStrikeDesc"),
    RECYCLE_SHIELD("shield", "🛡️", "battleMoveShield", "battleMoveShieldDesc"),
    CLEANSE_BURST("burst", "⚡", "battleMoveBurst", "battleMoveBurstDesc")
}

data class ArenaOpponent(
    val userId: String,
    val name: String,
    val faculty: String,
    val petName: String,
    val species: PetSpecies,
    val stage: PetStage,
    val skin: PetSkin,
    val personality: PetPersonality,
    val level: Int,
    val rp: Int,
    val maxHp: Int,
    val attack: Int,
    val weeklyKg: Double = 0.0,
    val weeklyPoints: Int = 0,
    val equippedTitleId: String? = null
)

enum class TitleCategory(
    val id: String,
    val emoji: String,
    val nameKey: String
) {
    RECYCLING("recycling", "♻️", "titleCatRecycling"),
    WEEKLY_BATTLE("weekly_battle", "⚔️", "titleCatWeeklyBattle"),
    CAMPAIGN("campaign", "📣", "titleCatCampaign"),
    CITY_PET("city_pet", "🏙️", "titleCatCityPet")
}

data class EcoTitle(
    val id: String,
    val emoji: String,
    val category: TitleCategory,
    val nameKey: String,
    val descKey: String
)

val ECO_TITLES: List<EcoTitle> = listOf(
    EcoTitle("clean_saint", "✨", TitleCategory.RECYCLING, "titleCleanSaintName", "titleCleanSaintDesc"),
    EcoTitle("pet_king", "🧴", TitleCategory.RECYCLING, "titlePetKingName", "titlePetKingDesc"),
    EcoTitle("sorting_master", "♻️", TitleCategory.RECYCLING, "titleSortingMasterName", "titleSortingMasterDesc"),
    EcoTitle("weekly_rookie", "⚡", TitleCategory.WEEKLY_BATTLE, "titleWeeklyRookieName", "titleWeeklyRookieDesc"),
    EcoTitle("win_streaker", "🔥", TitleCategory.WEEKLY_BATTLE, "titleWinStreakerName", "titleWinStreakerDesc"),
    EcoTitle("weekly_champion", "👑", TitleCategory.WEEKLY_BATTLE, "titleWeeklyChampionName", "titleWeeklyChampionDesc"),
    EcoTitle("campaign_leader", "📣", TitleCategory.CAMPAIGN, "titleCampaignLeaderName", "titleCampaignLeaderDesc"),
    EcoTitle("eco_connector", "🤝", TitleCategory.CAMPAIGN, "titleEcoConnectorName", "titleEcoConnectorDesc"),
    EcoTitle("green_photographer", "📸", TitleCategory.CAMPAIGN, "titleGreenPhotographerName", "titleGreenPhotographerDesc"),
    EcoTitle("seed_planter", "🌱", TitleCategory.CITY_PET, "titleSeedPlanterName", "titleSeedPlanterDesc"),
    EcoTitle("eco_architect", "🏙️", TitleCategory.CITY_PET, "titleEcoArchitectName", "titleEcoArchitectDesc"),
    EcoTitle("pet_soulmate", "💖", TitleCategory.CITY_PET, "titlePetSoulmateName", "titlePetSoulmateDesc")
)

data class WeeklyRecyclingSummary(
    val weeklyKg: Double,
    val weeklyTxCount: Int,
    val weeklyTxPoints: Int,
    val weeklyBattleBonusPts: Int,
    val totalWeeklyScore: Int,
    val cleanRatio: Double,
    val distinctPlasticsCount: Int
)

data class InvitedCityMascot(
    val id: String,
    val ownerName: String,
    val mascotName: String,
    val species: PetSpecies = PetSpecies.SPROUTLING,
    val skin: PetSkin = PetSkin.CLASSIC,
    val plasticMascot: PlasticType? = null,
    val campaignId: String? = null
)

data class CampaignPhoto(
    val id: String,
    val authorUserId: String,
    val authorName: String,
    val caption: String,
    val plasticType: PlasticType = PlasticType.PET,
    val kg: Double = 0.5,
    val localImagePath: String? = null,
    val samplePhotoKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class CampaignParticipant(
    val userId: String,
    val name: String,
    val mascotName: String,
    val species: PetSpecies = PetSpecies.SPROUTLING,
    val skin: PetSkin = PetSkin.CLASSIC,
    val plasticMascot: PlasticType? = null,
    val kgRecycled: Double = 0.0,
    val points: Int = 0,
    val equippedTitleId: String? = null
)

data class EcoCampaign(
    val id: String,
    val title: String,
    val description: String,
    val inviteCode: String,
    val inviteLink: String,
    val targetKg: Double,
    val creatorId: String,
    val habitatTheme: HabitatTheme = HabitatTheme.MEADOW,
    val isEnded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val participants: List<CampaignParticipant>? = emptyList(),
    val photos: List<CampaignPhoto>? = emptyList()
) {
    fun safeParticipants(): List<CampaignParticipant> = participants ?: emptyList()

    fun safePhotos(): List<CampaignPhoto> = photos ?: emptyList()

    fun totalKg(): Double =
        ((safeParticipants().sumOf { it.kgRecycled }) * 100.0).roundToInt() / 100.0

    fun rankedParticipants(): List<CampaignParticipant> =
        safeParticipants().sortedWith(
            compareByDescending<CampaignParticipant> { it.points }
                .thenByDescending { it.kgRecycled }
        )

    fun photosOf(userId: String): List<CampaignPhoto> =
        safePhotos().filter { it.authorUserId == userId }
}

data class UserPersonalizationState(
    val petNickname: String = "",
    val personalityId: String? = null,
    val ecoPersonaId: String = EcoPersona.FOREST_KEEPER.id,
    val favoritePlasticCode: String = PlasticType.PET.code,
    val habitatThemeId: String = HabitatTheme.MEADOW.id,
    val unlockedSkinIds: Set<String>? = setOf(PetSkin.CLASSIC.id),
    val activeSkinId: String = PetSkin.CLASSIC.id,
    val petBond: Int = 25,
    val city: GreenCityState? = GreenCityState(),
    val arenaRp: Int = 1000,
    val arenaWins: Int = 0,
    val arenaLosses: Int = 0,
    val arenaStreak: Int = 0,
    val equippedTitleId: String? = null,
    val weeklyBattleBonusPts: Int = 0,
    val weeklyBattleKgBonus: Double = 0.0,
    val invitedMascotsInCity: List<InvitedCityMascot>? = emptyList()
) {
    fun safeUnlockedSkins(): Set<String> =
        (unlockedSkinIds ?: emptySet()) + PetSkin.CLASSIC.id

    fun safeCity(): GreenCityState = city ?: GreenCityState()

    fun safeInvitedMascots(): List<InvitedCityMascot> = invitedMascotsInCity ?: emptyList()

    fun resolvedPersonality(species: PetSpecies): PetPersonality =
        if (personalityId.isNullOrBlank()) species.defaultPersonality else PetPersonality.fromId(personalityId)

    fun resolvedSkin(): PetSkin =
        PetSkin.fromId(activeSkinId).let {
            if (it.id in safeUnlockedSkins()) it else PetSkin.CLASSIC
        }

    fun resolvedHabitat(): HabitatTheme = HabitatTheme.fromId(habitatThemeId)

    fun resolvedPersona(): EcoPersona = EcoPersona.fromId(ecoPersonaId)

    fun resolvedFavoritePlastic(): PlasticType = PlasticType.fromCode(favoritePlasticCode)

    fun resolvedTier(): ArenaTier = ArenaTier.fromRp(arenaRp)

    fun resolvedEquippedTitle(): EcoTitle? = PetProgression.titleById(equippedTitleId)
}

enum class PetAnimationEvent {
    IDLE,
    HAPPY_TAP,
    POINTS_ADDED,
    LEVEL_UP
}

enum class DuoInteractionType {
    INTRO,
    HIGH_FIVE,
    DANCE
}

data class PlasticMascotMeta(
    val plasticType: PlasticType,
    val primaryHex: Long,
    val secondaryHex: Long,
    val nameKey: String,
    val introKey: String,
    val cheerKey: String
)

val PLASTIC_MASCOTS: Map<PlasticType, PlasticMascotMeta> = mapOf(
    PlasticType.PET to PlasticMascotMeta(
        plasticType = PlasticType.PET,
        primaryHex = 0xFF2B95D6,
        secondaryHex = 0xFF9BE0FF,
        nameKey = "mascotPetName",
        introKey = "mascotPetIntro",
        cheerKey = "mascotPetCheer"
    ),
    PlasticType.HDPE to PlasticMascotMeta(
        plasticType = PlasticType.HDPE,
        primaryHex = 0xFF20A064,
        secondaryHex = 0xFF8BE8A8,
        nameKey = "mascotHdpeName",
        introKey = "mascotHdpeIntro",
        cheerKey = "mascotHdpeCheer"
    ),
    PlasticType.PP to PlasticMascotMeta(
        plasticType = PlasticType.PP,
        primaryHex = 0xFFE87A42,
        secondaryHex = 0xFFFFC599,
        nameKey = "mascotPpName",
        introKey = "mascotPpIntro",
        cheerKey = "mascotPpCheer"
    ),
    PlasticType.LDPE to PlasticMascotMeta(
        plasticType = PlasticType.LDPE,
        primaryHex = 0xFFE5A100,
        secondaryHex = 0xFFFFE596,
        nameKey = "mascotLdpeName",
        introKey = "mascotLdpeIntro",
        cheerKey = "mascotLdpeCheer"
    ),
    PlasticType.PS to PlasticMascotMeta(
        plasticType = PlasticType.PS,
        primaryHex = 0xFF8A65C9,
        secondaryHex = 0xFFD9C7FF,
        nameKey = "mascotPsName",
        introKey = "mascotPsIntro",
        cheerKey = "mascotPsCheer"
    )
)

data class PetCompanionState(
    val species: PetSpecies = PetSpecies.SPROUTLING,
    val level: Int = 1,
    val bonusXp: Int = 0,
    val lastCelebratedPoints: Int = 0,
    val patsCount: Int = 0
)

data class PetBattleStats(
    val maxHp: Int,
    val attack: Int,
    val defense: Int
)

object PetProgression {
    const val FEED_BONUS_XP = 15

    fun titleById(id: String?): EcoTitle? =
        if (id.isNullOrBlank()) null else ECO_TITLES.firstOrNull { it.id == id }

    fun currentWeekStartMs(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun computeWeeklySummary(
        state: FlysticState,
        userId: String,
        personalization: UserPersonalizationState,
        now: Long = System.currentTimeMillis()
    ): WeeklyRecyclingSummary {
        val weekStart = currentWeekStartMs(now)
        val weekTxs = state.transactions.filter {
            it.userId == userId &&
                effectiveTransactionStatus(it, now) == TransactionStatus.CONFIRMED &&
                (it.confirmedAt ?: it.createdAt) >= weekStart
        }
        val txKg = weekTxs.sumOf { it.weightKg }
        val txPts = weekTxs.sumOf { it.points }
        val allItems = weekTxs.flatMap { it.items }
        val cleanCount = allItems.count { it.cleanliness == Cleanliness.CLEAN }
        val cleanRatio = if (allItems.isEmpty()) 0.0 else cleanCount.toDouble() / allItems.size.toDouble()
        val distinctPlastics = allItems.map { it.plasticType }.toSet().size
        val weeklyKg = ((txKg + personalization.weeklyBattleKgBonus) * 100.0).roundToInt() / 100.0
        val totalWeeklyScore = txPts + personalization.weeklyBattleBonusPts
        return WeeklyRecyclingSummary(
            weeklyKg = weeklyKg,
            weeklyTxCount = weekTxs.size,
            weeklyTxPoints = txPts,
            weeklyBattleBonusPts = personalization.weeklyBattleBonusPts,
            totalWeeklyScore = totalWeeklyScore,
            cleanRatio = cleanRatio,
            distinctPlasticsCount = distinctPlastics
        )
    }

    fun evaluateUnlockedTitles(
        state: FlysticState,
        petState: PetCompanionState,
        personalization: UserPersonalizationState,
        campaigns: List<EcoCampaign> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): Set<String> {
        val userId = state.user?.id.orEmpty()
        val confirmedTxs = state.transactions.filter {
            it.userId == userId && effectiveTransactionStatus(it, now) == TransactionStatus.CONFIRMED
        }
        val allItems = confirmedTxs.flatMap { it.items }
        val cleanItems = allItems.count { it.cleanliness == Cleanliness.CLEAN }
        val cleanRatio = if (allItems.isEmpty()) 0.0 else cleanItems.toDouble() / allItems.size.toDouble()
        val petKg = allItems.filter { it.plasticType == PlasticType.PET }.sumOf { it.quantity }
        val distinctPlastics = allItems.map { it.plasticType }.toSet().size
        val weekly = computeWeeklySummary(state, userId, personalization, now)
        val city = personalization.safeCity()

        val allPhotos = campaigns.flatMap { it.safePhotos() }
        val userPhotosCount = allPhotos.count { it.authorUserId == userId }
        val invitedCount = personalization.safeInvitedMascots().size

        val unlocked = mutableSetOf<String>()
        if (cleanItems >= 1 && (cleanRatio >= 0.6 || state.totalKg >= 0.5)) unlocked.add("clean_saint")
        if (petKg >= 0.8 || allItems.count { it.plasticType == PlasticType.PET } >= 2) unlocked.add("pet_king")
        if (distinctPlastics >= 3 || state.totalKg >= 3.0) unlocked.add("sorting_master")

        if (weekly.totalWeeklyScore >= 20 || personalization.arenaWins >= 1) unlocked.add("weekly_rookie")
        if (personalization.arenaStreak >= 2 || personalization.arenaWins >= 3) unlocked.add("win_streaker")
        if (personalization.arenaRp >= 1060 || weekly.weeklyKg >= 2.0) unlocked.add("weekly_champion")

        if (campaigns.isNotEmpty()) unlocked.add("campaign_leader")
        if (invitedCount >= 2 || campaigns.any { it.safeParticipants().size >= 3 }) unlocked.add("eco_connector")
        if (userPhotosCount >= 1 || allPhotos.any { !it.localImagePath.isNullOrBlank() }) unlocked.add("green_photographer")

        if (city.treesPlanted >= 2 || city.treeLevel >= 3) unlocked.add("seed_planter")
        if (city.totalBuildingLevels() >= 3) unlocked.add("eco_architect")
        if (petState.level >= 3 || personalization.petBond >= 45) unlocked.add("pet_soulmate")
        return unlocked
    }

    fun xpThresholdForLevel(level: Int): Int {
        if (level <= 1) return 0
        var total = 0
        for (l in 1 until level) {
            total += 15 + l * 10
        }
        return total
    }

    fun totalXp(studentPoints: Int, petState: PetCompanionState): Int =
        max(0, studentPoints + petState.bonusXp)

    fun canUpgrade(studentPoints: Int, petState: PetCompanionState): Boolean =
        totalXp(studentPoints, petState) >= xpThresholdForLevel(petState.level + 1)

    fun maxReachableLevel(totalXp: Int): Int {
        var lvl = 1
        while (totalXp >= xpThresholdForLevel(lvl + 1) && lvl < 99) {
            lvl++
        }
        return lvl
    }

    fun progressInCurrentLevel(studentPoints: Int, petState: PetCompanionState): Triple<Int, Int, Float> {
        val xp = totalXp(studentPoints, petState)
        val currentFloor = xpThresholdForLevel(petState.level)
        val nextTarget = xpThresholdForLevel(petState.level + 1)
        val span = max(1, nextTarget - currentFloor)
        val earnedInLevel = max(0, xp - currentFloor)
        val fraction = (earnedInLevel.toFloat() / span.toFloat()).coerceIn(0f, 1f)
        return Triple(earnedInLevel, span, fraction)
    }

    fun battleStats(
        petState: PetCompanionState,
        personalization: UserPersonalizationState
    ): PetBattleStats {
        val cityBonus = personalization.safeCity().cityBattlePowerBonus()
        val bondBonus = personalization.petBond / 10
        val skinBonus = if (personalization.resolvedSkin() == PetSkin.CLASSIC) 0 else 4
        val personality = personalization.resolvedPersonality(petState.species)
        val hpBias = if (personality == PetPersonality.CHILL || personality == PetPersonality.DREAMY) 12 else 0
        val atkBias = if (personality == PetPersonality.ENERGETIC || personality == PetPersonality.CHEERFUL) 4 else 0
        return PetBattleStats(
            maxHp = 95 + petState.level * 12 + cityBonus + bondBonus + hpBias,
            attack = 18 + petState.level * 3 + (cityBonus / 3) + skinBonus + atkBias,
            defense = 6 + petState.level + (cityBonus / 4)
        )
    }

    fun reactionTranslationKey(
        personality: PetPersonality,
        context: PetReactionContext
    ): String {
        val p = personality.id
        return when (context) {
            PetReactionContext.PAT -> "react_${p}_pat"
            PetReactionContext.FEED -> "react_${p}_feed"
            PetReactionContext.SCAN -> "react_${p}_scan"
            PetReactionContext.LEVEL_UP -> "react_${p}_levelup"
            PetReactionContext.WATER_TREE -> "react_${p}_water"
            PetReactionContext.CITY_BUILD -> "react_${p}_build"
            PetReactionContext.SKIN_EQUIP -> "react_${p}_skin"
            PetReactionContext.BATTLE_WIN -> "react_${p}_win"
            PetReactionContext.BATTLE_LOSE -> "react_${p}_lose"
        }
    }

    fun buildArenaOpponents(
        leaderboardRows: List<LeaderboardRow>,
        currentUserId: String
    ): List<ArenaOpponent> {
        val defaultChallengers = listOf(
            ArenaOpponent(
                userId = "bot-linh",
                name = "Nguyễn Thảo Linh",
                faculty = "Trường CNTT&TT (SoICT)",
                petName = "Mầm B1",
                species = PetSpecies.SPROUTLING,
                stage = PetStage.SPROUT,
                skin = PetSkin.SAKURA,
                personality = PetPersonality.CHEERFUL,
                level = 3,
                rp = 980,
                maxHp = 128,
                attack = 24,
                weeklyKg = 2.4,
                weeklyPoints = 58,
                equippedTitleId = "clean_saint"
            ),
            ArenaOpponent(
                userId = "bot-minh",
                name = "Trần Quang Minh",
                faculty = "Trường Cơ khí",
                petName = "Boba Tank",
                species = PetSpecies.TURTLE,
                stage = PetStage.SPROUT,
                skin = PetSkin.SOLAR_TECH,
                personality = PetPersonality.CHILL,
                level = 4,
                rp = 1140,
                maxHp = 148,
                attack = 26,
                weeklyKg = 4.1,
                weeklyPoints = 96,
                equippedTitleId = "pet_king"
            ),
            ArenaOpponent(
                userId = "bot-an",
                name = "Lê Hoàng An",
                faculty = "Trường Hóa & KHSS",
                petName = "Cáo Tia Chớp",
                species = PetSpecies.FOX,
                stage = PetStage.GUARDIAN,
                skin = PetSkin.GOLDEN_LEAF,
                personality = PetPersonality.ENERGETIC,
                level = 6,
                rp = 1360,
                maxHp = 166,
                attack = 33,
                weeklyKg = 6.3,
                weeklyPoints = 148,
                equippedTitleId = "sorting_master"
            ),
            ArenaOpponent(
                userId = "bot-huy",
                name = "Phạm Gia Huy",
                faculty = "Trung tâm Đổi mới Sáng tạo",
                petName = "Voi Mây Bạc",
                species = PetSpecies.WHALE,
                stage = PetStage.LEGEND,
                skin = PetSkin.GALAXY_ECO,
                personality = PetPersonality.DREAMY,
                level = 9,
                rp = 1620,
                maxHp = 195,
                attack = 38,
                weeklyKg = 9.5,
                weeklyPoints = 225,
                equippedTitleId = "weekly_champion"
            )
        )

        val fromBoard = leaderboardRows
            .filter { it.userId != currentUserId }
            .mapIndexed { idx, row ->
                val hash = (row.userId.hashCode() and 0x7FFFFFFF) + idx
                val species = PetSpecies.entries[hash % PetSpecies.entries.size]
                val lvl = max(1, min(10, 1 + row.points / 35))
                val stage = PetStage.fromLevel(lvl)
                val skin = PetSkin.entries[hash % PetSkin.entries.size]
                val rp = 940 + row.points * 3 + (idx % 3) * 25
                val title = ECO_TITLES[hash % ECO_TITLES.size]
                ArenaOpponent(
                    userId = row.userId,
                    name = row.name,
                    faculty = row.faculty,
                    petName = "${row.name.split(" ").lastOrNull() ?: "Eco"} ${species.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    species = species,
                    stage = stage,
                    skin = skin,
                    personality = species.defaultPersonality,
                    level = lvl,
                    rp = rp,
                    maxHp = 95 + lvl * 12,
                    attack = 18 + lvl * 3,
                    weeklyKg = row.kg,
                    weeklyPoints = row.points,
                    equippedTitleId = title.id
                )
            }

        val merged = (fromBoard + defaultChallengers)
            .distinctBy { it.userId }
            .sortedByDescending { it.rp }
        return merged
    }
}
