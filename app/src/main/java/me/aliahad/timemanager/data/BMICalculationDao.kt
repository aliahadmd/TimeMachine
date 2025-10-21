package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BMICalculationDao {
    @Query("SELECT * FROM bmi_calculations ORDER BY updatedAt DESC")
    fun getAllCalculations(): Flow<List<BMICalculation>>
    
    @Query("SELECT * FROM bmi_calculations WHERE id = :id")
    suspend fun getCalculationById(id: Long): BMICalculation?
    
    @Query("SELECT * FROM bmi_calculations ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestCalculation(): BMICalculation?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calculation: BMICalculation): Long
    
    @Update
    suspend fun updateCalculation(calculation: BMICalculation)
    
    @Delete
    suspend fun deleteCalculation(calculation: BMICalculation)
    
    @Query("DELETE FROM bmi_calculations WHERE id = :id")
    suspend fun deleteCalculationById(id: Long)
    
    @Query("SELECT COUNT(*) FROM bmi_calculations")
    suspend fun getCalculationCount(): Int
    
    @Query("DELETE FROM bmi_calculations")
    suspend fun deleteAllCalculations()
    
    @Query("SELECT * FROM bmi_calculations ORDER BY updatedAt DESC")
    suspend fun getAllRecordsSync(): List<BMICalculation>
}

