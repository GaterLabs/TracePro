package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "bs_sortirs",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["timestamp"])
    ]
)
data class BsSortirEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val totalBsAwalPcs: Int,
    val bsLayakJualPcs: Int, // masuk ke stok_pribadi_layak_jual
    val bsRusakPcs: Int, // masuk ke kerugian pribadi (write-off)
    val estimasiNilaiModal: Double = 0.0,
    val estimasiNilaiJual: Double = 0.0,
    val estimasiProfitMurni: Double = 0.0,
    val catatan: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
