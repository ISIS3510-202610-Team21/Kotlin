package com.example.spendantt.data.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.LabelRepository
import com.example.spendantt.data.repository.PineconeRepository
import kotlinx.coroutines.flow.first

class AutoCategorizationService(
    private val context: Context,
    private val userId: Int
) {
    private val db = AppDatabase.getInstance(context)
    private val expenseRepository = ExpenseRepository(db.expenseDao(), db.labelDao())
    private val labelRepository = LabelRepository(db.labelDao())
    private val pineconeRepository = PineconeRepository()

    private var isSeeded = false

    suspend fun categorizeExpense(
        expenseId: Int,
        expenseName: String,
        firebaseUid: String? = null
    ): Boolean {
        if (!hasInternet()) return false

        ensurePineconeSeeded()

        val labelName = pineconeRepository.findLabelForExpense(expenseName) ?: return false

        val labels = labelRepository.getLabelsByUser(userId).first()
        val matchedLabel = labels.firstOrNull {
            it.name.lowercase() == labelName.lowercase()
        } ?: return false

        expenseRepository.categorizeExpense(expenseId, matchedLabel.id, firebaseUid)
        return true
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
        }
    }

    private fun hasInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}