package dk.ek.shift_happens.shiftswap;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shift.ShiftService;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShiftSwapService {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_APPROVED = "Approved";
    public static final String STATUS_DECLINED = "Declined";
    public static final String STATUS_CANCELLED = "Cancelled";
    public static final int REASON_MAX_LENGTH = 500;

    private static final Set<String> VALID_STATUSES =
            Set.of(STATUS_PENDING, STATUS_APPROVED, STATUS_DECLINED, STATUS_CANCELLED);
    private static final String EMPLOYMENT_ACTIVE = "Active";

    private final ShiftSwapRepository shiftSwapRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;

    public List<ShiftSwap> findAll() {
        return shiftSwapRepository.findAll();
    }

    public List<ShiftSwap> findByEmployee(Integer employeeId) {
        return shiftSwapRepository.findByEmployeeFromIdOrEmployeeToId(employeeId, employeeId);
    }

    public ShiftSwap findById(Integer id) {
        return shiftSwapRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift swap not found with id " + id));
    }

    public ShiftSwap create(ShiftSwap swap) {
        if (swap.getOriginalShiftAssignmentId() == null || swap.getOriginalShiftAssignmentId() <= 0) {
            throw new IllegalArgumentException("originalShiftAssignmentId is required and must be positive");
        }
        ShiftAssignment original = shiftAssignmentRepository
                .findById(swap.getOriginalShiftAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "originalShiftAssignmentId " + swap.getOriginalShiftAssignmentId() + " not found"));

        if (ShiftAssignmentService.STATUS_CANCELLED.equalsIgnoreCase(original.getAssignmentStatus())) {
            throw new IllegalArgumentException("Cannot create swap for a cancelled assignment");
        }

        Shift originalShift = shiftRepository
                .findById(original.getShiftId())
                .orElseThrow(() -> new IllegalArgumentException("Original shift not found"));
        if (ShiftService.STATUS_CANCELLED.equalsIgnoreCase(originalShift.getShiftStatus())) {
            throw new IllegalArgumentException("Cannot create swap for a cancelled shift");
        }
        if (originalShift.getStartDatetime() != null
                && !originalShift.getStartDatetime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot create swap once the original shift has started");
        }

        if (swap.getEmployeeFromId() == null || swap.getEmployeeFromId() <= 0) {
            throw new IllegalArgumentException("employeeFromId is required and must be positive");
        }
        if (!swap.getEmployeeFromId().equals(original.getEmployeeId())) {
            throw new IllegalArgumentException("employeeFromId must own the original assignment");
        }

        if (swap.getEmployeeToId() == null || swap.getEmployeeToId() <= 0) {
            throw new IllegalArgumentException("employeeToId is required and must be positive");
        }
        Employee employeeTo = employeeRepository
                .findById(swap.getEmployeeToId())
                .orElseThrow(
                        () -> new IllegalArgumentException("employeeToId " + swap.getEmployeeToId() + " not found"));
        if (employeeTo.getEmploymentStatus() != null
                && !EMPLOYMENT_ACTIVE.equalsIgnoreCase(employeeTo.getEmploymentStatus())) {
            throw new IllegalArgumentException("Target employee is not active");
        }

        if (swap.getEmployeeFromId().equals(swap.getEmployeeToId())) {
            throw new IllegalArgumentException("employeeFromId and employeeToId must differ");
        }

        boolean duplicatePending = !shiftSwapRepository
                .findByOriginalShiftAssignmentIdAndSwapStatusIgnoreCase(original.getShiftAssignmentId(), STATUS_PENDING)
                .isEmpty();
        if (duplicatePending) {
            throw new IllegalArgumentException("A pending swap already exists for this assignment");
        }

        validateReason(swap.getReason());

        swap.setShiftSwapId(null);
        swap.setSwapStatus(STATUS_PENDING);
        if (swap.getRequestDatetime() == null) {
            swap.setRequestDatetime(LocalDateTime.now());
        }
        if (swap.getRequestDatetime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("requestDatetime cannot be in the future");
        }

        return shiftSwapRepository.save(swap);
    }

    public ShiftSwap cancel(Integer id, Integer cancellingEmployeeId) {
        ShiftSwap swap = findById(id);
        if (!STATUS_PENDING.equalsIgnoreCase(swap.getSwapStatus())) {
            throw new IllegalArgumentException("Only pending swaps can be cancelled");
        }
        if (cancellingEmployeeId != null && !cancellingEmployeeId.equals(swap.getEmployeeFromId())) {
            throw new IllegalArgumentException("Only the requesting employee can cancel the swap");
        }
        swap.setSwapStatus(STATUS_CANCELLED);
        return shiftSwapRepository.save(swap);
    }

    public ShiftSwap update(Integer id, ShiftSwap updates) {
        ShiftSwap existing = findById(id);

        if (updates.getReason() != null) {
            validateReason(updates.getReason());
            existing.setReason(updates.getReason());
        }
        if (updates.getSwapStatus() != null) {
            if (!VALID_STATUSES.contains(updates.getSwapStatus())) {
                throw new IllegalArgumentException("swapStatus must be one of " + VALID_STATUSES);
            }
            assertTransitionAllowed(existing.getSwapStatus(), updates.getSwapStatus());
            existing.setSwapStatus(updates.getSwapStatus());
        }
        return shiftSwapRepository.save(existing);
    }

    public void delete(Integer id) {
        ShiftSwap swap = findById(id);
        shiftSwapRepository.delete(swap);
    }

    private void assertTransitionAllowed(String from, String to) {
        if (from == null) return;
        if (from.equalsIgnoreCase(to)) {
            throw new IllegalArgumentException("Swap is already in status " + from);
        }
        if (!STATUS_PENDING.equalsIgnoreCase(from)) {
            throw new IllegalArgumentException(
                    "Cannot transition swap from " + from + " to " + to + "; only Pending swaps may change status");
        }
    }

    private void validateReason(String reason) {
        if (reason == null) return;
        if (reason.isEmpty()) return;
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("reason must not be only whitespace");
        }
        if (reason.length() > REASON_MAX_LENGTH) {
            throw new IllegalArgumentException("reason must not exceed " + REASON_MAX_LENGTH + " characters");
        }
        if (reason.contains("<") || reason.contains(">")) {
            throw new IllegalArgumentException("reason contains unsafe content");
        }
    }
}
