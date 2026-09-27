# Contracts: Homework Admin Search, Sort & Pagination

No API contract changes. This feature is front-end-only (see `plan.md` Summary and `research.md` Decision 1).

- **Endpoint**: `GET /api/v1/admin/homework` — unchanged. No new query parameters (no `search`, `sortBy`, `sortDir`, `page`, or `size`).
- **Response shape**: unchanged — `List<HomeworkAdminItem>` (`back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkAdminItem.java`), still arriving pre-sorted `ORDER BY created_at DESC`.
- **Auth/roles**: unchanged — existing `ADMIN`-only access to `/api/v1/admin/**` is untouched.

All search, sort, and pagination behavior described in `spec.md` is computed client-side over this existing response — see `data-model.md` for the resulting view-state shape.
