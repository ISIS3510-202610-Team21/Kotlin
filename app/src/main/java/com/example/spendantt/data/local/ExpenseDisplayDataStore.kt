package com.example.spendantt.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// LOCAL STORAGE | William | 5pts | BD Llave/Valor: DataStore<Preferences> persiste preferencias de visualización de gastos (orden y filtro de pendientes) como pares clave-valor tipados; a diferencia de SharedPreferences, las lecturas son reactivas (Flow) y las escrituras son atómicas y crash-safe — equivalente Android de Hive/RealmDB
private val Context.expenseDisplayStore: DataStore<Preferences> by preferencesDataStore(
    name = "expense_display_prefs"
)

object ExpenseDisplayDataStore {

    private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")
    private val SHOW_PENDING_ONLY_KEY = booleanPreferencesKey("show_pending_only")

    fun sortOrderFlow(context: Context): Flow<String> =
        context.expenseDisplayStore.data.map { prefs ->
            prefs[SORT_ORDER_KEY] ?: SortOrder.NEWEST.key
        }

    fun showPendingOnlyFlow(context: Context): Flow<Boolean> =
        context.expenseDisplayStore.data.map { prefs ->
            prefs[SHOW_PENDING_ONLY_KEY] ?: false
        }

    suspend fun setSortOrder(context: Context, order: SortOrder) {
        context.expenseDisplayStore.edit { prefs ->
            prefs[SORT_ORDER_KEY] = order.key
        }
    }

    suspend fun setShowPendingOnly(context: Context, enabled: Boolean) {
        context.expenseDisplayStore.edit { prefs ->
            prefs[SHOW_PENDING_ONLY_KEY] = enabled
        }
    }

    enum class SortOrder(val key: String) {
        NEWEST("newest"),
        OLDEST("oldest"),
        HIGHEST("highest"),
        LOWEST("lowest")
    }
}
