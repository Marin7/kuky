# Quickstart: Presentation Activities

Validate end-to-end against [spec.md](./spec.md), [data-model.md](./data-model.md), and [contracts/activities-api.md](./contracts/activities-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` + Flyway through `V11__presentation_activities.sql`
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081`
3. Frontend: `npm run dev` in `front-end/` → `:8080`
4. Teacher (`ADMIN`) and at least one `STUDENT` with access to a unit that contains a multi-page PDF presentation

## Setup data

1. As admin: ensure a presentation has ≥1 PDF with several pages; share/assign so the student can open it.
2. **Activities** tab → create Activity A: title, instructions PDF, link presentation, format MANUAL, save.
3. Create Activity B: EXERCISE with ≥1 question; set page trigger = that PDF + page 2; save.
4. Reorder A/B under the presentation; reload — order persists.

## Student — nested list (US2)

1. Log in as student → open the unit → expand the presentation.
2. Expect activities listed under the presentation in teacher order (not as top-level unit peers).
3. Open Activity A from the list → full page → view instructions PDF → submit free text → status updates under the presentation.

## Student — viewer prompt + overlay (US3)

1. Open the presentation PDF in the on-site viewer (full-page route).
2. Navigate to page 2 → within ~2s see a non-blocking prompt for Activity B.
3. Open from prompt → overlay on top of viewer → complete/submit or close → viewer page position unchanged.
4. Dismiss without opening → viewing continues; activity still under presentation expansion.
5. After fulfilling B, return to page 2 → **no** prompt.
6. Open a different PDF file in the same presentation → B’s prompt does not fire.

## Teacher — review + progress (US4)

1. Admin: review MANUAL submission; view EXERCISE result.
2. Open student profile progress → activity breakdown / counts reflect fulfilled work.

## Regression

1. Homework create/submit/review still works.
2. Presentation without activities: expand shows no activity list; viewer has no prompts.
3. Delete presentation → its activities and submissions are gone; instruction files removed from disk.

## API smoke (optional)

```bash
# After login cookie captured:
# GET /api/v1/learning  → sharedPresentations[].activities present
# GET /api/v1/admin/activities?presentationId=...
```

## Done when

- [ ] Author + reorder + trigger persist
- [ ] Nested student list + full-page fulfill
- [ ] Page-land prompt + overlay + no re-prompt when done
- [ ] Progress overview includes activities
- [ ] Cascade delete + homework/presentation regressions clean
