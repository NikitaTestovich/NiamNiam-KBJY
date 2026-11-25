const submissionFields = new Set(['id', 'invite', 'name', 'unit', 'kcal', 'protein', 'fat', 'carbs', 'source', 'ingredients', 'cookedGrams']);
const proposalColumns = ['id', 'received_at', 'status', 'name', 'unit', 'kcal', 'protein', 'fat', 'carbs', 'source', 'ingredients_json', 'cooked_grams'];

function validateSubmission(body) {
  if (typeof body !== 'string' || body.length > 16000) throw new Error('invalid_body');
  const value = JSON.parse(body);
  validateCard(value);
  return value;
}

function validateCard(value) {
  const text = (input, max) => typeof input === 'string' && input.trim().length > 0 && input.length <= max && !/[\x00-\x1f\x7f]/.test(input);
  const number = (input, max) => Number.isFinite(input) && input >= 0 && input <= max;
  if (!value || Object.keys(value).some(key => !submissionFields.has(key)) ||
      typeof value.id !== 'string' || !/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(value.id) ||
      !text(value.name, 120) || !['g', 'ml'].includes(value.unit) ||
      !['manual', 'label', 'recipe'].includes(value.source) ||
      !number(value.kcal, 1000) || !number(value.protein, 100) || !number(value.fat, 100) || !number(value.carbs, 100)) {
    throw new Error('invalid_card');
  }
  if (value.source === 'recipe') {
    if (value.unit !== 'g' || !number(value.cookedGrams, 100000) || value.cookedGrams === 0 ||
        !Array.isArray(value.ingredients) || value.ingredients.length < 1 || value.ingredients.length > 40 ||
        value.ingredients.some(item => !item || Object.keys(item).some(key => !['name', 'amount', 'unit'].includes(key)) ||
          !text(item.name, 120) || !['g', 'ml'].includes(item.unit) || !number(item.amount, 100000) || item.amount === 0)) {
      throw new Error('invalid_recipe');
    }
  } else if ('ingredients' in value || 'cookedGrams' in value) {
    throw new Error('unexpected_recipe');
  }
  return value;
}

if (typeof module !== 'undefined') module.exports = { proposalColumns, validateCard };

function receiveSubmission(body, deps) {
  let card;
  try { card = validateSubmission(body); }
  catch (_) { return { ok: false, error: 'invalid_submission' }; }
  const row = [card.id, deps.now(), 'pending', card.name.trim(), card.unit, card.kcal, card.protein,
    card.fat, card.carbs, card.source, card.ingredients ? JSON.stringify(card.ingredients) : '', card.cookedGrams ?? ''];
  return { ok: true, id: card.id, duplicate: !deps.appendOnce(card.id, row) };
}

function doPost(event) {
  let result;
  try {
    const properties = PropertiesService.getScriptProperties();
    const spreadsheetId = properties.getProperty('CATALOG_SPREADSHEET_ID');
    if (!spreadsheetId) throw new Error('not_configured');
    result = receiveSubmission(event && event.postData && event.postData.contents, {
      now: () => new Date().toISOString(),
      appendOnce(id, row) {
        const lock = LockService.getScriptLock();
        lock.waitLock(10000);
        try {
          const ids = Sheets.Spreadsheets.Values.get(spreadsheetId, 'Proposals!A:A').values || [];
          if (ids[0]?.[0] !== proposalColumns[0]) throw new Error('missing_header');
          if (ids.some(cell => cell[0] === id)) return false;
          Sheets.Spreadsheets.Values.append({ values: [row] }, spreadsheetId, 'Proposals!A:L', {
            valueInputOption: 'RAW', insertDataOption: 'INSERT_ROWS',
          });
          return true;
        } finally { lock.releaseLock(); }
      },
    });
  } catch (_) {
    result = { ok: false, error: 'temporarily_unavailable' };
  }
  return ContentService.createTextOutput(JSON.stringify(result)).setMimeType(ContentService.MimeType.JSON);
}
