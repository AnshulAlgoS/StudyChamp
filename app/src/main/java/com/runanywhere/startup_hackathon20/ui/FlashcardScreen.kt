package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runanywhere.startup_hackathon20.Flashcard
import com.runanywhere.startup_hackathon20.FlashcardSet
import com.runanywhere.startup_hackathon20.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    flashcardSet: FlashcardSet,
    onComplete: (masteredCount: Int) -> Unit,
    onBack: () -> Unit
) {
    var currentCardIndex by remember { mutableStateOf(0) }
    var masteredCards by remember { mutableStateOf(mutableSetOf<Int>()) }
    var showConfetti by remember { mutableStateOf(false) }

    val currentCard = flashcardSet.cards.getOrNull(currentCardIndex)
    val progress = (currentCardIndex + 1).toFloat() / flashcardSet.cards.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flashcards: ${flashcardSet.topic}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal40,
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
            if (currentCard != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Card ${currentCardIndex + 1}/${flashcardSet.cards.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Teal40
                        )
                        Text(
                            text = "Mastered: ${masteredCards.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Teal40,
                        trackColor = Teal80,
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Flashcard with flip
                    FlippableCard(
                        term = currentCard.term,
                        definition = currentCard.definition,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Instructions
                    Text(
                        text = "Tap card to flip • Use buttons to navigate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Still Learning
                        OutlinedButton(
                            onClick = {
                                if (currentCardIndex < flashcardSet.cards.size - 1) {
                                    currentCardIndex++
                                } else {
                                    onComplete(masteredCards.size)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowRight, null)
                                Text("Still Learning")
                            }
                        }

                        // Mark as Mastered
                        Button(
                            onClick = {
                                masteredCards.add(currentCardIndex)
                                if (currentCardIndex < flashcardSet.cards.size - 1) {
                                    currentCardIndex++
                                } else {
                                    showConfetti = true
                                    onComplete(masteredCards.size)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null)
                                Text("Mastered!")
                            }
                        }
                    }
                }
            } else {
                // All done
                CompletionCard(
                    masteredCount = masteredCards.size,
                    totalCards = flashcardSet.cards.size,
                    onDone = onBack
                )
            }

            // Confetti animation
            if (showConfetti) {
                ConfettiAnimation()
            }
        }
    }
}

@Composable
fun FlippableCard(
    term: String,
    definition: String,
    modifier: Modifier = Modifier
) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "flip"
    )

    Card(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isFlipped = !isFlipped }
                )
            }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        colors = CardDefaults.cardColors(
            containerColor = if (rotation > 90f) Purple80 else Teal80
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front (Term)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📚",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = term,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Teal40
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap to see definition",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Back (Definition) - Mirrored text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                ) {
                    Text(
                        text = "✨",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = definition,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = Purple40
                    )
                }
            }
        }
    }
}

@Composable
fun CompletionCard(
    masteredCount: Int,
    totalCards: Int,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Flashcards Complete!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = SuccessGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You mastered $masteredCount out of $totalCards cards!",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SuccessGreen
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ConfettiAnimation() {
    val particles = remember {
        List(30) {
            ConfettiParticle(
                color = listOf(Purple40, Teal40, Yellow40, SuccessGreen).random(),
                startX = (-50..50).random().dp,
                delay = (0..500).random()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            ConfettiParticleView(particle)
        }
    }
}

data class ConfettiParticle(
    val color: Color,
    val startX: androidx.compose.ui.unit.Dp,
    val delay: Int
)

@Composable
fun ConfettiParticleView(particle: ConfettiParticle) {
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(particle.delay.toLong())
        offsetY.animateTo(
            targetValue = 1200f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .offset(x = particle.startX, y = offsetY.value.dp)
            .size(8.dp)
            .background(particle.color, shape = RoundedCornerShape(2.dp))
    )
}
