package com.example.caloriescounter.data

import kotlinx.coroutines.flow.Flow

class MealRepository(private val mealDao: MealDao) {
    val allMeals: Flow<List<MealRecord>> = mealDao.getAllMeals()

    suspend fun insertMeal(meal: MealRecord): Long {
        return mealDao.insertMeal(meal)
    }

    suspend fun saveHistoryChange(meal: MealRecord, attemptSync: Boolean): MealRecord =
        mealDao.saveHistoryChange(meal, attemptSync)

    suspend fun syncSavedMeal(meal: MealRecord, healthConnectManager: HealthConnectManager): Boolean {
        // The local transaction reserves the version before sending it to Health Connect.
        val result = healthConnectManager.writeMeal(meal.copy(
            healthConnectClientRecordVersion = meal.healthConnectClientRecordVersion - 1
        ))
        recordSyncResult(meal, result)
        return result.success
    }

    internal suspend fun recordSyncResult(meal: MealRecord, result: HealthConnectWriteResult) {
        mealDao.recordSyncResult(
            meal.id, meal.healthConnectClientRecordId, meal.healthConnectClientRecordVersion,
            result.success, if (result.success) null else "Saved on this device."
        )
    }

    suspend fun deleteMeal(meal: MealRecord) {
        mealDao.deleteMeal(meal)
    }

    suspend fun deleteMealById(id: Int) {
        mealDao.deleteMealById(id)
    }
}
