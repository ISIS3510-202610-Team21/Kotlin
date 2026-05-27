package com.example.spendantt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "report_cache")
data class ReportCacheEntity(
    @PrimaryKey
    val cacheKey: String,         // "{userId}_{startEpochDay}_{endEpochDay}"
    val userId: Int,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val reportJson: String,
    val generatedAt: Long,        // Unix millis
)
