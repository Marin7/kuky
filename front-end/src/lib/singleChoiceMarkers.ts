import { MAX_SINGLE_CHOICE_ITEMS } from "@/lib/exerciseLimits";

// Mirrors SingleChoiceMarkerParser.java: `(1)`, `(01)` ≡ `(1)`; `(ser)` ignored.
const MARKER = /\((\d+)\)/g;

/** Distinct marker numbers ≥ 1, in numeric order. */
export function distinctMarkerNumbers(prompt: string): number[] {
  if (!prompt) return [];
  const numbers = new Set<number>();
  for (const match of prompt.matchAll(MARKER)) {
    const n = Number.parseInt(match[1], 10);
    if (Number.isFinite(n) && n >= 1) numbers.add(n);
  }
  return [...numbers].sort((a, b) => a - b);
}

export function hasSingleChoiceMarkers(prompt: string): boolean {
  return distinctMarkerNumbers(prompt).length > 0;
}

export interface SingleChoiceMarkerParse {
  numbered: boolean;
  valid: boolean;
  n: number;
  numbers: number[];
}

/**
 * Classic (no markers) or numbered 1…N. Invalid when numbers are gapped,
 * do not start at 1, or N exceeds {@link MAX_SINGLE_CHOICE_ITEMS}.
 */
export function parseSingleChoiceMarkers(
  prompt: string,
): SingleChoiceMarkerParse {
  const numbers = distinctMarkerNumbers(prompt);
  if (numbers.length === 0) {
    return { numbered: false, valid: true, n: 0, numbers };
  }
  const n = numbers[numbers.length - 1];
  if (n > MAX_SINGLE_CHOICE_ITEMS) {
    return { numbered: true, valid: false, n: 0, numbers };
  }
  for (let i = 1; i <= n; i++) {
    if (!numbers.includes(i)) {
      return { numbered: true, valid: false, n: 0, numbers };
    }
  }
  return { numbered: true, valid: true, n, numbers };
}
