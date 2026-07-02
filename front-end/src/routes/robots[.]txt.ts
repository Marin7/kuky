import { createFileRoute } from "@tanstack/react-router";
import type {} from "@tanstack/react-start";
import { SITE_URL } from "@/lib/seo";

export const Route = createFileRoute("/robots.txt")({
  server: {
    handlers: {
      GET: async () => {
        const body = [
          "User-agent: *",
          "Allow: /",
          "Disallow: /panel",
          "Disallow: /aprendizaje",
          "Disallow: /prueba-de-nivel",
          "",
          `Sitemap: ${SITE_URL}/sitemap.xml`,
          "",
        ].join("\n");
        return new Response(body, {
          headers: { "Content-Type": "text/plain" },
        });
      },
    },
  },
});
