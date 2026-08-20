package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nama: String,
    val kategori: String = "Makanan & Minuman",
    val pabrikId: String? = null, // Relasi ke PabrikEntity (Supplier / Principal)
    val satuanBesar: String = "Pack",
    val satuanKecil: String = "Pcs",
    val rasioKonversi: Int = 10, // 1 Satuan Besar = X Satuan Kecil
    val hargaBeliPabrik: Double = 11000.0, // Per Satuan Besar
    val hargaJualDefault: Double = 1600.0, // Per Satuan Kecil
    val stokMinimumAlert: Int = 20,
    val status: String = "Aktif", // Aktif, Non-Aktif
    val createdAt: Long = System.currentTimeMillis()
)
