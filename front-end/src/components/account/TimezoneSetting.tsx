import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useTimezone } from "@/hooks/useTimezone";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const ALL_ZONES: string[] =
  typeof Intl.supportedValuesOf === "function"
    ? Intl.supportedValuesOf("timeZone")
    : [];

export function TimezoneSetting() {
  const { t } = useTranslation();
  const { zone, isManual, isFallback, setZone, clearOverride } = useTimezone();
  const [saving, setSaving] = useState(false);

  const handleChange = async (newZone: string) => {
    setSaving(true);
    try {
      await setZone(newZone);
    } finally {
      setSaving(false);
    }
  };

  const handleUseDevice = async () => {
    setSaving(true);
    try {
      await clearOverride();
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-1.5">
      <Label htmlFor="timezone">{t("account.timezoneLabel")}</Label>
      <div className="flex items-center gap-2">
        <Select value={zone} onValueChange={handleChange} disabled={saving}>
          <SelectTrigger id="timezone" className="flex-1">
            <SelectValue />
          </SelectTrigger>
          <SelectContent className="max-h-72">
            {(ALL_ZONES.includes(zone) ? ALL_ZONES : [zone, ...ALL_ZONES]).map(
              (z) => (
                <SelectItem key={z} value={z}>
                  {z}
                </SelectItem>
              ),
            )}
          </SelectContent>
        </Select>
        {isManual && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleUseDevice}
            disabled={saving}
          >
            {t("account.timezoneUseDevice")}
          </Button>
        )}
      </div>
      <p className="text-xs text-muted-foreground">
        {isFallback
          ? t("account.timezoneFallbackNote")
          : isManual
            ? t("account.timezoneManualNote")
            : t("account.timezoneAutoNote")}
      </p>
    </div>
  );
}
