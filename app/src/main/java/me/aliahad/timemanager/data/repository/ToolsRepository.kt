package me.aliahad.timemanager.data.repository

import kotlinx.coroutines.flow.Flow
import me.aliahad.timemanager.data.BMICalculation
import me.aliahad.timemanager.data.BMICalculationDao
import me.aliahad.timemanager.data.DateCalculation
import me.aliahad.timemanager.data.DateCalculationDao
import me.aliahad.timemanager.data.UserProfile
import me.aliahad.timemanager.data.UserProfileDao

class ToolsRepository(
    private val dateCalculationDao: DateCalculationDao,
    private val bmiCalculationDao: BMICalculationDao,
    private val userProfileDao: UserProfileDao
) {
    val allDateCalculations: Flow<List<DateCalculation>> = dateCalculationDao.getAllCalculations()
    val allBmiCalculations: Flow<List<BMICalculation>> = bmiCalculationDao.getAllCalculations()
    val userProfile: Flow<UserProfile?> = userProfileDao.getProfile()

    suspend fun insertDateCalculation(item: DateCalculation): Long =
        dateCalculationDao.insertCalculation(item)

    suspend fun updateDateCalculation(item: DateCalculation) =
        dateCalculationDao.updateCalculation(item)

    suspend fun deleteDateCalculation(item: DateCalculation) =
        dateCalculationDao.deleteCalculation(item)

    suspend fun insertBmiCalculation(item: BMICalculation): Long =
        bmiCalculationDao.insertCalculation(item)

    suspend fun updateBmiCalculation(item: BMICalculation) =
        bmiCalculationDao.updateCalculation(item)

    suspend fun deleteBmiCalculation(item: BMICalculation) =
        bmiCalculationDao.deleteCalculation(item)

    suspend fun getOrCreateProfile(): UserProfile {
        val existing = userProfileDao.getProfileSync()
        if (existing != null) return existing
        val created = UserProfile()
        val id = userProfileDao.insertProfile(created)
        return created.copy(id = id)
    }

    suspend fun updateProfile(profile: UserProfile) {
        val count = userProfileDao.getProfileCount()
        if (count == 0) {
            userProfileDao.insertProfile(profile)
        } else {
            userProfileDao.updateProfile(profile)
        }
    }
}
