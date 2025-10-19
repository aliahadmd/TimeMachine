# Focus Timer Ecosystem Debug Guide

## 🔍 What Was Fixed

### 1. **Added Refresh Trigger Parameter to UI Components**
   - `TodaySummaryCard` now accepts `refreshTrigger: Int` parameter
   - `RecentSessionsCard` now accepts `refreshTrigger: Int` parameter
   - Both components' `LaunchedEffect` now depends on `refreshTrigger`
   - This ensures they re-fetch data when lifecycle events trigger

### 2. **Enhanced Database Save Logic**
   - Session save now uses `withContext(Dispatchers.IO)` properly
   - Returns session ID from insert operation
   - Immediately verifies save by querying total minutes
   - Added comprehensive logging at every step

### 3. **Comprehensive Logging Added**
   
   **ImmersiveTimerActivity:**
   - Logs when session save starts
   - Logs session details (categoryId, duration, date)
   - Logs session ID after save
   - Verifies total minutes immediately after save
   - Logs if session was too short to save

   **HomeScreen:**
   - Logs every time it fetches today's minutes
   - Shows date, minutes, and refreshTrigger value
   
   **FocusTrackerScreen (TrackingTab):**
   - Logs ON_RESUME events
   - Shows refreshTrigger value after increment
   
   **TodaySummaryCard:**
   - Logs when fetching data (with refreshTrigger)
   - Shows total minutes and session count
   - Shows category-specific minutes
   
   **RecentSessionsCard:**
   - Logs when fetching sessions (with refreshTrigger)
   - Shows number of sessions found

## 📋 Testing Instructions

### Step 1: Start Fresh
```bash
adb shell pm clear me.aliahad.timemanager
adb shell am start -n me.aliahad.timemanager/.MainActivity
```

### Step 2: Setup
1. Open the app
2. Go to Focus Tracker → Categories tab
3. Create a new category (e.g., "Test" with 1 minute daily goal)
4. Return to Track tab
5. Select the "Test" category

### Step 3: Run 1-Minute Timer
1. Tap "Start Focus Session"
2. Wait for 1 minute in full-screen mode
3. Tap to show controls (double-tap)
4. Press STOP button
5. Wait for activity to close

### Step 4: Monitor Logs in Real-Time
```bash
# In a separate terminal, run:
adb logcat -c && adb logcat | grep -E '(ImmersiveTimer|FocusTracker|HomeScreen)'
```

### Step 5: Check What You Should See

**Expected Log Sequence:**

```
// When you press STOP:
ImmersiveTimer: Saving session: categoryId=X, duration=1min, date=2025-10-19
ImmersiveTimer: Session saved with ID=Y. Total today: 1min
ImmersiveTimer: Session committed to database successfully (ID: Y)

// When returning to HomeScreen:
FocusTracker: TrackingTab ON_RESUME - refreshTrigger now: 1
HomeScreen: Fetching today's minutes for 2025-10-19: 1 min (refreshTrigger=1)

// When FocusTracker updates:
FocusTracker: TodaySummaryCard fetching data for 2025-10-19 (refreshTrigger=1)
FocusTracker: TodaySummaryCard: Total=1 min, Count=1 sessions
FocusTracker: TodaySummaryCard: Category 'Test' today=1 min

FocusTracker: RecentSessionsCard fetching sessions for 2025-10-19 (refreshTrigger=1)
FocusTracker: RecentSessionsCard: Found 1 sessions
```

## 🐛 Debugging Checklist

If sync still doesn't work, check:

### ✅ Session is Being Saved
Look for: `ImmersiveTimer: Session saved with ID=X`
- If missing: Check if timer ran for >= 60 seconds
- If present: Database insert is working

### ✅ Lifecycle Events Firing
Look for: `FocusTracker: TrackingTab ON_RESUME - refreshTrigger now:`
- If missing: Lifecycle observer not working
- If present: Refresh trigger is incrementing

### ✅ UI Components Fetching Data
Look for: `TodaySummaryCard fetching data` and `RecentSessionsCard fetching sessions`
- If missing: LaunchedEffect not triggering
- If present but shows 0: Database query issue

### ✅ Date Format Consistency
All logs should show same date format: `2025-10-19` (ISO_LOCAL_DATE)
- If different: Date formatting issue
- If same: Date is consistent

## 🔧 Common Issues & Solutions

### Issue 1: Session Saves but UI Shows 0
**Diagnosis:** Check if dates match in logs
**Solution:** Verify `getTodayDateString()` returns consistent format

### Issue 2: No Lifecycle Events
**Diagnosis:** `ON_RESUME` never logged
**Solution:** Check if `DisposableEffect` is properly set up

### Issue 3: LaunchedEffect Not Triggered
**Diagnosis:** No "fetching data" logs
**Solution:** Verify `refreshTrigger` is passed as parameter and used in `LaunchedEffect`

### Issue 4: Database Query Returns 0
**Diagnosis:** Session saved but query returns 0
**Solution:** 
```bash
# Manually inspect database:
adb shell "run-as me.aliahad.timemanager cat /data/data/me.aliahad.timemanager/databases/timer_database | strings | grep time_sessions"
```

## 📊 Expected Behavior After Fix

1. **Immediate Update:** All UI components update within 1 second of returning from timer
2. **Consistent Data:** HomeScreen, TrackingTab, and StatsTab all show same values
3. **Progress Bar:** Full-screen mode shows accurate progress including just-completed sessions
4. **Calendar View:** Today's date immediately turns green when goal is met

## 🎯 Success Criteria

✅ HomeScreen block shows "1 min today"  
✅ TrackingTab "Today's Progress" shows 1 session, 1 min  
✅ RecentSessionsCard lists the completed session  
✅ StatsTab shows updated category statistics  
✅ Calendar view shows today as green (goal met)  
✅ Starting another session shows progress > 100% in full-screen

## 📝 Additional Debug Commands

```bash
# Clear app data and restart
adb shell pm clear me.aliahad.timemanager && adb shell am start -n me.aliahad.timemanager/.MainActivity

# View ALL logs (verbose)
adb logcat | grep -E '(ImmersiveTimer|FocusTracker|HomeScreen|TimeSession|ActivityCategory)'

# Check database file exists
adb shell "run-as me.aliahad.timemanager ls -la /data/data/me.aliahad.timemanager/databases/"

# Export database for inspection (on rooted device)
adb pull /data/data/me.aliahad.timemanager/databases/timer_database ./debug_db.sqlite
```

## 🚀 Next Steps

1. Test with the instructions above
2. Share the logcat output if issues persist
3. Check for any exceptions or errors in logs
4. Verify all expected log messages appear in correct order

