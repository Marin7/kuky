import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { Fragment, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe } from "@/lib/auth";
import {
  getStudentMistakes,
  getStudentProfile,
  getStudentQuizzes,
  homeworkBreakdownFromList,
  isExerciseResultFormat,
  quizBreakdownFromList,
  setBookingNoShow,
  studentDisplayName,
  type MistakePeriod,
  type StudentMistakeEntry,
  type StudentMistakes,
  type StudentProfile,
  type StudentQuizSummary,
  type StudentProfileHomework,
} from "@/lib/admin";
import { useTeacherTimezone } from "@/hooks/useTeacherTimezone";
import { StudentStatBox } from "@/components/admin/students/StudentStatBox";
import { StudentClassesPanel } from "@/components/admin/students/StudentClassesPanel";
import { StudentPresentationsPanel } from "@/components/admin/students/StudentPresentationsPanel";
import { StudentQuizzesPanel } from "@/components/admin/students/StudentQuizzesPanel";
import { StudentMistakesList } from "@/components/admin/students/StudentMistakesList";
import { HomeworkReviewDialog } from "@/components/admin/homework/HomeworkReviewDialog";
import { ExerciseResultDialog } from "@/components/admin/homework/ExerciseResultDialog";
import { QuizReviewDialog } from "@/components/quiz/admin/QuizReviewDialog";
import { NotificationDot } from "@/components/NotificationDot";
import { notifyBadgesChanged } from "@/lib/notifications";

export const Route = createFileRoute("/panel_/alumnos/$studentId")({
  component: StudentProfilePage,
});

/** Box order on the profile; expanded panels render in the same order. */
const BOX_ORDER = [
  "classes",
  "homework",
  "presentations",
  "mistakes",
  "quizzes",
] as const;

type StudentBoxKey = (typeof BOX_ORDER)[number];

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(iso));
}

function StatusBadge({ status }: { status: string }) {
  const { t } = useTranslation();
  const label =
    (t(`admin.studentProfile.status.${status}` as never) as string) || status;
  const cls =
    status === "SUBMITTED" || status === "REVIEWED"
      ? "bg-green-100 text-green-700"
      : status === "CONFIRMED"
        ? "bg-blue-100 text-blue-700"
        : "bg-muted text-muted-foreground";
  return (
    <span
      className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${cls}`}
    >
      {label}
    </span>
  );
}

function sortHomeworks(homeworks: StudentProfileHomework[]) {
  return [...homeworks].sort((a, b) => {
    const aPending = a.status === "PENDING";
    const bPending = b.status === "PENDING";
    if (aPending !== bPending) return aPending ? -1 : 1;
    if (aPending) return 0;
    const aTime = a.submittedAt ? Date.parse(a.submittedAt) : 0;
    const bTime = b.submittedAt ? Date.parse(b.submittedAt) : 0;
    return bTime - aTime;
  });
}

function StudentProfilePage() {
  const { t } = useTranslation();
  const { studentId } = Route.useParams();
  const navigate = useNavigate();
  const teacherTimezone = useTeacherTimezone();

  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [quizzes, setQuizzes] = useState<StudentQuizSummary[] | null>(null);
  const [expanded, setExpanded] = useState<Set<StudentBoxKey>>(new Set());
  const [openSubmissionId, setOpenSubmissionId] = useState<string | null>(null);
  const [openResultId, setOpenResultId] = useState<string | null>(null);
  const [openQuizAttempt, setOpenQuizAttempt] = useState<{
    quizId: string;
    attemptId: string;
  } | null>(null);

  const [mistakes, setMistakes] = useState<StudentMistakes | null>(null);
  const [mistakesLoading, setMistakesLoading] = useState(true);
  const [mistakesError, setMistakesError] = useState<string | null>(null);
  const [mistakesPeriod, setMistakesPeriod] = useState<MistakePeriod>("ALL");
  const [mistakesLabel, setMistakesLabel] = useState<string | null>(null);
  const [mistakesPage, setMistakesPage] = useState(1);
  const [mistakesReload, setMistakesReload] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setMistakesLoading(true);
    getStudentMistakes(studentId, {
      period: mistakesPeriod,
      label: mistakesLabel,
      page: mistakesPage,
    })
      .then((data) => {
        if (cancelled) return;
        setMistakes(data);
        setMistakesError(null);
        // Selected label no longer present → server served "all labels".
        if (mistakesLabel !== null && data.appliedLabel === null) {
          setMistakesLabel(null);
        }
        if (data.page !== mistakesPage) setMistakesPage(data.page);
      })
      .catch(() => {
        if (!cancelled) {
          setMistakesError(t("admin.studentProfile.mistakes.loadError"));
        }
      })
      .finally(() => {
        if (!cancelled) setMistakesLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [
    studentId,
    mistakesPeriod,
    mistakesLabel,
    mistakesPage,
    mistakesReload,
    t,
  ]);

  const toggleBox = (key: StudentBoxKey) =>
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });

  const openMistake = (entry: StudentMistakeEntry) => {
    if (isExerciseResultFormat(entry.homeworkFormat)) {
      setOpenResultId(entry.submissionId);
    } else {
      setOpenSubmissionId(entry.submissionId);
    }
  };

  const reloadProfile = () => {
    getStudentProfile(studentId).then(setProfile);
    getStudentQuizzes(studentId)
      .then(setQuizzes)
      .catch(() => setQuizzes([]));
    setMistakesReload((n) => n + 1);
    notifyBadgesChanged();
  };

  useEffect(() => {
    getMe()
      .then((me) => {
        if (me.role !== "ADMIN") {
          navigate({ to: "/" });
        }
      })
      .catch(() => navigate({ to: "/cuenta" }));
  }, []);

  useEffect(() => {
    getStudentProfile(studentId)
      .then(setProfile)
      .catch(() => setError(t("admin.studentProfile.loadError")))
      .finally(() => setLoading(false));
    getStudentQuizzes(studentId)
      .then(setQuizzes)
      .catch(() => setQuizzes([]));
  }, [studentId]);

  const handleToggleNoShow = (
    bookingId: string,
    noShow: boolean,
    isCompanionStudent: boolean,
  ) => {
    setBookingNoShow(
      bookingId,
      noShow,
      isCompanionStudent ? "COMPANION" : "BOOKING_STUDENT",
    )
      .then(() => getStudentProfile(studentId))
      .then(setProfile)
      .catch(() => setError(t("admin.studentProfile.loadError")));
  };

  const upcoming =
    profile?.bookings.filter(
      (b) => b.status === "CONFIRMED" && new Date(b.slotEnd) > new Date(),
    ) ?? [];
  const past =
    profile?.bookings.filter(
      (b) => b.status === "CONFIRMED" && new Date(b.slotEnd) <= new Date(),
    ) ?? [];

  const name = profile ? studentDisplayName(profile) : "…";

  const renderHomeworkPanel = (homeworks: StudentProfileHomework[]) =>
    homeworks.length === 0 ? (
      <p className="text-sm text-muted-foreground">
        {t("admin.studentProfile.emptyHomework")}
      </p>
    ) : (
      <div className="divide-y rounded-lg border">
        {sortHomeworks(homeworks).map((hw) => (
          <div
            key={hw.id}
            className="flex items-center justify-between px-4 py-3 text-sm"
          >
            <span className="inline-flex items-center gap-1.5">
              {hw.title}
              {hw.unseen && <NotificationDot label={t("notification.row")} />}
              {hw.overdue && (
                <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800">
                  {t("learning.homework.overdue")}
                </span>
              )}
            </span>
            <div className="flex items-center gap-2 ml-4 shrink-0">
              {hw.dueOn && (
                <span className="text-xs text-muted-foreground">
                  {t("admin.homework.dueOn")}{" "}
                  {new Intl.DateTimeFormat("es", {
                    day: "numeric",
                    month: "long",
                    year: "numeric",
                  }).format(new Date(`${hw.dueOn}T00:00:00`))}
                </span>
              )}
              {hw.submittedAt && (
                <span className="text-xs text-muted-foreground">
                  {formatDate(hw.submittedAt)}
                </span>
              )}
              <StatusBadge status={hw.status} />
              {hw.status === "GRADED" && hw.scorePercent !== null && (
                <span className="text-xs font-medium text-muted-foreground">
                  {hw.scorePercent}%
                </span>
              )}
              {hw.hasTeacherFeedback && (
                <span className="rounded-full bg-sky-100 px-2 py-0.5 text-xs font-medium text-sky-800">
                  {t("admin.exerciseResult.hasFeedbackBadge")}
                </span>
              )}
              {hw.needsReview && hw.submissionId && (
                <button
                  type="button"
                  onClick={() => setOpenSubmissionId(hw.submissionId)}
                  className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 hover:underline"
                >
                  {t("admin.homeworkReview.needsReviewBadge")}
                </button>
              )}
              {!hw.needsReview &&
                hw.unseen &&
                hw.submissionId &&
                hw.status !== "GRADED" && (
                  <button
                    type="button"
                    onClick={() => setOpenSubmissionId(hw.submissionId)}
                    className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary hover:underline"
                  >
                    {t("admin.homeworkReview.reviewAction")}
                  </button>
                )}
              {hw.status === "GRADED" && hw.submissionId && (
                <button
                  type="button"
                  onClick={() => {
                    const id = hw.submissionId;
                    if (!id) return;
                    if (isExerciseResultFormat(hw.format)) {
                      setOpenResultId(id);
                    } else {
                      setOpenSubmissionId(id);
                    }
                  }}
                  className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary hover:underline"
                >
                  {t("admin.exerciseResult.viewAction")}
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    );

  const renderPanel = (key: StudentBoxKey, p: StudentProfile) => {
    switch (key) {
      case "classes":
        return (
          <StudentClassesPanel
            upcoming={upcoming}
            past={past}
            teacherTimezone={teacherTimezone}
            onToggleNoShow={handleToggleNoShow}
          />
        );
      case "homework":
        return renderHomeworkPanel(p.homeworks);
      case "presentations":
        return <StudentPresentationsPanel presentations={p.presentations} />;
      case "mistakes":
        return (
          <StudentMistakesList
            data={mistakes}
            loading={mistakesLoading}
            error={mistakesError}
            period={mistakesPeriod}
            labelKey={mistakesLabel}
            onPeriodChange={(period) => {
              setMistakesPeriod(period);
              setMistakesPage(1);
            }}
            onLabelChange={(label) => {
              setMistakesLabel(label);
              setMistakesPage(1);
            }}
            onPageChange={setMistakesPage}
            onOpen={openMistake}
          />
        );
      case "quizzes":
        return (
          <StudentQuizzesPanel
            quizzes={quizzes ?? []}
            onOpenAttempt={(quizId, attemptId) =>
              setOpenQuizAttempt({ quizId, attemptId })
            }
          />
        );
    }
  };

  const renderBox = (key: StudentBoxKey, p: StudentProfile) => {
    const common = {
      expanded: expanded.has(key),
      onToggle: () => toggleBox(key),
    };
    switch (key) {
      case "classes":
        return (
          <StudentStatBox
            {...common}
            title={t("admin.studentProfile.stats.classes")}
            count={upcoming.length + past.length}
            breakdown={[
              {
                label: t("admin.studentProfile.classesUpcoming"),
                value: upcoming.length,
              },
              {
                label: t("admin.studentProfile.classesPast"),
                value: past.length,
              },
            ]}
            expandLabel={t("admin.studentProfile.expandClasses")}
            collapseLabel={t("admin.studentProfile.collapseClasses")}
          />
        );
      case "homework": {
        const hw = homeworkBreakdownFromList(p.homeworks);
        return (
          <StudentStatBox
            {...common}
            title={t("admin.studentProfile.stats.homework")}
            count={p.homeworks.length}
            breakdown={[
              {
                label: t("admin.studentProfile.homeworkPending"),
                value: hw.pending,
              },
              {
                label: t("admin.studentProfile.homeworkSubmitted"),
                value: hw.submitted,
              },
              {
                label: t("admin.studentProfile.homeworkCompleted"),
                value: hw.completed,
              },
            ]}
            unseen={p.homeworks.some((h) => h.unseen)}
            expandLabel={t("admin.studentProfile.expandHomework")}
            collapseLabel={t("admin.studentProfile.collapseHomework")}
          />
        );
      }
      case "presentations":
        return (
          <StudentStatBox
            {...common}
            title={t("admin.studentProfile.stats.presentations")}
            count={p.presentations.length}
            expandLabel={t("admin.studentProfile.expandPresentations")}
            collapseLabel={t("admin.studentProfile.collapsePresentations")}
          />
        );
      case "mistakes":
        return (
          <StudentStatBox
            {...common}
            title={t("admin.studentProfile.mistakes.title")}
            count={mistakes?.allTimeMistakeCount ?? null}
            expandLabel={t("admin.studentProfile.mistakes.expand")}
            collapseLabel={t("admin.studentProfile.mistakes.collapse")}
          />
        );
      case "quizzes": {
        const qb = quizzes ? quizBreakdownFromList(quizzes) : null;
        return (
          <StudentStatBox
            {...common}
            title={t("admin.studentProfile.stats.quizzes")}
            count={quizzes?.length ?? null}
            breakdown={
              qb
                ? [
                    {
                      label: t("admin.studentProfile.quizzesToReview"),
                      value: qb.toReview,
                    },
                    {
                      label: t("admin.studentProfile.quizzesGraded"),
                      value: qb.graded,
                    },
                  ]
                : undefined
            }
            unseen={quizzes?.some((q) => q.unseen) ?? false}
            expandLabel={t("admin.studentProfile.expandQuizzes")}
            collapseLabel={t("admin.studentProfile.collapseQuizzes")}
          />
        );
      }
    }
  };

  return (
    <div className="mx-auto max-w-3xl px-6 py-12">
      <Link
        to="/panel"
        search={{ tab: "students" } as never}
        className="mb-8 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("admin.studentProfile.back")}
      </Link>

      {loading && (
        <p className="mt-6 text-sm text-muted-foreground animate-pulse">
          {t("admin.studentProfile.loading")}
        </p>
      )}

      {error && <p className="mt-6 text-sm text-destructive">{error}</p>}

      {profile && (
        <div className="mt-6">
          <h1 className="font-display text-3xl font-semibold text-primary">
            {name}
          </h1>
          <p className="mt-1 text-muted-foreground">
            {profile.email}
            {profile.username && (
              <span className="ml-3 text-sm">@{profile.username}</span>
            )}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            {t("admin.studentProfile.studentSince")}{" "}
            {formatDate(profile.createdAt)}
          </p>

          <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5">
            {BOX_ORDER.map((key) => (
              <Fragment key={key}>{renderBox(key, profile)}</Fragment>
            ))}
          </div>

          {BOX_ORDER.filter((key) => expanded.has(key)).map((key) => (
            <div key={key} className="mt-4">
              {renderPanel(key, profile)}
            </div>
          ))}
        </div>
      )}

      {openSubmissionId && (
        <HomeworkReviewDialog
          submissionId={openSubmissionId}
          onClose={() => setOpenSubmissionId(null)}
          onReviewed={() => {
            setOpenSubmissionId(null);
            reloadProfile();
          }}
        />
      )}
      {openResultId && (
        <ExerciseResultDialog
          submissionId={openResultId}
          onClose={() => setOpenResultId(null)}
          onFeedbackSaved={reloadProfile}
        />
      )}
      {openQuizAttempt && (
        <QuizReviewDialog
          quizId={openQuizAttempt.quizId}
          attemptId={openQuizAttempt.attemptId}
          onClose={() => setOpenQuizAttempt(null)}
          onSaved={reloadProfile}
        />
      )}
    </div>
  );
}
