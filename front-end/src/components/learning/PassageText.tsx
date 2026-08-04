import { Fragment } from "react";

/**
 * Renders authored passage text with explicit line breaks.
 * Prefer this over CSS whitespace-pre-wrap when the text sits beside
 * inline-block blank controls — browsers often mishandle newlines there.
 */
export function PassageText({ text }: { text: string }) {
  const lines = text.split(/\r\n|\n|\r/);
  return (
    <>
      {lines.map((line, i) => (
        <Fragment key={i}>
          {i > 0 && <br />}
          {line}
        </Fragment>
      ))}
    </>
  );
}
