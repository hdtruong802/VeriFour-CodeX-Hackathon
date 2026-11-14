package com.verifour.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verifour.app.data.FlysticApiClient
import com.verifour.app.data.FlysticRepository
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AccountStatus
import com.verifour.app.model.AdminOpsState
import com.verifour.app.model.AppLocale
import com.verifour.app.model.CampaignParticipant
import com.verifour.app.model.CampaignPhoto
import com.verifour.app.model.CityBuildingType
import com.verifour.app.model.Cleanliness
import com.verifour.app.model.CollectionRun
import com.verifour.app.model.EcoCampaign
import com.verifour.app.model.EcoPersona
import com.verifour.app.model.FlysticState
import com.verifour.app.model.HabitatTheme
import com.verifour.app.model.InvitedCityMascot
import com.verifour.app.model.LeaderboardRow
import com.verifour.app.model.PetAnimationEvent
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetPersonality
import com.verifour.app.model.PetProgression
import com.verifour.app.model.PetReactionContext
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PlasticType
import com.verifour.app.model.PointDestination
import com.verifour.app.model.Role
import com.verifour.app.model.StudentOrganization
import com.verifour.app.model.Transaction
import com.verifour.app.model.User
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.model.round2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

data class FlysticUiState(
    val state: FlysticState = FlysticState(),
    val isReady: Boolean = false,
    val isRemote: Boolean = true,
    val serverUrl: String = FlysticApiClient.DEFAULT_BASE_URL,
    val petState: PetCompanionState = PetCompanionState(),
    val personalization: UserPersonalizationState = UserPersonalizationState(),
    val campaigns: List<EcoCampaign> = emptyList(),
    val adminOps: AdminOpsState = AdminOpsState(),
    val isAdminOpsLoading: Boolean = false,
    val petEvent: PetAnimationEvent = PetAnimationEvent.IDLE,
    val petEventTrigger: Int = 0,
    val petEventPointsDelta: Int = 0,
    val petEventNewLevel: Int = 1,
    val petReactionKey: String? = null
)

class FlysticViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FlysticRepository(application)

    private val _uiState = MutableStateFlow(
        FlysticUiState(
            state = FlysticState(locale = repository.getPersistedLocale()),
            isReady = false,
            isRemote = repository.isRemoteMode(),
            serverUrl = repository.getServerUrl()
        )
    )
    val uiState: StateFlow<FlysticUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repository.loadInitialState()
            val pet = initializePetForUser(loaded)
            val personalization = repository.loadPersonalization(loaded.user?.id)
            val campaigns = repository.loadCampaigns(loaded.user?.id)
            val adminOps = if (loaded.user?.role == Role.ADMIN) {
                runCatching { repository.loadAdminOpsState(loaded) }.getOrElse { AdminOpsState() }
            } else {
                AdminOpsState()
            }
            _uiState.update {
                it.copy(
                    state = loaded,
                    isReady = true,
                    isRemote = repository.isRemoteMode(),
                    petState = pet,
                    personalization = personalization,
                    campaigns = campaigns,
                    adminOps = adminOps
                )
            }
        }
    }

    private fun initializePetForUser(loaded: FlysticState): PetCompanionState {
        val raw = repository.loadPetState(loaded.user?.id)
        val normalized = if (raw.lastCelebratedPoints == 0 && loaded.points > 0) {
            raw.copy(lastCelebratedPoints = loaded.points).also {
                repository.savePetState(loaded.user?.id, it)
            }
        } else {
            raw
        }
        return normalized
    }

    fun t(key: String, params: Map<String, Any> = emptyMap()): String =
        FlysticTranslations.t(key, _uiState.value.state.locale, params)

    fun setLocale(locale: AppLocale) {
        repository.saveLocale(locale)
        _uiState.update { current ->
            val updatedState = current.state.copy(locale = locale)
            if (!current.isRemote) repository.saveLocalState(updatedState)
            current.copy(state = updatedState)
        }
    }

    fun setServerUrl(url: String): String {
        val normalized = repository.saveServerUrl(url)
        _uiState.update { it.copy(serverUrl = normalized) }
        return normalized
    }

    suspend fun testServerConnection(url: String): Result<String> {
        val normalized = setServerUrl(url)
        return repository.pingServer(normalized)
    }

    fun setRemoteMode(remote: Boolean) {
        if (_uiState.value.isRemote == remote) return
        repository.setRemoteMode(remote)
        viewModelScope.launch {
            val loaded = repository.loadInitialState()
            val pet = initializePetForUser(loaded)
            val personalization = repository.loadPersonalization(loaded.user?.id)
            val campaigns = repository.loadCampaigns(loaded.user?.id)
            _uiState.update {
                it.copy(
                    state = loaded,
                    isRemote = remote,
                    isReady = true,
                    petState = pet,
                    personalization = personalization,
                    campaigns = campaigns
                )
            }
        }
    }

    suspend fun signIn(
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
    ): User {
        val next = repository.signIn(
            currentState = _uiState.value.state,
            email = email,
            password = password,
            isRegister = isRegister,
            role = role,
            name = name,
            birthDate = birthDate,
            organization = organization,
            school = school,
            organizationDetail = organizationDetail,
            studentId = studentId,
            stationId = stationId,
            inviteCode = inviteCode
        )
        val pet = initializePetForUser(next)
        val personalization = repository.loadPersonalization(next.user?.id)
        val campaigns = repository.loadCampaigns(next.user?.id)
        val adminOps = if (next.user?.role == Role.ADMIN) {
            runCatching { repository.loadAdminOpsState(next) }.getOrElse { AdminOpsState() }
        } else {
            AdminOpsState()
        }
        _uiState.update {
            it.copy(
                state = next,
                petState = pet,
                personalization = personalization,
                campaigns = campaigns,
                adminOps = adminOps,
                petReactionKey = null
            )
        }
        return next.user!!
    }

    fun signOut() {
        val next = repository.signOut(_uiState.value.state)
        _uiState.update {
            it.copy(
                state = next,
                petState = PetCompanionState(),
                personalization = UserPersonalizationState(),
                campaigns = emptyList(),
                adminOps = AdminOpsState(),
                petEvent = PetAnimationEvent.IDLE,
                petEventTrigger = 0,
                petReactionKey = null
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (_uiState.value.isRemote && repository.getSessionToken() != null) {
                runCatching { repository.refreshRemoteState(locale = _uiState.value.state.locale) }
                    .onSuccess { next -> applyPointsUpdate(next) }
            } else {
                val stations = repository.fetchStations()
                _uiState.update { it.copy(state = it.state.copy(stations = stations)) }
            }
            if (_uiState.value.state.user?.role == Role.ADMIN) {
                refreshAdminOps()
            }
        }
    }

    fun refreshAdminOps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdminOpsLoading = true) }
            val loaded = runCatching { repository.loadAdminOpsState(_uiState.value.state) }.getOrNull()
            _uiState.update { current ->
                current.copy(
                    adminOps = loaded ?: current.adminOps,
                    isAdminOpsLoading = false
                )
            }
        }
    }

    suspend fun createManagedAccount(
        name: String,
        email: String,
        password: String,
        role: Role,
        studentId: String?,
        faculty: String?,
        stationId: String?
    ) {
        val nextOps = repository.createManagedAccount(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps,
            name = name,
            email = email,
            password = password,
            role = role,
            studentId = studentId,
            faculty = faculty,
            stationId = stationId
        )
        _uiState.update { it.copy(adminOps = nextOps) }
    }

    suspend fun updateAccountStatus(userId: String, status: AccountStatus) {
        val nextOps = repository.updateManagedAccountStatus(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps,
            userId = userId,
            status = status
        )
        _uiState.update { it.copy(adminOps = nextOps) }
    }

    suspend fun updateAccountRole(userId: String, role: Role, stationId: String?) {
        val nextOps = repository.updateManagedAccountRole(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps,
            userId = userId,
            role = role,
            stationId = stationId
        )
        _uiState.update { it.copy(adminOps = nextOps) }
    }

    suspend fun revokeUserTokens(userId: String) {
        val nextOps = repository.revokeManagedUserTokens(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps,
            userId = userId
        )
        _uiState.update { it.copy(adminOps = nextOps) }
    }

    suspend fun revokeQrToken(txId: String) {
        val (nextState, nextOps) = repository.revokeQrToken(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps,
            txId = txId
        )
        _uiState.update { it.copy(state = nextState, adminOps = nextOps) }
    }

    suspend fun expireStaleQrTokens() {
        val (nextState, nextOps) = repository.expireStaleQrTokens(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps
        )
        _uiState.update { it.copy(state = nextState, adminOps = nextOps) }
    }

    suspend fun createAdminInviteToken(
        label: String,
        stationId: String,
        maxUses: Int,
        ttlHours: Int
    ) {
        val nextOps = repository.createAdminInviteToken(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps,
            label = label,
            stationId = stationId,
            maxUses = maxUses,
            ttlHours = ttlHours
        )
        _uiState.update { it.copy(adminOps = nextOps) }
    }

    suspend fun revokeAdminInviteToken(inviteId: String) {
        val nextOps = repository.revokeAdminInviteToken(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps,
            inviteId = inviteId
        )
        _uiState.update { it.copy(adminOps = nextOps) }
    }

    suspend fun toggleStationOperational(stationId: String) {
        val nextOps = repository.toggleStationOperational(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps,
            stationId = stationId
        )
        _uiState.update { it.copy(adminOps = nextOps) }
    }

    suspend fun clearAuthLockout(throttleKey: String?) {
        val nextOps = repository.clearAuthLockout(
            currentState = _uiState.value.state,
            currentOps = _uiState.value.adminOps,
            throttleKey = throttleKey
        )
        _uiState.update { it.copy(adminOps = nextOps) }
    }

    suspend fun createTransaction(
        items: List<Triple<PlasticType, Double, Cleanliness>>,
        collectionPoint: String
    ): Transaction {
        val (created, next) = repository.createTransaction(
            currentState = _uiState.value.state,
            items = items,
            collectionPoint = collectionPoint
        )
        _uiState.update { it.copy(state = next) }
        refreshAdminOps()
        return created
    }

    suspend fun getTransaction(qrToken: String): Transaction? =
        repository.getTransaction(_uiState.value.state, qrToken)

    suspend fun confirmTransaction(
        qrToken: String,
        destination: PointDestination,
        previewTx: Transaction?
    ): Transaction? {
        val (confirmed, next) = repository.confirmTransaction(
            currentState = _uiState.value.state,
            qrToken = qrToken,
            destination = destination,
            previewTx = previewTx
        )
        applyPointsUpdate(next, fallbackDelta = confirmed?.points ?: 0)
        return confirmed
    }

    suspend fun cancelTransaction(id: String) {
        val next = repository.cancelTransaction(_uiState.value.state, id)
        _uiState.update { it.copy(state = next) }
        refreshAdminOps()
    }

    suspend fun emptyCollectionPoint(collectionPoint: String): CollectionRun? {
        val (run, next) = repository.emptyCollectionPoint(_uiState.value.state, collectionPoint)
        _uiState.update { it.copy(state = next) }
        refreshAdminOps()
        return run
    }

    suspend fun getLeaderboard(period: String): List<LeaderboardRow> =
        repository.fetchLeaderboard(period, _uiState.value.state)

    fun selectPetSpecies(species: PetSpecies) {
        _uiState.update { current ->
            val updatedPet = current.petState.copy(species = species)
            repository.savePetState(current.state.user?.id, updatedPet)
            val personality = current.personalization.resolvedPersonality(species)
            current.copy(
                petState = updatedPet,
                petEvent = PetAnimationEvent.HAPPY_TAP,
                petEventTrigger = current.petEventTrigger + 1,
                petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.PAT)
            )
        }
    }

    fun patPet() {
        _uiState.update { current ->
            val updatedPet = current.petState.copy(patsCount = current.petState.patsCount + 1)
            repository.savePetState(current.state.user?.id, updatedPet)
            val personality = current.personalization.resolvedPersonality(updatedPet.species)
            val updatedPersonalization = current.personalization.copy(
                petBond = min(100, current.personalization.petBond + 2)
            )
            repository.savePersonalization(current.state.user?.id, updatedPersonalization)
            current.copy(
                petState = updatedPet,
                personalization = updatedPersonalization,
                petEvent = PetAnimationEvent.HAPPY_TAP,
                petEventTrigger = current.petEventTrigger + 1,
                petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.PAT)
            )
        }
    }

    fun feedPet(xpGain: Int = PetProgression.FEED_BONUS_XP) {
        _uiState.update { current ->
            val updatedPet = current.petState.copy(bonusXp = current.petState.bonusXp + xpGain)
            repository.savePetState(current.state.user?.id, updatedPet)
            val city = current.personalization.safeCity()
            val updatedPersonalization = current.personalization.copy(
                petBond = min(100, current.personalization.petBond + 4),
                city = city.copy(
                    ecoSeeds = city.ecoSeeds + 5,
                    waterDrops = city.waterDrops + 1
                )
            )
            repository.savePersonalization(current.state.user?.id, updatedPersonalization)
            val personality = updatedPersonalization.resolvedPersonality(updatedPet.species)
            current.copy(
                petState = updatedPet,
                personalization = updatedPersonalization,
                petEvent = PetAnimationEvent.POINTS_ADDED,
                petEventTrigger = current.petEventTrigger + 1,
                petEventPointsDelta = xpGain,
                petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.FEED)
            )
        }
    }

    fun upgradePet() {
        _uiState.update { current ->
            if (!PetProgression.canUpgrade(current.state.points, current.petState)) {
                return@update current
            }
            val nextLevel = (current.petState.level + 1).coerceAtMost(99)
            val updatedPet = current.petState.copy(level = nextLevel)
            repository.savePetState(current.state.user?.id, updatedPet)
            val city = current.personalization.safeCity()
            val updatedPersonalization = current.personalization.copy(
                petBond = min(100, current.personalization.petBond + 8),
                city = city.copy(
                    ecoSeeds = city.ecoSeeds + 20,
                    waterDrops = city.waterDrops + 2
                )
            )
            repository.savePersonalization(current.state.user?.id, updatedPersonalization)
            val personality = updatedPersonalization.resolvedPersonality(updatedPet.species)
            current.copy(
                petState = updatedPet,
                personalization = updatedPersonalization,
                petEvent = PetAnimationEvent.LEVEL_UP,
                petEventTrigger = current.petEventTrigger + 1,
                petEventNewLevel = nextLevel,
                petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.LEVEL_UP)
            )
        }
    }

    fun demoGrantAndUpgradePet() {
        _uiState.update { current ->
            val nextLevel = (current.petState.level + 1).coerceAtMost(99)
            val neededTotal = PetProgression.xpThresholdForLevel(nextLevel)
            val currentTotal = PetProgression.totalXp(current.state.points, current.petState)
            val extraBonus = (neededTotal - currentTotal).coerceAtLeast(0)
            val updatedPet = current.petState.copy(
                level = nextLevel,
                bonusXp = current.petState.bonusXp + extraBonus
            )
            repository.savePetState(current.state.user?.id, updatedPet)
            val city = current.personalization.safeCity()
            val updatedPersonalization = current.personalization.copy(
                petBond = min(100, current.personalization.petBond + 10),
                city = city.copy(
                    ecoSeeds = city.ecoSeeds + 30,
                    waterDrops = city.waterDrops + 2
                )
            )
            repository.savePersonalization(current.state.user?.id, updatedPersonalization)
            val personality = updatedPersonalization.resolvedPersonality(updatedPet.species)
            current.copy(
                petState = updatedPet,
                personalization = updatedPersonalization,
                petEvent = PetAnimationEvent.LEVEL_UP,
                petEventTrigger = current.petEventTrigger + 1,
                petEventNewLevel = nextLevel,
                petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.LEVEL_UP)
            )
        }
    }

    fun waterEcoTree() {
        _uiState.update { current ->
            val city = current.personalization.safeCity()
            if (city.waterDrops <= 0) return@update current
            val nextTreeLevel = city.treeLevel + 1
            val nextTreesPlanted = if (nextTreeLevel % 2 == 0) city.treesPlanted + 1 else city.treesPlanted
            val updatedCity = city.copy(
                waterDrops = city.waterDrops - 1,
                treeLevel = nextTreeLevel,
                treesPlanted = nextTreesPlanted,
                ecoSeeds = city.ecoSeeds + 10
            )
            val updatedPersonalization = current.personalization.copy(
                city = updatedCity,
                petBond = min(100, current.personalization.petBond + 5)
            )
            repository.savePersonalization(current.state.user?.id, updatedPersonalization)
            val personality = updatedPersonalization.resolvedPersonality(current.petState.species)
            current.copy(
                personalization = updatedPersonalization,
                petEvent = PetAnimationEvent.HAPPY_TAP,
                petEventTrigger = current.petEventTrigger + 1,
                petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.WATER_TREE)
            )
        }
    }

    fun upgradeCityBuilding(building: CityBuildingType) {
        _uiState.update { current ->
            val city = current.personalization.safeCity()
            val curLvl = city.levelOf(building)
            if (curLvl >= building.maxLevel) return@update current
            val cost = building.upgradeCost(curLvl)
            if (city.ecoSeeds < cost) return@update current
            val nextMap = city.buildingLevels.orEmpty().toMutableMap().apply {
                put(building.id, curLvl + 1)
            }
            val updatedCity = city.copy(
                ecoSeeds = city.ecoSeeds - cost,
                buildingLevels = nextMap
            )
            val updatedPersonalization = current.personalization.copy(
                city = updatedCity,
                petBond = min(100, current.personalization.petBond + 6)
            )
            repository.savePersonalization(current.state.user?.id, updatedPersonalization)
            val personality = updatedPersonalization.resolvedPersonality(current.petState.species)
            current.copy(
                personalization = updatedPersonalization,
                petEvent = PetAnimationEvent.POINTS_ADDED,
                petEventTrigger = current.petEventTrigger + 1,
                petEventPointsDelta = 15,
                petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.CITY_BUILD)
            )
        }
    }

    fun unlockOrEquipSkin(skin: PetSkin) {
        _uiState.update { current ->
            val unlocked = current.personalization.safeUnlockedSkins()
            val city = current.personalization.safeCity()
            if (skin.id in unlocked) {
                val updated = current.personalization.copy(activeSkinId = skin.id)
                repository.savePersonalization(current.state.user?.id, updated)
                val personality = updated.resolvedPersonality(current.petState.species)
                return@update current.copy(
                    personalization = updated,
                    petEvent = PetAnimationEvent.HAPPY_TAP,
                    petEventTrigger = current.petEventTrigger + 1,
                    petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.SKIN_EQUIP)
                )
            }
            if (current.petState.level < skin.minLevel || city.ecoSeeds < skin.seedCost) {
                return@update current
            }
            val updated = current.personalization.copy(
                unlockedSkinIds = unlocked + skin.id,
                activeSkinId = skin.id,
                city = city.copy(ecoSeeds = city.ecoSeeds - skin.seedCost)
            )
            repository.savePersonalization(current.state.user?.id, updated)
            val personality = updated.resolvedPersonality(current.petState.species)
            current.copy(
                personalization = updated,
                petEvent = PetAnimationEvent.LEVEL_UP,
                petEventTrigger = current.petEventTrigger + 1,
                petEventNewLevel = current.petState.level,
                petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.SKIN_EQUIP)
            )
        }
    }

    fun updatePersonalization(
        petNickname: String,
        personality: PetPersonality,
        ecoPersona: EcoPersona,
        favoritePlastic: PlasticType,
        habitatTheme: HabitatTheme
    ) {
        _uiState.update { current ->
            val updated = current.personalization.copy(
                petNickname = petNickname.trim(),
                personalityId = personality.id,
                ecoPersonaId = ecoPersona.id,
                favoritePlasticCode = favoritePlastic.code,
                habitatThemeId = habitatTheme.id
            )
            repository.savePersonalization(current.state.user?.id, updated)
            current.copy(
                personalization = updated,
                petEvent = PetAnimationEvent.HAPPY_TAP,
                petEventTrigger = current.petEventTrigger + 1,
                petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.PAT)
            )
        }
    }

    fun grantDemoEcoResources() {
        _uiState.update { current ->
            val city = current.personalization.safeCity()
            val updated = current.personalization.copy(
                city = city.copy(
                    ecoSeeds = city.ecoSeeds + 60,
                    waterDrops = city.waterDrops + 3
                )
            )
            repository.savePersonalization(current.state.user?.id, updated)
            current.copy(
                personalization = updated,
                petEvent = PetAnimationEvent.POINTS_ADDED,
                petEventTrigger = current.petEventTrigger + 1,
                petEventPointsDelta = 60
            )
        }
    }

    fun equipTitle(titleId: String?) {
        _uiState.update { current ->
            val nextId = if (current.personalization.equippedTitleId == titleId) null else titleId
            val updated = current.personalization.copy(equippedTitleId = nextId)
            repository.savePersonalization(current.state.user?.id, updated)
            current.copy(personalization = updated)
        }
    }

    fun saveCampaignBitmap(bitmap: Bitmap): String? = repository.saveCampaignBitmap(bitmap)

    fun saveCampaignUri(uri: Uri): String? = repository.copyImageUriToCampaignStorage(uri)

    fun openCampaign(
        title: String,
        description: String,
        targetKg: Double
    ): EcoCampaign {
        val current = _uiState.value
        val user = current.state.user
        val userId = user?.id ?: "me"
        val userName = user?.name ?: "Sinh viên FLYSTIC"
        val mascotName = current.personalization.petNickname.ifBlank {
            t(current.petState.species.nameKey)
        }
        val code = "ECO-${UUID.randomUUID().toString().take(4).uppercase()}"
        val campaign = EcoCampaign(
            id = "cmp-${UUID.randomUUID().toString().take(8)}",
            title = title.trim().ifBlank { "Chiến Dịch Tái Chế Tuần" },
            description = description.trim().ifBlank { "Cùng gom rác nhựa sạch và mời mascot về Thành Phố Xanh!" },
            inviteCode = code,
            inviteLink = "https://verifour.app/campaign/$code",
            targetKg = targetKg.coerceAtLeast(1.0),
            creatorId = userId,
            habitatTheme = current.personalization.resolvedHabitat(),
            participants = listOf(
                CampaignParticipant(
                    userId = userId,
                    name = userName,
                    mascotName = mascotName,
                    species = current.petState.species,
                    skin = current.personalization.resolvedSkin(),
                    kgRecycled = round2(max(0.5, current.state.totalKg)),
                    points = max(15, current.state.points),
                    equippedTitleId = current.personalization.equippedTitleId
                )
            ),
            photos = emptyList()
        )
        _uiState.update { state ->
            val nextList = listOf(campaign) + state.campaigns
            repository.saveCampaigns(userId, nextList)
            state.copy(campaigns = nextList)
        }
        return campaign
    }

    fun joinCampaignByCodeOrLink(rawCodeOrLink: String): Boolean {
        val code = rawCodeOrLink.trim().substringAfterLast("/").uppercase()
        if (code.isBlank()) return false

        val current = _uiState.value
        val user = current.state.user ?: return false
        val mascotName = current.personalization.petNickname.ifBlank {
            t(current.petState.species.nameKey)
        }

        val existing = current.campaigns.firstOrNull {
            it.inviteCode.equals(code, ignoreCase = true)
        }
        if (existing != null) {
            val alreadyIn = existing.safeParticipants().any { it.userId == user.id }
            if (!alreadyIn) {
                val updated = existing.copy(
                    participants = existing.safeParticipants() + CampaignParticipant(
                        userId = user.id,
                        name = user.name,
                        mascotName = mascotName,
                        species = current.petState.species,
                        skin = current.personalization.resolvedSkin(),
                        kgRecycled = round2(max(0.4, current.state.totalKg)),
                        points = max(10, current.state.points),
                        equippedTitleId = current.personalization.equippedTitleId
                    )
                )
                updateCampaignItem(updated)
            }
            return true
        }

        val preset = repository.buildPresetJoinableCampaign(code) ?: return false
        val withMe = preset.copy(
            participants = preset.safeParticipants() + CampaignParticipant(
                userId = user.id,
                name = user.name,
                mascotName = mascotName,
                species = current.petState.species,
                skin = current.personalization.resolvedSkin(),
                kgRecycled = round2(max(0.5, current.state.totalKg)),
                points = max(15, current.state.points),
                equippedTitleId = current.personalization.equippedTitleId
            )
        )
        val cityMascotsToAdd = preset.safeParticipants().map { p ->
            InvitedCityMascot(
                id = "city-${preset.id}-${p.userId}",
                ownerName = p.name,
                mascotName = p.mascotName,
                species = p.species,
                skin = p.skin,
                plasticMascot = p.plasticMascot,
                campaignId = preset.id
            )
        }
        _uiState.update { state ->
            val nextCampaigns = listOf(withMe) + state.campaigns
            val mergedCityMascots = (state.personalization.safeInvitedMascots() + cityMascotsToAdd)
                .distinctBy { it.id }
            val nextPersonalization = state.personalization.copy(
                invitedMascotsInCity = mergedCityMascots
            )
            repository.saveCampaigns(user.id, nextCampaigns)
            repository.savePersonalization(user.id, nextPersonalization)
            state.copy(
                campaigns = nextCampaigns,
                personalization = nextPersonalization,
                petEvent = PetAnimationEvent.HAPPY_TAP,
                petEventTrigger = state.petEventTrigger + 1
            )
        }
        return true
    }

    fun inviteMascotToCampaign(
        campaignId: String,
        friendName: String,
        mascotName: String,
        species: PetSpecies,
        skin: PetSkin,
        plasticMascot: PlasticType?
    ) {
        _uiState.update { state ->
            val userId = state.state.user?.id
            val campaign = state.campaigns.firstOrNull { it.id == campaignId } ?: return@update state
            val cleanFriend = friendName.trim().ifBlank { "Bạn Eco" }
            val cleanMascot = mascotName.trim().ifBlank {
                plasticMascot?.let { "${it.code}-Bot" } ?: t(species.nameKey)
            }
            val invitedUserId = "friend-${UUID.randomUUID().toString().take(6)}"
            val newParticipant = CampaignParticipant(
                userId = invitedUserId,
                name = cleanFriend,
                mascotName = cleanMascot,
                species = species,
                skin = skin,
                plasticMascot = plasticMascot,
                kgRecycled = 0.6,
                points = 15,
                equippedTitleId = "eco_connector"
            )
            val updatedCampaign = campaign.copy(
                participants = campaign.safeParticipants() + newParticipant
            )
            val nextCampaigns = state.campaigns.map { if (it.id == campaignId) updatedCampaign else it }

            val cityMascot = InvitedCityMascot(
                id = "city-$invitedUserId",
                ownerName = cleanFriend,
                mascotName = cleanMascot,
                species = species,
                skin = skin,
                plasticMascot = plasticMascot,
                campaignId = campaignId
            )
            val city = state.personalization.safeCity()
            val updatedPersonalization = state.personalization.copy(
                invitedMascotsInCity = state.personalization.safeInvitedMascots() + cityMascot,
                city = city.copy(ecoSeeds = city.ecoSeeds + 10)
            )
            repository.saveCampaigns(userId, nextCampaigns)
            repository.savePersonalization(userId, updatedPersonalization)
            state.copy(
                campaigns = nextCampaigns,
                personalization = updatedPersonalization,
                petEvent = PetAnimationEvent.HAPPY_TAP,
                petEventTrigger = state.petEventTrigger + 1
            )
        }
    }

    fun addCampaignPhoto(
        campaignId: String,
        caption: String,
        plasticType: PlasticType,
        kg: Double,
        localImagePath: String?,
        samplePhotoKey: String? = null
    ) {
        _uiState.update { state ->
            val user = state.state.user ?: return@update state
            val campaign = state.campaigns.firstOrNull { it.id == campaignId } ?: return@update state
            val safeKg = round2(kg.coerceIn(0.1, 25.0))
            val pts = max(5, (safeKg * plasticType.basePoints * 1.5).toInt())

            val photo = CampaignPhoto(
                id = "ph-${UUID.randomUUID().toString().take(8)}",
                authorUserId = user.id,
                authorName = user.name,
                caption = caption.trim().ifBlank { "Thu gom ${safeKg}kg nhựa ${plasticType.code} sạch!" },
                plasticType = plasticType,
                kg = safeKg,
                localImagePath = localImagePath,
                samplePhotoKey = samplePhotoKey
            )

            val updatedParticipants = if (campaign.safeParticipants().any { it.userId == user.id }) {
                campaign.safeParticipants().map { p ->
                    if (p.userId == user.id) {
                        p.copy(
                            kgRecycled = round2(p.kgRecycled + safeKg),
                            points = p.points + pts
                        )
                    } else p
                }
            } else {
                campaign.safeParticipants() + CampaignParticipant(
                    userId = user.id,
                    name = user.name,
                    mascotName = state.personalization.petNickname.ifBlank { t(state.petState.species.nameKey) },
                    species = state.petState.species,
                    skin = state.personalization.resolvedSkin(),
                    kgRecycled = safeKg,
                    points = pts,
                    equippedTitleId = state.personalization.equippedTitleId
                )
            }

            val updatedCampaign = campaign.copy(
                participants = updatedParticipants,
                photos = listOf(photo) + campaign.safePhotos()
            )
            val nextCampaigns = state.campaigns.map { if (it.id == campaignId) updatedCampaign else it }

            val city = state.personalization.safeCity()
            val updatedPersonalization = state.personalization.copy(
                weeklyBattleBonusPts = state.personalization.weeklyBattleBonusPts + pts,
                weeklyBattleKgBonus = round2(state.personalization.weeklyBattleKgBonus + safeKg),
                city = city.copy(
                    ecoSeeds = city.ecoSeeds + 15,
                    waterDrops = city.waterDrops + 1
                )
            )
            repository.saveCampaigns(user.id, nextCampaigns)
            repository.savePersonalization(user.id, updatedPersonalization)
            state.copy(
                campaigns = nextCampaigns,
                personalization = updatedPersonalization,
                petEvent = PetAnimationEvent.POINTS_ADDED,
                petEventTrigger = state.petEventTrigger + 1,
                petEventPointsDelta = pts
            )
        }
    }

    fun concludeCampaign(campaignId: String) {
        _uiState.update { state ->
            val userId = state.state.user?.id
            val nextCampaigns = state.campaigns.map { c ->
                if (c.id == campaignId) c.copy(isEnded = true, endedAt = System.currentTimeMillis()) else c
            }
            repository.saveCampaigns(userId, nextCampaigns)
            state.copy(campaigns = nextCampaigns)
        }
    }

    fun reopenCampaign(campaignId: String) {
        _uiState.update { state ->
            val userId = state.state.user?.id
            val nextCampaigns = state.campaigns.map { c ->
                if (c.id == campaignId) c.copy(isEnded = false, endedAt = null) else c
            }
            repository.saveCampaigns(userId, nextCampaigns)
            state.copy(campaigns = nextCampaigns)
        }
    }

    fun populateDemoCampaign(campaignId: String? = null, autoEnd: Boolean = true): String {
        val current = _uiState.value
        val user = current.state.user
        val userId = user?.id ?: "me"
        val targetCampaign = if (campaignId != null) {
            current.campaigns.firstOrNull { it.id == campaignId }
        } else {
            current.campaigns.firstOrNull()
        } ?: openCampaign(
            title = "Chiến Dịch Tuần Lễ Xanh Bách Khoa",
            description = "Thi đua phân loại nhựa PET/PP & mời mascot chạy nhảy trong Thành Phố Xanh!",
            targetKg = 10.0
        )

        val demoFriends = listOf(
            CampaignParticipant(
                userId = "demo-linh",
                name = "Nguyễn Thảo Linh",
                mascotName = "PET-Chan",
                species = PetSpecies.SPROUTLING,
                skin = PetSkin.SAKURA,
                plasticMascot = PlasticType.PET,
                kgRecycled = 2.8,
                points = 72,
                equippedTitleId = "clean_saint"
            ),
            CampaignParticipant(
                userId = "demo-minh",
                name = "Trần Quang Minh",
                mascotName = "PP-Cáo",
                species = PetSpecies.FOX,
                skin = PetSkin.SOLAR_TECH,
                plasticMascot = PlasticType.PP,
                kgRecycled = 2.1,
                points = 54,
                equippedTitleId = "pet_king"
            ),
            CampaignParticipant(
                userId = "demo-an",
                name = "Lê Hoàng An",
                mascotName = "HDPE-Rùa",
                species = PetSpecies.TURTLE,
                skin = PetSkin.GOLDEN_LEAF,
                plasticMascot = PlasticType.HDPE,
                kgRecycled = 1.6,
                points = 40,
                equippedTitleId = "sorting_master"
            )
        )

        val demoPhotos = listOf(
            CampaignPhoto(
                id = "demo-ph-1",
                authorUserId = "demo-linh",
                authorName = "Nguyễn Thảo Linh",
                caption = "Gom 22 chai PET sạch tại sảnh B1!",
                plasticType = PlasticType.PET,
                kg = 1.5,
                samplePhotoKey = "pet"
            ),
            CampaignPhoto(
                id = "demo-ph-2",
                authorUserId = "demo-minh",
                authorName = "Trần Quang Minh",
                caption = "Rửa sạch ly nhựa PP sau giờ học",
                plasticType = PlasticType.PP,
                kg = 1.1,
                samplePhotoKey = "pp"
            ),
            CampaignPhoto(
                id = "demo-ph-3",
                authorUserId = "demo-an",
                authorName = "Lê Hoàng An",
                caption = "Chai nhựa HDPE phân loại gọn gàng",
                plasticType = PlasticType.HDPE,
                kg = 0.9,
                samplePhotoKey = "hdpe"
            )
        )

        val newCityMascots = demoFriends.map { p ->
            InvitedCityMascot(
                id = "city-${targetCampaign.id}-${p.userId}",
                ownerName = p.name,
                mascotName = p.mascotName,
                species = p.species,
                skin = p.skin,
                plasticMascot = p.plasticMascot,
                campaignId = targetCampaign.id
            )
        }

        _uiState.update { state ->
            val freshCampaign = state.campaigns.firstOrNull { it.id == targetCampaign.id } ?: targetCampaign
            val mergedParticipants = (freshCampaign.safeParticipants() + demoFriends).distinctBy { it.userId }
            val mergedPhotos = (freshCampaign.safePhotos() + demoPhotos).distinctBy { it.id }
            val updatedCampaign = freshCampaign.copy(
                participants = mergedParticipants,
                photos = mergedPhotos,
                isEnded = autoEnd,
                endedAt = if (autoEnd) System.currentTimeMillis() else null
            )
            val nextCampaigns = state.campaigns.map {
                if (it.id == updatedCampaign.id) updatedCampaign else it
            }.let { list ->
                if (list.any { it.id == updatedCampaign.id }) list else listOf(updatedCampaign) + list
            }
            val mergedCityMascots = (state.personalization.safeInvitedMascots() + newCityMascots)
                .distinctBy { it.id }
            val updatedPersonalization = state.personalization.copy(
                invitedMascotsInCity = mergedCityMascots
            )
            repository.saveCampaigns(userId, nextCampaigns)
            repository.savePersonalization(userId, updatedPersonalization)
            state.copy(
                campaigns = nextCampaigns,
                personalization = updatedPersonalization,
                petEvent = PetAnimationEvent.LEVEL_UP,
                petEventTrigger = state.petEventTrigger + 1,
                petEventNewLevel = state.petState.level
            )
        }
        return targetCampaign.id
    }

    private fun updateCampaignItem(updated: EcoCampaign) {
        _uiState.update { state ->
            val userId = state.state.user?.id
            val nextCampaigns = state.campaigns.map { if (it.id == updated.id) updated else it }
            repository.saveCampaigns(userId, nextCampaigns)
            state.copy(campaigns = nextCampaigns)
        }
    }

    fun recordBattleResult(
        won: Boolean,
        rpDelta: Int,
        seedsEarned: Int,
        xpEarned: Int,
        weeklyPtsEarned: Int = if (won) 28 else 10,
        weeklyKgEarned: Double = if (won) 0.5 else 0.2
    ) {
        _uiState.update { current ->
            val city = current.personalization.safeCity()
            val nextRp = max(800, current.personalization.arenaRp + rpDelta)
            val nextStreak = if (won) current.personalization.arenaStreak + 1 else 0
            val updatedPersonalization = current.personalization.copy(
                arenaRp = nextRp,
                arenaWins = current.personalization.arenaWins + if (won) 1 else 0,
                arenaLosses = current.personalization.arenaLosses + if (won) 0 else 1,
                arenaStreak = nextStreak,
                weeklyBattleBonusPts = current.personalization.weeklyBattleBonusPts + weeklyPtsEarned,
                weeklyBattleKgBonus = round2(current.personalization.weeklyBattleKgBonus + weeklyKgEarned),
                petBond = min(100, current.personalization.petBond + if (won) 6 else 2),
                city = city.copy(
                    ecoSeeds = city.ecoSeeds + seedsEarned,
                    waterDrops = city.waterDrops + if (won) 1 else 0
                )
            )
            val updatedPet = current.petState.copy(
                bonusXp = current.petState.bonusXp + xpEarned
            )
            repository.savePersonalization(current.state.user?.id, updatedPersonalization)
            repository.savePetState(current.state.user?.id, updatedPet)
            val personality = updatedPersonalization.resolvedPersonality(updatedPet.species)
            current.copy(
                petState = updatedPet,
                personalization = updatedPersonalization,
                petEvent = if (won) PetAnimationEvent.POINTS_ADDED else PetAnimationEvent.IDLE,
                petEventTrigger = if (won) current.petEventTrigger + 1 else current.petEventTrigger,
                petEventPointsDelta = xpEarned,
                petReactionKey = PetProgression.reactionTranslationKey(
                    personality,
                    if (won) PetReactionContext.BATTLE_WIN else PetReactionContext.BATTLE_LOSE
                )
            )
        }
    }

    private fun applyPointsUpdate(next: FlysticState, fallbackDelta: Int = 0) {
        _uiState.update { current ->
            val prevCelebrated = current.petState.lastCelebratedPoints
            val delta = (next.points - prevCelebrated).coerceAtLeast(0).let {
                if (it == 0 && fallbackDelta > 0) fallbackDelta else it
            }
            if (delta > 0) {
                val updatedPet = current.petState.copy(
                    lastCelebratedPoints = maxOf(next.points, prevCelebrated)
                )
                val city = current.personalization.safeCity()
                val updatedPersonalization = current.personalization.copy(
                    petBond = min(100, current.personalization.petBond + 5),
                    city = city.copy(
                        ecoSeeds = city.ecoSeeds + max(10, delta),
                        waterDrops = city.waterDrops + max(1, delta / 15)
                    )
                )
                repository.savePetState(next.user?.id, updatedPet)
                repository.savePersonalization(next.user?.id, updatedPersonalization)
                val personality = updatedPersonalization.resolvedPersonality(updatedPet.species)
                current.copy(
                    state = next,
                    petState = updatedPet,
                    personalization = updatedPersonalization,
                    petEvent = PetAnimationEvent.POINTS_ADDED,
                    petEventTrigger = current.petEventTrigger + 1,
                    petEventPointsDelta = delta,
                    petReactionKey = PetProgression.reactionTranslationKey(personality, PetReactionContext.SCAN)
                )
            } else {
                current.copy(state = next)
            }
        }
    }
}
