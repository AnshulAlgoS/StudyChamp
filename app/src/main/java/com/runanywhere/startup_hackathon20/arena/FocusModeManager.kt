package com.runanywhere.startup_hackathon20.arena

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Focus Mode Manager - Detects app switches and monitors focus
 */
class FocusModeManager(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager

    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring

    private val _focusTimeSeconds = MutableStateFlow(0)
    val focusTimeSeconds: StateFlow<Int> = _focusTimeSeconds

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak

    private val _violations = MutableStateFlow(0)
    val violations: StateFlow<Int> = _violations

    private var allowedApps: Set<String> = emptySet()
    private var lastFocusCheck = System.currentTimeMillis()
    private var sessionStartTime = 0L
    private var totalFocusTime = 0L
    private var currentStreakStart = 0L

    // Callbacks
    private var onViolation: ((ViolationType, String) -> Unit)? = null
    private var onFocusGained: (() -> Unit)? = null
    private var onFocusLost: (() -> Unit)? = null

    companion object {
        private const val TAG = "FocusModeManager"
        private const val CHECK_INTERVAL = 3000L // Check every 3 seconds
        private const val IDLE_THRESHOLD = 60000L // 1 minute idle = violation
    }

    /**
     * Start monitoring focus
     */
    fun startMonitoring(
        allowedApps: List<String> = emptyList(),
        onViolation: (ViolationType, String) -> Unit,
        onFocusGained: () -> Unit = {},
        onFocusLost: () -> Unit = {}
    ) {
        if (_isMonitoring.value) {
            Log.w(TAG, "Already monitoring")
            return
        }

        this.allowedApps = (allowedApps + context.packageName).toSet()
        this.onViolation = onViolation
        this.onFocusGained = onFocusGained
        this.onFocusLost = onFocusLost

        sessionStartTime = System.currentTimeMillis()
        currentStreakStart = sessionStartTime
        lastFocusCheck = sessionStartTime

        _isMonitoring.value = true
        _focusTimeSeconds.value = 0
        _currentStreak.value = 0
        _violations.value = 0

        startMonitoringLoop()

        Log.d(TAG, "Focus monitoring started. Allowed apps: ${this.allowedApps}")
    }

    /**
     * Stop monitoring focus
     */
    fun stopMonitoring() {
        _isMonitoring.value = false
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        monitoringRunnable = null

        Log.d(TAG, "Focus monitoring stopped. Total focus time: ${_focusTimeSeconds.value}s")
    }

    /**
     * Main monitoring loop
     */
    private fun startMonitoringLoop() {
        monitoringRunnable = object : Runnable {
            override fun run() {
                if (!_isMonitoring.value) return

                checkFocus()

                // Schedule next check
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }
        handler.post(monitoringRunnable!!)
    }

    /**
     * Check if user is focused on StudyChamp or allowed apps
     */
    private fun checkFocus() {
        try {
            val currentApp = getCurrentForegroundApp()
            val currentTime = System.currentTimeMillis()
            val timeSinceLastCheck = currentTime - lastFocusCheck

            if (currentApp == null) {
                Log.w(TAG, "Could not determine current app")
                return
            }

            Log.d(TAG, "Current app: $currentApp, Allowed: ${allowedApps.contains(currentApp)}")

            // Check if user is in StudyChamp or allowed app
            if (allowedApps.contains(currentApp)) {
                // User is focused!
                handleFocusGained(timeSinceLastCheck)
            } else {
                // User left StudyChamp - VIOLATION!
                handleViolation(
                    ViolationType.LEFT_APP,
                    "Switched to: ${getAppName(currentApp)}"
                )
            }

            // Check for idle time
            if (timeSinceLastCheck > IDLE_THRESHOLD) {
                handleViolation(
                    ViolationType.IDLE_TOO_LONG,
                    "No activity for ${timeSinceLastCheck / 1000}s"
                )
            }

            lastFocusCheck = currentTime

        } catch (e: Exception) {
            Log.e(TAG, "Error checking focus", e)
        }
    }

    /**
     * Get current foreground app package name
     */
    private fun getCurrentForegroundApp(): String? {
        try {
            if (usageStatsManager == null) {
                Log.w(TAG, "UsageStatsManager not available")
                return null
            }

            val currentTime = System.currentTimeMillis()
            val usageEvents = usageStatsManager.queryEvents(
                currentTime - CHECK_INTERVAL - 1000, // Look back a bit more
                currentTime
            )

            var lastApp: String? = null
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)

                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                ) {
                    lastApp = event.packageName
                }
            }

            return lastApp ?: context.packageName

        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app", e)
            return context.packageName
        }
    }

    /**
     * Handle focus gained
     */
    private fun handleFocusGained(timeDelta: Long) {
        // Add to focus time
        val secondsToAdd = (timeDelta / 1000).toInt()
        _focusTimeSeconds.value += secondsToAdd

        // Update streak
        val streakSeconds = ((System.currentTimeMillis() - currentStreakStart) / 1000).toInt()
        _currentStreak.value = streakSeconds

        onFocusGained?.invoke()
    }

    /**
     * Handle violation
     */
    private fun handleViolation(type: ViolationType, description: String) {
        Log.w(TAG, "VIOLATION: $type - $description")

        // Increment violation count
        _violations.value += 1

        // Reset current streak
        _currentStreak.value = 0
        currentStreakStart = System.currentTimeMillis()

        // Notify callback
        onViolation?.invoke(type, description)
        onFocusLost?.invoke()

        // Try to bring StudyChamp back to front
        bringAppToFront()
    }

    /**
     * Bring StudyChamp to foreground
     */
    private fun bringAppToFront() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bring app to front", e)
        }
    }

    /**
     * Get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * Get current focus stats
     */
    fun getFocusStats(): FocusStats {
        val totalTime = (System.currentTimeMillis() - sessionStartTime) / 1000
        val focusScore = if (totalTime > 0) {
            (_focusTimeSeconds.value.toFloat() / totalTime * 100).coerceIn(0f, 100f)
        } else {
            0f
        }

        return FocusStats(
            totalTimeSeconds = totalTime.toInt(),
            focusTimeSeconds = _focusTimeSeconds.value,
            currentStreakSeconds = _currentStreak.value,
            violations = _violations.value,
            focusScore = focusScore
        )
    }

    /**
     * Reset stats
     */
    fun reset() {
        _focusTimeSeconds.value = 0
        _currentStreak.value = 0
        _violations.value = 0
        sessionStartTime = System.currentTimeMillis()
        currentStreakStart = sessionStartTime
        lastFocusCheck = sessionStartTime
        totalFocusTime = 0L
    }
}

data class FocusStats(
    val totalTimeSeconds: Int,
    val focusTimeSeconds: Int,
    val currentStreakSeconds: Int,
    val violations: Int,
    val focusScore: Float
)
