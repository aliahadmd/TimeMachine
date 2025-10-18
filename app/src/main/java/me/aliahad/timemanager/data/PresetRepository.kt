package me.aliahad.timemanager.data

import kotlinx.coroutines.flow.Flow

class PresetRepository(private val presetDao: PresetDao) {
    
    val allPresets: Flow<List<Preset>> = presetDao.getAllPresets()
    
    suspend fun insertPreset(preset: Preset): Long {
        return presetDao.insertPreset(preset)
    }
    
    suspend fun deletePreset(preset: Preset) {
        presetDao.deletePreset(preset)
    }
    
    suspend fun deletePresetById(presetId: Long) {
        presetDao.deletePresetById(presetId)
    }
}

