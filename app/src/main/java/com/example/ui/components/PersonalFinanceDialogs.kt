package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.PersonalAccountEntity
import com.example.data.local.entity.PersonalDebtEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.SfaViewModel
import com.example.util.AppStrings
import com.example.util.LocalAppLanguage
import java.util.UUID

// -------------------------------------------------------------------------------------------------
// 1. ADD / EDIT PERSONAL ACCOUNT DIALOG (Dompet Tunai, Bank, E-Wallet, Paylater)
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPersonalAccountDialog(
    account: PersonalAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (PersonalAccountEntity) -> Unit
) {
    val lang = LocalAppLanguage.current
    var namaAkun by remember { mutableStateOf(account?.namaAkun ?: "") }
    var tipeAkun by remember { mutableStateOf(account?.tipeAkun ?: "BANK") }
    var saldoStr by remember { mutableStateOf(if (account != null) account.saldo.toLong().toString() else "") }
    var nomorRekening by remember { mutableStateOf(account?.nomorRekening ?: "") }
    var catatan by remember { mutableStateOf(account?.catatan ?: "") }
    var isPaylater by remember { mutableStateOf(account?.isPaylater ?: false) }
    var limitKreditStr by remember { mutableStateOf(if (account != null && account.limitKredit > 0) account.limitKredit.toLong().toString() else "") }
    var tanggalJatuhTempoStr by remember { mutableStateOf(if (account != null && account.tanggalJatuhTempo > 0) account.tanggalJatuhTempo.toString() else "25") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp)),
            color = Color.White
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
                    Text(
                        text = if (account == null) AppStrings.tr("Tambah Akun Keuangan", "Add Financial Account", lang) else AppStrings.tr("Edit Akun Keuangan", "Edit Account", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                // Tipe Akun Selector
                Text(AppStrings.tr("Jenis Akun / Fasilitas:", "Account / Facility Type:", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "CASH" to AppStrings.tr("💵 Uang Tunai", "💵 Cash", lang),
                        "BANK" to AppStrings.tr("🏦 Rekening Bank", "🏦 Bank Account", lang),
                        "EWALLET" to AppStrings.tr("📱 E-Wallet", "📱 E-Wallet", lang),
                        "PAYLATER" to AppStrings.tr("💳 PayLater / Kartu", "💳 PayLater / Card", lang)
                    ).forEach { (tipe, label) ->
                        val isSelected = tipeAkun == tipe
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                tipeAkun = tipe
                                isPaylater = (tipe == "PAYLATER")
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (tipe == "PAYLATER") AmberWarning else Slate900,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700
                            )
                        )
                    }
                }

                // Nama Akun Input
                OutlinedTextField(
                    value = namaAkun,
                    onValueChange = { namaAkun = it },
                    label = { Text(AppStrings.tr("Nama Akun / Dompet", "Account Name", lang)) },
                    placeholder = { Text(if (isPaylater) "Shopee PayLater, Kredivo, CC BCA" else "Dompet Kas, Bank Mandiri, GoPay") },
                    modifier = Modifier.fillMaxWidth().testTag("input_nama_akun"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Saldo / Tagihan Input
                OutlinedTextField(
                    value = saldoStr,
                    onValueChange = { saldoStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isPaylater) AppStrings.tr("Tagihan Pemakaian Saat Ini (Rp)", "Current Used Amount (Rp)", lang) else AppStrings.tr("Saldo Tersedia Saat Ini (Rp)", "Current Available Balance (Rp)", lang)) },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth().testTag("input_saldo_akun"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Paylater specific fields: Limit Kredit & Tanggal Jatuh Tempo
                if (isPaylater) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "⚙️ ${AppStrings.tr("Pengaturan Paylater & Tagihan", "Paylater & Billing Setup", lang)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )

                            OutlinedTextField(
                                value = limitKreditStr,
                                onValueChange = { limitKreditStr = it.filter { ch -> ch.isDigit() } },
                                label = { Text(AppStrings.tr("Total Pagu / Limit Kredit (Rp)", "Total Approved Credit Limit (Rp)", lang)) },
                                placeholder = { Text("Contoh: 3000000") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = tanggalJatuhTempoStr,
                                onValueChange = { tanggalJatuhTempoStr = it.filter { ch -> ch.isDigit() }.take(2) },
                                label = { Text(AppStrings.tr("Tanggal Jatuh Tempo Bulanan (1-31)", "Monthly Due Date (Day 1-31)", lang)) },
                                placeholder = { Text("Contoh: 25") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Optional: Nomor Rekening
                OutlinedTextField(
                    value = nomorRekening,
                    onValueChange = { nomorRekening = it },
                    label = { Text(AppStrings.tr("Nomor Rekening / No HP / Kartu (Opsional)", "Account Number / Phone (Optional)", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Catatan
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text(AppStrings.tr("Catatan / Keterangan (Opsional)", "Notes (Optional)", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )

                // Submit Button
                Button(
                    onClick = {
                        if (namaAkun.isNotBlank()) {
                            val accId = account?.id ?: "ACC_${UUID.randomUUID().toString().take(8)}"
                            val saldoVal = saldoStr.toDoubleOrNull() ?: 0.0
                            val limitVal = limitKreditStr.toDoubleOrNull() ?: 0.0
                            val jthTempo = tanggalJatuhTempoStr.toIntOrNull() ?: 0
                            onSave(
                                PersonalAccountEntity(
                                    id = accId,
                                    namaAkun = namaAkun.trim(),
                                    tipeAkun = tipeAkun,
                                    saldo = saldoVal,
                                    nomorRekening = nomorRekening.trim(),
                                    catatan = catatan.trim(),
                                    isPaylater = isPaylater,
                                    limitKredit = limitVal,
                                    tanggalJatuhTempo = jthTempo
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_simpan_akun"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
                    enabled = namaAkun.isNotBlank()
                ) {
                    Text(AppStrings.tr("Simpan Akun Keuangan", "Save Financial Account", lang), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. ADD PERSONAL EXPENSE / INCOME / TRANSFER DIALOG
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalExpenseDialog(
    defaultJenis: String,
    accounts: List<PersonalAccountEntity>,
    onDismiss: () -> Unit,
    onSubmit: (jenis: String, kategori: String, nominal: Double, accountId: String, toAccountId: String?, judul: String, catatan: String) -> Unit
) {
    val lang = LocalAppLanguage.current
    var jenis by remember { mutableStateOf(defaultJenis) } // "PENGELUARAN", "PEMASUKAN", "TRANSFER"
    var nominalStr by remember { mutableStateOf("") }
    var judul by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("Makan & Minum") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedToAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var catatan by remember { mutableStateOf("") }

    val expenseCategories = listOf(
        "Makan & Minum",
        "Bensin & Transport",
        "Belanja Harian",
        "Pulsa & Kuota",
        "Tagihan & Listrik",
        "Keluarga & Anak",
        "Servis Motor/Mobil",
        "Cicilan / Paylater",
        "Kebutuhan Pribadi",
        "Lainnya"
    )

    val incomeCategories = listOf(
        "Gaji & Komisi Sales",
        "Bonus Penjualan",
        "Sampingan / Dagang",
        "Hadiah / Kiriman",
        "Lainnya"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp)),
            color = Color.White
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
                    Text(
                        text = AppStrings.tr("Catat Keuangan Pribadi", "Log Personal Transaction", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                // Jenis Transaksi Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "PENGELUARAN" to AppStrings.tr("🔴 Pengeluaran", "🔴 Expense", lang),
                        "PEMASUKAN" to AppStrings.tr("🟢 Pemasukan", "🟢 Income", lang),
                        "TRANSFER" to AppStrings.tr("🔄 Transfer", "🔄 Transfer", lang)
                    ).forEach { (jns, label) ->
                        val isSelected = jenis == jns
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                jenis = jns
                                kategori = if (jns == "PEMASUKAN") incomeCategories.first() else expenseCategories.first()
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (jns == "PENGELUARAN") RoseCritical else if (jns == "PEMASUKAN") EmeraldSuccess else Slate900,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700
                            )
                        )
                    }
                }

                // Nominal Input
                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = { nominalStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(AppStrings.tr("Nominal (Rp)", "Amount (Rp)", lang)) },
                    placeholder = { Text("Contoh: 50000") },
                    modifier = Modifier.fillMaxWidth().testTag("input_nominal_expense"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Judul / Keterangan Singkat
                OutlinedTextField(
                    value = judul,
                    onValueChange = { judul = it },
                    label = { Text(AppStrings.tr("Keterangan Singkat", "Short Description", lang)) },
                    placeholder = { Text(if (jenis == "PENGELUARAN") "Contoh: Bensin motor keliling warung" else "Contoh: Komisi mingguan masuk") },
                    modifier = Modifier.fillMaxWidth().testTag("input_judul_expense"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Kategori Selector (If not Transfer)
                if (jenis != "TRANSFER") {
                    Text(AppStrings.tr("Pilih Kategori:", "Category:", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val activeCategories = if (jenis == "PEMASUKAN") incomeCategories else expenseCategories
                        activeCategories.forEach { kat ->
                            val isSelected = kategori == kat
                            FilterChip(
                                selected = isSelected,
                                onClick = { kategori = kat },
                                label = { Text(kat, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Slate800,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100,
                                    labelColor = Slate700
                                )
                            )
                        }
                    }
                }

                // Sumber Akun / Dompet
                Text(
                    text = if (jenis == "TRANSFER") AppStrings.tr("Dari Akun Asal:", "From Account:", lang) else AppStrings.tr("Sumber Akun / Dompet:", "Payment Account / Wallet:", lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accounts.forEach { acc ->
                        val isSelected = selectedAccountId == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccountId = acc.id },
                            label = {
                                Text(
                                    "${acc.namaAkun} (${SfaViewModel.formatRupiah(acc.saldo)})",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (acc.isPaylater) AmberWarning else Slate900,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700
                            )
                        )
                    }
                }

                // If Transfer: Target Akun
                if (jenis == "TRANSFER") {
                    Text(AppStrings.tr("Ke Akun Tujuan:", "To Target Account:", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            val isSelected = selectedToAccountId == acc.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedToAccountId = acc.id },
                                label = {
                                    Text(
                                        "${acc.namaAkun} (${SfaViewModel.formatRupiah(acc.saldo)})",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSuccess,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100,
                                    labelColor = Slate700
                                )
                            )
                        }
                    }
                }

                // Catatan tambahan
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text(AppStrings.tr("Catatan Detail (Opsional)", "Notes (Optional)", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )

                // Submit Button
                val nominalVal = nominalStr.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (nominalVal > 0 && selectedAccountId.isNotBlank()) {
                            onSubmit(
                                jenis,
                                if (jenis == "TRANSFER") "Transfer Antar Akun" else kategori,
                                nominalVal,
                                selectedAccountId,
                                if (jenis == "TRANSFER") selectedToAccountId else null,
                                judul.ifBlank { kategori },
                                catatan.trim()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_simpan_expense"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (jenis == "PENGELUARAN") RoseCritical else if (jenis == "PEMASUKAN") EmeraldSuccess else Slate900,
                        contentColor = Color.White
                    ),
                    enabled = nominalVal > 0 && selectedAccountId.isNotBlank()
                ) {
                    Text(
                        text = if (jenis == "PENGELUARAN") AppStrings.tr("Catat Pengeluaran", "Record Expense", lang) else if (jenis == "PEMASUKAN") AppStrings.tr("Catat Pemasukan", "Record Income", lang) else AppStrings.tr("Eksekusi Transfer", "Execute Transfer", lang),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 3. ADD / EDIT PERSONAL DEBT DIALOG (Catatan Hutang Teman / Keluarga / Diri Sendiri)
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPersonalDebtDialog(
    debt: PersonalDebtEntity?,
    onDismiss: () -> Unit,
    onSave: (PersonalDebtEntity) -> Unit
) {
    val lang = LocalAppLanguage.current
    var jenis by remember { mutableStateOf(debt?.jenis ?: "PIUTANG_SAYA") } // "PIUTANG_SAYA", "HUTANG_SAYA"
    var namaPihak by remember { mutableStateOf(debt?.namaPihak ?: "") }
    var hubungan by remember { mutableStateOf(debt?.hubungan ?: "Teman") }
    var kontak by remember { mutableStateOf(debt?.kontak ?: "") }
    var totalNominalStr by remember { mutableStateOf(if (debt != null) debt.totalNominal.toLong().toString() else "") }
    var sisaNominalStr by remember { mutableStateOf(if (debt != null) debt.sisaNominal.toLong().toString() else "") }
    var tanggalPinjam by remember { mutableStateOf(debt?.tanggalPinjam ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) }
    var tanggalJatuhTempo by remember { mutableStateOf(debt?.tanggalJatuhTempo ?: "") }
    var catatan by remember { mutableStateOf(debt?.catatan ?: "") }

    val hubunganList = listOf("Teman", "Keluarga", "Rekan Kerja", "Tetangga", "Lainnya")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp)),
            color = Color.White
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
                    Text(
                        text = if (debt == null) AppStrings.tr("Catat Hutang / Piutang", "Add Debt / Loan", lang) else AppStrings.tr("Edit Catatan Hutang", "Edit Debt", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                // Mode Selector: Dia pinjam ke kita VS Kita hutang ke dia
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPiutang = jenis == "PIUTANG_SAYA"
                    FilterChip(
                        selected = isPiutang,
                        onClick = { jenis = "PIUTANG_SAYA" },
                        label = { Text(AppStrings.tr("Teman/Keluarga Pinjam (Piutang)", "They Borrow (Owed to Me)", lang), fontSize = 11.sp, fontWeight = if (isPiutang) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB),
                            selectedLabelColor = Color.White,
                            containerColor = Slate100,
                            labelColor = Slate700
                        )
                    )

                    val isHutang = jenis == "HUTANG_SAYA"
                    FilterChip(
                        selected = isHutang,
                        onClick = { jenis = "HUTANG_SAYA" },
                        label = { Text(AppStrings.tr("Kita yang Berhutang", "I Borrow (I Owe)", lang), fontSize = 11.sp, fontWeight = if (isHutang) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseCritical,
                            selectedLabelColor = Color.White,
                            containerColor = Slate100,
                            labelColor = Slate700
                        )
                    )
                }

                // Nama Pihak
                OutlinedTextField(
                    value = namaPihak,
                    onValueChange = { namaPihak = it },
                    label = { Text(AppStrings.tr("Nama Teman / Keluarga", "Friend / Family Name", lang)) },
                    placeholder = { Text("Contoh: Budi Teman SMA, Om Anton") },
                    modifier = Modifier.fillMaxWidth().testTag("input_nama_pihak_debt"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Hubungan
                Text(AppStrings.tr("Hubungan Relasi:", "Relationship:", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    hubunganList.forEach { rel ->
                        val isSelected = hubungan == rel
                        FilterChip(
                            selected = isSelected,
                            onClick = { hubungan = rel },
                            label = { Text(rel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate900,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700
                            )
                        )
                    }
                }

                // Kontak / HP
                OutlinedTextField(
                    value = kontak,
                    onValueChange = { kontak = it },
                    label = { Text(AppStrings.tr("Nomor WhatsApp / HP (Opsional)", "WhatsApp / Phone (Optional)", lang)) },
                    placeholder = { Text("081234567890") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Total Pinjaman
                OutlinedTextField(
                    value = totalNominalStr,
                    onValueChange = {
                        totalNominalStr = it.filter { ch -> ch.isDigit() }
                        if (debt == null) {
                            sisaNominalStr = totalNominalStr
                        }
                    },
                    label = { Text(AppStrings.tr("Total Pinjaman Awal (Rp)", "Total Initial Loan (Rp)", lang)) },
                    placeholder = { Text("Contoh: 500000") },
                    modifier = Modifier.fillMaxWidth().testTag("input_nominal_debt"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // If Editing: Sisa yang belum dibayar
                if (debt != null) {
                    OutlinedTextField(
                        value = sisaNominalStr,
                        onValueChange = { sisaNominalStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(AppStrings.tr("Sisa Saldo Belum Lunas (Rp)", "Remaining Balance (Rp)", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Tanggal Janji Bayar / Jatuh Tempo
                OutlinedTextField(
                    value = tanggalJatuhTempo,
                    onValueChange = { tanggalJatuhTempo = it },
                    label = { Text(AppStrings.tr("Janji Bayar / Jatuh Tempo (Opsional)", "Due Date / Promise (Optional)", lang)) },
                    placeholder = { Text("Contoh: 2026-04-15 atau Akhir Bulan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Catatan
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text(AppStrings.tr("Keperluan / Catatan", "Purpose / Notes", lang)) },
                    placeholder = { Text("Contoh: Pinjam buat tambal ban / bayar kontrakan") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )

                // Submit Button
                val totalNominalVal = totalNominalStr.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (namaPihak.isNotBlank() && totalNominalVal > 0) {
                            val debtId = debt?.id ?: "DEBT_${UUID.randomUUID().toString().take(8)}"
                            val sisaVal = (sisaNominalStr.toDoubleOrNull() ?: totalNominalVal).coerceAtMost(totalNominalVal)
                            val statusVal = if (sisaVal <= 0.0) "LUNAS" else if (sisaVal < totalNominalVal) "SEBAGIAN" else "BELUM_LUNAS"
                            onSave(
                                PersonalDebtEntity(
                                    id = debtId,
                                    jenis = jenis,
                                    namaPihak = namaPihak.trim(),
                                    hubungan = hubungan,
                                    kontak = kontak.trim(),
                                    totalNominal = totalNominalVal,
                                    sisaNominal = sisaVal,
                                    tanggalPinjam = tanggalPinjam,
                                    tanggalJatuhTempo = tanggalJatuhTempo.trim(),
                                    status = statusVal,
                                    catatan = catatan.trim(),
                                    riwayatBayarJson = debt?.riwayatBayarJson ?: "[]"
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_simpan_debt"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (jenis == "PIUTANG_SAYA") Color(0xFF2563EB) else RoseCritical,
                        contentColor = Color.White
                    ),
                    enabled = namaPihak.isNotBlank() && totalNominalVal > 0
                ) {
                    Text(AppStrings.tr("Simpan Catatan Hutang", "Save Debt Record", lang), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4. BAYAR / CICIL HUTANG DIALOG
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BayarCicilanHutangDialog(
    debt: PersonalDebtEntity,
    accounts: List<PersonalAccountEntity>,
    onDismiss: () -> Unit,
    onConfirmPay: (nominal: Double, accountId: String?, keterangan: String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val isPiutang = debt.jenis == "PIUTANG_SAYA"
    var bayarNominalStr by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var updateAccountBalance by remember { mutableStateOf(true) }
    var keterangan by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp)),
            color = Color.White
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
                    Text(
                        text = if (isPiutang) AppStrings.tr("Terima Cicilan / Pelunasan", "Receive Loan Payment", lang) else AppStrings.tr("Bayar Cicilan / Pelunasan", "Pay Debt Installment", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                // Info Pihak & Sisa
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPiutang) Color(0xFFEFF6FF) else Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = debt.namaPihak,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                        Text(
                            text = "${debt.hubungan} • ${if (isPiutang) AppStrings.tr("Dia berhutang ke kita", "Owes money to us", lang) else AppStrings.tr("Kita berhutang ke dia", "We owe money to them", lang)}",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStrings.tr("Sisa yang belum lunas:", "Remaining unpaid:", lang), fontSize = 11.sp, color = Slate600)
                            Text(
                                text = SfaViewModel.formatRupiah(debt.sisaNominal),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPiutang) Color(0xFF2563EB) else RoseCritical
                            )
                        }
                    }
                }

                // Nominal Pembayaran Input
                OutlinedTextField(
                    value = bayarNominalStr,
                    onValueChange = { bayarNominalStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isPiutang) AppStrings.tr("Nominal Uang Diterima (Rp)", "Amount Received (Rp)", lang) else AppStrings.tr("Nominal Uang Dibayarkan (Rp)", "Amount Paid (Rp)", lang)) },
                    placeholder = { Text("Contoh: 100000") },
                    modifier = Modifier.fillMaxWidth().testTag("input_nominal_bayar_debt"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Quick Full Pay button
                TextButton(
                    onClick = { bayarNominalStr = debt.sisaNominal.toLong().toString() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(AppStrings.tr("Set Lunas Penuh (${SfaViewModel.formatRupiah(debt.sisaNominal)})", "Full Settle (${SfaViewModel.formatRupiah(debt.sisaNominal)})", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Hubungkan ke Akun Keuangan (Auto Inflow / Outflow)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPiutang) AppStrings.tr("Masuk ke Saldo Akun?", "Add to Account Balance?", lang) else AppStrings.tr("Potong dari Saldo Akun?", "Deduct from Account?", lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = AppStrings.tr("Otomatis perbarui dompet/bank", "Auto update wallet/bank", lang),
                            fontSize = 10.sp,
                            color = Slate500
                        )
                    }
                    Switch(
                        checked = updateAccountBalance,
                        onCheckedChange = { updateAccountBalance = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Slate900)
                    )
                }

                if (updateAccountBalance) {
                    Text(AppStrings.tr("Pilih Akun / Dompet:", "Choose Account / Wallet:", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.filter { !it.isPaylater }.forEach { acc ->
                            val isSelected = selectedAccountId == acc.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAccountId = acc.id },
                                label = {
                                    Text(
                                        "${acc.namaAkun} (${SfaViewModel.formatRupiah(acc.saldo)})",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Slate900,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100,
                                    labelColor = Slate700
                                )
                            )
                        }
                    }
                }

                // Keterangan / Bukti
                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text(AppStrings.tr("Keterangan Pembayaran", "Payment Note", lang)) },
                    placeholder = { Text(AppStrings.tr("Cicilan ke-1 / Lunas tunai", "Installment 1 / Full cash", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Submit Button
                val bayarVal = bayarNominalStr.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (bayarVal > 0) {
                            onConfirmPay(
                                bayarVal,
                                if (updateAccountBalance) selectedAccountId else null,
                                keterangan.trim()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_konfirmasi_bayar_debt"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPiutang) Color(0xFF2563EB) else RoseCritical,
                        contentColor = Color.White
                    ),
                    enabled = bayarVal > 0
                ) {
                    Text(
                        text = if (isPiutang) AppStrings.tr("Konfirmasi Terima Pembayaran", "Confirm Received Payment", lang) else AppStrings.tr("Konfirmasi Bayar Hutang", "Confirm Debt Payment", lang),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
