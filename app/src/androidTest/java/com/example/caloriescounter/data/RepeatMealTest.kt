package com.example.caloriescounter.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RepeatMealTest {
    @Test
    fun repeatedSaveKeepsOneOccurrenceAndIgnoresStaleSyncResults() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MealDatabase::class.java).build()
        try {
            val repository = MealRepository(database.mealDao())
            val source = MealRecord(name = "Soup", mealType = "lunch", timestamp = 1,
                endTimestamp = 900_001, calories = 150.0, rawJson = "")
            val original = repository.saveHistoryChange(source, attemptSync = true)
            val copy = original.newMealOccurrence(now = 1_000_000)
            assertNotEquals(original.healthConnectClientRecordId, copy.healthConnectClientRecordId)
            assertEquals(1_060_000L, copy.endTimestamp)
            val offline = repository.saveHistoryChange(copy, attemptSync = false)
            assertEquals(0L, offline.healthConnectClientRecordVersion)
            val pending = repository.saveHistoryChange(copy, attemptSync = true)
            assertEquals(offline.id, pending.id)
            val edited = repository.saveHistoryChange(pending.copy(calories = 220.0), attemptSync = true)
            assertEquals(2L, edited.healthConnectClientRecordVersion)
            repository.recordSyncResult(pending, HealthConnectWriteResult(true, ""))
            val meals = repository.allMeals.first()
            assertEquals(2, meals.size)
            assertEquals(edited, meals.single { it.id == edited.id })
            assertEquals(original, meals.single { it.id == original.id })
        } finally {
            database.close()
        }
    }
}
