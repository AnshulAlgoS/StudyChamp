package com.runanywhere.startup_hackathon20.ai

import kotlinx.serialization.Serializable

// ===== LEARNING JOURNEYS — Story Mode Logic =====

@Serializable
data class LearningQuest(
    val questId: String,
    val subject: String,
    val questName: String,
    val description: String,
    val lore: String, // Story narrative
    val missions: List<Mission>,
    val unlockRequirement: UnlockRequirement? = null,
    val completionReward: QuestReward,
    val progress: Float = 0f, // 0.0 - 1.0
    val status: QuestStatus = QuestStatus.LOCKED
)

@Serializable
data class Mission(
    val missionId: String,
    val missionName: String,
    val topic: String,
    val description: String,
    val narrative: String, // Story for this mission
    val challenges: List<Challenge>,
    val completionReward: MissionReward,
    val status: MissionStatus = MissionStatus.LOCKED,
    val progress: Float = 0f
)

@Serializable
data class Challenge(
    val challengeId: String,
    val challengeName: String,
    val type: ChallengeType,
    val difficulty: String, // "easy", "medium", "hard"
    val description: String,
    val xpReward: Int,
    val status: ChallengeStatus = ChallengeStatus.LOCKED,
    val attempts: Int = 0,
    val bestScore: Float = 0f
)

enum class ChallengeType {
    QUIZ,           // Multiple choice questions
    FLASHCARD,      // Memory challenges
    PRACTICE,       // Apply concepts
    BOSS_BATTLE,    // Major test at end of quest
    EXPLORATION     // Open-ended learning
}

enum class QuestStatus {
    LOCKED,         // Not yet available
    UNLOCKED,       // Available to start
    IN_PROGRESS,    // Currently working on
    COMPLETED       // Finished
}

enum class MissionStatus {
    LOCKED,
    UNLOCKED,
    IN_PROGRESS,
    COMPLETED
}

enum class ChallengeStatus {
    LOCKED,
    UNLOCKED,
    IN_PROGRESS,
    COMPLETED,
    MASTERED        // Completed with >90% score
}

@Serializable
data class UnlockRequirement(
    val type: String, // "xp", "level", "quest_complete", "mission_complete"
    val value: Int,
    val description: String
)

@Serializable
data class QuestReward(
    val xp: Int,
    val badge: String?,
    val title: String?, // Achievement title like "Algebra Master"
    val unlocksQuest: String?, // ID of next quest unlocked
    val narrativeReward: String // Story text for completion
)

@Serializable
data class MissionReward(
    val xp: Int,
    val badge: String?,
    val unlocksChallenge: String? // ID of special challenge unlocked
)

@Serializable
data class Achievement(
    val achievementId: String,
    val title: String,
    val description: String,
    val badge: String,
    val xpReward: Int,
    val rarity: AchievementRarity,
    val unlockedAt: Long? = null,
    val isUnlocked: Boolean = false
)

enum class AchievementRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

// ===== QUEST GENERATOR =====

class QuestGenerator {

    fun generateQuest(subject: String, topics: List<String>, userLevel: Int): LearningQuest {
        val questId = "${subject.lowercase()}_${System.currentTimeMillis()}"
        val questName = generateQuestName(subject, topics.firstOrNull() ?: "Fundamentals")
        val lore = generateQuestLore(subject, topics, userLevel)

        val missions = topics.mapIndexed { index, topic ->
            generateMission(index, topic, subject, userLevel)
        }

        return LearningQuest(
            questId = questId,
            subject = subject,
            questName = questName,
            description = "Master the fundamentals of ${topics.joinToString(", ")}",
            lore = lore,
            missions = missions,
            unlockRequirement = if (userLevel > 1) UnlockRequirement(
                type = "level",
                value = userLevel,
                description = "Reach Level $userLevel"
            ) else null,
            completionReward = QuestReward(
                xp = 500,
                badge = "🏆",
                title = "$subject Scholar",
                unlocksQuest = null,
                narrativeReward = generateCompletionNarrative(subject, topics)
            ),
            status = if (userLevel <= 1) QuestStatus.UNLOCKED else QuestStatus.LOCKED
        )
    }

    private fun generateMission(
        index: Int,
        topic: String,
        subject: String,
        userLevel: Int
    ): Mission {
        val missionId =
            "mission_${topic.lowercase().replace(" ", "_")}_${System.currentTimeMillis()}"
        val challenges = generateChallenges(topic, subject, userLevel)

        return Mission(
            missionId = missionId,
            missionName = "Chapter ${index + 1}: $topic",
            topic = topic,
            description = "Explore and master $topic in $subject",
            narrative = generateMissionNarrative(topic, subject, index),
            challenges = challenges,
            completionReward = MissionReward(
                xp = 100,
                badge = "⭐",
                unlocksChallenge = null
            ),
            status = if (index == 0) MissionStatus.UNLOCKED else MissionStatus.LOCKED
        )
    }

    private fun generateChallenges(
        topic: String,
        subject: String,
        userLevel: Int
    ): List<Challenge> {
        return listOf(
            Challenge(
                challengeId = "learn_${topic}_${System.currentTimeMillis()}",
                challengeName = "Study Session",
                type = ChallengeType.EXPLORATION,
                difficulty = "easy",
                description = "Learn the fundamentals of $topic",
                xpReward = 25,
                status = ChallengeStatus.UNLOCKED
            ),
            Challenge(
                challengeId = "quiz_${topic}_${System.currentTimeMillis()}",
                challengeName = "Knowledge Test",
                type = ChallengeType.QUIZ,
                difficulty = "medium",
                description = "Test your understanding with a quiz",
                xpReward = 50,
                status = ChallengeStatus.LOCKED
            ),
            Challenge(
                challengeId = "flashcard_${topic}_${System.currentTimeMillis()}",
                challengeName = "Memory Challenge",
                type = ChallengeType.FLASHCARD,
                difficulty = "medium",
                description = "Master key concepts with flashcards",
                xpReward = 40,
                status = ChallengeStatus.LOCKED
            ),
            Challenge(
                challengeId = "practice_${topic}_${System.currentTimeMillis()}",
                challengeName = "Practice Problems",
                type = ChallengeType.PRACTICE,
                difficulty = "hard",
                description = "Apply your knowledge to solve problems",
                xpReward = 75,
                status = ChallengeStatus.LOCKED
            )
        )
    }

    private fun generateQuestName(subject: String, mainTopic: String): String {
        val names = when (subject.lowercase()) {
            "physics" -> listOf(
                "The Quantum Odyssey",
                "Journey Through Forces",
                "The Mechanics Chronicles",
                "Voyage Through Motion"
            )

            "math", "mathematics" -> listOf(
                "The Algebraic Quest",
                "Journey Through Numbers",
                "The Calculus Chronicles",
                "Voyage to Mathematical Mastery"
            )

            "chemistry" -> listOf(
                "The Atomic Adventure",
                "Journey Through Elements",
                "The Molecular Quest",
                "Voyage to Chemical Mastery"
            )

            "biology" -> listOf(
                "The Living World Quest",
                "Journey Through Life",
                "The Cellular Chronicles",
                "Voyage Through Evolution"
            )

            "history" -> listOf(
                "The Time Traveler's Quest",
                "Journey Through Ages",
                "Chronicles of Civilization",
                "Voyage Through Time"
            )

            else -> listOf(
                "The $subject Odyssey",
                "Journey Through $mainTopic",
                "The $subject Chronicles"
            )
        }
        return names.random()
    }

    private fun generateQuestLore(subject: String, topics: List<String>, userLevel: Int): String {
        return """
            Welcome, brave scholar! You stand at the threshold of a great learning adventure.
            
            In this quest, you will journey through the realm of $subject, uncovering the mysteries of ${
            topics.joinToString(
                ", "
            )
        }.
            
            Each mission will challenge your mind and expand your understanding. Complete challenges to earn XP and unlock new knowledge.
            
            Your mentor will guide you through this journey, adapting to your learning style and pace.
            
            Are you ready to begin? The path to mastery awaits!
        """.trimIndent()
    }

    private fun generateMissionNarrative(
        topic: String,
        subject: String,
        missionIndex: Int
    ): String {
        val narratives = listOf(
            "The journey begins as you enter the domain of $topic. Ancient knowledge awaits discovery...",
            "You've reached a new chapter in your quest. $topic holds secrets that will empower your understanding.",
            "As you progress, the path leads to $topic - a crucial piece in the $subject puzzle.",
            "Welcome to the next phase of your adventure. $topic will challenge and enlighten you."
        )
        return narratives.getOrNull(missionIndex) ?: narratives.random()
    }

    private fun generateCompletionNarrative(subject: String, topics: List<String>): String {
        return """
            🎉 Congratulations, Champion!
            
            You have successfully conquered the realm of ${topics.joinToString(", ")} in $subject!
            
            Your dedication and perseverance have paid off. You've gained valuable knowledge and skills that will serve you well.
            
            But this is not the end - it's just the beginning. New quests await, each more challenging and rewarding than the last.
            
            Take a moment to celebrate your achievement, then prepare for the next adventure!
            
            The path to ultimate mastery continues...
        """.trimIndent()
    }
}

// ===== JOURNEY MANAGER =====

class JourneyManager {

    private val quests = mutableMapOf<String, LearningQuest>()
    private val achievements = mutableMapOf<String, Achievement>()

    fun createJourney(subject: String, topics: List<String>, userLevel: Int): LearningQuest {
        val generator = QuestGenerator()
        val quest = generator.generateQuest(subject, topics, userLevel)
        quests[quest.questId] = quest
        return quest
    }

    fun unlockNextMission(questId: String): Mission? {
        val quest = quests[questId] ?: return null
        val nextLockedMission = quest.missions.firstOrNull { it.status == MissionStatus.LOCKED }

        if (nextLockedMission != null) {
            val updatedMission = nextLockedMission.copy(status = MissionStatus.UNLOCKED)
            val updatedMissions = quest.missions.map {
                if (it.missionId == nextLockedMission.missionId) updatedMission else it
            }
            quests[questId] = quest.copy(missions = updatedMissions)
            return updatedMission
        }

        return null
    }

    fun completeChallenge(
        questId: String,
        missionId: String,
        challengeId: String,
        score: Float
    ): ChallengeRewardResult {
        val quest = quests[questId] ?: return ChallengeRewardResult(0, null, null)
        val mission =
            quest.missions.find { it.missionId == missionId } ?: return ChallengeRewardResult(
                0,
                null,
                null
            )
        val challenge = mission.challenges.find { it.challengeId == challengeId }
            ?: return ChallengeRewardResult(0, null, null)

        // Update challenge status
        val newStatus = if (score >= 0.9f) ChallengeStatus.MASTERED else ChallengeStatus.COMPLETED
        val updatedChallenge = challenge.copy(
            status = newStatus,
            attempts = challenge.attempts + 1,
            bestScore = maxOf(challenge.bestScore, score)
        )

        // Unlock next challenge
        val challengeIndex = mission.challenges.indexOf(challenge)
        val updatedChallenges = mission.challenges.mapIndexed { index, ch ->
            when {
                ch.challengeId == challengeId -> updatedChallenge
                index == challengeIndex + 1 && ch.status == ChallengeStatus.LOCKED ->
                    ch.copy(status = ChallengeStatus.UNLOCKED)

                else -> ch
            }
        }

        // Check if mission is complete
        val allChallengesComplete = updatedChallenges.all {
            it.status == ChallengeStatus.COMPLETED || it.status == ChallengeStatus.MASTERED
        }

        val updatedMission = mission.copy(
            challenges = updatedChallenges,
            status = if (allChallengesComplete) MissionStatus.COMPLETED else mission.status,
            progress = updatedChallenges.count { it.status != ChallengeStatus.LOCKED }
                .toFloat() / updatedChallenges.size
        )

        // Update quest
        val updatedMissions = quest.missions.map {
            if (it.missionId == missionId) updatedMission else it
        }

        val questProgress =
            updatedMissions.sumOf { it.progress.toDouble() }.toFloat() / updatedMissions.size
        val questComplete = updatedMissions.all { it.status == MissionStatus.COMPLETED }

        quests[questId] = quest.copy(
            missions = updatedMissions,
            progress = questProgress,
            status = if (questComplete) QuestStatus.COMPLETED else QuestStatus.IN_PROGRESS
        )

        val xpEarned = challenge.xpReward + if (newStatus == ChallengeStatus.MASTERED) 25 else 0
        val unlockedMission = if (allChallengesComplete) unlockNextMission(questId) else null
        val narrative = generateChallengeCompletionNarrative(
            challenge,
            newStatus,
            allChallengesComplete,
            questComplete
        )

        return ChallengeRewardResult(xpEarned, unlockedMission, narrative)
    }

    private fun generateChallengeCompletionNarrative(
        challenge: Challenge,
        status: ChallengeStatus,
        missionComplete: Boolean,
        questComplete: Boolean
    ): String {
        return buildString {
            if (status == ChallengeStatus.MASTERED) {
                appendLine("🌟 MASTERED! Outstanding performance!")
                appendLine("You've achieved mastery in ${challenge.challengeName}!")
            } else {
                appendLine("✅ Challenge Complete!")
                appendLine("You've successfully completed ${challenge.challengeName}!")
            }

            if (missionComplete) {
                appendLine()
                appendLine("🎊 MISSION ACCOMPLISHED!")
                appendLine("You've completed this entire mission! New challenges await...")
            }

            if (questComplete) {
                appendLine()
                appendLine("🏆 QUEST COMPLETED!")
                appendLine("You've conquered this entire quest! Legendary achievement!")
            }
        }
    }

    fun getActiveQuest(): LearningQuest? {
        return quests.values.firstOrNull { it.status == QuestStatus.IN_PROGRESS || it.status == QuestStatus.UNLOCKED }
    }

    fun getAllQuests(): List<LearningQuest> {
        return quests.values.toList()
    }
}

data class ChallengeRewardResult(
    val xpEarned: Int,
    val unlockedMission: Mission?,
    val narrative: String?
)
