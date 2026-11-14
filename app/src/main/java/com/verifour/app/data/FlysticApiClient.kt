package com.verifour.app.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.verifour.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class FlysticApiException(
    override val message: String,
    val statusCode: Int
) : Exception(message)

data class ApiErrorResponse(
    val error: String?
)

data class ApiUserDto(
    val id: String,
    val email: String,
    val role: String,
    val name: String,
    val studentId: String?,
    val faculty: String?,
    val stationId: String?,
    val points: Int,
    val rewardPoints: Int,
    val fundPoints: Int,
    val totalKg: Double
)

data class ApiTxItemDto(
    val plasticType: String,
    val quantity: Double,
    val cleanliness: String,
    val points: Int
)

data class ApiTransactionDto(
    val id: String,
    val token: String?,
    val userId: String?,
    val userName: String?,
    val userFaculty: String?,
    val adminId: String,
    val stationId: String,
    val collectionPoint: String,
    val items: List<ApiTxItemDto>,
    val totalQuantity: Double,
    val weightKg: Double,
    val points: Int,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long,
    val confirmedAt: Long?
)

data class ApiStationDto(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val capacityKg: Double,
    val filledKg: Double,
    val fillPercent: Int
)

data class ApiCollectionRunDto(
    val id: String,
    val stationId: String,
    val collectionPoint: String,
    val weightKg: Double,
    val adminId: String,
    val createdAt: Long
)

data class ApiDemoCampaignMetricDto(
    val label: String,
    val kg: Double
)

data class ApiDemoCampaignDto(
    val registeredStudents: Int,
    val participantCount: Int,
    val transactionCount: Int,
    val totalKg: Double,
    val totalPoints: Int,
    val fundraisingPoints: Int,
    val fundraisingPercent: Int,
    val byType: List<ApiDemoCampaignMetricDto>,
    val byQuality: List<ApiDemoCampaignMetricDto>
)

data class ApiLeaderboardRowDto(
    val userId: String,
    val name: String,
    val faculty: String,
    val points: Int,
    val kg: Double
)

data class ApiSnapshotDto(
    val user: ApiUserDto,
    val stations: List<ApiStationDto>,
    val transactions: List<ApiTransactionDto>,
    val collectionRuns: List<ApiCollectionRunDto>,
    val demoCampaign: ApiDemoCampaignDto?
)

data class AuthTokenResponseDto(
    val token: String,
    val user: ApiUserDto
)

data class LeaderboardResponseDto(
    val period: String,
    val rows: List<ApiLeaderboardRowDto>
)

data class ConfirmTransactionResponseDto(
    val id: String,
    val points: Int,
    val weightKg: Double,
    val destination: String,
    val confirmedAt: Long
)

data class TransferResponseDto(
    val stationId: String,
    val weightKg: Double,
    val createdAt: Long
)

data class CreateItemInputDto(
    val plasticType: String,
    val quantity: Double,
    val cleanliness: String
)

data class CancelResponseDto(
    val id: String,
    @SerializedName("cancelledAt") val cancelledAt: Long
)

data class ApiStationOpsDto(
    val id: String,
    val name: String,
    val address: String,
    val capacityKg: Double,
    val filledKg: Double,
    val fillPercent: Int,
    val isActive: Boolean = true,
    val pendingTokens: Int = 0,
    val confirmedToday: Int = 0
)

data class ApiAdminOpsOverviewDto(
    val stations: List<ApiStationOpsDto>
)

data class ApiManagedAccountDto(
    val id: String,
    val email: String,
    val role: String,
    val name: String,
    val studentId: String?,
    val faculty: String?,
    val stationId: String?,
    val collectionPoint: String?,
    val points: Int,
    val totalKg: Double,
    val status: String?,
    val tokenVersion: Int?,
    val lastLoginAt: Long?,
    val createdAt: Long?
)

data class ApiAdminInviteDto(
    val id: String,
    val code: String,
    val label: String,
    val stationId: String,
    val stationName: String?,
    val maxUses: Int,
    val usedCount: Int,
    val expiresAt: Long,
    val isRevoked: Boolean,
    val createdAt: Long
)

data class ApiAdminTokensResponseDto(
    val qrTransactions: List<ApiTransactionDto>,
    val invites: List<ApiAdminInviteDto>
)

data class ApiAccessLogDto(
    val id: String,
    val userId: String?,
    val email: String,
    val role: String?,
    val eventType: String,
    val outcome: String,
    val ipAddress: String?,
    val deviceInfo: String?,
    val detail: String?,
    val createdAt: Long
)

data class ApiAuthLockoutDto(
    val throttleKey: String,
    val emailHint: String,
    val failureCount: Int,
    val lockedUntil: Long,
    val updatedAt: Long
)

data class ApiAccessLogsResponseDto(
    val logs: List<ApiAccessLogDto>,
    val lockouts: List<ApiAuthLockoutDto>
)

data class ApiSimpleStatusDto(
    val id: String? = null,
    val status: String? = null,
    val tokenVersion: Int? = null,
    val isActive: Boolean? = null,
    val expiredCount: Int? = null,
    val cleared: Boolean? = null
)

class FlysticApiClient(
    initialBaseUrl: String = DEFAULT_BASE_URL
) {
    companion object {
        val DEFAULT_BASE_URL: String = BuildConfig.FLYSTIC_API_BASE_URL.ifBlank { "https://api.yoha.io.vn" }
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isBlank()) return DEFAULT_BASE_URL
            return if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "http://$trimmed"
            }
        }
    }

    @Volatile
    var baseUrl: String = normalizeBaseUrl(initialBaseUrl)
        private set

    fun updateBaseUrl(newUrl: String): String {
        val normalized = normalizeBaseUrl(newUrl)
        baseUrl = normalized
        return normalized
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    suspend fun checkHealth(targetUrl: String = baseUrl): Result<String> = withContext(Dispatchers.IO) {
        val normalized = normalizeBaseUrl(targetUrl)
        val req = Request.Builder()
            .url("$normalized/api/v1/health")
            .get()
            .build()
        runCatching {
            pingClient.newCall(req).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw FlysticApiException("HTTP ${response.code}", response.code)
                }
                raw.ifBlank { "OK" }
            }
        }
    }

    private suspend inline fun <reified T> request(
        path: String,
        method: String = "GET",
        token: String? = null,
        body: Any? = null
    ): T = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/v1$path")

        if (!token.isNullOrBlank()) {
            builder.addHeader("authorization", "Bearer $token")
        }

        when (method) {
            "POST" -> {
                val jsonBody = gson.toJson(body ?: emptyMap<String, Any>())
                builder.post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            }
            else -> builder.get()
        }

        try {
            client.newCall(builder.build()).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val parsedError = runCatching {
                        gson.fromJson(raw, ApiErrorResponse::class.java)?.error
                    }.getOrNull()
                    throw FlysticApiException(
                        parsedError ?: "Server error (${response.code})",
                        response.code
                    )
                }
                gson.fromJson(raw, T::class.java)
                    ?: throw FlysticApiException("Empty response from server", response.code)
            }
        } catch (e: FlysticApiException) {
            throw e
        } catch (e: IOException) {
            throw FlysticApiException(e.message ?: "Network error", 0)
        }
    }

    suspend fun getStations(): List<ApiStationDto> {
        val arr = request<Array<ApiStationDto>>("/stations")
        return arr.toList()
    }

    suspend fun getLeaderboard(period: String): LeaderboardResponseDto =
        request("/leaderboard?period=$period")

    suspend fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        studentId: String?,
        faculty: String?,
        stationId: String?,
        inviteCode: String?
    ): AuthTokenResponseDto {
        val payload = buildMap<String, Any> {
            put("email", email)
            put("password", password)
            put("name", name)
            put("role", role)
            if (!studentId.isNullOrBlank()) put("studentId", studentId)
            if (!faculty.isNullOrBlank()) put("faculty", faculty)
            if (role == "admin") {
                if (!stationId.isNullOrBlank()) put("stationId", stationId)
                if (!inviteCode.isNullOrBlank()) put("inviteCode", inviteCode)
            }
        }
        return request("/auth/register", method = "POST", body = payload)
    }

    suspend fun login(email: String, password: String): AuthTokenResponseDto =
        request("/auth/login", method = "POST", body = mapOf("email" to email, "password" to password))

    suspend fun getSnapshot(token: String): ApiSnapshotDto =
        request("/snapshot", token = token)

    suspend fun createAdminTransaction(
        token: String,
        items: List<CreateItemInputDto>
    ): ApiTransactionDto =
        request("/admin/transactions", method = "POST", token = token, body = mapOf("items" to items))

    suspend fun cancelAdminTransaction(token: String, id: String): CancelResponseDto =
        request("/admin/transactions/$id/cancel", method = "POST", token = token)

    suspend fun getTransaction(token: String, qrToken: String): ApiTransactionDto =
        request("/transactions/$qrToken", token = token)

    suspend fun confirmTransaction(
        token: String,
        qrToken: String,
        destination: String
    ): ConfirmTransactionResponseDto =
        request(
            "/transactions/$qrToken/confirm",
            method = "POST",
            token = token,
            body = mapOf("destination" to destination)
        )

    suspend fun recordStationTransfer(token: String, stationId: String): TransferResponseDto =
        request(
            "/admin/stations/$stationId/transfers",
            method = "POST",
            token = token,
            body = emptyMap<String, Any>()
        )

    // Admin Ops endpoints
    suspend fun getAdminOpsOverview(token: String): ApiAdminOpsOverviewDto =
        request("/admin/ops/overview", token = token)

    suspend fun getAdminUsers(token: String): List<ApiManagedAccountDto> {
        val arr = request<Array<ApiManagedAccountDto>>("/admin/users", token = token)
        return arr.toList()
    }

    suspend fun createAdminUser(
        token: String,
        name: String,
        email: String,
        password: String,
        role: String,
        studentId: String?,
        faculty: String?,
        stationId: String?
    ): ApiManagedAccountDto {
        val payload = buildMap<String, Any> {
            put("name", name)
            put("email", email)
            put("password", password)
            put("role", role)
            if (!studentId.isNullOrBlank()) put("studentId", studentId)
            if (!faculty.isNullOrBlank()) put("faculty", faculty)
            if (!stationId.isNullOrBlank()) put("stationId", stationId)
        }
        return request("/admin/users", method = "POST", token = token, body = payload)
    }

    suspend fun updateAdminUserStatus(
        token: String,
        userId: String,
        status: String
    ): ApiSimpleStatusDto =
        request(
            "/admin/users/$userId/status",
            method = "POST",
            token = token,
            body = mapOf("status" to status)
        )

    suspend fun updateAdminUserRole(
        token: String,
        userId: String,
        role: String,
        stationId: String?
    ): ApiSimpleStatusDto {
        val payload = buildMap<String, Any> {
            put("role", role)
            if (!stationId.isNullOrBlank()) put("stationId", stationId)
        }
        return request("/admin/users/$userId/role", method = "POST", token = token, body = payload)
    }

    suspend fun revokeAdminUserTokens(token: String, userId: String): ApiSimpleStatusDto =
        request("/admin/users/$userId/revoke-tokens", method = "POST", token = token)

    suspend fun getAdminTokens(token: String): ApiAdminTokensResponseDto =
        request("/admin/tokens", token = token)

    suspend fun revokeAdminQrToken(token: String, txId: String): CancelResponseDto =
        request("/admin/tokens/$txId/revoke", method = "POST", token = token)

    suspend fun expireStaleQrTokens(token: String): ApiSimpleStatusDto =
        request("/admin/tokens/expire-stale", method = "POST", token = token)

    suspend fun createAdminInvite(
        token: String,
        label: String,
        stationId: String,
        maxUses: Int,
        ttlHours: Int
    ): ApiAdminInviteDto =
        request(
            "/admin/invites",
            method = "POST",
            token = token,
            body = mapOf(
                "label" to label,
                "stationId" to stationId,
                "maxUses" to maxUses,
                "ttlHours" to ttlHours
            )
        )

    suspend fun revokeAdminInvite(token: String, inviteId: String): ApiSimpleStatusDto =
        request("/admin/invites/$inviteId/revoke", method = "POST", token = token)

    suspend fun getAdminAccessLogs(token: String): ApiAccessLogsResponseDto =
        request("/admin/access-logs", token = token)

    suspend fun clearAuthThrottle(token: String, throttleKey: String?): ApiSimpleStatusDto =
        request(
            "/admin/throttles/clear",
            method = "POST",
            token = token,
            body = if (throttleKey.isNullOrBlank()) emptyMap() else mapOf("throttleKey" to throttleKey)
        )

    suspend fun toggleAdminStation(token: String, stationId: String): ApiSimpleStatusDto =
        request("/admin/stations/$stationId/toggle", method = "POST", token = token)
}
