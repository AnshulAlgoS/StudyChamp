package com.runanywhere.startup_hackathon20

import androidx.room.*
import kotlinx.serialization.Serializable

/**
 * Gamification & Progress Tracking Models for StudyChamp
 */

// ===== USER PROGRESS ENTITY =====
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val userId: String = "default_user",
    val totalXP: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStudyDate: Long = 0L, // timestamp
    val selectedMentor: String = "Sensei",
    val totalTopicsCompleted: Int = 0,
    val totalQuizzesCompleted: Int = 0,
    val totalFlashcardsCompleted: Int = 0
)

// ===== ACHIEVEMENT ENTITY =====
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L
)

// ===== QUIZ HISTORY ENTITY =====
@Entity(tableName = "quiz_history")
data class QuizHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val subject: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val xpEarned: Int,
    val completedAt: Long,
    val mentorUsed: String
)

// ===== FLASHCARD PROGRESS ENTITY =====
@Entity(tableName = "flashcard_progress")
data class FlashcardProgress(
    @PrimaryKey val id: String, // topic-based ID
    val topic: String,
    val subject: String,
    val totalCards: Int,
    val masteredCards: Int,
    val lastReviewedAt: Long,
    val xpEarned: Int
)

// ===== CACHED QUIZ ENTITY =====
@Entity(tableName = "cached_quizzes")
data class CachedQuiz(
    @PrimaryKey val topicKey: String, // subject_topic combination
    val quizJson: String, // JSON string of QuizData
    val generatedAt: Long,
    val subject: String,
    val topic: String
)

// ===== CACHED FLASHCARD ENTITY =====
@Entity(tableName = "cached_flashcards")
data class CachedFlashcards(
    @PrimaryKey val topicKey: String,
    val flashcardsJson: String, // JSON string of FlashcardSet
    val generatedAt: Long,
    val subject: String,
    val topic: String
)

// ===== RUNTIME DATA MODELS (NOT ENTITIES) =====

@Serializable
data class QuizData(
    val topic: String,
    val questions: List<QuizQuestion>
)

@Serializable
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val answer: String,
    val hint: String
)

@Serializable
data class FlashcardSet(
    val topic: String,
    val cards: List<Flashcard>
)

@Serializable
data class Flashcard(
    val term: String,
    val definition: String,
    val isMastered: Boolean = false
)

// ===== MENTOR PROFILE =====
data class MentorProfile(
    val id: String,
    val name: String,
    val emoji: String,
    val style: String,
    val intro: String,
    val tone: String,
    val description: String,
    val color: String, // hex color for UI
    val voiceId: String = id // Voice ID for TTS (same as mentor id by default)
)

// ===== PREDEFINED MENTORS =====
object Mentors {
    val SENSEI = MentorProfile(
        id = "sensei",
        name = "Sensei",
        emoji = "🧙‍♂️",
        style = "philosophical",
        intro = "Welcome, young scholar. Consider this: The journey of a thousand miles begins with understanding a single step.",
        tone = "philosophical and contemplative, use wisdom, metaphors from nature and the cosmos",
        description = "Philosophical mentor who teaches through wisdom and deep reflection",
        color = "#7E22CE", // Purple
        voiceId = "sensei"
    )

    val COACH_MAX = MentorProfile(
        id = "coach_max",
        name = "Coach Max",
        emoji = "⚡",
        style = "friendly",
        intro = "Hey there, champ! I'm so excited to learn and grow with you on this journey together!",
        tone = "warm, friendly, and supportive - emphasize mutual growth and partnership",
        description = "Friendly growth buddy who learns alongside you",
        color = "#F59E0B", // Yellow/Gold
        voiceId = "coach_max"
    )

    val MIRA = MentorProfile(
        id = "mira",
        name = "Mira",
        emoji = "🧚‍♀️",
        style = "storyteller",
        intro = "✨ Hello, dear friend! Let me tell you a magical tale about the wonders we'll discover together! 🌟",
        tone = "magical fairy who tells enchanting stories, use fairy-tale language and whimsical imagery",
        description = "Magical fairy who transforms learning into enchanting adventures",
        color = "#14B8A6", // Teal
        voiceId = "mira"
    )

    fun getAll() = listOf(SENSEI, COACH_MAX, MIRA)

    fun getById(id: String) = getAll().find { it.id == id } ?: SENSEI
}

// ===== ACHIEVEMENT DEFINITIONS =====
object AchievementDefinitions {
    fun getAll() = listOf(
        Achievement(
            id = "first_quiz",
            title = "Quiz Rookie",
            description = "Complete your first quiz!",
            emoji = "📝",
            xpReward = 50,
            isUnlocked = false
        ),
        Achievement(
            id = "problem_solver",
            title = "Problem-Solving Pro",
            description = "Score 100% on any quiz",
            emoji = "🧠",
            xpReward = 100,
            isUnlocked = false
        ),
        Achievement(
            id = "concept_conqueror",
            title = "Concept Conqueror",
            description = "Complete 5 topics",
            emoji = "⚔️",
            xpReward = 150,
            isUnlocked = false
        ),
        Achievement(
            id = "streak_3",
            title = "3-Day Streak",
            description = "Study for 3 days in a row",
            emoji = "🔥",
            xpReward = 75,
            isUnlocked = false
        ),
        Achievement(
            id = "streak_7",
            title = "Streak Star",
            description = "Study for 7 days in a row!",
            emoji = "⭐",
            xpReward = 200,
            isUnlocked = false
        ),
        Achievement(
            id = "flashcard_master",
            title = "Flashcard Master",
            description = "Master 25 flashcards",
            emoji = "🃏",
            xpReward = 120,
            isUnlocked = false
        ),
        Achievement(
            id = "level_5",
            title = "Rising Star",
            description = "Reach Level 5",
            emoji = "🌟",
            xpReward = 250,
            isUnlocked = false
        ),
        Achievement(
            id = "level_10",
            title = "Champion Scholar",
            description = "Reach Level 10!",
            emoji = "🏆",
            xpReward = 500,
            isUnlocked = false
        ),
        Achievement(
            id = "quiz_marathon",
            title = "Quiz Marathon",
            description = "Complete 10 quizzes",
            emoji = "🎯",
            xpReward = 180,
            isUnlocked = false
        ),
        Achievement(
            id = "perfect_week",
            title = "Perfect Week",
            description = "Study every day for a week",
            emoji = "💎",
            xpReward = 300,
            isUnlocked = false
        )
    )
}

// ===== XP SYSTEM HELPERS =====
object XPSystem {
    private val levelThresholds = listOf(
        0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3200, 4000
    )

    fun calculateLevel(xp: Int): Int {
        for (level in 10 downTo 1) {
            if (xp >= levelThresholds[level - 1]) {
                return level
            }
        }
        return 1
    }

    fun xpForNextLevel(currentXP: Int, currentLevel: Int): Int {
        return if (currentLevel >= 10) 0
        else levelThresholds[currentLevel] - currentXP
    }

    fun progressToNextLevel(currentXP: Int, currentLevel: Int): Float {
        if (currentLevel >= 10) return 1f
        val currentThreshold = levelThresholds[currentLevel - 1]
        val nextThreshold = levelThresholds[currentLevel]
        val progress = currentXP - currentThreshold
        val total = nextThreshold - currentThreshold
        return (progress.toFloat() / total).coerceIn(0f, 1f)
    }
}
