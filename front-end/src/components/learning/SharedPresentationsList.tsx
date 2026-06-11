import { useState } from "react";
import type { SharedPresentationSummary } from "@/lib/learning";
import { downloadPresentation } from "@/lib/learning";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

interface Props {
  presentations: SharedPresentationSummary[];
}

export function SharedPresentationsList({ presentations }: Props) {
  const [downloading, setDownloading] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleDownload = async (p: SharedPresentationSummary) => {
    setDownloading(p.id);
    setError(null);
    try {
      await downloadPresentation(p.id, `${p.title}.pptx`);
    } catch {
      setError("No se pudo descargar la presentación.");
    } finally {
      setDownloading(null);
    }
  };

  return (
    <section className="space-y-4">
      <h2 className="font-display text-xl font-bold">Presentaciones</h2>
      {presentations.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Tu profesora aún no ha compartido ninguna presentación contigo.
        </p>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          {presentations.map((p) => (
            <Card key={p.id}>
              <CardContent className="flex items-center justify-between pt-4">
                <p className="font-medium truncate">{p.title}</p>
                {p.hasFile ? (
                  <Button
                    variant="outline"
                    size="sm"
                    className="shrink-0 ml-3"
                    disabled={downloading === p.id}
                    onClick={() => handleDownload(p)}
                  >
                    {downloading === p.id ? "Descargando…" : "Descargar"}
                  </Button>
                ) : (
                  <span className="text-xs text-muted-foreground shrink-0 ml-3">
                    Sin archivo
                  </span>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
      {error && <p className="text-sm text-destructive">{error}</p>}
    </section>
  );
}
