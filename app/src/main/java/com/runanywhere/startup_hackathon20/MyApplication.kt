package com.runanywhere.startup_hackathon20

import android.app.Application
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.data.models.SDKEnvironment
import com.runanywhere.sdk.public.extensions.addModelFromURL
import com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider
import com.runanywhere.startup_hackathon20.repository.GamificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SDK asynchronously
        GlobalScope.launch(Dispatchers.IO) {
            initializeSDK()
            initializeGamification()
        }
    }

    private suspend fun initializeSDK() {
        try {
            // Step 1: Initialize SDK
            RunAnywhere.initialize(
                context = this@MyApplication,
                apiKey = "dev",  // Any string works in dev mode
                environment = SDKEnvironment.DEVELOPMENT
            )

            // Step 2: Register LLM Service Provider
            LlamaCppServiceProvider.register()

            // Step 3: Register Models
            registerModels()

            // Step 4: Scan for previously downloaded models
            RunAnywhere.scanForDownloadedModels()

            Log.i("MyApp", "SDK initialized successfully")

        } catch (e: Exception) {
            Log.e("MyApp", "SDK initialization failed: ${e.message}")
        }
    }

    private suspend fun initializeGamification() {
        try {
            val repository = GamificationRepository(this@MyApplication)

            // Initialize achievements in database
            repository.initializeAchievements()

            // Ensure user progress exists
            repository.getUserProgressSync()

            Log.i("MyApp", "Gamification system initialized")
        } catch (e: Exception) {
            Log.e("MyApp", "Gamification initialization failed: ${e.message}", e)
        }
    }

    private suspend fun registerModels() {
        // ============================================
        // 🏆 STUDY CHAMP AI MODELS - QWEN 2.5
        // ============================================
        // Using Qwen 2.5 - Publicly accessible, no auth required!
        // Better performance for educational tasks

        // PRIMARY MODEL: Qwen 2.5 1.5B - Best Quality (1.2 GB)
        // Excellent instruction following and reasoning
        addModelFromURL(
            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q6_k.gguf",
            name = "Qwen 2.5 1.5B Instruct Q6_K",
            type = "LLM"
        )
        Log.i("StudyChamp", "🎯 Registered Qwen 2.5 1.5B Q6_K - Premium Quality")

        // BALANCED MODEL: Qwen 2.5 1.5B Q5 (950 MB)
        // Good balance between quality and size
        addModelFromURL(
            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q5_k_m.gguf",
            name = "Qwen 2.5 1.5B Instruct Q5_K_M",
            type = "LLM"
        )
        Log.i("StudyChamp", "⚡ Registered Qwen 2.5 1.5B Q5_K_M - Balanced")

        // LIGHTWEIGHT MODEL: Qwen 2.5 1.5B Q4 (750 MB)
        // Fast and efficient for quick responses
        addModelFromURL(
            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            name = "Qwen 2.5 1.5B Instruct Q4_K_M",
            type = "LLM"
        )
        Log.i("StudyChamp", "🚀 Registered Qwen 2.5 1.5B Q4_K_M - Fast Mode")

        Log.i("StudyChamp", "✅ ALL QWEN MODELS REGISTERED - Ready to learn! 🏆")
    }
}
