# Feature Specification: Differentiate Users from Students

**Feature Branch**: `012-user-student-roles`

**Created**: 2026-07-01

**Status**: Draft

**Input**: User description: "Differentiate between user and student. I want only certain users, selected by the teacher from the admin panel to be actual students. Creating an account does NOT automatically make you a student."

## Clarifications

### Session 2026-07-01

- Q: Should non-student users be able to browse public-facing content in student-only areas (e.g. view the class schedule, browse resource listings) even though they can't act on them (book, purchase/unlock, submit), or should those entire areas be fully inaccessible until student status is granted? → A: Browsing is allowed for any logged-in user; only the gated actions (booking a class, purchasing/unlocking a resource, submitting coursework) require student status. Coursework content itself (units, homework, presentations) remains restricted to students since it isn't public-facing browsing.
- Q: When the teacher grants or revokes a user's student status, should the system proactively notify that user (e.g. via email), or is it enough that they see the change reflected on their next visit? → A: Send an email notification to the user both when student status is granted and when it is revoked.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher designates a student (Priority: P1)

Paula (the teacher) reviews the people who have created an account on the site and explicitly marks a specific person as a "student" from her admin panel. Only after she does this does that person gain access to booking classes, resources, and coursework.

**Why this priority**: This is the core capability the feature exists for — without it, there is no way to separate "has an account" from "is an accepted student," which is the entire point of the request.

**Independent Test**: Can be fully tested by creating a new account, confirming it has no student-only access, having the teacher designate it as a student from the admin panel, and confirming access is granted immediately after.

**Acceptance Scenarios**:

1. **Given** a registered user with no student status, **When** the teacher designates them as a student from the admin panel, **Then** that user gains access to booking classes, resources, and coursework and receives an email notifying them of the change.
2. **Given** the teacher is browsing registered users in the admin panel, **When** she views the list, **Then** she can clearly see which users are already students and which are not.
3. **Given** a user who is already a student, **When** the teacher attempts to designate them as a student again, **Then** the system indicates they already have student status without creating a duplicate or error.

---

### User Story 2 - Non-student user experience (Priority: P2)

A newly registered user who has not yet been designated a student can still browse the class schedule and resource listings, but when they try to actually book a class, purchase/unlock a resource, or open coursework, they are clearly told that action requires student status granted by the teacher. They can still take the placement test, since that is the evaluation step that helps the teacher decide whether to accept them as a student.

**Why this priority**: Without clear boundaries and messaging, non-student users will be confused about why access is missing, generating support burden and a poor first impression.

**Independent Test**: Can be fully tested by registering a new account, confirming the schedule and resource listings are browsable, then confirming that booking, purchasing/unlocking a resource, and opening coursework are each blocked with an explanatory message, while the placement test remains fully accessible and completable.

**Acceptance Scenarios**:

1. **Given** a logged-in registered user without student status, **When** they browse the class schedule or resource listings, **Then** they can view them normally.
2. **Given** a logged-in registered user without student status, **When** they attempt to book a class, purchase/unlock a resource, or open coursework (units, homework, presentations), **Then** they are blocked and shown a message explaining that student access must be granted by the teacher.
3. **Given** a logged-in registered user without student status, **When** they navigate to the placement test, **Then** they can start and complete it normally.
4. **Given** a user has just created an account, **When** they finish registration, **Then** nothing in the product implies they are automatically a student.

---

### User Story 3 - Teacher revokes student status (Priority: P3)

Paula decides a particular student should no longer have active student access (e.g., they stopped attending) and removes their student status from the admin panel. That person's history is preserved, but they can no longer book new classes, purchase or use new resources, or access new coursework until re-designated.

**Why this priority**: Important for keeping the student roster accurate over time, but less urgent than establishing the initial grant mechanism and the non-student experience.

**Independent Test**: Can be fully tested by revoking a student's status from the admin panel and confirming they lose student-only access while their historical bookings, submissions, and purchases remain visible/intact.

**Acceptance Scenarios**:

1. **Given** a user currently designated as a student, **When** the teacher revokes their student status, **Then** the user immediately loses access to booking, resources, and coursework going forward and receives an email notifying them of the change.
2. **Given** a user whose student status was revoked, **When** they view their account, **Then** their past bookings, submitted homework, and purchased resources remain visible.
3. **Given** a user whose student status was revoked, **When** the teacher re-designates them as a student later, **Then** they regain access without any data loss.

---

### Edge Cases

- What happens when the teacher tries to designate her own (admin) account as a student? The admin role already grants full access and is unaffected by student designation either way.
- What happens when a non-student user directly opens a booking action, resource purchase/unlock, or coursework URL rather than navigating via the site menu? They must still be blocked with the same explanatory message — access control cannot be bypassed by deep-linking. Deep-linking to the schedule or resource listing pages themselves (browsing only) is allowed.
- What happens to a student's in-progress booking or resource purchase flow if their student status is revoked mid-flow? Any new student-only action must be blocked at the point of attempt, even if earlier steps of the flow had already started.
- How does the placement test connect to the decision to grant student status? The placement test and its results remain visible to the teacher (already surfaced in the admin panel) so she can use them to decide whether to designate someone a student, but completing it does not automatically grant student status.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST distinguish two states for non-admin accounts: "registered user" (the default state for every account created through sign-up) and "student" (an explicitly granted state).
- **FR-002**: Creating an account MUST NOT automatically grant student status.
- **FR-003**: The system MUST allow the teacher to view registered users in the admin panel and see, for each, whether they currently have student status.
- **FR-004**: The system MUST allow the teacher to designate a specific registered user as a student from the admin panel.
- **FR-005**: The system MUST allow the teacher to revoke student status from a user previously designated as a student.
- **FR-006**: The system MUST allow any logged-in registered user, regardless of student status, to browse the class schedule and resource listings.
- **FR-007**: The system MUST restrict the actions of booking a class, purchasing/unlocking a resource, and accessing coursework content (units, homework, presentations) to users who currently have student status.
- **FR-008**: The system MUST allow any logged-in registered user, regardless of student status, to access and complete the placement test.
- **FR-009**: The system MUST show a clear, non-technical message to a non-student user whenever they attempt a student-only action, explaining that student access is granted by the teacher.
- **FR-010**: The system MUST treat all accounts that existed before this change ships as already having student status, so their access is not interrupted.
- **FR-011**: The system MUST default every account created after this change ships to non-student status until the teacher grants it.
- **FR-012**: Revoking a user's student status MUST NOT delete or hide their historical bookings, submitted homework, or purchased resources.
- **FR-013**: The teacher's own admin account MUST retain full access regardless of student status, since the existing admin role already supersedes it.
- **FR-014**: The system MUST send the user an email notification whenever the teacher grants them student status, and another when the teacher revokes it.

### Key Entities

- **Registered User**: Any account created through sign-up. Has authentication credentials and profile information. May or may not currently have student status.
- **Student Designation**: The explicit grant, made by the teacher, that elevates a registered user to student status. Can be granted or revoked independently of the account itself.
- **Placement Test Result**: A record of a user's placement test attempt/outcome. Exists independently of student status and remains visible to the teacher regardless of whether the user is later designated a student.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The teacher can designate any registered user as a student, or revoke that status, in under 10 seconds from the admin panel.
- **SC-002**: 100% of accounts created after this change ships start without student status until the teacher explicitly grants it.
- **SC-003**: 100% of attempts by non-student users to book a class, purchase/unlock a resource, or open coursework are blocked with an explanatory message — no silent failures or confusing errors — while browsing the schedule and resource listings remains unaffected.
- **SC-004**: 100% of accounts that existed before this change ships retain uninterrupted access to booking, resources, and coursework after the change goes live.
- **SC-005**: A student's historical activity (bookings, submissions, purchases) remains fully visible after their student status is revoked, with 0% data loss.
- **SC-006**: 100% of student status grants and revocations trigger an email notification to the affected user.

## Assumptions

- The teacher can both grant and revoke student status, since an accurate, current student roster requires removing people as well as adding them.
- The existing admin role is unaffected by this change — an admin account's access does not depend on student status.
- The placement test remains open to any logged-in registered user because it is the evaluation step the teacher uses to help decide whether to accept someone as a student; it is intentionally excluded from the student-only restriction.
- Revoking student status only blocks future student-only actions; it does not remove or hide a user's past activity.
- All accounts that exist at the time this feature ships are grandfathered in as students, so no current student loses access; only accounts created afterward start without student status.
- The one-time grandfathering of existing accounts at launch does not itself trigger the grant/revoke email notification (FR-014) — that notification applies only to the teacher's subsequent, deliberate grant/revoke actions from the admin panel.
