// Shift Happens - Neo4j startup schema
// Run this in Neo4j Browser or with cypher-shell.
// This file is intended for startup initialization and should only contain
// constraints and documentation of the intended graph model.

// ========================
// Constraints
// ========================
CREATE CONSTRAINT employee_id IF NOT EXISTS
FOR (e:Employee) REQUIRE e.employeeId IS UNIQUE;

CREATE CONSTRAINT department_id IF NOT EXISTS
FOR (d:Department) REQUIRE d.departmentId IS UNIQUE;

CREATE CONSTRAINT work_location_id IF NOT EXISTS
FOR (w:WorkLocation) REQUIRE w.workLocationId IS UNIQUE;

CREATE CONSTRAINT job_role_id IF NOT EXISTS
FOR (j:JobRole) REQUIRE j.jobRoleId IS UNIQUE;

CREATE CONSTRAINT shift_id IF NOT EXISTS
FOR (s:Shift) REQUIRE s.shiftId IS UNIQUE;

CREATE CONSTRAINT shift_swap_id IF NOT EXISTS
FOR (ss:ShiftSwap) REQUIRE ss.shiftSwapId IS UNIQUE;

CREATE CONSTRAINT leave_type_id IF NOT EXISTS
FOR (lt:LeaveType) REQUIRE lt.leaveTypeId IS UNIQUE;

CREATE CONSTRAINT leave_request_id IF NOT EXISTS
FOR (lr:LeaveRequest) REQUIRE lr.leaveRequestId IS UNIQUE;

CREATE CONSTRAINT leave_approval_id IF NOT EXISTS
FOR (la:LeaveApproval) REQUIRE la.leaveApprovalId IS UNIQUE;

CREATE CONSTRAINT shift_approval_id IF NOT EXISTS
FOR (sa:ShiftApproval) REQUIRE sa.shiftApprovalId IS UNIQUE;

CREATE CONSTRAINT shift_swap_approval_id IF NOT EXISTS
FOR (ssa:ShiftSwapApproval) REQUIRE ssa.shiftSwapApprovalId IS UNIQUE;

CREATE CONSTRAINT leave_ledger_id IF NOT EXISTS
FOR (ll:LeaveLedger) REQUIRE ll.leaveLedgerId IS UNIQUE;

CREATE CONSTRAINT employee_contract_id IF NOT EXISTS
FOR (ec:EmployeeContract) REQUIRE ec.contractId IS UNIQUE;

// ========================
// Recommended graph model
// ========================
// Nodes:
//   (:Employee)
//   (:Department)
//   (:WorkLocation)
//   (:JobRole)
//   (:Shift)
//   (:ShiftSwap)
//   (:LeaveType)
//   (:LeaveRequest)
//   (:LeaveApproval)
//   (:ShiftApproval)
//   (:ShiftSwapApproval)
//   (:LeaveLedger)
//   (:EmployeeContract)
//
// Relationships:
//   (:Employee)-[:WORKS_IN_DEPT]->(:Department)
//   (:Employee)-[:WORKS_AT_LOCATION]->(:WorkLocation)
//   (:Employee)-[:HAS_JOB_ROLE {assignedDate, expiryDate, proficiencyLevel}]->(:JobRole)
//   (:Employee)-[:HAS_CONTRACT]->(:EmployeeContract)
//   (:EmployeeContract)-[:CONTRACT_IN_DEPT]->(:Department)
//   (:Shift)-[:SHIFT_IN_DEPT]->(:Department)
//   (:Shift)-[:SHIFT_AT_LOCATION]->(:WorkLocation)
//   (:Employee)-[:ASSIGNED_TO_SHIFT {assignmentStatus, assignedDatetime, checkInDatetime, checkOutDatetime}]->(:Shift)
//   (:Shift)-[:REQUIRES_ROLE {requiredEmployeeCount}]->(:JobRole)
//   (:ShiftSwap)-[:SWAP_FOR_SHIFT]->(:Shift)
//   (:ShiftSwap)-[:SWAP_FROM_EMPLOYEE]->(:Employee)
//   (:ShiftSwap)-[:SWAP_TO_EMPLOYEE]->(:Employee)
//   (:Employee)-[:REQUESTED_LEAVE]->(:LeaveRequest)
//   (:LeaveRequest)-[:OF_LEAVE_TYPE]->(:LeaveType)
//   (:LeaveApproval)-[:REVIEWS_LEAVE_REQUEST]->(:LeaveRequest)
//   (:Employee)-[:APPROVES_LEAVE]->(:LeaveApproval)
//   (:LeaveApproval)-[:CONCERNS_EMPLOYEE]->(:Employee)
//   (:Employee)-[:ENTITLED_TO_LEAVE]->(:LeaveLedger)
//   (:LeaveLedger)-[:TRACKS_LEAVE_TYPE]->(:LeaveType)
//   (:Shift)-[:HAS_SHIFT_APPROVAL]->(:ShiftApproval)
//   (:ShiftApproval)-[:APPROVED_BY_EMPLOYEE]->(:Employee)
//   (:ShiftApproval)-[:APPROVAL_FOR_EMPLOYEE]->(:Employee)
//   (:ShiftSwap)-[:HAS_SWAP_APPROVAL]->(:ShiftSwapApproval)
//   (:ShiftSwapApproval)-[:APPROVED_BY_EMPLOYEE]->(:Employee)

