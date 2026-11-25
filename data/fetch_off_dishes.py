"""Fetch a checkpointed OFF search sample, respecting its search rate limit."""
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

root = Path(__file__).resolve().parent.parent
document = (root / "data/catalog-acceptance/queries-ru.md").read_text(encoding="utf-8")
section = document.split("## Блюда", 1)[1]
queries = re.findall(r"^\d+\. (.+)$", section, re.MULTILINE)
target = root / "data/off-dish-searches.json"
results = json.loads(target.read_text(encoding="utf-8")) if target.exists() else {}
failures = 0
for query in queries:
    if query in results:
        continue
    parameters = urllib.parse.urlencode({
        "search_terms": query,
        "search_simple": 1,
        "action": "process",
        "json": 1,
        "page_size": 20,
        "fields": "code,product_name,product_name_ru,brands,nutriments,nutrition_data_per",
    })
    request = urllib.request.Request(
        "https://world.openfoodfacts.org/cgi/search.pl?" + parameters,
        headers={"User-Agent": "NiamNiamKBJY/0.5 (personal offline food diary)"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            payload = json.load(response)
        products = payload.get("products")
        if not isinstance(products, list):
            raise ValueError("Missing products")
        results[query] = products
        temporary = target.with_suffix(".tmp")
        temporary.write_text(json.dumps(results, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        os.replace(temporary, target)
        failures = 0
        print(f"{len(results)}/{len(queries)}: {query} — {len(products)} результатов", flush=True)
    except Exception as error:
        failures += 1
        print(f"Ошибка запроса {query}: {type(error).__name__}: {error}", file=sys.stderr, flush=True)
        if failures >= 3:
            raise SystemExit("Три ошибки подряд, поиск приостановлен с сохранением результатов.")
    time.sleep(8)
