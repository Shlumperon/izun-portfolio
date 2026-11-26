package com.example.caloriescounter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.caloriescounter.data.UserProfile
import com.example.caloriescounter.nutrition.CalorieGoal
import com.example.caloriescounter.nutrition.GoalPace
import com.example.caloriescounter.nutrition.MacroPreference
import com.example.caloriescounter.nutrition.ManualStepRange
import com.example.caloriescounter.nutrition.RecentWeightStatus
import com.example.caloriescounter.nutrition.SexFormula
import com.example.caloriescounter.nutrition.TimeframeUnit
import com.example.caloriescounter.nutrition.WeightUnit

@Composable
fun ProfileCreationScreen(
    onProfileCreated: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableIntStateOf(0) }
    var nicknameInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var sexFormula by remember { mutableStateOf(SexFormula.MALE) }
    var goal by remember { mutableStateOf(CalorieGoal.LOSE_WEIGHT) }
    var goalPace by remember { mutableStateOf(GoalPace.STANDARD) }
    var hasTargetWeight by remember { mutableStateOf(false) }
    var targetWeightInput by remember { mutableStateOf("") }
    var macroPreference by remember { mutableStateOf(MacroPreference.BALANCED) }
    var stepRange by remember { mutableStateOf(ManualStepRange.FROM_6000_TO_8999) }
    var recentWeightStatus by remember { mutableStateOf(RecentWeightStatus.STABLE) }
    var recentWeightAmountInput by remember { mutableStateOf("") }
    var recentWeightDaysInput by remember { mutableStateOf("") }
    var isPregnant by remember { mutableStateOf(false) }
    var isBreastfeeding by remember { mutableStateOf(false) }
    var hasEatingDisorderHistory by remember { mutableStateOf(false) }
    var hasRelevantMedicalCondition by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun clearError() {
        errorText = null
    }

    fun validateBasics(): Boolean {
        val age = ageInput.toIntOrNull()
        val height = heightInput.toDoubleOrNull()
        val weight = weightInput.toDoubleOrNull()
        errorText = when {
            nicknameInput.trim().isBlank() -> "Enter a nickname."
            age == null || age !in 13..120 -> "Enter a valid age."
            height == null || height !in 100.0..250.0 -> "Enter height in cm."
            weight == null || weight !in 30.0..300.0 -> "Enter weight in kg."
            else -> null
        }
        return errorText == null
    }

    fun buildProfile(): UserProfile? {
        if (!validateBasics()) return null
        val targetWeight = targetWeightInput.toDoubleOrNull().takeIf { hasTargetWeight }
        if (hasTargetWeight && (targetWeight == null || targetWeight !in 25.0..500.0)) {
            errorText = "Enter a plausible target weight in kg."
            return null
        }
        val needsRecentDetails =
            recentWeightStatus == RecentWeightStatus.LOST_WEIGHT ||
                recentWeightStatus == RecentWeightStatus.GAINED_WEIGHT
        val recentAmount = recentWeightAmountInput.toDoubleOrNull()
        val recentDays = recentWeightDaysInput.toDoubleOrNull()
        if (needsRecentDetails && (recentAmount == null || recentAmount <= 0.0 || recentDays == null || recentDays <= 0.0)) {
            errorText = "Enter the weight change amount and number of days."
            return null
        }
        return UserProfile(
            nickname = nicknameInput.trim(),
            age = ageInput.toInt(),
            heightCm = heightInput.toDouble(),
            weightKg = weightInput.toDouble(),
            sexFormula = sexFormula,
            goal = goal,
            goalPace = if (goal == CalorieGoal.MAINTAIN) GoalPace.STANDARD else goalPace,
            targetWeightKg = targetWeight,
            macroPreference = macroPreference,
            manualStepRange = stepRange,
            avgStepGoal = stepRange.midpoint,
            recentWeightStatus = recentWeightStatus,
            recentWeightAmount = recentAmount.takeIf { needsRecentDetails },
            recentWeightUnit = WeightUnit.KG.takeIf { needsRecentDetails },
            recentWeightTimeframeValue = recentDays.takeIf { needsRecentDetails },
            recentWeightTimeframeUnit = TimeframeUnit.DAYS.takeIf { needsRecentDetails },
            isPregnant = isPregnant,
            isBreastfeeding = isBreastfeeding,
            hasEatingDisorderHistory = hasEatingDisorderHistory,
            hasRelevantMedicalCondition = hasRelevantMedicalCondition
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileHeader(page = page, pageCount = PROFILE_PAGE_COUNT)

        AnimatedContent(
            targetState = page,
            label = "profilePage"
        ) { activePage ->
            when (activePage) {
                0 -> BasicsPage(
                    nicknameInput = nicknameInput,
                    onNicknameChange = {
                        nicknameInput = it
                        clearError()
                    },
                    ageInput = ageInput,
                    onAgeChange = {
                        ageInput = it
                        clearError()
                    },
                    heightInput = heightInput,
                    onHeightChange = {
                        heightInput = it
                        clearError()
                    },
                    weightInput = weightInput,
                    onWeightChange = {
                        weightInput = it
                        clearError()
                    },
                    sexFormula = sexFormula,
                    onSexChange = {
                        sexFormula = it
                        clearError()
                    }
                )
                1 -> GoalStepsPage(
                    goal = goal,
                    onGoalChange = {
                        goal = it
                        if (it == CalorieGoal.MAINTAIN) goalPace = GoalPace.STANDARD
                        clearError()
                    },
                    stepRange = stepRange,
                    onStepRangeChange = {
                        stepRange = it
                        clearError()
                    }
                )
                2 -> PaceTargetPage(
                    goal = goal,
                    goalPace = goalPace,
                    onGoalPaceChange = {
                        goalPace = it
                        clearError()
                    },
                    hasTargetWeight = hasTargetWeight,
                    onHasTargetWeightChange = {
                        hasTargetWeight = it
                        if (!it) targetWeightInput = ""
                        clearError()
                    },
                    targetWeightInput = targetWeightInput,
                    onTargetWeightChange = {
                        targetWeightInput = it
                        clearError()
                    }
                )
                3 -> MacroPreferencePage(
                    macroPreference = macroPreference,
                    onMacroPreferenceChange = {
                        macroPreference = it
                        clearError()
                    }
                )
                else -> SafetyPage(
                    recentWeightStatus = recentWeightStatus,
                    onRecentWeightStatusChange = {
                        recentWeightStatus = it
                        clearError()
                    },
                    recentWeightAmountInput = recentWeightAmountInput,
                    onRecentWeightAmountChange = {
                        recentWeightAmountInput = it
                        clearError()
                    },
                    recentWeightDaysInput = recentWeightDaysInput,
                    onRecentWeightDaysChange = {
                        recentWeightDaysInput = it
                        clearError()
                    },
                    isPregnant = isPregnant,
                    onPregnantChange = {
                        isPregnant = it
                        clearError()
                    },
                    isBreastfeeding = isBreastfeeding,
                    onBreastfeedingChange = {
                        isBreastfeeding = it
                        clearError()
                    },
                    hasEatingDisorderHistory = hasEatingDisorderHistory,
                    onEatingDisorderHistoryChange = {
                        hasEatingDisorderHistory = it
                        clearError()
                    },
                    hasRelevantMedicalCondition = hasRelevantMedicalCondition,
                    onMedicalConditionChange = {
                        hasRelevantMedicalCondition = it
                        clearError()
                    }
                )
            }
        }

        errorText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    if (page > 0) {
                        page--
                        clearError()
                    }
                },
                enabled = page > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Back")
            }

            Button(
                onClick = {
                    if (page == 0 && !validateBasics()) return@Button
                    if (page < PROFILE_PAGE_COUNT - 1) {
                        page++
                        clearError()
                    } else {
                        buildProfile()?.let(onProfileCreated)
                    }
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (page < PROFILE_PAGE_COUNT - 1) "Continue" else "Create profile")
            }
        }
    }
}

@Composable
private fun ProfileHeader(page: Int, pageCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "IZUN Profile",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Let's setup your profile!",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${page + 1}/$pageCount",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LinearProgressIndicator(
            progress = { (page + 1) / pageCount.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun BasicsPage(
    nicknameInput: String,
    onNicknameChange: (String) -> Unit,
    ageInput: String,
    onAgeChange: (String) -> Unit,
    heightInput: String,
    onHeightChange: (String) -> Unit,
    weightInput: String,
    onWeightChange: (String) -> Unit,
    sexFormula: SexFormula,
    onSexChange: (SexFormula) -> Unit
) {
    ProfileCard(
        icon = Icons.Default.Person,
        title = "Profile basics",
        subtitle = "A few details help IZUN make the dashboard feel like yours"
    ) {
        OutlinedTextField(
            value = nicknameInput,
            onValueChange = onNicknameChange,
            label = { Text("Nickname") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileNumberField(
                value = ageInput,
                onValueChange = onAgeChange,
                label = "Age",
                modifier = Modifier.weight(1f)
            )
            ProfileNumberField(
                value = heightInput,
                onValueChange = onHeightChange,
                label = "Height cm",
                modifier = Modifier.weight(1f)
            )
        }
        ProfileNumberField(
            value = weightInput,
            onValueChange = onWeightChange,
            label = "Weight kg",
            modifier = Modifier.fillMaxWidth()
        )
        SectionLabel("Sex")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionPill(
                text = "Male",
                selected = sexFormula == SexFormula.MALE,
                onClick = { onSexChange(SexFormula.MALE) },
                modifier = Modifier.weight(1f)
            )
            OptionPill(
                text = "Female",
                selected = sexFormula == SexFormula.FEMALE,
                onClick = { onSexChange(SexFormula.FEMALE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GoalStepsPage(
    goal: CalorieGoal,
    onGoalChange: (CalorieGoal) -> Unit,
    stepRange: ManualStepRange,
    onStepRangeChange: (ManualStepRange) -> Unit
) {
    ProfileCard(
        icon = Icons.Default.Route,
        title = "Goal and steps",
        subtitle = "Steps are the only activity input used by IZUN"
    ) {
        SectionLabel("Goal")
        TwoColumnOptions(
            options = listOf(
                CalorieGoal.MAINTAIN to "Maintain",
                CalorieGoal.LOSE_WEIGHT to "Lose weight",
                CalorieGoal.GAIN_MUSCLE to "Gain muscle",
                CalorieGoal.RECOMPOSITION to "Recomp"
            ),
            selected = goal,
            onSelect = onGoalChange
        )

        SectionLabel("Typical daily steps")
        Text(
            text = "What is your average daily step count (estimation)",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stepRangeOptions.forEach { (range, label) ->
                OptionPill(
                    text = label,
                    selected = stepRange == range,
                    onClick = { onStepRangeChange(range) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PaceTargetPage(
    goal: CalorieGoal,
    goalPace: GoalPace,
    onGoalPaceChange: (GoalPace) -> Unit,
    hasTargetWeight: Boolean,
    onHasTargetWeightChange: (Boolean) -> Unit,
    targetWeightInput: String,
    onTargetWeightChange: (String) -> Unit
) {
    ProfileCard(
        icon = Icons.Default.Flag,
        title = "Pace and Target",
        subtitle = "Set your pace and target value"
    ) {
        if (goal != CalorieGoal.MAINTAIN) {
            SectionLabel("How fast do you want to move toward your goal?")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goalPaceOptions.forEach { (pace, label) ->
                    OptionPill(
                        text = label,
                        selected = goalPace == pace,
                        onClick = { onGoalPaceChange(pace) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Text(
                text = "Pace is ignored for maintenance goals.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 13.sp
            )
        }

        SectionLabel("Do you have a target weight?")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionPill(
                text = "No target",
                selected = !hasTargetWeight,
                onClick = { onHasTargetWeightChange(false) },
                modifier = Modifier.weight(1f)
            )
            OptionPill(
                text = "Enter target",
                selected = hasTargetWeight,
                onClick = { onHasTargetWeightChange(true) },
                modifier = Modifier.weight(1f)
            )
        }
        AnimatedContent(
            targetState = hasTargetWeight,
            label = "targetWeightInput"
        ) { visible ->
            if (visible) {
                ProfileNumberField(
                    value = targetWeightInput,
                    onValueChange = onTargetWeightChange,
                    label = "Target weight kg",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MacroPreferencePage(
    macroPreference: MacroPreference,
    onMacroPreferenceChange: (MacroPreference) -> Unit
) {
    ProfileCard(
        icon = Icons.Default.MonitorWeight,
        title = "Macro style",
        subtitle = "This will adjust your daily targets for protein, fat and carbs"
    ) {
        SectionLabel("Do you have a preferred macro style?")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            macroPreferenceOptions.forEach { (preference, label) ->
                OptionPill(
                    text = label,
                    selected = macroPreference == preference,
                    onClick = { onMacroPreferenceChange(preference) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SafetyPage(
    recentWeightStatus: RecentWeightStatus,
    onRecentWeightStatusChange: (RecentWeightStatus) -> Unit,
    recentWeightAmountInput: String,
    onRecentWeightAmountChange: (String) -> Unit,
    recentWeightDaysInput: String,
    onRecentWeightDaysChange: (String) -> Unit,
    isPregnant: Boolean,
    onPregnantChange: (Boolean) -> Unit,
    isBreastfeeding: Boolean,
    onBreastfeedingChange: (Boolean) -> Unit,
    hasEatingDisorderHistory: Boolean,
    onEatingDisorderHistoryChange: (Boolean) -> Unit,
    hasRelevantMedicalCondition: Boolean,
    onMedicalConditionChange: (Boolean) -> Unit
) {
    val needsRecentDetails =
        recentWeightStatus == RecentWeightStatus.LOST_WEIGHT ||
            recentWeightStatus == RecentWeightStatus.GAINED_WEIGHT

    ProfileCard(
        icon = Icons.Default.MonitorWeight,
        title = "Recent trend",
        subtitle = "This helps the model avoid overreacting to short-term changes."
    ) {
        TwoColumnOptions(
            options = listOf(
                RecentWeightStatus.STABLE to "Stable",
                RecentWeightStatus.NOT_SURE to "Not sure",
                RecentWeightStatus.LOST_WEIGHT to "Lost weight",
                RecentWeightStatus.GAINED_WEIGHT to "Gained weight"
            ),
            selected = recentWeightStatus,
            onSelect = onRecentWeightStatusChange
        )
        AnimatedContent(
            targetState = needsRecentDetails,
            label = "recentWeightDetails"
        ) { visible ->
            if (visible) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileNumberField(
                        value = recentWeightAmountInput,
                        onValueChange = onRecentWeightAmountChange,
                        label = "Change kg",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileNumberField(
                        value = recentWeightDaysInput,
                        onValueChange = onRecentWeightDaysChange,
                        label = "Days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    ProfileCard(
        icon = Icons.Default.Flag,
        title = "Safety checks",
        subtitle = "This information will affect calorie intake"
    ) {
        SafetyCheckRow("Pregnant", isPregnant, onPregnantChange)
        SafetyCheckRow("Breastfeeding", isBreastfeeding, onBreastfeedingChange)
        SafetyCheckRow("Eating-disorder history", hasEatingDisorderHistory, onEatingDisorderHistoryChange)
        SafetyCheckRow(
            label = "Relevant medical condition",
            checked = hasRelevantMedicalCondition,
            onCheckedChange = onMedicalConditionChange,
            showInfoButton = true
        )
    }
}

@Composable
private fun ProfileCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
                }
            }
            content()
        }
    }
}

@Composable
private fun ProfileNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.all { it.isDigit() || it == '.' }) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    )
}

@Composable
private fun <T> TwoColumnOptions(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (value, label) ->
                    OptionPill(
                        text = label,
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OptionPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant
    val backgroundColor =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(16.dp)
            )
        }
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SafetyCheckRow(
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
                .clip(RoundedCornerShape(12.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (showInfoButton) {
                    IconButton(
                        onClick = { showInfo = !showInfo },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Medical condition info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
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
            Text(
                text = "Any condition or medication that affects nutrition or calorie needs, like diabetes or heart conditions",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        modifier = Modifier.padding(top = 4.dp)
    )
}

private val stepRangeOptions = listOf(
    ManualStepRange.LESS_THAN_3000 to "Under 3,000",
    ManualStepRange.FROM_3000_TO_5999 to "3,000-5,999",
    ManualStepRange.FROM_6000_TO_8999 to "6,000-8,999",
    ManualStepRange.FROM_9000_TO_11999 to "9,000-11,999",
    ManualStepRange.FROM_12000_TO_15999 to "12,000-15,999",
    ManualStepRange.FROM_16000_PLUS to "16,000+"
)

private val goalPaceOptions = listOf(
    GoalPace.SLOW to "Slow and easier to maintain",
    GoalPace.STANDARD to "Standard",
    GoalPace.FASTER to "Faster, still safe"
)

private val macroPreferenceOptions = listOf(
    MacroPreference.BALANCED to "Balanced",
    MacroPreference.HIGHER_CARB to "Higher carb",
    MacroPreference.LOWER_CARB to "Lower carb",
    MacroPreference.HIGHER_PROTEIN to "Higher protein",
    MacroPreference.HIGHER_FAT to "Higher fat"
)

private const val PROFILE_PAGE_COUNT = 5
