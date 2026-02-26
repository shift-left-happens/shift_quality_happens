USE railway;

--
-- Views based on employee and shift
--

DROP VIEW IF EXISTS vw_employee_shift_overview;
CREATE VIEW vw_employee_shift_overview AS
SELECT
    sa.shift_assignment_id,
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
FROM shiftassignment sa
JOIN employee e ON e.employee_id = sa.employee_id
JOIN shift s ON s.shift_id = sa.shift_id
JOIN department d ON d.department_id = s.department_id
JOIN worklocation wl ON wl.work_location_id = s.work_location_id;

DROP VIEW IF EXISTS vw_open_shifts;
CREATE VIEW vw_open_shifts AS
SELECT
    s.shift_id,
    s.shift_name,
    s.start_datetime,
    s.end_datetime,
    s.shift_status,
    d.department_name,
    wl.location_name,
    COALESCE(srjr.required_employee_count, 0) AS required_employee_count,
    COUNT(sa.shift_assignment_id) AS assigned_employee_count,
    COALESCE(srjr.required_employee_count, 0) - COUNT(sa.shift_assignment_id) AS remaining_slots
FROM shift s
JOIN department d ON d.department_id = s.department_id
JOIN worklocation wl ON wl.work_location_id = s.work_location_id
LEFT JOIN shiftrequiredjobrole srjr ON srjr.shift_id = s.shift_id
LEFT JOIN shiftassignment sa ON sa.shift_id = s.shift_id
GROUP BY
    s.shift_id,
    s.shift_name,
    s.start_datetime,
    s.end_datetime,
    s.shift_status,
    d.department_name,
    wl.location_name,
    srjr.required_employee_count;

--
-- View for employee leave requests with approval and leave type info
--

DROP VIEW IF EXISTS vw_employee_leave_overview;
CREATE VIEW vw_employee_leave_overview AS
SELECT
    lr.leave_request_id,
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
    la.decision AS approval_decision,
    la.leave_comment AS approval_comment,
    la.decision_datetime AS approval_datetime,
    approver.employee_number AS approver_number,
    approver.first_name AS approver_first_name,
    approver.last_name AS approver_last_name
FROM leaverequest lr
JOIN employee e ON e.employee_id = lr.employee_id
JOIN leavetype lt ON lt.leave_type_id = lr.leave_type_id
LEFT JOIN leaveapproval la ON la.leave_request_id = lr.leave_request_id
LEFT JOIN employee approver ON approver.employee_id = la.approver_employee_id;
 