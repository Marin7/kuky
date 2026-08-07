import type { HomeworkItem, SharedPresentationSummary } from "@/lib/learning";

export const OTHER_KEY = "__other__";

export interface UnitGroup {
  key: string;
  unitId: string | null;
  level: string | null;
  label: string | null; // null → "Other" bucket
  position: number;
  presentations: SharedPresentationSummary[];
  homework: HomeworkItem[];
}

export function buildGroups(
  presentations: SharedPresentationSummary[],
  homework: HomeworkItem[],
): UnitGroup[] {
  const map = new Map<string, UnitGroup>();
  const other: UnitGroup = {
    key: OTHER_KEY,
    unitId: null,
    level: null,
    label: null,
    position: Number.MAX_SAFE_INTEGER,
    presentations: [],
    homework: [],
  };

  const ensure = (unit: {
    id: string;
    level: string;
    subject: string;
    position: number;
  }) => {
    const key = unit.id;
    let g = map.get(key);
    if (!g) {
      g = {
        key,
        unitId: unit.id,
        level: unit.level,
        label: `${unit.level} · ${unit.subject}`,
        position: unit.position,
        presentations: [],
        homework: [],
      };
      map.set(key, g);
    }
    return g;
  };

  for (const p of presentations) {
    if (p.unit) ensure(p.unit).presentations.push(p);
    else other.presentations.push(p);
  }
  for (const h of homework) {
    if (h.unit) ensure(h.unit).homework.push(h);
    else other.homework.push(h);
  }

  const groups = [...map.values()].sort((a, b) => {
    const lvl = (a.level ?? "").localeCompare(b.level ?? "");
    return lvl !== 0 ? lvl : a.position - b.position;
  });
  if (other.presentations.length > 0 || other.homework.length > 0) {
    groups.push(other);
  }
  return groups;
}

export function findUnitGroup(
  groups: UnitGroup[],
  unitId: string | null,
): UnitGroup | undefined {
  if (unitId === null) {
    return groups.find((g) => g.unitId === null);
  }
  return groups.find((g) => g.unitId === unitId);
}
