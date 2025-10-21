package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityCategoryDao {
    @Query("SELECT * FROM activity_categories WHERE isActive = 1 ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllActiveCategories(): Flow<List<ActivityCategory>>
    
    @Query("SELECT * FROM activity_categories ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllCategories(): Flow<List<ActivityCategory>>
    
    @Query("SELECT * FROM activity_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): ActivityCategory?
    
    @Query("SELECT * FROM activity_categories WHERE id = :id")
    fun getCategoryByIdFlow(id: Long): Flow<ActivityCategory?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ActivityCategory): Long
    
    @Update
    suspend fun updateCategory(category: ActivityCategory)
    
    @Delete
    suspend fun deleteCategory(category: ActivityCategory)
    
    @Query("UPDATE activity_categories SET isActive = 0 WHERE id = :id")
    suspend fun archiveCategory(id: Long)
    
    @Query("SELECT COUNT(*) FROM activity_categories WHERE isActive = 1")
    suspend fun getActiveCategoryCount(): Int
    
    @Query("UPDATE activity_categories SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
    
    @Query("SELECT * FROM activity_categories ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllCategoriesSync(): List<ActivityCategory>
}

