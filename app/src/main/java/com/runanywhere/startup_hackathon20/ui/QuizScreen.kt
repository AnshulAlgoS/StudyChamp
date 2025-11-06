package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runanywhere.startup_hackathon20.QuizData
import com.runanywhere.startup_hackathon20.QuizQuestion
import com.runanywhere.startup_hackathon20.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizData: QuizData,
    onQuizComplete: (correctAnswers: Int, totalQuestions: Int) -> Unit,
    onBack: () -> Unit
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var correctAnswersCount by remember { mutableStateOf(0) }
    var showHint by remember { mutableStateOf(false) }
    var showXPAnimation by remember { mutableStateOf(false) }
    var showCompletionScreen by remember { mutableStateOf(false) }

    val currentQuestion = quizData.questions.getOrNull(currentQuestionIndex)
    val totalXPEarned = correctAnswersCount * 25 // 25 XP per correct answer

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz: ${quizData.topic}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Show current XP earned
                    Surface(
                        color = Gold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⭐", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$totalXPEarned XP",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Gold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Purple40,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = LightBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showCompletionScreen) {
                // Quiz completion summary
                QuizCompletionScreen(
                    correctAnswers = correctAnswersCount,
                    totalQuestions = quizData.questions.size,
                    xpEarned = totalXPEarned,
                    onDone = { onQuizComplete(correctAnswersCount, quizData.questions.size) }
                )
            } else if (currentQuestion != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Progress indicator
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${currentQuestionIndex + 1}/${quizData.questions.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Purple40
                            )
                            Text(
                                text = "✓ $correctAnswersCount correct",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (currentQuestionIndex + 1).toFloat() / quizData.questions.size },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Purple40,
                            trackColor = Purple80,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Question card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CardLight
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = currentQuestion.question,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Purple40,
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Options
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        currentQuestion.options.forEach { option ->
                            OptionButton(
                                option = option,
                                isSelected = selectedAnswer == option,
                                isCorrect = option == currentQuestion.answer,
                                isAnswerSubmitted = isAnswerSubmitted,
                                onClick = {
                                    if (!isAnswerSubmitted) {
                                        selectedAnswer = option
                                    }
                                }
                            )
                        }
                    }

                    // Hint card (shown after wrong answer)
                    AnimatedVisibility(
                        visible = showHint,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Yellow80
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Gold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = currentQuestion.hint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gold
                                )
                            }
                        }
                    }

                    // Action button
                    Button(
                        onClick = {
                            if (isAnswerSubmitted) {
                                // Move to next question
                                if (currentQuestionIndex < quizData.questions.size - 1) {
                                    currentQuestionIndex++
                                    selectedAnswer = null
                                    isAnswerSubmitted = false
                                    showHint = false
                                    showXPAnimation = false
                                } else {
                                    // Quiz complete - show summary
                                    showCompletionScreen = true
                                }
                            } else {
                                // Submit answer
                                if (selectedAnswer != null) {
                                    isAnswerSubmitted = true
                                    val isCorrect = selectedAnswer == currentQuestion.answer
                                    if (isCorrect) {
                                        correctAnswersCount++
                                        showXPAnimation = true
                                    } else {
                                        showHint = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        enabled = selectedAnswer != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAnswerSubmitted) Teal40 else Purple40
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = when {
                                isAnswerSubmitted && currentQuestionIndex < quizData.questions.size - 1 -> "Next Question →"
                                isAnswerSubmitted -> "See Results 🎉"
                                else -> "Submit Answer"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // XP animation overlay
            AnimatedVisibility(
                visible = showXPAnimation,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                XPRewardAnimation(xpAmount = 25)
            }
        }
    }
}

@Composable
fun QuizCompletionScreen(
    correctAnswers: Int,
    totalQuestions: Int,
    xpEarned: Int,
    onDone: () -> Unit
) {
    val percentage = (correctAnswers.toFloat() / totalQuestions * 100).toInt()
    val performanceMessage = when {
        percentage == 100 -> "Perfect Score! 🏆"
        percentage >= 80 -> "Excellent Work! ⭐"
        percentage >= 60 -> "Good Job! 👍"
        else -> "Keep Learning! 💪"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Celebration icon
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 2
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Quiz Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Purple40
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Results card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SuccessGreen.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = performanceMessage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Score
                Text(
                    text = "$correctAnswers / $totalQuestions",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Purple40
                )
                Text(
                    text = "Correct Answers",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Divider()

                Spacer(modifier = Modifier.height(24.dp))

                // XP earned
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⭐", style = MaterialTheme.typography.displaySmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "+$xpEarned XP Earned!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                        Text(
                            text = "Saving to your profile...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Done button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple40
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Continue Learning",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OptionButton(
    option: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isAnswerSubmitted: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isAnswerSubmitted && isCorrect -> SuccessGreen.copy(alpha = 0.3f)
        isAnswerSubmitted && isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.3f)
        isSelected -> Purple80
        else -> CardLight
    }

    val borderColor = when {
        isAnswerSubmitted && isCorrect -> SuccessGreen
        isAnswerSubmitted && isSelected && !isCorrect -> ErrorRed
        isSelected -> Purple40
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (borderColor != Color.Transparent) 3.dp else 0.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (isAnswerSubmitted) {
                if (isCorrect || isSelected) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Clear,
                        contentDescription = null,
                        tint = if (isCorrect) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun XPRewardAnimation(xpAmount: Int = 25) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1000, delayMillis = 500)
        )
    }

    Box(
        modifier = Modifier
            .size(150.dp)
            .scale(scale.value)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Yellow60.copy(alpha = alpha.value),
                        Gold.copy(alpha = alpha.value * 0.5f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✨",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.scale(1.5f)
            )
            Text(
                text = "+$xpAmount XP",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Gold
            )
        }
    }
}
