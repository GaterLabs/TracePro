package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "rutes")
data class RuteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val namaRute: String,
    val hariKunjungan: String = "Senin", // Senin, Selasa, Rabu, Kamis, Jumat, Sabtu, Minggu
    val idSalesman: String = "SALES-01",
    val estimasiJumlahWarung: Int = 50,
    val jarakTotalKm: Double = 24.5,
    val status: String = "Aktif"
)
