import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
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

      <Tabs defaultValue="perWeek">
        <TabsList>
          <TabsTrigger value="perWeek">
            {t("admin.availability.perWeek.tab")}
          </TabsTrigger>
          <TabsTrigger value="general">
            {t("admin.availability.general.tab")}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="perWeek" className="mt-6">
          <WeeklyAvailabilityEditor onConflicts={setConflicts} />
        </TabsContent>
        <TabsContent value="general" className="mt-6">
          <GeneralAvailabilityEditor onConflicts={setConflicts} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
