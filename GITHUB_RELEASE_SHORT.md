# GitHub Release - Copy & Paste Format

## 📋 Release Title (for GitHub)

```
🎉 TimeMachine v2.0.0 - Focus Tracker Rewrite + Major Updates
```

---

## 📝 Release Description (for GitHub)

```markdown
> Major release with complete Focus Tracker rewrite, new features, and 10 critical bug fixes

## 📥 Download

**APK:** TimeMachineV2.0.0.apk (23 MB)  
**MD5:** `624619775c92b2466774015748e20a82`  
**Requires:** Android 12+ (API 31)

## ✨ What's New

### 🔥 Focus Tracker - Complete Rewrite
- **Category-based tracking** for different activities
- **Immersive fullscreen mode** with landscape lock
- **Real-time feedback:** 30s countdown → green checkmark
- **Universal session saving** (all exit methods work)
- **Advanced analytics** with calendar goal view
- **Customizable daily goals** per category

### 📅 Year Calculator
- Calculate dates from years ago or future
- Full leap year support (no crashes!)
- Save calculation history

### ⚖️ BMI Calculator
- WHO/DGE classifications
- Visual graphs & ideal weight ranges
- Health tips & history tracking

## 🐛 Critical Fixes

1. ✅ Universal session saving (back gesture, EXIT button now save)
2. ✅ Leap year crash resolution (Feb 29 handling)
3. ✅ Timezone fixes (habit dates now accurate)
4. ✅ Midnight rollover fix (date caching)
5. ✅ UI refresh improvements (lifecycle observers)
6. ✅ Progress bar always visible in fullscreen
7. ✅ Back gesture in habit detail view
8. ✅ Notification banner dismiss button
9. ✅ Session save feedback (30s minimum clear)
10. ✅ Real-time UI updates

## 🎨 UI/UX Improvements

- Zero elevation design (no shadows)
- Vibrant color palette
- Gradient icon backgrounds
- Live countdown & confirmation indicators
- Smooth animations & transitions

## 🔧 Technical

- Room Database v6 (auto-migrates)
- ProGuard + R8 optimization
- Resource shrinking
- Improved performance & stability

## 📦 Installation

**Method 1:** Download APK → Enable Unknown Sources → Install

**Method 2 (ADB):**
```bash
adb install TimeMachineV2.0.0.apk
```

## 🔄 Upgrading from v1.x

✅ Install directly over v1.x - data migrates automatically  
✅ All habits, calculations preserved  
✅ Settings carry over

## 📚 Documentation

- [Full Release Notes](./RELEASE_NOTES_V2.0.0.md)
- [Installation Guide](./INSTALL_GUIDE.md)

## 🎯 What's Next (v2.1.0)

- Export/Import data
- Cloud sync (optional)
- More themes
- Home screen widgets

---

**Full Changelog:** v1.3.0...v2.0.0

**Enjoy TimeMachine v2.0.0!** ⏰✨
```

---

## 🏷️ Release Tags (for GitHub)

Add these tags to the release:

- `major-release`
- `android`
- `productivity`
- `time-tracking`
- `habit-tracker`
- `focus-timer`
- `bmi-calculator`

---

## 📎 Assets to Upload

Upload these files as release assets:

1. **TimeMachineV2.0.0.apk** (Required - Main download)
2. **RELEASE_NOTES_V2.0.0.md** (Optional - Detailed notes)
3. **INSTALL_GUIDE.md** (Optional - Installation instructions)

---

## 🎨 Additional Notes for Release

**Highlight:** This is a major version bump from v1.3.0 to v2.0.0 because:
- Complete rewrite of core feature (Focus Timer → Focus Tracker)
- Breaking architectural changes (new database schema)
- Significant UX paradigm shift (category-based system)

**Mark as:** ✅ Latest Release

**Pre-release:** ❌ No (this is stable)

