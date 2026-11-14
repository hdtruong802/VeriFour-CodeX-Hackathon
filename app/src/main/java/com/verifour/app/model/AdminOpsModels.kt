package com.verifour.app.model

enum class AccountStatus(val apiValue: String) {
    ACTIVE("active"),
    LOCKED("locked"),
    SUSPENDED("suspended");

    companion object {
        fun fromApi(value: String?): AccountStatus =
            entries.firstOrNull { it.apiValue == value } ?: ACTIVE
    }
}

enum class AccessOutcome(val apiValue: String) {
    SUCCESS("success"),
    FAILED("failed"),
    BLOCKED("blocked");

    companion object {
        fun fromApi(value: String?): AccessOutcome =
            entries.firstOrNull { it.apiValue == value } ?: SUCCESS
    }
}

data class ManagedAccount(
    val id: String,
    val email: String,
    val name: String,
    val role: Role,
    val studentId: String? = null,
    val faculty: String? = null,
    val stationId: String? = null,
    val collectionPoint: String? = null,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val points: Int = 0,
    val totalKg: Double = 0.0,
    val tokenVersion: Int = 1,
    val lastLoginAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AccessLogEntry(
    val id: String,
    val userId: String? = null,
    val email: String,
    val role: String? = null,
    val eventType: String,
    val outcome: AccessOutcome,
    val ipAddress: String? = null,
    val deviceInfo: String? = null,
    val detail: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AuthLockoutEntry(
    val throttleKey: String,
    val emailHint: String,
    val failureCount: Int,
    val lockedUntil: Long,
    val updatedAt: Long
)

data class AdminInviteToken(
    val id: String,
    val code: String,
    val label: String,
    val stationId: String,
    val stationName: String,
    val maxUses: Int,
    val usedCount: Int,
    val expiresAt: Long,
    val isRevoked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class StationOpsSummary(
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

data class TokenMonitorSummary(
    val activeQr: Int = 0,
    val confirmedQr: Int = 0,
    val expiredQr: Int = 0,
    val cancelledQr: Int = 0,
    val activeInvites: Int = 0
)

data class AccountMonitorSummary(
    val total: Int = 0,
    val students: Int = 0,
    val admins: Int = 0,
    val active: Int = 0,
    val locked: Int = 0,
    val suspended: Int = 0
)

data class AdminOpsState(
    val stationsOps: List<StationOpsSummary> = emptyList(),
    val accounts: List<ManagedAccount> = emptyList(),
    val qrTokens: List<Transaction> = emptyList(),
    val inviteTokens: List<AdminInviteToken> = emptyList(),
    val accessLogs: List<AccessLogEntry> = emptyList(),
    val lockouts: List<AuthLockoutEntry> = emptyList(),
    val lastUpdatedAt: Long = 0L
)

fun isInviteTokenActive(
    invite: AdminInviteToken,
    now: Long = System.currentTimeMillis()
): Boolean = !invite.isRevoked && invite.expiresAt > now && invite.usedCount < invite.maxUses

fun filterManagedAccounts(
    accounts: List<ManagedAccount>,
    query: String,
    roleFilter: Role? = null,
    statusFilter: AccountStatus? = null
): List<ManagedAccount> {
    val q = query.trim().lowercase()
    return accounts.filter { acc ->
        val matchesRole = roleFilter == null || acc.role == roleFilter
        val matchesStatus = statusFilter == null || acc.status == statusFilter
        val matchesQuery = q.isEmpty() ||
            acc.name.lowercase().contains(q) ||
            acc.email.lowercase().contains(q) ||
            (acc.studentId?.lowercase()?.contains(q) == true) ||
            (acc.faculty?.lowercase()?.contains(q) == true) ||
            (acc.collectionPoint?.lowercase()?.contains(q) == true)
        matchesRole && matchesStatus && matchesQuery
    }
}

fun filterAccessLogs(
    logs: List<AccessLogEntry>,
    query: String,
    outcomeFilter: AccessOutcome? = null
): List<AccessLogEntry> {
    val q = query.trim().lowercase()
    return logs.filter { log ->
        val matchesOutcome = outcomeFilter == null || log.outcome == outcomeFilter
        val matchesQuery = q.isEmpty() ||
            log.email.lowercase().contains(q) ||
            log.eventType.lowercase().contains(q) ||
            (log.detail?.lowercase()?.contains(q) == true) ||
            (log.ipAddress?.lowercase()?.contains(q) == true)
        matchesOutcome && matchesQuery
    }
}

fun computeTokenSummary(
    transactions: List<Transaction>,
    invites: List<AdminInviteToken>,
    now: Long = System.currentTimeMillis()
): TokenMonitorSummary {
    var activeQr = 0
    var confirmedQr = 0
    var expiredQr = 0
    var cancelledQr = 0
    transactions.forEach { tx ->
        when (effectiveTransactionStatus(tx, now)) {
            TransactionStatus.PENDING -> activeQr++
            TransactionStatus.CONFIRMED -> confirmedQr++
            TransactionStatus.EXPIRED -> expiredQr++
            TransactionStatus.CANCELLED -> cancelledQr++
        }
    }
    val activeInvites = invites.count { isInviteTokenActive(it, now) }
    return TokenMonitorSummary(
        activeQr = activeQr,
        confirmedQr = confirmedQr,
        expiredQr = expiredQr,
        cancelledQr = cancelledQr,
        activeInvites = activeInvites
    )
}

fun computeAccountSummary(accounts: List<ManagedAccount>): AccountMonitorSummary =
    AccountMonitorSummary(
        total = accounts.size,
        students = accounts.count { it.role == Role.STUDENT },
        admins = accounts.count { it.role == Role.ADMIN },
        active = accounts.count { it.status == AccountStatus.ACTIVE },
        locked = accounts.count { it.status == AccountStatus.LOCKED },
        suspended = accounts.count { it.status == AccountStatus.SUSPENDED }
    )
