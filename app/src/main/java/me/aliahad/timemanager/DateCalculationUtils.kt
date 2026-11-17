package me.aliahad.timemanager

import me.aliahad.timemanager.core.common.SafeDateTime
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class DateDifference(
    val years: Long,
    val months: Long,
    val days: Long,
    val totalYears: Double,
    val totalMonths: Long,
    val totalWeeks: Long,
    val totalDays: Long,
    val totalHours: Long,
    val totalMinutes: Long,
    val totalSeconds: Long,
    val isNegative: Boolean,
    val nextOccurrence: LocalDate?,
    val daysUntilNext: Long?
)

enum class WesternZodiac(
    val displayName: String,
    val symbol: String,
    val startMonth: Int,
    val startDay: Int,
    val endMonth: Int,
    val endDay: Int
) {
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

            return entries.find { zodiac ->
                if (zodiac.startMonth == zodiac.endMonth) {
                    month == zodiac.startMonth && day in zodiac.startDay..zodiac.endDay
                } else {
                    (month == zodiac.startMonth && day >= zodiac.startDay) ||
                        (month == zodiac.endMonth && day <= zodiac.endDay)
                }
            } ?: CAPRICORN
        }
    }
}

enum class ChineseZodiac(
    val displayName: String,
    val symbol: String,
    val element: String,
    val traits: String
) {
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
            // Safe floorMod handles years < 1900 without negative array indices
            val index = Math.floorMod(year - 1900, entries.size)
            return entries[index]
        }
    }
}

fun calculateDateDifference(startDate: LocalDate, endDate: LocalDate): DateDifference {
    val isNegative = startDate.isAfter(endDate)
    val earlier = if (isNegative) endDate else startDate
    val later = if (isNegative) startDate else endDate

    val period = Period.between(earlier, later)
    val years = period.years.toLong()
    val months = period.months.toLong()
    val days = period.days.toLong()

    val totalDays = ChronoUnit.DAYS.between(earlier, later)
    val totalWeeks = totalDays / 7
    val totalMonths = ChronoUnit.MONTHS.between(earlier, later)
    val totalYears = totalDays / 365.25
    val totalHours = totalDays * 24
    val totalMinutes = totalHours * 60
    val totalSeconds = totalMinutes * 60

    val nextOccurrence = calculateNextOccurrence(startDate, endDate)
    val daysUntilNext = ChronoUnit.DAYS.between(endDate, nextOccurrence)

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

fun calculateNextOccurrence(startDate: LocalDate, currentDate: LocalDate): LocalDate {
    val currentYear = currentDate.year
    val monthDay = MonthDay.of(startDate.monthValue, startDate.dayOfMonth)

    var nextDate = if (startDate.monthValue == 2 && startDate.dayOfMonth == 29) {
        var candidateYear = currentYear
        while (!Year.isLeap(candidateYear.toLong())) {
            candidateYear++
        }
        monthDay.atYear(candidateYear)
    } else {
        monthDay.atYear(currentYear)
    }

    if (nextDate.isBefore(currentDate) || nextDate.isEqual(currentDate)) {
        var candidateYear = nextDate.year + 1
        nextDate = if (startDate.monthValue == 2 && startDate.dayOfMonth == 29) {
            while (!Year.isLeap(candidateYear.toLong())) {
                candidateYear++
            }
            monthDay.atYear(candidateYear)
        } else {
            monthDay.atYear(candidateYear)
        }
    }

    return nextDate
}

fun LocalDate.toReadableString(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    return this.format(formatter)
}

fun LocalDate.toShortString(): String = this.format(SafeDateTime.DISPLAY_DATE_FULL)

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

fun Long.toLocalDate(): LocalDate = SafeDateTime.epochMillisToLocalDate(this)

fun LocalDate.toMillis(): Long = SafeDateTime.localDateToEpochMillis(this)
