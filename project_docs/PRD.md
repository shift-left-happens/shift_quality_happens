# PRD — Shift Happens Frontend

## 1. Overview

A web frontend for the **Shift Happens** shift-scheduling and leave-management system. It consumes the existing Spring Boot REST API (`localhost:8080`) and provides role-aware dashboards for **Employees**, **Managers**, and **Administrators**.

| Item | Detail |
|------|--------|
| Stack | **Vite + React 18 + TypeScript** (SPA) |
| Styling | **Tailwind CSS 4** |
| Server | **Express** dev-proxy / lightweight production server |
| Location | `shift_happens/frontend/` |
| API Target | Spring Boot backend on `http://localhost:8080` |

---

## 2. Goals & Non-Goals

### Goals
- Provide a usable UI that exercises every existing API endpoint.
- Role-based views: what an Employee sees differs from a Manager or Admin.
- Enable full CRUD workflows for the entities that already support it (employees) and read-only views for the rest until the backend catches up.
- Clean, responsive layout—works on desktop and tablet.
- Easy to run alongside the existing `docker-compose` setup.

### Non-Goals (for now)
- Authentication / JWT — the backend has no auth yet. We'll use a **role-switcher** dropdown to simulate logged-in users until Spring Security is added.
- Real-time updates (WebSockets).
- Mobile-first / PWA.
- AI schedule generation UI (future phase).

---

## 3. User Roles

| Role ID | Name | Capabilities |
|---------|------|-------------|
| 1 | **Administrator** | Full CRUD on all entities. Access to audit log. Manage departments, locations, job roles, user roles. |
| 2 | **Employee** | View own schedule, request leave, request shift swaps, check in/out. |
| 3 | **Manager** | Everything an Employee can do + approve/reject shift assignments, shift swaps, and leave requests for their department. |

---

## 4. Pages & Features

### 4.1 Global Shell
- **Top navbar**: App logo / name, role-switcher (simulated login), current user display.
- **Sidebar nav**: Links filtered by active role.
- **Toast notifications** for success/error on mutations.

### 4.2 Dashboard (`/`)
| Role | Content |
|------|---------|
| Employee | My upcoming shifts, leave balance summary, pending requests. |
| Manager | Pending approvals count, department schedule at a glance, team headcount. |
| Admin | System-wide stats: total employees, departments, open shifts, pending items. |

### 4.3 Employee Pages
| Route | Description | API Endpoints |
|-------|-------------|---------------|
| `/employees` | Searchable, paginated table of all employees. Admin only. | `GET /employees` |
| `/employees/:id` | Employee detail / edit form (admin can edit, others read-only). | `GET /employees/:id`, `PATCH /employees/:id` |
| `/employees/new` | Create employee form. Admin only. | `POST /employees` |

### 4.4 Schedule / Shifts
| Route | Description | API Endpoints |
|-------|-------------|---------------|
| `/shifts` | Weekly calendar view of shifts. Filterable by department & location. | `GET /shifts` |
| `/shifts/:id` | Shift detail — assigned employees, required job roles, status. | `GET /shifts`, `GET /shift-assignments`, `GET /shift-required-job-roles` |
| `/my-schedule` | Employee's own assigned shifts (calendar + list). | `GET /shift-assignments` (filtered client-side by employee) |

### 4.5 Shift Assignments & Approvals
| Route | Description | API Endpoints |
|-------|-------------|---------------|
| `/shift-assignments` | List of all assignments. Manager/Admin. | `GET /shift-assignments` |
| `/shift-approvals` | Pending shift approvals queue. Manager view — approve/decline buttons. | `GET /shift-approvals` |

### 4.6 Shift Swaps
| Route | Description | API Endpoints |
|-------|-------------|---------------|
| `/shift-swaps` | List swap requests. Employee can create; Manager sees pending. | `GET /shift-swaps` |
| `/shift-swap-approvals` | Manager approval queue for swaps. | `GET /shift-swap-approvals` |

### 4.7 Leave Management
| Route | Description | API Endpoints |
|-------|-------------|---------------|
| `/leave/request` | Employee submits a leave request (date range, leave type, reason). | `POST /leave-requests` (once backend supports it; form-only for now) |
| `/leave/my-requests` | Employee's own leave requests + status. | `GET /leave-requests` (filtered) |
| `/leave/approvals` | Manager queue — approve / reject pending leave requests. | `GET /leave-approvals`, view data from `vw_pending_leave_requests` |
| `/leave/balances` | Employee sees own balance; Manager sees team balances. | `GET /leave-ledger` (aggregated client-side) |

### 4.8 Organisation (Admin)
| Route | Description | API Endpoints |
|-------|-------------|---------------|
| `/departments` | CRUD departments. | `GET /departments` |
| `/work-locations` | CRUD work locations. | `GET /work-locations` |
| `/job-roles` | CRUD job roles. | `GET /job-roles` |
| `/user-roles` | View user roles. | `GET /user-roles` |
| `/employee-contracts` | View / manage contracts. | `GET /employee-contracts` |
| `/employee-job-roles` | Assign job roles to employees. | `GET /employee-job-roles` |

### 4.9 Audit Log (Admin)
| Route | Description | API Endpoints |
|-------|-------------|---------------|
| `/audit-log` | Filterable table of all audit entries. | `GET /audit-log` |

### 4.10 Views (Read-Only Reports)
| Route | Description | Backend View |
|-------|-------------|--------------|
| `/reports/employee-overview` | Employee overview with department, contract, location. | `vw_employee_overview` |
| `/reports/shift-schedule` | Full shift schedule with assignments. | `vw_shift_schedule` |
| `/reports/leave-balances` | Leave balances per employee per type. | `vw_leave_balance` |
| `/reports/pending-leave` | Pending leave requests detail. | `vw_pending_leave_requests` |
| `/reports/employee-leave` | Employee leave history. | `vw_employee_leave_overview` |
| `/reports/employee-shifts` | Employee shift history. | `vw_employee_shift_overview` |

---

## 5. Tech Architecture

```
shift_happens/
├── frontend/
│   ├── package.json
│   ├── vite.config.ts          # dev server + proxy to :8080
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   ├── server.ts               # Express production server (serves dist + proxies API)
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── api/                 # Typed fetch wrappers per resource
│       │   ├── client.ts        # Base fetch with error handling
│       │   ├── employees.ts
│       │   ├── shifts.ts
│       │   ├── leave.ts
│       │   └── ...
│       ├── components/          # Shared UI components
│       │   ├── Layout.tsx       # Shell (navbar + sidebar + outlet)
│       │   ├── DataTable.tsx    # Reusable sortable/filterable table
│       │   ├── Calendar.tsx     # Weekly shift calendar
│       │   ├── Modal.tsx
│       │   ├── Toast.tsx
│       │   └── RoleSwitcher.tsx
│       ├── pages/               # One file/folder per route
│       │   ├── Dashboard.tsx
│       │   ├── employees/
│       │   ├── shifts/
│       │   ├── leave/
│       │   ├── organisation/
│       │   ├── reports/
│       │   └── audit/
│       ├── hooks/               # Custom hooks (useApi, useRole, etc.)
│       ├── context/             # RoleContext (simulated auth)
│       ├── types/               # TypeScript interfaces matching backend entities
│       └── utils/               # Date formatting, constants, helpers
├── src/                         # (existing Spring Boot backend)
├── docker-compose.yml
└── ...
```

### 5.1 Dev Proxy

Vite's dev server proxies `/api/**` → `http://localhost:8080/**` so the frontend and backend run on different ports without CORS issues during development.

```ts
// vite.config.ts (excerpt)
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      rewrite: (path) => path.replace(/^\/api/, ''),
      changeOrigin: true,
    },
  },
}
```

### 5.2 Express Production Server

A minimal Express server that:
1. Serves the Vite production build from `dist/`.
2. Proxies `/api/*` requests to the Spring Boot backend.
3. Falls back to `index.html` for client-side routing.

### 5.3 Key Libraries

| Library | Purpose |
|---------|---------|
| `react-router-dom` v7 | Client-side routing |
| `@tanstack/react-query` | Data fetching, caching, mutations |
| `tailwindcss` | Utility-first CSS |
| `lucide-react` | Icons |
| `date-fns` | Date formatting & manipulation |
| `react-hot-toast` | Toast notifications |

---

## 6. TypeScript Types (matching backend entities)

```ts
// types/employee.ts
interface Employee {
  employeeId: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  loginPassword?: string;
  fkUserRoleId: number;
  phoneNumber: string;
  hireDate: string;          // ISO date
  employmentStatus: string;
  primaryWorkLocationId: number;
}

// types/shift.ts
interface Shift {
  shiftId: number;
  departmentId: number;
  workLocationId: number;
  shiftName: string;
  startDatetime: string;     // ISO datetime
  endDatetime: string;
  shiftStatus: string;
}

// types/leave.ts
interface LeaveRequest {
  leaveRequestId: number;
  employeeId: number;
  leaveTypeId: number;
  startDate: string;
  endDate: string;
  requestStatus: string;
  reason: string;
  requestedDatetime: string;
}

// ... (one interface per entity)
```

---

## 7. Phased Delivery

### Phase 1 — Foundation (MVP)
- [x] Project scaffolding (Vite + React + TS + Tailwind + Express)
- [ ] Layout shell (navbar, sidebar, role switcher)
- [ ] Dashboard (basic stats)
- [ ] Employees list + detail + create/edit
- [ ] Departments & Work Locations list (read-only)
- [ ] API client layer with React Query

### Phase 2 — Scheduling
- [ ] Shifts list + calendar view
- [ ] Shift assignments list
- [ ] My Schedule page (employee view)
- [ ] Shift approvals queue (manager)

### Phase 3 — Leave
- [ ] Leave request form
- [ ] My leave requests page
- [ ] Leave approvals queue (manager)
- [ ] Leave balance display

### Phase 4 — Swaps & Admin
- [ ] Shift swap request + approval flow
- [ ] Organisation pages (job roles, contracts, employee job roles)
- [ ] Audit log viewer
- [ ] Report pages (DB views)

### Phase 5 — Polish & Auth
- [ ] Real authentication (once Spring Security is added)
- [ ] Loading skeletons, empty states, error boundaries
- [ ] Responsive refinements
- [ ] Dark mode toggle
- [ ] Docker integration (add `frontend` service to `docker-compose.yml`)

---

## 8. API Gaps / Backend Work Needed

The current backend is mostly read-only (`@GetMapping` on all controllers). Only `EmployeeController` has `POST` and `PATCH`. For the frontend to be fully functional, the backend needs:

| Entity | Missing | Priority |
|--------|---------|----------|
| All entities (except Employee) | `POST`, `PUT/PATCH`, `DELETE` | High |
| Shift Assignments | `POST` via stored procedure `sp_assign_employee_to_shift` | High |
| Leave Requests | `POST` via stored procedure `sp_submit_leave_request` | High |
| Leave Approvals | `POST` via stored procedure `approve_leave_request` | High |
| Shift Swaps | `POST` (create swap request) | Medium |
| Shift Swap Approvals | `POST` (approve/reject) | Medium |
| Shift Approvals | `POST` (approve/reject) | Medium |
| DB Views | Dedicated endpoints (e.g. `GET /views/employee-overview`) | Medium |
| Employee login | `POST /auth/login` (once Spring Security exists) | Phase 5 |

> **The frontend will build optimistic UI for all forms now.** When a `POST`/`PATCH` endpoint doesn't exist yet, the form will show a "Backend not yet implemented" toast instead of crashing.

---

## 9. Running the Frontend

```bash
# Development
cd frontend
npm install
npm run dev          # Vite on :3000, proxies API to :8080

# Production build
npm run build        # outputs to frontend/dist/
npm run serve        # Express serves dist + proxies API
```

### Future Docker Integration

```yaml
# Added to docker-compose.yml
frontend:
  build: ./frontend
  container_name: shift-happens-frontend
  ports:
    - "3000:3000"
  environment:
    API_URL: http://app:8080
  depends_on:
    - app
```

---

## 10. Design Principles

1. **Data tables are king** — most pages are tables with sort, filter, search. Build one great `<DataTable>` and reuse it everywhere.
2. **Fail gracefully** — if the backend returns 404/500 or an endpoint doesn't exist, show a clear message, never a blank screen.
3. **Minimal state** — React Query handles server state. Local state only for UI concerns (modals, form drafts).
4. **Convention over configuration** — one API file per resource, one page folder per route, consistent naming.
5. **Ship incrementally** — every phase is independently deployable and useful.
