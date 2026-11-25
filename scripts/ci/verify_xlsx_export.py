"""Read NiamNiam's synthetic exports with an independent XLSX parser."""

from datetime import datetime
from pathlib import Path
import unittest

from openpyxl import load_workbook


REPORTS = Path(__file__).resolve().parents[2] / "android/app/build/reports"
FIRST = datetime(2026, 11, 20)
SECOND = datetime(2026, 11, 21)
THIRD = datetime(2026, 11, 22)


class WorkbookCompatibilityTest(unittest.TestCase):
    def test_exported_workbooks_preserve_values_and_types(self):
        for filename, sheets, header in (
            ("synthetic-workbook.xlsx", ["По дням", "Дневник", "Вес"], "Дата"),
            ("synthetic-workbook-en.xlsx", ["Days", "Diary", "Weight"], "Date"),
        ):
            with self.subTest(filename=filename), (REPORTS / filename).open("rb") as source:
                workbook = load_workbook(source, read_only=True, data_only=False)
                try:
                    self.assertEqual(sheets, workbook.sheetnames)
                    days, diary, weight = workbook.worksheets
                    for sheet in workbook.worksheets:
                        self.assertEqual(header, sheet["A1"].value)
                        self.assertEqual("d", sheet["A2"].data_type)
                    self.assertEqual(
                        [
                            (FIRST, 282.5, 2000, 4, 100, 1.25, 70, 63.5, 240, 250, None, 81.4),
                            (SECOND, None, 1800, None, 110, None, 60, None, 210, 250, 2000, None),
                            (THIRD, None, 1800, None, 110, None, 60, None, 210, None, 2000, None),
                        ],
                        list(days.iter_rows(min_row=2, max_col=12, values_only=True)),
                    )
                    self.assertEqual("n", days["B2"].data_type)
                    self.assertEqual(
                        [
                            (FIRST, "lunch", "Тестовый сок", 250, "ml", 100, 0, 0, 25, 250),
                            (FIRST, "dinner", "=1+1 & <рис> 🍚 ＋\r\n\tбез соли", 125, "g", 182.5, 4, 1.25, 38.5, None),
                            (SECOND, "lunch", "Вода", 250, "ml", 0, 0, 0, 0, 250),
                        ],
                        list(diary.iter_rows(min_row=2, max_col=10, values_only=True)),
                    )
                    self.assertEqual("s", diary["C3"].data_type)
                    self.assertEqual(
                        [(FIRST, 82), (FIRST, 81.4)],
                        list(weight.iter_rows(min_row=2, max_col=2, values_only=True)),
                    )
                finally:
                    workbook.close()


if __name__ == "__main__":
    unittest.main()
