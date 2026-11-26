package com.example.caloriescounter.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.health.connect.client.PermissionController
import com.example.caloriescounter.BuildConfig
import com.example.caloriescounter.R
import com.example.caloriescounter.data.HealthConnectManager
import com.example.caloriescounter.data.DailyHealthSummary
import com.example.caloriescounter.data.DailyStepTotal
import com.example.caloriescounter.data.MealDatabase
import com.example.caloriescounter.data.MealRecord
import com.example.caloriescounter.data.MealRepository
import com.example.caloriescounter.data.UserProfileRepository
import com.example.caloriescounter.data.WeightSample
import com.example.caloriescounter.data.WeightRepository
import com.example.caloriescounter.ui.HomeDemoData
import com.example.caloriescounter.ui.HomeScreen
import com.example.caloriescounter.ui.MealScreen
import com.example.caloriescounter.ui.MealTutorialMode
import com.example.caloriescounter.ui.MealTutorialState
import com.example.caloriescounter.ui.ProfileCreationScreen
import com.example.caloriescounter.ui.SettingsScreen
import com.example.caloriescounter.nutrition.CalorieRange
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

private const val HEALTH_PERMISSION_PREFS = "health_permission_onboarding"
private const val KEY_NUTRITION_PERMISSION_REQUESTED = "nutrition_permission_requested"
private const val KEY_EDIT_PERMISSION_REQUESTED = "edit_permission_requested"
private const val KEY_DASHBOARD_PERMISSION_REQUESTED = "dashboard_permission_requested"
private const val KEY_CHAT_PROMPT_COPIED = "chat_prompt_copied"
private const val KEY_LANDING_TUTORIAL_COMPLETED = "landing_tutorial_completed"

private enum class LandingTutorialStep {
    HOME_INTRO,
    MEAL_INTRO,
    ADD_DEMO,
    HISTORY_DELETE,
    SETTINGS_CUSTOMIZE,
    SETTINGS_PROMPT,
    CHATBOT_OUTPUT,
    CHATBOT_ADD_PROMPT,
    RAMEN_ADD,
    FINISH_HOME
}

internal class MealDraftViewModel : ViewModel() {
    var draft by mutableStateOf<MealRecord?>(null)
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    // Initialize data layers
    val database = remember { MealDatabase.getDatabase(appContext) }
    val mealRepository = remember { MealRepository(database.mealDao()) }
    val healthConnectManager = remember { HealthConnectManager(appContext) }
    val weightRepository = remember { WeightRepository(appContext) }
    val userProfileRepository = remember { UserProfileRepository(appContext) }
    val permissionPrefs = remember {
        appContext.getSharedPreferences(HEALTH_PERMISSION_PREFS, Context.MODE_PRIVATE)
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(HOME_TAB) }
    var showSplash by remember { mutableStateOf(true) }
    var pendingPermissionRequest by remember { mutableStateOf(emptySet<String>()) }
    var profileCreated by remember { mutableStateOf(userProfileRepository.hasProfile()) }
    var showProfileWelcome by remember { mutableStateOf(false) }
    var showProfileCreation by remember { mutableStateOf(false) }
    val mealDraft: MealDraftViewModel = viewModel()
    var landingTutorialStep by remember { mutableStateOf<LandingTutorialStep?>(null) }
    var tutorialAddStage by remember { mutableIntStateOf(0) }
    var tutorialHistoryMealVisible by remember { mutableStateOf(true) }
    var tutorialHistoryExpanded by remember { mutableStateOf(false) }
    var tutorialChatStage by remember { mutableIntStateOf(0) }
    var showTutorialConfetti by remember { mutableStateOf(false) }
    var tutorialCoachReadyForConfirm by remember { mutableStateOf(false) }
    var chatPromptCopyStreak by remember { mutableIntStateOf(0) }
    var showDeveloperControls by remember { mutableStateOf(false) }
    var chatPromptCopied by remember {
        mutableStateOf(permissionPrefs.getBoolean(KEY_CHAT_PROMPT_COPIED, false))
    }
    val tutorialActive = landingTutorialStep != null
    val developerControlsVisible = BuildConfig.DEBUG && showDeveloperControls

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val hasPermission = granted.containsAll(pendingPermissionRequest)
        Toast.makeText(
            context,
            if (hasPermission) "Health Connect permission granted" else "Health Connect permissions can be changed in Android settings",
            Toast.LENGTH_LONG
        ).show()

    }

    fun requestHealthPermissionOnce(
        prefKey: String,
        permissions: Set<String> = healthConnectManager.getRequiredPermissions(),
        allowRepeatRequest: Boolean = false
    ) {
        if (!healthConnectManager.isSdkAvailable()) {
            Toast.makeText(context, "Health Connect is not available on this device", Toast.LENGTH_LONG).show()
            return
        }

        if (!allowRepeatRequest && permissionPrefs.getBoolean(prefKey, false)) {
            Toast.makeText(
                context,
                "Health Connect permission is still required. You can grant it from Android settings",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        permissionPrefs.edit { putBoolean(prefKey, true) }
        pendingPermissionRequest = permissions
        permissionLauncher.launch(permissions)
    }

    fun resetProfileCreationForDeveloper() {
        userProfileRepository.clearProfile()
        profileCreated = false
        landingTutorialStep = null
        tutorialHistoryMealVisible = true
        tutorialHistoryExpanded = false
        tutorialAddStage = 0
        tutorialCoachReadyForConfirm = false
        selectedTab = HOME_TAB
        showProfileWelcome = true
        Toast.makeText(
            context,
            "Profile creation reset. Meal history was not changed",
            Toast.LENGTH_LONG
        ).show()
    }

    fun resetTutorialForDeveloper() {
        permissionPrefs.edit {
            putBoolean(KEY_LANDING_TUTORIAL_COMPLETED, false)
            putBoolean(KEY_CHAT_PROMPT_COPIED, false)
        }
        chatPromptCopied = false
        showProfileWelcome = false
        tutorialHistoryMealVisible = true
        tutorialHistoryExpanded = false
        tutorialAddStage = 0
        tutorialChatStage = 0
        tutorialCoachReadyForConfirm = false
        selectedTab = HOME_TAB
        landingTutorialStep = LandingTutorialStep.HOME_INTRO
        Toast.makeText(
            context,
            "Tutorial reset. Meal history was not changed",
            Toast.LENGTH_LONG
        ).show()
    }

    LaunchedEffect(Unit) {
        delay(1700)
        showSplash = false
    }

    if (showSplash) {
        IzunLaunchScreen(modifier = modifier)
        return
    }

    LaunchedEffect(Unit) {
        if (!profileCreated) {
            showProfileWelcome = true
        }
    }

    if (showProfileWelcome) {
        ProfileWelcomeScreen(
            onContinue = {
                showProfileWelcome = false
                showProfileCreation = true
            },
            modifier = modifier
        )
        return
    }

    if (showProfileCreation) {
        ProfileCreationScreen(
            onProfileCreated = { profile ->
                userProfileRepository.saveProfile(profile)
                profileCreated = true
                showProfileCreation = false
                selectedTab = HOME_TAB
                if (!permissionPrefs.getBoolean(KEY_LANDING_TUTORIAL_COMPLETED, false)) {
                    landingTutorialStep = LandingTutorialStep.HOME_INTRO
                    tutorialHistoryMealVisible = true
                    tutorialHistoryExpanded = false
                    tutorialAddStage = 0
                    tutorialCoachReadyForConfirm = false
                }
                Toast.makeText(context, "Profile created", Toast.LENGTH_LONG).show()
            },
            modifier = modifier
        )
        return
    }

    LaunchedEffect(landingTutorialStep) {
        if (landingTutorialStep == LandingTutorialStep.ADD_DEMO) {
            tutorialAddStage = 0
            tutorialCoachReadyForConfirm = false
            delay(550)
            tutorialAddStage = 1
            delay(650)
            tutorialAddStage = 2
            delay(650)
            tutorialAddStage = 3
            delay(1_500)
            tutorialCoachReadyForConfirm = true
        }
        if (landingTutorialStep == LandingTutorialStep.HISTORY_DELETE) {
            tutorialHistoryExpanded = false
            delay(700)
            tutorialHistoryExpanded = true
        }
        if (landingTutorialStep == LandingTutorialStep.CHATBOT_OUTPUT) {
            tutorialChatStage = 0
        }
        if (landingTutorialStep == LandingTutorialStep.RAMEN_ADD) {
            tutorialAddStage = 0
            tutorialCoachReadyForConfirm = false
            delay(450)
            tutorialAddStage = 1
            delay(550)
            tutorialAddStage = 2
            delay(550)
            tutorialAddStage = 3
            delay(1_500)
            tutorialCoachReadyForConfirm = true
        }
    }

    fun advanceTutorial() {
        when (landingTutorialStep) {
            LandingTutorialStep.HOME_INTRO -> {
                selectedTab = MEAL_TAB
                landingTutorialStep = LandingTutorialStep.MEAL_INTRO
            }
            LandingTutorialStep.MEAL_INTRO -> {
                selectedTab = MEAL_TAB
                landingTutorialStep = LandingTutorialStep.ADD_DEMO
            }
            LandingTutorialStep.ADD_DEMO -> Unit
            LandingTutorialStep.HISTORY_DELETE -> Unit
            LandingTutorialStep.SETTINGS_CUSTOMIZE -> {
                selectedTab = SETTINGS_TAB
                landingTutorialStep = LandingTutorialStep.SETTINGS_PROMPT
            }
            LandingTutorialStep.SETTINGS_PROMPT -> Unit
            LandingTutorialStep.CHATBOT_OUTPUT -> Unit
            LandingTutorialStep.CHATBOT_ADD_PROMPT -> Unit
            LandingTutorialStep.RAMEN_ADD -> Unit
            LandingTutorialStep.FINISH_HOME -> {
                permissionPrefs.edit { putBoolean(KEY_LANDING_TUTORIAL_COMPLETED, true) }
                landingTutorialStep = null
            }
            null -> Unit
        }
    }

    val demoHomeData = remember(landingTutorialStep) {
        if (tutorialActive) tutorialHomeData(finalView = landingTutorialStep == LandingTutorialStep.FINISH_HOME) else null
    }
    val mealTutorialState = when (landingTutorialStep) {
        LandingTutorialStep.MEAL_INTRO,
        LandingTutorialStep.ADD_DEMO -> MealTutorialState(
            mode = MealTutorialMode.ADD,
            addStage = tutorialAddStage
        )
        LandingTutorialStep.CHATBOT_ADD_PROMPT -> MealTutorialState(
            mode = null,
            awaitingAddMode = true
        )
        LandingTutorialStep.RAMEN_ADD -> MealTutorialState(
            mode = MealTutorialMode.ADD,
            addStage = tutorialAddStage,
            mealName = "Waffle breakfast plate",
            caloriesText = "540 kcal",
            timeText = "Today, 10:10",
            proteinText = "21 g",
            carbsText = "58 g",
            fatText = "24 g",
            fiberText = "3 g",
            sodiumText = "720 mg",
            sugarText = "15 g"
        )
        LandingTutorialStep.HISTORY_DELETE -> MealTutorialState(
            mode = MealTutorialMode.HISTORY,
            historyExpanded = tutorialHistoryMealVisible && tutorialHistoryExpanded
        )
        else -> null
    }
    val tutorialContentClearance = if (tutorialActive) 260.dp else 20.dp

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == MEAL_TAB,
                    onClick = { selectedTab = MEAL_TAB },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = "Meal") },
                    label = { Text("Meal") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == HOME_TAB,
                    onClick = { selectedTab = HOME_TAB },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == SETTINGS_TAB,
                    onClick = { selectedTab = SETTINGS_TAB },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                HOME_TAB -> HomeScreen(
                    mealRepository = mealRepository,
                    healthConnectManager = healthConnectManager,
                    weightRepository = weightRepository,
                    userProfileRepository = userProfileRepository,
                    onPermissionNeeded = {
                        requestHealthPermissionOnce(
                            KEY_DASHBOARD_PERMISSION_REQUESTED,
                            healthConnectManager.getDashboardReadPermissions(),
                            allowRepeatRequest = true
                        )
                    },
                    demoData = demoHomeData,
                    bottomContentPadding = tutorialContentClearance
                )
                MEAL_TAB -> MealScreen(
                    mealRepository = mealRepository,
                    healthConnectManager = healthConnectManager,
                    addMealDraft = mealDraft.draft,
                    onAddMealDraftChanged = { mealDraft.draft = it },
                    onHistoryPermissionNeeded = {
                        requestHealthPermissionOnce(KEY_EDIT_PERMISSION_REQUESTED)
                    },
                    tutorialState = mealTutorialState,
                    tutorialBottomPadding = tutorialContentClearance,
                    onTutorialAddModeSelected = {
                        if (landingTutorialStep == LandingTutorialStep.CHATBOT_ADD_PROMPT) {
                            landingTutorialStep = LandingTutorialStep.RAMEN_ADD
                        }
                    },
                    onTutorialAddConfirmed = {
                        if (landingTutorialStep == LandingTutorialStep.ADD_DEMO) {
                            selectedTab = MEAL_TAB
                            landingTutorialStep = LandingTutorialStep.HISTORY_DELETE
                        } else if (landingTutorialStep == LandingTutorialStep.RAMEN_ADD) {
                            selectedTab = HOME_TAB
                            landingTutorialStep = LandingTutorialStep.FINISH_HOME
                            showTutorialConfetti = true
                        }
                    },
                    onTutorialMealDeleted = {
                        tutorialHistoryMealVisible = false
                        selectedTab = SETTINGS_TAB
                        landingTutorialStep = LandingTutorialStep.SETTINGS_CUSTOMIZE
                    }
                )
                SETTINGS_TAB -> SettingsScreen(
                    chatPromptCopied = chatPromptCopied,
                    showDeveloperControls = developerControlsVisible,
                    onConnectNutrition = {
                        requestHealthPermissionOnce(KEY_NUTRITION_PERMISSION_REQUESTED, allowRepeatRequest = true)
                    },
                    onChatPromptCopied = {
                        permissionPrefs.edit { putBoolean(KEY_CHAT_PROMPT_COPIED, true) }
                        chatPromptCopied = true
                        if (BuildConfig.DEBUG) {
                            chatPromptCopyStreak += 1
                            if (chatPromptCopyStreak >= 5) {
                                showDeveloperControls = true
                            }
                        }
                        if (landingTutorialStep == LandingTutorialStep.SETTINGS_PROMPT) {
                            landingTutorialStep = LandingTutorialStep.CHATBOT_OUTPUT
                        }
                    },
                    onResetProfileCreation = { resetProfileCreationForDeveloper() },
                    onResetTutorial = { resetTutorialForDeveloper() },
                    bottomContentPadding = tutorialContentClearance
                )
            }

            landingTutorialStep?.let { step ->
                if (step == LandingTutorialStep.CHATBOT_OUTPUT) {
                    ChatbotExampleCard(
                        stage = tutorialChatStage,
                        onPasteImage = { tutorialChatStage = 1 },
                        onCopyReport = {
                            selectedTab = MEAL_TAB
                            landingTutorialStep = LandingTutorialStep.CHATBOT_ADD_PROMPT
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    TutorialCoachCard(
                        step = step,
                        addStage = if (tutorialCoachReadyForConfirm) 3 else 0,
                        onAdvance = { advanceTutorial() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            if (showTutorialConfetti) {
                TutorialConfettiOverlay(
                    onFinished = { showTutorialConfetti = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private const val MEAL_TAB = 0
private const val HOME_TAB = 1
private const val SETTINGS_TAB = 2

@Composable
private fun TutorialCoachCard(
    step: LandingTutorialStep,
    addStage: Int,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, message, buttonText, showButton) = when (step) {
        LandingTutorialStep.HOME_INTRO -> TutorialCopy(
            "Home shows your balance",
            "Your Home page displays your goals, meal tracking, steps, and steady weight measurements",
            "Got it",
            true
        )
        LandingTutorialStep.MEAL_INTRO -> TutorialCopy(
            "Meal is where logging starts",
            "Use \"Add\" for a new meal and \"History\" to manage meals you already logged. We will run a simulated example now",
            "Show me",
            true
        )
        LandingTutorialStep.ADD_DEMO -> TutorialCopy(
            "This is a meal card",
            if (addStage < 3) {
                "A meal card contains a name, calories intake, date and time and nutritional values"
            } else {
                "The card is ready. Tap Confirm to continue the tutorial"
            },
            "",
            false
        )
        LandingTutorialStep.HISTORY_DELETE -> TutorialCopy(
            "History of your meals",
            "Meals can be re-added, edited, or deleted. Delete this example meal to continue",
            "",
            false
        )
        LandingTutorialStep.SETTINGS_CUSTOMIZE -> TutorialCopy(
            "Let's set(tings) the vibe",
            "This is where you can change the app theme and copy the \"Chat Prompt\"",
            "Next",
            true
        )
        LandingTutorialStep.SETTINGS_PROMPT -> TutorialCopy(
            "Copy the Chat Prompt",
            "Paste it into your chatbot(GPT/Gemini). After that, meal photos, descriptions, or nutrition labels can come back as IZUN-ready meal reports",
            "",
            false
        )
        LandingTutorialStep.CHATBOT_OUTPUT -> TutorialCopy(
            "This is how a typical chat response will look like",
            "Lets copy this IZUN Report!",
            "",
            false
        )
        LandingTutorialStep.CHATBOT_ADD_PROMPT -> TutorialCopy(
            "Now we can head back to the \"Meal\" tab",
            "Choose Add to continue this demo. For a real meal, tap Paste on the meal card to import a copied report",
            "",
            false
        )
        LandingTutorialStep.RAMEN_ADD -> TutorialCopy(
            "Back in Meal",
            if (addStage < 3) {
                "This is much easier than manually writing everything!"
            } else {
                "Confirm this breakfast meal"
            },
            "",
            false
        )
        LandingTutorialStep.FINISH_HOME -> TutorialCopy(
            "Back to Home",
            "See how the daily calorie target was reached? Don't worry these meals won't be registered",
            "Finish",
            true
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE6F6EA).copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            if (showButton) {
                Button(
                    onClick = onAdvance,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun ChatbotExampleCard(
    stage: Int,
    onPasteImage: () -> Unit,
    onCopyReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Try it with your chatbot",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                textAlign = TextAlign.Center
            )
            if (stage == 0) {
                Text(
                    text = "After you paste the Chat Prompt, you can share a photo with your chatbot. For example, this delicious breakfast plate",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
                Image(
                    painter = painterResource(id = R.drawable.tutorial_breakfast_demo),
                    contentDescription = "Breakfast plate example",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Button(
                    onClick = onPasteImage,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Paste Image")
                }
            } else {
                Text(
                    text = "This will be the chatbot response. The part you copy back into IZUN is the IZUN Report",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
                IzunReportPreview()
                Button(
                    onClick = onCopyReport,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy IZUN Report")
                }
            }
        }
    }
}

@Composable
private fun IzunReportPreview() {
    val reportText = """
IZUN Report
{
  "name": "Waffle breakfast plate",
  "meal_type": "breakfast",
  "start_time": "2026-06-24T10:10:00",
  "end_time": "2026-06-24T10:25:00",
  "energy_kcal": 540,
  "nutrients": {
    "protein_g": 21,
    "total_carbohydrate_g": 58,
    "total_fat_g": 24,
    "dietary_fiber_g": 3,
    "sugar_g": 15,
    "sodium_mg": 720
  }
}
""".trimIndent()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = reportText,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            lineHeight = 10.sp,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(12.dp)
        )
    }
}

private data class TutorialCopy(
    val title: String,
    val message: String,
    val buttonText: String,
    val showButton: Boolean
)

@Composable
private fun TutorialConfettiOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 2_400, easing = LinearEasing),
        label = "tutorialConfettiProgress"
    )
    val particles = remember {
        val colors = listOf(
            Color(0xFF22C55E),
            Color(0xFFF59E0B),
            Color(0xFF38BDF8),
            Color(0xFFEF4444),
            Color(0xFFA855F7)
        )
        List(34) { index ->
            ConfettiParticle(
                startX = ((index * 37) % 100) / 100f,
                drift = (((index * 19) % 41) - 20) / 100f,
                delay = ((index * 53) % 500) / 2400f,
                size = 7f + (index % 4) * 3f,
                rotation = (index * 29f) % 180f,
                color = colors[index % colors.size]
            )
        }
    }

    LaunchedEffect(Unit) {
        visible = false
        kotlinx.coroutines.delay(2_450)
        onFinished()
    }

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val localProgress = ((1f - progress) - particle.delay).coerceIn(0f, 1f)
            if (localProgress > 0f && localProgress < 1f) {
                val alpha = if (localProgress < 0.92f) {
                    1f
                } else {
                    ((1f - localProgress) / 0.08f).coerceIn(0f, 1f)
                }
                val x = (particle.startX + particle.drift * localProgress) * size.width
                val y = -32f + localProgress * (size.height + 96f)
                rotate(
                    degrees = particle.rotation + localProgress * 360f,
                    pivot = Offset(x, y)
                ) {
                    drawRoundRect(
                        color = particle.color.copy(alpha = alpha),
                        topLeft = Offset(x - particle.size / 2f, y - particle.size / 2f),
                        size = Size(particle.size * 1.5f, particle.size)
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val startX: Float,
    val drift: Float,
    val delay: Float,
    val size: Float,
    val rotation: Float,
    val color: Color
)

private fun tutorialHomeData(finalView: Boolean): HomeDemoData {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    fun at(hour: Int, minute: Int): Long = today.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
    fun daysAgo(days: Long, weight: Double): WeightSample {
        return WeightSample(
            timestamp = today.minusDays(days).atTime(7, 10).atZone(zoneId).toInstant().toEpochMilli(),
            weightKg = weight
        )
    }

    val meals = if (finalView) {
        listOf(
            demoMeal("Greek yogurt bowl", "breakfast", at(8, 15), 410.0, 28.0, 44.0, 10.0, 7.0),
            demoMeal("Waffle breakfast plate", "breakfast", at(10, 10), 540.0, 21.0, 58.0, 24.0, 3.0),
            demoMeal("Chicken rice plate", "dinner", at(18, 45), 750.0, 48.0, 78.0, 22.0, 8.0)
        )
    } else {
        listOf(
            demoMeal("Oat yogurt bowl", "breakfast", at(8, 15), 430.0, 26.0, 52.0, 11.0, 8.0),
            demoMeal("Chicken rice plate", "dinner", at(18, 45), 820.0, 52.0, 86.0, 24.0, 8.0),
            demoMeal("Apple and almonds", "snack", at(16, 20), 220.0, 6.0, 24.0, 13.0, 5.0)
        )
    }

    return HomeDemoData(
        meals = meals,
        healthSummary = DailyHealthSummary(
            steps = 10_450,
            distanceKm = 7.3,
            activeMinutes = 84,
            walkingCaloriesKcal = 292.0,
            hasReadPermission = true
        ),
        weeklySteps = (6 downTo 0).mapIndexed { index, daysAgo ->
            DailyStepTotal(today.minusDays(daysAgo.toLong()), listOf(8200L, 9400L, 10150L, 7600L, 11200L, 9800L, 10450L)[index])
        },
        weightSamples = listOf(
            daysAgo(28, 80.8),
            daysAgo(21, 80.4),
            daysAgo(14, 80.0),
            daysAgo(7, 79.7),
            daysAgo(0, 79.5)
        ),
        calorieRange = CalorieRange(low = 1600, mid = 1700, high = 1800)
    )
}

private fun demoMeal(
    name: String,
    mealType: String,
    timestamp: Long,
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    fiber: Double
): MealRecord {
    return MealRecord(
        id = timestamp.hashCode(),
        name = name,
        mealType = mealType,
        timestamp = timestamp,
        endTimestamp = timestamp + 1_800_000L,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        fiber = fiber,
        sugar = 7.0,
        sodium = 640.0,
        rawJson = "{}"
    )
}

@Composable
private fun ProfileWelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome to IZUN",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Before using IZUN, let's create your profile and set your personal goals. These details stay on your device and help IZUN personalize your calorie and nutrition targets",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Profile")
                }
            }
        }
    }
}

@Composable
private fun IzunLaunchScreen(modifier: Modifier = Modifier) {
    var textVisible by remember { mutableStateOf(false) }
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "izunTextAlpha"
    )
    val transition = rememberInfiniteTransition(label = "scaleMotion")
    val scaleTilt by transition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleTilt"
    )

    LaunchedEffect(Unit) {
        textVisible = true
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "IZUN",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-128).dp)
                .graphicsLayer(alpha = textAlpha)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(116.dp)
                .clip(CircleShape)
                .graphicsLayer(rotationZ = scaleTilt)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "IZUN balance scales",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
