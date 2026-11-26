package com.example.caloriescounter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealRecord): Long

    @Query("SELECT * FROM meals WHERE healthConnectClientRecordId = :clientId LIMIT 1")
    suspend fun findByClientId(clientId: String): MealRecord?

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun findById(id: Int): MealRecord?

    @Transaction
    suspend fun saveHistoryChange(meal: MealRecord, attemptSync: Boolean): MealRecord {
        val stored = when {
            meal.id != 0 -> checkNotNull(findById(meal.id)) { "Meal no longer exists." }
            meal.healthConnectClientRecordId.isNotBlank() -> findByClientId(meal.healthConnectClientRecordId)
            else -> null
        }
        val previous = stored ?: meal
        val legacySynced = previous.healthConnectSynced && previous.healthConnectClientRecordId.isBlank()
        val local = if (legacySynced) {
            meal.copy(lastSyncError = "Changes saved on this device.")
        } else {
            val priorVersion = previous.healthConnectClientRecordVersion
            meal.copy(
                id = previous.id,
                healthConnectSynced = false,
                healthConnectClientRecordId = previous.healthConnectClientRecordId.ifBlank {
                    "calories-counter-${java.util.UUID.randomUUID()}"
                },
                healthConnectClientRecordVersion = if (attemptSync || priorVersion > 0) priorVersion + 1 else 0,
                lastSyncError = null
            )
        }
        return local.copy(id = insertMeal(local).toInt())
    }

    @Query("""
        UPDATE meals SET healthConnectSynced = :synced, lastSyncError = :error
        WHERE id = :id AND healthConnectClientRecordId = :clientId
        AND healthConnectClientRecordVersion = :version
    """)
    suspend fun recordSyncResult(id: Int, clientId: String, version: Long, synced: Boolean, error: String?)

    @Delete
    suspend fun deleteMeal(meal: MealRecord)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMealById(id: Int)
}
