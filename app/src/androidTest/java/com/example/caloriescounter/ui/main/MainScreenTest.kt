package com.example.caloriescounter.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.caloriescounter.data.UserProfileRepository
import com.example.caloriescounter.theme.CaloriesCounterTheme
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun freshInstallCanOpenProfileSetupWithoutHealthConnect() {
        assumeFalse(UserProfileRepository(composeTestRule.activity).hasProfile())
        composeTestRule.setContent {
            CaloriesCounterTheme {
                MainScreen()
            }
        }
        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.onNodeWithText("Create Profile").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Let's setup your profile!").assertIsDisplayed()
    }
}
