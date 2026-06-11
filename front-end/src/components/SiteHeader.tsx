import { useEffect, useState } from "react";
import { Link, useRouterState } from "@tanstack/react-router";
import { Menu, X } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { getMe } from "@/lib/auth";

const baseNav = [
  { to: "/", label: "Inicio" },
  { to: "/sobre-mi", label: "Sobre mí" },
  { to: "/reservas", label: "Reservas" },
] as const;

// Visible only to logged-in users
const learningNav = { to: "/aprendizaje", label: "Mi aprendizaje" } as const;
// Visible only to the teacher/admin
const panelNav = { to: "/panel", label: "Panel" } as const;
const accountNav = { to: "/cuenta", label: "Mi cuenta" } as const;

export function SiteHeader() {
  const [open, setOpen] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  // Re-check auth on every navigation so the protected link stays in sync.
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
    // Update immediately on login/logout without a full reload.
    window.addEventListener("auth-changed", check);
    return () => {
      active = false;
      window.removeEventListener("auth-changed", check);
    };
  }, [pathname]);

  const nav = [
    ...baseNav,
    ...(authed ? [learningNav] : []),
    ...(isAdmin ? [panelNav] : []),
    accountNav,
  ];

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border/60 bg-background/80 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link
          to="/"
          className="font-display text-xl font-semibold text-primary"
        >
          Español con Paula
        </Link>

        {/* Desktop nav */}
        <nav className="hidden gap-7 text-sm md:flex">
          {nav.map((n) => (
            <Link
              key={n.to}
              to={n.to}
              className="text-muted-foreground transition-colors hover:text-foreground"
              activeProps={{ className: "text-foreground font-medium" }}
              activeOptions={{ exact: n.to === "/" }}
            >
              {n.label}
            </Link>
          ))}
        </nav>

        {/* Mobile nav */}
        <Sheet open={open} onOpenChange={setOpen}>
          <SheetTrigger asChild>
            <button
              className="inline-flex items-center justify-center rounded-md p-2 text-muted-foreground hover:bg-accent/30 hover:text-foreground md:hidden"
              aria-label="Abrir menú"
            >
              {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
          </SheetTrigger>
          <SheetContent side="right" className="w-64 pt-10">
            <SheetTitle className="sr-only">Menú de navegación</SheetTitle>
            <nav className="flex flex-col gap-1">
              {nav.map((n) => (
                <Link
                  key={n.to}
                  to={n.to}
                  onClick={() => setOpen(false)}
                  className="rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-accent/30 hover:text-foreground"
                  activeProps={{
                    className:
                      "rounded-md px-3 py-2 text-sm bg-accent/20 text-foreground font-medium",
                  }}
                  activeOptions={{ exact: n.to === "/" }}
                >
                  {n.label}
                </Link>
              ))}
            </nav>
          </SheetContent>
        </Sheet>
      </div>
    </header>
  );
}

export function SiteFooter() {
  return (
    <footer className="mt-24 border-t border-border/60 bg-secondary/40">
      <div className="mx-auto max-w-6xl px-6 py-10 text-sm text-muted-foreground">
        © {new Date().getFullYear()} Español con Paula — Recursos y clases para
        profesores de español.
      </div>
    </footer>
  );
}
