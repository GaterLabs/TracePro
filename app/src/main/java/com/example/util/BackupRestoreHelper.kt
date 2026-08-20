package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entity.*
import com.example.data.repository.SfaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSelection(
    val exportProfile: Boolean = true,
    val exportProducts: Boolean = true,
    val exportWarungs: Boolean = true,
    val exportRutes: Boolean = true,
    val exportPabriks: Boolean = true,
    val exportCustomPrices: Boolean = true,
    val exportTransactions: Boolean = true,
    val exportInventory: Boolean = true,
    val exportPhotos: Boolean = true
)

data class ExportResult(
    val jsonString: String,
    val summary: String,
    val fileName: String,
    val file: File? = null
)

data class ZipBackupResult(
    val zipFile: File,
    val summary: String,
    val fileName: String,
    val photoCount: Int,
    val fileSizeFormatted: String
)

data class ImportResult(
    val success: Boolean,
    val message: String,
    val productsCount: Int = 0,
    val warungsCount: Int = 0,
    val rutesCount: Int = 0,
    val transactionsCount: Int = 0,
    val pabriksCount: Int = 0,
    val photosCount: Int = 0
)

object BackupRestoreHelper {

    suspend fun exportModularDatabaseToJson(
        context: Context,
        repository: SfaRepository,
        selection: BackupSelection = BackupSelection()
    ): ExportResult = withContext(Dispatchers.IO) {
        val root = buildJsonRoot(repository, selection)
        val jsonStr = root.toString(2)
        val dateCompact = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "BACKUP_SFA_${dateCompact}.json"
        
        val backupsDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val jsonFile = File(backupsDir, fileName)
        FileOutputStream(jsonFile).use { it.write(jsonStr.toByteArray(Charsets.UTF_8)) }

        val summary = "Ekspor JSON: " + summarizeSelection(root)

        ExportResult(jsonStr, summary, fileName, jsonFile)
    }

    suspend fun exportFullDatabaseToJson(context: Context, repository: SfaRepository): ExportResult {
        return exportModularDatabaseToJson(context, repository, BackupSelection())
    }

    /**
     * Creates a complete .ZIP package containing:
     * 1. backup_data.json (all Room tables: profile, products, rutes, warungs, pabriks, transactions, drawers, loadings, bs_sortirs, write_offs)
     * 2. photos/ directory containing all compressed outlet photos
     *
     * Perfect for migrating to a new phone without missing photos or operational records!
     */
    suspend fun exportFullBackupToZip(
        context: Context,
        repository: SfaRepository,
        selection: BackupSelection = BackupSelection()
    ): ZipBackupResult = withContext(Dispatchers.IO) {
        val dateCompact = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val zipFileName = "MIGRASI_SFA_LENGKAP_${dateCompact}.zip"
        val backupsDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val zipFile = File(backupsDir, zipFileName)

        val rootJson = buildJsonRoot(repository, selection)
        val jsonBytes = rootJson.toString(2).toByteArray(Charsets.UTF_8)

        var photoCount = 0
        val photosDir = ImageCompressor.getOutletPhotosDir(context)
        val processedPhotoNames = mutableSetOf<String>()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            // 1. Write backup_data.json
            val jsonEntry = ZipEntry("backup_data.json")
            zos.putNextEntry(jsonEntry)
            zos.write(jsonBytes)
            zos.closeEntry()

            // 2. Write photos if requested
            if (selection.exportPhotos) {
                // Collect from photosDir
                if (photosDir.exists()) {
                    val photoFiles = photosDir.listFiles { file ->
                        file.isFile && (file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true) || file.name.endsWith(".png", true) || file.name.endsWith(".webp", true))
                    } ?: emptyArray()

                    for (photoFile in photoFiles) {
                        try {
                            if (!processedPhotoNames.contains(photoFile.name)) {
                                val photoEntry = ZipEntry("photos/${photoFile.name}")
                                zos.putNextEntry(photoEntry)
                                FileInputStream(photoFile).use { fis -> fis.copyTo(zos) }
                                zos.closeEntry()
                                processedPhotoNames.add(photoFile.name)
                                photoCount++
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Also check direct warung photo paths if stored elsewhere
                val warungs = repository.getAllWarungsDirect()
                for (w in warungs) {
                    val rawPhoto = w.fotoOutlet ?: continue
                    try {
                        val path = Uri.parse(rawPhoto).path ?: rawPhoto
                        val file = File(path)
                        if (file.exists() && file.isFile && !processedPhotoNames.contains(file.name)) {
                            val photoEntry = ZipEntry("photos/${file.name}")
                            zos.putNextEntry(photoEntry)
                            FileInputStream(file).use { fis -> fis.copyTo(zos) }
                            zos.closeEntry()
                            processedPhotoNames.add(file.name)
                            photoCount++
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        val sizeFormatted = ImageCompressor.formatFileSize(zipFile.length())
        val summary = "Paket Migrasi Lengkap (.ZIP): ${summarizeSelection(rootJson)}" +
                if (photoCount > 0) " + $photoCount Foto Outlet" else " (Tanpa foto)"

        ZipBackupResult(
            zipFile = zipFile,
            summary = summary,
            fileName = zipFileName,
            photoCount = photoCount,
            fileSizeFormatted = sizeFormatted
        )
    }

    private suspend fun buildJsonRoot(
        repository: SfaRepository,
        selection: BackupSelection
    ): JSONObject {
        val root = JSONObject()
        root.put("app", "TracerPro - SFA Konsinyasi")
        root.put("version", 7)
        root.put("timestamp", System.currentTimeMillis())
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // 1. User Profile
        if (selection.exportProfile) {
            val profile = repository.getUserProfileDirect()
            if (profile != null) {
                val profObj = JSONObject().apply {
                    put("namaSalesman", profile.namaSalesman)
                    put("noHp", profile.noHp)
                    put("namaDistributor", profile.namaDistributor)
                    put("alamatDepo", profile.alamatDepo)
                    put("platNomorMobil", profile.platNomorMobil)
                    put("areaOperasional", profile.areaOperasional)
                    put("pinKeamanan", profile.pinKeamanan)
                    put("isConfigured", profile.isConfigured)
                    put("createdAt", profile.createdAt)
                }
                root.put("userProfile", profObj)
            }
        }

        // 2. Products
        if (selection.exportProducts) {
            val products = repository.getAllProductsDirect()
            val prodArray = JSONArray()
            products.forEach { p ->
                prodArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("nama", p.nama)
                    put("kategori", p.kategori)
                    put("satuanBesar", p.satuanBesar)
                    put("satuanKecil", p.satuanKecil)
                    put("rasioKonversi", p.rasioKonversi)
                    put("hargaBeliPabrik", p.hargaBeliPabrik)
                    put("hargaJualDefault", p.hargaJualDefault)
                    put("stokMinimumAlert", p.stokMinimumAlert)
                    put("status", p.status)
                    put("createdAt", p.createdAt)
                })
            }
            root.put("products", prodArray)
        }

        // 3. Rutes
        if (selection.exportRutes) {
            val rutes = repository.getAllRutesDirect()
            val ruteArray = JSONArray()
            rutes.forEach { r ->
                ruteArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("namaRute", r.namaRute)
                    put("hariKunjungan", r.hariKunjungan)
                    put("idSalesman", r.idSalesman)
                    put("estimasiJumlahWarung", r.estimasiJumlahWarung)
                    put("jarakTotalKm", r.jarakTotalKm)
                    put("status", r.status)
                })
            }
            root.put("rutes", ruteArray)
        }

        // 4. Pabriks
        if (selection.exportPabriks) {
            val pabriks = repository.getAllPabriksDirect()
            val pabrikArray = JSONArray()
            pabriks.forEach { p ->
                pabrikArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("namaPabrik", p.namaPabrik)
                    put("alamatLengkap", p.alamatLengkap)
                    put("noHpCp", p.noHpCp)
                    put("namaCp", p.namaCp)
                    put("syaratPembayaran", p.syaratPembayaran)
                    put("kebijakanRetur", p.kebijakanRetur)
                    put("rekeningBank", p.rekeningBank)
                    put("status", p.status)
                })
            }
            root.put("pabriks", pabrikArray)
        }

        // 5. Warungs & Outlets
        if (selection.exportWarungs) {
            val warungs = repository.getAllWarungsDirect()
            val warungArray = JSONArray()
            warungs.forEach { w ->
                val rawPhoto = w.fotoOutlet ?: ""
                val photoFileName = if (rawPhoto.isNotBlank()) {
                    try {
                        val file = File(Uri.parse(rawPhoto).path ?: rawPhoto)
                        file.name
                    } catch (_: Exception) {
                        rawPhoto
                    }
                } else ""

                warungArray.put(JSONObject().apply {
                    put("id", w.id)
                    put("namaWarung", w.namaWarung)
                    put("namaPemilik", w.namaPemilik)
                    put("ruteId", w.ruteId)
                    put("latitude", w.latitude)
                    put("longitude", w.longitude)
                    put("alamatLengkap", w.alamatLengkap)
                    put("akurasiGpsMeter", w.akurasiGpsMeter)
                    put("noHp", w.noHp)
                    put("kategoriWarung", w.kategoriWarung)
                    put("limitHutangMaksimal", w.limitHutangMaksimal)
                    put("saldoPiutang", w.saldoPiutang)
                    put("stokTitipanPcs", w.stokTitipanPcs)
                    put("tglKunjunganTerakhir", w.tglKunjunganTerakhir)
                    put("tglMulaiHutang", w.tglMulaiHutang)
                    put("urutanKunjungan", w.urutanKunjungan)
                    put("notes", w.notes)
                    put("status", w.status)
                    put("fotoOutlet", w.fotoOutlet ?: "")
                    put("photoFileName", photoFileName)
                    put("tanggalBerlangganan", w.tanggalBerlangganan)
                    put("pendingAddressSync", w.pendingAddressSync)
                })
            }
            root.put("warungs", warungArray)
        }

        // 6. Custom Prices
        if (selection.exportCustomPrices) {
            val customPrices = repository.getAllCustomPricesDirect()
            val cpArray = JSONArray()
            customPrices.forEach { cp ->
                cpArray.put(JSONObject().apply {
                    put("id", cp.id)
                    put("warungId", cp.warungId)
                    put("productId", cp.productId)
                    put("hargaJualPcs", cp.hargaJualPcs)
                    put("updatedAt", cp.updatedAt)
                })
            }
            root.put("customPrices", cpArray)
        }

        // 7. Inventory Drawers, Daily Loadings, Sortir BS, Write Offs
        if (selection.exportInventory) {
            val drawers = repository.getAllDrawersDirect()
            val drawerArray = JSONArray()
            drawers.forEach { d ->
                drawerArray.put(JSONObject().apply {
                    put("id", d.id)
                    put("productId", d.productId)
                    put("stokFreshPabrikPcs", d.stokFreshPabrikPcs)
                    put("stokBsBelumSortirPcs", d.stokBsBelumSortirPcs)
                    put("stokPribadiLayakJualPcs", d.stokPribadiLayakJualPcs)
                    put("stokPribadiRusakPcs", d.stokPribadiRusakPcs)
                    put("lastUpdated", d.lastUpdated)
                })
            }
            root.put("drawers", drawerArray)

            val loadings = repository.getAllDailyLoadingsDirect()
            val loadingArray = JSONArray()
            loadings.forEach { l ->
                loadingArray.put(JSONObject().apply {
                    put("id", l.id)
                    put("tanggal", l.tanggal)
                    put("productId", l.productId)
                    put("jumlahDus", l.jumlahDus)
                    put("rasioKonversi", l.rasioKonversi)
                    put("totalPcs", l.totalPcs)
                    put("hargaBeliPabrikDus", l.hargaBeliPabrikDus)
                    put("potensiHutangPabrik", l.potensiHutangPabrik)
                    put("sisaDusSore", l.sisaDusSore)
                    put("terjualDus", l.terjualDus)
                    put("tagihanPabrikClosing", l.tagihanPabrikClosing)
                    put("statusClosing", l.statusClosing)
                    put("createdAt", l.createdAt)
                })
            }
            root.put("loadings", loadingArray)

            val bsSortirs = repository.getAllBsSortirsDirect()
            val sortirArray = JSONArray()
            bsSortirs.forEach { s ->
                sortirArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("productId", s.productId)
                    put("totalBsAwalPcs", s.totalBsAwalPcs)
                    put("bsLayakJualPcs", s.bsLayakJualPcs)
                    put("bsRusakPcs", s.bsRusakPcs)
                    put("estimasiNilaiModal", s.estimasiNilaiModal)
                    put("estimasiNilaiJual", s.estimasiNilaiJual)
                    put("estimasiProfitMurni", s.estimasiProfitMurni)
                    put("catatan", s.catatan)
                    put("timestamp", s.timestamp)
                })
            }
            root.put("bsSortirs", sortirArray)

            val writeOffs = repository.getAllWriteOffsDirect()
            val woArray = JSONArray()
            writeOffs.forEach { wo ->
                woArray.put(JSONObject().apply {
                    put("id", wo.id)
                    put("warungId", wo.warungId)
                    put("namaWarung", wo.namaWarung)
                    put("piutangDihapus", wo.piutangDihapus)
                    put("stokHangusPcs", wo.stokHangusPcs)
                    put("nilaiStokHangus", wo.nilaiStokHangus)
                    put("totalKerugian", wo.totalKerugian)
                    put("alasan", wo.alasan)
                    put("timestamp", wo.timestamp)
                })
            }
            root.put("writeOffs", woArray)
        }

        // 8. Transactions
        if (selection.exportTransactions) {
            val transactions = repository.getAllTransactionsDirect()
            val txArray = JSONArray()
            transactions.forEach { t ->
                txArray.put(JSONObject().apply {
                    put("id", t.id)
                    put("warungId", t.warungId)
                    put("ruteId", t.ruteId)
                    put("productId", t.productId)
                    put("tanggal", t.tanggal)
                    put("jenis", t.jenis)
                    put("sumberStok", t.sumberStok)
                    put("sisaTitipanLaluPcs", t.sisaTitipanLaluPcs)
                    put("sisaFisikPcs", t.sisaFisikPcs)
                    put("pcsLaku", t.pcsLaku)
                    put("hargaSatuan", t.hargaSatuan)
                    put("subtotalLaku", t.subtotalLaku)
                    put("saldoPiutangLama", t.saldoPiutangLama)
                    put("grandTotalTagihan", t.grandTotalTagihan)
                    put("uangDiterima", t.uangDiterima)
                    put("saldoPiutangBaru", t.saldoPiutangBaru)
                    put("statusBayar", t.statusBayar)
                    put("bsDitarikPcs", t.bsDitarikPcs)
                    put("restockBaruPcs", t.restockBaruPcs)
                    put("totalTitipanAktifPcs", t.totalTitipanAktifPcs)
                    put("gpsLat", t.gpsLat)
                    put("gpsLng", t.gpsLng)
                    put("gpsAddress", t.gpsAddress)
                    put("catatan", t.catatan)
                    put("timestamp", t.timestamp)
                })
            }
            root.put("transactions", txArray)
        }

        return root
    }

    private fun summarizeSelection(root: JSONObject): String {
        val list = mutableListOf<String>()
        if (root.has("userProfile")) list.add("Profil Sales")
        if (root.has("products")) list.add("${root.getJSONArray("products").length()} SKU")
        if (root.has("warungs")) list.add("${root.getJSONArray("warungs").length()} Outlet")
        if (root.has("rutes")) list.add("${root.getJSONArray("rutes").length()} Rute")
        if (root.has("pabriks")) list.add("${root.getJSONArray("pabriks").length()} Pabrik")
        if (root.has("transactions")) list.add("${root.getJSONArray("transactions").length()} Tx")
        if (root.has("loadings")) list.add("${root.getJSONArray("loadings").length()} Muat Pagi")
        return if (list.isEmpty()) "Kosong" else list.joinToString(", ")
    }

    /**
     * Automatically inspects imported file (either .zip or .json stream/uri)
     * and restores database tables + outlet photos.
     */
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        repository: SfaRepository,
        selection: BackupSelection = BackupSelection()
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val isZip = isZipFile(context, uri)

            if (isZip) {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: return@withContext ImportResult(false, "Tidak dapat membuka stream file ZIP.")
                importFromZipStream(context, inputStream, repository, selection)
            } else {
                val jsonString = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: return@withContext ImportResult(false, "File JSON tidak dapat dibaca.")
                importModularDatabaseFromJson(jsonString, repository, selection, context)
            }
        } catch (e: Exception) {
            ImportResult(false, "Gagal mengimpor file backup: ${e.localizedMessage}")
        }
    }

    private fun isZipFile(context: Context, uri: Uri): Boolean {
        try {
            val path = uri.path?.lowercase() ?: ""
            if (path.endsWith(".zip")) return true

            // Read magic bytes: ZIP files start with PK\x03\x04
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                if (read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    suspend fun importFromZipStream(
        context: Context,
        inputStream: InputStream,
        repository: SfaRepository,
        selection: BackupSelection = BackupSelection()
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val photosDir = ImageCompressor.getOutletPhotosDir(context)
            var jsonString: String? = null
            var restoredPhotos = 0

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName == "backup_data.json" || (!entryName.contains("/") && entryName.endsWith(".json", true))) {
                        jsonString = zis.bufferedReader(Charsets.UTF_8).readText()
                    } else if (entryName.startsWith("photos/") || entryName.endsWith(".jpg", true) || entryName.endsWith(".png", true) || entryName.endsWith(".webp", true)) {
                        val fileName = File(entryName).name
                        if (fileName.isNotBlank()) {
                            val targetFile = File(photosDir, fileName)
                            FileOutputStream(targetFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            restoredPhotos++
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (jsonString.isNullOrBlank()) {
                return@withContext ImportResult(false, "File .zip tidak memuat metadata backup_data.json yang valid.")
            }

            val result = importModularDatabaseFromJson(
                jsonString = jsonString!!,
                repository = repository,
                selection = selection,
                context = context
            )

            result.copy(
                photosCount = restoredPhotos,
                message = "${result.message} (${restoredPhotos} foto outlet berhasil dipulihkan)."
            )
        } catch (e: Exception) {
            ImportResult(false, "Gagal mengekstrak dan memulihkan ZIP backup: ${e.localizedMessage}")
        }
    }

    suspend fun importModularDatabaseFromJson(
        jsonString: String,
        repository: SfaRepository,
        selection: BackupSelection = BackupSelection(),
        context: Context? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val importedList = mutableListOf<String>()
            val photosDir = context?.let { ImageCompressor.getOutletPhotosDir(it) }

            // 1. Restore User Profile
            if (selection.exportProfile && root.has("userProfile")) {
                val profObj = root.getJSONObject("userProfile")
                val profile = UserProfileEntity(
                    id = "PRIMARY_PROFILE",
                    namaSalesman = profObj.optString("namaSalesman", ""),
                    noHp = profObj.optString("noHp", ""),
                    namaDistributor = profObj.optString("namaDistributor", ""),
                    alamatDepo = profObj.optString("alamatDepo", ""),
                    platNomorMobil = profObj.optString("platNomorMobil", ""),
                    areaOperasional = profObj.optString("areaOperasional", ""),
                    pinKeamanan = profObj.optString("pinKeamanan", ""),
                    isConfigured = profObj.optBoolean("isConfigured", true),
                    createdAt = profObj.optLong("createdAt", System.currentTimeMillis())
                )
                repository.saveUserProfile(profile)
                importedList.add("Profil Salesman")
            }

            // 2. Restore Products
            var prodCount = 0
            if (selection.exportProducts && root.has("products")) {
                val prodArray = root.getJSONArray("products")
                val productList = mutableListOf<ProductEntity>()
                for (i in 0 until prodArray.length()) {
                    val o = prodArray.getJSONObject(i)
                    productList.add(
                        ProductEntity(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            nama = o.optString("nama", "Produk"),
                            kategori = o.optString("kategori", "Makanan & Minuman"),
                            satuanBesar = o.optString("satuanBesar", "Dus"),
                            satuanKecil = o.optString("satuanKecil", "Pcs"),
                            rasioKonversi = o.optInt("rasioKonversi", 10),
                            hargaBeliPabrik = o.optDouble("hargaBeliPabrik", 11000.0),
                            hargaJualDefault = o.optDouble("hargaJualDefault", 1600.0),
                            stokMinimumAlert = o.optInt("stokMinimumAlert", 20),
                            status = o.optString("status", "Aktif"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (productList.isNotEmpty()) {
                    repository.insertProductsBatch(productList)
                    prodCount = productList.size
                    importedList.add("$prodCount SKU Produk")
                }
            }

            // 3. Restore Rutes
            var ruteCount = 0
            if (selection.exportRutes && root.has("rutes")) {
                val ruteArray = root.getJSONArray("rutes")
                val ruteList = mutableListOf<RuteEntity>()
                for (i in 0 until ruteArray.length()) {
                    val o = ruteArray.getJSONObject(i)
                    ruteList.add(
                        RuteEntity(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            namaRute = o.optString("namaRute", "Rute"),
                            hariKunjungan = o.optString("hariKunjungan", "Senin"),
                            idSalesman = o.optString("idSalesman", "SALES-01"),
                            estimasiJumlahWarung = o.optInt("estimasiJumlahWarung", 50),
                            jarakTotalKm = o.optDouble("jarakTotalKm", 20.0),
                            status = o.optString("status", "Aktif")
                        )
                    )
                }
                if (ruteList.isNotEmpty()) {
                    repository.insertRutesBatch(ruteList)
                    ruteCount = ruteList.size
                    importedList.add("$ruteCount Rute")
                }
            }

            // 4. Restore Pabriks
            var pabrikCount = 0
            if (selection.exportPabriks && root.has("pabriks")) {
                val pabrikArray = root.getJSONArray("pabriks")
                val pabrikList = mutableListOf<PabrikEntity>()
                for (i in 0 until pabrikArray.length()) {
                    val o = pabrikArray.getJSONObject(i)
                    pabrikList.add(
                        PabrikEntity(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            namaPabrik = o.optString("namaPabrik", "Pabrik"),
                            alamatLengkap = o.optString("alamatLengkap", ""),
                            noHpCp = o.optString("noHpCp", ""),
                            namaCp = o.optString("namaCp", ""),
                            syaratPembayaran = o.optString("syaratPembayaran", "Harian"),
                            kebijakanRetur = o.optString("kebijakanRetur", "BS Tidak Diterima"),
                            rekeningBank = o.optString("rekeningBank", ""),
                            status = o.optString("status", "Aktif")
                        )
                    )
                }
                if (pabrikList.isNotEmpty()) {
                    repository.insertPabriksBatch(pabrikList)
                    pabrikCount = pabrikList.size
                    importedList.add("$pabrikCount Pabrik")
                }
            }

            // 5. Restore Warungs & Remap Photo Paths
            var warungCount = 0
            if (selection.exportWarungs && root.has("warungs")) {
                val warungArray = root.getJSONArray("warungs")
                val warungList = mutableListOf<WarungEntity>()
                for (i in 0 until warungArray.length()) {
                    val o = warungArray.getJSONObject(i)
                    val rawPhoto = o.optString("fotoOutlet", "")
                    val photoFileName = o.optString("photoFileName", "")

                    // Re-link photo to local storage on this phone
                    var finalPhoto: String? = null
                    if (photoFileName.isNotBlank() && photosDir != null) {
                        val localPhotoFile = File(photosDir, photoFileName)
                        if (localPhotoFile.exists()) {
                            finalPhoto = Uri.fromFile(localPhotoFile).toString()
                        }
                    }
                    if (finalPhoto == null && rawPhoto.isNotBlank()) {
                        finalPhoto = rawPhoto
                    }

                    val rawUrutan = o.optInt("urutanKunjungan", 0)
                    val sequenceNo = if (rawUrutan > 0) rawUrutan else (i + 1)
                    val rawRuteId = o.optString("ruteId", "")
                    val finalRuteId = if (rawRuteId.isNotBlank()) rawRuteId else (repository.getAllRutesDirect().firstOrNull()?.id ?: "")

                    warungList.add(
                        WarungEntity(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            namaWarung = o.optString("namaWarung", "Warung"),
                            namaPemilik = o.optString("namaPemilik", ""),
                            ruteId = finalRuteId,
                            latitude = o.optDouble("latitude", -6.2088),
                            longitude = o.optDouble("longitude", 106.8456),
                            alamatLengkap = o.optString("alamatLengkap", ""),
                            akurasiGpsMeter = o.optInt("akurasiGpsMeter", 12),
                            noHp = o.optString("noHp", ""),
                            kategoriWarung = o.optString("kategoriWarung", "Kelontong"),
                            limitHutangMaksimal = o.optDouble("limitHutangMaksimal", 500000.0),
                            saldoPiutang = o.optDouble("saldoPiutang", 0.0),
                            stokTitipanPcs = o.optInt("stokTitipanPcs", 0),
                            tglKunjunganTerakhir = o.optLong("tglKunjunganTerakhir", System.currentTimeMillis()),
                            tglMulaiHutang = o.optLong("tglMulaiHutang", System.currentTimeMillis()),
                            urutanKunjungan = sequenceNo,
                            notes = o.optString("notes", ""),
                            status = o.optString("status", "Aktif"),
                            fotoOutlet = finalPhoto?.takeIf { it.isNotBlank() },
                            tanggalBerlangganan = o.optLong("tanggalBerlangganan", System.currentTimeMillis()),
                            pendingAddressSync = o.optBoolean("pendingAddressSync", false)
                        )
                    )
                }
                if (warungList.isNotEmpty()) {
                    repository.insertWarungsBatch(warungList)
                    warungCount = warungList.size
                    importedList.add("$warungCount Outlet")
                }
            }

            // 6. Restore Custom Prices
            if (selection.exportCustomPrices && root.has("customPrices")) {
                val cpArray = root.getJSONArray("customPrices")
                val cpList = mutableListOf<WarungCustomPriceEntity>()
                for (i in 0 until cpArray.length()) {
                    val o = cpArray.getJSONObject(i)
                    cpList.add(
                        WarungCustomPriceEntity(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            warungId = o.optString("warungId", ""),
                            productId = o.optString("productId", ""),
                            hargaJualPcs = o.optDouble("hargaJualPcs", 0.0),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                if (cpList.isNotEmpty()) {
                    repository.insertCustomPricesBatch(cpList)
                }
            }

            // 7. Restore Drawers, Loadings, BsSortirs, WriteOffs
            if (selection.exportInventory) {
                if (root.has("drawers")) {
                    val drawerArray = root.getJSONArray("drawers")
                    val drawerList = mutableListOf<InventoryDrawerEntity>()
                    for (i in 0 until drawerArray.length()) {
                        val o = drawerArray.getJSONObject(i)
                        drawerList.add(
                            InventoryDrawerEntity(
                                id = o.optString("id", UUID.randomUUID().toString()),
                                productId = o.optString("productId", ""),
                                stokFreshPabrikPcs = o.optInt("stokFreshPabrikPcs", 0),
                                stokBsBelumSortirPcs = o.optInt("stokBsBelumSortirPcs", 0),
                                stokPribadiLayakJualPcs = o.optInt("stokPribadiLayakJualPcs", 0),
                                stokPribadiRusakPcs = o.optInt("stokPribadiRusakPcs", 0),
                                lastUpdated = o.optLong("lastUpdated", System.currentTimeMillis())
                            )
                        )
                    }
                    if (drawerList.isNotEmpty()) {
                        repository.insertDrawersBatch(drawerList)
                        importedList.add("Laci Stok")
                    }
                }

                if (root.has("loadings")) {
                    val loadingArray = root.getJSONArray("loadings")
                    val loadingList = mutableListOf<DailyLoadingEntity>()
                    for (i in 0 until loadingArray.length()) {
                        val o = loadingArray.getJSONObject(i)
                        loadingList.add(
                            DailyLoadingEntity(
                                id = o.optString("id", UUID.randomUUID().toString()),
                                tanggal = o.optString("tanggal", ""),
                                productId = o.optString("productId", ""),
                                jumlahDus = o.optInt("jumlahDus", 0),
                                rasioKonversi = o.optInt("rasioKonversi", 10),
                                totalPcs = o.optInt("totalPcs", 0),
                                hargaBeliPabrikDus = o.optDouble("hargaBeliPabrikDus", 0.0),
                                potensiHutangPabrik = o.optDouble("potensiHutangPabrik", 0.0),
                                sisaDusSore = o.optInt("sisaDusSore", 0),
                                terjualDus = o.optInt("terjualDus", 0),
                                tagihanPabrikClosing = o.optDouble("tagihanPabrikClosing", 0.0),
                                statusClosing = o.optBoolean("statusClosing", false),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                    if (loadingList.isNotEmpty()) {
                        repository.insertDailyLoadingsBatch(loadingList)
                        importedList.add("${loadingList.size} Riwayat Muat Pagi")
                    }
                }

                if (root.has("bsSortirs")) {
                    val sortirArray = root.getJSONArray("bsSortirs")
                    val sortirList = mutableListOf<BsSortirEntity>()
                    for (i in 0 until sortirArray.length()) {
                        val o = sortirArray.getJSONObject(i)
                        sortirList.add(
                            BsSortirEntity(
                                id = o.optString("id", UUID.randomUUID().toString()),
                                productId = o.optString("productId", ""),
                                totalBsAwalPcs = o.optInt("totalBsAwalPcs", 0),
                                bsLayakJualPcs = o.optInt("bsLayakJualPcs", 0),
                                bsRusakPcs = o.optInt("bsRusakPcs", 0),
                                estimasiNilaiModal = o.optDouble("estimasiNilaiModal", 0.0),
                                estimasiNilaiJual = o.optDouble("estimasiNilaiJual", 0.0),
                                estimasiProfitMurni = o.optDouble("estimasiProfitMurni", 0.0),
                                catatan = o.optString("catatan", ""),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    if (sortirList.isNotEmpty()) {
                        repository.insertBsSortirsBatch(sortirList)
                    }
                }

                if (root.has("writeOffs")) {
                    val woArray = root.getJSONArray("writeOffs")
                    val woList = mutableListOf<WriteOffEntity>()
                    for (i in 0 until woArray.length()) {
                        val o = woArray.getJSONObject(i)
                        woList.add(
                            WriteOffEntity(
                                id = o.optString("id", UUID.randomUUID().toString()),
                                warungId = o.optString("warungId", ""),
                                namaWarung = o.optString("namaWarung", ""),
                                piutangDihapus = o.optDouble("piutangDihapus", 0.0),
                                stokHangusPcs = o.optInt("stokHangusPcs", 0),
                                nilaiStokHangus = o.optDouble("nilaiStokHangus", 0.0),
                                totalKerugian = o.optDouble("totalKerugian", 0.0),
                                alasan = o.optString("alasan", "Write-Off"),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    if (woList.isNotEmpty()) {
                        repository.insertWriteOffsBatch(woList)
                    }
                }
            }

            // 8. Restore Transactions
            var txCount = 0
            if (selection.exportTransactions && root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                val txList = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    txList.add(
                        TransactionEntity(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            warungId = o.optString("warungId", ""),
                            ruteId = o.optString("ruteId", ""),
                            productId = o.optString("productId", ""),
                            tanggal = o.optString("tanggal", ""),
                            jenis = o.optString("jenis", "TITIP_BARU"),
                            sumberStok = o.optString("sumberStok", "FRESH_PABRIK"),
                            sisaTitipanLaluPcs = o.optInt("sisaTitipanLaluPcs", 0),
                            sisaFisikPcs = o.optInt("sisaFisikPcs", 0),
                            pcsLaku = o.optInt("pcsLaku", 0),
                            hargaSatuan = o.optDouble("hargaSatuan", 0.0),
                            subtotalLaku = o.optDouble("subtotalLaku", 0.0),
                            saldoPiutangLama = o.optDouble("saldoPiutangLama", 0.0),
                            grandTotalTagihan = o.optDouble("grandTotalTagihan", 0.0),
                            uangDiterima = o.optDouble("uangDiterima", 0.0),
                            saldoPiutangBaru = o.optDouble("saldoPiutangBaru", 0.0),
                            statusBayar = o.optString("statusBayar", "LUNAS"),
                            bsDitarikPcs = o.optInt("bsDitarikPcs", 0),
                            restockBaruPcs = o.optInt("restockBaruPcs", 0),
                            totalTitipanAktifPcs = o.optInt("totalTitipanAktifPcs", 0),
                            gpsLat = o.optDouble("gpsLat", 0.0),
                            gpsLng = o.optDouble("gpsLng", 0.0),
                            gpsAddress = o.optString("gpsAddress", ""),
                            catatan = o.optString("catatan", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (txList.isNotEmpty()) {
                    repository.insertTransactionsBatch(txList)
                    txCount = txList.size
                    importedList.add("$txCount Transaksi")
                }
            }

            ImportResult(
                success = true,
                message = "Berhasil memulihkan data: " + (if (importedList.isEmpty()) "Tidak ada entitas yang dipilih/ditemukan" else importedList.joinToString(", ")),
                productsCount = prodCount,
                warungsCount = warungCount,
                rutesCount = ruteCount,
                transactionsCount = txCount,
                pabriksCount = pabrikCount
            )
        } catch (e: Exception) {
            ImportResult(false, "Gagal mengimpor data: ${e.message}")
        }
    }

    suspend fun importFullDatabaseFromJson(
        jsonString: String,
        repository: SfaRepository,
        context: Context? = null
    ): ImportResult {
        return importModularDatabaseFromJson(jsonString, repository, BackupSelection(), context)
    }

    /**
     * Copies a local file into a Storage Access Framework (SAF) destination URI
     */
    suspend fun copyFileToUri(context: Context, sourceFile: File, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Shares a backup file (either .zip or .json) safely using FileProvider content:// URI
     */
    fun shareBackupFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
