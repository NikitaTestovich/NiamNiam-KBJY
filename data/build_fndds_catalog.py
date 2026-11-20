"""Build the offline FNDDS catalog from the official USDA JSON archive."""

import argparse
import hashlib
import json
import math
from collections import Counter
from pathlib import Path
from zipfile import ZipFile


NUTRIENTS = {
    1008: ("calories", "kcal"),
    1003: ("protein", "g"),
    1004: ("fat", "g"),
    1005: ("carbohydrate", "g"),
}
SOURCE = "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_survey_food_json_2024-10-31.zip"


def convert(archive_path):
    archive_hash = hashlib.sha256(archive_path.read_bytes()).hexdigest()
    with ZipFile(archive_path) as archive:
        source = json.load(archive.open("surveyDownload.json"))

    result = []
    skipped = Counter()
    ids = set()
    for food in source["SurveyFoods"]:
        fdc_id = food.get("fdcId")
        name = food.get("description", "").strip()
        code = food.get("foodCode")
        if not isinstance(fdc_id, int) or fdc_id in ids or not code or not name or "\ufffd" in name:
            skipped["invalid_identity"] += 1
            continue
        amounts = {}
        valid = True
        for row in food.get("foodNutrients", []):
            nutrient = row.get("nutrient", {})
            definition = NUTRIENTS.get(nutrient.get("id"))
            if definition is None:
                continue
            field, unit = definition
            value = row.get("amount")
            if (
                field in amounts
                or nutrient.get("unitName", "").lower() != unit
                or isinstance(value, bool)
                or not isinstance(value, (int, float))
                or not math.isfinite(value)
                or value < 0
            ):
                valid = False
                break
            amounts[field] = value
        if not valid or len(amounts) != len(NUTRIENTS):
            skipped["incomplete_or_invalid_nutrients"] += 1
            continue
        ids.add(fdc_id)
        result.append({"fdcId": fdc_id, "foodCode": str(code), "nameEn": name, **amounts})
    result.sort(key=lambda item: item["fdcId"])
    return {
        "source": SOURCE,
        "sourceName": "USDA FoodData Central, FNDDS 2021-2023",
        "release": "2024-10-31",
        "license": "CC0-1.0",
        "basis": "per 100 g edible portion",
        "archiveSha256": archive_hash,
        "foods": result,
    }, skipped, len(source["SurveyFoods"])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    catalog, skipped, total = convert(args.archive)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(catalog, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"Input: {total}; exported: {len(catalog['foods'])}; skipped: {dict(skipped)}")
    print(f"Archive SHA-256: {catalog['archiveSha256']}")


if __name__ == "__main__":
    main()
