package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.SfaRepository
import com.example.util.OfflineSyncHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class AppNavScreen(val title: String, val iconName: String) {
    TRANSAKSI("Transaksi", "assignment"),
    RIWAYAT("Riwayat", "history"),
    DASHBOARD("Dashboard", "dashboard"),
    MASTER_DATA("Master Data", "inventory"),
    LAPORAN("Laporan", "analytics"),
    UTILITAS("Utilitas", "settings")
}

enum class OutletSortBy(val label: String, val icon: String) {
    TERDEKAT_GPS("Jarak Terdekat (GPS)", "location_on"),
    LAMA_TIDAK_DIKUNJUNGI("Terlama Belum Dikunjungi", "history"),
    URUTAN_RUTE("Urutan Jalur Rute", "route"),
    OMSET_TERBESAR("Omset Penjualan Terbesar", "trending_up"),
    PIUTANG_TERBESAR("Saldo Piutang Terbesar", "money_off"),
    STOK_MENIPIS("Stok Titipan Sedikit", "inventory_2"),
    NAMA_AZ("Nama Outlet (A-Z)", "sort_by_alpha")
}

enum class OutletFilterAging(val label: String) {
    SEMUA("Semua Outlet"),
    BELUM_HARI_INI("Belum Hari Ini"),
    LEBIH_3_HARI("> 3 Hari"),
    LEBIH_7_HARI("> 7 Hari (Mingguan)"),
    LEBIH_14_HARI("> 14 Hari (Kritis)"),
    LEBIH_30_HARI("> 30 Hari (Dormant)"),
    KUSTOM_HARI("Kustom Hari..."),
    SUDAH_HARI_INI("Selesai Hari Ini")
}

class SfaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SfaRepository
    
    // Offline / Online Connectivity & Sync State
    val isOnline: StateFlow<Boolean> = OfflineSyncHelper.observeNetworkConnectivity(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OfflineSyncHelper.isNetworkAvailable(application))

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = SfaRepository(db.sfaDao())

        // Auto-sync addresses when device goes online
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    syncPendingAddresses(silent = true)
                }
            }
        }

        // Self-heal and normalize any unlinked ruteId or duplicate/zero visit sequences
        viewModelScope.launch {
            try {
                val rutes = repository.getAllRutesDirect()
                val warungs = repository.getAllWarungsDirect()
                if (rutes.isNotEmpty() && warungs.isNotEmpty()) {
                    val validRuteIds = rutes.map { it.id }.toSet()
                    val defaultRuteId = rutes.first().id
                    var hasChanges = false
                    val normalizedWarungs = warungs.mapIndexed { index, w ->
                        var updated = w
                        if (w.ruteId.isBlank() || !validRuteIds.contains(w.ruteId)) {
                            updated = updated.copy(ruteId = defaultRuteId)
                            hasChanges = true
                        }
                        if (updated.urutanKunjungan <= 0) {
                            updated = updated.copy(urutanKunjungan = index + 1)
                            hasChanges = true
                        }
                        updated
                    }
                    if (hasChanges) {
                        repository.insertWarungsBatch(normalizedWarungs)
                    }
                }
                repository.deleteLegacyClosingTransactions()
            } catch (_: Exception) {}
        }
    }

    fun syncPendingAddresses(silent: Boolean = false) {
        viewModelScope.launch {
            if (!isOnline.value) {
                if (!silent) {
                    _feedbackSnackbar.value = "Perangkat sedang Offline. Alamat akan otomatis disinkronkan saat terhubung ke internet."
                }
                return@launch
            }

            _isSyncing.value = true
            try {
                val syncedCount = OfflineSyncHelper.syncPendingWarungAddresses(getApplication(), repository)
                if (syncedCount > 0) {
                    _feedbackSnackbar.value = "Berhasil menerjemahkan $syncedCount koordinat GPS menjadi alamat jalan resmi."
                } else if (!silent) {
                    _feedbackSnackbar.value = "Semua koordinat outlet sudah tersinkronisasi."
                }
            } catch (e: Exception) {
                if (!silent) {
                    _feedbackSnackbar.value = "Gagal sinkronisasi alamat: ${e.message}"
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // State Flows from DB (Eagerly subscribed for instant real-time updates without reload/relog)
    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val warungs: StateFlow<List<WarungEntity>> = repository.allWarungs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rutes: StateFlow<List<RuteEntity>> = repository.allRutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val drawers: StateFlow<List<InventoryDrawerEntity>> = repository.allInventoryDrawers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dailyLoadings: StateFlow<List<DailyLoadingEntity>> = repository.allDailyLoadings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bsSortirs: StateFlow<List<BsSortirEntity>> = repository.allBsSortirs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pabriks: StateFlow<List<PabrikEntity>> = repository.allPabriks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val writeOffs: StateFlow<List<WriteOffEntity>> = repository.allWriteOffs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val appLanguage: StateFlow<String> = userProfile
        .map { it?.appLanguage?.ifBlank { "ID" } ?: "ID" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "ID")

    val customPrices: StateFlow<List<WarungCustomPriceEntity>> = repository.allCustomPrices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Location & GPS Tracking (100% Offline Compatible)
    private val _currentGpsLocation = MutableStateFlow(
        com.example.util.LocationHelper.getInstantLocation(application)
    )
    val currentGpsLocation: StateFlow<com.example.util.UserGpsLocation> = _currentGpsLocation.asStateFlow()

    private var gpsTrackingJob: kotlinx.coroutines.Job? = null

    init {
        startGpsTracking()
    }

    fun startGpsTracking() {
        gpsTrackingJob?.cancel()
        gpsTrackingJob = viewModelScope.launch {
            // First fetch best instant location available
            val instant = com.example.util.LocationHelper.getInstantLocation(getApplication())
            if (instant.isAvailable) {
                _currentGpsLocation.value = instant
            }
            // Then continuously collect live GPS stream
            com.example.util.LocationHelper.observeCurrentLocation(getApplication()).collect { loc ->
                _currentGpsLocation.value = loc
            }
        }
    }

    fun refreshGpsLocation() {
        val instant = com.example.util.LocationHelper.getInstantLocation(getApplication())
        _currentGpsLocation.value = instant
        startGpsTracking()
    }

    // Outlet List Sorting and Aging Filter States
    private val _outletSortBy = MutableStateFlow(OutletSortBy.TERDEKAT_GPS)
    val outletSortBy: StateFlow<OutletSortBy> = _outletSortBy.asStateFlow()

    private val _outletFilterAging = MutableStateFlow(OutletFilterAging.SEMUA)
    val outletFilterAging: StateFlow<OutletFilterAging> = _outletFilterAging.asStateFlow()

    private val _customMinDaysFilter = MutableStateFlow<Int?>(null)
    val customMinDaysFilter: StateFlow<Int?> = _customMinDaysFilter.asStateFlow()

    fun setOutletSortBy(sortBy: OutletSortBy) {
        _outletSortBy.value = sortBy
    }

    fun setOutletFilterAging(filterAging: OutletFilterAging) {
        _outletFilterAging.value = filterAging
        if (filterAging != OutletFilterAging.KUSTOM_HARI) {
            _customMinDaysFilter.value = null
        }
    }

    fun setCustomMinDaysFilter(days: Int?) {
        _customMinDaysFilter.value = days
        if (days != null) {
            _outletFilterAging.value = OutletFilterAging.KUSTOM_HARI
        }
    }

    // Navigation and UI state
    private val _currentScreen = MutableStateFlow(AppNavScreen.TRANSAKSI)
    val currentScreen: StateFlow<AppNavScreen> = _currentScreen.asStateFlow()

    private val _selectedRuteId = MutableStateFlow<String?>(null)
    val selectedRuteId: StateFlow<String?> = _selectedRuteId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dialog & Flow States
    private val _activeTransactionDialog = MutableStateFlow<TransactionDialogState?>(null)
    val activeTransactionDialog: StateFlow<TransactionDialogState?> = _activeTransactionDialog.asStateFlow()

    private val _showReceiptDialog = MutableStateFlow<TransactionEntity?>(null)
    val showReceiptDialog: StateFlow<TransactionEntity?> = _showReceiptDialog.asStateFlow()

    private val _showClosingReceipt = MutableStateFlow<ClosingSummaryData?>(null)
    val showClosingReceipt: StateFlow<ClosingSummaryData?> = _showClosingReceipt.asStateFlow()

    private val _feedbackSnackbar = MutableStateFlow<String?>(null)
    val feedbackSnackbar: StateFlow<String?> = _feedbackSnackbar.asStateFlow()

    fun setScreen(screen: AppNavScreen) {
        _currentScreen.value = screen
    }

    fun setSelectedRute(ruteId: String?) {
        _selectedRuteId.value = ruteId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openTransactionDialog(state: TransactionDialogState) {
        _activeTransactionDialog.value = state
    }

    fun closeTransactionDialog() {
        _activeTransactionDialog.value = null
    }

    fun showReceipt(transaction: TransactionEntity) {
        _showReceiptDialog.value = transaction
    }

    fun closeReceipt() {
        _showReceiptDialog.value = null
    }

    fun showClosingReceiptDialog(data: ClosingSummaryData) {
        _showClosingReceipt.value = data
    }

    fun closeClosingReceipt() {
        _showClosingReceipt.value = null
    }

    fun dismissSnackbar() {
        _feedbackSnackbar.value = null
    }

    // --- BUSINESS ACTIONS ---

    fun executeBatchLoadingPagi(items: List<com.example.data.repository.LoadingItemInput>) {
        viewModelScope.launch {
            val validItems = items.filter { it.jumlahDus > 0 }
            if (validItems.isEmpty()) {
                _feedbackSnackbar.value = "Tidak ada barang yang dimuat (Kuantiti 0)."
                return@launch
            }
            repository.processBatchDailyLoading(validItems)
            val totalPack = validItems.sumOf { it.jumlahDus }
            val totalPcs = validItems.sumOf { it.jumlahDus * it.rasioKonversi }
            val hasCash = validItems.any { it.opsiBayarMuat == "BAYAR_LANGSUNG" }
            val hasDebt = validItems.any { it.opsiBayarMuat == "HUTANG" }
            val noteStatus = if (hasCash) " (Bayar Cash Langsung)" else if (hasDebt) " (Dicatat Hutang/Tempo)" else " (Konsinyasi Closing)"
            _feedbackSnackbar.value = "Berhasil muat ${validItems.size} produk (Total $totalPack Pack / $totalPcs Pcs)$noteStatus ke Stok Fresh Mobil!"
            closeTransactionDialog()
        }
    }

    fun openBayarHutangSupplierDialog(loading: DailyLoadingEntity) {
        _activeTransactionDialog.value = TransactionDialogState.BayarHutangSupplier(loading)
    }

    fun executePayLoadingDebt(loadingId: String, bayarAmount: Double, namaProduk: String = "") {
        viewModelScope.launch {
            if (bayarAmount <= 0) {
                _feedbackSnackbar.value = "Nominal pembayaran harus lebih dari 0."
                return@launch
            }
            repository.payLoadingDebt(loadingId, bayarAmount)
            _feedbackSnackbar.value = "Pembayaran hutang supplier sebesar ${formatRupiah(bayarAmount)} $namaProduk berhasil dicatat!"
            closeTransactionDialog()
        }
    }

    fun executeLoadingPagi(
        productId: String,
        jumlahDus: Int,
        rasio: Int,
        hargaBeliDus: Double
    ) {
        executeBatchLoadingPagi(
            listOf(
                com.example.data.repository.LoadingItemInput(
                    productId = productId,
                    jumlahDus = jumlahDus,
                    rasioKonversi = rasio,
                    hargaBeliDus = hargaBeliDus
                )
            )
        )
    }

    fun executeTitipBaru(
        warung: WarungEntity,
        productId: String,
        sumberStok: String,
        jumlahPcs: Int,
        hargaSatuan: Double,
        gpsLat: Double,
        gpsLng: Double,
        gpsAddress: String,
        catatan: String
    ) {
        viewModelScope.launch {
            repository.processTitipBaru(
                warung = warung,
                productId = productId,
                sumberStok = sumberStok,
                jumlahPcs = jumlahPcs,
                hargaSatuan = hargaSatuan,
                gpsLat = gpsLat,
                gpsLng = gpsLng,
                gpsAddress = gpsAddress,
                catatan = catatan
            )
            _feedbackSnackbar.value = "Konsinyasi Baru Berhasil: +$jumlahPcs Pcs ke Outlet ${warung.namaWarung}"
            closeTransactionDialog()
        }
    }

    fun executeTarikSisaDanRestock(
        warung: WarungEntity,
        productId: String,
        sisaTitipanLalu: Int,
        sisaFisik: Int,
        hargaSatuan: Double,
        uangDiterima: Double,
        restockPcs: Int,
        sumberRestock: String,
        gpsLat: Double,
        gpsLng: Double,
        gpsAddress: String,
        catatan: String
    ) {
        viewModelScope.launch {
            repository.processTarikSisaDanRestock(
                warung = warung,
                productId = productId,
                sisaTitipanLalu = sisaTitipanLalu,
                sisaFisik = sisaFisik,
                hargaSatuan = hargaSatuan,
                uangDiterima = uangDiterima,
                restockPcs = restockPcs,
                sumberRestock = sumberRestock,
                gpsLat = gpsLat,
                gpsLng = gpsLng,
                gpsAddress = gpsAddress,
                catatan = catatan
            )
            _feedbackSnackbar.value = "Transaksi Outlet Selesai: Laku ${sisaTitipanLalu - sisaFisik} Pcs, Retur ${sisaFisik} Pcs, Bayar ${formatRupiah(uangDiterima)}"
            closeTransactionDialog()
        }
    }

    fun executeSortirBs(
        productId: String,
        totalBsAwal: Int,
        bsLayakJual: Int,
        bsRusak: Int,
        hargaBeliPcs: Double,
        hargaJualPcs: Double,
        catatan: String
    ) {
        viewModelScope.launch {
            repository.processSortirBs(
                productId = productId,
                totalBsAwal = totalBsAwal,
                bsLayakJual = bsLayakJual,
                bsRusak = bsRusak,
                hargaBeliPcs = hargaBeliPcs,
                hargaJualPcs = hargaJualPcs,
                catatan = catatan
            )
            _feedbackSnackbar.value = "Sortir Retur Selesai: +$bsLayakJual Pcs Layak Jual (Aset Mandiri), $bsRusak Pcs Afkir Rusak"
            closeTransactionDialog()
        }
    }

    fun executeBatchClosingSore(
        items: List<com.example.data.repository.ProductClosingInput>,
        summaryData: ClosingSummaryData? = null
    ) {
        viewModelScope.launch {
            repository.processBatchClosingSore(items)
            _feedbackSnackbar.value = "Closing Harian Multi-Supplier Berhasil: Setoran Pabrik & Stok Mobil Diperbarui."
            closeTransactionDialog()
            if (summaryData != null) {
                _showClosingReceipt.value = summaryData
            }
        }
    }

    fun executeClosingSore(
        loadingId: String,
        sisaDusSore: Int,
        sisaPcsLepasanSore: Int = 0,
        summaryData: ClosingSummaryData? = null
    ) {
        viewModelScope.launch {
            repository.processClosingSore(loadingId, sisaDusSore, sisaPcsLepasanSore)
            _feedbackSnackbar.value = "Closing Harian Berhasil: Tagihan Supplier & Stok Mobil diperbarui."
            closeTransactionDialog()
            if (summaryData != null) {
                _showClosingReceipt.value = summaryData
            }
        }
    }

    fun executeWriteOff(
        warung: WarungEntity,
        hargaSatuan: Double,
        alasan: String
    ) {
        viewModelScope.launch {
            repository.processWriteOff(warung, hargaSatuan, alasan)
            _feedbackSnackbar.value = "Status ${warung.namaWarung} diubah Blacklist & Saldo Piutang/Stok di Write-Off."
            closeTransactionDialog()
        }
    }

    fun addOrUpdateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product)
            _feedbackSnackbar.value = "Produk ${product.nama} tersimpan."
            closeTransactionDialog()
        }
    }

    fun addOrUpdateWarung(warung: WarungEntity) {
        viewModelScope.launch {
            var finalWarung = warung

            // Compress and persist photo into app internal storage
            finalWarung.fotoOutlet?.let { photoUri ->
                if (photoUri.isNotBlank()) {
                    try {
                        val compressedUri = com.example.util.ImageCompressor.compressAndPersistPhoto(
                            context = getApplication(),
                            sourceUriString = photoUri
                        )
                        finalWarung = finalWarung.copy(fotoOutlet = compressedUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // If address is empty or contains coordinates placeholder
            if (finalWarung.alamatLengkap.isBlank() || finalWarung.alamatLengkap.startsWith("Koordinat GPS")) {
                if (isOnline.value) {
                    val resolved = OfflineSyncHelper.reverseGeocodeCoordinates(
                        getApplication(),
                        finalWarung.latitude,
                        finalWarung.longitude
                    )
                    if (!resolved.isNullOrBlank()) {
                        finalWarung = finalWarung.copy(alamatLengkap = resolved, pendingAddressSync = false)
                    } else {
                        finalWarung = finalWarung.copy(
                            alamatLengkap = "Koordinat GPS: ${String.format(Locale.US, "%.5f", finalWarung.latitude)}, ${String.format(Locale.US, "%.5f", finalWarung.longitude)}",
                            pendingAddressSync = true
                        )
                    }
                } else {
                    finalWarung = finalWarung.copy(
                        alamatLengkap = "Koordinat GPS: ${String.format(Locale.US, "%.5f", finalWarung.latitude)}, ${String.format(Locale.US, "%.5f", finalWarung.longitude)} (Offline)",
                        pendingAddressSync = true
                    )
                }
            }

            repository.saveWarung(finalWarung)
            _feedbackSnackbar.value = if (finalWarung.pendingAddressSync) {
                "Outlet ${finalWarung.namaWarung} tersimpan offline (Titik GPS tercatat, alamat otomatis di-sync saat online)."
            } else {
                "Outlet ${finalWarung.namaWarung} tersimpan."
            }
            closeTransactionDialog()
        }
    }

    fun addOrUpdateRute(rute: RuteEntity) {
        viewModelScope.launch {
            repository.saveRute(rute)
            _feedbackSnackbar.value = "Rute ${rute.namaRute} tersimpan."
            closeTransactionDialog()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _feedbackSnackbar.value = "Produk ${product.nama} berhasil dihapus."
            closeTransactionDialog()
        }
    }

    fun deleteWarung(warung: WarungEntity) {
        viewModelScope.launch {
            repository.deleteWarung(warung)
            _feedbackSnackbar.value = "Outlet ${warung.namaWarung} berhasil dihapus."
            closeTransactionDialog()
        }
    }

    fun deleteRute(rute: RuteEntity) {
        viewModelScope.launch {
            repository.deleteRute(rute)
            _feedbackSnackbar.value = "Rute ${rute.namaRute} berhasil dihapus."
            closeTransactionDialog()
        }
    }

    fun addOrUpdatePabrik(pabrik: PabrikEntity) {
        viewModelScope.launch {
            repository.savePabrik(pabrik)
            _feedbackSnackbar.value = "Supplier / Principal ${pabrik.namaPabrik} tersimpan."
            closeTransactionDialog()
        }
    }

    fun deletePabrik(pabrik: PabrikEntity) {
        viewModelScope.launch {
            repository.deletePabrik(pabrik)
            _feedbackSnackbar.value = "Supplier / Principal ${pabrik.namaPabrik} berhasil dihapus."
            closeTransactionDialog()
        }
    }

    fun resetDataForProduction() {
        viewModelScope.launch {
            repository.resetTransactionalDataForProduction()
            _feedbackSnackbar.value = "Data transaksi, laci stok, dan saldo piutang berhasil dikosongkan (Siap Produksi)."
        }
    }

    fun wipeAllMasterAndTransactionalData() {
        viewModelScope.launch {
            repository.wipeAllDataCompletely()
            _feedbackSnackbar.value = "Seluruh database berhasil dikosongkan secara total (Fresh Production Start)."
        }
    }

    // --- USER PROFILE ---
    fun saveUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveUserProfile(profile.copy(isConfigured = true))
            _feedbackSnackbar.value = "Profil Salesman & Usaha berhasil disimpan."
            closeTransactionDialog()
        }
    }

    fun setAppLanguage(lang: String) {
        viewModelScope.launch {
            val current = repository.getUserProfileDirect() ?: UserProfileEntity()
            val updated = current.copy(appLanguage = lang)
            repository.saveUserProfile(updated)
            _feedbackSnackbar.value = if (lang.equals("EN", ignoreCase = true)) {
                "Language changed to English (US)"
            } else {
                "Bahasa diubah ke Bahasa Indonesia (ID)"
            }
        }
    }

    // --- CUSTOM PRICING PER WARUNG ---
    fun getEffectivePriceForWarung(warungId: String, productId: String, defaultPrice: Double): Double {
        val custom = customPrices.value.find { it.warungId == warungId && it.productId == productId }
        return custom?.hargaJualPcs ?: defaultPrice
    }

    fun setCustomPrice(warungId: String, productId: String, hargaJualPcs: Double) {
        viewModelScope.launch {
            repository.saveCustomPrice(warungId, productId, hargaJualPcs)
            _feedbackSnackbar.value = "Harga khusus Rp${hargaJualPcs.toLong()}/pcs berhasil disetel untuk outlet ini."
        }
    }

    fun deleteCustomPrice(warungId: String, productId: String) {
        viewModelScope.launch {
            repository.deleteCustomPrice(warungId, productId)
            _feedbackSnackbar.value = "Harga khusus dihapus. Kembali ke harga default produk."
        }
    }

    // --- BACKUP & RESTORE ---
    fun exportBackup(onResult: (com.example.util.ExportResult) -> Unit) {
        viewModelScope.launch {
            try {
                val result = com.example.util.BackupRestoreHelper.exportFullDatabaseToJson(getApplication(), repository)
                onResult(result)
            } catch (e: Exception) {
                _feedbackSnackbar.value = "Gagal membuat backup: ${e.message}"
            }
        }
    }

    fun exportModularBackup(selection: com.example.util.BackupSelection, onResult: (com.example.util.ExportResult) -> Unit) {
        viewModelScope.launch {
            try {
                val result = com.example.util.BackupRestoreHelper.exportModularDatabaseToJson(getApplication(), repository, selection)
                onResult(result)
            } catch (e: Exception) {
                _feedbackSnackbar.value = "Gagal membuat backup modular: ${e.message}"
            }
        }
    }

    fun exportZipBackup(selection: com.example.util.BackupSelection, onResult: (com.example.util.ZipBackupResult) -> Unit) {
        viewModelScope.launch {
            try {
                val result = com.example.util.BackupRestoreHelper.exportFullBackupToZip(getApplication(), repository, selection)
                onResult(result)
            } catch (e: Exception) {
                _feedbackSnackbar.value = "Gagal membuat paket ZIP backup: ${e.message}"
            }
        }
    }

    fun copyBackupToSaf(sourceFile: java.io.File, targetUri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = com.example.util.BackupRestoreHelper.copyFileToUri(getApplication(), sourceFile, targetUri)
            if (success) {
                _feedbackSnackbar.value = "Berkas migrasi berhasil disimpan ke folder HP!"
            } else {
                _feedbackSnackbar.value = "Gagal menyimpan berkas ke folder yang dipilih."
            }
            onComplete(success)
        }
    }

    fun importBackup(jsonString: String, onComplete: (com.example.util.ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = com.example.util.BackupRestoreHelper.importFullDatabaseFromJson(jsonString, repository, getApplication())
            refreshGpsLocation()
            _feedbackSnackbar.value = result.message
            onComplete(result)
        }
    }

    fun importModularBackup(jsonString: String, selection: com.example.util.BackupSelection, onComplete: (com.example.util.ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = com.example.util.BackupRestoreHelper.importModularDatabaseFromJson(jsonString, repository, selection, getApplication())
            refreshGpsLocation()
            _feedbackSnackbar.value = result.message
            onComplete(result)
        }
    }

    fun importBackupFromUri(uri: android.net.Uri, selection: com.example.util.BackupSelection, onComplete: (com.example.util.ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = com.example.util.BackupRestoreHelper.importFromUri(getApplication(), uri, repository, selection)
            refreshGpsLocation()
            _feedbackSnackbar.value = result.message
            onComplete(result)
        }
    }

    // ==========================================
    // AI GATEWAY & COPILOT INTEGRATION
    // ==========================================
    private val _aiConfig = MutableStateFlow(com.example.data.ai.AiPreferencesHelper.getAiConfig(application))
    val aiConfig: StateFlow<com.example.data.ai.AiConfig> = _aiConfig.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<com.example.data.ai.AiChatMessage>>(
        listOf(
            com.example.data.ai.AiChatMessage(
                role = "assistant",
                content = "Halo! Saya **TracerPro AI Copilot**, asisten operasional SFA Konsinyasi FMCG Anda. Ada yang bisa saya bantu terkait analisa performa toko, ringkasan setoran harian, atau strategi restock produk?"
            )
        )
    )
    val aiChatMessages: StateFlow<List<com.example.data.ai.AiChatMessage>> = _aiChatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun updateAiConfig(newConfig: com.example.data.ai.AiConfig) {
        com.example.data.ai.AiPreferencesHelper.saveAiConfig(getApplication(), newConfig)
        _aiConfig.value = newConfig
        _feedbackSnackbar.value = "Pengaturan AI Gateway berhasil diperbarui!"
    }

    fun testAiConnection(config: com.example.data.ai.AiConfig, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val result = com.example.data.ai.OpenAiClient.testConnection(config)
            _isAiLoading.value = false
            result.onSuccess { msg ->
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, "Koneksi Gagal: ${err.message}")
            }
        }
    }

    fun clearAiChatHistory() {
        _aiChatMessages.value = listOf(
            com.example.data.ai.AiChatMessage(
                role = "assistant",
                content = "Riwayat chat telah dibersihkan. Silakan tanyakan hal baru seputar operasional, keuangan, atau rute outlet Anda."
            )
        )
    }

    fun sendAiChatMessage(userMessage: String) {
        val cleanMsg = userMessage.trim()
        if (cleanMsg.isBlank()) return

        val currentList = _aiChatMessages.value.toMutableList()
        currentList.add(com.example.data.ai.AiChatMessage(role = "user", content = cleanMsg))
        _aiChatMessages.value = currentList

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val cfg = _aiConfig.value
                val systemPrompt = com.example.data.ai.AiPromptBuilder.buildSystemPrompt(cfg.customPersona)
                
                // Build dynamic operational context from Room DB (Full Database Context)
                val contextData = com.example.data.ai.AiPromptBuilder.buildCopilotContextPrompt(
                    profile = userProfile.value,
                    pabriks = pabriks.value,
                    products = products.value,
                    rutes = rutes.value,
                    warungs = warungs.value,
                    customPrices = customPrices.value,
                    drawers = drawers.value,
                    allLoadings = dailyLoadings.value,
                    transactions = transactions.value,
                    sortirs = bsSortirs.value,
                    writeOffs = writeOffs.value
                )

                val fullMessages = mutableListOf<com.example.data.ai.AiChatMessage>()
                fullMessages.add(com.example.data.ai.AiChatMessage(role = "system", content = "$systemPrompt\n\n$contextData"))
                
                // Add conversation history (last 8 messages for context window efficiency)
                val conversationHistory = currentList.takeLast(8)
                fullMessages.addAll(conversationHistory)

                val result = com.example.data.ai.OpenAiClient.generateChatCompletion(cfg, fullMessages)
                _isAiLoading.value = false

                result.onSuccess { reply ->
                    val updated = _aiChatMessages.value.toMutableList()
                    updated.add(com.example.data.ai.AiChatMessage(role = "assistant", content = reply))
                    _aiChatMessages.value = updated
                }.onFailure { err ->
                    val updated = _aiChatMessages.value.toMutableList()
                    val errorMsg = if (cfg.apiKey.isBlank() && cfg.endpoint.contains("openai.com")) {
                        "⚠️ **API Key Belum Diisi:** Silakan atur Endpoint, API Key, dan Model Anda di menu **Utilitas > Pengaturan AI Gateway**."
                    } else {
                        "⚠️ **Gagal Mendapatkan Respons AI:** ${err.message}"
                    }
                    updated.add(com.example.data.ai.AiChatMessage(role = "assistant", content = errorMsg))
                    _aiChatMessages.value = updated
                }
            } catch (e: Exception) {
                _isAiLoading.value = false
                val updated = _aiChatMessages.value.toMutableList()
                updated.add(com.example.data.ai.AiChatMessage(role = "assistant", content = "⚠️ Terjadi kesalahan: ${e.message}"))
                _aiChatMessages.value = updated
            }
        }
    }

    fun generateAiWhatsAppDraft(onResult: (String) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val cfg = _aiConfig.value
            val systemPrompt = com.example.data.ai.AiPromptBuilder.buildSystemPrompt(cfg.customPersona)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayTxs = transactions.value.filter { it.tanggal == todayStr || it.tanggal.startsWith(todayStr) }
            val contextData = com.example.data.ai.AiPromptBuilder.buildCopilotContextPrompt(
                profile = userProfile.value,
                pabriks = pabriks.value,
                products = products.value,
                rutes = rutes.value,
                warungs = warungs.value,
                customPrices = customPrices.value,
                drawers = drawers.value,
                allLoadings = dailyLoadings.value,
                transactions = transactions.value,
                sortirs = bsSortirs.value,
                writeOffs = writeOffs.value
            )

            val messages = listOf(
                com.example.data.ai.AiChatMessage(role = "system", content = "$systemPrompt\n\n$contextData"),
                com.example.data.ai.AiChatMessage(
                    role = "user",
                    content = "Buatkan draf teks pesan laporan operasional harian yang sangat rapi, profesional, dan siap dikirim ke WhatsApp Pemilik / Bos Distributor. Cantumkan rincian toko dikunjungi, total omset laku, kas tunai terkumpul, potensi setoran pabrik, sisa stok mobil, dan catatan piutang penting. Gunakan format WhatsApp (bold bintang, bullet points, emoji)."
                )
            )

            val result = com.example.data.ai.OpenAiClient.generateChatCompletion(cfg, messages)
            _isAiLoading.value = false

            result.onSuccess { draft ->
                onResult(draft)
            }.onFailure { err ->
                val fallbackDraft = buildFallbackWhatsAppReport(todayTxs)
                onResult(fallbackDraft)
            }
        }
    }

    fun getAiOutletRecommendation(warung: WarungEntity, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val cfg = _aiConfig.value
            val systemPrompt = com.example.data.ai.AiPromptBuilder.buildSystemPrompt(cfg.customPersona)
            val outletPrompt = com.example.data.ai.AiPromptBuilder.buildOutletRecommendationPrompt(
                warung = warung,
                products = products.value,
                transactions = transactions.value,
                customPrices = customPrices.value,
                drawers = drawers.value,
                rutes = rutes.value
            )

            val messages = listOf(
                com.example.data.ai.AiChatMessage(role = "system", content = systemPrompt),
                com.example.data.ai.AiChatMessage(role = "user", content = outletPrompt)
            )

            val result = com.example.data.ai.OpenAiClient.generateChatCompletion(cfg, messages)
            _isAiLoading.value = false

            result.onSuccess { advice ->
                onResult(advice)
            }.onFailure { err ->
                val fallback = "💡 **Saran Sistem Heuristik:** Berdasarkan kategori ${warung.kategoriWarung}, titipkan 15–20 pcs produk fast-moving. Saldo bon saat ini ${formatRupiah(warung.saldoPiutang)} (Limit: ${formatRupiah(warung.limitHutangMaksimal)}). Pastikan tarik kas sebelum menambah limit kredit."
                onResult(fallback)
            }
        }
    }

    private fun buildFallbackWhatsAppReport(todayTxs: List<TransactionEntity>): String {
        val totalOmset = todayTxs.sumOf { it.subtotalLaku }
        val totalKas = todayTxs.sumOf { it.uangDiterima }
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val profile = userProfile.value

        return """
📋 *LAPORAN PENJUALAN HARIAN FMCG*
📅 Tanggal: $dateStr
👤 Salesman: ${profile?.namaSalesman ?: "Salesman"}
🚚 Armada: ${profile?.platNomorMobil ?: "-"}
🏢 Depo: ${profile?.namaDistributor ?: "Distributor"}

*RINGKASAN OPERASIONAL:*
• Toko Dikunjungi: ${todayTxs.size} Warung
• Total Penjualan: ${formatRupiah(totalOmset)}
• Kas Terkumpul: ${formatRupiah(totalKas)}
• Total Piutang Aktif: ${formatRupiah(warungs.value.sumOf { it.saldoPiutang })}

*STATUS STOK MOBIL:*
• Fresh Pabrik: ${drawers.value.sumOf { it.stokFreshPabrikPcs }} Pcs
• Retur Belum Sortir: ${drawers.value.sumOf { it.stokBsBelumSortirPcs }} Pcs
• Aset Repack: ${drawers.value.sumOf { it.stokPribadiLayakJualPcs }} Pcs

_Laporan dibuat otomatis via TracerPro SFA_
""".trimIndent()
    }

    companion object {
        private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

        private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

        @Synchronized
        fun formatRupiah(amount: Double): String {
            return rupiahFormat.format(amount).replace(",00", "")
        }

        @Synchronized
        fun formatDate(timestamp: Long): String {
            return dateTimeFormat.format(Date(timestamp))
        }

        @Synchronized
        fun formatSimpleDate(timestamp: Long): String {
            return dateFormat.format(Date(timestamp))
        }
    }
}

data class ClosingProductSummary(
    val productId: String,
    val productName: String,
    val pabrikId: String,
    val pabrikName: String,
    val satuanBesar: String = "Pack",
    val rasioKonversi: Int,
    val hargaBeliPabrikDus: Double,
    val totalMuatDus: Int,
    val totalMuatPcs: Int,
    val sisaDusSore: Int,
    val sisaPcsLepasanSore: Int,
    val sisaTotalPcsSore: Int,
    val pcsTerdistribusi: Int,
    val terjualDusEquivalent: Double,
    val tagihanPabrik: Double
)

data class ClosingSupplierSummary(
    val pabrikId: String,
    val pabrikName: String,
    val products: List<ClosingProductSummary>,
    val totalMuatDus: Int,
    val sisaDusSore: Int,
    val sisaPcsLepasanSore: Int,
    val sisaTotalPcsSore: Int,
    val pcsTerdistribusi: Int,
    val totalTerjualDusEquivalent: Double,
    val totalTagihanPabrik: Double
)

data class ClosingSummaryData(
    val tanggal: String,
    val waktuClosing: String,
    val totalMuatDus: Int,
    val totalTagihanSemuaSupplier: Double,
    val totalKasWarungHariIni: Double,
    val selisihKas: Double,
    val totalTxCount: Int,
    val totalSortirTodayPcs: Int,
    val supplierSummaries: List<ClosingSupplierSummary> = emptyList(),
    val productSummaries: List<ClosingProductSummary> = emptyList(),
    val productName: String = "",
    val sisaDusSore: Int = 0,
    val sisaPcsLepasan: Int = 0,
    val terjualDus: Int = 0,
    val tagihanPabrikFinal: Double = totalTagihanSemuaSupplier
)

sealed class TransactionDialogState {
    object MuatPagi : TransactionDialogState()
    data class TitipBaru(val warung: WarungEntity) : TransactionDialogState()
    data class TarikSisa(val warung: WarungEntity) : TransactionDialogState()
    object SortirBs : TransactionDialogState()
    object ClosingSore : TransactionDialogState()
    data class AddEditProduct(val product: ProductEntity?) : TransactionDialogState()
    data class AddEditWarung(val warung: WarungEntity?) : TransactionDialogState()
    data class AddEditRute(val rute: RuteEntity?) : TransactionDialogState()
    data class AddEditPabrik(val pabrik: PabrikEntity?) : TransactionDialogState()
    data class WriteOff(val warung: WarungEntity) : TransactionDialogState()
    data class WarungDetail(val warung: WarungEntity) : TransactionDialogState()
    data class OutletStatistics(val warung: WarungEntity) : TransactionDialogState()
    data class ManageCustomPrices(val warung: WarungEntity) : TransactionDialogState()
    object SetupProfile : TransactionDialogState()
    object GpsTool : TransactionDialogState()
    object ExportBackup : TransactionDialogState()
    object ImportBackup : TransactionDialogState()
    data class EditConfig(val key: String, val currentVal: String) : TransactionDialogState()
    object AiCopilot : TransactionDialogState()
    object AiConfigSettings : TransactionDialogState()
    data class AiOutletRecommendation(val warung: WarungEntity) : TransactionDialogState()
    data class BayarHutangSupplier(val loading: DailyLoadingEntity) : TransactionDialogState()
}
