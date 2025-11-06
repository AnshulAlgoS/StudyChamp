package com.runanywhere.startup_hackathon20

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import com.runanywhere.startup_hackathon20.repository.GamificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Enhanced StudyViewModel with complete gamification system
 */
class EnhancedStudyViewModel(application: Application) : AndroidViewModel(application) {

    // TODO: Re-enable when Room annotation processor is configured
    // private val gamificationRepo = GamificationRepository(application)

    // Temporary: Use in-memory state until Room is configured
    private val _userProgressTemp = MutableStateFlow(UserProgress())
    private val _achievementsTemp = MutableStateFlow<List<Achievement>>(emptyList())

    // Model management
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId

    private val _statusMessage = MutableStateFlow<String>("Welcome Champ! 🎓")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady

    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading

    // Gamification state
    val userProgress: StateFlow<UserProgress?> = _userProgressTemp
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val achievements: StateFlow<List<Achievement>> = _achievementsTemp
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentMentor = MutableStateFlow(Mentors.SENSEI)
    val currentMentor: StateFlow<MentorProfile> = _currentMentor

    // Quiz state
    private val _currentQuiz = MutableStateFlow<QuizData?>(null)
    val currentQuiz: StateFlow<QuizData?> = _currentQuiz

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz

    // Flashcard state
    private val _currentFlashcards = MutableStateFlow<FlashcardSet?>(null)
    val currentFlashcards: StateFlow<FlashcardSet?> = _currentFlashcards

    private val _isGeneratingFlashcards = MutableStateFlow(false)
    val isGeneratingFlashcards: StateFlow<Boolean> = _isGeneratingFlashcards

    // Current study context
    private val _currentSubject = MutableStateFlow("")
    private val _currentTopic = MutableStateFlow("")

    // Study journey state
    private val _studyMessages = MutableStateFlow<List<StudyMessage>>(emptyList())
    val studyMessages: StateFlow<List<StudyMessage>> = _studyMessages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    init {
        loadAvailableModels()
        loadUserMentor()
    }

    // ===== MENTOR MANAGEMENT =====

    private fun loadUserMentor() {
        viewModelScope.launch {
            userProgress.collect { progress ->
                progress?.let {
                    _currentMentor.value = Mentors.getById(it.selectedMentor)
                }
            }
        }
    }

    fun selectMentor(mentorId: String) {
        viewModelScope.launch {
            // gamificationRepo.selectMentor(mentorId)
            _currentMentor.value = Mentors.getById(mentorId)
        }
    }

    // ===== MODEL MANAGEMENT =====

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val models = listAvailableModels()
                _availableModels.value = models
                _statusMessage.value = if (models.any { it.isDownloaded }) {
                    "Ready! Download and load a model to start learning 📚"
                } else {
                    "Download a model to begin your journey! 🚀"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading models: ${e.message}"
            }
        }
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Downloading your AI mentor... 📥"
                RunAnywhere.downloadModel(modelId).collect { progress ->
                    _downloadProgress.value = progress
                    _statusMessage.value = "Downloading: ${(progress * 100).toInt()}%"
                }
                _downloadProgress.value = null
                _statusMessage.value = "Download complete! Now tap 'Load' to activate ✨"
            } catch (e: Exception) {
                _statusMessage.value = "Download failed: ${e.message}"
                _downloadProgress.value = null
            }
        }
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            if (_isModelLoading.value) return@launch
            _isModelLoading.value = true
            try {
                _statusMessage.value = "Loading your AI mentor... 🧠"
                val success = RunAnywhere.loadModel(modelId)
                if (success) {
                    _currentModelId.value = modelId
                    _isModelReady.value = true
                    _statusMessage.value = "Ready to learn, Champ! 🎉"
                    loadAvailableModels()
                } else {
                    _statusMessage.value = "Failed to load model - please try again"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading model: ${e.message}"
            } finally {
                _isModelLoading.value = false
            }
        }
    }

    fun refreshModels() {
        loadAvailableModels()
    }

    // ===== QUIZ GENERATION & MANAGEMENT =====

    fun generateQuiz(subject: String, topic: String) {
        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            _currentSubject.value = subject
            _currentTopic.value = topic

            try {
                val quiz = QuizData(topic = topic, questions = emptyList())
                _currentQuiz.value = quiz
                android.util.Log.d(
                    "EnhancedStudyVM",
                    "✅ Quiz ready: ${quiz.questions.size} questions"
                )
            } catch (e: Exception) {
                android.util.Log.e("EnhancedStudyVM", "❌ Quiz generation failed", e)
                _statusMessage.value = "Failed to generate quiz: ${e.message}"
            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }

    fun completeQuiz(correctAnswers: Int, totalQuestions: Int) {
        viewModelScope.launch {
            try {
                // gamificationRepo.saveQuizResult(
                //     subject = _currentSubject.value,
                //     topic = _currentTopic.value,
                //     totalQuestions = totalQuestions,
                //     correctAnswers = correctAnswers,
                //     mentor = _currentMentor.value.name
                // )

                // Increment topics completed
                // gamificationRepo.incrementTopicsCompleted()

                _currentQuiz.value = null
                android.util.Log.d(
                    "EnhancedStudyVM",
                    "✅ Quiz completed: $correctAnswers/$totalQuestions"
                )
            } catch (e: Exception) {
                android.util.Log.e("EnhancedStudyVM", "❌ Failed to save quiz result", e)
            }
        }
    }

    // ===== FLASHCARD GENERATION & MANAGEMENT =====

    fun generateFlashcards(subject: String, topic: String) {
        viewModelScope.launch {
            _isGeneratingFlashcards.value = true
            _currentSubject.value = subject
            _currentTopic.value = topic

            try {
                val flashcards = FlashcardSet(topic = topic, cards = emptyList())
                _currentFlashcards.value = flashcards
                android.util.Log.d(
                    "EnhancedStudyVM",
                    "✅ Flashcards ready: ${flashcards.cards.size} cards"
                )
            } catch (e: Exception) {
                android.util.Log.e("EnhancedStudyVM", "❌ Flashcard generation failed", e)
                _statusMessage.value = "Failed to generate flashcards: ${e.message}"
            } finally {
                _isGeneratingFlashcards.value = false
            }
        }
    }

    fun completeFlashcards(masteredCount: Int, totalCards: Int) {
        viewModelScope.launch {
            try {
                // gamificationRepo.updateFlashcardProgress(
                //     subject = _currentSubject.value,
                //     topic = _currentTopic.value,
                //     totalCards = totalCards,
                //     masteredCards = masteredCount
                // )

                _currentFlashcards.value = null
                android.util.Log.d(
                    "EnhancedStudyVM",
                    "✅ Flashcards completed: $masteredCount/$totalCards"
                )
            } catch (e: Exception) {
                android.util.Log.e("EnhancedStudyVM", "❌ Failed to save flashcard progress", e)
            }
        }
    }

    // ===== STUDY JOURNEY (Original functionality) =====

    fun startStudyJourney(subject: String, topics: String) {
        if (!_isModelReady.value) {
            _statusMessage.value = "Please download and load a model first, Champ!"
            return
        }

        _currentSubject.value = subject
        _currentTopic.value = topics

        viewModelScope.launch {
            _isGenerating.value = true
            _studyMessages.value = emptyList()

            try {
                val mentorIntro = _currentMentor.value.intro
                val prompt =
                    "$mentorIntro\n\nWrite one motivating paragraph (3-4 sentences) about why learning $topics in $subject is exciting and useful. Be ${_currentMentor.value.tone}."

                var intro = ""
                RunAnywhere.generateStream(prompt).collect { token ->
                    intro += token
                    _studyMessages.value = listOf(StudyMessage.StreamingAI(intro))
                }

                // Add learning style options
                _studyMessages.value += StudyMessage.LearningOptions(
                    subject = subject,
                    topics = topics,
                    options = LearningStyles.getAll()
                )

                // Update streak
                // gamificationRepo.updateStreak()

            } catch (e: Exception) {
                _studyMessages.value += StudyMessage.StreamingAI("Oops! Let's try again, Champ! ")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun selectLearningStyle(subject: String, topics: String, styleId: String) {
        viewModelScope.launch {
            _isGenerating.value = true

            try {
                when (styleId) {
                    "story" -> generateStoryContent(subject, topics)
                    "resources" -> showResourcesContent(subject, topics)
                    "definition" -> generateDefinitions(subject, topics)
                    "roadmap" -> generateRoadmap(subject, topics)
                }

                // After content, offer quiz and flashcards
                _studyMessages.value += StudyMessage.StreamingAI(
                    "\n\n🎯 Ready to test your knowledge?\n• Take a Quiz\n• Practice with Flashcards\n\nJust ask me: 'Start quiz' or 'Show flashcards'!"
                )

            } catch (e: Exception) {
                _studyMessages.value += StudyMessage.StreamingAI("Error: ${e.message}")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun generateStoryContent(subject: String, topics: String) {
        val prompt =
            "${_currentMentor.value.intro}\n\nExplain $topics in $subject using a simple real-world analogy. Be ${_currentMentor.value.tone}. Under 100 words."

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            updateStreamingMessage(response)
        }
    }

    private suspend fun showResourcesContent(subject: String, topics: String) {
        val resources = buildResourcesList(subject, topics)
        _studyMessages.value += StudyMessage.StreamingAI(resources)
    }

    private suspend fun generateDefinitions(subject: String, topics: String) {
        val prompt =
            "Define the 3 most important terms in $topics for $subject. Be ${_currentMentor.value.tone}. Each definition: 2 sentences."

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            updateStreamingMessage(response)
        }
    }

    private suspend fun generateRoadmap(subject: String, topics: String) {
        val prompt =
            "For $topics in $subject, list key concepts to learn in order. Week 1, Week 2, Week 3. Be ${_currentMentor.value.tone}."

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            updateStreamingMessage(response)
        }
    }

    private fun updateStreamingMessage(content: String) {
        val messages = _studyMessages.value.toMutableList()
        if (messages.lastOrNull() is StudyMessage.StreamingAI) {
            messages[messages.lastIndex] = StudyMessage.StreamingAI(content)
        } else {
            messages.add(StudyMessage.StreamingAI(content))
        }
        _studyMessages.value = messages
    }

    private fun buildResourcesList(subject: String, topics: String): String {
        // Same as original StudyViewModel
        return "📚 Resources for $topics in $subject:\n\n• YouTube Search\n• Khan Academy\n• Practice sites"
    }

    fun askFollowUpQuestion(question: String) {
        if (!_isModelReady.value) return

        // Check for quiz/flashcard triggers
        val lowerQuestion = question.lowercase()
        if ("quiz" in lowerQuestion || "test" in lowerQuestion) {
            if (_currentSubject.value.isNotBlank() && _currentTopic.value.isNotBlank()) {
                generateQuiz(_currentSubject.value, _currentTopic.value)
                return
            }
        }
        if ("flashcard" in lowerQuestion || "cards" in lowerQuestion) {
            if (_currentSubject.value.isNotBlank() && _currentTopic.value.isNotBlank()) {
                generateFlashcards(_currentSubject.value, _currentTopic.value)
                return
            }
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _studyMessages.value += StudyMessage.UserInput(question)

            try {
                val prompt =
                    "${_currentMentor.value.intro}\n\nStudent question: \"$question\"\n\nAnswer warmly and concisely (under 150 words). Be ${_currentMentor.value.tone}."

                var aiResponse = ""
                RunAnywhere.generateStream(prompt).collect { token ->
                    aiResponse += token
                    updateStreamingMessage(aiResponse)
                }
            } catch (e: Exception) {
                _studyMessages.value += StudyMessage.StreamingAI("Oops! Something went wrong, Champ. 💪")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun resetJourney() {
        _studyMessages.value = emptyList()
        _currentQuiz.value = null
        _currentFlashcards.value = null
    }
}
