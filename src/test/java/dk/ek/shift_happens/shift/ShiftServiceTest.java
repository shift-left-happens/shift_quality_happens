package dk.ek.shift_happens.shift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import dk.ek.shift_happens.department.DepartmentRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftswap.ShiftSwapRepository;
import dk.ek.shift_happens.worklocation.WorkLocationRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ShiftService — focused on docx §"Shift Duration & Timing"
 * decision-table rules 1 (valid) and 2 (start >= end rejected).
 *
 * Rules 4 (rest >= 11h) and 5 (job role match) are enforced in
 * ShiftAssignmentService and tested in ShiftAssignmentServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private WorkLocationRepository workLocationRepository;

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private ShiftSwapRepository shiftSwapRepository;

    private ShiftService service;

    @BeforeEach
    void setUp() {
        service = new ShiftService(
                shiftRepository,
                departmentRepository,
                workLocationRepository,
                shiftAssignmentRepository,
                shiftSwapRepository);
        lenient().when(departmentRepository.existsById(1)).thenReturn(true);
        lenient().when(workLocationRepository.existsById(1)).thenReturn(true);
        lenient().when(shiftRepository.save(any(Shift.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(shiftAssignmentRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(shiftSwapRepository.findAll()).thenReturn(Collections.emptyList());
    }

    private Shift base() {
        Shift s = new Shift();
        s.setDepartmentId(1);
        s.setWorkLocationId(1);
        s.setShiftName("Morning");
        s.setStartDatetime(LocalDateTime.of(2026, 5, 12, 8, 0));
        s.setEndDatetime(LocalDateTime.of(2026, 5, 12, 16, 0));
        return s;
    }

    @Nested
    class TimingValidation {

        @Test
        void should_accept_when_start_before_end() {
            // docx §"Shift Creation" Rule 1 — Valid (happy path)
            Shift s = base();
            assertThat(service.create(s)).isNotNull();
        }

        @Test
        void should_reject_when_start_equals_end() {
            // Rule 2 boundary — start == end is not strictly before, must error
            Shift s = base();
            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 8, 0);
            s.setStartDatetime(t);
            s.setEndDatetime(t);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("after");
        }

        @Test
        void should_reject_when_start_after_end() {
            // Rule 2 — start > end / Error
            Shift s = base();
            s.setStartDatetime(LocalDateTime.of(2026, 5, 12, 16, 0));
            s.setEndDatetime(LocalDateTime.of(2026, 5, 12, 8, 0));
            assertThatThrownBy(() -> service.create(s)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_accept_overnight_shift() {
            // §"ISO 8601 Dates" implies overnight support; ensure 22:00 → 06:00 next day works
            Shift s = base();
            s.setStartDatetime(LocalDateTime.of(2026, 5, 12, 22, 0));
            s.setEndDatetime(LocalDateTime.of(2026, 5, 13, 6, 0));
            assertThat(service.create(s)).isNotNull();
        }
    }
}
