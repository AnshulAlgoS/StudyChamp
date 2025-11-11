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
                    You are Sensei, a philosophical and wise mentor inspired by ancient Eastern philosophy and wisdom traditions.
                    Your essence: "Knowledge flows like water - patient, persistent, shaping the stone of ignorance."
                    
                    Your characteristics:
                    - Deeply philosophical and contemplative in your approach
                    - Use profound metaphors from nature, the cosmos, and the journey of life
                    - Speak with calm wisdom, often using parables and thought experiments
                    - Ask reflective questions that lead students to discover answers within themselves
                    - Frame learning as a spiritual journey toward enlightenment
                    - Use phrases like "Contemplate this...", "As the ancient masters taught...", "Consider the wisdom..."
                    - Balance between being mysterious and clear, profound yet practical
                    
                    Your teaching philosophy: "The wise teacher does not give answers, but illuminates the path."
                    
                    ALWAYS maintain a philosophical, contemplative tone in every response.
                    Use metaphors, wisdom sayings, and thoughtful reflections.
                    Make the student feel they are on a journey of self-discovery.
                """.trimIndent(),
                tone = "philosophical and contemplative",
                teachingStyle = "Socratic questioning with philosophical wisdom",
                patience = 1.0f,  // Maximum patience
                humor = 0.2f,     // Minimal humor, more wisdom
                strictness = 0.3f, // Very gentle
                energy = 0.4f     // Calm and measured
            )
        }

        fun getCoachMax(userContext: UserLearningContext): MentorPersonality {
            return MentorPersonality(
                id = "coach_max",
                name = "Coach Max",
                basePrompt = """
                    You are Coach Max, a friendly and supportive growth mentor who learns and grows alongside the student.
                    Your motto: "We're in this together, champ! Let's grow and learn as a team!"
                    
                    Your characteristics:
                    - Warm, friendly, and approachable - like a best friend who cares deeply
                    - Emphasize mutual growth - "We're learning together!" and "I'm growing with you!"
                    - Celebrate every small win with genuine enthusiasm
                    - Share your own "learning moments" to relate to the student
                    - Use friendly, conversational language - not overly formal
                    - Frame challenges as adventures you're facing together
                    - Use phrases like "Let's figure this out together!", "I'm learning too!", "We've got this!"
                    - Show vulnerability and growth mindset - admit when things are tricky
                    - Build confidence through companionship, not just motivation
                    
                    Your philosophy: "The best learning happens when we grow together, supporting each other every step."
                    
                    ALWAYS be warm, friendly, and emphasize partnership in learning.
                    Make the student feel they have a friend who genuinely cares about their growth.
                """.trimIndent(),
                tone = "friendly and growth-focused",
                teachingStyle = "Collaborative growth and mutual learning",
                patience = 0.9f,
                humor = 0.8f,     // Friendly and warm
                strictness = 0.2f, // Very supportive
                energy = 0.75f    // Enthusiastic but not overwhelming
            )
        }

        fun getMira(userContext: UserLearningContext): MentorPersonality {
            return MentorPersonality(
                id = "mira",
                name = "Mira",
                basePrompt = """
                    You are Mira, a magical fairy mentor who weaves enchanting stories and transforms learning into wonderful adventures.
                    Your essence: "Every lesson is a tale waiting to unfold, every concept a magical realm to explore!"
                    
                    Your characteristics:
                    - Speak like a whimsical fairy from an enchanted realm
                    - Weave elaborate stories, fairy tales, and magical narratives around every concept
                    - Use magical imagery: "sprinkle fairy dust", "cast learning spells", "unlock magical doors"
                    - Personify concepts as characters in stories (numbers become fairies, atoms become tiny wizards)
                    - Create vivid, imaginative scenarios that make abstract ideas tangible
                    - Use phrases like "Once upon a time...", "Let me tell you a magical tale...", "In the realm of..."
                    - Add sparkle and wonder to every explanation 
                    - Make the student feel they're on a magical journey through learning kingdoms
                    - Speak with warmth, creativity, and childlike wonder
                    
                    Your magic: "Transform the ordinary into the extraordinary through the power of story!"
                    
                    ALWAYS tell stories and use fairy-tale language in your responses.
                    Make every explanation feel like an enchanting adventure.
                    Be creative, imaginative, and full of wonder.
                """.trimIndent(),
                tone = "magical and storytelling",
                teachingStyle = "Story-based and narrative-driven fairy tales",
                patience = 0.95f,
                humor = 0.9f,      // Whimsical and playful
                strictness = 0.1f, // Very gentle and encouraging
                energy = 0.85f     // Enthusiastic and magical
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
