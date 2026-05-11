package dk.ek.shift_happens.shiftswapapproval;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.employee.UserRole;
import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shift.ShiftService;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentService;
import dk.ek.shift_happens.shiftswap.ShiftSwap;
import dk.ek.shift_happens.shiftswap.ShiftSwapRepository;
import dk.ek.shift_happens.shiftswap.ShiftSwapService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShiftSwapApprovalService {

    public static final String DECISION_APPROVED = "Approved";
    public static final String DECISION_DECLINED = "Declined";
    public static final int COMMENT_MAX_LENGTH = 500;

    private final ShiftSwapApprovalRepository shiftSwapApprovalRepository;
    private final ShiftSwapRepository shiftSwapRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftAssignmentService shiftAssignmentService;

    public List<ShiftSwapApproval> findAll() {
        return shiftSwapApprovalRepository.findAll();
    }

    public ShiftSwapApproval findById(Integer id) {
        return shiftSwapApprovalRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found with id " + id));
    }

    @Transactional
    public ShiftSwapApproval approve(ShiftSwapApproval approval) {
        if (approval.getShiftSwapId() == null || approval.getShiftSwapId() <= 0) {
            throw new IllegalArgumentException("shiftSwapId is required and must be positive");
        }
        if (approval.getApproverEmployeeId() == null || approval.getApproverEmployeeId() <= 0) {
            throw new IllegalArgumentException("approverEmployeeId is required and must be positive");
        }
        if (approval.getDecision() == null || approval.getDecision().isBlank()) {
            throw new IllegalArgumentException("decision is required");
        }

        String normalized = normaliseDecision(approval.getDecision());

        ShiftSwap swap = shiftSwapRepository
                .findById(approval.getShiftSwapId())
                .orElseThrow(() -> new IllegalArgumentException("Shift swap not found"));

        if (ShiftSwapService.STATUS_CANCELLED.equalsIgnoreCase(swap.getSwapStatus())) {
            throw new IllegalArgumentException("Cannot approve a cancelled swap request");
        }
        if (ShiftSwapService.STATUS_DECLINED.equalsIgnoreCase(swap.getSwapStatus())) {
            throw new IllegalArgumentException("Cannot approve a declined swap request");
        }
        if (!ShiftSwapService.STATUS_PENDING.equalsIgnoreCase(swap.getSwapStatus())) {
            throw new IllegalArgumentException("Only pending swaps can be approved");
        }

        Employee approver = employeeRepository
                .findById(approval.getApproverEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Approver not found"));

        UserRole role = approver.getUserRole();
        if (role == null || (role != UserRole.Administrator && role != UserRole.Manager)) {
            throw new IllegalArgumentException("Only Administrator or Manager can approve a swap");
        }

        if (approver.getEmployeeId().equals(swap.getEmployeeFromId())
                || approver.getEmployeeId().equals(swap.getEmployeeToId())) {
            throw new IllegalArgumentException("Approver cannot be a party to the swap");
        }

        ShiftAssignment originalAssignment = shiftAssignmentRepository
                .findById(swap.getOriginalShiftAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Original assignment not found"));
        Shift originalShift = shiftRepository
                .findById(originalAssignment.getShiftId())
                .orElseThrow(() -> new IllegalArgumentException("Original shift not found"));

        if (ShiftService.STATUS_CANCELLED.equalsIgnoreCase(originalShift.getShiftStatus())) {
            throw new IllegalArgumentException("Cannot approve a swap for a cancelled shift");
        }
        if (ShiftAssignmentService.STATUS_CANCELLED.equalsIgnoreCase(originalAssignment.getAssignmentStatus())) {
            throw new IllegalArgumentException("Cannot approve a swap for a cancelled assignment");
        }

        validateComment(approval.getShiftSwapComment());

        approval.setShiftSwapApprovalId(null);
        approval.setDecision(normalized);
        approval.setDecisionDatetime(LocalDateTime.now());

        if (swap.getRequestDatetime() != null && approval.getDecisionDatetime().isBefore(swap.getRequestDatetime())) {
            throw new IllegalArgumentException("Decision time cannot be before request time");
        }

        if (DECISION_APPROVED.equalsIgnoreCase(normalized)) {
            if (shiftAssignmentService.hasOverlapOrInsufficientRest(
                    swap.getEmployeeToId(), originalShift, originalAssignment.getShiftAssignmentId())) {
                throw new IllegalArgumentException("Swap would cause overlap or violate "
                        + ShiftAssignmentService.REST_HOURS + " hour rest for the target employee");
            }

            originalAssignment.setEmployeeId(swap.getEmployeeToId());
            shiftAssignmentRepository.save(originalAssignment);
            swap.setSwapStatus(ShiftSwapService.STATUS_APPROVED);
        } else {
            swap.setSwapStatus(ShiftSwapService.STATUS_DECLINED);
        }

        shiftSwapRepository.save(swap);
        return shiftSwapApprovalRepository.save(approval);
    }

    public Optional<ShiftSwapApproval> update(Integer id, ShiftSwapApproval details) {
        return shiftSwapApprovalRepository.findById(id).map(existing -> {
            if (details.getShiftSwapComment() != null) {
                validateComment(details.getShiftSwapComment());
                existing.setShiftSwapComment(details.getShiftSwapComment());
            }
            return shiftSwapApprovalRepository.save(existing);
        });
    }

    public boolean delete(Integer id) {
        if (!shiftSwapApprovalRepository.existsById(id)) {
            return false;
        }
        shiftSwapApprovalRepository.deleteById(id);
        return true;
    }

    private String normaliseDecision(String decision) {
        String trimmed = decision.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (upper.equals("APPROVED")) return DECISION_APPROVED;
        if (upper.equals("DECLINED")) return DECISION_DECLINED;
        throw new IllegalArgumentException("decision must be Approved or Declined");
    }

    private void validateComment(String comment) {
        if (comment == null || comment.isEmpty()) return;
        if (comment.trim().isEmpty()) {
            throw new IllegalArgumentException("shiftSwapComment must not be only whitespace");
        }
        if (comment.length() > COMMENT_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "shiftSwapComment must not exceed " + COMMENT_MAX_LENGTH + " characters");
        }
        if (comment.contains("<") || comment.contains(">")) {
            throw new IllegalArgumentException("shiftSwapComment contains unsafe content");
        }
    }
}
