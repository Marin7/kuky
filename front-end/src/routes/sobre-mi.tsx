import { createFileRoute } from "@tanstack/react-router";
import { Trans, useTranslation } from "react-i18next";
import teacherUrl from "@/assets/teacher.jpg";
import paulaUrl from "@/assets/paula-portrait.jpg";
import cvUrl from "@/assets/CV-aprilie 2026.pdf";
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
    name: "Destino: Español",
    url: SITE_URL,
  },
};

export const Route = createFileRoute("/sobre-mi")({
  head: () => ({
    meta: [
      ...seo({
        title: "Sobre mí — Destino: Español",
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
      <img
        src={teacherUrl}
        alt="Paula, profesora de español"
        className="mb-8 aspect-[4/5] w-full rounded-2xl object-cover shadow-lg md:float-left md:mb-4 md:mr-10 md:w-[38%]"
      />
      <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-primary">
        {t("about.badge")}
      </span>
      <h1 className="mt-4 font-display text-4xl font-semibold md:text-5xl">
        {t("about.title")}
      </h1>
      <div className="mt-6 space-y-4 text-muted-foreground">
        <p>{t("about.p1")}</p>
        <p>
          <Trans i18nKey="about.p2" components={{ em: <em /> }} />
        </p>
        <p>{t("about.p3")}</p>
        <p>{t("about.p4")}</p>
        <img
          src={paulaUrl}
          alt="Paula, profesora de español"
          className="mb-8 aspect-[4/5] w-full rounded-2xl object-cover shadow-lg md:float-right md:mb-4 md:ml-10 md:mt-4 md:w-[38%]"
        />
        <h2 className="clear-left pt-8 font-display text-2xl font-semibold text-foreground">
          {t("about.trajectoryTitle")}
        </h2>
        <p>{t("about.p5")}</p>
        <p>
          <Trans i18nKey="about.p6" components={{ em: <em /> }} />
        </p>
        <p>{t("about.p7")}</p>
        <blockquote className="border-l-4 border-primary bg-primary/5 px-5 py-4 font-display text-lg text-foreground">
          {t("about.takeaway")}
        </blockquote>
        <p>
          <Trans
            i18nKey="about.cv"
            components={{
              cvLink: (
                <a
                  href={cvUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-primary hover:underline"
                />
              ),
            }}
          />
        </p>
      </div>
    </div>
  );
}
