package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 4 Laci Virtual Inventory:
 * 1. stokFreshPabrik: Milik Pabrik (Fresh Morning Loading)
 * 2. stokBsBelumSortir: Milik Pribadi (BS tarikan warung, belum dipilah)
 * 3. stokPribadiLayakJual: Modal Pribadi Siap Edar (100% Profit Murni)
 * 4. stokPribadiRusak: Kerugian Pribadi (Write-off)
 */
@Entity(
    tableName = "inventory_drawers",
    indices = [
        Index(value = ["productId"], unique = true)
    ]
)
data class InventoryDrawerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val stokFreshPabrikPcs: Int = 0,
    val stokBsBelumSortirPcs: Int = 0,
    val stokPribadiLayakJualPcs: Int = 0,
    val stokPribadiRusakPcs: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
