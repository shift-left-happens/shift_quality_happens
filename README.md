# Shift Happens

A Spring Boot + MySQL shift scheduling and leave management system, with a Vite + React frontend.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 3.5 (Java 21) |
| Build | Maven |
| Database | MySQL 8.0 + Spring Data JPA |
| Auth | Spring Security + JWT (HMAC-SHA256) |
| Frontend | Vite + React 18 + TypeScript + Tailwind |
| Other | Lombok, Docker, Docker Compose |

---

## Installation & Setup

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/)
- Java 21 (only needed if running the app outside Docker)

### 1. Clone the repository

```bash
git clone https://github.com/Luke3520/shift_happens.git
cd shift_happens
```

### 2. Configure environment

```bash
cp .env.example .env
```

The `.env.example` file contains working defaults for local development — no changes required to get started.

### 3. Start everything with Docker Compose

```bash
docker compose up app
```

This starts MySQL and the Spring Boot application. The database is initialized automatically from the SQL scripts in `docker/init/`.

The API will be available at **http://localhost:8080** once you see:
```
Started ShiftHappensApplication in X.XXX seconds
```

To stop:
```bash
docker compose down
```

To wipe data and start fresh:
```bash
docker compose down -v
docker compose up app
```

---

## Make Commands (optional shortcut — requires `make`)

If you have `make` installed, these wrap the docker/maven commands above:

| Command | Equivalent |
|---------|------------|
| `make run-all` | Start MySQL + run app locally (outside Docker) |
| `make run-dbs` | `docker compose up -d --wait db` |
| `make run-app` | `./mvnw spring-boot:run` |
| `make reset` | `docker compose down -v && docker compose up -d --wait db` |
| `make down` | `docker compose down` |
| `make clean` | `docker compose down -v` |
| `make db-shell` | Open MySQL CLI inside the container |

---

## Security

### SQL Injection Prevention
All database access goes through Spring Data JPA repositories (`JpaRepository`) and named JPQL parameters. Queries are never built by string concatenation — the framework compiles them into parameterized prepared statements, so user input cannot alter query structure.

### Database Users & Privileges
The application connects as `app_user`, a least-privilege account defined in [`src/main/resources/db/mysql/migrations/v6_users_privileges.sql`](src/main/resources/db/mysql/migrations/v6_users_privileges.sql). It holds:
- **`SELECT` only** on `audit_log` (no writes)
- **`SELECT, INSERT` only** on `leave_ledger` (double-entry ledger, no updates/deletes)
- **Full CRUD** on all other operational tables
- No `DROP`, `ALTER`, `CREATE`, or `GRANT` permissions

In Docker, the user is created at startup by [`docker/init/09-create-app-user.sh`](docker/init/09-create-app-user.sh) using credentials from `.env`.

---

## Authentication

All endpoints except `POST /auth/login` require a JWT bearer token.

**Login:**
```
POST /auth/login
Content-Type: application/json

{ "email": "user@example.com", "password": "yourpassword" }
```

**Response:**
```json
{ "token": "eyJ...", "employeeId": 1, "role": "ADMINISTRATOR", ... }
```

**Using the token:**
```
Authorization: Bearer <token>
```

**Role-based access:**
- `GET /**` — any authenticated user
- `POST / PUT / PATCH / DELETE /**` — ADMINISTRATOR or MANAGER only
- `GET /audit-log/**` — ADMINISTRATOR only

---

## API Endpoints

| Resource | Path |
|----------|------|
| Auth | `POST /auth/login` (public) |
| Employees | `/employees` |
| Departments | `/departments` |
| Work Locations | `/work-locations` |
| Shifts | `/shifts` |
| Shift Assignments | `/shift-assignments` |
| Shift Approvals | `/shift-approvals` |
| Shift Swaps | `/shift-swaps` |
| Shift Swap Approvals | `/shift-swap-approvals` |
| Leave Types | `/leave-types` |
| Leave Requests | `/leave-requests` |
| Leave Approvals | `/leave-approvals` |
| Leave Ledger | `/leave-ledger` |
| Job Roles | `/job-roles` |
| Employee Job Roles | `/employee-job-roles` |
| Employee Contracts | `/employee-contracts` |
| User Roles | `/user-roles` |
| Audit Log | `/audit-log` |

---

## Backup & Restore

Take a snapshot of the live database:

```bash
make run-dbs   # ensure MySQL is running
make backup    # writes backups/<timestamp>/mysql.sql
```

Restore from a backup:

```bash
make reset
make restore BACKUP=backups/<timestamp>
```

Quick verify (employee row count):

```bash
make verify
```

---

## Database Schema

**18 tables** across four domains:

**Organisation:** `department`, `work_location`, `user_role`

**People:** `employee`, `employee_contract`, `job_role`, `employee_job_role`

**Scheduling:** `shift`, `shift_required_job_role`, `shift_assignment`, `shift_approval`, `shift_swap`, `shift_swap_approval`

**Leave:** `leave_type`, `leave_request`, `leave_approval`, `leave_ledger`

**Auditing:** `audit_log`

Notable constraints:
- `leave_approval` inserts are validated by a DB trigger that checks manager role and leave balance
- `leave_ledger` tracks balances double-entry style (ACCRUALs +, USAGEs −)
- Composite PKs on `employee_job_role` and `shift_required_job_role` use `@IdClass`

---

## Connecting to the Database

**MySQL CLI:**
```bash
make db-shell
# or
docker exec -it shift-quality-happens-db mysql -u root -prootpassword shift_happens
```

**MySQL GUI (Workbench, DBeaver, etc.):**

| Setting | Value |
|---------|-------|
| Host | `127.0.0.1` |
| Port | `3308` |
| User | `root` |
| Password | `MYSQL_ROOT_PASSWORD` from `.env` |
| Database | `shift_happens` |

---

## User Stories

| Role | Story |
|------|-------|
| Admin | Create and manage employees, departments, work locations |
| Admin | Build schedules and approve/decline shift assignments |
| Admin | Approve/decline shift swap requests |
| Manager | Approve/decline leave requests |
| Employee | View assigned schedule |
| Employee | Apply for a shift or leave |
| Employee | Request a shift swap with another employee |
