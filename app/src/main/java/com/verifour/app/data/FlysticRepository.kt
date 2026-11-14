package com.verifour.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.verifour.app.model.AppLocale
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.CampaignParticipant
import com.verifour.app.model.CampaignPhoto
import com.verifour.app.model.Cleanliness
import com.verifour.app.model.CollectionRun
import com.verifour.app.model.DemoCampaign
import com.verifour.app.model.DemoCampaignMetric
import com.verifour.app.model.EcoCampaign
import com.verifour.app.model.FlysticState
import com.verifour.app.model.HabitatTheme
import com.verifour.app.model.LeaderboardRow
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PlasticType
import com.verifour.app.model.PointDestination
import com.verifour.app.model.Role
import com.verifour.app.model.Station
import com.verifour.app.model.StudentOrganization
import com.verifour.app.model.Transaction
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.TxItem
import com.verifour.app.model.User
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.model.calculatePoints
import com.verifour.app.model.getStationFillKg
import com.verifour.app.model.round2
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random

class FlysticRepository(context: Context) {
    companion object {
        private const val PREFS_NAME = "flystic_prefs"
        private const val KEY_STATE = "yoha:state:v3"
        private const val KEY_SESSION = "yoha:session:v1"
        private const val KEY_LOCALE = "yoha:locale:v1"
        private const val KEY_IS_REMOTE = "yoha:remote:v1"
        private const val KEY_PET_PREFIX = "yoha:pet:v1:"
        private const val KEY_PERSONALIZATION_PREFIX = "yoha:personalization:v1:"
        private const val KEY_CAMPAIGNS_PREFIX = "yoha:campaigns:v1:"
        private const val TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val api = FlysticApiClient()
    private val secureRandom = SecureRandom()

    fun isRemoteMode(): Boolean = prefs.getBoolean(KEY_IS_REMOTE, true)

    fun setRemoteMode(remote: Boolean) {
        prefs.edit().putBoolean(KEY_IS_REMOTE, remote).apply()
    }

    fun getPersistedLocale(): AppLocale =
        AppLocale.fromCode(prefs.getString(KEY_LOCALE, AppLocale.VI.code))

    fun saveLocale(locale: AppLocale) {
        prefs.edit().putString(KEY_LOCALE, locale.code).apply()
    }

    fun getSessionToken(): String? = prefs.getString(KEY_SESSION, null)

    fun saveSessionToken(token: String?) {
        prefs.edit().apply {
            if (token != null) putString(KEY_SESSION, token) else remove(KEY_SESSION)
        }.apply()
    }

    fun loadPetState(userId: String?): PetCompanionState {
        if (userId.isNullOrBlank()) return PetCompanionState()
        val raw = prefs.getString("$KEY_PET_PREFIX$userId", null) ?: return PetCompanionState()
        return runCatching {
            gson.fromJson(raw, PetCompanionState::class.java) ?: PetCompanionState()
        }.getOrElse { PetCompanionState() }
    }

    fun savePetState(userId: String?, petState: PetCompanionState) {
        if (userId.isNullOrBlank()) return
        prefs.edit().putString("$KEY_PET_PREFIX$userId", gson.toJson(petState)).apply()
    }

    fun loadPersonalization(userId: String?): UserPersonalizationState {
        if (userId.isNullOrBlank()) return UserPersonalizationState()
        val raw = prefs.getString("$KEY_PERSONALIZATION_PREFIX$userId", null)
            ?: return UserPersonalizationState()
        return runCatching {
            gson.fromJson(raw, UserPersonalizationState::class.java) ?: UserPersonalizationState()
        }.getOrElse { UserPersonalizationState() }
    }

    fun savePersonalization(userId: String?, personalization: UserPersonalizationState) {
        if (userId.isNullOrBlank()) return
        prefs.edit().putString("$KEY_PERSONALIZATION_PREFIX$userId", gson.toJson(personalization)).apply()
    }

    fun loadCampaigns(userId: String?): List<EcoCampaign> {
        if (userId.isNullOrBlank()) return emptyList()
        val raw = prefs.getString("$KEY_CAMPAIGNS_PREFIX$userId", null) ?: return emptyList()
        val type = object : TypeToken<List<EcoCampaign>>() {}.type
        return runCatching {
            gson.fromJson<List<EcoCampaign>>(raw, type) ?: emptyList()
        }.getOrElse { emptyList() }
    }

    fun saveCampaigns(userId: String?, campaigns: List<EcoCampaign>) {
        if (userId.isNullOrBlank()) return
        prefs.edit().putString("$KEY_CAMPAIGNS_PREFIX$userId", gson.toJson(campaigns)).apply()
    }

    fun buildPresetJoinableCampaign(rawCodeOrLink: String): EcoCampaign? {
        val code = rawCodeOrLink.trim()
            .substringAfterLast("/")
            .uppercase()
        if (code.isBlank()) return null
        val presets = mapOf(
            "ECO-HUST" to Triple(
                "Chiến Dịch Chai Sạch Bách Khoa",
                "Tuần lễ gom chai PET & cốc PP sạch tại B1 - C7.",
                HabitatTheme.MEADOW
            ),
            "GREEN-BK" to Triple(
                "Tuần Lễ Thành Phố Xanh K68",
                "Kết nối mascot các khoa cùng phân loại nhựa & trồng cây.",
                HabitatTheme.SAKURA_GARDEN
            )
        )
        val matched = presets[code] ?: return null
        val now = System.currentTimeMillis()
        return EcoCampaign(
            id = "preset-${code.lowercase()}",
            title = matched.first,
            description = matched.second,
            inviteCode = code,
            inviteLink = "https://flystic.app/campaign/$code",
            targetKg = 12.0,
            creatorId = "bot-linh",
            habitatTheme = matched.third,
            createdAt = now - 86_400_000L,
            participants = listOf(
                CampaignParticipant(
                    userId = "bot-linh",
                    name = "Nguyễn Thảo Linh",
                    mascotName = "Mầm B1",
                    species = PetSpecies.SPROUTLING,
                    skin = PetSkin.SAKURA,
                    kgRecycled = 2.6,
                    points = 64,
                    equippedTitleId = "clean_saint"
                ),
                CampaignParticipant(
                    userId = "bot-minh",
                    name = "Trần Quang Minh",
                    mascotName = "PET-Chan",
                    species = PetSpecies.TURTLE,
                    skin = PetSkin.SOLAR_TECH,
                    plasticMascot = PlasticType.PET,
                    kgRecycled = 1.9,
                    points = 46,
                    equippedTitleId = "pet_king"
                )
            ),
            photos = listOf(
                CampaignPhoto(
                    id = "photo-preset-1",
                    authorUserId = "bot-linh",
                    authorName = "Nguyễn Thảo Linh",
                    caption = "Rửa sạch 18 chai PET trước khi bỏ thùng B1!",
                    plasticType = PlasticType.PET,
                    kg = 1.4,
                    samplePhotoKey = "pet",
                    createdAt = now - 36_000_000L
                ),
                CampaignPhoto(
                    id = "photo-preset-2",
                    authorUserId = "bot-minh",
                    authorName = "Trần Quang Minh",
                    caption = "Gom cốc trà sữa PP tại căng tin Bách Khoa",
                    plasticType = PlasticType.PP,
                    kg = 1.1,
                    samplePhotoKey = "pp",
                    createdAt = now - 18_000_000L
                )
            )
        )
    }

    fun saveCampaignBitmap(bitmap: Bitmap): String? = runCatching {
        val dir = File(appContext.filesDir, "campaign_photos").apply { mkdirs() }
        val file = File(dir, "eco_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        file.absolutePath
    }.getOrNull()

    fun copyImageUriToCampaignStorage(uri: Uri): String? = runCatching {
        val dir = File(appContext.filesDir, "campaign_photos").apply { mkdirs() }
        val file = File(dir, "eco_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: return null
        file.absolutePath
    }.getOrNull()

    fun loadLocalState(): FlysticState {
        val raw = prefs.getString(KEY_STATE, null) ?: return FlysticState(locale = getPersistedLocale())
        return runCatching {
            val loaded = gson.fromJson(raw, FlysticState::class.java)
            loaded?.copy(locale = getPersistedLocale()) ?: FlysticState(locale = getPersistedLocale())
        }.getOrElse {
            FlysticState(locale = getPersistedLocale())
        }
    }

    fun saveLocalState(state: FlysticState) {
        prefs.edit().putString(KEY_STATE, gson.toJson(state)).apply()
    }

    suspend fun loadInitialState(): FlysticState {
        val locale = getPersistedLocale()
        if (!isRemoteMode()) {
            return loadLocalState()
        }
        val token = getSessionToken()
        if (token.isNullOrBlank()) {
            val publicStations = runCatching { api.getStations().map(::mapStation) }
                .getOrElse { FlysticState().stations }
            return FlysticState(stations = publicStations, locale = locale)
        }
        return runCatching {
            refreshRemoteState(token, locale)
        }.getOrElse {
            saveSessionToken(null)
            FlysticState(locale = locale)
        }
    }

    suspend fun refreshRemoteState(
        token: String? = getSessionToken(),
        locale: AppLocale = getPersistedLocale()
    ): FlysticState {
        if (token.isNullOrBlank()) throw FlysticApiException("Session expired", 401)
        val snapshot = api.getSnapshot(token)
        val leaderboard = runCatching { api.getLeaderboard("all").rows.map(::mapLeaderboard) }
            .getOrElse { emptyList() }
        val stations = snapshot.stations.map(::mapStation)
        return FlysticState(
            user = mapUser(snapshot.user, stations),
            points = snapshot.user.points,
            rewardPoints = snapshot.user.rewardPoints,
            fundPoints = snapshot.user.fundPoints,
            totalKg = snapshot.user.totalKg,
            transactions = snapshot.transactions.map(::mapTransaction),
            collectionRuns = snapshot.collectionRuns.map(::mapCollectionRun),
            stations = stations,
            leaderboard = leaderboard,
            demoCampaign = snapshot.demoCampaign?.let(::mapDemoCampaign),
            locale = locale
        )
    }

    suspend fun fetchStations(): List<Station> =
        if (isRemoteMode()) {
            runCatching { api.getStations().map(::mapStation) }.getOrElse { loadLocalState().stations }
        } else {
            loadLocalState().stations
        }

    suspend fun fetchLeaderboard(period: String, currentState: FlysticState): List<LeaderboardRow> {
        if (!isRemoteMode()) return currentState.leaderboard
        return api.getLeaderboard(period).rows.map(::mapLeaderboard)
    }

    suspend fun signIn(
        currentState: FlysticState,
        email: String,
        password: String,
        isRegister: Boolean,
        role: Role,
        name: String,
        birthDate: String?,
        organization: StudentOrganization?,
        school: String?,
        organizationDetail: String?,
        studentId: String?,
        stationId: String?,
        inviteCode: String?
    ): FlysticState {
        val locale = currentState.locale
        if (isRemoteMode()) {
            val faculty = school?.takeIf { it.isNotBlank() }
                ?: organizationDetail?.takeIf { it.isNotBlank() }
                ?: if (organization == StudentOrganization.OTHER) "Khác" else null

            val response = if (isRegister) {
                api.register(
                    email = email.trim(),
                    password = password,
                    name = name.trim(),
                    role = role.apiValue,
                    studentId = studentId?.takeIf { it.isNotBlank() },
                    faculty = faculty,
                    stationId = stationId ?: COLLECTION_POINTS.first().id,
                    inviteCode = inviteCode
                )
            } else {
                api.login(email.trim(), password)
            }
            saveSessionToken(response.token)
            return refreshRemoteState(response.token, locale)
        }

        // Local demo mode
        val isAdmin = role == Role.ADMIN
        val assignedStation = COLLECTION_POINTS.firstOrNull { it.id == stationId } ?: COLLECTION_POINTS.first()
        val user = User(
            id = UUID.randomUUID().toString().take(8),
            email = email.trim(),
            role = role,
            name = name.trim().ifBlank {
                if (isAdmin) "Admin Trạm" else email.substringBefore("@").ifBlank { "Sinh viên" }
            },
            birthDate = if (isAdmin) null else birthDate,
            organization = if (isAdmin) null else organization,
            organizationDetail = if (isAdmin) null else organizationDetail,
            studentId = if (isAdmin) null else studentId?.takeIf { it.isNotBlank() }
                ?: ("SV" + Random.nextInt(10000, 99999)),
            faculty = if (isAdmin) null else (
                school?.takeIf { it.isNotBlank() }
                    ?: organizationDetail?.takeIf { it.isNotBlank() }
                    ?: if (organization == StudentOrganization.OTHER) "Khác" else "Khoa CNTT"
                ),
            className = if (isAdmin) null else "K65-IT3",
            collectionPoint = if (isAdmin) assignedStation.canonicalName else null
        )
        val next = currentState.copy(user = user)
        saveLocalState(next)
        return next
    }

    fun signOut(currentState: FlysticState): FlysticState {
        if (isRemoteMode()) {
            saveSessionToken(null)
            return FlysticState(
                stations = currentState.stations,
                locale = currentState.locale
            )
        }
        val next = currentState.copy(user = null)
        saveLocalState(next)
        return next
    }

    suspend fun createTransaction(
        currentState: FlysticState,
        items: List<Triple<PlasticType, Double, Cleanliness>>,
        collectionPoint: String
    ): Pair<Transaction, FlysticState> {
        val user = currentState.user ?: throw FlysticApiException("Admin required", 403)
        if (user.role != Role.ADMIN) throw FlysticApiException("Admin required", 403)

        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            val dto = api.createAdminTransaction(
                token = token,
                items = items.filter { it.second > 0.0 }.map {
                    CreateItemInputDto(
                        plasticType = it.first.code,
                        quantity = round2(it.second),
                        cleanliness = it.third.code
                    )
                }
            )
            val created = mapTransaction(dto)
            val refreshed = refreshRemoteState(token, currentState.locale)
            return created to refreshed
        }

        val builtItems = items.filter { it.second > 0.0 }.map { (type, qty, clean) ->
            val q = round2(max(0.0, qty))
            TxItem(
                plasticType = type,
                quantity = q,
                cleanliness = clean,
                points = calculatePoints(q, type, clean)
            )
        }
        if (builtItems.isEmpty()) throw FlysticApiException("Cần ít nhất 1 loại nhựa", 400)
        val totalPts = builtItems.sumOf { it.points }
        val totalQty = round2(builtItems.sumOf { it.quantity })
        val now = System.currentTimeMillis()
        val usedTokens = currentState.transactions.map { it.token }.toSet()
        var qrToken = generateQrToken()
        while (qrToken in usedTokens) {
            qrToken = generateQrToken()
        }
        val tx = Transaction(
            id = UUID.randomUUID().toString().take(8),
            token = qrToken,
            userId = null,
            userName = null,
            adminId = user.id,
            collectionPoint = collectionPoint,
            items = builtItems,
            totalQuantity = totalQty,
            weightKg = totalQty,
            points = totalPts,
            status = TransactionStatus.PENDING,
            createdAt = now,
            expiresAt = now + 5 * 60_000L
        )
        val next = currentState.copy(transactions = listOf(tx) + currentState.transactions)
        saveLocalState(next)
        return tx to next
    }

    suspend fun getTransaction(currentState: FlysticState, qrToken: String): Transaction? {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            return mapTransaction(api.getTransaction(token, qrToken))
        }
        return currentState.transactions.firstOrNull { it.token == qrToken }
    }

    suspend fun confirmTransaction(
        currentState: FlysticState,
        qrToken: String,
        destination: PointDestination,
        previewTx: Transaction?
    ): Pair<Transaction?, FlysticState> {
        val user = currentState.user ?: return null to currentState
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            val result = api.confirmTransaction(token, qrToken, destination.apiValue)
            val confirmed = previewTx?.copy(
                status = TransactionStatus.CONFIRMED,
                confirmedAt = result.confirmedAt
            )
            val next = runCatching { refreshRemoteState(token, currentState.locale) }
                .getOrElse {
                    currentState.copy(
                        points = currentState.points + result.points,
                        rewardPoints = if (destination == PointDestination.REWARDS) {
                            currentState.rewardPoints + result.points
                        } else {
                            currentState.rewardPoints
                        },
                        fundPoints = if (destination == PointDestination.FUNDRAISING) {
                            currentState.fundPoints + result.points
                        } else {
                            currentState.fundPoints
                        },
                        totalKg = round2(currentState.totalKg + result.weightKg),
                        transactions = if (confirmed != null) {
                            listOf(confirmed) + currentState.transactions.filterNot { it.id == confirmed.id }
                        } else {
                            currentState.transactions
                        }
                    )
                }
            return confirmed to next
        }

        val idx = currentState.transactions.indexOfFirst { it.token == qrToken }
        if (idx == -1) return null to currentState
        val tx = currentState.transactions[idx]
        if (tx.status != TransactionStatus.PENDING) return null to currentState
        val now = System.currentTimeMillis()
        if (now > tx.expiresAt) {
            val updated = currentState.transactions.toMutableList()
            updated[idx] = tx.copy(status = TransactionStatus.EXPIRED)
            val expiredState = currentState.copy(transactions = updated)
            saveLocalState(expiredState)
            return null to expiredState
        }
        val confirmed = tx.copy(
            status = TransactionStatus.CONFIRMED,
            userId = user.id,
            userName = user.name,
            userFaculty = user.faculty,
            confirmedAt = now
        )
        val updated = currentState.transactions.toMutableList()
        updated[idx] = confirmed
        val next = currentState.copy(
            transactions = updated,
            points = currentState.points + confirmed.points,
            rewardPoints = if (destination == PointDestination.REWARDS) {
                currentState.rewardPoints + confirmed.points
            } else {
                currentState.rewardPoints
            },
            fundPoints = if (destination == PointDestination.FUNDRAISING) {
                currentState.fundPoints + confirmed.points
            } else {
                currentState.fundPoints
            },
            totalKg = round2(currentState.totalKg + confirmed.weightKg)
        )
        saveLocalState(next)
        return confirmed to next
    }

    suspend fun cancelTransaction(currentState: FlysticState, id: String): FlysticState {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.cancelAdminTransaction(token, id)
            return refreshRemoteState(token, currentState.locale)
        }
        val updated = currentState.transactions.map {
            if (it.id == id && it.status == TransactionStatus.PENDING) {
                it.copy(status = TransactionStatus.CANCELLED)
            } else {
                it
            }
        }
        val next = currentState.copy(transactions = updated)
        saveLocalState(next)
        return next
    }

    suspend fun emptyCollectionPoint(
        currentState: FlysticState,
        collectionPoint: String
    ): Pair<CollectionRun?, FlysticState> {
        val user = currentState.user ?: return null to currentState
        if (user.role != Role.ADMIN) return null to currentState
        if (isRemoteMode()) {
            val station = currentState.stations.firstOrNull { it.name == collectionPoint }
                ?: return null to currentState
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            val result = api.recordStationTransfer(token, station.id)
            val run = CollectionRun(
                id = "${result.stationId}-${result.createdAt}",
                collectionPoint = collectionPoint,
                weightKg = result.weightKg,
                adminId = user.id,
                createdAt = result.createdAt
            )
            val refreshed = refreshRemoteState(token, currentState.locale)
            return run to refreshed
        }

        val weightKg = getStationFillKg(
            currentState.transactions,
            currentState.collectionRuns,
            collectionPoint
        )
        if (weightKg <= 0.0) return null to currentState
        val run = CollectionRun(
            id = UUID.randomUUID().toString().take(8),
            collectionPoint = collectionPoint,
            weightKg = weightKg,
            adminId = user.id,
            createdAt = System.currentTimeMillis()
        )
        val next = currentState.copy(collectionRuns = listOf(run) + currentState.collectionRuns)
        saveLocalState(next)
        return run to next
    }

    private fun generateQrToken(length: Int = 12): String {
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(TOKEN_ALPHABET[secureRandom.nextInt(TOKEN_ALPHABET.length)])
        }
        return sb.toString()
    }

    private fun mapUser(dto: ApiUserDto, stations: List<Station>): User = User(
        id = dto.id,
        email = dto.email,
        role = Role.fromApi(dto.role),
        name = dto.name,
        studentId = dto.studentId,
        faculty = dto.faculty,
        collectionPoint = stations.firstOrNull { it.id == dto.stationId }?.name
    )

    private fun mapTransaction(dto: ApiTransactionDto): Transaction = Transaction(
        id = dto.id,
        token = dto.token.orEmpty(),
        userId = dto.userId,
        userName = dto.userName,
        userFaculty = dto.userFaculty,
        adminId = dto.adminId,
        collectionPoint = dto.collectionPoint,
        items = dto.items.map {
            TxItem(
                plasticType = PlasticType.fromCode(it.plasticType),
                quantity = it.quantity,
                cleanliness = Cleanliness.fromCode(it.cleanliness),
                points = it.points
            )
        },
        totalQuantity = dto.totalQuantity,
        weightKg = dto.weightKg,
        points = dto.points,
        status = TransactionStatus.fromApi(dto.status),
        createdAt = dto.createdAt,
        confirmedAt = dto.confirmedAt,
        expiresAt = dto.expiresAt
    )

    private fun mapStation(dto: ApiStationDto): Station = Station(
        id = dto.id,
        name = dto.name,
        address = dto.address,
        latitude = dto.latitude,
        longitude = dto.longitude,
        capacityKg = dto.capacityKg,
        filledKg = dto.filledKg,
        fillPercent = dto.fillPercent
    )

    private fun mapCollectionRun(dto: ApiCollectionRunDto): CollectionRun = CollectionRun(
        id = dto.id,
        collectionPoint = dto.collectionPoint,
        weightKg = dto.weightKg,
        adminId = dto.adminId,
        createdAt = dto.createdAt
    )

    private fun mapLeaderboard(dto: ApiLeaderboardRowDto): LeaderboardRow = LeaderboardRow(
        userId = dto.userId,
        name = dto.name,
        faculty = dto.faculty,
        points = dto.points,
        kg = dto.kg
    )

    private fun mapDemoCampaign(dto: ApiDemoCampaignDto): DemoCampaign = DemoCampaign(
        registeredStudents = dto.registeredStudents,
        participantCount = dto.participantCount,
        transactionCount = dto.transactionCount,
        totalKg = dto.totalKg,
        totalPoints = dto.totalPoints,
        fundraisingPoints = dto.fundraisingPoints,
        fundraisingPercent = dto.fundraisingPercent,
        byType = dto.byType.map { DemoCampaignMetric(it.label, it.kg) },
        byQuality = dto.byQuality.map { DemoCampaignMetric(it.label, it.kg) }
    )
}
