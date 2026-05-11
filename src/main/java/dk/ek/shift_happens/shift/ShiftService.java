package dk.ek.shift_happens.shift;

import dk.ek.shift_happens.department.DepartmentRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftswap.ShiftSwapRepository;
import dk.ek.shift_happens.worklocation.WorkLocationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShiftService {

    public static final int SHIFT_NAME_MAX_LENGTH = 100;
    public static final long MIN_DURATION_MINUTES = 60;
    public static final long MAX_DURATION_MINUTES = 12 * 60;

    public static final String STATUS_OPEN = "Open";
    public static final String STATUS_ASSIGNED = "Assigned";
    public static final String STATUS_PENDING_SWAP = "Pending Swap";
    public static final String STATUS_CANCELLED = "Cancelled";
    public static final String STATUS_COMPLETED = "Completed";

    private static final Set<String> VALID_STATUSES =
            Set.of(STATUS_OPEN, STATUS_ASSIGNED, STATUS_PENDING_SWAP, STATUS_CANCELLED, STATUS_COMPLETED);

    private final ShiftRepository shiftRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkLocationRepository workLocationRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftSwapRepository shiftSwapRepository;

    public List<Shift> findAll() {
        return shiftRepository.findAll();
    }

    public Shift findById(Integer id) {
        return shiftRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found with id " + id));
    }

    public Shift create(Shift shift) {
        validate(shift);
        shift.setShiftId(null);
        if (shift.getShiftStatus() == null || shift.getShiftStatus().isBlank()) {
            shift.setShiftStatus(STATUS_OPEN);
        }
        if (shift.getShiftName() != null) {
            shift.setShiftName(shift.getShiftName().trim());
        }
        return shiftRepository.save(shift);
    }

    public Shift update(Integer id, Shift updates) {
        Shift existing = findById(id);

        if (updates.getDepartmentId() != null) existing.setDepartmentId(updates.getDepartmentId());
        if (updates.getWorkLocationId() != null) existing.setWorkLocationId(updates.getWorkLocationId());
        if (updates.getShiftName() != null)
            existing.setShiftName(updates.getShiftName().trim());
        if (updates.getStartDatetime() != null) existing.setStartDatetime(updates.getStartDatetime());
        if (updates.getEndDatetime() != null) existing.setEndDatetime(updates.getEndDatetime());
        if (updates.getShiftStatus() != null) existing.setShiftStatus(updates.getShiftStatus());

        validate(existing);
        return shiftRepository.save(existing);
    }

    public Shift cancel(Integer id) {
        Shift existing = findById(id);
        if (STATUS_COMPLETED.equalsIgnoreCase(existing.getShiftStatus())) {
            throw new IllegalArgumentException("Completed shifts cannot be cancelled");
        }
        existing.setShiftStatus(STATUS_CANCELLED);
        return shiftRepository.save(existing);
    }

    public void delete(Integer id) {
        Shift existing = findById(id);

        if (existing.getStartDatetime() != null
                && existing.getStartDatetime().isBefore(LocalDateTime.now())
                && !STATUS_OPEN.equalsIgnoreCase(existing.getShiftStatus())) {
            throw new IllegalArgumentException("Shifts that have started cannot be deleted");
        }

        boolean pendingSwap = shiftAssignmentRepository.findAll().stream()
                .filter(a -> existing.getShiftId().equals(a.getShiftId()))
                .anyMatch(a -> shiftSwapRepository.findAll().stream()
                        .anyMatch(s -> a.getShiftAssignmentId().equals(s.getOriginalShiftAssignmentId())
                                && "Pending".equalsIgnoreCase(s.getSwapStatus())));
        if (pendingSwap) {
            throw new IllegalArgumentException("Shift has an unresolved swap request and cannot be deleted");
        }

        shiftRepository.delete(existing);
    }

    private void validate(Shift shift) {
        if (shift.getDepartmentId() == null || shift.getDepartmentId() <= 0) {
            throw new IllegalArgumentException("departmentId is required and must be positive");
        }
        if (!departmentRepository.existsById(shift.getDepartmentId())) {
            throw new IllegalArgumentException("departmentId " + shift.getDepartmentId() + " does not exist");
        }

        if (shift.getWorkLocationId() == null || shift.getWorkLocationId() <= 0) {
            throw new IllegalArgumentException("workLocationId is required and must be positive");
        }
        if (!workLocationRepository.existsById(shift.getWorkLocationId())) {
            throw new IllegalArgumentException("workLocationId " + shift.getWorkLocationId() + " does not exist");
        }

        if (shift.getStartDatetime() == null) {
            throw new IllegalArgumentException("startDatetime is required");
        }
        if (shift.getEndDatetime() == null) {
            throw new IllegalArgumentException("endDatetime is required");
        }
        if (!shift.getEndDatetime().isAfter(shift.getStartDatetime())) {
            throw new IllegalArgumentException("endDatetime must be after startDatetime");
        }

        long minutes = Duration.between(shift.getStartDatetime(), shift.getEndDatetime())
                .toMinutes();
        if (minutes < MIN_DURATION_MINUTES) {
            throw new IllegalArgumentException("Shift must be at least " + MIN_DURATION_MINUTES + " minutes long");
        }
        if (minutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("Shift must not exceed " + MAX_DURATION_MINUTES + " minutes");
        }

        if (shift.getShiftName() == null) {
            throw new IllegalArgumentException("shiftName is required");
        }
        String name = shift.getShiftName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("shiftName must not be blank");
        }
        if (name.length() > SHIFT_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("shiftName must not exceed " + SHIFT_NAME_MAX_LENGTH + " characters");
        }
        if (containsUnsafeContent(name)) {
            throw new IllegalArgumentException("shiftName contains unsafe content");
        }

        if (shift.getShiftStatus() != null
                && !shift.getShiftStatus().isBlank()
                && !VALID_STATUSES.contains(shift.getShiftStatus())) {
            throw new IllegalArgumentException("shiftStatus must be one of " + VALID_STATUSES);
        }
    }

    private boolean containsUnsafeContent(String value) {
        return value.contains("<") || value.contains(">");
    }
}
