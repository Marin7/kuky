import { useEffect, useState } from "react";
import { getSchedule } from "@/lib/scheduling";

const FALLBACK_ZONE = "Europe/Madrid";

/**
 * The teacher's fixed working time zone, read from the existing public schedule
 * endpoint instead of a hardcoded literal or the viewer's own (browser-local) zone —
 * admin views must always reflect the teacher's zone, regardless of device (FR-007).
 */
export function useTeacherTimezone(): string {
  const [zone, setZone] = useState(FALLBACK_ZONE);

  useEffect(() => {
    let cancelled = false;
    getSchedule()
      .then((schedule) => {
        if (!cancelled) setZone(schedule.teacherTimezone);
      })
      .catch(() => {
        // Keep the fallback — better than crashing the admin panel.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return zone;
}
