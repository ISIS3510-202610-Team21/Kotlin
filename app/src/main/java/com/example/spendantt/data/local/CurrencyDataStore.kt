package com.example.spendantt.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.currencyStore by preferencesDataStore(name = "currency_prefs")

object CurrencyDataStore {
    private val ACTIVE_CURRENCY = stringPreferencesKey("active_currency")

    suspend fun saveActiveCurrency(context: Context, iso: String) {
        context.currencyStore.edit { prefs ->
            prefs[ACTIVE_CURRENCY] = iso
        }
    }

    suspend fun loadActiveCurrency(context: Context): String? {
        return context.currencyStore.data.first()[ACTIVE_CURRENCY]
    }
}
