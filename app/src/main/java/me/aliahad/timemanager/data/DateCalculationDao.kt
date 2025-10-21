package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DateCalculationDao {
    @Query("SELECT * FROM date_calculations ORDER BY updatedAt DESC")
    fun getAllCalculations(): Flow<List<DateCalculation>>
    
    @Query("SELECT * FROM date_calculations WHERE category = :category ORDER BY updatedAt DESC")
    fun getCalculationsByCategory(category: String): Flow<List<DateCalculation>>
    
    @Query("SELECT * FROM date_calculations WHERE id = :id")
    suspend fun getCalculationById(id: Long): DateCalculation?
    
    @Query("SELECT DISTINCT category FROM date_calculations ORDER BY category")
    fun getAllCategories(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calculation: DateCalculation): Long
    
    @Update
    suspend fun updateCalculation(calculation: DateCalculation)
    
    @Delete
    suspend fun deleteCalculation(calculation: DateCalculation)
    
    @Query("DELETE FROM date_calculations WHERE id = :id")
    suspend fun deleteCalculationById(id: Long)
    
    @Query("SELECT COUNT(*) FROM date_calculations")
    suspend fun getCalculationCount(): Int
    
    @Query("SELECT * FROM date_calculations ORDER BY updatedAt DESC")
    suspend fun getAllCalculationsSync(): List<DateCalculation>
}

