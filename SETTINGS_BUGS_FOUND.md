# 🐛 Settings Section - Bugs & Issues Report

## ⚠️ CRITICAL BUGS

### 1. **Foreign Key Constraint Violation - Import Functions**
**Severity:** CRITICAL  
**Impact:** Import fails or creates orphaned data

**Issue:**
When importing parent-child relationships (categories → sessions/expenses, habits → completions), the child entities reference OLD IDs from the backup, but the parents get NEW IDs from the database.

**Affected Functions:**
- `importFocusTracker()` - Categories & Sessions
- `importHabitTracker()` - Habits & Completions
- `importExpenseTracker()` - Categories & Expenses

**Example:**
```kotlin
// Categories imported with id=0, get NEW IDs (e.g., 100, 101, 102)
database.expenseCategoryDao().insertCategory(category)

// Expenses still reference OLD IDs from backup (e.g., 1, 2, 3)
categoryId = expenseBackup.categoryId // ❌ WRONG! References old ID
```

**Fix:**
Create ID mapping: `oldId → newId` when inserting parents, then use mapping for children.

---

### 2. **No Clear All Data Functionality**
**Severity:** MODERATE  
**Impact:** User cannot reset/clear all data (mentioned in warning dialog but not implemented)

**Issue:**
The import warning dialog mentions "Cannot be undone automatically" and suggests exporting first, but there's no "Clear All Data" button to actually clear the database.

**Fix:**
Add "Clear All Data" button with confirmation dialog.

---

## 🎨 UI/UX ISSUES

### 3. **No Progress Indication for Large Imports**
**Severity:** MODERATE  
**Impact:** Poor UX for large data imports

**Issue:**
Only shows generic "Importing data..." message. For large datasets, user has no idea of progress.

**Fix:**
Add progress percentage or item count updates during import.

---

### 4. **Success Message Auto-Dismiss Missing**
**Severity:** MINOR  
**Impact:** User must manually dismiss success messages

**Issue:**
Success/error messages require manual dismissal. Should auto-dismiss after a few seconds.

**Fix:**
Add `LaunchedEffect` to auto-dismiss after 5 seconds.

---

### 5. **No File Size Warning**
**Severity:** MINOR  
**Impact:** User might try to import very large files

**Issue:**
No validation of file size before attempting import. Could cause app to hang or crash.

**Fix:**
Check file size before import, warn if > 10MB.

---

### 6. **Import Doesn't Show Details**
**Severity:** MINOR  
**Impact:** User doesn't know what was actually imported

**Issue:**
Success message shows total count but not breakdown by module.

**Example:**
Current: "✅ Imported 20 items successfully!"  
Better: "✅ Imported 20 items: 5 categories, 10 sessions, 3 habits, 2 expenses"

**Fix:**
Display detailed breakdown in success message.

---

## 🔧 LOGIC ISSUES

### 7. **Duplicate Data on Multiple Imports**
**Severity:** MODERATE  
**Impact:** Multiple imports of same file create duplicates

**Issue:**
Import always adds new data, never checks for duplicates. Warning mentions this but doesn't prevent it.

**Fix:**
Add option to "Merge" or "Replace" on import, or implement duplicate detection by timestamp/content hash.

---

### 8. **Missing Import Validation**
**Severity:** MODERATE  
**Impact:** Invalid JSON or wrong schema crashes import

**Issue:**
No validation that the imported file is a valid TimeManager backup.

**Fix:**
Validate backup version and structure before importing.

---

### 9. **No Rollback on Partial Failure**
**Severity:** HIGH  
**Impact:** Partial import leaves database in inconsistent state

**Issue:**
If import fails mid-way (e.g., at expenses after categories imported), no rollback occurs. Database is left with partial data.

**Fix:**
Wrap entire import in a database transaction, rollback on any error.

---

## 📊 PERFORMANCE ISSUES

### 10. **No Batch Inserts**
**Severity:** MODERATE  
**Impact:** Slow import for large datasets

**Issue:**
Each item is inserted individually. For 1000 items, that's 1000 separate database calls.

**Fix:**
Use Room's batch insert methods or prepare bulk insert statements.

---

### 11. **Running on Main Thread Risk**
**Severity:** LOW  
**Impact:** Could cause ANR for very large exports

**Issue:**
Although `withContext(Dispatchers.IO)` is used, the JSON serialization happens in the main export function.

**Fix:**
Ensure all heavy operations are properly dispatched to IO thread.

---

## 🔒 SECURITY/PRIVACY ISSUES

### 12. **No Backup Encryption**
**Severity:** LOW (Enhancement)  
**Impact:** Backup files contain sensitive data in plain text

**Issue:**
Exported JSON is plain text. Anyone with file access can read all user data (expenses, habits, etc.).

**Fix:**
Add optional password-protected encryption for backups.

---

### 13. **No Backup Validation Hash**
**Severity:** LOW  
**Impact:** Can't verify backup file integrity

**Issue:**
No checksum or hash to verify backup hasn't been tampered with or corrupted.

**Fix:**
Add SHA-256 hash to backup metadata, verify on import.

---

## 📱 UI POLISH

### 14. **No Empty State for About Dialog**
**Severity:** TRIVIAL  
**Impact:** None

**Issue:**
About dialog looks good but could use app icon or better branding.

**Fix:**
Add app icon image instead of emoji.

---

### 15. **Settings Cards Not Disabled During Operation**
**Severity:** MINOR  
**Impact:** User could trigger multiple imports/exports

**Issue:**
Export/Import buttons are disabled, but other settings cards aren't during operations.

**Fix:**
Disable all interactive elements during import/export.

---

## 🧪 TESTING ISSUES

### 16. **No Error Recovery Testing**
**Severity:** MODERATE  
**Impact:** Unknown behavior on edge cases

**Issue:**
No clear error handling for:
- Corrupted JSON files
- Missing permissions
- Disk space full
- Database locked

**Fix:**
Add comprehensive error handling and user-friendly error messages.

---

## 📋 SUMMARY

| Priority | Count | Issues |
|----------|-------|--------|
| CRITICAL | 2 | Foreign key violations, No rollback |
| HIGH | 1 | Partial failure handling |
| MODERATE | 6 | Clear data, validation, duplicates, batch inserts, progress, testing |
| MINOR | 5 | Auto-dismiss, file size, details, UI polish |
| LOW | 2 | Encryption, hash validation |

**Total Issues Found: 16**

---

## 🎯 RECOMMENDED FIX ORDER

1. ✅ **Foreign Key Constraint Fix** (CRITICAL)
2. ✅ **Transaction Rollback** (HIGH)
3. ✅ **Import Validation** (MODERATE)
4. ✅ **Clear All Data Button** (MODERATE)
5. ✅ **Detailed Success Message** (MINOR)
6. ✅ **Auto-dismiss Messages** (MINOR)
7. 🔄 **Batch Inserts** (MODERATE - optional enhancement)
8. 🔄 **Progress Indication** (MODERATE - optional enhancement)
9. 🔄 **Encryption** (LOW - future enhancement)

---

*Generated: October 21, 2025*  
*Review Status: Complete*  
*Next: Implement Fixes*

