import { useEffect, useState } from "react";
import { Link, useRouterState } from "@tanstack/react-router";
import { Menu, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
  Sheet,
  SheetContent,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { getMe } from "@/lib/auth";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import logoUrl from "@/assets/logo.png";

export function SiteHeader() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  useEffect(() => {
    let active = true;
    const check = () =>
      getMe()
        .then((me) => {
          if (!active) return;
          setAuthed(true);
          setIsAdmin(me.role === "ADMIN");
        })
        .catch(() => {
          if (!active) return;
          setAuthed(false);
          setIsAdmin(false);
        });
    check();
    window.addEventListener("auth-changed", check);
    return () => {
      active = false;
      window.removeEventListener("auth-changed", check);
    };
  }, [pathname]);

  const nav = [
    { to: "/", label: t("nav.home") },
    ...(isAdmin
      ? []
      : [
          { to: "/sobre-mi", label: t("nav.about") },
          ...(authed
            ? [{ to: "/reservas", label: t("nav.schedule") }]
            : []),
        ]),
    ...(authed && !isAdmin
      ? [{ to: "/aprendizaje", label: t("nav.learning") }]
      : []),
    ...(isAdmin ? [{ to: "/panel", label: t("nav.panel") }] : []),
    { to: "/cuenta", label: t("nav.account") },
  ] as const;

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border/60 bg-background/80 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-6 py-2.5">
        <Link to="/" className="flex shrink-0 items-center">
          <img
            src={logoUrl}
            alt={t("nav.brand")}
            className="h-12 w-auto max-w-[240px] object-contain sm:h-14 sm:max-w-[300px] md:h-16 md:max-w-[340px]"
          />
        </Link>

        {/* Desktop nav */}
        <nav className="hidden gap-1 text-base md:flex">
          {nav.map((n) => (
            <Link
              key={n.to}
              to={n.to}
              className="rounded-md px-3 py-1.5 text-muted-foreground transition-colors hover:bg-accent/30 hover:text-foreground"
              activeProps={{
                className:
                  "rounded-md px-3 py-1.5 text-foreground font-medium",
              }}
              activeOptions={{ exact: n.to === "/" }}
            >
              {n.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-1">
          <LanguageSwitcher />

          {/* Mobile nav */}
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild>
              <button
                className="inline-flex items-center justify-center rounded-md p-2 text-muted-foreground hover:bg-accent/30 hover:text-foreground md:hidden"
                aria-label={t("nav.openMenu")}
              >
                {open ? (
                  <X className="h-5 w-5" />
                ) : (
                  <Menu className="h-5 w-5" />
                )}
              </button>
            </SheetTrigger>
            <SheetContent side="right" className="w-64 pt-10">
              <SheetTitle className="sr-only">{t("nav.menuTitle")}</SheetTitle>
              <nav className="flex flex-col gap-1">
                {nav.map((n) => (
                  <Link
                    key={n.to}
                    to={n.to}
                    onClick={() => setOpen(false)}
                    className="rounded-md px-3 py-2 text-base text-muted-foreground transition-colors hover:bg-accent/30 hover:text-foreground"
                    activeProps={{
                      className:
                        "rounded-md px-3 py-2 text-base bg-accent/20 text-foreground font-medium",
                    }}
                    activeOptions={{ exact: n.to === "/" }}
                  >
                    {n.label}
                  </Link>
                ))}
                <div className="pt-2 border-t border-border/40 mt-2">
                  <LanguageSwitcher />
                </div>
              </nav>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}

export function SiteFooter() {
  const { t } = useTranslation();
  return (
    <footer className="mt-24 border-t border-border/60 bg-secondary/40">
      <div className="mx-auto max-w-6xl px-6 py-10 text-sm text-muted-foreground">
        © {new Date().getFullYear()} {t("nav.brand")} — {t("nav.footer")}
      </div>
    </footer>
  );
}
