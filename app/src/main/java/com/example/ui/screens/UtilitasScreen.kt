package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.util.LocationHelper
import com.example.ui.components.AiRobotAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.SfaViewModel
import com.example.ui.viewmodel.TransactionDialogState

@Composable
fun UtilitasScreen(
    viewModel: SfaViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val warungs by viewModel.warungs.collectAsState()
    val rutes by viewModel.rutes.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentGps by viewModel.currentGpsLocation.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()
    val pendingSyncCount = remember(warungs) { warungs.count { it.pendingAddressSync } }

    val gpsAccuracy = remember(currentGps) {
        if (currentGps.isAvailable) {
            "${currentGps.accuracyMeter.toInt().coerceAtLeast(3)} meter (Akurat)"
        } else {
            "Mencari Sinyal GPS..."
        }
    }

    val liveAddress = remember(currentGps) {
        if (currentGps.isAvailable) {
            LocationHelper.reverseGeocode(context, currentGps.latitude, currentGps.longitude)
        } else {
            "Sensor GPS Aktif (Menunggu kunci satelit...)"
        }
    }
    var showExportSuccessDialog by remember { mutableStateOf<String?>(null) }
    var showMyMapsGuideDialog by remember { mutableStateOf(false) }

    var limitBonConfig by remember { mutableStateOf("Rp 500.000 / Outlet") }
    var gpsRadiusConfig by remember { mutableStateOf("< 20 meter") }
    var editConfigDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = com.example.util.AppStrings.tr("Menu Navigasi", "Navigation Menu", lang),
                            tint = Slate800,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = com.example.util.AppStrings.settingsTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = com.example.util.AppStrings.settingsSubtitle(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Slate200, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // Profile & Identity Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Slate900
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Slate900),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = userProfile?.namaSalesman?.ifBlank { com.example.util.AppStrings.tr("Belum Atur Akun Sales", "Sales Account Not Set", lang) } ?: com.example.util.AppStrings.tr("Belum Atur Akun Sales", "Sales Account Not Set", lang),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = userProfile?.namaDistributor?.ifBlank { com.example.util.AppStrings.tr("Distributor / Agen Mandiri", "Independent Distributor / Agent", lang) } ?: com.example.util.AppStrings.tr("Distributor / Agen Mandiri", "Independent Distributor / Agent", lang),
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.openTransactionDialog(TransactionDialogState.SetupProfile)
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(com.example.util.AppStrings.editProfile(lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (userProfile != null) {
                            HorizontalDivider(color = Slate100, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(com.example.util.AppStrings.tr("WhatsApp / HP:", "WhatsApp / Phone:", lang), color = Slate600, fontSize = 11.sp)
                                Text(userProfile!!.noHp.ifBlank { "-" }, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Slate900)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(com.example.util.AppStrings.tr("Kendaraan / Rayon:", "Vehicle / Region:", lang), color = Slate600, fontSize = 11.sp)
                                Text("${userProfile!!.platNomorMobil.ifBlank { com.example.util.AppStrings.tr("Mobil", "Vehicle", lang) }} • ${userProfile!!.areaOperasional.ifBlank { com.example.util.AppStrings.tr("Semua Area", "All Areas", lang) }}", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Slate900)
                            }
                        }
                    }
                }
            }

            // Language Switcher Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("language_switch_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Slate900
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF4F46E5).copy(alpha = 0.5f)))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF4F46E5).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = com.example.util.AppStrings.langSettingTitle(lang),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = com.example.util.AppStrings.langSettingDesc(lang),
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        // 2 Buttons / Switches for Bahasa Indonesia & English
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isIndo = lang.equals("ID", ignoreCase = true)
                            val isEng = lang.equals("EN", ignoreCase = true)

                            // Indonesian Option
                            Surface(
                                onClick = { viewModel.setAppLanguage("ID") },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isIndo) Slate900 else Slate50,
                                border = if (isIndo) null else CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lang_switch_id")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "🇮🇩", fontSize = 16.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Indonesia",
                                            fontSize = 12.sp,
                                            fontWeight = if (isIndo) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isIndo) Color.White else Slate800
                                        )
                                        Text(
                                            text = com.example.util.AppStrings.tr("Bahasa Utama", "Primary Language", lang),
                                            fontSize = 9.sp,
                                            color = if (isIndo) Slate300 else Slate400
                                        )
                                    }
                                    if (isIndo) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // English Option
                            Surface(
                                onClick = { viewModel.setAppLanguage("EN") },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEng) Slate900 else Slate50,
                                border = if (isEng) null else CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lang_switch_en")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "🇺🇸", fontSize = 16.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "English",
                                            fontSize = 12.sp,
                                            fontWeight = if (isEng) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isEng) Color.White else Slate800
                                        )
                                        Text(
                                            text = com.example.util.AppStrings.tr("Bahasa Inggris", "US International", lang),
                                            fontSize = 9.sp,
                                            color = if (isEng) Slate300 else Slate400
                                        )
                                    }
                                    if (isEng) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI Gateway & Copilot Engine Card
            item {
                val aiConfig by viewModel.aiConfig.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Slate900
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(EmeraldPrimary))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AiRobotAvatar(
                                    size = 40.dp,
                                    containerBackground = Slate900,
                                    accentColor = EmeraldPrimary
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "AI Gateway & Copilot",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Slate900
                                        )
                                        Surface(
                                            color = Slate100,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "OpenAI Compatible",
                                                color = EmeraldPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Model: ${aiConfig.model.ifBlank { "auto" }}",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.openTransactionDialog(TransactionDialogState.AiCopilot)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(com.example.util.AppStrings.tr("Buka Chat", "Open Chat", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(com.example.util.AppStrings.tr("Gateway Endpoint:", "Gateway Endpoint:", lang), color = Slate600, fontSize = 11.sp)
                            Text(
                                text = aiConfig.endpoint.take(32) + if (aiConfig.endpoint.length > 32) "..." else "",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = Slate900
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(com.example.util.AppStrings.tr("Sistem Persona:", "System Persona:", lang), color = Slate600, fontSize = 11.sp)
                            Text(
                                text = if (aiConfig.customPersona.isNotBlank()) com.example.util.AppStrings.tr("Kustom + Core Hardcoded", "Custom + Core Hardcoded", lang) else com.example.util.AppStrings.tr("Core Hardcoded SFA", "Core Hardcoded SFA", lang),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = EmeraldPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = com.example.util.AppStrings.tr(
                                        "Domain System Persona terlindungi: AI memahami aturan 4 laci mobil, piutang, dan setoran pabrik.",
                                        "Protected System Persona: AI understands 4 van stock drawers, debts, and supplier settlements.",
                                        lang
                                    ),
                                    fontSize = 10.sp,
                                    color = Color(0xFF166534),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.AiConfigSettings)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(com.example.util.AppStrings.tr("Konfigurasi Endpoint, API Key, Model & Persona", "Configure Endpoint, API Key, Model & Persona", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Offline-First & Geocoding Auto-Sync Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Slate900
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isOnline) EmeraldSurface else AmberSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                        contentDescription = null,
                                        tint = if (isOnline) EmeraldSuccess else AmberWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(com.example.util.AppStrings.networkModeTitle(lang), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                    Text(
                                        text = if (isOnline) com.example.util.AppStrings.networkOnlineDesc(lang) else com.example.util.AppStrings.networkOfflineDesc(lang),
                                        fontSize = 11.sp,
                                        color = if (isOnline) EmeraldSuccess else AmberWarning,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.syncPendingAddresses(silent = false) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOnline) Slate900 else Slate300,
                                    contentColor = Color.White
                                ),
                                enabled = !isSyncing,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(com.example.util.AppStrings.tr("Syncing...", "Syncing...", lang), fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(com.example.util.AppStrings.tr("Sync Alamat", "Sync Address", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(com.example.util.AppStrings.tr("Pending Sync Alamat GPS:", "Pending GPS Address Sync:", lang), color = Slate600, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (pendingSyncCount == 0) com.example.util.AppStrings.tr("Semua Terjemahan Beres (0)", "All Addresses Geocoded (0)", lang) else com.example.util.AppStrings.tr("$pendingSyncCount Outlet Menunggu Sync", "$pendingSyncCount Outlets Pending Sync", lang),
                                color = if (pendingSyncCount == 0) EmeraldSuccess else AmberWarning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = com.example.util.AppStrings.tr(
                                "Aplikasi bekerja penuh tanpa internet (Full Offline). Titik koordinat GPS tersimpan aman di database lokal HP. Saat online, koordinat otomatis diterjemahkan menjadi nama jalan.",
                                "Application is fully functional offline. GPS coordinates are securely stored in the phone's local database. When online, coordinates automatically reverse-geocode to street names.",
                                lang
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // GPS Checker Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Slate900
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Slate100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = null,
                                        tint = Slate800,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(com.example.util.AppStrings.tr("GPS Tracking & Anti-Fraud", "GPS Tracking & Anti-Fraud", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                    Text(com.example.util.AppStrings.tr("Auto-Lock Koordinat Aktif", "Auto-Lock Coordinates Active", lang), fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    viewModel.openTransactionDialog(TransactionDialogState.GpsTool)
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Slate100,
                                    contentColor = Slate900
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(com.example.util.AppStrings.tr("Cek Lokasi", "Check Location", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(com.example.util.AppStrings.tr("Akurasi Sensor:", "Sensor Accuracy:", lang), color = Slate600, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (currentGps.isAvailable) com.example.util.AppStrings.tr("${currentGps.accuracyMeter.toInt().coerceAtLeast(3)} meter (Akurat)", "${currentGps.accuracyMeter.toInt().coerceAtLeast(3)} meters (Accurate)", lang) else com.example.util.AppStrings.tr("Mencari Sinyal GPS...", "Searching GPS Signal...", lang),
                                color = if (currentGps.isAvailable) EmeraldSuccess else AmberWarning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "${com.example.util.AppStrings.tr("Lokasi terdeteksi:", "Detected location:", lang)} ${if (currentGps.isAvailable) liveAddress else com.example.util.AppStrings.tr("Sensor GPS Aktif (Menunggu kunci satelit...)", "GPS Sensor Active (Waiting for satellite lock...)", lang)} (${String.format(java.util.Locale.US, "%.5f, %.5f", currentGps.latitude, currentGps.longitude)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Google Maps & My Maps Integration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Slate900
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BlueBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                com.example.util.AppStrings.tr("INTEGRASI PETA & GOOGLE MAPS", "MAPS & GOOGLE MAPS INTEGRATION", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = BlueAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BlueSurface
                            ) {
                                Text(
                                    text = "${warungs.count { it.latitude != 0.0 }} ${com.example.util.AppStrings.tr("Titik GPS", "GPS Pins", lang)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        SettingTile(
                            icon = Icons.Default.Directions,
                            title = com.example.util.AppStrings.tr("Mulai Navigasi Keliling di Google Maps (Multi-Stop)", "Start Route Navigation in Google Maps (Multi-Stop)", lang),
                            subtitle = com.example.util.AppStrings.tr("Buka rute navigasi turn-by-turn langsung ke semua outlet aktif hari ini", "Open turn-by-turn navigation directly to all active outlets today", lang),
                            onClick = {
                                LocationHelper.openMultiStopGoogleMapsRoute(context, warungs, com.example.util.AppStrings.tr("Semua Outlet", "All Outlets", lang))
                            }
                        )

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        SettingTile(
                            icon = Icons.Default.PinDrop,
                            title = com.example.util.AppStrings.tr("Ekspor Peta ke Google My Maps (.KML)", "Export Map to Google My Maps (.KML)", lang),
                            subtitle = com.example.util.AppStrings.tr("Buat semua toko jadi pin penanda berwarna otomatis di akun Google Maps HP", "Turn all stores into colored map pins in your phone's Google Maps", lang),
                            onClick = {
                                val kmlContent = LocationHelper.generateWarungsKml(warungs, rutes)
                                LocationHelper.shareExportedMapFile(
                                    context = context,
                                    content = kmlContent,
                                    fileName = "outlet_sfa_konsinyasi.kml",
                                    mimeType = "application/vnd.google-earth.kml+xml"
                                )
                            }
                        )

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        SettingTile(
                            icon = Icons.Default.TableView,
                            title = com.example.util.AppStrings.tr("Ekspor Koordinat Lengkap (.CSV)", "Export Complete Coordinates (.CSV)", lang),
                            subtitle = com.example.util.AppStrings.tr("Tabel data toko + GPS (Lat/Lng) untuk diimpor ke spreadsheet / peta", "Store table + GPS (Lat/Lng) for spreadsheet / map import", lang),
                            onClick = {
                                val csvContent = LocationHelper.generateWarungsCsv(warungs, rutes)
                                LocationHelper.shareExportedMapFile(
                                    context = context,
                                    content = csvContent,
                                    fileName = "outlet_sfa_koordinat.csv",
                                    mimeType = "text/csv"
                                )
                            }
                        )

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        SettingTile(
                            icon = Icons.Default.HelpOutline,
                            title = com.example.util.AppStrings.tr("Panduan: Cara Munculkan Pin Toko di Google Maps", "Guide: How to Display Store Pins in Google Maps", lang),
                            subtitle = com.example.util.AppStrings.tr("3 langkah mudah import ke Google My Maps & otomatis tampil di Google Maps HP", "3 easy steps to import to Google My Maps & auto-display on phone Maps", lang),
                            onClick = {
                                showMyMapsGuideDialog = true
                            }
                        )
                    }
                }
            }

            // Backup & Export Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Slate900
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(com.example.util.AppStrings.tr("DATA BACKUP & EXPORT (MIGRASI HP)", "DATA BACKUP & EXPORT (PHONE MIGRATION)", lang), style = MaterialTheme.typography.labelSmall, color = Slate600, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

                        SettingTile(
                            icon = Icons.Default.CloudUpload,
                            title = com.example.util.AppStrings.tr("Ekspor Cadangan & Migrasi HP (.ZIP / .JSON)", "Backup Export & Phone Migration (.ZIP / .JSON)", lang),
                            subtitle = com.example.util.AppStrings.tr("Paket pindah HP lengkap beserta foto toko, atau cadangan modular JSON", "Full phone migration package with store photos, or modular JSON backup", lang),
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.ExportBackup)
                            }
                        )

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        SettingTile(
                            icon = Icons.Default.CloudDownload,
                            title = com.example.util.AppStrings.tr("Pulihkan Cadangan & Migrasi HP (Restore)", "Restore Backup & Phone Migration", lang),
                            subtitle = com.example.util.AppStrings.tr("Muat berkas .ZIP (ekstrak foto otomatis) atau berkas .JSON dari HP lain", "Load .ZIP (auto-extracts photos) or .JSON file from another phone", lang),
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.ImportBackup)
                            }
                        )

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        SettingTile(
                            icon = Icons.Default.FileDownload,
                            title = com.example.util.AppStrings.tr("Export Rekap Laporan (Excel / CSV)", "Export Report Summary (Excel / CSV)", lang),
                            subtitle = com.example.util.AppStrings.tr("Data transaksi harian, setoran pabrik, dan aging piutang", "Daily transactions, factory settlements, and debt aging data", lang),
                            onClick = { showExportSuccessDialog = com.example.util.AppStrings.tr("File laporan Excel/CSV berhasil diexport ke folder Downloads.", "Excel/CSV report file successfully exported to Downloads folder.", lang) }
                        )

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        SettingTile(
                            icon = Icons.Default.PictureAsPdf,
                            title = com.example.util.AppStrings.tr("Export Struk & Rekonsiliasi (PDF)", "Export Receipts & Reconciliation (PDF)", lang),
                            subtitle = com.example.util.AppStrings.tr("Dokumen siap cetak untuk kantor distributor", "Print-ready documents for distributor office", lang),
                            onClick = { showExportSuccessDialog = com.example.util.AppStrings.tr("Dokumen PDF rekap kas & faktur berhasil digenerate.", "PDF summary & invoice documents generated successfully.", lang) }
                        )

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        SettingTile(
                            icon = Icons.Default.RestartAlt,
                            title = com.example.util.AppStrings.tr("Reset Data Operasional (Mode Pre-Production)", "Reset Operational Data (Pre-Production Mode)", lang),
                            subtitle = com.example.util.AppStrings.tr("Kosongkan riwayat transaksi, laci stok, dan saldo piutang untuk mulai dari nol", "Clear transaction history, stock drawers, and debt balances to start fresh", lang),
                            onClick = { showExportSuccessDialog = "RESET_CONFIRM" }
                        )

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        SettingTile(
                            icon = Icons.Default.DeleteForever,
                            title = com.example.util.AppStrings.tr("Wipe Database Total (Format Bersih 100%)", "Wipe Entire Database (100% Clean Format)", lang),
                            subtitle = com.example.util.AppStrings.tr("Hapus seluruh master warung, produk, rute, pabrik & transaksi untuk input data riil", "Delete all master stores, products, routes, suppliers & transactions for real data entry", lang),
                            onClick = { showExportSuccessDialog = "WIPE_ALL_CONFIRM" }
                        )
                    }
                }
            }

            // Business Policy Configuration with Quick Edit
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Slate900
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(com.example.util.AppStrings.tr("KONFIGURASI BISNIS & ATURAN", "BUSINESS RULES & POLICIES", lang), style = MaterialTheme.typography.labelSmall, color = Slate600, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    editConfigDialog = com.example.util.AppStrings.tr("Limit Bon Default", "Default Credit Limit", lang) to limitBonConfig
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(com.example.util.AppStrings.tr("Batas Bon Default", "Default Credit Limit", lang), color = Slate700, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(com.example.util.AppStrings.tr("Ketuk untuk mengubah limit", "Tap to modify limit", lang), color = Slate500, fontSize = 10.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(limitBonConfig, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberWarning)
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                            }
                        }

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    editConfigDialog = com.example.util.AppStrings.tr("Radius Validasi GPS", "GPS Validation Radius", lang) to gpsRadiusConfig
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(com.example.util.AppStrings.tr("Ambang Batas Radius GPS", "GPS Radius Threshold", lang), color = Slate700, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(com.example.util.AppStrings.tr("Jarak maksimal validasi lokasi outlet", "Max distance for outlet location validation", lang), color = Slate500, fontSize = 10.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(gpsRadiusConfig, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldSuccess)
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Config Edit Dialog
    if (editConfigDialog != null) {
        val (configTitle, configVal) = editConfigDialog!!
        var tempVal by remember { mutableStateOf(configVal) }

        AlertDialog(
            onDismissRequest = { editConfigDialog = null },
            title = { Text(com.example.util.AppStrings.tr("Ubah $configTitle", "Change $configTitle", lang), fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                OutlinedTextField(
                    value = tempVal,
                    onValueChange = { tempVal = it },
                    label = { Text(com.example.util.AppStrings.tr("Nilai Baru", "New Value", lang)) },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (configTitle.contains("Limit", ignoreCase = true) || configTitle.contains("Bon", ignoreCase = true)) {
                            limitBonConfig = tempVal
                        } else {
                            gpsRadiusConfig = tempVal
                        }
                        editConfigDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                ) {
                    Text(com.example.util.AppStrings.btnSave(lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editConfigDialog = null }) {
                    Text(com.example.util.AppStrings.btnCancel(lang), color = Slate700)
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        )
    }

    if (showExportSuccessDialog != null) {
        if (showExportSuccessDialog == "RESET_CONFIRM") {
            AlertDialog(
                onDismissRequest = { showExportSuccessDialog = null },
                title = { Text(com.example.util.AppStrings.tr("Konfirmasi Reset Pre-Production", "Pre-Production Reset Confirmation", lang), fontWeight = FontWeight.Bold, color = Slate900) },
                text = {
                    Text(
                        com.example.util.AppStrings.tr(
                            "Apakah Anda yakin ingin mengosongkan seluruh riwayat transaksi harian, mengembalikan saldo piutang warung ke Rp 0, dan mereset laci stok mobil ke 0 untuk memulai operasional nyata dari nol?",
                            "Are you sure you want to clear all daily transaction history, reset outlet debts to Rp 0, and clear car stock drawers to start fresh for real production operations?",
                            lang
                        ),
                        color = Slate700,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetDataForProduction()
                            showExportSuccessDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                    ) {
                        Text(com.example.util.AppStrings.tr("Ya, Kosongkan Data", "Yes, Reset Data", lang), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showExportSuccessDialog = null }) {
                        Text(com.example.util.AppStrings.btnCancel(lang), color = Slate700)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White
            )
        } else if (showExportSuccessDialog == "WIPE_ALL_CONFIRM") {
            AlertDialog(
                onDismissRequest = { showExportSuccessDialog = null },
                title = { Text(com.example.util.AppStrings.tr("Wipe Total Database (100% Kosong)", "Wipe Entire Database (100% Clean)", lang), fontWeight = FontWeight.Bold, color = RoseDanger) },
                text = {
                    Text(
                        com.example.util.AppStrings.tr(
                            "PERINGATAN: Tindakan ini akan menghapus SELURUH data di aplikasi (Semua Warung, Produk, Rute Jalur, Pabrik Supplier, Laci Stok, dan Transaksi). Anda akan mulai dari database kosong bersih untuk memasukkan data riil perusahaan. Lanjutkan?",
                            "WARNING: This action will delete ALL data in the application (All Outlets, Products, Routes, Suppliers, Stock Drawers, and Transactions). You will start with an empty clean database for company real data entry. Continue?",
                            lang
                        ),
                        color = Slate700,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.wipeAllMasterAndTransactionalData()
                            showExportSuccessDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                    ) {
                        Text(com.example.util.AppStrings.tr("Wipe Bersih Total", "Wipe Entire Database", lang), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showExportSuccessDialog = null }) {
                        Text(com.example.util.AppStrings.btnCancel(lang), color = Slate700)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White
            )
        } else {
            AlertDialog(
                onDismissRequest = { showExportSuccessDialog = null },
                title = { Text(com.example.util.AppStrings.tr("Operasi Berhasil", "Operation Successful", lang), fontWeight = FontWeight.Bold, color = Slate900) },
                text = { Text(showExportSuccessDialog ?: "", color = Slate700) },
                confirmButton = {
                    Button(
                        onClick = { showExportSuccessDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text(com.example.util.AppStrings.btnClose(lang), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White
            )
        }
    }

    if (showMyMapsGuideDialog) {
        AlertDialog(
            onDismissRequest = { showMyMapsGuideDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PinDrop, contentDescription = null, tint = BlueAccent)
                    Text(com.example.util.AppStrings.tr("Panduan Google My Maps", "Google My Maps Guide", lang), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        com.example.util.AppStrings.tr(
                            "Untuk memunculkan seluruh titik warung sebagai penanda (pin) di Google Maps HP Anda, ikuti langkah berikut:",
                            "To display all outlet points as colored pins on your phone's Google Maps, follow these steps:",
                            lang
                        ),
                        fontSize = 12.sp,
                        color = Slate700
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BlueSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BlueBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(com.example.util.AppStrings.tr("1. Ekspor Berkas KML / CSV", "1. Export KML / CSV File", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueAccent)
                            Text(com.example.util.AppStrings.tr("Klik tombol 'Ekspor Peta ke Google My Maps (.KML)' atau '.CSV' lalu kirim ke WhatsApp / Google Drive Anda.", "Click 'Export Map to Google My Maps (.KML)' or '.CSV' and share to your WhatsApp / Google Drive.", lang), fontSize = 11.sp, color = Slate700)

                            HorizontalDivider(color = BlueBorder)

                            Text(com.example.util.AppStrings.tr("2. Buka mymaps.google.com", "2. Open mymaps.google.com", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueAccent)
                            Text(com.example.util.AppStrings.tr("Buka browser di HP/Laptop -> kunjungi mymaps.google.com -> Login dengan akun Google yang sama dengan akun Google Maps di HP Anda -> Klik 'Buat Peta Baru' -> Klik 'Impor' -> Pilih berkas KML / CSV tadi.", "Open browser -> visit mymaps.google.com -> Login with your Google account -> Click 'Create a New Map' -> Click 'Import' -> Choose the exported KML / CSV file.", lang), fontSize = 11.sp, color = Slate700)

                            HorizontalDivider(color = BlueBorder)

                            Text(com.example.util.AppStrings.tr("3. Buka Aplikasi Google Maps di HP", "3. Open Google Maps App on Phone", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueAccent)
                            Text(com.example.util.AppStrings.tr("Buka Google Maps -> Tab 'Tersimpan' (Saved) -> Pilih 'Peta' (Maps) -> Pilih peta yang baru dibuat. Seluruh warung otomatis muncul sebagai pin berwarna lengkap dengan nama & nomor HP!", "Open Google Maps -> 'Saved' tab -> Tap 'Maps' -> Select the newly created map. All outlets automatically appear as colored pins complete with store name & phone!", lang), fontSize = 11.sp, color = Slate700)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMyMapsGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                ) {
                    Text(com.example.util.AppStrings.tr("Mengerti", "Got it", lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun SettingTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Slate100),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Slate900, fontSize = 12.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate500, fontSize = 10.sp)
        }

        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
    }
}

