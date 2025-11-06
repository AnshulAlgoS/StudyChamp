package com.runanywhere.startup_hackathon20.repository

import android.content.Context
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.startup_hackathon20.*
import com.runanywhere.startup_hackathon20.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class GamificationRepository(context: Context) {
    private val database = StudyChampDatabase.getDatabase(context)
    private val userProgressDao = database.userProgressDao()
    private val achievementDao = database.achievementDao()
    private val quizHistoryDao = database.quizHistoryDao()
    private val flashcardProgressDao = database.flashcardProgressDao()
    private val cachedQuizDao = database.cachedQuizDao()
    private val cachedFlashcardsDao = database.cachedFlashcardsDao()

    private val json = Json { ignoreUnknownKeys = true }

    // ===== USER PROGRESS =====

    fun getUserProgress(): Flow<UserProgress?> = userProgressDao.getUserProgress()

    suspend fun getUserProgressSync(): UserProgress {
        return userProgressDao.getUserProgressSync() ?: UserProgress().also {
            userProgressDao.insertOrUpdate(it)
        }
    }

    suspend fun addXP(amount: Int, reason: String = ""): UserProgress {
        val current = getUserProgressSync()
        val newXP = current.totalXP + amount
        val newLevel = XPSystem.calculateLevel(newXP)

        val updated = current.copy(
            totalXP = newXP,
            level = newLevel
        )

        userProgressDao.update(updated)

        // Check for level-based achievements
        checkLevelAchievements(newLevel)

        Log.d(
            "GamificationRepo",
            "Added $amount XP. Reason: $reason. Total: $newXP, Level: $newLevel"
        )
        return updated
    }

    suspend fun updateStreak(): UserProgress {
        val current = getUserProgressSync()
        val today = System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1)
        val lastStudyDay = current.lastStudyDate / TimeUnit.DAYS.toMillis(1)

        val newStreak = when {
            lastStudyDay == today -> current.currentStreak // Same day
            lastStudyDay == today - 1 -> current.currentStreak + 1 // Consecutive day
            else -> 1 // Streak broken, reset
        }

        val updated = current.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(newStreak, current.longestStreak),
            lastStudyDate = System.currentTimeMillis()
        )

        userProgressDao.update(updated)

        // Check streak achievements
        checkStreakAchievements(newStreak)

        return updated
    }

    suspend fun selectMentor(mentorId: String) {
        val current = getUserProgressSync()
        userProgressDao.update(current.copy(selectedMentor = mentorId))
    }

    suspend fun incrementTopicsCompleted() {
        val current = getUserProgressSync()
        val newCount = current.totalTopicsCompleted + 1
        userProgressDao.update(current.copy(totalTopicsCompleted = newCount))

        // Check topic-based achievements
        if (newCount >= 5) unlockAchievement("concept_conqueror")
    }

    // ===== ACHIEVEMENTS =====

    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()

    suspend fun initializeAchievements() {
        // Insert all predefined achievements if not exists
        achievementDao.insertAll(AchievementDefinitions.getAll())
    }

    suspend fun unlockAchievement(achievementId: String): Boolean {
        val achievement = achievementDao.getAchievementById(achievementId) ?: return false

        if (achievement.isUnlocked) return false

        val unlocked = achievement.copy(
            isUnlocked = true,
            unlockedAt = System.currentTimeMillis()
        )
        achievementDao.update(unlocked)

        // Award XP for achievement
        addXP(achievement.xpReward, "Achievement: ${achievement.title}")

        Log.d("GamificationRepo", "🏆 Achievement unlocked: ${achievement.title}")
        return true
    }

    private suspend fun checkLevelAchievements(level: Int) {
        when (level) {
            5 -> unlockAchievement("level_5")
            10 -> unlockAchievement("level_10")
        }
    }

    private suspend fun checkStreakAchievements(streak: Int) {
        when {
            streak >= 7 -> {
                unlockAchievement("streak_7")
                unlockAchievement("perfect_week")
            }

            streak >= 3 -> unlockAchievement("streak_3")
        }
    }

    // ===== QUIZ SYSTEM =====

    fun getAllQuizHistory(): Flow<List<QuizHistory>> = quizHistoryDao.getAllQuizHistory()

    suspend fun generateQuiz(subject: String, topic: String, mentorTone: String): QuizData {
        val topicKey = "${subject}_${topic}".replace(" ", "_")

        // Check cache first
        val cached = cachedQuizDao.getCachedQuiz(topicKey)
        if (cached != null) {
            Log.d("GamificationRepo", "📚 Using cached quiz for $topicKey")
            return json.decodeFromString(cached.quizJson)
        }

        // Generate with AI
        val prompt =
            """Generate exactly 5 multiple-choice questions about "$topic" in $subject. Be $mentorTone in tone.

Format your response EXACTLY like this JSON structure (no extra text):
{
  "topic": "$topic",
  "questions": [
    {
      "question": "What is the value of G in the universal law of gravitation?",
      "options": ["6.67×10^-11 N·m²/kg²", "9.8 m/s²", "3×10^8 m/s", "1.6×10^-19 C"],
      "answer": "6.67×10^-11 N·m²/kg²",
      "hint": "G is the universal constant, not acceleration!"
    }
  ]
}

Make exactly 5 questions. Each question must have exactly 4 options. Make hints motivating and helpful. Output ONLY valid JSON."""

        Log.d("GamificationRepo", "🤖 Generating quiz with AI...")

        try {
            var aiResponse = ""
            RunAnywhere.generateStream(prompt).collect { token ->
                aiResponse += token
            }

            Log.d("GamificationRepo", "AI Response length: ${aiResponse.length}")

            // Extract JSON from response
            val jsonStart = aiResponse.indexOf('{')
            val jsonEnd = aiResponse.lastIndexOf('}') + 1

            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                Log.e("GamificationRepo", "No valid JSON found in AI response")
                return getFallbackQuiz(subject, topic)
            }

            val jsonString = aiResponse.substring(jsonStart, jsonEnd)
            val quizData = json.decodeFromString<QuizData>(jsonString)

            // Cache the quiz
            cachedQuizDao.insert(
                CachedQuiz(
                    topicKey = topicKey,
                    quizJson = jsonString,
                    generatedAt = System.currentTimeMillis(),
                    subject = subject,
                    topic = topic
                )
            )

            Log.d(
                "GamificationRepo",
                "✅ Quiz generated successfully: ${quizData.questions.size} questions"
            )
            return quizData

        } catch (e: Exception) {
            Log.e("GamificationRepo", "❌ Error generating quiz: ${e.message}", e)
            return getFallbackQuiz(subject, topic)
        }
    }

    private fun getFallbackQuiz(subject: String, topic: String): QuizData {
        return QuizData(
            topic = topic,
            questions = listOf(
                QuizQuestion(
                    question = "This is a practice question about $topic. Which concept is most important?",
                    options = listOf(
                        "Understanding fundamentals",
                        "Memorizing formulas",
                        "Skipping examples",
                        "Avoiding practice"
                    ),
                    answer = "Understanding fundamentals",
                    hint = "Great effort, Champ! Always build on the basics first."
                ),
                QuizQuestion(
                    question = "What's the best way to master $topic in $subject?",
                    options = listOf(
                        "Practice consistently",
                        "Study once before exam",
                        "Skip difficult parts",
                        "Copy from others"
                    ),
                    answer = "Practice consistently",
                    hint = "You're on the right track! Consistency is key to mastery."
                ),
                QuizQuestion(
                    question = "When learning $topic, what should you do first?",
                    options = listOf(
                        "Understand the basic concept",
                        "Jump to advanced problems",
                        "Skip the introduction",
                        "Avoid asking questions"
                    ),
                    answer = "Understand the basic concept",
                    hint = "Nice try! Always start with understanding the basics."
                ),
                QuizQuestion(
                    question = "How can you check if you truly understand $topic?",
                    options = listOf(
                        "Teach it to someone else",
                        "Read it once",
                        "Watch a video",
                        "Copy notes"
                    ),
                    answer = "Teach it to someone else",
                    hint = "Good thinking! Teaching others is the ultimate test of understanding."
                ),
                QuizQuestion(
                    question = "What mindset helps most when studying $topic?",
                    options = listOf(
                        "Curious and persistent",
                        "Worried about grades",
                        "Rushed and anxious",
                        "Comparing with others"
                    ),
                    answer = "Curious and persistent",
                    hint = "That's the spirit! Stay curious and keep pushing forward, Champ!"
                )
            )
        )
    }

    suspend fun saveQuizResult(
        subject: String,
        topic: String,
        totalQuestions: Int,
        correctAnswers: Int,
        mentor: String
    ) {
        val score = correctAnswers.toFloat() / totalQuestions
        val xpEarned = when {
            score >= 1.0f -> 100 // Perfect score
            score >= 0.8f -> 75  // Great
            score >= 0.6f -> 50  // Good
            else -> 25           // Participation
        }

        // Save to history
        quizHistoryDao.insert(
            QuizHistory(
                topic = topic,
                subject = subject,
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                xpEarned = xpEarned,
                completedAt = System.currentTimeMillis(),
                mentorUsed = mentor
            )
        )

        // Award XP
        addXP(xpEarned, "Quiz: $topic ($correctAnswers/$totalQuestions)")

        // Update user progress
        val current = getUserProgressSync()
        userProgressDao.update(current.copy(totalQuizzesCompleted = current.totalQuizzesCompleted + 1))

        // Check achievements
        val totalQuizzes = quizHistoryDao.getTotalQuizzesCompleted()
        if (totalQuizzes == 1) unlockAchievement("first_quiz")
        if (totalQuizzes >= 10) unlockAchievement("quiz_marathon")
        if (score >= 1.0f) unlockAchievement("problem_solver")

        // Update streak
        updateStreak()
    }

    // ===== FLASHCARD SYSTEM =====

    fun getAllFlashcardProgress(): Flow<List<FlashcardProgress>> =
        flashcardProgressDao.getAllProgress()

    suspend fun generateFlashcards(
        subject: String,
        topic: String,
        mentorTone: String
    ): FlashcardSet {
        val topicKey = "${subject}_${topic}".replace(" ", "_")

        // Check cache
        val cached = cachedFlashcardsDao.getCachedFlashcards(topicKey)
        if (cached != null) {
            Log.d("GamificationRepo", "🃏 Using cached flashcards for $topicKey")
            return json.decodeFromString(cached.flashcardsJson)
        }

        // Generate with AI
        val prompt =
            """Generate exactly 5 flashcards for studying "$topic" in $subject. Be $mentorTone in tone.

Format your response EXACTLY like this JSON structure (no extra text):
{
  "topic": "$topic",
  "cards": [
    {
      "term": "Gravitational Constant (G)",
      "definition": "The universal constant in Newton's law of gravitation with value 6.67×10^-11 N·m²/kg². It determines the strength of gravitational force between two masses.",
      "isMastered": false
    }
  ]
}

Make exactly 5 flashcards. Each should have a clear term and a helpful definition (2-3 sentences). Output ONLY valid JSON."""

        Log.d("GamificationRepo", "🤖 Generating flashcards with AI...")

        try {
            var aiResponse = ""
            RunAnywhere.generateStream(prompt).collect { token ->
                aiResponse += token
            }

            // Extract JSON
            val jsonStart = aiResponse.indexOf('{')
            val jsonEnd = aiResponse.lastIndexOf('}') + 1

            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                Log.e("GamificationRepo", "No valid JSON found in AI response")
                return getFallbackFlashcards(subject, topic)
            }

            val jsonString = aiResponse.substring(jsonStart, jsonEnd)
            val flashcardSet = json.decodeFromString<FlashcardSet>(jsonString)

            // Cache
            cachedFlashcardsDao.insert(
                CachedFlashcards(
                    topicKey = topicKey,
                    flashcardsJson = jsonString,
                    generatedAt = System.currentTimeMillis(),
                    subject = subject,
                    topic = topic
                )
            )

            Log.d("GamificationRepo", "✅ Flashcards generated: ${flashcardSet.cards.size} cards")
            return flashcardSet

        } catch (e: Exception) {
            Log.e("GamificationRepo", "❌ Error generating flashcards: ${e.message}", e)
            return getFallbackFlashcards(subject, topic)
        }
    }

    private fun getFallbackFlashcards(subject: String, topic: String): FlashcardSet {
        return FlashcardSet(
            topic = topic,
            cards = listOf(
                Flashcard(
                    term = "Key Concept 1",
                    definition = "The first fundamental concept in $topic involves understanding the basic principles and how they apply to $subject."
                ),
                Flashcard(
                    term = "Key Concept 2",
                    definition = "The second important idea builds on the first, adding more depth and practical applications to your understanding."
                ),
                Flashcard(
                    term = "Key Concept 3",
                    definition = "This concept connects the previous ideas together, showing how everything fits into the bigger picture."
                ),
                Flashcard(
                    term = "Common Mistake",
                    definition = "Students often struggle with $topic by rushing. Take time to understand each part before moving forward."
                ),
                Flashcard(
                    term = "Mastery Tip",
                    definition = "To truly master $topic, practice regularly, explain it to others, and apply it to real-world situations."
                )
            )
        )
    }

    suspend fun updateFlashcardProgress(
        subject: String,
        topic: String,
        totalCards: Int,
        masteredCards: Int
    ) {
        val id = "${subject}_${topic}".replace(" ", "_")
        val xpPerCard = 10
        val xpEarned = masteredCards * xpPerCard

        flashcardProgressDao.insertOrUpdate(
            FlashcardProgress(
                id = id,
                topic = topic,
                subject = subject,
                totalCards = totalCards,
                masteredCards = masteredCards,
                lastReviewedAt = System.currentTimeMillis(),
                xpEarned = xpEarned
            )
        )

        // Award XP for mastered cards
        if (masteredCards > 0) {
            addXP(xpPerCard, "Flashcard mastered: $topic")
        }

        // Check flashcard achievement
        val totalMastered = flashcardProgressDao.getTotalMasteredCards() ?: 0
        if (totalMastered >= 25) {
            unlockAchievement("flashcard_master")
        }

        // Update user progress
        val current = getUserProgressSync()
        userProgressDao.update(current.copy(totalFlashcardsCompleted = totalMastered))

        // Update streak
        updateStreak()
    }

    // ===== CLEANUP =====

    suspend fun cleanupOldCache() {
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        cachedQuizDao.deleteOldQuizzes(sevenDaysAgo)
        cachedFlashcardsDao.deleteOldFlashcards(sevenDaysAgo)
    }
}
