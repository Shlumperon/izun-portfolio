package com.example.caloriescounter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FoodJsonParserTest {
    @Test
    fun parse_healthConnectJson_mapsNutritionFields() {
        val meal = FoodJsonParser.parse(
            """
            {
              "name": "Chicken rice bowl",
              "meal_type": "lunch",
              "start_time": "2026-06-20T12:30:00",
              "end_time": "2026-06-20T12:45:00",
              "energy_kcal": 650,
              "unrecognized_note": "This field is outside the import format",
              "nutrients": {
                "protein_g": 42,
                "total_carbohydrate_g": 70,
                "total_fat_g": 18,
                "dietary_fiber_g": 6,
                "sugar_g": 8,
                "saturated_fat_g": 4,
                "trans_fat_g": 0,
                "sodium_mg": 900,
                "potassium_mg": 620,
                "cholesterol_mg": 85,
                "calcium_mg": 80,
                "iron_mg": 3.2,
                "vitamin_a_mcg": 120,
                "vitamin_c_mg": 18,
                "vitamin_d_mcg": 2,
                "caffeine_mg": null
              }
            }
            """.trimIndent()
        )

        assertEquals("Chicken rice bowl", meal.name)
        assertEquals("", meal.rawJson)
        assertEquals("lunch", meal.mealType)
        assertEquals(650.0, meal.calories, 0.0)
        assertEquals(42.0, meal.protein ?: 0.0, 0.0)
        assertEquals(70.0, meal.carbs ?: 0.0, 0.0)
        assertEquals(18.0, meal.fat ?: 0.0, 0.0)
        assertEquals(4.0, meal.saturatedFat ?: 0.0, 0.0)
        assertEquals(900.0, meal.sodium ?: 0.0, 0.0)
        assertEquals(120.0, meal.vitaminA ?: 0.0, 0.0)
        assertEquals(null, meal.caffeine)
        assert(meal.endTimestamp > meal.timestamp)
    }

    @Test
    fun parse_legacyJson_remainsCompatible() {
        val meal = FoodJsonParser.parse(
            """
            {
              "meal_name": "Yogurt",
              "meal_type": "snack",
              "timestamp": "2026-06-20 09:00",
              "calories_kcal": 180,
              "nutrients": {
                "carbs_g": 22,
                "fat_g": 4,
                "fiber_g": 1
              }
            }
            """.trimIndent()
        )

        assertEquals("Yogurt", meal.name)
        assertEquals("snack", meal.mealType)
        assertEquals(180.0, meal.calories, 0.0)
        assertEquals(22.0, meal.carbs ?: 0.0, 0.0)
        assertEquals(4.0, meal.fat ?: 0.0, 0.0)
    }

    @Test
    fun parse_missingEnergy_rejectsBeforeHealthConnectWrite() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            FoodJsonParser.parse(
                """
                {
                  "name": "Incomplete meal",
                  "meal_type": "dinner",
                  "start_time": "2026-06-20T18:00:00",
                  "nutrients": {}
                }
                """.trimIndent()
            )
        }

        assertNotNull(error.message)
    }

    @Test
    fun parse_negativeNutrient_rejects() {
        assertThrows(IllegalArgumentException::class.java) {
            FoodJsonParser.parse(
                """
                {
                  "name": "Impossible meal",
                  "meal_type": "breakfast",
                  "start_time": "2026-06-20T08:00:00",
                  "energy_kcal": 120,
                  "nutrients": {
                    "protein_g": -1
                  }
                }
                """.trimIndent()
            )
        }
    }
}
