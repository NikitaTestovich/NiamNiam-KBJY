const test = require('node:test');
const assert = require('node:assert/strict');
const { exportApproved } = require('./export-approved.cjs');
const { proposalColumns } = require('./Intake.js');

const approved = ['e0dffbac-337d-431e-8164-b68fd264abee', '2026-11-22', 'approved',
  'Борщ', 'g', 56, 2, 3, 6, 'manual'];

test('exports only approved cards and omits moderation metadata', () => {
  const result = exportApproved({ values: [proposalColumns, ['ignored', '', 'pending'], approved, ['ignored', '', 'rejected']] });
  assert.deepEqual(result, { schemaVersion: 1, cards: [{ id: approved[0], name: 'Борщ', unit: 'g',
    kcal: 56, protein: 2, fat: 3, carbs: 6, source: 'manual' }] });
});

test('refuses duplicate approved IDs and invalid nutrition', () => {
  assert.throws(() => exportApproved({ values: [proposalColumns, approved, approved] }), /row 3/);
  const invalid = [...approved];
  invalid[5] = '56';
  assert.throws(() => exportApproved({ values: [proposalColumns, invalid] }), /row 2/);
});
