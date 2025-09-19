package com.mtlc.studyplan.settings.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mtlc.studyplan.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * Comprehensive voice search manager with speech recognition
 */
class VoiceSearchManager(
    private val context: Context,
    private val fragment: Fragment? = null
) {

    private val voiceSearchScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _voiceSearchState = MutableStateFlow(VoiceSearchState())
    val voiceSearchState: StateFlow<VoiceSearchState> = _voiceSearchState.asStateFlow()

    private val _voiceSearchResults = MutableSharedFlow<VoiceSearchResult>()
    val voiceSearchResults: SharedFlow<VoiceSearchResult> = _voiceSearchResults.asSharedFlow()

    data class VoiceSearchState(
        val isListening: Boolean = false,
        val isAvailable: Boolean = false,
        val hasPermission: Boolean = false,
        val error: VoiceSearchError? = null,
        val partialResult: String = "",
        val confidenceLevel: Float = 0f
    )

    sealed class VoiceSearchResult {
        data class Success(
            val query: String,
            val confidence: Float,
            val alternatives: List<String> = emptyList()
        ) : VoiceSearchResult()

        data class Error(val error: VoiceSearchError) : VoiceSearchResult()
        object Cancelled : VoiceSearchResult()
        data class PartialResult(val partialText: String) : VoiceSearchResult()
    }

    enum class VoiceSearchError(val messageRes: Int) {
        NO_PERMISSION(R.string.voice_search_permission_required),
        NOT_AVAILABLE(R.string.voice_search_not_available),
        NETWORK_ERROR(R.string.voice_search_network_error),
        AUDIO_ERROR(R.string.voice_search_audio_error),
        RECOGNITION_ERROR(R.string.voice_search_recognition_error),
        TIMEOUT(R.string.voice_search_timeout),
        UNKNOWN(R.string.voice_search_unknown_error)
    }

    companion object {
        private const val VOICE_SEARCH_TIMEOUT_MS = 10000L
        private const val MIN_CONFIDENCE_THRESHOLD = 0.6f
    }

    init {
        checkVoiceSearchAvailability()
    }

    /**
     * Check if voice search is available on the device
     */
    private fun checkVoiceSearchAvailability() {
        val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        _voiceSearchState.value = _voiceSearchState.value.copy(
            isAvailable = isAvailable,
            hasPermission = hasPermission
        )
    }

    /**
     * Start voice search with enhanced recognition
     */
    fun startVoiceSearch(
        language: String = Locale.getDefault().toLanguageTag(),
        maxResults: Int = 5
    ) {
        if (!_voiceSearchState.value.isAvailable) {
            emitError(VoiceSearchError.NOT_AVAILABLE)
            return
        }

        if (!_voiceSearchState.value.hasPermission) {
            emitError(VoiceSearchError.NO_PERMISSION)
            return
        }

        if (isListening) {
            stopVoiceSearch()
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(VoiceRecognitionListener())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_search_prompt))

                // Enhanced recognition parameters
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "en-GB"))
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }

            isListening = true
            _voiceSearchState.value = _voiceSearchState.value.copy(
                isListening = true,
                error = null,
                partialResult = ""
            )

            speechRecognizer?.startListening(intent)

            // Set timeout
            voiceSearchScope.launch {
                kotlinx.coroutines.delay(VOICE_SEARCH_TIMEOUT_MS)
                if (isListening) {
                    stopVoiceSearch()
                    emitError(VoiceSearchError.TIMEOUT)
                }
            }

        } catch (e: Exception) {
            emitError(VoiceSearchError.UNKNOWN)
        }
    }

    /**
     * Stop voice search
     */
    fun stopVoiceSearch() {
        if (!isListening) return

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore errors when stopping
        }

        isListening = false
        _voiceSearchState.value = _voiceSearchState.value.copy(
            isListening = false,
            partialResult = ""
        )
    }

    /**
     * Request microphone permission
     */
    fun requestMicrophonePermission(permissionLauncher: ActivityResultLauncher<String>) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Handle permission result
     */
    fun onPermissionResult(granted: Boolean) {
        _voiceSearchState.value = _voiceSearchState.value.copy(hasPermission = granted)

        if (!granted) {
            emitError(VoiceSearchError.NO_PERMISSION)
        }
    }

    /**
     * Get voice search intent for external recognition
     */
    fun getVoiceSearchIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_search_prompt))
        }
    }

    /**
     * Process external voice search results
     */
    fun processExternalResult(data: Intent?) {
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val confidences = data?.getFloatArray(RecognizerIntent.EXTRA_CONFIDENCE_SCORES)

        if (results.isNullOrEmpty()) {
            emitError(VoiceSearchError.RECOGNITION_ERROR)
            return
        }

        val bestResult = results[0]
        val confidence = confidences?.get(0) ?: 1.0f
        val alternatives = results.drop(1)

        voiceSearchScope.launch {
            _voiceSearchResults.emit(
                VoiceSearchResult.Success(
                    query = bestResult,
                    confidence = confidence,
                    alternatives = alternatives
                )
            )
        }
    }

    /**
     * Emit error result
     */
    private fun emitError(error: VoiceSearchError) {
        _voiceSearchState.value = _voiceSearchState.value.copy(
            error = error,
            isListening = false
        )

        voiceSearchScope.launch {
            _voiceSearchResults.emit(VoiceSearchResult.Error(error))
        }
    }

    /**
     * Enhanced voice recognition listener
     */
    private inner class VoiceRecognitionListener : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            _voiceSearchState.value = _voiceSearchState.value.copy(
                isListening = true,
                error = null
            )
        }

        override fun onBeginningOfSpeech() {
            // User started speaking
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed - could be used for visual feedback
            val normalizedLevel = (rmsdB + 10) / 10 // Normalize to 0-1 range
            _voiceSearchState.value = _voiceSearchState.value.copy(
                confidenceLevel = normalizedLevel.coerceIn(0f, 1f)
            )
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }

        override fun onEndOfSpeech() {
            // User stopped speaking
            _voiceSearchState.value = _voiceSearchState.value.copy(
                isListening = false
            )
        }

        override fun onError(error: Int) {
            val voiceError = when (error) {
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_NETWORK -> VoiceSearchError.NETWORK_ERROR
                SpeechRecognizer.ERROR_AUDIO -> VoiceSearchError.AUDIO_ERROR
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceSearchError.NO_PERMISSION
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceSearchError.TIMEOUT
                else -> VoiceSearchError.UNKNOWN
            }

            isListening = false
            emitError(voiceError)
        }

        override fun onResults(results: Bundle?) {
            val recognizedText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

            isListening = false
            _voiceSearchState.value = _voiceSearchState.value.copy(isListening = false)

            if (recognizedText.isNullOrEmpty()) {
                emitError(VoiceSearchError.RECOGNITION_ERROR)
                return
            }

            val bestResult = recognizedText[0]
            val confidence = confidences?.get(0) ?: 1.0f
            val alternatives = recognizedText.drop(1)

            // Filter results by confidence threshold
            if (confidence >= MIN_CONFIDENCE_THRESHOLD) {
                voiceSearchScope.launch {
                    _voiceSearchResults.emit(
                        VoiceSearchResult.Success(
                            query = bestResult,
                            confidence = confidence,
                            alternatives = alternatives
                        )
                    )
                }
            } else {
                emitError(VoiceSearchError.RECOGNITION_ERROR)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partialText = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = partialText?.firstOrNull() ?: ""

            _voiceSearchState.value = _voiceSearchState.value.copy(partialResult = partial)

            voiceSearchScope.launch {
                _voiceSearchResults.emit(VoiceSearchResult.PartialResult(partial))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Handle custom events
        }
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        stopVoiceSearch()
        voiceSearchScope.launch {
            kotlinx.coroutines.cancel()
        }
    }

    /**
     * Voice search utility functions
     */
    object VoiceSearchUtils {

        /**
         * Clean voice search query for better search results
         */
        fun cleanVoiceQuery(query: String): String {
            return query
                .lowercase()
                .replace(Regex("\\b(search for|find|look for|show me)\\b"), "")
                .replace(Regex("\\b(setting|settings|option|options)\\b"), "")
                .trim()
                .takeIf { it.isNotEmpty() } ?: query
        }

        /**
         * Suggest alternative queries based on voice input
         */
        fun suggestAlternatives(query: String): List<String> {
            val alternatives = mutableListOf<String>()

            // Add common variations
            alternatives.add(query.replace("notification", "notifications"))
            alternatives.add(query.replace("notifications", "notification"))
            alternatives.add(query.replace("privacy", "private"))
            alternatives.add(query.replace("security", "secure"))

            // Add singular/plural variations
            if (query.endsWith("s")) {
                alternatives.add(query.dropLast(1))
            } else {
                alternatives.add(query + "s")
            }

            return alternatives.distinct().filter { it != query }
        }
    }
}