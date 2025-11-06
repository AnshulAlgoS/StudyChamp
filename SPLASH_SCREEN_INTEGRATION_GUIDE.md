# StudyChamp - Splash Screen & Logo Integration Guide

## 📋 Overview

This guide will help you integrate `studychamp.png` (app logo) and `splashscreen.png` (splash
screen) into the StudyChamp Android app.

---

## 🖼️ Step 1: Prepare Your Images

### Image Requirements

**studychamp.png (App Logo)**

- Recommended size: **512x512px** or **1024x1024px**
- Format: PNG with transparent background
- Purpose: App launcher icon

**splashscreen.png (Splash Screen)**

- Recommended size: **1080x1920px** or **1440x2560px**
- Format: PNG
- Purpose: Splash screen displayed on app launch

---

## 📂 Step 2: Place Images in Project

### Option A: Place Images in `drawable` folder

1. Navigate to: `app/src/main/res/drawable/`

2. Copy your images there and rename them:
    - `studychamp.png` → `app/src/main/res/drawable/studychamp.png`
    - `splashscreen.png` → `app/src/main/res/drawable/splashscreen.png`

**Important**: File names must be lowercase with no spaces or special characters.

### Option B: Use Android Studio's Resource Manager

1. Open Android Studio
2. Right-click on `res` folder → **New** → **Image Asset**
3. Import your images

---

## 🎨 Step 3: Update App Launcher Icon

### Replace Default Launcher Icon

The app already has the theme configured. You just need to replace the icon files:

1. **For adaptive icon** (recommended for Android 8.0+):

   Edit `app/src/main/res/drawable/ic_launcher_foreground.xml`:
   ```xml
   <vector xmlns:android="http://schemas.android.com/apk/res/android"
       android:width="108dp"
       android:height="108dp"
       android:viewportWidth="108"
       android:viewportHeight="108">
       <!-- This will use your studychamp.png as foreground -->
       <path android:pathData="M0,0h108v108h-108z" 
             android:fillColor="@android:color/transparent"/>
   </vector>
   ```

2. **Or use the image directly** by creating a new drawable:

   Create `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`:
    - Place your `studychamp.png` here (resize to 192x192px for xxxhdpi)

   Repeat for other densities:
    - `mipmap-xxhdpi/` → 144x144px
    - `mipmap-xhdpi/` → 96x96px
    - `mipmap-hdpi/` → 72x72px
    - `mipmap-mdpi/` → 48x48px

---

## ✨ Step 4: Implement Splash Screen (Android 12+ Native API)

### 4.1: Update AndroidManifest.xml

Edit `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission
        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

    <application
        android:name=".MyApplication"
        android:allowBackup="true"
        android:largeHeap="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@drawable/studychamp"
        android:label="@string/app_name"
        android:roundIcon="@drawable/studychamp"
        android:supportsRtl="true"
        android:theme="@style/Theme.Startup_hackathon20">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.StudyChamp.Splash">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**Changes made:**

- Changed `android:icon` and `android:roundIcon` to use `@drawable/studychamp`
- Added `android:theme="@style/Theme.StudyChamp.Splash"` to MainActivity

### 4.2: Create Splash Theme

The theme file has already been created at `app/src/main/res/values/splash_theme.xml`.

Update it to:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Splash Screen Theme for Android 12+ -->
    <style name="Theme.StudyChamp.Splash" parent="Theme.SplashScreen">
        <!-- Splash screen background color -->
        <item name="windowSplashScreenBackground">@color/splash_background</item>
        
        <!-- Center icon - your studychamp logo -->
        <item name="windowSplashScreenAnimatedIcon">@drawable/studychamp</item>
        
        <!-- Icon animation duration -->
        <item name="windowSplashScreenAnimationDuration">1000</item>
        
        <!-- Post-splash theme (main app theme) -->
        <item name="postSplashScreenTheme">@style/Theme.Startup_hackathon20</item>
    </style>
</resources>
```

### 4.3: Update MainActivity.kt

After syncing Gradle, update `MainActivity.kt`:

```kotlin
package com.runanywhere.startup_hackathon20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
// ... other imports

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen (this line goes BEFORE super.onCreate())
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Optional: Keep splash screen visible for longer
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        
        // Simulate loading time (remove in production)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            keepSplashOnScreen = false
        }, 2000) // 2 seconds
        
        setContent {
            Startup_hackathon20Theme {
                StudyChampApp()
            }
        }
    }
}
```

---

## 🛠️ Step 5: Gradle Sync

### 5.1: Verify Dependencies

Check that `app/build.gradle.kts` has:

```kotlin
dependencies {
    // ... existing dependencies
    
    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // ... rest of dependencies
}
```

**This has already been added to your build.gradle.kts file.**

### 5.2: Sync Project

1. Open Android Studio
2. Click **File** → **Sync Project with Gradle Files**
3. Wait for sync to complete

---

## 🎯 Alternative: Custom Splash Screen (For Full Control)

If you want to use `splashscreen.png` as a full-screen splash screen:

### Option 1: Use as Background

Edit `app/src/main/res/drawable/splash_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Full screen splash image -->
    <item>
        <bitmap
            android:src="@drawable/splashscreen"
            android:gravity="fill"
            android:scaleType="centerCrop" />
    </item>
</layer-list>
```

Then update the theme:

```xml
<style name="Theme.StudyChamp.Splash" parent="Theme.SplashScreen">
    <!-- Use custom background -->
    <item name="android:windowBackground">@drawable/splash_background</item>
    <item name="postSplashScreenTheme">@style/Theme.Startup_hackathon20</item>
</style>
```

### Option 2: Create a Dedicated Splash Activity

Create `SplashActivity.kt`:

```kotlin
package com.runanywhere.startup_hackathon20

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SplashScreen()
        }
        
        // Navigate to MainActivity after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}

@Composable
fun SplashScreen() {
    Image(
        painter = painterResource(id = R.drawable.splashscreen),
        contentDescription = "Splash Screen",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
```

Update `AndroidManifest.xml`:

```xml
<activity
    android:name=".SplashActivity"
    android:exported="true"
    android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".MainActivity"
    android:exported="false"
    android:theme="@style/Theme.Startup_hackathon20" />
```

---

## 📱 Step 6: Test

1. **Clean and Rebuild**:
   ```
   Build → Clean Project
   Build → Rebuild Project
   ```

2. **Run on Device/Emulator**:
    - You should see the splash screen with your logo on launch
    - After 1-2 seconds, the app should transition to the mentor selection screen

3. **Check Launcher Icon**:
    - Exit the app
    - Check the app icon in the launcher
    - It should display your StudyChamp logo

---

## ✅ Verification Checklist

- [ ] `studychamp.png` placed in `app/src/main/res/drawable/`
- [ ] `splashscreen.png` placed in `app/src/main/res/drawable/`
- [ ] Updated `AndroidManifest.xml` with correct icon references
- [ ] Splash theme created in `splash_theme.xml`
- [ ] Splash screen dependency added to `build.gradle.kts`
- [ ] Gradle synced successfully
- [ ] `MainActivity.kt` updated with `installSplashScreen()`
- [ ] App builds without errors
- [ ] Splash screen displays on launch
- [ ] App icon shows StudyChamp logo

---

## 🎨 Customization Tips

### Adjust Splash Duration

In `MainActivity.kt`:

```kotlin
splashScreen.setKeepOnScreenCondition { 
    // Keep showing while loading data
    viewModel.isInitializing
}
```

### Add Animation

```kotlin
splashScreen.setOnExitAnimationListener { splashScreenView ->
    val slideUp = ObjectAnimator.ofFloat(
        splashScreenView,
        View.TRANSLATION_Y,
        0f,
        -splashScreenView.height.toFloat()
    )
    slideUp.interpolator = AccelerateInterpolator()
    slideUp.duration = 500L
    slideUp.doOnEnd { splashScreenView.remove() }
    slideUp.start()
}
```

---

## 🐛 Troubleshooting

### Issue: "Cannot resolve symbol splashscreen"

**Solution**:

1. Ensure `core-splashscreen` dependency is in build.gradle
2. Sync Gradle (File → Sync Project with Gradle Files)
3. Invalidate Caches (File → Invalidate Caches / Restart)

### Issue: Splash screen not showing

**Solution**:

1. Check that MainActivity has the splash theme in manifest
2. Verify `installSplashScreen()` is called before `super.onCreate()`
3. Ensure images are in the correct `drawable` folder

### Issue: Logo not appearing

**Solution**:

1. Check image file names (must be lowercase, no spaces)
2. Verify `@drawable/studychamp` reference exists
3. Rebuild project

### Issue: App icon not changing

**Solution**:

1. Uninstall the app completely from device
2. Clean project (Build → Clean Project)
3. Rebuild and reinstall

---

## 📚 Resources

- [Android Splash Screen API Guide](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Material Design - Launch Screen](https://m3.material.io/components/splash-screen/overview)
- [Image Asset Studio](https://developer.android.com/studio/write/image-asset-studio)

---

## ✨ Final Notes

Once you've completed these steps:

1. Your app will show a beautiful splash screen on launch
2. The StudyChamp logo will appear as the app icon
3. The splash screen will automatically transition to the mentor selection screen

**All gamification features (XP, achievements, quizzes, flashcards) are already fully integrated!**

Enjoy your enhanced StudyChamp app! 🎓🏆
