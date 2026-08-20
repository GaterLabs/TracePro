package com.example.util

import androidx.compose.runtime.compositionLocalOf
import com.example.ui.viewmodel.OutletFilterAging
import com.example.ui.viewmodel.OutletSortBy

val LocalAppLanguage = compositionLocalOf { "ID" }

enum class LanguageCode(val code: String, val displayName: String, val flag: String) {
    ID("ID", "Bahasa Indonesia", "🇮🇩"),
    EN("EN", "English", "🇺🇸");

    companion object {
        fun fromCode(code: String): LanguageCode {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ID
        }
    }
}

object AppStrings {

    fun tr(idText: String, enText: String, lang: String = "ID"): String {
        return if (lang.equals("EN", ignoreCase = true)) enText else idText
    }

    // Common Nav Labels
    fun navTransaksi(lang: String) = tr("Transaksi", "Transactions", lang)
    fun navRiwayat(lang: String) = tr("Riwayat", "History", lang)
    fun navDashboard(lang: String) = tr("Dashboard", "Dashboard", lang)
    fun navMaster(lang: String) = tr("Master Data", "Master Data", lang)
    fun navLaporan(lang: String) = tr("Laporan", "Reports", lang)
    fun navUtilitas(lang: String) = tr("Utilitas", "Settings", lang)

    // Drawer Subtitles
    fun subTransaksi(lang: String) = tr("Kunjungan outlet, titip baru & restock", "Outlet visits, drops & restock", lang)
    fun subRiwayat(lang: String) = tr("Audit trail lengkap muat, closing & transaksi", "Full audit log of loads, closing & visits", lang)
    fun subDashboard(lang: String) = tr("Volume, omset kas & laci stok", "Volume, cash collection & stock drawers", lang)
    fun subMaster(lang: String) = tr("Produk UOM, Outlet, Rute & Supplier", "Product SKUs, Outlets, Routes & Suppliers", lang)
    fun subLaporan(lang: String) = tr("4 Laci Stok, Setoran & Piutang Bon", "4 Stock Drawers, Settlement & Debts", lang)
    fun subUtilitas(lang: String) = tr("Profil, Bahasa, Backup JSON & Konfigurasi", "Profile, Language, JSON Backup & Config", lang)

    // Actions
    fun actionMuatPagi(lang: String) = tr("Muat Barang Pagi (Loading)", "Morning Stock Loading", lang)
    fun actionSortirBs(lang: String) = tr("Sortir Barang Rusak (BS)", "Damaged Goods Sorting (BS)", lang)
    fun actionExportBackup(lang: String) = tr("Ekspor Backup Modular (JSON)", "Export Modular Backup (JSON)", lang)
    fun actionImportBackup(lang: String) = tr("Pulihkan Data (Import JSON)", "Restore Data (Import JSON)", lang)
    fun actionGpsTool(lang: String) = tr("Alat GPS & Titik Koordinat", "GPS & Coordinates Tool", lang)
    fun actionClosingSore(lang: String) = tr("Closing Sore & Rekap Harian", "Evening Closing & Settlement", lang)

    // Status Badges
    fun statusLunas(lang: String) = tr("LUNAS", "PAID", lang)
    fun statusSebagian(lang: String) = tr("SEBAGIAN", "PARTIAL", lang)
    fun statusBonFull(lang: String) = tr("BON / TEMPO", "CREDIT / UNPAID", lang)
    fun statusTitipBaru(lang: String) = tr("TITIP BARU", "NEW DROP", lang)
    fun statusTarikSisa(lang: String) = tr("ISI ULANG / TARIK", "REFILL / RETURN", lang)
    fun statusClosing(lang: String) = tr("CLOSING SORE", "EVENING CLOSING", lang)
    fun statusMuat(lang: String) = tr("MUAT PAGI", "MORNING LOAD", lang)
    fun statusSortir(lang: String) = tr("SORTIR RETUR", "RETURN SORTING", lang)
    fun statusWriteOff(lang: String) = tr("HAPUS BUKU", "WRITE OFF", lang)
    fun statusBayarHutang(lang: String) = tr("BAYAR SUPPLIER", "SUPPLIER PAYMENT", lang)

    // Language Section
    fun langSettingTitle(lang: String) = tr("Bahasa Aplikasi", "App Language", lang)
    fun langSettingDesc(lang: String) = tr("Pilih bahasa tampilan antarmuka (Indonesian / English)", "Choose interface display language (Indonesian / English)", lang)
    fun indonesian(lang: String) = tr("Bahasa Indonesia (ID)", "Indonesian (ID)", lang)
    fun english(lang: String) = tr("English (US)", "English (US)", lang)

    // Riwayat Screen Strings
    fun historyTitle(lang: String) = tr("Riwayat & Audit Log Transaksi", "Transaction History & Audit Log", lang)
    fun historySubtitle(lang: String) = tr("Detail pencatatan muat pagi, titip baru, isi ulang, closing & sortir", "Full records of loading, new drops, refills, closing & sorting", lang)
    fun filterAll(lang: String) = tr("Semua", "All", lang)
    fun filterVisits(lang: String) = tr("Kunjungan & Titipan", "Visits & Drops", lang)
    fun filterLoading(lang: String) = tr("Muat Barang", "Morning Loads", lang)
    fun filterClosing(lang: String) = tr("Closing Sore", "Daily Closing", lang)
    fun filterSortir(lang: String) = tr("Sortir Retur & Lainnya", "Return Sorting & Write-Off", lang)
    fun searchHistoryPlaceholder(lang: String) = tr("Cari warung, produk, nomor transaksi, status...", "Search outlet, SKU, transaction ID, status...", lang)
    
    // Metrics
    fun totalRecords(lang: String) = tr("Total Riwayat", "Total Records", lang)
    fun totalCash(lang: String) = tr("Total Kas Masuk", "Cash Collected", lang)
    fun totalPcsDistributed(lang: String) = tr("Pcs Terdistribusi", "Units Distributed", lang)
    fun totalBsReturned(lang: String) = tr("Retur Ditarik", "Returns Pulled", lang)
    fun filterDateAll(lang: String) = tr("Semua Waktu", "All Time", lang)
    fun filterDateToday(lang: String) = tr("Hari Ini", "Today", lang)
    fun filterDate7Days(lang: String) = tr("7 Hari Terakhir", "Last 7 Days", lang)
    fun filterDate30Days(lang: String) = tr("30 Hari Terakhir", "Last 30 Days", lang)

    // Detail Dialog
    fun detailReceiptTitle(lang: String) = tr("Rincian Lengkap Transaksi", "Complete Transaction Details", lang)
    fun digitalReceipt(lang: String) = tr("STRUK DIGITAL TRACERPRO", "TRACERPRO DIGITAL RECEIPT", lang)
    fun transactionId(lang: String) = tr("ID Transaksi / Ref", "Transaction ID / Ref", lang)
    fun dateAndTime(lang: String) = tr("Tanggal & Jam", "Date & Time", lang)
    fun outletName(lang: String) = tr("Nama Outlet / Warung", "Outlet / Store Name", lang)
    fun productName(lang: String) = tr("Nama Produk / SKU", "Product Name / SKU", lang)
    fun previousDeposit(lang: String) = tr("Sisa Titipan Lalu", "Previous Deposit", lang)
    fun currentPhysicalStock(lang: String) = tr("Sisa Fisik Ditemukan", "Physical Stock Left", lang)
    fun unitsSold(lang: String) = tr("Pcs Laku Terjual", "Units Sold", lang)
    fun unitPrice(lang: String) = tr("Harga Satuan", "Unit Price", lang)
    fun subtotalSales(lang: String) = tr("Subtotal Penjualan", "Sales Subtotal", lang)
    fun previousDebt(lang: String) = tr("Saldo Piutang Lalu (Bon)", "Previous Debt (Credit)", lang)
    fun grandTotalBill(lang: String) = tr("Grand Total Tagihan", "Grand Total Bill", lang)
    fun cashCollected(lang: String) = tr("Uang Diterima (Kas)", "Cash Received", lang)
    fun remainingDebt(lang: String) = tr("Sisa Piutang Baru (Bon)", "New Remaining Debt", lang)
    fun restockUnits(lang: String) = tr("Restock Titipan Baru", "New Restock Units", lang)
    fun bsUnitsReturned(lang: String) = tr("Barang Retur Ditarik", "Returned Units Pulled", lang)
    fun stockSource(lang: String) = tr("Sumber Laci Stok", "Stock Drawer Source", lang)
    fun freshFactory(lang: String) = tr("Fresh Pabrik (Principal)", "Fresh Factory (Supplier)", lang)
    fun privateRepack(lang: String) = tr("Aset Pribadi (Repack Retur)", "Personal Asset (Repack)", lang)
    fun gpsCoordinates(lang: String) = tr("Titik GPS Lapangan", "GPS Coordinates", lang)
    fun gpsAddressRecorded(lang: String) = tr("Alamat Terverifikasi", "Verified Address", lang)
    fun notes(lang: String) = tr("Catatan Transaksi", "Transaction Notes", lang)
    fun btnClose(lang: String) = tr("Tutup", "Close", lang)
    fun btnShareReceipt(lang: String) = tr("Salin / Bagikan Struk", "Share / Copy Receipt", lang)
    fun emptyHistory(lang: String) = tr("Belum ada riwayat transaksi pada filter ini.", "No transaction records found in this filter.", lang)

    // Master Data Strings
    fun masterTitle(lang: String) = tr("Master Data", "Master Data", lang)
    fun masterSubtitle(lang: String) = tr("Produk • Outlet • Rute • Supplier Pabrik", "Products • Outlets • Routes • Suppliers", lang)
    fun tabProductUom(lang: String) = tr("Produk UOM", "Product SKUs", lang)
    fun tabOutlets(lang: String) = tr("Outlet", "Outlets", lang)
    fun tabRoutes(lang: String) = tr("Rute Jalur", "Routes", lang)
    fun tabSuppliers(lang: String) = tr("Supplier", "Suppliers", lang)
    fun btnAdd(lang: String) = tr("Tambah Data", "Add New", lang)
    fun btnEdit(lang: String) = tr("Edit", "Edit", lang)
    fun btnDelete(lang: String) = tr("Hapus", "Delete", lang)
    fun btnCancel(lang: String) = tr("Batal", "Cancel", lang)
    fun btnSave(lang: String) = tr("Simpan", "Save", lang)
    fun deleteConfirmTitle(lang: String) = tr("Hapus Data", "Delete Record", lang)
    fun deleteConfirmMsg(name: String, lang: String) = tr("Apakah Anda yakin ingin menghapus $name? Tindakan ini tidak dapat dibatalkan.", "Are you sure you want to delete $name? This action cannot be undone.", lang)
    fun buyPriceFactory(lang: String) = tr("HARGA BELI PABRIK", "FACTORY BUY PRICE", lang)
    fun sellPriceConsignment(lang: String) = tr("HARGA JUAL TITIPAN", "CONSIGNMENT SELL PRICE", lang)
    fun contactOwner(lang: String) = tr("Kontak / Pemilik", "Contact / Owner", lang)
    fun routeAssignment(lang: String) = tr("Jalur Rute", "Route Assignment", lang)
    fun debtBalance(lang: String) = tr("Saldo Piutang", "Outstanding Debt", lang)
    fun mappedOutlets(lang: String) = tr("Outlet Terpetakan", "Mapped Outlets", lang)
    fun openMapsRoute(lang: String) = tr("Buka Rute Maps", "Open Maps Route", lang)
    fun openNavigation(lang: String) = tr("Navigasi GPS", "GPS Navigation", lang)
    fun outletStats(lang: String) = tr("Statistik & Riwayat", "Stats & History", lang)
    fun customPrices(lang: String) = tr("Harga Khusus", "Custom Prices", lang)

    // Laporan Strings & Tabs
    fun reportTitle(lang: String) = tr("Laporan Realtime & Finansial", "Realtime Reports & Financials", lang)
    fun reportSubtitle(lang: String) = tr("4 Laci Stok, Setoran Pabrik & Piutang Bon", "4 Stock Drawers, Supplier Settlement & Debts", lang)
    fun tab4Drawers(lang: String) = tr("4 Laci Stok", "4 Stock Drawers", lang)
    fun tabSupplierDeposit(lang: String) = tr("Setoran Supplier", "Supplier Settlement", lang)
    fun tabOutletReceivables(lang: String) = tr("Piutang Outlet", "Outlet Debts", lang)
    fun tabAssetProfit(lang: String) = tr("Aset & Laba", "Assets & Profit", lang)
    fun tabWriteOff(lang: String) = tr("Kerugian", "Losses / Write-off", lang)
    fun tabTxHistory(lang: String) = tr("Riwayat Tx", "Tx Log", lang)
    fun tabDrawerStock(lang: String) = tr("4 Laci Stok", "4 Stock Drawers", lang)
    fun tabSupplierSettlement(lang: String) = tr("Setoran Supplier", "Supplier Settlement", lang)
    fun tabOutletDebt(lang: String) = tr("Piutang Outlet", "Outlet Debts", lang)
    fun tabPrivateAssets(lang: String) = tr("Aset & Laba", "Assets & Profit", lang)

    fun periodToday(lang: String) = tr("Hari Ini", "Today", lang)
    fun periodYesterday(lang: String) = tr("Kemarin", "Yesterday", lang)
    fun periodSelectDate(lang: String) = tr("Pilih Tanggal", "Select Date", lang)
    fun periodThisMonth(lang: String) = tr("Bulan Ini", "This Month", lang)
    fun periodThisYear(lang: String) = tr("Tahun Ini", "This Year", lang)
    fun periodAll(lang: String) = tr("Semua Periode", "All Periods", lang)

    fun pdfGenerating(lang: String) = tr("Membuat PDF...", "Generating PDF...", lang)
    fun pdfExport(lang: String) = tr("Cetak PDF", "Export PDF", lang)
    fun btnExportPdf(lang: String) = tr("Cetak PDF", "Export PDF", lang)
    fun btnExportCsv(lang: String) = tr("Ekspor CSV", "Export CSV", lang)
    fun filterPeriod(lang: String) = tr("Periode:", "Period:", lang)
    fun activeFilter(lang: String) = tr("Filter Aktif", "Active Filter", lang)
    fun btnChange(lang: String) = tr("Ganti", "Change", lang)
    fun loadedOnly(lang: String) = tr("Hanya Produk Muat Hari Ini", "Today's Loaded Products Only", lang)
    fun allCatalog(lang: String) = tr("Semua Katalog Master Produk", "All Master Catalog Products", lang)

    fun drawerFresh(lang: String) = tr("1. Laci Fresh Pabrik", "1. Fresh Factory Drawer", lang)
    fun drawerConsignment(lang: String) = tr("2. Laci Titipan Outlet", "2. Outlet Consignment Drawer", lang)
    fun drawerReturn(lang: String) = tr("3. Laci Retur Tarikan", "3. Pulled Returns Drawer", lang)
    fun drawerRepack(lang: String) = tr("4. Laci Aset Pribadi Repack", "4. Personal Repack Asset Drawer", lang)
    fun drawerBsSortir(lang: String) = tr("2. Laci Retur (Belum Sortir)", "2. Unsorted Returns Drawer", lang)
    fun drawerPribadi(lang: String) = tr("3. Laci Aset Pribadi (Repack)", "3. Personal Repack Asset Drawer", lang)
    fun drawerRusak(lang: String) = tr("4. Laci Rusak / Afkir (Dibuang)", "4. Damaged / Waste Drawer (Scrapped)", lang)

    fun supplierSummaryTitle(lang: String) = tr("REKAPITULASI TAGIHAN SUPPLIER VS KAS OUTLET", "SUPPLIER BILL VS OUTLET CASH SUMMARY", lang)
    fun factoryBillVsCash(lang: String) = tr("REKAPITULASI TAGIHAN SUPPLIER VS KAS OUTLET", "SUPPLIER BILL VS OUTLET CASH SUMMARY", lang)
    fun totalPackLoaded(lang: String) = tr("Total Muat Pack", "Total Packs Loaded", lang)
    fun totalLoadPack(lang: String) = tr("Total Muat Pack", "Total Packs Loaded", lang)
    fun totalSupplierBill(lang: String) = tr("Total Tagihan Supplier", "Total Supplier Invoices", lang)
    fun cashCollectedOutlet(lang: String) = tr("Kas Terkumpul Outlet", "Cash Collected from Outlets", lang)
    fun totalCashCollected(lang: String) = tr("Kas Terkumpul Outlet", "Cash Collected from Outlets", lang)
    fun cashDeficit(lang: String) = tr("Defisit Kas (Modal Tertanam)", "Cash Deficit (Capital Bound)", lang)
    fun paidSurplus(lang: String) = tr("LUNAS / SURPLUS", "PAID / SURPLUS", lang)
    fun depositStatus(lang: String) = tr("Status Setoran", "Settlement Status", lang)
    fun morningLoadingDetails(lang: String) = tr("RINCIAN LOADING PAGI & SETORAN SUPPLIER", "MORNING LOADING & SUPPLIER SETTLEMENTS", lang)
    fun loadingDetailsTitle(lang: String) = tr("RINCIAN LOADING PAGI & SETORAN SUPPLIER", "MORNING LOADING & SUPPLIER SETTLEMENTS", lang)
    fun noLoadingData(lang: String) = tr("Tidak ada data loading untuk periode ini", "No loading data found for this period", lang)

    fun totalDebtAtOutlets(lang: String) = tr("TOTAL SALDO BON DI OUTLET", "TOTAL OUTSTANDING CREDIT AT OUTLETS", lang)
    fun totalBonOutlets(lang: String) = tr("TOTAL SALDO BON DI OUTLET", "TOTAL OUTSTANDING CREDIT AT OUTLETS", lang)
    fun outletsWithDebt(lang: String) = tr("Outlet Bon", "Indebted Outlets", lang)
    fun bonOutletCount(lang: String) = tr("Outlet Bon", "Indebted Outlets", lang)
    fun bonOutletCount(count: Int, lang: String) = tr("$count Outlet Bon", "$count Indebted Outlets", lang)
    fun bonBalance(lang: String) = tr("Saldo Bon", "Debt Balance", lang)

    fun personalRepackReady(lang: String) = tr("ASET PRIBADI REPACK SIAP EDAR", "PERSONAL REPACK ASSETS READY TO SELL", lang)
    fun assetRepackReady(lang: String) = tr("ASET PRIBADI REPACK SIAP EDAR", "PERSONAL REPACK ASSETS READY TO SELL", lang)
    fun personalRepackDesc(lang: String) = tr("Hasil penjualan stok Repack 100% menjadi hak laba bersih salesman.", "100% of Repack sales revenue is the salesman's direct net profit.", lang)
    fun repackProfitNote(lang: String) = tr("Hasil penjualan stok Repack 100% menjadi hak laba bersih salesman.", "100% of Repack sales revenue is the salesman's direct net profit.", lang)
    fun pcsCirculating(lang: String) = tr("Pcs Siap Edar", "Units Ready", lang)
    fun pcsCirculating(pcs: Int, lang: String) = tr("$pcs Pcs Siap Edar", "$pcs Units Ready", lang)
    fun sortirHistory(lang: String) = tr("RIWAYAT SORTIR RETUR & REPACK", "RETURN SORTING & REPACK HISTORY", lang)
    fun sortirHistoryTitle(lang: String) = tr("RIWAYAT SORTIR RETUR & REPACK", "RETURN SORTING & REPACK HISTORY", lang)
    fun noSortirData(lang: String) = tr("Belum ada data sortir retur pada periode ini", "No return sorting data found for this period", lang)
    fun cleanProfitEstimate(lang: String) = tr("Estimasi Margin Bersih", "Estimated Net Profit", lang)

    fun writeOffReport(lang: String) = tr("LAPORAN KERUGIAN USAHA (WRITE-OFF)", "BUSINESS LOSS REPORT (WRITE-OFF)", lang)
    fun writeOffReportTitle(lang: String) = tr("LAPORAN KERUGIAN USAHA (WRITE-OFF)", "BUSINESS LOSS REPORT (WRITE-OFF)", lang)
    fun totalLoss(lang: String) = tr("Total Kerugian", "Total Loss", lang)
    fun bsDamagedDiscarded(lang: String) = tr("Stok Retur Rusak Dibuang", "Damaged Stock Discarded", lang)
    fun discardedDamagedStock(lang: String) = tr("Stok Retur Rusak Dibuang", "Damaged Stock Discarded", lang)
    fun discardedDamagedStock(pcs: Int, lang: String) = tr("$pcs Pcs Stok Rusak Dibuang", "$pcs Damaged Units Discarded", lang)
    fun noLossData(lang: String) = tr("Tidak ada kerugian / write-off pada periode ini", "No losses / write-offs recorded in this period", lang)
    fun reason(lang: String) = tr("Alasan", "Reason", lang)
    fun writtenOffDebt(lang: String) = tr("Piutang Dihapus", "Written-off Debt", lang)
    fun forfeitedStock(lang: String) = tr("Stok Hangus", "Forfeited Stock", lang)
    fun totalBookLoss(lang: String) = tr("Total Rugi Buku", "Total Book Loss", lang)

    fun totalOutletTx(lang: String) = tr("Total Transaksi Kunjungan", "Total Outlet Visits", lang)
    fun totalOutletTx(count: Int, lang: String) = tr("Total: $count Transaksi Kunjungan", "Total: $count Outlet Visits", lang)
    fun noTxHistory(lang: String) = tr("Tidak ada transaksi pada periode ini", "No transactions recorded for this period", lang)
    fun viewReceipt(lang: String) = tr("Lihat Struk", "View Receipt", lang)
    fun btnApplyFilter(lang: String) = tr("Terapkan Filter", "Apply Filter", lang)

    fun pdfReadyTitle(lang: String) = tr("Laporan PDF Berhasil Dibuat", "PDF Report Generated Successfully", lang)
    fun pdfReadyMsg(lang: String) = tr("File laporan PDF telah siap untuk dicetak atau dibagikan.", "The PDF report file is ready to print or share.", lang)
    fun pdfReadyMsg(period: String, lang: String) = tr("Laporan periode $period telah berhasil dikonversi ke PDF formal lengkap tabel finansial & inventori.", "Report for period $period was successfully converted to formal PDF with financial & inventory tables.", lang)
    fun sharePdf(lang: String) = tr("Bagikan PDF", "Share PDF", lang)
    fun openPdf(lang: String) = tr("Buka File", "Open File", lang)

    // Utilitas / Settings Strings
    fun settingsTitle(lang: String) = tr("Utilitas & Pengaturan", "Settings & Utilities", lang)
    fun settingsSubtitle(lang: String) = tr("Profil Sales • Backup ZIP & Foto • Offline GPS", "Sales Profile • Backup ZIP & Photos • Offline GPS", lang)
    fun editProfile(lang: String) = tr("Edit Profil", "Edit Profile", lang)
    fun networkModeTitle(lang: String) = tr("Mode & Status Jaringan", "Network Mode & Status", lang)
    fun networkOnlineDesc(lang: String) = tr("Online • Auto-Sync Aktif", "Online • Auto-Sync Active", lang)
    fun networkOfflineDesc(lang: String) = tr("100% Offline (SQLite Lokal)", "100% Offline (Local SQLite)", lang)
    fun syncNow(lang: String) = tr("Sinkron Sekarang", "Sync Now", lang)
    fun backupExportTitle(lang: String) = tr("Ekspor Backup Modular (JSON)", "Modular Backup Export (JSON)", lang)
    fun backupExportDesc(lang: String) = tr("Unduh arsip database lokal lengkap", "Download full local database archive", lang)
    fun backupImportTitle(lang: String) = tr("Pulihkan Data (Import JSON)", "Restore Data (Import JSON)", lang)
    fun backupImportDesc(lang: String) = tr("Pulihkan database dari file backup JSON sebelumnya", "Restore database from previous JSON backup file", lang)
    fun gpsToolTitle(lang: String) = tr("Alat GPS & Titik Koordinat", "GPS & Coordinates Tool", lang)
    fun gpsToolDesc(lang: String) = tr("Lihat koordinat satelit akurat & alamat geocoding", "View accurate satellite coordinates & geocoded address", lang)
    fun businessRulesTitle(lang: String) = tr("Konfigurasi Parameter Bisnis SFA", "SFA Business Rules & Thresholds", lang)
    fun maxCreditLimit(lang: String) = tr("Batas Maksimal Bon Outlet:", "Max Outlet Credit Limit:", lang)
    fun gpsLockRadius(lang: String) = tr("Radius Kunci GPS Valid:", "Valid GPS Lock Radius:", lang)

    // Sort and Filter Labels
    fun getSortLabel(sortBy: OutletSortBy, lang: String): String {
        return when (sortBy) {
            OutletSortBy.TERDEKAT_GPS -> tr("Jarak Terdekat (GPS)", "Nearest Distance (GPS)", lang)
            OutletSortBy.LAMA_TIDAK_DIKUNJUNGI -> tr("Terlama Belum Dikunjungi", "Longest Not Visited", lang)
            OutletSortBy.URUTAN_RUTE -> tr("Urutan Jalur Rute", "Route Order", lang)
            OutletSortBy.OMSET_TERBESAR -> tr("Omset Penjualan Terbesar", "Highest Revenue", lang)
            OutletSortBy.PIUTANG_TERBESAR -> tr("Saldo Piutang Terbesar", "Highest Debt", lang)
            OutletSortBy.STOK_MENIPIS -> tr("Stok Titipan Sedikit", "Low Consignment Stock", lang)
            OutletSortBy.NAMA_AZ -> tr("Nama Outlet (A-Z)", "Outlet Name (A-Z)", lang)
        }
    }

    fun getAgingFilterLabel(aging: OutletFilterAging, lang: String, customDays: Int? = null): String {
        return when (aging) {
            OutletFilterAging.SEMUA -> tr("Semua Outlet", "All Outlets", lang)
            OutletFilterAging.BELUM_HARI_INI -> tr("Belum Hari Ini", "Unvisited Today", lang)
            OutletFilterAging.LEBIH_3_HARI -> tr("> 3 Hari", "> 3 Days", lang)
            OutletFilterAging.LEBIH_7_HARI -> tr("> 7 Hari (Mingguan)", "> 7 Days (Weekly)", lang)
            OutletFilterAging.LEBIH_14_HARI -> tr("> 14 Hari (Kritis)", "> 14 Days (Critical)", lang)
            OutletFilterAging.LEBIH_30_HARI -> tr("> 30 Hari (Dormant)", "> 30 Days (Dormant)", lang)
            OutletFilterAging.KUSTOM_HARI -> {
                val days = customDays ?: 7
                tr("Kustom (≥ $days Hari)", "Custom (≥ $days Days)", lang)
            }
            OutletFilterAging.SUDAH_HARI_INI -> tr("Selesai Hari Ini", "Done Today", lang)
        }
    }

    fun sortLabel(lang: String, sortBy: String): String {
        return when (sortBy) {
            "TERDEKAT_GPS" -> tr("Jarak Terdekat (GPS)", "Nearest Distance (GPS)", lang)
            "LAMA_TIDAK_DIKUNJUNGI" -> tr("Terlama Belum Dikunjungi", "Longest Not Visited", lang)
            "URUTAN_RUTE" -> tr("Urutan Jalur Rute", "Route Order", lang)
            "OMSET_TERBESAR" -> tr("Omset Penjualan Terbesar", "Highest Revenue", lang)
            "PIUTANG_TERBESAR" -> tr("Saldo Piutang Terbesar", "Highest Debt", lang)
            "STOK_MENIPIS" -> tr("Stok Titipan Sedikit", "Low Consignment Stock", lang)
            "NAMA_AZ" -> tr("Nama Outlet (A-Z)", "Outlet Name (A-Z)", lang)
            else -> sortBy
        }
    }

    fun agingLabel(lang: String, aging: String): String {
        return when (aging) {
            "SEMUA" -> tr("Semua Outlet", "All Outlets", lang)
            "BELUM_HARI_INI" -> tr("Belum Hari Ini", "Unvisited Today", lang)
            "LEBIH_3_HARI" -> tr("> 3 Hari", "> 3 Days", lang)
            "LEBIH_7_HARI" -> tr("> 7 Hari (Mingguan)", "> 7 Days (Weekly)", lang)
            "LEBIH_14_HARI" -> tr("> 14 Hari (Kritis)", "> 14 Days (Critical)", lang)
            "LEBIH_30_HARI" -> tr("> 30 Hari (Dormant)", "> 30 Days (Dormant)", lang)
            "KUSTOM_HARI" -> tr("Kustom Hari...", "Custom Days...", lang)
            "SUDAH_HARI_INI" -> tr("Selesai Hari Ini", "Done Today", lang)
            else -> aging
        }
    }
}


