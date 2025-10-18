# Changelog

All notable changes to TimeMachine will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2025-10-18

### Added
- **Preset System** - Save timer durations with custom names
  - SQLite database with Room for data persistence
  - Horizontal scrolling preset chips
  - One-tap preset loading and auto-start
  - Delete presets functionality
  - Unlimited preset storage
- **Haptic Feedback** - Subtle vibrations when scrolling time picker
- **Material Icons Extended** - Enhanced icon library

### Fixed
- **Picker Alignment Bug** - Highlight box now perfectly aligns with selected number
  - Rewrote using `derivedStateOf` for accurate center calculation
  - Fixed one-item offset issue
- **Button Text Cutoff** - Start/Stop buttons now display text properly
  - Increased button size to 90dp
  - Added proper content padding
- **Missing Haptic Feedback** - Added iOS-like scroll vibrations

### Changed
- **App Name** - Rebranded from "Pomodoro Timer" to "TimeMachine"
- **APK Name** - Changed from `PomodoroTimer-*.apk` to `TimeMachine-*.apk`
- **GitHub Repository** - Updated to `github.com/aliahadmd/TimeMachine`
- **Button Sizes** - Increased from 80dp to 90dp for better UX
- **Time Picker Width** - Increased from 100dp to 120dp

### Technical
- Added Room 2.6.1 for database management
- Added KSP 2.0.21 for annotation processing
- Implemented Repository pattern for data layer
- Added Flow-based reactive data updates
- Updated ProGuard rules for Room
- Database schema with Entity, DAO, and Database classes

---

## [1.0.0] - 2025-10-18

### Added
- **Initial Release**
- Apple-style scrollable time picker (hours and minutes)
- Real-time countdown timer with circular progress indicator
- Alarm sound and vibration when timer completes
- Automatic dark/light mode based on system preference
- Material 3 design with modern UI
- Start/Stop functionality
- Alarm dismissal screen
- Edge-to-edge immersive display
- Custom app icon with timer design

### Features
- Set duration from 1 minute to 23 hours 59 minutes
- Visual countdown with remaining time display
- System alarm sound (looping)
- Vibration pattern on completion
- Clean, minimal interface
- Smooth animations and transitions
- No ads, completely free
- Works 100% offline
- Battery efficient

### Technical
- Built with Jetpack Compose
- Kotlin 2.0.21
- Material 3 components
- Minimum Android 12 (API 31)
- Target Android 15 (API 36)
- ProGuard/R8 optimization enabled
- Signed release APK

---

## Version History

| Version | Release Date | Key Features |
|---------|--------------|--------------|
| **1.1.0** | 2025-10-18 | Presets, UI fixes, rebranding |
| **1.0.0** | 2025-10-18 | Initial release |

---

## Upgrade Notes

### 1.0.0 → 1.1.0
- ✅ Seamless upgrade (no data loss)
- ✅ Same package name (install over existing)
- ✅ App renamed to "TimeMachine"
- 🆕 New preset functionality available immediately
- 💾 Database created automatically on first launch
- 📊 Version code: 1 → 2

---

## Future Plans

### v1.2.0 (Planned)
- [ ] Pause/Resume timer functionality
- [ ] Background notifications during countdown
- [ ] Timer completion history
- [ ] Statistics and usage analytics

### v1.3.0 (Planned)
- [ ] Custom alarm sounds
- [ ] Home screen widgets
- [ ] Multiple simultaneous timers
- [ ] Cloud sync for presets

### Future Considerations
- [ ] Preset categories/folders
- [ ] Preset sharing via QR code
- [ ] Themes and color customization
- [ ] Tablet optimization
- [ ] Wear OS companion app

---

## Links

- **Repository**: https://github.com/aliahadmd/TimeMachine
- **Releases**: https://github.com/aliahadmd/TimeMachine/releases
- **Issues**: https://github.com/aliahadmd/TimeMachine/issues
- **Latest APK**: https://github.com/aliahadmd/TimeMachine/releases/latest

