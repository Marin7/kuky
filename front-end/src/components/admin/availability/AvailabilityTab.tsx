import { useState } from "react";
import { useTranslation } from "react-i18next";
import type { BookingConflict } from "@/lib/admin";
import { GeneralAvailabilityEditor } from "./GeneralAvailabilityEditor";
import { WeeklyAvailabilityEditor } from "./WeeklyAvailabilityEditor";
import { BookingConflictNotice } from "./BookingConflictNotice";

export function AvailabilityTab() {
  const { t } = useTranslation();
  const [conflicts, setConflicts] = useState<BookingConflict[]>([]);

  return (
    <div className="space-y-6">
      <p className="text-sm text-muted-foreground">
        {t("admin.availability.description")}
      </p>
      <BookingConflictNotice conflicts={conflicts} />
      <GeneralAvailabilityEditor onConflicts={setConflicts} />
      <WeeklyAvailabilityEditor onConflicts={setConflicts} />
    </div>
  );
}
