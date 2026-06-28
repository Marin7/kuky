import { useState } from "react";
import { Check, Plus } from "lucide-react";
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
import type { HomeworkLevel } from "@/lib/admin";
import { LEVEL_CLASS } from "./UnitsTab";

export interface ComboboxOption {
  id: string;
  title: string;
  level?: HomeworkLevel | null;
}

interface Props {
  triggerLabel: string;
  searchPlaceholder: string;
  emptyLabel: string;
  options: ComboboxOption[];
  onSelect: (id: string) => void;
}

export function AddContentCombobox({
  triggerLabel,
  searchPlaceholder,
  emptyLabel,
  options,
  onSelect,
}: Props) {
  const [open, setOpen] = useState(false);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" className="h-7 text-xs">
          <Plus className="mr-1 h-3 w-3" />
          {triggerLabel}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-72 p-0" align="start">
        <Command
          filter={(value, search) =>
            value.toLowerCase().includes(search.toLowerCase()) ? 1 : 0
          }
        >
          <CommandInput placeholder={searchPlaceholder} className="text-xs" />
          <CommandList>
            <CommandEmpty className="py-4 text-center text-xs text-muted-foreground">
              {emptyLabel}
            </CommandEmpty>
            <CommandGroup>
              {options.map((o) => (
                <CommandItem
                  key={o.id}
                  value={`${o.title} ${o.level ?? ""}`}
                  onSelect={() => onSelect(o.id)}
                  className="text-xs"
                >
                  <Check className="mr-2 h-3 w-3 opacity-0" />
                  <span className="flex-1 truncate">{o.title}</span>
                  {o.level && (
                    <span
                      className={[
                        "ml-2 rounded-full px-1.5 py-0.5 text-[10px] font-medium",
                        LEVEL_CLASS[o.level],
                      ].join(" ")}
                    >
                      {o.level}
                    </span>
                  )}
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
