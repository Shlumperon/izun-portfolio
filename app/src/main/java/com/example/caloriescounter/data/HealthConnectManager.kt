package com.example.caloriescounter.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

private const val ESTIMATED_WALKING_STEPS_PER_MINUTE = 100L
private const val ESTIMATED_KM_PER_STEP = 0.00072
private const val ESTIMATED_WEIGHT_KG = 80.0
private const val WALKING_KCAL_PER_KG_PER_KM = 0.5
private const val TAG = "IZUN_HEALTH"

private val PREFERRED_STEP_SOURCE_PACKAGE_PREFIXES = listOf(
    "com.sec.android.app.shealth",
    "com.google.android.apps.fitness",
    "com.android.healthconnect.phone"
)

data class HealthConnectWriteResult(
    val success: Boolean,
    val message: String,
    val clientRecordId: String = "",
    val clientRecordVersion: Long = 0
)

data class DailyHealthSummary(
    val steps: Long = 0,
    val distanceKm: Double? = null,
    val activeMinutes: Long = 0,
    val walkingCaloriesKcal: Double? = null,
    val activeMinutesEstimated: Boolean = false,
    val walkingCaloriesEstimated: Boolean = false,
    val hasReadPermission: Boolean = false,
    val errorMessage: String? = null
)

data class DailyStepTotal(
    val date: LocalDate,
    val steps: Long
)

private data class StepSourceTotal(
    val packageName: String,
    val steps: Long
)

private data class StepSummary(
    val steps: Long,
    val sourcePackageName: String?
)

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            null
        }
    }

    fun isSdkAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    suspend fun grantedPermissions(): Set<String> {
        val client = healthConnectClient ?: return emptySet()
        return try {
            client.permissionController.getGrantedPermissions()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptySet()
        }
    }

    val nutritionPermission = HealthPermission.getWritePermission(NutritionRecord::class)
    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)

    val activityPermissions = setOf(
        stepsPermission,
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    suspend fun hasWritePermission(): Boolean = nutritionPermission in grantedPermissions()

    private suspend fun hasStepReadPermission(): Boolean = stepsPermission in grantedPermissions()

    suspend fun readDailyHealthSummary(date: LocalDate = LocalDate.now()): DailyHealthSummary {
        val client = healthConnectClient
            ?: return DailyHealthSummary(errorMessage = "Health Connect client is not available.")
        if (!hasStepReadPermission()) {
            return DailyHealthSummary(errorMessage = "Health Connect step permission is required.")
        }

        val zoneId = ZoneId.systemDefault()
        val start = date.atStartOfDay(zoneId).toInstant()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        val range = TimeRangeFilter.between(start, end)

        return try {
            val stepSummary = readStepSummary(client, range)
            val steps = stepSummary.steps

            val dailyAggregation = runCatching {
                client.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            DistanceRecord.DISTANCE_TOTAL,
                            ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                        ),
                        timeRangeFilter = range,
                        dataOriginFilter = stepSummary.sourcePackageName
                            ?.let { setOf(DataOrigin(it)) }
                            ?: emptySet()
                    )
                )
            }.onFailure {
                Log.w(TAG, "dashboard aggregate unavailable; using step fallbacks")
            }.getOrNull()

            val healthDistanceKm = dailyAggregation?.get(DistanceRecord.DISTANCE_TOTAL)
                ?.inKilometers
                ?.takeIf { it > 0.0 }
            val stepEstimatedDistanceKm = steps * ESTIMATED_KM_PER_STEP
            val distanceKm =
                if (steps > 0 && (healthDistanceKm == null || healthDistanceKm < stepEstimatedDistanceKm * 0.5)) {
                    stepEstimatedDistanceKm
                } else {
                    healthDistanceKm
                }

            val exerciseSessions = runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = range
                    )
                ).records
            }.onFailure {
                Log.w(TAG, "exercise sessions unavailable; using step-based active minutes")
            }.getOrDefault(emptyList())

            val activeMinutes = exerciseSessions.sumOf { session ->
                Duration.between(session.startTime, session.endTime).toMinutes().coerceAtLeast(0)
            }

            val walkingSessions = exerciseSessions
                .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_WALKING }

            var walkingCaloriesTotal = 0.0
            for (session in walkingSessions) {
                walkingCaloriesTotal += runCatching {
                    client.readRecords(
                        ReadRecordsRequest(
                            recordType = ActiveCaloriesBurnedRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
                        )
                    ).records.sumOf { it.energy.inKilocalories }
                }.onFailure {
                    Log.w(TAG, "walking calories unavailable; using step estimate")
                }.getOrDefault(0.0)
            }
            val dailyActiveCalories = dailyAggregation?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                ?.inKilocalories
                ?: 0.0

            val estimatedActiveMinutes = estimateActiveMinutesFromSteps(steps)
            val estimatedWalkingCalories = estimateWalkingCaloriesFromSteps(
                steps = steps,
                distanceKm = distanceKm
            )
            val resolvedActiveMinutes = activeMinutes.takeIf { it > 0 } ?: estimatedActiveMinutes
            val walkingCalories = when {
                walkingCaloriesTotal > 0.0 -> walkingCaloriesTotal
                dailyActiveCalories > 0.0 -> dailyActiveCalories
                else -> estimatedWalkingCalories
            }.takeIf { it > 0.0 }

            DailyHealthSummary(
                steps = steps,
                distanceKm = distanceKm,
                activeMinutes = resolvedActiveMinutes,
                walkingCaloriesKcal = walkingCalories,
                activeMinutesEstimated = activeMinutes == 0L && resolvedActiveMinutes > 0,
                walkingCaloriesEstimated = walkingCaloriesTotal <= 0.0 && dailyActiveCalories <= 0.0 && walkingCalories != null,
                hasReadPermission = true
            )
        } catch (_: Exception) {
            Log.w(TAG, "Health Connect dashboard read failed")
            DailyHealthSummary(
                hasReadPermission = true,
                errorMessage = "Could not read Health Connect dashboard data."
            )
        }
    }

    suspend fun readWeeklyStepTotals(endDate: LocalDate = LocalDate.now()): List<DailyStepTotal> {
        val client = healthConnectClient ?: return emptyList()
        if (!hasStepReadPermission()) return emptyList()

        val zoneId = ZoneId.systemDefault()
        return (6L downTo 0L).map { daysAgo ->
            val date = endDate.minusDays(daysAgo)
            val start = date.atStartOfDay(zoneId).toInstant()
            val end = date.plusDays(1).atStartOfDay(zoneId).toInstant()
            val steps = runCatching {
                readStepSummary(client, TimeRangeFilter.between(start, end)).steps
            }.getOrDefault(0L)
            DailyStepTotal(date = date, steps = steps)
        }
    }

    private fun estimateActiveMinutesFromSteps(steps: Long): Long {
        if (steps <= 0) return 0
        return (steps / ESTIMATED_WALKING_STEPS_PER_MINUTE)
            .coerceAtLeast(1)
    }

    private suspend fun readStepSummary(
        client: HealthConnectClient,
        range: TimeRangeFilter
    ): StepSummary {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = range,
                pageSize = 5000
            )
        ).records
        val sourceTotals = records
            .groupBy { it.metadata.dataOrigin.packageName }
            .map { (packageName, sourceRecords) ->
                StepSourceTotal(packageName, sourceRecords.sumOf { it.count })
            }
            .sortedByDescending { it.steps }
        val selectedSource = selectStepSource(sourceTotals)
        return StepSummary(
            steps = selectedSource?.steps ?: 0L,
            sourcePackageName = selectedSource?.packageName
        )
    }

    private fun selectStepSource(sourceTotals: List<StepSourceTotal>): StepSourceTotal? {
        if (sourceTotals.isEmpty()) return null
        for (preferredPrefix in PREFERRED_STEP_SOURCE_PACKAGE_PREFIXES) {
            val preferredSource = sourceTotals.firstOrNull {
                it.packageName.startsWith(preferredPrefix) && it.steps > 0L
            }
            if (preferredSource != null) return preferredSource
        }
        return sourceTotals
            .filter { it.steps > 0L }
            .maxByOrNull { it.steps }
    }

    private fun estimateWalkingCaloriesFromSteps(
        steps: Long,
        distanceKm: Double?
    ): Double {
        if (steps <= 0) return 0.0
        val resolvedDistanceKm = distanceKm ?: steps * ESTIMATED_KM_PER_STEP
        return resolvedDistanceKm * ESTIMATED_WEIGHT_KG * WALKING_KCAL_PER_KG_PER_KM
    }

    suspend fun writeMeal(meal: MealRecord): HealthConnectWriteResult {
        val client = healthConnectClient
            ?: return HealthConnectWriteResult(false, "Health Connect client is not available.")

        val mappedMealType = when (meal.mealType.lowercase()) {
            "breakfast" -> MealType.MEAL_TYPE_BREAKFAST
            "lunch" -> MealType.MEAL_TYPE_LUNCH
            "dinner" -> MealType.MEAL_TYPE_DINNER
            "snack" -> MealType.MEAL_TYPE_SNACK
            "unknown" -> MealType.MEAL_TYPE_UNKNOWN
            else -> MealType.MEAL_TYPE_UNKNOWN
        }

        return try {
            val startTime = Instant.ofEpochMilli(meal.timestamp)
            val endTime =
                if (meal.endTimestamp > meal.timestamp) {
                    Instant.ofEpochMilli(meal.endTimestamp)
                } else {
                    startTime.plusSeconds(60)
                }
            val now = Instant.now()

            if (startTime.isAfter(now) || endTime.isAfter(now.plusSeconds(60))) {
                return HealthConnectWriteResult(false, "Health Connect rejected future meal times. Choose a current or past time.")
            }
            if (!endTime.isAfter(startTime)) {
                return HealthConnectWriteResult(false, "Meal end time must be after start time.")
            }

            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)
            val clientRecordId = meal.healthConnectClientRecordId.ifBlank { "calories-counter-${UUID.randomUUID()}" }
            val clientRecordVersion = (meal.healthConnectClientRecordVersion + 1).coerceAtLeast(1)
            val nutritionRecord = NutritionRecord(
                name = meal.name,
                mealType = mappedMealType,
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = zoneOffset,
                endZoneOffset = zoneOffset,
                metadata = Metadata.manualEntry(clientRecordId, clientRecordVersion),
                energy = Energy.kilocalories(meal.calories),
                protein = Mass.grams(meal.protein ?: 0.0),
                totalCarbohydrate = Mass.grams(meal.carbs ?: 0.0),
                totalFat = Mass.grams(meal.fat ?: 0.0),
                dietaryFiber = meal.fiber?.let { Mass.grams(it) },
                sugar = meal.sugar?.let { Mass.grams(it) },
                saturatedFat = meal.saturatedFat?.let { Mass.grams(it) },
                unsaturatedFat = meal.unsaturatedFat?.let { Mass.grams(it) },
                transFat = meal.transFat?.let { Mass.grams(it) },
                sodium = meal.sodium?.let { Mass.milligrams(it) },
                potassium = meal.potassium?.let { Mass.milligrams(it) },
                cholesterol = meal.cholesterol?.let { Mass.milligrams(it) },
                calcium = meal.calcium?.let { Mass.milligrams(it) },
                iron = meal.iron?.let { Mass.milligrams(it) },
                magnesium = meal.magnesium?.let { Mass.milligrams(it) },
                zinc = meal.zinc?.let { Mass.milligrams(it) },
                vitaminA = meal.vitaminA?.let { Mass.micrograms(it) },
                vitaminC = meal.vitaminC?.let { Mass.milligrams(it) },
                vitaminD = meal.vitaminD?.let { Mass.micrograms(it) },
                vitaminE = meal.vitaminE?.let { Mass.milligrams(it) },
                caffeine = meal.caffeine?.let { Mass.milligrams(it) }
            )

            client.insertRecords(listOf(nutritionRecord))
            HealthConnectWriteResult(
                success = true,
                message = "Meal synced successfully.",
                clientRecordId = clientRecordId,
                clientRecordVersion = clientRecordVersion
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w(TAG, "Health Connect nutrition write failed")
            HealthConnectWriteResult(
                success = false,
                message = "Saved on this device. Try syncing again later.",
                clientRecordId = meal.healthConnectClientRecordId,
                clientRecordVersion = meal.healthConnectClientRecordVersion
            )
        }
    }

    suspend fun deleteMeal(meal: MealRecord): HealthConnectWriteResult {
        val clientRecordId = meal.healthConnectClientRecordId
        if (clientRecordId.isBlank() || (!meal.healthConnectSynced && meal.healthConnectClientRecordVersion == 0L)) {
            val message = if (meal.healthConnectSynced) {
                "Removed from IZUN. The older entry remains in Health Connect."
            } else {
                "Meal deleted."
            }
            return HealthConnectWriteResult(true, message)
        }
        val client = healthConnectClient
            ?: return HealthConnectWriteResult(false, "Couldn't delete this meal. Try again.")

        return try {
            client.deleteRecords(NutritionRecord::class, emptyList(), listOf(clientRecordId))
            HealthConnectWriteResult(true, "Meal deleted.")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w(TAG, "Health Connect nutrition delete failed")
            HealthConnectWriteResult(false, "Couldn't delete this meal. Try again.")
        }
    }
}
