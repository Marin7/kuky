import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { ChevronsUpDown, Plus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { labelGroupKey } from "@/lib/homeworkLabels";

interface Props {
  value: string[];
  onChange: (next: string[]) => void;
  existing: string[];
  compact?: boolean;
  disabled?: boolean;
}

export function HomeworkLabelField({
  value,
  onChange,
  existing,
  compact = false,
  disabled = false,
}: Props) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const addedRef = useRef(false);

  const selectedKeys = new Set(
    value.map((label) => labelGroupKey(label)).filter(Boolean),
  );

  const add = (raw: string) => {
    const trimmed = raw.trim().slice(0, 40);
    const key = labelGroupKey(trimmed);
    if (!key || selectedKeys.has(key)) {
      setSearch("");
      setOpen(false);
      return;
    }
    const canonical =
      existing.find((label) => labelGroupKey(label) === key) ?? trimmed;
    onChange([...value, canonical]);
    addedRef.current = true;
    setSearch("");
    setOpen(false);
  };

  const remove = (label: string) => {
    const key = labelGroupKey(label);
    onChange(value.filter((item) => labelGroupKey(item) !== key));
  };

  const searchKey = labelGroupKey(search);
  const filtered = existing.filter((label) => {
    const key = labelGroupKey(label);
    if (!key || selectedKeys.has(key)) return false;
    if (!searchKey) return true;
    return key.includes(searchKey);
  });
  const searchIsNew =
    Boolean(search.trim()) &&
    !selectedKeys.has(searchKey) &&
    !existing.some((label) => labelGroupKey(label) === searchKey);

  return (
    <div className="flex flex-wrap items-center gap-1">
      {value.map((label) => (
        <span
          key={labelGroupKey(label) ?? label}
          className={[
            "inline-flex items-center gap-0.5 rounded-full bg-slate-100 font-medium text-slate-700",
            compact ? "px-1.5 py-0 text-[11px]" : "px-2 py-0.5 text-xs",
          ].join(" ")}
        >
          {label}
          <button
            type="button"
            disabled={disabled}
            className="rounded-full p-0.5 text-slate-500 hover:bg-slate-200 hover:text-slate-800 disabled:opacity-50"
            aria-label={t("admin.homework.editor.labelRemove", { label })}
            onClick={() => remove(label)}
          >
            <X className={compact ? "h-2.5 w-2.5" : "h-3 w-3"} />
          </button>
        </span>
      ))}
      <Popover
        open={open}
        onOpenChange={(next) => {
          setOpen(next);
          if (!next) {
            if (!addedRef.current && search.trim()) add(search);
            addedRef.current = false;
            setSearch("");
          }
        }}
      >
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="outline"
            role="combobox"
            aria-expanded={open}
            aria-label={t("admin.homework.editor.labelPlaceholder")}
            disabled={disabled}
            className={
              compact
                ? "h-7 w-7 px-0 text-xs font-normal"
                : "h-9 gap-1 px-3 font-normal"
            }
          >
            <Plus className="h-3.5 w-3.5 shrink-0 opacity-60" />
            {compact ? null : (
              <>
                <span className="text-muted-foreground">
                  {t("admin.homework.editor.labelPlaceholder")}
                </span>
                <ChevronsUpDown className="h-3.5 w-3.5 shrink-0 opacity-50" />
              </>
            )}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-56 p-0" align="start">
          <Command shouldFilter={false}>
            <CommandInput
              value={search}
              onValueChange={(next) => setSearch(next.slice(0, 40))}
              placeholder={t("admin.homework.editor.labelSearch")}
            />
            <CommandList>
              <CommandEmpty className="py-3 text-center text-xs text-muted-foreground">
                {t("admin.homework.editor.labelEmpty")}
              </CommandEmpty>
              <CommandGroup>
                {filtered.map((label) => (
                  <CommandItem
                    key={labelGroupKey(label) ?? label}
                    value={`existing-${label}`}
                    onSelect={() => add(label)}
                  >
                    {label}
                  </CommandItem>
                ))}
                {searchIsNew ? (
                  <CommandItem
                    value={`new-${search.trim()}`}
                    onSelect={() => add(search)}
                  >
                    {t("admin.homework.editor.labelUseNew", {
                      label: search.trim(),
                    })}
                  </CommandItem>
                ) : null}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>
    </div>
  );
}
