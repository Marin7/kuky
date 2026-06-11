import { useEffect, useRef, useState } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { z } from "zod";
import { Camera } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RegistrationForm } from "@/components/auth/RegistrationForm";
import { LoginForm } from "@/components/auth/LoginForm";
import { PasswordResetForm } from "@/components/auth/PasswordResetForm";
import {
  activate,
  getMe,
  logout as apiLogout,
  resendActivation,
  updateProfile,
  uploadAvatar,
  type ApiError,
  type UserResponse,
} from "@/lib/auth";
import { seo } from "@/lib/seo";

const IMAGE_BASE = "http://localhost:8081/api/v1/images";

const searchSchema = z.object({
  token: z.string().optional(),
  activateToken: z.string().optional(),
});

export const Route = createFileRoute("/cuenta")({
  validateSearch: searchSchema,
  head: () => ({
    meta: seo({
      title: "Mi cuenta — Español con Paula",
      description: "Gestiona tu cuenta y tu perfil de estudiante.",
      path: "/cuenta",
    }),
  }),
  component: CuentaPage,
});

function displayName(user: UserResponse): string {
  if (user.firstName && user.lastName) return `${user.firstName} ${user.lastName}`;
  if (user.firstName) return user.firstName;
  if (user.username) return `@${user.username}`;
  return user.email.split("@")[0];
}

function initials(user: UserResponse): string {
  if (user.firstName && user.lastName)
    return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
  if (user.firstName) return user.firstName[0].toUpperCase();
  if (user.username) return user.username[0].toUpperCase();
  return user.email[0].toUpperCase();
}

type View = "tabs" | "forgot-password";

function CuentaPage() {
  const { token, activateToken } = Route.useSearch();
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

  // Activation link — handle before all other states
  if (activateToken) {
    return (
      <ActivateView
        token={activateToken}
        onSuccess={() => {
          navigate({ to: "/cuenta" });
          handleAuthSuccess();
        }}
      />
    );
  }

  // Password reset flow
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

  // Pending activation — account created but not yet confirmed
  if (user && user.status === "PENDING") {
    return (
      <PendingActivationView
        email={user.email}
        onActivated={handleAuthSuccess}
      />
    );
  }

  // Authenticated profile
  if (user) {
    return (
      <div className="mx-auto max-w-md px-4 py-12">
        <ProfileView
          user={user}
          onUpdated={handleAuthSuccess}
          onLogout={handleLogout}
        />
      </div>
    );
  }

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
          <RegistrationForm onSuccess={handleAuthSuccess} />
        </TabsContent>

        <TabsContent value="login">
          <LoginForm
            onSuccess={handleAuthSuccess}
            onForgotPassword={() => setView("forgot-password")}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}

// ---------- Activation views ----------

function ActivateView({
  token,
  onSuccess,
}: {
  token: string;
  onSuccess: () => void;
}) {
  const [state, setState] = useState<"activating" | "error">("activating");
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    activate(token)
      .then(() => onSuccess())
      .catch((err: ApiError) => {
        setState("error");
        setErrorMsg(err.message ?? "El enlace no es válido o ha expirado.");
      });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  if (state === "activating") {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <p className="text-muted-foreground text-sm">Activando tu cuenta…</p>
      </div>
    );
  }

  return (
    <AuthCard title="Enlace no válido">
      <p className="text-center text-sm text-destructive">{errorMsg}</p>
      <ResendForm />
    </AuthCard>
  );
}

function PendingActivationView({
  email,
  onActivated,
}: {
  email: string;
  onActivated: () => void;
}) {
  const [resent, setResent] = useState(false);
  const [resending, setResending] = useState(false);

  const handleResend = async () => {
    setResending(true);
    try {
      await resendActivation(email);
      setResent(true);
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="mx-auto max-w-md px-4 py-16 text-center space-y-4">
      <h1 className="font-display text-2xl font-bold">Revisa tu correo</h1>
      <p className="text-muted-foreground text-sm">
        Hemos enviado un enlace de activación a{" "}
        <strong>{email}</strong>. Haz clic en él para activar tu cuenta.
      </p>
      {resent ? (
        <p className="text-sm text-green-600">
          Correo reenviado. Revisa tu bandeja de entrada.
        </p>
      ) : (
        <Button variant="outline" onClick={handleResend} disabled={resending}>
          {resending ? "Reenviando…" : "Reenviar correo de activación"}
        </Button>
      )}
      <p className="text-xs text-muted-foreground pt-2">
        ¿Ya activaste tu cuenta?{" "}
        <button
          type="button"
          className="underline hover:text-foreground"
          onClick={onActivated}
        >
          Haz clic aquí para continuar
        </button>
      </p>
    </div>
  );
}

/** Small resend form shown when an activation link has expired. */
function ResendForm() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSending(true);
    try {
      await resendActivation(email.trim());
      setSent(true);
    } finally {
      setSending(false);
    }
  };

  if (sent) {
    return (
      <p className="text-center text-sm text-green-600">
        Si tu cuenta está pendiente, recibirás un nuevo enlace.
      </p>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <p className="text-sm text-center text-muted-foreground">
        Introduce tu correo para recibir un nuevo enlace de activación.
      </p>
      <Input
        type="email"
        placeholder="tu@correo.com"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
      />
      <Button type="submit" className="w-full" disabled={sending}>
        {sending ? "Enviando…" : "Reenviar enlace"}
      </Button>
    </form>
  );
}

// ---------- Profile view ----------

function ProfileView({
  user,
  onUpdated,
  onLogout,
}: {
  user: UserResponse;
  onUpdated: () => void;
  onLogout: () => void;
}) {
  const [firstName, setFirstName] = useState(user.firstName ?? "");
  const [lastName, setLastName] = useState(user.lastName ?? "");
  const [username, setUsername] = useState(user.username ?? "");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const avatarUrl = user.avatarImageId
    ? `${IMAGE_BASE}/${user.avatarImageId}`
    : null;

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSaveError(null);
    setSaveSuccess(false);
    try {
      await updateProfile({
        firstName: firstName.trim() || null,
        lastName: lastName.trim() || null,
        username: username.trim() || null,
      });
      onUpdated();
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err) {
      setSaveError((err as ApiError).message ?? "Error al guardar.");
    } finally {
      setSaving(false);
    }
  };

  const handleAvatarChange = async (
    e: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      await uploadAvatar(file);
      onUpdated();
    } catch (err) {
      setSaveError((err as ApiError).message ?? "Error al subir la imagen.");
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  return (
    <div className="space-y-8">
      <div className="text-center space-y-3">
        <div
          className="relative mx-auto w-20 h-20 cursor-pointer group"
          onClick={() => fileInputRef.current?.click()}
          title="Cambiar foto de perfil"
        >
          {avatarUrl ? (
            <img
              src={avatarUrl}
              alt={displayName(user)}
              className="w-20 h-20 rounded-full object-cover border-2 border-border"
            />
          ) : (
            <div className="w-20 h-20 rounded-full bg-primary/10 border-2 border-border flex items-center justify-center text-2xl font-semibold text-primary select-none">
              {initials(user)}
            </div>
          )}
          <div className="absolute inset-0 rounded-full bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
            {uploading ? (
              <span className="text-white text-xs">Subiendo…</span>
            ) : (
              <Camera className="text-white h-5 w-5" />
            )}
          </div>
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          className="hidden"
          onChange={handleAvatarChange}
        />
        <div>
          <h1 className="font-display text-2xl font-bold">
            {displayName(user)}
          </h1>
          <p className="text-muted-foreground text-sm">{user.email}</p>
        </div>
      </div>

      <form onSubmit={handleSave} className="space-y-4">
        <p className="text-xs font-semibold text-muted-foreground uppercase tracking-widest">
          Editar perfil
        </p>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="firstName">Nombre</Label>
            <Input
              id="firstName"
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              placeholder="Tu nombre"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="lastName">Apellidos</Label>
            <Input
              id="lastName"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              placeholder="Tus apellidos"
            />
          </div>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="username">
            Nombre de usuario{" "}
            <span className="font-normal text-muted-foreground">
              (opcional si tienes nombre y apellidos)
            </span>
          </Label>
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm pointer-events-none">
              @
            </span>
            <Input
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="pl-7"
              placeholder="nombre_usuario"
            />
          </div>
        </div>

        {saveError && (
          <p className="text-sm text-destructive">{saveError}</p>
        )}
        {saveSuccess && (
          <p className="text-sm text-green-600">Perfil actualizado correctamente.</p>
        )}

        <Button type="submit" disabled={saving} className="w-full">
          {saving ? "Guardando…" : "Guardar cambios"}
        </Button>
      </form>

      <Button variant="outline" onClick={onLogout} className="w-full">
        Cerrar sesión
      </Button>
    </div>
  );
}

// ---------- Shared ----------

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
