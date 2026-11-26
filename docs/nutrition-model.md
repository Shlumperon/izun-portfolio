# Nutrition estimates in IZUN

IZUN calculates an estimated energy range and nutrient targets on the device. The app presents estimates for general tracking. They have no clinical validation.

## Inputs and outputs

- `CalorieEstimationModel` estimates resting energy expenditure with the Mifflin–St Jeor equation. It uses age, height, weight, and the selected formula variant.
- The model estimates activity from available step data or a manually selected step range. Goal, pace, and recent weight change inform the target range and associated flags.
- `NutritionTargetModel` calculates macronutrient targets and guidance for tracked nutrients from the energy estimate and profile inputs.
- The models flag cases where standard goals are limited, including age under 18 and the health considerations entered during profile creation. The unit tests cover these cases and the supported goal calculations.

The implementation lives in [`nutrition/`](../app/src/main/java/com/example/caloriescounter/nutrition). Formula behavior and edge cases are covered in the [nutrition unit tests](../app/src/test/java/com/example/caloriescounter/nutrition).
