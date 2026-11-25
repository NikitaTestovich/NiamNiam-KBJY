"""Generate offline reference recipes from ingredient weights and cooked yields."""

import argparse
import json
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from urllib.parse import urlsplit


DATA = Path(__file__).resolve().parent
ASSETS = DATA.parent / "android/app/src/main/assets"
OUTPUT = ASSETS / "reference_recipes.json"
NUTRIENTS = ("calories", "protein", "fat", "carbohydrate")
SOURCES = {
    "USDA FNDDS 2021–2023": ("fndds_2021_2023.json", "fdcId"),
    "USDA SR28": ("usda_sr28_sample.json", "ndbId"),
    "BLS 4.0": ("bls4_sample.json", "id"),
}


def build_recipe(recipe, sources):
    weight = Decimal(0)
    totals = dict.fromkeys(NUTRIENTS, Decimal(0))
    ingredients = []
    for ingredient in recipe["ingredients"]:
        grams = Decimal(str(ingredient["grams"]))
        if not grams.is_finite() or grams <= 0:
            raise ValueError(f'{recipe["id"]}: ingredient weight must be positive')
        original = sources[ingredient["source"]][ingredient["id"]]
        weight += grams
        for nutrient in NUTRIENTS:
            value = Decimal(str(original[nutrient]))
            if not value.is_finite() or value < 0:
                raise ValueError(f'{recipe["id"]}: invalid {nutrient} for {ingredient["id"]}')
            totals[nutrient] += value * grams
        ingredients.append({**ingredient, "sourceNameEn": original["nameEn"]})
    if not weight:
        raise ValueError(f'{recipe["id"]}: add at least one ingredient')

    weight_metadata = {"assembledWeightGrams": float(weight)}
    if "cookedYield" in recipe:
        yield_data = recipe["cookedYield"]
        finished_weight = Decimal(str(yield_data["grams"]))
        if not finished_weight.is_finite() or finished_weight <= 0:
            raise ValueError(f'{recipe["id"]}: cooked yield must be positive')
        source = urlsplit(yield_data.get("sourceUrl", ""))
        if source.scheme != "https" or not source.hostname:
            raise ValueError(f'{recipe["id"]}: cooked yield requires a source')
        weight = finished_weight
        weight_metadata = {}

    return {
        **recipe,
        "prepared": True,
        **weight_metadata,
        "ingredients": ingredients,
        **{
            key: float((value / weight).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))
            for key, value in totals.items()
        },
    }


def build_catalog():
    sources = {}
    for source, (filename, id_field) in SOURCES.items():
        foods = json.loads((ASSETS / filename).read_text(encoding="utf-8"))["foods"]
        sources[source] = {food[id_field]: food for food in foods}

    recipes = json.loads((DATA / "reference-recipes.json").read_text(encoding="utf-8"))
    if len({recipe["id"] for recipe in recipes}) != len(recipes):
        raise ValueError("Duplicate reference recipe ID")
    return {
        "source": "NiamNiam reference recipes",
        "ingredientSources": {
            "USDA": {"license": "CC0-1.0", "url": "https://fdc.nal.usda.gov/"},
            "BLS 4.0": {
                "author": "Max Rubner-Institut",
                "license": "CC BY 4.0",
                "licenseUrl": "https://creativecommons.org/licenses/by/4.0/",
                "url": "https://blsdb.de/download",
            },
        },
        "basis": "100 g of the finished dish",
        "method": "Weighted ingredient values using either assembled weight or a published cooked yield.",
        "foods": [build_recipe(recipe, sources) for recipe in recipes],
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check the committed catalog without writing it")
    args = parser.parse_args()
    rendered = json.dumps(build_catalog(), ensure_ascii=False, indent=2) + "\n"
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != rendered:
            parser.exit(1, "Reference recipes are out of date. Run data/build_reference_recipes.py.\n")
        print("Reference recipes match their source data and finished weights.")
    else:
        OUTPUT.write_text(rendered, encoding="utf-8")
