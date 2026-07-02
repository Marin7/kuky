import { useCallback, useEffect, useState } from "react";
import { getMe, updateTimezone as apiUpdateTimezone } from "@/lib/auth";
import { getSchedule } from "@/lib/scheduling";

/** Browser-detected IANA zone, or null if detection is unavailable. */
function detectZone(): string | null {
  try {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    return zone || null;
  } catch {
    return null;
  }
}

interface TimezoneState {
  /** Zone currently in effect for display. */
  zone: string;
  /** True once resolved (detection attempted / account preference loaded). */
  ready: boolean;
  /** True if the student explicitly chose this zone (blocks auto-resync). */
  isManual: boolean;
  /** True if `zone` is a fallback (teacher's zone) because detection failed, not the
   *  student's actual preference — per FR-008. */
  isFallback: boolean;
}

const DEFAULT_STATE: TimezoneState = {
  zone: "UTC",
  ready: false,
  isManual: false,
  isFallback: false,
};

/**
 * Resolves the zone used to display class times to the current viewer.
 *
 * Auto-detects the browser's zone every session and silently syncs it to the account
 * (so server-side emails can use it later) unless the student has set a manual override,
 * which persists until explicitly changed or cleared (Clarification Session 2026-07-02).
 */
export function useTimezone() {
  const [state, setState] = useState<TimezoneState>(DEFAULT_STATE);

  const resolveFallbackZone = useCallback(async (): Promise<string> => {
    try {
      const schedule = await getSchedule();
      return schedule.teacherTimezone;
    } catch {
      return "UTC";
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      const detected = detectZone();

      let user;
      try {
        user = await getMe();
      } catch {
        // Not logged in — use the detected zone locally, no account to sync to.
        const zone = detected ?? (await resolveFallbackZone());
        if (!cancelled) {
          setState({
            zone,
            ready: true,
            isManual: false,
            isFallback: !detected,
          });
        }
        return;
      }

      if (cancelled) return;

      if (user.timezoneIsManual && user.timezone) {
        setState({
          zone: user.timezone,
          ready: true,
          isManual: true,
          isFallback: false,
        });
        return;
      }

      if (detected) {
        if (detected !== user.timezone) {
          apiUpdateTimezone(detected, false).catch(() => {
            // Sync is best-effort — display still uses the freshly detected zone.
          });
        }
        setState({
          zone: detected,
          ready: true,
          isManual: false,
          isFallback: false,
        });
        return;
      }

      // Detection unavailable — fall back to the account's last-known zone, else the
      // teacher's zone, clearly flagged as an estimate (FR-008).
      const fallbackZone = user.timezone ?? (await resolveFallbackZone());
      setState({
        zone: fallbackZone,
        ready: true,
        isManual: false,
        isFallback: true,
      });
    })();

    return () => {
      cancelled = true;
    };
  }, [resolveFallbackZone]);

  const setZone = useCallback(async (zone: string) => {
    await apiUpdateTimezone(zone, true);
    setState({ zone, ready: true, isManual: true, isFallback: false });
  }, []);

  const clearOverride = useCallback(async () => {
    const detected = detectZone();
    const zone = detected ?? (await resolveFallbackZone());
    await apiUpdateTimezone(zone, false);
    setState({ zone, ready: true, isManual: false, isFallback: !detected });
  }, [resolveFallbackZone]);

  return { ...state, setZone, clearOverride };
}
