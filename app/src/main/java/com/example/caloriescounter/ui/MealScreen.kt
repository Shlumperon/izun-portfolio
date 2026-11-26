package com.example.caloriescounter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.caloriescounter.theme.IzunCard
import com.example.caloriescounter.data.HealthConnectManager
import com.example.caloriescounter.data.MealRecord
import com.example.caloriescounter.data.MealRepository

private enum class MealMode(val label: String) {
    ADD("Add"),
    HISTORY("History")
}

enum class MealTutorialMode {
    ADD,
    HISTORY
}

data class MealTutorialState(
    val mode: MealTutorialMode?,
    val addStage: Int = 0,
    val historyExpanded: Boolean = false,
    val awaitingAddMode: Boolean = false,
    val mealName: String = "Ramen bowl",
    val caloriesText: String = "620 kcal",
    val timeText: String = "Today, 12:30",
    val proteinText: String = "24 g",
    val carbsText: String = "78 g",
    val fatText: String = "19 g",
    val fiberText: String = "6 g",
    val sodiumText: String = "980 mg",
    val sugarText: String = "7 g"
)

@Composable
fun MealScreen(
    mealRepository: MealRepository,
    healthConnectManager: HealthConnectManager,
    addMealDraft: MealRecord?,
    onAddMealDraftChanged: (MealRecord?) -> Unit,
    tutorialState: MealTutorialState? = null,
    onTutorialAddConfirmed: () -> Unit = {},
    onTutorialMealDeleted: () -> Unit = {},
    onTutorialAddModeSelected: () -> Unit = {},
    tutorialBottomPadding: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    var selectedMode by rememberSaveable { mutableStateOf(if (addMealDraft != null) MealMode.ADD else null) }
    var showInstructions by rememberSaveable { mutableStateOf(false) }
    val displayedMode = tutorialState?.mode?.let {
        when (it) {
            MealTutorialMode.ADD -> MealMode.ADD
            MealTutorialMode.HISTORY -> MealMode.HISTORY
        }
    } ?: selectedMode

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Meal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { showInstructions = !showInstructions },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "How to add meals",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showInstructions,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
        ) {
            IzunCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("How to add with a chatbot", fontWeight = FontWeight.Bold)
                    Text("1. Go to Settings and copy the Chat Prompt", fontSize = 13.sp)
                    Text("2. Send it to your chatbot with a meal photo or description", fontSize = 13.sp)
                    Text("3. Copy the IZUN Report from the chatbot response", fontSize = 13.sp)
                    Text("4. Choose Add, then tap Paste to fill the meal card. Review the values before confirming", fontSize = 13.sp)
                }
            }
        }

        MealModeSelector(
            selectedMode = displayedMode,
            onModeSelected = {
                if (tutorialState?.awaitingAddMode == true && it == MealMode.ADD) {
                    onTutorialAddModeSelected()
                } else if (tutorialState == null) {
                    selectedMode = it
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )

        AnimatedContent(
            targetState = displayedMode,
            transitionSpec = {
                (fadeIn(tween(180)) togetherWith fadeOut(tween(120))).using(SizeTransform(clip = false))
            },
            label = "mealModeContent",
            modifier = Modifier.weight(1f)
        ) { mode ->
            when (mode) {
                MealMode.ADD -> {
                    if (tutorialState != null) {
                        TutorialAddMealCard(
                            tutorialState = tutorialState,
                            onConfirm = onTutorialAddConfirmed,
                            bottomContentPadding = tutorialBottomPadding,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PasteScreen(
                            mealRepository = mealRepository,
                            healthConnectManager = healthConnectManager,
                            controlledActiveMeal = addMealDraft,
                            onActiveMealChanged = onAddMealDraftChanged,
                            showStandaloneHeader = false,
                            showAddLauncher = false,
                            openOnEntry = true,
                            onMealCompleted = {
                                selectedMode = null
                                onAddMealDraftChanged(null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                MealMode.HISTORY -> {
                    if (tutorialState != null) {
                        TutorialHistoryMealCard(
                            expanded = tutorialState.historyExpanded,
                            onDelete = onTutorialMealDeleted,
                            bottomContentPadding = tutorialBottomPadding,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        HistoryScreen(
                            mealRepository = mealRepository,
                            healthConnectManager = healthConnectManager,
                            showHeader = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "Choose Add to create a meal or History to manage past meals",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialAddMealCard(
    tutorialState: MealTutorialState,
    onConfirm: () -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val stage = tutorialState.addStage
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            IzunCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {}) { Text("Paste") }
                        TextButton(onClick = {}) { Text("Clear") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text(
                                text = if (stage >= 1) tutorialState.mealName else "Meal title",
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = if (stage >= 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                            )
                            Text(
                                text = if (stage >= 2) tutorialState.caloriesText else "Calories",
                                color = if (stage >= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Text(
                        text = if (stage >= 2) tutorialState.timeText else "Date and time",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (stage >= 2) 0.74f else 0.42f),
                        fontSize = 13.sp
                    )
                    Text("Nutrient Breakdown", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TutorialNutrientChip("Protein", if (stage >= 3) tutorialState.proteinText else "--", Modifier.weight(1f))
                        TutorialNutrientChip("Carbs", if (stage >= 3) tutorialState.carbsText else "--", Modifier.weight(1f))
                        TutorialNutrientChip("Fats", if (stage >= 3) tutorialState.fatText else "--", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TutorialNutrientChip("Fiber", if (stage >= 3) tutorialState.fiberText else "--", Modifier.weight(1f))
                        TutorialNutrientChip("Sodium", if (stage >= 3) tutorialState.sodiumText else "--", Modifier.weight(1f))
                        TutorialNutrientChip("Sugar", if (stage >= 3) tutorialState.sugarText else "--", Modifier.weight(1f))
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = stage >= 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialNutrientChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 11.sp)
    }
}

@Composable
private fun TutorialHistoryMealCard(
    expanded: Boolean,
    onDelete: () -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Search meals") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            TutorialHistorySummaryCard(
                title = "Oat yogurt bowl",
                subtitle = "Breakfast - Today, 08:15",
                calories = "430 kcal"
            )
        }
        item {
            IzunCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ramen bowl", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Lunch - Today, 12:30", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp)
                        }
                        Text("620 kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "This is an example meal. In the real app, these controls help reuse or edit meals you already logged",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {}, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Add, contentDescription = "Re-add", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Re-add")
                                }
                                TextButton(onClick = {}, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Edit")
                                }
                                TextButton(onClick = onDelete) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialHistorySummaryCard(
    title: String,
    subtitle: String,
    calories: String
) {
    IzunCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp)
                }
            }
            Text(calories, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MealModeSelector(
    selectedMode: MealMode?,
    onModeSelected: (MealMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MealMode.entries.forEach { mode ->
            val selected = selectedMode == mode
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f))
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (mode == MealMode.ADD) Icons.Default.Add else Icons.Default.History,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(7.dp))
                Text(
                    text = mode.label,
                    textAlign = TextAlign.Center,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
        }
    }
}
