import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { login as apiLogin, type ApiError } from "@/lib/auth";

const schema = z.object({
  email: z.string().min(1, "El correo electrónico es obligatorio."),
  password: z.string().min(1, "La contraseña es obligatoria."),
});

type FormData = z.infer<typeof schema>;

interface Props {
  onSuccess: () => void;
  onForgotPassword: () => void;
}

export function LoginForm({ onSuccess, onForgotPassword }: Props) {
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setServerError(null);
    try {
      await apiLogin(data.email, data.password);
      onSuccess();
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr.error === "RATE_LIMIT_EXCEEDED") {
        setServerError("Demasiados intentos. Por favor, espera un momento e inténtalo de nuevo.");
      } else {
        setServerError("Correo electrónico o contraseña incorrectos.");
      }
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="space-y-1">
        <Label htmlFor="login-email">Correo electrónico</Label>
        <Input id="login-email" type="email" placeholder="tu@correo.com" {...register("email")} />
        {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
      </div>

      <div className="space-y-1">
        <Label htmlFor="login-password">Contraseña</Label>
        <Input id="login-password" type="password" placeholder="Tu contraseña" {...register("password")} />
        {errors.password && <p className="text-sm text-destructive">{errors.password.message}</p>}
      </div>

      {serverError && <p className="text-sm text-destructive">{serverError}</p>}

      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? "Iniciando sesión…" : "Iniciar sesión"}
      </Button>

      <button
        type="button"
        onClick={onForgotPassword}
        className="w-full text-sm text-muted-foreground hover:text-foreground underline"
      >
        ¿Olvidaste tu contraseña?
      </button>
    </form>
  );
}
