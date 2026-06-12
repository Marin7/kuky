import { useEffect, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import {
  getHomework,
  deleteHomework,
  type HomeworkAdminItem,
  type HomeworkType,
  type HomeworkLevel,
} from "@/lib/admin";
import { StudentLink } from "@/components/admin/students/StudentLink";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

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
  GRADED: "Calificada",
};

const TYPE_LABEL: Record<HomeworkType, string> = {
  AUDIO: "Escucha",
  READ: "Lectura",
  WRITE: "Escritura",
  GRAMMAR: "Gramática",
};

const TYPE_CLASS: Record<HomeworkType, string> = {
  AUDIO: "bg-purple-100 text-purple-700",
  READ: "bg-blue-100 text-blue-700",
  WRITE: "bg-yellow-100 text-yellow-700",
  GRAMMAR: "bg-orange-100 text-orange-700",
};

const LEVEL_CLASS: Record<HomeworkLevel, string> = {
  A1: "bg-green-100 text-green-700",
  A2: "bg-green-100 text-green-700",
  B1: "bg-teal-100 text-teal-700",
  B2: "bg-teal-100 text-teal-700",
  C1: "bg-indigo-100 text-indigo-700",
  C2: "bg-indigo-100 text-indigo-700",
};

export function HomeworkAdminList() {
  const navigate = useNavigate();
  const [items, setItems] = useState<HomeworkAdminItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterType, setFilterType] = useState<HomeworkType | "ALL">("ALL");
  const [filterLevel, setFilterLevel] = useState<HomeworkLevel | "ALL">("ALL");

  const load = () => {
    setLoading(true);
    getHomework()
      .then(setItems)
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const openCreate = () => navigate({ to: "/panel/tareas/nueva" });

  const openEdit = (item: HomeworkAdminItem) =>
    navigate({
      to: "/panel/tareas/$homeworkId",
      params: { homeworkId: item.id },
    });

  const remove = async (item: HomeworkAdminItem) => {
    if (!window.confirm(`¿Eliminar la tarea "${item.title}"?`)) return;
    await deleteHomework(item.id);
    load();
  };

  const filtered = items.filter((item) => {
    if (filterType !== "ALL" && item.homeworkType !== filterType) return false;
    if (filterLevel !== "ALL" && item.level !== filterLevel) return false;
    return true;
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <Select
            value={filterType}
            onValueChange={(v) => setFilterType(v as HomeworkType | "ALL")}
          >
            <SelectTrigger className="h-8 w-36 text-xs">
              <SelectValue placeholder="Tipo" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Todos los tipos</SelectItem>
              <SelectItem value="AUDIO">Escucha</SelectItem>
              <SelectItem value="READ">Lectura</SelectItem>
              <SelectItem value="WRITE">Escritura</SelectItem>
              <SelectItem value="GRAMMAR">Gramática</SelectItem>
            </SelectContent>
          </Select>
          <Select
            value={filterLevel}
            onValueChange={(v) => setFilterLevel(v as HomeworkLevel | "ALL")}
          >
            <SelectTrigger className="h-8 w-28 text-xs">
              <SelectValue placeholder="Nivel" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Todos los niveles</SelectItem>
              {(["A1", "A2", "B1", "B2", "C1", "C2"] as HomeworkLevel[]).map(
                (l) => (
                  <SelectItem key={l} value={l}>
                    {l}
                  </SelectItem>
                ),
              )}
            </SelectContent>
          </Select>
        </div>
        <Button size="sm" onClick={openCreate}>
          Nueva tarea
        </Button>
      </div>

      {loading ? (
        <p className="text-sm text-muted-foreground">Cargando…</p>
      ) : filtered.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {items.length === 0
            ? "Aún no has creado ninguna tarea."
            : "No hay tareas con esos filtros."}
        </p>
      ) : (
        filtered.map((item) => (
          <Card key={item.id}>
            <CardHeader className="pb-2">
              <div className="flex items-start justify-between gap-2">
                <div className="flex flex-wrap items-center gap-1.5">
                  <CardTitle className="text-base">{item.title}</CardTitle>
                  {item.homeworkType && (
                    <span
                      className={[
                        "rounded-full px-2 py-0.5 text-xs font-medium",
                        TYPE_CLASS[item.homeworkType],
                      ].join(" ")}
                    >
                      {TYPE_LABEL[item.homeworkType]}
                    </span>
                  )}
                  {item.level && (
                    <span
                      className={[
                        "rounded-full px-2 py-0.5 text-xs font-medium",
                        LEVEL_CLASS[item.level],
                      ].join(" ")}
                    >
                      {item.level}
                    </span>
                  )}
                  {item.format === "EXERCISE" && (
                    <span className="rounded-full bg-pink-100 px-2 py-0.5 text-xs font-medium text-pink-700">
                      Ejercicio
                    </span>
                  )}
                </div>
                <div className="flex shrink-0 gap-1">
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
                        <StudentLink
                          student={{
                            id: a.userId,
                            email: a.email,
                            firstName: a.firstName,
                            lastName: a.lastName,
                            username: a.username,
                          }}
                          showEmail
                        />
                        <span
                          className={[
                            "rounded-full px-2 py-0.5 text-xs font-medium",
                            a.status === "SUBMITTED" ||
                            a.status === "REVIEWED" ||
                            a.status === "GRADED"
                              ? "bg-green-100 text-green-700"
                              : "bg-muted text-muted-foreground",
                          ].join(" ")}
                        >
                          {STATUS_LABEL[a.status] ?? a.status}
                          {a.status === "GRADED" &&
                            a.scorePercent !== null &&
                            ` — ${a.scorePercent}%`}
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
                        <span className="font-medium">
                          <StudentLink
                            student={{
                              id: a.userId,
                              email: a.email,
                              firstName: a.firstName,
                              lastName: a.lastName,
                              username: a.username,
                            }}
                          />
                          :
                        </span>{" "}
                        {a.responseText}
                      </div>
                    ))}
                </div>
              )}
            </CardContent>
          </Card>
        ))
      )}
    </div>
  );
}
