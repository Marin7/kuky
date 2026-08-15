// @lovable.dev/vite-tanstack-config already includes the following — do NOT add them manually
// or the app will break with duplicate plugins:
//   - tanstackStart, viteReact, tailwindcss, tsConfigPaths, nitro (build-only using cloudflare as a default target),
//     componentTagger (dev-only), VITE_* env injection, @ path alias, React/TanStack dedupe,
//     error logger plugins, and sandbox detection (port/host/strictPort).
// You can pass additional config via defineConfig({ vite: { ... }, etc... }) if needed.
import { defineConfig } from "@lovable.dev/vite-tanstack-config";
import type { Plugin } from "vite";

/** pdf.js 6 uses DOMMatrix; never evaluate the modern build in Node SSR. */
function stubPdfjsForSsr(): Plugin {
  const stubId = "\0stub-pdfjs-ssr";
  return {
    name: "stub-pdfjs-ssr",
    enforce: "pre",
    resolveId(source, _importer, options) {
      if (!options?.ssr) return;
      const bare = source.split("?")[0];
      if (bare === "pdfjs-dist" || bare.startsWith("pdfjs-dist/")) {
        return stubId;
      }
    },
    load(id) {
      if (id !== stubId) return;
      return `
        export const GlobalWorkerOptions = { workerSrc: "" };
        export function getDocument() {
          return { promise: Promise.reject(new Error("pdfjs is client-only")), destroy() {} };
        }
        export class TextLayer { constructor() {} async render() {} cancel() {} }
        export class AnnotationLayer { constructor() {} async render() {} destroy() {} }
        export const LinkTarget = { BLANK: 2 };
        export class SimpleLinkService { constructor() {} setDocument() {} }
        export default "";
      `;
    },
  };
}

export default defineConfig({
  tanstackStart: {
    // Redirect TanStack Start's bundled server entry to src/server.ts (our SSR error wrapper).
    // nitro/vite builds from this
    server: { entry: "server" },
  },
  // Outside the Lovable sandbox this forces nitro to run (it otherwise no-ops)
  // and targets a plain Node server, since prod runs on a VM, not Cloudflare.
  nitro: { preset: "node-server" },
  // Keep peak RSS down for 2 GB VMs (Docker image build). Gzip size reporting
  // and high parallel file ops are the usual heap spikes on TanStack SSR builds.
  vite: {
    plugins: [stubPdfjsForSsr()],
    build: {
      reportCompressedSize: false,
      rollupOptions: {
        maxParallelFileOps: 2,
      },
    },
  },
});
