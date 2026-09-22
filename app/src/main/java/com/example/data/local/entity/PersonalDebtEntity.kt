package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for personal debt records (Hutang & Piutang Pribadi):
 * - "PIUTANG_TEMAN_KELUARGA" (Orang lain berhutang ke kita / mereka pinjam uang kita)
 * - "HUTANG_KITA" (Kita yang berhutang ke teman / keluarga / pihak lain)
 */
@Entity(tableName = "personal_debts")
data class PersonalDebtEntity(
    @PrimaryKey
    val id: String,
    val jenis: String,                 // "PIUTANG_SAYA" (Orang hutang ke kita), "HUTANG_SAYA" (Kita hutang ke orang)
    val namaPihak: String,             // Nama teman, keluarga, rekan e.g. "Budi Teman", "Om Joko", "Kak Lisa"
    val hubungan: String = "Teman",    // "Teman", "Keluarga", "Rekan Kerja", "Lainnya"
    val kontak: String = "",           // No HP / WhatsApp
    val totalNominal: Double,          // Total pinjaman awal
    val sisaNominal: Double,           // Sisa yang belum dilunasi
    val tanggalPinjam: String,         // YYYY-MM-DD
    val tanggalJatuhTempo: String = "",// YYYY-MM-DD (optional deadline)
    val status: String = "BELUM_LUNAS",// "BELUM_LUNAS", "LUNAS", "SEBAGIAN"
    val catatan: String = "",
    val riwayatBayarJson: String = "[]", // JSON array of payment logs
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
