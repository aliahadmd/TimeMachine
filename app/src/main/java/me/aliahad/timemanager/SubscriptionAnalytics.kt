package me.aliahad.timemanager

import me.aliahad.timemanager.core.common.CurrencyCatalog
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.data.Subscription
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object SubscriptionAnalytics {

    val DEFAULT_CATEGORIES = listOf(
        "Entertainment" to "🎬",
        "Productivity" to "💼",
        "Cloud Storage" to "☁️",
        "Music & Audio" to "🎵",
        "Gaming" to "🎮",
        "Education" to "📚",
        "Health & Fitness" to "💪",
        "News & Magazines" to "📰",
        "Communication" to "💬",
        "Security" to "🔒",
        "Design & Creative" to "🎨",
        "Other" to "📦"
    )

    fun calculateNextBillingDate(
        startDate: String,
        billingCycle: String,
        currentDate: LocalDate = LocalDate.now()
    ): String {
        val start = SafeDateTime.parseLocalDateOrNull(startDate) ?: return currentDate.format(SafeDateTime.ISO_DATE)

        if (currentDate.isBefore(start)) {
            return startDate
        }

        var nextBilling = start
        val maxIterations = 5000 // Safety bound against pathological inputs
        var iterations = 0
        while (!nextBilling.isAfter(currentDate) && iterations < maxIterations) {
            nextBilling = advanceOneCycle(nextBilling, billingCycle)
            iterations++
        }
        return nextBilling.format(SafeDateTime.ISO_DATE)
    }

    fun calculateNextBillingFromDate(currentBillingDate: String, billingCycle: String): String {
        val current = SafeDateTime.parseLocalDateOrNull(currentBillingDate) ?: LocalDate.now()
        return advanceOneCycle(current, billingCycle).format(SafeDateTime.ISO_DATE)
    }

    private fun advanceOneCycle(date: LocalDate, billingCycle: String): LocalDate {
        return when (billingCycle) {
            "Weekly" -> date.plusWeeks(1)
            "Monthly" -> date.plusMonths(1)
            "Quarterly" -> date.plusMonths(3)
            "Yearly" -> date.plusYears(1)
            else -> date.plusMonths(1)
        }
    }

    fun daysUntilBilling(nextBillingDate: String, today: LocalDate = LocalDate.now()): Long {
        val billing = SafeDateTime.parseLocalDateOrNull(nextBillingDate) ?: return Long.MAX_VALUE
        return ChronoUnit.DAYS.between(today, billing)
    }

    fun isRenewalDueSoon(subscription: Subscription, today: LocalDate = LocalDate.now()): Boolean {
        val days = daysUntilBilling(subscription.nextBillingDate, today)
        return days in 0..subscription.reminderDaysBefore.toLong()
    }

    fun isTrialEndingSoon(subscription: Subscription, today: LocalDate = LocalDate.now()): Boolean {
        if (!subscription.isTrial || subscription.trialEndDate.isNullOrBlank()) return false
        val days = daysUntilBilling(subscription.trialEndDate, today)
        return days in 0..3
    }

    fun convertToMonthlyEquivalent(cost: Double, billingCycle: String): Double {
        return when (billingCycle) {
            "Weekly" -> cost * 4.33
            "Monthly" -> cost
            "Quarterly" -> cost / 3.0
            "Yearly" -> cost / 12.0
            else -> cost
        }
    }

    fun convertToYearlyEquivalent(cost: Double, billingCycle: String): Double {
        return when (billingCycle) {
            "Weekly" -> cost * 52.0
            "Monthly" -> cost * 12.0
            "Quarterly" -> cost * 4.0
            "Yearly" -> cost
            else -> cost * 12.0
        }
    }

    fun calculateTotalMonthlyCost(subscriptions: List<Subscription>): Double {
        return subscriptions
            .filter { it.isActive }
            .sumOf { convertToMonthlyEquivalent(it.cost, it.billingCycle) }
    }

    fun calculateTotalYearlyCost(subscriptions: List<Subscription>): Double {
        return subscriptions
            .filter { it.isActive }
            .sumOf { convertToYearlyEquivalent(it.cost, it.billingCycle) }
    }

    fun calculateMonthlyTotalsByCurrency(subscriptions: List<Subscription>): Map<String, Double> {
        return subscriptions
            .filter { it.isActive }
            .groupBy { it.currency.ifBlank { CurrencyCatalog.symbolFor(it.currencyCode) } }
            .mapValues { (_, subs) -> subs.sumOf { convertToMonthlyEquivalent(it.cost, it.billingCycle) } }
    }

    data class CategorySpending(
        val category: String,
        val icon: String,
        val count: Int,
        val monthlyTotal: Double,
        val yearlyTotal: Double,
        val percentage: Float
    )

    fun calculateCategorySpending(subscriptions: List<Subscription>): List<CategorySpending> {
        val activeSubscriptions = subscriptions.filter { it.isActive }
        val totalMonthly = calculateTotalMonthlyCost(activeSubscriptions)
        if (totalMonthly <= 0) return emptyList()

        return activeSubscriptions
            .groupBy { it.category }
            .map { (category, subs) ->
                val monthlyTotal = subs.sumOf { convertToMonthlyEquivalent(it.cost, it.billingCycle) }
                val yearlyTotal = subs.sumOf { convertToYearlyEquivalent(it.cost, it.billingCycle) }
                val icon = subs.firstOrNull()?.icon ?: "📦"

                CategorySpending(
                    category = category,
                    icon = icon,
                    count = subs.size,
                    monthlyTotal = monthlyTotal,
                    yearlyTotal = yearlyTotal,
                    percentage = ((monthlyTotal / totalMonthly) * 100.0).toFloat().coerceIn(0f, 100f)
                )
            }
            .sortedByDescending { it.monthlyTotal }
    }

    data class UpcomingRenewal(
        val subscription: Subscription,
        val daysUntil: Long,
        val isUrgent: Boolean,
        val isOverdue: Boolean = false
    )

    fun getUpcomingRenewals(
        subscriptions: List<Subscription>,
        days: Int = 30,
        includeOverdue: Boolean = true,
        today: LocalDate = LocalDate.now()
    ): List<UpcomingRenewal> {
        return subscriptions
            .filter { it.isActive }
            .mapNotNull { subscription ->
                val daysUntil = daysUntilBilling(subscription.nextBillingDate, today)
                if (daysUntil == Long.MAX_VALUE) return@mapNotNull null
                val isOverdue = daysUntil < 0
                val inWindow = if (includeOverdue) daysUntil <= days.toLong() else daysUntil in 0..days.toLong()
                if (!inWindow) return@mapNotNull null
                UpcomingRenewal(
                    subscription = subscription,
                    daysUntil = daysUntil,
                    isUrgent = daysUntil in 0..3 || isOverdue,
                    isOverdue = isOverdue
                )
            }
            .sortedBy { it.daysUntil }
    }

    data class SubscriptionStats(
        val totalActive: Int,
        val totalInactive: Int,
        val totalMonthly: Double,
        val totalYearly: Double,
        val mostExpensive: Subscription?,
        val cheapest: Subscription?,
        val avgCostPerSub: Double,
        val upcomingRenewalsCount: Int,
        val hasMixedCurrencies: Boolean,
        val monthlyByCurrency: Map<String, Double>
    )

    fun calculateStats(subscriptions: List<Subscription>, today: LocalDate = LocalDate.now()): SubscriptionStats {
        val active = subscriptions.filter { it.isActive }
        val inactive = subscriptions.filter { !it.isActive }

        val monthlyEquivalents = active.map { convertToMonthlyEquivalent(it.cost, it.billingCycle) }
        val totalMonthly = monthlyEquivalents.sum()
        val totalYearly = calculateTotalYearlyCost(subscriptions)

        val mostExpensive = active.maxByOrNull { convertToMonthlyEquivalent(it.cost, it.billingCycle) }
        val cheapest = active.minByOrNull { convertToMonthlyEquivalent(it.cost, it.billingCycle) }

        val avgCost = if (active.isNotEmpty()) totalMonthly / active.size else 0.0
        val upcoming = getUpcomingRenewals(subscriptions, 7, includeOverdue = true, today = today).size
        val byCurrency = calculateMonthlyTotalsByCurrency(active)

        return SubscriptionStats(
            totalActive = active.size,
            totalInactive = inactive.size,
            totalMonthly = totalMonthly,
            totalYearly = totalYearly,
            mostExpensive = mostExpensive,
            cheapest = cheapest,
            avgCostPerSub = avgCost,
            upcomingRenewalsCount = upcoming,
            hasMixedCurrencies = byCurrency.size > 1,
            monthlyByCurrency = byCurrency
        )
    }

    fun formatCurrency(amount: Double, currency: String = CurrencyCatalog.DEFAULT_SYMBOL): String {
        return Formatters.formatCurrency(amount, currency)
    }

    fun formatDateForDisplay(dateString: String): String = SafeDateTime.formatRelativeDate(dateString)

    fun getRelativeTimeDescription(daysUntil: Long): String {
        return when {
            daysUntil == Long.MAX_VALUE -> "Invalid date"
            daysUntil < 0 -> "Overdue by ${-daysUntil}d"
            daysUntil == 0L -> "Today"
            daysUntil == 1L -> "Tomorrow"
            daysUntil in 2..7 -> "In $daysUntil days"
            daysUntil in 8..14 -> "Next week"
            daysUntil in 15..30 -> "In ${daysUntil / 7} weeks"
            else -> "In ${daysUntil / 30} months"
        }
    }

    fun getBillingCycleColor(billingCycle: String): Long {
        return when (billingCycle) {
            "Weekly" -> 0xFF4DABF7
            "Monthly" -> 0xFF51CF66
            "Quarterly" -> 0xFF9775FA
            "Yearly" -> 0xFFFF6B6B
            else -> 0xFFADB5BD
        }
    }

    fun getTodayDateString(): String = LocalDate.now().format(SafeDateTime.ISO_DATE)

    fun isValidDate(dateString: String): Boolean = SafeDateTime.parseLocalDateOrNull(dateString) != null

    fun getMonthYear(dateString: String): String {
        val date = SafeDateTime.parseLocalDateOrNull(dateString) ?: return dateString
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    fun calculatePotentialSavings(subscription: Subscription, months: Int = 12): Double {
        return convertToMonthlyEquivalent(subscription.cost, subscription.billingCycle) * months
    }
}
