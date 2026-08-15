# Quickstart: Quiz Terminology

**Feature**: `040-quiz-terminology` | **Date**: 2026-08-15

Manual validation. Contract: [quiz-api.md](./contracts/quiz-api.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- PostgreSQL `kuky_dev`; backend `local` (`:8081`); frontend `npm run dev` (`:8080`)
- Teacher (ADMIN) and two students A and B (`STUDENT`). One extra `USER` account (not a student).

Flyway `V21` drops placement tables on boot.

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

## Scenarios

### 1. Placement is gone (P1)

1. Open `/prueba-de-nivel` → lands on `/quizzes` (login if needed).
2. Main nav shows Quizzes, not “Prueba de nivel”.
3. Admin panel tab is Quizzes, not Prueba de nivel. No CEFR / bank-transfer / timed-section tools.
4. Student profile has no placement CEFR block.
5. Site copy (es/en/ro): no “prueba de nivel” / “placement test” product strings.

### 2. Teacher authors a mixed quiz and assigns A only (P1)

1. Panel → Quizzes → new quiz. Title + questions: reading SINGLE_CHOICE, grammar TRUE_FALSE, listening with YouTube or upload, writing FREE_TEXT. Each has a skill.
2. Save. Assign student A (not B). Assigning a `USER` is rejected.
3. A sees the quiz on `/quizzes`. B’s list does not include it. The `USER` account cannot take quizzes.

### 3. A submits mixed quiz; B never assigned (P1)

1. A opens the quiz, answers all, submits.
2. Auto questions show keys + overall % + per-skill %; writing skill awaits teacher.
3. Quiz does not appear under Aprendizaje homework/units.

### 4. Teacher scores writing (P2)

1. Admin opens A’s attempt: snapshot + auto results + writing box.
2. Set writing % (and optional note), finalize.
3. A sees combined overall and all skill figures. Fully-correct only at 100%.

### 5. Edit after A submitted; assign B (P1 / clarify)

1. Teacher changes a grammar prompt and correct option; adds a reading question; saves.
2. A’s result still shows the old questions and scores.
3. Assign B. B’s take shows the **new** live questions.
4. B submits; graded against the new set. A unchanged.

### 6. One attempt

1. A cannot submit again (`409 QUIZ_ALREADY_SUBMITTED`). Result is read-only.

### 7. Homework unchanged (SC-008)

1. Take/submit/grade an existing mixed homework as before.
