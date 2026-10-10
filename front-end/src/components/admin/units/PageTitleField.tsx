import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { setUnitPageTitle, type ApiError, type UnitDetail } from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface Props {
  unitId: string;
  page: number;
  title: string;
  onSaved: (detail: UnitDetail) => void;
}

/** The selected page's title (required — every page of a unit has one). */
export function PageTitleField({ unitId, page, title, onSaved }: Props) {
  const { t } = useTranslation();
  const [value, setValue] = useState(title);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setValue(title);
    setError(null);
  }, [title, page]);

  const trimmed = value.trim();

  const save = async () => {
    if (!trimmed) {
      setError(t("admin.units.editor.pageTitleRequired"));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      onSaved(await setUnitPageTitle(unitId, page, trimmed));
    } catch (err) {
      setError(
        (err as ApiError).message ?? t("admin.units.editor.pageTitleError"),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-1">
      <Label htmlFor={`page-title-${page}`}>
        {t("admin.units.editor.pageTitleLabel", { page })}
      </Label>
      <div className="flex gap-2">
        <Input
          id={`page-title-${page}`}
          value={value}
          maxLength={200}
          required
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && void save()}
        />
        <Button
          variant="outline"
          onClick={save}
          disabled={saving || trimmed === title}
        >
          {saving ? t("common.saving") : t("common.save")}
        </Button>
      </div>
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
