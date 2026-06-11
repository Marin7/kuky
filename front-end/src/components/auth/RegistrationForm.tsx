import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { register as apiRegister, updateProfile, type ApiError } from "@/lib/auth";

const schema = z
  .object({
    email: z.string().email("El correo electrónico no es válido."),
    password: z
      .string()
      .min(8, "La contraseña debe tener al menos 8 caracteres."),
    confirmPassword: z.string(),
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
      message: "Debes aceptar la política de privacidad para crear una cuenta.",
    }),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: "Las contraseñas no coinciden.",
    path: ["confirmPassword"],
  });

type FormData = z.infer<typeof schema>;

interface Props {
  onSuccess: () => void;
}

export function RegistrationForm({ onSuccess }: Props) {
  const [serverError, setServerError] = useState<string | null>(null);
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
      setServerError(
        apiErr.message ?? "Ha ocurrido un error. Inténtalo de nuevo.",
      );
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label htmlFor="reg-firstName">
            Nombre <span className="text-muted-foreground font-normal">(opcional)</span>
          </Label>
          <Input
            id="reg-firstName"
            placeholder="Tu nombre"
            {...register("firstName")}
          />
          {errors.firstName && (
            <p className="text-sm text-destructive">{errors.firstName.message}</p>
          )}
        </div>
        <div className="space-y-1">
          <Label htmlFor="reg-lastName">
            Apellidos <span className="text-muted-foreground font-normal">(opcional)</span>
          </Label>
          <Input
            id="reg-lastName"
            placeholder="Tus apellidos"
            {...register("lastName")}
          />
          {errors.lastName && (
            <p className="text-sm text-destructive">{errors.lastName.message}</p>
          )}
        </div>
      </div>

      <div className="space-y-1">
        <Label htmlFor="reg-username">
          Nombre de usuario{" "}
          <span className="text-muted-foreground font-normal">(opcional)</span>
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
        <Label htmlFor="reg-email">Correo electrónico</Label>
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
        <Label htmlFor="reg-password">Contraseña</Label>
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
        <Label htmlFor="reg-confirm">Confirmar contraseña</Label>
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
        {isSubmitting ? "Creando cuenta…" : "Crear cuenta"}
      </Button>
    </form>
  );
}
