import type { ReactNode } from "react";

export function Placeholder({ eyebrow, title, description, children }: {
  eyebrow: string;
  title: string;
  description: string;
  children?: ReactNode;
}) {
  return (
    <div className="mx-auto max-w-4xl px-6 py-24">
      <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-primary">
        {eyebrow}
      </span>
      <h1 className="mt-4 font-display text-4xl font-semibold md:text-5xl">{title}</h1>
      <p className="mt-4 max-w-2xl text-lg text-muted-foreground">{description}</p>
      <div className="mt-10 rounded-2xl border border-dashed border-border bg-card/50 p-10 text-center text-muted-foreground">
        {children ?? "Próximamente — esta sección estará disponible muy pronto."}
      </div>
    </div>
  );
}
