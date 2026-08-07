import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "@tanstack/react-router";
import {
  getUnit,
  listPresentations,
  getHomework,
  setUnitPresentations,
  setUnitHomeworks,
  uploadPresentationFile,
  deletePresentationFile,
  setPresentationLevel,
  reorderUnitContents,
  type UnitDetail,
  type UnitContentItem,
  type PresentationSummary,
  type HomeworkAdminItem,
  type HomeworkLevel,
  type ApiError,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { LEVELS } from "./UnitsTab";
import { AddContentCombobox } from "./AddContentCombobox";
import { UnitContentSortableList } from "./UnitContentSortableList";

interface Props {
  unitId: string;
  onUpdated: (detail: UnitDetail) => void;
}

function presentationIds(detail: UnitDetail): string[] {
  return detail.contents
    .filter((c) => c.type === "PRESENTATION" && c.presentation)
    .map((c) => c.presentation!.id.toString());
}

function homeworkIds(detail: UnitDetail): string[] {
  return detail.contents
    .filter((c) => c.type === "HOMEWORK" && c.homework)
    .map((c) => c.homework!.id.toString());
}

export function UnitContentPicker({ unitId, onUpdated }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<UnitDetail | null>(null);
  const [allPresentations, setAllPresentations] = useState<
    PresentationSummary[]
  >([]);
  const [allHomeworks, setAllHomeworks] = useState<HomeworkAdminItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    Promise.all([getUnit(unitId), listPresentations(), getHomework()])
      .then(([d, p, h]) => {
        setDetail(d);
        setAllPresentations(p);
        setAllHomeworks(h);
        onUpdated(d);
      })
      .catch(() => setError(t("admin.units.loadError")))
      .finally(() => setLoading(false));
  };

  useEffect(load, [unitId]);

  if (loading)
    return (
      <p className="text-xs text-muted-foreground">
        {t("admin.units.loading")}
      </p>
    );
  if (!detail) return <p className="text-xs text-destructive">{error}</p>;

  const unitPresentationIds = new Set(presentationIds(detail));
  const unitHomeworkIds = new Set(homeworkIds(detail));

  const availablePresentations = allPresentations.filter(
    (p) => !unitPresentationIds.has(String(p.id)),
  );
  const availableHomeworks = allHomeworks.filter(
    (h) => !unitHomeworkIds.has(String(h.id)),
  );
  const detachPresentation = async (pid: string) => {
    try {
      const updated = await setUnitPresentations(
        unitId,
        presentationIds(detail).filter((id) => id !== pid),
      );
      setDetail(updated);
      onUpdated(updated);
    } catch {
      setError(t("admin.units.contents.detachError"));
    }
  };

  const attachPresentation = async (pid: string) => {
    try {
      const updated = await setUnitPresentations(unitId, [
        ...presentationIds(detail),
        pid,
      ]);
      setDetail(updated);
      onUpdated(updated);
    } catch {
      setError(t("admin.units.contents.detachError"));
    }
  };

  const detachHomework = async (hid: string) => {
    try {
      const updated = await setUnitHomeworks(
        unitId,
        homeworkIds(detail).filter((id) => id !== hid),
      );
      setDetail(updated);
      onUpdated(updated);
    } catch {
      setError(t("admin.units.contents.detachError"));
    }
  };

  const attachHomework = async (hid: string) => {
    try {
      const updated = await setUnitHomeworks(unitId, [
        ...homeworkIds(detail),
        hid,
      ]);
      setDetail(updated);
      onUpdated(updated);
    } catch {
      setError(t("admin.units.contents.detachError"));
    }
  };

  const handleReorder = async (items: UnitContentItem[]) => {
    try {
      const updated = await reorderUnitContents(
        unitId,
        items.map((c) => ({
          type: c.type,
          id:
            c.type === "PRESENTATION"
              ? c.presentation!.id.toString()
              : c.homework!.id.toString(),
        })),
      );
      setDetail(updated);
      onUpdated(updated);
    } catch {
      setError(t("admin.units.contents.reorderError"));
      load();
    }
  };

  const patchPresentation = (updated: PresentationSummary) => {
    setDetail((prev) =>
      prev
        ? {
            ...prev,
            contents: prev.contents.map((c) =>
              c.type === "PRESENTATION" &&
              c.presentation?.id === updated.id
                ? { ...c, presentation: updated }
                : c,
            ),
          }
        : prev,
    );
  };

  return (
    <div className="space-y-4 border-t pt-4">
      {error && <p className="text-xs text-destructive">{error}</p>}

      <div>
        <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          {t("admin.units.contents.sequence")}
        </p>

        {detail.contents.length === 0 ? (
          <p className="text-xs text-muted-foreground">
            {t("admin.units.contents.empty")}
          </p>
        ) : (
          <UnitContentSortableList
            items={detail.contents}
            onReorder={handleReorder}
            moveUpLabel={t("admin.units.contents.moveUp")}
            moveDownLabel={t("admin.units.contents.moveDown")}
            renderItem={(item) =>
              item.type === "PRESENTATION" && item.presentation ? (
                <PresentationRow
                  presentation={item.presentation}
                  typeLabel={t("admin.units.contents.typePresentation")}
                  onDetach={() =>
                    detachPresentation(item.presentation!.id.toString())
                  }
                  onUpdated={patchPresentation}
                />
              ) : item.homework ? (
                <HomeworkRow
                  homework={item.homework}
                  typeLabel={t("admin.units.contents.typeHomework")}
                  onDetach={() =>
                    detachHomework(item.homework!.id.toString())
                  }
                  onEditClick={() =>
                    navigate({
                      to: "/panel/tareas/$homeworkId",
                      params: {
                        homeworkId: item.homework!.id.toString(),
                      },
                    })
                  }
                />
              ) : null
            }
          />
        )}

        <div className="mt-3 flex flex-wrap gap-2">
          {availablePresentations.length > 0 && (
            <AddContentCombobox
              triggerLabel={t("admin.units.contents.addPresentation")}
              searchPlaceholder={t("admin.units.contents.searchPresentations")}
              emptyLabel={t("admin.units.contents.noMatches")}
              options={availablePresentations.map((p) => ({
                id: String(p.id),
                title: p.title,
                level: p.level,
              }))}
              onSelect={attachPresentation}
            />
          )}
          {availableHomeworks.length > 0 && (
            <AddContentCombobox
              triggerLabel={t("admin.units.contents.addHomework")}
              searchPlaceholder={t("admin.units.contents.searchHomeworks")}
              emptyLabel={t("admin.units.contents.noMatches")}
              options={availableHomeworks.map((h) => ({
                id: String(h.id),
                title: h.title,
                level: h.level,
              }))}
              onSelect={attachHomework}
            />
          )}
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------

interface PresentationRowProps {
  presentation: PresentationSummary;
  typeLabel: string;
  onDetach: () => void;
  onUpdated: (updated: PresentationSummary) => void;
}

function PresentationRow({
  presentation: p,
  typeLabel,
  onDetach,
  onUpdated,
}: PresentationRowProps) {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [rowError, setRowError] = useState<string | null>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";
    setUploading(true);
    setRowError(null);
    try {
      const detail = await uploadPresentationFile(p.id, file);
      onUpdated({
        ...p,
        files: detail.files,
      });
    } catch (err) {
      setRowError(
        (err as ApiError).message ?? t("admin.units.contents.uploadError"),
      );
    } finally {
      setUploading(false);
    }
  };

  const handleDeleteFile = async (fileId: string) => {
    try {
      await deletePresentationFile(p.id, fileId);
      onUpdated({
        ...p,
        files: p.files.filter((f) => f.id !== fileId),
      });
    } catch {
      setRowError(t("admin.units.contents.deleteFileError"));
    }
  };

  const handleLevelChange = async (value: string) => {
    const level = value === "NONE" ? null : (value as HomeworkLevel);
    try {
      await setPresentationLevel(p.id, level);
      onUpdated({ ...p, level });
    } catch {
      setRowError(t("admin.units.contents.levelError"));
    }
  };

  return (
    <div className="flex flex-wrap items-center gap-2 px-2 py-1.5 text-xs">
      <input
        type="file"
        accept=".pptx,.pdf,application/pdf,application/vnd.openxmlformats-officedocument.presentationml.presentation"
        className="hidden"
        ref={fileInputRef}
        onChange={handleFileChange}
      />
      <span className="rounded bg-background px-1.5 py-0.5 text-[10px] uppercase text-muted-foreground">
        {typeLabel}
      </span>
      <span className="flex-1 truncate font-medium">{p.title}</span>

      <Select value={p.level ?? "NONE"} onValueChange={handleLevelChange}>
        <SelectTrigger className="h-6 w-20 text-xs">
          <SelectValue placeholder={t("admin.units.contents.noLevel")} />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="NONE">
            {t("admin.units.contents.noLevel")}
          </SelectItem>
          {LEVELS.map((l) => (
            <SelectItem key={l} value={l}>
              {l}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {p.files.length > 0 ? (
        <div className="flex w-full flex-wrap items-center gap-1">
          {p.files.map((f) => (
            <span
              key={f.id}
              className="inline-flex items-center gap-1 text-muted-foreground"
            >
              <span className="max-w-[10rem] truncate">📎 {f.displayName}</span>
              <Button
                variant="ghost"
                size="sm"
                className="h-6 px-1 text-xs text-destructive"
                onClick={() => handleDeleteFile(f.id)}
                disabled={uploading}
              >
                {t("admin.units.contents.deleteFile")}
              </Button>
            </span>
          ))}
          <Button
            variant="ghost"
            size="sm"
            className="h-6 px-1 text-xs"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading || p.files.length >= 10}
          >
            {t("admin.units.contents.uploadFile")}
          </Button>
        </div>
      ) : (
        <Button
          variant="outline"
          size="sm"
          className="h-6 px-1 text-xs"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
        >
          {uploading
            ? t("admin.units.contents.uploading")
            : t("admin.units.contents.uploadFile")}
        </Button>
      )}

      <Button
        variant="ghost"
        size="sm"
        className="h-6 px-1 text-xs text-destructive"
        onClick={onDetach}
      >
        {t("admin.units.contents.detach")}
      </Button>

      {rowError && <p className="w-full text-destructive">{rowError}</p>}
    </div>
  );
}

// ---------------------------------------------------------------------------

interface HomeworkRowProps {
  homework: HomeworkAdminItem;
  typeLabel: string;
  onDetach: () => void;
  onEditClick: () => void;
}

function HomeworkRow({
  homework: h,
  typeLabel,
  onDetach,
  onEditClick,
}: HomeworkRowProps) {
  const { t } = useTranslation();

  return (
    <div className="flex flex-wrap items-center gap-2 px-2 py-1.5 text-xs">
      <span className="rounded bg-background px-1.5 py-0.5 text-[10px] uppercase text-muted-foreground">
        {typeLabel}
      </span>
      <span className="flex-1 truncate font-medium">{h.title}</span>
      <Button
        variant="ghost"
        size="sm"
        className="h-6 px-1 text-xs"
        onClick={onEditClick}
      >
        {t("admin.units.contents.editHomework")}
      </Button>
      <Button
        variant="ghost"
        size="sm"
        className="h-6 px-1 text-xs text-destructive"
        onClick={onDetach}
      >
        {t("admin.units.contents.detach")}
      </Button>
    </div>
  );
}
