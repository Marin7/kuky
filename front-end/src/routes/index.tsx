import { createFileRoute, Link } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import teacherUrl from "@/assets/teacher.jpg";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: seo({
      title: "Español con Paula — Clases de español para rumanos",
      description:
        "Clases de español personalizadas, 100% online, para estudiantes rumanos de todos los niveles.",
      path: "/",
    }),
  }),
  component: Index,
});

function Index() {
  const { t } = useTranslation();
  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-br from-secondary/60 via-background to-accent/20" />
        <div className="mx-auto grid max-w-6xl gap-12 px-6 py-20 md:grid-cols-2 md:items-center md:py-28">
          <div>
            <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-primary">
              {t("home.hero.badge")}
            </span>
            <h1 className="mt-5 font-display text-5xl font-semibold leading-tight md:text-6xl">
              {t("home.hero.titleBefore")}{" "}
              <span className="text-primary italic">
                {t("home.hero.titleItalic")}
              </span>
              {t("home.hero.titleAfter")}
            </h1>
            <p className="mt-5 max-w-lg text-lg text-muted-foreground">
              {t("home.hero.subtitle")}
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to="/reservas"
                className="rounded-md bg-primary px-5 py-3 text-sm font-medium text-primary-foreground transition hover:bg-primary/90"
              >
                {t("home.hero.ctaBook")}
              </Link>
              <Link
                to="/sobre-mi"
                className="rounded-md border border-border bg-background px-5 py-3 text-sm font-medium hover:bg-accent/30"
              >
                {t("home.hero.ctaAbout")}
              </Link>
            </div>
          </div>
          <div className="relative">
            <div className="absolute -inset-4 -z-10 rounded-3xl bg-accent/40 blur-2xl" />
            <img
              src={teacherUrl}
              alt="Paula, profesora de español"
              className="aspect-[4/5] w-full rounded-2xl object-cover shadow-xl"
            />
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="mx-auto max-w-6xl px-6 py-16">
        <div className="grid gap-8 md:grid-cols-3">
          {(["personalized", "romanian", "materials"] as const).map((key) => (
            <div
              key={key}
              className="rounded-xl border border-border bg-card p-6"
            >
              <h3 className="font-display text-xl font-semibold">
                {t(`home.features.${key}.title`)}
              </h3>
              <p className="mt-2 text-sm text-muted-foreground">
                {t(`home.features.${key}.desc`)}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="mx-auto max-w-6xl px-6 py-16">
        <div className="rounded-2xl bg-primary px-8 py-12 text-center text-primary-foreground md:py-16">
          <h2 className="font-display text-3xl font-semibold md:text-4xl">
            {t("home.cta.title")}
          </h2>
          <p className="mx-auto mt-3 max-w-xl text-primary-foreground/80">
            {t("home.cta.subtitle")}
          </p>
          <Link
            to="/reservas"
            className="mt-6 inline-block rounded-md bg-background px-5 py-3 text-sm font-medium text-foreground hover:bg-secondary"
          >
            {t("home.cta.button")}
          </Link>
        </div>
      </section>
    </div>
  );
}
