package com.example.spendantt.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseSource
import com.example.spendantt.data.notifications.BoldTransactionParser
import com.example.spendantt.data.notifications.PaymentNotificationParser
import com.example.spendantt.data.repository.AppNotification
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.LabelRepository
import com.example.spendantt.data.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsViewModel(
    context: Context,
    private val userId: Int
) : ViewModel() {
    private val database = AppDatabase.getInstance(context)
    private val repository = NotificationRepository(context)
    private val expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
    private val labelRepository = LabelRepository(database.labelDao())

    private val _notifications = mutableStateOf<List<AppNotification>>(emptyList())
    val notifications: State<List<AppNotification>> = _notifications

    init {
        refresh()
    }

    fun refresh() {
        _notifications.value = repository.getNotifications(userId)
    }

    fun markAllAsRead() {
        repository.markAllAsRead(userId)
        refresh()
    }

    fun simulateGooglePayExpense() {
        viewModelScope.launch {
            val notificationId = "google_pay_sim_${System.currentTimeMillis()}"
            val title = "Compra aprobada por \$51.600"
            val body = "Tu compra en CAFE QUINDIO EXPRESS TITAN PLAZA por \$51.600,00 con tu tarjeta terminada en 3141 ha sido APROBADA."

            repository.postSimulationNotification(
                notificationId = notificationId,
                title = title,
                body = body
            )

            val parsed = PaymentNotificationParser.parse(title, body) ?: return@launch
            val labels = labelRepository.getLabelsByUser(userId).first()
            val foodLabelId = labels.firstOrNull { it.name.equals("Food", ignoreCase = true) }?.id
            val now = System.currentTimeMillis()
            val timeText = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date(now))

            expenseRepository.insertExpense(
                expense = ExpenseEntity(
                    userId = userId,
                    name = parsed.merchantName,
                    amount = parsed.amount,
                    date = now,
                    time = timeText,
                    locationName = parsed.merchantName,
                    source = ExpenseSource.GOOGLE_PAY
                ),
                labelIds = listOfNotNull(foodLabelId)
            )

            repository.upsertNotification(
                userId = userId,
                notificationId = "google_pay_imported_$notificationId",
                type = "google_pay_imported",
                title = "Payment detected",
                body = "${parsed.merchantName} for COP ${parsed.amount.toInt()} was added as an expense."
            )
            refresh()
        }
    }

    /**
     * Procesa un email de transacción Bold y crea un expense automáticamente
     * @param emailContent El contenido completo del email de Bold
     */
    fun processBoldTransaction(emailContent: String) {
        viewModelScope.launch {
            val parsed = BoldTransactionParser.parse(emailContent) ?: return@launch

            val notificationId = "bold_${parsed.transactionId ?: System.currentTimeMillis()}"

            // Buscar etiqueta apropiada basada en el nombre del comercio
            val labels = labelRepository.getLabelsByUser(userId).first()
            val labelId = findMatchingLabel(parsed.merchantName, labels)

            expenseRepository.insertExpense(
                expense = ExpenseEntity(
                    userId = userId,
                    name = parsed.merchantName,
                    amount = parsed.amount,
                    date = parsed.date,
                    time = parsed.time,
                    locationName = parsed.location,
                    source = ExpenseSource.BOLD
                ),
                labelIds = listOfNotNull(labelId)
            )

            repository.upsertNotification(
                userId = userId,
                notificationId = "bold_imported_$notificationId",
                type = "bold_imported",
                title = "Bold transaction imported",
                body = "${parsed.merchantName} for COP ${parsed.amount.toInt()} was added as an expense."
            )
            refresh()
        }
    }

    /**
     * Simula una transacción Bold con datos de ejemplo
     */
    fun simulateBoldExpense() {
        val sampleBoldEmail = """
            mí
            Compra en Rose Cafe por 1000
            >> Pagos
            >> Transacción aprobada
            >> COP $1.000
            >> Rose Cafe
            >> 2026/03/27 17:43:42
            >> Calle 38A sur # 34B - 08, Bogotá D.C.
            >> 3214776858
            >> andromedadecolombia@gmail.com
            >> MID
            >> 50267756
            >> Terminal
            >> ON0CZ862
            >> ID Transacción Bold	CPRY4OV3JQBY
            >> Código de autorización	445665
            >> AID	A0000000041010
            >> App label	Mastercard
            >> Metodo de cobro	Datáfono
            >> Medio de pago	Mastercard ***1719
            >> Cuotas	1
            >> Cuenta	Crédito
            >> Subtotal	$1.000
            >> Propina	0
            >> Total COP	$1.000
            >> Pago sin contacto
        """.trimIndent()

        processBoldTransaction(sampleBoldEmail)
    }

    /**
     * Busca una etiqueta que coincida con el nombre del comercio
     */
    private fun findMatchingLabel(merchantName: String, labels: List<com.example.spendantt.data.local.entity.LabelEntity>): Int? {
        val lowerMerchant = merchantName.lowercase()
        
        // Palabras clave para categorías comunes
        val foodKeywords = listOf("cafe", "restaurant", "pizza", "burger", "food", "comida", "cafetería", "panadería", "bakery")
        val transportKeywords = listOf("uber", "didi", "taxi", "bus", "transporte", "gasolina", "gas")
        val shoppingKeywords = listOf("tienda", "store", "shop", "mall", "centro comercial", "supermercado", "market")
        
        return when {
            foodKeywords.any { lowerMerchant.contains(it) } -> 
                labels.firstOrNull { it.name.equals("Food", ignoreCase = true) }?.id
            transportKeywords.any { lowerMerchant.contains(it) } -> 
                labels.firstOrNull { it.name.equals("Transport", ignoreCase = true) }?.id
            shoppingKeywords.any { lowerMerchant.contains(it) } -> 
                labels.firstOrNull { it.name.equals("Shopping", ignoreCase = true) }?.id
            else -> null
        }
    }
}
