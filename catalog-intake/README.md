# Подключение Google Таблицы к NiamNiam KBJY

## 1. Если Google пишет «Не удалось открыть файл»

Сначала попробуйте открыть Apps Script отдельно от таблицы. Скрипт умеет работать с таблицей по её идентификатору, поэтому привязка через меню «Расширения» необязательна.

1. На компьютере откройте приватное окно браузера. В Chrome нажмите **Ctrl+Shift+N**, в Firefox **Ctrl+Shift+P**.
2. В этом окне войдите **только в тот аккаунт Google, которому принадлежит таблица**.
3. Введите в адресной строке [script.google.com](https://script.google.com/) и нажмите **Новый проект / New project**. Если открылась стартовая страница с проектами, кнопка находится слева. Успешный результат — редактор с файлом `Code.gs`.
4. Если стартовая страница работает, а прежняя ссылка выдаёт ошибку, продолжайте в новом проекте. Если не открывается и новый проект, попробуйте другой браузер с одним аккаунтом. Для рабочего или учебного аккаунта уточните у администратора, доступен ли Apps Script. Для этой очереди можно использовать личный аккаунт и создать в нём отдельную таблицу.

Google указывает, что одновременный вход в несколько аккаунтов может мешать работе Apps Script. По одному сообщению об ошибке установить точную причину нельзя. [Инструкция Google по созданию проектов и устранению проблем с аккаунтами](https://developers.google.com/apps-script/guides/projects#fix_issues_with_multiple_google_accounts)

## 2. Подготовьте закрытую таблицу

1. Откройте [Google Таблицы](https://sheets.google.com/) в том же аккаунте и создайте пустую таблицу. Назовите её **NiamNiam — заявки в каталог**.
2. Внизу дважды нажмите на имя листа **Лист1 / Sheet1**. Введите ровно `Proposals` и нажмите **Enter**.
3. Нажмите на ячейку **A1** и вставьте следующую строку. Между заголовками стоят табуляции. Проверьте, что `id` находится в A1, `status` в C1, а `cooked_grams` в L1.

```text
id	received_at	status	name	unit	kcal	protein	fat	carbs	source	ingredients_json	cooked_grams
```

Если заголовки попали в одну ячейку, выделите её и выберите **Данные → Разделить текст на столбцы**, либо вставьте заголовки по ячейкам.

4. В правом верхнем углу откройте **Настройки доступа / Share**. В разделе **Общий доступ / General access** оставьте **Ограниченный доступ / Restricted**. Пользователям приложения доступ к таблице выдавать не требуется.
5. В адресной строке найдите фрагмент вида `/spreadsheets/d/ИДЕНТИФИКАТОР/edit`. Скопируйте символы между `/d/` и `/edit`. Это значение для `CATALOG_SPREADSHEET_ID`. [Справка Google о настройках доступа](https://support.google.com/drive/answer/2494822)

## 3. Вставьте код Apps Script

1. Вернитесь в редактор Apps Script из шага 1. Нажмите название **Проект без названия / Untitled project** и задайте имя **NiamNiam — приём заявок**.
2. Слева выберите **Редактор / Editor**, затем `Code.gs`. Откройте локальный [Intake.js](Intake.js), скопируйте весь текст и вставьте его в `Code.gs` вместо шаблона `myFunction`. Сохраните изменения.
3. Слева нажмите шестерёнку **Настройки проекта / Project Settings**. Включите **Показывать файл манифеста appsscript.json в редакторе / Show “appsscript.json” manifest file in editor**.
4. Вернитесь в редактор, откройте `appsscript.json` и замените его содержимое текстом из локального [appsscript.json](appsscript.json). Сохраните. В разделе **Службы / Services** должна появиться служба `Sheets`. Манифест уже содержит её подключение. [Скриншоты настроек и редактора в Google Codelab](https://codelabs.developers.google.com/devsite/codelabs/apps-script-vibe-code-gmail-add-on-gemini-cli-mcp-servers) · [Как Google подключает расширенные службы](https://developers.google.com/apps-script/guides/services/advanced)

Пример расположения шестерёнки и переключателя манифеста из учебного проекта Google.

![Шестерёнка настроек проекта](https://codelabs.developers.google.com/static/devsite/codelabs/apps-script-vibe-code-gmail-add-on-gemini-cli-mcp-servers/img/2bc043bb3c3a216d.png)

![Переключатель отображения appsscript.json](https://codelabs.developers.google.com/static/devsite/codelabs/apps-script-vibe-code-gmail-add-on-gemini-cli-mcp-servers/img/e74dca570d64e540.png)

## 4. Укажите идентификатор таблицы

В Apps Script откройте **Настройки проекта → Свойства скрипта**. Добавьте `CATALOG_SPREADSHEET_ID` со значением из шага 2 и нажмите **Сохранить свойства скрипта**. Для новой версии приёмника свойство `CATALOG_INVITE_HASHES` не требуется. [Справка Google о свойствах скрипта](https://developers.google.com/apps-script/guides/properties)

## 5. Разверните приёмник

1. В правом верхнем углу редактора нажмите **Развернуть / Deploy → Новое развертывание / New deployment**.
2. У поля **Выберите тип / Select type** нажмите шестерёнку и выберите **Веб-приложение / Web app**.
3. В поле **Описание / Description** укажите `NiamNiam intake v1`. Для **Выполнять от имени / Execute as** выберите **Я / Me**. Для **У кого есть доступ / Who has access** выберите **Все / Anyone**, чтобы Android мог обратиться к приёмнику без входа в Google. Таблица при этом сохраняет настройку **Ограниченный доступ**. По этому URL можно отправить заявку без кода. Новые записи получают статус `pending`.
4. Нажмите **Развернуть / Deploy**. Если Google предложит **Авторизовать доступ / Authorize access**, выберите тот же аккаунт и проверьте запрос доступа к таблицам у созданного вами скрипта. Если пункта **Все / Anyone** нет в управляемом аккаунте, проверьте доступность этой настройки у администратора или повторите шаги в личном аккаунте.
5. Скопируйте **URL веб-приложения**. Адрес для Android имеет вид `https://script.google.com/macros/s/…/exec`. После изменений кода обновляйте существующее развертывание через **Управление развертываниями / Manage deployments → Изменить → Новая версия**, чтобы сохранить адрес. [Развёртывание веб-приложения](https://developers.google.com/apps-script/guides/web#deploy_a_script_as_a_web_app) · [Управление версиями](https://developers.google.com/apps-script/concepts/deployments)

## 6. Проверьте запись до сборки Android

В PowerShell вставьте этот блок и по запросу вставьте URL из шага 5. Будет отправлена вымышленная карточка.

```powershell
$catalogUrl = (Read-Host 'Вставьте полный URL веб-приложения с окончанием /exec').Trim()
if ($catalogUrl -cnotmatch '\Ahttps://script\.google\.com/macros/s/[A-Za-z0-9_-]+/exec\z') {
    throw 'Скопируйте URL веб-приложения целиком из окна развертывания. Он начинается с https://script.google.com/macros/s/ и заканчивается на /exec.'
}
$catalogCard = @{
    id = [guid]::NewGuid().ToString()
    name = 'Тестовый борщ NiamNiam'
    unit = 'g'
    kcal = 56
    protein = 2
    fat = 3
    carbs = 6
    source = 'manual'
}
$catalogPayload = $catalogCard | ConvertTo-Json -Compress
Invoke-RestMethod -Uri $catalogUrl -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($catalogPayload))
```

Ожидаемый результат — `ok: True`, `duplicate: False`, а в листе `Proposals` одна новая строка со статусом `pending`. Повторите последнюю команду с тем же `$catalogPayload`. Ожидайте `duplicate: True` без второй строки.

| Что видите | Что проверить |
| --- | --- |
| `Invalid URI: The hostname could not be parsed` | В Apps Script откройте **Развернуть → Управление развертываниями**, выберите активное развертывание и скопируйте **URL веб-приложения**. Вставьте его целиком, одной строкой, без кавычек. Нужен адрес с `https://` и `/exec`, а не идентификатор развертывания или адрес редактора. Затем запустите блок шага 6 заново. |
| Ошибка о функции `doGet` при открытии `/exec` в браузере | Адресная строка делает GET. Для проверки записи используйте POST из PowerShell. |
| Страница входа в Google вместо JSON | Проверьте URL с окончанием `/exec` и настройку доступа **Все / Anyone** у развертывания. |
| `invalid_submission` | Убедитесь, что отправляете JSON из приведённого блока. |
| `temporarily_unavailable` | Сверьте идентификатор таблицы, название `Proposals`, ячейку A1, доступ владельца и службу `Sheets`. |
| Кнопки предложения нет в Android | Соберите APK с параметром `-PcatalogIntakeUrl=https://script.google.com/macros/s/…/exec` и откройте сохранённую карточку. |

### Если сервер вернул `temporarily_unavailable`

Запрос дошёл до Apps Script. Чтобы увидеть причину от имени владельца, откройте **Редактор → Code.gs** и добавьте эту временную функцию в конец файла. Она читает настройки и заголовки листа. Строки в таблицу она не добавляет.

```javascript
function checkNiamNiamSetup() {
  const properties = PropertiesService.getScriptProperties();
  const sheetId = properties.getProperty('CATALOG_SPREADSHEET_ID');
  if (!sheetId) throw new Error('Отсутствует CATALOG_SPREADSHEET_ID');

  if (typeof Sheets === 'undefined') throw new Error('В разделе Службы отсутствует Google Sheets API');
  const result = Sheets.Spreadsheets.Values.get(sheetId, 'Proposals!A1:L1');
  const actual = result.values?.[0] || [];
  if (JSON.stringify(actual) !== JSON.stringify(proposalColumns)) {
    throw new Error('Заголовки Proposals!A1:L1 отличаются от строки из шага 2');
  }
  console.log('OK: свойства, служба Sheets и заголовки листа доступны.');
}
```

Сохраните файл. В списке функций рядом с кнопкой **Выполнить / Run** выберите **checkNiamNiamSetup** и нажмите **Выполнить**. Если Google запросит доступ к таблицам, выберите аккаунт владельца и подтвердите его для вашего скрипта. Результат появится в **Журнале выполнения / Execution log**. Для этой проверки новое развертывание не требуется.

- **Unable to parse range** — проверьте название вкладки внизу таблицы. Оно должно быть ровно `Proposals`.
- **Requested entity was not found** или **permission denied** — сверьте ID между `/d/` и `/edit` и проверьте, что аккаунт, от имени которого выполняется веб-приложение, имеет права редактора таблицы.
- **Google Sheets API has not been used** или **disabled** — если проект привязан к собственному Google Cloud проекту, включите в нём Google Sheets API. Для стандартной настройки Apps Script проверьте наличие `Sheets` в разделе **Службы**. [Справка по расширенным службам](https://developers.google.com/apps-script/guides/services/advanced#enable_advanced_services)
- **Проверка показала OK** — проверьте, что веб-приложение выполняется **от вашего имени**, а URL относится к нужному проекту. Если вы правили код или манифест после развертывания, обновите его через **Управление развертываниями → Изменить → Новая версия**. Затем повторите POST из шага 6. Если ошибка остаётся, пришлите время запроса и текст результата проверки без ID таблицы и секретов.

## 7. Модерация и выгрузка

### Проверьте заявку

Откройте вкладку `Proposals`. Столбец **C (`status`)** управляет выгрузкой. `pending` означает «на проверке», `approved` — «одобрено», `rejected` — «отклонено». Перед одобрением проверьте название, источник, единицу и КБЖУ на 100 г или 100 мл. Сверьте карточку с общим каталогом на дубли. Сохраняйте заголовки и идентификаторы строк.

Чтобы выбирать статус из списка, откройте **Данные → Проверка данных → Добавить правило**. Укажите диапазон `Proposals!C2:C` и критерий **Раскрывающийся список**. Добавьте `pending` (серый), `approved` (зелёный) и `rejected` (красный). Для недопустимых значений выберите **Отклонять ввод** и нажмите **Готово**. Эти значения уже используются приёмником и экспортом. После настройки проверьте, что выпадающий список виден и в новой строке, отправленной из приложения. [Инструкция Google со скриншотами и обозначениями](https://support.google.com/docs/answer/186103?co=GENIE.Platform%3DDesktop&hl=ru)

Для пробы выставьте `approved` в строке тестового блюда с телефона. Оставьте «Тестовый борщ NiamNiam», отправленный через PowerShell, в `pending`. Это позволит проверить, что выгрузка берёт только одобренную заявку. После пробы смените статус вымышленной карточки на `rejected`.

### Получите JSON через редактор Apps Script

Добавьте в конец `Code.gs` функцию ниже, сохраните и запустите **copyNiamNiamApprovedPreview** из списка функций. Она читает данные от имени владельца. Новое развертывание для этого шага не нужно. Функция подходит для пробной выгрузки небольшой пачки. Если журнал обрежет строку, выгрузите данные через Sheets API методом `spreadsheets.values.get` с `valueRenderOption=UNFORMATTED_VALUE`.

```javascript
function copyNiamNiamApprovedPreview() {
  const sheetId = PropertiesService.getScriptProperties().getProperty('CATALOG_SPREADSHEET_ID');
  const result = Sheets.Spreadsheets.Values.get(sheetId, 'Proposals!A:L', {
    valueRenderOption: 'UNFORMATTED_VALUE',
  });
  const [header, ...rows] = result.values || [];
  if (JSON.stringify(header) !== JSON.stringify(proposalColumns)) {
    throw new Error('Проверьте заголовки Proposals!A1:L1');
  }
  console.log(JSON.stringify({ values: [header, ...rows.filter(row => row[2] === 'approved')] }));
}
```

В **Журнале выполнения** скопируйте только JSON от первого `{` до последнего `}`. Вставьте его в Блокнот и сохраните в кодировке UTF-8 под именем `niamniam-sheet.json` в вашей папке **Загрузки**. В окне сохранения выберите тип **Все файлы**, чтобы к имени не добавилось `.txt`. Затем откройте PowerShell в корне репозитория и выполните блок. Он прочитает сохранённый файл и выведет количество карточек и путь к выгрузке.

```powershell
$catalogInput = Join-Path $env:USERPROFILE 'Downloads\niamniam-sheet.json'
$catalogOutput = Join-Path ([IO.Path]::GetTempPath()) ('niamniam-approved-' + [guid]::NewGuid().ToString('N') + '.json')
if (-not (Test-Path -LiteralPath $catalogInput -PathType Leaf)) { throw "Сохраните JSON как $catalogInput" }
node catalog-intake/export-approved.cjs $catalogInput $catalogOutput
if ($LASTEXITCODE -ne 0) { throw 'Выгрузка остановлена. Проверьте сохранённый JSON и сообщение экспортёра.' }
$catalogExport = Get-Content -LiteralPath $catalogOutput -Raw -Encoding utf8 | ConvertFrom-Json
"Карточек в выгрузке: $($catalogExport.cards.Count)"
"Файл: $catalogOutput"
```

Проверьте, что в JSON есть карточка со статусом `approved` из таблицы, а заявка `pending` отсутствует. Если одобрена только одна заявка, PowerShell покажет `Карточек в выгрузке: 1`. Экспортёр проверяет КБЖУ, единицы и повторы ID. Файл хранится вне репозитория. После проверки верните тестовые строки в `rejected` или `pending` в зависимости от того, нужны ли они для повторного теста. Импорт файла в общий каталог и доставка на телефоны относятся к следующему этапу.

Локальные проверки приёмника и экспорта запускаются командой `node --test "catalog-intake/*.test.cjs"`.
