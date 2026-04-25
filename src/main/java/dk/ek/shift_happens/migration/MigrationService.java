package dk.ek.shift_happens.migration;

import dk.ek.shift_happens.auditlog.AuditLog;
import dk.ek.shift_happens.auditlog.AuditLogRepository;
import dk.ek.shift_happens.auditlog.mongo.AuditLogDocument;
import dk.ek.shift_happens.auditlog.mongo.AuditLogMongoRepository;
import dk.ek.shift_happens.department.Department;
import dk.ek.shift_happens.department.DepartmentRepository;
import dk.ek.shift_happens.department.mongo.DepartmentDocument;
import dk.ek.shift_happens.department.mongo.DepartmentMongoRepository;
import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.employee.mongo.EmployeeDocument;
import dk.ek.shift_happens.employee.mongo.EmployeeMongoRepository;
import dk.ek.shift_happens.employeecontract.EmployeeContract;
import dk.ek.shift_happens.employeecontract.EmployeeContractRepository;
import dk.ek.shift_happens.employeejobrole.EmployeeJobRole;
import dk.ek.shift_happens.employeejobrole.EmployeeJobRoleRepository;
import dk.ek.shift_happens.jobrole.JobRole;
import dk.ek.shift_happens.jobrole.JobRoleRepository;
import dk.ek.shift_happens.jobrole.mongo.JobRoleDocument;
import dk.ek.shift_happens.jobrole.mongo.JobRoleMongoRepository;
import dk.ek.shift_happens.leaveapproval.LeaveApproval;
import dk.ek.shift_happens.leaveapproval.LeaveApprovalRepository;
import dk.ek.shift_happens.leaveledger.LeaveLedger;
import dk.ek.shift_happens.leaveledger.LeaveLedgerRepository;
import dk.ek.shift_happens.leaverequest.LeaveRequest;
import dk.ek.shift_happens.leaverequest.LeaveRequestRepository;
import dk.ek.shift_happens.leaverequest.mongo.LeaveDocument;
import dk.ek.shift_happens.leaverequest.mongo.LeaveMongoRepository;
import dk.ek.shift_happens.leavetype.LeaveType;
import dk.ek.shift_happens.leavetype.LeaveTypeRepository;
import dk.ek.shift_happens.leavetype.mongo.LeaveTypeDocument;
import dk.ek.shift_happens.leavetype.mongo.LeaveTypeMongoRepository;
import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shift.mongo.ShiftDocument;
import dk.ek.shift_happens.shift.mongo.ShiftMongoRepository;
import dk.ek.shift_happens.shiftapproval.ShiftApproval;
import dk.ek.shift_happens.shiftapproval.ShiftApprovalRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRole;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRoleRepository;
import dk.ek.shift_happens.shiftswap.ShiftSwap;
import dk.ek.shift_happens.shiftswap.ShiftSwapRepository;
import dk.ek.shift_happens.shiftswapapproval.ShiftSwapApproval;
import dk.ek.shift_happens.shiftswapapproval.ShiftSwapApprovalRepository;
import dk.ek.shift_happens.userrole.UserRole;
import dk.ek.shift_happens.userrole.UserRoleRepository;
import dk.ek.shift_happens.userrole.mongo.UserRoleDocument;
import dk.ek.shift_happens.userrole.mongo.UserRoleMongoRepository;
import dk.ek.shift_happens.worklocation.WorkLocation;
import dk.ek.shift_happens.worklocation.WorkLocationRepository;
import dk.ek.shift_happens.worklocation.mongo.WorkLocationDocument;
import dk.ek.shift_happens.worklocation.mongo.WorkLocationMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates data migration from MySQL into MongoDB and Neo4j.
 *
 * Mongo-specific mapping stays here, while graph-specific work lives in
 * Neo4jMigrationService to keep responsibilities separated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationService {

    // MySQL repositories (reads)
    private final AuditLogRepository auditLogRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final EmployeeJobRoleRepository employeeJobRoleRepository;
    private final JobRoleRepository jobRoleRepository;
    private final WorkLocationRepository workLocationRepository;
    private final UserRoleRepository userRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftApprovalRepository shiftApprovalRepository;
    private final ShiftSwapRepository shiftSwapRepository;
    private final ShiftSwapApprovalRepository shiftSwapApprovalRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveLedgerRepository leaveLedgerRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    // MongoDB repositories (writes)
    private final AuditLogMongoRepository auditLogMongoRepository;
    private final EmployeeMongoRepository employeeMongoRepository;
    private final ShiftMongoRepository shiftMongoRepository;
    private final DepartmentMongoRepository departmentMongoRepository;
    private final LeaveMongoRepository leaveMongoRepository;
    private final JobRoleMongoRepository jobRoleMongoRepository;
    private final WorkLocationMongoRepository workLocationMongoRepository;
    private final UserRoleMongoRepository userRoleMongoRepository;
    private final LeaveTypeMongoRepository leaveTypeMongoRepository;

    // Neo4j migration orchestration
    private final Neo4jMigrationService neo4jMigrationService;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public MigrationResult migrateAll() {
        MigrationResult mongo = migrateToMongo();
        MigrationResult neo4j = migrateToNeo4j();
        List<String> allErrors = new ArrayList<>(mongo.errors());
        allErrors.addAll(neo4j.errors());
        return new MigrationResult(
                mongo.employees(), mongo.shifts(), mongo.departments(), mongo.leaveDocuments(),
                neo4j.neo4jEmployees(), neo4j.neo4jDepartments(), neo4j.neo4jWorkLocations(),
                neo4j.neo4jShifts(), neo4j.neo4jJobRoles(), neo4j.neo4jShiftSwaps(), neo4j.neo4jShiftAssignments(),
                neo4j.neo4jLeaveTypes(), neo4j.neo4jLeaveRequests(), neo4j.neo4jLeaveApprovals(),
                neo4j.neo4jShiftApprovals(), neo4j.neo4jShiftSwapApprovals(),
                neo4j.neo4jLeaveLedgers(), neo4j.neo4jEmployeeContracts(),
                allErrors
        );
    }

    public MigrationResult migrateToMongo() {
        List<String> errors = new ArrayList<>();
        int auditLogs = 0, employees = 0, shifts = 0, departments = 0, leave = 0;

        try { auditLogs   = migrateAuditLogsToMongo(); }    catch (Exception e) { log.error("mongo:audit_logs failed",     e); errors.add("mongo:audit_logs — "     + e.getMessage()); }
        try { employees   = migrateEmployeesToMongo(); }    catch (Exception e) { log.error("mongo:employees failed",      e); errors.add("mongo:employees — "      + e.getMessage()); }
        try { shifts      = migrateShiftsToMongo(); }       catch (Exception e) { log.error("mongo:shifts failed",         e); errors.add("mongo:shifts — "         + e.getMessage()); }
        try { departments = migrateDepartmentsToMongo(); }  catch (Exception e) { log.error("mongo:departments failed",    e); errors.add("mongo:departments — "    + e.getMessage()); }
        try { leave       = migrateLeaveToMongo(); }        catch (Exception e) { log.error("mongo:leave failed",          e); errors.add("mongo:leave — "          + e.getMessage()); }
        try { migrateJobRolesToMongo(); }                   catch (Exception e) { log.error("mongo:job_roles failed",      e); errors.add("mongo:job_roles — "      + e.getMessage()); }
        try { migrateWorkLocationsToMongo(); }              catch (Exception e) { log.error("mongo:work_locations failed", e); errors.add("mongo:work_locations — " + e.getMessage()); }
        try { migrateUserRolesToMongo(); }                  catch (Exception e) { log.error("mongo:user_roles failed",     e); errors.add("mongo:user_roles — "     + e.getMessage()); }
        try { migrateLeaveTypesToMongo(); }                 catch (Exception e) { log.error("mongo:leave_types failed",    e); errors.add("mongo:leave_types — "    + e.getMessage()); }

        return new MigrationResult(employees, shifts, departments, leave, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, errors);
    }

    public MigrationResult migrateToNeo4j() {
        return neo4jMigrationService.migrateToNeo4j();
    }

    // -------------------------------------------------------------------------
    // MongoDB migration methods
    // -------------------------------------------------------------------------

    public int migrateAuditLogsToMongo() {


        auditLogMongoRepository.deleteAll();
        List<AuditLogDocument> docs = auditLogRepository.findAll().stream()
                .map(this::toAuditLogDocument)
                .toList();
        auditLogMongoRepository.saveAll(docs);
        return docs.size();
    }

    public int migrateEmployeesToMongo() {
        // Load lookup tables
        Map<Integer, Employee>   allEmployees  = index(employeeRepository.findAll(), Employee::getEmployeeId);
        Map<Integer, Department> departments   = index(departmentRepository.findAll(), Department::getDepartmentId);
        Map<Integer, WorkLocation> locations   = index(workLocationRepository.findAll(), WorkLocation::getWorkLocationId);
        Map<Integer, UserRole> userRoles       = index(userRoleRepository.findAll(), UserRole::getUserRoleId);
        Map<Integer, JobRole> jobRoles         = index(jobRoleRepository.findAll(), JobRole::getJobRoleId);

        Map<Integer, List<EmployeeContract>> contractsByEmployee =
                employeeContractRepository.findAll().stream()
                        .collect(Collectors.groupingBy(EmployeeContract::getEmployeeId));

        Map<Integer, List<EmployeeJobRole>> rolesByEmployee =
                employeeJobRoleRepository.findAll().stream()
                        .collect(Collectors.groupingBy(EmployeeJobRole::getEmployeeId));

        Map<Integer, List<LeaveRequest>> leaveRequestsByEmployee =
                leaveRequestRepository.findAll().stream()
                        .collect(Collectors.groupingBy(LeaveRequest::getEmployeeId));

        Map<Integer, List<LeaveApproval>> leaveApprovalsByRequest =
                leaveApprovalRepository.findAll().stream()
                        .collect(Collectors.groupingBy(LeaveApproval::getLeaveRequestId));

        Map<Integer, List<LeaveLedger>> leaveLedgerByEmployee =
                leaveLedgerRepository.findAll().stream()
                        .collect(Collectors.groupingBy(LeaveLedger::getEmployeeId));

        employeeMongoRepository.deleteAll();

        List<EmployeeDocument> docs = allEmployees.values().stream()
                .map(e -> toEmployeeDocument(e, departments, locations, userRoles, jobRoles,
                        contractsByEmployee, rolesByEmployee, leaveRequestsByEmployee, leaveApprovalsByRequest,
                        leaveLedgerByEmployee, allEmployees))
                .toList();

        employeeMongoRepository.saveAll(docs);
        return docs.size();
    }

    public int migrateShiftsToMongo() {
        Map<Integer, Department>  departments = index(departmentRepository.findAll(),  Department::getDepartmentId);
        Map<Integer, WorkLocation> locations  = index(workLocationRepository.findAll(), WorkLocation::getWorkLocationId);
        Map<Integer, JobRole>     jobRoles    = index(jobRoleRepository.findAll(),     JobRole::getJobRoleId);
        Map<Integer, Employee>    employees   = index(employeeRepository.findAll(),    Employee::getEmployeeId);

        Map<Integer, List<ShiftRequiredJobRole>> requiredByShift =
                shiftRequiredJobRoleRepository.findAll().stream()
                        .collect(Collectors.groupingBy(ShiftRequiredJobRole::getShiftId));

        Map<Integer, List<ShiftAssignment>> assignmentsByShift =
                shiftAssignmentRepository.findAll().stream()
                        .collect(Collectors.groupingBy(ShiftAssignment::getShiftId));

        Map<Integer, List<ShiftApproval>> approvalsByAssignment =
                shiftApprovalRepository.findAll().stream()
                        .collect(Collectors.groupingBy(ShiftApproval::getShiftAssignmentId));

        Map<Integer, List<ShiftSwap>> swapsByAssignment =
                shiftSwapRepository.findAll().stream()
                        .collect(Collectors.groupingBy(ShiftSwap::getOriginalShiftAssignmentId));

        Map<Integer, List<ShiftSwapApproval>> swapApprovalsBySwap =
                shiftSwapApprovalRepository.findAll().stream()
                        .collect(Collectors.groupingBy(ShiftSwapApproval::getShiftSwapId));

        shiftMongoRepository.deleteAll();

        List<ShiftDocument> docs = shiftRepository.findAll().stream()
                .map(s -> toShiftDocument(s, departments, locations, jobRoles, employees,
                        requiredByShift, assignmentsByShift, approvalsByAssignment,
                        swapsByAssignment, swapApprovalsBySwap))
                .toList();

        shiftMongoRepository.saveAll(docs);
        return docs.size();
    }

    public int migrateDepartmentsToMongo() {
        departmentMongoRepository.deleteAll();
        List<DepartmentDocument> docs = departmentRepository.findAll().stream()
                .map(this::toDepartmentDocument)
                .toList();
        departmentMongoRepository.saveAll(docs);
        return docs.size();
    }

    public int migrateLeaveToMongo() {
        Map<Integer, LeaveType> leaveTypes = index(leaveTypeRepository.findAll(), LeaveType::getLeaveTypeId);

        Map<Integer, List<LeaveRequest>> requestsByEmployee =
                leaveRequestRepository.findAll().stream()
                        .collect(Collectors.groupingBy(LeaveRequest::getEmployeeId));

        Map<Integer, List<LeaveApproval>> approvalsByRequest =
                leaveApprovalRepository.findAll().stream()
                        .collect(Collectors.groupingBy(LeaveApproval::getLeaveRequestId));

        leaveMongoRepository.deleteAll();

        List<LeaveDocument> docs = requestsByEmployee.entrySet().stream()
                .map(entry -> toLeaveDocument(entry.getKey(), entry.getValue(), leaveTypes, approvalsByRequest))
                .toList();

        leaveMongoRepository.saveAll(docs);
        return docs.size();
    }

    public int migrateJobRolesToMongo() {
        jobRoleMongoRepository.deleteAll();
        List<JobRoleDocument> docs = jobRoleRepository.findAll().stream()
                .map(this::toJobRoleDocument)
                .toList();
        jobRoleMongoRepository.saveAll(docs);
        return docs.size();
    }

    public int migrateWorkLocationsToMongo() {
        workLocationMongoRepository.deleteAll();
        List<WorkLocationDocument> docs = workLocationRepository.findAll().stream()
                .map(this::toWorkLocationDocument)
                .toList();
        workLocationMongoRepository.saveAll(docs);
        return docs.size();
    }

    public int migrateUserRolesToMongo() {
        userRoleMongoRepository.deleteAll();
        List<UserRoleDocument> docs = userRoleRepository.findAll().stream()
                .map(this::toUserRoleDocument)
                .toList();
        userRoleMongoRepository.saveAll(docs);
        return docs.size();
    }

    public int migrateLeaveTypesToMongo() {
        leaveTypeMongoRepository.deleteAll();
        List<LeaveTypeDocument> docs = leaveTypeRepository.findAll().stream()
                .map(this::toLeaveTypeDocument)
                .toList();
        leaveTypeMongoRepository.saveAll(docs);
        return docs.size();
    }

    // -------------------------------------------------------------------------
    // Mappers — MongoDB
    // -------------------------------------------------------------------------
    private AuditLogDocument toAuditLogDocument(AuditLog log) {
        AuditLogDocument doc = new AuditLogDocument();
        doc.setId(String.valueOf(log.getAuditLogId()));
        doc.setEntityType(log.getEntityType());
        doc.setEntityId(log.getEntityId());
        doc.setActionType(log.getActionType());
        doc.setDbUser(log.getDbUser());
        doc.setActionDatetime(log.getActionDatetime());
        doc.setOldValueSnapshot(log.getOldValueSnapshot());
        doc.setNewValueSnapshot(log.getNewValueSnapshot());
        return doc;
    }

    private EmployeeDocument toEmployeeDocument(
            Employee e,
            Map<Integer, Department> departments,
            Map<Integer, WorkLocation> locations,
            Map<Integer, UserRole> userRoles,
            Map<Integer, JobRole> jobRoles,
            Map<Integer, List<EmployeeContract>> contractsByEmployee,
            Map<Integer, List<EmployeeJobRole>> rolesByEmployee,
            Map<Integer, List<LeaveRequest>> leaveRequestsByEmployee,
            Map<Integer, List<LeaveApproval>> leaveApprovalsByRequest,
            Map<Integer, List<LeaveLedger>> leaveLedgerByEmployee,
            Map<Integer, Employee> allEmployees) {

        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployeeId(e.getEmployeeId());
        doc.setEmployeeNumber(e.getEmployeeNumber());
        doc.setFirstName(e.getFirstName());
        doc.setLastName(e.getLastName());
        doc.setEmail(e.getEmail());
        doc.setLoginPassword(e.getLoginPassword());
        doc.setPhoneNumber(e.getPhoneNumber());
        doc.setHireDate(e.getHireDate());
        doc.setEmploymentStatus(e.getEmploymentStatus());

        // Work Location
        WorkLocation loc = locations.get(e.getPrimaryWorkLocationId());
        if (loc != null) {
            EmployeeDocument.WorkLocation wl = new EmployeeDocument.WorkLocation();
            wl.setWorkLocationId(loc.getWorkLocationId());
            wl.setLocationName(loc.getLocationName());
            doc.setPrimaryWorkLocation(wl);
        }

        // User Role
        UserRole ur = userRoles.get(e.getFkUserRoleId());
        if (ur != null) {
            EmployeeDocument.UserRole role = new EmployeeDocument.UserRole();
            role.setRoleId(ur.getUserRoleId());
            role.setRoleName(ur.getUserRoleName());
            doc.setUserRole(role);
        }

        // Contracts
        doc.setEmployeeContracts(contractsByEmployee.getOrDefault(e.getEmployeeId(), List.of()).stream()
                .map(c -> {
                    EmployeeDocument.EmployeeContract contract = new EmployeeDocument.EmployeeContract();
                    Department d = departments.get(c.getDepartmentId());
                    if (d != null) {
                        EmployeeDocument.Department dept = new EmployeeDocument.Department();
                        dept.setDepartmentId(d.getDepartmentId());
                        dept.setDepartmentName(d.getDepartmentName());
                        contract.setDepartment(dept);
                    }
                    contract.setContractType(c.getContractType());
                    contract.setStartDate(c.getStartDate());
                    contract.setEndDate(c.getEndDate());
                    contract.setWeeklyHours(c.getWeeklyHours());
                    contract.setSalaryAmount(c.getSalaryAmount());
                    contract.setIsActive(c.getIsActive());
                    return contract;
                }).toList());

        // Job Roles
        doc.setJobRoles(rolesByEmployee.getOrDefault(e.getEmployeeId(), List.of()).stream()
                .map(ejr -> {
                    JobRole jr = jobRoles.get(ejr.getJobRoleId());
                    EmployeeDocument.JobRole jobRole = new EmployeeDocument.JobRole();
                    jobRole.setJobRoleId(String.valueOf(ejr.getJobRoleId()));
                    jobRole.setRoleName(jr != null ? jr.getRoleName() : null);
                    jobRole.setAssignedDate(ejr.getAssignedDate());
                    jobRole.setProficiencyLevel(ejr.getProficiencyLevel());
                    // Note: expiryDate is not in EmployeeJobRole MySQL entity based on typical patterns, 
                    // but it's in EmployeeDocument. Leaving null if not found.
                    return jobRole;
                }).toList());

        // Leave Requests
        doc.setLeaveRequests(leaveRequestsByEmployee.getOrDefault(e.getEmployeeId(), List.of()).stream()
                .map(lr -> {
                    EmployeeDocument.LeaveRequest req = new EmployeeDocument.LeaveRequest();
                    req.setLeaveTypeId(lr.getLeaveTypeId());
                    req.setStartDate(lr.getStartDate());
                    req.setEndDate(lr.getEndDate());
                    req.setRequestStatus(lr.getRequestStatus());
                    req.setReason(lr.getReason());
                    req.setRequestedDatetime(lr.getRequestedDatetime());

                    req.setApprovals(leaveApprovalsByRequest.getOrDefault(lr.getLeaveRequestId(), List.of()).stream()
                            .map(la -> {
                                EmployeeDocument.Approval app = new EmployeeDocument.Approval();
                                app.setDecision(la.getDecision());
                                app.setLeaveComment(la.getLeaveComment());
                                app.setDecisionDatetime(la.getDecisionDatetime());

                                Employee approver = allEmployees.get(la.getApproverEmployeeId());
                                if (approver != null) {
                                    EmployeeDocument.ApproverEmployee ae = new EmployeeDocument.ApproverEmployee();
                                    ae.setEmployeeId(approver.getEmployeeId());
                                    ae.setFirstName(approver.getFirstName());
                                    ae.setLastName(approver.getLastName());
                                    app.setApproverEmployee(ae);
                                }
                                return app;
                            }).toList());
                    return req;
                }).toList());

        // Leave Ledger
        doc.setLeaveLedger(leaveLedgerByEmployee.getOrDefault(e.getEmployeeId(), List.of()).stream()
                .map(ll -> {
                    EmployeeDocument.LeaveLedgerEntry entry = new EmployeeDocument.LeaveLedgerEntry();
                    entry.setLeaveTypeId(ll.getLeaveTypeId());
                    entry.setChangeAmountDays(ll.getChangeAmountDays());
                    entry.setTransactionType(ll.getTransactionType());
                    entry.setReferenceEntityType(ll.getReferenceEntityType());
                    entry.setReferenceEntityId(ll.getReferenceEntityId());
                    entry.setTransactionDatetime(ll.getTransactionDatetime());
                    return entry;
                }).toList());

        return doc;
    }

    private ShiftDocument toShiftDocument(
            Shift s,
            Map<Integer, Department> departments,
            Map<Integer, WorkLocation> locations,
            Map<Integer, JobRole> jobRoles,
            Map<Integer, Employee> employees,
            Map<Integer, List<ShiftRequiredJobRole>> requiredByShift,
            Map<Integer, List<ShiftAssignment>> assignmentsByShift,
            Map<Integer, List<ShiftApproval>> approvalsByAssignment,
            Map<Integer, List<ShiftSwap>> swapsByAssignment,
            Map<Integer, List<ShiftSwapApproval>> swapApprovalsBySwap) {

        ShiftDocument doc = new ShiftDocument();
        doc.setShiftId(s.getShiftId());
        doc.setShiftName(s.getShiftName());
        doc.setStartDateTime(s.getStartDatetime());
        doc.setEndDateTime(s.getEndDatetime());
        doc.setShiftStatus(s.getShiftStatus());

        Department dept = departments.get(s.getDepartmentId());
        if (dept != null) {
            ShiftDocument.Department dRef = new ShiftDocument.Department();
            dRef.setDepartmentId(dept.getDepartmentId());
            dRef.setDepartmentName(dept.getDepartmentName());
            doc.setDepartment(dRef);
        }

        WorkLocation loc = locations.get(s.getWorkLocationId());
        if (loc != null) {
            ShiftDocument.WorkLocation lRef = new ShiftDocument.WorkLocation();
            lRef.setWorkLocationId(loc.getWorkLocationId());
            lRef.setLocationName(loc.getLocationName());
            doc.setWorkLocation(lRef);
        }

        // Required roles
        Map<Integer, List<ShiftRequiredJobRole>> groupedRequired = requiredByShift.getOrDefault(s.getShiftId(), List.of()).stream()
                .collect(Collectors.groupingBy(ShiftRequiredJobRole::getRequiredEmployeeCount));

        doc.setRequiredJobRoles(groupedRequired.entrySet().stream()
                .map(entry -> {
                    ShiftDocument.RequiredJobRole req = new ShiftDocument.RequiredJobRole();
                    req.setRequiredEmployees(entry.getKey());
                    req.setJobRoles(entry.getValue().stream()
                            .map(r -> {
                                JobRole jr = jobRoles.get(r.getJobRoleId());
                                ShiftDocument.JobRole jrRef = new ShiftDocument.JobRole();
                                jrRef.setJobRoleId(r.getJobRoleId());
                                jrRef.setRoleName(jr != null ? jr.getRoleName() : null);
                                return jrRef;
                            }).toList());
                    return req;
                }).toList());

        // Assignments with nested approvals and swap requests
        doc.setShiftAssignments(assignmentsByShift.getOrDefault(s.getShiftId(), List.of()).stream()
                .map(a -> {
                    Employee emp = employees.get(a.getEmployeeId());
                    ShiftDocument.ShiftAssignment aRef = new ShiftDocument.ShiftAssignment();
                    
                    ShiftDocument.AssignedEmployee assignedEmp = new ShiftDocument.AssignedEmployee();
                    if (emp != null) {
                        assignedEmp.setEmployeeId(emp.getEmployeeId());
                        assignedEmp.setFirstName(emp.getFirstName());
                        assignedEmp.setLastName(emp.getLastName());
                    } else {
                        assignedEmp.setEmployeeId(a.getEmployeeId());
                    }
                    aRef.setAssignedEmployee(assignedEmp);
                    
                    aRef.setAssignmentStatus(a.getAssignmentStatus());
                    aRef.setAssignmentDate(a.getAssignedDatetime());
                    aRef.setCheckInDate(a.getCheckInDatetime());
                    aRef.setCheckOutDatetime(a.getCheckOutDatetime());

                    // Nested approvals for this assignment
                    aRef.setShiftApprovals(approvalsByAssignment.getOrDefault(a.getShiftAssignmentId(), List.of()).stream()
                            .map(ap -> {
                                ShiftDocument.ShiftApproval approval = new ShiftDocument.ShiftApproval();
                                Employee approver = employees.get(ap.getApproverEmployeeId());
                                ShiftDocument.AssignedEmployee appEmp = new ShiftDocument.AssignedEmployee();
                                if (approver != null) {
                                    appEmp.setEmployeeId(approver.getEmployeeId());
                                    appEmp.setFirstName(approver.getFirstName());
                                    appEmp.setLastName(approver.getLastName());
                                } else {
                                    appEmp.setEmployeeId(ap.getApproverEmployeeId());
                                }
                                approval.setApproverEmployee(appEmp);
                                approval.setDecision(ap.getDecision());
                                approval.setApprovalComment(ap.getApprovalComment());
                                approval.setDecisionDatetime(ap.getDecisionDatetime());
                                return approval;
                            }).toList());

                    // Nested swap requests for this assignment
                    aRef.setSwapRequests(swapsByAssignment.getOrDefault(a.getShiftAssignmentId(), List.of()).stream()
                            .map(sw -> {
                                ShiftDocument.SwapRequest swap = new ShiftDocument.SwapRequest();
                                Employee from = employees.get(sw.getEmployeeFromId());
                                Employee to = employees.get(sw.getEmployeeToId());
                                
                                ShiftDocument.AssignedEmployee fromEmp = new ShiftDocument.AssignedEmployee();
                                if (from != null) {
                                    fromEmp.setEmployeeId(from.getEmployeeId());
                                    fromEmp.setFirstName(from.getFirstName());
                                    fromEmp.setLastName(from.getLastName());
                                } else {
                                    fromEmp.setEmployeeId(sw.getEmployeeFromId());
                                }
                                swap.setEmployeeFrom(fromEmp);

                                ShiftDocument.AssignedEmployee toEmp = new ShiftDocument.AssignedEmployee();
                                if (to != null) {
                                    toEmp.setEmployeeId(to.getEmployeeId());
                                    toEmp.setFirstName(to.getFirstName());
                                    toEmp.setLastName(to.getLastName());
                                } else {
                                    toEmp.setEmployeeId(sw.getEmployeeToId());
                                }
                                swap.setEmployeeTo(toEmp);

                                swap.setSwapStatus(sw.getSwapStatus());
                                swap.setRequestDatetime(sw.getRequestDatetime());
                                swap.setReason(sw.getReason());

                                // Swap approvals
                                swap.setSwapApprovals(swapApprovalsBySwap.getOrDefault(sw.getShiftSwapId(), List.of()).stream()
                                        .map(sa -> {
                                            ShiftDocument.SwapApproval saRef = new ShiftDocument.SwapApproval();
                                            Employee approver = employees.get(sa.getApproverEmployeeId());
                                            ShiftDocument.AssignedEmployee appEmp = new ShiftDocument.AssignedEmployee();
                                            if (approver != null) {
                                                appEmp.setEmployeeId(approver.getEmployeeId());
                                                appEmp.setFirstName(approver.getFirstName());
                                                appEmp.setLastName(approver.getLastName());
                                            } else {
                                                appEmp.setEmployeeId(sa.getApproverEmployeeId());
                                            }
                                            saRef.setApproverEmployee(appEmp);
                                            saRef.setDecision(sa.getDecision());
                                            saRef.setSwapComment(sa.getShiftSwapComment());
                                            saRef.setDecisionDatetime(sa.getDecisionDatetime());
                                            return saRef;
                                        }).toList());
                                return swap;
                            }).toList());

                    return aRef;
                }).toList());

        return doc;
    }

    private JobRoleDocument toJobRoleDocument(JobRole jr) {
        JobRoleDocument doc = new JobRoleDocument();
        doc.setJobRoleId(jr.getJobRoleId());
        doc.setRoleName(jr.getRoleName());
        doc.setJobRoleDescription(jr.getJobRoleDescription());
        doc.setIsCertificationRequired(jr.getIsCertificationRequired());
        return doc;
    }

    private WorkLocationDocument toWorkLocationDocument(WorkLocation w) {
        WorkLocationDocument doc = new WorkLocationDocument();
        doc.setWorkLocationId(w.getWorkLocationId());
        doc.setLocationName(w.getLocationName());
        doc.setAddressLine1(w.getAddressLine1());
        doc.setAddressLine2(w.getAddressLine2());
        doc.setCity(w.getCity());
        doc.setCountry(w.getCountry());
        doc.setTimezone(w.getTimezone());
        doc.setIsActive(w.getIsActive());
        return doc;
    }

    private UserRoleDocument toUserRoleDocument(UserRole ur) {
        UserRoleDocument doc = new UserRoleDocument();
        doc.setUserRoleId(ur.getUserRoleId());
        doc.setUserRoleName(ur.getUserRoleName());
        return doc;
    }

    private LeaveTypeDocument toLeaveTypeDocument(LeaveType lt) {
        LeaveTypeDocument doc = new LeaveTypeDocument();
        doc.setLeaveTypeId(lt.getLeaveTypeId());
        doc.setLeaveTypeName(lt.getLeaveTypeName());
        doc.setLeaveTypeDescription(lt.getLeaveTypeDescription());
        doc.setRequiresApproval(lt.getRequiresApproval());
        doc.setIsPaidLeave(lt.getIsPaidLeave());
        return doc;
    }

    private DepartmentDocument toDepartmentDocument(Department d) {
        DepartmentDocument doc = new DepartmentDocument();
        doc.setDepartmentId(d.getDepartmentId());
        doc.setDepartmentName(d.getDepartmentName());
        doc.setIsActive(d.getIsActive());
        return doc;
    }

    private LeaveDocument toLeaveDocument(
            Integer employeeId,
            List<LeaveRequest> requests,
            Map<Integer, LeaveType> leaveTypes,
            Map<Integer, List<LeaveApproval>> approvalsByRequest) {

        LeaveDocument doc = new LeaveDocument();
        doc.setEmployeeId(employeeId);
        doc.setRequests(requests.stream().map(r -> {
            LeaveType lt = leaveTypes.get(r.getLeaveTypeId());
            LeaveDocument.LeaveRequestRef ref = new LeaveDocument.LeaveRequestRef();
            ref.setLeaveRequestId(r.getLeaveRequestId());
            ref.setLeaveTypeId(r.getLeaveTypeId());
            ref.setLeaveTypeName(lt != null ? lt.getLeaveTypeName() : null);
            ref.setStartDate(r.getStartDate());
            ref.setEndDate(r.getEndDate());
            ref.setRequestStatus(r.getRequestStatus());
            ref.setReason(r.getReason());
            ref.setRequestedAt(r.getRequestedDatetime());
            ref.setApprovals(approvalsByRequest.getOrDefault(r.getLeaveRequestId(), List.of()).stream()
                    .map(a -> {
                        LeaveDocument.ApprovalRef aRef = new LeaveDocument.ApprovalRef();
                        aRef.setApproverEmployeeId(a.getApproverEmployeeId());
                        aRef.setDecision(a.getDecision());
                        aRef.setComment(a.getLeaveComment());
                        aRef.setDecidedAt(a.getDecisionDatetime());
                        return aRef;
                    }).toList());
            return ref;
        }).toList());
        return doc;
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private <K, V> Map<K, V> index(List<V> list, java.util.function.Function<V, K> keyMapper) {
        return list.stream().collect(Collectors.toMap(keyMapper, v -> v));
    }
}

