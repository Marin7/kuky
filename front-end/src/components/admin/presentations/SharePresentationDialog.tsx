import { useState } from "react";
import { useTranslation } from "react-i18next";
import { setShares, type PresentationDetail, type ApiError } from "@/lib/admin";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { StudentMultiSelect } from "@/components/admin/homework/StudentMultiSelect";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  deck: PresentationDetail;
  onShared: (updated: PresentationDetail) => void;
}

export function SharePresentationDialog({
  open,
  onOpenChange,
  deck,
  onShared,
}: Props) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<string[]>(
    deck.sharedWith.map((s) => s.id),
  );
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      const updated = await setShares(deck.id, selected);
      onShared(updated);
      onOpenChange(false);
    } catch (e) {
      setError(
        (e as ApiError).message ?? t("admin.presentations.shareDialog.error"),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Compartir «{deck.title}»</DialogTitle>
        </DialogHeader>
        <p className="text-sm text-muted-foreground">
          {t("admin.presentations.shareDialog.description")}
        </p>
        <StudentMultiSelect selected={selected} onChange={setSelected} />
        {error && <p className="text-sm text-destructive">{error}</p>}
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t("admin.presentations.shareDialog.cancel")}
          </Button>
          <Button onClick={save} disabled={saving}>
            {saving
              ? t("admin.presentations.shareDialog.sharing")
              : t("admin.presentations.shareDialog.share")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
