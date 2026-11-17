package me.aliahad.timemanager

import androidx.compose.ui.graphics.Color
import me.aliahad.timemanager.core.common.CurrencyCatalog
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.normalizeStoredColor
import me.aliahad.timemanager.core.common.toComposeColor
import me.aliahad.timemanager.core.common.toStorageLong
import me.aliahad.timemanager.core.datastore.UserPreferences
import me.aliahad.timemanager.data.BMIClassification
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.DGEClassification
import me.aliahad.timemanager.data.DailyTask
import me.aliahad.timemanager.data.Expense
import me.aliahad.timemanager.data.ExpenseCategory
import me.aliahad.timemanager.data.Gender
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitCompletion
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.data.HabitType
import me.aliahad.timemanager.data.Subscription
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CoreAndAnalyticsUnitTest {

    @Test
    fun colorStorage_recoversShiftedColorValuesAndStoresArgb32() {
        val sample = Color(0xFF4DABF7L)
        val stored32 = sample.toStorageLong()
        assertEquals(0xFF4DABF7L, stored32)

        val corrupted64 = sample.value.toLong()
        val repaired = normalizeStoredColor(corrupted64)
        assertEquals(0xFF4DABF7L, repaired)
        assertEquals(sample, repaired.toComposeColor())
    }

    @Test
    fun currencyCatalog_distinguishesCnyAndJpy() {
        val cny = CurrencyCatalog.byCode("CNY")
        val jpy = CurrencyCatalog.byCode("JPY")
        assertEquals("CN¥", cny.symbol)
        assertEquals("JP¥", jpy.symbol)
        assertEquals("CNY", CurrencyCatalog.inferCodeFromSymbol("¥"))
        assertEquals("JPY", CurrencyCatalog.inferCodeFromSymbol("JP¥"))
        assertEquals("$12.50", Formatters.formatCurrency(12.5, "$"))
    }

    @Test
    fun safeDateTime_utcPickerConversionPreservesDate() {
        val picked = LocalDate.of(2026, 2, 29.coerceAtMost(28))
        val millis = SafeDateTime.localDateToUtcPickerMillis(picked)
        assertEquals(picked, SafeDateTime.utcPickerMillisToLocalDate(millis))
    }

    @Test
    fun chineseZodiac_handlesPre1900YearsWithoutCrash() {
        assertEquals(ChineseZodiac.RAT, ChineseZodiac.fromYear(1900))
        val z1899 = ChineseZodiac.fromYear(1899)
        assertEquals(ChineseZodiac.PIG, z1899)
        val z1800 = ChineseZodiac.fromYear(1800)
        assertTrue(z1800.displayName.isNotEmpty())
    }

    @Test
    fun dateDifference_handlesLeapDayNextOccurrence() {
        val leapBirth = LocalDate.of(2000, 2, 29)
        val current = LocalDate.of(2025, 3, 1)
        val diff = calculateDateDifference(leapBirth, current)
        assertEquals(LocalDate.of(2028, 2, 29), diff.nextOccurrence)
        assertEquals(25L, diff.years)
    }

    @Test
    fun bmiUtils_imperialInchesCarryDgeFemaleAndCeilWeeks() {
        // 152.4 cm == 60.0 in == 5 ft 0 in (never 4 ft 12 in)
        val (ft, inches) = cmToFeetAndInches(152.39f)
        assertEquals(5, ft)
        assertEquals(0, inches)
        assertTrue(inches in 0..11)

        // DGE female band is 1.0 lower than male band
        val maleRange = DGEClassification.getIdealRange(30, Gender.MALE)
        val femaleRange = DGEClassification.getIdealRange(30, Gender.FEMALE)
        assertEquals(maleRange.minBMI - 1f, femaleRange.minBMI, 0.01f)
        assertEquals("Normal", getBMICategory(19.5f, 30, Gender.FEMALE, BMIClassification.DGE))

        assertFalse(validateBMIInputs(1, 170f, 65f).isValid)
        assertTrue(validateBMIInputs(30, 170f, 65f).isValid)

        val guidance = getTargetWeightGuidance(71f, 70f)
        assertTrue(guidance.contains("2 week"))
    }

    @Test
    fun streaks_handleDuplicatesFutureDatesAndClamping() {
        val today = LocalDate.of(2026, 11, 17)
        val dates = listOf(
            "2026-11-17",
            "2026-11-17",
            "2026-11-16",
            "2026-11-15",
            "bad-date",
            "2027-01-01"
        )
        val info = calculateStreaks(dates, totalDays = 2, today = today)
        assertEquals(3, info.currentStreak)
        assertEquals(3, info.longestStreak)
        assertEquals(3, info.totalActiveDays)
        assertEquals(100f, info.consistencyPercentage, 0.01f)
    }

    @Test
    fun dailyPlanner_handlesOvernightSpansAndOverlaps() {
        val task = DailyTask(
            id = 1L,
            title = "Night Shift",
            date = "2026-11-17",
            startTime = "22:30",
            endTime = "01:30",
            taskType = "FOCUS"
        )
        val lateNight = LocalDate.of(2026, 11, 17)
        val earlyNextMorning = LocalDate.of(2026, 11, 18)

        assertTrue(DailyPlannerUtils.isTaskNow(task, lateNight, LocalTime.of(23, 15)))
        assertTrue(DailyPlannerUtils.isTaskNow(task, earlyNextMorning, LocalTime.of(0, 45)))
        assertFalse(DailyPlannerUtils.isTaskNow(task, earlyNextMorning, LocalTime.of(2, 0)))

        val morningTask = DailyTask(
            id = 2L,
            title = "Morning Sync",
            date = "2026-11-18",
            startTime = "01:00",
            endTime = "02:00",
            taskType = "TASK"
        )
        assertTrue(DailyPlannerUtils.isTaskOverlapping(task, morningTask))
    }

    @Test
    fun subscriptionAndExpenseAnalytics_boundIterationsAndTrackMixedCurrencies() {
        val today = LocalDate.of(2026, 11, 17)
        val invalidDateNext = SubscriptionAnalytics.calculateNextBillingDate("bad-date", "Monthly", today)
        assertEquals("2026-11-17", invalidDateNext)

        val subs = listOf(
            Subscription(
                id = 1L, name = "A", cost = 10.0, currency = "$", currencyCode = "USD",
                billingCycle = "Monthly", startDate = "2026-01-01", nextBillingDate = "2026-11-10",
                category = "SaaS", icon = "💻", color = 0xFF000000L, isActive = true
            ),
            Subscription(
                id = 2L, name = "B", cost = 120.0, currency = "CN¥", currencyCode = "CNY",
                billingCycle = "Yearly", startDate = "2026-01-01", nextBillingDate = "2026-11-18",
                category = "Media", icon = "🎵", color = 0xFF000000L, isActive = true
            )
        )
        val stats = SubscriptionAnalytics.calculateStats(subs, today)
        assertTrue(stats.hasMixedCurrencies)
        assertEquals(2, stats.monthlyByCurrency.size)
        val calendar = SubscriptionAnalytics.getUpcomingRenewals(subs, 30, includeOverdue = true, today = today)
        assertEquals(2, calendar.size)
        assertTrue(calendar.first().isOverdue)

        val cat = ExpenseCategory(id = 1L, name = "Food", icon = "🍽️", color = 0xFF000000L, budget = 100.0)
        val expenses = listOf(
            Expense(id = 1L, categoryId = 1L, amount = 85.0, description = "Dinner", date = "2026-11-15")
        )
        val budgets = ExpenseAnalytics.calculateBudgetStatus(expenses, listOf(cat))
        assertEquals(1, budgets.size)
        assertEquals(85f, budgets.first().percentageUsed, 0.1f)
    }

    @Test
    fun screenTimeSplitter_splitsMidnightAndHoursAndDropsStaleSessions() {
        val zone = ZoneId.of("UTC")
        // 2026-11-17T23:55:00Z -> 2026-11-18T00:10:00Z (15m across midnight & hours 23 and 0)
        val start = LocalDate.of(2026, 11, 17).atTime(23, 55).atZone(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 11, 18).atTime(0, 10).atZone(zone).toInstant().toEpochMilli()

        val slices = ScreenTimeSegmentSplitter.splitSessionByDay(
            RawScreenSession(startMs = start, endMs = end, unlockCount = 2, appUsed = "Browser"),
            zoneId = zone
        )
        assertEquals(2, slices.size)
        assertEquals("2026-11-17", slices[0].date)
        assertEquals(300, slices[0].durationSeconds)
        assertEquals("2026-11-18", slices[1].date)
        assertEquals(600, slices[1].durationSeconds)

        val day1Hours = ScreenTimeSegmentSplitter.buildHourlyBuckets("2026-11-17", listOf(slices[0]), zone)
        assertEquals(1, day1Hours.size)
        assertEquals(23, day1Hours[0].hour)
        assertEquals(300, day1Hours[0].screenTimeSeconds)

        // > 12h phantom session must be dropped cleanly
        val staleEnd = start + (13 * 60 * 60 * 1000L)
        assertTrue(ScreenTimeSegmentSplitter.splitSessionByDay(RawScreenSession(start, staleEnd), zone).isEmpty())
    }

    @Test
    fun habitStats_preserveStreaksOverRestDaysAndHistoryBeyond90Days() {
        val today = LocalDate.of(2026, 11, 17) // Tuesday (ISO day 2)
        val habit = Habit(
            id = 1L,
            name = "Gym",
            color = 0xFF0A66C2L,
            type = HabitType.BUILD,
            goalPeriodDays = 30,
            isEveryday = false,
            scheduledDays = "1,3,5", // Mon, Wed, Fri
            createdAt = LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        // Old 5-day Mon/Wed/Fri/Mon/Wed streak in early 2026 + achieved yesterday (Mon Nov 16)
        val completions = listOf(
            HabitCompletion(1L, "2026-01-05", CompletionType.ACHIEVED), // Mon
            HabitCompletion(1L, "2026-01-07", CompletionType.ACHIEVED), // Wed
            HabitCompletion(1L, "2026-01-09", CompletionType.ACHIEVED), // Fri
            HabitCompletion(1L, "2026-01-12", CompletionType.ACHIEVED), // Mon
            HabitCompletion(1L, "2026-01-14", CompletionType.ACHIEVED), // Wed
            HabitCompletion(1L, "2026-11-16", CompletionType.ACHIEVED)  // Mon (yesterday)
        )
        val stats = HabitRepository.computeHabitStats(habit, completions, today = today, zoneId = ZoneId.of("UTC"))
        assertEquals(5, stats.longestStreak)
        assertEquals(1, stats.currentStreak)
        assertEquals(6, stats.totalAchieved)
    }

    @Test
    fun quietHours_handleOvernightAndSameDaySpans() {
        val overnight = UserPreferences(
            quietHoursEnabled = true,
            quietStartHour = 22, quietStartMinute = 0,
            quietEndHour = 7, quietEndMinute = 0
        )
        assertTrue(NotificationChannelRegistry.isQuietHoursNow(overnight, LocalTime.of(23, 30)))
        assertTrue(NotificationChannelRegistry.isQuietHoursNow(overnight, LocalTime.of(6, 59)))
        assertFalse(NotificationChannelRegistry.isQuietHoursNow(overnight, LocalTime.of(12, 0)))

        val daytime = UserPreferences(
            quietHoursEnabled = true,
            quietStartHour = 13, quietStartMinute = 0,
            quietEndHour = 15, quietEndMinute = 0
        )
        assertTrue(NotificationChannelRegistry.isQuietHoursNow(daytime, LocalTime.of(14, 0)))
        assertFalse(NotificationChannelRegistry.isQuietHoursNow(daytime, LocalTime.of(15, 1)))
    }
}
