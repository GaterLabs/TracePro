package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pabriks")
data class PabrikEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val namaPabrik: String,
    val alamatLengkap: String = "",
    val noHpCp: String = "",
    val namaCp: String = "",
    val syaratPembayaran: String = "Harian", // Harian, Mingguan, Tempo
    val kebijakanRetur: String = "BS Tidak Diterima (Salesman Wajib Tebus)",
    val rekeningBank: String = "BCA 8730-1928-11 a/n PT Distribusi Sejahtera",
    val status: String = "Aktif"
)
