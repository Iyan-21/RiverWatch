# RiverWatch Cebu — Increment 1: User Accounts and Authentication

Stack (per SRS): Spring Boot 3 (Java 17) · ReactJS (Vite) · PostgreSQL · JWT + RBAC.
Implements SRS FR-001 to FR-005 and NFR-S01, S03, S04, S05, S08.

## Run it

Needs: JDK 17+, Maven, Node 20+, PostgreSQL 16 (or Docker).

```bash
# 1. Database + backend
cd backend
cp .env.example .env            # edit DB_PASSWORD, JWT_SECRET (32+ chars), ADMIN_EMAIL, ADMIN_PASSWORD
docker compose up -d            # or use your own PostgreSQL and match DB_* in .env
mvn spring-boot:run             # Flyway creates the tables and the 5 sample barangays; API on :8080

# 2. Frontend (new terminal)
cd frontend
npm install
npm run dev                     # http://localhost:5173  (Vite proxies /api to :8080)
```

## Demo script (about 3 minutes)
1. Open `/register/resident`, submit empty form (field errors), then register a resident. Try the same email again (duplicate rejected).
2. Log in with a wrong password (error), then correctly → lands on the **Resident** dashboard. Menu: Dashboard, My profile.
3. Register a BDRRM officer at `/register/officer`, try to log in → "waiting for administrator approval" (status PENDING).
4. Log in as the admin from `.env` → **Admin** dashboard → *Officer approvals* → Approve.
5. Log in as the officer → **Officer** dashboard → *Residents* (only residents of the officer's own barangay).
6. While logged in as the resident, type `/app/admin` in the URL → redirected to the 403 page. `curl` the API with the resident token → HTTP 403.
7. Log out → the token stops working immediately (session is revoked in the database).
8. Enter 5 wrong passwords for one account → locked for 15 minutes.

## How the requirements are met
| Requirement | Where |
|---|---|
| FR-001 resident registration, validation, duplicate email blocked | `AuthService.register`, `RegisterRequest`, unique index on `lower(email)` |
| FR-002 officer registration with PENDING status | `AuthService.register(Role.OFFICER)` |
| FR-003 admin approves/rejects officers, audited | `AdminController`, `audit_log` |
| FR-004 login, time-limited token, logout | `AuthService.login/refresh/logout`; access token 30 min, refresh cookie 7 days (HttpOnly, rotated) |
| FR-005 RBAC, own-barangay scope, 403 otherwise | `SecurityConfig` URL rules + `DashboardController` barangay-scoped queries; frontend `RouteGuards.jsx` |
| NFR-S01 BCrypt cost 10 | `SecurityConfig.passwordEncoder` |
| NFR-S04 lock after 5 failures for 15 min | `AuthService.login` |
| NFR-S08 no secrets in repo | `.env` (git-ignored), `application.yml` reads environment |
| Roles | `RESIDENT`, `OFFICER`, `ADMIN` in `user_account.role` (CHECK constraint) |

Design notes: the access token is held only in browser memory; the refresh token is an HttpOnly, SameSite=Strict cookie stored hashed (SHA-256) in `user_session`. Logout revokes the session row, and every request checks that row, so a copied access token dies at logout. Unknown email and wrong password give the same message.

## Known limits
- Sample barangays and coordinates in `V1__auth_schema.sql` are placeholders until the pilot barangays are confirmed (SRS Appendix F).
- Password reset (FR-006, Should) and the household, alert and check-in features belong to later increments.
- No automated tests yet. Authorization test suite (NFR-S05) is the next addition.
## Increment 2: User Profile and Password Management

New in this increment (SRS FR-006, FR-011, NFR-S01, NFR-S03):

| Feature | Frontend | Backend endpoint |
|---|---|---|
| View and edit own profile (name, email, mobile) | `/app/profile` | `GET /api/v1/profile`, `PUT /api/v1/profile` |
| Change password (current password required) | `/app/change-password` | `POST /api/v1/auth/change-password` |
| Forgot password (email link) | `/forgot-password` | `POST /api/v1/auth/forgot-password` |
| Reset password with the emailed link | `/reset-password?token=...` | `GET /api/v1/auth/reset-password/validate`, `POST /api/v1/auth/reset-password` |

How it works
- **Profile:** the server edits only the account in the verified login token. No user id is accepted from the request, so one user cannot modify another user's profile. Email must stay unique.
- **Change password:** the current password is checked, the new one must be 8 to 72 characters, confirmed, and different from the old one. The new password is stored as a BCrypt hash and all other logins of that user are ended. The old password no longer works.
- **Forgot / reset password:** the system emails a link with a random 256-bit token. Only the SHA-256 hash of the token is stored (`password_reset_token`). The link expires after 30 minutes and works once; requesting a new link cancels older ones; a new request within 60 seconds is ignored. The response is the same whether or not the email exists. After a reset, all existing logins are ended and any lock on the account is cleared.
- Database: Flyway migration `V2__password_reset_and_rls.sql` adds `password_reset_token` and turns on row level security for all tables (this also blocks Supabase's public REST API).

### Email setup for password reset
Add to `backend/.env` (see `.env.example`):
```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<your gmail>
MAIL_PASSWORD=<Google App password>
MAIL_FROM=<your gmail>
APP_BASE_URL=http://localhost:5173
```
For a Gmail App password: Google Account → Security → 2-Step Verification → App passwords.
If `MAIL_HOST` is empty, the reset link is only printed in the backend console (development only).

### Demo script for Increment 2
1. Log in, open **My profile**, change the name and mobile number, save. Try an email that is already used (rejected) and an invalid mobile number.
2. Open **Change password**: wrong current password (error), mismatched confirmation (error), then a valid change. Log out and log in with the new password; the old one is rejected.
3. On the login page click **Forgot password?**, enter your email, open the emailed link, set a new password, and log in with it.
4. Open the same link again: it is rejected (single use). A link older than 30 minutes is rejected too.
