package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.ui.components.AiRobotAvatar
import com.example.ui.components.DashboardSummaryChart
import com.example.ui.components.DrawerInventorySummary
import com.example.ui.components.MinimalStatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppNavScreen
import com.example.ui.viewmodel.SfaViewModel
import com.example.ui.viewmodel.TransactionDialogState
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: SfaViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val warungs by viewModel.warungs.collectAsState()
    val rutes by viewModel.rutes.collectAsState()
    val drawers by viewModel.drawers.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val dailyLoadings by viewModel.dailyLoadings.collectAsState()
    val products by viewModel.products.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    val todayDateStr = remember(lang) {
        val locale = if (lang.equals("EN", ignoreCase = true)) Locale.US else Locale("id", "ID")
        val pattern = if (lang.equals("EN", ignoreCase = true)) "EEEE, MMMM dd, yyyy" else "EEEE, dd MMMM yyyy"
        SimpleDateFormat(pattern, locale).format(Date())
    }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Optimized Calculations cached via remember
    val totalFresh = remember(drawers) { drawers.sumOf { it.stokFreshPabrikPcs } }
    val totalBsBelumSortir = remember(drawers) { drawers.sumOf { it.stokBsBelumSortirPcs } }
    val totalPribadiLayak = remember(drawers) { drawers.sumOf { it.stokPribadiLayakJualPcs } }
    val totalPribadiRusak = remember(drawers) { drawers.sumOf { it.stokPribadiRusakPcs } }

    val todayTx = remember(transactions, todayKey) {
        transactions.filter { it.tanggal == todayKey && it.warungId != "CLOSING_SALES" && it.jenis != "CLOSING_HARIAN" }
    }
    val totalKasHariIni = remember(todayTx) { todayTx.sumOf { it.uangDiterima } }
    val totalPiutangWarung = remember(warungs) { warungs.filter { it.status != "Blacklist" }.sumOf { it.saldoPiutang } }

    // Today loading and pabrik bill
    val todayLoadings = remember(dailyLoadings, todayKey) { dailyLoadings.filter { it.tanggal == todayKey } }
    val isClosingDone = remember(todayLoadings) { todayLoadings.isNotEmpty() && todayLoadings.all { it.statusClosing } }
    val tagihanPabrik = remember(isClosingDone, todayLoadings) {
        if (isClosingDone) {
            todayLoadings.sumOf { it.tagihanPabrikClosing }
        } else {
            todayLoadings.sumOf { it.potensiHutangPabrik }
        }
    }

    // Dynamic per product net profit margin for salesman from today's real outlet sales
    val penghasilanBersihSales = remember(todayTx, products) {
        todayTx.sumOf { tx ->
            val prod = products.find { it.id == tx.productId }
            val hBeliPerPcs = if (prod != null && prod.rasioKonversi > 0) prod.hargaBeliPabrik / prod.rasioKonversi else 0.0
            val marginPerPcs = (tx.hargaSatuan - hBeliPerPcs).coerceAtLeast(0.0)
            marginPerPcs * tx.pcsLaku
        }
    }

    val totalTitipBaruPcs = remember(todayTx) { todayTx.filter { it.jenis == "TITIP_BARU" }.sumOf { it.restockBaruPcs } }

    // Overdue count (> 14 days)
    val overdueCount = remember(warungs) { warungs.count { it.saldoPiutang > 0 } }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Persistent Executive Header (Always Visible)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
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
                            text = com.example.util.AppStrings.tr("SFA KONSINYASI", "CONSIGNMENT SFA", lang),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = com.example.util.AppStrings.tr("Dashboard Operasional", "Operational Dashboard", lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = todayDateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.testTag("driver_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = userProfile?.namaSalesman?.ifBlank { "Salesman" } ?: "Salesman",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {

        // AI Copilot Assistant Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openTransactionDialog(TransactionDialogState.AiCopilot) }
                    .testTag("ai_copilot_dashboard_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900, contentColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        AiRobotAvatar(
                            size = 38.dp,
                            containerBackground = Slate900,
                            accentColor = EmeraldPrimary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "TracerPro AI Copilot",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Surface(
                                    color = Slate800,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "OpenAI Gateway",
                                        color = EmeraldPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = com.example.util.AppStrings.tr("Draf WA Bos, Analisis Piutang & Saran Muat", "Boss WA Drafts, Debt Analysis & Loading Advice", lang),
                                fontSize = 11.sp,
                                color = Slate400,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { viewModel.openTransactionDialog(TransactionDialogState.AiCopilot) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Slate800,
                            contentColor = EmeraldPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(com.example.util.AppStrings.tr("Tanya AI", "Ask AI", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Today's Performance Summary Chart (Sales Volume & Outlet Visits)
        item {
            DashboardSummaryChart(
                todayTransactions = todayTx,
                totalWarungsInRoute = warungs.size,
                modifier = Modifier.testTag("dashboard_summary_chart"),
                lang = lang
            )
        }

        // 4 Virtual Inventory Drawers Card
        item {
            DrawerInventorySummary(
                stokFresh = totalFresh,
                stokBsBelumSortir = totalBsBelumSortir,
                stokPribadiLayak = totalPribadiLayak,
                stokPribadiRusak = totalPribadiRusak,
                onSortirClick = {
                    viewModel.openTransactionDialog(TransactionDialogState.SortirBs)
                },
                modifier = Modifier.testTag("inventory_drawer_card"),
                lang = lang
            )
        }

        // Quick Operational Actions Row
        item {
            Text(
                text = com.example.util.AppStrings.tr("AKSI OPERASIONAL", "OPERATIONAL ACTIONS", lang),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Muat Pagi
                QuickActionButton(
                    icon = Icons.Default.LocalShipping,
                    title = com.example.util.AppStrings.tr("Muat Pagi", "Load Stock", lang),
                    subtitle = com.example.util.AppStrings.tr("Inbound Pack", "Inbound Pack", lang),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_muat_pagi"),
                    onClick = {
                        viewModel.openTransactionDialog(TransactionDialogState.MuatPagi)
                    }
                )

                // Kunjungan Rute
                QuickActionButton(
                    icon = Icons.Default.Storefront,
                    title = com.example.util.AppStrings.tr("Kunjungan", "Visits", lang),
                    subtitle = com.example.util.AppStrings.tr("Titip & Bon", "Drops & Debt", lang),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_kunjungan_rute"),
                    onClick = {
                        viewModel.setScreen(AppNavScreen.TRANSAKSI)
                    }
                )

                // Closing Sore
                QuickActionButton(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = com.example.util.AppStrings.tr("Closing", "Closing", lang),
                    subtitle = com.example.util.AppStrings.tr("Setor Kas", "Settle Cash", lang),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_closing_sore"),
                    onClick = {
                        viewModel.openTransactionDialog(TransactionDialogState.ClosingSore)
                    }
                )
            }
        }

        // Key Financial Metrics
        item {
            Text(
                text = com.example.util.AppStrings.tr("RINGKASAN FINANSIAL", "FINANCIAL SUMMARY", lang),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalStatCard(
                    title = com.example.util.AppStrings.tr("Kas Diterima Warung", "Cash Collected", lang),
                    value = SfaViewModel.formatRupiah(totalKasHariIni),
                    subtitle = com.example.util.AppStrings.tr("${todayTx.size} transaksi outlet", "${todayTx.size} outlet visits", lang),
                    icon = Icons.Default.Payments,
                    accentColor = Slate800,
                    modifier = Modifier.weight(1f)
                )

                MinimalStatCard(
                    title = com.example.util.AppStrings.tr("Setoran Supplier", "Supplier Bill", lang),
                    value = SfaViewModel.formatRupiah(tagihanPabrik),
                    subtitle = if (isClosingDone) com.example.util.AppStrings.tr("Lunas Closing Sore", "Closed & Reconciled", lang) else com.example.util.AppStrings.tr("Sesuai Muat Pagi", "Estimated Morning Load", lang),
                    icon = Icons.Default.Factory,
                    accentColor = Slate800,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalStatCard(
                    title = com.example.util.AppStrings.tr("Total Bon Outlet", "Total Outlet Debt", lang),
                    value = SfaViewModel.formatRupiah(totalPiutangWarung),
                    subtitle = com.example.util.AppStrings.tr("$overdueCount outlet piutang", "$overdueCount outlets in debt", lang),
                    icon = Icons.Default.ReceiptLong,
                    accentColor = AmberWarning,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setScreen(AppNavScreen.LAPORAN)
                    }
                )

                MinimalStatCard(
                    title = com.example.util.AppStrings.tr("Penghasilan Sales", "Sales Earnings", lang),
                    value = SfaViewModel.formatRupiah(penghasilanBersihSales),
                    subtitle = com.example.util.AppStrings.tr("Margin Hak Bersih", "Net Profit Margin", lang),
                    icon = Icons.Default.TrendingUp,
                    accentColor = Slate800,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Route Progress
        item {
            val activeRute = rutes.firstOrNull()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(AppNavScreen.TRANSAKSI) },
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Slate100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AltRoute,
                                    contentDescription = null,
                                    tint = Slate800,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = activeRute?.namaRute ?: com.example.util.AppStrings.tr("Jalur 1 - Pasar Rebo & Cibubur", "Route 1 - East Sector", lang),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "${com.example.util.AppStrings.tr("Jadwal", "Schedule", lang)}: ${activeRute?.hariKunjungan ?: com.example.util.AppStrings.tr("Senin", "Monday", lang)} • ${com.example.util.AppStrings.tr("Estimasi", "Est.", lang)} ${activeRute?.jarakTotalKm ?: 18.5} km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate100,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Text(
                                text = "${todayTx.size}/50 ${com.example.util.AppStrings.tr("Toko", "Stores", lang)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (todayTx.size / 50f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Slate900,
                        trackColor = Slate100
                    )

                    val routeWarungs = remember(warungs, activeRute) {
                        if (activeRute != null) {
                            warungs.filter { it.ruteId == activeRute.id && it.status == "Aktif" }
                        } else {
                            warungs.filter { it.status == "Aktif" }
                        }
                    }
                    val mappedCount = routeWarungs.count { it.latitude != 0.0 || it.longitude != 0.0 }

                    Button(
                        onClick = {
                            LocationHelper.openMultiStopGoogleMapsRoute(
                                context = context,
                                warungs = routeWarungs,
                                routeTitle = activeRute?.namaRute ?: com.example.util.AppStrings.tr("Rute Hari Ini", "Today's Route", lang)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_start_route_navigation"),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${com.example.util.AppStrings.tr("Mulai Navigasi Rute di Google Maps", "Start Route Navigation in Google Maps", lang)} ($mappedCount ${com.example.util.AppStrings.tr("Outlet", "Outlets", lang)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Modal Tertanam Info
        if (tagihanPabrik > totalKasHariIni && totalKasHariIni > 0) {
            item {
                val kurangSetor = tagihanPabrik - totalKasHariIni
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "${com.example.util.AppStrings.tr("Info Alokasi Kas & Modal Tertanam", "Cash Allocation & Invested Capital Info", lang)}: ${SfaViewModel.formatRupiah(kurangSetor)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberText
                            )
                            Text(
                                text = if (totalTitipBaruPcs > 0) {
                                    com.example.util.AppStrings.tr(
                                        "Terdapat $totalTitipBaruPcs Pcs Titip Baru di warung hari ini. Selisih kas ini adalah modal tertanam Anda yang akan cair saat warung isi ulang minggu depan.",
                                        "There are $totalTitipBaruPcs new drop units placed today. This cash difference is your invested capital that will be collected on next week's refill.",
                                        lang
                                    )
                                } else {
                                    com.example.util.AppStrings.tr(
                                        "Kas warung terkumpul hari ini belum menutup setoran muat pabrik. Pastikan closing sore sesuai fisik mobil.",
                                        "Collected outlet cash has not fully covered supplier bill yet. Ensure evening closing matches physical van inventory.",
                                        lang
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberText.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Slate800,
                    modifier = Modifier.size(17.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

