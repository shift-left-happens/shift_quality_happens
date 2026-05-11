package dk.ek.shift_happens.shiftassignment;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.employeejobrole.EmployeeJobRoleRepository;
import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shift.ShiftService;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRole;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRoleRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShiftAssignmentService {

    public static final String STATUS_ASSIGNED = "Assigned";
    public static final String STATUS_CANCELLED = "Cancelled";
    public static final String STATUS_COMPLETED = "Completed";
    public static final long REST_HOURS = 11;

    private static final Set<String> VALID_STATUSES = Set.of(STATUS_ASSIGNED, STATUS_CANCELLED, STATUS_COMPLETED);
    private static final String EMPLOYMENT_ACTIVE = "Active";

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeJobRoleRepository employeeJobRoleRepository;
    private final ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;

    public List<ShiftAssignment> findAll() {
        return shiftAssignmentRepository.findAll();
    }

    public List<ShiftAssignment> findByEmployeeId(Integer employeeId) {
        return shiftAssignmentRepository.findByEmployeeId(employeeId);
    }

    public ShiftAssignment findById(Integer id) {
        return shiftAssignmentRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found with id " + id));
    }

    public ShiftAssignment assign(ShiftAssignment assignment) {
        validateForAssignment(assignment);

        assignment.setShiftAssignmentId(null);
        if (assignment.getAssignmentStatus() == null
                || assignment.getAssignmentStatus().isBlank()) {
            assignment.setAssignmentStatus(STATUS_ASSIGNED);
        } else if (!VALID_STATUSES.contains(assignment.getAssignmentStatus())) {
            throw new IllegalArgumentException("assignmentStatus must be one of " + VALID_STATUSES);
        }
        if (assignment.getAssignedDatetime() == null) {
            assignment.setAssignedDatetime(LocalDateTime.now());
        }

        return shiftAssignmentRepository.save(assignment);
    }

    public ShiftAssignment update(Integer id, ShiftAssignment updates) {
        ShiftAssignment existing = findById(id);

        if (updates.getAssignmentStatus() != null) {
            if (!VALID_STATUSES.contains(updates.getAssignmentStatus())) {
                throw new IllegalArgumentException("assignmentStatus must be one of " + VALID_STATUSES);
            }
            existing.setAssignmentStatus(updates.getAssignmentStatus());
        }
        if (updates.getCheckInDatetime() != null) existing.setCheckInDatetime(updates.getCheckInDatetime());
        if (updates.getCheckOutDatetime() != null) existing.setCheckOutDatetime(updates.getCheckOutDatetime());

        Shift shift = shiftRepository.findById(existing.getShiftId()).orElse(null);
        if (shift != null) {
            validateCheckTimes(existing, shift);
        }

        return shiftAssignmentRepository.save(existing);
    }

    public void delete(Integer id) {
        ShiftAssignment existing = findById(id);
        shiftAssignmentRepository.delete(existing);
    }

    private void validateForAssignment(ShiftAssignment assignment) {
        if (assignment.getShiftId() == null || assignment.getShiftId() <= 0) {
            throw new IllegalArgumentException("shiftId is required and must be positive");
        }
        Shift shift = shiftRepository
                .findById(assignment.getShiftId())
                .orElseThrow(() -> new IllegalArgumentException("shiftId " + assignment.getShiftId() + " not found"));

        if (!ShiftService.STATUS_OPEN.equalsIgnoreCase(shift.getShiftStatus())) {
            throw new IllegalArgumentException(
                    "Shift is not open for assignment (status: " + shift.getShiftStatus() + ")");
        }

        if (assignment.getEmployeeId() == null || assignment.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("employeeId is required and must be positive");
        }
        Employee employee = employeeRepository
                .findById(assignment.getEmployeeId())
                .orElseThrow(
                        () -> new IllegalArgumentException("employeeId " + assignment.getEmployeeId() + " not found"));

        if (employee.getEmploymentStatus() != null
                && !EMPLOYMENT_ACTIVE.equalsIgnoreCase(employee.getEmploymentStatus())) {
            throw new IllegalArgumentException(
                    "Employee is not active (status: " + employee.getEmploymentStatus() + ")");
        }

        if (hasOverlapOrInsufficientRest(assignment.getEmployeeId(), shift, null)) {
            throw new IllegalArgumentException(
                    "Assignment overlaps an existing shift or violates the " + REST_HOURS + " hour rest period");
        }

        if (!employeeHasRequiredRole(shift.getShiftId(), assignment.getEmployeeId())) {
            throw new IllegalArgumentException("Employee does not hold any of the required job roles for this shift");
        }
    }

    public boolean hasOverlapOrInsufficientRest(Integer employeeId, Shift candidateShift, Integer ignoreAssignmentId) {
        List<ShiftAssignment> employeeAssignments = shiftAssignmentRepository.findByEmployeeId(employeeId);
        for (ShiftAssignment existing : employeeAssignments) {
            if (ignoreAssignmentId != null && ignoreAssignmentId.equals(existing.getShiftAssignmentId())) {
                continue;
            }
            if (STATUS_CANCELLED.equalsIgnoreCase(existing.getAssignmentStatus())) {
                continue;
            }
            Optional<Shift> otherOpt = shiftRepository.findById(existing.getShiftId());
            if (otherOpt.isEmpty()) continue;
            Shift other = otherOpt.get();
            if (ShiftService.STATUS_CANCELLED.equalsIgnoreCase(other.getShiftStatus())) {
                continue;
            }
            if (overlaps(candidateShift, other) || violatesRest(candidateShift, other)) {
                return true;
            }
        }
        return false;
    }

    private boolean overlaps(Shift a, Shift b) {
        return a.getStartDatetime().isBefore(b.getEndDatetime())
                && b.getStartDatetime().isBefore(a.getEndDatetime());
    }

    private boolean violatesRest(Shift a, Shift b) {
        LocalDateTime gapStart;
        LocalDateTime gapEnd;
        if (!a.getEndDatetime().isAfter(b.getStartDatetime())) {
            gapStart = a.getEndDatetime();
            gapEnd = b.getStartDatetime();
        } else if (!b.getEndDatetime().isAfter(a.getStartDatetime())) {
            gapStart = b.getEndDatetime();
            gapEnd = a.getStartDatetime();
        } else {
            return false;
        }
        return Duration.between(gapStart, gapEnd).toMinutes() < REST_HOURS * 60;
    }

    private boolean employeeHasRequiredRole(Integer shiftId, Integer employeeId) {
        List<ShiftRequiredJobRole> required = shiftRequiredJobRoleRepository.findByShiftId(shiftId);
        if (required.isEmpty()) {
            return true;
        }
        Set<Integer> employeeRoleIds = new HashSet<>();
        employeeJobRoleRepository.findByEmployeeId(employeeId).forEach(r -> employeeRoleIds.add(r.getJobRoleId()));
        return required.stream().anyMatch(r -> employeeRoleIds.contains(r.getJobRoleId()));
    }

    private void validateCheckTimes(ShiftAssignment assignment, Shift shift) {
        LocalDateTime in = assignment.getCheckInDatetime();
        LocalDateTime out = assignment.getCheckOutDatetime();
        if (in != null && in.isAfter(shift.getEndDatetime())) {
            throw new IllegalArgumentException("checkInDatetime cannot be after shift end");
        }
        if (in != null && out != null && out.isBefore(in)) {
            throw new IllegalArgumentException("checkOutDatetime cannot be before checkInDatetime");
        }
    }
}
