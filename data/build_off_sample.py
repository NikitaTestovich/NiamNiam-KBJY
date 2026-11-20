"""Build a small, attributable Open Food Facts sample from a saved API response."""
import json
import math
from pathlib import Path

sources = [Path(f"data/off-{country}.json") for country in ("belarus", "russia", "ukraine", "georgia")]
sources.extend(Path(f"data/off-{country}-2.json") for country in ("belarus", "russia", "ukraine"))
products = []
for source in sources:
    response = json.loads(source.read_text(encoding="utf-8"))
    products.extend(response["products"])
result = []
seen_codes = set()
for product in products:
    name = product.get("product_name_ru") or product.get("product_name")
    code = product.get("code")
    values = product.get("nutriments", {})
    numbers = [values.get(key) for key in ("energy-kcal_100g", "proteins_100g", "fat_100g", "carbohydrates_100g")]
    if not isinstance(name, str) or not name.strip() or not isinstance(code, str) or not code.isdigit():
        continue
    if code in seen_codes:
        continue
    if product.get("nutrition_data_per") != "100g":
        continue
    if not all(isinstance(value, (int, float)) and math.isfinite(value) and value >= 0 for value in numbers):
        continue
    calories, protein, fat, carbohydrate = numbers
    if calories > 900 or any(value > 100 for value in (protein, fat, carbohydrate)) or protein + fat + carbohydrate > 110:
        continue
    seen_codes.add(code)
    result.append({"code": code, "name": name.strip(), "brand": product.get("brands", ""),
                   "calories": calories, "protein": protein, "fat": fat, "carbohydrate": carbohydrate})

output = Path("android/app/src/main/assets/off_regional_sample.json")
output.write_text(json.dumps({"source": "Open Food Facts contributors", "license": "ODbL-1.0",
                              "products": result}, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
print(f"Included {len(result)} of {len(products)} products with complete plausible values")
