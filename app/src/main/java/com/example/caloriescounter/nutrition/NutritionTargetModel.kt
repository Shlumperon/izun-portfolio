package com.example.caloriescounter.nutrition

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CalorieTargetSource {
    INITIAL,
    ADAPTIVE
}

enum class WeightTrendQuality {
    HIGH,
    MEDIUM,
    LOW
}

enum class StepStability {
    STABLE,
    CHANGED,
    UNKNOWN
}

enum class MacroPreference {
    BALANCED,
    HIGHER_CARB,
    LOWER_CARB,
    HIGHER_PROTEIN,
    HIGHER_FAT
}

enum class NutritionFlag {
    LIMITED_SAFETY_MODE,
    PROTEIN_BASED_ON_ADJUSTED_WEIGHT,
    PROTEIN_REDUCED_TO_AMDR_CAP,
    FAT_REDUCED_TO_FIT_CALORIES,
    PROTEIN_REDUCED_TO_FIT_CALORIES,
    CARBOHYDRATE_BELOW_DEFAULT_RDA_WARNING,
    PROTEIN_ABOVE_AMDR_WARNING,
    FAT_BELOW_AMDR_WARNING,
    FAT_ABOVE_AMDR_WARNING,
    CARBOHYDRATE_BELOW_AMDR_WARNING,
    CARBOHYDRATE_ABOVE_AMDR_WARNING,
    NOT_ENOUGH_MACRO_LOGS,
    OBSERVED_PROTEIN_BELOW_MINIMUM,
    ADAPTATION_REDUCED_FOR_CALORIE_ADHERENCE,
    ADAPTATION_REDUCED_FOR_WEIGHT_TREND,
    ADAPTATION_REDUCED_FOR_STEP_CHANGE,
    HIGH_STEP_FAT_CAP_APPLIED
}

data class NutritionLogDay(
    val dayIndex: Int,
    val calories: Double,
    val proteinG: Double?,
    val fatG: Double?,
    val carbohydrateG: Double?,
    val isOutlier: Boolean = false
)

data class NutritionTargetInput(
    val calorieTarget: CalorieRange,
    val calorieTargetSource: CalorieTargetSource,
    val calorieConfidence: ModelConfidence,
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val sexFormula: SexFormula,
    val goal: CalorieGoal,
    val avgSteps: Int?,
    val stepConfidence: StepConfidence,
    val weightTrendQuality: WeightTrendQuality = WeightTrendQuality.MEDIUM,
    val stepStability: StepStability = StepStability.STABLE,
    val safetyFlags: Set<ModelFlag> = emptySet(),
    val nutritionLogs: List<NutritionLogDay> = emptyList(),
    val macroPreference: MacroPreference? = null
)

data class MacroRange(
    val lowG: Int,
    val highG: Int
)

data class MacroTarget(
    val targetG: Int,
    val rangeG: MacroRange,
    val kcal: Int,
    val percent: Int
)

data class MacroTargets(
    val protein: MacroTarget,
    val fat: MacroTarget,
    val carbohydrate: MacroTarget
)

data class NutritionTargetResult(
    val status: ModelStatus,
    val source: String,
    val calorieTarget: CalorieRange,
    val bmi: Double,
    val proteinReferenceWeightKg: Double,
    val proteinMinimumG: Int,
    val adaptationWeight: Double,
    val macros: MacroTargets?,
    val appliedMacroPreference: MacroPreference,
    val macroPreferenceFlags: Set<NutritionFlag>,
    val flags: Set<NutritionFlag>,
    val message: String? = null
)

private data class ProteinFactors(
    val target: Double,
    val minimum: Double
)

private data class MacroPrior(
    val proteinG: Double,
    val proteinMinG: Double,
    val fatPercent: Double,
    val flags: MutableSet<NutritionFlag>
)

private data class NutritionLogSummary(
    val validMacroDays: Int,
    val totalDays: Int,
    val avgCalories: Double,
    val avgProteinG: Double,
    val avgFatG: Double,
    val avgCarbohydrateG: Double,
    val calorieAdherence: Double,
    val hasEnoughMacroData: Boolean
)

object NutritionTargetModel {
    fun estimate(input: NutritionTargetInput): NutritionTargetResult {
        validateInput(input)

        val flags = linkedSetOf<NutritionFlag>()
        val macroPreference = input.macroPreference ?: MacroPreference.BALANCED
        val macroPreferenceFlags = linkedSetOf<NutritionFlag>()
        val bmi = CalorieEstimationModel.calculateBmi(input.heightCm, input.weightKg)
        if (hasLimitedSafety(input, bmi)) {
            flags += NutritionFlag.LIMITED_SAFETY_MODE
            return NutritionTargetResult(
                status = ModelStatus.LIMITED,
                source = "limited",
                calorieTarget = input.calorieTarget,
                bmi = roundToOneDecimal(bmi),
                proteinReferenceWeightKg = roundToOneDecimal(proteinReferenceWeight(input.weightKg, input.heightCm, bmi)),
                proteinMinimumG = 0,
                adaptationWeight = 0.0,
                macros = null,
                appliedMacroPreference = macroPreference,
                macroPreferenceFlags = emptySet(),
                flags = flags,
                message = "Standard macro targets may not be appropriate for this profile."
            )
        }

        val proteinReferenceWeight = proteinReferenceWeight(input.weightKg, input.heightCm, bmi)
        if (bmi >= 30.0) flags += NutritionFlag.PROTEIN_BASED_ON_ADJUSTED_WEIGHT

        val staticPrior = buildStaticPrior(
            input = input,
            proteinReferenceWeight = proteinReferenceWeight,
            flags = flags,
            macroPreference = macroPreference,
            macroPreferenceFlags = macroPreferenceFlags
        )
        val staticMacros = buildMacros(
            calorieTarget = input.calorieTarget,
            proteinG = staticPrior.proteinG,
            proteinMinG = staticPrior.proteinMinG,
            fatPercent = staticPrior.fatPercent,
            flags = flags
        )
        collectMacroPreferenceFlags(
            macroPreference = macroPreference,
            macros = staticMacros,
            flags = flags,
            macroPreferenceFlags = macroPreferenceFlags
        )

        val logSummary = summarizeNutritionLogs(input.nutritionLogs, input.calorieTarget.mid)
        if (!logSummary.hasEnoughMacroData) {
            flags += NutritionFlag.NOT_ENOUGH_MACRO_LOGS
            return NutritionTargetResult(
                status = ModelStatus.OK,
                source = "static_from_${input.calorieTargetSource.name.lowercase()}_calories",
                calorieTarget = input.calorieTarget,
                bmi = roundToOneDecimal(bmi),
                proteinReferenceWeightKg = roundToOneDecimal(proteinReferenceWeight),
                proteinMinimumG = roundToNearest5(staticPrior.proteinMinG),
                adaptationWeight = 0.0,
                macros = staticMacros,
                appliedMacroPreference = macroPreference,
                macroPreferenceFlags = macroPreferenceFlags,
                flags = flags
            )
        }

        val adaptationWeight = calculateAdaptationWeight(
            logSummary = logSummary,
            input = input,
            flags = flags
        )
        val adaptiveProteinG =
            if (logSummary.avgProteinG >= staticPrior.proteinMinG) {
                blend(staticPrior.proteinG, logSummary.avgProteinG, adaptationWeight * 0.30)
            } else {
                flags += NutritionFlag.OBSERVED_PROTEIN_BELOW_MINIMUM
                staticPrior.proteinG
            }
        val maxProteinG = input.calorieTarget.mid * 0.35 / 4
        val cappedProteinG =
            if (adaptiveProteinG > maxProteinG) {
                flags += NutritionFlag.PROTEIN_REDUCED_TO_AMDR_CAP
                if (macroPreference == MacroPreference.HIGHER_PROTEIN) {
                    macroPreferenceFlags += NutritionFlag.PROTEIN_REDUCED_TO_AMDR_CAP
                }
                maxProteinG
            } else {
                adaptiveProteinG
            }

        var adaptiveFatPercent = blend(
            staticPrior.fatPercent,
            (logSummary.avgFatG * 9) / logSummary.avgCalories,
            adaptationWeight
        ).coerceIn(0.20, 0.35)
        if ((input.avgSteps ?: 0) >= 12_000 && adaptiveFatPercent > 0.30) {
            adaptiveFatPercent = 0.30
            flags += NutritionFlag.HIGH_STEP_FAT_CAP_APPLIED
        }
        if ((input.avgSteps ?: 0) >= 16_000 && adaptiveFatPercent > 0.27) {
            adaptiveFatPercent = 0.27
            flags += NutritionFlag.HIGH_STEP_FAT_CAP_APPLIED
        }

        val adaptiveMacros = buildMacros(
            calorieTarget = input.calorieTarget,
            proteinG = cappedProteinG,
            proteinMinG = staticPrior.proteinMinG,
            fatPercent = adaptiveFatPercent,
            flags = flags
        )
        collectMacroPreferenceFlags(
            macroPreference = macroPreference,
            macros = adaptiveMacros,
            flags = flags,
            macroPreferenceFlags = macroPreferenceFlags
        )

        return NutritionTargetResult(
            status = ModelStatus.OK,
            source = "adaptive_macro_from_${input.calorieTargetSource.name.lowercase()}_calories",
            calorieTarget = input.calorieTarget,
            bmi = roundToOneDecimal(bmi),
            proteinReferenceWeightKg = roundToOneDecimal(proteinReferenceWeight),
            proteinMinimumG = roundToNearest5(staticPrior.proteinMinG),
            adaptationWeight = roundToTwoDecimals(adaptationWeight),
            macros = adaptiveMacros,
            appliedMacroPreference = macroPreference,
            macroPreferenceFlags = macroPreferenceFlags,
            flags = flags
        )
    }

    fun proteinReferenceWeight(weightKg: Double, heightCm: Double, bmi: Double): Double {
        require(weightKg > 0.0) { "weightKg must be greater than 0." }
        require(heightCm > 0.0) { "heightCm must be greater than 0." }
        if (bmi < 30.0) return weightKg
        val heightM = heightCm / 100
        val weightAtBmi25 = 25 * heightM * heightM
        return weightAtBmi25 + 0.25 * (weightKg - weightAtBmi25)
    }

    fun proteinFactorsFromGoal(goal: CalorieGoal, age: Int): Pair<Double, Double> {
        val base = when (goal) {
            CalorieGoal.MAINTAIN -> ProteinFactors(target = 1.2, minimum = 1.0)
            CalorieGoal.LOSE_WEIGHT -> ProteinFactors(target = 1.6, minimum = 1.4)
            CalorieGoal.GAIN_MUSCLE -> ProteinFactors(target = 1.7, minimum = 1.5)
            CalorieGoal.RECOMPOSITION -> ProteinFactors(target = 1.6, minimum = 1.4)
        }
        return Pair(
            max(base.target, if (age >= 65) 1.2 else base.target),
            max(base.minimum, if (age >= 65) 1.0 else base.minimum)
        )
    }

    fun baseFatPercentFromGoal(goal: CalorieGoal): Double {
        return when (goal) {
            CalorieGoal.MAINTAIN -> 0.30
            CalorieGoal.LOSE_WEIGHT -> 0.28
            CalorieGoal.GAIN_MUSCLE -> 0.27
            CalorieGoal.RECOMPOSITION -> 0.28
        }
    }

    fun fatAdjustmentFromSteps(avgSteps: Int?): Double {
        return when {
            avgSteps == null -> 0.0
            avgSteps < 3_000 -> 0.02
            avgSteps < 9_000 -> 0.0
            avgSteps < 12_000 -> -0.02
            avgSteps < 16_000 -> -0.03
            else -> -0.05
        }
    }

    private fun buildStaticPrior(
        input: NutritionTargetInput,
        proteinReferenceWeight: Double,
        flags: MutableSet<NutritionFlag>,
        macroPreference: MacroPreference,
        macroPreferenceFlags: MutableSet<NutritionFlag>
    ): MacroPrior {
        val (proteinFactor, proteinMinFactor) = proteinFactorsFromGoal(input.goal, input.age)
        val proteinMinG = proteinReferenceWeight * proteinMinFactor
        val maxProteinG = input.calorieTarget.mid * 0.35 / 4
        val adjustedProteinFactor =
            if (macroPreference == MacroPreference.HIGHER_PROTEIN) proteinFactor + 0.15 else proteinFactor
        var proteinG = proteinReferenceWeight * adjustedProteinFactor
        if (proteinG > maxProteinG) {
            proteinG = maxProteinG
            flags += NutritionFlag.PROTEIN_REDUCED_TO_AMDR_CAP
            if (macroPreference == MacroPreference.HIGHER_PROTEIN) {
                macroPreferenceFlags += NutritionFlag.PROTEIN_REDUCED_TO_AMDR_CAP
            }
        }
        val preferredFatAdjustment = when (macroPreference) {
            MacroPreference.BALANCED,
            MacroPreference.HIGHER_PROTEIN -> 0.0
            MacroPreference.HIGHER_CARB -> -0.04
            MacroPreference.LOWER_CARB,
            MacroPreference.HIGHER_FAT -> 0.05
        }
        val fatPercent =
            (baseFatPercentFromGoal(input.goal) + fatAdjustmentFromSteps(input.avgSteps) + preferredFatAdjustment)
                .coerceIn(0.20, 0.35)
        return MacroPrior(
            proteinG = proteinG,
            proteinMinG = proteinMinG,
            fatPercent = fatPercent,
            flags = flags
        )
    }

    private fun buildMacros(
        calorieTarget: CalorieRange,
        proteinG: Double,
        proteinMinG: Double,
        fatPercent: Double,
        flags: MutableSet<NutritionFlag>
    ): MacroTargets {
        var adjustedProteinG = proteinG
        var adjustedFatPercent = fatPercent.coerceIn(0.20, 0.35)
        var fatG = calorieTarget.mid * adjustedFatPercent / 9
        var proteinKcal = adjustedProteinG * 4
        var fatKcal = fatG * 9
        var remainingKcal = calorieTarget.mid - proteinKcal - fatKcal

        if (remainingKcal < 0) {
            adjustedFatPercent = 0.20
            fatG = calorieTarget.mid * adjustedFatPercent / 9
            fatKcal = fatG * 9
            remainingKcal = calorieTarget.mid - proteinKcal - fatKcal
            flags += NutritionFlag.FAT_REDUCED_TO_FIT_CALORIES
        }
        if (remainingKcal < 0) {
            adjustedProteinG = max(0.0, (calorieTarget.mid - fatKcal) / 4)
            proteinKcal = adjustedProteinG * 4
            remainingKcal = calorieTarget.mid - proteinKcal - fatKcal
            flags += NutritionFlag.PROTEIN_REDUCED_TO_FIT_CALORIES
        }

        val carbohydrateG = max(0.0, remainingKcal / 4)
        val carbohydrateKcal = carbohydrateG * 4
        addMacroWarnings(
            calorieMid = calorieTarget.mid,
            proteinKcal = proteinKcal,
            fatKcal = fatKcal,
            carbKcal = carbohydrateKcal,
            carbG = carbohydrateG,
            flags = flags
        )

        return MacroTargets(
            protein = MacroTarget(
                targetG = roundToNearest5(adjustedProteinG),
                rangeG = proteinRange(adjustedProteinG, proteinMinG),
                kcal = proteinKcal.roundToInt(),
                percent = percentOfCalories(proteinKcal, calorieTarget.mid)
            ),
            fat = MacroTarget(
                targetG = roundToNearest5(fatG),
                rangeG = fatRange(fatG),
                kcal = fatKcal.roundToInt(),
                percent = percentOfCalories(fatKcal, calorieTarget.mid)
            ),
            carbohydrate = MacroTarget(
                targetG = roundToNearest5(carbohydrateG),
                rangeG = carbohydrateRange(calorieTarget, adjustedProteinG, fatG),
                kcal = carbohydrateKcal.roundToInt(),
                percent = percentOfCalories(carbohydrateKcal, calorieTarget.mid)
            )
        )
    }

    private fun collectMacroPreferenceFlags(
        macroPreference: MacroPreference,
        macros: MacroTargets,
        flags: Set<NutritionFlag>,
        macroPreferenceFlags: MutableSet<NutritionFlag>
    ) {
        if (
            macroPreference == MacroPreference.LOWER_CARB &&
            macros.carbohydrate.targetG < 130 &&
            NutritionFlag.CARBOHYDRATE_BELOW_DEFAULT_RDA_WARNING in flags
        ) {
            macroPreferenceFlags += NutritionFlag.CARBOHYDRATE_BELOW_DEFAULT_RDA_WARNING
        }
        if (
            macroPreference == MacroPreference.HIGHER_PROTEIN &&
            NutritionFlag.PROTEIN_REDUCED_TO_AMDR_CAP in flags
        ) {
            macroPreferenceFlags += NutritionFlag.PROTEIN_REDUCED_TO_AMDR_CAP
        }
    }

    private fun summarizeNutritionLogs(
        logs: List<NutritionLogDay>,
        calorieTargetMid: Int
    ): NutritionLogSummary {
        val validLogs = logs.filter {
            !it.isOutlier &&
                it.calories >= calorieTargetMid * 0.70 &&
                it.proteinG != null &&
                it.fatG != null &&
                it.carbohydrateG != null
        }
        val avgCalories = validLogs.map { it.calories }.averageOrZero()
        return NutritionLogSummary(
            validMacroDays = validLogs.size,
            totalDays = logs.map { it.dayIndex }.distinct().size,
            avgCalories = avgCalories,
            avgProteinG = validLogs.map { it.proteinG ?: 0.0 }.averageOrZero(),
            avgFatG = validLogs.map { it.fatG ?: 0.0 }.averageOrZero(),
            avgCarbohydrateG = validLogs.map { it.carbohydrateG ?: 0.0 }.averageOrZero(),
            calorieAdherence = if (calorieTargetMid > 0) avgCalories / calorieTargetMid else 0.0,
            hasEnoughMacroData = validLogs.size >= 10
        )
    }

    private fun calculateAdaptationWeight(
        logSummary: NutritionLogSummary,
        input: NutritionTargetInput,
        flags: MutableSet<NutritionFlag>
    ): Double {
        var weight = when {
            logSummary.validMacroDays < 10 -> 0.0
            logSummary.validMacroDays < 20 -> 0.25
            logSummary.validMacroDays < 40 -> 0.40
            else -> 0.60
        }
        if (logSummary.calorieAdherence < 0.75 || logSummary.calorieAdherence > 1.25) {
            weight *= 0.5
            flags += NutritionFlag.ADAPTATION_REDUCED_FOR_CALORIE_ADHERENCE
        }
        if (input.weightTrendQuality == WeightTrendQuality.LOW) {
            weight *= 0.5
            flags += NutritionFlag.ADAPTATION_REDUCED_FOR_WEIGHT_TREND
        }
        if (input.stepStability == StepStability.CHANGED) {
            weight *= 0.75
            flags += NutritionFlag.ADAPTATION_REDUCED_FOR_STEP_CHANGE
        }
        return weight.coerceIn(0.0, 0.60)
    }

    private fun hasLimitedSafety(input: NutritionTargetInput, bmi: Double): Boolean {
        return input.age < 18 ||
            bmi < 18.5 ||
            CalorieEstimationModel.hasLimitedModeFlag(input.safetyFlags)
    }

    private fun validateInput(input: NutritionTargetInput) {
        require(input.calorieTarget.low > 0) { "calorie target low must be greater than 0." }
        require(input.calorieTarget.mid > 0) { "calorie target mid must be greater than 0." }
        require(input.calorieTarget.high > 0) { "calorie target high must be greater than 0." }
        require(input.calorieTarget.low <= input.calorieTarget.mid) { "calorie target low cannot exceed mid." }
        require(input.calorieTarget.mid <= input.calorieTarget.high) { "calorie target mid cannot exceed high." }
        require(input.weightKg > 0.0) { "weightKg must be greater than 0." }
        require(input.heightCm > 0.0) { "heightCm must be greater than 0." }
        require(input.age > 0) { "age must be greater than 0." }
    }

    private fun addMacroWarnings(
        calorieMid: Int,
        proteinKcal: Double,
        fatKcal: Double,
        carbKcal: Double,
        carbG: Double,
        flags: MutableSet<NutritionFlag>
    ) {
        val proteinPercent = proteinKcal / calorieMid
        val fatPercent = fatKcal / calorieMid
        val carbPercent = carbKcal / calorieMid
        if (carbG < 130.0) flags += NutritionFlag.CARBOHYDRATE_BELOW_DEFAULT_RDA_WARNING
        if (proteinPercent > 0.35) flags += NutritionFlag.PROTEIN_ABOVE_AMDR_WARNING
        if (fatPercent < 0.20) flags += NutritionFlag.FAT_BELOW_AMDR_WARNING
        if (fatPercent > 0.35) flags += NutritionFlag.FAT_ABOVE_AMDR_WARNING
        if (carbPercent < 0.45) flags += NutritionFlag.CARBOHYDRATE_BELOW_AMDR_WARNING
        if (carbPercent > 0.65) flags += NutritionFlag.CARBOHYDRATE_ABOVE_AMDR_WARNING
    }

    private fun proteinRange(proteinG: Double, proteinMinG: Double): MacroRange {
        return MacroRange(
            lowG = roundToNearest5(max(proteinMinG, proteinG * 0.90)),
            highG = roundToNearest5(proteinG * 1.10)
        )
    }

    private fun fatRange(fatG: Double): MacroRange {
        return MacroRange(
            lowG = roundToNearest5(fatG * 0.85),
            highG = roundToNearest5(fatG * 1.15)
        )
    }

    private fun carbohydrateRange(
        calorieTarget: CalorieRange,
        proteinG: Double,
        fatG: Double
    ): MacroRange {
        val proteinLow = proteinG * 0.90
        val proteinHigh = proteinG * 1.10
        val fatLow = fatG * 0.85
        val fatHigh = fatG * 1.15
        val carbLow = (calorieTarget.low - proteinHigh * 4 - fatHigh * 9) / 4
        val carbHigh = (calorieTarget.high - proteinLow * 4 - fatLow * 9) / 4
        return MacroRange(
            lowG = roundToNearest5(max(0.0, carbLow)),
            highG = roundToNearest5(max(0.0, carbHigh))
        )
    }

    private fun blend(staticValue: Double, observedValue: Double, weight: Double): Double {
        return staticValue * (1 - weight) + observedValue * weight
    }

    private fun percentOfCalories(kcal: Double, calorieMid: Int): Int {
        return (kcal / calorieMid * 100).roundToInt()
    }

    private fun roundToNearest5(value: Double): Int = (value / 5).roundToInt() * 5

    private fun roundToOneDecimal(value: Double): Double = (value * 10).roundToInt() / 10.0

    private fun roundToTwoDecimals(value: Double): Double = (value * 100).roundToInt() / 100.0

    private fun Iterable<Double>.averageOrZero(): Double {
        val values = toList()
        return if (values.isEmpty()) 0.0 else values.average()
    }
}
