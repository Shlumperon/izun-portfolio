package com.example.caloriescounter.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.caloriescounter.data.HealthConnectManager
import com.example.caloriescounter.data.MealDatabase
import com.example.caloriescounter.data.MealRepository
import com.example.caloriescounter.ui.MealScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeFalse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MealDraftTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mealDraftSurvivesTabChangesAndSavesWithoutHealthConnect() {
        val context = compose.activity.applicationContext
        val health = HealthConnectManager(context)
        assumeFalse(runBlocking { health.hasWritePermission() })
        val database = Room.inMemoryDatabaseBuilder(context, MealDatabase::class.java).build()
        try {
            val repository = MealRepository(database.mealDao())
            showMealScreen(repository, health)
            compose.onNodeWithText("Add").performClick()
            compose.onNode(hasSetTextAction() and hasText("Meal name")).performTextReplacement("Berry oatmeal")
            compose.onNode(hasSetTextAction() and hasText("Calories")).performTextReplacement("425")
            compose.onNodeWithText("History").performClick()
            compose.onNodeWithText("Add").performClick()
            compose.onNodeWithText("425").assertIsDisplayed()
            compose.onNodeWithText("Switch tab").performClick()
            compose.onNodeWithText("Switch tab").performClick()
            compose.onNodeWithText("425").assertIsDisplayed()

            compose.onNodeWithText("Confirm").performScrollTo().performClick()
            val meal = runBlocking {
                withTimeout(5_000) { repository.allMeals.first { it.isNotEmpty() }.single() }
            }
            assertEquals("Berry oatmeal", meal.name)
            assertEquals(425.0, meal.calories, 0.0)
            assertEquals(false, meal.healthConnectSynced)
            compose.waitForIdle()
        } finally {
            database.close()
        }
    }

    private fun showMealScreen(repository: MealRepository, health: HealthConnectManager) {
        compose.setContent {
            val state: MealDraftViewModel = viewModel()
            var inMeal by remember { mutableStateOf(true) }
            Column {
                Button(onClick = { inMeal = !inMeal }) { Text("Switch tab") }
                if (inMeal) {
                    MealScreen(
                        mealRepository = repository,
                        healthConnectManager = health,
                        addMealDraft = state.draft,
                        onAddMealDraftChanged = { state.draft = it },
                        onHistoryPermissionNeeded = {}
                    )
                }
            }
        }
    }
}
