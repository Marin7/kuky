import { API_ORIGIN } from "@/lib/api";
const API_BASE = `${API_ORIGIN}/api/v1`;

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

export interface Testimonial {
  text: string;
  studentName: string;
  displayOrder: number;
}

export const getTestimonials = () => apiCall<Testimonial[]>("/testimonials");

export type TestimonialStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "UNPUBLISHED";

export interface MyTestimonial {
  text: string;
  status: TestimonialStatus;
  submittedAt: string;
}

export const submitTestimonial = (text: string) =>
  apiCall<MyTestimonial>("/testimonials", {
    method: "POST",
    body: JSON.stringify({ text }),
  });

// Returns undefined when the caller has never submitted a testimonial (204 No Content).
export const getMyTestimonial = () =>
  apiCall<MyTestimonial | undefined>("/testimonials/me");
