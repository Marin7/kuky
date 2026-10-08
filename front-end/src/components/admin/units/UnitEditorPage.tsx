import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  deleteUnit,
  fetchUnitPdfBlob,
  getHomework,
  getStudents,
  getUnit,
  reorderUnitContents,
  setUnitHomeworks,
  updateUnit,
  type ApiError,
  type HomeworkAdminItem,
  type HomeworkLevel,
  type Student,
  type UnitContentItem,
  type UnitDetail,
} from "@/lib/admin";
import { getMe } from "@/lib/auth";
import { usePdfDocument } from "@/lib/pdf";
import { notifyBadgesChanged } from "@/lib/notifications";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PdfSinglePage } from "@/components/learning/PdfSinglePage";
import { StudentLink } from "@/components/admin/students/StudentLink";
import { LEVELS, LEVEL_CLASS } from "./UnitsTab";
import { AddContentCombobox } from "./AddContentCombobox";
import { UnitContentSortableList } from "./UnitContentSortableList";
import { UnitAssignDialog } from "./UnitAssignDialog";
import { UnitPdfUploader } from "./UnitPdfUploader";
import { PdfThumbnailStrip } from "./PdfThumbnailStrip";
import { PageActivityEditor } from "./PageActivityEditor";
import { UnitActivityProgress } from "./UnitActivityProgress";

interface Props {
  unitId: string;
}

function homeworkIds(detail: UnitDetail): string[] {
  return detail.contents
    .filter((c) => c.type === "HOMEWORK" && c.homework)
    .map((c) => c.homework!.id);
}

/**
 * The whole unit in one view: its PDF (upload / replace, page by page), which pages are
 * activities and their questions, the order of the PDF and the unit's homeworks, students.
 */
export function UnitEditorPage({ unitId }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [authChecked, setAuthChecked] = useState(false);
  const [unit, setUnit] = useState<UnitDetail | null>(null);
  const [allHomeworks, setAllHomeworks] = useState<HomeworkAdminItem[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [selectedPage, setSelectedPage] = useState(1);
  const [progressKey, setProgressKey] = useState(0);
  const [editLevel, setEditLevel] = useState<HomeworkLevel>("A1");
  const [editSubject, setEditSubject] = useState("");
  const [assignOpen, setAssignOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMe()
      .then((me) => {
        if (me.role !== "ADMIN") navigate({ to: "/" });
        else setAuthChecked(true);
      })
      .catch(() => navigate({ to: "/cuenta" }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const applyUnit = (detail: UnitDetail) => {
    setUnit(detail);
    setEditLevel(detail.level);
    setEditSubject(detail.subject);
  };

  const reloadUnit = () =>
    getUnit(unitId)
      .then(applyUnit)
      .catch(() => setError(t("admin.units.loadError")));

  useEffect(() => {
    if (!authChecked) return;
    Promise.all([getUnit(unitId), getHomework(), getStudents()])
      .then(([u, h, s]) => {
        applyUnit(u);
        setAllHomeworks(h);
        setStudents(s);
      })
      .catch(() => setError(t("admin.units.loadError")));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authChecked, unitId]);

  const pdfKey = unit?.pdf?.fileId ?? null;
  const pdfState = usePdfDocument(
    pdfKey ? () => fetchUnitPdfBlob(unitId) : null,
    pdfKey,
  );

  const pageCount = unit?.pdf?.pageCount ?? 0;
  useEffect(() => {
    if (pageCount > 0 && selectedPage > pageCount) setSelectedPage(pageCount);
  }, [pageCount, selectedPage]);

  const activityByPage = useMemo(
    () => new Map((unit?.activities ?? []).map((a) => [a.page, a])),
    [unit?.activities],
  );
  const activityPages = useMemo(
    () => new Set(activityByPage.keys()),
    [activityByPage],
  );
  const unseenPages = useMemo(
    () =>
      new Set(
        (unit?.activities ?? [])
          .filter((a) => a.hasUnseenSubmissions)
          .map((a) => a.page),
      ),
    [unit?.activities],
  );

  const previewRef = useRef<HTMLDivElement>(null);
  const [previewWidth, setPreviewWidth] = useState(0);
  useEffect(() => {
    const el = previewRef.current;
    if (!el) return;
    const update = () => setPreviewWidth(el.clientWidth);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(el);
    return () => observer.disconnect();
  }, [pdfState.status]);

  if (!authChecked || (!unit && !error)) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("admin.units.loading")}
        </p>
      </div>
    );
  }
  if (!unit) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
        <p className="text-sm text-destructive">{error}</p>
      </div>
    );
  }

  const handleSaveHeader = async () => {
    setError(null);
    try {
      applyUnit(await updateUnit(unit.id, editLevel, editSubject.trim()));
    } catch {
      setError(t("admin.units.saveError"));
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(t("admin.units.deleteConfirm"))) return;
    try {
      await deleteUnit(unit.id);
      navigate({ to: "/panel", search: { tab: "units" } as never });
    } catch (err) {
      setError((err as ApiError).message ?? t("admin.units.deleteError"));
    }
  };

  const changeHomeworks = async (ids: string[]) => {
    setError(null);
    try {
      applyUnit(await setUnitHomeworks(unit.id, ids));
    } catch {
      setError(t("admin.units.contents.detachError"));
    }
  };

  const handleReorder = async (items: UnitContentItem[]) => {
    try {
      applyUnit(
        await reorderUnitContents(
          unit.id,
          items.map((c) => ({
            type: c.type,
            id: c.type === "PDF" ? unit.pdf!.presentationId : c.homework!.id,
          })),
        ),
      );
    } catch {
      setError(t("admin.units.contents.reorderError"));
      void reloadUnit();
    }
  };

  const unitHomeworkIds = new Set(homeworkIds(unit));
  const availableHomeworks = allHomeworks.filter(
    (h) => !unitHomeworkIds.has(h.id),
  );

  const onActivitiesChanged = () => {
    void reloadUnit();
    setProgressKey((k) => k + 1);
    notifyBadgesChanged();
  };

  return (
    <div className="mx-auto max-w-5xl space-y-8 px-4 py-8 sm:px-6">
      <Link
        to="/panel"
        search={{ tab: "units" } as never}
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("admin.units.editor.back")}
      </Link>

      {/* Header */}
      <section className="space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <span
            className={[
              "rounded-full px-2.5 py-0.5 text-xs font-semibold",
              LEVEL_CLASS[unit.level],
            ].join(" ")}
          >
            {unit.level}
          </span>
          <h1 className="font-display text-2xl font-semibold text-primary">
            {unit.subject}
          </h1>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Select
            value={editLevel}
            onValueChange={(v) => setEditLevel(v as HomeworkLevel)}
          >
            <SelectTrigger className="h-9 w-24 text-xs">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {LEVELS.map((l) => (
                <SelectItem key={l} value={l}>
                  {l}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Input
            className="h-9 min-w-0 flex-1 text-sm sm:min-w-64"
            value={editSubject}
            maxLength={200}
            onChange={(e) => setEditSubject(e.target.value)}
          />
          <Button
            size="sm"
            variant="outline"
            onClick={handleSaveHeader}
            disabled={
              !editSubject.trim() ||
              (editSubject.trim() === unit.subject && editLevel === unit.level)
            }
          >
            {t("common.save")}
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => setAssignOpen(true)}
          >
            {t("admin.units.assign")}
          </Button>
          <Button
            size="sm"
            variant="ghost"
            className="text-destructive"
            onClick={handleDelete}
          >
            {t("admin.units.delete")}
          </Button>
        </div>
        {unit.assignedStudents.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {unit.assignedStudents.map((s) => (
              <span
                key={s.id}
                className="rounded-full bg-muted px-2 py-0.5 text-xs"
              >
                <StudentLink student={s} />
              </span>
            ))}
          </div>
        )}
        {error && <p className="text-sm text-destructive">{error}</p>}
      </section>

      {/* PDF + page activities */}
      <section className="space-y-4">
        <h2 className="text-lg font-semibold">
          {t("admin.units.editor.pdfTitle")}
        </h2>
        <UnitPdfUploader
          unit={unit}
          onUploaded={(detail) => {
            applyUnit(detail);
            setProgressKey((k) => k + 1);
          }}
        />

        {!unit.pdf ? (
          <p className="text-sm text-muted-foreground">
            {t("admin.units.editor.noPdf")}
          </p>
        ) : pdfState.status === "loading" ? (
          <p className="animate-pulse text-sm text-muted-foreground">
            {t("learning.presentations.viewing")}
          </p>
        ) : pdfState.status === "error" ? (
          <p className="text-sm text-destructive">
            {t("learning.presentations.loadError")}
          </p>
        ) : (
          <>
            <PdfThumbnailStrip
              pdf={pdfState.pdf}
              selectedPage={selectedPage}
              onSelect={setSelectedPage}
              activityPages={activityPages}
              unseenPages={unseenPages}
            />
            {unit.activities.length > 0 && (
              <div className="flex flex-wrap items-center gap-1.5 text-sm">
                <span className="text-muted-foreground">
                  {t("admin.units.editor.activitiesList")}
                </span>
                {unit.activities.map((a) => (
                  <button
                    key={a.id}
                    type="button"
                    onClick={() => setSelectedPage(a.page)}
                    className={[
                      "rounded-full border px-2 py-0.5 text-xs",
                      a.page === selectedPage
                        ? "border-primary bg-primary/10"
                        : "hover:bg-muted",
                    ].join(" ")}
                  >
                    {t("learning.activities.pageShort", { page: a.page })} ·{" "}
                    {a.title ??
                      t("learning.activities.pageTitle", { page: a.page })}
                    {a.awaitingCorrectionCount > 0 &&
                      ` · ${t("admin.units.editor.toCorrect", { count: a.awaitingCorrectionCount })}`}
                  </button>
                ))}
              </div>
            )}
            {/* The selected page at full width; its activity editor sits below it. */}
            <div className="space-y-4">
              <div ref={previewRef} className="min-w-0">
                {previewWidth > 0 && (
                  <PdfSinglePage
                    pdf={pdfState.pdf}
                    pageNumber={selectedPage}
                    width={previewWidth}
                  />
                )}
                <p className="mt-1 text-center text-xs text-muted-foreground">
                  {t("admin.units.editor.pageOf", {
                    page: selectedPage,
                    total: unit.pdf.pageCount,
                  })}
                </p>
              </div>
              <div className="min-w-0">
                <PageActivityEditor
                  unitId={unit.id}
                  page={selectedPage}
                  activity={activityByPage.get(selectedPage)}
                  onChanged={onActivitiesChanged}
                />
              </div>
            </div>
          </>
        )}
      </section>

      {/* Student progress on the activities */}
      {unit.activities.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold">
            {t("admin.units.editor.progressTitle")}
          </h2>
          <UnitActivityProgress
            unit={unit}
            reloadKey={progressKey}
            onChanged={() => {
              void reloadUnit();
              notifyBadgesChanged();
            }}
          />
        </section>
      )}

      {/* Order of the PDF and the unit's homeworks */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold">
          {t("admin.units.contents.sequence")}
        </h2>
        {unit.contents.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("admin.units.contents.empty")}
          </p>
        ) : (
          <UnitContentSortableList
            items={unit.contents}
            onReorder={handleReorder}
            moveUpLabel={t("admin.units.contents.moveUp")}
            moveDownLabel={t("admin.units.contents.moveDown")}
            renderItem={(item) =>
              item.type === "PDF" ? (
                <div className="flex items-center gap-2 px-2 py-1.5 text-xs">
                  <span className="rounded bg-background px-1.5 py-0.5 text-[10px] uppercase text-muted-foreground">
                    {t("admin.units.contents.typePdf")}
                  </span>
                  <span className="flex-1 truncate font-medium">
                    {unit.pdf?.originalName}
                  </span>
                </div>
              ) : item.homework ? (
                <div className="flex flex-wrap items-center gap-2 px-2 py-1.5 text-xs">
                  <span className="rounded bg-background px-1.5 py-0.5 text-[10px] uppercase text-muted-foreground">
                    {t("admin.units.contents.typeHomework")}
                  </span>
                  <span className="flex-1 truncate font-medium">
                    {item.homework.title}
                  </span>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-6 px-1 text-xs"
                    onClick={() =>
                      navigate({
                        to: "/panel/tareas/$homeworkId",
                        params: { homeworkId: item.homework!.id },
                      })
                    }
                  >
                    {t("admin.units.contents.editHomework")}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-6 px-1 text-xs text-destructive"
                    onClick={() =>
                      changeHomeworks(
                        homeworkIds(unit).filter(
                          (id) => id !== item.homework!.id,
                        ),
                      )
                    }
                  >
                    {t("admin.units.contents.detach")}
                  </Button>
                </div>
              ) : null
            }
          />
        )}
        {availableHomeworks.length > 0 && (
          <AddContentCombobox
            triggerLabel={t("admin.units.contents.addHomework")}
            searchPlaceholder={t("admin.units.contents.searchHomeworks")}
            emptyLabel={t("admin.units.contents.noMatches")}
            options={availableHomeworks.map((h) => ({
              id: h.id,
              title: h.title,
              level: h.level,
            }))}
            onSelect={(id) => changeHomeworks([...homeworkIds(unit), id])}
          />
        )}
      </section>

      {assignOpen && (
        <UnitAssignDialog
          open={assignOpen}
          onOpenChange={setAssignOpen}
          unit={{
            ...unit,
            assignedStudentIds: unit.assignedStudents.map((s) => s.id),
          }}
          allStudents={students}
          onAssigned={(detail) => {
            applyUnit(detail);
            setProgressKey((k) => k + 1);
          }}
        />
      )}
    </div>
  );
}
