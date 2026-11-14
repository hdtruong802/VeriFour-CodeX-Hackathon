package com.verifour.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.CollectionRun
import com.verifour.app.model.FlysticState
import com.verifour.app.model.PlasticType
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.model.getStationFillKg
import com.verifour.app.model.getStationFillPercent
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.StatusPill
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

@Composable
fun AdminDashboardScreen(
    state: FlysticState,
    isRemote: Boolean,
    onOpenNewTransaction: () -> Unit,
    onOpenTransactions: () -> Unit,
    onEmptyCollectionPoint: suspend (String) -> CollectionRun?,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val scope = rememberCoroutineScope()
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var handoverMessage by remember { mutableStateOf<String?>(null) }
    var isHandoverLoading by remember { mutableStateOf(false) }

    val stationName = state.user?.collectionPoint ?: COLLECTION_POINTS.first().canonicalName
    val station = COLLECTION_POINTS.firstOrNull {
        it.canonicalName == stationName || it.id == stationName
    } ?: COLLECTION_POINTS.first()

    val localizedStationName = FlysticTranslations.localizedCollectionPoint(
        station.canonicalName,
        locale
    ).first

    val now = System.currentTimeMillis()
    val stationTransactions = state.transactions.filter {
        it.collectionPoint == station.canonicalName
    }
    val recentConfirmed = stationTransactions.filter { tx ->
        effectiveTransactionStatus(tx, now) == TransactionStatus.CONFIRMED &&
            now - (tx.confirmedAt ?: tx.createdAt) < 86_400_000L * 7L
    }

    val totalKg = recentConfirmed.sumOf { it.weightKg }
    val totalPoints = recentConfirmed.sumOf { it.points }
    val pendingCount = stationTransactions.count {
        effectiveTransactionStatus(tx = it, now = now) == TransactionStatus.PENDING
    }

    val remoteStation = state.stations.firstOrNull { it.id == station.id }
    val stationFillKg = if (isRemote) {
        remoteStation?.filledKg ?: 0.0
    } else {
        getStationFillKg(state.transactions, state.collectionRuns, station.canonicalName, now)
    }
    val stationFillPercent = if (isRemote) {
        remoteStation?.fillPercent ?: 0
    } else {
        getStationFillPercent(state.transactions, state.collectionRuns, station, now)
    }

    val byType = PlasticType.entries.map { type ->
        val kg = recentConfirmed.sumOf { tx ->
            tx.items.filter { it.plasticType == type }.sumOf { it.quantity }
        }
        type to kg
    }
    val maxTypeKg = max(1.0, byType.maxOfOrNull { it.second } ?: 1.0)

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
                        text = t("adminStation", mapOf("station" to localizedStationName)).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoMutedForeground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = t("adminDashboardTitle"),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onOpenNewTransaction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EcoPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = t("adminCreateTransaction"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // KPI Grid 2x2
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        label = t("adminCollectedWeek"),
                        value = "${String.format(Locale.US, "%.1f", totalKg)} kg",
                        trend = t("adminTrendThisWeek"),
                        valueColor = EcoPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        label = t("adminTransactions"),
                        value = "${recentConfirmed.size}",
                        trend = t("adminTrendThisWeek"),
                        valueColor = EcoForeground,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        label = t("adminPointsOut"),
                        value = FlysticTranslations.formatNumber(totalPoints, locale),
                        trend = t("adminTrendThisWeek"),
                        valueColor = EcoOcean,
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        label = t("adminPending"),
                        value = "$pendingCount",
                        trend = t("adminTrendActive"),
                        valueColor = EcoCoral,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Plastic breakdown card
        item {
            EcoCard(contentPadding = 18.dp) {
                Text(
                    text = t("adminPlasticBreakdown"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    byType.forEach { (type, kg) ->
                        val ratio = (kg / maxTypeKg).toFloat().coerceIn(0f, 1f)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${type.emoji} ${type.code}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoForeground
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", kg)} kg",
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

        // Station capacity & handover
        item {
            val highFill = stationFillPercent > 80
            EcoCard(contentPadding = 18.dp) {
                Text(
                    text = t("adminCapacity"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedStationName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoForeground,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$stationFillPercent%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (highFill) EcoCoral else EcoMutedForeground
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(EcoSecondary)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((stationFillPercent / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (highFill) EcoCoral else EcoPrimary)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = t(
                        "adminStationFillDetail",
                        mapOf(
                            "filled" to String.format(Locale.US, "%.2f", stationFillKg),
                            "capacity" to station.capacityKg.toInt(),
                            "percent" to stationFillPercent
                        )
                    ),
                    fontSize = 12.sp,
                    color = EcoMutedForeground
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        if (!isHandoverLoading) {
                            scope.launch {
                                isHandoverLoading = true
                                val result = runCatching {
                                    onEmptyCollectionPoint(station.canonicalName)
                                }.getOrNull()
                                handoverMessage = if (result != null) {
                                    t(
                                        "adminStationEmptySuccess",
                                        mapOf("weight" to String.format(Locale.US, "%.2f", result.weightKg))
                                    )
                                } else {
                                    t("adminStationEmptyEmpty")
                                }
                                isHandoverLoading = false
                            }
                        }
                    },
                    enabled = !isHandoverLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = t("adminStationEmpty"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = handoverMessage ?: t("adminStationEmptyHint"),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = if (handoverMessage != null) EcoPrimary else EcoMutedForeground
                )
            }
        }

        // Recent transactions header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t("adminRecent"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                TextButton(onClick = onOpenTransactions) {
                    Text(
                        text = t("adminSeeAll"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoPrimary
                    )
                }
            }
        }

        val recentSix = stationTransactions.take(6)
        if (recentSix.isEmpty()) {
            item {
                EcoCard(contentPadding = 20.dp) {
                    Text(
                        text = t("adminNoData"),
                        fontSize = 14.sp,
                        color = EcoMutedForeground
                    )
                }
            }
        } else {
            items(recentSix, key = { it.id }) { tx ->
                val status = effectiveTransactionStatus(tx, now)
                EcoCard(contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EcoSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "♻️", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${tx.userName ?: t("adminAwaitingStudent")} · ${String.format(Locale.US, "%.2f", tx.totalQuantity)} kg",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = tx.items.joinToString(" · ") {
                                    "${it.plasticType.code}×${it.quantity} kg"
                                },
                                fontSize = 12.sp,
                                color = EcoMutedForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            StatusPill(status = status, locale = locale)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "+${tx.points}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 10.dp),
                        color = EcoBorder.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = FlysticTranslations.localizedCollectionPoint(tx.collectionPoint, locale).first,
                        fontSize = 11.sp,
                        color = EcoMutedForeground
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    label: String,
    value: String,
    trend: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    EcoCard(
        modifier = modifier,
        contentPadding = 16.dp
    ) {
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
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = trend,
            fontSize = 11.sp,
            color = EcoMutedForeground
        )
    }
}
