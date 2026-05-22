package com.example.spendantt.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale
import com.example.spendantt.data.local.VoiceDraftDataStore
import com.example.spendantt.data.voice.VoiceExpenseParser
import com.example.spendantt.data.voice.VoiceParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// MULTI-THREADING:
//   Main  — SpeechRecognizer (Android requirement) and UI state updates
//   IO    — VoiceExpenseParser.parse() + VoiceDraftDataStore read/write
//
// CONNECTIVITY (offline-first):
//   1st attempt  → EXTRA_PREFER_OFFLINE = true  (uses on-device model if available)
//   error 12     → retry without EXTRA_PREFER_OFFLINE  (falls back to online silently)
//   error 4/net  → show message, let user retry manually

private const val TAG = "VoiceInputVM"
private const val ERROR_LANGUAGE_NOT_SUPPORTED  = 12
private const val ERROR_LANGUAGE_UNAVAILABLE    = 13

data class VoiceInputUiState(
    val isListening: Boolean          = false,
    val transcript: String            = "",
    val parsedResult: VoiceParseResult = VoiceParseResult(),
    val isParsing: Boolean            = false,
    val hasParsedResult: Boolean      = false,
    val error: String?                = null
)

class VoiceInputViewModel(private val appContext: Context) {

    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val parser = VoiceExpenseParser()
    private var recognizer: SpeechRecognizer? = null

    private val _uiState = MutableStateFlow(VoiceInputUiState())
    val uiState: StateFlow<VoiceInputUiState> = _uiState

    init { loadDraft() }

    // ── Draft persistence ────────────────────────────────────────────────────

    private fun loadDraft() {
        scope.launch(Dispatchers.IO) {
            val draft = VoiceDraftDataStore.loadDraft(appContext) ?: return@launch
            _uiState.value = _uiState.value.copy(
                transcript      = draft.transcript,
                parsedResult    = draft.result,
                hasParsedResult = true
            )
        }
    }

    fun clearDraft() {
        scope.launch(Dispatchers.IO) { VoiceDraftDataStore.clearDraft(appContext) }
        _uiState.value = VoiceInputUiState()
    }

    // ── Speech recognition ───────────────────────────────────────────────────

    fun startListening() = startListeningInternal(preferOffline = true)

    fun stopListening() {
        recognizer?.stopListening()
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    private fun startListeningInternal(preferOffline: Boolean) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _uiState.value = _uiState.value.copy(
                error = "Voice recognition is not available on this device."
            )
            return
        }
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(buildListener(preferOffline))
        }
        val intent = buildIntent(preferOffline)
        _uiState.value = _uiState.value.copy(isListening = true, error = null)
        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(isListening = false, error = "Failed to start listening.")
        }
    }

    private fun buildIntent(preferOffline: Boolean) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,      RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,            Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,                    3_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,          2_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
            if (preferOffline) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

    private fun buildListener(preferOffline: Boolean) = object : RecognitionListener {
        override fun onReadyForSpeech(p: Bundle?) {
            Log.d(TAG, "onReadyForSpeech (offline=$preferOffline)")
            _uiState.value = _uiState.value.copy(isListening = true, error = null)
        }
        override fun onBeginningOfSpeech() { Log.d(TAG, "onBeginningOfSpeech") }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buf: ByteArray?) {}
        override fun onPartialResults(p: Bundle?) {}
        override fun onEvent(type: Int, p: Bundle?) {}
        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            _uiState.value = _uiState.value.copy(isListening = false)
        }

        override fun onResults(results: Bundle) {
            val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            Log.d(TAG, "onResults: $text")
            handleTranscript(text)
        }

        override fun onError(error: Int) {
            Log.e(TAG, "onError code=$error (offline=$preferOffline)")
            when {
                // Offline model doesn't have the language → retry transparently using online
                (error == ERROR_LANGUAGE_NOT_SUPPORTED || error == ERROR_LANGUAGE_UNAVAILABLE) && preferOffline -> {
                    Log.d(TAG, "Offline language unavailable (error=$error), retrying online")
                    _uiState.value = _uiState.value.copy(isListening = false, error = null)
                    startListeningInternal(preferOffline = false)
                }
                // Recognizer busy or client error (previous session not released) → wait then retry
                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                error == SpeechRecognizer.ERROR_CLIENT -> {
                    Log.d(TAG, "Recognizer not ready (error=$error), retrying after delay")
                    _uiState.value = _uiState.value.copy(isListening = false, error = null)
                    scope.launch {
                        delay(600)
                        startListeningInternal(preferOffline)
                    }
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isListening = false,
                        error = mapSpeechError(error)
                    )
                }
            }
        }
    }

    private fun handleTranscript(text: String) {
        _uiState.value = _uiState.value.copy(transcript = text, isParsing = true)
        scope.launch(Dispatchers.IO) {
            val result = parser.parse(text)
            VoiceDraftDataStore.saveDraft(appContext, text, result)
            _uiState.value = _uiState.value.copy(
                isParsing       = false,
                parsedResult    = result,
                hasParsedResult = true,
                error           = null
            )
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    fun cleanup() {
        recognizer?.destroy()
        recognizer = null
        _uiState.value = _uiState.value.copy(isListening = false, isParsing = false)
    }

    fun cancel() {
        cleanup()
        scope.cancel()
    }

    // ── Error mapping ────────────────────────────────────────────────────────

    private fun mapSpeechError(error: Int): String? = when (error) {
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "No internet connection. Connect to internet to use voice input."
        SpeechRecognizer.ERROR_SERVER          -> "Server error. Tap the mic and try again."
        SpeechRecognizer.ERROR_NO_MATCH        -> "Couldn't understand. Tap the mic and try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT  -> "No speech detected. Tap the mic to try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        ERROR_LANGUAGE_NOT_SUPPORTED,
        ERROR_LANGUAGE_UNAVAILABLE             -> "Language not available on this device."
        else                                    -> "Error $error. Tap the mic and try again."
    }
}
