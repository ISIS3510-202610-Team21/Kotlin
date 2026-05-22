package com.example.spendantt.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.spendantt.data.voice.VoiceParseResult
import kotlinx.coroutines.flow.first

// LOCAL STORAGE | DataStore | Persists the last voice transcript + parsed fields across
// sessions — user sees their previous attempt if they exit and reopen Voice Register.
private val Context.voiceDraftStore by preferencesDataStore(name = "voice_draft_prefs")

object VoiceDraftDataStore {

    private val TRANSCRIPT = stringPreferencesKey("transcript")
    private val NAME       = stringPreferencesKey("name")
    private val AMOUNT     = stringPreferencesKey("amount")
    private val CURRENCY   = stringPreferencesKey("currency")
    private val DATE       = stringPreferencesKey("date")
    private val TIME       = stringPreferencesKey("time")
    private val LOCATION   = stringPreferencesKey("location")

    data class VoiceDraft(val transcript: String, val result: VoiceParseResult)

    suspend fun saveDraft(context: Context, transcript: String, result: VoiceParseResult) {
        context.voiceDraftStore.edit { p ->
            p[TRANSCRIPT] = transcript
            p[NAME]       = result.name.orEmpty()
            p[AMOUNT]     = result.amount.orEmpty()
            p[CURRENCY]   = result.detectedCurrencyCode.orEmpty()
            p[DATE]       = result.date.orEmpty()
            p[TIME]       = result.time.orEmpty()
            p[LOCATION]   = result.locationName.orEmpty()
        }
    }

    suspend fun loadDraft(context: Context): VoiceDraft? {
        val p = context.voiceDraftStore.data.first()
        val transcript = p[TRANSCRIPT]?.ifEmpty { null } ?: return null
        return VoiceDraft(
            transcript = transcript,
            result = VoiceParseResult(
                name                 = p[NAME]?.ifEmpty { null },
                amount               = p[AMOUNT]?.ifEmpty { null },
                detectedCurrencyCode = p[CURRENCY]?.ifEmpty { null },
                date                 = p[DATE]?.ifEmpty { null },
                time                 = p[TIME]?.ifEmpty { null },
                locationName         = p[LOCATION]?.ifEmpty { null }
            )
        )
    }

    suspend fun clearDraft(context: Context) {
        context.voiceDraftStore.edit { it.clear() }
    }
}
