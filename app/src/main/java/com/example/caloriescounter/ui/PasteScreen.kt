package com.example.caloriescounter.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.caloriescounter.theme.IzunCard
import com.example.caloriescounter.data.FoodJsonParser
import com.example.caloriescounter.data.HealthConnectManager
import com.example.caloriescounter.data.MealRecord
import com.example.caloriescounter.data.MealRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID

@Composable
fun PasteScreen(
    mealRepository: MealRepository,
    healthConnectManager: HealthConnectManager,
    controlledActiveMeal: MealRecord? = null,
    onActiveMealChanged: ((MealRecord?) -> Unit)? = null,
    showStandaloneHeader: Boolean = true,
    showAddLauncher: Boolean = true,
    openOnEntry: Boolean = false,
    onMealCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var localActiveMeal by remember { mutableStateOf<MealRecord?>(null) }
    val activeMeal = controlledActiveMeal ?: localActiveMeal
    var highlightName by remember { mutableStateOf(false) }
    var highlightCalories by remember { mutableStateOf(false) }
    var syncLoading by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(false) }
    val cardBringIntoViewRequester = remember { BringIntoViewRequester() }

    fun showMealMessage(message: String, duration: Int) {
        ContextCompat.getMainExecutor(context).execute {
            Toast.makeText(context, message, duration).show()
        }
    }

    fun emptyManualMeal(now: Long = System.currentTimeMillis()): MealRecord {
        return MealRecord(
            name = "",
            mealType = "snack",
            timestamp = now,
            endTimestamp = now + 60_000,
            calories = 0.0,
            healthConnectClientRecordId = "calories-counter-${UUID.randomUUID()}"
        )
    }

    fun clipboardText(): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        return if (clipData != null && clipData.itemCount > 0) {
            clipData.getItemAt(0).text?.toString().orEmpty()
        } else {
            ""
        }
    }

    fun parseClipboardMeal(): MealRecord? {
        val text = clipboardText()
        if (text.isBlank()) return null
        return runCatching { FoodJsonParser.parse(text) }.getOrNull()
    }

    fun resetValidation() {
        highlightName = false
        highlightCalories = false
    }

    fun setActiveMeal(meal: MealRecord?) {
        if (onActiveMealChanged != null) {
            localActiveMeal = null
            onActiveMealChanged(meal)
        } else {
            localActiveMeal = meal
        }
    }

    fun startBlankMeal() {
        // Keep an untouched blank form local so reopening it uses the current time.
        onActiveMealChanged?.invoke(null)
        localActiveMeal = emptyManualMeal()
    }

    fun openMealCard() {
        startBlankMeal()
        resetValidation()
    }

    LaunchedEffect(openOnEntry) {
        if (openOnEntry && activeMeal == null) {
            openMealCard()
        }
    }

    fun pasteMealIntoCard() {
        val pastedMeal = parseClipboardMeal()
        if (pastedMeal != null) {
            setActiveMeal(pastedMeal)
            resetValidation()
        } else {
            showMealMessage("Could not read the pasted meal", Toast.LENGTH_LONG)
        }
    }

    fun clearMealCard() {
        startBlankMeal()
        resetValidation()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showStandaloneHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable { showInstructions = !showInstructions },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "How to add meals",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // App Header
            Text(
                text = "IZUN",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        AnimatedVisibility(
            visible = showStandaloneHeader && showInstructions,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            IzunCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How to add a meal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("1. Go to Settings and copy the Chat Prompt", fontSize = 13.sp)
                    Text("2. Paste it into your chatbot, then send a meal photo, label, or description", fontSize = 13.sp)
                    Text("3. Copy the IZUN Report the chatbot gives you", fontSize = 13.sp)
                    Text("4. Open the meal card and tap Paste, or enter your meal manually", fontSize = 13.sp)
                    Text("5. Press the Confirm button at the end of the card", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (showAddLauncher) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { openMealCard() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add meal",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Preview Section
        AnimatedVisibility(
            visible = activeMeal != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            activeMeal?.let { meal ->
                PreviewCard(
                    meal = meal,
                    onMealChanged = {
                        setActiveMeal(it)
                        if (it.name.isNotBlank()) highlightName = false
                        if (it.calories > 0.0) highlightCalories = false
                    },
                    manualEntryMode = true,
                    highlightName = highlightName,
                    highlightCalories = highlightCalories,
                    onPasteClick = { pasteMealIntoCard() },
                    onClearClick = { clearMealCard() },
                    modifier = Modifier.bringIntoViewRequester(cardBringIntoViewRequester),
                    syncLoading = syncLoading,
                    onSyncClick = confirm@{
                        if (syncLoading) return@confirm
                        syncLoading = true
                        scope.launch {
                            val currentMeal = meal
                            val missingName = currentMeal.name.isBlank()
                            val missingCalories = !currentMeal.calories.isFinite() || currentMeal.calories <= 0.0
                            if (missingName || missingCalories) {
                                syncLoading = false
                                highlightName = missingName
                                highlightCalories = missingCalories
                                showMealMessage("Meal name and calories are required", Toast.LENGTH_LONG)
                                cardBringIntoViewRequester.bringIntoView()
                                return@launch
                            }

                            try {
                                val attemptSync = healthConnectManager.isSdkAvailable() && healthConnectManager.hasWritePermission()
                                val local = mealRepository.saveHistoryChange(currentMeal, attemptSync)
                                // Keep the durable meal ID if leaving the screen interrupts the optional sync.
                                setActiveMeal(local)
                                val synced = attemptSync && mealRepository.syncSavedMeal(local, healthConnectManager)
                                val message = if (synced) "Meal saved and synced." else "Saved on this device."
                                showMealMessage(message, Toast.LENGTH_SHORT)
                                setActiveMeal(null)
                                onMealCompleted()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                showMealMessage("Unable to finish saving this meal. Try again.", Toast.LENGTH_LONG)
                            } finally {
                                syncLoading = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PreviewCard(
    meal: MealRecord,
    onMealChanged: (MealRecord) -> Unit,
    manualEntryMode: Boolean = false,
    highlightName: Boolean = false,
    highlightCalories: Boolean = false,
    onPasteClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    syncLoading: Boolean,
    onSyncClick: () -> Unit
) {
    val context = LocalContext.current
    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember(meal.name) { mutableStateOf(meal.name) }
    var caloriesInput by remember(meal.healthConnectClientRecordId) {
        mutableStateOf(meal.calories.takeIf { it > 0.0 }?.formatForInput().orEmpty())
    }
    var editingNutrient by remember { mutableStateOf<Pair<String, Double?>?>(null) }

    fun getUnit(key: String): String {
        return when (key.lowercase()) {
            "calories" -> "kcal"
            "sodium", "potassium", "cholesterol", "calcium", "iron", "magnesium", "zinc", "vitamin c", "vitamin e", "caffeine" -> "mg"
            "vitamin a", "vitamin d" -> "mcg"
            else -> "g"
        }
    }

    if (editingNutrient != null) {
        val (nutrientKey, currentValue) = editingNutrient!!
        var inputValue by remember { mutableStateOf(currentValue?.toString() ?: "") }
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingNutrient = null },
            title = { Text("Edit ${nutrientKey.replaceFirstChar { it.uppercase() }}") },
            text = {
                Column {
                    Text("Enter value in ${getUnit(nutrientKey)}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newValue = inputValue.toDoubleOrNull()
                        val updatedMeal = when (nutrientKey.lowercase()) {
                            "calories" -> meal.copy(calories = newValue ?: 0.0)
                            "protein" -> meal.copy(protein = newValue)
                            "carbs" -> meal.copy(carbs = newValue)
                            "fats" -> meal.copy(fat = newValue)
                            "fiber" -> meal.copy(fiber = newValue)
                            "sugar" -> meal.copy(sugar = newValue)
                            "saturated fat" -> meal.copy(saturatedFat = newValue)
                            "unsaturated fat" -> meal.copy(unsaturatedFat = newValue)
                            "trans fat" -> meal.copy(transFat = newValue)
                            "sodium" -> meal.copy(sodium = newValue)
                            "potassium" -> meal.copy(potassium = newValue)
                            "cholesterol" -> meal.copy(cholesterol = newValue)
                            "calcium" -> meal.copy(calcium = newValue)
                            "iron" -> meal.copy(iron = newValue)
                            "magnesium" -> meal.copy(magnesium = newValue)
                            "zinc" -> meal.copy(zinc = newValue)
                            "vitamin a" -> meal.copy(vitaminA = newValue)
                            "vitamin c" -> meal.copy(vitaminC = newValue)
                            "vitamin d" -> meal.copy(vitaminD = newValue)
                            "vitamin e" -> meal.copy(vitaminE = newValue)
                            "caffeine" -> meal.copy(caffeine = newValue)
                            else -> meal
                        }
                        if (manualEntryMode && nutrientKey.equals("calories", ignoreCase = true)) {
                            caloriesInput = updatedMeal.calories.takeIf { it > 0.0 }?.formatForInput().orEmpty()
                        }
                        onMealChanged(updatedMeal)
                        editingNutrient = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { editingNutrient = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    IzunCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Card Title (Edit Pencil Removed)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (manualEntryMode) {
                    TextButton(onClick = { onPasteClick?.invoke() }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onClearClick?.invoke() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                } else {
                    Text(
                        text = "Review",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Meal Name
            if (manualEntryMode) {
                key(meal.healthConnectClientRecordId) {
                    MealNameField(meal, onMealChanged, isError = highlightName)
                }
            } else if (isEditingName) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onMealChanged(meal.copy(name = nameInput))
                                isEditingName = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save name",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (highlightName) MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .then(
                            if (highlightName) {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable { isEditingName = true }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = meal.name.ifBlank { "Meal name" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (highlightName) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = if (highlightName) 10.dp else 0.dp, vertical = if (highlightName) 6.dp else 0.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (manualEntryMode) {
                OutlinedTextField(
                    value = caloriesInput,
                    onValueChange = { input ->
                        caloriesInput = input
                        val parsedCalories = input.toDoubleOrNull()
                        onMealChanged(meal.copy(calories = parsedCalories ?: 0.0))
                    },
                    isError = highlightCalories,
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Meal Type Selector
            Text("Meal Type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val types = listOf("breakfast", "lunch", "dinner", "snack")
                val selectedMealType = meal.mealType.lowercase().takeIf { it in types } ?: "snack"
                val mealTypeFontSize = if (manualEntryMode) 12.sp else 9.sp
                types.forEach { type ->
                    val isSelected = selectedMealType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onMealChanged(meal.copy(mealType = type)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.replaceFirstChar { it.uppercase() },
                            fontSize = mealTypeFontSize,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date & Time Picker
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
            val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(meal.timestamp), ZoneId.systemDefault())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Date Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = meal.timestamp
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, day)
                                    val duration = (meal.endTimestamp - meal.timestamp).coerceAtLeast(60_000)
                                    onMealChanged(meal.copy(timestamp = calendar.timeInMillis, endTimestamp = calendar.timeInMillis + duration))
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Date", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(localDateTime.format(formatter), fontSize = 13.sp)
                }

                // Time Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = meal.timestamp
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    val duration = (meal.endTimestamp - meal.timestamp).coerceAtLeast(60_000)
                                    onMealChanged(meal.copy(timestamp = calendar.timeInMillis, endTimestamp = calendar.timeInMillis + duration))
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                            ).show()
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Time", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(localDateTime.format(timeFormatter), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nutrients Grid
            Text("Nutrient Breakdown", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            // Grid Layout
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutrientBadge(name = "Calories", value = "${meal.calories.toInt()} kcal", color = MaterialTheme.colorScheme.primary, highlight = highlightCalories, onClick = { editingNutrient = Pair("Calories", meal.calories) }, modifier = Modifier.weight(1f))
                    NutrientBadge(name = "Protein", value = meal.protein?.let { "${it} g" } ?: "--", color = Color(0xFFF59E0B), onClick = { editingNutrient = Pair("Protein", meal.protein) }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutrientBadge(name = "Carbs", value = meal.carbs?.let { "${it} g" } ?: "--", color = Color(0xFF3B82F6), onClick = { editingNutrient = Pair("Carbs", meal.carbs) }, modifier = Modifier.weight(1f))
                    NutrientBadge(name = "Fats", value = meal.fat?.let { "${it} g" } ?: "--", color = Color(0xFFEF4444), onClick = { editingNutrient = Pair("Fats", meal.fat) }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutrientBadge(name = "Fiber", value = meal.fiber?.let { "${it} g" } ?: "--", color = Color(0xFF10B981), onClick = { editingNutrient = Pair("Fiber", meal.fiber) }, modifier = Modifier.weight(1f))
                    NutrientBadge(name = "Sugar", value = meal.sugar?.let { "${it} g" } ?: "--", color = Color(0xFFEC4899), onClick = { editingNutrient = Pair("Sugar", meal.sugar) }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutrientBadge(name = "Sat Fat", value = meal.saturatedFat?.let { "${it} g" } ?: "--", color = Color(0xFFF97316), onClick = { editingNutrient = Pair("Saturated Fat", meal.saturatedFat) }, modifier = Modifier.weight(1f))
                    NutrientBadge(name = "Trans Fat", value = meal.transFat?.let { "${it} g" } ?: "--", color = Color(0xFFB91C1C), onClick = { editingNutrient = Pair("Trans Fat", meal.transFat) }, modifier = Modifier.weight(1f))
                }
                if (meal.sodium != null || meal.potassium != null || meal.cholesterol != null || meal.calcium != null || meal.iron != null || meal.magnesium != null || meal.zinc != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientBadge(name = "Sodium", value = meal.sodium?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF6B7280), onClick = { editingNutrient = Pair("Sodium", meal.sodium) }, modifier = Modifier.weight(1f))
                        NutrientBadge(name = "Potassium", value = meal.potassium?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF8B5CF6), onClick = { editingNutrient = Pair("Potassium", meal.potassium) }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientBadge(name = "Cholesterol", value = meal.cholesterol?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF64748B), onClick = { editingNutrient = Pair("Cholesterol", meal.cholesterol) }, modifier = Modifier.weight(1f))
                        NutrientBadge(name = "Calcium", value = meal.calcium?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF14B8A6), onClick = { editingNutrient = Pair("Calcium", meal.calcium) }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientBadge(name = "Iron", value = meal.iron?.let { "${it} mg" } ?: "--", color = Color(0xFF78716C), onClick = { editingNutrient = Pair("Iron", meal.iron) }, modifier = Modifier.weight(1f))
                        NutrientBadge(name = "Magnesium", value = meal.magnesium?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF0EA5E9), onClick = { editingNutrient = Pair("Magnesium", meal.magnesium) }, modifier = Modifier.weight(1f))
                    }
                }
                if (meal.vitaminA != null || meal.vitaminC != null || meal.vitaminD != null || meal.vitaminE != null || meal.caffeine != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientBadge(name = "Vitamin A", value = meal.vitaminA?.let { "${it.toInt()} mcg" } ?: "--", color = Color(0xFFFBBF24), onClick = { editingNutrient = Pair("Vitamin A", meal.vitaminA) }, modifier = Modifier.weight(1f))
                        NutrientBadge(name = "Vitamin C", value = meal.vitaminC?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF22C55E), onClick = { editingNutrient = Pair("Vitamin C", meal.vitaminC) }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientBadge(name = "Vitamin D", value = meal.vitaminD?.let { "${it.toInt()} mcg" } ?: "--", color = Color(0xFFFACC15), onClick = { editingNutrient = Pair("Vitamin D", meal.vitaminD) }, modifier = Modifier.weight(1f))
                        NutrientBadge(name = "Caffeine", value = meal.caffeine?.let { "${it.toInt()} mg" } ?: "--", color = Color(0xFF92400E), onClick = { editingNutrient = Pair("Caffeine", meal.caffeine) }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sync Button with status transitions
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (syncLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onSyncClick() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Confirm",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MealNameField(
    meal: MealRecord,
    onMealChanged: (MealRecord) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    scrollState: ScrollState = rememberScrollState()
) {
    val name = rememberTextFieldState(initialText = meal.name, initialSelection = TextRange.Zero)
    val currentMeal by rememberUpdatedState(meal)
    val updateMeal by rememberUpdatedState(onMealChanged)
    LaunchedEffect(name) {
        snapshotFlow { name.text.toString() }.collect { text ->
            if (text != currentMeal.name) updateMeal(currentMeal.copy(name = text))
        }
    }
    OutlinedTextField(
        state = name,
        label = { Text("Meal name") },
        isError = isError,
        lineLimits = TextFieldLineLimits.SingleLine,
        scrollState = scrollState,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun NutrientBadge(
    name: String,
    value: String,
    color: Color,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .then(
                if (highlight) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Double.formatForInput(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}
