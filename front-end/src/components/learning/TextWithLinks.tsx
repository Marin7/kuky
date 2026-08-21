import { Fragment } from "react";
import { splitLinkifiedText } from "@/lib/textLinks";

/** Classic hyperlink look: blue + underline (not hover-only). */
const TEXT_LINK_CLASS =
  "text-blue-600 underline underline-offset-2 hover:text-blue-800 visited:text-blue-600";

/** Renders plain text with http(s) and www. URLs as clickable links. */
export function TextWithLinks({ text }: { text: string }) {
  const parts = splitLinkifiedText(text);
  if (parts.length === 0) return null;
  return (
    <>
      {parts.map((part, i) =>
        part.type === "link" ? (
          <a
            key={i}
            href={part.href}
            target="_blank"
            rel="noopener noreferrer"
            className={TEXT_LINK_CLASS}
          >
            {part.text}
          </a>
        ) : (
          <Fragment key={i}>{part.text}</Fragment>
        ),
      )}
    </>
  );
}
