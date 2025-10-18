# 🎉 TimeMachine v1.1.0 - Major Update

**Release Date**: October 18, 2025

A major update with preset functionality, UI improvements, and app rebranding!

---

## ✨ What's New

### 🔖 **Preset System**
- **Save Timer Presets** - Save your favorite durations with custom names
- **SQLite Database** - All presets stored locally and persist forever
- **Horizontal Scroll** - Beautiful card-based horizontal scrolling for presets
- **One-Tap Start** - Tap any preset to instantly start the timer
- **Quick Access** - No need to manually set time anymore!
- **Delete Presets** - Remove unwanted presets with X button

### 🎨 **UI/UX Improvements**
- **Fixed Picker Alignment** - Highlight box now perfectly aligns with selected number
- **Haptic Feedback** - Feel subtle vibrations when scrolling time picker
- **Better Button Text** - Fixed text cutoff issues on Start/Stop buttons
- **Improved Touch Targets** - Larger, easier to tap buttons
- **Cleaner Header** - Simplified top bar design

### 📱 **App Rebranding**
- **New Name**: TimeMachine ⏰ (formerly Pomodoro Timer)
- **Modern Identity** - Better reflects versatile timer functionality
- **Updated Icon** - Fresh new app icon

---

## 🆕 New Features in Detail

### **Preset Management**
Save commonly used timers for instant access:
- "Focus Session" - 25 minutes
- "Short Break" - 5 minutes  
- "Long Break" - 15 minutes
- "Deep Work" - 90 minutes
- Any custom duration you need!

### **Workflow Example**
```
1. Set 25 minutes → Tap save → Name it "Focus"
2. Next time: Just tap "Focus" chip → Timer starts immediately!
3. When done: Tap "Break" preset → 5 min countdown begins
```

### **Database Features**
- Automatic persistence across app restarts
- Unlimited presets (store as many as you need)
- Fast retrieval with Room database
- Efficient SQLite storage

---

## 🐛 Bug Fixes

### **Fixed Selection Highlight Misalignment**
- **Issue**: Highlight box showed one number above/below selected value
- **Fix**: Complete rewrite using `derivedStateOf` for accurate center calculation
- **Result**: Pixel-perfect alignment between highlight and selected number

### **Fixed Button Text Cutoff**
- **Issue**: "Start" showing as "Star\nt", "Stop" as "Sto\np"
- **Fix**: Increased button size (90dp), proper content padding
- **Result**: Full text visible, properly centered

### **Added Missing Haptic Feedback**
- **Issue**: No vibration when scrolling time picker
- **Fix**: Implemented vibration on each number change (10ms subtle pulse)
- **Result**: iOS-like smooth scrolling experience

---

## 🔧 Technical Improvements

### **New Dependencies**
- Room 2.6.1 - SQLite database wrapper
- KSP 2.0.21 - Kotlin Symbol Processing for Room
- Material Icons Extended - Additional icon set

### **Architecture**
- Repository pattern for data layer
- Flow-based reactive updates
- Proper database schema with migrations support
- Coroutines for async operations

### **Code Quality**
- ProGuard rules updated for Room
- Database queries optimized
- Memory leak prevention with proper disposal
- Better state management

---

## 📥 Installation

### **New Users**
1. Download `TimeMachine-v1.1.0.apk` below
2. Enable "Install from Unknown Sources"
3. Install and enjoy!

### **Upgrading from v1.0.0**
Simply install the new APK over the old one - all your settings will be preserved!

---

## 📊 Technical Details

- **App Name**: TimeMachine (formerly Pomodoro Timer)
- **Version**: 1.1.0 (Version Code: 2)
- **APK Size**: ~22MB (increased due to Room database)
- **Min Android**: 12 (API 31)
- **Target Android**: 15 (API 36)
- **Database**: Room 2.6.1 with SQLite

---

## 🎯 Use Cases

### **Productivity**
- Quick work sessions with saved presets
- Pomodoro technique made easy
- Focus time management

### **Health & Wellness**
- Meditation timers
- Workout intervals
- Breathing exercises

### **Daily Life**
- Cooking timers
- Study sessions
- Power naps
- Tea/coffee brewing

---

## 🔄 Migration from v1.0.0

**Good News**: Seamless upgrade!
- ✅ No data loss
- ✅ All settings preserved
- ✅ New preset feature automatically available
- ✅ Same package name (no uninstall needed)

**What Changes**:
- 📱 App name: "Pomodoro Timer" → "TimeMachine"
- 🆕 New preset chips appear at top
- 💾 Database created automatically

---

## 🌟 Highlights

| Feature | v1.0.0 | v1.1.0 |
|---------|--------|--------|
| **Presets** | ❌ None | ✅ Unlimited saved presets |
| **One-tap start** | ❌ No | ✅ Yes |
| **Haptic feedback** | ❌ No | ✅ Yes |
| **Picker alignment** | ⚠️ Buggy | ✅ Perfect |
| **Database** | ❌ None | ✅ SQLite with Room |
| **App name** | Pomodoro Timer | **TimeMachine** |

---

## 📝 Known Issues

None! 🎉 This version has been thoroughly tested.

---

## 🔗 Links

- **Repository**: https://github.com/aliahadmd/TimeMachine
- **Download APK**: See attachments below
- **Report Issues**: https://github.com/aliahadmd/TimeMachine/issues
- **Full Changelog**: https://github.com/aliahadmd/TimeMachine/compare/v1.0.0...v1.1.0

---

## 🙏 Thank You

Thank you for using TimeMachine! This update brings significant improvements based on user feedback.

If you enjoy the app:
- ⭐ **Star** the repository
- 🐛 **Report** any bugs you find
- 💡 **Suggest** new features
- 📢 **Share** with friends

---

## 🚀 What's Next?

Planned for future versions:
- [ ] Pause/Resume functionality
- [ ] Background notifications
- [ ] Timer history and statistics
- [ ] Custom alarm sounds
- [ ] Widget support
- [ ] Multiple simultaneous timers
- [ ] Cloud backup of presets

---

<div align="center">

**Built with ❤️ using Jetpack Compose**

Enjoy TimeMachine v1.1.0! ⏰✨

</div>

