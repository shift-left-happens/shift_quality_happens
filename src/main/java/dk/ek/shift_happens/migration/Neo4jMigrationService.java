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
import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shift.neo4j.ShiftNode;
import dk.ek.shift_happens.shift.neo4j.ShiftNeo4jRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRoleRepository;
import dk.ek.shift_happens.shiftswap.ShiftSwap;
import dk.ek.shift_happens.shiftswap.ShiftSwapRepository;
import dk.ek.shift_happens.shiftswap.neo4j.ShiftSwapNeo4jRepository;
import dk.ek.shift_happens.shiftswap.neo4j.ShiftSwapNode;
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

    private final EmployeeNeo4jRepository employeeNeo4jRepository;
    private final DepartmentNeo4jRepository departmentNeo4jRepository;
    private final WorkLocationNeo4jRepository workLocationNeo4jRepository;
    private final ShiftNeo4jRepository shiftNeo4jRepository;
    private final JobRoleNeo4jRepository jobRoleNeo4jRepository;
    private final ShiftSwapNeo4jRepository shiftSwapNeo4jRepository;
    private final Neo4jClient neo4jClient;

    public MigrationService.MigrationResult migrateToNeo4j() {
        List<String> errors = new ArrayList<>();
        int employees = 0, departments = 0, workLocations = 0, shifts = 0, jobRoles = 0, shiftSwaps = 0;
        try { employees     = migrateEmployeesToNeo4j(); }     catch (Exception e) { log.error("neo4j:employees failed",     e); errors.add("neo4j:employees — "     + e.getMessage()); }
        try { departments   = migrateDepartmentsToNeo4j(); }   catch (Exception e) { log.error("neo4j:departments failed",   e); errors.add("neo4j:departments — "   + e.getMessage()); }
        try { workLocations = migrateWorkLocationsToNeo4j(); } catch (Exception e) { log.error("neo4j:worklocations failed", e); errors.add("neo4j:worklocations — " + e.getMessage()); }
        try { shifts        = migrateShiftsToNeo4j(); }        catch (Exception e) { log.error("neo4j:shifts failed",        e); errors.add("neo4j:shifts — "        + e.getMessage()); }
        try { jobRoles      = migrateJobRolesToNeo4j(); }      catch (Exception e) { log.error("neo4j:jobroles failed",      e); errors.add("neo4j:jobroles — "      + e.getMessage()); }
        try { shiftSwaps    = migrateShiftSwapsToNeo4j(); }    catch (Exception e) { log.error("neo4j:shiftswaps failed",    e); errors.add("neo4j:shiftswaps — "    + e.getMessage()); }
        try { createNeo4jRelationships(); }                    catch (Exception e) { log.error("neo4j:relationships failed", e); errors.add("neo4j:relationships — " + e.getMessage()); }
        return new MigrationService.MigrationResult(0, 0, 0, 0, employees, departments, workLocations, shifts, jobRoles, shiftSwaps, errors);
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

    private void createNeo4jRelationships() {
        createEmployeeWorkLocationRelationships();
        createEmployeeDepartmentRelationships();
        createEmployeeJobRoleRelationships();
        createShiftDepartmentRelationships();
        createShiftLocationRelationships();
        createShiftRequiredRoleRelationships();
        createShiftAssignmentRelationships();
        createShiftSwapRelationships();
    }

    private void createEmployeeWorkLocationRelationships() {
        neo4jClient.query("""
                MATCH (e:Employee)
                MATCH (w:WorkLocation {workLocationId: e.primaryWorkLocationId})
                MERGE (e)-[r:WORKS_AT]->(w)
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
                            MERGE (e)-[r:WORKS_IN]->(d)
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
                    MERGE (e)-[r:HAS_ROLE]->(j)
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
                MERGE (sh)-[:IN_DEPARTMENT]->(d)
                """)
                .bind(s.getShiftId()).to("shiftId")
                .bind(s.getDepartmentId()).to("departmentId")
                .run());
    }

    private void createShiftLocationRelationships() {
        shiftRepository.findAll().forEach(s -> neo4jClient.query("""
                MATCH (sh:Shift {shiftId: $shiftId})
                MATCH (w:WorkLocation {workLocationId: $workLocationId})
                MERGE (sh)-[:AT_LOCATION]->(w)
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
            params.put("employeeId", a.getEmployeeId());
            params.put("shiftId", a.getShiftId());
            params.put("assignmentStatus", a.getAssignmentStatus());
            params.put("assignedDatetime", a.getAssignedDatetime());
            params.put("checkInDatetime", a.getCheckInDatetime());
            params.put("checkOutDatetime", a.getCheckOutDatetime());

            neo4jClient.query("""
                    MATCH (e:Employee {employeeId: $employeeId})
                    MATCH (sh:Shift {shiftId: $shiftId})
                    MERGE (e)-[r:ASSIGNED_TO]->(sh)
                    SET r.assignmentStatus = $assignmentStatus,
                        r.assignedDatetime = $assignedDatetime,
                        r.checkInDatetime = $checkInDatetime,
                        r.checkOutDatetime = $checkOutDatetime
                    """)
                    .bindAll(params)
                    .run();
        });
    }

    private void createShiftSwapRelationships() {
        Map<Integer, ShiftAssignment> assignmentsById = shiftAssignmentRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(ShiftAssignment::getShiftAssignmentId, a -> a));

        shiftSwapRepository.findAll().forEach(swap -> {
            ShiftAssignment assignment = assignmentsById.get(swap.getOriginalShiftAssignmentId());
            if (assignment == null) {
                return;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("shiftSwapId", swap.getShiftSwapId());
            params.put("employeeFromId", swap.getEmployeeFromId());
            params.put("employeeToId", swap.getEmployeeToId());
            params.put("shiftId", assignment.getShiftId());

            neo4jClient.query("""
                    MATCH (ss:ShiftSwap {shiftSwapId: $shiftSwapId})
                    MATCH (from:Employee {employeeId: $employeeFromId})
                    MATCH (to:Employee {employeeId: $employeeToId})
                    MATCH (sh:Shift {shiftId: $shiftId})
                    MERGE (ss)-[:FROM_EMPLOYEE]->(from)
                    MERGE (ss)-[:TO_EMPLOYEE]->(to)
                    MERGE (ss)-[:FOR_SHIFT]->(sh)
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
