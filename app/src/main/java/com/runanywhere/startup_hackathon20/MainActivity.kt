package com.runanywhere.startup_hackathon20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Startup_hackathon20Theme {
                StudyChampApp()
            }
        }
    }
}

@Composable
fun StudyChampApp(viewModel: StudyViewModel = viewModel()) {
    val studyMessages by viewModel.studyMessages.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            AppScreen.HOME -> HomeScreen(
                viewModel = viewModel,
                onNavigateToStudy = { currentScreen = AppScreen.STUDY },
                onNavigateToModels = { currentScreen = AppScreen.MODELS }
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
        }
    }
}

enum class AppScreen {
    HOME, STUDY, MODELS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StudyViewModel,
    onNavigateToStudy: () -> Unit,
    onNavigateToModels: () -> Unit
) {
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()

    var subject by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf("") }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
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
                text = "StudyChamp",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = DeepPurple
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your AI Study Companion",
                style = MaterialTheme.typography.titleMedium,
                color = DeepTeal
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome message
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardLight.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hey Champ! 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Purple40
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ready to learn something awesome today?",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Input fields
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                placeholder = { Text("e.g., Physics, History, Math...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardLight,
                    unfocusedContainerColor = CardLight
                ),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Edit, "Subject") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = topics,
                onValueChange = { topics = it },
                label = { Text("Topics") },
                placeholder = { Text("e.g., Newton's Laws, Momentum...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardLight,
                    unfocusedContainerColor = CardLight
                ),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.List, "Topics") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Start button
            Button(
                onClick = {
                    if (subject.isNotBlank() && topics.isNotBlank()) {
                        viewModel.startStudyJourney(subject, topics)
                        onNavigateToStudy()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Models button
            OutlinedButton(
                onClick = onNavigateToModels,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Model Settings")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status message
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyJourneyScreen(
    viewModel: StudyViewModel,
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
        containerColor = LightBackground
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
                        placeholder = { Text("Ask a question, Champ...") },
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
    viewModel: StudyViewModel
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
    viewModel: StudyViewModel,
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
                    containerColor = Teal40,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = LightBackground
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