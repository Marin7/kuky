# Implementation Plan: University Student Portal

**Branch**: `023-university-portal` | **Date**: 2026-08-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/023-university-portal/spec.md`

## Summary

Deliver a **separate public entry** (production subdomain / local university host) for Paula’s university cohort, sharing the existing account system. Add role `UNIVERSITY_STUDENT` (mutually exclusive with `STUDENT`) plus `users.university_level` (`BEGINNER` | `INTERMEDIATE`). Public university GETs expose schedule (template + dated exceptions; full labeled timetable for anonymous readers, level-filtered for enrolled students), exam dates, and news. Gated learning reuses the private homework/presentation catalogs via new per-level availability joins; submissions reuse existing homework pipelines. Teacher manages university roster/content from the existing admin panel; grant/revoke sends **no** email. CORS and auth-cookie domain are extended so login works across both entries.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5 (`-web`, `-security`, `-jdbc`, `-mail`), Flyway, PostgreSQL JDBC. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new dependencies** — host-aware layout + university routes/components from existing primitives.

**Storage**: PostgreSQL 18. Migration `V4__university_portal.sql`:
- Widen `users.role` CHECK to include `UNIVERSITY_STUDENT`; add nullable `university_level` with CHECK (`BEGINNER`,`INTERMEDIATE`) and consistency rule (level NOT NULL iff role is `UNIVERSITY_STUDENT` — enforce in service + DB CHECK or trigger-friendly CHECK).
- Tables: `university_schedule_sessions`, `university_schedule_exceptions`, `university_exam_dates`, `university_news_items`, `university_homework_availability`, `university_presentation_availability`.

**Testing**:
- Backend: JUnit 5 + Spring Security test — role mutual exclusion, public vs gated matchers, schedule merge (template + exceptions), availability grants, homework submit path for university students, admin CRUD.
- Frontend: constitution browser verification — university host shell, public informative pages, gated materials/homework, admin university tabs, registration on university entry.

**Target Platform**: Browser via TanStack Start SSR + JVM API (`:8081`). Two public front-end hosts in production (private site + university subdomain) against one API.

**Project Type**: Full-stack web (existing `front-end/` + `back-end/`).

**Performance Goals**: Informative pages load for anonymous visitors within normal page expectations (SC-002/004 style); schedule merge is tiny (≤7 template rows + sparse exceptions).

**Constraints**: Single role column; no dual STUDENT+UNIVERSITY; no university grant/revoke email; public informative / gated learning; shared catalogs with explicit per-level availability; cookie+CORS must support both origins.

**Scale/Scope**: One teacher, one university cohort (tens of students), two levels, seven weekly sessions, modest news/exam lists. New backend package `university`, SecurityConfig/CORS/cookie edits, admin panel tabs, host-aware frontend shell + university routes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS with one justified complexity: host-based second entry is required by clarified FR-001 (not a second app/repo). Prefer fourth role value + availability joins over multi-role tables or catalog duplication. See Complexity Tracking.
- **II. Component-Driven UI** — PASS. University shell, schedule, news, exams, learning views, and admin tabs are named React components; no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Client API modules under `front-end/src/lib/` (`university.ts`, extend `auth.ts`/`admin.ts`/`learning.ts`); no fetches inlined beyond existing patterns.
- **Technology Stack** — PASS. No new runtime dependencies.
- **Development Workflow** — PASS. Quickstart requires browser verification on university host; branch `023-university-portal`.

**Post–Phase 1 re-check**: PASS — contracts and data model stay within JDBC + existing learning reuse; complexity limited to multi-origin auth as required by spec.

**Result: PASS — one justified complexity entry below.**

## Project Structure

### Documentation (this feature)

```text
specs/023-university-portal/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── university-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                 # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V4__university_portal.sql

back-end/src/main/java/com/kuky/backend/
├── university/                          # NEW package
│   ├── model/ … dto/ … repository/ … service/ … controller/
│   ├── UniversityScheduleController.java      # public GETs
│   ├── UniversityExamsController.java         # public GETs
│   ├── UniversityNewsController.java          # public GETs
│   ├── UniversityLearningController.java      # UNIVERSITY_STUDENT|ADMIN
│   └── admin/UniversityAdminController.java   # or split admin controllers under admin/
├── auth/
│   ├── model/User.java                    # EDIT: universityLevel; role default unchanged
│   ├── dto/UserResponse.java              # EDIT: role union + universityLevel
│   └── repository/UserRepository.java     # EDIT: grant/revoke university helpers
├── admin/controller/StudentAdminController.java  # EDIT: university grant/revoke + mutual exclusion with STUDENT
├── learning/                              # EDIT: access checks accept university availability
├── config/
│   ├── SecurityConfig.java                # EDIT: public university GETs; UNIVERSITY_STUDENT gates
│   ├── CorsConfig.java                    # EDIT: multiple allowed origins
│   └── JwtCookieAuthenticationFilter.java # EDIT: optional cookie Domain from config
└── …

front-end/src/
├── lib/
│   ├── auth.ts                            # EDIT: role UNIVERSITY_STUDENT; universityLevel
│   ├── university.ts                      # NEW: public + learning + (if needed) thin wrappers
│   ├── admin.ts                           # EDIT: university admin APIs
│   └── learning.ts                        # EDIT or parallel university learning helpers
├── components/university/                 # NEW: schedule, news, exams, materials, homework, notices
├── components/admin/university/           # NEW: admin tabs (roster, schedule, news, exams, availability)
├── routes/
│   ├── __root.tsx                         # EDIT: host-aware shell selection
│   ├── (private)/…                        # existing private routes (unchanged behavior)
│   └── (university)/…                     # NEW route group: home, schedule, exams, news, learning, cuenta
└── i18n/locales/{es,en,ro}.ts             # EDIT: university copy
```

**Structure Decision**: Web application (`front-end/` + `back-end/`). New `university` backend package for informative domain; extend `auth`/`admin`/`learning`/`config` for role and shared-catalog access. Frontend uses one app with host-based shell and a university route group — not a second package.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Second public entry (subdomain/host) + multi-origin CORS/cookie Domain | Clarified FR-001: separate URL/subdomain with shared accounts | Path-only `/universidad` on one origin fails the clarified separation; second frontend repo doubles maintenance |
