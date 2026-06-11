import { useEffect, useState } from "react";
import {
  getHomework,
  deleteHomework,
  type HomeworkAdminItem,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { HomeworkEditorDialog } from "./HomeworkEditorDialog";

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${iso}T00:00:00`));
}

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Pendiente",
  SUBMITTED: "Entregada",
  REVIEWED: "Revisada",
};

export function HomeworkAdminList() {
  const [items, setItems] = useState<HomeworkAdminItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<HomeworkAdminItem | null>(null);

  const load = () => {
    setLoading(true);
    getHomework()
      .then(setItems)
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  const openEdit = (item: HomeworkAdminItem) => {
    setEditing(item);
    setDialogOpen(true);
  };

  const remove = async (item: HomeworkAdminItem) => {
    if (!window.confirm(`¿Eliminar la tarea "${item.title}"?`)) return;
    await deleteHomework(item.id);
    load();
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Crea tareas y asígnalas a tus alumnos.
        </p>
        <Button size="sm" onClick={openCreate}>
          Nueva tarea
        </Button>
      </div>

      {loading ? (
        <p className="text-sm text-muted-foreground">Cargando…</p>
      ) : items.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Aún no has creado ninguna tarea.
        </p>
      ) : (
        items.map((item) => (
          <Card key={item.id}>
            <CardHeader className="pb-2">
              <div className="flex items-start justify-between gap-2">
                <CardTitle className="text-base">{item.title}</CardTitle>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => openEdit(item)}
                  >
                    Editar
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs text-destructive"
                    onClick={() => remove(item)}
                  >
                    Eliminar
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <p className="whitespace-pre-wrap text-muted-foreground">
                {item.instructions}
              </p>
              {item.dueOn && (
                <p className="text-xs text-muted-foreground">
                  Fecha límite: {formatDate(item.dueOn)}
                </p>
              )}
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Asignada a
                </p>
                {item.assignees.length === 0 ? (
                  <p className="text-xs text-muted-foreground">
                    Sin asignar (borrador).
                  </p>
                ) : (
                  <ul className="mt-1 space-y-1">
                    {item.assignees.map((a) => (
                      <li
                        key={a.userId}
                        className="flex items-center justify-between"
                      >
                        <span>{a.email}</span>
                        <span
                          className={[
                            "rounded-full px-2 py-0.5 text-xs font-medium",
                            a.status === "SUBMITTED" || a.status === "REVIEWED"
                              ? "bg-green-100 text-green-700"
                              : "bg-muted text-muted-foreground",
                          ].join(" ")}
                        >
                          {STATUS_LABEL[a.status] ?? a.status}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              {item.assignees.some((a) => a.responseText) && (
                <div className="space-y-1">
                  <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Respuestas
                  </p>
                  {item.assignees
                    .filter((a) => a.responseText)
                    .map((a) => (
                      <div
                        key={a.userId}
                        className="rounded-md bg-muted/50 p-2 text-xs"
                      >
                        <span className="font-medium">{a.email}:</span>{" "}
                        {a.responseText}
                      </div>
                    ))}
                </div>
              )}
            </CardContent>
          </Card>
        ))
      )}

      {dialogOpen && (
        <HomeworkEditorDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          existing={editing}
          onSaved={load}
        />
      )}
    </div>
  );
}
