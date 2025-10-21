package me.aliahad.timemanager

import me.aliahad.timemanager.data.Subscription
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object SubscriptionAnalytics {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    // Default categories for subscriptions
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
    
    // Calculate next billing date based on start date and billing cycle
    fun calculateNextBillingDate(startDate: String, billingCycle: String, currentDate: LocalDate = LocalDate.now()): String {
        val start = LocalDate.parse(startDate, dateFormatter)
        
        // If start date is in the future, next billing is the start date
        if (currentDate.isBefore(start)) {
            return startDate
        }
        
        // Calculate next billing from start date
        var nextBilling = start
        when (billingCycle) {
            "Weekly" -> {
                while (nextBilling.isBefore(currentDate) || nextBilling == currentDate) {
                    nextBilling = nextBilling.plusWeeks(1)
                }
            }
            "Monthly" -> {
                while (nextBilling.isBefore(currentDate) || nextBilling == currentDate) {
                    nextBilling = nextBilling.plusMonths(1)
                }
            }
            "Quarterly" -> {
                while (nextBilling.isBefore(currentDate) || nextBilling == currentDate) {
                    nextBilling = nextBilling.plusMonths(3)
                }
            }
            "Yearly" -> {
                while (nextBilling.isBefore(currentDate) || nextBilling == currentDate) {
                    nextBilling = nextBilling.plusYears(1)
                }
            }
            else -> {
                while (nextBilling.isBefore(currentDate) || nextBilling == currentDate) {
                    nextBilling = nextBilling.plusMonths(1)
                }
            }
        }
        
        return nextBilling.format(dateFormatter)
    }
    
    // Add additional calculation from any billing date
    fun calculateNextBillingFromDate(currentBillingDate: String, billingCycle: String): String {
        val current = LocalDate.parse(currentBillingDate, dateFormatter)
        val next = when (billingCycle) {
            "Weekly" -> current.plusWeeks(1)
            "Monthly" -> current.plusMonths(1)
            "Quarterly" -> current.plusMonths(3)
            "Yearly" -> current.plusYears(1)
            else -> current.plusMonths(1)
        }
        return next.format(dateFormatter)
    }
    
    // Calculate days until next billing
    fun daysUntilBilling(nextBillingDate: String): Long {
        val billing = LocalDate.parse(nextBillingDate, dateFormatter)
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, billing)
    }
    
    // Check if renewal is due soon (within reminder days)
    fun isRenewalDueSoon(subscription: Subscription): Boolean {
        val days = daysUntilBilling(subscription.nextBillingDate)
        return days in 0..subscription.reminderDaysBefore.toLong()
    }
    
    // Check if trial is ending soon (within 3 days)
    fun isTrialEndingSoon(subscription: Subscription): Boolean {
        if (!subscription.isTrial || subscription.trialEndDate == null) return false
        val days = daysUntilBilling(subscription.trialEndDate)
        return days in 0..3
    }
    
    // Convert any billing cycle to monthly equivalent
    fun convertToMonthlyEquivalent(cost: Double, billingCycle: String): Double {
        return when (billingCycle) {
            "Weekly" -> cost * 4.33 // Average weeks per month
            "Monthly" -> cost
            "Quarterly" -> cost / 3
            "Yearly" -> cost / 12
            else -> cost
        }
    }
    
    // Convert any billing cycle to yearly equivalent
    fun convertToYearlyEquivalent(cost: Double, billingCycle: String): Double {
        return when (billingCycle) {
            "Weekly" -> cost * 52
            "Monthly" -> cost * 12
            "Quarterly" -> cost * 4
            "Yearly" -> cost
            else -> cost * 12
        }
    }
    
    // Calculate total monthly cost from all subscriptions
    fun calculateTotalMonthlyCost(subscriptions: List<Subscription>): Double {
        return subscriptions
            .filter { it.isActive }
            .sumOf { convertToMonthlyEquivalent(it.cost, it.billingCycle) }
    }
    
    // Calculate total yearly cost
    fun calculateTotalYearlyCost(subscriptions: List<Subscription>): Double {
        return subscriptions
            .filter { it.isActive }
            .sumOf { convertToYearlyEquivalent(it.cost, it.billingCycle) }
    }
    
    // Group subscriptions by category
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
                    percentage = ((monthlyTotal / totalMonthly) * 100).toFloat()
                )
            }
            .sortedByDescending { it.monthlyTotal }
    }
    
    // Get upcoming renewals (next 30 days)
    data class UpcomingRenewal(
        val subscription: Subscription,
        val daysUntil: Long,
        val isUrgent: Boolean
    )
    
    fun getUpcomingRenewals(subscriptions: List<Subscription>, days: Int = 30): List<UpcomingRenewal> {
        val today = LocalDate.now()
        val endDate = today.plusDays(days.toLong())
        
        return subscriptions
            .filter { it.isActive }
            .map { subscription ->
                val daysUntil = daysUntilBilling(subscription.nextBillingDate)
                UpcomingRenewal(
                    subscription = subscription,
                    daysUntil = daysUntil,
                    isUrgent = daysUntil in 0..3
                )
            }
            .filter { it.daysUntil in 0..days.toLong() }
            .sortedBy { it.daysUntil }
    }
    
    // Statistics
    data class SubscriptionStats(
        val totalActive: Int,
        val totalInactive: Int,
        val totalMonthly: Double,
        val totalYearly: Double,
        val mostExpensive: Subscription?,
        val cheapest: Subscription?,
        val avgCostPerSub: Double,
        val upcomingRenewalsCount: Int
    )
    
    fun calculateStats(subscriptions: List<Subscription>): SubscriptionStats {
        val active = subscriptions.filter { it.isActive }
        val inactive = subscriptions.filter { !it.isActive }
        
        val monthlyEquivalents = active.map { convertToMonthlyEquivalent(it.cost, it.billingCycle) }
        val totalMonthly = monthlyEquivalents.sum()
        val totalYearly = calculateTotalYearlyCost(subscriptions)
        
        val mostExpensive = active.maxByOrNull { convertToMonthlyEquivalent(it.cost, it.billingCycle) }
        val cheapest = active.minByOrNull { convertToMonthlyEquivalent(it.cost, it.billingCycle) }
        
        val avgCost = if (active.isNotEmpty()) totalMonthly / active.size else 0.0
        
        val upcoming = getUpcomingRenewals(subscriptions, 7).size
        
        return SubscriptionStats(
            totalActive = active.size,
            totalInactive = inactive.size,
            totalMonthly = totalMonthly,
            totalYearly = totalYearly,
            mostExpensive = mostExpensive,
            cheapest = cheapest,
            avgCostPerSub = avgCost,
            upcomingRenewalsCount = upcoming
        )
    }
    
    // Format currency with smart negative handling
    fun formatCurrency(amount: Double, currency: String = "৳"): String {
        // Smart prefix for refunds/credits: "-$50.00" instead of "$-50.00"
        val prefix = if (amount < 0) "-" else ""
        return "$prefix$currency%.2f".format(kotlin.math.abs(amount))
    }
    
    // Format date for display
    fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString, dateFormatter)
            val today = LocalDate.now()
            
            when {
                date == today -> "Today"
                date == today.plusDays(1) -> "Tomorrow"
                date == today.minusDays(1) -> "Yesterday"
                date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM dd"))
                else -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            }
        } catch (e: Exception) {
            dateString
        }
    }
    
    // Get relative time description
    fun getRelativeTimeDescription(daysUntil: Long): String {
        return when {
            daysUntil < 0 -> "Overdue"
            daysUntil == 0L -> "Today"
            daysUntil == 1L -> "Tomorrow"
            daysUntil in 2..7 -> "In $daysUntil days"
            daysUntil in 8..14 -> "Next week"
            daysUntil in 15..30 -> "In ${daysUntil / 7} weeks"
            else -> "In ${daysUntil / 30} months"
        }
    }
    
    // Get billing cycle color (for UI differentiation)
    fun getBillingCycleColor(billingCycle: String): Long {
        return when (billingCycle) {
            "Weekly" -> 0xFF4DABF7 // Blue
            "Monthly" -> 0xFF51CF66 // Green
            "Quarterly" -> 0xFF9775FA // Purple
            "Yearly" -> 0xFFFF6B6B // Red
            else -> 0xFFADB5BD // Gray
        }
    }
    
    // Get today's date string
    fun getTodayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }
    
    // Validate date string
    fun isValidDate(dateString: String): Boolean {
        return try {
            LocalDate.parse(dateString, dateFormatter)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Get month name from date
    fun getMonthYear(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString, dateFormatter)
            date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        } catch (e: Exception) {
            dateString
        }
    }
    
    // Calculate savings if subscription is cancelled
    fun calculatePotentialSavings(subscription: Subscription, months: Int = 12): Double {
        return convertToMonthlyEquivalent(subscription.cost, subscription.billingCycle) * months
    }
}

