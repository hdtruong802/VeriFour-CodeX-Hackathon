package com.verifour.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AccessLogEntry
import com.verifour.app.model.AccessOutcome
import com.verifour.app.model.AccountStatus
import com.verifour.app.model.AdminInviteToken
import com.verifour.app.model.AdminOpsState
import com.verifour.app.model.AppLocale
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.CollectionRun
import com.verifour.app.model.FlysticState
import com.verifour.app.model.ManagedAccount
import com.verifour.app.model.Role
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.computeAccountSummary
import com.verifour.app.model.computeTokenSummary
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.model.filterAccessLogs
import com.verifour.app.model.filterManagedAccounts
import com.verifour.app.model.isInviteTokenActive
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.StatusPill
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

enum class AdminOpsTab {
    MONITOR,
    ACCOUNTS,
    TOKENS,
    ACCESS
}

private enum class AccountFilterChip {
    ALL,
    STUDENTS,
    ADMINS,
    LOCKED
}

private enum class QrTokenFilter {
    PENDING,
    ALL,
    CONFIRMED,
    ENDED
}

private enum class AccessFilterChip {
    ALL,
    SUCCESS,
    FAILED,
    BLOCKED
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminOpsScreen(
    state: FlysticState,
    adminOps: AdminOpsState,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onToggleStation: suspend (String) -> Unit,
    onEmptyCollectionPoint: suspend (String) -> CollectionRun?,
    onCreateAccount: suspend (
        name: String,
        email: String,
        password: String,
        role: Role,
        studentId: String?,
        faculty: String?,
        stationId: String?
    ) -> Unit,
    onUpdateAccountStatus: suspend (userId: String, status: AccountStatus) -> Unit,
    onUpdateAccountRole: suspend (userId: String, role: Role, stationId: String?) -> Unit,
    onRevokeUserTokens: suspend (userId: String) -> Unit,
    onRevokeQrToken: suspend (txId: String) -> Unit,
    onExpireStaleQrTokens: suspend () -> Unit,
    onCreateInviteToken: suspend (label: String, stationId: String, maxUses: Int, ttlHours: Int) -> Unit,
    onRevokeInviteToken: suspend (inviteId: String) -> Unit,
    onClearAuthLockout: suspend (throttleKey: String?) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var selectedTab by remember { mutableStateOf(AdminOpsTab.MONITOR) }
    var feedbackMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Account tab state
    var accountQuery by remember { mutableStateOf("") }
    var accountFilter by remember { mutableStateOf(AccountFilterChip.ALL) }
    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var editingRoleAccount by remember { mutableStateOf<ManagedAccount?>(null) }

    // Token tab state
    var qrQuery by remember { mutableStateOf("") }
    var qrFilter by remember { mutableStateOf(QrTokenFilter.PENDING) }
    var showCreateInviteDialog by remember { mutableStateOf(false) }

    // Access tab state
    var accessQuery by remember { mutableStateOf("") }
    var accessFilter by remember { mutableStateOf(AccessFilterChip.ALL) }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    LaunchedEffect(selectedTab) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1.seconds)
        }
    }

    val dateFormatter = remember(locale) {
        val jLoc = when (locale) {
            AppLocale.VI -> Locale.forLanguageTag("vi-VN")
            AppLocale.EN -> Locale.US
            AppLocale.KO -> Locale.KOREA
        }
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, jLoc)
    }

    fun runAction(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }
                .onSuccess {
                    feedbackMessage = t("adminOpsActionSuccess") to false
                }
                .onFailure { err ->
                    feedbackMessage = (err.message ?: "Error") to true
                }
        }
    }

    fun copyText(text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("VeriFour", text))
        feedbackMessage = t("copied") to false
    }

    val accountSummary = remember(adminOps.accounts) {
        computeAccountSummary(adminOps.accounts)
    }
    val tokenSummary = remember(adminOps.qrTokens, adminOps.inviteTokens, now) {
        computeTokenSummary(adminOps.qrTokens, adminOps.inviteTokens, now)
    }
    val activeStationsCount = adminOps.stationsOps.count { it.isActive }
    val securityAlertsCount = adminOps.lockouts.size +
        adminOps.accessLogs.count { it.outcome != AccessOutcome.SUCCESS && now - it.createdAt < 86_400_000L }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t("adminOpsTitle"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = t("adminOpsSubtitle"),
                        fontSize = 12.sp,
                        color = EcoMutedForeground
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = EcoPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = t("adminOpsRefresh"),
                            tint = EcoPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = t("adminOpsRefresh"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoPrimary
                    )
                }
            }
        }

        // Sub-navigation segmented tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    AdminOpsTab.MONITOR to t("adminOpsTabMonitor"),
                    AdminOpsTab.ACCOUNTS to "${t("adminOpsTabAccounts")} (${accountSummary.total})",
                    AdminOpsTab.TOKENS to "${t("adminOpsTabTokens")} (${tokenSummary.activeQr + tokenSummary.activeInvites})",
                    AdminOpsTab.ACCESS to "${t("adminOpsTabAccess")} (${adminOps.lockouts.size})"
                )
                tabs.forEach { (tab, label) ->
                    val selected = selectedTab == tab
                    Surface(
                        shape = CircleShape,
                        color = if (selected) EcoForeground else Color.White,
                        border = BorderStroke(1.dp, if (selected) EcoForeground else EcoBorder),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                selectedTab = tab
                                feedbackMessage = null
                            }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else EcoForeground,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Feedback banner
        if (feedbackMessage != null) {
            val (msg, isError) = feedbackMessage!!
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isError) EcoCoral.copy(alpha = 0.14f) else EcoPrimary.copy(alpha = 0.14f)
                        )
                        .border(
                            1.dp,
                            if (isError) EcoCoral.copy(alpha = 0.4f) else EcoPrimary.copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isError) EcoCoral else EcoPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "✕",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoMutedForeground,
                            modifier = Modifier
                                .clickable { feedbackMessage = null }
                                .padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // TAB 1: MONITOR
        if (selectedTab == AdminOpsTab.MONITOR) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OpsKpiCard(
                            label = t("adminOpsActiveStations"),
                            value = "$activeStationsCount / ${adminOps.stationsOps.size.coerceAtLeast(COLLECTION_POINTS.size)}",
                            valueColor = EcoPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        OpsKpiCard(
                            label = t("adminOpsOpenQrTokens"),
                            value = "${tokenSummary.activeQr}",
                            valueColor = EcoOcean,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OpsKpiCard(
                            label = t("adminOpsManagedAccounts"),
                            value = "${accountSummary.active} / ${accountSummary.total}",
                            valueColor = EcoForeground,
                            modifier = Modifier.weight(1f)
                        )
                        OpsKpiCard(
                            label = t("adminOpsSecurityAlerts"),
                            value = "$securityAlertsCount",
                            valueColor = if (securityAlertsCount > 0) EcoCoral else EcoPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Operational & Security Alerts Card
            item {
                val highFillStations = adminOps.stationsOps.filter { it.fillPercent >= 80 }
                val hasAlerts = highFillStations.isNotEmpty() || adminOps.lockouts.isNotEmpty() || accountSummary.locked > 0
                EcoCard(
                    contentPadding = 16.dp,
                    containerColor = if (hasAlerts) EcoSunshine.copy(alpha = 0.14f) else Color.White,
                    borderColor = if (hasAlerts) EcoSunshine.copy(alpha = 0.5f) else EcoBorder
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasAlerts) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (hasAlerts) EcoCoral else EcoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = t("adminOpsAlertsTitle"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!hasAlerts) {
                        Text(
                            text = t("adminOpsAllClear"),
                            fontSize = 12.sp,
                            color = EcoMutedForeground
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            highFillStations.forEach { st ->
                                val locName = FlysticTranslations.localizedCollectionPoint(st.name, locale).first
                                Text(
                                    text = "• $locName: ${st.fillPercent}% (${String.format(Locale.US, "%.1f", st.filledKg)}/${st.capacityKg.toInt()} kg)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EcoForeground
                                )
                            }
                            if (adminOps.lockouts.isNotEmpty()) {
                                Text(
                                    text = "• ${adminOps.lockouts.size} ${t("adminOpsLockoutsTitle")}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EcoCoral
                                )
                            }
                            if (accountSummary.locked > 0) {
                                Text(
                                    text = "• ${accountSummary.locked} ${t("adminOpsStatusLocked")}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EcoCoral
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = t("adminOpsStationControlTitle"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
            }

            items(adminOps.stationsOps, key = { it.id }) { st ->
                val locName = FlysticTranslations.localizedCollectionPoint(st.name, locale).first
                val highFill = st.fillPercent >= 80
                EcoCard(contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = locName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SmallPill(
                                    label = if (st.isActive) t("adminOpsStationActive") else t("adminOpsStationPaused"),
                                    fg = if (st.isActive) EcoPrimary else EcoCoral,
                                    bg = if (st.isActive) EcoPrimary.copy(alpha = 0.14f) else EcoCoral.copy(alpha = 0.14f)
                                )
                                Text(
                                    text = t("adminOpsStationPendingCount", mapOf("count" to st.pendingTokens)),
                                    fontSize = 11.sp,
                                    color = EcoMutedForeground
                                )
                                Text(
                                    text = t("adminOpsStationConfirmedToday", mapOf("count" to st.confirmedToday)),
                                    fontSize = 11.sp,
                                    color = EcoMutedForeground
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = st.isActive,
                            onCheckedChange = {
                                runAction { onToggleStation(st.id) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EcoPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = EcoCoral.copy(alpha = 0.6f)
                            )
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", st.filledKg)} / ${st.capacityKg.toInt()} kg",
                            fontSize = 12.sp,
                            color = EcoMutedForeground
                        )
                        Text(
                            text = "${st.fillPercent}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (highFill) EcoCoral else EcoPrimary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(EcoSecondary)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((st.fillPercent / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(if (highFill) EcoCoral else EcoPrimary)
                        )
                    }

                    if (st.filledKg > 0.0) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val run = runCatching { onEmptyCollectionPoint(st.name) }.getOrNull()
                                    feedbackMessage = if (run != null) {
                                        t("adminStationEmptySuccess", mapOf("weight" to String.format(Locale.US, "%.2f", run.weightKg))) to false
                                    } else {
                                        t("adminStationEmptyEmpty") to true
                                    }
                                    onRefresh()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t("adminOpsStationHandoverBtn"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary
                            )
                        }
                    }
                }
            }
        }

        // TAB 2: ACCOUNTS
        if (selectedTab == AdminOpsTab.ACCOUNTS) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = accountQuery,
                        onValueChange = { accountQuery = it },
                        placeholder = {
                            Text(
                                text = t("adminOpsAccountSearch"),
                                fontSize = 12.sp,
                                color = EcoMutedForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = EcoMutedForeground,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = EcoPrimary,
                            unfocusedBorderColor = EcoBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showCreateAccountDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EcoPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = t("adminOpsCreateAccount"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chips = listOf(
                        AccountFilterChip.ALL to "${t("adminOpsFilterAll")} (${accountSummary.total})",
                        AccountFilterChip.STUDENTS to "${t("adminOpsFilterStudents")} (${accountSummary.students})",
                        AccountFilterChip.ADMINS to "${t("adminOpsFilterAdmins")} (${accountSummary.admins})",
                        AccountFilterChip.LOCKED to "${t("adminOpsFilterLocked")} (${accountSummary.locked + accountSummary.suspended})"
                    )
                    chips.forEach { (chip, label) ->
                        FilterPillChip(
                            label = label,
                            selected = accountFilter == chip,
                            onClick = { accountFilter = chip }
                        )
                    }
                }
            }

            val filteredAccounts = when (accountFilter) {
                AccountFilterChip.ALL -> filterManagedAccounts(adminOps.accounts, accountQuery)
                AccountFilterChip.STUDENTS -> filterManagedAccounts(adminOps.accounts, accountQuery, roleFilter = Role.STUDENT)
                AccountFilterChip.ADMINS -> filterManagedAccounts(adminOps.accounts, accountQuery, roleFilter = Role.ADMIN)
                AccountFilterChip.LOCKED -> filterManagedAccounts(adminOps.accounts, accountQuery).filter {
                    it.status != AccountStatus.ACTIVE
                }
            }

            if (filteredAccounts.isEmpty()) {
                item {
                    EcoCard(contentPadding = 24.dp) {
                        Text(
                            text = t("adminNoData"),
                            fontSize = 14.sp,
                            color = EcoMutedForeground
                        )
                    }
                }
            } else {
                items(filteredAccounts, key = { it.id }) { acc ->
                    val isSelf = acc.id == state.user?.id || acc.email.equals(state.user?.email, ignoreCase = true)
                    EcoCard(contentPadding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = acc.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoForeground
                                    )
                                    SmallPill(
                                        label = if (acc.role == Role.ADMIN) "Admin" else t("adminOpsFilterStudents"),
                                        fg = if (acc.role == Role.ADMIN) EcoOcean else EcoPrimary,
                                        bg = if (acc.role == Role.ADMIN) EcoOcean.copy(alpha = 0.14f) else EcoSecondary
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = acc.email,
                                    fontSize = 12.sp,
                                    color = EcoMutedForeground
                                )
                                val subMeta = when (acc.role) {
                                    Role.ADMIN -> acc.collectionPoint?.let {
                                        FlysticTranslations.localizedCollectionPoint(it, locale).first
                                    } ?: acc.stationId.orEmpty()
                                    Role.STUDENT -> listOfNotNull(acc.studentId, acc.faculty).joinToString(" · ")
                                }
                                if (subMeta.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = subMeta,
                                        fontSize = 11.sp,
                                        color = EcoMutedForeground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            AccountStatusPill(status = acc.status, locale = locale)
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = t("adminOpsTokenVersion", mapOf("version" to acc.tokenVersion)),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoOcean
                            )
                            Text(
                                text = if (acc.lastLoginAt != null) {
                                    t("adminOpsLastLogin", mapOf("time" to dateFormatter.format(Date(acc.lastLoginAt))))
                                } else {
                                    t("adminOpsNeverLoggedIn")
                                },
                                fontSize = 11.sp,
                                color = EcoMutedForeground
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isSelf) {
                                if (acc.status == AccountStatus.ACTIVE) {
                                    ActionChip(
                                        icon = Icons.Rounded.Lock,
                                        label = t("adminOpsActionLock"),
                                        color = EcoCoral,
                                        onClick = {
                                            runAction { onUpdateAccountStatus(acc.id, AccountStatus.LOCKED) }
                                        }
                                    )
                                    ActionChip(
                                        icon = Icons.Rounded.Block,
                                        label = t("adminOpsActionSuspend"),
                                        color = EcoMutedForeground,
                                        onClick = {
                                            runAction { onUpdateAccountStatus(acc.id, AccountStatus.SUSPENDED) }
                                        }
                                    )
                                } else {
                                    ActionChip(
                                        icon = Icons.Rounded.LockOpen,
                                        label = t("adminOpsActionUnlock"),
                                        color = EcoPrimary,
                                        onClick = {
                                            runAction { onUpdateAccountStatus(acc.id, AccountStatus.ACTIVE) }
                                        }
                                    )
                                }
                            }
                            ActionChip(
                                icon = Icons.Rounded.AdminPanelSettings,
                                label = t("adminOpsActionRole"),
                                color = EcoOcean,
                                onClick = { editingRoleAccount = acc }
                            )
                            ActionChip(
                                icon = Icons.Rounded.Key,
                                label = t("adminOpsActionRevokeTokens"),
                                color = EcoCoral,
                                onClick = {
                                    runAction { onRevokeUserTokens(acc.id) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // TAB 3: TOKENS
        if (selectedTab == AdminOpsTab.TOKENS) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactTokenStat(
                        label = t("adminOpsTokenSummaryPending"),
                        count = tokenSummary.activeQr,
                        color = EcoOcean,
                        modifier = Modifier.weight(1f)
                    )
                    CompactTokenStat(
                        label = t("adminOpsTokenSummaryConfirmed"),
                        count = tokenSummary.confirmedQr,
                        color = EcoPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    CompactTokenStat(
                        label = t("adminOpsTokenSummaryExpired"),
                        count = tokenSummary.expiredQr + tokenSummary.cancelledQr,
                        color = EcoCoral,
                        modifier = Modifier.weight(1f)
                    )
                    CompactTokenStat(
                        label = t("adminOpsTokenSummaryInvites"),
                        count = tokenSummary.activeInvites,
                        color = EcoForeground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { runAction { onExpireStaleQrTokens() } },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = t("adminOpsExpireStaleBtn"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                    }
                    Button(
                        onClick = { showCreateInviteDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EcoPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = t("adminOpsCreateInviteBtn"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Admin Invite / Access Tokens
            item {
                Text(
                    text = t("adminOpsInviteTokensSection"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
            }

            items(adminOps.inviteTokens, key = { it.id }) { inv ->
                InviteTokenCard(
                    invite = inv,
                    now = now,
                    locale = locale,
                    dateFormatter = dateFormatter,
                    onCopy = { copyText(inv.code) },
                    onRevoke = { runAction { onRevokeInviteToken(inv.id) } }
                )
            }

            // QR Transaction Tokens
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = t("adminOpsQrTokensSection"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    OutlinedTextField(
                        value = qrQuery,
                        onValueChange = { qrQuery = it },
                        placeholder = {
                            Text(t("adminOpsQrSearch"), fontSize = 12.sp, color = EcoMutedForeground)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = EcoMutedForeground,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = EcoPrimary,
                            unfocusedBorderColor = EcoBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chips = listOf(
                            QrTokenFilter.PENDING to t("adminTransactionPending"),
                            QrTokenFilter.ALL to t("adminStatusAll"),
                            QrTokenFilter.CONFIRMED to t("adminTransactionConfirmed"),
                            QrTokenFilter.ENDED to "${t("adminTransactionExpired")} / ${t("adminTransactionCancelled")}"
                        )
                        chips.forEach { (chip, label) ->
                            FilterPillChip(
                                label = label,
                                selected = qrFilter == chip,
                                onClick = { qrFilter = chip }
                            )
                        }
                    }
                }
            }

            val filteredQr = adminOps.qrTokens.filter { tx ->
                val eff = effectiveTransactionStatus(tx, now)
                val matchesStatus = when (qrFilter) {
                    QrTokenFilter.PENDING -> eff == TransactionStatus.PENDING
                    QrTokenFilter.ALL -> true
                    QrTokenFilter.CONFIRMED -> eff == TransactionStatus.CONFIRMED
                    QrTokenFilter.ENDED -> eff == TransactionStatus.EXPIRED || eff == TransactionStatus.CANCELLED
                }
                val q = qrQuery.trim().lowercase()
                val matchesQuery = q.isEmpty() ||
                    tx.token.lowercase().contains(q) ||
                    tx.id.lowercase().contains(q) ||
                    tx.collectionPoint.lowercase().contains(q) ||
                    (tx.userName?.lowercase()?.contains(q) == true)
                matchesStatus && matchesQuery
            }

            if (filteredQr.isEmpty()) {
                item {
                    EcoCard(contentPadding = 20.dp) {
                        Text(text = t("adminNoData"), fontSize = 13.sp, color = EcoMutedForeground)
                    }
                }
            } else {
                items(filteredQr, key = { it.id }) { tx ->
                    val effStatus = effectiveTransactionStatus(tx, now)
                    val remainingSec = max(0L, (tx.expiresAt - now) / 1000L)
                    EcoCard(contentPadding = 14.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tx.token.ifBlank { tx.id.take(10).uppercase() },
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoForeground
                                )
                                if (tx.token.isNotBlank()) {
                                    IconButton(
                                        onClick = { copyText(tx.token) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = t("copy"),
                                            tint = EcoMutedForeground,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            StatusPill(status = effStatus, locale = locale)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = FlysticTranslations.localizedCollectionPoint(tx.collectionPoint, locale).first,
                            fontSize = 12.sp,
                            color = EcoMutedForeground
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.2f", tx.weightKg)} kg · +${tx.points} pts" +
                                    (tx.userName?.let { " · $it" } ?: ""),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoForeground,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (effStatus == TransactionStatus.PENDING) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", remainingSec / 60, remainingSec % 60),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoOcean
                                )
                            }
                        }
                        if (effStatus == TransactionStatus.PENDING) {
                            Spacer(Modifier.height(10.dp))
                            ActionChip(
                                icon = Icons.Rounded.Block,
                                label = t("adminOpsRevokeQrBtn"),
                                color = EcoCoral,
                                onClick = { runAction { onRevokeQrToken(tx.id) } }
                            )
                        }
                    }
                }
            }
        }

        // TAB 4: ACCESS & SECURITY
        if (selectedTab == AdminOpsTab.ACCESS) {
            item {
                EcoCard(
                    contentPadding = 16.dp,
                    containerColor = if (adminOps.lockouts.isNotEmpty()) EcoCoral.copy(alpha = 0.08f) else Color.White,
                    borderColor = if (adminOps.lockouts.isNotEmpty()) EcoCoral.copy(alpha = 0.35f) else EcoBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = if (adminOps.lockouts.isNotEmpty()) EcoCoral else EcoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = t("adminOpsLockoutsTitle"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground
                            )
                        }
                        if (adminOps.lockouts.isNotEmpty()) {
                            TextButton(onClick = { runAction { onClearAuthLockout(null) } }) {
                                Text(
                                    text = t("adminOpsClearAllThrottlesBtn"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoCoral
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (adminOps.lockouts.isEmpty()) {
                        Text(
                            text = t("adminOpsNoLockouts"),
                            fontSize = 12.sp,
                            color = EcoMutedForeground
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            adminOps.lockouts.forEach { lockout ->
                                val minsLeft = max(1L, (lockout.lockedUntil - now) / 60_000L)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .border(1.dp, EcoBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lockout.emailHint,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoForeground
                                        )
                                        Text(
                                            text = "${t("adminOpsFailureCount", mapOf("count" to lockout.failureCount))} · ~${minsLeft}m",
                                            fontSize = 11.sp,
                                            color = EcoCoral
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { runAction { onClearAuthLockout(lockout.throttleKey) } },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = t("adminOpsUnlockThrottleBtn"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = t("adminOpsAccessLogsTitle"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    OutlinedTextField(
                        value = accessQuery,
                        onValueChange = { accessQuery = it },
                        placeholder = {
                            Text(t("adminOpsAccessSearch"), fontSize = 12.sp, color = EcoMutedForeground)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = EcoMutedForeground,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = EcoPrimary,
                            unfocusedBorderColor = EcoBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chips = listOf(
                            AccessFilterChip.ALL to t("adminOpsFilterAll"),
                            AccessFilterChip.SUCCESS to t("adminOpsOutcomeSuccess"),
                            AccessFilterChip.FAILED to t("adminOpsOutcomeFailed"),
                            AccessFilterChip.BLOCKED to t("adminOpsOutcomeBlocked")
                        )
                        chips.forEach { (chip, label) ->
                            FilterPillChip(
                                label = label,
                                selected = accessFilter == chip,
                                onClick = { accessFilter = chip }
                            )
                        }
                    }
                }
            }

            val filteredLogs = filterAccessLogs(
                logs = adminOps.accessLogs,
                query = accessQuery,
                outcomeFilter = when (accessFilter) {
                    AccessFilterChip.ALL -> null
                    AccessFilterChip.SUCCESS -> AccessOutcome.SUCCESS
                    AccessFilterChip.FAILED -> AccessOutcome.FAILED
                    AccessFilterChip.BLOCKED -> AccessOutcome.BLOCKED
                }
            )

            if (filteredLogs.isEmpty()) {
                item {
                    EcoCard(contentPadding = 20.dp) {
                        Text(text = t("adminNoData"), fontSize = 13.sp, color = EcoMutedForeground)
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    AccessLogCard(
                        log = log,
                        locale = locale,
                        dateFormatter = dateFormatter
                    )
                }
            }
        }
    }

    if (showCreateAccountDialog) {
        CreateAccountDialog(
            locale = locale,
            onDismiss = { showCreateAccountDialog = false },
            onSubmit = { name, email, password, role, studentId, faculty, stationId ->
                showCreateAccountDialog = false
                runAction {
                    onCreateAccount(name, email, password, role, studentId, faculty, stationId)
                }
            }
        )
    }

    if (editingRoleAccount != null) {
        EditRoleDialog(
            account = editingRoleAccount!!,
            locale = locale,
            onDismiss = { editingRoleAccount = null },
            onSubmit = { role, stationId ->
                val targetId = editingRoleAccount!!.id
                editingRoleAccount = null
                runAction { onUpdateAccountRole(targetId, role, stationId) }
            }
        )
    }

    if (showCreateInviteDialog) {
        CreateInviteDialog(
            locale = locale,
            onDismiss = { showCreateInviteDialog = false },
            onSubmit = { label, stationId, maxUses, ttlHours ->
                showCreateInviteDialog = false
                runAction { onCreateInviteToken(label, stationId, maxUses, ttlHours) }
            }
        )
    }
}

@Composable
private fun OpsKpiCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    EcoCard(modifier = modifier, contentPadding = 14.dp) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = EcoMutedForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun CompactTokenStat(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    EcoCard(modifier = modifier, contentPadding = 10.dp) {
        Text(
            text = "$count",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = EcoMutedForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SmallPill(label: String, fg: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun AccountStatusPill(status: AccountStatus, locale: AppLocale) {
    val (label, fg, bg) = when (status) {
        AccountStatus.ACTIVE -> Triple(
            FlysticTranslations.t("adminOpsStatusActive", locale),
            EcoPrimary,
            EcoPrimary.copy(alpha = 0.14f)
        )
        AccountStatus.LOCKED -> Triple(
            FlysticTranslations.t("adminOpsStatusLocked", locale),
            EcoCoral,
            EcoCoral.copy(alpha = 0.14f)
        )
        AccountStatus.SUSPENDED -> Triple(
            FlysticTranslations.t("adminOpsStatusSuspended", locale),
            EcoMutedForeground,
            EcoSecondary
        )
    }
    SmallPill(label = label, fg = fg, bg = bg)
}

@Composable
private fun FilterPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (selected) EcoPrimary else Color.White,
        border = BorderStroke(1.dp, if (selected) EcoPrimary else EcoBorder),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else EcoForeground,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun InviteTokenCard(
    invite: AdminInviteToken,
    now: Long,
    locale: AppLocale,
    dateFormatter: DateFormat,
    onCopy: () -> Unit,
    onRevoke: () -> Unit
) {
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    val active = isInviteTokenActive(invite, now)
    val (statusLabel, statusFg, statusBg) = when {
        invite.isRevoked -> Triple(t("adminOpsInviteRevoked"), EcoCoral, EcoCoral.copy(alpha = 0.14f))
        active -> Triple(t("adminOpsInviteActive"), EcoPrimary, EcoPrimary.copy(alpha = 0.14f))
        else -> Triple(t("adminOpsInviteExpired"), EcoMutedForeground, EcoSecondary)
    }

    EcoCard(contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = invite.code,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = t("copy"),
                        tint = EcoMutedForeground,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            SmallPill(label = statusLabel, fg = statusFg, bg = statusBg)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = invite.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoForeground
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${FlysticTranslations.localizedCollectionPoint(invite.stationName, locale).first} · ${
                t("adminOpsInviteUsage", mapOf("used" to invite.usedCount, "max" to invite.maxUses))
            }",
            fontSize = 11.sp,
            color = EcoMutedForeground
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateFormatter.format(Date(invite.expiresAt)),
                fontSize = 11.sp,
                color = EcoMutedForeground
            )
            if (active) {
                ActionChip(
                    icon = Icons.Rounded.Block,
                    label = t("adminOpsRevokeInviteBtn"),
                    color = EcoCoral,
                    onClick = onRevoke
                )
            }
        }
    }
}

@Composable
private fun AccessLogCard(
    log: AccessLogEntry,
    locale: AppLocale,
    dateFormatter: DateFormat
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)

    val (outcomeLabel, outcomeFg, outcomeBg) = when (log.outcome) {
        AccessOutcome.SUCCESS -> Triple(t("adminOpsOutcomeSuccess"), EcoPrimary, EcoPrimary.copy(alpha = 0.14f))
        AccessOutcome.FAILED -> Triple(t("adminOpsOutcomeFailed"), EcoSunshine, EcoSunshine.copy(alpha = 0.2f))
        AccessOutcome.BLOCKED -> Triple(t("adminOpsOutcomeBlocked"), EcoCoral, EcoCoral.copy(alpha = 0.14f))
    }

    EcoCard(contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = outcomeFg,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = log.eventType.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
            }
            SmallPill(label = outcomeLabel, fg = outcomeFg, bg = outcomeBg)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = log.email + (log.role?.let { " ($it)" } ?: ""),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoForeground
        )
        if (!log.detail.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = log.detail,
                fontSize = 12.sp,
                color = EcoMutedForeground
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = listOfNotNull(log.ipAddress, log.deviceInfo).joinToString(" · ").ifBlank { "—" },
                fontSize = 10.sp,
                color = EcoMutedForeground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = dateFormatter.format(Date(log.createdAt)),
                fontSize = 10.sp,
                color = EcoMutedForeground
            )
        }
    }
}

@Composable
private fun CreateAccountDialog(
    locale: AppLocale,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        email: String,
        password: String,
        role: Role,
        studentId: String?,
        faculty: String?,
        stationId: String?
    ) -> Unit
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.STUDENT) }
    var studentId by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var stationId by remember { mutableStateOf(COLLECTION_POINTS.first().id) }
    var stationExpanded by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && email.contains("@") && password.length >= 8

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("adminOpsCreateAccountTitle"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPillChip(
                        label = t("adminOpsFilterStudents"),
                        selected = role == Role.STUDENT,
                        onClick = { role = Role.STUDENT }
                    )
                    FilterPillChip(
                        label = t("adminOpsFilterAdmins"),
                        selected = role == Role.ADMIN,
                        onClick = { role = Role.ADMIN }
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(t("authFullName")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(t("adminOpsPasswordLabel")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (role == Role.STUDENT) {
                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = { Text(t("authStudentId")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = faculty,
                        onValueChange = { faculty = it },
                        label = { Text(t("authSchool")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box {
                        val selectedPoint = COLLECTION_POINTS.first { it.id == stationId }
                        OutlinedButton(
                            onClick = { stationExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = FlysticTranslations.localizedCollectionPoint(
                                    selectedPoint.canonicalName,
                                    locale
                                ).first,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = stationExpanded,
                            onDismissRequest = { stationExpanded = false }
                        ) {
                            COLLECTION_POINTS.forEach { pt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            FlysticTranslations.localizedCollectionPoint(pt.canonicalName, locale).first,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        stationId = pt.id
                                        stationExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        name,
                        email,
                        password,
                        role,
                        studentId.ifBlank { null },
                        faculty.ifBlank { null },
                        if (role == Role.ADMIN) stationId else null
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
            ) {
                Text(t("adminOpsSave"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("adminOpsCancel"), color = EcoMutedForeground)
            }
        }
    )
}

@Composable
private fun EditRoleDialog(
    account: ManagedAccount,
    locale: AppLocale,
    onDismiss: () -> Unit,
    onSubmit: (role: Role, stationId: String?) -> Unit
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)
    var role by remember { mutableStateOf(account.role) }
    var stationId by remember {
        mutableStateOf(account.stationId ?: COLLECTION_POINTS.first().id)
    }
    var stationExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("adminOpsEditRoleTitle"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${account.name} (${account.email})",
                    fontSize = 13.sp,
                    color = EcoMutedForeground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPillChip(
                        label = t("adminOpsFilterStudents"),
                        selected = role == Role.STUDENT,
                        onClick = { role = Role.STUDENT }
                    )
                    FilterPillChip(
                        label = t("adminOpsFilterAdmins"),
                        selected = role == Role.ADMIN,
                        onClick = { role = Role.ADMIN }
                    )
                }
                if (role == Role.ADMIN) {
                    Box {
                        val selectedPoint = COLLECTION_POINTS.firstOrNull { it.id == stationId }
                            ?: COLLECTION_POINTS.first()
                        OutlinedButton(
                            onClick = { stationExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = FlysticTranslations.localizedCollectionPoint(
                                    selectedPoint.canonicalName,
                                    locale
                                ).first,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = stationExpanded,
                            onDismissRequest = { stationExpanded = false }
                        ) {
                            COLLECTION_POINTS.forEach { pt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            FlysticTranslations.localizedCollectionPoint(pt.canonicalName, locale).first,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        stationId = pt.id
                                        stationExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(role, if (role == Role.ADMIN) stationId else null) },
                colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
            ) {
                Text(t("adminOpsSave"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("adminOpsCancel"), color = EcoMutedForeground)
            }
        }
    )
}

@Composable
private fun CreateInviteDialog(
    locale: AppLocale,
    onDismiss: () -> Unit,
    onSubmit: (label: String, stationId: String, maxUses: Int, ttlHours: Int) -> Unit
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)
    var label by remember { mutableStateOf("") }
    var stationId by remember { mutableStateOf(COLLECTION_POINTS.first().id) }
    var maxUsesText by remember { mutableStateOf("3") }
    var ttlHoursText by remember { mutableStateOf("24") }
    var stationExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("adminOpsCreateInviteTitle"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(t("adminOpsInviteLabelHint")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    val selectedPoint = COLLECTION_POINTS.first { it.id == stationId }
                    OutlinedButton(
                        onClick = { stationExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = FlysticTranslations.localizedCollectionPoint(
                                selectedPoint.canonicalName,
                                locale
                            ).first,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = stationExpanded,
                        onDismissRequest = { stationExpanded = false }
                    ) {
                        COLLECTION_POINTS.forEach { pt ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        FlysticTranslations.localizedCollectionPoint(pt.canonicalName, locale).first,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    stationId = pt.id
                                    stationExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxUsesText,
                        onValueChange = { maxUsesText = it.filter { c -> c.isDigit() } },
                        label = { Text(t("adminOpsInviteMaxUses")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ttlHoursText,
                        onValueChange = { ttlHoursText = it.filter { c -> c.isDigit() } },
                        label = { Text(t("adminOpsInviteTtlHours")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val uses = maxUsesText.toIntOrNull()?.coerceIn(1, 50) ?: 3
                    val ttl = ttlHoursText.toIntOrNull()?.coerceIn(1, 720) ?: 24
                    onSubmit(label, stationId, uses, ttl)
                },
                enabled = label.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
            ) {
                Text(t("adminOpsSave"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("adminOpsCancel"), color = EcoMutedForeground)
            }
        }
    )
}
