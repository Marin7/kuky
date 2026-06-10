import type { HomeworkItem } from "@/lib/learning";
import { HomeworkItemCard } from "./HomeworkItemCard";

interface HomeworkListProps {
  items: HomeworkItem[];
  onOpen: (item: HomeworkItem) => void;
}

export function HomeworkList({ items, onOpen }: HomeworkListProps) {
  return (
    <section className="space-y-4">
      <h2 className="font-display text-xl font-bold text-foreground">Tareas</h2>

      {items.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          No tienes tareas asignadas por ahora. Cuando Paula te asigne una, aparecerá aquí.
        </p>
      ) : (
        <div className="space-y-3">
          {items.map((item) => (
            <HomeworkItemCard key={item.id} item={item} onOpen={onOpen} />
          ))}
        </div>
      )}
    </section>
  );
}
