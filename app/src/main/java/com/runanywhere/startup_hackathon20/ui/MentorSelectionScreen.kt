package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runanywhere.startup_hackathon20.Mentors
import com.runanywhere.startup_hackathon20.MentorProfile
import com.runanywhere.startup_hackathon20.ui.theme.*

@Composable
fun MentorSelectionScreen(
    onMentorSelected: (String) -> Unit
) {
    var selectedMentor by remember { mutableStateOf<String?>(null) }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Purple80,
                        Teal80,
                        Yellow80.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "Choose Your Mentor",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = DeepPurple,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your guide through every learning adventure",
                style = MaterialTheme.typography.bodyLarge,
                color = DeepTeal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Mentor cards
            Mentors.getAll().forEachIndexed { index, mentor ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = index * 150
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = index * 150
                        )
                    ) { it / 2 }
                ) {
                    MentorCard(
                        mentor = mentor,
                        isSelected = selectedMentor == mentor.id,
                        onSelect = { selectedMentor = mentor.id }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = {
                    selectedMentor?.let { onMentorSelected(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = selectedMentor != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple40,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text(
                    text = "Start My Journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MentorCard(
    mentor: MentorProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(android.graphics.Color.parseColor(mentor.color)).copy(alpha = 0.2f)
            else
                CardLight.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 12.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji with animation
            Text(
                text = mentor.emoji,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(end = 16.dp)
            )

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = mentor.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(android.graphics.Color.parseColor(mentor.color))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mentor.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Check mark
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(android.graphics.Color.parseColor(mentor.color)),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
