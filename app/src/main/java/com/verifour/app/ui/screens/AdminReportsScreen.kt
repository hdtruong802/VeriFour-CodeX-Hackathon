package com.verifour.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.DemoCampaignMetric
import com.verifour.app.model.FlysticState
import com.verifour.app.model.PlasticType
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMuted
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CO2_KG_PER_KG_PLASTIC = 2.5
private const val DEMO_TRANSACTION_PREFIX = "demo-2026-"

private enum class ReportView {
    DEMO,
    LIVE
}

private data class DayBarMetric(
    val label: String,
    val kg: Double
)

private data class TopContributor(
    val name: String,
    val points: Int,
    val kg: Double
)

@Composable
fun AdminReportsScreen(
    state: FlysticState,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    val demoCampaign = state.demoCampaign
    val hasDemo = (demoCampaign?.transactionCount ?: 0) > 0
    var view by remember(hasDemo) {
        mutableStateOf(if (hasDemo) ReportView.DEMO else ReportView.LIVE)
    }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    val showDemo = view == ReportView.DEMO && hasDemo
    val stationName = state.user?.collectionPoint ?: COLLECTION_POINTS.first().canonicalName
    val now = System.currentTimeMillis()
    val since = now - 7L * 86_400_000L

    val confirmed = state.transactions.filter { tx ->
        tx.collectionPoint == stationName &&
            effectiveTransactionStatus(tx, now) == TransactionStatus.CONFIRMED &&
            if (showDemo) {
                tx.id.startsWith(DEMO_TRANSACTION_PREFIX)
            } else {
                (tx.confirmedAt ?: tx.createdAt) >= since
            }
    }

    val liveTotalKg = confirmed.sumOf { it.weightKg }
    val liveTotalPoints = confirmed.sumOf { it.points }
    val liveParticipantCount = confirmed.mapNotNull { it.userId }.distinct().size

    val totalKg = if (showDemo) (demoCampaign?.totalKg ?: 0.0) else liveTotalKg
    val totalPoints = if (showDemo) (demoCampaign?.totalPoints ?: 0) else liveTotalPoints
    val participantCount = if (showDemo) (demoCampaign?.participantCount ?: 0) else liveParticipantCount

    val jLocale = when (locale) {
        AppLocale.VI -> Locale.forLanguageTag("vi-VN")
        AppLocale.EN -> Locale.US
        AppLocale.KO -> Locale.KOREA
    }

    val dayBars = remember(confirmed, locale, now) {
        val weekdayFormat = SimpleDateFormat("EEE", jLocale)
        (0 until 7).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -(6 - offset))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            val end = start + 86_400_000L
            val kg = confirmed
                .filter { tx ->
                    val recorded = tx.confirmedAt ?: tx.createdAt
                    recorded in start until end
                }
                .sumOf { it.weightKg }
            DayBarMetric(label = weekdayFormat.format(cal.time), kg = kg)
        }
    }
    val maxDayKg = max(1.0, dayBars.maxOfOrNull { it.kg } ?: 1.0)

    val topContributors = remember(confirmed) {
        confirmed
            .filter { it.userId != null }
            .groupBy { it.userId!! }
            .values
            .map { txs ->
                TopContributor(
                    name = txs.firstNotNullOfOrNull { it.userName } ?: "VeriFour Student",
                    points = txs.sumOf { it.points },
                    kg = txs.sumOf { it.weightKg }
                )
            }
            .sortedByDescending { it.points }
            .take(5)
    }

    fun exportCsv() {
        runCatching {
            val dateFmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, jLocale)
            val rows = buildList {
                add(listOf("Mã giao dịch", "Thời gian xác nhận", "Sinh viên", "Trạm", "Khối lượng (kg)", "Điểm"))
                confirmed.forEach { tx ->
                    add(
                        listOf(
                            tx.id,
                            dateFmt.format(Date(tx.confirmedAt ?: tx.createdAt)),
                            tx.userName.orEmpty(),
                            tx.collectionPoint,
                            String.format(Locale.US, "%.2f", tx.weightKg),
                            tx.points.toString()
                        )
                    )
                }
            }
            val csv = "\uFEFF" + rows.joinToString("\n") { row ->
                row.joinToString(",") { cell -> "\"${cell.replace("\"", "\"\"")}\"" }
            }
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val dateTag = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            val fileName = "verifour-${if (showDemo) "demo-jun-jul-2026" else dateTag}.csv"
            val csvFile = File(exportDir, fileName)
            csvFile.writeText(csv, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, t("adminExportExcel")))
            exportMessage = t("adminReportCsvDownloaded")
        }.onFailure {
            exportMessage = t("adminReportCsvError")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t("adminReportsTitle"),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (showDemo) t("adminReportDemoDescription") else t("adminReportsDescription"),
                        fontSize = 13.sp,
                        color = EcoMutedForeground
                    )
                }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = { exportCsv() },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        tint = EcoForeground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = t("adminExportExcel"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoForeground
                    )
                }
            }
        }

        if (exportMessage != null) {
            item {
                Text(
                    text = exportMessage!!,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoPrimary
                )
            }
        }

        // View switcher: Demo vs Live
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportViewTab(
                    label = t("adminReportViewDemo"),
                    selected = showDemo,
                    enabled = hasDemo,
                    onClick = { if (hasDemo) view = ReportView.DEMO }
                )
                ReportViewTab(
                    label = t("adminReportViewLive"),
                    selected = !showDemo,
                    enabled = true,
                    onClick = { view = ReportView.LIVE }
                )
            }
        }

        if (showDemo) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EcoSunshine.copy(alpha = 0.22f))
                        .border(1.dp, EcoSunshine.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "⚠️ ${t("adminReportDemoLabel")} · 06–07/2026",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                }
            }
        }

        // 4 KPI Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard(
                        label = t("adminReportTotalWaste"),
                        value = "${String.format(Locale.US, "%.1f", totalKg)} kg",
                        sub = "≈ ${t("adminReportPetBottles", mapOf("count" to (totalKg * 40).roundToInt()))}",
                        modifier = Modifier.weight(1f)
                    )
                    ReportKpiCard(
                        label = t("adminReportPointsIssued"),
                        value = FlysticTranslations.formatNumber(totalPoints, locale),
                        sub = "${t("adminReportTotal")} ${confirmed.size} ${t("adminReportTransactions")}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showDemo) {
                        ReportKpiCard(
                            label = t("adminRegisteredStudents"),
                            value = FlysticTranslations.formatNumber(
                                demoCampaign?.registeredStudents ?: 0,
                                locale
                            ),
                            sub = "",
                            modifier = Modifier.weight(1f)
                        )
                        ReportKpiCard(
                            label = t("adminFundraisingAllocation"),
                            value = "${demoCampaign?.fundraisingPercent ?: 0}%",
                            sub = t(
                                "adminDemoFundraisingDetail",
                                mapOf(
                                    "points" to FlysticTranslations.formatNumber(
                                        demoCampaign?.fundraisingPoints ?: 0,
                                        locale
                                    ),
                                    "total" to FlysticTranslations.formatNumber(totalPoints, locale)
                                )
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        ReportKpiCard(
                            label = t("adminReportParticipants"),
                            value = "$participantCount",
                            sub = "",
                            modifier = Modifier.weight(1f)
                        )
                        ReportKpiCard(
                            label = t("adminReportCo2Saved"),
                            value = "${String.format(Locale.US, "%.1f", totalKg * CO2_KG_PER_KG_PLASTIC)} kg",
                            sub = t("adminReportEstimated"),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Charts: Demo Breakdown vs Live 7-day vertical bars
        if (showDemo) {
            item {
                BreakdownCard(
                    title = t("adminDemoBreakdown"),
                    rows = demoCampaign?.byType ?: emptyList(),
                    labelForRow = { row ->
                        val emoji = PlasticType.entries.firstOrNull { it.code == row.label }?.emoji ?: "♻️"
                        "$emoji ${row.label}"
                    }
                )
            }
            item {
                BreakdownCard(
                    title = t("adminDemoQuality"),
                    rows = demoCampaign?.byQuality ?: emptyList(),
                    labelForRow = { row ->
                        when (row.label) {
                            "CLEAN" -> t("adminQualityClean")
                            "DIRTY" -> t("adminQualityDirty")
                            else -> t("adminQualityNormal")
                        }
                    }
                )
            }
        } else {
            item {
                EcoCard(contentPadding = 18.dp) {
                    Text(
                        text = t("adminWeeklyCollectionByDay"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayBars.forEach { bar ->
                            val heightFraction = max(0.06f, (bar.kg / maxDayKg).toFloat().coerceIn(0f, 1f))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(heightFraction)
                                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(EcoPrimaryGlow, EcoPrimary)
                                                )
                                            )
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = bar.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoMutedForeground,
                                    maxLines = 1
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", bar.kg),
                                    fontSize = 10.sp,
                                    color = EcoMutedForeground
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top 5 students
        item {
            EcoCard(contentPadding = 18.dp) {
                Text(
                    text = t("adminTopStudents"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(12.dp))
                if (topContributors.isEmpty()) {
                    Text(
                        text = t("adminNoData"),
                        fontSize = 13.sp,
                        color = EcoMutedForeground
                    )
                } else {
                    topContributors.forEachIndexed { index, row ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = EcoBorder.copy(alpha = 0.6f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeBg = when (index) {
                                0 -> EcoSunshine.copy(alpha = 0.45f)
                                1 -> EcoMuted
                                2 -> EcoCoral.copy(alpha = 0.2f)
                                else -> EcoSecondary
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(badgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoForeground
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoForeground
                                )
                                Text(
                                    text = t(
                                        "adminReportKgCollected",
                                        mapOf("kg" to String.format(Locale.US, "%.1f", row.kg))
                                    ),
                                    fontSize = 12.sp,
                                    color = EcoMutedForeground
                                )
                            }
                            Text(
                                text = "${FlysticTranslations.formatNumber(row.points, locale)}đ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary
                            )
                        }
                    }
                }
            }
        }

        // Monthly target progress
        item {
            val goals = listOf(
                Triple(t("adminGoalCollected"), totalKg, 100.0 to "kg"),
                Triple(t("adminGoalParticipants"), participantCount.toDouble(), 500.0 to "SV"),
                Triple(t("adminGoalCo2"), totalKg * CO2_KG_PER_KG_PLASTIC, 250.0 to "kg")
            )
            EcoCard(contentPadding = 18.dp) {
                Text(
                    text = t("adminMonthlyTargetProgress"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    goals.forEach { (label, current, targetPair) ->
                        val (target, unit) = targetPair
                        val percent = min(100, ((current / target) * 100.0).roundToInt())
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoForeground
                                )
                                Text(
                                    text = "${current.roundToInt()} / ${target.toInt()} $unit",
                                    fontSize = 12.sp,
                                    color = EcoMutedForeground
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(EcoSecondary)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(EcoPrimary)
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "$percent%",
                                fontSize = 10.sp,
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

@Composable
private fun ReportViewTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) EcoPrimary else Color.White,
        border = BorderStroke(1.dp, if (selected) EcoPrimary else EcoBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                selected -> Color.White
                !enabled -> EcoMutedForeground.copy(alpha = 0.4f)
                else -> EcoMutedForeground
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun ReportKpiCard(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    EcoCard(modifier = modifier, contentPadding = 16.dp) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = EcoMutedForeground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EcoPrimary
        )
        if (sub.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = sub,
                fontSize = 11.sp,
                color = EcoMutedForeground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BreakdownCard(
    title: String,
    rows: List<DemoCampaignMetric>,
    labelForRow: (DemoCampaignMetric) -> String
) {
    val maxKg = max(1.0, rows.maxOfOrNull { it.kg } ?: 1.0)
    EcoCard(contentPadding = 18.dp) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EcoForeground
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                val ratio = (row.kg / maxKg).toFloat().coerceIn(0f, 1f)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = labelForRow(row),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EcoForeground
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", row.kg)} kg",
                            fontSize = 12.sp,
                            color = EcoMutedForeground
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(EcoSecondary)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(EcoPrimary)
                        )
                    }
                }
            }
        }
    }
}
