package com.example.caloriescounter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.caloriescounter.data.DailyHealthSummary
import com.example.caloriescounter.data.DailyStepTotal
import com.example.caloriescounter.data.HealthConnectManager
import com.example.caloriescounter.data.MealRecord
import com.example.caloriescounter.data.MealRepository
import com.example.caloriescounter.data.UserProfile
import com.example.caloriescounter.data.UserProfileRepository
import com.example.caloriescounter.data.WeightRepository
import com.example.caloriescounter.data.WeightSample
import com.example.caloriescounter.nutrition.CalorieGoal
import com.example.caloriescounter.nutrition.CalorieEstimationModel
import com.example.caloriescounter.nutrition.CalorieModelInput
import com.example.caloriescounter.nutrition.CalorieRange
import com.example.caloriescounter.nutrition.CalorieTargetSource
import com.example.caloriescounter.nutrition.GoalPace
import com.example.caloriescounter.nutrition.MacroPreference
import com.example.caloriescounter.nutrition.ManualStepRange
import com.example.caloriescounter.nutrition.NutritionTargetInput
import com.example.caloriescounter.nutrition.NutritionTargetModel
import com.example.caloriescounter.nutrition.RecentWeightChangeInput
import com.example.caloriescounter.nutrition.SafetyInputs
import com.example.caloriescounter.nutrition.SexFormula
import com.example.caloriescounter.nutrition.StepConfidence
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class WeightWindow(val label: String, val days: Long) {
    WEEK("7D", 7),
    MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    YEAR("1Y", 365)
}

private data class DailyMealTotals(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double
)

private data class DailyCalorieTotal(
    val date: LocalDate,
    val calories: Double
)

private data class MacroDistribution(
    val protein: Double,
    val carbs: Double,
    val fat: Double
) {
    val total: Double get() = protein + carbs + fat
}

data class HomeDemoData(
    val meals: List<MealRecord>,
    val healthSummary: DailyHealthSummary,
    val weeklySteps: List<DailyStepTotal>,
    val weightSamples: List<WeightSample>,
    val calorieRange: CalorieRange? = null
)

@Composable
fun HomeScreen(
    mealRepository: MealRepository,
    healthConnectManager: HealthConnectManager,
    weightRepository: WeightRepository,
    userProfileRepository: UserProfileRepository,
    onPermissionNeeded: () -> Unit,
    demoData: HomeDemoData? = null,
    bottomContentPadding: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val storedMeals by mealRepository.allMeals.collectAsState(initial = emptyList())
    val meals = demoData?.meals ?: storedMeals
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    val latestMealDate = remember(meals) { meals.latestMealDate() }
    val intakeDate = remember(today, latestMealDate, meals) {
        if (meals.hasMealsOn(today) || latestMealDate == null) today else latestMealDate
    }
    val intakeDateLabel = remember(today, intakeDate) { dashboardDateLabel(today, intakeDate) }
    val displayMeals = remember(meals, intakeDate) { meals.onDate(intakeDate) }
    val totals = remember(displayMeals) { displayMeals.toDailyTotals() }
    val weeklyCalorieTotals = remember(meals) { meals.toWeeklyCalorieTotals() }
    val weeklyMacroTotals = remember(meals) { meals.toWeeklyMacroDistribution() }
    var healthSummary by remember { mutableStateOf(demoData?.healthSummary ?: DailyHealthSummary()) }
    var weeklySteps by remember { mutableStateOf(demoData?.weeklySteps ?: emptyList()) }
    var healthRefreshKey by remember { mutableIntStateOf(0) }
    var weightRefreshKey by remember { mutableIntStateOf(0) }
    var userProfile by remember { mutableStateOf(userProfileRepository.getProfile()) }
    var showProfileDetails by remember { mutableStateOf(false) }
    val weightSamples = remember(weightRefreshKey, demoData) { demoData?.weightSamples ?: weightRepository.getSamples() }

    LaunchedEffect(healthRefreshKey, demoData) {
        if (demoData != null) {
            healthSummary = demoData.healthSummary
            weeklySteps = demoData.weeklySteps
        } else {
            healthSummary = healthConnectManager.readDailyHealthSummary(LocalDate.now())
            weeklySteps = healthConnectManager.readWeeklyStepTotals(LocalDate.now())
        }
    }

    val latestStepDay = remember(weeklySteps) { weeklySteps.lastOrNull { it.steps > 0L } }
    val avgSteps = healthSummary.steps.takeIf { it > 0 }?.toInt()
        ?: latestStepDay?.steps?.toInt()
        ?: userProfile?.manualStepRange?.midpoint
        ?: DEFAULT_STEP_ESTIMATE
    val displayedStepSummary = remember(healthSummary, latestStepDay) {
        if (healthSummary.steps > 0L || latestStepDay == null) {
            healthSummary
        } else {
            val estimatedDistanceKm = latestStepDay.steps * KM_PER_STEP
            DailyHealthSummary(
                steps = latestStepDay.steps,
                distanceKm = estimatedDistanceKm,
                activeMinutes = (latestStepDay.steps / 100L).coerceAtLeast(1L),
                walkingCaloriesKcal = estimatedDistanceKm * 80.0 * 0.5,
                activeMinutesEstimated = true,
                walkingCaloriesEstimated = true,
                hasReadPermission = healthSummary.hasReadPermission,
                errorMessage = healthSummary.errorMessage
            )
        }
    }
    val stepDateLabel = remember(today, healthSummary.steps, latestStepDay) {
        if (healthSummary.steps > 0L || latestStepDay == null) {
            "Today"
        } else {
            dashboardDateLabel(today, latestStepDay.date)
        }
    }
    val stepGoal = userProfile?.avgStepGoal ?: STEP_TARGET
    val calorieModelResult = remember(avgSteps, healthSummary.hasReadPermission, userProfile) {
        userProfile?.let { profile ->
            CalorieEstimationModel.estimate(
                CalorieModelInput(
                    age = profile.age,
                    heightCm = profile.heightCm,
                    weightKg = profile.weightKg,
                    sexFormula = profile.sexFormula,
                    goal = profile.goal,
                    safetyInputs = SafetyInputs(
                        isPregnant = profile.isPregnant,
                        isBreastfeeding = profile.isBreastfeeding,
                        hasEatingDisorderHistory = profile.hasEatingDisorderHistory,
                        hasRelevantMedicalCondition = profile.hasRelevantMedicalCondition
                    ),
                    manualStepRange = profile.manualStepRange,
                    recentWeightChange = RecentWeightChangeInput(
                        status = profile.recentWeightStatus,
                        amount = profile.recentWeightAmount,
                        unit = profile.recentWeightUnit,
                        timeframeValue = profile.recentWeightTimeframeValue,
                        timeframeUnit = profile.recentWeightTimeframeUnit
                    ),
                    goalPace = profile.goalPace,
                    targetWeightKg = profile.targetWeightKg
                )
            )
        }
    }
    val dashboardCalorieRange = demoData?.calorieRange ?: calorieModelResult?.goalTarget?.let {
        CalorieRange(low = it.low, mid = it.target, high = it.high)
    } ?: DEFAULT_CALORIE_RANGE
    val nutritionTarget = remember(avgSteps, healthSummary.hasReadPermission, userProfile, calorieModelResult) {
        NutritionTargetModel.estimate(
            NutritionTargetInput(
                calorieTarget = dashboardCalorieRange,
                calorieTargetSource = CalorieTargetSource.INITIAL,
                calorieConfidence = calorieModelResult?.confidence ?: com.example.caloriescounter.nutrition.ModelConfidence.MEDIUM,
                weightKg = userProfile?.weightKg ?: DEFAULT_WEIGHT_KG,
                heightCm = userProfile?.heightCm ?: DEFAULT_HEIGHT_CM,
                age = userProfile?.age ?: DEFAULT_AGE,
                sexFormula = userProfile?.sexFormula ?: SexFormula.MALE,
                goal = calorieModelResult?.goalTarget?.goal ?: CalorieGoal.MAINTAIN,
                avgSteps = avgSteps,
                stepConfidence = calorieModelResult?.stepConfidence
                    ?: if (healthSummary.hasReadPermission) StepConfidence.HIGH else StepConfidence.LOW,
                safetyFlags = calorieModelResult?.flags ?: emptySet(),
                macroPreference = userProfile?.macroPreference
            )
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HomeHeader(
                profile = userProfile,
                onOpenProfile = { showProfileDetails = true },
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )
        }
        item {
            CalorieCard(
                calories = totals.calories,
                targetRange = nutritionTarget.calorieTarget,
                weeklyCalories = weeklyCalorieTotals,
                dateLabel = intakeDateLabel
            )
        }
        item {
            NutritionCard(
                totals = totals,
                proteinTarget = nutritionTarget.macros?.protein?.targetG ?: 0,
                carbTarget = nutritionTarget.macros?.carbohydrate?.targetG ?: 0,
                fatTarget = nutritionTarget.macros?.fat?.targetG ?: 0,
                weeklyMacroTotals = weeklyMacroTotals,
                dateLabel = intakeDateLabel
            )
        }
        item {
            WeightCard(
                samples = weightSamples,
                onAddWeight = { weight ->
                    if (demoData == null) {
                        weightRepository.addSample(weight)
                        weightRefreshKey++
                    }
                },
                onDeleteWeight = { timestamp ->
                    if (demoData == null) {
                        weightRepository.deleteSample(timestamp)
                        weightRefreshKey++
                    }
                },
                onUpdateWeight = { originalTimestamp, weight, newTimestamp ->
                    if (demoData == null) {
                        weightRepository.updateSample(originalTimestamp, weight, newTimestamp)
                        weightRefreshKey++
                    }
                }
            )
        }
        item {
            StepsCard(
                summary = displayedStepSummary,
                weeklySteps = weeklySteps,
                stepGoal = stepGoal,
                dateLabel = stepDateLabel,
                onStepGoalChanged = { newGoal ->
                    if (demoData == null) {
                        val updatedProfile = userProfile?.copy(avgStepGoal = newGoal)
                        if (updatedProfile != null) {
                            userProfileRepository.saveProfile(updatedProfile)
                            userProfile = updatedProfile
                        }
                    }
                },
                onPermissionNeeded = onPermissionNeeded,
                onRefresh = { healthRefreshKey++ }
            )
        }
        item {
            ActivityCard(summary = displayedStepSummary)
        }
    }

    val profile = userProfile
    if (showProfileDetails && profile != null) {
        ProfileDetailsDialog(
            profile = profile,
            onDismiss = { showProfileDetails = false },
            onSave = { updatedProfile ->
                userProfileRepository.saveProfile(updatedProfile)
                userProfile = updatedProfile
                showProfileDetails = false
            }
        )
    }
}

@Composable
private fun HomeHeader(
    profile: UserProfile?,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var greetingIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            greetingIndex = (greetingIndex + 1) % HOME_GREETINGS.size
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = HOME_GREETINGS[greetingIndex],
                transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(260)) },
                label = "homeGreeting",
                modifier = Modifier.weight(1f)
            ) { greeting ->
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = profile?.nickname?.takeIf { it.isNotBlank() } ?: "Profile",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(enabled = profile != null) { onOpenProfile() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        Text(
            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ProfileDetailsDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var nicknameInput by remember(profile) { mutableStateOf(profile.nickname) }
    var ageInput by remember(profile) { mutableStateOf(profile.age.toString()) }
    var stepGoalInput by remember(profile) { mutableStateOf((profile.avgStepGoal ?: STEP_TARGET).toString()) }
    var goalPace by remember(profile) { mutableStateOf(profile.goalPace) }
    var hasTargetWeight by remember(profile) { mutableStateOf(profile.targetWeightKg != null) }
    var targetWeightInput by remember(profile) { mutableStateOf(profile.targetWeightKg?.let(::formatWeight) ?: "") }
    var macroPreference by remember(profile) { mutableStateOf(profile.macroPreference) }
    var isPregnant by remember(profile) { mutableStateOf(profile.isPregnant) }
    var isBreastfeeding by remember(profile) { mutableStateOf(profile.isBreastfeeding) }
    var hasEatingDisorderHistory by remember(profile) { mutableStateOf(profile.hasEatingDisorderHistory) }
    var hasRelevantMedicalCondition by remember(profile) { mutableStateOf(profile.hasRelevantMedicalCondition) }
    var showConditions by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = profileConditionSummary(
                                isPregnant = isPregnant,
                                isBreastfeeding = isBreastfeeding,
                                hasEatingDisorderHistory = hasEatingDisorderHistory,
                                hasRelevantMedicalCondition = hasRelevantMedicalCondition
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = nicknameInput.trim().firstOrNull()?.uppercase() ?: "?",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = {
                        nicknameInput = it
                        errorText = null
                    },
                    label = { Text("Nickname") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                ageInput = input
                                errorText = null
                            }
                        },
                        label = { Text("Age") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stepGoalInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                stepGoalInput = input
                                errorText = null
                            }
                        },
                        label = { Text("Step goal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (profile.goal != CalorieGoal.MAINTAIN) {
                    SectionText("Goal pace")
                    ProfileChoiceRow(
                        options = listOf(
                            GoalPace.SLOW to "Slow",
                            GoalPace.STANDARD to "Standard",
                            GoalPace.FASTER to "Faster"
                        ),
                        selected = goalPace,
                        onSelect = {
                            goalPace = it
                            errorText = null
                        }
                    )
                }

                SectionText("Target weight")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !hasTargetWeight,
                        onClick = {
                            hasTargetWeight = false
                            targetWeightInput = ""
                            errorText = null
                        },
                        label = { Text("No target", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = hasTargetWeight,
                        onClick = {
                            hasTargetWeight = true
                            errorText = null
                        },
                        label = { Text("Use target", fontSize = 12.sp) }
                    )
                }
                if (hasTargetWeight) {
                    OutlinedTextField(
                        value = targetWeightInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) {
                                targetWeightInput = input
                                errorText = null
                            }
                        },
                        label = { Text("Target kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SectionText("Macro style")
                ProfileChoiceRow(
                    options = listOf(
                        MacroPreference.BALANCED to "Balanced",
                        MacroPreference.HIGHER_CARB to "Higher carb",
                        MacroPreference.LOWER_CARB to "Lower carb",
                        MacroPreference.HIGHER_PROTEIN to "Higher protein",
                        MacroPreference.HIGHER_FAT to "Higher fat"
                    ),
                    selected = macroPreference,
                    onSelect = {
                        macroPreference = it
                        errorText = null
                    }
                )

                TextButton(onClick = { showConditions = !showConditions }) {
                    Text(if (showConditions) "Hide conditions" else "Review conditions")
                }

                AnimatedVisibility(
                    visible = showConditions,
                    enter = expandVertically(
                        animationSpec = tween(180),
                        expandFrom = Alignment.Top
                    ) + fadeIn(tween(120)),
                    exit = shrinkVertically(
                        animationSpec = tween(140),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(tween(100))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProfileConditionRow(
                            label = "Pregnant",
                            checked = isPregnant,
                            onCheckedChange = { isPregnant = it }
                        )
                        ProfileConditionRow(
                            label = "Breastfeeding",
                            checked = isBreastfeeding,
                            onCheckedChange = { isBreastfeeding = it }
                        )
                        ProfileConditionRow(
                            label = "Eating-disorder history",
                            checked = hasEatingDisorderHistory,
                            onCheckedChange = { hasEatingDisorderHistory = it }
                        )
                        ProfileConditionRow(
                            label = "Relevant medical condition",
                            checked = hasRelevantMedicalCondition,
                            onCheckedChange = { hasRelevantMedicalCondition = it },
                            showInfoButton = true
                        )
                    }
                }

                errorText?.let {
                    Text(it, color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val age = ageInput.toIntOrNull()
                            val stepGoal = stepGoalInput.toIntOrNull()
                            val targetWeight = targetWeightInput.toDoubleOrNull().takeIf { hasTargetWeight }
                            errorText = when {
                                nicknameInput.trim().isBlank() -> "Enter a nickname"
                                age == null || age !in 13..120 -> "Enter a valid age"
                                stepGoal == null || stepGoal !in 500..50_000 -> "Enter a step goal"
                                hasTargetWeight && (targetWeight == null || targetWeight !in 25.0..500.0) ->
                                    "Enter a plausible target weight"
                                else -> null
                            }
                            if (errorText != null) return@TextButton
                            onSave(
                                profile.copy(
                                    nickname = nicknameInput.trim(),
                                    age = age!!,
                                    avgStepGoal = stepGoal,
                                    goalPace = if (profile.goal == CalorieGoal.MAINTAIN) GoalPace.STANDARD else goalPace,
                                    targetWeightKg = targetWeight,
                                    macroPreference = macroPreference,
                                    isPregnant = isPregnant,
                                    isBreastfeeding = isBreastfeeding,
                                    hasEatingDisorderHistory = hasEatingDisorderHistory,
                                    hasRelevantMedicalCondition = hasRelevantMedicalCondition
                                )
                            )
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun <T> ProfileChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileConditionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showInfoButton: Boolean = false
) {
    var showInfo by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (showInfoButton) {
                    IconButton(
                        onClick = { showInfo = !showInfo },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Medical condition info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
        AnimatedVisibility(
            visible = showInfo,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Any condition or medication that affects nutrition or calorie needs, like diabetes or heart conditions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
        }
    }
}

private fun profileConditionSummary(
    isPregnant: Boolean,
    isBreastfeeding: Boolean,
    hasEatingDisorderHistory: Boolean,
    hasRelevantMedicalCondition: Boolean
): String {
    val selected = listOfNotNull(
        "pregnancy".takeIf { isPregnant },
        "breastfeeding".takeIf { isBreastfeeding },
        "eating-disorder history".takeIf { hasEatingDisorderHistory },
        "medical condition".takeIf { hasRelevantMedicalCondition }
    )
    return if (selected.isEmpty()) "No stated conditions" else selected.joinToString()
}

@Composable
private fun CalorieCard(
    calories: Double,
    targetRange: CalorieRange,
    weeklyCalories: List<DailyCalorieTotal>,
    dateLabel: String
) {
    DashboardCard(title = "Daily intake", icon = Icons.Default.Restaurant) {
        var showWeeklyGraph by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { showWeeklyGraph = !showWeeklyGraph },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = dateLabel,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                AnimatedIntText(
                    target = calories.roundToInt(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    suffix = " kcal",
                    delayMillis = 0
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target range",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${targetRange.low}-${targetRange.high} kcal",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 14.sp
                    )
                }
            }
            CalorieRangeBar(
                calories = calories,
                targetRange = targetRange
            )
            AnimatedVisibility(
                visible = showWeeklyGraph,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
            ) {
                WeeklyCaloriesChart(
                    weeklyCalories = weeklyCalories,
                    targetRange = targetRange
                )
            }
        }
    }
}

@Composable
private fun WeeklyCaloriesChart(
    weeklyCalories: List<DailyCalorieTotal>,
    targetRange: CalorieRange
) {
    val maxCalories = max(
        weeklyCalories.maxOfOrNull { it.calories }?.roundToInt() ?: 0,
        targetRange.high
    ).coerceAtLeast(1)
    val roundedMax = ceilToStep(maxCalories.toDouble(), 500.0).toInt().coerceAtLeast(500)
    val formatter = DateTimeFormatter.ofPattern("E", Locale.US)
    WeeklyBarChart(
        values = weeklyCalories.map { it.calories.roundToInt() },
        labels = weeklyCalories.map { it.date.format(formatter).take(1) },
        targetValue = targetRange.mid,
        maxValue = roundedMax,
        targetRange = targetRange.low..targetRange.high,
        metColor = Color(0xFF16A34A),
        lowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
        valueFormatter = { "%,d".format(it) },
        isValueMet = { it in targetRange.low..targetRange.high }
    )
}

@Composable
private fun CalorieRangeBar(
    calories: Double,
    targetRange: CalorieRange
) {
    val animatedCalories = remember { Animatable(0f) }
    val maxScale = max(targetRange.high * 1.18, max(calories * 1.08, targetRange.high + 200.0)).toFloat()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    val rangeColor = Color(0xFF22C55E).copy(alpha = 0.22f)
    val lowColor = Color(0xFF3B82F6)
    val targetColor = Color(0xFF22C55E)
    val overColor = Color(0xFFEF4444)
    val fillColor = when {
        animatedCalories.value < targetRange.low -> lowColor
        animatedCalories.value <= targetRange.high -> targetColor
        else -> overColor
    }

    LaunchedEffect(calories, targetRange) {
        animatedCalories.snapTo(0f)
        animatedCalories.animateTo(
            targetValue = calories.toFloat(),
            animationSpec = tween(durationMillis = 900)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            val radius = size.height / 2f
            val rangeStart = (targetRange.low / maxScale).coerceIn(0f, 1f) * size.width
            val rangeEnd = (targetRange.high / maxScale).coerceIn(0f, 1f) * size.width
            val fillEnd = (animatedCalories.value / maxScale).coerceIn(0f, 1f) * size.width

            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = rangeColor,
                topLeft = Offset(rangeStart, 0f),
                size = Size((rangeEnd - rangeStart).coerceAtLeast(0f), size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = fillColor,
                size = Size(fillEnd, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
        }
    }
}

@Composable
private fun NutritionCard(
    totals: DailyMealTotals,
    proteinTarget: Int,
    carbTarget: Int,
    fatTarget: Int,
    weeklyMacroTotals: MacroDistribution,
    dateLabel: String
) {
    DashboardCard(title = "Nutrition", icon = Icons.Default.AutoGraph) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = dateLabel,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MacroProgressRow(
                        label = "Protein",
                        current = totals.protein,
                        target = proteinTarget.toDouble(),
                        color = NUTRITION_PROTEIN_COLOR
                    )
                    MacroProgressRow(
                        label = "Carbs",
                        current = totals.carbs,
                        target = carbTarget.toDouble(),
                        color = NUTRITION_CARB_COLOR
                    )
                    MacroProgressRow(
                        label = "Fat",
                        current = totals.fat,
                        target = fatTarget.toDouble(),
                        color = NUTRITION_FAT_COLOR
                    )
                    MacroProgressRow(
                        label = "Fiber",
                        current = totals.fiber,
                        target = DEFAULT_FIBER_TARGET_G,
                        color = Color(0xFFA78BFA)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    MacroPieChart(
                        title = "Actual",
                        distribution = weeklyMacroTotals
                    )
                    MacroPieChart(
                        title = "Recommended",
                        distribution = MacroDistribution(
                            protein = proteinTarget.toDouble(),
                            carbs = carbTarget.toDouble(),
                            fat = fatTarget.toDouble()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroPieChart(
    title: String,
    distribution: MacroDistribution
) {
    val total = distribution.total
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Canvas(modifier = Modifier.size(82.dp)) {
            if (total <= 0.0) {
                drawCircle(emptyColor)
                return@Canvas
            }
            var startAngle = -90f
            listOf(
                distribution.protein to NUTRITION_PROTEIN_COLOR,
                distribution.carbs to NUTRITION_CARB_COLOR,
                distribution.fat to NUTRITION_FAT_COLOR
            ).forEach { (value, color) ->
                val sweep = (value / total * 360.0).toFloat()
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true
                )
                startAngle += sweep
            }
            drawCircle(Color.White.copy(alpha = 0.86f), radius = size.minDimension * 0.27f)
        }
        if (total > 0.0) {
            Text(
                text = "${(distribution.protein / total * 100).roundToInt()}% / ${(distribution.carbs / total * 100).roundToInt()}% / ${(distribution.fat / total * 100).roundToInt()}%",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text("No data", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun WeightCard(
    samples: List<WeightSample>,
    onAddWeight: (Double) -> Unit,
    onDeleteWeight: (Long) -> Unit,
    onUpdateWeight: (Long, Double, Long) -> Unit
) {
    var selectedWindow by remember { mutableStateOf(WeightWindow.WEEK) }
    var weightInput by remember { mutableStateOf("") }
    var showAddWeight by remember { mutableStateOf(false) }
    var showDataList by remember { mutableStateOf(false) }
    var editingSample by remember { mutableStateOf<WeightSample?>(null) }
    val cutoff = remember(selectedWindow, samples) {
        System.currentTimeMillis() - selectedWindow.days * DAY_MS
    }
    val visibleSamples = remember(samples, selectedWindow) {
        samples.filter { it.timestamp >= cutoff }
    }

    DashboardCard(
        title = "Weight",
        icon = Icons.Default.MonitorWeight,
        action = {
            IconButton(
                onClick = { showAddWeight = !showAddWeight },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add weight",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WeightTrend(
                samples = visibleSamples,
                targetWeightKg = DEFAULT_TARGET_WEIGHT_KG,
                onOpenDetails = {
                    if (visibleSamples.isNotEmpty()) showDataList = !showDataList
                }
            )
            WeightWindowSelector(
                selectedWindow = selectedWindow,
                onWindowSelected = { selectedWindow = it }
            )
            AnimatedVisibility(
                visible = showAddWeight,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Try measuring in similar settings, like after waking up",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("kg") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val weight = weightInput.toDoubleOrNull()
                                if (weight != null) {
                                    onAddWeight(weight)
                                    weightInput = ""
                                    showAddWeight = false
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save weight", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(
                            onClick = {
                                weightInput = ""
                                showAddWeight = false
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel weight")
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = showDataList && visibleSamples.isNotEmpty(),
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
            ) {
                WeightDataCard(
                    samples = visibleSamples,
                    onEdit = { editingSample = it },
                    onDelete = { timestamp ->
                        onDeleteWeight(timestamp)
                        if (visibleSamples.size <= 1) showDataList = false
                    }
                )
            }
        }
    }

    editingSample?.let { sample ->
        WeightEditDialog(
            sample = sample,
            onDismiss = { editingSample = null },
            onSave = { weight, timestamp ->
                onUpdateWeight(sample.timestamp, weight, timestamp)
                editingSample = null
            }
        )
    }
}

@Composable
private fun WeightTrend(
    samples: List<WeightSample>,
    targetWeightKg: Double,
    onOpenDetails: () -> Unit
) {
    if (samples.isEmpty()) {
        EmptyGraph(
            title = "No weight data yet",
            body = "Add a measurement to start the trend"
        )
        return
    }

    val rawMin = min(samples.minOf { it.weightKg }, targetWeightKg)
    val rawMax = max(samples.maxOf { it.weightKg }, targetWeightKg)
    val step = niceWeightStep((rawMax - rawMin).coerceAtLeast(1.0) / 4.0)
    val yMin = floorToStep(rawMin - step, step)
    val yMax = ceilToStep(rawMax + step, step)
    val yLabels = generateYLabels(yMin, yMax, step)
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable { onOpenDetails() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(138.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.reversed().forEach { label ->
                    Text(
                        text = formatAxisWeight(label),
                        color = textColor,
                        fontSize = 9.sp,
                        lineHeight = 9.sp
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val chartPaddingX = 10.dp.toPx()
                val chartWidth = size.width - chartPaddingX * 2
                val ySpan = (yMax - yMin).coerceAtLeast(1.0)
                fun pointFor(index: Int, sample: WeightSample): Offset {
                    val x = if (samples.size == 1) {
                        size.width / 2f
                    } else {
                        chartPaddingX + chartWidth * (index.toFloat() / (samples.lastIndex).coerceAtLeast(1))
                    }
                    val y = size.height - (((sample.weightKg - yMin) / ySpan).toFloat().coerceIn(0f, 1f) * size.height)
                    return Offset(x, y)
                }

                yLabels.forEach { label ->
                    val y = size.height - (((label - yMin) / ySpan).toFloat().coerceIn(0f, 1f) * size.height)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val points = samples.mapIndexed(::pointFor)
                points.zipWithNext().forEach { (start, end) ->
                    drawLine(
                        color = lineColor,
                        start = start,
                        end = end,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                points.forEach { point ->
                    drawCircle(
                        color = lineColor,
                        radius = 5.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 2.dp.toPx(),
                        center = point
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            samples.forEach { sample ->
                Text(
                    text = sample.shortDayLabel(),
                    color = textColor,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeightWindowSelector(
    selectedWindow: WeightWindow,
    onWindowSelected: (WeightWindow) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WeightWindow.entries.forEach { window ->
            val selected = selectedWindow == window
            Box(
                modifier = Modifier
                    .weight(if (selected) 1.45f else 0.82f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onWindowSelected(window) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = window.label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
        }
    }
}

@Composable
private fun WeightDataCard(
    samples: List<WeightSample>,
    onEdit: (WeightSample) -> Unit,
    onDelete: (Long) -> Unit
) {
    var selectedTimestamp by remember(samples) { mutableStateOf<Long?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Weight points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn(
                modifier = Modifier.heightIn(max = 190.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(samples.sortedByDescending { it.timestamp }, key = { it.timestamp }) { sample ->
                    val selected = selectedTimestamp == sample.timestamp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTimestamp = sample.timestamp }
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${formatWeight(sample.weightKg)} kg",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = sample.fullDayLabel(),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                    fontSize = 12.sp
                                )
                            }
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(tween(180)),
                                exit = fadeOut(tween(120))
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onEdit(sample) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit weight")
                                    }
                                    IconButton(onClick = { onDelete(sample.timestamp) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete weight")
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightEditDialog(
    sample: WeightSample,
    onDismiss: () -> Unit,
    onSave: (Double, Long) -> Unit
) {
    var weightInput by remember(sample) { mutableStateOf(formatWeight(sample.weightKg)) }
    var dateInput by remember(sample) { mutableStateOf(sample.editDateLabel()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit weight", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = {
                        weightInput = it
                        errorText = null
                    },
                    label = { Text("Weight kg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = {
                        dateInput = it
                        errorText = null
                    },
                    label = { Text("Date yyyy-mm-dd") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorText?.let {
                    Text(it, color = Color(0xFFEF4444), fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val weight = weightInput.toDoubleOrNull()
                            val date = runCatching { LocalDate.parse(dateInput) }.getOrNull()
                            if (weight == null || date == null) {
                                errorText = "Enter a valid weight and date"
                                return@TextButton
                            }
                            onSave(weight, date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsCard(
    summary: DailyHealthSummary,
    weeklySteps: List<DailyStepTotal>,
    stepGoal: Int,
    dateLabel: String,
    onStepGoalChanged: (Int) -> Unit,
    onPermissionNeeded: () -> Unit,
    onRefresh: () -> Unit
) {
    val distanceKm = summary.distanceKm ?: summary.steps * KM_PER_STEP
    var showWeeklyGraph by remember { mutableStateOf(false) }
    var showGoalEditor by remember { mutableStateOf(false) }

    DashboardCard(title = "Steps", icon = Icons.AutoMirrored.Filled.DirectionsWalk) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = summary.hasReadPermission) { showWeeklyGraph = !showWeeklyGraph },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = dateLabel,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                AnimatedIntText(
                    target = summary.steps.toInt(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    formatter = { "%,d".format(it) }
                )
                Text(
                    text = "${formatOneDecimal(distanceKm)} km",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            SimpleProgressBar(
                progress = summary.steps.toFloat() / stepGoal.coerceAtLeast(1),
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target %,d".format(stepGoal),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { showGoalEditor = !showGoalEditor }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                if (summary.hasReadPermission) {
                    Text(
                        text = "Refresh",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onRefresh() }
                    )
                } else {
                    Button(onClick = onPermissionNeeded) {
                        Text("Connect")
                    }
                }
            }
            summary.errorMessage?.let {
                Text(
                    text = if (summary.hasReadPermission) "Could not refresh health data" else "Health data is not connected yet",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    fontSize = 12.sp
                )
            }
            AnimatedVisibility(
                visible = showGoalEditor,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
            ) {
                StepGoalPicker(
                    currentGoal = stepGoal,
                    onCancel = { showGoalEditor = false },
                    onSave = {
                        onStepGoalChanged(it)
                        showGoalEditor = false
                    }
                )
            }
            AnimatedVisibility(
                visible = showWeeklyGraph && weeklySteps.isNotEmpty(),
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
            ) {
                WeeklyStepsChart(
                    weeklySteps = weeklySteps,
                    stepGoal = stepGoal
                )
            }
        }
    }
}

@Composable
private fun WeeklyStepsChart(
    weeklySteps: List<DailyStepTotal>,
    stepGoal: Int
) {
    val maxSteps = max(
        weeklySteps.maxOfOrNull { it.steps }?.toInt() ?: 0,
        stepGoal
    ).coerceAtLeast(1)
    val roundedMax = ceilToStep(maxSteps.toDouble(), 2_000.0).toInt().coerceAtLeast(2_000)
    val goalRatio = (stepGoal.toFloat() / roundedMax).coerceIn(0f, 1f)
    val formatter = DateTimeFormatter.ofPattern("E", Locale.US)

    WeeklyBarChart(
        values = weeklySteps.map { it.steps.toInt() },
        labels = weeklySteps.map { it.date.format(formatter).take(1) },
        targetValue = stepGoal,
        maxValue = roundedMax,
        targetRatio = goalRatio,
        metColor = Color(0xFF16A34A),
        lowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
        valueFormatter = { "%,d".format(it) },
        isValueMet = { it >= stepGoal }
    )
}

@Composable
private fun StepGoalPicker(
    currentGoal: Int,
    onCancel: () -> Unit,
    onSave: (Int) -> Unit
) {
    var thousands by remember(currentGoal) { mutableIntStateOf((currentGoal / 1000).coerceIn(1, 30)) }
    var hundreds by remember(currentGoal) { mutableIntStateOf(((currentGoal % 1000) / 100).coerceIn(0, 9)) }
    var fifty by remember(currentGoal) { mutableIntStateOf(if (currentGoal % 100 >= 50) 1 else 0) }
    val goal = (thousands * 1000 + hundreds * 100 + fifty * 50).coerceAtLeast(1000)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Step goal %,d".format(goal), fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StepGoalWheel(
                    value = thousands,
                    label = "000s",
                    onUp = { thousands = (thousands + 1).coerceAtMost(30) },
                    onDown = { thousands = (thousands - 1).coerceAtLeast(1) },
                    modifier = Modifier.weight(1f)
                )
                StepGoalWheel(
                    value = hundreds,
                    label = "100s",
                    onUp = { hundreds = (hundreds + 1).coerceAtMost(9) },
                    onDown = { hundreds = (hundreds - 1).coerceAtLeast(0) },
                    modifier = Modifier.weight(1f)
                )
                StepGoalWheel(
                    value = if (fifty == 1) 50 else 0,
                    label = "+50",
                    onUp = { fifty = 1 },
                    onDown = { fifty = 0 },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel step goal")
                }
                IconButton(
                    onClick = { onSave(goal) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save step goal", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun StepGoalWheel(
    value: Int,
    label: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onUp, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase $label")
        }
        Text(value.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), fontSize = 10.sp)
        IconButton(onClick = onDown, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease $label")
        }
    }
}

@Composable
private fun WeeklyBarChart(
    values: List<Int>,
    labels: List<String>,
    targetValue: Int,
    maxValue: Int,
    targetRatio: Float? = null,
    targetRange: IntRange? = null,
    metColor: Color,
    lowColor: Color,
    valueFormatter: (Int) -> String,
    isValueMet: (Int) -> Boolean
) {
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
    val goalColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
    val targetBandColor = Color(0xFF22C55E).copy(alpha = 0.18f)
    val density = LocalDensity.current
    val chartHeight = 144.dp
    fun labelOffsetFor(value: Int): Dp = with(density) {
        val ratio = (value.toFloat() / maxValue.coerceAtLeast(1)).coerceIn(0f, 1f)
        (chartHeight.toPx() * (1f - ratio) - 8.dp.toPx()).toDp()
            .coerceIn(0.dp, chartHeight - 16.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .height(chartHeight + 26.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val safeMax = maxValue.coerceAtLeast(1).toFloat()
                    val chartValues = values.take(labels.size)
                    val gap = 7.dp.toPx()
                    val barSlotWidth = if (chartValues.isNotEmpty()) {
                        (size.width - gap * (chartValues.size - 1)).coerceAtLeast(0f) / chartValues.size
                    } else {
                        0f
                    }
                    val barWidth = barSlotWidth * 0.72f
                    val corner = 8.dp.toPx()

                    chartValues.forEachIndexed { index, value ->
                        val ratio = (value.toFloat() / safeMax).coerceIn(0f, 1f)
                        val barHeight = (ratio * size.height).coerceAtLeast(6.dp.toPx())
                        val left = index * (barSlotWidth + gap) + (barSlotWidth - barWidth) / 2f
                        drawRoundRect(
                            color = if (isValueMet(value)) metColor else lowColor,
                            topLeft = Offset(left, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(corner, corner)
                        )
                    }

                    targetRange?.let { range ->
                        val highRatio = (range.last.toFloat() / safeMax).coerceIn(0f, 1f)
                        val lowRatio = (range.first.toFloat() / safeMax).coerceIn(0f, 1f)
                        val top = size.height - highRatio * size.height
                        val bottom = size.height - lowRatio * size.height
                        drawRoundRect(
                            color = targetBandColor,
                            topLeft = Offset(0f, top),
                            size = Size(size.width, (bottom - top).coerceAtLeast(2.dp.toPx())),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }

                    targetRatio?.let { ratio ->
                        val y = size.height - ratio * size.height
                        val dash = 8.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = goalColor,
                                start = Offset(x, y),
                                end = Offset((x + dash).coerceAtMost(size.width), y),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            x += dash * 1.8f
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .height(chartHeight)
                .width(48.dp)
        ) {
            Text(
                valueFormatter(maxValue),
                fontSize = 10.sp,
                color = textColor,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            if (targetRange == null) {
                Text(
                    valueFormatter(targetValue),
                    fontSize = 10.sp,
                    color = goalColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = labelOffsetFor(targetValue))
                )
            } else {
                Text(
                    valueFormatter(targetRange.last),
                    fontSize = 10.sp,
                    color = goalColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = labelOffsetFor(targetRange.last))
                )
                Text(
                    valueFormatter(targetRange.first),
                    fontSize = 10.sp,
                    color = goalColor.copy(alpha = 0.76f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = labelOffsetFor(targetRange.first))
                )
            }
            Text(
                "0",
                fontSize = 10.sp,
                color = textColor,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun ActivityCard(summary: DailyHealthSummary) {
    DashboardCard(title = "Activity", icon = Icons.Default.LocalFireDepartment) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                label = if (summary.activeMinutesEstimated) "Active est." else "Active",
                value = "${summary.activeMinutes}",
                suffix = "min",
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = if (summary.walkingCaloriesEstimated) "Burn est." else "Walk burn",
                value = summary.walkingCaloriesKcal?.roundToInt()?.toString() ?: "--",
                suffix = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    suffix: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            val numericValue = value.toIntOrNull()
            if (numericValue != null) {
                AnimatedIntText(
                    target = numericValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(4.dp))
            Text(suffix, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp)
        }
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp)
    }
}

@Composable
private fun DashboardCard(
    title: String,
    icon: ImageVector,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    primaryText: String,
    secondaryText: String
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val progressColor = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(118.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke
            )
            if (progress > 1f) {
                drawArc(
                    color = Color(0xFFF59E0B),
                    startAngle = -90f,
                    sweepAngle = 360f * (progress - 1f).coerceIn(0f, 0.35f),
                    useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(primaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(secondaryText, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    current: Double,
    target: Double,
    color: Color
) {
    val targetText = if (target > 0.0) "${target.roundToInt()}g" else "--"
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedIntText(
                    target = current.roundToInt(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    suffix = "g"
                )
                Text(
                    text = " / $targetText",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontSize = 12.sp
                )
            }
        }
        SimpleProgressBar(
            progress = if (target > 0.0) (current / target).toFloat() else 0f,
            color = color
        )
    }
}

@Composable
private fun SimpleProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        delay(500)
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 650)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress.value)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
    }
}

@Composable
private fun EmptyGraph(title: String, body: String) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val color = Color.White.copy(alpha = 0.25f)
            drawLine(color, Offset(0f, size.height * 0.72f), Offset(size.width, size.height * 0.3f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(primaryColor, radius = 5.dp.toPx(), center = Offset(size.width, size.height * 0.3f))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnimatedIntText(
    target: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    suffix: String = "",
    delayMillis: Long = 500,
    formatter: (Int) -> String = { it.toString() }
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(target) {
        animatedValue.snapTo(0f)
        delay(delayMillis)
        animatedValue.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = 700)
        )
    }

    Text(
        text = formatter(animatedValue.value.roundToInt()) + suffix,
        modifier = modifier,
        style = style,
        fontWeight = fontWeight,
        color = color
    )
}

private fun List<MealRecord>.toDailyTotals(): DailyMealTotals {
    return DailyMealTotals(
        calories = sumOf { it.calories },
        protein = sumOf { it.protein ?: 0.0 },
        carbs = sumOf { it.carbs ?: 0.0 },
        fat = sumOf { it.fat ?: 0.0 },
        fiber = sumOf { it.fiber ?: 0.0 }
    )
}

private fun List<MealRecord>.toWeeklyCalorieTotals(): List<DailyCalorieTotal> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    return (6L downTo 0L).map { daysAgo ->
        val date = today.minusDays(daysAgo)
        DailyCalorieTotal(
            date = date,
            calories = filter {
                Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == date
            }.sumOf { it.calories }
        )
    }
}

private fun List<MealRecord>.toWeeklyMacroDistribution(): MacroDistribution {
    val zone = ZoneId.systemDefault()
    val cutoff = LocalDate.now(zone).minusDays(6)
    val meals = filter {
        Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() >= cutoff
    }
    return MacroDistribution(
        protein = meals.sumOf { it.protein ?: 0.0 },
        carbs = meals.sumOf { it.carbs ?: 0.0 },
        fat = meals.sumOf { it.fat ?: 0.0 }
    )
}

private fun List<MealRecord>.hasMealsOn(date: LocalDate): Boolean {
    return any { it.localDate() == date }
}

private fun List<MealRecord>.onDate(date: LocalDate): List<MealRecord> {
    return filter { it.localDate() == date }
}

private fun List<MealRecord>.latestMealDate(): LocalDate? {
    return maxByOrNull { it.timestamp }?.localDate()
}

private fun MealRecord.localDate(): LocalDate {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
}

private fun dashboardDateLabel(today: LocalDate, date: LocalDate): String {
    return if (date == today) {
        "Today"
    } else {
        "Latest ${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}"
    }
}

private fun WeightSample.dayLabel(): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MMM d"))
}

private fun WeightSample.shortDayLabel(): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("d/M"))
}

private fun WeightSample.fullDayLabel(): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
}

private fun WeightSample.editDateLabel(): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
}

private fun formatWeight(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun formatOneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun floorToStep(value: Double, step: Double): Double = floor(value / step) * step

private fun ceilToStep(value: Double, step: Double): Double = ceil(value / step) * step

private fun generateYLabels(minValue: Double, maxValue: Double, step: Double): List<Double> {
    val labels = mutableListOf<Double>()
    var current = minValue
    while (current <= maxValue + 0.01) {
        labels += current
        current += step
    }
    return labels
}

private fun niceWeightStep(rawStep: Double): Double {
    return when {
        rawStep <= 0.5 -> 0.5
        rawStep <= 1.0 -> 1.0
        rawStep <= 2.0 -> 2.0
        rawStep <= 5.0 -> 5.0
        else -> ceil(rawStep / 5.0) * 5.0
    }
}

private fun formatAxisWeight(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

private const val DEFAULT_AGE = 29
private const val DEFAULT_HEIGHT_CM = 170.0
private const val DEFAULT_WEIGHT_KG = 80.0
private const val DEFAULT_TARGET_WEIGHT_KG = 75.0
private const val DEFAULT_STEP_ESTIMATE = 9_000
private const val DEFAULT_FIBER_TARGET_G = 30.0
private const val STEP_TARGET = 10_000
private const val KM_PER_STEP = 0.00072
private const val DAY_MS = 86_400_000L
private val NUTRITION_PROTEIN_COLOR = Color(0xFF22C55E)
private val NUTRITION_CARB_COLOR = Color(0xFF3B82F6)
private val NUTRITION_FAT_COLOR = Color(0xFFF59E0B)
private val DEFAULT_CALORIE_RANGE = CalorieRange(low = 1900, mid = 2100, high = 2300)
private val HOME_GREETINGS = listOf(
    "Welcome back",
    "Balance looks good",
    "Small bites big wins",
    "Today is yours",
    "Steady tastes better",
    "One meal at a time",
    "Nice to see you",
    "Fresh day fresh data",
    "You are in balance",
    "Keep it light",
    "Tiny choices add up",
    "Your day on a plate",
    "Calories with clarity",
    "A balanced beginning",
    "Good rhythm today",
    "Lets plate it smart",
    "A little wiser daily",
    "Fuel with intention",
    "Your numbers are waiting",
    "Healthy math time",
    "Meal it over",
    "Snack to the future",
    "You kcal this",
    "Serving balance",
    "Fork yeah",
    "Scale it gently",
    "Bite by bright bite",
    "Data tastes better",
    "Track happy",
    "Here for the balance"
)
