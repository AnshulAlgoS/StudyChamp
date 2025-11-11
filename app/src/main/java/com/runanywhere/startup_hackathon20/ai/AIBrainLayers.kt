package com.runanywhere.startup_hackathon20.ai

import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable

// ===== COGNITIVE LAYER (The Model) =====

@Serializable
data class MentorPersonality(
    val id: String,
    val name: String,
    val basePrompt: String,
    val tone: String,
    val teachingStyle: String,
    val patience: Float = 0.5f,    // 0.0 - 1.0
    val humor: Float = 0.5f,       // 0.0 - 1.0
    val strictness: Float = 0.5f,  // 0.0 - 1.0
    val energy: Float = 0.5f       // 0.0 - 1.0
) {
    fun getSystemPrompt(userContext: UserLearningContext): String {
        val adaptedPrompt = buildString {
            appendLine(basePrompt)
            appendLine()
            appendLine("Current Student Context:")
            appendLine("- Learning Rate: ${userContext.learningRate}")
            appendLine("- Recent Performance: ${(userContext.recentAccuracy * 100).toInt()}%")
            appendLine("- Streak: ${userContext.streakDays} days")
            appendLine("- Mood Indicator: ${userContext.currentMood}")
            appendLine()
            appendLine("Adjust your teaching approach accordingly:")

            when {
                userContext.recentAccuracy < 0.6f -> {
                    appendLine("- Use ${getToneAdjustment("gentle")} and simpler explanations")
                    appendLine("- Provide more encouragement and examples")
                    appendLine("- Review fundamentals before advancing")
                }

                userContext.streakDays > 5 -> {
                    appendLine("- Use ${getToneAdjustment("motivational")} and celebrate progress")
                    appendLine("- Introduce challenging content")
                    appendLine("- Acknowledge their dedication")
                }

                userContext.learningRate == "fast" -> {
                    appendLine("- Move at a brisk pace")
                    appendLine("- Introduce advanced concepts")
                    appendLine("- Challenge with deeper questions")
                }

                else -> {
                    appendLine("- Maintain steady, balanced pace")
                    appendLine("- Mix review with new content")
                }
            }
        }
        return adaptedPrompt
    }

    private fun getToneAdjustment(desiredTone: String): String {
        return when (desiredTone) {
            "gentle" -> "extra patience and supportive language"
            "motivational" -> "energetic and inspiring language"
            "challenging" -> "direct and thought-provoking questions"
            else -> "your natural $tone style"
        }
    }

    companion object {
        fun getSensei(userContext: UserLearningContext): MentorPersonality {
            return MentorPersonality(
                id = "sensei",
                name = "Sensei",
                basePrompt = """
                    You are Sensei, a wise and patient AI mentor inspired by ancient wisdom traditions.
                    Your teaching philosophy: "The path to mastery is walked one mindful step at a time."
                    
                    Your characteristics:
                    - Calm, thoughtful, and reflective
                    - Use analogies from nature, martial arts, and life philosophy
                    - Teach through questions and guided discovery
                    - Encourage deep understanding over memorization
                    - Celebrate effort and growth mindset
                    
                    Always respond in a way that makes the student feel guided, not lectured.
                """.trimIndent(),
                tone = "calm and wise",
                teachingStyle = "Socratic questioning and analogies",
                patience = 0.9f,
                humor = 0.3f,
                strictness = 0.4f,
                energy = 0.5f
            )
        }

        fun getCoachMax(userContext: UserLearningContext): MentorPersonality {
            return MentorPersonality(
                id = "coach_max",
                name = "Coach Max",
                basePrompt = """
                    You are Coach Max, an energetic and motivational AI mentor who teaches like a champion coach.
                    Your motto: "Let's crush this challenge together, champion!"
                    
                    Your characteristics:
                    - High energy, enthusiastic, and encouraging
                    - Use sports metaphors and achievement language
                    - Frame learning as challenges and victories
                    - Push students to level up their skills
                    - Celebrate wins with excitement
                    
                    Make every lesson feel like training for greatness!
                """.trimIndent(),
                tone = "energetic and motivational",
                teachingStyle = "Challenge-based and goal-oriented",
                patience = 0.6f,
                humor = 0.7f,
                strictness = 0.6f,
                energy = 0.95f
            )
        }

        fun getMira(userContext: UserLearningContext): MentorPersonality {
            return MentorPersonality(
                id = "mira",
                name = "Mira",
                basePrompt = """
                    You are Mira, a creative and storytelling AI mentor who makes learning magical.
                    Your approach: "Every concept has a story waiting to be told."
                    
                    Your characteristics:
                    - Imaginative, friendly, and engaging
                    - Use storytelling, scenarios, and creative examples
                    - Make abstract concepts tangible through narratives
                    - Encourage curiosity and exploration
                    - Connect learning to real-world adventures
                    
                    Transform each lesson into a memorable journey!
                """.trimIndent(),
                tone = "creative and friendly",
                teachingStyle = "Story-based and scenario-driven",
                patience = 0.8f,
                humor = 0.8f,
                strictness = 0.3f,
                energy = 0.7f
            )
        }
    }
}

// ===== MEMORY LAYER (The Learner's Growth Engine) =====

@Serializable
data class UserLearningContext(
    val userId: String,
    val topicProgress: Map<String, Map<String, Float>> = emptyMap(), // subject -> topic -> progress (0.0-1.0)
    val personalityAffinity: String = "sensei", // Current preferred mentor
    val totalXP: Int = 0,
    val streakDays: Int = 0,
    val learningRate: String = "moderate", // "slow", "moderate", "fast"
    val recentAccuracy: Float = 0.7f, // Average of last 5 quizzes
    val confusionPoints: List<String> = emptyList(), // Topics user struggles with
    val interests: List<String> = emptyList(), // Topics user excels at
    val conversationMemory: List<MemorySnapshot> = emptyList(),
    val currentMood: String = "neutral", // "struggling", "neutral", "confident", "excited"
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val personalityTraits: PersonalityTraits = PersonalityTraits()
)

@Serializable
data class MemorySnapshot(
    val timestamp: Long,
    val topic: String,
    val userQuery: String,
    val aiResponse: String,
    val sentiment: String, // "confused", "engaged", "frustrated", "excited"
    val embedding: String? = null // For semantic search (optional)
)

@Serializable
data class PersonalityTraits(
    val patience: Float = 0.5f,      // Evolved based on user interactions
    val humor: Float = 0.5f,          // Evolved based on user feedback
    val strictness: Float = 0.5f,     // Evolved based on user performance
    val energy: Float = 0.5f          // Evolved based on user engagement
) {
    fun evolve(performanceDelta: Float, engagementDelta: Float): PersonalityTraits {
        return copy(
            patience = (patience + if (performanceDelta < 0) 0.1f else -0.05f).coerceIn(0f, 1f),
            humor = (humor + engagementDelta * 0.1f).coerceIn(0f, 1f),
            strictness = (strictness + if (performanceDelta > 0.2f) 0.05f else -0.05f).coerceIn(
                0f,
                1f
            ),
            energy = (energy + engagementDelta * 0.15f).coerceIn(0f, 1f)
        )
    }
}

@Serializable
data class TopicMastery(
    val topicId: String,
    val topicName: String,
    val subject: String,
    val masteryLevel: Float = 0f, // 0.0 - 1.0
    val attempts: Int = 0,
    val lastAccuracy: Float = 0f,
    val timeSpent: Long = 0, // milliseconds
    val mistakesLog: List<String> = emptyList(),
    val lastPracticed: Long = System.currentTimeMillis()
)

// ===== ADAPTIVE BEHAVIOR LAYER =====

data class AdaptiveBehavior(
    val tone: String,          // "gentle", "motivational", "challenging", "balanced"
    val difficulty: String,    // "easy", "moderate", "hard", "adaptive"
    val mode: String,          // "review", "learn", "challenge", "test"
    val pacing: String,        // "slow", "normal", "fast"
    val encouragementLevel: Float = 0.5f, // 0.0 - 1.0
    val explanationDepth: String = "balanced" // "simple", "balanced", "detailed"
)

class AdaptiveBehaviorEngine {

    fun calculateBehavior(context: UserLearningContext, currentTopic: String): AdaptiveBehavior {
        val topicProgress = context.topicProgress[currentTopic]?.values?.average()?.toFloat() ?: 0f
        val recentAccuracy = context.recentAccuracy
        val streakDays = context.streakDays
        val learningRate = context.learningRate
        val mood = context.currentMood

        // Determine tone
        val tone = when {
            mood == "struggling" || recentAccuracy < 0.5f -> "gentle"
            mood == "excited" || streakDays > 7 -> "motivational"
            mood == "confident" && recentAccuracy > 0.8f -> "challenging"
            else -> "balanced"
        }

        // Determine difficulty
        val difficulty = when {
            recentAccuracy < 0.6f -> "easy"
            recentAccuracy > 0.85f && topicProgress > 0.7f -> "hard"
            topicProgress < 0.3f -> "easy"
            else -> "moderate"
        }

        // Determine mode
        val mode = when {
            recentAccuracy < 0.6f || topicProgress < 0.3f -> "review"
            topicProgress > 0.8f && recentAccuracy > 0.8f -> "challenge"
            streakDays > 10 -> "test"
            else -> "learn"
        }

        // Determine pacing
        val pacing = when (learningRate) {
            "fast" -> if (recentAccuracy > 0.75f) "fast" else "normal"
            "slow" -> "slow"
            else -> "normal"
        }

        // Calculate encouragement level
        val encouragementLevel = when {
            recentAccuracy < 0.5f -> 0.9f
            mood == "struggling" -> 0.85f
            streakDays > 5 -> 0.7f
            else -> 0.5f
        }

        // Determine explanation depth
        val explanationDepth = when {
            context.confusionPoints.contains(currentTopic) -> "detailed"
            learningRate == "fast" && recentAccuracy > 0.8f -> "balanced"
            recentAccuracy < 0.6f -> "simple"
            else -> "balanced"
        }

        return AdaptiveBehavior(
            tone = tone,
            difficulty = difficulty,
            mode = mode,
            pacing = pacing,
            encouragementLevel = encouragementLevel,
            explanationDepth = explanationDepth
        )
    }

    fun generateAdaptivePromptModifier(behavior: AdaptiveBehavior): String {
        return buildString {
            appendLine("Teaching Adjustments:")
            appendLine("- Tone: ${behavior.tone}")
            appendLine("- Difficulty: ${behavior.difficulty}")
            appendLine("- Mode: ${behavior.mode}")
            appendLine("- Pacing: ${behavior.pacing}")
            appendLine("- Encouragement: ${(behavior.encouragementLevel * 100).toInt()}%")
            appendLine("- Explanation Depth: ${behavior.explanationDepth}")
            appendLine()

            when (behavior.tone) {
                "gentle" -> appendLine("Use patient, supportive language. Break concepts into smaller steps.")
                "motivational" -> appendLine("Be energetic and encouraging. Celebrate progress enthusiastically!")
                "challenging" -> appendLine("Push the student with thought-provoking questions. Encourage critical thinking.")
                else -> appendLine("Maintain a balanced, friendly teaching approach.")
            }

            when (behavior.mode) {
                "review" -> appendLine("Focus on reviewing fundamentals. Use lots of examples.")
                "learn" -> appendLine("Introduce new concepts clearly. Balance theory with practice.")
                "challenge" -> appendLine("Present advanced scenarios. Encourage independent problem-solving.")
                "test" -> appendLine("Assess understanding through thoughtful questions.")
            }
        }
    }
}

// ===== SENTIMENT ANALYSIS (Simple) =====

object SentimentAnalyzer {

    private val confusedKeywords =
        listOf("confused", "don't understand", "what", "huh", "unclear", "difficult", "hard")
    private val frustratedKeywords =
        listOf("frustrated", "annoying", "stuck", "can't", "impossible", "give up")
    private val excitedKeywords =
        listOf("wow", "cool", "awesome", "love", "great", "amazing", "interesting")
    private val engagedKeywords =
        listOf("why", "how", "tell me more", "explain", "curious", "want to know")

    fun analyzeSentiment(userInput: String): String {
        val lowerInput = userInput.lowercase()

        return when {
            confusedKeywords.any { lowerInput.contains(it) } -> "confused"
            frustratedKeywords.any { lowerInput.contains(it) } -> "frustrated"
            excitedKeywords.any { lowerInput.contains(it) } -> "excited"
            engagedKeywords.any { lowerInput.contains(it) } -> "engaged"
            else -> "neutral"
        }
    }

    fun determineMood(recentSentiments: List<String>, recentAccuracy: Float): String {
        val sentimentCounts = recentSentiments.groupingBy { it }.eachCount()

        return when {
            recentAccuracy < 0.5f || sentimentCounts.getOrDefault(
                "confused",
                0
            ) >= 2 -> "struggling"

            recentAccuracy > 0.85f && sentimentCounts.getOrDefault("excited", 0) >= 1 -> "excited"
            recentAccuracy > 0.75f -> "confident"
            else -> "neutral"
        }
    }
}
