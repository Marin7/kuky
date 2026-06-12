import { useTranslation } from "react-i18next";
import type { BookingConflict } from "@/lib/admin";

function formatSlot(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

interface Props {
  conflicts: BookingConflict[];
}

export function BookingConflictNotice({ conflicts }: Props) {
  const { t } = useTranslation();
  if (conflicts.length === 0) return null;
  return (
    <div className="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
      <p className="font-medium">
        {t("admin.availability.conflict.title", { count: conflicts.length })}
      </p>
      <p className="mt-1 text-xs">{t("admin.availability.conflict.body")}</p>
      <ul className="mt-2 space-y-1">
        {conflicts.map((c) => (
          <li key={c.bookingId} className="text-xs">
            {c.studentEmail} · {formatSlot(c.slotStart)}
          </li>
        ))}
      </ul>
    </div>
  );
}
