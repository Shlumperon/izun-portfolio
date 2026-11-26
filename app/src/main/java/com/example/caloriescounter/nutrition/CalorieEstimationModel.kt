package com.example.caloriescounter.nutrition

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class SexFormula {
    MALE,
    FEMALE
}

enum class CalorieGoal {
    MAINTAIN,
    LOSE_WEIGHT,
    GAIN_MUSCLE,
    RECOMPOSITION
}

enum class GoalPace {
    SLOW,
    STANDARD,
    FASTER
}

enum class ModelStatus {
    OK,
    LIMITED
}

enum class ManualStepRange(val midpoint: Int) {
    LESS_THAN_3000(1500),
    FROM_3000_TO_5999(4500),
    FROM_6000_TO_8999(7500),
    FROM_9000_TO_11999(10500),
    FROM_12000_TO_15999(14000),
    FROM_16000_PLUS(17000)
}

enum class StepConfidence {
    HIGH,
    MEDIUM,
    MEDIUM_LOW,
    LOW
}

enum class ModelConfidence {
    HIGH,
    MEDIUM,
    LOW
}

enum class TrainingExperience {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

enum class RecentWeightStatus {
    STABLE,
    NOT_SURE,
    LOST_WEIGHT,
    GAINED_WEIGHT
}

enum class WeightUnit {
    KG,
    LB
}

enum class TimeframeUnit {
    DAYS,
    WEEKS,
    MONTHS
}

enum class ModelFlag {
    UNDER_18,
    UNDERWEIGHT_BMI,
    PREGNANCY,
    BREASTFEEDING,
    EATING_DISORDER_HISTORY,
    MEDICAL_CONDITION,
    MANUAL_STEP_RANGE_USED,
    STEP_ESTIMATE_REQUIRED,
    MAJOR_RECENT_WEIGHT_CHANGE,
    VERY_FAST_WEIGHT_CHANGE,
    SHORT_RECENT_WEIGHT_TIMEFRAME,
    PAL_MINIMUM_APPLIED,
    DEFICIT_REDUCED_DUE_TO_FLOOR,
    LIMITED_MODE_GOAL_REPLACED_WITH_MAINTENANCE,
    GOAL_PACE_REDUCED_FOR_SAFETY,
    TARGET_WEIGHT_CONFLICTS_WITH_GOAL,
    TARGET_WEIGHT_UNDERWEIGHT_WARNING
}

data class SafetyInputs(
    val isPregnant: Boolean = false,
    val isBreastfeeding: Boolean = false,
    val hasEatingDisorderHistory: Boolean = false,
    val hasRelevantMedicalCondition: Boolean = false
)

data class StepRecord(
    val steps: Int,
    val isCompleteDay: Boolean,
    val deviceActive: Boolean? = null
)

data class RecentWeightChangeInput(
    val status: RecentWeightStatus = RecentWeightStatus.STABLE,
    val amount: Double? = null,
    val unit: WeightUnit? = null,
    val timeframeValue: Double? = null,
    val timeframeUnit: TimeframeUnit? = null
)

data class RecentWeightChangeResult(
    val status: RecentWeightStatus,
    val changeKg: Double?,
    val timeDays: Double?,
    val changePercent: Double?,
    val weeklyRatePercent: Double?
)

data class CalorieModelInput(
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val sexFormula: SexFormula,
    val goal: CalorieGoal,
    val safetyInputs: SafetyInputs = SafetyInputs(),
    val bodyFatPercent: Double? = null,
    val bodyFatSource: String? = null,
    val manualStepRange: ManualStepRange? = null,
    val stepRecords: List<StepRecord> = emptyList(),
    val recentWeightChange: RecentWeightChangeInput = RecentWeightChangeInput(),
    val trainingExperience: TrainingExperience = TrainingExperience.BEGINNER,
    val goalPace: GoalPace? = null,
    val targetWeightKg: Double? = null
)

data class CalorieRange(
    val low: Int,
    val mid: Int,
    val high: Int
)

data class GoalTarget(
    val goal: CalorieGoal,
    val target: Int,
    val low: Int,
    val high: Int,
    val deficit: Int? = null,
    val surplus: Int? = null,
    val limited: Boolean = false
)

data class TargetWeightMetadata(
    val targetWeightKg: Double,
    val targetBMI: Double,
    val deltaKg: Double,
    val estimatedWeeksLow: Int?,
    val estimatedWeeksHigh: Int?
)

data class CalorieModelResult(
    val status: ModelStatus,
    val bmi: Double,
    val rmr: Int,
    val lifestyleFactor: Double,
    val avgSteps: Int?,
    val stepConfidence: StepConfidence,
    val palTotal: Double,
    val estimatedMaintenance: CalorieRange,
    val goalTarget: GoalTarget,
    val appliedGoalPace: GoalPace,
    val goalPaceFlags: Set<ModelFlag>,
    val targetWeight: TargetWeightMetadata?,
    val confidence: ModelConfidence,
    val flags: Set<ModelFlag>,
    val recentWeightChange: RecentWeightChangeResult
)

enum class AdaptiveFlag {
    INSUFFICIENT_DATA,
    LOW_LOG_COMPLETENESS,
    WEIGHT_MEASUREMENTS_NOT_STANDARDIZED,
    FAST_WEIGHT_CHANGE,
    STEP_ADJUSTED
}

data class DailyCalorieLog(
    val dayIndex: Int,
    val calories: Double,
    val avgSteps: Int
)

data class WeightMeasurement(
    val dayIndex: Int,
    val weightKg: Double,
    val measuredInSimilarConditions: Boolean = true
)

data class AdaptiveCalorieInput(
    val initialMaintenanceMid: Int,
    val initialAvgSteps: Int,
    val goal: CalorieGoal,
    val sexFormula: SexFormula,
    val calorieLogs: List<DailyCalorieLog>,
    val weightMeasurements: List<WeightMeasurement>,
    val targetAvgSteps: Int? = null,
    val trainingExperience: TrainingExperience = TrainingExperience.BEGINNER,
    val expectedLogDays: Int? = null,
    val goalPace: GoalPace? = null
)

data class AdaptiveCalorieResult(
    val status: ModelStatus,
    val averageIntake: Int?,
    val observedMaintenance: Int?,
    val stepAdjustedObservedMaintenance: Int?,
    val adaptiveWeight: Double,
    val updatedMaintenance: CalorieRange,
    val updatedGoalTarget: GoalTarget,
    val averageSteps: Int?,
    val flags: Set<AdaptiveFlag>
)

private enum class RecentWeightConfidence {
    HIGH,
    MEDIUM_LOW,
    LOW
}

private data class StepSelection(
    val lifestyleFactor: Double,
    val avgSteps: Int?,
    val confidence: StepConfidence,
    val flags: Set<ModelFlag>
)

object CalorieEstimationModel {
    fun estimate(input: CalorieModelInput): CalorieModelResult {
        validateInput(input)

        val flags = linkedSetOf<ModelFlag>()
        val bmi = calculateBmi(input.heightCm, input.weightKg)
        if (input.age < 18) flags += ModelFlag.UNDER_18
        if (bmi < 18.5) flags += ModelFlag.UNDERWEIGHT_BMI
        if (input.safetyInputs.isPregnant) flags += ModelFlag.PREGNANCY
        if (input.safetyInputs.isBreastfeeding) flags += ModelFlag.BREASTFEEDING
        if (input.safetyInputs.hasEatingDisorderHistory) flags += ModelFlag.EATING_DISORDER_HISTORY
        if (input.safetyInputs.hasRelevantMedicalCondition) flags += ModelFlag.MEDICAL_CONDITION

        val recentWeightChange = calculateRecentWeightChange(input.recentWeightChange, input.weightKg)
        val recentConfidence = classifyRecentWeightChange(recentWeightChange, flags)
        var rmr = calculateRmr(input.sexFormula, input.weightKg, input.heightCm, input.age)
        if (
            ModelFlag.MAJOR_RECENT_WEIGHT_CHANGE in flags &&
            (recentWeightChange.changeKg ?: 0.0) < 0.0
        ) {
            rmr *= 0.97
        }

        val stepSelection = selectStepActivity(input.stepRecords, input.manualStepRange)
        flags += stepSelection.flags

        var tdee = rmr * stepSelection.lifestyleFactor
        if (tdee / rmr < 1.15) {
            tdee = rmr * 1.15
            flags += ModelFlag.PAL_MINIMUM_APPLIED
        }

        val status = if (hasLimitedModeFlag(flags)) ModelStatus.LIMITED else ModelStatus.OK
        val confidence = classifyConfidence(
            status = status,
            stepConfidence = stepSelection.confidence,
            recentWeightConfidence = recentConfidence,
            bmi = bmi,
            flags = flags
        )
        val errorPct = when (confidence) {
            ModelConfidence.HIGH -> 0.12
            ModelConfidence.MEDIUM -> 0.15
            ModelConfidence.LOW -> 0.22
        }
        val maintenance = CalorieRange(
            low = roundToNearest50(tdee * (1 - errorPct)),
            mid = roundToNearest50(tdee),
            high = roundToNearest50(tdee * (1 + errorPct))
        )
        val requestedGoalPace = input.goalPace ?: GoalPace.STANDARD
        val goalPaceFlags = linkedSetOf<ModelFlag>()
        val goalTarget = calculateGoalTarget(
            goal = input.goal,
            tdee = tdee,
            sexFormula = input.sexFormula,
            trainingExperience = input.trainingExperience,
            goalPace = requestedGoalPace,
            status = status,
            flags = flags,
            goalPaceFlags = goalPaceFlags
        )
        val appliedGoalPace =
            if (input.goal == CalorieGoal.MAINTAIN || goalTarget.limited) GoalPace.STANDARD else requestedGoalPace
        val targetWeight = calculateTargetWeightMetadata(
            targetWeightKg = input.targetWeightKg,
            currentWeightKg = input.weightKg,
            heightCm = input.heightCm,
            goal = input.goal,
            goalPace = requestedGoalPace,
            flags = flags
        )

        return CalorieModelResult(
            status = status,
            bmi = roundToOneDecimal(bmi),
            rmr = roundToNearest25(rmr),
            lifestyleFactor = stepSelection.lifestyleFactor,
            avgSteps = stepSelection.avgSteps,
            stepConfidence = stepSelection.confidence,
            palTotal = roundToTwoDecimals(tdee / rmr),
            estimatedMaintenance = maintenance,
            goalTarget = goalTarget,
            appliedGoalPace = appliedGoalPace,
            goalPaceFlags = goalPaceFlags,
            targetWeight = targetWeight,
            confidence = confidence,
            flags = flags,
            recentWeightChange = recentWeightChange
        )
    }

    fun calculateRmr(sexFormula: SexFormula, weightKg: Double, heightCm: Double, age: Int): Double {
        return when (sexFormula) {
            SexFormula.MALE -> 10 * weightKg + 6.25 * heightCm - 5 * age + 5
            SexFormula.FEMALE -> 10 * weightKg + 6.25 * heightCm - 5 * age - 161
        }
    }

    fun calculateBmi(heightCm: Double, weightKg: Double): Double {
        require(heightCm > 0.0) { "heightCm must be greater than 0." }
        require(weightKg > 0.0) { "weightKg must be greater than 0." }
        val heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    fun factorFromSteps(avgSteps: Int): Double {
        require(avgSteps >= 0) { "avgSteps cannot be negative." }
        return when {
            avgSteps < 3_000 -> 1.20
            avgSteps < 6_000 -> 1.28
            avgSteps < 9_000 -> 1.38
            avgSteps < 12_000 -> 1.48
            avgSteps < 16_000 -> 1.60
            else -> 1.70
        }
    }

    fun calculateRecentWeightChange(
        input: RecentWeightChangeInput,
        currentWeightKg: Double
    ): RecentWeightChangeResult {
        require(currentWeightKg > 0.0) { "currentWeightKg must be greater than 0." }
        return when (input.status) {
            RecentWeightStatus.STABLE -> RecentWeightChangeResult(
                status = input.status,
                changeKg = 0.0,
                timeDays = null,
                changePercent = 0.0,
                weeklyRatePercent = 0.0
            )
            RecentWeightStatus.NOT_SURE -> RecentWeightChangeResult(
                status = input.status,
                changeKg = null,
                timeDays = null,
                changePercent = null,
                weeklyRatePercent = null
            )
            RecentWeightStatus.LOST_WEIGHT,
            RecentWeightStatus.GAINED_WEIGHT -> {
                val amount = requireNotNull(input.amount) { "Recent weight change amount is required." }
                val unit = requireNotNull(input.unit) { "Recent weight change unit is required." }
                val timeframeValue = requireNotNull(input.timeframeValue) { "Recent weight change timeframe is required." }
                val timeframeUnit = requireNotNull(input.timeframeUnit) { "Recent weight change timeframe unit is required." }
                require(amount > 0.0) { "Recent weight change amount must be greater than 0." }
                require(timeframeValue > 0.0) { "Recent weight change timeframe must be greater than 0." }

                val amountKg = convertWeightToKg(amount, unit)
                val signedChangeKg =
                    if (input.status == RecentWeightStatus.LOST_WEIGHT) -amountKg else amountKg
                val timeDays = convertTimeframeToDays(timeframeValue, timeframeUnit)
                val changePercent = signedChangeKg / currentWeightKg * 100
                val weeklyRatePercent = changePercent / (timeDays / 7)
                RecentWeightChangeResult(
                    status = input.status,
                    changeKg = signedChangeKg,
                    timeDays = timeDays,
                    changePercent = changePercent,
                    weeklyRatePercent = weeklyRatePercent
                )
            }
        }
    }

    fun calorieFloor(sexFormula: SexFormula, tdee: Double): Double {
        return when (sexFormula) {
            SexFormula.FEMALE -> max(1_200.0, 0.70 * tdee)
            SexFormula.MALE -> max(1_500.0, 0.70 * tdee)
        }
    }

    fun roundToNearest25(value: Double): Int = (value / 25).roundToInt() * 25

    fun roundToNearest50(value: Double): Int = (value / 50).roundToInt() * 50

    private fun validateInput(input: CalorieModelInput) {
        require(input.age > 0) { "age must be greater than 0." }
        require(input.heightCm > 0.0) { "heightCm must be greater than 0." }
        require(input.weightKg > 0.0) { "weightKg must be greater than 0." }
        input.bodyFatPercent?.let {
            require(it in 0.0..100.0) { "bodyFatPercent must be between 0 and 100." }
        }
        input.targetWeightKg?.let {
            require(it > 0.0) { "targetWeightKg must be greater than 0." }
            require(it <= 500.0) { "targetWeightKg must be plausible." }
        }
    }

    private fun selectStepActivity(
        stepRecords: List<StepRecord>,
        manualStepRange: ManualStepRange?
    ): StepSelection {
        val validDays = stepRecords.filter { it.isCompleteDay && it.steps >= 500 && it.deviceActive != false }
        if (validDays.size >= 14) {
            val avgSteps = validDays.map { it.steps }.average().roundToInt()
            return StepSelection(
                lifestyleFactor = factorFromSteps(avgSteps),
                avgSteps = avgSteps,
                confidence = StepConfidence.HIGH,
                flags = emptySet()
            )
        }
        if (validDays.size >= 7) {
            val avgSteps = validDays.map { it.steps }.average().roundToInt()
            return StepSelection(
                lifestyleFactor = factorFromSteps(avgSteps),
                avgSteps = avgSteps,
                confidence = StepConfidence.MEDIUM,
                flags = emptySet()
            )
        }
        if (manualStepRange != null) {
            return StepSelection(
                lifestyleFactor = factorFromSteps(manualStepRange.midpoint),
                avgSteps = manualStepRange.midpoint,
                confidence = StepConfidence.MEDIUM_LOW,
                flags = setOf(ModelFlag.MANUAL_STEP_RANGE_USED)
            )
        }
        return StepSelection(
            lifestyleFactor = factorFromSteps(ManualStepRange.LESS_THAN_3000.midpoint),
            avgSteps = null,
            confidence = StepConfidence.LOW,
            flags = setOf(ModelFlag.STEP_ESTIMATE_REQUIRED)
        )
    }

    private fun classifyRecentWeightChange(
        recentWeightChange: RecentWeightChangeResult,
        flags: MutableSet<ModelFlag>
    ): RecentWeightConfidence {
        if (recentWeightChange.status == RecentWeightStatus.NOT_SURE) {
            return RecentWeightConfidence.LOW
        }
        if (recentWeightChange.status == RecentWeightStatus.STABLE) {
            return RecentWeightConfidence.HIGH
        }

        val absChangePercent = abs(recentWeightChange.changePercent ?: 0.0)
        val absWeeklyRate = abs(recentWeightChange.weeklyRatePercent ?: 0.0)
        val timeDays = recentWeightChange.timeDays ?: return RecentWeightConfidence.LOW
        var confidence = if (timeDays >= 30) RecentWeightConfidence.HIGH else RecentWeightConfidence.MEDIUM_LOW

        if (timeDays < 14) {
            flags += ModelFlag.SHORT_RECENT_WEIGHT_TIMEFRAME
            confidence = RecentWeightConfidence.LOW
        }
        if (absChangePercent >= 5.0 && timeDays <= 120.0) {
            flags += ModelFlag.MAJOR_RECENT_WEIGHT_CHANGE
            if (confidence == RecentWeightConfidence.HIGH) {
                confidence = RecentWeightConfidence.MEDIUM_LOW
            }
        }
        if (absWeeklyRate > 1.5) {
            flags += ModelFlag.VERY_FAST_WEIGHT_CHANGE
            confidence = RecentWeightConfidence.LOW
        }
        return confidence
    }

    private fun classifyConfidence(
        status: ModelStatus,
        stepConfidence: StepConfidence,
        recentWeightConfidence: RecentWeightConfidence,
        bmi: Double,
        flags: Set<ModelFlag>
    ): ModelConfidence {
        if (
            status == ModelStatus.LIMITED ||
            stepConfidence == StepConfidence.LOW ||
            recentWeightConfidence == RecentWeightConfidence.LOW ||
            bmi >= 40.0
        ) {
            return ModelConfidence.LOW
        }
        if (stepConfidence == StepConfidence.HIGH && recentWeightConfidence == RecentWeightConfidence.HIGH) {
            return ModelConfidence.HIGH
        }
        return ModelConfidence.MEDIUM
    }

    fun calculateGoalTarget(
        goal: CalorieGoal,
        tdee: Double,
        sexFormula: SexFormula,
        trainingExperience: TrainingExperience,
        goalPace: GoalPace = GoalPace.STANDARD,
        status: ModelStatus = ModelStatus.OK,
        flags: MutableSet<ModelFlag> = linkedSetOf(),
        goalPaceFlags: MutableSet<ModelFlag> = linkedSetOf()
    ): GoalTarget {
        if (status == ModelStatus.LIMITED && goal != CalorieGoal.MAINTAIN) {
            flags += ModelFlag.LIMITED_MODE_GOAL_REPLACED_WITH_MAINTENANCE
            flags += ModelFlag.GOAL_PACE_REDUCED_FOR_SAFETY
            goalPaceFlags += ModelFlag.GOAL_PACE_REDUCED_FOR_SAFETY
            val target = roundToNearest50(tdee)
            return GoalTarget(
                goal = CalorieGoal.MAINTAIN,
                target = target,
                low = target - 100,
                high = target + 100,
                limited = true
            )
        }

        return when (goal) {
            CalorieGoal.MAINTAIN -> {
                val target = roundToNearest50(tdee)
                GoalTarget(goal = goal, target = target, low = target - 100, high = target + 100)
            }
            CalorieGoal.LOSE_WEIGHT -> {
                val deficit = when (goalPace) {
                    GoalPace.SLOW -> min(0.10 * tdee, 300.0)
                    GoalPace.STANDARD -> min(0.15 * tdee, 500.0)
                    GoalPace.FASTER -> min(0.20 * tdee, 750.0)
                }
                val floor = calorieFloor(sexFormula, tdee)
                var rawTarget = tdee - deficit
                if (rawTarget < floor) {
                    rawTarget = floor
                    flags += ModelFlag.DEFICIT_REDUCED_DUE_TO_FLOOR
                    flags += ModelFlag.GOAL_PACE_REDUCED_FOR_SAFETY
                    goalPaceFlags += ModelFlag.GOAL_PACE_REDUCED_FOR_SAFETY
                }
                val target = roundToNearest50(rawTarget)
                GoalTarget(
                    goal = goal,
                    target = target,
                    low = target - 100,
                    high = target + 100,
                    deficit = roundToNearest50(tdee - rawTarget)
                )
            }
            CalorieGoal.GAIN_MUSCLE -> {
                val surplus = when (goalPace) {
                    GoalPace.SLOW -> min(0.03 * tdee, 150.0)
                    GoalPace.STANDARD -> min(0.05 * tdee, 250.0)
                    GoalPace.FASTER -> min(0.08 * tdee, 400.0)
                }
                val target = roundToNearest50(tdee + surplus)
                GoalTarget(
                    goal = goal,
                    target = target,
                    low = target - 100,
                    high = target + 100,
                    surplus = roundToNearest50(surplus)
                )
            }
            CalorieGoal.RECOMPOSITION -> {
                val multiplier = when (goalPace) {
                    GoalPace.SLOW -> 1.0
                    GoalPace.STANDARD -> 0.97
                    GoalPace.FASTER -> 0.95
                }
                val target = roundToNearest50(tdee * multiplier)
                GoalTarget(
                    goal = goal,
                    target = target,
                    low = roundToNearest50(tdee * 0.95),
                    high = roundToNearest50(tdee * 1.05),
                    deficit = (tdee - tdee * multiplier).takeIf { it > 0.0 }?.let(::roundToNearest50)
                )
            }
        }
    }

    private fun calculateTargetWeightMetadata(
        targetWeightKg: Double?,
        currentWeightKg: Double,
        heightCm: Double,
        goal: CalorieGoal,
        goalPace: GoalPace,
        flags: MutableSet<ModelFlag>
    ): TargetWeightMetadata? {
        if (targetWeightKg == null) return null
        val targetBmi = calculateBmi(heightCm, targetWeightKg)
        val deltaKg = targetWeightKg - currentWeightKg
        if (goal == CalorieGoal.LOSE_WEIGHT && deltaKg >= 0.0) {
            flags += ModelFlag.TARGET_WEIGHT_CONFLICTS_WITH_GOAL
        }
        if (goal == CalorieGoal.GAIN_MUSCLE && deltaKg <= 0.0) {
            flags += ModelFlag.TARGET_WEIGHT_CONFLICTS_WITH_GOAL
        }
        if (targetBmi < 18.5) {
            flags += ModelFlag.TARGET_WEIGHT_UNDERWEIGHT_WARNING
        }

        val weeklyRate = weeklyWeightRate(goal, goalPace)
        val estimatedWeeks =
            if (weeklyRate != null && abs(deltaKg) > 0.01) {
                abs(deltaKg) / (currentWeightKg * weeklyRate)
            } else {
                null
            }
        return TargetWeightMetadata(
            targetWeightKg = roundToOneDecimal(targetWeightKg),
            targetBMI = roundToOneDecimal(targetBmi),
            deltaKg = roundToOneDecimal(deltaKg),
            estimatedWeeksLow = estimatedWeeks?.let { max(1, floorToInt(it * 0.85)) },
            estimatedWeeksHigh = estimatedWeeks?.let { max(1, ceilToInt(it * 1.15)) }
        )
    }

    private fun weeklyWeightRate(goal: CalorieGoal, goalPace: GoalPace): Double? {
        return when (goal) {
            CalorieGoal.LOSE_WEIGHT -> when (goalPace) {
                GoalPace.SLOW -> 0.0025
                GoalPace.STANDARD -> 0.005
                GoalPace.FASTER -> 0.0075
            }
            CalorieGoal.GAIN_MUSCLE -> when (goalPace) {
                GoalPace.SLOW -> 0.001
                GoalPace.STANDARD -> 0.002
                GoalPace.FASTER -> 0.003
            }
            CalorieGoal.MAINTAIN,
            CalorieGoal.RECOMPOSITION -> null
        }
    }

    internal fun hasLimitedModeFlag(flags: Set<ModelFlag>): Boolean {
        return ModelFlag.UNDER_18 in flags ||
            ModelFlag.UNDERWEIGHT_BMI in flags ||
            ModelFlag.PREGNANCY in flags ||
            ModelFlag.BREASTFEEDING in flags ||
            ModelFlag.EATING_DISORDER_HISTORY in flags ||
            ModelFlag.MEDICAL_CONDITION in flags
    }

    private fun convertWeightToKg(amount: Double, unit: WeightUnit): Double {
        return when (unit) {
            WeightUnit.KG -> amount
            WeightUnit.LB -> amount * 0.45359237
        }
    }

    private fun convertTimeframeToDays(value: Double, unit: TimeframeUnit): Double {
        return when (unit) {
            TimeframeUnit.DAYS -> value
            TimeframeUnit.WEEKS -> value * 7
            TimeframeUnit.MONTHS -> value * 30.4375
        }
    }

    private fun roundToOneDecimal(value: Double): Double = (value * 10).roundToInt() / 10.0

    private fun roundToTwoDecimals(value: Double): Double = (value * 100).roundToInt() / 100.0

    private fun floorToInt(value: Double): Int = floor(value).roundToInt()

    private fun ceilToInt(value: Double): Int = ceil(value).roundToInt()
}

object AdaptiveCalorieModel {
    private const val KCAL_PER_KG_BODY_WEIGHT = 7_700.0

    fun update(input: AdaptiveCalorieInput): AdaptiveCalorieResult {
        require(input.initialMaintenanceMid > 0) { "initialMaintenanceMid must be greater than 0." }
        require(input.initialAvgSteps >= 0) { "initialAvgSteps cannot be negative." }

        val flags = linkedSetOf<AdaptiveFlag>()
        val sortedWeights = input.weightMeasurements.sortedBy { it.dayIndex }
        val firstWeight = sortedWeights.firstOrNull()
        val lastWeight = sortedWeights.lastOrNull()
        val daySpan =
            if (firstWeight != null && lastWeight != null) lastWeight.dayIndex - firstWeight.dayIndex else 0

        val relevantLogs =
            if (firstWeight != null && lastWeight != null) {
                input.calorieLogs.filter { it.dayIndex in firstWeight.dayIndex..lastWeight.dayIndex }
            } else {
                emptyList()
            }
        val expectedDays = input.expectedLogDays ?: max(daySpan, 1)
        val logCompleteness = relevantLogs.size.toDouble() / expectedDays.coerceAtLeast(1)

        if (sortedWeights.size < 2 || daySpan < 14 || relevantLogs.isEmpty()) {
            flags += AdaptiveFlag.INSUFFICIENT_DATA
            val initialRange = CalorieRange(
                low = CalorieEstimationModel.roundToNearest50(input.initialMaintenanceMid * 0.85),
                mid = CalorieEstimationModel.roundToNearest50(input.initialMaintenanceMid.toDouble()),
                high = CalorieEstimationModel.roundToNearest50(input.initialMaintenanceMid * 1.15)
            )
            return AdaptiveCalorieResult(
                status = ModelStatus.LIMITED,
                averageIntake = relevantLogs.takeIf { it.isNotEmpty() }?.let { averageCalories(it) },
                observedMaintenance = null,
                stepAdjustedObservedMaintenance = null,
                adaptiveWeight = 0.0,
                updatedMaintenance = initialRange,
                updatedGoalTarget = CalorieEstimationModel.calculateGoalTarget(
                    goal = input.goal,
                    tdee = initialRange.mid.toDouble(),
                    sexFormula = input.sexFormula,
                    trainingExperience = input.trainingExperience,
                    goalPace = input.goalPace ?: GoalPace.STANDARD
                ),
                averageSteps = relevantLogs.takeIf { it.isNotEmpty() }?.let { averageSteps(it) },
                flags = flags
            )
        }

        if (logCompleteness < 0.8) {
            flags += AdaptiveFlag.LOW_LOG_COMPLETENESS
        }
        if (sortedWeights.any { !it.measuredInSimilarConditions }) {
            flags += AdaptiveFlag.WEIGHT_MEASUREMENTS_NOT_STANDARDIZED
        }

        val averageIntake = averageCalories(relevantLogs)
        val observedAverageSteps = averageSteps(relevantLogs)
        val targetSteps = input.targetAvgSteps ?: observedAverageSteps
        val weightChangeKg = lastWeight!!.weightKg - firstWeight!!.weightKg
        val dailyWeightEnergyDelta = weightChangeKg * KCAL_PER_KG_BODY_WEIGHT / daySpan
        val observedMaintenance = averageIntake - dailyWeightEnergyDelta

        val stepAdjustedObservedMaintenance =
            if (targetSteps != observedAverageSteps) {
                flags += AdaptiveFlag.STEP_ADJUSTED
                observedMaintenance *
                    CalorieEstimationModel.factorFromSteps(targetSteps) /
                    CalorieEstimationModel.factorFromSteps(observedAverageSteps)
            } else {
                observedMaintenance
            }

        val weeklyWeightRatePercent =
            abs(weightChangeKg / firstWeight.weightKg * 100) / (daySpan / 7.0)
        if (weeklyWeightRatePercent > 1.5) {
            flags += AdaptiveFlag.FAST_WEIGHT_CHANGE
        }

        val adaptiveWeight = adaptiveDominance(daySpan, logCompleteness, flags)
        val updatedMid =
            input.initialMaintenanceMid * (1 - adaptiveWeight) +
                stepAdjustedObservedMaintenance * adaptiveWeight
        val errorPct = when {
            adaptiveWeight >= 0.7 && AdaptiveFlag.LOW_LOG_COMPLETENESS !in flags -> 0.10
            adaptiveWeight >= 0.5 -> 0.12
            else -> 0.15
        }
        val updatedRange = CalorieRange(
            low = CalorieEstimationModel.roundToNearest50(updatedMid * (1 - errorPct)),
            mid = CalorieEstimationModel.roundToNearest50(updatedMid),
            high = CalorieEstimationModel.roundToNearest50(updatedMid * (1 + errorPct))
        )

        return AdaptiveCalorieResult(
            status = ModelStatus.OK,
            averageIntake = averageIntake,
            observedMaintenance = CalorieEstimationModel.roundToNearest50(observedMaintenance),
            stepAdjustedObservedMaintenance = CalorieEstimationModel.roundToNearest50(stepAdjustedObservedMaintenance),
            adaptiveWeight = roundToTwoDecimals(adaptiveWeight),
            updatedMaintenance = updatedRange,
            updatedGoalTarget = CalorieEstimationModel.calculateGoalTarget(
                goal = input.goal,
                tdee = updatedRange.mid.toDouble(),
                sexFormula = input.sexFormula,
                trainingExperience = input.trainingExperience,
                goalPace = input.goalPace ?: GoalPace.STANDARD
            ),
            averageSteps = observedAverageSteps,
            flags = flags
        )
    }

    private fun adaptiveDominance(
        daySpan: Int,
        logCompleteness: Double,
        flags: Set<AdaptiveFlag>
    ): Double {
        var dominance = when {
            daySpan < 14 -> 0.0
            daySpan < 28 -> 0.25
            daySpan < 56 -> 0.50
            daySpan < 84 -> 0.70
            else -> 0.85
        }
        if (logCompleteness < 0.8) dominance *= 0.7
        if (AdaptiveFlag.WEIGHT_MEASUREMENTS_NOT_STANDARDIZED in flags) dominance *= 0.8
        if (AdaptiveFlag.FAST_WEIGHT_CHANGE in flags) dominance *= 0.8
        return dominance.coerceIn(0.0, 0.85)
    }

    private fun averageCalories(logs: List<DailyCalorieLog>): Int {
        return CalorieEstimationModel.roundToNearest25(logs.map { it.calories }.average())
    }

    private fun averageSteps(logs: List<DailyCalorieLog>): Int {
        return logs.map { it.avgSteps }.average().roundToInt()
    }

    private fun roundToTwoDecimals(value: Double): Double = (value * 100).roundToInt() / 100.0
}

object NutritionModelUiNotes {
    const val STEP_SOURCE =
        "Ask for phone step permission first. If fewer than 7 valid recent step days are available, ask the user for a step-range estimate."
    const val WEIGHT_MEASUREMENT_CONSISTENCY =
        "For adaptive calorie updates, ask the user to measure weight in similar conditions, ideally after waking up and before food or drink."
}
