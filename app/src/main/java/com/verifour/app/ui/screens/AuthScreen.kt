package com.verifour.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.R
import com.verifour.app.data.FlysticApiClient
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.COLLECTION_POINTS
import com.verifour.app.model.HUST_SCHOOLS
import com.verifour.app.model.Role
import com.verifour.app.model.StudentOrganization
import com.verifour.app.model.User
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.components.LanguageSwitcherRow
import com.verifour.app.ui.components.PlasticPatternBackdrop
import com.verifour.app.ui.theme.EcoBackground
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    locale: AppLocale,
    isRemote: Boolean,
    serverUrl: String,
    onLocaleChange: (AppLocale) -> Unit,
    onRemoteModeChange: (Boolean) -> Unit,
    onServerUrlChange: (String) -> Unit,
    onTestServerConnection: suspend (String) -> Result<String>,
    onBackHome: () -> Unit,
    onSubmitAuth: suspend (
        email: String,
        password: String,
        isRegister: Boolean,
        role: Role,
        name: String,
        birthDate: String?,
        organization: StudentOrganization?,
        school: String?,
        organizationDetail: String?,
        studentId: String?,
        stationId: String?,
        inviteCode: String?
    ) -> User,
    onAuthSuccess: (User) -> Unit
) {
    val scope = rememberCoroutineScope()
    fun t(key: String) = FlysticTranslations.t(key, locale)

    var isRegister by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(Role.STUDENT) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var organization by remember { mutableStateOf<StudentOrganization?>(null) }
    var orgDropdownOpen by remember { mutableStateOf(false) }
    var school by remember { mutableStateOf("") }
    var schoolDropdownOpen by remember { mutableStateOf(false) }
    var studentId by remember { mutableStateOf("") }
    var organizationDetail by remember { mutableStateOf("") }
    var selectedStationId by remember { mutableStateOf(COLLECTION_POINTS.first().id) }
    var stationDropdownOpen by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var serverUrlInput by remember(serverUrl) { mutableStateOf(serverUrl) }
    var isPingingServer by remember { mutableStateOf(false) }
    var pingSuccess by remember { mutableStateOf<Boolean?>(null) }
    var pingMessage by remember { mutableStateOf<String?>(null) }

    val passwordMismatch = isRegister && confirmPassword.isNotEmpty() && password != confirmPassword
    val isStudentReg = isRegister && role == Role.STUDENT

    Scaffold(
        containerColor = EcoBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            PlasticPatternBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("backHome"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoMutedForeground,
                        modifier = Modifier.clickable { onBackHome() }
                    )
                    LanguageSwitcherRow(
                        currentLocale = locale,
                        onSelect = onLocaleChange
                    )
                }

                // Mode switcher (Live Server vs Offline Demo)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EcoSecondary)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModeTab(
                        label = t("authModeRemote"),
                        selected = isRemote,
                        modifier = Modifier.weight(1f),
                        onClick = { onRemoteModeChange(true); error = null }
                    )
                    ModeTab(
                        label = t("authModeDemo"),
                        selected = !isRemote,
                        modifier = Modifier.weight(1f),
                        onClick = { onRemoteModeChange(false); error = null }
                    )
                }

                if (isRemote) {
                    EcoCard(contentPadding = 14.dp) {
                        AuthFieldLabel(t("authServerUrlLabel"))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = serverUrlInput,
                                onValueChange = {
                                    serverUrlInput = it
                                    pingSuccess = null
                                    pingMessage = null
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        FlysticApiClient.DEFAULT_BASE_URL,
                                        fontSize = 12.sp,
                                        color = EcoMutedForeground.copy(alpha = 0.7f)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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
                                    isPingingServer = true
                                    pingSuccess = null
                                    pingMessage = null
                                    scope.launch {
                                        val res = onTestServerConnection(serverUrlInput)
                                        isPingingServer = false
                                        res.onSuccess {
                                            serverUrlInput = FlysticApiClient.normalizeBaseUrl(serverUrlInput)
                                            pingSuccess = true
                                            pingMessage = t("authServerOk")
                                        }.onFailure { err ->
                                            pingSuccess = false
                                            pingMessage = "${t("authServerFail")}: ${err.message.orEmpty()}"
                                        }
                                    }
                                },
                                enabled = !isPingingServer && serverUrlInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EcoPrimary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = if (isPingingServer) t("authServerPinging") else t("authServerPing"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PresetChip(
                                label = "Mac LAN",
                                selected = serverUrlInput.trimEnd('/') == FlysticApiClient.DEFAULT_BASE_URL,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val next = onServerUrlChangePreset(FlysticApiClient.DEFAULT_BASE_URL, onServerUrlChange)
                                    serverUrlInput = next
                                    pingSuccess = null
                                    pingMessage = null
                                }
                            )
                            PresetChip(
                                label = "Emulator",
                                selected = serverUrlInput.contains("10.0.2.2"),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val next = onServerUrlChangePreset("http://10.0.2.2:8787", onServerUrlChange)
                                    serverUrlInput = next
                                    pingSuccess = null
                                    pingMessage = null
                                }
                            )
                            PresetChip(
                                label = "Cloud",
                                selected = serverUrlInput.contains("api.yoha.io.vn"),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val next = onServerUrlChangePreset("https://api.yoha.io.vn", onServerUrlChange)
                                    serverUrlInput = next
                                    pingSuccess = null
                                    pingMessage = null
                                }
                            )
                        }

                        if (pingMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = pingMessage.orEmpty(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (pingSuccess == true) EcoPrimary else EcoCoral
                            )
                        }
                    }
                }

                EcoCard(contentPadding = 22.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, EcoBorder, RoundedCornerShape(12.dp))
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.verifour_avatar),
                                contentDescription = "VeriFour",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "VeriFour",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoForeground
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (isRegister) t("authRegister") else t("authLogin"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isRegister) t("authStartGreen") else t("authWelcomeBack"),
                        fontSize = 14.sp,
                        color = EcoMutedForeground
                    )

                    Spacer(Modifier.height(16.dp))

                    // Role Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(EcoSecondary)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ModeTab(
                            label = t("authStudentRole"),
                            selected = role == Role.STUDENT,
                            modifier = Modifier.weight(1f),
                            onClick = { role = Role.STUDENT; error = null }
                        )
                        ModeTab(
                            label = t("authAdminRole"),
                            selected = role == Role.ADMIN,
                            modifier = Modifier.weight(1f),
                            onClick = { role = Role.ADMIN; error = null }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (isRegister) {
                        AuthFieldLabel(t("authFullName"))
                        AuthInput(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Nguyễn Văn A"
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    if (isStudentReg) {
                        AuthFieldLabel(t("authBirthDate"))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(EcoSecondary)
                                .border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = birthDate.ifBlank { "YYYY-MM-DD" },
                                    color = if (birthDate.isBlank()) EcoMutedForeground else EcoForeground,
                                    fontSize = 14.sp
                                )
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = EcoMutedForeground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        AuthFieldLabel(t("authOrganization"))
                        Box {
                            DropdownFieldButton(
                                text = when (organization) {
                                    StudentOrganization.HUST -> t("authOrganizationHust")
                                    StudentOrganization.OTHER -> t("authOrganizationOther")
                                    null -> t("authOrganizationPlaceholder")
                                },
                                isPlaceholder = organization == null,
                                onClick = { orgDropdownOpen = true }
                            )
                            DropdownMenu(
                                expanded = orgDropdownOpen,
                                onDismissRequest = { orgDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(t("authOrganizationHust")) },
                                    onClick = {
                                        organization = StudentOrganization.HUST
                                        school = ""
                                        studentId = ""
                                        organizationDetail = ""
                                        orgDropdownOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(t("authOrganizationOther")) },
                                    onClick = {
                                        organization = StudentOrganization.OTHER
                                        school = ""
                                        studentId = ""
                                        organizationDetail = ""
                                        orgDropdownOpen = false
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        if (organization == StudentOrganization.HUST) {
                            AuthFieldLabel(t("authSchool"))
                            Box {
                                DropdownFieldButton(
                                    text = school.ifBlank { t("authSchoolPlaceholder") },
                                    isPlaceholder = school.isBlank(),
                                    onClick = { schoolDropdownOpen = true }
                                )
                                DropdownMenu(
                                    expanded = schoolDropdownOpen,
                                    onDismissRequest = { schoolDropdownOpen = false }
                                ) {
                                    HUST_SCHOOLS.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item, fontSize = 13.sp) },
                                            onClick = {
                                                school = item
                                                schoolDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            if (school.isNotBlank()) {
                                AuthFieldLabel(t("authStudentId"))
                                AuthInput(
                                    value = studentId,
                                    onValueChange = { studentId = it },
                                    placeholder = t("authStudentIdPlaceholder")
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        } else if (organization == StudentOrganization.OTHER) {
                            AuthFieldLabel(t("authOrganizationDetail"))
                            AuthInput(
                                value = organizationDetail,
                                onValueChange = { organizationDetail = it },
                                placeholder = t("authOrganizationDetailPlaceholder")
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    if (isRegister && role == Role.ADMIN) {
                        AuthFieldLabel(t("adminCollectionPoint"))
                        Box {
                            val selectedMeta = COLLECTION_POINTS.first { it.id == selectedStationId }
                            val localizedName = FlysticTranslations.localizedCollectionPoint(
                                selectedMeta.canonicalName,
                                locale
                            ).first
                            DropdownFieldButton(
                                text = localizedName,
                                isPlaceholder = false,
                                onClick = { stationDropdownOpen = true }
                            )
                            DropdownMenu(
                                expanded = stationDropdownOpen,
                                onDismissRequest = { stationDropdownOpen = false }
                            ) {
                                COLLECTION_POINTS.forEach { point ->
                                    val locName = FlysticTranslations.localizedCollectionPoint(
                                        point.canonicalName,
                                        locale
                                    ).first
                                    DropdownMenuItem(
                                        text = { Text(locName, fontSize = 13.sp) },
                                        onClick = {
                                            selectedStationId = point.id
                                            stationDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    AuthFieldLabel(if (role == Role.ADMIN) t("authAdminEmail") else t("authStudentEmail"))
                    AuthInput(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = if (role == Role.ADMIN) t("authEmailPlaceholderAdmin") else t("authEmailPlaceholderStudent"),
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(Modifier.height(12.dp))

                    AuthFieldLabel(t("authPassword"))
                    AuthInput(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        isPassword = true
                    )

                    if (isRegister && role == Role.ADMIN && isRemote) {
                        Spacer(Modifier.height(12.dp))
                        AuthFieldLabel(t("authAdminInviteCode"))
                        AuthInput(
                            value = inviteCode,
                            onValueChange = { inviteCode = it },
                            placeholder = "••••••••",
                            isPassword = true
                        )
                    }

                    if (isRegister) {
                        Spacer(Modifier.height(12.dp))
                        AuthFieldLabel(t("authConfirmPassword"))
                        AuthInput(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "••••••••",
                            isPassword = true,
                            isError = passwordMismatch
                        )
                        if (passwordMismatch) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = t("authPasswordMismatch"),
                                fontSize = 12.sp,
                                color = EcoCoral,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EcoCoral.copy(alpha = 0.14f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = error.orEmpty(),
                                color = EcoCoral,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    val canSubmit = !isSubmitting && !passwordMismatch && email.isNotBlank() &&
                        password.length >= 6 &&
                        (!isRegister || (name.isNotBlank() && password == confirmPassword &&
                            (!isRemote || role != Role.ADMIN || inviteCode.isNotBlank()) &&
                            (role != Role.STUDENT || (birthDate.isNotBlank() && organization != null &&
                                ((organization == StudentOrganization.HUST && school.isNotBlank() && studentId.isNotBlank()) ||
                                    (organization == StudentOrganization.OTHER && organizationDetail.isNotBlank()))))))

                    Button(
                        onClick = {
                            error = null
                            isSubmitting = true
                            scope.launch {
                                try {
                                    if (isRemote && serverUrlInput.isNotBlank()) {
                                        onServerUrlChange(serverUrlInput)
                                    }
                                    val user = onSubmitAuth(
                                        email,
                                        password,
                                        isRegister,
                                        role,
                                        name,
                                        birthDate.ifBlank { null },
                                        organization,
                                        school.ifBlank { null },
                                        organizationDetail.ifBlank { null },
                                        studentId.ifBlank { null },
                                        selectedStationId,
                                        inviteCode.ifBlank { null }
                                    )
                                    onAuthSuccess(user)
                                } catch (e: Exception) {
                                    error = e.message ?: t("authServerError")
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EcoPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = when {
                                isSubmitting -> t("authSubmitting")
                                isRegister -> t("authRegister")
                                else -> t("authLogin")
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    if (!isRegister && isRemote) {
                        Spacer(Modifier.height(14.dp))
                        AuthFieldLabel(t("authQuickDemoTitle"))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetChip(
                                label = t("authQuickDemoStudent"),
                                selected = email == "sinhvien@sis.hust.edu.vn",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    role = Role.STUDENT
                                    email = "sinhvien@sis.hust.edu.vn"
                                    password = "student1234"
                                    error = null
                                }
                            )
                            PresetChip(
                                label = t("authQuickDemoAdmin"),
                                selected = email == "admin@hust.edu.vn",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    role = Role.ADMIN
                                    email = "admin@hust.edu.vn"
                                    password = "admin1234"
                                    error = null
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isRegister) t("authHaveAccount") else t("authNoAccount"),
                            fontSize = 13.sp,
                            color = EcoMutedForeground
                        )
                        Text(
                            text = if (isRegister) t("authSignInNow") else t("authSignUpNow"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoPrimary,
                            modifier = Modifier.clickable {
                                isRegister = !isRegister
                                error = null
                            }
                        )
                    }
                }

                Text(
                    text = if (isRemote) t("authRemoteNote") else t("authDemoNote"),
                    fontSize = 12.sp,
                    color = EcoMutedForeground,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            birthDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = EcoPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = EcoMutedForeground)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) EcoForeground else EcoMutedForeground
        )
    }
}

private fun onServerUrlChangePreset(
    presetUrl: String,
    onServerUrlChange: (String) -> Unit
): String {
    onServerUrlChange(presetUrl)
    return presetUrl
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) EcoPrimary.copy(alpha = 0.14f) else EcoSecondary)
            .border(
                width = 1.dp,
                color = if (selected) EcoPrimary else EcoBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) EcoPrimary else EcoMutedForeground,
            maxLines = 1
        )
    }
}

@Composable
private fun AuthFieldLabel(label: String) {
    Text(
        text = label.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = EcoMutedForeground,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun AuthInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = EcoMutedForeground.copy(alpha = 0.7f)) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = isError,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = EcoSecondary,
            unfocusedContainerColor = EcoSecondary,
            focusedBorderColor = EcoPrimary,
            unfocusedBorderColor = EcoBorder,
            errorBorderColor = EcoCoral
        )
    )
}

@Composable
private fun DropdownFieldButton(
    text: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EcoSecondary)
            .border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (isPlaceholder) EcoMutedForeground else EcoForeground,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = EcoMutedForeground
        )
    }
}
