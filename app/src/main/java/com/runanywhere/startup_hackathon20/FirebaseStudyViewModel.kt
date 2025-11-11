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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlin.Result

/**
 * Firebase-integrated StudyViewModel with complete gamification system + AI Brain
 */
class FirebaseStudyViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepo = FirebaseRepository()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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
            val currentUser = firebaseRepo.getCurrentUser()
            if (currentUser != null) {
                _currentUserId.value = currentUser.uid
                _isSignedIn.value = true
                loadUserProfile(currentUser.uid)
            } else {
                // Auto sign-in anonymously
                signInAnonymously()
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
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
                    _statusMessage.value = "Sign-in failed. Please try again."
                }
            }
        }
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
            _statusMessage.value =
                "Your mentor ${_currentMentor.value.name} is ready to guide you! "
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
                        question = "What does Newton's First Law state?",
                        options = listOf(
                            "An object at rest stays at rest unless acted upon by an external force",
                            "Force equals mass times acceleration",
                            "For every action there is an equal and opposite reaction",
                            "Objects fall at the same rate regardless of mass"
                        ),
                        answer = "An object at rest stays at rest unless acted upon by an external force",
                        hint = "This is about inertia - objects resist changes in motion! "
                    ),
                    QuizQuestion(
                        question = "What is the formula for Newton's Second Law?",
                        options = listOf(
                            "F = ma",
                            "E = mc²",
                            "v = u + at",
                            "W = Fd"
                        ),
                        answer = "F = ma",
                        hint = "Force equals mass times acceleration! "
                    ),
                    QuizQuestion(
                        question = "Newton's Third Law states that for every action, there is a:",
                        options = listOf(
                            "Equal and opposite reaction",
                            "Proportional force",
                            "Slower reaction",
                            "No reaction"
                        ),
                        answer = "Equal and opposite reaction",
                        hint = "Think about when you push against a wall - it pushes back! "
                    ),
                    QuizQuestion(
                        question = "If a 10kg object accelerates at 5 m/s², what force is applied?",
                        options = listOf(
                            "50 N",
                            "15 N",
                            "2 N",
                            "100 N"
                        ),
                        answer = "50 N",
                        hint = "Use F = ma. Multiply 10kg × 5m/s² = 50N! "
                    ),
                    QuizQuestion(
                        question = "Which law explains why seatbelts are important in cars?",
                        options = listOf(
                            "Newton's First Law (Inertia)",
                            "Newton's Second Law",
                            "Newton's Third Law",
                            "Law of Gravitation"
                        ),
                        answer = "Newton's First Law (Inertia)",
                        hint = "Your body wants to keep moving forward when the car stops! "
                    )
                )
            }

            topic.contains("Gravity", ignoreCase = true) || topic.contains(
                "Gravitation",
                ignoreCase = true
            ) -> {
                listOf(
                    QuizQuestion(
                        question = "What is the value of gravitational constant G?",
                        options = listOf(
                            "6.67 × 10⁻¹¹ N·m²/kg²",
                            "9.8 m/s²",
                            "3 × 10⁸ m/s",
                            "1.6 × 10⁻¹⁹ C"
                        ),
                        answer = "6.67 × 10⁻¹¹ N·m²/kg²",
                        hint = "This is the universal gravitational constant! "
                    ),
                    QuizQuestion(
                        question = "What is Earth's acceleration due to gravity?",
                        options = listOf(
                            "9.8 m/s²",
                            "6.67 m/s²",
                            "10 m/s²",
                            "8 m/s²"
                        ),
                        answer = "9.8 m/s²",
                        hint = "Objects fall at about 9.8 meters per second per second! "
                    ),
                    QuizQuestion(
                        question = "What happens to gravitational force if distance doubles?",
                        options = listOf(
                            "It becomes 1/4 of original",
                            "It doubles",
                            "It becomes half",
                            "It stays the same"
                        ),
                        answer = "It becomes 1/4 of original",
                        hint = "Gravity follows inverse square law! "
                    ),
                    QuizQuestion(
                        question = "Who discovered the Law of Universal Gravitation?",
                        options = listOf(
                            "Isaac Newton",
                            "Albert Einstein",
                            "Galileo Galilei",
                            "Stephen Hawking"
                        ),
                        answer = "Isaac Newton",
                        hint = "The story involves an apple falling from a tree! "
                    ),
                    QuizQuestion(
                        question = "Gravitational force depends on:",
                        options = listOf(
                            "Mass and distance between objects",
                            "Only mass",
                            "Only distance",
                            "Speed of objects"
                        ),
                        answer = "Mass and distance between objects",
                        hint = "Both factors matter in the formula F = G(m₁m₂)/r²! "
                    )
                )
            }

            subject.contains("Math", ignoreCase = true) || subject.contains(
                "Algebra",
                ignoreCase = true
            ) -> {
                listOf(
                    QuizQuestion(
                        question = "What is the value of x in the equation: 2x + 5 = 15?",
                        options = listOf(
                            "x = 5",
                            "x = 10",
                            "x = 7.5",
                            "x = 20"
                        ),
                        answer = "x = 5",
                        hint = "Subtract 5 from both sides, then divide by 2! "
                    ),
                    QuizQuestion(
                        question = "What is the formula for the area of a circle?",
                        options = listOf(
                            "πr²",
                            "2πr",
                            "πd",
                            "4πr²"
                        ),
                        answer = "πr²",
                        hint = "Pi times radius squared! "
                    ),
                    QuizQuestion(
                        question = "If y = 3x + 2, what is y when x = 4?",
                        options = listOf(
                            "14",
                            "12",
                            "10",
                            "16"
                        ),
                        answer = "14",
                        hint = "Substitute: y = 3(4) + 2 = 12 + 2 = 14! "
                    ),
                    QuizQuestion(
                        question = "What is the Pythagorean theorem?",
                        options = listOf(
                            "a² + b² = c²",
                            "a + b = c",
                            "a² = b² + c²",
                            "ab = c"
                        ),
                        answer = "a² + b² = c²",
                        hint = "For right triangles! "
                    ),
                    QuizQuestion(
                        question = "What is 15% of 200?",
                        options = listOf(
                            "30",
                            "15",
                            "45",
                            "20"
                        ),
                        answer = "30",
                        hint = "Multiply 200 × 0.15 = 30! "
                    )
                )
            }

            subject.contains("History", ignoreCase = true) -> {
                listOf(
                    QuizQuestion(
                        question = "In what year did World War II end?",
                        options = listOf(
                            "1945",
                            "1944",
                            "1946",
                            "1943"
                        ),
                        answer = "1945",
                        hint = "The war ended in the mid-1940s! "
                    ),
                    QuizQuestion(
                        question = "Who was the first President of the United States?",
                        options = listOf(
                            "George Washington",
                            "Thomas Jefferson",
                            "John Adams",
                            "Benjamin Franklin"
                        ),
                        answer = "George Washington",
                        hint = "He's on the one dollar bill! "
                    ),
                    QuizQuestion(
                        question = "The Renaissance began in which country?",
                        options = listOf(
                            "Italy",
                            "France",
                            "England",
                            "Spain"
                        ),
                        answer = "Italy",
                        hint = "Think of cities like Florence and Venice! "
                    ),
                    QuizQuestion(
                        question = "Who wrote the Declaration of Independence?",
                        options = listOf(
                            "Thomas Jefferson",
                            "George Washington",
                            "John Adams",
                            "Benjamin Franklin"
                        ),
                        answer = "Thomas Jefferson",
                        hint = "He was also the 3rd President! "
                    ),
                    QuizQuestion(
                        question = "The Industrial Revolution started in:",
                        options = listOf(
                            "Great Britain",
                            "United States",
                            "Germany",
                            "France"
                        ),
                        answer = "Great Britain",
                        hint = "It began in the 18th century! "
                    )
                )
            }

            else -> {
                // Generic but topic-specific questions
                listOf(
                    QuizQuestion(
                        question = "What is $topic primarily about in $subject?",
                        options = listOf(
                            "Core concepts and fundamental principles of $topic",
                            "Memorizing random facts",
                            "Only historical dates",
                            "Unrelated information"
                        ),
                        answer = "Core concepts and fundamental principles of $topic",
                        hint = "Focus on the main ideas that define $topic! "
                    ),
                    QuizQuestion(
                        question = "Which of these is a key component or principle in $topic?",
                        options = listOf(
                            "Understanding the fundamental relationships in $topic",
                            "Ignoring all examples",
                            "Avoiding practice",
                            "Random guessing"
                        ),
                        answer = "Understanding the fundamental relationships in $topic",
                        hint = "Think about what makes $topic work! "
                    ),
                    QuizQuestion(
                        question = "When applying $topic concepts, what's most important?",
                        options = listOf(
                            "Understanding how $topic principles work together",
                            "Just memorizing terms",
                            "Skipping foundational concepts",
                            "Avoiding real examples"
                        ),
                        answer = "Understanding how $topic principles work together",
                        hint = "Concepts in $topic connect to form a bigger picture! "
                    ),
                    QuizQuestion(
                        question = "What makes $topic relevant in $subject?",
                        options = listOf(
                            "It forms the foundation for understanding $subject",
                            "It has no real application",
                            "It's only theoretical with no use",
                            "It's completely optional"
                        ),
                        answer = "It forms the foundation for understanding $subject",
                        hint = "$topic is a building block in $subject! "
                    ),
                    QuizQuestion(
                        question = "To demonstrate understanding of $topic, you would need to:",
                        options = listOf(
                            "Explain key concepts and solve related problems in $topic",
                            "Only recite definitions",
                            "Avoid any practical application",
                            "Skip all examples"
                        ),
                        answer = "Explain key concepts and solve related problems in $topic",
                        hint = "True understanding comes from applying $topic knowledge! "
                    )
                )
            }
        }

        return QuizData(topic = topic, questions = questions)
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
        return FlashcardSet(
            topic = topic,
            cards = listOf(
                Flashcard(
                    term = "Key Concept",
                    definition = "The main idea or principle in $topic"
                )
            )
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
        viewModelScope.launch {
            try {
                _statusMessage.value = "Downloading your AI mentor... 📥"
                RunAnywhere.downloadModel(modelId).collect { progress ->
                    _downloadProgress.value = progress
                    _statusMessage.value = "Downloading: ${(progress * 100).toInt()}%"
                }
                _downloadProgress.value = null
                _statusMessage.value = "Download complete! Now tap 'Load' to activate "

                // Refresh model list to update UI with new download status
                loadAvailableModels()
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

                // Build adaptive prompt
                val systemPrompt = if (userContext != null) {
                    mentorPersonality.getSystemPrompt(userContext)
                } else {
                    mentorPersonality.basePrompt
                }

                val prompt = """
$systemPrompt

Now, write one motivating paragraph (3-4 sentences) about why learning $topics in $subject is exciting and useful.
Use your unique teaching style and personality.
                """.trimIndent()

                var intro = ""
                RunAnywhere.generateStream(prompt).collect { token ->
                    intro += token
                    _studyMessages.value = listOf(StudyMessage.StreamingAI(intro))
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
                    "\n\n🎯 Ready to test your knowledge, Champ?\n\n📝 Type 'quiz' to take a quiz\n🃏 Type 'flashcards' to practice with flashcards\n\nOr ask me any question you have!"
                )

            } catch (e: Exception) {
                _studyMessages.value += StudyMessage.StreamingAI("Error: ${e.message}")
            } finally {
                _isGenerating.value = false
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

Now, explain $topics in $subject using a simple real-world analogy or story.
Use your unique teaching style and personality. Keep it under 150 words.
        """.trimIndent()

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            updateStreamingMessage(response)
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
        val resources = buildResourcesList(subject, topics)
        _studyMessages.value += StudyMessage.StreamingAI(resources)
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

Now, define the 3 most important terms in $topics for $subject.
Use your unique teaching style. Each definition should be 2-3 sentences and easy to understand.
        """.trimIndent()

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            updateStreamingMessage(response)
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

Now, create a simple 3-week learning roadmap for $topics in $subject.
List key concepts for each week using your unique teaching style and personality.
        """.trimIndent()

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            updateStreamingMessage(response)
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
}

// ViewModelFactory for FirebaseStudyViewModel
class FirebaseStudyViewModelFactory(private val application: Application) :
    ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return FirebaseStudyViewModel(application) as T
    }
}
