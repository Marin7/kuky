import { useTranslation } from "react-i18next";

interface StudentHomeworkBreakdownProps {
  pending: number;
  submitted: number;
  completed: number;
  pendingLabel?: string;
  submittedLabel?: string;
  completedLabel?: string;
}

export function StudentHomeworkBreakdown({
  pending,
  submitted,
  completed,
  pendingLabel,
  submittedLabel,
  completedLabel,
}: StudentHomeworkBreakdownProps) {
  const { t } = useTranslation();

  const groups = [
    {
      label:
        pendingLabel ?? t("admin.studentProfile.progress.homeworkPending"),
      value: pending,
    },
    {
      label:
        submittedLabel ??
        t("admin.studentProfile.progress.homeworkSubmitted"),
      value: submitted,
    },
    {
      label:
        completedLabel ??
        t("admin.studentProfile.progress.homeworkCompleted"),
      value: completed,
    },
  ];

  return (
    <div className="grid grid-cols-3 gap-4">
      {groups.map((group) => (
        <div
          key={group.label}
          className="rounded-lg border bg-card p-3 text-center"
        >
          <p className="text-lg font-semibold">{group.value}</p>
          <p className="text-xs text-muted-foreground">{group.label}</p>
        </div>
      ))}
    </div>
  );
}
