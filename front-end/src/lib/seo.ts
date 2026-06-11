// Centralised <head> metadata builder. Produces the full set of standard,
// Open Graph, and Twitter Card tags so shared links (WhatsApp, Instagram,
// Telegram, X) render a rich preview card instead of a bare URL.
//
// Routes call `seo({ title, description, path })` inside their `head()` and
// spread the result into `meta`. TanStack Router dedupes meta by name/property,
// so a route's tags override the site-wide defaults set in __root.tsx.

const SITE_NAME = "Español con Paula";

// Public base URL. Override per-environment with VITE_SITE_URL (e.g. a staging
// domain); falls back to the production origin. No trailing slash.
export const SITE_URL = (
  import.meta.env.VITE_SITE_URL ?? "https://kuky.es"
).replace(/\/+$/, "");

const DEFAULT_DESCRIPTION =
  "Clases de español personalizadas, 100% online, para estudiantes rumanos de todos los niveles.";

// Stable path served from front-end/public/. Absolute URL required by OG.
const OG_IMAGE = `${SITE_URL}/og-image.jpg`;

type SeoInput = {
  title: string;
  description?: string;
  /** Route path beginning with "/", used to build the canonical og:url. */
  path?: string;
};

export function seo({
  title,
  description = DEFAULT_DESCRIPTION,
  path = "/",
}: SeoInput) {
  const url = `${SITE_URL}${path}`;

  return [
    { title },
    { name: "description", content: description },

    // Open Graph
    { property: "og:type", content: "website" },
    { property: "og:site_name", content: SITE_NAME },
    { property: "og:title", content: title },
    { property: "og:description", content: description },
    { property: "og:url", content: url },
    { property: "og:image", content: OG_IMAGE },
    { property: "og:image:alt", content: "Paula, profesora de español" },
    { property: "og:locale", content: "es_ES" },

    // Twitter Card
    { name: "twitter:card", content: "summary_large_image" },
    { name: "twitter:title", content: title },
    { name: "twitter:description", content: description },
    { name: "twitter:image", content: OG_IMAGE },
  ];
}
