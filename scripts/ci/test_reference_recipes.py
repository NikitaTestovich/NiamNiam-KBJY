from unittest import TestCase

from data.build_reference_recipes import build_recipe


class ReferenceRecipesTest(TestCase):
    def setUp(self):
        self.sources = {
            "fixture": {
                1: {"nameEn": "Bread", "calories": 250, "protein": 8, "fat": 4, "carbohydrate": 45},
                2: {"nameEn": "Butter", "calories": 740, "protein": 0, "fat": 82, "carbohydrate": 0},
            },
        }
        self.recipe = {
            "id": "sandwich",
            "ingredients": [
                {"source": "fixture", "id": 1, "grams": 40},
                {"source": "fixture", "id": 2, "grams": 10},
            ],
        }

    def test_normalizes_to_100_grams_and_retains_ingredient_sources(self):
        food = build_recipe(self.recipe, self.sources)
        self.assertEqual(food["assembledWeightGrams"], 50)
        self.assertEqual(food["calories"], 348)
        self.assertEqual(food["protein"], 6.4)
        self.assertEqual(food["fat"], 19.6)
        self.assertEqual(food["carbohydrate"], 36)
        self.assertEqual(food["ingredients"][1]["sourceNameEn"], "Butter")

    def test_rejects_non_positive_component_weight(self):
        self.recipe["ingredients"][0]["grams"] = 0
        with self.assertRaisesRegex(ValueError, "weight must be positive"):
            build_recipe(self.recipe, self.sources)

    def test_rejects_missing_nutrient_without_assigning_zero(self):
        del self.sources["fixture"][1]["protein"]
        with self.assertRaisesRegex(KeyError, "protein"):
            build_recipe(self.recipe, self.sources)

    def test_uses_published_cooked_yield_instead_of_ingredient_weight(self):
        self.recipe["cookedYield"] = {"grams": 40, "sourceUrl": "https://example.org/yield"}
        food = build_recipe(self.recipe, self.sources)
        self.assertEqual(food["cookedYield"]["grams"], 40)
        self.assertEqual(food["calories"], 435)
        self.assertEqual(food["protein"], 8)
        self.assertEqual(food["fat"], 24.5)
        self.assertEqual(food["carbohydrate"], 45)
        self.assertNotIn("assembledWeightGrams", food)

    def test_rejects_non_positive_cooked_yield(self):
        self.recipe["cookedYield"] = {"grams": 0, "sourceUrl": "https://example.org/yield"}
        with self.assertRaisesRegex(ValueError, "cooked yield must be positive"):
            build_recipe(self.recipe, self.sources)

    def test_requires_source_for_explicit_cooked_yield(self):
        self.recipe["cookedYield"] = {"grams": 40, "sourceUrl": ""}
        with self.assertRaisesRegex(ValueError, "cooked yield requires a source"):
            build_recipe(self.recipe, self.sources)
