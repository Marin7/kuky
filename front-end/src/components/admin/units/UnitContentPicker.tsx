import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "@tanstack/react-router";
import {
  getUnit,
  listPresentations,
  getHomework,
  setUnitPresentations,
  setUnitHomeworks,
  setAssignees,
  uploadPresentationFile,
  deletePresentationFile,
  setPresentationLevel,
  type UnitDetail,
  type PresentationSummary,
  type HomeworkAdminItem,
  type HomeworkLevel,
  type ApiError,
} from "@/lib/admin";
import { StudentLink } from "@/components/admin/students/StudentLink";
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

interface Props {
  unitId: string;
  onUpdated: (detail: UnitDetail) => void;
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

  const unitPresentationIds = new Set(
    detail.presentations.map((p) => p.id.toString()),
  );
  const unitHomeworkIds = new Set(detail.homeworks.map((h) => h.id.toString()));

  const detachPresentation = async (pid: string) => {
    const newIds = detail.presentations
      .map((p) => p.id.toString())
      .filter((id) => id !== pid);
    try {
      const updated = await setUnitPresentations(unitId, newIds);
      setDetail(updated);
      onUpdated(updated);
    } catch {
      setError(t("admin.units.contents.detachError"));
    }
  };

  const attachPresentation = async (pid: string) => {
    const newIds = [...detail.presentations.map((p) => p.id.toString()), pid];
    try {
      const updated = await setUnitPresentations(unitId, newIds);
      setDetail(updated);
      onUpdated(updated);
    } catch {
      setError(t("admin.units.contents.detachError"));
    }
  };

  const detachHomework = async (hid: string) => {
    const newIds = detail.homeworks
      .map((h) => h.id.toString())
      .filter((id) => id !== hid);
    try {
      const updated = await setUnitHomeworks(unitId, newIds);
      setDetail(updated);
      onUpdated(updated);
    } catch {
      setError(t("admin.units.contents.detachError"));
    }
  };

  const attachHomework = async (hid: string) => {
    const newIds = [...detail.homeworks.map((h) => h.id.toString()), hid];
    try {
      const updated = await setUnitHomeworks(unitId, newIds);
      setDetail(updated);
      onUpdated(updated);
    } catch {
      setError(t("admin.units.contents.detachError"));
    }
  };

  const availablePresentations = allPresentations.filter(
    (p) => !unitPresentationIds.has(p.id),
  );
  const availableHomeworks = allHomeworks.filter(
    (h) => !unitHomeworkIds.has(h.id.toString()),
  );

  return (
    <div className="space-y-4 border-t pt-4">
      {error && <p className="text-xs text-destructive">{error}</p>}

      {/* Presentations section */}
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground mb-2">
          {t("admin.units.contents.presentations")}
        </p>

        {detail.presentations.length === 0 ? (
          <p className="text-xs text-muted-foreground">
            {t("admin.units.contents.emptyPresentations")}
          </p>
        ) : (
          <div className="space-y-2">
            {detail.presentations.map((p) => (
              <PresentationRow
                key={p.id.toString()}
                presentation={p}
                onDetach={() => detachPresentation(p.id.toString())}
                onUpdated={(updated) => {
                  setDetail((prev) =>
                    prev
                      ? {
                          ...prev,
                          presentations: prev.presentations.map((pp) =>
                            pp.id === updated.id ? updated : pp,
                          ),
                        }
                      : prev,
                  );
                }}
              />
            ))}
          </div>
        )}

        {availablePresentations.length > 0 && (
          <div className="mt-2">
            <AddContentCombobox
              triggerLabel={t("admin.units.contents.addPresentation")}
              searchPlaceholder={t("admin.units.contents.searchPresentations")}
              emptyLabel={t("admin.units.contents.noMatches")}
              options={availablePresentations.map((p) => ({
                id: p.id,
                title: p.title,
                level: p.level,
              }))}
              onSelect={attachPresentation}
            />
          </div>
        )}
      </div>

      {/* Homework section */}
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground mb-2">
          {t("admin.units.contents.homeworks")}
        </p>

        {detail.homeworks.length === 0 ? (
          <p className="text-xs text-muted-foreground">
            {t("admin.units.contents.emptyHomeworks")}
          </p>
        ) : (
          <div className="space-y-2">
            {detail.homeworks.map((h) => (
              <HomeworkRow
                key={h.id.toString()}
                homework={h}
                assignedStudents={detail.assignedStudents}
                onDetach={() => detachHomework(h.id.toString())}
                onEditClick={() =>
                  navigate({
                    to: "/panel/tareas/$homeworkId",
                    params: { homeworkId: h.id.toString() },
                  })
                }
              />
            ))}
          </div>
        )}

        {availableHomeworks.length > 0 && (
          <div className="mt-2">
            <AddContentCombobox
              triggerLabel={t("admin.units.contents.addHomework")}
              searchPlaceholder={t("admin.units.contents.searchHomeworks")}
              emptyLabel={t("admin.units.contents.noMatches")}
              options={availableHomeworks.map((h) => ({
                id: h.id.toString(),
                title: h.title,
                level: h.level,
              }))}
              onSelect={attachHomework}
            />
          </div>
        )}
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------

interface PresentationRowProps {
  presentation: PresentationSummary;
  onDetach: () => void;
  onUpdated: (updated: PresentationSummary) => void;
}

function PresentationRow({
  presentation: p,
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
        hasFile: true,
        originalFileName: detail.originalFileName,
      });
    } catch (err) {
      setRowError(
        (err as ApiError).message ?? t("admin.units.contents.uploadError"),
      );
    } finally {
      setUploading(false);
    }
  };

  const handleDeleteFile = async () => {
    try {
      await deletePresentationFile(p.id);
      onUpdated({ ...p, hasFile: false, originalFileName: null });
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
    <div className="flex flex-wrap items-center gap-2 rounded-md bg-muted/40 px-2 py-1.5 text-xs">
      <input
        type="file"
        accept=".pptx"
        className="hidden"
        ref={fileInputRef}
        onChange={handleFileChange}
      />
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

      {p.hasFile ? (
        <>
          <span className="text-muted-foreground truncate max-w-[12rem]">
            📎 {p.originalFileName}
          </span>
          <Button
            variant="ghost"
            size="sm"
            className="h-6 text-xs px-1"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
          >
            {t("admin.units.contents.uploadFile")}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-6 text-xs px-1 text-destructive"
            onClick={handleDeleteFile}
            disabled={uploading}
          >
            {t("admin.units.contents.deleteFile")}
          </Button>
        </>
      ) : (
        <Button
          variant="outline"
          size="sm"
          className="h-6 text-xs px-1"
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
        className="h-6 text-xs px-1 text-destructive"
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
  assignedStudents: {
    id: string;
    email: string;
    firstName: string | null;
    lastName: string | null;
    username: string | null;
  }[];
  onDetach: () => void;
  onEditClick: () => void;
}

function HomeworkRow({
  homework: h,
  assignedStudents,
  onDetach,
  onEditClick,
}: HomeworkRowProps) {
  const { t } = useTranslation();
  const [assigning, setAssigning] = useState(false);
  const [rowError, setRowError] = useState<string | null>(null);
  const [currentAssigneeIds, setCurrentAssigneeIds] = useState<string[]>(
    h.assignees.map((a) => a.userId.toString()),
  );

  const toggleStudent = async (studentId: string) => {
    const next = currentAssigneeIds.includes(studentId)
      ? currentAssigneeIds.filter((id) => id !== studentId)
      : [...currentAssigneeIds, studentId];
    setAssigning(true);
    try {
      await setAssignees(h.id.toString(), next);
      setCurrentAssigneeIds(next);
    } catch {
      setRowError(t("admin.units.homework.assignError"));
    } finally {
      setAssigning(false);
    }
  };

  return (
    <div className="rounded-md bg-muted/40 px-2 py-1.5 text-xs space-y-1.5">
      <div className="flex flex-wrap items-center gap-2">
        <span className="flex-1 truncate font-medium">{h.title}</span>
        <Button
          variant="ghost"
          size="sm"
          className="h-6 text-xs px-1"
          onClick={onEditClick}
        >
          {t("admin.units.contents.editHomework")}
        </Button>
        <Button
          variant="ghost"
          size="sm"
          className="h-6 text-xs px-1 text-destructive"
          onClick={onDetach}
        >
          {t("admin.units.contents.detach")}
        </Button>
      </div>

      {/* Per-student assignment toggles */}
      {assignedStudents.length > 0 && (
        <div>
          <p className="text-muted-foreground mb-1">
            {t("admin.units.homework.assignedTo")}:
          </p>
          <div className="flex flex-wrap gap-1">
            {assignedStudents.map((s) => {
              const assigned = currentAssigneeIds.includes(s.id);
              return (
                <button
                  key={s.id}
                  disabled={assigning}
                  onClick={() => toggleStudent(s.id)}
                  className={[
                    "rounded-full px-2 py-0.5 text-xs border transition-colors",
                    assigned
                      ? "bg-primary text-primary-foreground border-primary"
                      : "border-dashed text-muted-foreground hover:border-foreground",
                  ].join(" ")}
                >
                  <StudentLink student={s} />
                </button>
              );
            })}
          </div>
        </div>
      )}

      {rowError && <p className="text-destructive">{rowError}</p>}
    </div>
  );
}
