package com.runanywhere.startup_hackathon20.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.runanywhere.startup_hackathon20.*

@Database(
    entities = [
        UserProgress::class,
        Achievement::class,
        QuizHistory::class,
        FlashcardProgress::class,
        CachedQuiz::class,
        CachedFlashcards::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudyChampDatabase : RoomDatabase() {
    abstract fun userProgressDao(): UserProgressDao
    abstract fun achievementDao(): AchievementDao
    abstract fun quizHistoryDao(): QuizHistoryDao
    abstract fun flashcardProgressDao(): FlashcardProgressDao
    abstract fun cachedQuizDao(): CachedQuizDao
    abstract fun cachedFlashcardsDao(): CachedFlashcardsDao

    companion object {
        @Volatile
        private var INSTANCE: StudyChampDatabase? = null

        fun getDatabase(context: Context): StudyChampDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyChampDatabase::class.java,
                    "study_champ_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
