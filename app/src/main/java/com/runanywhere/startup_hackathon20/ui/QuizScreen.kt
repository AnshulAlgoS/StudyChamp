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

        // XP Badge - floating top right
        Surface(
            color = Color(0xFFFFF3E0),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$totalXPEarned XP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFC107)
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (showCompletionScreen) {
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Question ${currentQuestionIndex + 1}/${quizData.questions.size}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "$correctAnswersCount correct",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (currentQuestionIndex + 1).toFloat() / quizData.questions.size },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFF4E6AF6),
                                trackColor = Color(0xFFE0E0E0),
                            )
                        }
                    }

                    // Question Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF4E6AF6),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentQuestion.question,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    // Options
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        currentQuestion.options.forEach { option ->
                            CleanOptionButton(
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

                    // Hint Card
                    AnimatedVisibility(
                        visible = showHint,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = currentQuestion.hint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF424242)
                                )
                            }
                        }
                    }

                    // Action Button
                    Button(
                        onClick = {
                            if (isAnswerSubmitted) {
                                if (currentQuestionIndex < quizData.questions.size - 1) {
                                    currentQuestionIndex++
                                    selectedAnswer = null
                                    isAnswerSubmitted = false
                                    showHint = false
                                    showXPAnimation = false
                                } else {
                                    showCompletionScreen = true
                                }
                            } else {
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
                            .height(56.dp),
                        enabled = selectedAnswer != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAnswerSubmitted) Color(0xFF4CAF50) else Color(
                                0xFF4E6AF6
                            ),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = when {
                                isAnswerSubmitted && currentQuestionIndex < quizData.questions.size - 1 -> "Next Question"
                                isAnswerSubmitted -> "See Results"
                                else -> "Submit Answer"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // XP Animation
            AnimatedVisibility(
                visible = showXPAnimation,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                XPRewardAnimation(xpAmount = 25)
            }
        }
    }
}

@Composable
fun CleanOptionButton(
    option: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isAnswerSubmitted: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isAnswerSubmitted && isCorrect -> Color(0xFFE8F5E9)
        isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFFFEBEE)
        isSelected -> Color(0xFFE3F2FD)
        else -> Color.White
    }

    val borderColor = when {
        isAnswerSubmitted && isCorrect -> Color(0xFF4CAF50)
        isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFF44336)
        isSelected -> Color(0xFF4E6AF6)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (borderColor != Color.Transparent) 2.dp else 0.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isAnswerSubmitted) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else if (isSelected) Icons.Default.Clear else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isCorrect) Color(0xFF4CAF50) else if (isSelected) Color(0xFFF44336) else Color.Transparent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
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
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Results card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = performanceMessage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Score
                Text(
                    text = "$correctAnswers / $totalQuestions",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Correct Answers",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF424242)
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
                            color = Color(0xFFFFC107)
                        )
                        Text(
                            text = "Saving to your profile...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242)
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
                containerColor = Color(0xFF4E6AF6)
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
                        Color(0xFFFFF3E0).copy(alpha = alpha.value),
                        Color(0xFFFFC107).copy(alpha = alpha.value * 0.5f),
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
                color = Color(0xFFFFC107)
            )
        }
    }
}
