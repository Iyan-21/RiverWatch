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
