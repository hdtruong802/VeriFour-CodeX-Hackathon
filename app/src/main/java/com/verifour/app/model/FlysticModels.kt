package com.verifour.app.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class Role(val apiValue: String) {
    STUDENT("student"),
    ADMIN("admin");

    companion object {
        fun fromApi(value: String?): Role =
            if (value == "admin") ADMIN else STUDENT
    }
}

enum class StudentOrganization(val apiValue: String) {
    HUST("hust"),
    OTHER("other")
}

enum class AppLocale(val code: String) {
    VI("vi"),
    EN("en"),
    KO("ko");

    companion object {
        fun fromCode(code: String?): AppLocale =
            entries.firstOrNull { it.code == code } ?: VI
    }
}

enum class PlasticType(
    val code: String,
    val basePoints: Int,
    val emoji: String
) {
    PET("PET", 10, "🧴"),
    HDPE("HDPE", 10, "🧼"),
    PP("PP", 8, "🥤"),
    LDPE("LDPE", 2, "🛍️"),
    PS("PS", 1, "🥡");

    companion object {
        fun fromCode(code: String?): PlasticType =
            entries.firstOrNull { it.code == code } ?: PET
    }
}

enum class Cleanliness(
    val code: String,
    val multiplier: Double
) {
    CLEAN("CLEAN", 1.5),
    NORMAL("NORMAL", 1.0),
    DIRTY("DIRTY", 0.5);

    companion object {
        fun fromCode(code: String?): Cleanliness =
            entries.firstOrNull { it.code == code } ?: NORMAL
    }
}

enum class TransactionStatus(val apiValue: String) {
    PENDING("pending"),
    CONFIRMED("confirmed"),
    EXPIRED("expired"),
    CANCELLED("cancelled");

    companion object {
        fun fromApi(value: String?): TransactionStatus =
            entries.firstOrNull { it.apiValue == value } ?: PENDING
    }
}

enum class PointDestination(val apiValue: String) {
    REWARDS("rewards"),
    FUNDRAISING("fundraising")
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: Role,
    val birthDate: String? = null,
    val organization: StudentOrganization? = null,
    val organizationDetail: String? = null,
    val studentId: String? = null,
    val faculty: String? = null,
    val className: String? = null,
    val collectionPoint: String? = null
)

data class TxItem(
    val plasticType: PlasticType,
    val quantity: Double,
    val cleanliness: Cleanliness,
    val points: Int
)

data class Transaction(
    val id: String,
    val token: String,
    val userId: String?,
    val userName: String?,
    val userFaculty: String? = null,
    val adminId: String,
    val collectionPoint: String,
    val items: List<TxItem>,
    val totalQuantity: Double,
    val weightKg: Double,
    val points: Int,
    val status: TransactionStatus,
    val createdAt: Long,
    val confirmedAt: Long? = null,
    val expiresAt: Long
)

data class CollectionRun(
    val id: String,
    val collectionPoint: String,
    val weightKg: Double,
    val adminId: String,
    val createdAt: Long
)

data class Station(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val capacityKg: Double,
    val filledKg: Double,
    val fillPercent: Int
)

data class LeaderboardRow(
    val userId: String,
    val name: String,
    val faculty: String,
    val points: Int,
    val kg: Double
)

data class DemoCampaignMetric(
    val label: String,
    val kg: Double
)

data class DemoCampaign(
    val registeredStudents: Int,
    val participantCount: Int,
    val transactionCount: Int,
    val totalKg: Double,
    val totalPoints: Int,
    val fundraisingPoints: Int,
    val fundraisingPercent: Int,
    val byType: List<DemoCampaignMetric>,
    val byQuality: List<DemoCampaignMetric>
)

data class CollectionPointMeta(
    val id: String,
    val canonicalName: String,
    val canonicalAddress: String,
    val capacityKg: Double,
    val mapXFraction: Float,
    val mapYFraction: Float,
    val nameKey: String,
    val addressKey: String
)

val COLLECTION_POINTS = listOf(
    CollectionPointMeta(
        id = "soict-b1",
        canonicalName = "Trường Công nghệ Thông tin và Truyền thông",
        canonicalAddress = "Tòa B1, Đại học Bách khoa Hà Nội",
        capacityKg = 25.0,
        mapXFraction = 0.65f,
        mapYFraction = 0.55f,
        nameKey = "collectionPointSoictName",
        addressKey = "collectionPointSoictAddress"
    ),
    CollectionPointMeta(
        id = "chem-life-c3",
        canonicalName = "Trường Hóa và Khoa học Sự sống",
        canonicalAddress = "Tòa C3, Đại học Bách khoa Hà Nội",
        capacityKg = 25.0,
        mapXFraction = 0.35f,
        mapYFraction = 0.24f,
        nameKey = "collectionPointChemLifeName",
        addressKey = "collectionPointChemLifeAddress"
    ),
    CollectionPointMeta(
        id = "mechanical-c7",
        canonicalName = "Trường Cơ khí",
        canonicalAddress = "Tòa C7, Đại học Bách khoa Hà Nội",
        capacityKg = 25.0,
        mapXFraction = 0.49f,
        mapYFraction = 0.35f,
        nameKey = "collectionPointMechanicalName",
        addressKey = "collectionPointMechanicalAddress"
    ),
    CollectionPointMeta(
        id = "innovation-center",
        canonicalName = "Trung tâm Đổi mới Sáng tạo - Đại học Bách khoa Hà Nội",
        canonicalAddress = "Tòa nhà C7, Đại học Bách khoa Hà Nội",
        capacityKg = 25.0,
        mapXFraction = 0.19f,
        mapYFraction = 0.54f,
        nameKey = "collectionPointInnovationName",
        addressKey = "collectionPointInnovationAddress"
    )
)

val HUST_SCHOOLS = listOf(
    "Trường Công nghệ Thông tin và Truyền thông",
    "Trường Cơ khí",
    "Trường Hoá và Khoa học Sự sống",
    "Trường Điện - Điện tử",
    "Trường Kinh tế",
    "Trường Vật liệu",
    "Khoa Toán Tin",
    "Khoa Khoa học và Công nghệ Giáo dục",
    "Khoa Vật lý Kỹ thuật",
    "Khoa Ngoại ngữ"
)

fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0

fun calculatePoints(
    quantity: Double,
    plasticType: PlasticType,
    cleanliness: Cleanliness
): Int {
    val q = max(0.0, round2(quantity))
    return (q * plasticType.basePoints * cleanliness.multiplier).roundToInt()
}

fun effectiveTransactionStatus(
    tx: Transaction,
    now: Long = System.currentTimeMillis()
): TransactionStatus {
    return if (tx.status == TransactionStatus.PENDING && now > tx.expiresAt) {
        TransactionStatus.EXPIRED
    } else {
        tx.status
    }
}

fun getStationFillKg(
    transactions: List<Transaction>,
    collectionRuns: List<CollectionRun>,
    collectionPoint: String,
    now: Long = System.currentTimeMillis()
): Double {
    val receivedKg = transactions
        .filter {
            it.collectionPoint == collectionPoint &&
                effectiveTransactionStatus(it, now) == TransactionStatus.CONFIRMED
        }
        .sumOf { it.weightKg }
    val transferredKg = collectionRuns
        .filter { it.collectionPoint == collectionPoint }
        .sumOf { it.weightKg }
    return round2(max(0.0, receivedKg - transferredKg))
}

fun getStationFillPercent(
    transactions: List<Transaction>,
    collectionRuns: List<CollectionRun>,
    point: CollectionPointMeta,
    now: Long = System.currentTimeMillis()
): Int {
    val filledKg = getStationFillKg(transactions, collectionRuns, point.canonicalName, now)
    return min(100, ((filledKg / point.capacityKg) * 100.0).roundToInt())
}

data class FlysticState(
    val user: User? = null,
    val points: Int = 0,
    val rewardPoints: Int = 0,
    val fundPoints: Int = 0,
    val totalKg: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val collectionRuns: List<CollectionRun> = emptyList(),
    val stations: List<Station> = COLLECTION_POINTS.map {
        Station(
            id = it.id,
            name = it.canonicalName,
            address = it.canonicalAddress,
            capacityKg = it.capacityKg,
            filledKg = 0.0,
            fillPercent = 0
        )
    },
    val leaderboard: List<LeaderboardRow> = emptyList(),
    val demoCampaign: DemoCampaign? = null,
    val locale: AppLocale = AppLocale.VI
)
