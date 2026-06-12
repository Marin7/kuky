import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { login as apiLogin, resendActivation, type ApiError } from "@/lib/auth";

type FormData = { email: string; password: string };

interface Props {
  onSuccess: () => void;
  onForgotPassword: () => void;
}

export function LoginForm({ onSuccess, onForgotPassword }: Props) {
  const { t } = useTranslation();
  const [serverError, setServerError] = useState<string | null>(null);
  const [notActivatedEmail, setNotActivatedEmail] = useState<string | null>(
    null,
  );
  const [resent, setResent] = useState(false);

  const schema = z.object({
    email: z.string().min(1, t("account.loginForm.emailRequired")),
    password: z.string().min(1, t("account.loginForm.passwordRequired")),
  });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setServerError(null);
    setNotActivatedEmail(null);
    setResent(false);
    try {
      await apiLogin(data.email, data.password);
      onSuccess();
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr.error === "RATE_LIMIT_EXCEEDED") {
        setServerError(t("account.loginForm.rateLimitError"));
      } else if (apiErr.error === "ACCOUNT_NOT_ACTIVATED") {
        setNotActivatedEmail(data.email);
      } else {
        setServerError(t("account.loginForm.credentialsError"));
      }
    }
  };

  const handleResend = async () => {
    if (!notActivatedEmail) return;
    await resendActivation(notActivatedEmail);
    setResent(true);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="space-y-1">
        <Label htmlFor="login-email">{t("account.loginForm.emailLabel")}</Label>
        <Input
          id="login-email"
          type="email"
          placeholder="tu@correo.com"
          {...register("email")}
        />
        {errors.email && (
          <p className="text-sm text-destructive">{errors.email.message}</p>
        )}
      </div>

      <div className="space-y-1">
        <Label htmlFor="login-password">
          {t("account.loginForm.passwordLabel")}
        </Label>
        <Input
          id="login-password"
          type="password"
          placeholder="Tu contraseña"
          {...register("password")}
        />
        {errors.password && (
          <p className="text-sm text-destructive">{errors.password.message}</p>
        )}
      </div>

      {serverError && <p className="text-sm text-destructive">{serverError}</p>}

      {notActivatedEmail && (
        <div className="rounded-md border border-amber-200 bg-amber-50 p-3 text-sm space-y-2">
          <p className="text-amber-800">
            {t("account.loginForm.notActivatedWarning")}
          </p>
          {resent ? (
            <p className="text-green-700">
              {t("account.loginForm.resentEmail")}
            </p>
          ) : (
            <button
              type="button"
              onClick={handleResend}
              className="text-amber-700 underline hover:text-amber-900"
            >
              {t("account.loginForm.resendActivation")}
            </button>
          )}
        </div>
      )}

      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting
          ? t("account.loginForm.submitting")
          : t("account.loginForm.submit")}
      </Button>

      <button
        type="button"
        onClick={onForgotPassword}
        className="w-full text-sm text-muted-foreground hover:text-foreground underline"
      >
        {t("account.loginForm.forgotPassword")}
      </button>
    </form>
  );
}
