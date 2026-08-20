package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SfaViewModel
import com.example.util.AppStrings
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class RiwayatCategoryFilter {
    ALL,
    VISITS,
    LOADING,
    CLOSING,
    SORTIR,
    WRITEOFF
}

enum class RiwayatDateFilter {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    ALL_TIME
}

sealed class HistoryItem(val timestamp: Long, val dateStr: String) {
    data class VisitTransaction(val entity: TransactionEntity) : HistoryItem(entity.timestamp, entity.tanggal)
    data class StockLoading(val entity: DailyLoadingEntity) : HistoryItem(entity.createdAt, entity.tanggal)
    data class DailyClosing(val date: String, val loadings: List<DailyLoadingEntity>, val closeTimestamp: Long) : HistoryItem(closeTimestamp, date)
    data class BsSortir(val entity: BsSortirEntity) : HistoryItem(entity.timestamp, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entity.timestamp)))
    data class WriteOff(val entity: WriteOffEntity) : HistoryItem(entity.timestamp, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entity.timestamp)))
}

@Composable
fun RiwayatScreen(
    viewModel: SfaViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val lang by viewModel.appLanguage.collectAsState()

    val transactions by viewModel.transactions.collectAsState()
    val dailyLoadings by viewModel.dailyLoadings.collectAsState()
    val bsSortirs by viewModel.bsSortirs.collectAsState()
    val writeOffs by viewModel.writeOffs.collectAsState()
    val products by viewModel.products.collectAsState()
    val warungs by viewModel.warungs.collectAsState()
    val rutes by viewModel.rutes.collectAsState()

    var selectedCategory by remember { mutableStateOf(RiwayatCategoryFilter.ALL) }
    var selectedDateFilter by remember { mutableStateOf(RiwayatDateFilter.ALL_TIME) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedHistoryDetail by remember { mutableStateOf<HistoryItem?>(null) }

    val productMap = remember(products) { products.associateBy { it.id } }
    val warungMap = remember(warungs) { warungs.associateBy { it.id } }
    val ruteMap = remember(rutes) { rutes.associateBy { it.id } }

    val now = remember { System.currentTimeMillis() }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Combine and sort all history records
    val allHistoryList = remember(transactions, dailyLoadings, bsSortirs, writeOffs) {
        val list = mutableListOf<HistoryItem>()

        // 1. Visit Transactions (Titip Baru, Tarik Sisa / Restock)
        transactions.forEach { tx ->
            list.add(HistoryItem.VisitTransaction(tx))
        }

        // 2. Stock Loadings (Muat Barang Pagi)
        dailyLoadings.forEach { load ->
            list.add(HistoryItem.StockLoading(load))
        }

        // 3. Daily Closings
        val closingsByDate = dailyLoadings.filter { it.statusClosing }.groupBy { it.tanggal }
        closingsByDate.forEach { (date, listItems) ->
            val maxTime = listItems.maxOfOrNull { it.createdAt } ?: now
            list.add(HistoryItem.DailyClosing(date, listItems, maxTime))
        }

        // 4. BS Sortirs
        bsSortirs.forEach { bs ->
            list.add(HistoryItem.BsSortir(bs))
        }

        // 5. Write-offs
        writeOffs.forEach { wo ->
            list.add(HistoryItem.WriteOff(wo))
        }

        list.sortedByDescending { it.timestamp }
    }

    // Filter by Date
    val dateFilteredList = remember(allHistoryList, selectedDateFilter, todayStr) {
        val calendar = Calendar.getInstance()
        when (selectedDateFilter) {
            RiwayatDateFilter.TODAY -> {
                allHistoryList.filter { it.dateStr == todayStr }
            }
            RiwayatDateFilter.LAST_7_DAYS -> {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val cutoff = calendar.timeInMillis
                allHistoryList.filter { it.timestamp >= cutoff }
            }
            RiwayatDateFilter.LAST_30_DAYS -> {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val cutoff = calendar.timeInMillis
                allHistoryList.filter { it.timestamp >= cutoff }
            }
            RiwayatDateFilter.ALL_TIME -> allHistoryList
        }
    }

    // Filter by Category
    val categoryFilteredList = remember(dateFilteredList, selectedCategory) {
        when (selectedCategory) {
            RiwayatCategoryFilter.ALL -> dateFilteredList
            RiwayatCategoryFilter.VISITS -> dateFilteredList.filterIsInstance<HistoryItem.VisitTransaction>()
            RiwayatCategoryFilter.LOADING -> dateFilteredList.filterIsInstance<HistoryItem.StockLoading>()
            RiwayatCategoryFilter.CLOSING -> dateFilteredList.filterIsInstance<HistoryItem.DailyClosing>()
            RiwayatCategoryFilter.SORTIR -> dateFilteredList.filter { it is HistoryItem.BsSortir || it is HistoryItem.WriteOff }
            RiwayatCategoryFilter.WRITEOFF -> dateFilteredList.filterIsInstance<HistoryItem.WriteOff>()
        }
    }

    // Search query filter
    val finalHistoryList = remember(categoryFilteredList, searchQuery, productMap, warungMap) {
        if (searchQuery.isBlank()) {
            categoryFilteredList
        } else {
            val q = searchQuery.trim().lowercase()
            categoryFilteredList.filter { item ->
                when (item) {
                    is HistoryItem.VisitTransaction -> {
                        val wName = warungMap[item.entity.warungId]?.namaWarung?.lowercase() ?: ""
                        val pName = productMap[item.entity.productId]?.nama?.lowercase() ?: ""
                        wName.contains(q) || pName.contains(q) || item.entity.id.lowercase().contains(q) ||
                                item.entity.jenis.lowercase().contains(q) || item.entity.catatan.lowercase().contains(q)
                    }
                    is HistoryItem.StockLoading -> {
                        val pName = productMap[item.entity.productId]?.nama?.lowercase() ?: ""
                        pName.contains(q) || item.entity.id.lowercase().contains(q) || item.entity.catatanMuat.lowercase().contains(q)
                    }
                    is HistoryItem.DailyClosing -> {
                        item.date.contains(q) || "closing".contains(q)
                    }
                    is HistoryItem.BsSortir -> {
                        val pName = productMap[item.entity.productId]?.nama?.lowercase() ?: ""
                        pName.contains(q) || item.entity.catatan.lowercase().contains(q)
                    }
                    is HistoryItem.WriteOff -> {
                        item.entity.namaWarung.lowercase().contains(q) || item.entity.alasan.lowercase().contains(q)
                    }
                }
            }
        }
    }

    // KPI Metrics calculation
    val totalTransactionsCount = finalHistoryList.size
    val totalCashReceived = remember(finalHistoryList) {
        finalHistoryList.filterIsInstance<HistoryItem.VisitTransaction>().sumOf { it.entity.uangDiterima }
    }
    val totalUnitsDistributed = remember(finalHistoryList) {
        val fromVisits = finalHistoryList.filterIsInstance<HistoryItem.VisitTransaction>().sumOf { it.entity.restockBaruPcs }
        val fromLoading = finalHistoryList.filterIsInstance<HistoryItem.StockLoading>().sumOf { it.entity.totalPcs }
        fromVisits + fromLoading
    }
    val totalBsReturnedCount = remember(finalHistoryList) {
        finalHistoryList.filterIsInstance<HistoryItem.VisitTransaction>().sumOf { it.entity.bsDitarikPcs }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar / Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            text = AppStrings.historyTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = AppStrings.historySubtitle(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
                ) {
                    Text(
                        text = "$totalTransactionsCount ${AppStrings.tr("Catatan", "Logs", lang)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = AppStrings.searchHistoryPlaceholder(lang),
                        fontSize = 12.sp,
                        color = Slate400
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Slate900,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterChip(
                    label = AppStrings.filterAll(lang),
                    isSelected = selectedCategory == RiwayatCategoryFilter.ALL,
                    onClick = { selectedCategory = RiwayatCategoryFilter.ALL }
                )
                HistoryFilterChip(
                    label = AppStrings.filterVisits(lang),
                    isSelected = selectedCategory == RiwayatCategoryFilter.VISITS,
                    onClick = { selectedCategory = RiwayatCategoryFilter.VISITS }
                )
                HistoryFilterChip(
                    label = AppStrings.filterLoading(lang),
                    isSelected = selectedCategory == RiwayatCategoryFilter.LOADING,
                    onClick = { selectedCategory = RiwayatCategoryFilter.LOADING }
                )
                HistoryFilterChip(
                    label = AppStrings.filterClosing(lang),
                    isSelected = selectedCategory == RiwayatCategoryFilter.CLOSING,
                    onClick = { selectedCategory = RiwayatCategoryFilter.CLOSING }
                )
                HistoryFilterChip(
                    label = AppStrings.filterSortir(lang),
                    isSelected = selectedCategory == RiwayatCategoryFilter.SORTIR,
                    onClick = { selectedCategory = RiwayatCategoryFilter.SORTIR }
                )
            }

            // Date Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DateFilterPill(
                    label = AppStrings.filterDateAll(lang),
                    isSelected = selectedDateFilter == RiwayatDateFilter.ALL_TIME,
                    onClick = { selectedDateFilter = RiwayatDateFilter.ALL_TIME }
                )
                DateFilterPill(
                    label = AppStrings.filterDateToday(lang),
                    isSelected = selectedDateFilter == RiwayatDateFilter.TODAY,
                    onClick = { selectedDateFilter = RiwayatDateFilter.TODAY }
                )
                DateFilterPill(
                    label = AppStrings.filterDate7Days(lang),
                    isSelected = selectedDateFilter == RiwayatDateFilter.LAST_7_DAYS,
                    onClick = { selectedDateFilter = RiwayatDateFilter.LAST_7_DAYS }
                )
                DateFilterPill(
                    label = AppStrings.filterDate30Days(lang),
                    isSelected = selectedDateFilter == RiwayatDateFilter.LAST_30_DAYS,
                    onClick = { selectedDateFilter = RiwayatDateFilter.LAST_30_DAYS }
                )
            }
        }

        HorizontalDivider(color = Slate200, thickness = 1.dp)

        // KPI Metrics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate100)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricPill(
                title = AppStrings.totalCash(lang),
                value = "Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(totalCashReceived.toLong())}",
                accentColor = EmeraldSuccess,
                modifier = Modifier.weight(1.3f)
            )
            MetricPill(
                title = AppStrings.totalPcsDistributed(lang),
                value = "$totalUnitsDistributed Pcs",
                accentColor = Color(0xFF4F46E5),
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                title = AppStrings.totalBsReturned(lang),
                value = "$totalBsReturnedCount Pcs",
                accentColor = AmberWarning,
                modifier = Modifier.weight(1f)
            )
        }

        // History Content List
        if (finalHistoryList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = AppStrings.emptyHistory(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                    Text(
                        text = AppStrings.tr("Lakukan transaksi kunjungan, muat barang, atau closing sore untuk melihat log di sini.", "Perform outlet visits, loading stock, or daily closing to view logs here.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                items(finalHistoryList, key = { item ->
                    when (item) {
                        is HistoryItem.VisitTransaction -> "tx_${item.entity.id}"
                        is HistoryItem.StockLoading -> "load_${item.entity.id}"
                        is HistoryItem.DailyClosing -> "close_${item.date}"
                        is HistoryItem.BsSortir -> "bs_${item.entity.id}"
                        is HistoryItem.WriteOff -> "wo_${item.entity.id}"
                    }
                }) { item ->
                    HistoryCard(
                        item = item,
                        lang = lang,
                        productMap = productMap,
                        warungMap = warungMap,
                        ruteMap = ruteMap,
                        onClick = { selectedHistoryDetail = item }
                    )
                }
            }
        }
    }

    // Full Details Modal Dialog
    if (selectedHistoryDetail != null) {
        HistoryDetailDialog(
            item = selectedHistoryDetail!!,
            lang = lang,
            productMap = productMap,
            warungMap = warungMap,
            ruteMap = ruteMap,
            onDismiss = { selectedHistoryDetail = null }
        )
    }
}

@Composable
fun HistoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Slate900 else Color.White,
        border = if (isSelected) null else CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Slate700,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun DateFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Slate800 else Slate100
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Slate600,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MetricPill(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(text = title, fontSize = 9.sp, color = Slate500, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
fun HistoryCard(
    item: HistoryItem,
    lang: String,
    productMap: Map<String, ProductEntity>,
    warungMap: Map<String, WarungEntity>,
    ruteMap: Map<String, RuteEntity>,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_card_${item.timestamp}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (item) {
                is HistoryItem.VisitTransaction -> {
                    val tx = item.entity
                    val warung = warungMap[tx.warungId]
                    val product = productMap[tx.productId]
                    val isTitipBaru = tx.jenis == "TITIP_BARU"

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
                                color = if (isTitipBaru) EmeraldSuccess.copy(alpha = 0.15f) else Color(0xFF4F46E5).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isTitipBaru) AppStrings.statusTitipBaru(lang) else AppStrings.statusTarikSisa(lang),
                                    color = if (isTitipBaru) EmeraldSuccess else Color(0xFF4F46E5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = timeFormat.format(Date(tx.timestamp)),
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }

                        // Payment status
                        if (!isTitipBaru) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (tx.statusBayar) {
                                    "LUNAS" -> EmeraldSuccess.copy(alpha = 0.15f)
                                    "SEBAGIAN" -> AmberWarning.copy(alpha = 0.15f)
                                    else -> RoseDanger.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = when (tx.statusBayar) {
                                        "LUNAS" -> AppStrings.statusLunas(lang)
                                        "SEBAGIAN" -> AppStrings.statusSebagian(lang)
                                        else -> AppStrings.statusBonFull(lang)
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (tx.statusBayar) {
                                        "LUNAS" -> EmeraldSuccess
                                        "SEBAGIAN" -> AmberWarning
                                        else -> RoseDanger
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Warung & Product Title
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = warung?.namaWarung ?: tx.warungId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${product?.nama ?: tx.productId} • ${if (tx.sumberStok == "FRESH_PABRIK") AppStrings.freshFactory(lang) else AppStrings.privateRepack(lang)}",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }

                    HorizontalDivider(color = Slate100, thickness = 1.dp)

                    // Movement & Financials
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = if (isTitipBaru) AppStrings.tr("Titip Awal", "Initial Drop", lang) else AppStrings.tr("Laku / Restock", "Sold / Restocked", lang), fontSize = 9.sp, color = Slate400)
                            Text(
                                text = if (isTitipBaru) "+${tx.restockBaruPcs} Pcs" else "${tx.pcsLaku} Laku • Restock +${tx.restockBaruPcs} Pcs",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate800
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = AppStrings.tr("Uang Diterima", "Payment Received", lang), fontSize = 9.sp, color = Slate400)
                            Text(
                                text = "Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(tx.uangDiterima.toLong())}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.uangDiterima > 0) EmeraldSuccess else Slate600
                            )
                        }
                    }

                    if (tx.bsDitarikPcs > 0 || tx.gpsAddress.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tx.bsDitarikPcs > 0) {
                                Text(
                                    text = "⚠️ ${AppStrings.tr("Tarik Retur:", "Returns Pulled:", lang)} ${tx.bsDitarikPcs} Pcs",
                                    fontSize = 10.sp,
                                    color = AmberWarning,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (tx.gpsAddress.isNotBlank()) {
                                Text(
                                    text = "📍 ${tx.gpsAddress.take(32)}...",
                                    fontSize = 9.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }

                is HistoryItem.StockLoading -> {
                    val load = item.entity
                    val product = productMap[load.productId]

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
                                color = Color(0xFF4F46E5).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = AppStrings.statusMuat(lang),
                                    color = Color(0xFF4F46E5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = timeFormat.format(Date(load.createdAt)), fontSize = 10.sp, color = Slate400)
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (load.statusLunasHutang) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (load.statusLunasHutang) AppStrings.statusLunas(lang) else AppStrings.tr("HUTANG SUPPLIER", "SUPPLIER DEBT", lang),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (load.statusLunasHutang) EmeraldSuccess else AmberWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = product?.nama ?: load.productId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${load.jumlahDus} Dus (${load.totalPcs} Pcs) • @ Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(load.hargaBeliPabrikDus.toLong())}/Dus",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }

                    HorizontalDivider(color = Slate100, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${AppStrings.tr("Opsi Bayar:", "Payment Option:", lang)} ${load.opsiBayarMuat}",
                            fontSize = 10.sp,
                            color = Slate500
                        )
                        Text(
                            text = "${AppStrings.tr("Hutang:", "Payable:", lang)} Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(load.potensiHutangPabrik.toLong())}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }
                }

                is HistoryItem.DailyClosing -> {
                    val closing = item
                    val totalTagihan = closing.loadings.sumOf { it.tagihanPabrikClosing }
                    val totalTerjualDus = closing.loadings.sumOf { it.terjualDus }

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
                                color = Color(0xFF9333EA).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = AppStrings.statusClosing(lang),
                                    color = Color(0xFF9333EA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = closing.date, fontSize = 10.sp, color = Slate400)
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = AppStrings.tr("CLOSING SELESAI", "CLOSED", lang),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${AppStrings.tr("Rekap Setoran Closing", "Closing Settlement Summary", lang)} (${closing.loadings.size} SKU)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${AppStrings.tr("Terjual Akumulasi:", "Sold Total:", lang)} $totalTerjualDus Dus",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }

                    HorizontalDivider(color = Slate100, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = AppStrings.tr("Tagihan Supplier Final:", "Final Supplier Due:", lang),
                            fontSize = 10.sp,
                            color = Slate500
                        )
                        Text(
                            text = "Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(totalTagihan.toLong())}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                }

                is HistoryItem.BsSortir -> {
                    val bs = item.entity
                    val product = productMap[bs.productId]

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AmberWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = AppStrings.statusSortir(lang),
                                color = AmberWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(text = timeFormat.format(Date(bs.timestamp)), fontSize = 10.sp, color = Slate400)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = product?.nama ?: bs.productId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${AppStrings.tr("Awal", "Initial", lang)}: ${bs.totalBsAwalPcs} Pcs ➜ ${AppStrings.tr("Layak Jual", "Repack", lang)}: +${bs.bsLayakJualPcs} Pcs • ${AppStrings.tr("Rusak/Buang", "Damaged", lang)}: ${bs.bsRusakPcs} Pcs",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }

                    HorizontalDivider(color = Slate100, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = AppStrings.tr("Estimasi Profit Bersih:", "Estimated Net Profit:", lang),
                            fontSize = 10.sp,
                            color = Slate500
                        )
                        Text(
                            text = "Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(bs.estimasiProfitMurni.toLong())}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                }

                is HistoryItem.WriteOff -> {
                    val wo = item.entity

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RoseDanger.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = AppStrings.statusWriteOff(lang),
                                color = RoseDanger,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(text = timeFormat.format(Date(wo.timestamp)), fontSize = 10.sp, color = Slate400)
                    }

                    Text(
                        text = wo.namaWarung,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "${AppStrings.tr("Alasan:", "Reason:", lang)} ${wo.alasan}",
                        fontSize = 11.sp,
                        color = Slate600
                    )

                    HorizontalDivider(color = Slate100, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = AppStrings.tr("Total Kerugian:", "Total Loss:", lang),
                            fontSize = 10.sp,
                            color = Slate500
                        )
                        Text(
                            text = "Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(wo.totalKerugian.toLong())}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseDanger
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryDetailDialog(
    item: HistoryItem,
    lang: String,
    productMap: Map<String, ProductEntity>,
    warungMap: Map<String, WarungEntity>,
    ruteMap: Map<String, RuteEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    val receiptText = remember(item, lang) {
        val sb = StringBuilder()
        sb.append("===============================\n")
        sb.append("      TRACERPRO SFA FMCG       \n")
        sb.append("   ${if (lang == "en") "DIGITAL TRANSACTION AUDIT" else "BUKTI TRANSAKSI DIGITAL"}   \n")
        sb.append("===============================\n")
        when (item) {
            is HistoryItem.VisitTransaction -> {
                val tx = item.entity
                val w = warungMap[tx.warungId]
                val p = productMap[tx.productId]
                val r = ruteMap[tx.ruteId]
                sb.append("ID Ref     : ${tx.id}\n")
                sb.append("${if (lang == "en") "Time       " else "Waktu      "}: ${timeFormat.format(Date(tx.timestamp))}\n")
                sb.append("${if (lang == "en") "Type       " else "Jenis      "}: ${tx.jenis}\n")
                sb.append("Outlet     : ${w?.namaWarung ?: tx.warungId}\n")
                sb.append("${if (lang == "en") "Route      " else "Rute       "}: ${r?.namaRute ?: tx.ruteId}\n")
                sb.append("${if (lang == "en") "Product    " else "Produk     "}: ${p?.nama ?: tx.productId}\n")
                sb.append("${if (lang == "en") "Source     " else "Sumber     "}: ${tx.sumberStok}\n")
                sb.append("-------------------------------\n")
                sb.append("${if (lang == "en") "Prev Consign " else "Titipan Lalu "}: ${tx.sisaTitipanLaluPcs} Pcs\n")
                sb.append("${if (lang == "en") "Physical Rem " else "Sisa Fisik   "}: ${tx.sisaFisikPcs} Pcs\n")
                sb.append("${if (lang == "en") "Sold Pcs     " else "Pcs Laku     "}: ${tx.pcsLaku} Pcs\n")
                sb.append("${if (lang == "en") "Unit Price   " else "Harga Satuan "}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(tx.hargaSatuan.toLong())}\n")
                sb.append("${if (lang == "en") "Subtotal     " else "Subtotal     "}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(tx.subtotalLaku.toLong())}\n")
                sb.append("${if (lang == "en") "Old Debt     " else "Piutang Lama "}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(tx.saldoPiutangLama.toLong())}\n")
                sb.append("${if (lang == "en") "Total Bill   " else "Total Tagihan"}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(tx.grandTotalTagihan.toLong())}\n")
                sb.append("${if (lang == "en") "Payment Rcvd " else "Uang Masuk   "}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(tx.uangDiterima.toLong())}\n")
                sb.append("${if (lang == "en") "Rem Debt     " else "Sisa Bon     "}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(tx.saldoPiutangBaru.toLong())}\n")
                sb.append("${if (lang == "en") "Pay Status   " else "Status Bayar "}: ${tx.statusBayar}\n")
                sb.append("-------------------------------\n")
                sb.append("${if (lang == "en") "Pull Return  " else "Tarik Retur  "}: ${tx.bsDitarikPcs} Pcs\n")
                sb.append("${if (lang == "en") "New Restock  " else "Restock Baru "}: ${tx.restockBaruPcs} Pcs\n")
                sb.append("${if (lang == "en") "Active Consign" else "Titipan Aktif"}: ${tx.totalTitipanAktifPcs} Pcs\n")
                if (tx.gpsAddress.isNotBlank()) {
                    sb.append("GPS Lat/Lng  : ${tx.gpsLat}, ${tx.gpsLng}\n")
                    sb.append("GPS Address  : ${tx.gpsAddress}\n")
                }
                if (tx.catatan.isNotBlank()) {
                    sb.append("${if (lang == "en") "Notes        " else "Catatan      "}: ${tx.catatan}\n")
                }
            }
            is HistoryItem.StockLoading -> {
                val load = item.entity
                val p = productMap[load.productId]
                sb.append("ID Ref       : ${load.id}\n")
                sb.append("${if (lang == "en") "Time         " else "Waktu        "}: ${timeFormat.format(Date(load.createdAt))}\n")
                sb.append("${if (lang == "en") "Type         : MORNING STOCK LOADING\n" else "Jenis        : MUAT BARANG PAGI\n"}")
                sb.append("${if (lang == "en") "Product      " else "Produk       "}: ${p?.nama ?: load.productId}\n")
                sb.append("${if (lang == "en") "Load Qty     " else "Jumlah Muat  "}: ${load.jumlahDus} Dus (${load.totalPcs} Pcs)\n")
                sb.append("${if (lang == "en") "Cost Price   " else "Harga Beli   "}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(load.hargaBeliPabrikDus.toLong())}/Dus\n")
                sb.append("${if (lang == "en") "Supplier Due " else "Potensi Hutang"}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(load.potensiHutangPabrik.toLong())}\n")
                sb.append("${if (lang == "en") "Pay Option   " else "Opsi Bayar   "}: ${load.opsiBayarMuat}\n")
                sb.append("${if (lang == "en") "Rem Payable  " else "Sisa Hutang  "}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(load.sisaHutangMuat.toLong())}\n")
                sb.append("${if (lang == "en") "Paid Status  : ${if (load.statusLunasHutang) "PAID" else "UNPAID"}\n" else "Status Lunas : ${if (load.statusLunasHutang) "LUNAS" else "BELUM LUNAS"}\n"}")
            }
            is HistoryItem.DailyClosing -> {
                sb.append("${if (lang == "en") "Date         " else "Tanggal      "}: ${item.date}\n")
                sb.append("${if (lang == "en") "Type         : EVENING CLOSING RECONCILIATION\n" else "Jenis        : CLOSING SORE REKONSILIASI\n"}")
                sb.append("${if (lang == "en") "SKU Count    " else "Jumlah SKU   "}: ${item.loadings.size}\n")
                item.loadings.forEach { l ->
                    val p = productMap[l.productId]
                    sb.append(" - ${p?.nama ?: l.productId}: ${if (lang == "en") "Sold" else "Terjual"} ${l.terjualDus} Dus, ${if (lang == "en") "Due" else "Tagihan"} Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(l.tagihanPabrikClosing.toLong())}\n")
                }
            }
            is HistoryItem.BsSortir -> {
                val bs = item.entity
                val p = productMap[bs.productId]
                sb.append("${if (lang == "en") "Time         " else "Waktu        "}: ${timeFormat.format(Date(bs.timestamp))}\n")
                sb.append("${if (lang == "en") "Type         : RETURN REPACK SORTING\n" else "Jenis        : SORTIR RETUR REPACK\n"}")
                sb.append("${if (lang == "en") "Product      " else "Produk       "}: ${p?.nama ?: bs.productId}\n")
                sb.append("${if (lang == "en") "Initial Ret  " else "Total Retur Awal"}: ${bs.totalBsAwalPcs} Pcs\n")
                sb.append("${if (lang == "en") "Salable      " else "Layak Repack "}: ${bs.bsLayakJualPcs} Pcs\n")
                sb.append("${if (lang == "en") "Damaged/Waste" else "Rusak/Buang  "}: ${bs.bsRusakPcs} Pcs\n")
                sb.append("${if (lang == "en") "Est. Profit  " else "Estimasi Laba"}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(bs.estimasiProfitMurni.toLong())}\n")
            }
            is HistoryItem.WriteOff -> {
                val wo = item.entity
                sb.append("${if (lang == "en") "Time         " else "Waktu        "}: ${timeFormat.format(Date(wo.timestamp))}\n")
                sb.append("${if (lang == "en") "Type         : BAD DEBT WRITE-OFF\n" else "Jenis        : PENGHAPUSAN PIUTANG (WRITE-OFF)\n"}")
                sb.append("Outlet       : ${wo.namaWarung}\n")
                sb.append("${if (lang == "en") "Total Loss   " else "Total Rugi   "}: Rp ${NumberFormat.getNumberInstance(Locale.GERMAN).format(wo.totalKerugian.toLong())}\n")
                sb.append("${if (lang == "en") "Reason       " else "Alasan       "}: ${wo.alasan}\n")
            }
        }
        sb.append("===============================\n")
        sb.append("   ${if (lang == "en") "SAVED FOR SFA AUDIT" else "SIMPAN SEBAGAI AUDIT SFA"}   \n")
        sb.append("===============================\n")
        sb.toString()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.detailReceiptTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate200)

                // Monospace receipt viewer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp)
                        .background(Slate950, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = receiptText,
                        color = EmeraldSuccess,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("TracerPro Receipt", receiptText)
                            clipboard.setPrimaryClip(clip)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Bukti Transaksi"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.btnShareReceipt(lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text(AppStrings.btnClose(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
