import { useTranslation } from "react-i18next";
import type { TableFillCell, TableFillStructure } from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface Props {
  structure: TableFillStructure;
  onChange: (structure: TableFillStructure) => void;
}

const MAX_DIM = 12;

function cellAt(cells: TableFillCell[], r: number, c: number): TableFillCell {
  return (
    cells.find((cell) => cell.r === r && cell.c === c) ?? {
      r,
      c,
      type: "blank",
      acceptedAnswers: [""],
    }
  );
}

/**
 * Authoring UI for TABLE_FILL — editable row/column headers plus a grid of
 * fixed/blank cells. Blank cells carry their own accepted-answer list (same
 * matching rule as FILL_BLANK); fixed cells carry static text.
 */
export function TableFillEditor({ structure, onChange }: Props) {
  const { t } = useTranslation();
  const rowHeaders = structure.rowHeaders ?? [];
  const colHeaders = structure.colHeaders ?? [];
  const rows = rowHeaders.length;
  const cols = colHeaders.length;

  const updateCell = (r: number, c: number, patch: Partial<TableFillCell>) => {
    const merged: TableFillCell = {
      ...cellAt(structure.cells, r, c),
      ...patch,
    };
    const idx = structure.cells.findIndex(
      (cell) => cell.r === r && cell.c === c,
    );
    const next = [...structure.cells];
    if (idx >= 0) next[idx] = merged;
    else next.push(merged);
    onChange({ ...structure, cells: next });
  };

  const setCellType = (r: number, c: number, type: "fixed" | "blank") =>
    type === "fixed"
      ? updateCell(r, c, {
          type: "fixed",
          text: "",
          acceptedAnswers: undefined,
        })
      : updateCell(r, c, {
          type: "blank",
          acceptedAnswers: [""],
          text: undefined,
        });

  const setCellAnswer = (
    r: number,
    c: number,
    answerIndex: number,
    value: string,
  ) => {
    const accepted = [
      ...(cellAt(structure.cells, r, c).acceptedAnswers ?? [""]),
    ];
    accepted[answerIndex] = value;
    updateCell(r, c, { acceptedAnswers: accepted });
  };

  const addCellAnswer = (r: number, c: number) =>
    updateCell(r, c, {
      acceptedAnswers: [
        ...(cellAt(structure.cells, r, c).acceptedAnswers ?? [""]),
        "",
      ],
    });

  const removeCellAnswer = (r: number, c: number, answerIndex: number) => {
    const accepted = (
      cellAt(structure.cells, r, c).acceptedAnswers ?? [""]
    ).filter((_, i) => i !== answerIndex);
    updateCell(r, c, {
      acceptedAnswers: accepted.length > 0 ? accepted : [""],
    });
  };

  const setRowHeader = (r: number, value: string) =>
    onChange({
      ...structure,
      rowHeaders: rowHeaders.map((h, i) => (i === r ? value : h)),
    });

  const setColHeader = (c: number, value: string) =>
    onChange({
      ...structure,
      colHeaders: colHeaders.map((h, i) => (i === c ? value : h)),
    });

  const addRow = () => {
    if (rows >= MAX_DIM) return;
    const r = rows;
    const newCells: TableFillCell[] = Array.from({ length: cols }, (_, c) => ({
      r,
      c,
      type: "blank",
      acceptedAnswers: [""],
    }));
    onChange({
      ...structure,
      rowHeaders: [...rowHeaders, ""],
      cells: [...structure.cells, ...newCells],
    });
  };

  const removeRow = (r: number) => {
    if (rows <= 1) return;
    const remaining = structure.cells
      .filter((cell) => cell.r !== r)
      .map((cell) => (cell.r > r ? { ...cell, r: cell.r - 1 } : cell));
    onChange({
      ...structure,
      rowHeaders: rowHeaders.filter((_, i) => i !== r),
      cells: remaining,
    });
  };

  const addCol = () => {
    if (cols >= MAX_DIM) return;
    const c = cols;
    const newCells: TableFillCell[] = Array.from({ length: rows }, (_, r) => ({
      r,
      c,
      type: "blank",
      acceptedAnswers: [""],
    }));
    onChange({
      ...structure,
      colHeaders: [...colHeaders, ""],
      cells: [...structure.cells, ...newCells],
    });
  };

  const removeCol = (c: number) => {
    if (cols <= 1) return;
    const remaining = structure.cells
      .filter((cell) => cell.c !== c)
      .map((cell) => (cell.c > c ? { ...cell, c: cell.c - 1 } : cell));
    onChange({
      ...structure,
      colHeaders: colHeaders.filter((_, i) => i !== c),
      cells: remaining,
    });
  };

  return (
    <div className="space-y-3 rounded-md border border-dashed p-3">
      <p className="text-xs text-muted-foreground">
        {t("admin.homework.questions.tableFillHint")}
      </p>
      <div className="overflow-x-auto">
        <table className="border-collapse text-sm">
          <thead>
            <tr>
              <th className="p-1" />
              {colHeaders.map((h, c) => (
                <th key={c} className="p-1 align-top">
                  <div className="flex items-center gap-1">
                    <Input
                      value={h}
                      onChange={(e) => setColHeader(c, e.target.value)}
                      className="h-8 w-28 text-xs"
                      placeholder={t(
                        "admin.homework.questions.columnHeaderPlaceholder",
                      )}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="h-7 px-1.5 text-xs text-destructive"
                      disabled={cols <= 1}
                      onClick={() => removeCol(c)}
                    >
                      ✕
                    </Button>
                  </div>
                </th>
              ))}
              <th className="p-1 align-top">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="h-7 text-xs"
                  disabled={cols >= MAX_DIM}
                  onClick={addCol}
                >
                  {t("admin.homework.questions.addColumn")}
                </Button>
              </th>
            </tr>
          </thead>
          <tbody>
            {rowHeaders.map((rh, r) => (
              <tr key={r}>
                <td className="p-1 align-top">
                  <div className="flex items-center gap-1">
                    <Input
                      value={rh}
                      onChange={(e) => setRowHeader(r, e.target.value)}
                      className="h-8 w-28 text-xs"
                      placeholder={t(
                        "admin.homework.questions.rowHeaderPlaceholder",
                      )}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="h-7 px-1.5 text-xs text-destructive"
                      disabled={rows <= 1}
                      onClick={() => removeRow(r)}
                    >
                      ✕
                    </Button>
                  </div>
                </td>
                {colHeaders.map((_, c) => {
                  const cell = cellAt(structure.cells, r, c);
                  return (
                    <td key={c} className="p-1 align-top">
                      <div className="w-36 space-y-1 rounded border bg-card p-1.5">
                        <Select
                          value={cell.type}
                          onValueChange={(v) =>
                            setCellType(r, c, v as "fixed" | "blank")
                          }
                        >
                          <SelectTrigger className="h-7 text-xs">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="blank">
                              {t("admin.homework.questions.cellBlank")}
                            </SelectItem>
                            <SelectItem value="fixed">
                              {t("admin.homework.questions.cellFixed")}
                            </SelectItem>
                          </SelectContent>
                        </Select>
                        {cell.type === "fixed" ? (
                          <Input
                            value={cell.text ?? ""}
                            onChange={(e) =>
                              updateCell(r, c, { text: e.target.value })
                            }
                            className="h-7 text-xs"
                            placeholder={t(
                              "admin.homework.questions.cellTextPlaceholder",
                            )}
                          />
                        ) : (
                          <div className="space-y-1">
                            {(cell.acceptedAnswers ?? [""]).map((a, ai) => (
                              <div key={ai} className="flex items-center gap-1">
                                <Input
                                  value={a}
                                  onChange={(e) =>
                                    setCellAnswer(r, c, ai, e.target.value)
                                  }
                                  className="h-7 text-xs"
                                  placeholder={t(
                                    "admin.homework.questions.acceptedAnswerPlaceholder",
                                  )}
                                />
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="sm"
                                  className="h-6 px-1 text-xs text-destructive"
                                  disabled={
                                    (cell.acceptedAnswers ?? [""]).length === 1
                                  }
                                  onClick={() => removeCellAnswer(r, c, ai)}
                                >
                                  ✕
                                </Button>
                              </div>
                            ))}
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              className="h-6 text-xs"
                              onClick={() => addCellAnswer(r, c)}
                            >
                              {t("admin.homework.questions.addAnswer")}
                            </Button>
                          </div>
                        )}
                      </div>
                    </td>
                  );
                })}
              </tr>
            ))}
            <tr>
              <td className="p-1">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="h-7 text-xs"
                  disabled={rows >= MAX_DIM}
                  onClick={addRow}
                >
                  {t("admin.homework.questions.addRow")}
                </Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
