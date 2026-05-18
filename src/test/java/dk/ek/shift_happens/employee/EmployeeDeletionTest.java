package dk.ek.shift_happens.employee;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for Employee deletion constraints, derived from
 * §"Deletion Constraints & Dependencies" in the .md file.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeDeletionTest {

    private static final int EMPLOYEE_ID = 42;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-12T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private EmployeeRepository repo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private ShiftRepository shiftRepository;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        EmployeeValidator validator = new EmployeeValidator(FIXED_CLOCK);
        service = new EmployeeService(
                repo, passwordEncoder, validator, shiftAssignmentRepository, shiftRepository, FIXED_CLOCK);

        Employee existing = new Employee();
        existing.setEmployeeId(EMPLOYEE_ID);
        existing.setFirstName("Jensen");
        existing.setLastName("Jensen");
        existing.setEmail("a@a.dk");
        existing.setBirthDate(LocalDate.of(1990, 1, 1));

        when(repo.findById(EMPLOYEE_ID)).thenReturn(Optional.of(existing));
    }

    @Test
    void should_delete_when_no_dependencies() {
        // §"Deletion Constraints & Dependencies" case 3
        when(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Collections.emptyList());
        service.delete(EMPLOYEE_ID);
        verify(repo).delete(any(Employee.class));
    }

    @Test
    void should_block_when_future_assignment_exists() {
        // §"Deletion Constraints & Dependencies" case 1
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftAssignmentId(1);
        a.setShiftId(100);
        a.setEmployeeId(EMPLOYEE_ID);
        a.setAssignmentStatus(ShiftAssignmentService.STATUS_ASSIGNED);

        Shift future = new Shift();
        future.setShiftId(100);
        future.setStartDatetime(LocalDateTime.of(2030, 1, 1, 8, 0));

        when(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(a));
        when(shiftRepository.findById(100)).thenReturn(Optional.of(future));

        assertThatThrownBy(() -> service.delete(EMPLOYEE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
        verify(repo, never()).delete(any(Employee.class));
    }

    @Test
    void should_allow_delete_when_only_past_assignments() {
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftAssignmentId(1);
        a.setShiftId(100);
        a.setEmployeeId(EMPLOYEE_ID);
        a.setAssignmentStatus(ShiftAssignmentService.STATUS_COMPLETED);

        Shift past = new Shift();
        past.setShiftId(100);
        past.setStartDatetime(LocalDateTime.of(2020, 1, 1, 8, 0));

        when(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(a));
        when(shiftRepository.findById(100)).thenReturn(Optional.of(past));

        service.delete(EMPLOYEE_ID);
        verify(repo).delete(any(Employee.class));
    }

    @Test
    void should_allow_delete_when_future_assignment_is_cancelled() {
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftAssignmentId(1);
        a.setShiftId(100);
        a.setEmployeeId(EMPLOYEE_ID);
        a.setAssignmentStatus(ShiftAssignmentService.STATUS_CANCELLED);

        service.delete(EMPLOYEE_ID);
        verify(repo).delete(any(Employee.class));
    }
}
