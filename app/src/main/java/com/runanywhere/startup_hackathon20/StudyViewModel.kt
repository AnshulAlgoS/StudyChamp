package com.runanywhere.startup_hackathon20

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StudyViewModel : ViewModel() {

    // Model management
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId

    private val _statusMessage = MutableStateFlow<String>("Welcome Champ! 🎓")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady

    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading

    // Study journey state
    private val _currentJourney = MutableStateFlow<StudyJourney?>(null)
    val currentJourney: StateFlow<StudyJourney?> = _currentJourney

    private val _studyMessages = MutableStateFlow<List<StudyMessage>>(emptyList())
    val studyMessages: StateFlow<List<StudyMessage>> = _studyMessages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    init {
        loadAvailableModels()
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val models = listAvailableModels()
                _availableModels.value = models

                // Check if any model is already loaded
                val loadedModel = models.firstOrNull { it.isDownloaded }
                if (loadedModel != null && _currentModelId.value == null) {
                    _statusMessage.value = "Ready! Download and load a model to start learning 📚"
                } else {
                    _statusMessage.value = "Download a model to begin your journey! 🚀"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading models: ${e.message}"
            }
        }
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Downloading your AI mentor... 📥"
                RunAnywhere.downloadModel(modelId).collect { progress ->
                    _downloadProgress.value = progress
                    _statusMessage.value = "Downloading: ${(progress * 100).toInt()}%"
                }
                _downloadProgress.value = null
                _statusMessage.value = "Download complete! Now tap 'Load' to activate ✨"
            } catch (e: Exception) {
                _statusMessage.value = "Download failed: ${e.message}"
                _downloadProgress.value = null
            }
        }
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            if (_isModelLoading.value) {
                return@launch
            }
            _isModelLoading.value = true
            try {
                android.util.Log.d("StudyViewModel", "🔄 Attempting to load model: $modelId")
                _statusMessage.value = "Loading your AI mentor... 🧠"
                val success = RunAnywhere.loadModel(modelId)
                android.util.Log.d("StudyViewModel", "📊 Load model result: $success")
                if (success) {
                    _currentModelId.value = modelId
                    _isModelReady.value = true
                    _statusMessage.value = "Ready to learn, Champ! 🎉"
                    android.util.Log.d("StudyViewModel", "✅ Model loaded successfully!")

                    // Refresh models list to update UI state
                    loadAvailableModels()
                } else {
                    android.util.Log.e("StudyViewModel", "❌ Model loading returned false")
                    _statusMessage.value = "Failed to load model - please try again"
                }
            } catch (e: Exception) {
                android.util.Log.e("StudyViewModel", "❌ Error loading model: ${e.message}", e)
                _statusMessage.value = "Error loading model: ${e.message}"
            } finally {
                _isModelLoading.value = false
            }
        }
    }

    fun refreshModels() {
        loadAvailableModels()
    }

    fun startStudyJourney(subject: String, topics: String) {
        if (_currentModelId.value == null) {
            android.util.Log.e(
                "StudyViewModel",
                "❌ No model selected!"
            )
            _statusMessage.value = "Please download and load a model first, Champ!"
            return
        }

        if (!_isModelReady.value) {
            android.util.Log.w("StudyViewModel", "⚠️ Model not ready, attempting to reload...")
            _statusMessage.value = "Model not ready, loading it now... ⏳"
            // Try to load the model
            loadModel(_currentModelId.value!!)
            // Give it a moment and then check again
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000) // Wait 3 seconds
                if (_isModelReady.value) {
                    _statusMessage.value = "Model loaded! Please try starting your journey again."
                } else {
                    _statusMessage.value =
                        "Failed to load model. Please go to Model Settings and load it manually."
                }
            }
            return
        }

        if (subject.isBlank() || topics.isBlank()) {
            _statusMessage.value = "Please enter both subject and topics!"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _studyMessages.value = emptyList()
            _statusMessage.value = "Creating your intro... "

            try {
                // Generate intro first
                val introPrompt =
                    "Write one motivating paragraph (3-4 sentences) about why learning $topics in $subject is exciting and useful. Call me Champ. Be enthusiastic!"

                android.util.Log.d("StudyViewModel", " Generating intro")

                var intro = ""
                RunAnywhere.generateStream(introPrompt).collect { token ->
                    intro += token
                    _studyMessages.value = listOf(StudyMessage.StreamingAI(intro))
                }

                android.util.Log.d("StudyViewModel", " Intro generated: ${intro.length} chars")

                // Add learning style options
                _studyMessages.value += StudyMessage.LearningOptions(
                    subject = subject,
                    topics = topics,
                    options = LearningStyles.getAll()
                )

                _statusMessage.value = "Choose how you want to learn, Champ! "

            } catch (e: Exception) {
                android.util.Log.e("StudyViewModel", " Error: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
                _studyMessages.value += StudyMessage.StreamingAI("Oops! Let's try again, Champ! ")
            }

            _isGenerating.value = false
        }
    }

    fun selectLearningStyle(subject: String, topics: String, styleId: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            _statusMessage.value = "Creating your content... "

            try {
                when (styleId) {
                    "story" -> generateStoryContent(subject, topics)
                    "resources" -> showResourcesContent(subject, topics)
                    "definition" -> generateDefinitions(subject, topics)
                    "roadmap" -> generateRoadmap(subject, topics)
                }
            } catch (e: Exception) {
                android.util.Log.e("StudyViewModel", " Error: ${e.message}", e)
                _studyMessages.value += StudyMessage.StreamingAI("Error: ${e.message}")
            }

            _isGenerating.value = false
        }
    }

    private suspend fun generateStoryContent(subject: String, topics: String) {
        val prompt =
            "Explain $topics in $subject using a simple real-world analogy. Compare it to something everyday people understand. Give one clear example. Make it memorable. Under 100 words."

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            val messages = _studyMessages.value.toMutableList()
            if (messages.lastOrNull() is StudyMessage.StreamingAI) {
                messages[messages.lastIndex] = StudyMessage.StreamingAI(response)
            } else {
                messages.add(StudyMessage.StreamingAI(response))
            }
            _studyMessages.value = messages
        }

        _statusMessage.value = "Story complete! Want to see resources? "
    }

    private suspend fun showResourcesContent(subject: String, topics: String) {
        val resources = buildResourcesList(subject, topics)
        _studyMessages.value += StudyMessage.StreamingAI(resources)
        _statusMessage.value = "Resources ready! Tap any link to open! "
    }

    private suspend fun generateDefinitions(subject: String, topics: String) {
        val prompt =
            "Define the 3 most important terms in $topics for $subject. Each definition should be 2 sentences: what it is, and why it matters. Be precise and accurate."

        var response = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            response += token
            val messages = _studyMessages.value.toMutableList()
            if (messages.lastOrNull() is StudyMessage.StreamingAI) {
                messages[messages.lastIndex] = StudyMessage.StreamingAI(response)
            } else {
                messages.add(StudyMessage.StreamingAI(response))
            }
            _studyMessages.value = messages
        }

        _statusMessage.value = "Definitions ready! "
    }

    private suspend fun generateRoadmap(subject: String, topics: String) {
        // First, generate AI roadmap
        val prompt =
            "For $topics in $subject, list EXACTLY what concepts to learn in order. Format: Week 1: [specific topics]. Week 2: [build on week 1]. Week 3: [advanced concepts]. Be specific about what to study."

        var aiRoadmap = ""
        RunAnywhere.generateStream(prompt).collect { token ->
            aiRoadmap += token
            val messages = _studyMessages.value.toMutableList()
            if (messages.lastOrNull() is StudyMessage.StreamingAI) {
                messages[messages.lastIndex] = StudyMessage.StreamingAI(aiRoadmap)
            } else {
                messages.add(StudyMessage.StreamingAI(aiRoadmap))
            }
            _studyMessages.value = messages
        }

        // Then add detailed curated roadmap
        val detailedRoadmap = buildDetailedRoadmap(subject, topics)
        _studyMessages.value += StudyMessage.StreamingAI(detailedRoadmap)

        _statusMessage.value = "Complete roadmap ready! "
    }

    private fun buildDetailedRoadmap(subject: String, topics: String): String {
        return when (subject.lowercase()) {
            "physics" -> """

 Complete Mastery Roadmap:

 Phase 1: Foundation (Week 1-2)
What to learn:
• Basic concepts and terminology
• Fundamental equations
• Units and measurements
• Simple problem-solving

How to practice:
• Watch intro videos
• Do 10-15 basic problems daily
• Use PhET simulations
• Make formula flashcards

 Phase 2: Application (Week 3-4)
What to learn:
• Complex problems
• Multiple concept integration
• Real-world applications
• Common problem patterns

How to practice:
• Solve 20+ practice problems
• Work through Khan Academy exercises
• Explain concepts to others
• Start a practice journal

 Phase 3: Mastery (Week 5-6)
What to learn:
• Advanced problem types
• Exam-level questions
• Edge cases and exceptions
• Speed and accuracy

How to practice:
• Timed practice tests
• Challenge problems
• Teaching others
• Review and reinforce weak areas"""

            "math", "mathematics", "maths" -> """

 Complete Mastery Roadmap:

 Phase 1: Understanding (Week 1-2)
What to learn:
• Core definitions and rules
• Basic operations and properties
• Foundational theorems
• Standard notation

How to practice:
• 15-20 basic problems daily
• Write out solution steps
• Use Wolfram Alpha to check
• Create concept maps

 Phase 2: Problem Solving (Week 3-4)
What to learn:
• Multi-step problems
• Word problems
• Pattern recognition
• Common mistakes to avoid

How to practice:
• Mixed practice sets
• Brilliant.org challenges
• Study with problem-solving groups
• Time yourself on problems

 Phase 3: Advanced Skills (Week 5-6)
What to learn:
• Competition-level problems
• Proof techniques
• Advanced applications
• Speed solving strategies

How to practice:
• Past exam papers
• Competition problems
• Teach the material
• Create your own problems"""

            "chemistry" -> """

 Complete Mastery Roadmap:

 Phase 1: Basics (Week 1-2)
What to learn:
• Periodic table fundamentals
• Basic bonding and structure
• Mole concept and stoichiometry
• Naming conventions

How to practice:
• Memorize first 20 elements
• Balance 30+ equations
• Use ptable.com interactive tools
• Draw molecular structures

 Phase 2: Reactions (Week 3-4)
What to learn:
• Types of reactions
• Reaction mechanisms
• Equilibrium concepts
• Energy changes

How to practice:
• Predict reaction products
• Virtual lab simulations
• Solve stoichiometry problems
• Make reaction flashcards

 Phase 3: Applications (Week 5-6)
What to learn:
• Advanced concepts
• Lab techniques
• Real-world chemistry
• Problem-solving strategies

How to practice:
• Complex multi-step problems
• Lab report analysis
• Connect concepts together
• Review with practice exams"""

            else -> """

 Complete Mastery Roadmap:

 Phase 1: Learn the Basics (Week 1-2)
• Understand core vocabulary and concepts
• Watch beginner tutorials
• Do simple exercises
• Take notes on key ideas
• Practice 30 minutes daily

 Phase 2: Build Skills (Week 3-4)
• Apply concepts to problems
• Work through practice sets
• Study examples and solutions
• Join study groups
• Practice 1 hour daily

 Phase 3: Master the Topic (Week 5-6)
• Solve advanced problems
• Take practice tests
• Teach the material to others
• Review weak areas
• Practice 1-2 hours daily

 Study Tips:
• Consistency beats cramming
• Active practice > passive reading
• Test yourself frequently
• Explain concepts out loud
• Review regularly"""
        }
    }

    private fun buildResourcesList(subject: String, topics: String): String {
        // Curated resources with actual URLs
        val youtubeSearch = "https://www.youtube.com/results?search_query=" + topics.replace(
            " ",
            "+"
        ) + "+" + subject.replace(" ", "+")

        return when (subject.lowercase()) {
            "physics" -> """

 Your Learning Resources:

 YouTube Videos (tap to open):
• Physics Girl: $youtubeSearch+physics+girl
• Veritasium: $youtubeSearch+veritasium  
• MinutePhysics: $youtubeSearch+minutephysics

 Practice Sites:
• PhET Simulations: https://phet.colorado.edu
• Khan Academy Physics: https://khanacademy.org/science/physics
• Physics Classroom: https://physicsclassroom.com

 Reference:
• HyperPhysics: http://hyperphysics.phy-astr.gsu.edu
• Questions: https://physics.stackexchange.com"""

            "math", "mathematics", "maths" -> """

 Your Learning Resources:

 YouTube Videos (tap to open):
• 3Blue1Brown: $youtubeSearch+3blue1brown
• PatrickJMT: $youtubeSearch+patrickjmt
• Khan Academy: $youtubeSearch+khan+academy

 Practice Sites:
• Khan Academy Math: https://khanacademy.org/math
• Brilliant: https://brilliant.org
• Wolfram Alpha: https://wolframalpha.com

 Reference:
• Better Explained: https://betterexplained.com
• Math is Fun: https://mathsisfun.com"""

            "chemistry" -> """

 Your Learning Resources:

 YouTube Videos (tap to open):
• CrashCourse: $youtubeSearch+crashcourse
• Tyler DeWitt: $youtubeSearch+tyler+dewitt
• Professor Dave: $youtubeSearch+professor+dave

 Practice Sites:
• Khan Academy Chemistry: https://khanacademy.org/science/chemistry
• Periodic Table: https://ptable.com
• ChemCollective Labs: https://chemcollective.org

 Reference:
• Chem Guide: https://chemguide.co.uk
• Chemistry LibreTexts: https://chem.libretexts.org"""

            "biology" -> """

 Your Learning Resources:

 YouTube Videos (tap to open):
• CrashCourse Biology: $youtubeSearch+crashcourse+biology
• Amoeba Sisters: $youtubeSearch+amoeba+sisters
• Bozeman Science: $youtubeSearch+bozeman+science

 Practice Sites:
• Khan Academy Biology: https://khanacademy.org/science/biology
• HHMI BioInteractive: https://biointeractive.org
• Biology Online: https://biology-online.org

 Reference:
• Nature Education: https://nature.com/scitable
• Biology LibreTexts: https://bio.libretexts.org"""

            "history" -> """

 Your Learning Resources:

 YouTube Videos (tap to open):
• CrashCourse History: $youtubeSearch+crashcourse+history
• OverSimplified: $youtubeSearch+oversimplified
• History Matters: $youtubeSearch+history+matters

 Learning Sites:
• Khan Academy History: https://khanacademy.org/humanities/world-history
• Britannica: https://britannica.com
• History.com: https://history.com

 Reference:
• Smithsonian: https://smithsonianmag.com
• World History Encyclopedia: https://worldhistory.org"""

            "programming", "coding", "computer science" -> """

 Your Learning Resources:

 YouTube Videos (tap to open):
• freeCodeCamp: $youtubeSearch+freecodecamp
• Fireship: $youtubeSearch+fireship
• Web Dev Simplified: $youtubeSearch+web+dev+simplified

 Practice Coding:
• freeCodeCamp: https://freecodecamp.org
• LeetCode: https://leetcode.com
• HackerRank: https://hackerrank.com

 Documentation:
• MDN Web Docs: https://developer.mozilla.org
• W3Schools: https://w3schools.com"""

            else -> """

 Your Learning Resources:

 YouTube Videos:
Search: $youtubeSearch

 Learning Sites:
• Khan Academy: https://khanacademy.org
• Search: "$subject $topics tutorial"

 Community:
• Reddit: https://reddit.com/r/$subject
• Stack Exchange: https://stackexchange.com

 Tip: Tap any link to open in browser!"""
        }
    }

    fun askFollowUpQuestion(question: String) {
        if (_currentModelId.value == null || !_isModelReady.value) {
            _statusMessage.value = "Please load a model first, Champ!"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _studyMessages.value += StudyMessage.UserInput(question)

            try {
                val prompt =
                    """You are an enthusiastic AI study mentor. The student (Champ) has a question: "$question"

Answer in a warm, encouraging, storytelling way. Keep it concise (under 150 words) but engaging. Use metaphors and real-world examples. Address them as "Champ" or "Explorer"."""

                var aiResponse = ""

                RunAnywhere.generateStream(prompt).collect { token ->
                    aiResponse += token

                    val messages = _studyMessages.value.toMutableList()
                    if (messages.lastOrNull() is StudyMessage.StreamingAI) {
                        messages[messages.lastIndex] = StudyMessage.StreamingAI(aiResponse)
                    } else {
                        messages.add(StudyMessage.StreamingAI(aiResponse))
                    }
                    _studyMessages.value = messages
                }

            } catch (e: Exception) {
                _studyMessages.value += StudyMessage.StreamingAI("Oops! Something went wrong, Champ. Let's try again! 💪")
            }

            _isGenerating.value = false
        }
    }

    fun resetJourney() {
        _currentJourney.value = null
        _studyMessages.value = emptyList()
        _statusMessage.value = "Ready for a new adventure, Champ! 🎓"
    }
}
