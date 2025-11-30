package com.runanywhere.startup_hackathon20.arena

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Main Focus Arena Screen - Entry point
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusArenaMainScreen(
    viewModel: ArenaViewModel,
    onNavigateBack: () -> Unit
) {
    val currentRoom by viewModel.currentRoom.collectAsState()
    val arenaResult by viewModel.arenaResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Show error if any
    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    when {
        arenaResult != null -> {
            // Show results screen
            ArenaResultScreen(
                result = arenaResult!!,
                onPlayAgain = {
                    viewModel.resetResult()
                },
                onExit = {
                    viewModel.resetResult()
                    onNavigateBack()
                }
            )
        }

        currentRoom != null -> {
            // Show arena lobby or active session
            when (currentRoom!!.status) {
                RoomStatus.WAITING, RoomStatus.STARTING -> {
                    ArenaLobbyScreen(
                        viewModel = viewModel,
                        room = currentRoom!!,
                        onBack = {
                            viewModel.leaveRoom()
                            onNavigateBack()
                        }
                    )
                }

                RoomStatus.ACTIVE -> {
                    ArenaActiveSessionScreen(
                        viewModel = viewModel,
                        room = currentRoom!!,
                        onBack = {
                            viewModel.leaveRoom()
                            onNavigateBack()
                        }
                    )
                }

                RoomStatus.COMPLETED -> {
                    // Waiting for results
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Calculating results...")
                    }
                }
            }
        }

        else -> {
            // Show menu (Create/Join)
            ArenaMenuScreen(
                viewModel = viewModel,
                onBack = onNavigateBack
            )
        }
    }
}

/**
 * Arena Menu - Create or Join
 */
@Composable
fun ArenaMenuScreen(
    viewModel: ArenaViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val arenaHistory by viewModel.arenaHistory.collectAsState()
    val currentRoom by viewModel.currentRoom.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 20.dp, top = 24.dp)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.92f), CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF4E6AF6)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Resume Active Arena Button (if exists)
            currentRoom?.let { room ->
                if (room.status == RoomStatus.WAITING || room.status == RoomStatus.ACTIVE) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Already have the room, just navigate to it
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFF6B35)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (room.status == RoomStatus.ACTIVE)
                                            "ARENA IN PROGRESS!"
                                        else
                                            "ARENA WAITING",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Room: ${room.roomCode} - ${room.subject}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = "${room.participants.size} participants",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Permission Card (if not granted)
            if (!context.hasUsageStatsPermission()) {
                item {
                    FocusModePermissionCard(
                        onRequestPermission = { showPermissionDialog = true }
                    )
                }
            }

            // Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0xFF6B46C1).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = null,
                            tint = Color(0xFF6B46C1),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Focus Arena",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Compete with friends in real-time!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }

            // Active Arena Banner (if exists)
            if (arenaHistory.isNotEmpty()) {
                val recentArena = arenaHistory.firstOrNull()
                recentArena?.let { result ->
                    // Check if this arena might still be active (completed recently)
                    val now = com.google.firebase.Timestamp.now()
                    val timeSinceCompletion = now.seconds - result.completedAt.seconds

                    // If completed less than 5 minutes ago, don't show resume
                    if (timeSinceCompletion > 300) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Try to resume - not applicable for completed arenas */ },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Active Arena",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Tap to resume: ${result.subject}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Create Room Button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateDialog = true },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF6B46C1)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Create Room",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Start a new challenge",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Join Room Button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showJoinDialog = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFF4E6AF6).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF4E6AF6),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Join Room",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Enter room code",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Arena History
            if (arenaHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Arenas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(arenaHistory.take(3)) { result ->
                    ArenaHistoryCard(result)
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateRoomDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showJoinDialog) {
        JoinRoomDialog(
            viewModel = viewModel,
            onDismiss = { showJoinDialog = false }
        )
    }

    if (showPermissionDialog) {
        FocusModePermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onGranted = { /* Permission granted */ }
        )
    }
}

/**
 * Create Room Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomDialog(
    viewModel: ArenaViewModel,
    onDismiss: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var task by remember { mutableStateOf("") }
    var taskCount by remember { mutableStateOf("20") }
    var timeLimit by remember { mutableStateOf("60") }
    var strictMode by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Focus Arena") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        placeholder = { Text("e.g., Math, Physics") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = task,
                        onValueChange = { task = it },
                        label = { Text("Task") },
                        placeholder = { Text("e.g., Complete 20 problems") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = taskCount,
                        onValueChange = { if (it.all { c -> c.isDigit() }) taskCount = it },
                        label = { Text("Number of Tasks") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = timeLimit,
                        onValueChange = { if (it.all { c -> c.isDigit() }) timeLimit = it },
                        label = { Text("Time Limit (minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Strict Focus Mode")
                        Switch(
                            checked = strictMode,
                            onCheckedChange = { strictMode = it }
                        )
                    }
                    Text(
                        text = "Blocks other apps during focus session",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.createRoom(
                        subject = subject.ifBlank { "Study" },
                        task = task.ifBlank { "Complete tasks" },
                        taskCount = taskCount.toIntOrNull() ?: 20,
                        timeLimit = timeLimit.toIntOrNull() ?: 60,
                        strictFocusMode = strictMode
                    )
                    onDismiss()
                },
                enabled = subject.isNotBlank() && task.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Join Room Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomDialog(
    viewModel: ArenaViewModel,
    onDismiss: () -> Unit
) {
    var roomCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Arena") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the 6-character room code")
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
                    label = { Text("Room Code") },
                    placeholder = { Text("MATH01") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.joinRoomByCode(roomCode)
                    onDismiss()
                },
                enabled = roomCode.length == 6
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Arena History Card
 */
@Composable
fun ArenaHistoryCard(result: ArenaResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = result.task,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${result.winnerXP} XP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E6AF6)
                )
                Text(
                    text = "${result.participants.size} players",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Arena Lobby - Waiting room before game starts
 */
@Composable
fun ArenaLobbyScreen(
    viewModel: ArenaViewModel,
    room: ArenaRoom,
    onBack: () -> Unit
) {
    val isHost =
        room.hostUserId == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val participants = room.participants.values.toList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 20.dp, top = 24.dp)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.92f), CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF4E6AF6)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(40.dp)) }

            // Room Code
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF6B46C1)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Room Code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = room.roomCode,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 4.sp
                        )
                        Text(
                            text = "Share this with friends!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Challenge Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Challenge",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = null,
                                tint = Color(0xFF4E6AF6),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = room.subject,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4E6AF6),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = room.task,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${room.taskCount} tasks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${room.timeLimit} min",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Participants
            item {
                Text(
                    text = "Players (${participants.size}/${room.maxParticipants})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            items(participants) { participant ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF4E6AF6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = participant.userName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = participant.userName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    if (participant.userId == room.hostUserId) {
                                        Text(
                                            text = "HOST",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFC107),
                                            modifier = Modifier
                                                .background(
                                                    Color(0xFFFFC107).copy(alpha = 0.2f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Start Button (host only)
            if (isHost) {
                item {
                    Button(
                        onClick = { viewModel.startArena() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B46C1)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = participants.size >= 2
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Arena",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Active Arena Session - Main competition screen
 */
@Composable
fun ArenaActiveSessionScreen(
    viewModel: ArenaViewModel,
    room: ArenaRoom,
    onBack: () -> Unit
) {
    val currentParticipant by viewModel.currentParticipant.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val focusStats by viewModel.focusStats.collectAsState()
    var tasksCompleted by remember { mutableStateOf(currentParticipant?.tasksCompleted ?: 0) }

    // Countdown timer - real-time calculation
    var timeRemaining by remember { mutableStateOf(room.timeLimit * 60) } // in seconds

    LaunchedEffect(Unit) {
        val startTime = room.startedAt?.seconds ?: (System.currentTimeMillis() / 1000)

        while (timeRemaining > 0) {
            val now = System.currentTimeMillis() / 1000
            val elapsedSeconds = (now - startTime).toInt()
            val totalSeconds = room.timeLimit * 60
            timeRemaining = maxOf(0, totalSeconds - elapsedSeconds)

            // End arena when time runs out
            if (timeRemaining <= 0) {
                viewModel.endArena()
                break
            }

            delay(1000) // Update every second
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (timeRemaining <= 5) Color(0xFFFF5252) else Color(
                            0xFF6B46C1
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Time Remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = String.format(
                                "%02d:%02d",
                                timeRemaining / 60,
                                timeRemaining % 60
                            ),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Focus Stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Your Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${focusStats.focusScore.toInt()}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4E6AF6)
                                )
                                Text(
                                    text = "Focus Score",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${focusStats.violations}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                                Text(
                                    text = "Violations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${focusStats.currentStreakSeconds / 60}m",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "Streak",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Task Progress
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tasks Completed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "$tasksCompleted / ${room.taskCount}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4E6AF6)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { tasksCompleted.toFloat() / room.taskCount },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color(0xFF4E6AF6),
                            trackColor = Color(0xFFE0E0E0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    tasksCompleted++
                                    viewModel.updateTaskProgress(tasksCompleted)
                                    if (tasksCompleted >= room.taskCount) {
                                        viewModel.completeTask()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4E6AF6)
                                ),
                                enabled = tasksCompleted < room.taskCount,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Task Done")
                            }
                            if (tasksCompleted >= room.taskCount) {
                                Button(
                                    onClick = { viewModel.completeTask() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Finish!")
                                }
                            }
                        }
                    }
                }
            }

            // Leaderboard
            item {
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            items(leaderboard) { participant ->
                LeaderboardItem(participant, room.taskCount)
            }
        }
    }
}

@Composable
fun LeaderboardItem(participant: ArenaParticipant, totalTasks: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            when (participant.finishPosition) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> Color(0xFF4E6AF6)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (participant.finishPosition > 0)
                            "${participant.finishPosition}"
                        else
                            participant.userName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column {
                    Text(
                        text = participant.userName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${participant.tasksCompleted}/$totalTasks tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${participant.focusTimeSeconds / 60}m",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E6AF6)
                )
                if (participant.status == ParticipantStatus.COMPLETED) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Arena Result Screen
 */
@Composable
fun ArenaResultScreen(
    result: ArenaResult,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val userResult = result.participants.find { it.userId == currentUserId }
    val isWinner = userResult?.userId == result.winnerId

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Winner Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWinner) Color(0xFFFFD700) else Color(0xFF6B46C1)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (isWinner) Icons.Default.Star else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isWinner) "YOU WON!" else "Arena Complete!",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (!isWinner) {
                            Text(
                                text = "${result.winnerName} won!",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "+${userResult?.xpEarned ?: 0} XP",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // User Stats
            userResult?.let { stats ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Your Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "#${stats.position}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4E6AF6)
                                    )
                                    Text(
                                        "Position",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${stats.tasksCompleted}/${stats.totalTasks}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        "Tasks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${stats.focusTimeMinutes}m",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFC107)
                                    )
                                    Text(
                                        "Focus",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Final Standings
            item {
                Text(
                    text = "Final Standings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            items(result.participants.sortedBy { it.position }) { participant ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        when (participant.position) {
                                            1 -> Color(0xFFFFD700)
                                            2 -> Color(0xFFC0C0C0)
                                            3 -> Color(0xFFCD7F32)
                                            else -> Color.Gray
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${participant.position}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = participant.userName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${participant.tasksCompleted} tasks • ${participant.focusTimeMinutes}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        Text(
                            text = "+${participant.xpEarned} XP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4E6AF6)
                        )
                    }
                }
            }

            // Buttons
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B46C1)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Play Again",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Exit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
