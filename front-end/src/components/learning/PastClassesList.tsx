import type { PastClass } from "@/lib/learning";
import { Card, CardContent } from "@/components/ui/card";

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(iso));
}

interface PastClassesListProps {
  classes: PastClass[];
}

export function PastClassesList({ classes }: PastClassesListProps) {
  return (
    <section className="space-y-4">
      <h2 className="font-display text-xl font-bold text-foreground">
        Clases anteriores
      </h2>

      {classes.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          Aún no tienes clases anteriores. Aquí aparecerá un resumen de cada
          clase que completes.
        </p>
      ) : (
        <div className="space-y-3">
          {classes.map((c) => (
            <Card key={c.id} className="text-sm">
              <CardContent className="pt-4 space-y-1">
                <p className="font-medium text-foreground">{c.title}</p>
                <p className="text-xs text-muted-foreground capitalize">
                  {formatDate(c.heldOn)}
                </p>
                <p className="text-muted-foreground">{c.teacherNote}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </section>
  );
}
