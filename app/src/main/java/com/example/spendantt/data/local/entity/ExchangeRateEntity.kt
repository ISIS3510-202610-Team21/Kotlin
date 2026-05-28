package com.example.spendantt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Santiago Gomez | Local Storage | 10 pts
// Room entity used by the local relational database to persist exchange rates and their fetch time.
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currency: String,
    val rate: Double,
    val fetchedAt: Long
)
