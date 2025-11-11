# 🎓 StudyChamp - AI-Powered Study Companion

<p align="center">
  <img src="app/src/main/res/drawable/studychamp.png" alt="StudyChamp Logo" width="200"/>
</p>

<p align="center">
  <strong>Your personal AI study companion with gamification, mentors, and offline learning</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#setup">Setup</a> •
  <a href="#firebase-setup">Firebase Setup</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#architecture">Architecture</a>
</p>

---

## 🌟 Overview

**StudyChamp** is an advanced Android study application that combines AI-powered learning with
gamification elements. Using the RunAnywhere SDK for offline AI capabilities, students can learn any
subject with personalized mentors, earn XP, unlock achievements, and track their progress - all
without requiring an internet connection for core features.

## ✨ Features

### 🤖 AI-Powered Learning

- **Offline AI Model** using RunAnywhere SDK (local LLM execution)
- **Streaming AI Responses** for real-time interaction
- **Multiple Learning Styles**: Story-based, Resources, Definitions, Roadmap
- **Context-Aware Responses** tailored to your learning needs

### 🧙‍♂️ AI Mentor Personalities

Choose your personal AI mentor with unique teaching styles:

- **🧙‍♂️ Sensei** - Calm, philosophical, wisdom-focused
- **⚡ Coach Max** - Energetic, motivational, enthusiastic
- **🌸 Mira** - Gentle, story-driven, emotional and caring

All AI responses adapt to match your mentor's personality!

### 🎮 Complete Gamification System

- **XP Points**: Earn 25 XP per correct quiz answer, 10 XP per mastered flashcard
- **Level Progression**: 10 levels from beginner to champion (0 → 4000 XP)
- **Daily Streaks**: Track consecutive study days with 🔥 indicator
- **10 Unlockable Achievements**:
    - 📝 Quiz Rookie (Complete 1 quiz) - 50 XP
    - 🧠 Problem-Solving Pro (Score 100%) - 100 XP
    - ⚔️ Concept Conqueror (Complete 5 topics) - 150 XP
    - 🔥 3-Day Streak - 75 XP
    - ⭐ Streak Star (7 days) - 200 XP
    - 🃏 Flashcard Master (25 cards) - 120 XP
    - 🌟 Rising Star (Level 5) - 250 XP
    - 🏆 Champion Scholar (Level 10) - 500 XP
    - 🎯 Quiz Marathon (10 quizzes) - 180 XP
    - 💎 Perfect Week - 300 XP

### 📝 AI-Generated Quizzes

- **Real Educational Content** - Not generic study tips!
- **Topic-Specific Questions** for Physics, Math, History, and more
- **5 Questions per Quiz** with multiple-choice format
- **Motivational Hints** for wrong answers
- **Real-Time XP Tracking** shown in top bar
- **Beautiful Completion Screen** with score and XP summary
- **Automatic Firebase Sync** to save progress

### 🃏 AI-Generated Flashcards

- **5 Cards per Topic** with term and definition
- **Swipeable Card UI** with smooth animations
- **3D Flip Animation** to reveal answers
- **Mastery Tracking** for each card
- **10 XP per Mastered Card**
- **Progress Indicators** and confetti on completion

### 👤 Profile Management

- **Create/Edit Profile** with name and email
- **Real-Time Stats Display**:
    - Current Level and Total XP
    - Current Streak and Longest Streak
    - Topics, Quizzes, and Flashcards Completed
- **Profile Picture Support** (ready for implementation)
- **Firebase Cloud Sync** for cross-device access

### 🔥 Firebase Integration

- **Anonymous Authentication** for instant access
- **Cloud Firestore Database** for profile storage
- **Real-Time Updates** using Flow
- **Quiz History Tracking**
- **Flashcard Progress Sync**
- **Achievement Unlocking**
- **Study Session Analytics**

### 🎨 Beautiful UI/UX

- **Material 3 Design** with purple theme
- **Edge-to-Edge Display** (no white headers!)
- **Smooth Animations** and transitions
- **XP Pop-up Animations** when earning points
- **Confetti Effects** on achievements
- **Responsive Layouts** optimized for all screen sizes

## 🛠 Tech Stack

### Core Technologies

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Async**: Coroutines + Flow
- **Dependency Injection**: Manual (ready for Hilt)

### AI & ML

- **RunAnywhere SDK** v0.1.3-alpha
    - Core SDK (4.01MB)
    - LLM Module with llama.cpp
    - 7 ARM64 CPU variants for optimization
    - Offline AI execution

### Firebase

- **Firebase Authentication** (Anonymous + Email ready)
- **Cloud Firestore** for real-time database
- **Firebase Analytics** (configured)

### Local Storage

- **Room Database** 2.6.1
    - User progress caching
    - Quiz/Flashcard history
    - Achievement tracking
- **SharedPreferences** for app settings

### Networking

- **Ktor Client** 3.0.3
- **OkHttp** 4.12.0
- **Retrofit** 2.11.0

### Serialization

- **Kotlinx Serialization** 1.7.3
- **Gson** 2.11.0

### UI Libraries

- **Compose BOM** (latest stable)
- **Material Icons Extended**
- **Core SplashScreen** 1.0.1

## 📦 Setup

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK** 17 or higher
- **Android SDK** API 24+ (Minimum) / API 36 (Target)
- **Gradle** 8.2+

### Installation Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/AnshulAlgoS/StudyChamp.git
   cd StudyChamp
   ```

2. **Open in Android Studio**
    - File → Open → Select the project folder
    - Wait for Gradle sync to complete

3. **Add RunAnywhere SDK Libraries**

   The app requires RunAnywhere SDK AAR files in `app/libs/`:
    - `RunAnywhereKotlinSDK-release.aar` (4.01MB)
    - `runanywhere-llm-llamacpp-release.aar` (2.12MB)

   Download
   from [RunAnywhere GitHub Releases v0.1.3-alpha](https://github.com/runanywhere/sdk/releases)

4. **Firebase Setup** (See [Firebase Setup](#firebase-setup) section)

5. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```
   Or use the ▶️ Run button in Android Studio

## 🔥 Firebase Setup

### 1. Enable Anonymous Authentication

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project: **studychamp-12bba**
3. Navigate to: **Authentication** → **Sign-in method**
4. Click **Anonymous** → **Enable** → **Save**

### 2. Firestore Database Setup

1. Go to **Firestore Database** in Firebase Console
2. Click **Create Database**
3. Choose **Production Mode** (or Test Mode for development)
4. Select a region close to your users
5. The app will automatically create these collections:
    - `users` - User profiles
    - `quiz_results` - Quiz history
    - `flashcard_progress` - Flashcard tracking
    - `achievements` - Unlocked achievements
    - `study_sessions` - Session analytics

### 3. Security Rules (Recommended)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Quiz results - user-specific
    match /quiz_results/{resultId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
    
    // Flashcard progress - user-specific
    match /flashcard_progress/{progressId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
    
    // Achievements - user-specific
    match /achievements/{achievementId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
    
    // Study sessions - user-specific
    match /study_sessions/{sessionId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
  }
}
```

## 📸 Screenshots

*Coming soon - Add screenshots of your app here!*

## 🏗 Architecture

### Project Structure

```
app/src/main/java/com/runanywhere/startup_hackathon20/
├── MainActivity.kt                 # Main entry point with navigation
├── MyApplication.kt               # Application class
├── ChatViewModel.kt               # Legacy chat functionality
├── StudyViewModel.kt              # Original study ViewModel
├── EnhancedStudyViewModel.kt      # Enhanced with Room DB
├── FirebaseStudyViewModel.kt      # ⭐ Main ViewModel with Firebase
├── GamificationModels.kt          # XP, achievements, progress models
├── FirebaseModels.kt              # Firebase-specific data models
├── StudyModels.kt                 # Study journey data models
├── database/
│   ├── StudyChampDao.kt          # Room database DAOs
│   └── StudyChampDatabase.kt     # Database configuration
├── repository/
│   ├── GamificationRepository.kt # Room operations
│   └── FirebaseRepository.kt     # ⭐ Firebase operations
└── ui/
    ├── MentorSelectionScreen.kt  # Mentor choice UI
    ├── QuizScreen.kt             # Quiz UI with animations
    ├── FlashcardScreen.kt        # Flashcard swipe UI
    ├── AchievementsAndProfile.kt # Achievements grid
    ├── ProfileScreen.kt          # Profile management
    └── theme/                    # Material 3 theme files
```

### Key Components

#### FirebaseStudyViewModel

Main ViewModel handling:

- User authentication and profile management
- AI quiz/flashcard generation
- XP calculation and Firebase sync
- Achievement checking and unlocking
- Study journey orchestration

#### FirebaseRepository

Centralized Firebase operations:

- User CRUD operations
- Quiz/flashcard result saving
- Real-time data streaming with Flow
- Achievement tracking
- Session analytics

#### GamificationModels

Data classes for:

- `UserProgress` - XP, level, streaks
- `Achievement` - Badges and rewards
- `QuizData` & `FlashcardSet` - Learning content
- `XPSystem` - Level calculation logic

## 🚀 Usage

### First Launch

1. **App opens** → Automatic anonymous authentication
2. **Profile Setup** → Enter your name (email optional)
3. **Mentor Selection** → Choose your AI mentor
4. **Home Screen** → Ready to learn!

### Starting a Study Session

1. Enter a **Subject** (e.g., "Physics")
2. Enter **Topics** (e.g., "Newton's Laws, Gravitation")
3. Tap **"Start My Study Journey"**
4. Choose a **Learning Style** (Story/Resources/Definitions/Roadmap)
5. Learn from AI-generated content

### Taking Quizzes

**Method 1**: Tap the **"📝 Take Quiz"** button after learning content
**Method 2**: Type **"quiz"** in the chat input
**Method 3**: Use the quiz card in the study journey

- Answer 5 questions
- Earn 25 XP per correct answer
- See hints for wrong answers
- View completion summary with total XP

### Using Flashcards

**Method 1**: Tap the **"🃏 Flashcards"** button
**Method 2**: Type **"flashcards"** in the chat

- Swipe through 5 cards
- Tap to flip and see definitions
- Mark cards as "Mastered"
- Earn 10 XP per mastered card

### Tracking Progress

- **Profile Screen**: View your level, XP, streaks, and stats
- **Achievements Screen**: See locked/unlocked achievements with requirements
- **Profile HUD**: Quick stats displayed on home screen

## 🎯 Roadmap

### Coming Soon

- [ ] More AI models support (GPT-4, Claude, Gemini)
- [ ] Voice input/output for questions
- [ ] Spaced repetition algorithm for flashcards
- [ ] Study groups and leaderboards
- [ ] Custom quiz creation
- [ ] Dark mode support
- [ ] Export study notes as PDF
- [ ] Pomodoro timer integration
- [ ] Calendar view for study sessions

### Future Enhancements

- [ ] Email/Google authentication
- [ ] Social sharing of achievements
- [ ] Weekly study reports
- [ ] AI tutor video explanations
- [ ] Collaborative study rooms
- [ ] Parent/teacher dashboard
- [ ] Offline mode improvements
- [ ] Widget support

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Anshul Saxena**

- GitHub: [@AnshulAlgoS](https://github.com/AnshulAlgoS)
- Project: [StudyChamp](https://github.com/AnshulAlgoS/StudyChamp)

## 🙏 Acknowledgments

- **RunAnywhere SDK** for offline AI capabilities
- **Firebase** for cloud infrastructure
- **Material Design 3** for beautiful UI components
- **Jetpack Compose** team for modern Android UI toolkit
- All open-source contributors whose libraries power this app

## 📞 Support

For issues, questions, or feature requests:

- Open an [Issue](https://github.com/AnshulAlgoS/StudyChamp/issues)
- Check existing [Discussions](https://github.com/AnshulAlgoS/StudyChamp/discussions)

---

<p align="center">
  Made with ❤️ for students worldwide
  <br>
  <strong>Study Smart, Champion! 🎓🏆</strong>
</p>

