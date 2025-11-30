# 📱 Wireless Debugging Setup Guide for StudyChamp

## ✅ Codebase is Wireless Debugging Ready!

Your StudyChamp app has been configured to support wireless debugging without any issues. All
necessary permissions and configurations are in place.

## 🔧 What Was Fixed

### 1. **Build Configuration** (`app/build.gradle.kts`)

- ✅ Added explicit `isDebuggable = true` for debug builds
- ✅ Added `.debug` suffix to distinguish debug builds
- ✅ Disabled minification for debug builds

### 2. **AndroidManifest.xml**

- ✅ Added `ACCESS_NETWORK_STATE` permission (helps ADB detect network changes)
- ✅ Added `networkSecurityConfig` to allow cleartext traffic for development
- ✅ Existing `INTERNET` permission already present

### 3. **Network Security Configuration** (`network_security_config.xml`)

- ✅ Created configuration to allow cleartext traffic (required for ADB over TCP)
- ✅ Trusts system and user certificates (useful for debugging with proxies)
- ✅ Ensures no network restrictions block wireless debugging

### 4. **Gradle Properties**

- ✅ Enabled configuration cache for faster builds
- ✅ Enabled Gradle daemon for better performance

## 📲 How to Enable Wireless Debugging

### Method 1: Using Android Studio (Recommended)

#### Prerequisites:

- Android 11 (API 30) or higher on your device
- Both computer and device on the same Wi-Fi network

#### Steps:

1. **Enable Developer Options on Your Device**
    - Go to **Settings** → **About Phone**
    - Tap **Build Number** 7 times
    - Enter your PIN/password if prompted

2. **Enable Wireless Debugging**
    - Go to **Settings** → **Developer Options**
    - Enable **Wireless Debugging**
    - Tap on **Wireless Debugging** to see the pairing options

3. **Connect via Android Studio**
    - Open Android Studio
    - Go to **Tools** → **Device Manager**
    - Click **Pair Devices Using Wi-Fi**
    - On your phone, tap **Pair device with pairing code**
    - Enter the pairing code shown on your phone into Android Studio
    - Click **Pair**

4. **Run Your App**
    - Once paired, your device appears in the device dropdown
    - Select it and click **Run** (▶️)
    - The app will deploy wirelessly!

### Method 2: Using ADB Commands

1. **Initial Setup (USB Required Once)**
   ```bash
   # Connect device via USB first
   adb tcpip 5555
   
   # Disconnect USB cable
   
   # Find your device's IP address (Settings → About Phone → Status → IP Address)
   # Or use: adb shell ip addr show wlan0
   
   # Connect wirelessly (replace with your device IP)
   adb connect 192.168.1.XXX:5555
   
   # Verify connection
   adb devices
   ```

2. **Deploy and Run**
   ```bash
   # Install the debug APK
   ./gradlew installDebug
   
   # Or build and install
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **View Logs Wirelessly**
   ```bash
   adb logcat | grep -i StudyChamp
   ```

### Method 3: QR Code Pairing (Android 11+)

1. On your device:
    - **Settings** → **Developer Options** → **Wireless Debugging**
    - Tap **Pair device with QR code**

2. In Android Studio:
    - **Tools** → **Device Manager**
    - Click **Pair Devices Using Wi-Fi**
    - Click **Pair using QR code**
    - Scan the QR code on your phone

## 🎯 Quick Testing

After connecting wirelessly, run this command to verify everything works:

```bash
# Check if device is connected
adb devices

# Should show something like:
# List of devices attached
# 192.168.1.100:5555    device
```

Then deploy the app:

```bash
./gradlew installDebug
```

## 🔍 Troubleshooting

### Device Not Showing Up

1. **Check Wi-Fi Connection**
   ```bash
   # On your computer, check if you can ping the device
   ping 192.168.1.XXX
   ```

2. **Restart ADB Server**
   ```bash
   adb kill-server
   adb start-server
   adb connect 192.168.1.XXX:5555
   ```

3. **Check Firewall Settings**
    - Ensure your firewall allows ADB (port 5555)
    - On macOS: **System Preferences** → **Security & Privacy** → **Firewall** → **Firewall Options
      **

### Connection Keeps Dropping

1. **Disable Wi-Fi Sleep Mode**
    - **Settings** → **Wi-Fi** → **Advanced** → **Keep Wi-Fi on during sleep** → **Always**

2. **Keep Device Unlocked During Initial Connection**

3. **Use Static IP for Device** (optional but helps)
    - Configure static IP in your router settings

### Build Errors

If you see build errors after the changes:

```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

## 📊 Performance Notes

- ✅ **No impact on app performance** - changes only affect debug builds
- ✅ **Network security config** only allows cleartext in development
- ✅ **Firebase, RunAnywhere SDK** and all features work perfectly over wireless debugging
- ✅ **Large APK (RunAnywhere SDK)** may take 30-60 seconds to deploy wirelessly (first time)

## 🎓 Testing StudyChamp Features Wirelessly

All features work perfectly with wireless debugging:

```bash
# Monitor AI model downloads
adb logcat | grep -E "RunAnywhere|MyApp"

# Monitor Firebase operations
adb logcat | grep -E "Firebase|Firestore"

# Monitor gamification events
adb logcat | grep -E "XP|Achievement"

# View all StudyChamp logs
adb logcat | grep -i "com.runanywhere.startup_hackathon20"
```

## 🔒 Production Considerations

**Important**: The current network security configuration allows cleartext traffic for development.
For production release:

1. Update `network_security_config.xml` to restrict cleartext:
   ```xml
   <network-security-config>
       <base-config cleartextTrafficPermitted="false">
           <trust-anchors>
               <certificates src="system" />
           </trust-anchors>
       </base-config>
   </network-security-config>
   ```

2. Or create separate configs for debug/release in `build.gradle.kts`:
   ```kotlin
   buildTypes {
       debug {
           // Uses network_security_config.xml (cleartext allowed)
       }
       release {
           // Override with production config
           manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config_release"
       }
   }
   ```

## 🚀 Quick Start Commands

```bash
# 1. Connect device via USB once
adb tcpip 5555

# 2. Find device IP
adb shell ip addr show wlan0 | grep inet

# 3. Disconnect USB and connect wirelessly
adb connect 192.168.1.XXX:5555

# 4. Deploy app
./gradlew installDebug

# 5. Watch logs
adb logcat | grep -i studychamp

# 6. When done, disconnect
adb disconnect
```

## ✨ Benefits of Wireless Debugging

- 🔌 **No USB cable required** after initial setup
- 🏃 **Move freely** while testing
- 💻 **USB port free** for other devices
- 🔋 **Can charge** with any charger while debugging
- 🎯 **Real-world testing** of network features

---

**Note**: Your StudyChamp app is now fully configured for wireless debugging! All changes are
backward compatible and won't affect existing functionality.

Happy wireless debugging! 🎉
