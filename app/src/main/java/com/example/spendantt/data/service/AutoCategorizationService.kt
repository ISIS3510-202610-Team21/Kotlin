package com.example.spendantt.data.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.LruCache
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.LabelRepository
import com.example.spendantt.data.repository.PineconeRepository
import com.example.spendantt.util.AnalyticsHelper
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutoCategorizationService(
    private val context: Context,
    private val userId: Int
) {
    private val db = AppDatabase.getInstance(context)
    private val expenseRepository = ExpenseRepository(db.expenseDao(), db.labelDao())
    private val labelRepository = LabelRepository(db.labelDao())
    private val pineconeRepository = PineconeRepository()
    private val autoCategorizationFallbackLogFile =
        File(context.filesDir, AUTO_CATEGORIZATION_FALLBACK_LOG_FILE_NAME)

    private var isSeeded = false

    suspend fun categorizeExpense(
        expenseId: Int,
        expenseName: String,
        firebaseUid: String? = null
    ): Boolean {
        val online = hasInternet()
        val strategy = if (online) {
            OnlinePineconeCategorizationStrategy()
        } else {
            OfflineCategorizationStrategy()
        }

        val categorized = strategy.categorizeExpense(
            expenseId = expenseId,
            expenseName = expenseName,
            firebaseUid = firebaseUid
        )

        if (!categorized) {
            appendAutoCategorizationFallbackLog(
                expenseId = expenseId,
                expenseName = expenseName,
                attemptedOnline = online
            )
        }

        return categorized
    }

    suspend fun assignLabelManually(
        expenseId: Int,
        labelId: Int,
        expenseName: String,
        firebaseUid: String? = null
    ) {
        expenseRepository.categorizeExpense(expenseId, labelId, firebaseUid)

        if (hasInternet()) {
            val label = labelRepository.getLabelById(labelId)
            if (label != null) {
                pineconeRepository.saveExpenseLabel(
                    expenseName = expenseName,
                    labelName = label.name,
                    labelCategory = label.category ?: "Other"
                )
            }
        }
    }

    private suspend fun ensurePineconeSeeded() {
        if (isSeeded) return
        android.util.Log.d("PINECONE", "Haciendo seed...")
        try {
            val labels = labelRepository.getLabelsByUser(userId).first()
            android.util.Log.d("PINECONE", "Labels a subir: ${labels.size}")
            if (labels.isNotEmpty()) {
                pineconeRepository.seedDefaultLabels(labels)
                isSeeded = true
                android.util.Log.d("PINECONE", "Seed completado")
            }
        } catch (e: Exception) {
            android.util.Log.d("PINECONE", "Error seed: ${e.message}")
            AnalyticsHelper.logModuleCrash(context, "AutoCategorizationService", e.message ?: "unknown")

        }
    }

    private fun hasInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // LOCAL STORAGE | Nano | 5pts | Archivos Locales: guarda en autocategorization_fallbacks.txt cada gasto que no pudo autocategorizarse y pasó a etiquetado manual
    private fun appendAutoCategorizationFallbackLog(
        expenseId: Int,
        expenseName: String,
        attemptedOnline: Boolean
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = buildString {
                append("[$timestamp] Auto-categorization fallback")
                append("\nuserId=")
                append(userId)
                append("\nexpenseId=")
                append(expenseId)
                append("\nexpenseName=")
                append(expenseName)
                append("\nmode=")
                append(if (attemptedOnline) "online" else "offline")
                append("\nresult=manual_label_required")
                append("\n\n")
            }
            autoCategorizationFallbackLogFile.appendText(entry)
        } catch (_: Exception) {
            // El registro local no debe romper el flujo principal.
        }
    }

    private interface AutoCategorizationStrategy {
        suspend fun categorizeExpense(
            expenseId: Int,
            expenseName: String,
            firebaseUid: String?
        ): Boolean
    }

    private inner class OnlinePineconeCategorizationStrategy : AutoCategorizationStrategy {
        override suspend fun categorizeExpense(
            expenseId: Int,
            expenseName: String,
            firebaseUid: String?
        ): Boolean {
            val cacheKey = expenseName.lowercase().trim()

            // CACHING | William | 10pts | LruCache: caché de resultados Pinecone indexado por nombre
            // de comerciante normalizado (lowercase + trim). maxSize=50 cubre la variedad típica de
            // comercios de un usuario sin exceder memoria. Compartido en companion object para persistir
            // entre instancias de AutoCategorizationService (cada notificación Bold crea una nueva).
            // Evita llamadas redundantes a la API cuando el mismo comercio aparece en varias
            // notificaciones consecutivas (ej: compras recurrentes en el mismo establecimiento).
            val cachedLabelName = merchantLabelCache.get(cacheKey)
            if (cachedLabelName != null) {
                val labels = labelRepository.getLabelsByUser(userId).first()
                val matchedLabel = labels.firstOrNull {
                    it.name.lowercase() == cachedLabelName.lowercase()
                } ?: return false
                expenseRepository.categorizeExpense(expenseId, matchedLabel.id, firebaseUid)
                return true
            }

            ensurePineconeSeeded()

            val labelName = pineconeRepository.findLabelForExpense(expenseName) ?: return false

            val labels = labelRepository.getLabelsByUser(userId).first()
            val matchedLabel = labels.firstOrNull {
                it.name.lowercase() == labelName.lowercase()
            } ?: return false

            merchantLabelCache.put(cacheKey, labelName)
            expenseRepository.categorizeExpense(expenseId, matchedLabel.id, firebaseUid)
            return true
        }
    }

    private class OfflineCategorizationStrategy : AutoCategorizationStrategy {
        override suspend fun categorizeExpense(
            expenseId: Int,
            expenseName: String,
            firebaseUid: String?
        ): Boolean {
            return false
        }
    }

    companion object {
        private const val AUTO_CATEGORIZATION_FALLBACK_LOG_FILE_NAME =
            "autocategorization_fallbacks.txt"

        private val merchantLabelCache = LruCache<String, String>(50)
    }
}
