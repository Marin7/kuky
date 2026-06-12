import { useTranslation } from "react-i18next";
import { type Lang, storeLang } from "@/i18n";

export function useLanguage() {
  const { i18n } = useTranslation();

  const setLanguage = (lang: Lang) => {
    i18n.changeLanguage(lang);
    storeLang(lang);
    if (typeof document !== "undefined") {
      document.documentElement.lang = lang;
    }
  };

  return {
    language: i18n.language as Lang,
    setLanguage,
  };
}
