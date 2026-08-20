package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.ui.components.RiskAgingBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.SfaViewModel
import com.example.util.AppStrings
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LaporanScreen(
    viewModel: SfaViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val drawers by viewModel.drawers.collectAsState()
    val products by viewModel.products.collectAsState()
    val warungs by viewModel.warungs.collectAsState()
    val pabriks by viewModel.pabriks.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val allDailyLoadings by viewModel.dailyLoadings.collectAsState()
    val allBsSortirs by viewModel.bsSortirs.collectAsState()
    val allWriteOffs by viewModel.writeOffs.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }

    var selectedReportTab by remember { mutableStateOf(0) }
    var showAllReportProducts by remember { mutableStateOf(false) }
    val reportTitles = listOf(
        AppStrings.tab4Drawers(lang),
        AppStrings.tabSupplierDeposit(lang),
        AppStrings.tabOutletReceivables(lang),
        AppStrings.tabAssetProfit(lang),
        AppStrings.tabWriteOff(lang),
        AppStrings.tabTxHistory(lang)
    )

    // Filter Mode: 0 = Hari Ini (Default), 1 = Kemarin, 2 = Pilih Tanggal, 3 = Bulan Ini, 4 = Tahun Ini, 5 = Semua
    var filterPeriodType by remember { mutableStateOf(0) } // Default: 0 (Hari Ini)

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val yesterdayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 86400000L)) }
    val thisMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val thisYearStr = remember { SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) }

    var selectedDate by remember { mutableStateOf(todayDateStr) }
    var selectedMonth by remember { mutableStateOf(thisMonthStr) }
    var selectedYear by remember { mutableStateOf(thisYearStr) }

    val currentPeriodLabel = remember(filterPeriodType, selectedDate, selectedMonth, selectedYear, todayDateStr, yesterdayDateStr, lang) {
        when (filterPeriodType) {
            0 -> "${AppStrings.periodToday(lang)} ($todayDateStr)"
            1 -> "${AppStrings.periodYesterday(lang)} ($yesterdayDateStr)"
            2 -> "${AppStrings.tr("Tanggal", "Date", lang)} $selectedDate"
            3 -> "${AppStrings.tr("Bulan", "Month", lang)} $selectedMonth"
            4 -> "${AppStrings.tr("Tahun", "Year", lang)} $selectedYear"
            else -> AppStrings.periodAll(lang)
        }
    }

    // Dialog state for custom date picker / selector
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Filtered data based on chosen period
    val transactions = remember(allTransactions, filterPeriodType, selectedDate, selectedMonth, selectedYear, todayDateStr, yesterdayDateStr) {
        when (filterPeriodType) {
            0 -> allTransactions.filter { it.tanggal == todayDateStr || it.tanggal.startsWith(todayDateStr) }
            1 -> allTransactions.filter { it.tanggal == yesterdayDateStr || it.tanggal.startsWith(yesterdayDateStr) }
            2 -> allTransactions.filter { it.tanggal == selectedDate || it.tanggal.startsWith(selectedDate) }
            3 -> allTransactions.filter { it.tanggal.startsWith(selectedMonth) }
            4 -> allTransactions.filter { it.tanggal.startsWith(selectedYear) }
            else -> allTransactions
        }
    }

    val dailyLoadings = remember(allDailyLoadings, filterPeriodType, selectedDate, selectedMonth, selectedYear, todayDateStr, yesterdayDateStr) {
        when (filterPeriodType) {
            0 -> allDailyLoadings.filter { it.tanggal == todayDateStr || it.tanggal.startsWith(todayDateStr) }
            1 -> allDailyLoadings.filter { it.tanggal == yesterdayDateStr || it.tanggal.startsWith(yesterdayDateStr) }
            2 -> allDailyLoadings.filter { it.tanggal == selectedDate || it.tanggal.startsWith(selectedDate) }
            3 -> allDailyLoadings.filter { it.tanggal.startsWith(selectedMonth) }
            4 -> allDailyLoadings.filter { it.tanggal.startsWith(selectedYear) }
            else -> allDailyLoadings
        }
    }

    val bsSortirs = remember(allBsSortirs, filterPeriodType, selectedDate, selectedMonth, selectedYear, todayDateStr, yesterdayDateStr) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())
        when (filterPeriodType) {
            0 -> allBsSortirs.filter { sdfDate.format(Date(it.timestamp)) == todayDateStr }
            1 -> allBsSortirs.filter { sdfDate.format(Date(it.timestamp)) == yesterdayDateStr }
            2 -> allBsSortirs.filter { sdfDate.format(Date(it.timestamp)) == selectedDate }
            3 -> allBsSortirs.filter { sdfMonth.format(Date(it.timestamp)) == selectedMonth }
            4 -> allBsSortirs.filter { sdfYear.format(Date(it.timestamp)) == selectedYear }
            else -> allBsSortirs
        }
    }

    val writeOffs = remember(allWriteOffs, filterPeriodType, selectedDate, selectedMonth, selectedYear, todayDateStr, yesterdayDateStr) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())
        when (filterPeriodType) {
            0 -> allWriteOffs.filter { sdfDate.format(Date(it.timestamp)) == todayDateStr }
            1 -> allWriteOffs.filter { sdfDate.format(Date(it.timestamp)) == yesterdayDateStr }
            2 -> allWriteOffs.filter { sdfDate.format(Date(it.timestamp)) == selectedDate }
            3 -> allWriteOffs.filter { sdfMonth.format(Date(it.timestamp)) == selectedMonth }
            4 -> allWriteOffs.filter { sdfYear.format(Date(it.timestamp)) == selectedYear }
            else -> allWriteOffs
        }
    }

    val loadedProductIds = remember(dailyLoadings, drawers, transactions) {
        val fromLoadings = dailyLoadings.map { it.productId }.toSet()
        val fromTransactions = transactions.map { it.productId }.toSet()
        val fromDrawersWithStock = drawers.filter {
            it.stokFreshPabrikPcs > 0 || it.stokPribadiLayakJualPcs > 0 || it.stokBsBelumSortirPcs > 0 || it.stokPribadiRusakPcs > 0
        }.map { it.productId }.toSet()
        fromLoadings + fromTransactions + fromDrawersWithStock
    }

    val reportProducts = remember(products, loadedProductIds, showAllReportProducts) {
        if (showAllReportProducts || loadedProductIds.isEmpty()) {
            products
        } else {
            products.filter { it.id in loadedProductIds }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
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
                            contentDescription = "Menu Navigasi",
                            tint = Slate800,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = AppStrings.reportTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = AppStrings.reportSubtitle(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // PDF Export Action Button
                Button(
                    onClick = {
                        scope.launch {
                            isGeneratingPdf = true
                            try {
                                val file = PdfReportGenerator.generateDailyDistributionReport(
                                    context = context,
                                    profile = userProfile,
                                    periodLabel = currentPeriodLabel,
                                    transactions = transactions,
                                    loadings = dailyLoadings,
                                    drawers = drawers,
                                    products = products,
                                    warungs = warungs,
                                    pabriks = pabriks
                                )
                                generatedPdfFile = file
                                showPdfSuccessDialog = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "${AppStrings.tr("Gagal membuat PDF:", "Failed to generate PDF:", lang)} ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                isGeneratingPdf = false
                            }
                        }
                    },
                    enabled = !isGeneratingPdf,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.pdfGenerating(lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.pdfExport(lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Horizontal Scrollable Report Tabs Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            reportTitles.forEachIndexed { index, title ->
                val isSelected = selectedReportTab == index
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedReportTab = index },
                    label = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Slate900,
                        selectedLabelColor = Color.White,
                        containerColor = Slate100,
                        labelColor = Slate700
                    ),
                    border = null
                )
            }
        }

        // Dedicated Date / Period Carousel & Clean Active Indicator
        Surface(
            color = Slate50,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Period Chips Carousel (Scrollable, single-line, no text wrapping)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val periodOptions: List<Pair<Int, String>> = listOf(
                        Pair(0, AppStrings.periodToday(lang)),
                        Pair(1, AppStrings.periodYesterday(lang)),
                        Pair(2, AppStrings.periodSelectDate(lang)),
                        Pair(3, AppStrings.periodThisMonth(lang)),
                        Pair(4, AppStrings.periodThisYear(lang)),
                        Pair(5, AppStrings.periodAll(lang))
                    )

                    periodOptions.forEach { (typeIdx, label) ->
                        val isSelected = filterPeriodType == typeIdx
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Slate900 else Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Slate900 else Slate300),
                            modifier = Modifier.clickable {
                                filterPeriodType = typeIdx
                                if (typeIdx == 2 || typeIdx == 3 || typeIdx == 4) {
                                    showDatePickerDialog = true
                                }
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Slate700,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Active Period Indicator Bar (Clean & Roomy)
                val activeLabel = when (filterPeriodType) {
                    0 -> "${AppStrings.periodToday(lang)} ($todayDateStr)"
                    1 -> "${AppStrings.periodYesterday(lang)} ($yesterdayDateStr)"
                    2 -> "${AppStrings.tr("Tanggal:", "Date:", lang)} $selectedDate"
                    3 -> "${AppStrings.tr("Bulan:", "Month:", lang)} $selectedMonth"
                    4 -> "${AppStrings.tr("Tahun:", "Year:", lang)} $selectedYear"
                    else -> AppStrings.tr("Seluruh Riwayat Transaksi", "All Transaction History", lang)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = AppStrings.activeFilter(lang),
                            fontSize = 11.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = activeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (filterPeriodType in 2..4) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Slate200,
                            modifier = Modifier.clickable { showDatePickerDialog = true }
                        ) {
                            Text(
                                text = AppStrings.btnChange(lang),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Slate200, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            when (selectedReportTab) {
                0 -> {
                    // 1. LAPORAN 4 LACI INVENTORY (FILTERED BY LOADED / ACTIVE PRODUCTS)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (!showAllReportProducts && loadedProductIds.isNotEmpty()) "${AppStrings.tr("4 LACI PRODUK DIMUAT / AKTIF", "4 DRAWERS LOADED / ACTIVE PRODUCTS", lang)} (${reportProducts.size} SKU)" else "${AppStrings.tr("STATUS REAL-TIME 4 LACI INVENTORY", "REAL-TIME STATUS 4 INVENTORY DRAWERS", lang)} (${reportProducts.size} SKU)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                letterSpacing = 0.5.sp
                            )
                            if (loadedProductIds.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (showAllReportProducts) Slate200 else BlueSurface,
                                    modifier = Modifier.clickable { showAllReportProducts = !showAllReportProducts }
                                ) {
                                    Text(
                                        text = if (showAllReportProducts) AppStrings.loadedOnly(lang) else AppStrings.allCatalog(lang),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showAllReportProducts) Slate800 else BlueAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (loadedProductIds.isEmpty() && !showAllReportProducts) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate50),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        AppStrings.tr("Belum Ada Produk Dimuat untuk Periode Ini", "No Products Loaded for This Period", lang),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Slate800
                                    )
                                    Text(
                                        AppStrings.tr("Laporan 4 Laci Realtime diatur untuk hanya menampilkan jenis produk yang sudah dimuat ke armada mobil.", "Real-time 4 Drawers Report is set to only show product types loaded into the fleet vehicle.", lang),
                                        fontSize = 11.sp,
                                        color = Slate500,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    TextButton(onClick = { showAllReportProducts = true }) {
                                        Text("${AppStrings.tr("Tampilkan Semua Master Produk", "Show All Product Masters", lang)} (${products.size} SKU)", fontSize = 11.sp, color = BlueAccent)
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(reportProducts, key = { _, it -> it.id }, contentType = { _, _ -> "report_product" }) { index, product ->
                            val drawer = drawers.find { it.productId == product.id }
                            LaciStokProductCard(product = product, drawer = drawer, orderNumber = index + 1, lang = lang)
                        }
                    }
                }

                1 -> {
                    // 2. KAS & SETORAN SUPPLIER
                    val totalMuatDus = dailyLoadings.sumOf { it.jumlahDus }
                    val totalTagihanPabrik = dailyLoadings.sumOf { it.potensiHutangPabrik }
                    val totalKasMasuk = transactions.sumOf { it.uangDiterima }
                    val selisih = totalTagihanPabrik - totalKasMasuk

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
                                Text(
                                    text = AppStrings.supplierSummaryTitle(lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500,
                                    letterSpacing = 0.5.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(AppStrings.totalLoadPack(lang), color = Slate500, fontSize = 11.sp)
                                        Text("$totalMuatDus Pack", color = Slate900, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(AppStrings.totalSupplierBill(lang), color = Slate500, fontSize = 11.sp)
                                        Text(SfaViewModel.formatRupiah(totalTagihanPabrik), color = Slate900, fontWeight = FontWeight.Bold)
                                    }
                                }

                                HorizontalDivider(color = Slate100, thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(AppStrings.totalCashCollected(lang), color = Slate500, fontSize = 11.sp)
                                        Text(SfaViewModel.formatRupiah(totalKasMasuk), color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(if (selisih > 0) AppStrings.cashDeficit(lang) else AppStrings.tr("Status Setoran", "Deposit Status", lang), color = Slate500, fontSize = 11.sp)
                                        Text(
                                            text = if (selisih > 0) "- ${SfaViewModel.formatRupiah(selisih)}" else AppStrings.paidSurplus(lang),
                                            color = if (selisih > 0) AmberWarning else EmeraldSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = AppStrings.loadingDetailsTitle(lang),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (dailyLoadings.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate50),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(AppStrings.noLoadingData(lang), color = Slate500, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    itemsIndexed(dailyLoadings, key = { _, it -> it.id }, contentType = { _, _ -> "report_loading" }) { index, loading ->
                        val product = products.find { it.id == loading.productId }
                        val matchedPabrik = pabriks.find { it.id == product?.pabrikId }
                        DailyLoadingItemCard(
                            loading = loading,
                            orderNumber = index + 1,
                            productName = product?.nama ?: AppStrings.tr("Produk", "Product", lang),
                            pabrikName = matchedPabrik?.namaPabrik,
                            lang = lang,
                            onPayDebt = { viewModel.openBayarHutangSupplierDialog(it) }
                        )
                    }
                }

                2 -> {
                    // 3. PIUTANG & AGING
                    val warungsWithDebt = warungs.filter { it.saldoPiutang > 0 && it.status != "Blacklist" }
                    val totalDebt = warungsWithDebt.sumOf { it.saldoPiutang }

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
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(AppStrings.totalBonOutlets(lang), color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Text(SfaViewModel.formatRupiah(totalDebt), color = Slate900, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AmberSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberBorder)
                                ) {
                                    Text(AppStrings.bonOutletCount(warungsWithDebt.size, lang), color = AmberText, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    itemsIndexed(warungsWithDebt.sortedByDescending { it.saldoPiutang }, key = { _, it -> it.id }, contentType = { _, _ -> "report_debt" }) { index, warung ->
                        val days = ((System.currentTimeMillis() - warung.tglMulaiHutang) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
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
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Slate100
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate700,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }

                                    Column {
                                        Text(warung.namaWarung, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Slate900)
                                        Text("${warung.namaPemilik} • ${warung.alamatLengkap}", color = Slate500, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        RiskAgingBadge(daysOverdue = days, saldoPiutang = warung.saldoPiutang)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(AppStrings.bonBalance(lang), color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Text(SfaViewModel.formatRupiah(warung.saldoPiutang), color = AmberWarning, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // 4. ASET PRIBADI & LABA REPACK
                    val totalPribadiStock = drawers.sumOf { it.stokPribadiLayakJualPcs }
                    val totalSortirProfit = bsSortirs.sumOf { it.estimasiProfitMurni }

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
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(AppStrings.assetRepackReady(lang), color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(AppStrings.pcsCirculating(totalPribadiStock, lang), color = Slate900, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(AppStrings.repackProfitNote(lang), color = Slate600, fontSize = 11.sp)
                            }
                        }
                    }

                    item {
                        Text(
                            text = AppStrings.sortirHistoryTitle(lang),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (bsSortirs.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate50),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(AppStrings.noSortirData(lang), color = Slate500, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    itemsIndexed(bsSortirs, key = { _, it -> it.id }, contentType = { _, _ -> "report_sortir" }) { index, sortir ->
                        val product = products.find { it.id == sortir.productId }
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
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Slate100
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(product?.nama ?: AppStrings.tr("Produk", "Product", lang), fontWeight = FontWeight.Bold, color = Slate900)
                                    }
                                    Text(SfaViewModel.formatDate(sortir.timestamp), fontSize = 10.sp, color = Slate500)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${AppStrings.tr("Retur Awal:", "Initial Return:", lang)} ${sortir.totalBsAwalPcs} Pcs", fontSize = 11.sp, color = Slate600)
                                    Text("${AppStrings.tr("Layak Jual:", "Salable:", lang)} +${sortir.bsLayakJualPcs} Pcs", fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                    Text("${AppStrings.tr("Rusak:", "Damaged:", lang)} -${sortir.bsRusakPcs} Pcs", fontSize = 11.sp, color = RoseDanger, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "${AppStrings.cleanProfitEstimate(lang)}: ${SfaViewModel.formatRupiah(sortir.estimasiProfitMurni)}",
                                    color = EmeraldSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                4 -> {
                    // 5. KERUGIAN & WRITE-OFF
                    val totalWriteOffVal = writeOffs.sumOf { it.totalKerugian }
                    val totalBsRusakPcs = drawers.sumOf { it.stokPribadiRusakPcs }

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
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(AppStrings.writeOffReportTitle(lang), color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(
                                    text = "${AppStrings.totalLoss(lang)}: ${SfaViewModel.formatRupiah(totalWriteOffVal)}",
                                    color = RoseDanger,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(AppStrings.discardedDamagedStock(totalBsRusakPcs, lang), color = Slate600, fontSize = 11.sp)
                            }
                        }
                    }

                    if (writeOffs.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate50),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(AppStrings.noLossData(lang), color = Slate500, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    itemsIndexed(writeOffs, key = { _, it -> it.id }, contentType = { _, _ -> "report_writeoff" }) { index, wo ->
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
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Slate100
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(wo.namaWarung, fontWeight = FontWeight.Bold, color = Slate900)
                                    }
                                    Text(SfaViewModel.formatDate(wo.timestamp), fontSize = 10.sp, color = Slate500)
                                }
                                Text("${AppStrings.reason(lang)}: ${wo.alasan}", fontSize = 11.sp, color = Slate600)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${AppStrings.writtenOffDebt(lang)}: ${SfaViewModel.formatRupiah(wo.piutangDihapus)}", fontSize = 11.sp, color = Slate600)
                                    Text("${AppStrings.forfeitedStock(lang)}: ${wo.stokHangusPcs} Pcs", fontSize = 11.sp, color = Slate600)
                                }
                                Text(
                                    text = "${AppStrings.totalBookLoss(lang)}: ${SfaViewModel.formatRupiah(wo.totalKerugian)}",
                                    color = RoseDanger,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                5 -> {
                    // 6. RIWAYAT TRANSAKSI OUTLET
                    val regularTx = transactions.filter { it.warungId != "CLOSING_SALES" && it.jenis != "CLOSING_HARIAN" }
                    val titipCount = regularTx.count { it.jenis == "TITIP_BARU" }
                    val tarikCount = regularTx.count { it.jenis != "TITIP_BARU" }

                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.totalOutletTx(regularTx.size, lang),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                                Text(
                                    text = "$tarikCount ${AppStrings.tr("Tarik/Settle", "Pull/Settle", lang)} • $titipCount ${AppStrings.tr("Titip Baru", "New Consignment", lang)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            }
                        }
                    }

                    if (regularTx.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate50),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(AppStrings.noTxHistory(lang), color = Slate500, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    itemsIndexed(regularTx, key = { _, tx -> tx.id }, contentType = { _, _ -> "report_tx" }) { index, tx ->
                        val warung = warungs.find { it.id == tx.warungId }
                        val prod = products.find { it.id == tx.productId }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.showReceipt(tx) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White,
                                contentColor = Slate900
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = SolidColor(Slate200)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Slate100
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = warung?.namaWarung ?: AppStrings.tr("Outlet", "Outlet", lang),
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (tx.jenis == "TITIP_BARU") BlueSurface else EmeraldSurface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (tx.jenis == "TITIP_BARU") BlueBorder else EmeraldBorder
                                        )
                                    ) {
                                        Text(
                                            text = if (tx.jenis == "TITIP_BARU") AppStrings.tr("DROP TITIP BARU", "NEW CONSIGNMENT DROP", lang) else AppStrings.tr("TARIK & SETTLE", "PULL & SETTLE", lang),
                                            color = if (tx.jenis == "TITIP_BARU") BlueAccent else EmeraldText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                val satuanKecilLabel = prod?.satuanKecil ?: "Pcs"
                                if (tx.jenis == "TITIP_BARU") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${AppStrings.tr("Titip Awal:", "Initial Consign:", lang)} ${tx.restockBaruPcs} $satuanKecilLabel", fontSize = 11.sp, color = Slate600)
                                        Text("${AppStrings.tr("Modal:", "Cost:", lang)} ${SfaViewModel.formatRupiah(tx.grandTotalTagihan)}", fontWeight = FontWeight.Bold, color = BlueAccent, fontSize = 12.sp)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${AppStrings.tr("Laku:", "Sold:", lang)} ${tx.pcsLaku} $satuanKecilLabel • ${AppStrings.tr("Retur:", "Return:", lang)} ${tx.bsDitarikPcs} $satuanKecilLabel", fontSize = 11.sp, color = Slate600)
                                        Text("${AppStrings.tr("Bayar:", "Paid:", lang)} ${SfaViewModel.formatRupiah(tx.uangDiterima)}", fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 12.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(SfaViewModel.formatDate(tx.timestamp), fontSize = 10.sp, color = Slate400)
                                    Text("${AppStrings.viewReceipt(lang)} >", fontSize = 11.sp, color = BlueAccent, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Date / Month / Year Picker Modal Dialog
    if (showDatePickerDialog) {
        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            title = {
                Text(
                    text = when (filterPeriodType) {
                        2 -> AppStrings.tr("Pilih Tanggal Spesifik", "Select Specific Date", lang)
                        3 -> AppStrings.tr("Pilih Bulan Laporan", "Select Report Month", lang)
                        4 -> AppStrings.tr("Pilih Tahun Laporan", "Select Report Year", lang)
                        else -> AppStrings.tr("Pilih Filter", "Select Filter", lang)
                    },
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (filterPeriodType) {
                        2 -> {
                            OutlinedTextField(
                                value = selectedDate,
                                onValueChange = { selectedDate = it },
                                label = { Text("${AppStrings.tr("Format:", "Format:", lang)} YYYY-MM-DD") },
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("${AppStrings.tr("Contoh:", "Example:", lang)} $todayDateStr", fontSize = 11.sp, color = Slate600)
                        }
                        3 -> {
                            OutlinedTextField(
                                value = selectedMonth,
                                onValueChange = { selectedMonth = it },
                                label = { Text("${AppStrings.tr("Format:", "Format:", lang)} YYYY-MM") },
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("${AppStrings.tr("Contoh:", "Example:", lang)} $thisMonthStr", fontSize = 11.sp, color = Slate600)
                        }
                        4 -> {
                            OutlinedTextField(
                                value = selectedYear,
                                onValueChange = { selectedYear = it },
                                label = { Text("${AppStrings.tr("Format:", "Format:", lang)} YYYY") },
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("${AppStrings.tr("Contoh:", "Example:", lang)} $thisYearStr", fontSize = 11.sp, color = Slate600)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDatePickerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                ) {
                    Text(AppStrings.btnApplyFilter(lang), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePickerDialog = false }) {
                    Text(AppStrings.btnClose(lang), color = Slate700)
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        )
    }

    if (showPdfSuccessDialog && generatedPdfFile != null) {
        val pdfFile = generatedPdfFile!!
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EmeraldSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = AppStrings.pdfReadyTitle(lang),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = AppStrings.pdfReadyMsg(currentPeriodLabel, lang),
                        fontSize = 12.sp,
                        color = Slate600
                    )
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(pdfFile.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate900)
                            Text("${AppStrings.tr("Ukuran File:", "File Size:", lang)} ${pdfFile.length() / 1024} KB • A4 Print Ready", fontSize = 10.sp, color = Slate500)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PdfReportGenerator.sharePdfReport(context, pdfFile, "Bagikan Laporan TracerPro ke Distributor")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.sharePdf(lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            PdfReportGenerator.openPdfReport(context, pdfFile)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(AppStrings.openPdf(lang), fontSize = 12.sp, color = Slate800, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = { showPdfSuccessDialog = false }
                    ) {
                        Text(AppStrings.btnClose(lang), fontSize = 12.sp, color = Slate500)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun LaciStokProductCard(
    product: ProductEntity,
    drawer: InventoryDrawerEntity?,
    orderNumber: Int? = null,
    lang: String = "id",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (orderNumber != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Slate100
                    ) {
                        Text(
                            text = "#$orderNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(product.nama, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Slate900)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate50)
                        .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(EmeraldSuccess))
                            Text(AppStrings.drawerFresh(lang), fontSize = 10.sp, color = Slate600, fontWeight = FontWeight.Medium)
                        }
                        Text("${drawer?.stokFreshPabrikPcs ?: 0} Pcs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate50)
                        .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AmberWarning))
                            Text(AppStrings.drawerBsSortir(lang), fontSize = 10.sp, color = Slate600, fontWeight = FontWeight.Medium)
                        }
                        Text("${drawer?.stokBsBelumSortirPcs ?: 0} Pcs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate50)
                        .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(IndigoAsset))
                            Text(AppStrings.drawerPribadi(lang), fontSize = 10.sp, color = Slate600, fontWeight = FontWeight.Medium)
                        }
                        Text("${drawer?.stokPribadiLayakJualPcs ?: 0} Pcs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate50)
                        .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(RoseDanger))
                            Text(AppStrings.drawerRusak(lang), fontSize = 10.sp, color = Slate600, fontWeight = FontWeight.Medium)
                        }
                        Text("${drawer?.stokPribadiRusakPcs ?: 0} Pcs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyLoadingItemCard(
    loading: DailyLoadingEntity,
    productName: String,
    pabrikName: String? = null,
    orderNumber: Int? = null,
    lang: String = "id",
    onPayDebt: ((DailyLoadingEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (orderNumber != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Slate100
                        ) {
                            Text(
                                text = "#$orderNumber",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Column {
                        Text(productName, fontWeight = FontWeight.Bold, color = Slate900)
                        if (!pabrikName.isNullOrBlank()) {
                            Text(
                                text = "${AppStrings.tr("Supplier:", "Supplier:", lang)} $pabrikName",
                                fontSize = 10.sp,
                                color = BlueAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(loading.tanggal, fontSize = 11.sp, color = Slate500)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${AppStrings.tr("Muat:", "Load:", lang)} ${loading.jumlahDus} Pack (${loading.totalPcs} Pcs)", fontSize = 11.sp, color = Slate600)
                Text("${AppStrings.tr("Total:", "Total:", lang)} ${SfaViewModel.formatRupiah(loading.potensiHutangPabrik)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
            }

            // Opsi & Status Pembayaran
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (badgeText, badgeBg, badgeTextColor) = when (loading.opsiBayarMuat) {
                    "BAYAR_LANGSUNG" -> Triple(AppStrings.tr("Bayar Langsung (Lunas)", "Direct Pay (Paid)", lang), EmeraldSurface, EmeraldText)
                    "BAYAR_CLOSING" -> Triple(AppStrings.tr("Bayar Pas Closing", "Pay At Closing", lang), BlueSurface, BlueText)
                    "HUTANG" -> if (loading.statusLunasHutang || loading.sisaHutangMuat <= 0) {
                        Triple(AppStrings.tr("Hutang (Lunas)", "Payable (Settled)", lang), EmeraldSurface, EmeraldText)
                    } else {
                        Triple("${AppStrings.tr("Hutang: Sisa", "Payable: Rem.", lang)} ${SfaViewModel.formatRupiah(loading.sisaHutangMuat)}", AmberSurface, AmberText)
                    }
                    else -> Triple(AppStrings.tr("Muat Harian", "Daily Loading", lang), Slate100, Slate700)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (loading.opsiBayarMuat == "HUTANG" && !loading.statusLunasHutang && loading.sisaHutangMuat > 0 && onPayDebt != null) {
                    OutlinedButton(
                        onClick = { onPayDebt(loading) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueAccent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BlueBorder)
                    ) {
                        Text(AppStrings.tr("Bayar Hutang", "Pay Debt", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

