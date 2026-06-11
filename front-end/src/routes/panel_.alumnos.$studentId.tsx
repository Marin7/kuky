import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getMe } from "@/lib/auth";
import {
  getStudentProfile,
  studentDisplayName,
  type StudentProfile,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/panel_/alumnos/$studentId")({
  component: StudentProfilePage,
});

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Pendiente",
  SUBMITTED: "Entregada",
  REVIEWED: "Revisada",
  CONFIRMED: "Confirmada",
  CANCELLED: "Cancelada",
};

function formatSlot(isoStart: string, isoEnd: string): string {
  const start = new Date(isoStart);
  const end = new Date(isoEnd);
  const datePart = new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "Europe/Madrid",
  }).format(start);
  const timePart = new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "Europe/Madrid",
  }).format(start);
  const endTime = new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "Europe/Madrid",
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
  const label = STATUS_LABEL[status] ?? status;
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
  const { studentId } = Route.useParams();
  const navigate = useNavigate();

  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
      .catch(() => setError("No se pudo cargar el perfil del alumno."))
      .finally(() => setLoading(false));
  }, [studentId]);

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
      {/* Back link */}
      <Link
        to="/panel"
        search={{ tab: "students" } as never}
        className="mb-8 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        ← Volver al panel
      </Link>

      {loading && (
        <p className="mt-6 text-sm text-muted-foreground animate-pulse">
          Cargando perfil…
        </p>
      )}

      {error && <p className="mt-6 text-sm text-destructive">{error}</p>}

      {profile && (
        <>
          {/* Header */}
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
              Alumno desde {formatDate(profile.createdAt)}
            </p>

            {/* Quick stats */}
            <div className="mt-6 grid grid-cols-3 gap-4">
              {[
                {
                  label: "Clases",
                  value: profile.bookings.filter(
                    (b) => b.status === "CONFIRMED",
                  ).length,
                },
                { label: "Tareas", value: profile.homeworks.length },
                {
                  label: "Presentaciones",
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
            {/* Upcoming classes */}
            <Section title="Próximas clases" count={upcoming.length}>
              {upcoming.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Ninguna próxima.
                </p>
              ) : (
                <div className="divide-y rounded-lg border">
                  {upcoming.map((b) => (
                    <div
                      key={b.id}
                      className="flex items-center justify-between px-4 py-3 text-sm"
                    >
                      <span className="capitalize">
                        {formatSlot(b.slotStart, b.slotEnd)}
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

            {/* Past classes */}
            <Section title="Clases pasadas" count={past.length}>
              {past.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Ninguna registrada.
                </p>
              ) : (
                <div className="divide-y rounded-lg border">
                  {past.map((b) => (
                    <div
                      key={b.id}
                      className="px-4 py-3 text-sm capitalize text-muted-foreground"
                    >
                      {formatSlot(b.slotStart, b.slotEnd)}
                    </div>
                  ))}
                </div>
              )}
            </Section>

            {/* Homeworks */}
            <Section title="Tareas" count={profile.homeworks.length}>
              {profile.homeworks.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Sin tareas asignadas.
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
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Section>

            {/* Presentations */}
            <Section
              title="Presentaciones compartidas"
              count={profile.presentations.length}
            >
              {profile.presentations.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Sin presentaciones compartidas.
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
          </div>
        </>
      )}
    </div>
  );
}
