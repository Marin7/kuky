import { useTranslation } from "react-i18next";
import type { StudentStructure, UnitResult } from "@/lib/learning";

interface Props {
  structure: StudentStructure;
  unitResults: UnitResult[];
}

function displayOrDash(value: string | null | undefined, empty: string): string {
  const trimmed = value?.trim();
  return trimmed ? trimmed : empty;
}

/** Graded TABLE_FILL review: same grid layout as the take UI, with per-blank feedback. */
export function TableFillResult({ structure, unitResults }: Props) {
  const { t } = useTranslation();
  const noAnswer = t("learning.exerciseResult.noAnswer");
  const rowHeaders = structure.rowHeaders ?? [];
  const colHeaders = structure.colHeaders ?? [];
  const cells = structure.cells ?? [];

  const blankUnits = [...cells]
    .filter((c) => c.type === "blank")
    .sort((a, b) => a.r - b.r || a.c - b.c);
  const unitByCoord = new Map(
    blankUnits.map((cell, i) => [`${cell.r},${cell.c}`, unitResults[i]]),
  );

  const cellAt = (r: number, c: number) =>
    cells.find((cell) => cell.r === r && cell.c === c);

  return (
    <div className="mt-2 overflow-x-auto">
      <table className="border-collapse text-sm">
        <thead>
          <tr>
            <th className="p-2" />
            {colHeaders.map((h, c) => (
              <th key={c} className="border p-2 text-left font-medium">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rowHeaders.map((rh, r) => (
            <tr key={r}>
              <th className="border p-2 text-left font-medium">{rh}</th>
              {colHeaders.map((_, c) => {
                const cell = cellAt(r, c);
                if (!cell) return <td key={c} className="border p-2" />;
                if (cell.type === "fixed") {
                  return (
                    <td key={c} className="border p-2">
                      <span>{cell.text}</span>
                    </td>
                  );
                }

                const unit = unitByCoord.get(`${r},${c}`);
                const correct = unit?.correct ?? false;
                return (
                  <td
                    key={c}
                    className={`border p-2 align-top ${
                      correct
                        ? "bg-green-100 text-green-700"
                        : "bg-red-100 text-red-700"
                    }`}
                  >
                    <div className="space-y-0.5 text-xs">
                      <p className="font-medium">
                        {displayOrDash(unit?.studentDisplay, noAnswer)}
                      </p>
                      {!correct &&
                        unit?.expectedDisplay &&
                        unit.expectedDisplay.length > 0 && (
                          <p className="text-[11px] opacity-90">
                            {t("learning.exerciseResult.unitExpected")}{" "}
                            {unit.expectedDisplay.join(" / ")}
                          </p>
                        )}
                    </div>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
