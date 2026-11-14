package com.verifour.app

import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.Cleanliness
import com.verifour.app.model.CollectionRun
import com.verifour.app.model.PlasticType
import com.verifour.app.model.Transaction
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.TxItem
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
}
