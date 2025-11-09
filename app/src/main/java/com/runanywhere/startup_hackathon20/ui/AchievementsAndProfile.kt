package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

// Achievements Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievements: List<Achievement>,
    onBack: () -> Unit
) {
    val unlockedCount = achievements.count { it.isUnlocked }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        // Floating Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 20.dp, top = 24.dp)
                .size(48.dp)
                .background(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = CircleShape
                )
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF4E6AF6)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Achievements",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "$unlockedCount of ${achievements.size} unlocked",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFC107)
                        )
                    }

                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of achievements
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementCard(achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement) {
    val scale by animateFloatAsState(
        targetValue = if (achievement.isUnlocked) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Get requirement text based on achievement ID
    val requirementText = when (achievement.id) {
        "first_quiz" -> "Complete 1 quiz"
        "problem_solver" -> "Score 100% on any quiz"
        "concept_conqueror" -> "Complete 5 topics"
        "streak_3" -> "Study for 3 days in a row"
        "streak_7" -> "Study for 7 days in a row"
        "flashcard_master" -> "Master 25 flashcards"
        "level_5" -> "Reach Level 5 (1000 XP)"
        "level_10" -> "Reach Level 10 (4000 XP)"
        "quiz_marathon" -> "Complete 10 quizzes"
        "perfect_week" -> "Study every day for a week"
        else -> "Complete the challenge"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                SuccessGreen.copy(alpha = 0.15f)
            else
                CardLight
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.isUnlocked) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Emoji
                Text(
                    text = achievement.emoji,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.alpha(if (achievement.isUnlocked) 1f else 0.3f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (achievement.isUnlocked)
                        SuccessGreen
                    else
                        Purple40,
                    modifier = Modifier.alpha(if (achievement.isUnlocked) 1f else 0.7f),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Description
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(if (achievement.isUnlocked) 1f else 0.6f),
                    maxLines = 2,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.9f
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Requirement text (if not unlocked)
                if (!achievement.isUnlocked) {
                    Text(
                        text = "📍 $requirementText",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = Teal40,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // XP reward
                Surface(
                    color = if (achievement.isUnlocked) Gold.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (achievement.isUnlocked) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "${achievement.xpReward} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (achievement.isUnlocked) Gold else Color.Gray
                        )
                    }
                }
            }

            // Locked overlay
            if (!achievement.isUnlocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                )
            } else {
                // Unlocked checkmark
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = SuccessGreen,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                )
            }
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
