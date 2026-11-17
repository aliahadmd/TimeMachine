package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import me.aliahad.timemanager.core.common.CurrencyCatalog

@Entity(
    tableName = "subscriptions",
    indices = [
        Index(value = ["nextBillingDate"]),
        Index(value = ["isActive"]),
        Index(value = ["isActive", "nextBillingDate"]),
        Index(value = ["isTrial", "isActive"])
    ]
)
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cost: Double,
    val currency: String = CurrencyCatalog.DEFAULT_SYMBOL,
    val currencyCode: String = CurrencyCatalog.DEFAULT_CODE,
    val billingCycle: String, // Weekly, Monthly, Quarterly, Yearly
    val startDate: String, // yyyy-MM-dd
    val nextBillingDate: String, // yyyy-MM-dd
    val category: String,
    val icon: String,
    val color: Long,
    val paymentMethod: String = "Card",
    val website: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val isTrial: Boolean = false,
    val trialEndDate: String? = null, // yyyy-MM-dd
    val reminderDaysBefore: Int = 3,
    val expenseCategoryId: Long? = null,
    val autoLogExpense: Boolean = false
)
