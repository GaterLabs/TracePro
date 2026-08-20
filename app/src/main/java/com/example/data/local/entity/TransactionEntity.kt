package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["warungId"]),
        Index(value = ["tanggal"]),
        Index(value = ["productId"]),
        Index(value = ["ruteId"]),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val warungId: String,
    val ruteId: String = "",
    val productId: String = "",
    val tanggal: String, // YYYY-MM-DD
    val jenis: String, // "TITIP_BARU" atau "TARIK_SISA"
    val sumberStok: String, // "FRESH_PABRIK" atau "PRIBADI_REPACK"
    val sisaTitipanLaluPcs: Int = 0, // Stok opname awal di warung
    val sisaFisikPcs: Int = 0, // Sisa barang sekarang
    val pcsLaku: Int = 0, // sisaTitipanLaluPcs - sisaFisikPcs
    val hargaSatuan: Double = 1600.0,
    val subtotalLaku: Double = 0.0,
    val saldoPiutangLama: Double = 0.0,
    val grandTotalTagihan: Double = 0.0,
    val uangDiterima: Double = 0.0,
    val saldoPiutangBaru: Double = 0.0,
    val statusBayar: String = "LUNAS", // "LUNAS", "SEBAGIAN", "BON_FULL"
    val bsDitarikPcs: Int = 0, // Barang sisa ditarik ke laci belum sortir
    val restockBaruPcs: Int = 0, // Barang baru yang dititipkan
    val totalTitipanAktifPcs: Int = 0, // sisaFisikPcs + restockBaruPcs (or restockBaruPcs)
    val gpsLat: Double = 0.0,
    val gpsLng: Double = 0.0,
    val gpsAddress: String = "",
    val catatan: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
