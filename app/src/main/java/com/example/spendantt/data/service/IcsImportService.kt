package com.example.spendantt.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ImportedCalendarEvent(
    val title: String,
    val startAt: Long?,
    val endAt: Long?,
    val location: String?
)

// LOCAL STORAGE | William | 5pts | Archivos Locales: lee archivo .ics del filesystem del dispositivo vía ContentResolver + BufferedReader/InputStreamReader; parsea VCALENDAR y persiste eventos en SharedPreferences
class IcsImportService(
    context: Context
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun importFromUri(userId: Int, uri: Uri): Result<Int> {
        return try {
            Log.d(TAG, "import start userId=$userId uri=$uri")

            if (!isValidIcsUri(uri)) {
                Log.d(TAG, "import invalid_extension userId=$userId uri=$uri")
                return Result.failure(Exception("Please select a valid .ics file."))
            }

            val rawContent = appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: run {
                Log.d(TAG, "import unreadable_file userId=$userId uri=$uri")
                return Result.failure(Exception("We couldn't read the selected file."))
            }

            if (!rawContent.contains("BEGIN:VCALENDAR", ignoreCase = true)) {
                Log.d(TAG, "import invalid_calendar_format userId=$userId uri=$uri")
                return Result.failure(Exception("The selected file is not a valid .ics calendar."))
            }

            val events = parseEvents(rawContent)
            if (events.isEmpty()) {
                Log.d(TAG, "import no_events_found userId=$userId uri=$uri")
                return Result.failure(Exception("We couldn't find any calendar events in that file."))
            }

            Log.d(
                TAG,
                "import parsed_successfully userId=$userId events=${events.size} firstTitle=${events.firstOrNull()?.title.orEmpty()}"
            )

            val serializedEvents = JSONArray()
            events.forEach { event ->
                serializedEvents.put(
                    JSONObject().apply {
                        put("title", event.title)
                        put("startAt", event.startAt ?: JSONObject.NULL)
                        put("endAt", event.endAt ?: JSONObject.NULL)
                        put("location", event.location ?: JSONObject.NULL)
                    }
                )
            }

            prefs.edit()
                .putString(eventsKey(userId), serializedEvents.toString())
                .putLong(lastImportKey(userId), System.currentTimeMillis())
                .apply()

            Log.d(TAG, "import successful userId=$userId storedEvents=${events.size}")
            Result.success(events.size)
        } catch (e: Exception) {
            Log.e(TAG, "import failed userId=$userId error=${e.message}", e)
            Result.failure(Exception(e.message ?: "Unable to import the selected .ics file."))
        }
    }

    fun getImportedEvents(userId: Int): List<ImportedCalendarEvent> {
        val raw = prefs.getString(eventsKey(userId), "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    ImportedCalendarEvent(
                        title = item.optString("title", "Imported event"),
                        startAt = item.optLongOrNull("startAt"),
                        endAt = item.optLongOrNull("endAt"),
                        location = item.optStringOrNull("location")
                    )
                )
            }
        }
    }

    fun hasImportedEvents(userId: Int): Boolean {
        return getImportedEvents(userId).isNotEmpty()
    }

    private fun isValidIcsUri(uri: Uri): Boolean {
        val uriString = uri.toString().lowercase(Locale.getDefault())
        val displayName = appContext.contentResolver.query(uri, arrayOf("_display_name"), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            ?.lowercase(Locale.getDefault())

        return uriString.endsWith(".ics") || (displayName?.endsWith(".ics") == true)
    }

    private fun parseEvents(content: String): List<ImportedCalendarEvent> {
        val unfoldedLines = unfoldLines(content)
        val events = mutableListOf<ImportedCalendarEvent>()
        var currentEvent = mutableMapOf<String, String>()
        var isInsideEvent = false

        unfoldedLines.forEach { line ->
            when {
                line == "BEGIN:VEVENT" -> {
                    isInsideEvent = true
                    currentEvent = mutableMapOf()
                }
                line == "END:VEVENT" -> {
                    if (isInsideEvent) {
                        val title = currentEvent["SUMMARY"]?.takeIf { it.isNotBlank() } ?: "Imported event"
                        events.add(
                            ImportedCalendarEvent(
                                title = title,
                                startAt = parseIcsDate(currentEvent["DTSTART"]),
                                endAt = parseIcsDate(currentEvent["DTEND"]),
                                location = currentEvent["LOCATION"]?.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                    isInsideEvent = false
                }
                isInsideEvent -> {
                    val separatorIndex = line.indexOf(':')
                    if (separatorIndex > 0) {
                        val rawKey = line.substring(0, separatorIndex)
                        val value = line.substring(separatorIndex + 1)
                        val normalizedKey = rawKey.substringBefore(';')
                        currentEvent[normalizedKey] = value
                    }
                }
            }
        }

        return events
    }

    private fun unfoldLines(content: String): List<String> {
        val rawLines = content.replace("\r\n", "\n").split('\n')
        val unfolded = mutableListOf<String>()

        rawLines.forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && unfolded.isNotEmpty()) {
                unfolded[unfolded.lastIndex] = unfolded.last() + line.trimStart()
            } else {
                unfolded.add(line.trim())
            }
        }

        return unfolded.filter { it.isNotBlank() }
    }

    private fun parseIcsDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null

        val formats = listOf(
            SimpleDateFormat("yyyyMMdd", Locale.US),
            SimpleDateFormat("yyyyMMdd'T'HHmm", Locale.US),
            SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US),
            SimpleDateFormat("yyyyMMdd'T'HHmm'Z'", Locale.US),
            SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        )

        formats.forEach { format ->
            try {
                if (value.endsWith("Z")) {
                    format.timeZone = TimeZone.getTimeZone("UTC")
                }
                return format.parse(value)?.time
            } catch (_: Exception) {
                // Try next format.
            }
        }

        return null
    }

    private fun eventsKey(userId: Int): String = "ics_events_$userId"

    private fun lastImportKey(userId: Int): String = "ics_last_import_$userId"

    private companion object {
        const val PREFS_NAME = "ics_import"
        const val TAG = "SpendAntIcs"
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    return if (!has(key) || isNull(key)) null else optLong(key)
}

private fun JSONObject.optStringOrNull(key: String): String? {
    return if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
