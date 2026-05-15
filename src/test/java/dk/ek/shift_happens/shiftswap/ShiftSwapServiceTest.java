package dk.ek.shift_happens.shiftswap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shift.ShiftService;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ShiftSwapService}.
 *
 * Covers:
 *   create() — FR-SW-02, FR-SW-07, and all input-validation guards
 *   cancel() — FR-SW-06 (only requester, only while Pending)
 */
@ExtendWith(MockitoExtension.class)
class ShiftSwapServiceTest {

    private static final int ASSIGNMENT_ID = 10;
    private static final int SHIFT_ID = 100;
    private static final int EMPLOYEE_FROM_ID = 1;
    private static final int EMPLOYEE_TO_ID = 2;
    private static final int SWAP_ID = 99;

    @Mock
    private ShiftSwapRepository shiftSwapRepository;

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private ShiftSwapService service;

    // Shared test fixtures
    private ShiftAssignment assignment;
    private Shift shift;
    private Employee employeeTo;

    @BeforeEach
    void setUp() {
        service = new ShiftSwapService(
                shiftSwapRepository, shiftAssignmentRepository, shiftRepository, employeeRepository);

        assignment = new ShiftAssignment();
        assignment.setShiftAssignmentId(ASSIGNMENT_ID);
        assignment.setShiftId(SHIFT_ID);
        assignment.setEmployeeId(EMPLOYEE_FROM_ID);
        assignment.setAssignmentStatus(ShiftAssignmentService.STATUS_ASSIGNED);

        shift = new Shift();
        shift.setShiftId(SHIFT_ID);
        shift.setShiftStatus(ShiftService.STATUS_ASSIGNED);
        shift.setStartDatetime(LocalDateTime.now().plusDays(7));
        shift.setEndDatetime(LocalDateTime.now().plusDays(7).plusHours(8));

        employeeTo = new Employee();
        employeeTo.setEmployeeId(EMPLOYEE_TO_ID);
        employeeTo.setEmploymentStatus("Active");

        Employee employeeFrom = new Employee();
        employeeFrom.setEmployeeId(EMPLOYEE_FROM_ID);
        employeeFrom.setEmploymentStatus("Active");

        lenient().when(shiftAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        lenient().when(shiftRepository.findById(SHIFT_ID)).thenReturn(Optional.of(shift));
        lenient().when(employeeRepository.findById(EMPLOYEE_FROM_ID)).thenReturn(Optional.of(employeeFrom));
        lenient().when(employeeRepository.findById(EMPLOYEE_TO_ID)).thenReturn(Optional.of(employeeTo));
        lenient()
                .when(shiftSwapRepository.findByOriginalShiftAssignmentIdAndSwapStatusIgnoreCase(anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        lenient().when(shiftSwapRepository.save(any(ShiftSwap.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ShiftSwap validSwap() {
        ShiftSwap s = new ShiftSwap();
        s.setOriginalShiftAssignmentId(ASSIGNMENT_ID);
        s.setEmployeeFromId(EMPLOYEE_FROM_ID);
        s.setEmployeeToId(EMPLOYEE_TO_ID);
        s.setRequestDatetime(LocalDateTime.now().minusMinutes(1));
        return s;
    }

    // -----------------------------------------------------------------------
    // create()
    // -----------------------------------------------------------------------

    @Nested
    class Create {

        @Test
        void fr_sw_02_creates_swap_with_status_pending() {
            // FR-SW-02: swap must be created with status Pending
            ShiftSwap result = service.create(validSwap());
            assertThat(result.getSwapStatus()).isEqualTo(ShiftSwapService.STATUS_PENDING);
        }

        @Test
        void fr_sw_07_rejects_duplicate_pending_swap_for_same_assignment() {
            // FR-SW-07: no two pending swaps for the same assignment
            when(shiftSwapRepository.findByOriginalShiftAssignmentIdAndSwapStatusIgnoreCase(
                            ASSIGNMENT_ID, ShiftSwapService.STATUS_PENDING))
                    .thenReturn(Collections.singletonList(new ShiftSwap()));

            assertThatThrownBy(() -> service.create(validSwap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pending swap already exists");
        }

        @Test
        void rejects_swap_when_assignment_is_cancelled() {
            assignment.setAssignmentStatus(ShiftAssignmentService.STATUS_CANCELLED);

            assertThatThrownBy(() -> service.create(validSwap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cancelled assignment");
        }

        @Test
        void rejects_swap_when_shift_is_cancelled() {
            // Relates to BR-AP-05 on the create side as well
            shift.setShiftStatus(ShiftService.STATUS_CANCELLED);

            assertThatThrownBy(() -> service.create(validSwap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cancelled shift");
        }

        @Test
        void rejects_swap_when_shift_has_already_started() {
            shift.setStartDatetime(LocalDateTime.now().minusHours(1));

            assertThatThrownBy(() -> service.create(validSwap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("started");
        }

        @Test
        void rejects_swap_when_employee_from_does_not_own_assignment() {
            ShiftSwap swap = validSwap();
            swap.setEmployeeFromId(EMPLOYEE_FROM_ID + 99); // wrong owner

            assertThatThrownBy(() -> service.create(swap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must own");
        }

        @Test
        void rejects_swap_when_employee_from_equals_employee_to() {
            ShiftSwap swap = validSwap();
            swap.setEmployeeToId(EMPLOYEE_FROM_ID); // same person

            assertThatThrownBy(() -> service.create(swap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must differ");
        }

        @Test
        void rejects_swap_when_target_employee_is_inactive() {
            employeeTo.setEmploymentStatus("Inactive");

            assertThatThrownBy(() -> service.create(validSwap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        void rejects_reason_containing_html_tags() {
            ShiftSwap swap = validSwap();
            swap.setReason("<script>alert('xss')</script>");

            assertThatThrownBy(() -> service.create(swap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe");
        }

        @Test
        void rejects_reason_exceeding_max_length() {
            ShiftSwap swap = validSwap();
            swap.setReason("x".repeat(ShiftSwapService.REASON_MAX_LENGTH + 1));

            assertThatThrownBy(() -> service.create(swap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceed");
        }

        @Test
        void rejects_request_datetime_in_the_future() {
            ShiftSwap swap = validSwap();
            swap.setRequestDatetime(LocalDateTime.now().plusHours(1));

            assertThatThrownBy(() -> service.create(swap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("future");
        }
    }

    // -----------------------------------------------------------------------
    // cancel()
    // -----------------------------------------------------------------------

    @Nested
    class Cancel {

        private ShiftSwap pendingSwap() {
            ShiftSwap s = new ShiftSwap();
            s.setShiftSwapId(SWAP_ID);
            s.setEmployeeFromId(EMPLOYEE_FROM_ID);
            s.setSwapStatus(ShiftSwapService.STATUS_PENDING);
            return s;
        }

        @BeforeEach
        void stubFindById() {
            lenient().when(shiftSwapRepository.findById(SWAP_ID)).thenReturn(Optional.of(pendingSwap()));
        }

        @Test
        void fr_sw_06_requester_can_cancel_pending_swap() {
            // FR-SW-06: the requesting employee can cancel a pending swap
            ShiftSwap result = service.cancel(SWAP_ID, EMPLOYEE_FROM_ID);
            assertThat(result.getSwapStatus()).isEqualTo(ShiftSwapService.STATUS_CANCELLED);
        }

        @Test
        void rejects_cancel_when_swap_is_not_pending() {
            ShiftSwap nonPending = pendingSwap();
            nonPending.setSwapStatus(ShiftSwapService.STATUS_APPROVED);
            when(shiftSwapRepository.findById(SWAP_ID)).thenReturn(Optional.of(nonPending));

            assertThatThrownBy(() -> service.cancel(SWAP_ID, EMPLOYEE_FROM_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pending");
        }

        @Test
        void rejects_cancel_by_a_different_employee() {
            int differentEmployee = EMPLOYEE_FROM_ID + 50;

            assertThatThrownBy(() -> service.cancel(SWAP_ID, differentEmployee))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requesting employee");
        }
    }
}