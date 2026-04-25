// Shift Happens - Neo4j Indexes
// Kør denne fil i Neo4j Browser eller med cypher-shell.
// Constraints (unikke) er defineret i neo4j-schema.cypher.
// Denne fil indeholder kun performance-indexes.

// ========================
// Employee
// ========================
CREATE INDEX employee_email IF NOT EXISTS
FOR (e:Employee) ON (e.email);

CREATE INDEX employee_status IF NOT EXISTS
FOR (e:Employee) ON (e.employmentStatus);

CREATE INDEX employee_user_role IF NOT EXISTS
FOR (e:Employee) ON (e.fkUserRoleId);

// ========================
// Shift
// ========================
CREATE INDEX shift_status IF NOT EXISTS
FOR (s:Shift) ON (s.shiftStatus);

CREATE INDEX shift_start IF NOT EXISTS
FOR (s:Shift) ON (s.startDatetime);

CREATE INDEX shift_end IF NOT EXISTS
FOR (s:Shift) ON (s.endDatetime);

// ========================
// ShiftSwap
// ========================
CREATE INDEX shift_swap_status IF NOT EXISTS
FOR (ss:ShiftSwap) ON (ss.swapStatus);

CREATE INDEX shift_swap_employee_from IF NOT EXISTS
FOR (ss:ShiftSwap) ON (ss.employeeFromId);

CREATE INDEX shift_swap_employee_to IF NOT EXISTS
FOR (ss:ShiftSwap) ON (ss.employeeToId);

// ========================
// LeaveRequest
// ========================
CREATE INDEX leave_request_status IF NOT EXISTS
FOR (lr:LeaveRequest) ON (lr.status);

CREATE INDEX leave_request_employee IF NOT EXISTS
FOR (lr:LeaveRequest) ON (lr.employeeId);

// ========================
// LeaveApproval
// ========================
CREATE INDEX leave_approval_status IF NOT EXISTS
FOR (la:LeaveApproval) ON (la.approvalStatus);

// ========================
// ShiftApproval
// ========================
CREATE INDEX shift_approval_status IF NOT EXISTS
FOR (sa:ShiftApproval) ON (sa.approvalStatus);

// ========================
// ShiftSwapApproval
// ========================
CREATE INDEX shift_swap_approval_status IF NOT EXISTS
FOR (ssa:ShiftSwapApproval) ON (ssa.approvalStatus);

// ========================
// Department
// ========================
CREATE INDEX department_active IF NOT EXISTS
FOR (d:Department) ON (d.isActive);

// ========================
// WorkLocation
// ========================
CREATE INDEX work_location_active IF NOT EXISTS
FOR (w:WorkLocation) ON (w.isActive);

CREATE INDEX work_location_city IF NOT EXISTS
FOR (w:WorkLocation) ON (w.city);

// ========================
// LeaveLedger
// ========================
CREATE INDEX leave_ledger_employee IF NOT EXISTS
FOR (ll:LeaveLedger) ON (ll.employeeId);

CREATE INDEX leave_ledger_type IF NOT EXISTS
FOR (ll:LeaveLedger) ON (ll.leaveTypeId);

CREATE INDEX leave_ledger_transaction_type IF NOT EXISTS
FOR (ll:LeaveLedger) ON (ll.transactionType);

// ========================
// EmployeeContract
// ========================
CREATE INDEX employee_contract_employee IF NOT EXISTS
FOR (ec:EmployeeContract) ON (ec.employeeId);

CREATE INDEX employee_contract_department IF NOT EXISTS
FOR (ec:EmployeeContract) ON (ec.departmentId);

CREATE INDEX employee_contract_active IF NOT EXISTS
FOR (ec:EmployeeContract) ON (ec.isActive);

// ========================
// JobRole
// ========================
CREATE INDEX job_role_certification IF NOT EXISTS
FOR (j:JobRole) ON (j.isCertificationRequired);
