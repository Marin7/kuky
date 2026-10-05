# Feature Specification: Spot the Wrong Words (new homework question type)

**Feature Branch**: `054-spot-wrong-words`

**Created**: 2026-10-05

**Status**: Draft

**Input**: User description: "Create a new type of homeworks. The teacher will provide a text in which multiple words are wrong. The student will have to select the wrong words by clicking on them."

## Clarifications

### Session 2026-10-05

- Q: How should a spot-the-wrong-words question be scored? → A: Found ÷ total errors; wrongly marked correct words carry no penalty.
- Q: Can a student mark more words than there are errors? → A: No — marked words are capped at the number of errors; at the cap, the student must unmark a word before marking another.
- Q: How does the teacher mark which words are wrong? → A: Type/paste the text, then click words in a clickable view of it to mark them as errors; each marked word gets an optional correction field. No inline markup syntax.
- Q: What happens to marked errors when the teacher edits the text afterwards? → A: Marks (and corrections) stay on words that still exist unchanged; marks on deleted or changed words are dropped.
- Q: Where is the question type available in v1? → A: Homework, presentation activities and quizzes — same as the other auto-graded types.
- Q: How much does one spot-the-wrong-words question weigh in the overall homework %? → A: One question with a fractional score (like fill-in-the-blanks/matching); fully correct only at 100%. Errors are not expanded into separate questions.
- Q: What counts as one clickable word? → A: A run of letters/digits; internal hyphens or apostrophes keep it one word (*bien-estar*, *l'hotel*); numbers are clickable; surrounding punctuation is not.
- Q: How is the graded result shown? → A: Inline in the full text — found errors marked as found with their correction next to them, missed errors marked as missed with their correction, and wrongly marked correct words in a third distinct style with a "marcada por error" hint. No separate list.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Student finds the wrong words in a text (Priority: P1)

A student opens an assigned homework containing a "spot the wrong words" question. They see a Spanish text in which some words are wrong (e.g. wrong conjugation, wrong gender agreement, misspelling) and a hint telling them how many errors to find. They click words to mark them as wrong (clicking again unmarks them), then submit the homework. The question is graded automatically and the student sees, for each wrong word, whether they found it, which words they marked by mistake, and — where the teacher provided one — the correct form.

**Why this priority**: This is the core learning interaction; without it the question type delivers no value.

**Independent Test**: Seed a homework with one spot-the-wrong-words question (text with 3 marked errors), assign it to a student, mark 2 of the errors plus 1 correct word, submit, and verify the result screen and score.

**Acceptance Scenarios**:

1. **Given** an assigned, unsubmitted homework with a spot-the-wrong-words question, **When** the student opens it, **Then** they see the full text with every word individually clickable, the number of errors to find, and no indication of which words are wrong.
2. **Given** the student is answering, **When** they click an unmarked word, **Then** it becomes visibly marked; **When** they click a marked word, **Then** it becomes unmarked.
3. **Given** the student has already marked as many words as there are errors, **When** they click another unmarked word, **Then** it is not marked and they are told to unmark a word first.
4. **Given** the student marked some words, **When** they submit, **Then** the question is graded automatically with no teacher action needed, and a homework consisting only of auto-graded questions becomes graded immediately.
5. **Given** a graded submission, **When** the student views the result, **Then** within the full text each wrong word is shown as found or missed, each correct word the student marked is shown as wrongly marked, and the teacher's correction (if any) is shown next to each wrong word.
6. **Given** the student marked no words, **When** they submit, **Then** the submission is accepted and the question scores 0%.

---

### User Story 2 - Teacher authors a spot-the-wrong-words question (Priority: P1)

In the admin homework editor, the teacher adds a question of the new type. They write (or paste) the text and indicate which words are wrong, optionally giving the correct form for each one. Before saving they can preview how the student will see it. The question can sit in the same homework alongside any other question types.

**Why this priority**: Students cannot do the exercise unless the teacher can create it; equal priority with Story 1 (both needed for an MVP).

**Independent Test**: In the admin panel, create a homework with one question of the new type, mark 4 words as wrong (2 with corrections), save, reopen, and verify the text, the marked words and the corrections are preserved exactly.

**Acceptance Scenarios**:

1. **Given** the teacher is editing a homework, **When** they add a question, **Then** "spot the wrong words" is offered as a question type alongside the existing ones.
2. **Given** the teacher has typed or pasted a text, **When** they click a word in the clickable view of that text, **Then** that word is highlighted as an error and a correction field appears for it (optional to fill); **When** they click a highlighted word, **Then** it is no longer an error and its correction is discarded.
3. **Given** the teacher tries to save a question with no wrong words marked, **When** they save, **Then** saving is refused with a clear message.
4. **Given** a saved question, **When** the teacher reopens the homework, **Then** the text, the wrong words and their corrections are shown exactly as saved.
5. **Given** a student has already submitted the homework, **When** the teacher edits the question, **Then** the student's existing result is unaffected (it is judged against the version they answered).

---

### User Story 3 - Teacher reviews results and common errors (Priority: P2)

The teacher looks at a student's submission and sees the same per-word breakdown the student sees. Missed wrong words also appear in the student's "Errores frecuentes" box in the admin student profile, like wrong answers on other auto-graded question types.

**Why this priority**: Valuable for follow-up teaching, but the exercise works for students without it.

**Independent Test**: After a student submits with one missed error, open the student's admin profile and verify the submission breakdown and an entry in "Errores frecuentes".

**Acceptance Scenarios**:

1. **Given** a submitted spot-the-wrong-words answer, **When** the teacher opens the submission, **Then** they see found, missed and wrongly marked words.
2. **Given** a student missed at least one wrong word, **When** the teacher opens that student's "Errores frecuentes", **Then** the question appears as one entry and counts as one mistake, consistent with it weighing as one question in the score (FR-013).
3. **Given** a student found every error, **When** the teacher opens "Errores frecuentes", **Then** the question does not appear.

---

### Edge Cases

- **Punctuation**: punctuation attached to a word (commas, periods, ¿ ¡ ? !, quotes) is not part of the clickable word and is never itself selectable; in *¿Dónde?* only *Dónde* is clickable.
- **Hyphens, apostrophes, numbers**: *bien-estar* and *l'hotel* are each one clickable word; a leading/trailing or standalone hyphen (e.g. a dash used as punctuation) is not part of any word; *1990* is clickable (FR-006).
- **Repeated words**: the same word appearing several times in the text is judged per occurrence — only the occurrence(s) the teacher marked are wrong.
- **Accents and case**: in a saved question and in grading, words are identified by position in the text, not by spelling (FR-002b only governs carrying marks across edits in the editor), so accents, capitalisation and identical spellings never cause ambiguity.
- **Line breaks / paragraphs**: the teacher's paragraph breaks are preserved in what the student sees.
- **Over-marking**: prevented by the cap (FR-008a), so "mark everything" cannot reach 100%; wrongly marked correct words do not lower the score (FR-012) but are shown as wrongly marked in the result.
- **Very long text or many errors**: the editor enforces upper limits (see FR-004) and explains them when exceeded.
- **Small screens**: words remain individually tappable on a phone-width screen without accidentally selecting neighbours.
- **Draft answers**: like every other auto-graded question type, markings are not kept if the homework is left before submitting (only free-text answers have a device draft today).
- **Teacher edits the text after marking errors**: marks and their corrections stay on words that still exist unchanged (even if their position shifts); a mark on a word that was deleted or whose spelling changed is dropped. The teacher always sees the up-to-date set of marked words before saving (FR-002b).

## Requirements *(mandatory)*

### Functional Requirements

**Authoring (teacher)**

- **FR-001**: The teacher MUST be able to add a question of type "spot the wrong words" to a homework, mixed freely with all existing question types.
- **FR-002**: The question MUST consist of a text (multi-paragraph; it is the question's statement, as the passage is for fill-in-the-blanks) and a set of words in that text marked as wrong. The student-facing instruction ("Encuentra los N errores") is shown automatically; homework-level instructions remain available for anything more.
- **FR-002a**: The teacher MUST mark wrong words by clicking them in a clickable view of the entered text (the same word segmentation the student sees); clicking a marked word unmarks it. No markup syntax is required in the text itself.
- **FR-002b**: When the teacher edits the text after marking errors, each mark and its correction MUST be preserved on its word if that word still exists unchanged, and MUST be dropped if the word was deleted or its spelling changed.
- **FR-003**: For each wrong word the teacher MAY provide a correct form in a field shown alongside that marked word; it is shown to the student only after grading.
- **FR-004**: The system MUST require at least 1 wrong word and enforce upper limits of 30 wrong words and 3,000 characters of text per question, rejecting saves that violate them with a clear message.
- **FR-005**: The teacher MUST be able to preview the question as the student will see it before saving.
- **FR-006**: The unit of selection MUST be a single word, defined as a run of letters (including accented letters and ñ) and/or digits; a hyphen or apostrophe between two such characters keeps them in one word (*bien-estar*, *l'hotel*). Numbers (*1990*) are words. All other characters — whitespace and punctuation such as ¿ ? ¡ ! , . ; : quotes and parentheses — are never selectable. Teacher and student MUST see the identical segmentation.

**Answering (student)**

- **FR-007**: The student MUST see the text with every word individually selectable by click/tap, with selected words clearly distinguished from unselected ones, and toggle selection by clicking again.
- **FR-008**: The student MUST be told how many wrong words the text contains, and see how many they have currently marked (e.g. "3 / 5 marcadas").
- **FR-008a**: The number of marked words MUST be capped at the number of wrong words. When the cap is reached, clicking an unmarked word MUST NOT mark it (the student is told to unmark a word first); a submission with more marked words than wrong words MUST be refused.
- **FR-009**: The student MUST NOT be able to learn which words are wrong, or the corrections, before submitting (answer keys hidden pre-submit, as for other auto-graded types).
- **FR-010**: The student MUST be able to submit with any number of words marked from zero up to the cap (FR-008a).

**Grading & results**

- **FR-011**: The question MUST be graded automatically on submission, behaving as an auto-graded (structured) question in the homework's composition and lifecycle: an all-auto homework is graded immediately; a mixed homework awaits the teacher only for its manual parts.
- **FR-012**: The question's score MUST be: (number of wrong words found) ÷ (total number of wrong words). Correct words the student marked do not lower the score. 100% means every error was found; 0% when nothing is marked.
- **FR-013**: The question MUST weigh as exactly one question in the overall percentage (its fractional FR-012 score), like fill-in-the-blanks or matching — errors are NOT expanded into separate questions the way numbered single-choice items are. It counts toward the fully-correct count only at 100%.
- **FR-014**: After grading, the student and the teacher MUST see the result inline in the full original text (paragraphs preserved, no separate list): each wrong word in a "found" style or a "missed" style, each followed by its correction when one was provided; each correct word the student marked in a third, distinct "marcada por error" style; all other words unstyled. The three states MUST be distinguishable without relying on colour alone (e.g. icon, underline or label).
- **FR-015**: A submission MUST always be judged and displayed against the version of the question the student answered, even if the teacher later edits it.

**Wider integration**

- **FR-016**: A spot-the-wrong-words question with any missed wrong words MUST appear as a single entry in the student's "Errores frecuentes" admin view, consistent with how it is scored; a 100% answer MUST NOT appear.
- **FR-017**: The question type MUST be available in v1 in all three places the other auto-graded question types can be authored — homework, presentation activities and quizzes — with identical authoring, answering, cap and scoring behaviour in each. ("Errores frecuentes", FR-016, covers homework only, as for other types.)
- **FR-018**: All new student-facing and teacher-facing labels MUST be provided in every site language (es/en/ro), consistent with the rest of the site; validation messages are in Spanish like existing ones.

### Key Entities

- **Spot-the-wrong-words question**: a question within a homework/activity/quiz. Attributes: text, ordered list of words derived from the text, and the set of word positions marked as wrong, each with an optional correction.
- **Student answer**: the set of word positions the student marked. Graded into per-word outcomes (found, missed, wrongly marked) and an overall question score.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The teacher can author a spot-the-wrong-words question with a 150-word text and 5 errors in under 3 minutes.
- **SC-002**: A student can mark and unmark words on a phone-width screen with no accidental selection of an adjacent word in normal use.
- **SC-003**: 100% of submissions are graded automatically at submit time with no teacher intervention for this question type.
- **SC-004**: For any submission, the displayed score matches the scoring rule in FR-012 exactly (verifiable by hand for any test case).
- **SC-005**: Editing a question after students have submitted changes none of their previously shown results or scores.

## Assumptions

- This is a new **question type** inside the existing homework model (like fill-in-the-blanks or matching), not a separate kind of homework; homeworks can mix it with other types.
- Students only identify wrong words; typing the correction is out of scope for this version (could be a later extension).
- Each error is a single word; multi-word errors and missing-word errors are out of scope.
- The number of errors is always shown to the student (no teacher toggle in v1).
- Scoring counts only found errors (FR-012); wrongly marked words are feedback, not a penalty.
- Reuses existing behaviour for assignment, submission lifecycle, labels, notifications/unseen dots, email on new homework, and the frozen snapshot of the question at submission time.
- Words are identified by their position in the text, so identical spellings are independent.
