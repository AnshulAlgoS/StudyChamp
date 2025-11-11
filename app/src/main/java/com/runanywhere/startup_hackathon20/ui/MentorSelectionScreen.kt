package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.Mentors
import com.runanywhere.startup_hackathon20.MentorProfile
import com.runanywhere.startup_hackathon20.R
import com.runanywhere.startup_hackathon20.ui.theme.*

@Composable
fun MentorSelectionScreen(
    onMentorSelected: (String) -> Unit
) {
    var selectedMentor by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }

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
            // Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFFFC107)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose Your AI Mentor",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your personal AI guide who adapts to your learning style",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Mentor Cards
            item {
                MentorCardWithAvatar(
                    mentorId = "sensei",
                    name = "Sensei",
                    tagline = "The Wise Guide",
                    description = "Patient • Thoughtful • Uses Analogies",
                    personality = "Teaches through questions and guided discovery. Perfect for deep understanding.",
                    imageRes = R.drawable.sensei,
                    accentColor = Color(0xFF6366F1),
                    isSelected = selectedMentor == "sensei",
                    onSelect = {
                        selectedMentor = "sensei"
                        showDetails = true
                    }
                )
            }

            item {
                MentorCardWithAvatar(
                    mentorId = "coach_max",
                    name = "Coach Max",
                    tagline = "The Motivator",
                    description = "Energetic • Enthusiastic • Challenge-Focused",
                    personality = "Frames learning as exciting challenges. Great for staying motivated!",
                    imageRes = R.drawable.coachmax,
                    accentColor = Color(0xFFEF4444),
                    isSelected = selectedMentor == "coach_max",
                    onSelect = {
                        selectedMentor = "coach_max"
                        showDetails = true
                    }
                )
            }

            item {
                MentorCardWithAvatar(
                    mentorId = "mira",
                    name = "Mira",
                    tagline = "The Storyteller",
                    description = "Creative • Friendly • Story-Driven",
                    personality = "Makes learning magical with stories and scenarios. Perfect for creative minds!",
                    imageRes = R.drawable.mira,
                    accentColor = Color(0xFF8B5CF6),
                    isSelected = selectedMentor == "mira",
                    onSelect = {
                        selectedMentor = "mira"
                        showDetails = true
                    }
                )
            }

            // Continue Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        selectedMentor?.let { mentorId ->
                            onMentorSelected(mentorId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedMentor != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4E6AF6),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (selectedMentor != null) "Start Learning Journey" else "Select a Mentor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun MentorCardWithAvatar(
    mentorId: String,
    name: String,
    tagline: String,
    description: String,
    personality: String,
    imageRes: Int,
    accentColor: Color,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.98f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 2.dp,
        animationSpec = tween(durationMillis = 300),
        label = "elevation"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = borderWidth,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                accentColor.copy(alpha = 0.15f)
            } else {
                Color.White
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Image with animated border
                val avatarBorderWidth by animateDpAsState(
                    targetValue = if (isSelected) 4.dp else 2.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "avatarBorder"
                )

                val avatarScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "avatarScale"
                )

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(avatarScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow effect when selected
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(95.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(85.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.2f),
                                            accentColor.copy(alpha = 0.1f)
                                        )
                                    )
                                } else {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFF5F5F7),
                                            Color(0xFFE0E0E0)
                                        )
                                    )
                                }
                            )
                            .border(
                                width = avatarBorderWidth,
                                brush = if (isSelected) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            accentColor,
                                            accentColor.copy(alpha = 0.7f),
                                            accentColor
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFE0E0E0),
                                            Color(0xFFE0E0E0)
                                        )
                                    )
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = "$name avatar",
                            modifier = Modifier
                                .size(75.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name and Tagline
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) accentColor else Color.Black,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) accentColor.copy(alpha = 0.8f) else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                // Selection Indicator with pulse animation
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(pulseScale),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )

                        // Main circle
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            accentColor,
                                            accentColor.copy(alpha = 0.9f)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description with animated color
            val descriptionColor by animateColorAsState(
                targetValue = if (isSelected) Color.Black else Color(0xFF424242),
                animationSpec = tween(durationMillis = 300),
                label = "descColor"
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = descriptionColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Personality card with gradient when selected
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        Color.Transparent
                    } else {
                        accentColor.copy(alpha = 0.08f)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.2f),
                                        accentColor.copy(alpha = 0.1f),
                                        accentColor.copy(alpha = 0.2f)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent
                                    )
                                )
                            }
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = personality,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.Black else Color(0xFF616161),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        lineHeight = 18.sp
                    )
                }
            }

            // Teaching Style Chips (shown when selected) with staggered animation
            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 400)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))

                    // "Teaching Style" label
                    Text(
                        text = "TEACHING STYLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Use Column with evenly spaced rows for better alignment
                    when (mentorId) {
                        "sensei" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyleChip("🧘 Calm", accentColor, isSelected)
                                StyleChip("🤔 Reflective", accentColor, isSelected)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyleChip("🎯 Focused", accentColor, isSelected)
                            }
                        }

                        "coach_max" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyleChip("⚡ High Energy", accentColor, isSelected)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyleChip("🏆 Goal-Driven", accentColor, isSelected)
                                StyleChip("💪 Motivating", accentColor, isSelected)
                            }
                        }

                        "mira" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyleChip("✨ Creative", accentColor, isSelected)
                                StyleChip("📖 Story-Based", accentColor, isSelected)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyleChip("🎨 Imaginative", accentColor, isSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StyleChip(text: String, color: Color, isSelected: Boolean) {
    val chipScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipScale"
    )

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.scale(chipScale),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
