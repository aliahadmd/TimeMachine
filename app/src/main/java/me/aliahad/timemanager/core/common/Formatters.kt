package me.aliahad.timemanager.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

data class CurrencySpec(
    val code: String,
    val symbol: String,
    val name: String
)

object CurrencyCatalog {
    const val DEFAULT_CODE = "BDT"
    const val DEFAULT_SYMBOL = "৳"

    val ALL: List<CurrencySpec> = listOf(
        CurrencySpec("BDT", "৳", "Bangladeshi Taka"),
        CurrencySpec("USD", "$", "US Dollar"),
        CurrencySpec("EUR", "€", "Euro"),
        CurrencySpec("GBP", "£", "British Pound"),
        CurrencySpec("CNY", "CN¥", "Chinese Yuan / RMB"),
        CurrencySpec("INR", "₹", "Indian Rupee"),
        CurrencySpec("JPY", "JP¥", "Japanese Yen"),
        CurrencySpec("KRW", "₩", "South Korean Won"),
        CurrencySpec("RUB", "₽", "Russian Ruble"),
        CurrencySpec("BRL", "R$", "Brazilian Real"),
        CurrencySpec("CAD", "C$", "Canadian Dollar"),
        CurrencySpec("AUD", "A$", "Australian Dollar"),
        CurrencySpec("CHF", "CHF", "Swiss Franc"),
        CurrencySpec("SEK", "kr", "Swedish Krona"),
        CurrencySpec("PLN", "zł", "Polish Zloty")
    )

    fun byCode(code: String?): CurrencySpec {
        return ALL.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ALL.first()
    }

    fun inferCodeFromSymbol(symbol: String?): String {
        if (symbol.isNullOrBlank()) return DEFAULT_CODE
        if (symbol == "¥") return "CNY"
        return ALL.firstOrNull { it.symbol == symbol }?.code
            ?: ALL.firstOrNull { it.code.equals(symbol, ignoreCase = true) }?.code
            ?: DEFAULT_CODE
    }

    fun symbolFor(codeOrSymbol: String?): String {
        if (codeOrSymbol.isNullOrBlank()) return DEFAULT_SYMBOL
        ALL.firstOrNull { it.code.equals(codeOrSymbol, ignoreCase = true) }?.let { return it.symbol }
        return codeOrSymbol
    }
}

object Formatters {

    fun formatCurrency(amount: Double, symbol: String = CurrencyCatalog.DEFAULT_SYMBOL): String {
        val prefix = if (amount < 0) "-" else ""
        return String.format(Locale.US, "%s%s%.2f", prefix, symbol, abs(amount))
    }

    fun formatCurrencyByCode(amount: Double, currencyCode: String): String {
        return formatCurrency(amount, CurrencyCatalog.symbolFor(currencyCode))
    }

    fun formatMinutes(minutes: Int): String {
        val clamped = minutes.coerceAtLeast(0)
        val hours = clamped / 60
        val mins = clamped % 60
        return when {
            hours == 0 -> "${mins}m"
            mins == 0 -> "${hours}h"
            else -> "${hours}h ${mins}m"
        }
    }

    fun formatSeconds(seconds: Int): String {
        val clamped = seconds.coerceAtLeast(0)
        val hours = clamped / 3600
        val minutes = (clamped % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${clamped}s"
        }
    }

    fun formatShortSeconds(seconds: Int): String {
        val clamped = seconds.coerceAtLeast(0)
        val hours = clamped / 3600
        val minutes = (clamped % 3600) / 60
        return when {
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${clamped}s"
        }
    }

    fun formatTimerClock(seconds: Int): String {
        val clamped = seconds.coerceAtLeast(0)
        val hours = clamped / 3600
        val minutes = (clamped % 3600) / 60
        val secs = clamped % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, secs)
        }
    }

    fun formatHourLabel(hour: Int): String {
        return String.format(Locale.US, "%02d:00", hour.coerceIn(0, 23))
    }

    fun formatOneDecimal(value: Float): String {
        return String.format(Locale.US, "%.1f", value)
    }

    fun formatTwoDecimals(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }
}

object SafeDateTime {
    val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    val TIME_24H: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    val DISPLAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    val DISPLAY_DATE_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val DISPLAY_DATE_FULL: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val DISPLAY_DATE_WEEKDAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    fun parseLocalDateOrNull(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value.trim(), ISO_DATE)
        } catch (_: Exception) {
            null
        }
    }

    fun parseLocalTimeOrNull(value: String?): LocalTime? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalTime.parse(value.trim(), TIME_24H)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Material 3 DatePicker uses UTC midnight epoch millis.
     * Convert strictly in ZoneOffset.UTC to avoid off-by-one-day shifts in non-UTC timezones.
     */
    fun utcPickerMillisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    }

    fun localDateToUtcPickerMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    fun epochMillisToLocalDate(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
    }

    fun localDateToEpochMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun formatRelativeDate(dateString: String, today: LocalDate = LocalDate.now()): String {
        val date = parseLocalDateOrNull(dateString) ?: return dateString
        return when {
            date.isEqual(today) -> "Today"
            date.isEqual(today.plusDays(1)) -> "Tomorrow"
            date.isEqual(today.minusDays(1)) -> "Yesterday"
            date.year == today.year -> date.format(DISPLAY_DATE_SHORT)
            else -> date.format(DISPLAY_DATE_FULL)
        }
    }

    fun formatEpochTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return try {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId).format(DISPLAY_TIME)
        } catch (_: Exception) {
            "--"
        }
    }
}
