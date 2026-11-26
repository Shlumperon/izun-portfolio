<p align="center">
  <img src="docs/izun-banner.png" width="850" alt="IZUN · Android meal and nutrition tracker">
</p>

# IZUN

IZUN is an Android meal and nutrition tracker that works offline. Log a meal by hand or paste a structured **IZUN Report** into an editable meal card. The app keeps your meal history on the device and can connect to Android Health Connect for activity data and nutrition records.

## What you can do

| Feature | What it does |
| --- | --- |
| **Meal logging** | Enter calories and nutrients manually, or paste JSON and review the values before saving. Edit, repeat, or delete meals in History. |
| **Daily dashboard** | See calorie and nutrient totals, activity summaries, and weight trends. Calculate estimated targets from your profile and available activity data. |
| **Optional Health Connect** | Read steps, distance, exercise, and active calories with permission. Grant nutrition write access separately in Settings. |

## Try a meal import

1. Build and open IZUN, then complete the profile and the guided demo.
2. Copy the example JSON below, open **Meal → Add**, and tap **Paste**.
3. Review the fields, adjust the date to today if you want it on the daily dashboard, and tap **Confirm**. The saved meal appears in **History**.

<details>
<summary>Sample IZUN Report · fictional meal</summary>

```json
{
  "name": "Berry oatmeal",
  "meal_type": "breakfast",
  "start_time": "2026-01-15T08:00:00",
  "end_time": "2026-01-15T08:15:00",
  "energy_kcal": 380,
  "nutrients": {
    "protein_g": 14,
    "total_carbohydrate_g": 56,
    "total_fat_g": 12,
    "dietary_fiber_g": 8
  }
}
```

</details>

**Settings → Copy Chat Prompt** offers another way to prepare a report using a chatbot you open yourself. Review any estimates before saving them.

## Build and run

Use Android Studio with support for Android Gradle Plugin 9.3, JDK 17, Android SDK Platform 36, and Android SDK Build Tools 36.0.0. Open the repository root in Android Studio and run the `app` configuration on a device or emulator running Android 8.0 (API 26) or later. Health Connect requires a [supported device and setup](https://developer.android.com/health-and-fitness/health-connect/availability).

```bash
git clone https://github.com/Shlumperon/izun-portfolio.git
cd izun-portfolio
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows, use `gradlew.bat` in Command Prompt or `.\gradlew.bat` in PowerShell. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Run device tests with `:app:connectedDebugAndroidTest` while a device or emulator is connected.

## How it works

- **UI** — Kotlin, Jetpack Compose, Material 3, and Navigation 3.
- **Storage** — Room for meal history and Android app-private preferences for profile, weight, and settings. Room schema exports support migration tests.
- **Import and sync** — `FoodJsonParser` validates supported JSON fields. Meals are written locally before the optional Health Connect write. Stable client record IDs and versions support updates to existing synced meals.
- **Nutrition estimates** — The [model notes](docs/nutrition-model.md) describe the profile inputs, calorie range, nutrient targets, and guardrails.

## Privacy

IZUN runs without an account, API keys, analytics, or an IZUN server. It reads clipboard text when you tap **Paste** and saves the recognized meal fields in app-private storage. Android backup and device transfer exclude the meal database and preferences.

Health Connect permissions are optional. You can revoke them in Android settings. Clear IZUN's storage in Android settings to remove its local data. Manage any exported nutrition records separately in Health Connect.

Anything you send to an external chatbot is handled by that service. You can enter meals by hand or use the sample report without sending anything to a chatbot. The tutorial uses example data and a generated breakfast image.
