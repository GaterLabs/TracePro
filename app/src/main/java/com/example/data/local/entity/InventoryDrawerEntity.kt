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
    val stokPoolGudangPcs: Int = 0, // Kantung Jatah Mingguan / Pool Rumah (Aset Bos/Pabrik yang disimpan di rumah)
    val stokFreshPabrikPcs: Int = 0, // Tas Motor / Kendaraan Harian (Muat Harian Siap Edar)
    val stokBsBelumSortirPcs: Int = 0,
    val stokPribadiLayakJualPcs: Int = 0, // Hasil Tarik Warung Masih Renyah / Siap Rolling
    val stokPribadiRusakPcs: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
