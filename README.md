# Shift Happens

## Project Overview

Shift Happens allows organisations to manage employees, departments, work locations, shift scheduling, leave requests, and shift swaps — all backed by a normalised relational database with enforced referential integrity.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/)

## Installation & Setup

### 1. Clone the repository

```bash
git clone https://github.com/Luke3520/shift_happens.git
cd shift_happens
```

### 2. Configure environment

```bash
cp .env.example .env
```

Fill in your credentials in `.env`. The Docker defaults work out of the box.

### 3. Start the application

```bash
docker compose up
```

This single command will:
- Start a **MySQL 8.0** container with the database `shift_happens`
- Run all SQL init scripts automatically (schema, seed data, views, routines, triggers, events)
- Build the **Spring Boot** application from source
- Start the API on **http://localhost:8080**

The database is exposed on **localhost:3307** (to avoid conflicts with any local MySQL on 3306).

### 4. Verify it works

Once the logs show `Started ShiftHappensApplication`, the API is ready:

```bash
curl http://localhost:8080/employees
```

### 5. Stop the application

```bash
docker compose down
```

To also remove the database volume (full reset):

```bash
docker compose down -v
```

## You can use Make Commands In our project just make sure you have the prerequisites for it

| Command | Description |
|---------|-------------|
| `make up` | Start everything (DB + app) |
| `make db` | Start only the database |
| `make app` | Start only the app (rebuilds jar) |
| `make reset` | Nuke DB volume and restart fresh (re-runs all init scripts) |
| `make down` | Stop everything |
| `make clean` | Stop everything and delete DB data |
| `make db-logs` | Show DB logs |
| `make db-shell` | Connect to MySQL CLI inside the container |

## Connecting to the Database

### CLI

```bash
make db-shell
```

### GUI (MySQL Workbench)

| Setting | Value |
|---------|-------|
| Host | `127.0.0.1` |
| Port | `3307` |
| User | See `DB_USERNAME` in `.env` |
| Password | See `MYSQL_ROOT_PASSWORD` in `.env` |
| Database | See `MYSQL_DATABASE` in `.env` |

A shift scheduling and leave management system built with **Spring Boot 3.5** and **MySQL**.

The long-term vision is to integrate AI-driven schedule generation, but the current focus is on the relational database design and SQL implementation.

---

### User Stories

| Role  | Story |
|-------|-------|
| Admin | Create and manage employees |
| Admin | Create and manage departments and work locations |
| Admin | Build schedules (shifts) for employees in a department |
| Admin | Approve / decline shift assignments |
| Admin | Approve / decline shift swap requests |
| User  | View my assigned schedule |
| User  | Apply for a shift |
| User  | Apply for leave (vacation, sick, parental, etc.) through a unified flow |
| User  | Swap a shift with another employee |
| User  | Check in / check out of a shift |

---

## Database Schema

**17 tables** organised around four domains:

### Organisation
- `department` — company departments
- `work_location` — physical sites
- `user_role` — role-based access (Administrator, Employee, Manager)

### People
- `employee` — core employee record with login credentials and role FK
- `employee_contract` — contract details (type, hours, salary, department)
- `job_role` — certifiable job roles (Nurse, Doctor, Lab Tech, etc.)
- `employee_job_role` — many-to-many linking employees to their qualified roles
- `user_role`- we organise permission here

### Scheduling
- `shift` — a scheduled shift (department, location, time window, status)
- `shift_required_job_role` — what roles a shift needs and how many
- `shift_assignment` — assigns an employee to a shift (with check-in/out)
- `shift_approval` — manager decision on a shift assignment
- `shift_swap` — employee-initiated swap request
- `shift_swap_approval` — manager decision on a swap

### Leave
- `leave_type` — vacation, sick, maternity, etc.
- `leave_request` — employee leave application
- `leave_approval` — manager decision (triggers ledger via DB trigger)
- `leave_ledger` — double-entry style balance tracking (ACCRUALs +, USAGEs −)

### Auditing
- `audit_log` — generic entity change log

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.5 (Java 21) |
| Build Tool | Maven |
| Phase 1 DB | MySQL 8.0 + Spring Data JPA |
| Phase 2 DB | MongoDB + Spring Data MongoDB (Coming soon) |
| Phase 3 DB | Neo4j + Spring Data Neo4j (Coming soon) |
| Unit Testing | JUnit 5 + Mockito (Coming soon) |
| Integration Testing | Testcontainers (Coming soon) |
| Other | Lombok, Docker & Docker Compose |

## API Endpoints

Every entity exposes standard REST endpoints via Spring controllers:

| Resource | Base Path |
|----------|-----------|
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

## Future Work

- **AI-driven scheduling** — auto-generate optimal shift schedules based on employee availability, contracts, roles, and leave
- **Stored procedures** — complex multi-step operations (e.g. bulk schedule creation)
- **Events** — scheduled jobs for leave accrual, audit cleanup
- **Views** — department schedule overview, employee dashboard summary
- **Authentication** — Spring Security integration using the existing `login_password` and `user_role` columns
=======
A shift management system developed throughout our Database and Software Quality course. The project evolves across multiple phases, each introducing a new database technology while keeping Spring Boot as the core framework.