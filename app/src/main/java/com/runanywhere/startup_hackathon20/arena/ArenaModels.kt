package com.runanywhere.startup_hackathon20.arena

import com.google.firebase.Timestamp

/**
 * Focus Arena - Real-time competitive study sessions
 */

data class ArenaRoom(
    val roomId: String = "",
    val roomCode: String = "", // 6-digit code like "MATH01"
    val hostUserId: String = "",
    val hostName: String = "",

    // Challenge details
    val subject: String = "",
    val task: String = "", // e.g. "Complete 20 Math questions"
    val taskCount: Int = 20, // number of questions/tasks
    val timeLimit: Int = 60, // minutes

    // Room status
    val status: RoomStatus = RoomStatus.WAITING,
    val createdAt: Timestamp = Timestamp.now(),
    val startedAt: Timestamp? = null,
    val endedAt: Timestamp? = null,

    // Participants
    val participants: Map<String, ArenaParticipant> = emptyMap(), // userId -> participant
    val maxParticipants: Int = 10,

    // Settings
    val strictFocusMode: Boolean = true, // block other apps
    val allowedApps: List<String> = listOf() // apps allowed during focus
)

enum class RoomStatus {
    WAITING,    // Room created, waiting for participants
    STARTING,   // Countdown before start
    ACTIVE,     // Arena in progress
    COMPLETED   // Arena finished
}

data class ArenaParticipant(
    val userId: String = "",
    val userName: String = "",
    val avatarUrl: String = "",

    // Live stats
    val tasksCompleted: Int = 0,
    val focusTimeSeconds: Int = 0, // total focused time
    val currentStreak: Int = 0, // current uninterrupted focus streak
    val violations: Int = 0,

    // Status
    val status: ParticipantStatus = ParticipantStatus.READY,
    val joinedAt: Timestamp = Timestamp.now(),
    val lastSeenAt: Timestamp = Timestamp.now(),

    // Results
    val finishPosition: Int = 0, // 0 = not finished, 1 = first, 2 = second, etc.
    val finalXP: Int = 0,
    val completedAt: Timestamp? = null
)

enum class ParticipantStatus {
    READY,      // Joined, ready to start
    ACTIVE,     // Currently studying
    FOCUSED,    // Currently focused (on task)
    DISTRACTED, // Left app or lost focus
    COMPLETED,  // Finished task
    QUIT        // Left arena
}

data class ArenaViolation(
    val userId: String = "",
    val userName: String = "",
    val type: ViolationType = ViolationType.LEFT_APP,
    val description: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

enum class ViolationType {
    LEFT_APP,           // Switched to another app
    OPENED_BLOCKED_APP, // Opened non-allowed app
    IDLE_TOO_LONG,      // No activity for 5+ minutes
    CLOSED_APP          // Closed StudyChamp
}

data class ArenaMessage(
    val userId: String = "",
    val userName: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val type: MessageType = MessageType.CHAT
)

enum class MessageType {
    CHAT,           // Normal chat message
    SYSTEM,         // System notification
    ACHIEVEMENT,    // Someone unlocked achievement
    VIOLATION       // Someone got a violation
}

data class ArenaResult(
    val roomId: String = "",
    val roomCode: String = "",
    val subject: String = "",
    val task: String = "",

    // Winner
    val winnerId: String = "",
    val winnerName: String = "",
    val winnerXP: Int = 0,

    // All results
    val participants: List<ArenaParticipantResult> = emptyList(),

    // Stats
    val totalFocusTime: Int = 0, // combined minutes
    val averageCompletion: Float = 0f,
    val completedAt: Timestamp = Timestamp.now()
)

data class ArenaParticipantResult(
    val userId: String = "",
    val userName: String = "",
    val position: Int = 0,
    val tasksCompleted: Int = 0,
    val totalTasks: Int = 0,
    val focusTimeMinutes: Int = 0,
    val violations: Int = 0,
    val xpEarned: Int = 0
)

/**
 * XP Calculation for Arena
 */
object ArenaXPCalculator {

    fun calculateXP(
        taskCompleted: Boolean,
        tasksCompleted: Int,
        totalTasks: Int,
        focusTimeMinutes: Int,
        finishPosition: Int,
        violations: Int
    ): Int {
        var xp = 0

        // 1. Participation bonus
        xp += 100

        // 2. Task completion (proportional)
        val completionRate = tasksCompleted.toFloat() / totalTasks
        xp += (completionRate * 500).toInt()

        // 3. Full completion bonus
        if (taskCompleted) xp += 200

        // 4. Focus time bonus (10 XP per minute)
        xp += focusTimeMinutes * 10

        // 5. Position bonus
        xp += when (finishPosition) {
            1 -> 300 // Winner
            2 -> 200 // Second place
            3 -> 100 // Third place
            else -> 0
        }

        // 6. Violation penalty (50 XP per violation)
        xp -= violations * 50

        // Can't be negative
        return maxOf(0, xp)
    }

    fun calculateFocusScore(
        focusTimeSeconds: Int,
        totalTimeSeconds: Int
    ): Float {
        if (totalTimeSeconds == 0) return 0f
        return (focusTimeSeconds.toFloat() / totalTimeSeconds * 100).coerceIn(0f, 100f)
    }
}
