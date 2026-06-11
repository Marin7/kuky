import { useEffect, useRef, useState } from "react";
import {
  listPresentations,
  createPresentation,
  deletePresentation,
  renamePresentation,
  getPresentation,
  uploadPresentationFile,
  deletePresentationFile,
  type PresentationSummary,
  type PresentationDetail,
  type ApiError,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { SharePresentationDialog } from "./SharePresentationDialog";

export function PresentationAdminList() {
  const [items, setItems] = useState<PresentationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [newTitle, setNewTitle] = useState("");
  const [creating, setCreating] = useState(false);

  const load = () => {
    setLoading(true);
    listPresentations()
      .then(setItems)
      .catch(() => setItems([]))
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
        { id: deck.id, title: deck.title, hasFile: false, originalFileName: null,
          sharedWith: 0, updatedAt: new Date().toISOString() },
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

  return (
    <div className="space-y-4">
      <div className="flex items-end gap-2">
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

      {loading ? (
        <p className="text-sm text-muted-foreground">Cargando…</p>
      ) : items.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Aún no has creado ninguna presentación.
        </p>
      ) : (
        items.map((item) => (
          <PresentationCard
            key={item.id}
            item={item}
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
  onDeleted: () => void;
  onUpdated: (updated: PresentationSummary) => void;
}

function PresentationCard({ item, onDeleted, onUpdated }: CardProps) {
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

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";
    setUploading(true);
    setError(null);
    try {
      const updated = await uploadPresentationFile(item.id, file);
      onUpdated({
        ...item,
        hasFile: true,
        originalFileName: updated.originalFileName,
      });
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
        {/* Title row */}
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
            <button
              className="flex-1 text-left font-medium hover:underline truncate"
              onClick={() => {
                setTitleValue(item.title);
                setEditingTitle(true);
              }}
            >
              {item.title}
            </button>
          )}
          <Button variant="outline" size="sm" className="h-8 text-xs shrink-0" onClick={handleShare}>
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
        <p className="text-xs text-muted-foreground">
          Compartida con {item.sharedWith} alumno(s)
        </p>

        {error && <p className="text-xs text-destructive">{error}</p>}
      </CardContent>

      {shareOpen && shareDeck && (
        <SharePresentationDialog
          open={shareOpen}
          onOpenChange={setShareOpen}
          deck={shareDeck}
          onShared={(updated) => {
            setShareDeck(updated);
            onUpdated({ ...item, sharedWith: updated.sharedWith.length });
          }}
        />
      )}
    </Card>
  );
}
