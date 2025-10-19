package me.aliahad.timemanager

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Comprehensive date calculation result
 */
data class DateDifference(
    // Breakdown
    val years: Long,
    val months: Long,
    val days: Long,
    
    // Totals
    val totalYears: Double,
    val totalMonths: Long,
    val totalWeeks: Long,
    val totalDays: Long,
    val totalHours: Long,
    val totalMinutes: Long,
    val totalSeconds: Long,
    
    // Additional info
    val isNegative: Boolean,
    val nextOccurrence: LocalDate?,
    val daysUntilNext: Long?
)

/**
 * Western zodiac signs
 */
enum class WesternZodiac(val displayName: String, val symbol: String, val startMonth: Int, val startDay: Int, val endMonth: Int, val endDay: Int) {
    ARIES("Aries", "♈", 3, 21, 4, 19),
    TAURUS("Taurus", "♉", 4, 20, 5, 20),
    GEMINI("Gemini", "♊", 5, 21, 6, 20),
    CANCER("Cancer", "♋", 6, 21, 7, 22),
    LEO("Leo", "♌", 7, 23, 8, 22),
    VIRGO("Virgo", "♍", 8, 23, 9, 22),
    LIBRA("Libra", "♎", 9, 23, 10, 22),
    SCORPIO("Scorpio", "♏", 10, 23, 11, 21),
    SAGITTARIUS("Sagittarius", "♐", 11, 22, 12, 21),
    CAPRICORN("Capricorn", "♑", 12, 22, 1, 19),
    AQUARIUS("Aquarius", "♒", 1, 20, 2, 18),
    PISCES("Pisces", "♓", 2, 19, 3, 20);
    
    companion object {
        fun fromDate(date: LocalDate): WesternZodiac {
            val month = date.monthValue
            val day = date.dayOfMonth
            
            return values().find { zodiac ->
                if (zodiac.startMonth == zodiac.endMonth) {
                    month == zodiac.startMonth && day >= zodiac.startDay && day <= zodiac.endDay
                } else {
                    (month == zodiac.startMonth && day >= zodiac.startDay) ||
                    (month == zodiac.endMonth && day <= zodiac.endDay)
                }
            } ?: CAPRICORN
        }
    }
}

/**
 * Chinese zodiac animals
 */
enum class ChineseZodiac(val displayName: String, val symbol: String, val element: String, val traits: String) {
    RAT("Rat", "🐀", "Water", "Quick-witted, resourceful, versatile"),
    OX("Ox", "🐂", "Earth", "Diligent, dependable, strong"),
    TIGER("Tiger", "🐅", "Wood", "Brave, confident, competitive"),
    RABBIT("Rabbit", "🐇", "Wood", "Quiet, elegant, kind"),
    DRAGON("Dragon", "🐉", "Earth", "Confident, intelligent, enthusiastic"),
    SNAKE("Snake", "🐍", "Fire", "Enigmatic, intuitive, wise"),
    HORSE("Horse", "🐴", "Fire", "Animated, active, energetic"),
    GOAT("Goat", "🐐", "Earth", "Calm, gentle, sympathetic"),
    MONKEY("Monkey", "🐵", "Metal", "Sharp, smart, curious"),
    ROOSTER("Rooster", "🐓", "Metal", "Observant, hardworking, courageous"),
    DOG("Dog", "🐕", "Earth", "Lovely, honest, loyal"),
    PIG("Pig", "🐖", "Water", "Compassionate, generous, diligent");
    
    companion object {
        fun fromYear(year: Int): ChineseZodiac {
            // Chinese zodiac repeats every 12 years
            // 1900 is Year of the Rat (index 0)
            val index = (year - 1900) % 12
            return values()[index]
        }
    }
}

/**
 * Calculate comprehensive date difference between two dates
 */
fun calculateDateDifference(startDate: LocalDate, endDate: LocalDate): DateDifference {
    val isNegative = startDate.isAfter(endDate)
    val earlier = if (isNegative) endDate else startDate
    val later = if (isNegative) startDate else endDate
    
    // Calculate breakdown (years, months, days)
    val period = Period.between(earlier, later)
    val years = period.years.toLong()
    val months = period.months.toLong()
    val days = period.days.toLong()
    
    // Calculate totals
    val totalDays = ChronoUnit.DAYS.between(earlier, later)
    val totalWeeks = totalDays / 7
    val totalMonths = ChronoUnit.MONTHS.between(earlier, later)
    val totalYears = totalDays / 365.25
    val totalHours = totalDays * 24
    val totalMinutes = totalHours * 60
    val totalSeconds = totalMinutes * 60
    
    // Calculate next occurrence (for recurring dates like birthdays)
    val nextOccurrence = calculateNextOccurrence(startDate, endDate)
    val daysUntilNext = nextOccurrence?.let { ChronoUnit.DAYS.between(endDate, it) }
    
    return DateDifference(
        years = years,
        months = months,
        days = days,
        totalYears = totalYears,
        totalMonths = totalMonths,
        totalWeeks = totalWeeks,
        totalDays = totalDays,
        totalHours = totalHours,
        totalMinutes = totalMinutes,
        totalSeconds = totalSeconds,
        isNegative = isNegative,
        nextOccurrence = nextOccurrence,
        daysUntilNext = daysUntilNext
    )
}

/**
 * Calculate next occurrence of a recurring date (like birthday)
 */
fun calculateNextOccurrence(startDate: LocalDate, currentDate: LocalDate): LocalDate {
    val currentYear = currentDate.year
    val monthDay = MonthDay.of(startDate.monthValue, startDate.dayOfMonth)
    
    var nextDate = monthDay.atYear(currentYear)
    
    // If this year's occurrence has passed, get next year's
    if (nextDate.isBefore(currentDate) || nextDate.isEqual(currentDate)) {
        nextDate = monthDay.atYear(currentYear + 1)
    }
    
    return nextDate
}

/**
 * Format date in a readable way
 */
fun LocalDate.toReadableString(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    return this.format(formatter)
}

/**
 * Format date as short string
 */
fun LocalDate.toShortString(): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    return this.format(formatter)
}

/**
 * Get age milestones
 */
fun getAgeMilestones(years: Long): List<String> {
    val milestones = mutableListOf<String>()
    
    when {
        years == 18L -> milestones.add("🎓 Legal adult")
        years == 21L -> milestones.add("🍾 21st birthday")
        years == 25L -> milestones.add("🌟 Quarter century")
        years == 30L -> milestones.add("🎂 30th birthday")
        years == 40L -> milestones.add("💫 40 and fabulous")
        years == 50L -> milestones.add("👑 Half century")
        years == 60L -> milestones.add("💎 Diamond jubilee")
        years == 75L -> milestones.add("💍 Platinum jubilee")
        years == 100L -> milestones.add("🎊 Century!")
        years % 10 == 0L && years > 0 -> milestones.add("🎉 ${years}th birthday")
    }
    
    return milestones
}

/**
 * Convert milliseconds timestamp to LocalDate
 */
fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

/**
 * Convert LocalDate to milliseconds timestamp
 */
fun LocalDate.toMillis(): Long {
    return this.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

