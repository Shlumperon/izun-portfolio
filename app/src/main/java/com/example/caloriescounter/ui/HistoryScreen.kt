package com.example.caloriescounter.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.caloriescounter.theme.IzunCard
import com.example.caloriescounter.data.HealthConnectManager
import com.example.caloriescounter.data.newMealOccurrence
import com.example.caloriescounter.data.MealRecord
import com.example.caloriescounter.data.MealRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class NutrientColorCandidate(
    val grams: Double,
    val color: Color
)

@Composable
fun HistoryScreen(
    mealRepository: MealRepository,
    healthConnectManager: HealthConnectManager,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val meals by mealRepository.allMeals.collectAsState(initial = emptyList())
    var expandedMealId by remember { mutableStateOf<Int?>(null) }
    var editingMeal by remember { mutableStateOf<MealRecord?>(null) }
    var editSyncLoading by remember { mutableStateOf(false) }
    var editHighlightName by remember { mutableStateOf(false) }
    var editHighlightCalories by remember { mutableStateOf(false) }
    var addingMealAgain by remember { mutableStateOf<MealRecord?>(null) }
    var addAgainSyncLoading by remember { mutableStateOf(false) }
    var addAgainHighlightName by remember { mutableStateOf(false) }
    var addAgainHighlightCalories by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredMeals = remember(meals, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            meals
        } else {
            meals.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    fun validateRequiredMealFields(
        meal: MealRecord,
        setHighlightName: (Boolean) -> Unit,
        setHighlightCalories: (Boolean) -> Unit
    ): Boolean {
        val missingName = meal.name.isBlank()
        val missingCalories = meal.calories <= 0.0
        setHighlightName(missingName)
        setHighlightCalories(missingCalories)
        if (missingName || missingCalories) {
            Toast.makeText(context, "Meal name and calories are required", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    suspend fun saveHistoryMeal(meal: MealRecord, onLocalSave: (MealRecord) -> Unit): Boolean {
        return try {
            val attemptSync = healthConnectManager.isSdkAvailable() && healthConnectManager.hasWritePermission()
            val local = mealRepository.saveHistoryChange(meal, attemptSync)
            onLocalSave(local)
            val message = if (attemptSync && local.healthConnectClientRecordId.isNotBlank()) {
                if (mealRepository.syncSavedMeal(local, healthConnectManager)) "Meal saved." else "Saved on this device."
            } else {
                "Saved on this device."
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to finish saving this meal. Try again.", Toast.LENGTH_LONG).show()
            false
        }
    }

    editingMeal?.let { draftMeal ->
        AlertDialog(
            onDismissRequest = {
                if (!editSyncLoading) {
                    editingMeal = null
                    editHighlightName = false
                    editHighlightCalories = false
                }
            },
            title = { Text("Edit Meal") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    PreviewCard(
                        meal = draftMeal,
                        onMealChanged = {
                            editingMeal = it
                            if (it.name.isNotBlank()) editHighlightName = false
                            if (it.calories > 0.0) editHighlightCalories = false
                        },
                        highlightName = editHighlightName,
                        highlightCalories = editHighlightCalories,
                        syncLoading = editSyncLoading,
                        onSyncClick = {
                            scope.launch {
                                val currentMeal = editingMeal
                                if (currentMeal == null) {
                                    return@launch
                                }
                                if (!validateRequiredMealFields(currentMeal, { editHighlightName = it }, { editHighlightCalories = it })) {
                                    return@launch
                                }
                                if (editSyncLoading) return@launch
                                editSyncLoading = true
                                try {
                                    if (saveHistoryMeal(currentMeal) { editingMeal = it }) {
                                        editingMeal = null
                                    }
                                } finally {
                                    editSyncLoading = false
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!editSyncLoading) {
                            editingMeal = null
                            editHighlightName = false
                            editHighlightCalories = false
                        }
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    addingMealAgain?.let { draftMeal ->
        AlertDialog(
            onDismissRequest = {
                if (!addAgainSyncLoading) {
                    addingMealAgain = null
                    addAgainHighlightName = false
                    addAgainHighlightCalories = false
                }
            },
            title = { Text("Add Again") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    PreviewCard(
                        meal = draftMeal,
                        onMealChanged = {
                            addingMealAgain = it
                            if (it.name.isNotBlank()) addAgainHighlightName = false
                            if (it.calories > 0.0) addAgainHighlightCalories = false
                        },
                        highlightName = addAgainHighlightName,
                        highlightCalories = addAgainHighlightCalories,
                        syncLoading = addAgainSyncLoading,
                        onSyncClick = {
                            scope.launch {
                                val currentMeal = addingMealAgain ?: return@launch
                                if (!validateRequiredMealFields(currentMeal, { addAgainHighlightName = it }, { addAgainHighlightCalories = it })) {
                                    return@launch
                                }
                                if (addAgainSyncLoading) return@launch
                                addAgainSyncLoading = true
                                try {
                                    if (saveHistoryMeal(currentMeal) { addingMealAgain = it }) {
                                        addingMealAgain = null
                                    }
                                } finally {
                                    addAgainSyncLoading = false
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!addAgainSyncLoading) {
                            addingMealAgain = null
                            addAgainHighlightName = false
                            addAgainHighlightCalories = false
                        }
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showHeader) {
            Text(
                text = "Meal History",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Text(
                text = "Review and manage your previously synced meals saved locally",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (meals.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history recorded yet\nGo to the Meal tab and choose \"Add\" to create one",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by meal title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredMeals.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching meals",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
            } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredMeals, key = { it.id }) { meal ->
                    val isExpanded = expandedMealId == meal.id
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        HistoryItemCard(
                            meal = meal,
                            isExpanded = isExpanded,
                            onExpandClick = {
                                expandedMealId = if (isExpanded) null else meal.id
                            },
                            onDeleteClick = {
                                scope.launch {
                                    if (editSyncLoading || addAgainSyncLoading) return@launch
                                    val needsRemoteDelete = meal.healthConnectClientRecordId.isNotBlank() &&
                                        (meal.healthConnectSynced || meal.healthConnectClientRecordVersion > 0)
                                    if (needsRemoteDelete && healthConnectManager.isSdkAvailable() && !healthConnectManager.hasWritePermission()) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            Toast.makeText(context, "Allow nutrition access in Settings > Health Connect to remove this synced meal.", Toast.LENGTH_LONG).show()
                                        }
                                        return@launch
                                    }
                                    val result = healthConnectManager.deleteMeal(meal)
                                    if (result.success) mealRepository.deleteMeal(meal)
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                }
                            },
                            onAddAgainClick = {
                                addingMealAgain = meal.newMealOccurrence()
                                addAgainHighlightName = false
                                addAgainHighlightCalories = false
                            },
                            onEditClick = {
                                editingMeal = meal
                                editHighlightName = false
                                editHighlightCalories = false
                            }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    meal: MealRecord,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddAgainClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a")
    val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(meal.timestamp), ZoneId.systemDefault())
    val dominantNutrientColor = meal.dominantNutrientColor()

    IzunCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onExpandClick).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dominantNutrientColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = meal.mealType.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Text(
                        text = localDateTime.format(dateTimeFormatter),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = when {
                            meal.healthConnectSynced && meal.healthConnectClientRecordId.isNotBlank() -> "Health Connect synced"
                            else -> "Saved on this device"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            meal.healthConnectSynced && meal.healthConnectClientRecordId.isNotBlank() -> Color(0xFF10B981)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }

                Text(
                    text = "${meal.calories.toInt()} kcal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientBadge(name = "Protein", value = meal.protein?.let { "${it} g" } ?: "--", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                        NutrientBadge(name = "Carbs", value = meal.carbs?.let { "${it} g" } ?: "--", color = Color(0xFF3B82F6), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientBadge(name = "Fats", value = meal.fat?.let { "${it} g" } ?: "--", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                        NutrientBadge(name = "Fiber", value = meal.fiber?.let { "${it} g" } ?: "--", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientBadge(name = "Sugar", value = meal.sugar?.let { "${it} g" } ?: "--", color = Color(0xFFEC4899), modifier = Modifier.weight(1f))
                        NutrientBadge(name = "Sodium", value = meal.sodium?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF6B7280), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientBadge(name = "Sat Fat", value = meal.saturatedFat?.let { "${it} g" } ?: "--", color = Color(0xFFF97316), modifier = Modifier.weight(1f))
                        NutrientBadge(name = "Trans Fat", value = meal.transFat?.let { "${it} g" } ?: "--", color = Color(0xFFB91C1C), modifier = Modifier.weight(1f))
                    }
                    if (meal.potassium != null || meal.cholesterol != null || meal.calcium != null || meal.iron != null || meal.magnesium != null || meal.zinc != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NutrientBadge(name = "Potassium", value = meal.potassium?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                            NutrientBadge(name = "Cholesterol", value = meal.cholesterol?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF64748B), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NutrientBadge(name = "Calcium", value = meal.calcium?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF14B8A6), modifier = Modifier.weight(1f))
                            NutrientBadge(name = "Iron", value = meal.iron?.let { "${it} mg" } ?: "--", color = Color(0xFF78716C), modifier = Modifier.weight(1f))
                        }
                    }
                    if (meal.vitaminA != null || meal.vitaminC != null || meal.vitaminD != null || meal.vitaminE != null || meal.caffeine != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NutrientBadge(name = "Vitamin A", value = meal.vitaminA?.let { "${it.toInt()} mcg" } ?: "--", color = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
                            NutrientBadge(name = "Vitamin C", value = meal.vitaminC?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF22C55E), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NutrientBadge(name = "Vitamin D", value = meal.vitaminD?.let { "${it.toInt()} mcg" } ?: "--", color = Color(0xFFFACC15), modifier = Modifier.weight(1f))
                            NutrientBadge(name = "Caffeine", value = meal.caffeine?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF92400E), modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onAddAgainClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Re-add",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-add")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun MealRecord.dominantNutrientColor(): Color {
    val candidates = listOfNotNull(
        protein?.asGramCandidate(Color(0xFFF59E0B)),
        carbs?.asGramCandidate(Color(0xFF3B82F6)),
        fat?.asGramCandidate(Color(0xFFEF4444)),
        fiber?.asGramCandidate(Color(0xFF10B981)),
        sugar?.asGramCandidate(Color(0xFFEC4899)),
        saturatedFat?.asGramCandidate(Color(0xFFF97316)),
        transFat?.asGramCandidate(Color(0xFFB91C1C)),
        sodium?.milligramsAsGramCandidate(Color(0xFF6B7280)),
        potassium?.milligramsAsGramCandidate(Color(0xFF8B5CF6)),
        cholesterol?.milligramsAsGramCandidate(Color(0xFF64748B)),
        calcium?.milligramsAsGramCandidate(Color(0xFF14B8A6)),
        iron?.milligramsAsGramCandidate(Color(0xFF78716C)),
        magnesium?.milligramsAsGramCandidate(Color(0xFF0EA5E9)),
        zinc?.milligramsAsGramCandidate(Color(0xFF78716C)),
        vitaminA?.microgramsAsGramCandidate(Color(0xFFFBBF24)),
        vitaminC?.milligramsAsGramCandidate(Color(0xFF22C55E)),
        vitaminD?.microgramsAsGramCandidate(Color(0xFFFACC15)),
        vitaminE?.milligramsAsGramCandidate(Color(0xFFFBBF24)),
        caffeine?.milligramsAsGramCandidate(Color(0xFF92400E))
    )

    return candidates.maxByOrNull { it.grams }?.color ?: Color(0xFF6B7280)
}

private fun Double.asGramCandidate(color: Color): NutrientColorCandidate? {
    return takeIf { it > 0.0 }?.let { NutrientColorCandidate(it, color) }
}

private fun Double.milligramsAsGramCandidate(color: Color): NutrientColorCandidate? {
    return takeIf { it > 0.0 }?.let { NutrientColorCandidate(it / 1_000.0, color) }
}

private fun Double.microgramsAsGramCandidate(color: Color): NutrientColorCandidate? {
    return takeIf { it > 0.0 }?.let { NutrientColorCandidate(it / 1_000_000.0, color) }
}
