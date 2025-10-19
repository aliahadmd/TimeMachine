# 🎉 TimeMachine v2.0.0 - Major Release

> **Major update with complete Focus Tracker rewrite, new features, and critical bug fixes**

---

## 📱 Download

**APK File:** [TimeMachineV2.0.0.apk](../../releases/download/v2.0.0/TimeMachineV2.0.0.apk) (23 MB)

**MD5 Checksum:** `624619775c92b2466774015748e20a82`

**Minimum Android Version:** Android 12 (API 31) or higher

---

## ✨ What's New in v2.0.0

### 🔥 Focus Tracker - Complete Rewrite

The Focus Timer has been completely redesigned as the **Focus Tracker** with powerful new capabilities:

- **📂 Category-Based Tracking**: Create custom categories for different activities (Work, Study, Exercise, etc.)
- **🖼️ Immersive Fullscreen Mode**: 
  - Landscape orientation locked for desk placement
  - System UI completely hidden for distraction-free focus
  - Double-tap gesture to show/hide controls
- **⏱️ Real-Time Visual Feedback**:
  - Orange countdown: "Keep going... 15s to save" (first 29 seconds)
  - Green confirmation: "✓ Session will be saved" (after 30 seconds)
  - Dynamic daily goal progress bar
- **💾 Universal Session Saving**: All exit methods now save sessions (STOP button, EXIT button, back gesture, system back)
- **📊 Advanced Analytics**:
  - Category-based statistics
  - Calendar view with goal completion indicators (green = met, red = not met)
  - Session history with timestamps
  - Daily, weekly, and total tracking
- **🎯 Customizable Daily Goals**: Set and track different goals for each category

### 📅 Year Calculator

Calculate dates from years ago or in the future with robust date handling:

- Calculate any date based on years from now or from a specific date
- Full leap year support (no more crashes on Feb 29!)
- Save and view calculation history
- Perfect for age calculations, anniversaries, and milestones

### ⚖️ BMI Calculator

Comprehensive BMI tracking with health insights:

- Multiple input options: Age, Height (ft/cm), Weight (kg/lb)
- Gender selection (Male/Female) for accurate ranges
- Two classification systems: WHO (International) and DGE (German)
- Visual classification graph (Underweight, Normal, Overweight categories)
- Ideal weight range table based on your height
- Personalized health tips and interpretation
- Save and track BMI history over time

### 📊 Habit Tracker Improvements

Enhanced habit tracking features:

- Fixed timezone issues affecting streak calculations
- Improved back gesture handling in detail view
- Better midnight rollover detection
- More accurate completion rate tracking

---

## 🐛 Critical Bug Fixes

### Focus Tracker Fixes

1. **✅ Universal Session Saving**: Fixed issue where sessions weren't saving when exiting via back gesture or EXIT button. Now ALL exit methods save sessions properly.

2. **✅ UI Refresh on Return**: Implemented lifecycle observers to immediately refresh data when returning from immersive mode. No more stale data!

3. **✅ Progress Bar Visibility**: Daily goal progress bar now always visible in fullscreen mode and updates in real-time.

4. **✅ 30-Second Minimum Communication**: Added clear visual feedback so users know sessions need 30 seconds to save. Prevents accidental short taps from cluttering data.

### Date Calculator Fixes

5. **✅ Leap Year Crash**: Fixed `DateTimeException` crash when calculating with Feb 29 in non-leap years. The calculator now automatically finds the next valid Feb 29.

### Habit Tracker Fixes

6. **✅ Timezone Bug**: Fixed UTC epoch math that was causing wrong habit creation dates and skewed streak calculations. Now uses device timezone consistently.

7. **✅ Midnight Rollover**: Fixed cached date issue causing habit submissions to go to the wrong date after midnight. Date now updates automatically.

8. **✅ Back Gesture**: Fixed back gesture in habit detail view - now closes the overlay instead of the entire screen.

### UI/UX Fixes

9. **✅ Notification Banner**: Added dismiss button to notification banner and fixed channel checks. Removed references to deleted TimerAlarm channel.

10. **✅ Real-Time Feedback**: Added clear countdown and confirmation indicators throughout the app for better user experience.

---

## 🎨 UI/UX Improvements

### Visual Design

- **Zero Elevation Design**: Removed shadows from timer blocks for a modern, flat look
- **Vibrant Color Palette**: Updated all accent colors for better visual appeal
- **Gradient Icon Backgrounds**: Icon backgrounds now use smooth vertical gradients
- **Subtle Borders**: Added 1dp borders to cards for better definition without shadows
- **Improved Typography**: Better text hierarchy and readability throughout

### User Experience

- **Clear Communication**: Features and requirements clearly explained before use
- **Live Feedback**: Real-time countdown and confirmation messages
- **Smooth Animations**: Fade-in/fade-out transitions for controls
- **Intuitive Gestures**: Single tap for hints, double-tap for controls in immersive mode
- **Responsive UI**: All screens refresh automatically when resumed

---

## 🗄️ Technical Improvements

### Architecture

- **Room Database v6**: Robust local persistence with automatic migrations
- **Coroutines & Flow**: Reactive UI updates with proper lifecycle management
- **Lifecycle Observers**: Automatic data refresh when screens resume
- **SharedPreferences**: State persistence for immersive timer across lifecycle events
- **Null Safety**: Comprehensive null checks to prevent crashes

### Performance

- **ProGuard + R8**: Code minification and obfuscation enabled (reduces APK size)
- **Resource Shrinking**: Unused resources automatically removed
- **Optimized Database Queries**: Improved query performance for statistics
- **Efficient Recomposition**: LaunchedEffect keys properly managed for optimal recomposition

### Code Quality

- **Race Condition Fixes**: Proper coroutine scoping and state capture
- **Error Handling**: Try-catch blocks with detailed logging for debugging
- **Consolidated Utilities**: Removed duplicate code and organized utilities
- **Clean Architecture**: Separated concerns between UI, data, and business logic

---

## 📦 Installation

### Method 1: Direct Installation (Recommended)

1. Download `TimeMachineV2.0.0.apk` from the release assets
2. Enable "Unknown Sources" in your device settings
3. Open the APK file and tap Install
4. Launch TimeMachine and enjoy!

### Method 2: ADB Installation (For Developers)

```bash
# Download the APK, then:
adb install TimeMachineV2.0.0.apk

# Launch the app
adb shell am start -n me.aliahad.timemanager/.MainActivity
```

**📄 Full installation guide:** See [INSTALL_GUIDE.md](./INSTALL_GUIDE.md)

---

## 🔄 Upgrading from v1.x

**Good news!** You can install v2.0.0 directly over v1.x without losing data:

- Database automatically migrates to version 6
- All existing habits, BMI calculations, and year calculations are preserved
- Focus Timer data (if any) is maintained
- Settings and preferences carry over

**Note:** The old Focus Timer has been replaced with the new Focus Tracker. Any old timer data will be preserved but you'll need to create categories for the new system.

---

## 📋 Version History

- **v2.0.0** (October 20, 2025): Major update - Focus Tracker rewrite, new features, 10 bug fixes
- **v1.3.0**: BMI Calculator and Year Calculator added
- **v1.2.0**: Habit Tracker improvements
- **v1.1.0**: Initial Habit Tracker
- **v1.0.0**: Initial release with basic timer

---

## 🎯 What's Coming Next

### Planned for v2.1.0

- 💾 **Export/Import Data**: Backup and restore all your data
- ☁️ **Cloud Sync**: Optional cloud backup (Google Drive integration)
- 🎨 **More Themes**: Light/Dark theme with accent color customization
- 📱 **Home Screen Widget**: Quick access to timer and stats

### Future Roadmap

- 🌍 **Multi-language Support**: Localization for major languages
- 📈 **Advanced Charts**: Beautiful visualizations for analytics
- 🍅 **Pomodoro Integration**: Structured focus sessions with breaks
- 🤝 **Social Features**: Optional sharing and accountability (privacy-focused)

---

## 🙏 Credits & Support

**Developer:** Ali Ahad  
**Repository:** [github.com/aliahadmd/TimeMachine](https://github.com/aliahadmd/TimeMachine)  
**License:** MIT (or your license)

### Support & Feedback

- 🐛 **Bug Reports**: Open an issue on GitHub
- 💡 **Feature Requests**: Submit via GitHub Issues
- ⭐ **Star the Repository**: If you find TimeMachine useful!
- 📢 **Share**: Tell your friends about TimeMachine

---

## 🔐 Security & Privacy

- ✅ **No Network Permissions**: App works completely offline
- ✅ **No Data Collection**: All data stays on your device
- ✅ **Signed APK**: Release signed with production keystore
- ✅ **ProGuard Obfuscation**: Code protected against reverse engineering
- ✅ **Open Source**: Code available for inspection (if applicable)

---

## 📊 Release Statistics

- **Version:** 2.0.0 (Build 5)
- **APK Size:** 23 MB (optimized with R8)
- **Build Time:** 1 minute 20 seconds
- **Files Changed:** 15+ files
- **Lines Added:** 2000+
- **Bug Fixes:** 10 critical fixes
- **New Features:** 3 major features
- **Database Version:** 6 (auto-migrates)

---

## 🎉 Thank You!

Thank you for using TimeMachine! This v2.0.0 release represents months of development, testing, and refinement. We hope these new features help you be more productive and healthy.

**Enjoy TimeMachine v2.0.0!** ⏰✨

---

## 📸 Screenshots

*(Add screenshots here when creating the GitHub release)*

1. Focus Tracker - Category Selection
2. Immersive Fullscreen Timer
3. Statistics & Calendar View
4. Year Calculator
5. BMI Calculator with Results
6. Habit Tracker

---

## 🔗 Quick Links

- 📥 [Download APK](../../releases/download/v2.0.0/TimeMachineV2.0.0.apk)
- 📄 [Full Release Notes](./RELEASE_NOTES_V2.0.0.md)
- 📖 [Installation Guide](./INSTALL_GUIDE.md)
- 🐛 [Report Issues](../../issues)
- 💬 [Discussions](../../discussions)

---

**Happy Time Managing!** 🚀

