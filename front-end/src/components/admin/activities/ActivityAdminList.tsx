import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "@tanstack/react-router";
import {
  listActivities,
  deleteActivity,
  reorderActivities,
  listPresentations,
  type ActivityAdminItem,
  type PresentationSummary,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function ActivityAdminList() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [items, setItems] = useState<ActivityAdminItem[]>([]);
  const [presentations, setPresentations] = useState<PresentationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterPresentation, setFilterPresentation] = useState<string>("ALL");
  const [reordering, setReordering] = useState(false);

  const load = () => {
    setLoading(true);
    Promise.all([
      listActivities(
        filterPresentation === "ALL" ? undefined : filterPresentation,
      ),
      listPresentations(),
    ])
      .then(([acts, pres]) => {
        setItems(acts);
        setPresentations(pres);
      })
      .catch(() => {
        setItems([]);
      })
      .finally(() => setLoading(false));
  };

  useEffect(load, [filterPresentation]);

  const sorted = useMemo(
    () => [...items].sort((a, b) => a.position - b.position),
    [items],
  );

  const canReorder =
    filterPresentation !== "ALL" && sorted.length > 1 && !reordering;

  const openCreate = () => navigate({ to: "/panel/actividades/nueva" });

  const openEdit = (item: ActivityAdminItem) =>
    navigate({
      to: "/panel/actividades/$activityId",
      params: { activityId: item.id },
    });

  const remove = async (item: ActivityAdminItem) => {
    if (!window.confirm(t("admin.activities.deleteConfirm"))) return;
    await deleteActivity(item.id);
    load();
  };

  const move = async (index: number, direction: -1 | 1) => {
    if (filterPresentation === "ALL") return;
    const next = index + direction;
    if (next < 0 || next >= sorted.length) return;
    const ids = sorted.map((a) => a.id);
    const tmp = ids[index];
    ids[index] = ids[next];
    ids[next] = tmp;
    setReordering(true);
    try {
      await reorderActivities(filterPresentation, ids);
      load();
    } catch {
      // keep list; user can retry
    } finally {
      setReordering(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Select
          value={filterPresentation}
          onValueChange={setFilterPresentation}
        >
          <SelectTrigger className="h-8 w-56 text-xs">
            <SelectValue placeholder={t("admin.activities.allPresentations")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">
              {t("admin.activities.allPresentations")}
            </SelectItem>
            {presentations.map((p) => (
              <SelectItem key={p.id} value={p.id}>
                {p.title}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button size="sm" onClick={openCreate}>
          {t("admin.activities.newActivity")}
        </Button>
      </div>

      {loading ? (
        <p className="text-sm text-muted-foreground">
          {t("admin.activities.loading")}
        </p>
      ) : sorted.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("admin.activities.empty")}
        </p>
      ) : (
        sorted.map((item, index) => (
          <Card key={item.id}>
            <CardHeader className="pb-2">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0 space-y-1">
                  <CardTitle className="text-base">{item.title}</CardTitle>
                  <p className="text-xs text-muted-foreground">
                    {item.presentationTitle}
                    {item.triggerPage != null && item.triggerFileId
                      ? ` · ${t("admin.activities.triggerPage")} ${item.triggerPage}`
                      : ""}
                  </p>
                  <div className="flex flex-wrap gap-1">
                    {item.format === "EXERCISE" && (
                      <span className="rounded-full bg-pink-100 px-2 py-0.5 text-xs font-medium text-pink-700">
                        {t("admin.homework.exercise")}
                      </span>
                    )}
                    {item.level && (
                      <span className="rounded-full bg-teal-100 px-2 py-0.5 text-xs font-medium text-teal-700">
                        {item.level}
                      </span>
                    )}
                  </div>
                </div>
                <div className="flex shrink-0 flex-wrap gap-1">
                  {canReorder && (
                    <>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 px-2 text-xs"
                        disabled={index === 0}
                        onClick={() => move(index, -1)}
                        aria-label={t("admin.activities.moveUp")}
                      >
                        ▲
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 px-2 text-xs"
                        disabled={index === sorted.length - 1}
                        onClick={() => move(index, 1)}
                        aria-label={t("admin.activities.moveDown")}
                      >
                        ▼
                      </Button>
                    </>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => openEdit(item)}
                  >
                    {t("admin.activities.edit")}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs text-destructive"
                    onClick={() => remove(item)}
                  >
                    {t("admin.activities.delete")}
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground">
              {item.triggerPage != null
                ? t("admin.activities.triggerSummary", {
                    page: item.triggerPage,
                  })
                : t("admin.activities.noTrigger")}
            </CardContent>
          </Card>
        ))
      )}
    </div>
  );
}
