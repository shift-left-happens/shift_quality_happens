package dk.ek.shift_happens.jobrole;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.ek.shift_happens.employeejobrole.EmployeeJobRoleRepository;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRole;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRoleRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the docx §"Deletion Constraints & Dependencies" decision table:
 *   Case 1 — referenced by an active shift → Block
 *   Case 2 — held by an active employee → Block
 *   Case 3 — no dependencies → Delete
 */
@ExtendWith(MockitoExtension.class)
class JobRoleServiceTest {

    private static final int ROLE_ID = 7;

    @Mock
    private JobRoleRepository jobRoleRepository;

    @Mock
    private ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;

    @Mock
    private EmployeeJobRoleRepository employeeJobRoleRepository;

    private JobRoleService service;
    private JobRole role;

    @BeforeEach
    void setUp() {
        service = new JobRoleService(jobRoleRepository, shiftRequiredJobRoleRepository, employeeJobRoleRepository);

        role = new JobRole();
        role.setJobRoleId(ROLE_ID);
        role.setRoleName("Cashier");
        role.setIsCertificationRequired(false);

        when(jobRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
        lenient().when(shiftRequiredJobRoleRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(employeeJobRoleRepository.existsByJobRoleId(ROLE_ID)).thenReturn(false);
    }

    @Test
    void case1_should_block_when_referenced_by_shift() {
        ShiftRequiredJobRole link = new ShiftRequiredJobRole();
        link.setShiftId(1);
        link.setJobRoleId(ROLE_ID);
        when(shiftRequiredJobRoleRepository.findAll()).thenReturn(List.of(link));

        assertThatThrownBy(() -> service.delete(ROLE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shift");
        verify(jobRoleRepository, never()).delete(any(JobRole.class));
    }

    @Test
    void case2_should_block_when_held_by_employee() {
        when(employeeJobRoleRepository.existsByJobRoleId(ROLE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(ROLE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employee");
        verify(jobRoleRepository, never()).delete(any(JobRole.class));
    }

    @Test
    void case3_should_delete_when_no_dependencies() {
        service.delete(ROLE_ID);
        verify(jobRoleRepository).delete(role);
    }
}
