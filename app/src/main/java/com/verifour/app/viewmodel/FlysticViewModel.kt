package com.verifour.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verifour.app.data.FlysticRepository
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.Cleanliness
import com.verifour.app.model.CollectionRun
import com.verifour.app.model.FlysticState
import com.verifour.app.model.LeaderboardRow
import com.verifour.app.model.PlasticType
import com.verifour.app.model.PointDestination
import com.verifour.app.model.Role
import com.verifour.app.model.StudentOrganization
import com.verifour.app.model.Transaction
import com.verifour.app.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlysticUiState(
    val state: FlysticState = FlysticState(),
    val isReady: Boolean = false,
    val isRemote: Boolean = true
)

class FlysticViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FlysticRepository(application)

    private val _uiState = MutableStateFlow(
        FlysticUiState(
            state = FlysticState(locale = repository.getPersistedLocale()),
            isReady = false,
            isRemote = repository.isRemoteMode()
        )
    )
    val uiState: StateFlow<FlysticUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repository.loadInitialState()
            _uiState.update {
                it.copy(
                    state = loaded,
                    isReady = true,
                    isRemote = repository.isRemoteMode()
                )
            }
        }
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

    fun setRemoteMode(remote: Boolean) {
        if (_uiState.value.isRemote == remote) return
        repository.setRemoteMode(remote)
        viewModelScope.launch {
            val loaded = repository.loadInitialState()
            _uiState.update {
                it.copy(
                    state = loaded,
                    isRemote = remote,
                    isReady = true
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
        _uiState.update { it.copy(state = next) }
        return next.user!!
    }

    fun signOut() {
        val next = repository.signOut(_uiState.value.state)
        _uiState.update { it.copy(state = next) }
    }

    fun refresh() {
        viewModelScope.launch {
            if (_uiState.value.isRemote && repository.getSessionToken() != null) {
                runCatching { repository.refreshRemoteState(locale = _uiState.value.state.locale) }
                    .onSuccess { next -> _uiState.update { it.copy(state = next) } }
            } else {
                val stations = repository.fetchStations()
                _uiState.update { it.copy(state = it.state.copy(stations = stations)) }
            }
        }
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
        _uiState.update { it.copy(state = next) }
        return confirmed
    }

    suspend fun cancelTransaction(id: String) {
        val next = repository.cancelTransaction(_uiState.value.state, id)
        _uiState.update { it.copy(state = next) }
    }

    suspend fun emptyCollectionPoint(collectionPoint: String): CollectionRun? {
        val (run, next) = repository.emptyCollectionPoint(_uiState.value.state, collectionPoint)
        _uiState.update { it.copy(state = next) }
        return run
    }

    suspend fun getLeaderboard(period: String): List<LeaderboardRow> =
        repository.fetchLeaderboard(period, _uiState.value.state)
}
