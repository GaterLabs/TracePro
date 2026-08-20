package com.example.data.ai

import com.example.data.local.entity.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object AiPromptBuilder {

    /**
     * INTI SISTEM (HARDCODED DOMAIN BASE PERSONA)
     * Tidak dapat dihilangkan oleh user, bertindak sebagai jangkar domain SFA Konsinyasi FMCG.
     */
    const val HARDCODED_BASE_SYSTEM_PERSONA = """
Kamu adalah "TracerPro AI Copilot", asisten cerdas, taktis, dan analis bisnis operasional untuk Salesman & Distribusi Konsinyasi FMCG Motoris di Indonesia.

ATURAN UTAMA & PENGETAHUAN DOMAIN:
1. SISTEM 4 LACI STOK MOBIL/MOTOR:
   - Fresh Pabrik: Stok titipan resmi supplier/distributor. Penjualan barang ini wajib disetor ke kasir pabrik/distributor saat closing sore.
   - Retur Belum Sortir: Retur barang rusak/tarikan yang ditarik dari warung kelontong, menunggu proses sortir.
   - Pribadi Layak Jual (Aset Repack): Barang retur yang berhasil disortir & dikemas ulang menjadi layak jual. Hasil penjualannya 100% menjadi hak LABA BERSIH MURNI salesman.
   - Pribadi Rusak (Musnah/Hangus): Barang rusak parah yang dibuang dan menjadi beban rugi buku (write-off).
2. MANAJEMEN PIUTANG & RISIKO OUTLET:
   - Bon Piutang: Uang titipan yang belum dibayar tunai oleh pemilik warung.
   - Usia Piutang (Aging): >7 hari = Perhatian / Tempo, >14 hari = Kritis / Wajib Tagih Tunai, >30 hari = Berisiko Macet.
   - Limit Kredit: Jika saldo piutang mendekati/melebihi limit, sarankan penagihan ketat sebelum menambah drop barang baru.
3. EFISIENSI MUATAN & LOGISTIK:
   - Analisis rasio muatan vs barang laku (Sell-Through Rate).
   - Berikan rekomendasi muatan pagi (Daily Loading) berdasarkan data historis penjualan rute.
4. GAYA KOMUNIKASI:
   - Gunakan Bahasa Indonesia yang taktis, ramah, profesional, mudah dipahami di lapangan.
   - Format semua angka nominal uang dalam standar Rupiah Indonesia (contoh: Rp 1.250.000).
   - Berikan jawaban yang langsung to the point, berbasis angka riil, dan memberikan rekomendasi tindakan nyata (actionable insights).
   - Jika diminta membuatkan draft pesan WhatsApp ke Bos/Distributor/Pemilik Warung, sediakan format teks rapi dengan bullet points dan emoji yang elegan.
"""

    fun buildSystemPrompt(customPersona: String): String {
        val base = HARDCODED_BASE_SYSTEM_PERSONA.trimIndent()
        return if (customPersona.isNotBlank()) {
            """
$base

--------------------------------------------------
[INSTRUKSI & PERSONA TAMBAHAN DARI PENGGUNA]:
$customPersona
--------------------------------------------------
(Catatan: Terapkan persona/instruksi di atas dengan tetap mematuhi prinsip akurasi data dan logika bisnis konsinyasi di atas).
""".trimIndent()
        } else {
            base
        }
    }

    /**
     * Membangun prompt konteks LENGKAP mencakup seluruh isi database (11 entitas DB):
     * Profile, Pabrik/Supplier, Produk & Harga, Rute, Warung & Aging/Piutang, Custom Price,
     * Stok 4 Laci, Riwayat Muat Pagi (Loading Hari Ini & Historis), Riwayat Transaksi Penjualan,
     * Hasil Sortir Retur, dan Catatan Write-Off.
     */
    fun buildCopilotContextPrompt(
        profile: UserProfileEntity?,
        pabriks: List<PabrikEntity>,
        products: List<ProductEntity>,
        rutes: List<RuteEntity>,
        warungs: List<WarungEntity>,
        customPrices: List<WarungCustomPriceEntity>,
        drawers: List<InventoryDrawerEntity>,
        allLoadings: List<DailyLoadingEntity>,
        transactions: List<TransactionEntity>,
        sortirs: List<BsSortirEntity>,
        writeOffs: List<WriteOffEntity>
    ): String {
        val todayDate = Date()
        val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(todayDate)
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(todayDate)

        val productMap = products.associateBy { it.id }
        val warungMap = warungs.associateBy { it.id }
        val ruteMap = rutes.associateBy { it.id }
        val pabrikMap = pabriks.associateBy { it.id }

        // Filter Transaksi Hari Ini vs Keseluruhan
        val todayTxs = transactions.filter { it.tanggal == todayIso || it.tanggal.startsWith(todayIso) }
        val totalOmsetHariIni = todayTxs.sumOf { it.subtotalLaku }
        val totalKasHariIni = todayTxs.sumOf { it.uangDiterima }
        val totalPiutangBaruHariIni = todayTxs.sumOf { it.saldoPiutangBaru }
        val totalPcsLakuHariIni = todayTxs.sumOf { it.pcsLaku }
        val totalBsDitarikHariIni = todayTxs.sumOf { it.bsDitarikPcs }
        val totalTitipBaruHariIni = todayTxs.sumOf { it.restockBaruPcs }

        // Filter Muatan Hari Ini
        val todayLoadings = allLoadings.filter { it.tanggal == todayIso || it.tanggal.startsWith(todayIso) }
        val totalMuatPcsHariIni = todayLoadings.sumOf { it.totalPcs }
        val totalNilaiMuatHariIni = todayLoadings.sumOf { it.potensiHutangPabrik }

        // Total Piutang & Statistik Warung
        val totalPiutangSemuaWarung = warungs.sumOf { it.saldoPiutang }
        val warungBerhutang = warungs.filter { it.saldoPiutang > 0 }.sortedByDescending { it.saldoPiutang }
        val warungKritis = warungs.filter {
            val days = (System.currentTimeMillis() - it.tglKunjunganTerakhir) / (1000 * 60 * 60 * 24)
            it.saldoPiutang > 0 && days >= 7
        }

        // Total Stok 4 Laci Mobil
        val totalStokFresh = drawers.sumOf { it.stokFreshPabrikPcs }
        val totalStokBsUnsorted = drawers.sumOf { it.stokBsBelumSortirPcs }
        val totalStokPribadi = drawers.sumOf { it.stokPribadiLayakJualPcs }
        val totalStokRusak = drawers.sumOf { it.stokPribadiRusakPcs }
        val totalProfitSortir = sortirs.sumOf { it.estimasiProfitMurni }
        val totalWriteOff = writeOffs.sumOf { it.totalKerugian }

        val salesName = profile?.namaSalesman?.ifBlank { "Salesman" } ?: "Salesman"
        val distName = profile?.namaDistributor?.ifBlank { "Distributor" } ?: "Distributor"
        val vehicle = profile?.platNomorMobil?.ifBlank { "-" } ?: "-"
        val userPhone = profile?.noHp?.ifBlank { "-" } ?: "-"

        // Rincian Supplier / Pabrik
        val pabrikSummary = if (pabriks.isEmpty()) {
            "- Belum ada data pabrik terdaftar"
        } else {
            pabriks.joinToString("\n") { p ->
                val prodCount = products.count { it.pabrikId == p.id }
                "- [ID: ${p.id}] ${p.namaPabrik} | CP: ${p.namaCp.ifBlank { "-" }} (${p.noHpCp.ifBlank { "-" }}) | Alamat: ${p.alamatLengkap.ifBlank { "-" }} ($prodCount SKU)"
            }
        }

        // Rincian Master Katalog Produk
        val productSummary = if (products.isEmpty()) {
            "- Belum ada produk terdaftar di katalog"
        } else {
            products.joinToString("\n") { p ->
                val supp = pabrikMap[p.pabrikId]?.namaPabrik ?: "Tanpa Supplier"
                "- [ID: ${p.id}] ${p.nama} (${p.kategori}) | 1 ${p.satuanBesar} = ${p.rasioKonversi} ${p.satuanKecil} | Beli: ${formatRupiah(p.hargaBeliPabrik)}/${p.satuanBesar} | Jual Default: ${formatRupiah(p.hargaJualDefault)}/${p.satuanKecil} | Pabrik: $supp"
            }
        }

        // Rincian Rute Kunjungan
        val ruteSummary = if (rutes.isEmpty()) {
            "- Belum ada rute terdaftar"
        } else {
            rutes.joinToString("\n") { r ->
                val outletCount = warungs.count { it.ruteId == r.id }
                "- [ID: ${r.id}] ${r.namaRute} (Jadwal: ${r.hariKunjungan}) | $outletCount Outlet"
            }
        }

        // Rincian Stok 4 Laci Mobil Realtime per SKU
        val drawerDetails = if (drawers.isEmpty()) {
            "- Belum ada catatan stok di mobil"
        } else {
            drawers.joinToString("\n") { d ->
                val p = productMap[d.productId]
                "- ${p?.nama ?: d.productId}: Fresh Pabrik=${d.stokFreshPabrikPcs} ${p?.satuanKecil ?: "pcs"}, Repack Pribadi=${d.stokPribadiLayakJualPcs}, Retur Belum Sortir=${d.stokBsBelumSortirPcs}, Rusak Hangus=${d.stokPribadiRusakPcs}"
            }
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Rincian Muat Pagi Hari Ini (Daily Loading Breakdown)
        val todayLoadingDetails = if (todayLoadings.isEmpty()) {
            "- Belum ada muat pagi yang dicatat hari ini"
        } else {
            todayLoadings.joinToString("\n") { l ->
                val p = productMap[l.productId]
                val timeStr = timeFormat.format(Date(l.createdAt))
                "- ${p?.nama ?: l.productId}: ${l.jumlahDus} Dus (${l.rasioKonversi} pcs/dus) = Total ${l.totalPcs} Pcs (Modal: ${formatRupiah(l.potensiHutangPabrik)}) [Pukul $timeStr]"
            }
        }

        // Rincian Transaksi Penjualan Hari Ini
        val todayTxDetails = if (todayTxs.isEmpty()) {
            "- Belum ada transaksi penjualan yang dicatat hari ini"
        } else {
            todayTxs.joinToString("\n") { tx ->
                val w = warungMap[tx.warungId]
                val p = productMap[tx.productId]
                val wName = w?.namaWarung ?: tx.warungId
                val pName = p?.nama ?: tx.productId
                val timeStr = timeFormat.format(Date(tx.timestamp))
                "- [$timeStr] $wName: $pName | Laku: ${tx.pcsLaku} pcs (${formatRupiah(tx.subtotalLaku)}) | Kas: ${formatRupiah(tx.uangDiterima)} | Bon Baru: ${formatRupiah(tx.saldoPiutangBaru)} | Drop Baru: ${tx.restockBaruPcs} pcs | Tarik Retur: ${tx.bsDitarikPcs} pcs | Sumber: ${tx.sumberStok} | Status: ${tx.statusBayar}"
            }
        }

        // Rincian Seluruh Warung / Outlet di Database (Ringkas & Informatif)
        val warungSummary = if (warungs.isEmpty()) {
            "- Belum ada warung terdaftar"
        } else {
            warungs.joinToString("\n") { w ->
                val r = ruteMap[w.ruteId]?.namaRute ?: "Tanpa Rute"
                val days = if (w.tglKunjunganTerakhir > 0) {
                    "${(System.currentTimeMillis() - w.tglKunjunganTerakhir) / (1000 * 60 * 60 * 24)} hari lalu"
                } else "Belum pernah"
                val gpsStatus = if (w.latitude != 0.0 || w.longitude != 0.0) "GPS Ada" else "Tanpa GPS"
                "- [ID: ${w.id}] ${w.namaWarung} (${w.namaPemilik.ifBlank { "Owner" }}) | Rute: $r | Kat: ${w.kategoriWarung} | Piutang: ${formatRupiah(w.saldoPiutang)} (Limit: ${formatRupiah(w.limitHutangMaksimal)}) | Titipan: ${w.stokTitipanPcs} pcs | Kunjungan Terakhir: $days | $gpsStatus"
            }
        }

        // Daftar Harga Khusus Outlet
        val customPricesSummary = if (customPrices.isEmpty()) {
            "- Semua outlet menggunakan harga jual standar katalog"
        } else {
            customPrices.joinToString("\n") { cp ->
                val w = warungMap[cp.warungId]?.namaWarung ?: cp.warungId
                val p = productMap[cp.productId]?.nama ?: cp.productId
                "- $w: $p -> Khusus ${formatRupiah(cp.hargaJualPcs)}"
            }
        }

        // Ringkasan Historis Keseluruhan
        val allTimeOmset = transactions.sumOf { it.subtotalLaku }
        val allTimeKas = transactions.sumOf { it.uangDiterima }
        val allTimePcsLaku = transactions.sumOf { it.pcsLaku }
        val allTimeBsDitarik = transactions.sumOf { it.bsDitarikPcs }
        val allTimeLoadingsPcs = allLoadings.sumOf { it.totalPcs }

        return """
==================================================
DATABASE LENGKAP OPERASIONAL REALTIME ($todayStr)
==================================================

[1. PROFIL PENGGUNA & DISTRIBUTOR]
- Salesman: $salesName | No. HP: $userPhone
- Depo / Distributor: $distName
- Armada Kendaraan: $vehicle

[2. MASTER SUPPLIER / PABRIK (${pabriks.size} Pabrik)]
$pabrikSummary

[3. MASTER KATALOG PRODUK & HARGA (${products.size} SKU)]
$productSummary

[4. MASTER RUTE DISTRIBUSI (${rutes.size} Rute)]
$ruteSummary

[5. STATUS 4 LACI STOK MOBIL SAAT INI]
- Total Fresh Pabrik: $totalStokFresh pcs
- Total Repack Pribadi Layak Jual: $totalStokPribadi pcs (Laba Murni Salesman)
- Total Retur Belum Sortir: $totalStokBsUnsorted pcs
- Total Rusak Hangus: $totalStokRusak pcs
Rincian Stok per Produk:
$drawerDetails

[6. RIWAYAT MUAT PAGI / DAILY LOADING HARI INI]
- Total Dimuat Hari Ini: $totalMuatPcsHariIni pcs (Nilai Modal: ${formatRupiah(totalNilaiMuatHariIni)})
Rincian Muatan Hari Ini:
$todayLoadingDetails

[7. PERFORMA PENJUALAN HARI INI ($todayStr)]
- Kunjungan / Transaksi: ${todayTxs.size} Transaksi
- Total Penjualan Laku: ${formatRupiah(totalOmsetHariIni)} ($totalPcsLakuHariIni pcs)
- Kas Tunai Terkumpul: ${formatRupiah(totalKasHariIni)}
- Piutang / Bon Baru Hari Ini: ${formatRupiah(totalPiutangBaruHariIni)}
- Titip Baru Hari Ini: $totalTitipBaruHariIni pcs
- Retur Ditarik Hari Ini: $totalBsDitarikHariIni pcs
Rincian Transaksi Hari Ini:
$todayTxDetails

[8. DATABASE OUTLET & SALDO PIUTANG (${warungs.size} Warung)]
- Total Keseluruhan Piutang Beredar di Lapangan: ${formatRupiah(totalPiutangSemuaWarung)} (pada ${warungBerhutang.size} warung)
- Warung Status Kritis (>7 hari ada bon belum lunas): ${warungKritis.size} warung
Daftar Lengkap Outlet:
$warungSummary

[9. DAFTAR HARGA KHUSUS OUTLET (${customPrices.size} Aturan)]
$customPricesSummary

[10. REKAP HISTORIS GLOBAL & SORTIR / WRITE-OFF]
- Total Keseluruhan Transaksi Tercatat: ${transactions.size} transaksi
- Total Historis Omset Penjualan: ${formatRupiah(allTimeOmset)} ($allTimePcsLaku pcs)
- Total Historis Kas Masuk: ${formatRupiah(allTimeKas)}
- Total Historis Retur Ditarik: $allTimeBsDitarik pcs
- Total Historis Muat Gudang: $allTimeLoadingsPcs pcs
- Total Akumulasi Laba Sortir Repack Pribadi: ${formatRupiah(totalProfitSortir)} (${sortirs.size} sesi sortir)
- Total Kerugian Write-Off / Rusak Musnah: ${formatRupiah(totalWriteOff)} (${writeOffs.size} kali pemusnahan)
==================================================
""".trimIndent()
    }

    /**
     * Membangun prompt rekomendasi khusus untuk satu outlet tertentu
     * dengan menyertakan seluruh riwayat transaksi warung tersebut, stok mobil yang tersedia,
     * harga khusus, jadwal rute, dan peluang cross-selling.
     */
    fun buildOutletRecommendationPrompt(
        warung: WarungEntity,
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>,
        customPrices: List<WarungCustomPriceEntity>,
        drawers: List<InventoryDrawerEntity>,
        rutes: List<RuteEntity>
    ): String {
        val daysSinceVisit = if (warung.tglKunjunganTerakhir > 0) {
            "${(System.currentTimeMillis() - warung.tglKunjunganTerakhir) / (1000 * 60 * 60 * 24)} hari yang lalu"
        } else "Belum pernah dikunjungi"

        val warungTx = transactions.filter { it.warungId == warung.id }
        val totalPcsLaku = warungTx.sumOf { it.pcsLaku }
        val totalOmset = warungTx.sumOf { it.subtotalLaku }
        val totalKas = warungTx.sumOf { it.uangDiterima }
        val totalBs = warungTx.sumOf { it.bsDitarikPcs }

        val productMap = products.associateBy { it.id }
        val drawerMap = drawers.associateBy { it.productId }
        val ruteName = rutes.find { it.id == warung.ruteId }?.namaRute ?: "Tanpa Rute"

        val soldProductIds = warungTx.map { it.productId }.toSet()
        val unsoldProducts = products.filter { it.id !in soldProductIds }

        val customPriceList = customPrices.filter { it.warungId == warung.id }.joinToString(", ") { cp ->
            val p = productMap[cp.productId]
            "${p?.nama ?: "Produk"}: ${formatRupiah(cp.hargaJualPcs)}"
        }

        val warungHistoryDetails = if (warungTx.isEmpty()) {
            "- Belum ada riwayat transaksi dengan warung ini."
        } else {
            val dateTimeFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            warungTx.takeLast(10).joinToString("\n") { tx ->
                val p = productMap[tx.productId]?.nama ?: tx.productId
                val timeStr = dateTimeFormat.format(Date(tx.timestamp))
                "- [$timeStr] $p | Laku: ${tx.pcsLaku} pcs (${formatRupiah(tx.subtotalLaku)}) | Kas: ${formatRupiah(tx.uangDiterima)} | Bon Baru: ${formatRupiah(tx.saldoPiutangBaru)} | Retur: ${tx.bsDitarikPcs} pcs"
            }
        }

        val availableStockInCar = products.joinToString("\n") { p ->
            val d = drawerMap[p.id]
            "- ${p.nama}: Fresh Pabrik=${d?.stokFreshPabrikPcs ?: 0} ${p.satuanKecil}, Repack Pribadi=${d?.stokPribadiLayakJualPcs ?: 0} ${p.satuanKecil}"
        }

        return """
Bantu berikan analisis taktis dan saran cerdas (Smart Restock, Cross-Selling, & Manajemen Piutang) untuk outlet ini:

DATA LENGKAP WARUNG:
- Nama Warung: ${warung.namaWarung} (Pemilik: ${warung.namaPemilik.ifBlank { "-" }})
- Rute: $ruteName | Kategori: ${warung.kategoriWarung}
- No. HP: ${warung.noHp.ifBlank { "-" }}
- Alamat / Lokasi: ${warung.alamatLengkap} (Koordinat: ${warung.latitude}, ${warung.longitude})
- Kunjungan Terakhir: $daysSinceVisit
- Stok Titipan Aktif Saat Ini: ${warung.stokTitipanPcs} pcs
- Saldo Bon / Piutang Saat Ini: ${formatRupiah(warung.saldoPiutang)} (Limit Maks: ${formatRupiah(warung.limitHutangMaksimal)})
- Catatan Khusus Toko: ${warung.notes.ifBlank { "Tidak ada" }}
- Harga Khusus: ${customPriceList.ifBlank { "Standar Default Katalog" }}

HISTORIS PERFORMA DI WARUNG INI:
- Total Transaksi: ${warungTx.size} transaksi
- Total Volume Laku: $totalPcsLaku pcs (Omset: ${formatRupiah(totalOmset)})
- Total Uang Kas Masuk: ${formatRupiah(totalKas)}
- Total Retur Ditarik: $totalBs pcs
10 Transaksi Terakhir di Toko Ini:
$warungHistoryDetails

STOK TERSEDIA DI MOBIL SAAT INI (JANGAN SARANKAN JIKA STOK 0):
$availableStockInCar

PRODUK KATALOG YANG BELUM PERNAH DITITIPKAN DI TOKO INI:
${if (unsoldProducts.isEmpty()) "- Semua produk katalog sudah pernah dititipkan" else unsoldProducts.take(5).joinToString("\n") { "- ${it.nama} (Kategori: ${it.kategori}, Harga Jual: ${formatRupiah(it.hargaJualDefault)})" }}

TUGAS AI:
1. Berikan rekomendasi jumlah titip barang (Restock Quantity) per produk yang realistis dan siap drop hari ini.
2. Berikan 1-2 rekomendasi Cross-Selling produk baru yang cocok dengan kategori toko ini beserta cara salesman menawarkan ke pemilik toko.
3. Berikan saran penagihan / mitigasi risiko piutang berdasarkan saldo bon dan limit kredit toko.
Jawab dengan ringkas, padat, menggunakan bullet points dan Bahasa Indonesia taktis.
""".trimIndent()
    }

    fun formatRupiah(number: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(number).replace("Rp", "Rp ")
    }
}

