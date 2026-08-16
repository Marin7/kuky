import { useTranslation } from "react-i18next";
import { localIsoDate } from "@/lib/admin";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

/** Date picker that makes a missing deadline obvious (native empty date looks like a value). */
export function DueOnDateInput({
  value,
  disabled,
  onChange,
  className,
}: {
  value: string;
  disabled?: boolean;
  onChange: (next: string) => void;
  className?: string;
}) {
  const { t } = useTranslation();
  const empty = !value;
  return (
    <div className={cn("flex w-[9.5rem] flex-col gap-0.5", className)}>
      <Input
        type="date"
        min={localIsoDate()}
        disabled={disabled}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={t("admin.homework.dueOn")}
        className={cn(
          "h-8 text-xs",
          empty &&
            !disabled &&
            "border-dashed border-muted-foreground/50 bg-muted/70",
        )}
      />
      {empty && !disabled && (
        <span className="text-[11px] font-semibold leading-tight text-foreground">
          {t("admin.homework.noDueOn")}
        </span>
      )}
    </div>
  );
}
