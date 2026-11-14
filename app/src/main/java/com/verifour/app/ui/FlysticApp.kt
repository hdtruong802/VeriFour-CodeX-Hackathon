package com.verifour.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.Role
import com.verifour.app.ui.components.PlasticPatternBackdrop
import com.verifour.app.ui.screens.AdminDashboardScreen
import com.verifour.app.ui.screens.AdminNewTransactionScreen
import com.verifour.app.ui.screens.AdminReportsScreen
import com.verifour.app.ui.screens.AdminTransactionsScreen
import com.verifour.app.ui.screens.AuthScreen
import com.verifour.app.ui.screens.CampaignScreen
import com.verifour.app.ui.screens.GuideScreen
import com.verifour.app.ui.screens.LandingScreen
import com.verifour.app.ui.screens.LeaderboardScreen
import com.verifour.app.ui.screens.MapScreen
import com.verifour.app.ui.screens.PetBattleScreen
import com.verifour.app.ui.screens.PlasticAiSortScreen
import com.verifour.app.ui.screens.ProfileScreen
import com.verifour.app.ui.screens.RewardsScreen
import com.verifour.app.ui.screens.ScanScreen
import com.verifour.app.ui.screens.StudentHomeScreen
import com.verifour.app.ui.theme.EcoBackground
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCard
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSurface
import com.verifour.app.viewmodel.FlysticViewModel

private object FlysticRoutes {
    const val LANDING = "landing"
    const val AUTH = "auth"
    const val PUBLIC_MAP = "public_map"

    const val STUDENT_HOME = "student/home"
    const val STUDENT_MAP = "student/map"
    const val STUDENT_SCAN = "student/scan"
    const val STUDENT_REWARDS = "student/rewards"
    const val STUDENT_PROFILE = "student/profile"
    const val STUDENT_GUIDE = "student/guide"
    const val STUDENT_LEADERBOARD = "student/leaderboard"
    const val STUDENT_BATTLE = "student/battle"
    const val STUDENT_CAMPAIGNS = "student/campaigns"
    const val STUDENT_AI_SORT = "student/ai-sort"

    const val ADMIN_DASHBOARD = "admin/dashboard"
    const val ADMIN_NEW = "admin/new"
    const val ADMIN_TRANSACTIONS = "admin/transactions"
    const val ADMIN_REPORTS = "admin/reports"
}

@Composable
fun FlysticApp(viewModel: FlysticViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val state = uiState.state
    val locale = state.locale
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    if (!uiState.isReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EcoBackground),
            contentAlignment = Alignment.Center
        ) {
            PlasticPatternBackdrop()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(EcoPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "F",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(color = EcoPrimary)
            }
        }
        return
    }

    val startDestination = remember {
        when (state.user?.role) {
            Role.STUDENT -> FlysticRoutes.STUDENT_HOME
            Role.ADMIN -> FlysticRoutes.ADMIN_DASHBOARD
            null -> FlysticRoutes.LANDING
        }
    }

    // Role & auth navigation guard
    LaunchedEffect(state.user?.id, state.user?.role, currentRoute) {
        if (currentRoute == null) return@LaunchedEffect
        val user = state.user
        when {
            user == null && (currentRoute.startsWith("student/") || currentRoute.startsWith("admin/")) -> {
                navController.navigate(FlysticRoutes.LANDING) {
                    popUpTo(0) { inclusive = true }
                }
            }
            user?.role == Role.STUDENT && (
                currentRoute == FlysticRoutes.LANDING ||
                    currentRoute == FlysticRoutes.AUTH ||
                    currentRoute == FlysticRoutes.PUBLIC_MAP ||
                    currentRoute.startsWith("admin/")
                ) -> {
                navController.navigate(FlysticRoutes.STUDENT_HOME) {
                    popUpTo(0) { inclusive = true }
                }
            }
            user?.role == Role.ADMIN && (
                currentRoute == FlysticRoutes.LANDING ||
                    currentRoute == FlysticRoutes.AUTH ||
                    currentRoute == FlysticRoutes.PUBLIC_MAP ||
                    currentRoute.startsWith("student/")
                ) -> {
                navController.navigate(FlysticRoutes.ADMIN_DASHBOARD) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val isStudentShell = currentRoute?.startsWith("student/") == true && state.user?.role == Role.STUDENT
    val isAdminShell = currentRoute?.startsWith("admin/") == true && state.user?.role == Role.ADMIN

    when {
        isStudentShell -> {
            Scaffold(
                containerColor = EcoSurface,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    StudentBottomBar(
                        currentRoute = currentRoute,
                        locale = locale,
                        onNavigate = { route -> navigateTab(navController, route) }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    PlasticPatternBackdrop()
                    FlysticNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        viewModel = viewModel,
                        contentPadding = innerPadding
                    )
                }
            }
        }

        isAdminShell -> {
            Scaffold(
                containerColor = EcoSurface,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    AdminTopBar(
                        locale = locale,
                        onSignOut = {
                            viewModel.signOut()
                            navController.navigate(FlysticRoutes.LANDING) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                },
                bottomBar = {
                    AdminBottomBar(
                        currentRoute = currentRoute,
                        locale = locale,
                        onNavigate = { route -> navigateTab(navController, route) }
                    )
                }
            ) { innerPadding ->
                FlysticNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    viewModel = viewModel,
                    contentPadding = innerPadding
                )
            }
        }

        else -> {
            FlysticNavHost(
                navController = navController,
                startDestination = startDestination,
                viewModel = viewModel,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            )
        }
    }
}

@Composable
private fun FlysticNavHost(
    navController: NavHostController,
    startDestination: String,
    viewModel: FlysticViewModel,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val state = uiState.state
    var pendingBattleOpponentId by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(FlysticRoutes.LANDING) {
            LandingScreen(
                locale = state.locale,
                onLocaleChange = { viewModel.setLocale(it) },
                onNavigateAuth = { navController.navigate(FlysticRoutes.AUTH) },
                onNavigateMap = { navController.navigate(FlysticRoutes.PUBLIC_MAP) }
            )
        }

        composable(FlysticRoutes.PUBLIC_MAP) {
            Scaffold(
                containerColor = EcoBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    PlasticPatternBackdrop()
                    MapScreen(
                        state = state,
                        isRemote = uiState.isRemote,
                        showBackButton = true,
                        onBack = { navController.popBackStack() },
                        contentPadding = innerPadding
                    )
                }
            }
        }

        composable(FlysticRoutes.AUTH) {
            AuthScreen(
                locale = state.locale,
                isRemote = uiState.isRemote,
                onLocaleChange = { viewModel.setLocale(it) },
                onRemoteModeChange = { viewModel.setRemoteMode(it) },
                onBackHome = {
                    if (!navController.popBackStack()) {
                        navController.navigate(FlysticRoutes.LANDING)
                    }
                },
                onSubmitAuth = { email, password, isRegister, role, name, birthDate, organization, school, organizationDetail, studentId, stationId, inviteCode ->
                    viewModel.signIn(
                        email = email,
                        password = password,
                        isRegister = isRegister,
                        role = role,
                        name = name,
                        birthDate = birthDate,
                        organization = organization,
                        school = school,
                        organizationDetail = organizationDetail,
                        studentId = studentId,
                        stationId = stationId,
                        inviteCode = inviteCode
                    )
                },
                onAuthSuccess = { user ->
                    val target = if (user.role == Role.ADMIN) {
                        FlysticRoutes.ADMIN_DASHBOARD
                    } else {
                        FlysticRoutes.STUDENT_HOME
                    }
                    navController.navigate(target) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Student Routes
        composable(FlysticRoutes.STUDENT_HOME) {
            StudentHomeScreen(
                state = state,
                petState = uiState.petState,
                personalization = uiState.personalization,
                petReactionKey = uiState.petReactionKey,
                petEvent = uiState.petEvent,
                petEventTrigger = uiState.petEventTrigger,
                petEventPointsDelta = uiState.petEventPointsDelta,
                petEventNewLevel = uiState.petEventNewLevel,
                onPatPet = { viewModel.patPet() },
                onFeedPet = { viewModel.feedPet() },
                onUpgradePet = { viewModel.upgradePet() },
                onDemoAddPoints = { viewModel.feedPet(25) },
                onDemoUpgradePet = { viewModel.demoGrantAndUpgradePet() },
                onSelectPetSpecies = { viewModel.selectPetSpecies(it) },
                onSelectSkin = { viewModel.unlockOrEquipSkin(it) },
                onWaterTree = { viewModel.waterEcoTree() },
                onUpgradeBuilding = { viewModel.upgradeCityBuilding(it) },
                onGrantDemoResources = { viewModel.grantDemoEcoResources() },
                onSavePersonalization = { nick, personality, persona, plastic, theme ->
                    viewModel.updatePersonalization(nick, personality, persona, plastic, theme)
                },
                onOpenScan = { navigateTab(navController, FlysticRoutes.STUDENT_SCAN) },
                onOpenAiSort = { navController.navigate(FlysticRoutes.STUDENT_AI_SORT) },
                onOpenGuide = { navController.navigate(FlysticRoutes.STUDENT_GUIDE) },
                onOpenLeaderboard = { navController.navigate(FlysticRoutes.STUDENT_LEADERBOARD) },
                onOpenBattle = {
                    pendingBattleOpponentId = null
                    navController.navigate(FlysticRoutes.STUDENT_BATTLE)
                },
                onOpenCampaigns = {
                    navController.navigate(FlysticRoutes.STUDENT_CAMPAIGNS)
                },
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.STUDENT_MAP) {
            LaunchedEffect(Unit) { viewModel.refresh() }
            MapScreen(
                state = state,
                isRemote = uiState.isRemote,
                showBackButton = false,
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.STUDENT_SCAN) {
            ScanScreen(
                state = state,
                isRemote = uiState.isRemote,
                petState = uiState.petState,
                personalization = uiState.personalization,
                petEvent = uiState.petEvent,
                petEventTrigger = uiState.petEventTrigger,
                petEventNewLevel = uiState.petEventNewLevel,
                onUpgradePet = { viewModel.upgradePet() },
                onGetTransaction = { token -> viewModel.getTransaction(token) },
                onConfirmTransaction = { token, destination, preview ->
                    viewModel.confirmTransaction(token, destination, preview)
                },
                onBackHome = { navigateTab(navController, FlysticRoutes.STUDENT_HOME) },
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.STUDENT_REWARDS) {
            RewardsScreen(
                state = state,
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.STUDENT_PROFILE) {
            ProfileScreen(
                state = state,
                isRemote = uiState.isRemote,
                petState = uiState.petState,
                personalization = uiState.personalization,
                campaigns = uiState.campaigns,
                onEquipTitle = { viewModel.equipTitle(it) },
                onLocaleChange = { viewModel.setLocale(it) },
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(FlysticRoutes.LANDING) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.STUDENT_GUIDE) {
            GuideScreen(
                locale = state.locale,
                onOpenAiSort = { navController.navigate(FlysticRoutes.STUDENT_AI_SORT) },
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.STUDENT_AI_SORT) {
            PlasticAiSortScreen(
                locale = state.locale,
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.STUDENT_LEADERBOARD) {
            LeaderboardScreen(
                state = state,
                isRemote = uiState.isRemote,
                petState = uiState.petState,
                personalization = uiState.personalization,
                onFetchLeaderboard = { period -> viewModel.getLeaderboard(period) },
                onOpenBattle = { opponentId ->
                    pendingBattleOpponentId = opponentId
                    navController.navigate(FlysticRoutes.STUDENT_BATTLE)
                },
                onOpenCampaigns = {
                    navController.navigate(FlysticRoutes.STUDENT_CAMPAIGNS)
                },
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.STUDENT_BATTLE) {
            PetBattleScreen(
                state = state,
                petState = uiState.petState,
                personalization = uiState.personalization,
                initialOpponentId = pendingBattleOpponentId,
                onBattleFinished = { won, rpDelta, seedsEarned, xpEarned, weeklyPtsEarned, weeklyKgEarned ->
                    viewModel.recordBattleResult(
                        won = won,
                        rpDelta = rpDelta,
                        seedsEarned = seedsEarned,
                        xpEarned = xpEarned,
                        weeklyPtsEarned = weeklyPtsEarned,
                        weeklyKgEarned = weeklyKgEarned
                    )
                },
                onOpenLeaderboard = {
                    navController.navigate(FlysticRoutes.STUDENT_LEADERBOARD) {
                        popUpTo(FlysticRoutes.STUDENT_BATTLE) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.STUDENT_CAMPAIGNS) {
            CampaignScreen(
                state = state,
                petState = uiState.petState,
                personalization = uiState.personalization,
                campaigns = uiState.campaigns,
                onOpenCampaign = { title, desc, targetKg ->
                    viewModel.openCampaign(title, desc, targetKg)
                },
                onJoinCampaign = { codeOrLink ->
                    viewModel.joinCampaignByCodeOrLink(codeOrLink)
                },
                onInviteMascot = { cmpId, friendName, mascotName, species, skin, plastic ->
                    viewModel.inviteMascotToCampaign(cmpId, friendName, mascotName, species, skin, plastic)
                },
                onAddPhoto = { cmpId, caption, plastic, kg, localPath, sampleKey ->
                    viewModel.addCampaignPhoto(cmpId, caption, plastic, kg, localPath, sampleKey)
                },
                onSaveBitmap = { bmp -> viewModel.saveCampaignBitmap(bmp) },
                onSaveUri = { uri -> viewModel.saveCampaignUri(uri) },
                onConcludeCampaign = { viewModel.concludeCampaign(it) },
                onReopenCampaign = { viewModel.reopenCampaign(it) },
                onPopulateDemoCampaign = { cmpId, autoEnd ->
                    viewModel.populateDemoCampaign(cmpId, autoEnd)
                },
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding
            )
        }

        // Admin Routes
        composable(FlysticRoutes.ADMIN_DASHBOARD) {
            LaunchedEffect(Unit) { viewModel.refresh() }
            AdminDashboardScreen(
                state = state,
                isRemote = uiState.isRemote,
                onOpenNewTransaction = { navigateTab(navController, FlysticRoutes.ADMIN_NEW) },
                onOpenTransactions = { navigateTab(navController, FlysticRoutes.ADMIN_TRANSACTIONS) },
                onEmptyCollectionPoint = { stationName ->
                    viewModel.emptyCollectionPoint(stationName)
                },
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.ADMIN_NEW) {
            AdminNewTransactionScreen(
                state = state,
                isRemote = uiState.isRemote,
                onCreateTransaction = { items, station ->
                    viewModel.createTransaction(items, station)
                },
                onGetTransaction = { token -> viewModel.getTransaction(token) },
                onCancelTransaction = { id -> viewModel.cancelTransaction(id) },
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.ADMIN_TRANSACTIONS) {
            AdminTransactionsScreen(
                state = state,
                contentPadding = contentPadding
            )
        }

        composable(FlysticRoutes.ADMIN_REPORTS) {
            AdminReportsScreen(
                state = state,
                contentPadding = contentPadding
            )
        }
    }
}

private fun navigateTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isPrimaryCenter: Boolean = false
)

@Composable
private fun StudentBottomBar(
    currentRoute: String?,
    locale: AppLocale,
    onNavigate: (String) -> Unit
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)

    val tabs = listOf(
        NavTabItem(FlysticRoutes.STUDENT_HOME, t("routeHome"), Icons.Rounded.Home),
        NavTabItem(FlysticRoutes.STUDENT_MAP, t("routeMap"), Icons.Rounded.LocationOn),
        NavTabItem(FlysticRoutes.STUDENT_SCAN, t("routeScan"), Icons.Rounded.QrCodeScanner, isPrimaryCenter = true),
        NavTabItem(FlysticRoutes.STUDENT_REWARDS, t("routeRewards"), Icons.Rounded.CardGiftcard),
        NavTabItem(FlysticRoutes.STUDENT_PROFILE, t("routeProfile"), Icons.Rounded.Person)
    )

    Surface(
        color = EcoCard.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, EcoBorder),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val active = currentRoute == tab.route ||
                    (tab.route == FlysticRoutes.STUDENT_HOME &&
                        (currentRoute == FlysticRoutes.STUDENT_GUIDE ||
                            currentRoute == FlysticRoutes.STUDENT_LEADERBOARD ||
                            currentRoute == FlysticRoutes.STUDENT_BATTLE ||
                            currentRoute == FlysticRoutes.STUDENT_CAMPAIGNS ||
                            currentRoute == FlysticRoutes.STUDENT_AI_SORT))

                if (tab.isPrimaryCenter) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-14).dp)
                            .size(56.dp)
                            .shadow(10.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(EcoPrimary)
                            .clickable { onNavigate(tab.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigate(tab.route) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (active) EcoPrimary else EcoMutedForeground,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) EcoPrimary else EcoMutedForeground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTopBar(
    locale: AppLocale,
    onSignOut: () -> Unit
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)

    Surface(color = EcoCard) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EcoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "F",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = t("adminMobileTitle"),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground
                    )
                }

                TextButton(onClick = onSignOut) {
                    Text(
                        text = t("adminSignOut"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoCoral
                    )
                }
            }
            HorizontalDivider(color = EcoBorder)
        }
    }
}

@Composable
private fun AdminBottomBar(
    currentRoute: String?,
    locale: AppLocale,
    onNavigate: (String) -> Unit
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)

    val tabs = listOf(
        NavTabItem(FlysticRoutes.ADMIN_DASHBOARD, t("routeDashboard"), Icons.Rounded.Dashboard),
        NavTabItem(FlysticRoutes.ADMIN_NEW, t("routeNewTransaction"), Icons.Rounded.AddCircleOutline),
        NavTabItem(FlysticRoutes.ADMIN_TRANSACTIONS, t("routeTransactions"), Icons.AutoMirrored.Rounded.ReceiptLong),
        NavTabItem(FlysticRoutes.ADMIN_REPORTS, t("routeReports"), Icons.Rounded.Assessment)
    )

    Surface(
        color = EcoCard.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, EcoBorder),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val active = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(tab.route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (active) EcoPrimary else EcoMutedForeground,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) EcoPrimary else EcoMutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
