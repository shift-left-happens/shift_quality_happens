USE shift_happens;
START TRANSACTION;
SET FOREIGN_KEY_CHECKS = 0;
SET @entities_to_generate = 100;

-- =========================================
-- DEPARTMENTS (100)
-- =========================================
INSERT INTO department (department_name, is_active)
WITH RECURSIVE seq AS (
    SELECT 1 n UNION ALL SELECT n+1 FROM seq WHERE n < @entities_to_generate
)
SELECT CONCAT('department ', n), IF(RAND()>0.05,1,0)
FROM seq;

-- =========================================
-- WORK LOCATIONS (100)
-- =========================================
INSERT INTO work_location (location_name,address_line_1,address_line_2,city,country,timezone,is_active)
WITH RECURSIVE seq AS (
    SELECT 1 n UNION ALL SELECT n+1 FROM seq WHERE n < @entities_to_generate
)
SELECT
    CONCAT('Site ',n),
    CONCAT(n,' Industrial Road'),
    CONCAT('Building ', MOD(n,10)),
    ELT(1+MOD(n,6),'Copenhagen','Aarhus','Odense','Aalborg','Esbjerg','Randers'),
    'Denmark',
    'Europe/Copenhagen',
    1
FROM seq;

-- =========================================
-- USER ROLES
-- =========================================
INSERT INTO user_role (user_role_id, user_role_name)
VALUES (1, 'Administrator'), (2, 'Employee'), (3, 'Manager');

-- =========================================
-- EMPLOYEES (100)
-- Every 30th employee is a Manager, rest are Employee
-- =========================================
INSERT INTO employee (
    employee_number, first_name, last_name, email, login_password,
    phone_number, hire_date, employment_status,
    fk_user_role_id, primary_work_location_id
)
WITH RECURSIVE seq AS (
    SELECT 1 n UNION ALL SELECT n+1 FROM seq WHERE n < @entities_to_generate
)
SELECT
    CONCAT('EMP',LPAD(n,5,'0')),
    CONCAT('First',n),
    CONCAT('Last',n),
    CONCAT('employee',n,'@shift.dk'),
    SHA2(CONCAT(UUID(), RAND()), 256),
    CONCAT('+45 50',LPAD(n,6,'0')),
    DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*3000) DAY),
    ELT(1+MOD(n,3),'ACTIVE','ACTIVE','INACTIVE'),
    CASE WHEN MOD(n,30) = 0 THEN 3 ELSE 2 END,
    1+MOD(n,100)
FROM seq;

-- =========================================
-- CONTRACTS (100)
-- =========================================
INSERT INTO employee_contract (
    employee_id, department_id, contract_type,
    start_date, end_date, weekly_hours, salary_amount, is_active
)
SELECT
    employee_id,
    1+MOD(employee_id,@entities_to_generate),
    ELT(1+MOD(employee_id,3),'FULL_TIME','PART_TIME','TEMP'),
    DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*2000) DAY),
    NULL,
    30+MOD(employee_id,10),
    35000 + RAND()*40000,
    1
FROM employee;

-- =========================================
-- JOB ROLES (REALISTIC SMALL SET)
-- =========================================
INSERT INTO job_role (role_name,job_role_description,is_certification_required) VALUES
    ('Nurse','Registered nurse',1),
    ('Doctor','Medical doctor',1),
    ('Shift Supervisor','Oversees shift operations',0),
    ('Care Assistant','Supports patient care',0),
    ('Receptionist','Front desk operations',0),
    ('Security Officer','Site security',1),
    ('Cleaner','Facility cleaning',0),
    ('Lab Technician','Handles lab samples',1),
    ('Pharmacist','Medication management',1),
    ('Driver','Transport duties',0),
    ('Warehouse Operator','Stock handling',0),
    ('IT Support','Technical support',0);

-- =========================================
-- EMPLOYEE JOB ROLES
-- =========================================
INSERT INTO employee_job_role (employee_id, job_role_id, assigned_date, expiry_date, proficiency_level)
SELECT
    e.employee_id,
    1 + MOD(e.employee_id,12),
    DATE_SUB(CURDATE(), INTERVAL MOD(e.employee_id,600) DAY),
    NULL,
    ELT(1+MOD(e.employee_id,3),'BEGINNER','INTERMEDIATE','ADVANCED')
FROM employee e;

-- =========================================
-- SHIFTS (100)
-- =========================================
INSERT INTO shift (
    department_id, work_location_id,
    shift_name, start_datetime, end_datetime, shift_status
)
WITH RECURSIVE seq AS (
    SELECT 1 n UNION ALL SELECT n+1 FROM seq WHERE n < @entities_to_generate
)
SELECT
    1+MOD(n,@entities_to_generate),
    1+MOD(n,@entities_to_generate),
    CONCAT('Shift ',n),
    DATE_ADD(CURDATE(), INTERVAL MOD(n,14) DAY) + INTERVAL (MOD(n,24)) HOUR,
    DATE_ADD(CURDATE(), INTERVAL MOD(n,14) DAY) + INTERVAL (MOD(n,24)+8) HOUR,
    ELT(1+MOD(n,3),'PLANNED','COMPLETED','CANCELLED')
FROM seq;

-- =========================================
-- SHIFT REQUIRED ROLES
-- =========================================
INSERT INTO shift_required_job_role (shift_id, job_role_id, required_employee_count)
SELECT
    shift_id,
    1+MOD(shift_id,12),
    1+MOD(shift_id,3)
FROM shift;

-- =========================================
-- SHIFT ASSIGNMENTS (100) UNIQUE PAIRS
-- =========================================
INSERT INTO shift_assignment (
    shift_id, employee_id,
    assignment_status, assigned_datetime,
    check_in_datetime, check_out_datetime
)
SELECT
    s.shift_id,
    s.shift_id,
    ELT(1+MOD(s.shift_id,4),'ASSIGNED','CONFIRMED','COMPLETED','NO_SHOW'),
    NOW() - INTERVAL MOD(s.shift_id,10) DAY,
    NOW() - INTERVAL MOD(s.shift_id,10) DAY + INTERVAL 1 HOUR,
    NOW() - INTERVAL MOD(s.shift_id,10) DAY + INTERVAL 8 HOUR
FROM shift s;

-- =========================================
-- SHIFT APPROVALS (~70)
-- =========================================
INSERT INTO shift_approval (
    shift_assignment_id, approver_employee_id,
    decision, approval_comment, decision_datetime
)
SELECT
    shift_assignment_id,
    1+MOD(shift_assignment_id+5,@entities_to_generate),
    ELT(1+MOD(shift_assignment_id,3),'APPROVED','REJECTED','PENDING'),
    'Auto decision',
    NOW()
FROM shift_assignment
WHERE shift_assignment_id <= 70;

-- =========================================
-- SHIFT SWAPS (~50)
-- =========================================
INSERT INTO shift_swap (
    original_shift_assignment_id,
    employee_from_id,
    employee_to_id,
    swap_status,
    request_datetime,
    reason
)
SELECT
    sa.shift_assignment_id,
    sa.employee_id,
    1 + MOD(sa.employee_id + 5, @entities_to_generate),
    ELT(1 + MOD(sa.shift_assignment_id, 3), 'REQUESTED','APPROVED','DECLINED'),
    NOW() - INTERVAL MOD(sa.shift_assignment_id, 5) DAY,
    'Personal reason'
FROM shift_assignment sa
WHERE sa.shift_assignment_id <= 50;

-- =========================================
-- SHIFT SWAP APPROVALS (~40)
-- =========================================
INSERT INTO shift_swap_approval (
    shift_swap_id, approver_employee_id,
    decision, shift_swap_comment, decision_datetime
)
SELECT
    shift_swap_id,
    1+MOD(shift_swap_id+10,@entities_to_generate),
    ELT(1+MOD(shift_swap_id,3),'APPROVED','REJECTED','PENDING'),
    'Swap review',
    NOW()
FROM shift_swap
WHERE shift_swap_id <= 40;

-- =========================================
-- LEAVE TYPES
-- =========================================
INSERT INTO leave_type (
    leave_type_name, leave_type_description,
    requires_approval, is_paid_leave
) VALUES
    ('Vacation','Paid annual leave',1,1),
    ('Sick Leave','Medical leave',1,1),
    ('Maternity','Maternity leave',1,1),
    ('Paternity','Paternity leave',1,1),
    ('Unpaid Leave','Unpaid time off',1,0),
    ('Bereavement','Family death leave',1,1),
    ('Study Leave','Education leave',1,0),
    ('Emergency Leave','Emergency personal leave',1,1);

-- =========================================
-- LEAVE REQUESTS (100)
-- =========================================
INSERT INTO leave_request (
    employee_id, leave_type_id,
    start_date, end_date,
    request_status, reason, requested_datetime
)
SELECT
    employee_id,
    1+MOD(employee_id,8),
    CURDATE() - INTERVAL MOD(employee_id,30) DAY,
    CURDATE() + INTERVAL MOD(employee_id,10) DAY,
    ELT(1+MOD(employee_id,3),'PENDING','APPROVED','REJECTED'),
    'Personal leave',
    NOW() - INTERVAL MOD(employee_id,5) DAY
FROM employee;

-- =========================================
-- LEAVE APPROVALS (~70)
-- =========================================
INSERT INTO leave_approval (
    leave_request_id,
    approver_employee_id,
    decision,
    leave_comment,
    decision_datetime
)
SELECT
    leave_request_id,
    1+MOD(leave_request_id+7,@entities_to_generate),
    ELT(1+MOD(leave_request_id,3),'APPROVED','REJECTED','PENDING'),
    'Reviewed',
    NOW()
FROM leave_request
WHERE leave_request_id <= 70;

-- =========================================
-- LEAVE LEDGER (100)
-- =========================================
INSERT INTO leave_ledger (
    employee_id, leave_type_id,
    change_amount_days,
    transaction_type,
    reference_entity_type,
    reference_entity_id,
    transaction_datetime
)
SELECT
    employee_id,
    1+MOD(employee_id,8),
    (1+MOD(employee_id,5)),
    ELT(1+MOD(employee_id,2),'ACCRUAL','USAGE'),
    'LeaveRequest',
    employee_id,
    NOW() - INTERVAL MOD(employee_id,30) DAY
FROM employee;

-- =========================================
-- AUDIT LOG (100)
-- =========================================
INSERT INTO audit_log (
    entity_type, entity_id, action_type,
    performed_by_employee_id,
    action_datetime,
    old_value_snapshot,
    new_value_snapshot
)
SELECT
    'Employee',
    employee_id,
    ELT(1+MOD(employee_id,3),'CREATE','UPDATE','DELETE'),
    1+MOD(employee_id+3,100),
    NOW(),
    '{}',
    '{}'
FROM employee;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;
