# Quickstart: Validating Unit as a PDF with Page Activities

## Prerequisites

- PostgreSQL 18 `kuky_dev` and Mailpit running (see `CLAUDE.md` → Local dev setup).
- Back-end: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'`. Flyway applies `V28__unit_owned_pdf_activities.sql` on startup.
- Front-end: `cd front-end && npm run dev`, then open http://localhost:8080.
- One admin account (the `TEACHER_EMAIL` account) and at least one student account with `STUDENT` role.
- Two test PDFs: `ten-pages.pdf` (10 pages) and `six-pages.pdf` (6 pages).

## Automated checks

```bash
cd back-end && ./gradlew test
```
```bash
cd front-end && npm run lint && npm run build
```

Integration tests run in CI against Postgres and are skipped locally. The unit tests must
cover:
- PDF validation: rejects non-PDF files and an out-of-range `pageCount`.
- Replace-with-fewer-pages: returns 409 without the flag; with the flag, deletes only the activities beyond the new page count.
- Marking: rejects a duplicate page (409) and a page outside 1..pageCount.
- Unmarking: returns 409 when submissions exist; succeeds with `deleteSubmissions=true`.
- The Presentations service returns 404 for unit-owned presentation ids.
- Activity submit sets the submission unseen; opening the review marks it seen.

## Browser scenarios

| # | Steps | Expected |
|---|---|---|
| 1 | **Panel → Units**: create unit "A1 · La familia". | You land on `/panel/unidades/{id}`, which shows "no PDF yet" and an upload control. The Units tab has no presentation picker. |
| 2 | Upload `ten-pages.pdf`. | A 10-page thumbnail strip appears and page 1 shows large. Reloading the page keeps the PDF. |
| 3 | Try uploading a `.docx`. | You get a clear error and the PDF is unchanged. |
| 4 | Select page 4, switch on **Marcar como actividad**, add two self-correcting questions (e.g. single choice + spot-wrong-words), leave the title blank, save. | Page 4's thumbnail shows an activity badge and the activity is listed as "Actividad – página 4". |
| 5 | Select page 7, mark it, add one FREE_TEXT question, title "Escribe sobre tu familia", save. | Two activities are listed, in page order. |
| 6 | Try marking page 4 again (e.g. by refreshing the form state). | You cannot create a second activity on that page. |
| 7 | Add an existing homework through the unit's content list and drag it above the PDF. | The order persists after reload. |
| 8 | Assign the unit to the student. | The new-homework email reaches Mailpit for the unit's homework (as today). |
| 9 | Check the **Panel** tabs. | There is no **Actividades** tab, and `/panel/actividades/nueva` no longer exists (not found). |
| 10 | **Panel → Presentaciones**. | Only standalone presentations are listed; the unit's PDF is absent. Sharing a standalone presentation still works. |
| 11 | As the student, go to **Mi aprendizaje → unit**. | The unit shows the homework and the PDF in the teacher's order. The PDF entry lists "p. 4 · Actividad – página 4 · Pendiente" and "p. 7 · Escribe sobre tu familia · Pendiente". |
| 12 | Open the PDF viewer and scroll to page 4. | Directly after page 4 there is an activity block. Opening it shows the questions; submitting shows the score immediately with status Graded. |
| 13 | Scroll to page 7, answer the free-text question, submit. | Status becomes Submitted, awaiting correction. |
| 14 | Open `/aprendizaje/actividad/{id}` for page 7 from the list. | Page 7 is rendered as the instructions above the answer and status. |
| 15 | As the teacher, check the Panel. | The **Panel** dot and the **Unidades** tab dot are on; the unit card dot is on. The unit page's progress shows the student "Por corregir" on page 7. |
| 16 | Open the submission, set 80 %, finalize. | The dots clear. The student sees Graded at the combined %. |
| 17 | Student profile → **Presentaciones** box. | Both activities are listed under "La familia" with their status, and clicking page 7 opens the same review dialog. |
| 18 | Replace the PDF with `six-pages.pdf`. | A confirmation says 1 activity (page 7) and its submissions will be removed. Cancel leaves everything unchanged; confirm leaves only the page 4 activity and a 6-page strip. |
| 19 | Unmark page 4, which has a submission. | You are asked to confirm that submissions will be deleted; confirming removes it. |
| 20 | Delete the unit. | The unit disappears. The student no longer sees it. The homework still exists in the Tareas tab. |
| 21 | Repeat 2, 4, 11 and 12 at 375 px width. | No horizontal page scroll; thumbnails scroll horizontally inside their strip; the editor stacks vertically. |
