# StudyChamp - Your AI Study Companion 🎓

An engaging Android study app that transforms learning into an adventure! StudyChamp uses the
RunAnywhere SDK for on-device AI inference to create personalized, storytelling-driven learning
experiences.

## 🌟 What This App Does

StudyChamp is your personal AI mentor that helps you learn any subject through engaging
storytelling. Simply enter your subject and topics, and watch as the AI creates a customized
learning journey just for you!

## ✨ Features

- **🎨 Vibrant Student-Friendly UI**: Beautiful gradient backgrounds with purple, teal, and yellow
  themes
- **📖 Storytelling Learning**: AI addresses you as "Champ" or "Explorer" and presents concepts as
  adventures
- **🗺️ Personalized Study Journeys**: AI breaks down topics into digestible chapters with narratives
- **💬 Interactive Q&A**: Ask follow-up questions and get motivational, story-driven responses
- **🤖 On-Device AI**: All inference runs locally on your Android device using RunAnywhere SDK
- **📱 Modern Jetpack Compose**: Clean, responsive UI with smooth animations

## 🚀 Quick Start

### 1. Build and Run

```bash
./gradlew assembleDebug
# Or open in Android Studio and click Run
```

### 2. Download & Load a Model

1. Launch StudyChamp
2. Tap **"Model Settings"** button on the home screen
3. Tap **"Download"** on a model (recommended: Qwen 2.5 0.5B Instruct - 374 MB)
4. Once downloaded, tap **"Load"** to activate the AI mentor
5. Go back to the home screen

### 3. Start Your Learning Journey!

1. Enter your **Subject** (e.g., "Physics", "History", "Programming")
2. Enter **Topics** you want to learn (e.g., "Newton's Laws, Momentum, Energy")
3. Tap **"Start My Study Journey"** 🚀
4. Watch as your AI mentor creates a personalized learning adventure!
5. Ask follow-up questions anytime using the chat input

## 🎯 How It Works

### Example Study Journey

**Input:**

- Subject: Physics
- Topics: Newton's Laws of Motion

**AI Response (Storytelling Style):**

```
Alright Champ! Today we're entering Newton's Realm, where three powerful 
laws govern the universe of motion. Think of yourself as a physics explorer, 
about to uncover secrets that explain everything from how you walk to how 
rockets soar through space!

Chapter 1: The Law of Inertia - Objects at Rest
Imagine you're sitting on a skateboard. Why don't you start moving on your 
own? That's inertia! Newton's first law tells us...

[Resources: Khan Academy video, Interactive simulation, Practice problems]

Chapter 2: Force and Acceleration - The Push Effect
Now picture pushing that skateboard. The harder you push, the faster you go...
```

## 🎨 App Screens

### Home Screen

- Welcome message with "Hey Champ! 👋"
- Subject and Topics input fields
- Beautiful gradient background
- Model status indicator
- Quick access to Model Settings

### Study Journey Screen

- Real-time AI story generation
- Color-coded message cards (AI, User)
- Follow-up question input
- Smooth auto-scrolling

### Model Management Screen

- List of available AI models
- Download progress tracking
- One-tap model loading
- Visual status indicators

## 📦 Available Models

| Model                       | Size   | Quality   | Best For                                     |
|-----------------------------|--------|-----------|----------------------------------------------|
| Qwen 2.5 0.5B Instruct Q6_K | 374 MB | Excellent | Story-driven learning, detailed explanations |

*More models can be added in `MyApplication.kt`*

## 🛠️ Technical Details

### SDK Components

- **RunAnywhere Core SDK**: Model management and inference
- **LlamaCpp Module**: Optimized on-device inference with 7 ARM64 CPU variants
- **Kotlin Coroutines**: Async operations and streaming responses

### Architecture

```
MyApplication (SDK initialization)
    ↓
StudyViewModel (study journey logic, AI prompts)
    ↓
StudyChamp UI (Home → Study Journey → Model Settings)
```

### Key Files

- **`MyApplication.kt`** - SDK initialization and model registration
- **`StudyViewModel.kt`** - Study journey management, storytelling prompts
- **`StudyModels.kt`** - Data models for chapters, resources, journeys
- **`MainActivity.kt`** - UI components (Home, Study, Models screens)
- **`ui/theme/`** - Vibrant color scheme and typography

## 🎨 Design Highlights

### Color Palette

- **Primary**: Purple shades (motivation, creativity)
- **Secondary**: Teal/Cyan (energy, focus)
- **Accent**: Yellow/Gold (achievement, success)
- **Status**: Green (success), Orange (warning), Red (error)

### UI Patterns

- Rounded corners (16-24dp) for friendly feel
- Elevated cards with shadows
- Smooth gradient backgrounds
- Icon-enhanced buttons
- Emoji reactions for engagement

## 💡 AI Storytelling Approach

The AI mentor:

- Addresses students as "Champ" or "Explorer"
- Uses metaphors and real-world connections
- Breaks complex topics into narrative "chapters"
- Provides motivational encouragement
- Suggests specific learning resources
- Keeps responses concise but engaging

## 🔧 Requirements

- Android 7.0 (API 24) or higher
- ~400 MB free storage (for model)
- Internet connection (for initial model download only)
- 2GB+ RAM recommended

## 🐛 Troubleshooting

### Models not loading

- Wait 10-15 seconds for SDK initialization
- Tap "Refresh" in Model Settings
- Check logcat for initialization errors

### Download fails

- Verify internet connection
- Ensure sufficient storage space
- Check INTERNET permission in AndroidManifest.xml

### AI responses seem generic

- Try smaller, more specific topics
- Ask follow-up questions for depth
- The AI quality depends on the model size

### App is slow

- Normal for on-device AI
- Smaller models = faster responses
- Close other apps to free RAM
- Device CPU affects performance

## 🚀 Customization Ideas

Want to enhance StudyChamp?

1. **Add Daily Streaks**: Track consecutive learning days
2. **Progress Persistence**: Save journeys with Room database
3. **Quiz Generation**: AI creates practice questions
4. **Study Schedules**: Remind students to learn
5. **Achievement System**: Badges for completed chapters
6. **Multi-Language**: Support different languages
7. **Voice Input**: Speak your questions
8. **Resource Integration**: Open links in-app
9. **Study Groups**: Share journeys with friends
10. **Adaptive Learning**: AI adjusts difficulty based on responses

## 📚 Resources

- [RunAnywhere SDK Complete Guide](RUNANYWHERE_SDK_COMPLETE_GUIDE.md)
- [RunAnywhere SDK Repository](https://github.com/RunanywhereAI/runanywhere-sdks)
- [Quick Start Android Guide](app/src/main/java/com/runanywhere/startup_hackathon20/QUICK_START_ANDROID.md)

## 🎓 Educational Philosophy

StudyChamp believes that:

- **Learning should be fun** - Stories make concepts memorable
- **Everyone's a champion** - Positive reinforcement builds confidence
- **Small steps matter** - Breaking topics into chapters prevents overwhelm
- **Questions are power** - Interactive learning deepens understanding
- **Resources guide the way** - Curated materials support self-directed learning

## 📄 License

This app uses the RunAnywhere SDK and follows its license terms.

---

**Built with ❤️ for students who want to learn smarter, not harder!**

🌟 Happy Learning, Champ! 🌟
