import { createFileRoute } from "@tanstack/react-router";
import teacherUrl from "@/assets/teacher.jpg";

export const Route = createFileRoute("/sobre-mi")({
  head: () => ({
    meta: [
      { title: "Sobre mí — Español con Paula" },
      { name: "description", content: "Conoce a Paula, profesora de español dedicada a ayudar a estudiantes rumanos a dominar el idioma." },
    ],
  }),
  component: SobreMi,
});

function SobreMi() {
  return (
    <div className="mx-auto max-w-5xl px-6 py-20">
      <div className="grid gap-12 md:grid-cols-[2fr_3fr] md:items-start">
        <img
          src={teacherUrl}
          alt="Paula, profesora de español"
          className="aspect-[4/5] w-full rounded-2xl object-cover shadow-lg"
        />
        <div>
          <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-primary">
            Sobre mí
          </span>
          <h1 className="mt-4 font-display text-4xl font-semibold md:text-5xl">¡Hola! Soy Paula.</h1>
          <div className="mt-6 space-y-4 text-muted-foreground">
            <p>
              Soy profesora de español con años de experiencia ayudando a estudiantes rumanos a descubrir y dominar este precioso idioma. Mi pasión es crear un espacio cálido donde aprender se sienta natural y divertido.
            </p>
            <p>
              Creo en clases dinámicas, conversación real desde el primer día y materiales que se adaptan a cada estudiante. Trabajo con todos los niveles, desde principiantes absolutos hasta hablantes avanzados que quieren pulir su fluidez.
            </p>
            <p>
              Mi objetivo no es solo enseñarte gramática y vocabulario — quiero que te sientas seguro hablando español en cualquier situación, ya sea para viajar, trabajar o conectar con la cultura hispanohablante.
            </p>
          </div>

          <div className="mt-10 grid gap-4 sm:grid-cols-2">
            {[
              { k: "Niveles", v: "A2 – C2" },
              { k: "Modalidad", v: "100% online" },
              { k: "Idiomas", v: "Español y rumano" },
              { k: "Enfoque", v: "Conversación + gramática" },
            ].map((i) => (
              <div key={i.k} className="rounded-lg border border-border bg-card p-4">
                <div className="text-xs uppercase tracking-wider text-muted-foreground">{i.k}</div>
                <div className="mt-1 font-medium">{i.v}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
