package com.verifour.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.Cleanliness
import com.verifour.app.model.FlysticState
import com.verifour.app.model.PlasticType
import com.verifour.app.model.Transaction
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.calculatePoints
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.model.round2
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.QrCodeBitmapImage
import com.verifour.app.ui.components.StatusPill
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoLeaf
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private data class AdminTxLine(
    val plasticType: PlasticType = PlasticType.PET,
    val quantity: Double = 1.0,
    val quantityText: String = "1.0",
    val cleanliness: Cleanliness = Cleanliness.CLEAN
)

@Composable
fun AdminNewTransactionScreen(
    state: FlysticState,
    isRemote: Boolean,
    onCreateTransaction: suspend (List<Triple<PlasticType, Double, Cleanliness>>, String) -> Transaction,
    onGetTransaction: suspend (String) -> Transaction?,
    onCancelTransaction: suspend (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    val defaultStation = state.user?.collectionPoint ?: COLLECTION_POINTS.first().canonicalName
    var collectionPoint by remember(defaultStation) { mutableStateOf(defaultStation) }
    var stationMenuOpen by remember { mutableStateOf(false) }

    var lines by remember { mutableStateOf(listOf(AdminTxLine())) }
    var activeTx by remember { mutableStateOf<Transaction?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var copied by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    // 1-second ticker for countdown timer
    LaunchedEffect(activeTx?.id) {
        while (activeTx != null) {
            now = System.currentTimeMillis()
            delay(1.seconds)
        }
    }

    // Sync local state updates (e.g. Demo mode confirmation)
    LaunchedEffect(state.transactions, activeTx?.id) {
        val current = activeTx ?: return@LaunchedEffect
        val fresh = state.transactions.firstOrNull { it.id == current.id }
        if (fresh != null && fresh.status != current.status) {
            activeTx = fresh
        }
    }

    // Remote polling every 3s while pending
    LaunchedEffect(isRemote, activeTx?.id, activeTx?.status) {
        val current = activeTx ?: return@LaunchedEffect
        if (!isRemote || effectiveTransactionStatus(current) != TransactionStatus.PENDING) return@LaunchedEffect
        while (true) {
            delay(3.seconds)
            val updated = runCatching { onGetTransaction(current.token) }.getOrNull()
            if (updated != null) {
                activeTx = updated
                if (effectiveTransactionStatus(updated) != TransactionStatus.PENDING) {
                    break
                }
            }
        }
    }

    fun resetActive() {
        val current = activeTx
        if (current != null && effectiveTransactionStatus(current) == TransactionStatus.PENDING) {
            scope.launch {
                runCatching { onCancelTransaction(current.id) }
            }
        }
        activeTx = null
        copied = false
    }

    if (activeTx != null) {
        val active = activeTx!!
        val remainingSec = max(0L, (active.expiresAt - now) / 1000L)
        val mins = remainingSec / 60L
        val secs = remainingSec % 60L
        val currentStatus = effectiveTransactionStatus(active, now)
        val confirmed = currentStatus == TransactionStatus.CONFIRMED
        val expired = currentStatus == TransactionStatus.EXPIRED

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "← ${t("adminCreateAnotherTransaction")}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoMutedForeground,
                    modifier = Modifier.clickable { resetActive() }
                )
            }

            // QR Card
            item {
                EcoCard(contentPadding = 24.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = t("adminQrHeader").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = EcoMutedForeground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(
                                    3.dp,
                                    EcoForeground.copy(alpha = 0.1f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(14.dp)
                        ) {
                            QrCodeBitmapImage(content = active.token, sizeDp = 220.dp)
                        }

                        if (!confirmed && !expired) {
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                fontSize = 26.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground
                            )
                            Text(
                                text = t("adminTokenExpiresIn"),
                                fontSize = 12.sp,
                                color = EcoMutedForeground
                            )
                        } else if (expired) {
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = t("adminTokenExpired"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoCoral
                            )
                        } else {
                            Spacer(Modifier.height(18.dp))
                            Text(text = "✅", fontSize = 36.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = t("adminStudentConfirmed"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary
                            )
                            val confirmedTime = active.confirmedAt?.let {
                                DateFormat.getTimeInstance(
                                    DateFormat.MEDIUM,
                                    localeToJava(locale)
                                ).format(Date(it))
                            }.orEmpty()
                            Text(
                                text = listOfNotNull(active.userName, confirmedTime.takeIf { it.isNotBlank() })
                                    .joinToString(" · "),
                                fontSize = 13.sp,
                                color = EcoMutedForeground
                            )
                        }
                    }
                }
            }

            // Transaction Details Card
            item {
                EcoCard(contentPadding = 18.dp) {
                    Text(
                        text = t("adminTransactionDetails"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(14.dp))

                    // Token + Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("adminTransactionToken"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EcoSecondary)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = active.token,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoForeground
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, EcoBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                                        clipboard?.setPrimaryClip(
                                            ClipData.newPlainText("VeriFour Token", active.token)
                                        )
                                        copied = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                        contentDescription = null,
                                        tint = if (copied) EcoPrimary else EcoForeground,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = t(if (copied) "copied" else "copy"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (copied) EcoPrimary else EcoForeground
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    DetailRow(
                        label = t("adminTransactionStation"),
                        value = FlysticTranslations.localizedCollectionPoint(
                            active.collectionPoint,
                            locale
                        ).first
                    )
                    Spacer(Modifier.height(8.dp))
                    DetailRow(
                        label = t("adminTransactionTypeCount"),
                        value = "${active.items.size} ${t("adminTypes")} · ${String.format(Locale.US, "%.2f", active.totalQuantity)} kg"
                    )
                    Spacer(Modifier.height(8.dp))
                    DetailRow(
                        label = t("adminTransactionWeightEstimate"),
                        value = "${String.format(Locale.US, "%.2f", active.weightKg)} kg"
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = t("adminTransactionTotalPoints"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground
                        )
                        Text(
                            text = "+${active.points}",
                            fontSize = 15.sp,
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
                            text = t("adminTransactionStatus"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground
                        )
                        StatusPill(status = currentStatus, locale = locale)
                    }

                    Spacer(Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EcoSecondary.copy(alpha = 0.6f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        active.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.plasticType.emoji} ${item.plasticType.code} × ${item.quantity} kg · ${cleanLabel(item.cleanliness, locale)}",
                                    fontSize = 12.sp,
                                    color = EcoForeground
                                )
                                Text(
                                    text = "+${item.points}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoPrimary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    if (!confirmed) {
                        OutlinedButton(
                            onClick = { resetActive() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, EcoCoral.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = EcoCoral.copy(alpha = 0.1f),
                                contentColor = EcoCoral
                            )
                        ) {
                            Text(
                                text = t("adminCancelTransaction"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { resetActive() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EcoPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = t("adminCreateAnotherTransaction"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val lineTotals = lines.map { calculatePoints(it.quantity, it.plasticType, it.cleanliness) }
    val totalPoints = lineTotals.sum()
    val totalQty = round2(lines.sumOf { it.quantity })
    val canSubmit = totalQty > 0.0 && lines.all { it.quantity > 0.0 }

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
        item {
            Column {
                Text(
                    text = t("adminNewTransactionTitle"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t("adminNewTransactionDescription"),
                    fontSize = 13.sp,
                    color = EcoMutedForeground
                )
            }
        }

        // Collection Point selector
        item {
            EcoCard(contentPadding = 16.dp) {
                Text(
                    text = t("adminCollectionPoint").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoMutedForeground
                )
                Spacer(Modifier.height(8.dp))
                Box {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EcoSecondary,
                        border = BorderStroke(1.dp, EcoBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { stationMenuOpen = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FlysticTranslations.localizedCollectionPoint(
                                    collectionPoint,
                                    locale
                                ).first,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = EcoForeground,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = null,
                                tint = EcoMutedForeground
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = stationMenuOpen,
                        onDismissRequest = { stationMenuOpen = false }
                    ) {
                        COLLECTION_POINTS.forEach { point ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = FlysticTranslations.localizedCollectionPoint(
                                            point.canonicalName,
                                            locale
                                        ).first
                                    )
                                },
                                onClick = {
                                    collectionPoint = point.canonicalName
                                    stationMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Plastic line builders
        itemsIndexed(lines) { index, line ->
            val pts = lineTotals[index]
            EcoCard(contentPadding = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("adminLineNumber", mapOf("number" to index + 1)).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoMutedForeground
                    )
                    TextButton(
                        onClick = {
                            if (lines.size > 1) {
                                lines = lines.filterIndexed { i, _ -> i != index }
                            }
                        },
                        enabled = lines.size > 1,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = t("adminDeleteLine"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lines.size > 1) EcoCoral else EcoMutedForeground.copy(alpha = 0.35f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 5 Plastic types
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlasticType.entries.forEach { type ->
                        val selected = line.plasticType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    lines = lines.mapIndexed { i, row ->
                                        if (i == index) row.copy(plasticType = type) else row
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) EcoPrimary.copy(alpha = 0.12f) else EcoSecondary,
                            border = BorderStroke(
                                1.dp,
                                if (selected) EcoPrimary else EcoBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = type.emoji, fontSize = 18.sp)
                                Text(
                                    text = type.code,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) EcoPrimary else EcoForeground
                                )
                                Text(
                                    text = "${type.basePoints}",
                                    fontSize = 10.sp,
                                    color = EcoMutedForeground
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Quantity stepper
                Text(
                    text = t("adminQuantityUnits").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoMutedForeground
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EcoSecondary,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val nextVal = max(0.0, round2(line.quantity - 0.1))
                                lines = lines.mapIndexed { i, row ->
                                    if (i == index) {
                                        row.copy(
                                            quantity = nextVal,
                                            quantityText = String.format(Locale.US, "%.2f", nextVal)
                                                .trimEnd('0').trimEnd('.')
                                                .ifBlank { "0" }
                                        )
                                    } else row
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Remove, contentDescription = null, tint = EcoForeground)
                        }
                    }

                    OutlinedTextField(
                        value = line.quantityText,
                        onValueChange = { raw ->
                            val sanitized = raw.replace(',', '.')
                            val parsed = sanitized.toDoubleOrNull()
                            val clamped = if (parsed != null) min(200.0, max(0.0, round2(parsed))) else 0.0
                            lines = lines.mapIndexed { i, row ->
                                if (i == index) {
                                    row.copy(
                                        quantityText = sanitized,
                                        quantity = clamped
                                    )
                                } else row
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = EcoSecondary,
                            unfocusedContainerColor = EcoSecondary,
                            focusedBorderColor = EcoPrimary,
                            unfocusedBorderColor = EcoBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EcoSecondary,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val nextVal = min(200.0, round2(line.quantity + 0.1))
                                lines = lines.mapIndexed { i, row ->
                                    if (i == index) {
                                        row.copy(
                                            quantity = nextVal,
                                            quantityText = String.format(Locale.US, "%.2f", nextVal)
                                                .trimEnd('0').trimEnd('.')
                                                .ifBlank { "0" }
                                        )
                                    } else row
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = EcoForeground)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Cleanliness selector
                Text(
                    text = t("adminCondition").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoMutedForeground
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Cleanliness.entries.forEach { cond ->
                        val selected = line.cleanliness == cond
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    lines = lines.mapIndexed { i, row ->
                                        if (i == index) row.copy(cleanliness = cond) else row
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) EcoPrimary else EcoSecondary,
                            border = BorderStroke(
                                1.dp,
                                if (selected) EcoPrimary else EcoBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = cleanLabel(cond, locale),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else EcoForeground,
                                    maxLines = 1
                                )
                                Text(
                                    text = "×${cond.multiplier}",
                                    fontSize = 10.sp,
                                    color = if (selected) Color.White.copy(alpha = 0.85f) else EcoMutedForeground
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EcoPrimary.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${line.quantity} kg × ${line.plasticType.basePoints} × ${line.cleanliness.multiplier}",
                        fontSize = 12.sp,
                        color = EcoMutedForeground
                    )
                    Text(
                        text = "+$pts ${t("studentPoints")}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary
                    )
                }
            }
        }

        // Add another line
        item {
            OutlinedButton(
                onClick = {
                    val used = lines.map { it.plasticType }.toSet()
                    val nextType = PlasticType.entries.firstOrNull { it !in used } ?: PlasticType.PET
                    lines = lines + AdminTxLine(plasticType = nextType)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, EcoBorder)
            ) {
                Text(
                    text = t("adminAddOtherType"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoPrimary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Estimated total + submit
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                EcoPrimary.copy(alpha = 0.14f),
                                EcoLeaf.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(1.dp, EcoPrimary.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = t("adminEstimatedPointsHeader").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "+$totalPoints",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EcoForeground
                    )
                    Text(
                        text = "${lines.size} ${t("adminTypes")} · ${String.format(Locale.US, "%.2f", totalQty)} kg",
                        fontSize = 12.sp,
                        color = EcoMutedForeground
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (canSubmit && !isCreating) {
                        scope.launch {
                            createError = null
                            isCreating = true
                            val items = lines.map {
                                Triple(it.plasticType, it.quantity, it.cleanliness)
                            }
                            runCatching {
                                onCreateTransaction(items, collectionPoint)
                            }.onSuccess { tx ->
                                activeTx = tx
                            }.onFailure { err ->
                                createError = err.message ?: t("scanConfirmFail")
                            }
                            isCreating = false
                        }
                    }
                },
                enabled = canSubmit && !isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoForeground,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isCreating) t("authSubmitting") else t("adminQrCreateButton"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (createError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = createError!!,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = EcoCoral,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = t("adminQrExpirationNote"),
                fontSize = 12.sp,
                color = EcoMutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            EcoCard(
                containerColor = EcoSecondary.copy(alpha = 0.6f),
                contentPadding = 14.dp
            ) {
                Text(
                    text = t("adminPointsFormulaLabel"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t("adminPointsFormulaDetail"),
                    fontSize = 12.sp,
                    color = EcoMutedForeground
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = EcoMutedForeground
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoForeground,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun cleanLabel(cleanliness: Cleanliness, locale: AppLocale): String {
    return when (cleanliness) {
        Cleanliness.CLEAN -> FlysticTranslations.t("cleanShortClean", locale)
        Cleanliness.NORMAL -> FlysticTranslations.t("cleanShortNormal", locale)
        Cleanliness.DIRTY -> FlysticTranslations.t("cleanShortDirty", locale)
    }
}

private fun localeToJava(locale: AppLocale): Locale = when (locale) {
    AppLocale.VI -> Locale.forLanguageTag("vi-VN")
    AppLocale.EN -> Locale.US
    AppLocale.KO -> Locale.KOREA
}
