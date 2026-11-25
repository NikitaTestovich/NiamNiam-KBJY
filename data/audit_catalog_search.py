"""Report literal query results for the bundled offline catalog."""

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "android/app/src/main/assets"
SOURCE = ROOT / "android/app/src/main/java/com/niamniam/kbjy/data/StarterCatalog.kt"
QUERIES = ROOT / "data/catalog-acceptance/queries-ru.md"


def normalize(value):
    return value.lower().replace("ё", "е").strip()


def matches(name, query):
    title = normalize(name)
    return all(re.search(r"(?<![^\W_])" + re.escape(word), title) for word in normalize(query).split())


def catalog_names():
    source = SOURCE.read_text(encoding="utf-8")
    maps = {}
    for key in ("russianNames", "preparedDishNames"):
        body = re.search(rf"private val {key} = mapOf\((.*?)\n    \)", source, re.S)
        if not body:
            raise ValueError(f"Catalog name map missing: {key}")
        maps[key] = {int(i): name for i, name in re.findall(r'(\d+) to "([^"]+)"', body[1])}
    russian, prepared = maps.values()
    usda = json.loads((ASSETS / "fndds_2021_2023.json").read_text(encoding="utf-8"))["foods"]
    packaged = json.loads((ASSETS / "off_regional_sample.json").read_text(encoding="utf-8"))["products"]
    branded = json.loads((ASSETS / "usda_branded_sample.json").read_text(encoding="utf-8"))["products"]
    packaged_by_code = {item["code"]: item for item in packaged}
    for item in branded:
        packaged_by_code.setdefault(item["code"], item)
    essentials = set(map(int, re.findall(r"\d+", re.search(r"private val essentials = setOf\(([^)]*)\)", source)[1])))
    priority = {**dict.fromkeys(prepared, 2), **dict.fromkeys(russian, 1), **dict.fromkeys(essentials, 0)}
    usda.sort(key=lambda item: priority.get(item["fdcId"], 3))
    starter_names = []
    for item in usda:
        identifier = item["fdcId"]
        alias = russian.get(identifier, prepared.get(identifier))
        starter_names.append(f'{alias} / {item["nameEn"]}' if alias else item["nameEn"])
    reference = json.loads((ASSETS / "usda_sr28_sample.json").read_text(encoding="utf-8"))["foods"]
    for item in reference:
        name = f'{item["nameRu"]} / {item["nameEn"]}'
        if item["ndbId"] == 2047:
            starter_names.insert(0, name)
        else:
            starter_names.append(name)
    packaged_names = [
        " / ".join(" · ".join(filter(None, (name, item.get("brand"))))
                   for name in (item["name"], item.get("nameEn")) if name)
        for item in packaged_by_code.values()
    ]
    swedish = json.loads((ASSETS / "swedish_food_agency.json").read_text(encoding="utf-8"))["foods"]
    swedish_names = [f'{item["nameRu"]} / {item["nameEn"]}' for item in swedish]
    bls = json.loads((ASSETS / "bls4_sample.json").read_text(encoding="utf-8"))["foods"]
    german_names = [f'{item["nameRu"]} / {item["nameEn"]}' for item in bls]
    cofid = json.loads((ASSETS / "cofid_2021_sample.json").read_text(encoding="utf-8"))["foods"]
    british_names = [f'{item["nameRu"]} / {item["nameEn"]}' for item in cofid]
    reference_recipes = json.loads((ASSETS / "reference_recipes.json").read_text(encoding="utf-8"))["foods"]
    recipe_names = [f'{item["nameRu"]} / {item["nameEn"]}' for item in reference_recipes]
    return {
        "Фасованные продукты Open Food Facts и USDA": packaged_names,
        "USDA": starter_names,
        "Swedish Food Agency": swedish_names,
        "BLS": german_names,
        "CoFID": british_names,
        "NiamNiam": recipe_names,
    }


def query_groups():
    text = QUERIES.read_text(encoding="utf-8")
    sections = re.split(r"^## .+$", text, flags=re.MULTILINE)[1:]
    groups = [re.findall(r"^\d+\. (.+)$", section, flags=re.MULTILINE) for section in sections]
    if [len(group) for group in groups] != [50, 100]:
        raise ValueError("Expected 50 product and 100 dish queries")
    return zip(("Продукты", "Блюда"), (45, 90), groups)


def build_report():
    catalogs = catalog_names()
    counts = ", ".join(f"{source} — {len(names)}" for source, names in catalogs.items())
    lines = [
        "# Офлайн-каталог. Контрольные запросы",
        "",
        f"Проверены встроенные данные APK. Число записей по источникам: {counts}.",
        "Проверка повторяет поиск по началу слова, обработку `ё` и лимит 100 результатов на источник.",
        "Персональная база пуста. Кандидаты требуют сверки с ожидаемым блюдом и его составом.",
        "Нулевой результат означает, что текущий запрос не находит карточку. Причиной может быть название или отсутствие данных.",
    ]
    for label, target, queries in query_groups():
        results = []
        for query in queries:
            found = []
            for names in catalogs.values():
                found.extend([name for name in names if matches(name, query)][:100])
            results.append((query, found))
        count = sum(bool(found) for _, found in results)
        lines += ["", f"## {label}", "",
                  f"Кандидаты найдены для **{count} из {len(queries)}** запросов. "
                  f"Запросов без результата — **{len(queries) - count}**. "
                  f"До порога даже при принятии всех кандидатов не хватает **{max(0, target - count)}**.",
                  "", "### Запросы без результата", ""]
        lines.extend(f"- {query}" for query, found in results if not found)
        lines += ["", "### Запросы с кандидатами", "", "| Запрос | Кандидатов | Первые три результата |",
                  "| --- | ---: | --- |"]
        for query, found in results:
            if found:
                sample = "<br>".join(name.replace("|", "\\|") for name in found[:3])
                lines.append(f"| {query} | {len(found)} | {sample} |")
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, help="Write Markdown instead of printing it")
    options = parser.parse_args()
    report = build_report()
    if options.output:
        options.output.write_text(report, encoding="utf-8")
    else:
        for line in report.splitlines():
            if line.startswith("## ") or line.startswith("Кандидаты найдены"):
                print(line)
