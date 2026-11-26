package com.example.caloriescounter.data

import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

object FoodJsonParser {
    private const val MAX_REPORT_LENGTH = 32_768
    private val allowedMealTypes = setOf("breakfast", "lunch", "dinner", "snack")

    fun parse(jsonString: String): MealRecord {
        require(jsonString.length <= MAX_REPORT_LENGTH) { "Paste a shorter IZUN Report." }
        val root = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Paste a single valid JSON object.", e)
        }

        val mealName = root.requiredString("name", "meal_name", "food_name")
        val mealType = root.optionalString("meal_type", defaultValue = "snack").lowercase(Locale.ROOT)
        require(mealType in allowedMealTypes) {
            "meal_type must be one of breakfast, lunch, dinner, or snack."
        }

        val calories = root.requiredPositiveDouble("energy_kcal", "calories_kcal")
        val nutrientsObj = root.optJSONObject("nutrients") ?: JSONObject()
        val startTime = parseTimestamp(root.optionalString("start_time", "timestamp", defaultValue = ""))
        val endTime = parseOptionalTimestamp(root.optionalString("end_time", defaultValue = ""), startTime)

        require(endTime > startTime) {
            "end_time must be after start_time."
        }

        return MealRecord(
            name = mealName,
            mealType = mealType,
            timestamp = startTime,
            endTimestamp = endTime,
            calories = calories,
            protein = nutrientsObj.optionalDouble("protein_g"),
            carbs = nutrientsObj.optionalDouble("total_carbohydrate_g", "carbs_g", "carbohydrate_g"),
            fat = nutrientsObj.optionalDouble("total_fat_g", "fat_g"),
            fiber = nutrientsObj.optionalDouble("dietary_fiber_g", "fiber_g"),
            sugar = nutrientsObj.optionalDouble("sugar_g"),
            saturatedFat = nutrientsObj.optionalDouble("saturated_fat_g"),
            unsaturatedFat = nutrientsObj.optionalDouble("unsaturated_fat_g"),
            transFat = nutrientsObj.optionalDouble("trans_fat_g"),
            sodium = nutrientsObj.optionalDouble("sodium_mg"),
            potassium = nutrientsObj.optionalDouble("potassium_mg"),
            cholesterol = nutrientsObj.optionalDouble("cholesterol_mg"),
            calcium = nutrientsObj.optionalDouble("calcium_mg"),
            iron = nutrientsObj.optionalDouble("iron_mg"),
            magnesium = nutrientsObj.optionalDouble("magnesium_mg"),
            zinc = nutrientsObj.optionalDouble("zinc_mg"),
            vitaminA = nutrientsObj.optionalDouble("vitamin_a_mcg"),
            vitaminC = nutrientsObj.optionalDouble("vitamin_c_mg"),
            vitaminD = nutrientsObj.optionalDouble("vitamin_d_mcg"),
            vitaminE = nutrientsObj.optionalDouble("vitamin_e_mg"),
            caffeine = nutrientsObj.optionalDouble("caffeine_mg"),
            healthConnectClientRecordId = "calories-counter-${UUID.randomUUID()}"
        )
    }

    private fun JSONObject.requiredString(vararg keys: String): String {
        val value = optionalString(*keys, defaultValue = "")
        require(value.isNotBlank()) {
            "${keys.first()} is required and must not be blank."
        }
        return value
    }

    private fun JSONObject.optionalString(vararg keys: String, defaultValue: String): String {
        for (key in keys) {
            if (has(key) && !isNull(key)) {
                val value = optString(key).trim()
                if (value.isNotBlank()) return value
            }
        }
        return defaultValue
    }

    private fun JSONObject.requiredPositiveDouble(vararg keys: String): Double {
        val value = optionalDouble(*keys)
        require(value != null && value > 0.0) {
            "${keys.first()} is required and must be greater than 0."
        }
        return value
    }

    private fun JSONObject.optionalDouble(vararg keys: String): Double? {
        for (key in keys) {
            if (!has(key) || isNull(key)) continue

            val value = when (val raw = opt(key)) {
                is Number -> raw.toDouble()
                is String -> raw.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                else -> null
            }

            require(value == null || value >= 0.0) {
                "$key must be a positive number or null."
            }

            if (value != null && !value.isNaN() && !value.isInfinite()) {
                return value
            }
        }
        return null
    }

    private fun parseOptionalTimestamp(raw: String, startTime: Long): Long {
        if (raw.isBlank()) return startTime + 60_000
        return parseTimestamp(raw)
    }

    private fun parseTimestamp(raw: String): Long {
        if (raw.isBlank()) {
            return System.currentTimeMillis()
        }

        val trimmed = raw.trim()
        val cleaned = trimmed.lowercase(Locale.ROOT)

        if (cleaned.contains("today") || cleaned.contains("yesterday")) {
            val date = if (cleaned.contains("yesterday")) LocalDate.now().minusDays(1) else LocalDate.now()
            val time = extractTime(cleaned) ?: LocalTime.now()
            return LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        parseInstant(trimmed)?.let { return it }

        val localFormatters = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        )

        for (formatter in localFormatters) {
            runCatching {
                LocalDateTime.parse(trimmed, formatter)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()?.let { return it }
        }

        runCatching {
            LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()?.let { return it }

        throw IllegalArgumentException("start_time/end_time must be ISO-8601, yyyy-MM-dd HH:mm, today HH:mm, or yesterday HH:mm.")
    }

    private fun parseInstant(raw: String): Long? {
        runCatching { return Instant.parse(raw).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
        return null
    }

    private fun extractTime(input: String): LocalTime? {
        val timePattern = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(am|pm)?")
        val matcher = timePattern.matcher(input)

        if (!matcher.find()) return null

        var hour = matcher.group(1)?.toIntOrNull() ?: return null
        val minute = matcher.group(2)?.toIntOrNull() ?: return null
        val ampm = matcher.group(3)

        if (ampm != null) {
            if (ampm == "pm" && hour < 12) {
                hour += 12
            } else if (ampm == "am" && hour == 12) {
                hour = 0
            }
        }

        return runCatching { LocalTime.of(hour, minute) }.getOrNull()
    }
}
