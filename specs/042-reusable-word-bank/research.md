# Research: Reusable Word Bank

**Feature**: `042-reusable-word-bank` | **Date**: 2026-08-15

## 1. How the take view learns mode without the answer key

**Decision**: Student DRAG_DROP `structure` becomes `{ bank, bankReusable }`. Server sets `bankReusable: true` iff any bank item id appears in two or more blanks’ `correctBankIds` after `DragDropStructureSupport.resolve`. Never send `blanks` or `correctBankIds` pre-submit. Missing/`false` → exclusive (today’s UX).

**Rationale**: FR-008 forbids leaking which items are correct for which blanks. A single boolean only reveals that *some* item is shared — which the enabled chips already imply. Detection matches FR-001 (identity, not label).

**Alternatives considered**:
- Send `blanks[].correctBankIds` to the client — leaks the key; rejected.
- Always allow reuse — rejected by spec (exclusive questions must stay disable-on-place).
- Client infers from bank size vs blank count — wrong: distractors already make bank larger without sharing; shared-id questions can still have bank.length ≥ blanks.
- Per-word reusable set — YAGNI; spec is question-level (all chips stay available).

## 2. Placement: copy vs move

**Decision**: Gate existing `place()` in `DragDropQuestion`:
- **Exclusive** (`bankReusable !== true`): keep today’s `map(v => v === itemId ? null : v)` then set the target (move); disable/strikethrough placed chips; not draggable while placed.
- **Reusable**: set `next[blankIndex] = itemId` only (copy); do not clear other blanks; chips stay enabled and draggable; click-to-select still works after placement.

Replace-on-occupied-blank and click-to-clear a blank with no selection stay as today in both modes.

**Rationale**: Current `place()` is why students cannot occupy two blanks with one id even if grading would accept it. Clarification: no new instructional copy.

**Alternatives considered**:
- Only stop disabling chips but keep move semantics — still cannot show the same word in two blanks; rejected.
- Drag from filled blanks as a second source — extra UX; blanks are not draggable today; YAGNI.

## 3. Grading and submit

**Decision**: Keep per-blank `correctIds.contains(placedId)`. Add a test that the **same** id in two placements scores both units correct when that id is in both keys. Do not reject duplicate ids in `answerJson.placements`. Exclusive questions can still be submitted with duplicates via a tampered client; that cannot yield extra credit because exclusive keys do not share an id (the same id cannot be correct for two blanks).

**Rationale**: `QuestionScoring.gradeDragDrop` / activity twin already any-of. Existing `dragDrop_orderIndependentSharedAcceptedSet` places *different* ids that both keys accept; it does not prove duplicate occupancy.

**Alternatives considered**:
- Enforce unique placements on exclusive submit — extra validation with no score benefit; YAGNI.
- Re-grade old submissions — spec says no.

## 4. Where to compute `bankReusable`

**Decision**: `DragDropStructureSupport.isBankReusable(Resolved)` (or equivalent). Call it from both `ExerciseGradingService.stripStructureForStudent` and `ActivityExerciseGradingService.stripStructureForStudent`. Quizzes already map take questions through `ExerciseGradingService.studentQuestionsFor`.

Legacy positional structures (`bank` only) resolve to `blanks[i] = [bank[i].id]` → no shared ids → `bankReusable: false`.

**Rationale**: One definition of FR-001; homework + activities already duplicate strip; quizzes inherit homework strip.

**Alternatives considered**:
- Extract a full shared strip helper for all kinds — larger refactor; out of scope.
- Compute only on the frontend from a new admin-only field — would not work for student GET.

## 5. Authoring

**Decision**: No editor changes. `validateDragDrop` already persists shared `correctBankIds` (`dragDropAllowsSameBankIdOnTwoBlanks`). Bank size still ≥ blank count (existing 034 rule): a two-blank “only *el*” key still needs a second bank chip (typically a distractor).

**Rationale**: Spec: authoring unchanged. 034 research table that said “same bank id on two blanks: Forbidden” is superseded by 034 spec/contract and current code.

**Alternatives considered**:
- Teacher toggle “allow reuse” — rejected; mode is derived.
- Lower bank minimum when ids are shared — separate product change; out of scope.

## 6. Surfaces and result UI

**Decision**: Pass `bankReusable` into `DragDropQuestion` from `ExerciseForm` and `MixedHomeworkForm`. That covers homework take, inline/mixed, presentation activities (`ActivityPanel`), and quizzes (`quizzes_.$quizId`). Result/review already renders per-blank `studentDisplay` via `MultiBlankResult`; duplicate labels need no new result component.

**Rationale**: Clarification: every student take of this kind. No i18n (clarification: no extra copy).

**Alternatives considered**:
- Homework-only flag — more branching; rejected.
- New result bank visualization — YAGNI.
