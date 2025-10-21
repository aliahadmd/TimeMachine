# 📊 TimeManager Database Structure
**Database Version:** 9  
**Database Name:** timer_database  
**Total Tables:** 11

---

## 🗄️ Database Schema Visualization

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          TIMEMANAGER DATABASE v9                              │
│                         (timer_database.db)                                   │
└──────────────────────────────────────────────────────────────────────────────┘


╔════════════════════════════════════════════════════════════════════════════╗
║                        FOCUS TRACKER MODULE                                 ║
╚════════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────┐         ┌────────────────────────────────┐
│   activity_categories           │         │      time_sessions             │
├─────────────────────────────────┤         ├────────────────────────────────┤
│ 🔑 id (PK, LONG)                │◄────────│ 🔑 id (PK, LONG)              │
│    name (STRING)                │    │    │ 🔗 categoryId (FK, LONG)      │
│    color (LONG)                 │    │    │    startTime (LONG)           │
│    icon (STRING)                │    │    │    endTime (LONG)             │
│    dailyGoalMinutes (INT)       │    │    │    durationMinutes (INT)      │
│    isActive (BOOLEAN)           │    │    │    date (STRING)              │
│    sortOrder (INT)              │    │    │    notes (STRING)             │
│    createdAt (LONG)             │    │    │    createdAt (LONG)           │
│    updatedAt (LONG)             │    │    ├────────────────────────────────┤
├─────────────────────────────────┤    │    │ 📑 Indices:                   │
│ No Foreign Keys                 │    │    │    - categoryId               │
└─────────────────────────────────┘    │    │    - date                     │
                                       │    ├────────────────────────────────┤
                                       └────│ 🔗 Foreign Keys:              │
                                            │    categoryId → activity_      │
                                            │    categories.id (CASCADE)    │
                                            └────────────────────────────────┘


╔════════════════════════════════════════════════════════════════════════════╗
║                        HABIT TRACKER MODULE                                 ║
╚════════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────┐         ┌────────────────────────────────┐
│         habits                  │         │    habit_completions           │
├─────────────────────────────────┤         ├────────────────────────────────┤
│ 🔑 id (PK, LONG)                │◄────────│ 🔑 habitId (PK, LONG)         │
│    name (STRING)                │    │    │ 🔑 date (PK, STRING)          │
│    description (STRING)         │    │    │    completionType (ENUM)      │
│    color (LONG)                 │    │    │    completedAt (LONG)         │
│    iconName (STRING)            │    │    │    notes (STRING)             │
│    type (ENUM: BUILD/QUIT)      │    │    ├────────────────────────────────┤
│    goalPeriodDays (INT)         │    └────│ 🔗 Composite Primary Key:     │
│    isEveryday (BOOLEAN)         │         │    (habitId, date)            │
│    reminderTimeHour (INT?)      │         ├────────────────────────────────┤
│    reminderTimeMinute (INT?)    │         │ Note: habitId references      │
│    createdAt (LONG)             │         │       habits.id               │
│    isActive (BOOLEAN)           │         └────────────────────────────────┘
└─────────────────────────────────┘


╔════════════════════════════════════════════════════════════════════════════╗
║                     YEAR CALCULATOR MODULE                                  ║
╚════════════════════════════════════════════════════════════════════════════╝

┌──────────────────────────────────────────────────────────────────────────┐
│                      date_calculations                                    │
├──────────────────────────────────────────────────────────────────────────┤
│ 🔑 id (PK, LONG)                                                         │
│    title (STRING)                                                        │
│    category (STRING)                                                     │
│    startDate (LONG - timestamp)                                          │
│    endDate (LONG - timestamp)                                            │
│    createdAt (LONG)                                                      │
│    updatedAt (LONG)                                                      │
├──────────────────────────────────────────────────────────────────────────┤
│ No Foreign Keys                                                          │
│ No Indices                                                               │
└──────────────────────────────────────────────────────────────────────────┘


╔════════════════════════════════════════════════════════════════════════════╗
║                      BMI CALCULATOR MODULE                                  ║
╚════════════════════════════════════════════════════════════════════════════╝

┌──────────────────────────────────────────────────────────────────────────┐
│                      bmi_calculations                                     │
├──────────────────────────────────────────────────────────────────────────┤
│ 🔑 id (PK, LONG)                                                         │
│    name (STRING)                                                         │
│    age (INT)                                                             │
│    heightCm (FLOAT)                                                      │
│    weightKg (FLOAT)                                                      │
│    gender (ENUM: MALE/FEMALE)                                            │
│    classification (ENUM: WHO/DGE)                                        │
│    bmiValue (FLOAT)                                                      │
│    category (STRING)                                                     │
│    createdAt (LONG)                                                      │
│    updatedAt (LONG)                                                      │
├──────────────────────────────────────────────────────────────────────────┤
│ No Foreign Keys                                                          │
│ No Indices                                                               │
└──────────────────────────────────────────────────────────────────────────┘


╔════════════════════════════════════════════════════════════════════════════╗
║                      EXPENSE TRACKER MODULE                                 ║
╚════════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────┐         ┌────────────────────────────────┐
│   expense_categories            │         │         expenses               │
├─────────────────────────────────┤         ├────────────────────────────────┤
│ 🔑 id (PK, LONG)                │◄────────│ 🔑 id (PK, LONG)              │
│    name (STRING)                │    │    │ 🔗 categoryId (FK, LONG)      │
│    icon (STRING)                │    │    │    amount (DOUBLE)            │
│    color (LONG)                 │    │    │    description (STRING)       │
│    budget (DOUBLE)              │    │    │    date (STRING)              │
│    isActive (BOOLEAN)           │    │    │    timestamp (LONG)           │
│    createdAt (LONG)             │    │    │    paymentMethod (STRING)     │
└─────────────────────────────────┘    │    │    createdAt (LONG)           │
                                       │    ├────────────────────────────────┤
                                       └────│ 🔗 Foreign Key:               │
                                            │    categoryId references      │
                                            │    expense_categories.id      │
                                            └────────────────────────────────┘


╔════════════════════════════════════════════════════════════════════════════╗
║                    SUBSCRIPTION TRACKER MODULE                              ║
╚════════════════════════════════════════════════════════════════════════════╝

┌──────────────────────────────────────────────────────────────────────────┐
│                        subscriptions                                      │
├──────────────────────────────────────────────────────────────────────────┤
│ 🔑 id (PK, LONG)                                                         │
│    name (STRING)                                                         │
│    cost (DOUBLE)                                                         │
│    currency (STRING)                                                     │
│    billingCycle (STRING: Weekly/Monthly/Quarterly/Yearly)                │
│    startDate (STRING: yyyy-MM-dd)                                        │
│    nextBillingDate (STRING: yyyy-MM-dd)                                  │
│    category (STRING)                                                     │
│    icon (STRING)                                                         │
│    color (LONG)                                                          │
│    paymentMethod (STRING)                                                │
│    website (STRING)                                                      │
│    notes (STRING)                                                        │
│    isActive (BOOLEAN)                                                    │
│    isTrial (BOOLEAN)                                                     │
│    trialEndDate (STRING?)                                                │
│    reminderDaysBefore (INT)                                              │
├──────────────────────────────────────────────────────────────────────────┤
│ No Foreign Keys                                                          │
│ No Indices                                                               │
└──────────────────────────────────────────────────────────────────────────┘


╔════════════════════════════════════════════════════════════════════════════╗
║                       DAILY PLANNER MODULE                                  ║
╚════════════════════════════════════════════════════════════════════════════╝

┌──────────────────────────────────────────────────────────────────────────┐
│                        daily_tasks                                        │
├──────────────────────────────────────────────────────────────────────────┤
│ 🔑 id (PK, LONG)                                                         │
│    title (STRING)                                                        │
│    description (STRING)                                                  │
│    date (STRING: yyyy-MM-dd)                                             │
│    startTime (STRING: HH:mm)                                             │
│    endTime (STRING: HH:mm)                                               │
│    taskType (STRING: TASK/EVENT/BREAK/FOCUS/ROUTINE)                     │
│    category (STRING)                                                     │
│    priority (STRING: Low/Medium/High)                                    │
│    icon (STRING)                                                         │
│    color (LONG)                                                          │
│    isCompleted (BOOLEAN)                                                 │
│    isRecurring (BOOLEAN)                                                 │
│    recurringDays (STRING)                                                │
│    reminderMinutes (INT)                                                 │
│    notes (STRING)                                                        │
│    createdAt (LONG)                                                      │
├──────────────────────────────────────────────────────────────────────────┤
│ No Foreign Keys                                                          │
│ No Indices                                                               │
└──────────────────────────────────────────────────────────────────────────┘


╔════════════════════════════════════════════════════════════════════════════╗
║                         TIMER PRESETS MODULE                                ║
╚════════════════════════════════════════════════════════════════════════════╝

┌──────────────────────────────────────────────────────────────────────────┐
│                          presets                                          │
├──────────────────────────────────────────────────────────────────────────┤
│ 🔑 id (PK, LONG)                                                         │
│    name (STRING)                                                         │
│    hours (INT)                                                           │
│    minutes (INT)                                                         │
│    createdAt (LONG)                                                      │
├──────────────────────────────────────────────────────────────────────────┤
│ No Foreign Keys                                                          │
│ No Indices                                                               │
└──────────────────────────────────────────────────────────────────────────┘


═══════════════════════════════════════════════════════════════════════════
                        FOREIGN KEY RELATIONSHIPS
═══════════════════════════════════════════════════════════════════════════

1. time_sessions.categoryId → activity_categories.id
   ├─ Type: FOREIGN KEY with CASCADE DELETE
   ├─ Indexed: YES
   └─ Purpose: Links focus sessions to their categories

2. habit_completions.habitId → habits.id
   ├─ Type: COMPOSITE PRIMARY KEY (habitId, date)
   ├─ Indexed: Implicit (primary key)
   └─ Purpose: Tracks daily habit completions

3. expenses.categoryId → expense_categories.id
   ├─ Type: FOREIGN KEY (inferred from structure)
   ├─ Indexed: Recommended
   └─ Purpose: Links expenses to their categories


═══════════════════════════════════════════════════════════════════════════
                            TABLE STATISTICS
═══════════════════════════════════════════════════════════════════════════

╔════════════════════╤═══════════════╤═══════════╤════════════╤═════════════╗
║ Table Name         │ Total Fields  │ Has FK?   │ Indices    │ Module      ║
╠════════════════════╪═══════════════╪═══════════╪════════════╪═════════════╣
║ activity_categories│       9       │    No     │    None    │ Focus       ║
║ time_sessions      │       8       │    Yes    │     2      │ Focus       ║
║ habits             │      11       │    No     │    None    │ Habit       ║
║ habit_completions  │       5       │    Yes*   │   Impl.    │ Habit       ║
║ date_calculations  │       7       │    No     │    None    │ Year Calc   ║
║ bmi_calculations   │      10       │    No     │    None    │ BMI Calc    ║
║ expense_categories │       6       │    No     │    None    │ Expense     ║
║ expenses           │       8       │    Yes    │   Rec.**   │ Expense     ║
║ subscriptions      │      16       │    No     │    None    │ Subscription║
║ daily_tasks        │      16       │    No     │    None    │ Daily Plan  ║
║ presets            │       4       │    No     │    None    │ Timer       ║
╚════════════════════╧═══════════════╧═══════════╧════════════╧═════════════╝

* Composite PK acts as FK reference
** Recommended but not explicitly defined


═══════════════════════════════════════════════════════════════════════════
                           DATA RELATIONSHIPS
═══════════════════════════════════════════════════════════════════════════

📊 PARENT-CHILD RELATIONSHIPS:

activity_categories (Parent)
    └─→ time_sessions (Child)
        ├─ Relationship: 1:N (One category, many sessions)
        └─ Delete Behavior: CASCADE (deleting category deletes all sessions)

habits (Parent)
    └─→ habit_completions (Child)
        ├─ Relationship: 1:N (One habit, many daily completions)
        └─ Delete Behavior: Not specified (likely CASCADE)

expense_categories (Parent)
    └─→ expenses (Child)
        ├─ Relationship: 1:N (One category, many expenses)
        └─ Delete Behavior: Not explicitly defined


═══════════════════════════════════════════════════════════════════════════
                          DATABASE FEATURES
═══════════════════════════════════════════════════════════════════════════

✅ Uses Room Persistence Library
✅ Fallback to Destructive Migration (initial release)
✅ Singleton Pattern (INSTANCE management)
✅ Flow-based Reactive Queries
✅ Coroutine-based Async Operations
✅ Indexed Foreign Keys for Performance
✅ Cascade Delete for Data Integrity
✅ Composite Primary Keys
✅ Auto-generated Primary Keys
✅ Timestamp Tracking (createdAt, updatedAt)


═══════════════════════════════════════════════════════════════════════════
                        MODULE DEPENDENCIES
═══════════════════════════════════════════════════════════════════════════

Focus Tracker
    └─ Requires: activity_categories, time_sessions

Habit Tracker
    └─ Requires: habits, habit_completions

Year Calculator
    └─ Requires: date_calculations

BMI Calculator
    └─ Requires: bmi_calculations

Expense Tracker
    └─ Requires: expense_categories, expenses

Subscription Tracker
    └─ Requires: subscriptions

Daily Planner
    └─ Requires: daily_tasks

Timer Presets
    └─ Requires: presets


═══════════════════════════════════════════════════════════════════════════
                       DESIGN RECOMMENDATIONS
═══════════════════════════════════════════════════════════════════════════

⚠️  RECOMMENDED IMPROVEMENTS:

1. Add Foreign Key for expenses.categoryId
   └─ Currently inferred but not explicitly defined

2. Add Indices for Performance
   ├─ expenses.date (frequently queried)
   ├─ daily_tasks.date (frequently queried)
   ├─ subscriptions.nextBillingDate (for sorting)
   └─ habit_completions.date (for date range queries)

3. Add Foreign Key Constraints
   └─ expenses.categoryId → expense_categories.id with CASCADE

4. Consider Migration Strategy
   └─ Currently using fallbackToDestructiveMigration()
   └─ Implement proper migrations for production

5. Add Composite Indices
   └─ time_sessions (date, categoryId) for analytics
   └─ expenses (date, categoryId) for filtering

