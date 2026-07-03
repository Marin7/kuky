import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe } from "@/lib/auth";
import {
  getStudentProfile,
  getStudentPlacementEvaluation,
  setBookingNoShow,
  studentDisplayName,
  type StudentProfile,
  type StudentPlacementEvaluation,
} from "@/lib/admin";
import { useTeacherTimezone } from "@/hooks/useTeacherTimezone";
import { Button } from "@/components/ui/button";
import { StudentHomeworkBreakdown } from "@/components/admin/students/StudentHomeworkBreakdown";
import { HomeworkReviewDialog } from "@/components/admin/homework/HomeworkReviewDialog";

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

function StudentProfilePage() {
  const { t } = useTranslation();
  const { studentId } = Route.useParams();
  const navigate = useNavigate();
  const teacherTimezone = useTeacherTimezone();

  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [placement, setPlacement] = useState<StudentPlacementEvaluation | null>(
    null,
  );
  const [openSubmissionId, setOpenSubmissionId] = useState<string | null>(null);

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
    getStudentPlacementEvaluation(studentId)
      .then(setPlacement)
      .catch(() => setPlacement(null));
  }, [studentId]);

  const handleToggleNoShow = (bookingId: string, noShow: boolean) => {
    setBookingNoShow(bookingId, noShow)
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
              {[
                {
                  label: t("admin.studentProfile.stats.classes"),
                  value: profile.bookings.filter(
                    (b) => b.status === "CONFIRMED",
                  ).length,
                },
                {
                  label: t("admin.studentProfile.stats.homework"),
                  value: profile.homeworks.length,
                },
                {
                  label: t("admin.studentProfile.stats.presentations"),
                  value: profile.presentations.length,
                },
              ].map((stat) => (
                <div
                  key={stat.label}
                  className="rounded-lg border bg-card p-4 text-center"
                >
                  <p className="text-2xl font-semibold">{stat.value}</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    {stat.label}
                  </p>
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-10">
            <Section
              title={t("admin.studentProfile.progress.title")}
              count={profile.progress.units.length}
            >
              {profile.progress.units.length === 0 &&
              profile.progress.homeworkBreakdown.pending === 0 &&
              profile.progress.homeworkBreakdown.submitted === 0 &&
              profile.progress.homeworkBreakdown.completed === 0 &&
              profile.progress.attendedClasses === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t("admin.studentProfile.progress.empty")}
                </p>
              ) : (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                    <div className="rounded-lg border bg-card p-3 text-center">
                      <p className="text-lg font-semibold">
                        {profile.progress.attendedClasses}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {t("admin.studentProfile.progress.attendedClasses")}
                      </p>
                    </div>
                    <div className="rounded-lg border bg-card p-3 text-center">
                      <p className="text-lg font-semibold">
                        {placement?.result?.overallCefr ??
                          t("admin.studentProfile.progress.noLevel")}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {t("admin.studentProfile.progress.level")}
                      </p>
                    </div>
                  </div>

                  {profile.progress.units.length > 0 && (
                    <div>
                      <p className="mb-2 text-xs font-medium text-muted-foreground">
                        {t("admin.studentProfile.progress.units")}
                      </p>
                      <div className="divide-y rounded-lg border">
                        {profile.progress.units.map((u) => (
                          <div
                            key={u.unitId}
                            className="flex items-center justify-between px-4 py-3 text-sm"
                          >
                            <span>
                              {u.subject}{" "}
                              <span className="text-xs text-muted-foreground">
                                ({u.level})
                              </span>
                            </span>
                            <span className="flex items-center gap-2 ml-4 shrink-0">
                              <span className="text-xs text-muted-foreground">
                                {u.completedHomeworks}/{u.totalHomeworks}
                              </span>
                              <span
                                className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                  u.complete
                                    ? "bg-green-100 text-green-700"
                                    : "bg-muted text-muted-foreground"
                                }`}
                              >
                                {u.complete
                                  ? t(
                                      "admin.studentProfile.progress.unitComplete",
                                    )
                                  : t(
                                      "admin.studentProfile.progress.unitInProgress",
                                    )}
                              </span>
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  <StudentHomeworkBreakdown
                    pending={profile.progress.homeworkBreakdown.pending}
                    submitted={profile.progress.homeworkBreakdown.submitted}
                    completed={profile.progress.homeworkBreakdown.completed}
                  />
                </div>
              )}
            </Section>

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
                        onClick={() => handleToggleNoShow(b.id, !b.noShow)}
                        className={`ml-4 shrink-0 rounded-full px-2 py-0.5 text-xs font-medium hover:underline ${
                          b.noShow
                            ? "bg-red-100 text-red-700"
                            : "bg-muted text-muted-foreground"
                        }`}
                      >
                        {b.noShow
                          ? t("admin.studentProfile.progress.unmarkNoShow")
                          : t("admin.studentProfile.progress.markNoShow")}
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </Section>

            <Section
              title={t("admin.studentProfile.homework")}
              count={profile.homeworks.length}
            >
              {profile.homeworks.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t("admin.studentProfile.emptyHomework")}
                </p>
              ) : (
                <div className="divide-y rounded-lg border">
                  {profile.homeworks.map((hw) => (
                    <div
                      key={hw.id}
                      className="flex items-center justify-between px-4 py-3 text-sm"
                    >
                      <span>{hw.title}</span>
                      <div className="flex items-center gap-2 ml-4 shrink-0">
                        {hw.submittedAt && (
                          <span className="text-xs text-muted-foreground">
                            {formatDate(hw.submittedAt)}
                          </span>
                        )}
                        <StatusBadge status={hw.status} />
                        {hw.needsReview && hw.submissionId && (
                          <button
                            type="button"
                            onClick={() => setOpenSubmissionId(hw.submissionId)}
                            className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 hover:underline"
                          >
                            {t("admin.homeworkReview.needsReviewBadge")}
                          </button>
                        )}
                      </div>
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

            {placement && (
              <Section
                title={t("placement.admin.studentEvaluation.title")}
                count={placement.writing.length}
              >
                {placement.result ? (
                  <div className="mb-4 rounded-lg border bg-card p-4">
                    <p className="text-sm font-medium">
                      {t("placement.admin.studentEvaluation.overall")}:{" "}
                      {placement.result.overallCefr ?? "—"}
                    </p>
                    <div className="mt-2 grid grid-cols-3 gap-2 text-center text-xs">
                      {placement.result.skills.map((s) => (
                        <div key={s.skill} className="rounded border p-2">
                          <p className="font-medium">{s.skill}</p>
                          <p>
                            {s.cefrLevel} ({s.scorePercent}%)
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    {t("placement.admin.studentEvaluation.noResult")}
                  </p>
                )}

                {placement.writing.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    {t("placement.admin.studentEvaluation.noWriting")}
                  </p>
                ) : (
                  <div className="space-y-3">
                    {placement.writing.map((w) => (
                      <div
                        key={w.id}
                        className="rounded-lg border bg-muted/40 p-3 text-sm"
                      >
                        <p className="mb-1 text-xs text-muted-foreground">
                          {formatDate(w.submittedAt)}
                        </p>
                        <p className="whitespace-pre-line">{w.body}</p>
                      </div>
                    ))}
                  </div>
                )}
              </Section>
            )}
          </div>
        </>
      )}

      {openSubmissionId && (
        <HomeworkReviewDialog
          submissionId={openSubmissionId}
          onClose={() => setOpenSubmissionId(null)}
          onReviewed={() => {
            setOpenSubmissionId(null);
            getStudentProfile(studentId).then(setProfile);
          }}
        />
      )}
    </div>
  );
}
