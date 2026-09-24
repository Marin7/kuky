import { API_ORIGIN } from "@/lib/api";

const API_BASE = `${API_ORIGIN}/api/v1`;

/** Wire value of a supported email option. The server drives the list, not the client. */
export type EmailPreferenceType = "NEW_HOMEWORK_ASSIGNED";

export interface EmailPreference {
  type: EmailPreferenceType;
  enabled: boolean;
}

interface EmailPreferencesResponse {
  preferences: EmailPreference[];
}

export interface ApiError {
  error: string;
  message: string;
}

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

export const getEmailPreferences = async (): Promise<EmailPreference[]> => {
  const data = await apiCall<EmailPreferencesResponse>("/me/email-preferences");
  return data.preferences ?? [];
};

export const setEmailPreference = (
  type: EmailPreferenceType,
  enabled: boolean,
) =>
  apiCall<EmailPreference>(`/me/email-preferences/${type}`, {
    method: "PUT",
    body: JSON.stringify({ enabled }),
  });
