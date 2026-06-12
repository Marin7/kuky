import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { forgotPassword, resetPassword, type ApiError } from "@/lib/auth";

type EmailData = { email: string };
type NewPasswordData = { newPassword: string; confirmPassword: string };

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
  const { t } = useTranslation();
  const [submitted, setSubmitted] = useState(false);

  const emailSchema = z.object({
    email: z
      .string()
      .min(1, t("account.passwordResetForm.emailRequired"))
      .email(t("account.passwordResetForm.emailRequired")),
  });

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
          {t("account.passwordResetForm.linkSent")}
        </p>
        <Button variant="outline" onClick={onSuccess} className="w-full">
          {t("account.passwordResetForm.back")}
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <p className="text-sm text-muted-foreground">
        {t("account.passwordResetForm.forgotDescription")}
      </p>
      <div className="space-y-1">
        <Label htmlFor="forgot-email">
          {t("account.loginForm.emailLabel")}
        </Label>
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
        {isSubmitting
          ? t("account.passwordResetForm.sending")
          : t("account.passwordResetForm.sendLink")}
      </Button>
      <Button
        type="button"
        variant="outline"
        onClick={onSuccess}
        className="w-full"
      >
        {t("account.passwordResetForm.cancel")}
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
  const { t } = useTranslation();
  const [serverError, setServerError] = useState<string | null>(null);

  const newPasswordSchema = z
    .object({
      newPassword: z
        .string()
        .min(1, t("account.passwordResetForm.passwordRequired"))
        .min(8, t("account.passwordResetForm.passwordTooShort")),
      confirmPassword: z.string(),
    })
    .refine((d) => d.newPassword === d.confirmPassword, {
      message: t("account.registrationForm.passwordMismatch"),
      path: ["confirmPassword"],
    });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<NewPasswordData>({
    resolver: zodResolver(newPasswordSchema),
  });

  const onSubmit = async (data: NewPasswordData) => {
    setServerError(null);
    try {
      await resetPassword(token, data.newPassword);
      onSuccess();
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr.error === "INVALID_OR_EXPIRED_TOKEN") {
        setServerError(t("account.passwordResetForm.linkExpiredError"));
      } else {
        setServerError(t("account.passwordResetForm.genericError"));
      }
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <p className="text-sm text-muted-foreground">
        {t("account.passwordResetForm.resetDescription")}
      </p>
      <div className="space-y-1">
        <Label htmlFor="new-password">
          {t("account.passwordResetForm.newPasswordLabel")}
        </Label>
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
        <Label htmlFor="confirm-new-password">
          {t("account.registrationForm.confirmPasswordLabel")}
        </Label>
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
        {isSubmitting
          ? t("account.passwordResetForm.saving")
          : t("account.passwordResetForm.changePassword")}
      </Button>
    </form>
  );
}
