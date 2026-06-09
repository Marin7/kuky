# Tasks: 1-on-1 Class Scheduling

**Input**: Design documents from `specs/003-class-scheduling/`

**Prerequisites**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/api.md ✅ quickstart.md ✅

**Tests**: Limited to the two backend tests explicitly named in plan.md (`AvailabilityServiceTest`, `BookingControllerIntegrationTest`). No frontend test framework is configured; UI is browser-verified per the constitution. No full TDD ordering is imposed.

**Organization**: Tasks grouped by user story (US1 = Browse slots, US2 = Book + Zoom, US3 = Manage bookings) to enable independent implementation and delivery. Backend package `com.kuky.backend.scheduling` mirrors the existing `auth` package (plain JDBC, POJO models, record DTOs).

## Format: `[ID] [P?] [Story?] Description — file path`

- **[P]**: Can run in parallel (different files, no incomplete-task dependencies)
- **[Story]**: Which user story (US1, US2, US3); Setup/Foundational/Polish carry no story label
- All backend paths are under `back-end/src/main/java/com/kuky/backend/`; tests under `back-end/src/test/java/com/kuky/backend/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configuration plumbing for scheduling + Zoom. No user story work begins here.

- [x] T001 [P] Create `SchedulingProperties` `@ConfigurationProperties` in `config/SchedulingProperties.java` — bind `app.scheduling` (teacherTimezone, teacherEmail, dayStart, dayEnd, classDurationMinutes, minLeadHours, cancelCutoffHours) and `app.zoom` (accountId, clientId, clientSecret, userId); register via `@EnableConfigurationProperties` on `BackEndApplication.java`
- [x] T002 [P] Update `back-end/src/main/resources/application.yaml` — add `app.scheduling` block (env placeholders, defaults: timezone `Europe/Madrid`, dayStart `09:00`, dayEnd `18:00`, classDurationMinutes `60`, minLeadHours `24`, cancelCutoffHours `24`, teacher-email `${TEACHER_EMAIL:noreply@kuky.es}`) and `app.zoom` block (`account-id`/`client-id`/`client-secret` from `ZOOM_*` env, `user-id: ${ZOOM_USER_ID:me}`)
- [x] T003 [P] Update `back-end/src/main/resources/application-local.yaml` — local defaults: teacher-timezone `Europe/Madrid`, teacher-email `paula@kuky.es`, leave `app.zoom` credentials blank (selects the stub provider locally)

**Checkpoint**: Backend starts with the new config bound; no behaviour change yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Booking schema, models, and persistence that every user story depends on (US1 reads bookings to mark slots BOOKED; US2 writes; US3 reads/cancels).

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T004 Create `back-end/src/main/resources/db/migration/V3__create_bookings.sql` per `data-model.md` — `bookings` table (`id` UUID PK `gen_random_uuid()`, `user_id` UUID FK → `users(id)` ON DELETE CASCADE, `slot_start` TIMESTAMPTZ, `duration_minutes` INT, `status` VARCHAR(20), `zoom_meeting_id` VARCHAR(64), `zoom_join_url` TEXT, `created_at` TIMESTAMPTZ DEFAULT NOW(), `cancelled_at` TIMESTAMPTZ); `CHECK (status IN ('CONFIRMED','CANCELLED'))`; partial unique index `bookings_active_slot_uniq ON bookings(slot_start) WHERE status='CONFIRMED'`; index `bookings_user_id_idx ON bookings(user_id)`
- [x] T005 [P] Create `Booking` POJO in `scheduling/model/Booking.java` (fields mirroring the table; getters/setters per `User.java` style)
- [x] T006 [P] Create `Slot` value object + `SlotStatus` enum (`OPEN`, `BOOKED`, `UNAVAILABLE`) in `scheduling/model/Slot.java` (start/end `Instant`, status)
- [x] T007 Create `BookingRepository` (`NamedParameterJdbcTemplate`) in `scheduling/repository/BookingRepository.java` — `RowMapper<Booking>`; methods `findConfirmedSlotStartsBetween(Instant from, Instant to)`, `findById(UUID)`, `findByUserId(UUID)`, `insert(Booking)` (throws `DuplicateKeyException` on slot clash), `markCancelled(UUID id, Instant when)`, `updateZoomDetails(UUID id, String meetingId, String joinUrl)`, `delete(UUID id)` (depends on T005)

**Checkpoint**: Schema migrates on boot; persistence layer ready.

---

## Phase 3: User Story 1 — Browse available class slots (Priority: P1) 🎯 MVP

**Goal**: Logged-out and logged-in visitors see Paula's open/unavailable slots for the current + next calendar week on `/reservas`, in their local timezone.

**Independent Test**: Open `/reservas` in an incognito window (logged out) → open and unavailable slots render correctly for the two-week horizon; `GET /api/v1/schedule` returns the slot list with no cookie (quickstart Scenario A).

- [x] T008 [US1] Implement `AvailabilityService` in `scheduling/service/AvailabilityService.java` — generate slots for the horizon (current ISO week + next, Monday-start, in teacher timezone) at `classDurationMinutes` steps between dayStart/dayEnd; convert to UTC `Instant`; mark `BOOKED` from `BookingRepository.findConfirmedSlotStartsBetween`, `UNAVAILABLE` if past or within `minLeadHours`, else `OPEN`; expose `generateSchedule()` and a `validateBookable(Instant slotStart)` helper for US2 (depends on T006, T007, T001)
- [x] T009 [P] [US1] Create `SlotResponse` and `ScheduleResponse` record DTOs in `scheduling/dto/` (ScheduleResponse: teacherTimezone, horizonStart, horizonEnd, List<SlotResponse>; SlotResponse: start, end, status) per `contracts/api.md` §1
- [x] T010 [US1] Implement `ScheduleController` `GET /api/v1/schedule` in `scheduling/controller/ScheduleController.java` returning `ScheduleResponse` (depends on T008, T009)
- [x] T011 [US1] Update `config/SecurityConfig.java` — add `.requestMatchers(HttpMethod.GET, "/api/v1/schedule").permitAll()` before `anyRequest().authenticated()`
- [x] T012 [P] [US1] Backend unit test `AvailabilityServiceTest` in `scheduling/AvailabilityServiceTest.java` — horizon bounds (current+next week), slot alignment/count for 60-min duration, lead-window slots marked UNAVAILABLE, booked slot_start marked BOOKED
- [x] T013 [P] [US1] Create frontend API client `front-end/src/lib/scheduling.ts` with `getSchedule()` (types `Slot`, `SlotStatus`, `ScheduleResponse`; `fetch` with `credentials: "include"`, mirroring `lib/auth.ts` `apiCall`)
- [x] T014 [P] [US1] Create `SlotGrid` component in `front-end/src/components/scheduling/SlotGrid.tsx` — render a week's slots grouped by day, open vs unavailable styling, times localized via `Intl.DateTimeFormat`; emits `onSelect(slot)` for OPEN slots
- [x] T015 [US1] Create `ScheduleView` component in `front-end/src/components/scheduling/ScheduleView.tsx` — fetch via `getSchedule()`, week navigation (current/next), render `SlotGrid`, loading + empty-state ("no hay horas disponibles") per FR-019 (depends on T013, T014)
- [x] T016 [US1] Replace placeholder in `front-end/src/routes/reservas.tsx` with `ScheduleView` (publicly viewable; keep head/meta) (depends on T015)
- [ ] T017 [US1] Browser-verify US1 per quickstart Scenario A (logged-out viewing, local timezone, open/unavailable distinction)

**Checkpoint**: Public schedule fully functional and demoable as the MVP.

---

## Phase 4: User Story 2 — Book a 1-on-1 class with automatic Zoom meeting (Priority: P1)

**Goal**: A signed-in student books an open slot; the slot is reserved exclusively, a Zoom meeting is created, the join link is shown and emailed to student + Paula.

**Independent Test**: Signed in, book an OPEN slot ≥24 h out → `201` with a Zoom join link; the slot shows BOOKED for everyone; emails appear in Mailpit; concurrency/lead-time/Zoom-failure guards behave per quickstart Scenarios B & C.

- [x] T018 [P] [US2] Create scheduling exceptions `SlotUnavailableException`, `BookingNotAllowedException` (with a reason enum: RANGE, LEAD, STATE), `MeetingProvisioningException` in `scheduling/exception/`
- [x] T019 [US2] Update `config/GlobalExceptionHandler.java` — map `SlotUnavailableException`→409 `SLOT_UNAVAILABLE` (also translate `org.springframework.dao.DuplicateKeyException`→409 `SLOT_UNAVAILABLE`), `BookingNotAllowedException` reasons→422 `SLOT_OUT_OF_RANGE`/`BOOKING_TOO_SOON`, `MeetingProvisioningException`→502 `MEETING_PROVISIONING_FAILED` (depends on T018)
- [x] T020 [P] [US2] Create `MeetingProvider` interface + `MeetingDetails` record (meetingId, joinUrl) in `scheduling/meeting/MeetingProvider.java` — `create(Instant start, int durationMinutes, String topic)`, `cancel(String meetingId)`
- [x] T021 [P] [US2] Implement `StubMeetingProvider` in `scheduling/meeting/StubMeetingProvider.java` — `@ConditionalOnProperty` selecting it when `app.zoom.client-id` is blank; returns placeholder join URL and logs a WARN (mirrors `EmailService` local behaviour)
- [x] T022 [US2] Implement `ZoomMeetingProvider` in `scheduling/meeting/ZoomMeetingProvider.java` — Server-to-Server OAuth token fetch + in-memory caching; create/cancel via Spring `RestClient`; active when Zoom credentials are present (depends on T020, T001)
- [x] T023 [P] [US2] Create `BookingEmailService` in `scheduling/service/BookingEmailService.java` — `sendConfirmation(studentEmail, teacherEmail, slot, joinUrl)` via the existing `JavaMailSender`; failures logged, non-fatal
- [x] T024 [P] [US2] Create `CreateBookingRequest` (slotStart, `@NotNull`) and `BookingResponse` record DTOs in `scheduling/dto/` per `contracts/api.md` §2
- [x] T025 [US2] Implement `BookingService.createBooking(userEmail, slotStart)` in `scheduling/service/BookingService.java` — resolve user via `UserRepository`; validate via `AvailabilityService.validateBookable`; insert CONFIRMED row (reserve); call `MeetingProvider.create`; on failure delete the row + throw `MeetingProvisioningException`; on success `updateZoomDetails` + `BookingEmailService.sendConfirmation` (depends on T007, T008, T020, T023, T018)
- [x] T026 [US2] Implement `BookingController` `POST /api/v1/bookings` in `scheduling/controller/BookingController.java` — `@AuthenticationPrincipal String email`, `@Valid CreateBookingRequest`, returns 201 `BookingResponse` (depends on T025, T024)
- [x] T027 [P] [US2] Backend integration test `BookingControllerIntegrationTest` in `scheduling/BookingControllerIntegrationTest.java` — happy path (201 + join link), concurrent same-slot → one 201 + one 409, lead-time → 422, Zoom-failure → 502 with no surviving booking
- [x] T028 [US2] Add `createBooking(slotStart)` to `front-end/src/lib/scheduling.ts` (POST, returns BookingResponse; surfaces `SLOT_UNAVAILABLE`/`BOOKING_TOO_SOON`/`MEETING_PROVISIONING_FAILED`) (depends on T013)
- [x] T029 [P] [US2] Create `BookingDialog` component in `front-end/src/components/scheduling/BookingDialog.tsx` — Shadcn `Dialog` confirming the selected slot, shows join link on success, maps error codes to Spanish messages, prompts sign-in (link to `/cuenta`) if not authenticated
- [x] T030 [US2] Wire booking into `ScheduleView`/`reservas.tsx` — selecting an OPEN slot opens `BookingDialog` (auth-gated via existing `getMe()`); on success refetch the schedule so the slot flips to BOOKED (SC-005) (depends on T029, T016, T028)
- [ ] T031 [US2] Browser-verify US2 per quickstart Scenarios B (book + Zoom + emails + concurrency/lead guards) and C (Zoom-failure does not finalize)

**Checkpoint**: Core book-a-class flow works end-to-end; US1 + US2 form the deliverable MVP.

---

## Phase 5: User Story 3 — View and manage my bookings (Priority: P2)

**Goal**: A signed-in student sees upcoming/past bookings with join links and can cancel an upcoming class (≥24 h before start), which frees the slot, cancels the Zoom meeting, and notifies Paula.

**Independent Test**: With an existing booking, `GET /api/v1/bookings` returns it under `upcoming` with `cancellable: true`; cancelling (≥24 h out) returns 204, the slot reopens publicly, Paula is emailed; cancelling inside the 24 h cutoff returns 422 (quickstart Scenario D).

- [x] T032 [P] [US3] Add `BookingNotFoundException` in `scheduling/exception/` and update `config/GlobalExceptionHandler.java` — `BookingNotFoundException`→404 `BOOKING_NOT_FOUND`, plus `BookingNotAllowedException` STATE→409 `ALREADY_CANCELLED` and a CUTOFF reason→422 `CANCELLATION_TOO_LATE`
- [x] T033 [US3] Add `sendCancellation(teacherEmail, slot)` to `back-end/.../scheduling/service/BookingEmailService.java` notifying Paula of the freed slot (depends on T023)
- [x] T034 [US3] Implement `BookingService.listForUser(email)` (split upcoming/past by slot end vs now, compute `cancellable`) and `cancelBooking(email, id)` (ownership check → 404; already-cancelled → 409; cutoff check → 422; `markCancelled`; `MeetingProvider.cancel`; `sendCancellation`) in `scheduling/service/BookingService.java` (depends on T025, T020, T033)
- [x] T035 [P] [US3] Create `MyBookingsResponse` + `BookingSummary` record DTOs in `scheduling/dto/` per `contracts/api.md` §3 (upcoming/past lists, cancellable flag)
- [x] T036 [US3] Implement `BookingController` `GET /api/v1/bookings` and `DELETE /api/v1/bookings/{id}` in `scheduling/controller/BookingController.java` (204 on cancel) (depends on T034, T035)
- [x] T037 [US3] Add `listBookings()` and `cancelBooking(id)` to `front-end/src/lib/scheduling.ts` (depends on T028)
- [x] T038 [P] [US3] Create `MyBookings` component in `front-end/src/components/scheduling/MyBookings.tsx` — upcoming/past sections, join link, cancel button disabled when `!cancellable`, confirm + error handling
- [x] T039 [US3] Render `MyBookings` on `front-end/src/routes/reservas.tsx` for authenticated users (via `getMe()`); refetch schedule + bookings after a cancel so the slot reopens (depends on T038, T030, T037)
- [ ] T040 [US3] Browser-verify US3 per quickstart Scenario D (list, cancel frees slot + emails Paula, cutoff enforced)

**Checkpoint**: All three user stories independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation and full-suite validation across stories.

- [x] T041 [P] Update root `CLAUDE.md` — change `/reservas` row in "Current pages" to ✅ Live (schedule + booking), and note the scheduling/Zoom env vars (`TEACHER_EMAIL`, `ZOOM_ACCOUNT_ID`, `ZOOM_CLIENT_ID`, `ZOOM_CLIENT_SECRET`, `ZOOM_USER_ID`) in the production env-vars table
- [x] T042 [P] Run `./gradlew test` (backend) and `npm run lint` + `npm run build` (frontend); fix any failures
- [ ] T043 Run full quickstart.md validation end-to-end (Scenarios A–D), confirming success-criteria mapping (SC-001…SC-007)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational — the MVP slice, fully independent
- **US2 (Phase 4)**: Depends on Foundational; reuses US1's `AvailabilityService` (T008) and `ScheduleView`/`lib/scheduling.ts` (T015/T013). Build after US1.
- **US3 (Phase 5)**: Depends on Foundational; reuses US2's `BookingService`/`MeetingProvider`/`BookingEmailService` and frontend client. Build after US2.
- **Polish (Phase 6)**: After all targeted stories complete

### Within Each User Story

- DTOs + exceptions ([P]) can precede or parallel the service/controller that uses them
- Models before repository; repository/services before controllers; backend endpoint before its frontend client method; client method before the component that calls it
- Browser-verify task is last in each story

### Parallel Opportunities

- Setup: T001, T002, T003 are all [P] (different files)
- Foundational: T005, T006 [P]; T007 follows T005
- US1: T009, T012, T013, T014 are [P]; T008→T010→T011 backend chain; FE T013/T014→T015→T016
- US2: T018, T020, T021, T023, T024 are [P]; service chain T025→T026; FE T029 [P] then T030
- US3: T032, T035, T038 are [P]; service T034→controller T036; FE T037→T039
- Backend and frontend tracks within a story can be developed in parallel by two people up to the wiring task

---

## Parallel Example: User Story 1

```bash
# After T008 (AvailabilityService) lands, run the independent US1 tasks together:
Task: "T009 Create SlotResponse/ScheduleResponse DTOs in scheduling/dto/"
Task: "T012 AvailabilityServiceTest in scheduling/AvailabilityServiceTest.java"
Task: "T013 Create front-end/src/lib/scheduling.ts getSchedule()"
Task: "T014 Create front-end/src/components/scheduling/SlotGrid.tsx"
```

---

## Implementation Strategy

### MVP scope

US1 + US2 (both P1) together form the MVP: a public schedule plus the ability for a signed-in student to book a Zoom-backed class. US1 alone is independently demoable (read-only schedule) and is the smallest shippable increment.

1. Complete Phase 1 (Setup) + Phase 2 (Foundational)
2. Complete Phase 3 (US1) → **STOP & VALIDATE** Scenario A → demoable read-only schedule
3. Complete Phase 4 (US2) → validate Scenarios B & C → MVP booking flow
4. Complete Phase 5 (US3) → validate Scenario D → full management
5. Phase 6 polish → run `tasks` validation + docs

### Incremental Delivery

Each story is a deployable increment: US1 (browse) → US2 (book) → US3 (manage). No later story breaks an earlier one; the public view simply reflects new bookings/cancellations.

---

## Notes

- [P] = different files, no incomplete-task dependencies
- Backend mirrors the existing `auth` package: plain JDBC repositories, POJO models, record DTOs, error envelope `{"error","message"}`
- No new backend dependency: Zoom via Spring `RestClient`, email via existing `JavaMailSender`
- Zoom is optional locally (StubMeetingProvider); set `ZOOM_*` env vars to exercise the real provider
- All UI tasks require browser verification before being marked complete (constitution: Development Workflow)
- Commit after each task or logical group
