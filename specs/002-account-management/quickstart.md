# Quickstart: Account Management Validation

**Feature**: `002-account-management` | **Date**: 2026-06-08

Run the full account management feature locally and validate every flow end-to-end.

---

## Prerequisites

| Tool | Required version |
|------|-----------------|
| PostgreSQL | 16+ (running via pgAdmin) |
| Mailpit | Latest — native Windows binary from https://github.com/axllent/mailpit/releases |
| Node.js (or Bun) | 22+ (Node) / latest (Bun) |
| Java | 26 |

---

## 1. Set Up Local Database (pgAdmin)

Open pgAdmin and run the following in the Query Tool (connect as the superuser):

```sql
CREATE DATABASE kuky_dev;
CREATE USER kuky WITH PASSWORD 'kuky';
GRANT ALL PRIVILEGES ON DATABASE kuky_dev TO kuky;
```

Flyway will create the tables automatically when the backend first starts.

---

## 2. Start Mailpit

Download `mailpit-windows-amd64.zip` from https://github.com/axllent/mailpit/releases, extract, then run:

```powershell
.\mailpit.exe
```

| Service | Port |
|---------|------|
| SMTP sink | 1025 |
| Web UI | http://localhost:8025 |

---

## 3. Start the Backend

```powershell
cd back-end
./gradlew bootRun --args='--spring.profiles.active=local'
```

- Flyway runs DB migrations automatically on startup
- API available at `http://localhost:8081/api/v1`
- Watch logs for `Started BackEndApplication`

---

## 4. Start the Frontend

```powershell
cd front-end
npm run dev
```

Frontend available at `http://localhost:8080`.

---

## Validation Scenarios

### Scenario 1 — Registration (FR-001, FR-002, FR-003, FR-014)

1. Navigate to `http://localhost:8080/cuenta`
2. Select the **Registrarse** tab
3. Fill the form:
   - Email: `test@ejemplo.com`
   - Password: `password123` (8+ characters)
   - Check the privacy policy consent checkbox
4. Submit

**Expected**: Account created → user signed in automatically → authenticated dashboard shown. `auth-token` cookie visible in browser DevTools (Application → Cookies); flag `HttpOnly` is checked.

**Error paths to verify**:
- Submit without the consent checkbox → validation error shown inline
- Submit with a password shorter than 8 characters → password requirements shown
- Submit the same email a second time → "email already in use" error (HTTP 409)
- Submit a malformed email (`notanemail`) → format validation error

---

### Scenario 2 — Login (FR-006, FR-007, FR-017)

1. Log out if currently authenticated
2. Navigate to `http://localhost:8080/cuenta`, select **Iniciar sesión**
3. Enter credentials from Scenario 1 (`test@ejemplo.com` / `password123`)
4. Submit

**Expected**: Authenticated → dashboard shown. Cookie `auth-token` is present with a ~604800 s `Max-Age`.

**Error paths to verify**:
- Wrong password → generic error "Correo electrónico o contraseña incorrectos" (no hint which field is wrong)
- Unknown email → same generic error (no user enumeration)

---

### Scenario 3 — Session Expiry (FR-017)

1. Log in successfully
2. Open DevTools → Application → Cookies → confirm `auth-token` present
3. Manually delete the `auth-token` cookie
4. Refresh the page

**Expected**: Treated as unauthenticated → login/register tabs shown.

---

### Scenario 4 — Password Reset (FR-009 – FR-013)

1. Navigate to the **¿Olvidaste tu contraseña?** link on the login screen
2. Enter a registered email (`test@ejemplo.com`)
3. Submit

**Expected**: Neutral confirmation message regardless of whether the email is registered.

4. Open Mailpit at `http://localhost:8025` → confirm reset email arrived with a link
5. Click the reset link → password reset form
6. Enter a new password (`newPassword456`) and submit

**Expected**: Password updated → user signed in automatically.

7. Try clicking the same reset link again

**Expected**: "Token inválido o expirado" error (FR-011).

8. Try the old password (`password123`) on the login screen

**Expected**: Login fails — new password is now active.

---

### Scenario 5 — Rate Limiting (FR-016)

1. Log out and navigate to the login screen
2. Submit 10 failed login attempts in quick succession (wrong password, same email)

**Expected**: After 10 attempts within the 15-minute window, responses return HTTP 429 with message "Demasiados intentos. Por favor, espera un momento."

---

### Scenario 6 — GDPR Consent (FR-014, FR-015)

1. Inspect the registration form — confirm:
   - A consent checkbox is present
   - The checkbox label links to the privacy policy URL
2. Attempt to submit without checking the box → form blocked (client-side and server-side)
3. Check the box and submit → registration proceeds normally

---

## API Smoke Test (curl)

Quick sanity checks without the UI. See [contracts/api.md](contracts/api.md) for full request/response shapes.

```bash
# Register
curl -s -c cookies.txt -X POST http://localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke@test.com","password":"smokepass1","gdprConsent":true}'

# Login
curl -s -c cookies.txt -X POST http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke@test.com","password":"smokepass1"}'

# Verify session
curl -s -b cookies.txt http://localhost:8081/api/v1/auth/me

# Logout
curl -s -b cookies.txt -c cookies.txt -X POST http://localhost:8081/api/v1/auth/logout
```

---

## Useful URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:8080 |
| Backend API base | http://localhost:8081/api/v1 |
| Mailpit email UI | http://localhost:8025 |
| PostgreSQL | localhost:5432 — DB: `kuky_dev`, user/pass: `kuky` |
