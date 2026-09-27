import { useTranslation } from "react-i18next";
import type { StudentProfileBooking } from "@/lib/admin";

interface Props {
  upcoming: StudentProfileBooking[];
  past: StudentProfileBooking[];
  teacherTimezone: string;
  onToggleNoShow: (
    bookingId: string,
    noShow: boolean,
    isCompanionStudent: boolean,
  ) => void;
}

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

/** Expanded "Clases" box: upcoming and past classes, with the no-show toggle. */
export function StudentClassesPanel({
  upcoming,
  past,
  teacherTimezone,
  onToggleNoShow,
}: Props) {
  const { t } = useTranslation();
  return (
    <div className="space-y-6">
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
                    onToggleNoShow(b.id, !b.noShow, b.isCompanionStudent)
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
    </div>
  );
}
