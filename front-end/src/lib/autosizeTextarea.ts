/** Grow a textarea to fit its content instead of scrolling within a fixed-size box. */
export function autosizeTextarea(el: HTMLTextAreaElement | null) {
  if (!el) return;
  el.style.height = "auto";
  el.style.height = `${el.scrollHeight}px`;
}
