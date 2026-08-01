import { createFileRoute } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/condiciones")({
  head: () => ({
    meta: seo({
      title: "Condiciones — Destino: Español",
      description:
        "Precio, cancelación y condiciones de las clases de español.",
      path: "/condiciones",
    }),
  }),
  component: CondicionesPage,
});

const SECTIONS = ["price", "cancellation", "payment", "other"] as const;

function CondicionesPage() {
  const { t } = useTranslation();
  return (
    <div className="mx-auto max-w-3xl px-6 py-20">
      <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-primary">
        {t("rules.badge")}
      </span>
      <h1 className="mt-4 font-display text-4xl font-semibold md:text-5xl">
        {t("rules.title")}
      </h1>
      <p className="mt-4 text-lg text-muted-foreground">{t("rules.intro")}</p>

      <div className="mt-12 space-y-10">
        {SECTIONS.map((key) => (
          <section key={key}>
            <h2 className="font-display text-2xl font-semibold">
              {t(`rules.${key}.title`)}
            </h2>
            <ul className="mt-4 list-disc space-y-2 pl-5 text-muted-foreground">
              {(
                t(`rules.${key}.items`, {
                  returnObjects: true,
                }) as string[]
              ).map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  );
}
