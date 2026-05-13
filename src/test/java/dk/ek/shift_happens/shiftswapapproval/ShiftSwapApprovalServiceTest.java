package dk.ek.shift_happens.shiftswapapproval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for docx §"Shift Swap Approvals" decision table cases 1, 2, 3, 5, 6.
 *
 *   Case 1 — Admin/Manager, not the requester, status Pending, shift live, no overlap → Approve
 *   Case 2 — non-manager role → Deny
 *   Case 3 — approver is the requester → Deny
 *   Case 5 — swap status is Cancelled/Declined → Deny
 *   Case 6 — underlying shift is Cancelled → Deny
 */
@ExtendWith(MockitoExtension.class)
class ShiftSwapApprovalServiceTest {

    private static final int SWAP_ID = 10;
    private static final int APPROVER_ID = 99;
    private static final int REQUESTER_ID = 1;
    private static final int TARGET_ID = 2;
    private static final int ASSIGNMENT_ID = 50;
    private static final int SHIFT_ID = 500;

    @Mock
    private ShiftSwapApprovalRepository shiftSwapApprovalRepository;

    @Mock
    private ShiftSwapRepository shiftSwapRepository;

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ShiftAssignmentService shiftAssignmentService;

    private ShiftSwapApprovalService service;

    private ShiftSwap swap;
    private Shift originalShift;
    private ShiftAssignment originalAssignment;

    @BeforeEach
    void setUp() {
        service = new ShiftSwapApprovalService(
                shiftSwapApprovalRepository,
                shiftSwapRepository,
                shiftAssignmentRepository,
                shiftRepository,
                employeeRepository,
                shiftAssignmentService);

        swap = new ShiftSwap();
        swap.setShiftSwapId(SWAP_ID);
        swap.setOriginalShiftAssignmentId(ASSIGNMENT_ID);
        swap.setEmployeeFromId(REQUESTER_ID);
        swap.setEmployeeToId(TARGET_ID);
        swap.setSwapStatus(ShiftSwapService.STATUS_PENDING);
        swap.setRequestDatetime(LocalDateTime.of(2026, 5, 1, 10, 0));

        originalShift = new Shift();
        originalShift.setShiftId(SHIFT_ID);
        originalShift.setDepartmentId(1);
        originalShift.setWorkLocationId(1);
        originalShift.setShiftName("Morning");
        originalShift.setStartDatetime(LocalDateTime.of(2026, 5, 20, 8, 0));
        originalShift.setEndDatetime(LocalDateTime.of(2026, 5, 20, 16, 0));
        originalShift.setShiftStatus(ShiftService.STATUS_ASSIGNED);

        originalAssignment = new ShiftAssignment();
        originalAssignment.setShiftAssignmentId(ASSIGNMENT_ID);
        originalAssignment.setShiftId(SHIFT_ID);
        originalAssignment.setEmployeeId(REQUESTER_ID);
        originalAssignment.setAssignmentStatus(ShiftAssignmentService.STATUS_ASSIGNED);

        lenient().when(shiftSwapRepository.findById(SWAP_ID)).thenReturn(Optional.of(swap));
        lenient().when(shiftAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(originalAssignment));
        lenient().when(shiftRepository.findById(SHIFT_ID)).thenReturn(Optional.of(originalShift));
        lenient()
                .when(employeeRepository.findById(APPROVER_ID))
                .thenReturn(Optional.of(employeeWithRole(APPROVER_ID, UserRole.Manager)));
        lenient()
                .when(shiftAssignmentService.hasOverlapOrInsufficientRest(anyInt(), any(), any()))
                .thenReturn(false);
        lenient()
                .when(shiftSwapApprovalRepository.save(any(ShiftSwapApproval.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(shiftSwapRepository.save(any(ShiftSwap.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient()
                .when(shiftAssignmentRepository.save(any(ShiftAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private Employee employeeWithRole(int id, UserRole role) {
        Employee e = new Employee();
        e.setEmployeeId(id);
        e.setUserRole(role);
        return e;
    }

    private ShiftSwapApproval buildApproval(String decision) {
        ShiftSwapApproval a = new ShiftSwapApproval();
        a.setShiftSwapId(SWAP_ID);
        a.setApproverEmployeeId(APPROVER_ID);
        a.setDecision(decision);
        return a;
    }

    // --- Case 1 — happy path ----------------------------------------------
    @Test
    void case1_should_approve_when_manager_not_requester_pending_live_no_overlap() {
        ShiftSwapApproval result = service.approve(buildApproval("Approved"));
        assertThat(result.getDecision()).isEqualTo(ShiftSwapApprovalService.DECISION_APPROVED);
        assertThat(swap.getSwapStatus()).isEqualTo(ShiftSwapService.STATUS_APPROVED);
        assertThat(originalAssignment.getEmployeeId()).isEqualTo(TARGET_ID);
    }

    // --- Case 2 — approver is a regular Employee → Deny -------------------
    @Test
    void case2_should_deny_when_approver_is_employee_role() {
        when(employeeRepository.findById(APPROVER_ID))
                .thenReturn(Optional.of(employeeWithRole(APPROVER_ID, UserRole.Employee)));
        assertThatThrownBy(() -> service.approve(buildApproval("Approved")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Administrator or Manager");
    }

    // --- Case 3 — self-approval → Deny ------------------------------------
    @Test
    void case3_should_deny_when_approver_is_the_requester() {
        when(employeeRepository.findById(REQUESTER_ID))
                .thenReturn(Optional.of(employeeWithRole(REQUESTER_ID, UserRole.Manager)));
        ShiftSwapApproval approval = buildApproval("Approved");
        approval.setApproverEmployeeId(REQUESTER_ID);

        assertThatThrownBy(() -> service.approve(approval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Approver cannot be a party");
    }

    // --- Case 5 — swap status is Cancelled / Declined → Deny --------------
    @Test
    void case5_should_deny_when_swap_already_cancelled() {
        swap.setSwapStatus(ShiftSwapService.STATUS_CANCELLED);
        assertThatThrownBy(() -> service.approve(buildApproval("Approved")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void case5_should_deny_when_swap_already_declined() {
        swap.setSwapStatus(ShiftSwapService.STATUS_DECLINED);
        assertThatThrownBy(() -> service.approve(buildApproval("Approved")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declined");
    }

    // --- Case 6 — underlying shift cancelled → Deny -----------------------
    @Test
    void case6_should_deny_when_underlying_shift_cancelled() {
        originalShift.setShiftStatus(ShiftService.STATUS_CANCELLED);
        assertThatThrownBy(() -> service.approve(buildApproval("Approved")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelled");
    }

    // --- Case 1 condition inverted — would cause overlap → Deny -----------
    @Test
    void case1_should_deny_when_approval_would_cause_overlap() {
        when(shiftAssignmentService.hasOverlapOrInsufficientRest(anyInt(), any(), any()))
                .thenReturn(true);
        assertThatThrownBy(() -> service.approve(buildApproval("Approved")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }
}
