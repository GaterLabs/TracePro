package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "daily_loadings",
    indices = [
        Index(value = ["tanggal"]),
        Index(value = ["productId"])
    ]
)
data class DailyLoadingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tanggal: String, // YYYY-MM-DD
    val productId: String,
    val jumlahDus: Int, // Satuan besar (Pack / Dus)
    val rasioKonversi: Int = 10,
    val totalPcs: Int = jumlahDus * rasioKonversi,
    val hargaBeliPabrikDus: Double = 11000.0,
    val potensiHutangPabrik: Double = jumlahDus * hargaBeliPabrikDus,
    val sisaDusSore: Int = 0, // Input saat closing sore
    val terjualDus: Int = 0, // jumlahDus - sisaDusSore
    val tagihanPabrikClosing: Double = 0.0,
    val statusClosing: Boolean = false, // true jika sudah closing sore
    val opsiBayarMuat: String = "BAYAR_CLOSING", // "BAYAR_LANGSUNG", "BAYAR_CLOSING", "HUTANG"
    val jumlahBayarMuat: Double = 0.0,
    val sisaHutangMuat: Double = 0.0,
    val statusLunasHutang: Boolean = false,
    val catatanMuat: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
