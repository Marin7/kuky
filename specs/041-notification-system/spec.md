# Feature Specification: In-App Activity Notifications

**Feature Branch**: `041-notification-system`

**Created**: 2026-08-15

**Status**: Draft

**Input**: User description: "Let's create a notification system. When a student does a homework/test -> the teacher gets a notification. Similarly, when the teacher assigns something to a student, the student will get a notification when logging-in or when accesing the website. Add a small notification icon to the Panel and then to Tareas or Pruebas de evaluacion depending on where the news is. Similarly, for students, add a small notification icon to Mi aprendizaje."

## Clarifications

### Session 2026-08-15

- Q: When is a teacher notification marked seen? → A: Only opening that student’s submitted work (the review or result) marks that one submission as seen; other unseen submissions on the same homework or test keep their icons. Opening it from the student profile counts too.
- Q: Does adding homework to a unit the student already has create a notification? → A: No — only assigning a unit or a test creates a student notification.
- Q: When is a student assignment notification marked seen? → A: Opening the assigned unit or the assigned test in Mi aprendizaje marks that notification as seen; they do not need to start or submit.
- Q: How does the teacher see which students still have unseen submissions? → A: Icon on that homework or test, and also on each student whose submitted work she has not opened yet.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher sees that a student turned in homework (Priority: P1)

A student submits a homework. The next time the teacher is on the site (including after login, or while already signed in and opening or moving around the site), she sees a small notification icon on **Panel**. Opening Panel, she also sees the same kind of icon on the **Tareas** tab — not on **Pruebas de evaluación** — so she knows the news is homework, not a test. She opens Tareas, finds the homework that was just turned in (it also carries a small indicator), and opens **that student’s submitted work** (the review or result). In the list of students for that homework, each student with unseen submitted work also shows a small indicator. Opening the homework itself (for example the editor or a list of students) is not enough to clear those. After she has opened that submission — from Tareas or from that student’s profile — the icons for **that** submission go away (including that student’s row); other unseen submissions on the same homework keep theirs, and the homework keeps its indicator until none remain.

**Why this priority**: This is the teacher’s half of the request and the most frequent “something happened while I was away” event. Without it, Paula only discovers submissions by remembering to check Tareas.

**Independent Test**: As a student, submit a homework. As the teacher, open the site (or refresh / move to another page if already signed in) and confirm a small icon on Panel and on Tareas, none on Pruebas de evaluación; open that student’s submitted work (not only the homework editor) and confirm the icons for it disappear.

**Acceptance Scenarios**:

1. **Given** a student has just submitted a homework, **When** the teacher next loads or navigates the site, **Then** she sees a small notification icon on **Panel**.
2. **Given** that homework submission is unseen, **When** the teacher opens Panel, **Then** she sees a small notification icon on the **Tareas** tab and does not see one on **Pruebas de evaluación** from this event.
3. **Given** unseen homework submissions exist, **When** the teacher opens Tareas, **Then** the homework that received the new work also shows a small indicator so she can tell which one has news.
4. **Given** that homework has unseen submissions, **When** the teacher opens it and sees the list of students, **Then** each student whose submitted work she has not opened yet shows a small indicator.
5. **Given** the teacher opens that student’s submitted homework (review or result), from Tareas or from the student’s profile, **When** she returns to Panel, **Then** the icons for that submission are gone (including that student’s row). If other unseen homework submissions remain — including another student on the same homework — Tareas (and Panel) still show an icon, the homework still shows an indicator, and the other students’ rows still show theirs.
6. **Given** the teacher opens the homework from Tareas but only sees the editor or the list of students, without opening a specific submission, **When** she returns to Panel, **Then** the icons for those unseen submissions remain, including on those students’ rows.
7. **Given** a homework is fully auto-graded on submit, **When** the student submits, **Then** the teacher still gets the same homework notification (turning work in is the event, not “needs my review”).

---

### User Story 2 - Teacher sees that a student submitted a test (Priority: P1)

A student submits a prueba de evaluación (quiz). The teacher sees a small notification icon on **Panel** and, inside Panel, on **Pruebas de evaluación** — not on **Tareas**. She opens that tab, finds the test with the new attempt, and opens **that student’s submitted attempt** (review or result). In the list of attempts, each student with unseen submitted work also shows a small indicator. Opening the test itself (for example the editor or a list of attempts) is not enough to clear those. After she has opened that attempt — from Pruebas de evaluación or from that student’s profile — the icons for **that** attempt go away (including that student’s row); other unseen attempts on the same test keep theirs, and the test keeps its indicator until none remain.

**Why this priority**: Tests are the other student-work event in the request. They must route to a different tab than homework so the teacher does not open the wrong place.

**Independent Test**: As an assigned student, submit a test. As the teacher, confirm icons on Panel and Pruebas de evaluación only; open that student’s submitted attempt (not only the test editor); confirm those icons clear for that attempt.

**Acceptance Scenarios**:

1. **Given** a student has just submitted a test, **When** the teacher next loads or navigates the site, **Then** she sees a small notification icon on **Panel**.
2. **Given** that test submission is unseen, **When** the teacher opens Panel, **Then** she sees a small notification icon on **Pruebas de evaluación** and does not see one on **Tareas** from this event.
3. **Given** unseen test submissions exist, **When** the teacher opens Pruebas de evaluación, **Then** the test that received the new attempt also shows a small indicator.
4. **Given** that test has unseen attempts, **When** the teacher opens it and sees the list of attempts, **Then** each student whose submitted attempt she has not opened yet shows a small indicator.
5. **Given** the teacher opens that student’s submitted attempt (review or result), from Pruebas de evaluación or from the student’s profile, **When** she returns to Panel, **Then** the icons for that attempt are gone (including that student’s row). Remaining unseen test attempts — including another student on the same test — keep the Pruebas de evaluación (and Panel) icon, the test still shows an indicator, and the other students’ rows still show theirs.
6. **Given** the teacher opens the test from Pruebas de evaluación but only sees the editor or the list of attempts, without opening a specific attempt, **When** she returns to Panel, **Then** the icons for those unseen attempts remain, including on those students’ rows.
7. **Given** both an unseen homework submission and an unseen test submission, **When** the teacher opens Panel, **Then** both **Tareas** and **Pruebas de evaluación** show an icon, and **Panel** shows one icon covering both.

---

### User Story 3 - Student sees newly assigned work on Mi aprendizaje (Priority: P1)

The teacher assigns a unit (which gives the student that unit’s homework and related learning content) or assigns a prueba de evaluación. The next time that student logs in or opens the site, they see a small notification icon on **Mi aprendizaje**. Inside that area, the newly assigned unit or test also shows a small indicator. **Opening the unit or the test** (from Mi aprendizaje, or opening the test from its own page) clears that indicator; they do not need to start or submit the work. The nav icon stays until nothing unseen remains.

**Why this priority**: This is the student’s half of the request. Assigned work is easy to miss if the student only notices it when they happen to browse learning.

**Independent Test**: As the teacher, assign a unit to a student and a test to another (or the same) student. As each student, log in or open the site and confirm a small icon on Mi aprendizaje and on the new item; open the unit or the test (without submitting) and confirm that item’s indicator (and the nav icon, if it was the last one) disappear.

**Acceptance Scenarios**:

1. **Given** the teacher has just assigned a unit to a student, **When** that student next logs in or loads or navigates the site, **Then** they see a small notification icon on **Mi aprendizaje**.
2. **Given** the teacher has just assigned a test to a student, **When** that student next logs in or loads or navigates the site, **Then** they see a small notification icon on **Mi aprendizaje**.
3. **Given** unseen assigned work exists, **When** the student opens Mi aprendizaje, **Then** each newly assigned unit and each newly assigned test shows a small indicator.
4. **Given** the student opens the newly assigned unit or test (they do not need to start or submit), **When** they look at navigation, **Then** that item’s indicator is gone; **Mi aprendizaje** still shows an icon if other unseen assigned items remain, and shows none if that was the last one.
5. **Given** the teacher assigned the same unit or test to several students, **When** each of those students opens the site, **Then** each of them sees their own icon; a student who was not assigned does not.
6. **Given** a visitor who is not signed in, or an account that is not a student, **When** they view the site, **Then** they do not see a Mi aprendizaje notification icon for assigned work.
7. **Given** a student already has a unit assigned, **When** the teacher later adds or edits homework on that unit, **Then** that student does not get a new notification icon.
8. **Given** an unseen assigned unit, **When** the student opens only a homework that belongs to that unit and does not open the unit itself, **Then** the unit’s notification stays unseen.
9. **Given** an unseen assigned test, **When** the student opens that test from Mi aprendizaje or from the test page but does not submit, **Then** the test assignment is marked seen.

---

### User Story 4 - Icons stay until the news is actually opened (Priority: P2)

Notifications are not a separate inbox or email. They are small icons that remain until the person opens the relevant item: for the teacher, **that student’s submitted homework or test** (review or result), including from the student’s profile; for the student, **opening the newly assigned unit or test** (they do not need to start or submit). Visiting Panel, Tareas, Pruebas de evaluación, or Mi aprendizaje is enough to *see* where the news is; opening a homework or test editor or a list of students/attempts is not enough to clear teacher icons; opening only a homework inside a unit is not enough to clear a unit assignment. If they leave without opening the new work, the icons are still there next time.

**Why this priority**: Persistence is what makes the icons trustworthy. If they vanished on a glance at Panel, the teacher could still miss which homework was turned in.

**Independent Test**: Create unseen submissions from two students on the same homework; as teacher, confirm icons on Panel, Tareas, the homework, and both student rows; open Panel, Tareas, and the homework list but not a submission — icons still present. Open one student’s submitted work from Tareas or from the student profile — that student’s row and icons for that submission gone; the other student, the homework, Tareas, and Panel still show icons.

**Acceptance Scenarios**:

1. **Given** unseen teacher news exists, **When** the teacher opens Panel, the matching tab, or the homework/test editor or student/attempt list, but does not open that student’s submitted work, **Then** the Panel and tab icons remain.
2. **Given** unseen student news exists, **When** the student opens Mi aprendizaje but does not open the new unit or test, **Then** the Mi aprendizaje icon remains.
3. **Given** the teacher has opened that student’s submitted work (from Tareas, Pruebas de evaluación, or the student profile), **When** she next loads the site, **Then** that submission’s icons are gone; other unseen submissions (including on the same homework or test) are unaffected.
4. **Given** the teacher is already signed in on another page when a student submits, **When** she next loads or navigates the site, **Then** she sees the icons without needing to log out and back in.

---

### Edge Cases

- **Starting but not submitting**: Opening or saving progress on a homework or test without submitting does not notify the teacher.
- **Several students, one homework or test**: Each submission is its own unseen event. Opening one student’s submitted work does not clear another student’s, even on the same homework or test. The homework or test keeps its indicator until every unseen submission on it has been opened; each unseen student still shows a row indicator.
- **Teacher opens the homework or test, not the submission**: Opening the editor or a list of students/attempts does not mark any submission as seen. Student-row indicators stay until she opens that student’s submitted work.
- **Teacher reviews from the student profile**: Opening that student’s submitted homework or test from their profile marks that same notification as seen as if she had opened it from Tareas or Pruebas de evaluación.
- **Assigning someone who already has that unit or test**: No second notification for work they already had.
- **Unassign before they see it**: If the teacher removes the student from the unit or test before they open the assigned item, that student’s notification for it disappears (there is nothing new to open).
- **New homework on an existing unit**: Adding homework (or editing homework) on a unit the student already has does not create a student notification. Only a new unit assignment or a new test assignment does.
- **Student opens a homework but not the unit**: Opening only a homework that belongs to a newly assigned unit does not mark the unit assignment as seen. They must open the unit itself.
- **Student opens the unit or test without doing the work**: Opening the assigned unit or test marks that assignment as seen even if they do not start or submit.
- **Student submits then teacher is already on Tareas**: Icons appear on the teacher’s next load or navigation; they are not required to pop in the same instant without any page change.
- **Presentation activities**: Submitting or assigning presentation activities does not create these notifications (homework and tests only, plus unit assignment for the student).
- **Teacher grading or leaving feedback**: Does not notify the student in this feature.
- **Past work from before this ships**: Existing submissions and assignments are not backfilled as unseen; only events after the feature is live create icons.
- **Deleted homework, test, or unit**: If the work is removed, related unseen icons for it go away.
- **Teacher’s own account**: Assigning work does not notify the teacher; submitting as a student-role check account still notifies the teacher as a submission if that account is a student.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST create an unseen teacher notification when a student submits a homework.
- **FR-002**: The system MUST create an unseen teacher notification when a student submits a prueba de evaluación (test).
- **FR-003**: The system MUST create an unseen student notification when the teacher assigns that student to a unit they did not already have.
- **FR-004**: The system MUST create an unseen student notification when the teacher assigns that student a prueba de evaluación they did not already have.
- **FR-017**: Adding or editing homework on a unit the student already has MUST NOT create a student notification. Student notifications MUST be created only by assigning a unit they did not already have or assigning a test they did not already have.
- **FR-005**: Teacher notifications MUST be visible as a small icon on the **Panel** navigation item whenever the teacher has any unseen homework or test submissions.
- **FR-006**: Unseen homework submissions MUST also show a small icon on the **Tareas** tab inside Panel; unseen test submissions MUST also show a small icon on the **Pruebas de evaluación** tab. Each tab’s icon reflects only its own kind of news.
- **FR-007**: Student notifications MUST be visible as a small icon on the **Mi aprendizaje** navigation item whenever that student has any unseen assigned units or tests.
- **FR-008**: The homework, test, unit, or assigned test that the news refers to MUST itself show a small indicator until the recipient opens that specific item (for the teacher: until no unseen submissions remain on that homework or test; for the student: until they open the newly assigned unit or test). The **Mi aprendizaje** icon MUST remain while that student still has any other unseen assigned unit or test.
- **FR-019**: For the teacher, each student whose submitted homework or test she has not opened yet MUST also show a small indicator on that student’s row in the homework or test list, and on that submitted work where it appears on the student’s profile. Opening that student’s submitted work MUST clear that student’s row indicator without clearing other students’ row indicators.
- **FR-009**: For the teacher, opening that student’s submitted homework or test (the review or result) MUST mark only that notification as seen. Opening it from Tareas, from Pruebas de evaluación, or from the student’s profile MUST all count. Parent icons (Panel, Tareas, Pruebas de evaluación) MUST remain while any other unseen submission still belongs there, including another student on the same homework or test.
- **FR-010**: Merely opening Panel, a Panel tab, Mi aprendizaje, a homework or test editor, or a list of students/attempts MUST NOT mark teacher notifications as seen.
- **FR-018**: For the student, opening the assigned unit or opening the assigned test MUST mark that assignment as seen. They MUST NOT need to start or submit. Opening the test from Mi aprendizaje or from the test page MUST both count. Opening only a homework that belongs to an assigned unit, without opening the unit itself, MUST NOT mark the unit assignment as seen.
- **FR-011**: Recipients MUST see their icons when they log in and when they next load or navigate the site while already signed in. They MUST NOT need a separate notification list, email, or device push for this feature.
- **FR-012**: A student MUST NOT be notified of their own submission. The teacher MUST NOT be notified of their own assign action.
- **FR-013**: Only the teacher sees teacher icons; only the assigned student sees that student’s icons. Other students and signed-out visitors MUST NOT see another person’s news.
- **FR-014**: Unassigning a student from a unit or test, or deleting that work, MUST remove any still-unseen notification that pointed at it.
- **FR-015**: Notifications MUST NOT be created for presentation-activity submit or assign, for the teacher finishing a grade or comment, for homework/test progress that has not been submitted, or for adding or editing homework on a unit the student already has.
- **FR-016**: Events that already happened before this feature is available MUST NOT appear as unseen icons.

### Key Entities

- **Unseen notification**: One piece of news for one recipient (the teacher, or one student). It points at one homework submission, one test attempt, one newly assigned unit, or one newly assigned test. For the teacher it stays unseen until she opens that student’s submitted work (from Tareas, Pruebas de evaluación, or the student profile), or the target is removed. For the student it stays unseen until they open the assigned unit or the assigned test (without needing to start or submit), or it is unassigned/removed.
- **Homework submission**: A student turning in a homework. Notifies the teacher; surfaces under Tareas. The seen target is that student’s submitted work, not the homework editor.
- **Test submission**: A student turning in a prueba de evaluación. Notifies the teacher; surfaces under Pruebas de evaluación.
- **Unit assignment**: The teacher giving a student a unit they did not already have. Notifies that student; surfaces under Mi aprendizaje.
- **Test assignment**: The teacher giving a student a prueba de evaluación they did not already have. Notifies that student; surfaces under Mi aprendizaje.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After a student submits homework or a test, the teacher sees the matching Panel and tab icons on her next load or navigation of the site, in 100% of those submits, without opening Tareas or Pruebas de evaluación first to “poll” for work.
- **SC-002**: After the teacher assigns a unit or a test, the assigned student sees the Mi aprendizaje icon on their next login or load/navigation of the site, in 100% of those assignments; students who were not assigned never see that icon for that event.
- **SC-003**: In a mixed case (one new homework submit and one new test submit), 100% of teachers can tell from the tab icons which area has news before opening either tab.
- **SC-004**: After the teacher opens that student’s submitted work (from Tareas, Pruebas de evaluación, or the student profile), that submission’s indicator and any parent icon that had only that news disappear before she leaves the page (or on the immediate next view of navigation). Icons do not come back for that same submission. Opening the homework or test without opening that submission never clears it. After a student opens the assigned unit or test (without needing to start or submit), the same disappearance rule applies for that assignment. Opening only a homework inside the unit does not clear the unit assignment.
- **SC-005**: Recipients do not need email or a notification inbox to discover the news: 100% of the events in this feature are reachable from the small icons on Panel / Tareas / Pruebas de evaluación or Mi aprendizaje.
- **SC-006**: A teacher who opens Panel, Tareas/Pruebas de evaluación, or the homework/test list but not that student’s submitted work still has the icons on her next visit, in 100% of such checks. A student who opens Mi aprendizaje but not the new unit or test still has the icon on their next visit, in 100% of such checks.
- **SC-007**: When two students have unseen submissions on the same homework or test, the teacher can identify both the homework/test and each unseen student from the small indicators, without opening a submission first, in 100% of such checks. Opening one student’s work clears only that student’s indicators.

## Assumptions

- “Test” means a prueba de evaluación (the standalone assessments in Panel → Pruebas de evaluación and listed for students in Mi aprendizaje), not a class booking or a placement product name.
- “Does a homework/test” means **submits** it, not opening or saving a draft.
- “Assigns something” means assigning a **unit** or a **prueba de evaluación** to a student. Adding or editing homework on a unit the student already has does not create a notification.
- Presentation activities (assign or submit) are out of scope.
- Teacher finishing a grade, annotation, or comment does not notify the student in this version.
- These are **in-site icons only**. No email, SMS, or device push. Class reminder emails stay as they are and are not part of this feature.
- There is no separate notification centre, dropdown list, or mark-all-read control. The small icons *are* the notification.
- The small icon is a compact visual marker (for example a dot or similar attention mark) on the nav item, tab, or list item — not a full message with body text.
- The site has a single teacher (admin). All teacher-side news goes to her.
- Homework is reached by students through units in Mi aprendizaje; tests assigned to them also appear there, so students only need the Mi aprendizaje icon (they do not need a second icon on a separate tests nav item for this feature).
- No backfill of historical submits or assignments when the feature goes live.
- Icons are required after login or the next load/navigation; live pop-in with zero navigation while the teacher stares at an already-open page is not required.
- Existing authentication and roles are reused: only signed-in teacher sees Panel icons; only signed-in students see Mi aprendizaje icons.
