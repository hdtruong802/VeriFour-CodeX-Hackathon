package com.verifour.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.R
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.FlysticState
import com.verifour.app.model.getStationFillKg
import com.verifour.app.model.getStationFillPercent
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCard
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    state: FlysticState,
    isRemote: Boolean,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    fun updateZoom(delta: Float) {
        val next = (zoom + delta).coerceIn(1f, 2.5f)
        zoom = next
        if (next == 1f) pan = Offset.Zero
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
        if (showBackButton) {
            item {
                Text(
                    text = t("backHome"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoMutedForeground,
                    modifier = Modifier.clickable { onBack() }
                )
            }
        }

        item {
            Text(
                text = t("mapTitle"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = t("mapDescription"),
                fontSize = 14.sp,
                color = EcoMutedForeground
            )
        }

        // Interactive Campus Map Frame
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = EcoCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(48f / 41f)
                        .clip(RoundedCornerShape(24.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, panChange, zoomChange, _ ->
                                val nextZoom = (zoom * zoomChange).coerceIn(1f, 2.5f)
                                val maxOffsetX = (size.width * (nextZoom - 1f)) / 2f
                                val maxOffsetY = (size.height * (nextZoom - 1f)) / 2f
                                zoom = nextZoom
                                pan = if (nextZoom <= 1.01f) {
                                    Offset.Zero
                                } else {
                                    Offset(
                                        x = (pan.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                                        y = (pan.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                }
                            }
                        }
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoom,
                                scaleY = zoom,
                                translationX = pan.x,
                                translationY = pan.y
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.hust_map),
                            contentDescription = t("mapUniversity"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        COLLECTION_POINTS.forEachIndexed { index, point ->
                            val xPx = (point.mapXFraction * widthPx).roundToInt()
                            val yPx = (point.mapYFraction * heightPx).roundToInt()
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = xPx - 18.dp.roundToPx(),
                                            y = yPx - 36.dp.roundToPx()
                                        )
                                    }
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EcoPrimary)
                                    .border(2.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Top-right badge
                    Surface(
                        shape = CircleShape,
                        color = EcoCard.copy(alpha = 0.94f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = t("mapPins", mapOf("count" to COLLECTION_POINTS.size)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Bottom-left university label
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EcoCard.copy(alpha = 0.94f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = t("mapUniversity"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground
                            )
                            Text(
                                text = t("mapPins", mapOf("count" to COLLECTION_POINTS.size)),
                                fontSize = 10.sp,
                                color = EcoMutedForeground
                            )
                        }
                    }

                    // Bottom-right zoom controls
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EcoCard.copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Column {
                            IconButton(
                                onClick = { updateZoom(0.35f) },
                                enabled = zoom < 2.5f,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = t("mapZoomIn"),
                                    tint = EcoForeground
                                )
                            }
                            HorizontalDivider(color = EcoBorder, modifier = Modifier.width(36.dp))
                            IconButton(
                                onClick = { updateZoom(-0.35f) },
                                enabled = zoom > 1f,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = t("mapZoomOut"),
                                    tint = EcoForeground
                                )
                            }
                        }
                    }
                }
            }
        }

        // Station list
        itemsIndexed(COLLECTION_POINTS) { index, point ->
            val remoteStation = state.stations.firstOrNull { it.id == point.id }
            val fillPercent = if (isRemote) {
                remoteStation?.fillPercent ?: 0
            } else {
                getStationFillPercent(state.transactions, state.collectionRuns, point)
            }
            val filledKg = if (isRemote) {
                remoteStation?.filledKg ?: 0.0
            } else {
                getStationFillKg(state.transactions, state.collectionRuns, point.canonicalName)
            }
            val (name, address) = FlysticTranslations.localizedCollectionPoint(point.canonicalName, locale)

            EcoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EcoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                        Text(
                            text = address,
                            fontSize = 12.sp,
                            color = EcoMutedForeground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = t(
                                "mapStorageCurrent",
                                mapOf(
                                    "filled" to String.format(Locale.US, "%.1f", filledKg),
                                    "capacity" to point.capacityKg.toInt()
                                )
                            ),
                            fontSize = 11.sp,
                            color = EcoMutedForeground
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$fillPercent%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fillPercent > 80) EcoCoral else EcoPrimary
                        )
                        Text(
                            text = t("mapCapacity"),
                            fontSize = 10.sp,
                            color = EcoMutedForeground
                        )
                    }
                }
            }
        }
    }
}
