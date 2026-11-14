package com.verifour.app.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.R
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.CampaignParticipant
import com.verifour.app.model.CampaignPhoto
import com.verifour.app.model.EcoCampaign
import com.verifour.app.model.FlysticState
import com.verifour.app.model.PetCompanionState
import com.verifour.app.model.PetProgression
import com.verifour.app.model.PetSkin
import com.verifour.app.model.PetSpecies
import com.verifour.app.model.PetStage
import com.verifour.app.model.PlasticType
import com.verifour.app.model.UserPersonalizationState
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.PixelAnimatedCityStage
import com.verifour.app.ui.components.PixelPetSprite
import com.verifour.app.ui.components.PixelPlasticMascotSprite
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCard
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoLeaf
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoPrimaryGlow
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreen(
    state: FlysticState,
    petState: PetCompanionState,
    personalization: UserPersonalizationState,
    campaigns: List<EcoCampaign>,
    onOpenCampaign: (title: String, description: String, targetKg: Double) -> EcoCampaign,
    onJoinCampaign: (codeOrLink: String) -> Boolean,
    onInviteMascot: (
        campaignId: String,
        friendName: String,
        mascotName: String,
        species: PetSpecies,
        skin: PetSkin,
        plasticMascot: PlasticType?
    ) -> Unit,
    onAddPhoto: (
        campaignId: String,
        caption: String,
        plasticType: PlasticType,
        kg: Double,
        localImagePath: String?,
        samplePhotoKey: String?
    ) -> Unit,
    onSaveBitmap: (Bitmap) -> String?,
    onSaveUri: (Uri) -> String?,
    onConcludeCampaign: (String) -> Unit,
    onReopenCampaign: (String) -> Unit,
    onPopulateDemoCampaign: (campaignId: String?, autoEnd: Boolean) -> String,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val locale = state.locale
    fun t(key: String, params: Map<String, Any> = emptyMap()) =
        FlysticTranslations.t(key, locale, params)

    var selectedCampaignId by remember(campaigns) {
        mutableStateOf(campaigns.firstOrNull()?.id)
    }
    val activeCampaign = campaigns.firstOrNull { it.id == selectedCampaignId } ?: campaigns.firstOrNull()

    var joinInput by remember { mutableStateOf("") }
    var joinFeedback by remember { mutableStateOf<String?>(null) }

    var showCreateSheet by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) }
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var inspectedPhoto by remember { mutableStateOf<CampaignPhoto?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val savedPath = onSaveBitmap(bitmap)
            if (savedPath != null) {
                pendingPhotoPath = savedPath
                showPhotoDialog = true
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching { cameraLauncher.launch(null) }
        } else {
            Toast.makeText(context, t("scanCameraDenied"), Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = onSaveUri(uri)
            if (savedPath != null) {
                pendingPhotoPath = savedPath
                showPhotoDialog = true
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = t("backHome"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoMutedForeground,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "📣 ${t("campaignTitle")}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = t("campaignSubtitle"),
                fontSize = 13.sp,
                color = EcoMutedForeground
            )
        }

        // Create + Join by Link / Invite Code Bar
        item {
            EcoCard(contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showCreateSheet = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                    ) {
                        Text(
                            text = t("campaignOpenNewBtn"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val id = onPopulateDemoCampaign(activeCampaign?.id, true)
                            selectedCampaignId = id
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EcoSunshine),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = EcoSunshine.copy(alpha = 0.25f),
                            contentColor = EcoForeground
                        )
                    ) {
                        Text(
                            text = t("campaignDemoEndBtn"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = joinInput,
                        onValueChange = {
                            joinInput = it
                            joinFeedback = null
                        },
                        placeholder = {
                            Text(
                                text = t("campaignJoinPlaceholder"),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = EcoSecondary,
                            unfocusedContainerColor = EcoSecondary,
                            focusedBorderColor = EcoPrimary,
                            unfocusedBorderColor = EcoBorder
                        )
                    )
                    Button(
                        onClick = {
                            val ok = onJoinCampaign(joinInput)
                            joinFeedback = if (ok) {
                                joinInput = ""
                                t("campaignJoinSuccess")
                            } else {
                                t("campaignJoinNotFound")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoForeground)
                    ) {
                        Text(
                            text = t("campaignJoinBtn"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimaryGlow
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("ECO-HUST", "GREEN-BK").forEach { code ->
                        Surface(
                            shape = CircleShape,
                            color = EcoSecondary,
                            border = BorderStroke(1.dp, EcoBorder),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { joinInput = code }
                        ) {
                            Text(
                                text = "🔗 $code",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (!joinFeedback.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = joinFeedback!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoPrimary
                    )
                }
            }
        }

        // Selector chips if user has multiple campaigns
        if (campaigns.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    campaigns.forEach { cmp ->
                        val selected = cmp.id == activeCampaign?.id
                        Surface(
                            shape = CircleShape,
                            color = if (selected) EcoPrimary else Color.White,
                            border = BorderStroke(1.dp, if (selected) EcoPrimary else EcoBorder),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedCampaignId = cmp.id }
                        ) {
                            Text(
                                text = "${if (cmp.isEnded) "🏁" else "📣"} ${cmp.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else EcoForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        if (activeCampaign == null) {
            item {
                EcoCard(contentPadding = 24.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🌍", fontSize = 42.sp)
                        Text(
                            text = t("campaignEmptyTitle"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = t("campaignEmptyDesc"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val totalKg = activeCampaign.totalKg()
            val progress = (totalKg / activeCampaign.targetKg.coerceAtLeast(0.1)).toFloat().coerceIn(0.04f, 1f)
            val invitedInCity = personalization.safeInvitedMascots()

            // Campaign Summary & Invite Link Card
            item {
                EcoCard(contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeCampaign.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoForeground,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (activeCampaign.isEnded) EcoCoral.copy(alpha = 0.14f)
                                    else EcoPrimary.copy(alpha = 0.14f)
                                )
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (activeCampaign.isEnded) t("campaignEndedBadge") else t("campaignActiveBadge"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (activeCampaign.isEnded) EcoCoral else EcoPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = activeCampaign.description,
                        fontSize = 13.sp,
                        color = EcoMutedForeground
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalKg)} / ${String.format(Locale.US, "%.1f", activeCampaign.targetKg)} kg",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary
                        )
                        Text(
                            text = "${activeCampaign.safeParticipants().size} thành viên · ${activeCampaign.safePhotos().size} ảnh",
                            fontSize = 12.sp,
                            color = EcoMutedForeground
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(EcoSecondary)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(EcoPrimary)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Shareable Invite Link & Code
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EcoSecondary)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${t("campaignInviteCodeLabel")}: ${activeCampaign.inviteCode}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EcoForeground
                            )
                            Text(
                                text = activeCampaign.inviteLink,
                                fontSize = 10.sp,
                                color = EcoOcean,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("Campaign Invite", activeCampaign.inviteLink)
                                    )
                                    Toast.makeText(context, t("campaignCopiedLink"), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(t("campaignCopyLink"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${activeCampaign.title}\n${activeCampaign.inviteLink}\n${t("campaignInviteCodeLabel")}: ${activeCampaign.inviteCode}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, t("campaignShareLink")))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EcoOcean),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(t("campaignShareLink"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Custom Green City Stage with Invited Mascots Running & Jumping!
            item {
                EcoCard(contentPadding = 14.dp) {
                    Text(
                        text = "🏙️ ${t("campaignCityStageTitle", mapOf("count" to (invitedInCity.size + 1)))}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(8.dp))
                    PixelAnimatedCityStage(
                        city = personalization.safeCity(),
                        habitat = personalization.resolvedHabitat(),
                        userPetSpecies = petState.species,
                        userPetStage = PetStage.fromLevel(petState.level),
                        userPetSkin = personalization.resolvedSkin(),
                        userPetName = personalization.petNickname.ifBlank { t(petState.species.nameKey) },
                        invitedMascots = invitedInCity,
                        stageHeight = 148.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.5.dp, EcoBorder, RoundedCornerShape(18.dp))
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showInviteSheet = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = t("campaignInviteMascotBtn"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        if (!activeCampaign.isEnded) {
                            Button(
                                onClick = { onConcludeCampaign(activeCampaign.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EcoForeground),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = t("campaignEndBtn"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoPrimaryGlow,
                                    maxLines = 1
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onReopenCampaign(activeCampaign.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = t("campaignReopenBtn"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    runCatching { cameraLauncher.launch(null) }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EcoPrimary.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = t("campaignTakePhotoBtn"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EcoOcean.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = EcoOcean, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = t("campaignPickPhotoBtn"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoOcean,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Final / Live Campaign Leaderboard with Participant Captured Photos!
            val ranked = activeCampaign.rankedParticipants()
            item {
                Text(
                    text = "🏆 ${t("campaignFinalLeaderboard")}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
            }

            if (activeCampaign.isEnded && ranked.isNotEmpty()) {
                item {
                    CampaignTop3Podium(ranked = ranked.take(3))
                }
            }

            itemsIndexed(ranked, key = { _, p -> "${activeCampaign.id}-${p.userId}" }) { idx, participant ->
                val participantPhotos = activeCampaign.photosOf(participant.userId)
                val title = PetProgression.titleById(participant.equippedTitleId)
                EcoCard(
                    containerColor = if (idx == 0 && activeCampaign.isEnded) EcoSunshine.copy(alpha = 0.16f) else Color.White,
                    borderColor = if (idx == 0 && activeCampaign.isEnded) EcoSunshine else EcoBorder,
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = when (idx) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "#${idx + 1}"
                            },
                            fontSize = if (idx < 3) 20.sp else 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoForeground
                        )
                        if (participant.plasticMascot != null) {
                            PixelPlasticMascotSprite(
                                plasticType = participant.plasticMascot,
                                spriteSize = 46.dp,
                                happyOverride = true
                            )
                        } else {
                            PixelPetSprite(
                                species = participant.species,
                                stage = PetStage.SPROUT,
                                skin = participant.skin,
                                spriteSize = 48.dp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = participant.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${participant.mascotName} · ${String.format(Locale.US, "%.1f", participant.kgRecycled)} kg",
                                fontSize = 11.sp,
                                color = EcoMutedForeground
                            )
                            if (title != null) {
                                Text(
                                    text = "${title.emoji} ${t(title.nameKey)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoPrimary
                                )
                            }
                        }
                        Text(
                            text = "${participant.points}đ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoPrimary
                        )
                    }

                    if (participantPhotos.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(participantPhotos, key = { it.id }) { ph ->
                                CampaignPhotoThumbnail(
                                    photo = ph,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, EcoBorder, RoundedCornerShape(10.dp))
                                        .clickable { inspectedPhoto = ph }
                                )
                            }
                        }
                    }
                }
            }

            // Full Campaign Captured Photo Gallery
            val allPhotos = activeCampaign.safePhotos()
            item {
                Text(
                    text = "📸 ${t("campaignPhotosGallery", mapOf("count" to allPhotos.size))}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
            }

            if (allPhotos.isEmpty()) {
                item {
                    EcoCard(contentPadding = 16.dp) {
                        Text(
                            text = t("campaignNoPhotos"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                items(allPhotos.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { photo ->
                            EcoCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { inspectedPhoto = photo },
                                contentPadding = 10.dp
                            ) {
                                CampaignPhotoThumbnail(
                                    photo = photo,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${photo.plasticType.emoji} ${photo.plasticType.code} · ${photo.kg}kg",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EcoPrimary
                                )
                                Text(
                                    text = photo.authorName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoForeground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = photo.caption,
                                    fontSize = 10.sp,
                                    color = EcoMutedForeground,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Create Campaign Sheet
    if (showCreateSheet) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var targetKgStr by remember { mutableStateOf("10") }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📣 ${t("campaignCreateTitle")}",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(t("campaignNameLabel")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EcoCard,
                        unfocusedContainerColor = EcoCard
                    )
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(t("campaignDescLabel")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EcoCard,
                        unfocusedContainerColor = EcoCard
                    )
                )
                OutlinedTextField(
                    value = targetKgStr,
                    onValueChange = { targetKgStr = it },
                    label = { Text(t("campaignTargetKgLabel")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EcoCard,
                        unfocusedContainerColor = EcoCard
                    )
                )
                Button(
                    onClick = {
                        val target = targetKgStr.toDoubleOrNull() ?: 10.0
                        val created = onOpenCampaign(title, desc, target)
                        selectedCampaignId = created.id
                        showCreateSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                ) {
                    Text(t("campaignCreateConfirm"), fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Invite / Add Mascot Sheet
    if (showInviteSheet && activeCampaign != null) {
        var friendName by remember { mutableStateOf("") }
        var mascotName by remember { mutableStateOf("") }
        var selectedSpecies by remember { mutableStateOf(PetSpecies.FOX) }
        var selectedSkin by remember { mutableStateOf(PetSkin.SAKURA) }
        var selectedPlastic by remember { mutableStateOf<PlasticType?>(PlasticType.PET) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showInviteSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🐾 ${t("campaignInviteModalTitle")}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                OutlinedTextField(
                    value = friendName,
                    onValueChange = { friendName = it },
                    label = { Text(t("campaignFriendNameLabel")) },
                    placeholder = { Text("VD: Minh Khoa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = mascotName,
                    onValueChange = { mascotName = it },
                    label = { Text(t("campaignMascotNameLabel")) },
                    placeholder = { Text("VD: PET-Chan / Cáo Xanh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = t("campaignMascotSpeciesLabel"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlasticType.entries.forEach { pl ->
                        val selected = selectedPlastic == pl
                        Surface(
                            shape = CircleShape,
                            color = if (selected) EcoPrimary else EcoSecondary,
                            border = BorderStroke(1.dp, if (selected) EcoPrimary else EcoBorder),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedPlastic = pl }
                        ) {
                            Text(
                                text = "${pl.emoji} ${pl.code}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else EcoForeground,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                    PetSpecies.entries.forEach { sp ->
                        val selected = selectedPlastic == null && selectedSpecies == sp
                        Surface(
                            shape = CircleShape,
                            color = if (selected) EcoPrimary else EcoSecondary,
                            border = BorderStroke(1.dp, if (selected) EcoPrimary else EcoBorder),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    selectedPlastic = null
                                    selectedSpecies = sp
                                }
                        ) {
                            Text(
                                text = t(sp.nameKey),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else EcoForeground,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                if (selectedPlastic == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PetSkin.entries.forEach { sk ->
                            val selected = selectedSkin == sk
                            Surface(
                                shape = CircleShape,
                                color = if (selected) EcoForeground else EcoSecondary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { selectedSkin = sk }
                            ) {
                                Text(
                                    text = "${sk.emoji} ${t(sk.nameKey)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else EcoForeground,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        onInviteMascot(
                            activeCampaign.id,
                            friendName,
                            mascotName,
                            selectedSpecies,
                            selectedSkin,
                            selectedPlastic
                        )
                        showInviteSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                ) {
                    Text(t("campaignInviteConfirm"), fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Confirm Captured/Uploaded Photo Dialog
    if (showPhotoDialog && activeCampaign != null) {
        var caption by remember { mutableStateOf("") }
        var kgText by remember { mutableStateOf("1.2") }
        var plastic by remember { mutableStateOf(PlasticType.PET) }

        AlertDialog(
            onDismissRequest = {
                showPhotoDialog = false
                pendingPhotoPath = null
            },
            title = {
                Text(t("campaignPhotoModalTitle"), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pendingPhotoPath?.let { path ->
                        val bmp = remember(path) { BitmapFactory.decodeFile(path) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlasticType.entries.forEach { pl ->
                            val sel = plastic == pl
                            Surface(
                                shape = CircleShape,
                                color = if (sel) EcoPrimary else EcoSecondary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { plastic = pl }
                            ) {
                                Text(
                                    text = "${pl.emoji} ${pl.code}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else EcoForeground,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text(t("campaignPhotoCaptionLabel")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = kgText,
                        onValueChange = { kgText = it },
                        label = { Text(t("campaignPhotoKgLabel")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kgVal = kgText.toDoubleOrNull() ?: 1.0
                        onAddPhoto(activeCampaign.id, caption, plastic, kgVal, pendingPhotoPath, null)
                        showPhotoDialog = false
                        pendingPhotoPath = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                ) {
                    Text(t("campaignPhotoSaveBtn"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPhotoDialog = false
                        pendingPhotoPath = null
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    // Full-screen Photo Inspector Dialog
    inspectedPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { inspectedPhoto = null },
            title = {
                Text(
                    text = "${photo.plasticType.emoji} ${photo.plasticType.code} · ${photo.kg} kg",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampaignPhotoThumbnail(
                        photo = photo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Text(
                        text = photo.authorName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Text(
                        text = photo.caption,
                        fontSize = 13.sp,
                        color = EcoMutedForeground
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { inspectedPhoto = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                ) {
                    Text("Đóng", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun CampaignTop3Podium(ranked: List<CampaignParticipant>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(1, 0, 2).forEach { idx ->
            val p = ranked.getOrNull(idx)
            if (p == null) {
                Spacer(Modifier.weight(1f))
            } else {
                val height = when (idx) {
                    0 -> 96.dp
                    1 -> 74.dp
                    else -> 60.dp
                }
                val tone = when (idx) {
                    0 -> EcoSunshine
                    1 -> EcoLeaf.copy(alpha = 0.45f)
                    else -> EcoCoral.copy(alpha = 0.4f)
                }
                val medal = when (idx) {
                    0 -> "🥇"
                    1 -> "🥈"
                    else -> "🥉"
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(medal, fontSize = 20.sp)
                    if (p.plasticMascot != null) {
                        PixelPlasticMascotSprite(
                            plasticType = p.plasticMascot,
                            spriteSize = 44.dp,
                            happyOverride = true
                        )
                    } else {
                        PixelPetSprite(
                            species = p.species,
                            stage = PetStage.SPROUT,
                            skin = p.skin,
                            spriteSize = 46.dp
                        )
                    }
                    Text(
                        text = p.name.split(" ").takeLast(2).joinToString(" "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${p.points}đ · ${p.kgRecycled}kg",
                        fontSize = 10.sp,
                        color = EcoMutedForeground
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(tone),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = EcoForeground
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampaignPhotoThumbnail(
    photo: CampaignPhoto,
    modifier: Modifier = Modifier
) {
    val bmp = remember(photo.localImagePath) {
        photo.localImagePath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
    }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = photo.caption,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        val drawableRes = when (photo.samplePhotoKey ?: photo.plasticType.code.lowercase()) {
            "hdpe" -> R.drawable.plastic_hdpe
            "pp" -> R.drawable.plastic_pp
            "ldpe" -> R.drawable.plastic_ldpe
            "ps" -> R.drawable.plastic_ps
            else -> R.drawable.plastic_pet
        }
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = photo.caption,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
