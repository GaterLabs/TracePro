package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppDialogsHost
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppNavScreen
import com.example.ui.viewmodel.SfaViewModel
import com.example.ui.viewmodel.TransactionDialogState
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: SfaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SfaMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SfaMainApp(
    viewModel: SfaViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackbarMessage by viewModel.feedbackSnackbar.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val warungs by viewModel.warungs.collectAsState()
    val currentGps by viewModel.currentGpsLocation.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var hasPromptedFirstOpenProfile by remember { mutableStateOf(false) }

    val allRequiredPermissions = remember {
        buildList {
            add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            add(android.Manifest.permission.CAMERA)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
                add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    var locationPermissionGranted by remember { mutableStateOf(true) }
    var cameraPermissionGranted by remember { mutableStateOf(true) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        locationPermissionGranted = fineGranted || coarseGranted
        cameraPermissionGranted = permissions[android.Manifest.permission.CAMERA] ?: false

        if (locationPermissionGranted) {
            viewModel.refreshGpsLocation()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(allRequiredPermissions)
    }

    LaunchedEffect(userProfile) {
        if (!hasPromptedFirstOpenProfile && userProfile != null && !userProfile!!.isConfigured && userProfile!!.namaSalesman.isBlank()) {
            hasPromptedFirstOpenProfile = true
            viewModel.openTransactionDialog(TransactionDialogState.SetupProfile)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                viewModel.dismissSnackbar()
            }
        }
    }

    CompositionLocalProvider(com.example.util.LocalAppLanguage provides lang) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color.White,
                    drawerContentColor = Slate900,
                    modifier = Modifier.width(310.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Drawer Header: Profile Info
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate900)
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_app_logo_1787113131508),
                                            contentDescription = "Logo TracerPro",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldSuccess.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(EmeraldSuccess)
                                            )
                                            Text(
                                                text = com.example.util.AppStrings.tr("OFFLINE AKTIF", "OFFLINE ACTIVE", lang),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldSuccess
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        text = userProfile?.namaSalesman?.ifBlank { com.example.util.AppStrings.tr("Salesman FMCG", "FMCG Salesman", lang) } ?: com.example.util.AppStrings.tr("Salesman FMCG", "FMCG Salesman", lang),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${userProfile?.namaDistributor?.ifBlank { com.example.util.AppStrings.tr("Distributor Mandiri", "Independent Distributor", lang) } ?: com.example.util.AppStrings.tr("Distributor Mandiri", "Independent Distributor", lang)} • ${userProfile?.platNomorMobil?.ifBlank { com.example.util.AppStrings.tr("Mobil Canvas", "Van Canvas", lang) } ?: com.example.util.AppStrings.tr("Mobil Canvas", "Van Canvas", lang)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📍 Lat: ${String.format(Locale.US, "%.3f", currentGps.latitude)}, Lng: ${String.format(Locale.US, "%.3f", currentGps.longitude)}",
                                        fontSize = 10.sp,
                                        color = Slate400
                                    )
                                    TextButton(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            viewModel.openTransactionDialog(TransactionDialogState.SetupProfile)
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(com.example.util.AppStrings.tr("Ubah Profil", "Edit Profile", lang), color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Navigation Menu Header
                    Text(
                        text = com.example.util.AppStrings.tr("NAVIGASI UTAMA", "MAIN NAVIGATION", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    // Navigation Items
                    DrawerNavTile(
                        icon = Icons.Default.Storefront,
                        title = com.example.util.AppStrings.navTransaksi(lang),
                        subtitle = com.example.util.AppStrings.subTransaksi(lang),
                        isSelected = currentScreen == AppNavScreen.TRANSAKSI,
                        badge = "${warungs.size} Outlet",
                        onClick = {
                            viewModel.setScreen(AppNavScreen.TRANSAKSI)
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerNavTile(
                        icon = Icons.Default.History,
                        title = com.example.util.AppStrings.navRiwayat(lang),
                        subtitle = com.example.util.AppStrings.subRiwayat(lang),
                        isSelected = currentScreen == AppNavScreen.RIWAYAT,
                        onClick = {
                            viewModel.setScreen(AppNavScreen.RIWAYAT)
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerNavTile(
                        icon = Icons.Default.Dashboard,
                        title = com.example.util.AppStrings.navDashboard(lang),
                        subtitle = com.example.util.AppStrings.subDashboard(lang),
                        isSelected = currentScreen == AppNavScreen.DASHBOARD,
                        onClick = {
                            viewModel.setScreen(AppNavScreen.DASHBOARD)
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerNavTile(
                        icon = Icons.Default.Inventory2,
                        title = com.example.util.AppStrings.navMaster(lang),
                        subtitle = com.example.util.AppStrings.subMaster(lang),
                        isSelected = currentScreen == AppNavScreen.MASTER_DATA,
                        onClick = {
                            viewModel.setScreen(AppNavScreen.MASTER_DATA)
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerNavTile(
                        icon = Icons.Default.Assessment,
                        title = com.example.util.AppStrings.navLaporan(lang),
                        subtitle = com.example.util.AppStrings.subLaporan(lang),
                        isSelected = currentScreen == AppNavScreen.LAPORAN,
                        onClick = {
                            viewModel.setScreen(AppNavScreen.LAPORAN)
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerNavTile(
                        icon = Icons.Default.Settings,
                        title = com.example.util.AppStrings.navUtilitas(lang),
                        subtitle = com.example.util.AppStrings.subUtilitas(lang),
                        isSelected = currentScreen == AppNavScreen.UTILITAS,
                        onClick = {
                            viewModel.setScreen(AppNavScreen.UTILITAS)
                            scope.launch { drawerState.close() }
                        }
                    )

                    HorizontalDivider(color = Slate200, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Quick Actions / Utilities
                    Text(
                        text = com.example.util.AppStrings.tr("AKSI CEPAT LAPANGAN", "QUICK FIELD ACTIONS", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    DrawerActionTile(
                        icon = Icons.Default.LocalShipping,
                        title = com.example.util.AppStrings.actionMuatPagi(lang),
                        iconTint = Color(0xFF4F46E5),
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.openTransactionDialog(TransactionDialogState.MuatPagi)
                        }
                    )

                    DrawerActionTile(
                        icon = Icons.Default.Autorenew,
                        title = com.example.util.AppStrings.actionSortirBs(lang),
                        iconTint = AmberWarning,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.openTransactionDialog(TransactionDialogState.SortirBs)
                        }
                    )

                    DrawerActionTile(
                        icon = Icons.Default.CloudUpload,
                        title = com.example.util.AppStrings.actionExportBackup(lang),
                        iconTint = Slate800,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.openTransactionDialog(TransactionDialogState.ExportBackup)
                        }
                    )

                    DrawerActionTile(
                        icon = Icons.Default.CloudDownload,
                        title = com.example.util.AppStrings.actionImportBackup(lang),
                        iconTint = Slate800,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.openTransactionDialog(TransactionDialogState.ImportBackup)
                        }
                    )

                    DrawerActionTile(
                        icon = Icons.Default.GpsFixed,
                        title = com.example.util.AppStrings.actionGpsTool(lang),
                        iconTint = EmeraldSuccess,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.openTransactionDialog(TransactionDialogState.GpsTool)
                        }
                    )

                    DrawerActionTile(
                        icon = Icons.Default.ReceiptLong,
                        title = com.example.util.AppStrings.actionClosingSore(lang),
                        iconTint = RoseDanger,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.openTransactionDialog(TransactionDialogState.ClosingSore)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Version Footer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "TracerPro - SFA FMCG & Konsinyasi v4.0.0\n100% Offline-Ready SQLite Room",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Column {
                    HorizontalDivider(color = Slate200, thickness = 1.dp)
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        val navItems = listOf(
                            Triple(AppNavScreen.TRANSAKSI, Icons.Filled.Storefront to Icons.Outlined.Storefront, com.example.util.AppStrings.navTransaksi(lang)),
                            Triple(AppNavScreen.RIWAYAT, Icons.Filled.History to Icons.Outlined.History, com.example.util.AppStrings.navRiwayat(lang)),
                            Triple(AppNavScreen.DASHBOARD, Icons.Filled.Dashboard to Icons.Outlined.Dashboard, com.example.util.AppStrings.navDashboard(lang)),
                            Triple(AppNavScreen.MASTER_DATA, Icons.Filled.Inventory2 to Icons.Outlined.Inventory2, com.example.util.AppStrings.navMaster(lang)),
                            Triple(AppNavScreen.LAPORAN, Icons.Filled.Assessment to Icons.Outlined.Assessment, com.example.util.AppStrings.navLaporan(lang))
                        )

                        navItems.forEach { (screen, icons, label) ->
                            val isSelected = currentScreen == screen
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.setScreen(screen) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) icons.first else icons.second,
                                        contentDescription = label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Slate950,
                                    selectedTextColor = Slate950,
                                    unselectedIconColor = Slate600,
                                    unselectedTextColor = Slate600,
                                    indicatorColor = Slate200
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val openDrawerAction = { scope.launch { drawerState.open() } }

                AnimatedContent(
                    targetState = currentScreen,
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        AppNavScreen.TRANSAKSI -> TransaksiScreen(viewModel = viewModel, onOpenDrawer = { openDrawerAction() })
                        AppNavScreen.RIWAYAT -> RiwayatScreen(viewModel = viewModel, onOpenDrawer = { openDrawerAction() })
                        AppNavScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel, onOpenDrawer = { openDrawerAction() })
                        AppNavScreen.MASTER_DATA -> MasterDataScreen(viewModel = viewModel, onOpenDrawer = { openDrawerAction() })
                        AppNavScreen.LAPORAN -> LaporanScreen(viewModel = viewModel, onOpenDrawer = { openDrawerAction() })
                        AppNavScreen.UTILITAS -> UtilitasScreen(viewModel = viewModel, onOpenDrawer = { openDrawerAction() })
                    }
                }

                // Dialog Host
                AppDialogsHost(viewModel = viewModel)
            }
        }
    }
}
}

@Composable
fun DrawerNavTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Slate900 else Color.Transparent,
        border = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Slate600,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else Slate800,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) Slate300 else Slate500,
                    fontSize = 10.sp
                )
            }

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Slate200
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerActionTile(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Slate800,
            fontSize = 12.sp
        )
    }
}
