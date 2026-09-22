package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PersonalAccountEntity
import com.example.data.local.entity.PersonalDebtEntity
import com.example.data.local.entity.PersonalExpenseEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.SfaViewModel
import com.example.ui.viewmodel.TransactionDialogState
import com.example.util.AppStrings
import com.example.util.LocalAppLanguage

enum class KeuanganSubTab(val titleId: String, val titleEn: String, val icon: ImageVector) {
    OVERVIEW("Ringkasan", "Overview", Icons.Default.PieChart),
    PENGELUARAN("Pengeluaran", "Expenses", Icons.Default.ReceiptLong),
    HUTANG("Hutang & Piutang", "Debts & Loans", Icons.Default.Handshake),
    DOMPET("Dompet & Paylater", "Wallets & Cards", Icons.Default.AccountBalanceWallet)
}

@Composable
fun KeuanganPribadiScreen(
    viewModel: SfaViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val accounts by viewModel.personalAccounts.collectAsState()
    val expenses by viewModel.personalExpenses.collectAsState()
    val debts by viewModel.personalDebts.collectAsState()

    var selectedTab by remember { mutableStateOf(KeuanganSubTab.OVERVIEW) }
    var selectedFilterDebtType by remember { mutableStateOf("ALL") } // "ALL", "PIUTANG_SAYA", "HUTANG_SAYA"
    var showLunasDebts by remember { mutableStateOf(false) }

    // Computations
    val totalCashAndBank = remember(accounts) {
        accounts.filter { !it.isPaylater }.sumOf { it.saldo }
    }
    val totalTagihanPaylater = remember(accounts) {
        accounts.filter { it.isPaylater }.sumOf { it.saldo }
    }
    val totalLimitPaylater = remember(accounts) {
        accounts.filter { it.isPaylater }.sumOf { it.limitKredit }
    }

    // Debts calculations
    val totalPiutangTeman = remember(debts) {
        debts.filter { it.jenis == "PIUTANG_SAYA" && it.status != "LUNAS" }.sumOf { it.sisaNominal }
    }
    val totalHutangSaya = remember(debts) {
        debts.filter { it.jenis == "HUTANG_SAYA" && it.status != "LUNAS" }.sumOf { it.sisaNominal }
    }

    // Expenses calculations
    val totalPengeluaranBulan = remember(expenses) {
        expenses.filter { it.jenis == "PENGELUARAN" }.sumOf { it.nominal }
    }
    val totalPemasukanBulan = remember(expenses) {
        expenses.filter { it.jenis == "PEMASUKAN" }.sumOf { it.nominal }
    }

    // Net Worth (Kekayaan Bersih Pribadi = Kas/Bank + Piutang Teman - Hutang Saya - Tagihan Paylater)
    val kekayaanBersih = totalCashAndBank + totalPiutangTeman - totalHutangSaya - totalTagihanPaylater

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // App Top Bar
        Surface(
            color = Color.White,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100)
                                .testTag("btn_drawer_keuangan")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Buka Menu",
                                tint = Slate900,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = AppStrings.tr("Manajemen Keuangan Pribadi", "Personal Finance Manager", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate950
                            )
                            Text(
                                text = AppStrings.tr("Dompet tunai, bank, hutang teman & paylater", "Cash, banks, loans & paylater", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Quick Action: Catat Pengeluaran
                    Button(
                        onClick = {
                            viewModel.openTransactionDialog(TransactionDialogState.AddPersonalExpense("PENGELUARAN"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate900,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_catat_pengeluaran_top")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.tr("Catat Kas", "Add Tx", lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Horizontal Sub Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.White,
                    contentColor = Slate900,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider(color = Slate200, thickness = 1.dp) }
                ) {
                    KeuanganSubTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Slate900 else Slate400
                                    )
                                    Text(
                                        text = if (lang.equals("EN", ignoreCase = true)) tab.titleEn else tab.titleId,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Slate900 else Slate500
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            KeuanganSubTab.OVERVIEW -> {
                KeuanganOverviewTab(
                    viewModel = viewModel,
                    lang = lang,
                    kekayaanBersih = kekayaanBersih,
                    totalCashAndBank = totalCashAndBank,
                    totalTagihanPaylater = totalTagihanPaylater,
                    totalPiutangTeman = totalPiutangTeman,
                    totalHutangSaya = totalHutangSaya,
                    totalPengeluaranBulan = totalPengeluaranBulan,
                    totalPemasukanBulan = totalPemasukanBulan,
                    accounts = accounts,
                    recentExpenses = expenses.take(5),
                    activeDebts = debts.filter { it.status != "LUNAS" }.take(5),
                    onNavigateTab = { selectedTab = it }
                )
            }
            KeuanganSubTab.PENGELUARAN -> {
                KeuanganPengeluaranTab(
                    viewModel = viewModel,
                    lang = lang,
                    expenses = expenses,
                    accounts = accounts,
                    totalPengeluaran = totalPengeluaranBulan,
                    totalPemasukan = totalPemasukanBulan
                )
            }
            KeuanganSubTab.HUTANG -> {
                KeuanganHutangTab(
                    viewModel = viewModel,
                    lang = lang,
                    debts = debts,
                    accounts = accounts,
                    totalPiutangTeman = totalPiutangTeman,
                    totalHutangSaya = totalHutangSaya,
                    filterType = selectedFilterDebtType,
                    onFilterChange = { selectedFilterDebtType = it },
                    showLunas = showLunasDebts,
                    onToggleShowLunas = { showLunasDebts = !showLunasDebts }
                )
            }
            KeuanganSubTab.DOMPET -> {
                KeuanganDompetTab(
                    viewModel = viewModel,
                    lang = lang,
                    accounts = accounts,
                    totalCashAndBank = totalCashAndBank,
                    totalTagihanPaylater = totalTagihanPaylater,
                    totalLimitPaylater = totalLimitPaylater
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: OVERVIEW SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
private fun KeuanganOverviewTab(
    viewModel: SfaViewModel,
    lang: String,
    kekayaanBersih: Double,
    totalCashAndBank: Double,
    totalTagihanPaylater: Double,
    totalPiutangTeman: Double,
    totalHutangSaya: Double,
    totalPengeluaranBulan: Double,
    totalPemasukanBulan: Double,
    accounts: List<PersonalAccountEntity>,
    recentExpenses: List<PersonalExpenseEntity>,
    activeDebts: List<PersonalDebtEntity>,
    onNavigateTab: (KeuanganSubTab) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Total Saldo Bersih & Status Paylater
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900, contentColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = AppStrings.tr("Kekayaan Bersih Pribadi", "Total Net Balance", lang),
                                fontSize = 12.sp,
                                color = Slate400,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = SfaViewModel.formatRupiah(kekayaanBersih),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (kekayaanBersih >= 0) Color.White else Color(0xFFFCA5A5)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Slate800
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess)
                                )
                                Text(
                                    text = "${accounts.size} Akun",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Slate800, thickness = 1.dp)

                    // 4-Column Quick Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(AppStrings.tr("Saldo Kas & Bank", "Cash & Bank", lang), fontSize = 10.sp, color = Slate400)
                            Text(
                                text = SfaViewModel.formatRupiah(totalCashAndBank),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(AppStrings.tr("Tagihan Paylater", "Paylater Bills", lang), fontSize = 10.sp, color = Slate400)
                            Text(
                                text = SfaViewModel.formatRupiah(totalTagihanPaylater),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalTagihanPaylater > 0) AmberWarning else Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(AppStrings.tr("Piutang (Teman Pinjam)", "Receivable (Friends)", lang), fontSize = 10.sp, color = Slate400)
                            Text(
                                text = SfaViewModel.formatRupiah(totalPiutangTeman),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(AppStrings.tr("Hutang Saya ke Orang", "My Personal Debts", lang), fontSize = 10.sp, color = Slate400)
                            Text(
                                text = SfaViewModel.formatRupiah(totalHutangSaya),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalHutangSaya > 0) RoseCritical else EmeraldSuccess
                            )
                        }
                    }

                    // Quick Action Buttons Inside Hero Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.AddPersonalExpense("PENGELUARAN"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseCritical, contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Catat Biaya", "Expense", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.AddPersonalExpense("PEMASUKAN"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Pemasukan", "Income", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.AddEditPersonalDebt(null))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Hutang", "Loan", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Horizontal Quick Accounts Scroll
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.tr("DOMPET, BANK & PAYLATER", "WALLETS, BANKS & PAYLATER", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = AppStrings.tr("Kelola Semua", "Manage All", lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        modifier = Modifier.clickable { onNavigateTab(KeuanganSubTab.DOMPET) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accounts.forEach { acc ->
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable {
                                    viewModel.openTransactionDialog(TransactionDialogState.AddEditPersonalAccount(acc))
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(if (acc.isPaylater) AmberWarning.copy(alpha = 0.5f) else Slate200))
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
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (acc.isPaylater) AmberWarning.copy(alpha = 0.15f) else Slate100),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (acc.isPaylater) Icons.Default.CreditCard else if (acc.tipeAkun == "BANK") Icons.Default.AccountBalance else Icons.Default.Payments,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (acc.isPaylater) AmberWarning else Slate800
                                        )
                                    }
                                    if (acc.isPaylater) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AmberWarning.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "PAYLATER",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberWarning,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = acc.namaAkun,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Column {
                                    Text(
                                        text = if (acc.isPaylater) AppStrings.tr("Tagihan Berjalan", "Current Used", lang) else AppStrings.tr("Saldo Aktif", "Balance", lang),
                                        fontSize = 9.sp,
                                        color = Slate500
                                    )
                                    Text(
                                        text = SfaViewModel.formatRupiah(acc.saldo),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (acc.isPaylater) (if (acc.saldo > 0) AmberWarning else Slate700) else EmeraldSuccess
                                    )
                                }
                            }
                        }
                    }

                    // Add Account Quick Button
                    OutlinedButton(
                        onClick = {
                            viewModel.openTransactionDialog(TransactionDialogState.AddEditPersonalAccount(null))
                        },
                        modifier = Modifier
                            .width(130.dp)
                            .height(115.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(AppStrings.tr("+ Akun Baru", "+ Add Account", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Debts Summary Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Handshake, contentDescription = null, tint = Slate900, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(
                                    text = AppStrings.tr("HUTANG & PIUTANG BELUM LUNAS", "ACTIVE DEBTS & LOANS", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate900,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${activeDebts.size} ${AppStrings.tr("catatan aktif berjalan", "active records", lang)}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        TextButton(
                            onClick = { onNavigateTab(KeuanganSubTab.HUTANG) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(AppStrings.tr("Lihat Semua", "View All", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                    }

                    if (activeDebts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AppStrings.tr("Semua hutang & piutang telah lunas! 🎉", "All debts & loans are fully settled! 🎉", lang),
                                fontSize = 12.sp,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        activeDebts.forEach { debt ->
                            val isPiutang = debt.jenis == "PIUTANG_SAYA"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.openTransactionDialog(TransactionDialogState.BayarCicilanHutang(debt))
                                    },
                                color = Slate50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = debt.namaPihak,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Slate900
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isPiutang) Color(0xFFEFF6FF) else Color(0xFFFEF2F2)
                                            ) {
                                                Text(
                                                    text = if (isPiutang) AppStrings.tr("DIA PINJAM KE KITA", "OWES ME", lang) else AppStrings.tr("KITA HUTANG", "I OWE", lang),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPiutang) Color(0xFF2563EB) else RoseCritical,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${debt.hubungan} • ${AppStrings.tr("Awal", "Initial", lang)}: ${SfaViewModel.formatRupiah(debt.totalNominal)}",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = SfaViewModel.formatRupiah(debt.sisaNominal),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isPiutang) Color(0xFF2563EB) else RoseCritical
                                        )
                                        Text(
                                            text = AppStrings.tr("Klik utk Bayar/Cicil", "Click to pay", lang),
                                            fontSize = 9.sp,
                                            color = Slate400
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Expenses List Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        Text(
                            text = AppStrings.tr("TRANSAKSI PENGELUARAN TERBARU", "RECENT EXPENSES", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate900,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { onNavigateTab(KeuanganSubTab.PENGELUARAN) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(AppStrings.tr("Lihat Riwayat", "View History", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                    }

                    if (recentExpenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AppStrings.tr("Belum ada pengeluaran dicatat. Klik '+ Catat Kas' di atas.", "No transactions logged yet.", lang),
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    } else {
                        recentExpenses.forEach { exp ->
                            ExpenseRowItem(
                                expense = exp,
                                accounts = accounts,
                                lang = lang,
                                onDelete = { viewModel.deletePersonalExpense(exp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 2: PENGELUARAN / EXPENSES SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
private fun KeuanganPengeluaranTab(
    viewModel: SfaViewModel,
    lang: String,
    expenses: List<PersonalExpenseEntity>,
    accounts: List<PersonalAccountEntity>,
    totalPengeluaran: Double,
    totalPemasukan: Double
) {
    var filterJenis by remember { mutableStateOf("ALL") } // "ALL", "PENGELUARAN", "PEMASUKAN", "TRANSFER"
    var searchQuery by remember { mutableStateOf("") }

    val filteredExpenses = remember(expenses, filterJenis, searchQuery) {
        expenses.filter { exp ->
            val matchJenis = filterJenis == "ALL" || exp.jenis == filterJenis
            val matchQuery = searchQuery.isBlank() || exp.judul.contains(searchQuery, ignoreCase = true) || exp.kategori.contains(searchQuery, ignoreCase = true)
            matchJenis && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Header Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(AppStrings.tr("Total Pengeluaran", "Total Expense", lang), fontSize = 10.sp, color = RoseCritical, fontWeight = FontWeight.SemiBold)
                        Text(SfaViewModel.formatRupiah(totalPengeluaran), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RoseCritical)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6EE7B7))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(AppStrings.tr("Total Pemasukan", "Total Income", lang), fontSize = 10.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                        Text(SfaViewModel.formatRupiah(totalPemasukan), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }
                }
            }
        }

        // Action & Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(AppStrings.tr("Cari pengeluaran, bensin, makan...", "Search expenses...", lang), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to AppStrings.tr("Semua", "All", lang),
                        "PENGELUARAN" to AppStrings.tr("Pengeluaran", "Expenses", lang),
                        "PEMASUKAN" to AppStrings.tr("Pemasukan", "Income", lang),
                        "TRANSFER" to AppStrings.tr("Transfer", "Transfer", lang)
                    ).forEach { (type, label) ->
                        val isSelected = filterJenis == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterJenis = type },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate900,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Slate700
                            )
                        )
                    }
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Slate400, modifier = Modifier.size(40.dp))
                        Text(AppStrings.tr("Tidak ada transaksi yang cocok", "No matching transactions", lang), color = Slate500, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(filteredExpenses, key = { it.id }) { expense ->
                ExpenseRowItem(
                    expense = expense,
                    accounts = accounts,
                    lang = lang,
                    onDelete = { viewModel.deletePersonalExpense(expense) }
                )
            }
        }
    }
}

@Composable
private fun ExpenseRowItem(
    expense: PersonalExpenseEntity,
    accounts: List<PersonalAccountEntity>,
    lang: String,
    onDelete: () -> Unit
) {
    val account = accounts.find { it.id == expense.accountId }
    val toAccount = accounts.find { it.id == expense.toAccountId }
    val isOut = expense.jenis == "PENGELUARAN"
    val isIn = expense.jenis == "PEMASUKAN"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOut) Color(0xFFFEE2E2) else if (isIn) Color(0xFFD1FAE5) else Color(0xFFE0E7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOut) Icons.Default.ArrowOutward else if (isIn) Icons.Default.ArrowDownward else Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = if (isOut) RoseCritical else if (isIn) EmeraldSuccess else Color(0xFF4F46E5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = expense.judul,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${expense.kategori} • ${expense.tanggal}",
                            fontSize = 10.sp,
                            color = Slate500
                        )
                    }
                    val accLabel = if (expense.jenis == "TRANSFER") {
                        "${account?.namaAkun ?: "Akun"} ➜ ${toAccount?.namaAkun ?: "Tujuan"}"
                    } else {
                        account?.namaAkun ?: "Akun Terhapus"
                    }
                    Text(
                        text = accLabel,
                        fontSize = 10.sp,
                        color = Slate600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isOut) "-" else if (isIn) "+" else ""}${SfaViewModel.formatRupiah(expense.nominal)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isOut) RoseCritical else if (isIn) EmeraldSuccess else Color(0xFF4F46E5)
                    )
                    if (expense.catatan.isNotBlank()) {
                        Text(
                            text = expense.catatan,
                            fontSize = 9.sp,
                            color = Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 3: HUTANG & PIUTANG SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
private fun KeuanganHutangTab(
    viewModel: SfaViewModel,
    lang: String,
    debts: List<PersonalDebtEntity>,
    accounts: List<PersonalAccountEntity>,
    totalPiutangTeman: Double,
    totalHutangSaya: Double,
    filterType: String,
    onFilterChange: (String) -> Unit,
    showLunas: Boolean,
    onToggleShowLunas: () -> Unit
) {
    val filteredDebts = remember(debts, filterType, showLunas) {
        debts.filter { d ->
            val matchType = filterType == "ALL" || d.jenis == filterType
            val matchStatus = showLunas || d.status != "LUNAS"
            matchType && matchStatus
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Debts Stats Overview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Piutang Card (Teman pinjam uang kita)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(AppStrings.tr("Piutang (Orang Lain Pinjam)", "Receivables (Owed to Me)", lang), fontSize = 10.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                        Text(SfaViewModel.formatRupiah(totalPiutangTeman), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                        Text(AppStrings.tr("Uang kita di luar", "Money owed to us", lang), fontSize = 9.sp, color = Color(0xFF3B82F6))
                    }
                }

                // Hutang Card (Kita pinjam ke orang lain)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(AppStrings.tr("Hutang Saya ke Orang", "My Personal Debts", lang), fontSize = 10.sp, color = RoseCritical, fontWeight = FontWeight.Bold)
                        Text(SfaViewModel.formatRupiah(totalHutangSaya), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RoseCritical)
                        Text(AppStrings.tr("Kewajiban harus dibayar", "Must pay back", lang), fontSize = 9.sp, color = RoseCritical.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // Add Debt Button & Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        viewModel.openTransactionDialog(TransactionDialogState.AddEditPersonalDebt(null))
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.tr("Catat Hutang/Piutang", "Add Debt/Loan", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(AppStrings.tr("Tampilkan Lunas", "Show Settled", lang), fontSize = 11.sp, color = Slate600)
                    Checkbox(
                        checked = showLunas,
                        onCheckedChange = { onToggleShowLunas() },
                        colors = CheckboxDefaults.colors(checkedColor = Slate900)
                    )
                }
            }
        }

        // Filter Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to AppStrings.tr("Semua", "All", lang),
                    "PIUTANG_SAYA" to AppStrings.tr("Teman/Keluarga Pinjam", "Owed to Me", lang),
                    "HUTANG_SAYA" to AppStrings.tr("Kita Berhutang", "I Owe Others", lang)
                ).forEach { (type, label) ->
                    val isSelected = filterType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterChange(type) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate900,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Slate700
                        )
                    )
                }
            }
        }

        if (filteredDebts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.tr("Tidak ada catatan hutang/piutang yang aktif.", "No active debt records.", lang),
                        color = Slate500,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(filteredDebts, key = { it.id }) { debt ->
                DebtCardItem(
                    debt = debt,
                    lang = lang,
                    onPay = { viewModel.openTransactionDialog(TransactionDialogState.BayarCicilanHutang(debt)) },
                    onEdit = { viewModel.openTransactionDialog(TransactionDialogState.AddEditPersonalDebt(debt)) },
                    onDelete = { viewModel.deletePersonalDebt(debt) }
                )
            }
        }
    }
}

@Composable
private fun DebtCardItem(
    debt: PersonalDebtEntity,
    lang: String,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPiutang = debt.jenis == "PIUTANG_SAYA"
    val isLunas = debt.status == "LUNAS"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isLunas) Slate200 else if (isPiutang) Color(0xFF93C5FD) else Color(0xFFFCA5A5))
        )
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLunas) Slate100 else if (isPiutang) Color(0xFFEFF6FF) else Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPiutang) Icons.Default.CallReceived else Icons.Default.CallMade,
                            contentDescription = null,
                            tint = if (isLunas) Slate500 else if (isPiutang) Color(0xFF2563EB) else RoseCritical,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = debt.namaPihak,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${debt.hubungan} ${if (debt.kontak.isNotBlank()) "• ${debt.kontak}" else ""}",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isLunas) EmeraldSuccess.copy(alpha = 0.15f) else if (isPiutang) Color(0xFFEFF6FF) else Color(0xFFFEF2F2)
                ) {
                    Text(
                        text = if (isLunas) "LUNAS" else if (isPiutang) AppStrings.tr("PINJAM KE KITA", "OWES ME", lang) else AppStrings.tr("KITA HUTANG", "I OWE", lang),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLunas) EmeraldSuccess else if (isPiutang) Color(0xFF2563EB) else RoseCritical,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.tr("Sisa Tagihan / Hutang", "Remaining Balance", lang),
                        fontSize = 10.sp,
                        color = Slate500
                    )
                    Text(
                        text = SfaViewModel.formatRupiah(debt.sisaNominal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLunas) Slate400 else if (isPiutang) Color(0xFF2563EB) else RoseCritical
                    )
                    Text(
                        text = "${AppStrings.tr("Total Awal:", "Original:", lang)} ${SfaViewModel.formatRupiah(debt.totalNominal)}",
                        fontSize = 10.sp,
                        color = Slate400
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isLunas) {
                        Button(
                            onClick = onPay,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPiutang) Color(0xFF2563EB) else RoseCritical,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isPiutang) AppStrings.tr("Terima Cicilan", "Receive", lang) else AppStrings.tr("Bayar Cicilan", "Pay", lang),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate600, modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (debt.catatan.isNotBlank() || debt.tanggalJatuhTempo.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (debt.tanggalJatuhTempo.isNotBlank()) {
                            Text(
                                text = "⏰ ${AppStrings.tr("Janji Bayar / Jatuh Tempo:", "Due date:", lang)} ${debt.tanggalJatuhTempo}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                        }
                        if (debt.catatan.isNotBlank()) {
                            Text(
                                text = "📝 ${debt.catatan}",
                                fontSize = 10.sp,
                                color = Slate600
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 4: DOMPET, BANK & PAYLATER SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
private fun KeuanganDompetTab(
    viewModel: SfaViewModel,
    lang: String,
    accounts: List<PersonalAccountEntity>,
    totalCashAndBank: Double,
    totalTagihanPaylater: Double,
    totalLimitPaylater: Double
) {
    val standardAccounts = accounts.filter { !it.isPaylater }
    val paylaterAccounts = accounts.filter { it.isPaylater }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Cards: Saldo Tunai vs Paylater
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900, contentColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(AppStrings.tr("Total Saldo Cash & Bank", "Total Cash & Bank", lang), fontSize = 10.sp, color = Slate400)
                        Text(SfaViewModel.formatRupiah(totalCashAndBank), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        Text("${standardAccounts.size} ${AppStrings.tr("rekening & dompet", "accounts", lang)}", fontSize = 10.sp, color = Slate400)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(AppStrings.tr("Total Tagihan Paylater", "Total Paylater Bills", lang), fontSize = 10.sp, color = AmberWarning, fontWeight = FontWeight.Bold)
                        Text(SfaViewModel.formatRupiah(totalTagihanPaylater), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                        Text(
                            text = "${AppStrings.tr("Limit sisa:", "Avail limit:", lang)} ${SfaViewModel.formatRupiah((totalLimitPaylater - totalTagihanPaylater).coerceAtLeast(0.0))}",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }
            }
        }

        // Header and Add Account Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.tr("REKENING & DOMPET AKTIF", "ACTIVE WALLETS & BANKS", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Button(
                    onClick = {
                        viewModel.openTransactionDialog(TransactionDialogState.AddEditPersonalAccount(null))
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.tr("Tambah Akun", "Add Account", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Standard Cash / Bank Accounts
        if (standardAccounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AppStrings.tr("Belum ada akun dompet / bank. Klik 'Tambah Akun' untuk mencatat dompet atau rekening Anda.", "No wallet or bank accounts yet. Click 'Add Account' to register.", lang),
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }
            }
        } else {
            items(standardAccounts, key = { it.id }) { acc ->
                AccountCardItem(
                    account = acc,
                    lang = lang,
                    onEdit = { viewModel.openTransactionDialog(TransactionDialogState.AddEditPersonalAccount(acc)) },
                    onDelete = { viewModel.deletePersonalAccount(acc) }
                )
            }
        }

        // Section Paylater & Cicilan
        item {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = AppStrings.tr("FASILITAS PAYLATER & KARTU KREDIT", "PAYLATER & CREDIT CARDS", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberWarning,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = AppStrings.tr("Catatan limit pagu, tagihan pemakaian, dan tanggal jatuh tempo bulanan", "Track credit limits, current usage and due dates", lang),
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }

        if (paylaterAccounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AppStrings.tr("Belum ada akun Paylater/Kartu Kredit. Klik 'Tambah Akun' untuk mencatat.", "No Paylater accounts registered yet.", lang),
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }
            }
        } else {
            items(paylaterAccounts, key = { it.id }) { acc ->
                AccountCardItem(
                    account = acc,
                    lang = lang,
                    onEdit = { viewModel.openTransactionDialog(TransactionDialogState.AddEditPersonalAccount(acc)) },
                    onDelete = { viewModel.deletePersonalAccount(acc) }
                )
            }
        }
    }
}

@Composable
private fun AccountCardItem(
    account: PersonalAccountEntity,
    lang: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(if (account.isPaylater) AmberWarning.copy(alpha = 0.5f) else Slate200))
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (account.isPaylater) AmberWarning.copy(alpha = 0.15f) else Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (account.tipeAkun) {
                                "BANK" -> Icons.Default.AccountBalance
                                "EWALLET" -> Icons.Default.PhoneAndroid
                                "PAYLATER" -> Icons.Default.CreditCard
                                else -> Icons.Default.Payments
                            },
                            contentDescription = null,
                            tint = if (account.isPaylater) AmberWarning else Slate800,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = account.namaAkun,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = if (account.nomorRekening.isNotBlank()) "${account.tipeAkun} • ${account.nomorRekening}" else account.tipeAkun,
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate600, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            if (account.isPaylater) {
                // Paylater layout: Pemakaian & Limit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(AppStrings.tr("Tagihan / Pemakaian", "Current Used", lang), fontSize = 10.sp, color = Slate500)
                        Text(
                            text = SfaViewModel.formatRupiah(account.saldo),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (account.saldo > 0) AmberWarning else Slate700
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(AppStrings.tr("Pagu Limit Disetujui", "Approved Credit Limit", lang), fontSize = 10.sp, color = Slate500)
                        Text(
                            text = SfaViewModel.formatRupiah(account.limitKredit),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                    }
                }

                if (account.tanggalJatuhTempo > 0) {
                    Text(
                        text = "📅 ${AppStrings.tr("Jatuh tempo setiap tanggal", "Due date every", lang)} ${account.tanggalJatuhTempo} ${AppStrings.tr("tiap bulan", "each month", lang)}",
                        fontSize = 10.sp,
                        color = AmberWarning,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Standard Cash / Bank layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(AppStrings.tr("Saldo Tersedia", "Available Balance", lang), fontSize = 10.sp, color = Slate500)
                        Text(
                            text = SfaViewModel.formatRupiah(account.saldo),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }

                    if (account.catatan.isNotBlank()) {
                        Text(
                            text = account.catatan,
                            fontSize = 10.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
