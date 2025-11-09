package com.runanywhere.startup_hackathon20

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.ui.*
import com.runanywhere.startup_hackathon20.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        
        // COMPLETE FIX FOR WHITE HEADER - Make everything purple from top to bottom
        window.apply {
            // Set status bar and navigation bar to light colors
            statusBarColor = android.graphics.Color.parseColor("#F5F5F7")
            navigationBarColor = android.graphics.Color.WHITE
            
            // Draw behind system bars
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
            }
        }

        // Make status bar icons dark (visible on light background)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = true  // Dark icons = true for light background
        }

        setContent {
            val viewModel: FirebaseStudyViewModel =
                ViewModelProvider(this, FirebaseStudyViewModelFactory(application)).get(
                    FirebaseStudyViewModel::class.java
                )
            Startup_hackathon20Theme {
                StudyChampApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StudyChampApp(viewModel: FirebaseStudyViewModel = viewModel()) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()
    val currentQuiz by viewModel.currentQuiz.collectAsState()
    val currentFlashcards by viewModel.currentFlashcards.collectAsState()
    val currentMentor by viewModel.currentMentor.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var showProfileSetup by remember { mutableStateOf(false) }

    // Check if user needs profile setup
    LaunchedEffect(isSignedIn, userProfile) {
        if (isSignedIn && userProfile == null) {
            showProfileSetup = true
        } else if (userProfile != null && showProfileSetup) {
            showProfileSetup = false
            // Show mentor selection if no mentor selected
            if (userProfile?.selectedMentor.isNullOrEmpty()) {
                currentScreen = AppScreen.MENTOR_SELECT
            }
        }
    }

    // Determine if we should show bottom nav
    val showBottomNav = when {
        showProfileSetup -> false
        currentQuiz != null -> false
        currentFlashcards != null -> false
        currentScreen == AppScreen.MENTOR_SELECT -> false
        else -> true
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                // Profile Setup for new users
                showProfileSetup -> {
                    ProfileSetupScreen(
                        onProfileCreated = { name, email ->
                            viewModel.createOrUpdateProfile(name, email)
                            currentScreen = AppScreen.MENTOR_SELECT
                        }
                    )
                }
                // Quiz active
                currentQuiz != null -> {
                    QuizScreen(
                        quizData = currentQuiz!!,
                        onQuizComplete = { correct, total ->
                            viewModel.completeQuiz(correct, total)
                            currentScreen = AppScreen.STUDY
                        },
                        onBack = {
                            viewModel.resetJourney()
                            currentScreen = AppScreen.HOME
                        }
                    )
                }
                // Flashcards active
                currentFlashcards != null -> {
                    FlashcardScreen(
                        flashcardSet = currentFlashcards!!,
                        onComplete = { mastered ->
                            viewModel.completeFlashcards(mastered, currentFlashcards!!.cards.size)
                            currentScreen = AppScreen.STUDY
                        },
                        onBack = {
                            viewModel.resetJourney()
                            currentScreen = AppScreen.HOME
                        }
                    )
                }
                // Regular navigation
                else -> when (currentScreen) {
                    AppScreen.MENTOR_SELECT -> MentorSelectionScreen(
                        onMentorSelected = { mentorId ->
                            viewModel.selectMentor(mentorId)
                            currentScreen = AppScreen.HOME
                        }
                    )

                    AppScreen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        userProfile = userProfile,
                        onNavigateToStudy = { currentScreen = AppScreen.STUDY },
                        onNavigateToModels = { currentScreen = AppScreen.MODELS },
                        onNavigateToAchievements = { currentScreen = AppScreen.ACHIEVEMENTS },
                        onNavigateToProfile = { currentScreen = AppScreen.PROFILE }
                    )

                    AppScreen.STUDY -> StudyJourneyScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            viewModel.resetJourney()
                            currentScreen = AppScreen.HOME
                        }
                    )

                    AppScreen.MODELS -> ModelManagementScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.HOME }
                    )

                    AppScreen.ACHIEVEMENTS -> {
                        val achievements by viewModel.achievements.collectAsState()
                        AchievementsScreen(
                            achievements = achievements,
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                    }

                    AppScreen.PROFILE -> {
                        userProfile?.let {
                            ProfileScreen(
                                userProfile = it,
                                onBack = { currentScreen = AppScreen.HOME },
                                onChangeMentor = { currentScreen = AppScreen.MENTOR_SELECT },
                                onEditProfile = { name, email ->
                                    viewModel.createOrUpdateProfile(name, email)
                                }
                            )
                        } ?: run {
                            // Show loading/error screen if profile not loaded
                            ProfileLoadingScreen(
                                onBack = { currentScreen = AppScreen.HOME },
                                onRetry = { viewModel.signInAnonymously() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
        color = Color.White
    ) {
        NavigationBar(
            containerColor = Color.White,
            contentColor = Color(0xFF4E6AF6),
            tonalElevation = 0.dp,
            modifier = Modifier.height(80.dp)
        ) {
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "Home",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                selected = currentScreen == AppScreen.HOME,
                onClick = { onNavigate(AppScreen.HOME) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFF4E6AF6),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color(0xFF4E6AF6)
                )
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Progress",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "Progress",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                selected = currentScreen == AppScreen.ACHIEVEMENTS,
                onClick = { onNavigate(AppScreen.ACHIEVEMENTS) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFF4E6AF6),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color(0xFF4E6AF6)
                )
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Tasks",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "Tasks",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                selected = currentScreen == AppScreen.MODELS,
                onClick = { onNavigate(AppScreen.MODELS) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFF4E6AF6),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color(0xFF4E6AF6)
                )
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                selected = currentScreen == AppScreen.PROFILE,
                onClick = { onNavigate(AppScreen.PROFILE) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFF4E6AF6),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color(0xFF4E6AF6)
                )
            )
        }
    }
}
// ... existing code ...

enum class AppScreen {
    MENTOR_SELECT, HOME, STUDY, MODELS, ACHIEVEMENTS, PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FirebaseStudyViewModel,
    userProfile: UserProfile?,
    onNavigateToStudy: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()
    val currentMentor by viewModel.currentMentor.collectAsState()

    var subject by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            // Featured Card with Mascot (first item - no welcome header)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4E6AF6)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(0.35f)
                                    .padding(end = 4.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Start Your",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Learning",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Journey",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { /* Scroll to input */ },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFC107)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 24.dp,
                                        vertical = 8.dp
                                    )
                                ) {
                                    Text(
                                        "Let's Go",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Image(
                                painter = painterResource(R.drawable.study_mascot),
                                contentDescription = "Mascot",
                                modifier = Modifier
                                    .weight(0.65f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }

            // Search Bar with Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Search topics...", color = Color.Gray)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        // Start with search query as topic (subject can be generic)
                                        subject = ""
                                        topics = searchQuery
                                        viewModel.startStudyJourney(subject, topics)
                                        onNavigateToStudy()
                                        searchQuery = ""
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Go",
                                        tint = Color(0xFF4E6AF6)
                                    )
                                }
                            }
                        }
                    )
                    IconButton(
                        onClick = { /* Filter */ },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF4E6AF6), RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Filter",
                            tint = Color.White
                        )
                    }
                }
            }

            // Quick Stats Row
            item {
                userProfile?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFFFF3E0), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Level ${it.level}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "${it.totalXP} XP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFFFEBEE), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${it.currentStreak} Days",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
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
            }


            // Subject Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF4E6AF6)
                            )
                            Text(
                                text = "Subject",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            placeholder = {
                                Text(
                                    "e.g., Physics, Math, History...",
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF5F5F7),
                                unfocusedContainerColor = Color(0xFFF5F5F7),
                                focusedBorderColor = Color(0xFF4E6AF6),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Topics Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF4E6AF6)
                            )
                            Text(
                                text = "Topics to Learn",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        OutlinedTextField(
                            value = topics,
                            onValueChange = { topics = it },
                            placeholder = {
                                Text(
                                    "e.g., Newton's Laws, Momentum...",
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF5F5F7),
                                unfocusedContainerColor = Color(0xFFF5F5F7),
                                focusedBorderColor = Color(0xFF4E6AF6),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Start Button
            item {
                Button(
                    onClick = {
                        if (subject.isNotBlank() && topics.isNotBlank()) {
                            viewModel.startStudyJourney(subject, topics)
                            onNavigateToStudy()
                        }
                    },
                    enabled = isModelReady && subject.isNotBlank() && topics.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4E6AF6),
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Learning Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isModelReady) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isModelReady) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFFFFC107),
                                strokeWidth = 3.dp
                            )
                        }
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isModelReady) Color(0xFF2E7D32) else Color(0xFFF57C00),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
// ... existing code ...

@Composable
fun ModernProfileCard(
    userProfile: UserProfile,
    onProfileClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProfileClick),
        backgroundColor = Color.White.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Gold, Yellow40)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "Level ${userProfile.level}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                        Text(
                            text = "${userProfile.totalXP} XP",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(CoralPink.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = CoralPink
                    )
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "${userProfile.currentStreak}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = CoralPink
                        )
                        Text(
                            text = "streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCardWithIcon(
    icon: ImageVector,
    label: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = modifier
            .scale(scale)
            .aspectRatio(1f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyJourneyScreen(
    viewModel: FirebaseStudyViewModel,
    onNavigateBack: () -> Unit
) {
    val studyMessages by viewModel.studyMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    var questionText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        // Floating Back Button - overlays content
        IconButton(
            onClick = onNavigateBack,
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
        Column(modifier = Modifier.fillMaxSize()) {

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (studyMessages.isEmpty() && isGenerating) {
                    item {
                        CleanLoadingCard()
                    }
                }

                items(studyMessages) { message ->
                    when (message) {
                        is StudyMessage.UserInput -> CleanUserMessage(message.text)
                        is StudyMessage.StreamingAI -> CleanAIMessage(message.text)
                        is StudyMessage.IntroMessage -> CleanIntroCard(message.text)
                        is StudyMessage.ChapterContent -> CleanChapterCard(
                            message.chapter,
                            message.chapterNumber
                        )
                        is StudyMessage.ClosingMessage -> CleanClosingCard(message.text)
                        is StudyMessage.LearningOptions -> CleanLearningOptionsCard(
                            message.subject,
                            message.topics,
                            message.options,
                            viewModel
                        )
                    }
                }

                // Quiz/Flashcard options
                if (studyMessages.isNotEmpty() && !isGenerating) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4E6AF6),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Test Your Knowledge",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.askFollowUpQuestion("quiz") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4E6AF6)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Quiz", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.askFollowUpQuestion("flashcards") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFC107)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Flashcards", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Auto-scroll
            LaunchedEffect(studyMessages.size) {
                if (studyMessages.isNotEmpty()) {
                    listState.animateScrollToItem(studyMessages.size - 1)
                }
            }

            // Clean Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask anything...", color = Color.Gray) },
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isGenerating,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F7),
                            unfocusedContainerColor = Color(0xFFF5F5F7),
                            focusedBorderColor = Color(0xFF4E6AF6),
                            unfocusedBorderColor = Color.Transparent,
                            disabledContainerColor = Color(0xFFF5F5F7),
                            disabledBorderColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = {
                            if (!isGenerating && questionText.isNotBlank()) {
                                viewModel.askFollowUpQuestion(questionText)
                                questionText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (!isGenerating && questionText.isNotBlank())
                                    Color(0xFF4E6AF6)
                                else
                                    Color(0xFFE0E0E0),
                                shape = CircleShape
                            ),
                        enabled = !isGenerating && questionText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CleanAIMessage(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF4E6AF6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                if (text.contains("http://") || text.contains("https://")) {
                    ClickableUrlText(text = text, modifier = Modifier.padding(12.dp))
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CleanUserMessage(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4E6AF6)),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun CleanIntroCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Your Journey Begins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF424242)
            )
        }
    }
}

@Composable
fun CleanChapterCard(chapter: StudyChapter, chapterNumber: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF4E6AF6), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$chapterNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Text(
                text = chapter.story,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF424242)
            )

            if (chapter.resources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Resources",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E6AF6)
                )
                chapter.resources.forEach { resource ->
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4E6AF6), CircleShape)
                        )
                        Text(
                            text = resource.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CleanClosingCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Journey Complete!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF424242),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CleanLoadingCard() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF4E6AF6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF4E6AF6),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Thinking...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun CleanLearningOptionsCard(
    subject: String,
    topics: String,
    options: List<LearningOption>,
    viewModel: FirebaseStudyViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose Your Learning Style",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            options.forEach { option ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectLearningStyle(subject, topics, option.id)
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F7)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF4E6AF6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.emoji,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClickableUrlText(text: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val urlPattern = Regex("https?://[^\\s]+")

    Column(modifier = modifier.fillMaxWidth()) {
        val lines = text.split("\n")
        lines.forEach { line ->
            if (urlPattern.containsMatchIn(line)) {
                val matches = urlPattern.findAll(line).toList()
                if (matches.isNotEmpty()) {
                    // Line contains URL
                    val url = matches.first().value
                    val beforeUrl = line.substringBefore(url)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (beforeUrl.isNotEmpty()) {
                            Text(
                                text = beforeUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = " 🔗 TAP",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4E6AF6),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(url)
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.util.Log.e("Link", "Failed to open URL: $url", e)
                                    }
                                }
                                .padding(horizontal = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            } else {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    viewModel: FirebaseStudyViewModel,
    onNavigateBack: () -> Unit
) {
    val availableModels by viewModel.availableModels.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val currentModelId by viewModel.currentModelId.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isModelLoading by viewModel.isModelLoading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        // Floating Back Button
        IconButton(
            onClick = onNavigateBack,
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

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF4E6AF6),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        downloadProgress?.let { progress ->
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF4E6AF6),
                                trackColor = Color(0xFFE0E0E0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                if (availableModels.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF4E6AF6),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Loading models...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(availableModels) { model ->
                            CleanModelCard(
                                model = model,
                                isLoaded = model.id == currentModelId,
                                isLoading = isModelLoading,
                                onDownload = { viewModel.downloadModel(model.id) },
                                onLoad = { viewModel.loadModel(model.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanModelCard(
    model: com.runanywhere.sdk.models.ModelInfo,
    isLoaded: Boolean,
    isLoading: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoaded) Color(0xFFE8F5E9) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (isLoaded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDownload,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = !model.isDownloaded && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4E6AF6),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (model.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (model.isDownloaded) "Downloaded" else "Download",
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onLoad,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = model.isDownloaded && !isLoaded && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun FirebaseProfileHUD(userProfile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Purple80
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Level and XP
            Column {
                Text(
                    text = "Level ${userProfile.level}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Text(
                    text = "${userProfile.totalXP} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Teal40
                )
            }

            // Right side - Streak
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${userProfile.currentStreak}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                }
                Text(
                    text = "day streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

