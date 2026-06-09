const API_BASE = "http://localhost:8081/api/v1/auth";

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
  });

export const login = (email: string, password: string) =>
  apiCall<UserResponse>("/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

export const logout = () => apiCall<void>("/logout", { method: "POST" });

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
