export const INTEREST_CODES = [
  "TRAVEL",
  "MUSIC",
  "SPORTS",
  "FOOD",
  "CINEMA",
  "READING",
  "TECHNOLOGY",
  "NATURE",
  "ART",
  "WORK",
  "FAMILY",
  "CULTURE",
] as const;

export type InterestCode = (typeof INTEREST_CODES)[number];

export const MAX_INTEREST_SELECTIONS = 10;
export const MAX_INTERESTS_NOTE_LENGTH = 280;
