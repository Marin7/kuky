import { useEffect, useState } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { z } from "zod";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { RegistrationForm } from "@/components/auth/RegistrationForm";
import { LoginForm } from "@/components/auth/LoginForm";
import { PasswordResetForm } from "@/components/auth/PasswordResetForm";
import { getMe, logout as apiLogout, type UserResponse } from "@/lib/auth";

const searchSchema = z.object({
  token: z.string().optional(),
});

export const Route = createFileRoute("/cuenta")({
  validateSearch: searchSchema,
  head: () => ({
    meta: [
      { title: "Mi cuenta — Español con Paula" },
      {
        name: "description",
        content: "Gestiona tu cuenta y tu perfil de estudiante.",
      },
    ],
  }),
  component: CuentaPage,
});

type View = "tabs" | "forgot-password";

function CuentaPage() {
  const { token } = Route.useSearch();
  const navigate = useNavigate();

  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [view, setView] = useState<View>("tabs");
  const [activeTab, setActiveTab] = useState("register");

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setAuthLoading(false));
  }, []);

  const handleAuthSuccess = () => {
    getMe()
      .then(setUser)
      .catch(() => setUser(null));
  };

  const handleLogout = async () => {
    await apiLogout();
    setUser(null);
    setView("tabs");
    setActiveTab("login");
  };

  if (authLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <p className="text-muted-foreground text-sm">Cargando…</p>
      </div>
    );
  }

  // Password reset flow via ?token= URL param
  if (token) {
    return (
      <AuthCard title="Nueva contraseña">
        <PasswordResetForm
          token={token}
          onSuccess={() => {
            navigate({ to: "/cuenta" });
            handleAuthSuccess();
          }}
        />
      </AuthCard>
    );
  }

  // Authenticated dashboard
  if (user) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center space-y-4">
        <h1 className="font-display text-2xl font-bold">Mi cuenta</h1>
        <p className="text-muted-foreground text-sm">
          Has iniciado sesión como <strong>{user.email}</strong>
        </p>
        <Button variant="outline" onClick={handleLogout} className="w-full">
          Cerrar sesión
        </Button>
      </div>
    );
  }

  // Forgot password view
  if (view === "forgot-password") {
    return (
      <AuthCard title="Recuperar contraseña">
        <PasswordResetForm
          onSuccess={() => {
            setView("tabs");
            setActiveTab("login");
          }}
        />
      </AuthCard>
    );
  }

  // Default: register / login tabs
  return (
    <div className="mx-auto max-w-md px-4 py-16">
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="w-full mb-6">
          <TabsTrigger value="register" className="flex-1">
            Registrarse
          </TabsTrigger>
          <TabsTrigger value="login" className="flex-1">
            Iniciar sesión
          </TabsTrigger>
        </TabsList>

        <TabsContent value="register">
          <RegistrationForm
            onSuccess={() => {
              handleAuthSuccess();
            }}
          />
        </TabsContent>

        <TabsContent value="login">
          <LoginForm
            onSuccess={() => {
              handleAuthSuccess();
            }}
            onForgotPassword={() => setView("forgot-password")}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}

function AuthCard({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="mx-auto max-w-md px-4 py-16 space-y-6">
      <h1 className="font-display text-2xl font-bold text-center">{title}</h1>
      {children}
    </div>
  );
}
