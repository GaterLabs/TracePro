package com.example.data.repository

import com.example.data.local.dao.SfaDao
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class LoadingItemInput(
    val productId: String,
    val jumlahDus: Int,
    val rasioKonversi: Int,
    val hargaBeliDus: Double,
    val opsiBayarMuat: String = "BAYAR_CLOSING", // "BAYAR_LANGSUNG", "BAYAR_CLOSING", "HUTANG"
    val jumlahBayarMuat: Double = 0.0,
    val catatanMuat: String = ""
)

data class ProductClosingInput(
    val productId: String,
    val sisaDusSore: Int,
    val sisaPcsLepasanSore: Int
)

class SfaRepository(private val dao: SfaDao) {

    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val allWarungs: Flow<List<WarungEntity>> = dao.getAllWarungs()
    val allRutes: Flow<List<RuteEntity>> = dao.getAllRutes()
    val allInventoryDrawers: Flow<List<InventoryDrawerEntity>> = dao.getAllInventoryDrawers()
    val allDailyLoadings: Flow<List<DailyLoadingEntity>> = dao.getAllDailyLoadings()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allBsSortirs: Flow<List<BsSortirEntity>> = dao.getAllBsSortirs()
    val allPabriks: Flow<List<PabrikEntity>> = dao.getAllPabriks()
    val allWriteOffs: Flow<List<WriteOffEntity>> = dao.getAllWriteOffs()
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val allCustomPrices: Flow<List<WarungCustomPriceEntity>> = dao.getAllCustomPrices()

    fun getWarungsByRute(ruteId: String): Flow<List<WarungEntity>> = dao.getWarungsByRute(ruteId)
    fun getCustomPricesByWarung(warungId: String): Flow<List<WarungCustomPriceEntity>> = dao.getCustomPricesByWarung(warungId)

    suspend fun getUserProfileDirect(): UserProfileEntity? = dao.getUserProfileDirect()
    suspend fun saveUserProfile(profile: UserProfileEntity) = dao.saveUserProfile(profile)

    suspend fun getProductById(id: String): ProductEntity? = dao.getProductById(id)
    suspend fun getWarungById(id: String): WarungEntity? = dao.getWarungById(id)
    suspend fun getWarungsPendingAddressSync(): List<WarungEntity> = dao.getWarungsPendingAddressSync()

    suspend fun getCustomPrice(warungId: String, productId: String): Double? {
        return dao.getCustomPrice(warungId, productId)?.hargaJualPcs
    }

    suspend fun saveCustomPrice(warungId: String, productId: String, hargaJualPcs: Double) {
        val existing = dao.getCustomPrice(warungId, productId)
        if (existing != null) {
            dao.insertCustomPrice(existing.copy(hargaJualPcs = hargaJualPcs, updatedAt = System.currentTimeMillis()))
        } else {
            dao.insertCustomPrice(WarungCustomPriceEntity(warungId = warungId, productId = productId, hargaJualPcs = hargaJualPcs))
        }
    }

    suspend fun deleteCustomPrice(warungId: String, productId: String) {
        dao.deleteCustomPrice(warungId, productId)
    }

    // Direct Getters for Backup
    suspend fun getAllProductsDirect(): List<ProductEntity> = dao.getAllProductsDirect()
    suspend fun getAllWarungsDirect(): List<WarungEntity> = dao.getAllWarungsDirect()
    suspend fun getAllRutesDirect(): List<RuteEntity> = dao.getAllRutesDirect()
    suspend fun getAllPabriksDirect(): List<PabrikEntity> = dao.getAllPabriksDirect()
    suspend fun getAllDrawersDirect(): List<InventoryDrawerEntity> = dao.getAllDrawersDirect()
    suspend fun getAllDailyLoadingsDirect(): List<DailyLoadingEntity> = dao.getAllDailyLoadingsDirect()
    suspend fun getAllTransactionsDirect(): List<TransactionEntity> = dao.getAllTransactionsDirect()
    suspend fun getAllBsSortirsDirect(): List<BsSortirEntity> = dao.getAllBsSortirsDirect()
    suspend fun getAllWriteOffsDirect(): List<WriteOffEntity> = dao.getAllWriteOffsDirect()
    suspend fun getAllCustomPricesDirect(): List<WarungCustomPriceEntity> = dao.getAllCustomPricesDirect()

    // Batch Insert for Restore
    suspend fun insertProductsBatch(products: List<ProductEntity>) = dao.insertProducts(products)
    suspend fun insertWarungsBatch(warungs: List<WarungEntity>) = dao.insertWarungs(warungs)
    suspend fun insertRutesBatch(rutes: List<RuteEntity>) = dao.insertRutes(rutes)
    suspend fun insertPabriksBatch(pabriks: List<PabrikEntity>) = dao.insertPabriks(pabriks)
    suspend fun insertDrawersBatch(drawers: List<InventoryDrawerEntity>) = dao.insertDrawers(drawers)
    suspend fun insertDailyLoadingsBatch(loadings: List<DailyLoadingEntity>) = dao.insertDailyLoadings(loadings)
    suspend fun insertTransactionsBatch(transactions: List<TransactionEntity>) = dao.insertTransactions(transactions)
    suspend fun insertBsSortirsBatch(sortirs: List<BsSortirEntity>) = dao.insertBsSortirs(sortirs)
    suspend fun insertWriteOffsBatch(writeOffs: List<WriteOffEntity>) = dao.insertWriteOffs(writeOffs)
    suspend fun insertCustomPricesBatch(prices: List<WarungCustomPriceEntity>) = dao.insertCustomPrices(prices)

    suspend fun saveProduct(product: ProductEntity) {
        dao.insertProduct(product)
        // Ensure drawer exists
        val existingDrawer = dao.getDrawerByProductId(product.id)
        if (existingDrawer == null) {
            dao.insertDrawer(
                InventoryDrawerEntity(
                    productId = product.id,
                    stokFreshPabrikPcs = 0,
                    stokBsBelumSortirPcs = 0,
                    stokPribadiLayakJualPcs = 0,
                    stokPribadiRusakPcs = 0
                )
            )
        }
    }

    suspend fun saveWarung(warung: WarungEntity) {
        dao.insertWarung(warung)
    }

    suspend fun saveRute(rute: RuteEntity) {
        dao.insertRute(rute)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        dao.deleteProduct(product)
        dao.deleteDrawerByProductId(product.id)
    }

    suspend fun deleteWarung(warung: WarungEntity) {
        dao.deleteWarung(warung)
    }

    suspend fun deleteRute(rute: RuteEntity) {
        dao.deleteRute(rute)
    }

    suspend fun savePabrik(pabrik: PabrikEntity) {
        dao.insertPabrik(pabrik)
    }

    suspend fun deletePabrik(pabrik: PabrikEntity) {
        dao.deletePabrik(pabrik)
    }

    /**
     * TAHAP 1: Muat Barang Pagi (Loading)
     * Convert Dus to Pcs -> Tambah stok_fresh_pabrik -> Catat potensi hutang pabrik
     */
    suspend fun processDailyLoading(
        productId: String,
        jumlahDus: Int,
        rasioKonversi: Int,
        hargaBeliDus: Double
    ) {
        processBatchDailyLoading(
            listOf(
                LoadingItemInput(
                    productId = productId,
                    jumlahDus = jumlahDus,
                    rasioKonversi = rasioKonversi,
                    hargaBeliDus = hargaBeliDus
                )
            )
        )
    }

    suspend fun processBatchDailyLoading(items: List<LoadingItemInput>) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        items.filter { it.jumlahDus > 0 }.forEach { item ->
            val totalPcs = item.jumlahDus * item.rasioKonversi
            val potensiHutang = item.jumlahDus * item.hargaBeliDus
            val sisaHutang = when (item.opsiBayarMuat) {
                "BAYAR_LANGSUNG" -> (potensiHutang - item.jumlahBayarMuat).coerceAtLeast(0.0)
                "HUTANG" -> (potensiHutang - item.jumlahBayarMuat).coerceAtLeast(0.0)
                else -> 0.0 // BAYAR_CLOSING: ditagihkan saat closing sore
            }
            val isLunas = when (item.opsiBayarMuat) {
                "BAYAR_LANGSUNG" -> item.jumlahBayarMuat >= potensiHutang
                "HUTANG" -> item.jumlahBayarMuat >= potensiHutang
                else -> false
            }

            val loading = DailyLoadingEntity(
                tanggal = today,
                productId = item.productId,
                jumlahDus = item.jumlahDus,
                rasioKonversi = item.rasioKonversi,
                totalPcs = totalPcs,
                hargaBeliPabrikDus = item.hargaBeliDus,
                potensiHutangPabrik = potensiHutang,
                opsiBayarMuat = item.opsiBayarMuat,
                jumlahBayarMuat = item.jumlahBayarMuat,
                sisaHutangMuat = sisaHutang,
                statusLunasHutang = isLunas,
                catatanMuat = item.catatanMuat
            )
            dao.insertDailyLoading(loading)

            // Update Drawer stok_fresh_pabrik
            val currentDrawer = dao.getDrawerByProductId(item.productId) ?: InventoryDrawerEntity(productId = item.productId)
            val updatedDrawer = currentDrawer.copy(
                stokFreshPabrikPcs = currentDrawer.stokFreshPabrikPcs + totalPcs,
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertDrawer(updatedDrawer)
        }
    }

    suspend fun payLoadingDebt(loadingId: String, bayarAmount: Double) {
        val loading = dao.getDailyLoadingById(loadingId) ?: return
        val totalBayar = loading.jumlahBayarMuat + bayarAmount
        val sisa = (loading.potensiHutangPabrik - totalBayar).coerceAtLeast(0.0)
        val isLunas = sisa <= 0.0
        dao.updateLoadingDebtPayment(loadingId, totalBayar, sisa, isLunas)
    }

    /**
     * TAHAP 2: Kunjungan Warung (Titip Baru / Tambah Stok)
     */
    suspend fun processTitipBaru(
        warung: WarungEntity,
        productId: String,
        sumberStok: String, // "FRESH_PABRIK" or "PRIBADI_REPACK"
        jumlahPcs: Int,
        hargaSatuan: Double,
        gpsLat: Double,
        gpsLng: Double,
        gpsAddress: String,
        catatan: String
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1. Kurangi stok di mobil sesuai sumber laci
        val currentDrawer = dao.getDrawerByProductId(productId) ?: InventoryDrawerEntity(productId = productId)
        val updatedDrawer = if (sumberStok == "FRESH_PABRIK") {
            currentDrawer.copy(
                stokFreshPabrikPcs = (currentDrawer.stokFreshPabrikPcs - jumlahPcs).coerceAtLeast(0),
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            currentDrawer.copy(
                stokPribadiLayakJualPcs = (currentDrawer.stokPribadiLayakJualPcs - jumlahPcs).coerceAtLeast(0),
                lastUpdated = System.currentTimeMillis()
            )
        }
        dao.insertDrawer(updatedDrawer)

        // 2. Update Warung titipan across all products
        val warungTxList = dao.getTransactionsByWarungSync(warung.id)
        val otherProductsActiveTitipan = warungTxList
            .filter { it.productId != productId }
            .groupBy { it.productId }
            .mapValues { entry -> entry.value.maxByOrNull { it.timestamp }?.totalTitipanAktifPcs ?: 0 }
            .values.sum()
        val prevTitipanThisProduct = warungTxList.filter { it.productId == productId }.maxByOrNull { it.timestamp }?.totalTitipanAktifPcs ?: 0
        val totalAktifThisProduct = prevTitipanThisProduct + jumlahPcs
        val updatedWarungTotalTitipan = otherProductsActiveTitipan + totalAktifThisProduct

        val updatedWarung = warung.copy(
            stokTitipanPcs = updatedWarungTotalTitipan,
            tglKunjunganTerakhir = System.currentTimeMillis()
        )
        dao.updateWarung(updatedWarung)

        // 3. Catat Transaksi
        val transaction = TransactionEntity(
            warungId = warung.id,
            ruteId = warung.ruteId,
            productId = productId,
            tanggal = today,
            jenis = "TITIP_BARU",
            sumberStok = sumberStok,
            sisaTitipanLaluPcs = prevTitipanThisProduct,
            sisaFisikPcs = 0,
            pcsLaku = 0,
            hargaSatuan = hargaSatuan,
            subtotalLaku = 0.0,
            saldoPiutangLama = warung.saldoPiutang,
            grandTotalTagihan = 0.0,
            uangDiterima = 0.0,
            saldoPiutangBaru = warung.saldoPiutang,
            statusBayar = "TITIP_BARU",
            bsDitarikPcs = 0,
            restockBaruPcs = jumlahPcs,
            totalTitipanAktifPcs = totalAktifThisProduct,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            gpsAddress = gpsAddress,
            catatan = catatan
        )
        dao.insertTransaction(transaction)
    }

    /**
     * TAHAP 2 Skenario B: Ganti Barang / Tarik Sisa (Minggu ke-2+)
     * 1. Sisa fisik di warung -> Pcs Laku = sisaTitipanLalu - sisaFisik
     * 2. Tagihan = (Pcs Laku * Harga) + Saldo Piutang Lama
     * 3. Bayar = Uang diterima, Sisa Piutang = Tagihan - Uang diterima
     * 4. Tarik BS = sisaFisik -> masuk stok_bs_belum_sortir
     * 5. Restock = Barang baru (Fresh/Pribadi) -> stokTitipan baru = Restock
     */
    suspend fun processTarikSisaDanRestock(
        warung: WarungEntity,
        productId: String,
        sisaTitipanLalu: Int,
        sisaFisik: Int,
        hargaSatuan: Double,
        uangDiterima: Double,
        restockPcs: Int,
        sumberRestock: String, // "FRESH_PABRIK" or "PRIBADI_REPACK"
        gpsLat: Double,
        gpsLng: Double,
        gpsAddress: String,
        catatan: String
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val pcsLaku = (sisaTitipanLalu - sisaFisik).coerceAtLeast(0)
        val subtotalLaku = pcsLaku * hargaSatuan
        val grandTotal = subtotalLaku + warung.saldoPiutang
        val saldoPiutangBaru = (grandTotal - uangDiterima).coerceAtLeast(0.0)

        val statusBayar = when {
            uangDiterima >= grandTotal -> "LUNAS"
            uangDiterima > 0 -> "SEBAGIAN"
            else -> "BON_FULL"
        }

        // Update 4 Virtual Drawers:
        // - Tambah stok_bs_belum_sortir dari BS tarikan (sisaFisik)
        // - Kurangi restock dari Fresh atau Pribadi
        val currentDrawer = dao.getDrawerByProductId(productId) ?: InventoryDrawerEntity(productId = productId)
        var freshCount = currentDrawer.stokFreshPabrikPcs
        var pribadiCount = currentDrawer.stokPribadiLayakJualPcs
        val bsCount = currentDrawer.stokBsBelumSortirPcs + sisaFisik

        if (sumberRestock == "FRESH_PABRIK") {
            freshCount = (freshCount - restockPcs).coerceAtLeast(0)
        } else {
            pribadiCount = (pribadiCount - restockPcs).coerceAtLeast(0)
        }

        dao.insertDrawer(
            currentDrawer.copy(
                stokFreshPabrikPcs = freshCount,
                stokPribadiLayakJualPcs = pribadiCount,
                stokBsBelumSortirPcs = bsCount,
                lastUpdated = System.currentTimeMillis()
            )
        )

        // Update Warung state across all products
        val warungTxList = dao.getTransactionsByWarungSync(warung.id)
        val otherProductsActiveTitipan = warungTxList
            .filter { it.productId != productId }
            .groupBy { it.productId }
            .mapValues { entry -> entry.value.maxByOrNull { it.timestamp }?.totalTitipanAktifPcs ?: 0 }
            .values.sum()
        val updatedWarungTotalTitipan = otherProductsActiveTitipan + restockPcs

        val updatedWarung = warung.copy(
            saldoPiutang = saldoPiutangBaru,
            stokTitipanPcs = updatedWarungTotalTitipan,
            tglKunjunganTerakhir = System.currentTimeMillis(),
            tglMulaiHutang = if (saldoPiutangBaru > 0 && warung.saldoPiutang == 0.0) System.currentTimeMillis() else warung.tglMulaiHutang
        )
        dao.updateWarung(updatedWarung)

        // Record Transaction
        val transaction = TransactionEntity(
            warungId = warung.id,
            ruteId = warung.ruteId,
            productId = productId,
            tanggal = today,
            jenis = "TARIK_SISA",
            sumberStok = sumberRestock,
            sisaTitipanLaluPcs = sisaTitipanLalu,
            sisaFisikPcs = sisaFisik,
            pcsLaku = pcsLaku,
            hargaSatuan = hargaSatuan,
            subtotalLaku = subtotalLaku,
            saldoPiutangLama = warung.saldoPiutang,
            grandTotalTagihan = grandTotal,
            uangDiterima = uangDiterima,
            saldoPiutangBaru = saldoPiutangBaru,
            statusBayar = statusBayar,
            bsDitarikPcs = sisaFisik,
            restockBaruPcs = restockPcs,
            totalTitipanAktifPcs = restockPcs,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            gpsAddress = gpsAddress,
            catatan = catatan
        )
        dao.insertTransaction(transaction)
    }

    /**
     * TAHAP 3: Closing Sore & Rekonsiliasi Setoran Supplier / Principal
     * Mendukung Multi-Loading (lebih dari 1x muat per hari) & Multi-Supplier (berbagai pabrik)
     */
    suspend fun processBatchClosingSore(items: List<ProductClosingInput>) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val allTodayLoadings = dao.getDailyLoadingsByDateSync(today)

        for (item in items) {
            val product = dao.getProductById(item.productId)
            val productLoadings = allTodayLoadings.filter { it.productId == item.productId }
            val rasio = productLoadings.firstOrNull()?.rasioKonversi ?: product?.rasioKonversi ?: 10
            val hargaBeliDus = productLoadings.firstOrNull()?.hargaBeliPabrikDus ?: product?.hargaBeliPabrik ?: 0.0

            val totalMuatDus = if (productLoadings.isNotEmpty()) productLoadings.sumOf { it.jumlahDus } else 0
            val totalMuatPcs = if (productLoadings.isNotEmpty()) productLoadings.sumOf { it.totalPcs } else (totalMuatDus * rasio)

            val sisaTotalPcs = (item.sisaDusSore * rasio) + item.sisaPcsLepasanSore
            val pcsTerdistribusi = (totalMuatPcs - sisaTotalPcs).coerceAtLeast(0)
            val terjualDusEquivalent = if (rasio > 0) pcsTerdistribusi.toDouble() / rasio else 0.0
            val tagihanPabrikFinal = terjualDusEquivalent * hargaBeliDus

            // Distribute closing records proportionally across all today's loadings for this product
            if (productLoadings.isNotEmpty()) {
                var remainingTerjualDus = terjualDusEquivalent
                var remainingSisaDus = item.sisaDusSore.toDouble()

                for ((idx, loading) in productLoadings.withIndex()) {
                    val isLast = idx == productLoadings.size - 1
                    val share = if (totalMuatDus > 0) loading.jumlahDus.toDouble() / totalMuatDus else (1.0 / productLoadings.size)
                    
                    val loadingTerjualDus = if (isLast) remainingTerjualDus else (terjualDusEquivalent * share)
                    val loadingSisaDus = if (isLast) remainingSisaDus else (item.sisaDusSore.toDouble() * share)
                    val loadingTagihan = loadingTerjualDus * loading.hargaBeliPabrikDus

                    remainingTerjualDus = (remainingTerjualDus - loadingTerjualDus).coerceAtLeast(0.0)
                    remainingSisaDus = (remainingSisaDus - loadingSisaDus).coerceAtLeast(0.0)

                    dao.updateDailyLoading(
                        loading.copy(
                            sisaDusSore = Math.round(loadingSisaDus).toInt(),
                            terjualDus = Math.round(loadingTerjualDus).toInt(),
                            tagihanPabrikClosing = loadingTagihan,
                            statusClosing = true
                        )
                    )
                }
            }

            // Sinkronisasi Laci Virtual Stok Fresh Mobil
            val currentDrawer = dao.getDrawerByProductId(item.productId)
            if (currentDrawer != null) {
                dao.insertDrawer(
                    currentDrawer.copy(
                        stokFreshPabrikPcs = sisaTotalPcs,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                dao.insertDrawer(
                    InventoryDrawerEntity(
                        productId = item.productId,
                        stokFreshPabrikPcs = sisaTotalPcs,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun processClosingSore(
        loadingId: String,
        sisaDusSore: Int,
        sisaPcsLepasanSore: Int = 0
    ) {
        val loading = dao.getDailyLoadingById(loadingId)
        if (loading != null) {
            val totalMuatPcs = loading.totalPcs
            val sisaTotalPcs = (sisaDusSore * loading.rasioKonversi) + sisaPcsLepasanSore
            val pcsTerdistribusi = (totalMuatPcs - sisaTotalPcs).coerceAtLeast(0)
            val terjualDusEquivalent = (pcsTerdistribusi.toDouble() / loading.rasioKonversi)
            val tagihanPabrikFinal = terjualDusEquivalent * loading.hargaBeliPabrikDus

            val updatedLoading = loading.copy(
                sisaDusSore = sisaDusSore,
                terjualDus = (loading.jumlahDus - sisaDusSore).coerceAtLeast(0),
                tagihanPabrikClosing = tagihanPabrikFinal,
                statusClosing = true
            )
            dao.updateDailyLoading(updatedLoading)

            // Sinkronisasi Laci Virtual Stok Fresh Mobil
            val currentDrawer = dao.getDrawerByProductId(loading.productId)
            if (currentDrawer != null) {
                dao.insertDrawer(
                    currentDrawer.copy(
                        stokFreshPabrikPcs = sisaTotalPcs,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun deleteLegacyClosingTransactions() {
        dao.deleteLegacyClosingTransactions()
    }

    /**
     * MANAJEMEN ASET PRIBADI: Sortir BS Repack
     * Ambil dari stok_bs_belum_sortir -> pisah ke stok_pribadi_layak_jual dan stok_pribadi_rusak
     */
    suspend fun processSortirBs(
        productId: String,
        totalBsAwal: Int,
        bsLayakJual: Int,
        bsRusak: Int,
        hargaBeliPcs: Double,
        hargaJualPcs: Double,
        catatan: String
    ) {
        val currentDrawer = dao.getDrawerByProductId(productId) ?: InventoryDrawerEntity(productId = productId)
        val newBsBelumSortir = (currentDrawer.stokBsBelumSortirPcs - (bsLayakJual + bsRusak)).coerceAtLeast(0)
        val newLayakJual = currentDrawer.stokPribadiLayakJualPcs + bsLayakJual
        val newRusak = currentDrawer.stokPribadiRusakPcs + bsRusak

        dao.insertDrawer(
            currentDrawer.copy(
                stokBsBelumSortirPcs = newBsBelumSortir,
                stokPribadiLayakJualPcs = newLayakJual,
                stokPribadiRusakPcs = newRusak,
                lastUpdated = System.currentTimeMillis()
            )
        )

        val modalTertanam = bsLayakJual * hargaBeliPcs
        val nilaiJual = bsLayakJual * hargaJualPcs
        val profitMurni = nilaiJual - modalTertanam

        dao.insertBsSortir(
            BsSortirEntity(
                productId = productId,
                totalBsAwalPcs = totalBsAwal,
                bsLayakJualPcs = bsLayakJual,
                bsRusakPcs = bsRusak,
                estimasiNilaiModal = modalTertanam,
                estimasiNilaiJual = nilaiJual,
                estimasiProfitMurni = profitMurni,
                catatan = catatan
            )
        )
    }

    /**
     * RISK MANAGEMENT: Warung Bangkrut / Blacklist & Write-Off
     */
    suspend fun processWriteOff(
        warung: WarungEntity,
        hargaSatuan: Double,
        alasan: String
    ) {
        val piutang = warung.saldoPiutang
        val stokHangus = warung.stokTitipanPcs
        val nilaiStok = stokHangus * hargaSatuan
        val totalKerugian = piutang + nilaiStok

        // 1. Insert write off
        dao.insertWriteOff(
            WriteOffEntity(
                warungId = warung.id,
                namaWarung = warung.namaWarung,
                piutangDihapus = piutang,
                stokHangusPcs = stokHangus,
                nilaiStokHangus = nilaiStok,
                totalKerugian = totalKerugian,
                alasan = alasan
            )
        )

        // 2. Set warung to Blacklist and clear debt/stock to keep ledger clean
        dao.updateWarung(
            warung.copy(
                status = "Blacklist",
                saldoPiutang = 0.0,
                stokTitipanPcs = 0,
                notes = "${warung.notes} [WRITE-OFF Rp${totalKerugian.toLong()} on ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}]"
            )
        )
    }

    suspend fun resetTransactionalDataForProduction() {
        dao.clearAllTransactions()
        dao.clearAllDailyLoadings()
        dao.clearAllBsSortirs()
        dao.clearAllWriteOffs()
        dao.resetAllWarungBalances()
        dao.resetAllDrawers()
    }

    suspend fun wipeAllDataCompletely() {
        dao.clearAllTransactions()
        dao.clearAllDailyLoadings()
        dao.clearAllBsSortirs()
        dao.clearAllWriteOffs()
        dao.clearAllWarungs()
        dao.clearAllProducts()
        dao.clearAllRutes()
        dao.clearAllPabriks()
        dao.clearAllDrawersCompletely()
    }
}
