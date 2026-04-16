#!/bin/bash
set -e

echo "Initializing app user and role..."

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is not set}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE is not set}"
: "${APP_DB_USER:?APP_DB_USER is not set}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD is not set}"

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL

USE \`${MYSQL_DATABASE}\`;

-- create role safely
CREATE ROLE IF NOT EXISTS app_crud_role;

-- schema visibility
GRANT USAGE ON \`${MYSQL_DATABASE}\`.* TO app_crud_role;
GRANT SHOW VIEW ON \`${MYSQL_DATABASE}\`.* TO app_crud_role;

-- table permissions
GRANT SELECT ON \`${MYSQL_DATABASE}\`.audit_log TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.employee TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.department TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.employee_contract TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.employee_job_role TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.job_role TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.leave_approval TO app_crud_role;
GRANT SELECT, INSERT ON \`${MYSQL_DATABASE}\`.leave_ledger TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.leave_request TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.leave_type TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.shift TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.shift_approval TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.shift_assignment TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.shift_required_job_role TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.shift_swap TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.shift_swap_approval TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.user_role TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.work_location TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.vw_employee_leave_overview TO app_crud_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.vw_employee_shift_overview TO app_crud_role;

-- create user if not exists
CREATE USER IF NOT EXISTS '${APP_DB_USER}'@'%' IDENTIFIED BY '${APP_DB_PASSWORD}';

-- ensure password is always correct (idempotent)
ALTER USER '${APP_DB_USER}'@'%' IDENTIFIED BY '${APP_DB_PASSWORD}';

-- assign role
GRANT app_crud_role TO '${APP_DB_USER}'@'%';

-- default role
SET DEFAULT ROLE app_crud_role TO '${APP_DB_USER}'@'%';

FLUSH PRIVILEGES;

EOSQL

echo "App user and role created."