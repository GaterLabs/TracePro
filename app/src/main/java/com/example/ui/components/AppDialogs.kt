package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.local.entity.*
import com.example.data.repository.LoadingItemInput
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import com.example.util.LocationHelper
import com.example.util.LocalAppLanguage
import com.example.util.AppStrings.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppDialogsHost(
    viewModel: SfaViewModel
) {
    val lang = LocalAppLanguage.current

val activeDialog by viewModel.activeTransactionDialog.collectAsState()
    val receiptTx by viewModel.showReceiptDialog.collectAsState()
    val closingReceiptData by viewModel.showClosingReceipt.collectAsState()
    val warungs by viewModel.warungs.collectAsState()
    val products by viewModel.products.collectAsState()
    val drawers by viewModel.drawers.collectAsState()
    val dailyLoadings by viewModel.dailyLoadings.collectAsState()
    val rutes by viewModel.rutes.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val bsSortirs by viewModel.bsSortirs.collectAsState()
    val customPrices by viewModel.customPrices.collectAsState()
    val pabriks by viewModel.pabriks.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    receiptTx?.let { tx ->
        val warung = warungs.find { it.id == tx.warungId }
        val product = products.find { it.id == tx.productId }
        ReceiptDialog(
            transaction = tx,
            warung = warung,
            product = product,
            userProfile = userProfile,
            onDismiss = { viewModel.closeReceipt() }
        )
    }

    closingReceiptData?.let { closingData ->
        ClosingReceiptDialog(
            data = closingData,
            userProfile = userProfile,
            onDismiss = { viewModel.closeClosingReceipt() }
        )
    }

    when (val state = activeDialog) {
        is TransactionDialogState.MuatPagi -> {
            MuatPagiDialog(
                products = products,
                drawers = drawers,
                dailyLoadings = dailyLoadings,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSubmitBatch = { items ->
                    viewModel.executeBatchLoadingPagi(items)
                }
            )
        }
        is TransactionDialogState.TitipBaru -> {
            val currentWarung = warungs.find { it.id == state.warung.id } ?: state.warung
            TitipBaruDialog(
                warung = currentWarung,
                products = products,
                drawers = drawers,
                dailyLoadings = dailyLoadings,
                customPrices = customPrices,
                onDismiss = { viewModel.closeTransactionDialog() },
                onNavigateToMuatPagi = {
                    viewModel.openTransactionDialog(TransactionDialogState.MuatPagi)
                },
                onSubmit = { productId, sumber, qty, harga, lat, lng, addr, note ->
                    viewModel.executeTitipBaru(
                        warung = currentWarung,
                        productId = productId,
                        sumberStok = sumber,
                        jumlahPcs = qty,
                        hargaSatuan = harga,
                        gpsLat = lat,
                        gpsLng = lng,
                        gpsAddress = addr,
                        catatan = note
                    )
                }
            )
        }
        is TransactionDialogState.TarikSisa -> {
            val currentWarung = warungs.find { it.id == state.warung.id } ?: state.warung
            TarikSisaDialog(
                warung = currentWarung,
                products = products,
                drawers = drawers,
                dailyLoadings = dailyLoadings,
                transactions = transactions,
                customPrices = customPrices,
                onDismiss = { viewModel.closeTransactionDialog() },
                onNavigateToMuatPagi = {
                    viewModel.openTransactionDialog(TransactionDialogState.MuatPagi)
                },
                onSubmit = { productId, sisaLalu, sisaFisik, harga, bayar, restock, sumber, lat, lng, addr, note ->
                    viewModel.executeTarikSisaDanRestock(
                        warung = currentWarung,
                        productId = productId,
                        sisaTitipanLalu = sisaLalu,
                        sisaFisik = sisaFisik,
                        hargaSatuan = harga,
                        uangDiterima = bayar,
                        restockPcs = restock,
                        sumberRestock = sumber,
                        gpsLat = lat,
                        gpsLng = lng,
                        gpsAddress = addr,
                        catatan = note
                    )
                }
            )
        }
        is TransactionDialogState.SortirBs -> {
            SortirBsDialog(
                products = products,
                drawers = drawers,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSubmit = { productId, totalAwal, layak, rusak, hb, hj, note ->
                    viewModel.executeSortirBs(productId, totalAwal, layak, rusak, hb, hj, note)
                }
            )
        }
        is TransactionDialogState.ClosingSore -> {
            ClosingSoreDialog(
                loadings = dailyLoadings,
                products = products,
                pabriks = pabriks,
                drawers = drawers,
                transactions = transactions,
                bsSortirs = bsSortirs,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSubmitBatch = { items, summary ->
                    viewModel.executeBatchClosingSore(items, summary)
                }
            )
        }
        is TransactionDialogState.WriteOff -> {
            val currentWarung = warungs.find { it.id == state.warung.id } ?: state.warung
            WriteOffDialog(
                warung = currentWarung,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSubmit = { harga, alasan ->
                    viewModel.executeWriteOff(currentWarung, harga, alasan)
                }
            )
        }
        is TransactionDialogState.AddEditProduct -> {
            AddEditProductDialog(
                product = state.product,
                pabriks = pabriks,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSave = { viewModel.addOrUpdateProduct(it) }
            )
        }
        is TransactionDialogState.AddEditWarung -> {
            AddEditWarungDialog(
                warung = state.warung,
                rutes = rutes,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSave = { viewModel.addOrUpdateWarung(it) }
            )
        }
        is TransactionDialogState.AddEditRute -> {
            AddEditRuteDialog(
                rute = state.rute,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSave = { viewModel.addOrUpdateRute(it) }
            )
        }
        is TransactionDialogState.AddEditPabrik -> {
            AddEditPabrikDialog(
                pabrik = state.pabrik,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSave = { viewModel.addOrUpdatePabrik(it) }
            )
        }
        is TransactionDialogState.WarungDetail -> {
            val currentWarung = warungs.find { it.id == state.warung.id } ?: state.warung
            WarungDetailDialog(
                warung = currentWarung,
                onDismiss = { viewModel.closeTransactionDialog() },
                onWriteOff = {
                    viewModel.openTransactionDialog(TransactionDialogState.WriteOff(currentWarung))
                },
                onManageCustomPrices = {
                    viewModel.openTransactionDialog(TransactionDialogState.ManageCustomPrices(currentWarung))
                },
                onViewStatistics = {
                    viewModel.openTransactionDialog(TransactionDialogState.OutletStatistics(currentWarung))
                }
            )
        }
        is TransactionDialogState.OutletStatistics -> {
            val currentWarung = warungs.find { it.id == state.warung.id } ?: state.warung
            val warungTx = transactions.filter { it.warungId == currentWarung.id }
            OutletStatisticsDialog(
                warung = currentWarung,
                transactions = warungTx,
                products = products,
                userProfile = userProfile,
                onDismiss = { viewModel.closeTransactionDialog() },
                onTitipBaru = {
                    viewModel.openTransactionDialog(TransactionDialogState.TitipBaru(currentWarung))
                },
                onTarikSisa = {
                    viewModel.openTransactionDialog(TransactionDialogState.TarikSisa(currentWarung))
                },
                onManageCustomPrices = {
                    viewModel.openTransactionDialog(TransactionDialogState.ManageCustomPrices(currentWarung))
                },
                onAiRecommendation = {
                    viewModel.openTransactionDialog(TransactionDialogState.AiOutletRecommendation(currentWarung))
                }
            )
        }
        is TransactionDialogState.ManageCustomPrices -> {
            val currentWarung = warungs.find { it.id == state.warung.id } ?: state.warung
            ManageCustomPricesDialog(
                warung = currentWarung,
                products = products,
                customPrices = customPrices,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSaveCustomPrice = { productId, price ->
                    viewModel.setCustomPrice(currentWarung.id, productId, price)
                },
                onDeleteCustomPrice = { productId ->
                    viewModel.deleteCustomPrice(currentWarung.id, productId)
                }
            )
        }
        is TransactionDialogState.SetupProfile -> {
            UserProfileDialog(
                currentProfile = userProfile,
                onDismiss = { viewModel.closeTransactionDialog() },
                onSave = { viewModel.saveUserProfile(it) }
            )
        }
        is TransactionDialogState.GpsTool -> {
            GpsToolDialog(onDismiss = { viewModel.closeTransactionDialog() })
        }
        is TransactionDialogState.ExportBackup -> {
            ExportBackupDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeTransactionDialog() }
            )
        }
        is TransactionDialogState.ImportBackup -> {
            ImportBackupDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeTransactionDialog() }
            )
        }
        is TransactionDialogState.AiCopilot -> {
            AiCopilotDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeTransactionDialog() },
                onOpenSettings = {
                    viewModel.openTransactionDialog(TransactionDialogState.AiConfigSettings)
                }
            )
        }
        is TransactionDialogState.AiConfigSettings -> {
            AiConfigDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeTransactionDialog() }
            )
        }
        is TransactionDialogState.AiOutletRecommendation -> {
            val currentWarung = warungs.find { it.id == state.warung.id } ?: state.warung
            AiOutletRecommendationDialog(
                warung = currentWarung,
                viewModel = viewModel,
                onDismiss = { viewModel.closeTransactionDialog() },
                onTitipBaruClicked = {
                    viewModel.openTransactionDialog(TransactionDialogState.TitipBaru(currentWarung))
                }
            )
        }
        is TransactionDialogState.BayarHutangSupplier -> {
            BayarHutangSupplierDialog(
                loading = state.loading,
                products = products,
                pabriks = pabriks,
                onDismiss = { viewModel.closeTransactionDialog() },
                onConfirmPay = { amount ->
                    val prod = products.find { it.id == state.loading.productId }
                    viewModel.executePayLoadingDebt(state.loading.id, amount, prod?.nama ?: "")
                }
            )
        }
        is TransactionDialogState.EditConfig -> {
            viewModel.closeTransactionDialog()
        }
        null -> {}
    }
}

// 1. MUAT PAGI DIALOG (MULTI-ITEM / BATCH LOADING DENGAN OPSI PEMBAYARAN)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuatPagiDialog(
    products: List<ProductEntity>,
    drawers: List<InventoryDrawerEntity> = emptyList(),
    dailyLoadings: List<DailyLoadingEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSubmitBatch: (List<LoadingItemInput>) -> Unit
) {
    val lang = LocalAppLanguage.current

val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayLoadings = remember(dailyLoadings, today) {
        dailyLoadings.filter { it.tanggal == today }
    }

    // Map of productId to quantity in Pack (string input)
    var quantities by remember {
        mutableStateOf(
            products.associate { it.id to "0" }
        )
    }

    var opsiBayarMuat by remember { mutableStateOf("BAYAR_CLOSING") } // "BAYAR_CLOSING", "BAYAR_LANGSUNG", "HUTANG"
    var dpNominalInput by remember { mutableStateOf("") }
    var catatanMuat by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(products) {
        products.map { it.kategori }.distinct().sorted()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchQuery = searchQuery.isBlank() ||
                    p.nama.contains(searchQuery, ignoreCase = true) ||
                    p.kategori.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == null || p.kategori == selectedCategory
            matchQuery && matchCat
        }
    }

    // Summary calculations
    val totalSkuWithQty = remember(quantities, products) {
        quantities.count { (_, qtyStr) -> (qtyStr.toIntOrNull() ?: 0) > 0 }
    }
    val totalPack = remember(quantities) {
        quantities.values.sumOf { it.toIntOrNull() ?: 0 }
    }
    val totalPcsFresh = remember(quantities, products) {
        quantities.entries.sumOf { (id, qtyStr) ->
            val p = products.find { it.id == id }
            val qty = qtyStr.toIntOrNull() ?: 0
            qty * (p?.rasioKonversi ?: 10)
        }
    }
    val totalPotensiHutang = remember(quantities, products) {
        quantities.entries.sumOf { (id, qtyStr) ->
            val p = products.find { it.id == id }
            val qty = qtyStr.toIntOrNull() ?: 0
            val hargaPack = p?.hargaBeliPabrik ?: (11000.0 * (p?.rasioKonversi ?: 10))
            qty * hargaPack
        }
    }

    val dpAmount = (dpNominalInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val sisaHutangEst = (totalPotensiHutang - dpAmount).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Slate900),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(tr("Muat Barang Pagi (Inbound)", "Morning Loading (Inbound)", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(tr("Pilih produk & metode pembayaran supplier", "Select products & supplier payment method", lang), fontSize = 11.sp, color = Slate500)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500)
                    }
                }

                // Live Total Summary Bar
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    modifier = Modifier.fillMaxWidth()
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
                            Column {
                                Text(tr("TOTAL MUATAN HARI INI", "TODAY'S TOTAL LOAD", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(tr("$totalSkuWithQty SKU Produk • $totalPack Pack", "$totalSkuWithQty Product SKUs • $totalPack Packs", lang), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(tr("Laci Fresh Mobil", "Car Fresh Compartment", lang), color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(tr("+$totalPcsFresh Pcs", "+$totalPcsFresh Pcs", lang), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("Nilai Muatan Supplier:", "Supplier Load Value:", lang), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(SfaViewModel.formatRupiah(totalPotensiHutang), color = AmberWarning, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // OPSI PEMBAYARAN MUAT BARANG (KONSINYASI / TUNAI / HUTANG TEMPO)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            tr("Opsi Pembayaran ke Supplier / Pabrik:", "Payment Option to Supplier / Factory:", lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Option 1: Bayar Pas Closing (Konsinyasi)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (opsiBayarMuat == "BAYAR_CLOSING") Slate900 else Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (opsiBayarMuat == "BAYAR_CLOSING") Slate900 else Slate300),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { opsiBayarMuat = "BAYAR_CLOSING" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        tr("Pas Closing", "At Closing", lang),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (opsiBayarMuat == "BAYAR_CLOSING") Color.White else Slate900,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        tr("Setor Sore", "Evening Deposit", lang),
                                        fontSize = 9.sp,
                                        color = if (opsiBayarMuat == "BAYAR_CLOSING") Slate300 else Slate500,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Option 2: Bayar Tunai Langsung
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (opsiBayarMuat == "BAYAR_LANGSUNG") EmeraldSuccess else Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (opsiBayarMuat == "BAYAR_LANGSUNG") EmeraldSuccess else Slate300),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { opsiBayarMuat = "BAYAR_LANGSUNG" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        tr("Bayar Langsung", "Pay Directly", lang),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (opsiBayarMuat == "BAYAR_LANGSUNG") Color.White else Slate900,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        tr("Lunas Tunai", "Cash Paid", lang),
                                        fontSize = 9.sp,
                                        color = if (opsiBayarMuat == "BAYAR_LANGSUNG") Color.White.copy(alpha = 0.8f) else Slate500,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Option 3: Catat Hutang (Bisa Tempo / Cicil)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (opsiBayarMuat == "HUTANG") AmberWarning else Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (opsiBayarMuat == "HUTANG") AmberWarning else Slate300),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { opsiBayarMuat = "HUTANG" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        tr("Hutang Tempo", "Credit / Due", lang),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (opsiBayarMuat == "HUTANG") Slate900 else Slate900,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        tr("Bisa Cicil / Sisa", "Installment / Balance", lang),
                                        fontSize = 9.sp,
                                        color = if (opsiBayarMuat == "HUTANG") Slate800 else Slate500,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Detailed fields for HUTANG
                        if (opsiBayarMuat == "HUTANG") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tr("Uang Muka / Bayar Sekarang (Opsional):", "Down Payment / Pay Now (Optional):", lang), fontSize = 11.sp, color = Slate700, fontWeight = FontWeight.Medium)
                                        Text(tr("Sisa Hutang: ${SfaViewModel.formatRupiah(sisaHutangEst)}", "Remaining Debt: ${SfaViewModel.formatRupiah(sisaHutangEst)}", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoseDanger)
                                    }

                                    OutlinedTextField(
                                        value = dpNominalInput,
                                        onValueChange = { input ->
                                            if (input.all { it.isDigit() }) dpNominalInput = input
                                        },
                                        placeholder = { Text(tr("0 (Full Hutang / Tempo)", "0 (Full Debt / Due)", lang), fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = appTextFieldColors(),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Quick DP Preset Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(0, 25, 50, 75).forEach { pct ->
                                            val nominal = if (pct == 0) 0.0 else (totalPotensiHutang * pct / 100.0)
                                            SuggestionChip(
                                                onClick = {
                                                    dpNominalInput = if (pct == 0) "0" else nominal.toLong().toString()
                                                },
                                                label = {
                                                    Text(if (pct == 0) "Full Hutang (Rp 0)" else "$pct% (${SfaViewModel.formatRupiah(nominal)})", fontSize = 9.sp)
                                                },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (dpAmount == nominal) AmberWarning.copy(alpha = 0.3f) else Slate100,
                                                    labelColor = Slate800
                                                ),
                                                border = null,
                                                modifier = Modifier.height(26.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (opsiBayarMuat == "BAYAR_LANGSUNG") {
                            Text(
                                tr("Total ${SfaViewModel.formatRupiah(totalPotensiHutang)} dibayar lunas tunai saat muat barang.", "Total ${SfaViewModel.formatRupiah(totalPotensiHutang)} paid in cash upon loading.", lang),
                                fontSize = 10.sp,
                                color = EmeraldText,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                tr("Barang dibawa konsinyasi. Tagihan dihitung pada struk closing sore hari sesuai barang terjual.", "Goods taken on consignment. Invoiced on evening closing receipt based on goods sold.", lang),
                                fontSize = 10.sp,
                                color = Slate600
                            )
                        }
                    }
                }

                // Search & Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(tr("Cari produk FMCG...", "Search FMCG products...", lang), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category chips
                if (categories.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(tr("Semua (${products.size})", "All (${products.size})", lang), fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate900,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700
                            ),
                            border = null,
                            modifier = Modifier.height(28.dp)
                        )
                        categories.forEach { cat ->
                            val count = products.count { it.kategori == cat }
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text("$cat ($count)", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Slate900,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100,
                                    labelColor = Slate700
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                // Product List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val currentQtyStr = quantities[product.id] ?: "0"
                        val currentQty = currentQtyStr.toIntOrNull() ?: 0
                        val drawer = drawers.find { it.productId == product.id }
                        val todayLoaded = todayLoadings.filter { it.productId == product.id }.sumOf { it.jumlahDus }
                        val rasio = product.rasioKonversi
                        val totalPcs = currentQty * rasio
                        val hargaPack = product.hargaBeliPabrik
                        val subtotalHutang = currentQty * hargaPack
                        val hasQty = currentQty > 0
                        val satuanBesarLabel = product.satuanBesar.ifBlank { "Pack" }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasQty) BlueSurface.copy(alpha = 0.35f) else Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (hasQty) BlueBorder else Slate200
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(product.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                            if (product.kategori.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Slate100
                                                ) {
                                                    Text(product.kategori, fontSize = 9.sp, color = Slate600, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            "1 $satuanBesarLabel = ${product.rasioKonversi} ${product.satuanKecil} • Beli: ${SfaViewModel.formatRupiah(hargaPack)}/$satuanBesarLabel",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }

                                    // Stok info
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldSurface
                                        ) {
                                            Text(
                                                "Fresh: ${drawer?.stokFreshPabrikPcs ?: 0} ${product.satuanKecil}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        if (todayLoaded > 0) {
                                            Text(tr("Dimuat tgl ini: $todayLoaded $satuanBesarLabel", "Loaded on this date: $todayLoaded $satuanBesarLabel", lang), fontSize = 9.sp, color = Slate500)
                                        }
                                    }
                                }

                                // Stepper & Input Section
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Jumlah Muat ($satuanBesarLabel):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasQty) Slate900 else Slate600
                                        )
                                        if (hasQty) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = BlueAccent.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "$currentQty $satuanBesarLabel = $totalPcs ${product.satuanKecil}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BlueAccent,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Stepper Button Row with Center Input
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // -10 button
                                        FilledTonalIconButton(
                                            onClick = {
                                                val newQty = (currentQty - 10).coerceAtLeast(0)
                                                quantities = quantities + (product.id to newQty.toString())
                                            },
                                            modifier = Modifier.size(38.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("-10", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }

                                        // -1 button
                                        FilledTonalIconButton(
                                            onClick = {
                                                val newQty = (currentQty - 1).coerceAtLeast(0)
                                                quantities = quantities + (product.id to newQty.toString())
                                            },
                                            modifier = Modifier.size(38.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("-1", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        // Center Input Surface (Guaranteed No Vertical Cutoff)
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (hasQty) Color.White else Slate100,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                if (hasQty) BlueAccent else Slate300
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = currentQtyStr,
                                                    onValueChange = { input ->
                                                        if (input.all { it.isDigit() }) {
                                                            quantities = quantities + (product.id to input)
                                                        }
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = Slate900
                                                    ),
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(BlueAccent)
                                                )
                                            }
                                        }

                                        // +1 button
                                        FilledTonalIconButton(
                                            onClick = {
                                                val newQty = currentQty + 1
                                                quantities = quantities + (product.id to newQty.toString())
                                            },
                                            modifier = Modifier.size(38.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("+1", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        // +10 button
                                        FilledTonalIconButton(
                                            onClick = {
                                                val newQty = currentQty + 10
                                                quantities = quantities + (product.id to newQty.toString())
                                            },
                                            modifier = Modifier.size(38.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("+10", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }

                                        // +50 button
                                        FilledTonalIconButton(
                                            onClick = {
                                                val newQty = currentQty + 50
                                                quantities = quantities + (product.id to newQty.toString())
                                            },
                                            modifier = Modifier.size(38.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("+50", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }
                                }

                                // Live calculation summary row per product
                                if (hasQty) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Slate100)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "= $totalPcs ${product.satuanKecil} masuk Laci Fresh",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldText
                                        )
                                        Text(
                                            "Nilai: ${SfaViewModel.formatRupiah(subtotalHutang)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(tr("Batal", "Cancel", lang), color = Slate700)
                    }

                    Button(
                        onClick = {
                            val dpTotal = if (opsiBayarMuat == "HUTANG") dpAmount else 0.0
                            val items = quantities.mapNotNull { (productId, qtyStr) ->
                                val qty = qtyStr.toIntOrNull() ?: 0
                                if (qty > 0) {
                                    val p = products.find { it.id == productId }
                                    if (p != null) {
                                        val itemHutang = qty * p.hargaBeliPabrik
                                        val itemBayar = when (opsiBayarMuat) {
                                            "BAYAR_LANGSUNG" -> itemHutang
                                            "HUTANG" -> if (totalPotensiHutang > 0) (dpTotal * (itemHutang / totalPotensiHutang)).coerceAtMost(itemHutang) else 0.0
                                            else -> 0.0 // BAYAR_CLOSING
                                        }
                                        LoadingItemInput(
                                            productId = p.id,
                                            jumlahDus = qty,
                                            rasioKonversi = p.rasioKonversi,
                                            hargaBeliDus = p.hargaBeliPabrik,
                                            opsiBayarMuat = opsiBayarMuat,
                                            jumlahBayarMuat = itemBayar,
                                            catatanMuat = catatanMuat
                                        )
                                    } else null
                                } else null
                            }
                            if (items.isNotEmpty()) {
                                onSubmitBatch(items)
                            }
                        },
                        enabled = totalPack > 0,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (totalPack > 0) "Simpan ($totalPack Pack)" else "Isi Jumlah Muat",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 1.1 DIALOG PELUNASAN / PEMBAYARAN HUTANG SUPPLIER (MUAT BARANG)
@Composable
fun BayarHutangSupplierDialog(
    loading: DailyLoadingEntity,
    products: List<ProductEntity>,
    pabriks: List<PabrikEntity>,
    onDismiss: () -> Unit,
    onConfirmPay: (Double) -> Unit
) {
    val lang = LocalAppLanguage.current

val product = remember(products, loading.productId) { products.find { it.id == loading.productId } }
    val pabrik = remember(pabriks, product?.pabrikId) { pabriks.find { it.id == product?.pabrikId } }
    val satuanBesarLabel = product?.satuanBesar ?: "Pack"
    val satuanKecilLabel = product?.satuanKecil ?: "Pcs"

    var inputAmount by remember { mutableStateOf("") }
    val bayarAmount = inputAmount.toDoubleOrNull() ?: 0.0
    val newSisa = (loading.sisaHutangMuat - bayarAmount).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Slate900),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberWarning),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = Slate900, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(tr("Bayar Hutang Supplier", "Pay Supplier Debt", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(tr("Pelunasan tagihan muat ke pabrik/supplier", "Payment of loading bill to factory/supplier", lang), fontSize = 11.sp, color = Slate500)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500)
                    }
                }

                // Info Barang & Supplier Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(product?.nama ?: "Produk Muat", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                        if (pabrik != null) {
                            Text(tr("Supplier: ${pabrik.namaPabrik}", "Supplier: ${pabrik.namaPabrik}", lang), fontSize = 11.sp, color = Slate600)
                        }
                        Text(
                            "Tanggal Muat: ${loading.tanggal} • Muat: ${loading.jumlahDus} $satuanBesarLabel (${loading.totalPcs} $satuanKecilLabel)",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                        Text(
                            "Harga Modal: ${SfaViewModel.formatRupiah(loading.hargaBeliPabrikDus)} / $satuanBesarLabel",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                // Financial Balance Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tr("Total Hutang Awal:", "Initial Total Debt:", lang), color = Slate400, fontSize = 11.sp)
                            Text(SfaViewModel.formatRupiah(loading.potensiHutangPabrik), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tr("Sudah Dibayar Sebelumnya:", "Previously Paid:", lang), color = Slate400, fontSize = 11.sp)
                            Text(SfaViewModel.formatRupiah(loading.jumlahBayarMuat), color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("SISA HUTANG SAAT INI:", "CURRENT REMAINING DEBT:", lang), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(SfaViewModel.formatRupiah(loading.sisaHutangMuat), color = AmberWarning, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Nominal Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("Nominal Pembayaran Sekarang (Rp):", "Payment Amount Now (Rp):", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) inputAmount = input
                        },
                        placeholder = { Text(tr("Contoh: 100000", "Example: 100000", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = { inputAmount = loading.sisaHutangMuat.toLong().toString() },
                            label = { Text(tr("Lunas (${SfaViewModel.formatRupiah(loading.sisaHutangMuat)})", "Paid Off (${SfaViewModel.formatRupiah(loading.sisaHutangMuat)})", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = EmeraldSurface, labelColor = EmeraldText),
                            border = null,
                            modifier = Modifier.height(28.dp)
                        )
                        if (loading.sisaHutangMuat > 50000) {
                            SuggestionChip(
                                onClick = { inputAmount = "50000" },
                                label = { Text("Rp 50.000", fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Slate100, labelColor = Slate700),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                        if (loading.sisaHutangMuat > 100000) {
                            SuggestionChip(
                                onClick = { inputAmount = "100000" },
                                label = { Text("Rp 100.000", fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Slate100, labelColor = Slate700),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                        val half = (loading.sisaHutangMuat / 2).toLong()
                        if (half > 0) {
                            SuggestionChip(
                                onClick = { inputAmount = half.toString() },
                                label = { Text("50% (${SfaViewModel.formatRupiah(half.toDouble())})", fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Slate100, labelColor = Slate700),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                // Sisa Hutang Proyeksi
                if (bayarAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (newSisa <= 0) EmeraldSurface else Slate100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("Sisa Hutang Setelah Bayar:", "Remaining Debt After Payment:", lang), fontSize = 11.sp, color = Slate700)
                            Text(
                                if (newSisa <= 0) "LUNAS (Rp 0)" else SfaViewModel.formatRupiah(newSisa),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (newSisa <= 0) EmeraldText else RoseDanger
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Text(tr("Batal", "Cancel", lang))
                    }
                    Button(
                        onClick = {
                            if (bayarAmount > 0) {
                                onConfirmPay(bayarAmount)
                            }
                        },
                        enabled = bayarAmount > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(tr("Simpan Pembayaran", "Save Payment", lang))
                    }
                }
            }
        }
    }
}

// 2. TITIP BARU DIALOG
@Composable
fun TitipBaruDialog(
    warung: WarungEntity,
    products: List<ProductEntity>,
    drawers: List<InventoryDrawerEntity>,
    dailyLoadings: List<DailyLoadingEntity> = emptyList(),
    customPrices: List<WarungCustomPriceEntity> = emptyList(),
    onDismiss: () -> Unit,
    onNavigateToMuatPagi: () -> Unit = {
},
    onSubmit: (productId: String, sumberStok: String, jumlahPcs: Int, hargaSatuan: Double, gpsLat: Double, gpsLng: Double, gpsAddr: String, catatan: String) -> Unit
) {
    val lang = LocalAppLanguage.current

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val loadedProductIdsToday = remember(dailyLoadings, drawers, today) {
        val loadedTodayIds = dailyLoadings.filter { it.tanggal == today }.map { it.productId }.toSet()
        val withStockIds = drawers.filter { it.stokFreshPabrikPcs > 0 || it.stokPribadiLayakJualPcs > 0 }.map { it.productId }.toSet()
        loadedTodayIds + withStockIds
    }

    var showAllCatalog by remember { mutableStateOf(false) }

    val availableProducts = remember(products, loadedProductIdsToday, showAllCatalog) {
        if (showAllCatalog) {
            products
        } else {
            products.filter { it.id in loadedProductIdsToday }
        }
    }

    var selectedProductIndex by remember { mutableStateOf(0) }
    val product = availableProducts.getOrNull(selectedProductIndex.coerceIn(0, (availableProducts.size - 1).coerceAtLeast(0)))
    val drawer = drawers.find { it.productId == product?.id }

    val satuanBesar = product?.satuanBesar ?: "Pack"
    val satuanKecil = product?.satuanKecil ?: "Pcs"
    val rasioKonversi = (product?.rasioKonversi ?: 10).coerceAtLeast(1)

    val customPriceObj = customPrices.find { it.warungId == warung.id && it.productId == product?.id }
    val defaultHarga = customPriceObj?.hargaJualPcs ?: product?.hargaJualDefault ?: 1600.0

    var jumlahBesarInput by remember { mutableStateOf("2") }
    var jumlahLepasanPcsInput by remember { mutableStateOf("0") }
    var hargaSatuanInput by remember(product?.id, customPriceObj?.hargaJualPcs) { mutableStateOf("${defaultHarga.toLong()}") }
    var sumberStok by remember { mutableStateOf("FRESH_PABRIK") }
    var catatan by remember { mutableStateOf("") }
    var gpsLocked by remember { mutableStateOf(true) }

    val jumlahBesar = jumlahBesarInput.toIntOrNull() ?: 0
    val jumlahLepasanPcs = jumlahLepasanPcsInput.toIntOrNull() ?: 0
    val jumlahPcs = (jumlahBesar * rasioKonversi) + jumlahLepasanPcs
    val hargaSatuan = hargaSatuanInput.toDoubleOrNull() ?: defaultHarga
    val totalNilaiTitipan = jumlahPcs * hargaSatuan

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Titip Baru / Tambah Stok",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Toko: ${warung.namaWarung} (Titipan saat ini: ${warung.stokTitipanPcs} ${product?.satuanKecil ?: "Pcs"})",
                    fontSize = 12.sp,
                    color = Slate500
                )

                // Warning if no products loaded yet today
                if (loadedProductIdsToday.isEmpty() && !showAllCatalog) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                                Text(tr("Belum Ada Barang Dimuat di Mobil", "No Goods Loaded in Vehicle Yet", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                            }
                            Text(
                                tr("Stok mobil masih kosong / belum ada barang yang dimuat hari ini. Muat barang terlebih dahulu agar stok Fresh tersedia.", "Vehicle stock is empty / no goods loaded today. Load goods first so Fresh stock is available.", lang),
                                fontSize = 11.sp,
                                color = Slate700
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onNavigateToMuatPagi()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr("Muat Barang Pagi", "Morning Load", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                TextButton(onClick = { showAllCatalog = true }) {
                                    Text(tr("Lihat Katalog", "View Catalog", lang), fontSize = 11.sp, color = Slate600)
                                }
                            }
                        }
                    }
                }

                // Filter status & Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!showAllCatalog && loadedProductIdsToday.isNotEmpty()) "Pilih Produk (Tersedia di Mobil - ${availableProducts.size} SKU):" else "Pilih Produk (Semua Katalog - ${availableProducts.size} SKU):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                    if (loadedProductIdsToday.isNotEmpty()) {
                        Text(
                            text = if (showAllCatalog) "Hanya Muatan" else "Semua Katalog",
                            fontSize = 10.sp,
                            color = BlueAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showAllCatalog = !showAllCatalog }
                                .padding(4.dp)
                        )
                    }
                }

                // Select Product
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    availableProducts.forEachIndexed { index, p ->
                        val pCustom = customPrices.find { it.warungId == warung.id && it.productId == p.id }
                        val effectivePrice = pCustom?.hargaJualPcs ?: p.hargaJualDefault
                        val pDrawer = drawers.find { it.productId == p.id }
                        val isSelected = selectedProductIndex == index

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedProductIndex = index },
                            color = if (isSelected) Slate900 else Slate100,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Slate900) else androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = p.nama,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Slate900
                                    )
                                    Text(
                                        text = "${SfaViewModel.formatRupiah(effectivePrice)}/${p.satuanKecil} • Stok Mobil: Fresh ${pDrawer?.stokFreshPabrikPcs ?: 0} | Pribadi ${pDrawer?.stokPribadiLayakJualPcs ?: 0}",
                                        fontSize = 10.sp,
                                        color = if (isSelected) Slate300 else Slate500
                                    )
                                }
                                if (pCustom != null) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) EmeraldSuccess else EmeraldSurface
                                    ) {
                                        Text(
                                            tr("Harga Khusus", "Special Price", lang),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else EmeraldText,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Harga Satuan Field (Editable / Custom) + Price Suggestions
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = hargaSatuanInput,
                        onValueChange = { hargaSatuanInput = it },
                        label = {
                            Text(if (customPriceObj != null) "Harga Jual (Harga Khusus Toko)" else "Harga Jual (Rp/${product?.satuanKecil ?: "Pcs"})")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick suggestion chips based on default base price
                    val basePrice = product?.hargaJualDefault ?: 1600.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.95 to "-5%", 1.0 to "Normal", 1.05 to "+5%", 1.10 to "+10%", 1.15 to "+15%", 1.20 to "+20%").forEach { (multiplier, label) ->
                            val calcPrice = ((basePrice * multiplier + 49) / 50).toLong() * 50
                            SuggestionChip(
                                onClick = { hargaSatuanInput = calcPrice.toString() },
                                label = { Text("$label (Rp$calcPrice)", fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (hargaSatuanInput == calcPrice.toString()) EmeraldSuccess else Slate100,
                                    labelColor = if (hargaSatuanInput == calcPrice.toString()) Color.White else Slate700
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                // Sumber Stok Toggle
                Text(tr("Pilih Sumber Stok di Mobil:", "Select Vehicle Stock Source:", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = sumberStok == "FRESH_PABRIK",
                        onClick = { sumberStok = "FRESH_PABRIK" },
                        label = {
                            Text("Fresh Pabrik (${drawer?.stokFreshPabrikPcs ?: 0} ${product?.satuanKecil ?: "Pcs"})", fontSize = 11.sp)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = sumberStok == "PRIBADI_REPACK",
                        onClick = { sumberStok = "PRIBADI_REPACK" },
                        label = {
                            Text("Pribadi Repack (${drawer?.stokPribadiLayakJualPcs ?: 0} ${product?.satuanKecil ?: "Pcs"})", fontSize = 11.sp)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Input Jumlah Dititipkan dalam Satuan Besar (Pack / Dus) - BORDER BIRU ELEKTRIK
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueSurface.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BlueAccent)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("Titip Baru / Drop Awal:", "New Consignment / Initial Drop:", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BlueBorder)
                            ) {
                                Text(
                                    text = "Total: $jumlahPcs $satuanKecil (1 $satuanBesar = $rasioKonversi $satuanKecil)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = jumlahBesarInput,
                                onValueChange = { jumlahBesarInput = it },
                                label = { Text(tr("Jumlah ($satuanBesar)", "Quantity ($satuanBesar)", lang)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = {
                                    IconButton(
                                        onClick = {
                                            val curr = jumlahBesarInput.toIntOrNull() ?: 0
                                            if (curr > 1) jumlahBesarInput = (curr - 1).toString()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = tr("Kurang 1", "Subtract 1", lang), tint = Slate700)
                                    }
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val curr = jumlahBesarInput.toIntOrNull() ?: 0
                                            jumlahBesarInput = (curr + 1).toString()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = tr("Tambah 1", "Add 1", lang), tint = Slate700)
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = jumlahLepasanPcsInput,
                                onValueChange = { jumlahLepasanPcsInput = it },
                                label = { Text(tr("+ Eceran ($satuanKecil)", "+ Retail ($satuanKecil)", lang)) },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Ergonomic Fast Stepper Buttons (-5, -1, +1, +5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(-5 to "-5", -1 to "-1", 1 to "+1", 5 to "+5").forEach { (delta, label) ->
                                OutlinedButton(
                                    onClick = {
                                        val curr = jumlahBesarInput.toIntOrNull() ?: 0
                                        jumlahBesarInput = (curr + delta).coerceAtLeast(1).toString()
                                    },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (delta > 0) BlueSurface else Slate100,
                                        contentColor = if (delta > 0) BlueAccent else Slate800
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (delta > 0) BlueBorder else Slate200)
                                ) {
                                    Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Pintasan Cepat 1-10 Pack/Dus
                        Text(tr("Pintasan Cepat ($satuanBesar):", "Quick Shortcuts ($satuanBesar):", lang), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate600)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (1..10).forEach { qty ->
                                val isSelected = jumlahBesarInput == qty.toString() && (jumlahLepasanPcsInput.isEmpty() || jumlahLepasanPcsInput == "0")
                                SuggestionChip(
                                    onClick = {
                                        jumlahBesarInput = qty.toString()
                                        jumlahLepasanPcsInput = "0"
                                    },
                                    label = {
                                        Text(
                                            text = "$qty $satuanBesar (${qty * rasioKonversi} $satuanKecil)",
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) BlueAccent else Color.White,
                                        labelColor = if (isSelected) Color.White else Slate800
                                    ),
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BlueBorder),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        // Estimasi Nilai Modal Tertanam
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tr("Estimasi Modal Tertanam:", "Estimated Capital Invested:", lang), fontSize = 11.sp, color = Slate600)
                                Text(
                                    text = "${SfaViewModel.formatRupiah(totalNilaiTitipan)} ($jumlahPcs $satuanKecil)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }
                        }
                    }
                }

                // GPS Check-in
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Slate100)) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Column {
                            Text("GPS Check-in: ${String.format(Locale.US, "%.4f", warung.latitude)}, ${String.format(Locale.US, "%.4f", warung.longitude)} (Akurasi: ${warung.akurasiGpsMeter}m)", fontSize = 10.sp, color = Slate600)
                            Text(tr("Timestamp Otomatis Terverifikasi", "Automatic Verified Timestamp", lang), fontSize = 10.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(tr("Batal", "Cancel", lang), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (product != null && jumlahPcs > 0) {
                                onSubmit(
                                    product.id,
                                    sumberStok,
                                    jumlahPcs,
                                    hargaSatuan,
                                    warung.latitude,
                                    warung.longitude,
                                    warung.alamatLengkap,
                                    catatan
                                )
                            }
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Simpan Titipan", "Save Consignment", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 3. TARIK SISA & GANTI BARANG DIALOG (MASTER PLAN FLOW 4.2 SKENARIO B)
@Composable
fun TarikSisaDialog(
    warung: WarungEntity,
    products: List<ProductEntity>,
    drawers: List<InventoryDrawerEntity>,
    dailyLoadings: List<DailyLoadingEntity> = emptyList(),
    transactions: List<TransactionEntity> = emptyList(),
    customPrices: List<WarungCustomPriceEntity> = emptyList(),
    onDismiss: () -> Unit,
    onNavigateToMuatPagi: () -> Unit = {
},
    onSubmit: (productId: String, sisaLalu: Int, sisaFisik: Int, harga: Double, bayar: Double, restock: Int, sumberRestock: String, lat: Double, lng: Double, addr: String, note: String) -> Unit
) {
    val lang = LocalAppLanguage.current

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val loadedProductIdsToday = remember(dailyLoadings, drawers, today) {
        val loadedTodayIds = dailyLoadings.filter { it.tanggal == today }.map { it.productId }.toSet()
        val withStockIds = drawers.filter { it.stokFreshPabrikPcs > 0 || it.stokPribadiLayakJualPcs > 0 }.map { it.productId }.toSet()
        loadedTodayIds + withStockIds
    }

    var showAllCatalog by remember { mutableStateOf(false) }

    val availableProducts = remember(products, loadedProductIdsToday, showAllCatalog) {
        if (showAllCatalog) {
            products
        } else {
            products.filter { it.id in loadedProductIdsToday }
        }
    }

    var selectedProductIndex by remember { mutableStateOf(0) }
    val product = availableProducts.getOrNull(selectedProductIndex.coerceIn(0, (availableProducts.size - 1).coerceAtLeast(0)))
    val drawer = drawers.find { it.productId == product?.id }

    val satuanBesar = product?.satuanBesar ?: "Pack"
    val satuanKecil = product?.satuanKecil ?: "Pcs"
    val rasioKonversi = (product?.rasioKonversi ?: 10).coerceAtLeast(1)

    val customPriceObj = customPrices.find { it.warungId == warung.id && it.productId == product?.id }
    val defaultHarga = customPriceObj?.hargaJualPcs ?: product?.hargaJualDefault ?: 1600.0

    // Deteksi titipan lalu spesifik untuk produk ini
    val lastTxForThisProduct = remember(product?.id, transactions, warung.id) {
        transactions.filter { it.warungId == warung.id && it.productId == product?.id }.maxByOrNull { it.timestamp }
    }
    val defaultTitipanLalu = lastTxForThisProduct?.totalTitipanAktifPcs ?: (if (warung.stokTitipanPcs > 0) warung.stokTitipanPcs else 20)
    var sisaTitipanLaluInput by remember(product?.id, defaultTitipanLalu) { mutableStateOf("$defaultTitipanLalu") }
    val sisaTitipanLalu = sisaTitipanLaluInput.toIntOrNull() ?: defaultTitipanLalu

    var sisaFisikInput by remember { mutableStateOf("0") }
    var hargaSatuanInput by remember(product?.id, customPriceObj?.hargaJualPcs) { mutableStateOf("${defaultHarga.toLong()}") }

    val defaultRestockBesar = if (sisaTitipanLalu > 0 && rasioKonversi > 0) "${(sisaTitipanLalu / rasioKonversi).coerceAtLeast(1)}" else "2"
    val defaultRestockLepasan = if (sisaTitipanLalu > 0 && rasioKonversi > 0) "${sisaTitipanLalu % rasioKonversi}" else "0"

    var restockBesarInput by remember(sisaTitipanLalu, rasioKonversi) { mutableStateOf(defaultRestockBesar) }
    var restockLepasanPcsInput by remember(sisaTitipanLalu, rasioKonversi) { mutableStateOf(defaultRestockLepasan) }
    var sumberRestock by remember { mutableStateOf("FRESH_PABRIK") }
    var catatanTransaksi by remember { mutableStateOf(warung.notes) }

    val sisaFisik = sisaFisikInput.toIntOrNull() ?: 0
    val pcsLaku = (sisaTitipanLalu - sisaFisik).coerceAtLeast(0)
    val hargaSatuan = hargaSatuanInput.toDoubleOrNull() ?: defaultHarga
    val subtotalLaku = pcsLaku * hargaSatuan
    val saldoPiutangLama = warung.saldoPiutang
    val grandTotal = subtotalLaku + saldoPiutangLama

    var bayarInput by remember(subtotalLaku, saldoPiutangLama) { mutableStateOf("${(subtotalLaku + saldoPiutangLama).toLong()}") }
    val uangDiterima = bayarInput.toDoubleOrNull() ?: 0.0
    val sisaPiutangBaru = (grandTotal - uangDiterima).coerceAtLeast(0.0)

    val restockBesar = restockBesarInput.toIntOrNull() ?: 0
    val restockLepasanPcs = restockLepasanPcsInput.toIntOrNull() ?: 0
    val restockPcs = (restockBesar * rasioKonversi) + restockLepasanPcs

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ganti Barang / Tarik Sisa (Siklus 2+)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Toko: ${warung.namaWarung} • Titipan Lalu: $sisaTitipanLalu ${product?.satuanKecil ?: "Pcs"}",
                    fontSize = 11.sp,
                    color = Slate500
                )

                // Warning if no products loaded yet today
                if (loadedProductIdsToday.isEmpty() && !showAllCatalog) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                                Text(tr("Belum Ada Barang Dimuat di Mobil", "No Goods Loaded in Vehicle Yet", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                            }
                            Text(
                                tr("Stok mobil masih kosong / belum ada barang yang dimuat hari ini. Muat barang terlebih dahulu agar stok Fresh/Pribadi tersedia.", "Vehicle stock is empty / no goods loaded today. Load goods first so Fresh/Personal stock is available.", lang),
                                fontSize = 11.sp,
                                color = Slate700
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onNavigateToMuatPagi()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr("Muat Barang Pagi", "Morning Load", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                TextButton(onClick = { showAllCatalog = true }) {
                                    Text(tr("Buka Semua Katalog", "Open Full Catalog", lang), fontSize = 11.sp, color = Slate600)
                                }
                            }
                        }
                    }
                }

                // Select Product if multiple
                if (availableProducts.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (!showAllCatalog && loadedProductIdsToday.isNotEmpty()) "Pilih Produk (Dimuat/Ada Stok - ${availableProducts.size} SKU):" else "Pilih Produk (${availableProducts.size} SKU):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600
                        )
                        if (loadedProductIdsToday.isNotEmpty()) {
                            Text(
                                text = if (showAllCatalog) "Hanya Muatan" else "Semua Katalog",
                                fontSize = 10.sp,
                                color = BlueAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showAllCatalog = !showAllCatalog }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableProducts.forEachIndexed { index, p ->
                            val pCustom = customPrices.find { it.warungId == warung.id && it.productId == p.id }
                            val effectivePrice = pCustom?.hargaJualPcs ?: p.hargaJualDefault
                            val pDrawer = drawers.find { it.productId == p.id }
                            val isSelected = selectedProductIndex == index

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedProductIndex = index },
                                color = if (isSelected) Slate900 else Slate100,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Slate900) else androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = p.nama,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Slate900
                                        )
                                        Text(
                                            text = "${SfaViewModel.formatRupiah(effectivePrice)}/${p.satuanKecil} • Fresh: ${pDrawer?.stokFreshPabrikPcs ?: 0} Pcs",
                                            fontSize = 9.sp,
                                            color = if (isSelected) Slate300 else Slate500
                                        )
                                    }
                                    if (pCustom != null) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) EmeraldSuccess else EmeraldSurface
                                        ) {
                                            Text(
                                                tr("Harga Khusus", "Special Price", lang),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else EmeraldText,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 1: Stock Opname Fisik (Sisa di Warung) & Harga - BORDER ORANYE / AMBER
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberSurface.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberWarning)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("STEP 1: Sisa Barang Fisik di Warung", "STEP 1: Physical Stock Left in Store", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberWarning)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberBorder)
                            ) {
                                Text(
                                    text = "Sisa Lalu: $sisaTitipanLalu $satuanKecil",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = sisaFisikInput,
                            onValueChange = { sisaFisikInput = it },
                            label = { Text(tr("Sisa Barang Fisik di Warung ($satuanKecil)", "Physical Stock Left in Store ($satuanKecil)", lang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            leadingIcon = {
                                IconButton(
                                    onClick = {
                                        val curr = sisaFisikInput.toIntOrNull() ?: 0
                                        if (curr > 0) sisaFisikInput = (curr - 1).toString()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = tr("Kurang 1", "Subtract 1", lang), tint = Slate700)
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val curr = sisaFisikInput.toIntOrNull() ?: 0
                                        sisaFisikInput = (curr + 1).toString()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = tr("Tambah 1", "Add 1", lang), tint = Slate700)
                                }
                            }
                        )

                        // Quick Pcs Shortcut for Sisa Fisik Warung (Eceran)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0 to "0 (Habis)",
                                1 to "1 $satuanKecil",
                                2 to "2 $satuanKecil",
                                3 to "3 $satuanKecil",
                                5 to "5 $satuanKecil",
                                10 to "10 $satuanKecil"
                            ).forEach { (pcsVal, label) ->
                                val isSelected = sisaFisikInput == pcsVal.toString()
                                SuggestionChip(
                                    onClick = { sisaFisikInput = pcsVal.toString() },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) AmberWarning else Color.White,
                                        labelColor = if (isSelected) Color.White else Slate800
                                    ),
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, AmberBorder),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                            if (sisaTitipanLalu > 0) {
                                val isUtuhSelected = sisaFisikInput == sisaTitipanLalu.toString()
                                SuggestionChip(
                                    onClick = { sisaFisikInput = sisaTitipanLalu.toString() },
                                    label = { Text(tr("Utuh ($sisaTitipanLalu $satuanKecil)", "Intact ($sisaTitipanLalu $satuanKecil)", lang), fontSize = 10.sp, fontWeight = if (isUtuhSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isUtuhSelected) AmberWarning else Color.White,
                                        labelColor = if (isUtuhSelected) Color.White else Slate800
                                    ),
                                    border = if (isUtuhSelected) null else androidx.compose.foundation.BorderStroke(1.dp, AmberBorder),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = hargaSatuanInput,
                            onValueChange = { hargaSatuanInput = it },
                            label = { Text(if (customPriceObj != null) "Harga Jual (Harga Khusus Toko)" else "Harga Jual (Rp/$satuanKecil)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Quick Price Suggestions
                        val basePrice = product?.hargaJualDefault ?: 1600.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0.95 to "-5%", 1.0 to "Normal", 1.05 to "+5%", 1.10 to "+10%", 1.15 to "+15%", 1.20 to "+20%").forEach { (multiplier, label) ->
                                val calcPrice = ((basePrice * multiplier + 49) / 50).toLong() * 50
                                SuggestionChip(
                                    onClick = { hargaSatuanInput = calcPrice.toString() },
                                    label = { Text("$label (Rp$calcPrice)", fontSize = 9.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (hargaSatuanInput == calcPrice.toString()) EmeraldSuccess else Color.White,
                                        labelColor = if (hargaSatuanInput == calcPrice.toString()) Color.White else Slate800
                                    ),
                                    border = if (hargaSatuanInput == calcPrice.toString()) null else androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }
                }

                // STEP 2 & 3: Kalkulasi & Tagihan
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Slate900)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Pcs Laku Terjual ($sisaTitipanLalu - $sisaFisik):", "Pcs Sold ($sisaTitipanLalu - $sisaFisik):", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text("$pcsLaku $satuanKecil", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Subtotal Penjualan ($pcsLaku × ${SfaViewModel.formatRupiah(hargaSatuan)}):", "Sales Subtotal ($pcsLaku × ${SfaViewModel.formatRupiah(hargaSatuan)}):", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(SfaViewModel.formatRupiah(subtotalLaku), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Saldo Piutang / Bon Lama:", "Previous Credit / Outstanding Balance:", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(SfaViewModel.formatRupiah(saldoPiutangLama), color = AmberWarning, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("TOTAL TAGIHAN KAS:", "TOTAL CASH DUE:", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(SfaViewModel.formatRupiah(grandTotal), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // STEP 4: Input Pembayaran
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr("Uang Diterima dari Toko (Rp):", "Money Received from Store (Rp):", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
                        if (uangDiterima > 0 && uangDiterima < grandTotal) {
                            Text(
                                text = "Kurang: ${SfaViewModel.formatRupiah(grandTotal - uangDiterima)}",
                                color = AmberWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bayarInput,
                        onValueChange = { bayarInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (bayarInput.isNotBlank()) {
                                IconButton(onClick = { bayarInput = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    // Quick payment presets & Denominations
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = uangDiterima == grandTotal && grandTotal > 0,
                            onClick = { bayarInput = "${grandTotal.toLong()}" },
                            label = { Text(tr("💰 Lunas Full (${SfaViewModel.formatRupiah(grandTotal)})", "💰 Full Payment (${SfaViewModel.formatRupiah(grandTotal)})", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldSuccess, selectedLabelColor = Color.White)
                        )
                        if (subtotalLaku != grandTotal && subtotalLaku > 0) {
                            FilterChip(
                                selected = uangDiterima == subtotalLaku,
                                onClick = { bayarInput = "${subtotalLaku.toLong()}" },
                                label = { Text(tr("Bayar Laku (${SfaViewModel.formatRupiah(subtotalLaku)})", "Pay Sold Only (${SfaViewModel.formatRupiah(subtotalLaku)})", lang), fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        FilterChip(
                            selected = uangDiterima == 0.0,
                            onClick = { bayarInput = "0" },
                            label = { Text(tr("🔴 Bon Full (Rp 0)", "🔴 Full Credit (Rp 0)", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoseDanger, selectedLabelColor = Color.White)
                        )
                        listOf(20000L, 50000L, 100000L, 200000L, 500000L).forEach { denom ->
                            SuggestionChip(
                                onClick = {
                                    val curr = bayarInput.toDoubleOrNull() ?: 0.0
                                    bayarInput = (curr + denom).toLong().toString()
                                },
                                label = { Text("+${denom / 1000}rb", fontSize = 10.sp) },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    if (sisaPiutangBaru > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tr("Sisa Saldo Bon Setelah Kunjungan Ini:", "Remaining Credit Balance After Visit:", lang), fontSize = 10.sp, color = Slate700)
                                Text(
                                    text = SfaViewModel.formatRupiah(sisaPiutangBaru),
                                    color = AmberWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // STEP 5: Tarik Retur & Restock Baru (Isi Ulang) - BORDER BIRU ELEKTRIK
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueSurface.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BlueAccent)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("Retur Ditarik: $sisaFisik $satuanKecil → Masuk Laci 'Retur Belum Sortir'", "Returned Items: $sisaFisik $satuanKecil → Transferred to 'Unsorted Return' Drawer", lang), fontSize = 11.sp, color = AmberWarning, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("Isi Ulang / Restock Baru:", "Refill / New Restock:", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueAccent)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BlueBorder)
                            ) {
                                Text(
                                    text = "Total: $restockPcs $satuanKecil (1 $satuanBesar = $rasioKonversi $satuanKecil)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = restockBesarInput,
                                onValueChange = { restockBesarInput = it },
                                label = { Text(tr("Restock ($satuanBesar)", "Restock ($satuanBesar)", lang)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = {
                                    IconButton(
                                        onClick = {
                                            val curr = restockBesarInput.toIntOrNull() ?: 0
                                            if (curr > 1) restockBesarInput = (curr - 1).toString()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = tr("Kurang 1", "Subtract 1", lang), tint = Slate700)
                                    }
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val curr = restockBesarInput.toIntOrNull() ?: 0
                                            restockBesarInput = (curr + 1).toString()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = tr("Tambah 1", "Add 1", lang), tint = Slate700)
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = restockLepasanPcsInput,
                                onValueChange = { restockLepasanPcsInput = it },
                                label = { Text(tr("+ Eceran ($satuanKecil)", "+ Retail ($satuanKecil)", lang)) },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Ergonomic Fast Stepper Buttons for Restock (-5, -1, +1, +5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(-5 to "-5", -1 to "-1", 1 to "+1", 5 to "+5").forEach { (delta, label) ->
                                OutlinedButton(
                                    onClick = {
                                        val curr = restockBesarInput.toIntOrNull() ?: 0
                                        restockBesarInput = (curr + delta).coerceAtLeast(0).toString()
                                    },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (delta > 0) BlueSurface else Slate100,
                                        contentColor = if (delta > 0) BlueAccent else Slate800
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (delta > 0) BlueBorder else Slate200)
                                ) {
                                    Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Pintasan Cepat 1-10 Pack/Dus untuk Restock
                        Text(tr("Pintasan Cepat ($satuanBesar):", "Quick Shortcuts ($satuanBesar):", lang), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate600)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (sisaTitipanLalu > 0) {
                                val laluBesar = sisaTitipanLalu / rasioKonversi
                                val laluPcs = sisaTitipanLalu % rasioKonversi
                                val isLaluSelected = restockBesarInput == laluBesar.toString() && restockLepasanPcsInput == laluPcs.toString()
                                SuggestionChip(
                                    onClick = {
                                        restockBesarInput = laluBesar.toString()
                                        restockLepasanPcsInput = laluPcs.toString()
                                    },
                                    label = {
                                        Text(
                                            text = "Sama Spt Lalu ($sisaTitipanLalu $satuanKecil)",
                                            fontSize = 9.sp,
                                            fontWeight = if (isLaluSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isLaluSelected) EmeraldSuccess else Color.White,
                                        labelColor = if (isLaluSelected) Color.White else Slate800
                                    ),
                                    border = if (isLaluSelected) null else androidx.compose.foundation.BorderStroke(1.dp, EmeraldBorder),
                                    modifier = Modifier.height(26.dp)
                                )
                            }

                            (1..10).forEach { qty ->
                                val isSelected = restockBesarInput == qty.toString() && (restockLepasanPcsInput.isEmpty() || restockLepasanPcsInput == "0")
                                SuggestionChip(
                                    onClick = {
                                        restockBesarInput = qty.toString()
                                        restockLepasanPcsInput = "0"
                                    },
                                    label = {
                                        Text(
                                            text = "$qty $satuanBesar (${qty * rasioKonversi} $satuanKecil)",
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) BlueAccent else Color.White,
                                        labelColor = if (isSelected) Color.White else Slate800
                                    ),
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BlueBorder),
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = sumberRestock == "FRESH_PABRIK",
                                onClick = { sumberRestock = "FRESH_PABRIK" },
                                label = { Text("Fresh Pabrik (${drawer?.stokFreshPabrikPcs ?: 0} $satuanKecil)", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = sumberRestock == "PRIBADI_REPACK",
                                onClick = { sumberRestock = "PRIBADI_REPACK" },
                                label = { Text("Pribadi Repack (${drawer?.stokPribadiLayakJualPcs ?: 0} $satuanKecil)", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(tr("Batal", "Cancel", lang), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (product != null) {
                                onSubmit(
                                    product.id,
                                    sisaTitipanLalu,
                                    sisaFisik,
                                    hargaSatuan,
                                    uangDiterima,
                                    restockPcs,
                                    sumberRestock,
                                    warung.latitude,
                                    warung.longitude,
                                    warung.alamatLengkap,
                                    catatanTransaksi
                                )
                            }
                        },
                        modifier = Modifier.weight(1.6f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Selesai & Struk", "Finish & Receipt", lang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// 4. SORTIR BS DIALOG (MANAJEMEN ASET PRIBADI)
@Composable
fun SortirBsDialog(
    products: List<ProductEntity>,
    drawers: List<InventoryDrawerEntity>,
    onDismiss: () -> Unit,
    onSubmit: (productId: String, totalAwal: Int, layak: Int, rusak: Int, hb: Double, hj: Double, note: String) -> Unit
) {
    val lang = LocalAppLanguage.current

var selectedProductIndex by remember { mutableStateOf(0) }
    val product = products.getOrNull(selectedProductIndex) ?: products.firstOrNull()
    val drawer = drawers.find { it.productId == product?.id }
    val bsTersedia = drawer?.stokBsBelumSortirPcs ?: 0

    var layakJualInput by remember { mutableStateOf(if (bsTersedia > 0) "${(bsTersedia * 0.8).toInt()}" else "0") }
    var rusakInput by remember { mutableStateOf(if (bsTersedia > 0) "${bsTersedia - (bsTersedia * 0.8).toInt()}" else "0") }

    val layak = layakJualInput.toIntOrNull() ?: 0
    val rusak = rusakInput.toIntOrNull() ?: 0
    val hargaBeliPcs = if (product != null && product.rasioKonversi > 0) product.hargaBeliPabrik / product.rasioKonversi else 1100.0
    val hargaJualPcs = product?.hargaJualDefault ?: 1600.0

    val modalTertanam = layak * hargaBeliPcs
    val nilaiJual = layak * hargaJualPcs
    val profitBersih = (nilaiJual - modalTertanam).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Autorenew, contentDescription = null, tint = AmberWarning)
                    Text(tr("Sortir Barang Retur (Repack Mandiri)", "Sort Returned Goods (Repack Assets)", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = AmberSurface)) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr("Total Retur Belum Disortir:", "Total Unsorted Return Goods:", lang), fontSize = 11.sp, color = AmberText)
                        Text("$bsTersedia Pcs", fontWeight = FontWeight.Bold, color = AmberText)
                    }
                }

                OutlinedTextField(
                    value = layakJualInput,
                    onValueChange = { layakJualInput = it },
                    label = { Text(tr("Retur Bagus / Repack Siap Jual (Pcs)", "Good Returns / Repack Ready to Sell (Pcs)", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = rusakInput,
                    onValueChange = { rusakInput = it },
                    label = { Text(tr("Rusak / Dibuang / Afkir (Kerugian Pcs)", "Damaged / Discarded / Waste (Loss Pcs)", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Profit Analysis Card
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Slate900)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tr("Simulasi Profit Aset Pribadi:", "Personal Asset Profit Simulation:", lang), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Modal Tertanam ($layak Pcs):", "Invested Capital ($layak Pcs):", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(SfaViewModel.formatRupiah(modalTertanam), color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Estimasi Nilai Jual:", "Estimated Selling Value:", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(SfaViewModel.formatRupiah(nilaiJual), color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Laba Bersih Salesman:", "Salesman Net Profit:", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(SfaViewModel.formatRupiah(profitBersih), color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(tr("Batal", "Cancel", lang))
                    }
                    Button(
                        onClick = {
                            if (product != null) {
                                onSubmit(product.id, bsTersedia, layak, rusak, hargaBeliPcs, hargaJualPcs, "Sortir Mandiri")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Text(tr("Simpan Sortir", "Save Sorting", lang))
                    }
                }
            }
        }
    }
}

// 5. CLOSING SORE & SETORAN SUPPLIER / PRINCIPAL DIALOG (MULTI-LOADING & MULTI-SUPPLIER)
@Composable
fun ClosingSoreDialog(
    loadings: List<DailyLoadingEntity>,
    products: List<ProductEntity>,
    pabriks: List<PabrikEntity> = emptyList(),
    drawers: List<InventoryDrawerEntity> = emptyList(),
    transactions: List<TransactionEntity>,
    bsSortirs: List<BsSortirEntity>,
    onDismiss: () -> Unit,
    onSubmitBatch: (items: List<com.example.data.repository.ProductClosingInput>, summary: ClosingSummaryData) -> Unit
) {
    val lang = LocalAppLanguage.current

val today = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }
    val todayLoadings = remember(loadings, today) { loadings.filter { it.tanggal == today } }

    // Dapatkan daftar produk yang dimuat hari ini atau ada stok fisik
    val loadedProductIds = remember(todayLoadings, products, drawers) {
        val fromLoadings = todayLoadings.map { it.productId }.distinct()
        if (fromLoadings.isNotEmpty()) {
            fromLoadings
        } else {
            val fromDrawers = products.filter { p ->
                (drawers.find { it.productId == p.id }?.stokFreshPabrikPcs ?: 0) > 0
            }.map { it.id }
            if (fromDrawers.isNotEmpty()) fromDrawers else products.map { it.id }
        }
    }

    val sisaDusInputs = remember { mutableStateMapOf<String, String>() }
    val sisaPcsInputs = remember { mutableStateMapOf<String, String>() }

    // Inisialisasi input state untuk masing-masing produk
    LaunchedEffect(loadedProductIds) {
        loadedProductIds.forEach { pId ->
            if (!sisaDusInputs.containsKey(pId)) {
                val existingClosingDus = todayLoadings.filter { it.productId == pId }.lastOrNull()?.sisaDusSore ?: 0
                sisaDusInputs[pId] = if (existingClosingDus > 0) existingClosingDus.toString() else "0"
            }
            if (!sisaPcsInputs.containsKey(pId)) {
                sisaPcsInputs[pId] = "0"
            }
        }
    }

    // Kalkulasi per produk
    val productSummaries = loadedProductIds.mapNotNull { pId ->
        val product = products.find { it.id == pId } ?: return@mapNotNull null
        val pLoadings = todayLoadings.filter { it.productId == pId }
        val pabrik = pabriks.find { it.id == product.pabrikId }
        val pabrikName = pabrik?.namaPabrik ?: "Supplier Utama"
        val rasio = pLoadings.firstOrNull()?.rasioKonversi ?: product.rasioKonversi
        val hargaBeliDus = pLoadings.firstOrNull()?.hargaBeliPabrikDus ?: product.hargaBeliPabrik

        val totalMuatDus = if (pLoadings.isNotEmpty()) pLoadings.sumOf { it.jumlahDus } else 0
        val totalMuatPcs = if (pLoadings.isNotEmpty()) pLoadings.sumOf { it.totalPcs } else (totalMuatDus * rasio)

        val sisaDus = sisaDusInputs[pId]?.toIntOrNull() ?: 0
        val sisaPcsLepasan = sisaPcsInputs[pId]?.toIntOrNull() ?: 0
        val sisaTotalPcs = (sisaDus * rasio) + sisaPcsLepasan
        val pcsTerdistribusi = (totalMuatPcs - sisaTotalPcs).coerceAtLeast(0)
        val terjualDusEquivalent = if (rasio > 0) pcsTerdistribusi.toDouble() / rasio else 0.0
        val tagihanPabrik = terjualDusEquivalent * hargaBeliDus

        ClosingProductSummary(
            productId = pId,
            productName = product.nama,
            pabrikId = product.pabrikId ?: "",
            pabrikName = pabrikName,
            satuanBesar = product.satuanBesar.ifBlank { "Pack" },
            rasioKonversi = rasio,
            hargaBeliPabrikDus = hargaBeliDus,
            totalMuatDus = totalMuatDus,
            totalMuatPcs = totalMuatPcs,
            sisaDusSore = sisaDus,
            sisaPcsLepasanSore = sisaPcsLepasan,
            sisaTotalPcsSore = sisaTotalPcs,
            pcsTerdistribusi = pcsTerdistribusi,
            terjualDusEquivalent = terjualDusEquivalent,
            tagihanPabrik = tagihanPabrik
        )
    }

    // Kelompokkan per Supplier / Pabrik
    val supplierSummaries = productSummaries.groupBy { it.pabrikName }.map { (pabrikName, items) ->
        ClosingSupplierSummary(
            pabrikId = items.firstOrNull()?.pabrikId ?: "",
            pabrikName = pabrikName,
            products = items,
            totalMuatDus = items.sumOf { it.totalMuatDus },
            sisaDusSore = items.sumOf { it.sisaDusSore },
            sisaPcsLepasanSore = items.sumOf { it.sisaPcsLepasanSore },
            sisaTotalPcsSore = items.sumOf { it.sisaTotalPcsSore },
            pcsTerdistribusi = items.sumOf { it.pcsTerdistribusi },
            totalTerjualDusEquivalent = items.sumOf { it.terjualDusEquivalent },
            totalTagihanPabrik = items.sumOf { it.tagihanPabrik }
        )
    }

    val totalMuatDusOverall = supplierSummaries.sumOf { it.totalMuatDus }
    val totalTagihanSemuaSupplier = supplierSummaries.sumOf { it.totalTagihanPabrik }

    // Data kas outlet & sortir BS hari ini
    val todayTransactions = transactions.filter { it.tanggal == today && it.warungId != "CLOSING_SALES" && it.jenis != "CLOSING_HARIAN" }
    val totalKasWarungHariIni = todayTransactions.sumOf { it.uangDiterima }
    val totalLakuPcsToday = todayTransactions.sumOf { it.pcsLaku }
    val totalTitipBaruPcsToday = todayTransactions.filter { it.jenis == "TITIP_BARU" }.sumOf { it.restockBaruPcs }
    val totalSortirTodayPcs = bsSortirs.filter {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp)) == today
    }.sumOf { it.totalBsAwalPcs }

    val selisihKas = totalKasWarungHariIni - totalTagihanSemuaSupplier

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BlueAccent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Closing Sore Multi-Supplier",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Rekonsiliasi Fisik Mobil & Tagihan Principal ($today)",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Notice Konsinyasi
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = RoseSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RoseDanger.copy(alpha = 0.3f)))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Aturan Konsinyasi: Barang Retur ditarik TIDAK mengurangi tagihan supplier. Salesman wajib setor barang fresh yang keluar/terdistribusi.",
                                fontSize = 11.sp,
                                color = RoseText,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Loop per Supplier
                    supplierSummaries.forEach { supplier ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate100),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate300))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Supplier Title Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = supplier.pabrikName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate900
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BlueAccent.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${supplier.products.size} SKU",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BlueAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Slate300)

                                // List of Products under this supplier
                                supplier.products.forEach { prod ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = prod.productName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Slate900
                                                    )
                                                    Text(
                                                        text = "Rasio: 1 ${prod.satuanBesar} = ${prod.rasioKonversi} Pcs • Modal: ${SfaViewModel.formatRupiah(prod.hargaBeliPabrikDus)}/${prod.satuanBesar}",
                                                        fontSize = 10.sp,
                                                        color = Slate500
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Slate200
                                                ) {
                                                    Text(
                                                        text = "Muat: ${prod.totalMuatDus} ${prod.satuanBesar} (${prod.totalMuatPcs} Pcs)",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate800,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = sisaDusInputs[prod.productId] ?: "0",
                                                    onValueChange = { sisaDusInputs[prod.productId] = it },
                                                    label = { Text(tr("Sisa ${prod.satuanBesar} Utuh", "Remaining Intact ${prod.satuanBesar}", lang), fontSize = 11.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = appTextFieldColors(),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    singleLine = true
                                                )
                                                OutlinedTextField(
                                                    value = sisaPcsInputs[prod.productId] ?: "0",
                                                    onValueChange = { sisaPcsInputs[prod.productId] = it },
                                                    label = { Text(tr("Sisa Pcs Lepasan", "Remaining Loose Pcs", lang), fontSize = 11.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = appTextFieldColors(),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    singleLine = true
                                                )
                                            }

                                            // Mini Result Chip
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF1F5F9),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Terjual: ${prod.pcsTerdistribusi} Pcs (~${String.format(java.util.Locale.US, "%.1f", prod.terjualDusEquivalent)} ${prod.satuanBesar})",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldSuccess
                                                    )
                                                    Text(
                                                        text = "Setoran: ${SfaViewModel.formatRupiah(prod.tagihanPabrik)}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate900
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Subtotal Supplier
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Subtotal Setoran ${supplier.pabrikName}:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700
                                    )
                                    Text(
                                        text = SfaViewModel.formatRupiah(supplier.totalTagihanPabrik),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                }
                            }
                        }
                    }

                    // Rekap Total Kasir Card
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Slate900)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "REKAPITULASI KEUANGAN HARIAN",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(tr("Total Muat Seluruh SKU:", "Total Load All SKUs:", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text("$totalMuatDusOverall Pack", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(tr("Penjualan Outlet (Laku):", "Outlet Sales (Sold):", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text("$totalLakuPcsToday Pcs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            if (totalTitipBaruPcsToday > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tr("Titip Baru di Warung:", "New Consignment at Store:", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    Text(tr("$totalTitipBaruPcsToday Pcs (Modal Tertanam)", "$totalTitipBaruPcsToday Pcs (Invested Capital)", lang), color = BlueAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(tr("TOTAL SETORAN SUPPLIER:", "TOTAL SUPPLIER DEPOSIT:", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(SfaViewModel.formatRupiah(totalTagihanSemuaSupplier), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(tr("Total Kas Diterima dari Outlet:", "Total Cash Received from Outlets:", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(SfaViewModel.formatRupiah(totalKasWarungHariIni), color = AmberWarning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (selisihKas >= 0) "Surplus Kasir:" else "Selisih Kas (Modal Tertanam/Bon):", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(
                                    text = if (selisihKas >= 0) "+${SfaViewModel.formatRupiah(selisihKas)}" else "-${SfaViewModel.formatRupiah(kotlin.math.abs(selisihKas))}",
                                    color = if (selisihKas >= 0) EmeraldSuccess else RoseDanger,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(tr("Batal", "Cancel", lang))
                    }
                    Button(
                        onClick = {
                            val items = loadedProductIds.map { pId ->
                                val dus = sisaDusInputs[pId]?.toIntOrNull() ?: 0
                                val pcs = sisaPcsInputs[pId]?.toIntOrNull() ?: 0
                                com.example.data.repository.ProductClosingInput(
                                    productId = pId,
                                    sisaDusSore = dus,
                                    sisaPcsLepasanSore = pcs
                                )
                            }
                            val summary = ClosingSummaryData(
                                tanggal = today,
                                waktuClosing = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                                totalMuatDus = totalMuatDusOverall,
                                totalTagihanSemuaSupplier = totalTagihanSemuaSupplier,
                                totalKasWarungHariIni = totalKasWarungHariIni,
                                selisihKas = selisihKas,
                                totalTxCount = todayTransactions.size,
                                totalSortirTodayPcs = totalSortirTodayPcs,
                                supplierSummaries = supplierSummaries,
                                productSummaries = productSummaries,
                                productName = if (productSummaries.size == 1) productSummaries.first().productName else "Multi-SKU (${productSummaries.size} Produk)",
                                sisaDusSore = supplierSummaries.sumOf { it.sisaDusSore },
                                sisaPcsLepasan = supplierSummaries.sumOf { it.sisaPcsLepasanSore },
                                terjualDus = supplierSummaries.sumOf { it.totalTerjualDusEquivalent }.toInt(),
                                tagihanPabrikFinal = totalTagihanSemuaSupplier
                            )
                            onSubmitBatch(items, summary)
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Simpan & Cetak Struk", "Save & Print Receipt", lang))
                    }
                }
            }
        }
    }
}

// 6. BLUETOOTH THERMAL RECEIPT DIALOG (58mm)
@Composable
fun ReceiptDialog(
    transaction: TransactionEntity,
    warung: WarungEntity?,
    product: ProductEntity?,
    userProfile: UserProfileEntity? = null,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current

val isTitip = transaction.jenis == "TITIP_BARU"
    val satuanKecil = product?.satuanKecil ?: "Pcs"
    val qtyTitipBaru = if (transaction.restockBaruPcs > 0) transaction.restockBaruPcs else transaction.totalTitipanAktifPcs

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate400)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = userProfile?.namaDistributor?.ifBlank { "DISTRIBUTOR & SFA DISTRIBUSI" } ?: "DISTRIBUTOR & SFA DISTRIBUSI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                if (!userProfile?.alamatDepo.isNullOrBlank()) {
                    Text(
                        text = userProfile!!.alamatDepo,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        color = Slate600
                    )
                }
                Text(
                    text = if (isTitip) "BUKTI DROP KONSINYASI OUTLET" else "BUKTI TRANSAKSI & RETUR OUTLET",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "================================",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Slate500
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("No. Faktur:", "Invoice No:", lang), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text(transaction.id.take(12).uppercase(), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("Tipe Transaksi:", "Transaction Type:", lang), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text(
                        if (isTitip) "DROP TITIP BARU" else "TARIK SISA & SETTLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("Salesman:", "Salesman:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(
                        "${userProfile?.namaSalesman?.ifBlank { "Sales" } ?: "Sales"} ${if (!userProfile?.platNomorMobil.isNullOrBlank()) "(${userProfile!!.platNomorMobil})" else ""}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("Outlet:", "Outlet:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(warung?.namaWarung ?: "Outlet", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("Waktu:", "Time:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(SfaViewModel.formatDate(transaction.timestamp), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                if (transaction.gpsLat != 0.0 || transaction.gpsLng != 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("GPS:", "GPS:", lang), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        Text(String.format(java.util.Locale.US, "%.5f, %.5f", transaction.gpsLat, transaction.gpsLng), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }

                Text(
                    text = "--------------------------------",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Slate500
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Produk: ${product?.nama ?: "Barang SKU"}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (isTitip) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Dititipkan (Baru):", "Consigned (New):", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("$qtyTitipBaru $satuanKecil", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Harga Titip Satuan:", "Consignment Unit Price:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text(SfaViewModel.formatRupiah(transaction.hargaSatuan), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Nilai Barang Dititip:", "Consigned Goods Value:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(SfaViewModel.formatRupiah(qtyTitipBaru * transaction.hargaSatuan), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Sumber Stok Mobil:", "Vehicle Stock Source:", lang), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        Text(if (transaction.sumberStok == "FRESH_PABRIK") "Fresh Pabrik" else "Pribadi Repack", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Total Titipan Aktif:", "Total Active Consignment:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${transaction.totalTitipanAktifPcs} $satuanKecil", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (transaction.saldoPiutangBaru > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Saldo Bon Outlet:", "Outlet Credit Balance:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            Text(SfaViewModel.formatRupiah(transaction.saldoPiutangBaru), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Titipan Periode Lalu:", "Previous Consignment:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("${transaction.sisaTitipanLaluPcs} $satuanKecil", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Sisa Fisik Ditarik:", "Physical Stock Returned:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("${transaction.sisaFisikPcs} $satuanKecil", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Terjual: ${transaction.pcsLaku} $satuanKecil @ ${SfaViewModel.formatRupiah(transaction.hargaSatuan)}", "Sold: ${transaction.pcsLaku} $satuanKecil @ ${SfaViewModel.formatRupiah(transaction.hargaSatuan)}", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text(SfaViewModel.formatRupiah(transaction.subtotalLaku), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Saldo Bon Lama:", "Previous Credit Balance:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text(SfaViewModel.formatRupiah(transaction.saldoPiutangLama), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("TOTAL TAGIHAN:", "TOTAL DUE:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(SfaViewModel.formatRupiah(transaction.grandTotalTagihan), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("BAYAR DITERIMA:", "PAYMENT RECEIVED:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(SfaViewModel.formatRupiah(transaction.uangDiterima), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("SISA BON BARU:", "NEW CREDIT BALANCE:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text(SfaViewModel.formatRupiah(transaction.saldoPiutangBaru), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    Text(
                        text = "--------------------------------",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Slate500
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Retur Ditarik:", "Returns Pulled:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("${transaction.bsDitarikPcs} $satuanKecil", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Drop Restock Baru:", "New Drop Restock:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("${transaction.restockBaruPcs} $satuanKecil", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Total Titipan Aktif:", "Total Active Consignment:", lang), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${transaction.totalTitipanAktifPcs} $satuanKecil", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (transaction.catatan.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Catatan: ${transaction.catatan}", "Note: ${transaction.catatan}", lang), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Slate600)
                    }
                }

                Text(
                    text = "================================",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Slate500
                )
                Text(
                    text = "Terima Kasih Atas Kerjasamanya\nBarang titipan tanggung jawab bersama",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = Slate600
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(tr("Tutup", "Close", lang))
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Cetak BT", "Print BT", lang))
                    }
                }
            }
        }
    }
}

// 6B. THERMAL RECEIPT CLOSING HARIAN & SETORAN MULTI-SUPPLIER (58mm)
@Composable
fun ClosingReceiptDialog(
    data: ClosingSummaryData,
    userProfile: UserProfileEntity? = null,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current

val context = androidx.compose.ui.platform.LocalContext.current
    val suppliers = data.supplierSummaries

    // 0 = Rekap Lengkap Gabungan, 1..N = Supplier N
    var selectedSupplierIndex by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate400)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = Slate900)
                        Text(
                            text = "Struk Closing Harian 58mm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500)
                    }
                }

                // If multiple suppliers, provide separated tabs!
                if (suppliers.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedSupplierIndex,
                        edgePadding = 0.dp,
                        containerColor = Slate100,
                        contentColor = Slate900,
                        indicator = { tabPositions ->
                            if (selectedSupplierIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedSupplierIndex]),
                                    color = Slate900,
                                    height = 3.dp
                                )
                            }
                        },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedSupplierIndex == 0,
                            onClick = { selectedSupplierIndex = 0 },
                            selectedContentColor = Slate900,
                            unselectedContentColor = Slate600,
                            text = { Text(tr("⚡ REKAP GABUNGAN", "⚡ COMBINED SUMMARY", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedSupplierIndex == 0) Slate900 else Slate600) }
                        )
                        suppliers.forEachIndexed { idx, supp ->
                            val isSuppSelected = selectedSupplierIndex == idx + 1
                            Tab(
                                selected = isSuppSelected,
                                onClick = { selectedSupplierIndex = idx + 1 },
                                selectedContentColor = Slate900,
                                unselectedContentColor = Slate600,
                                text = { Text("🏭 ${supp.pabrikName.take(16)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSuppSelected) Slate900 else Slate600) }
                            )
                        }
                    }
                }

                // Monospace Thermal Receipt 58mm Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFFBFBFB), RoundedCornerShape(8.dp))
                        .border(1.dp, Slate300, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedSupplierIndex == 0 || suppliers.size <= 1) {
                        CombinedClosingReceiptContent(data = data, userProfile = userProfile)
                    } else {
                        val activeSupplier = suppliers.getOrNull(selectedSupplierIndex - 1)
                        if (activeSupplier != null) {
                            SingleSupplierReceiptContent(
                                supplier = activeSupplier,
                                closingDate = data.tanggal,
                                closingTime = data.waktuClosing,
                                userProfile = userProfile
                            )
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(tr("Tutup", "Close", lang), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val msg = if (selectedSupplierIndex == 0 || suppliers.size <= 1) {
                                "Mencetak Rekap Closing Gabungan ke printer Bluetooth thermal 58mm..."
                            } else {
                                val suppName = suppliers.getOrNull(selectedSupplierIndex - 1)?.pabrikName ?: "Supplier"
                                "Mencetak Struk Khusus Supplier $suppName ke printer thermal 58mm..."
                            }
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedSupplierIndex == 0 || suppliers.size <= 1) "Cetak Rekap" else "Cetak Struk Supplier",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CombinedClosingReceiptContent(
    data: ClosingSummaryData,
    userProfile: UserProfileEntity?
) {
    val lang = LocalAppLanguage.current

Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = userProfile?.namaDistributor?.ifBlank { "DISTRIBUTOR & SFA DISTRIBUSI" } ?: "DISTRIBUTOR & SFA DISTRIBUSI",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        if (!userProfile?.alamatDepo.isNullOrBlank()) {
            Text(
                text = userProfile?.alamatDepo.orEmpty(),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                color = Slate600
            )
        }
        Text(
            text = "REKAPITULASI CLOSING GABUNGAN",
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "================================",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Slate500
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Tanggal Closing:", "Closing Date:", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(data.tanggal, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Waktu Cetak    :", "Print Time     :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(data.waktuClosing, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Salesman / User:", "Salesman / User:", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(userProfile?.namaSalesman?.ifBlank { "Sales SFA" } ?: "Sales SFA", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            text = "--------------------------------",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Slate500
        )

        // Rincian per Supplier & SKU
        data.supplierSummaries.forEach { supp ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🏭 [${supp.pabrikName.uppercase()}]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueText
                )
            }
            supp.products.forEach { prod ->
                Text(
                    text = "• ${prod.productName}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("  Muat/Sisa : ${prod.totalMuatDus}${prod.satuanBesar.take(1)} / ${prod.sisaDusSore}${prod.satuanBesar.take(1)}+${prod.sisaPcsLepasanSore}P", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("Laku: ${prod.pcsTerdistribusi}P", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("  Setoran   : ${String.format(java.util.Locale.US, "%.1f", prod.terjualDusEquivalent)} ${prod.satuanBesar}", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text(SfaViewModel.formatRupiah(prod.tagihanPabrik), fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("  SUBTOTAL ${supp.pabrikName.take(10)}:", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(SfaViewModel.formatRupiah(supp.totalTagihanPabrik), fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = " - - - - - - - - - - - - - - - -",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Slate400
            )
        }

        // Summary Total
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("TOTAL SETORAN SEMUA SUPPLIER:", "TOTAL DEPOSIT ALL SUPPLIERS:", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(SfaViewModel.formatRupiah(data.totalTagihanSemuaSupplier), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("KAS OUTLET DITERIMA         :", "OUTLET CASH RECEIVED        :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(SfaViewModel.formatRupiah(data.totalKasWarungHariIni), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("STATUS KASIR (SELISIH)      :", "CASHIER STATUS (VARIANCE)   :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(
                if (data.selisihKas >= 0) "Surplus +${SfaViewModel.formatRupiah(data.selisihKas)}" else "Defisit -${SfaViewModel.formatRupiah(kotlin.math.abs(data.selisihKas))}",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (data.selisihKas >= 0) EmeraldSuccess else RoseDanger
            )
        }

        Text(
            text = "--------------------------------",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Slate500
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Kunjungan Toko :", "Store Visits   :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(tr("${data.totalTxCount} Outlet", "${data.totalTxCount} Outlets", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Sortir Retur  :", "Sort Returns   :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text("${data.totalSortirTodayPcs} Pcs", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }

        Text(
            text = "================================",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Salesman", "Salesman", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("(..........)", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Kasir / Finance", "Cashier / Finance", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("(..........)", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SingleSupplierReceiptContent(
    supplier: ClosingSupplierSummary,
    closingDate: String,
    closingTime: String,
    userProfile: UserProfileEntity?
) {
    val lang = LocalAppLanguage.current

Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = userProfile?.namaDistributor?.ifBlank { "DISTRIBUTOR & SFA DISTRIBUSI" } ?: "DISTRIBUTOR & SFA DISTRIBUSI",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Text(
            text = "REKAP SETORAN PRINCIPAL",
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "SUPPLIER: ${supplier.pabrikName.uppercase()}",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = BlueText
        )
        Text(
            text = "================================",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Slate500
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Tanggal Closing:", "Closing Date:", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(closingDate, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Waktu Cetak    :", "Print Time     :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(closingTime, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Salesman       :", "Salesman       :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(userProfile?.namaSalesman?.ifBlank { "Sales SFA" } ?: "Sales SFA", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            text = "--------------------------------",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Slate500
        )

        supplier.products.forEach { prod ->
            Text(
                text = prod.productName,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("• Rasio Konversi :", "• Conversion Ratio :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text(tr("1 ${prod.satuanBesar} = ${prod.rasioKonversi} Pcs", "1 ${prod.satuanBesar} = ${prod.rasioKonversi} Pcs", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("• Total Muat     :", "• Total Load       :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text(tr("${prod.totalMuatDus} ${prod.satuanBesar} (${prod.totalMuatPcs} Pcs)", "${prod.totalMuatDus} ${prod.satuanBesar} (${prod.totalMuatPcs} Pcs)", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("• Sisa Fisik Sore:", "• Evening Stock Left:", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text("${prod.sisaDusSore} ${prod.satuanBesar} + ${prod.sisaPcsLepasanSore} Pcs", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("• Terdistribusi  :", "• Distributed      :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text("${prod.pcsTerdistribusi} Pcs (~${String.format(java.util.Locale.US, "%.1f", prod.terjualDusEquivalent)} ${prod.satuanBesar})", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("• Harga Beli/${prod.satuanBesar} :", "• Buy Price/${prod.satuanBesar} :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text(SfaViewModel.formatRupiah(prod.hargaBeliPabrikDus), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("• Tagihan SKU    :", "• SKU Invoice      :", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(SfaViewModel.formatRupiah(prod.tagihanPabrik), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = " - - - - - - - - - - - - - - - -",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Slate400
            )
        }

        Text(
            text = "--------------------------------",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Slate500
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("TOTAL WAJIB SETOR:", "TOTAL DEPOSIT REQUIRED:", lang), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(SfaViewModel.formatRupiah(supplier.totalTagihanPabrik), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            text = "================================",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Salesman", "Salesman", lang), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("(..........)", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kasir / ${supplier.pabrikName.take(12)}", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("(..........)", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
        }
    }
}

// 7. WRITE OFF DIALOG
@Composable
fun WriteOffDialog(
    warung: WarungEntity,
    onDismiss: () -> Unit,
    onSubmit: (harga: Double, alasan: String) -> Unit
) {
    val lang = LocalAppLanguage.current

var alasan by remember { mutableStateOf("Warung Bangkrut / Tutup Permanen") }
    val piutang = warung.saldoPiutang
    val stok = warung.stokTitipanPcs
    val totalKerugian = piutang + (stok * 1600.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Write-Off / Hapus Buku Warung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RoseDanger
                )
                Text(
                    text = "Warung: ${warung.namaWarung}",
                    fontSize = 12.sp,
                    color = Slate500
                )

                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = RoseSurface)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tr("Piutang Bon Hangus: ${SfaViewModel.formatRupiah(piutang)}", "Written-Off Credit: ${SfaViewModel.formatRupiah(piutang)}", lang), color = RoseDanger, fontSize = 11.sp)
                        Text(tr("Stok Titipan Hangus: $stok Pcs (${SfaViewModel.formatRupiah(stok * 1600.0)})", "Written-Off Stock: $stok Pcs (${SfaViewModel.formatRupiah(stok * 1600.0)})", lang), color = RoseDanger, fontSize = 11.sp)
                        HorizontalDivider(color = RoseBorder)
                        Text(tr("Total Kerugian Write-Off: ${SfaViewModel.formatRupiah(totalKerugian)}", "Total Write-Off Loss: ${SfaViewModel.formatRupiah(totalKerugian)}", lang), color = RoseText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = alasan,
                    onValueChange = { alasan = it },
                    label = { Text(tr("Alasan Write-Off", "Write-Off Reason", lang)) },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(tr("Batal", "Cancel", lang))
                    }
                    Button(
                        onClick = { onSubmit(1600.0, alasan) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                    ) {
                        Text(tr("Hapus Buku", "Write-Off", lang))
                    }
                }
            }
        }
    }
}

// ADD/EDIT PRODUCT DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: ProductEntity?,
    pabriks: List<PabrikEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val lang = LocalAppLanguage.current

var nama by remember { mutableStateOf(product?.nama ?: "") }
    var kategori by remember { mutableStateOf(product?.kategori ?: "Makanan & Roti") }
    var selectedPabrikId by remember { mutableStateOf(product?.pabrikId) }
    var satuanBesar by remember { mutableStateOf(product?.satuanBesar ?: "Pack") }
    var satuanKecil by remember { mutableStateOf(product?.satuanKecil ?: "Pcs") }
    var rasioKonversi by remember { mutableStateOf("${product?.rasioKonversi ?: 10}") }
    var hargaBeli by remember { mutableStateOf("${product?.hargaBeliPabrik ?: 11000.0}") }
    var hargaJual by remember { mutableStateOf("${product?.hargaJualDefault ?: 1600.0}") }

    val rasioInt = rasioKonversi.toIntOrNull() ?: 1
    val hargaBeliNum = hargaBeli.toDoubleOrNull() ?: 0.0
    val modalPerUnit = if (rasioInt > 0 && hargaBeliNum > 0) hargaBeliNum / rasioInt else 0.0

    val unitBesarPresets = listOf("Pack", "Dus", "Slop", "Bal", "Renteng", "Box", "Krat", "Lusin", "Karton")
    val unitKecilPresets = listOf("Pcs", "Sachet", "Bungkus", "Butir", "Botol", "Lembar", "Porsi")

    val selectedPabrik = pabriks.find { it.id == selectedPabrikId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (product == null) "Tambah Master Produk" else "Edit Master Produk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Konfigurasi Satuan, Supplier & Saran Harga",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500, modifier = Modifier.size(18.dp))
                    }
                }

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text(tr("Nama Barang / SKU", "Item Name / SKU", lang)) },
                    placeholder = { Text(tr("Contoh: Roti Manis Coklat", "Example: Chocolate Sweet Bread", lang)) },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = kategori,
                    onValueChange = { kategori = it },
                    label = { Text(tr("Kategori Produk", "Product Category", lang)) },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Supplier / Pabrik Asal Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Supplier / Pabrik Asal:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                    
                    if (pabriks.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada master supplier/pabrik terdaftar. Tambahkan supplier di tab 'Supplier & Pabrik' jika ingin mengaitkan produk.",
                                fontSize = 10.sp,
                                color = Slate500,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedPabrikId == null,
                                onClick = { selectedPabrikId = null },
                                label = { Text(tr("Tanpa Supplier", "No Supplier", lang), fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Slate900,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100,
                                    labelColor = Slate700
                                ),
                                border = null
                            )
                            pabriks.forEach { p ->
                                val isSelected = selectedPabrikId == p.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPabrikId = p.id },
                                    label = { Text(p.namaPabrik, fontSize = 11.sp) },
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

                        if (selectedPabrik != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BlueSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BlueBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "Supplier: ${selectedPabrik.namaPabrik}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BlueAccent
                                        )
                                    }
                                    if (selectedPabrik.kebijakanRetur.isNotBlank()) {
                                        Text(
                                            text = "Kebijakan Retur: ${selectedPabrik.kebijakanRetur}",
                                            fontSize = 10.sp,
                                            color = Slate600
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Satuan Besar & Preset Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("Satuan Besar (Grosir/Pabrik):", "Large Unit (Wholesale/Factory):", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
                    OutlinedTextField(
                        value = satuanBesar,
                        onValueChange = { satuanBesar = it },
                        placeholder = { Text(tr("Dus / Pack / Slop...", "Box / Pack / Carton...", lang)) },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        unitBesarPresets.forEach { preset ->
                            SuggestionChip(
                                onClick = { satuanBesar = preset },
                                label = { Text(preset, fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (satuanBesar.equals(preset, ignoreCase = true)) Slate900 else Slate100,
                                    labelColor = if (satuanBesar.equals(preset, ignoreCase = true)) Color.White else Slate700
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                // Satuan Kecil & Preset Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("Satuan Kecil (Eceran/Warung):", "Small Unit (Retail/Store):", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
                    OutlinedTextField(
                        value = satuanKecil,
                        onValueChange = { satuanKecil = it },
                        placeholder = { Text(tr("Pcs / Sachet / Bungkus...", "Pcs / Sachet / Pouch...", lang)) },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        unitKecilPresets.forEach { preset ->
                            SuggestionChip(
                                onClick = { satuanKecil = preset },
                                label = { Text(preset, fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (satuanKecil.equals(preset, ignoreCase = true)) Slate900 else Slate100,
                                    labelColor = if (satuanKecil.equals(preset, ignoreCase = true)) Color.White else Slate700
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                // Rasio Konversi
                OutlinedTextField(
                    value = rasioKonversi,
                    onValueChange = { rasioKonversi = it.filter { ch -> ch.isDigit() } },
                    label = { Text(tr("Isi per 1 $satuanBesar ($satuanKecil)", "Quantity per 1 $satuanBesar ($satuanKecil)", lang)) },
                    placeholder = { Text("10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Harga Beli Pabrik
                OutlinedTextField(
                    value = hargaBeli,
                    onValueChange = { hargaBeli = it },
                    label = { Text(tr("Harga Beli Pabrik / $satuanBesar (Rp)", "Factory Buy Price / $satuanBesar (Rp)", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Modal calculated info
                if (modalPerUnit > 0) {
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
                                tr("Modal Pokok / $satuanKecil:", "Base Cost / $satuanKecil:", lang),
                                fontSize = 11.sp,
                                color = Slate600,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                SfaViewModel.formatRupiah(modalPerUnit),
                                fontSize = 12.sp,
                                color = Slate900,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Harga Jual Default + Auto Suggestion Margins
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = hargaJual,
                        onValueChange = { hargaJual = it },
                        label = { Text(tr("Harga Jual Eceran / $satuanKecil (Rp)", "Retail Price / $satuanKecil (Rp)", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (modalPerUnit > 0) {
                        Text(
                            tr("Saran Cepat Harga Jual (+Margin):", "Quick Selling Price Suggestion (+Margin):", lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(10, 15, 20, 25, 30, 40, 50).forEach { marginPct ->
                                val suggested = (modalPerUnit * (1.0 + marginPct / 100.0)).toLong()
                                // Round to nearest 100 or 500 for clean selling price
                                val roundedClean = ((suggested + 49) / 50) * 50
                                SuggestionChip(
                                    onClick = { hargaJual = roundedClean.toString() },
                                    label = {
                                        Text("+$marginPct% (Rp$roundedClean)", fontSize = 10.sp)
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (hargaJual == roundedClean.toString()) EmeraldSuccess else EmeraldSurface,
                                        labelColor = if (hargaJual == roundedClean.toString()) Color.White else EmeraldText
                                    ),
                                    border = null,
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(tr("Batal", "Cancel", lang))
                    }
                    Button(
                        onClick = {
                            if (nama.isNotBlank()) {
                                onSave(
                                    product?.copy(
                                        nama = nama,
                                        kategori = kategori,
                                        pabrikId = selectedPabrikId,
                                        satuanBesar = satuanBesar.ifBlank { "Pack" },
                                        satuanKecil = satuanKecil.ifBlank { "Pcs" },
                                        rasioKonversi = rasioKonversi.toIntOrNull() ?: 10,
                                        hargaBeliPabrik = hargaBeli.toDoubleOrNull() ?: 11000.0,
                                        hargaJualDefault = hargaJual.toDoubleOrNull() ?: 1600.0
                                    ) ?: ProductEntity(
                                        nama = nama,
                                        kategori = kategori,
                                        pabrikId = selectedPabrikId,
                                        satuanBesar = satuanBesar.ifBlank { "Pack" },
                                        satuanKecil = satuanKecil.ifBlank { "Pcs" },
                                        rasioKonversi = rasioKonversi.toIntOrNull() ?: 10,
                                        hargaBeliPabrik = hargaBeli.toDoubleOrNull() ?: 11000.0,
                                        hargaJualDefault = hargaJual.toDoubleOrNull() ?: 1600.0
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text(tr("Simpan", "Save", lang))
                    }
                }
            }
        }
    }
}

// ADD/EDIT PABRIK DIALOG
@Composable
fun AddEditPabrikDialog(
    pabrik: PabrikEntity?,
    onDismiss: () -> Unit,
    onSave: (PabrikEntity) -> Unit
) {
    val lang = LocalAppLanguage.current

var namaPabrik by remember { mutableStateOf(pabrik?.namaPabrik ?: "") }
    var cp by remember { mutableStateOf(pabrik?.namaCp ?: "") }
    var noHp by remember { mutableStateOf(pabrik?.noHpCp ?: "") }
    var kebijakan by remember { mutableStateOf(pabrik?.kebijakanRetur ?: "BS Tidak Diterima / Hangus") }
    var rekening by remember { mutableStateOf(pabrik?.rekeningBank ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (pabrik == null) "Tambah Master Pabrik" else "Edit Master Pabrik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                OutlinedTextField(value = namaPabrik, onValueChange = { namaPabrik = it }, label = { Text(tr("Nama Pabrik / Principal", "Factory / Principal Name", lang)) }, colors = appTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cp, onValueChange = { cp = it }, label = { Text(tr("Nama Contact Person", "Contact Person Name", lang)) }, colors = appTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = noHp, onValueChange = { noHp = it }, label = { Text(tr("Nomor HP / WhatsApp CP", "CP Phone / WhatsApp Number", lang)) }, colors = appTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = kebijakan, onValueChange = { kebijakan = it }, label = { Text(tr("Kebijakan Retur Barang", "Product Return Policy", lang)) }, colors = appTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rekening, onValueChange = { rekening = it }, label = { Text(tr("Rekening Bank Transfer", "Bank Transfer Account", lang)) }, colors = appTextFieldColors(), modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(tr("Batal", "Cancel", lang)) }
                    Button(
                        onClick = {
                            if (namaPabrik.isNotBlank()) {
                                onSave(
                                    pabrik?.copy(
                                        namaPabrik = namaPabrik,
                                        namaCp = cp,
                                        noHpCp = noHp,
                                        kebijakanRetur = kebijakan,
                                        rekeningBank = rekening
                                    ) ?: PabrikEntity(
                                        namaPabrik = namaPabrik,
                                        namaCp = cp,
                                        noHpCp = noHp,
                                        kebijakanRetur = kebijakan,
                                        rekeningBank = rekening
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) { Text(tr("Simpan", "Save", lang)) }
                }
            }
        }
    }
}

// 9. ADD/EDIT WARUNG DIALOG
@Composable
fun AddEditWarungDialog(
    warung: WarungEntity?,
    rutes: List<RuteEntity>,
    onDismiss: () -> Unit,
    onSave: (WarungEntity) -> Unit
) {
    val lang = LocalAppLanguage.current

val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var namaWarung by remember { mutableStateOf(warung?.namaWarung ?: "") }
    var namaPemilik by remember { mutableStateOf(warung?.namaPemilik ?: "") }
    var noHp by remember { mutableStateOf(warung?.noHp ?: "") }
    var kategoriWarung by remember { mutableStateOf(warung?.kategoriWarung ?: "Kelontong") }
    var alamat by remember { mutableStateOf(warung?.alamatLengkap ?: "") }
    var notes by remember { mutableStateOf(warung?.notes ?: "") }
    var limitHutang by remember { mutableStateOf("${warung?.limitHutangMaksimal?.toLong() ?: 500000}") }
    var ruteId by remember { mutableStateOf(warung?.ruteId ?: rutes.firstOrNull()?.id ?: "RUTE-01") }
    var fotoOutlet by remember { mutableStateOf(warung?.fotoOutlet) }
    var latitude by remember { mutableDoubleStateOf(warung?.latitude ?: -6.2088) }
    var longitude by remember { mutableDoubleStateOf(warung?.longitude ?: 106.8456) }
    var akurasiGps by remember { mutableIntStateOf(warung?.akurasiGpsMeter ?: 10) }
    var showInAppCamera by remember { mutableStateOf(false) }

    var isDetectingGps by remember { mutableStateOf(false) }
    var gpsLockStatus by remember {
        mutableStateOf(
            if (warung != null && warung.latitude != 0.0) {
                "Terkunci: ${String.format(Locale.US, "%.5f, %.5f", warung.latitude, warung.longitude)} (±${warung.akurasiGpsMeter}m)"
            } else null
        )
    }

    val detectGpsAndReverseGeocode: () -> Unit = {
        isDetectingGps = true
        gpsLockStatus = "📡 Mengunci sinyal Satelit GPS (Mode Offline)..."
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val freshLoc = com.example.util.LocationHelper.acquireFreshSatelliteFix(context, maxTimeoutMs = 8000L, targetAccuracyMeters = 25f)
                val detectedLat = if (freshLoc.isAvailable && freshLoc.latitude != 0.0) freshLoc.latitude else com.example.util.LocationHelper.DEFAULT_LAT
                val detectedLng = if (freshLoc.isAvailable && freshLoc.longitude != 0.0) freshLoc.longitude else com.example.util.LocationHelper.DEFAULT_LNG
                val detectedAccuracy = if (freshLoc.isAvailable) freshLoc.accuracyMeter.toInt().coerceAtLeast(3) else 10

                val convertedAddress = try {
                    com.example.util.LocationHelper.reverseGeocode(context, detectedLat, detectedLng)
                } catch (_: Exception) {
                    "Koordinat: ${String.format(Locale.US, "%.5f, %.5f", detectedLat, detectedLng)}"
                }

                withContext(Dispatchers.Main) {
                    latitude = detectedLat
                    longitude = detectedLng
                    akurasiGps = detectedAccuracy
                    if (alamat.isBlank() || alamat.startsWith("Koordinat GPS") || alamat.startsWith("Koordinat:")) {
                        alamat = convertedAddress
                    }
                    gpsLockStatus = "Terkunci: ${String.format(Locale.US, "%.5f, %.5f", detectedLat, detectedLng)} (±${detectedAccuracy}m • ${freshLoc.provider})"
                    isDetectingGps = false
                }
            } catch (e: Exception) {
                val fallback = com.example.util.LocationHelper.getInstantLocation(context)
                withContext(Dispatchers.Main) {
                    latitude = fallback.latitude
                    longitude = fallback.longitude
                    akurasiGps = fallback.accuracyMeter.toInt().coerceAtLeast(10)
                    gpsLockStatus = "Terkunci: ${String.format(Locale.US, "%.5f, %.5f", fallback.latitude, fallback.longitude)} (±${akurasiGps}m)"
                    isDetectingGps = false
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        detectGpsAndReverseGeocode()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoOutlet = uri.toString()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (warung == null) "Tambah Master Outlet" else "Edit Master Outlet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Lengkapi data toko, lokasi GPS & catatan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = Slate200)

                OutlinedTextField(
                    value = namaWarung,
                    onValueChange = { namaWarung = it },
                    label = { Text(tr("Nama Outlet / Toko *", "Outlet / Store Name *", lang)) },
                    placeholder = { Text(tr("Contoh: Warung Berkah Jaya", "Example: Berkah Jaya Grocery", lang)) },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Kategori Outlet Fast Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "KATEGORI OUTLET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Kelontong", "Warkop", "Sembako", "Kantin/Kios", "Minimarket", "Grosir").forEach { cat ->
                            val isSelected = kategoriWarung.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { kategoriWarung = cat },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Slate900,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = namaPemilik,
                        onValueChange = { namaPemilik = it },
                        label = { Text(tr("Nama Pemilik", "Owner Name", lang)) },
                        placeholder = { Text(tr("Ibu Siti", "Mrs. Siti", lang)) },
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.1f)
                    )

                    OutlinedTextField(
                        value = noHp,
                        onValueChange = { noHp = it },
                        label = { Text(tr("No. WA / HP", "WA / Phone No", lang)) },
                        placeholder = { Text("0812...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.1f)
                    )
                }

                // Pilih Jalur Rute Kunjungan
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JALUR RUTE KUNJUNGAN *",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate600,
                            letterSpacing = 0.5.sp
                        )
                        val activeRuteObj = rutes.find { it.id == ruteId }
                        if (activeRuteObj != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate900
                            ) {
                                Text(
                                    text = "Hari: ${activeRuteObj.hariKunjungan}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (rutes.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rutes.forEach { r ->
                                val isSelected = ruteId == r.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { ruteId = r.id },
                                    label = {
                                        Text(
                                            text = "${r.namaRute.split("-").first().trim()} (${r.hariKunjungan})",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                    } else {
                        Text(tr("Belum ada master rute. Default: Jalur 1", "No master routes yet. Default: Route 1", lang), fontSize = 11.sp, color = Slate500)
                    }
                }

                // Alamat Field with GPS Auto-detect Button on the Side
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = alamat,
                            onValueChange = { alamat = it },
                            label = { Text(tr("Alamat Lengkap", "Full Address", lang)) },
                            placeholder = { Text(tr("Ketik alamat atau klik tombol GPS", "Type address or tap GPS button", lang)) },
                            colors = appTextFieldColors(),
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_alamat_warung")
                        )

                        // GPS Auto-detect Button
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            enabled = !isDetectingGps,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Slate900,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(64.dp)
                                .testTag("btn_detect_gps_alamat")
                        ) {
                            if (isDetectingGps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = tr("Deteksi GPS & Auto-Fill Alamat", "Detect GPS & Auto-Fill Address", lang),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "GPS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // GPS Status / Lock indicator
                    if (gpsLockStatus != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = gpsLockStatus ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Note / Catatan Toko Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(tr("Note / Catatan Khusus Toko (Opsional)", "Special Store Notes (Optional)", lang)) },
                    placeholder = { Text(tr("Contoh: Patokan seberang masjid, istirahat jam 12-13...", "Example: Landmark opposite mosque, lunch break 12-1", lang)) },
                    minLines = 2,
                    maxLines = 3,
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_note_warung")
                )

                // Limit Bon Maksimal Field + Presets
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = limitHutang,
                        onValueChange = { limitHutang = it },
                        label = { Text(tr("Limit Bon Maksimal (Rp)", "Max Credit Limit (Rp)", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(200000L to "200rb", 500000L to "500rb", 1000000L to "1 Juta", 2000000L to "2 Juta", 5000000L to "5 Juta").forEach { (amount, label) ->
                            val isSelected = limitHutang == amount.toString()
                            SuggestionChip(
                                onClick = { limitHutang = amount.toString() },
                                label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) Slate900 else Color.White,
                                    labelColor = if (isSelected) Color.White else Slate800
                                ),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                // Foto Outlet (Opsional: Kamera In-App & Galeri)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "FOTO OUTLET / TOKO (OPSIONAL)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )

                    if (fotoOutlet != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Slate900),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = fotoOutlet,
                                        contentDescription = tr("Foto Outlet", "Outlet Photo", lang),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldSuccess,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                            Text(tr("Foto Terpasang", "Photo Attached", lang), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { showInAppCamera = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tr("Ulang Foto", "Retake Photo", lang), fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tr("Galeri", "Gallery", lang), fontSize = 11.sp)
                                    }

                                    TextButton(
                                        onClick = { fotoOutlet = null },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(tr("Hapus", "Delete", lang), color = RoseDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Slate50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Ambil foto tampak depan outlet / etalase toko",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showInAppCamera = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tr("Kamera In-App", "In-App Camera", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tr("Pilih Galeri", "Select Gallery", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(tr("Batal", "Cancel", lang), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (namaWarung.isNotBlank()) {
                                onSave(
                                    warung?.copy(
                                        namaWarung = namaWarung,
                                        namaPemilik = namaPemilik,
                                        noHp = noHp,
                                        kategoriWarung = kategoriWarung,
                                        alamatLengkap = alamat,
                                        notes = notes,
                                        latitude = latitude,
                                        longitude = longitude,
                                        akurasiGpsMeter = akurasiGps,
                                        limitHutangMaksimal = limitHutang.toDoubleOrNull() ?: 500000.0,
                                        ruteId = ruteId,
                                        fotoOutlet = fotoOutlet
                                    ) ?: WarungEntity(
                                        namaWarung = namaWarung,
                                        namaPemilik = namaPemilik,
                                        noHp = noHp,
                                        kategoriWarung = kategoriWarung,
                                        alamatLengkap = alamat,
                                        notes = notes,
                                        latitude = latitude,
                                        longitude = longitude,
                                        akurasiGpsMeter = akurasiGps,
                                        limitHutangMaksimal = limitHutang.toDoubleOrNull() ?: 500000.0,
                                        ruteId = ruteId,
                                        fotoOutlet = fotoOutlet
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1.5f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Simpan Outlet", "Save Outlet", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showInAppCamera) {
        InAppCameraDialog(
            onDismiss = { showInAppCamera = false },
            onPhotoCaptured = { capturedUri ->
                fotoOutlet = capturedUri
                showInAppCamera = false
            }
        )
    }
}

// 10. ADD/EDIT RUTE DIALOG
@Composable
fun AddEditRuteDialog(
    rute: RuteEntity?,
    onDismiss: () -> Unit,
    onSave: (RuteEntity) -> Unit
) {
    val lang = LocalAppLanguage.current

var namaRute by remember { mutableStateOf(rute?.namaRute ?: "") }
    var hari by remember { mutableStateOf(rute?.hariKunjungan ?: "Senin") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (rute == null) "Tambah Rute Jalur" else "Edit Rute Jalur",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(value = namaRute, onValueChange = { namaRute = it }, label = { Text(tr("Nama Rute / Jalur", "Route Name", lang)) }, colors = appTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hari, onValueChange = { hari = it }, label = { Text(tr("Hari Kunjungan", "Visit Day", lang)) }, colors = appTextFieldColors(), modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(tr("Batal", "Cancel", lang)) }
                    Button(
                        onClick = {
                            if (namaRute.isNotBlank()) {
                                onSave(
                                    rute?.copy(namaRute = namaRute, hariKunjungan = hari)
                                        ?: RuteEntity(namaRute = namaRute, hariKunjungan = hari)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) { Text(tr("Simpan", "Save", lang)) }
                }
            }
        }
    }
}

// 11. WARUNG DETAIL DIALOG
@Composable
fun WarungDetailDialog(
    warung: WarungEntity,
    onDismiss: () -> Unit,
    onWriteOff: () -> Unit,
    onManageCustomPrices: () -> Unit = {
},
    onViewStatistics: () -> Unit = {}
) {
    val lang = LocalAppLanguage.current

    // Forward directly to full-fledged Outlet Detail & Statistics Dialog
    onViewStatistics()
}

// 11B. OUTLET COMPREHENSIVE STATISTICS & PERFORMANCE ANALYTICS DIALOG
data class ProductSalesStat(val name: String, val pcs: Int, val revenue: Double)

enum class OutletTxFilter(val label: String) {
    SEMUA("Semua Transaksi"),
    TITIP_BARU("Drop Konsinyasi"),
    TARIK_SETTLE("Tarik & Settle"),
    LUNAS("Lunas"),
    PIUTANG_BON("Ada Bon / Piutang")
}

enum class OutletTxSort(val label: String) {
    TERBARU("Terbaru (Waktu)"),
    TERLAMA("Terlama"),
    NILAI_TERBESAR("Nilai Omset Terbesar"),
    PCS_TERBANYAK("Qty Laku Terbanyak"),
    RETUR_BS_TERBANYAK("Retur Terbanyak")
}

@Composable
fun OutletStatisticsDialog(
    warung: WarungEntity,
    transactions: List<TransactionEntity>,
    products: List<ProductEntity>,
    userProfile: UserProfileEntity? = null,
    onDismiss: () -> Unit,
    onTitipBaru: () -> Unit,
    onTarikSisa: () -> Unit,
    onManageCustomPrices: () -> Unit,
    onAiRecommendation: () -> Unit = {
}
) {
    val lang = LocalAppLanguage.current

    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(OutletTxFilter.SEMUA) }
    var selectedSort by remember { mutableStateOf(OutletTxSort.TERBARU) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedReceiptTx by remember { mutableStateOf<TransactionEntity?>(null) }

    // Live GPS distance
    val currentGps = remember { LocationHelper.getInstantLocation(context) }
    val distanceMeters = remember(currentGps, warung) {
        LocationHelper.calculateDistanceMeters(currentGps.latitude, currentGps.longitude, warung.latitude, warung.longitude)
    }
    val formattedDistance = remember(distanceMeters) {
        LocationHelper.formatDistance(distanceMeters)
    }

    // Filtered & Sorted Transactions
    val processedTransactions = remember(transactions, searchQuery, selectedFilter, selectedSort) {
        transactions.filter { tx ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val prod = products.find { it.id == tx.productId }
                tx.tanggal.contains(searchQuery, ignoreCase = true) ||
                tx.catatan.contains(searchQuery, ignoreCase = true) ||
                tx.jenis.contains(searchQuery, ignoreCase = true) ||
                (prod?.nama?.contains(searchQuery, ignoreCase = true) == true)
            }

            val matchesFilter = when (selectedFilter) {
                OutletTxFilter.SEMUA -> true
                OutletTxFilter.TITIP_BARU -> tx.jenis == "TITIP_BARU"
                OutletTxFilter.TARIK_SETTLE -> tx.jenis != "TITIP_BARU" && tx.jenis != "CLOSING_HARIAN"
                OutletTxFilter.LUNAS -> tx.statusBayar.equals("LUNAS", ignoreCase = true) || tx.uangDiterima >= tx.grandTotalTagihan
                OutletTxFilter.PIUTANG_BON -> tx.grandTotalTagihan > tx.uangDiterima || tx.statusBayar.contains("BON", ignoreCase = true)
            }

            matchesSearch && matchesFilter
        }.sortedWith { a, b ->
            when (selectedSort) {
                OutletTxSort.TERBARU -> b.timestamp.compareTo(a.timestamp)
                OutletTxSort.TERLAMA -> a.timestamp.compareTo(b.timestamp)
                OutletTxSort.NILAI_TERBESAR -> b.grandTotalTagihan.compareTo(a.grandTotalTagihan)
                OutletTxSort.PCS_TERBANYAK -> b.pcsLaku.compareTo(a.pcsLaku)
                OutletTxSort.RETUR_BS_TERBANYAK -> b.bsDitarikPcs.compareTo(a.bsDitarikPcs)
            }
        }
    }

    val totalTransactionsCount = transactions.size
    val totalGrossSales = transactions.sumOf { it.subtotalLaku }
    val totalCashCollected = transactions.sumOf { it.uangDiterima }
    val totalPcsSold = transactions.sumOf { it.pcsLaku }
    val totalBsPcs = transactions.sumOf { it.bsDitarikPcs }

    val avgSalesPerVisit = if (totalTransactionsCount > 0) totalGrossSales / totalTransactionsCount else 0.0
    val avgPcsPerVisit = if (totalTransactionsCount > 0) totalPcsSold.toDouble() / totalTransactionsCount else 0.0
    val collectionRatePercent = if (totalGrossSales > 0.0) ((totalCashCollected / totalGrossSales) * 100.0).coerceIn(0.0, 100.0) else 100.0
    val bsRatioPercent = if (totalPcsSold + totalBsPcs > 0) ((totalBsPcs.toDouble() / (totalPcsSold + totalBsPcs)) * 100.0) else 0.0

    // Visit days calculation
    val daysSinceVisit = if (warung.tglKunjunganTerakhir > 0) {
        ((System.currentTimeMillis() - warung.tglKunjunganTerakhir) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    } else 999

    // Estimated daily velocity (consumption rate)
    val dailyVelocity = if (totalTransactionsCount >= 2) {
        (totalPcsSold.toDouble() / (totalTransactionsCount * 7.0)).coerceAtLeast(0.5)
    } else {
        (totalPcsSold.toDouble() / 7.0).coerceAtLeast(0.5)
    }
    val suggestedRestock7Days = (dailyVelocity * 7.0).toInt().coerceIn(6, 60)
    val estimatedDaysLeft = if (dailyVelocity > 0 && warung.stokTitipanPcs > 0) (warung.stokTitipanPcs / dailyVelocity).toInt() else 0

    // Top selling products in this warung
    val productSales: List<ProductSalesStat> = remember(transactions, products) {
        transactions.groupBy { it.productId }.map { (pId, txList) ->
            val prod = products.find { it.id == pId }
            val pcs = txList.sumOf { it.pcsLaku }
            val rev = txList.sumOf { it.subtotalLaku }
            ProductSalesStat(prod?.nama ?: "Produk SKU #$pId", pcs, rev)
        }.sortedByDescending { it.pcs }
    }

    // Last 6 transactions for trend visualization
    val recentTx = remember(transactions) {
        transactions.sortedByDescending { it.timestamp }.take(6).reversed()
    }
    val maxTxPcs = (recentTx.maxOfOrNull { it.pcsLaku } ?: 1).coerceAtLeast(1)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Slate900),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: Title, Outlet Name & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Slate900),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = warung.namaWarung,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Pemilik: ${warung.namaPemilik.ifBlank { "-" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                                Text("•", fontSize = 10.sp, color = Slate400)
                                Text(
                                    text = formattedDistance,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Slate100)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate700, modifier = Modifier.size(18.dp))
                    }
                }

                // 3 TABS: 0 -> Riwayat Transaksi, 1 -> Ringkasan & Analisis, 2 -> Profil & Lokasi
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Slate100,
                    contentColor = Slate900,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = Slate900,
                                height = 3.dp
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        selectedContentColor = Slate900,
                        unselectedContentColor = Slate600,
                        text = {
                            Text(
                                tr("Riwayat Tx (${transactions.size})", "Tx History (${transactions.size})", lang),
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == 0) Slate900 else Slate700
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        selectedContentColor = Slate900,
                        unselectedContentColor = Slate600,
                        text = {
                            Text(
                                tr("Analisis", "Analysis", lang),
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == 1) Slate900 else Slate700
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        selectedContentColor = Slate900,
                        unselectedContentColor = Slate600,
                        text = {
                            Text(
                                tr("Profil & GPS", "Profile & GPS", lang),
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == 2) Slate900 else Slate700
                            )
                        }
                    )
                }

                // MAIN CONTENT ACCORDING TO ACTIVE TAB
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        // TAB 0: RIWAYAT TRANSAKSI LENGKAP DENGAN FILTER & SORT
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Search Bar & Sort Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text(tr("Cari tanggal, produk, catatan...", "Search date, product, notes...", lang), fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        trailingIcon = {
                                            if (searchQuery.isNotBlank()) {
                                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        colors = appTextFieldColors(),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    )

                                    // Sort Menu Trigger
                                    Box {
                                        OutlinedButton(
                                            onClick = { showSortMenu = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.height(44.dp)
                                        ) {
                                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(tr("Urutkan", "Sort", lang), fontSize = 11.sp)
                                        }

                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false }
                                        ) {
                                            OutletTxSort.values().forEach { sortOption ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (selectedSort == sortOption) {
                                                                Icon(Icons.Default.Check, contentDescription = null, tint = Slate900, modifier = Modifier.size(14.dp))
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                            }
                                                            Text(sortOption.label, fontSize = 12.sp, fontWeight = if (selectedSort == sortOption) FontWeight.Bold else FontWeight.Normal)
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedSort = sortOption
                                                        showSortMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Filter Chips Horizontal Scroll
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutletTxFilter.values().forEach { filterOpt ->
                                        val isSelected = selectedFilter == filterOpt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedFilter = filterOpt },
                                            label = { Text(filterOpt.label, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Slate900,
                                                selectedLabelColor = Color.White,
                                                containerColor = Slate100,
                                                labelColor = Slate700
                                            ),
                                            border = null,
                                            modifier = Modifier.height(30.dp)
                                        )
                                    }
                                }

                                // Transaction List
                                if (processedTransactions.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Slate300, modifier = Modifier.size(40.dp))
                                            Text(tr("Tidak ada riwayat transaksi yang cocok", "No matching transaction history found", lang), fontSize = 12.sp, color = Slate500)
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 12.dp)
                                    ) {
                                        items(processedTransactions, key = { it.id }) { tx ->
                                            val prod = products.find { it.id == tx.productId }
                                            val satuanKecil = prod?.satuanKecil ?: "Pcs"
                                            val isTitip = tx.jenis == "TITIP_BARU"
                                            val isLunas = tx.uangDiterima >= tx.grandTotalTagihan

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedReceiptTx = tx },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = Slate50),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Header Row: Type badge, Date, Struk button
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
                                                                color = if (isTitip) BlueSurface else EmeraldSurface,
                                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isTitip) BlueBorder else EmeraldBorder)
                                                            ) {
                                                                Text(
                                                                    text = if (isTitip) "DROP TITIP" else "TARIK & SETTLE",
                                                                    color = if (isTitip) BlueAccent else EmeraldText,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = tx.tanggal,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Slate800
                                                            )
                                                        }

                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isLunas) EmeraldSurface else AmberSurface
                                                        ) {
                                                            Text(
                                                                text = if (isLunas) "Lunas" else "Ada Bon",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isLunas) EmeraldText else AmberText,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    // Product & Quantities
                                                    val qtyDititip = if (tx.restockBaruPcs > 0) tx.restockBaruPcs else tx.totalTitipanAktifPcs
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = prod?.nama ?: "Produk SKU #${tx.productId}",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Slate900
                                                            )
                                                            Text(
                                                                text = if (isTitip) "Jumlah Dititipkan: $qtyDititip $satuanKecil" else "Laku: ${tx.pcsLaku} $satuanKecil • Retur Ditarik: ${tx.bsDitarikPcs} $satuanKecil",
                                                                fontSize = 11.sp,
                                                                color = Slate600
                                                            )
                                                        }

                                                        Column(horizontalAlignment = Alignment.End) {
                                                            if (isTitip) {
                                                                val nilaiDrop = qtyDititip * tx.hargaSatuan
                                                                Text(
                                                                    text = SfaViewModel.formatRupiah(nilaiDrop),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Slate900
                                                                )
                                                                Text(
                                                                    text = "Drop Konsinyasi",
                                                                    fontSize = 10.sp,
                                                                    color = BlueAccent,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            } else {
                                                                Text(
                                                                    text = SfaViewModel.formatRupiah(tx.grandTotalTagihan),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Slate900
                                                                )
                                                                Text(
                                                                    text = "Bayar: ${SfaViewModel.formatRupiah(tx.uangDiterima)}",
                                                                    fontSize = 10.sp,
                                                                    color = if (isLunas) EmeraldSuccess else AmberWarning
                                                                )
                                                            }
                                                        }
                                                    }

                                                    if (tx.catatan.isNotBlank()) {
                                                        Text(
                                                            text = "Catatan: ${tx.catatan}",
                                                            fontSize = 10.sp,
                                                            color = Slate500,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = SfaViewModel.formatDate(tx.timestamp),
                                                            fontSize = 9.sp,
                                                            color = Slate400
                                                        )
                                                        Text(
                                                            text = "Lihat Detail Struk >",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BlueAccent
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // TAB 1: RINGKASAN & ANALISIS PERFORMA
                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Status & Aging banner
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Slate50,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "${warung.kategoriWarung} • Urutan #${warung.urutanKunjungan}",
                                                fontSize = 11.sp,
                                                color = Slate600
                                            )
                                            Text(
                                                text = "Kunjungan Terakhir:",
                                                fontSize = 10.sp,
                                                color = Slate500
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when {
                                                daysSinceVisit >= 14 -> Color(0xFFFEE2E2)
                                                daysSinceVisit >= 7 -> Color(0xFFFEF3C7)
                                                daysSinceVisit == 0 -> Color(0xFFECFDF5)
                                                else -> Slate200
                                            }
                                        ) {
                                            Text(
                                                text = when {
                                                    daysSinceVisit == 0 -> "Hari ini"
                                                    daysSinceVisit >= 900 -> "Belum Pernah"
                                                    daysSinceVisit >= 14 -> "⚠️ $daysSinceVisit hari lalu (Kritis)"
                                                    daysSinceVisit >= 7 -> "⚠️ $daysSinceVisit hari lalu (Tempo)"
                                                    else -> "$daysSinceVisit hari lalu"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    daysSinceVisit >= 14 -> RoseDanger
                                                    daysSinceVisit >= 7 -> AmberText
                                                    daysSinceVisit == 0 -> EmeraldText
                                                    else -> Slate700
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // 2. Performa Penjualan
                                Text(tr("PERFORMA PENJUALAN & OMSET", "SALES & REVENUE PERFORMANCE", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 0.5.sp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = RoundedCornerShape(10.dp), color = Slate100, modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(tr("TOTAL OMSET", "TOTAL REVENUE", lang), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                            Text(SfaViewModel.formatRupiah(totalGrossSales), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                            Text(tr("Semua Transaksi", "All Transactions", lang), fontSize = 9.sp, color = Slate500)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(10.dp), color = Slate100, modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(tr("TOTAL LAKU", "TOTAL SOLD", lang), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                            Text("$totalPcsSold Unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                            Text(tr(tr("$totalTransactionsCount x Kunjungan", "$totalTransactionsCount x Visits", lang), "$totalTransactionsCount x Visits", lang), fontSize = 9.sp, color = Slate500)
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = RoundedCornerShape(10.dp), color = Slate50, border = androidx.compose.foundation.BorderStroke(1.dp, Slate200), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(tr("RATA-RATA / KUNJUNGAN", "AVERAGE / VISIT", lang), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                            Text(SfaViewModel.formatRupiah(avgSalesPerVisit), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                                            Text(String.format(java.util.Locale.US, "%.1f unit / visit", avgPcsPerVisit), fontSize = 9.sp, color = Slate500)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(10.dp), color = Slate50, border = androidx.compose.foundation.BorderStroke(1.dp, Slate200), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(tr("KAS TERKUMPUL", "CASH COLLECTED", lang), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                            Text(SfaViewModel.formatRupiah(totalCashCollected), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                            Text(tr(tr("${collectionRatePercent.toInt()}% Terbayar Tunai", "${collectionRatePercent.toInt()}% Paid Cash", lang), "${collectionRatePercent.toInt()}% Paid in Cash", lang), fontSize = 9.sp, color = Slate500)
                                        }
                                    }
                                }

                                // 3. Smart Restock & Velocity
                                Text(tr("KECEPATAN PERPUTARAN & SARAN RESTOCK", "TURNOVER VELOCITY & RESTOCK ADVICE", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 0.5.sp)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                                                Text(tr("Smart Restock Forecast", "Smart Restock Forecast", lang), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E40AF))
                                            }
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDBEAFE)) {
                                                Text(String.format(java.util.Locale.US, "%.1f unit/hari", dailyVelocity), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(tr("Disarankan Titip (7 Hari):", "Recommended Drop (7 Days):", lang), fontSize = 10.sp, color = Slate700)
                                                Text("$suggestedRestock7Days Unit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(tr("Sisa Titipan Fisik:", "Remaining Consignment Stock:", lang), fontSize = 10.sp, color = Slate700)
                                                Text("${warung.stokTitipanPcs} Unit (${if (estimatedDaysLeft > 0) "cukup ~$estimatedDaysLeft hari" else "stok menipis"})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (warung.stokTitipanPcs <= 5) RoseDanger else Slate900)
                                            }
                                        }
                                    }
                                }

                                // 4. Piutang & BS Ratio
                                Text(tr("KESEHATAN PIUTANG & TINGKAT RETUR", "CREDIT HEALTH & RETURN RATIO", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 0.5.sp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (warung.saldoPiutang > 0) Color(0xFFFFFBEB) else Color(0xFFECFDF5),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (warung.saldoPiutang > 0) AmberBorder else EmeraldBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(tr("SALDO BON AKTIF", "ACTIVE CREDIT BALANCE", lang), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate600)
                                            Text(SfaViewModel.formatRupiah(warung.saldoPiutang), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (warung.saldoPiutang > 0) AmberWarning else EmeraldSuccess)
                                            Text(tr("Limit: ${SfaViewModel.formatRupiah(warung.limitHutangMaksimal)}", "Limit: ${SfaViewModel.formatRupiah(warung.limitHutangMaksimal)}", lang), fontSize = 9.sp, color = Slate500)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (bsRatioPercent > 5.0) Color(0xFFFFF1F2) else Slate50,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (bsRatioPercent > 5.0) RoseBorder else Slate200),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(tr("PERSENTASE RETUR", "RETURN GOODS RATE", lang), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate600)
                                            Text(String.format(java.util.Locale.US, "%.1f%% Retur", bsRatioPercent), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (bsRatioPercent > 5.0) RoseDanger else EmeraldSuccess)
                                            Text(tr("$totalBsPcs Unit Pernah Retur", "$totalBsPcs Units Ever Returned", lang), fontSize = 9.sp, color = Slate500)
                                        }
                                    }
                                }

                                // 5. Top Products
                                if (productSales.isNotEmpty()) {
                                    Text(tr("PRODUK TERLARIS DI TOKO INI", "TOP SELLING PRODUCTS IN THIS STORE", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 0.5.sp)
                                    val topPcsMax = productSales.firstOrNull()?.pcs?.coerceAtLeast(1) ?: 1
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        productSales.take(5).forEachIndexed { idx, item ->
                                            val ratio = (item.pcs.toFloat() / topPcsMax).coerceIn(0.1f, 1f)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Slate50,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Text("#${idx + 1} ${item.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text("${item.pcs} Unit (${SfaViewModel.formatRupiah(item.revenue)})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                                    }
                                                    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = if (idx == 0) AmberWarning else Slate700, trackColor = Slate200)
                                                }
                                            }
                                        }
                                    }
                                }

                                // 6. Historical trend
                                if (recentTx.isNotEmpty()) {
                                    Text(tr("TREN KUNJUNGAN TERAKHIR", "RECENT VISIT TREND", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 0.5.sp)
                                    Surface(shape = RoundedCornerShape(10.dp), color = Slate50, border = androidx.compose.foundation.BorderStroke(1.dp, Slate200), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                                recentTx.forEach { tx ->
                                                    val heightRatio = (tx.pcsLaku.toFloat() / maxTxPcs).coerceIn(0.15f, 1f)
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("${tx.pcsLaku}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                                        Box(modifier = Modifier.width(28.dp).height((40 * heightRatio).dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(if (tx.pcsLaku > 0) EmeraldSuccess else Slate300))
                                                        Text(text = tx.tanggal.takeLast(5), fontSize = 8.sp, color = Slate500)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // TAB 2: PROFIL TOKO & NAVIGASI GPS OFFLINE
                        2 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Photo if available
                                if (warung.fotoOutlet != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Slate900),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = warung.fotoOutlet,
                                            contentDescription = tr("Foto Outlet", "Outlet Photo", lang),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                // Profile Cards
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Slate50,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(tr("INFORMASI TOKO", "STORE INFORMATION", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 0.5.sp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(tr("Nama Pemilik:", "Owner Name:", lang), fontSize = 11.sp, color = Slate600)
                                            Text(warung.namaPemilik.ifBlank { "-" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(tr("Kategori:", "Category:", lang), fontSize = 11.sp, color = Slate600)
                                            Text(warung.kategoriWarung, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(tr("Urutan Kunjungan:", "Visit Order:", lang), fontSize = 11.sp, color = Slate600)
                                            Text(tr("Nomor #${warung.urutanKunjungan}", "Number #${warung.urutanKunjungan}", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(tr("Status Outlet:", "Outlet Status:", lang), fontSize = 11.sp, color = Slate600)
                                            Text(warung.status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (warung.status == "Blacklist") RoseDanger else EmeraldSuccess)
                                        }
                                    }
                                }

                                // Location & Distance Card
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Slate50,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(tr("LOKASI & JARAK REALTIME", "REAL-TIME LOCATION & DISTANCE", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 0.5.sp)

                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Slate600, modifier = Modifier.size(16.dp))
                                            Text(warung.alamatLengkap.ifEmpty { "Belum ada alamat tertulis" }, fontSize = 11.sp, color = Slate800)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Slate100,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Default.NearMe, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                                    Column {
                                                        Text(tr("Jarak dari Posisi Anda:", "Distance from Your Position:", lang), fontSize = 10.sp, color = Slate500)
                                                        Text(formattedDistance, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        com.example.util.LocationHelper.openGoogleMapsNavigation(
                                                            context = context,
                                                            lat = warung.latitude,
                                                            lng = warung.longitude,
                                                            outletName = warung.namaWarung
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(tr("Buka Maps", "Open Maps", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Koordinat: Lat ${String.format(Locale.US, "%.5f", warung.latitude)}, Lng ${String.format(Locale.US, "%.5f", warung.longitude)} (±${warung.akurasiGpsMeter}m)",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                if (warung.notes.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = AmberSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Notes, contentDescription = null, tint = AmberText, modifier = Modifier.size(16.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(tr("CATATAN OUTLET", "OUTLET NOTES", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberText)
                                                Text(warung.notes, fontSize = 11.sp, color = Slate900)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // BOTTOM ACTION BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onAiRecommendation,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(tr("AI Saran", "AI Suggestion", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onTitipBaru,
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(tr("Titip", "Consign", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onTarikSisa,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(tr("Tarik/Ganti", "Return/Exchange", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onManageCustomPrices,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(tr("Harga", "Price", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Interactive Full Receipt / Faktur Thermal Dialog (Sama persis dengan Struk di Laporan)
    selectedReceiptTx?.let { tx ->
        val prod = products.find { it.id == tx.productId }
        ReceiptDialog(
            transaction = tx,
            warung = warung,
            product = prod,
            userProfile = userProfile,
            onDismiss = { selectedReceiptTx = null }
        )
    }
}

// 12. GPS TOOL DIALOG
@Composable
fun GpsToolDialog(onDismiss: () -> Unit) {
    val lang = LocalAppLanguage.current

val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentLoc by remember { mutableStateOf(com.example.util.LocationHelper.getInstantLocation(context)) }
    var isRefreshing by remember { mutableStateOf(false) }
    var addressText by remember(currentLoc) {
        mutableStateOf(
            if (currentLoc.isAvailable) {
                com.example.util.LocationHelper.reverseGeocode(context, currentLoc.latitude, currentLoc.longitude)
            } else {
                "Sensor GPS aktif (mencari sinyal satelit...)"
            }
        )
    }

    val refreshLocation = {
        isRefreshing = true
        addressText = "📡 Mencari dan mengunci satelit GPS GNSS (Mode Offline)..."
        coroutineScope.launch(Dispatchers.IO) {
            val freshLoc = com.example.util.LocationHelper.acquireFreshSatelliteFix(context, maxTimeoutMs = 12000L, targetAccuracyMeters = 20f)
            val addr = if (freshLoc.isAvailable) {
                com.example.util.LocationHelper.reverseGeocode(context, freshLoc.latitude, freshLoc.longitude)
            } else {
                "Sensor GPS aktif (mencari sinyal satelit di ruang terbuka...)"
            }
            withContext(Dispatchers.Main) {
                currentLoc = freshLoc
                addressText = addr
                isRefreshing = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshLocation()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(40.dp))
                Text(tr("GPS Sensor Terkunci", "GPS Sensor Locked", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Koordinat:", "Coordinates:", lang), fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                            Text(
                                text = String.format(Locale.US, "%.5f, %.5f", currentLoc.latitude, currentLoc.longitude),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Akurasi Sensor:", "Sensor Accuracy:", lang), fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (currentLoc.isAvailable) "${currentLoc.accuracyMeter.toInt().coerceAtLeast(3)} meter (Akurat)" else "Mencari Sinyal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentLoc.isAvailable) EmeraldSuccess else AmberWarning
                            )
                        }
                        HorizontalDivider(color = Slate200)
                        Text(
                            text = "Wilayah / Alamat: $addressText",
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Kunci Ulang", "Relock", lang), fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(tr("Tutup", "Close", lang), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// 13. USER PROFILE / IDENTITAS SALESMAN DIALOG
@Composable
fun UserProfileDialog(
    currentProfile: UserProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (UserProfileEntity) -> Unit
) {
    val lang = LocalAppLanguage.current

var namaSalesman by remember { mutableStateOf(currentProfile?.namaSalesman ?: "") }
    var noHp by remember { mutableStateOf(currentProfile?.noHp ?: "") }
    var namaDistributor by remember { mutableStateOf(currentProfile?.namaDistributor ?: "") }
    var alamatDepo by remember { mutableStateOf(currentProfile?.alamatDepo ?: "") }
    var platNomorMobil by remember { mutableStateOf(currentProfile?.platNomorMobil ?: "") }
    var areaRayon by remember { mutableStateOf(currentProfile?.areaOperasional ?: "") }
    var pinKeamanan by remember { mutableStateOf(currentProfile?.pinKeamanan ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = Slate900, modifier = Modifier.size(24.dp))
                        Text(
                            text = if (currentProfile == null) "Registrasi Akun Sales" else "Profil & Identitas Sales",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "Identitas ini disimpan lokal di HP Anda dan otomatis dicetak pada kepala struk nota transaksi.",
                    fontSize = 11.sp,
                    color = Slate600
                )

                HorizontalDivider(color = Slate200)

                OutlinedTextField(
                    value = namaSalesman,
                    onValueChange = { namaSalesman = it },
                    label = { Text(tr("Nama Lengkap Salesman *", "Full Salesman Name *", lang)) },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                OutlinedTextField(
                    value = noHp,
                    onValueChange = { noHp = it },
                    label = { Text(tr("Nomor WhatsApp / HP *", "WhatsApp / Phone Number *", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                OutlinedTextField(
                    value = namaDistributor,
                    onValueChange = { namaDistributor = it },
                    label = { Text(tr("Nama Distributor / Agen / Usaha", "Distributor / Agency / Business Name", lang)) },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                OutlinedTextField(
                    value = alamatDepo,
                    onValueChange = { alamatDepo = it },
                    label = { Text(tr("Alamat Depo / Gudang", "Depot / Warehouse Address", lang)) },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = platNomorMobil,
                        onValueChange = { platNomorMobil = it },
                        label = { Text(tr("Plat Nomor Mobil/Motor", "Vehicle License Plate", lang)) },
                        colors = appTextFieldColors(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = areaRayon,
                        onValueChange = { areaRayon = it },
                        label = { Text(tr("Area / Rayon", "Area / Region", lang)) },
                        colors = appTextFieldColors(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = pinKeamanan,
                    onValueChange = { pinKeamanan = it },
                    label = { Text(tr("PIN Keamanan Utilitas (Opsional)", "Security PIN for Utilities (Optional)", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(tr("Batal", "Cancel", lang))
                    }
                    Button(
                        onClick = {
                            if (namaSalesman.isNotBlank()) {
                                onSave(
                                    UserProfileEntity(
                                        namaSalesman = namaSalesman.trim(),
                                        noHp = noHp.trim(),
                                        namaDistributor = namaDistributor.trim(),
                                        alamatDepo = alamatDepo.trim(),
                                        platNomorMobil = platNomorMobil.trim().uppercase(),
                                        areaOperasional = areaRayon.trim(),
                                        pinKeamanan = pinKeamanan.trim(),
                                        isConfigured = true
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text(tr("Simpan Profil", "Save Profile", lang))
                    }
                }
            }
        }
    }
}

// 14. MANAGE CUSTOM PRICES PER TOKO DIALOG
@Composable
fun ManageCustomPricesDialog(
    warung: WarungEntity,
    products: List<ProductEntity>,
    customPrices: List<WarungCustomPriceEntity>,
    onDismiss: () -> Unit,
    onSaveCustomPrice: (productId: String, customPrice: Double) -> Unit,
    onDeleteCustomPrice: (productId: String) -> Unit
) {
    val lang = LocalAppLanguage.current

var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var priceInput by remember { mutableStateOf("") }

    val warungPrices = customPrices.filter { it.warungId == warung.id }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Harga Khusus Toko",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = warung.namaWarung,
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "Atur harga jual khusus untuk toko ini jika berbeda dari harga standar katalog.",
                    fontSize = 11.sp,
                    color = Slate600
                )

                HorizontalDivider(color = Slate200)

                // Sub-dialog or inline editor for editing specific product price
                if (editingProduct != null) {
                    val p = editingProduct!!
                    val currentCustom = warungPrices.find { it.productId == p.id }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(tr(tr("Atur Harga: ${p.nama}", "Set Price: ${p.nama}", lang), "Set Price: ${p.nama}", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(tr("Harga Katalog Standar: ${SfaViewModel.formatRupiah(p.hargaJualDefault)}/Pcs", "Standard Catalog Price: ${SfaViewModel.formatRupiah(p.hargaJualDefault)}/Pcs", lang), fontSize = 11.sp, color = Slate600)

                            OutlinedTextField(
                                value = priceInput,
                                onValueChange = { priceInput = it },
                                label = { Text(tr("Harga Khusus Toko (Rp/Pcs)", "Store Special Price (Rp/Pcs)", lang)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (currentCustom != null) {
                                    OutlinedButton(
                                        onClick = {
                                            onDeleteCustomPrice(p.id)
                                            editingProduct = null
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseDanger),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(tr("Hapus Khusus", "Remove Special", lang), fontSize = 11.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { editingProduct = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(tr("Batal", "Cancel", lang), fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        val price = priceInput.toDoubleOrNull()
                                        if (price != null && price > 0) {
                                            onSaveCustomPrice(p.id, price)
                                            editingProduct = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(tr("Simpan", "Save", lang), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // List of Products and their custom price status
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    products.forEach { p ->
                        val custom = warungPrices.find { it.productId == p.id }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (custom != null) EmeraldSurface else Slate50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (custom != null) EmeraldBorder else Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.nama, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                                    if (custom != null) {
                                        Text(
                                            tr("Harga Khusus: ${SfaViewModel.formatRupiah(custom.hargaJualPcs)}/Pcs (Standar: ${SfaViewModel.formatRupiah(p.hargaJualDefault)})", "Special Price: ${SfaViewModel.formatRupiah(custom.hargaJualPcs)}/Pcs (Standard: ${SfaViewModel.formatRupiah(p.hargaJualDefault)})", lang),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldText
                                        )
                                    } else {
                                        Text(
                                            tr("Harga Standar: ${SfaViewModel.formatRupiah(p.hargaJualDefault)}/Pcs", "Standard Price: ${SfaViewModel.formatRupiah(p.hargaJualDefault)}/Pcs", lang),
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        editingProduct = p
                                        priceInput = (custom?.hargaJualPcs ?: p.hargaJualDefault).toLong().toString()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (custom != null) EmeraldSuccess else Slate900
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (custom != null) "Ubah" else "Set Khusus", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(tr("Selesai", "Finish", lang))
                }
            }
        }
    }
}

// 15. EKSPOR BACKUP & MIGRASI HP DIALOG (ZIP + FOTO & JSON)
@Composable
fun ExportBackupDialog(
    viewModel: SfaViewModel,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current

val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Paket Migrasi HP (.ZIP), 1: Berkas JSON Modular
    var copied by remember { mutableStateOf(false) }

    // Modular Checkboxes
    var exportProfile by remember { mutableStateOf(true) }
    var exportProducts by remember { mutableStateOf(true) }
    var exportWarungs by remember { mutableStateOf(true) }
    var exportRutes by remember { mutableStateOf(true) }
    var exportPabriks by remember { mutableStateOf(true) }
    var exportCustomPrices by remember { mutableStateOf(true) }
    var exportTransactions by remember { mutableStateOf(true) }
    var exportInventory by remember { mutableStateOf(true) }
    var exportPhotos by remember { mutableStateOf(true) }

    var isGenerating by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<com.example.util.ExportResult?>(null) }
    var zipResult by remember { mutableStateOf<com.example.util.ZipBackupResult?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // SAF Document Creation Launchers
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            zipResult?.zipFile?.let { zipFile ->
                viewModel.copyBackupToSaf(zipFile, targetUri) { success ->
                    if (success) {
                        feedbackMessage = "Paket Migrasi (.ZIP) berhasil disimpan ke folder HP Anda!"
                    }
                }
            }
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            exportResult?.file?.let { jsonFile ->
                viewModel.copyBackupToSaf(jsonFile, targetUri) { success ->
                    if (success) {
                        feedbackMessage = "Berkas Cadangan (.JSON) berhasil disimpan ke folder HP Anda!"
                    }
                }
            }
        }
    }

    val generateZip = {
        isGenerating = true
        feedbackMessage = null
        val selection = com.example.util.BackupSelection(
            exportProfile = true,
            exportProducts = true,
            exportWarungs = true,
            exportRutes = true,
            exportPabriks = true,
            exportCustomPrices = true,
            exportTransactions = true,
            exportInventory = true,
            exportPhotos = exportPhotos
        )
        viewModel.exportZipBackup(selection) { result ->
            zipResult = result
            isGenerating = false
        }
    }

    val generateJson = {
        isGenerating = true
        feedbackMessage = null
        val selection = com.example.util.BackupSelection(
            exportProfile = exportProfile,
            exportProducts = exportProducts,
            exportWarungs = exportWarungs,
            exportRutes = exportRutes,
            exportPabriks = exportPabriks,
            exportCustomPrices = exportCustomPrices,
            exportTransactions = exportTransactions,
            exportInventory = exportInventory,
            exportPhotos = false
        )
        viewModel.exportModularBackup(selection) { result ->
            exportResult = result
            isGenerating = false
            copied = false
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && zipResult == null) {
            generateZip()
        } else if (selectedTab == 1 && exportResult == null) {
            generateJson()
        }
    }

    LaunchedEffect(exportPhotos) {
        if (selectedTab == 0) {
            generateZip()
        }
    }

    LaunchedEffect(exportProfile, exportProducts, exportWarungs, exportRutes, exportPabriks, exportCustomPrices, exportTransactions, exportInventory) {
        if (selectedTab == 1) {
            generateJson()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                        Text(
                            text = "Ekspor Cadangan & Migrasi HP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500, modifier = Modifier.size(18.dp))
                    }
                }

                // Format Selector Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Slate100,
                    contentColor = Slate900,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Slate900,
                                height = 3.dp
                            )
                        }
                    },
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        selectedContentColor = Slate900,
                        unselectedContentColor = Slate600,
                        text = {
                            Text(
                                tr("📦 Paket Migrasi (.ZIP)", "📦 Migration Package (.ZIP)", lang),
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) Slate900 else Slate700
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        selectedContentColor = Slate900,
                        unselectedContentColor = Slate600,
                        text = {
                            Text(
                                tr("📄 File Modular (.JSON)", "📄 Modular File (.JSON)", lang),
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) Slate900 else Slate700
                            )
                        }
                    )
                }

                if (feedbackMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            Text(feedbackMessage!!, fontSize = 11.sp, color = EmeraldText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (selectedTab == 0) {
                    // --- TAB 1: PAKET MIGRASI HP (.ZIP) ---
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text(tr("Paket Lengkap Pindah HP", "Full Migration Package", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                                    Text(tr("Seluruh database & foto toko dikemas utuh", "Full database & store photos bundled", lang), fontSize = 10.sp, color = Slate600)
                                }
                            }

                            HorizontalDivider(color = Slate200)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportPhotos = !exportPhotos }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = exportPhotos, onCheckedChange = { exportPhotos = it })
                                Column {
                                    Text(tr("Sertakan Semua Foto Outlet / Toko", "Include All Outlet / Store Photos", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                                    Text(tr("Foto akan otomatis diekstrak saat dipulihkan di HP baru", "Photos will be automatically extracted on new device", lang), fontSize = 10.sp, color = Slate500)
                                }
                            }

                            if (zipResult != null && !isGenerating) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(tr("Nama File:", "File Name:", lang), fontSize = 11.sp, color = Slate600)
                                            Text(zipResult!!.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(tr("Ukuran Paket:", "Package Size:", lang), fontSize = 11.sp, color = Slate600)
                                            Text(zipResult!!.fileSizeFormatted, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(tr("Jumlah Foto Toko:", "Store Photos Count:", lang), fontSize = 11.sp, color = Slate600)
                                            Text(tr("${zipResult!!.photoCount} Foto", "${zipResult!!.photoCount} Photos", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isGenerating) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Slate900)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("Mengompres database & foto ke format .ZIP...", "Compressing database & photos to .ZIP format...", lang), fontSize = 11.sp, color = Slate700)
                        }
                    } else if (zipResult != null) {
                        // Actions for ZIP
                        Button(
                            onClick = {
                                createZipLauncher.launch(zipResult!!.fileName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Simpan Berkas .ZIP ke HP (Download)", "Save .ZIP File to Phone (Download)", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                com.example.util.BackupRestoreHelper.shareBackupFile(
                                    context = context,
                                    file = zipResult!!.zipFile,
                                    mimeType = "application/zip",
                                    chooserTitle = "Kirim Paket Migrasi HP SFA (.ZIP)"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Kirim Berkas .ZIP (WhatsApp / Drive / Share)", "Send .ZIP File (WhatsApp / Drive / Share)", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // --- TAB 2: BERKAS CADANGAN JSON MODULAR ---
                    Text(
                        text = "Pilih entitas data yang ingin disertakan ke file cadangan JSON:",
                        fontSize = 11.sp,
                        color = Slate600
                    )

                    // Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = exportWarungs && exportProducts && exportPabriks && exportRutes && exportTransactions && exportProfile,
                            onClick = {
                                exportProfile = true
                                exportProducts = true
                                exportWarungs = true
                                exportRutes = true
                                exportPabriks = true
                                exportCustomPrices = true
                                exportTransactions = true
                                exportInventory = true
                            },
                            label = { Text(tr("Semua Data", "All Data", lang), fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Slate900, selectedLabelColor = Color.White, containerColor = Slate100, labelColor = Slate700),
                            border = null
                        )

                        FilterChip(
                            selected = exportWarungs && !exportProducts && !exportPabriks && !exportTransactions,
                            onClick = {
                                exportWarungs = true
                                exportRutes = true
                                exportCustomPrices = true
                                exportProducts = false
                                exportPabriks = false
                                exportTransactions = false
                                exportInventory = false
                                exportProfile = false
                            },
                            label = { Text(tr("Hanya Outlet & GPS", "Outlets & GPS Only", lang), fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Slate900, selectedLabelColor = Color.White, containerColor = Slate100, labelColor = Slate700),
                            border = null
                        )

                        FilterChip(
                            selected = !exportWarungs && exportProducts && exportPabriks && !exportTransactions,
                            onClick = {
                                exportWarungs = false
                                exportRutes = false
                                exportCustomPrices = false
                                exportProducts = true
                                exportPabriks = true
                                exportTransactions = false
                                exportInventory = false
                                exportProfile = false
                            },
                            label = { Text(tr("Produk & Pabrik", "Products & Factories", lang), fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Slate900, selectedLabelColor = Color.White, containerColor = Slate100, labelColor = Slate700),
                            border = null
                        )
                    }

                    // Checkboxes
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportWarungs = !exportWarungs }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = exportWarungs, onCheckedChange = { exportWarungs = it })
                                Text(tr("Data Master Outlet / Warung & GPS", "Outlet Master Data & GPS", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportProducts = !exportProducts }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = exportProducts, onCheckedChange = { exportProducts = it })
                                Text(tr("Data Master Produk & Rasio Konversi", "Product Master Data & Conversion", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportPabriks = !exportPabriks }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = exportPabriks, onCheckedChange = { exportPabriks = it })
                                Text(tr("Data Supplier & Pabrik", "Supplier & Factory Data", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportRutes = !exportRutes }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = exportRutes, onCheckedChange = { exportRutes = it })
                                Text(tr("Data Rute / Jalur Kunjungan", "Visit Route Master Data", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportTransactions = !exportTransactions }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = exportTransactions, onCheckedChange = { exportTransactions = it })
                                Text(tr("Riwayat Transaksi Harian", "Daily Transaction History", lang), fontSize = 11.sp, color = Slate700)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportInventory = !exportInventory }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = exportInventory, onCheckedChange = { exportInventory = it })
                                Text(tr("Laci Stok Mobil & Muat Pagi", "Vehicle Stock Drawer & Morning Load", lang), fontSize = 11.sp, color = Slate700)
                            }
                        }
                    }

                    if (exportResult != null) {
                        Text(
                            text = exportResult!!.summary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldText
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate900,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp)
                        ) {
                            Text(
                                text = exportResult!!.jsonString,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }

                        // JSON Action Buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    createJsonLauncher.launch(exportResult!!.fileName)
                                },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Simpan .JSON", "Save .JSON", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    exportResult?.file?.let { f ->
                                        com.example.util.BackupRestoreHelper.shareBackupFile(
                                            context = context,
                                            file = f,
                                            mimeType = "application/json",
                                            chooserTitle = "Bagikan Berkas Cadangan JSON"
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1.1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Kirim File", "Send File", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText(tr("SFA_BACKUP", "SFA_BACKUP", lang), exportResult!!.jsonString)
                                    clipboard.setPrimaryClip(clip)
                                    copied = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (copied) EmeraldSuccess else Slate800),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (copied) "Tersalin" else "Salin Teks", fontSize = 10.sp)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(tr("Tutup", "Close", lang))
                }
            }
        }
    }
}

// 16. IMPOR & PULIHKAN CADANGAN DIALOG (ZIP + FOTO & JSON)
@Composable
fun ImportBackupDialog(
    viewModel: SfaViewModel,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current

val context = LocalContext.current
    var jsonInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // User Selections for Import
    var importWarungs by remember { mutableStateOf(true) }
    var importProducts by remember { mutableStateOf(true) }
    var importPabriks by remember { mutableStateOf(true) }
    var importRutes by remember { mutableStateOf(true) }
    var importProfile by remember { mutableStateOf(true) }
    var importTransactions by remember { mutableStateOf(true) }
    var importCustomPrices by remember { mutableStateOf(true) }
    var importInventory by remember { mutableStateOf(true) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            isProcessing = true
            errorMessage = null
            successMessage = null

            val selection = com.example.util.BackupSelection(
                exportProfile = importProfile,
                exportProducts = importProducts,
                exportWarungs = importWarungs,
                exportRutes = importRutes,
                exportPabriks = importPabriks,
                exportCustomPrices = importCustomPrices,
                exportTransactions = importTransactions,
                exportInventory = importInventory,
                exportPhotos = true
            )

            viewModel.importBackupFromUri(pickedUri, selection) { result ->
                isProcessing = false
                if (result.success) {
                    successMessage = result.message
                } else {
                    errorMessage = result.message
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Slate900
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                        Text(
                            text = "Pulihkan Cadangan (Restore)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close", lang), tint = Slate500, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "Pilih berkas cadangan (.ZIP paket lengkap atau .JSON) yang didapat dari HP lama atau backup sebelumnya:",
                    fontSize = 11.sp,
                    color = Slate600
                )

                // Main Action Button for ZIP / JSON File Selection
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Mengekstrak Foto & Memulihkan...", "Extracting Photos & Restoring...", lang), fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Pilih Berkas .ZIP / .JSON dari HP", "Select .ZIP / .JSON File from Phone", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (successMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                Text(tr("Pemulihan Berhasil!", "Restoration Successful!", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldText)
                            }
                            Text(successMessage!!, fontSize = 11.sp, color = Slate700)
                        }
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoseSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(18.dp))
                            Text(errorMessage!!, fontSize = 11.sp, color = RoseDanger, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // Optional Module Filter
                Text(tr("Opsi Kategori yang Dipulihkan:", "Restored Category Options:", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { importWarungs = !importWarungs },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importWarungs, onCheckedChange = { importWarungs = it })
                        Text(tr("Outlet / Warung & Titik GPS", "Outlets & GPS Points", lang), fontSize = 11.sp, color = Slate800)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { importProducts = !importProducts },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importProducts, onCheckedChange = { importProducts = it })
                        Text(tr("Master Produk & Harga Satuan", "Product Master & Unit Price", lang), fontSize = 11.sp, color = Slate800)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { importRutes = !importRutes },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importRutes, onCheckedChange = { importRutes = it })
                        Text(tr("Daftar Rute Harian", "Daily Route List", lang), fontSize = 11.sp, color = Slate800)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { importPabriks = !importPabriks },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importPabriks, onCheckedChange = { importPabriks = it })
                        Text(tr("Data Pabrik & Supplier", "Factory & Supplier Data", lang), fontSize = 11.sp, color = Slate800)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { importTransactions = !importTransactions },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importTransactions, onCheckedChange = { importTransactions = it })
                        Text(tr("Riwayat Kunjungan & Transaksi", "Visit & Transaction History", lang), fontSize = 11.sp, color = Slate800)
                    }
                }

                HorizontalDivider(color = Slate200)

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = {
                        jsonInput = it
                        errorMessage = null
                    },
                    label = { Text(tr("Atau Tempel (Paste) Teks JSON Cadangan", "Or Paste JSON Backup Text", lang)) },
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 130.dp),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                )

                if (jsonInput.isNotBlank()) {
                    Button(
                        onClick = {
                            val selection = com.example.util.BackupSelection(
                                exportProfile = importProfile,
                                exportProducts = importProducts,
                                exportWarungs = importWarungs,
                                exportRutes = importRutes,
                                exportPabriks = importPabriks,
                                exportCustomPrices = importCustomPrices,
                                exportTransactions = importTransactions,
                                exportInventory = importInventory
                            )
                            viewModel.importModularBackup(jsonInput.trim(), selection) {
                                successMessage = "Data JSON berhasil dipulihkan!"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Impor Teks JSON", "Import JSON Text", lang), fontSize = 11.sp)
                    }
                }

                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Tutup", "Close", lang))
                }
            }
        }
    }
}
