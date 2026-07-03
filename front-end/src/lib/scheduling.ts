import { API_ORIGIN } from "@/lib/api";
const API_BASE = `${API_ORIGIN}/api/v1`;

export type SlotStatus = "OPEN" | "BOOKED" | "UNAVAILABLE";

export interface Slot {
  start: string;
  end: string;
  status: SlotStatus;
}

export interface ScheduleResponse {
  teacherTimezone: string;
  horizonStart: string;
  horizonEnd: string;
  slots: Slot[];
}

export interface BookingResponse {
  id: string;
  slotStart: string;
  slotEnd: string;
  durationMinutes: number;
  status: string;
  zoomJoinUrl: string;
  createdAt: string;
}

export interface BookingSummary {
  id: string;
  slotStart: string;
  slotEnd: string;
  status: string;
  zoomJoinUrl: string;
  cancellable: boolean;
}

export interface MyBookingsResponse {
  upcoming: BookingSummary[];
  past: BookingSummary[];
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

  if (!res.ok) {
    throw data as ApiError;
  }

  return data as T;
}

export const getSchedule = (durationMinutes = 60) =>
  apiCall<ScheduleResponse>(`/schedule?durationMinutes=${durationMinutes}`);

export const createBooking = (slotStart: string, durationMinutes: number) =>
  apiCall<BookingResponse>("/bookings", {
    method: "POST",
    body: JSON.stringify({ slotStart, durationMinutes }),
  });

export const listBookings = () => apiCall<MyBookingsResponse>("/bookings");

export const cancelBooking = (id: string) =>
  apiCall<void>(`/bookings/${id}`, { method: "DELETE" });
