import { useTranslation } from "react-i18next";

interface StudentHomeworkBreakdownProps {
  pending: number;
  submitted: number;
  completed: number;
  pendingLabel?: string;
  submittedLabel?: string;
  completedLabel?: string;
  compact?: boolean;
}

export function StudentHomeworkBreakdown({
  pending,
  submitted,
  completed,
  pendingLabel,
  submittedLabel,
  completedLabel,
  compact = false,
}: StudentHomeworkBreakdownProps) {
  const { t } = useTranslation();

  const groups = [
    {
      label: pendingLabel ?? t("admin.studentProfile.homeworkPending"),
      value: pending,
    },
    {
      label: submittedLabel ?? t("admin.studentProfile.homeworkSubmitted"),
      value: submitted,
    },
    {
      label: completedLabel ?? t("admin.studentProfile.homeworkCompleted"),
      value: completed,
    },
  ];

  if (compact) {
    return (
      <div className="grid grid-cols-3 gap-1">
        {groups.map((group) => (
          <div key={group.label} className="text-center">
            <p className="text-sm font-semibold leading-tight">{group.value}</p>
            <p className="text-[10px] leading-tight text-muted-foreground">
              {group.label}
            </p>
          </div>
        ))}
      </div>
    );
  }

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
