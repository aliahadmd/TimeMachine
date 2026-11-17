package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date = :date ORDER BY timestamp DESC")
    fun getExpensesForDate(date: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getExpensesInDateRange(startDate: String, endDate: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date = :date")
    fun getTotalForDateFlow(date: String): Flow<Double>

    @Query("SELECT SUM(amount) FROM expenses WHERE date = :date")
    suspend fun getTotalForDate(date: String): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalForDateRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalForCategoryInRange(categoryId: Long, startDate: String, endDate: String): Double?

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getTotalExpenseCountSql(): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getExpenseCountInRangeSql(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(DISTINCT date) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getDistinctExpenseDaysInRangeSql(startDate: String, endDate: String): Int

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses")
    suspend fun getAllTimeTotalSql(): Double

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("DELETE FROM expenses WHERE date < :date")
    suspend fun deleteExpensesBefore(date: String)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesSync(): List<Expense>
}
