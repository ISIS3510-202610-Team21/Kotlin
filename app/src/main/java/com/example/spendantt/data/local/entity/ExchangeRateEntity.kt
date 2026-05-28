package com.example.spendantt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Santiago Gomez | Local Storage
// Room entity that stores downloaded exchange rates locally with their fetch timestamp.
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currency: String,
    val rate: Double,
    val fetchedAt: Long
)
