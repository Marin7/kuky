import { API_ORIGIN } from "@/lib/api";
const API_BASE = `${API_ORIGIN}/api/v1/auth`;

export interface ApiError {
  error: string;
  message: string;
}

export interface AuthResponse {
  id: string;
  email: string;
  createdAt: string;
}

export interface UserResponse {
  id: string;
  email: string;
  role: "STUDENT" | "ADMIN";
  firstName?: string;
  lastName?: string;
  username?: string;
  avatarImageId?: string;
  status: "ACTIVE" | "PENDING";
}

/**
 * Notify the rest of the app (e.g. SiteHeader) that auth state changed, so
 * auth-conditional UI can re-check without a full page reload. SSR-safe.
 */
function notifyAuthChanged() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event("auth-changed"));
  }
}

async function apiCall<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (res.status === 204) return undefined as T;

  const data = await res.json();

  if (!res.ok) {
    throw data as ApiError;
  }

  return data as T;
}

export const register = (
  email: string,
  password: string,
  gdprConsent: boolean,
) =>
  apiCall<AuthResponse>("/register", {
    method: "POST",
    body: JSON.stringify({ email, password, gdprConsent }),
  }).then((res) => {
    notifyAuthChanged();
    return res;
  });

export const login = (email: string, password: string) =>
  apiCall<UserResponse>("/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  }).then((res) => {
    notifyAuthChanged();
    return res;
  });

export const logout = () =>
  apiCall<void>("/logout", { method: "POST" }).then(() => {
    notifyAuthChanged();
  });

export const getMe = () => apiCall<UserResponse>("/me");

export const forgotPassword = (email: string) =>
  apiCall<{ message: string }>("/forgot-password", {
    method: "POST",
    body: JSON.stringify({ email }),
  });

export const resetPassword = (token: string, newPassword: string) =>
  apiCall<{ message: string }>("/reset-password", {
    method: "POST",
    body: JSON.stringify({ token, newPassword }),
  });

export const updateProfile = (data: {
  firstName: string | null;
  lastName: string | null;
  username: string | null;
}) =>
  apiCall<UserResponse>("/profile", {
    method: "PUT",
    body: JSON.stringify(data),
  });

export const activate = (token: string) =>
  apiCall<{ message: string }>("/activate", {
    method: "POST",
    body: JSON.stringify({ token }),
  }).then((res) => {
    notifyAuthChanged();
    return res;
  });

export const resendActivation = (email: string) =>
  apiCall<{ message: string }>("/resend-activation", {
    method: "POST",
    body: JSON.stringify({ email }),
  });

export const uploadAvatar = async (file: File): Promise<{ avatarImageId: string }> => {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch(`${API_BASE}/avatar`, {
    method: "POST",
    credentials: "include",
    body: formData,
  });
  const data = await res.json();
  if (!res.ok) throw data as ApiError;
  return data as { avatarImageId: string };
};
