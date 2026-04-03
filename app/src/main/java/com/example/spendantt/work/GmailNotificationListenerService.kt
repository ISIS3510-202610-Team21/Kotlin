package com.example.spendantt.work

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseSource
import com.example.spendantt.data.notifications.BoldTransactionParser
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.LabelRepository
import com.example.spendantt.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Servicio que escucha notificaciones del sistema para detectar
 * transacciones de Gmail (Bold) y crear expenses automáticamente.
 */
class GmailNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var labelRepository: LabelRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(applicationContext)
        expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
        labelRepository = LabelRepository(database.labelDao())
        notificationRepository = NotificationRepository(applicationContext)
        prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "GmailNotificationListenerService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        
        Log.d(TAG, "Notification received from: $packageName")
        
        // Solo procesar notificaciones de Gmail
        if (packageName != GMAIL_PACKAGE) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extraer título y contenido de la notificación
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

        // Usar bigText si está disponible, si no usar text
        val content = when {
            bigText.isNotEmpty() -> bigText
            text.isNotEmpty() -> text
            else -> ""
        }

        Log.d(TAG, "Gmail notification - Title: '$title'")
        Log.d(TAG, "Gmail notification - Content preview: '${content.take(100)}...'")

        // Verificar si parece una transacción Bold
        if (!isBoldTransaction(title, content)) {
            Log.d(TAG, "Not a Bold transaction, ignoring")
            return
        }

        // Evitar procesar la misma notificación dos veces
        val notificationKey = sbn.key ?: "${sbn.id}_${sbn.postTime}"
        if (wasAlreadyProcessed(notificationKey)) {
            Log.d(TAG, "Notification already processed: $notificationKey")
            return
        }

        // Combinar título y contenido para el parser
        val fullContent = "$title\n$content"
        
        Log.d(TAG, "Processing Bold transaction from notification")
        processBoldNotification(fullContent, notificationKey)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No necesitamos hacer nada cuando se elimina la notificación
    }

    /**
     * Verifica si la notificación parece ser una transacción Bold/pagos
     */
    private fun isBoldTransaction(title: String, content: String): Boolean {
        val combined = "$title $content".lowercase()
        // Debe contener "compra" Y "bold" (o indicadores de transacción)
        val hasCompra = combined.contains("compra")
        val hasBoldOrPayment = combined.contains("bold") || 
                               combined.contains("transacci") ||
                               combined.contains("pagos")
        val hasAmount = combined.contains("cop") || combined.contains("$")
        
        return hasCompra && (hasBoldOrPayment || hasAmount)
    }

    /**
     * Procesa una notificación de Bold y crea el expense
     */
    private fun processBoldNotification(content: String, notificationKey: String) {
        serviceScope.launch {
            try {
                val parsed = BoldTransactionParser.parse(content)
                if (parsed == null) {
                    Log.w(TAG, "Could not parse Bold transaction from notification")
                    return@launch
                }

                val userId = getCurrentUserId()
                if (userId == null) {
                    Log.w(TAG, "No logged in user, cannot create expense")
                    return@launch
                }

                // Buscar etiqueta apropiada
                val labels = labelRepository.getLabelsByUser(userId).first()
                val labelId = findMatchingLabel(parsed.merchantName, labels)

                // Crear el expense
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

                // Marcar como procesada
                markAsProcessed(notificationKey)

                // Crear notificación interna
                val internalNotificationId = "bold_${parsed.transactionId ?: System.currentTimeMillis()}"
                notificationRepository.upsertNotification(
                    userId = userId,
                    notificationId = "bold_imported_$internalNotificationId",
                    type = "bold_imported",
                    title = "Bold transaction imported",
                    body = "${parsed.merchantName} for COP ${parsed.amount.toInt()} was added as an expense."
                )

                Log.d(TAG, "Successfully created expense from Bold notification: ${parsed.merchantName} - ${parsed.amount}")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing Bold notification", e)
            }
        }
    }

    /**
     * Obtiene el ID del usuario actualmente logueado
     */
    private fun getCurrentUserId(): Int? {
        val userPrefs = applicationContext.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        val userId = userPrefs.getInt(KEY_LAST_USER_ID, -1)
        return if (userId != -1) userId else null
    }

    /**
     * Verifica si una notificación ya fue procesada
     */
    private fun wasAlreadyProcessed(notificationKey: String): Boolean {
        val processedKeys = prefs.getStringSet(PROCESSED_NOTIFICATIONS_KEY, emptySet()) ?: emptySet()
        return notificationKey in processedKeys
    }

    /**
     * Marca una notificación como procesada
     */
    private fun markAsProcessed(notificationKey: String) {
        val processedKeys = prefs.getStringSet(PROCESSED_NOTIFICATIONS_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        processedKeys.add(notificationKey)
        
        // Mantener solo las últimas 100 para no crecer indefinidamente
        val limitedKeys = if (processedKeys.size > 100) {
            processedKeys.toList().takeLast(100).toSet()
        } else {
            processedKeys
        }
        
        prefs.edit().putStringSet(PROCESSED_NOTIFICATIONS_KEY, limitedKeys).apply()
    }

    /**
     * Busca una etiqueta que coincida con el nombre del comercio
     */
    private fun findMatchingLabel(
        merchantName: String,
        labels: List<com.example.spendantt.data.local.entity.LabelEntity>
    ): Int? {
        val lowerMerchant = merchantName.lowercase()

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

    companion object {
        private const val TAG = "GmailNotifListener"
        private const val GMAIL_PACKAGE = "com.google.android.gm"
        private const val PREFS_NAME = "gmail_notification_listener_prefs"
        private const val PROCESSED_NOTIFICATIONS_KEY = "processed_notifications"
        private const val AUTH_PREFS_NAME = "auth_prefs"
        private const val KEY_LAST_USER_ID = "last_user_id"

        /**
         * Verifica si el servicio tiene permiso de acceso a notificaciones
         */
        fun isNotificationListenerEnabled(context: Context): Boolean {
            val componentName = ComponentName(context, GmailNotificationListenerService::class.java)
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(componentName.flattenToString()) == true
        }

        /**
         * Abre la configuración para habilitar el acceso a notificaciones
         */
        fun openNotificationListenerSettings(context: Context) {
            val intent = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
