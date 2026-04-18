package dk.ek.shift_happens.migration;

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
import dk.ek.shift_happens.leaveapproval.LeaveApproval;
import dk.ek.shift_happens.leaveapproval.LeaveApprovalRepository;
import dk.ek.shift_happens.leaverequest.LeaveRequest;
import dk.ek.shift_happens.leaverequest.LeaveRequestRepository;
import dk.ek.shift_happens.leaverequest.mongo.LeaveDocument;
import dk.ek.shift_happens.leaverequest.mongo.LeaveMongoRepository;
import dk.ek.shift_happens.leavetype.LeaveType;
import dk.ek.shift_happens.leavetype.LeaveTypeRepository;
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
import dk.ek.shift_happens.worklocation.WorkLocation;
import dk.ek.shift_happens.worklocation.WorkLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// One-time migrator: reads all data from MySQL (via JPA/Hibernate ORM) and writes
// it into MongoDB (ODM) and Neo4j (OGM).
//
// Strategy: load all SQL tables into memory as Maps keyed by their primary ID,
// then do all the joining/embedding in Java. This avoids N+1 queries and keeps
// the mapping logic readable.
//
// MongoDB collections: employees, shifts, departments, leave_requests
// Neo4j graph types:   Employee, Department, WorkLocation, Shift, JobRole
// Neo4j relationships: WORKS_AT, WORKS_IN, HAS_ROLE, IN_DEPARTMENT,
//                      AT_LOCATION, REQUIRES_ROLE, ASSIGNED_TO
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationService {

    // MySQL repositories (reads)
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
    private final LeaveTypeRepository leaveTypeRepository;

    // MongoDB repositories (writes)
    private final EmployeeMongoRepository employeeMongoRepository;
    private final ShiftMongoRepository shiftMongoRepository;
    private final DepartmentMongoRepository departmentMongoRepository;
    private final LeaveMongoRepository leaveMongoRepository;

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
                neo4j.neo4jShifts(), neo4j.neo4jJobRoles(), neo4j.neo4jShiftSwaps(),
                allErrors
        );
    }

    public MigrationResult migrateToMongo() {
        List<String> errors = new ArrayList<>();
        int employees = 0, shifts = 0, departments = 0, leave = 0;
        try { employees   = migrateEmployeesToMongo(); }   catch (Exception e) { log.error("mongo:employees failed",   e); errors.add("mongo:employees — "   + e.getMessage()); }
        try { shifts      = migrateShiftsToMongo(); }      catch (Exception e) { log.error("mongo:shifts failed",      e); errors.add("mongo:shifts — "      + e.getMessage()); }
        try { departments = migrateDepartmentsToMongo(); } catch (Exception e) { log.error("mongo:departments failed", e); errors.add("mongo:departments — " + e.getMessage()); }
        try { leave       = migrateLeaveToMongo(); }       catch (Exception e) { log.error("mongo:leave failed",       e); errors.add("mongo:leave — "       + e.getMessage()); }
        return new MigrationResult(employees, shifts, departments, leave, 0, 0, 0, 0, 0, 0, errors);
    }

    public MigrationResult migrateToNeo4j() {
        return neo4jMigrationService.migrateToNeo4j();
    }

    // -------------------------------------------------------------------------
    // MongoDB migration methods
    // -------------------------------------------------------------------------

    public int migrateEmployeesToMongo() {
        // Load lookup tables
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

        employeeMongoRepository.deleteAll();

        List<EmployeeDocument> docs = employeeRepository.findAll().stream()
                .map(e -> toEmployeeDocument(e, departments, locations, userRoles, jobRoles,
                        contractsByEmployee, rolesByEmployee))
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

    // -------------------------------------------------------------------------
    // Mappers — MongoDB
    // -------------------------------------------------------------------------

    private EmployeeDocument toEmployeeDocument(
            Employee e,
            Map<Integer, Department> departments,
            Map<Integer, WorkLocation> locations,
            Map<Integer, UserRole> userRoles,
            Map<Integer, JobRole> jobRoles,
            Map<Integer, List<EmployeeContract>> contractsByEmployee,
            Map<Integer, List<EmployeeJobRole>> rolesByEmployee) {

        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployeeId(e.getEmployeeId());
        doc.setEmployeeNumber(e.getEmployeeNumber());
        doc.setEmail(e.getEmail());
        doc.setPhone(e.getPhoneNumber());
        doc.setEmploymentStatus(e.getEmploymentStatus());
        doc.setHireDate(e.getHireDate());

        EmployeeDocument.Name name = new EmployeeDocument.Name();
        name.setFirst(e.getFirstName());
        name.setLast(e.getLastName());
        doc.setName(name);

        // Embed primary department from active contract
        contractsByEmployee.getOrDefault(e.getEmployeeId(), List.of()).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .findFirst()
                .ifPresent(c -> {
                    Department dept = departments.get(c.getDepartmentId());
                    if (dept != null) {
                        EmployeeDocument.DepartmentRef ref = new EmployeeDocument.DepartmentRef();
                        ref.setDepartmentId(dept.getDepartmentId());
                        ref.setName(dept.getDepartmentName());
                        doc.setDepartment(ref);
                    }
                });

        // Embed primary work location
        WorkLocation loc = locations.get(e.getPrimaryWorkLocationId());
        if (loc != null) {
            EmployeeDocument.WorkLocationRef wRef = new EmployeeDocument.WorkLocationRef();
            wRef.setWorkLocationId(loc.getWorkLocationId());
            wRef.setLocationName(loc.getLocationName());
            wRef.setCity(loc.getCity());
            wRef.setCountry(loc.getCountry());
            wRef.setTimezone(loc.getTimezone());
            wRef.setIsPrimary(true);
            doc.setWorkLocations(List.of(wRef));
        }

        // Embed user role
        UserRole ur = userRoles.get(e.getFkUserRoleId());
        if (ur != null) {
            EmployeeDocument.UserRoleRef urRef = new EmployeeDocument.UserRoleRef();
            urRef.setUserRoleId(ur.getUserRoleId());
            urRef.setRoleName(ur.getUserRoleName());
            doc.setUserRole(urRef);
        }

        // Embed job roles
        List<EmployeeDocument.JobRoleRef> jrRefs = rolesByEmployee
                .getOrDefault(e.getEmployeeId(), List.of()).stream()
                .map(ejr -> {
                    JobRole jr = jobRoles.get(ejr.getJobRoleId());
                    EmployeeDocument.JobRoleRef ref = new EmployeeDocument.JobRoleRef();
                    ref.setJobRoleId(ejr.getJobRoleId());
                    ref.setRoleName(jr != null ? jr.getRoleName() : null);
                    ref.setAssignedDate(ejr.getAssignedDate());
                    ref.setProficiencyLevel(ejr.getProficiencyLevel());
                    return ref;
                }).toList();
        doc.setJobRoles(jrRefs);

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
        doc.setStatus(s.getShiftStatus());

        Department dept = departments.get(s.getDepartmentId());
        if (dept != null) {
            ShiftDocument.DepartmentRef dRef = new ShiftDocument.DepartmentRef();
            dRef.setDepartmentId(dept.getDepartmentId());
            dRef.setName(dept.getDepartmentName());
            doc.setDepartment(dRef);
        }

        WorkLocation loc = locations.get(s.getWorkLocationId());
        if (loc != null) {
            ShiftDocument.WorkLocationRef lRef = new ShiftDocument.WorkLocationRef();
            lRef.setWorkLocationId(loc.getWorkLocationId());
            lRef.setLocationName(loc.getLocationName());
            doc.setWorkLocation(lRef);
        }

        // Required roles
        doc.setRequiredRoles(requiredByShift.getOrDefault(s.getShiftId(), List.of()).stream()
                .map(r -> {
                    JobRole jr = jobRoles.get(r.getJobRoleId());
                    ShiftDocument.RequiredRoleRef ref = new ShiftDocument.RequiredRoleRef();
                    ref.setJobRoleId(r.getJobRoleId());
                    ref.setRoleName(jr != null ? jr.getRoleName() : null);
                    ref.setRequiredEmployeeCount(r.getRequiredEmployeeCount());
                    return ref;
                }).toList());

        // Assignments with nested approvals and swap requests
        doc.setAssignments(assignmentsByShift.getOrDefault(s.getShiftId(), List.of()).stream()
                .map(a -> {
                    Employee emp = employees.get(a.getEmployeeId());
                    ShiftDocument.AssignmentRef aRef = new ShiftDocument.AssignmentRef();
                    aRef.setShiftAssignmentId(a.getShiftAssignmentId());
                    aRef.setEmployeeId(a.getEmployeeId());
                    aRef.setEmployeeName(emp != null ? emp.getFirstName() + " " + emp.getLastName() : null);
                    aRef.setAssignmentStatus(a.getAssignmentStatus());
                    aRef.setAssignedAt(a.getAssignedDatetime());
                    aRef.setCheckInAt(a.getCheckInDatetime());
                    aRef.setCheckOutAt(a.getCheckOutDatetime());
                    return aRef;
                }).toList());

        // Shift-level approvals (collected across all assignments)
        doc.setApprovals(assignmentsByShift.getOrDefault(s.getShiftId(), List.of()).stream()
                .flatMap(a -> approvalsByAssignment.getOrDefault(a.getShiftAssignmentId(), List.of()).stream())
                .map(ap -> {
                    ShiftDocument.ApprovalRef ref = new ShiftDocument.ApprovalRef();
                    ref.setShiftApprovalId(ap.getShiftApprovalId());
                    ref.setApproverEmployeeId(ap.getApproverEmployeeId());
                    ref.setDecision(ap.getDecision());
                    ref.setComment(ap.getApprovalComment());
                    ref.setDecidedAt(ap.getDecisionDatetime());
                    return ref;
                }).toList());

        // Swap requests with nested approvals
        doc.setSwapRequests(assignmentsByShift.getOrDefault(s.getShiftId(), List.of()).stream()
                .flatMap(a -> swapsByAssignment.getOrDefault(a.getShiftAssignmentId(), List.of()).stream())
                .map(sw -> {
                    Employee from = employees.get(sw.getEmployeeFromId());
                    Employee to   = employees.get(sw.getEmployeeToId());
                    ShiftDocument.SwapRequestRef ref = new ShiftDocument.SwapRequestRef();
                    ref.setShiftSwapId(sw.getShiftSwapId());
                    ref.setEmployeeFromId(sw.getEmployeeFromId());
                    ref.setEmployeeFromName(from != null ? from.getFirstName() + " " + from.getLastName() : null);
                    ref.setEmployeeToId(sw.getEmployeeToId());
                    ref.setEmployeeToName(to != null ? to.getFirstName() + " " + to.getLastName() : null);
                    ref.setSwapStatus(sw.getSwapStatus());
                    ref.setReason(sw.getReason());
                    ref.setRequestedAt(sw.getRequestDatetime());
                    ref.setApprovals(swapApprovalsBySwap.getOrDefault(sw.getShiftSwapId(), List.of()).stream()
                            .map(sa -> {
                                ShiftDocument.SwapApprovalRef saRef = new ShiftDocument.SwapApprovalRef();
                                saRef.setApproverEmployeeId(sa.getApproverEmployeeId());
                                saRef.setDecision(sa.getDecision());
                                saRef.setComment(sa.getShiftSwapComment());
                                saRef.setDecidedAt(sa.getDecisionDatetime());
                                return saRef;
                            }).toList());
                    return ref;
                }).toList());

        return doc;
    }

    private DepartmentDocument toDepartmentDocument(Department d) {
        DepartmentDocument doc = new DepartmentDocument();
        doc.setDepartmentId(d.getDepartmentId());
        doc.setName(d.getDepartmentName());
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

    // Return type summarising what was migrated.
    // Any step that throws has its count left at 0 and its error message in errors[].
    public record MigrationResult(
            int employees, int shifts, int departments, int leaveDocuments,
            int neo4jEmployees, int neo4jDepartments, int neo4jWorkLocations,
            int neo4jShifts, int neo4jJobRoles, int neo4jShiftSwaps,
            List<String> errors
    ) {}
}
