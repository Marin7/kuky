import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AvailabilityTab } from "@/components/admin/availability/AvailabilityTab";
import { BookingsTab } from "@/components/admin/bookings/BookingsTab";
import { StudentsTab } from "@/components/admin/students/StudentsTab";
import { UsersTab } from "@/components/admin/students/UsersTab";
import { UnitsTab } from "@/components/admin/units/UnitsTab";
import { HomeworkTab } from "@/components/admin/homework/HomeworkTab";
import { PresentationsTab } from "@/components/admin/presentations/PresentationsTab";
import { ActivitiesTab } from "@/components/admin/activities/ActivitiesTab";
import { QuizTab } from "@/components/quiz/admin/QuizTab";
import { TestimonialsTab } from "@/components/admin/testimonials/TestimonialsTab";
import { NotificationDot } from "@/components/NotificationDot";
import {
  getBadges,
  onBadgesInvalidate,
  type BadgeSummary,
} from "@/lib/notifications";

const VALID_TABS = [
  "bookings",
  "students",
  "users",
  "availability",
  "units",
  "homework",
  "presentations",
  "activities",
  "quizzes",
  "testimonials",
];

const EMPTY_BADGES: BadgeSummary = {
  panel: false,
  homework: false,
  quiz: false,
  learning: false,
};

export function AdminPanel({ initialTab }: { initialTab?: string }) {
  const { t } = useTranslation();
  const [badges, setBadges] = useState<BadgeSummary>(EMPTY_BADGES);
  const defaultTab =
    initialTab && VALID_TABS.includes(initialTab) ? initialTab : "bookings";

  useEffect(() => {
    let active = true;
    const load = () => {
      getBadges().then((b) => {
        if (active) setBadges(b);
      });
    };
    load();
    const unsubscribe = onBadgesInvalidate(load);
    return () => {
      active = false;
      unsubscribe();
    };
  }, []);

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
          <TabsTrigger value="users">{t("admin.tabs.users")}</TabsTrigger>
          <TabsTrigger value="availability">
            {t("admin.tabs.availability")}
          </TabsTrigger>
          <TabsTrigger value="units">{t("admin.tabs.units")}</TabsTrigger>
          <TabsTrigger value="homework" className="gap-1.5">
            {t("admin.tabs.homework")}
            {badges.homework && (
              <NotificationDot label={t("notification.homework")} />
            )}
          </TabsTrigger>
          <TabsTrigger value="presentations">
            {t("admin.tabs.presentations")}
          </TabsTrigger>
          <TabsTrigger value="activities">
            {t("admin.tabs.activities")}
          </TabsTrigger>
          <TabsTrigger value="quizzes" className="gap-1.5">
            {t("admin.tabs.quizzes")}
            {badges.quiz && <NotificationDot label={t("notification.quiz")} />}
          </TabsTrigger>
          <TabsTrigger value="testimonials">
            {t("admin.tabs.testimonials")}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="bookings" className="mt-6">
          <BookingsTab />
        </TabsContent>
        <TabsContent value="students" className="mt-6">
          <StudentsTab />
        </TabsContent>
        <TabsContent value="users" className="mt-6">
          <UsersTab />
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
        <TabsContent value="activities" className="mt-6">
          <ActivitiesTab />
        </TabsContent>
        <TabsContent value="quizzes" className="mt-6">
          <QuizTab />
        </TabsContent>
        <TabsContent value="testimonials" className="mt-6">
          <TestimonialsTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}
