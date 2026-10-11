import { useEffect, useState } from "react";

/** Below the sticky site header (6rem), 30 % down the viewport. */
const HEADER_PX = 96;
const READING_LINE_RATIO = 0.3;

/**
 * The anchor currently being read: the last one (in reading order) whose top has passed
 * the reading line. Covers pages and activity blocks alike.
 */
export function useActiveUnitAnchor(anchorIds: string[]): string | null {
  const [active, setActive] = useState<string | null>(anchorIds[0] ?? null);
  const key = anchorIds.join("|");

  useEffect(() => {
    const ids = key ? key.split("|") : [];
    if (ids.length === 0) {
      setActive(null);
      return;
    }

    let frame = 0;
    const compute = () => {
      frame = 0;
      const line = HEADER_PX + window.innerHeight * READING_LINE_RATIO;
      let current = ids[0];
      for (const id of ids) {
        const el = document.getElementById(id);
        if (!el) continue;
        if (el.getBoundingClientRect().top <= line) current = id;
        else break;
      }
      setActive(current);
    };
    const schedule = () => {
      if (!frame) frame = requestAnimationFrame(compute);
    };

    compute();
    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("resize", schedule, { passive: true });
    // Pages grow as they render, moving later anchors without any scroll event.
    const observer = new ResizeObserver(schedule);
    observer.observe(document.body);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("scroll", schedule);
      window.removeEventListener("resize", schedule);
      observer.disconnect();
    };
  }, [key]);

  return active;
}
