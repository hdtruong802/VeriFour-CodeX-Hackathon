package com.verifour.app.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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

class FlysticApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.yoha.io.vn"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

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
}
