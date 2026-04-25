package dk.ek.shift_happens.migration;

import dk.ek.shift_happens.department.Department;
import dk.ek.shift_happens.department.DepartmentRepository;
import dk.ek.shift_happens.department.neo4j.DepartmentNode;
import dk.ek.shift_happens.department.neo4j.DepartmentNeo4jRepository;
import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.employee.neo4j.EmployeeNode;
import dk.ek.shift_happens.employee.neo4j.EmployeeNeo4jRepository;
import dk.ek.shift_happens.employeecontract.EmployeeContractRepository;
import dk.ek.shift_happens.employeejobrole.EmployeeJobRoleRepository;
import dk.ek.shift_happens.jobrole.JobRole;
import dk.ek.shift_happens.jobrole.JobRoleRepository;
import dk.ek.shift_happens.jobrole.neo4j.JobRoleNode;
import dk.ek.shift_happens.jobrole.neo4j.JobRoleNeo4jRepository;
import dk.ek.shift_happens.employeecontract.EmployeeContract;
import dk.ek.shift_happens.leaveapproval.LeaveApproval;
import dk.ek.shift_happens.leaveapproval.LeaveApprovalRepository;
import dk.ek.shift_happens.leaveledger.LeaveLedger;
import dk.ek.shift_happens.leaveledger.LeaveLedgerRepository;
import dk.ek.shift_happens.leaverequest.LeaveRequest;
import dk.ek.shift_happens.leaverequest.LeaveRequestRepository;
import dk.ek.shift_happens.leavetype.LeaveType;
import dk.ek.shift_happens.leavetype.LeaveTypeRepository;
import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shift.neo4j.ShiftNode;
import dk.ek.shift_happens.shift.neo4j.ShiftNeo4jRepository;
import dk.ek.shift_happens.shiftapproval.ShiftApproval;
import dk.ek.shift_happens.shiftapproval.ShiftApprovalRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRoleRepository;
import dk.ek.shift_happens.shiftswap.ShiftSwap;
import dk.ek.shift_happens.shiftswap.ShiftSwapRepository;
import dk.ek.shift_happens.shiftswap.neo4j.ShiftSwapNeo4jRepository;
import dk.ek.shift_happens.shiftswap.neo4j.ShiftSwapNode;
import dk.ek.shift_happens.shiftswapapproval.ShiftSwapApproval;
import dk.ek.shift_happens.shiftswapapproval.ShiftSwapApprovalRepository;
import dk.ek.shift_happens.worklocation.WorkLocation;
import dk.ek.shift_happens.worklocation.WorkLocationRepository;
import dk.ek.shift_happens.worklocation.neo4j.WorkLocationNode;
import dk.ek.shift_happens.worklocation.neo4j.WorkLocationNeo4jRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class Neo4jMigrationService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final EmployeeJobRoleRepository employeeJobRoleRepository;
    private final JobRoleRepository jobRoleRepository;
    private final WorkLocationRepository workLocationRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftSwapRepository shiftSwapRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveLedgerRepository leaveLedgerRepository;
    private final ShiftApprovalRepository shiftApprovalRepository;
    private final ShiftSwapApprovalRepository shiftSwapApprovalRepository;

    private final EmployeeNeo4jRepository employeeNeo4jRepository;
    private final DepartmentNeo4jRepository departmentNeo4jRepository;
    private final WorkLocationNeo4jRepository workLocationNeo4jRepository;
    private final ShiftNeo4jRepository shiftNeo4jRepository;
    private final JobRoleNeo4jRepository jobRoleNeo4jRepository;
    private final ShiftSwapNeo4jRepository shiftSwapNeo4jRepository;
    private final Neo4jClient neo4jClient;

    public MigrationResult migrateToNeo4j() {
        List<String> errors = new ArrayList<>();
        int employees = 0, departments = 0, workLocations = 0, shifts = 0, jobRoles = 0, shiftSwaps = 0, shiftAssignments = 0;
        int leaveTypes = 0, leaveRequests = 0, leaveApprovals = 0, shiftApprovals = 0, shiftSwapApprovals = 0;
        int leaveLedgers = 0, employeeContracts = 0;

        try { employees          = migrateEmployeesToNeo4j(); }           catch (Exception e) { log.error("neo4j:employees failed",            e); errors.add("neo4j:employees — "            + e.getMessage()); }
        try { departments        = migrateDepartmentsToNeo4j(); }         catch (Exception e) { log.error("neo4j:departments failed",          e); errors.add("neo4j:departments — "          + e.getMessage()); }
        try { workLocations      = migrateWorkLocationsToNeo4j(); }       catch (Exception e) { log.error("neo4j:worklocations failed",        e); errors.add("neo4j:worklocations — "        + e.getMessage()); }
        try { shifts             = migrateShiftsToNeo4j(); }              catch (Exception e) { log.error("neo4j:shifts failed",               e); errors.add("neo4j:shifts — "               + e.getMessage()); }
        try { jobRoles           = migrateJobRolesToNeo4j(); }            catch (Exception e) { log.error("neo4j:jobroles failed",             e); errors.add("neo4j:jobroles — "             + e.getMessage()); }
        try { shiftSwaps         = migrateShiftSwapsToNeo4j(); }          catch (Exception e) { log.error("neo4j:shiftswaps failed",           e); errors.add("neo4j:shiftswaps — "           + e.getMessage()); }
        try { shiftAssignments   = migrateShiftAssignmentsToNeo4j(); }    catch (Exception e) { log.error("neo4j:shiftassignments failed",     e); errors.add("neo4j:shiftassignments — "     + e.getMessage()); }
        try { leaveTypes         = migrateLeaveTypesToNeo4j(); }          catch (Exception e) { log.error("neo4j:leavetypes failed",           e); errors.add("neo4j:leavetypes — "           + e.getMessage()); }
        try { leaveRequests      = migrateLeaveRequestsToNeo4j(); }       catch (Exception e) { log.error("neo4j:leaverequests failed",        e); errors.add("neo4j:leaverequests — "        + e.getMessage()); }
        try { leaveApprovals     = migrateLeaveApprovalsToNeo4j(); }      catch (Exception e) { log.error("neo4j:leaveapprovals failed",       e); errors.add("neo4j:leaveapprovals — "       + e.getMessage()); }
        try { shiftApprovals     = migrateShiftApprovalsToNeo4j(); }      catch (Exception e) { log.error("neo4j:shiftapprovals failed",       e); errors.add("neo4j:shiftapprovals — "       + e.getMessage()); }
        try { shiftSwapApprovals = migrateShiftSwapApprovalsToNeo4j(); }  catch (Exception e) { log.error("neo4j:shiftswapapprovals failed",   e); errors.add("neo4j:shiftswapapprovals — "   + e.getMessage()); }
        try { leaveLedgers       = migrateLeaveLedgersToNeo4j(); }        catch (Exception e) { log.error("neo4j:leaveledgers failed",          e); errors.add("neo4j:leaveledgers — "          + e.getMessage()); }
        try { employeeContracts  = migrateEmployeeContractsToNeo4j(); }   catch (Exception e) { log.error("neo4j:employeecontracts failed",      e); errors.add("neo4j:employeecontracts — "      + e.getMessage()); }
        try { createNeo4jRelationships(); }                               catch (Exception e) { log.error("neo4j:relationships failed",          e); errors.add("neo4j:relationships — "          + e.getMessage()); }

        return new MigrationResult(
                0, 0, 0, 0,
                employees, departments, workLocations, shifts, jobRoles, shiftSwaps, shiftAssignments,
                leaveTypes, leaveRequests, leaveApprovals, shiftApprovals, shiftSwapApprovals,
                leaveLedgers, employeeContracts,
                errors
        );
    }

    public int migrateEmployeesToNeo4j() {
        employeeNeo4jRepository.deleteAll();
        List<EmployeeNode> nodes = employeeRepository.findAll().stream()
                .map(this::toEmployeeNode)
                .toList();
        employeeNeo4jRepository.saveAll(nodes);
        return nodes.size();
    }

    public int migrateDepartmentsToNeo4j() {
        departmentNeo4jRepository.deleteAll();
        List<DepartmentNode> nodes = departmentRepository.findAll().stream()
                .map(this::toDepartmentNode)
                .toList();
        departmentNeo4jRepository.saveAll(nodes);
        return nodes.size();
    }

    public int migrateWorkLocationsToNeo4j() {
        workLocationNeo4jRepository.deleteAll();
        List<WorkLocationNode> nodes = workLocationRepository.findAll().stream()
                .map(this::toWorkLocationNode)
                .toList();
        workLocationNeo4jRepository.saveAll(nodes);
        return nodes.size();
    }

    public int migrateShiftsToNeo4j() {
        shiftNeo4jRepository.deleteAll();
        List<ShiftNode> nodes = shiftRepository.findAll().stream()
                .map(this::toShiftNode)
                .toList();
        shiftNeo4jRepository.saveAll(nodes);
        return nodes.size();
    }

    public int migrateJobRolesToNeo4j() {
        jobRoleNeo4jRepository.deleteAll();
        List<JobRoleNode> nodes = jobRoleRepository.findAll().stream()
                .map(this::toJobRoleNode)
                .toList();
        jobRoleNeo4jRepository.saveAll(nodes);
        return nodes.size();
    }

    public int migrateShiftSwapsToNeo4j() {
        shiftSwapNeo4jRepository.deleteAll();
        List<ShiftSwapNode> nodes = shiftSwapRepository.findAll().stream()
                .map(this::toShiftSwapNode)
                .toList();
        shiftSwapNeo4jRepository.saveAll(nodes);
        return nodes.size();
    }

    public int migrateShiftAssignmentsToNeo4j() {
        deleteAllNodesByLabel("ShiftAssignment");
        List<ShiftAssignment> assignments = shiftAssignmentRepository.findAll();

        assignments.forEach(a -> {
            Map<String, Object> params = new HashMap<>();
            params.put("shiftAssignmentId", a.getShiftAssignmentId());
            params.put("shiftId", a.getShiftId());
            params.put("employeeId", a.getEmployeeId());
            params.put("assignmentStatus", a.getAssignmentStatus());
            params.put("assignedDatetime", a.getAssignedDatetime());
            params.put("checkInDatetime", a.getCheckInDatetime());
            params.put("checkOutDatetime", a.getCheckOutDatetime());

            neo4jClient.query("""
                    MERGE (sa:ShiftAssignment {shiftAssignmentId: $shiftAssignmentId})
                    SET sa.shiftId = $shiftId,
                        sa.employeeId = $employeeId,
                        sa.assignmentStatus = $assignmentStatus,
                        sa.assignedDatetime = $assignedDatetime,
                        sa.checkInDatetime = $checkInDatetime,
                        sa.checkOutDatetime = $checkOutDatetime
                    """)
                    .bindAll(params)
                    .run();
        });

        return assignments.size();
    }

    public int migrateLeaveTypesToNeo4j() {
        deleteAllNodesByLabel("LeaveType");
        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();

        leaveTypes.forEach(type -> {
            Map<String, Object> params = new HashMap<>();
            params.put("leaveTypeId", type.getLeaveTypeId());
            params.put("leaveTypeName", type.getLeaveTypeName());
            params.put("leaveTypeDescription", type.getLeaveTypeDescription());
            params.put("requiresApproval", type.getRequiresApproval());
            params.put("isPaidLeave", type.getIsPaidLeave());

            neo4jClient.query("""
                    MERGE (lt:LeaveType {leaveTypeId: $leaveTypeId})
                    SET lt.leaveTypeName = $leaveTypeName,
                        lt.leaveTypeDescription = $leaveTypeDescription,
                        lt.requiresApproval = $requiresApproval,
                        lt.isPaidLeave = $isPaidLeave
                    """)
                    .bindAll(params)
                    .run();
        });

        return leaveTypes.size();
    }

    public int migrateLeaveRequestsToNeo4j() {
        deleteAllNodesByLabel("LeaveRequest");
        List<LeaveRequest> requests = leaveRequestRepository.findAll();

        requests.forEach(request -> {
            Map<String, Object> params = new HashMap<>();
            params.put("leaveRequestId", request.getLeaveRequestId());
            params.put("employeeId", request.getEmployeeId());
            params.put("leaveTypeId", request.getLeaveTypeId());
            params.put("startDate", request.getStartDate());
            params.put("endDate", request.getEndDate());
            params.put("requestStatus", request.getRequestStatus());
            params.put("reason", request.getReason());
            params.put("requestedDatetime", request.getRequestedDatetime());

            neo4jClient.query("""
                    MERGE (lr:LeaveRequest {leaveRequestId: $leaveRequestId})
                    SET lr.employeeId = $employeeId,
                        lr.leaveTypeId = $leaveTypeId,
                        lr.startDate = $startDate,
                        lr.endDate = $endDate,
                        lr.requestStatus = $requestStatus,
                        lr.reason = $reason,
                        lr.requestedDatetime = $requestedDatetime
                    """)
                    .bindAll(params)
                    .run();
        });

        return requests.size();
    }

    public int migrateLeaveApprovalsToNeo4j() {
        deleteAllNodesByLabel("LeaveApproval");
        List<LeaveApproval> approvals = leaveApprovalRepository.findAll();

        approvals.forEach(approval -> {
            Map<String, Object> params = new HashMap<>();
            params.put("leaveApprovalId", approval.getLeaveApprovalId());
            params.put("leaveRequestId", approval.getLeaveRequestId());
            params.put("approverEmployeeId", approval.getApproverEmployeeId());
            params.put("decision", approval.getDecision());
            params.put("leaveComment", approval.getLeaveComment());
            params.put("decisionDatetime", approval.getDecisionDatetime());

            neo4jClient.query("""
                    MERGE (la:LeaveApproval {leaveApprovalId: $leaveApprovalId})
                    SET la.leaveRequestId = $leaveRequestId,
                        la.approverEmployeeId = $approverEmployeeId,
                        la.decision = $decision,
                        la.leaveComment = $leaveComment,
                        la.decisionDatetime = $decisionDatetime
                    """)
                    .bindAll(params)
                    .run();
        });

        return approvals.size();
    }

    public int migrateShiftApprovalsToNeo4j() {
        deleteAllNodesByLabel("ShiftApproval");
        List<ShiftApproval> approvals = shiftApprovalRepository.findAll();

        approvals.forEach(approval -> {
            Map<String, Object> params = new HashMap<>();
            params.put("shiftApprovalId", approval.getShiftApprovalId());
            params.put("shiftAssignmentId", approval.getShiftAssignmentId());
            params.put("approverEmployeeId", approval.getApproverEmployeeId());
            params.put("decision", approval.getDecision());
            params.put("approvalComment", approval.getApprovalComment());
            params.put("decisionDatetime", approval.getDecisionDatetime());

            neo4jClient.query("""
                    MERGE (sa:ShiftApproval {shiftApprovalId: $shiftApprovalId})
                    SET sa.shiftAssignmentId = $shiftAssignmentId,
                        sa.approverEmployeeId = $approverEmployeeId,
                        sa.decision = $decision,
                        sa.approvalComment = $approvalComment,
                        sa.decisionDatetime = $decisionDatetime
                    """)
                    .bindAll(params)
                    .run();
        });

        return approvals.size();
    }

    public int migrateLeaveLedgersToNeo4j() {
        deleteAllNodesByLabel("LeaveLedger");
        List<LeaveLedger> ledgers = leaveLedgerRepository.findAll();

        ledgers.forEach(ledger -> {
            Map<String, Object> params = new HashMap<>();
            params.put("leaveLedgerId", ledger.getLeaveLedgerId());
            params.put("employeeId", ledger.getEmployeeId());
            params.put("leaveTypeId", ledger.getLeaveTypeId());
            params.put("changeAmountDays", ledger.getChangeAmountDays() != null ? ledger.getChangeAmountDays().doubleValue() : null);
            params.put("transactionType", ledger.getTransactionType());
            params.put("referenceEntityType", ledger.getReferenceEntityType());
            params.put("referenceEntityId", ledger.getReferenceEntityId());
            params.put("transactionDatetime", ledger.getTransactionDatetime());

            neo4jClient.query("""
                    MERGE (ll:LeaveLedger {leaveLedgerId: $leaveLedgerId})
                    SET ll.employeeId = $employeeId,
                        ll.leaveTypeId = $leaveTypeId,
                        ll.changeAmountDays = $changeAmountDays,
                        ll.transactionType = $transactionType,
                        ll.referenceEntityType = $referenceEntityType,
                        ll.referenceEntityId = $referenceEntityId,
                        ll.transactionDatetime = $transactionDatetime
                    """)
                    .bindAll(params)
                    .run();
        });

        return ledgers.size();
    }

    public int migrateEmployeeContractsToNeo4j() {
        deleteAllNodesByLabel("EmployeeContract");
        List<EmployeeContract> contracts = employeeContractRepository.findAll();

        contracts.forEach(contract -> {
            Map<String, Object> params = new HashMap<>();
            params.put("contractId", contract.getContractId());
            params.put("employeeId", contract.getEmployeeId());
            params.put("departmentId", contract.getDepartmentId());
            params.put("contractType", contract.getContractType());
            params.put("startDate", contract.getStartDate());
            params.put("endDate", contract.getEndDate());
            params.put("weeklyHours", contract.getWeeklyHours());
            params.put("isActive", contract.getIsActive());

            neo4jClient.query("""
                    MERGE (ec:EmployeeContract {contractId: $contractId})
                    SET ec.employeeId = $employeeId,
                        ec.departmentId = $departmentId,
                        ec.contractType = $contractType,
                        ec.startDate = $startDate,
                        ec.endDate = $endDate,
                        ec.weeklyHours = $weeklyHours,
                        ec.isActive = $isActive
                    """)
                    .bindAll(params)
                    .run();
        });

        return contracts.size();
    }

    public int migrateShiftSwapApprovalsToNeo4j() {
        deleteAllNodesByLabel("ShiftSwapApproval");
        List<ShiftSwapApproval> approvals = shiftSwapApprovalRepository.findAll();

        approvals.forEach(approval -> {
            Map<String, Object> params = new HashMap<>();
            params.put("shiftSwapApprovalId", approval.getShiftSwapApprovalId());
            params.put("shiftSwapId", approval.getShiftSwapId());
            params.put("approverEmployeeId", approval.getApproverEmployeeId());
            params.put("decision", approval.getDecision());
            params.put("shiftSwapComment", approval.getShiftSwapComment());
            params.put("decisionDatetime", approval.getDecisionDatetime());

            neo4jClient.query("""
                    MERGE (ssa:ShiftSwapApproval {shiftSwapApprovalId: $shiftSwapApprovalId})
                    SET ssa.shiftSwapId = $shiftSwapId,
                        ssa.approverEmployeeId = $approverEmployeeId,
                        ssa.decision = $decision,
                        ssa.shiftSwapComment = $shiftSwapComment,
                        ssa.decisionDatetime = $decisionDatetime
                    """)
                    .bindAll(params)
                    .run();
        });

        return approvals.size();
    }

    private void deleteAllNodesByLabel(String label) {
        neo4jClient.query("MATCH (n:" + label + ") DETACH DELETE n").run();
    }

    private void createNeo4jRelationships() {
        createEmployeeWorkLocationRelationships();
        createEmployeeDepartmentRelationships();
        createEmployeeContractRelationships();
        createEmployeeJobRoleRelationships();
        createShiftDepartmentRelationships();
        createShiftLocationRelationships();
        createShiftRequiredRoleRelationships();
        createShiftAssignmentRelationships();
        createShiftSwapRelationships();
        createLeaveRequestRelationships();
        createLeaveApprovalRelationships();
        createLeaveLedgerRelationships();
        createShiftApprovalRelationships();
        createShiftSwapApprovalRelationships();
    }

    private void createEmployeeWorkLocationRelationships() {
        neo4jClient.query("""
                MATCH (e:Employee)
                MATCH (w:WorkLocation {workLocationId: e.primaryWorkLocationId})
                MERGE (e)-[r:WORKS_AT_LOCATION]->(w)
                SET r.isPrimary = true
                """).run();
    }

    private void createEmployeeDepartmentRelationships() {
        employeeContractRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .forEach(c -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("employeeId", c.getEmployeeId());
                    params.put("departmentId", c.getDepartmentId());
                    params.put("contractType", c.getContractType());
                    params.put("startDate", c.getStartDate());
                    params.put("endDate", c.getEndDate());
                    params.put("weeklyHours", c.getWeeklyHours());
                    params.put("isActive", c.getIsActive());

                    neo4jClient.query("""
                            MATCH (e:Employee {employeeId: $employeeId})
                            MATCH (d:Department {departmentId: $departmentId})
                            MERGE (e)-[r:WORKS_IN_DEPT]->(d)
                            SET r.contractType = $contractType,
                                r.startDate = $startDate,
                                r.endDate = $endDate,
                                r.weeklyHours = $weeklyHours,
                                r.isActive = $isActive
                            """)
                            .bindAll(params)
                            .run();
                });
    }

    private void createEmployeeJobRoleRelationships() {
        employeeJobRoleRepository.findAll().forEach(ejr -> {
            Map<String, Object> params = new HashMap<>();
            params.put("employeeId", ejr.getEmployeeId());
            params.put("jobRoleId", ejr.getJobRoleId());
            params.put("assignedDate", ejr.getAssignedDate());
            params.put("expiryDate", ejr.getExpiryDate());
            params.put("proficiencyLevel", ejr.getProficiencyLevel());

            neo4jClient.query("""
                    MATCH (e:Employee {employeeId: $employeeId})
                    MATCH (j:JobRole {jobRoleId: $jobRoleId})
                    MERGE (e)-[r:HAS_JOB_ROLE]->(j)
                    SET r.assignedDate = $assignedDate,
                        r.expiryDate = $expiryDate,
                        r.proficiencyLevel = $proficiencyLevel
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createShiftDepartmentRelationships() {
        shiftRepository.findAll().forEach(s -> neo4jClient.query("""
                MATCH (sh:Shift {shiftId: $shiftId})
                MATCH (d:Department {departmentId: $departmentId})
                MERGE (sh)-[:SHIFT_IN_DEPT]->(d)
                """)
                .bind(s.getShiftId()).to("shiftId")
                .bind(s.getDepartmentId()).to("departmentId")
                .run());
    }

    private void createShiftLocationRelationships() {
        shiftRepository.findAll().forEach(s -> neo4jClient.query("""
                MATCH (sh:Shift {shiftId: $shiftId})
                MATCH (w:WorkLocation {workLocationId: $workLocationId})
                MERGE (sh)-[:SHIFT_AT_LOCATION]->(w)
                """)
                .bind(s.getShiftId()).to("shiftId")
                .bind(s.getWorkLocationId()).to("workLocationId")
                .run());
    }

    private void createShiftRequiredRoleRelationships() {
        shiftRequiredJobRoleRepository.findAll().forEach(req -> {
            Map<String, Object> params = new HashMap<>();
            params.put("shiftId", req.getShiftId());
            params.put("jobRoleId", req.getJobRoleId());
            params.put("requiredEmployeeCount", req.getRequiredEmployeeCount());

            neo4jClient.query("""
                    MATCH (sh:Shift {shiftId: $shiftId})
                    MATCH (j:JobRole {jobRoleId: $jobRoleId})
                    MERGE (sh)-[r:REQUIRES_ROLE]->(j)
                    SET r.requiredEmployeeCount = $requiredEmployeeCount
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createShiftAssignmentRelationships() {
        shiftAssignmentRepository.findAll().forEach(a -> {
            Map<String, Object> params = new HashMap<>();
            params.put("shiftAssignmentId", a.getShiftAssignmentId());
            params.put("employeeId", a.getEmployeeId());
            params.put("shiftId", a.getShiftId());

            neo4jClient.query("""
                    MATCH (e:Employee {employeeId: $employeeId})
                    MATCH (sa:ShiftAssignment {shiftAssignmentId: $shiftAssignmentId})
                    MATCH (sh:Shift {shiftId: $shiftId})
                    MERGE (e)-[:ASSIGNED_TO_SHIFT]->(sa)
                    MERGE (sa)-[:COVER_SHIFT_ASSIGNMENT]->(sh)
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createShiftSwapRelationships() {
        shiftSwapRepository.findAll().forEach(swap -> {
            Map<String, Object> params = new HashMap<>();
            params.put("shiftSwapId", swap.getShiftSwapId());
            params.put("employeeFromId", swap.getEmployeeFromId());
            params.put("employeeToId", swap.getEmployeeToId());
            params.put("originalShiftAssignmentId", swap.getOriginalShiftAssignmentId());

            neo4jClient.query("""
                    MATCH (ss:ShiftSwap {shiftSwapId: $shiftSwapId})
                    MATCH (from:Employee {employeeId: $employeeFromId})
                    MATCH (to:Employee {employeeId: $employeeToId})
                    MATCH (sa:ShiftAssignment {shiftAssignmentId: $originalShiftAssignmentId})
                    MERGE (ss)-[:SWAP_FROM_EMPLOYEE]->(from)
                    MERGE (ss)-[:SWAP_TO_EMPLOYEE]->(to)
                    MERGE (ss)-[:ASSIGNMENT_GETTING_SWAPPED]->(sa)
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createLeaveRequestRelationships() {
        leaveRequestRepository.findAll().forEach(request -> {
            Map<String, Object> params = new HashMap<>();
            params.put("employeeId", request.getEmployeeId());
            params.put("leaveRequestId", request.getLeaveRequestId());
            params.put("leaveTypeId", request.getLeaveTypeId());

            neo4jClient.query("""
                    MATCH (e:Employee {employeeId: $employeeId})
                    MATCH (lr:LeaveRequest {leaveRequestId: $leaveRequestId})
                    MATCH (lt:LeaveType {leaveTypeId: $leaveTypeId})
                    MERGE (e)-[:REQUESTED_LEAVE]->(lr)
                    MERGE (lr)-[:OF_LEAVE_TYPE]->(lt)
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createLeaveApprovalRelationships() {
        leaveApprovalRepository.findAll().forEach(approval -> {
            Map<String, Object> params = new HashMap<>();
            params.put("leaveRequestId", approval.getLeaveRequestId());
            params.put("leaveApprovalId", approval.getLeaveApprovalId());
            params.put("approverEmployeeId", approval.getApproverEmployeeId());

            neo4jClient.query("""
                    MATCH (la:LeaveApproval {leaveApprovalId: $leaveApprovalId})
                    MATCH (lr:LeaveRequest {leaveRequestId: $leaveRequestId})
                    MATCH (approver:Employee {employeeId: $approverEmployeeId})
                    OPTIONAL MATCH (requester:Employee {employeeId: lr.employeeId})
                    MERGE (la)-[:REVIEWS_LEAVE_REQUEST]->(lr)
                    MERGE (approver)-[:APPROVES_LEAVE]->(la)
                    WITH la, requester WHERE requester IS NOT NULL
                    MERGE (la)-[:CONCERNS_EMPLOYEE]->(requester)
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createLeaveLedgerRelationships() {
        leaveLedgerRepository.findAll().forEach(ledger -> {
            Map<String, Object> params = new HashMap<>();
            params.put("leaveLedgerId", ledger.getLeaveLedgerId());
            params.put("employeeId", ledger.getEmployeeId());
            params.put("leaveTypeId", ledger.getLeaveTypeId());

            neo4jClient.query("""
                    MATCH (e:Employee {employeeId: $employeeId})
                    MATCH (ll:LeaveLedger {leaveLedgerId: $leaveLedgerId})
                    MATCH (lt:LeaveType {leaveTypeId: $leaveTypeId})
                    MERGE (e)-[:ENTITLED_TO_LEAVE]->(ll)
                    MERGE (ll)-[:TRACKS_LEAVE_TYPE]->(lt)
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createEmployeeContractRelationships() {
        employeeContractRepository.findAll().forEach(contract -> {
            Map<String, Object> params = new HashMap<>();
            params.put("contractId", contract.getContractId());
            params.put("employeeId", contract.getEmployeeId());
            params.put("departmentId", contract.getDepartmentId());

            neo4jClient.query("""
                    MATCH (e:Employee {employeeId: $employeeId})
                    MATCH (ec:EmployeeContract {contractId: $contractId})
                    MATCH (d:Department {departmentId: $departmentId})
                    MERGE (e)-[:HAS_CONTRACT]->(ec)
                    MERGE (ec)-[:CONTRACT_IN_DEPT]->(d)
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createShiftApprovalRelationships() {
        shiftApprovalRepository.findAll().forEach(approval -> {
            Map<String, Object> params = new HashMap<>();
            params.put("shiftApprovalId", approval.getShiftApprovalId());
            params.put("shiftAssignmentId", approval.getShiftAssignmentId());
            params.put("approverEmployeeId", approval.getApproverEmployeeId());

            neo4jClient.query("""
                    MATCH (sa:ShiftAssignment {shiftAssignmentId: $shiftAssignmentId})
                    MATCH (sap:ShiftApproval {shiftApprovalId: $shiftApprovalId})
                    MATCH (approver:Employee {employeeId: $approverEmployeeId})
                    MERGE (sap)-[:NEED_APPROVAL]->(sa)
                    MERGE (approver)-[:APPROVED_BY_EMPLOYEE]->(sap)
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createShiftSwapApprovalRelationships() {
        shiftSwapApprovalRepository.findAll().forEach(approval -> {
            Map<String, Object> params = new HashMap<>();
            params.put("shiftSwapId", approval.getShiftSwapId());
            params.put("shiftSwapApprovalId", approval.getShiftSwapApprovalId());
            params.put("approverEmployeeId", approval.getApproverEmployeeId());

            neo4jClient.query("""
                    MATCH (ss:ShiftSwap {shiftSwapId: $shiftSwapId})
                    MATCH (ssa:ShiftSwapApproval {shiftSwapApprovalId: $shiftSwapApprovalId})
                    MATCH (approver:Employee {employeeId: $approverEmployeeId})
                    MERGE (ss)-[:HAS_SWAP_APPROVAL]->(ssa)
                    MERGE (ssa)-[:APPROVED_BY_EMPLOYEE]->(approver)
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private EmployeeNode toEmployeeNode(Employee e) {
        EmployeeNode node = new EmployeeNode();
        node.setEmployeeId(e.getEmployeeId());
        node.setEmployeeNumber(e.getEmployeeNumber());
        node.setFirstName(e.getFirstName());
        node.setLastName(e.getLastName());
        node.setEmail(e.getEmail());
        node.setFkUserRoleId(e.getFkUserRoleId());
        node.setPhoneNumber(e.getPhoneNumber());
        node.setHireDate(e.getHireDate());
        node.setEmploymentStatus(e.getEmploymentStatus());
        node.setPrimaryWorkLocationId(e.getPrimaryWorkLocationId());
        return node;
    }

    private DepartmentNode toDepartmentNode(Department d) {
        DepartmentNode node = new DepartmentNode();
        node.setDepartmentId(d.getDepartmentId());
        node.setDepartmentName(d.getDepartmentName());
        node.setIsActive(d.getIsActive());
        return node;
    }

    private WorkLocationNode toWorkLocationNode(WorkLocation w) {
        WorkLocationNode node = new WorkLocationNode();
        node.setWorkLocationId(w.getWorkLocationId());
        node.setLocationName(w.getLocationName());
        node.setCity(w.getCity());
        node.setCountry(w.getCountry());
        node.setTimezone(w.getTimezone());
        node.setIsActive(w.getIsActive());
        return node;
    }

    private ShiftNode toShiftNode(Shift s) {
        ShiftNode node = new ShiftNode();
        node.setShiftId(s.getShiftId());
        node.setShiftName(s.getShiftName());
        node.setStartDatetime(s.getStartDatetime());
        node.setEndDatetime(s.getEndDatetime());
        node.setShiftStatus(s.getShiftStatus());
        return node;
    }

    private JobRoleNode toJobRoleNode(JobRole jr) {
        JobRoleNode node = new JobRoleNode();
        node.setJobRoleId(jr.getJobRoleId());
        node.setRoleName(jr.getRoleName());
        node.setJobRoleDescription(jr.getJobRoleDescription());
        node.setIsCertificationRequired(jr.getIsCertificationRequired());
        return node;
    }

    private ShiftSwapNode toShiftSwapNode(ShiftSwap swap) {
        ShiftSwapNode node = new ShiftSwapNode();
        node.setShiftSwapId(swap.getShiftSwapId());
        node.setOriginalShiftAssignmentId(swap.getOriginalShiftAssignmentId());
        node.setEmployeeFromId(swap.getEmployeeFromId());
        node.setEmployeeToId(swap.getEmployeeToId());
        node.setSwapStatus(swap.getSwapStatus());
        node.setRequestDatetime(swap.getRequestDatetime());
        node.setReason(swap.getReason());
        return node;
    }
}
