package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "warung_custom_prices",
    indices = [Index(value = ["warungId", "productId"], unique = true)]
)
data class WarungCustomPriceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val warungId: String,
    val productId: String,
    val hargaJualPcs: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
