package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY createdAt DESC")
    fun getAllPresets(): Flow<List<Preset>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Preset): Long
    
    @Delete
    suspend fun deletePreset(preset: Preset)
    
    @Query("DELETE FROM presets WHERE id = :presetId")
    suspend fun deletePresetById(presetId: Long)
}

