# Feature Specification: Student Dots for Homework Corrections and Feedback

**Feature Branch**: `047-homework-grade-dots`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "I want the notification dot to appear for students when the teacher corrects a homework or leaves a feedback to a homework"

## Clarifications

### Session 2026-08-16

- Q: Should a student also get the notification dot when the teacher corrects or leaves feedback on a presentation activity? → A: Homework only — presentation activities stay out of scope
- Q: When the teacher leaves feedback, which teacher marks create the student notification dot? → A: Written comments and annotations — any student-visible mark on the answers also creates a dot
- Q: If the teacher saves a written comment or annotations while the homework is still awaiting her scoring, when should the student get the dot? → A: Only when the student can see the marks — wait until finalize for awaiting homework; notify immediately on already-graded or auto-scored homework
- Q: For a homework correction or feedback, what should clear the student notification dot? → A: Opening that homework’s result — opening Mi aprendizaje or the unit is not enough

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Student sees that the teacher has corrected their homework (Priority: P1)

A student has turned in homework that still needs the teacher’s scoring (Writing, all free-text, or mixed work with free-text answers). The teacher finishes correcting it so the student can see the teacher’s percentages. The next time that student logs in or opens the site, they see a small notification icon on **Mi aprendizaje**. Inside that area, the unit that contains the homework and the homework itself also show a small indicator. Opening **that homework’s result** (where the teacher’s scoring is shown) clears those indicators for this news. Opening Mi aprendizaje or the unit without opening that homework is not enough.

**Why this priority**: This is the main gap: students already get a dot when work is assigned, and the teacher already gets a dot when work is turned in, but students currently have no in-site cue that a correction is ready. Without it they only discover grades by remembering to reopen old homework.

**Independent Test**: As a student, submit homework that needs teacher scoring. As the teacher, finalize the correction. As the student, open or navigate the site and confirm a small icon on Mi aprendizaje, on the unit, and on that homework; open the homework result and confirm those icons for this news disappear.

**Acceptance Scenarios**:

1. **Given** the teacher has just finalized a correction on a student’s submitted homework, **When** that student next logs in or loads or navigates the site, **Then** they see a small notification icon on **Mi aprendizaje**.
2. **Given** that correction is unseen, **When** the student opens Mi aprendizaje, **Then** the unit that contains the homework and the homework itself show a small indicator.
3. **Given** the student opens that homework’s result (where they can see the teacher’s scoring), **When** they look at navigation, **Then** that homework’s indicator is gone; the unit and **Mi aprendizaje** still show an icon if other unseen news remains, and show none if that was the last one.
4. **Given** the student opens Mi aprendizaje or the unit but does not open that homework’s result, **When** they leave and return, **Then** the icons for this correction remain.
5. **Given** two of the student’s homeworks were corrected, **When** they open only one result, **Then** the other homework (and its unit, and Mi aprendizaje if needed) still shows an indicator.
6. **Given** the teacher has saved scoring progress, comments, or annotations but has not finalized, **When** the student loads the site, **Then** they do not get a notification icon for that homework yet (they still cannot see the teacher’s percentages or those marks).
7. **Given** a homework that was scored automatically when the student submitted it, and the teacher has not left feedback, **When** the student loads the site, **Then** they do not get a new notification icon from that automatic scoring.

---

### User Story 2 - Student sees that the teacher has left feedback on their homework (Priority: P1)

The teacher leaves a student-visible mark on homework the student can already see — a written comment (short note or exercise feedback text) **or** annotations on the student’s answers — and saves it. Saving annotations alone is enough; a written comment is not required. That student sees the same kind of small notification icon on **Mi aprendizaje**, on the unit, and on that homework. Opening that homework’s result — where the feedback is shown — clears the icons for this news.

This also covers homework that was already scored automatically: the student already knows they submitted it, but they do not know the teacher later left a comment or annotations unless a dot appears. Marks saved while the homework is still awaiting the teacher do not notify yet; they become news when she finalizes (together with the correction).

**Why this priority**: Feedback only helps if the student knows it exists. Today a homework can already show that feedback is present once they look, but there is no unseen cue to go look.

**Independent Test**: As the teacher, save written feedback on one homework and annotations only on another (including an already auto-scored exercise). As that student, confirm icons on Mi aprendizaje, the unit, and each homework; open each result and confirm they clear for this news.

**Acceptance Scenarios**:

1. **Given** the teacher has just saved a written comment the student can already see on a submitted homework (already graded or auto-scored), **When** that student next logs in or loads or navigates the site, **Then** they see a small notification icon on **Mi aprendizaje**, and the unit and that homework show indicators.
2. **Given** the teacher has just saved annotations on the student’s answers with no written comment, on homework the student can already see, **When** that student next loads or navigates the site, **Then** they see the same unseen icons for that homework.
3. **Given** the homework was already auto-scored on submit and the student may already have opened the result once, **When** the teacher later saves a written comment or annotations, **Then** the student gets a new unseen icon for that homework even if they had already looked at the automatic score.
4. **Given** the homework is still awaiting the teacher, **When** she saves a written comment or annotations without finalizing, **Then** the student does not get a notification icon yet.
5. **Given** the student opens that homework’s result, **When** they look at navigation, **Then** the icons for this feedback are gone (parent icons remain only if other unseen news remains).
6. **Given** the student opens Mi aprendizaje or the unit but does not open that homework’s result, **When** they return later, **Then** the icons for this feedback remain.
7. **Given** the teacher saves a correction and feedback (comment and/or annotations) in the same action, **When** the student loads the site, **Then** they see one set of icons for that homework, not two stacked markers for the same item.
8. **Given** the teacher clears all written comments and annotations so none remain, and there is no new correction for the student to see, **When** the student has not yet opened the item, **Then** that unseen notification is removed (there is nothing new to read).

---

### User Story 3 - Later teacher changes become news again (Priority: P2)

After the student has already opened the result and the icons are gone, the teacher changes the score or saves new or updated student-visible marks (written comment or annotations). That homework becomes unseen again: the student gets the Mi aprendizaje / unit / homework icons until they open the result once more.

**Why this priority**: Students should not miss a revised mark or an extra comment, but first-time “your work was reviewed” is the core value.

**Independent Test**: Finalize a homework, open it as the student so icons clear, then as teacher change the percentage or save new comments or annotations; as the student, confirm the icons return and clear again after reopening the result.

**Acceptance Scenarios**:

1. **Given** the student has already opened the corrected result (icons gone), **When** the teacher later changes a visible score and saves, **Then** that student sees the unseen icons again until they reopen the result.
2. **Given** the student has already opened the result, **When** the teacher later saves a new or updated written comment **or** annotations, **Then** that student sees the unseen icons again until they reopen the result.
3. **Given** the student still has an unseen icon for that homework, **When** the teacher saves additional feedback or adjusts the score before the student opens it, **Then** they still see a single unseen indicator for that homework (not a second copy).

---

### User Story 4 - Icons stay until the student actually opens the result (Priority: P2)

These are the same kind of in-site dots already used for assigned work: not email, not a separate inbox. Visiting **Mi aprendizaje** or the unit is enough to *see* which homework has news; it is not enough to clear it. Unlike a newly assigned unit (which becomes seen when they open the unit), a correction or feedback becomes seen only when they open **that homework’s result**. If they leave without opening it, the icons are still there next time.

**Why this priority**: Persistence is what makes the dots trustworthy, matching how teacher submission dots already work.

**Independent Test**: Create an unseen correction; as the student, open Mi aprendizaje and the unit but not the homework result — icons remain. Open the result — that homework’s icons go away.

**Acceptance Scenarios**:

1. **Given** unseen teacher review news exists, **When** the student opens Mi aprendizaje or the unit but not that homework’s result, **Then** the icons remain.
2. **Given** the student has opened that homework’s result, **When** they next load the site, **Then** that homework’s icons are gone and do not come back until the teacher makes a new correction or feedback the student can see.
3. **Given** the student is already signed in on another page when the teacher saves a correction or feedback, **When** they next load or navigate the site, **Then** they see the icons without needing to log out and back in.
4. **Given** the student still has an unseen newly assigned unit from before, **When** a homework in that unit is later corrected, **Then** both kinds of news can show at once: opening the unit still clears only the assignment news; opening the homework result clears only the correction/feedback news.

---

### Edge Cases

- **Partial teacher save**: Saving scores, comments, or annotations while the homework is still awaiting the teacher does not notify the student. Those marks become news when she finalizes (one indicator covering the now-visible correction and any comments/annotations saved with it).
- **Automatic score on submit**: Auto-scored homework does not notify the student when they submit. The teacher’s later written comment, annotations, or a later score change they can see does (the student can already see that result).
- **Same save, grade plus comment or annotations**: One unseen indicator per homework, not two.
- **Annotations only**: Saving annotations with no written comment still creates the same unseen indicator as a written comment.
- **Already-unseen homework, extra teacher save**: Still one unseen indicator for that homework.
- **Student opened the result while still awaiting teacher**: That visit does not consume a future correction notification. When the teacher later finalizes, the student gets a new unseen icon.
- **Feedback removed**: If the teacher deletes all written comments and annotations and there is no new visible correction to discover, any still-unseen notification for that feedback goes away.
- **Homework or unit removed / student unassigned**: Related unseen student icons for that homework go away.
- **Another student’s homework**: Only the student whose work was reviewed sees the icons. Classmates and signed-out visitors do not.
- **Teacher’s own action**: The teacher does not get a new student-style icon for having graded or commented; existing teacher dots for *submissions* are unchanged.
- **Tests**: Teacher scoring or comments on pruebas de evaluación are out of scope.
- **Presentation activities**: Teacher scoring or comments on presentation activities are out of scope for this feature (homework only).
- **Past reviews from before this ships**: Existing grades and comments are not backfilled as unseen; only teacher actions after the feature is live create icons.
- **Student already looking at the result**: Icons and newly visible comments appear on their next load or navigation; they are not required to pop in the same instant with no page change.
- **Persistent “has feedback” mark**: Any existing mark that only means “feedback exists” (not unread) can stay; this feature adds the unseen notification dot, which clears when the student opens the result.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST create an unseen student notification when the teacher finalizes a homework correction so that student can see the teacher’s scoring.
- **FR-002**: The system MUST create an unseen student notification when the teacher saves any student-visible mark on homework the student can already see (already graded or auto-scored): a written comment (short note or exercise feedback text) **or** annotations on the student’s answers. Saving annotations alone MUST be sufficient.
- **FR-003**: Saving teacher scoring progress, comments, or annotations without finalizing MUST NOT create a student notification while the homework is still awaiting the teacher. Those marks MUST become news when she finalizes (see FR-001 and FR-009).
- **FR-004**: Automatic scoring that happens when the student submits MUST NOT create a student notification. A later written comment, annotations, or a later teacher score change the student can see MUST still create one.
- **FR-005**: Student notifications from this feature MUST be visible as a small icon on the **Mi aprendizaje** navigation item whenever that student has any unseen homework correction or feedback.
- **FR-006**: The unit that contains the homework and the homework itself MUST each show a small indicator until the student opens that homework’s result. **Mi aprendizaje** MUST remain while that student still has any other unseen news of this kind (or existing unseen assigned-work news).
- **FR-007**: Opening that homework’s result (the view where the teacher’s scoring and/or feedback is shown) MUST mark that homework’s correction/feedback notification as seen. Opening it from the unit page or from the homework’s own page MUST both count.
- **FR-008**: Merely opening Mi aprendizaje or the unit, without opening that homework’s result, MUST NOT mark the correction/feedback notification as seen. This differs from assignment notifications, which become seen when the student opens the unit.
- **FR-009**: If the teacher finalizes a correction and saves a written comment and/or annotations in the same action, the system MUST show a single unseen indicator for that homework, not two.
- **FR-010**: If that homework already has an unseen correction/feedback notification, further teacher saves on the same homework MUST keep a single unseen indicator until the student opens the result.
- **FR-011**: After the student has opened the result, a later teacher save that changes a visible score, adds or updates a written comment, or adds or updates annotations MUST create a new unseen notification for that homework.
- **FR-012**: Recipients MUST see their icons when they log in and when they next load or navigate the site while already signed in. They MUST NOT need a separate notification list, email, or device push for this feature.
- **FR-013**: Only the student whose homework was reviewed MUST see these icons. Other students, signed-out visitors, and the teacher MUST NOT see another person’s correction/feedback news. The teacher MUST NOT receive a new icon for their own grading or commenting.
- **FR-014**: Unassigning the student from the unit, or deleting the homework, MUST remove any still-unseen correction/feedback notification that pointed at it.
- **FR-015**: If the teacher removes all student-visible written comments and annotations and there is no new visible correction to discover, the system MUST remove any still-unseen notification that existed only for that feedback.
- **FR-016**: Events that already happened before this feature is available MUST NOT appear as unseen icons.
- **FR-017**: Teacher scoring or comments on pruebas de evaluación and on presentation activities MUST NOT create these student notifications.
- **FR-018**: These notifications MUST be independent of assignment notifications: opening a unit MUST still only clear “newly assigned unit” news; opening a homework result MUST only clear that homework’s correction/feedback news.

### Key Entities

- **Unseen correction/feedback notification**: One piece of news for one student, pointing at one homework. It is created when the teacher finalizes a visible correction, or when she saves a written comment or annotations on homework the student can already see. It stays unseen until that student opens that homework’s result, or the target is removed / all comments and annotations are fully cleared with nothing left to discover.
- **Homework correction**: The teacher finishing scoring so the student can see teacher percentages (and overall result) on a submitted homework.
- **Homework feedback**: Any student-visible teacher mark on that submission — a written comment (short note or exercise feedback text) **or** annotations on the student’s answers. Either kind is enough to create the notification.
- **Homework result**: The student-facing view of that submitted homework after review, where scores and/or teacher comments are shown.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After the teacher finalizes a homework correction, that student sees the Mi aprendizaje icon (and the unit/homework indicators) on their next login or load/navigation of the site, in 100% of those finalizations.
- **SC-002**: After the teacher saves a student-visible written comment or annotations on a homework, that student sees the same icons on their next login or load/navigation, in 100% of those saves — including on homework that was already auto-scored, and including annotation-only saves.
- **SC-003**: After the student opens that homework’s result, that homework’s indicator and any parent icon that had only that news disappear before they leave the page (or on the immediate next view of navigation). Icons do not come back for that same teacher action.
- **SC-004**: A student who opens Mi aprendizaje or the unit but not the homework result still has the icons on their next visit, in 100% of such checks.
- **SC-005**: Partial teacher scoring, comments, or annotations that are not yet visible to the student (homework still awaiting the teacher) never produce a student icon, in 100% of such saves. Automatic scoring on submit never produces a student icon, in 100% of such submits.
- **SC-006**: Only the student whose homework was reviewed sees the icons; other students never see that news, in 100% of checks.
- **SC-007**: Recipients do not need email or a notification inbox to discover the news: 100% of corrections and feedback in this feature are reachable from the small icons on Mi aprendizaje, the unit, and the homework.

## Assumptions

- This extends the existing in-site notification dots (assigned work for students; submissions for the teacher). It does not add email, SMS, device push, or a notification centre.
- “Corrects a homework” means the teacher **finalizes** scoring so the student can see teacher percentages. It does not mean saving unfinished scoring, and it does not mean automatic scoring at submit time.
- “Leaves a feedback” means the teacher saves any student-visible mark on homework the student can already see: a written comment (short note or exercise feedback text) **or** annotations on the answers. Annotations alone count. Marks saved while the homework is still awaiting the teacher wait until finalize. A persistent “this homework has feedback” mark, if one already exists, is not a substitute for the unseen dot.
- Homework is reached through units in Mi aprendizaje; students only need the existing **Mi aprendizaje** nav icon plus unit and homework indicators (no new top-level nav item).
- Pruebas de evaluación and presentation activities are out of scope.
- There is a single teacher (admin). She does not receive student-style icons for her own review actions.
- No backfill of historical grades or comments when the feature goes live.
- Icons are required after login or the next load/navigation; live pop-in with zero navigation on an already-open page is not required.
- Existing authentication and roles are reused: only the signed-in student whose work was reviewed sees these icons.
- The earlier notification feature explicitly deferred “teacher finished a grade or comment.” This feature is that deferred student-side event for **homework only**.
