package com.runanywhere.startup_hackathon20

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import com.runanywhere.startup_hackathon20.repository.FirebaseRepository
import com.runanywhere.startup_hackathon20.ai.*
import com.runanywhere.startup_hackathon20.audio.VoiceHandler
import com.runanywhere.startup_hackathon20.youtube.YouTubeRepository
import com.runanywhere.startup_hackathon20.youtube.YouTubeVideo
import com.runanywhere.startup_hackathon20.youtube.YouTubeChannel
import com.runanywhere.startup_hackathon20.utils.NetworkUtils
import com.runanywhere.startup_hackathon20.services.ResourceFetcherService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlin.Result

/**
 * Firebase-integrated StudyViewModel with complete gamification system + AI Brain + Voice + YouTube
 */
class FirebaseStudyViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepo = FirebaseRepository()
    private val youtubeRepo = YouTubeRepository()
    private val resourceFetcher = ResourceFetcherService()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ===== YOUTUBE STATE =====
    private val _youtubeVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val youtubeVideos: StateFlow<List<YouTubeVideo>> = _youtubeVideos

    private val _youtubeChannels = MutableStateFlow<List<YouTubeChannel>>(emptyList())
    val youtubeChannels: StateFlow<List<YouTubeChannel>> = _youtubeChannels

    private val _isYouTubeLoading = MutableStateFlow(false)
    val isYouTubeLoading: StateFlow<Boolean> = _isYouTubeLoading

    // ===== VOICE HANDLER =====
    private val voiceHandler = VoiceHandler(application)

    // Voice state
    private val _isVoiceEnabled = MutableStateFlow(true) // Auto-speak AI responses
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled

    private val _isSpeaking = voiceHandler.isSpeaking
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    fun setVoiceEnabled(enabled: Boolean) {
        _isVoiceEnabled.value = enabled
        if (!enabled) {
            voiceHandler.stop()
        }
    }

    fun stopVoice() {
        voiceHandler.stop()
    }

    // ===== AI BRAIN INTEGRATION =====
    private val memoryStorage = LocalMemoryStorage(application)
    private val memoryContextBuilder = MemoryContextBuilder(memoryStorage)
    private val adaptiveBehaviorEngine = AdaptiveBehaviorEngine()

    // ===== USER & AUTH STATE =====
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    // ===== MODEL MANAGEMENT =====
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _downloadingModelId = MutableStateFlow<String?>(null)
    val downloadingModelId: StateFlow<String?> = _downloadingModelId

    private var downloadJob: kotlinx.coroutines.Job? = null

    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId

    private val _statusMessage = MutableStateFlow<String>("Welcome Champ! 🎓")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady

    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading

    // ===== MENTOR STATE =====
    private val _currentMentor = MutableStateFlow(Mentors.SENSEI)
    val currentMentor: StateFlow<MentorProfile> = _currentMentor

    // ===== QUIZ STATE =====
    private val _currentQuiz = MutableStateFlow<QuizData?>(null)
    val currentQuiz: StateFlow<QuizData?> = _currentQuiz

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz

    private val _quizHistory = MutableStateFlow<List<FirebaseQuizResult>>(emptyList())
    val quizHistory: StateFlow<List<FirebaseQuizResult>> = _quizHistory

    // ===== FLASHCARD STATE =====
    private val _currentFlashcards = MutableStateFlow<FlashcardSet?>(null)
    val currentFlashcards: StateFlow<FlashcardSet?> = _currentFlashcards

    private val _isGeneratingFlashcards = MutableStateFlow(false)
    val isGeneratingFlashcards: StateFlow<Boolean> = _isGeneratingFlashcards

    private val _flashcardHistory = MutableStateFlow<List<FirebaseFlashcardProgress>>(emptyList())
    val flashcardHistory: StateFlow<List<FirebaseFlashcardProgress>> = _flashcardHistory

    // ===== ACHIEVEMENTS STATE =====
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements

    // ===== STUDY JOURNEY STATE =====
    private val _studyMessages = MutableStateFlow<List<StudyMessage>>(emptyList())
    val studyMessages: StateFlow<List<StudyMessage>> = _studyMessages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _currentSubject = MutableStateFlow("")
    private val _currentTopic = MutableStateFlow("")
    private var currentSessionId: String? = null

    init {
        checkAuthAndLoadProfile()
        loadAvailableModels()
        initializeAIBrain()
    }

    // ===== AI BRAIN INITIALIZATION =====

    private fun initializeAIBrain() {
        viewModelScope.launch {
            val userId = "local_user" // Use local ID for offline storage

            // Check if user context exists, if not create it
            var userContext = memoryStorage.loadUserContext(userId)
            if (userContext == null) {
                // Create initial learning context
                userContext = UserLearningContext(
                    userId = userId,
                    personalityAffinity = "sensei",
                    learningRate = "moderate",
                    recentAccuracy = 0.7f,
                    currentMood = "neutral"
                )
                memoryStorage.saveUserContext(userContext)
                android.util.Log.d("AIBrain", "Created initial user context")
            } else {
                android.util.Log.d("AIBrain", "Loaded existing user context")
            }
        }
    }

    // ===== HELPER: Get AI Brain Mentor Personality =====

    private fun getAIBrainMentorPersonality(): MentorPersonality {
        val userId = "local_user"
        val userContext = memoryStorage.loadUserContext(userId) ?: UserLearningContext(
            userId = userId,
            personalityAffinity = _currentMentor.value.id,
            learningRate = "moderate",
            recentAccuracy = 0.7f
        )

        return when (_currentMentor.value.id) {
            "sensei" -> MentorPersonality.getSensei(userContext)
            "coach_max" -> MentorPersonality.getCoachMax(userContext)
            "mira" -> MentorPersonality.getMira(userContext)
            else -> MentorPersonality.getSensei(userContext)
        }
    }

    // ===== AUTHENTICATION & USER MANAGEMENT =====

    private fun checkAuthAndLoadProfile() {
        viewModelScope.launch {
            try {
                val currentUser = firebaseRepo.getCurrentUser()
                if (currentUser != null) {
                    _currentUserId.value = currentUser.uid
                    _isSignedIn.value = true
                    loadUserProfile(currentUser.uid)
                } else {
                    // Auto sign-in anonymously
                    signInAnonymously()
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseStudyVM", "Auth check failed: ${e.message}", e)
                // Use offline mode
                enableOfflineMode()
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.signInAnonymously()
                when {
                    result.isSuccess -> {
                        val user = result.getOrNull()
                        user?.let {
                            _currentUserId.value = it.uid
                            _isSignedIn.value = true

                            // Check if profile exists, if not, prompt for creation
                            loadUserProfile(it.uid)
                        }
                    }
                    result.isFailure -> {
                        android.util.Log.e(
                            "FirebaseStudyVM",
                            "Sign-in failed: ${result.exceptionOrNull()?.message}"
                        )
                        _statusMessage.value = "Using offline mode - features limited"
                        // Use offline mode as fallback
                        enableOfflineMode()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseStudyVM", "Sign-in exception: ${e.message}", e)
                _statusMessage.value = "Using offline mode - features limited"
                // Use offline mode as fallback
                enableOfflineMode()
            }
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.signInWithEmail(email, password)
                when {
                    result.isSuccess -> {
                        val user = result.getOrNull()
                        user?.let {
                            _currentUserId.value = it.uid
                            _isSignedIn.value = true
                            _statusMessage.value = "Welcome back!"
                            loadUserProfile(it.uid)
                        }
                        onResult(true, null)
                    }

                    result.isFailure -> {
                        val error = result.exceptionOrNull()?.message ?: "Sign in failed"
                        _statusMessage.value = error
                        _isSignedIn.value = false
                        onResult(false, error)
                    }
                }
            } catch (e: Exception) {
                val error = e.message ?: "Sign in failed"
                _statusMessage.value = error
                _isSignedIn.value = false
                onResult(false, error)
            }
        }
    }

    fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.signUpWithEmail(email, password, name)
                when {
                    result.isSuccess -> {
                        val user = result.getOrNull()
                        user?.let {
                            _currentUserId.value = it.uid
                            _isSignedIn.value = true

                            // Create user profile with name
                            val profileResult = firebaseRepo.createUserProfile(name, email)
                            if (profileResult.isSuccess) {
                                loadUserProfile(it.uid)
                                _statusMessage.value = "Welcome to StudyChamp, $name!"
                                onResult(true, null)
                            } else {
                                _statusMessage.value = "Account created but profile setup failed"
                                onResult(false, "Profile setup failed")
                            }
                        }
                    }

                    result.isFailure -> {
                        val error = result.exceptionOrNull()?.message ?: "Sign up failed"
                        _statusMessage.value = error
                        _isSignedIn.value = false
                        onResult(false, error)
                    }
                }
            } catch (e: Exception) {
                val error = e.message ?: "Sign up failed"
                _statusMessage.value = error
                _isSignedIn.value = false
                onResult(false, error)
            }
        }
    }

    /**
     * Enable offline mode when Firebase is not available
     */
    private fun enableOfflineMode() {
        android.util.Log.d("FirebaseStudyVM", "Enabling offline mode")

        // Create a local offline user ID
        _currentUserId.value = "offline_user"
        _isSignedIn.value = true

        // Create a basic offline profile
        _userProfile.value = UserProfile(
            userId = "offline_user",
            name = "Offline User",
            email = "",
            totalXP = 0,
            level = 1,
            currentStreak = 0,
            longestStreak = 0,
            selectedMentor = "sensei",
            quizzesCompleted = 0,
            flashcardsCompleted = 0,
            topicsCompleted = 0
        )

        _statusMessage.value = "Offline Mode - AI features available! "
        android.util.Log.d("FirebaseStudyVM", "Offline mode enabled successfully")
    }

    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            // First try to get existing profile
            val result = firebaseRepo.getUserProfile(userId)
            when {
                result.isSuccess -> {
                    val profile = result.getOrNull()
                    if (profile != null) {
                        _userProfile.value = profile
                        _currentMentor.value = Mentors.getById(profile.selectedMentor)

                        // Observe changes
                        firebaseRepo.observeUserProfile(userId).collect {
                            _userProfile.value = it
                        }

                        // Load quiz and flashcard history
                        loadQuizHistory(userId)
                        loadFlashcardHistory(userId)

                        // Load achievements
                        loadAchievements(userId)

                        // Update AI Brain with user stats
                        updateAIBrainContext(profile)
                    } else {
                        // Profile doesn't exist - this is a new user
                        // Will be created when user sets their name
                    }
                }
            }
        }
    }

    private fun updateAIBrainContext(profile: UserProfile) {
        viewModelScope.launch {
            val userId = "local_user"
            var context =
                memoryStorage.loadUserContext(userId) ?: UserLearningContext(userId = userId)

            // Update context with Firebase profile data
            context = context.copy(
                totalXP = profile.totalXP,
                streakDays = profile.currentStreak,
                personalityAffinity = profile.selectedMentor
            )

            memoryStorage.saveUserContext(context)
        }
    }

    fun createOrUpdateProfile(name: String, email: String = "") {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch

            val existingProfile = _userProfile.value
            if (existingProfile == null) {
                // Create new profile
                val result = firebaseRepo.createUserProfile(name, email)
                when {
                    result.isSuccess -> {
                        _statusMessage.value = "Welcome to StudyChamp, $name! "
                        loadUserProfile(userId)
                    }

                    result.isFailure -> {
                        _statusMessage.value = "Failed to create profile. Please try again."
                    }
                }
            } else {
                // Update existing profile
                val updates = mutableMapOf<String, Any>("name" to name)
                if (email.isNotEmpty()) updates["email"] = email

                firebaseRepo.updateUserProfile(userId, updates)
                _statusMessage.value = "Profile updated! 🎉"
            }
        }
    }

    fun signOut() {
        firebaseRepo.signOut()
        _currentUserId.value = null
        _isSignedIn.value = false
        _userProfile.value = null
        _studyMessages.value = emptyList()
        _currentQuiz.value = null
        _currentFlashcards.value = null
        _achievements.value = emptyList()
        _statusMessage.value = "Signed out successfully"

        // Stop voice if speaking
        voiceHandler.stop()

        android.util.Log.d("FirebaseStudyVM", "User signed out successfully")
    }

    fun selectMentor(mentorId: String) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            _currentMentor.value = Mentors.getById(mentorId)
            firebaseRepo.updateUserProfile(userId, mapOf("selectedMentor" to mentorId))

            // Update AI Brain memory with new mentor
            val localUserId = "local_user"
            val context = memoryStorage.loadUserContext(localUserId)
            if (context != null) {
                memoryStorage.saveUserContext(context.copy(personalityAffinity = mentorId))
            }

            android.util.Log.d("AIBrain", "Mentor changed to: ${_currentMentor.value.name}")

            // Generate personalized introduction from the mentor
            val mentor = _currentMentor.value
            val introMessage = when (mentorId) {
                "sensei" -> """
                    Greetings, young scholar. I am Sensei, your philosophical guide on this journey of enlightenment.
                    
                    Like the ancient oak that grows slowly yet stands for centuries, true knowledge comes not from rushing, but from patient contemplation. I shall guide you not with answers, but with questions that illuminate the path within.
                    
                    As the masters of old taught: "The journey of a thousand miles begins with understanding a single step." Let us walk this path together, with wisdom as our compass and curiosity as our light.
                    
                    Tell me, what knowledge do you seek today?
                """.trimIndent()

                "coach_max" -> """
                    Hey there, champ! I'm Coach Max, and I'm SO excited to be learning with you!
                    
                    You know what's awesome? We're going to grow together on this journey! I'm not just here to teach you - I'm here to learn alongside you, celebrate your wins, and pick you up when things get tricky.
                    
                    We're a team now, you and me! And you know what teams do? They support each other, they grow together, and they have fun while doing it! Every challenge we face, we'll tackle it together.
                    
                    So champ, what are we learning today? Let's do this! 💪
                """.trimIndent()

                "mira" -> """
                    ✨ Greetings, dear friend! I am Mira, a fairy from the enchanted realm of knowledge, and I'm delighted to be your guide!
                    
                    *sparkles of stardust swirl around* Once upon a time, in a magical library far beyond the clouds, fairies like me discovered that every subject is actually a wondrous tale waiting to be told! Mathematics dances with number sprites, science holds the secrets of wizard atoms, and history whispers ancient stories...
                    
                    I shall weave every lesson into an enchanting adventure, transforming concepts into magical characters and ideas into fairy tales! Together, we'll explore realms where learning feels like opening a storybook.
                    
                    So tell me, dear friend, which magical realm shall we venture into today? 🧚‍♀️✨
                """.trimIndent()

                else -> "${mentor.name} is ready to guide you! ${mentor.intro}"
            }

            _statusMessage.value = "Your mentor ${mentor.name} is ready to guide you! 🌟"

            // Speak the introduction in the mentor's voice
            if (_isVoiceEnabled.value) {
                voiceHandler.speak(introMessage, mentor.voiceId)
            }

            android.util.Log.d("MentorIntro", "Playing introduction for ${mentor.name}")
        }
    }

    // ===== QUIZ GENERATION & MANAGEMENT =====

    fun generateQuiz(subject: String, topic: String) {
        if (!_isModelReady.value) {
            _statusMessage.value = "Please load a model first!"
            return
        }

        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            _currentSubject.value = subject
            _currentTopic.value = topic

            try {
                val mentorContext = getAIBrainMentorPersonality()
                val prompt = """
You are creating a quiz to test knowledge about "$topic" in $subject.

IMPORTANT: All questions must be directly about $topic content, concepts, definitions, or applications.
DO NOT ask questions about study methods, note-taking, or general learning tips.

Create exactly 5 multiple-choice questions that test understanding of $topic.
Each question must:
- Be specifically about $topic (NOT about how to study)
- Test knowledge of concepts, facts, formulas, or applications
- Have 4 options labeled A, B, C, D
- Have exactly one correct answer
- Include a helpful hint related to $topic

Return ONLY valid JSON in this exact format (no markdown, no code blocks):
{
  "topic": "$topic",
  "questions": [
    {
      "question": "What is the specific concept/definition/formula in $topic?",
      "options": ["Correct answer about $topic", "Wrong answer", "Wrong answer", "Wrong answer"],
      "answer": "Correct answer about $topic",
      "hint": "Hint explaining the concept in $topic"
    }
  ]
}

Examples of GOOD questions for Physics/Newton's Laws:
- "What does Newton's First Law state?"
- "What is the formula for Newton's Second Law?"

Examples of BAD questions to AVOID:
- "How should you study effectively?"
- "Should we study from notes or textbooks?"
- "What is the best way to memorize?"

Now create 5 questions specifically about $topic in $subject:
                """.trimIndent()

                android.util.Log.d("FirebaseStudyVM", "Sending quiz prompt for: $topic in $subject")

                var fullResponse = ""
                RunAnywhere.generateStream(prompt).collect { token ->
                    fullResponse += token
                }

                android.util.Log.d(
                    "FirebaseStudyVM",
                    "AI Response length: ${fullResponse.length} chars"
                )
                android.util.Log.d(
                    "FirebaseStudyVM",
                    "AI Response preview: ${fullResponse.take(300)}"
                )

                // Parse JSON response
                val quizData = parseQuizResponse(fullResponse, topic)
                _currentQuiz.value = quizData

                android.util.Log.d(
                    "FirebaseStudyVM",
                    " Quiz generated: ${quizData.questions.size} questions"
                )

                // Speak the AI quiz instructions in mentor voice, if enabled
                if (_isVoiceEnabled.value && quizData.questions.isNotEmpty()) {
                    val mentorVoice = _currentMentor.value.voiceId ?: "default"
                    val intro = "Here's your quiz on ${quizData.topic}. Ready, Champ?"
                    voiceHandler.speak(intro, mentorVoice)
                }

            } catch (e: Exception) {
                android.util.Log.e("FirebaseStudyVM", " Quiz generation failed: ${e.message}", e)

                // Fallback: Create topic-specific quiz
                _currentQuiz.value = createTopicSpecificQuiz(subject, topic)

            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }

    private fun parseQuizResponse(response: String, topic: String): QuizData {
        return try {
            // Try to extract JSON from markdown code blocks if present
            var jsonContent = response

            // Remove markdown code blocks
            if (response.contains("```json")) {
                jsonContent = response.substringAfter("```json").substringBefore("```").trim()
            } else if (response.contains("```")) {
                jsonContent = response.substringAfter("```").substringBefore("```").trim()
            }

            // Try to find JSON object in the response
            val jsonStart = jsonContent.indexOf("{")
            val jsonEnd = jsonContent.lastIndexOf("}") + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonContent = jsonContent.substring(jsonStart, jsonEnd)
            }

            android.util.Log.d("FirebaseStudyVM", "Parsing JSON: ${jsonContent.take(200)}")

            @Serializable
            data class QuizResponse(
                val topic: String,
                val questions: List<QuizQuestion>
            )

            val quizResponse = json.decodeFromString<QuizResponse>(jsonContent)

            if (quizResponse.questions.isEmpty()) {
                android.util.Log.e("FirebaseStudyVM", "No questions in parsed response")
                throw Exception("No questions generated")
            }

            QuizData(
                topic = quizResponse.topic,
                questions = quizResponse.questions
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStudyVM", "Failed to parse quiz JSON: ${e.message}", e)
            throw e
        }
    }

    private fun createTopicSpecificQuiz(subject: String, topic: String): QuizData {
        android.util.Log.d(
            "FirebaseStudyVM",
            "Creating topic-specific fallback quiz for: $topic in $subject"
        )

        // Create better topic-specific questions based on common subjects
        val questions = when {
            topic.contains("Newton", ignoreCase = true) || topic.contains(
                "Motion",
                ignoreCase = true
            ) -> {
                listOf(
                    QuizQuestion(
                        question = "A 5kg object is initially moving at 10 m/s. If a force of 25N is applied in the direction of motion for 2 seconds, what is the final velocity?",
                        options = listOf(
                            "15 m/s",
                            "20 m/s",
                            "10 m/s",
                            "25 m/s"
                        ),
                        answer = "20 m/s",
                        hint = "Use Newton's Second Law: F = ma, then find acceleration and use v = u + at. Acceleration = 25N/5kg = 5 m/s². Final velocity = 10 + (5×2) = 20 m/s"
                    ),
                    QuizQuestion(
                        question = "Two astronauts push off each other in space. Astronaut A (mass 60kg) moves at 2 m/s. If Astronaut B has mass 80kg, what is B's velocity?",
                        options = listOf(
                            "1.5 m/s in opposite direction",
                            "2 m/s in same direction",
                            "2.67 m/s in opposite direction",
                            "1 m/s in opposite direction"
                        ),
                        answer = "1.5 m/s in opposite direction",
                        hint = "Newton's Third Law: momentum is conserved. 60×2 = 80×v, so v = 120/80 = 1.5 m/s in opposite direction"
                    ),
                    QuizQuestion(
                        question = "A car traveling at 20 m/s brakes with a constant deceleration and stops in 50 meters. What was the magnitude of acceleration?",
                        options = listOf(
                            "4 m/s²",
                            "2 m/s²",
                            "8 m/s²",
                            "0.4 m/s²"
                        ),
                        answer = "4 m/s²",
                        hint = "Use v² = u² + 2as. Here: 0² = 20² + 2×a×50, so 0 = 400 + 100a, therefore a = -4 m/s² (magnitude is 4 m/s²)"
                    ),
                    QuizQuestion(
                        question = "A 0.5kg ball moving at 10 m/s hits a wall and bounces back at 8 m/s. What is the change in momentum?",
                        options = listOf(
                            "9 kg⋅m/s",
                            "1 kg⋅m/s",
                            "18 kg⋅m/s",
                            "2 kg⋅m/s"
                        ),
                        answer = "9 kg⋅m/s",
                        hint = "Change in momentum = final momentum - initial momentum. Taking direction into account: (0.5×(-8)) - (0.5×10) = -4 - 5 = -9 kg⋅m/s. Magnitude is 9 kg⋅m/s"
                    ),
                    QuizQuestion(
                        question = "Why does it take longer to stop a loaded truck than an empty truck traveling at the same speed?",
                        options = listOf(
                            "The loaded truck has more inertia (greater mass) and requires more force or time to stop",
                            "The empty truck has less friction with the road",
                            "The loaded truck has better brakes",
                            "The empty truck is always faster"
                        ),
                        answer = "The loaded truck has more inertia (greater mass) and requires more force or time to stop",
                        hint = "Newton's First Law (inertia) - objects with greater mass resist changes in motion more. For the same braking force, the more massive loaded truck has less deceleration"
                    )
                )
            }

            topic.contains("Gravity", ignoreCase = true) || topic.contains(
                "Gravitation",
                ignoreCase = true
            ) -> {
                listOf(
                    QuizQuestion(
                        question = "Two planets have masses M and 2M, separated by distance D. At what point between them is the gravitational force on a small mass zero?",
                        options = listOf(
                            "At distance D/(1+√2) from planet with mass M",
                            "At the midpoint between the planets",
                            "At distance D/3 from planet with mass M",
                            "At distance D/2 from planet with mass 2M"
                        ),
                        answer = "At distance D/(1+√2) from planet with mass M",
                        hint = "Set the gravitational forces equal: GM/x² = G(2M)/(D-x)². Solving gives x = D/(1+√2)"
                    ),
                    QuizQuestion(
                        question = "If you move from Earth's surface to an altitude equal to Earth's radius, by what factor does your weight change?",
                        options = listOf(
                            "It becomes 1/4 of the original weight",
                            "It becomes 1/2 of the original weight",
                            "It becomes 1/8 of the original weight",
                            "It stays the same"
                        ),
                        answer = "It becomes 1/4 of the original weight",
                        hint = "Gravitational force follows inverse square law: F ∝ 1/r². At radius R+R=2R, force becomes 1/(2²) = 1/4 of original"
                    ),
                    QuizQuestion(
                        question = "Calculate the gravitational force between two 50kg masses placed 1 meter apart. (G = 6.67×10⁻¹¹ N⋅m²/kg²)",
                        options = listOf(
                            "1.67 × 10⁻⁷ N",
                            "3.34 × 10⁻⁷ N",
                            "6.67 × 10⁻¹¹ N",
                            "1.67 × 10⁻⁹ N"
                        ),
                        answer = "1.67 × 10⁻⁷ N",
                        hint = "F = G(m₁m₂)/r² = (6.67×10⁻¹¹)(50×50)/1² = 6.67×10⁻¹¹×2500 = 1.67×10⁻⁷ N"
                    ),
                    QuizQuestion(
                        question = "A satellite orbits Earth at radius r with velocity v. If it moves to orbit at radius 4r, what is its new orbital velocity?",
                        options = listOf(
                            "v/2",
                            "v/4",
                            "2v",
                            "v/√2"
                        ),
                        answer = "v/2",
                        hint = "Orbital velocity v ∝ 1/√r. If radius increases by factor of 4, velocity changes by 1/√4 = 1/2"
                    ),
                    QuizQuestion(
                        question = "Why do astronauts feel weightless in orbit even though gravity is still acting on them?",
                        options = listOf(
                            "They are in continuous free fall, accelerating toward Earth at the same rate as their spacecraft",
                            "Gravity is absent in space",
                            "They are too far from Earth for gravity to affect them",
                            "They are moving too fast for gravity to pull them down"
                        ),
                        answer = "They are in continuous free fall, accelerating toward Earth at the same rate as their spacecraft",
                        hint = "Weightlessness = free fall. Both astronaut and spacecraft accelerate at same rate due to gravity, creating no normal force between them"
                    )
                )
            }

            subject.contains("Math", ignoreCase = true) || subject.contains(
                "Algebra",
                ignoreCase = true
            ) -> {
                listOf(
                    QuizQuestion(
                        question = "Solve the system: 2x + 3y = 12 and 4x - y = 5. What is x + y?",
                        options = listOf(
                            "5",
                            "4",
                            "6",
                            "3"
                        ),
                        answer = "5",
                        hint = "Multiply second equation by 3: 12x - 3y = 15. Add to first: 14x = 27, so x = 27/14... Actually solving correctly: x = 3, y = 2, so x+y = 5"
                    ),
                    QuizQuestion(
                        question = "If f(x) = 2x² - 3x + 1, what is f(3) - f(1)?",
                        options = listOf(
                            "12",
                            "8",
                            "10",
                            "14"
                        ),
                        answer = "12",
                        hint = "f(3) = 2(9) - 3(3) + 1 = 18 - 9 + 1 = 10. f(1) = 2(1) - 3(1) + 1 = 0. Difference = 10 - (-2) = 12"
                    ),
                    QuizQuestion(
                        question = "The product of two consecutive odd numbers is 195. What are the numbers?",
                        options = listOf(
                            "13 and 15",
                            "11 and 13",
                            "15 and 17",
                            "9 and 11"
                        ),
                        answer = "13 and 15",
                        hint = "Let numbers be n and n+2. Then n(n+2) = 195, so n² + 2n - 195 = 0. Solving: n = 13, so numbers are 13 and 15"
                    ),
                    QuizQuestion(
                        question = "A rectangle's length is 3 meters more than its width. If the area is 40 m², what is the perimeter?",
                        options = listOf(
                            "26 meters",
                            "24 meters",
                            "28 meters",
                            "22 meters"
                        ),
                        answer = "26 meters",
                        hint = "Let width = w. Then w(w+3) = 40. Solving: w = 5, length = 8. Perimeter = 2(5+8) = 26 meters"
                    ),
                    QuizQuestion(
                        question = "If log₂(x) = 5, what is the value of log₂(8x)?",
                        options = listOf(
                            "8",
                            "7",
                            "40",
                            "13"
                        ),
                        answer = "8",
                        hint = "log₂(8x) = log₂(8) + log₂(x) = 3 + 5 = 8, since log₂(8) = 3"
                    )
                )
            }

            subject.contains("History", ignoreCase = true) -> {
                listOf(
                    QuizQuestion(
                        question = "What was the primary economic reason for European colonization of the Americas in the 16th century?",
                        options = listOf(
                            "Access to gold, silver, and new trade routes to reduce dependence on Asian trade",
                            "To find new farming land",
                            "To escape religious persecution",
                            "To establish democratic governments"
                        ),
                        answer = "Access to gold, silver, and new trade routes to reduce dependence on Asian trade",
                        hint = "Mercantilism drove European powers to seek precious metals and trade advantages"
                    ),
                    QuizQuestion(
                        question = "How did the printing press (1440s) contribute to the Protestant Reformation?",
                        options = listOf(
                            "It allowed rapid dissemination of reformist ideas and vernacular Bibles, challenging Church authority",
                            "It was invented by Martin Luther",
                            "It only printed Catholic texts",
                            "It had no impact on religion"
                        ),
                        answer = "It allowed rapid dissemination of reformist ideas and vernacular Bibles, challenging Church authority",
                        hint = "Information technology revolution enabled spread of Luther's 95 Theses and translated Bibles"
                    ),
                    QuizQuestion(
                        question = "What was a key difference between the American and French Revolutions?",
                        options = listOf(
                            "The American Revolution fought for independence from colonial rule, while the French Revolution sought to overthrow existing social order and monarchy",
                            "The French Revolution happened first",
                            "The American Revolution was more violent",
                            "They had the same leaders"
                        ),
                        answer = "The American Revolution fought for independence from colonial rule, while the French Revolution sought to overthrow existing social order and monarchy",
                        hint = "Different goals: external independence vs internal restructuring of society"
                    ),
                    QuizQuestion(
                        question = "Why was the Battle of Stalingrad (1942-43) considered a turning point in World War II?",
                        options = listOf(
                            "It marked the first major German defeat, stopped Nazi expansion eastward, and shifted momentum to the Allies",
                            "It was the first battle of the war",
                            "Japan surrendered after this battle",
                            "It was fought in Western Europe"
                        ),
                        answer = "It marked the first major German defeat, stopped Nazi expansion eastward, and shifted momentum to the Allies",
                        hint = "Massive Soviet victory that began the pushback against Nazi Germany"
                    ),
                    QuizQuestion(
                        question = "What economic factors contributed to the Great Depression of 1929?",
                        options = listOf(
                            "Stock market speculation, overproduction, unequal wealth distribution, and banking failures",
                            "Only the stock market crash",
                            "World War I ending",
                            "Too many people had jobs"
                        ),
                        answer = "Stock market speculation, overproduction, unequal wealth distribution, and banking failures",
                        hint = "Multiple systemic economic problems culminated in the crash, not just one factor"
                    )
                )
            }

            else -> {
                // Generic but more complex questions
                listOf(
                    QuizQuestion(
                        question = "Which approach demonstrates deeper understanding of $topic in $subject?",
                        options = listOf(
                            "Analyzing how concepts interconnect and applying them to solve novel problems",
                            "Memorizing definitions without understanding",
                            "Only reading about the topic",
                            "Avoiding practice problems"
                        ),
                        answer = "Analyzing how concepts interconnect and applying them to solve novel problems",
                        hint = "Deep understanding requires synthesis and application, not just memorization"
                    ),
                    QuizQuestion(
                        question = "In the context of $topic, what distinguishes expert understanding from novice understanding?",
                        options = listOf(
                            "Experts see patterns, relationships between concepts, and can transfer knowledge to new contexts",
                            "Experts have memorized more facts",
                            "Experts study longer hours",
                            "Experts only use textbooks"
                        ),
                        answer = "Experts see patterns, relationships between concepts, and can transfer knowledge to new contexts",
                        hint = "Expertise involves recognizing deep structures and flexible application"
                    ),
                    QuizQuestion(
                        question = "When solving complex problems in $topic, which strategy is most effective?",
                        options = listOf(
                            "Break down the problem into smaller parts, identify relevant principles, and systematically apply them",
                            "Guess and check randomly",
                            "Only use the first formula you find",
                            "Skip the difficult parts"
                        ),
                        answer = "Break down the problem into smaller parts, identify relevant principles, and systematically apply them",
                        hint = "Systematic problem-solving uses decomposition and principle-based reasoning"
                    ),
                    QuizQuestion(
                        question = "How do fundamental principles in $topic relate to more advanced concepts in $subject?",
                        options = listOf(
                            "Fundamentals provide the foundation that advanced concepts build upon and extend",
                            "They are completely unrelated",
                            "Advanced concepts replace fundamentals",
                            "Fundamentals are only for beginners"
                        ),
                        answer = "Fundamentals provide the foundation that advanced concepts build upon and extend",
                        hint = "Knowledge in $subject forms a hierarchical structure"
                    ),
                    QuizQuestion(
                        question = "What is the most effective way to verify your understanding of $topic?",
                        options = listOf(
                            "Teach the concept to someone else and solve problems you haven't seen before",
                            "Re-read your notes multiple times",
                            "Watch videos about it",
                            "Just take the test"
                        ),
                        answer = "Teach the concept to someone else and solve problems you haven't seen before",
                        hint = "The Feynman Technique and transfer tasks best reveal understanding"
                    )
                )
            }
        }

        // RANDOMIZE THE OPTIONS so correct answer isn't always first!
        val randomizedQuestions = questions.map { question ->
            val shuffledOptions = question.options.shuffled()
            question.copy(options = shuffledOptions)
        }

        return QuizData(topic = topic, questions = randomizedQuestions)
    }

    private fun createFallbackQuiz(topic: String): QuizData {
        // This is now only called in extreme failure cases
        return createTopicSpecificQuiz("General", topic)
    }

    fun completeQuiz(correctAnswers: Int, totalQuestions: Int) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            val subject = _currentSubject.value
            val topic = _currentTopic.value

            try {
                // Calculate XP (25 XP per correct answer)
                val xpEarned = correctAnswers * 25

                // Save to Firebase
                val quizResult = FirebaseQuizResult(
                    userId = userId,
                    subject = subject,
                    topic = topic,
                    totalQuestions = totalQuestions,
                    correctAnswers = correctAnswers,
                    xpEarned = xpEarned,
                    mentorUsed = _currentMentor.value.name,
                    completedAt = Timestamp.now()
                )

                firebaseRepo.saveQuizResult(quizResult)

                // Update user XP and streak
                firebaseRepo.updateXP(userId, xpEarned)
                firebaseRepo.updateStreak(userId)

                // Update user stats
                val profile = _userProfile.value
                if (profile != null) {
                    firebaseRepo.updateUserProfile(
                        userId,
                        mapOf("quizzesCompleted" to (profile.quizzesCompleted + 1))
                    )
                }

                // Check for achievements
                checkAndUnlockAchievements()

                _currentQuiz.value = null
                _statusMessage.value = "Great job! +$xpEarned XP earned! 🎉"

            } catch (e: Exception) {
                android.util.Log.e("FirebaseStudyVM", "Failed to save quiz result", e)
            }
        }
    }

    private fun loadQuizHistory(userId: String) {
        viewModelScope.launch {
            firebaseRepo.getQuizHistory(userId).collect { history ->
                _quizHistory.value = history
            }
        }
    }

    // ===== FLASHCARD GENERATION & MANAGEMENT =====

    fun generateFlashcards(subject: String, topic: String) {
        if (!_isModelReady.value) {
            _statusMessage.value = "Please load a model first!"
            return
        }

        viewModelScope.launch {
            _isGeneratingFlashcards.value = true
            _currentSubject.value = subject
            _currentTopic.value = topic

            try {
                val mentorContext = getAIBrainMentorPersonality()
                val prompt = """
Generate 5 flashcards for learning "$topic" in $subject.
Each flashcard should have a term (front) and definition (back).

Output in this exact JSON format:
{
  "topic": "$topic",
  "cards": [
    {
      "term": "Term or concept",
      "definition": "Clear, concise definition"
    }
  ]
}
                """.trimIndent()

                var fullResponse = ""
                RunAnywhere.generateStream(prompt).collect { token ->
                    fullResponse += token
                }

                // Parse JSON response
                val flashcardSet = parseFlashcardResponse(fullResponse, topic)
                _currentFlashcards.value = flashcardSet

                android.util.Log.d(
                    "FirebaseStudyVM",
                    "✅ Flashcards generated: ${flashcardSet.cards.size} cards"
                )

                // Speak flashcard intro in mentor voice, if enabled
                if (_isVoiceEnabled.value && flashcardSet.cards.isNotEmpty()) {
                    val mentorVoice = _currentMentor.value.voiceId ?: "default"
                    val intro = "Here are your flashcards for ${flashcardSet.topic}."
                    voiceHandler.speak(intro, mentorVoice)
                }

            } catch (e: Exception) {
                android.util.Log.e("FirebaseStudyVM", "❌ Flashcard generation failed", e)

                // Fallback: Create default flashcards
                _currentFlashcards.value = createFallbackFlashcards(topic)

            } finally {
                _isGeneratingFlashcards.value = false
            }
        }
    }

    private fun parseFlashcardResponse(response: String, topic: String): FlashcardSet {
        return try {
            val jsonContent = if (response.contains("```json")) {
                response.substringAfter("```json").substringBefore("```").trim()
            } else if (response.contains("```")) {
                response.substringAfter("```").substringBefore("```").trim()
            } else {
                response.trim()
            }

            @Serializable
            data class FlashcardResponse(
                val topic: String,
                val cards: List<Flashcard>
            )

            val flashcardResponse = json.decodeFromString<FlashcardResponse>(jsonContent)
            FlashcardSet(
                topic = flashcardResponse.topic,
                cards = flashcardResponse.cards
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStudyVM", "Failed to parse flashcard JSON", e)
            createFallbackFlashcards(topic)
        }
    }

    private fun createFallbackFlashcards(topic: String): FlashcardSet {
        // Provide fallback flashcards with highly informative, comprehensive definitions for a few common topics
        val fallbackCards = when {
            topic.contains("Newton", ignoreCase = true) -> listOf(
                Flashcard(
                    term = "Newton's First Law (Law of Inertia)",
                    definition = "An object at rest stays at rest, and an object in motion continues moving at constant velocity in a straight line, unless acted upon by an external unbalanced force. This law explains why passengers lurch forward when a car suddenly stops - their bodies want to maintain their forward motion."
                ),
                Flashcard(
                    term = "Newton's Second Law (F = ma)",
                    definition = "The acceleration of an object is directly proportional to the net force acting on it and inversely proportional to its mass. The formula is Force equals mass times acceleration. This means heavier objects require more force to accelerate at the same rate as lighter objects. For example, pushing a shopping cart becomes harder as you add more items."
                ),
                Flashcard(
                    term = "Newton's Third Law (Action-Reaction)",
                    definition = "For every action force, there is an equal and opposite reaction force. These forces act on different objects. When you push against a wall, the wall pushes back with equal force. This principle explains how rockets work - exhaust gases are pushed downward (action), which propels the rocket upward (reaction)."
                ),
                Flashcard(
                    term = "Inertia",
                    definition = "The tendency of an object to resist changes in its state of motion. Objects with greater mass have greater inertia. This is why it's harder to push a car than a bicycle, and why it's harder to stop a moving truck than a moving skateboard."
                ),
                Flashcard(
                    term = "Net Force and Equilibrium",
                    definition = "The net force is the vector sum of all forces acting on an object. When the net force is zero, the object is in equilibrium and maintains constant velocity (which could be zero). When forces are balanced, there's no acceleration. Example: a book resting on a table has balanced forces - gravity pulling down and the table pushing up."
                )
            )
            topic.contains("Gravity", ignoreCase = true) -> listOf(
                Flashcard(
                    term = "Universal Law of Gravitation",
                    definition = "Every object in the universe attracts every other object with a force that is proportional to the product of their masses and inversely proportional to the square of the distance between their centers. Formula: F = G(m₁m₂)/r². This law explains planetary orbits, tides, and why objects fall to Earth."
                ),
                Flashcard(
                    term = "Gravitational Constant (G)",
                    definition = "A universal constant equal to 6.67 × 10⁻¹¹ N·m²/kg². This extremely small number explains why we don't feel gravitational attraction to everyday objects around us - only massive objects like planets and stars create noticeable gravitational forces. It's the same everywhere in the universe."
                ),
                Flashcard(
                    term = "Weight vs Mass",
                    definition = "Mass is the amount of matter in an object (measured in kilograms) and never changes. Weight is the gravitational force acting on that mass (measured in Newtons) and varies with location. Your mass stays 70 kg everywhere, but your weight on Earth (686 N) is six times greater than on the Moon (114 N) because the Moon's gravity is weaker."
                ),
                Flashcard(
                    term = "Acceleration Due to Gravity (g)",
                    definition = "On Earth's surface, all objects accelerate downward at 9.8 m/s² when falling freely (ignoring air resistance). This means velocity increases by 9.8 meters per second every second. After 1 second of falling, an object moves at 9.8 m/s; after 2 seconds, 19.6 m/s. This value decreases with altitude and varies slightly at different locations on Earth."
                ),
                Flashcard(
                    term = "Inverse Square Law",
                    definition = "Gravitational force decreases with the square of the distance. If you double the distance between two objects, the gravitational force becomes 1/4 as strong. Triple the distance, and the force becomes 1/9 as strong. This explains why gravity weakens rapidly as you move away from a planet."
                )
            )
            else -> listOf(
                Flashcard(
                    term = "Fundamental Concept of $topic",
                    definition = "The core principle underlying $topic that forms the foundation for understanding more advanced ideas. This concept connects theoretical knowledge with practical application and is essential for mastering this subject area."
                ),
                Flashcard(
                    term = "Key Definition in $topic",
                    definition = "A clear, precise explanation of an important term specific to $topic. Understanding this definition is crucial because it provides the language and framework needed to discuss and analyze concepts in this field accurately."
                ),
                Flashcard(
                    term = "Practical Application of $topic",
                    definition = "How $topic is used in real-world scenarios to solve practical problems. This bridges the gap between theory and practice, showing why the concept matters and how it's applied in professional settings or everyday situations."
                ),
                Flashcard(
                    term = "Relationship in $topic",
                    definition = "How different elements in $topic connect and influence each other. Understanding these relationships helps you see the bigger picture, recognize patterns, and solve complex problems by understanding how components work together."
                ),
                Flashcard(
                    term = "Guiding Principle of $topic",
                    definition = "A fundamental truth or rule that governs $topic and guides how we approach problems, make predictions, and understand phenomena in this subject. Principles provide the logical framework for analysis and problem-solving."
                )
            )
        }
        return FlashcardSet(
            topic = topic,
            cards = fallbackCards
        )
    }

    fun completeFlashcards(masteredCount: Int, totalCards: Int) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            val subject = _currentSubject.value
            val topic = _currentTopic.value

            try {
                // Calculate XP (10 XP per mastered card)
                val xpEarned = masteredCount * 10

                // Save to Firebase
                val progress = FirebaseFlashcardProgress(
                    userId = userId,
                    subject = subject,
                    topic = topic,
                    totalCards = totalCards,
                    masteredCards = masteredCount,
                    xpEarned = xpEarned,
                    lastReviewedAt = Timestamp.now()
                )

                firebaseRepo.saveFlashcardProgress(progress)

                // Update user XP and streak
                firebaseRepo.updateXP(userId, xpEarned)
                firebaseRepo.updateStreak(userId)

                // Check for achievements
                checkAndUnlockAchievements()

                _currentFlashcards.value = null
                _statusMessage.value = "Awesome work! +$xpEarned XP earned! 🎉"

            } catch (e: Exception) {
                android.util.Log.e("FirebaseStudyVM", "Failed to save flashcard progress", e)
            }
        }
    }

    private fun loadFlashcardHistory(userId: String) {
        viewModelScope.launch {
            firebaseRepo.getFlashcardHistory(userId).collect { history ->
                _flashcardHistory.value = history
            }
        }
    }

    // ===== ACHIEVEMENTS =====

    private fun loadAchievements(userId: String) {
        viewModelScope.launch {
            val result = firebaseRepo.getUnlockedAchievements(userId)
            when {
                result.isSuccess -> {
                    val firebaseAchievements = result.getOrNull() ?: emptyList()
                    // Map Firebase achievements to local Achievement objects
                    val allAchievements = AchievementDefinitions.getAll()
                    val unlockedIds = firebaseAchievements.map { it.achievementId }.toSet()

                    _achievements.value = allAchievements.map { achievement ->
                        achievement.copy(isUnlocked = unlockedIds.contains(achievement.id))
                    }
                }
            }
        }
    }

    private fun checkAndUnlockAchievements() {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            val profile = _userProfile.value ?: return@launch

            val allAchievements = AchievementDefinitions.getAll()
            val currentAchievements = _achievements.value

            allAchievements.forEach { achievement ->
                val isAlreadyUnlocked =
                    currentAchievements.find { it.id == achievement.id }?.isUnlocked == true

                if (!isAlreadyUnlocked && checkAchievementCondition(achievement, profile)) {
                    // Unlock achievement
                    firebaseRepo.unlockAchievement(userId, achievement)

                    // Award XP
                    firebaseRepo.updateXP(userId, achievement.xpReward)

                    _statusMessage.value =
                        "🏆 Achievement Unlocked: ${achievement.title}! +${achievement.xpReward} XP!"
                }
            }

            // Reload achievements
            loadAchievements(userId)
        }
    }

    private fun checkAchievementCondition(achievement: Achievement, profile: UserProfile): Boolean {
        return when (achievement.id) {
            "quiz_rookie" -> profile.quizzesCompleted >= 1
            "quiz_master" -> profile.quizzesCompleted >= 10
            "flashcard_fan" -> profile.flashcardsCompleted >= 5
            "flashcard_guru" -> profile.flashcardsCompleted >= 50
            "streak_starter" -> profile.currentStreak >= 3
            "week_warrior" -> profile.currentStreak >= 7
            "xp_novice" -> profile.totalXP >= 100
            "xp_champion" -> profile.totalXP >= 1000
            "level_5" -> profile.level >= 5
            "scholar" -> profile.level >= 10
            else -> false
        }
    }

    // ===== MODEL MANAGEMENT =====

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val models = listAvailableModels()
                _availableModels.value = models
                _statusMessage.value = if (models.any { it.isDownloaded }) {
                    "Ready! Load a model to start learning 📚"
                } else {
                    "Download a model to begin your journey! 🚀"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading models: ${e.message}"
            }
        }
    }

    fun downloadModel(modelId: String) {
        android.util.Log.d("DownloadModel", "=== DOWNLOAD START ===")
        android.util.Log.d("DownloadModel", "Model ID: $modelId")

        // Check internet connectivity first
        val hasInternet = NetworkUtils.isInternetAvailable(getApplication())
        android.util.Log.d("DownloadModel", "Internet available: $hasInternet")

        if (!hasInternet) {
            _statusMessage.value = "No internet connection. Please check your network."
            android.util.Log.e("DownloadModel", "No internet - aborting download")
            return
        }

        // Cancel any existing download
        downloadJob?.cancel()
        android.util.Log.d("DownloadModel", "Previous download job cancelled (if any)")

        downloadJob = viewModelScope.launch {
            try {
                android.util.Log.d("DownloadModel", "Starting download coroutine...")
                _downloadingModelId.value = modelId
                _downloadProgress.value = 0f
                _statusMessage.value = "Initializing download... 📥"
                android.util.Log.d(
                    "DownloadModel",
                    "State updated - starting RunAnywhere.downloadModel"
                )

                var progressCount = 0
                RunAnywhere.downloadModel(modelId).collect { progress ->
                    progressCount++
                    _downloadProgress.value = progress
                    _statusMessage.value = "Downloading: ${(progress * 100).toInt()}%"
                    android.util.Log.d(
                        "DownloadModel",
                        "Progress update #$progressCount: ${(progress * 100).toInt()}%"
                    )
                }

                android.util.Log.d(
                    "DownloadModel",
                    "Download flow completed! Total progress updates: $progressCount"
                )

                // Download complete - clear progress and refresh models
                _downloadProgress.value = null
                _downloadingModelId.value = null
                downloadJob = null
                android.util.Log.d(
                    "DownloadModel",
                    "State cleared, scanning for downloaded models..."
                )

                // Scan for downloaded models first
                RunAnywhere.scanForDownloadedModels()
                android.util.Log.d("DownloadModel", "Scan completed")

                // Then refresh the list
                delay(500) // Small delay to ensure scan completes
                loadAvailableModels()
                android.util.Log.d("DownloadModel", "Models list refreshed")

                _statusMessage.value = "Download complete! Now tap 'Load' to activate ✅"
                android.util.Log.d("DownloadModel", "=== DOWNLOAD SUCCESS ===")
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.w("DownloadModel", "Download cancelled by user")
                _statusMessage.value = "Download cancelled"
                _downloadProgress.value = null
                _downloadingModelId.value = null
                downloadJob = null
            } catch (e: Exception) {
                android.util.Log.e("DownloadModel", "Download FAILED with exception:", e)
                android.util.Log.e("DownloadModel", "Exception type: ${e::class.java.simpleName}")
                android.util.Log.e("DownloadModel", "Exception message: ${e.message}")
                e.printStackTrace()

                _statusMessage.value = "Download failed: ${e.message ?: "Unknown error"}"
                _downloadProgress.value = null
                _downloadingModelId.value = null
                downloadJob = null

                android.util.Log.d("DownloadModel", "=== DOWNLOAD FAILED ===")
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _downloadProgress.value = null
        _downloadingModelId.value = null
        downloadJob = null
        _statusMessage.value = "Download cancelled"
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

                    // Refresh models list to show updated status
                    delay(300)
                    loadAvailableModels()
                } else {
                    _statusMessage.value = "Failed to load model - please try again"
                    _currentModelId.value = null
                    _isModelReady.value = false
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading model: ${e.message}"
                _currentModelId.value = null
                _isModelReady.value = false
            } finally {
                _isModelLoading.value = false
            }
        }
    }

    fun refreshModels() {
        loadAvailableModels()
    }

    // ===== STUDY JOURNEY =====

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

            // Create study session
            val userId = _currentUserId.value
            if (userId != null) {
                val session = StudySession(
                    userId = userId,
                    subject = subject,
                    topics = topics,
                    learningStyle = "",
                    startedAt = Timestamp.now()
                )
                val result = firebaseRepo.createStudySession(session)
                when {
                    result.isSuccess -> {
                        currentSessionId = result.getOrNull()
                    }
                }
            }

            try {
                // Use AI Brain mentor personality
                val mentorPersonality = getAIBrainMentorPersonality()
                val localUserId = "local_user"
                val userContext = memoryStorage.loadUserContext(localUserId)

                // Build adaptive prompt with mentor personality emphasized
                val systemPrompt = if (userContext != null) {
                    mentorPersonality.getSystemPrompt(userContext)
                } else {
                    mentorPersonality.basePrompt
                }

                val prompt = """
$systemPrompt

IMPORTANT: Stay completely in character as ${_currentMentor.value.name}. Use ${_currentMentor.value.tone} in your response.

Now, write a welcoming paragraph (3-4 sentences) about learning $topics in $subject.
- Stay true to your personality and teaching style
- Use your characteristic phrases and tone
- Make it feel personal and engaging
                """.trimIndent()

                var intro = ""
                RunAnywhere.generateStream(prompt).collect { token ->
                    intro += token
                    _studyMessages.value = listOf(StudyMessage.StreamingAI(intro))
                }

                // Speak the intro in mentor's voice, if enabled
                if (_isVoiceEnabled.value && intro.isNotBlank()) {
                    val mentorVoice = _currentMentor.value.voiceId
                    voiceHandler.speak(intro, mentorVoice)
                }

                // Save this interaction to memory
                val sentiment = SentimentAnalyzer.analyzeSentiment("starting $subject")
                memoryStorage.addMemorySnapshot(
                    MemorySnapshot(
                        timestamp = System.currentTimeMillis(),
                        topic = subject,
                        userQuery = "Start learning $topics in $subject",
                        aiResponse = intro,
                        sentiment = sentiment
                    )
                )

                // Add learning style options
                _studyMessages.value += StudyMessage.LearningOptions(
                    subject = subject,
                    topics = topics,
                    options = LearningStyles.getAll()
                )

                // Update streak
                val userId = _currentUserId.value
                if (userId != null) {
                    firebaseRepo.updateStreak(userId)
                }

            } catch (e: Exception) {
                _studyMessages.value += StudyMessage.StreamingAI("Oops! Let's try again, Champ! 💪")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun selectLearningStyle(subject: String, topics: String, styleId: String) {
        android.util.Log.d("FirebaseStudyVM", "=== SELECT LEARNING STYLE START ===")
        android.util.Log.d("FirebaseStudyVM", "Style: $styleId, Subject: $subject, Topics: $topics")
        android.util.Log.d("FirebaseStudyVM", "Model Ready: ${_isModelReady.value}")
        
        viewModelScope.launch {
            try {
                _isGenerating.value = true
                android.util.Log.d("FirebaseStudyVM", "Set isGenerating = true")

                // ALWAYS show content immediately (fallback or AI-generated)
                // This ensures the user always sees something
                showFallbackContent(subject, topics, styleId)
                android.util.Log.d("FirebaseStudyVM", "Fallback content displayed")

                // After content, offer quiz and flashcards
                kotlinx.coroutines.delay(500) // Small delay to let content show
                _studyMessages.value += StudyMessage.StreamingAI(
                    "\n\n🎯 Ready to test your knowledge?\n\n📝 Type 'quiz' for a quiz\n🃏 Type 'flashcards' to practice\n\nOr ask me anything!"
                )
                android.util.Log.d("FirebaseStudyVM", "Quiz/Flashcard options added")

            } catch (e: Exception) {
                android.util.Log.e("FirebaseStudyVM", "Error in selectLearningStyle", e)
                _studyMessages.value += StudyMessage.StreamingAI(
                    "I'm ready to help you learn about $topics! Ask me anything or type 'quiz' or 'flashcards'!"
                )
            } finally {
                _isGenerating.value = false
                android.util.Log.d("FirebaseStudyVM", "=== SELECT LEARNING STYLE END ===")
            }
        }
    }

    private suspend fun generateStoryContent(subject: String, topics: String) {
        // Use AI Brain mentor personality
        val mentorPersonality = getAIBrainMentorPersonality()
        val localUserId = "local_user"
        val userContext = memoryStorage.loadUserContext(localUserId)

        // Build adaptive prompt
        val systemPrompt = if (userContext != null) {
            mentorPersonality.getSystemPrompt(userContext)
        } else {
            mentorPersonality.basePrompt
        }

        val prompt = """
$systemPrompt

CRITICAL: You MUST stay completely in character as ${_currentMentor.value.name}.
Your tone: ${_currentMentor.value.tone}
Your style: ${_currentMentor.value.style}

Now, explain $topics in $subject using your unique teaching approach:
- ${
            if (_currentMentor.value.id == "sensei") "Use philosophical metaphors and wisdom from nature"
            else if (_currentMentor.value.id == "mira") "Tell it as a magical fairy tale with characters and enchantment"
            else "Share it like a supportive friend, showing how we're learning together"
        }
- Keep it under 150 words
- Stay TRUE to your personality
        """.trimIndent()

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            updateStreamingMessage(response)
        }

        // Speak in mentor voice if enabled
        if (_isVoiceEnabled.value && response.isNotBlank()) {
            val mentorVoice = _currentMentor.value.voiceId
            voiceHandler.speak(response, mentorVoice)
        }

        // Save to memory
        val sentiment = SentimentAnalyzer.analyzeSentiment("story about $topics")
        memoryStorage.addMemorySnapshot(
            MemorySnapshot(
                timestamp = System.currentTimeMillis(),
                topic = subject,
                userQuery = "Explain $topics with a story",
                aiResponse = response,
                sentiment = sentiment
            )
        )
    }

    private suspend fun showResourcesContent(subject: String, topics: String) {
        // Check if internet is available
        val hasInternet = NetworkUtils.isInternetAvailable(getApplication())

        android.util.Log.d(
            "FirebaseStudyVM",
            "Fetching resources - Internet available: $hasInternet"
        )

        // Add mentor personality-specific intro to resources
        val mentorIntro = when (_currentMentor.value.id) {
            "sensei" -> "As your Sensei, wisdom is found in many sources. Here are resources for your journey:"
            "coach_max" -> "Alright champ, here's your Resource Playbook to help us win at $topics!"
            "mira" -> "✨ For our magical journey, let's gather resources from the enchanted library:"
            else -> "Here are some helpful resources!"
        }

        val resources = if (hasInternet) {
            // Fetch REAL resources from the internet
            try {
                android.util.Log.d("FirebaseStudyVM", "Fetching REAL online resources...")
                val fetchedResources = resourceFetcher.fetchResources(subject, topics)
                mentorIntro + "\n\n" + formatFetchedResources(fetchedResources)
            } catch (e: Exception) {
                android.util.Log.e("FirebaseStudyVM", "Failed to fetch online resources", e)
                mentorIntro + "\n\n" + buildResourcesList(subject, topics)
            }
        } else {
            // Use fallback curated resources
            android.util.Log.d("FirebaseStudyVM", "No internet - using curated resources")
            mentorIntro + "\n\n" + buildResourcesList(subject, topics)
        }

        _studyMessages.value += StudyMessage.StreamingAI(resources)

        // Speak resources content if enabled (remove emojis for speech)
        if (_isVoiceEnabled.value && resources.isNotBlank()) {
            val mentorVoice = _currentMentor.value.voiceId
            val cleanResources = removeEmojisForSpeech(resources)
            voiceHandler.speak(cleanResources, mentorVoice)
        }
    }

    private suspend fun generateDefinitions(subject: String, topics: String) {
        // Use AI Brain mentor personality
        val mentorPersonality = getAIBrainMentorPersonality()
        val localUserId = "local_user"
        val userContext = memoryStorage.loadUserContext(localUserId)

        // Build adaptive prompt
        val systemPrompt = if (userContext != null) {
            mentorPersonality.getSystemPrompt(userContext)
        } else {
            mentorPersonality.basePrompt
        }

        val prompt = """
$systemPrompt

CRITICAL: You MUST stay completely in character as ${_currentMentor.value.name}.
Your tone: ${_currentMentor.value.tone}
Your style: ${_currentMentor.value.style}

Now, define the 3 most important terms in $topics for $subject, using your characteristic phrases and style:
- Each definition should be 2-3 sentences, easy to understand, and fit your teaching personality
- Stay TRUE to your character throughout
        """.trimIndent()

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            updateStreamingMessage(response)
        }

        // Speak in mentor voice if enabled
        if (_isVoiceEnabled.value && response.isNotBlank()) {
            val mentorVoice = _currentMentor.value.voiceId
            voiceHandler.speak(response, mentorVoice)
        }

        // Save to memory
        val sentiment = SentimentAnalyzer.analyzeSentiment("definitions for $topics")
        memoryStorage.addMemorySnapshot(
            MemorySnapshot(
                timestamp = System.currentTimeMillis(),
                topic = subject,
                userQuery = "Define key terms in $topics",
                aiResponse = response,
                sentiment = sentiment
            )
        )
    }

    private suspend fun generateRoadmap(subject: String, topics: String) {
        // Use AI Brain mentor personality
        val mentorPersonality = getAIBrainMentorPersonality()
        val localUserId = "local_user"
        val userContext = memoryStorage.loadUserContext(localUserId)

        // Build adaptive prompt
        val systemPrompt = if (userContext != null) {
            mentorPersonality.getSystemPrompt(userContext)
        } else {
            mentorPersonality.basePrompt
        }

        val prompt = """
$systemPrompt

CRITICAL: Stay COMPLETELY in character as ${_currentMentor.value.name}.
Your tone: ${_currentMentor.value.tone}
Your style: ${_currentMentor.value.style}

Now, create a simple 3-week learning roadmap for $topics in $subject:
- List the key concepts for each week
- Present it in your signature style (e.g. philosophical wisdom, fairy tale, or coach energy)
- Make sure your character shines through
        """.trimIndent()

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            updateStreamingMessage(response)
        }

        // Speak in mentor voice if enabled
        if (_isVoiceEnabled.value && response.isNotBlank()) {
            val mentorVoice = _currentMentor.value.voiceId
            voiceHandler.speak(response, mentorVoice)
        }

        // Save to memory
        val sentiment = SentimentAnalyzer.analyzeSentiment("roadmap for $topics")
        memoryStorage.addMemorySnapshot(
            MemorySnapshot(
                timestamp = System.currentTimeMillis(),
                topic = subject,
                userQuery = "Create learning roadmap for $topics",
                aiResponse = response,
                sentiment = sentiment
            )
        )
    }

    /**
     * Fallback content when AI model is not available
     */
    private suspend fun showFallbackContent(subject: String, topics: String, styleId: String) {
        val mentor = _currentMentor.value

        val content = when (styleId) {
            "story" -> generateDetailedStoryContent(subject, topics, mentor)
            "definition" -> generateDetailedDefinitions(subject, topics, mentor)
            "roadmap" -> generateDetailedRoadmap(subject, topics, mentor)
            "resources" -> generateDetailedResources(subject, topics, mentor)
            else -> generateDetailedStoryContent(subject, topics, mentor)
        }

        // Add the content as a message
        _studyMessages.value += StudyMessage.StreamingAI(content)

        // Speak it in mentor's voice WITHOUT emojis
        if (_isVoiceEnabled.value) {
            val cleanContent = removeEmojisForSpeech(content)
            voiceHandler.speak(cleanContent, mentor.voiceId)
        }

        android.util.Log.d("FirebaseStudyVM", "Fallback content displayed and spoken")
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

    /**
     * Remove emojis and special characters from text for speech
     */
    private fun removeEmojisForSpeech(text: String): String {
        return text
            .replace(Regex("[\\p{So}\\p{Sk}]"), "") // Remove emoji symbols
            .replace("*", "") // Remove asterisks
            .replace("✨", "")
            .replace("🧙‍♂️", "")
            .replace("⚡", "")
            .replace("🧚‍♀️", "")
            .replace("📚", "")
            .replace("🎯", "")
            .replace("📝", "")
            .replace("🃏", "")
            .replace("💪", "")
            .replace("🔥", "")
            .replace("🌟", "")
            .replace("🏆", "")
            .replace("✓", "")
            .replace("  ", " ") // Remove double spaces
            .trim()
    }

    private fun generateDetailedStoryContent(subject: String, topics: String, mentor: MentorProfile): String {
        // Create topic-specific content based on common subjects
        val baseContent = when {
            topics.contains("Newton", ignoreCase = true) -> """
                Newton's Laws of Motion are three fundamental principles that describe how objects move and interact with forces.
                
                The First Law, also called the Law of Inertia, states that an object at rest stays at rest, and an object in motion stays in motion at constant velocity, unless acted upon by an external force. Think of a hockey puck sliding on ice - it keeps going until friction or something stops it.
                
                The Second Law gives us the famous equation F equals ma, meaning Force equals mass times acceleration. This tells us that the force needed to accelerate an object depends on both its mass and how quickly you want to speed it up.
                
                The Third Law states that for every action, there is an equal and opposite reaction. When you push against a wall, the wall pushes back with equal force. This is why rockets work - they push gas downward, and the gas pushes the rocket upward.
            """.trimIndent()
            
            topics.contains("Gravity", ignoreCase = true) || topics.contains("Gravitation", ignoreCase = true) -> """
                Gravity is the fundamental force of attraction between all objects with mass. Every object in the universe pulls on every other object.
                
                The Universal Law of Gravitation states that the gravitational force between two objects is proportional to the product of their masses and inversely proportional to the square of the distance between them. The formula is F equals G times m1 times m2 divided by r squared, where G is the gravitational constant.
                
                On Earth, gravity gives us an acceleration of 9.8 meters per second squared. This means that when you drop something, it accelerates downward at this rate. The farther you go from Earth, the weaker gravity becomes, following the inverse square law.
                
                Mass and weight are different - mass is the amount of matter in an object, while weight is the force of gravity acting on that mass. Your mass stays the same on the Moon, but your weight is only one-sixth of what it is on Earth because the Moon's gravity is weaker.
            """.trimIndent()
            
            subject.contains("Math", ignoreCase = true) || subject.contains("Algebra", ignoreCase = true) -> """
                $topics in mathematics involves understanding relationships between quantities and solving equations systematically.
                
                In algebra, we use variables like x and y to represent unknown values. The goal is to isolate the variable to find its value. For example, in the equation 2x plus 5 equals 15, we subtract 5 from both sides to get 2x equals 10, then divide by 2 to find x equals 5.
                
                Key concepts include: combining like terms, using the distributive property, factoring expressions, and working with exponents. The order of operations - Parentheses, Exponents, Multiplication and Division, Addition and Subtraction - helps us solve problems correctly.
                
                Understanding these fundamentals allows you to solve real-world problems, from calculating distances and speeds to managing finances and analyzing data patterns.
            """.trimIndent()
            
            else -> """
                $topics in $subject is a fundamental concept that builds the foundation for deeper understanding in this field.
                
                The key ideas include understanding the basic principles, recognizing patterns and relationships, and applying these concepts to solve problems. Each component connects to create a comprehensive framework.
                
                To master $topics, start with the foundational definitions, practice with examples, and gradually work toward more complex applications. Understanding why things work, not just how they work, leads to true mastery.
                
                This knowledge serves as a stepping stone to more advanced topics and practical applications in $subject.
            """.trimIndent()
        }
        
        // Add mentor-specific framing WITHOUT emojis in the spoken part
        return when (mentor.id) {
            "sensei" -> "As your Sensei, let me share wisdom about $topics.\n\n$baseContent\n\nContemplate these principles, for true understanding comes from reflection."
            "coach_max" -> "Alright champ, let's break down $topics together!\n\n$baseContent\n\nWe're building your knowledge foundation, one concept at a time. You've got this!"
            "mira" -> "Let me tell you about the fascinating world of $topics, dear friend.\n\n$baseContent\n\nIsn't learning wonderful when we understand the magic behind how things work?"
            else -> baseContent
        }
    }

    private fun generateDetailedDefinitions(subject: String, topics: String, mentor: MentorProfile): String {
        val definitions = when {
            topics.contains("Newton", ignoreCase = true) -> """
                Key Definitions for Newton's Laws:
                
                1. INERTIA: The tendency of an object to resist changes in its state of motion. Objects at rest want to stay at rest, and objects in motion want to keep moving at the same speed and direction. Mass is a measure of inertia - more massive objects have more inertia.
                
                2. FORCE: A push or pull acting on an object, measured in Newtons. Force causes acceleration and is calculated using F equals ma. Forces can be contact forces like friction and tension, or non-contact forces like gravity and magnetism.
                
                3. ACCELERATION: The rate of change of velocity over time, measured in meters per second squared. Acceleration occurs when an object speeds up, slows down, or changes direction. It is directly proportional to the net force and inversely proportional to mass.
                
                4. MASS: The amount of matter in an object, measured in kilograms. Mass is a scalar quantity that doesn't change with location. It determines both the inertia of an object and how much it's affected by gravitational force.
                
                5. ACTION-REACTION PAIR: Two forces that are equal in magnitude, opposite in direction, and act on different objects. These pairs always occur together - you cannot have an action without a reaction.
            """.trimIndent()
            
            topics.contains("Gravity", ignoreCase = true) -> """
                Key Definitions for Gravity and Gravitation:
                
                1. GRAVITATIONAL FORCE: The attractive force between any two objects with mass. Every object in the universe attracts every other object. The force depends on the masses of the objects and the distance between them.
                
                2. GRAVITATIONAL CONSTANT (G): A universal constant equal to 6.67 times 10 to the negative 11 Newton meter squared per kilogram squared. This constant appears in Newton's Law of Universal Gravitation and is the same everywhere in the universe.
                
                3. WEIGHT: The force of gravity acting on an object's mass, calculated as weight equals mass times gravitational acceleration. Weight varies with location - you weigh less on the Moon than on Earth because the Moon's gravity is weaker.
                
                4. GRAVITATIONAL FIELD: The region around a mass where its gravitational force can be detected. The strength of the field decreases with distance following the inverse square law. Earth's gravitational field strength at the surface is 9.8 Newtons per kilogram.
                
                5. ESCAPE VELOCITY: The minimum speed needed for an object to break free from a celestial body's gravitational pull without further propulsion. For Earth, this is about 11.2 kilometers per second or 25,000 miles per hour.
            """.trimIndent()
            
            subject.contains("Math", ignoreCase = true) -> """
                Key Definitions for $topics:
                
                1. VARIABLE: A symbol, usually a letter, that represents an unknown or changing value. Variables allow us to write general formulas and solve for unknown quantities. Common variables include x, y, and z.
                
                2. EQUATION: A mathematical statement showing that two expressions are equal. Equations contain an equals sign and can be solved to find the value of variables. Example: 3x plus 7 equals 22.
                
                3. EXPRESSION: A combination of numbers, variables, and operations that represents a value. Unlike equations, expressions don't have an equals sign. Example: 2x squared minus 5x plus 3.
                
                4. COEFFICIENT: The numerical factor of a term containing a variable. In the term 5x, the coefficient is 5. Coefficients tell us how many times the variable is being multiplied.
                
                5. CONSTANT: A value that doesn't change. In the equation y equals 2x plus 3, the number 3 is a constant. Constants remain the same regardless of the variable's value.
            """.trimIndent()
            
            else -> """
                Key Definitions for $topics in $subject:
                
                1. FUNDAMENTAL CONCEPT: The core principle underlying $topics. This forms the foundation for understanding more advanced ideas. It connects theory with practical application.
                
                2. KEY TERMINOLOGY: Important terms specific to $topics that you need to know. These words have precise meanings in $subject and are essential for clear communication.
                
                3. RELATIONSHIP: How different elements in $topics connect and influence each other. Understanding these connections helps you see the bigger picture and solve complex problems.
                
                4. APPLICATION: How $topics is used in real-world scenarios or to solve practical problems. This bridges the gap between theory and practice.
                
                5. PRINCIPLE: A fundamental truth or rule that governs $topics. Principles guide how we approach problems and make predictions in $subject.
            """.trimIndent()
        }
        
        return when (mentor.id) {
            "sensei" -> "Young scholar, meditate on these essential definitions for $topics:\n\n$definitions\n\nKnowledge begins with understanding the language of wisdom."
            "coach_max" -> "Hey champ! Here are the key terms you need to master for $topics:\n\n$definitions\n\nKnow these definitions and you're already winning!"
            "mira" -> "Dear friend, let me reveal the meanings behind the magic of $topics:\n\n$definitions\n\nEach definition is like a key unlocking deeper understanding."
            else -> definitions
        }
    }

    private fun generateDetailedRoadmap(subject: String, topics: String, mentor: MentorProfile): String {
        val roadmap = """
            3-Week Learning Roadmap for $topics in $subject:
            
            WEEK 1: Foundations and Core Concepts
            - Day 1-2: Understand basic definitions and terminology
            - Day 3-4: Learn the fundamental principles and laws
            - Day 5-6: Study examples and simple applications
            - Day 7: Review and practice basic problems
            Goal: Build a solid foundation of understanding
            
            WEEK 2: Application and Problem Solving
            - Day 8-9: Work through practice problems step by step
            - Day 10-11: Apply concepts to real-world scenarios
            - Day 12-13: Learn problem-solving strategies and shortcuts
            - Day 14: Take practice quizzes to test understanding
            Goal: Develop confidence in applying knowledge
            
            WEEK 3: Mastery and Advanced Understanding
            - Day 15-16: Explore connections to related topics
            - Day 17-18: Tackle challenging problems and edge cases
            - Day 19-20: Review everything and identify weak areas
            - Day 21: Final comprehensive assessment
            Goal: Achieve mastery and deep understanding
            
            Study Tips:
            - Spend 30-45 minutes per day for consistent progress
            - Practice problems immediately after learning concepts
            - Teach the material to someone else to reinforce learning
            - Use flashcards for memorizing key facts and formulas
            - Don't move forward until you understand current material
        """.trimIndent()
        
        return when (mentor.id) {
            "sensei" -> "The path to mastery is a journey of patient steps. Here is your learning path for $topics:\n\n$roadmap\n\nAs water shapes stone through persistence, so shall you shape your understanding through steady practice."
            "coach_max" -> "Alright champ! Here's our winning game plan for mastering $topics:\n\n$roadmap\n\nStick to this schedule and we'll crush this together! One day at a time, one victory at a time!"
            "mira" -> "Come, dear friend! Let me show you the magical journey through $topics:\n\n$roadmap\n\nEach week brings new wonders and discoveries. Trust the process and enjoy the adventure!"
            else -> roadmap
        }
    }

    private suspend fun generateDetailedResources(
        subject: String,
        topics: String,
        mentor: MentorProfile
    ): String {
        // Check if internet is available
        val hasInternet = NetworkUtils.isInternetAvailable(getApplication())

        val resources = if (hasInternet) {
            // Fetch REAL resources from the internet
            try {
                android.util.Log.d("FirebaseStudyVM", "Fetching REAL detailed resources online...")
                val fetchedResources = resourceFetcher.fetchResources(subject, topics)
                formatDetailedFetchedResources(fetchedResources, subject, topics)
            } catch (e: Exception) {
                android.util.Log.e("FirebaseStudyVM", "Failed to fetch detailed resources", e)
                getDefaultDetailedResources(subject, topics)
            }
        } else {
            // Use fallback curated resources
            android.util.Log.d("FirebaseStudyVM", "No internet - using default detailed resources")
            getDefaultDetailedResources(subject, topics)
        }

        return when (mentor.id) {
            "sensei" -> "Wise scholars seek knowledge from many sources. Here are paths to deepen your understanding of $topics:\n\n$resources\n\nTrue wisdom comes not from a single source, but from the synthesis of many teachings."
            "coach_max" -> "Hey champ! Here's your resource playbook for dominating $topics:\n\n$resources\n\nThese tools will help us train and level up your skills. Let's use them all!"
            "mira" -> "Let me show you the treasures of knowledge for learning $topics, dear friend:\n\n$resources\n\nEach resource is a magical scroll containing wisdom. Explore them and watch your understanding grow!"
            else -> resources
        }
    }

    private fun formatDetailedFetchedResources(
        resources: ResourceFetcherService.ResourceCollection,
        subject: String,
        topics: String
    ): String {
        val sb = StringBuilder()
        sb.append("Recommended Resources for $topics in $subject:\n\n")

        // Video Resources
        sb.append("VIDEO RESOURCES:\n")
        if (resources.videos.isNotEmpty()) {
            resources.videos.forEach { resource ->
                sb.append("- ${resource.title}\n")
                sb.append("  ${resource.url}\n")
                sb.append("  ${resource.description}\n\n")
            }
        }

        // Reading Materials
        sb.append("READING MATERIALS:\n")
        if (resources.articles.isNotEmpty()) {
            resources.articles.forEach { resource ->
                sb.append("- ${resource.title}\n")
                sb.append("  ${resource.url}\n")
                sb.append("  ${resource.description}\n\n")
            }
        }

        // Online Courses
        sb.append("ONLINE COURSES:\n")
        if (resources.courses.isNotEmpty()) {
            resources.courses.forEach { resource ->
                sb.append("- ${resource.title}\n")
                sb.append("  ${resource.url}\n")
                sb.append("  ${resource.description}\n\n")
            }
        }

        // Practice Platforms
        sb.append("PRACTICE PLATFORMS & TOOLS:\n")
        if (resources.practice.isNotEmpty()) {
            resources.practice.forEach { resource ->
                sb.append("- ${resource.title}\n")
                sb.append("  ${resource.url}\n")
                sb.append("  ${resource.description}\n\n")
            }
        }

        // Add study strategies
        sb.append("STUDY STRATEGIES:\n")
        sb.append("- Create your own summary notes after each lesson\n")
        sb.append("- Form study groups to discuss concepts\n")
        sb.append("- Use the Feynman Technique: Explain concepts in simple terms\n")
        sb.append("- Practice spaced repetition for long-term retention\n")
        sb.append("- Test yourself regularly with quizzes and problems\n")

        return sb.toString()
    }

    private fun getDefaultDetailedResources(subject: String, topics: String): String {
        return """
            Recommended Resources for $topics in $subject:
            
            VIDEO RESOURCES:
            - Khan Academy: Search for "$topics" for step-by-step video lessons
            - YouTube channels: 3Blue1Brown (Math), CrashCourse (Science), Professor Dave Explains
            - MIT OpenCourseWare: Free university-level lectures
            
            READING MATERIALS:
            - Wikipedia: "$topics" for comprehensive overview
            - OpenStax: Free digital textbooks for $subject
            - Google Scholar: Academic papers and research articles
            
            PRACTICE PLATFORMS:
            - Brilliant.org: Interactive problem-solving
            - Quizlet: Flashcards created by other students
            - Practice worksheets available online
            
            INTERACTIVE TOOLS:
            - Desmos (for Math): Visual graphing calculator
            - PhET Simulations (for Science): Interactive simulations
            - Wolfram Alpha: Computational knowledge engine
            
            STUDY STRATEGIES:
            - Create your own summary notes after each lesson
            - Form study groups to discuss concepts
            - Use the Feynman Technique: Explain concepts in simple terms
            - Practice spaced repetition for long-term retention
            - Test yourself regularly with quizzes and problems
        """.trimIndent()
    }

    /**
     * Format fetched resources into a readable string with clickable links
     */
    private fun formatFetchedResources(resources: ResourceFetcherService.ResourceCollection): String {
        val sb = StringBuilder()

        // Video Resources
        if (resources.videos.isNotEmpty()) {
            sb.append("🎥 **Video Learning**\n")
            resources.videos.take(4).forEach { resource ->
                sb.append("   • ${resource.title}\n")
                sb.append("     ${resource.url}\n")
                sb.append("     ${resource.description}\n\n")
            }
        }

        // Article Resources
        if (resources.articles.isNotEmpty()) {
            sb.append("📖 **Reading Materials**\n")
            resources.articles.take(4).forEach { resource ->
                sb.append("   • ${resource.title}\n")
                sb.append("     ${resource.url}\n")
                sb.append("     ${resource.description}\n\n")
            }
        }

        // Course Resources
        if (resources.courses.isNotEmpty()) {
            sb.append("📚 **Online Courses**\n")
            resources.courses.take(4).forEach { resource ->
                sb.append("   • ${resource.title}\n")
                sb.append("     ${resource.url}\n")
                sb.append("     ${resource.description}\n\n")
            }
        }

        // Practice Resources
        if (resources.practice.isNotEmpty()) {
            sb.append("✍️ **Practice & Tools**\n")
            resources.practice.take(5).forEach { resource ->
                sb.append("   • ${resource.title}\n")
                sb.append("     ${resource.url}\n")
                sb.append("     ${resource.description}\n\n")
            }
        }

        sb.append("\n💡 Tap any URL to open it in your browser!\n")
        sb.append("\n🎯 Ready to test your knowledge? Type 'quiz' or 'flashcards'!")

        return sb.toString().trim()
    }

    private fun buildResourcesList(subject: String, topics: String): String {
        return """
📚 Resources for $topics in $subject:

🎥 **Video Learning**
   • Search YouTube for "$topics $subject tutorial"
   • Khan Academy
   • Crash Course

📖 **Reading Materials**
   • Wikipedia: $topics
   • Google Scholar articles
   
✍️ **Practice**
   • Practice problems and exercises
   • Online quizzes

Would you like to start with a quiz or flashcards? Just ask! 😊
        """.trimIndent()
    }

    fun askFollowUpQuestion(question: String) {
        if (!_isModelReady.value) {
            android.util.Log.d("FirebaseStudyVM", "Model not ready for question: $question")
            return
        }

        // Check for quiz/flashcard triggers
        val lowerQuestion = question.lowercase()
        if ("quiz" in lowerQuestion || "test" in lowerQuestion) {
            val subject = _currentSubject.value
            val topic = _currentTopic.value

            android.util.Log.d(
                "FirebaseStudyVM",
                "Quiz requested - Subject: '$subject', Topic: '$topic'"
            )

            if (subject.isNotBlank() && topic.isNotBlank()) {
                generateQuiz(subject, topic)
                return
            } else {
                // Generate a general quiz with fallback
                android.util.Log.d("FirebaseStudyVM", "No subject/topic - generating fallback quiz")
                generateQuiz("General Knowledge", "Study Topics")
                return
            }
        }
        if ("flashcard" in lowerQuestion || "cards" in lowerQuestion) {
            val subject = _currentSubject.value
            val topic = _currentTopic.value

            android.util.Log.d(
                "FirebaseStudyVM",
                "Flashcards requested - Subject: '$subject', Topic: '$topic'"
            )

            if (subject.isNotBlank() && topic.isNotBlank()) {
                generateFlashcards(subject, topic)
                return
            } else {
                // Generate fallback flashcards
                android.util.Log.d(
                    "FirebaseStudyVM",
                    "No subject/topic - generating fallback flashcards"
                )
                generateFlashcards("General Knowledge", "Study Topics")
                return
            }
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _studyMessages.value += StudyMessage.UserInput(question)

            try {
                // Use AI Brain with adaptive personality
                val mentorPersonality = getAIBrainMentorPersonality()
                val localUserId = "local_user"
                val currentTopic = _currentTopic.value.ifEmpty { _currentSubject.value }

                // Build context-aware prompt with memory
                val contextualPrompt = memoryContextBuilder.buildContextualPrompt(
                    userId = localUserId,
                    currentTopic = currentTopic,
                    userQuery = question,
                    basePrompt = mentorPersonality.basePrompt
                )

                android.util.Log.d("AIBrain", "Using contextual prompt with memory")

                var aiResponse = ""
                RunAnywhere.generateStream(contextualPrompt).collect { token ->
                    aiResponse += token
                    updateStreamingMessage(aiResponse)
                }

                // Speak the AI response in mentor's voice, if enabled
                if (_isVoiceEnabled.value && aiResponse.isNotBlank()) {
                    val mentorVoice = _currentMentor.value.voiceId ?: "default"
                    voiceHandler.speak(aiResponse, mentorVoice)
                }

                // Analyze sentiment and save to memory
                val sentiment = SentimentAnalyzer.analyzeSentiment(question)
                memoryStorage.addMemorySnapshot(
                    MemorySnapshot(
                        timestamp = System.currentTimeMillis(),
                        topic = currentTopic,
                        userQuery = question,
                        aiResponse = aiResponse,
                        sentiment = sentiment
                    )
                )

                // Update mood based on recent interactions
                memoryStorage.inferMoodFromInteractions(localUserId)

                android.util.Log.d("AIBrain", "Saved interaction - Sentiment: $sentiment")

            } catch (e: Exception) {
                _studyMessages.value += StudyMessage.StreamingAI("Oops! Something went wrong, Champ. ")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun resetJourney() {
        _studyMessages.value = emptyList()
        _currentQuiz.value = null
        _currentFlashcards.value = null

        // End study session
        currentSessionId?.let { sessionId ->
            viewModelScope.launch {
                val profile = _userProfile.value
                firebaseRepo.endStudySession(sessionId, profile?.totalXP ?: 0)
                currentSessionId = null
            }
        }
    }

    // ===== YOUTUBE INTEGRATION =====

    /**
     * Search for YouTube videos and channels based on current topic
     */
    fun searchYouTubeContent(topic: String, subject: String = "") {
        viewModelScope.launch {
            _isYouTubeLoading.value = true
            _youtubeVideos.value = emptyList()
            _youtubeChannels.value = emptyList()

            try {
                android.util.Log.d("YouTubeVM", "Searching YouTube for: $topic in $subject")

                // Search videos
                val videosResult = youtubeRepo.searchVideos(topic, subject)
                if (videosResult.isSuccess) {
                    _youtubeVideos.value = videosResult.getOrNull() ?: emptyList()
                    android.util.Log.d("YouTubeVM", "Found ${_youtubeVideos.value.size} videos")
                } else {
                    android.util.Log.e(
                        "YouTubeVM",
                        "Video search failed: ${videosResult.exceptionOrNull()?.message}"
                    )
                }

                // Search channels
                val channelsResult = youtubeRepo.searchChannels(topic, subject)
                if (channelsResult.isSuccess) {
                    _youtubeChannels.value = channelsResult.getOrNull() ?: emptyList()
                    android.util.Log.d("YouTubeVM", "Found ${_youtubeChannels.value.size} channels")
                } else {
                    android.util.Log.e(
                        "YouTubeVM",
                        "Channel search failed: ${channelsResult.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e("YouTubeVM", "YouTube search exception", e)
                _statusMessage.value = "Failed to load YouTube content: ${e.message}"
            } finally {
                _isYouTubeLoading.value = false
            }
        }
    }

    /**
     * Refresh YouTube content with current subject/topic
     */
    fun refreshYouTubeContent() {
        val subject = _currentSubject.value
        val topic = _currentTopic.value
        if (topic.isNotEmpty()) {
            searchYouTubeContent(topic, subject)
        }
    }

    /**
     * Clear YouTube search results
     */
    fun clearYouTubeContent() {
        _youtubeVideos.value = emptyList()
        _youtubeChannels.value = emptyList()
    }
}

// ViewModelFactory for FirebaseStudyViewModel
class FirebaseStudyViewModelFactory(private val application: Application) :
    ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return FirebaseStudyViewModel(application) as T
    }
}
