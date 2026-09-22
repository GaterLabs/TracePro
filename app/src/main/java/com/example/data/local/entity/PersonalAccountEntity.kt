package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for personal financial accounts:
 * e.g., Cash (Uang Tunai), Bank (BCA, Mandiri, BRI), E-Wallet (GoPay, OVO, Dana),
 * and Paylater / Kartu Kredit facilities with credit limits.
 */
@Entity(tableName = "personal_accounts")
data class PersonalAccountEntity(
    @PrimaryKey
    val id: String,
    val namaAkun: String,              // e.g. "Dompet Tunai", "BCA", "GoPay", "Shopee Paylater"
    val tipeAkun: String,              // "CASH", "BANK", "EWALLET", "PAYLATER", "LAINNYA"
    val saldo: Double = 0.0,           // Current available balance (positive) or used credit
    val nomorRekening: String = "",    // Optional: Account / Card number
    val catatan: String = "",
    val isPaylater: Boolean = false,   // True if this is a paylater / credit facility
    val limitKredit: Double = 0.0,     // Total approved limit if paylater
    val tanggalJatuhTempo: Int = 0,    // Day of month (e.g. 25th of month)
    val warnaHex: String = "#0F172A",  // Slate theme color for card
    val updatedAt: Long = System.currentTimeMillis()
)
