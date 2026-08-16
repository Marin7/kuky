import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe } from "@/lib/auth";
import {
  getStudentProfile,
  getStudentQuizzes,
  homeworkBreakdownFromList,
  isExerciseResultFormat,
  setBookingNoShow,
  studentDisplayName,
  type StudentProfile,
  type StudentQuizSummary,
  type StudentProfileHomework,
} from "@/lib/admin";
import { useTeacherTimezone } from "@/hooks/useTeacherTimezone";
import { StudentHomeworkBreakdown } from "@/components/admin/students/StudentHomeworkBreakdown";
import { HomeworkReviewDialog } from "@/components/admin/homework/HomeworkReviewDialog";
import { ExerciseResultDialog } from "@/components/admin/homework/ExerciseResultDialog";
import { QuizReviewDialog } from "@/components/quiz/admin/QuizReviewDialog";
import { NotificationDot } from "@/components/NotificationDot";
import { notifyBadgesChanged } from "@/lib/notifications";

export const Route = createFileRoute("/panel_/alumnos/$studentId")({
  component: StudentProfilePage,
});

function formatSlot(
  isoStart: string,
  isoEnd: string,
  timezone: string,
): string {
  const start = new Date(isoStart);
  const end = new Date(isoEnd);
  const datePart = new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: timezone,
  }).format(start);
  const timePart = new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: timezone,
  }).format(start);
  const endTime = new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: timezone,
  }).format(end);
  return `${datePart}, ${timePart}–${endTime}`;
}

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

function Section({
  title,
  count,
  children,
}: {
  title: string;
  count: number;
  children: React.ReactNode;
}) {
  return (
    <div>
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        {title}{" "}
        <span className="ml-1 rounded-full bg-muted px-2 py-0.5 text-xs font-normal">
          {count}
        </span>
      </h2>
      {children}
    </div>
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
  const [quizzes, setQuizzes] = useState<StudentQuizSummary[]>([]);
  const [tareasExpanded, setTareasExpanded] = useState(false);
  const [openSubmissionId, setOpenSubmissionId] = useState<string | null>(null);
  const [openResultId, setOpenResultId] = useState<string | null>(null);
  const [openQuizAttempt, setOpenQuizAttempt] = useState<{
    quizId: string;
    attemptId: string;
  } | null>(null);

  const reloadProfile = () => {
    getStudentProfile(studentId).then(setProfile);
    getStudentQuizzes(studentId).then(setQuizzes).catch(() => setQuizzes([]));
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
  const homeworkBreakdown = profile
    ? homeworkBreakdownFromList(profile.homeworks)
    : { pending: 0, submitted: 0, completed: 0 };

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
        <>
          <div className="mt-6 mb-10">
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

            <div className="mt-6 grid grid-cols-3 gap-4">
              <div className="rounded-lg border bg-card p-4 text-center">
                <p className="text-2xl font-semibold">
                  {
                    profile.bookings.filter((b) => b.status === "CONFIRMED")
                      .length
                  }
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {t("admin.studentProfile.stats.classes")}
                </p>
              </div>

              <button
                type="button"
                aria-expanded={tareasExpanded}
                onClick={() => setTareasExpanded((open) => !open)}
                className={`rounded-lg border bg-card p-4 text-center transition-colors hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${
                  tareasExpanded ? "ring-2 ring-primary/30" : ""
                }`}
              >
                <p className="text-2xl font-semibold">
                  {profile.homeworks.length}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {t("admin.studentProfile.stats.homework")}
                </p>
                <div className="mt-3">
                  <StudentHomeworkBreakdown
                    pending={homeworkBreakdown.pending}
                    submitted={homeworkBreakdown.submitted}
                    completed={homeworkBreakdown.completed}
                    compact
                  />
                </div>
                <p className="mt-2 text-[11px] text-muted-foreground">
                  {tareasExpanded
                    ? t("admin.studentProfile.collapseHomework")
                    : t("admin.studentProfile.expandHomework")}
                </p>
              </button>

              <div className="rounded-lg border bg-card p-4 text-center">
                <p className="text-2xl font-semibold">
                  {profile.presentations.length}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {t("admin.studentProfile.stats.presentations")}
                </p>
              </div>
            </div>

            {tareasExpanded && (
              <div className="mt-4">
                {profile.homeworks.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    {t("admin.studentProfile.emptyHomework")}
                  </p>
                ) : (
                  <div className="divide-y rounded-lg border">
                    {sortHomeworks(profile.homeworks).map((hw) => (
                      <div
                        key={hw.id}
                        className="flex items-center justify-between px-4 py-3 text-sm"
                      >
                        <span className="inline-flex items-center gap-1.5">
                          {hw.title}
                          {hw.unseen && (
                            <NotificationDot label={t("notification.row")} />
                          )}
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
                          {hw.status === "GRADED" &&
                            hw.scorePercent !== null && (
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
                              onClick={() =>
                                setOpenSubmissionId(hw.submissionId)
                              }
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
                                onClick={() =>
                                  setOpenSubmissionId(hw.submissionId)
                                }
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
                )}
              </div>
            )}
          </div>

          <div className="space-y-10">
            <Section
              title={t("admin.studentProfile.upcomingClasses")}
              count={upcoming.length}
            >
              {upcoming.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t("admin.studentProfile.emptyUpcoming")}
                </p>
              ) : (
                <div className="divide-y rounded-lg border">
                  {upcoming.map((b) => (
                    <div
                      key={b.id}
                      className="flex items-center justify-between px-4 py-3 text-sm"
                    >
                      <span className="capitalize">
                        {formatSlot(b.slotStart, b.slotEnd, teacherTimezone)}
                      </span>
                      {b.zoomJoinUrl && (
                        <a
                          href={b.zoomJoinUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-xs text-primary hover:underline ml-4 shrink-0"
                        >
                          Zoom
                        </a>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </Section>

            <Section
              title={t("admin.studentProfile.pastClasses")}
              count={past.length}
            >
              {past.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t("admin.studentProfile.emptyPast")}
                </p>
              ) : (
                <div className="divide-y rounded-lg border">
                  {past.map((b) => (
                    <div
                      key={b.id}
                      className="flex items-center justify-between px-4 py-3 text-sm text-muted-foreground"
                    >
                      <span className="capitalize">
                        {formatSlot(b.slotStart, b.slotEnd, teacherTimezone)}
                      </span>
                      <button
                        type="button"
                        onClick={() =>
                          handleToggleNoShow(
                            b.id,
                            !b.noShow,
                            b.isCompanionStudent,
                          )
                        }
                        className={`ml-4 shrink-0 rounded-full px-2 py-0.5 text-xs font-medium hover:underline ${
                          b.noShow
                            ? "bg-red-100 text-red-700"
                            : "bg-muted text-muted-foreground"
                        }`}
                      >
                        {b.noShow
                          ? t("admin.studentProfile.unmarkNoShow")
                          : t("admin.studentProfile.markNoShow")}
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </Section>

            <Section
              title={t("admin.studentProfile.sharedPresentations")}
              count={profile.presentations.length}
            >
              {profile.presentations.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t("admin.studentProfile.emptyPresentations")}
                </p>
              ) : (
                <div className="divide-y rounded-lg border">
                  {profile.presentations.map((p) => (
                    <div
                      key={p.id}
                      className="flex items-center justify-between px-4 py-3 text-sm"
                    >
                      <span>{p.title}</span>
                      {p.level && (
                        <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground ml-4 shrink-0">
                          {p.level}
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </Section>

            <Section
              title={t("quiz.admin.studentQuizzes")}
              count={quizzes.length}
            >
              {quizzes.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t("quiz.admin.noStudentQuizzes")}
                </p>
              ) : (
                <div className="space-y-3">
                  {quizzes.map((q) => (
                    <div key={q.attemptId} className="rounded-lg border p-3 text-sm">
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <p className="inline-flex items-center gap-1.5 font-medium">
                            {q.title}
                            {q.unseen && (
                              <NotificationDot label={t("notification.row")} />
                            )}
                          </p>
                          <p className="text-muted-foreground">
                            {t(`quiz.status.${q.status}` as never)}
                            {q.scorePercent != null
                              ? ` · ${q.scorePercent}%`
                              : ""}
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() =>
                            setOpenQuizAttempt({
                              quizId: q.quizId,
                              attemptId: q.attemptId,
                            })
                          }
                          className="shrink-0 text-xs font-medium text-primary hover:underline"
                        >
                          {q.status === "GRADED"
                            ? t("quiz.admin.view")
                            : t("quiz.admin.review")}
                        </button>
                      </div>
                      {q.skills.length > 0 && (
                        <div className="mt-2 grid grid-cols-2 gap-2 text-xs">
                          {q.skills.map((s) => (
                            <div key={s.skill} className="rounded border p-2">
                              <p className="font-medium">
                                {t(`quiz.skills.${s.skill}`)}
                              </p>
                              <p>
                                {s.awaitingTeacher
                                  ? t("quiz.skillAwaiting")
                                  : `${s.scorePercent}%`}
                              </p>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </Section>
          </div>
        </>
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
