// API client for the student-facing placement test (/api/v1/placement/**).
// Mirrors lib/learning.ts: cookie-based auth (credentials: "include"), {error,message} errors.

import { API_ORIGIN } from "@/lib/api";
const API_BASE = `${API_ORIGIN}/api/v1`;

export type Skill = "READING" | "LISTENING" | "GRAMMAR";
export type QuestionKind = "SINGLE_CHOICE" | "MULTI_CHOICE" | "FILL_BLANK";
export type SectionStatus = "NOT_STARTED" | "IN_PROGRESS" | "SUBMITTED";

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

export const placementAudioUrl = (audioFileId: string) =>
  `${API_BASE}/audio/${audioFileId}`;

// --- Free auto-graded test --------------------------------------------------

export interface StudentOption {
  id: string;
  label: string;
}

export interface StudentQuestion {
  id: string;
  kind: QuestionKind;
  prompt: string;
  audioUrl: string | null;
  audioFileId: string | null;
  options: StudentOption[];
}

export interface SectionDto {
  skill: Skill;
  timeLimitSeconds: number;
  status: SectionStatus;
  deadlineAt: string | null;
  questions: StudentQuestion[];
}

export interface PlacementTestResponse {
  attemptId: string | null;
  sections: SectionDto[];
}

export interface StartSectionResponse {
  skill: Skill;
  deadlineAt: string;
  serverNow: string;
}

export interface AnswerPayload {
  questionId: string;
  selectedOptionIds: string[];
  answerText: string | null;
}

export interface QuestionResultDto {
  questionId: string;
  score: number;
  correct: boolean;
  correctOptionIds: string[];
  acceptedAnswers: string[];
}

export interface SectionResultResponse {
  skill: Skill;
  scorePercent: number;
  cefrLevel: string; // "A0".."C2"
  questionResults: QuestionResultDto[];
}

export interface SkillResult {
  skill: Skill;
  scorePercent: number;
  cefrLevel: string;
}

export interface AttemptResultResponse {
  status: "IN_PROGRESS" | "COMPLETED";
  overallCefr: string | null;
  skills: SkillResult[];
}

export const getPlacementTest = () =>
  apiCall<PlacementTestResponse>("/placement/test");

export const startAttempt = () =>
  apiCall<{ attemptId: string; status: string }>("/placement/attempts", {
    method: "POST",
  });

export const startSection = (attemptId: string, skill: Skill) =>
  apiCall<StartSectionResponse>(
    `/placement/attempts/${attemptId}/sections/${skill}/start`,
    { method: "POST" },
  );

export const submitSection = (
  attemptId: string,
  skill: Skill,
  answers: AnswerPayload[],
) =>
  apiCall<SectionResultResponse>(
    `/placement/attempts/${attemptId}/sections/${skill}/submit`,
    { method: "POST", body: JSON.stringify({ answers }) },
  );

export const getAttemptResult = (attemptId: string) =>
  apiCall<AttemptResultResponse>(`/placement/attempts/${attemptId}/result`);

// --- Full evaluation (offline payment, Writing, booking) -------------------

export interface WritingSubmissionDto {
  id: string;
  body: string;
  submittedAt: string;
}

export interface WritingSectionDto {
  status: "NOT_STARTED" | "IN_PROGRESS";
  timeLimitSeconds: number;
  deadlineAt: string | null;
}

export interface FullEvaluationResponse {
  writingPrompt: string;
  mySubmission: WritingSubmissionDto | null;
  writingSection: WritingSectionDto;
}

export interface WritingStartResponse {
  deadlineAt: string;
  serverNow: string;
}

export const getFullEvaluation = () =>
  apiCall<FullEvaluationResponse>("/placement/full-evaluation");

export const startWriting = () =>
  apiCall<WritingStartResponse>("/placement/writing/start", {
    method: "POST",
  });

export const submitWriting = (body: string) =>
  apiCall<WritingSubmissionDto>("/placement/writing", {
    method: "POST",
    body: JSON.stringify({ body }),
  });
