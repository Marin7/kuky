# Research: Admin Homework Verify Review

**Feature**: `032-homework-verify-review` | **Date**: 2026-08-10

## 1. Legacy vs new review model discriminator

**Decision**: Add nullable `review_model` (`LEGACY_RICH` | `ANNOTATED`) on `homework_submissions` and `activity_submissions`. Migration sets `LEGACY_RICH` for every already-`REVIEWED` MANUAL (or MANUAL activity) row that has non-null `feedback`. First successful new-model review sets `ANNOTATED`. `SUBMITTED` / never-reviewed rows keep `NULL` until that save.

**Rationale**: Spec freezes legacy rich whole-submission feedback and allows re-edit only for new-model reviews. Plain legacy notes without marks are indistinguishable from new plain notes by content alone, so a stored discriminator is required (clarify Q4).

**Alternatives considered**:
- Heuristic on formatted marks in `feedback` — fails for plain-only legacy notes; rejected.
- Separate `feedback_plain` column only — still need a flag for frozen vs editable; rejected as incomplete alone.

## 2. In-place annotation storage (WRITE + FREE_TEXT)

**Decision**:
- **WRITE**: Keep annotating on `response_text` (FormattedText JSON). On save, require `plainText(incoming) == plainText(stored)` so wording cannot change; marks may be added/cleared/changed (clarify Q3).
- **FREE_TEXT multi answers**: Keep using `answer_text`. Until annotated it stays plain string; on teacher save, store FormattedText JSON in the same column when any marks (or always store as single-segment JSON for consistency after first new-model review). Readers: if value parses as FormattedText array, treat as formatted; else plain. Same plainText equality check against the pre-save plain wording.

**Rationale**: Avoids new answer columns (YAGNI). Matches existing WRITE persistence. Student submit path stays plain for FREE_TEXT.

**Alternatives considered**:
- New `answer_formatted` column — clearer separation, extra migration/DTO surface; deferred unless needed.
- Parallel annotation overlay table — overkill for single-teacher site.

## 3. Plain ≤500-char feedback replaces rich feedback editor

**Decision**: New-model MANUAL review PUT accepts `feedback` as optional plain string (max 500 after strip; whitespace-only → null/clear). Persist via existing `feedback` column using single-segment FormattedText JSON (same encoding as exercise plain feedback, but max **500**). API exposes `feedbackText` for ANNOTATED reviews; `feedback` FormattedText remains for `LEGACY_RICH` display only. Empty feedback allowed on first review and later edits (spec).

**Rationale**: Clarify Q1 replaces rich whole-submission editor. Reusing the column avoids dual storage; length differs from exercise’s 2000.

**Alternatives considered**: Keep rich editor alongside plain box — rejected in clarify. New `feedback_plain` column — unnecessary if encoding stays single plain segment for ANNOTATED.

## 4. Post-review edits vs ALREADY_REVIEWED

**Decision**: For `review_model = ANNOTATED` (or first transition SUBMITTED→ANNOTATED), allow PUT feedback/annotate anytime while status is `SUBMITTED` or `REVIEWED`. Remove/relax `AlreadyReviewedException` for that path. For `LEGACY_RICH`, keep view-only and reject saves with `ALREADY_REVIEWED` (or a dedicated frozen error mapped to the same client handling). First new-model save still sets `status = REVIEWED`, `reviewed_at` (set once on first transition; later edits bump `updated_at` only).

**Rationale**: Clarify Q2. Legacy freeze is Q4.

**Alternatives considered**: Always allow re-edit including legacy — rejected. Time-boxed edit window — rejected.

## 5. Format-only rich-text editor UX

**Decision**: Extend existing `RichTextEditor` with `formatOnly` (name TBD in tasks): toolbar + selection formatting work; typing/paste/delete that would change characters are blocked (ignore input or revert). Mirror uses stronger wrap (`overflow-wrap: anywhere` / Tailwind `break-all` as needed) so unbroken lines never force horizontal scroll in review or student views. Apply the same wrap class to plain answer `<p>` blocks in review dialogs.

**Rationale**: Spec forbids rewriting words; reusing the editor preserves palette/toolbar. `break-words` alone does not always break long tokens — `anywhere`/`break-all` satisfies SC-001.

**Alternatives considered**: contentEditable annotate layer — more complex, against current textarea+mirror pattern. Separate annotation component — duplication.

## 6. Activity parity + EXERCISE out of scope

**Decision**: Mirror homework review changes in `ActivityAdminService`, activity submission repository, `ActivityReviewDialog`, and student activity item DTOs. Leave `…/exercise-feedback` endpoints and GRADED behavior unchanged.

**Rationale**: Clarify Q5; FR-014/FR-015.

**Alternatives considered**: Homework-only — rejected in clarify.
