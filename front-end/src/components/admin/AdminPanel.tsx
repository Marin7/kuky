import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AvailabilityTab } from "@/components/admin/availability/AvailabilityTab";
import { HomeworkTab } from "@/components/admin/homework/HomeworkTab";
import { PresentationsTab } from "@/components/admin/presentations/PresentationsTab";
import { BookingsTab } from "@/components/admin/bookings/BookingsTab";
import { StudentsTab } from "@/components/admin/students/StudentsTab";

/**
 * Teacher control panel. Three areas — availability, homework, presentations —
 * presented as tabs. The route (panel.tsx) guarantees only ADMIN reaches here.
 */
const VALID_TABS = [
  "bookings",
  "students",
  "availability",
  "homework",
  "presentations",
];

export function AdminPanel({ initialTab }: { initialTab?: string }) {
  const defaultTab =
    initialTab && VALID_TABS.includes(initialTab) ? initialTab : "bookings";
  return (
    <div className="mx-auto max-w-5xl px-6 py-12">
      <h1 className="font-display text-3xl font-semibold text-primary">
        Panel de control
      </h1>
      <p className="mt-2 text-muted-foreground">
        Gestiona tu disponibilidad, las tareas de tus alumnos y tus
        presentaciones.
      </p>

      <Tabs defaultValue={defaultTab} className="mt-8">
        <TabsList>
          <TabsTrigger value="bookings">Reservas</TabsTrigger>
          <TabsTrigger value="students">Alumnos</TabsTrigger>
          <TabsTrigger value="availability">Disponibilidad</TabsTrigger>
          <TabsTrigger value="homework">Tareas</TabsTrigger>
          <TabsTrigger value="presentations">Presentaciones</TabsTrigger>
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
        <TabsContent value="homework" className="mt-6">
          <HomeworkTab />
        </TabsContent>
        <TabsContent value="presentations" className="mt-6">
          <PresentationsTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}
