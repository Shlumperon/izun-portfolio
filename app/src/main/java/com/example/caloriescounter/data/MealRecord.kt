package com.example.caloriescounter.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mealType: String, // breakfast, lunch, dinner, snack
    val timestamp: Long, // start time, epoch milliseconds
    @ColumnInfo(defaultValue = "0")
    val endTimestamp: Long = 0, // end time, epoch milliseconds; 0 means one-minute fallback
    val calories: Double, // Health Connect energy, kcal
    val protein: Double? = null, // grams
    val carbs: Double? = null, // grams
    val fat: Double? = null, // grams
    val fiber: Double? = null, // grams
    val sugar: Double? = null, // grams
    val saturatedFat: Double? = null, // grams
    val unsaturatedFat: Double? = null, // grams
    val transFat: Double? = null, // grams
    val sodium: Double? = null, // milligrams
    val potassium: Double? = null, // milligrams
    val cholesterol: Double? = null, // milligrams
    val calcium: Double? = null, // milligrams
    val iron: Double? = null, // milligrams
    val magnesium: Double? = null, // milligrams
    val zinc: Double? = null, // milligrams
    val vitaminA: Double? = null, // micrograms
    val vitaminC: Double? = null, // milligrams
    val vitaminD: Double? = null, // micrograms
    val vitaminE: Double? = null, // milligrams
    val caffeine: Double? = null, // milligrams
    @ColumnInfo(defaultValue = "0")
    val healthConnectSynced: Boolean = false,
    val lastSyncError: String? = null,
    @ColumnInfo(defaultValue = "")
    val healthConnectClientRecordId: String = "",
    @ColumnInfo(defaultValue = "0")
    val healthConnectClientRecordVersion: Long = 0,
    val rawJson: String = "" // Retained as an empty column for database compatibility.
)

internal fun MealRecord.newMealOccurrence(now: Long = System.currentTimeMillis()): MealRecord =
    copy(
        id = 0,
        timestamp = now,
        endTimestamp = now + 60_000,
        healthConnectSynced = false,
        lastSyncError = null,
        healthConnectClientRecordId = "calories-counter-${java.util.UUID.randomUUID()}",
        healthConnectClientRecordVersion = 0
    )
