# Quickstart: Listening Media Sources

**Feature**: `035-listening-media-sources` | **Date**: 2026-08-11

Manual browser validation after implementation. See [data-model.md](./data-model.md) and [contracts/listening-media-sources-api.md](./contracts/listening-media-sources-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` + Mailpit (usual local setup).
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081` (Flyway applies `V18__listening_media_source_kind`).
3. Frontend: `npm run dev` in `front-end/` → `:8080`
4. Admin (teacher) and student accounts; student has `STUDENT` role.

## Scenario A — External video-page link

1. Admin → Homework → create/edit **AUDIO** listening homework.
2. Choose **Enlace a vídeo** / Video page link; paste a non-YouTube page URL (e.g. a Vimeo *page* URL is fine here only if you want outbound — prefer a generic `https://example.com/...` or a known video landing page).
3. Save; assign to student.
4. As student, open the homework.
5. **Expect**: a clear link that opens in a **new tab**; no in-page `<audio>` and no YouTube/Vimeo iframe for this kind.

## Scenario B — YouTube option + auto-switch

1. Author AUDIO → choose **YouTube**; paste a watch/youtu.be/shorts URL; confirm editor preview embeds.
2. Save; student opens → in-page YouTube embed.
3. Edit again: select **Enlace (URL)**; paste a YouTube URL.
4. **Expect**: radio auto-switches to **YouTube** with the same URL.
5. Repeat paste into **Enlace a vídeo** → same auto-switch to YouTube.
6. Paste a non-YouTube URL into YouTube mode → save blocked with clear validation message.

## Scenario C — Legacy Enlace YouTube / Vimeo

1. Use a pre-migration (or backfilled) AUDIO homework with `mediaSourceKind=AUDIO_URL` and a YouTube URL in `audioUrl` (do not change kind).
2. Teacher opens editor → **Enlace (URL)** remains selected (not YouTube).
3. Student opens → YouTube still embeds in-page.
4. Same for a Vimeo URL under `AUDIO_URL` → student still sees Vimeo embed.

## Scenario D — Uploaded file + direct audio URL still work

1. Author with **Archivo subido**; student hears native audio player.
2. Author with **Enlace (URL)** pointing at a direct `.mp3` (or similar); student hears native audio player.

## Scenario E — Media required + incomplete block

1. Attempt to save AUDIO with no source / empty fields → blocked with clear message.
2. If a backfilled incomplete AUDIO exists (`mediaSourceKind` null): student cannot open/take it; teacher can open editor, set a source, save; then student can open.

## Scenario F — Non-listening unchanged

1. Create/edit a READ or WRITE homework → no four-option media picker; save without media succeeds.

## Backend smoke (optional)

```bash
cd back-end
./gradlew test --tests '*HomeworkAdmin*' --tests '*Exercise*'
```

(Adjust test filters to the suites touched during implementation.)
