# Kuky — Español con Paula

Personal website for Paula, a Spanish teacher targeting Romanian students.  
Full-stack: React 19 SSR frontend + Java Spring Boot REST API.

## Tech stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TanStack Start (SSR), TanStack Router, TailwindCSS 4, Shadcn UI |
| Backend | Java 21, Spring Boot 3.5, Spring Security, plain JDBC (NamedParameterJdbcTemplate) |
| Database | PostgreSQL 17+ |
| Auth | JWT in `HttpOnly; Secure; SameSite=Lax` cookies, 7-day rolling session |
| Email | JavaMailSender (Mailpit for local dev) |
| Meetings | Zoom Server-to-Server OAuth (stub provider for local dev) |
| Build | Vite 7 (frontend), Gradle (backend) |
| CI | GitHub Actions |

## Local dev setup

### Prerequisites

- **Node.js 22+** (Vite 7 minimum; Node 20 is too old)
- **Java 21**
- **PostgreSQL 17+** running locally (pgAdmin works)
- **Mailpit** for SMTP capture — `mailpit` (SMTP on port 1025, web UI on http://localhost:8025)

### 1. Database

Run once in psql:

```sql
CREATE DATABASE kuky_dev;
CREATE USER kuky WITH PASSWORD 'kuky';
GRANT ALL PRIVILEGES ON DATABASE kuky_dev TO kuky;
GRANT CREATE ON SCHEMA public TO kuky;
ALTER USER kuky WITH SUPERUSER;
```

### 2. Backend

```bash
cd back-end
./gradlew bootRun --args='--spring.profiles.active=local'
# API → http://localhost:8081
```

Flyway runs all migrations automatically on first start. The teacher account
(`paula@kuky.es` by default in local profile) is promoted to `ADMIN` on startup
by `AdminBootstrap` — the role lands in the JWT on her next login.

### 3. Frontend

```bash
cd front-end
npm install
npm run dev
# App → http://localhost:8080
```

No environment variables needed for local dev — the API client falls back to
`http://localhost:8081` automatically.

## Pages

| Route | Description |
|-------|-------------|
| `/` | Landing page — hero, features, CTA |
| `/sobre-mi` | Paula's bio and teaching stats |
| `/cuenta` | Register, login, logout, forgot/reset password |
| `/reservas` | Public schedule, 1-on-1 booking with Zoom, upcoming & past classes |
| `/recursos` | Free & paid teaching resources, purchase, unlock, receipts |
| `/aprendizaje` | Student-only: class intro, presentations, homework |
| `/panel` | **Admin-only**: availability, homework authoring, presentation builder |

## Other commands

```bash
# Frontend
npm run lint      # ESLint
npm run build     # Production build → dist/

# Backend
./gradlew build   # Compile + test
./gradlew test    # Tests only
```

## Production environment variables

Set these in your hosting environment before deploying.

### Backend

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC URL — `jdbc:postgresql://host:5432/kuky` |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `APP_JWT_SECRET` | HS256 signing key, ≥32 characters (`openssl rand -base64 48`) |
| `CORS_ALLOWED_ORIGIN` | Frontend origin — `https://kuky.es` |
| `FRONTEND_BASE_URL` | Used in email links — `https://kuky.es` |
| `MAIL_HOST` | SMTP host |
| `MAIL_PORT` | SMTP port (typically 587 for STARTTLS) |
| `MAIL_USERNAME` | SMTP username (e.g. `apikey` for SendGrid) |
| `MAIL_PASSWORD` | SMTP password / API key |
| `MAIL_FROM` | From address |
| `TEACHER_EMAIL` | Paula's email — receives booking notifications and gets promoted to ADMIN on startup |
| `ZOOM_ACCOUNT_ID` | Zoom Server-to-Server OAuth — leave blank to use the stub provider |
| `ZOOM_CLIENT_ID` | Zoom client ID |
| `ZOOM_CLIENT_SECRET` | Zoom client secret |
| `ZOOM_USER_ID` | Zoom user who hosts meetings (default: `me`) |

See [`back-end/.env.example`](back-end/.env.example) for a copy-paste starting point.

### Frontend

| Variable | Description |
|----------|-------------|
| `VITE_API_BASE_URL` | Backend origin — `https://api.kuky.es` (no trailing slash) |
| `VITE_SITE_URL` | Public site URL for SEO — `https://kuky.es` |

See [`front-end/.env.example`](front-end/.env.example) for a copy-paste starting point.

## Project structure

```
kuky/
├── front-end/          # React 19 + TanStack Start SSR app
│   ├── src/
│   │   ├── components/ # UI primitives (Shadcn), auth forms, scheduling, etc.
│   │   ├── lib/        # API clients (auth, scheduling, resources, learning, admin)
│   │   └── routes/     # File-based pages (TanStack Router)
│   └── .env.example
├── back-end/           # Java 21 + Spring Boot 3.5 REST API
│   ├── src/main/
│   │   ├── java/com/kuky/backend/
│   │   │   ├── auth/       # Register, login, JWT, password reset, activation
│   │   │   ├── scheduling/ # Public schedule, bookings, Zoom integration
│   │   │   ├── resources/  # Catalog, purchases, receipts
│   │   │   ├── learning/   # Homework, past classes, presentations (student)
│   │   │   ├── admin/      # Backoffice: availability, homework, presentations
│   │   │   └── config/     # Security, CORS, Flyway, JWT, scheduling properties
│   │   └── resources/
│   │       ├── application.yaml        # Production config (env var placeholders)
│   │       ├── application-local.yaml  # Local dev overrides
│   │       └── db/migration/           # Flyway migrations V1–V14
│   └── .env.example
└── .github/workflows/ci.yml  # CI: backend build+test, frontend lint+build
```
