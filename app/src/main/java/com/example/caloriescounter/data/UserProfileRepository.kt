package com.example.caloriescounter.data

import android.content.Context
import androidx.core.content.edit
import com.example.caloriescounter.nutrition.CalorieGoal
import com.example.caloriescounter.nutrition.GoalPace
import com.example.caloriescounter.nutrition.MacroPreference
import com.example.caloriescounter.nutrition.ManualStepRange
import com.example.caloriescounter.nutrition.RecentWeightStatus
import com.example.caloriescounter.nutrition.SexFormula
import com.example.caloriescounter.nutrition.TimeframeUnit
import com.example.caloriescounter.nutrition.WeightUnit

data class UserProfile(
    val nickname: String,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val sexFormula: SexFormula,
    val goal: CalorieGoal,
    val goalPace: GoalPace = GoalPace.STANDARD,
    val targetWeightKg: Double? = null,
    val macroPreference: MacroPreference = MacroPreference.BALANCED,
    val manualStepRange: ManualStepRange,
    val avgStepGoal: Int? = null,
    val recentWeightStatus: RecentWeightStatus,
    val recentWeightAmount: Double? = null,
    val recentWeightUnit: WeightUnit? = null,
    val recentWeightTimeframeValue: Double? = null,
    val recentWeightTimeframeUnit: TimeframeUnit? = null,
    val isPregnant: Boolean = false,
    val isBreastfeeding: Boolean = false,
    val hasEatingDisorderHistory: Boolean = false,
    val hasRelevantMedicalCondition: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

class UserProfileRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasProfile(): Boolean = prefs.getBoolean(KEY_PROFILE_CREATED, false)

    fun saveProfile(profile: UserProfile) {
        prefs.edit {
            putBoolean(KEY_PROFILE_CREATED, true)
            putString(KEY_NICKNAME, profile.nickname)
            putInt(KEY_AGE, profile.age)
            putString(KEY_HEIGHT_CM, profile.heightCm.toString())
            putString(KEY_WEIGHT_KG, profile.weightKg.toString())
            putString(KEY_SEX_FORMULA, profile.sexFormula.name)
            putString(KEY_GOAL, profile.goal.name)
            putString(KEY_GOAL_PACE, profile.goalPace.name)
            putString(KEY_TARGET_WEIGHT_KG, profile.targetWeightKg?.toString())
            putString(KEY_MACRO_PREFERENCE, profile.macroPreference.name)
            putString(KEY_STEP_RANGE, profile.manualStepRange.name)
            putInt(KEY_AVG_STEP_GOAL, profile.avgStepGoal ?: profile.manualStepRange.midpoint)
            putString(KEY_RECENT_WEIGHT_STATUS, profile.recentWeightStatus.name)
            putString(KEY_RECENT_WEIGHT_AMOUNT, profile.recentWeightAmount?.toString())
            putString(KEY_RECENT_WEIGHT_UNIT, profile.recentWeightUnit?.name)
            putString(KEY_RECENT_WEIGHT_TIMEFRAME_VALUE, profile.recentWeightTimeframeValue?.toString())
            putString(KEY_RECENT_WEIGHT_TIMEFRAME_UNIT, profile.recentWeightTimeframeUnit?.name)
            putBoolean(KEY_PREGNANT, profile.isPregnant)
            putBoolean(KEY_BREASTFEEDING, profile.isBreastfeeding)
            putBoolean(KEY_EATING_DISORDER_HISTORY, profile.hasEatingDisorderHistory)
            putBoolean(KEY_MEDICAL_CONDITION, profile.hasRelevantMedicalCondition)
            putLong(KEY_CREATED_AT, profile.createdAt)
        }
    }

    fun getProfile(): UserProfile? {
        if (!hasProfile()) return null
        return UserProfile(
            nickname = prefs.getString(KEY_NICKNAME, null)?.trim().takeUnless { it.isNullOrBlank() } ?: "You",
            age = prefs.getInt(KEY_AGE, 0),
            heightCm = prefs.getString(KEY_HEIGHT_CM, null)?.toDoubleOrNull() ?: return null,
            weightKg = prefs.getString(KEY_WEIGHT_KG, null)?.toDoubleOrNull() ?: return null,
            sexFormula = prefs.getEnum(KEY_SEX_FORMULA, SexFormula.MALE),
            goal = prefs.getEnum(KEY_GOAL, CalorieGoal.MAINTAIN),
            goalPace = prefs.getEnum(KEY_GOAL_PACE, GoalPace.STANDARD),
            targetWeightKg = prefs.getString(KEY_TARGET_WEIGHT_KG, null)?.toDoubleOrNull(),
            macroPreference = prefs.getEnum(KEY_MACRO_PREFERENCE, MacroPreference.BALANCED),
            manualStepRange = prefs.getEnum(KEY_STEP_RANGE, ManualStepRange.FROM_6000_TO_8999),
            avgStepGoal = prefs.getInt(KEY_AVG_STEP_GOAL, -1).takeIf { it > 0 },
            recentWeightStatus = prefs.getEnum(KEY_RECENT_WEIGHT_STATUS, RecentWeightStatus.STABLE),
            recentWeightAmount = prefs.getString(KEY_RECENT_WEIGHT_AMOUNT, null)?.toDoubleOrNull(),
            recentWeightUnit = prefs.getNullableEnum(KEY_RECENT_WEIGHT_UNIT),
            recentWeightTimeframeValue = prefs.getString(KEY_RECENT_WEIGHT_TIMEFRAME_VALUE, null)?.toDoubleOrNull(),
            recentWeightTimeframeUnit = prefs.getNullableEnum(KEY_RECENT_WEIGHT_TIMEFRAME_UNIT),
            isPregnant = prefs.getBoolean(KEY_PREGNANT, false),
            isBreastfeeding = prefs.getBoolean(KEY_BREASTFEEDING, false),
            hasEatingDisorderHistory = prefs.getBoolean(KEY_EATING_DISORDER_HISTORY, false),
            hasRelevantMedicalCondition = prefs.getBoolean(KEY_MEDICAL_CONDITION, false),
            createdAt = prefs.getLong(KEY_CREATED_AT, System.currentTimeMillis())
        )
    }

    fun clearProfile() {
        prefs.edit { clear() }
    }

    private inline fun <reified T : Enum<T>> android.content.SharedPreferences.getEnum(
        key: String,
        fallback: T
    ): T {
        return getString(key, null)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
    }

    private inline fun <reified T : Enum<T>> android.content.SharedPreferences.getNullableEnum(
        key: String
    ): T? {
        return getString(key, null)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
    }

    companion object {
        private const val PREFS_NAME = "user_profile"
        private const val KEY_PROFILE_CREATED = "profile_created"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_AGE = "age"
        private const val KEY_HEIGHT_CM = "height_cm"
        private const val KEY_WEIGHT_KG = "weight_kg"
        private const val KEY_SEX_FORMULA = "sex_formula"
        private const val KEY_GOAL = "goal"
        private const val KEY_GOAL_PACE = "goal_pace"
        private const val KEY_TARGET_WEIGHT_KG = "target_weight_kg"
        private const val KEY_MACRO_PREFERENCE = "macro_preference"
        private const val KEY_STEP_RANGE = "step_range"
        private const val KEY_AVG_STEP_GOAL = "avg_step_goal"
        private const val KEY_RECENT_WEIGHT_STATUS = "recent_weight_status"
        private const val KEY_RECENT_WEIGHT_AMOUNT = "recent_weight_amount"
        private const val KEY_RECENT_WEIGHT_UNIT = "recent_weight_unit"
        private const val KEY_RECENT_WEIGHT_TIMEFRAME_VALUE = "recent_weight_timeframe_value"
        private const val KEY_RECENT_WEIGHT_TIMEFRAME_UNIT = "recent_weight_timeframe_unit"
        private const val KEY_PREGNANT = "pregnant"
        private const val KEY_BREASTFEEDING = "breastfeeding"
        private const val KEY_EATING_DISORDER_HISTORY = "eating_disorder_history"
        private const val KEY_MEDICAL_CONDITION = "medical_condition"
        private const val KEY_CREATED_AT = "created_at"
    }
}
