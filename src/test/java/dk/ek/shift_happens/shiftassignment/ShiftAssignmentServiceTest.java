package dk.ek.shift_happens.shiftassignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.employeejobrole.EmployeeJobRole;
import dk.ek.shift_happens.employeejobrole.EmployeeJobRoleRepository;
import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shift.ShiftService;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRole;
import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRoleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for ShiftAssignmentService validation, derived from
 * "Software Quality exam - Shift Happens (1).docx".
 *
 * Covers:
 *   §"Shift Duration & Timing" rules 4 (rest >=11h) and 5 (job role match)
 *   §"Weekly Working Hours" — 2-point BVA
 *   §"Consecutive Work Days" — 3-point BVA for days and night shifts
 *   §"Consecutive Work Days" night-shift definition (>=3h in 22:00-05:00)
 */
@ExtendWith(MockitoExtension.class)
class ShiftAssignmentServiceTest {

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeJobRoleRepository employeeJobRoleRepository;

    @Mock
    private ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;

    private ShiftAssignmentService service;

    /** All shifts the service needs to look up via shiftRepository.findById. Keyed by shiftId. */
    private Map<Integer, Shift> shiftStore;

    private Shift candidateShift;

    private static final int EMPLOYEE_ID = 7;
    private static final int CANDIDATE_SHIFT_ID = 100;
    private static final int REQUIRED_ROLE_ID = 1;

    @BeforeEach
    void setUp() {
        service = new ShiftAssignmentService(
                shiftAssignmentRepository,
                shiftRepository,
                employeeRepository,
                employeeJobRoleRepository,
                shiftRequiredJobRoleRepository);

        shiftStore = new HashMap<>();
        candidateShift = openShift(
                CANDIDATE_SHIFT_ID, LocalDateTime.of(2026, 5, 12, 8, 0), LocalDateTime.of(2026, 5, 12, 16, 0));
        shiftStore.put(CANDIDATE_SHIFT_ID, candidateShift);

        lenient()
                .when(shiftRepository.findById(anyInt()))
                .thenAnswer(inv -> Optional.ofNullable(shiftStore.get(inv.getArgument(0))));
        lenient()
                .when(shiftAssignmentRepository.save(any(ShiftAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(shiftAssignmentRepository.findByEmployeeId(anyInt())).thenReturn(Collections.emptyList());

        Employee employee = new Employee();
        employee.setEmployeeId(EMPLOYEE_ID);
        employee.setEmploymentStatus("Active");
        lenient().when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        // employee holds the required role by default
        EmployeeJobRole ejr = new EmployeeJobRole();
        ejr.setEmployeeId(EMPLOYEE_ID);
        ejr.setJobRoleId(REQUIRED_ROLE_ID);
        lenient().when(employeeJobRoleRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(ejr));

        ShiftRequiredJobRole required = new ShiftRequiredJobRole();
        required.setShiftId(CANDIDATE_SHIFT_ID);
        required.setJobRoleId(REQUIRED_ROLE_ID);
        lenient()
                .when(shiftRequiredJobRoleRepository.findByShiftId(CANDIDATE_SHIFT_ID))
                .thenReturn(List.of(required));
    }

    // --- helpers -----------------------------------------------------------

    private Shift openShift(int id, LocalDateTime start, LocalDateTime end) {
        Shift s = new Shift();
        s.setShiftId(id);
        s.setDepartmentId(1);
        s.setWorkLocationId(1);
        s.setShiftName("Shift " + id);
        s.setStartDatetime(start);
        s.setEndDatetime(end);
        s.setShiftStatus(ShiftService.STATUS_OPEN);
        return s;
    }

    private ShiftAssignment candidateAssignment() {
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftId(CANDIDATE_SHIFT_ID);
        a.setEmployeeId(EMPLOYEE_ID);
        return a;
    }

    /** Register an existing assignment for the test employee — non-cancelled, with the given shift. */
    private void existingAssignment(int shiftId, LocalDateTime start, LocalDateTime end) {
        Shift s = openShift(shiftId, start, end);
        shiftStore.put(shiftId, s);
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftAssignmentId(shiftId);
        a.setShiftId(shiftId);
        a.setEmployeeId(EMPLOYEE_ID);
        a.setAssignmentStatus(ShiftAssignmentService.STATUS_ASSIGNED);
        List<ShiftAssignment> current = new ArrayList<>(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID));
        current.add(a);
        when(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(current);
    }

    // -----------------------------------------------------------------------
    // CRUD coverage for ShiftAssignmentService
    // -----------------------------------------------------------------------
    @Nested
    class CrudOperations {

        @Test
        void should_create_assignment_and_apply_defaults() {
            ShiftAssignment create = candidateAssignment();
            create.setShiftAssignmentId(999);
            create.setAssignmentStatus(null);
            create.setAssignedDatetime(null);

            ShiftAssignment saved = service.assign(create);

            assertThat(saved.getShiftAssignmentId()).isNull();
            assertThat(saved.getAssignmentStatus()).isEqualTo(ShiftAssignmentService.STATUS_ASSIGNED);
            assertThat(saved.getAssignedDatetime()).isNotNull();
        }

        @Test
        void should_read_all_assignments() {
            ShiftAssignment a1 = new ShiftAssignment();
            a1.setShiftAssignmentId(1);
            ShiftAssignment a2 = new ShiftAssignment();
            a2.setShiftAssignmentId(2);
            when(shiftAssignmentRepository.findAll()).thenReturn(List.of(a1, a2));

            List<ShiftAssignment> result = service.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ShiftAssignment::getShiftAssignmentId).containsExactly(1, 2);
        }

        @Test
        void should_read_assignments_by_employee_id() {
            ShiftAssignment a1 = new ShiftAssignment();
            a1.setEmployeeId(EMPLOYEE_ID);
            when(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(a1));

            List<ShiftAssignment> result = service.findByEmployeeId(EMPLOYEE_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        }

        @Test
        void should_read_assignment_by_id() {
            ShiftAssignment existing = candidateAssignment();
            existing.setShiftAssignmentId(77);
            when(shiftAssignmentRepository.findById(77)).thenReturn(Optional.of(existing));

            ShiftAssignment result = service.findById(77);

            assertThat(result.getShiftAssignmentId()).isEqualTo(77);
        }

        @Test
        void should_throw_not_found_when_assignment_id_missing() {
            when(shiftAssignmentRepository.findById(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(404))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("404 NOT_FOUND")
                    .hasMessageContaining("Assignment not found with id 404");
        }

        @Test
        void should_update_assignment_status_and_check_times() {
            ShiftAssignment existing = candidateAssignment();
            existing.setShiftAssignmentId(88);
            existing.setAssignmentStatus(ShiftAssignmentService.STATUS_ASSIGNED);
            when(shiftAssignmentRepository.findById(88)).thenReturn(Optional.of(existing));

            ShiftAssignment updates = new ShiftAssignment();
            updates.setAssignmentStatus(ShiftAssignmentService.STATUS_COMPLETED);
            updates.setCheckInDatetime(LocalDateTime.of(2026, 5, 12, 9, 0));
            updates.setCheckOutDatetime(LocalDateTime.of(2026, 5, 12, 15, 0));

            ShiftAssignment result = service.update(88, updates);

            assertThat(result.getAssignmentStatus()).isEqualTo(ShiftAssignmentService.STATUS_COMPLETED);
            assertThat(result.getCheckInDatetime()).isEqualTo(LocalDateTime.of(2026, 5, 12, 9, 0));
            assertThat(result.getCheckOutDatetime()).isEqualTo(LocalDateTime.of(2026, 5, 12, 15, 0));
        }

        @Test
        void should_delete_assignment() {
            ShiftAssignment existing = candidateAssignment();
            existing.setShiftAssignmentId(99);
            when(shiftAssignmentRepository.findById(99)).thenReturn(Optional.of(existing));

            service.delete(99);

            verify(shiftAssignmentRepository).delete(existing);
        }
    }

    // -----------------------------------------------------------------------
    // §"Shift Duration & Timing" Rule 4 — rest >= 11h (3-point boundary)
    // -----------------------------------------------------------------------
    @Nested
    class RestPeriodBVA {

        @Test
        void should_reject_when_gap_10h59m() {
            existingAssignment(200, LocalDateTime.of(2026, 5, 11, 12, 0), LocalDateTime.of(2026, 5, 11, 21, 1));
            assertThatThrownBy(() -> service.assign(candidateAssignment()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rest");
        }

        @Test
        void should_accept_when_gap_11h() {
            existingAssignment(200, LocalDateTime.of(2026, 5, 11, 12, 0), LocalDateTime.of(2026, 5, 11, 21, 0));
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }

        @Test
        void should_accept_when_gap_11h01m() {
            existingAssignment(200, LocalDateTime.of(2026, 5, 11, 12, 0), LocalDateTime.of(2026, 5, 11, 20, 59));
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // §"Shift Duration & Timing" Rule 5 — job role match
    // -----------------------------------------------------------------------
    @Nested
    class JobRoleMatch {

        @Test
        void should_reject_when_employee_lacks_required_role() {
            EmployeeJobRole otherRole = new EmployeeJobRole();
            otherRole.setEmployeeId(EMPLOYEE_ID);
            otherRole.setJobRoleId(999);
            when(employeeJobRoleRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(otherRole));

            assertThatThrownBy(() -> service.assign(candidateAssignment()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("job role");
        }

        @Test
        void should_accept_when_employee_holds_required_role() {
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // §"Weekly Working Hours" — 2-point BVA: 0, 0.1, 36.9, 37 valid; 37.1, 38 invalid
    // Candidate = 2026-05-12 Tue 08:00–16:00 (8h). Existing shifts placed in the same ISO week
    // with enough spacing to satisfy the 11h rest rule, so weekly-hours validation is exercised
    // in isolation rather than tripping the rest-period guard.
    //   Mon 08:00–12:00 (4h)   ← gap to Tue 08:00 is 20h ✓
    //   [Tue candidate 08:00–16:00 8h]
    //   Wed 04:00–14:00 (10h)  ← gap from Tue 16:00 is 12h ✓
    //   Thu 04:00–14:00 (10h)
    //   Fri 04:00–04:00+Xh     ← X varies per test
    //   Base existing total = 4 + 10 + 10 = 24h. Plus candidate 8h = 32h. So Fri = total - 32h.
    // -----------------------------------------------------------------------
    @Nested
    class WeeklyHoursBVA {

        private void seedBase() {
            existingAssignment(300, LocalDateTime.of(2026, 5, 11, 8, 0), LocalDateTime.of(2026, 5, 11, 12, 0));
            existingAssignment(301, LocalDateTime.of(2026, 5, 13, 4, 0), LocalDateTime.of(2026, 5, 13, 14, 0));
            existingAssignment(302, LocalDateTime.of(2026, 5, 14, 4, 0), LocalDateTime.of(2026, 5, 14, 14, 0));
        }

        private void fridayShiftMinutes(long minutes) {
            LocalDateTime start = LocalDateTime.of(2026, 5, 15, 4, 0);
            existingAssignment(303, start, start.plusMinutes(minutes));
        }

        @Test
        void should_accept_total_8h_candidate_only() {
            // 0 existing + 8h candidate = 8h ≤ 37h
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }

        @Test
        void should_accept_total_36h54m() {
            seedBase();
            // 32h base + 4h54m Fri = 36h54m
            fridayShiftMinutes(4L * 60 + 54);
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }

        @Test
        void should_accept_total_37h() {
            seedBase();
            fridayShiftMinutes(5L * 60); // total 37h boundary
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }

        @Test
        void should_reject_total_37h06m() {
            seedBase();
            fridayShiftMinutes(5L * 60 + 6); // total 37h06m
            assertThatThrownBy(() -> service.assign(candidateAssignment()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("weekly");
        }

        @Test
        void should_reject_total_38h() {
            seedBase();
            fridayShiftMinutes(6L * 60); // total 38h
            assertThatThrownBy(() -> service.assign(candidateAssignment()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("weekly");
        }
    }

    // -----------------------------------------------------------------------
    // §"Consecutive Work Days" — 3-point BVA: 5,6 valid; 7,8 invalid
    // -----------------------------------------------------------------------
    @Nested
    class ConsecutiveDaysBVA {

        /** Pre-load `daysBefore` consecutive non-cancelled shifts ending the day before candidate. */
        private void streakBefore(int daysBefore) {
            for (int i = 1; i <= daysBefore; i++) {
                LocalDateTime start = LocalDateTime.of(2026, 5, 12, 8, 0).minusDays(i);
                existingAssignment(400 + i, start, start.plusHours(2));
            }
        }

        @Test
        void should_accept_5_consecutive_days() {
            streakBefore(4);
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }

        @Test
        void should_accept_6_consecutive_days() {
            streakBefore(5);
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }

        @Test
        void should_reject_7_consecutive_days() {
            streakBefore(6);
            assertThatThrownBy(() -> service.assign(candidateAssignment()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consecutive");
        }

        @Test
        void should_reject_8_consecutive_days() {
            streakBefore(7);
            assertThatThrownBy(() -> service.assign(candidateAssignment()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // §"Consecutive Work Days" — night shift definition (>=3h in 22:00–05:00)
    // -----------------------------------------------------------------------
    @Nested
    class NightShiftDefinition {

        @Test
        void should_not_count_2h59m_within_window_as_night() {
            // 02:01 -> 05:00 = 2h59m within the 22-05 window
            Shift s = openShift(500, LocalDateTime.of(2026, 5, 12, 2, 1), LocalDateTime.of(2026, 5, 12, 5, 0));
            assertThat(service.isNightShift(s)).isFalse();
        }

        @Test
        void should_count_3h_exactly_as_night() {
            // 02:00 -> 05:00 = 3h
            Shift s = openShift(501, LocalDateTime.of(2026, 5, 12, 2, 0), LocalDateTime.of(2026, 5, 12, 5, 0));
            assertThat(service.isNightShift(s)).isTrue();
        }

        @Test
        void should_count_3h01m_as_night() {
            // 22:00 -> 01:01 next day = 3h01m
            Shift s = openShift(502, LocalDateTime.of(2026, 5, 12, 22, 0), LocalDateTime.of(2026, 5, 13, 1, 1));
            assertThat(service.isNightShift(s)).isTrue();
        }

        @Test
        void should_not_count_2h_within_window_as_night() {
            // 21:00 -> 00:00 = 2h within night window (only 22:00–00:00 falls in 22–05)
            Shift s = openShift(503, LocalDateTime.of(2026, 5, 12, 21, 0), LocalDateTime.of(2026, 5, 13, 0, 0));
            assertThat(service.isNightShift(s)).isFalse();
        }

        @Test
        void should_not_count_shift_entirely_outside_window() {
            // 14:00 -> 22:00 = 0h in window (22:00 lower bound is exclusive of preceding time)
            Shift s = openShift(504, LocalDateTime.of(2026, 5, 12, 14, 0), LocalDateTime.of(2026, 5, 12, 22, 0));
            assertThat(service.isNightShift(s)).isFalse();
        }

        @Test
        void should_count_overnight_22_to_06_as_night() {
            // 22:00 -> 06:00 = 7h in window
            Shift s = openShift(505, LocalDateTime.of(2026, 5, 12, 22, 0), LocalDateTime.of(2026, 5, 13, 6, 0));
            assertThat(service.isNightShift(s)).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // §"Consecutive Work Days" — consecutive night shifts BVA: 2,3 valid; 4,5 invalid
    // -----------------------------------------------------------------------
    @Nested
    class ConsecutiveNightShiftsBVA {

        /** Make the candidate a night shift on 2026-05-12. */
        private void candidateIsNight() {
            candidateShift.setStartDatetime(LocalDateTime.of(2026, 5, 12, 22, 0));
            candidateShift.setEndDatetime(LocalDateTime.of(2026, 5, 13, 6, 0));
        }

        /** Add `n` consecutive prior night shifts (each ≥3h in window), ending day before candidate. */
        private void priorNightStreak(int n) {
            for (int i = 1; i <= n; i++) {
                LocalDateTime start = LocalDateTime.of(2026, 5, 12, 22, 0).minusDays(i);
                LocalDateTime end = LocalDateTime.of(2026, 5, 13, 6, 0).minusDays(i);
                existingAssignment(600 + i, start, end);
            }
        }

        @Test
        void should_accept_2_consecutive_nights() {
            candidateIsNight();
            priorNightStreak(1);
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }

        @Test
        void should_accept_3_consecutive_nights() {
            candidateIsNight();
            priorNightStreak(2);
            assertThat(service.assign(candidateAssignment())).isNotNull();
        }

        @Test
        void should_reject_4_consecutive_nights() {
            candidateIsNight();
            priorNightStreak(3);
            assertThatThrownBy(() -> service.assign(candidateAssignment()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("night");
        }

        @Test
        void should_reject_5_consecutive_nights() {
            candidateIsNight();
            priorNightStreak(4);
            assertThatThrownBy(() -> service.assign(candidateAssignment()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
