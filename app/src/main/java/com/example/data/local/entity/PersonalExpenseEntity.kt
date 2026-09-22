package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for personal income and expense transactions.
 * Types: "PENGELUARAN" (Expense), "PEMASUKAN" (Income), "TRANSFER" (Internal transfer between accounts)
 */
@Entity(tableName = "personal_expenses")
data class PersonalExpenseEntity(
    @PrimaryKey
    val id: String,
    val jenis: String,                 // "PENGELUARAN", "PEMASUKAN", "TRANSFER"
    val kategori: String,              // "Makan & Minum", "Bensin & Transport", "Belanja Harian", "Tagihan & Listrik", "Keluarga", "Gaji / Komisi", "Lainnya"
    val nominal: Double,
    val tanggal: String,               // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val accountId: String,             // ID of source PersonalAccountEntity
    val toAccountId: String? = null,   // Target account if jenis == "TRANSFER"
    val judul: String,                 // Short description e.g. "Bensin motor keliling"
    val catatan: String = "",
    val fotoNotaUri: String = ""       // Optional photo of receipt
)
