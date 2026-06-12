import { useEffect, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import {
  createHomework,
  updateHomework,
  setAssignees,
  getHomeworkById,
  type AdminQuestion,
  type HomeworkType,
  type HomeworkLevel,
  type HomeworkFormat,
  type ApiError,
} from "@/lib/admin";
import { getMe } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { StudentMultiSelect } from "./StudentMultiSelect";
import { QuestionListEditor } from "./QuestionListEditor";

const TYPE_OPTIONS: { value: HomeworkType; label: string }[] = [
  { value: "AUDIO", label: "Escucha" },
  { value: "READ", label: "Lectura" },
  { value: "WRITE", label: "Escritura" },
  { value: "GRAMMAR", label: "Gramática" },
];

const LEVEL_OPTIONS: HomeworkLevel[] = ["A1", "A2", "B1", "B2", "C1", "C2"];

interface Props {
  homeworkId?: string; // undefined ⇒ create
}

export function HomeworkEditorPage({ homeworkId }: Props) {
  const navigate = useNavigate();
  const isEdit = Boolean(homeworkId);

  const [authChecked, setAuthChecked] = useState(false);
  const [loading, setLoading] = useState(isEdit);

  const [title, setTitle] = useState("");
  const [instructions, setInstructions] = useState("");
  const [dueOn, setDueOn] = useState("");
  const [homeworkType, setHomeworkType] = useState<HomeworkType | "">("");
  const [level, setLevel] = useState<HomeworkLevel | "">("");
  const [format, setFormat] = useState<HomeworkFormat>("MANUAL");
  const [questions, setQuestions] = useState<AdminQuestion[]>([]);
  const [assigneeIds, setAssigneeIds] = useState<string[]>([]);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Admin gate.
  useEffect(() => {
    getMe()
      .then((me) => {
        if (me.role !== "ADMIN") navigate({ to: "/" });
        else setAuthChecked(true);
      })
      .catch(() => navigate({ to: "/cuenta" }));
  }, []);

  // Load existing homework when editing.
  useEffect(() => {
    if (!homeworkId) return;
    getHomeworkById(homeworkId)
      .then((hw) => {
        setTitle(hw.title);
        setInstructions(hw.instructions);
        setDueOn(hw.dueOn ?? "");
        setHomeworkType(hw.homeworkType ?? "");
        setLevel(hw.level ?? "");
        setFormat(hw.format);
        setQuestions(hw.questions ?? []);
        setAssigneeIds(hw.assignees.map((a) => a.userId));
      })
      .catch(() => setError("No se pudo cargar la tarea."))
      .finally(() => setLoading(false));
  }, [homeworkId]);

  const backToList = () =>
    navigate({ to: "/panel", search: { tab: "homework" } as never });

  const save = async () => {
    if (!title.trim() || !instructions.trim()) {
      setError("El título y las instrucciones son obligatorios.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const due = dueOn ? dueOn : null;
      const type = homeworkType || null;
      const lvl = level || null;
      const qs = format === "EXERCISE" ? questions : [];
      if (homeworkId) {
        await updateHomework(
          homeworkId,
          title,
          instructions,
          due,
          type,
          lvl,
          format,
          qs,
        );
        await setAssignees(homeworkId, assigneeIds);
      } else {
        await createHomework(
          title,
          instructions,
          due,
          type,
          lvl,
          format,
          qs,
          assigneeIds,
        );
      }
      backToList();
    } catch (e) {
      setError((e as ApiError).message ?? "No se pudo guardar la tarea.");
    } finally {
      setSaving(false);
    }
  };

  if (!authChecked) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <p className="animate-pulse text-sm text-muted-foreground">Cargando…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-6 py-12">
      <button
        type="button"
        onClick={backToList}
        className="mb-8 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        ← Volver al panel
      </button>

      <h1 className="font-display text-3xl font-semibold text-primary">
        {isEdit ? "Editar tarea" : "Nueva tarea"}
      </h1>

      {loading ? (
        <p className="mt-8 animate-pulse text-sm text-muted-foreground">
          Cargando tarea…
        </p>
      ) : (
        <div className="mt-8 space-y-6">
          <div className="space-y-1">
            <Label htmlFor="hw-title">Título</Label>
            <Input
              id="hw-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={200}
            />
          </div>

          <div className="space-y-1">
            <Label htmlFor="hw-instructions">Instrucciones</Label>
            <Textarea
              id="hw-instructions"
              value={instructions}
              onChange={(e) => setInstructions(e.target.value)}
              rows={4}
              maxLength={5000}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="space-y-1">
              <Label>Tipo</Label>
              <Select
                value={homeworkType}
                onValueChange={(v) => setHomeworkType(v as HomeworkType)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Sin tipo" />
                </SelectTrigger>
                <SelectContent>
                  {TYPE_OPTIONS.map((o) => (
                    <SelectItem key={o.value} value={o.value}>
                      {o.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Nivel</Label>
              <Select
                value={level}
                onValueChange={(v) => setLevel(v as HomeworkLevel)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Sin nivel" />
                </SelectTrigger>
                <SelectContent>
                  {LEVEL_OPTIONS.map((l) => (
                    <SelectItem key={l} value={l}>
                      {l}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="hw-due">Fecha límite (opcional)</Label>
              <Input
                id="hw-due"
                type="date"
                value={dueOn}
                onChange={(e) => setDueOn(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label>Formato</Label>
            <RadioGroup
              value={format}
              onValueChange={(v) => setFormat(v as HomeworkFormat)}
              className="flex flex-col gap-2"
            >
              <label className="flex items-center gap-2 text-sm">
                <RadioGroupItem value="MANUAL" id="fmt-manual" />
                Manual — el alumno escribe una respuesta libre que tú revisas.
              </label>
              <label className="flex items-center gap-2 text-sm">
                <RadioGroupItem value="EXERCISE" id="fmt-exercise" />
                Ejercicio autocorregible — preguntas con corrección automática.
              </label>
            </RadioGroup>
          </div>

          {format === "EXERCISE" && (
            <div className="rounded-lg border bg-muted/30 p-4">
              <QuestionListEditor
                questions={questions}
                onChange={setQuestions}
              />
            </div>
          )}

          <div className="space-y-1">
            <Label>Asignar a</Label>
            <StudentMultiSelect
              selected={assigneeIds}
              onChange={setAssigneeIds}
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={backToList} disabled={saving}>
              Cancelar
            </Button>
            <Button onClick={save} disabled={saving}>
              {saving ? "Guardando…" : "Guardar"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
