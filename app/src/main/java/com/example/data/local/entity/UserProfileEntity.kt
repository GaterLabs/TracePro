package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "PRIMARY_PROFILE",
    val namaSalesman: String = "",
    val noHp: String = "",
    val namaDistributor: String = "",
    val alamatDepo: String = "",
    val platNomorMobil: String = "",
    val areaOperasional: String = "",
    val pinKeamanan: String = "",
    val isConfigured: Boolean = false,
    val appLanguage: String = "ID", // "ID" or "EN"
    val createdAt: Long = System.currentTimeMillis()
)
