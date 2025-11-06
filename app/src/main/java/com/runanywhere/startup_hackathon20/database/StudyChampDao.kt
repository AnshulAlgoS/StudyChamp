package com.runanywhere.startup_hackathon20.database

import androidx.room.*
import com.runanywhere.startup_hackathon20.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE userId = :userId LIMIT 1")
    fun getUserProgress(userId: String = "default_user"): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE userId = :userId LIMIT 1")
    suspend fun getUserProgressSync(userId: String = "default_user"): UserProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userProgress: UserProgress)

    @Update
    suspend fun update(userProgress: UserProgress)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, id ASC")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievementById(id: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<Achievement>)

    @Update
    suspend fun update(achievement: Achievement)
}

@Dao
interface QuizHistoryDao {
    @Query("SELECT * FROM quiz_history ORDER BY completedAt DESC")
    fun getAllQuizHistory(): Flow<List<QuizHistory>>

    @Query("SELECT * FROM quiz_history WHERE topic = :topic ORDER BY completedAt DESC LIMIT 5")
    suspend fun getQuizHistoryForTopic(topic: String): List<QuizHistory>

    @Insert
    suspend fun insert(quizHistory: QuizHistory)

    @Query("SELECT COUNT(*) FROM quiz_history")
    suspend fun getTotalQuizzesCompleted(): Int
}

@Dao
interface FlashcardProgressDao {
    @Query("SELECT * FROM flashcard_progress ORDER BY lastReviewedAt DESC")
    fun getAllProgress(): Flow<List<FlashcardProgress>>

    @Query("SELECT * FROM flashcard_progress WHERE id = :id LIMIT 1")
    suspend fun getProgressById(id: String): FlashcardProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: FlashcardProgress)

    @Query("SELECT SUM(masteredCards) FROM flashcard_progress")
    suspend fun getTotalMasteredCards(): Int?
}

@Dao
interface CachedQuizDao {
    @Query("SELECT * FROM cached_quizzes WHERE topicKey = :topicKey LIMIT 1")
    suspend fun getCachedQuiz(topicKey: String): CachedQuiz?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cachedQuiz: CachedQuiz)

    @Query("DELETE FROM cached_quizzes WHERE generatedAt < :timestamp")
    suspend fun deleteOldQuizzes(timestamp: Long)
}

@Dao
interface CachedFlashcardsDao {
    @Query("SELECT * FROM cached_flashcards WHERE topicKey = :topicKey LIMIT 1")
    suspend fun getCachedFlashcards(topicKey: String): CachedFlashcards?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cachedFlashcards: CachedFlashcards)

    @Query("DELETE FROM cached_flashcards WHERE generatedAt < :timestamp")
    suspend fun deleteOldFlashcards(timestamp: Long)
}
