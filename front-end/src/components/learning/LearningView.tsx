import { useEffect, useState } from "react";
import { getLearning, type LearningResponse, type HomeworkItem } from "@/lib/learning";
import { ClassPresentation } from "./ClassPresentation";
import { PastClassesList } from "./PastClassesList";
import { HomeworkList } from "./HomeworkList";
import { HomeworkSubmitDialog } from "./HomeworkSubmitDialog";

export function LearningView() {
  const [data, setData] = useState<LearningResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dialogItem, setDialogItem] = useState<HomeworkItem | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    getLearning()
      .then(setData)
      .catch(() => setError("No se pudo cargar tu aprendizaje. Inténtalo de nuevo."))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const handleSubmitted = (updated: HomeworkItem) => {
    setData((prev) =>
      prev
        ? {
            ...prev,
            homework: prev.homework.map((h) =>
              h.id === updated.id ? updated : h,
            ),
          }
        : prev,
    );
  };

  return (
    <div className="mx-auto max-w-5xl px-6 py-10 space-y-10">
      <div>
        <h1 className="font-display text-3xl font-bold text-foreground">
          Mi aprendizaje
        </h1>
        <p className="mt-2 text-muted-foreground">
          Tu espacio personal: la presentación de las clases, tus clases anteriores y tus tareas.
        </p>
      </div>

      {loading && (
        <p className="text-muted-foreground animate-pulse">Cargando…</p>
      )}

      {error && <p className="text-destructive">{error}</p>}

      {!loading && !error && data && (
        <>
          <ClassPresentation blocks={data.presentation} />
          <PastClassesList classes={data.pastClasses} />
          <HomeworkList items={data.homework} onOpen={setDialogItem} />
        </>
      )}

      <HomeworkSubmitDialog
        item={dialogItem}
        onClose={() => setDialogItem(null)}
        onSubmitted={handleSubmitted}
      />
    </div>
  );
}
