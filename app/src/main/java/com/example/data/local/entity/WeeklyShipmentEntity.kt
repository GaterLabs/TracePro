package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entitas Kiriman Mingguan (Pool Rumah / Gudang Pribadi):
 * Menyimpan batch kiriman dari pabrik / bos mingguan.
 * Misal: jatah 300 pack, atau kiriman baru 250 pack yang otomatis terakumulasi dengan sisa lama.
 */
@Entity(tableName = "weekly_shipments")
data class WeeklyShipmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tanggal: String,
    val productId: String,
    val jumlahPack: Int,
    val rasioKonversi: Int = 10,
    val totalPcs: Int = jumlahPack * rasioKonversi,
    val hargaBeliPerPack: Double = 0.0,
    val totalNilaiBeli: Double = jumlahPack * hargaBeliPerPack,
    val nomorDoAtauNota: String = "",
    val namaSupplier: String = "",
    val catatan: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
