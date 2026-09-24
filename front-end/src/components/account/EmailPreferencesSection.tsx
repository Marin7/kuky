import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getEmailPreferences,
  setEmailPreference,
  type ApiError,
  type EmailPreference,
  type EmailPreferenceType,
} from "@/lib/emailPreferences";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";

/**
 * The signed-in account's email options.
 *
 * Renders one toggle per option the server returns rather than a hardcoded list, so a new
 * option needs only a translation key here — no layout change.
 */
export function EmailPreferencesSection() {
  const { t } = useTranslation();
  const [preferences, setPreferences] = useState<EmailPreference[] | null>(
    null,
  );
  const [saving, setSaving] = useState<EmailPreferenceType | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getEmailPreferences()
      .then((prefs) => {
        if (!cancelled) setPreferences(prefs);
      })
      .catch(() => {
        if (!cancelled) setPreferences([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleToggle = async (type: EmailPreferenceType, enabled: boolean) => {
    setError(null);
    setSaving(type);
    // Optimistic: reverted below if the write fails.
    setPreferences(
      (prev) =>
        prev?.map((p) => (p.type === type ? { ...p, enabled } : p)) ?? prev,
    );
    try {
      await setEmailPreference(type, enabled);
    } catch (err) {
      setPreferences(
        (prev) =>
          prev?.map((p) =>
            p.type === type ? { ...p, enabled: !enabled } : p,
          ) ?? prev,
      );
      setError((err as ApiError).message ?? t("account.emailPrefsSaveError"));
    } finally {
      setSaving(null);
    }
  };

  if (preferences === null || preferences.length === 0) return null;

  return (
    <div className="space-y-3">
      <div className="space-y-1.5">
        <Label>{t("account.emailPrefsTitle")}</Label>
        <p className="text-xs text-muted-foreground">
          {t("account.emailPrefsHint")}
        </p>
      </div>

      {preferences.map((pref) => (
        <div key={pref.type} className="flex items-start justify-between gap-4">
          <div className="space-y-0.5">
            <Label htmlFor={`email-pref-${pref.type}`} className="font-normal">
              {t(`account.emailPrefs${toKeySuffix(pref.type)}`)}
            </Label>
            <p className="text-xs text-muted-foreground">
              {t(`account.emailPrefs${toKeySuffix(pref.type)}Hint`)}
            </p>
          </div>
          <Switch
            id={`email-pref-${pref.type}`}
            checked={pref.enabled}
            disabled={saving === pref.type}
            onCheckedChange={(checked) => handleToggle(pref.type, checked)}
          />
        </div>
      ))}

      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

/** NEW_HOMEWORK_ASSIGNED → NewHomeworkAssigned, to build the translation key. */
function toKeySuffix(type: EmailPreferenceType): string {
  return type
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join("");
}
