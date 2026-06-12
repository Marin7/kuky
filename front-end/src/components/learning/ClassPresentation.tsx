import { useTranslation } from "react-i18next";
import type { PresentationBlock } from "@/lib/learning";

interface ClassPresentationProps {
  blocks: PresentationBlock[];
}

export function ClassPresentation({ blocks }: ClassPresentationProps) {
  const { t } = useTranslation();

  if (blocks.length === 0) {
    return (
      <section className="rounded-xl border border-border/60 bg-secondary/30 p-6">
        <h2 className="font-display text-xl font-semibold text-foreground">
          {t("learning.classPresentation.title")}
        </h2>
        <p className="mt-2 text-muted-foreground">
          {t("learning.classPresentation.intro")}
        </p>
      </section>
    );
  }

  return (
    <section className="rounded-xl border border-border/60 bg-secondary/30 p-6">
      <div className="space-y-5">
        {blocks.map((block, i) => (
          <div key={i}>
            <h2 className="font-display text-lg font-semibold text-foreground">
              {block.heading}
            </h2>
            <p className="mt-1 text-muted-foreground">{block.body}</p>
          </div>
        ))}
      </div>
    </section>
  );
}
