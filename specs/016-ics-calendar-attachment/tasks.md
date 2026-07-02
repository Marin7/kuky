---

description: "Task list for Calendar (.ics) Attachment for Bookings"
---

# Tasks: Calendar (.ics) Attachment for Bookings

**Input**: Design documents from `/specs/016-ics-calendar-attachment/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/ics-attachment.md](./contracts/ics-attachment.md), [quickstart.md](./quickstart.md)

**Tests**: Included — `plan.md`'s Project Structure explicitly calls out `IcsEventFactoryTest.java` and an extended `BookingEmailServiceTest.java`.

**Organization**: Tasks are grouped by user story (US1/US2/US3, matching `spec.md` priorities P1/P2/P3) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- All back-end paths are relative to the repository root (`back-end/...`)

## Path Conventions

Existing single Spring Boot back-end (`back-end/src/main/java/com/kuky/backend/scheduling/...`, `back-end/src/test/java/com/kuky/backend/scheduling/...`) — no front-end changes, per `plan.md`.

---

## Phase 1: Setup

**Purpose**: Confirm the project is ready for this feature; no new dependencies or scaffolding are required (per `research.md`).

- [X] T001 Verify `back-end/build.gradle` already provides `spring-boot-starter-mail` (needed for `JavaMailSender`/`MimeMessage`) and that no new dependency is required for this feature.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared ICS-generation and multipart-email infrastructure that every user story's emails depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 Create `IcsEventFactory` in `back-end/src/main/java/com/kuky/backend/scheduling/service/IcsEventFactory.java`: builds a minimal `VCALENDAR`/`VEVENT` iCalendar payload (RFC 5545/5546) from booking data, per `data-model.md`'s `CalendarEvent` shape and `contracts/ics-attachment.md`'s payload structure — supports both `METHOD:REQUEST` (sequence 0, `STATUS:CONFIRMED`) and `METHOD:CANCEL` (sequence 1, `STATUS:CANCELLED`) for a given `bookingId`/`slotStart`/`durationMinutes`/`zoomJoinUrl`/organizer+attendee emails, with UTC (`Z`-suffixed) `DTSTART`/`DTEND`, `UID` = `booking-<bookingId>@kuky.es`, and RFC 5545-compliant line folding (75 octets) + CRLF line endings.
- [X] T003 [P] Create `IcsEventFactoryTest` in `back-end/src/test/java/com/kuky/backend/scheduling/service/IcsEventFactoryTest.java`: asserts `REQUEST` and `CANCEL` payloads contain correct `UID`, `METHOD`, `SEQUENCE`, `STATUS`, UTC `DTSTART`/`DTEND`, `SUMMARY`, `DESCRIPTION`/`LOCATION` (Zoom link), and that both payloads for the same booking share the same `UID`.
- [X] T004 Refactor `BookingEmailService`'s internal send helper in `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java` from `SimpleMailMessage` to `JavaMailSender.createMimeMessage()` + `MimeMessageHelper` (multipart), adding an overload that accepts an optional `.ics` byte payload to attach as `clase.ics` with the correct `text/calendar; charset=UTF-8; method=REQUEST|CANCEL` content type; wrap ICS attachment logic in its own try/catch so any failure falls back to sending the email without the attachment (FR-007), logging a warning.

**Checkpoint**: Foundation ready — `IcsEventFactory` and the attachment-capable mail helper exist; user story wiring can now begin.

---

## Phase 3: User Story 1 - Add booked class to personal calendar (Priority: P1) 🎯 MVP

**Goal**: The student's booking confirmation email includes a `REQUEST` calendar attachment for the newly booked class.

**Independent Test**: Book a class as a student (per `quickstart.md` Scenario 1), open the confirmation email in Mailpit, and verify it has a `clase.ics` attachment that imports as a correctly-timed event with the Zoom link.

### Implementation for User Story 1

- [X] T005 [US1] Update `BookingService.createBooking` in `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java` to pass `booking.getId()` through to `emailService.sendConfirmation(...)`.
- [X] T006 [US1] Update `BookingEmailService.sendConfirmation` in `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java` to accept the booking id, build a `REQUEST` `CalendarEvent` via `IcsEventFactory`, and attach `clase.ics` to the **student's** confirmation email (teacher's copy is handled in US3).
- [X] T007 [P] [US1] Extend `BookingEmailServiceTest` in `back-end/src/test/java/com/kuky/backend/scheduling/service/BookingEmailServiceTest.java` to assert the student confirmation email carries a `REQUEST` ics attachment with the correct `UID`, `DTSTART`, and `DTEND` matching the booking.
- [X] T008 [US1] Manually run `quickstart.md` Scenario 1 against local dev (Mailpit) and confirm the imported event is correctly timed with the Zoom link.

**Checkpoint**: User Story 1 fully functional and independently testable — students get a working calendar attachment on booking.

---

## Phase 4: User Story 2 - Calendar stays accurate after cancellation (Priority: P2)

**Goal**: Whenever a booking is canceled (by the student or by the teacher/admin), the student receives a cancellation email carrying a `CANCEL` calendar update matching the original event's `UID` (FR-005).

**Independent Test**: Book then cancel a class (per `quickstart.md` Scenario 3, both as self-cancel and as teacher-cancel), and verify the student's cancellation email's `.ics` attachment cancels the matching event (same `UID`, higher `SEQUENCE`).

### Implementation for User Story 2

- [X] T009 [US2] Update `BookingService.cancelBooking` in `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java` to pass `booking.getId()`, `booking.getDurationMinutes()`, and `booking.getZoomJoinUrl()` through to `emailService.sendCancellation(...)`.
- [X] T010 [US2] Update `BookingEmailService.sendCancellation` in `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java` to also send the **student** a cancellation email (previously this method only notified the teacher) carrying a `CANCEL` ics attachment built via `IcsEventFactory` (same `UID` as the original booking, `SEQUENCE=1`) — satisfies FR-005 for the student self-cancel path.
- [X] T011 [US2] Update `BookingService.cancelBookingAsAdmin` in `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java` to pass `booking.getId()`, `booking.getDurationMinutes()`, and `booking.getZoomJoinUrl()` through to `emailService.sendCancellationByTeacher(...)`.
- [X] T012 [US2] Update `BookingEmailService.sendCancellationByTeacher` in `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java` to attach the same `CANCEL` ics (same `UID`/`SEQUENCE` scheme as T010) to the **student's** cancellation email for the teacher-initiated cancellation path.
- [X] T013 [P] [US2] Extend `BookingEmailServiceTest` to assert both cancellation paths (`sendCancellation`, `sendCancellationByTeacher`) send the student a `CANCEL` ics with `UID` matching the original confirmation and `SEQUENCE` greater than 0.
- [X] T014 [US2] Manually run `quickstart.md` Scenario 3 against local dev and confirm the cancellation `.ics` cancels the previously imported event rather than duplicating it.

**Checkpoint**: User Stories 1 AND 2 both work independently — students get accurate calendar entries through the full booking lifecycle.

---

## Phase 5: User Story 3 - Teacher's own calendar stays in sync (Priority: P3)

**Goal**: The teacher's booking notification email gets the same `REQUEST` attachment as the student's, and her cancellation notification gets the matching `CANCEL` attachment (FR-006, FR-010).

**Independent Test**: Book then cancel a class (per `quickstart.md` Scenario 2), and verify the teacher's notification and cancellation emails carry matching `REQUEST`/`CANCEL` ics attachments for the same class.

### Implementation for User Story 3

- [X] T015 [US3] Update `BookingEmailService.sendConfirmation` in `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java` to also attach the `REQUEST` ics (built in T006) to the **teacher's** "Nueva reserva" notification email.
- [X] T016 [US3] Update `BookingEmailService.sendCancellation` and `sendCancellationByTeacher` in `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java` to also attach the `CANCEL` ics (built in T010/T012) to the **teacher's** own cancellation notification email, regardless of who initiated the cancellation.
- [X] T017 [P] [US3] Extend `BookingEmailServiceTest` to assert the teacher's notification and cancellation emails carry ics attachments matching the student's (same `UID`/`METHOD`/`SEQUENCE`).
- [X] T018 [US3] Manually run `quickstart.md` Scenario 2 against local dev and confirm the teacher's notification email carries a valid attachment for the same class.

**Checkpoint**: All three user stories independently functional — students and the teacher both get accurate calendar attachments across the booking lifecycle.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verify the non-functional requirements that span all stories (FR-007, FR-009) and close out the feature.

- [X] T019 [P] Add a `BookingEmailServiceTest` case simulating `IcsEventFactory` throwing during attachment generation and asserting the underlying email is still sent successfully without an attachment (FR-007, `quickstart.md` Scenario 4).
- [X] T020 [P] Add/verify a `BookingReminderScheduler`-related test or manual check (per `quickstart.md` Scenario 5) confirming `sendReminderToStudent`/`sendReminderToTeacher` remain unmodified and never attach an ics file (FR-009 regression guard).
- [X] T021 Review `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java` and `IcsEventFactory.java` for dead code left over from the `SimpleMailMessage` removal and confirm line-folding/CRLF handling matches `contracts/ics-attachment.md`.
- [X] T022 Run `./gradlew test` and `./gradlew build` from `back-end/` and confirm the full suite passes.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories (T005–T018 all need `IcsEventFactory` and the multipart mail helper from T002–T004).
- **User Stories (Phase 3–5)**: All depend on Foundational phase completion.
  - US1 (T005–T008) has no dependency on US2/US3.
  - US2 (T009–T014) has no dependency on US1's code changes, but reuses the `IcsEventFactory` REQUEST/CANCEL logic already built in Phase 2.
  - US3 (T015–T018) builds on the `REQUEST`/`CANCEL` ics objects constructed in US1 (T006) and US2 (T010/T012) — attaching them to the teacher's copies of the same emails — so it is sequenced after US1 and US2 in this plan, though it touches no code that US1/US2 depend on (safe to defer or drop without breaking either).
- **Polish (Phase 6)**: Depends on all three user stories being complete.

### Parallel Opportunities

- T003 (`IcsEventFactoryTest`) can be written in parallel with T004 once T002 (`IcsEventFactory`) exists.
- T007, T013, T017 (test extensions) are marked `[P]` relative to other same-phase implementation tasks touching different concerns, but all edit `BookingEmailServiceTest.java` — coordinate if working in parallel to avoid merge conflicts.
- T019 and T020 (Phase 6) are independent of each other and can run in parallel.

---

## Parallel Example: Foundational Phase

```bash
# After T002 (IcsEventFactory) exists:
Task: "Create IcsEventFactoryTest in back-end/src/test/java/com/kuky/backend/scheduling/service/IcsEventFactoryTest.java"
Task: "Refactor BookingEmailService to MimeMessage + optional ics attachment"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001).
2. Complete Phase 2: Foundational (T002–T004) — CRITICAL, blocks all stories.
3. Complete Phase 3: User Story 1 (T005–T008).
4. **STOP and VALIDATE**: Run `quickstart.md` Scenario 1 independently.
5. Deploy/demo if ready — students already get working calendar attachments on booking.

### Incremental Delivery

1. Setup + Foundational → Foundation ready.
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!).
3. Add User Story 2 → Test independently (cancellation now keeps the student's calendar accurate) → Deploy/Demo.
4. Add User Story 3 → Test independently (teacher's own calendar now stays in sync too) → Deploy/Demo.
5. Phase 6 Polish closes out FR-007/FR-009 regression coverage and runs the full test suite.

---

## Notes

- `[P]` tasks touch different files or independent concerns within the same file — verify no overlapping edits before running truly in parallel.
- `[Story]` labels map every Phase 3–5 task to its user story for traceability back to `spec.md`.
- This is a back-end-only feature — no front-end tasks are needed (per `plan.md`'s Structure Decision).
- Commit after each task or logical group, per repository convention.
- Stop at any checkpoint (end of Phase 3, 4, or 5) to validate that story independently before continuing.
