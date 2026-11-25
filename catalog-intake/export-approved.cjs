const { readFileSync, writeFileSync } = require('node:fs');
const { proposalColumns, validateCard } = require('./Intake.js');

function exportApproved({ values }) {
  if (!Array.isArray(values) || !Array.isArray(values[0]) ||
      values[0].length !== proposalColumns.length ||
      proposalColumns.some((column, index) => values[0][index] !== column)) {
    throw new Error('Unexpected proposal headers');
  }
  const ids = new Set();
  const cards = [];
  for (const [index, row] of values.slice(1).entries()) {
    if (row[2] !== 'approved') continue;
    const [id, , , name, unit, kcal, protein, fat, carbs, source, ingredients, cookedGrams] = row;
    try {
      const card = { id, name, unit, kcal, protein, fat, carbs, source };
      if (source === 'recipe') {
        card.ingredients = JSON.parse(ingredients);
        card.cookedGrams = cookedGrams;
      }
      validateCard(card);
      if (ids.has(id)) throw new Error('Duplicate proposal');
      ids.add(id);
      cards.push(card);
    } catch (error) {
      throw new Error(`Invalid approved proposal at row ${index + 2}`, { cause: error });
    }
  }
  return { schemaVersion: 1, cards };
}

if (require.main === module) {
  const [, , input, output] = process.argv;
  if (!input || !output) throw new Error('Usage: node export-approved.cjs <sheet-values.json> <approved-cards.json>');
  const result = exportApproved(JSON.parse(readFileSync(input, 'utf8')));
  writeFileSync(output, `${JSON.stringify(result, null, 2)}\n`, { flag: 'wx' });
}

module.exports = { exportApproved };
