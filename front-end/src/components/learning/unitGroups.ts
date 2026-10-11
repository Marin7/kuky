import type {
  HomeworkItem,
  SharedPresentationSummary,
  UnitQuizSummary,
} from "@/lib/learning";

export const OTHER_KEY = "__other__";

export interface UnitGroup {
  key: string;
  unitId: string | null;
  level: string | null;
  label: string | null; // null → "Other" bucket
  subject: string | null;
  position: number;
  unseen: boolean;
  presentations: SharedPresentationSummary[];
  homework: HomeworkItem[];
  /** Quizzes at the end of the unit, in the teacher's order (never in the "other" bucket). */
  quizzes: UnitQuizSummary[];
}

export function buildGroups(
  presentations: SharedPresentationSummary[],
  homework: HomeworkItem[],
  quizzes: UnitQuizSummary[] = [],
): UnitGroup[] {
  const map = new Map<string, UnitGroup>();
  const other: UnitGroup = {
    key: OTHER_KEY,
    unitId: null,
    level: null,
    label: null,
    subject: null,
    position: Number.MAX_SAFE_INTEGER,
    unseen: false,
    presentations: [],
    homework: [],
    quizzes: [],
  };

  const ensure = (unit: {
    id: string;
    level: string;
    subject: string;
    position: number;
    unseen?: boolean;
  }) => {
    const key = unit.id;
    let g = map.get(key);
    if (!g) {
      g = {
        key,
        unitId: unit.id,
        level: unit.level,
        label: `${unit.level} · ${unit.subject}`,
        subject: unit.subject,
        position: unit.position,
        unseen: Boolean(unit.unseen),
        presentations: [],
        homework: [],
        quizzes: [],
      };
      map.set(key, g);
    } else if (unit.unseen) {
      g.unseen = true;
    }
    return g;
  };

  for (const p of presentations) {
    if (p.unit) ensure(p.unit).presentations.push(p);
    else other.presentations.push(p);
  }
  // Homework is never part of a unit: it all lives in the "other" bucket.
  for (const h of homework) {
    other.homework.push(h);
    if (h.unseen) other.unseen = true;
  }
  for (const q of quizzes) {
    const g = ensure(q.unit);
    g.quizzes.push(q);
    if (q.unseen) g.unseen = true;
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
