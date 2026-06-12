import { useTranslation } from "react-i18next";
import type { HomeworkItem } from "@/lib/learning";
import { HomeworkItemCard } from "./HomeworkItemCard";

interface HomeworkListProps {
  items: HomeworkItem[];
  onOpen: (item: HomeworkItem) => void;
}

export function HomeworkList({ items, onOpen }: HomeworkListProps) {
  const { t } = useTranslation();
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
          {items.map((item) => (
            <HomeworkItemCard key={item.id} item={item} onOpen={onOpen} />
          ))}
        </div>
      )}
    </section>
  );
}
