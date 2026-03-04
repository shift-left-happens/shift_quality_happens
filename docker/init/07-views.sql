USE shift_happens;

-- =========================================
-- VIEW: Employee overview with department
-- =========================================
CREATE OR REPLACE VIEW vw_employee_overview AS
SELECT e.employee_id,
       e.employee_number,
       CONCAT(e.first_name, ' ', e.last_name) AS full_name,
       e.email,
       e.employment_status,
       d.department_name,
       ec.contract_type,
       ec.weekly_hours,
       wl.location_name                       AS primary_location
FROM employee e
         LEFT JOIN employee_contract ec ON e.employee_id = ec.employee_id AND ec.is_active = 1
         LEFT JOIN department d ON ec.department_id = d.department_id
         LEFT JOIN work_location wl ON e.primary_work_location_id = wl.work_location_id;

-- =========================================
-- VIEW: Current shift schedule with assignments
-- =========================================
CREATE OR REPLACE VIEW vw_shift_schedule AS
SELECT s.shift_id,
       s.shift_name,
       s.start_datetime,
       s.end_datetime,
       s.shift_status,
       d.department_name,
       wl.location_name,
       sa.shift_assignment_id,
       sa.assignment_status,
       e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS assigned_employee
FROM shift s
         JOIN department d ON s.department_id = d.department_id
         JOIN work_location wl ON s.work_location_id = wl.work_location_id
         LEFT JOIN shift_assignment sa ON s.shift_id = sa.shift_id
         LEFT JOIN employee e ON sa.employee_id = e.employee_id;

-- =========================================
-- VIEW: Leave balances per employee per leave type
-- =========================================
CREATE OR REPLACE VIEW vw_leave_balance AS
SELECT e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name)  AS full_name,
       lt.leave_type_id,
       lt.leave_type_name,
       COALESCE(SUM(ll.change_amount_days), 0) AS balance_days
FROM employee e
         CROSS JOIN leave_type lt
         LEFT JOIN leave_ledger ll
                   ON e.employee_id = ll.employee_id
                       AND lt.leave_type_id = ll.leave_type_id
GROUP BY e.employee_id, e.first_name, e.last_name, lt.leave_type_id, lt.leave_type_name;

-- =========================================
-- VIEW: Pending leave requests with employee info
-- =========================================
CREATE OR REPLACE VIEW vw_pending_leave_requests AS
SELECT lr.leave_request_id,
       e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name)   AS employee_name,
       lt.leave_type_name,
       lr.start_date,
       lr.end_date,
       DATEDIFF(lr.end_date, lr.start_date) + 1 AS days_requested,
       lr.reason,
       lr.requested_datetime
FROM leave_request lr
         JOIN employee e ON lr.employee_id = e.employee_id
         JOIN leave_type lt ON lr.leave_type_id = lt.leave_type_id
WHERE lr.request_status = 'PENDING';

-- =========================================
-- VIEW: Employee leave overview (full details)
-- =========================================
CREATE OR REPLACE VIEW vw_employee_leave_overview AS
SELECT lr.leave_request_id,
       e.employee_id,
       e.employee_number,
       e.first_name,
       e.last_name,
       e.email,
       lt.leave_type_id,
       lt.leave_type_name,
       lt.is_paid_leave,
       lt.requires_approval,
       lr.start_date,
       lr.end_date,
       lr.request_status,
       lr.reason,
       lr.requested_datetime,
       la.leave_approval_id,
       la.decision          AS approval_decision,
       la.leave_comment     AS approval_comment,
       la.decision_datetime AS approval_datetime,
       approver.employee_number AS approver_number,
       approver.first_name      AS approver_first_name,
       approver.last_name       AS approver_last_name
FROM leave_request lr
         JOIN employee e ON lr.employee_id = e.employee_id
         JOIN leave_type lt ON lr.leave_type_id = lt.leave_type_id
         LEFT JOIN leave_approval la ON lr.leave_request_id = la.leave_request_id
         LEFT JOIN employee approver ON la.approver_employee_id = approver.employee_id;

-- =========================================
-- VIEW: Employee shift overview (full details)
-- =========================================
CREATE OR REPLACE VIEW vw_employee_shift_overview AS
SELECT sa.shift_assignment_id,
       e.employee_id,
       e.employee_number,
       e.first_name,
       e.last_name,
       e.email,
       s.shift_id,
       s.shift_name,
       s.start_datetime,
       s.end_datetime,
       s.shift_status,
       d.department_name,
       wl.location_name,
       sa.assignment_status,
       sa.assigned_datetime,
       sa.check_in_datetime,
       sa.check_out_datetime
FROM shift_assignment sa
         JOIN employee e ON sa.employee_id = e.employee_id
         JOIN shift s ON sa.shift_id = s.shift_id
         JOIN department d ON s.department_id = d.department_id
         JOIN work_location wl ON s.work_location_id = wl.work_location_id;
