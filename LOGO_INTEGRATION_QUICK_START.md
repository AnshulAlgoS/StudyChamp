# StudyChamp Logo & Splash Screen - Quick Start 🚀

## ⚡ Immediate Steps

### 1. Place Your Images (5 minutes)

Copy your image files to the project:

```bash
# From your project root directory:
cp studychamp.png app/src/main/res/drawable/studychamp.png
cp splashscreen.png app/src/main/res/drawable/splashscreen.png
```

**Or manually**:

1. Open `app/src/main/res/drawable/` folder
2. Paste `studychamp.png` there
3. Paste `splashscreen.png` there
4. Rename to lowercase if needed (no spaces!)

---

### 2. Sync Gradle (2 minutes)

1. Open Android Studio
2. Click **File** → **Sync Project with Gradle Files**
3. Wait for completion ✅

The splash screen library (`androidx.core:core-splashscreen:1.0.1`) has already been added to
`build.gradle.kts`.

---

### 3. Uncomment Splash Screen Code (1 minute)

Open `app/src/main/java/com/runanywhere/startup_hackathon20/MainActivity.kt`:

Find lines 7-8 and **uncomment**:

```kotlin
// TODO: After Gradle sync, uncomment this line:
// import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
```

**Change to**:

```kotlin
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
```

Find lines 38-39 and **uncomment**:

```kotlin
// TODO: After Gradle sync and placing images, uncomment this line:
// val splashScreen = installSplashScreen()
```

**Change to**:

```kotlin
val splashScreen = installSplashScreen()
```

---

### 4. Update AndroidManifest (2 minutes)

Open `app/src/main/AndroidManifest.xml`:

Change these lines (around line 16-18):

```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```

**To**:

```xml
android:icon="@drawable/studychamp"
android:roundIcon="@drawable/studychamp"
```

And update MainActivity theme (around line 22):

```xml
android:theme="@style/Theme.Startup_hackathon20"
```

**To**:

```xml
android:theme="@style/Theme.StudyChamp.Splash"
```

---

### 5. Build & Run (2 minutes)

1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**
3. **Run** → **Run 'app'**

---

## ✅ Done!

Your app now has:

- ✅ StudyChamp logo as app icon
- ✅ Beautiful splash screen on launch
- ✅ All gamification features working
- ✅ AI quiz & flashcard generation
- ✅ 3 mentor personalities
- ✅ XP system with 10 levels
- ✅ 10 unlockable achievements

---

## 🎬 Expected Behavior

1. **Tap app icon** → See StudyChamp logo
2. **App launches** → Beautiful splash screen (2 seconds)
3. **Automatic transition** → Mentor selection screen
4. **Select mentor** → Home screen with profile HUD

---

## 🆘 Quick Troubleshooting

**Splash screen not showing?**

- Make sure images are in `drawable/` folder
- Check file names are lowercase
- Sync Gradle again

**App icon not changing?**

- Uninstall app from device
- Clean project
- Reinstall

**Build errors?**

- File → Invalidate Caches / Restart
- Clean + Rebuild

---

## 📖 Full Documentation

For detailed instructions and customization options, see:

- `SPLASH_SCREEN_INTEGRATION_GUIDE.md`
- `GAMIFICATION_FEATURES.md`
- `IMPLEMENTATION_SUMMARY.md`

---

**Total Time: ~12 minutes** ⏱️

Enjoy your fully enhanced StudyChamp app! 🎓✨
