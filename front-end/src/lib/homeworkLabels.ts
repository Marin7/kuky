/** Case-fold that keeps accents (`sí` ≠ `si`; `Subjuntivo` = `subjuntivo`). */
export function labelGroupKey(label: string | null | undefined): string | null {
  if (label == null) return null;
  const trimmed = label.trim();
  if (!trimmed) return null;
  return trimmed.toLocaleLowerCase("es");
}

export function homeworkLabels(item: { labels?: string[] | null }): string[] {
  return item.labels ?? [];
}

/** One display spelling per group; first-seen wins. */
export function uniqueLabels(items: { labels?: string[] | null }[]): string[] {
  const seen = new Map<string, string>();
  for (const item of items) {
    for (const label of homeworkLabels(item)) {
      const key = labelGroupKey(label);
      if (!key || seen.has(key)) continue;
      seen.set(key, label.trim());
    }
  }
  return [...seen.values()];
}

export function homeworkHasLabelGroup(
  item: { labels?: string[] | null },
  groupKey: string,
): boolean {
  return homeworkLabels(item).some(
    (label) => labelGroupKey(label) === groupKey,
  );
}

export function labelsEqual(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false;
  return a.every((label, i) => label === b[i]);
}
