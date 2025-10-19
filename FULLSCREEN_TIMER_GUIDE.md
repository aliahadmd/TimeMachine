# Fullscreen Focus Timer - User Guide

## 🎯 Overview

The Focus Tracker now includes a **beautiful fullscreen timer mode** perfect for placing your phone on your desk while you work. This mode features large, easy-to-read text, pause/resume controls, and full landscape orientation support.

---

## ✨ Features

### 1. **Automatic Fullscreen on Start**
- When you start tracking, the app automatically enters fullscreen mode
- Large, 120sp timer display visible from across the room
- Dark gradient background reduces eye strain
- Category icon and name displayed prominently

### 2. **Landscape Orientation Support**
- Rotate your phone to landscape for optimal desk viewing
- Screen rotation enabled (`sensor` mode in AndroidManifest)
- Layout adjusts automatically
- Timer state preserved during rotation

### 3. **Pause/Resume Functionality**
- ⏸️ Pause button - Stops the timer without ending the session
- ▶️ Resume button - Continues tracking from where you paused
- Timer changes color when paused (orange tint)
- "PAUSED" indicator clearly visible

### 4. **Keep Screen On**
- Screen stays awake while in fullscreen mode
- Uses `FLAG_KEEP_SCREEN_ON` automatically
- Clears flag when exiting fullscreen
- No manual screen timeout needed

### 5. **Desk-Friendly UI**
- **Giant Timer**: 120sp font size (5x larger than normal)
- **Dark Theme**: Black gradient background with subtle depth
- **High Contrast**: White text on dark background
- **Color Accents**: Category color for timer and visual identity
- **Large Buttons**: Easy to tap from across your desk

### 6. **Three Control Buttons**
- 🛑 **Stop** (Red) - End session and save (if ≥1 minute)
- ⏸️/▶️ **Pause/Resume** (Orange/Green) - Hold timer temporarily
- ℹ️ **Details** (White) - Exit fullscreen, return to normal view

### 7. **Bottom Info Bar**
- **Elapsed Time**: Current session duration
- **Status**: Running or Paused
- **Mode**: Active or Hold

---

## 📱 How to Use

### Starting Fullscreen Mode

**Method 1: Automatic (Recommended)**
1. Go to Focus Tracker → Track tab
2. Select a category
3. Tap "Start Tracking"
4. **Fullscreen mode activates automatically!** 🎉

**Method 2: Manual Entry**
1. Start tracking normally
2. Tap "Enter Fullscreen Mode" button
3. Fullscreen view appears

### Using the Timer

**In Fullscreen:**
- **Pause**: Tap the large orange pause button (⏸️)
- **Resume**: Tap the large green play button (▶️)
- **Stop**: Tap the red stop button (🛑)
- **Exit**: Tap the ℹ️ button or the × in the top-left

**Rotation:**
- Simply rotate your phone
- State is preserved automatically
- Layout adjusts to orientation

**Exiting Fullscreen:**
- Tap "Details" (ℹ️) button
- Tap × icon in top-left corner
- Press device back button
- Timer continues running in normal view

### Back to Normal View

When you exit fullscreen:
- Timer keeps running in background
- Can see Today's Summary, Recent Sessions
- Can manually enter fullscreen again
- Pause/Resume still available

---

## 🎨 Visual Design

### Color Scheme
```
Background: Dark gradient (#0D1117 → #161B22 → #0D1117)
Timer (Active): Category color (vibrant)
Timer (Paused): Orange (#FFA726)
Text: White with varying opacity
Buttons:
  - Stop: Red (#EF5350)
  - Pause: Orange (#FFA726)
  - Resume: Green (#66BB6A)
  - Details: White transparent
```

### Typography
- **Timer**: 120sp, Bold, Letter-spacing 4sp
- **Category Name**: Headline Medium, Bold
- **Duration**: Headline Medium, 60% opacity
- **Status**: Title Large, Bold (when paused)
- **Button Labels**: Body Medium, 60% opacity

### Spacing
- Padding: 32dp main, 16dp secondary
- Button spacing: 24dp horizontal
- Vertical spacing: 20-64dp depending on section

---

## 🔧 Technical Details

### Files Modified

**AndroidManifest.xml**
```xml
<activity
    android:name=".MainActivity"
    android:screenOrientation="sensor"
    android:configChanges="orientation|screenSize|keyboardHidden">
```
- Enables sensor-based rotation
- Handles orientation changes without recreating Activity
- Preserves state during rotation

**New File: FullscreenTimer.kt**
- 330 lines of beautiful Compose UI
- Custom gradient background
- Responsive layout for landscape/portrait
- Wake lock management
- Back button handling

**Updated: FocusTrackerScreen.kt**
- Added `isPaused` state
- Added `showFullscreen` state
- Modified timer ticker to respect pause
- Auto-enter fullscreen on start
- Pause/Resume buttons in normal view

### State Management
```kotlin
var isRunning: Boolean        // Timer is active
var isPaused: Boolean         // Timer is paused
var showFullscreen: Boolean   // In fullscreen mode
var elapsedSeconds: Int       // Time elapsed
var trackingCategoryId: Long  // Current category
```

### Timer Logic
```kotlin
LaunchedEffect(isRunning, isPaused) {
    while (isRunning && !isPaused) {
        delay(1000)
        elapsedSeconds++
    }
}
```
- Ticks every second when running AND not paused
- Automatically stops when paused
- Resumes from current value

---

## 🎯 Use Cases

### 1. **Pomodoro Technique**
- Set phone on desk in landscape
- Start 25-minute focus session
- Pause for short breaks
- Large timer visible at a glance

### 2. **Deep Work Sessions**
- Enter fullscreen for distraction-free tracking
- No notifications visible
- Just you, the timer, and your work
- Pause for quick interruptions

### 3. **Study Sessions**
- Track study time with large visual feedback
- Category shows which subject
- Pause for bathroom/water breaks
- Resume seamlessly

### 4. **Desk Workouts**
- Landscape mode for exercise routines
- Pause between sets
- Large buttons easy to tap with sweaty fingers
- Timer visible from exercise mat

---

## 🚀 Benefits

### For Users
✅ **Motivation**: Large visible timer keeps you accountable  
✅ **Flexibility**: Pause for interruptions without losing progress  
✅ **Comfort**: Dark theme reduces eye strain  
✅ **Convenience**: Auto-fullscreen removes extra steps  
✅ **Reliability**: Screen stays on automatically  

### For Productivity
✅ **Focus**: Minimal UI = fewer distractions  
✅ **Tracking**: Every second counted accurately  
✅ **Flexibility**: Pause/resume for real-world interruptions  
✅ **Visibility**: See timer from across the room  
✅ **Rotation**: Use in any orientation  

---

## 🧪 Testing Checklist

- [x] Fullscreen activates on timer start
- [x] Timer counts up correctly
- [x] Pause stops the timer
- [x] Resume continues from pause point
- [x] Stop saves session (if ≥1 min)
- [x] Rotation preserves state
- [x] Screen stays on in fullscreen
- [x] Exit returns to normal view
- [x] Back button exits fullscreen
- [x] × button exits fullscreen
- [x] Landscape orientation works
- [x] Portrait orientation works
- [x] Category color displays correctly
- [x] Bottom info bar updates
- [x] Button sizes are tappable

---

## 💡 Pro Tips

1. **Place Phone in Landscape**
   - Easier to see from your desk
   - More screen space for timer
   - Better button layout

2. **Use Pause Liberally**
   - Quick bathroom break? Pause!
   - Phone call? Pause!
   - Water refill? Pause!
   - Only running time counts

3. **Auto-Enter is Smart**
   - Just tap "Start" and you're in fullscreen
   - No extra steps needed
   - Exit anytime with back button

4. **Manual Entry Available**
   - Start normally if you want to see summary first
   - Enter fullscreen later with button
   - Flexibility for different workflows

5. **Screen Stays Alive**
   - No need to change screen timeout settings
   - Automatic wake lock in fullscreen only
   - Normal power management elsewhere

---

## 🎉 Enjoy Your Focus Sessions!

The fullscreen timer transforms your phone into a dedicated focus companion. Large, beautiful, and distraction-free—exactly what you need for deep work sessions.

**Happy Focusing!** 🚀

---

**Version**: 1.3.0  
**Last Updated**: 2025-10-19  
**Feature Status**: ✅ Production Ready

