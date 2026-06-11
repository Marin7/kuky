import type { PresentationBlock } from "@/lib/learning";

interface ClassPresentationProps {
  blocks: PresentationBlock[];
}

export function ClassPresentation({ blocks }: ClassPresentationProps) {
  // Sensible default when no presentation content has been authored yet.
  if (blocks.length === 0) {
    return (
      <section className="rounded-xl border border-border/60 bg-secondary/30 p-6">
        <h2 className="font-display text-xl font-semibold text-foreground">
          Tus clases de español
        </h2>
        <p className="mt-2 text-muted-foreground">
          Aquí encontrarás tus clases anteriores y las tareas que te asigne.
          ¡Vamos a aprender juntos!
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
