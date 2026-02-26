# Shift Happens

A shift scheduling and leave management system built with **Spring Boot 3.5** and **MySQL**.

The long-term vision is to integrate AI-driven schedule generation, but the current focus is on the relational database design and SQL implementation.

---

## Project Overview

Shift Happens allows organisations to manage employees, departments, work locations, shift scheduling, leave requests, and shift swaps — all backed by a normalised relational database with enforced referential integrity.

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

## Tech Stack

| Layer      | Technology |
|------------|------------|
| Language   | Java 21 |
| Framework  | Spring Boot 3.5 (Web, Data JPA) |
| Database   | MySQL 8+ (InnoDB) |
| ORM        | Hibernate (validate mode — schema managed by SQL scripts) |
| Build      | Maven |
| Misc       | Lombok, spring-dotenv |

---

## Database Schema

**16 tables** organised around four domains:

### Organisation
- `department` — company departments
- `work_location` — physical sites
- `user_role` — role-based access (Administrator, Employee, Manager)

### People
- `employee` — core employee record with login credentials and role FK
- `employee_contract` — contract details (type, hours, salary, department)
- `job_role` — certifiable job roles (Nurse, Doctor, Lab Tech, etc.)
- `employee_job_role` — many-to-many linking employees to their qualified roles

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

## SQL Artefacts

All scripts live in `src/main/resources/queries/`:

| File | Purpose |
|------|---------|
| `create_sql_db.sql` | Full DDL — tables, keys, indexes, constraints |
| `initial_insert.sql` | Populates 100 rows per entity with realistic test data |
| `migration_1_login.sql` | Adds `login_password` column to `employee` |
| `migration_2_user_roles.sql` | Creates `user_role` table, adds FK to `employee`, seeds roles |
| `migration_3_leaveapprovaltrigger.sql` | `BEFORE INSERT` trigger on `leave_approval` — validates manager role, checks leave balance, deducts days via ledger |
| `migration_4_stored_functions.sql` | Stored functions: `fn_leave_balance`, `fn_count_shift_assignments`, `fn_scheduled_hours_in_period`, `fn_is_employee_on_leave` |

### Indexes

Defined in `create_sql_db.sql`:

- `idx_employee_email` — fast employee lookup by email
- `idx_shift_datetime` — range queries on shift time windows
- `idx_leave_employee` — leave requests by employee
- `idx_leave_ledger_emp_type` — ledger balance lookups by employee + leave type
- Unique constraints on `shift_assignment(shift_id, employee_id)`, `employee_job_role(employee_id, job_role_id)`, `shift_required_job_role(shift_id, job_role_id)`

### Trigger

`trg_leaveapproval_before_insert` (migration 3):
1. Validates the leave request exists and is `PENDING`
2. Validates the approver has role = Manager
3. On `APPROVED`: checks balance via `leave_ledger`, deducts days, updates request status

### Stored Functions (migration 4)

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `fn_leave_balance` | `(employee_id, leave_type_id)` | `DECIMAL(6,2)` | Net leave balance from the ledger |
| `fn_count_shift_assignments` | `(employee_id, status)` | `INT` | Count of shift assignments, optionally filtered by status (pass `NULL` for all) |
| `fn_scheduled_hours_in_period` | `(employee_id, start_date, end_date)` | `DECIMAL(10,2)` | Total scheduled hours in a date range (non-cancelled shifts) |
| `fn_is_employee_on_leave` | `(employee_id, check_date)` | `TINYINT` | Returns 1 if employee has approved leave covering the given date |

---

## Installation

### Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8.0+

### Database Setup

1. Create a MySQL database:
   ```sql
   CREATE DATABASE shift_happens;
   USE shift_happens;
   ```

2. Run the scripts **in order**:
   ```bash
   mysql -u <user> -p shift_happens < src/main/resources/queries/create_sql_db.sql
   mysql -u <user> -p shift_happens < src/main/resources/queries/initial_insert.sql
   mysql -u <user> -p shift_happens < src/main/resources/queries/migration_1_login.sql
   mysql -u <user> -p shift_happens < src/main/resources/queries/migration_2_user_roles.sql
   mysql -u <user> -p shift_happens < src/main/resources/queries/migration_3_leaveapprovaltrigger.sql
   mysql -u <user> -p shift_happens < src/main/resources/queries/migration_4_stored_functions.sql
   ```

3. Create a `.env` file in the project root:
   ```
   DB_URL=jdbc:mysql://localhost:3306/shift_happens
   DB_USERNAME=your_user
   DB_PASSWORD=your_password
   ```

### Run the Application

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

---

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
