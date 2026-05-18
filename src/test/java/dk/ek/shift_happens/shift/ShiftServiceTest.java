package dk.ek.shift_happens.shift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.ek.shift_happens.department.DepartmentRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftswap.ShiftSwap;
import dk.ek.shift_happens.shiftswap.ShiftSwapRepository;
import dk.ek.shift_happens.worklocation.WorkLocationRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link ShiftService}.
 *
 * <p>The cases are blackbox-derived from "Software Quality exam - Shift Happens":
 *
 * <ul>
 *   <li>§"Shift Duration &amp; Timing" decision table — rules 1 (valid) and 2 (start &gt;= end).
 *   <li>§"Shift Status Transitions" — status enum and the cancel transition.
 *   <li>§9 "Deletion Constraints &amp; Dependencies" decision table — shift deletion guards.
 * </ul>
 *
 * <p>Rules 4 (rest &gt;= 11h) and 5 (job-role match) of the timing table are enforced in
 * ShiftAssignmentService and tested in {@code ShiftAssignmentServiceTest}.
 *
 * <p>The duration, name and reference checks below cover the remaining branches of
 * {@code ShiftService.validate()} that are not spelled out as named docx tables but
 * follow the same equivalence-partitioning / boundary-value approach.
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

    /** A valid shift: 8h long, real department/location, in the future. */
    private Shift base() {
        Shift s = new Shift();
        s.setDepartmentId(1);
        s.setWorkLocationId(1);
        s.setShiftName("Morning");
        s.setStartDatetime(LocalDateTime.of(2026, 5, 12, 8, 0));
        s.setEndDatetime(LocalDateTime.of(2026, 5, 12, 16, 0));
        return s;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // §"Shift Duration & Timing" — decision table rules 1 & 2
    // ──────────────────────────────────────────────────────────────────────────

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

        @Test
        void should_reject_when_start_is_null() {
            Shift s = base();
            s.setStartDatetime(null);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("startDatetime");
        }

        @Test
        void should_reject_when_end_is_null() {
            Shift s = base();
            s.setEndDatetime(null);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("endDatetime");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Shift duration — 3-point BVA on the 60-minute and 12-hour bounds
    // ShiftService.MIN_DURATION_MINUTES = 60, MAX_DURATION_MINUTES = 720
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class DurationBVA {

        private Shift shiftLasting(long minutes) {
            Shift s = base();
            s.setStartDatetime(LocalDateTime.of(2026, 5, 12, 8, 0));
            s.setEndDatetime(s.getStartDatetime().plusMinutes(minutes));
            return s;
        }

        @Test
        void should_reject_when_duration_just_below_minimum() {
            // 59 min — one below the 60-minute lower bound
            assertThatThrownBy(() -> service.create(shiftLasting(59)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least");
        }

        @Test
        void should_accept_when_duration_at_minimum() {
            // 60 min — exactly the lower bound
            assertThat(service.create(shiftLasting(60))).isNotNull();
        }

        @Test
        void should_accept_when_duration_just_above_minimum() {
            // 61 min — one above the lower bound
            assertThat(service.create(shiftLasting(61))).isNotNull();
        }

        @Test
        void should_accept_when_duration_just_below_maximum() {
            // 719 min — one below the 12h upper bound
            assertThat(service.create(shiftLasting(719))).isNotNull();
        }

        @Test
        void should_accept_when_duration_at_maximum() {
            // 720 min (12h) — exactly the upper bound
            assertThat(service.create(shiftLasting(720))).isNotNull();
        }

        @Test
        void should_reject_when_duration_just_above_maximum() {
            // 721 min — one above the upper bound
            assertThatThrownBy(() -> service.create(shiftLasting(721)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceed");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Shift name — EP (null / blank / unsafe) + BVA on the 100-char limit
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class ShiftNameValidation {

        @Test
        void should_reject_when_name_is_null() {
            Shift s = base();
            s.setShiftName(null);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required");
        }

        @Test
        void should_reject_when_name_is_blank() {
            Shift s = base();
            s.setShiftName("   ");
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        void should_accept_when_name_at_max_length() {
            // 100 chars — exactly SHIFT_NAME_MAX_LENGTH
            Shift s = base();
            s.setShiftName("a".repeat(100));
            assertThat(service.create(s)).isNotNull();
        }

        @Test
        void should_reject_when_name_just_above_max_length() {
            // 101 chars — one above the limit
            Shift s = base();
            s.setShiftName("a".repeat(101));
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceed");
        }

        @Test
        void should_reject_when_name_contains_unsafe_content() {
            // '<' / '>' are rejected to keep stored-XSS payloads out
            Shift s = base();
            s.setShiftName("Late <script>alert(1)</script>");
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe");
        }

        @Test
        void should_trim_name_on_create() {
            Shift s = base();
            s.setShiftName("  Morning  ");
            assertThat(service.create(s).getShiftName()).isEqualTo("Morning");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Department / work-location references — must be present, positive, existing
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class ReferenceValidation {

        @Test
        void should_reject_when_department_id_is_null() {
            Shift s = base();
            s.setDepartmentId(null);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("departmentId");
        }

        @Test
        void should_reject_when_department_id_is_zero() {
            Shift s = base();
            s.setDepartmentId(0);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        void should_reject_when_department_id_is_negative() {
            Shift s = base();
            s.setDepartmentId(-1);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        void should_reject_when_department_does_not_exist() {
            Shift s = base();
            s.setDepartmentId(999);
            when(departmentRepository.existsById(999)).thenReturn(false);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not exist");
        }

        @Test
        void should_reject_when_work_location_id_is_null() {
            Shift s = base();
            s.setWorkLocationId(null);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("workLocationId");
        }

        @Test
        void should_reject_when_work_location_id_is_zero() {
            Shift s = base();
            s.setWorkLocationId(0);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        void should_reject_when_work_location_does_not_exist() {
            Shift s = base();
            s.setWorkLocationId(999);
            when(workLocationRepository.existsById(999)).thenReturn(false);
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not exist");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // §"Shift Status Transitions" — status enum
    // Valid statuses: Open, Assigned, Pending Swap, Cancelled, Completed
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class StatusValidation {

        @Test
        void should_accept_each_valid_status() {
            for (String status : List.of("Open", "Assigned", "Pending Swap", "Cancelled", "Completed")) {
                Shift s = base();
                s.setShiftStatus(status);
                assertThat(service.create(s).getShiftStatus())
                        .as("status %s should be accepted", status)
                        .isEqualTo(status);
            }
        }

        @Test
        void should_reject_unknown_status() {
            Shift s = base();
            s.setShiftStatus("SCHEDULED");
            assertThatThrownBy(() -> service.create(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("shiftStatus");
        }

        @Test
        void should_default_to_open_when_status_is_null() {
            Shift s = base();
            s.setShiftStatus(null);
            assertThat(service.create(s).getShiftStatus()).isEqualTo(ShiftService.STATUS_OPEN);
        }

        @Test
        void should_default_to_open_when_status_is_blank() {
            Shift s = base();
            s.setShiftStatus("  ");
            assertThat(service.create(s).getShiftStatus()).isEqualTo(ShiftService.STATUS_OPEN);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CRUD behaviour — findAll / findById / create / update
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class CrudOperations {

        @Test
        void should_return_all_shifts() {
            when(shiftRepository.findAll()).thenReturn(List.of(base(), base()));
            assertThat(service.findAll()).hasSize(2);
        }

        @Test
        void should_throw_not_found_when_shift_id_is_unknown() {
            when(shiftRepository.findById(42)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findById(42))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Shift not found");
        }

        @Test
        void should_clear_client_supplied_id_on_create() {
            Shift s = base();
            s.setShiftId(123);
            assertThat(service.create(s).getShiftId()).isNull();
        }

        @Test
        void should_merge_only_non_null_fields_on_update() {
            Shift existing = base();
            existing.setShiftId(7);
            when(shiftRepository.findById(7)).thenReturn(Optional.of(existing));

            Shift updates = new Shift();
            updates.setShiftName("Evening");

            Shift result = service.update(7, updates);

            assertThat(result.getShiftName()).isEqualTo("Evening");
            // untouched fields keep their original values
            assertThat(result.getDepartmentId()).isEqualTo(1);
            assertThat(result.getStartDatetime()).isEqualTo(LocalDateTime.of(2026, 5, 12, 8, 0));
        }

        @Test
        void should_validate_merged_shift_on_update() {
            Shift existing = base();
            existing.setShiftId(7);
            when(shiftRepository.findById(7)).thenReturn(Optional.of(existing));

            Shift updates = new Shift();
            // pushes start past end → merged shift is invalid
            updates.setStartDatetime(LocalDateTime.of(2026, 5, 12, 20, 0));

            assertThatThrownBy(() -> service.update(7, updates)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Cancel — FR-SH-06; completed shifts cannot be cancelled
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class CancelShift {

        @Test
        void should_set_status_to_cancelled() {
            Shift existing = base();
            existing.setShiftId(5);
            existing.setShiftStatus(ShiftService.STATUS_ASSIGNED);
            when(shiftRepository.findById(5)).thenReturn(Optional.of(existing));

            assertThat(service.cancel(5).getShiftStatus()).isEqualTo(ShiftService.STATUS_CANCELLED);
        }

        @Test
        void should_reject_cancelling_a_completed_shift() {
            Shift existing = base();
            existing.setShiftId(5);
            existing.setShiftStatus(ShiftService.STATUS_COMPLETED);
            when(shiftRepository.findById(5)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.cancel(5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Completed");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // §9 "Deletion Constraints & Dependencies" — FR-SH-03
    // A shift may be deleted only if it has not started and has no pending swap.
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class DeleteConstraints {

        @Test
        void should_delete_future_shift_without_dependencies() {
            Shift existing = base();
            existing.setShiftId(8);
            existing.setStartDatetime(LocalDateTime.now().plusDays(3));
            existing.setEndDatetime(existing.getStartDatetime().plusHours(8));
            when(shiftRepository.findById(8)).thenReturn(Optional.of(existing));

            service.delete(8);

            verify(shiftRepository, times(1)).delete(existing);
        }

        @Test
        void should_block_deleting_a_shift_that_has_already_started() {
            // started in the past and no longer Open → counts as active working time
            Shift existing = base();
            existing.setShiftId(8);
            existing.setStartDatetime(LocalDateTime.now().minusDays(1));
            existing.setEndDatetime(existing.getStartDatetime().plusHours(8));
            existing.setShiftStatus(ShiftService.STATUS_ASSIGNED);
            when(shiftRepository.findById(8)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.delete(8))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("started");
            verify(shiftRepository, never()).delete(any(Shift.class));
        }

        @Test
        void should_allow_deleting_a_past_shift_still_open() {
            // a past shift that nobody picked up (still Open) is exempt from the started guard
            Shift existing = base();
            existing.setShiftId(8);
            existing.setStartDatetime(LocalDateTime.now().minusDays(1));
            existing.setEndDatetime(existing.getStartDatetime().plusHours(8));
            existing.setShiftStatus(ShiftService.STATUS_OPEN);
            when(shiftRepository.findById(8)).thenReturn(Optional.of(existing));

            service.delete(8);

            verify(shiftRepository, times(1)).delete(existing);
        }

        @Test
        void should_block_deleting_a_shift_with_a_pending_swap() {
            Shift existing = base();
            existing.setShiftId(8);
            existing.setStartDatetime(LocalDateTime.now().plusDays(3));
            existing.setEndDatetime(existing.getStartDatetime().plusHours(8));
            when(shiftRepository.findById(8)).thenReturn(Optional.of(existing));

            ShiftAssignment assignment = new ShiftAssignment();
            assignment.setShiftAssignmentId(50);
            assignment.setShiftId(8);
            when(shiftAssignmentRepository.findAll()).thenReturn(List.of(assignment));

            ShiftSwap swap = new ShiftSwap();
            swap.setOriginalShiftAssignmentId(50);
            swap.setSwapStatus("Pending");
            when(shiftSwapRepository.findAll()).thenReturn(List.of(swap));

            assertThatThrownBy(() -> service.delete(8))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("swap");
            verify(shiftRepository, never()).delete(any(Shift.class));
        }
    }
}
