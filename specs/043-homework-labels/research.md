# Research: Homework Labels

**Feature**: `043-homework-labels` | **Date**: 2026-08-16

## 1. Storage: column vs catalog table

**Decision**: Nullable `homework_assignments.label VARCHAR(40)`. No `labels` table, no join, no unique catalog of names.

**Rationale**: Spec: a label exists only as text on homeworks; unused labels disappear; one optional label per homework. A catalog would need create/rename/delete UX the spec forbids.

**Alternatives considered**:
- `labels` + `homework_label_id` FK — extra table and lifecycle; rejected (YAGNI).
- Many-to-many tags — spec is one label.
- Store in `structure_json` / a JSON metadata column — homework already has typed columns for type/level; a column is queryable and matches that pattern.

## 2. Where filtering happens

**Decision**: Keep `GET /api/v1/admin/homework` returning the full list. Derive filter choices and apply the label filter in `HomeworkAdminList`, the same way type and level already work.

**Rationale**: The list is already loaded for the tab. A `?label=` query param would not reduce payload today and would duplicate AND-logic already on the client. Distinct labels = unique case-folded values from the loaded items (null/blank omitted). No unlabeled choice.

**Alternatives considered**:
- Server-side `?label=` — extra contract and no win at current scale.
- Dedicated `GET /api/v1/admin/homework/labels` — second request for data already on the list; rejected.

## 3. Case-insensitive grouping

**Decision**: Group with Unicode case folding that **keeps accents**: Java `trim` then `toLowerCase(Locale.ROOT)` for tests/normalization keys if needed; TypeScript `trim()` + `toLocaleLowerCase('es')` (or equivalent) as the filter/reuse key. `Gramática` ≠ `Gramatica`; `Subjuntivo` = `subjuntivo`. Each homework still displays its stored string. Filter/reuse dropdown shows **one** entry per key; display text is the first spelling encountered while scanning the current list.

**Rationale**: Clarification B. ROOT/`es` lowercasing does not strip combining marks, so `sí`/`si` stay distinct. First-seen display is deterministic enough for a single-teacher list and avoids a “canonical spelling” write-back.

**Alternatives considered**:
- Exact match including case — rejected by clarification.
- Accent-insensitive (`sensitivity: 'base'`) — would merge `sí`/`si`; rejected.
- Rewrite stored text to the picked spelling on save — extra mutation; spec wants each homework to keep what was saved.

## 4. Normalization and validation

**Decision**: Server is source of truth. On create/update: treat `null` and missing as unlabeled; `trim`; if empty after trim → `null`; if length > 40 after trim → `400 VALIDATION_ERROR` with a Spanish message (existing handler). Do **not** apply `@Size(max = 40)` on the raw JSON string (would reject legal values that only fit after trim). Optional `@Size(max = 80)` as a request-size guard is allowed. DB `VARCHAR(40)` is the hard cap. Client may disable save / show the same limit in the field (`maxLength={40}` after the teacher is typing trimmed intent is optional; show remaining or reject on submit).

**Rationale**: FR-003/004/005. Bean Validation on untrimmed input is stricter than the spec.

**Alternatives considered**:
- Silent truncate — forbidden.
- Client-only validation — teacher could still POST 41 characters.

## 5. Freeze / `content_revised_at`

**Decision**: Label is **not** student-facing content. `contentChanged(...)` must **not** treat a label change as a content revision. Label-only save leaves `content_revised_at` and snapshots alone (FR-011). Do not add `label` to `assignment_snapshot`.

**Rationale**: Students must not see labels; freeze exists so question/key edits do not rewrite submissions. A teacher-only badge must not force `HOMEWORK_UPDATED` or wipe in-progress takes.

**Alternatives considered**:
- Bump freeze on any PUT — would surprise students when Paula only retags a homework; rejected.

## 6. Editor and list UI

**Decision**:
- **Editor**: optional combobox (Shadcn `Popover` + `Command`, same idea as `AddContentCombobox`): type a new label or pick one already on another homework (one row per case-folded group). Empty / clear control = unlabeled. Place it with type/level/due date on `HomeworkEditorPage`.
- **List**: new `Select` next to type and level: `ALL` + one item per group. Badge on the card when `label` is non-null (alongside type/level chips). If the selected group key vanishes from the list (last homework unlabeled or deleted), set filter back to `ALL` (do not show empty stale filter). Combined type+level+label with no rows still uses `admin.homework.noTasksFiltered`.

**Rationale**: Matches existing tab filters; reuse without a catalog; FR-008 vs FR-008a distinguished.

**Alternatives considered**:
- Plain text input only — fails FR-010 reuse-without-retyping.
- Inline label edit on the card — second write path; editor already saves metadata; YAGNI.

## 7. Surfaces that omit labels

**Decision**: Add `label` only to admin homework item + create/update bodies. Do **not** add it to student `HomeworkItemResponse` / exercise take DTOs, review-queue items, unit content picker payloads, quizzes, or presentations. Extra JSON on `HomeworkAdminItem` is harmless if the unit picker already uses that type — it simply ignores the field (spec: may ignore).

**Rationale**: FR-013, FR-014, homework-tab-only scope.

**Alternatives considered**:
- Hide on student UI but still send the field — unnecessary leak of teacher organization text; omit instead.
