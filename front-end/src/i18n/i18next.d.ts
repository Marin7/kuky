import type { esDict } from "./locales/es";

declare module "i18next" {
  interface CustomTypeOptions {
    defaultNS: "translation";
    resources: {
      translation: typeof esDict;
    };
  }
}
