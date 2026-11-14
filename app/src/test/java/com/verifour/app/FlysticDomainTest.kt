package com.verifour.app

import com.verifour.app.data.DetectionVerdict
import com.verifour.app.data.OpenAiPlasticDetector
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.ArenaTier
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.CampaignParticipant
import com.verifour.app.model.CampaignPhoto
import com.verifour.app.model.CityBuildingType
import com.verifour.app.model.Cleanliness
import com.verifour.app.model.CollectionRun
import com.verifour.app.model.DuoInteractionType
import com.verifour.app.model.ECO_TITLES
import com.verifour.app.model.EcoCampaign
import com.verifour.app.model.FlysticState
import com.verifour.app.model.GreenCityState
import com.verifour.app.model.InvitedCityMascot
import com.verifour.app.model.PLASTIC_MASCOTS
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetPersonality
import com.verifour.app.model.PetProgression
import com.verifour.app.model.PetReactionContext
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PetStage
import com.verifour.app.model.PlasticType
import com.verifour.app.model.Role
import com.verifour.app.model.TitleCategory
import com.verifour.app.model.Transaction
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.TxItem
import com.verifour.app.model.User
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.model.calculatePoints
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.model.getStationFillKg
import com.verifour.app.model.getStationFillPercent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlysticDomainTest {

    @Test
    fun calculatePoints_matchesFormula() {
        // PET (10) * 1.0 kg * CLEAN (1.5) = 15
        assertEquals(15, calculatePoints(1.0, PlasticType.PET, Cleanliness.CLEAN))
        // PP (8) * 1.5 kg * NORMAL (1.0) = 12
        assertEquals(12, calculatePoints(1.5, PlasticType.PP, Cleanliness.NORMAL))
        // LDPE (2) * 2.0 kg * DIRTY (0.5) = 2
        assertEquals(2, calculatePoints(2.0, PlasticType.LDPE, Cleanliness.DIRTY))
        // Negative quantity clamps to 0
        assertEquals(0, calculatePoints(-3.0, PlasticType.PET, Cleanliness.CLEAN))
    }

    @Test
    fun effectiveTransactionStatus_handlesExpiration() {
        val baseTx = Transaction(
            id = "tx-1",
            token = "TOKEN1",
            userId = null,
            userName = null,
            adminId = "admin-1",
            collectionPoint = COLLECTION_POINTS.first().canonicalName,
            items = listOf(TxItem(PlasticType.PET, 1.0, Cleanliness.CLEAN, 15)),
            totalQuantity = 1.0,
            weightKg = 1.0,
            points = 15,
            status = TransactionStatus.PENDING,
            createdAt = 1_000L,
            expiresAt = 5_000L
        )
        assertEquals(TransactionStatus.PENDING, effectiveTransactionStatus(baseTx, now = 4_000L))
        assertEquals(TransactionStatus.EXPIRED, effectiveTransactionStatus(baseTx, now = 6_000L))
        assertEquals(
            TransactionStatus.CONFIRMED,
            effectiveTransactionStatus(baseTx.copy(status = TransactionStatus.CONFIRMED), now = 10_000L)
        )
        assertEquals(
            TransactionStatus.CANCELLED,
            effectiveTransactionStatus(baseTx.copy(status = TransactionStatus.CANCELLED), now = 10_000L)
        )
    }

    @Test
    fun stationFill_subtractsHandoverRunsAndCapsAt100Percent() {
        val station = COLLECTION_POINTS.first()
        val tx1 = Transaction(
            id = "tx-a",
            token = "AAA",
            userId = "u1",
            userName = "Student",
            adminId = "admin",
            collectionPoint = station.canonicalName,
            items = listOf(TxItem(PlasticType.PET, 15.0, Cleanliness.CLEAN, 225)),
            totalQuantity = 15.0,
            weightKg = 15.0,
            points = 225,
            status = TransactionStatus.CONFIRMED,
            createdAt = 1_000L,
            confirmedAt = 2_000L,
            expiresAt = 10_000L
        )
        val txPending = tx1.copy(id = "tx-b", status = TransactionStatus.PENDING, weightKg = 5.0)
        val run = CollectionRun(
            id = "run-1",
            collectionPoint = station.canonicalName,
            weightKg = 2.5,
            adminId = "admin",
            createdAt = 3_000L
        )

        val fillKg = getStationFillKg(
            transactions = listOf(tx1, txPending),
            collectionRuns = listOf(run),
            collectionPoint = station.canonicalName,
            now = 4_000L
        )
        assertEquals(12.5, fillKg, 0.001)

        val fillPct = getStationFillPercent(
            transactions = listOf(tx1, txPending),
            collectionRuns = listOf(run),
            point = station,
            now = 4_000L
        )
        assertEquals(50, fillPct)
    }

    @Test
    fun translations_resolveInAllThreeLocalesWithParams() {
        val vi = FlysticTranslations.t("adminStationEmptySuccess", AppLocale.VI, mapOf("weight" to "4.20"))
        val en = FlysticTranslations.t("adminStationEmptySuccess", AppLocale.EN, mapOf("weight" to "4.20"))
        val ko = FlysticTranslations.t("adminStationEmptySuccess", AppLocale.KO, mapOf("weight" to "4.20"))
        assertTrue(vi.contains("4.20"))
        assertTrue(en.contains("4.20"))
        assertTrue(ko.contains("4.20"))

        val (nameEn, _) = FlysticTranslations.localizedCollectionPoint("soict-b1", AppLocale.EN)
        assertTrue(nameEn.contains("Information"))
    }

    @Test
    fun petProgression_calculatesThresholdsStagesAndUpgrades() {
        assertEquals(0, PetProgression.xpThresholdForLevel(1))
        assertEquals(25, PetProgression.xpThresholdForLevel(2))
        assertEquals(60, PetProgression.xpThresholdForLevel(3))

        val initialPet = PetCompanionState(species = PetSpecies.fromId("fox"), level = 1, bonusXp = 10)
        assertEquals(PetSpecies.FOX, initialPet.species)
        assertEquals(false, PetProgression.canUpgrade(studentPoints = 10, petState = initialPet))
        assertEquals(true, PetProgression.canUpgrade(studentPoints = 15, petState = initialPet))

        val (earned, span, fraction) = PetProgression.progressInCurrentLevel(studentPoints = 15, petState = initialPet)
        assertEquals(25, earned)
        assertEquals(25, span)
        assertEquals(1f, fraction, 0.001f)

        assertEquals(3, PetProgression.maxReachableLevel(totalXp = 65))
        assertEquals(PetStage.BABY, PetStage.fromLevel(1))
        assertEquals(PetStage.SPROUT, PetStage.fromLevel(3))
        assertEquals(PetStage.GUARDIAN, PetStage.fromLevel(6))
        assertEquals(PetStage.LEGEND, PetStage.fromLevel(10))
    }

    @Test
    fun plasticMascots_coverAllTypesAndTranslations() {
        assertEquals(PlasticType.entries.size, PLASTIC_MASCOTS.size)
        assertEquals(3, DuoInteractionType.entries.size)
        PlasticType.entries.forEach { type ->
            val meta = PLASTIC_MASCOTS.getValue(type)
            AppLocale.entries.forEach { locale ->
                val name = FlysticTranslations.t(meta.nameKey, locale)
                val intro = FlysticTranslations.t(meta.introKey, locale)
                val cheer = FlysticTranslations.t(meta.cheerKey, locale)
                assertTrue(name.isNotBlank() && name != meta.nameKey)
                assertTrue(intro.isNotBlank() && intro != meta.introKey)
                assertTrue(cheer.isNotBlank() && cheer != meta.cheerKey)
            }
        }
    }

    @Test
    fun petStoriesPersonalitiesSkinsCityAndBattle_areConsistent() {
        // Every species has 3 story chapters translated in all 3 locales
        PetSpecies.entries.forEach { species ->
            val chapters = species.storyChapters()
            assertEquals(3, chapters.size)
            chapters.forEach { ch ->
                AppLocale.entries.forEach { locale ->
                    val title = FlysticTranslations.t(ch.titleKey, locale)
                    val body = FlysticTranslations.t(ch.bodyKey, locale)
                    assertTrue(title.isNotBlank() && title != ch.titleKey)
                    assertTrue(body.isNotBlank() && body != ch.bodyKey)
                }
            }
        }

        // Every personality & reaction context resolves cleanly
        PetPersonality.entries.forEach { p ->
            PetReactionContext.entries.forEach { ctx ->
                val key = PetProgression.reactionTranslationKey(p, ctx)
                AppLocale.entries.forEach { locale ->
                    val text = FlysticTranslations.t(key, locale)
                    assertTrue(text.isNotBlank() && text != key)
                }
            }
        }

        // City green index & battle stats scale up with upgrades
        val basePersonalization = UserPersonalizationState()
        val upgradedCity = GreenCityState(
            treesPlanted = 6,
            buildingLevels = mapOf(
                CityBuildingType.ECO_FOREST.id to 3,
                CityBuildingType.SOLAR_HUB.id to 2,
                CityBuildingType.SKY_GARDEN.id to 2
            )
        )
        val boostedPersonalization = basePersonalization.copy(
            city = upgradedCity,
            activeSkinId = PetSkin.GOLDEN_LEAF.id,
            unlockedSkinIds = setOf(PetSkin.CLASSIC.id, PetSkin.GOLDEN_LEAF.id)
        )
        val pet = PetCompanionState(level = 4)
        val baseStats = PetProgression.battleStats(pet, basePersonalization)
        val boostedStats = PetProgression.battleStats(pet, boostedPersonalization)
        assertTrue(boostedStats.maxHp > baseStats.maxHp)
        assertTrue(boostedStats.attack > baseStats.attack)
        assertTrue(upgradedCity.greenIndexPercent() > basePersonalization.safeCity().greenIndexPercent())

        // Arena tiers and opponents
        assertEquals(ArenaTier.BRONZE, ArenaTier.fromRp(950))
        assertEquals(ArenaTier.SILVER, ArenaTier.fromRp(1150))
        assertEquals(ArenaTier.LEGEND, ArenaTier.fromRp(1800))
        val opponents = PetProgression.buildArenaOpponents(emptyList(), "me")
        assertTrue(opponents.size >= 4)
    }

    @Test
    fun campaignsWeeklyBattleAndCategorizedTitles_workEndToEnd() {
        // All 12 titles across 4 categories have valid translations in VI/EN/KO
        assertEquals(12, ECO_TITLES.size)
        assertEquals(4, TitleCategory.entries.size)
        TitleCategory.entries.forEach { cat ->
            AppLocale.entries.forEach { locale ->
                val catName = FlysticTranslations.t(cat.nameKey, locale)
                assertTrue(catName.isNotBlank() && catName != cat.nameKey)
            }
        }
        ECO_TITLES.forEach { title ->
            AppLocale.entries.forEach { locale ->
                val name = FlysticTranslations.t(title.nameKey, locale)
                val desc = FlysticTranslations.t(title.descKey, locale)
                assertTrue(name.isNotBlank() && name != title.nameKey)
                assertTrue(desc.isNotBlank() && desc != title.descKey)
            }
        }

        val now = System.currentTimeMillis()
        val user = User(id = "u-me", name = "Minh Anh", email = "ma@hust.edu.vn", role = Role.STUDENT)
        val weekTx = Transaction(
            id = "tx-w1",
            token = "WK1",
            userId = user.id,
            userName = user.name,
            adminId = "admin-1",
            collectionPoint = COLLECTION_POINTS.first().canonicalName,
            items = listOf(
                TxItem(PlasticType.PET, 1.0, Cleanliness.CLEAN, 15),
                TxItem(PlasticType.PP, 0.5, Cleanliness.CLEAN, 6),
                TxItem(PlasticType.HDPE, 0.5, Cleanliness.CLEAN, 8)
            ),
            totalQuantity = 2.0,
            weightKg = 2.0,
            points = 29,
            status = TransactionStatus.CONFIRMED,
            createdAt = now - 10_000L,
            confirmedAt = now - 5_000L,
            expiresAt = now + 60_000L
        )
        val state = FlysticState(
            user = user,
            points = 29,
            totalKg = 2.0,
            transactions = listOf(weekTx)
        )
        val personalization = UserPersonalizationState(
            arenaRp = 1070,
            arenaWins = 3,
            arenaStreak = 2,
            weeklyBattleBonusPts = 28,
            weeklyBattleKgBonus = 0.5,
            city = GreenCityState(
                treeLevel = 3,
                treesPlanted = 2,
                buildingLevels = mapOf(
                    CityBuildingType.ECO_FOREST.id to 2,
                    CityBuildingType.SOLAR_HUB.id to 1
                )
            ),
            invitedMascotsInCity = listOf(
                InvitedCityMascot("m1", "Linh", "PET-Chan", plasticMascot = PlasticType.PET),
                InvitedCityMascot("m2", "Minh", "PP-Cáo", plasticMascot = PlasticType.PP)
            )
        )
        val campaign = EcoCampaign(
            id = "cmp-1",
            title = "Chiến Dịch Chai Sạch",
            description = "Gom chai sạch",
            inviteCode = "ECO-TEST",
            inviteLink = "https://flystic.app/campaign/ECO-TEST",
            targetKg = 5.0,
            creatorId = user.id,
            isEnded = true,
            participants = listOf(
                CampaignParticipant(user.id, user.name, "Mầm", kgRecycled = 2.0, points = 50),
                CampaignParticipant("f1", "Linh", "PET-Chan", plasticMascot = PlasticType.PET, kgRecycled = 2.8, points = 72)
            ),
            photos = listOf(
                CampaignPhoto("p1", user.id, user.name, "Chụp tại B1", PlasticType.PET, 1.0, samplePhotoKey = "pet")
            )
        )

        val weekly = PetProgression.computeWeeklySummary(state, user.id, personalization, now)
        assertEquals(2.5, weekly.weeklyKg, 0.001)
        assertEquals(57, weekly.totalWeeklyScore)
        assertEquals(1.0, weekly.cleanRatio, 0.001)
        assertEquals(3, weekly.distinctPlasticsCount)

        assertEquals("f1", campaign.rankedParticipants().first().userId)
        assertEquals(4.8, campaign.totalKg(), 0.001)
        assertEquals(1, campaign.photosOf(user.id).size)

        val unlocked = PetProgression.evaluateUnlockedTitles(
            state = state,
            petState = PetCompanionState(level = 3),
            personalization = personalization,
            campaigns = listOf(campaign),
            now = now
        )
        assertEquals(ECO_TITLES.size, unlocked.size)
    }

    @Test
    fun openAiPlasticDetector_parsesDetectedUncertainAndUnsupported_andTranslationsExist() {
        val detectedJson = """
            ```json
            {
              "status": "DETECTED",
              "plastic_code": "PET",
              "confidence": 0.91,
              "mascot_message": "Chào bạn! Mình là PET-Chan đây, chai nước khoáng này chuẩn nhựa PET #1 nhé!",
              "prep_tip": "Đổ hết nước, tháo nhãn, bóp dẹp trước khi mang ra trạm.",
              "angle_suggestions": [],
              "packaging_origin_questions": []
            }
            ```
        """.trimIndent()
        val detected = OpenAiPlasticDetector.parseDetectionJson(detectedJson)
        assertEquals(DetectionVerdict.DETECTED, detected.verdict)
        assertEquals(PlasticType.PET, detected.plasticType)
        assertEquals("PET (#1)", detected.resinCodeLabel)
        assertEquals(0.91, detected.confidence, 0.001)
        assertTrue(detected.angleSuggestions.isEmpty())

        // Confidence below threshold (< 0.68) forces honest UNCERTAIN instead of guessing
        val lowConfidenceJson = """
            {
              "status": "DETECTED",
              "plastic_code": "PP",
              "confidence": 0.55,
              "mascot_message": "Chưa chắc chắn lắm.",
              "prep_tip": "",
              "angle_suggestions": ["Lật đáy hộp chụp ký hiệu tam giác"],
              "packaging_origin_questions": ["Hộp này trước đây đựng đồ nóng hay lạnh?"]
            }
        """.trimIndent()
        val uncertain = OpenAiPlasticDetector.parseDetectionJson(lowConfidenceJson)
        assertEquals(DetectionVerdict.UNCERTAIN, uncertain.verdict)
        assertEquals(null, uncertain.plasticType)
        assertEquals(1, uncertain.angleSuggestions.size)
        assertEquals(1, uncertain.packagingOrOriginQuestions.size)

        val unsupportedJson = """
            {
              "status": "UNSUPPORTED",
              "plastic_code": "PVC_OR_OTHER",
              "confidence": 0.88,
              "mascot_message": "Thẻ nhựa PVC #3 chưa thu gom tại trạm FLYSTIC.",
              "prep_tip": "Không bỏ vào thùng tái chế FLYSTIC.",
              "angle_suggestions": [],
              "packaging_origin_questions": []
            }
        """.trimIndent()
        val unsupported = OpenAiPlasticDetector.parseDetectionJson(unsupportedJson)
        assertEquals(DetectionVerdict.UNSUPPORTED_PLASTIC, unsupported.verdict)
        assertEquals(null, unsupported.plasticType)

        val aiKeys = listOf(
            "aiDetectNavBtn",
            "aiDetectTitle",
            "aiDetectSubtitle",
            "aiDetectPrivacyBadge",
            "aiDetectCameraBtn",
            "aiDetectGalleryBtn",
            "aiDetectHintLabel",
            "aiDetectHintPlaceholder",
            "aiDetectAnalyzeBtn",
            "aiDetectAnalyzing",
            "aiDetectClearBtn",
            "aiDetectQuickCluesTitle",
            "aiDetectUncertainTitle",
            "aiDetectAnglesTitle",
            "aiDetectPackagingTitle",
            "aiDetectUnsupportedTitle",
            "aiDetectPrepTipTitle",
            "aiDetectConfidence",
            "aiDetectPointsPerKg",
            "aiDetectRetakeAngleBtn",
            "aiDetectGuideBanner"
        )
        aiKeys.forEach { key ->
            AppLocale.entries.forEach { locale ->
                val translated = FlysticTranslations.t(key, locale)
                assertTrue("Missing $key for $locale", translated.isNotBlank() && translated != key)
            }
        }
    }
}
