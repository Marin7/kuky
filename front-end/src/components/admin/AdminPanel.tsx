import { useTranslation } from "react-i18next";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AvailabilityTab } from "@/components/admin/availability/AvailabilityTab";
import { BookingsTab } from "@/components/admin/bookings/BookingsTab";
import { StudentsTab } from "@/components/admin/students/StudentsTab";
import { UnitsTab } from "@/components/admin/units/UnitsTab";
import { HomeworkTab } from "@/components/admin/homework/HomeworkTab";
import { PresentationsTab } from "@/components/admin/presentations/PresentationsTab";
import { PlacementAuthoring } from "@/components/placement/admin/PlacementAuthoring";

const VALID_TABS = [
  "bookings",
  "students",
  "availability",
  "units",
  "homework",
  "presentations",
  "placement",
];

export function AdminPanel({ initialTab }: { initialTab?: string }) {
  const { t } = useTranslation();
  const defaultTab =
    initialTab && VALID_TABS.includes(initialTab) ? initialTab : "bookings";
  return (
    <div className="mx-auto max-w-5xl px-6 py-12">
      <h1 className="font-display text-3xl font-semibold text-primary">
        {t("admin.panel.title")}
      </h1>
      <p className="mt-2 text-muted-foreground">{t("admin.panel.subtitle")}</p>

      <Tabs defaultValue={defaultTab} className="mt-8">
        <TabsList>
          <TabsTrigger value="bookings">{t("admin.tabs.bookings")}</TabsTrigger>
          <TabsTrigger value="students">{t("admin.tabs.students")}</TabsTrigger>
          <TabsTrigger value="availability">
            {t("admin.tabs.availability")}
          </TabsTrigger>
          <TabsTrigger value="units">{t("admin.tabs.units")}</TabsTrigger>
          <TabsTrigger value="homework">{t("admin.tabs.homework")}</TabsTrigger>
          <TabsTrigger value="presentations">
            {t("admin.tabs.presentations")}
          </TabsTrigger>
          <TabsTrigger value="placement">
            {t("placement.admin.tab")}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="bookings" className="mt-6">
          <BookingsTab />
        </TabsContent>
        <TabsContent value="students" className="mt-6">
          <StudentsTab />
        </TabsContent>
        <TabsContent value="availability" className="mt-6">
          <AvailabilityTab />
        </TabsContent>
        <TabsContent value="units" className="mt-6">
          <UnitsTab />
        </TabsContent>
        <TabsContent value="homework" className="mt-6">
          <HomeworkTab />
        </TabsContent>
        <TabsContent value="presentations" className="mt-6">
          <PresentationsTab />
        </TabsContent>
        <TabsContent value="placement" className="mt-6">
          <PlacementAuthoring />
        </TabsContent>
      </Tabs>
    </div>
  );
}
