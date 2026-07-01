# Phase 0 Research: Placement Test

This resolves the planning-level unknowns the spec deferred ("defined during planning"): the CEFR banding rule, per-section time limits, where placement data lives, and how reuse of audio/booking is wired. No `NEEDS CLARIFICATION` markers remained in the spec; the decisions below are the concrete choices feeding Phase 1.

## 1. New `placement` domain vs. reusing the `learning` exercise tables

**Decision**: Create a dedicated `placement` backend domain with its own tables.

**Rationale**: The existing exercise mechanics (`homework_questions`, `homework_question_options`, `homework_answers`, `homework_answer_options`, `homework_submissions`) are bound to `homework_assignments` and per-student `homework_targets`. Placement questions are **global** (not assigned to a student), need a **per-section/skill** grouping, **server-authoritative per-section timers**, and **CEFR banding** — none of which the assignment/target model expresses. Forcing placement onto that schema would mean fake "assignments" and target rows for every visitor, contradicting Simplicity First. A parallel set of small `placement_*` tables is cleaner and isolated.

**Reuse without coupling**: The **grading rules** are identical, so `PlacementGradingService` reproduces the three rules already proven in `ExerciseGradingService` (`back-end/.../learning/service/ExerciseGradingService.java`): single-choice 0/1 exact set match, multi-choice partial credit (`rightDecisions / optionCount`), fill-blank `strip().toLowerCase(Locale.ROOT)` accent-exact match against accepted answers. The logic is ~40 lines; duplicating it keeps the domains decoupled and avoids a premature shared abstraction (YAGNI). If a third consumer ever appears, extract then.

**Alternatives considered**: (a) Overload `learning` tables — rejected (assignment/target coupling). (b) Extract a shared `grading` module now — rejected (only two consumers, premature abstraction).

## 2. Per-skill CEFR banding rule (deterministic)

**Decision**: Tag each auto-graded question with the CEFR level it exercises (`A1`…`C2`). A skill's level is the **highest level L for which the student answered at least 60% of the items at levels ≤ L correctly**; if even A1 is below threshold the skill is reported as `A0` ("below A1"/"principiante"). The **overall estimate** is the lowest of the three per-skill levels (a chain is as strong as its weakest skill), which is conventional for placement.

**Rationale**: Item-level CEFR tagging makes the per-skill result meaningful (it reflects *which* level the learner can sustain, not just a raw percentage) and is fully deterministic — identical answer sets always map to identical levels (SC-003). The 60% threshold and "weakest skill" overall rule are simple, explainable, and admin-tunable later if needed (threshold stored in `placement_config`).

**Worked example**: A Reading section with 2 items per level A1–C2 (12 items). A learner correct on all A1/A2/B1 items and 1 of 2 B2 → cumulative-correct through B1 = 100% (≥60%), through B2 = (6+1)/8 = 87% (≥60%), through C1 = 7/10 = 70% (≥60% → still passes unless C1 items missed)… the rule walks levels in order and stops at the highest still-passing level. Boundary behavior (exactly 60%, zero items at a level) is covered by `PlacementScoringServiceTest`.

**Alternatives considered**: (a) Flat percentage→band table ignoring item difficulty — rejected (less meaningful, A1 and C2 items weighted equally). (b) IRT/adaptive scoring — rejected (massive over-engineering for a lead-gen test). (c) Average per-skill level for overall — rejected; "weakest skill" is more honest for a single recommended starting level.

## 3. Per-section time limits

**Decision**: Defaults — **Reading 10 min, Listening 8 min, Grammar 7 min** — stored in the `placement_config` singleton and editable by the teacher in the admin tab. Total ≈ 25 min ceiling, comfortably supporting SC-001's "typically under 20 minutes".

**Rationale**: These are sensible first values for a ~10–15 item section; making them config (not hard-coded) lets Paula tune without a deploy. The exact value is a business knob, not an architectural decision.

**Mechanism**: When a section is started, the server stamps `started_at` and computes `deadline_at = started_at + limit`. The client renders a countdown to `deadline_at`. On submit the server **ignores the client clock** and treats any item not answered as incorrect; a submit arriving after `deadline_at` is still accepted but grades exactly what was sent (client auto-submits at expiry, so this is the normal path). Refreshing reloads remaining time from `deadline_at`; an already-elapsed section is auto-finalized on next interaction. This satisfies FR-006's "tracked authoritatively server-side so they cannot be bypassed."

**Alternatives considered**: Pausing timers on disconnect — rejected (spec says timer keeps running; pausing invites abuse).

## 4. Listening audio — reuse `audio_files`

**Decision**: Reuse the existing generic `audio_files` table and `GET /api/v1/audio/{id}` endpoint (added in `V16__add_homework_audio.sql`) for Listening questions. A placement question carries either `audio_url` (external link) or `audio_file_id` (uploaded clip), exactly like `homework_assignments`.

**Rationale**: The audio storage + streaming path already exists and is format-agnostic; no reason to duplicate. Replay is a front-end concern (a standard `<audio controls>` lets the user replay before answering, FR-003).

## 5. Speaking + appointment — reuse `bookings`, no new schema

**Decision**: The evaluation appointment is an ordinary booking created through the existing `POST /api/v1/bookings` flow (`scheduling` domain, `MeetingProvider` for Zoom/stub). No placement-specific booking table, status, or linkage is added. The frontend's `FullEvaluationPanel` links/redirects to the existing `/reservas` page.

**Rationale**: Spec is explicit — the speaking audition and results delivery happen inside one normal appointment that "behaves exactly like a regular class booking." Adding a typed/linked evaluation booking would be unused complexity (the teacher correlates the booking with the student's submitted Writing + results in the admin view). YAGNI.

**Trade-off accepted**: There is no hard in-app link between "this booking" and "the evaluation". The teacher identifies evaluation appointments contextually (the student has a Writing submission + placement result). This is acceptable for a single-teacher site and matches the trust-based design.

## 6. Payment instructions & writing prompt storage

**Decision**: Store the **bank-transfer instructions text** and the **writing prompt text** as columns on the `placement_config` singleton row, editable in the admin tab. No payment entity of any kind.

**Rationale**: They are static, teacher-owned content shown verbatim; a singleton config row is the lightest store and keeps FR-009/FR-012 honest (instructions are just text — no order, status, reference, or amount anywhere). Reusing the config row avoids extra tables.

## 7. Authentication / route protection

**Decision**: Place all student endpoints under `/api/v1/placement/**` (authenticated by `SecurityConfig`'s `anyRequest().authenticated()` default → 401 `UNAUTHENTICATED` for anonymous) and all authoring endpoints under `/api/v1/admin/placement/**` (`hasRole("ADMIN")`). The front-end route follows the existing pattern: `getMe()` in `useEffect`, redirect to `/cuenta` when unauthenticated.

**Rationale**: This gives FR-001's "login required for the whole test" for free with zero new security config — anonymous callers already get a JSON 401, and the page redirects to login. No endpoint is public (unlike `GET /schedule` and `GET /resources/**`), which is correct here.

## 8. Notifications

**Decision**: Reuse the existing email/notification mechanism (`spring-boot-starter-mail`, the same path booking confirmations use). The only notifications in scope are the standard booking confirmations that the existing flow already sends; the placement test itself sends none (results are instant on-screen, and the live results are delivered in person).

## Summary of resolved values

| Item | Resolution |
|------|-----------|
| Data location | New `placement` domain + 8 `placement_*` tables |
| Grading rules | Mirror `ExerciseGradingService` (single 0/1, multi partial, fill-blank normalize) |
| CEFR banding | Item-level CEFR tags; per-skill = highest level with ≥60% cumulative correct; overall = weakest skill; `A0` below A1 |
| Time limits | Reading 10m / Listening 8m / Grammar 7m, config-editable, server-authoritative deadlines |
| Audio | Reuse `audio_files` + `GET /api/v1/audio/{id}` |
| Speaking/appointment | Reuse existing `bookings` flow; no new schema or entity |
| Payment | Static text in `placement_config`; no payment entity at all |
| Auth | `/placement/**` authenticated, `/admin/placement/**` ADMIN |
