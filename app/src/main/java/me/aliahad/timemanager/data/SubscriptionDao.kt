package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Insert
    suspend fun insertSubscription(subscription: Subscription): Long
    
    @Update
    suspend fun updateSubscription(subscription: Subscription)
    
    @Delete
    suspend fun deleteSubscription(subscription: Subscription)
    
    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDate ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY nextBillingDate ASC")
    fun getActiveSubscriptions(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE isActive = 0 ORDER BY name ASC")
    fun getInactiveSubscriptions(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE category = :category AND isActive = 1")
    fun getSubscriptionsByCategory(category: String): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE isTrial = 1 AND isActive = 1")
    fun getTrialSubscriptions(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE nextBillingDate BETWEEN :startDate AND :endDate AND isActive = 1")
    fun getSubscriptionsInDateRange(startDate: String, endDate: String): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: Long): Subscription?
    
    @Query("SELECT SUM(cost) FROM subscriptions WHERE isActive = 1 AND billingCycle = 'Monthly'")
    suspend fun getTotalMonthlyCost(): Double?
    
    @Query("SELECT SUM(cost) FROM subscriptions WHERE isActive = 1 AND billingCycle = 'Yearly'")
    suspend fun getTotalYearlyCost(): Double?
    
    @Query("SELECT COUNT(*) FROM subscriptions WHERE isActive = 1")
    suspend fun getActiveSubscriptionCount(): Int
    
    @Query("SELECT DISTINCT category FROM subscriptions WHERE isActive = 1")
    suspend fun getAllCategories(): List<String>
}

