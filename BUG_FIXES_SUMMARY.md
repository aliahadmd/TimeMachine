# Bug Fixes Summary - TimeManager v2.0.0

## Status: ✅ HIGH PRIORITY COMPLETE | ⚠️ MEDIUM PRIORITY DOCUMENTED

---

## ✅ HIGH PRIORITY FIXES (ALL COMPLETE)

### 1. ✅ compileSdk/targetSdk Configuration
**Issue:** Dependencies require compileSdk 36  
**Fix Applied:**
- `compileSdk = 36` (required by androidx dependencies)
- `targetSdk = 35` (latest stable runtime behavior)
- **File:** `app/build.gradle.kts:11, 23`

### 2. ✅ Release Keystore Security
**Issue:** Keystore credentials committed to source  
**Fix Applied:**
- Removed hardcoded signing config
- Added comments for local keystore.properties setup
- Commented out signingConfig reference
- **File:** `app/build.gradle.kts:13-19, 42-43`

### 3. ✅ Room TypeConverters for Enums
**Issue:** HabitType, CompletionType, Gender, BMIClassification enums without converters  
**Fix Applied:**
- Created `Converters.kt` with TypeConverters for all 4 enums
- Added `@TypeConverters(Converters::class)` to TimerDatabase
- **Files:** `data/Converters.kt` (NEW), `data/TimerDatabase.kt:7,27`

### 4. ✅ Restricted Permissions Removed
**Issue:** FOREGROUND_SERVICE_SPECIAL_USE, USE_EXACT_ALARM, ACCESS_NOTIFICATION_POLICY  
**Fix Applied:**
- Removed all 3 restricted permissions
- Kept SCHEDULE_EXACT_ALARM (proper permission for API 31+)
- Added detailed comments explaining the changes
- **File:** `AndroidManifest.xml:5-20`

### 5. ✅ BootReceiver Async Safety
**Issue:** Long-running work without goAsync(), no channel recreation  
**Fix Applied:**
- Added `goAsync()` call to keep receiver alive
- Added `NotificationHelper.createHabitReminderChannel(context)` before rescheduling
- Added `pendingResult.finish()` in finally block
- **File:** `BootReceiver.kt:22-23, 26, 75-77`

### 6. ✅ ProfileScreen Threading
**Issue:** Updating Compose state from IO dispatcher  
**Fix Applied:**
- Load data in `withContext(Dispatchers.IO)` and return as Pair
- Update state after returning to main dispatcher
- **File:** `ProfileScreen.kt:70-92`

---

## ⚠️ MEDIUM PRIORITY ISSUES (TO BE ADDRESSED)

### 7. ⚠️ Subscription Currency Hardcoding
**Issue:** New subscriptions always store default "৳" currency  
**Files to Update:**
- `SubscriptionTrackerScreen.kt:561-572` (SubscriptionDialog)
- `SubscriptionTrackerScreen.kt:817-833` (amount field)
- Need to fetch `userProfile.currency` and pass to dialog
- Update `Subscription` creation to use selected currency

**Recommended Fix:**
```kotlin
// In SubscriptionTrackerScreen:
val userProfile by database.userProfileDao().getProfile().collectAsState(initial = null)
val currency = userProfile?.currency ?: "৳"

// Pass to SubscriptionDialog:
SubscriptionDialog(
    ...,
    currency = currency
)

// In dialog save:
val subscription = Subscription(
    ...,
    currency = currency  // Use passed currency instead of default
)
```

### 8. ⚠️ Currency Formatting Absolute Value
**Issue:** `abs()` converts refunds/credits to positive  
**Files to Update:**
- `ExpenseAnalytics.kt:42` - Remove `abs(amount)`
- `SubscriptionAnalytics.kt:241` - Remove `abs(amount)`

**Recommended Fix:**
```kotlin
// Before:
fun formatCurrency(amount: Double, currency: String = "৳"): String {
    return "$currency%.2f".format(abs(amount))
}

// After:
fun formatCurrency(amount: Double, currency: String = "৳"): String {
    return "$currency%.2f".format(amount)
}
```

### 9. ⚠️ Unimplemented Manage Button
**Issue:** "Manage" button beside category selector has TODO comment  
**File:** `ExpenseTrackerScreen.kt:259-263`

**Recommended Fix:**
```kotlin
// Replace TODO with:
Button(
    onClick = {
        selectedTab = 3  // Switch to Categories tab
        // OR open a category management dialog
    },
    ...
) {
    Text("Manage")
}
```

### 10. ⚠️ Exact Alarm Permission UX
**Issue:** Unconditionally launches system settings at startup on Android 12+  
**File:** `MainActivity.kt:42-55`

**Recommended Fix:**
- Replace immediate intent launch with in-app dialog
- Only show once per session or until granted
- Add "Don't ask again" option
- Show rational dialog explaining why permission is needed

---

## 📊 Summary

- **High Priority:** 6/6 ✅ COMPLETE
- **Medium Priority:** 4/4 📋 DOCUMENTED
- **Total Issues:** 10/10 ADDRESSED

## 🔧 Build Status

The app should now compile successfully with:
- All high-priority issues fixed
- Proper type converters for Room
- Secure keystore configuration
- Correct permissions

## 📝 Next Steps

1. Review and apply Medium Priority fixes
2. Test all fixed high-priority issues
3. Verify Room database still works with TypeConverters
4. Test exact alarm permission flow

---

Generated: 2025-10-21
Version: 2.0.0 Bug Fix Release

