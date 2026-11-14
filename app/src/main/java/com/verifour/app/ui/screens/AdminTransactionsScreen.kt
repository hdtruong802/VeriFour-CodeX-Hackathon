package com.verifour.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.Cleanliness
import com.verifour.app.model.FlysticState
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.StatusPill
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private enum class StatusFilter {
    ALL,
    CONFIRMED,
    PENDING,
    EXPIRED,
    CANCELLED
}

@Composable
fun AdminTransactionsScreen(
    state: FlysticState,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var selectedStatus by remember { mutableStateOf(StatusFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val stationName = state.user?.collectionPoint ?: COLLECTION_POINTS.first().canonicalName
    val stationTransactions = state.transactions.filter { it.collectionPoint == stationName }
    val now = System.currentTimeMillis()

    val filtered = stationTransactions.filter { tx ->
        val txStatus = effectiveTransactionStatus(tx, now)
        val matchesStatus = when (selectedStatus) {
            StatusFilter.ALL -> true
            StatusFilter.CONFIRMED -> txStatus == TransactionStatus.CONFIRMED
            StatusFilter.PENDING -> txStatus == TransactionStatus.PENDING
            StatusFilter.EXPIRED -> txStatus == TransactionStatus.EXPIRED
            StatusFilter.CANCELLED -> txStatus == TransactionStatus.CANCELLED
        }
        val query = searchQuery.trim().lowercase(Locale.getDefault())
        val matchesQuery = query.isEmpty() ||
            (tx.userName ?: "").lowercase(Locale.getDefault()).contains(query) ||
            tx.collectionPoint.lowercase(Locale.getDefault()).contains(query) ||
            tx.token.lowercase(Locale.getDefault()).contains(query)
        matchesStatus && matchesQuery
    }

    val dateFormatter = remember(locale) {
        val jLoc = when (locale) {
            AppLocale.VI -> Locale.forLanguageTag("vi-VN")
            AppLocale.EN -> Locale.US
            AppLocale.KO -> Locale.KOREA
        }
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, jLoc)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = t("adminTransactionTitle"),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t("adminTransactionDescription"),
                    fontSize = 13.sp,
                    color = EcoMutedForeground
                )
            }
        }

        // Status filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterChips = listOf(
                    StatusFilter.ALL to t("adminStatusAll"),
                    StatusFilter.CONFIRMED to t("adminTransactionConfirmed"),
                    StatusFilter.PENDING to t("adminTransactionPending"),
                    StatusFilter.EXPIRED to t("adminTransactionExpired"),
                    StatusFilter.CANCELLED to t("adminTransactionCancelled")
                )
                filterChips.forEach { (filter, label) ->
                    val selected = selectedStatus == filter
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { selectedStatus = filter },
                        shape = CircleShape,
                        color = if (selected) EcoForeground else Color.White,
                        border = BorderStroke(1.dp, if (selected) EcoForeground else EcoBorder)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else EcoForeground,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Search input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = t("adminSearchPlaceholder"),
                        fontSize = 13.sp,
                        color = EcoMutedForeground
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = EcoMutedForeground
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
        }

        if (filtered.isEmpty()) {
            item {
                EcoCard(contentPadding = 28.dp) {
                    Text(
                        text = t("adminNoFilterResults"),
                        fontSize = 14.sp,
                        color = EcoMutedForeground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                val status = effectiveTransactionStatus(tx, now)
                EcoCard(contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.userName ?: "— ${t("adminAwaitingStudent")}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.userName != null) EcoForeground else EcoMutedForeground
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = dateFormatter.format(Date(tx.createdAt)),
                                fontSize = 11.sp,
                                color = EcoMutedForeground
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "+${tx.points}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = FlysticTranslations.localizedCollectionPoint(
                                tx.collectionPoint,
                                locale
                            ).first,
                            fontSize = 12.sp,
                            color = EcoMutedForeground,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusPill(status = status, locale = locale)
                    }

                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tx.items.forEach { item ->
                            val cleanText = when (item.cleanliness) {
                                Cleanliness.CLEAN -> t("cleanShortClean")
                                Cleanliness.NORMAL -> t("cleanShortNormal")
                                Cleanliness.DIRTY -> t("cleanShortDirty")
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EcoSecondary)
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${item.plasticType.emoji} ${item.plasticType.code} × ${item.quantity} kg · $cleanText",
                                    fontSize = 11.sp,
                                    color = EcoForeground
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = t(
                    "adminFooterShowing",
                    mapOf("filtered" to filtered.size, "total" to stationTransactions.size)
                ),
                fontSize = 12.sp,
                color = EcoMutedForeground
            )
        }
    }
}
