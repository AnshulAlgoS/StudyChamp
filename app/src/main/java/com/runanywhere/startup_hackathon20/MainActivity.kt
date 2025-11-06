package com.runanywhere.startup_hackathon20

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
// import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // val splashScreen = installSplashScreen()  // Uncomment after Gradle sync

        // Make status bar purple - NO WHITE HEADER
        window.apply {
            statusBarColor = android.graphics.Color.parseColor("#6200EE")  // Purple40
            navigationBarColor = android.graphics.Color.parseColor("#6200EE")
        }

        // Set status bar icons to light color (white) since background is purple
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false  // Use light (white) icons on dark background
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "StudyChamp",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your AI Study Companion",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Purple40,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = LightBackground,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Profile HUD (Firebase version)
            item {
                userProfile?.let {
                    FirebaseProfileHUD(userProfile = it)
                }
            }

            // Quick actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Star,
                        label = "Achievements",
                        onClick = onNavigateToAchievements,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.Person,
                        label = "Profile",
                        onClick = onNavigateToProfile,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Current mentor display
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardLight.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentMentor.emoji,
                            style = MaterialTheme.typography.displaySmall
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Your Mentor: ${currentMentor.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentMentor.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Input fields
            item {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    placeholder = { Text("e.g., Physics, History, Math...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardLight,
                        unfocusedContainerColor = CardLight
                    ),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Edit, "Subject") }
                )
            }

            item {
                OutlinedTextField(
                    value = topics,
                    onValueChange = { topics = it },
                    label = { Text("Topics") },
                    placeholder = { Text("e.g., Newton's Laws, Momentum...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardLight,
                        unfocusedContainerColor = CardLight
                    ),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.List, "Topics") }
                )
            }

            // Start button
            item {
                Button(
                    onClick = {
                        if (subject.isNotBlank() && topics.isNotBlank()) {
                            viewModel.startStudyJourney(subject, topics)
                            onNavigateToStudy()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    enabled = isModelReady && subject.isNotBlank() && topics.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple40,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start My Study Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Models button
            item {
                OutlinedButton(
                    onClick = onNavigateToModels,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Model Settings")
                }
            }

            // Status message
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isModelReady) SuccessGreen.copy(alpha = 0.2f)
                    else WarningOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = if (isModelReady) SuccessGreen else WarningOrange
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Teal40
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Study Journey 🎓") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Purple40,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = LightBackground,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (studyMessages.isEmpty() && isGenerating) {
                    item {
                        LoadingCard()
                    }
                }

                items(studyMessages) { message ->
                    when (message) {
                        is StudyMessage.UserInput -> UserMessageCard(message.text)
                        is StudyMessage.StreamingAI -> AIMessageCard(message.text)
                        is StudyMessage.IntroMessage -> IntroCard(message.text)
                        is StudyMessage.ChapterContent -> ChapterCard(
                            message.chapter,
                            message.chapterNumber
                        )
                        is StudyMessage.ClosingMessage -> ClosingCard(message.text)
                        is StudyMessage.LearningOptions -> LearningOptionsCard(
                            message.subject,
                            message.topics,
                            message.options,
                            viewModel
                        )
                    }
                }

                // Show Quiz/Flashcard options after content
                if (studyMessages.isNotEmpty() && !isGenerating) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Teal80
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🎯 Test Your Knowledge!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Teal40
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.askFollowUpQuestion("quiz")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Purple40
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("📝 Take Quiz")
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.askFollowUpQuestion("flashcards")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Gold
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("🃏 Flashcards")
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

            // Input field for follow-up questions
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardLight,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask a question or type 'quiz' / 'flashcards'...") },
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isGenerating
                    )

                    FloatingActionButton(
                        onClick = {
                            if (!isGenerating && questionText.isNotBlank()) {
                                viewModel.askFollowUpQuestion(questionText)
                                questionText = ""
                            }
                        },
                        containerColor = Teal40,
                        modifier = Modifier.alpha(if (!isGenerating && questionText.isNotBlank()) 1f else 0.5f)
                    ) {
                        Icon(Icons.Default.Send, "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AIMessageCard(text: String) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Purple80
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Purple40,
                modifier = Modifier.size(24.dp)
            )

            // Check if text contains URLs
            if (text.contains("http://") || text.contains("https://")) {
                // Make URLs clickable
                ClickableUrlText(text = text)
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ClickableUrlText(text: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val urlPattern = Regex("https?://[^\\s]+")

    Column(modifier = Modifier.fillMaxWidth()) {
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = " 🔗 TAP",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Teal40,
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun UserMessageCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Teal80
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Teal40,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun IntroCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Yellow80
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🎯 Your Journey Begins",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Gold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ChapterCard(chapter: StudyChapter, chapterNumber: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardLight
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Chapter $chapterNumber: ${chapter.title}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Purple40
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = chapter.story,
                style = MaterialTheme.typography.bodyLarge
            )

            if (chapter.resources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "📚 Resources:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Teal40
                )
                Spacer(modifier = Modifier.height(4.dp))
                chapter.resources.forEach { resource ->
                    Text(
                        text = "• ${resource.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ClosingCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SuccessGreen.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🎉 Journey Complete!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Purple80
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Purple40
            )
            Text(
                text = "Creating your personalized journey... ✨",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun LearningOptionsCard(
    subject: String,
    topics: String,
    options: List<LearningOption>,
    viewModel: FirebaseStudyViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Yellow80
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "How would you like to learn, Champ?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Gold
            )
            Spacer(modifier = Modifier.height(12.dp))

            options.forEach { option ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardLight
                    ),
                    onClick = {
                        viewModel.selectLearningStyle(subject, topics, option.id)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.emoji,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Purple40
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Settings ⚙️") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshModels() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Purple40,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = LightBackground,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Yellow80
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    downloadProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = Gold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Available AI Models",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Purple40
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (availableModels.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardLight
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Loading models... Please wait.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableModels) { model ->
                        ModelCard(
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

@Composable
fun ModelCard(
    model: com.runanywhere.sdk.models.ModelInfo,
    isLoaded: Boolean,
    isLoading: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoaded) SuccessGreen.copy(alpha = 0.2f) else CardLight
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isLoaded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✓ Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
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
                    modifier = Modifier.weight(1f),
                    enabled = !model.isDownloaded && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal40
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (model.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (model.isDownloaded) "Downloaded" else "Download")
                }

                Button(
                    onClick = onLoad,
                    modifier = Modifier.weight(1f),
                    enabled = model.isDownloaded && !isLoaded && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple40
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load")
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

