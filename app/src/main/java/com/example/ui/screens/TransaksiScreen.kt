package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.WarungEntity
import com.example.ui.components.RiskAgingBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.OutletFilterAging
import com.example.ui.viewmodel.OutletSortBy
import com.example.ui.viewmodel.SfaViewModel
import com.example.ui.viewmodel.TransactionDialogState
import com.example.util.AppStrings
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaksiScreen(
    viewModel: SfaViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val warungs by viewModel.warungs.collectAsState()
    val rutes by viewModel.rutes.collectAsState()
    val selectedRuteId by viewModel.selectedRuteId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val currentGps by viewModel.currentGpsLocation.collectAsState()
    val outletSortBy by viewModel.outletSortBy.collectAsState()
    val outletFilterAging by viewModel.outletFilterAging.collectAsState()
    val customMinDaysFilter by viewModel.customMinDaysFilter.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCustomDaysDialog by remember { mutableStateOf(false) }
    var showTopActionMenu by remember { mutableStateOf(false) }

    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayVisitedWarungIds = remember(transactions) {
        transactions.filter { it.tanggal == todayKey }.map { it.warungId }.toSet()
    }

    val warungSalesMap = remember(transactions) {
        transactions.groupBy { it.warungId }.mapValues { entry -> entry.value.sumOf { tx -> tx.subtotalLaku } }
    }

    // Process warungs: calculate distance, days since last visit, filter and sort
    val processedWarungs = remember(
        warungs,
        selectedRuteId,
        searchQuery,
        currentGps,
        outletSortBy,
        outletFilterAging,
        customMinDaysFilter,
        todayVisitedWarungIds,
        warungSalesMap
    ) {
        val now = System.currentTimeMillis()

        // 1. Filtering
        val filtered = warungs.filter { warung ->
            val matchesRute = selectedRuteId == null || warung.ruteId == selectedRuteId
            val matchesSearch = searchQuery.isBlank() ||
                    warung.namaWarung.contains(searchQuery, ignoreCase = true) ||
                    warung.namaPemilik.contains(searchQuery, ignoreCase = true) ||
                    warung.alamatLengkap.contains(searchQuery, ignoreCase = true)

            val isVisitedToday = todayVisitedWarungIds.contains(warung.id)
            val daysSinceVisit = if (warung.tglKunjunganTerakhir > 0) {
                ((now - warung.tglKunjunganTerakhir) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            } else 999

            val matchesAging = when (outletFilterAging) {
                OutletFilterAging.SEMUA -> true
                OutletFilterAging.BELUM_HARI_INI -> !isVisitedToday
                OutletFilterAging.LEBIH_3_HARI -> daysSinceVisit >= 3
                OutletFilterAging.LEBIH_7_HARI -> daysSinceVisit >= 7
                OutletFilterAging.LEBIH_14_HARI -> daysSinceVisit >= 14
                OutletFilterAging.LEBIH_30_HARI -> daysSinceVisit >= 30
                OutletFilterAging.KUSTOM_HARI -> daysSinceVisit >= (customMinDaysFilter ?: 7)
                OutletFilterAging.SUDAH_HARI_INI -> isVisitedToday
            }

            matchesRute && matchesSearch && matchesAging
        }

        // 2. Sorting: Outlets filled/visited today automatically sink to the bottom!
        filtered.sortedWith(
            compareBy<WarungEntity> { warung ->
                if (todayVisitedWarungIds.contains(warung.id)) 1 else 0
            }.thenComparator { a, b ->
                when (outletSortBy) {
                    OutletSortBy.TERDEKAT_GPS -> {
                        if (currentGps.isAvailable && a.latitude != 0.0 && b.latitude != 0.0) {
                            val distA = LocationHelper.calculateDistanceMeters(currentGps.latitude, currentGps.longitude, a.latitude, a.longitude)
                            val distB = LocationHelper.calculateDistanceMeters(currentGps.latitude, currentGps.longitude, b.latitude, b.longitude)
                            distA.compareTo(distB)
                        } else {
                            a.urutanKunjungan.compareTo(b.urutanKunjungan)
                        }
                    }
                    OutletSortBy.URUTAN_RUTE -> {
                        a.urutanKunjungan.compareTo(b.urutanKunjungan)
                    }
                    OutletSortBy.LAMA_TIDAK_DIKUNJUNGI -> {
                        // Oldest last visit first (or never visited 0 first)
                        a.tglKunjunganTerakhir.compareTo(b.tglKunjunganTerakhir)
                    }
                    OutletSortBy.OMSET_TERBESAR -> {
                        val salesA = warungSalesMap[a.id] ?: 0.0
                        val salesB = warungSalesMap[b.id] ?: 0.0
                        salesB.compareTo(salesA)
                    }
                    OutletSortBy.PIUTANG_TERBESAR -> {
                        b.saldoPiutang.compareTo(a.saldoPiutang)
                    }
                    OutletSortBy.STOK_MENIPIS -> {
                        a.stokTitipanPcs.compareTo(b.stokTitipanPcs)
                    }
                    OutletSortBy.NAMA_AZ -> {
                        a.namaWarung.compareTo(b.namaWarung, ignoreCase = true)
                    }
                }
            }
        )
    }

    if (showCustomDaysDialog) {
        var inputDaysText by remember { mutableStateOf((customMinDaysFilter ?: 7).toString()) }
        AlertDialog(
            onDismissRequest = { showCustomDaysDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = Slate900)
                    Text(AppStrings.tr("Filter Kustom Hari Kunjungan", "Custom Visit Days Filter", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = AppStrings.tr("Tampilkan hanya outlet yang belum dikunjungi / restock minimal sekian hari:", "Show only outlets not visited / restocked for at least this many days:", lang),
                        fontSize = 12.sp,
                        color = Slate600
                    )

                    OutlinedTextField(
                        value = inputDaysText,
                        onValueChange = { inputDaysText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(AppStrings.tr("Jumlah Hari Minimal", "Minimum Days", lang)) },
                        placeholder = { Text(AppStrings.tr("Contoh: 10", "Example: 10", lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(AppStrings.tr("Preset Cepat:", "Quick Presets:", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3, 5, 7, 10, 14, 21, 30, 60).forEach { preset ->
                            SuggestionChip(
                                onClick = { inputDaysText = preset.toString() },
                                label = { Text("$preset ${AppStrings.tr("Hari", "Days", lang)}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputDaysText.toIntOrNull() ?: 7
                        viewModel.setCustomMinDaysFilter(parsed)
                        showCustomDaysDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                ) {
                    Text(AppStrings.tr("Terapkan Filter", "Apply Filter", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDaysDialog = false }) {
                    Text(AppStrings.tr("Batal", "Cancel", lang))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openTransactionDialog(TransactionDialogState.AddEditWarung(null)) },
                containerColor = Slate900,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(20.dp)) },
                text = { Text(AppStrings.tr("Tambah Outlet", "Add Outlet", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_warung_transaksi")
            )
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Streamlined Screen Header
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
                                contentDescription = AppStrings.tr("Menu Navigasi", "Navigation Menu", lang),
                                tint = Slate800,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = AppStrings.tr("Kunjungan & Transaksi", "Visits & Transactions", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "${processedWarungs.size} ${AppStrings.tr("Outlet • Kunjungan & Restock", "Outlets • Visits & Restock", lang)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Top Bar Right Actions: GPS Status Pill + Action Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // GPS Quick Status / Sync Pill
                        Surface(
                            onClick = { viewModel.refreshGpsLocation() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentGps.isAvailable) EmeraldSuccess.copy(alpha = 0.12f) else AmberWarning.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (currentGps.isAvailable) EmeraldSuccess.copy(alpha = 0.4f) else AmberWarning
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (currentGps.isAvailable) EmeraldSuccess else AmberWarning)
                                )
                                Text(
                                    text = if (currentGps.isAvailable) "GPS Live" else AppStrings.tr("Sinkron GPS", "Sync GPS", lang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentGps.isAvailable) Color(0xFF047857) else Color(0xFFB45309)
                                )
                            }
                        }

                        // Consolidated Action Menu (Neat & spacious)
                        Box {
                            IconButton(
                                onClick = { showTopActionMenu = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate100)
                                    .testTag("btn_top_action_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = AppStrings.tr("Menu Aksi Cepat", "Quick Actions", lang),
                                    tint = Slate800,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showTopActionMenu,
                                onDismissRequest = { showTopActionMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                            DropdownMenuItem(
                                text = { Text(AppStrings.tr("Tambah Outlet Baru", "Add New Outlet", lang), fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Slate900, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showTopActionMenu = false
                                    viewModel.openTransactionDialog(TransactionDialogState.AddEditWarung(null))
                                }
                            )

                            HorizontalDivider(color = Slate100)

                            DropdownMenuItem(
                                text = { Text("${AppStrings.tr("Mulai Navigasi Rute", "Start Route Navigation", lang)} (${processedWarungs.count { it.latitude != 0.0 }} ${AppStrings.tr("Toko", "Stores", lang)})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BlueAccent) },
                                leadingIcon = {
                                    Icon(Icons.Default.Directions, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showTopActionMenu = false
                                    LocationHelper.openMultiStopGoogleMapsRoute(context, processedWarungs, AppStrings.tr("Rute Kunjungan", "Visit Route", lang))
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(AppStrings.tr("Ekspor Pin Peta (.KML My Maps)", "Export Map Pins (.KML My Maps)", lang), fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.PinDrop, contentDescription = null, tint = Slate700, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showTopActionMenu = false
                                    val kml = LocationHelper.generateWarungsKml(processedWarungs, rutes)
                                    LocationHelper.shareExportedMapFile(context, kml, "rute_outlet.kml", "application/vnd.google-earth.kml+xml")
                                }
                            )

                            HorizontalDivider(color = Slate100)

                            DropdownMenuItem(
                                text = { Text(AppStrings.tr("Muat Pagi (Loading Pack)", "Morning Load (Loading Pack)", lang), fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Slate700, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showTopActionMenu = false
                                    viewModel.openTransactionDialog(TransactionDialogState.MuatPagi)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(AppStrings.tr("Sortir Retur & Repack", "Sort Returns & Repack", lang), fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = Slate700, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showTopActionMenu = false
                                    viewModel.openTransactionDialog(TransactionDialogState.SortirBs)
                                }
                            )
                        }
                    }
                }
            }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("search_warung_input"),
                    placeholder = {
                        Text(
                            AppStrings.tr("Cari nama outlet, pemilik, atau alamat...", "Search outlet name, owner, or address...", lang),
                            fontSize = 13.sp,
                            color = Slate400
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Slate500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = appTextFieldColors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        unfocusedBorderColor = Slate200,
                        focusedBorderColor = Slate900
                    )
                )

                // Horizontal Route Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedRuteId == null,
                        onClick = { viewModel.setSelectedRute(null) },
                        label = {
                            Text(
                                text = AppStrings.tr("Semua Rute", "All Routes", lang),
                                fontSize = 12.sp,
                                fontWeight = if (selectedRuteId == null) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate900,
                            selectedLabelColor = Color.White,
                            containerColor = Slate100,
                            labelColor = Slate600
                        ),
                        border = null
                    )

                    rutes.forEach { rute ->
                        val isSelected = selectedRuteId == rute.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedRute(rute.id) },
                            label = {
                                Text(
                                    text = "${rute.namaRute.split("-").first().trim()} (${rute.hariKunjungan})",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate900,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate600
                            ),
                            border = null
                        )
                    }
                }

                // Sorting & Aging Filter Selector Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort Dropdown Button
                    Box {
                        Surface(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (outletSortBy == OutletSortBy.TERDEKAT_GPS) Icons.Default.NearMe else Icons.Default.Sort,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = AppStrings.getSortLabel(outletSortBy, lang),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            OutletSortBy.values().forEach { sortBy ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (outletSortBy == sortBy) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                            } else {
                                                Spacer(modifier = Modifier.width(16.dp))
                                            }
                                            Text(
                                                text = AppStrings.getSortLabel(sortBy, lang),
                                                fontWeight = if (outletSortBy == sortBy) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = Slate900,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setOutletSortBy(sortBy)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Aging Filter Chips
                    OutletFilterAging.values().forEach { agingFilter ->
                        val isSelected = outletFilterAging == agingFilter
                        val chipLabel = AppStrings.getAgingFilterLabel(agingFilter, lang, customMinDaysFilter)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (agingFilter == OutletFilterAging.KUSTOM_HARI) {
                                    showCustomDaysDialog = true
                                } else {
                                    viewModel.setOutletFilterAging(agingFilter)
                                }
                            },
                            label = {
                                Text(
                                    text = chipLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (agingFilter == OutletFilterAging.LEBIH_7_HARI || agingFilter == OutletFilterAging.LEBIH_14_HARI || agingFilter == OutletFilterAging.LEBIH_30_HARI || agingFilter == OutletFilterAging.KUSTOM_HARI) AmberSurface else Slate900,
                                selectedLabelColor = if (agingFilter == OutletFilterAging.LEBIH_7_HARI || agingFilter == OutletFilterAging.LEBIH_14_HARI || agingFilter == OutletFilterAging.LEBIH_30_HARI || agingFilter == OutletFilterAging.KUSTOM_HARI) AmberText else Color.White,
                                containerColor = Slate50,
                                labelColor = Slate600
                            ),
                            border = null,
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
        ) {
            // Summary and GPS Status banner
            item {
                val currentRuteObj = rutes.find { it.id == selectedRuteId }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${AppStrings.tr("MENAMPILKAN", "SHOWING", lang)} ${processedWarungs.size} ${AppStrings.tr("DARI", "OF", lang)} ${warungs.size} OUTLET",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        if (currentRuteObj != null) {
                            Text(
                                text = "${AppStrings.tr("Jalur", "Route", lang)} ${currentRuteObj.hariKunjungan}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate700,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // GPS Distance Info pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "GPS: Lat ${String.format(Locale.US, "%.4f", currentGps.latitude)}, Lng ${String.format(Locale.US, "%.4f", currentGps.longitude)}",
                                    fontSize = 10.sp,
                                    color = Slate700,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = if (todayVisitedWarungIds.isNotEmpty()) "${todayVisitedWarungIds.size} ${AppStrings.tr("Selesai Hari Ini", "Completed Today", lang)}" else AppStrings.tr("Belum Ada Kunjungan", "No Visits Yet", lang),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (todayVisitedWarungIds.isNotEmpty()) EmeraldText else Slate500
                            )
                        }
                    }
                }
            }

            if (processedWarungs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = AppStrings.tr("Tidak ada outlet yang cocok", "No matching outlets", lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Text(
                                text = AppStrings.tr("Coba ubah filter rute, status tempo kunjungan, atau kata kunci pencarian.", "Try changing route filter, visit aging status, or search query.", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    viewModel.setOutletFilterAging(OutletFilterAging.SEMUA)
                                    viewModel.setSelectedRute(null)
                                    viewModel.setSearchQuery("")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(AppStrings.tr("Reset Semua Filter", "Reset All Filters", lang), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(processedWarungs, key = { _, warung -> warung.id }, contentType = { _, _ -> "warung_card" }) { index, warung ->
                    val isVisitedToday = todayVisitedWarungIds.contains(warung.id)
                    val distanceMeters = if (currentGps.isAvailable && warung.latitude != 0.0 && warung.longitude != 0.0) {
                        LocationHelper.calculateDistanceMeters(
                            currentGps.latitude,
                            currentGps.longitude,
                            warung.latitude,
                            warung.longitude
                        )
                    } else {
                        Double.MAX_VALUE
                    }
                    val formattedDistance = if (currentGps.isAvailable && warung.latitude != 0.0) {
                        LocationHelper.formatDistance(distanceMeters)
                    } else if (warung.latitude == 0.0) {
                        "No GPS"
                    } else {
                        "GPS..."
                    }

                    val daysSinceVisit = if (warung.tglKunjunganTerakhir > 0) {
                        ((System.currentTimeMillis() - warung.tglKunjunganTerakhir) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                    } else 999

                    WarungOperationalCard(
                        warung = warung,
                        orderNumber = index + 1,
                        isVisitedToday = isVisitedToday,
                        formattedDistance = formattedDistance,
                        daysSinceVisit = daysSinceVisit,
                        lang = lang,
                        onNavigateGmaps = {
                            LocationHelper.openGoogleMapsNavigation(
                                context = context,
                                lat = warung.latitude,
                                lng = warung.longitude,
                                outletName = warung.namaWarung
                            )
                        },
                        onTitipBaru = {
                            viewModel.openTransactionDialog(TransactionDialogState.TitipBaru(warung))
                        },
                        onTarikSisa = {
                            viewModel.openTransactionDialog(TransactionDialogState.TarikSisa(warung))
                        },
                        onStatistics = {
                            viewModel.openTransactionDialog(TransactionDialogState.OutletStatistics(warung))
                        },
                        onDetail = {
                            viewModel.openTransactionDialog(TransactionDialogState.WarungDetail(warung))
                        },
                        onWriteOff = {
                            viewModel.openTransactionDialog(TransactionDialogState.WriteOff(warung))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WarungOperationalCard(
    warung: WarungEntity,
    orderNumber: Int,
    isVisitedToday: Boolean,
    formattedDistance: String,
    daysSinceVisit: Int,
    onNavigateGmaps: () -> Unit,
    onTitipBaru: () -> Unit,
    onTarikSisa: () -> Unit,
    onStatistics: () -> Unit,
    onDetail: () -> Unit,
    onWriteOff: () -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "ID"
) {
    val debtRatio = (warung.saldoPiutang / warung.limitHutangMaksimal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val isBlacklist = warung.status == "Blacklist"
    val daysSinceDebt = if (warung.saldoPiutang > 0) {
        ((System.currentTimeMillis() - warung.tglMulaiHutang) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    } else 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("warung_card_${warung.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isBlacklist -> Color(0xFFFFF1F2)
                isVisitedToday -> Slate50
                else -> Color.White
            },
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(
                when {
                    isBlacklist -> RoseBorder
                    isVisitedToday -> Slate300
                    else -> Slate200
                }
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Outlet Image + Urutan + Nama Warung + Status + Distance & Visited Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Outlet Photo with Urutan tag
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate100),
                    contentAlignment = Alignment.Center
                ) {
                    if (warung.fotoOutlet != null && warung.fotoOutlet.isNotBlank()) {
                        AsyncImage(
                            model = warung.fotoOutlet,
                            contentDescription = "Foto Outlet",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = if (isVisitedToday) Slate400 else Slate600,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 6.dp),
                        color = if (isVisitedToday) Slate600 else Slate900.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "#$orderNumber",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = warung.namaWarung,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isVisitedToday) Slate700 else Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (isBlacklist) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = RoseSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RoseBorder)
                            ) {
                                Text(
                                    text = "BLACKLIST",
                                    color = RoseText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isVisitedToday) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(10.dp))
                                    Text(
                                        text = AppStrings.tr("SELESAI HARI INI", "VISITED TODAY", lang),
                                        color = EmeraldText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            RiskAgingBadge(
                                daysOverdue = daysSinceDebt,
                                saldoPiutang = warung.saldoPiutang,
                                lang = lang
                            )
                        }
                    }

                    Text(
                        text = "${warung.namaPemilik.ifEmpty { "-" }} • ${warung.kategoriWarung}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        fontSize = 11.sp
                    )

                    // Visit aging & notes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aging badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when {
                                daysSinceVisit >= 14 -> Color(0xFFFEE2E2)
                                daysSinceVisit >= 7 -> Color(0xFFFEF3C7)
                                daysSinceVisit == 0 -> Color(0xFFECFDF5)
                                else -> Slate100
                            }
                        ) {
                            Text(
                                text = when {
                                    daysSinceVisit == 0 -> AppStrings.tr("Kunjungan: Hari ini", "Visit: Today", lang)
                                    daysSinceVisit >= 900 -> AppStrings.tr("Belum pernah dikunjungi", "Never visited", lang)
                                    daysSinceVisit >= 14 -> "${AppStrings.tr("⚠️ Kunjungan", "⚠️ Visit", lang)}: $daysSinceVisit ${AppStrings.tr("hari lalu (Kritis)", "days ago (Critical)", lang)}"
                                    daysSinceVisit >= 7 -> "${AppStrings.tr("⚠️ Kunjungan", "⚠️ Visit", lang)}: $daysSinceVisit ${AppStrings.tr("hari lalu (Tempo)", "days ago (Due)", lang)}"
                                    else -> "${AppStrings.tr("Kunjungan", "Visit", lang)}: $daysSinceVisit ${AppStrings.tr("hari lalu", "days ago", lang)}"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    daysSinceVisit >= 14 -> RoseDanger
                                    daysSinceVisit >= 7 -> AmberText
                                    daysSinceVisit == 0 -> EmeraldText
                                    else -> Slate600
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (warung.notes.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = AmberText, modifier = Modifier.size(11.dp))
                            Text(
                                text = warung.notes,
                                fontSize = 10.sp,
                                color = Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Alamat, GPS Realtime Distance & GMaps Navigation Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate100, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(15.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = formattedDistance,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Slate900
                            )
                            Text(
                                text = AppStrings.tr("(dari titik Anda)", "(from your location)", lang),
                                fontSize = 9.sp,
                                color = Slate500
                            )
                        }
                        Text(
                            text = warung.alamatLengkap.ifEmpty { "GPS: ${String.format(Locale.US, "%.4f", warung.latitude)}, ${String.format(Locale.US, "%.4f", warung.longitude)}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Google Maps Direct Navigation Button
                Button(
                    onClick = onNavigateGmaps,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate900,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("GMaps", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Stock & Debt Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Titipan Aktif
                Column {
                    Text(
                        text = AppStrings.tr("STOK TITIPAN", "CONSIGNMENT STOCK", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${warung.stokTitipanPcs} Pcs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                // Saldo Piutang (Bon)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = AppStrings.tr("SALDO BON", "DEBT BALANCE", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = SfaViewModel.formatRupiah(warung.saldoPiutang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (warung.saldoPiutang > 0) AmberWarning else EmeraldSuccess
                    )
                }
            }

            // Debt limit bar if there's debt
            if (warung.saldoPiutang > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${AppStrings.tr("Limit Hutang", "Debt Limit", lang)}: ${SfaViewModel.formatRupiah(warung.limitHutangMaksimal)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Slate400
                        )
                        Text(
                            text = "${(debtRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (debtRatio > 0.8f) RoseDanger else AmberWarning
                        )
                    }
                    LinearProgressIndicator(
                        progress = { debtRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = if (debtRatio > 0.8f) RoseDanger else AmberWarning,
                        trackColor = Slate100
                    )
                }
            }

            // Action Buttons (High Mobility Touch Targets 44dp)
            if (!isBlacklist) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skenario A: Titip Baru
                    OutlinedButton(
                        onClick = onTitipBaru,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_titip_baru_${warung.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Slate900
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Slate300),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Slate900
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.tr("Titip Baru", "New Consign", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Skenario B: Ganti Barang / Tarik Sisa
                    Button(
                        onClick = onTarikSisa,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("btn_tarik_sisa_${warung.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVisitedToday) Slate700 else Slate900,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isVisitedToday) AppStrings.tr("Isi Ulang Lagi", "Refill Again", lang) else AppStrings.tr("Tarik / Ganti", "Pull / Replace", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Statistics Button
                    IconButton(
                        onClick = onStatistics,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100)
                            .testTag("btn_stats_${warung.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = AppStrings.tr("Statistik Outlet", "Outlet Statistics", lang),
                            tint = Slate700,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // More Menu
                    IconButton(
                        onClick = onDetail,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = AppStrings.tr("Menu Warung", "Outlet Menu", lang),
                            tint = Slate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Blacklist warung action
                OutlinedButton(
                    onClick = onDetail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseDanger),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = SolidColor(RoseBorder))
                ) {
                    Text(AppStrings.tr("Warung Blacklist • Lihat Detail Write-Off", "Blacklist Outlet • View Write-Off Details", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
