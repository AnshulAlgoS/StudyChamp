package com.runanywhere.startup_hackathon20

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firebase Models for StudyChamp
 */

// User Profile stored in Firebase
data class UserProfile(
    @DocumentId
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val selectedMentor: String = "sensei",
    val totalXP: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStudyDate: Timestamp? = null,
    val topicsCompleted: Int = 0,
    val quizzesCompleted: Int = 0,
    val flashcardsCompleted: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

// Quiz Result stored in Firebase
data class FirebaseQuizResult(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val topic: String = "",
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val xpEarned: Int = 0,
    val mentorUsed: String = "",
    val completedAt: Timestamp = Timestamp.now()
)

// Flashcard Progress stored in Firebase
data class FirebaseFlashcardProgress(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val topic: String = "",
    val totalCards: Int = 0,
    val masteredCards: Int = 0,
    val xpEarned: Int = 0,
    val lastReviewedAt: Timestamp = Timestamp.now()
)

// Achievement unlocked by user
data class FirebaseAchievement(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val achievementId: String = "",
    val title: String = "",
    val description: String = "",
    val emoji: String = "",
    val xpReward: Int = 0,
    val unlockedAt: Timestamp = Timestamp.now()
)

// Study Session for tracking activity
data class StudySession(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val topics: String = "",
    val learningStyle: String = "",
    val duration: Long = 0, // in seconds
    val xpEarned: Int = 0,
    val startedAt: Timestamp = Timestamp.now(),
    val endedAt: Timestamp? = null
)
