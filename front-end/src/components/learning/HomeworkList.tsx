import { useTranslation } from "react-i18next";
import type { HomeworkItem } from "@/lib/learning";
import { HomeworkItemCard } from "./HomeworkItemCard";

interface HomeworkListProps {
  items: HomeworkItem[];
  onOpen: (item: HomeworkItem) => void;
}

const STATUS_ORDER: Record<string, number> = {
  PENDING: 0,
  SUBMITTED: 1,
  REVIEWED: 2,
  GRADED: 3,
};

export function HomeworkList({ items, onOpen }: HomeworkListProps) {
  const { t } = useTranslation();
  const sorted = [...items].sort(
    (a, b) => (STATUS_ORDER[a.status] ?? 9) - (STATUS_ORDER[b.status] ?? 9),
  );
  return (
    <section className="space-y-4">
      <h2 className="font-display text-xl font-bold text-foreground">
        {t("learning.homework.title")}
      </h2>

      {items.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          {t("learning.homework.empty")}
        </p>
      ) : (
        <div className="space-y-3">
          {sorted.map((item) => (
            <HomeworkItemCard key={item.id} item={item} onOpen={onOpen} />
          ))}
        </div>
      )}
    </section>
  );
}
