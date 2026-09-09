package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_rates", primaryKeys = ["baseCode", "targetCode"])
data class CachedRateEntity(
    val baseCode: String,
    val targetCode: String,
    val rate: Double,
    val lastUpdatedUtc: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pinned_currencies")
data class PinnedCurrencyEntity(
    @PrimaryKey val code: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)
