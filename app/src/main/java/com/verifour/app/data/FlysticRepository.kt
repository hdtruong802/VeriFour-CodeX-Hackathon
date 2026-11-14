package com.verifour.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.verifour.app.model.AccessLogEntry
import com.verifour.app.model.AccessOutcome
import com.verifour.app.model.AccountStatus
import com.verifour.app.model.AdminInviteToken
import com.verifour.app.model.AdminOpsState
import com.verifour.app.model.AppLocale
import com.verifour.app.model.AuthLockoutEntry
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
import com.verifour.app.model.ManagedAccount
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PlasticType
import com.verifour.app.model.PointDestination
import com.verifour.app.model.Role
import com.verifour.app.model.Station
import com.verifour.app.model.StationOpsSummary
import com.verifour.app.model.StudentOrganization
import com.verifour.app.model.Transaction
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.TxItem
import com.verifour.app.model.User
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.model.calculatePoints
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.model.getStationFillKg
import com.verifour.app.model.getStationFillPercent
import com.verifour.app.model.round2
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        private const val KEY_ADMIN_OPS = "yoha:admin_ops:v1"
        private const val KEY_SERVER_URL = "yoha:server_url:v1"
        private const val TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val api = FlysticApiClient(getServerUrl())
    private val secureRandom = SecureRandom()

    fun getServerUrl(): String =
        prefs.getString(KEY_SERVER_URL, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { FlysticApiClient.normalizeBaseUrl(it) }
            ?: FlysticApiClient.DEFAULT_BASE_URL

    fun saveServerUrl(url: String): String {
        val normalized = api.updateBaseUrl(url)
        prefs.edit().putString(KEY_SERVER_URL, normalized).apply()
        return normalized
    }

    suspend fun pingServer(url: String): Result<String> = api.checkHealth(url)

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
            inviteLink = "https://verifour.app/campaign/$code",
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
        val rawOps = prefs.getString(KEY_ADMIN_OPS, null)
        val existingOps = if (rawOps != null) {
            runCatching { gson.fromJson(rawOps, AdminOpsState::class.java) }.getOrNull()
        } else null
        val matchedAccount = existingOps?.accounts?.firstOrNull {
            it.email.equals(user.email, ignoreCase = true)
        }
        if (matchedAccount != null && matchedAccount.status != AccountStatus.ACTIVE) {
            val blockedLog = AccessLogEntry(
                id = "log-${UUID.randomUUID().toString().take(8)}",
                userId = matchedAccount.id,
                email = matchedAccount.email,
                role = matchedAccount.role.apiValue,
                eventType = "login_blocked",
                outcome = AccessOutcome.BLOCKED,
                deviceInfo = "Demo Mode",
                detail = "Tài khoản đang ở trạng thái ${matchedAccount.status.apiValue}",
                createdAt = System.currentTimeMillis()
            )
            saveLocalAdminOps(
                existingOps.copy(accessLogs = listOf(blockedLog) + existingOps.accessLogs)
            )
            throw FlysticApiException("Tài khoản đang bị ${matchedAccount.status.apiValue}", 403)
        }
        val next = currentState.copy(user = user)
        saveLocalState(next)
        if (existingOps != null) {
            val now = System.currentTimeMillis()
            val loginLog = AccessLogEntry(
                id = "log-${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                email = user.email,
                role = user.role.apiValue,
                eventType = if (isRegister) "register" else "login_success",
                outcome = AccessOutcome.SUCCESS,
                deviceInfo = "Demo Mode",
                detail = if (isAdmin) "Đăng nhập Admin (${assignedStation.id})" else "Đăng nhập Sinh viên",
                createdAt = now
            )
            val updatedAccounts = if (existingOps.accounts.any { it.email.equals(user.email, ignoreCase = true) }) {
                existingOps.accounts.map { acc ->
                    if (acc.email.equals(user.email, ignoreCase = true)) acc.copy(lastLoginAt = now) else acc
                }
            } else {
                listOf(
                    ManagedAccount(
                        id = user.id,
                        email = user.email,
                        name = user.name,
                        role = user.role,
                        studentId = user.studentId,
                        faculty = user.faculty,
                        stationId = if (isAdmin) assignedStation.id else null,
                        collectionPoint = user.collectionPoint,
                        status = AccountStatus.ACTIVE,
                        lastLoginAt = now,
                        createdAt = now
                    )
                ) + existingOps.accounts
            }
            saveLocalAdminOps(
                existingOps.copy(
                    accounts = updatedAccounts,
                    accessLogs = listOf(loginLog) + existingOps.accessLogs,
                    lastUpdatedAt = now
                )
            )
        }
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
        val rawOps = prefs.getString(KEY_ADMIN_OPS, null)
        if (rawOps != null) {
            runCatching { gson.fromJson(rawOps, AdminOpsState::class.java) }.getOrNull()?.let { ops ->
                val log = AccessLogEntry(
                    id = "log-${UUID.randomUUID().toString().take(8)}",
                    userId = user.id,
                    email = user.email,
                    role = "admin",
                    eventType = "qr_created",
                    outcome = AccessOutcome.SUCCESS,
                    deviceInfo = "Demo Mode",
                    detail = "Tạo QR $qrToken (+$totalPts pts, $totalQty kg)",
                    createdAt = now
                )
                saveLocalAdminOps(
                    ops.copy(
                        qrTokens = listOf(tx) + ops.qrTokens,
                        accessLogs = listOf(log) + ops.accessLogs,
                        lastUpdatedAt = now
                    )
                )
            }
        }
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

    // --- Admin Operations, Account, Token & Access Governance ---

    fun saveLocalAdminOps(ops: AdminOpsState) {
        prefs.edit().putString(KEY_ADMIN_OPS, gson.toJson(ops)).apply()
    }

    private fun buildDefaultDemoAdminOps(state: FlysticState): AdminOpsState {
        val now = System.currentTimeMillis()
        val currentUser = state.user
        val seededAccounts = buildList {
            if (currentUser != null) {
                add(
                    ManagedAccount(
                        id = currentUser.id,
                        email = currentUser.email,
                        name = currentUser.name,
                        role = currentUser.role,
                        studentId = currentUser.studentId,
                        faculty = currentUser.faculty,
                        stationId = COLLECTION_POINTS.firstOrNull {
                            it.canonicalName == currentUser.collectionPoint
                        }?.id ?: if (currentUser.role == Role.ADMIN) COLLECTION_POINTS.first().id else null,
                        collectionPoint = currentUser.collectionPoint,
                        status = AccountStatus.ACTIVE,
                        points = state.points,
                        totalKg = state.totalKg,
                        tokenVersion = 1,
                        lastLoginAt = now - 60_000L,
                        createdAt = now - 86_400_000L * 14
                    )
                )
            }
            addAll(
                listOf(
                    ManagedAccount(
                        id = "adm-soict-01",
                        email = "admin.b1@hust.edu.vn",
                        name = "Nguyễn Minh Quân",
                        role = Role.ADMIN,
                        stationId = "soict-b1",
                        collectionPoint = COLLECTION_POINTS[0].canonicalName,
                        status = AccountStatus.ACTIVE,
                        tokenVersion = 2,
                        lastLoginAt = now - 15 * 60_000L,
                        createdAt = now - 86_400_000L * 30
                    ),
                    ManagedAccount(
                        id = "adm-chem-02",
                        email = "admin.c3@hust.edu.vn",
                        name = "Phạm Lan Hương",
                        role = Role.ADMIN,
                        stationId = "chem-life-c3",
                        collectionPoint = COLLECTION_POINTS[1].canonicalName,
                        status = AccountStatus.ACTIVE,
                        tokenVersion = 1,
                        lastLoginAt = now - 2 * 3_600_000L,
                        createdAt = now - 86_400_000L * 25
                    ),
                    ManagedAccount(
                        id = "stu-20225101",
                        email = "linh.nt225101@sis.hust.edu.vn",
                        name = "Nguyễn Thảo Linh",
                        role = Role.STUDENT,
                        studentId = "20225101",
                        faculty = "Trường Công nghệ Thông tin và Truyền thông",
                        status = AccountStatus.ACTIVE,
                        points = 420,
                        totalKg = 18.4,
                        tokenVersion = 1,
                        lastLoginAt = now - 42 * 60_000L,
                        createdAt = now - 86_400_000L * 20
                    ),
                    ManagedAccount(
                        id = "stu-20223412",
                        email = "minh.tq223412@sis.hust.edu.vn",
                        name = "Trần Quang Minh",
                        role = Role.STUDENT,
                        studentId = "20223412",
                        faculty = "Trường Cơ khí",
                        status = AccountStatus.ACTIVE,
                        points = 315,
                        totalKg = 13.8,
                        tokenVersion = 1,
                        lastLoginAt = now - 4 * 3_600_000L,
                        createdAt = now - 86_400_000L * 18
                    ),
                    ManagedAccount(
                        id = "stu-20231088",
                        email = "an.lh231088@sis.hust.edu.vn",
                        name = "Lê Hoàng An",
                        role = Role.STUDENT,
                        studentId = "20231088",
                        faculty = "Trường Hóa và Khoa học Sự sống",
                        status = AccountStatus.LOCKED,
                        points = 95,
                        totalKg = 4.2,
                        tokenVersion = 3,
                        lastLoginAt = now - 86_400_000L * 2,
                        createdAt = now - 86_400_000L * 15
                    ),
                    ManagedAccount(
                        id = "stu-20214590",
                        email = "duc.pv214590@sis.hust.edu.vn",
                        name = "Phạm Văn Đức",
                        role = Role.STUDENT,
                        studentId = "20214590",
                        faculty = "Trường Điện - Điện tử",
                        status = AccountStatus.SUSPENDED,
                        points = 40,
                        totalKg = 1.9,
                        tokenVersion = 2,
                        lastLoginAt = now - 86_400_000L * 5,
                        createdAt = now - 86_400_000L * 12
                    )
                )
            )
        }.distinctBy { it.email }

        val seededInvites = listOf(
            AdminInviteToken(
                id = "inv-1",
                code = "ADM-B1K8-9P2Q",
                label = "CTV Trạm B1 - Ca sáng",
                stationId = "soict-b1",
                stationName = COLLECTION_POINTS[0].canonicalName,
                maxUses = 5,
                usedCount = 2,
                expiresAt = now + 48 * 3_600_000L,
                isRevoked = false,
                createdAt = now - 12 * 3_600_000L
            ),
            AdminInviteToken(
                id = "inv-2",
                code = "ADM-C7ME-4X7R",
                label = "Đội tình nguyện Cơ khí C7",
                stationId = "mechanical-c7",
                stationName = COLLECTION_POINTS[2].canonicalName,
                maxUses = 3,
                usedCount = 1,
                expiresAt = now + 24 * 3_600_000L,
                isRevoked = false,
                createdAt = now - 6 * 3_600_000L
            ),
            AdminInviteToken(
                id = "inv-3",
                code = "ADM-OLD1-7N3K",
                label = "Mã sự kiện Tuần lễ Xanh (Đã thu hồi)",
                stationId = "innovation-center",
                stationName = COLLECTION_POINTS[3].canonicalName,
                maxUses = 2,
                usedCount = 2,
                expiresAt = now - 2 * 3_600_000L,
                isRevoked = true,
                createdAt = now - 72 * 3_600_000L
            )
        )

        val seededLogs = listOf(
            AccessLogEntry(
                id = "log-1",
                userId = currentUser?.id ?: "adm-soict-01",
                email = currentUser?.email ?: "admin.b1@hust.edu.vn",
                role = "admin",
                eventType = "login_success",
                outcome = AccessOutcome.SUCCESS,
                ipAddress = "14.177.224.18",
                deviceInfo = "VeriFour Android 15",
                detail = "Đăng nhập cổng quản trị vận hành",
                createdAt = now - 3 * 60_000L
            ),
            AccessLogEntry(
                id = "log-2",
                userId = null,
                email = "unknown.bot@mail.ru",
                role = "student",
                eventType = "login_locked",
                outcome = AccessOutcome.BLOCKED,
                ipAddress = "103.82.14.91",
                deviceInfo = "okhttp/4.12",
                detail = "Khóa 15 phút sau 5 lần sai mật khẩu liên tiếp",
                createdAt = now - 14 * 60_000L
            ),
            AccessLogEntry(
                id = "log-3",
                userId = "stu-20231088",
                email = "an.lh231088@sis.hust.edu.vn",
                role = "student",
                eventType = "token_revoked",
                outcome = AccessOutcome.BLOCKED,
                ipAddress = "113.190.44.10",
                deviceInfo = "Admin Console",
                detail = "Thu hồi toàn bộ phiên đăng nhập (Token v3)",
                createdAt = now - 48 * 60_000L
            ),
            AccessLogEntry(
                id = "log-4",
                userId = "stu-20225101",
                email = "linh.nt225101@sis.hust.edu.vn",
                role = "student",
                eventType = "qr_confirmed",
                outcome = AccessOutcome.SUCCESS,
                ipAddress = "171.244.61.22",
                deviceInfo = "VeriFour Android 14",
                detail = "Xác nhận QR token +22 điểm PET tại Trạm B1",
                createdAt = now - 95 * 60_000L
            ),
            AccessLogEntry(
                id = "log-5",
                userId = null,
                email = "student.test@sis.hust.edu.vn",
                role = "student",
                eventType = "login_failed",
                outcome = AccessOutcome.FAILED,
                ipAddress = "42.118.92.11",
                deviceInfo = "Chrome Mobile",
                detail = "Sai mật khẩu (lần 3/5)",
                createdAt = now - 130 * 60_000L
            )
        )

        val seededLockouts = listOf(
            AuthLockoutEntry(
                throttleKey = "thr-demo-bot-91",
                emailHint = "unknown.bot@mail.ru",
                failureCount = 5,
                lockedUntil = now + 12 * 60_000L,
                updatedAt = now - 3 * 60_000L
            )
        )

        val stationSummaries = buildDemoStationOps(state, emptyMap())

        return AdminOpsState(
            stationsOps = stationSummaries,
            accounts = seededAccounts,
            qrTokens = buildDemoQrTokens(state, now),
            inviteTokens = seededInvites,
            accessLogs = seededLogs,
            lockouts = seededLockouts,
            lastUpdatedAt = now
        )
    }

    private fun buildDemoQrTokens(state: FlysticState, now: Long): List<Transaction> {
        if (state.transactions.isNotEmpty()) return state.transactions
        return listOf(
            Transaction(
                id = "qr-demo-1",
                token = "XK9M2P4RT7WQ",
                userId = null,
                userName = null,
                adminId = state.user?.id ?: "adm-soict-01",
                collectionPoint = COLLECTION_POINTS[0].canonicalName,
                items = listOf(TxItem(PlasticType.PET, 1.5, Cleanliness.CLEAN, 23)),
                totalQuantity = 1.5,
                weightKg = 1.5,
                points = 23,
                status = TransactionStatus.PENDING,
                createdAt = now - 45_000L,
                expiresAt = now + 4 * 60_000L + 15_000L
            ),
            Transaction(
                id = "qr-demo-2",
                token = "HN7B3V8PL2ZQ",
                userId = "stu-20225101",
                userName = "Nguyễn Thảo Linh",
                userFaculty = "Trường Công nghệ Thông tin và Truyền thông",
                adminId = "adm-soict-01",
                collectionPoint = COLLECTION_POINTS[0].canonicalName,
                items = listOf(TxItem(PlasticType.PP, 1.2, Cleanliness.NORMAL, 10)),
                totalQuantity = 1.2,
                weightKg = 1.2,
                points = 10,
                status = TransactionStatus.CONFIRMED,
                createdAt = now - 3_600_000L,
                confirmedAt = now - 3_550_000L,
                expiresAt = now - 3_300_000L
            ),
            Transaction(
                id = "qr-demo-3",
                token = "DM4K9W2CY6NR",
                userId = null,
                userName = null,
                adminId = "adm-chem-02",
                collectionPoint = COLLECTION_POINTS[1].canonicalName,
                items = listOf(TxItem(PlasticType.HDPE, 0.8, Cleanliness.CLEAN, 12)),
                totalQuantity = 0.8,
                weightKg = 0.8,
                points = 12,
                status = TransactionStatus.PENDING,
                createdAt = now - 10 * 60_000L,
                expiresAt = now - 5 * 60_000L
            )
        )
    }

    private fun buildDemoStationOps(
        state: FlysticState,
        existingActiveMap: Map<String, Boolean>
    ): List<StationOpsSummary> {
        val now = System.currentTimeMillis()
        val qrList = buildDemoQrTokens(state, now)
        return COLLECTION_POINTS.map { point ->
            val remote = state.stations.firstOrNull { it.id == point.id }
            val filled = if (state.transactions.isNotEmpty()) {
                getStationFillKg(state.transactions, state.collectionRuns, point.canonicalName, now)
            } else {
                remote?.filledKg ?: when (point.id) {
                    "soict-b1" -> 16.4
                    "chem-life-c3" -> 9.2
                    "mechanical-c7" -> 21.5
                    else -> 5.8
                }
            }
            val pct = if (state.transactions.isNotEmpty()) {
                getStationFillPercent(state.transactions, state.collectionRuns, point, now)
            } else {
                ((filled / point.capacityKg) * 100).toInt().coerceIn(0, 100)
            }
            StationOpsSummary(
                id = point.id,
                name = point.canonicalName,
                address = point.canonicalAddress,
                capacityKg = point.capacityKg,
                filledKg = filled,
                fillPercent = pct,
                isActive = existingActiveMap[point.id] ?: true,
                pendingTokens = qrList.count {
                    it.collectionPoint == point.canonicalName &&
                        effectiveTransactionStatus(it, now) == TransactionStatus.PENDING
                },
                confirmedToday = qrList.count {
                    it.collectionPoint == point.canonicalName &&
                        effectiveTransactionStatus(it, now) == TransactionStatus.CONFIRMED
                }
            )
        }
    }

    suspend fun loadAdminOpsState(currentState: FlysticState): AdminOpsState {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            return coroutineScope {
                val overviewDef = async { api.getAdminOpsOverview(token) }
                val usersDef = async { api.getAdminUsers(token) }
                val tokensDef = async { api.getAdminTokens(token) }
                val logsDef = async { api.getAdminAccessLogs(token) }

                val overview = overviewDef.await()
                val users = usersDef.await()
                val tokens = tokensDef.await()
                val logsResp = logsDef.await()

                AdminOpsState(
                    stationsOps = overview.stations.map {
                        StationOpsSummary(
                            id = it.id,
                            name = it.name,
                            address = it.address,
                            capacityKg = it.capacityKg,
                            filledKg = it.filledKg,
                            fillPercent = it.fillPercent,
                            isActive = it.isActive,
                            pendingTokens = it.pendingTokens,
                            confirmedToday = it.confirmedToday
                        )
                    },
                    accounts = users.map {
                        ManagedAccount(
                            id = it.id,
                            email = it.email,
                            name = it.name,
                            role = Role.fromApi(it.role),
                            studentId = it.studentId,
                            faculty = it.faculty,
                            stationId = it.stationId,
                            collectionPoint = it.collectionPoint,
                            status = AccountStatus.fromApi(it.status),
                            points = it.points,
                            totalKg = it.totalKg,
                            tokenVersion = it.tokenVersion ?: 1,
                            lastLoginAt = it.lastLoginAt,
                            createdAt = it.createdAt ?: System.currentTimeMillis()
                        )
                    },
                    qrTokens = tokens.qrTransactions.map(::mapTransaction),
                    inviteTokens = tokens.invites.map { inv ->
                        AdminInviteToken(
                            id = inv.id,
                            code = inv.code,
                            label = inv.label,
                            stationId = inv.stationId,
                            stationName = inv.stationName ?: inv.stationId,
                            maxUses = inv.maxUses,
                            usedCount = inv.usedCount,
                            expiresAt = inv.expiresAt,
                            isRevoked = inv.isRevoked,
                            createdAt = inv.createdAt
                        )
                    },
                    accessLogs = logsResp.logs.map { l ->
                        AccessLogEntry(
                            id = l.id,
                            userId = l.userId,
                            email = l.email,
                            role = l.role,
                            eventType = l.eventType,
                            outcome = AccessOutcome.fromApi(l.outcome),
                            ipAddress = l.ipAddress,
                            deviceInfo = l.deviceInfo,
                            detail = l.detail,
                            createdAt = l.createdAt
                        )
                    },
                    lockouts = logsResp.lockouts.map { lk ->
                        AuthLockoutEntry(
                            throttleKey = lk.throttleKey,
                            emailHint = lk.emailHint,
                            failureCount = lk.failureCount,
                            lockedUntil = lk.lockedUntil,
                            updatedAt = lk.updatedAt
                        )
                    },
                    lastUpdatedAt = System.currentTimeMillis()
                )
            }
        }

        val raw = prefs.getString(KEY_ADMIN_OPS, null)
        val stored = if (raw != null) {
            runCatching { gson.fromJson(raw, AdminOpsState::class.java) }.getOrNull()
        } else null
        val base = stored ?: buildDefaultDemoAdminOps(currentState)
        val activeMap = base.stationsOps.associate { it.id to it.isActive }
        val synced = base.copy(
            stationsOps = buildDemoStationOps(currentState, activeMap),
            qrTokens = if (currentState.transactions.isNotEmpty()) currentState.transactions else base.qrTokens,
            lastUpdatedAt = System.currentTimeMillis()
        )
        saveLocalAdminOps(synced)
        return synced
    }

    suspend fun createManagedAccount(
        currentState: FlysticState,
        currentOps: AdminOpsState,
        name: String,
        email: String,
        password: String,
        role: Role,
        studentId: String?,
        faculty: String?,
        stationId: String?
    ): AdminOpsState {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.createAdminUser(
                token = token,
                name = cleanName,
                email = cleanEmail,
                password = password,
                role = role.apiValue,
                studentId = studentId?.takeIf { it.isNotBlank() },
                faculty = faculty?.takeIf { it.isNotBlank() },
                stationId = if (role == Role.ADMIN) stationId else null
            )
            return loadAdminOpsState(currentState)
        }
        if (currentOps.accounts.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            throw FlysticApiException("Email already registered", 409)
        }
        val now = System.currentTimeMillis()
        val station = if (role == Role.ADMIN) {
            COLLECTION_POINTS.firstOrNull { it.id == stationId } ?: COLLECTION_POINTS.first()
        } else null
        val newAcc = ManagedAccount(
            id = "usr-${UUID.randomUUID().toString().take(8)}",
            email = cleanEmail,
            name = cleanName,
            role = role,
            studentId = if (role == Role.STUDENT) studentId?.takeIf { it.isNotBlank() } else null,
            faculty = if (role == Role.STUDENT) faculty?.takeIf { it.isNotBlank() } else null,
            stationId = station?.id,
            collectionPoint = station?.canonicalName,
            status = AccountStatus.ACTIVE,
            tokenVersion = 1,
            createdAt = now
        )
        val log = AccessLogEntry(
            id = "log-${UUID.randomUUID().toString().take(8)}",
            userId = currentState.user?.id,
            email = currentState.user?.email ?: "admin@hust.edu.vn",
            role = "admin",
            eventType = "account_created",
            outcome = AccessOutcome.SUCCESS,
            deviceInfo = "Admin Ops Mobile",
            detail = "Tạo tài khoản ${role.apiValue}: $cleanEmail",
            createdAt = now
        )
        val next = currentOps.copy(
            accounts = listOf(newAcc) + currentOps.accounts,
            accessLogs = listOf(log) + currentOps.accessLogs,
            lastUpdatedAt = now
        )
        saveLocalAdminOps(next)
        return next
    }

    suspend fun updateManagedAccountStatus(
        currentState: FlysticState,
        currentOps: AdminOpsState,
        userId: String,
        status: AccountStatus
    ): AdminOpsState {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.updateAdminUserStatus(token, userId, status.apiValue)
            return loadAdminOpsState(currentState)
        }
        val now = System.currentTimeMillis()
        val target = currentOps.accounts.firstOrNull { it.id == userId }
        val updated = currentOps.accounts.map { acc ->
            if (acc.id == userId) {
                acc.copy(
                    status = status,
                    tokenVersion = if (status != AccountStatus.ACTIVE) acc.tokenVersion + 1 else acc.tokenVersion
                )
            } else acc
        }
        val log = AccessLogEntry(
            id = "log-${UUID.randomUUID().toString().take(8)}",
            userId = currentState.user?.id,
            email = currentState.user?.email ?: "admin@hust.edu.vn",
            role = "admin",
            eventType = "account_${status.apiValue}",
            outcome = if (status == AccountStatus.ACTIVE) AccessOutcome.SUCCESS else AccessOutcome.BLOCKED,
            deviceInfo = "Admin Ops Mobile",
            detail = "Đổi trạng thái ${target?.email ?: userId} → ${status.apiValue}",
            createdAt = now
        )
        val next = currentOps.copy(
            accounts = updated,
            accessLogs = listOf(log) + currentOps.accessLogs,
            lastUpdatedAt = now
        )
        saveLocalAdminOps(next)
        return next
    }

    suspend fun updateManagedAccountRole(
        currentState: FlysticState,
        currentOps: AdminOpsState,
        userId: String,
        role: Role,
        stationId: String?
    ): AdminOpsState {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.updateAdminUserRole(token, userId, role.apiValue, stationId)
            return loadAdminOpsState(currentState)
        }
        val now = System.currentTimeMillis()
        val station = if (role == Role.ADMIN) {
            COLLECTION_POINTS.firstOrNull { it.id == stationId } ?: COLLECTION_POINTS.first()
        } else null
        val target = currentOps.accounts.firstOrNull { it.id == userId }
        val updated = currentOps.accounts.map { acc ->
            if (acc.id == userId) {
                acc.copy(
                    role = role,
                    stationId = station?.id,
                    collectionPoint = station?.canonicalName
                )
            } else acc
        }
        val log = AccessLogEntry(
            id = "log-${UUID.randomUUID().toString().take(8)}",
            userId = currentState.user?.id,
            email = currentState.user?.email ?: "admin@hust.edu.vn",
            role = "admin",
            eventType = "account_role_updated",
            outcome = AccessOutcome.SUCCESS,
            deviceInfo = "Admin Ops Mobile",
            detail = "Phân quyền ${target?.email ?: userId} → ${role.apiValue}${station?.let { " (${it.id})" } ?: ""}",
            createdAt = now
        )
        val next = currentOps.copy(
            accounts = updated,
            accessLogs = listOf(log) + currentOps.accessLogs,
            lastUpdatedAt = now
        )
        saveLocalAdminOps(next)
        return next
    }

    suspend fun revokeManagedUserTokens(
        currentState: FlysticState,
        currentOps: AdminOpsState,
        userId: String
    ): AdminOpsState {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.revokeAdminUserTokens(token, userId)
            return loadAdminOpsState(currentState)
        }
        val now = System.currentTimeMillis()
        val target = currentOps.accounts.firstOrNull { it.id == userId }
        var newVersion = 2
        val updated = currentOps.accounts.map { acc ->
            if (acc.id == userId) {
                newVersion = acc.tokenVersion + 1
                acc.copy(tokenVersion = newVersion)
            } else acc
        }
        val log = AccessLogEntry(
            id = "log-${UUID.randomUUID().toString().take(8)}",
            userId = currentState.user?.id,
            email = currentState.user?.email ?: "admin@hust.edu.vn",
            role = "admin",
            eventType = "token_revoked",
            outcome = AccessOutcome.BLOCKED,
            deviceInfo = "Admin Ops Mobile",
            detail = "Thu hồi phiên đăng nhập ${target?.email ?: userId} (Token v$newVersion)",
            createdAt = now
        )
        val next = currentOps.copy(
            accounts = updated,
            accessLogs = listOf(log) + currentOps.accessLogs,
            lastUpdatedAt = now
        )
        saveLocalAdminOps(next)
        return next
    }

    suspend fun revokeQrToken(
        currentState: FlysticState,
        currentOps: AdminOpsState,
        txId: String
    ): Pair<FlysticState, AdminOpsState> {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.revokeAdminQrToken(token, txId)
            val nextState = refreshRemoteState(token, currentState.locale)
            val nextOps = loadAdminOpsState(nextState)
            return nextState to nextOps
        }
        val now = System.currentTimeMillis()
        val nextTransactions = currentState.transactions.map {
            if (it.id == txId && it.status == TransactionStatus.PENDING) {
                it.copy(status = TransactionStatus.CANCELLED)
            } else it
        }
        val nextState = currentState.copy(transactions = nextTransactions)
        saveLocalState(nextState)

        val nextQrTokens = currentOps.qrTokens.map {
            if (it.id == txId && it.status == TransactionStatus.PENDING) {
                it.copy(status = TransactionStatus.CANCELLED)
            } else it
        }
        val log = AccessLogEntry(
            id = "log-${UUID.randomUUID().toString().take(8)}",
            userId = currentState.user?.id,
            email = currentState.user?.email ?: "admin@hust.edu.vn",
            role = "admin",
            eventType = "qr_token_revoked",
            outcome = AccessOutcome.BLOCKED,
            deviceInfo = "Admin Ops Mobile",
            detail = "Thu hồi QR token #$txId",
            createdAt = now
        )
        val nextOps = currentOps.copy(
            qrTokens = nextQrTokens,
            accessLogs = listOf(log) + currentOps.accessLogs,
            lastUpdatedAt = now
        )
        saveLocalAdminOps(nextOps)
        return nextState to nextOps
    }

    suspend fun expireStaleQrTokens(
        currentState: FlysticState,
        currentOps: AdminOpsState
    ): Pair<FlysticState, AdminOpsState> {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.expireStaleQrTokens(token)
            val nextState = refreshRemoteState(token, currentState.locale)
            val nextOps = loadAdminOpsState(nextState)
            return nextState to nextOps
        }
        val now = System.currentTimeMillis()
        val nextTransactions = currentState.transactions.map {
            if (it.status == TransactionStatus.PENDING && now > it.expiresAt) {
                it.copy(status = TransactionStatus.EXPIRED)
            } else it
        }
        val nextState = currentState.copy(transactions = nextTransactions)
        saveLocalState(nextState)
        val nextQrTokens = currentOps.qrTokens.map {
            if (it.status == TransactionStatus.PENDING && now > it.expiresAt) {
                it.copy(status = TransactionStatus.EXPIRED)
            } else it
        }
        val nextOps = currentOps.copy(qrTokens = nextQrTokens, lastUpdatedAt = now)
        saveLocalAdminOps(nextOps)
        return nextState to nextOps
    }

    suspend fun createAdminInviteToken(
        currentState: FlysticState,
        currentOps: AdminOpsState,
        label: String,
        stationId: String,
        maxUses: Int,
        ttlHours: Int
    ): AdminOpsState {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.createAdminInvite(token, label, stationId, maxUses, ttlHours)
            return loadAdminOpsState(currentState)
        }
        val now = System.currentTimeMillis()
        val station = COLLECTION_POINTS.firstOrNull { it.id == stationId } ?: COLLECTION_POINTS.first()
        val code = "ADM-${generateQrToken(4)}-${generateQrToken(4)}"
        val invite = AdminInviteToken(
            id = "inv-${UUID.randomUUID().toString().take(8)}",
            code = code,
            label = label.trim().ifBlank { "Mã mời CTV ${station.id}" },
            stationId = station.id,
            stationName = station.canonicalName,
            maxUses = maxUses.coerceIn(1, 50),
            usedCount = 0,
            expiresAt = now + ttlHours.coerceIn(1, 720) * 3_600_000L,
            isRevoked = false,
            createdAt = now
        )
        val log = AccessLogEntry(
            id = "log-${UUID.randomUUID().toString().take(8)}",
            userId = currentState.user?.id,
            email = currentState.user?.email ?: "admin@hust.edu.vn",
            role = "admin",
            eventType = "invite_created",
            outcome = AccessOutcome.SUCCESS,
            deviceInfo = "Admin Ops Mobile",
            detail = "Tạo mã mời $code (${station.id})",
            createdAt = now
        )
        val next = currentOps.copy(
            inviteTokens = listOf(invite) + currentOps.inviteTokens,
            accessLogs = listOf(log) + currentOps.accessLogs,
            lastUpdatedAt = now
        )
        saveLocalAdminOps(next)
        return next
    }

    suspend fun revokeAdminInviteToken(
        currentState: FlysticState,
        currentOps: AdminOpsState,
        inviteId: String
    ): AdminOpsState {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.revokeAdminInvite(token, inviteId)
            return loadAdminOpsState(currentState)
        }
        val now = System.currentTimeMillis()
        val target = currentOps.inviteTokens.firstOrNull { it.id == inviteId }
        val updated = currentOps.inviteTokens.map {
            if (it.id == inviteId) it.copy(isRevoked = true) else it
        }
        val log = AccessLogEntry(
            id = "log-${UUID.randomUUID().toString().take(8)}",
            userId = currentState.user?.id,
            email = currentState.user?.email ?: "admin@hust.edu.vn",
            role = "admin",
            eventType = "invite_revoked",
            outcome = AccessOutcome.BLOCKED,
            deviceInfo = "Admin Ops Mobile",
            detail = "Thu hồi mã mời ${target?.code ?: inviteId}",
            createdAt = now
        )
        val next = currentOps.copy(
            inviteTokens = updated,
            accessLogs = listOf(log) + currentOps.accessLogs,
            lastUpdatedAt = now
        )
        saveLocalAdminOps(next)
        return next
    }

    suspend fun toggleStationOperational(
        currentState: FlysticState,
        currentOps: AdminOpsState,
        stationId: String
    ): AdminOpsState {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.toggleAdminStation(token, stationId)
            return loadAdminOpsState(currentState)
        }
        val now = System.currentTimeMillis()
        var nextActive = true
        var stationName = stationId
        val updatedStations = currentOps.stationsOps.map { st ->
            if (st.id == stationId) {
                nextActive = !st.isActive
                stationName = st.name
                st.copy(isActive = nextActive)
            } else st
        }
        val log = AccessLogEntry(
            id = "log-${UUID.randomUUID().toString().take(8)}",
            userId = currentState.user?.id,
            email = currentState.user?.email ?: "admin@hust.edu.vn",
            role = "admin",
            eventType = if (nextActive) "station_resumed" else "station_paused",
            outcome = AccessOutcome.SUCCESS,
            deviceInfo = "Admin Ops Mobile",
            detail = "$stationName → ${if (nextActive) "Hoạt động" else "Tạm ngưng"}",
            createdAt = now
        )
        val next = currentOps.copy(
            stationsOps = updatedStations,
            accessLogs = listOf(log) + currentOps.accessLogs,
            lastUpdatedAt = now
        )
        saveLocalAdminOps(next)
        return next
    }

    suspend fun clearAuthLockout(
        currentState: FlysticState,
        currentOps: AdminOpsState,
        throttleKey: String?
    ): AdminOpsState {
        if (isRemoteMode()) {
            val token = getSessionToken() ?: throw FlysticApiException("Session expired", 401)
            api.clearAuthThrottle(token, throttleKey)
            return loadAdminOpsState(currentState)
        }
        val now = System.currentTimeMillis()
        val remaining = if (throttleKey.isNullOrBlank()) {
            emptyList()
        } else {
            currentOps.lockouts.filterNot { it.throttleKey == throttleKey }
        }
        val log = AccessLogEntry(
            id = "log-${UUID.randomUUID().toString().take(8)}",
            userId = currentState.user?.id,
            email = currentState.user?.email ?: "admin@hust.edu.vn",
            role = "admin",
            eventType = "throttle_cleared",
            outcome = AccessOutcome.SUCCESS,
            deviceInfo = "Admin Ops Mobile",
            detail = if (throttleKey.isNullOrBlank()) "Gỡ toàn bộ khóa đăng nhập" else "Gỡ khóa đăng nhập ($throttleKey)",
            createdAt = now
        )
        val next = currentOps.copy(
            lockouts = remaining,
            accessLogs = listOf(log) + currentOps.accessLogs,
            lastUpdatedAt = now
        )
        saveLocalAdminOps(next)
        return next
    }
}
