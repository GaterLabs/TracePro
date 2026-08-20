package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "warungs",
    indices = [
        Index(value = ["ruteId"]),
        Index(value = ["status"]),
        Index(value = ["urutanKunjungan"]),
        Index(value = ["pendingAddressSync"]),
        Index(value = ["tglKunjunganTerakhir"]),
        Index(value = ["saldoPiutang"])
    ]
)
data class WarungEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val namaWarung: String,
    val namaPemilik: String = "",
    val ruteId: String = "",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val alamatLengkap: String = "",
    val akurasiGpsMeter: Int = 12,
    val noHp: String = "",
    val kategoriWarung: String = "Kelontong", // Kelontong, Minimarket, Kios, Toko Sembako
    val limitHutangMaksimal: Double = 500000.0,
    val saldoPiutang: Double = 0.0,
    val stokTitipanPcs: Int = 0,
    val tglKunjunganTerakhir: Long = System.currentTimeMillis(),
    val tglMulaiHutang: Long = System.currentTimeMillis(),
    val urutanKunjungan: Int = 1,
    val notes: String = "",
    val status: String = "Aktif", // Aktif, Tutup Sementara, Blacklist
    val fotoOutlet: String? = null,
    val tanggalBerlangganan: Long = System.currentTimeMillis(),
    val pendingAddressSync: Boolean = false
)
