import { useEffect, useRef, useState } from "react";
import {
  listPresentations,
  createPresentation,
  deletePresentation,
  renamePresentation,
  getPresentation,
  uploadPresentationFile,
  deletePresentationFile,
  setPresentationLevel,
  getStudents,
  studentDisplayName,
  type PresentationSummary,
  type PresentationDetail,
  type HomeworkLevel,
  type Student,
  type ApiError,
} from "@/lib/admin";
import { StudentLink } from "@/components/admin/students/StudentLink";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { SharePresentationDialog } from "./SharePresentationDialog";

const LEVELS: HomeworkLevel[] = ["A1", "A2", "B1", "B2", "C1", "C2"];

const LEVEL_CLASS: Record<HomeworkLevel, string> = {
  A1: "bg-green-100 text-green-700",
  A2: "bg-green-100 text-green-700",
  B1: "bg-teal-100 text-teal-700",
  B2: "bg-teal-100 text-teal-700",
  C1: "bg-indigo-100 text-indigo-700",
  C2: "bg-indigo-100 text-indigo-700",
};

export function PresentationAdminList() {
  const [items, setItems] = useState<PresentationSummary[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(true);
  const [newTitle, setNewTitle] = useState("");
  const [creating, setCreating] = useState(false);
  const [filterLevel, setFilterLevel] = useState<HomeworkLevel | "ALL">("ALL");
  const [filterStudent, setFilterStudent] = useState<string | "ALL">("ALL");

  const load = () => {
    setLoading(true);
    Promise.all([listPresentations(), getStudents()])
      .then(([presentations, studentList]) => {
        setItems(presentations);
        setStudents(studentList);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const create = async () => {
    if (!newTitle.trim()) return;
    setCreating(true);
    try {
      const deck = await createPresentation(newTitle.trim());
      setNewTitle("");
      setItems((prev) => [
        {
          id: deck.id,
          title: deck.title,
          level: null,
          hasFile: false,
          originalFileName: null,
          sharedWithIds: [],
          updatedAt: new Date().toISOString(),
        },
        ...prev,
      ]);
    } finally {
      setCreating(false);
    }
  };

  const handleDeleted = (id: string) =>
    setItems((prev) => prev.filter((p) => p.id !== id));

  const handleUpdated = (updated: PresentationSummary) =>
    setItems((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));

  const filtered = items.filter((item) => {
    if (filterLevel !== "ALL" && item.level !== filterLevel) return false;
    if (filterStudent !== "ALL" && !item.sharedWithIds.includes(filterStudent))
      return false;
    return true;
  });

  return (
    <div className="space-y-4">
      {/* Filters + create row */}
      <div className="flex flex-wrap items-end gap-2">
        <Select
          value={filterLevel}
          onValueChange={(v) => setFilterLevel(v as HomeworkLevel | "ALL")}
        >
          <SelectTrigger className="h-9 w-36 text-xs">
            <SelectValue placeholder="Nivel" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Todos los niveles</SelectItem>
            {LEVELS.map((l) => (
              <SelectItem key={l} value={l}>
                {l}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={filterStudent}
          onValueChange={(v) => setFilterStudent(v)}
        >
          <SelectTrigger className="h-9 w-48 text-xs">
            <SelectValue placeholder="Alumno" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Todos los alumnos</SelectItem>
            {students.map((s) => (
              <SelectItem key={s.id} value={s.id}>
                {studentDisplayName(s)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex flex-1 items-center gap-2 min-w-48">
          <Input
            placeholder="Título de la nueva presentación"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && create()}
            maxLength={200}
          />
          <Button onClick={create} disabled={creating || !newTitle.trim()}>
            {creating ? "Creando…" : "Crear"}
          </Button>
        </div>
      </div>

      {loading ? (
        <p className="text-sm text-muted-foreground">Cargando…</p>
      ) : filtered.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {items.length === 0
            ? "Aún no has creado ninguna presentación."
            : "No hay presentaciones con esos filtros."}
        </p>
      ) : (
        filtered.map((item) => (
          <PresentationCard
            key={item.id}
            item={item}
            students={students}
            onDeleted={() => handleDeleted(item.id)}
            onUpdated={handleUpdated}
          />
        ))
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------

interface CardProps {
  item: PresentationSummary;
  students: Student[];
  onDeleted: () => void;
  onUpdated: (updated: PresentationSummary) => void;
}

function PresentationCard({ item, students, onDeleted, onUpdated }: CardProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [editingTitle, setEditingTitle] = useState(false);
  const [titleValue, setTitleValue] = useState(item.title);
  const [uploading, setUploading] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const [shareDeck, setShareDeck] = useState<PresentationDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  const saveTitle = async () => {
    setEditingTitle(false);
    if (!titleValue.trim() || titleValue === item.title) return;
    try {
      const updated = await renamePresentation(item.id, titleValue.trim());
      onUpdated({ ...item, title: updated.title });
    } catch {
      setTitleValue(item.title);
    }
  };

  const handleLevelChange = async (value: string) => {
    const level = value === "NONE" ? null : (value as HomeworkLevel);
    try {
      await setPresentationLevel(item.id, level);
      onUpdated({ ...item, level });
    } catch (err) {
      setError((err as ApiError).message ?? "No se pudo guardar el nivel.");
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";
    setUploading(true);
    setError(null);
    try {
      const updated = await uploadPresentationFile(item.id, file);
      onUpdated({ ...item, hasFile: true, originalFileName: updated.originalFileName });
    } catch (err) {
      setError((err as ApiError).message ?? "Error al subir el archivo.");
    } finally {
      setUploading(false);
    }
  };

  const handleDeleteFile = async () => {
    setError(null);
    try {
      await deletePresentationFile(item.id);
      onUpdated({ ...item, hasFile: false, originalFileName: null });
    } catch (err) {
      setError((err as ApiError).message ?? "No se pudo eliminar el archivo.");
    }
  };

  const handleShare = async () => {
    try {
      const deck = await getPresentation(item.id);
      setShareDeck(deck);
      setShareOpen(true);
    } catch {
      setError("No se pudo cargar la presentación.");
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(`¿Eliminar «${item.title}»?`)) return;
    try {
      await deletePresentation(item.id);
      onDeleted();
    } catch (err) {
      setError((err as ApiError).message ?? "No se pudo eliminar.");
    }
  };

  return (
    <Card>
      <CardContent className="pt-4 space-y-3">
        {/* Title + level row */}
        <div className="flex items-center gap-2">
          {editingTitle ? (
            <Input
              autoFocus
              value={titleValue}
              onChange={(e) => setTitleValue(e.target.value)}
              onBlur={saveTitle}
              onKeyDown={(e) => {
                if (e.key === "Enter") saveTitle();
                if (e.key === "Escape") {
                  setTitleValue(item.title);
                  setEditingTitle(false);
                }
              }}
              className="flex-1"
              maxLength={200}
            />
          ) : (
            <div className="flex flex-1 items-center gap-2 min-w-0">
              <button
                className="text-left font-medium hover:underline truncate"
                onClick={() => {
                  setTitleValue(item.title);
                  setEditingTitle(true);
                }}
              >
                {item.title}
              </button>
              {item.level && (
                <span
                  className={[
                    "shrink-0 rounded-full px-2 py-0.5 text-xs font-medium",
                    LEVEL_CLASS[item.level],
                  ].join(" ")}
                >
                  {item.level}
                </span>
              )}
            </div>
          )}

          {/* Level picker */}
          <Select
            value={item.level ?? "NONE"}
            onValueChange={handleLevelChange}
          >
            <SelectTrigger className="h-8 w-24 shrink-0 text-xs">
              <SelectValue placeholder="Nivel" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="NONE">Sin nivel</SelectItem>
              {LEVELS.map((l) => (
                <SelectItem key={l} value={l}>
                  {l}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Button
            variant="outline"
            size="sm"
            className="h-8 text-xs shrink-0"
            onClick={handleShare}
          >
            Compartir
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-8 text-xs text-destructive shrink-0"
            onClick={handleDelete}
          >
            Eliminar
          </Button>
        </div>

        {/* File area */}
        <div className="flex items-center gap-2 flex-wrap">
          <input
            type="file"
            accept=".pptx"
            className="hidden"
            ref={fileInputRef}
            onChange={handleFileChange}
          />
          {item.hasFile ? (
            <>
              <span className="text-sm text-muted-foreground truncate max-w-xs">
                📎 {item.originalFileName}
              </span>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 text-xs"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
              >
                Reemplazar
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 text-xs text-destructive"
                onClick={handleDeleteFile}
                disabled={uploading}
              >
                Quitar
              </Button>
            </>
          ) : (
            <Button
              variant="outline"
              size="sm"
              className="text-xs"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
            >
              {uploading ? "Subiendo…" : "Subir archivo .pptx"}
            </Button>
          )}
          {uploading && (
            <span className="text-xs text-muted-foreground animate-pulse">
              Subiendo…
            </span>
          )}
        </div>

        {/* Share info */}
        {item.sharedWithIds.length === 0 ? (
          <p className="text-xs text-muted-foreground">Sin compartir.</p>
        ) : (
          <div className="flex flex-wrap gap-1">
            {item.sharedWithIds.map((id) => {
              const student = students.find((s) => s.id === id);
              if (!student) return null;
              return (
                <span
                  key={id}
                  className="rounded-full bg-muted px-2 py-0.5 text-xs"
                >
                  <StudentLink student={student} />
                </span>
              );
            })}
          </div>
        )}

        {error && <p className="text-xs text-destructive">{error}</p>}
      </CardContent>

      {shareOpen && shareDeck && (
        <SharePresentationDialog
          open={shareOpen}
          onOpenChange={setShareOpen}
          deck={shareDeck}
          onShared={(updated) => {
            setShareDeck(updated);
            onUpdated({
              ...item,
              sharedWithIds: updated.sharedWith.map((s) => s.id),
            });
          }}
        />
      )}
    </Card>
  );
}
