package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert
    suspend fun insertProfile(profile: UserProfile): Long
    
    @Update
    suspend fun updateProfile(profile: UserProfile)
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getProfile(): Flow<UserProfile?>
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfileSync(): UserProfile?
    
    @Query("SELECT COUNT(*) FROM user_profile")
    suspend fun getProfileCount(): Int
    
    @Query("DELETE FROM user_profile")
    suspend fun deleteAllProfiles()
}

