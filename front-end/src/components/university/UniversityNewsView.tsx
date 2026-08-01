import { useEffect, useState } from "react";
import { getUniversityNews, type UniversityNews } from "@/lib/university";

export function UniversityNewsView() {
  const [news, setNews] = useState<UniversityNews[] | null>(null);

  useEffect(() => {
    getUniversityNews().then(setNews).catch(() => setNews([]));
  }, []);

  if (!news) return <p className="animate-pulse text-muted-foreground">Cargando noticias…</p>;
  if (!news.length) return <p className="text-muted-foreground">No hay noticias publicadas por ahora.</p>;

  return (
    <div className="space-y-4">
      {news.map((item) => (
        <article key={item.id} className="rounded-lg border bg-card p-5">
          <p className="text-xs text-muted-foreground">
            {new Intl.DateTimeFormat("es", { dateStyle: "long" }).format(
              new Date(item.publishedAt),
            )}
          </p>
          <h2 className="mt-1 text-xl font-semibold">{item.title}</h2>
          <p className="mt-3 whitespace-pre-wrap text-muted-foreground">{item.body}</p>
        </article>
      ))}
    </div>
  );
}
