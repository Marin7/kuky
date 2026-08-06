# Quickstart: View Presentations On-Site

Validate end-to-end after implementation. See [contracts/presentation-viewer.md](./contracts/presentation-viewer.md) and [data-model.md](./data-model.md).

## Prerequisites

1. PostgreSQL `kuky_dev`; backend `local` profile; frontend dev server.
2. Teacher account: a presentation **shared** (or unit-assigned) to a student with at least:
   - one **PDF** file
   - one **PowerPoint** file (same or another presentation is fine)
3. That student account available for login.

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

## Automated checks

```bash
# back-end/ — existing learning/presentation access tests should still pass
./gradlew test --tests '*Learning*' --tests '*Presentation*'
```

No new backend tests required for MVP if the file GET is unchanged; add tests only if disposition/endpoint behavior is modified later.

## Browser validation

### Student — open PDF (P1)

1. Log in as the student → **Aprendizaje**.
2. Find the shared presentation’s PDF → confirm **Open/View** is the primary control and **Download** is secondary.
3. Click Open/View → full-page viewer shows PDF content (readable, not only a download).
4. Click **Back to learning materials** → land on `/aprendizaje` with the list available.
5. Open the same PDF again → copy the URL → refresh → PDF still loads.
6. Open PDF → use browser **Back** after coming from Aprendizaje → returns via history (typically Aprendizaje).

### Student — download still works (P1)

1. From the file list, download the PDF → file saves under display name.
2. Download the PowerPoint → succeeds.
3. After viewing a PDF and returning, download that PDF again → succeeds.

### Student — PowerPoint download-only (P2)

1. PowerPoint row has **Download** only (no Open/View).
2. Manually visit a viewer URL with a PPTX `fileId` (if known) → friendly not-viewable (or error) + Back; no fake PDF chrome.

### Access denied (P1)

1. As a student **without** access, open a bookmarked viewer URL for another student’s presentation → cannot see content; Back available.
2. Guest hitting the viewer URL → redirected to sign-in (`/cuenta`) consistent with Aprendizaje.

### Load failure

1. With network throttled or invalid ids → loading then clear error + Back to learning.

## Done when

- [ ] PDFs open on a dedicated full-page viewer without forced download-first
- [ ] Explicit Back always goes to `/aprendizaje`
- [ ] Refresh/bookmark works while shared
- [ ] PPTX is download-only; download remains on the list for all types
- [ ] Viewer has no download button
- [ ] i18n strings present for es/en/ro
- [ ] Verified in a running browser (constitution)
