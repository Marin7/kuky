import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "@tanstack/react-router";
import { getLearning, type HomeworkItem } from "@/lib/learning";
import { ManualAnswerForm } from "./ManualAnswerForm";

interface Props {
  homeworkId: string;
}

export function HomeworkWritePage({ homeworkId }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [item, setItem] = useState<HomeworkItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    getLearning()
      .then((data) => {
        const found = data.homework.find(
          (h) => h.id === homeworkId && h.format === "MANUAL",
        );
        if (!found) {
          setLoadError(t("learning.writePage.notFound"));
          return;
        }
        setItem(found);
      })
      .catch(() => setLoadError(t("learning.writePage.loadError")))
      .finally(() => setLoading(false));
  }, [homeworkId]);

  return (
    <div className="mx-auto max-w-2xl px-6 py-12">
      <Link
        to="/aprendizaje"
        className="mb-8 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("learning.writePage.back")}
      </Link>

      {loading && (
        <p className="mt-6 animate-pulse text-sm text-muted-foreground">
          {t("learning.writePage.loading")}
        </p>
      )}

      {loadError && !item && (
        <p className="mt-6 text-sm text-destructive">{loadError}</p>
      )}

      {item && (
        <>
          <h1 className="font-display text-3xl font-semibold text-primary">
            {item.title}
          </h1>
          <p className="mt-2 whitespace-pre-wrap text-muted-foreground">
            {item.instructions}
          </p>

          <ManualAnswerForm
            homeworkId={homeworkId}
            initialResponse={item.response}
            readOnly={item.status === "REVIEWED"}
            labels={{
              yourAnswer: t("learning.writePage.yourAnswer"),
              placeholder: t("learning.writePage.placeholder"),
              submit: t("learning.writePage.submit"),
              submitting: t("learning.writePage.submitting"),
              autosaveHint: t("learning.writePage.autosaveHint"),
            }}
            onSubmitted={() => navigate({ to: "/aprendizaje" })}
          />
        </>
      )}
    </div>
  );
}
