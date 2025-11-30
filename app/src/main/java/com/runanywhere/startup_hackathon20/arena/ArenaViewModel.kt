package com.runanywhere.startup_hackathon20.arena

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Focus Arena
 */
class ArenaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArenaRepository()
    private val focusModeManager = FocusModeManager(application)
    private val auth = FirebaseAuth.getInstance()

    // Current room state
    private val _currentRoom = MutableStateFlow<ArenaRoom?>(null)
    val currentRoom: StateFlow<ArenaRoom?> = _currentRoom

    private val _roomStatus = MutableStateFlow<RoomStatus?>(null)
    val roomStatus: StateFlow<RoomStatus?> = _roomStatus

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _statusMessage = MutableStateFlow("Welcome to Focus Arena!")
    val statusMessage: StateFlow<String> = _statusMessage

    // Chat & Violations
    private val _messages = MutableStateFlow<List<ArenaMessage>>(emptyList())
    val messages: StateFlow<List<ArenaMessage>> = _messages

    private val _violations = MutableStateFlow<List<ArenaViolation>>(emptyList())
    val violations: StateFlow<List<ArenaViolation>> = _violations

    // Focus stats
    val focusStats = focusModeManager.focusTimeSeconds
        .combine(focusModeManager.currentStreak) { focusTime, streak ->
            FocusStats(
                totalTimeSeconds = focusTime,
                focusTimeSeconds = focusTime,
                currentStreakSeconds = streak,
                violations = focusModeManager.violations.value,
                focusScore = 100f
            )
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, FocusStats(0, 0, 0, 0, 0f))

    // Results
    private val _arenaResult = MutableStateFlow<ArenaResult?>(null)
    val arenaResult: StateFlow<ArenaResult?> = _arenaResult

    // User history
    private val _arenaHistory = MutableStateFlow<List<ArenaResult>>(emptyList())
    val arenaHistory: StateFlow<List<ArenaResult>> = _arenaHistory

    // Current user's participant data
    val currentParticipant: StateFlow<ArenaParticipant?> = _currentRoom
        .map { room ->
            auth.currentUser?.uid?.let { userId ->
                room?.participants?.get(userId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Leaderboard (sorted by position or current progress)
    val leaderboard: StateFlow<List<ArenaParticipant>> = _currentRoom
        .map { room ->
            room?.participants?.values?.sortedWith(
                compareBy<ArenaParticipant> { it.finishPosition }
                    .thenByDescending { it.tasksCompleted }
                    .thenByDescending { it.focusTimeSeconds }
            )?.filter { it.status != ParticipantStatus.QUIT } ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadArenaHistory()
        checkForActiveArena()

        // Observe auth state changes to reload data for new user
        viewModelScope.launch {
            var previousUserId = auth.currentUser?.uid

            kotlinx.coroutines.delay(1000)
            while (true) {
                val currentUserId = auth.currentUser?.uid

                // If user changed, reload everything
                if (currentUserId != previousUserId) {
                    android.util.Log.d(
                        "ArenaVM",
                        "User changed from $previousUserId to $currentUserId"
                    )

                    // Clear current room and data
                    _currentRoom.value = null
                    _arenaResult.value = null
                    _messages.value = emptyList()
                    _violations.value = emptyList()

                    // Reload history for new user
                    if (currentUserId != null) {
                        loadArenaHistory()
                        checkForActiveArena()
                    } else {
                        _arenaHistory.value = emptyList()
                    }

                    previousUserId = currentUserId
                }

                kotlinx.coroutines.delay(2000)
            }
        }
    }

    // ===== ROOM MANAGEMENT =====

    /**
     * Create a new arena room
     */
    fun createRoom(
        subject: String,
        task: String,
        taskCount: Int,
        timeLimit: Int,
        strictFocusMode: Boolean,
        allowedApps: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.createRoom(
                    subject = subject,
                    task = task,
                    taskCount = taskCount,
                    timeLimit = timeLimit,
                    strictFocusMode = strictFocusMode,
                    allowedApps = allowedApps
                )

                when {
                    result.isSuccess -> {
                        val room = result.getOrNull()!!
                        _currentRoom.value = room
                        _statusMessage.value = "Room created! Code: ${room.roomCode}"
                        observeRoom(room.roomId)
                    }

                    result.isFailure -> {
                        _errorMessage.value =
                            "Failed to create room: ${result.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Join room by code
     */
    fun joinRoomByCode(roomCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.joinRoomByCode(roomCode.uppercase())

                when {
                    result.isSuccess -> {
                        val room = result.getOrNull()!!
                        _currentRoom.value = room
                        _statusMessage.value = "Joined arena! Get ready!"
                        observeRoom(room.roomId)
                    }

                    result.isFailure -> {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to join"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Leave current room
     */
    fun leaveRoom() {
        viewModelScope.launch {
            val roomId = _currentRoom.value?.roomId ?: return@launch

            try {
                repository.leaveRoom(roomId)
                focusModeManager.stopMonitoring()
                _currentRoom.value = null
                _statusMessage.value = "Left arena"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to leave: ${e.message}"
            }
        }
    }

    /**
     * Start the arena (host only)
     */
    fun startArena() {
        viewModelScope.launch {
            val roomId = _currentRoom.value?.roomId ?: return@launch

            _isLoading.value = true

            try {
                val result = repository.startArena(roomId)

                when {
                    result.isSuccess -> {
                        _statusMessage.value = "Arena started! Focus time!"
                        startFocusMode()
                    }

                    result.isFailure -> {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to start"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ===== FOCUS MODE =====

    /**
     * Start focus mode monitoring
     */
    private fun startFocusMode() {
        val room = _currentRoom.value ?: return

        focusModeManager.startMonitoring(
            allowedApps = room.allowedApps,
            onViolation = { type, description ->
                handleViolation(type, description)
            },
            onFocusGained = {
                // User regained focus
            },
            onFocusLost = {
                // User lost focus
            }
        )

        // Start periodic progress updates
        startProgressUpdates()
    }

    /**
     * Handle focus violation
     */
    private fun handleViolation(type: ViolationType, description: String) {
        viewModelScope.launch {
            val roomId = _currentRoom.value?.roomId ?: return@launch

            // Record violation in Firebase
            repository.recordViolation(roomId, type, description)

            // Send system message
            repository.sendMessage(
                roomId = roomId,
                message = "⚠️ Violation: $description",
                type = MessageType.VIOLATION
            )
        }
    }

    /**
     * Periodically update progress
     */
    private fun startProgressUpdates() {
        viewModelScope.launch {
            val roomId = _currentRoom.value?.roomId ?: return@launch

            // Update every 5 seconds
            kotlinx.coroutines.delay(5000)

            while (_currentRoom.value?.status == RoomStatus.ACTIVE) {
                val stats = focusModeManager.getFocusStats()

                repository.updateProgress(
                    roomId = roomId,
                    tasksCompleted = currentParticipant.value?.tasksCompleted ?: 0,
                    focusTimeSeconds = stats.focusTimeSeconds,
                    currentStreak = stats.currentStreakSeconds
                )

                kotlinx.coroutines.delay(5000)
            }
        }
    }

    // ===== TASK PROGRESS =====

    /**
     * Update task progress
     */
    fun updateTaskProgress(tasksCompleted: Int) {
        viewModelScope.launch {
            val roomId = _currentRoom.value?.roomId ?: return@launch
            val stats = focusModeManager.getFocusStats()

            repository.updateProgress(
                roomId = roomId,
                tasksCompleted = tasksCompleted,
                focusTimeSeconds = stats.focusTimeSeconds,
                currentStreak = stats.currentStreakSeconds
            )
        }
    }

    /**
     * Mark task as completed
     */
    fun completeTask() {
        viewModelScope.launch {
            val roomId = _currentRoom.value?.roomId ?: return@launch

            try {
                repository.completeTask(roomId)
                _statusMessage.value = "Task completed! Waiting for others..."

                // Check if all participants finished
                checkIfArenaComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to complete task: ${e.message}"
            }
        }
    }

    /**
     * Check if arena should end
     */
    private fun checkIfArenaComplete() {
        viewModelScope.launch {
            val room = _currentRoom.value ?: return@launch

            val allCompleted = room.participants.values
                .filter { it.status != ParticipantStatus.QUIT }
                .all { it.status == ParticipantStatus.COMPLETED }

            if (allCompleted) {
                endArena()
            }
        }
    }

    /**
     * End the arena and show results
     */
    fun endArena() {
        viewModelScope.launch {
            val roomId = _currentRoom.value?.roomId ?: return@launch

            _isLoading.value = true

            try {
                focusModeManager.stopMonitoring()

                val result = repository.endArena(roomId)

                when {
                    result.isSuccess -> {
                        _arenaResult.value = result.getOrNull()
                        _statusMessage.value = "Arena complete!"
                    }

                    result.isFailure -> {
                        _errorMessage.value =
                            "Failed to end arena: ${result.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ===== CHAT =====

    /**
     * Send chat message
     */
    fun sendMessage(message: String) {
        viewModelScope.launch {
            val roomId = _currentRoom.value?.roomId ?: return@launch

            try {
                repository.sendMessage(roomId, message, MessageType.CHAT)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send message: ${e.message}"
            }
        }
    }

    // ===== OBSERVERS =====

    /**
     * Observe room updates
     */
    private fun observeRoom(roomId: String) {
        viewModelScope.launch {
            repository.observeRoom(roomId).collect { room ->
                _currentRoom.value = room
                _roomStatus.value = room.status

                // Auto-start focus mode when arena starts
                if (room.status == RoomStatus.ACTIVE && !focusModeManager.isMonitoring.value) {
                    startFocusMode()
                }
            }
        }

        // Observe messages
        viewModelScope.launch {
            repository.observeMessages(roomId).collect { messages ->
                _messages.value = messages
            }
        }

        // Observe violations
        viewModelScope.launch {
            repository.observeViolations(roomId).collect { violations ->
                _violations.value = violations
            }
        }
    }

    /**
     * Load user's arena history
     */
    private fun loadArenaHistory() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                val result = repository.getUserArenaHistory(userId)
                when {
                    result.isSuccess -> {
                        _arenaHistory.value = result.getOrNull() ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                // Silent fail for history
            }
        }
    }

    /**
     * Check if user has an active arena to rejoin
     */
    private fun checkForActiveArena() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                val result = repository.findActiveArena(userId)
                when {
                    result.isSuccess -> {
                        val activeRoom = result.getOrNull()
                        if (activeRoom != null && _currentRoom.value == null) {
                            // Found an active room, but don't auto-join
                            // User will see "Resume Active Arena" button
                            android.util.Log.d(
                                "ArenaVM",
                                "Found active arena: ${activeRoom.roomCode}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    /**
     * Rejoin active arena
     */
    fun rejoinActiveArena() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.findActiveArena(userId)
                when {
                    result.isSuccess -> {
                        val activeRoom = result.getOrNull()
                        if (activeRoom != null) {
                            _currentRoom.value = activeRoom
                            _statusMessage.value = "Rejoined arena!"
                            observeRoom(activeRoom.roomId)
                        } else {
                            _errorMessage.value = "No active arena found"
                        }
                    }

                    result.isFailure -> {
                        _errorMessage.value =
                            "Failed to rejoin: ${result.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ===== CLEANUP =====

    override fun onCleared() {
        super.onCleared()
        focusModeManager.stopMonitoring()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Reset arena result (for new game)
     */
    fun resetResult() {
        _arenaResult.value = null
        _currentRoom.value = null
    }
}
