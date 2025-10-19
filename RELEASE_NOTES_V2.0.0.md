# TimeMachine v2.0.0 - Release Notes

**Release Date:** October 20, 2025  
**APK File:** `TimeMachineV2.0.0.apk`  
**File Size:** 23 MB  
**MD5 Checksum:** `624619775c92b2466774015748e20a82`

---

## 🎉 Major Version Release - v2.0.0

This is a major update with significant architectural improvements, new features, and comprehensive bug fixes.

---

## ✨ New Features

### 1. **Focus Tracker - Complete Rewrite** 🎯
- **Category-Based Time Tracking**: Create custom categories to track different activities
- **Immersive Fullscreen Mode**: 
  - Landscape orientation lock
  - System UI hidden for distraction-free focus
  - Double-tap to show/hide controls
- **Real-Time Session Feedback**:
  - Orange countdown: "Keep going... 15s to save"
  - Green confirmation: "✓ Session will be saved"
  - 30-second minimum requirement clearly communicated
- **Universal Session Saving**: All exit methods (STOP, EXIT, back gesture) save sessions
- **Advanced Statistics**:
  - Category-based analytics
  - Daily goal tracking with visual progress bars
  - Calendar view with goal completion indicators (green/red)
  - Session history with timestamps
- **Daily Goal Management**: Set and track daily goals per category

### 2. **Year Calculator** 📅
- Calculate dates from years ago or in the future
- Support for complex date scenarios including leap years
- Save and view calculation history
- Robust Feb 29 handling (no crashes!)

### 3. **BMI Calculator** ⚖️
- Age, Height (ft/cm), Weight (kg/lb) inputs
- Gender selection (Male/Female)
- Multiple classification systems (WHO/DGE)
- Visual classification graph
- Ideal weight range table
- Health tips and personalized interpretation
- Save and view BMI history

### 4. **Habit Tracker** 📊
- Track daily habits with streaks
- Visual completion calendar
- Detailed statistics and trends
- Reminder notifications

---

## 🐛 Critical Bug Fixes

### Focus Tracker Fixes
1. ✅ **Session Save Bug**: Fixed issue where sessions weren't saving when exiting via back gesture or EXIT button
2. ✅ **Midnight Rollover**: Fixed cached date issue causing submissions to wrong date after midnight
3. ✅ **UI Refresh**: Implemented lifecycle observers for immediate data refresh when returning from immersive mode
4. ✅ **Progress Bar Visibility**: Daily goal progress bar now always visible in fullscreen mode

### Date Calculator Fixes
5. ✅ **Leap Year Crash**: Fixed `DateTimeException` when calculating with Feb 29 in non-leap years
6. ✅ **Next Valid Date**: Automatically finds next valid Feb 29 when needed

### Habit Tracker Fixes
7. ✅ **Timezone Bug**: Fixed UTC epoch math causing wrong habit creation dates and skewed streaks
8. ✅ **Back Gesture**: Fixed back gesture in habit detail view (now closes overlay instead of entire screen)

### UI/UX Fixes
9. ✅ **Notification Banner**: Added dismiss button and fixed channel checks (removed old TimerAlarm channel)
10. ✅ **Session Indicators**: Added clear visual feedback for minimum session length requirement

---

## 🎨 UI/UX Improvements

### Design Changes
- **Cleaner Home Screen**: Removed shadows from timer blocks, added subtle borders
- **Vibrant Colors**: Updated color palette for better visual appeal
- **Modern Gradient Icons**: Icon backgrounds now use vertical gradients
- **Zero Elevation**: Card elevation set to 0dp for flat, modern look
- **Consistent Typography**: Improved text hierarchy and readability

### User Experience
- **Clear Feedback**: Users now see live countdown and confirmation when session will save
- **Feature Communication**: "Auto-saves after 30 seconds" shown before starting sessions
- **Smooth Animations**: Added fade-in/fade-out transitions for controls
- **Tap Gestures**: Single tap shows hint, double-tap toggles controls in immersive mode

---

## 🗄️ Database Changes

### Version 6 Schema
- **ActivityCategory** table: Store custom activity categories
- **TimeSession** table: Store time tracking sessions with category association
- **Improved Queries**: Optimized queries for statistics and analytics
- **Data Integrity**: Proper timezone handling and date storage

---

## 🏗️ Technical Improvements

### Architecture
- **Room Database**: Robust local persistence with migrations
- **Coroutines & Flow**: Reactive UI updates with proper lifecycle management
- **Lifecycle Observers**: Automatic data refresh on screen resume
- **SharedPreferences**: State persistence for immersive timer across lifecycle events

### Performance
- **ProGuard + R8**: Code minification and obfuscation enabled
- **Resource Shrinking**: Unused resources removed automatically
- **Optimized Queries**: Database queries optimized for performance
- **Efficient Recomposition**: LaunchedEffect keys properly managed

### Code Quality
- **Null Safety**: Comprehensive null checks to prevent crashes
- **Error Handling**: Try-catch blocks with proper logging
- **Race Condition Fixes**: Proper coroutine scoping and state capture
- **Consolidated Utilities**: Removed duplicate code, organized utilities

---

## 📋 Version History

- **v2.0.0** (October 20, 2025): Major update - Focus Tracker rewrite, bug fixes, new features
- **v1.3.0**: Previous stable version
- **v1.2.0**: Feature additions
- **v1.1.0**: Initial habit tracker
- **v1.0.0**: Initial release

---

## 🔐 Security

- **Signed APK**: Release signed with production keystore
- **ProGuard**: Code obfuscation enabled
- **No Permissions**: Minimal permission requirements
- **Local Storage**: All data stored locally on device

---

## 📦 Installation

1. **Download**: Get `TimeMachineV2.0.0.apk` from the release folder
2. **Enable Unknown Sources**: Settings → Security → Unknown Sources
3. **Install**: Tap the APK file to install
4. **Launch**: Open TimeMachine and enjoy!

---

## 🎯 Testing Checklist

### Focus Tracker
- [x] Create categories
- [x] Start sessions
- [x] Verify 30s minimum with visual feedback
- [x] Test all exit methods (STOP, EXIT, back gesture)
- [x] Check session history
- [x] Verify statistics
- [x] Test calendar goal view

### Date Calculator
- [x] Test Feb 29 scenarios
- [x] Verify calculation accuracy
- [x] Check history saving

### BMI Calculator
- [x] Test all input fields
- [x] Verify WHO/DGE classifications
- [x] Check unit conversions
- [x] Test history saving

### Habit Tracker
- [x] Create habits
- [x] Track completions
- [x] Verify streaks
- [x] Test after midnight

---

## 🚀 Future Roadmap

### v2.1.0 (Planned)
- Export/import data functionality
- Cloud sync support
- More themes and customization
- Widget support

### v2.2.0 (Planned)
- Multi-language support
- Advanced analytics charts
- Pomodoro timer integration
- Social features (optional sharing)

---

## 🙏 Credits

**Developer**: Ali Ahad  
**Version**: 2.0.0  
**Build Date**: October 20, 2025  
**Package**: me.aliahad.timemanager  

---

## 📞 Support

For bug reports or feature requests, please contact the developer.

---

**Enjoy TimeMachine v2.0.0!** 🎉✨

