import { API_ORIGIN } from "@/lib/api";

const API_BASE = `${API_ORIGIN}/api/v1`;

export interface BadgeSummary {
  panel: boolean;
  homework: boolean;
  quiz: boolean;
  learning: boolean;
}

export interface ApiError {
  error: string;
  message: string;
}

const EMPTY_BADGES: BadgeSummary = {
  panel: false,
  homework: false,
  quiz: false,
  learning: false,
};

async function apiCall<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (res.status === 204) return undefined as T;
  const data = await res.json();
  if (!res.ok) throw data as ApiError;
  return data as T;
}

export const getBadges = () =>
  apiCall<BadgeSummary>("/notifications/badges").catch(() => EMPTY_BADGES);

export const markUnitSeen = (unitId: string) =>
  apiCall<{ unseen: boolean }>(`/learning/units/${unitId}/seen`, {
    method: "POST",
  });

export const markHomeworkSeen = (assignmentId: string) =>
  apiCall<{ unseen: boolean }>(`/learning/homework/${assignmentId}/seen`, {
    method: "POST",
  });

export function notifyBadgesChanged() {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event("notifications-changed"));
}

export function onBadgesInvalidate(handler: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  window.addEventListener("notifications-changed", handler);
  window.addEventListener("auth-changed", handler);
  return () => {
    window.removeEventListener("notifications-changed", handler);
    window.removeEventListener("auth-changed", handler);
  };
}
