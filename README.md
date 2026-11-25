# NiamNiam KBJY

NiamNiam KBJY 0.5 — личный офлайн-дневник питания для Android. В приложении можно считать калории и БЖУ, искать продукты и готовые блюда, менять вес порции и сохранять свои рецепты. Сканер читает штрихкоды, QR-коды и таблицы пищевой ценности. Дневник также хранит объём выпитых напитков и историю веса. Отчёт за неделю, месяц или всё время можно сохранить и отправить в формате XLSX.

Дневник и профиль хранятся на устройстве. Интернет нужен для проверки обновлений и отправки предложения в каталог, если для сборки настроен приёмник. В репозитории используются синтетические тестовые записи.

## Сборка

Откройте каталог `android/` в Android Studio или соберите проект из командной строки с JDK 21 и установленной Android SDK Platform 37. На Windows используйте `gradlew.bat`.

```bash
cd android
bash ./gradlew :app:assembleDebug :app:verifyUnitCoverageBaseline :app:lintDebug
```

## Документация и данные

- [Спецификация](docs/superpowers/specs/2026-11-20-calorie-diary-design.md) и [сценарии приёмки](docs/superpowers/specs/2026-11-20-calorie-diary-acceptance.md).
- [Проверки, покрытие и выпуск](docs/CI-CD.md), [онлайн-обновление](docs/updates.md) и [модерация предложений](catalog-intake/README.md).
- [Источники каталога](data/CATALOG-SOURCES.md) и [лицензии данных и сторонних компонентов](data/THIRD-PARTY-AUDIT.md).
- [Машиночитаемая выборка Open Food Facts](android/app/src/main/assets/off_regional_sample.json), [лицензия базы ODbL 1.0](android/app/src/main/assets/licenses/ODbL-1.0.txt) и [лицензия содержимого DbCL 1.0](android/app/src/main/assets/licenses/DbCL-1.0.txt).
