import { useState } from "react";
import { useTranslation } from "react-i18next";
import { updateInterests, type UserResponse, type ApiError } from "@/lib/auth";
import {
  INTEREST_CODES,
  MAX_INTEREST_SELECTIONS,
  MAX_INTERESTS_NOTE_LENGTH,
  type InterestCode,
} from "@/lib/interests";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";

export function InterestsSetting({
  user,
  onUpdated,
}: {
  user: UserResponse;
  onUpdated: () => void | Promise<void>;
}) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<InterestCode[]>(() =>
    (user.interests ?? []).filter((c): c is InterestCode =>
      (INTEREST_CODES as readonly string[]).includes(c),
    ),
  );
  const [note, setNote] = useState(user.interestsNote ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const toggle = (code: InterestCode, checked: boolean) => {
    setSuccess(false);
    setError(null);
    setSelected((prev) => {
      if (checked) {
        if (prev.includes(code) || prev.length >= MAX_INTEREST_SELECTIONS) {
          return prev;
        }
        return [...prev, code];
      }
      return prev.filter((c) => c !== code);
    });
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    setSuccess(false);
    try {
      await updateInterests({
        interests: selected,
        interestsNote: note.trim() || null,
      });
      await onUpdated();
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (e) {
      const apiErr = e as ApiError;
      setError(apiErr.message || t("account.interestsSaveError"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-3">
      <div>
        <p className="text-xs font-semibold text-muted-foreground uppercase tracking-widest">
          {t("account.interestsTitle")}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">
          {t("account.interestsHint", { max: MAX_INTEREST_SELECTIONS })}
        </p>
      </div>

      <div className="grid grid-cols-2 gap-2">
        {INTEREST_CODES.map((code) => {
          const id = `interest-${code}`;
          const checked = selected.includes(code);
          const atCap = !checked && selected.length >= MAX_INTEREST_SELECTIONS;
          return (
            <label
              key={code}
              htmlFor={id}
              className={`flex items-center gap-2 rounded-md border px-2.5 py-2 text-sm ${
                atCap ? "opacity-50 cursor-not-allowed" : "cursor-pointer"
              }`}
            >
              <Checkbox
                id={id}
                checked={checked}
                disabled={atCap || saving}
                onCheckedChange={(v) => toggle(code, v === true)}
              />
              <span>{t(`interests.${code}`)}</span>
            </label>
          );
        })}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="interestsNote">{t("account.interestsNoteLabel")}</Label>
        <Textarea
          id="interestsNote"
          value={note}
          maxLength={MAX_INTERESTS_NOTE_LENGTH}
          onChange={(e) => {
            setNote(e.target.value);
            setSuccess(false);
            setError(null);
          }}
          disabled={saving}
          rows={3}
          placeholder={t("account.interestsNotePlaceholder")}
        />
        <p className="text-xs text-muted-foreground text-right">
          {note.length}/{MAX_INTERESTS_NOTE_LENGTH}
        </p>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}
      {success && (
        <p className="text-sm text-green-600">{t("account.interestsSaved")}</p>
      )}

      <Button
        type="button"
        onClick={handleSave}
        disabled={saving}
        className="w-full"
      >
        {saving ? t("common.saving") : t("account.interestsSave")}
      </Button>
    </div>
  );
}
