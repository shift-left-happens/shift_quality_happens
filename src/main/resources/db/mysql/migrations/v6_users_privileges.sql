-- ============================================================
-- v6: Application user, role, and privilege definitions
-- ============================================================
-- This script is the human-readable artifact showing the
-- privilege model. In Docker the actual user is created by
-- docker/init/09-create-app-user.sh using env-var credentials.
-- Run this manually when setting up a non-Docker environment.
-- ============================================================

USE shift_happens;

-- ── Role ─────────────────────────────────────────────────────
CREATE ROLE IF NOT EXISTS app_crud_role;

GRANT USAGE    ON shift_happens.*                        TO app_crud_role;
GRANT SHOW VIEW ON shift_happens.*                       TO app_crud_role;

-- Audit log: read-only (no writes by the application)
GRANT SELECT                                ON shift_happens.audit_log                  TO app_crud_role;

-- Leave ledger: insert + select only (double-entry, no updates/deletes)
GRANT SELECT, INSERT                        ON shift_happens.leave_ledger               TO app_crud_role;

-- All other operational tables: full CRUD
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.department                 TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.work_location              TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.user_role                  TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.employee                   TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.employee_contract          TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.job_role                   TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.employee_job_role          TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.shift                      TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.shift_required_job_role    TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.shift_assignment           TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.shift_approval             TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.shift_swap                 TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.shift_swap_approval        TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.leave_type                 TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.leave_request              TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.leave_approval             TO app_crud_role;

-- Views
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.vw_employee_leave_overview TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE        ON shift_happens.vw_employee_shift_overview TO app_crud_role;

-- ── User ─────────────────────────────────────────────────────
-- Replace the password below with the value of APP_DB_PASSWORD from your .env
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY 'change_me_in_production';
ALTER  USER              'app_user'@'%' IDENTIFIED BY 'change_me_in_production';

GRANT app_crud_role TO 'app_user'@'%';
SET DEFAULT ROLE app_crud_role TO 'app_user'@'%';

FLUSH PRIVILEGES;
