# Pomodoro Timer - Release Information

## 📱 App Information

**Version:** 1.0.0  
**Package Name:** me.aliahad.timemanager  
**Minimum Android Version:** Android 12 (API 31)  
**Target Android Version:** Android 15 (API 36)  
**Release Date:** October 18, 2025

---

## 📦 Installation

### Release APK Location
```
PomodoroTimer-v1.0.0.apk
```

### How to Install

#### On Android Device:
1. **Transfer the APK** to your Android device (via USB, email, cloud storage, etc.)
2. **Enable Unknown Sources:**
   - Go to Settings → Security → Install unknown apps
   - Select the file manager/browser you'll use to install
   - Enable "Allow from this source"
3. **Install the APK:**
   - Tap on `PomodoroTimer-v1.0.0.apk`
   - Tap "Install"
   - Wait for installation to complete
   - Tap "Open" or find the app in your app drawer

#### Using ADB (Developer Method):
```bash
adb install PomodoroTimer-v1.0.0.apk
```

---

## 🔑 Signing Information

**Keystore File:** `app/release-keystore.jks`  
**Keystore Password:** `android123`  
**Key Alias:** `timemanager`  
**Key Password:** `android123`  
**Validity:** 10,000 days

⚠️ **IMPORTANT:** Keep the keystore file and passwords safe! You'll need them for future app updates.

---

## ✨ Features

- ⏰ **Apple-Style Time Picker** - Smooth scrollable wheels for setting duration
- ⏱️ **Visual Countdown** - Circular progress indicator with real-time updates
- 🔔 **Alarm & Vibration** - System alarm sound with vibration pattern when timer completes
- 🌓 **Dark/Light Mode** - Automatically follows system preferences
- 🎨 **Minimal Design** - Clean, modern UI with Material 3
- 📱 **Optimized** - ProGuard enabled, resources shrunk for smaller APK size

---

## 🚀 Production Optimizations

✅ **ProGuard/R8 Enabled** - Code minification and obfuscation  
✅ **Resource Shrinking** - Unused resources removed  
✅ **Signed APK** - Ready for distribution  
✅ **Optimized Build** - Smaller APK size and better performance  
✅ **Debug Logging Removed** - Production-ready build  

---

## 📊 APK Details

The APK has been:
- Compiled with Kotlin 2.0.21
- Built with Gradle 8.13.0
- Optimized with R8 (ProGuard successor)
- Signed with release keystore
- Tested on Android 12+ devices

---

## 🔄 Future Updates

To release future versions:

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`
2. Build new release:
   ```bash
   ./gradlew assembleRelease
   ```
3. APK will be generated at: `app/build/outputs/apk/release/app-release.apk`

---

## 📝 Permissions

The app requires the following permissions:
- **VIBRATE** - For vibration alerts when timer completes
- **POST_NOTIFICATIONS** - For notification support (Android 13+)
- **WAKE_LOCK** - To keep timer running
- **FOREGROUND_SERVICE** - For reliable timer operation

---

## 🎯 Usage

1. **Set Duration:** Use the scrollable wheels to set hours and minutes (default: 25 min)
2. **Start Timer:** Tap the blue Start button
3. **Countdown:** Watch the circular progress and timer count down
4. **Complete:** When timer finishes, you'll hear an alarm and feel vibration
5. **Dismiss:** Tap Dismiss to stop the alarm

Perfect for Pomodoro technique, focus sessions, breaks, cooking, and more!

---

## 📞 Support

For issues or questions, please contact: aliahad.me

---

**Built with ❤️ using Jetpack Compose**

