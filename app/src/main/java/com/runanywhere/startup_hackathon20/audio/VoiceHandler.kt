package com.runanywhere.startup_hackathon20.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * Voice Handler for Speech-to-Text and Text-to-Speech
 */
class VoiceHandler(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError

    private val _ttsReady = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady

    init {
        initializeTTS()
    }

    // ===== TEXT-TO-SPEECH (Voice Output) =====

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                _ttsReady.value = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED

                // Set up utterance listener
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _voiceError.value = "Speech error occurred"
                    }
                })
            } else {
                _ttsReady.value = false
                _voiceError.value = "Text-to-Speech initialization failed"
            }
        }
    }

    /**
     * Speak text aloud with mentor-specific voice characteristics
     */
    fun speak(
        text: String,
        mentorId: String = "sensei",
        queueMode: Int = TextToSpeech.QUEUE_FLUSH
    ) {
        if (!_ttsReady.value) {
            _voiceError.value = "Text-to-Speech not ready"
            return
        }

        // Adjust voice parameters based on mentor personality
        val (pitch, speed) = getMentorVoiceParameters(mentorId)

        textToSpeech?.setPitch(pitch)
        textToSpeech?.setSpeechRate(speed)

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "mentor_speech")

        textToSpeech?.speak(text, queueMode, params, "mentor_speech")
    }

    /**
     * Get voice parameters for each mentor
     */
    private fun getMentorVoiceParameters(mentorId: String): Pair<Float, Float> {
        return when (mentorId) {
            "sensei" -> Pair(0.9f, 0.85f)      // Lower pitch, slower speed (calm, wise)
            "coach_max" -> Pair(1.1f, 1.0f)    // Normal pitch, normal speed (friendly)
            "mira" -> Pair(1.2f, 0.95f)        // Higher pitch, slightly slower (fairy-like)
            else -> Pair(1.0f, 1.0f)           // Default
        }
    }

    /**
     * Stop current speech
     */
    fun stopSpeaking() {
        if (_isSpeaking.value) {
            textToSpeech?.stop()
            _isSpeaking.value = false
        }
    }

    /**
     * Stop all voice operations (speech and listening)
     */
    fun stop() {
        stopSpeaking()
        stopListening()
    }

    /**
     * Pause speech (if supported)
     */
    fun pauseSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    // ===== SPEECH-TO-TEXT (Voice Input) =====

    /**
     * Initialize speech recognizer
     */
    private fun initializeSpeechRecognizer() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        }
    }

    /**
     * Start listening to user's voice input
     */
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceError.value = "Speech recognition not available on this device"
            return
        }

        initializeSpeechRecognizer()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask your mentor...")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        _isListening.value = true
        _recognizedText.value = ""
        _voiceError.value = null

        speechRecognizer?.startListening(intent)
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    /**
     * Cancel listening
     */
    fun cancelListening() {
        speechRecognizer?.cancel()
        _isListening.value = false
    }

    /**
     * Create recognition listener for handling speech recognition events
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceError.value = null
            }

            override fun onBeginningOfSpeech() {
                // User started speaking
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Voice volume changed (can be used for visual feedback)
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _voiceError.value = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error"
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                } else {
                    _voiceError.value = "No speech recognized"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Partial results during recognition (real-time feedback)
                val matches = partialResults?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Additional events
            }
        }
    }

    // ===== CLEANUP =====

    fun cleanup() {
        stopSpeaking()
        stopListening()

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // ===== UTILITY =====

    fun isVoiceInputAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun isTTSAvailable(): Boolean {
        return _ttsReady.value
    }
}

/**
 * Voice preferences for saving user settings
 */
data class VoicePreferences(
    val voiceEnabled: Boolean = true,
    val autoSpeak: Boolean = false,  // Auto-speak AI responses
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val language: String = "en-US"
)

/**
 * Voice feedback states
 */
sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Processing : VoiceState()
    object Speaking : VoiceState()
    data class Error(val message: String) : VoiceState()
    data class Recognized(val text: String) : VoiceState()
}
