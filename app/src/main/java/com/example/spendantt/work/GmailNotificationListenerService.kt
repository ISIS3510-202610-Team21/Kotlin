package com.example.spendantt.work

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.dao.UserDao
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseSource
import com.example.spendantt.data.notifications.BoldTransactionParser
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.NotificationRepository
import com.example.spendantt.data.service.AutoCategorizationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Servicio que escucha notificaciones del sistema para detectar
 * transacciones de Gmail (Bold) y crear expenses automáticamente.
 */
class GmailNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var prefs: SharedPreferences
    private lateinit var userDao: UserDao

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(applicationContext)
        expenseRepository = ExpenseRepository(database.expenseDao(), database.labelDao())
        notificationRepository = NotificationRepository(applicationContext)
        prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        userDao = database.userDao()
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

                val firebaseUid = getFirebaseUid(userId)

                // Crear el expense primero (sin categoría)
                val result = expenseRepository.insertExpense(
                    expense = ExpenseEntity(
                        userId = userId,
                        name = parsed.merchantName,
                        amount = parsed.amount,
                        date = parsed.date,
                        time = parsed.time,
                        locationName = parsed.location,
                        source = ExpenseSource.BOLD,
                        isPendingCategory = true  // Marcado como pendiente hasta categorizar
                    ),
                    labelIds = emptyList(),
                    firebaseUid = firebaseUid
                )

                result.fold(
                    onSuccess = { newExpenseId ->
                        val expenseId = newExpenseId.toInt()

                        // Marcar como procesada antes de categorizar
                        markAsProcessed(notificationKey)

                        // Intentar auto-categorización con Pinecone
                        val autoCategorizationService = AutoCategorizationService(applicationContext, userId)
                        val categorized = autoCategorizationService.categorizeExpense(
                            expenseId = expenseId,
                            expenseName = parsed.merchantName,
                            firebaseUid = firebaseUid
                        )

                        val internalNotificationId = "bold_${parsed.transactionId ?: System.currentTimeMillis()}"

                        if (categorized) {
                            // Categorizado exitosamente: notificación informativa normal
                            notificationRepository.upsertNotification(
                                userId = userId,
                                notificationId = "bold_imported_$internalNotificationId",
                                type = "bold_imported",
                                title = "Bold transaction imported",
                                body = "${parsed.merchantName} for COP ${parsed.amount.toInt()} was added and categorized automatically."
                            )
                        } else {
                            // Sin categoría: notificación con deep link a la pantalla de edición
                            notificationRepository.upsertNotificationWithExpenseDeepLink(
                                userId = userId,
                                notificationId = "bold_imported_$internalNotificationId",
                                type = "bold_needs_category",
                                title = "Tap to categorize your expense",
                                body = "${parsed.merchantName} for COP ${parsed.amount.toInt()} was added. Tap to assign a label.",
                                expenseId = expenseId
                            )
                        }

                        Log.d(TAG, "Successfully created expense from Bold notification: ${parsed.merchantName} - ${parsed.amount}, Categorized: $categorized")
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Failed to create expense from Bold notification", e)
                    }
                )

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
     * Obtiene el Firebase UID del usuario desde Room DB (igual que el registro manual),
     * con Firebase Auth como fallback.
     */
    private suspend fun getFirebaseUid(userId: Int): String? {
        return try {
            userDao.getUserById(userId)?.firebaseUid
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            } catch (_: Exception) {
                null
            }
        }
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
