package com.verifour.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.verifour.app.data.FlysticApiException
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.Cleanliness
import com.verifour.app.model.FlysticState
import com.verifour.app.model.PetAnimationEvent
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PlasticType
import com.verifour.app.model.PointDestination
import com.verifour.app.model.Role
import com.verifour.app.model.Transaction
import com.verifour.app.model.TransactionStatus
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.model.effectiveTransactionStatus
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.PixelPetScanCelebration
import com.verifour.app.ui.theme.EcoBackground
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCard
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSecondary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ScanScreen(
    state: FlysticState,
    isRemote: Boolean,
    petState: PetCompanionState,
    personalization: UserPersonalizationState,
    petEvent: PetAnimationEvent,
    petEventTrigger: Int,
    petEventNewLevel: Int,
    onUpgradePet: () -> Unit,
    onGetTransaction: suspend (String) -> Transaction?,
    onConfirmTransaction: suspend (String, PointDestination, Transaction?) -> Transaction?,
    onBackHome: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var tokenInput by remember { mutableStateOf("") }
    var previewTx by remember { mutableStateOf<Transaction?>(null) }
    var destination by remember { mutableStateOf<PointDestination?>(null) }
    var successResult by remember { mutableStateOf<Triple<Int, Double, PlasticType?>?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isConfirming by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var lastDetectedToken by remember { mutableStateOf("") }

    fun normalizeToken(raw: String): String = raw.trim().uppercase(Locale.US)

    fun lookupToken(raw: String) {
        val normalized = normalizeToken(raw)
        tokenInput = normalized
        errorMsg = null
        if (normalized.isBlank()) {
            previewTx = null
            destination = null
            return
        }
        if (state.user?.role != Role.STUDENT) {
            errorMsg = t("scanStudentRequired")
            previewTx = null
            return
        }
        scope.launch {
            try {
                val tx = onGetTransaction(normalized)
                if (tx == null) {
                    errorMsg = t("scanInvalid")
                    previewTx = null
                    destination = null
                    return@launch
                }
                when (effectiveTransactionStatus(tx)) {
                    TransactionStatus.CONFIRMED -> {
                        errorMsg = t("scanUsed")
                        previewTx = null
                    }
                    TransactionStatus.EXPIRED -> {
                        errorMsg = t("scanExpired")
                        previewTx = null
                    }
                    TransactionStatus.CANCELLED -> {
                        errorMsg = t("scanCancelled")
                        previewTx = null
                    }
                    TransactionStatus.PENDING -> {
                        previewTx = tx
                        destination = null
                        isScanning = false
                    }
                }
            } catch (_: Exception) {
                errorMsg = t("scanInvalid")
                previewTx = null
                destination = null
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            isScanning = true
        } else {
            permissionDenied = true
            isScanning = false
            errorMsg = t("scanCameraDenied")
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        errorMsg = null
        runCatching {
            val image = InputImage.fromFilePath(context, uri)
            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull()?.rawValue
                    if (raw != null) {
                        lookupToken(raw)
                    } else {
                        errorMsg = t("scanImageError")
                    }
                }
                .addOnFailureListener {
                    errorMsg = t("scanImageError")
                }
        }.onFailure {
            errorMsg = t("scanImageError")
        }
    }

    if (successResult != null) {
        val (pts, weightKg, mainPlastic) = successResult!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PixelPetScanCelebration(
                locale = locale,
                pointsEarned = pts,
                studentPoints = state.points,
                petState = petState,
                petEvent = petEvent,
                petEventTrigger = petEventTrigger,
                petEventNewLevel = petEventNewLevel,
                onUpgradePet = onUpgradePet,
                celebratedPlastic = mainPlastic,
                personalization = personalization
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = t("scanSuccessTitle"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = t("scanSuccessThanks"),
                fontSize = 14.sp,
                color = EcoMutedForeground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = EcoForeground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = t("scanSuccessYouEarned").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoPrimaryGlow.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "+$pts",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EcoPrimaryGlow
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = t(
                            "scanSuccessPoints",
                            mapOf("count" to String.format(Locale.US, "%.2f", weightKg))
                        ),
                        fontSize = 13.sp,
                        color = EcoBackground.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        successResult = null
                        lastDetectedToken = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(t("scanScanAgain"), fontWeight = FontWeight.Bold, color = EcoForeground)
                }
                Button(
                    onClick = onBackHome,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                ) {
                    Text(t("scanBackHome"), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = t("scanTitle"),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = EcoForeground
        )
        Text(
            text = t("scanDescription"),
            fontSize = 14.sp,
            color = EcoMutedForeground
        )

        // Camera Viewfinder Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(EcoForeground.copy(alpha = 0.92f))
                .border(2.dp, EcoPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                CameraQrPreview(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    onQrDetected = { raw ->
                        val normalized = normalizeToken(raw)
                        if (normalized.isNotBlank() && normalized != lastDetectedToken) {
                            lastDetectedToken = normalized
                            lookupToken(normalized)
                        }
                    }
                )
            } else {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = EcoPrimaryGlow,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (permissionDenied) t("scanCameraDenied") else t("scanCameraPermissionDescription"),
                        color = EcoBackground.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Camera Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        errorMsg = null
                        lastDetectedToken = ""
                        isScanning = true
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                enabled = !isScanning,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics(mergeDescendants = true) {},
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(t("scanCameraPermissionButton"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { isScanning = false },
                enabled = isScanning,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(t("scanStop"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isScanning) {
            Text(
                text = t("scanCameraActive"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = EcoPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Manual token input
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = t("scanManualLabel").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EcoMutedForeground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = {
                        tokenInput = normalizeToken(it)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(t("scanTokenPlaceholder"), fontSize = 13.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EcoCard,
                        unfocusedContainerColor = EcoCard,
                        focusedBorderColor = EcoPrimary,
                        unfocusedBorderColor = EcoBorder
                    )
                )
                Button(
                    onClick = { lookupToken(tokenInput) },
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoForeground)
                ) {
                    Text(t("scanLookupButton"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Scan QR from gallery image
        OutlinedButton(
            onClick = { galleryPickerLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics(mergeDescendants = true) {},
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, EcoBorder)
        ) {
            Icon(Icons.Rounded.Image, contentDescription = null, tint = EcoMutedForeground, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(t("scanUploadLabel"), color = EcoMutedForeground, fontWeight = FontWeight.SemiBold)
        }

        // In Demo Mode, show pending transactions created on this device for easy testing
        if (!isRemote) {
            val pendingDemo = state.transactions.filter {
                effectiveTransactionStatus(it) == TransactionStatus.PENDING
            }
            if (pendingDemo.isNotEmpty()) {
                EcoCard(containerColor = EcoSecondary.copy(alpha = 0.7f), contentPadding = 14.dp) {
                    Text(
                        text = t("scanDemoPending"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Text(
                        text = t("scanDemoHint"),
                        fontSize = 12.sp,
                        color = EcoMutedForeground
                    )
                    Spacer(Modifier.height(8.dp))
                    pendingDemo.forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, EcoBorder, RoundedCornerShape(12.dp))
                                .clickable { lookupToken(tx.token) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tx.token,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = EcoPrimary
                            )
                            Text(
                                text = "+${tx.points} (${tx.weightKg} kg)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoForeground
                            )
                        }
                    }
                }
            }
        }

        if (errorMsg != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EcoCoral.copy(alpha = 0.14f))
                    .padding(12.dp)
            ) {
                Text(
                    text = errorMsg.orEmpty(),
                    color = EcoCoral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Transaction confirmation preview card
        previewTx?.let { tx ->
            val cleanShort = { c: Cleanliness ->
                when (c) {
                    Cleanliness.CLEAN -> t("cleanShortClean")
                    Cleanliness.NORMAL -> t("cleanShortNormal")
                    Cleanliness.DIRTY -> t("cleanShortDirty")
                }
            }

            EcoCard(borderColor = EcoPrimary.copy(alpha = 0.4f), contentPadding = 18.dp) {
                Text(
                    text = t("scanConfirmTitle").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoMutedForeground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = FlysticTranslations.localizedCollectionPoint(tx.collectionPoint, locale).first,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(12.dp))
                tx.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EcoSecondary.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.plasticType.emoji} ${item.plasticType.code} × ${item.quantity} kg · ${cleanShort(item.cleanliness)}",
                            fontSize = 13.sp,
                            color = EcoForeground
                        )
                        Text(
                            text = "+${item.points}",
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = EcoBorder)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t("scanConfirmTotal"), fontSize = 14.sp, color = EcoMutedForeground)
                    Text(
                        text = "+${tx.points}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EcoPrimary
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = EcoBorder)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = t("scanAllocationTitle"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t("scanAllocationDescription"),
                    fontSize = 12.sp,
                    color = EcoMutedForeground,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(10.dp))

                AllocationOptionCard(
                    selected = destination == PointDestination.REWARDS,
                    title = t("scanRewardsOption"),
                    description = t("scanRewardsOptionDescription"),
                    onClick = {
                        destination = PointDestination.REWARDS
                        errorMsg = null
                    }
                )
                Spacer(Modifier.height(8.dp))
                AllocationOptionCard(
                    selected = destination == PointDestination.FUNDRAISING,
                    title = t("scanFundraisingOption"),
                    description = t("scanFundraisingOptionDescription"),
                    onClick = {
                        destination = PointDestination.FUNDRAISING
                        errorMsg = null
                    }
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val dest = destination
                        if (dest == null) {
                            errorMsg = t("scanSelectAllocation")
                            return@Button
                        }
                        isConfirming = true
                        errorMsg = null
                        scope.launch {
                            try {
                                val confirmed = onConfirmTransaction(tx.token, dest, tx)
                                if (confirmed != null) {
                                    val mainPlastic = confirmed.items.firstOrNull()?.plasticType
                                        ?: tx.items.firstOrNull()?.plasticType
                                    successResult = Triple(confirmed.points, confirmed.totalQuantity, mainPlastic)
                                    previewTx = null
                                    destination = null
                                    tokenInput = ""
                                    isScanning = false
                                } else {
                                    errorMsg = t("scanConfirmFail")
                                }
                            } catch (e: FlysticApiException) {
                                errorMsg = when (e.statusCode) {
                                    401 -> t("scanSessionExpired")
                                    403 -> t("scanStudentRequired")
                                    409 -> if (e.message.contains("expired", ignoreCase = true)) {
                                        t("scanExpired")
                                    } else {
                                        t("scanUsed")
                                    }
                                    else -> e.message
                                }
                            } catch (e: Exception) {
                                errorMsg = e.message ?: t("scanConfirmFail")
                            } finally {
                                isConfirming = false
                            }
                        }
                    },
                    enabled = !isConfirming,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                ) {
                    Text(
                        text = if (isConfirming) t("authSubmitting") else t("scanConfirmButton"),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraQrPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    DisposableEffect(lifecycleOwner) {
        val executor = ContextCompat.getMainExecutor(context)
        cameraController.setImageAnalysisAnalyzer(
            executor,
            MlKitAnalyzer(
                listOf(barcodeScanner),
                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                executor
            ) { result ->
                val barcodes = result?.getValue(barcodeScanner)
                val value = barcodes?.firstOrNull()?.rawValue
                if (!value.isNullOrBlank()) {
                    onQrDetected(value)
                }
            }
        )
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            barcodeScanner.close()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller = cameraController
            }
        }
    )
}

@Composable
private fun AllocationOptionCard(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) EcoPrimary.copy(alpha = 0.1f) else EcoSecondary.copy(alpha = 0.55f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) EcoPrimary else EcoBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EcoForeground
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = EcoMutedForeground,
            lineHeight = 16.sp
        )
    }
}
