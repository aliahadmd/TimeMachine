# ⏱️ Pomodoro Timer

<div align="center">

![Pomodoro Timer](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Android](https://img.shields.io/badge/platform-Android-green.svg)
![Min API](https://img.shields.io/badge/API-31%2B-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)

**A beautiful, minimal timer app for Android built with Jetpack Compose**

Perfect for Pomodoro technique, focus sessions, breaks, meditation, cooking, and more!

[Download APK](https://github.com/aliahadmd/TimeManager/releases/latest) • [Report Bug](https://github.com/aliahadmd/TimeManager/issues) • [Request Feature](https://github.com/aliahadmd/TimeManager/issues)

</div>

---

## 📱 Screenshots

> Add screenshots here when available

## ✨ Features

### 🎯 Core Functionality
- **⏰ Custom Duration** - Set any duration from 1 minute to 23 hours 59 minutes
- **⏱️ Real-Time Countdown** - Watch the timer count down with visual progress indicator
- **🔔 Alarm Sound** - System alarm plays when timer completes
- **📳 Vibration** - Physical feedback with repeating vibration pattern
- **🛑 Stop Anytime** - Cancel the timer whenever needed

### 🎨 User Experience
- **Apple-Style Picker** - Smooth, scrollable wheels for time selection (just like iOS)
- **Circular Progress** - Beautiful circular indicator shows remaining time
- **Minimal Design** - Clean, distraction-free interface
- **Smooth Animations** - Polished transitions between states
- **Large Touch Targets** - Easy to use with one hand

### 🌓 Appearance
- **Auto Dark/Light Mode** - Follows your system preferences
- **Material 3 Design** - Modern Android design language
- **Clean Colors** - Blue accent with high contrast
- **Edge-to-Edge** - Immersive full-screen experience

### 🔧 Technical
- **Lightweight** - Only 11MB APK size
- **No Ads** - Completely ad-free experience
- **No Internet Required** - Works 100% offline
- **Battery Efficient** - Optimized background timer
- **ProGuard Optimized** - Minified and shrunk for performance

---

## 📥 Installation

### Download APK
1. Go to [Releases](https://github.com/aliahadmd/TimeManager/releases/latest)
2. Download `PomodoroTimer-v1.0.0.apk`
3. Enable "Install from Unknown Sources" in your Android settings
4. Open the APK and install

### Minimum Requirements
- **Android 12 (API 31) or higher**
- **ARM or x86 processor**
- **~15MB free storage**

---

## 🚀 Usage

### Setting a Timer

1. **Open the app** - Launch Pomodoro Timer
2. **Set duration** - Scroll the time wheels to select hours and minutes
   - Default is 25 minutes (perfect for Pomodoro technique)
3. **Tap Start** - Big blue button starts the countdown
4. **Watch progress** - Circular indicator shows time remaining
5. **Get notified** - Alarm sound + vibration when complete
6. **Dismiss** - Tap to stop the alarm

### Pomodoro Technique

The classic Pomodoro technique:
1. **Work**: Set timer for 25 minutes and focus
2. **Short break**: Set timer for 5 minutes and rest
3. **Repeat**: After 4 pomodoros, take a longer 15-30 minute break

### Other Use Cases
- 🏋️ **Workout intervals** - HIIT, rest periods, exercise durations
- 🧘 **Meditation** - Timed meditation sessions
- 👨‍🍳 **Cooking** - Track cooking times
- ☕ **Coffee brewing** - Perfect pour-over timing
- 📚 **Study sessions** - Focused learning blocks
- 🎮 **Screen time limits** - Gaming or social media breaks
- 🧺 **Chores** - Time-boxed cleaning sessions

---

## 🛠️ Build from Source

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 11** or higher
- **Android SDK** with API 36
- **Git**

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/aliahadmd/TimeManager.git
cd TimeManager

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires keystore)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

### Project Structure
```
TimeManager/
├── app/
│   ├── src/main/
│   │   ├── java/me/aliahad/timemanager/
│   │   │   ├── MainActivity.kt          # App entry point
│   │   │   ├── TimerScreen.kt           # Main timer UI
│   │   │   └── ui/theme/                # Theme & colors
│   │   ├── res/                         # Resources
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts                 # App config
│   └── proguard-rules.pro               # ProGuard rules
├── gradle/
├── build.gradle.kts                     # Project config
└── README.md
```

---

## 🏗️ Tech Stack

### Core
- **Language**: [Kotlin](https://kotlinlang.org/) 2.0.21
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Material**: [Material 3](https://m3.material.io/)
- **Build System**: Gradle 8.13.0

### Architecture
- **Compose Runtime** - State management
- **Kotlin Coroutines** - Asynchronous operations
- **LazyColumn** - Scrollable time picker
- **AnimatedContent** - Smooth transitions

### Android Components
- **MediaPlayer** - Alarm sound playback
- **Vibrator** - Haptic feedback
- **System Theme** - Auto dark/light mode

### Optimization
- **ProGuard/R8** - Code minification
- **Resource Shrinking** - Unused resource removal
- **Vector Drawables** - Scalable icons

---

## 📦 Publishing Releases on GitHub

### Automated Release Process

1. **Update Version** in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2
   versionName = "1.0.1"
   ```

2. **Build Release APK**:
   ```bash
   ./gradlew assembleRelease
   cp app/build/outputs/apk/release/app-release.apk PomodoroTimer-v1.0.1.apk
   ```

3. **Commit Changes**:
   ```bash
   git add .
   git commit -m "Release v1.0.1"
   git tag -a v1.0.1 -m "Version 1.0.1"
   git push origin main --tags
   ```

4. **Create GitHub Release**:
   - Go to your repo on GitHub
   - Click "Releases" → "Draft a new release"
   - Choose tag: `v1.0.1`
   - Release title: `v1.0.1 - [Brief description]`
   - Add release notes (see template below)
   - Attach `PomodoroTimer-v1.0.1.apk`
   - Click "Publish release"

### Release Notes Template
```markdown
## What's New in v1.0.1

### 🎉 New Features
- Feature description here

### 🐛 Bug Fixes
- Bug fix description here

### 🔧 Improvements
- Improvement description here

### 📥 Installation
Download `PomodoroTimer-v1.0.1.apk` below and install on your Android device.

**Minimum Android Version**: Android 12 (API 31)

**Full Changelog**: https://github.com/aliahadmd/TimeManager/compare/v1.0.0...v1.0.1
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

## 🐛 Known Issues

None at the moment! 🎉

If you find any bugs, please [open an issue](https://github.com/aliahadmd/TimeManager/issues).

---

## 💡 Future Enhancements

Ideas for future versions:
- [ ] Save favorite timer presets
- [ ] Multiple timer profiles
- [ ] Statistics and usage tracking
- [ ] Custom alarm sounds
- [ ] Widget support
- [ ] Background timer notifications
- [ ] Pause/resume functionality
- [ ] Custom color themes

---

## 📞 Contact & Support

- **Developer**: Ali Ahad
- **Email**: your.email@example.com
- **Website**: [aliahad.me](https://aliahad.me)

---

## 🙏 Acknowledgments

- Jetpack Compose team for the amazing framework
- Material Design for the design guidelines
- Pomodoro Technique by Francesco Cirillo

---

<div align="center">

**Built with ❤️ using Jetpack Compose**

⭐ Star this repo if you find it helpful!

</div>

