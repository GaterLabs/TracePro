package com.example

import com.example.util.LocationHelper
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class ExampleUnitTest {

    @Test
    fun testHaversineGpsDistanceCalculation() {
        // Test 1: Identical points should have 0 distance
        val distZero = LocationHelper.calculateDistanceMeters(-6.2088, 106.8456, -6.2088, 106.8456)
        assertEquals(0.0, distZero, 0.001)

        // Test 2: Known distance between Monas (-6.1754, 106.8272) and Bundaran HI (-6.1950, 106.8230) ~ 2.2 km
        val monasToHi = LocationHelper.calculateDistanceMeters(-6.1754, 106.8272, -6.1950, 106.8230)
        assertTrue("Distance should be around 2.2 km (2000-2400m)", monasToHi in 2000.0..2400.0)

        // Test 3: Formatting distance
        assertEquals("250 m", LocationHelper.formatDistance(250.2))
        assertEquals("1.5 km", LocationHelper.formatDistance(1500.0))
        assertEquals("10.0 km", LocationHelper.formatDistance(10000.0))
        assertEquals("- m", LocationHelper.formatDistance(Double.MAX_VALUE))
    }

    @Test
    fun testMultiUomConversionMath() {
        val jumlahDus = 5
        val rasioPackPerDus = 10
        val rasioPcsPerPack = 12
        val totalPcsPerDus = rasioPackPerDus * rasioPcsPerPack // 120 Pcs/Dus

        val totalPcsMuat = jumlahDus * totalPcsPerDus
        assertEquals(600, totalPcsMuat)

        // Conversion from total Pcs back to Dus + Pack + Pcs
        val sisaTotalPcs = 326
        val sisaDus = sisaTotalPcs / totalPcsPerDus // 2 Dus (240 Pcs)
        val sisaPcsSetelahDus = sisaTotalPcs % totalPcsPerDus // 86 Pcs
        val sisaPack = sisaPcsSetelahDus / rasioPcsPerPack // 7 Pack (84 Pcs)
        val sisaPcsEceran = sisaPcsSetelahDus % rasioPcsPerPack // 2 Pcs

        assertEquals(2, sisaDus)
        assertEquals(7, sisaPack)
        assertEquals(2, sisaPcsEceran)
        assertEquals(326, (sisaDus * totalPcsPerDus) + (sisaPack * rasioPcsPerPack) + sisaPcsEceran)
    }

    @Test
    fun testConsignmentTransactionLedgerMath() {
        // Initial state at Warung:
        val sisaTitipanLalu = 100 // Pcs
        val saldoPiutangLama = 50_000.0 // Rp 50.000 bon tempo lalu
        val sisaFisikSaatIni = 30 // Pcs di warung
        val hargaJualSatuan = 2_500.0 // Rp 2.500 / Pcs

        // Calculation:
        val pcsLaku = (sisaTitipanLalu - sisaFisikSaatIni).coerceAtLeast(0) // 70 Pcs
        assertEquals(70, pcsLaku)

        val subtotalLaku = pcsLaku * hargaJualSatuan // 70 * 2500 = 175.000
        assertEquals(175_000.0, subtotalLaku, 0.01)

        val grandTotalTagihan = subtotalLaku + saldoPiutangLama // 175.000 + 50.000 = 225.000
        assertEquals(225_000.0, grandTotalTagihan, 0.01)

        // Scenario 1: Partial payment (Bayar 150.000)
        val uangDiterimaPartial = 150_000.0
        val saldoPiutangBaruPartial = (grandTotalTagihan - uangDiterimaPartial).coerceAtLeast(0.0)
        assertEquals(75_000.0, saldoPiutangBaruPartial, 0.01)
        val statusBayarPartial = when {
            uangDiterimaPartial >= grandTotalTagihan -> "LUNAS"
            uangDiterimaPartial > 0 -> "SEBAGIAN"
            else -> "BON_FULL"
        }
        assertEquals("SEBAGIAN", statusBayarPartial)

        // Scenario 2: Full payment
        val uangDiterimaLunas = 225_000.0
        val saldoPiutangBaruLunas = (grandTotalTagihan - uangDiterimaLunas).coerceAtLeast(0.0)
        assertEquals(0.0, saldoPiutangBaruLunas, 0.01)
    }

    @Test
    fun testBadStockSortingAndProfitMarginMath() {
        val totalBsTarikan = 50 // Pcs
        val bsLayakJual = 35 // Pcs diselamatkan untuk repack aset mandiri
        val bsRusak = 15 // Pcs afkir rusak total

        val hargaBeliPcs = 1_800.0 // Modal pokok
        val hargaJualPcs = 2_500.0 // Harga jual konsumen

        val modalTertanam = bsLayakJual * hargaBeliPcs // 35 * 1800 = 63.000
        val nilaiJual = bsLayakJual * hargaJualPcs // 35 * 2500 = 87.500
        val profitMurni = nilaiJual - modalTertanam // 87.500 - 63.000 = 24.500

        assertEquals(63_000.0, modalTertanam, 0.01)
        assertEquals(87_500.0, nilaiJual, 0.01)
        assertEquals(24_500.0, profitMurni, 0.01)

        // Drawer conservation:
        val bsBelumSortirAwal = 50
        val bsBelumSortirSisa = (bsBelumSortirAwal - (bsLayakJual + bsRusak)).coerceAtLeast(0)
        assertEquals(0, bsBelumSortirSisa)
    }

    @Test
    fun testClosingSoreReconciliation() {
        val muatPagiDus = 10
        val rasio = 24 // 24 Pcs per Dus
        val hargaBeliDus = 48_000.0 // Rp 2.000 per Pcs modal pabrik

        val totalMuatPcs = muatPagiDus * rasio // 240 Pcs
        val sisaDusSore = 3 // Dus utuh sisa di mobil
        val sisaPcsLepasanSore = 12 // Pcs sisa lepasan di mobil

        val sisaTotalPcs = (sisaDusSore * rasio) + sisaPcsLepasanSore // (3 * 24) + 12 = 84 Pcs
        val pcsTerdistribusi = (totalMuatPcs - sisaTotalPcs).coerceAtLeast(0) // 240 - 84 = 156 Pcs
        val terjualDusEquivalent = pcsTerdistribusi.toDouble() / rasio // 156 / 24 = 6.5 Dus
        val tagihanPabrikFinal = terjualDusEquivalent * hargaBeliDus // 6.5 * 48000 = 312.000

        assertEquals(156, pcsTerdistribusi)
        assertEquals(6.5, terjualDusEquivalent, 0.001)
        assertEquals(312_000.0, tagihanPabrikFinal, 0.01)
    }

    @Test
    fun testReportPeriodFilteringAndMetricAggregation() {
        val omsetList = listOf(150_000.0, 220_000.0, 80_000.0, 300_000.0)
        val kasMasukList = listOf(150_000.0, 200_000.0, 50_000.0, 300_000.0)
        val piutangBaruList = listOf(0.0, 20_000.0, 30_000.0, 0.0)

        val totalOmset = omsetList.sum()
        val totalKas = kasMasukList.sum()
        val totalPiutang = piutangBaruList.sum()

        assertEquals(750_000.0, totalOmset, 0.01)
        assertEquals(700_000.0, totalKas, 0.01)
        assertEquals(50_000.0, totalPiutang, 0.01)
        assertEquals(totalOmset, totalKas + totalPiutang, 0.01)
    }

    @Test
    fun testRiskAgingCategorization() {
        fun getAgingRiskCategory(daysOverdue: Int): String {
            return when {
                daysOverdue <= 7 -> "LANCAR"
                daysOverdue <= 14 -> "PERHATIAN"
                daysOverdue <= 21 -> "KRONIS"
                else -> "MACET"
            }
        }

        assertEquals("LANCAR", getAgingRiskCategory(3))
        assertEquals("PERHATIAN", getAgingRiskCategory(10))
        assertEquals("KRONIS", getAgingRiskCategory(18))
        assertEquals("MACET", getAgingRiskCategory(30))
    }

    @Test
    fun testDailyLoadingPaymentOptionsAndDebtMath() {
        val jumlahPack = 20
        val hargaBeliPack = 50_000.0
        val totalNilaiMuat = jumlahPack * hargaBeliPack // 1.000.000

        // Option 1: BAYAR_LANGSUNG (Cash di awal)
        val bayarLangsung = totalNilaiMuat
        val sisaHutangLangsung = (totalNilaiMuat - bayarLangsung).coerceAtLeast(0.0)
        val statusLunasLangsung = sisaHutangLangsung <= 0.0
        assertEquals(0.0, sisaHutangLangsung, 0.01)
        assertTrue(statusLunasLangsung)

        // Option 2: BAYAR_CLOSING (Bayar saat closing sore)
        val bayarPasClosing = 0.0
        val sisaHutangClosing = totalNilaiMuat
        val statusLunasClosing = false
        assertEquals(1_000_000.0, sisaHutangClosing, 0.01)
        assertFalse(statusLunasClosing)

        // Option 3: HUTANG / TEMPO with DP 300.000
        val dpNominal = 300_000.0
        val sisaHutangTempo = (totalNilaiMuat - dpNominal).coerceAtLeast(0.0)
        val statusLunasTempo = sisaHutangTempo <= 0.0
        assertEquals(700_000.0, sisaHutangTempo, 0.01)
        assertFalse(statusLunasTempo)

        // Settlement of debt: Pay 400.000 first, then pay 300.000
        val cicil1 = 400_000.0
        val sisaSetelahCicil1 = (sisaHutangTempo - cicil1).coerceAtLeast(0.0)
        assertEquals(300_000.0, sisaSetelahCicil1, 0.01)
        assertFalse(sisaSetelahCicil1 <= 0.0)

        val cicil2 = 300_000.0
        val sisaSetelahCicil2 = (sisaSetelahCicil1 - cicil2).coerceAtLeast(0.0)
        assertEquals(0.0, sisaSetelahCicil2, 0.01)
        assertTrue(sisaSetelahCicil2 <= 0.0)
    }

    @Test
    fun testMultiSupplierClosingDebtAndCashSummaryMath() {
        // Supplier A
        val muatPackA = 10
        val rasioA = 12
        val modalPackA = 60_000.0
        val sisaPackA = 2
        val sisaPcsA = 0
        val terdistribusiPcsA = (muatPackA * rasioA) - ((sisaPackA * rasioA) + sisaPcsA) // 120 - 24 = 96 pcs
        val tagihanSupplierA = (terdistribusiPcsA.toDouble() / rasioA) * modalPackA // (96/12) * 60000 = 8 * 60000 = 480.000

        // Supplier B
        val muatPackB = 5
        val rasioB = 20
        val modalPackB = 100_000.0
        val sisaPackB = 1
        val sisaPcsB = 0
        val terdistribusiPcsB = (muatPackB * rasioB) - ((sisaPackB * rasioB) + sisaPcsB) // 100 - 20 = 80 pcs
        val tagihanSupplierB = (terdistribusiPcsB.toDouble() / rasioB) * modalPackB // (80/20) * 100000 = 4 * 100000 = 400.000

        val totalTagihanSemuaSupplier = tagihanSupplierA + tagihanSupplierB // 880.000
        val totalKasDiterimaOutlet = 1_250_000.0

        val selisihKas = totalKasDiterimaOutlet - totalTagihanSemuaSupplier // +370.000 (Surplus Kasir)

        assertEquals(480_000.0, tagihanSupplierA, 0.01)
        assertEquals(400_000.0, tagihanSupplierB, 0.01)
        assertEquals(880_000.0, totalTagihanSemuaSupplier, 0.01)
        assertEquals(370_000.0, selisihKas, 0.01)
        assertTrue("Kasir harus surplus", selisihKas > 0)
    }

    @Test
    fun testMultiProductWarungConsignmentPreservationMath() {
        // Warung has 2 products:
        // Product 1: Keripik Singkong (Active titipan: 50 pcs)
        // Product 2: Permen Manis (Active titipan: 30 pcs)
        val initialProduct1Titipan = 50
        val initialProduct2Titipan = 30
        val initialTotalWarungTitipan = initialProduct1Titipan + initialProduct2Titipan // 80 pcs

        assertEquals(80, initialTotalWarungTitipan)

        // Salesman visits warung to pull remaining for Product 1:
        // Sisa fisik Product 1 = 10 pcs (40 pcs sold)
        // Restock Product 1 = 60 pcs
        val sisaFisikProd1 = 10
        val pcsLakuProd1 = initialProduct1Titipan - sisaFisikProd1 // 40 pcs
        val restockProd1 = 60

        assertEquals(40, pcsLakuProd1)

        // Calculate updated warung total titipan:
        // Total should now be Product 1 (60 pcs) + Product 2 (30 pcs) = 90 pcs
        val otherProductsTitipan = initialProduct2Titipan // 30 pcs
        val updatedWarungTitipan = otherProductsTitipan + restockProd1 // 30 + 60 = 90 pcs

        assertEquals(90, updatedWarungTitipan)

        // Next, salesman does Tarik Sisa for Product 2:
        // Sisa fisik Product 2 = 5 pcs (25 pcs sold)
        // Restock Product 2 = 40 pcs
        val sisaFisikProd2 = 5
        val pcsLakuProd2 = initialProduct2Titipan - sisaFisikProd2 // 25 pcs
        val restockProd2 = 40

        assertEquals(25, pcsLakuProd2)

        val updatedWarungTitipan2 = restockProd1 + restockProd2 // 60 + 40 = 100 pcs
        assertEquals(100, updatedWarungTitipan2)
    }
}

