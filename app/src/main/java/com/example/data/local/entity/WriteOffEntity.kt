package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "write_offs",
    indices = [
        Index(value = ["warungId"]),
        Index(value = ["timestamp"])
    ]
)
data class WriteOffEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val warungId: String,
    val namaWarung: String,
    val piutangDihapus: Double,
    val stokHangusPcs: Int,
    val nilaiStokHangus: Double,
    val totalKerugian: Double,
    val alasan: String = "Warung Bangkrut / Tutup Permanen",
    val timestamp: Long = System.currentTimeMillis()
)
