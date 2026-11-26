package com.example.caloriescounter.data

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class LegacyMealMigrationTest {
    @Test
    fun migratedSyncedMealReportsLocalRemovalOnly() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "migration-${UUID.randomUUID()}"
        val schema = instrumentation.context.assets
            .open("com.example.caloriescounter.data.MealDatabase/3.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val table = schema.getJSONObject("database").getJSONArray("entities").getJSONObject(0)
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
                db.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", "meals"))
                db.execSQL("INSERT INTO meals (name, mealType, timestamp, calories, rawJson, healthConnectSynced) VALUES ('Soup','lunch',1,120,'legacy payload',1)")
                db.version = 3
            }
            val database = Room.databaseBuilder(context, MealDatabase::class.java, name)
                .addMigrations(*MealDatabase.migrationsForTesting().toTypedArray()).build()
            try {
                val meal = database.mealDao().getAllMeals().first().single()
                assertEquals("Soup", meal.name)
                assertEquals("", meal.rawJson)
                val result = HealthConnectManager(context).deleteMeal(meal)
                assertEquals(true, result.success)
                assertEquals("Removed from IZUN. The older entry remains in Health Connect.", result.message)
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
        Unit
    }
}
