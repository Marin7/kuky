import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "@tanstack/react-router";
import {
  deleteQuiz,
  listAdminQuizzes,
  setQuizAssignees,
  type QuizAdminListItem,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StudentMultiSelect } from "@/components/admin/homework/StudentMultiSelect";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

export function QuizTab() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [items, setItems] = useState<QuizAdminListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [assignItem, setAssignItem] = useState<QuizAdminListItem | null>(null);

  const load = () => {
    setLoading(true);
    listAdminQuizzes()
      .then(setItems)
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">{t("quiz.admin.title")}</h2>
        <Button onClick={() => navigate({ to: "/panel/quizzes/nueva" })}>
          {t("quiz.admin.newQuiz")}
        </Button>
      </div>
      {loading ? (
        <p className="text-sm text-muted-foreground">{t("quiz.loading")}</p>
      ) : items.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t("quiz.admin.empty")}</p>
      ) : (
        <div className="space-y-3">
          {items.map((item) => (
            <Card key={item.id}>
              <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <CardTitle className="text-base">{item.title}</CardTitle>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      navigate({
                        to: "/panel/quizzes/$quizId",
                        params: { quizId: item.id },
                      })
                    }
                  >
                    {t("common.edit")}
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setAssignItem(item)}
                    disabled={item.questionCount < 1}
                  >
                    {t("quiz.admin.assign")}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      if (!confirm(t("quiz.admin.deleteConfirm"))) return;
                      deleteQuiz(item.id).then(load);
                    }}
                  >
                    {t("common.delete")}
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                {t("quiz.admin.questions", { count: item.questionCount })} ·{" "}
                {t("quiz.admin.assignees", { count: item.assigneeCount })} ·{" "}
                {t("quiz.admin.attempts", { count: item.attemptCount })}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
      {assignItem && (
        <QuizAssignDialog
          quiz={assignItem}
          onClose={() => setAssignItem(null)}
          onAssigned={load}
        />
      )}
    </div>
  );
}

function QuizAssignDialog({
  quiz,
  onClose,
  onAssigned,
}: {
  quiz: QuizAdminListItem;
  onClose: () => void;
  onAssigned: () => void;
}) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      await setQuizAssignees(quiz.id, selected);
      onAssigned();
      onClose();
    } catch {
      setError(t("quiz.admin.assignError"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {t("quiz.admin.assign")} — {quiz.title}
          </DialogTitle>
        </DialogHeader>
        <StudentMultiSelect selected={selected} onChange={setSelected} />
        {error && <p className="text-sm text-destructive">{error}</p>}
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            {t("common.cancel")}
          </Button>
          <Button onClick={handleSave} disabled={saving}>
            {saving ? t("common.saving") : t("common.save")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
