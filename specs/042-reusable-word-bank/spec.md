# Feature Specification: Reusable Word Bank

**Feature Branch**: `042-reusable-word-bank`

**Created**: 2026-08-15

**Status**: Draft

**Input**: User description: "In homeworks of type Arrastrar y soltar allow the same word in multiple places. On the admin panel, this is already possible. The change is mostly for student view. Detect if this is the case, if the exercise has one at least one word going in multiple places. If that is not the case, keep exactly as it is currently (drag-and-drop, disable the word in the word bank). If at least one word goes in multiple places, change the UI to no longer disable words once they are dragged and dropped."

## Clarifications

### Session 2026-08-15

- Q: Extra student instruction on reusable questions? → A: No extra text — only keep bank words available after placement
- Q: Which student take surfaces? → A: Every student take of Arrastrar y soltar, including quizzes when they include this kind

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Place the same bank word in several blanks (Priority: P1)

A student opens an Arrastrar y soltar (drag-and-drop word-bank) question — in homework, a presentation activity, or a quiz that includes this kind — whose answer key uses the same bank word as correct in more than one blank — for example two gaps that both accept “el”, or “___ y ___” where the same item is valid in either order. The word bank keeps every word available after it is placed, so the student can put that word into a second (or further) blank without taking it back from the first. The take view does not add a new hint or change the existing drag-and-drop instructions; the only visible difference is that placed words stay available in the bank.

**Why this priority**: The teacher can already mark the same bank word as correct for several blanks. Students currently cannot complete those questions at full credit because each bank chip can occupy only one blank. This is the core gap.

**Independent Test**: Open a word-bank question where one bank item is the correct answer for at least two blanks; place that same word into both blanks (drag or click); submit; both blanks grade according to the existing per-blank answer key.

**Acceptance Scenarios**:

1. **Given** a word-bank question whose answer key designates the same bank item as correct for two or more blanks, **When** the student opens the take view, **Then** bank words remain available after they are placed (not greyed out, not struck through, still selectable and draggable), and no extra instruction about reuse is shown.
2. **Given** that reusable-bank question, **When** the student places word A into blank 1 and then places word A into blank 2, **Then** both blanks show word A at the same time (placing into the second blank does not empty the first).
3. **Given** that reusable-bank question, **When** the student uses the click-to-select then click-blank alternative, **Then** the same reuse behaviour applies (the chip stays in the bank and can be placed again).
4. **Given** that reusable-bank question, **When** the student places a word into a blank that already has a different word, **Then** the new word replaces the previous one in that blank only; other blanks are unchanged.
5. **Given** that reusable-bank question, **When** the student clicks a filled blank with no bank word selected, **Then** that blank clears (same as today) and the bank is unaffected.

---

### User Story 2 - Exclusive word-bank questions stay unchanged (Priority: P1)

A student opens an Arrastrar y soltar question where no bank word is correct for more than one blank (today’s usual 1:1 or distractor drills). Behaviour stays exactly as it is now: placing a word into a blank removes it from the bank (disabled / struck through), and placing that word into another blank moves it (the previous blank empties).

**Why this priority**: Most existing word-bank homework must not change. The user required exclusive questions to keep the current disable-on-place interaction.

**Independent Test**: Open a word-bank question where each bank item is correct for at most one blank; place a word; confirm it disables in the bank; place it into a second blank and confirm it moved rather than copied.

**Acceptance Scenarios**:

1. **Given** a word-bank question where no bank item is designated correct for more than one blank, **When** the student places a word into a blank, **Then** that word is disabled in the bank (cannot be placed again until it is returned).
2. **Given** that exclusive question with word A already in blank 1, **When** the student places word A into blank 2, **Then** blank 2 shows A and blank 1 is empty (move, not copy).
3. **Given** that exclusive question, **When** the student clears a filled blank, **Then** that word becomes available in the bank again.
4. **Given** a previously authored exclusive word-bank homework, **When** a student takes it after this feature ships, **Then** take behaviour and scores for the same placements match today.

---

### User Story 3 - Grading and review accept the same word in several blanks (Priority: P2)

When a reusable-bank question is submitted with the same bank word in more than one blank, each blank is still graded on its own against the teacher’s answer key. Results and teacher review show whatever the student placed in each blank, including the same word appearing more than once.

**Why this priority**: Reuse in the take view is only useful if submit, scoring, and feedback allow the duplicated placement. Authoring already supports the answer key; this story closes the student-to-score loop.

**Independent Test**: Submit a reusable-bank question with the shared correct word in every blank that accepts it; confirm those blanks score correct and the result view shows that word in each of those blanks.

**Acceptance Scenarios**:

1. **Given** blanks 1 and 2 both accept bank item A, **When** the student submits A in both blanks, **Then** both blanks are graded correct.
2. **Given** blanks 1 and 2 both accept A, **When** the student submits A in blank 1 and a wrong item in blank 2, **Then** blank 1 is correct and blank 2 is incorrect (per-blank scoring unchanged).
3. **Given** a reusable-bank submission, **When** the student or teacher views results/review, **Then** each blank shows the word the student placed there, even when several blanks show the same word.
4. **Given** an exclusive word-bank submission (no shared correct item), **When** it is graded, **Then** scoring rules are unchanged from today.

---

### Edge Cases

- **Detection is by bank item, not wording**: Two different bank chips that happen to show the same text are still two items. Reuse mode turns on only when the **same** bank item is marked correct for two or more blanks.
- **Question-level mode**: If at least one bank item is correct for two or more blanks, **every** bank word on that question stays available after placement (not only the shared item). Distractors stay available too.
- **Exclusive remains the default**: If every bank item is correct for at most one blank (including questions with distractors or several accepted items **per** blank that are not shared across blanks), the take view stays exclusive.
- **Answer key stays hidden**: Before submit, the student must not be shown which words are correct for which blanks. The take view still uses the correct placement mode for that question.
- **Empty blanks**: Still allowed while answering; an empty blank still scores incorrect.
- **Partial reuse in the key**: Blank 1 accepts only A; blanks 2 and 3 both accept A. The question is reusable. The student may place A in all three blanks; grading still follows each blank’s key.
- **No extra copy**: Reusable mode does not add a new student hint and does not change the shared drag-and-drop instructions used on exclusive questions. The only take-view difference is that bank words stay available after placement.
- **Replace vs copy**: In reusable mode, placing into an occupied blank replaces that blank only. In exclusive mode, placing the same chip elsewhere still moves it.
- **All student takes**: Homework, presentation activities, and quizzes that include Arrastrar y soltar all follow the same exclusive vs reusable rules. Surfaces that do not present this kind are unchanged.
- **Authoring unchanged**: The teacher panel already allows the same bank item on several blanks; this feature does not change authoring, bank size limits, or how many correct items a blank may have.
- **Other question kinds**: Typed fill-in-the-gaps, matching, choice, true/false, and table fill are out of scope.
- **Existing submissions**: Already stored answers and scores are not re-graded.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: For each Arrastrar y soltar (word-bank) question, the system MUST detect reusable-bank mode when at least one bank item is designated correct for two or more blanks, and exclusive mode otherwise.
- **FR-002**: In exclusive mode, the student take view MUST keep today’s behaviour: each bank item occupies at most one blank at a time; a placed item is disabled in the bank until it is returned; placing that item into another blank moves it.
- **FR-003**: In reusable-bank mode, bank items MUST remain available after placement (not disabled). The student MUST be able to place the same bank item into more than one blank at the same time via drag-and-drop and via the existing click-to-place alternative. The take view MUST NOT add new instructional text about reuse and MUST NOT change the existing drag-and-drop instructions used for exclusive questions.
- **FR-004**: In reusable-bank mode, placing a bank item into a blank MUST NOT remove that item from other blanks. Placing into an already-filled blank MUST replace only that blank’s contents.
- **FR-005**: Clearing a blank MUST continue to empty that blank only. In exclusive mode the item returns to the bank; in reusable-bank mode the bank is already available and stays so.
- **FR-006**: Grading MUST continue to score each blank independently against that blank’s designated correct bank items, including when the same item is placed in several blanks. Submit MUST accept those placements.
- **FR-007**: Results and teacher review MUST display the student’s placed word for each blank, including repeated words across blanks.
- **FR-008**: The student MUST NOT see the answer key (which items are correct for which blanks) before submit. Placement mode (exclusive vs reusable) MUST still be applied correctly for that question.
- **FR-009**: Existing exclusive word-bank questions MUST keep the same take behaviour and the same per-blank outcomes for the same placements, without requiring the teacher to re-author them.
- **FR-010**: Every student take of an Arrastrar y soltar question MUST follow FR-001 through FR-009, including homework, presentation activities, and quizzes when they include this kind. Authoring, typed gaps, matching, choice, true/false, and table fill MUST NOT change.

### Key Entities

- **Word-bank question**: A passage with blanks plus a bank of placeable words; each blank has a set of correct bank items (already authored today).
- **Bank item**: One placeable word or phrase with a stable identity; the label is what the student sees.
- **Placement mode**: Exclusive (one occupancy per bank item) or reusable (the same item may occupy several blanks). Derived from whether any bank item appears in more than one blank’s correct set.
- **Placement**: The bank item occupying a given blank while the student answers; empty is allowed and scores incorrect.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In verification trials, 100% of word-bank questions where the same bank item is correct for two or more blanks use reusable-bank take behaviour (chips stay available; the same word can sit in several blanks), on every student take surface that presents this kind.
- **SC-002**: In verification trials, 100% of word-bank questions where no bank item is correct for more than one blank keep today’s exclusive take behaviour (disable on place; move rather than copy).
- **SC-003**: A student can complete a reusable-bank question that requires the same word in two blanks and submit a fully correct answer on the first attempt without returning a chip to the bank, in under 1 minute for a short (≤5 blank) passage.
- **SC-004**: 100% of exclusive questions from a representative sample of existing homework produce the same per-blank correct/incorrect outcomes for the same placements as before this feature.
- **SC-005**: Teachers can assign an already-authored reusable-key word-bank homework and a student can score full credit on the shared blanks without a manual grade override.

## Assumptions

- “Arrastrar y soltar” means the existing drag-and-drop word-bank question kind on every student take surface that already presents it (homework, presentation activities, and quizzes).
- Detection uses the teacher’s answer key: the **same** bank item marked correct for two or more blanks. Identical wording on two different bank chips does not by itself turn on reusable mode.
- Mode is per question, not per word: one shared correct item switches the whole bank to stay available after placement.
- Authoring already allows the same bank item on several blanks; this feature does not add a teacher-facing toggle.
- Per-blank “any of the designated correct items” scoring stays as today; reuse does not require the student to use a word only in “its” blanks — they may still place any bank word in any blank.
- Answer keys remain hidden until after grading; only placement behaviour changes for the student.
- Click-to-clear a filled blank, replace-on-drop into an occupied blank, shuffled bank order, and empty-blank-is-incorrect stay as today except where reusable mode requires copy-instead-of-move.
- Reusable mode is a behaviour change only (chips stay available; same word may occupy several blanks). No new student-facing sentence is added.
- Other question kinds remain out of scope. This feature does not add Arrastrar y soltar to a surface that does not already have it.
- Existing stored submissions are not re-graded.
