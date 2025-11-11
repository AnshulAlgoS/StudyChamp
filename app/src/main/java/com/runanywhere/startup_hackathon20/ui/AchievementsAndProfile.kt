package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.*
import com.runanywhere.startup_hackathon20.ui.theme.*
import kotlinx.coroutines.launch

// Profile HUD Component - Shows XP, Level, Streak
@Composable
fun ProfileHUD(
    userProgress: UserProgress,
    modifier: Modifier = Modifier
) {
    val xpForNext = XPSystem.xpForNextLevel(userProgress.totalXP, userProgress.level)
    val progressToNext = XPSystem.progressToNextLevel(userProgress.totalXP, userProgress.level)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardLight
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Level and XP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Level badge
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Purple40, DeepPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = userProgress.level.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "LVL",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "${userProgress.totalXP} XP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Purple40
                        )
                        if (userProgress.level < 10) {
                            Text(
                                text = "$xpForNext XP to Level ${userProgress.level + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Max Level! 🏆",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gold
                            )
                        }
                    }
                }

                // Streak
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (userProgress.currentStreak > 0)
                            Yellow80
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔥",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${userProgress.currentStreak}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (userProgress.currentStreak > 0) Gold else Color.Gray
                        )
                        Text(
                            text = "day${if (userProgress.currentStreak != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Progress bar
            if (userProgress.level < 10) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progressToNext },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Purple40,
                    trackColor = Purple80,
                )
            }

            // Stats
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("📚", userProgress.totalTopicsCompleted.toString(), "Topics")
                StatItem("📝", userProgress.totalQuizzesCompleted.toString(), "Quizzes")
                StatItem("🃏", userProgress.totalFlashcardsCompleted.toString(), "Cards")
            }
        }
    }
}

@Composable
fun StatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Teal40
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: com.runanywhere.startup_hackathon20.FirebaseStudyViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7FA)) {
        userProfile?.let { profile ->
            FuturisticAchievementsDashboard(
                userLevel = profile.level,
                userXP = profile.totalXP,
                totalQuizzesTaken = profile.quizzesCompleted,
                quizAccuracy = 85,
                studyStreak = profile.currentStreak,
                topicsMastered = profile.topicsCompleted
            )
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4E6AF6))
            }
        }
    }
}

@Composable
fun FuturisticAchievementsDashboard(
    userLevel: Int,
    userXP: Int,
    totalQuizzesTaken: Int,
    quizAccuracy: Int,
    studyStreak: Int,
    topicsMastered: Int
) {
    val xpForCurrentLevel = (userLevel - 1) * 100
    val xpForNextLevel = userLevel * 100
    val currentLevelProgress = userXP - xpForCurrentLevel
    val xpNeededForNextLevel = xpForNextLevel - userXP
    val progress = currentLevelProgress.toFloat() / (xpForNextLevel - xpForCurrentLevel).toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ProgressAndLevelsPanel(
                level = userLevel,
                currentXP = userXP,
                nextLevelXP = xpForNextLevel,
                xpNeeded = xpNeededForNextLevel,
                progress = progress.coerceIn(0f, 1f)
            )
        }

        item {
            BadgesGrid(
                totalQuizzes = totalQuizzesTaken,
                accuracy = quizAccuracy,
                streak = studyStreak,
                topicsMastered = topicsMastered
            )
        }

        item {
            RecordsSection(
                longestStreak = studyStreak,
                highestAccuracy = quizAccuracy,
                topicsMastered = topicsMastered,
                mentorBondLevel = userLevel / 3 + 1
            )
        }
    }
}

@Composable
fun ProgressAndLevelsPanel(
    level: Int,
    currentXP: Int,
    nextLevelXP: Int,
    xpNeeded: Int,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Level $level",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E6AF6)
                    )
                    Text(
                        "Scholar Tier",
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$currentXP XP",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        "of $nextLevelXP",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF4E6AF6), Color(0xFF7C3AED))
                            )
                        )
                        .animateContentSize()
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEEF2FF)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎯", fontSize = 18.sp)
                    Text(
                        "$xpNeeded XP to Level ${level + 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4E6AF6)
                    )
                }
            }
        }
    }
}

@Composable
fun BadgesGrid(totalQuizzes: Int, accuracy: Int, streak: Int, topicsMastered: Int) {
    val badges = listOf(
        Badge(
            "quiz_master",
            "Quiz Master",
            "Take 10 quizzes",
            totalQuizzes >= 10,
            com.runanywhere.startup_hackathon20.R.drawable.quizmaster
        ),
        Badge(
            "daily_streak_10",
            "10-Day Warrior",
            "10-day study streak",
            streak >= 10,
            com.runanywhere.startup_hackathon20.R.drawable.tenday
        ),
        Badge(
            "flashcard_pro",
            "Flashcard Pro",
            "Master 50 flashcards",
            topicsMastered >= 5,
            com.runanywhere.startup_hackathon20.R.drawable.flashcard
        ),
        Badge(
            "topic_slayer",
            "Topic Slayer",
            "Complete 10 topics",
            topicsMastered >= 10,
            com.runanywhere.startup_hackathon20.R.drawable.topicslayer
        ),
        Badge(
            "sensei_apprentice",
            "Sensei's Apprentice",
            "Study 20 sessions",
            totalQuizzes >= 5,
            com.runanywhere.startup_hackathon20.R.drawable.senseis
        ),
        Badge(
            "accuracy_king",
            "Accuracy King",
            "90%+ quiz accuracy",
            accuracy >= 90,
            com.runanywhere.startup_hackathon20.R.drawable.accuracyking
        ),
        Badge(
            "xp_collector",
            "XP Collector",
            "Earn 500 XP",
            true,
            com.runanywhere.startup_hackathon20.R.drawable.xpcollector
        ),
        Badge(
            "night_owl",
            "Night Owl",
            "Study after midnight",
            false,
            com.runanywhere.startup_hackathon20.R.drawable.nightowl
        ),
        Badge(
            "early_bird",
            "Early Bird",
            "Study before 7 AM",
            false,
            com.runanywhere.startup_hackathon20.R.drawable.earlybird
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "🏅 Achievements",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(520.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(badges) { badge ->
                BadgeCard(badge)
            }
        }
    }
}

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val unlocked: Boolean,
    val imageRes: Int
)

@Composable
fun BadgeCard(badge: Badge) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.unlocked) Color.White else Color(0xFFF9FAFB)
        ),
        elevation = CardDefaults.cardElevation(if (badge.unlocked) 6.dp else 2.dp),
        border = if (badge.unlocked) BorderStroke(
            2.5.dp, Brush.horizontalGradient(
                colors = listOf(Color(0xFF4E6AF6), Color(0xFF7C3AED))
            )
        ) else BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (badge.unlocked) 1f else 0.6f)
            ) {
                Image(
                    painter = painterResource(id = badge.imageRes),
                    contentDescription = badge.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (!badge.unlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF6B7280).copy(alpha = 0.9f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (badge.unlocked) Color(0xFFEEF2FF) else Color(0xFFF3F4F6),
                                CircleShape
                            )
                            .padding(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = badge.imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column {
                        Text(
                            badge.name,
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            if (badge.unlocked) "Unlocked" else "Locked",
                            color = if (badge.unlocked) Color(0xFF10B981) else Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        badge.description,
                        color = Color(0xFF6B7280),
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                    if (badge.unlocked) {
                        Surface(
                            color = Color(0xFFEEF2FF),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Achievement Unlocked! +50 XP",
                                    color = Color(0xFF4E6AF6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA726),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Keep learning to unlock this!",
                                    color = Color(0xFFEF6C00),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF4E6AF6)
                    )
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun RecordsSection(longestStreak: Int, highestAccuracy: Int, topicsMastered: Int, mentorBondLevel: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "🕰️ Your Records",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RecordCard("🔥", "Longest Study Streak", "$longestStreak days", Color(0xFFEF4444))
            RecordCard("🎯", "Highest Quiz Accuracy", "$highestAccuracy%", Color(0xFF10B981))
            RecordCard("📚", "Total Topics Mastered", "$topicsMastered topics", Color(0xFF8B5CF6))
            RecordCard("🤝", "AI Mentor Bond Level", "Level $mentorBondLevel", Color(0xFF4E6AF6))
        }
    }
}

@Composable
fun RecordCard(icon: String, title: String, value: String, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 24.sp)
                }
                Text(
                    title,
                    fontSize = 15.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

// XP Pop-up Animation (shows when gaining XP)
@Composable
fun XPPopupAnimation(
    xpAmount: Int,
    modifier: Modifier = Modifier
) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            offsetY.animateTo(
                targetValue = -100f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            kotlinx.coroutines.delay(800)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700)
            )
        }
    }

    Box(
        modifier = modifier.offset(y = offsetY.value.dp)
    ) {
        Surface(
            color = Gold.copy(alpha = alpha.value * 0.9f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "✨", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+$xpAmount XP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
