import { createFileRoute } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import teacherUrl from "@/assets/teacher.jpg";
import { seo, jsonLd, OG_IMAGE, SITE_URL } from "@/lib/seo";

const PAULA_JSON_LD = {
  "@context": "https://schema.org",
  "@type": "Person",
  name: "Paula",
  jobTitle: "Profesora de español",
  description:
    "Conoce a Paula, profesora de español dedicada a ayudar a estudiantes rumanos a dominar el idioma.",
  image: OG_IMAGE,
  worksFor: {
    "@type": "EducationalOrganization",
    name: "Español con Paula",
    url: SITE_URL,
  },
};

export const Route = createFileRoute("/sobre-mi")({
  head: () => ({
    meta: [
      ...seo({
        title: "Sobre mí — Español con Paula",
        description:
          "Conoce a Paula, profesora de español dedicada a ayudar a estudiantes rumanos a dominar el idioma.",
        path: "/sobre-mi",
      }),
      jsonLd(PAULA_JSON_LD),
    ],
  }),
  component: SobreMi,
});

function SobreMi() {
  const { t } = useTranslation();
  return (
    <div className="mx-auto max-w-5xl px-6 py-20">
      <div className="grid gap-12 md:grid-cols-[2fr_3fr] md:items-start">
        <img
          src={teacherUrl}
          alt="Paula, profesora de español"
          className="aspect-[4/5] w-full rounded-2xl object-cover shadow-lg"
        />
        <div>
          <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-primary">
            {t("about.badge")}
          </span>
          <h1 className="mt-4 font-display text-4xl font-semibold md:text-5xl">
            {t("about.title")}
          </h1>
          <div className="mt-6 space-y-4 text-muted-foreground">
            <p>{t("about.p1")}</p>
            <p>{t("about.p2")}</p>
            <p>{t("about.p3")}</p>
          </div>

          <div className="mt-10 grid gap-4 sm:grid-cols-2">
            {(["levels", "modality", "languages", "focus"] as const).map(
              (key) => (
                <div
                  key={key}
                  className="rounded-lg border border-border bg-card p-4"
                >
                  <div className="text-xs uppercase tracking-wider text-muted-foreground">
                    {t(`about.stats.${key}Label`)}
                  </div>
                  <div className="mt-1 font-medium">
                    {t(`about.stats.${key}Value`)}
                  </div>
                </div>
              ),
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
