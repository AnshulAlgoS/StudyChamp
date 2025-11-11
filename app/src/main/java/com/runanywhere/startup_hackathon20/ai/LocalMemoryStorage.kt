package com.runanywhere.startup_hackathon20.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Local Memory Storage - Offline persistence for AI Brain
 * Stores user learning context, conversation memory, and personality evolution
 */
class LocalMemoryStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ai_brain_memory",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    // ===== USER LEARNING CONTEXT =====

    fun saveUserContext(context: UserLearningContext) {
        val jsonString = json.encodeToString(context)
        prefs.edit().putString(KEY_USER_CONTEXT, jsonString).apply()
    }

    fun loadUserContext(userId: String): UserLearningContext? {
        val jsonString = prefs.getString(KEY_USER_CONTEXT, null) ?: return null
        return try {
            json.decodeFromString<UserLearningContext>(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("LocalMemoryStorage", "Failed to load user context", e)
            null
        }
    }

    // ===== CONVERSATION MEMORY =====

    fun addMemorySnapshot(snapshot: MemorySnapshot) {
        val context = loadUserContext(snapshot.topic) ?: return
        val updatedMemory = context.conversationMemory + snapshot

        // Keep only last 50 memories to save space
        val trimmedMemory = if (updatedMemory.size > 50) {
            updatedMemory.takeLast(50)
        } else {
            updatedMemory
        }

        val updatedContext = context.copy(conversationMemory = trimmedMemory)
        saveUserContext(updatedContext)
    }

    fun getRecentMemories(userId: String, limit: Int = 10): List<MemorySnapshot> {
        val context = loadUserContext(userId) ?: return emptyList()
        return context.conversationMemory.takeLast(limit)
    }

    fun getMemoriesByTopic(userId: String, topic: String): List<MemorySnapshot> {
        val context = loadUserContext(userId) ?: return emptyList()
        return context.conversationMemory.filter { it.topic.contains(topic, ignoreCase = true) }
    }

    // ===== TOPIC PROGRESS =====

    fun updateTopicProgress(userId: String, subject: String, topic: String, progress: Float) {
        val context = loadUserContext(userId) ?: return
        val subjectMap = context.topicProgress[subject]?.toMutableMap() ?: mutableMapOf()
        subjectMap[topic] = progress.coerceIn(0f, 1f)

        val updatedProgress = context.topicProgress.toMutableMap()
        updatedProgress[subject] = subjectMap

        val updatedContext = context.copy(topicProgress = updatedProgress)
        saveUserContext(updatedContext)
    }

    fun getTopicProgress(userId: String, subject: String, topic: String): Float {
        val context = loadUserContext(userId) ?: return 0f
        return context.topicProgress[subject]?.get(topic) ?: 0f
    }

    // ===== LEARNING RATE & PERFORMANCE =====

    fun updateLearningRate(userId: String, newRate: String) {
        val context = loadUserContext(userId) ?: return
        val updatedContext = context.copy(learningRate = newRate)
        saveUserContext(updatedContext)
    }

    fun updateRecentAccuracy(userId: String, accuracy: Float) {
        val context = loadUserContext(userId) ?: return
        val updatedContext = context.copy(recentAccuracy = accuracy.coerceIn(0f, 1f))
        saveUserContext(updatedContext)
    }

    fun calculateAverageAccuracy(userId: String, quizResults: List<Float>): Float {
        if (quizResults.isEmpty()) return 0.7f // Default

        // Weight recent results more heavily
        val recentResults = quizResults.takeLast(5)
        val average = recentResults.average().toFloat()

        updateRecentAccuracy(userId, average)
        return average
    }

    // ===== CONFUSION POINTS & INTERESTS =====

    fun addConfusionPoint(userId: String, topic: String) {
        val context = loadUserContext(userId) ?: return
        if (!context.confusionPoints.contains(topic)) {
            val updated = context.confusionPoints + topic
            saveUserContext(context.copy(confusionPoints = updated))
        }
    }

    fun removeConfusionPoint(userId: String, topic: String) {
        val context = loadUserContext(userId) ?: return
        val updated = context.confusionPoints.filter { it != topic }
        saveUserContext(context.copy(confusionPoints = updated))
    }

    fun addInterest(userId: String, topic: String) {
        val context = loadUserContext(userId) ?: return
        if (!context.interests.contains(topic)) {
            val updated = context.interests + topic
            saveUserContext(context.copy(interests = updated))
        }
    }

    // ===== MOOD TRACKING =====

    fun updateMood(userId: String, mood: String) {
        val context = loadUserContext(userId) ?: return
        val updatedContext = context.copy(currentMood = mood)
        saveUserContext(updatedContext)
    }

    fun inferMoodFromInteractions(userId: String) {
        val context = loadUserContext(userId) ?: return
        val recentSentiments = context.conversationMemory.takeLast(5).map { it.sentiment }
        val inferredMood = SentimentAnalyzer.determineMood(recentSentiments, context.recentAccuracy)
        updateMood(userId, inferredMood)
    }

    // ===== PERSONALITY TRAITS EVOLUTION =====

    fun evolvePersonality(userId: String, performanceDelta: Float, engagementDelta: Float) {
        val context = loadUserContext(userId) ?: return
        val evolvedTraits = context.personalityTraits.evolve(performanceDelta, engagementDelta)
        val updatedContext = context.copy(personalityTraits = evolvedTraits)
        saveUserContext(updatedContext)
    }

    fun getPersonalityTraits(userId: String): PersonalityTraits {
        val context = loadUserContext(userId) ?: return PersonalityTraits()
        return context.personalityTraits
    }

    // ===== QUESTS & JOURNEYS =====

    fun saveQuest(quest: LearningQuest) {
        val quests = loadAllQuests().toMutableList()
        val existingIndex = quests.indexOfFirst { it.questId == quest.questId }

        if (existingIndex >= 0) {
            quests[existingIndex] = quest
        } else {
            quests.add(quest)
        }

        val jsonString = json.encodeToString(quests)
        prefs.edit().putString(KEY_QUESTS, jsonString).apply()
    }

    fun loadQuest(questId: String): LearningQuest? {
        return loadAllQuests().firstOrNull { it.questId == questId }
    }

    fun loadAllQuests(): List<LearningQuest> {
        val jsonString = prefs.getString(KEY_QUESTS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<LearningQuest>>(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("LocalMemoryStorage", "Failed to load quests", e)
            emptyList()
        }
    }

    fun getActiveQuest(): LearningQuest? {
        return loadAllQuests().firstOrNull {
            it.status == QuestStatus.IN_PROGRESS || it.status == QuestStatus.UNLOCKED
        }
    }

    // ===== TOPIC MASTERY =====

    fun saveTopicMastery(mastery: TopicMastery) {
        val masteries = loadAllTopicMasteries().toMutableMap()
        masteries[mastery.topicId] = mastery

        val jsonString = json.encodeToString(masteries)
        prefs.edit().putString(KEY_TOPIC_MASTERY, jsonString).apply()
    }

    fun loadTopicMastery(topicId: String): TopicMastery? {
        return loadAllTopicMasteries()[topicId]
    }

    fun loadAllTopicMasteries(): Map<String, TopicMastery> {
        val jsonString = prefs.getString(KEY_TOPIC_MASTERY, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, TopicMastery>>(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("LocalMemoryStorage", "Failed to load topic masteries", e)
            emptyMap()
        }
    }

    // ===== ANALYTICS & INSIGHTS =====

    fun getTotalStudyTime(userId: String): Long {
        val masteries = loadAllTopicMasteries()
        return masteries.values.sumOf { it.timeSpent }
    }

    fun getTopPerformingTopics(userId: String, limit: Int = 5): List<TopicMastery> {
        return loadAllTopicMasteries().values
            .sortedByDescending { it.masteryLevel }
            .take(limit)
    }

    fun getStrugglingTopics(userId: String, threshold: Float = 0.5f): List<TopicMastery> {
        return loadAllTopicMasteries().values
            .filter { it.masteryLevel < threshold && it.attempts >= 2 }
            .sortedBy { it.masteryLevel }
    }

    // ===== UTILITY =====

    fun clearAllMemory() {
        prefs.edit().clear().apply()
    }

    fun exportLearningData(userId: String): String {
        val context = loadUserContext(userId)
        val quests = loadAllQuests()
        val masteries = loadAllTopicMasteries()

        val exportData = mapOf(
            "userContext" to context,
            "quests" to quests,
            "topicMasteries" to masteries,
            "exportedAt" to System.currentTimeMillis()
        )

        return json.encodeToString(exportData)
    }

    companion object {
        private const val KEY_USER_CONTEXT = "user_learning_context"
        private const val KEY_QUESTS = "learning_quests"
        private const val KEY_TOPIC_MASTERY = "topic_mastery"
    }
}

/**
 * Memory Context Builder - Constructs prompt context from stored memories
 */
class MemoryContextBuilder(private val storage: LocalMemoryStorage) {

    fun buildContextualPrompt(
        userId: String,
        currentTopic: String,
        userQuery: String,
        basePrompt: String
    ): String {
        val userContext = storage.loadUserContext(userId)
        val recentMemories = storage.getRecentMemories(userId, 5)
        val topicMemories = storage.getMemoriesByTopic(userId, currentTopic)
        val topicMastery = storage.loadTopicMastery(currentTopic)

        return buildString {
            appendLine(basePrompt)
            appendLine()

            // Add user context
            userContext?.let {
                appendLine("=== Student Context ===")
                appendLine("Learning Rate: ${it.learningRate}")
                appendLine("Recent Performance: ${(it.recentAccuracy * 100).toInt()}%")
                appendLine("Current Mood: ${it.currentMood}")
                appendLine("Streak: ${it.streakDays} days")

                if (it.confusionPoints.isNotEmpty()) {
                    appendLine("Struggling with: ${it.confusionPoints.joinToString(", ")}")
                }

                if (it.interests.isNotEmpty()) {
                    appendLine("Interested in: ${it.interests.joinToString(", ")}")
                }
                appendLine()
            }

            // Add topic mastery info
            topicMastery?.let {
                appendLine("=== Topic Mastery: ${it.topicName} ===")
                appendLine("Mastery Level: ${(it.masteryLevel * 100).toInt()}%")
                appendLine("Attempts: ${it.attempts}")
                appendLine("Last Accuracy: ${(it.lastAccuracy * 100).toInt()}%")
                if (it.mistakesLog.isNotEmpty()) {
                    appendLine("Common Mistakes: ${it.mistakesLog.take(3).joinToString("; ")}")
                }
                appendLine()
            }

            // Add relevant past conversations
            if (topicMemories.isNotEmpty()) {
                appendLine("=== Previous ${currentTopic} Discussions ===")
                topicMemories.takeLast(3).forEach { memory ->
                    appendLine("Student: ${memory.userQuery}")
                    appendLine("Response: ${memory.aiResponse.take(100)}...")
                    appendLine()
                }
            }

            appendLine("=== Current Question ===")
            appendLine("Student: $userQuery")
            appendLine()
            appendLine("Respond with consideration of the student's context and history.")
        }
    }

    fun buildAdaptiveSystemPrompt(
        userId: String,
        currentTopic: String,
        mentorPersonality: MentorPersonality
    ): String {
        val userContext = storage.loadUserContext(userId) ?: return mentorPersonality.basePrompt
        val adaptiveBehavior = AdaptiveBehaviorEngine().calculateBehavior(userContext, currentTopic)

        return buildString {
            appendLine(mentorPersonality.getSystemPrompt(userContext))
            appendLine()
            appendLine(AdaptiveBehaviorEngine().generateAdaptivePromptModifier(adaptiveBehavior))
        }
    }
}
