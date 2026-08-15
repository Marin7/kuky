import { useEffect, useState } from "react";
import { Link } from "@tanstack/react-router";
import { ChevronRight } from "lucide-react";
import { useTranslation } from "react-i18next";
import { listMyQuizzes, type QuizListItem, type QuizStatus } from "@/lib/quiz";
import { Card, CardContent } from "@/components/ui/card";

const ACTIONABLE: QuizStatus[] = ["AVAILABLE", "IN_PROGRESS"];

export function AssignedQuizList() {
  const { t } = useTranslation();
  const [items, setItems] = useState<QuizListItem[] | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    listMyQuizzes()
      .then(setItems)
      .catch(() => setError(true));
  }, []);

  if (error) {
    return (
      <section className="space-y-4">
        <h2 className="font-display text-xl font-bold text-foreground">
          {t("quiz.title")}
        </h2>
        <p className="text-sm text-destructive">{t("quiz.loadError")}</p>
      </section>
    );
  }

  if (items === null) return null;
  if (items.length === 0) return null;

  return (
    <section className="space-y-4">
      <h2 className="font-display text-xl font-bold text-foreground">
        {t("quiz.title")}
      </h2>
      <div className="space-y-3">
        {items.map((q) => (
          <Link
            key={q.id}
            to="/quizzes/$quizId"
            params={{ quizId: q.id }}
            className="block rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <Card className="transition-colors hover:bg-muted/40">
              <CardContent className="flex items-center justify-between gap-3 pt-4">
                <div className="min-w-0 space-y-1.5">
                  <p className="truncate font-medium text-foreground">
                    {q.title}
                  </p>
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
                    <span>{t(`quiz.status.${q.status}`)}</span>
                    {ACTIONABLE.includes(q.status) && (
                      <span className="inline-block rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
                        {t("learning.units.pendingBadge")}
                      </span>
                    )}
                  </div>
                </div>
                <ChevronRight
                  className="h-5 w-5 shrink-0 text-muted-foreground"
                  aria-hidden
                />
                <span className="sr-only">{t("quiz.open")}</span>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </section>
  );
}
