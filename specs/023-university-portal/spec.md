# Feature Specification: University Student Portal

**Feature Branch**: `023-university-portal`

**Created**: 2026-08-01

**Status**: Draft

**Input**: User description: "Create a totally separate portal for university students. These students don't do 1-1 classes with the teacher. Instead, they do a one-year class at the university with the same teacher. This website will be used mostly for informative parts, like seeing the schedule based on their level (7 classes per week, 2 for intermediates and 5 for beginners). They can also see exam dates. Also, a section for news. And another separate part where students can see class materials and even do homeworks. Once a person creates an account, they are not automatically university-students. Similar to the existing website, the teacher has to assign the role for each account created."

## Clarifications

### Session 2026-08-01

- Q: How “totally separate” should the university portal be relative to the existing private-lesson site? → A: Separate public entry (e.g. different URL/subdomain) that shares the same accounts
- Q: How should the weekly class schedule be modeled for the academic year? → A: Recurring weekly template plus optional one-off dated exceptions (cancellations/extra sessions)
- Q: Should exam dates and news be the same for all university students, or filtered by level? → A: Both exam dates and news are shared with all university students (not filtered by level)
- Q: What homework capabilities should the university portal support in v1? → A: Same capabilities as private-student homework (manual and auto-graded exercise types); homeworks created for 1-1 classes should be visible/available in the university portal too
- Q: Should class materials in the university portal reuse private 1-1 learning content, or be university-only? → A: Shared catalog: materials/presentations created for 1-1 can be made available per university level
- Q: When someone opens the university portal URL without being logged in, what should they see? → A: Public read of informative sections (schedule/exams/news); materials and homework still require login + university status
- Q: For visitors who are not logged-in university students, how should the public schedule show beginner vs intermediate sessions? → A: Show all sessions (both levels), each clearly labeled beginner or intermediate
- Q: Should the system email the user when the teacher grants or revokes university-student status? → A: No email; status change is visible only on next visit
- Q: If someone is both private and university student, how should shared homework progress be tracked? → A: Not possible — an account cannot hold both private-student and university-student status
- Q: Can visitors create an account from the university portal URL, or only from the private-lesson site? → A: Register and log in on the university entry (same shared accounts)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher grants university-student access (Priority: P1)

Paula (the teacher) reviews people who have created accounts and explicitly designates specific accounts as university students, assigning each one a level (beginner or intermediate). Only after this designation does that person gain access to the university portal. Creating an account alone never makes someone a university student.

**Why this priority**: Without teacher-controlled access, there is no way to separate "has an account" from "is enrolled in the university class," which is the foundation of every other university-portal capability.

**Independent Test**: Register a new account, confirm materials/homework are denied, confirm schedule/exams/news are readable without university status, have the teacher grant university-student status with a level, then confirm materials and homework open for that level.

**Acceptance Scenarios**:

1. **Given** a registered user without university-student status, **When** they try to open materials or homework in the university portal, **Then** they are blocked and shown a clear message that university access must be granted by the teacher.
2. **Given** anyone (logged in or not), **When** they open schedule, exam dates, or news on the university entry, **Then** they can read those informative sections without university-student status.
3. **Given** a registered user without university-student status, **When** the teacher designates them as a university student and assigns a level (beginner or intermediate), **Then** that user can access materials and homework for their level in addition to the public informative sections.
4. **Given** the teacher is managing accounts, **When** she views users, **Then** she can see who already has university-student status, their level, and who does not.
5. **Given** a user has just finished registration, **When** they land in the product, **Then** nothing implies they are automatically a university student.
6. **Given** a user who already has private-student status, **When** the teacher attempts to grant university-student status without revoking private-student status first, **Then** the grant is rejected and the account remains a private student only.
7. **Given** a user who already has university-student status, **When** the teacher attempts to grant private-student status without revoking university-student status first, **Then** the grant is rejected and the account remains a university student only.

---

### User Story 2 - Level-based weekly schedule (Priority: P1)

Anyone can open the university entry and view the class schedule. The overall university week has seven class sessions: five for beginners and two for intermediates. Logged-in university students see only the sessions for their assigned level as their timetable. Visitors without university-student status (including anonymous visitors) see all sessions with each clearly labeled as beginner or intermediate. The base timetable is a recurring weekly template; the teacher can also publish one-off dated exceptions (cancelled sessions or extra sessions) that appear for those dates.

**Why this priority**: Checking "when do I have class?" is the primary day-to-day use of an informative university portal.

**Independent Test**: Publish a full weekly template with beginner and intermediate sessions plus at least one dated exception; confirm anonymous visitors see all sessions labeled by level, and each enrolled university student sees only their level's sessions (including applicable exceptions).

**Acceptance Scenarios**:

1. **Given** a beginner university student is logged in, **When** they open the schedule, **Then** they see the beginner class sessions from the weekly template (the five beginner sessions).
2. **Given** an intermediate university student is logged in, **When** they open the schedule, **Then** they see the intermediate class sessions from the weekly template (the two intermediate sessions).
3. **Given** a university student views the schedule, **When** no template sessions exist yet for their level, **Then** they see an empty-state message rather than another level's timetable.
4. **Given** the teacher has added a dated exception (cancellation or extra session) for a student's level, **When** that student views the schedule covering that date, **Then** they see the exception applied (session removed or added) rather than only the unmodified template.
5. **Given** a visitor is not logged in (or has no university-student status), **When** they open the schedule on the university entry, **Then** they see all sessions for both levels, each clearly labeled beginner or intermediate.

---

### User Story 3 - University student views exam dates and news (Priority: P2)

Visitors and enrolled university students can open dedicated sections to see upcoming exam dates and news posts published by the teacher (announcements, reminders, course updates). Exam dates and news are the same for everyone who can read them (not filtered by level) and do not require login or university-student status. Materials and homework remain gated separately.

**Why this priority**: Exams and news are core informative needs of a year-long university class, but students can still get value from schedule alone if these arrive slightly later.

**Independent Test**: Publish exam dates and news as the teacher, then confirm they are readable without login and that university students at both levels see the same items.

**Acceptance Scenarios**:

1. **Given** the teacher has published one or more exam dates, **When** anyone opens the exam dates section on the university entry (logged in or not), **Then** they see those dates with enough detail to know what exam and when.
2. **Given** the teacher has published news items, **When** anyone opens the news section on the university entry, **Then** they see the published items ordered with newest first (or another clear chronological order).
3. **Given** a user without university-student status, **When** they try to open materials or homework, **Then** access is denied even though they can read exams and news.

---

### User Story 4 - University student uses class materials and homework (Priority: P2)

An enrolled university student opens a learning area of the university portal (separate from the informative schedule/news/exams sections) where they can view class materials for their level and complete assigned homework. Materials and homework both reuse the private 1-1 content catalogs: the teacher makes selected materials/presentations and homeworks (including those created for 1-1 classes) available per university level. Homework supports the same capabilities as private 1-1 homework (manual submission with teacher review, and auto-graded exercise types). This area is for the university cohort, not for private 1-1 lesson booking.

**Why this priority**: Materials and homework are essential for a full academic-year experience, but the portal still delivers value as an information hub without them.

**Independent Test**: Make a 1-1 material/presentation and a 1-1 homework available for a university level (including both a manual and an auto-graded homework type if available); confirm a student at that level can view materials and complete homework, while a student at the other level does not see that level's items as theirs.

**Acceptance Scenarios**:

1. **Given** materials/presentations have been made available for a student's level (including content originally created for 1-1), **When** they open the materials section, **Then** they can view those materials.
2. **Given** homework is assigned or made visible for a student's level (including homework originally created for 1-1 classes), **When** they open homework, **Then** they can view the assignment and complete it using the same homework interaction types available to private students (manual and auto-graded as applicable).
3. **Given** a university student has submitted or completed homework, **When** they return later, **Then** they can see that their work was recorded (and any review/result status provided, if applicable).
4. **Given** materials or homework exist only for the other level, **When** a student opens the learning area, **Then** they do not see the other level's items as theirs.
5. **Given** a material/presentation or homework exists in the shared catalog used for 1-1 classes, **When** the teacher makes it available for a university level, **Then** university students at that level can see and use it in the university portal.

---

### User Story 5 - Teacher manages university portal content (Priority: P2)

Paula maintains the university portal from her admin tools: assign/revoke university-student status and level, edit the weekly schedule template and dated exceptions, publish and update exam dates and news, and make materials/presentations and homeworks (including those created for 1-1 classes) available per university level.

**Why this priority**: Students cannot use informative or learning features unless the teacher can author and update that content; this pairs with student-facing stories rather than standing alone as the first demo.

**Independent Test**: As teacher, create schedule sessions, exam dates, a news item, materials, and homework for both levels; then verify students see the correct subset.

**Acceptance Scenarios**:

1. **Given** the teacher is in the admin area, **When** she manages university schedule sessions, **Then** she can create, update, and remove recurring weekly template sessions and mark each as beginner or intermediate, and she can add, update, and remove dated exceptions (cancellations or extra sessions) for a level.
2. **Given** the teacher manages exam dates or news, **When** she publishes or updates an item, **Then** enrolled university students see the change in the portal.
3. **Given** a university student currently has access, **When** the teacher revokes university-student status, **Then** that person immediately loses materials/homework access on their next request while their account remains valid, without an email notification about the change.
4. **Given** the teacher changes a student's level, **When** that student next opens schedule, materials, or homework, **Then** they see content for the new level.

---

### User Story 6 - Separation from the private 1-1 experience (Priority: P3)

The university portal has its own public entry (separate URL/subdomain) from the private-lesson site, while sharing the same accounts. Visitors can register and log in on the university entry. Inside that entry, students see university flows only (schedule, exams, news, materials/homework) — not 1-1 booking. Private-lesson features continue on the main site for private students without requiring university enrollment. Logging in on either entry uses the same account.

**Why this priority**: Separation is a stated product goal, but once access control and core portal sections exist, enforcing clear boundaries is a cross-cutting check rather than a standalone MVP slice.

**Independent Test**: Open the university entry URL and confirm registration and login work there; confirm its navigation offers no 1-1 booking; confirm a private student without university status cannot use materials/homework; confirm the same account can sign in on both entries with role-appropriate access.

**Acceptance Scenarios**:

1. **Given** a visitor opens the university portal entry (separate URL/subdomain), **When** they browse its main sections (after login with university status), **Then** they find schedule, exam dates, news, and materials/homework — not 1-1 class booking.
2. **Given** a user who has private-student status (and therefore not university-student status), **When** they try to use materials or homework via the university entry, **Then** they are denied until university status is granted after private status is revoked (while schedule, exams, and news remain readable).
3. **Given** a user who is a university student (and therefore not a private student), **When** they try private-student-only actions on the private-lesson site (e.g. book a 1-1 class or open private coursework), **Then** existing private-student rules still apply and university status alone does not grant those actions.
4. **Given** a person has one account, **When** they sign in via the university entry or the private-lesson site, **Then** the same account is recognized on both.
5. **Given** a new visitor on the university entry, **When** they register an account there, **Then** the account is created in the shared account system without university-student status (and without implying private-student status).

---

### Edge Cases

- What happens if the teacher grants university status without assigning a level? The system must require a level at grant time; university access cannot be active without beginner or intermediate.
- What happens if a user holds both private-student and university-student status? Not allowed — the two statuses are mutually exclusive. The teacher cannot grant one while the other is active; the existing status must be revoked first.
- What happens when university status is revoked mid-year? Materials/homework access ends on the next request; historical homework submissions remain stored for the teacher's records but are no longer reachable by that user through the portal until status is restored.
- What happens if schedule, news, or exams are empty? Students see a clear empty state, not an error.
- What happens when a dated exception conflicts with the weekly template (e.g. cancel one occurrence)? The exception wins for that date; other weeks continue to follow the template.
- What happens if someone deep-links to materials or homework without university status? Access is denied with the same explanatory message as navigating via menus.
- What happens if someone deep-links to schedule, exam dates, or news without logging in? Those informative sections remain readable.
- What happens if someone opens the private-lesson site while only having university status (or vice versa)? Each entry only grants the privileges of the roles the account holds; shared login does not merge the two experiences.
- What happens when a non–university visitor views the public schedule? They see the full labeled timetable for both levels, not a personalized filtered view.
- How does the one-year class duration affect the product? The portal serves the current academic cohort's content; archiving prior years is out of scope for this feature unless the teacher simply replaces content for the new year.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a university student portal with a separate public entry (distinct URL/subdomain) from the private-lesson site, while sharing the same account system; that entry MUST present university flows only (not 1-1 booking or private learning).
- **FR-001a**: The university entry MUST allow visitors to register and log in using the shared account system; registration MUST NOT grant university-student or private-student status.
- **FR-002**: The system MUST distinguish university-student status from ordinary registered-user status; account creation MUST NOT grant university-student status.
- **FR-003**: Only the teacher (admin) MUST be able to grant and revoke university-student status for individual accounts.
- **FR-003a**: Granting or revoking university-student status MUST NOT send an email notification; the change MUST take effect on the user's next visit or request.
- **FR-004**: When granting university-student status, the teacher MUST assign exactly one level: beginner or intermediate.
- **FR-005**: The teacher MUST be able to change a university student's level after the initial grant.
- **FR-006**: Schedule, exam dates, and news on the university entry MUST be publicly readable without login and without university-student status.
- **FR-006a**: Materials and homework MUST require both login and active university-student status; users lacking university-student status MUST be blocked from those learning sections.
- **FR-007**: The university weekly schedule MUST be based on a recurring weekly template supporting seven class sessions in total, categorized as beginner (five) or intermediate (two). Logged-in university students MUST see sessions for their own level only. Visitors without university-student status MUST see all sessions with each session clearly labeled by level.
- **FR-008**: The teacher MUST be able to create, update, and remove university weekly template sessions and set each session's level.
- **FR-008a**: The teacher MUST be able to create, update, and remove dated schedule exceptions (cancellations or extra sessions) for a level; when students view a date range, exceptions MUST override the template for those specific dates.
- **FR-009**: Anyone MUST be able to view exam dates published by the teacher on the university entry; exam dates MUST be shared with all readers (not filtered by level).
- **FR-010**: The teacher MUST be able to create, update, and remove exam date entries.
- **FR-011**: Anyone MUST be able to view a news section of teacher-published announcements on the university entry; news MUST be shared with all readers (not filtered by level).
- **FR-012**: The teacher MUST be able to create, update, and remove news items; readers MUST only see published items.
- **FR-013**: The university portal MUST include a learning area, separate from the informative schedule/news/exams sections, where students can view class materials for their level.
- **FR-013a**: Materials/presentations created for 1-1 classes MUST be usable in the university portal: the teacher MUST be able to make those materials visible/available for a university level so enrolled students at that level can view them there.
- **FR-014**: University students MUST be able to view and complete homework made available for their level, with the same homework capabilities as private 1-1 students (manual submission with teacher review, and auto-graded exercise types).
- **FR-014a**: Homeworks created for 1-1 classes MUST be usable in the university portal: the teacher MUST be able to make those homeworks visible/available for a university level so enrolled students at that level can do them there.
- **FR-015**: The teacher MUST be able to manage which materials/presentations and which homeworks are available for beginner and intermediate levels independently.
- **FR-016**: University-student status and private-student status MUST be mutually exclusive: an account MUST NOT hold both at the same time. Granting one while the other is active MUST be rejected until the other is revoked. Neither status grants the privileges of the other.
- **FR-017**: Users without university-student status who attempt materials or homework MUST receive a clear explanation that university learning access requires teacher approval.
- **FR-018**: The teacher MUST be able to see which accounts have university-student status and their levels when managing users.

### Key Entities

- **University Student**: A registered account that has been explicitly granted university-student status, with an assigned level (beginner or intermediate).
- **University Level**: Beginner or intermediate; determines which schedule sessions, materials, and homework a student sees.
- **University Schedule Session**: A recurring weekly template class meeting labeled for beginner or intermediate (day/time within the week).
- **University Schedule Exception**: A one-off dated override for a level — either a cancellation of a template occurrence or an extra session on a specific date.
- **Exam Date**: A published exam event with a date/time (and label/description) publicly visible on the university entry to all readers regardless of level or login.
- **News Item**: A teacher-authored announcement publicly visible on the university entry (when published) to all readers regardless of level or login.
- **University Material Availability**: A link between an existing class material/presentation (including content created for 1-1 classes) and a university level, making that material visible in the university portal for students at that level.
- **University Homework Availability**: A link between an existing homework (including homeworks created for 1-1 classes) and a university level, making that homework visible and completable in the university portal for students at that level.
- **University Homework Submission**: A university student's submitted or completed work for a homework made available to their level, including status/result when reviewed or graded.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A newly registered user without university-student status cannot access materials or homework (100% of ungated learning attempts blocked in acceptance testing), while schedule, exam dates, and news remain readable without university status.
- **SC-002**: After the teacher grants status with a level, the student can open materials/homework for their level within one minute without assistance (having registered or logged in via the university entry if needed).
- **SC-003**: In a published full week template (5 beginner + 2 intermediate sessions), beginner and intermediate university students each see only their level's sessions as their timetable; anonymous/non–university visitors see all sessions labeled by level; when a dated exception exists for a level, that level's university students see it applied for the affected date.
- **SC-004**: At least 90% of test university students can locate exam dates and the latest news item within two minutes on first use of those sections.
- **SC-005**: A university student can open a level-appropriate material (including one originally created for 1-1 use, once made available for their level) and complete a homework (likewise from the shared catalog) in under five minutes in guided acceptance testing.
- **SC-006**: Testers reach the university portal via its own public entry (separate URL/subdomain) and do not confuse it with 1-1 booking: university portal primary navigation contains no 1-1 booking entry, verified in walkthrough review.
- **SC-007**: Granting or revoking university status (or changing level) is reflected for the affected user on their next portal visit without requiring a new account.

## Assumptions

- The university portal serves Paula's university cohort for a one-year class; it is not a multi-institution marketplace.
- Authentication and account creation reuse the existing account system; university access is an additional teacher-granted status (parallel to how private-student status works today), not a separate sign-up product. Visitors can register and log in on the university entry as well as on the private-lesson site.
- "Totally separate portal" means a separate public entry (URL/subdomain) with its own student-facing experience and navigation for university content, while still sharing login/accounts with the private-lesson site.
- Levels are exactly two: beginner and intermediate. The weekly pattern is five beginner sessions and two intermediate sessions (seven total); the teacher may temporarily publish fewer template sessions, but the product is designed around that structure. Schedule presentation is template-plus-exceptions: a recurring weekly template with optional one-off dated cancellations or extra sessions.
- Schedule, exam dates, news, materials, and homework are authored only by the teacher (admin), not by students. Exam dates and news are cohort-wide (not filtered by level) and publicly readable on the university entry. Schedule is publicly readable: university students see only their level; other visitors see all sessions clearly labeled by level. Materials and homework remain level-specific and require login plus university-student status.
- University materials and homework both reuse the private 1-1 content catalogs: the teacher makes selected materials/presentations and homeworks (including those created for 1-1) available per university level rather than maintaining wholly separate authoring silos. Availability is explicit per level (not an automatic dump of the entire private catalog).
- Private 1-1 booking, Zoom meetings, placement test, and existing private-student learning remain unchanged except for clear separation, independent role checks, and shared use of the materials and homework catalogs where university availability is configured.
- A person may hold private-student status, university-student status, or neither — never both at once. The teacher must revoke one before granting the other.
- Multi-year archives, student-to-student discussion forums, grades transcripts, and university enrollment payments are out of scope.
- Email notification on university status grant/revoke is intentionally not sent; the user discovers the change on their next visit (unlike private-student grant/revoke email practice).
