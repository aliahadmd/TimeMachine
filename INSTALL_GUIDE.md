# TimeMachine v2.0.0 - Installation Guide

## 📱 Quick Installation

### Method 1: Direct Installation (Recommended)

1. **Transfer APK to Device**
   - Connect your Android device to computer via USB
   - Copy `TimeMachineV2.0.0.apk` to your device's Downloads folder
   - Or use ADB: `adb install TimeMachineV2.0.0.apk`

2. **Enable Unknown Sources**
   - Go to **Settings** → **Security** → **Unknown Sources**
   - Or **Settings** → **Apps** → **Special Access** → **Install Unknown Apps**
   - Enable for your file manager or browser

3. **Install**
   - Open your file manager
   - Navigate to Downloads
   - Tap on `TimeMachineV2.0.0.apk`
   - Tap **Install**
   - Wait for installation to complete
   - Tap **Open** to launch

### Method 2: ADB Installation (For Developers)

```bash
# Connect device via USB with USB debugging enabled
adb devices

# Install the APK
adb install -r TimeMachineV2.0.0.apk

# Launch the app
adb shell am start -n me.aliahad.timemanager/.MainActivity
```

---

## ✅ System Requirements

- **Android Version**: 12 (API 31) or higher
- **Storage**: At least 50 MB free space
- **RAM**: 2 GB or more recommended
- **Screen**: Any screen size supported

---

## 🔐 Security

### APK Verification

Verify the APK integrity before installation:

```bash
# Check MD5 hash
md5 TimeMachineV2.0.0.apk
# Should output: 624619775c92b2466774015748e20a82
```

### Signing Information

- **Signed**: ✅ Yes (Release keystore)
- **Keystore**: `release-keystore.jks`
- **Key Alias**: `timemanager`

---

## 🚀 First Launch

### Initial Setup

1. **Launch the App**
   - Tap the TimeMachine icon on your home screen
   - The app will initialize the database

2. **Grant Permissions (If Needed)**
   - The app requires minimal permissions
   - Notification permission (optional, for habit reminders)

3. **Start Using**
   - **Home Screen**: Shows all available features
   - **Focus Tracker**: Create categories and start tracking
   - **Year Calculator**: Calculate dates
   - **BMI Calculator**: Track your health metrics
   - **Habit Tracker**: Build better habits

---

## 🎯 Feature Quick Start

### Focus Tracker
1. Tap **Focus Tracker** on home screen
2. Go to **Categories** tab
3. Tap **+ Add Category**
4. Set name, icon, color, and daily goal
5. Go back to **Track** tab
6. Select your category
7. Tap **Start Focus Session**
8. Enter immersive fullscreen mode
9. Double-tap to show/hide controls
10. Press **STOP** when done (minimum 30s to save)

### Year Calculator
1. Tap **Year Calculator** on home screen
2. Select **Birthdate** or **From Date**
3. Enter number of years
4. View results instantly
5. Tap **Save Calculation** to keep history

### BMI Calculator
1. Tap **BMI Calculator** on home screen
2. Enter your details:
   - Age
   - Height (ft/cm)
   - Weight (kg/lb)
   - Gender
   - Classification system (WHO/DGE)
3. Tap **Calculate BMI**
4. View results with graph and recommendations
5. Tap **Save** to keep history

### Habit Tracker
1. Tap **Habit Tracker** on home screen
2. Tap **+** to add new habit
3. Set name, icon, color
4. Choose reminder time (optional)
5. Tap habit card to mark as complete
6. View streaks and statistics

---

## 🐛 Troubleshooting

### App Won't Install

**Error: "App not installed"**
- Solution: Uninstall old version first
- Go to Settings → Apps → TimeMachine → Uninstall
- Then install the new APK

**Error: "Package conflicts with existing package"**
- Solution: Clear app data before uninstalling
- Or use: `adb uninstall me.aliahad.timemanager`

### App Crashes on Launch

**Black screen or immediate crash**
- Solution 1: Clear app cache and data
- Solution 2: Restart your device
- Solution 3: Reinstall the app

**Database error**
- Solution: The app auto-migrates from older versions
- If issues persist, clear app data (loses local data)

### Features Not Working

**Sessions not saving**
- Ensure you track for at least 30 seconds
- Look for green "✓ Session will be saved" indicator
- Check if category is selected

**Notifications not showing**
- Go to Settings → Apps → TimeMachine → Notifications
- Enable all notification channels
- Check Do Not Disturb settings

**UI not updating**
- Pull to refresh if available
- Close and reopen the app
- Data refreshes automatically when screens resume

---

## 📊 Data Management

### Backup Your Data

The app stores data locally in a SQLite database:
- Location: `/data/data/me.aliahad.timemanager/databases/`
- Database file: `timer_database.db`

To backup (requires root or ADB):
```bash
# Backup database
adb pull /data/data/me.aliahad.timemanager/databases/timer_database.db ./backup.db

# Restore database
adb push ./backup.db /data/data/me.aliahad.timemanager/databases/timer_database.db
```

### Clear App Data

If you want to start fresh:
1. Settings → Apps → TimeMachine
2. Storage → Clear Data
3. This will reset everything

---

## 🔄 Updating from Previous Versions

### From v1.x.x to v2.0.0

**Automatic Migration**:
- Database automatically migrates to version 6
- Old timer data is preserved (if any)
- Habits and existing data remain intact

**Manual Steps**:
1. Install `TimeMachineV2.0.0.apk` over existing version
2. Or uninstall old version first (loses data)
3. Launch app
4. Migration happens automatically on first launch

**What's Changed**:
- Focus Timer replaced with Focus Tracker
- New category-based system
- Old timer sessions (if any) are preserved

---

## 💡 Tips & Tricks

### Focus Tracker
- Set realistic daily goals (e.g., 60-120 minutes)
- Use categories for different activities (Work, Study, Exercise)
- Double-tap in fullscreen to quickly check progress
- Sessions under 30s won't save (prevents accidental taps)

### Year Calculator
- Useful for age calculations
- Perfect for anniversaries and milestones
- Save important date calculations

### BMI Calculator
- Track weight changes over time
- Compare WHO vs DGE classifications
- Use health tips for guidance

### Habit Tracker
- Start with 1-3 habits (don't overwhelm yourself)
- Enable reminders for consistency
- Celebrate streaks!

---

## 🆘 Support

### Getting Help

1. **Check Release Notes**: Read `RELEASE_NOTES_V2.0.0.md`
2. **Check Logs**: Use `adb logcat | grep TimeMachine`
3. **Report Issues**: Contact developer with logs

### Known Issues

- **Deprecation warnings in build**: Cosmetic only, no impact on functionality
- **Leap year dates**: Handled gracefully, no crashes
- **Timezone**: All dates use device timezone

---

## 📝 Version Information

- **Version Name**: 2.0.0
- **Version Code**: 5
- **Package Name**: me.aliahad.timemanager
- **Min SDK**: 31 (Android 12)
- **Target SDK**: 36 (Android 16)
- **Release Date**: October 20, 2025

---

## 🎉 Enjoy TimeMachine v2.0.0!

Thank you for using TimeMachine. We hope this update brings you better productivity and health tracking!

**Happy Time Managing!** ⏰✨

