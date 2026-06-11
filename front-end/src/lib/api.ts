// Single source of truth for the backend origin.
// In dev this falls back to localhost:8081.
// In production set VITE_API_BASE_URL=https://api.kuky.es (no trailing slash).
export const API_ORIGIN: string =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";
