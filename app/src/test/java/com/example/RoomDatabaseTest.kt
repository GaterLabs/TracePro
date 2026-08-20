package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.SfaDao
import com.example.data.local.entity.*
import com.example.data.repository.LoadingItemInput
import com.example.data.repository.ProductClosingInput
import com.example.data.repository.SfaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SfaDao
    private lateinit var repository: SfaRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.sfaDao()
        repository = SfaRepository(dao)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testProductAndDrawerCreation() = runBlocking {
        val product = ProductEntity(
            id = "PROD_1",
            nama = "Kacang Dua Kelinci 100g",
            kategori = "Snack",
            satuanBesar = "Dus",
            satuanKecil = "Pcs",
            rasioKonversi = 20,
            hargaBeliPabrik = 40000.0,
            hargaJualDefault = 2500.0
        )
        repository.saveProduct(product)

        val products = repository.allProducts.first()
        assertEquals(1, products.size)
        assertEquals("PROD_1", products[0].id)

        val drawers = repository.allInventoryDrawers.first()
        assertEquals(1, drawers.size)
        assertEquals(0, drawers[0].stokFreshPabrikPcs)
        assertEquals(0, drawers[0].stokBsBelumSortirPcs)
    }

    @Test
    fun testConsignmentDailyLoading() = runBlocking {
        val product = ProductEntity(
            id = "PROD_1",
            nama = "Kacang Dua Kelinci",
            rasioKonversi = 10,
            hargaBeliPabrik = 20000.0,
            hargaJualDefault = 2500.0
        )
        repository.saveProduct(product)

        // Muat 5 Dus (50 Pcs)
        repository.processBatchDailyLoading(
            listOf(
                LoadingItemInput(
                    productId = "PROD_1",
                    jumlahDus = 5,
                    rasioKonversi = 10,
                    hargaBeliDus = 20000.0,
                    opsiBayarMuat = "BAYAR_CLOSING"
                )
            )
        )

        val loadings = repository.allDailyLoadings.first()
        assertEquals(1, loadings.size)
        assertEquals(5, loadings[0].jumlahDus)
        assertEquals(50, loadings[0].totalPcs)
        assertEquals(100000.0, loadings[0].potensiHutangPabrik, 0.01)

        val drawer = dao.getDrawerByProductId("PROD_1")
        assertNotNull(drawer)
        assertEquals(50, drawer!!.stokFreshPabrikPcs)
    }

    @Test
    fun testConsignmentTitipBaruAndTarikSisaLifecycle() = runBlocking {
        val product = ProductEntity(
            id = "PROD_1",
            nama = "Kopi Sachet",
            rasioKonversi = 10,
            hargaBeliPabrik = 10000.0,
            hargaJualDefault = 1500.0
        )
        repository.saveProduct(product)

        // 1. Muat 10 Dus (100 Pcs)
        repository.processBatchDailyLoading(
            listOf(
                LoadingItemInput(
                    productId = "PROD_1",
                    jumlahDus = 10,
                    rasioKonversi = 10,
                    hargaBeliDus = 10000.0
                )
            )
        )

        val rute = RuteEntity(id = "RUTE_1", namaRute = "Senin - Kota")
        repository.saveRute(rute)

        val warung = WarungEntity(
            id = "WARUNG_1",
            namaWarung = "Toko Berkah Jaya",
            namaPemilik = "Pak Haji",
            ruteId = "RUTE_1",
            urutanKunjungan = 1,
            stokTitipanPcs = 0,
            saldoPiutang = 0.0
        )
        repository.saveWarung(warung)

        // 2. Titip Baru 20 Pcs ke Toko
        repository.processTitipBaru(
            warung = warung,
            productId = "PROD_1",
            sumberStok = "FRESH_PABRIK",
            jumlahPcs = 20,
            hargaSatuan = 1500.0,
            gpsLat = -6.2,
            gpsLng = 106.8,
            gpsAddress = "Jl. Merdeka No 1",
            catatan = "Titip awal"
        )

        val updatedWarung1 = dao.getWarungById("WARUNG_1")!!
        assertEquals(20, updatedWarung1.stokTitipanPcs)

        val drawerAfterTitip = dao.getDrawerByProductId("PROD_1")!!
        assertEquals(80, drawerAfterTitip.stokFreshPabrikPcs) // 100 - 20 = 80

        // 3. Kunjungan Siklus 2: Sisa Fisik di Toko = 5 Pcs (Laku = 15 Pcs)
        // Toko Bayar Lunas (15 * 1500 = Rp 22.500), Lalu Restock Baru 20 Pcs
        repository.processTarikSisaDanRestock(
            warung = updatedWarung1,
            productId = "PROD_1",
            sisaTitipanLalu = 20,
            sisaFisik = 5,
            hargaSatuan = 1500.0,
            uangDiterima = 22500.0,
            restockPcs = 20,
            sumberRestock = "FRESH_PABRIK",
            gpsLat = -6.2,
            gpsLng = 106.8,
            gpsAddress = "Jl. Merdeka No 1",
            catatan = "Lunas & Restock"
        )

        val updatedWarung2 = dao.getWarungById("WARUNG_1")!!
        assertEquals(20, updatedWarung2.stokTitipanPcs)
        assertEquals(0.0, updatedWarung2.saldoPiutang, 0.01)

        val drawerAfterTarik = dao.getDrawerByProductId("PROD_1")!!
        assertEquals(60, drawerAfterTarik.stokFreshPabrikPcs) // 80 - 20 = 60
        assertEquals(5, drawerAfterTarik.stokBsBelumSortirPcs) // 5 Pcs BS masuk laci BS
    }

    @Test
    fun testSortirBsManagement() = runBlocking {
        val product = ProductEntity(
            id = "PROD_1",
            nama = "Krupuk Kaleng",
            rasioKonversi = 10,
            hargaBeliPabrik = 10000.0,
            hargaJualDefault = 1500.0
        )
        repository.saveProduct(product)

        // Seed drawer with 10 BS pcs
        dao.insertDrawer(
            InventoryDrawerEntity(
                productId = "PROD_1",
                stokBsBelumSortirPcs = 10
            )
        )

        // Sortir: 8 Layak Repack, 2 Rusak/Buang
        repository.processSortirBs(
            productId = "PROD_1",
            totalBsAwal = 10,
            bsLayakJual = 8,
            bsRusak = 2,
            hargaBeliPcs = 1000.0,
            hargaJualPcs = 1500.0,
            catatan = "Repack plastik kecil"
        )

        val drawer = dao.getDrawerByProductId("PROD_1")!!
        assertEquals(0, drawer.stokBsBelumSortirPcs)
        assertEquals(8, drawer.stokPribadiLayakJualPcs)
        assertEquals(2, drawer.stokPribadiRusakPcs)

        val sortirs = repository.allBsSortirs.first()
        assertEquals(1, sortirs.size)
        assertEquals(8, sortirs[0].bsLayakJualPcs)
        assertEquals(4000.0, sortirs[0].estimasiProfitMurni, 0.01) // (8*1500) - (8*1000) = 12000 - 8000 = 4000
    }

    @Test
    fun testClosingSoreReconciliation() = runBlocking {
        val product = ProductEntity(
            id = "PROD_1",
            nama = "Biskuit Renyah",
            rasioKonversi = 10,
            hargaBeliPabrik = 20000.0,
            hargaJualDefault = 2500.0
        )
        repository.saveProduct(product)

        // Muat 10 Dus (100 Pcs)
        repository.processBatchDailyLoading(
            listOf(
                LoadingItemInput(
                    productId = "PROD_1",
                    jumlahDus = 10,
                    rasioKonversi = 10,
                    hargaBeliDus = 20000.0
                )
            )
        )

        // Sore hari sisa di mobil: 3 Dus utuh + 4 Pcs lepasan (total 34 Pcs)
        // Terdistribusi = 100 - 34 = 66 Pcs (~6.6 Dus)
        // Tagihan Pabrik = 6.6 * 20000 = Rp 132.000
        repository.processBatchClosingSore(
            listOf(
                ProductClosingInput(
                    productId = "PROD_1",
                    sisaDusSore = 3,
                    sisaPcsLepasanSore = 4
                )
            )
        )

        val loadings = repository.allDailyLoadings.first()
        assertEquals(1, loadings.size)
        assertTrue(loadings[0].statusClosing)
        assertEquals(132000.0, loadings[0].tagihanPabrikClosing, 0.01)

        val drawer = dao.getDrawerByProductId("PROD_1")!!
        assertEquals(34, drawer.stokFreshPabrikPcs)
    }

    @Test
    fun testWriteOffRiskManagement() = runBlocking {
        val warung = WarungEntity(
            id = "WARUNG_BAD",
            namaWarung = "Toko Kabur",
            saldoPiutang = 150000.0,
            stokTitipanPcs = 20
        )
        repository.saveWarung(warung)

        repository.processWriteOff(
            warung = warung,
            hargaSatuan = 2000.0,
            alasan = "Pemilik pindah keluar kota tanpa kabar"
        )

        val writeOffs = repository.allWriteOffs.first()
        assertEquals(1, writeOffs.size)
        assertEquals(190000.0, writeOffs[0].totalKerugian, 0.01) // 150.000 + (20 * 2000)

        val updatedWarung = dao.getWarungById("WARUNG_BAD")!!
        assertEquals("Blacklist", updatedWarung.status)
        assertEquals(0.0, updatedWarung.saldoPiutang, 0.01)
        assertEquals(0, updatedWarung.stokTitipanPcs)
    }

    @Test
    fun testLanguagePreferencePersistence() = runBlocking {
        val defaultProfile = repository.userProfile.first()
        val initLang = defaultProfile?.appLanguage ?: "ID"
        assertEquals("ID", initLang)

        // Change to English
        val updatedProfile = (defaultProfile ?: UserProfileEntity()).copy(appLanguage = "EN")
        repository.saveUserProfile(updatedProfile)

        val retrieved = repository.userProfile.first()
        assertNotNull(retrieved)
        assertEquals("EN", retrieved!!.appLanguage)
    }
}
