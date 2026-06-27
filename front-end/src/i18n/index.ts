import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import { esDict } from "./locales/es";
import { roDict } from "./locales/ro";
import { enDict } from "./locales/en";

export type Lang = "es" | "ro" | "en";

export const LANGUAGES: { code: Lang; countryCode: string; label: string }[] = [
  { code: "es", countryCode: "es", label: "ES" },
  { code: "ro", countryCode: "ro", label: "RO" },
  { code: "en", countryCode: "gb", label: "EN" },
];

const STORAGE_KEY = "kuky-lang";
const VALID_LANGS: Lang[] = ["es", "ro", "en"];

// Falls back to "es" for SSR (no localStorage), missing key, or any unrecognised value.
export function getStoredLang(): Lang {
  if (typeof localStorage === "undefined") return "es";
  const stored = localStorage.getItem(STORAGE_KEY);
  return (VALID_LANGS.includes(stored as Lang) ? stored : "es") as Lang;
}

export function storeLang(lang: Lang): void {
  if (typeof localStorage !== "undefined") {
    localStorage.setItem(STORAGE_KEY, lang);
  }
}

i18n.use(initReactI18next).init({
  lng: "es",
  fallbackLng: "es",
  defaultNS: "translation",
  interpolation: { escapeValue: false },
  resources: {
    es: { translation: esDict },
    ro: { translation: roDict },
    en: { translation: enDict },
  },
});

export default i18n;
