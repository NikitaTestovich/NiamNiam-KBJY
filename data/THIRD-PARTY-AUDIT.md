# Сторонние компоненты и данные версии 0.5

Проверены источники данных и файлы уведомлений, подготовленные для APK. Этот документ фиксирует, какие файлы и версии сверены и какие части выпуска ещё требуют проверки.

## Данные каталога

| Источник | Что входит в APK | Условия и указание источника |
| --- | --- | --- |
| Open Food Facts | Отдельный `off_regional_sample.json` с выбранными и преобразованными карточками | Указаны Open Food Facts contributors и источник. База распространяется по ODbL 1.0, отдельное содержимое — по DbCL 1.0. Тексты обеих лицензий в APK. [Условия повторного использования](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/tutorials/license-be-on-the-legal-side/) · [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/) · [DbCL 1.0](https://opendatacommons.org/licenses/dbcl/1-0/) |
| USDA | FNDDS 2021–2023, выборка Global Branded Food Products Database и отдельная выборка SR28 | FoodData Central указывает CC0 1.0 и просит указывать источник. SR28 указан отдельно как выпуск USDA Agricultural Research Service. [FoodData Central](https://fdc.nal.usda.gov/api-guide/#licensing) · [Архивы SR28](https://www.ars.usda.gov/northeast-area/beltsville-md-bhnrc/beltsville-human-nutrition-research-center/methods-and-application-of-food-composition-laboratory/mafcl-site-pages/sr11-sr28/) |
| Livsmedelsverket | Выборка полей и карточек с русскими названиями | Указаны Swedish Food Agency, условия CC BY 4.0 и характер изменений. [Шведская база](https://www.livsmedelsverket.se/en/about-us/open-data/food-composition-data/) |
| Max Rubner-Institut | BLS 4.0, выборка карточек и показатели ингредиентов для расчётных блюд | Указаны издатель, DOI, лицензия CC BY 4.0, переводы и расчёт КБЖУ отдельных рецептов. [Загрузка BLS](https://blsdb.de/download) |
| Public Health England | Выборка CoFID 2021 с русскими названиями | Указаны издатель, выпуск, характер изменений и Open Government Licence v3.0. [Выпуск CoFID](https://www.gov.uk/government/publications/composition-of-foods-integrated-dataset-cofid) · [OGL 3.0](https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/) |

При публичном выпуске предусмотрите доступ получателей к той же машиночитаемой выборке Open Food Facts и её уведомлениям. Точный файл хранится отдельно от других наборов и доступен для извлечения из APK. Сопровождающий релиз отдельным JSON даст получателям прямой доступ. [ODbL, разделы 4.2–4.7](https://opendatacommons.org/licenses/odbl/1-0/) · [Источники и преобразования в проекте](CATALOG-SOURCES.md)

## Распознавание этикеток и кодов

| Компонент | Версия | Сверенные материалы |
| --- | --- | --- |
| Tesseract4Android и Tesseract OCR | 4.9.0 и 5.5.1 | Apache 2.0, сведения об авторах обёртки и ссылки на оба проекта. [Обёртка и зависимости](https://github.com/adaptech-cz/Tesseract4Android/tree/4.9.0) · [OCR](https://github.com/tesseract-ocr/tesseract/tree/5.5.1) |
| Leptonica | 1.85.0 | Оригинальный `leptonica-license.txt` из исходников обёртки переименован для APK. [Исходники зависимости](https://github.com/adaptech-cz/Tesseract4Android/tree/4.9.0/tesseract4android/src/main/cpp/leptonica/src) |
| libjpeg | 9f | Оригинальный README из исходников обёртки и указание Independent JPEG Group в сводном уведомлении. [Исходники зависимости](https://github.com/adaptech-cz/Tesseract4Android/tree/4.9.0/tesseract4android/src/main/cpp/libjpeg/src) · [Архивы IJG](https://ijg.org/files/) |
| libpng | 1.6.48 | Оригинальный `LICENSE` из исходников обёртки переименован для APK. [Лицензия libpng](https://github.com/pnggroup/libpng/blob/v1.6.48/LICENSE) |
| tessdata_fast | 4.1.0 | Apache 2.0, точное совпадение SHA-256 обеих моделей с файлами выпуска. [Модели и лицензия](https://github.com/tesseract-ocr/tessdata_fast/tree/4.1.0) |
| ML Kit Barcode Scanning | 17.3.0 | Указаны условия Google и сведения об обработке данных на устройстве и возможной передаче метрик SDK. Это описание на основе документации поставщика. [Условия и конфиденциальность](https://developers.google.com/ml-kit/terms) |

SHA-256 моделей tessdata_fast

- `eng.traineddata` — `7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2`
- `rus.traineddata` — `e16e5e036cce1d9ec2b00063cf8b54472625b9e14d893a169e2b0dedeb4df225`

## Проверка APK 24 ноября 2026 года

Проверены debug APK и неподписанный релизный APK `com.niamniam.kbjy` версии 0.5. Все семь файлов в `assets/licenses/` в обоих APK побайтово совпадают с исходниками проекта. Обе модели OCR в обоих APK совпадают с файлами выпуска 4.1.0, а отдельный JSON Open Food Facts совпадает с выборкой в проекте. Дополнительно в `META-INF` сохранены шесть файлов лицензий, поступивших из зависимостей.

В релизном APK обнаружены библиотеки `libandroidx.graphics.path.so`, `libbarhopper_v3.so`, `libjpeg.so`, `libleptonica.so`, `libpngx.so` и `libtesseract.so`. APK содержит варианты для `arm64-v8a`, `armeabi-v7a`, `x86` и `x86_64`. Команда `:app:dependencies --configuration releaseRuntimeClasspath` сформировала дерево зависимостей отдельно от инструментальных и модульных тестов.

## Уведомления зависимостей Android и Google

Версии сверены по `releaseRuntimeClasspath`, условия — по метаданным POM выбранных версий и исходникам поставщиков. Сводное уведомление включает AndroidX, JetBrains Annotations 23.0.0, Kotlin 2.2.21, Coroutines 1.8.1, Serialization 1.7.3, Guava ListenableFuture 1.0, javax.inject 1 и JSpecify 1.0.0. Для них используется уже вложенный текст Apache 2.0. В AndroidX отдельно проверены, в частности, Compose UI 1.9.1, Material3 1.3.2, Room 2.8.1 и Core 1.17.0. [AndroidX](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt) · [Kotlin](https://github.com/JetBrains/kotlin/blob/v2.2.21/license/LICENSE.txt) · [Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/1.8.1/LICENSE.txt) · [Serialization](https://github.com/Kotlin/kotlinx.serialization/blob/v1.7.3/LICENSE.txt) · [Guava](https://github.com/google/guava/blob/v26.0/COPYING)

| Условия из POM | Проверенные модули |
| --- | --- |
| [ML Kit Terms of Service](https://developers.google.com/ml-kit/terms) | Barcode Scanning 17.3.0, Barcode Scanning Common 17.0.0, Common 18.11.0, Vision Common 17.3.0, Vision Interfaces 16.3.0, Play Services ML Kit Barcode Scanning 18.3.1 |
| [Android Software Development Kit License Agreement](https://developer.android.com/studio/terms.html) | Play Services Base 18.5.0, Basement 18.4.0, Tasks 18.2.0, ODML Image 1.0.0-beta1 |
| [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | DataTransport API 2.2.1, Backend CCT 2.3.3, Runtime 2.2.6, Firebase Annotations 16.0.0, Components 16.1.0, Encoders 16.1.0, Encoders JSON 17.1.0 |

В отдельных AAR Google находятся пары `third_party_licenses.json` и `third_party_licenses.txt`. По указанным в JSON байтовым диапазонам извлечена 471 запись из 11 архивов с непустыми уведомлениями. В [Google-SDK-notices.txt](../android/app/src/main/assets/licenses/Google-SDK-notices.txt) сохранены 49 уникальных текстов, 103 исходных названия компонентов и модули SDK, которые предоставили каждый текст. При объединении убраны только повторы и внешние пробелы. Исходные уведомления охватывают в том числе TensorFlow, OpenCV и Abseil. При обновлении перечисленных зависимостей сверку и сборку этого файла необходимо повторить. [Как Google собирает уведомления из AAR](https://developers.google.com/android/guides/opensource#how_the_list_of_licenses_is_determined)

В новом debug APK и неподписанном release APK все восемь файлов в `assets/licenses/` побайтово совпали с исходниками проекта. SHA-256 вложенного `Google-SDK-notices.txt` — `eab91be2b00618c4e83beccac2a385309ca01d605637cb0398273aab67ed8f3b`.

## Оставшиеся проверки перед выпуском

- Проверить сетевое поведение ML Kit на релизной сборке. Для обновлений приложение использует разрешения `INTERNET`, `ACCESS_NETWORK_STATE` и `REQUEST_INSTALL_PACKAGES`. Сама проверка манифеста показывает доступные разрешения.
- Повторить сверку содержимого для подписанного APK и подготовить материалы для скачивания вместе с ним.

## Просмотр в приложении

Раздел «Настройки → Источники и лицензии» открывает сводку об источниках каталога и компонентах, полные тексты ODbL, DbCL, Apache 2.0, Leptonica, libjpeg и libpng, а также уведомления о компонентах Google SDK. Документы читаются из [файлов внутри APK](../android/app/src/main/assets/licenses/) без сети. Названия раздела и кнопки навигации доступны на русском и английском. Тексты сторонних лицензий показаны в оригинале.

На `NiamNiamTest` с включённым авиарежимом и отключённым Wi-Fi два теста подтвердили открытие сводки Open Food Facts и текста Leptonica через русское и английское меню. В русском сценарии проверены восстановление открытого документа после пересоздания экрана, системный возврат к списку и кнопка «Назад» для возврата в настройки. [Тесты](../android/app/src/androidTest/java/com/niamniam/kbjy/ThirdPartyNoticesTest.kt)

После добавления уведомлений Google SDK в том же офлайн-режиме прошли все три сценария экрана, включая открытие уведомления Abseil в новом документе.
