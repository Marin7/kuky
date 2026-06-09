import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { forgotPassword, resetPassword, type ApiError } from "@/lib/auth";

const emailSchema = z.object({
  email: z.string().email("El correo electrónico no es válido."),
});

const newPasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, "La contraseña debe tener al menos 8 caracteres."),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: "Las contraseñas no coinciden.",
    path: ["confirmPassword"],
  });

type EmailData = z.infer<typeof emailSchema>;
type NewPasswordData = z.infer<typeof newPasswordSchema>;

interface Props {
  token?: string;
  onSuccess: () => void;
}

export function PasswordResetForm({ token, onSuccess }: Props) {
  const isResetMode = !!token;

  return isResetMode ? (
    <NewPasswordForm token={token} onSuccess={onSuccess} />
  ) : (
    <ForgotPasswordForm onSuccess={onSuccess} />
  );
}

function ForgotPasswordForm({ onSuccess }: { onSuccess: () => void }) {
  const [submitted, setSubmitted] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EmailData>({ resolver: zodResolver(emailSchema) });

  const onSubmit = async (data: EmailData) => {
    await forgotPassword(data.email);
    setSubmitted(true);
  };

  if (submitted) {
    return (
      <div className="space-y-4 text-center">
        <p className="text-sm text-muted-foreground">
          Si existe una cuenta con ese correo, recibirás un enlace para
          restablecer tu contraseña. Revisa tu bandeja de entrada.
        </p>
        <Button variant="outline" onClick={onSuccess} className="w-full">
          Volver
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Introduce tu correo y te enviaremos un enlace para restablecer tu
        contraseña.
      </p>
      <div className="space-y-1">
        <Label htmlFor="forgot-email">Correo electrónico</Label>
        <Input
          id="forgot-email"
          type="email"
          placeholder="tu@correo.com"
          {...register("email")}
        />
        {errors.email && (
          <p className="text-sm text-destructive">{errors.email.message}</p>
        )}
      </div>
      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? "Enviando…" : "Enviar enlace"}
      </Button>
      <Button
        type="button"
        variant="outline"
        onClick={onSuccess}
        className="w-full"
      >
        Cancelar
      </Button>
    </form>
  );
}

function NewPasswordForm({
  token,
  onSuccess,
}: {
  token: string;
  onSuccess: () => void;
}) {
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<NewPasswordData>({ resolver: zodResolver(newPasswordSchema) });

  const onSubmit = async (data: NewPasswordData) => {
    setServerError(null);
    try {
      await resetPassword(token, data.newPassword);
      onSuccess();
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr.error === "INVALID_OR_EXPIRED_TOKEN") {
        setServerError(
          "El enlace de recuperación no es válido o ha expirado. Solicita uno nuevo.",
        );
      } else {
        setServerError("Ha ocurrido un error. Inténtalo de nuevo.");
      }
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Introduce tu nueva contraseña.
      </p>
      <div className="space-y-1">
        <Label htmlFor="new-password">Nueva contraseña</Label>
        <Input
          id="new-password"
          type="password"
          placeholder="Mínimo 8 caracteres"
          {...register("newPassword")}
        />
        {errors.newPassword && (
          <p className="text-sm text-destructive">
            {errors.newPassword.message}
          </p>
        )}
      </div>
      <div className="space-y-1">
        <Label htmlFor="confirm-new-password">Confirmar contraseña</Label>
        <Input
          id="confirm-new-password"
          type="password"
          placeholder="Repite tu contraseña"
          {...register("confirmPassword")}
        />
        {errors.confirmPassword && (
          <p className="text-sm text-destructive">
            {errors.confirmPassword.message}
          </p>
        )}
      </div>
      {serverError && <p className="text-sm text-destructive">{serverError}</p>}
      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? "Guardando…" : "Cambiar contraseña"}
      </Button>
    </form>
  );
}
