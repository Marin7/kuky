import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import {
  register as apiRegister,
  updateProfile,
  type ApiError,
} from "@/lib/auth";

type FormData = {
  email: string;
  password: string;
  confirmPassword: string;
  firstName?: string;
  lastName?: string;
  username?: string;
  gdprConsent: boolean;
};

interface Props {
  onSuccess: () => void;
}

export function RegistrationForm({ onSuccess }: Props) {
  const { t } = useTranslation();
  const [serverError, setServerError] = useState<string | null>(null);

  const schema = z
    .object({
      email: z
        .string()
        .min(1, t("account.registrationForm.emailRequired"))
        .email(t("account.registrationForm.emailInvalid")),
      password: z
        .string()
        .min(1, t("account.registrationForm.passwordRequired"))
        .min(8, t("account.registrationForm.passwordTooShort")),
      confirmPassword: z
        .string()
        .min(1, t("account.registrationForm.confirmPasswordRequired")),
      firstName: z.string().max(100).optional(),
      lastName: z.string().max(100).optional(),
      username: z
        .string()
        .max(50)
        .refine(
          (v) => !v || v.length >= 3,
          "El nombre de usuario debe tener al menos 3 caracteres.",
        )
        .refine(
          (v) => !v || /^[a-zA-Z0-9_-]+$/.test(v),
          "Solo letras, números, guiones y guiones bajos.",
        )
        .optional(),
      gdprConsent: z.boolean().refine((v) => v, {
        message:
          "Debes aceptar la política de privacidad para crear una cuenta.",
      }),
    })
    .refine((d) => d.password === d.confirmPassword, {
      message: t("account.registrationForm.passwordMismatch"),
      path: ["confirmPassword"],
    });

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { gdprConsent: false },
  });

  const gdprConsent = watch("gdprConsent");

  const onSubmit = async (data: FormData) => {
    setServerError(null);
    try {
      await apiRegister(data.email, data.password, data.gdprConsent);

      const firstName = data.firstName?.trim() || null;
      const lastName = data.lastName?.trim() || null;
      const username = data.username?.trim() || null;
      if (firstName || lastName || username) {
        await updateProfile({ firstName, lastName, username });
      }

      onSuccess();
    } catch (err) {
      const apiErr = err as ApiError;
      if (
        apiErr.error === "EMAIL_ALREADY_REGISTERED" ||
        apiErr.error === "EMAIL_TAKEN"
      ) {
        setServerError(t("account.registrationForm.emailTaken"));
      } else {
        setServerError(
          apiErr.message ?? t("account.registrationForm.genericError"),
        );
      }
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label htmlFor="reg-firstName">
            {t("account.registrationForm.firstNameLabel")}
          </Label>
          <Input
            id="reg-firstName"
            placeholder="Tu nombre"
            {...register("firstName")}
          />
          {errors.firstName && (
            <p className="text-sm text-destructive">
              {errors.firstName.message}
            </p>
          )}
        </div>
        <div className="space-y-1">
          <Label htmlFor="reg-lastName">
            {t("account.registrationForm.lastNameLabel")}
          </Label>
          <Input
            id="reg-lastName"
            placeholder="Tus apellidos"
            {...register("lastName")}
          />
          {errors.lastName && (
            <p className="text-sm text-destructive">
              {errors.lastName.message}
            </p>
          )}
        </div>
      </div>

      <div className="space-y-1">
        <Label htmlFor="reg-username">
          {t("account.registrationForm.usernameLabel")}
        </Label>
        <div className="relative">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm pointer-events-none">
            @
          </span>
          <Input
            id="reg-username"
            className="pl-7"
            placeholder="nombre_usuario"
            {...register("username")}
          />
        </div>
        {errors.username && (
          <p className="text-sm text-destructive">{errors.username.message}</p>
        )}
      </div>

      <div className="space-y-1">
        <Label htmlFor="reg-email">
          {t("account.registrationForm.emailLabel")}
        </Label>
        <Input
          id="reg-email"
          type="email"
          placeholder="tu@correo.com"
          {...register("email")}
        />
        {errors.email && (
          <p className="text-sm text-destructive">{errors.email.message}</p>
        )}
      </div>

      <div className="space-y-1">
        <Label htmlFor="reg-password">
          {t("account.registrationForm.passwordLabel")}
        </Label>
        <Input
          id="reg-password"
          type="password"
          placeholder="Mínimo 8 caracteres"
          {...register("password")}
        />
        {errors.password && (
          <p className="text-sm text-destructive">{errors.password.message}</p>
        )}
      </div>

      <div className="space-y-1">
        <Label htmlFor="reg-confirm">
          {t("account.registrationForm.confirmPasswordLabel")}
        </Label>
        <Input
          id="reg-confirm"
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

      <div className="flex items-start gap-2">
        <Checkbox
          id="gdpr"
          checked={gdprConsent}
          onCheckedChange={(checked) => setValue("gdprConsent", !!checked)}
        />
        <Label htmlFor="gdpr" className="text-sm leading-snug cursor-pointer">
          Acepto la{" "}
          <a
            href="/politica-privacidad"
            className="underline"
            target="_blank"
            rel="noopener noreferrer"
          >
            política de privacidad
          </a>
        </Label>
      </div>
      {errors.gdprConsent && (
        <p className="text-sm text-destructive">{errors.gdprConsent.message}</p>
      )}

      {serverError && <p className="text-sm text-destructive">{serverError}</p>}

      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting
          ? t("account.registrationForm.submitting")
          : t("account.registrationForm.submit")}
      </Button>
    </form>
  );
}
