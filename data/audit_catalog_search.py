"""Count preliminary textual matches; inspect results manually before acceptance."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parent.parent
assets = root / "android/app/src/main/assets"
source = (root / "android/app/src/main/java/com/niamniam/kbjy/data/StarterCatalog.kt").read_text(encoding="utf-8")
aliases = {int(key): value for key, value in re.findall(r'(\d+) to "([^"]+)"', source)}
usda = json.loads((assets / "fndds_2021_2023.json").read_text(encoding="utf-8"))["foods"]
packaged = json.loads((assets / "off_regional_sample.json").read_text(encoding="utf-8"))["products"]

def normalize(value):
    return value.lower().replace("ё", "е").strip()

names = [normalize(aliases.get(item["fdcId"], "") + " " + item["nameEn"]) for item in usda]
names.extend(normalize(item["name"] + " " + item.get("brand", "")) for item in packaged)
text = (root / "data/catalog-acceptance/queries-ru.md").read_text(encoding="utf-8")
for label, section in zip(("Продукты", "Блюда"), text.split("## Продукты", 1)[1].split("## ")[:2]):
    queries = re.findall(r"^\d+\. (.+)$", section, flags=re.MULTILINE)
    matches = sum(any(all(word in name for word in normalize(query).split()) for name in names) for query in queries)
    print(f"{label}: {matches}/{len(queries)} текстовых совпадений; соответствие продуктов не проверено")
