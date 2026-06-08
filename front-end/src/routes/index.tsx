import { createFileRoute, Link } from "@tanstack/react-router";
import teacherUrl from "@/assets/teacher.jpg";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Español con Paula — Clases de español para rumanos" },
      { name: "description", content: "Clases de español personalizadas, 100% online, para estudiantes rumanos de todos los niveles." },
    ],
  }),
  component: Index,
});

function Index() {
  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-br from-secondary/60 via-background to-accent/20" />
        <div className="mx-auto grid max-w-6xl gap-12 px-6 py-20 md:grid-cols-2 md:items-center md:py-28">
          <div>
            <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-primary">
              Clases para estudiantes rumanos
            </span>
            <h1 className="mt-5 font-display text-5xl font-semibold leading-tight md:text-6xl">
              Aprende español <span className="text-primary italic">con confianza</span>.
            </h1>
            <p className="mt-5 max-w-lg text-lg text-muted-foreground">
              Clases personalizadas, conversación real y un método pensado para hablantes de rumano. Da el siguiente paso en tu español hoy.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/reservas" className="rounded-md bg-primary px-5 py-3 text-sm font-medium text-primary-foreground transition hover:bg-primary/90">
                Reservar una clase
              </Link>
              <Link to="/sobre-mi" className="rounded-md border border-border bg-background px-5 py-3 text-sm font-medium hover:bg-accent/30">
                Conóceme
              </Link>
            </div>
          </div>
          <div className="relative">
            <div className="absolute -inset-4 -z-10 rounded-3xl bg-accent/40 blur-2xl" />
            <img
              src={teacherUrl}
              alt="Paula, profesora de español"
              className="aspect-[4/5] w-full rounded-2xl object-cover shadow-xl"
            />
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="mx-auto max-w-6xl px-6 py-16">
        <div className="grid gap-8 md:grid-cols-3">
          {[
            { t: "Método personalizado", d: "Cada clase se adapta a tu nivel, tus objetivos y tu ritmo." },
            { t: "Pensado para rumanos", d: "Aprovechamos las similitudes entre el rumano y el español para avanzar más rápido." },
            { t: "Materiales incluidos", d: "Recibes ejercicios, audios y lecturas para practicar entre clases." },
          ].map((f) => (
            <div key={f.t} className="rounded-xl border border-border bg-card p-6">
              <h3 className="font-display text-xl font-semibold">{f.t}</h3>
              <p className="mt-2 text-sm text-muted-foreground">{f.d}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="mx-auto max-w-6xl px-6 py-16">
        <div className="rounded-2xl bg-primary px-8 py-12 text-center text-primary-foreground md:py-16">
          <h2 className="font-display text-3xl font-semibold md:text-4xl">¿Listo para empezar?</h2>
          <p className="mx-auto mt-3 max-w-xl text-primary-foreground/80">
            Reserva tu primera clase de prueba y descubre cómo el español puede formar parte de tu día a día.
          </p>
          <Link to="/reservas" className="mt-6 inline-block rounded-md bg-background px-5 py-3 text-sm font-medium text-foreground hover:bg-secondary">
            Reservar ahora
          </Link>
        </div>
      </section>
    </div>
  );
}
