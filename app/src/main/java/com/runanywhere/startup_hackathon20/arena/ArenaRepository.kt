package com.runanywhere.startup_hackathon20.arena

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class ArenaRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val arenaCollection = firestore.collection("arenas")
    private val resultsCollection = firestore.collection("arena_results")
    private val usersCollection = firestore.collection("users")

    /**
     * Get user's actual name from Firestore user profile
     */
    private suspend fun getUserName(userId: String): String {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            userDoc.getString("name") ?: auth.currentUser?.displayName ?: "Student"
        } catch (e: Exception) {
            auth.currentUser?.displayName ?: "Student"
        }
    }

    /**
     * Generate unique 6-character room code
     */
    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Create a new Arena room
     */
    suspend fun createRoom(
        subject: String,
        task: String,
        taskCount: Int,
        timeLimit: Int,
        strictFocusMode: Boolean,
        allowedApps: List<String> = emptyList()
    ): Result<ArenaRoom> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

            val roomCode = generateRoomCode()
            val roomId = arenaCollection.document().id
            val userName = getUserName(currentUser.uid)

            val room = ArenaRoom(
                roomId = roomId,
                roomCode = roomCode,
                hostUserId = currentUser.uid,
                hostName = userName,
                subject = subject,
                task = task,
                taskCount = taskCount,
                timeLimit = timeLimit,
                status = RoomStatus.WAITING,
                strictFocusMode = strictFocusMode,
                allowedApps = allowedApps,
                createdAt = Timestamp.now()
            )

            arenaCollection.document(roomId).set(room).await()

            // Auto-join as host
            joinRoom(roomId)

            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Join room by room code
     */
    suspend fun joinRoomByCode(roomCode: String): Result<ArenaRoom> {
        return try {
            val query = arenaCollection
                .whereEqualTo("roomCode", roomCode)
                .whereEqualTo("status", RoomStatus.WAITING.name)
                .get()
                .await()

            if (query.isEmpty) {
                return Result.failure(Exception("Room not found or already started"))
            }

            val room = query.documents[0].toObject(ArenaRoom::class.java)
                ?: return Result.failure(Exception("Invalid room data"))

            // Check if room is full
            if (room.participants.size >= room.maxParticipants) {
                return Result.failure(Exception("Room is full"))
            }

            // Join the room
            joinRoom(room.roomId)

            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Join room by room ID
     */
    suspend fun joinRoom(roomId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            val userName = getUserName(currentUser.uid)

            val participant = ArenaParticipant(
                userId = currentUser.uid,
                userName = userName,
                avatarUrl = currentUser.photoUrl?.toString() ?: "",
                status = ParticipantStatus.READY,
                joinedAt = Timestamp.now(),
                lastSeenAt = Timestamp.now()
            )

            arenaCollection.document(roomId)
                .update("participants.${currentUser.uid}", participant)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leave room
     */
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

            // Update status to QUIT
            arenaCollection.document(roomId)
                .update(
                    mapOf(
                        "participants.${currentUser.uid}.status" to ParticipantStatus.QUIT.name,
                        "participants.${currentUser.uid}.lastSeenAt" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start the arena (host only)
     */
    suspend fun startArena(roomId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

            // Verify user is host
            val room = arenaCollection.document(roomId).get().await()
                .toObject(ArenaRoom::class.java)
                ?: return Result.failure(Exception("Room not found"))

            if (room.hostUserId != currentUser.uid) {
                return Result.failure(Exception("Only host can start arena"))
            }

            // Update room status
            arenaCollection.document(roomId)
                .update(
                    mapOf(
                        "status" to RoomStatus.ACTIVE.name,
                        "startedAt" to Timestamp.now()
                    )
                )
                .await()

            // Update all participants to ACTIVE
            room.participants.keys.forEach { userId ->
                arenaCollection.document(roomId)
                    .update("participants.$userId.status", ParticipantStatus.ACTIVE.name)
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update participant progress
     */
    suspend fun updateProgress(
        roomId: String,
        tasksCompleted: Int,
        focusTimeSeconds: Int,
        currentStreak: Int
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

            arenaCollection.document(roomId)
                .update(
                    mapOf(
                        "participants.${currentUser.uid}.tasksCompleted" to tasksCompleted,
                        "participants.${currentUser.uid}.focusTimeSeconds" to focusTimeSeconds,
                        "participants.${currentUser.uid}.currentStreak" to currentStreak,
                        "participants.${currentUser.uid}.lastSeenAt" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark task as completed
     */
    suspend fun completeTask(roomId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

            // Get current room state to determine finish position
            val room = arenaCollection.document(roomId).get().await()
                .toObject(ArenaRoom::class.java)
                ?: return Result.failure(Exception("Room not found"))

            // Calculate finish position
            val completedCount = room.participants.values.count {
                it.status == ParticipantStatus.COMPLETED
            }
            val finishPosition = completedCount + 1

            arenaCollection.document(roomId)
                .update(
                    mapOf(
                        "participants.${currentUser.uid}.status" to ParticipantStatus.COMPLETED.name,
                        "participants.${currentUser.uid}.finishPosition" to finishPosition,
                        "participants.${currentUser.uid}.completedAt" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record a violation
     */
    suspend fun recordViolation(
        roomId: String,
        type: ViolationType,
        description: String
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            val userName = getUserName(currentUser.uid)

            val violation = ArenaViolation(
                userId = currentUser.uid,
                userName = userName,
                type = type,
                description = description,
                timestamp = Timestamp.now()
            )

            // Add violation to subcollection
            arenaCollection.document(roomId)
                .collection("violations")
                .add(violation)
                .await()

            // Increment violation count
            arenaCollection.document(roomId)
                .update(
                    "participants.${currentUser.uid}.violations",
                    com.google.firebase.firestore.FieldValue.increment(1)
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send chat message
     */
    suspend fun sendMessage(
        roomId: String,
        message: String,
        type: MessageType = MessageType.CHAT
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            val userName = getUserName(currentUser.uid)

            val chatMessage = ArenaMessage(
                userId = currentUser.uid,
                userName = userName,
                message = message,
                timestamp = Timestamp.now(),
                type = type
            )

            arenaCollection.document(roomId)
                .collection("messages")
                .add(chatMessage)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * End arena and calculate results
     */
    suspend fun endArena(roomId: String): Result<ArenaResult> {
        return try {
            val room = arenaCollection.document(roomId).get().await()
                .toObject(ArenaRoom::class.java)
                ?: return Result.failure(Exception("Room not found"))

            // Calculate XP for each participant
            val participantResults = room.participants.map { (userId, participant) ->
                val xp = ArenaXPCalculator.calculateXP(
                    taskCompleted = participant.tasksCompleted >= room.taskCount,
                    tasksCompleted = participant.tasksCompleted,
                    totalTasks = room.taskCount,
                    focusTimeMinutes = participant.focusTimeSeconds / 60,
                    finishPosition = participant.finishPosition,
                    violations = participant.violations
                )

                // Update participant's final XP
                arenaCollection.document(roomId)
                    .update("participants.$userId.finalXP", xp)
                    .await()

                ArenaParticipantResult(
                    userId = userId,
                    userName = participant.userName,
                    position = participant.finishPosition,
                    tasksCompleted = participant.tasksCompleted,
                    totalTasks = room.taskCount,
                    focusTimeMinutes = participant.focusTimeSeconds / 60,
                    violations = participant.violations,
                    xpEarned = xp
                )
            }.sortedBy { it.position }

            // Determine winner (highest XP or first to finish)
            val winner = participantResults.maxByOrNull { it.xpEarned }
                ?: participantResults.firstOrNull()
                ?: return Result.failure(Exception("No participants"))

            // Create result
            val result = ArenaResult(
                roomId = roomId,
                roomCode = room.roomCode,
                subject = room.subject,
                task = room.task,
                winnerId = winner.userId,
                winnerName = winner.userName,
                winnerXP = winner.xpEarned,
                participants = participantResults,
                totalFocusTime = participantResults.sumOf { it.focusTimeMinutes },
                averageCompletion = participantResults.map {
                    it.tasksCompleted.toFloat() / it.totalTasks
                }.average().toFloat(),
                completedAt = Timestamp.now()
            )

            // Update room status
            arenaCollection.document(roomId)
                .update(
                    mapOf(
                        "status" to RoomStatus.COMPLETED.name,
                        "endedAt" to Timestamp.now()
                    )
                )
                .await()

            // Save result
            resultsCollection.add(result).await()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe room updates in real-time
     */
    fun observeRoom(roomId: String): Flow<ArenaRoom> = callbackFlow {
        val listener = arenaCollection.document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                snapshot?.toObject(ArenaRoom::class.java)?.let { room ->
                    trySend(room)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe chat messages
     */
    fun observeMessages(roomId: String): Flow<List<ArenaMessage>> = callbackFlow {
        val listener = arenaCollection.document(roomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(ArenaMessage::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe violations
     */
    fun observeViolations(roomId: String): Flow<List<ArenaViolation>> = callbackFlow {
        val listener = arenaCollection.document(roomId)
            .collection("violations")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val violations = snapshot?.documents?.mapNotNull {
                    it.toObject(ArenaViolation::class.java)
                } ?: emptyList()

                trySend(violations)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get user's arena history
     */
    suspend fun getUserArenaHistory(userId: String): Result<List<ArenaResult>> {
        return try {
            val results = resultsCollection
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ArenaResult::class.java) }
                .filter { result ->
                    result.participants.any { it.userId == userId }
                }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Find user's active arena room
     */
    suspend fun findActiveArena(userId: String): Result<ArenaRoom?> {
        return try {
            // Check for WAITING or ACTIVE rooms where user is a participant
            val activeRooms = arenaCollection
                .whereIn("status", listOf(RoomStatus.WAITING.name, RoomStatus.ACTIVE.name))
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ArenaRoom::class.java) }
                .filter { room ->
                    room.participants.containsKey(userId) &&
                            room.participants[userId]?.status != ParticipantStatus.QUIT
                }

            Result.success(activeRooms.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rejoin room by room ID
     */
    suspend fun rejoinRoom(roomId: String): Result<ArenaRoom> {
        return try {
            val room = arenaCollection.document(roomId).get().await()
                .toObject(ArenaRoom::class.java)
                ?: return Result.failure(Exception("Room not found"))

            // Update last seen
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            arenaCollection.document(roomId)
                .update("participants.${currentUser.uid}.lastSeenAt", Timestamp.now())
                .await()

            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
