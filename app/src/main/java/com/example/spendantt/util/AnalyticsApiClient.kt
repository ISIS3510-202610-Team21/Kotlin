package com.example.spendantt.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AnalyticsApiClient {

    // 10.0.2.2 = localhost desde el emulador de Android.
    // Si usas dispositivo físico en la misma red, cambia esto por la IP de tu PC.
    private const val BASE_URL = "http://10.0.2.2:8000"
    private const val TIMEOUT_MS = 3_000

    suspend fun post(endpoint: String, body: Map<String, Any?>) {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL("$BASE_URL$endpoint").openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }
                val json = JSONObject(body.filterValues { it != null }).toString()
                connection.outputStream.bufferedWriter().use { it.write(json) }
                connection.responseCode
                connection.disconnect()
            } catch (_: Exception) {
                // Analytics nunca debe crashear la app
            }
        }
    }
}
