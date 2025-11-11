package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.Flashcard
import com.runanywhere.startup_hackathon20.FlashcardSet
import com.runanywhere.startup_hackathon20.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    flashcardSet: FlashcardSet,
    onComplete: (masteredCount: Int) -> Unit,
    onBack: () -> Unit
) {
    var currentCardIndex by remember { mutableStateOf(0) }
    var masteredCards by remember { mutableStateOf(mutableSetOf<Int>()) }
    var stillLearningCards by remember { mutableStateOf(mutableSetOf<Int>()) }
    var showConfetti by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }
    var flipCount by remember { mutableStateOf(0) }

    val currentCard = flashcardSet.cards.getOrNull(currentCardIndex)
    val progress = if (flashcardSet.cards.isNotEmpty()) {
        (currentCardIndex + 1).toFloat() / flashcardSet.cards.size
    } else 0f
    
    val masteryPercentage = if (flashcardSet.cards.isNotEmpty()) {
        (masteredCards.size.toFloat() / flashcardSet.cards.size * 100).toInt()
    } else 0

    // Gradient colors based on mentor (can be passed as parameter later)
    val gradientColors = listOf(
        Color(0xFF6366F1),
        Color(0xFF8B5CF6),
        Color(0xFFA855F7)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FE),
                        Color(0xFFEEF2FF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Top Header with Back Button and Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF6366F1)
                    )
                }

                // Stats Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatsChip(
                        icon = Icons.Default.CheckCircle,
                        label = "${masteredCards.size}",
                        color = Color(0xFF10B981),
                        description = "Mastered"
                    )
                    StatsChip(
                        icon = Icons.Default.Star,
                        label = "${flipCount}",
                        color = Color(0xFFF59E0B),
                        description = "Flips"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentCard != null) {
                // Title and Progress Section
                Text(
                    text = flashcardSet.topic,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Flashcard Review",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Enhanced Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Card ${currentCardIndex + 1} of ${flashcardSet.cards.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "${flashcardSet.cards.size - currentCardIndex} remaining",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            
                            // Circular Progress Indicator
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(60.dp),
                                    color = Color(0xFF6366F1),
                                    strokeWidth = 6.dp,
                                    trackColor = Color(0xFFE5E7EB)
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Mastery Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mastery: $masteryPercentage%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { masteryPercentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF10B981),
                                    trackColor = Color(0xFFE5E7EB)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Enhanced Flippable Card
                EnhancedFlippableCard(
                    term = currentCard.term,
                    definition = currentCard.definition,
                    cardNumber = currentCardIndex + 1,
                    totalCards = flashcardSet.cards.size,
                    isFlipped = isFlipped,
                    onFlip = { 
                        isFlipped = !isFlipped
                        flipCount++
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons with Enhanced Design
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Still Learning Button
                    Button(
                        onClick = {
                            stillLearningCards.add(currentCardIndex)
                            isFlipped = false
                            if (currentCardIndex < flashcardSet.cards.size - 1) {
                                currentCardIndex++
                            } else {
                                onComplete(masteredCards.size)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Still Learning",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    // Mastered Button
                    Button(
                        onClick = {
                            masteredCards.add(currentCardIndex)
                            isFlipped = false
                            if (currentCardIndex < flashcardSet.cards.size - 1) {
                                currentCardIndex++
                            } else {
                                showConfetti = true
                                onComplete(masteredCards.size)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Mastered!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                EnhancedCompletionCard(
                    masteredCount = masteredCards.size,
                    stillLearningCount = stillLearningCards.size,
                    totalCards = flashcardSet.cards.size,
                    flips = flipCount,
                    onDone = onBack
                )
            }
        }

        if (showConfetti) {
            ConfettiAnimation()
        }
    }
}

@Composable
fun StatsChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
fun EnhancedFlippableCard(
    term: String,
    definition: String,
    cardNumber: Int,
    totalCards: Int,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "flip"
    )

    val scale by animateFloatAsState(
        targetValue = if (isFlipped) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onFlip() })
            }
            .graphicsLayer {
                rotationY = rotation
                scaleX = scale
                scaleY = scale
                cameraDistance = 12f * density
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (rotation <= 90f) {
                            listOf(Color.White, Color(0xFFFAFAFF))
                        } else {
                            listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))
                        }
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front (Term)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Decorative badge
                    Surface(
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "TERM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1),
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = term,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF1E293B),
                        lineHeight = 40.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Animated tap indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Tap to reveal definition",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Back (Definition) - Mirrored text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                ) {
                    // Decorative badge
                    Surface(
                        color = Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "DEFINITION",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = definition,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF1E293B),
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Success indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Did you get it right?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedCompletionCard(
    masteredCount: Int,
    stillLearningCount: Int,
    totalCards: Int,
    flips: Int,
    onDone: () -> Unit
) {
    val masteryPercentage = if (totalCards > 0) {
        (masteredCount.toFloat() / totalCards * 100).toInt()
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Success Icon with Animation
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color(0xFFD1FAE5),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(64.dp)
            )
        }

        Text(
            text = " Great Session!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )

        // Stats Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mastery Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mastery Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "$masteryPercentage%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                
                LinearProgressIndicator(
                    progress = { masteryPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFFE5E7EB)
                )
                
                Divider(color = Color(0xFFE5E7EB))
                
                // Individual Stats
                StatRow(
                    icon = Icons.Default.CheckCircle,
                    label = "Mastered",
                    value = "$masteredCount cards",
                    color = Color(0xFF10B981)
                )
                
                StatRow(
                    icon = Icons.Default.Refresh,
                    label = "Still Learning",
                    value = "$stillLearningCount cards",
                    color = Color(0xFFF59E0B)
                )
                
                StatRow(
                    icon = Icons.Default.Refresh,
                    label = "Total Flips",
                    value = "$flips times",
                    color = Color(0xFF6366F1)
                )
                
                StatRow(
                    icon = Icons.Default.Star,
                    label = "Cards Reviewed",
                    value = "$totalCards cards",
                    color = Color(0xFF8B5CF6)
                )
            }
        }

        // Motivational Message
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFEEF2FF)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = when {
                        masteryPercentage == 100 -> "Perfect! You've mastered all cards! "
                        masteryPercentage >= 80 -> "Excellent work! Keep it up! "
                        masteryPercentage >= 60 -> "Great progress! You're getting there! "
                        else -> "Good start! Practice makes perfect! "
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Done Button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Continue Learning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun StatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                        color = color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
    }
}

@Composable
fun ConfettiAnimation() {
    val particles = remember {
        List(60) {
        ConfettiParticle(
                color = listOf(
                    Color(0xFF6366F1), 
                    Color(0xFF10B981), 
                    Color(0xFFF59E0B),
                    Color(0xFFA855F7),
                    Color(0xFFEC4899),
                    Color(0xFF3B82F6),
                    Color(0xFFFBBF24),
                    Color(0xFF8B5CF6)
                ).random(),
                startX = (-100..100).random().dp,
                startY = (-50..0).random().dp,
                delay = (0..800).random(),
                size = (6..12).random().dp,
                rotation = (0..360).random().toFloat()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { }
    ) {
        particles.forEach { particle ->
            ConfettiParticleView(particle)
        }
    }
}

data class ConfettiParticle(
    val color: Color,
    val startX: androidx.compose.ui.unit.Dp,
    val startY: androidx.compose.ui.unit.Dp,
    val delay: Int,
    val size: androidx.compose.ui.unit.Dp,
    val rotation: Float
)

@Composable
fun ConfettiParticleView(particle: ConfettiParticle) {
    val offsetY = remember { Animatable(particle.startY.value) }
    val offsetX = remember { Animatable(particle.startX.value) }
    val rotation = remember { Animatable(particle.rotation) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(particle.delay.toLong())

        // Launch multiple animations in parallel within the LaunchedEffect scope
        launch {
            offsetY.animateTo(
                targetValue = 1400f,
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
            )
        }

        launch {
            // Add horizontal drift
            offsetX.animateTo(
                targetValue = particle.startX.value + ((-50..50).random()),
                animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
            )
        }

        launch {
            // Add rotation
            rotation.animateTo(
                targetValue = particle.rotation + 720f, // Two full rotations
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
            )
        }

        launch {
            // Fade out near the end
            kotlinx.coroutines.delay(2000)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
    }

    Box(
        modifier = Modifier
            .offset(x = offsetX.value.dp, y = offsetY.value.dp)
            .size(particle.size)
            .graphicsLayer {
                rotationZ = rotation.value
                this.alpha = alpha.value
            }
            .background(particle.color, shape = RoundedCornerShape(2.dp))
    )
}
