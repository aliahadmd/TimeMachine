package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subscriptions",
    indices = [
        Index(value = ["nextBillingDate"]),  // For sorting by next billing
        Index(value = ["isActive"]),  // For filtering active subscriptions
        Index(value = ["isActive", "nextBillingDate"]),  // Composite for queries
        Index(value = ["isTrial", "isActive"])  // For trial subscription queries
    ]
)
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cost: Double,
    val currency: String = "৳",
    val billingCycle: String, // Monthly, Yearly, Quarterly, Weekly
    val startDate: String, // yyyy-MM-dd
    val nextBillingDate: String, // yyyy-MM-dd
    val category: String, // Entertainment, Productivity, Cloud Storage, etc.
    val icon: String, // Emoji or icon identifier
    val color: Long, // Color as Long
    val paymentMethod: String = "Card", // Card, PayPal, Bank Transfer, etc.
    val website: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val isTrial: Boolean = false,
    val trialEndDate: String? = null, // yyyy-MM-dd
    val reminderDaysBefore: Int = 3 // Days before renewal to remind
)

