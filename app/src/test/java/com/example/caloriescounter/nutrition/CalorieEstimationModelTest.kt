package com.example.caloriescounter.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieEstimationModelTest {
    @Test
    fun mifflinStJeor_calculatesMaleAndFemaleRmr() {
        assertEquals(
            1673.75,
            CalorieEstimationModel.calculateRmr(
                sexFormula = SexFormula.MALE,
                weightKg = 70.0,
                heightCm = 175.0,
                age = 25
            ),
            0.0
        )
        assertEquals(
            1507.75,
            CalorieEstimationModel.calculateRmr(
                sexFormula = SexFormula.FEMALE,
                weightKg = 70.0,
                heightCm = 175.0,
                age = 25
            ),
            0.0
        )
    }

    @Test
    fun bmiCalculationAndUnderweightSafetyFlag() {
        assertEquals(24.22, CalorieEstimationModel.calculateBmi(heightCm = 170.0, weightKg = 70.0), 0.01)

        val result = CalorieEstimationModel.estimate(
            baseInput(
                heightCm = 170.0,
                weightKg = 52.0,
                goal = CalorieGoal.LOSE_WEIGHT
            )
        )

        assertEquals(ModelStatus.LIMITED, result.status)
        assertTrue(ModelFlag.UNDERWEIGHT_BMI in result.flags)
        assertEquals(CalorieGoal.MAINTAIN, result.goalTarget.goal)
        assertTrue(result.goalTarget.limited)
    }

    @Test
    fun stepFactorMapping_coversAllBoundaries() {
        assertEquals(1.20, CalorieEstimationModel.factorFromSteps(0), 0.0)
        assertEquals(1.20, CalorieEstimationModel.factorFromSteps(2_999), 0.0)
        assertEquals(1.28, CalorieEstimationModel.factorFromSteps(3_000), 0.0)
        assertEquals(1.28, CalorieEstimationModel.factorFromSteps(5_999), 0.0)
        assertEquals(1.38, CalorieEstimationModel.factorFromSteps(6_000), 0.0)
        assertEquals(1.38, CalorieEstimationModel.factorFromSteps(8_999), 0.0)
        assertEquals(1.48, CalorieEstimationModel.factorFromSteps(9_000), 0.0)
        assertEquals(1.48, CalorieEstimationModel.factorFromSteps(11_999), 0.0)
        assertEquals(1.60, CalorieEstimationModel.factorFromSteps(12_000), 0.0)
        assertEquals(1.60, CalorieEstimationModel.factorFromSteps(15_999), 0.0)
        assertEquals(1.70, CalorieEstimationModel.factorFromSteps(16_000), 0.0)
    }

    @Test
    fun stepSourcePriority_usesFourteenValidPhoneDaysAsHighConfidence() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                stepRecords = List(14) { StepRecord(steps = 7_000, isCompleteDay = true, deviceActive = true) },
                manualStepRange = ManualStepRange.LESS_THAN_3000
            )
        )

        assertEquals(7_000, result.avgSteps)
        assertEquals(StepConfidence.HIGH, result.stepConfidence)
        assertEquals(1.38, result.lifestyleFactor, 0.0)
    }

    @Test
    fun stepSourcePriority_usesSevenToThirteenPhoneDaysAsMediumConfidence() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                stepRecords = List(7) { StepRecord(steps = 9_000, isCompleteDay = true) },
                manualStepRange = ManualStepRange.LESS_THAN_3000
            )
        )

        assertEquals(9_000, result.avgSteps)
        assertEquals(StepConfidence.MEDIUM, result.stepConfidence)
        assertEquals(1.48, result.lifestyleFactor, 0.0)
    }

    @Test
    fun stepSourcePriority_fallsBackToUserEstimateWhenPhoneDaysAreInsufficient() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                stepRecords = listOf(
                    StepRecord(steps = 12_000, isCompleteDay = true),
                    StepRecord(steps = 20_000, isCompleteDay = false),
                    StepRecord(steps = 499, isCompleteDay = true),
                    StepRecord(steps = 8_000, isCompleteDay = true, deviceActive = false)
                ),
                manualStepRange = ManualStepRange.FROM_6000_TO_8999
            )
        )

        assertEquals(7_500, result.avgSteps)
        assertEquals(StepConfidence.MEDIUM_LOW, result.stepConfidence)
        assertEquals(1.38, result.lifestyleFactor, 0.0)
        assertTrue(ModelFlag.MANUAL_STEP_RANGE_USED in result.flags)
    }

    @Test
    fun stepSourcePriority_marksEstimateRequiredWhenNoUsableStepsOrEstimateExist() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                stepRecords = emptyList(),
                manualStepRange = null
            )
        )

        assertEquals(null, result.avgSteps)
        assertEquals(StepConfidence.LOW, result.stepConfidence)
        assertEquals(1.20, result.lifestyleFactor, 0.0)
        assertTrue(ModelFlag.STEP_ESTIMATE_REQUIRED in result.flags)
    }

    @Test
    fun initialEstimate_usesOnlyStepsNotWorkoutOrOccupation() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                age = 29,
                heightCm = 170.0,
                weightKg = 80.0,
                sexFormula = SexFormula.MALE,
                goal = CalorieGoal.LOSE_WEIGHT,
                manualStepRange = ManualStepRange.FROM_9000_TO_11999
            )
        )

        assertEquals(1725, result.rmr)
        assertEquals(1.48, result.lifestyleFactor, 0.0)
        assertEquals(2550, result.estimatedMaintenance.mid)
        assertEquals(2150, result.goalTarget.target)
        assertEquals(1.48, result.palTotal, 0.0)
    }

    @Test
    fun goalPace_missingDefaultsToStandard() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                goal = CalorieGoal.LOSE_WEIGHT,
                manualStepRange = ManualStepRange.FROM_9000_TO_11999
            )
        )

        assertEquals(GoalPace.STANDARD, result.appliedGoalPace)
        assertEquals(2050, result.goalTarget.target)
    }

    @Test
    fun goalPace_slowWeightLossCreatesSmallerDeficitThanStandard() {
        val slow = CalorieEstimationModel.estimate(
            baseInput(goal = CalorieGoal.LOSE_WEIGHT, goalPace = GoalPace.SLOW)
        )
        val standard = CalorieEstimationModel.estimate(
            baseInput(goal = CalorieGoal.LOSE_WEIGHT, goalPace = GoalPace.STANDARD)
        )

        assertTrue(slow.goalTarget.deficit!! < standard.goalTarget.deficit!!)
        assertTrue(slow.goalTarget.target > standard.goalTarget.target)
    }

    @Test
    fun goalPace_fasterWeightLossCreatesLargerDeficitButRespectsFloor() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                age = 60,
                heightCm = 155.0,
                weightKg = 45.0,
                sexFormula = SexFormula.FEMALE,
                goal = CalorieGoal.LOSE_WEIGHT,
                goalPace = GoalPace.FASTER,
                manualStepRange = ManualStepRange.LESS_THAN_3000
            )
        )

        assertEquals(GoalPace.FASTER, result.appliedGoalPace)
        assertTrue(result.goalTarget.target >= 1_200)
        assertTrue(ModelFlag.DEFICIT_REDUCED_DUE_TO_FLOOR in result.flags)
        assertTrue(ModelFlag.GOAL_PACE_REDUCED_FOR_SAFETY in result.goalPaceFlags)
    }

    @Test
    fun goalPace_isReducedWhenSafetyFlagsApply() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                goal = CalorieGoal.LOSE_WEIGHT,
                goalPace = GoalPace.FASTER,
                safetyInputs = SafetyInputs(hasRelevantMedicalCondition = true)
            )
        )

        assertEquals(ModelStatus.LIMITED, result.status)
        assertEquals(GoalPace.STANDARD, result.appliedGoalPace)
        assertEquals(CalorieGoal.MAINTAIN, result.goalTarget.goal)
        assertTrue(ModelFlag.GOAL_PACE_REDUCED_FOR_SAFETY in result.goalPaceFlags)
    }

    @Test
    fun goalPace_isIgnoredForMaintain() {
        val standard = CalorieEstimationModel.estimate(
            baseInput(goal = CalorieGoal.MAINTAIN, goalPace = GoalPace.STANDARD)
        )
        val faster = CalorieEstimationModel.estimate(
            baseInput(goal = CalorieGoal.MAINTAIN, goalPace = GoalPace.FASTER)
        )

        assertEquals(standard.goalTarget, faster.goalTarget)
        assertEquals(GoalPace.STANDARD, faster.appliedGoalPace)
    }

    @Test
    fun targetWeight_lowerThanCurrentIsValidForWeightLoss() {
        val result = CalorieEstimationModel.estimate(
            baseInput(goal = CalorieGoal.LOSE_WEIGHT, weightKg = 80.0, targetWeightKg = 75.0)
        )

        assertNotNull(result.targetWeight)
        assertEquals(-5.0, result.targetWeight!!.deltaKg, 0.0)
        assertTrue(ModelFlag.TARGET_WEIGHT_CONFLICTS_WITH_GOAL !in result.flags)
    }

    @Test
    fun targetWeight_higherThanCurrentConflictsWithWeightLoss() {
        val result = CalorieEstimationModel.estimate(
            baseInput(goal = CalorieGoal.LOSE_WEIGHT, weightKg = 80.0, targetWeightKg = 85.0)
        )

        assertTrue(ModelFlag.TARGET_WEIGHT_CONFLICTS_WITH_GOAL in result.flags)
    }

    @Test
    fun targetWeight_underweightTargetCreatesWarning() {
        val result = CalorieEstimationModel.estimate(
            baseInput(goal = CalorieGoal.LOSE_WEIGHT, heightCm = 170.0, weightKg = 80.0, targetWeightKg = 52.0)
        )

        assertTrue(result.targetWeight!!.targetBMI < 18.5)
        assertTrue(ModelFlag.TARGET_WEIGHT_UNDERWEIGHT_WARNING in result.flags)
    }

    @Test
    fun targetWeight_createsMetadataWithDeltaBmiAndTimeline() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                goal = CalorieGoal.LOSE_WEIGHT,
                weightKg = 80.0,
                heightCm = 170.0,
                targetWeightKg = 76.0,
                goalPace = GoalPace.STANDARD
            )
        )

        val targetWeight = result.targetWeight!!
        assertEquals(76.0, targetWeight.targetWeightKg, 0.0)
        assertEquals(26.3, targetWeight.targetBMI, 0.0)
        assertEquals(-4.0, targetWeight.deltaKg, 0.0)
        assertNotNull(targetWeight.estimatedWeeksLow)
        assertNotNull(targetWeight.estimatedWeeksHigh)
    }

    @Test
    fun targetWeight_missingDoesNotAffectCalories() {
        val withoutTarget = CalorieEstimationModel.estimate(
            baseInput(goal = CalorieGoal.LOSE_WEIGHT, targetWeightKg = null)
        )
        val withTarget = CalorieEstimationModel.estimate(
            baseInput(goal = CalorieGoal.LOSE_WEIGHT, targetWeightKg = 68.0)
        )

        assertNull(withoutTarget.targetWeight)
        assertEquals(withoutTarget.goalTarget, withTarget.goalTarget)
    }

    @Test
    fun recentWeightChange_handlesStableAndNotSure() {
        val stable = CalorieEstimationModel.calculateRecentWeightChange(
            RecentWeightChangeInput(status = RecentWeightStatus.STABLE),
            currentWeightKg = 80.0
        )
        val notSure = CalorieEstimationModel.calculateRecentWeightChange(
            RecentWeightChangeInput(status = RecentWeightStatus.NOT_SURE),
            currentWeightKg = 80.0
        )

        assertEquals(0.0, stable.changeKg ?: -1.0, 0.0)
        assertEquals(0.0, stable.changePercent ?: -1.0, 0.0)
        assertEquals(null, notSure.changeKg)
        assertEquals(null, notSure.weeklyRatePercent)
    }

    @Test
    fun recentWeightChange_convertsLostKgOverMonths() {
        val result = CalorieEstimationModel.calculateRecentWeightChange(
            RecentWeightChangeInput(
                status = RecentWeightStatus.LOST_WEIGHT,
                amount = 4.0,
                unit = WeightUnit.KG,
                timeframeValue = 2.0,
                timeframeUnit = TimeframeUnit.MONTHS
            ),
            currentWeightKg = 80.0
        )

        assertEquals(-4.0, result.changeKg ?: 0.0, 0.0)
        assertEquals(-5.0, result.changePercent ?: 0.0, 0.01)
        assertEquals(-0.58, result.weeklyRatePercent ?: 0.0, 0.01)
    }

    @Test
    fun recentWeightChange_convertsGainedPoundsOverWeeks() {
        val result = CalorieEstimationModel.calculateRecentWeightChange(
            RecentWeightChangeInput(
                status = RecentWeightStatus.GAINED_WEIGHT,
                amount = 5.0,
                unit = WeightUnit.LB,
                timeframeValue = 8.0,
                timeframeUnit = TimeframeUnit.WEEKS
            ),
            currentWeightKg = 80.0
        )

        assertEquals(2.27, result.changeKg ?: 0.0, 0.01)
        assertEquals(2.83, result.changePercent ?: 0.0, 0.01)
        assertEquals(0.35, result.weeklyRatePercent ?: 0.0, 0.01)
    }

    @Test
    fun recentWeightChange_shortTimeframeCreatesLowConfidenceFlag() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                recentWeightChange = RecentWeightChangeInput(
                    status = RecentWeightStatus.LOST_WEIGHT,
                    amount = 2.0,
                    unit = WeightUnit.KG,
                    timeframeValue = 10.0,
                    timeframeUnit = TimeframeUnit.DAYS
                )
            )
        )

        assertTrue(ModelFlag.SHORT_RECENT_WEIGHT_TIMEFRAME in result.flags)
        assertEquals(ModelConfidence.LOW, result.confidence)
    }

    @Test
    fun maintenanceRange_usesHighConfidenceTwelvePercentRange() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                stepRecords = List(14) { StepRecord(steps = 7_500, isCompleteDay = true) },
                recentWeightChange = RecentWeightChangeInput(status = RecentWeightStatus.STABLE)
            )
        )

        assertEquals(ModelConfidence.HIGH, result.confidence)
        assertEquals(2_250, result.estimatedMaintenance.mid)
        assertEquals(1_950, result.estimatedMaintenance.low)
        assertEquals(2_500, result.estimatedMaintenance.high)
    }

    @Test
    fun weightLossTarget_doesNotGoBelowCalorieFloor() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                age = 60,
                heightCm = 155.0,
                weightKg = 45.0,
                sexFormula = SexFormula.FEMALE,
                goal = CalorieGoal.LOSE_WEIGHT,
                manualStepRange = ManualStepRange.LESS_THAN_3000
            )
        )

        assertEquals(CalorieGoal.LOSE_WEIGHT, result.goalTarget.goal)
        assertTrue(result.goalTarget.target >= 1_200)
        assertTrue(ModelFlag.DEFICIT_REDUCED_DUE_TO_FLOOR in result.flags)
    }

    @Test
    fun limitedMode_replacesAggressiveGoalWithMaintenance() {
        val result = CalorieEstimationModel.estimate(
            baseInput(
                goal = CalorieGoal.GAIN_MUSCLE,
                safetyInputs = SafetyInputs(hasEatingDisorderHistory = true)
            )
        )

        assertEquals(ModelStatus.LIMITED, result.status)
        assertEquals(CalorieGoal.MAINTAIN, result.goalTarget.goal)
        assertTrue(result.goalTarget.limited)
        assertTrue(ModelFlag.EATING_DISORDER_HISTORY in result.flags)
        assertTrue(ModelFlag.LIMITED_MODE_GOAL_REPLACED_WITH_MAINTENANCE in result.flags)
    }

    @Test
    fun adaptiveModel_returnsInitialEstimateWhenDataIsInsufficient() {
        val result = AdaptiveCalorieModel.update(
            AdaptiveCalorieInput(
                initialMaintenanceMid = 2550,
                initialAvgSteps = 9_000,
                goal = CalorieGoal.LOSE_WEIGHT,
                sexFormula = SexFormula.MALE,
                calorieLogs = calorieLogs(days = 7, calories = 2_050.0, steps = 9_000),
                weightMeasurements = listOf(WeightMeasurement(dayIndex = 0, weightKg = 80.0))
            )
        )

        assertEquals(ModelStatus.LIMITED, result.status)
        assertEquals(0.0, result.adaptiveWeight, 0.0)
        assertEquals(2550, result.updatedMaintenance.mid)
        assertTrue(AdaptiveFlag.INSUFFICIENT_DATA in result.flags)
    }

    @Test
    fun adaptiveModel_stableWeightAtLowerIntakePullsMaintenanceTowardObservedValue() {
        val result = AdaptiveCalorieModel.update(
            AdaptiveCalorieInput(
                initialMaintenanceMid = 2550,
                initialAvgSteps = 9_000,
                goal = CalorieGoal.LOSE_WEIGHT,
                sexFormula = SexFormula.MALE,
                calorieLogs = calorieLogs(days = 28, calories = 2_050.0, steps = 9_000),
                weightMeasurements = listOf(
                    WeightMeasurement(dayIndex = 0, weightKg = 80.0),
                    WeightMeasurement(dayIndex = 28, weightKg = 80.0)
                )
            )
        )

        assertEquals(ModelStatus.OK, result.status)
        assertEquals(2050, result.averageIntake)
        assertEquals(2050, result.observedMaintenance)
        assertEquals(0.5, result.adaptiveWeight, 0.0)
        assertEquals(2300, result.updatedMaintenance.mid)
        assertEquals(1950, result.updatedGoalTarget.target)
    }

    @Test
    fun adaptiveModel_adjustsObservedMaintenanceWhenTargetStepsDiffer() {
        val result = AdaptiveCalorieModel.update(
            AdaptiveCalorieInput(
                initialMaintenanceMid = 2550,
                initialAvgSteps = 9_000,
                targetAvgSteps = 12_000,
                goal = CalorieGoal.MAINTAIN,
                sexFormula = SexFormula.MALE,
                calorieLogs = calorieLogs(days = 28, calories = 2_050.0, steps = 9_000),
                weightMeasurements = listOf(
                    WeightMeasurement(dayIndex = 0, weightKg = 80.0),
                    WeightMeasurement(dayIndex = 28, weightKg = 80.0)
                )
            )
        )

        assertEquals(2050, result.observedMaintenance)
        assertEquals(2200, result.stepAdjustedObservedMaintenance)
        assertEquals(2400, result.updatedMaintenance.mid)
        assertTrue(AdaptiveFlag.STEP_ADJUSTED in result.flags)
    }

    @Test
    fun adaptiveModel_flagsNonStandardizedWeightMeasurementsForFutureUi() {
        val result = AdaptiveCalorieModel.update(
            AdaptiveCalorieInput(
                initialMaintenanceMid = 2550,
                initialAvgSteps = 9_000,
                goal = CalorieGoal.MAINTAIN,
                sexFormula = SexFormula.MALE,
                calorieLogs = calorieLogs(days = 28, calories = 2_050.0, steps = 9_000),
                weightMeasurements = listOf(
                    WeightMeasurement(dayIndex = 0, weightKg = 80.0, measuredInSimilarConditions = true),
                    WeightMeasurement(dayIndex = 28, weightKg = 80.0, measuredInSimilarConditions = false)
                )
            )
        )

        assertTrue(AdaptiveFlag.WEIGHT_MEASUREMENTS_NOT_STANDARDIZED in result.flags)
        assertTrue(NutritionModelUiNotes.WEIGHT_MEASUREMENT_CONSISTENCY.contains("after waking up"))
    }

    @Test
    fun repeatedEstimates_areDeterministic() {
        val initialInput = baseInput(
            goal = CalorieGoal.RECOMPOSITION,
            stepRecords = List(14) { StepRecord(steps = 10_000 + it, isCompleteDay = true) }
        )
        val adaptiveInput = AdaptiveCalorieInput(
            initialMaintenanceMid = 2550,
            initialAvgSteps = 9_000,
            goal = CalorieGoal.RECOMPOSITION,
            sexFormula = SexFormula.MALE,
            calorieLogs = calorieLogs(days = 56, calories = 2_250.0, steps = 10_000),
            weightMeasurements = listOf(
                WeightMeasurement(dayIndex = 0, weightKg = 80.0),
                WeightMeasurement(dayIndex = 56, weightKg = 79.0)
            )
        )

        assertEquals(
            CalorieEstimationModel.estimate(initialInput),
            CalorieEstimationModel.estimate(initialInput)
        )
        assertEquals(
            AdaptiveCalorieModel.update(adaptiveInput),
            AdaptiveCalorieModel.update(adaptiveInput)
        )
    }

    private fun calorieLogs(days: Int, calories: Double, steps: Int): List<DailyCalorieLog> {
        return (1..days).map { day ->
            DailyCalorieLog(dayIndex = day, calories = calories, avgSteps = steps)
        }
    }

    private fun baseInput(
        age: Int = 35,
        heightCm: Double = 175.0,
        weightKg: Double = 70.0,
        sexFormula: SexFormula = SexFormula.MALE,
        goal: CalorieGoal = CalorieGoal.MAINTAIN,
        safetyInputs: SafetyInputs = SafetyInputs(),
        manualStepRange: ManualStepRange? = ManualStepRange.FROM_6000_TO_8999,
        stepRecords: List<StepRecord> = emptyList(),
        recentWeightChange: RecentWeightChangeInput = RecentWeightChangeInput(),
        trainingExperience: TrainingExperience = TrainingExperience.BEGINNER,
        goalPace: GoalPace? = null,
        targetWeightKg: Double? = null
    ): CalorieModelInput {
        return CalorieModelInput(
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            sexFormula = sexFormula,
            goal = goal,
            safetyInputs = safetyInputs,
            manualStepRange = manualStepRange,
            stepRecords = stepRecords,
            recentWeightChange = recentWeightChange,
            trainingExperience = trainingExperience,
            goalPace = goalPace,
            targetWeightKg = targetWeightKg
        )
    }
}
