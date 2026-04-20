# Shift Happens — Frontend PRD

## 1. Goal

Build a web frontend for the existing Spring Boot backend (`../shift_happens`) so users can log in and manage the core shift-scheduling / leave-management workflows through a browser instead of `curl`.

## 2. Stack

| Concern | Choice |
|---|---|
| Framework | React 18 |
| Build tool | Vite |
| Language | TypeScript (strict) |
| Routing | React Router v6 |
| HTTP | `fetch` wrapped in a small `apiClient` |
| State | React Context for auth; component-local state otherwise (no Redux/Zustand for v1) |
| Styling | Plain CSS modules (upgrade to Tailwind later if desired) |
| Forms | Controlled inputs; no form library for v1 |
| Package manager | `npm` |

Rationale: minimal dependencies, course-project appropriate, easy to reason about. Can grow into React Query / Tailwind / Zod later without rewrites.

## 3. Scope

### In scope (v1)

**Auth**
- Login page (email + password → `POST /auth/login`)
- JWT stored in `localStorage`; attached to every API request as `Authorization: Bearer <token>`
- Logout (clear token, redirect to login)
- Protected routes: any non-`/login` route requires a valid token; otherwise redirect to `/login`

**Entities (first slice)**
1. **Employees** — list, view, create, edit (admin/manager)
2. **Departments** — list, view, create, edit
3. **Shifts** — list, view, create, edit
4. **Shift Assignments** — list (filter by employee + by shift), create
5. **Leave Requests** — list (own + all for admin/manager), create

Read-only dependencies pulled in for dropdowns/labels: `work-locations`, `leave-types`, `user-roles`, `job-roles`.

**Role-aware UI**
- Role (from JWT login response) drives which pages + buttons are visible
- `ADMINISTRATOR` / `MANAGER`: full CRUD on the 5 entities above
- `EMPLOYEE`: read-only views + "My Schedule" page showing only their own shift assignments

### Out of scope (v1 — deferred)

- Shift swaps, shift approvals, shift-swap approvals, leave approvals, leave ledger
- Audit log viewer
- MongoDB / Neo4j endpoint variants
- Employee self-service writes (apply for leave, apply for shift, check in/out) — blocked by backend authorization rules; revisit once backend adds targeted exceptions
- Password reset, user registration
- Pagination / search / advanced filtering (simple client-side filter only)
- Responsive / mobile design (desktop-first; usable on mobile but not polished)
- Dark mode, i18n, accessibility audit
- Unit/integration tests (manual verification only in v1)

## 4. Known backend constraints

- **No CORS config on backend** → use Vite dev server proxy (`/api` → `http://localhost:8080`) during development. No backend change needed.
- **Write endpoints require ADMIN/MANAGER** (`SecurityConfig`) → employee self-service is read-only in v1.
- **Two DB variants exist** (MySQL vs Mongo/Neo4j). Frontend targets MySQL endpoints only (`/employees`, not `/employees/mongo`).

## 5. Pages & routes

| Route | Page | Access |
|---|---|---|
| `/login` | Login form | public |
| `/` | Dashboard (summary cards, quick links) | any auth |
| `/my-schedule` | Current user's upcoming shift assignments | any auth |
| `/employees` | Employee list | any auth; create/edit buttons hidden for EMPLOYEE |
| `/employees/:id` | Employee detail + edit | any auth |
| `/employees/new` | Create employee | ADMIN/MANAGER only |
| `/departments` | Department list + create/edit | any auth (read); ADMIN/MANAGER (write) |
| `/shifts` | Shift list | any auth |
| `/shifts/:id` | Shift detail + assignments | any auth |
| `/shifts/new` | Create shift | ADMIN/MANAGER |
| `/shift-assignments` | All assignments | any auth |
| `/leave-requests` | Leave request list | any auth |
| `/leave-requests/new` | Create leave request | ADMIN/MANAGER (until backend exception lands) |
| `*` | 404 | — |

Top-level layout: left sidebar nav + top bar with user name + logout. Nav items filtered by role.

## 6. Architecture

```
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts            # includes /api proxy to :8080
├── .env.example              # VITE_API_BASE_URL
└── src/
    ├── main.tsx
    ├── App.tsx                # router + AuthProvider
    ├── auth/
    │   ├── AuthContext.tsx    # { user, token, login, logout }
    │   ├── ProtectedRoute.tsx # redirects to /login if no token
    │   └── useAuth.ts
    ├── api/
    │   ├── client.ts          # fetch wrapper; injects Bearer token; handles 401
    │   ├── employees.ts
    │   ├── departments.ts
    │   ├── shifts.ts
    │   ├── shiftAssignments.ts
    │   ├── leaveRequests.ts
    │   └── types.ts           # DTO types mirroring backend
    ├── pages/
    │   ├── LoginPage.tsx
    │   ├── DashboardPage.tsx
    │   ├── MySchedulePage.tsx
    │   ├── employees/
    │   ├── departments/
    │   ├── shifts/
    │   ├── shiftAssignments/
    │   └── leaveRequests/
    ├── components/
    │   ├── Layout.tsx         # sidebar + topbar shell
    │   ├── NavSidebar.tsx
    │   ├── DataTable.tsx      # shared list component
    │   └── FormField.tsx
    └── styles/
        └── globals.css
```

### Auth flow

1. User submits login form → `POST /auth/login`
2. On success, store `{ token, employeeId, firstName, lastName, email, roleName, ... }` in Context + `localStorage`
3. `apiClient` reads token from Context and sets `Authorization: Bearer <token>`
4. On any `401` response, `apiClient` clears auth and redirects to `/login`
5. On app load, hydrate auth from `localStorage` (no validation call in v1 — trust until first 401)

## 7. Milestones (build order)

1. **Scaffold** — `npm create vite@latest`, TS strict, router, base folder structure, Vite proxy, `.env.example`
2. **Auth** — `AuthContext`, `LoginPage`, `ProtectedRoute`, `apiClient` with Bearer + 401 handling
3. **Layout** — sidebar + topbar + role-filtered nav
4. **Employees** — list + detail + create/edit (validates the full CRUD pattern; everything after reuses it)
5. **Departments** — CRUD (thin; ~1hr once pattern exists)
6. **Shifts** — list + create + detail (needs department + work-location dropdowns)
7. **Shift Assignments** — list + create (needs employee + shift dropdowns); `MySchedulePage` filters by current user
8. **Leave Requests** — list + create (needs employee + leave-type dropdowns)
9. **Dashboard** — basic summary page (counts + quick links)
10. **Polish pass** — empty states, loading spinners, error toasts, consistent styling

Each milestone is independently testable against a running backend.

## 8. Non-goals / explicit "no"s

- No SSR, no Next.js
- No component library (MUI/Chakra/Ant) in v1 — keep dependency count low
- No GraphQL / tRPC — plain REST
- No offline support, no service workers
- No analytics, no error reporting (Sentry etc.)

## 9. Open questions

- Do we want a "seed credentials" section in the README for testing? (e.g. which admin account to log in with from the seed data)
- Should logout also hit a backend endpoint, or purely client-side token discard? (JWT is stateless so purely client-side is fine)
- After v1 ships, priority: shift swaps vs. approvals vs. employee self-service unlock?
