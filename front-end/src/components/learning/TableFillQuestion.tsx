import type { StudentStructure } from "@/lib/learning";
import { Input } from "@/components/ui/input";

interface Props {
  structure: StudentStructure;
  value: Record<string, string>;
  onChange: (cells: Record<string, string>) => void;
  /** Teacher preview: show blank cells without answer inputs. */
  readOnly?: boolean;
}

/** Renders a TABLE_FILL grid: fixed cells as plain text, blank cells as inputs keyed `"r,c"`. */
export function TableFillQuestion({
  structure,
  value,
  onChange,
  readOnly = false,
}: Props) {
  const rowHeaders = structure.rowHeaders ?? [];
  const colHeaders = structure.colHeaders ?? [];
  const cells = structure.cells ?? [];
  const cellAt = (r: number, c: number) =>
    cells.find((cell) => cell.r === r && cell.c === c);

  const setCell = (r: number, c: number, text: string) =>
    onChange({ ...value, [`${r},${c}`]: text });

  return (
    <div className="overflow-x-auto">
      <table className="border-collapse text-base">
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
                return (
                  <td key={c} className="border p-2">
                    {cell.type === "fixed" ? (
                      <span>{cell.text}</span>
                    ) : readOnly ? (
                      <span
                        aria-hidden
                        className="inline-block h-9 w-32 border-b border-dashed border-muted-foreground/50"
                      />
                    ) : (
                      <Input
                        value={value[`${r},${c}`] ?? ""}
                        onChange={(e) => setCell(r, c, e.target.value)}
                        className="h-9 w-32 text-base md:text-base"
                      />
                    )}
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
