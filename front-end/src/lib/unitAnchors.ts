/** DOM ids of the unit PDF viewer's pages and activity blocks (targets of the contents panel). */
export const pageAnchorId = (page: number) => `unit-page-${page}`;
export const activityAnchorId = (activityId: string) =>
  `unit-activity-${activityId}`;

const TICK_MS = 100;
const WAIT_FOR_ANCHOR_MS = 5000;
const SETTLE_MS = 1500;
const DRIFT_PX = 8;

let cancelPending: (() => void) | null = null;

/**
 * Scrolls the window to an anchor of the unit viewer. Waits for it to mount (the PDF may
 * still be loading), then re-aligns while earlier pages finish sizing — pages get their
 * height only once rendered, which pushes the target down after the first jump. Any user
 * scroll input cancels the correction.
 */
export function scrollToUnitAnchor(anchorId: string): void {
  if (typeof window === "undefined") return;
  cancelPending?.();

  let timer = 0;
  let cancelled = false;
  const cancel = () => {
    cancelled = true;
    window.clearTimeout(timer);
    window.removeEventListener("wheel", cancel);
    window.removeEventListener("touchstart", cancel);
    window.removeEventListener("keydown", cancel);
    if (cancelPending === cancel) cancelPending = null;
  };
  cancelPending = cancel;
  window.addEventListener("wheel", cancel, { passive: true });
  window.addEventListener("touchstart", cancel, { passive: true });
  window.addEventListener("keydown", cancel);

  const offsetOf = (el: HTMLElement) =>
    el.getBoundingClientRect().top -
    (parseFloat(getComputedStyle(el).scrollMarginTop) || 0);

  // Once the smooth scroll has come to rest, snap back onto the target if it moved.
  const settle = (el: HTMLElement, until: number, previous: number) => {
    timer = window.setTimeout(() => {
      if (cancelled) return;
      const offset = offsetOf(el);
      const resting = Math.abs(offset - previous) < 1;
      if (resting && Math.abs(offset) > DRIFT_PX) {
        el.scrollIntoView({ block: "start", behavior: "auto" });
      }
      if (performance.now() > until) cancel();
      else settle(el, until, offsetOf(el));
    }, TICK_MS);
  };

  const find = (until: number) => {
    if (cancelled) return;
    const el = document.getElementById(anchorId);
    if (el) {
      el.scrollIntoView({ block: "start", behavior: "smooth" });
      settle(el, performance.now() + SETTLE_MS, offsetOf(el));
    } else if (performance.now() > until) {
      cancel();
    } else {
      timer = window.setTimeout(() => find(until), TICK_MS);
    }
  };
  find(performance.now() + WAIT_FOR_ANCHOR_MS);
}
