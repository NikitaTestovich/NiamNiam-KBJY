const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');

const context = vm.createContext({});
vm.runInContext(fs.readFileSync(require.resolve('./Intake.js'), 'utf8'), context);
const id = 'f07b4c5d-f6d1-47e4-bf79-00e2a72936e5';
const valid = { id, name: 'Борщ', unit: 'g', kcal: 65,
  protein: 3, fat: 4, carbs: 4, source: 'manual' };

test('rejects unknown fields, invalid enum values and invalid nutrition', () => {
  for (const patch of [{ status: 'approved' }, { diary: [] }, { protein: -1 }, { kcal: '65' }, { unit: 'kg' }]) {
    assert.throws(() => context.validateSubmission(JSON.stringify({ ...valid, ...patch })));
  }
});

test('accepts the old Android payload without storing its invite field', () => {
  const name = '=Текст карточки';
  const invite = 'a'.repeat(32);
  let row;
  const result = context.receiveSubmission(JSON.stringify({ ...valid, name, invite }), {
    now: () => '2026-11-22T12:00:00Z',
    appendOnce: (_, value) => { row = value; return true; },
  });
  assert.equal(result.ok, true);
  assert.equal(row[2], 'pending');
  assert.equal(row[3], name);
  assert.equal(row.includes(invite), false);
});

test('a duplicate submission is acknowledged without a second row', () => {
  const rows = new Map();
  const deps = { now: () => '2026-11-22T12:00:00Z',
    appendOnce(key, row) { if (rows.has(key)) return false; rows.set(key, row); return true; } };
  context.receiveSubmission(JSON.stringify(valid), deps);
  const second = context.receiveSubmission(JSON.stringify(valid), deps);
  assert.equal(second.duplicate, true);
  assert.equal(rows.size, 1);
});

test('Apps Script accepts a card without an invite and writes literal values with RAW mode', () => {
  let append;
  Object.assign(context, {
    PropertiesService: { getScriptProperties: () => ({ getProperty: key => key === 'CATALOG_SPREADSHEET_ID' ? 'test-sheet' : null }) },
    LockService: { getScriptLock: () => ({ waitLock() {}, releaseLock() {} }) },
    Sheets: { Spreadsheets: { Values: {
      get: () => ({ values: [['id']] }), append: (body, sheet, range, options) => { append = { body, sheet, range, options }; },
    } } },
    ContentService: { MimeType: { JSON: 0 }, createTextOutput: result => ({ setMimeType: () => result }) },
  });
  const result = context.doPost({ postData: { contents: JSON.stringify({ ...valid, name: '=Текст карточки' }) } });
  assert.equal(JSON.parse(result).ok, true);
  assert.equal(append.options.valueInputOption, 'RAW');
  assert.equal(append.range, 'Proposals!A:L');
  assert.equal(append.body.values[0][3], '=Текст карточки');
});
