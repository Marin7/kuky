import { useState } from "react";
import { useTranslation } from "react-i18next";
import type { SharedPresentationSummary } from "@/lib/learning";
import { downloadPresentation } from "@/lib/learning";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

interface Props {
  presentations: SharedPresentationSummary[];
}

export function SharedPresentationsList({ presentations }: Props) {
  const { t } = useTranslation();
  const [downloading, setDownloading] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleDownload = async (p: SharedPresentationSummary) => {
    setDownloading(p.id);
    setError(null);
    try {
      await downloadPresentation(p.id, `${p.title}.pptx`);
    } catch {
      setError(t("learning.presentations.loadError"));
    } finally {
      setDownloading(null);
    }
  };

  return (
    <section className="space-y-4">
      <h2 className="font-display text-xl font-bold">
        {t("learning.presentations.title")}
      </h2>
      {presentations.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("learning.presentations.empty")}
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
                    {downloading === p.id
                      ? t("learning.presentations.downloading")
                      : t("learning.presentations.download")}
                  </Button>
                ) : (
                  <span className="text-xs text-muted-foreground shrink-0 ml-3">
                    {t("learning.presentations.noFile")}
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
