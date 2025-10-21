package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {
    @Insert
    suspend fun insertCategory(category: ExpenseCategory): Long
    
    @Update
    suspend fun updateCategory(category: ExpenseCategory)
    
    @Delete
    suspend fun deleteCategory(category: ExpenseCategory)
    
    @Query("SELECT * FROM expense_categories WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCategories(): Flow<List<ExpenseCategory>>
    
    @Query("SELECT * FROM expense_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<ExpenseCategory>>
    
    @Query("SELECT * FROM expense_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): ExpenseCategory?
    
    @Query("SELECT COUNT(*) FROM expense_categories")
    suspend fun getCategoryCount(): Int
}

