// Shift Happens - Neo4j starter schema
// Run this in Neo4j Browser or with cypher-shell.
// This file creates the core constraints and shows the intended graph model.

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
//
// Relationships:
//   (:Employee)-[:WORKS_IN]->(:Department)
//   (:Employee)-[:WORKS_AT]->(:WorkLocation)
//   (:Employee)-[:HAS_ROLE {assignedDate, expiryDate, proficiencyLevel}]->(:JobRole)
//   (:Shift)-[:IN_DEPARTMENT]->(:Department)
//   (:Shift)-[:AT_LOCATION]->(:WorkLocation)
//   (:Employee)-[:ASSIGNED_TO {assignmentStatus, assignedDatetime, checkInDatetime, checkOutDatetime}]->(:Shift)
//   (:ShiftSwap)-[:FOR_SHIFT]->(:Shift)
//   (:ShiftSwap)-[:FROM_EMPLOYEE]->(:Employee)
//   (:ShiftSwap)-[:TO_EMPLOYEE]->(:Employee)
//   (:Employee)-[:REQUESTED_LEAVE]->(:LeaveRequest)
//   (:LeaveRequest)-[:OF_TYPE]->(:LeaveType)
//   (:LeaveRequest)-[:HAS_APPROVAL]->(:LeaveApproval)
//   (:LeaveApproval)-[:APPROVED_BY]->(:Employee)
//   (:Shift)-[:HAS_APPROVAL]->(:ShiftApproval)
//   (:ShiftSwap)-[:HAS_APPROVAL]->(:ShiftSwapApproval)

// ========================
// Example seed data
// ========================
MERGE (d:Department {departmentId: 1})
SET d.departmentName = 'Kitchen',
    d.isActive = true;

MERGE (w:WorkLocation {workLocationId: 1})
SET w.locationName = 'HQ',
    w.city = 'Copenhagen',
    w.country = 'Denmark',
    w.isActive = true;

MERGE (j:JobRole {jobRoleId: 1})
SET j.roleName = 'Chef',
    j.isCertificationRequired = false;

MERGE (e:Employee {employeeId: 1})
SET e.employeeNumber = 'EMP001',
    e.firstName = 'Anna',
    e.lastName = 'Jensen',
    e.email = 'anna@shift-happens.dk',
    e.employmentStatus = 'ACTIVE';

MERGE (s:Shift {shiftId: 1})
SET s.shiftName = 'Morning Shift',
    s.startDatetime = datetime('2026-04-19T08:00:00'),
    s.endDatetime = datetime('2026-04-19T16:00:00'),
    s.shiftStatus = 'PLANNED';

MERGE (e)-[:WORKS_IN]->(d)
MERGE (e)-[:WORKS_AT]->(w)
MERGE (e)-[:HAS_ROLE {
  assignedDate: date('2026-01-01'),
  proficiencyLevel: 'INTERMEDIATE'
}]->(j)
MERGE (s)-[:IN_DEPARTMENT]->(d)
MERGE (s)-[:AT_LOCATION]->(w)
MERGE (e)-[:ASSIGNED_TO {
  assignmentStatus: 'ASSIGNED',
  assignedDatetime: datetime('2026-04-18T12:00:00')
}]->(s);
