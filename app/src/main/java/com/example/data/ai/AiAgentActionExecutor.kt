package com.example.data.ai

import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.RuteEntity
import com.example.data.local.entity.WarungEntity
import com.example.data.repository.SfaRepository
import org.json.JSONObject
import java.util.UUID

/**
 * Eksekutor pemanggilan tools (Agent Action Executor).
 * Mengubah instruksi JSON dari LLM menjadi manipulasi data langsung pada Room Database TracerPro SFA.
 */
class AiAgentActionExecutor(
    private val repository: SfaRepository
) {

    suspend fun executeToolCall(toolCall: AiToolCall): AiToolExecutionResult {
        return try {
            val args = try {
                JSONObject(toolCall.argumentsJson)
            } catch (e: Exception) {
                JSONObject()
            }

            when (toolCall.name) {
                "add_warung" -> executeAddWarung(toolCall.id, args)
                "update_warung" -> executeUpdateWarung(toolCall.id, args)
                "record_transaction" -> executeRecordTransaction(toolCall.id, args)
                "pay_outlet_debt" -> executePayOutletDebt(toolCall.id, args)
                "set_custom_price" -> executeSetCustomPrice(toolCall.id, args)
                "add_product" -> executeAddProduct(toolCall.id, args)
                "add_rute" -> executeAddRute(toolCall.id, args)
                "mark_outlet_visited" -> executeMarkOutletVisited(toolCall.id, args)
                else -> {
                    AiToolExecutionResult(
                        toolCallId = toolCall.id,
                        toolName = toolCall.name,
                        isSuccess = false,
                        summary = "Tool '${toolCall.name}' tidak dikenal atau belum didukung."
                    )
                }
            }
        } catch (e: Exception) {
            AiToolExecutionResult(
                toolCallId = toolCall.id,
                toolName = toolCall.name,
                isSuccess = false,
                summary = "Gagal menjalankan '${toolCall.name}': ${e.message}"
            )
        }
    }

    private suspend fun executeAddWarung(callId: String, args: JSONObject): AiToolExecutionResult {
        val namaWarung = args.optString("nama_warung", "").trim()
        if (namaWarung.isBlank()) {
            return AiToolExecutionResult(callId, "add_warung", false, "Nama warung wajib diisi.")
        }

        val namaPemilik = args.optString("nama_pemilik", "").trim()
        val noHp = args.optString("no_hp", "").trim()
        val alamat = args.optString("alamat", "").trim()
        val kategori = args.optString("kategori", "WARUNG_KELONTONG").trim()
        val limitHutang = args.optDouble("limit_hutang", 500000.0).coerceAtLeast(0.0)
        val ruteName = args.optString("rute_name", "").trim()

        val allRutes = repository.getAllRutesDirect()
        val targetRute = if (ruteName.isNotBlank()) {
            allRutes.find { it.namaRute.contains(ruteName, ignoreCase = true) } ?: allRutes.firstOrNull()
        } else {
            allRutes.firstOrNull()
        }

        val ruteId = targetRute?.id ?: "RUTE-01"
        val ruteDisplayName = targetRute?.namaRute ?: "Rute Utama"

        val newWarung = WarungEntity(
            id = "W-${UUID.randomUUID().toString().take(8).uppercase()}",
            ruteId = ruteId,
            namaWarung = namaWarung,
            namaPemilik = namaPemilik.ifBlank { "Pemilik $namaWarung" },
            noHp = noHp,
            alamatLengkap = alamat.ifBlank { "Area $ruteDisplayName" },
            kategoriWarung = kategori,
            limitHutangMaksimal = limitHutang,
            saldoPiutang = 0.0,
            stokTitipanPcs = 0,
            tglKunjunganTerakhir = System.currentTimeMillis()
        )

        repository.saveWarung(newWarung)

        return AiToolExecutionResult(
            toolCallId = callId,
            toolName = "add_warung",
            isSuccess = true,
            summary = "Berhasil menambahkan warung '$namaWarung' ke rute '$ruteDisplayName' (Limit Bon: Rp ${limitHutang.toLong()}).",
            detailDataJson = JSONObject().apply {
                put("warungId", newWarung.id)
                put("namaWarung", newWarung.namaWarung)
                put("rute", ruteDisplayName)
            }.toString()
        )
    }

    private suspend fun executeUpdateWarung(callId: String, args: JSONObject): AiToolExecutionResult {
        val query = args.optString("warung_query", "").trim()
        if (query.isBlank()) {
            return AiToolExecutionResult(callId, "update_warung", false, "Nama warung yang dicari wajib diisi.")
        }

        val allWarungs = repository.getAllWarungsDirect()
        val warung = allWarungs.find { it.namaWarung.contains(query, ignoreCase = true) || it.id.equals(query, ignoreCase = true) }
            ?: return AiToolExecutionResult(callId, "update_warung", false, "Warung dengan pencarian '$query' tidak ditemukan.")

        var updated = warung
        val newName = args.optString("nama_warung_baru", "").trim()
        if (newName.isNotBlank()) updated = updated.copy(namaWarung = newName)

        val newPemilik = args.optString("nama_pemilik", "").trim()
        if (newPemilik.isNotBlank()) updated = updated.copy(namaPemilik = newPemilik)

        val newNoHp = args.optString("no_hp", "").trim()
        if (newNoHp.isNotBlank()) updated = updated.copy(noHp = newNoHp)

        val newAlamat = args.optString("alamat", "").trim()
        if (newAlamat.isNotBlank()) updated = updated.copy(alamatLengkap = newAlamat)

        if (args.has("limit_hutang")) {
            val limit = args.optDouble("limit_hutang", warung.limitHutangMaksimal)
            updated = updated.copy(limitHutangMaksimal = limit)
        }

        repository.saveWarung(updated)

        return AiToolExecutionResult(
            toolCallId = callId,
            toolName = "update_warung",
            isSuccess = true,
            summary = "Data warung '${warung.namaWarung}' berhasil diperbarui.",
            detailDataJson = JSONObject().apply {
                put("id", updated.id)
                put("namaWarung", updated.namaWarung)
                put("noHp", updated.noHp)
            }.toString()
        )
    }

    private suspend fun executeRecordTransaction(callId: String, args: JSONObject): AiToolExecutionResult {
        val warungQuery = args.optString("warung_query", "").trim()
        val productQuery = args.optString("product_query", "").trim()
        val pcsLaku = args.optInt("pcs_laku", 0).coerceAtLeast(0)
        val uangDiterima = args.optDouble("uang_diterima", 0.0).coerceAtLeast(0.0)
        val restockPcs = if (args.has("restock_pcs")) args.optInt("restock_pcs", 0) else pcsLaku
        val sumberRestock = args.optString("sumber_restock", "FRESH_PABRIK")
        val catatan = args.optString("catatan", "Transaksi dicatat oleh AI Agent")

        val allWarungs = repository.getAllWarungsDirect()
        val warung = allWarungs.find { it.namaWarung.contains(warungQuery, ignoreCase = true) }
            ?: return AiToolExecutionResult(callId, "record_transaction", false, "Warung '$warungQuery' tidak ditemukan.")

        val allProducts = repository.getAllProductsDirect()
        val product = allProducts.find { it.nama.contains(productQuery, ignoreCase = true) }
            ?: return AiToolExecutionResult(callId, "record_transaction", false, "Produk '$productQuery' tidak ditemukan.")

        // Ambil harga khusus atau default
        val customPrice = repository.getCustomPrice(warung.id, product.id)
        val hargaSatuan = customPrice ?: product.hargaJualDefault

        // Jika warung belum punya transaksi sebelumnya untuk produk ini dan pcsLaku == 0, lakukan Titip Baru
        val txList = repository.getTransactionsByWarungSync(warung.id)
        val prevTitipanThisProd = txList.filter { it.productId == product.id }.maxByOrNull { it.timestamp }?.totalTitipanAktifPcs ?: 0

        if (prevTitipanThisProd == 0 && pcsLaku == 0 && restockPcs > 0) {
            repository.processTitipBaru(
                warung = warung,
                productId = product.id,
                sumberStok = sumberRestock,
                jumlahPcs = restockPcs,
                hargaSatuan = hargaSatuan,
                gpsLat = warung.latitude,
                gpsLng = warung.longitude,
                gpsAddress = warung.alamatLengkap,
                catatan = catatan
            )
            return AiToolExecutionResult(
                toolCallId = callId,
                toolName = "record_transaction",
                isSuccess = true,
                summary = "Titip konsinyasi baru berhasil di '${warung.namaWarung}': +$restockPcs pcs ${product.nama} (Rp ${hargaSatuan.toLong()}/pcs)."
            )
        }

        // Hitung sisa fisik di warung
        val sisaTitipanLalu = prevTitipanThisProd.coerceAtLeast(pcsLaku)
        val sisaFisik = (sisaTitipanLalu - pcsLaku).coerceAtLeast(0)

        repository.processTarikSisaDanRestock(
            warung = warung,
            productId = product.id,
            sisaTitipanLalu = sisaTitipanLalu,
            sisaFisik = sisaFisik,
            hargaSatuan = hargaSatuan,
            uangDiterima = uangDiterima,
            restockPcs = restockPcs,
            sumberRestock = sumberRestock,
            gpsLat = warung.latitude,
            gpsLng = warung.longitude,
            gpsAddress = warung.alamatLengkap,
            catatan = catatan
        )

        val totalOmset = pcsLaku * hargaSatuan
        return AiToolExecutionResult(
            toolCallId = callId,
            toolName = "record_transaction",
            isSuccess = true,
            summary = "Transaksi berhasil di '${warung.namaWarung}': Laku $pcsLaku pcs ${product.nama} (Omset Rp ${totalOmset.toLong()}), Bayar Cash Rp ${uangDiterima.toLong()}, Restock $restockPcs pcs."
        )
    }

    private suspend fun executePayOutletDebt(callId: String, args: JSONObject): AiToolExecutionResult {
        val warungQuery = args.optString("warung_query", "").trim()
        val nominal = args.optDouble("nominal_bayar", 0.0).coerceAtLeast(0.0)
        val catatan = args.optString("catatan", "Pelunasan bon via AI Agent")

        if (nominal <= 0) {
            return AiToolExecutionResult(callId, "pay_outlet_debt", false, "Nominal bayar harus lebih dari 0.")
        }

        val allWarungs = repository.getAllWarungsDirect()
        val warung = allWarungs.find { it.namaWarung.contains(warungQuery, ignoreCase = true) }
            ?: return AiToolExecutionResult(callId, "pay_outlet_debt", false, "Warung '$warungQuery' tidak ditemukan.")

        val allProducts = repository.getAllProductsDirect()
        val fallbackProd = allProducts.firstOrNull()
        val productId = fallbackProd?.id ?: "PROD-GENERAL"

        // Catat sebagai transaksi pelunasan khusus
        val newSaldo = (warung.saldoPiutang - nominal).coerceAtLeast(0.0)
        val updatedWarung = warung.copy(saldoPiutang = newSaldo, tglKunjunganTerakhir = System.currentTimeMillis())
        repository.saveWarung(updatedWarung)

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val tx = com.example.data.local.entity.TransactionEntity(
            warungId = warung.id,
            ruteId = warung.ruteId,
            productId = productId,
            tanggal = today,
            jenis = "BAYAR_BON",
            sumberStok = "FRESH_PABRIK",
            sisaTitipanLaluPcs = warung.stokTitipanPcs,
            sisaFisikPcs = warung.stokTitipanPcs,
            pcsLaku = 0,
            hargaSatuan = 0.0,
            subtotalLaku = 0.0,
            saldoPiutangLama = warung.saldoPiutang,
            grandTotalTagihan = warung.saldoPiutang,
            uangDiterima = nominal,
            saldoPiutangBaru = newSaldo,
            statusBayar = if (newSaldo <= 0.0) "LUNAS" else "SEBAGIAN",
            bsDitarikPcs = 0,
            restockBaruPcs = 0,
            totalTitipanAktifPcs = warung.stokTitipanPcs,
            gpsLat = warung.latitude,
            gpsLng = warung.longitude,
            gpsAddress = warung.alamatLengkap,
            catatan = catatan
        )
        repository.insertTransaction(tx)

        return AiToolExecutionResult(
            toolCallId = callId,
            toolName = "pay_outlet_debt",
            isSuccess = true,
            summary = "Pelunasan piutang '${warung.namaWarung}' sebesar Rp ${nominal.toLong()} berhasil dicatat. Sisa bon: Rp ${newSaldo.toLong()}."
        )
    }

    private suspend fun executeSetCustomPrice(callId: String, args: JSONObject): AiToolExecutionResult {
        val warungQuery = args.optString("warung_query", "").trim()
        val productQuery = args.optString("product_query", "").trim()
        val hargaJualPcs = args.optDouble("harga_jual_pcs", 0.0)

        val allWarungs = repository.getAllWarungsDirect()
        val warung = allWarungs.find { it.namaWarung.contains(warungQuery, ignoreCase = true) }
            ?: return AiToolExecutionResult(callId, "set_custom_price", false, "Warung '$warungQuery' tidak ditemukan.")

        val allProducts = repository.getAllProductsDirect()
        val product = allProducts.find { it.nama.contains(productQuery, ignoreCase = true) }
            ?: return AiToolExecutionResult(callId, "set_custom_price", false, "Produk '$productQuery' tidak ditemukan.")

        if (hargaJualPcs <= 0.0) {
            repository.deleteCustomPrice(warung.id, product.id)
            return AiToolExecutionResult(
                toolCallId = callId,
                toolName = "set_custom_price",
                isSuccess = true,
                summary = "Harga khusus untuk ${product.nama} di ${warung.namaWarung} telah dihapus. Kembali ke harga standar (Rp ${product.hargaJualDefault.toLong()}/pcs)."
            )
        } else {
            repository.saveCustomPrice(warung.id, product.id, hargaJualPcs)
            return AiToolExecutionResult(
                toolCallId = callId,
                toolName = "set_custom_price",
                isSuccess = true,
                summary = "Harga khusus ${product.nama} di '${warung.namaWarung}' berhasil disetel menjadi Rp ${hargaJualPcs.toLong()}/pcs."
            )
        }
    }

    private suspend fun executeAddProduct(callId: String, args: JSONObject): AiToolExecutionResult {
        val nama = args.optString("nama_produk", "").trim()
        val kategori = args.optString("kategori", "Umum").trim()
        val satuan = args.optString("satuan_kemasan", "Dus").trim()
        val rasio = args.optInt("rasio_konversi", 24).coerceAtLeast(1)
        val hargaBeliDus = args.optDouble("harga_beli_dus", 0.0).coerceAtLeast(0.0)
        val hargaJualPcs = args.optDouble("harga_jual_pcs", 0.0).coerceAtLeast(0.0)
        val namaPabrik = args.optString("nama_pabrik", "").trim()

        if (nama.isBlank() || hargaJualPcs <= 0.0) {
            return AiToolExecutionResult(callId, "add_product", false, "Nama produk dan harga jual pcs wajib diisi.")
        }

        val allPabriks = repository.getAllPabriksDirect()
        val targetPabrik = if (namaPabrik.isNotBlank()) {
            allPabriks.find { it.namaPabrik.contains(namaPabrik, ignoreCase = true) } ?: allPabriks.firstOrNull()
        } else {
            allPabriks.firstOrNull()
        }
        val pabrikId = targetPabrik?.id

        val newProduct = ProductEntity(
            id = "PROD-${UUID.randomUUID().toString().take(6).uppercase()}",
            nama = nama,
            kategori = kategori,
            pabrikId = pabrikId,
            satuanBesar = satuan,
            satuanKecil = "Pcs",
            rasioKonversi = rasio,
            hargaBeliPabrik = hargaBeliDus,
            hargaJualDefault = hargaJualPcs
        )
        repository.saveProduct(newProduct)

        return AiToolExecutionResult(
            toolCallId = callId,
            toolName = "add_product",
            isSuccess = true,
            summary = "Master produk '$nama' berhasil ditambahkan (Isi $rasio pcs/$satuan, Harga Jual Rp ${hargaJualPcs.toLong()}/pcs)."
        )
    }

    private suspend fun executeAddRute(callId: String, args: JSONObject): AiToolExecutionResult {
        val namaRute = args.optString("nama_rute", "").trim()
        val hari = args.optString("hari_kunjungan", "Senin").trim()

        if (namaRute.isBlank()) {
            return AiToolExecutionResult(callId, "add_rute", false, "Nama rute wajib diisi.")
        }

        val newRute = RuteEntity(
            id = "RUTE-${UUID.randomUUID().toString().take(6).uppercase()}",
            namaRute = namaRute,
            hariKunjungan = hari
        )
        repository.saveRute(newRute)

        return AiToolExecutionResult(
            toolCallId = callId,
            toolName = "add_rute",
            isSuccess = true,
            summary = "Rute distribusi '$namaRute' ($hari) berhasil dibuat."
        )
    }

    private suspend fun executeMarkOutletVisited(callId: String, args: JSONObject): AiToolExecutionResult {
        val warungQuery = args.optString("warung_query", "").trim()
        val allWarungs = repository.getAllWarungsDirect()
        val warung = allWarungs.find { it.namaWarung.contains(warungQuery, ignoreCase = true) }
            ?: return AiToolExecutionResult(callId, "mark_outlet_visited", false, "Warung '$warungQuery' tidak ditemukan.")

        val updated = warung.copy(tglKunjunganTerakhir = System.currentTimeMillis())
        repository.saveWarung(updated)

        return AiToolExecutionResult(
            toolCallId = callId,
            toolName = "mark_outlet_visited",
            isSuccess = true,
            summary = "Kunjungan ke '${warung.namaWarung}' berhasil ditandai selesai hari ini."
        )
    }
}
