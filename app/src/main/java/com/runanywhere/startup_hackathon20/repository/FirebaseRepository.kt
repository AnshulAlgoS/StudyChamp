package com.runanywhere.startup_hackathon20.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.runanywhere.startup_hackathon20.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Collections
    private val usersCollection = firestore.collection("users")
    private val quizzesCollection = firestore.collection("quiz_results")
    private val flashcardsCollection = firestore.collection("flashcard_progress")
    private val achievementsCollection = firestore.collection("achievements")
    private val sessionsCollection = firestore.collection("study_sessions")
    
    // ===== AUTHENTICATION =====
    
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    suspend fun signInAnonymously(): Result<FirebaseUser?> {
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Anonymous sign in failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun createUserProfile(name: String, email: String = ""): Result<UserProfile> {
        val user = getCurrentUser() ?: return Result.failure(Exception("No user logged in"))
        
        val profile = UserProfile(
            userId = user.uid,
            name = name,
            email = email,
            photoUrl = user.photoUrl?.toString() ?: "",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        
        return try {
            usersCollection.document(user.uid).set(profile).await()
            Result.success(profile)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Create profile failed", e)
            Result.failure(e)
        }
    }
    
    // ===== USER PROFILE =====
    
    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        return try {
            val doc = usersCollection.document(userId).get().await()
            val profile = doc.toObject(UserProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Get profile failed", e)
            Result.failure(e)
        }
    }
    
    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Profile observation error", error)
                    return@addSnapshotListener
                }
                val profile = snapshot?.toObject(UserProfile::class.java)
                trySend(profile)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val data = updates.toMutableMap()
            data["updatedAt"] = Timestamp.now()
            usersCollection.document(userId).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Update profile failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateXP(userId: String, xpToAdd: Int): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val docRef = usersCollection.document(userId)
                val snapshot = transaction.get(docRef)
                val currentXP = snapshot.getLong("totalXP")?.toInt() ?: 0
                val newXP = currentXP + xpToAdd
                val newLevel = XPSystem.calculateLevel(newXP)
                
                transaction.update(docRef, mapOf(
                    "totalXP" to newXP,
                    "level" to newLevel,
                    "updatedAt" to Timestamp.now()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Update XP failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateStreak(userId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val docRef = usersCollection.document(userId)
                val snapshot = transaction.get(docRef)
                
                val lastStudyTimestamp = snapshot.getTimestamp("lastStudyDate")
                val currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0
                val longestStreak = snapshot.getLong("longestStreak")?.toInt() ?: 0
                
                val now = System.currentTimeMillis()
                val today = now / (24 * 60 * 60 * 1000)
                
                val lastStudyDay = if (lastStudyTimestamp != null) {
                    lastStudyTimestamp.seconds * 1000 / (24 * 60 * 60 * 1000)
                } else {
                    0
                }
                
                val newStreak = when {
                    lastStudyDay == today -> currentStreak // Same day
                    lastStudyDay == today - 1 -> currentStreak + 1 // Consecutive
                    else -> 1 // Broken, reset
                }
                
                transaction.update(docRef, mapOf(
                    "currentStreak" to newStreak,
                    "longestStreak" to maxOf(newStreak, longestStreak),
                    "lastStudyDate" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Update streak failed", e)
            Result.failure(e)
        }
    }
    
    // ===== QUIZ RESULTS =====
    
    suspend fun saveQuizResult(result: FirebaseQuizResult): Result<String> {
        return try {
            val docRef = quizzesCollection.add(result).await()
            
            // Update user stats
            firestore.runTransaction { transaction ->
                val userRef = usersCollection.document(result.userId)
                val snapshot = transaction.get(userRef)
                val quizzesCompleted = (snapshot.getLong("quizzesCompleted")?.toInt() ?: 0) + 1
                
                transaction.update(userRef, mapOf(
                    "quizzesCompleted" to quizzesCompleted,
                    "updatedAt" to Timestamp.now()
                ))
            }.await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Save quiz result failed", e)
            Result.failure(e)
        }
    }
    
    fun getQuizHistory(userId: String): Flow<List<FirebaseQuizResult>> = callbackFlow {
        val listener = quizzesCollection
            .whereEqualTo("userId", userId)
            .orderBy("completedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Quiz history error", error)
                    return@addSnapshotListener
                }
                val results = snapshot?.toObjects(FirebaseQuizResult::class.java) ?: emptyList()
                trySend(results)
            }
        
        awaitClose { listener.remove() }
    }
    
    // ===== FLASHCARD PROGRESS =====
    
    suspend fun saveFlashcardProgress(progress: FirebaseFlashcardProgress): Result<String> {
        return try {
            val docId = "${progress.userId}_${progress.subject}_${progress.topic}".replace(" ", "_")
            flashcardsCollection.document(docId).set(progress).await()
            
            // Update user stats
            firestore.runTransaction { transaction ->
                val userRef = usersCollection.document(progress.userId)
                transaction.update(userRef, mapOf(
                    "flashcardsCompleted" to (progress.masteredCards),
                    "updatedAt" to Timestamp.now()
                ))
            }.await()
            
            Result.success(docId)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Save flashcard progress failed", e)
            Result.failure(e)
        }
    }
    
    fun getFlashcardHistory(userId: String): Flow<List<FirebaseFlashcardProgress>> = callbackFlow {
        val listener = flashcardsCollection
            .whereEqualTo("userId", userId)
            .orderBy("lastReviewedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Flashcard history error", error)
                    return@addSnapshotListener
                }
                val results = snapshot?.toObjects(FirebaseFlashcardProgress::class.java) ?: emptyList()
                trySend(results)
            }
        
        awaitClose { listener.remove() }
    }
    
    // ===== ACHIEVEMENTS =====
    
    suspend fun unlockAchievement(userId: String, achievement: Achievement): Result<String> {
        return try {
            val firebaseAchievement = FirebaseAchievement(
                userId = userId,
                achievementId = achievement.id,
                title = achievement.title,
                description = achievement.description,
                emoji = achievement.emoji,
                xpReward = achievement.xpReward,
                unlockedAt = Timestamp.now()
            )
            
            val docRef = achievementsCollection.add(firebaseAchievement).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Unlock achievement failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUnlockedAchievements(userId: String): Result<List<FirebaseAchievement>> {
        return try {
            val snapshot = achievementsCollection
                .whereEqualTo("userId", userId)
                .orderBy("unlockedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val achievements = snapshot.toObjects(FirebaseAchievement::class.java)
            Result.success(achievements)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Get achievements failed", e)
            Result.failure(e)
        }
    }
    
    // ===== STUDY SESSIONS =====
    
    suspend fun createStudySession(session: StudySession): Result<String> {
        return try {
            val docRef = sessionsCollection.add(session).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Create session failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun endStudySession(sessionId: String, xpEarned: Int): Result<Unit> {
        return try {
            sessionsCollection.document(sessionId).update(mapOf(
                "endedAt" to Timestamp.now(),
                "xpEarned" to xpEarned
            )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "End session failed", e)
            Result.failure(e)
        }
    }
}
