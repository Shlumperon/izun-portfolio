package com.example.caloriescounter.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionTargetModelTest {
    @Test
    fun bmiIsCalculatedInternally() {
        val result = NutritionTargetModel.estimate(baseInput(heightCm = 170.0, weightKg = 80.0))

        assertEquals(27.7, result.bmi, 0.0)
    }

    @Test
    fun proteinReferenceWeight_usesActualWeightBelowBmiThirty() {
        val bmi = CalorieEstimationModel.calculateBmi(heightCm = 170.0, weightKg = 80.0)

        assertEquals(80.0, NutritionTargetModel.proteinReferenceWeight(80.0, 170.0, bmi), 0.0)
    }

    @Test
    fun proteinReferenceWeight_usesAdjustedWeightAtBmiThirtyOrAbove() {
        val bmi = CalorieEstimationModel.calculateBmi(heightCm = 170.0, weightKg = 110.0)

        assertEquals(81.7, NutritionTargetModel.proteinReferenceWeight(110.0, 170.0, bmi), 0.1)
    }

    @Test
    fun proteinFactors_followGoalAndAgeRules() {
        assertEquals(Pair(1.2, 1.0), NutritionTargetModel.proteinFactorsFromGoal(CalorieGoal.MAINTAIN, age = 29))
        assertEquals(Pair(1.6, 1.4), NutritionTargetModel.proteinFactorsFromGoal(CalorieGoal.LOSE_WEIGHT, age = 29))
        assertEquals(Pair(1.7, 1.5), NutritionTargetModel.proteinFactorsFromGoal(CalorieGoal.GAIN_MUSCLE, age = 29))
        assertEquals(Pair(1.6, 1.4), NutritionTargetModel.proteinFactorsFromGoal(CalorieGoal.RECOMPOSITION, age = 29))
        assertEquals(Pair(1.2, 1.0), NutritionTargetModel.proteinFactorsFromGoal(CalorieGoal.MAINTAIN, age = 70))
    }

    @Test
    fun proteinTarget_isCappedAtThirtyFivePercentOfCalories() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                calorieTarget = CalorieRange(low = 1100, mid = 1200, high = 1300),
                heightCm = 190.0,
                weightKg = 120.0,
                goal = CalorieGoal.GAIN_MUSCLE
            )
        )

        assertEquals(105, result.macros!!.protein.targetG)
        assertTrue(NutritionFlag.PROTEIN_REDUCED_TO_AMDR_CAP in result.flags)
    }

    @Test
    fun baseFatPercent_followsGoal() {
        assertEquals(0.30, NutritionTargetModel.baseFatPercentFromGoal(CalorieGoal.MAINTAIN), 0.0)
        assertEquals(0.28, NutritionTargetModel.baseFatPercentFromGoal(CalorieGoal.LOSE_WEIGHT), 0.0)
        assertEquals(0.27, NutritionTargetModel.baseFatPercentFromGoal(CalorieGoal.GAIN_MUSCLE), 0.0)
        assertEquals(0.28, NutritionTargetModel.baseFatPercentFromGoal(CalorieGoal.RECOMPOSITION), 0.0)
    }

    @Test
    fun fatAdjustment_usesOnlyStepCount() {
        assertEquals(0.02, NutritionTargetModel.fatAdjustmentFromSteps(2_999), 0.0)
        assertEquals(0.0, NutritionTargetModel.fatAdjustmentFromSteps(3_000), 0.0)
        assertEquals(0.0, NutritionTargetModel.fatAdjustmentFromSteps(8_999), 0.0)
        assertEquals(-0.02, NutritionTargetModel.fatAdjustmentFromSteps(9_000), 0.0)
        assertEquals(-0.03, NutritionTargetModel.fatAdjustmentFromSteps(12_000), 0.0)
        assertEquals(-0.05, NutritionTargetModel.fatAdjustmentFromSteps(16_000), 0.0)
        assertEquals(0.0, NutritionTargetModel.fatAdjustmentFromSteps(null), 0.0)
    }

    @Test
    fun staticModel_buildsProteinFatAndCarbsFromCalorieBudget() {
        val result = NutritionTargetModel.estimate(baseInput())
        val macros = result.macros!!

        assertEquals(ModelStatus.OK, result.status)
        assertEquals("static_from_initial_calories", result.source)
        assertEquals(MacroPreference.BALANCED, result.appliedMacroPreference)
        assertEquals(130, macros.protein.targetG)
        assertEquals(60, macros.fat.targetG)
        assertEquals(260, macros.carbohydrate.targetG)
        assertEquals(24, macros.protein.percent)
        assertEquals(26, macros.fat.percent)
        assertEquals(50, macros.carbohydrate.percent)
        assertTrue(NutritionFlag.NOT_ENOUGH_MACRO_LOGS in result.flags)
    }

    @Test
    fun macroPreference_missingDefaultsToBalanced() {
        val result = NutritionTargetModel.estimate(baseInput(macroPreference = null))

        assertEquals(MacroPreference.BALANCED, result.appliedMacroPreference)
    }

    @Test
    fun macroPreference_higherCarbReducesFatAndIncreasesCarbs() {
        val balanced = NutritionTargetModel.estimate(baseInput(macroPreference = MacroPreference.BALANCED)).macros!!
        val higherCarb = NutritionTargetModel.estimate(baseInput(macroPreference = MacroPreference.HIGHER_CARB)).macros!!

        assertTrue(higherCarb.fat.targetG < balanced.fat.targetG)
        assertTrue(higherCarb.carbohydrate.targetG > balanced.carbohydrate.targetG)
        assertEquals(50, higherCarb.fat.targetG)
        assertEquals(280, higherCarb.carbohydrate.targetG)
    }

    @Test
    fun macroPreference_lowerCarbIncreasesFatAndReducesCarbs() {
        val balanced = NutritionTargetModel.estimate(baseInput(macroPreference = MacroPreference.BALANCED)).macros!!
        val lowerCarb = NutritionTargetModel.estimate(baseInput(macroPreference = MacroPreference.LOWER_CARB)).macros!!

        assertTrue(lowerCarb.fat.targetG > balanced.fat.targetG)
        assertTrue(lowerCarb.carbohydrate.targetG < balanced.carbohydrate.targetG)
        assertTrue(lowerCarb.fat.percent <= 35)
    }

    @Test
    fun macroPreference_higherProteinRaisesProteinButRespectsThirtyFivePercentCap() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                calorieTarget = CalorieRange(low = 1100, mid = 1200, high = 1300),
                heightCm = 190.0,
                weightKg = 120.0,
                goal = CalorieGoal.GAIN_MUSCLE,
                macroPreference = MacroPreference.HIGHER_PROTEIN
            )
        )

        assertEquals(105, result.macros!!.protein.targetG)
        assertEquals(35, result.macros.protein.percent)
        assertTrue(NutritionFlag.PROTEIN_REDUCED_TO_AMDR_CAP in result.macroPreferenceFlags)
    }

    @Test
    fun macroPreference_higherFatRaisesFatButRespectsThirtyFivePercentCap() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                calorieTarget = CalorieRange(low = 1600, mid = 1800, high = 2000),
                avgSteps = 1_500,
                macroPreference = MacroPreference.HIGHER_FAT
            )
        )

        assertTrue(result.macros!!.fat.percent <= 35)
        assertEquals(35, result.macros.fat.percent)
    }

    @Test
    fun macroPreference_keepsFatInsideAmdrBoundsAndCarbsAsRemainingCalories() {
        val result = NutritionTargetModel.estimate(baseInput(macroPreference = MacroPreference.HIGHER_CARB))
        val macros = result.macros!!
        val totalMacroCalories = macros.protein.kcal + macros.fat.kcal + macros.carbohydrate.kcal

        assertTrue(macros.fat.percent in 20..35)
        assertEquals(result.calorieTarget.mid.toDouble(), totalMacroCalories.toDouble(), 2.0)
    }

    @Test
    fun macroPreference_lowerCarbCopiesLowCarbWarningToPreferenceFlags() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                calorieTarget = CalorieRange(low = 1100, mid = 1200, high = 1300),
                goal = CalorieGoal.LOSE_WEIGHT,
                macroPreference = MacroPreference.LOWER_CARB
            )
        )

        assertTrue(result.macros!!.carbohydrate.targetG < 130)
        assertTrue(NutritionFlag.CARBOHYDRATE_BELOW_DEFAULT_RDA_WARNING in result.flags)
        assertTrue(NutritionFlag.CARBOHYDRATE_BELOW_DEFAULT_RDA_WARNING in result.macroPreferenceFlags)
    }

    @Test
    fun macroPreference_doesNotAffectCalorieTarget() {
        val balanced = NutritionTargetModel.estimate(baseInput(macroPreference = MacroPreference.BALANCED))
        val higherFat = NutritionTargetModel.estimate(baseInput(macroPreference = MacroPreference.HIGHER_FAT))

        assertEquals(balanced.calorieTarget, higherFat.calorieTarget)
    }

    @Test
    fun lowCarbohydrateWarning_isAddedBelowDefaultRda() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                calorieTarget = CalorieRange(low = 1100, mid = 1200, high = 1300),
                goal = CalorieGoal.LOSE_WEIGHT
            )
        )

        assertTrue(result.macros!!.carbohydrate.targetG < 130)
        assertTrue(NutritionFlag.CARBOHYDRATE_BELOW_DEFAULT_RDA_WARNING in result.flags)
    }

    @Test
    fun limitedSafetyMode_returnsNoMacros() {
        val result = NutritionTargetModel.estimate(
            baseInput(safetyFlags = setOf(ModelFlag.EATING_DISORDER_HISTORY))
        )

        assertEquals(ModelStatus.LIMITED, result.status)
        assertEquals(null, result.macros)
        assertTrue(NutritionFlag.LIMITED_SAFETY_MODE in result.flags)
    }

    @Test
    fun adaptiveModel_usesTenOrMoreValidMacroDays() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                calorieTargetSource = CalorieTargetSource.ADAPTIVE,
                nutritionLogs = macroLogs(days = 10, calories = 2100.0, protein = 150.0, fat = 80.0, carbs = 205.0)
            )
        )
        val macros = result.macros!!

        assertEquals("adaptive_macro_from_adaptive_calories", result.source)
        assertEquals(0.25, result.adaptationWeight, 0.0)
        assertEquals(130, macros.protein.targetG)
        assertEquals(65, macros.fat.targetG)
        assertEquals(250, macros.carbohydrate.targetG)
    }

    @Test
    fun adaptiveProtein_doesNotAdaptBelowMinimum() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                nutritionLogs = macroLogs(days = 20, calories = 2100.0, protein = 70.0, fat = 80.0, carbs = 260.0)
            )
        )

        assertEquals(130, result.macros!!.protein.targetG)
        assertTrue(NutritionFlag.OBSERVED_PROTEIN_BELOW_MINIMUM in result.flags)
    }

    @Test
    fun adaptiveFatCarbSplitAdaptsButStaysInsideFatBounds() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                nutritionLogs = macroLogs(days = 40, calories = 2100.0, protein = 130.0, fat = 110.0, carbs = 147.5)
            )
        )

        assertEquals(0.60, result.adaptationWeight, 0.0)
        assertEquals(80, result.macros!!.fat.targetG)
        assertEquals(35, result.macros.fat.percent)
    }

    @Test
    fun highStepsCapAdaptiveFatToPreserveCarbs() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                avgSteps = 16_500,
                nutritionLogs = macroLogs(days = 40, calories = 2100.0, protein = 130.0, fat = 100.0, carbs = 170.0)
            )
        )

        assertEquals(65, result.macros!!.fat.targetG)
        assertEquals(27, result.macros.fat.percent)
        assertTrue(NutritionFlag.HIGH_STEP_FAT_CAP_APPLIED in result.flags)
    }

    @Test
    fun adaptiveCalorieRange_recalculatesMacrosWithoutEstimatingCalories() {
        val result = NutritionTargetModel.estimate(
            baseInput(
                calorieTarget = CalorieRange(low = 1800, mid = 2000, high = 2200),
                calorieTargetSource = CalorieTargetSource.ADAPTIVE
            )
        )

        assertEquals(CalorieRange(low = 1800, mid = 2000, high = 2200), result.calorieTarget)
        assertEquals(130, result.macros!!.protein.targetG)
        assertEquals(60, result.macros.fat.targetG)
        assertEquals(240, result.macros.carbohydrate.targetG)
    }

    @Test
    fun repeatedEstimate_isDeterministic() {
        val input = baseInput(
        nutritionLogs = macroLogs(days = 28, calories = 2050.0, protein = 140.0, fat = 70.0, carbs = 220.0)
        )

        assertEquals(NutritionTargetModel.estimate(input), NutritionTargetModel.estimate(input))
    }

    private fun macroLogs(
        days: Int,
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double
    ): List<NutritionLogDay> {
        return (1..days).map { day ->
            NutritionLogDay(
                dayIndex = day,
                calories = calories,
                proteinG = protein,
                fatG = fat,
                carbohydrateG = carbs
            )
        }
    }

    @Test
    fun medicalConditionAppliesMaintenanceAndWithholdsMacroTargets() {
        val input = baseInput()
        val calories = CalorieEstimationModel.estimate(
            CalorieModelInput(
                age = input.age, weightKg = input.weightKg, heightCm = input.heightCm,
                sexFormula = input.sexFormula, goal = CalorieGoal.LOSE_WEIGHT,
                safetyInputs = SafetyInputs(hasRelevantMedicalCondition = true)
            )
        )
        val nutrition = NutritionTargetModel.estimate(
            input.copy(goal = calories.goalTarget.goal, safetyFlags = calories.flags)
        )
        assertEquals(CalorieGoal.MAINTAIN, calories.goalTarget.goal)
        assertEquals(ModelStatus.LIMITED, nutrition.status)
        assertEquals(null, nutrition.macros)
    }

    private fun baseInput(
        calorieTarget: CalorieRange = CalorieRange(low = 1900, mid = 2100, high = 2300),
        calorieTargetSource: CalorieTargetSource = CalorieTargetSource.INITIAL,
        calorieConfidence: ModelConfidence = ModelConfidence.MEDIUM,
        weightKg: Double = 80.0,
        heightCm: Double = 170.0,
        age: Int = 29,
        sexFormula: SexFormula = SexFormula.MALE,
        goal: CalorieGoal = CalorieGoal.LOSE_WEIGHT,
        avgSteps: Int? = 10_500,
        stepConfidence: StepConfidence = StepConfidence.MEDIUM_LOW,
        weightTrendQuality: WeightTrendQuality = WeightTrendQuality.MEDIUM,
        stepStability: StepStability = StepStability.STABLE,
        safetyFlags: Set<ModelFlag> = emptySet(),
        nutritionLogs: List<NutritionLogDay> = emptyList(),
        macroPreference: MacroPreference? = null
    ): NutritionTargetInput {
        return NutritionTargetInput(
            calorieTarget = calorieTarget,
            calorieTargetSource = calorieTargetSource,
            calorieConfidence = calorieConfidence,
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            sexFormula = sexFormula,
            goal = goal,
            avgSteps = avgSteps,
            stepConfidence = stepConfidence,
            weightTrendQuality = weightTrendQuality,
            stepStability = stepStability,
            safetyFlags = safetyFlags,
            nutritionLogs = nutritionLogs,
            macroPreference = macroPreference
        )
    }
}
