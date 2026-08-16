import {
  useRef,
  useState,
  type ComponentProps,
  type MouseEvent,
  type PointerEvent,
} from "react";
import { useTranslation } from "react-i18next";
import { Smile } from "lucide-react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Textarea } from "@/components/ui/textarea";
import { CLASSROOM_EMOJIS, insertEmojiIntoTextarea } from "./classroomEmojis";

/** Keep a sibling textarea's selection when opening the grid. */
function keepSelection(e: PointerEvent | MouseEvent) {
  e.preventDefault();
}

interface PickerProps {
  disabled?: boolean;
  onInsert: (emoji: string) => void;
}

/** Clicks on a portaled emoji popover must not count as “outside” the review dialog. */
export function preventDialogDismissForPopover(event: {
  target: EventTarget | null;
  preventDefault: () => void;
}) {
  const node = event.target;
  if (
    node instanceof Element &&
    node.closest("[data-radix-popper-content-wrapper]")
  ) {
    event.preventDefault();
  }
}

export function ClassroomEmojiPicker({ disabled, onInsert }: PickerProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  return (
    <Popover
      modal
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) return;
        requestAnimationFrame(() => {
          const dialog = document.querySelector(
            '[role="dialog"]',
          ) as HTMLElement | null;
          if (dialog) dialog.style.pointerEvents = "auto";
        });
      }}
    >
      <PopoverTrigger asChild>
        <button
          type="button"
          disabled={disabled}
          onPointerDown={keepSelection}
          onMouseDown={keepSelection}
          title={t("richText.emoji.open")}
          aria-label={t("richText.emoji.open")}
          aria-expanded={open}
          className="flex h-6 w-6 items-center justify-center rounded border hover:border-foreground/40 disabled:opacity-40"
        >
          <Smile className="h-3.5 w-3.5" />
        </button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className="w-64 p-2 pointer-events-auto"
        onOpenAutoFocus={(e) => e.preventDefault()}
        onCloseAutoFocus={(e) => e.preventDefault()}
      >
        <div className="grid grid-cols-7 gap-0.5">
          {CLASSROOM_EMOJIS.map((emoji) => (
            <button
              key={emoji}
              type="button"
              disabled={disabled}
              onPointerDown={keepSelection}
              onMouseDown={keepSelection}
              onClick={() => {
                onInsert(emoji);
                setOpen(false);
              }}
              title={t("richText.emoji.insert", { emoji })}
              aria-label={t("richText.emoji.insert", { emoji })}
              className="flex h-8 w-8 items-center justify-center rounded text-lg hover:bg-muted disabled:opacity-40"
            >
              {emoji}
            </button>
          ))}
        </div>
      </PopoverContent>
    </Popover>
  );
}

type TextareaProps = Omit<
  ComponentProps<typeof Textarea>,
  "value" | "onChange" | "maxLength"
>;

interface TextareaWithEmojiProps extends TextareaProps {
  value: string;
  onChange: (value: string) => void;
  maxLength: number;
  allowEmojiInsert?: boolean;
}

/** Plain comment / FREE_TEXT field with the classroom picker when enabled. */
export function TextareaWithEmoji({
  value,
  onChange,
  maxLength,
  allowEmojiInsert = false,
  disabled,
  ...textareaProps
}: TextareaWithEmojiProps) {
  const ref = useRef<HTMLTextAreaElement>(null);

  const insert = (emoji: string) => {
    const next = insertEmojiIntoTextarea(ref.current, value, emoji, maxLength);
    onChange(next.text);
    requestAnimationFrame(() => {
      const el = ref.current;
      if (!el) return;
      el.focus();
      el.selectionStart = next.caret;
      el.selectionEnd = next.caret;
    });
  };

  return (
    <div className="space-y-1">
      {allowEmojiInsert && (
        <ClassroomEmojiPicker disabled={disabled} onInsert={insert} />
      )}
      <Textarea
        {...textareaProps}
        ref={ref}
        value={value}
        maxLength={maxLength}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  );
}
