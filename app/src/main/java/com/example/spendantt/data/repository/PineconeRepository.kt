package com.example.spendantt.data.repository

import com.example.spendantt.BuildConfig
import com.example.spendantt.data.local.entity.LabelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * PineconeRepository
 *
 * Maneja la base de datos vectorial para categorización automática de gastos.
 *
 * Flujo:
 * 1. Al primer registro → seedDefaultLabels() puebla Pinecone con las labels base
 * 2. Al guardar gasto → findLabelForExpense() busca label por nombre
 * 3. Si encuentra → asigna automáticamente
 * 4. Si no → isPendingCategory = true → usuario asigna manual
 * 5. Al asignar manual → saveExpenseLabel() aprende para el futuro
 *
 * API Key: BuildConfig.PINECONE_API_KEY (definida en local.properties)
 */
class PineconeRepository {

    private val apiKey = BuildConfig.PINECONE_API_KEY

    // ── Reemplaza con tu Host URL de Pinecone Console ─────────
    // Pinecone Console → índice "spendant" → copiar Host URL
    private val indexHost = "https://spendant-e0isilf.svc.aped-4627-b74a.pinecone.io"

    private val threshold = 0.75f

    // ── SEED: poblar Pinecone con labels por defecto ──────────
    /**
     * Construye todos los vectores y los envía en lotes de 10
     * para evitar el error STRING_TOO_LARGE de Android JSON.
     * Un vector de 384 dimensiones × 100 vectores = String gigante que explota.
     * Con lotes de 10 cada request es pequeño y manejable.
     */
    // MULTI-THREADING | Nano | 10pts (soporte) | withContext(Dispatchers.IO) en cada función suspend: el caller (ViewModel/Service en Main) puede invocar estas funciones suspend seguro; el trabajo HTTP/red se ejecuta en el pool I/O sin bloquear Main
    suspend fun seedDefaultLabels(labels: List<LabelEntity>) {
        withContext(Dispatchers.IO) {
            try {
                val allVectors = mutableListOf<JSONObject>()

                labels.forEach { label ->
                    allVectors.add(buildVector(
                        id = "label_${label.name.lowercase().replace(" ", "_")}",
                        text = label.name,
                        labelName = label.name,
                        category = label.category ?: "Other"
                    ))
                    val variants = getSemanticVariants(label.name)
                    variants.forEachIndexed { i, variant ->
                        allVectors.add(buildVector(
                            id = "label_${label.name.lowercase().replace(" ", "_")}_v$i",
                            text = variant,
                            labelName = label.name,
                            category = label.category ?: "Other"
                        ))
                    }
                }

                // Enviar en lotes de 10 para no superar el límite de tamaño
                allVectors.chunked(10).forEach { batch ->
                    val batchArray = JSONArray()
                    batch.forEach { batchArray.put(it) }
                    upsertVectors(batchArray)
                }

            } catch (e: Exception) {
                // Fallo silencioso
            }
        }
    }

    // ── BUSCAR label para un gasto ────────────────────────────
    suspend fun findLabelForExpense(expenseName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val embedding = generateSimpleEmbedding(expenseName)


                val url = URL("$indexHost/query")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Api-Key", apiKey)
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                val body = JSONObject().apply {
                    put("vector", JSONArray(embedding.toList()))
                    put("topK", 1)
                    put("includeMetadata", true)
                    put("namespace", "")
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(body.toString())
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val error = connection.errorStream?.bufferedReader()?.readText()
                    return@withContext null
                }

                val responseText = connection.inputStream.bufferedReader().readText()
                val response = JSONObject(responseText)

                //val response = JSONObject(connection.inputStream.bufferedReader().readText())
                val matches = response.optJSONArray("matches") ?: return@withContext null
                if (matches.length() == 0) return@withContext null

                val topMatch = matches.getJSONObject(0)
                val score = topMatch.optDouble("score", 0.0).toFloat()


                if (score >= threshold) {
                    topMatch.optJSONObject("metadata")?.optString("label")
                } else null

            } catch (e: Exception) {
                null
            }
        }
    }

    // ── GUARDAR nuevo vector cuando usuario asigna manual ─────
    suspend fun saveExpenseLabel(expenseName: String, labelName: String, labelCategory: String) {
        withContext(Dispatchers.IO) {
            try {
                val vectors = JSONArray()
                vectors.put(buildVector(
                    id = "expense_${expenseName.lowercase().replace(" ", "_")}_${System.currentTimeMillis()}",
                    text = expenseName,
                    labelName = labelName,
                    category = labelCategory
                ))
                upsertVectors(vectors)
            } catch (e: Exception) {
                // Fallo silencioso
            }
        }
    }

    // ── HELPERS ───────────────────────────────────────────────
    private fun buildVector(id: String, text: String, labelName: String, category: String): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("values", JSONArray(generateSimpleEmbedding(text).toList()))
            put("metadata", JSONObject().apply {
                put("text", text)
                put("label", labelName)
                put("category", category)
            })
        }
    }

    private fun upsertVectors(vectors: JSONArray) {
        val url = URL("$indexHost/vectors/upsert")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Api-Key", apiKey)
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        val body = JSONObject().apply { put("vectors", vectors) }
        OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
        connection.responseCode
    }

    /**
     * Variantes semánticas por label para mejorar el matching.
     * Ej: "Food" → ["restaurante", "almuerzo", "comida", "mercado"]
     */
    private fun getSemanticVariants(labelName: String): List<String> {
        return when (labelName.lowercase()) {
            "food" -> listOf("restaurante", "almuerzo", "comida", "lunch", "dinner", "breakfast", "cafe", "hamburgesa", "pizza")
            "transport" -> listOf("uber", "taxi", "bus", "metro", "transporte", "gasolina", "peaje", "transmilenio")
            "groceries" -> listOf("supermercado", "mercado", "exito", "jumbo", "d1", "ara", "carulla", "tienda")
            "food delivery" -> listOf("rappi", "ifood", "domicilio", "delivery", "pedido")
            "entertainment" -> listOf("cine", "netflix", "spotify", "pelicula", "concierto", "teatro")
            "subscriptions" -> listOf("suscripcion", "mensualidad", "plan", "prime", "youtube")
            "university fees" -> listOf("matricula", "universidad", "tuition", "semestre", "inscripcion")
            "learning materials" -> listOf("libro", "cuaderno", "lapiz", "papeleria", "fotocopias", "impresion")
            "commute" -> listOf("transmilenio", "sitp", "bus universitario", "ida", "vuelta")
            "rent" -> listOf("arriendo", "alquiler", "canon", "habitacion")
            "utilities" -> listOf("agua", "luz", "gas", "internet", "telefono", "epm", "codensa")
            "services" -> listOf("servicio", "reparacion", "plomero", "electricista")
            "personal care" -> listOf("peluqueria", "barberia", "farmacia", "drogueria", "medicamento")
            "gifts" -> listOf("regalo", "presente", "cumpleaños", "detalle")
            "group hangouts" -> listOf("salida", "rumba", "bar", "fiesta", "reunion")
            "owed" -> listOf("deuda", "prestamo", "pago", "debo", "debi")
            "impulse" -> listOf("antojo", "capricho", "impulso")
            "emergency" -> listOf("emergencia", "urgencia", "accidente")
            else -> emptyList()
        }
    }

    /**
     * Genera embedding simple basado en caracteres.
     * Fase 2: reemplazar con modelo de embeddings real.
     */
    private fun generateSimpleEmbedding(text: String): FloatArray {
        val dimension = 384
        val embedding = FloatArray(dimension)
        val normalized = text.lowercase().trim()

        normalized.forEachIndexed { i, char ->
            val idx = (char.code * (i + 1)) % dimension
            embedding[idx] += char.code.toFloat() / 128f
        }

        val magnitude = Math.sqrt(embedding.map { it * it }.sum().toDouble()).toFloat()
        if (magnitude > 0) {
            for (i in embedding.indices) embedding[i] /= magnitude
        }

        return embedding
    }
}