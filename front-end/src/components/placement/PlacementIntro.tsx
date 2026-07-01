import { useTranslation } from "react-i18next";
import type { SectionDto } from "@/lib/placement";
import { Button } from "@/components/ui/button";

interface Props {
  sections: SectionDto[];
  starting: boolean;
  onStart: () => void;
}

/** Welcome screen before the timed sections begin (the user is already authenticated — FR-001). */
export function PlacementIntro({ sections, starting, onStart }: Props) {
  const { t } = useTranslation();

  return (
    <div className="space-y-6 text-center">
      <h1 className="font-display text-3xl font-semibold text-primary">
        {t("placement.intro.title")}
      </h1>
      <p className="mx-auto max-w-xl text-muted-foreground">
        {t("placement.intro.subtitle")}
      </p>

      <ul className="mx-auto flex max-w-md flex-col gap-2 text-sm">
        {sections.map((s) => (
          <li
            key={s.skill}
            className="flex items-center justify-between rounded-lg border bg-card px-4 py-2"
          >
            <span>{t(`placement.intro.sections.${s.skill}` as never)}</span>
            <span className="text-muted-foreground">
              {t("placement.intro.minutes", {
                count: Math.round(s.timeLimitSeconds / 60),
              })}
            </span>
          </li>
        ))}
      </ul>

      <Button onClick={onStart} disabled={starting} size="lg">
        {t("placement.intro.start")}
      </Button>
    </div>
  );
}
